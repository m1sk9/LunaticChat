---
layout: doc
---

# 設計概要

LunaticChat は Paper/Folia におけるDM・チャンネルチャット・ローマ字変換に加え，Velocity プロキシ配下での**サーバー間グローバルチャット中継**を提供します．

このページはアーキテクチャの全体像と，モジュールをまたぐ横断的な設計判断をまとめます．各モジュールの詳細は配下のページを参照してください．

## モジュール構成

Gradle マルチモジュール構成で，共有カーネルとプラットフォーム実装を分離しています．

依存の向きは一方向で，`platform-paper` と `platform-velocity` の両方が `engine` に依存し，`engine` は下流に何も依存しません．どちらのプラットフォームも protocol を「所有」せず，中立な `engine` に対して対等な peer として依存します．

| モジュール | 役割 |
|-----------|------|
| `engine` | プラットフォーム非依存のコア (ドメインモデル・プロトコル・変換・例外・権限) |
| `platform-paper` | Paper / Folia プラグイン本体 |
| `platform-velocity` | Velocity プロキシプラグイン (サーバー間チャット中継) |
| `dokka` | API ドキュメント集約専用 (Kotlin ソースなし) |

### なぜ engine を切り出すのか

engine は**共有カーネル (Shared Kernel)** です．中身は 2 系統に分かれます．

#### (a) 両者が合意しなければならない契約

Paper と Velocity が同一定義でないと壊れるもの．

- `protocol` — 両プロセス間の通信契約 (同一定義でないと通信不能)
- `chat` / `channel`, `settings` — 永続化スキーマ (`@Serializable`)
- `exception` — ドメインエラーの共通語彙
- `permission`, `command` — 権限ノード文字列とコマンド結果の中立抽象

#### (b) プラットフォーム非依存の純ロジック

どこに置いてもよいが，純粋な再利用可能のロジックを中立コアに寄せたもの．

- `converter` — ローマ字変換の純アルゴリズム (Trie) ＋外部 API クライアント

(a) を engine に一元化する最大の狙いは，**ワイヤ契約の「単一の真実源」を作ること**です．Paper と Velocity は別々にビルド・デプロイ・バージョニングされる 2 つの成果物であり，protocol を両モジュールに複製すれば必ず drift します．engine に 1 つだけ置けば，契約の不一致が「本番での実行時ミスマッチ」ではなく「コンパイルエラー / スナップショットテスト失敗」として早期に顕在化します．

engine は Bukkit / Velocity API に依存せず，Adventure / Brigadier も「型・値の意味」だけを借りて本体依存を避けています (`compileOnly` の Adventure，Brigadier に依存せず `Int` を返す `toBrigadierResult()`)．これにより engine は Minecraft サーバーを立てずに pure-JVM でテストでき，プラットフォーム都合 (Folia のスケジューラ等) は platform 側に隔離されます．

## プロトコルバージョンによる互換管理

Paper–Velocity 間の互換性は，プラグインのバージョンではなく engine が持つ **`ProtocolVersion`** だけで決まります．これが LunaticChat のマルチプラットフォーム設計の要です．

- 互換判定は MAJOR 完全一致 ＆ リモート MINOR ∈ `[MIN_SUPPORTED_MINOR, MINOR]` となり， PATCH は無視
- 後方互換は JSON の `ignoreUnknownKeys` ＋ デフォルト値付きフィールド
- `ProtocolBackwardCompatibilityTest` が JSON スナップショットで後方互換を機械的に検証

互換をプラグインバージョンから切り離した帰結として，**Paper と Velocity を独立にバージョニングでき，更新頻度の異なる 2 プラットフォームをそれぞれのペースでリリースできます**．さらにプロトコルのバンプレベルごとに更新順序が定義され (PATCH=任意 / MINOR=Velocity 先 / MAJOR=同時)，これがローリングアップデートを可能にします．

詳細は [engine - 共通カーネル](/ja/docs/developers/engine) を参照してください．

## Service Container パターン ＋ Feature Gating

`platform-paper` は外部 DI フレームワークを使わず，手動 DI で機能を組み立てます．

- `ServiceInitializer` が構築・初期化順序・shutdown を担当
- `ServiceContainer` (イミュータブルな data class) がサービスを保持
- **無効な機能はサービスが `null`** になり，型で機能の有無が表現される
- コマンド・リスナー・SettingHandler の登録が `null` 判定で条件分岐する

要は「config フラグ → `ServiceInitializer` が nullable なサービスを生成 → `ServiceContainer` の nullable フィールド → 登録処理が `null` 判定で分岐」という一本の流れです．機能が無効なら対応するサービスが型のうえで「存在しない」ことになり，そのコードパスは最初から構築されません．外部フレームワークを持ち込まず，機能トグルとライフサイクル管理を Kotlin の型と null 許容性だけで表現しているのが特徴です．

詳細は [platform-paper - Paper / Folia プラグイン本体](/ja/docs/developers/platform-paper) を参照してください．

## 横断的な設計特徴

1. **engine / platform の関心分離** — platform はプラットフォーム API との橋渡しに徹し，ドメインモデル・アルゴリズム・プロトコルは engine が持ちます．platform 側は「Bukkit / Velocity という現実」を吸収するアダプタ層です．
2. **プロトコルバージョンによる互換管理** — 互換は `ProtocolVersion` だけで決まります．
3. **Service Container ＋ Feature Gating** — 機能の有無を型で表現します．
4. **アノテーション駆動コマンド** — `@Command` / `@Permission` / `@PlayerOnly` を Kotlin リフレクションで読み Brigadier ツリーへマッピングします．コマンドの定義とメタデータ (権限・エイリアス) が同じ場所に宣言的に並びます．
5. **Folia 互換性** — 非同期処理は `asyncScheduler` と `PluginCoroutineScope` (SupervisorJob) で行い，Bukkit API 呼び出しは `scheduler.runTask` でメインスレッドへ戻します．リージョンスレッド化された Folia でも壊れないよう，スレッド境界を明示的に扱います．
6. **永続化の使い分け** — 言語/プレイヤー設定＝KAML(YAML)，チャンネル/変換キャッシュ＝kotlinx.serialization JSON，チャンネルログ＝NDJSON．いずれも「メモリキャッシュ＋非同期保存 (デバウンス/キュー)＋shutdown 同期保存」の共通パターンに従います．
7. **DM/チャンネル＝ローカル，グローバル＝プロキシ経由** — チャットの種類でルーティングが分かれ，グローバルチャットだけが Velocity を経由します．中継は「送信元サーバー除外」＋「messageId による重複排除」の二段でループを防ぎます．

## 各モジュールの詳細についてはこちら

- [engine - 共通カーネル](/ja/docs/developers/engine)
- [platform-paper - Paper / Folia プラグイン本体](/ja/docs/developers/platform-paper)
- [platform-velocity - Velocity プラグイン本体 (プロキシ中継)](/ja/docs/developers/platform-velocity)
- [ビルド・リリース・バージョニング](/ja/docs/developers/resource)

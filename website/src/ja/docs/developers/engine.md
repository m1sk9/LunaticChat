---
layout: doc
---

# engine - 共通カーネル

`engine` はプラットフォーム非依存のコアモジュールです．

Paper と Velocity が共有する契約 (protocol・スキーマ・語彙) と，プラットフォームに依存しない純ロジック (変換アルゴリズム) を集約した**共有カーネル**として位置づけられます．

Bukkit / Velocity API に依存せず，Adventure / Brigadier も「型・値の意味」だけを借りるにとどめ，本体依存を持ちません．そのため Minecraft サーバーを立てずに pure-JVM でテストできます．

engine を切り出す意図の全体像は [設計概要](/ja/docs/developers/architecture#なぜ-engine-を切り出すのか) を参照してください．

## protocol — Paper ↔ Velocity 通信

Paper と Velocity は別プロセスの成果物であり，プラグインメッセージで通信します．その**ワイヤ契約を両者が同一定義で共有するため**に protocol は engine に置かれています．片方だけが定義を変えれば通信は壊れるので，唯一の定義を engine に持たせ，不一致をコンパイル時・テスト時に検出できるようにしています．

`sealed interface PluginMessage` を頂点とする 5 種類のメッセージがあります．

| 種別 | 方向 | 主なフィールド |
|------|------|---------------|
| `Handshake` | Paper→Velocity | `pluginVersion`, `protocol` 各要素 |
| `HandshakeResponse` | Velocity→Paper | `compatible`, `velocityVersion`, `error?`, `protocol` 各要素 |
| `StatusRequest` | Paper→Velocity | (フィールドなし) |
| `StatusResponse` | Velocity→Paper | `velocityVersion`, `protocolVersion`, `online` |
| `GlobalChatMessage` | Paper↔Velocity↔Paper | `messageId`, `serverName`, `playerId`, `playerName`, `message`, `timestamp` |

`GlobalChatMessage` の `messageId` は中継ループでの重複表示を防ぐための一意 ID です．また protocol 層では UUID を素の `String` として運びます (settings/channel 層の `UUID` 型＋カスタムシリアライザとは対照的に，移送を単純化する狙い) ．

### ワイヤフォーマット

- `[subChannel: UTF][messageJson: UTF]` — `DataOutputStream.writeUTF` で「サブチャネル名」「JSON 本文」の 2 つを書き出す，Minecraft のプラグインメッセージで扱いやすい `ByteArray` 形式
- JSON は kotlinx-serialization．`Json { ignoreUnknownKeys = true }` で，新バージョンが増やした未知フィールドを旧バージョンが受け取っても壊れない (前方互換の土台)
- サブチャネル: `handshake` / `handshake_response` / `status_request` / `status_response` / `global_chat`

### バージョニング戦略 (`ProtocolVersion`)

Paper–Velocity の互換性は，プラグインバージョンではなく `ProtocolVersion` だけで判定します．SemVer に沿って，変更の性質ごとにバンプするレベルとデプロイ順が決まります．

| レベル | いつ上げる | デプロイ順 |
|--------|-----------|-----------|
| PATCH | デフォルト付き任意フィールド追加 / 無視可能な新サブチャネル | 任意 |
| MINOR | 必須フィールド追加 / 欠けると機能低下するサブチャネル | Velocity → Paper |
| MAJOR | フィールド/サブチャネルの削除・改名，ワイヤ形式変更 | 全同時 |

互換判定は「**MAJOR 完全一致 ＆ リモート MINOR ∈ `[MIN_SUPPORTED_MINOR, MINOR]`，PATCH は無視**」で行っています．これにより `MIN_SUPPORTED_MINOR` を引き上げることで，古い MINOR の受け入れを段階的に打ち切れます．新しいメッセージやフィールドを追加したときは `ProtocolBackwardCompatibilityTest` に JSON スナップショットを足し，旧フォーマットが読み続けられることを機械的に保証します．

この設計の帰結として Paper と Velocity を独立にリリースできます．詳しくは [ビルド・リリース・バージョニング](/ja/docs/developers/resource#独立バージョニング) を参照してください．

## ローマ字変換はここにはない

ローマ字変換は「プラットフォームに依存しない純ロジックだから」という理由で engine に置かれていましたが，現在は唯一の呼び出し元である [platform-paper](/ja/docs/developers/platform-paper) にあります．

プラットフォーム非依存であることは，engine に置く理由としては不十分でした．ここに置くと engine が Ktor に依存し，両プラットフォームが engine に依存する構図上，**Velocity** の成果物が呼ばれることのない HTTP クライアントを同梱してしまうためです．これを外したことで engine の依存を `kotlinx-serialization-json` だけに絞れました．

## chat/channel — チャンネルのドメインモデル

チャンネルの永続化スキーマは，保存を行う paper 側と，将来的な共有可能性を見据えて engine 側に `@Serializable` なモデルとして置かれています．

- `Channel` — `init` でバリデーション (`id` は `^[a-zA-Z0-9_-]{3,30}$`，`name` は空白不可)
- `ChannelData` — 永続化ルート．`version` フィールドでスキーマ進化に対応
- `ChannelMember` / `ChannelRole` — メンバーとロール．ロールは `OWNER` / `MODERATOR` / `MEMBER` の 3 階層
- `ChannelContext` — 非 Serializable な実行時集約 DTO (`channel` ＋ `members` を操作に渡すビュー)
- `ChannelMessageLogEntry` — NDJSON・日次ローテーション・Grafana Loki 互換を想定したログエントリ

チャンネル数・メンバー数・所属数などの上限は，engine には**例外の「語彙」だけ**を置き，具体的な閾値は config (paper 側) が注入します．「上限があること」と「上限がいくつか」を分離する設計です．

## settings — プレイヤー設定と UUID シリアライズ

永続用と実行用でモデルを分けています．

- `PlayerSettingsData` — YAML 永続化のルート．3 種の設定を UUID→Boolean のマップで保持
- `PlayerChatSettings` — 1 プレイヤー単位のフラットモデル (全設定デフォルト true)．全体マップから射影した実行時ビュー

UUID シリアライザが 2 つあるのは用途が違うためです．

`UUIDSerializer` (descriptor 名 `"UUID"`) は汎用で channel や `PlayerChatSettings.uuid` に，`UUIDASStringSerializer` (descriptor 名 `"UUIDAsString"`) は YAML 互換のため `PlayerSettingsData` の**マップキー**に使います．`kotlinx.serialization` が UUID を標準サポートしないため自前実装しています．

## exception — 共通の例外語彙

ドメインエラーを Paper / Velocity 双方で同じ型として扱えるよう，例外を engine に集約しています．共通の封印基底は持たず，`Exception` を直接継承するフラット構造です．存在/参照系・状態系・制限系・権限/BAN・KICK 系に分類でき，多くが `playerId` / `channelId` / `limit` をコンストラクタで受けてメッセージを自前生成します．基底を持たないため，呼び出し側は個別に catch する前提です．

## debug — 両プラットフォームが読む単一のスイッチ

デバッグログは実装よりも先に契約です．`config.yml` とプロキシのシステムプロパティは `velocity,protocol` を同じ意味に解釈しなければならず，片側で増えたカテゴリはもう片側にも存在しなければなりません．そのため語彙を engine に置き，書き出しだけを platform 側に残しています．

- `DebugCategory` — ログを出せる領域．運用者が書く綴りを `key` として持つ
- `DebugCategories.parse` / `resolve` — デバッグスイッチの唯一の解釈．Paper の `DebugConfigSerializer` と Velocity の `VelocityDebugSwitch` が共有する．未知の名前は例外にせず収集するため，タイポの影響はその名前だけに留まる
- `DebugLogger` — `log(category) { message }`．lambda であることが要点で，チャット経路は 1 通あたり 1 回計装されるが，無効時は文字列自体が組み立てられない
- `DebugState` — 現在有効なカテゴリ．`/lc reload` と `/lc debug` が稼働中に差し替える

`JulDebugLogger` (Paper) と `Slf4jDebugLogger` (Velocity) はどちらも `INFO` で出力します．どちらのプラットフォームも，既定のログ設定では `INFO` 未満を出力しないためです．

## permission / command — 中立抽象

Bukkit / Velocity どちらの API にも渡せる中立表現として，権限とコマンド結果を engine に置いています．

- `LunaticChatPermissionNode` — `sealed class` ＋ `object` サブクラスで権限を型安全に列挙．文字列ノードは両プラットフォームの permission API に渡せ，`when` で網羅性チェックも効く
- `CommandResult` — `sealed class` (`Success` / `SuccessWithMessage` / `Failure` / `InvalidUsage`)．メッセージは平文の `String` として運ばれ，engine に Adventure の型は入らない (装飾は platform 側の責務)．`toBrigadierResult()` は Brigadier 本体に依存せず成功=1/失敗=0 という「戻り値の意味」だけを表現する

## 関連

- [設計概要](/ja/docs/developers/architecture)
- [platform-paper - Paper / Folia プラグイン本体](/ja/docs/developers/platform-paper)
- [platform-velocity - Velocity プラグイン本体](/ja/docs/developers/platform-velocity)

---
layout: doc
---

# platform-paper - Paper / Folia プラグイン本体

`platform-paper` はプラグインの本体です．

Bukkit / Paper / Folia API・Adventure・Brigadier・Plugin Messaging といったプラットフォーム API との橋渡しに徹する層で，ドメインモデルやアルゴリズム・プロトコルは [engine](/ja/docs/developers/engine) に委譲します．

paper 側は「Bukkit / Folia という現実」を吸収するアダプタとして働き，engine の純粋なモデルをプラットフォームの都合 (スケジューラ・スレッド・イベント) に接続するのが役割です．

## エントリポイントと DI (Service Container)

外部 DI フレームワークを使わず，手動 DI でサービスを組み立てます．**「構築の責務」と「保持の責務」を分離**しているのが要点です．

- `LunaticChat` (`JavaPlugin` + `Listener`) — プラグインのエントリポイント
- `ServiceInitializer` — サービスの構築・初期化順序・shutdown を担当
- `ServiceContainer` — 構築済みサービスを保持するイミュータブルな `data class`
- `PluginCoroutineScope` — `SupervisorJob` + `Dispatchers.Default`．`UpdateChecker` などの非ブロッキング実行に使う

### ライフサイクル

`onEnable` の流れは次の通りです．

1. `saveDefaultConfig()` → `ConfigManager` で `LunaticChatConfiguration` を生成
2. `HttpClient(CIO)` と `PluginCoroutineScope` を初期化
3. `ServiceInitializer.initialize()` → `ServiceContainer` を受け取る
4. コマンドから使う公開プロパティへサービスを移し替え
5. `schedulePeriodicTasks()` → `registerCommands()` → `registerEventListeners()`
6. `checkForUpdates` が有効なら `UpdateChecker` を起動

`onDisable` は `pluginScope.cancel()` → `serviceInitializer.shutdown()` の順で，設定・キャッシュ・チャンネル・ログ・Velocity 接続を順に閉じます．

### ServiceContainer と ServiceInitializer

`ServiceContainer` は，常時利用可能なサービス (`languageManager` / `playerSettingsManager` / `directMessageHandler`) を非 null，機能ゲート対象 (`channelManager` / `velocityConnectionManager` など) を nullable フィールド (デフォルト null) として保持します．null-assertion (`!!`) をコードから排除する狙いです．

`ServiceInitializer.initialize()` は依存順にサービスを生成します．

1. `LanguageManager` (コマンドより前，全機能の前提)
2. `PlayerSettingsManager` (DM 通知などに常時必要)
3. Japanese conversion (optional)
4. Channel 群 — `ChannelManager` / `ChannelMembershipManager` / `ChannelMessageHandler` / `ChannelNotificationHandler`，ログ有効時は `ChannelMessageLogger` (optional)
5. `DirectMessageHandler` (settings・romaji・language に依存)
6. Velocity integration (optional)
7. Cross-server chat (velocity 有効 かつ `crossServerGlobalChat` かつ velocity manager 非 null のときのみ)

### Feature Gating

機能トグルの実装本体はこの `initialize()` です．Japanese conversion / Channel 群 / Velocity integration / Cross-server chat は，**config フラグが true のときだけサービスを生成し，それ以外は `null`** にします．

```
config フラグ
  → ServiceInitializer が nullable なサービスを生成
  → ServiceContainer の nullable フィールドに格納
  → コマンド・リスナー・SettingHandler の登録が null 判定で条件分岐
```

無効な機能はサービスが型のうえで「存在しない」ことになり，そのコードパスは最初から構築されません．機能の有無を Kotlin の null 許容性で表現しています．

設計思想の全体像は [設計概要](/ja/docs/developers/architecture#service-container-パターン-feature-gating) を参照してください．

## コマンドフレームワーク (アノテーション駆動 + Brigadier)

コマンドの定義とメタデータ (権限・エイリアス・プレイヤー限定) を同じ場所に宣言的に並べ，**Kotlin リフレクションで読み取って Brigadier ツリーへマッピング**します．

### アノテーション

- `@Command(name, aliases, description)` — コマンド名・エイリアス・説明
- `@Permission(KClass<out LunaticChatPermissionNode>)` — 必要権限 (engine の権限ノードを型で指定)
- `@PlayerOnly` — プレイヤー専用マーカー

### LunaticCommand

全コマンドの抽象基底です．クラスに付いたアノテーションを lazy に読み取り，`buildWithChecks()` がサブクラスの `buildCommand()` を包んで共通処理を差し込みます．

- `@Deprecated` が付いていれば，実行時にエラーメッセージを返すハンドラへ差し替える
- `@Permission` があれば Brigadier の `.requires { source.sender.hasPermission(perm) }` を付与する
- `handleResult()` が engine の `CommandResult` を Adventure メッセージ送信＋`toBrigadierResult()` の `Int` へ変換する
- `withAliases()` は Brigadier ノードを複製してエイリアスノードを生成，`applyMethodPermission()` は**メソッドレベル**の `@Permission` を反映する

### CommandRegistry

`register` / `registerAll` でコマンドを蓄積し，`initialize()` で Paper の `LifecycleEvents.COMMANDS` にハンドラを登録します．実際の Brigadier ツリー構築 (`buildWithChecks().build()`) はこのライフサイクルイベント内で行われます．

### 規約: ルートとネストサブコマンド

- **ルートコマンド** — クラスに `@Command` を付ける
- **ネストサブコマンド** — `@Command` を付けず，`build()` メソッド＋メソッドレベル `@Permission` ＋ `applyMethodPermission("build", …)` で権限を適用する

### コマンド階層

| コマンド | エイリアス | 登録条件 |
|---------|-----------|---------|
| `lc` (→ settings / status / channel) | `lunaticchat` | 常時 |
| `channel` (14 サブコマンド) | `ch` | channelChat 有効時 |
| `tell` | `t` / `msg` / `m` / `w` / `whisper` | 常時 |
| `reply` | `r` | quickReplies 有効時 |
| `lcv` (→ status) | `lunaticvelocity` | velocity 有効時 |

`settings` は `SettingKey.values()` を回して各キーに on/off/status ノードを動的生成し，`SettingHandlerRegistry` に委譲します．設定を増やすのは「SettingKey 追加 → Handler 実装 → Registry 登録」の 3 ステップです．

## チャット処理

### ルーティング (PlayerChatListener)

チャットの振り分けはここが担い，**「ローカル (チャンネル) か，グローバル (プロキシ経由の可能性) か」** を決めます．`AsyncChatEvent` を `EventPriority.HIGHEST, ignoreCancelled = true` でフックします．

処理の流れ:

1. メッセージを plain text 化し，先頭の `!` (グローバル強制プレフィックス) を判定する
2. `!` のみで本文が空なら，イベントをキャンセルして終了する (空メッセージを流さない)
3. 送信者の設定でローマ字変換が有効なら `convertWithRomaji` を通す
4. `channelManager.getPlayerChannel()` でアクティブチャンネルの有無を判定する

分岐:

- **アクティブチャンネルあり かつ `!` なし** → `event.isCancelled = true` ＋ `viewers().clear()` ＋ `message(empty)` で通常チャットを止め，`ChannelMessageHandler.sendChannelMessage()` に流す (サーバーローカル完結)
- **それ以外** (チャンネル未所属 or `!` プレフィックス) → `handleGlobalChat()`．velocity cross-server が有効なら `CrossServerChatManager.sendGlobalMessage()` へ送りつつ通常チャットも表示，無効なら通常チャットのみ

### ダイレクトメッセージ (DirectMessageHandler)

`/tell`・`/reply` の状態を管理します．`lastMessager` / `lastRecipient` の 2 つの `ConcurrentHashMap` で返信先を追跡し，`getReplyTarget()` は「自分に送ってきた人 → 自分が送った人」の優先順でオンラインのプレイヤーを返します．

`sendDirectMessage()` は，送信者設定に応じたローマ字変換 → spy プレイヤーへの hover 付き配信 (送受信者は除外) → 送受信者への整形メッセージ送信＋通知音 (設定依存) を行います．メッセージには `/tell <sender>` を補完する `ClickEvent.suggestCommand` が付きます．

### チャンネルチャット (ChannelMessageHandler)

`sendChannelMessage()` は `channelManager.getPlayerChannelContext()` でアクティブチャンネルを解決し (無ければ何もしない)，spy 配信 (送信者とメンバーを除外) → チャンネルメンバー全員への配信＋受信者通知音 → ログ有効時は engine の `ChannelMessageLogEntry.create()` で NDJSON ログ，という順で処理します．

チャンネルの状態管理そのものは `chat/channel` パッケージが担います．

- `ChannelManager` — チャンネルの単一の真実源．`channelsCache` / `membersCache` / `activeChannels` の `ConcurrentHashMap` で状態を持ち，CRUD は `kotlin.Result` を返して失敗時に engine 例外を包む．config の上限 (0 = 無制限) を検査する
- `ChannelMembershipManager` — 入退室・切替・ロールのビジネスロジック．`joinChannel()` は 存在 / 既アクティブ / BAN / private-invite / 既メンバー / 所属上限 を順に検査する
- `ChannelStorage` — `ChannelData` を JSON (`channels.json`) で永続化
- `ChannelMessageLogger` — NDJSON の非同期ロガー．日次ローテーション＋サイズ上限＋保持日数超過ファイルの定期削除

## リスナー登録

- `EventListenerRegistry` (`object`) — `SpyPermissionManager` と `PlayerPresenceListener` は常時，`PlayerChatListener` は channel / velocity cross-server / romaji のいずれかが有効なときだけ登録する (ここも Feature Gating)
- `PlayerPresenceListener` — Join でアップデート通知・nightly 警告・アクティブチャンネル復元通知，Quit で DM 参照クリア＋アクティブチャンネル解除＋設定保存
- `SpyPermissionManager` (`object : Listener`) — `Spy` 権限保持者を join/quit でキャッシュし，DM・チャンネルハンドラが参照する

## config

- `ConfigManager` — メイン `config.yml` を **Bukkit の `FileConfiguration`** からドット記法で読み，`LunaticChatConfiguration` を手組みする (この経路は KAML ではない点に注意)
- 機能デフォルト: `quickReplies=true`, `japaneseConversion=false`, `channelChat=false`, `velocityIntegration=false`
- `config/key` 以下に `FeaturesConfig` / `ChannelChatFeatureConfig` / `JapaneseConversionFeatureConfig` / `VelocityIntegrationConfig` / `QuickRepliesFeatureConfig` / `MessageFormatConfig` / `ChannelMessageLoggingConfig`

::: warning 実装ノート
`ChannelChatFeatureConfig.messageLogging` は `ConfigManager` でロードされず，デフォルト値 (enabled=true, retention=30, 100MB) 固定になっています．意図的な仕様か要確認 — 修正するか，仕様として明記するかを決める必要があります．
:::

## i18n

- `Language` (enum) — `EN` / `JA`．未知コードは EN にフォールバック
- `LanguageManager` — 起動時に `resources/languages/` を KAML でロードし，ネストした YAML をドット記法 (`toggle.on` 等) にフラット化する．`getMessage(key, placeholders)` は 選択言語 → EN フォールバック で解決し `{placeholder}` を置換，未発見はキー自身を返す．EN が無ければ致命エラー
- `MessageFormatter` (`object`) — `[LC]` プレフィックス付きの Adventure `Component` を生成し，`{braces}` プレースホルダを正規表現で検出して色分けする

## converter (paper 側) — engine 連携

paper 側は「キャッシュ管理・タイムアウト・Bukkit スケジューリング」というプラットフォーム都合を担い，変換アルゴリズムと API 通信は engine に委譲します．

- `RomanjiConverter` — 2 段変換のオーケストレータ．単語ごとに キャッシュ確認 → engine `KanaConverter` でローマ字→ひらがな → engine `GoogleIMEClient` でひらがな→漢字．API 失敗時はひらがなにフォールバック
- `ConversionCache` — engine `CacheData` を JSON 永続化．メモリキャッシュ＋デバウンス保存 (`maxEntries` 超過時の退避は ConcurrentHashMap の順不同により実質ランダム，との FIXME あり)
- `RomajiConversionHelper` — `convertWithRomaji()`．`runBlocking` + `withTimeoutOrNull` (既定 1000ms) で同期呼び出しし，成功時 `"元文 §e(変換)"`，失敗/タイムアウト時は原文を返す

## velocity 連携 (Paper 側視点)

engine の protocol を使い，Bukkit の Plugin Messaging Channel (`lunaticchat:main`) でプロキシと通信します．実際のクロスサーバールーティングは Velocity 側が担い，paper は「送出・受信・重複排除・整形表示」を担当します．

- `VelocityConnectionManager` (`PluginMessageListener`) — `ConnectionState` (DISCONNECTED / HANDSHAKING / CONNECTED / FAILED) を管理．ハンドシェイクは engine の `PluginMessage.Handshake` を encode して送信し，5 秒でタイムアウトする．循環依存回避のため `CrossServerChatManager` は後入れ (setter injection)
- ハンドシェイクは**最初のプレイヤー参加を契機に一度だけ** (`AtomicBoolean`) 実行される．参加の 1 秒後に `asyncScheduler` でスケジュールし，結果は `HandshakeResult.Success` / `Error` で受ける
- `CrossServerChatManager` — グローバルチャットの送出・受信・**重複排除**．送信時に生成した `messageId` を即キャッシュ登録して自サーバーでのエコーを防ぎ (一段目)，受信時は `messageId` の重複排除キャッシュ (TTL 60s，`cacheSize` 超過で古い順に掃除) で二重表示を防ぐ．Bukkit API 呼び出しは `scheduler.runTask` でメインスレッドに戻す

## settings / common

- `PlayerSettingsManager` — 3 種のブール設定を `ConcurrentHashMap` で管理．engine の DTO を使い，未設定はデフォルト true
- `YamlPlayerSettingsStorage` — KAML で `player-settings.yaml` を read/write．読み込み失敗時はバックアップから復旧，5 秒デバウンス保存
- `UpdateChecker` — GitHub Releases API を Ktor で叩き semver 比較．結果は sealed `UpdateCheckResult`
- `SoundCollector` — 通知音の Adventure `Sound` 定数と Player 拡張関数
- `PermissionCollector` — `@PermissionDsl` ＋ `+LunaticChatPermissionNode` 演算子で権限を集める DSL．`requirePermission` は engine の `RequirePermissionException` を投げる

## 関連

- [設計概要](/ja/docs/developers/architecture)
- [engine - 共通カーネル](/ja/docs/developers/engine)
- [platform-velocity - Velocity プラグイン本体](/ja/docs/developers/platform-velocity)

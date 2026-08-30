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

1. `saveDefaultConfig()` → `ConfigManager` で `LunaticChatConfiguration` を生成し，`MessageFormatHolder` と `ConfigurationReloader` を構築
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

`/tell`・`/reply` の状態を管理します．`lastMessager` / `lastRecipient` の 2 つの `ConcurrentHashMap` が返信先を `sealed interface ReplyTarget` (`Local` = UUID / `Remote` = プレイヤー名＋サーバー名) として追跡し，`getReplyTarget()` は「自分に送ってきた人 → 自分が送った人」の優先順で解決しながら検証します．`Local` はオンラインであること，`Remote` は `RemotePlayerRegistry` がそのサーバーに在席を報告していることが条件です．

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

- `ConfigManager` — `config.yml` を **KAML** で `LunaticChatConfiguration` にデシリアライズする．デフォルト値の定義箇所をデータクラス 1 箇所に限定するためで，以前のドット記法の手組みマッパーは同じデフォルトを二重に持っており，実際に乖離していた (`checkForUpdates` が `config.yml` とデータクラスの双方と食い違い，`messageLogging` ブロックはドキュメント化されていながら一度も読まれていなかった)
- 失敗はファイル単位ではなく設定単位で処理する．`YamlException` が出た場合は該当キーをドキュメントから取り除いてデコードを再試行するため，読めない値 1 つの影響はその値だけに留まる．全体をデフォルトに落とすのは「YAML として成立していない」場合だけで，いずれのケースも `onEnable` の外へ例外を投げない
- `ConfigManager.loadStrictly` — `/lc reload` が使う読み取り経路．pruning ループは `loadConfiguration` と共有するが，結果の解釈が逆で，1 つでも pruning が発生した時点でファイル全体を拒否し，読み取れなかった設定をまとめて報告する．設定を含まないドキュメントも拒否する．コメントのみのファイルと書き込み途中のファイルを見分けられず，それを理由に全フォーマットをリセットするのは何もしないより悪いため
- `MessageFormatHolder` — 設定のうち唯一実行時に差し替わる部分．`LunaticChatConfiguration` より意図的に狭く，この型を受け取ることが「値が変わりうる」，`LunaticChatConfiguration` を受け取ることが「起動時に凍結された」というシグナルになる．設定全体を holder に入れると，差し替わるのは `messageFormat` だけであるため，どのバージョンのファイルとも一致しないツリーができてしまう
- `ConfigurationReloader` — ファイルを読み直して holder を差し替える．各設定を「適用済み」(`MessageFormatConfig` から leaf 単位) と「再起動が必要」(データクラスの `equals` に乗せた block 単位) に分類し，`memberProperties` を走査するテストが，どちらの表にも載っていない設定が `config.yml` に増えた時点で失敗する．2 つのリストは比較基準が異なり，適用済みは現在有効なフォーマットとの差分，再起動が必要は実際にサーバーが起動した設定との差分である．`messageFormat` 以外を適用できないのは，Paper がコマンドを `COMMANDS` ライフサイクルイベントでしか登録できず，Bukkit にリスナーの解除手段がなく，キャッシュ保存タスクがキャンセル用のハンドルを保持していないため
- `LenientBoolean` — `yes` / `no` / `on` / `off` も受け付けるシリアライザ付きの `Boolean` typealias．Bukkit は `config.yml` を YAML 1.1 として読んでいたためこれらは真偽値だったが，kaml が読む YAML 1.2 では単なる文字列であり，黙ってリセットすると `checkForUpdates: no` がデフォルトの逆の値に反転してしまう
- 機能デフォルト: `quickReplies=true`, `japaneseConversion=false`, `channelChat=false`, `velocityIntegration=false`
- `config/key` 以下に `FeaturesConfig` / `ChannelChatFeatureConfig` / `JapaneseConversionFeatureConfig` / `VelocityIntegrationConfig` / `QuickRepliesFeatureConfig` / `MessageFormatConfig` / `ChannelMessageLoggingConfig`

## i18n

- `Language` (enum) — `EN` / `JA`．未知コードは EN にフォールバック
- `LanguageManager` — 起動時に `resources/languages/` を KAML でロードし，ネストした YAML をドット記法 (`toggle.on` 等) にフラット化する．`getMessage(key, placeholders)` は 選択言語 → EN フォールバック で解決し `{placeholder}` を置換，未発見はキー自身を返す．EN が無ければ致命エラー
- `MessageFormatter` (`object`) — `[LC]` プレフィックス付きの Adventure `Component` を生成し，`{braces}` プレースホルダを正規表現で検出して色分けする

## converter — ローマ字→日本語変換

ローマ字変換はアルゴリズム・API クライアント・キャッシュ・プラットフォーム都合 (タイムアウト，スケジューリング) のすべてがここにあります．かつては「プラットフォーム非依存の純ロジック」として engine にありましたが，呼び出し元は `platform-paper` だけであり，engine に置いたままでは Velocity の成果物が使わない Ktor を同梱することになるため移されました．

- `KanaConverter` (`object`) — **Trie** でローマ字→ひらがなに変換．`sealed class TrieNode { Leaf, Branch }` の不変構造で，4 文字 (`xtsu`→っ) 〜1 文字 (`a`→あ) を網羅．`isValidRomaji()` で変換前検証，`toHiragana()` は最長一致＋促音処理を行う純アルゴリズム
- `GoogleIMEClient` — Ktor `HttpClient` を DI で受け取り，Google IME (`langpair=ja-Hira|ja`) でひらがな→漢字仮名交じりに変換．レスポンスの各セグメント第 1 候補を連結する
- `RomanjiConverter` — 2 段変換のオーケストレータ．単語ごとに キャッシュ確認 → `KanaConverter` → `GoogleIMEClient`．単語は並行して変換され，API 失敗時はメッセージ全体を失敗させずひらがなにフォールバックする
- `ConversionCache` — `CacheData` を JSON 永続化．メモリキャッシュ＋デバウンス保存 (`maxEntries` 超過時に削除されるのは古い順ではなく任意の 10%，ConcurrentHashMap が順不同であるため，との FIXME あり)
- `RomajiConversionHelper` — `convertWithRomaji()` は `suspend` 関数で `withTimeoutOrNull` (既定 1000ms) により上限が設けられ，成功時 `"元文 §e(変換)"`，失敗/タイムアウト時は原文を返す．`convertWithRomajiBlocking()` はこれを `runBlocking` で包んだもので，イベントをキャンセルするか否かを return 前に決めなければならない `AsyncChatEvent` だけが使う．コマンドハンドラは tick スレッドで動くため suspend 版を使う必要がある

## velocity 連携 (Paper 側視点)

engine の protocol を使い，Bukkit の Plugin Messaging Channel (`lunaticchat:main`) でプロキシと通信します．実際のクロスサーバールーティングは Velocity 側が担い，paper は「送出・受信・重複排除・整形表示」を担当します．

- `VelocityConnectionManager` (`PluginMessageListener`) — `ConnectionState` (DISCONNECTED / HANDSHAKING / CONNECTED / FAILED) を管理．ハンドシェイクは engine の `PluginMessage.Handshake` を encode して送信し，5 秒でタイムアウトする．循環依存回避のため `CrossServerChatManager` は後入れ (setter injection)
- ハンドシェイクは**最初のプレイヤー参加を契機に一度だけ** (`AtomicBoolean`) 実行される．参加の 1 秒後に `asyncScheduler` でスケジュールし，結果は `HandshakeResult.Success` / `Error` で受ける
- `CrossServerChatManager` — グローバルチャットの送出・受信・**重複排除**．送信時に生成した `messageId` を即キャッシュ登録して自サーバーでのエコーを防ぎ (一段目)，受信時は `messageId` の重複排除キャッシュ (TTL 60s，`cacheSize` 超過で古い順に掃除) で二重表示を防ぐ．Bukkit API 呼び出しは `scheduler.runTask` でメインスレッドに戻す

## settings / common

- `PlayerSettingsManager` — 3 種のブール設定を `ConcurrentHashMap` で管理．engine の DTO を使い，未設定はデフォルト true
- `YamlPlayerSettingsStorage` — KAML で `player-settings.yaml` を read/write．5 秒デバウンス保存．バックアップファイルは存在せず，読み込み失敗時はログを出して**空の設定**にフォールバックするため，全プレイヤーの設定が黙ってデフォルトに戻る
- `UpdateChecker` — GitHub Releases API を Ktor で叩き semver 比較．結果は sealed `UpdateCheckResult`
- `SoundCollector` — 通知音の Adventure `Sound` 定数と Player 拡張関数
- `PermissionCollector` — `@PermissionDsl` ＋ `+LunaticChatPermissionNode` 演算子で権限を集める DSL．`requirePermission` は engine の `RequirePermissionException` を投げる

## 関連

- [設計概要](/ja/docs/developers/architecture)
- [engine - 共通カーネル](/ja/docs/developers/engine)
- [platform-velocity - Velocity プラグイン本体](/ja/docs/developers/platform-velocity)

---
layout: doc
---

# 設定

LunaticChat の設定は `plugins/LunaticChat/config.yml` で管理されます．サーバーの初回起動時にデフォルトの設定ファイルが生成されます．

## 設定の反映

リロードコマンドはありません．`config.yml` を編集したら**サーバーを再起動**してください．

真偽値の設定は `true` / `false` のほか，Bukkit が従来受け付けていた `yes` / `no` / `on` / `off` の表記も使用できます．古いリリース向けに書かれたファイルもそのまま動作します．

## 不正なファイルからの復帰 <Badge type="tip" text="v1.3.0~" />

`config.yml` が使用できない状態であっても，プラグインの起動が止まることはありません．

- **1つの値**が読めない場合，その設定だけがデフォルトにフォールバックし，該当キーを示す警告がログに出力されます．ファイル内のほかの設定はそのまま反映されます
- ファイルが **YAML として不正**な場合やディスクから読み取れない場合は，すべての設定がデフォルトにフォールバックし，エラーがログに出力されます
- コメントのみのファイルは「デフォルトを使う」という有効な指定として扱われ，問題として報告されません

`config.yml` を編集したあとはサーバーログを確認してください．デフォルトに戻された設定があれば，そこに報告されています．

## グローバル設定

| キー | 型 | デフォルト | 説明 |
|------|------|------------|------|
| `debug` | Boolean | `false` | デバッグログを有効にする |
| `userSettingsFilePath` | String | `"player-settings.yaml"` | プレイヤー設定ファイルのパス |
| `checkForUpdates` | Boolean | `true` | 起動時にアップデートを確認する |
| `language` | String | `"en"` | プラグインの言語 (`en` / `ja`) |

## 機能設定 (`features`)

### クイックリプライ (`features.quickReplies`)

| キー | 型 | デフォルト | 説明 |
|------|------|------------|------|
| `enabled` | Boolean | `true` | `/reply` コマンドを有効にする |

### ローマ字変換 (`features.japaneseConversion`)

| キー | 型 | デフォルト | 説明 |
|------|------|------------|------|
| `enabled` | Boolean | `false` | ローマ字→ひらがな変換を有効にする |
| `cache.maxEntries` | Int | `500` | 変換キャッシュの最大エントリ数 |
| `cache.saveIntervalSeconds` | Int | `300` | キャッシュのディスク保存間隔(秒) |
| `cache.filePath` | String | `"conversion_cache.json"` | キャッシュファイルのパス |
| `api.timeout` | Long | `3000` | API リクエストのタイムアウト(ミリ秒) |

### チャンネルチャット (`features.channelChat`)

| キー | 型 | デフォルト | 説明 |
|------|------|------------|------|
| `enabled` | Boolean | `false` | チャンネルチャット機能を有効にする |
| `maxChannelsPerServer` | Int | `0` | サーバーあたりの最大チャンネル数(`0` = 無制限) |
| `maxMembersPerChannel` | Int | `0` | チャンネルあたりの最大メンバー数(`0` = 無制限) |
| `maxMembershipPerPlayer` | Int | `0` | プレイヤーあたりの最大参加チャンネル数(`0` = 無制限) |

#### メッセージログ (`features.channelChat.messageLogging`)

| キー | 型 | デフォルト | 説明 |
|------|------|------------|------|
| `enabled` | Boolean | `true` | チャンネルメッセージを NDJSON ファイルに記録する |
| `retentionDays` | Int | `30` | ログファイルの保持日数(`0` = 無期限) |
| `maxFileSizeMB` | Int | `100` | 単一ログファイルの最大サイズ(MB) |

### Velocity 連携 (`features.velocityIntegration`)

| キー | 型 | デフォルト | 説明 |
|------|------|------------|------|
| `enabled` | Boolean | `false` | Velocity プロキシとの連携を有効にする |
| `crossServerGlobalChat` | Boolean | `false` | サーバー間グローバルチャットを有効にする |
| `crossServerDirectMessage` | Boolean | `false` | サーバー間ダイレクトメッセージを有効にする |
| `serverName` | String | `"Unknown"` | クロスサーバーチャットで表示されるサーバー名 |
| `messageDeduplicationCacheSize` | Int | `100` | メッセージ重複排除キャッシュのサイズ |

## メッセージフォーマット (`messageFormat`)

| キー | デフォルト | 利用可能なプレースホルダー |
|------|------------|--------------------------|
| `directMessageFormat` | `§7[§e{sender} §7>> §e{recipient}§7] §f{message}` | `{sender}`, `{recipient}`, `{message}` |
| `channelMessageFormat` | `§7[§b#{channel}§7] §e{sender}: §f{message}` | `{sender}`, `{message}`, `{channel}` |
| `crossServerGlobalChatFormat` | `§7[§6{server}§7] §e{sender}: §f{message}` | `{sender}`, `{message}`, `{server}` |

## データファイル

プラグインが書き込むファイルはすべて `plugins/LunaticChat/` 配下に置かれます．

| ファイル | 書き込まれるタイミング | 備考 |
|----------|----------------------|------|
| `config.yml` | 初回起動時に生成 | プラグインが書き換えることはない |
| `player-settings.yaml` | プレイヤーが `/lc settings` で設定を変更したとき | パスは `userSettingsFilePath` で変更可能．起動時に読み取れなかった場合，**全プレイヤーの設定がデフォルトに戻る** |
| `channels.json` | チャンネルまたはメンバーシップが変化したとき | チャンネルチャットが有効な場合のみ |
| `conversion_cache.json` | `cache.saveIntervalSeconds` ごとに定期保存 | ローマ字変換が有効な場合のみ．パスは `cache.filePath` で変更可能 |
| `logs/channelchat/` | チャンネルメッセージごと | メッセージログが有効な場合のみ．[メッセージログ](/ja/docs/features/message-logging)を参照 |

保存は変更ごとではなくまとめて行われ，またすべてのファイルはアトミックに書き込まれるため，書き込み途中のファイルが読まれることはありません．いずれもサーバー停止時にも書き出されます．

## デフォルト設定ファイル

[GitHub で確認する](https://github.com/m1sk9/LunaticChat/blob/main/platform-paper/src/main/resources/config.yml)

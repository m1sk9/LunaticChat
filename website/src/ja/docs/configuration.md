---
layout: doc
---

# 設定

LunaticChat の設定は `plugins/LunaticChat/config.yml` で管理されます．サーバーの初回起動時にデフォルトの設定ファイルが生成されます．

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
| `api.retryAttempts` | Int | `2` | API リクエスト失敗時のリトライ回数 |

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
| `serverName` | String | `"Unknown"` | クロスサーバーチャットで表示されるサーバー名 |
| `messageDeduplicationCacheSize` | Int | `100` | メッセージ重複排除キャッシュのサイズ |

## メッセージフォーマット (`messageFormat`)

| キー | デフォルト | 利用可能なプレースホルダー |
|------|------------|--------------------------|
| `directMessageFormat` | `§7[§e{sender} §7>> §e{recipient}§7] §f{message}` | `{sender}`, `{recipient}`, `{message}` |
| `channelMessageFormat` | `§7[§b#{channel}§7] §e{sender}: §f{message}` | `{sender}`, `{message}`, `{channel}` |
| `crossServerGlobalChatFormat` | `§7[§6{server}§7] §e{sender}: §f{message}` | `{sender}`, `{message}`, `{server}` |

## デフォルト設定ファイル

[GitHub で確認する](https://github.com/m1sk9/LunaticChat/blob/main/platform-paper/src/main/resources/config.yml)

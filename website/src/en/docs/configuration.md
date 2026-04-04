---
layout: doc
---

# Configuration

LunaticChat's configuration is managed in `plugins/LunaticChat/config.yml`. A default configuration file is generated on the server's first startup.

## Global Settings

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `debug` | Boolean | `false` | Enable debug logging |
| `userSettingsFilePath` | String | `"player-settings.yaml"` | Path to the player settings file |
| `checkForUpdates` | Boolean | `true` | Check for updates on startup |
| `language` | String | `"en"` | Plugin language (`en` / `ja`) |

## Feature Settings (`features`)

### Quick Replies (`features.quickReplies`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable the `/reply` command |

### Japanese Conversion (`features.japaneseConversion`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable romaji-to-hiragana conversion |
| `cache.maxEntries` | Int | `500` | Maximum number of conversion cache entries |
| `cache.saveIntervalSeconds` | Int | `300` | Interval (in seconds) for saving cache to disk |
| `cache.filePath` | String | `"conversion_cache.json"` | Path to the cache file |
| `api.timeout` | Long | `3000` | API request timeout (in milliseconds) |
| `api.retryAttempts` | Int | `2` | Number of retries on API request failure |

### Channel Chat (`features.channelChat`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable the channel chat feature |
| `maxChannelsPerServer` | Int | `0` | Maximum channels per server (`0` = unlimited) |
| `maxMembersPerChannel` | Int | `0` | Maximum members per channel (`0` = unlimited) |
| `maxMembershipPerPlayer` | Int | `0` | Maximum channel memberships per player (`0` = unlimited) |

#### Message Logging (`features.channelChat.messageLogging`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | Boolean | `true` | Log channel messages to an NDJSON file |
| `retentionDays` | Int | `30` | Log file retention period in days (`0` = indefinite) |
| `maxFileSizeMB` | Int | `100` | Maximum size of a single log file (MB) |

### Velocity Integration (`features.velocityIntegration`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable integration with the Velocity proxy |
| `crossServerGlobalChat` | Boolean | `false` | Enable cross-server global chat |
| `serverName` | String | `"Unknown"` | Server name displayed in cross-server chat |
| `messageDeduplicationCacheSize` | Int | `100` | Size of the message deduplication cache |

## Message Format (`messageFormat`)

| Key | Default | Available Placeholders |
|-----|---------|----------------------|
| `directMessageFormat` | `§7[§e{sender} §7>> §e{recipient}§7] §f{message}` | `{sender}`, `{recipient}`, `{message}` |
| `channelMessageFormat` | `§7[§b#{channel}§7] §e{sender}: §f{message}` | `{sender}`, `{message}`, `{channel}` |
| `crossServerGlobalChatFormat` | `§7[§6{server}§7] §e{sender}: §f{message}` | `{sender}`, `{message}`, `{server}` |

## Default Configuration File

[View on GitHub](https://github.com/m1sk9/LunaticChat/blob/main/platform-paper/src/main/resources/config.yml)

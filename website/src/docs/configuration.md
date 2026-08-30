---
layout: doc
---

# Configuration

LunaticChat's configuration is managed in `plugins/LunaticChat/config.yml`. A default configuration file is generated on the server's first startup.

## Applying Changes

`/lc reload` <Badge type="tip" text="v1.4.0~" /> re-reads `config.yml` while the server runs. It applies the [`messageFormat`](/docs/reference/message-format) settings and nothing else.

Every other setting decides something that is settled once, when the plugin starts: which services exist, which commands and listeners are registered, which files are opened. Rather than pretend otherwise, the command lists the settings you changed that the running server cannot pick up, so you know a restart is still needed.

```
[LC] Reloaded config.yml, but some of the changes need a server restart to take effect.
```

The reply says only whether what you edited is in effect. Which settings moved is written to the server log.

| | Applied by `/lc reload` | Needs a restart |
|---|---|---|
| `messageFormat.*` | Yes | — |
| `features.*` | — | Yes |
| `debug`, `checkForUpdates`, `language`, `userSettingsFilePath` | — | Yes |

The command is available to the console and RCON as well as to players, and requires `lunaticchat.command.lc.reload` (op by default).

Boolean settings accept `true` / `false`, and also the `yes` / `no` / `on` / `off` spellings that Bukkit accepted historically, so a file written for an older release keeps working as it did.

## Recovery From an Invalid File <Badge type="tip" text="v1.3.0~" />

A `config.yml` the plugin cannot use never stops the plugin from starting.

- If a **single value** cannot be read, only that setting falls back to its default, and a warning naming the key is logged. Every other setting in the file is still honoured.
- If the file is **not valid YAML at all**, or cannot be read from disk, every setting falls back to its default and an error is logged.
- A file containing only comments is a valid way of saying "use the defaults" and is not reported as a problem.

Check the server log after editing `config.yml`: a setting that quietly reverted to its default was reported there.

`/lc reload` is stricter, because it has an option that startup does not — leaving the running configuration alone. It refuses the file rather than falling anything back to a default, naming every setting it could not read in one go, and the server keeps the configuration it already had. A file that holds no settings at all is refused for the same reason: a reload cannot tell a comments-only file apart from one caught halfway through being written.

A misspelled **key** is still not detected, by either path — an unknown key is ignored so that a `config.yml` written for a newer build does not break an older one. When a reload finds nothing to apply, it says so rather than reporting success, which is the signal that a key may be misspelled.

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
| `crossServerDirectMessage` | Boolean | `false` | Enable cross-server direct messaging |
| `serverName` | String | `"Unknown"` | Server name displayed in cross-server chat |
| `messageDeduplicationCacheSize` | Int | `100` | Size of the message deduplication cache |

## Message Format (`messageFormat`)

| Key | Default | Available Placeholders |
|-----|---------|----------------------|
| `directMessageFormat` | `§7[§e{sender} §7>> §e{recipient}§7] §f{message}` | `{sender}`, `{recipient}`, `{message}` |
| `channelMessageFormat` | `§7[§b#{channel}§7] §e{sender}: §f{message}` | `{sender}`, `{message}`, `{channel}` |
| `crossServerGlobalChatFormat` | `§7[§6{server}§7] §e{sender}: §f{message}` | `{sender}`, `{message}`, `{server}` |

## Data Files

Everything the plugin writes lives under `plugins/LunaticChat/`.

| File | Written when | Notes |
|------|--------------|-------|
| `config.yml` | Generated on first startup | Never rewritten by the plugin |
| `player-settings.yaml` | A player changes a setting with `/lc settings` | Path configurable via `userSettingsFilePath`. If it cannot be read at startup, **every player's settings fall back to their defaults** |
| `channels.json` | Channels or memberships change | Only when channel chat is enabled |
| `conversion_cache.json` | Periodically, per `cache.saveIntervalSeconds` | Only when Japanese conversion is enabled. Path configurable via `cache.filePath` |
| `logs/channelchat/` | Per channel message | Only when message logging is enabled. See [Message Logging](/docs/features/message-logging) |

Saves are coalesced rather than written on every change, and every file is written atomically, so nothing ever reads a half-written file. All of them are also flushed when the server stops.

## Default Configuration File

[View on GitHub](https://github.com/m1sk9/LunaticChat/blob/main/platform-paper/src/main/resources/config.yml)

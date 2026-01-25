# Configuration

```yaml
# ----------------------------------------------
# --------------   LunaticChat   ---------------
# ----------------------------------------------
#
#  Docs: https://lc.m1sk9.dev
#  GitHub: https://github.com/m1sk9/LunaticChat
#
#  This configuration file is for customizing LunaticChat's behavior.
#  Please specify appropriate values to ensure LunaticChat functions correctly.
#
#  For detailed configuration options, please refer to the documentation:
#       Japanese: https://lc.m1sk9.dev/guide/admin/configuration
#       English:  https://lc.m1sk9.dev/en/guide/admin/configuration
# ----------------------------------------------

# If enabled, Activate LunaticChat's debug mode, which provides detailed logging for troubleshooting.
debug: false

# Path to the YAML file storing player settings
userSettingsFilePath: "player-settings.yaml"

# If enabled, LunaticChat will check for updates on startup.
checkForUpdates: true

# Plugin Configuration Language. This setting applies only to player feedback and does not affect plugin logs or similar outputs.
language: "en"

# ----------------------------------------------
# -----------   Features Settings   ------------
# ----------------------------------------------

features:
  quickReplies:
    # If enabled, the quick reply feature via the /reply command will be activated.
    enabled: true
  japaneseConversion:
    # If enabled, enables the conversion function from Roman letters to hiragana.
    enabled: false
    cache:
      # Specifies the maximum number of entries to store in the Romanization conversion cache.
      maxEntries: 500
      # Specify the interval (in seconds) for saving the Romanization conversion cache to disk.
      saveIntervalSeconds: 300
      # Specify the file path where the cache for Romanization conversion is saved.
      filePath: "conversion_cache.json"
    api:
      # Specify the timeout duration (in milliseconds) for API requests to the Romanization conversion service.
      timeout: 3000
      # Specify the number of retry attempts for failed API requests to the Romanization conversion service.
      retryAttempts: 2
  channelChat:
    # If enabled, channel-based chat functionality will be activated.
    enabled: false

# ----------------------------------------------
# ---------   Message Format Settings   --------
# ----------------------------------------------
#
# Customize the format of various chat messages here.
# You can use placeholders such as {sender}, {message}, etc.
#
# {sender} - The name of the message sender
# {recipient} - The name of the message recipient
# {message} - The content of the message
# {channel} - The name of the chat channel (only for channel chat)
# ----------------------------------------------

messageFormat:
  # Configure the format for direct messages sent via /tell or /msg
  directMessageFormat: "§7[§e{sender} §7>> §e{recipient}§7] §f{message}"
  # Configure the format for messages sent in channel chat
  channelMessageFormat: "§7[§b#{channel}§7] §e{sender}: §f{message}"
```

## General Settings

### `debug`

- Type: `boolean`
- Default: `false`

Starts LunaticChat in debug mode.

### `userSettingsFilePath`

- Type: `string`
- Default: `player-settings.yaml`

Specifies the path to the YAML file where LunaticChat saves player settings.

### `checkForUpdates`

- Type: `boolean`
- Default: `true`

Configures whether to prompt for LunaticChat updates at startup and when players with the required permissions join the server.

### `language`

- Type: `string`
- Default: `en`

Specifies the language for LunaticChat's player-facing messages.

### Supported languages:

- `en`: English
- `ja`: Japanese (日本語)

## Features Settings

### `features.quickReplies.enabled`

- Type: `boolean`
- Default: `true`

Enables the quick reply feature via the [`/reply`](../../reference/commands/reply.md) command in LunaticChat.

When disabled, the [`/reply`](../../reference/commands/reply.md) command will not be registered with Paper and cannot be used.

### `features.japaneseConversion.enabled`

- Type: `boolean`
- Default: `false`

Enables the conversion feature from romaji to hiragana.

### `features.japaneseConversion`

#### `cache.maxEntries`

- Type: `integer`
- Default: `500`

Specifies the maximum number of entries to store in the romanization conversion cache.

When this value is exceeded, the oldest entries are deleted first.

Setting a higher value improves conversion performance, but also increases memory usage and cache file size.

#### `cache.saveIntervalSeconds`

- Type: `integer`
- Default: `300`

Specifies the interval (in seconds) for saving the romanization conversion cache to disk.

#### `cache.filePath`

- Type: `string`
- Default: `conversion_cache.json`

Specifies the file path where the romanization conversion cache is saved.

The path set here is interpreted as a relative path from the `plugins/LunaticChat/` directory.

#### `api.timeout`

- Type: `integer`
- Default: `3000`

Specifies the timeout duration (in milliseconds) for API requests to the romanization conversion service.

#### `api.retryAttempts`

- Type: `integer`
- Default: `2`

Specifies the number of retry attempts for failed API requests to the romanization conversion service.

### `features.channelChat.enabled`

- Type: `boolean`
- Default: `false`

Enables channel-based chat functionality.

## Message Format Settings

Available placeholders:

- `{sender}`: Name of the message sender
- `{recipient}`: Name of the message recipient
- `{message}`: Content of the message
- `{channel}`: Name of the chat channel (only for channel chat)

### `messageFormat.directMessageFormat`

- Type: `string`
- Default: `§7[§e{sender} §7>> §e{recipient}§7] §f{message}`

Specifies the format for messages sent via direct message ([`/tell`](../../reference/commands/tell.md) or [`/reply`](../../reference/commands/reply.md) commands).

### `messageFormat.channelMessageFormat`

- Type: `string`
- Default: `§7[§b#{channel}§7] §e{sender}: §f{message}`

Specifies the format for messages sent in channel chat.

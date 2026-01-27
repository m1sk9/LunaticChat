# 設定

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
    # Maximum number of channels that can be created per server. Set to 0 for unlimited.
    maxChannelsPerServer: 0
    # Maximum number of members allowed in a single channel. Set to 0 for unlimited.
    maxMembersPerChannel: 0
    # Maximum number of channels a single player can join. Set to 0 for unlimited.
    maxMembershipPerPlayer: 0

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

LunaticChat をデバッグモードで起動します．

### `userSettingsFilePath`

- Type: `string`
- Default: `player-settings.yaml`

LunaticChat がプレイヤーの設定を保存する YAML ファイルのパスを指定します．

### `checkForUpdates`

- Type: `boolean`
- Default: `true`

LunaticChat の起動時・権限を持ったプレイヤーがサーバに参加した際に，LunaticChat のアップデートを促すかどうか設定します．

### `language`

- Type: `string`
- Default: `en`

LunaticChat のプレイヤー向けメッセージの言語を指定します．

### Supported languages:

- `en`: English
- `ja`: 日本語

## Features Settings

### `features.quickReplies.enabled`

- Type: `boolean`
- Default: `true`

LunaticChat の [`/reply`](../../reference/commands/reply.md) コマンドによるクイックリプライ機能を有効にします．

無効にすると [`/reply`](../../reference/commands/reply.md) コマンドは Paper に登録されず，使用できなくなります．

### `features.japaneseConversion.enabled`

- Type: `boolean`
- Default: `false`

ローマ字からひらがなへの変換機能を有効にします．

### `features.japaneseConversion`

#### `cache.maxEntries`

- Type: `integer`
- Default: `500`

ローマ字変換のキャッシュに保存する最大エントリ数を指定します．

この値を超えると，最も古いエントリから順に削除されます．

値を高く設定すれば，変換のパフォーマンスが向上しますが，メモリ使用量・キャッシュファイルサイズも増加します．

#### `cache.saveIntervalSeconds`

- Type: `integer`
- Default: `300`

ローマ字変換のキャッシュをディスクに保存する間隔（秒）を指定します．

#### `cache.filePath`

- Type: `string`
- Default: `conversion_cache.json`

ローマ字変換のキャッシュを保存するファイルパスを指定します．

ここで設定したパスは `plugins/LunaticChat/` ディレクトリを基準とした相対パスとして解釈されます．

#### `api.timeout`

- Type: `integer`
- Default: `3000`

ローマ字変換 API へのリクエストのタイムアウト時間（ミリ秒）を指定します．

#### `api.retryAttempts`

- Type: `integer`
- Default: `2`

ローマ字変換 API へのリクエストが失敗した場合の再試行回数を指定します．

### `features.channelChat`

#### `enabled`

- Type: `boolean`
- Default: `false`

チャンネルチャット機能を有効にします．

#### `maxChannelsPerServer`

- Type: `integer`
- Default: `0`

サーバーあたりで作成可能なチャンネルの最大数を指定します．

`0` に設定すると無制限になります．

#### `maxMembersPerChannel`

- Type: `integer`
- Default: `0`

1 つのチャンネルに参加可能なメンバーの最大数を指定します．

`0` に設定すると無制限になります．

#### `maxMembershipPerPlayer`

- Type: `integer`
- Default: `0`

1 人のプレイヤーが参加可能なチャンネルの最大数を指定します．

`0` に設定すると無制限になります．

## Message Format Settings

使用できるプレースホルダー:

- `{sender}`: メッセージ送信者の名前
- `{recipient}`: メッセージ受信者の名前
- `{message}`: メッセージの内容
- `{channel}`: チャットチャンネルの名前 (チャンネルチャットの場合のみ)

### `messageFormat.directMessageFormat`

- Type: `string`
- Default: `§7[§e{sender} §7>> §e{recipient}§7] §f{message}`

ダイレクトメッセージ（ [`/tell`](../../reference/commands/tell.md) や [`/reply`](../../reference/commands/reply.md) コマンド）で送信されるメッセージのフォーマットを指定します．

### `messageFormat.channelMessageFormat`

- Type: `string`
- Default: `§7[§b#{channel}§7] §e{sender}: §f{message}`

チャンネルチャットで送信されるメッセージのフォーマットを指定します．

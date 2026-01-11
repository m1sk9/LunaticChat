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
#       https://lc.m1sk9.dev/guide/configuration
# ----------------------------------------------

# If enabled, Activate LunaticChat's debug mode, which provides detailed logging for troubleshooting.
debug: true

# Path to the YAML file storing player settings
userSettingsFilePath: "player-settings.yaml"

# ----------------------------------------------
# -----------   Features Settings   ------------
# ----------------------------------------------

features:
  quickReplies:
    # If enabled, the quick reply feature via the /reply command will be activated.
    enabled: true
  japaneseConversion:
    # If enabled, enables the conversion function from Roman letters to hiragana.
    enabled: true
    cache:
      maxEntries: 500
      saveIntervalSeconds: 300
      filePath: "conversion_cache.json"
    api:
      timeout: 3000
      retryAttempts: 2

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
# ----------------------------------------------

messageFormat:
  # Configure the format for direct messages sent via /tell or /msg
  directMessageFormat: "§7[§e{sender} §7>> §e{recipient}§7] §f{message}"
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

## Features Settings

### `features.quickReplies.enabled`

- Type: `boolean`
- Default: `true`

LunaticChat の [`/reply`](../reference/commands/reply.md) コマンドによるクイックリプライ機能を有効にします．

無効にすると [`/reply`](../reference/commands/reply.md) コマンドは Paper に登録されず，使用できなくなります．

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

## Message Format Settings

使用できるプレースホルダー:

- `{sender}`: メッセージ送信者の名前
- `{recipient}`: メッセージ受信者の名前
- `{message}`: メッセージの内容

### `messageFormat.directMessageFormat`

- Type: `string`
- Default: `§7[§e{sender} §7>> §e{recipient}§7] §f{message}`

ダイレクトメッセージ（ [`/tell`](../reference/commands/tell.md) や [`/reply`](../reference/commands/reply.md) コマンド）で送信されるメッセージのフォーマットを指定します．

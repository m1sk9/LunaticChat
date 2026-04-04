---
layout: doc
---

# Player Settings

Players can change their personal settings using the `/lc settings` command. Settings are saved per UUID in the server's `player-settings.yaml` (the file path can be changed via `userSettingsFilePath` in the configuration file).

## Command

```
/lc settings                  # Display settings list
/lc settings <key>            # Check current value
/lc settings <key> on|off     # Change value
```

## Setting Keys

| Key | Description | Default |
|-----|-------------|---------|
| `japanese` | Enable romaji-to-Japanese conversion | `true` |
| `notice` | Enable direct message notifications | `true` |
| `chNotice` | Enable channel message notifications | `true` |

### `japanese`

Automatically converts chat messages typed in romaji to Japanese (hiragana). This setting only works when `features.japaneseConversion.enabled` is set to `true` on the server side.

```
/lc settings japanese on      # Enable conversion
/lc settings japanese off     # Disable conversion
```

### `notice`

Controls notifications when receiving direct messages (`/tell` / `/reply`).

```
/lc settings notice on        # Enable notifications
/lc settings notice off       # Disable notifications
```

### `chNotice`

Controls notifications when receiving channel chat messages. This setting only works when `features.channelChat.enabled` is set to `true` on the server side.

```
/lc settings chNotice on      # Enable notifications
/lc settings chNotice off     # Disable notifications
```

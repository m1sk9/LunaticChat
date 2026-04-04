---
layout: doc
---

# Message Format

You can customize the display format of chat messages in the `messageFormat` section of `config.yml`.

## Placeholders

| Placeholder | Description | Available formats |
|-------------|-------------|-------------------|
| `{sender}` | Name of the message sender | All |
| `{recipient}` | Name of the message recipient | `directMessageFormat` |
| `{message}` | Message content | All |
| `{channel}` | Channel name | `channelMessageFormat` |
| `{server}` | Server name | `crossServerGlobalChatFormat` |

## Format List

### `directMessageFormat`

The display format for direct messages sent via `/tell` or `/reply`.

**Default:**
```
§7[§e{sender} §7>> §e{recipient}§7] §f{message}
```

**Example:** <span style="color: gray">[</span><span style="color: gold">Steve</span> <span style="color: gray">>></span> <span style="color: gold">Alex</span><span style="color: gray">]</span> <span style="color: white">Hello!</span>

### `channelMessageFormat`

The display format for messages sent in channel chat.

**Default:**
```
§7[§b#{channel}§7] §e{sender}: §f{message}
```

**Example:** <span style="color: gray">[</span><span style="color: aqua">#general</span><span style="color: gray">]</span> <span style="color: gold">Steve:</span> <span style="color: white">Hello!</span>

### `crossServerGlobalChatFormat`

The display format for cross-server global chat when using Velocity integration.

**Default:**
```
§7[§6{server}§7] §e{sender}: §f{message}
```

**Example:** <span style="color: gray">[</span><span style="color: gold">survival</span><span style="color: gray">]</span> <span style="color: gold">Steve:</span> <span style="color: white">Hello!</span>

## Color Codes

You can use color codes with the Minecraft section sign (`§`).

| Code | Color |
|------|-------|
| `§0` | Black |
| `§1` | Dark blue |
| `§2` | Dark green |
| `§3` | Dark aqua |
| `§4` | Dark red |
| `§5` | Dark purple |
| `§6` | Gold |
| `§7` | Gray |
| `§8` | Dark gray |
| `§9` | Blue |
| `§a` | Green |
| `§b` | Aqua |
| `§c` | Red |
| `§d` | Pink |
| `§e` | Yellow |
| `§f` | White |

### Formatting Codes

| Code | Effect |
|------|--------|
| `§l` | **Bold** |
| `§o` | *Italic* |
| `§n` | <u>Underline</u> |
| `§m` | ~~Strikethrough~~ |
| `§k` | Obfuscated (characters change randomly) |
| `§r` | Reset |

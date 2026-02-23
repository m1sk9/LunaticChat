# /lc channel <Badge type="tip" text="v0.6.0" /> <Badge type="tip" text="Paper" />

### `/lc channel create <channel-id> <channel-name> [channel-description] [private-setting]`

| Permission                      |
|------------------------------|
| `lunaticchat.command.lc.channel.create` |

Creates a channel.

- `<channel-id>` specifies the channel ID.
- `<channel-name>` specifies the channel name.
- `[channel-description]` specifies the channel description (optional).
    - The description will be displayed when viewing the channel list with the `/lc channel list` command.
- `[private-setting]` specifies the channel's privacy setting.
    - If `true` is specified, it will be created as a private channel, and only invited players can join.
    - If `false` is specified or omitted, it will be created as a public channel, and anyone can join.

::: warning Notes on Entering Channel Names and Descriptions

When entering channel names and descriptions, arguments must be enclosed in `"` (double quotes).

Example:

```
/lc channel create pvp_channel "PvP Enthusiasts Club" "A gathering of people who love PvP" false
```

:::

### `/lc channel list`

| Permission                               |
|---------------------------------------|
| `lunaticchat.command.lc.channel.list` |

Displays the list of channels.

### `/lc channel join <channel-id>`

| Permission                               |
|---------------------------------------|
| `lunaticchat.command.lc.channel.join` |

Joins the specified channel.

### `/lc channel leave`

| Permission                                |
|----------------------------------------|
| `lunaticchat.command.lc.channel.leave` |

Leaves the currently active channel.

### `/lc channel switch <channel-id>`

| Permission                                 |
|-----------------------------------------|
| `lunaticchat.command.lc.channel.switch` |

Switches to the specified channel. Use this when you want to switch to another channel while in channel chat mode.

### `/lc channel status`

| Permission                                 |
|-----------------------------------------|
| `lunaticchat.command.lc.channel.status` |

Displays information about the currently joined and active channels.

### `/lc channel delete <channel-id>`

| Permission                                 |
|-----------------------------------------|
| `lunaticchat.command.lc.channel.delete` |

Deletes the specified channel. You must be the channel creator (owner).

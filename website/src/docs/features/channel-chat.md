---
layout: doc
---

# Channel Chat

Create channels to separate conversations by group. To use this feature, set `features.channelChat.enabled` to `true` in `config.yml`.

## Creating a Channel

```
/lc channel create <channelId> <name> [description] [isPrivate]
```

- `channelId`: A unique identifier for the channel (alphanumeric, `_`, `-` only, 3-30 characters)
- `name`: The display name of the channel
- `description`: A description of the channel (optional)
- `isPrivate`: Set to `true` to make the channel private (default: `false`)

The creator automatically becomes the owner.

## Joining and Leaving Channels

```
/lc channel join <channelId>    # Join a channel
/lc channel leave               # Leave the active channel
/lc channel switch <channelId>  # Switch the active channel
```

Joining a private channel requires an invitation from the owner or a moderator.

## Active Channel

Players can join multiple channels, but only one channel can be active at a time. Chat messages are sent to the active channel. Use `/lc channel switch` to change the active channel.

```
/lc channel status    # Display the current active channel and list of joined channels
```

## Roles and Permissions

Channels have three roles.

| Role | Permissions |
|------|-------------|
| **OWNER** | Delete the channel, manage moderators, transfer ownership, manage members |
| **MODERATOR** | Invite, kick, ban/unban members |
| **MEMBER** | Participate in chat, view channel information |

### Moderator Management (Owner Only)

```
/lc channel mod <playerName>         # Grant/revoke moderator permissions
/lc channel ownership <playerName>   # Transfer ownership
```

### Member Management (Owner / Moderator)

```
/lc channel invite <playerName>   # Invite a player
/lc channel kick <playerName>     # Kick a player
/lc channel ban <playerName>      # Ban a player
/lc channel unban <playerName>    # Unban a player
```

## Limit Settings

You can set channel limits in `config.yml` (set to `0` for unlimited).

| Setting Key | Description |
|-------------|-------------|
| `maxChannelsPerServer` | Maximum number of channels per server |
| `maxMembersPerChannel` | Maximum number of members per channel |
| `maxMembershipPerPlayer` | Maximum number of channels a player can join |

## Message Logging

Channel messages can be logged in NDJSON format. Files are rotated daily, and a new file with a suffix is created when `maxFileSizeMB` is exceeded.

```json
{"timestamp":"2026-04-05T14:23:45.123Z","playerId":"550e8400-...","playerName":"Steve","channelId":"general","message":"Hello!"}
```

See the `features.channelChat.messageLogging` section on the [Configuration page](/docs/configuration) for logging settings.

## Bypass Permission

Players with the `lunaticchat.channelbypass` permission (default: op) are protected from kicks and bans, and can force-delete channels.

## Message Format

The display format for channel messages can be customized via `messageFormat.channelMessageFormat` in `config.yml`. See [Message Format](/docs/reference/message-format) for details.

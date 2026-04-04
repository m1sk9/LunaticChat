---
layout: doc
---

# Command Reference

A reference for all commands available in LunaticChat.

## Direct Messages

### `/tell <player> <message>`

Sends a direct message to a player.

- **Aliases**: `t`, `msg`, `m`, `w`, `whisper`
- **Permission**: `lunaticchat.command.tell`

### `/reply <message>`

Replies to the last player who sent you a message.

- **Aliases**: `r`
- **Permission**: `lunaticchat.command.reply`
- **Prerequisite**: Quick reply feature must be enabled

## Main Command (`/lc`)

**Aliases**: `lunaticchat`

### `/lc status`

Displays the plugin version, health, enabled features, and configuration values.

- **Permission**: `lunaticchat.command.lc.status`

### `/lc settings [key] [on|off]`

Views or changes your personal settings. Without arguments, displays the settings list.

- **Permission**: `lunaticchat.command.lc.settings`
- **Setting keys**: `japanese`, `notice`, `chNotice` (see [Player Settings](/en/docs/reference/player-settings) for details)

## Channel Commands (`/lc channel`)

Only available when the channel chat feature is enabled.

### Create & Browse

#### `/lc channel create <channelId> <name> [description] [isPrivate]`

Creates a new channel. The creator becomes the owner.

- **Permission**: `lunaticchat.command.lc.channel.create`
- `channelId`: Only alphanumeric characters, underscores, and hyphens are allowed
- `isPrivate`: `true` / `false` (default: `false`)

#### `/lc channel list [page]`

Displays a list of public channels (10 per page).

- **Permission**: `lunaticchat.command.lc.channel.list`

#### `/lc channel info [channelId]`

Displays detailed information about a channel. Without arguments, shows information about the active channel.

- **Permission**: `lunaticchat.command.lc.channel.info`

### Join & Leave

#### `/lc channel join <channelId>`

Joins a channel. An invitation is required for private channels.

- **Permission**: `lunaticchat.command.lc.channel.join`

#### `/lc channel leave`

Leaves the active channel.

- **Permission**: `lunaticchat.command.lc.channel.leave`

#### `/lc channel switch <channelId>`

Switches the active channel to another channel you have already joined.

- **Permission**: `lunaticchat.command.lc.channel.switch`

#### `/lc channel status`

Displays your channel membership status (active channel and list of joined channels).

- **Permission**: `lunaticchat.command.lc.channel.status`

### Moderation (Owner / Moderator)

#### `/lc channel invite <playerName>`

Invites a player to the active channel. Bypasses private channel restrictions.

- **Permission**: `lunaticchat.command.lc.channel.invite`
- **Required role**: OWNER or MODERATOR

#### `/lc channel kick <playerName>`

Kicks a player from the active channel.

- **Permission**: `lunaticchat.command.lc.channel.kick`
- **Required role**: OWNER or MODERATOR

#### `/lc channel ban <playerName>`

Bans a player from the active channel. Banned players cannot rejoin.

- **Permission**: `lunaticchat.command.lc.channel.ban`
- **Required role**: OWNER or MODERATOR

#### `/lc channel unban <playerName>`

Unbans a player from the channel.

- **Permission**: `lunaticchat.command.lc.channel.unban`
- **Required role**: OWNER or MODERATOR

### Administration (Owner Only)

#### `/lc channel delete <channelId>`

Deletes a channel.

- **Permission**: `lunaticchat.command.lc.channel.delete`
- **Required role**: OWNER (can be bypassed with `lunaticchat.channelbypass` permission)

#### `/lc channel mod <playerName>`

Grants or revokes moderator privileges for a channel member.

- **Permission**: `lunaticchat.command.lc.channel.mod`
- **Required role**: OWNER

#### `/lc channel ownership <playerName>`

Transfers channel ownership to another member.

- **Permission**: `lunaticchat.command.lc.channel.ownership`
- **Required role**: OWNER

## Velocity Commands (`/lcv`)

**Aliases**: `lunaticvelocity`

### `/lcv status`

Displays the connection status with the Velocity proxy, protocol version, and online player count.

- **Permission**: `lunaticchat.command.lcv.status`
- **Default**: op only

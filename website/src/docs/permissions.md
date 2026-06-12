---
layout: doc
---

# Permissions

A list of all permission nodes in LunaticChat.

## Command Permissions

All command permissions are granted to all players by default.

| Permission | Description |
|------------|-------------|
| `lunaticchat.command.lc` | Use the `/lc` command |
| `lunaticchat.command.tell` | Use the `/tell` command |
| `lunaticchat.command.reply` | Use the `/reply` command |
| `lunaticchat.command.lc.settings` | Use `/lc settings` |
| `lunaticchat.command.lc.status` | Use `/lc status` |

### Channel-Related

| Permission | Description |
|------------|-------------|
| `lunaticchat.command.lc.channel` | Use `/lc channel` |
| `lunaticchat.command.lc.channel.create` | Create a channel |
| `lunaticchat.command.lc.channel.list` | View the channel list |
| `lunaticchat.command.lc.channel.join` | Join a channel |
| `lunaticchat.command.lc.channel.leave` | Leave a channel |
| `lunaticchat.command.lc.channel.switch` | Switch the active channel |
| `lunaticchat.command.lc.channel.status` | View channel membership status |
| `lunaticchat.command.lc.channel.info` | View channel information |
| `lunaticchat.command.lc.channel.delete` | Delete a channel |
| `lunaticchat.command.lc.channel.invite` | Invite to a channel |
| `lunaticchat.command.lc.channel.kick` | Kick from a channel |
| `lunaticchat.command.lc.channel.ban` | Ban from a channel |
| `lunaticchat.command.lc.channel.unban` | Unban from a channel |
| `lunaticchat.command.lc.channel.mod` | Grant or revoke moderator privileges |
| `lunaticchat.command.lc.channel.ownership` | Transfer channel ownership |

## Admin Permissions

The following permissions are granted to OPs only by default.

| Permission | Default | Description |
|------------|---------|-------------|
| `lunaticchat.spy` | op | View all direct messages on the server |
| `lunaticchat.noticeupdate` | op | Receive update notifications |
| `lunaticchat.channelbypass` | op | Bypass channel restrictions (kick/ban protection, force deletion) |
| `lunaticchat.command.lcv.status` | op | Use the `/lcv status` command |

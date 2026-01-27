# Private Channels

Private channels are chat channels that only specific users can join.

## Creating a Private Channel

To create a private channel, use the [`/lc channel create`](../../../reference/commands/lc/channel.md) command and specify `true` for the private setting argument.

```
/lc channel create <channel ID> <channel name> [channel description] [private setting]
```

## Inviting to a Channel

Private channels are not listed in the `/lc channel list` command and cannot be joined using the `/lc channel join` command.

To join, you need to receive an invitation from the owner or moderator.

To invite someone, use the [`/lc channel invite`](../../../reference/commands/lc/channel.md#lc-channel-invite-player-id) command.

```
/lc channel invite <player ID>
```

The invited player automatically joins the channel.

# Channel Chat <Badge type="tip" text="v0.6.0" /> <Badge type="warning" text="Experimental" />

This feature allows specific players to share chat.

::: warning Experimental Feature

This feature is currently implemented experimentally, and specifications may change in future updates.

Since this is a large-scale feature, we plan to implement it gradually rather than all at once.

For detailed implementation plans, see the [Roadmap (m1sk9/LunaticChat #54)](https://github.com/m1sk9/LunaticChat/issues/54).

:::

::: tip This feature must be enabled on the server

If channel chat is disabled on the server, these features cannot be used.

Please contact your server administrator.

:::

## Creating a Channel

To use channel chat, you must first create a chat channel.

Use the [`/lc channnel create`](../commands/lc/channel.md#lc-channel-create-channel-id-channel-name-channel-description-private-setting) command to create a chat channel.

```
/lc channel create <channel-id> <channel-name> [channel-description] [private-setting]
```

- `<channel-id>` specifies the channel ID.
- `<channel-name>` specifies the channel name.
- `[channel-description]` specifies the channel description (optional).
  - The description will be displayed when viewing the channel list with the `/lc channel list` command.
- `[private-setting]` specifies the channel's privacy setting.
  - If `true` is specified, it will be created as a private channel, and only invited players can join.
  - If `false` is specified or omitted, it will be created as a public channel, and anyone can join.

::: tip Channel ID Requirements

Channel IDs must follow these requirements:

- Must be between 3 and 30 characters long
- Can only contain alphanumeric characters, underscores (\_), and hyphens (-)

:::

::: warning Notes on Entering Channel Names and Descriptions

When entering channel names and descriptions, arguments must be enclosed in `"` (double quotes).

Example:

```
/lc channel create pvp_channel "PvP Enthusiasts Club" "A gathering of people who love PvP" false
```

:::

The channel will be created and a confirmation message will be displayed in chat.

## Joining a Channel

To join a created channel, use the [`/lc channel join`](../commands/lc/channel.md#lc-channel-join-channel-id) command.

```
/lc channel join <channel-id>
```

When you join a channel, all your chat messages will be visible only to players in that channel.

## Viewing the Channel List

To view the list of channels you can join, use the [`/lc channel list`](../commands/lc/channel.md#lc-channel-list) command.

```
/lc channel list
```

A list of available channels will be displayed in the chat. You can click on them or use the [`/lc channel join`](#joining-a-channel) command to join a channel.

## Sending to Global Chat

While in a channel, all your chat messages are visible only to players in that channel.

To temporarily send a message to global chat, prefix your message with `!`.

```
!Hello everyone, how are you?
```

## Difference Between Active Channel and Membership Status

In LunaticChat, a player's active channel and membership status are managed separately.

- Active channel: The channel used in channel chat mode
- Membership status: List of channels the player has joined

Only one channel can be designated as the active channel at a time. To switch the active channel to a channel in your membership status, use the [`/lc channel switch`](../commands/lc/channel.md#lc-channel-switch-channel-id) command.

```
/lc channel switch <channel-id>
```

Removing a channel from your active status does not remove it from your membership status. To leave a channel, switch to it as your active channel first, then use the [`/lc channel leave`](../commands/lc/channel.md#lc-channel-leave) command.

```
/lc channel leave
```

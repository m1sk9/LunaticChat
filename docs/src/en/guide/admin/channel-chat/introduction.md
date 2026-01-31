# Channel Chat: Deployment Guide

This guide explains how to deploy channel chat.

## What is Channel Chat

Channel chat is a feature that allows players to create channels and share chat among specific players.

For detailed features, please refer to the [Player Guide](../../player/channel-chat/about.md).

## Preparing to Deploy Channel Chat

To deploy channel chat, you need to enable the channel chat feature in the LunaticChat configuration file `config.yml`.

1. Stop the server.
2. Open `plugins/LunaticChat/config.yml`.
3. Set `features.channelChat.enabled` to `true`.
4. Restart the server.

This will enable the channel chat feature and allow players to use channel chat.

## Channel Chat Configuration

The configuration items related to channel chat are as follows:

- `features.channelChat.maxChannelsPerPlayer`: Specifies the maximum number of channels a single player can create.
- `features.channelChat.maxMembersPerChannel`: Specifies the maximum number of members that can join a single channel.
- `features.channelChat.maxMembershipPerPlayer`: Specifies the maximum number of channels a single player can join.

The default is set to `0`, which means there is no limit.

::: tip Recommended Settings

If you want to actively use the channel chat feature, we recommend setting these values higher.

However, as this may impact server performance, please set them appropriately according to your server's resource situation.

Recommended settings are as follows:

- `features.channelChat.maxChannelsPerPlayer`: `3` to `5`
- `features.channelChat.maxMembersPerChannel`: `20` to `50`
- `features.channelChat.maxMembershipPerPlayer`: `5` to `10`

:::

## Channel Chat Logging

::: warning Compatibility with Plugins like CoreProtect

As of v0.7.0, LunaticChat's channel chat feature is not compatible with logging plugins like CoreProtect.

:::

Channel chat logging is enabled by default.

For more details, see [Channel Chat: Logs](logs.md).

## Channel Management

Basically, [players manage channels themselves](../../player/channel-chat/moderation.md).

However, as a server administrator, please note the following:

- Server administrators have owner permissions for all channels. If operations are required, please respond appropriately.
- To maintain server performance, set limits on the number of channels and members as needed.
- If inappropriate channels or member behavior occurs, take appropriate action.

If you want to avoid troubles related to channel management, consider restricting moderate commands such as `/lc channel ban`.

## Plugins that Intercept Channel Chat

Logging plugins like CoreProtect do not intercept LunaticChat's channel chat messages by default.

However, plugins that use Paper API's `originalMessage()` to retrieve messages may intercept LunaticChat's channel chat messages.

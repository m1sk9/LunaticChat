# Player Guide: Getting Started

This guide is for players who have joined a server with LunaticChat installed.

::: info For Server Administrators

For plugin installation and configuration, refer to the [Admin Guide: Getting Started](../admin-guide/getting-started.md).

:::

## What You Can Do with LunaticChat

LunaticChat is a plugin that extends the chat features of your Minecraft server. The following features are available:

- **Direct Messages**: Send one-on-one messages to specific players
- **Quick Reply**: Quickly reply to the last player who messaged you with a single command
- **Channel Chat**: Create and join channels with other players to chat only among specific members
- **Romanization Conversion**: Automatically converts messages typed in romaji to hiragana

::: tip

Available features may vary depending on server configuration. Contact your server administrator for details.

:::

## Sending Direct Messages

Use the [`/tell`](./commands/tell.md) command to send a message to a specific player.

```
/tell <player> <message>
```

After receiving a message, you can quickly reply using the [`/reply`](./commands/reply.md) command.

```
/reply <message>
```

For more details, see [Direct Messages](./direct-message.md).

## Joining Channel Chat

Channel chat lets you create and join channels where only specific members can converse.

You can see available channels with [`/lc channel list`](./commands/lc/channel.md).

```
/lc channel list
```

To join a channel, use [`/lc channel join`](./commands/lc/channel.md).

```
/lc channel join <channel-id>
```

For more details, see [Channel Chat](./channel-chat/about.md).

## Using Romanization Conversion

LunaticChat can automatically convert messages typed in romaji to hiragana.

You can toggle romanization conversion on or off with the [`/lc settings`](./commands/lc/settings.md) command.

```
/lc settings romanization <on|off>
```

For more details, see [Romanization Conversion](./japanese-romanization.md).

## What's Next?

- [Channel Chat](./channel-chat/about.md): Learn how to use channel chat
- [Direct Messages](./direct-message.md): Learn how to use direct messages
- [Romanization Conversion](./japanese-romanization.md): Details on the romanization conversion feature
- [Command List](./index.md): View all commands available in LunaticChat

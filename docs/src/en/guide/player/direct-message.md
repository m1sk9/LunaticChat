# Direct Messages <Badge type="tip" text="v0.1.0" />

The direct message feature allows you to send messages to specific players only.

## Sending Messages

To send a message, use the [`/tell`](../../reference/commands/tell.md) command.

You can only send messages to online players. Messages cannot be sent to offline players.

```
/tell <player> <message>
```

::: tip Auto-completion Feature

LunaticChat supports player name auto-completion during chat input.

For example, if you type `/tell Al`, online player names starting with `Al` will be suggested as candidates.

:::

::: warning Operation in GeyserMC Environments

We do not guarantee functionality for players connecting from Minecraft Bedrock Edition using GeyserMC environments.

Since LunaticChat is designed based on the Minecraft Java Edition/Paper chat system, issues may occur when operating through GeyserMC. There are no plans to support this.

:::

## Quick Reply Feature <Badge type="tip" text="v0.1.0" />

LunaticChat provides a quick reply feature that allows you to quickly respond to the most recent player who sent you a direct message.

To use quick reply, use the [`/reply`](../../reference/commands/reply.md) command.

```
/reply <message>
```

::: warning If This Command Cannot Be Used

This command may be disabled (this feature can be toggled ON/OFF in the configuration).

Please contact your server administrator.

:::

## Notification Settings <Badge type="tip" text="v0.4.0" />

As of v0.4.0, you can change the notification settings for direct messages.

A notification sound will play when you receive or send a direct message.

To change notification settings, use the [`/lc settings`](../../reference/commands/lc/settings.md) command.

::: tip Client Volume Settings

This notification sound depends on the **Players** category volume setting on the client.

If you cannot hear LunaticChat's notification sound, check your client's volume settings.

![](../../../assets/direct-message/minecraft-player-sound.png)

:::

# Channel Management

You can manage member behavior within a channel using channel chat moderation features.

::: danger Warning

These moderation features are only available for LunaticChat channel chat.

If you need to punish actual rule violators, we recommend reporting them to the server administrator.

:::

## Moderation Feature Specifications

Moderation features have several specifications:

- Players with Bypass permissions are not affected by moderation features.
  - This means you cannot use these features against server administrators.
- To use moderation features, you need the respective channel role and server permissions.
- The effects of moderation features are independent for each channel.
  - For example, a player permanently banned (BAN) from one channel can chat normally in other channels.

## Roles

Channels have the following roles:

- Member: Regular players participating in the channel
- Moderator: Players who can use channel moderation features
- Owner: The channel creator who can use all moderation features

::: tip Moderation by Server Administrators

Server administrators uniformly have the same permissions as owner permissions in all channels.

:::

## Temporarily Expel a Member (Kick)

To temporarily expel a member from a channel, use the [`/lc channel kick`](../../../reference/commands/lc/channel.md#lc-channel-kick-player-id) command.

```
/lc channel kick <player ID>
```

Executing this command will immediately expel the specified player from the channel.

However, the expelled player can rejoin.

## Permanently Ban a Member (Ban)

To permanently ban a member from a channel, use the [`/lc channel ban`](../../../reference/commands/lc/channel.md#lc-channel-ban-player-id) command.

```
/lc channel ban <player ID>
```

Executing this command will immediately expel the specified player from the channel and also prohibit them from rejoining.

## Unban a Permanently Banned Member

To unban a permanently banned member, use the [`/lc channel unban`](../../../reference/commands/lc/channel.md#lc-channel-unban-player-id) command.

```
/lc channel unban <player ID>
```

## Appoint/Dismiss Moderators

To appoint/dismiss a channel member as a moderator, use the [`/lc channel mod`](../../../reference/commands/lc/channel.md#lc-channel-mod-player-id) command.

```
/lc channel mod <player ID>
```

Executing this command will appoint the specified player as a moderator. Executing this command again on an appointed player will dismiss them from being a moderator.

## Transfer Owner Permissions

To transfer channel owner permissions to another member, use the [`/lc channel ownership`](../../../reference/commands/lc/channel.md#lc-channel-ownership-player-id) command.

```
/lc channel ownership <player ID>
```

Executing this command will transfer all owner permissions to the specified player. The original owner becomes a moderator.

::: danger Important Notes on Execution

- Owner permission transfer is an irreversible operation.
    - Once transferred, the original owner cannot regain owner permissions unless the player with owner permissions transfers them again.
    - This applies even to server administrators.
- The recipient of owner permissions must be a member of the channel.
- Owner permissions can use all moderation features and delete the channel, which is a powerful privilege. Choose carefully to whom you transfer permissions.

:::

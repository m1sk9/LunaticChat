# Permissions

For details on configuration using LuckPerms, refer to the [LuckPerms Wiki](https://luckperms.net/wiki/Home).

## `lunaticchat.*`

### `lunaticchat.spy`

- Default: `OP`

Visualizes communication via [`/tell`](commands/tell.md) / [`/reply`](commands/reply.md) commands.

Players with this permission will have messages from other players' [`/tell`](commands/tell.md) / [`/reply`](commands/reply.md) commands broadcast to them.

### `lunaticchat.noticeUpdate`

- Default: `OP`

Receives LunaticChat update notifications.

[You must have `checkForUpdates` enabled](../guide/admin/configuration.md#checkforupdates) to receive notifications.

## `lunaticchat.command.*`

### `lunaticchat.command.tell`

- Default: `non OP`

Toggles the use of the [`/tell`](commands/tell.md) command.

### `lunaticchat.command.reply`

- Default: `non OP`

Toggles the use of the [`/reply`](commands/reply.md) command.

### `lunaticchat.command.lc.settings`

- Default: `non OP`

Toggles the use of the [`/lc settings`](commands/lc/settings.md) command.

### `lunaticchat.command.lc.status`

- Default: `non OP`

Toggles the use of the [`/lc status`](commands/lc/status.md) command.

### `lunaticchat.command.jp` <Badge type="danger" text="Deprecated: Will be removed in v1.0.0" />

- Default: `non OP`

Toggles the use of the [`/jp`](commands/jp.md) command.

### `lunaticchat.command.notice` <Badge type="danger" text="Deprecated: Will be removed in v1.0.0" />

- Default: `non OP`

Toggles the use of the [`/notice`](commands/notice.md) command.

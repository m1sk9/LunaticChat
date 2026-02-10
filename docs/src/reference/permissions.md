# パーミッション

LuckPerms を使用した設定方法に関する詳細は [LuckPerms Wiki](https://luckperms.net/wiki/Home) を参照してください．

## `lunaticchat.*`

### `lunaticchat.spy`

- Default: `OP`

各種プレイヤー間のやり取りを可視化します．

この権限を持つプレイヤーは他プレイヤーの [`/tell`](commands/tell.md) / [`/reply`](commands/reply.md) でのメッセージ・全チャンネルチャットがブロードキャストされます．

### `lunaticchat.noticeUpdate`

- Default: `OP`

LunaticChat のアップデート通知を受け取ります．

[受け取るには `checkForUpdates` を有効にしておく](../guide/admin/configuration.md#checkforupdates) 必要があります．

### `lunaticchat.channelbypass`

- Default: `OP`

各チャンネルのモデレート機能やプライベートチャンネルに対する制限を無視します．

## `lunaticchat.command.*`

### `lunaticchat.command.tell`

- Default: `non OP`

[`/tell`](commands/tell.md) コマンドの使用を切り替えます．

### `lunaticchat.command.reply`

- Default: `non OP`

[`/reply`](commands/reply.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc`

- Default: `non OP`

`/lc` コマンドの使用を切り替えます．

### `lunaticchat.command.lc.settings`

- Default: `non OP`

[`/lc settings`](commands/lc/settings.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.status`

- Default: `non OP`

[`/lc status`](commands/lc/status.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel`

- Default: `non OP`

[`/lc channel`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.create`

- Default: `non OP`

[`/lc channel create`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.list`

- Default: `non OP`

[`/lc channel list`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.join`

- Default: `non OP`

[`/lc channel join`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.leave`

- Default: `non OP`

[`/lc channel leave`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.switch`

- Default: `non OP`

[`/lc channel switch`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.status`

- Default: `non OP`

[`/lc channel status`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.info`

- Default: `non OP`

[`/lc channel info`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.delete`

- Default: `non OP`

[`/lc channel delete`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.invite`

- Default: `non OP`

[`/lc channel invite`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.kick`

- Default: `non OP`

[`/lc channel kick`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.ban`

- Default: `non OP`

[`/lc channel ban`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.unban`

- Default: `non OP`

[`/lc channel unban`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.mod`

- Default: `non OP`

[`/lc channel mod`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.channel.ownership`

- Default: `non OP`

[`/lc channel ownership`](commands/lc/channel.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.chatmode`

- Default: `non OP`

[`/lc chatmode`](commands/lc/chatmode.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lc.chatmode.toggle`

- Default: `non OP`

[`/lc chatmode toggle`](commands/lc/chatmode.md) コマンドの使用を切り替えます．

### `lunaticchat.command.lcv.status`

- Default: `non OP`

[`/lcv status`](commands/lcv/status.md) コマンドの使用を切り替えます．

### `lunaticchat.command.jp` <Badge type="danger" text="非推奨: v1.0.0 で削除予定" />

- Default: `non OP`

[`/jp`](commands/jp.md) コマンドの使用を切り替えます．

### `lunaticchat.command.notice` <Badge type="danger" text="非推奨: v1.0.0 で削除予定" />

- Default: `non OP`

[`/notice`](commands/notice.md) コマンドの使用を切り替えます．

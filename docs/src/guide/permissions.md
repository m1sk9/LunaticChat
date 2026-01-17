# パーミッション

LuckPerms を使用した設定方法に関する詳細は [LuckPerms Wiki](https://luckperms.net/wiki/Home) を参照してください．

## `lunaticchat.*`

### `lunaticchat.spy`

- Default: `OP`

[`/tell`](../reference/commands/tell.md) / [`/reply`](../reference/commands/reply.md) コマンドでのやり取りを可視化します．

この権限を持つプレイヤーは他プレイヤーの [`/tell`](../reference/commands/tell.md) / [`/reply`](../reference/commands/reply.md) でのメッセージがブロードキャストされます．

### `lunaticchat.noticeUpdate`

- Default: `OP`

LunaticChat のアップデート通知を受け取ります．

[受け取るには `checkForUpdates` を有効にしておく](../guide/configuration.md#checkforupdates) 必要があります．

## `lunaticchat.command.*`

### `lunaticchat.command.tell`

- Default: `non OP`

[`/tell`](../reference/commands/tell.md) コマンドの使用を切り替えます．

### `lunaticchat.command.reply`

- Default: `non OP`

[`/reply`](../reference/commands/reply.md) コマンドの使用を切り替えます．

### `lunaticchat.command.jp`

- Default: `non OP`

[`/jp`](../reference/commands/jp.md) コマンドの使用を切り替えます．

### `lunaticchat.command.notice`

- Default: `non OP`

[`/notice`](../reference/commands/notice.md) コマンドの使用を切り替えます．

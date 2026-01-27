# プライベートチャンネル

プライベートチャンネルは，特定のユーザーのみが参加できるチャットチャンネルです．

## プライベートチャンネルの作成

プライベートを作成する場合は [`/lc channel create`](../../../reference/commands/lc/channel.md) コマンドで，プライベート設定引数に `true` を指定します．

```
/lc channel create <チャンネルID> <チャンネル名> [チャンネルの説明] [プライベート設定]
```

## チャンネルへの招待

プライベートチャンネルは `/lc channel list` コマンドでは一覧表示されず， `/lc channel join` コマンドで参加することもできません．

参加するにはオーナーもしくはモデレーターから招待を受ける必要があります．

招待するには，[`/lc channel invite`](../../../reference/commands/lc/channel.md#lc-channel-invite-プレイヤーid) コマンドを使用します．

```
/lc channel invite <プレイヤーID>
```

招待を受けたプレイヤーは自動でチャンネルに参加します．

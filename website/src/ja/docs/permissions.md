---
layout: doc
---

# パーミッション

LunaticChat のすべてのパーミッションノードの一覧です．

## コマンドパーミッション

すべてのコマンドパーミッションはデフォルトで全プレイヤーに付与されています．

| パーミッション | 説明 |
|---------------|------|
| `lunaticchat.command.lc` | `/lc` コマンドの使用 |
| `lunaticchat.command.tell` | `/tell` コマンドの使用 |
| `lunaticchat.command.reply` | `/reply` コマンドの使用 |
| `lunaticchat.command.lc.settings` | `/lc settings` の使用 |
| `lunaticchat.command.lc.status` | `/lc status` の使用 |

### チャンネル関連

| パーミッション | 説明 |
|---------------|------|
| `lunaticchat.command.lc.channel` | `/lc channel` の使用 |
| `lunaticchat.command.lc.channel.create` | チャンネルの作成 |
| `lunaticchat.command.lc.channel.list` | チャンネル一覧の表示 |
| `lunaticchat.command.lc.channel.join` | チャンネルへの参加 |
| `lunaticchat.command.lc.channel.leave` | チャンネルからの退出 |
| `lunaticchat.command.lc.channel.switch` | アクティブチャンネルの切り替え |
| `lunaticchat.command.lc.channel.status` | チャンネル参加状況の確認 |
| `lunaticchat.command.lc.channel.info` | チャンネル情報の表示 |
| `lunaticchat.command.lc.channel.delete` | チャンネルの削除 |
| `lunaticchat.command.lc.channel.invite` | チャンネルへの招待 |
| `lunaticchat.command.lc.channel.kick` | チャンネルからのキック |
| `lunaticchat.command.lc.channel.ban` | チャンネルからの BAN |
| `lunaticchat.command.lc.channel.unban` | チャンネル BAN の解除 |
| `lunaticchat.command.lc.channel.mod` | モデレーター権限の付与・剥奪 |
| `lunaticchat.command.lc.channel.ownership` | チャンネルオーナーの譲渡 |

## 管理者パーミッション

以下のパーミッションはデフォルトで OP のみに付与されています．

| パーミッション | デフォルト | 説明 |
|---------------|-----------|------|
| `lunaticchat.spy` | op | サーバー上の全ダイレクトメッセージを閲覧 |
| `lunaticchat.noticeupdate` | op | アップデート通知の受信 |
| `lunaticchat.channelbypass` | op | チャンネル制限のバイパス(キック・BAN 保護，強制削除) |
| `lunaticchat.command.lcv.status` | op | `/lcv status` コマンドの使用 |

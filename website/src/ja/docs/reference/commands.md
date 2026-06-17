---
layout: doc
---

# コマンド一覧

LunaticChat で使用できるすべてのコマンドのリファレンスです．

## ダイレクトメッセージ

### `/tell <player> <message>` / `/tell <player>@<server> <message>`

プレイヤーにダイレクトメッセージを送信します．

サーバー名を指定した場合はそのサーバーにいるプレイヤーに対してメッセージを送信します．

- **エイリアス**: `t`, `msg`, `m`, `w`, `whisper`
- **パーミッション**: `lunaticchat.command.tell`

### `/reply <message>`

最後にメッセージを送ってきたプレイヤーに返信します．

- **エイリアス**: `r`
- **パーミッション**: `lunaticchat.command.reply`
- **前提条件**: クイックリプライ機能が有効であること

## メインコマンド (`/lc`)

**エイリアス**: `lunaticchat`

### `/lc status`

プラグインのバージョン，ヘルス，有効な機能，設定値を表示します．

- **エイリアス**: `st`
- **パーミッション**: `lunaticchat.command.lc.status`

### `/lc settings [key] [on|off]`

プレイヤー個人の設定を確認・変更します．引数なしで設定一覧を表示します．

- **エイリアス**: `set`
- **パーミッション**: `lunaticchat.command.lc.settings`
- **設定キー**: `japanese`, `notice`, `chNotice`(詳細は[プレイヤー設定](/ja/docs/reference/player-settings)を参照)

## チャンネルコマンド (`/lc channel`)

**エイリアス**: `ch`

チャンネルチャット機能が有効な場合にのみ使用できます．

### 作成・探索

#### `/lc channel create <channelId> <name> [description] [isPrivate]`

新しいチャンネルを作成します．作成者がオーナーになります．

- **エイリアス**: `new`
- **パーミッション**: `lunaticchat.command.lc.channel.create`
- `channelId`: 英数字，アンダースコア，ハイフンのみ使用可能
- `isPrivate`: `true` / `false`(デフォルト: `false`)

#### `/lc channel list [page]`

公開チャンネルの一覧を表示します(1ページ10件)．

- **エイリアス**: `ls`
- **パーミッション**: `lunaticchat.command.lc.channel.list`

#### `/lc channel info [channelId]`

チャンネルの詳細情報を表示します．引数なしでアクティブチャンネルの情報を表示します．

- **エイリアス**: `i`
- **パーミッション**: `lunaticchat.command.lc.channel.info`

### 参加・退出

#### `/lc channel join <channelId>`

チャンネルに参加します．プライベートチャンネルには招待が必要です．

- **エイリアス**: `j`
- **パーミッション**: `lunaticchat.command.lc.channel.join`

#### `/lc channel leave`

アクティブチャンネルから退出します．

- **エイリアス**: `l`
- **パーミッション**: `lunaticchat.command.lc.channel.leave`

#### `/lc channel switch <channelId>`

参加済みの別チャンネルをアクティブに切り替えます．

- **エイリアス**: `sw`
- **パーミッション**: `lunaticchat.command.lc.channel.switch`

#### `/lc channel status`

自分のチャンネル参加状況(アクティブチャンネルと参加チャンネル一覧)を表示します．

- **エイリアス**: `st`
- **パーミッション**: `lunaticchat.command.lc.channel.status`

### モデレーション(オーナー / モデレーター)

#### `/lc channel invite <playerName>`

プレイヤーをアクティブチャンネルに招待します．プライベートチャンネルの制限をバイパスします．

- **エイリアス**: `inv`
- **パーミッション**: `lunaticchat.command.lc.channel.invite`
- **必要ロール**: OWNER または MODERATOR

#### `/lc channel kick <playerName>`

プレイヤーをアクティブチャンネルからキックします．

- **エイリアス**: `k`
- **パーミッション**: `lunaticchat.command.lc.channel.kick`
- **必要ロール**: OWNER または MODERATOR

#### `/lc channel ban <playerName>`

プレイヤーをアクティブチャンネルから BAN します．BAN されたプレイヤーは再参加できません．

- **パーミッション**: `lunaticchat.command.lc.channel.ban`
- **必要ロール**: OWNER または MODERATOR

#### `/lc channel unban <playerName>`

プレイヤーのチャンネル BAN を解除します．

- **パーミッション**: `lunaticchat.command.lc.channel.unban`
- **必要ロール**: OWNER または MODERATOR

### 管理(オーナーのみ)

#### `/lc channel delete <channelId>`

チャンネルを削除します．

- **エイリアス**: `del`
- **パーミッション**: `lunaticchat.command.lc.channel.delete`
- **必要ロール**: OWNER(`lunaticchat.channelbypass` 権限で制限をバイパス可能)

#### `/lc channel mod <playerName>`

チャンネルメンバーのモデレーター権限を付与・剥奪します．

- **パーミッション**: `lunaticchat.command.lc.channel.mod`
- **必要ロール**: OWNER

#### `/lc channel ownership <playerName>`

チャンネルのオーナー権限を別のメンバーに譲渡します．

- **エイリアス**: `own`
- **パーミッション**: `lunaticchat.command.lc.channel.ownership`
- **必要ロール**: OWNER

## Velocity コマンド (`/lcv`)

**エイリアス**: `lunaticvelocity`

### `/lcv status`

Velocity プロキシとの接続状態，プロトコルバージョン，オンラインプレイヤー数を表示します．

- **エイリアス**: `st`
- **パーミッション**: `lunaticchat.command.lcv.status`
- **デフォルト**: op のみ

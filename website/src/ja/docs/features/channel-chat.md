---
layout: doc
---

# チャンネルチャット

チャンネルを作成してグループごとに会話を分離できます．この機能を利用するには `config.yml` で `features.channelChat.enabled` を `true` に設定してください．

## チャンネルの作成

```
/lc channel create <channelId> <name> [description] [isPrivate]
```

- `channelId`: チャンネルの一意な識別子 (英数字, `_`, `-` のみ, 3〜30文字)
- `name`: チャンネルの表示名
- `description`: チャンネルの説明 (省略可)
- `isPrivate`: プライベートチャンネルにする場合は `true` (デフォルト: `false`)

作成者は自動的にオーナーになります．

## チャンネルへの参加・退出

```
/lc channel join <channelId>    # チャンネルに参加
/lc channel leave               # アクティブチャンネルから退出
/lc channel switch <channelId>  # アクティブチャンネルを切り替え
```

プライベートチャンネルに参加するには，オーナーまたはモデレーターからの招待が必要です．

## アクティブチャンネル

プレイヤーは複数のチャンネルに参加できますが，一度にアクティブにできるチャンネルは1つです．チャットメッセージはアクティブチャンネルに送信されます．`/lc channel switch` でアクティブチャンネルを切り替えられます．

```
/lc channel status    # 現在のアクティブチャンネルと参加チャンネル一覧を表示
```

## グローバルチャットへの送信 (`!` プレフィックス)

アクティブチャンネルがある間，チャットはそのチャンネルに送信されます．メッセージの先頭に `!` を付けると，チャンネルから退出したり切り替えたりせずに，そのメッセージだけをグローバルチャットへ送信できます．

```
!みんなこんにちは      # アクティブチャンネルがあってもグローバルチャットへ送信される
```

`!` とその直後の空白は送信前に除去されるため，メッセージ本文には残りません．`!` のみのメッセージは破棄され，何も送信されません．

> [!NOTE]
> このプレフィックスはチャットリスナーが処理します．リスナーはチャンネルチャット・クロスサーバーグローバルチャット・ローマ字変換のいずれかが有効なときのみ登録されるため，3つすべてが無効な場合は先頭の `!` は入力したまま残ります．

## ロールと権限

チャンネルには3つのロールがあります．

| ロール | 権限 |
|--------|------|
| **OWNER** | チャンネルの削除，モデレーター管理，オーナー譲渡，メンバー管理 |
| **MODERATOR** | メンバーの招待，キック，BAN/BAN解除 |
| **MEMBER** | チャットへの参加，チャンネル情報の閲覧 |

### モデレーター管理 (オーナーのみ)

```
/lc channel mod <playerName>         # モデレーター権限の付与/剥奪
/lc channel ownership <playerName>   # オーナー権限の譲渡
```

### メンバー管理 (オーナー / モデレーター)

```
/lc channel invite <playerName>   # プレイヤーを招待
/lc channel kick <playerName>     # プレイヤーをキック
/lc channel ban <playerName>      # プレイヤーを BAN
/lc channel unban <playerName>    # BAN を解除
```

## 制限設定

`config.yml` でチャンネルの上限を設定できます (すべて `0` で無制限) ．

| 設定キー | 説明 |
|----------|------|
| `maxChannelsPerServer` | サーバーあたりの最大チャンネル数 |
| `maxMembersPerChannel` | チャンネルあたりの最大メンバー数 |
| `maxMembershipPerPlayer` | プレイヤーあたりの最大参加チャンネル数 |

## メッセージログ

チャンネルメッセージは NDJSON 形式でログファイルに記録できます．ファイルは日次でローテーションされ，`maxFileSizeMB` を超えるとサフィックス付きの新しいファイルが作成されます．

```json
{"timestamp":"2026-04-05T14:23:45.123Z","playerId":"550e8400-...","playerName":"Steve","channelId":"general","message":"Hello!"}
```

ログ設定の詳細は[設定ページ](/ja/docs/configuration)の `features.channelChat.messageLogging` を参照してください．

## バイパス権限

`lunaticchat.channelbypass` パーミッション (デフォルト: op) を持つプレイヤーは，キック・BAN の保護やチャンネルの強制削除が可能です．

## スパイモード

チャンネルメッセージは管理者に対して非公開ではありません．`lunaticchat.spy` パーミッション (デフォルト: op) を持つプレイヤーには，送信者とそのチャンネルのメンバーを除いてチャンネルメッセージが配信されます．詳細は[スパイモード](/ja/docs/features/admin#スパイモード)を参照してください．

## メッセージフォーマット

チャンネルメッセージの表示形式は `config.yml` の `messageFormat.channelMessageFormat` でカスタマイズできます．詳細は[メッセージフォーマット](/ja/docs/reference/message-format)を参照してください．

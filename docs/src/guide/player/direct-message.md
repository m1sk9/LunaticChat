# ダイレクトメッセージ <Badge type="tip" text="v0.1.0" />

特定のプレイヤーに対してのみメッセージを送信するダイレクトメッセージ機能です．

## メッセージを送信する

メッセージを送信するには [`/tell`](../../reference/commands/tell.md) コマンドを使用します．

オンラインプレイヤーに対してのみメッセージを送信できます．オフラインプレイヤーには送信できません．

```
/tell <player> <message>
```

::: tip 補完機能

LunaticChat はチャット入力時にプレイヤー名の補完をサポートしています．

例えば，`/tell Al` と入力した場合，`Al` で始まるオンラインプレイヤー名が候補として表示されます．

:::

::: warning GeyserMC 環境での動作

GeyserMC 環境を用いて Minecraft Bedrock Edition から接続しているプレイヤーに機能を提供している場合の動作は保証していません．

LunaticChat は Minecraft Java Edition / Paper のチャットシステムを前提として設計されているため，GeyserMC 経由での動作に問題が発生する可能性があります．対応の予定はありません．

:::

## クイックリプライ機能 <Badge type="tip" text="v0.1.0" />

LunaticChat では，直前にダイレクトメッセージを送信した相手に対して素早く返信できるクイックリプライ機能が提供されています．

クイックリプライを使用するには [`/reply`](../../reference/commands/reply.md) コマンドを使用します．

```
/reply <message>
```

::: warning このコマンドが使用できない場合

このコマンドが無効化されている可能性があります (この機能は設定で ON/OFF できるため)

サーバーの管理者に連絡してください．

:::

## 通知設定 <Badge type="tip" text="v0.4.0" />

v0.4.0 以降，ダイレクトメッセージの通知設定を変更できるようになりました．

ダイレクトメッセージを受信・送信時に通知音がなるようになります．

通知設定を変更するには [`/lc settings`](../../reference/commands/lc/settings.md) コマンドを使用します．

::: tip クライアントでの音量設定

この通知音はクライアント上の **プレイヤー** カテゴリーの音量設定に依存します．

LunaticChat の通知音が聞こえない場合は，クライアントの音量設定を確認してください．

![](/static/direct-message/minecraft-player-sound.png)

:::

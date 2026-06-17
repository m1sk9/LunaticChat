---
layout: doc
---

# Velocity 連携

Velocity プロキシを経由して複数の Paper / Folia サーバー間でグローバルチャットをリレーします．

::: tip 互換性について
Paper プラグインと Velocity プラグインは独立にバージョン管理されています．**両方の最新版を使えば常に動作します．** 古いバージョンを混在させたい場合は，[Paper / Velocity 互換性](/ja/docs/reference/compatibility) を参照してください．
:::

## セットアップ

### 1. Velocity プラグインの導入

`LunaticChat-<version>-velocity.jar` を Velocity の `plugins/` ディレクトリに配置し，プロキシを再起動します．

### 2. Paper 側の設定

各 Paper サーバーの `config.yml` で以下を設定します．

```yaml
features:
  velocityIntegration:
    enabled: true
    crossServerGlobalChat: true
    serverName: "survival"    # Velocity 設定のサーバー名に合わせる
```

### 3. 接続の確認

```
/lcv status
```

接続状態，プロトコルバージョン，Velocity プラグインのバージョンなどを確認できます (パーミッション: `lunaticchat.command.lcv.status`, デフォルト: op) ．

## クロスサーバーグローバルチャット

`crossServerGlobalChat` を `true` にすると，プレイヤーのチャットメッセージが Velocity を経由して他のすべての Paper サーバーに中継されます．

### メッセージの流れ

1. プレイヤーがチャットメッセージを送信
2. Paper サーバーがメッセージを Velocity に送信
3. Velocity が送信元以外の全サーバーにメッセージを中継
4. 各サーバーのプレイヤーにメッセージが表示される

### メッセージ重複排除

各メッセージに一意な ID が付与され，キャッシュにより同じメッセージが重複して表示されることを防ぎます．キャッシュサイズは `messageDeduplicationCacheSize` (デフォルト: `100`) で設定できます．

## クロスサーバーダイレクトメッセージ <Badge type="tip" text="v1.3.0~" />

`crossServerDirectMessage` を `true` にすると，同プロキシ内で接続しているサーバーのプレイヤー同士でメッセージのやり取りができるようになります．

## 接続状態

`/lcv status` で確認できる状態と，それぞれの意味は以下の通りです．

| 状態 | 説明 |
|------|------|
| `DISCONNECTED` | 未接続 |
| `HANDSHAKING` | ハンドシェイク中 |
| `CONNECTED` | 接続済み |
| `FAILED` | 接続失敗 |

ハンドシェイクのタイムアウトは 5 秒です．タイムアウトした場合，状態は `FAILED` になります．

### `FAILED` になったときの確認ポイント

- Velocity プラグインが正しく導入され，プロキシが起動しているか
- Paper の `serverName` が Velocity 設定のサーバー名と一致しているか
- Paper / Velocity プラグインの**プロトコルバージョン**が互換であるか — [互換性マトリクス](/ja/docs/reference/compatibility#互換性マトリクス) で確認できます

## 設定一覧

| 設定キー | デフォルト | 説明 |
|----------|-----------|------|
| `enabled` | `false` | Velocity 連携を有効にする |
| `crossServerGlobalChat` | `false` | クロスサーバーグローバルチャットを有効にする |
| `serverName` | `"Unknown"` | クロスサーバーチャットで表示されるサーバー名 |
| `messageDeduplicationCacheSize` | `100` | メッセージ重複排除キャッシュのサイズ |

## メッセージフォーマット

クロスサーバーチャットの表示形式は `config.yml` の `messageFormat.crossServerGlobalChatFormat` でカスタマイズできます．詳細は[メッセージフォーマット](/ja/docs/reference/message-format)を参照してください．

## 関連ドキュメント

- [Paper / Velocity 互換性](/ja/docs/reference/compatibility) — プロトコルバージョンとローリングアップデートの詳細

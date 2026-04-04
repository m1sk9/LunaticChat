---
layout: doc
---

# Velocity 連携

Velocity プロキシを経由して複数の Paper / Folia サーバー間でグローバルチャットをリレーします．

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

## プロトコルバージョン

Paper と Velocity 間の互換性はプロトコルバージョンで管理されます．接続時にハンドシェイクが行われ，互換性のないバージョン同士では接続が拒否されます．

### バージョンバンプルール

| レベル | 変更例 | 互換性 | デプロイ順序 |
|--------|--------|--------|-------------|
| PATCH (1.0.0 → 1.0.1) | optional フィールド追加，新 sub-channel 追加 | 完全互換 (`ignoreUnknownKeys=true` で安全) | 順不同，いつでも |
| MINOR (1.0.x → 1.1.0) | required フィールド追加，既存 sub-channel のセマンティクス変更 | `MIN_SUPPORTED_MINOR` の範囲内で後方互換 | **Velocity を先に更新** → 各 Paper を順次更新 |
| MAJOR (1.x.x → 2.0.0) | ワイヤフォーマット変更，sub-channel 削除/リネーム | 非互換 | **全サーバー同時デプロイ** |

### 互換性判定

ハンドシェイク時に以下のルールで互換性が判定されます:

- **MAJOR** が一致すること
- リモートの **MINOR** が `MIN_SUPPORTED_MINOR` 以上かつ自身の MINOR 以下であること
- **PATCH** は互換性判定に影響しない

#### 例: Velocity がプロトコル 1.2.0 で `MIN_SUPPORTED_MINOR=1` の場合

| Paper プロトコル | 結果 |
|-----------------|------|
| 1.1.x | 接続 OK |
| 1.2.x | 接続 OK |
| 1.0.x | 拒否 (`MIN_SUPPORTED_MINOR` より古い) |
| 1.3.x | 拒否 (Velocity より新しい) |
| 2.0.x | 拒否 (MAJOR 不一致) |

### 運用サイクル

1. **プロトコル変更なし** → Paper / Velocity を独立にデプロイ可能
2. **PATCH 変更** → どちら側からでも自由にデプロイ
3. **MINOR 変更** → Velocity を先行更新し，`MIN_SUPPORTED_MINOR` で旧 Paper の猶予期間を設定．全 Paper 更新後に `MIN_SUPPORTED_MINOR` を引き上げ
4. **MAJOR 変更** → メンテナンスウィンドウで一括更新

## 接続状態

| 状態 | 説明 |
|------|------|
| `DISCONNECTED` | 未接続 |
| `HANDSHAKING` | ハンドシェイク中 |
| `CONNECTED` | 接続済み |
| `FAILED` | 接続失敗 |

ハンドシェイクのタイムアウトは5秒です．タイムアウトした場合，状態は `FAILED` になります．

## 設定一覧

| 設定キー | デフォルト | 説明 |
|----------|-----------|------|
| `enabled` | `false` | Velocity 連携を有効にする |
| `crossServerGlobalChat` | `false` | クロスサーバーグローバルチャットを有効にする |
| `serverName` | `"Unknown"` | クロスサーバーチャットで表示されるサーバー名 |
| `messageDeduplicationCacheSize` | `100` | メッセージ重複排除キャッシュのサイズ |

## メッセージフォーマット

クロスサーバーチャットの表示形式は `config.yml` の `messageFormat.crossServerGlobalChatFormat` でカスタマイズできます．詳細は[メッセージフォーマット](/docs/reference/message-format)を参照してください．

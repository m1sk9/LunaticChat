---
layout: doc
---

# メッセージログ

チャンネルチャットのメッセージを NDJSON (Newline Delimited JSON) 形式でファイルに記録します．この機能はチャンネルチャットが有効な場合に利用でき，デフォルトで有効です．

## 設定

```yaml
# config.yml
features:
  channelChat:
    enabled: true
    messageLogging:
      enabled: true
      retentionDays: 30
      maxFileSizeMB: 100
```

| 設定キー | デフォルト | 説明 |
|----------|-----------|------|
| `enabled` | `true` | メッセージログを有効にする |
| `retentionDays` | `30` | ログファイルの保持日数 (`0` で無期限保持) |
| `maxFileSizeMB` | `100` | 単一ログファイルの最大サイズ (MB) |

## ログファイルの形式

ログファイルは `plugins/LunaticChat/logs/` ディレクトリに保存されます．各行が1つの JSON オブジェクトです．

### ファイル名

```
channel-messages-YYYY-MM-dd.json
```

ファイルサイズが `maxFileSizeMB` を超えた場合，サフィックス付きの新しいファイルが作成されます．

```
channel-messages-2026-04-05.json       # 基本ファイル
channel-messages-2026-04-05-1.json     # サイズ超過時
channel-messages-2026-04-05-2.json     # さらに超過時
```

### エントリ形式

各行は以下の JSON 構造を持ちます．

```json
{
  "timestamp": "2026-04-05T14:23:45.123Z",
  "playerId": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "Steve",
  "channelId": "general",
  "message": "Hello everyone!"
}
```

| フィールド | 型 | 説明 |
|-----------|------|------|
| `timestamp` | String | ISO 8601 形式のタイムスタンプ (UTC) |
| `playerId` | String | プレイヤーの UUID |
| `playerName` | String | プレイヤーの表示名 |
| `channelId` | String | メッセージが送信されたチャンネルの ID |
| `message` | String | メッセージの内容 |

## ファイルローテーション

- **日次ローテーション**: 日付が変わると新しいファイルが作成されます
- **サイズローテーション**: `maxFileSizeMB` を超えるとサフィックス付きファイルに切り替わります
- **自動クリーンアップ**: `retentionDays` で指定した日数を超えたログファイルは自動的に削除されます (`0` の場合は削除されません)

## ログの活用例

NDJSON 形式のため，`jq` などのツールで簡単にフィルタリング・集計が可能です．

### 特定チャンネルのメッセージを抽出

```bash
jq 'select(.channelId == "general")' channel-messages-2026-04-05.json
```

### 特定プレイヤーのメッセージを抽出

```bash
jq 'select(.playerName == "Steve")' channel-messages-2026-04-05.json
```

### メッセージ数をチャンネルごとに集計

```bash
jq -s 'group_by(.channelId) | map({channel: .[0].channelId, count: length})' channel-messages-2026-04-05.json
```

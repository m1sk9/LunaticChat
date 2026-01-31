# チャンネルチャット: ログ  <Badge type="tip" text="v0.7.0" />

チャンネルチャットのログは，チャット内で行われたすべてのメッセージとアクティビティの記録です．

::: warning CoreProtect などのプラグインとの互換性

LunaticChat では，チャンネルチャット機能に限り v0.7.0 以降 CoreProtect などのログ記録プラグインと互換性はありません．

:::

## チャンネルチャットのログを確認する

チャンネルチャットのログは `plugins/LunaticChat/logs/channelchat/` ディレクトリに保存されます．

チャンネルチャットのログファイルは，以下のフォーマットで保存されます：

```
{"timestamp":"2026-01-31T08:54:18.504343071Z","playerId":"ceaea267-39dd-3bac-931c-761ada671ebe","playerName":"m1sk9","channelId":"test","message":"こんにちは"}
```

## ファイルサイズについて

各行が1つの完全な JSON オブジェクトであり，各行で区切られているだけで，ファイル全体としては JSON 配列ではありません．

```text
plugins/LunaticChat/logs/
├── channel-messages-2026-01-17.json  (5.2 MB)
├── channel-messages-2026-01-18.json  (4.8 MB)
├── channel-messages-2026-01-19.json  (6.1 MB)
├── channel-messages-2026-01-20.json  (5.5 MB)
├── channel-messages-2026-01-21.json  (7.2 MB) <- 週末、アクティブ
├── channel-messages-2026-01-22.json  (6.9 MB)
├── channel-messages-2026-01-23.json  (4.3 MB)
├── channel-messages-2026-01-24.json  (5.0 MB)
├── channel-messages-2026-01-25.json  (5.4 MB)
├── channel-messages-2026-01-26.json  (4.9 MB)
├── channel-messages-2026-01-27.json  (6.2 MB)
├── channel-messages-2026-01-28.json  (7.5 MB)
├── channel-messages-2026-01-29.json  (5.8 MB)
├── channel-messages-2026-01-30.json  (6.0 MB)
└── channel-messages-2026-01-31.json  (2.1 MB) <- 今日（進行中）
```

合計: 約 83 MB

30日保持設定 の場合，1月17日のファイルは明日自動削除されます．

### 1メッセージあたりのサイズ

チャンネルチャットのログエントリは1行につき，約200バイトです．

1時間に1000メッセージの計算として

```text
1000 msg/h × 24h × 220 bytes = 5,280,000 bytes ≈ 5.3 MB/日
```

デフォルト設定の30日間保持の場合は **159 MB** 程度になります．

```text
5.3 MB × 30日 = 159 MB
```

## Grafana Loki での可視化

JSON 形式のため，Promtail を使用し，Grafana Loki にチャンネルチャットのログを取り込むとパースされ読みやすくなります．

```text
2026-01-31 10:23:45.123  {job="lunatichat", player="Steve", channel="Global"}
Hello everyone!

2026-01-31 10:24:12.456  {job="lunatichat", player="Alex", channel="Global"}
Hi Steve!

2026-01-31 10:25:03.789  {job="lunatichat", player="Notch", channel="Development Team"}
Working on new features
```

::: tip フィルタリング例

Grafana Loki でログをクエリ化する例:

```text
{job="lunatichat"} |= "new features"
{job="lunatichat", channel="Global"}
{job="lunatichat", player="Steve"}
```

:::

## コマンドラインでの確認例

### 最新10件を見る

```bash
tail -n 10 plugins/LunaticChat/logs/channel-messages-2026-01-31.json | jq
```

### 特定プレイヤーのメッセージを抽出

```bash
cat plugins/LunaticChat/logs/channel-messages-*.json | \
jq 'select(.playerName=="Steve")'
```

### 特定チャンネルのメッセージ数をカウント

```bash
cat plugins/LunaticChat/logs/channel-messages-*.json | \
jq 'select(.channelId=="global")' | wc -l
```

### 日付別メッセージ数

```bash
for file in plugins/LunaticChat/logs/channel-messages-*.json; do
echo "$file: $(wc -l < $file) messages"
done
```

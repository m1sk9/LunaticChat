---
layout: doc
---

# メッセージフォーマット

`config.yml` の `messageFormat` セクションで，チャットメッセージの表示形式をカスタマイズできます．

## プレースホルダー

| プレースホルダー | 説明 | 使用可能なフォーマット |
|----------------|------|----------------------|
| `{sender}` | メッセージの送信者名 | すべて |
| `{recipient}` | メッセージの受信者名 | `directMessageFormat` |
| `{message}` | メッセージの内容 | すべて |
| `{channel}` | チャンネル名 | `channelMessageFormat` |
| `{server}` | サーバー名 | `crossServerGlobalChatFormat` |

## フォーマット一覧

### `directMessageFormat`

`/tell` や `/reply` で送信されるダイレクトメッセージの表示形式です．

**デフォルト:**
```
§7[§e{sender} §7>> §e{recipient}§7] §f{message}
```

**表示例:** <span style="color: gray">[</span><span style="color: gold">Steve</span> <span style="color: gray">>></span> <span style="color: gold">Alex</span><span style="color: gray">]</span> <span style="color: white">こんにちは！</span>

### `channelMessageFormat`

チャンネルチャットで送信されるメッセージの表示形式です．

**デフォルト:**
```
§7[§b#{channel}§7] §e{sender}: §f{message}
```

**表示例:** <span style="color: gray">[</span><span style="color: aqua">#general</span><span style="color: gray">]</span> <span style="color: gold">Steve:</span> <span style="color: white">こんにちは！</span>

### `crossServerGlobalChatFormat`

Velocity 連携時のクロスサーバーグローバルチャットの表示形式です．

**デフォルト:**
```
§7[§6{server}§7] §e{sender}: §f{message}
```

**表示例:** <span style="color: gray">[</span><span style="color: gold">survival</span><span style="color: gray">]</span> <span style="color: gold">Steve:</span> <span style="color: white">こんにちは！</span>

## カラーコード

Minecraft のセクション記号(`§`)を使ったカラーコードが使用できます．

| コード | 色 |
|--------|------|
| `§0` | 黒 |
| `§1` | 濃い青 |
| `§2` | 濃い緑 |
| `§3` | 濃い水色 |
| `§4` | 濃い赤 |
| `§5` | 濃い紫 |
| `§6` | 金色 |
| `§7` | 灰色 |
| `§8` | 濃い灰色 |
| `§9` | 青 |
| `§a` | 緑 |
| `§b` | 水色 |
| `§c` | 赤 |
| `§d` | ピンク |
| `§e` | 黄色 |
| `§f` | 白 |

### 装飾コード

| コード | 効果 |
|--------|------|
| `§l` | **太字** |
| `§o` | *斜体* |
| `§n` | <u>下線</u> |
| `§m` | ~~取り消し線~~ |
| `§k` | 難読化(文字がランダムに変化) |
| `§r` | リセット |

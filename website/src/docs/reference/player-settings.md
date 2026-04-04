---
layout: doc
---

# プレイヤー設定

プレイヤーは `/lc settings` コマンドで個人設定を変更できます．設定はサーバーの `player-settings.yaml`(設定ファイルの `userSettingsFilePath` で変更可能)に UUID ごとに保存されます．

## コマンド

```
/lc settings                  # 設定一覧を表示
/lc settings <key>            # 現在の値を確認
/lc settings <key> on|off     # 値を変更
```

## 設定キー

| キー | 説明 | デフォルト |
|------|------|-----------|
| `japanese` | ローマ字→日本語変換を有効にする | `true` |
| `notice` | ダイレクトメッセージの通知を有効にする | `true` |
| `chNotice` | チャンネルメッセージの通知を有効にする | `true` |

### `japanese`

ローマ字で入力したチャットメッセージを自動的に日本語(ひらがな)に変換します．この設定はサーバー側で `features.japaneseConversion.enabled` が `true` の場合にのみ機能します．

```
/lc settings japanese on      # 変換を有効化
/lc settings japanese off     # 変換を無効化
```

### `notice`

ダイレクトメッセージ(`/tell` / `/reply`)を受信した際の通知を制御します．

```
/lc settings notice on        # 通知を有効化
/lc settings notice off       # 通知を無効化
```

### `chNotice`

チャンネルチャットのメッセージを受信した際の通知を制御します．この設定はサーバー側で `features.channelChat.enabled` が `true` の場合にのみ機能します．

```
/lc settings chNotice on      # 通知を有効化
/lc settings chNotice off     # 通知を無効化
```

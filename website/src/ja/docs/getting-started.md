---
layout: doc
---

# はじめる

LunaticChat を導入するための手順を説明します．

::: warning Spigot / BungeeCord は非対応です
LunaticChat は Paper / Folia サーバーのみをサポートしています．Spigot / BungeeCord では動作せず，今後も対応予定はありません．Spigot 環境では [LunaChat の Fork](https://github.com/f1w3/LunaChat) の使用を推奨します．
:::

## 動作要件

| 項目 | 要件 |
|------|------|
| Minecraft | 26.1 以降 |
| Java | 25 以降 |
| サーバー | Paper, Folia, または Velocity |

## ダウンロード

以下のいずれかからプラグイン JAR をダウンロードできます．

- [GitHub Releases](https://github.com/m1sk9/LunaticChat/releases)
- [Modrinth](https://modrinth.com/project/lunaticchat)

Paper / Folia サーバーには `LunaticChat-<version>.jar` を，Velocity プロキシには `LunaticChat-<version>-velocity.jar` を使用してください．

## インストール

### Paper / Folia

1. ダウンロードした `LunaticChat-<version>.jar` をサーバーの `plugins/` ディレクトリに配置します
2. サーバーを起動 (または再起動) します
3. `plugins/LunaticChat/config.yml` が自動生成されます
4. 必要に応じて[設定](/ja/docs/configuration)を変更し，サーバーを再起動します

### Velocity

1. ダウンロードした `LunaticChat-<version>-velocity.jar` を Velocity の `plugins/` ディレクトリに配置します
2. Velocity プロキシを起動 (または再起動) します
3. Paper 側の `config.yml` で `features.velocityIntegration.enabled` を `true` に設定します
4. 詳細は [Velocity 連携](/ja/docs/features/velocity)を参照してください

## 次のステップ

- [設定](/ja/docs/configuration) - `config.yml` の全設定項目を確認する
- [ダイレクトメッセージ](/ja/docs/features/direct-message) - DM 機能の使い方
- [チャンネルチャット](/ja/docs/features/channel-chat) - チャンネル機能の使い方
- [コマンド一覧](/ja/docs/reference/commands) - 全コマンドのリファレンス

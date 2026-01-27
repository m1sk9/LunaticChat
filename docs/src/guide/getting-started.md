# はじめる

## 事前準備

LunaticChat を使用するには，以下のソフトウェアが必要です：

- Java 21 (LTS 以降)
- Paper 1.21 以降
  - 最新版は [こちら](https://papermc.io/downloads/paper), それ以降のバージョンは [こちら](https://fill-ui.papermc.io/projects/paper/family/1.21) から入手できます．

::: warning Spigot 系プラットフォームでの動作について

LunaticChat は Paper プラグインです．Spigot / Bukkit / CraftBukkit では動作しません．

:::

::: tip Folia のサポートについて

Folia は現在サポートされていません．将来的に対応する可能性がありますが，現時点では Folia 上での動作は保証されていません．

詳しくは [Folia Support (m1sk9/LunaticChat #42)](https://github.com/m1sk9/LunaticChat/issues/42) を参照してください．

:::

## インストール

LunaticChat をインストールします．LunaticChat は以下から入手できます：

- [GitHub](https://github.com/m1sk9/LunaticChat/releases)
- [Modrinth](https://modrinth.com/project/lunaticchat)

ダウンロードしたプラグインファイルをサーバーの `plugins` フォルダに配置し，サーバーを起動します．

## 設定

LunaticChat を起動すると設定ファイル `config.yml` が作成されます．

該当のファイルを開き，初期設定を変更してください．なお，全設定項目の詳細については，[設定ガイド](admin/configuration.md)を参照してください．

### 推奨する初期設定

`config.yml` の以下の項目を確認・変更してください：

- `checkForUpdates`: LunaticChat のアップデートチェックを有効にするかどうかを指定します．`true` に設定することを推奨します．
- `language`: LunaticChat のメッセージ言語を指定します．日本語環境の場合は `ja` に設定してください．
  - なお，日本語環境が使用できないサーバー向けにプラグイン内のログは全て英語で出力されます．
- `features.quickReplies.enabled`: クイックリプライ機能を有効にするかどうかを指定します．`true` に設定することを推奨します．
- `features.japaneseConversion.enabled`: ローマ字からひらがなへの変換機能を有効にするかどうかを指定します．日本人向けにサーバーを開放する場合は `true` に設定することを推奨します．

::: tip その他，設定項目について

チャンネルチャットなどの各機能に関する設定項目も存在します．

必要に応じて設定を変更してください．

:::

## パーミッション

LunaticChat のパーミッションを LuckPerms などのパーミッション管理プラグインで設定します．

基本的なパーミッションは Paper や Velocity のデフォルトパーミッションシステム ( `OP` / `non OP` ) でも使用できるように開発されていますが，より詳細な制御を行う場合は LuckPerms の使用を推奨します．

- パーミッションノードの詳細については [パーミッションガイド](../reference/permissions.md)を参照してください．
- コマンドの各機能に対応するパーミッションノードは，[コマンドリファレンス](../reference/index.md)を参照してください．

## サーバーの再起動

設定が完了したら，サーバーを再起動して設定を反映させてください．

以上で LunaticChat の基本的なセットアップは完了です．

## 次は?

- [キャッシュシステム](./admin/cache.md): LunaticChat のキャッシュシステムについて説明します．
- []

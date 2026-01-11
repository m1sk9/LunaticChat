# はじめる

## インストール

プラグインをインストールします．プラグインは以下から入手できます：

- [GitHub](https://github.com/m1sk9/LunaticChat/releases)
- [Modrinth](https://modrinth.com/project/lunaticchat)

ダウンロードしたプラグインファイルをサーバーの `plugins` フォルダに配置し，サーバーを再起動します．

## 設定

LunaticChat を起動すると以下のファイルが作成されます．

- `plugins/LunaticChat/config.yml`：プラグインの設定ファイル
- `plugins/LunaticChat/player-settings.yaml`：ユーザーごとの設定ファイル
- `plugins/LunaticChat/conversion_cache.json`: ローマ字変換のキャッシュファイル

設定ファイル `config.yml` を開き，必要に応じて設定を変更します．設定項目の詳細については，[設定ガイド](./configuration.md)を参照してください．

## パーミッション

LunaticChat のパーミッションを LuckPerms などのパーミッション管理プラグインで設定します．

基本的なパーミッションは Paper や Velocity のデフォルトパーミッションシステム ( `OP` / `non OP` ) でも設定できますが，より詳細な制御を行う場合は LuckPerms の使用を推奨します．

- パーミッションノードの詳細については [パーミッションガイド](./permissions.md)を参照してください．
- コマンドの各機能に対応するパーミッションノードは，[コマンドリファレンス](../reference/index.md)を参照してください．

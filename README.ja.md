# LunaticChat

[![CI](https://github.com/m1sk9/LunaticChat/actions/workflows/ci.yaml/badge.svg)](https://github.com/m1sk9/LunaticChat/actions/workflows/ci.yaml)
[![Release](https://github.com/m1sk9/LunaticChat/actions/workflows/release.yaml/badge.svg)](https://github.com/m1sk9/LunaticChat/actions/workflows/release.yaml)
[![GNU General Public License v3.0](https://img.shields.io/badge/license-GPL--3.0-9944ee)](https://github.com/m1sk9/LunaticChat/blob/main/LICENSE)
![Modrinth Downloads](https://img.shields.io/modrinth/dt/MBeAdO4L)
[![codecov](https://codecov.io/github/m1sk9/LunaticChat/graph/badge.svg?token=M3CJYTADYD)](https://codecov.io/github/m1sk9/LunaticChat)

[English](./README.md)

Paper・Folia・Velocity 向けの次世代チャットプラグイン．

- [ドキュメント](https://lc.m1sk9.dev)
- [API ドキュメント](https://lc.api.m1sk9.dev)

```shell
git clone git@github.com:m1sk9/LunaticChat.git
cd LunaticChat

./gradlew shadowJar
```

_[Supports Minecraft 26.1.2](https://ja.minecraft.wiki/w/Java_Edition_26.1.2) | [Requires Java 25+ and Gradle 9+](.github/CONTRIBUTING.md)_

## インストール

LunaticChat は以下のプラットフォームに対応しています．

- Paper
- Velocity
- Folia

ビルド済みのプラグインを [ダウンロードページ](https://lc.m1sk9.dev/download) から入手し，ファイルをサーバーの `plugins` フォルダに配置してサーバーを再起動してください．

詳しくは [ドキュメント](https://lc.m1sk9.dev/docs/getting-started) を参照してください．

> [!WARNING]
> LunaticChat は Paper / Folia サーバーのみをサポートしています．Spigot および BungeeCord はサポートしておらず，今後サポートする予定もありません．Spigot 環境では [LunaChat の fork](https://github.com/f1w3/LunaChat) の利用を推奨します．

## 機能

- 1 対 1 のダイレクトメッセージ機能 (`/tell`，`/msg`)
- クイックリプライ機能 (`/reply`)
- ローマ字から日本語への変換
- チャンネルチャット機能
- マルチプラットフォーム対応 (Paper，Folia，Velocity)

## Velocity 連携

LunaticChat は Velocity プロキシ配下の複数の Paper / Folia サーバー間でグローバルチャットを中継できます．Velocity プロキシと各バックエンドサーバーの両方にプラグインを導入することで，サーバーをまたいだチャットが可能になります．

Paper と Velocity の互換性は，プラグインのバージョンではなく内部の **プロトコルバージョン** のみで判定されます．プロトコルバージョンが非互換な Paper / Velocity ビルドはチャットの中継を拒否するため，両者は常に揃えて更新してください．

対応する組み合わせや設定の詳細は [ドキュメント](https://lc.m1sk9.dev) を参照してください．

## ライセンス

LunaticChat は [GNU General Public License v3.0](./LICENSE) のもとで公開されています．

<sub>
    ® 2026 m1sk9
    <br/>
    LunaticChat は Mojang Studios または Microsoft とは関係ありません．
</sub>

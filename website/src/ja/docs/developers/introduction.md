---
layout: doc
---

# はじめに

このガイドは LunaticChat の設計・アーキテクチャを解説する開発者ガイドになります．

プレイヤー・サーバ管理者は [ドキュメント/リファレンス](/ja/docs/getting-started) を参照してください．

::: tip 対象バージョン

このガイドの設計・アーキテクチャは [Paper/Folia: v1.2.2](https://github.com/m1sk9/LunaticChat/releases/tag/paper%2Fv1.2.2), [Velocity: v1.1.0](https://github.com/m1sk9/LunaticChat/releases/tag/velocity%2Fv1.1.0) 時点で記述しています．

:::

## モジュール構成

LunaticChat のモジュール構成は次の通りです．

設計の全体像は [設計/アーキテクチャ](/ja/docs/developers/architecture) を参照してください．

| モジュール | 役割 |
|-----------|------|
| `engine` | プラットフォーム非依存のコア |
| `platform-paper` | Paper / Folia プラグイン本体 |
| `platform-velocity` | Velocity プロキシプラグイン |

## ガイド一覧

- [設計/アーキテクチャ](/ja/docs/developers/architecture) — アーキテクチャの全体像
  - [engine - 共通カーネル](/ja/docs/developers/engine)
  - [platform-paper - Paper / Folia プラグイン本体](/ja/docs/developers/platform-paper)
  - [platform-velocity - Velocity プラグイン本体 (プロキシ中継)](/ja/docs/developers/platform-velocity)
- [ビルド・リリース・バージョニング](/ja/docs/developers/resource) — リリースフローとバージョニング

- [LunaChat の後継 "LunaticChat" を開発した話 - m1sk9 (Zenn)](https://zenn.dev/m1sk9/articles/adb6c0a7fa7bd2) — Null安全やコルーチン活用、キャッシュシステム導入など (外部サイト)

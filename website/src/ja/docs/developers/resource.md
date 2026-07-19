---
layout: doc
---

# ビルド・リリース・バージョニング

Gradle マルチモジュール構成で，engine を共有しつつ Paper / Velocity を独立した成果物としてビルド・リリースします．

## ビルド構成

- ルート `build.gradle.kts` — Kotlin 2.4.0 + serialization / Shadow / ktlint / dokka を管理．JVM ターゲットは **JVM_25**．テストは JUnit Platform + jacoco で，共通テスト依存を全モジュールへ注入
- `engine` — コアライブラリ (serialization / coroutines / ktor) を `api()` で公開しプラットフォームへ伝播．Adventure は `compileOnly`．Shadow を持たない純ライブラリ
- `platform-paper` — `version = paperVersion`．`api(project(":engine"))`．paper-api を `compileOnly`，KAML + kotlin-reflect を `implementation`．成果物は **`LunaticChat-<ver>.jar`** (classifier なし，`jar` は無効化)
- `platform-velocity` — `version = velocityVersion`．`api(project(":engine"))`．velocity-api を `compileOnly` + `annotationProcessor` (`@Plugin` 処理用)．成果物は **`LunaticChat-<ver>-velocity.jar`** (classifier で区別)
- `dokka` — engine/paper/velocity を集約し HTML に README を include

両プラットフォームの `processResources` は git short hash と `isNightly` から `version` / `gitCommitHash` / `channel` を算出し，`paper-plugin.yml` / `velocity-plugin.json` と `build-info.properties` にトークン展開します．

## 独立バージョニング

```properties
# gradle.properties
paperVersion=1.2.2
velocityVersion=1.1.0
```

Paper と Velocity は別々のバージョン番号を持ち，独立にリリースできます．**互換性を数値バージョンではなく engine 共有の [`ProtocolVersion`](/ja/docs/developers/engine#バージョニング戦略-protocolversion) で保証している**ため，更新頻度の異なる 2 プラットフォームをそれぞれのペースでバンプ・公開できるからです．ワイヤ形式は JSON + `ignoreUnknownKeys` で前方互換，プロトコルの MAJOR 一致 + MINOR 範囲チェックで後方互換をコントロールします．

## リリースワークフロー

タグのパターンでリリース対象が切り替わります．

| ワークフロー | トリガタグ | ビルド対象 | バージョン検証 |
|-------------|-----------|-----------|---------------|
| `release.yaml` | `v*` | Paper + Velocity 両方 | gradle.properties から両バージョン抽出 |
| `release-paper.yaml` | `paper/v*` | Paper のみ | タグと `paperVersion` の一致を必須検証 |
| `release-velocity.yaml` | `velocity/v*` | Velocity のみ | タグと `velocityVersion` の一致を必須検証 |

- 共通フロー: `validate` (既存リリースの重複チェック) → `build` (mise + Gradle setup, `shadowJar`) → `release` (`gh release create --draft` + Modrinth 公開)
- 個別ワークフロー (paper / velocity) はタグと `gradle.properties` の厳密一致を要求する点が `release.yaml` と異なる
- Modrinth の game-versions は Paper=`26.1.x` (loader: paper, folia)，Velocity=`1.21.x` + `26.1.x` (loader: velocity)

## CI

`ci.yaml` は main への push / PR / 手動実行で動きます．

- `build_plugin` — ktlintCheck → test + jacocoTestReport → Codecov アップロード → nightly shadowJar (`-PisNightly=true`) → artifact 保持
- `build_dokka` / `deploy_dokka` — Dokka 生成 → GitHub Pages デプロイ (main push のみ)
- `build_docs` — `website/` を bun で format/lint/build → Cloudflare Workers (wrangler) へデプロイ (main push のみ)

## 開発環境

- `mise.toml` — bun / java zulu-25 (`JVM_25` と整合)
- `x` — bash 製のデバッグサーバースクリプト．`./x <action> <platform> [--stable]` で start/stop/log/clean/rcon/help．`--stable` 省略時は nightly ビルド．`velocity` 指定時は **1 Velocity + 2 Paper** を立ち上げ，サーバー間チャット中継を実地検証できる
- `docker/` — paper / velocity / folia の 3 環境に `compose.yaml` (`itzg/minecraft-server:java25` 等)．velocity.toml は `bungee-plugin-message-channel=true` でプラグインメッセージを有効化

## 関連

- [設計概要](/ja/docs/developers/architecture)
- [はじめに](/ja/docs/developers/introduction)

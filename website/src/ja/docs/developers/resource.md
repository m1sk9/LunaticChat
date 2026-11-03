---
layout: doc
---

# ビルド・リリース・バージョニング

Gradle マルチモジュール構成で，engine を共有しつつ Paper / Velocity を独立した成果物としてビルド・リリースします．

## ビルド構成

- ルート `build.gradle.kts` — Kotlin 2.4.0 + serialization / Shadow / ktlint / dokka を管理．JVM ターゲットは **JVM_25**．テストは JUnit Platform + jacoco で，共通テスト依存を全モジュールへ注入
- `engine` — コアライブラリ (serialization / coroutines / ktor) を `api()` で公開しプラットフォームへ伝播．Adventure は `compileOnly`．Shadow を持たない純ライブラリ
- `platform-paper` — ルートの `version.txt` から読む `version`．`api(project(":engine"))`．paper-api を `compileOnly`，KAML + kotlin-reflect を `implementation`．成果物は **`LunaticChat-<ver>.jar`** (classifier なし，`jar` は無効化)
- `platform-velocity` — `platform-velocity/version.txt` から読む `version`．`api(project(":engine"))`．velocity-api を `compileOnly`．成果物は **`LunaticChat-<ver>-velocity.jar`** (classifier で区別)
- `dokka` — engine/paper/velocity を集約し HTML に README を include

両プラットフォームのバージョンは `isNightly` から導出されます．`-PisNightly=true` を付けるとベースバージョンに `-nightly.<git short hash>` が付き (`LunaticChat-1.3.0-nightly.44132f3.jar`)，開発ビルドを同じベースバージョンのリリースと取り違えることがなくなります．フラグを付けない場合は `version.txt` の中身そのままで，リリースワークフローはこちらをビルドします．

`processResources` はこの `version` と `gitCommitHash` / `channel` を `paper-plugin.yml` / `velocity-plugin.json` と `build-info.properties` にトークン展開します．

## 独立バージョニング

```text
version.txt                    # Paper:    1.7.0
platform-velocity/version.txt  # Velocity: 1.4.0
```

Paper と Velocity は別々のバージョン番号を持ち，独立にリリースできます．**互換性を数値バージョンではなく engine 共有の [`ProtocolVersion`](/ja/docs/developers/engine#バージョニング戦略-protocolversion) で保証している**ため，更新頻度の異なる 2 プラットフォームをそれぞれのペースでバンプ・公開できるからです．ワイヤ形式は JSON + `ignoreUnknownKeys` で前方互換，プロトコルの MAJOR 一致 + MINOR 範囲チェックで後方互換をコントロールします．

## リリースワークフロー

リリースは [release-please](https://github.com/googleapis/release-please) が準備し，その PR のマージで始まります．1 つのタグで 1 つのプラットフォームをリリースし，Paper と Velocity の Release PR は別々に作られます．

| ワークフロー | トリガ | 役割 |
|-------------|--------|------|
| `release-please.yaml` | `main` への push | Release PR を作成・更新する．マージされたらタグを push する |
| `release-pr-checks.yaml` | Release PR | プロトコルバージョン，Paper↔Velocity の互換性，リリースノート，Modrinth の game-versions を検査する．ブランチ保護で `gate` ジョブを必須にする |
| `release-paper.yaml` | タグ `vX.Y.Z` | Paper のみ |
| `release-velocity.yaml` | タグ `velocity/vX.Y.Z` | Velocity のみ |

- `version.txt` (Paper) と `platform-velocity/version.txt` (Velocity) は release-please のものです．バージョンは，前回のリリース以降にマージされたコミットの種類 (Conventional Commits) で決まります．`fix` は PATCH，`feat` は MINOR，破壊的変更は MAJOR です
- リリースノートは Release PR の中で手で書きます (`/create-release-note`)．検査がノートを見つけるまで，PR はマージできません．`CHANGELOG.md` は v1.7.0 で更新を止めており，このサイトのリリースノートが変更履歴です
- タグは release-please ではなく自前のジョブが，annotated タグとして作ります．リリースのタグは GPG 署名しません．署名には，公開鍵を置くための通常の GitHub アカウントがもう 1 つ必要で (GitHub App は鍵を登録できません)，その手間に見合わないと判断しました．push には GitHub App のトークンを使います．`GITHUB_TOKEN` で push したタグでは，リリースのワークフローが起動しないためです
- Paper が接頭辞なしの `vX.Y.Z` を使うのは，配布済みの Paper 版のアップデートチェッカーがこの形式しか解釈できないため
- v1.4.0 までは `vX.Y.Z` タグで両方をリリースしていた (`v1.0.0`，`v1.3.0`，`v1.4.0`)．Paper 単独のリリースには `paper/vX.Y.Z` を使ったものもある．これらのタグはそのまま残している
- どちらのリリースワークフローも共通の `_release.yaml` を呼び出す: `validate` (タグの形式，`version.txt` との一致．公開済みの Release があれば止める) → `build` (mise + Gradle setup，`shadowJar`) → `verify` (JAR のプロトコル定数がソースと一致し，リリースノートがサイトに公開されている) → `release` (draft の GitHub Release → Modrinth → 公開) → `verify-rollout` (失敗したら Issue を作る)．再実行すると，前回の実行が残した draft を再利用する
- GitHub Release の本文と Modrinth の changelog は，どちらも英語のリリースノートのページから生成する．Modrinth はスクリプト (`.github/scripts/publish-modrinth.sh`) が API で直接呼び，サードパーティの Action にトークンを渡さない
- Velocity のリリースは `--latest=false` を付けて公開し，リポジトリの Latest が Paper のリリースのままになるようにする
- Modrinth の game-versions は，呼び出し側の各ワークフローにパターンで書き (Paper=`26.3.x` (loader: paper, folia)，Velocity=`1.21.x` + `26.1.x` + `26.2.x` + `26.3.x` (loader: velocity))，Modrinth が持つ具体的なバージョンに展開して使う

## CI

`ci.yaml` は main への push / PR / 手動実行で動きます．

- `build_plugin` — ktlintCheck → test + jacocoTestReport → Codecov アップロード → nightly shadowJar (`-PisNightly=true`) → `LunaticChat-paper-<sha>` / `LunaticChat-velocity-<sha>` としてプラットフォームごとに artifact 保持 (JAR は artifact 直下)
- `build_dokka` / `deploy_dokka` — Dokka 生成 → GitHub Pages デプロイ (main push のみ)
- `build_docs` — `website/` を bun で format/lint/build → Cloudflare Workers (wrangler) へデプロイ (main push のみ)

## 開発環境

- `mise.toml` — bun / java zulu-25 (`JVM_25` と整合)
- `x` — bash 製のデバッグサーバースクリプト．`./x <action> <platform> [--stable]` で start/stop/log/clean/rcon/help．`--stable` 省略時は nightly ビルド．`velocity` 指定時は **1 Velocity + 2 Paper** を立ち上げ，サーバー間チャット中継を実地検証できる
- `docker/` — paper / velocity / folia の 3 環境に `compose.yaml` (`itzg/minecraft-server:java25` 等)．velocity.toml は `bungee-plugin-message-channel=true` でプラグインメッセージを有効化
- サーバーのバージョンは `x` が `build.gradle.kts` の `paper-api` / `velocity-api` から導出し，`PAPER_MC_VERSION` / `PAPER_BUILD` / `VELOCITY_VERSION` として Compose に渡す．そのためデバッグ環境は常にプラグインのコンパイル対象と同じビルドで動く．`docker compose` を直接叩くと `./x` を経由するよう促すメッセージで失敗する．Folia のビルド番号だけは Paper と別系列のため手動固定

## 関連

- [設計概要](/ja/docs/developers/architecture)
- [はじめに](/ja/docs/developers/introduction)

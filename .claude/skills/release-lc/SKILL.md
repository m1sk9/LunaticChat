---
description: CHANGELOG.md を基にリリース対象を決め，リリース前チェック，署名付きタグの作成と push，ワークフロー・GitHub Release・Modrinth・ドキュメントサイトへのロールアウト完了までを監視する．
disable-model-invocation: true
argument-hint: [paper|velocity|both]
allowed-tools: Bash(./gradlew *), Bash(git *), Bash(gh *), Bash(curl *), Bash(jq *), Bash(javap *), Bash(shasum *), Read, Grep, Glob
---

# Release LunaticChat

引数: `$ARGUMENTS`

リリースは「タグを push したら終わり」ではない．**GitHub Release が公開され，Modrinth に listed で並び，リリースノートのリンク先が生きている**ところまで確認して完了とする．

副作用のある操作 (タグ作成・push・draft の公開・ワークフロー再実行) は，必ず直前にユーザーの確認を得ること．

## 1. リリース対象の決定

1. `gradle.properties` から `paperVersion` / `velocityVersion` を読む
2. `CHANGELOG.md` の先頭エントリを読む．見出しがそのままタグ名になる (`### vX.Y.Z` → `vX.Y.Z`，`#### Velocity: vA.B.C` / `### Velocity: vA.B.C` → `velocity/vA.B.C`)
3. 既存リリースを確認する:
   ```
   gh release list --limit 20
   git tag --list 'v*' 'velocity/v*' --sort=-creatordate
   ```
4. 未リリースのバージョンから対象を決める．1 タグ = 1 プラットフォームで，両方を出す場合はタグを 2 つ作る．引数が与えられていればそれを優先し，推定と食い違えば指摘する:

   | プラットフォーム | タグ | ワークフロー | タグメッセージ |
   |-----------------|------|-------------|---------------|
   | Paper | `v{paperVersion}` | `release-paper.yaml` (`Release Paper`) | `Paper v{paperVersion}` |
   | Velocity | `velocity/v{velocityVersion}` | `release-velocity.yaml` (`Release Velocity`) | `Velocity v{velocityVersion}` |

   - `both` は上の 2 行を両方実行する．片側が既にリリース済みのバージョンなら，その側はリリースしない (Modrinth で同一バージョンの重複公開になる)
   - Paper を `paper/vX.Y.Z` でタグ付けしてはならない．どちらのワークフローも起動せず，配布済みの `UpdateChecker` も認識できない
5. 決定内容 (形態・タグ名・両バージョン) をユーザーに提示する

## 2. リリース前チェック

すべて通るまでタグの提案に進まない．失敗したら原因を報告し，直せるものは修正方針を提案する．

### リポジトリ状態

- `main` ブランチにいて，working tree がクリーンであること
- `git fetch origin` 後，`HEAD` が `origin/main` と一致すること (未 push のコミットをタグ付けしない)
- タグがローカル・リモートの両方に存在しないこと (`git ls-remote --tags origin {tag}`)
- `HEAD` の CI (`CI` ワークフロー) が成功していること:
  ```
  gh run list --workflow ci.yaml --commit $(git rev-parse HEAD) --json status,conclusion,url
  ```

### ビルドとテスト

```
./gradlew ktlintCheck
./gradlew test
./gradlew :platform-paper:shadowJar :platform-velocity:shadowJar
```

対象外のプラットフォームも含めて両方ビルドする (engine の変更がもう一方を壊していないことの確認を兼ねる)．

### プロトコル

前回リリースのタグ (対象プラットフォームごと) からの差分に `engine/src/main/kotlin/dev/m1sk9/lunaticChat/engine/protocol/` の変更があるか確認する:

```
git diff --stat {前回タグ}..HEAD -- engine/src/main/kotlin/dev/m1sk9/lunaticChat/engine/protocol/
```

変更がある場合:

- `ProtocolVersion.kt` が CLAUDE.md の規則どおりに上がっていること (変更の種類に対して PATCH / MINOR / MAJOR が妥当か)
- `ProtocolBackwardCompatibilityTest` にスナップショットが追加されていること
- `/test-integration` の実行を推奨する (未実行ならユーザーに確認する)
- MINOR なら Velocity を先に出す必要がある．`paper` 単独リリースで Velocity 側が未対応なら止める

### リリースノート

- `CHANGELOG.md` に対象バージョンのエントリがあること
- `website/src/changelog/{paper,velocity}/v{version}.md` と `website/src/ja/changelog/...` が対象の全プラットフォーム分あり，`config/en.ts` / `config/ja.ts` のサイドバーに登録されていること．無ければ `/create-release-note` を先に実行するよう伝えて止まる
- ページの Download リンクのタグが §1 の表と一致すること (Paper は `releases/tag/v{paperVersion}`，Velocity は `releases/tag/velocity/v{velocityVersion}`)
- **デプロイ済み**であること．Release と Modrinth の changelog はサイトを指すため，未デプロイだとリンク切れで公開される:
  ```
  curl -s -o /dev/null -w '%{http_code}\n' https://lc.m1sk9.dev/changelog/paper/v{paperVersion}
  curl -s -o /dev/null -w '%{http_code}\n' https://lc.m1sk9.dev/ja/changelog/paper/v{paperVersion}
  ```
  サイトは `main` への push 時に CI の `build_docs` ジョブが Cloudflare Workers へデプロイする．404 なら CI の完了を待つ

### Modrinth のメタデータ

対象ワークフローの `MODRINTH_*_GAME_VERSIONS` / `MODRINTH_MC_VERSION` が，`platform-paper/build.gradle.kts` の `paper-api` (と `platform-velocity/build.gradle.kts` の `velocity-api`) が示す Minecraft バージョンと食い違っていないか確認する．Paper の対応バージョンを上げたリリースで更新漏れが起きやすい．

## 3. タグの作成と push

チェック結果の要約を示し，ユーザーの確認を得てから実行する．既存タグと同じく署名付き annotated タグにする:

```
git tag -s {tag} -m "{タグメッセージ}"
git push origin {tag}
```

両方を出す場合は 2 つのタグを作ってから 1 回で push する．プロトコルの MINOR を上げたリリースでは Velocity が先に出ている必要があるので，Velocity のタグを先に書く:

```
git push origin velocity/v{velocityVersion} v{paperVersion}
```

2 つのワークフローが並行して走る．以降の §4〜§6 はタグごとに行う．

GPG / SSH のエラーが出たら再試行せずに止まり，ユーザーに伝える．

## 4. ワークフローの監視

1. 起動を確認する (数十秒かかることがある):
   ```
   gh run list --workflow {workflow-file} --limit 3 --json databaseId,headBranch,status,event,url
   ```
   `headBranch` がタグ名の run を選ぶ．1 分待っても現れなければ `git ls-remote --tags origin {tag}` で push を確かめる
2. 完了まで追う．長時間かかるので Bash の `run_in_background` で実行し，完了通知を待つ:
   ```
   gh run watch {run-id} --exit-status
   ```
3. 失敗したらジョブを切り分ける:
   ```
   gh run view {run-id} --log-failed
   ```
   - `validate` … タグと `gradle.properties` の不一致，または同名 Release が既に存在する
   - `build` … ローカルでは通っているので CI 環境固有の問題を疑う
   - `release` … GitHub Release 作成か Modrinth 公開の失敗．**GitHub Release 作成後に Modrinth で落ちた場合，再実行は `validate` の重複チェックで止まる**．Release とタグの状態を示したうえで，対処 (draft Release を削除して再実行する，Modrinth へ手動公開する，など) をユーザーに提案する
   - 再実行 (`gh run rerun`) やタグの付け直しは提案に留め，承認を得てから行う

## 5. draft の公開

ワークフローは Release を **draft** で作る．

```
gh release view {tag} --json isDraft,name,assets,url
```

- 添付ファイルが期待どおりであること:
  - Paper: `LunaticChat-{paperVersion}.jar`
  - Velocity: `LunaticChat-{velocityVersion}-velocity.jar`
- 添付 JAR をスクラッチパッドへダウンロードし，埋め込まれたプロトコル定数が `HEAD` の `ProtocolVersion.kt` と一致することを確かめる (別コミットからビルドされていないことの確認):
  ```
  gh release download {tag} --dir {scratchpad}/release-{tag}
  javap -constants -cp {jar} dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
  ```
- 問題なければ公開をユーザーに確認し，公開する:
  ```
  gh release edit v{paperVersion} --draft=false
  gh release edit velocity/v{velocityVersion} --draft=false --latest=false
  ```
  GitHub は公開したリリースを既定で Latest にする．リポジトリの Latest は常に Paper のリリースにしておくため，Velocity では必ず `--latest=false` を付ける．両方を出す場合は Paper を後に公開する

## 6. ロールアウトの確認

以下をすべて確認し，表にして報告する．

| 確認先 | 方法 | 期待値 |
|--------|------|--------|
| GitHub Release | `gh release view {tag} --json isDraft,assets` | `isDraft: false`，JAR 添付済み |
| Latest | `gh api repos/m1sk9/LunaticChat/releases/latest --jq .tag_name` | 最新の Paper のタグ (`v{paperVersion}` など)．`velocity/v*` になっていたら `gh release edit v{paperVersion} --latest` で戻す |
| Modrinth (Paper) | 下記 API | `version_number == {paperVersion}`，`status == listed`，loaders に `paper` と `folia`，game_versions にワークフローの値 |
| Modrinth (Velocity) | 下記 API | `version_number == {velocityVersion}-velocity`，`status == listed`，loaders に `velocity` |
| リリースノート | `curl` で en / ja の changelog ページ | `200` |
| Release 本文のリンク | 本文中の `lc.m1sk9.dev` リンクを `curl` | `200` |
| リリースノートの Download リンク | ページ内の GitHub / Modrinth リンクを `curl` | `200` |

Modrinth は公開 API をトークンなしで参照できる:

```
curl -s https://api.modrinth.com/v2/project/lunaticchat/version | jq '.[:4][] | {version_number, status, loaders, game_versions, date_published, file: .files[0].filename}'
```

Modrinth への反映は数分遅れることがある．見つからない場合は数分おいて再確認し，それでも無ければワークフローの `release` ジョブのログを確認する．

## 7. 完了報告

- リリースしたタグ・バージョン・Release の URL・Modrinth の URL
- §6 の確認表
- 未完了の項目があれば，その内容と次にユーザーが取るべき操作

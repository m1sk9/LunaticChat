---
description: Release PR (release-please) の状態を確認し，リリースノートの準備，マージ，タグ・ワークフロー・GitHub Release・Modrinth・ドキュメントサイトへのロールアウトの監視，失敗時の対処までを案内する．初期設定の確認にも使う．
disable-model-invocation: true
argument-hint: [paper|velocity|both|status|setup]
allowed-tools: Bash(git *), Bash(gh *), Bash(curl *), Bash(jq *), Bash(.github/scripts/*), Read, Grep, Glob
---

# Release LunaticChat

引数: `$ARGUMENTS`

リリースはほぼ自動で進む．人間がやるのは **リリースノートを書くこと** と **Release PR をマージすること** の 2 つだけで，それ以降 (タグ，ビルド，検証，Modrinth，GitHub Release) は Actions が行う．このスキルは，その各段階が正しく進んでいるかを確かめ，止まったときに原因を切り分ける．

```
main に fix/feat/依存更新が入る
  → release-please が Release PR を作る (Paper と Velocity は別々)
  → Release PR のチェック (release-pr-checks.yaml)        ← 失敗するとマージできない
  → 人間がリリースノートを書く (/create-release-note)
  → 人間が Release PR をマージする
  → release-please.yaml の tag ジョブがタグを push
  → release-paper.yaml / release-velocity.yaml (_release.yaml)
       validate → build → verify → release (draft → Modrinth → 公開) → verify-rollout
```

副作用のある操作 (Release PR のマージ，ワークフローの再実行，手動のタグ作成・push，ラベルの付け替え) は，必ず直前にユーザーの確認を得ること．

引数が `setup` なら §7 だけを実行する．`status` なら §1 だけを実行する．

## 1. 状態の確認

```
gh pr list --state open --search 'head:release-please--' --json number,title,headRefName,statusCheckRollup
gh release list --limit 6
gh run list --workflow ci.yaml --branch main --limit 3
```

- Release PR が無い: `main` に releasable なコミットが無いか，release-please のワークフローが失敗している (`gh run list --workflow release-please.yaml --limit 3`)
- Release PR のタイトルが提案するバージョンを，PR のブランチの `version.txt` (Paper) / `platform-velocity/version.txt` (Velocity) で確かめる．期待より低い・高い場合は，コミットの種類 (`fix` は PATCH，`feat` は MINOR，`!` や `BREAKING CHANGE` は MAJOR) を見直す．**バージョンを手で書き換えない** (release-please が上書きする)．MINOR 以上にしたい場合は，コミットの種類を正す．`Release-As: X.Y.Z` のフッターでバージョンを指定することもできるが，release-please はコミットが変更したパスでパッケージに振り分けるので，対象のパス (Paper はルート，Velocity は `platform-velocity/`) を変更するコミットに付けること (パスを変更しない空コミットが振り分けられるかは未確認)
- `engine/` だけを変えた変更は Velocity に振り分けられない．Velocity の JAR にも含まれる変更 (プロトコル，`kotlinx-serialization` の更新) で Velocity もリリースしたいときは，Velocity 側 (`platform-velocity/`) の変更を同じ PR に含める
- Paper と Velocity の両方に PR がある場合，プロトコルが MINOR 上がるなら Velocity を先にマージする (チェックのコメントに出る)．MAJOR なら同時にマージする

## 2. リリースノートの準備

ノートが無い間，Release PR には，手順を英語で案内するコメントが自動で付く (`remind-release-notes.sh`．検査のたびに同じコメントを更新し，ノートが揃うと消える)．

`/create-release-note` を実行し，Release PR のブランチにノート (英日のページ・サイドバー) をコミットして push する．書いている間は他の PR を `main` にマージしない (マージすると release-please が PR のブランチを作り直し，ノートのコミットが消える)．

## 3. マージ前のチェック

```
gh pr checks {PR 番号}
```

必須チェックは `gate` ジョブだけで，中身は次の 4 つ．失敗したら該当のスクリプトを手元で実行して原因を見る (どれも `.github/scripts/` にあり，リポジトリのルートで動く):

| ジョブ | 見ていること | 手元での確認 |
|--------|-------------|-------------|
| `protocol` | 通信形式 (`PluginMessage.kt` / `PluginMessageChannel.kt`) が前回のリリースから変わったのに，`ProtocolVersion` が上がっていない，または `ProtocolBackwardCompatibilityTest` にスナップショットが無い | `.github/scripts/check-protocol.sh` |
| `compat` | 両 JAR のプロトコル定数から，HEAD ↔ HEAD，HEAD Velocity ↔ 最新リリースの Paper，最新リリースの Velocity ↔ HEAD Paper の互換性を判定 | `./gradlew :platform-paper:shadowJar :platform-velocity:shadowJar` のあと `.github/scripts/check-compat.sh {paper.jar} {velocity.jar}` |
| `notes` | 英日のリリースノート，サイドバー登録，Download リンクのタグ | `.github/scripts/check-release-notes.sh {タグ...}` |
| `modrinth-meta` | `paper-api` が示す Minecraft バージョンと，`release-paper.yaml` の `modrinth-mc-version` / `modrinth-game-versions` の一致 (Paper のみ) | `.github/scripts/check-modrinth-meta.sh` |

- `compat` の通知 (notice) は失敗ではない．「Velocity を先に」「Paper を先に更新」などのデプロイ順序の制約を示す
- `modrinth-meta` が失敗したら，`release-paper.yaml` の入力を直す (Paper が新しい Minecraft に対応するとき，古いバージョンのサポートを打ち切るかも判断する)
- プロトコルの変更が MINOR 以上のときは `/test-integration` の実行を勧める (実機のハンドシェイクは自動化されていない)

## 4. マージ

チェックがすべて通り，ノートの内容をユーザーが確認したら，ユーザーの確認を得てマージする (squash)．

```
gh pr merge {PR 番号} --squash
```

両方出す場合は §1 の順序に従う．マージ後は何もしなくてよい．

## 5. ロールアウトの監視

マージ後に動くものを順に確かめる．

1. **タグ**: `release-please.yaml` の `tag` ジョブ (数十秒)．annotated タグ (署名なし) ができ，Release PR のラベルが `autorelease: tagged` になる:
   ```
   gh run list --workflow release-please.yaml --limit 2
   git fetch --tags && git tag -n1 {タグ}
   gh pr view {PR 番号} --json labels --jq '.labels[].name'
   ```
   タグのメッセージが `Paper vX.Y.Z` / `Velocity vX.Y.Z` で，ラベルに `autorelease: tagged` があること
2. **リリースワークフロー**: タグの push で `release-paper.yaml` / `release-velocity.yaml` が起動する:
   ```
   gh run list --workflow release-paper.yaml --limit 3
   gh run watch {run-id} --exit-status
   ```
   `gh run watch` は長いので Bash の `run_in_background` で実行する．`verify` ジョブは，サイトのリリースノートが公開されるまで最大 20 分待つ (マージ後の docs のデプロイ待ち)
3. **ロールアウトの検証**: `verify-rollout` ジョブが GitHub Release (draft でない，Latest が Paper のまま)，Modrinth (`listed`，loaders，game versions)，サイトのリンクを確認する．失敗すると Issue が起票される

## 6. 失敗時の対処

原因を示して対処を提案する．再実行 (`gh run rerun`) とタグの付け直しは，ユーザーの承認を得てから行う．

```
gh run view {run-id} --log-failed
```

### `release-please.yaml`

| 症状 | 原因と対処 |
|------|-----------|
| タグの push で失敗 | App の権限不足 (Contents: write)，またはタグ保護ルール．App のインストールと権限を確認する |
| Release PR が作られず，ログに `untagged, merged release PRs` | 直前の Release PR のラベルが `autorelease: pending` のまま．タグが無ければ §6 の手動タグを作り，ラベルを `autorelease: tagged` に付け替える |
| App トークンの取得で失敗 | `CLIENT_ID` / `PRIVATE_KEY` が無い，または App がこのリポジトリにインストールされていない |

### `_release.yaml`

| ジョブ | 症状 | 対処 |
|--------|------|------|
| `validate` | タグと `version.txt` が不一致 | Release PR のブランチが作り直された可能性．タグが指すコミットの `version.txt` を確認する |
| `validate` | `already published` | 同じタグの Release が公開済み．重複なので止まるのが正しい |
| `verify` | JAR のプロトコル定数がソースと不一致 | 別のコミットからビルドされた JAR．タグが指すコミットとビルドの対象を確認する |
| `verify` | サイトのページが 200 にならずタイムアウト | `ci.yaml` の `build_docs` (Cloudflare へのデプロイ) が `main` で失敗していないか確認する．直ったらワークフローを再実行する |
| `release` | Modrinth の公開で失敗 | draft の Release は残る．**再実行して構わない**: draft は再利用され，Modrinth に同じバージョンがあれば公開をスキップする．API のエラー本文はログに出る |
| `release` | 最後の公開で失敗 | Modrinth には出ているが Release は draft のまま．再実行すると公開まで進む |
| `verify-rollout` | Modrinth が `listed` にならない | 反映の遅れなら数分後に再実行する．それでも出ない場合は Modrinth の画面でバージョンの状態を確認する |
| `verify-rollout` | Latest が Velocity のタグになっている | `gh release edit {最新の Paper のタグ} --latest` で戻す (承認を得てから) |

### 手動でタグを作る (最終手段)

`tag` ジョブが使えない場合に限る．annotated タグで，Paper は `vX.Y.Z`，Velocity は `velocity/vX.Y.Z`．タグはマージされたコミットに付ける:

```
git switch main && git pull --ff-only
git tag -a {タグ} -m "{Paper|Velocity} {バージョン}" {マージコミットの SHA}
git push origin {タグ}
```

push したら Release PR のラベルを付け替える:

```
gh pr edit {PR 番号} --remove-label 'autorelease: pending' --add-label 'autorelease: tagged'
```

## 7. 初期設定 (一度だけ)

自動化が動くために必要な，リポジトリ外の設定．`setup` のときは，できる範囲で確かめて結果を表にして報告する．

| 項目 | 内容 | 確認 |
|------|------|------|
| GitHub App | LunaticChat にインストール済み．権限は Contents / Pull requests / Issues の Read & write | 設定画面 (CLI からは確認できない) |
| Secrets `CLIENT_ID` / `PRIVATE_KEY` | App の Client ID と秘密鍵 | `gh secret list` |
| 必須チェック | `main` のルールセット `branch-protect` の必須チェックに `gate` (ワークフロー `Release PR checks` のジョブ) を足す．マージして `gate` が `main` 向けの PR で報告されるようになってから足すこと (先に足すと，ほかの PR が `gate` を待ち続ける) | `gh api repos/m1sk9/LunaticChat/rules/branches/main --jq '.[] \| select(.type == "required_status_checks") \| .parameters.required_status_checks'` |
| タグのルールセット | `protect-tag` は削除・更新・non-fast-forward だけを禁じ，作成は禁じていない．`creation` のルールを足すと `tag` ジョブの push が失敗するので足さない | `gh api repos/m1sk9/LunaticChat/rulesets/11712454 --jq '[.rules[].type]'` |
| 既存の Secrets | `MODRINTH_TOKEN` / `MODRINTH_PROJECT_ID` | `gh secret list` |

秘密鍵や Secrets の値を読み出したり，会話に出力したりしない．

## 8. 完了報告

- リリースしたタグ・バージョン・GitHub Release の URL・Modrinth の URL
- §5 の確認結果 (タグ，ワークフロー，ロールアウトの検証)
- 未完了の項目があれば，その内容と次にユーザーが取るべき操作

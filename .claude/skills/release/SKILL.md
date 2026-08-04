---
description: リリースの準備とタグ作成を行う。引数に paper, velocity, both のいずれかを指定。
disable-model-invocation: true
argument-hint: [paper|velocity|both]
allowed-tools: Bash(./gradlew *), Bash(git *), Bash(gh *), Read, Grep, WebFetch
---

# Release

引数: `$ARGUMENTS`

## 手順

1. `gradle.properties` から `paperVersion` と `velocityVersion` を読み取る
2. 引数に応じてリリース対象を決定:
   - `paper` → Paper のみリリース
   - `velocity` → Velocity のみリリース
   - `both` → 両方同時リリース
   - 引数なし → ユーザーに確認する
3. リリース前チェック:
   - `./gradlew ktlintCheck` が通ること
   - `./gradlew test` が通ること
   - 対象プラットフォームの `shadowJar` がビルドできること
   - git working tree がクリーンであること
   - 該当タグがまだ存在しないこと
4. チェックがすべて通ったら、タグの作成をユーザーに提案する:
   - `paper` → `git tag paper/v{paperVersion}`
   - `velocity` → `git tag velocity/v{velocityVersion}`
   - `both` → `git tag v{paperVersion}` (Paper/Velocity バージョンが異なる場合は注意を促す)
5. タグを push するかユーザーに確認する (`git push origin {tag}`)
6. push 後は「リリース後の監視」に進む

## リリース後の監視

タグを push したら、リリースワークフローが完走するまで見届ける。ワークフローは `validate` → `build` → `release` の 3 ジョブで、`release` ジョブが GitHub Release の作成と Modrinth への公開を行う。

1. ワークフローの起動を確認する:
   ```
   gh run list --limit 5
   ```
   タグ名に対応するワークフロー (`Release Paper` / `Release Velocity` / `Release`) が起動していること。数十秒待っても現れない場合はタグの push 自体が失敗していないか確認する
2. 完了まで追跡する:
   ```
   gh run watch {run-id} --exit-status
   ```
3. 失敗した場合はジョブごとに原因を切り分け、ユーザーに報告する:
   ```
   gh run view {run-id} --log-failed
   ```
   - `validate` で失敗 → タグのバージョンと `gradle.properties` の不一致、または同名 Release が既に存在する
   - `build` で失敗 → リリース前チェックで通っていたはずなので、CI 環境固有の問題を疑う
   - `release` で失敗 → GitHub Release 作成か Modrinth 公開の失敗。Modrinth 側だけ失敗した場合、GitHub Release は既に作られているため再実行すると重複しうる点に注意する
4. 成功したら成果物を確認する:
   ```
   gh release view {tag} --json isDraft,assets,url
   ```
   - **Release は draft で作成される**。JAR が添付されていることを確認したうえで、公開するかユーザーに確認する (`gh release edit {tag} --draft=false`)
   - 添付ファイル名が期待どおりであること (Paper は `LunaticChat-{version}.jar`、Velocity は `LunaticChat-{version}-velocity.jar`)
5. リリースノートのリンク先が存在することを確認する。Release と Modrinth の changelog はどちらもドキュメントサイトを指しているため、対応するページが未デプロイだとリンク切れになる:
   - Paper: `https://lc.m1sk9.dev/changelog/paper/v{paperVersion}`
   - Velocity: `https://lc.m1sk9.dev/changelog/velocity/v{velocityVersion}`
6. Modrinth に該当バージョンが公開されているかユーザーに確認を依頼する (Modrinth のバージョン一覧は API トークンなしでは追えないため、ここは目視確認を頼む)

## 注意事項

- タグの作成と push は必ずユーザーの確認を得てから行うこと
- `both` の場合、Paper と Velocity のバージョンが異なる場合はその旨を明示すること
- プロトコルバージョン (`ProtocolVersion.kt`) の変更がある場合は、後方互換テスト (`ProtocolBackwardCompatibilityTest`) が通っていることを確認すること
- リリースは draft で作られる。ワークフローが成功しても、draft を公開するまでリリースは完了していない
- 失敗したワークフローの再実行 (`gh run rerun`) は、`validate` の「Release が既に存在しないこと」チェックに引っかかる可能性がある。再実行の前に GitHub Release とタグの状態を確認し、対処方針をユーザーに提案すること

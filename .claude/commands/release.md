---
description: リリースの準備とタグ作成を行う。引数に paper, velocity, both のいずれかを指定。
---

# Release Command

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

## 注意事項

- タグの作成と push は必ずユーザーの確認を得てから行うこと
- `both` の場合、Paper と Velocity のバージョンが異なる場合はその旨を明示すること
- プロトコルバージョン (`ProtocolVersion.kt`) の変更がある場合は、後方互換テスト (`ProtocolBackwardCompatibilityTest`) が通っていることを確認すること

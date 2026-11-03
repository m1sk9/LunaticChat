---
description: 開いている Release PR (release-please) の変更から，ドキュメントサイトの英語・日本語リリースノート (website/src/changelog/, website/src/ja/changelog/) を作成する．
disable-model-invocation: true
argument-hint: [paper|velocity|both] [version]
allowed-tools: Bash(git *), Bash(gh *), Bash(bun *), Bash(curl *), Read, Grep, Glob, Edit, Write
---

# Create Release Note

引数: `$ARGUMENTS`

リリースノートは利用者 (サーバー管理者) 向けの要約である．材料はコミット履歴と PR の説明で，このスキルはそれを利用者に見える変化として書き直す．逐語訳・丸写しはしない．

`CHANGELOG.md` は v1.7.0 を最後に更新されない (release-please は `skip-changelog` で動く)．サイトのリリースノートが，新しいリリースの唯一の変更記録である．

## 1. 対象バージョンの決定

リリースは release-please が開く Release PR で決まる．バージョンを自分で決めない．

1. 開いている Release PR を探す (Paper と Velocity で別々の PR になる):
   ```
   gh pr list --state open --search 'head:release-please--' --json number,title,headRefName
   ```
   PR が無い場合 (対象のコミットがまだ `main` に無い) は，その旨を報告して止まる
2. 各 PR のバージョンを，PR のブランチの `version.txt` (Paper) / `platform-velocity/version.txt` (Velocity) から読む．タグは Paper が `vX.Y.Z`，Velocity が `velocity/vA.B.C`
3. 引数があればそれに従う．無ければ，`website/src/changelog/{paper,velocity}/v{version}.md` がまだ存在しないものを対象にする
4. 対象のページが既に存在する場合は上書きせず，差分の追記か作り直しかをユーザーに確認する

### 作業するブランチ

ノートは Release PR のブランチにコミットする．

```
gh pr checkout {PR 番号}
```

release-please は，`main` に releasable なコミット (`feat` / `fix` や `fix(deps)` の依存更新) が入るたびに，この PR のブランチを **force-push で作り直す**．そのとき手で足したコミットは消える．作業の前に `main` との差を確認し，書いている間は他の PR を `main` にマージしないようユーザーに伝える．消えた場合は，このスキルをもう一度実行する．

### 材料を集める

前回のリリースから対象の変更を集める．Paper は `engine/` や共通ファイルも含み，Velocity は `platform-velocity/` だけを見る:

```
git log --format='%h %s%n%b---' {前回のタグ}..HEAD
gh pr view {番号}     # 変更の説明が必要な PR だけ
```

前回のタグは `git tag --list 'v*' --sort=-v:refname | head -1` (Paper)，`git tag --list 'velocity/v*' --sort=-v:refname | head -1` (Velocity) で取る．コミットの本文には変更の Why が書かれているので，利用者に見える結果の説明に使う．

## 2. リンク先

Download リンクはバージョン番号だけで決まる:

| | GitHub | Modrinth |
|--|--------|----------|
| Paper | `https://github.com/m1sk9/LunaticChat/releases/tag/vX.Y.Z` | `https://modrinth.com/plugin/lunaticchat/version/X.Y.Z` |
| Velocity | `https://github.com/m1sk9/LunaticChat/releases/tag/velocity/vA.B.C` | `https://modrinth.com/plugin/lunaticchat/version/A.B.C-velocity` |

## 3. 本文の書き起こし

テンプレートは `website/src/assets/changelog-default.md`．既存ページ (`website/src/changelog/paper/v1.3.0.md` など) の書きぶりに揃える．

### 振り分け

集めた変更を次の見出しに振り分ける．項目の無い見出しは削除する．

| 見出し | 入れるもの |
|--------|-----------|
| `## Security` | 脆弱性の修正．GHSA へのリンクを付け，問題の内容と修正後の挙動を同じ項目にまとめて書く (Changes / Bug Fixes に分けない)．利用者が取るべき対応 (もう一方のプラットフォームも更新する，など) は Notes に書く |
| `## New Features` | 新機能，新コマンド，新しいプラットフォームへの対応 (サポート打ち切りはここに子項目として併記)，プロトコルバージョンの引き上げ |
| `## Improvements` | 性能改善，堅牢性の向上など，利用者の操作が変わらない改善 |
| `## Changes` | 挙動や既定値の変更で，利用者が意識すべきもの |
| `## Bug Fixes` | 不具合修正．`Fixed ...` で始める |
| `## Notes` | 更新時の注意 (デプロイ順序，手動作業，互換性の注意) |

### 書き換えの原則

- 利用者に見えない変更は落とすか一行にまとめる．テスト数の増減，内部リファクタリング，クラス名・例外名だけで説明している項目は `Performed internal cleanup.` に畳む
- 原因の実装詳細 (例: 「`CancellationException` として報告していた」) は書かず，利用者から見えた症状と直った結果を書く
- 設定キーは `` `features.xxx.yyy` `` のようにコード表記のまま残す
- 項目が長い場合は親項目に結論，子項目に補足を置く (既存ページのインデントは 2 スペース)
- プロトコルバージョンの変更は Paper・Velocity 両方のページに書く．PATCH なら「どちらを先に更新してもよい」，MINOR なら「Velocity を先に更新する」ことを必ず添える (`CLAUDE.md` の Protocol Version 節)
- `platform-velocity/` だけを変える項目は Velocity ページへ，`platform-paper/` や共通のものは Paper ページへ．両方に関わる項目 (engine の変更，プロトコル) は両方へ
- 依存の更新だけのリリースは，項目を並べず `## Improvements` に一行で書く (例: `Updated the supported Paper API to build 237.`)．更新が利用者に見えない場合は `Updated dependencies.` に畳む．Paper API の更新で対応する Minecraft バージョンが変わる場合は，`## New Features` に書き，サポートを打ち切るバージョンも併記する
- ドキュメントの該当ページへのリンクは積極的に張る．リンクは英語版は `/docs/...`，日本語版は `/ja/docs/...` の絶対パスで書き，アンカーは実在する見出しから作る (VitePress の slug 規則: 小文字化，空白は `-`，記号除去．日本語見出しはそのまま)．張る前に `Grep` で見出しの存在を確かめる

### 日本語版

- 英語版の直訳ではなく，同じ内容を自然な日本語で書く
- 句読点は `，` `．`，括弧は半角 `()` を使う (`、` `。` `（）` は使わない)
- 見出し (`## New Features` など) と `- Download:` は英語のまま残す (既存ページに合わせる)
- 文末は「〜しました．」「〜できるようになりました．」のですます調で統一する
- 技術用語・固有名詞は英語のまま (Velocity，Paper，JAR など)

## 4. サイドバーへの登録

`website/.vitepress/config/en.ts` と `website/.vitepress/config/ja.ts` の `'/changelog/'` / `'/ja/changelog/'` サイドバーに新ページを追加する．

- 対象プラットフォーム (`Paper` / `Velocity`) の該当メジャー (`v1` など) グループの**先頭**に入れる (新しい順)
- 新しいメジャーバージョンなら `text: 'v2'` のグループを先頭に作る
- en と ja で同じ位置に入れる

## 5. 検証

`website/` で次を実行し，すべて通ることを確認する．

```
bun run check
bun run build
```

`bun run build` は内部リンク切れで失敗するので，アンカーの誤りもここで検出される．失敗した場合は原因を直して再実行する．

外部リンク (GitHub / Modrinth) はリリース前には 404 になるのが正常なので，URL の形だけを §2 の表と照合する．

Release PR の必須チェックも同じ内容を見る．手元で先に確かめるには次を実行する (タグは §1 で決めたもの):

```
.github/scripts/check-release-notes.sh v1.7.1 velocity/v1.4.1
```

## 6. 報告

- 作成・変更したファイルの一覧
- コミット履歴から落とした項目・畳んだ項目とその理由
- 判断に迷った振り分け (ユーザーが見直すべき箇所)

コミットはユーザーに確認してから行い，push はユーザーが行う (Release PR のブランチへの push になる)．メッセージ例: `docs(changelog): add release notes for paper v1.7.1`．push 後は Release PR のチェックが通ることを確かめ，ユーザーがマージすればリリースが始まる．

このスキルが作ったページの本文は，リリース時に GitHub Release と Modrinth の changelog としてそのまま使われる (Download リストと `# vX.Y.Z` の見出しは除かれる)．英語ページの内容は，Release と Modrinth を読む人にも通じる書き方にする．

---
description: CHANGELOG.md の未公開エントリから，ドキュメントサイトの英語・日本語リリースノート (website/src/changelog/, website/src/ja/changelog/) を作成する．
disable-model-invocation: true
argument-hint: [paper|velocity|both] [version]
allowed-tools: Bash(git *), Bash(gh *), Bash(bun *), Bash(curl *), Read, Grep, Glob, Edit, Write
---

# Create Release Note

引数: `$ARGUMENTS`

`CHANGELOG.md` は開発者向けの網羅的な変更記録，サイトのリリースノートは利用者 (サーバー管理者) 向けの要約である．このスキルは前者を後者へ書き直す．逐語訳・丸写しはしない．

## 1. 対象バージョンの決定

1. `CHANGELOG.md` を読む．見出しはリリースタグと 1 対 1 に対応する:

   | 見出し | リリース | タグ |
   |--------|---------|------|
   | `### vX.Y.Z` | Paper / Folia 版 | `vX.Y.Z` |
   | その直下の `#### Velocity: vA.B.C` | 同時に出す Velocity 版 | `velocity/vA.B.C` |
   | `### Velocity: vA.B.C` | Velocity 版の単独リリース | `velocity/vA.B.C` |

   v1.4.0 以前の見出しには旧規則のもの (Velocity のためだけに作った `### vX.Y.Z` など) があるが，新しいページの対象にはならないので気にしなくてよい
2. `gradle.properties` の `paperVersion` / `velocityVersion` と突き合わせる．CHANGELOG の先頭のエントリと一致しない場合はユーザーに報告して止まる
3. 引数があればそれに従う．無ければ，先頭のエントリのうち `website/src/changelog/{paper,velocity}/v{version}.md` がまだ存在しないものを対象にする
4. 対象のページが既に存在する場合は上書きせず，差分の追記か作り直しかをユーザーに確認する

## 2. リンク先

Download リンクはバージョン番号だけで決まる:

| | GitHub | Modrinth |
|--|--------|----------|
| Paper | `https://github.com/m1sk9/LunaticChat/releases/tag/vX.Y.Z` | `https://modrinth.com/plugin/lunaticchat/version/X.Y.Z` |
| Velocity | `https://github.com/m1sk9/LunaticChat/releases/tag/velocity/vA.B.C` | `https://modrinth.com/plugin/lunaticchat/version/A.B.C-velocity` |

## 3. 本文の書き起こし

テンプレートは `website/src/assets/changelog-default.md`．既存ページ (`website/src/changelog/paper/v1.3.0.md` など) の書きぶりに揃える．

### 振り分け

CHANGELOG の各項目を次の見出しに振り分ける．項目の無い見出しは削除する．

| 見出し | 入れるもの |
|--------|-----------|
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
- `#### Velocity:` 以下の項目は Velocity ページへ，それ以外は Paper ページへ．両方に関わる項目 (engine の変更，プロトコル) は両方へ
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

Velocity 単独のリリースで CHANGELOG に `### Velocity: vA.B.C` の見出しが無く，Paper の見出しの下に書かれている場合は，見出しの修正を提案する．

## 6. 報告

- 作成・変更したファイルの一覧
- CHANGELOG から落とした項目・畳んだ項目とその理由
- 判断に迷った振り分け (ユーザーが見直すべき箇所)

コミットはユーザーに確認してから行う．メッセージ例: `docs(changelog): add release notes for paper v1.5.0 and velocity v1.4.0`

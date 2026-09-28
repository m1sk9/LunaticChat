---
description: ドキュメントサイト (website/) 全体を検査し，誤字・表記揺れ・リンク切れ・英日の抜け・ソースコードとの食い違いを洗い出して修正する．
disable-model-invocation: true
argument-hint: [path ...]
allowed-tools: Bash(bun *), Bash(curl *), Bash(git *), Bash(gh *), Bash(jq *), Read, Grep, Glob, Edit, Write, Agent
---

# Correction Docs

引数: `$ARGUMENTS`

引数でパス (例: `website/src/docs/features`) が与えられた場合はその配下だけを対象にする．無ければ `website/` 全体が対象．

ドキュメントの正しさの基準は**ソースコード**である．ドキュメント同士が一致していても，コードと食い違っていれば誤りとして扱う．

## 対象

| 対象 | パス |
|------|------|
| 英語ページ | `website/src/**/*.md` (`ja/` と `assets/` を除く) |
| 日本語ページ | `website/src/ja/**/*.md` |
| ナビゲーション・サイドバー | `website/.vitepress/config/en.ts`，`ja.ts`，`config.mts` |
| コンポーネントの表示文言 | `website/.vitepress/theme/components/*.vue` |

`website/src/assets/changelog-default.md` はテンプレートなので内容検査の対象外 (公開されない)．

## 1. 機械的な検査

`website/` で実行する:

```
bun install
bun run format:check
bun run lint
bun run test
bun run build
```

`bun run build` は内部リンク切れで失敗する．エラーがあれば記録する (修正は §4 でまとめて行う)．

## 2. 検査の観点

以下の観点で全ページを読む．ページ数が多いので，観点ごとに `Agent` (subagent_type: `Explore` など読み取り専用のもの) へ並列に委譲してよい．委譲する場合は各エージェントに「修正はせず，`ファイル:行` と誤り・根拠・修正案を列挙して返す」ことを指示する．目安は次の 4 分担:

1. リファレンス (`docs/reference/`，`docs/permissions.md`，`docs/configuration.md`) とソースの照合
2. 機能ガイド・導入 (`docs/features/`，`docs/getting-started.md`，`download.md`，`index.md`) とソースの照合
3. 開発者向け (`docs/developers/`) とソースの照合
4. 英日の対応と文章品質 (全ページ)

### 2-1. ソースコードとの照合

| ドキュメントの記述 | 照合先 |
|------------------|--------|
| コマンド名・エイリアス・引数・説明 | `platform-paper/.../command/impl/` の `@Command` と `LunaticSubCommand` の `literal` / `aliases`，Velocity 側のコマンド実装 |
| 権限ノード | `engine/.../permission/LunaticChatPermissionNode.kt`，各コマンドの `@Permission` / `permissionNode` |
| 設定キー・既定値・説明 | `platform-paper/src/main/resources/config.yml` と `platform-paper/.../config/` の設定クラス |
| プレイヤー設定 | `engine/.../settings/` と `platform-paper/.../command/setting/` |
| メッセージ書式・表示文言 | `platform-paper/src/main/resources/languages/{en,ja}.yml`，`i18n/` |
| デバッグカテゴリ・環境変数 | `engine/.../debug/DebugCategory.kt`，Velocity 側の `debug/VelocityDebugSwitch.kt` |
| 対応バージョン | `platform-paper/build.gradle.kts` の `paper-api`，`platform-velocity/build.gradle.kts` の `velocity-api`，`platform-paper/src/main/resources/paper-plugin.yml` の `api-version` |
| プロトコル・互換性 | `engine/.../protocol/ProtocolVersion.kt` と `PluginMessage` / `PluginMessageCodec` |
| モジュール構成・パッケージ・設計 (開発者向け) | 実際のディレクトリ構成と各モジュールの `build.gradle.kts`．CLAUDE.md の記述のとおり，開発者向けドキュメントはリファクタリングに追従できていない箇所がある (例: `converter` の所属，`ConfigManager` の読み込み方式) |

ソース上に存在するのにドキュメントに無いもの (新しいコマンド・設定キー・権限) は**抜け**として扱う．

### 2-2. 更新履歴

- `CHANGELOG.md` にある v1.3.0 以降の各リリースについて，`website/src/changelog/` と `website/src/ja/changelog/` にページがあり，サイドバーに登録されていること
- 各ページの Download リンクが，実在するリリースタグを指していること．v1.4.0 より後の規則は Paper が `vX.Y.Z`，Velocity が `velocity/vX.Y.Z`．v1.4.0 までは同時リリースの `vX.Y.Z` に両方が入っている場合がある (例: Velocity v1.3.0 は `v1.4.0` の Release)．実在するタグは `gh release list --limit 50` で確かめる
- リリースノートの内容が `CHANGELOG.md` と矛盾しないこと (書き換え・要約は正常．事実の食い違いだけを指摘する)
- 公開済みのリリースノートの**内容の書き換え**は，事実誤認・リンク切れ・誤字の修正に留める

### 2-3. 英日の対応

- すべての英語ページに対応する日本語ページがあり，逆も同様であること
- 見出しの数と階層，表の行，コードブロック，`:::` コンテナ (tip / warning) の数が対応していること．片方にしか無い段落や項目は抜けとして扱う
- `en.ts` と `ja.ts` の nav / sidebar が同じ構造であること
- 日本語ページ内のリンクは `/ja/...` を指していること (英語ページへ飛ばない)．英語ページも同様に `/ja/` を指さないこと
- アンカー付きリンクは，リンク先ページに該当見出しが実在すること (日本語の見出しは日本語のままアンカーになる)

### 2-4. 文章品質

英語:

- 綴り・文法・時制の誤り，主語と動詞の不一致，冠詞の欠落
- 用語の揺れ (例: `direct message` / `DM`，`channel chat` / `channel`) は，同じ概念に同じ語を使っているかで判断する

日本語:

- 句読点は `，` `．`，括弧は半角 `()`．`、` `。` `（）` を検出したら置き換える
- 文末の句点の欠落，ですます調と常体の混在
- 誤字・脱字・送り仮名 (例: 「有効する」→「有効にする」)
- 表記揺れ (例: 「フォルダー」/「フォルダ」，「サーバー」/「サーバ」)．サイト内で多数派の表記に揃える
- 技術用語・固有名詞は英語のまま (Paper，Velocity，JAR など)

### 2-5. 外部リンク

全ページとコンポーネントから `https?://` のリンクを抜き出し，重複を除いて `curl -s -o /dev/null -w '%{http_code}' -L {url}` で確かめる．`200` 以外を記録する．レート制限 (GitHub の `429` など) は失敗扱いにせず，時間をおいて再確認する．

## 3. 指摘の整理

指摘を次の 2 種類に分ける．

| 種類 | 例 | 扱い |
|------|-----|------|
| **自明な修正** | 誤字，句読点，リンク切れ (正しいリンク先が一意に決まる)，英日の対応でもう一方から明らかに補える抜け，ソースと一字一句で照合できる値 (既定値・権限ノード名など) | そのまま修正する |
| **判断を要する修正** | 説明の書き直し，節の追加・削除，ページ構成の変更，正しい記述がソースから一意に決まらないもの，公開済みリリースノートの内容変更 | 一覧にしてユーザーの承認を得てから修正する |

判断を要する修正は，`ファイル:行`，現状，根拠 (ソースの `ファイル:行`)，修正案 をまとめて一度に提示する．

## 4. 修正

- 既存ページの書きぶり (見出しの付け方，表の形，`:::` の使い方) に合わせる
- 英語版を直したら日本語版も，日本語版を直したら英語版も対応箇所を直す
- 開発者向けドキュメントでモジュール境界の記述を直す場合は，CLAUDE.md の記述とも矛盾しないようにする
- サイドバーを変更した場合は `en.ts` と `ja.ts` の両方を直す

修正後，§1 のコマンドをもう一度すべて実行し，通ることを確認する．

## 5. 報告

- 修正した内容 (ファイルごと，観点ごとの件数と主な例)
- 承認待ちの「判断を要する修正」の一覧 (未承認のもの)
- 外部リンクで修正できなかったもの (リンク先が消えていて代替が無い，など)
- §1 のコマンドの最終結果

コミットはユーザーに確認してから行う．メッセージ例: `docs: fix typos and stale references across the documentation site`

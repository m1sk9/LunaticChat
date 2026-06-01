---
layout: doc
---

# Paper / Velocity 互換性

LunaticChat の Paper プラグインと Velocity プラグインは独立にバージョン管理されています．それぞれの組み合わせが動作するかどうかは，両プラグインに埋め込まれた**プロトコルバージョン**で判定されます．

::: warning プラグインバージョン ≠ プロトコルバージョン
**プラグインバージョン** (例: Paper v1.2.0) と **プロトコルバージョン** (例: 1.0.0) は別物です．プラグイン側のリリースを重ねてもプロトコルが変わらなければ互換性は維持されます．互換性を決めるのはプロトコルバージョンのみです．
:::

## 結論から

- **両プラグインの最新版同士は常に互換性があります．** 迷ったら両方を最新にしてください．
- 古いバージョンを混在させたい場合は，下のマトリクスで組み合わせを確認してください．
- 接続状態は Minecraft サーバーで `/lcv status` を実行すると確認できます．

## 互換性マトリクス

各セルは「その Paper × Velocity の組み合わせが接続できるか」を示します．データは GitHub Releases から自動取得されます．

<CompatibilityMatrix />

## プロトコルバージョンとは

Paper / Velocity 間の通信は LunaticChat 独自のプラグインメッセージプロトコルで行われています．プロトコルにはセマンティックバージョニング (`MAJOR.MINOR.PATCH`) が振られており，接続時のハンドシェイクで Velocity 側がバージョンを照合します．

::: tip 互換性チェックは Velocity 側のみ
互換性判定をしているのは Velocity 側のみです．Paper 側はハンドシェイクを送るだけで，バージョンチェックはしません．つまり「Velocity が Paper のプロトコルを受け入れられるか」がそのまま接続可否になります．
:::

判定ルールは以下です（Velocity 視点）：

- **MAJOR** が一致すること
- Paper の **MINOR** が，Velocity の `MIN_SUPPORTED_MINOR` 以上かつ Velocity の `MINOR` 以下であること
- **PATCH** は判定に影響しない

`MIN_SUPPORTED_MINOR` は「Velocity がどこまで古い Paper の MINOR を受け入れるか」を示す値で，ローリングアップデート中の猶予期間を作るために使われます．

### バージョンバンプの基準

| レベル | 変更例 | 互換性 | デプロイ順序 |
|--------|--------|--------|-------------|
| **PATCH** (1.0.0 → 1.0.1) | optional フィールド追加，新 sub-channel 追加 | 完全互換 (`ignoreUnknownKeys=true` で安全) | 順不同，いつでも |
| **MINOR** (1.0.x → 1.1.0) | required フィールド追加，既存 sub-channel のセマンティクス変更 | `MIN_SUPPORTED_MINOR` の範囲内で後方互換 | **Velocity を先に更新** → 各 Paper を順次更新 |
| **MAJOR** (1.x.x → 2.0.0) | ワイヤフォーマット変更，sub-channel 削除/リネーム | 非互換 | **全サーバー同時デプロイ** |

### ローリングアップデートの考え方

1. **プロトコル変更なし**：Paper / Velocity を独立にデプロイ可能．プラグインのバグ修正やリファクタはここに入ります．
2. **PATCH 変更**：どちら側からでも自由にデプロイ可能．
3. **MINOR 変更**：Velocity を先行更新し，`MIN_SUPPORTED_MINOR` で旧 Paper を許容．全 Paper 更新後に `MIN_SUPPORTED_MINOR` を引き上げ．
4. **MAJOR 変更**：メンテナンスウィンドウで一括更新．

## ハンドシェイクの挙動

接続時は以下の流れで互換性が確認されます：

1. Paper サーバー起動時に Velocity に対してハンドシェイクを送信
2. Velocity が Paper のプロトコルバージョンを自身のものと照合
3. 不一致の場合は Velocity が接続を拒否し，Paper 側の状態が `FAILED` になる
4. ハンドシェイクのタイムアウトは 5 秒

接続状態は `/lcv status` で確認できます．詳細は [Velocity 連携](/docs/features/velocity#接続状態) を参照してください．

---
description: 現在のコードベースからビルドした Paper 版と Velocity 版の LunaticChat が互換性を持つかを，JAR のプロトコル定数・後方互換テスト・Docker 上の実機ハンドシェイクで検証する．
disable-model-invocation: true
argument-hint: [--static-only]
allowed-tools: Bash(./gradlew *), Bash(./x *), Bash(git *), Bash(javap *), Bash(docker *), Bash(curl *), Read, Grep, Glob, Edit
---

# Test Integration

引数: `$ARGUMENTS`

Paper ↔ Velocity の互換性は**プロトコルバージョンだけ**で決まる (プラグインバージョンは見ない)．判定するのは Velocity 側で，`PluginMessageHandler` が Paper から届いた Handshake に対し「MAJOR が一致し，Paper の MINOR が `MIN_SUPPORTED_MINOR..MINOR` に入る」かを評価する．Paper 側は応答の `compatible` をそのまま受け入れる．

検証は 3 段階で行う．`--static-only` のときは段階 1・2 だけ実行する．

## 段階 1: ビルドと JAR の静的検査

1. 古い JAR を掴まないように clean してからビルドする:
   ```
   ./gradlew :platform-paper:clean :platform-velocity:clean :platform-paper:shadowJar :platform-velocity:shadowJar
   ```
2. 生成物を特定する: `platform-paper/build/libs/LunaticChat-*.jar` と `platform-velocity/build/libs/LunaticChat-*-velocity.jar` (それぞれ 1 つだけのはず)
3. 各 JAR に埋め込まれたプロトコル定数を取り出す:
   ```
   javap -constants -cp {jar} dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
   ```
   `MAJOR` / `MINOR` / `PATCH` / `MIN_SUPPORTED_MINOR` の 4 値を記録する
4. 次の組み合わせで互換性を判定する (判定式は `ProtocolVersion.isCompatible` と同じ．左が Velocity，右が Paper):

   | 組み合わせ | Velocity 側の定数 | Paper 側の定数 |
   |-----------|------------------|---------------|
   | HEAD ↔ HEAD | HEAD の Velocity JAR | HEAD の Paper JAR |
   | HEAD Velocity ↔ 最新リリース済み Paper | HEAD の Velocity JAR | 最新 Paper リリースのタグ時点の `ProtocolVersion.kt` |
   | 最新リリース済み Velocity ↔ HEAD Paper | 最新 Velocity リリースのタグ時点の `ProtocolVersion.kt` | HEAD の Paper JAR |

   Paper と Velocity は独立にリリースされるため，下 2 行は「片側だけ更新した運用」が成り立つかの確認になる．最新リリースのタグは `gh release list` で特定する (`velocity/vX.Y.Z` は Velocity，`vX.Y.Z` は Paper を含む．v1.4.0 以前の `vX.Y.Z` は両方を含み，`paper/vX.Y.Z` は Paper のみ．どの版が入っているかは添付 JAR の名前で判断する)．タグ時点のソースは次で読む:
   ```
   git show {tag}:engine/src/main/kotlin/dev/m1sk9/lunaticChat/engine/protocol/ProtocolVersion.kt
   ```
5. 下 2 行が非互換の場合，それが意図された変更 (MAJOR / MINOR の引き上げ) であれば失敗ではなく**デプロイ順序の制約**として報告する (MINOR: Velocity を先に出す，MAJOR: 同時に出す)．意図されていない非互換は失敗とする

HEAD ↔ HEAD が非互換なのは両 JAR が別の engine からビルドされたことを意味するので，即座に失敗として報告する．

## 段階 2: プロトコルのテスト

```
./gradlew :engine:test --tests '*ProtocolBackwardCompatibilityTest' --tests '*PluginMessageCodec*' --tests '*ProtocolVersion*'
./gradlew :platform-velocity:test :platform-paper:test --tests '*velocity*'
```

存在しないテストクラス名のパターンで Gradle が失敗した場合は，`Grep` で実在するテストクラスを確認してパターンを直す．

直近リリースタグから `engine/.../protocol/` に変更がある場合は，`ProtocolBackwardCompatibilityTest` に新しいスナップショットが追加されているかも確認する．追加されていなければ指摘する．

## 段階 3: Docker 上の実機検証

`./x` の Velocity 環境 (`docker/velocity/`，プロキシ `debug-velocity` + バックエンド `debug-server-s1` / `debug-server-s2`) を使う．サーバーバージョンは `./x` が `build.gradle.kts` から導出するので，compose にバージョンを書き足さないこと．

### 準備

1. Docker が動いていることを確認する (`docker info`)．動いていなければユーザーに起動を依頼して止まる
2. 既に `debug-*` コンテナが動いていれば，停止してよいかユーザーに確認する
3. バックエンドの設定を確認する: `docker/velocity/plugins-paper-s1/LunaticChat/config.yml` と `plugins-paper-s2/...` の `features.velocityIntegration.enabled` が `true` であること
   - ファイルが無い場合 (初回起動前) は，一度起動して生成させてから編集し，再起動する
   - `false` の場合は変更内容をユーザーに伝えてから `true` に書き換える (このディレクトリは gitignore 済みのローカルデータ)．元の値を報告に残す
   - `crossServerGlobalChat` / `crossServerDirectMessage` も現在値を記録しておく (段階 3 の任意項目で使う)

### 起動

`./x start velocity` はビルドと JAR の配置を行ったうえで `docker compose up` をフォアグラウンドで実行する．Bash の `run_in_background` で起動し，3 コンテナのヘルスチェックが `healthy` になるまで待つ:

```
docker inspect -f '{{.Name}} {{.State.Health.Status}}' debug-velocity debug-server-s1 debug-server-s2
```

5 分経っても `healthy` にならなければ `docker logs` を確認して原因を報告する．

### 起動時ログの検査

```
docker logs debug-velocity 2>&1 | grep -iE 'lunaticchat|plugin message|presence'
docker logs debug-server-s1 2>&1 | grep -iE 'lunaticchat|velocity'
docker logs debug-server-s2 2>&1 | grep -iE 'lunaticchat|velocity'
```

期待する行:

| コンテナ | 期待するログ |
|---------|-------------|
| `debug-velocity` | `LunaticChat Velocity plugin initialized successfully`，`Plugin message handler registered for channel:` |
| `debug-server-s1` / `s2` | `Velocity integration channel registered:`，`Velocity integration initialized. Waiting for first player join to perform handshake.` |

LunaticChat に起因する `ERROR` / `SEVERE` / 例外スタックトレースが無いことも確認する．

### ハンドシェイク (プレイヤーの接続が必要)

Plugin Messaging はプレイヤーの接続を経由して流れるため，ハンドシェイクは**最初のプレイヤーが参加した 1 秒後**に始まる．Claude からはプレイヤーを接続できないので，ユーザーに次を依頼する:

1. Minecraft クライアント (バージョンは `platform-paper/build.gradle.kts` の `paper-api` が示すもの) で `localhost:25577` に接続する．offline mode なので任意のプレイヤー名でよい
2. s1 に入ったら，`/server s2` で s2 に移動する

依頼を出したら，ログにハンドシェイクの結果が出るまで待つ．期待する行:

| コンテナ | 成功 | 失敗 |
|---------|------|------|
| `debug-velocity` | `Handshake successful with s1` (s2 も同様) | `Protocol version incompatible: Paper=..., Velocity=...` |
| `debug-server-s1` / `s2` | `Successfully connected to Velocity (version: ..., protocol: ...)` | `Velocity handshake failed:` / `Handshake timeout` |

ログの `protocol:` が段階 1 で取り出した値と一致することも確かめる．

続けて，プレイヤーが接続したまま console からステータス要求を送り，往復を確認する:

```
docker exec debug-server-s1 rcon-cli lcv status
docker logs debug-server-s1 2>&1 | grep 'Received status response from Velocity'
```

### 任意: サーバー間機能

ユーザーがクライアントを 2 つ用意できる場合に限り実施する (s1 と s2 に 1 人ずつ)．

- `crossServerGlobalChat: true` なら，一方のグローバルチャットがもう一方に届くこと
- `crossServerDirectMessage: true` なら，`/tell {player}@{server}` が届き，`/reply` で返せること

結果はユーザーに目視で確認してもらい，報告に含める．

### 後片付け

環境を止めるかユーザーに確認する．止める場合は `./x stop velocity` (ボリュームは残す)．書き換えた `config.yml` を元に戻すかも確認する．

## 報告

次の形でまとめる．

1. **判定**: 互換 / 非互換 / デプロイ順序の制約つきで互換
2. **プロトコル定数表**: HEAD Paper・HEAD Velocity・最新リリース済み Paper・最新リリース済み Velocity の 4 値と，段階 1 の 3 組み合わせの判定
3. **テスト結果**: 段階 2 の成否 (失敗したテスト名)
4. **実機検証**: 起動ログ・ハンドシェイク・ステータス往復・(実施した場合) サーバー間機能の結果．実施しなかった項目は「未実施」と明記する
5. **設定の変更**: 段階 3 で書き換えたファイルと元の値

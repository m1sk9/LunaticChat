---
description: 指定したプラットフォーム (paper / folia / velocity) のデバッグサーバーを現在のコードでビルドして起動し，Windows の Minecraft クライアントから LAN または Tailscale 経由で接続できる状態にする．
disable-model-invocation: true
argument-hint: <paper|folia|velocity> [--stable]
allowed-tools: Bash(./x *), Bash(docker *), Bash(jq *), Bash(lsof *), Bash(ipconfig *), Bash(tailscale *), Bash(colima *), Bash(/usr/libexec/ApplicationFirewall/socketfilterfw *), Read, Grep
---

# Start Preview Server

引数: `$ARGUMENTS`

`./x` のデバッグ環境を起動し，別マシン (Windows) のクライアントから入れるところまでを確認する．サーバーの構成そのもの (compose，`velocity.toml` など) は変更しない．

## 1. 引数の解釈

| 引数 | 起動するもの | クライアントの接続ポート | コンテナ |
|------|-------------|------------------------|---------|
| `paper` | Paper 1 台 | `25565` | `debug-paper` |
| `folia` | Folia 1 台 | `25565` | `debug-folia` |
| `velocity` | Velocity + Paper 2 台 (s1 / s2) | `25577` | `debug-velocity`，`debug-server-s1`，`debug-server-s2` |

- プラットフォームが無い，または上の 3 つ以外なら，どれを起動するかユーザーに確認する
- `--stable` があれば `./x` にそのまま渡す．無ければ nightly ビルドになる (`./x` の既定)

## 2. 事前確認

1. Docker が応答すること (`docker info`)．応答しなければ `colima status` を確認し，Colima の起動 (`colima start`) をユーザーに依頼して止まる
2. **別の環境が動いていないこと**．コンテナ名は固定で，全 worktree・全チェックアウトで共有される．また，3 環境ともホストの `25565` / `25575` などを取り合う:
   ```
   docker ps -a --filter name=debug-
   ```
   動いているものがあれば，名前と起動元を示し，止めてよいかユーザーに確認する．止める場合は該当環境の `./x stop <platform>` を使う (ボリュームは残す)
3. 接続ポートがほかのプロセスに使われていないこと:
   ```
   lsof -nP -iTCP:{port} -sTCP:LISTEN
   ```
   Colima 以外のプロセスが使っていれば報告して止まる

## 3. 起動

`./x start <platform> [--stable]` はビルドと JAR の配置のあと，`docker compose up` をフォアグラウンドで実行する．Bash の `run_in_background` で起動する (止まるまで返ってこないため)．

起動後，§1 の表のコンテナがすべて `healthy` になるまで待つ．初回はサーバー JAR のダウンロードとワールド生成があるので数分かかる:

```
docker inspect {container} | jq -r '.[0].State.Health.Status'
```

5 分経っても `healthy` にならなければ，`docker logs {container} --tail 100` を確認して原因を報告する．ビルドで失敗した場合は，バックグラウンドの出力に Gradle のエラーが出る．

LunaticChat が有効になったことをログで確かめる:

```
docker logs {container} 2>&1 | grep -iE 'lunaticchat'
```

`velocity` の場合は `debug-velocity` に `LunaticChat Velocity plugin initialized successfully` が出ていること．LunaticChat 由来の `ERROR` / `SEVERE` や例外があれば報告に含める．

## 4. 接続先の特定

Colima は公開ポートを macOS 上の `ssh` プロセスで全インターフェイス (`*:{port}`) に転送する．したがって，Mac の LAN の IP と Tailscale の IP のどちらからでも届く．

1. 転送が全インターフェイスで待ち受けていることを確かめる:
   ```
   lsof -nP -iTCP:{port} -sTCP:LISTEN
   ```
   `ssh` が `*:{port}` で待ち受けていれば良い．`127.0.0.1:{port}` だけなら別マシンからは届かないので，報告して止まる
2. macOS のファイアウォールが `ssh` の着信を塞いでいないこと:
   ```
   /usr/libexec/ApplicationFirewall/socketfilterfw --getblockall
   /usr/libexec/ApplicationFirewall/socketfilterfw --listapps
   ```
   block all が有効，または `/usr/bin/ssh` が `Block incoming connections` なら，設定の変更をユーザーに依頼する (Claude からは変更しない)
3. 接続先の候補を集める:
   - LAN: `ipconfig getifaddr en0` (空なら `en1`)
   - Tailscale: `tailscale ip -4` と，`tailscale status --json | jq -r '.Self.DNSName'` (MagicDNS 名．末尾の `.` は外す)
   - Tailscale 上の Windows 機: `tailscale status --json | jq -r '.Peer[] | select(.OS == "windows") | "\(.HostName) \(.TailscaleIPs[0]) online=\(.Online)"'`
4. Windows 機が Tailscale に参加していてオンラインなら，経路を確かめる．結果の `via` が LAN のアドレスなら直結，DERP なら中継経由:
   ```
   tailscale ping --c 1 {windows-hostname}
   ```

Windows 側から実際にポートへ届くかは，ここでは確かめられない．クライアントで接続してもらって確認する．

## 5. 案内

ユーザーに次をまとめて伝える．

- **接続先**: 表にする．Windows 機が Tailscale でオンラインならそれを先頭にし，LAN は同じネットワークにいる場合の代替として並べる

  | 経路 | アドレス |
  |------|---------|
  | Tailscale (MagicDNS) | `{dns-name}:{port}` |
  | Tailscale (IP) | `{tailscale-ip}:{port}` |
  | LAN | `{lan-ip}:{port}` |

- **クライアントのバージョン**: `platform-paper/build.gradle.kts` の `paper-api` が示す Minecraft バージョン．Velocity でも，バックエンドの Paper と同じバージョンのクライアントが必要
- **ログイン**: offline mode なので，Microsoft アカウントの認証なしに任意のプレイヤー名で入れる
- **velocity の場合**: 最初に s1 に入る．`/server s2` で s2 に移る
- **操作用のコマンド**:
  - ログを追う: `./x log {platform}`
  - console: `./x rcon {platform}` (velocity は `./x rcon velocity s1` / `s2`)
  - 停止: `./x stop {platform}`．ボリュームごと消すなら `./x clean {platform}`

### 注意として必ず伝えること

- offline mode のため，同じネットワークに入れる人なら誰でも任意の名前 (op を持つ名前も含む) で参加できる
- RCON (`25575` など) もパスワード `minecraft` のまま全インターフェイスに公開されている
- 以上から，信頼できる LAN または自分の Tailnet の中だけで使うこと．不特定多数がいるネットワーク (カフェ，イベント会場など) では起動しない

## 6. 報告

- 起動した環境，ビルドの種類 (nightly / stable)，LunaticChat のバージョン
- §5 の接続先の表
- §4 の確認結果 (待ち受けのアドレス，ファイアウォール，`tailscale ping` の成否)．確認できなかった項目は「未確認」と書く
- 起動ログで見つかった警告・エラー

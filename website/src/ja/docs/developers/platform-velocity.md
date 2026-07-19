---
layout: doc
---

# platform-velocity - Velocity プラグイン本体 (プロキシ中継)

`platform-velocity` はサーバー間グローバルチャットの**中継**だけを担う薄い層です．

実体は `LunaticChat` / `BuildInfo` / `messaging/` の 2 ファイルのみで，コマンドクラスは持ちません．`/lcv` は Velocity に関する機能ですが，コマンドの実装は [platform-paper](/ja/docs/developers/platform-paper) 側にある点に注意してください．

::: tip なぜ Velocity 側は薄いのか
protocol の定義は engine が持ち，チャットの状態 (チャンネル・DM・設定) はすべて Paper 側にあります．Velocity に残る責務は「受け取ったグローバルチャットを他サーバーへ配る」ことだけなので，この層は意図的に薄く保たれています．どちらのプラットフォームも protocol を所有せず，engine に対して対等に依存する構図です (詳細は [設計概要](/ja/docs/developers/architecture#なぜ-engine-を切り出すのか))．
:::

## ライフサイクル

- `LunaticChat` (`@Plugin`) — Guice の `@Inject` コンストラクタで `ProxyServer` / `Logger` / `PluginContainer` を受け取る
- `@Plugin` アノテーションの `version` は `"0.0.0"` 固定で**実行時には使われない**．実バージョンは `velocity-plugin.json` から `PluginContainer.description.version` 経由で取得する (見つからなければ起動を止める)
- `@Subscribe onProxyInitialization` で `CrossServerChatRelay` を生成 → それを注入して `PluginMessageHandler` を生成し `initialize()`
- `@Subscribe onProxyShutdown` で `messageHandler.shutdown()`

## メッセージ受信とディスパッチ

`PluginMessageHandler` がチャンネル `lunaticchat:main` の受信を捌きます．`initialize()` で `channelRegistrar.register(CHANNEL)` とイベント購読を行います．

`@Subscribe onPluginMessage` の処理:

1. `event.identifier != CHANNEL` なら無視する
2. 送信元が `ServerConnection` でなければ警告して破棄する
3. `PluginMessageCodec.decode()` の結果を `when` で分岐する
4. `Handshake` → 互換判定して `HandshakeResponse` 返送 / `StatusRequest` → `StatusResponse` 返送 / `GlobalChatMessage` → 中継へ委譲 / それ以外 (Velocity 発の応答型) → 警告のみ

### 信頼境界: クライアント由来メッセージの拒否

送信元が `ServerConnection` かどうかのチェックは，単なる型ガードではなく**信頼境界**です．

Velocity のプラグインメッセージはバックエンドサーバーだけでなくクライアントからも届き得ます．ここでバックエンド接続以外を弾くことで，**クライアントがグローバルチャットや偽ハンドシェイクを直接注入することを防いでいます**．中継されるのは信頼できるサーバー接続から来たメッセージだけです．

## ハンドシェイク処理

`Handshake` を受け取ると engine の `ProtocolVersion.isCompatible(major, minor)` で互換性を判定します．

- **互換** — `compatible=true` の `HandshakeResponse` を返送
- **非互換** — Paper 側・Velocity 側のバージョンを載せた error 文字列とともに `compatible=false` を返送

`HandshakeResponse` / `StatusResponse` には Velocity 自身の `ProtocolVersion` (`MAJOR` / `MINOR` / `PATCH`) を必ず載せるため，Paper 側はレスポンスから相手のプロトコルを知ることができます．

## サーバー間中継

`CrossServerChatRelay.relayGlobalMessage(message, sourceServer)` が中継の核心です．

```
server.allServers
    .filter { it != sourceServer }   // 送信元を除外
    .forEach { it.sendPluginMessage(CHANNEL, encoded) }
```

**送信元サーバーを除外**して残り全バックエンドへブロードキャストします (エコー防止の一段目) ．中継件数はログに出ます．

### 中継されるもの / ローカルに留まるもの

中継の範囲を最小に絞っているのが設計上のポイントです．

- Velocity が他サーバーへ中継するのは **`GlobalChatMessage` のみ**
- `Handshake` / `HandshakeResponse` / `StatusRequest` / `StatusResponse` は Velocity ↔ 単一 Paper の間で完結し，転送しない
- **DM・チャンネルチャットはそもそも Velocity へ送られない** (Paper 内でローカル完結する)

### エコー / ループ防止の二段構え

グローバルチャットが中継ループで多重表示されないよう，2 箇所で防いでいます．

1. **Velocity 側** — 送信元サーバーを除外してブロードキャスト
2. **Paper 側** — `messageId` による重複排除 LRU キャッシュ (TTL 60s)．送信側は生成直後に自分の `messageId` を登録して自サーバーでのエコーも防ぐ

## 通信フロー

1. Paper がプレイヤー接続を契機に `Handshake` (自プロトコルバージョン) を送信する
2. Velocity が `ProtocolVersion.isCompatible` で判定し `HandshakeResponse` を返送する → 互換なら Paper 側は `CONNECTED`
3. プレイヤーがグローバルチャット (チャンネル未所属 or `!` プレフィックス) を送信 → Paper が `GlobalChatMessage` (新規 `messageId`) を Velocity へ送信し，送信元では通常チャットを表示する
4. Velocity が送信元以外の全バックエンドへ中継する
5. 各 Paper が受信 → `messageId` で dedup → `crossServerGlobalChatFormat` で整形して全プレイヤーへ配信する

## 実装ノート

- `PluginMessageHandler` の `plugin` パラメータ型が `Any` なのは，Velocity の `EventManager.register()` が `Object` を取るためです (API 自体が型安全でないので，ジェネリクス化しても実益が薄いとの判断)．
- クロスサーバーチャットを動かすには，Velocity の `velocity.toml` で `bungee-plugin-message-channel=true` (プラグインメッセージ有効) が必要です．

## 関連

- [設計概要](/ja/docs/developers/architecture)
- [engine - 共通カーネル](/ja/docs/developers/engine) — protocol の詳細
- [platform-paper - Paper / Folia プラグイン本体](/ja/docs/developers/platform-paper) — Paper 側の対向実装

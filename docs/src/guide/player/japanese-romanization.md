# ローマ字変換 <Badge type="tip" text="v0.1.0" /> <Badge type="tip" text="Paper" />

LunaticChat では、日本語のテキストをローマ字に変換する機能が提供されています．

## 有効化・無効化

ローマ字変換機能はデフォルトで有効化されています．

[`/lc settings`](../../reference/commands/lc/settings.md) コマンドを使用して，ローマ字変換の設定を切り替えることができます．

## 変換の仕組み

LunaticChat はプレイヤーからのテキストを以下の手順で変換を行います．

1. プレイヤーからのテキストがローマ字で構成されているかを検証する
2. メモリキャッシュから該当のフレーズがあるかどうかを調べる
   1. ここで該当フレーズがヒットした場合はそれをサーバーに返す
3. メモリキャッシュに存在していない場合は，ローマ字からひらがなに変換する
4. 変換した文字列を Google IME API へ送り，人が読める形に変換する
5. メモリキャッシュに保存し，それをサーバーに返す

```
┌───────────────────────────────────────────────────┐
│                   User Input                      │
│                  (Romanji Text)                   │
└─────────────────────┬─────────────────────────────┘
                      │
                      ▼
┌───────────────────────────────────────────────────┐
│              RomanjiConverter                     │
│  ┌───────────────────────────────────────────┐    │
│  │  1. Check Memory Cache                    │    │
│  │     └─→ Hit: Return immediately           │    │
│  │                                           │    │
│  │  2. Call Google IME API                   │    │
│  │                                           │    │
│  │  3. Store in Memory Cache                 │    │
│  │                                           │    │
│  │  4. Queue for Disk Save (async)           │    │
│  └───────────────────────────────────────────┘    │
└─────────────────────┬─────────────────────────────┘
                      │
                      ▼
┌───────────────────────────────────────────────────┐
│                  Converted Text                   │
│                 (Japanese Text)                   │
└───────────────────────────────────────────────────┘
```

::: tip ファイルへの保存

メモリキャッシュの内容は負荷にならないよう，設定した秒毎・サーバ停止後にファイルキャッシュへ自動でセーブします．

:::

キャッシュファイルに関する情報は [こちら](../admin/cache.md) をご覧ください．

## 改善されたキャッシュ戦略 <Badge type="tip" text="v0.5.0" />

v0.5.0 以降，LunaticChat はローマ字変換のキャッシュ戦略を改善しました．

プレイヤーのチャットを単語ごとにキャッシュするようになり，より効率的に変換を行えるようになりました．

例えば，以下のような長文のチャットがあるとします．

> konnichiwa minna ohayou gozaimasu kyou wa totemo ii tenki desu ne bokutachi wa issho ni asobi ni ikimashou kono atarashii game wo tameshite mitai to omoimasu sore wa totemo omoshiroi to kiite imasu arigatou gozaimasu mata ne

LunaticChat はこの文章を一度に変換するのではなく，単語ごとに分割してキャッシュを行います．

これにより，例えば「konnichiwa」や「minna」などの単語が既にキャッシュされている場合，それらの単語は再度変換する必要がなくなり，変換速度が大幅に向上します．

### 1回目の変換

```
入力: "konnichiwa minna ohayou gozaimasu"

konnichiwa → API → こんにちは (キャッシュ保存)
minna      → API → みんな (キャッシュ保存)
ohayou     → API → おはよう (キャッシュ保存)
gozaimasu  → API → ございます (キャッシュ保存)

結果: "こんにちは みんな おはよう ございます"
APIコール: 4回
```

### 2回目の変換

```
入力: "ohayou gozaimasu kyou wa ii tenki desu"

ohayou    → キャッシュヒット → おはよう
gozaimasu → キャッシュヒット → ございます
kyou      → API → 今日 (キャッシュ保存)
wa        → API → は (キャッシュ保存)
ii        → API → いい (キャッシュ保存)
tenki     → API → 天気 (キャッシュ保存)
desu      → API → です (キャッシュ保存)

結果: "おはよう ございます 今日 は いい 天気 です"
APIコール: 5回（キャッシュヒット: 2回）
```

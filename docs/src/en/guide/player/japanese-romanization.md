# Romanization Conversion <Badge type="tip" text="v0.1.0" /> <Badge type="tip" text="Paper" />

LunaticChat provides a feature to convert Japanese text from romaji.

## Enabling/Disabling

The romanization conversion feature is enabled by default.

You can toggle the romanization conversion setting using the [`/lc settings`](../../reference/commands/lc/settings.md) command.

## Conversion Process

LunaticChat converts text from players through the following steps:

1. Verify that the player's text is composed of romaji
2. Check if the phrase exists in the memory cache
   1. If a matching phrase is found here, return it to the server
3. If it doesn't exist in the memory cache, convert from romaji to hiragana
4. Send the converted string to the Google IME API to convert it to a human-readable form
5. Save to memory cache and return it to the server

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

::: tip Saving to File

To avoid adding load, the contents of the memory cache are automatically saved to file cache at configured intervals and after server shutdown.

:::

For information on cache files, see [here](../admin/cache.md).

## Improved Cache Strategy <Badge type="tip" text="v0.5.0" />

As of v0.5.0, LunaticChat has improved the cache strategy for romanization conversion.

Player chat is now cached word by word, enabling more efficient conversion.

For example, consider the following long chat message:

> konnichiwa minna ohayou gozaimasu kyou wa totemo ii tenki desu ne bokutachi wa issho ni asobi ni ikimashou kono atarashii game wo tameshite mitai to omoimasu sore wa totemo omoshiroi to kiite imasu arigatou gozaimasu mata ne

LunaticChat doesn't convert this sentence all at once, but splits and caches it word by word.

This means that if words like "konnichiwa" or "minna" are already cached, those words don't need to be converted again, significantly improving conversion speed.

### First Conversion

```
Input: "konnichiwa minna ohayou gozaimasu"

konnichiwa → API → こんにちは (cached)
minna      → API → みんな (cached)
ohayou     → API → おはよう (cached)
gozaimasu  → API → ございます (cached)

Result: "こんにちは みんな おはよう ございます"
API Calls: 4
```

### Second Conversion

```
Input: "ohayou gozaimasu kyou wa ii tenki desu"

ohayou    → Cache hit → おはよう
gozaimasu → Cache hit → ございます
kyou      → API → 今日 (cached)
wa        → API → は (cached)
ii        → API → いい (cached)
tenki     → API → 天気 (cached)
desu      → API → です (cached)

Result: "おはよう ございます 今日 は いい 天気 です"
API Calls: 5 (Cache hits: 2)
```

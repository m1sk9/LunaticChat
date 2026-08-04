---
layout: doc
---

# Japanese Conversion

Automatically converts chat messages typed in romaji into Japanese. To use this feature, set `features.japaneseConversion.enabled` to `true` in `config.yml`.

## How Conversion Works

Conversion is performed in two stages.

1. **Romaji to Hiragana**: The plugin's built-in Trie-based conversion engine converts romaji to hiragana
2. **Hiragana to Kanji/Katakana**: The Google IME API converts hiragana into natural Japanese

### Conversion Example

```
Input:       konnichiha sekai
Stage 1:     こんにちは せかい
Stage 2:     こんにちは 世界
Sent:        konnichiha sekai §e(こんにちは 世界)
```

The original text is **not** replaced. What you typed is kept, and the conversion result is appended in parentheses, so both are visible to everyone who receives the message.

## Conversion Targets

- Normal chat
- Direct messages (`/tell`, `/reply`)
- Channel chat

If the input is not valid romaji (e.g., contains English words), no conversion is performed and the message is sent as-is.

## Player Settings

Players can individually toggle conversion on or off.

```
/lc settings japanese on     # Enable conversion
/lc settings japanese off    # Disable conversion
```

## Cache

Conversion results are cached per word. When the same word is converted again, the result is retrieved from cache instead of calling the API. The cache is periodically saved to disk as a JSON file.

| Setting Key | Default | Description |
|-------------|---------|-------------|
| `cache.maxEntries` | `500` | Maximum number of cache entries |
| `cache.saveIntervalSeconds` | `300` | Interval for saving to disk (seconds) |
| `cache.filePath` | `"conversion_cache.json"` | Path to the cache file |

When the cache reaches its limit, 10% of the entries are removed. Which entries are dropped is not defined: the in-memory cache is unordered, so eviction is effectively arbitrary rather than oldest-first.

## API Settings

Settings related to the connection to the Google IME API.

| Setting Key | Default | Description |
|-------------|---------|-------------|
| `api.timeout` | `3000` | Timeout for a single API request (milliseconds) |

If the API times out or fails, the message is sent in hiragana as-is.

> [!WARNING]
> Independently of `api.timeout`, converting one message is given an overall budget of **1000 ms**. When that budget runs out the conversion is abandoned and the message is sent exactly as typed, with nothing appended.
>
> Because the overall budget is shorter than the default `api.timeout`, raising `api.timeout` above `1000` has no practical effect.

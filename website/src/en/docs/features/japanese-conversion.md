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
```

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

When the cache reaches its limit, the oldest 10% of entries are automatically removed.

## API Settings

Settings related to the connection to the Google IME API.

| Setting Key | Default | Description |
|-------------|---------|-------------|
| `api.timeout` | `3000` | Request timeout (milliseconds) |
| `api.retryAttempts` | `2` | Number of retry attempts on failure |

If the API times out or fails, the message is sent in hiragana as-is.

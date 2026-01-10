# LunaticChat - Design Document

## Project Overview

LunaticChat is a Minecraft chat plugin providing 1on1 messaging, quick reply functionality, and romaji-to-Japanese conversion features.

## Technology Stack

- **Language**: Kotlin
- **Target Platforms**:
    - Paper (Minecraft 1.21.x+)
    - Velocity (planned for future)
- **Build Tool**: Gradle (multi-project setup)

## Core Principles

1. **Always support the latest version** while maintaining backward compatibility (e.g., 1.21.x)
2. **Maintainability**: Design for extensibility and easy maintenance
3. **Use Paper's LifecycleEventManager** for command registration
4. **Chat logs must be compatible** with CoreProtect and similar logging plugins

## Project Structure

```
LunaticChat/
├── engine/              # Core logic (shared code, chat processing, romaji conversion)
├── platform-paper/      # Paper plugin implementation
├── platform-velocity/   # Velocity plugin implementation
└── docker/              # Docker configuration
```

### Why Separate JARs?

- Paper and Velocity use different APIs
- Avoids classloader conflicts
- Clear deployment boundaries
- Gradle multi-project keeps build unified

## Features (v0.1.0)

### 1. Direct Messaging System

**Commands**:
- `/tell` (aliases: `/t`, `/msg`, `/m`, `/w`, `/whisper`)
- `/reply` (alias: `/r`)

**Requirements**:
- Use Paper's `LifecycleEventManager` for command registration
- Messages must appear in CoreProtect logs
- Use `io.papermc.paper.event.player.AsyncChatEvent` (not deprecated `AsyncPlayerChatEvent`)
- Don't cancel events; modify messages instead

### 2. Quick Reply Functionality

- `/reply` sends message to last person who messaged you
- Maintain conversation context per player

### 3. Romaji to Japanese Conversion

**Trigger**: Player's personal setting via `/jp on` or `/jp off`

**Conversion Timing**: When player sends message (`AsyncChatEvent` fires)

**Architecture**: Simple cache + Google IME API approach

```
┌─────────────────────────────────────────┐
│         Player Input (Romanji)          │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│         Check Memory Cache              │
├─────────────────────────────────────────┤
│  Hit: Return cached result (< 1ms)      │
│  Miss: Call Google IME API              │
│        → Save to cache                  │
│        → Queue async disk save          │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│       Converted Text (Japanese)         │
└─────────────────────────────────────────┘
```

**Key Components**:

1. **RomanjiConverter** - Main conversion coordinator
2. **ConversionCache** - Two-tier caching (memory + disk)
    - Memory: ConcurrentHashMap for instant access
    - Disk: JSON file loaded on startup, saved periodically
3. **GoogleIMEClient** - HTTP client for Google Transliterate API

**Cache Strategy**:
- Load cache from disk on plugin enable (once)
- All conversions check memory cache first
- Cache misses trigger API call and store result
- Periodic async saves (every 5 minutes) + final save on disable
- LRU eviction when max entries (500) exceeded

**Performance**:
- Cached conversions: < 1ms
- API calls: < 3000ms (first time only per phrase)
- Disk I/O: Async, no gameplay impact
- Memory footprint: ~25KB for 500 entries
- Startup load time: < 10ms

**Example Implementation**:
```kotlin
class RomanjiConverter(
    private val cache: ConversionCache,
    private val apiClient: GoogleIMEClient
) {
    suspend fun convert(input: String): String {
        // Check cache first
        cache.get(input)?.let { return it }
        
        // Call Google IME API
        val result = apiClient.convert(input)
        
        // Store in cache
        cache.put(input, result)
        
        return result
    }
}
```

**Cache Implementation**:
```kotlin
class ConversionCache(
    private val cacheFile: Path,
    private val maxEntries: Int = 500
) {
    private val memoryCache = ConcurrentHashMap<String, String>()
    private val saveQueue = AtomicBoolean(false)
    
    fun loadFromDisk() {
        if (!cacheFile.exists()) return
        val data = Json.decodeFromString<CacheData>(cacheFile.readText())
        memoryCache.putAll(data.entries)
    }
    
    fun get(key: String): String? = memoryCache[key]
    
    fun put(key: String, value: String) {
        if (memoryCache.size >= maxEntries) evictOldest()
        memoryCache[key] = value
        queueDiskSave()
    }
    
    fun saveToDisk() {
        val data = CacheData(version = "1.0", entries = memoryCache.toMap())
        cacheFile.writeText(Json.encodeToString(data))
    }
}
```

## Data Persistence

**No SQL databases** - Use JSON file storage instead

**Storage Strategy**:
- Player settings stored as JSON files
- UUID-based file naming
- In-memory cache with periodic saves
- Use kotlinx.serialization for JSON handling

**Data Model**:
```kotlin
data class PlayerChatSettings(
    val uuid: UUID,
    val japaneseConversionEnabled: Boolean = false
)
```

**Cache Data Model**:
```kotlin
@Serializable
data class CacheData(
    val version: String,
    val entries: Map<String, String>
)
```

## Configuration

```yaml
features:
  japaneseConversion:
    enabled: true
    cache:
      maxEntries: 500
      saveIntervalSeconds: 300  # 5 minutes
      cacheFile: "conversion-cache.json"
    api:
      timeout: 3000  # milliseconds
      retryCount: 2
```

## Future Features (Post v0.1.0)

### Cross-Server Chat (Velocity)

- Broadcast normal messages across all servers
- Enable `/tell` for 1on1 chat across servers

### Channel Chat System

- Players can create custom channels
- Chat within specific channels
- Channel management commands

## Implementation Order

1. **Setup multi-project structure** (Paper/Velocity extensibility)
2. **Implement JSON-based data persistence**
3. **Implement `/tell` and `/reply` commands**
4. **Implement romaji conversion system**

## Event Handling

```kotlin
@EventHandler(priority = EventPriority.HIGHEST)
fun onChat(event: AsyncChatEvent) {
    val player = event.player
    val settings = settingsManager.get(player.uniqueId)
    
    if (settings.japaneseConversionEnabled) {
        val plainText = (event.message() as? TextComponent)?.content() ?: return
        val converted = runBlocking { romajiConverter.convert(plainText) }
        event.message(Component.text(converted))
    }
}
```

## Plugin Lifecycle

```kotlin
class LunaticChat : JavaPlugin() {
    private lateinit var romanjiConverter: RomanjiConverter
    
    override fun onEnable() {
        // Load cache on startup
        val cache = ConversionCache(
            cacheFile = dataFolder.resolve("conversion-cache.json").toPath(),
            maxEntries = config.getInt("features.japaneseConversion.cache.maxEntries", 500)
        )
        cache.loadFromDisk()
        
        // Initialize converter
        val apiClient = GoogleIMEClient(
            timeout = config.getInt("features.japaneseConversion.api.timeout", 3000).milliseconds
        )
        romanjiConverter = RomanjiConverter(cache, apiClient)
        
        // Periodic save task
        val saveInterval = config.getLong(
            "features.japaneseConversion.cache.saveIntervalSeconds", 300
        ) * 20L // Convert seconds to ticks
        
        server.scheduler.runTaskTimerAsynchronously(this, {
            cache.saveToDisk()
        }, saveInterval, saveInterval)
        
        logger.info("Japanese conversion system initialized")
    }
    
    override fun onDisable() {
        // Final save on shutdown
        cache.saveToDisk()
        logger.info("Cache saved on shutdown")
    }
}
```

## Notes

- **AsyncChatEvent** uses Paper's Component API - handle accordingly
- Command aliases must be properly registered
- Settings file location: `plugins/LunaticChat/settings/`
- Cache settings in memory to avoid frequent file I/O
- Cache file location: `plugins/LunaticChat/conversion-cache.json`
- All disk I/O is async to prevent blocking game thread

## Performance Considerations

### Memory Usage
- 500 entries × ~50 bytes average = ~25KB
- Parse-time memory consumption: < 100KB
- Negligible impact on Minecraft server

### Disk I/O
- **Startup**: Once (< 10ms for 500 entries)
- **Runtime**: Periodic saves every 5 minutes (async)
- **Shutdown**: Once (final save)

### Network
- No network calls after cache hit
- Each unique phrase calls Google API only once

## Development Environment

- Shell: Fish
- Java: 21+
- Gradle: 9+
- Kotlin: 2.3.0+

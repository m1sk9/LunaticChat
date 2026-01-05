# RuneChat - Design Document

## Project Overview

RuneChat is a Minecraft chat plugin providing 1on1 messaging, quick reply functionality, and romaji-to-Japanese conversion features.

## Technology Stack

- **Language**: Kotlin
- **Target Platforms**:
    - Paper (Minecraft 1.21.x+)
    - Velocity (planned for future)
- **Build Tool**: Gradle (multi-project setup)

## Core Principles

1. **Always support the latest version** while maintaining backward compatibility (e.g., 1.21.x)
2. **Maintainability**: Design for extensibility and easy maintenance
3. **Use Paper's LifecycleEventManager** for command registration (not RuneCore's implementation)
4. **Chat logs must be compatible** with CoreProtect and similar logging plugins

## Project Structure

```
rune-chat/
├── common/          # Shared code (data models, interfaces)
├── paper/           # Paper plugin implementation
├── velocity/        # Velocity plugin implementation (future)
└── core/            # Core logic (chat processing, romaji conversion)
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

**Implementation Strategy**:
1. **Phase 1**: Implement with Kotlin sealed classes
    - Define romaji mappings as sealed class hierarchy
    - If this becomes unmanageable, proceed to Phase 2
2. **Phase 2**: Use Map-based approach
3. **Phase 3**: External API with request limiting and caching

**Example sealed class structure**:
```kotlin
sealed class RomajiMapping {
    abstract val romaji: String
    abstract val hiragana: String
    
    data object A : RomajiMapping() {
        override val romaji = "a"
        override val hiragana = "あ"
    }
    // ... more mappings
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
        val converted = romajiConverter.convert(/* message */)
        // Modify message (don't cancel event for CoreProtect compatibility)
    }
}
```

## Notes

- **AsyncChatEvent** uses Paper's Component API - handle accordingly
- Command aliases must be properly registered
- Settings file location: `plugins/RuneChat/settings/`
- Cache settings in memory to avoid frequent file I/O

## Development Environment

- Shell: Fish
- Always use Japanese punctuation (，．) in responses.

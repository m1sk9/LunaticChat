# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LunaticChat is a Minecraft chat plugin supporting Paper, Folia, and Velocity servers. It provides direct messaging, channel chat, romaji-to-Japanese conversion, and cross-server global chat via Velocity proxy.

- **Language**: Kotlin (JVM 21)
- **Build**: Gradle 9+ with Kotlin DSL
- **Tooling**: mise (Java zulu-21, Bun 1.3.9)

## Common Commands

### Build
```bash
./gradlew clean build                    # Full build
./gradlew :platform-paper:shadowJar      # Paper/Folia plugin JAR
./gradlew :platform-velocity:shadowJar   # Velocity plugin JAR
```

### Test
```bash
./gradlew test                           # Run all tests
./gradlew :engine:test                   # Run engine tests only
./gradlew :platform-paper:test           # Run paper tests only
./gradlew :platform-paper:test --tests "*ChannelManagerTest"  # Single test class
```

### Lint
```bash
./gradlew ktlintCheck                    # Check style
./gradlew ktlintFormat                   # Auto-format
```

### Debug Server (Docker)
```bash
./x start         # Velocity + 2 Paper servers
./x folia start   # Single Folia server
./x rcon s1       # RCON console to Paper s1
./x help          # All available commands
```

## Architecture

### Module Structure

```
engine/             → Platform-agnostic core (models, converters, protocol, exceptions)
platform-paper/     → Paper & Folia plugin (commands, listeners, config, services)
platform-velocity/  → Velocity proxy plugin (message relay between servers)
dokka/              → API documentation aggregator (no Kotlin source)
docs/               → VitePress documentation site
```

**Dependency flow**: `platform-paper` and `platform-velocity` both depend on `engine`. The engine has no Minecraft platform dependencies.

### Key Architectural Patterns

**Service Container** (`platform-paper`): All services are held in an immutable `ServiceContainer` data class. `ServiceInitializer` handles initialization order and conditional feature setup. Optional features (channel chat, Velocity integration, Japanese conversion) are nullable fields.

**Feature Gating**: Features are toggled via `config.yml`. The `ServiceInitializer` conditionally creates services based on config, and the `ServiceContainer` holds them as nullable properties.

**Plugin Messaging Protocol**: Cross-server communication uses a custom `PluginMessageCodec` in the engine module. Paper servers encode/decode messages via this codec, and Velocity relays them between servers.

### Key Packages (engine)

| Package | Purpose |
|---------|---------|
| `chat.channel` | Channel data model and validation |
| `converter` | Google IME API client for romaji conversion |
| `exception` | Custom exception hierarchy (23 types) |
| `protocol` | Plugin messaging codec for Velocity communication |
| `settings` | Player settings models with UUID serialization |

### Key Packages (platform-paper)

| Package | Purpose |
|---------|---------|
| `chat.handler` | DirectMessage, ChannelMessage, ChannelNotification handlers |
| `command` | Command registration and execution |
| `config` | YAML config loading via KAML |
| `i18n` | Language support (EN, JA) |
| `listener` | Event listeners (chat, join/quit, plugin messages) |
| `velocity` | Cross-server integration |

## Code Conventions

- Follows [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html), enforced by Ktlint
- PRs must pass `./gradlew ktlintCheck`
- Tests use JUnit 5 + MockK; coroutine tests use `kotlinx-coroutines-test`
- Shadow JAR output: `LunaticChat-{version}.jar` (Paper), `LunaticChat-{version}-velocity.jar` (Velocity)
- Serialization uses kotlinx-serialization with KAML for YAML config files

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LunaticChat is a Minecraft chat plugin supporting Paper, Folia, and Velocity servers. It provides direct messaging, channel chat, and romaji-to-Japanese conversion. Velocity proxy support relays global chat and (optionally) direct messages across backend servers.

- **Language**: Kotlin (JVM 25)
- **Build**: Gradle 9+ with Kotlin DSL
- **Tooling**: mise provisions Java and Bun (versions in `mise.toml`)
- **Docs site**: VitePress in `website/`, deployed to Cloudflare Workers (`lc.m1sk9.dev`)

## Common Commands

### Build
```bash
./gradlew clean build                    # Full build (build depends on shadowJar)
./gradlew :platform-paper:shadowJar      # Paper/Folia plugin JAR
./gradlew :platform-velocity:shadowJar   # Velocity plugin JAR
```

### Test
```bash
./gradlew test                           # Run all tests
./gradlew :engine:test                   # Run engine tests only
./gradlew :platform-paper:test --tests "*ChannelManagerTest"  # Single test class
./gradlew test jacocoTestReport          # What CI runs (coverage goes to Codecov)
```

### Lint
```bash
./gradlew ktlintCheck                    # Check style
./gradlew ktlintFormat                   # Auto-format
```

### Documentation site (`website/`, Bun)
```bash
bun install
bun run dev                              # VitePress dev server
bun run build                            # Also what CI verifies
bun run check                            # Biome format + lint, writing fixes
bun run format:check                     # What CI runs (biome ci .)
```

### API docs (Dokka)
```bash
./gradlew :dokka:dokkaGenerate           # Aggregated HTML into dokka/build/dokka/html
```

### Debug server (Docker)

`./x <action> <platform> [--stable]` — the platform argument is required.

```bash
./x start velocity   # Velocity proxy + Paper s1 & s2 (localhost:25577)
./x start paper      # Single Paper server (localhost:25565)
./x start folia      # Single Folia server (localhost:25565)
./x rcon velocity s1 # RCON console to backend s1
./x rcon paper       # RCON console to the single Paper server
./x log velocity     # Follow logs
./x clean folia      # Stop and drop volumes
./x help
```

Builds default to a nightly version derived from the git short hash; `--stable` builds a stable release. Server versions are derived from the `paper-api` / `velocity-api` coordinates in the module `build.gradle.kts` files — those are the single source of truth, so never duplicate a Minecraft version into the compose files.

## Architecture

### Module Structure

```
engine/             → Platform-agnostic shared kernel (models, protocol, permissions, exceptions)
platform-paper/     → Paper & Folia plugin (commands, listeners, config, services, converter)
platform-velocity/  → Velocity proxy plugin (cross-server relay)
dokka/              → API documentation aggregator (no Kotlin source)
website/            → VitePress documentation site
```

**Dependency flow**: `platform-paper` and `platform-velocity` both depend on `engine` as equal peers. `engine` has no Minecraft, Adventure, or Brigadier dependency — its only dependency is `kotlinx-serialization-json`, exposed via `api()`. Keep it that way: `engine` must stay testable on a bare JVM.

`engine` exists to be the single source of truth for anything both sides must agree on — the wire protocol, persistence schemas, permission nodes, `CommandResult`, and the domain exception vocabulary.

### Key Packages

| Module | Package | Purpose |
|--------|---------|---------|
| engine | `protocol` | `PluginMessage`, `PluginMessageCodec`, `ProtocolVersion` — the Paper↔Velocity wire contract |
| engine | `chat.channel` | Channel domain model, roles, log entries (`@Serializable` persistence schema) |
| engine | `settings` | Player settings DTOs with UUID serialization |
| engine | `permission` | `LunaticChatPermissionNode` — permission node strings, referenced by type from `@Permission` |
| engine | `command` | `CommandResult` sealed hierarchy (`toBrigadierResult()` returns `Int`, no Brigadier dependency) |
| engine | `exception` | Domain exception hierarchy (mostly channel errors) |
| paper | `command` | Annotation-driven Brigadier command framework (`core`, `annotation`, `impl`, `setting`) |
| paper | `chat.handler` | DirectMessage / ChannelMessage / ChannelNotification handlers |
| paper | `chat.channel` | `ChannelManager` (source of truth), membership, storage, NDJSON logger |
| paper | `converter` | Romaji→Japanese: `KanaConverter`, `GoogleIMEClient` (Ktor), `ConversionCache` |
| paper | `config` | `config.yml` → `LunaticChatConfiguration` via KAML |
| paper | `velocity` | Plugin-messaging client, cross-server chat/DM, remote player registry |
| paper | `i18n` | `LanguageManager` (KAML), `MessageFormatter`, `ChatFormat` |
| velocity | `messaging` | `PluginMessageHandler`, cross-server chat and DM relays |
| velocity | `presence` | `PresenceTracker` — which backend a player is on |

### Key Architectural Patterns

**Service Container + Feature Gating** (`platform-paper`): manual DI, no framework. `ServiceInitializer` constructs services in dependency order and owns shutdown; the immutable `ServiceContainer` data class holds them. A disabled feature's service is `null`, so feature presence is expressed in the type system and command/listener/setting registration branches on null checks. Adding a feature means: config flag → conditional construction in `ServiceInitializer` → nullable field on `ServiceContainer` → null-checked registration. Never reintroduce `!!` to work around this.

**Annotation-driven commands**: `@Command(name, aliases, description)`, `@Permission(KClass<out LunaticChatPermissionNode>)`, and `@PlayerOnly` are read via Kotlin reflection (hence the `kotlin-reflect` dependency) and mapped onto a Brigadier tree. Root commands extend `LunaticCommand` and carry `@Command`; nested subcommands extend `LunaticSubCommand` and declare `literal` / `permissionNode` / `aliases` as properties instead. `CommandRegistry` builds the trees inside Paper's `LifecycleEvents.COMMANDS`. Handlers return `CommandResult`; `handleResult()` turns it into an Adventure message plus the Brigadier int.

**Chat routing** (`PlayerChatListener`): a leading `!` forces global. Active channel and no `!` → cancel the event and deliver via `ChannelMessageHandler` (local to the server). Otherwise → global chat, additionally relayed through Velocity when cross-server chat is enabled. Channel chat is always server-local; global chat and (when `crossServerDirectMessage` is on) direct messages cross the proxy.

**Folia-safe concurrency**: async work goes through `plugin.server.asyncScheduler` or `PluginCoroutineScope` (SupervisorJob); Bukkit API calls come back via `scheduler.runTask`. `PerPlayerWorkQueue` serializes each player's outgoing messages so a cached conversion cannot overtake an uncached one. Never block the tick thread — the only sanctioned `runBlocking` is the bounded `withTimeoutOrNull` in `RomajiConversionHelper`.

**Persistence**: in-memory cache + debounced async write (`DebouncedSaver`) + synchronous save on shutdown, with `Path.writeTextAtomically()` for every write, since a shutdown save and a pending debounced save can race on the same file. Formats: KAML/YAML for languages and player settings, JSON for `channels.json` and the conversion cache, NDJSON for channel logs.

**Config robustness**: `ConfigManager` deserializes `config.yml` with KAML and, on a failure, prunes the offending key and retries so only that setting falls back to its default. A malformed config must never throw out of `onEnable` and disable the plugin. `LenientBoolean` keeps YAML 1.1 spellings (`yes`/`no`/`on`/`off`) working, since Bukkit used to accept them.

**i18n**: `resources/languages/{en,ja}.yml` are loaded and flattened to dotted keys. Lookup falls back selected-language → EN; a missing `en.yml` entry is fatal. Add every new user-facing string to both files.

## Versioning & Release

Paper and Velocity have independent versions in `gradle.properties` (`paperVersion`, `velocityVersion`).

| Tag Pattern | Workflow | Target |
|-------------|----------|--------|
| `paper/v1.3.0` | `release-paper.yaml` | Paper/Folia JAR only |
| `velocity/v1.2.0` | `release-velocity.yaml` | Velocity JAR only |
| `v1.3.0` | `release.yaml` | Both (e.g. engine changes) |

Use the `release` skill (`.claude/skills/release/SKILL.md`) to run the pre-release checks and propose tags.

### Protocol Version (`engine/protocol/ProtocolVersion.kt`)

Paper–Velocity compatibility is gated by protocol version only; plugin version is never used for compatibility checks. MAJOR must match exactly, remote MINOR must fall in `MIN_SUPPORTED_MINOR..MINOR`, PATCH is ignored.

| Level | When to bump | Deployment order |
|-------|-------------|-----------------|
| **PATCH** | Optional fields with defaults, ignorable new sub-channels | Any order |
| **MINOR** | Required fields, sub-channels whose absence degrades behavior | Velocity first, then Paper |
| **MAJOR** | Removals, renames, wire format changes | Simultaneous |

Adding a message type: data class in `PluginMessage` → constant in `PluginMessageCodec.SubChannel` → encode/decode branches → a snapshot in `ProtocolBackwardCompatibilityTest`. Backward compatibility rests on `ignoreUnknownKeys = true` plus defaulted fields; protocol changes must ship with snapshot tests.

## Code Conventions

- Follows [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html), enforced by Ktlint; PRs must pass `./gradlew ktlintCheck`
- Comments explain **why not** — a constraint, trade-off, or deliberately avoided approach that the code cannot state itself. The existing comments in `AtomicWrite.kt`, `ConfigManager.kt`, and `PerPlayerWorkQueue.kt` are the reference style
- Tests use JUnit 5 + MockK; coroutine tests use `kotlinx-coroutines-test`
- Serialization is kotlinx-serialization, with KAML for YAML
- Shadow JAR output: `LunaticChat-{version}.jar` (Paper), `LunaticChat-{version}-velocity.jar` (Velocity)
- New Velocity-side dependencies are weighed against JAR size: `platform-velocity` deliberately pulls in no Ktor

## Deeper Documentation

`website/src/docs/developers/` holds long-form design docs (`architecture.md`, `engine.md`, `platform-paper.md`, `platform-velocity.md`, `resource.md`). Read them for rationale, but verify against the source — they lag behind refactors (they still place `converter` in `engine` and describe `ConfigManager` as reading Bukkit's `FileConfiguration`). Update them in the same change when you move a boundary they describe.

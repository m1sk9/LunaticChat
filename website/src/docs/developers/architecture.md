---
layout: doc
---

# Design Overview

Alongside direct messaging, channel chat, and romaji conversion on Paper/Folia, LunaticChat provides **cross-server global chat relay** behind a Velocity proxy.

This page covers the big picture and the cross-cutting design decisions that span modules. See the per-module pages for details.

## Module Structure

A Gradle multi-module setup separates the shared kernel from the platform implementations.

The dependency direction is one-way: both `platform-paper` and `platform-velocity` depend on `engine`, and `engine` depends on nothing downstream. Neither platform "owns" the protocol — both depend on the neutral `engine` as equal peers.

| Module | Role |
|--------|------|
| `engine` | Platform-independent core (domain models, protocol, conversion, exceptions, permissions) |
| `platform-paper` | Paper / Folia plugin |
| `platform-velocity` | Velocity proxy plugin (cross-server chat relay) |
| `dokka` | API documentation aggregator (no Kotlin source) |

### Why extract the engine module

`engine` is a **Shared Kernel**. Its contents fall into two categories.

#### (a) Contracts both sides must agree on

Things that break unless Paper and Velocity share the exact same definition.

- `protocol` — the wire contract between the two processes (communication breaks without identical definitions)
- `chat` / `channel`, `settings` — persistence schemas (`@Serializable`)
- `exception` — the shared vocabulary of domain errors
- `permission`, `command` — neutral abstractions for permission node strings and command results

#### (b) Platform-independent pure logic

Logic that could live anywhere, but is pulled into the neutral core because it is pure and reusable.

- `converter` — the pure romaji-conversion algorithm (Trie) plus an external API client

The primary goal of centralizing (a) in `engine` is to create a **single source of truth for the wire contract**. Paper and Velocity are two artifacts built, deployed, and versioned separately; duplicating the protocol in both modules would inevitably drift. With a single definition in `engine`, a contract mismatch surfaces early as a compile error or a snapshot-test failure rather than a runtime mismatch in production.

`engine` depends on no Bukkit / Velocity API, and borrows only the "meaning of types and values" from Adventure / Brigadier to avoid depending on their runtimes (`compileOnly` Adventure, and `toBrigadierResult()` returning an `Int` without depending on Brigadier itself). This lets `engine` be tested on a pure JVM without spinning up a Minecraft server, while platform concerns (the Folia scheduler, etc.) stay isolated in the platform modules.

## Compatibility via the protocol version

Paper–Velocity compatibility is determined solely by the **`ProtocolVersion`** held in `engine`, not by the plugin version. This is the linchpin of LunaticChat's multi-platform design.

- Compatibility check: MAJOR must match exactly, the remote MINOR must be within `[MIN_SUPPORTED_MINOR, MINOR]`, and PATCH is ignored
- Backward compatibility: JSON `ignoreUnknownKeys` plus fields with default values
- `ProtocolBackwardCompatibilityTest` verifies backward compatibility mechanically via JSON snapshots

Decoupling compatibility from the plugin version means **Paper and Velocity can be versioned independently, each released at its own pace** even though the two platforms change at different rates. Update ordering is also defined per bump level (PATCH = any order / MINOR = Velocity first / MAJOR = simultaneous), which is what makes rolling updates possible.

For details, see [engine - Shared Kernel](/docs/developers/engine).

## Service Container pattern + Feature Gating

`platform-paper` assembles its features via manual DI, without an external DI framework.

- `ServiceInitializer` handles construction, initialization order, and shutdown
- `ServiceContainer` (an immutable data class) holds the services
- **A disabled feature's service is `null`**, so the presence of a feature is expressed in the type
- Command, listener, and SettingHandler registration branches on `null` checks

In short: "config flag → `ServiceInitializer` creates a nullable service → nullable field on `ServiceContainer` → registration branches on a `null` check." When a feature is disabled, its service simply does not exist at the type level, and the corresponding code path is never built. Feature toggling and lifecycle management are expressed purely through Kotlin's type system and null-safety, with no external framework.

For details, see [platform-paper - Paper / Folia Plugin](/docs/developers/platform-paper).

## Cross-cutting design traits

1. **engine / platform separation of concerns** — the platform layer is a bridge to the platform API, while domain models, algorithms, and the protocol live in `engine`. The platform side is an adapter layer that absorbs "the reality of Bukkit / Velocity".
2. **Compatibility via the protocol version** — compatibility is decided by `ProtocolVersion` alone.
3. **Service Container + Feature Gating** — the presence of a feature is expressed in the type.
4. **Annotation-driven commands** — `@Command` / `@Permission` / `@PlayerOnly` are read via Kotlin reflection and mapped onto the Brigadier tree. A command's definition and its metadata (permission, aliases) are declared together in one place.
5. **Folia compatibility** — asynchronous work runs on `asyncScheduler` and `PluginCoroutineScope` (SupervisorJob), and Bukkit API calls are moved back to the main thread via `scheduler.runTask`. Thread boundaries are handled explicitly so it also works on region-threaded Folia.
6. **Persistence chosen per purpose** — languages / player settings = KAML (YAML), channels / conversion cache = kotlinx.serialization JSON, channel logs = NDJSON. All follow the same pattern: in-memory cache + asynchronous save (debounce/queue) + synchronous save on shutdown.
7. **DM/channel = local, global = via the proxy** — routing differs by chat type; only global chat goes through Velocity. The relay prevents loops in two stages: "exclude the source server" + "deduplicate by messageId".

## Module details

- [engine - Shared Kernel](/docs/developers/engine)
- [platform-paper - Paper / Folia Plugin](/docs/developers/platform-paper)
- [platform-velocity - Velocity Plugin (Proxy Relay)](/docs/developers/platform-velocity)
- [Build, Release & Versioning](/docs/developers/resource)

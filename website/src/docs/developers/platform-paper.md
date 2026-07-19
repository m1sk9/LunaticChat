---
layout: doc
---

# platform-paper - Paper / Folia Plugin

`platform-paper` is the plugin itself.

It is the layer that bridges to the platform APIs — Bukkit / Paper / Folia, Adventure, Brigadier, Plugin Messaging — and delegates domain models, algorithms, and the protocol to [engine](/docs/developers/engine).

The paper side acts as an adapter absorbing "the reality of Bukkit / Folia", connecting `engine`'s pure models to platform concerns (scheduler, threads, events).

## Entry point and DI (Service Container)

Features are assembled via manual DI, without an external DI framework. The key idea is **separating "the responsibility of construction" from "the responsibility of holding"**.

- `LunaticChat` (`JavaPlugin` + `Listener`) — the plugin entry point
- `ServiceInitializer` — handles service construction, initialization order, and shutdown
- `ServiceContainer` — an immutable `data class` holding the constructed services
- `PluginCoroutineScope` — `SupervisorJob` + `Dispatchers.Default`; used for non-blocking work such as `UpdateChecker`

### Lifecycle

The `onEnable` flow:

1. `saveDefaultConfig()` → build `LunaticChatConfiguration` via `ConfigManager`
2. Initialize `HttpClient(CIO)` and `PluginCoroutineScope`
3. `ServiceInitializer.initialize()` → receive a `ServiceContainer`
4. Move services into the public properties used by commands
5. `schedulePeriodicTasks()` → `registerCommands()` → `registerEventListeners()`
6. Start `UpdateChecker` if `checkForUpdates` is enabled

`onDisable` runs `pluginScope.cancel()` → `serviceInitializer.shutdown()`, closing settings, caches, channels, logs, and the Velocity connection in order.

### ServiceContainer and ServiceInitializer

`ServiceContainer` holds always-available services (`languageManager` / `playerSettingsManager` / `directMessageHandler`) as non-null, and feature-gated ones (`channelManager` / `velocityConnectionManager`, etc.) as nullable fields (default null). The aim is to eliminate null-assertions (`!!`) from the codebase.

`ServiceInitializer.initialize()` creates services in dependency order.

1. `LanguageManager` (before commands; a prerequisite for all features)
2. `PlayerSettingsManager` (always needed, e.g. for DM notifications)
3. Japanese conversion (optional)
4. Channel group — `ChannelManager` / `ChannelMembershipManager` / `ChannelMessageHandler` / `ChannelNotificationHandler`, plus `ChannelMessageLogger` when logging is enabled (optional)
5. `DirectMessageHandler` (depends on settings, romaji, language)
6. Velocity integration (optional)
7. Cross-server chat (only when velocity is enabled, `crossServerGlobalChat` is on, and the velocity manager is non-null)

### Feature Gating

This `initialize()` is where feature toggling actually happens. Japanese conversion / Channel group / Velocity integration / Cross-server chat are **created only when their config flag is true, and are `null` otherwise**.

```
config flag
  → ServiceInitializer creates a nullable service
  → stored in a nullable field on ServiceContainer
  → command / listener / SettingHandler registration branches on a null check
```

A disabled feature's service simply does not exist at the type level, and its code path is never built. The presence of a feature is expressed through Kotlin's null-safety.

For the design rationale, see the [Design Overview](/docs/developers/architecture#service-container-pattern-feature-gating).

## Command framework (annotation-driven + Brigadier)

A command's definition and its metadata (permission, aliases, player-only) are declared together in one place, then **read via Kotlin reflection and mapped onto the Brigadier tree**.

### Annotations

- `@Command(name, aliases, description)` — command name, aliases, description
- `@Permission(KClass<out LunaticChatPermissionNode>)` — required permission (specified by type via the engine's permission node)
- `@PlayerOnly` — a player-only marker

### LunaticCommand

The abstract base for all commands. It lazily reads the annotations on the class, and `buildWithChecks()` wraps the subclass's `buildCommand()` to inject shared behavior.

- If `@Deprecated` is present, it swaps in a handler that returns an error message at runtime
- If `@Permission` is present, it attaches Brigadier's `.requires { source.sender.hasPermission(perm) }`
- `handleResult()` converts the engine's `CommandResult` into an Adventure message plus the `Int` from `toBrigadierResult()`
- `withAliases()` clones a Brigadier node to create alias nodes, and `applyMethodPermission()` reflects a **method-level** `@Permission`

### CommandRegistry

`register` / `registerAll` accumulate commands, and `initialize()` registers a handler on Paper's `LifecycleEvents.COMMANDS`. The actual Brigadier tree construction (`buildWithChecks().build()`) happens inside that lifecycle event.

### Convention: root and nested subcommands

- **Root command** — annotate the class with `@Command`
- **Nested subcommand** — no `@Command`; apply permission via a `build()` method plus a method-level `@Permission` and `applyMethodPermission("build", …)`

### Command hierarchy

| Command | Aliases | Registration condition |
|---------|---------|------------------------|
| `lc` (→ settings / status / channel) | `lunaticchat` | Always |
| `channel` (14 subcommands) | `ch` | When channelChat is enabled |
| `tell` | `t` / `msg` / `m` / `w` / `whisper` | Always |
| `reply` | `r` | When quickReplies is enabled |
| `lcv` (→ status) | `lunaticvelocity` | When velocity is enabled |

`settings` iterates `SettingKey.values()` to dynamically generate on/off/status nodes for each key and delegates to `SettingHandlerRegistry`. Adding a setting is a three-step process: "add a SettingKey → implement a Handler → register it in the Registry".

## Chat processing

### Routing (PlayerChatListener)

This is where routing happens, deciding **"local (channel) vs. global (possibly via the proxy)"**. It hooks `AsyncChatEvent` at `EventPriority.HIGHEST, ignoreCancelled = true`.

Flow:

1. Serialize the message to plain text and check for a leading `!` (the force-global prefix)
2. If it is `!` with an empty body, cancel the event and return (don't emit an empty message)
3. If the sender has romaji conversion enabled, run it through `convertWithRomaji`
4. Determine whether the player has an active channel via `channelManager.getPlayerChannel()`

Branches:

- **Active channel and no `!`** → `event.isCancelled = true` + `viewers().clear()` + `message(empty)` to stop normal chat, then route to `ChannelMessageHandler.sendChannelMessage()` (local to the server)
- **Otherwise** (no active channel, or a `!` prefix) → `handleGlobalChat()`. If velocity cross-server is enabled, send to `CrossServerChatManager.sendGlobalMessage()` while also displaying normal chat; otherwise, normal chat only

### Direct messages (DirectMessageHandler)

Manages `/tell`・`/reply` state. Two `ConcurrentHashMap`s, `lastMessager` / `lastRecipient`, track reply targets, and `getReplyTarget()` returns an online player in the order "whoever messaged me → whoever I messaged".

`sendDirectMessage()` applies romaji conversion per the sender's settings → delivers a hover-annotated copy to spy players (excluding sender and recipient) → sends the formatted message to sender and recipient plus a notification sound (settings-dependent). The message carries a `ClickEvent.suggestCommand` that fills in `/tell <sender>`.

### Channel chat (ChannelMessageHandler)

`sendChannelMessage()` resolves the active channel via `channelManager.getPlayerChannelContext()` (doing nothing if absent), then delivers to spies (excluding the sender and members) → delivers to all channel members plus a receiver notification sound → writes an NDJSON log via the engine's `ChannelMessageLogEntry.create()` when logging is enabled.

Channel state itself is managed by the `chat/channel` package.

- `ChannelManager` — the single source of truth for channels. It holds state in `channelsCache` / `membersCache` / `activeChannels` (`ConcurrentHashMap`), and its CRUD returns `kotlin.Result`, wrapping engine exceptions on failure. It checks config limits (0 = unlimited)
- `ChannelMembershipManager` — the business logic for join/leave/switch/role. `joinChannel()` checks existence / already-active / BAN / private-invite / already-a-member / membership limit in order
- `ChannelStorage` — persists `ChannelData` as JSON (`channels.json`)
- `ChannelMessageLogger` — an asynchronous NDJSON logger with daily rotation, a size cap, and periodic deletion of files past the retention period

## Listener registration

- `EventListenerRegistry` (`object`) — `SpyPermissionManager` and `PlayerPresenceListener` are always registered; `PlayerChatListener` is registered only when channel / velocity cross-server / romaji is enabled (Feature Gating again)
- `PlayerPresenceListener` — on Join: update notification, nightly warning, active-channel restoration notice; on Quit: clear DM references, deactivate the active channel, and save settings
- `SpyPermissionManager` (`object : Listener`) — caches holders of the `Spy` permission on join/quit; referenced by the DM and channel handlers

## config

- `ConfigManager` — reads the main `config.yml` from **Bukkit's `FileConfiguration`** by dotted keys and hand-assembles `LunaticChatConfiguration` (note: this path is not KAML)
- Feature defaults: `quickReplies=true`, `japaneseConversion=false`, `channelChat=false`, `velocityIntegration=false`
- Under `config/key`: `FeaturesConfig` / `ChannelChatFeatureConfig` / `JapaneseConversionFeatureConfig` / `VelocityIntegrationConfig` / `QuickRepliesFeatureConfig` / `MessageFormatConfig` / `ChannelMessageLoggingConfig`

::: warning Implementation note
`ChannelChatFeatureConfig.messageLogging` is not loaded by `ConfigManager` and stays at its default values (enabled=true, retention=30, 100MB). Whether this is intentional needs confirmation — decide whether to fix it or document it as intended behavior.
:::

## i18n

- `Language` (enum) — `EN` / `JA`; unknown codes fall back to EN
- `LanguageManager` — loads `resources/languages/` with KAML at startup and flattens the nested YAML into dotted keys (`toggle.on`, etc.). `getMessage(key, placeholders)` resolves with selected-language → EN fallback and substitutes `{placeholder}`, returning the key itself if not found. A missing EN is a fatal error
- `MessageFormatter` (`object`) — produces an Adventure `Component` with a `[LC]` prefix and highlights `{braces}` placeholders detected by regex

## converter (paper side) — engine integration

The paper side handles the platform concerns of "cache management, timeouts, Bukkit scheduling", and delegates the conversion algorithm and API calls to `engine`.

- `RomanjiConverter` — the two-stage conversion orchestrator. Per word: cache lookup → engine `KanaConverter` for romaji→hiragana → engine `GoogleIMEClient` for hiragana→kanji. Falls back to hiragana on API failure
- `ConversionCache` — persists engine `CacheData` as JSON. In-memory cache plus debounced save (a FIXME notes that eviction on `maxEntries` overflow is effectively random due to `ConcurrentHashMap` ordering)
- `RomajiConversionHelper` — `convertWithRomaji()`. Calls synchronously via `runBlocking` + `withTimeoutOrNull` (default 1000ms), returning `"original §e(converted)"` on success and the original text on failure/timeout

## Velocity integration (Paper side)

Using the engine's protocol, it communicates with the proxy over Bukkit's Plugin Messaging Channel (`lunaticchat:main`). The actual cross-server routing is handled by the Velocity side; paper is responsible for "sending, receiving, deduplication, and formatted display".

- `VelocityConnectionManager` (`PluginMessageListener`) — manages `ConnectionState` (DISCONNECTED / HANDSHAKING / CONNECTED / FAILED). It encodes and sends the engine's `PluginMessage.Handshake`, timing out after 5 seconds. To avoid a circular dependency, `CrossServerChatManager` is injected afterward (setter injection)
- The handshake runs **only once, triggered by the first player join** (`AtomicBoolean`). It is scheduled 1 second after the join via `asyncScheduler`, and the result is received as `HandshakeResult.Success` / `Error`
- `CrossServerChatManager` — the send/receive and **deduplication** of global chat. On send, it registers the generated `messageId` in the cache immediately to prevent an echo on its own server (stage one); on receive, it prevents duplicate display with a dedup cache keyed by `messageId` (TTL 60s, oldest-first cleanup when over `cacheSize`). Bukkit API calls are moved to the main thread via `scheduler.runTask`

## settings / common

- `PlayerSettingsManager` — manages three boolean settings in `ConcurrentHashMap`s. Uses the engine DTOs; unset values default to true
- `YamlPlayerSettingsStorage` — reads/writes `player-settings.yaml` with KAML. Recovers from a backup on load failure; debounced save (5s)
- `UpdateChecker` — hits the GitHub Releases API via Ktor and compares semver. The result is a sealed `UpdateCheckResult`
- `SoundCollector` — Adventure `Sound` constants for notifications plus Player extension functions
- `PermissionCollector` — a DSL that collects permissions via `@PermissionDsl` + the `+LunaticChatPermissionNode` operator. `requirePermission` throws the engine's `RequirePermissionException`

## Related

- [Design Overview](/docs/developers/architecture)
- [engine - Shared Kernel](/docs/developers/engine)
- [platform-velocity - Velocity Plugin](/docs/developers/platform-velocity)

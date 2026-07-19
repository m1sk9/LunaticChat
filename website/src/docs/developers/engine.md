---
layout: doc
---

# engine - Shared Kernel

`engine` is the platform-independent core module.

It is positioned as a **Shared Kernel** that gathers the contracts Paper and Velocity share (protocol, schemas, vocabulary) together with platform-independent pure logic (the conversion algorithm).

It depends on no Bukkit / Velocity API, and borrows only the "meaning of types and values" from Adventure / Brigadier without depending on their runtimes. That is why it can be tested on a pure JVM without spinning up a Minecraft server.

For the full rationale behind extracting `engine`, see the [Design Overview](/docs/developers/architecture#why-extract-the-engine-module).

## protocol — Paper ↔ Velocity communication

Paper and Velocity are separate-process artifacts that communicate via plugin messaging. `protocol` lives in `engine` **so that both sides share the exact same wire contract**. Since changing the definition on only one side breaks communication, a single definition is kept in `engine` so mismatches can be caught at compile time and in tests.

There are five message types, headed by `sealed interface PluginMessage`.

| Type | Direction | Key fields |
|------|-----------|-----------|
| `Handshake` | Paper→Velocity | `pluginVersion`, `protocol` components |
| `HandshakeResponse` | Velocity→Paper | `compatible`, `velocityVersion`, `error?`, `protocol` components |
| `StatusRequest` | Paper→Velocity | (no fields) |
| `StatusResponse` | Velocity→Paper | `velocityVersion`, `protocolVersion`, `online` |
| `GlobalChatMessage` | Paper↔Velocity↔Paper | `messageId`, `serverName`, `playerId`, `playerName`, `message`, `timestamp` |

`GlobalChatMessage.messageId` is a unique ID that prevents duplicate display during relay loops. Note also that the protocol layer carries UUIDs as plain `String`s (in contrast to the `UUID` type plus custom serializer used in the settings/channel layers — this keeps transport simple).

### Wire format

- `[subChannel: UTF][messageJson: UTF]` — `DataOutputStream.writeUTF` writes the "sub-channel name" and the "JSON body", a `ByteArray` form convenient for Minecraft plugin messaging
- JSON via kotlinx-serialization. `Json { ignoreUnknownKeys = true }` means an older version won't break when it receives unknown fields added by a newer version (the basis for forward compatibility)
- Sub-channels: `handshake` / `handshake_response` / `status_request` / `status_response` / `global_chat`

### Versioning strategy (`ProtocolVersion`)

Paper–Velocity compatibility is judged by `ProtocolVersion` alone, not the plugin version. Following SemVer, the bump level and deployment order are determined by the nature of the change.

| Level | When to bump | Deployment order |
|-------|--------------|------------------|
| PATCH | Add an optional field with a default / an ignorable new sub-channel | Any order |
| MINOR | Add a required field / a sub-channel whose absence degrades functionality | Velocity → Paper |
| MAJOR | Remove/rename fields or sub-channels, or change the wire format | All simultaneously |

The compatibility check is "**MAJOR matches exactly, the remote MINOR is within `[MIN_SUPPORTED_MINOR, MINOR]`, and PATCH is ignored**". Raising `MIN_SUPPORTED_MINOR` lets you phase out acceptance of older MINOR versions. When adding a new message or field, add a JSON snapshot to `ProtocolBackwardCompatibilityTest` to mechanically guarantee that the old format keeps parsing.

As a consequence of this design, Paper and Velocity can be released independently. See [Build, Release & Versioning](/docs/developers/resource#independent-versioning).

## converter — Romaji-to-Japanese conversion

`converter` is not a Paper↔Velocity contract (Velocity does no romaji conversion); it lives in `engine` **because it is platform-independent pure logic**. It has three layers.

- `KanaConverter` (`object`) — converts romaji to hiragana with a **Trie**. An immutable structure of `sealed class TrieNode { Leaf, Branch }` covers mappings from 4 characters (`xtsu`→っ) down to 1 (`a`→あ). `isValidRomaji()` validates before conversion; `toHiragana()` is a pure algorithm using longest-match plus sokuon handling
- `GoogleIMEClient` — receives a Ktor `HttpClient` via DI and converts hiragana to kanji-kana via Google IME (`langpair=ja-Hira|ja`), concatenating the top candidate of each segment of the response
- `CacheData` (`@Serializable`) — the persistence schema for conversion results (`version` plus `entries: Map`). It is a container for caching the expensive IME conversions; the caching logic itself lives on the paper side

## chat/channel — Channel domain model

The channel persistence schemas are placed on the `engine` side as `@Serializable` models — used by the paper side that persists them, and kept sharable for the future.

- `Channel` — validated in `init` (`id` matches `^[a-zA-Z0-9_-]{3,30}$`, `name` must not be blank)
- `ChannelData` — the persistence root; a `version` field accommodates schema evolution
- `ChannelMember` / `ChannelRole` — members and roles; roles are the three tiers `OWNER` / `MODERATOR` / `MEMBER`
- `ChannelContext` — a non-Serializable runtime aggregate DTO (a view passing `channel` + `members` to operations)
- `ChannelMessageLogEntry` — a log entry designed for NDJSON, daily rotation, and Grafana Loki compatibility

Limits such as the number of channels, members, and memberships keep only the **vocabulary of exceptions** in `engine`, while the concrete thresholds are injected by config (paper side). This separates "that a limit exists" from "what the limit is".

## settings — Player settings and UUID serialization

The persistence model and the runtime model are separated.

- `PlayerSettingsData` — the YAML persistence root; holds three settings as UUID→Boolean maps
- `PlayerChatSettings` — a flat per-player model (all settings default to true); a runtime view projected from the whole map

There are two UUID serializers because they serve different purposes. `UUIDSerializer` (descriptor name `"UUID"`) is the general one, used by channel and `PlayerChatSettings.uuid`; `UUIDASStringSerializer` (descriptor name `"UUIDAsString"`) is used for the **map keys** of `PlayerSettingsData` for YAML compatibility. They are hand-written because `kotlinx.serialization` does not support UUID out of the box.

## exception — Shared error vocabulary

So that Paper and Velocity can handle domain errors as the same types, exceptions are centralized in `engine`. There is no common sealed base — it is a flat structure (23 types) that directly extends `Exception`. They fall into existence/reference, state, limit, and permission/BAN/KICK categories, and many take `playerId` / `channelId` / `limit` in the constructor and build their own messages. Because there is no base type, callers are expected to catch each individually.

## permission / command — Neutral abstractions

Permissions and command results are placed in `engine` as neutral representations that can be passed to either the Bukkit or Velocity API.

- `LunaticChatPermissionNode` — permissions enumerated type-safely as `sealed class` + `object` subclasses. The string node can be passed to either platform's permission API, and `when` also gives exhaustiveness checking
- `CommandResult` — a `sealed class` (`Success` / `SuccessWithMessage` / `Failure` / `InvalidUsage`). The message is an Adventure `Component`, and `toBrigadierResult()` expresses only "the meaning of the return value" (success=1/failure=0) without depending on Brigadier itself

## Related

- [Design Overview](/docs/developers/architecture)
- [platform-paper - Paper / Folia Plugin](/docs/developers/platform-paper)
- [platform-velocity - Velocity Plugin](/docs/developers/platform-velocity)

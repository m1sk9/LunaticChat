---
layout: doc
---

# platform-velocity - Velocity Plugin (Proxy Relay)

`platform-velocity` is a thin layer whose only job is to **relay** cross-server global chat.

Its substance is just `LunaticChat` / `BuildInfo` / two files under `messaging/`; it has no command classes. Note that while `/lcv` is a Velocity-related feature, the command implementation lives on the [platform-paper](/docs/developers/platform-paper) side.

::: tip Why the Velocity side is thin
The protocol definition is held by `engine`, and all chat state (channels, DMs, settings) lives on the Paper side. The only responsibility left to Velocity is "distribute a received global chat message to the other servers", so this layer is intentionally kept thin. Neither platform owns the protocol; both depend on `engine` as equal peers (see the [Design Overview](/docs/developers/architecture#why-extract-the-engine-module)).
:::

## Lifecycle

- `LunaticChat` (`@Plugin`) — receives `ProxyServer` / `Logger` / `PluginContainer` via a Guice `@Inject` constructor
- The `version` in the `@Plugin` annotation is fixed at `"0.0.0"` and **is not used at runtime**. The real version is obtained from `velocity-plugin.json` via `PluginContainer.description.version` (startup fails if it is missing)
- `@Subscribe onProxyInitialization` creates `CrossServerChatRelay`, then creates and `initialize()`s `PluginMessageHandler` with it injected
- `@Subscribe onProxyShutdown` calls `messageHandler.shutdown()`

## Message reception and dispatch

`PluginMessageHandler` handles reception on the `lunaticchat:main` channel. In `initialize()` it calls `channelRegistrar.register(CHANNEL)` and subscribes to events.

`@Subscribe onPluginMessage` processing:

1. Ignore if `event.identifier != CHANNEL`
2. Mark the event `ForwardResult.handled()` so Velocity forwards it nowhere
3. Warn and discard if the source is not a `ServerConnection`
4. Branch on the result of `PluginMessageCodec.decode()` with `when`
5. `Handshake` → check compatibility and reply with `HandshakeResponse` / `StatusRequest` → reply with `StatusResponse` / `GlobalChatMessage` → delegate to the relay / otherwise (a Velocity-originated response type) → warn only

### Trust boundary: rejecting client-originated messages

Velocity plugin messages on `lunaticchat:main` can arrive not only from backend servers but also from clients, and the trust boundary is made of two steps that only work together.

- **Marking the event handled.** Velocity forwards a plugin message to the other side of the connection unless a listener consumes it. A client's message would otherwise reach the backend, and Bukkit cannot tell it apart from one the proxy sent, so the backend would act on a forged global chat, direct message, presence snapshot or handshake response. The event is therefore marked `handled()` before anything else, whatever its source.
- **Checking the source is a `ServerConnection`.** This keeps the proxy itself from acting on a client's message: only messages from backend connections are relayed.

The Paper side additionally ignores every relay until a handshake has succeeded and strips legacy formatting codes and control characters from relayed text, but those are defence in depth: against a proxy that forwards client messages, only an authenticated wire format would let a backend tell the two apart.

## Handshake handling

On receiving a `Handshake`, it judges compatibility via the engine's `ProtocolVersion.isCompatible(major, minor)`.

- **Compatible** — reply with `HandshakeResponse` where `compatible=true`
- **Incompatible** — reply with `compatible=false` and an error string carrying both the Paper-side and Velocity-side versions

`HandshakeResponse` / `StatusResponse` always carry Velocity's own `ProtocolVersion` (`MAJOR` / `MINOR` / `PATCH`), so the Paper side can learn the peer's protocol from the response.

## Cross-server relay

`CrossServerChatRelay.relayGlobalMessage(message, sourceServer)` is the heart of the relay.

```
server.allServers
    .filter { it != sourceServer }   // exclude the source
    .forEach { it.sendPluginMessage(CHANNEL, encoded) }
```

It **excludes the source server** and broadcasts to all remaining backends (stage one of echo prevention). The relay count is logged.

### What gets relayed / what stays local

Keeping the relay scope minimal is a key design point.

- The only thing Velocity relays to other servers is the **`GlobalChatMessage`**
- `Handshake` / `HandshakeResponse` / `StatusRequest` / `StatusResponse` complete between Velocity and a single Paper, and are not forwarded
- **DM and channel chat are never sent to Velocity at all** (they complete locally within Paper)

### Two-stage echo/loop prevention

To keep global chat from being displayed multiple times through relay loops, it is prevented in two places.

1. **Velocity side** — broadcast excluding the source server
2. **Paper side** — a dedup LRU cache keyed by `messageId` (TTL 60s). The sender also registers its own `messageId` right after generation to prevent an echo on its own server

## Message flow

1. Paper sends a `Handshake` (its own protocol version), triggered by a player connecting
2. Velocity judges with `ProtocolVersion.isCompatible` and replies with `HandshakeResponse` → if compatible, the Paper side becomes `CONNECTED`
3. A player sends global chat (no active channel, or a `!` prefix) → Paper sends a `GlobalChatMessage` (a new `messageId`) to Velocity and displays normal chat on the source
4. Velocity relays to all backends except the source
5. Each Paper receives it → dedups by `messageId` → formats with `crossServerGlobalChatFormat` and delivers to all players

## Implementation notes

- The `plugin` parameter of `PluginMessageHandler` is typed `Any` because Velocity's `EventManager.register()` takes an `Object` (the API itself isn't type-safe, so making it generic offers little benefit).
- To run cross-server chat, Velocity's `velocity.toml` needs `bungee-plugin-message-channel=true` (plugin messaging enabled).

## Related

- [Design Overview](/docs/developers/architecture)
- [engine - Shared Kernel](/docs/developers/engine) — protocol details
- [platform-paper - Paper / Folia Plugin](/docs/developers/platform-paper) — the Paper-side counterpart

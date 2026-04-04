---
layout: doc
---

# Velocity Integration

Relays global chat across multiple Paper / Folia servers via a Velocity proxy.

## Setup

### 1. Install the Velocity Plugin

Place `LunaticChat-<version>-velocity.jar` in the Velocity `plugins/` directory and restart the proxy.

### 2. Paper-Side Configuration

Set the following in each Paper server's `config.yml`.

```yaml
features:
  velocityIntegration:
    enabled: true
    crossServerGlobalChat: true
    serverName: "survival"    # Must match the server name in the Velocity configuration
```

### 3. Verify the Connection

```
/lcv status
```

You can check the connection status, protocol version, Velocity plugin version, and more (permission: `lunaticchat.command.lcv.status`, default: op).

## Cross-Server Global Chat

When `crossServerGlobalChat` is set to `true`, player chat messages are relayed via Velocity to all other Paper servers.

### Message Flow

1. A player sends a chat message
2. The Paper server sends the message to Velocity
3. Velocity relays the message to all servers except the sender's
4. The message is displayed to players on each server

### Message Deduplication

Each message is assigned a unique ID, and a cache prevents the same message from being displayed more than once. The cache size can be configured with `messageDeduplicationCacheSize` (default: `100`).

## Protocol Version

Compatibility between Paper and Velocity is managed by protocol version. A handshake is performed upon connection, and incompatible versions are rejected.

### Version Bump Rules

| Level | Example Change | Compatibility | Deployment Order |
|-------|---------------|---------------|-----------------|
| PATCH (1.0.0 -> 1.0.1) | Adding optional fields, new sub-channels | Fully compatible (safe with `ignoreUnknownKeys=true`) | Any order, anytime |
| MINOR (1.0.x -> 1.1.0) | Adding required fields, changing existing sub-channel semantics | Backward compatible within `MIN_SUPPORTED_MINOR` range | **Update Velocity first** -> then update each Paper server |
| MAJOR (1.x.x -> 2.0.0) | Wire format changes, removing/renaming sub-channels | Incompatible | **Simultaneous deployment of all servers** |

### Compatibility Check

Compatibility is determined during the handshake using the following rules:

- **MAJOR** versions must match
- The remote **MINOR** must be at least `MIN_SUPPORTED_MINOR` and at most the local MINOR
- **PATCH** does not affect the compatibility check

#### Example: Velocity with protocol 1.2.0 and `MIN_SUPPORTED_MINOR=1`

| Paper Protocol | Result |
|---------------|--------|
| 1.1.x | Connection OK |
| 1.2.x | Connection OK |
| 1.0.x | Rejected (older than `MIN_SUPPORTED_MINOR`) |
| 1.3.x | Rejected (newer than Velocity) |
| 2.0.x | Rejected (MAJOR mismatch) |

### Operational Cycle

1. **No protocol change** -> Paper / Velocity can be deployed independently
2. **PATCH change** -> Deploy freely from either side
3. **MINOR change** -> Update Velocity first and set `MIN_SUPPORTED_MINOR` to allow a grace period for older Paper servers. After all Paper servers are updated, raise `MIN_SUPPORTED_MINOR`
4. **MAJOR change** -> Simultaneous update during a maintenance window

## Connection States

| State | Description |
|-------|-------------|
| `DISCONNECTED` | Not connected |
| `HANDSHAKING` | Handshake in progress |
| `CONNECTED` | Connected |
| `FAILED` | Connection failed |

The handshake timeout is 5 seconds. If the handshake times out, the state becomes `FAILED`.

## Configuration Reference

| Setting Key | Default | Description |
|-------------|---------|-------------|
| `enabled` | `false` | Enable Velocity integration |
| `crossServerGlobalChat` | `false` | Enable cross-server global chat |
| `serverName` | `"Unknown"` | Server name displayed in cross-server chat |
| `messageDeduplicationCacheSize` | `100` | Size of the message deduplication cache |

## Message Format

The display format for cross-server chat can be customized via `messageFormat.crossServerGlobalChatFormat` in `config.yml`. See [Message Format](/en/docs/reference/message-format) for details.

---
layout: doc
---

# Velocity Integration

Relays global chat across multiple Paper / Folia servers via a Velocity proxy.

::: tip About compatibility
The Paper and Velocity plugins are versioned independently. **Using the latest of both always works.** If you need to mix older versions, see [Paper / Velocity Compatibility](/docs/reference/compatibility).
:::

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

## Connection States

The states reported by `/lcv status` and their meanings:

| State | Description |
|-------|-------------|
| `DISCONNECTED` | Not connected |
| `HANDSHAKING` | Handshake in progress |
| `CONNECTED` | Connected |
| `FAILED` | Connection failed |

The handshake timeout is 5 seconds. If the handshake times out, the state becomes `FAILED`.

### Troubleshooting `FAILED`

- Confirm the Velocity plugin is installed and the proxy is running
- Confirm the Paper `serverName` matches the server name configured in Velocity
- Confirm the **protocol versions** of the Paper and Velocity plugins are compatible — see the [compatibility matrix](/docs/reference/compatibility#compatibility-matrix)

## Configuration Reference

| Setting Key | Default | Description |
|-------------|---------|-------------|
| `enabled` | `false` | Enable Velocity integration |
| `crossServerGlobalChat` | `false` | Enable cross-server global chat |
| `serverName` | `"Unknown"` | Server name displayed in cross-server chat |
| `messageDeduplicationCacheSize` | `100` | Size of the message deduplication cache |

## Message Format

The display format for cross-server chat can be customized via `messageFormat.crossServerGlobalChatFormat` in `config.yml`. See [Message Format](/docs/reference/message-format) for details.

## Related Documents

- [Paper / Velocity Compatibility](/docs/reference/compatibility) — protocol version and rolling update details

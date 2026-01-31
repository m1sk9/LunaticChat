# LunaticChat Docker Development Environment

This directory contains Docker Compose configuration for testing LunaticChat with Velocity proxy integration.

## Architecture

```
┌─────────────────┐
│  Minecraft      │
│  Client         │
│  localhost:25577│
└────────┬────────┘
         │
         v
┌─────────────────┐
│  Velocity       │
│  Proxy          │
│  Port: 25577    │
│  + LunaticChat  │
│    (Velocity)   │
└────────┬────────┘
         │
         v
┌─────────────────┐
│  Paper Server   │
│  minecraft:25565│
│  + LunaticChat  │
│    (Paper)      │
└─────────────────┘
```

## Requirements

- Docker
- Docker Compose
- Gradle (for building plugins)

## Quick Start

```bash
# Build plugins and start containers
./x start

# View Minecraft server logs
./x logs

# View Velocity proxy logs
./x vlogs

# Restart containers
./x restart

# Stop containers
./x stop

# Clean up (remove volumes)
./x clean

# Access RCON
./x rcon
```

## Configuration

### LunaticChat Paper Plugin

Configuration file: `docker/plugins/LunaticChat/config.yml`

Key settings:
- `features.velocityIntegration.enabled: true` - Enables Velocity integration
- `debug: true` - Enables debug logging

### Velocity Proxy

Configuration file: `docker/velocity.toml`

Key settings:
- `online-mode = false` - Offline mode for testing
- `player-info-forwarding-mode = "NONE"` - No player info forwarding
- `servers.minecraft = "minecraft:25565"` - Backend server connection

## Testing Velocity Integration

### 1. Start the environment

```bash
./x start
```

Wait for both containers to start. You should see:
- Paper server: "Done! For help, type 'help'"
- Velocity: "Done (X.XXs)!"

### 2. Connect with Minecraft client

1. Open Minecraft Java Edition
2. Add a server with address: `localhost:25577`
3. Connect to the server

### 3. Verify handshake

When you join the server, LunaticChat will perform a handshake between Paper and Velocity.

**Expected behavior:**
- Paper plugin sends handshake message to Velocity
- Velocity plugin validates plugin version and protocol version
- If compatible: connection succeeds, handshake logs appear in both containers
- If incompatible: Paper plugin disables itself with error message

**Check logs:**

Paper server:
```bash
./x logs | grep -i handshake
```

Expected output:
```
[INFO]: [LunaticChat] Sending handshake to Velocity (Plugin: 0.8.0, Protocol: 1.0.0)
[INFO]: [LunaticChat] Velocity handshake successful with version 0.7.0
```

Velocity proxy:
```bash
./x vlogs | grep -i handshake
```

Expected output:
```
[INFO] [lunaticchat]: Received handshake from minecraft: Plugin=0.8.0, Protocol=1.0.0
[INFO] [lunaticchat]: Handshake successful with minecraft
```

### 4. Test /lcv status command

In-game or via RCON:
```
/lcv status
```

Expected output:
- Paper Plugin Version
- Velocity Plugin Version
- Protocol Version
- Connection State: Connected
- Live status check results

## Version Compatibility

### Plugin Version Check
- **Rule:** Paper and Velocity plugin versions must match exactly
- **Example:** Paper 0.8.0 + Velocity 0.8.0 = ✓ PASS
- **Example:** Paper 0.8.0 + Velocity 0.7.0 = ✗ FAIL

### Protocol Version Check
- **Rule:** MAJOR.MINOR must match (PATCH differences are OK)
- **Example:** Paper 1.0.0 + Velocity 1.0.1 = ✓ PASS
- **Example:** Paper 1.0.0 + Velocity 1.1.0 = ✗ FAIL
- **Example:** Paper 1.0.0 + Velocity 2.0.0 = ✗ FAIL

## Troubleshooting

### Paper plugin disabled on startup

**Cause:** Handshake failed or timed out

**Solutions:**
1. Check Velocity is running: `docker ps | grep velocity`
2. Check plugin versions match in both containers
3. Review error messages in Paper logs: `./x logs | grep ERROR`

### "Handshake timeout" error

**Cause:** Paper plugin couldn't reach Velocity proxy

**Solutions:**
1. Verify network connectivity: `docker network inspect docker_minecraft-network`
2. Check Velocity logs for errors: `./x vlogs`
3. Restart containers: `./x restart`

### Version mismatch errors

**Cause:** Plugin versions don't match or protocol incompatible

**Solutions:**
1. Rebuild both plugins: `./gradlew clean :platform-paper:shadowJar :platform-velocity:shadowJar`
2. Restart containers: `./x restart`
3. Verify versions in logs match

## Network Details

- **Velocity Public Port:** 25577 (connect here with Minecraft client)
- **Paper RCON Port:** 25575 (for remote commands)
- **Internal Network:** `minecraft-network` (bridge driver)
- **Paper Internal Address:** `minecraft:25565` (accessible from Velocity)

## Useful Commands

```bash
# Follow logs in real-time
docker compose -f docker/compose.yaml logs -f

# Execute command in Minecraft server
docker compose -f docker/compose.yaml exec minecraft rcon-cli
> /list
> /lcv status

# Shell access to containers
docker compose -f docker/compose.yaml exec minecraft bash
docker compose -f docker/compose.yaml exec velocity bash

# View Velocity configuration
docker compose -f docker/compose.yaml exec velocity cat /server/velocity.toml
```

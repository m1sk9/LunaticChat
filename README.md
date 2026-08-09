# LunaticChat

[![CI](https://github.com/m1sk9/LunaticChat/actions/workflows/ci.yaml/badge.svg)](https://github.com/m1sk9/LunaticChat/actions/workflows/ci.yaml)
[![Release](https://github.com/m1sk9/LunaticChat/actions/workflows/release.yaml/badge.svg)](https://github.com/m1sk9/LunaticChat/actions/workflows/release.yaml)
[![GNU General Public License v3.0](https://img.shields.io/badge/license-GPL--3.0-9944ee)](https://github.com/m1sk9/LunaticChat/blob/main/LICENSE)
![Modrinth Downloads](https://img.shields.io/modrinth/dt/MBeAdO4L)
[![codecov](https://codecov.io/github/m1sk9/LunaticChat/graph/badge.svg?token=M3CJYTADYD)](https://codecov.io/github/m1sk9/LunaticChat)

A next-generation chat plugin for Paper, Folia and Velocity.

- [Documentation](https://lc.m1sk9.dev)
- [API Documentation](https://lc.api.m1sk9.dev)

_[Supports Minecraft 26.2](https://minecraft.wiki/w/Java_Edition_26.2) | [Requires Java 25+ and Gradle 9+](.github/CONTRIBUTING.md)_

## Features

- **Channel Chat**: **Create and manage channels for group conversations between specific players. Includes private channels and moderation features.**
- **Direct Messages**: **Send 1-on-1 chats with `/tell` or `/msg` commands. Quickly reply to the last sender with `/reply`.**
- **Romaji Conversion**: **Automatically convert romaji input into Japanese. Fast performance powered by caching.**
- **Velocity Cross-Server Chat**: **Relay global chat across multiple servers via a Velocity proxy. Join conversations from any server.**
- **Flexible Configuration**: **Toggle features on/off with a YAML-based config file. Customize to fit your server's needs.**
- **Latest Version Support**: **Minimal external plugin dependencies, always supporting the latest Minecraft versions.**

## Installation

> [!WARNING]
> LunaticChat only supports Paper / Folia servers. Spigot and BungeeCord are not supported, and there are no plans to support them in the future. For Spigot environments, we recommend using a [fork of LunaChat](https://github.com/f1w3/LunaChat).

### Requirements

| Item | Requirement |
|------|-------------|
| Minecraft | 26.2 or later |
| Java | 25 or later |
| Server | Paper, Folia, or Velocity |

### Download

Grab the plugin JAR from either of the following:

- [GitHub Releases](https://github.com/m1sk9/LunaticChat/releases)
- [Modrinth](https://modrinth.com/project/lunaticchat)

Paper and Folia use `LunaticChat-<version>.jar`; Velocity proxies use `LunaticChat-<version>-velocity.jar`. The two are versioned independently.

### Paper / Folia

1. Place `LunaticChat-<version>.jar` into the server's `plugins/` directory
2. Start (or restart) the server
3. `plugins/LunaticChat/config.yml` is generated automatically
4. Adjust the [configuration](#configuration) as needed, then run `/lc reload` or restart the server

### Velocity

1. Place `LunaticChat-<version>-velocity.jar` into the Velocity `plugins/` directory
2. Start (or restart) the proxy
3. Set `features.velocityIntegration.enabled` to `true` in the `config.yml` of every backend server
4. Restart the backend servers, then check the connection with `/lcv status` — the handshake is sent once a player joins, so an empty server reports `DISCONNECTED`

For more detail, see [Getting Started](https://lc.m1sk9.dev/docs/getting-started).

## Configuration

Server-wide settings live in `plugins/LunaticChat/config.yml`, generated on first startup. **Most features are disabled by default**, so enable the ones you want:

```yaml
language: "en" # Plugin language (en / ja)

features:
  quickReplies:
    enabled: true # /reply command
  japaneseConversion:
    enabled: false # Romaji to Japanese conversion
  channelChat:
    enabled: false # Channel chat
  velocityIntegration:
    enabled: false # Velocity proxy integration
```

`/lc reload` re-reads `config.yml` and applies the `messageFormat` settings without a restart. Every other setting is fixed when the server starts, so the command reports which of your changes still need a restart. A file it cannot read in full is refused outright and the running configuration is kept.

Chat output is customizable through `messageFormat` with placeholders such as `{sender}`, `{message}`, `{channel}` and `{server}`:

```yaml
messageFormat:
  channelMessageFormat: "§7[§b#{channel}§7] §e{sender}: §f{message}"
```

Players control their own preferences with `/lc settings` (direct message notifications, channel notifications, and romaji conversion). These are stored per UUID in `player-settings.yaml`.

Every key, its type, and its default are listed in the [Configuration reference](https://lc.m1sk9.dev/docs/configuration). See also the [Player Settings reference](https://lc.m1sk9.dev/docs/reference/player-settings) and the [default `config.yml`](./platform-paper/src/main/resources/config.yml).

## Velocity Integration

LunaticChat can relay global chat across multiple Paper / Folia servers behind a Velocity proxy, and optionally deliver direct messages across servers. Install the plugin on both the Velocity proxy and each backend server.

Paper–Velocity compatibility is determined solely by an internal **protocol version**, not by the plugin version. Builds with incompatible protocol versions will refuse to relay chat, so keep both sides updated together.

For supported combinations and setup details, see [Paper / Velocity Compatibility](https://lc.m1sk9.dev/docs/reference/compatibility) and [Velocity Integration](https://lc.m1sk9.dev/docs/features/velocity).

## Building from Source

```shell
git clone git@github.com:m1sk9/LunaticChat.git
cd LunaticChat

./gradlew shadowJar
```

The JARs are written to `platform-paper/build/libs/` and `platform-velocity/build/libs/`. See [CONTRIBUTING](.github/CONTRIBUTING.md) for the development workflow, and the [Developer Guide](https://lc.m1sk9.dev/docs/developers/introduction) for the design overview.

## License

LunaticChat is published under [GNU General Public License v3.0](./LICENSE).

<sub>
    ® 2026 m1sk9
    <br/>
    LunaticChat is not affiliated with Mojang Studios or Microsoft.
</sub>

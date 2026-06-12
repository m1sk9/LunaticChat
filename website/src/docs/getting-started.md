---
layout: doc
---

# Getting Started

This guide explains how to set up LunaticChat.

::: warning Spigot / BungeeCord is not supported
LunaticChat only supports Paper / Folia servers. It does not work on Spigot or BungeeCord, and there are no plans to support them in the future. For Spigot environments, we recommend using a [fork of LunaChat](https://github.com/f1w3/LunaChat).
:::

## Requirements

| Item | Requirement |
|------|-------------|
| Minecraft | 26.1 or later |
| Java | 25 or later |
| Server | Paper, Folia, or Velocity |

## Download

You can download the plugin JAR from either of the following:

- [GitHub Releases](https://github.com/m1sk9/LunaticChat/releases)
- [Modrinth](https://modrinth.com/project/lunaticchat)

Use `LunaticChat-<version>.jar` for Paper / Folia servers and `LunaticChat-<version>-velocity.jar` for Velocity proxies.

## Installation

### Paper / Folia

1. Place the downloaded `LunaticChat-<version>.jar` into the server's `plugins/` directory
2. Start (or restart) the server
3. `plugins/LunaticChat/config.yml` will be generated automatically
4. Modify the [configuration](/docs/configuration) as needed and restart the server

### Velocity

1. Place the downloaded `LunaticChat-<version>-velocity.jar` into the Velocity `plugins/` directory
2. Start (or restart) the Velocity proxy
3. Set `features.velocityIntegration.enabled` to `true` in the Paper-side `config.yml`
4. See [Velocity Integration](/docs/features/velocity) for details

## Next Steps

- [Configuration](/docs/configuration) - Review all settings in `config.yml`
- [Direct Message](/docs/features/direct-message) - How to use the DM feature
- [Channel Chat](/docs/features/channel-chat) - How to use the channel feature
- [Command Reference](/docs/reference/commands) - Reference for all commands

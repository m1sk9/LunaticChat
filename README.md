# LunaticChat

[![CI](https://github.com/m1sk9/LunaticChat/actions/workflows/ci.yaml/badge.svg)](https://github.com/m1sk9/LunaticChat/actions/workflows/ci.yaml)
[![Release](https://github.com/m1sk9/LunaticChat/actions/workflows/release.yaml/badge.svg)](https://github.com/m1sk9/LunaticChat/actions/workflows/release.yaml)
[![Deploy Dokka](https://github.com/m1sk9/LunaticChat/actions/workflows/dokka.yaml/badge.svg)](https://github.com/m1sk9/LunaticChat/actions/workflows/dokka.yaml)
[![GNU General Public License v3.0](https://img.shields.io/github/license/m1sk9/LunaticChat?color=%239944ee)](https://github.com/m1sk9/LunaticChat/blob/main/LICENSE)
![Modrinth Downloads](https://img.shields.io/modrinth/dt/MBeAdO4L)
[![codecov](https://codecov.io/github/m1sk9/LunaticChat/graph/badge.svg?token=M3CJYTADYD)](https://codecov.io/github/m1sk9/LunaticChat)

A next-generation chat plugin for Paper, Folia and Velocity.

- [Documentation](https://lc.m1sk9.dev)
- [API Documentation](https://lc.api.m1sk9.dev)

```shell
git clone git@github.com:m1sk9/LunaticChat.git
cd LunaticChat

./gradlew shadowJar
```

_[Supports Minecraft 26.1](https://minecraft.wiki/w/Java_Edition_26.1) | [Requires Java 25+ and Gradle 9+](.github/CONTRIBUTING.md)_

## Installation

LunaticChat is compatible with the following platforms:

- Paper
- Velocity
- Folia

Pre-built artifacts can be installed from the following locations. Place the downloaded plugin file in your server's `plugins` folder and restart the server.

- [GitHub](https://github.com/m1sk9/LunaticChat/releases)
- [Modrinth](https://modrinth.com/project/lunaticchat)

See the [Documentation](https://lc.m1sk9.dev/docs/getting-started).

> [!WARNING]
> LunaticChat only supports Paper / Folia servers. Spigot and BungeeCord are not supported, and there are no plans to support them in the future. For Spigot environments, we recommend using a [fork of LunaChat](https://github.com/f1w3/LunaChat).

## Features

- 1on1 Direct Messaging System (`/tell`, `/msg`)
- Quick Reply Functionality (`/reply`)
- Romaji to Japanese Conversion
- Channel Chat System
- Multi-platform support (Paper, Folia, Velocity)

## License

LunaticChat is published under [GNU General Public License v3.0](./LICENSE).

<sub>
    ® 2026 m1sk9
    <br/>
    LunaticChat is not affiliated with Mojang Studios or Microsoft.
</sub>

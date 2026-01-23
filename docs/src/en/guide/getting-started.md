# Getting Started

## Installation

Install LunaticChat. LunaticChat can be obtained from:

- [GitHub](https://github.com/m1sk9/LunaticChat/releases)
- [Modrinth](https://modrinth.com/project/lunaticchat)

Place the downloaded plugin file in your server's `plugins` folder and restart the server.

## Configuration

When LunaticChat starts, the following files will be created:

- `plugins/LunaticChat/config.yml`: Plugin configuration file
- `plugins/LunaticChat/player-settings.yaml`: User-specific settings file
- `plugins/LunaticChat/conversion_cache.json`: Romanization conversion cache file

Open the configuration file `config.yml` and modify the settings as needed. For details on configuration options, refer to the [Configuration Guide](admin/configuration.md).

## Permissions

Set LunaticChat's permissions using a permission management plugin like LuckPerms.

Basic permissions can be configured using Paper or Velocity's default permission system (`OP` / `non OP`), but LuckPerms is recommended for more detailed control.

- For details on permission nodes, refer to the [Permissions Guide](../reference/permissions.md).
- For permission nodes corresponding to each command feature, refer to the [Command Reference](../reference/index.md).

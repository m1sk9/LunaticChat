# Data and Logs

::: danger Do Not Edit

These data files are essential to the operation of LunaticChat. Direct editing may cause data corruption or unexpected behavior. Do not directly edit these files unless you are backing up data.

:::

## Data Storage Location

LunaticChat saves channel data and configuration information to local disk.

- `channels.json`: Stores channel information.
- `chatmodes.json`: Stores player chat mode settings.
- `conversion_cache.json`: Stores cache for channel conversion.
- `player-settings.yaml`: Stores player-specific settings.

::: tip Regular Backups

To ensure the safety of your LunaticChat data, we recommend creating regular backups.

:::

### `channels.json`

The `channels.json` file stores information about channels managed by LunaticChat. This file contains information such as channel names, participant lists, and chat modes.

```json
{
    "channels": {
        "general-channel": {
            "id": "general-channel",
            "name": "General Channel",
            "ownerId": "a01e3843-e521-3998-958a-f459800e4d11",
            "createdAt": 1769507213150,
            "bannedPlayers": [
                "ceaea267-39dd-3bac-931c-761ada671ebe"
            ]
        }
    },
    "members": {
        "general-channel": [
            {
                "channelId": "test2",
                "playerId": "a01e3843-e521-3998-958a-f459800e4d11",
                "role": "OWNER",
                "joinedAt": 1769507213150
            }
        ]
    },
    "activeChannels": {
        "a01e3843-e521-3998-958a-f459800e4d11": "test2"
    }
}
```

### `chatmodes.json`

The `chatmodes.json` file stores player chat mode settings. This file contains chat mode information for each player.

```json
{
  "modes": {
    "aed5efd4-551b-3965-bc28-ae21aa072a66": "CHANNEL",
    "ceaea267-39dd-3bac-931c-761ada671ebe": "CHANNEL",
    "a01e3843-e521-3998-958a-f459800e4d11": "CHANNEL",
    "681f539b-8bb8-3f85-85e5-a2945f6c6539": "GLOBAL"
  }
}
```

### `conversion_cache.json`

The `conversion_cache.json` file stores cache for channel conversion. This file contains channel conversion information for each player.

For more information about the cache system, see [here](./cache.md).


```json
{"version":"1","entries":{"hi":"日"}}
```

### `player-settings.yaml`

The `player-settings.yaml` file stores player-specific settings. This file contains individual player settings.

```yaml
version: 1
japaneseConversion:
  "aed5efd4-551b-3965-bc28-ae21aa072a66": false
  "ceaea267-39dd-3bac-931c-761ada671ebe": false
directMessageNotification:
  "aed5efd4-551b-3965-bc28-ae21aa072a66": true
  "ceaea267-39dd-3bac-931c-761ada671ebe": true
channelMessageNotification:
  "ceaea267-39dd-3bac-931c-761ada671ebe": true
```

## Cache Version

Files used for disk caching include a `version` field to accommodate changes in cache format as LunaticChat is upgraded.

If the version does not match, LunaticChat recognizes the cache file as **old format cache**, ignores the contents, and recreates it in the new format.

```json
{"version":"1","entries":{}}
```

## About CoreProtect

Various chat logs in LunaticChat can also be recorded in CoreProtect without requiring an API.

Logs for each feature can be checked with the following actions. When using the `/co lookup` command, specify the following actions:

- Direct messages: `command`
- Global chat and channel chat: `chat`
  - Japanese and romanization conversion is saved according to the player's settings.



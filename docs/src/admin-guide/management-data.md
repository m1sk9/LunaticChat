# データ・ログ

::: danger 編集厳禁

これらのデータファイルは LunaticChat の動作に不可欠です．直接編集すると，データの破損や予期せぬ動作を引き起こす可能性があります．データのバックアップを取る場合を除き，これらのファイルを直接編集しないでください．

:::

## データの保存場所

LunaticChat は、チャンネルデータや設定情報をローカルディスクに保存します．

- `channels.json`: チャンネル情報を保存するファイルです．
- `chatmodes.json`: プレイヤーのチャットモード設定を保存するファイルです．
- `conversion_cache.json`: チャンネル変換のキャッシュを保存するファイルです．
- `player-settings.yaml`: プレイヤーごとの設定情報を保存するファイルです．

::: tip 定期的なバックアップ

LunaticChat のデータの安全性を確保するために，定期的にバックアップを作成することをお勧めします．

:::

### `channels.json`

`channels.json` ファイルは、LunaticChat が管理するチャンネルの情報を保存します．このファイルには，チャンネル名、参加者リスト、チャットモードなどの情報が含まれます．

```json
{
    "channels": {
        "general-channel": {
            "id": "general-channel",
            "name": "一般チャンネル",
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

`chatmodes.json` ファイルは、プレイヤーのチャットモード設定を保存します．このファイルには，プレイヤーごとのチャットモード情報が含まれます．

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

`conversion_cache.json` ファイルは、チャンネル変換のキャッシュを保存します．このファイルには，プレイヤーごとのチャンネル変換情報が含まれます．

キャッシュシステムに関する詳細は [こちら](./cache.md) をご覧ください．


```json
{"version":"1","entries":{"hi":"日"}}
```

### `player-settings.yaml`

`player-settings.yaml` ファイルは、プレイヤーごとの設定情報を保存します．このファイルには，プレイヤーの個別設定が含まれます．

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

## キャッシュバージョン

ディスクキャッシュに使用されるファイルには `version` フィールドが含まれており，LunaticChat のバージョンアップに伴うキャッシュフォーマットの変更に対応しています．

バージョンが不一致の場合，LunaticChat はキャッシュファイルを **古い形式のキャッシュ** として認識し，内容を無視して新しい形式で再作成します．

```json
{"version":"1","entries":{}}
```

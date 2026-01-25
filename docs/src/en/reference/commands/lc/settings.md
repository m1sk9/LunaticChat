# /lc settings <Badge type="tip" text="v0.5.0" /> <Badge type="tip" text="Paper" />

### `/lc settings <key> [value]`

| Permission                        |
|--------------------------------|
| `lunaticchat.command.settings` |

Manages player settings for LunaticChat. If no value is specified, the current setting value will be displayed.

This setting is saved by UUID, so the setting will not be lost even if you change your MCID.

#### Available Keys

| Key       | Description                                                       | Default Value |
|----------|----------------------------------------------------------|--------|
| `japanese`     | Toggles romanization conversion on or off.                                   | `true` |
| `notice` | Toggles whether to receive notifications when receiving direct messages via `/tell` or `/reply`. | `true` |

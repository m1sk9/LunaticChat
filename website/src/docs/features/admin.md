---
layout: doc
---

# Admin Features

This page covers features intended for server administrators. These features are primarily available to players with OP permissions.

## Plugin Status (`/lc status`)

View an overview of the plugin's operational status.

```
/lc status
```

Displayed information:

- Plugin version (with Git commit hash)
- Health status (OK / Degraded)
- Enabled/disabled state of each feature
- Configuration values (debug mode, update checking, language)
- Links to GitHub, Modrinth, and documentation

## Configuration Reload (`/lc reload`) <Badge type="tip" text="v1.4.0~" />

Re-read `config.yml` without restarting the server.

```
/lc reload
```

Only the [`messageFormat`](/docs/reference/message-format) settings are applied. Everything else in `config.yml` decides something the plugin settles at startup, so the command lists the changed settings that still need a restart instead of pretending they took effect.

```
[LC] Reloaded config.yml, but some of the changes need a server restart to take effect.
```

The reply says only whether what you edited is in effect. Which settings moved is written to the server log.

- **Permission**: `lunaticchat.command.lc.reload` (default: op). Usable from the console and RCON
- A `config.yml` the plugin cannot read in full is refused: every unreadable setting is named at once and the running configuration is kept. This is stricter than startup, which falls a single unreadable setting back to its default so the server can come up
- Every reload is written to the server log, so a format change can be dated later. The file is read off the main thread, and an RCON session closes before the reply reaches it, so RCON callers read the outcome from the log rather than from the command output

## Debug Logging (`/lc debug`) <Badge type="tip" text="v1.4.0~" />

Debug logging is split into categories — `config`, `chat`, `channel`, `conversion`, `protocol`, `velocity`, `storage`, `command` — so that turning it on to chase a handshake does not bury the log under one line per chat message. See [Debug Logging](/docs/configuration#debug-logging) for what each category reports.

```
/lc debug                     # what is logging now
/lc debug velocity on
/lc debug all off
```

- **Permission**: `lunaticchat.command.lc.debug` (default: op). Usable from the console and RCON
- The change is **not** written to `config.yml`. A restart or `/lc reload` puts the value in the file back in charge, so a category left on by accident cannot outlive the session
- Lines are written at `INFO` with a `[LC/<category>]` prefix, because Paper's stock log4j configuration does not print anything below `INFO`

On a Velocity proxy there is no `config.yml`, so the same switch is read from `-Dlunaticchat.debug=velocity,protocol` or the `LUNATICCHAT_DEBUG` environment variable.

## Diagnostics Report (`/lc dump`) <Badge type="tip" text="v1.4.0~" />

Writes everything a maintainer needs to triage a bug report to `plugins/LunaticChat/debug/report-<timestamp>.txt`, and confirms in chat that it wrote one to the plugin folder.

```
/lc dump
```

The report holds:

- LunaticChat version and commit, and the protocol version
- Server software, Bukkit API, Java and OS versions
- Which features are enabled, and this server's `serverName`
- Which debug categories are logging
- Velocity connection state, proxy plugin version and last error
- Channel and member counts, conversion cache fill, tracked player settings
- The installed plugins

It deliberately holds **no message text, player name or UUID** — it is meant to be pasted into a public issue.

- **Permission**: `lunaticchat.command.lc.dump` (default: op). Usable from the console and RCON

## Spy Mode

Players with the `lunaticchat.spy` permission (default: op) can view both the direct messages and the channel messages sent on the server.

- Direct messages are delivered to spies, excluding the sender and the recipient
- Channel messages are delivered to spies, excluding the sender and the channel's own members
- For direct messages, spies see the original text before romaji conversion. Channel messages reach spies in the same converted form the members see
- Hover text indicates the message is a spy message
- Spy players themselves are not included in the normal sender/recipient list

## Channel Bypass

Players with the `lunaticchat.channelbypass` permission (default: op) can bypass the following channel restrictions.

- Cannot be kicked or banned
- Can delete channels even without being the owner

## Update Notifications

When `checkForUpdates` is `true` (default), the plugin checks for new versions at startup. Players with the `lunaticchat.noticeupdate` permission (default: op) receive an update notification when they join the server.

```yaml
# config.yml
checkForUpdates: true
```

## Nightly Builds

Builds produced from the `main` branch outside of a release are marked as nightly, and the plugin says so rather than letting it go unnoticed.

- The version carries a `-nightly.<commit hash>` suffix (`1.3.0-nightly.44132f3`), so the JAR file name, `/plugins` and `/lc status` all tell it apart from the 1.3.0 release
- Every player is warned on join that the build may be unstable, along with a pointer to GitHub Issues
- `/lc status` shows the same warning, and displays the release channel in yellow instead of green

Nightly builds are not covered by the [security policy](https://github.com/m1sk9/LunaticChat/blob/main/.github/SECURITY.md); use a release build on a production server.

## Language Setting

You can change the language of messages displayed to players. Plugin logs and console output are not affected and remain in English only.

```yaml
# config.yml
language: "ja"   # "en" or "ja"
```

## Admin Permissions Reference

| Permission | Default | Description |
|-----------|---------|-------------|
| `lunaticchat.spy` | op | View all direct and channel messages |
| `lunaticchat.channelbypass` | op | Bypass channel restrictions |
| `lunaticchat.noticeupdate` | op | Receive update notifications |
| `lunaticchat.command.lcv.status` | op | Use the `/lcv status` command |
| `lunaticchat.command.lc.debug` | op | Use the `/lc debug` command |
| `lunaticchat.command.lc.dump` | op | Use the `/lc dump` command |

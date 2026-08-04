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

## Spy Mode

Players with the `lunaticchat.spy` permission (default: op) can view both the direct messages and the channel messages sent on the server.

- Direct messages are delivered to spies, excluding the sender and the recipient
- Channel messages are delivered to spies, excluding the sender and the channel's own members
- Spy players see the original message before romaji conversion
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

- Every player is warned on join that the build may be unstable, along with a pointer to GitHub Issues
- `/lc status` shows the same warning, and displays the release channel in yellow instead of green

Nightly builds are not covered by the [security policy](https://github.com/m1sk9/LunaticChat/blob/main/.github/SECURITY.md); use a release build on a production server.

## Debug Mode

Setting `debug` to `true` enables verbose plugin logging. This is useful for troubleshooting issues or submitting bug reports.

```yaml
# config.yml
debug: true
```

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

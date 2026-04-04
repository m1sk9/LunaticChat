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

Players with the `lunaticchat.spy` permission (default: op) can view all direct messages sent and received on the server.

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
| `lunaticchat.spy` | op | View all direct messages |
| `lunaticchat.channelbypass` | op | Bypass channel restrictions |
| `lunaticchat.noticeupdate` | op | Receive update notifications |
| `lunaticchat.command.lcv.status` | op | Use the `/lcv status` command |

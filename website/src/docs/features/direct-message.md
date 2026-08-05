---
layout: doc
---

# Direct Message

Send and receive private 1-on-1 messages between players.

## Basic Usage

### Sending a Message

```
/tell <player> <message>
```

Aliases: `/t`, `/msg`, `/m`, `/w`, `/whisper`

Sends a direct message to the specified player. Clicking on a received message will auto-fill the reply command to the sender.

### Quick Reply

```
/reply <message>
```

Alias: `/r`

Replies to the last player who sent you a message. If there is no such player, the message is sent to the last player you messaged.

To use quick reply, `features.quickReplies.enabled` must be `true` (default) in `config.yml`.

## Cross-Server Direct Messages <Badge type="tip" text="Paper v1.3.0~ / Velocity v1.2.0~" />

> [!NOTE]
>
> To use this feature, set `features.velocityIntegration.crossServerDirectMessage` to `true` in `config.yml`.

To message a player on another server, specify the player argument as `playerName@serverName`.

```
/tell <player>@<server> <message>
```

`serverName` is the name of the destination server **as registered in your Velocity configuration** (`velocity.toml`), which is what the proxy resolves the target against. Tab completion offers the names and players it currently knows about.

Set `features.velocityIntegration.serverName` on each backend to that same name. It does not affect routing, but it fills `{server}` in cross-server chat and is how a server recognises which players are its own — if it disagrees with the Velocity name, local players are treated as remote in tab completion.

Delivery can fail in two ways, and the sender is told which:

| Reason | Meaning |
|--------|---------|
| `SERVER_NOT_FOUND` | No server registered on the proxy has that name |
| `TARGET_OFFLINE` | The server exists, but that player is not on it — including when they are online on a different server |

## Notification Settings

Players can individually control the sound notification when receiving direct messages.

```
/lc settings notice on     # Enable notifications
/lc settings notice off    # Disable notifications
```

## Integration with Japanese Conversion

When [Japanese Conversion](/docs/features/japanese-conversion) is enabled, direct message content is also automatically converted to Japanese. Conversion follows each player's `japanese` setting.

## Spy Feature

Players with the `lunaticchat.spy` permission (default: op) can view all direct messages on the server. Spy players see the original message before conversion.

## Message Format

The display format for direct messages can be customized via `messageFormat.directMessageFormat` in `config.yml`. See [Message Format](/docs/reference/message-format) for details.

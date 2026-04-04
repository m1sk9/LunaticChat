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

## Notification Settings

Players can individually control the sound notification when receiving direct messages.

```
/lc settings notice on     # Enable notifications
/lc settings notice off    # Disable notifications
```

## Integration with Japanese Conversion

When [Japanese Conversion](/en/docs/features/japanese-conversion) is enabled, direct message content is also automatically converted to Japanese. Conversion follows each player's `japanese` setting.

## Spy Feature

Players with the `lunaticchat.spy` permission (default: op) can view all direct messages on the server. Spy players see the original message before conversion.

## Message Format

The display format for direct messages can be customized via `messageFormat.directMessageFormat` in `config.yml`. See [Message Format](/en/docs/reference/message-format) for details.

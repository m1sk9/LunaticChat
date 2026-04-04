---
layout: doc
---

# Message Logging

Records channel chat messages in NDJSON (Newline Delimited JSON) format to files. This feature is available when channel chat is enabled and is turned on by default.

## Configuration

```yaml
# config.yml
features:
  channelChat:
    enabled: true
    messageLogging:
      enabled: true
      retentionDays: 30
      maxFileSizeMB: 100
```

| Setting Key | Default | Description |
|-------------|---------|-------------|
| `enabled` | `true` | Enable message logging |
| `retentionDays` | `30` | Number of days to retain log files (`0` for unlimited retention) |
| `maxFileSizeMB` | `100` | Maximum size of a single log file (MB) |

## Log File Format

Log files are saved in the `plugins/LunaticChat/logs/` directory. Each line is a single JSON object.

### File Naming

```
channel-messages-YYYY-MM-dd.json
```

When the file size exceeds `maxFileSizeMB`, a new file with a suffix is created.

```
channel-messages-2026-04-05.json       # Base file
channel-messages-2026-04-05-1.json     # On size overflow
channel-messages-2026-04-05-2.json     # On further overflow
```

### Entry Format

Each line has the following JSON structure.

```json
{
  "timestamp": "2026-04-05T14:23:45.123Z",
  "playerId": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "Steve",
  "channelId": "general",
  "message": "Hello everyone!"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | String | ISO 8601 timestamp (UTC) |
| `playerId` | String | Player UUID |
| `playerName` | String | Player display name |
| `channelId` | String | ID of the channel the message was sent to |
| `message` | String | Message content |

## File Rotation

- **Daily rotation**: A new file is created when the date changes
- **Size rotation**: Switches to a suffixed file when `maxFileSizeMB` is exceeded
- **Automatic cleanup**: Log files older than the number of days specified by `retentionDays` are automatically deleted (no deletion when set to `0`)

## Usage Examples

Since the format is NDJSON, you can easily filter and aggregate logs using tools like `jq`.

### Extract Messages from a Specific Channel

```bash
jq 'select(.channelId == "general")' channel-messages-2026-04-05.json
```

### Extract Messages from a Specific Player

```bash
jq 'select(.playerName == "Steve")' channel-messages-2026-04-05.json
```

### Count Messages by Channel

```bash
jq -s 'group_by(.channelId) | map({channel: .[0].channelId, count: length})' channel-messages-2026-04-05.json
```

# Channel Chat: Logs  <Badge type="tip" text="v0.7.0" />

Channel chat logs are a record of all messages and activities that occur within the chat.

::: warning Compatibility with Plugins like CoreProtect

As of v0.7.0, LunaticChat's channel chat feature is not compatible with logging plugins like CoreProtect.

:::

## Checking Channel Chat Logs

Channel chat logs are stored in the `plugins/LunaticChat/logs/channelchat/` directory.

Channel chat log files are saved in the following format:

```
{"timestamp":"2026-01-31T08:54:18.504343071Z","playerId":"ceaea267-39dd-3bac-931c-761ada671ebe","playerName":"m1sk9","channelId":"test","message":"Hello"}
```

## About File Size

Each line is a complete JSON object, separated by line breaks, and the file as a whole is not a JSON array.

```text
plugins/LunaticChat/logs/
├── channel-messages-2026-01-17.json  (5.2 MB)
├── channel-messages-2026-01-18.json  (4.8 MB)
├── channel-messages-2026-01-19.json  (6.1 MB)
├── channel-messages-2026-01-20.json  (5.5 MB)
├── channel-messages-2026-01-21.json  (7.2 MB) <- Weekend, active
├── channel-messages-2026-01-22.json  (6.9 MB)
├── channel-messages-2026-01-23.json  (4.3 MB)
├── channel-messages-2026-01-24.json  (5.0 MB)
├── channel-messages-2026-01-25.json  (5.4 MB)
├── channel-messages-2026-01-26.json  (4.9 MB)
├── channel-messages-2026-01-27.json  (6.2 MB)
├── channel-messages-2026-01-28.json  (7.5 MB)
├── channel-messages-2026-01-29.json  (5.8 MB)
├── channel-messages-2026-01-30.json  (6.0 MB)
└── channel-messages-2026-01-31.json  (2.1 MB) <- Today (in progress)
```

Total: approximately 83 MB

With a 30-day retention setting, the January 17 file will be automatically deleted tomorrow.

### Size per Message

Channel chat log entries are approximately 200 bytes per line.

Assuming 1000 messages per hour:

```text
1000 msg/h × 24h × 220 bytes = 5,280,000 bytes ≈ 5.3 MB/day
```

With the default 30-day retention setting, this amounts to approximately **159 MB**.

```text
5.3 MB × 30 days = 159 MB
```

## Visualization with Grafana Loki

Due to the JSON format, using Promtail to ingest channel chat logs into Grafana Loki will parse them for easier reading.

```text
2026-01-31 10:23:45.123  {job="lunatichat", player="Steve", channel="Global"}
Hello everyone!

2026-01-31 10:24:12.456  {job="lunatichat", player="Alex", channel="Global"}
Hi Steve!

2026-01-31 10:25:03.789  {job="lunatichat", player="Notch", channel="Development Team"}
Working on new features
```

::: tip Filtering Examples

Examples of querying logs in Grafana Loki:

```text
{job="lunatichat"} |= "new features"
{job="lunatichat", channel="Global"}
{job="lunatichat", player="Steve"}
```

:::

## Command Line Checking Examples

### View the latest 10 entries

```bash
tail -n 10 plugins/LunaticChat/logs/channel-messages-2026-01-31.json | jq
```

### Extract messages from a specific player

```bash
cat plugins/LunaticChat/logs/channel-messages-*.json | \
jq 'select(.playerName=="Steve")'
```

### Count messages from a specific channel

```bash
cat plugins/LunaticChat/logs/channel-messages-*.json | \
jq 'select(.channelId=="global")' | wc -l
```

### Message count by date

```bash
for file in plugins/LunaticChat/logs/channel-messages-*.json; do
echo "$file: $(wc -l < $file) messages"
done
```

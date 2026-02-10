# Velocity Integration (Cross-Server Chat) <Badge type="tip" text="v0.8.0" />

This feature enables cross-server chat between Paper servers connected through a Velocity proxy server.

::: warning Experimental Feature

This feature is currently provided as an experimental feature. Specifications may change in future updates.

:::

## Enabling Integration

To enable Velocity integration, follow these steps:

1. Install the Velocity version of LunaticChat on the Velocity side.
2. Open `plugins/LunaticChat/config.yml` on each Paper server and set `velocity.enabled` to `true`.
3. Start Velocity and Paper, and verify that the integration completion message appears.

## Plugin Version and Protocol Version

For Velocity and Paper's LunaticChat to integrate correctly, both plugin versions and protocol versions must be compatible.

- Ensure that the LunaticChat versions on Velocity and Paper sides are the same.
  - Compatibility may not be guaranteed when using beta or development versions.
- Ensure that protocol versions match.

::: danger Regarding LunaticChat versions prior to v0.7.0

LunaticChat versions prior to v0.7.0 are not backward compatible with the Velocity version.

They cannot be used together, so if you are using LunaticChat prior to v0.7.0, do not use the Velocity integration feature.

:::

## How to Check Integration Status

To verify that Velocity integration is working properly, use the `/lcv status` command.

The current protocol version and connection state will be displayed.

::: warning Behavior When Versions Mismatch

When the version or protocol version does not match, the Velocity integration feature is disabled, but **the plugin itself continues to operate**.

You can check the connection state and error details using the `/lcv status` command. To reconnect, ensure that the LunaticChat version / protocol version on Velocity and Paper sides match.

:::

::: tip About Velocity Integration Timing

LunaticChat connects to Velocity **when the first player joins the server** after startup.

Therefore, if you run the `/lcv status` command immediately after server startup, the Velocity integration may not be established yet.

:::

## Troubleshooting

If you encounter issues with Velocity integration, check the following:

- **Run the `/lcv status` command to check the connection state**. If there are errors such as version mismatches, an error message will be displayed.
- Ensure that the LunaticChat version / protocol version on Velocity and Paper sides are the same.
- Verify that LunaticChat settings are correct.
- Ensure that network connection between Velocity server and Paper servers is working properly.

## Cross-Server Chat

When Velocity integration is enabled, chat messages are shared across all servers connected through the proxy server.

## Features Compatible with Velocity Integration

The main features available during Velocity integration are as follows:

|            | Compatibility | Behavior | Notes |
|------------| --- | --- | --- |
| Direct Messages | × | `/tell` and `/reply` commands operate only within the server | Direct messages do not support Velocity integration. |
| Channel Chat | × | Channel chat operates only within the server | Channel chat does not support Velocity integration. |
| Kana/Romanization | ◯ | Kana/Romanization works across all servers |  |

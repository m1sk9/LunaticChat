# LunaticChat Changelog

## v1

### v1.4.0

- Supports reloading configurations while the system is running. Except certain settings, changes can be applied immediately.
- We have enhanced the debug functionality. You can now toggle individual features, and the displayed logs have also been improved.
- Added `/lc debug`. Debugging features can now be controlled without restarting.
- Added `/lc dump`. Diagnostic reports for plugins can now be generated.

#### Velocity: v1.3.0

- Debugging functionality has been implemented in Velocity. It can be controlled by switching environment variables.

### v1.3.0

- Paper 26.2 (Minecraft 26.2) is now supported.
  - Support for Paper 26.1 has been dropped. Paper 26.2 bundles Adventure 5.2.0, which is not binary compatible with the Adventure 4.x shipped by 26.1, so `api-version` has been raised to `26.2` and the plugin will no longer load on 26.1 servers.
- Added cross-server direct messaging functionality.
  - `/tell <player>@<server>` sends a direct message to a player on another backend server. Disabled by default; enable `features.velocityIntegration.crossServerDirectMessage`.
- Fixed a bug where the player argument for `/tell` used partial matching (we switched from the old Bukkit API to the new API)
- `config.yml` is now read with KAML, which changes how a broken file is treated.
  - A `config.yml` that cannot be read no longer disables the plugin; it starts on its defaults instead.
  - A single unreadable value now falls back to its own default with a warning, instead of discarding every other setting in the file.
  - `features.channelChat.messageLogging` is now actually read. It was documented but never loaded, so it stayed at its defaults regardless of what the file said.
- Fixed a bug where a slow Google IME reply could silently stop delivering a player's messages for the rest of their session.
  - `GoogleIMEClient` reported a request timeout as a `CancellationException`, which the delivery queue worker correctly read as shutdown and ended its loop on — leaving the player's queue registered with nothing draining it. Triggered whenever `features.japaneseConversion.api.timeout` was set below the message conversion budget. Timeouts are now an ordinary exception.
- Fixed a bug where a single slow conversion pinned a word to hiragana permanently.
  - A timed-out conversion was cached as though the API had answered, so that word rendered unconverted for the life of the cache. Timeouts now leave the cache untouched and the next message retries.
- Fixed a bug where `/reply` could not see a reply target that had just been recorded, and a duplicated recording that re-inserted entries already cleared on disconnect.
- Fixed a bug where a setting changed while a save was already pending was not written to disk.
- A failed save no longer aborts the remaining shutdown steps, so channel logs and the Velocity connection are always closed.
- File persistence moved behind a single storage layer. Every data file (`channels.json`, player settings, the conversion cache) is written atomically and debounced by construction rather than per call site, so a crash or a concurrent save can no longer leave a half-written file behind.
- Service teardown is now driven by a `StoppableService` type registered at construction, so a service cannot be missed from the shutdown path.
- Per-player delivery queues are now bounded. A player who sends faster than delivery drains is refused with a warning instead of building an unbounded backlog that arrives minutes late.
- Each player's messages are delivered in the order they were sent. Work still queued when a player disconnects is discarded rather than delivered to a player who has left, and work dropped at shutdown is logged instead of disappearing silently.
- Performance improvements.
  - Direct message delivery and channel message logging no longer run on the tick thread.
  - Cached romaji conversions no longer wait on the shared API concurrency limiter. A message whose every word was already cached could previously exhaust its conversion budget waiting for a permit it did not need, and be sent unconverted.
  - The words of a message are converted concurrently.
  - Player settings are only rewritten when something changed. Every player quit previously re-serialized every player stored in the file to write identical bytes.
  - Clearing a player's active channel on quit no longer snapshots the channel caches when that player had no active channel. A mass disconnect previously paid a full snapshot per player within one tick.
  - Direct message and channel message spy notification no longer builds its translation lookup and member set when no spy is online.
  - Channel data writes are coalesced instead of rewriting the file on every change.
  - Features that are turned off no longer cost anything at startup, and the HTTP client is only created when a feature actually needs it.
- Internal cleanup. Removed dead code and unreachable error paths: an unused spy accessor, a `Result` that could not fail (with three unreachable handlers and one unreachable user-facing message), a redundant feature-gate clause, and a duplicated error boundary beneath the delivery queue.
  - Test suite: 561 → 569.
- The protocol version has been raised to 1.0.1 for the new direct message and presence sub-channels.
  - This is a PATCH bump, so Paper and Velocity can be updated in any order. Cross-server direct messages require both sides to be updated.

#### Velocity: v1.2.0

- Velocity 4.0.0 is now supported.
  - Support for Velocity 3.5.x has been dropped.
- Added cross-server direct message relay and player presence tracking.
- The Velocity JAR shrank from about 8.2 MiB to about 2.6 MiB.
  - Paper-only dependencies (Ktor and the romaji converter) were moved out of the shared module, so they are no longer bundled into the Velocity build.

### v1.2.2

This release includes updates to the Velocity version.

- Dependency updates only. There are no functional changes to the plugin.
  - Updated Paper API to 26.1.2 (build.69).
  - Updated libraries: kotlin-reflect, serialization, jvm to 2.4.0.

#### Velocity: v1.1.0

- Velocity 3.5.0 is now supported.
  - Support for Velocity 3.4.0 has been dropped.

### v1.2.1

- Dependency updates only. There are no functional changes to the plugin.
  - Updated Paper API to 26.1.2 (build.67).
  - Updated libraries: kotlinx-coroutines 1.11.0, Ktor 3.5.0, JUnit 6.1.0, and MockK 1.14.11.
  - Updated Gradle to 9.5.1.
  - Updated the Shadow plugin to 9.4.2.

### v1.2.0

- Paper 26.1.2 (Minecraft 26.1.2) is now supported.
  - We have also added code support for Paper 26.1.1 (Minecraft 26.1.1). However, since this is not the latest version, we cannot guarantee its functionality (LunaticChat only supports the latest version).

### v1.1.0

- Add an alias for a subcommand.
- Standardize CI/CD.
- Fixed an issue on the documentation site (lc.m1sk9.dev) where the deployment process would get stuck on the “Deploying...” screen under certain conditions.

### v1.0.0

- Paper 26.1 (Minecraft 26.1) is now supported.
  - Support for Paper 1.21.X, Folia 1.21.X (and later) has been dropped.
- We have added warnings when running `/lc status` or `/lcv status`, or when logging in, if you are using the Nightly version.
- Fixed an issue where, while Romaji conversion was enabled, chat events were not registered under certain conditions, making Romaji conversion unavailable.
- Improving Velocity's Cycling Compatibility.

### v0.11.0

- We have removed the chat mode implementation.
  - All chat messages sent after joining a channel will now appear in the channel chat.
- Clicking the notification message now displays the channel's status.
- Optimization of internal logic.
- We have improved the notification message that appears when you log in to the server while in a channel.
- Added a delay to prevent other plugins from interfering with login notifications.

### v0.10.1

- Codecov was introduced to expand test coverage.
- The release steps for Paper and Folia in Modrinth were consolidated.
  - This is because both loaders use the same build, eliminating the need for separate steps. The v0.10.0 release was merged on 2026/02/26 1:00 (UTC+9).

### v0.10.0

- Added support for Folia 1.21 and later.
- We have revised the document structure.
- We have added Folia-related references.
- The current health status is now displayed in the status shown when you run `/lc status`.
- The current configuration status is now displayed in the status shown when you run `/lc status`.
- You can now copy the version by clicking on the status displayed when you run `/lc status`.

### v0.9.1

- Fixed an issue where update notification messages were not formatted correctly.

### v0.9.0

- Modrinth releases are now split per Loader.
- Allow LunaticChat for Velocity to run standalone when launch fails due to version mismatch
- Prevent `/lcv status` from becoming disabled when Velocity integration fails due to version mismatch
- Enable checking Velocity integration errors via `/lcv status`
- Add hover text to `/lcv status` indicating current integration status
- Fixed an issue where metadata linked to the Velocity version of LunaticChat build was incorrect.
- Enabled execution of `/lcv status` from the console.
  - Live status display requires the Player API and is therefore not supported.

### v0.8.0

- Velocity integration is now available.
- Fixed the header design for certain commands such as `/lc channel`.
- When converting to Roman letters, if kana characters are included, those conversions should be skipped.

### v0.7.0

#### Breaking Changes

- Other plugins such as CoreProtect can no longer intercept channel chat.
  - This has broken the integration functionality with CoreProtect.
  - Channel Chat logs are now uniformly accessible via a text-based viewing method.

----

- Channel chat logging functionality has been implemented.
    - Logs are now recorded daily in `plugins/LunaticChat/logs/channelchat/`.

### v0.6.0

- Added experimental feature to Channel Chat support.
- Fixed broken links to documentation referenced in config.yml.
- Added a description indicating this to messages sent in Spy Mode.
- Other bug fixes.

### v0.5.0

- Added i18n support for English and Japanese languages.
- Fixed a Dokka currently references only the platform-paper module.
- Improve the cache strategy for kana conversion.
- Save the cache in memory to disk when the server stops.
- Added `/lc settings` command to manage LunaticChat user settings.
- Added `/lc status` command to check the status of LunaticChat.

### v0.4.1

- Fixed Kana conversion with voiced consonants.

### v0.4.0

- Added an update checker that verifies whether updates are available when the server starts up.
- Change so that Japanese input settings are enabled by default.
- Improved certain modules to eliminate Paper dependencies.
- Added notification feature for receiving direct messages via `/tell` and `/reply`.

### v0.3.1

#### Breaking Changes:

- Change user settings file format from JSON to TOML.
  - **As a result, players will need to re-enable the `/jp` setting**.
  - The old JSON format user configuration files are no longer used. You can safely delete them.
- The configuration file has been deleted or modified.
  - It must be initialized when the server starts.

----

- Fixed an issue where Japanese text was being converted to Romanized characters.

### v0.3.0

- Fixed an issue where romaji conversion did not work properly in some cases.

### v0.2.0

- Fixed a release workflow issue.

### v0.1.0 - Initial release

- Added `/tell`, `/reply`, and `/jp` commands.
- Implemented romaji conversion.

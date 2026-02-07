# LunaticChat Changelog

## v0

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

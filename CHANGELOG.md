# LunaticChat Changelog

### v0.4.0

- Added an update checker that verifies whether updates are available when the server starts up.
- Change so that Japanese input settings are enabled by default.
- Improved certain modules to eliminate Paper dependencies.
- Added notification feature for receiving direct messages via `/tell` and `/reply`.
- Fixed an issue where the internal API documentation for LunaticChat published at `lc.api.m1sk9.dev` only referenced the `platform-paper` module.

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

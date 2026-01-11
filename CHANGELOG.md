# LunaticChat Changelog

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

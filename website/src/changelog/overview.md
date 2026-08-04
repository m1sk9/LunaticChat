---
layout: doc
---

# Changelog

The release history of LunaticChat.

## Versioning

::: tip

For the details of independent versioning in LunaticChat, see [Build, Release & Versioning](/docs/developers/resource).

:::

LunaticChat follows semantic versioning. A version number consists of the three numbers `MAJOR.MINOR.PATCH`, each raised on the following basis.

| Position | Raised when | Example |
|----------|-------------|---------|
| **MAJOR** | Used for the move from the pre-release (v0) line to the first stable release. It has not been raised since entering the stable line | `v0.11.0` → `v1.0.0` |
| **MINOR** | A feature is added or changed, and **when a supported platform is dropped** | `v1.2.2` → `v1.3.0` (cross-server DM added, Paper 26.1 dropped) |
| **PATCH** | Bug fixes and dependency updates only. No functional changes | `v1.2.0` → `v1.2.1` (dependency updates only) |

::: warning Dropping support is done in a MINOR bump

Cutting a runtime environment loose — "support for Paper 26.1 has ended", "support for Velocity 3.5.x has ended" — **is done in a MINOR bump**. MAJOR is not raised for it, so there can be combinations that stop working across nothing more than a minor version update. Check the changelog for the target version before updating.

:::

### The Paper and Velocity builds have separate version numbers

The plugin for Paper / Folia and the plugin for Velocity are released independently, so their version numbers advance separately. Numbers that do not line up are the normal state of affairs.

Releases are cut with a tag naming the target.

| Tag | Released target |
|-----|-----------------|
| `paper/vX.Y.Z` | The Paper / Folia build only |
| `velocity/vX.Y.Z` | The Velocity build only |
| `vX.Y.Z` | Both at once (v1.0.0 on 2026-04-04 was the last of these; it is no longer used) |

### The plugin version does not express compatibility

Whether a Paper build and a Velocity build can talk to each other is decided by the **protocol version** embedded in both, not by the plugin version. Builds with distant version numbers may be combinable, and conversely builds with adjacent numbers may not be.

For details, see [Paper / Velocity Compatibility](/docs/reference/compatibility).

### Supported versions

Only **the latest release of each platform** is supported. Fixes are not backported to older versions unless it is unavoidable.

Nightly builds are generated automatically from in-development commits. They are not formal releases and are therefore not supported. When a nightly build is in use, a warning is shown on join and when `/lc status` is run.

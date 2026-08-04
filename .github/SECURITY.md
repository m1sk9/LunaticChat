# Security Policy

## Supported Versions

The Paper/Folia and Velocity plugins are released independently, and **only the latest release of each is supported**. Older releases are not supported, and backports will not be provided unless absolutely necessary.

| Component | Supported |
|-----------|-----------|
| LunaticChat for Paper / Folia (latest `paper/vX.Y.Z`) | Yes |
| LunaticChat for Velocity (latest `velocity/vX.Y.Z`) | Yes |
| Any earlier release | No |
| Nightly builds and CI artifacts | No |

### Platform requirements

The supported platforms and runtime follow whatever the latest release is built against:

| | Supported |
|---|---|
| Minecraft | The `api-version` the latest release declares (currently `26.2`), i.e. `26.2.x` |
| Server software | Paper, Folia, and Velocity 4 |
| Java | 25 or later |

Spigot, BungeeCord, and Velocity 3.5.x are **not** supported, and there are no plans to support them.

### Paper and Velocity combinations

Paper–Velocity compatibility is decided by the internal **protocol version**, not by the plugin version. A combination that the protocol rejects will refuse to relay chat; that is intended behavior, not a vulnerability. See [Paper / Velocity Compatibility](https://lc.m1sk9.dev/docs/reference/compatibility) for the rules and the compatibility matrix.

If a report involves a proxy setup, please include the plugin version of **both** sides and the output of `/lcv status`.

## Reporting a Vulnerability

**Do not report security issues through Issues or Discussions.** Please use one of the following private channels:

1. **GitHub private vulnerability reporting** (preferred) — [open a draft security advisory](https://github.com/m1sk9/LunaticChat/security/advisories/new). This keeps the report private until a fix is published.
2. **Encrypted email** — [me@m1sk9.dev](mailto:me@m1sk9.dev), encrypted with the public key at [github.com/m1sk9.gpg](https://github.com/m1sk9.gpg).

Please include as much of the following as you can:

- The affected component (Paper/Folia or Velocity) and its version
- Minecraft and server software versions, and the Java version
- Which optional features were enabled in `config.yml` (`japaneseConversion`, `channelChat`, `velocityIntegration`, …), since most of them are off by default
- Steps to reproduce, and the impact you believe it has

A fix is shipped as a new release of the affected platform. If you would like to be credited in the advisory, please say so in your report.

### Vulnerabilities in dependencies

Known vulnerabilities in third-party dependencies are tracked by Dependabot and do not need a private report — an ordinary issue or pull request is fine. Please do use a private channel if you can demonstrate that a dependency issue is actually exploitable through LunaticChat.

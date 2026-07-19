---
layout: doc
---

# Introduction

This is a developer guide covering the design and architecture of LunaticChat.

Players and server administrators should refer to the [documentation / reference](/docs/getting-started).

::: tip Target versions
The design and architecture described in this guide reflect [Paper/Folia: v1.2.2](https://github.com/m1sk9/LunaticChat/releases/tag/paper%2Fv1.2.2) and [Velocity: v1.1.0](https://github.com/m1sk9/LunaticChat/releases/tag/velocity%2Fv1.1.0).
:::

## Module Structure

LunaticChat is organized into the following modules.

For the overall design, see [Design / Architecture](/docs/developers/architecture).

| Module | Role |
|--------|------|
| `engine` | Platform-independent core |
| `platform-paper` | Paper / Folia plugin |
| `platform-velocity` | Velocity proxy plugin |

## Guide Index

- [Design / Architecture](/docs/developers/architecture) — the big picture
  - [engine - Shared Kernel](/docs/developers/engine)
  - [platform-paper - Paper / Folia Plugin](/docs/developers/platform-paper)
  - [platform-velocity - Velocity Plugin (Proxy Relay)](/docs/developers/platform-velocity)
- [Build, Release & Versioning](/docs/developers/resource) — release flow and versioning

- [The story of building "LunaticChat", a successor to LunaChat - m1sk9 (Zenn)](https://zenn.dev/m1sk9/articles/adb6c0a7fa7bd2) — null-safety, coroutine usage, cache system, and more (external site, Japanese)

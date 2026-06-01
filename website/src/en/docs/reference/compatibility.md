---
layout: doc
---

# Paper / Velocity Compatibility

The Paper and Velocity plugins of LunaticChat are versioned independently. Whether a given combination works is determined by the **protocol version** embedded in each plugin.

::: warning Plugin version ≠ protocol version
The **plugin version** (e.g., Paper v1.2.0) and the **protocol version** (e.g., 1.0.0) are different things. New plugin releases do not necessarily change the protocol — and only the protocol version determines compatibility.
:::

## TL;DR

- **The latest Paper and the latest Velocity are always compatible.** When in doubt, use the latest of both.
- If you need to mix older versions, check the matrix below.
- You can verify the live connection state by running `/lcv status` on the Minecraft server.

## Compatibility Matrix

Each cell indicates whether the corresponding Paper × Velocity combination can connect. Data is fetched from GitHub Releases automatically.

<CompatibilityMatrix />

## What Is a Protocol Version?

Paper and Velocity communicate via a LunaticChat-specific plugin messaging protocol. The protocol carries a semantic version (`MAJOR.MINOR.PATCH`), and at connection time Velocity validates the protocol version sent by Paper.

::: tip Velocity is the only side that checks
Only Velocity performs the compatibility check. Paper just sends a handshake — it does not validate Velocity's version. So the question reduces to: "Does Velocity accept Paper's protocol version?"
:::

The rules (from Velocity's perspective) are:

- **MAJOR** must match exactly
- Paper's **MINOR** must be at least Velocity's `MIN_SUPPORTED_MINOR` and at most Velocity's `MINOR`
- **PATCH** does not affect compatibility

`MIN_SUPPORTED_MINOR` controls how far back Velocity accepts older Paper peers, providing a grace window during rolling updates.

### Version Bump Rules

| Level | Example Change | Compatibility | Deployment Order |
|-------|---------------|---------------|------------------|
| **PATCH** (1.0.0 → 1.0.1) | Adding optional fields, new sub-channels | Fully compatible (safe with `ignoreUnknownKeys=true`) | Any order, anytime |
| **MINOR** (1.0.x → 1.1.0) | Adding required fields, changing existing sub-channel semantics | Backward compatible within `MIN_SUPPORTED_MINOR` range | **Update Velocity first** → then update each Paper server |
| **MAJOR** (1.x.x → 2.0.0) | Wire format changes, removing/renaming sub-channels | Incompatible | **Simultaneous deployment of all servers** |

### Rolling Update Strategy

1. **No protocol change**: Paper and Velocity can be deployed independently. Plugin bug fixes and refactors fall here.
2. **PATCH change**: Deploy from either side freely.
3. **MINOR change**: Update Velocity first and use `MIN_SUPPORTED_MINOR` as a grace window for older Paper servers. After all Paper servers are updated, bump `MIN_SUPPORTED_MINOR`.
4. **MAJOR change**: Update all servers simultaneously during a maintenance window.

## Handshake Behavior

Compatibility is checked at connection time:

1. The Paper server sends a handshake to Velocity at startup
2. Velocity validates Paper's protocol version against its own
3. On mismatch, Velocity rejects the connection and Paper's state becomes `FAILED`
4. The handshake timeout is 5 seconds

Live connection state is available via `/lcv status`. See [Velocity Integration](/en/docs/features/velocity#connection-states) for details.

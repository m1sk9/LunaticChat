---
layout: doc
---

# Paper / Velocity Compatibility

The Paper and Velocity plugins of LunaticChat are versioned independently. Whether a given combination works is determined by the **protocol version** embedded in each plugin.

## TL;DR

- **The latest Paper and the latest Velocity are always compatible.** When in doubt, use the latest of both.
- If you need to mix older versions, check the matrix below.
- You can verify the live connection state by running `/lcv status` on the Minecraft server.

## Compatibility Matrix

Each cell indicates whether the corresponding Paper × Velocity combination can connect. Data is fetched from GitHub Releases automatically.

<CompatibilityMatrix />

## What Is a Protocol Version?

Paper and Velocity communicate via a LunaticChat-specific plugin messaging protocol. The protocol carries a semantic version (`MAJOR.MINOR.PATCH`), and a handshake at connection time validates both sides.

The rules are:

- **MAJOR** must match exactly
- The remote **MINOR** must be at least `MIN_SUPPORTED_MINOR` and at most the local `MINOR`
- **PATCH** does not affect compatibility

`MIN_SUPPORTED_MINOR` controls how far back the local plugin accepts older peers, providing a grace window during rolling updates.

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
2. Velocity validates the protocol version against its own
3. On mismatch, the connection is rejected and the state becomes `FAILED`
4. The handshake timeout is 5 seconds

Live connection state is available via `/lcv status`. See [Velocity Integration](/en/docs/features/velocity#connection-states) for details.

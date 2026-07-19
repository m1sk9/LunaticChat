---
layout: doc
---

# Build, Release & Versioning

A Gradle multi-module setup shares `engine` while building and releasing Paper / Velocity as independent artifacts.

## Build configuration

- Root `build.gradle.kts` — manages Kotlin 2.4.0 + serialization / Shadow / ktlint / dokka. The JVM target is **JVM_25**. Tests use JUnit Platform + jacoco, with common test dependencies injected into all modules
- `engine` — exposes core libraries (serialization / coroutines / ktor) via `api()` to propagate them to the platforms. Adventure is `compileOnly`. A pure library with no Shadow
- `platform-paper` — `version = paperVersion`. `api(project(":engine"))`. paper-api as `compileOnly`, KAML + kotlin-reflect as `implementation`. Output is **`LunaticChat-<ver>.jar`** (no classifier; `jar` disabled)
- `platform-velocity` — `version = velocityVersion`. `api(project(":engine"))`. velocity-api as `compileOnly` + `annotationProcessor` (for `@Plugin` processing). Output is **`LunaticChat-<ver>-velocity.jar`** (distinguished by classifier)
- `dokka` — aggregates engine/paper/velocity and includes the README in the HTML

Both platforms' `processResources` compute `version` / `gitCommitHash` / `channel` from the git short hash and `isNightly`, and token-expand them into `paper-plugin.yml` / `velocity-plugin.json` and `build-info.properties`.

## Independent versioning

```properties
# gradle.properties
paperVersion=1.2.2
velocityVersion=1.1.0
```

Paper and Velocity carry separate version numbers and can be released independently. That's because **compatibility is guaranteed by the engine-shared [`ProtocolVersion`](/docs/developers/engine#versioning-strategy-protocolversion) rather than the numeric version**, so the two platforms — which change at different rates — can be bumped and published at their own pace. The wire format is forward-compatible via JSON + `ignoreUnknownKeys`, and backward compatibility is controlled by matching MAJOR + a MINOR-range check on the protocol.

## Release workflows

The release target switches based on the tag pattern.

| Workflow | Trigger tag | Build target | Version validation |
|----------|-------------|--------------|--------------------|
| `release.yaml` | `v*` | Both Paper + Velocity | extract both versions from gradle.properties |
| `release-paper.yaml` | `paper/v*` | Paper only | requires the tag to match `paperVersion` |
| `release-velocity.yaml` | `velocity/v*` | Velocity only | requires the tag to match `velocityVersion` |

- Common flow: `validate` (check for a duplicate existing release) → `build` (mise + Gradle setup, `shadowJar`) → `release` (`gh release create --draft` + publish to Modrinth)
- The per-platform workflows (paper / velocity) differ from `release.yaml` in requiring a strict match between the tag and `gradle.properties`
- Modrinth game-versions are Paper=`26.1.x` (loaders: paper, folia) and Velocity=`1.21.x` + `26.1.x` (loader: velocity)

## CI

`ci.yaml` runs on push to main / PR / manual dispatch.

- `build_plugin` — ktlintCheck → test + jacocoTestReport → upload to Codecov → nightly shadowJar (`-PisNightly=true`) → retain artifacts
- `build_dokka` / `deploy_dokka` — generate Dokka → deploy to GitHub Pages (main push only)
- `build_docs` — format/lint/build `website/` with bun → deploy to Cloudflare Workers (wrangler) (main push only)

## Development environment

- `mise.toml` — bun / java zulu-25 (consistent with `JVM_25`)
- `x` — a bash debug-server script. `./x <action> <platform> [--stable]` for start/stop/log/clean/rcon/help. Without `--stable` it builds nightly. With `velocity` it brings up **1 Velocity + 2 Paper** so you can test cross-server chat relay for real
- `docker/` — `compose.yaml` for three environments: paper / velocity / folia (`itzg/minecraft-server:java25`, etc.). velocity.toml enables plugin messaging with `bungee-plugin-message-channel=true`

## Related

- [Design Overview](/docs/developers/architecture)
- [Introduction](/docs/developers/introduction)

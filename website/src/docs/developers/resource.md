---
layout: doc
---

# Build, Release & Versioning

A Gradle multi-module setup shares `engine` while building and releasing Paper / Velocity as independent artifacts.

## Build configuration

- Root `build.gradle.kts` — manages Kotlin 2.4.0 + serialization / Shadow / ktlint / dokka. The JVM target is **JVM_25**. Tests use JUnit Platform + jacoco, with common test dependencies injected into all modules
- `engine` — exposes core libraries (serialization / coroutines / ktor) via `api()` to propagate them to the platforms. Adventure is `compileOnly`. A pure library with no Shadow
- `platform-paper` — `version = paperVersion`. `api(project(":engine"))`. paper-api as `compileOnly`, KAML + kotlin-reflect as `implementation`. Output is **`LunaticChat-<ver>.jar`** (no classifier; `jar` disabled)
- `platform-velocity` — `version = velocityVersion`. `api(project(":engine"))`. velocity-api as `compileOnly`. Output is **`LunaticChat-<ver>-velocity.jar`** (distinguished by classifier)
- `dokka` — aggregates engine/paper/velocity and includes the README in the HTML

Both platforms derive their version from `isNightly`: with `-PisNightly=true` the base version gains a `-nightly.<git short hash>` suffix, so a development build is never mistakable for the release of the same base version (`LunaticChat-1.3.0-nightly.44132f3.jar`). Without the flag the version stays the bare `paperVersion` / `velocityVersion`, which is what the release workflows build.

`processResources` then token-expands `version` / `gitCommitHash` / `channel` into `paper-plugin.yml` / `velocity-plugin.json` and `build-info.properties`.

## Independent versioning

```properties
# gradle.properties
paperVersion=1.3.0
velocityVersion=1.2.0
```

Paper and Velocity carry separate version numbers and can be released independently. That's because **compatibility is guaranteed by the engine-shared [`ProtocolVersion`](/docs/developers/engine#versioning-strategy-protocolversion) rather than the numeric version**, so the two platforms — which change at different rates — can be bumped and published at their own pace. The wire format is forward-compatible via JSON + `ignoreUnknownKeys`, and backward compatibility is controlled by matching MAJOR + a MINOR-range check on the protocol.

## Release workflows

One tag releases exactly one platform. The tag pattern selects which.

| Workflow | Trigger tag | Build target | Version validation |
|----------|-------------|--------------|--------------------|
| `release-paper.yaml` | `vX.Y.Z` | Paper only | requires the tag to match `paperVersion` |
| `release-velocity.yaml` | `velocity/vX.Y.Z` | Velocity only | requires the tag to match `velocityVersion` |

- Releasing both at once means pushing both tags together (`git push origin v1.5.0 velocity/v1.4.0`). Each produces its own GitHub Release
- Paper keeps the bare `vX.Y.Z` tag because the update checker in already-deployed Paper builds only understands that form
- Up to v1.4.0, a `vX.Y.Z` tag released both platforms (`v1.0.0`, `v1.3.0`, `v1.4.0`), and some Paper-only releases used `paper/vX.Y.Z`. Those tags are kept as they are
- Both workflows call the shared `_release.yaml`: `validate` (tag format, match with `gradle.properties`, duplicate release check) → `build` (mise + Gradle setup, `shadowJar`) → `release` (`gh release create --draft` + publish to Modrinth)
- The GitHub Release is created as a draft. Publish a Velocity release with `--latest=false`, so that the repository's latest release stays the Paper one
- Modrinth game-versions are Paper=`26.2.x` (loaders: paper, folia) and Velocity=`1.21.x` + `26.1.x` + `26.2.x` (loader: velocity), set in each caller workflow

## CI

`ci.yaml` runs on push to main / PR / manual dispatch.

- `build_plugin` — ktlintCheck → test + jacocoTestReport → upload to Codecov → nightly shadowJar (`-PisNightly=true`) → retain artifacts as `LunaticChat-paper-<sha>` / `LunaticChat-velocity-<sha>` (one per platform, each holding the JAR at the archive root)
- `build_dokka` / `deploy_dokka` — generate Dokka → deploy to GitHub Pages (main push only)
- `build_docs` — format/lint/build `website/` with bun → deploy to Cloudflare Workers (wrangler) (main push only)

## Development environment

- `mise.toml` — bun / java zulu-25 (consistent with `JVM_25`)
- `x` — a bash debug-server script. `./x <action> <platform> [--stable]` for start/stop/log/clean/rcon/help. Without `--stable` it builds nightly. With `velocity` it brings up **1 Velocity + 2 Paper** so you can test cross-server chat relay for real
- `docker/` — `compose.yaml` for three environments: paper / velocity / folia (`itzg/minecraft-server:java25`, etc.). velocity.toml enables plugin messaging with `bungee-plugin-message-channel=true`
- Server versions are derived by `x` from the `paper-api` / `velocity-api` coordinates in `build.gradle.kts` and passed to Compose as `PAPER_MC_VERSION` / `PAPER_BUILD` / `VELOCITY_VERSION`, so the debug environments always run the build the plugin is compiled against. Invoking `docker compose` directly fails with a message telling you to go through `./x`. Only Folia's build number is pinned by hand, since it is numbered separately from Paper

## Related

- [Design Overview](/docs/developers/architecture)
- [Introduction](/docs/developers/introduction)

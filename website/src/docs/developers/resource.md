---
layout: doc
---

# Build, Release & Versioning

A Gradle multi-module setup shares `engine` while building and releasing Paper / Velocity as independent artifacts.

## Build configuration

- Root `build.gradle.kts` — manages Kotlin 2.4.0 + serialization / Shadow / ktlint / dokka. The JVM target is **JVM_25**. Tests use JUnit Platform + jacoco, with common test dependencies injected into all modules
- `engine` — exposes core libraries (serialization / coroutines / ktor) via `api()` to propagate them to the platforms. Adventure is `compileOnly`. A pure library with no Shadow
- `platform-paper` — `version` read from the root `version.txt`. `api(project(":engine"))`. paper-api as `compileOnly`, KAML + kotlin-reflect as `implementation`. Output is **`LunaticChat-<ver>.jar`** (no classifier; `jar` disabled)
- `platform-velocity` — `version` read from `platform-velocity/version.txt`. `api(project(":engine"))`. velocity-api as `compileOnly`. Output is **`LunaticChat-<ver>-velocity.jar`** (distinguished by classifier)
- `dokka` — aggregates engine/paper/velocity and includes the README in the HTML

Both platforms derive their version from `isNightly`: with `-PisNightly=true` the base version gains a `-nightly.<git short hash>` suffix, so a development build is never mistakable for the release of the same base version (`LunaticChat-1.3.0-nightly.44132f3.jar`). Without the flag the version stays the bare contents of `version.txt`, which is what the release workflows build.

`processResources` then token-expands `version` / `gitCommitHash` / `channel` into `paper-plugin.yml` / `velocity-plugin.json` and `build-info.properties`.

## Independent versioning

```text
version.txt                    # Paper:    1.7.0
platform-velocity/version.txt  # Velocity: 1.4.0
```

Paper and Velocity carry separate version numbers and can be released independently. That's because **compatibility is guaranteed by the engine-shared [`ProtocolVersion`](/docs/developers/engine#versioning-strategy-protocolversion) rather than the numeric version**, so the two platforms — which change at different rates — can be bumped and published at their own pace. The wire format is forward-compatible via JSON + `ignoreUnknownKeys`, and backward compatibility is controlled by matching MAJOR + a MINOR-range check on the protocol.

## Release workflows

A release is prepared by [release-please](https://github.com/googleapis/release-please) and started by merging its pull request. One tag releases exactly one platform, and Paper and Velocity get separate Release PRs.

| Workflow | Trigger | Role |
|----------|---------|------|
| `release-please.yaml` | push to `main` | Opens and updates the Release PRs; once one is merged, pushes the tag |
| `release-pr-checks.yaml` | Release PR | Checks the protocol version, Paper↔Velocity compatibility, the release notes and the Modrinth game versions. The `main` ruleset requires its `gate` job |
| `release-paper.yaml` | tag `vX.Y.Z` | Paper only |
| `release-velocity.yaml` | tag `velocity/vX.Y.Z` | Velocity only |

- `version.txt` (Paper) and `platform-velocity/version.txt` (Velocity) belong to release-please. The bump follows the Conventional Commit types merged since the last release: `fix` is PATCH, `feat` is MINOR, a breaking change is MAJOR
- The release notes are written by hand in the Release PR (`/create-release-note`). The PR cannot be merged until the checks find them. `CHANGELOG.md` stops at v1.7.0; the notes on this site are the changelog
- The tag is created by a job of our own rather than by release-please, as an annotated tag. Release tags are no longer GPG-signed: that needed a second, ordinary GitHub account to hold the public key, because a GitHub App cannot register one. It is pushed with a GitHub App token, because a tag pushed with `GITHUB_TOKEN` would not start the release workflows
- Paper keeps the bare `vX.Y.Z` tag because the update checker in already-deployed Paper builds only understands that form
- Up to v1.4.0, a `vX.Y.Z` tag released both platforms (`v1.0.0`, `v1.3.0`, `v1.4.0`), and some Paper-only releases used `paper/vX.Y.Z`. Those tags are kept as they are
- Both release workflows call the shared `_release.yaml`: `validate` (tag format, match with `version.txt`; a published Release stops the run) → `build` (mise + Gradle setup, `shadowJar`) → `verify` (the JAR's protocol constants match the source, and the docs deploy of the tagged commit has finished, so the release notes are on the site) → `release` (draft GitHub Release → Modrinth → publish) → `verify-rollout` (opens an issue on failure). A re-run reuses the draft an earlier run left behind
- The GitHub Release body and the Modrinth changelog are both rendered from the English release notes page. Modrinth is called through its API by `.github/scripts/publish-modrinth.sh`; no third-party action receives the token
- A Velocity release is published with `--latest=false`, so that the repository's latest release stays the Paper one
- Modrinth game-versions are written as patterns in each caller workflow (Paper=`26.3.x`, loaders: paper, folia; Velocity=`1.21.x` + `26.1.x` + `26.2.x` + `26.3.x`, loader: velocity) and expanded to the concrete versions Modrinth stores

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

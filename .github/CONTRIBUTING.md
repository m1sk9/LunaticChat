# LunaticChat Contribution Guidelines

## Looking for Support?

For instructions on using LunaticChat, see the [official documentation](https://lc.m1sk9.dev).

For questions, see the [discussion](https://github.com/m1sk9/LunaticChat/discussions).

## Reporting Bugs / Requesting Features

Before reporting a bug or requesting a feature, please search the [issues](https://github.com/m1sk9/LunaticChat/issues).

If your issue is not already reported, please create a new issue using the provided template. Be sure to include as much detail as possible, including steps to reproduce the issue if applicable.

## Contributing Code

We welcome contributions to LunaticChat!

LunaticChat adheres to the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).

It also uses Ktlint. Merging pull requests into the main branch requires passing Ktlint checks. When submitting a pull request, remember to run `./gradlew ktlintFormat`.

When making major changes that break backward compatibility, please discuss them beforehand via an issue or discussion.
Such changes often cause confusion when made without prior discussion.

### Design Guide

Our documentation site provides an overview of LunaticChat's design. 

We highly recommend reading it before you begin development.

[Introduction / Developer Guide - LunaticChat Documentation](https://lc.m1sk9.dev/docs/developers/introduction)

### Project Structure

LunaticChat uses a multi-module Gradle setup.

- `engine`: Contains code shared across all platforms.
- `platform-paper`: Contains code specific to the Paper platform.
- `platform-velocity`: Contains code specific to the Velocity platform.

### Debugging

LunaticChat comes with a Docker environment that can be launched as a debug server.

It is driven by `./x <action> <platform>`, where the platform is required:

- `./x start paper` builds the plugin and starts a single Paper server (`localhost:25565`)
- `./x start folia` starts a single Folia server (`localhost:25565`)
- `./x start velocity` starts a Velocity proxy with two backend Paper servers (`localhost:25577`)

Builds default to a nightly version derived from the current commit; pass `--stable` to build as a stable release.

Run `./x help` for the remaining actions (`stop`, `log`, `clean`, `rcon`).

#!/usr/bin/env bash
# Checks that the Minecraft version Modrinth is told about matches the one paper-api
# targets. Paper bumps rarely, and a stale value ships a release listed for the wrong game
# version without any build failing.
set -euo pipefail
# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

workflow=.github/workflows/release-paper.yaml

api=$(sed -nE 's/.*compileOnly\("io\.papermc\.paper:paper-api:([^"]+)"\).*/\1/p' platform-paper/build.gradle.kts | head -n1)
if ! [[ "$api" =~ ^([0-9]+\.[0-9]+)\.build\.[0-9]+- ]]; then
  error "Cannot derive the Minecraft version from paper-api:${api}; teach check-modrinth-meta.sh this format"
  exit "$STATUS"
fi
minecraft=${BASH_REMATCH[1]}

mc_version=$(sed -nE 's/.*modrinth-mc-version: *"([^"]*)".*/\1/p' "$workflow")
game_versions=$(sed -nE 's/.*modrinth-game-versions: *"([^"]*)".*/\1/p' "$workflow")

if [ "$mc_version" != "$minecraft" ]; then
  error "${workflow}: modrinth-mc-version is '${mc_version}' but paper-api targets ${minecraft}"
fi
if ! grep -qxF "${minecraft}.x" <<< "${game_versions//$'\n'/$'\n'}"; then
  error "${workflow}: modrinth-game-versions ('${game_versions}') does not list ${minecraft}.x"
fi

exit "$STATUS"

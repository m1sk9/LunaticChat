#!/usr/bin/env bash
# Shared helpers for the release checks. Source it; do not execute it.

# These are read by the scripts that source this file.
# shellcheck disable=SC2034
PROTOCOL_DIR=engine/src/main/kotlin/dev/m1sk9/lunaticChat/engine/protocol
PROTOCOL_TEST=engine/src/test/kotlin/dev/m1sk9/lunaticChat/engine/protocol/ProtocolBackwardCompatibilityTest.kt

# Set to 1 by error(); scripts exit with it so every problem is reported, not only the first.
STATUS=0

error() {
  echo "::error::$*"
  STATUS=1
}

notice() {
  echo "::notice::$*"
}

# The most recent released tag of a platform. PAPER_BASE_TAG / VELOCITY_BASE_TAG override
# it, which is how the checks are exercised against older history.
latest_tag() {
  case "$1" in
    paper)
      echo "${PAPER_BASE_TAG:-$(git tag --list 'v*' --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -n1)}"
      ;;
    velocity)
      echo "${VELOCITY_BASE_TAG:-$(git tag --list 'velocity/v*' --sort=-v:refname | grep -E '^velocity/v[0-9]+\.[0-9]+\.[0-9]+$' | head -n1)}"
      ;;
  esac
}

# Reads ProtocolVersion.kt on stdin, prints KEY=VALUE lines.
constants_from_source() {
  sed -nE 's/.*const val (MAJOR|MINOR|PATCH|MIN_SUPPORTED_MINOR) *= *([0-9]+).*/\1=\2/p'
}

# The same four constants, read from the compiled class inside a built JAR. This is what
# actually ships, so it also catches a JAR built from a different engine than expected.
constants_from_jar() {
  javap -constants -cp "$1" dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion \
    | sed -nE 's/.*static final int (MAJOR|MINOR|PATCH|MIN_SUPPORTED_MINOR) *= *([0-9]+);.*/\1=\2/p'
}

# load_constants <prefix>: turns KEY=VALUE lines on stdin into <prefix>_KEY variables.
load_constants() {
  local prefix=$1 key value
  while IFS='=' read -r key value; do
    printf -v "${prefix}_${key}" '%s' "$value"
  done
}

# Same rule as ProtocolVersion.isCompatible: the proxy judges the backend's version
# against its own MAJOR and MIN_SUPPORTED_MINOR..MINOR range.
# is_compatible <velocity-major> <velocity-minor> <velocity-min-supported> <paper-major> <paper-minor>
is_compatible() {
  [ "$1" -eq "$4" ] && [ "$5" -ge "$3" ] && [ "$5" -le "$2" ]
}

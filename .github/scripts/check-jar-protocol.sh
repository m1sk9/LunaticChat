#!/usr/bin/env bash
# Checks that the protocol constants compiled into a JAR are the ones in the source tree it
# is supposed to come from, so a JAR built from another commit cannot be released.
#
# Usage: check-jar-protocol.sh <jar>
set -euo pipefail
# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

jar=$1

in_jar=$(constants_from_jar "$jar" | sort | tr '\n' ' ')
in_source=$(constants_from_source < "$PROTOCOL_DIR/ProtocolVersion.kt" | sort | tr '\n' ' ')

if [ -z "$in_jar" ] || [ "$in_jar" != "$in_source" ]; then
  error "${jar} carries protocol constants [${in_jar}] but the source has [${in_source}]"
fi

exit "$STATUS"

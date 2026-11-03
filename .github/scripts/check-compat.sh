#!/usr/bin/env bash
# Checks that the built Paper and Velocity JARs can talk to each other, and to the last
# released build of the other platform.
#
# Usage: check-compat.sh <paper.jar> <velocity.jar>
# The <prefix>_KEY variables are created by load_constants (printf -v), which shellcheck
# cannot follow.
# shellcheck disable=SC2154
set -euo pipefail
# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

paper_jar=$1
velocity_jar=$2
summary=${GITHUB_STEP_SUMMARY:-/dev/null}

load_constants head_p < <(constants_from_jar "$paper_jar")
load_constants head_v < <(constants_from_jar "$velocity_jar")

paper_tag=$(latest_tag paper)
velocity_tag=$(latest_tag velocity)
load_constants rel_p < <(git show "${paper_tag}:${PROTOCOL_DIR}/ProtocolVersion.kt" | constants_from_source)
load_constants rel_v < <(git show "${velocity_tag}:${PROTOCOL_DIR}/ProtocolVersion.kt" | constants_from_source)

describe() {
  echo "$1.$2.$3 (min supported minor $4)"
}

{
  echo "| Pair | Velocity | Paper | Result |"
  echo "|------|----------|-------|--------|"
} >> "$summary"

# row <label> <velocity-prefix> <paper-prefix> -> 0 when compatible
row() {
  local label=$1 v=$2 p=$3
  local vmaj="${v}_MAJOR" vmin="${v}_MINOR" vsup="${v}_MIN_SUPPORTED_MINOR" vpatch="${v}_PATCH"
  local pmaj="${p}_MAJOR" pmin="${p}_MINOR" ppatch="${p}_PATCH" psup="${p}_MIN_SUPPORTED_MINOR"
  if is_compatible "${!vmaj}" "${!vmin}" "${!vsup}" "${!pmaj}" "${!pmin}"; then
    echo "| ${label} | $(describe "${!vmaj}" "${!vmin}" "${!vpatch}" "${!vsup}") | $(describe "${!pmaj}" "${!pmin}" "${!ppatch}" "${!psup}") | compatible |" >> "$summary"
    return 0
  fi
  echo "| ${label} | $(describe "${!vmaj}" "${!vmin}" "${!vpatch}" "${!vsup}") | $(describe "${!pmaj}" "${!pmin}" "${!ppatch}" "${!psup}") | **incompatible** |" >> "$summary"
  return 1
}

if ! row "HEAD <-> HEAD" head_v head_p; then
  error "The Paper and Velocity JARs built from this commit cannot talk to each other"
fi

# The two mixed pairs only fail on purpose when that side's protocol numbers moved; if they
# did not move, the break is not this release's doing and nothing should wave it through.
if ! row "HEAD Velocity <-> released Paper (${paper_tag})" head_v rel_p; then
  if [ "$head_v_MAJOR.$head_v_MINOR.$head_v_MIN_SUPPORTED_MINOR" = "$rel_v_MAJOR.$rel_v_MINOR.$rel_v_MIN_SUPPORTED_MINOR" ]; then
    error "Velocity no longer accepts ${paper_tag}, but its protocol range did not change"
  else
    notice "Velocity no longer accepts ${paper_tag}: update Paper before or with this Velocity"
  fi
fi

if ! row "released Velocity (${velocity_tag}) <-> HEAD Paper" rel_v head_p; then
  if [ "$head_p_MAJOR.$head_p_MINOR" = "$rel_p_MAJOR.$rel_p_MINOR" ]; then
    error "${velocity_tag} no longer accepts this Paper, but Paper's protocol MAJOR.MINOR did not change"
  else
    notice "${velocity_tag} does not accept this Paper yet: release Velocity first"
  fi
fi

exit "$STATUS"

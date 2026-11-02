#!/usr/bin/env bash
# Prints the tags a merged Release PR calls for, one per line.
#
# Usage: release-tags.sh <manifest-before.json> <manifest-after.json>
set -euo pipefail

before=$1
after=$2

version_of() {
  jq -r --arg path "$2" '.[$path] // empty' "$1"
}

# Velocity goes first: a protocol MINOR bump must reach the proxy before the backends,
# and a single push starts both release workflows in the order the tags are listed.
for path in platform-velocity .; do
  new=$(version_of "$after" "$path")
  old=$(version_of "$before" "$path")
  if [ -z "$new" ] || [ "$new" = "$old" ]; then
    continue
  fi
  case "$path" in
    .) echo "v${new}" ;;
    platform-velocity) echo "velocity/v${new}" ;;
  esac
done

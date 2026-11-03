#!/usr/bin/env bash
# Publishes a version to Modrinth, once. A version number that already exists is left alone,
# so a re-run after a later step failed does not create a duplicate.
#
# Usage: publish-modrinth.sh <jar> <version-number> <name> <changelog-file>
# Env:   MODRINTH_TOKEN, MODRINTH_PROJECT_ID,
#        MODRINTH_LOADERS (newline-separated),
#        MODRINTH_GAME_VERSIONS (newline-separated; "26.3.x" patterns are expanded),
#        DRY_RUN=1 to print the request instead of sending it.
set -euo pipefail
# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

jar=$1
version_number=$2
name=$3
changelog_file=$4

max_changelog_chars=60000

changelog=$(cat "$changelog_file")
if [ "${#changelog}" -gt "$max_changelog_chars" ]; then
  # Modrinth caps the changelog length, but the API reference gives no figure. Stay far below
  # any plausible cap, and fall back to the link line rather than fail the release.
  changelog=$(tail -n 1 "$changelog_file")
fi

lines_to_json() {
  jq -R -s 'split("\n") | map(select(length > 0))'
}

loaders=$(printf '%s\n' "${MODRINTH_LOADERS:?MODRINTH_LOADERS is required}" | lines_to_json)
game_versions=$(printf '%s\n' "${MODRINTH_GAME_VERSIONS:?MODRINTH_GAME_VERSIONS is required}" | expand_game_versions | lines_to_json)

data=$(jq -n \
  --arg project_id "${MODRINTH_PROJECT_ID:?MODRINTH_PROJECT_ID is required}" \
  --arg name "$name" \
  --arg version_number "$version_number" \
  --arg changelog "$changelog" \
  --argjson loaders "$loaders" \
  --argjson game_versions "$game_versions" \
  '{
    project_id: $project_id,
    name: $name,
    version_number: $version_number,
    changelog: $changelog,
    dependencies: [],
    game_versions: $game_versions,
    version_type: "release",
    loaders: $loaders,
    status: "listed",
    file_parts: ["jar"],
    primary_file: "jar"
  }')

if [ "${DRY_RUN:-0}" = 1 ]; then
  jq --arg n "${#changelog}" 'del(.changelog) + {changelog_chars: ($n | tonumber)}' <<< "$data"
  exit 0
fi

token=${MODRINTH_TOKEN:?MODRINTH_TOKEN is required}

existing=$(curl -sS --fail-with-body \
  -H "User-Agent: ${MODRINTH_USER_AGENT}" \
  "${MODRINTH_API}/project/${MODRINTH_PROJECT_ID}/version")
if jq -e --arg v "$version_number" 'any(.[]; .version_number == $v)' <<< "$existing" > /dev/null; then
  echo "Modrinth already has version ${version_number}; nothing to publish"
  exit 0
fi

curl -sS --fail-with-body -X POST "${MODRINTH_API}/version" \
  -H "Authorization: ${token}" \
  -H "User-Agent: ${MODRINTH_USER_AGENT}" \
  -F "data=${data};type=application/json" \
  -F "jar=@${jar};type=application/java-archive" \
  | jq '{id, version_number, status}'

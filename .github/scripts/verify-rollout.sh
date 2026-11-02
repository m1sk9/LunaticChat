#!/usr/bin/env bash
# Checks that a finished release is what it should be on GitHub, Modrinth and the site.
#
# Usage: verify-rollout.sh <tag> <paper|velocity> <version> <modrinth-version>
# Env:   GH_TOKEN, GITHUB_REPOSITORY, MODRINTH_PROJECT_ID, MODRINTH_LOADERS and
#        MODRINTH_GAME_VERSIONS (newline-separated, as passed to publish-modrinth.sh).
set -euo pipefail
# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

tag=$1
platform=$2
version=$3
modrinth_version=$4

repo=${GITHUB_REPOSITORY:-m1sk9/LunaticChat}
site=${SITE_URL:-https://lc.m1sk9.dev}

release=$(gh release view "$tag" --repo "$repo" --json isDraft,assets,body)
if [ "$(jq -r .isDraft <<< "$release")" != false ]; then
  error "GitHub Release ${tag} is still a draft"
fi
if [ "$(jq '.assets | length' <<< "$release")" -lt 1 ]; then
  error "GitHub Release ${tag} has no assets"
fi

# Velocity releases are published with --latest=false so the repository's latest stays Paper.
latest=$(gh api "repos/${repo}/releases/latest" --jq .tag_name)
case "$platform" in
  velocity)
    if [[ "$latest" == velocity/* ]]; then
      error "The latest release is ${latest}; it must stay a Paper release"
    fi
    ;;
  paper)
    if [ "$latest" != "$tag" ]; then
      error "The latest release is ${latest}, expected ${tag}"
    fi
    ;;
esac

# Modrinth can take a few minutes to list a new version.
found=
for attempt in 1 2 3 4 5 6 7 8 9 10; do
  versions=$(curl -sS --fail-with-body -H "User-Agent: ${MODRINTH_USER_AGENT}" "${MODRINTH_API}/project/${MODRINTH_PROJECT_ID:?}/version")
  found=$(jq -c --arg v "$modrinth_version" '[.[] | select(.version_number == $v)][0] // empty' <<< "$versions")
  if [ -n "$found" ] && [ "$(jq -r .status <<< "$found")" = listed ]; then
    break
  fi
  echo "Modrinth version ${modrinth_version} not listed yet (attempt ${attempt}/10)"
  sleep 30
done

if [ -z "$found" ]; then
  error "Modrinth has no version ${modrinth_version}"
else
  if [ "$(jq -r .status <<< "$found")" != listed ]; then
    error "Modrinth version ${modrinth_version} has status $(jq -r .status <<< "$found"), expected listed"
  fi

  want_loaders=$(printf '%s\n' "${MODRINTH_LOADERS:?}" | grep -v '^$' | sort | tr '\n' ' ')
  have_loaders=$(jq -r '.loaders | sort | .[]' <<< "$found" | tr '\n' ' ')
  if [ "$want_loaders" != "$have_loaders" ]; then
    error "Modrinth loaders are [${have_loaders}], expected [${want_loaders}]"
  fi

  want_game_versions=$(printf '%s\n' "${MODRINTH_GAME_VERSIONS:?}" | expand_game_versions)
  while IFS= read -r game_version; do
    if ! jq -e --arg g "$game_version" '.game_versions | index($g)' <<< "$found" > /dev/null; then
      error "Modrinth version ${modrinth_version} is not listed for ${game_version}"
    fi
  done <<< "$want_game_versions"
fi

# Every site link in the release body, plus the notes pages themselves.
urls=$(
  {
    jq -r .body <<< "$release" | grep -oE "${site}[^) ]*" || true
    echo "${site}/changelog/${platform}/v${version}"
    echo "${site}/ja/changelog/${platform}/v${version}"
  } | sort -u
)
while IFS= read -r url; do
  code=$(curl -s -o /dev/null -w '%{http_code}' -L "$url" || true)
  if [ "$code" != 200 ]; then
    error "${url} answers ${code}"
  fi
done <<< "$urls"

exit "$STATUS"

#!/usr/bin/env bash
# Checks that the site release notes for each tag exist in English and Japanese, are
# registered in the sidebars, and link to the right GitHub and Modrinth versions.
#
# Usage: check-release-notes.sh <tag>...    (v1.7.1 for Paper, velocity/v1.4.1 for Velocity)
set -euo pipefail
# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

repo_url=https://github.com/m1sk9/LunaticChat
modrinth_url=https://modrinth.com/plugin/lunaticchat/version

for tag in "$@"; do
  case "$tag" in
    velocity/v*)
      platform=velocity
      version=${tag#velocity/v}
      modrinth_version="${version}-velocity"
      ;;
    v*)
      platform=paper
      version=${tag#v}
      modrinth_version="$version"
      ;;
    *)
      error "Unrecognised tag: ${tag}"
      continue
      ;;
  esac

  for lang in en ja; do
    if [ "$lang" = en ]; then
      page="website/src/changelog/${platform}/v${version}.md"
      sidebar=website/.vitepress/config/en.ts
      link="/changelog/${platform}/v${version}"
    else
      page="website/src/ja/changelog/${platform}/v${version}.md"
      sidebar=website/.vitepress/config/ja.ts
      link="/ja/changelog/${platform}/v${version}"
    fi

    if [ ! -f "$page" ]; then
      error "Release notes for ${tag} are missing: ${page} (write them with /create-release-note)"
      continue
    fi
    if ! grep -qF "link: '${link}'" "$sidebar"; then
      error "${page} is not registered in ${sidebar}"
    fi
    if ! grep -qF "(${repo_url}/releases/tag/${tag})" "$page"; then
      error "${page} does not link to ${repo_url}/releases/tag/${tag}"
    fi
    if ! grep -qF "(${modrinth_url}/${modrinth_version})" "$page"; then
      error "${page} does not link to ${modrinth_url}/${modrinth_version}"
    fi
  done
done

exit "$STATUS"

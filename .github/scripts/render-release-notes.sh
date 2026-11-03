#!/usr/bin/env bash
# Prints the body of a release (GitHub Release and Modrinth changelog) from the English
# release notes page on the site, so the text people write in the Release PR is the only
# copy.
#
# Usage: render-release-notes.sh <paper|velocity> <version>
set -euo pipefail

platform=$1
version=$2
site=${SITE_URL:-https://lc.m1sk9.dev}
page="website/src/changelog/${platform}/v${version}.md"

if [ ! -f "$page" ]; then
  echo "Release notes page not found: ${page}" >&2
  exit 1
fi

# Dropped: the front matter, the "# vX.Y.Z" title (the release has its own) and the Download
# list (it points at the very release being read).
awk '
  NR == 1 && /^---$/ { front = 1; next }
  front && /^---$/ { front = 0; next }
  front { next }
  /^# / && !titled { titled = 1; next }
  /^- Download:/ { download = 1; next }
  download && /^[[:space:]]+-/ { next }
  download && /^[[:space:]]*$/ { download = 0; next }
  { download = 0; print }
' "$page" \
  | sed -E "s#\]\(/#](${site}/#g" \
  | sed '/./,$!d' \
  | cat -s

printf '\nFull release notes: %s/changelog/%s/v%s\n' "$site" "$platform" "$version"

#!/usr/bin/env bash
# Waits until the English and Japanese release notes of a version answer 200 on the site.
#
# The GitHub Release and the Modrinth version both link to these pages, and the site is
# deployed by CI after the Release PR is merged, so the release can start before the pages
# exist. Publishing in that window would ship dead links.
#
# Usage: wait-for-release-notes.sh <paper|velocity> <version>
set -euo pipefail
# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

platform=$1
version=$2
base=${SITE_URL:-https://lc.m1sk9.dev}
timeout=${TIMEOUT_SECONDS:-1200}
interval=${INTERVAL_SECONDS:-20}

deadline=$((SECONDS + timeout))
for path in "/changelog/${platform}/v${version}" "/ja/changelog/${platform}/v${version}"; do
  while true; do
    code=$(curl -s -o /dev/null -w '%{http_code}' -L "${base}${path}" || true)
    if [ "$code" = 200 ]; then
      echo "${base}${path}: 200"
      break
    fi
    if [ "$SECONDS" -ge "$deadline" ]; then
      error "${base}${path} still answers ${code} after ${timeout}s; has the docs deploy on main finished?"
      exit "$STATUS"
    fi
    echo "${base}${path}: ${code}, retrying in ${interval}s"
    sleep "$interval"
  done
done

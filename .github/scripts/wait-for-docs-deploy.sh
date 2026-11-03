#!/usr/bin/env bash
# Waits until the docs site has been deployed for a commit, by watching the build_docs job of
# the CI run that main's push started. The release notes the GitHub Release and the Modrinth
# version link to are part of that deploy, and publishing before it ends would ship dead links.
#
# The site itself is not fetched: it answers 403 to GitHub-hosted runners, so a page that is
# live for everyone else never looked live from here and the release stalled until it timed out.
#
# Usage: wait-for-docs-deploy.sh <commit-sha>
# Env:   GH_TOKEN, GITHUB_REPOSITORY, TIMEOUT_SECONDS, INTERVAL_SECONDS
set -euo pipefail
# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

sha=$1
repo=${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}
timeout=${TIMEOUT_SECONDS:-1200}
interval=${INTERVAL_SECONDS:-20}

deadline=$((SECONDS + timeout))
while true; do
  run_id=$(gh run list --repo "$repo" --workflow ci.yaml --commit "$sha" --event push \
    --json databaseId --jq '.[0].databaseId // empty')

  if [ -n "$run_id" ]; then
    job=$(gh api "repos/${repo}/actions/runs/${run_id}/jobs" \
      --jq '.jobs[] | select(.name == "build_docs") | "\(.status) \(.conclusion // "")"')
    status=${job%% *}
    conclusion=${job#* }

    if [ "$status" = completed ]; then
      if [ "$conclusion" = success ]; then
        echo "The docs deploy for ${sha} succeeded (CI run ${run_id})"
        exit 0
      fi
      error "The docs deploy for ${sha} ended with '${conclusion}' (CI run ${run_id}); the release notes are not on the site"
      exit "$STATUS"
    fi
    echo "build_docs for ${sha} is ${status}"
  else
    echo "No CI run for ${sha} on push yet"
  fi

  if [ "$SECONDS" -ge "$deadline" ]; then
    error "No finished docs deploy for ${sha} after ${timeout}s. Is it a commit on main?"
    exit "$STATUS"
  fi
  sleep "$interval"
done

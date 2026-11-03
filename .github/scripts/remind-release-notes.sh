#!/usr/bin/env bash
# Keeps one comment on a Release PR that tells the reader to write the release notes while
# they are missing, and removes it once they are there. The comment is found by a hidden
# marker and edited in place, so re-running the checks never piles up comments.
#
# Usage: remind-release-notes.sh <failure|success> <check-log-file>
# Env:   GH_TOKEN, GITHUB_REPOSITORY, PR_NUMBER,
#        TAGS (newline-separated tags the PR will publish),
#        DRY_RUN=1 to print the comment instead of posting it.
set -euo pipefail

outcome=$1
log_file=$2

marker='<!-- release-notes-reminder -->'
repo=${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}
pr=${PR_NUMBER:?PR_NUMBER is required}
tags=${TAGS:?TAGS is required}

existing=
if [ "${DRY_RUN:-0}" != 1 ]; then
  existing=$(gh api "repos/${repo}/issues/${pr}/comments" --paginate \
    --jq ".[] | select(.body | contains(\"${marker}\")) | .id" | head -n1)
fi

if [ "$outcome" = success ]; then
  if [ -n "$existing" ]; then
    gh api -X DELETE "repos/${repo}/issues/comments/${existing}"
    echo "Release notes found; removed the reminder comment"
  fi
  exit 0
fi

# The backticks are literal Markdown code marks, not command substitution.
# shellcheck disable=SC2016
tag_list=$(printf '%s\n' "$tags" | sed 's/.*/`&`/' | paste -sd, - | sed 's/,/, /g')
problems=$(sed -n 's/^::error:://p' "$log_file" | sed -e 's/ (write them with \/create-release-note)//' -e 's/^/- /')

body=$(cat <<EOF
${marker}
### Release notes needed

This Release PR will publish ${tag_list}.

The release notes are not on this branch yet, and the PR cannot be merged until they are:

${problems}

To write them:

1. Check out this branch: \`gh pr checkout ${pr}\`
2. Run \`/create-release-note\` in Claude Code
3. Review, commit and push to this branch. The checks run again on push.

> [!WARNING]
> release-please rebuilds this branch whenever a new releasable commit lands on \`main\`, and that discards commits added by hand. Do not merge other PRs while you write the notes. If the notes disappear, run \`/create-release-note\` again.
EOF
)

if [ "${DRY_RUN:-0}" = 1 ]; then
  echo "$body"
  exit 0
fi

if [ -n "$existing" ]; then
  gh api -X PATCH "repos/${repo}/issues/comments/${existing}" -f body="$body" > /dev/null
  echo "Updated the reminder comment"
else
  gh api -X POST "repos/${repo}/issues/${pr}/comments" -f body="$body" > /dev/null
  echo "Posted the reminder comment"
fi

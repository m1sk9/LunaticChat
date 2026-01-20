#!/bin/bash
set -e

MARKER="<!-- test-results-comment -->"

TEST_RESULTS_DIR="build/test-results/test"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0
FAILED_TEST_NAMES=""

if [ -d "$TEST_RESULTS_DIR" ]; then
  for xml_file in "$TEST_RESULTS_DIR"/*.xml; do
    if [ -f "$xml_file" ]; then
      tests=$(sed -n 's/.*tests="\([0-9]*\)".*/\1/p' "$xml_file" | head -n1)
      failures=$(sed -n 's/.*failures="\([0-9]*\)".*/\1/p' "$xml_file" | head -n1)
      skipped=$(sed -n 's/.*skipped="\([0-9]*\)".*/\1/p' "$xml_file" | head -n1)

      tests=${tests:-0}
      failures=${failures:-0}
      skipped=${skipped:-0}

      TOTAL_TESTS=$((TOTAL_TESTS + tests))
      FAILED_TESTS=$((FAILED_TESTS + failures))
      SKIPPED_TESTS=$((SKIPPED_TESTS + skipped))

      if [ "$failures" -gt 0 ]; then
        failed_names=$(sed -n 's/.*<testcase name="\([^"]*\)".*<failure.*/\1/p' "$xml_file" || echo "")
        if [ -n "$failed_names" ]; then
          FAILED_TEST_NAMES="${FAILED_TEST_NAMES}${failed_names}\n"
        fi
      fi
    fi
  done

  PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS - SKIPPED_TESTS))
fi

if [ "$FAILED_TESTS" -gt 0 ]; then
  STATUS="❌ Failed"
  STATUS_EMOJI="❌"
else
  STATUS="✅ Passed"
  STATUS_EMOJI="✅"
fi

COMMENT_BODY="${MARKER}
## ${STATUS_EMOJI} Test Results

| Status | Count |
|--------|-------|
| ✅ Passed | ${PASSED_TESTS} |
| ❌ Failed | ${FAILED_TESTS} |
| ⏭️ Skipped | ${SKIPPED_TESTS} |
| **Total** | **${TOTAL_TESTS}** |

**Overall Status:** ${STATUS}
"

if [ "$FAILED_TESTS" -gt 0 ] && [ -n "$FAILED_TEST_NAMES" ]; then
  COMMENT_BODY="${COMMENT_BODY}

<details>
<summary>❌ Failed Tests</summary>

$(echo -e "$FAILED_TEST_NAMES" | sed 's/^/- /')

</details>
"
fi

COMMENT_BODY="${COMMENT_BODY}

---
*Updated at $(date -u '+%Y-%m-%d %H:%M:%S UTC')*"

echo "$COMMENT_BODY" > /tmp/test-comment.md

PR_NUMBER="${GITHUB_REF#refs/pull/}"
PR_NUMBER="${PR_NUMBER%/merge}"

if [ -z "$PR_NUMBER" ] || [ "$PR_NUMBER" = "$GITHUB_REF" ]; then
  echo "Not a pull request, skipping comment"
  exit 0
fi

EXISTING_COMMENT_ID=$(gh api \
  "repos/$GITHUB_REPOSITORY/issues/${PR_NUMBER}/comments" \
  --jq ".[] | select(.body | contains(\"$MARKER\")) | .id" \
  | head -n 1 || echo "")

if [ -n "$EXISTING_COMMENT_ID" ]; then
  echo "Updating existing comment (ID: $EXISTING_COMMENT_ID)"
  gh api \
    "repos/$GITHUB_REPOSITORY/issues/comments/${EXISTING_COMMENT_ID}" \
    -X PATCH \
    -f body="$(cat /tmp/test-comment.md)"
else
  echo "Creating new comment"
  gh pr comment "$PR_NUMBER" --body-file /tmp/test-comment.md
fi

echo "Test results comment posted successfully"

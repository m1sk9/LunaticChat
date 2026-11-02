#!/usr/bin/env bash
# Checks that wire-format changes since the last release of each platform came with a
# ProtocolVersion change and a backward-compatibility snapshot, and says in what order
# the platforms have to be deployed.
#
# Only PluginMessage.kt and PluginMessageChannel.kt define the wire format. Other files in
# the protocol package (the codec's sanitising, hex dumps) change without touching what two
# versions send each other; requiring a bump for them would have failed v1.7.0.
# The <prefix>_KEY variables are created by load_constants (printf -v), which shellcheck
# cannot follow.
# shellcheck disable=SC2154
set -euo pipefail
# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

wire_files=("$PROTOCOL_DIR/PluginMessage.kt" "$PROTOCOL_DIR/PluginMessageChannel.kt")

for platform in paper velocity; do
  tag=$(latest_tag "$platform")
  if [ -z "$tag" ]; then
    notice "No released ${platform} tag; skipping the protocol check for it"
    continue
  fi

  load_constants old < <(git show "${tag}:${PROTOCOL_DIR}/ProtocolVersion.kt" | constants_from_source)
  load_constants new < <(constants_from_source < "$PROTOCOL_DIR/ProtocolVersion.kt")

  if ! git diff --quiet "$tag" -- "${wire_files[@]}"; then
    if [ "$old_MAJOR.$old_MINOR.$old_PATCH.$old_MIN_SUPPORTED_MINOR" = "$new_MAJOR.$new_MINOR.$new_PATCH.$new_MIN_SUPPORTED_MINOR" ]; then
      error "The wire format changed since ${tag}, but ProtocolVersion did not"
    fi
    if git diff --quiet "$tag" -- "$PROTOCOL_TEST"; then
      error "The wire format changed since ${tag}, but ProtocolBackwardCompatibilityTest has no new snapshot"
    fi
  fi

  other=$(git diff --name-only "$tag" -- "$PROTOCOL_DIR" | grep -v -e '/PluginMessage.kt$' -e '/PluginMessageChannel.kt$' -e '/ProtocolVersion.kt$' || true)
  if [ -n "$other" ]; then
    notice "Protocol package files changed since ${tag} without a wire-format change: $(echo "$other" | xargs -n1 basename | tr '\n' ' ')"
  fi

  if [ "$new_MAJOR" -gt "$old_MAJOR" ]; then
    notice "Protocol MAJOR went ${old_MAJOR} -> ${new_MAJOR} since ${tag}: release Paper and Velocity together"
  elif [ "$new_MINOR" -gt "$old_MINOR" ]; then
    notice "Protocol MINOR went ${old_MINOR} -> ${new_MINOR} since ${tag}: release Velocity first, then Paper"
  fi
done

exit "$STATUS"

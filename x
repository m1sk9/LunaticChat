#!/usr/bin/env bash

set -e

DC_PAPER="docker compose -f docker/paper/compose.yaml"
DC_VELOCITY="docker compose -f docker/velocity/compose.yaml"
DC_FOLIA="docker compose -f docker/folia/compose.yaml"

GRADLE_EXTRA_ARGS=""

# compose 側に値を複製すると Renovate の依存更新と乖離するため、
# デバッグ環境のサーバーバージョンは build.gradle.kts を単一の情報源として導出する。
# compose は未設定時に停止するので、全アクションの前に export しておく必要がある。
derive_server_versions() {
    local paper_coord velocity_coord

    paper_coord=$(grep -oE 'paper-api:[0-9.]+\.build\.[0-9]+' platform-paper/build.gradle.kts | head -1)
    if [[ -z "$paper_coord" ]]; then
        echo "Error: could not derive paper-api version from platform-paper/build.gradle.kts" >&2
        exit 1
    fi
    paper_coord=${paper_coord#paper-api:}
    PAPER_BUILD=${paper_coord##*.build.}
    PAPER_MC_VERSION=${paper_coord%%.build.*}

    velocity_coord=$(grep -oE 'velocity-api:[0-9.]+' platform-velocity/build.gradle.kts | head -1)
    if [[ -z "$velocity_coord" ]]; then
        echo "Error: could not derive velocity-api version from platform-velocity/build.gradle.kts" >&2
        exit 1
    fi
    VELOCITY_VERSION=${velocity_coord#velocity-api:}

    export PAPER_MC_VERSION PAPER_BUILD VELOCITY_VERSION
}

derive_server_versions

help() {
    cat <<EOF
Usage: ./x <action> <platform> [--stable]

Actions:
    start       Build and start containers
    stop        Stop containers
    log         Show container logs
    clean       Stop containers and remove volumes
    rcon        Open rcon-cli console (velocity: ./x rcon velocity [s1|s2])
    help        Show this help message

Platforms:
    paper       Single Paper server (localhost:25565)
    velocity    Velocity proxy + Paper s1 & s2 (localhost:25577)
    folia       Single Folia server (localhost:25565)

Options:
    --stable    Build as stable release (default: nightly)

Examples:
    ./x start paper             Build nightly and start Paper server
    ./x start velocity          Build nightly and start Velocity environment
    ./x start folia             Build nightly and start Folia server
    ./x start paper --stable    Build stable and start Paper server
    ./x log velocity            Show Velocity environment logs
    ./x rcon paper              Open rcon-cli for Paper server
    ./x stop velocity           Stop Velocity environment
    ./x clean folia             Stop Folia and remove volumes

EOF
}

# The version suffix is derived in build.gradle.kts from -PisNightly; passing -Pversion here
# has no effect, since each module assigns its own version from paperVersion/velocityVersion.
setup_nightly() {
    SHORT_HASH=$(git rev-parse --short HEAD)
    GRADLE_EXTRA_ARGS="-PisNightly=true"
    echo "==> Nightly build: nightly.${SHORT_HASH}"
}

ensure_plugin_dir() {
    mkdir -p "$1"
}

# Parse --stable flag (only setup build for actions that need it)
ACTION="${1:-}"
PLATFORM="${2:-}"

if [[ "$ACTION" == "start" ]]; then
    STABLE=false
    for arg in "$@"; do
        if [[ "$arg" == "--stable" ]]; then
            STABLE=true
        fi
    done

    if [[ "$STABLE" == "false" ]]; then
        setup_nightly
    else
        echo "==> Stable build"
    fi
fi

# --- Actions per platform ---

do_start() {
    case "$1" in
        paper)
            ./gradlew :platform-paper:clean :platform-paper:shadowJar $GRADLE_EXTRA_ARGS
            ensure_plugin_dir docker/paper/plugins
            rm -f docker/paper/plugins/*.jar
            cp platform-paper/build/libs/*.jar docker/paper/plugins/
            $DC_PAPER up
            ;;
        velocity)
            ./gradlew :platform-paper:clean :platform-paper:shadowJar :platform-velocity:clean :platform-velocity:shadowJar $GRADLE_EXTRA_ARGS
            ensure_plugin_dir docker/velocity/plugins-paper-s1
            ensure_plugin_dir docker/velocity/plugins-paper-s2
            ensure_plugin_dir docker/velocity/plugins-velocity
            rm -f docker/velocity/plugins-paper-s1/*.jar
            rm -f docker/velocity/plugins-paper-s2/*.jar
            rm -f docker/velocity/plugins-velocity/*.jar
            cp platform-paper/build/libs/*.jar docker/velocity/plugins-paper-s1/
            cp platform-paper/build/libs/*.jar docker/velocity/plugins-paper-s2/
            cp platform-velocity/build/libs/*.jar docker/velocity/plugins-velocity/
            $DC_VELOCITY up
            ;;
        folia)
            ./gradlew :platform-paper:clean :platform-paper:shadowJar $GRADLE_EXTRA_ARGS
            ensure_plugin_dir docker/folia/plugins
            rm -f docker/folia/plugins/*.jar
            cp platform-paper/build/libs/*.jar docker/folia/plugins/
            $DC_FOLIA up
            ;;
        *)
            echo "Unknown platform: ${1:-}"
            echo ""
            help
            exit 1
            ;;
    esac
}

do_stop() {
    case "$1" in
        paper)    $DC_PAPER down ;;
        velocity) $DC_VELOCITY down ;;
        folia)    $DC_FOLIA down ;;
        *)        echo "Unknown platform: ${1:-}"; exit 1 ;;
    esac
}

do_log() {
    case "$1" in
        paper)    $DC_PAPER logs -f paper ;;
        velocity) $DC_VELOCITY logs -f ;;
        folia)    $DC_FOLIA logs -f folia ;;
        *)        echo "Unknown platform: ${1:-}"; exit 1 ;;
    esac
}

do_clean() {
    case "$1" in
        paper)    $DC_PAPER down -v ;;
        velocity) $DC_VELOCITY down -v ;;
        folia)    $DC_FOLIA down -v ;;
        *)        echo "Unknown platform: ${1:-}"; exit 1 ;;
    esac
}

do_rcon() {
    case "$1" in
        paper)    $DC_PAPER exec paper rcon-cli ;;
        velocity)
            local server="${2:-s1}"
            if [[ "$server" != "s1" && "$server" != "s2" ]]; then
                echo "Error: Invalid server '$server'. Available: s1, s2"
                exit 1
            fi
            $DC_VELOCITY exec "$server" rcon-cli
            ;;
        folia)    $DC_FOLIA exec folia rcon-cli ;;
        *)        echo "Unknown platform: ${1:-}"; exit 1 ;;
    esac
}

# --- Main ---
case "$ACTION" in
    start) do_start "$PLATFORM" ;;
    stop)  do_stop "$PLATFORM" ;;
    log)   do_log "$PLATFORM" ;;
    clean) do_clean "$PLATFORM" ;;
    rcon)  do_rcon "$PLATFORM" "${3:-}" ;;
    help|"") help ;;
    *)
        echo "Unknown action: $ACTION"
        echo ""
        help
        exit 1
        ;;
esac

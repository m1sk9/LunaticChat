#!/usr/bin/env bash

set -e

DC_VELOCITY="docker compose -f docker/velocity/compose.yaml"
DC_FOLIA="docker compose -f docker/folia/compose.yaml"

help() {
    cat <<EOF
Usage: ./x <command> [options]

Velocity environment (Paper s1 + s2 + Velocity proxy):
    start           Build Paper+Velocity and start all containers
    start-s1        Build Paper and start only s1 container
    restart         Restart s1, s2 and velocity containers
    stop            Stop velocity environment
    clean           Stop velocity environment and remove volumes
    rcon [server]   Open rcon-cli for server container (default: s1)
                    Available servers: s1, s2
    logs [server]   Show server container logs (default: s1)
                    Available servers: s1, s2
    vlogs           Show velocity proxy container logs

Folia environment (single Folia server):
    folia start     Build Paper and start Folia server
    folia restart   Restart Folia server
    folia stop      Stop Folia server
    folia clean     Stop Folia server and remove volumes
    folia rcon      Open rcon-cli for Folia server
    folia logs      Show Folia server logs

General:
    help            Show this help message

EOF
}

ensure_plugin_dir() {
    mkdir -p "$1"
}

# --- Folia commands ---
folia_cmd() {
    case "${1:-}" in
        start)
            ./gradlew :platform-paper:clean :platform-paper:shadowJar
            ensure_plugin_dir docker/folia/plugins
            rm -f docker/folia/plugins/*.jar
            cp platform-paper/build/libs/*.jar docker/folia/plugins/
            $DC_FOLIA up
            ;;
        restart)
            $DC_FOLIA restart folia
            ;;
        stop)
            $DC_FOLIA down
            ;;
        clean)
            $DC_FOLIA down -v
            ;;
        rcon)
            $DC_FOLIA exec folia rcon-cli
            ;;
        logs)
            $DC_FOLIA logs -f folia
            ;;
        *)
            echo "Unknown folia command: ${1:-}"
            echo ""
            help
            exit 1
            ;;
    esac
}

# --- Main ---
case "${1:-}" in
    folia)
        folia_cmd "${2:-}"
        ;;
    start)
        ./gradlew :platform-paper:clean :platform-paper:shadowJar :platform-velocity:clean :platform-velocity:shadowJar
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
    start-s1)
        ./gradlew :platform-paper:clean :platform-paper:shadowJar
        ensure_plugin_dir docker/velocity/plugins-paper-s1
        rm -f docker/velocity/plugins-paper-s1/*.jar
        cp platform-paper/build/libs/*.jar docker/velocity/plugins-paper-s1/
        $DC_VELOCITY up s1
        ;;
    restart)
        $DC_VELOCITY restart s1 s2 velocity
        ;;
    stop)
        $DC_VELOCITY down
        ;;
    clean)
        $DC_VELOCITY down -v
        ;;
    rcon)
        SERVER="${2:-s1}"
        if [[ "$SERVER" != "s1" && "$SERVER" != "s2" ]]; then
            echo "Error: Invalid server '$SERVER'. Available servers: s1, s2"
            exit 1
        fi
        $DC_VELOCITY exec "$SERVER" rcon-cli
        ;;
    logs)
        SERVER="${2:-s1}"
        if [[ "$SERVER" != "s1" && "$SERVER" != "s2" ]]; then
            echo "Error: Invalid server '$SERVER'. Available servers: s1, s2"
            exit 1
        fi
        $DC_VELOCITY logs -f "$SERVER"
        ;;
    vlogs)
        $DC_VELOCITY logs -f velocity
        ;;
    help|"")
        help
        ;;
    *)
        echo "Unknown command: $1"
        echo ""
        help
        exit 1
        ;;
esac

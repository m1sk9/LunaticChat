#!/usr/bin/env bash

set -e

DOCKER_COMPOSE="docker compose -f docker/compose.yaml"

help() {
    cat <<EOF
Usage: ./x <command> [options]

Commands:
    start           Build with gradlew and start docker compose
    start-s1        Build Paper and start only s1 container
    restart         Restart s1, s2 and velocity containers
    stop            Stop docker compose
    clean           Stop docker compose and remove volumes
    rcon [server]   Open rcon-cli for server container (default: s1)
                    Available servers: s1, s2
    logs [server]   Show server container logs (default: s1)
                    Available servers: s1, s2
    vlogs           Show velocity container logs
    help            Show this help message

EOF
}

case "${1:-}" in
    start)
        ./gradlew :platform-paper:clean :platform-paper:shadowJar :platform-velocity:clean :platform-velocity:shadowJar
        # Copy built JARs to plugin directories
        cp platform-paper/build/libs/*.jar docker/plugins-paper-s1/
        cp platform-paper/build/libs/*.jar docker/plugins-paper-s2/
        cp platform-velocity/build/libs/*.jar docker/plugins-velocity/
        $DOCKER_COMPOSE up
        ;;
    start-s1)
        ./gradlew :platform-paper:clean :platform-paper:shadowJar
        # Copy built JAR to s1 plugin directory
        cp platform-paper/build/libs/*.jar docker/plugins-paper-s1/
        $DOCKER_COMPOSE up s1
        ;;
    restart)
        $DOCKER_COMPOSE restart s1 s2 velocity
        ;;
    stop)
        $DOCKER_COMPOSE down
        ;;
    clean)
        $DOCKER_COMPOSE down -v
        ;;
    rcon)
        SERVER="${2:-s1}"
        if [[ "$SERVER" != "s1" && "$SERVER" != "s2" ]]; then
            echo "Error: Invalid server '$SERVER'. Available servers: s1, s2"
            exit 1
        fi
        $DOCKER_COMPOSE exec $SERVER rcon-cli
        ;;
    logs)
        SERVER="${2:-s1}"
        if [[ "$SERVER" != "s1" && "$SERVER" != "s2" ]]; then
            echo "Error: Invalid server '$SERVER'. Available servers: s1, s2"
            exit 1
        fi
        $DOCKER_COMPOSE logs -f $SERVER
        ;;
    vlogs)
        $DOCKER_COMPOSE logs -f velocity
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

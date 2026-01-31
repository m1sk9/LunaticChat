#!/usr/bin/env bash

set -e

DOCKER_COMPOSE="docker compose -f docker/compose.yaml"

help() {
    cat <<EOF
Usage: ./x <command>

Commands:
    start      Build with gradlew and start docker compose
    restart    Restart minecraft and velocity containers
    stop       Stop docker compose
    clean      Stop docker compose and remove volumes
    rcon       Open rcon-cli for minecraft container
    logs       Show minecraft container logs
    vlogs      Show velocity container logs
    help       Show this help message

EOF
}

case "${1:-}" in
    start)
        ./gradlew :platform-paper:clean :platform-paper:shadowJar :platform-velocity:clean :platform-velocity:shadowJar
        $DOCKER_COMPOSE up
        ;;
    restart)
        $DOCKER_COMPOSE restart minecraft velocity
        ;;
    stop)
        $DOCKER_COMPOSE down
        ;;
    clean)
        $DOCKER_COMPOSE down -v
        ;;
    rcon)
        $DOCKER_COMPOSE exec minecraft rcon-cli
        ;;
    logs)
        $DOCKER_COMPOSE logs -f minecraft
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

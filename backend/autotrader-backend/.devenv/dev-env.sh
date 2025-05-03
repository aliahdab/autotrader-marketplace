#!/bin/zsh

# Colors for better output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Project root directory (one level up from .devenv)
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")/.." && pwd)"
cd "${PROJECT_ROOT}"

# Set the project name for Docker Compose
export COMPOSE_PROJECT_NAME="autotrader_dev"

# Function to handle errors
error_exit() {
    echo -e "${RED}Error: $1${NC}" >&2
    exit 1
}

# Help function
show_help() {
    echo "Usage: ./dev-env.sh [COMMAND]"
    echo "Commands:"
    echo "  start           - Start the development environment"
    echo "  stop            - Stop the development environment"
    echo "  restart         - Restart the development environment"
    echo "  rebuild         - Rebuild the environment (includes tests)"
    echo "  rebuild-notest  - Rebuild the environment (skips tests)"
    echo "  status          - Check the status of containers"
    echo "  logs            - Follow the logs from all containers"
    echo "  test            - Run tests in the development environment"
    echo "  endpoints       - Show all API endpoints"
    echo "  health          - Run a quick health check of all services"
    echo "  help            - Show this help message"
    exit 0
}

# Check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        error_exit "Docker is not running. Please start Docker and try again."
    fi
}

# Start the development environment
start_dev_env() {
    # Load environment variables
    if [ -f .devenv/.env ]; then
        echo -e "${YELLOW}Loading environment from .env file...${NC}"
        source .devenv/.env
    fi
    
    # Load local overrides if available
    if [ -f .devenv/.env.local ]; then
        echo -e "${YELLOW}Loading local environment overrides...${NC}"
        source .devenv/.env.local
    fi
    
    echo -e "${YELLOW}Starting development environment...${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml up -d || error_exit "Failed to start services"
    
    # Wait for app to be healthy
    echo -e "${YELLOW}Waiting for services to be ready...${NC}"
    for i in {1..30}; do
        if curl -s http://localhost:${SERVER_PORT:-8080}/status > /dev/null; then
            break
        fi
        echo -n "."
        sleep 1
    done
    echo ""
    
    echo -e "${GREEN}Development environment started successfully!${NC}"
    echo -e "${YELLOW}Services available at:${NC}"
    echo -e "- Spring Boot App:  ${GREEN}http://localhost:${SERVER_PORT:-8080}${NC}"
    echo -e "- API Documentation: ${GREEN}http://localhost:${SERVER_PORT:-8080}/swagger-ui/index.html${NC}"
    echo -e "- MinIO Console:    ${GREEN}http://localhost:${MINIO_CONSOLE_PORT:-9001}${NC} (${MINIO_ROOT_USER:-minioadmin}/${MINIO_ROOT_PASSWORD:-minioadmin})" # Reverted port
    echo -e "- Adminer:          ${GREEN}http://localhost:8081${NC} (Server: postgres, User: ${DB_USER:-postgres}, Password: ${DB_PASSWORD:-postgres})"
    
    # Debugging info
    echo -e "- Debug Port:       ${GREEN}${JVM_DEBUG_PORT:-5005}${NC} (attach your IDE for debugging)"
    
    # Check if Redis is used
    if [ "${REDIS_ENABLED:-true}" = "true" ] && docker-compose -f .devenv/docker-compose.dev.yml ps | grep -q redis; then
        echo -e "- Redis:            ${GREEN}localhost:${REDIS_PORT:-6379}${NC}"
    fi
    
    echo -e "\n${GREEN}--------------------------------------------${NC}"
    echo -e "${YELLOW}Environment ready for testing and development!${NC}"
    echo -e "${GREEN}--------------------------------------------${NC}"
}

# Stop the development environment
stop_dev_env() {
    echo -e "${YELLOW}Stopping development environment...${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml down || error_exit "Failed to stop services"
    echo -e "${GREEN}Development environment stopped${NC}"
}

# Rebuild the development environment (down, pull, build, up) - Includes Tests
rebuild_dev_env() {
    echo -e "${YELLOW}Stopping and removing containers...${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml down -v || error_exit "Failed to stop services"
    echo -e "${YELLOW}Pulling latest base images (including MinIO)...${NC}"
    docker pull gradle:8.5-jdk21 || echo -e "${YELLOW}Warning: Failed to pull gradle image, using cache.${NC}"
    docker pull postgres:15-alpine || echo -e "${YELLOW}Warning: Failed to pull postgres image, using cache.${NC}"
    docker pull minio/minio:latest || error_exit "Failed to pull MinIO image"
    docker pull minio/mc:latest || error_exit "Failed to pull MinIO client image"
    docker pull adminer:latest || echo -e "${YELLOW}Warning: Failed to pull adminer image, using cache.${NC}"
    echo -e "${YELLOW}Building application (including tests)...${NC}"
    ./gradlew clean build || error_exit "Gradle build failed (check tests)"
    echo -e "${YELLOW}Building and starting Docker environment...${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml up --build -d || error_exit "Failed to build or start services"
    start_dev_env # Reuse start logic for waiting and showing info
}

# Rebuild the development environment (down, pull, build, up) - Skips Tests
rebuild_dev_env_notest() {
    echo -e "${YELLOW}Stopping and removing containers...${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml down -v || error_exit "Failed to stop services"
    echo -e "${YELLOW}Pulling latest base images (including MinIO)...${NC}"
    docker pull gradle:8.5-jdk21 || echo -e "${YELLOW}Warning: Failed to pull gradle image, using cache.${NC}"
    docker pull postgres:15-alpine || echo -e "${YELLOW}Warning: Failed to pull postgres image, using cache.${NC}"
    docker pull minio/minio:latest || error_exit "Failed to pull MinIO image"
    docker pull minio/mc:latest || error_exit "Failed to pull MinIO client image"
    docker pull adminer:latest || echo -e "${YELLOW}Warning: Failed to pull adminer image, using cache.${NC}"
    echo -e "${YELLOW}Building application (skipping tests)...${NC}"
    ./gradlew clean build -x test || error_exit "Gradle build failed"
    echo -e "${YELLOW}Building and starting Docker environment...${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml up --build -d || error_exit "Failed to build or start services"
    start_dev_env # Reuse start logic for waiting and showing info
}

# Check status of containers
check_status() {
    echo -e "${YELLOW}Development environment status:${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml ps
}

# Show logs from all containers
show_logs() {
    echo -e "${YELLOW}Following logs from all containers. Press Ctrl+C to exit.${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml logs -f
}

# Run tests in the dev environment
run_tests() {
    echo -e "${YELLOW}Running tests in development environment...${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml exec app gradle test || error_exit "Tests failed"
    echo -e "${GREEN}Tests completed successfully${NC}"
}

# Show all api endpoints from Spring Boot
show_api_endpoints() {
    echo -e "${YELLOW}Fetching API endpoints...${NC}"
    docker-compose -f .devenv/docker-compose.dev.yml exec app curl -s http://localhost:8080/actuator/mappings | \
        grep -o '"patterns":\[[^]]*\]' | \
        sed 's/"patterns":\[//g' | \
        sed 's/\]//g' | \
        sed 's/","/\n/g' | \
        sed 's/"//g' | \
        sort | \
        grep -v '/actuator' | \
        grep -v '/error' | \
        grep -v '/swagger' | \
        grep -v '/v3/api-docs' | \
        while read endpoint; do
            echo -e "${GREEN}$endpoint${NC}"
        done
}

# Run a quick health check
health_check() {
    echo -e "${YELLOW}Running health check...${NC}"
    
    if curl -s http://localhost:${SERVER_PORT:-8080}/status > /dev/null; then
        echo -e "${GREEN}✓ Spring Boot API is running${NC}"
    else
        echo -e "${RED}✗ Spring Boot API is not available${NC}"
    fi
    
    if curl -s http://localhost:${MINIO_API_PORT:-9000}/minio/health/live > /dev/null; then
        echo -e "${GREEN}✓ MinIO is running${NC}"
    else
        echo -e "${RED}✗ MinIO is not available${NC}"
    fi
    
    if pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PostgreSQL is running${NC}"
    else
        echo -e "${RED}✗ PostgreSQL is not available${NC}"
    fi
    
    if [ "${REDIS_ENABLED:-true}" = "true" ]; then
        if redis-cli -h localhost -p ${REDIS_PORT:-6379} ping > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Redis is running${NC}"
        else
            echo -e "${RED}✗ Redis is not available${NC}"
        fi
    fi
}

# Main script
check_docker

if [ $# -eq 0 ]; then
    show_help
fi

case "$1" in
    start)
        start_dev_env
        ;;
    stop)
        stop_dev_env
        ;;
    restart)
        stop_dev_env
        start_dev_env
        ;;
    rebuild)
        rebuild_dev_env
        ;;
    rebuild-notest) # Added new command
        rebuild_dev_env_notest
        ;;
    status)
        check_status
        ;;
    logs)
        show_logs
        ;;
    test)
        run_tests
        ;;
    endpoints)
        show_api_endpoints
        ;;
    health)
        health_check
        ;;
    help)
        show_help
        ;;
    *)
        error_exit "Unknown command: $1. Use 'help' to see available commands."
        ;;
esac

exit 0

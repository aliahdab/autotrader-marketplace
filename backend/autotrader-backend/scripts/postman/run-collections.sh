#!/bin/bash
#
# Postman Collections Runner Script
#
# This script runs Postman collections using Newman.
# It supports running individual collections or all collections at once.
#
# Usage: ./run-collections.sh [collection_name] [--all] [--start-env] [--force]
#   collection_name  Name of the collection to run (without .json extension)
#   --all            Run all collections
#   --start-env      Start the development environment if not running
#   --force          Continue even if environment checks fail
#   --help           Show this help message
#

# Import common utility functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$PROJECT_ROOT/scripts/utils/template.sh"

# Override the template's cleanup trap to do nothing
# as we don't want to automatically shut down the environment
trap - EXIT

# Set variables
POSTMAN_DIR="$PROJECT_ROOT/src/test/resources/postman"

# Check if the Postman directory exists
if [ ! -d "$POSTMAN_DIR" ]; then
    print_warning "Postman directory not found at: $POSTMAN_DIR"
    print_info "Trying alternate location..."
    POSTMAN_DIR="/Users/aliahdab/Documents/Dev/ali/autotrader-marketplace/backend/autotrader-backend/src/test/resources/postman"
    if [ ! -d "$POSTMAN_DIR" ]; then
        print_error "Unable to find Postman directory"
        exit 1
    else
        print_success "Found Postman directory at: $POSTMAN_DIR"
    fi
fi

COLLECTIONS_DIR="$POSTMAN_DIR/collections"
ENVIRONMENT_FILE="$POSTMAN_DIR/environment.json"

# Function to run a collection
run_collection() {
  local collection_file=$1
  local collection_name=$(basename "$collection_file" .json)
  
  print_info "Running collection: $collection_name"
  
  # Run the collection with Newman
  if newman run "$collection_file" -e "$ENVIRONMENT_FILE" --color auto; then
    print_success "Collection $collection_name passed"
    return 0
  else
    print_error "Collection $collection_name failed"
    return 1
  fi
}

# Function to run all collections
run_all_collections() {
  print_info "Running all collections"
  
  local all_passed=true
  local collections=("$COLLECTIONS_DIR"/*.json)
  
  # Check if combined collection exists and use it if available
  if [[ -f "$POSTMAN_DIR/autotrader-api-collection.json" ]]; then
    print_info "Using combined API collection"
    if newman run "$POSTMAN_DIR/autotrader-api-collection.json" -e "$ENVIRONMENT_FILE" --color auto; then
      print_success "All tests passed"
      return 0
    else
      print_error "Some tests failed"
      return 1
    fi
  else
    # Run each collection individually
    for collection in "${collections[@]}"; do
      if ! run_collection "$collection"; then
        all_passed=false
      fi
    done
    
    if [ "$all_passed" = true ]; then
      print_success "All collections passed"
      return 0
    else
      print_error "Some collections failed"
      return 1
    fi
  fi
}

# Show usage information
show_usage() {
  echo "Usage: $(basename "$0") [collection_name] [--all] [--start-env] [--force]"
  echo "  collection_name  Name of the collection to run (without .json extension)"
  echo "  --all            Run all collections"
  echo "  --start-env      Start the development environment if not running"
  echo "  --force          Continue even if environment checks fail"
  echo "  --help           Show this help message"
}

# Main function
main() {
  # Check if Newman is installed
  if ! command_exists newman; then
    print_error "Newman is not installed. Please install it using: npm install -g newman"
    exit 1
  fi
  
  # Parse specific arguments for this script
  local collection=""
  local run_all=false
  
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --all)
        run_all=true
        shift
        ;;
      --start-env|--force|--help)
        # These are handled by the template
        shift
        ;;
      *)
        if [[ -z "$collection" ]]; then
          collection="$1"
        fi
        shift
        ;;
    esac
  done
  
  # Ensure API is running
  if ! check_api_running; then
    if [ "$FORCE" = false ]; then
      print_error "API is not running. Cannot run Postman tests."
      print_info "Use --start-env to start the development environment"
      print_info "or --force to run tests anyway."
      exit 1
    else
      print_warning "API is not running but continuing due to --force flag"
    fi
  fi
  
  # Run the appropriate collection(s)
  if [ "$run_all" = true ]; then
    run_all_collections
  elif [ -n "$collection" ]; then
    local collection_file="$COLLECTIONS_DIR/${collection}.json"
    if [ -f "$collection_file" ]; then
      run_collection "$collection_file"
    else
      print_error "Collection not found: $collection"
      print_info "Available collections:"
      ls -1 "$COLLECTIONS_DIR" | sed 's/\.json$//'
      exit 1
    fi
  else
    print_info "No collection specified, running all collections"
    run_all_collections
  fi
}

# Initialize and run
main "$@"

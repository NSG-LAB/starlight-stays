#!/usr/bin/env bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}Stopping Starlight Stays platform...${NC}"

# 1. Stop Vite dev server process if running
VITE_PID=$(pgrep -f "vite --port 5173" || true)
if [ -n "$VITE_PID" ]; then
  echo -e "Stopping Vite frontend dev server (PID: $VITE_PID)..."
  kill -9 $VITE_PID 2>/dev/null || true
fi

# 2. Stop Docker Compose mesh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STARLIGHT_DIR="$SCRIPT_DIR/starlight-stays"

if [ -d "$STARLIGHT_DIR" ]; then
  echo -e "Stopping all Docker containers in Starlight Stays mesh..."
  cd "$STARLIGHT_DIR"
  docker compose down
fi

echo -e "${GREEN}✓ Starlight Stays platform stopped cleanly.${NC}"

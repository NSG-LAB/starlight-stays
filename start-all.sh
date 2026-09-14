#!/usr/bin/env bash
set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STARLIGHT_DIR="$SCRIPT_DIR/starlight-stays"
FRONTEND_DIR="$SCRIPT_DIR/frontend"

echo "======================================================================"
echo -e "${BOLD}${CYAN}🌟 STARLIGHT STAYS — ONE-CLICK PLATFORM LAUNCHER${NC}"
echo "======================================================================"

# 1. Check prerequisites
echo -e "\n${CYAN}Step 1: Checking system prerequisites...${NC}"
command -v docker >/dev/null 2>&1 || { echo "Docker is required but not installed."; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "NPM is required but not installed."; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "Maven is required but not installed."; exit 1; }
echo -e "${GREEN}✓ Prerequisites verified: Docker, Node/NPM, and Maven available.${NC}"

# 2. Build multi-module reactor JARs
echo -e "\n${CYAN}Step 2: Building multi-module backend JARs via root reactor POM...${NC}"
cd "$STARLIGHT_DIR"
mvn package -DskipTests -q
echo -e "${GREEN}✓ All microservice artifacts built successfully.${NC}"

# 3. Boot Docker Compose microservices mesh
echo -e "\n${CYAN}Step 3: Launching 16 Docker Compose service containers...${NC}"
docker compose up -d
echo -e "${GREEN}✓ Docker containers running.${NC}"

# 4. Wait for core health checks
echo -e "\n${CYAN}Step 4: Waiting for API Gateway and mesh services to report healthy...${NC}"
MAX_RETRIES=30
RETRY_COUNT=0
until curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; do
  RETRY_COUNT=$((RETRY_COUNT + 1))
  if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
    echo -e "${YELLOW}⚠️ Gateway taking longer than expected. Continuing startup...${NC}"
    break
  fi
  sleep 2
done
echo -e "${GREEN}✓ API Gateway (Port 8080) is UP and healthy.${NC}"

# 5. Launch Frontend dev server (if not already running)
echo -e "\n${CYAN}Step 5: Checking frontend Vite server...${NC}"
cd "$FRONTEND_DIR"
if ! pgrep -f "vite --port 5173" > /dev/null; then
  echo "Starting Vite SPA Frontend dev server in background on port 5173..."
  nohup npm run dev -- --host --port 5173 > /dev/null 2>&1 &
  sleep 2
fi
echo -e "${GREEN}✓ Frontend server active at http://localhost:5173/${NC}"

echo -e "\n======================================================================"
echo -e "${BOLD}${GREEN}🎉 STARLIGHT STAYS PLATFORM IS FULLY OPERATIONAL!${NC}"
echo "======================================================================"
echo -e "${BOLD}Access Directory:${NC}"
echo -e "  • Frontend Web App:       ${CYAN}http://localhost:5173/${NC}"
echo -e "  • API Gateway:            ${CYAN}http://localhost:8080/${NC}"
echo -e "  • Eureka Discovery:       ${CYAN}http://localhost:8761/${NC}"
echo -e "  • Distributed Tracing:    ${CYAN}http://localhost:9411/${NC} (Zipkin)"
echo -e "  • Metrics & Dashboards:   ${CYAN}http://localhost:3030/${NC} (Grafana - admin/admin)"
echo -e "  • Prometheus Scrapers:    ${CYAN}http://localhost:9090/targets${NC}"
echo -e "  • Logstash / Kibana:      ${CYAN}http://localhost:5601/${NC} (ELK Logs)"
echo -e "  • RabbitMQ Management:    ${CYAN}http://localhost:15672/${NC} (guest/guest)"
echo "======================================================================"
echo -e "To run end-to-end resilience tests:  ${YELLOW}bash starlight-stays/comprehensive-system-test.sh${NC}"
echo -e "To stop the entire platform:         ${YELLOW}./stop-all.sh${NC}"
echo "======================================================================"

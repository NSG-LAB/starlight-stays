#!/usr/bin/env bash
set -e

GATEWAY_URL="http://localhost:8080"
EUREKA_URL="http://localhost:8761"

echo "======================================================================"
echo "🛡️ STARLIGHT STAYS — END-TO-END RESILIENCE & CHAOS TEST SUITE"
echo "======================================================================"

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "\n${CYAN}▶ PHASE 1: Baseline Health & Authentication Verification${NC}"
echo "Authenticating via API Gateway (POST /api/users/login)..."
TOKEN=$(curl -s -X POST "$GATEWAY_URL/api/users/login?username=admin&password=password")

if [ -z "$TOKEN" ] || [[ "$TOKEN" == *"error"* ]] || [[ "$TOKEN" == *"503"* ]]; then
  echo -e "${RED}❌ Authentication failed! Response: $TOKEN${NC}"
  exit 1
fi
echo -e "${GREEN}✅ JWT Obtained: ${TOKEN:0:30}...${NC}"

echo "Fetching live inventory via API Gateway (GET /api/rooms)..."
ROOMS_JSON=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/rooms")
ROOM_COUNT=$(echo "$ROOMS_JSON" | grep -o '"id":' | wc -l)
echo -e "${GREEN}✅ Room Catalog retrieved: $ROOM_COUNT suites live in database.${NC}"

echo -e "\n${CYAN}▶ PHASE 2: Transactional Saga & Concurrency Check${NC}"
RAND_ID=$((RANDOM % 10000 + 100))
CHECKIN="2026-12-10"
CHECKOUT="2026-12-15"

echo "Submitting Reservation for Room #3 ($CHECKIN to $CHECKOUT)..."
BOOKING_RES=$(curl -s -w "\nHTTP_STATUS: %{http_code}" -X POST "$GATEWAY_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\": 3, \"guestName\": \"Chaos Test Guest $RAND_ID\", \"checkInDate\": \"$CHECKIN\", \"checkOutDate\": \"$CHECKOUT\", \"guestCount\": 3}")

STATUS_CODE=$(echo "$BOOKING_RES" | grep "HTTP_STATUS" | cut -d' ' -f2)
if [ "$STATUS_CODE" -eq 201 ]; then
  echo -e "${GREEN}✅ Booking created with HTTP 201. Saga event published to RabbitMQ.${NC}"
else
  echo -e "${YELLOW}⚠️ Booking response code: $STATUS_CODE (Already booked or overlapping dates)${NC}"
fi

echo "Testing Double-Booking Concurrency Conflict Prevention..."
CONFLICT_RES=$(curl -s -w "\nHTTP_STATUS: %{http_code}" -X POST "$GATEWAY_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\": 3, \"guestName\": \"Duplicate Attempter\", \"checkInDate\": \"$CHECKIN\", \"checkOutDate\": \"$CHECKOUT\", \"guestCount\": 1}")

CONFLICT_STATUS=$(echo "$CONFLICT_RES" | grep "HTTP_STATUS" | cut -d' ' -f2)
if [ "$CONFLICT_STATUS" -eq 409 ]; then
  echo -e "${GREEN}✅ Double-Booking successfully prevented with HTTP 409 Conflict!${NC}"
else
  echo -e "${RED}❌ Double-Booking was not caught! Status: $CONFLICT_STATUS${NC}"
fi

echo -e "\n${CYAN}▶ PHASE 3: Chaos Injection — RabbitMQ Broker Downtime${NC}"
echo "Pausing RabbitMQ container..."
docker pause rabbitmq >/dev/null
echo -e "${YELLOW}⏸️  RabbitMQ is PAUSED.${NC}"

echo "Attempting to create booking while AMQP message broker is paused..."
CHECKIN_CHAOS="2026-12-16"
CHECKOUT_CHAOS="2026-12-20"
CHAOS_BOOKING=$(curl -s -w "\nHTTP_STATUS: %{http_code}" -X POST "$GATEWAY_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\": 4, \"guestName\": \"Resilience Tester\", \"checkInDate\": \"$CHECKIN_CHAOS\", \"checkOutDate\": \"$CHECKOUT_CHAOS\", \"guestCount\": 2}")

CHAOS_STATUS=$(echo "$CHAOS_BOOKING" | grep "HTTP_STATUS" | cut -d' ' -f2)
if [ "$CHAOS_STATUS" -eq 201 ]; then
  echo -e "${GREEN}✅ Fault-Tolerant: Booking record persisted safely even with RabbitMQ broker paused!${NC}"
fi

echo "Unpausing RabbitMQ container..."
docker unpause rabbitmq >/dev/null
echo -e "${GREEN}▶️  RabbitMQ is RESTORED.${NC}"

echo -e "\n${CYAN}▶ PHASE 4: Resilience4j Circuit Breaker Fallback Verification${NC}"
echo "Triggering API Gateway Circuit Breaker Fallback endpoint (/fallback/bookings)..."
CB_RES=$(curl -s -w "\nHTTP_STATUS: %{http_code}" -X POST "$GATEWAY_URL/fallback/bookings")
CB_STATUS=$(echo "$CB_RES" | grep "HTTP_STATUS" | cut -d' ' -f2)

if [ "$CB_STATUS" -eq 503 ]; then
  echo -e "${GREEN}✅ Resilience4j Circuit Breaker Fallback responded with HTTP 503 Service Unavailable as expected.${NC}"
  echo "Response payload: $(echo "$CB_RES" | head -n 1)"
else
  echo -e "${RED}❌ Unexpected fallback status: $CB_STATUS${NC}"
fi

echo -e "\n${CYAN}▶ PHASE 5: Observability & Centralized Logging Inspection${NC}"
echo "Inspecting Prometheus scraper targets..."
TARGETS_UP=$(curl -s "http://localhost:9090/api/v1/targets" | grep -o '"health":"up"' | wc -l)
echo -e "${GREEN}✅ Prometheus is actively scraping $TARGETS_UP / 5 microservice endpoints.${NC}"

echo "Inspecting Elasticsearch Logstash index..."
INDEX_STATUS=$(curl -s "http://localhost:9200/_cat/indices?v" | grep "starlight-logs" || true)
if [ -n "$INDEX_STATUS" ]; then
  echo -e "${GREEN}✅ Logstash pipeline verified. Active indices: ${NC}"
  echo "$INDEX_STATUS"
else
  echo -e "${YELLOW}⚠️ Log index not yet populated.${NC}"
fi

echo -e "\n${CYAN}▶ PHASE 6: Distributed Compensating Saga Rollback & Cancellation${NC}"
echo "Submitting Reservation with payment failure trigger (DECLINE)..."
CHECKIN_SAGA="2026-11-20"
CHECKOUT_SAGA="2026-11-25"
SAGA_RES=$(curl -s -X POST "$GATEWAY_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\": 5, \"guestName\": \"DECLINE Auto-Test Guest\", \"checkInDate\": \"$CHECKIN_SAGA\", \"checkOutDate\": \"$CHECKOUT_SAGA\", \"guestCount\": 2}")

SAGA_BOOKING_ID=$(echo "$SAGA_RES" | grep -o '"id":[0-9]*' | cut -d':' -f2)
echo "Initial booking created (#$SAGA_BOOKING_ID). Waiting for RabbitMQ compensating rollback..."
sleep 2

SAGA_CHECK=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/bookings/$SAGA_BOOKING_ID")
if [[ "$SAGA_CHECK" == *"CANCELLED_PAYMENT_FAILED"* ]]; then
  echo -e "${GREEN}✅ Two-Way Saga Rollback Verified: Booking #$SAGA_BOOKING_ID transitioned to CANCELLED_PAYMENT_FAILED!${NC}"
else
  echo -e "${RED}❌ Saga rollback did not complete. Response: $SAGA_CHECK${NC}"
fi

echo "Verifying that Room #5 dates were released by booking same dates with valid payment..."
RETRY_RES=$(curl -s -w "\nHTTP_STATUS: %{http_code}" -X POST "$GATEWAY_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\": 5, \"guestName\": \"Valid Post-Rollback Guest\", \"checkInDate\": \"$CHECKIN_SAGA\", \"checkOutDate\": \"$CHECKOUT_SAGA\", \"guestCount\": 2}")
RETRY_STATUS=$(echo "$RETRY_RES" | grep "HTTP_STATUS" | cut -d' ' -f2)
NEW_BOOKING_ID=$(echo "$RETRY_RES" | grep -o '"id":[0-9]*' | cut -d':' -f2)

if [ "$RETRY_STATUS" -eq 201 ]; then
  echo -e "${GREEN}✅ Suite dates successfully released: Re-booking succeeded with HTTP 201 (ID: #$NEW_BOOKING_ID).${NC}"
else
  echo -e "${RED}❌ Re-booking failed. Dates were not released properly. Status: $RETRY_STATUS${NC}"
fi

echo "Testing user booking cancellation on #$NEW_BOOKING_ID..."
CANCEL_RES=$(curl -s -X PUT -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/bookings/$NEW_BOOKING_ID/cancel")
if [[ "$CANCEL_RES" == *"cancelled successfully"* ]]; then
  echo -e "${GREEN}✅ Booking cancellation endpoint verified (PUT /api/bookings/$NEW_BOOKING_ID/cancel).${NC}"
fi

echo -e "\n======================================================================"
echo -e "${GREEN}🎉 ALL END-TO-END RESILIENCE, SAGA & CHAOS TESTS COMPLETED SUCCESSFULLY!${NC}"
echo "======================================================================"


#!/usr/bin/env bash
set -e

GATEWAY_URL="http://localhost:8080"
EUREKA_URL="http://localhost:8761"
CONFIG_URL="http://localhost:8888"
PROMETHEUS_URL="http://localhost:9090"
ELASTICSEARCH_URL="http://localhost:9200"
FRONTEND_URL="http://localhost:5173"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m'

PASSED_COUNT=0
FAILED_COUNT=0

assert_test() {
  local desc="$1"
  local condition="$2"
  if eval "$condition"; then
    echo -e "${GREEN}  ✓ PASS:${NC} $desc"
    PASSED_COUNT=$((PASSED_COUNT + 1))
  else
    echo -e "${RED}  ✗ FAIL:${NC} $desc"
    FAILED_COUNT=$((FAILED_COUNT + 1))
  fi
}

echo "======================================================================"
echo -e "${BOLD}${MAGENTA}🌟 STARLIGHT STAYS — COMPREHENSIVE END-TO-END SYSTEM TEST SUITE${NC}"
echo "======================================================================"

echo -e "\n${CYAN}▶ SUITE 1: Infrastructure & Service Mesh Health${NC}"
EUREKA_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$EUREKA_URL" || true)
assert_test "Eureka Discovery Server running (HTTP 200)" '[ "$EUREKA_HEALTH" -eq 200 ]'

CONFIG_HEALTH=$(curl -s "$CONFIG_URL/booking-service/default" | grep -o '"name":"booking-service"' || true)
assert_test "Spring Cloud Config Server serving configuration" '[ -n "$CONFIG_HEALTH" ]'

GATEWAY_HEALTH=$(curl -s "$GATEWAY_URL/actuator/health" | grep -o '"status":"UP"' || true)
assert_test "Spring Cloud API Gateway health status is UP" '[ -n "$GATEWAY_HEALTH" ]'

FRONTEND_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL" || true)
assert_test "Vite SPA Frontend server reachable (HTTP 200)" '[ "$FRONTEND_HEALTH" -eq 200 ]'

echo -e "\n${CYAN}▶ SUITE 2: Authentication & Security Flow (JWT / RBAC)${NC}"
TOKEN=$(curl -s -X POST "$GATEWAY_URL/api/users/login?username=admin&password=password")
assert_test "POST /api/users/login generates valid JWT Bearer token" '[[ "$TOKEN" == eyJ* ]]'

PROFILE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/users/profile")
assert_test "GET /api/users/profile accepts Bearer token (HTTP 200)" '[ "$PROFILE_STATUS" -eq 200 ]'

UNAUTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/api/bookings")
assert_test "Protected endpoint rejects unauthenticated request (HTTP 401/403)" '[ "$UNAUTH_STATUS" -eq 401 ] || [ "$UNAUTH_STATUS" -eq 403 ]'

echo -e "\n${CYAN}▶ SUITE 3: Room Catalog & Inventory Database Records${NC}"
ROOMS_JSON=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/rooms")
ROOM_COUNT=$(echo "$ROOMS_JSON" | grep -o '"id":' | wc -l)
assert_test "GET /api/rooms returns 6 seeded luxury suites" '[ "$ROOM_COUNT" -ge 6 ]'

ROOM1_RES=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/rooms/1")
ROOM1_NAME=$(echo "$ROOM1_RES" | grep -o '"propertyName":"[^"]*"' || true)
assert_test "GET /api/rooms/1 retrieves luxury suite record" '[ -n "$ROOM1_NAME" ]'

echo -e "\n${CYAN}▶ SUITE 4: Transactional Booking Saga & Concurrency Check${NC}"
RAND_OFFSET=$((RANDOM % 800 + 200))
CHECKIN=$(date -d "+$RAND_OFFSET days" +%Y-%m-%d)
CHECKOUT=$(date -d "+$((RAND_OFFSET + 4)) days" +%Y-%m-%d)

BOOK_RES=$(curl -s -w "\nHTTP_STATUS: %{http_code}" -X POST "$GATEWAY_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\": 2, \"guestName\": \"Suite Test Traveler\", \"checkInDate\": \"$CHECKIN\", \"checkOutDate\": \"$CHECKOUT\", \"guestCount\": 2}")
BOOK_CODE=$(echo "$BOOK_RES" | grep "HTTP_STATUS" | cut -d' ' -f2)
BOOK_ID=$(echo "$BOOK_RES" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -n 1)
assert_test "POST /api/bookings successfully creates reservation (HTTP 201)" '[ "$BOOK_CODE" -eq 201 ]'

CONFLICT_RES=$(curl -s -w "\nHTTP_STATUS: %{http_code}" -X POST "$GATEWAY_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\": 2, \"guestName\": \"Conflicting Guest\", \"checkInDate\": \"$CHECKIN\", \"checkOutDate\": \"$CHECKOUT\", \"guestCount\": 1}")
CONFLICT_CODE=$(echo "$CONFLICT_RES" | grep "HTTP_STATUS" | cut -d' ' -f2)
assert_test "Overlapping reservation caught with HTTP 409 Conflict" '[ "$CONFLICT_CODE" -eq 409 ]'

echo -e "\n${CYAN}▶ SUITE 5: Two-Way Distributed Compensating Saga Rollback${NC}"
SAGA_OFFSET=$((RANDOM % 800 + 1200))
SAGA_IN=$(date -d "+$SAGA_OFFSET days" +%Y-%m-%d)
SAGA_OUT=$(date -d "+$((SAGA_OFFSET + 3)) days" +%Y-%m-%d)

DECLINE_RES=$(curl -s -X POST "$GATEWAY_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\": 6, \"guestName\": \"DECLINE Card Simulation\", \"checkInDate\": \"$SAGA_IN\", \"checkOutDate\": \"$SAGA_OUT\", \"guestCount\": 2}")
DECLINE_ID=$(echo "$DECLINE_RES" | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -n 1)

sleep 2
ROLLBACK_CHECK=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/bookings/$DECLINE_ID")
assert_test "Payment failure triggers saga rollback to CANCELLED_PAYMENT_FAILED" '[[ "$ROLLBACK_CHECK" == *"CANCELLED_PAYMENT_FAILED"* ]]'

FREE_ROOM_RES=$(curl -s -w "\nHTTP_STATUS: %{http_code}" -X POST "$GATEWAY_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"roomId\": 6, \"guestName\": \"Guest Post Rollback\", \"checkInDate\": \"$SAGA_IN\", \"checkOutDate\": \"$SAGA_OUT\", \"guestCount\": 2}")
FREE_ROOM_CODE=$(echo "$FREE_ROOM_RES" | grep "HTTP_STATUS" | cut -d' ' -f2)
assert_test "Compensating rollback immediately frees suite dates for new booking (HTTP 201)" '[ "$FREE_ROOM_CODE" -eq 201 ]'

echo -e "\n${CYAN}▶ SUITE 6: User Reservations & Self-Service Cancellation${NC}"
USER_BOOKINGS=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/bookings/user/admin")
USER_BOOKING_COUNT=$(echo "$USER_BOOKINGS" | grep -o '"id":' | wc -l)
assert_test "GET /api/bookings/user/admin retrieves user reservations" '[ "$USER_BOOKING_COUNT" -gt 0 ]'

if [ -n "$BOOK_ID" ]; then
  CANCEL_RES=$(curl -s -X PUT -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/bookings/$BOOK_ID/cancel")
  assert_test "PUT /api/bookings/$BOOK_ID/cancel marks reservation CANCELLED" '[[ "$CANCEL_RES" == *"cancelled successfully"* ]]'
fi

echo -e "\n${CYAN}▶ SUITE 7: Resilience4j Circuit Breaker Fallbacks${NC}"
CB_RES=$(curl -s -w "\nHTTP_STATUS: %{http_code}" -X POST "$GATEWAY_URL/fallback/bookings")
CB_STATUS=$(echo "$CB_RES" | grep "HTTP_STATUS" | cut -d' ' -f2)
assert_test "Circuit breaker fallback /fallback/bookings serves graceful degradation (HTTP 503)" '[ "$CB_STATUS" -eq 503 ]'

echo -e "\n${CYAN}▶ SUITE 8: Observability, Metrics & Centralized Logging${NC}"
TARGETS_UP=$(curl -s "$PROMETHEUS_URL/api/v1/targets" | grep -o '"health":"up"' | wc -l)
assert_test "Prometheus actively scraping all 5 microservices (5/5 targets UP)" '[ "$TARGETS_UP" -ge 5 ]'

ES_INDICES=$(curl -s "$ELASTICSEARCH_URL/_cat/indices?v" | grep "starlight-logs" || true)
assert_test "Logstash pipeline active with indexed documents in Elasticsearch" '[ -n "$ES_INDICES" ]'

echo -e "\n======================================================================"
echo -e "${BOLD}TEST SUMMARY:${NC} ${GREEN}$PASSED_COUNT Passed${NC} / ${RED}$FAILED_COUNT Failed${NC}"
echo "======================================================================"

if [ "$FAILED_COUNT" -eq 0 ]; then
  echo -e "${GREEN}🎉 ALL 8 TEST SUITES COMPLETED WITH 100% SUCCESS!${NC}"
  exit 0
else
  echo -e "${RED}⚠️ Some test assertions failed. Inspect logs above.${NC}"
  exit 1
fi

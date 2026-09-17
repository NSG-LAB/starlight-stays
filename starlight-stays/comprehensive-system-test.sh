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
assert_test "Prometheus actively scraping microservices (targets UP)" '[ "$TARGETS_UP" -ge 5 ]'

ES_INDICES=$(curl -s "$ELASTICSEARCH_URL/_cat/indices?v" | grep "starlight-logs" || true)
assert_test "Logstash pipeline active with indexed documents in Elasticsearch" '[ -n "$ES_INDICES" ]'

echo -e "\n${CYAN}▶ SUITE 9: Notification Service & Digital Vouchers (AMQP Pub-Sub)${NC}"
NOTIF_RES=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY_URL/api/notifications")
assert_test "GET /api/notifications returns issued VIP vouchers" '[[ "$NOTIF_RES" == *"VCHR-STARLIGHT"* ]]'

echo -e "\n${CYAN}▶ SUITE 10: Guest Reviews & Star Ratings Microservice${NC}"
REVIEWS_RES=$(curl -s "$GATEWAY_URL/api/reviews")
assert_test "GET /api/reviews publicly accessible with seeded ratings" '[[ "$REVIEWS_RES" == *"rating"* ]]'

NEW_REV_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/api/reviews" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roomId": 1, "guestName": "Automated CI Test", "rating": 5, "comment": "Verification passed."}')
assert_test "POST /api/reviews creates new verified review (HTTP 201)" '[ "$NEW_REV_CODE" -eq 201 ]'

echo -e "\n${CYAN}▶ SUITE 11: Intelligent AI Concierge Recommendation Service${NC}"
STYLES_RES=$(curl -s "$GATEWAY_URL/api/concierge/styles")
assert_test "GET /api/concierge/styles returns curated travel styles" '[[ "$STYLES_RES" == *"CELESTIAL"* ]]'

REC_RES=$(curl -s -X POST "$GATEWAY_URL/api/concierge/recommend" \
  -H "Content-Type: application/json" \
  -d '{"travelStyle": "OCEANIC", "guestCount": 2, "budgetPreference": "PREMIUM"}')
assert_test "POST /api/concierge/recommend generates confidence score & itinerary" '[[ "$REC_RES" == *"confidenceScore"* ]] && [[ "$REC_RES" == *"curatedThreeDayItinerary"* ]]'

echo -e "\n${CYAN}▶ SUITE 12: Real-Time VIP Concierge Live Chat Microservice (8088)${NC}"
CHAT_SEND_RES=$(curl -s -X POST "$GATEWAY_URL/api/chat/send" \
  -H "Content-Type: application/json" \
  -d '{"channelId": "ci-test-channel", "sender": "CI Runner", "senderRole": "GUEST", "content": "Can you prepare vintage champagne on arrival?"}')
assert_test "POST /api/chat/send dispatches guest message to broker" '[[ "$CHAT_SEND_RES" == *"ci-test-channel"* ]]'

sleep 1
CHAT_HIST_RES=$(curl -s "$GATEWAY_URL/api/chat/history/ci-test-channel")
assert_test "GET /api/chat/history retrieves AI Butler auto-response" '[[ "$CHAT_HIST_RES" == *"Lord Alistair"* ]] || [[ "$CHAT_HIST_RES" == *"BUTLER_BOT"* ]]'

echo -e "\n${CYAN}▶ SUITE 13: Luxury Add-ons & VIP Experience Catalog (8089)${NC}"
ADDON_CATALOG=$(curl -s "$GATEWAY_URL/api/addons")
assert_test "GET /api/addons returns signature VIP experience catalog" '[[ "$ADDON_CATALOG" == *"HELI_TRANSFER"* ]] && [[ "$ADDON_CATALOG" == *"CHAMPAGNE_VIP"* ]]'

ATTACH_ADDON=$(curl -s -X POST "$GATEWAY_URL/api/addons/attach" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookingId": 1, "addonCode": "HELI_TRANSFER", "quantity": 1, "specialRequests": "Helipad VIP arrival"}')
assert_test "POST /api/addons/attach links luxury experience to booking" '[[ "$ATTACH_ADDON" == *"HELI_TRANSFER"* ]]'

echo -e "\n${CYAN}▶ SUITE 14: Automated Luxury PDF Voucher & Itinerary Generator${NC}"
PDF_CONTENT_TYPE=$(curl -s -I "$GATEWAY_URL/api/notifications/pdf/1" | grep -i "content-type" || true)
assert_test "GET /api/notifications/pdf/1 generates valid application/pdf binary" '[[ "$PDF_CONTENT_TYPE" == *"application/pdf"* ]]'

echo -e "\n${CYAN}▶ SUITE 15: OAuth2 / Social SSO & 2FA Security Enhancement${NC}"
SSO_TOKEN=$(curl -s -X POST "$GATEWAY_URL/api/users/oauth2/sso" \
  -H "Content-Type: application/json" \
  -d '{"provider": "Google", "email": "elena.rostova@starlightstays.luxury", "name": "Elena Rostova"}')
assert_test "POST /api/users/oauth2/sso generates signed federated JWT" '[[ "$SSO_TOKEN" == *"token"* ]] && [[ "$SSO_TOKEN" == *"AUTHENTICATED_SSO"* ]]'

TWOFA_RES=$(curl -s -X POST "$GATEWAY_URL/api/users/2fa/verify" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "code": "777888"}')
assert_test "POST /api/users/2fa/verify validates TOTP & grants elevation" '[[ "$TWOFA_RES" == *"VERIFIED"* ]]'

echo -e "\n======================================================================"
echo -e "${BOLD}TEST SUMMARY:${NC} ${GREEN}$PASSED_COUNT Passed${NC} / ${RED}$FAILED_COUNT Failed${NC}"
echo "======================================================================"

if [ "$FAILED_COUNT" -eq 0 ]; then
  echo -e "${GREEN}🎉 ALL 15 TEST SUITES COMPLETED WITH 100% SUCCESS!${NC}"
  exit 0
else
  echo -e "${RED}⚠️ Some test assertions failed. Inspect logs above.${NC}"
  exit 1
fi

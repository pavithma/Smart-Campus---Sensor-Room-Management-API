#!/bin/bash
BASE="http://localhost:8080/smartcampus/api/v1"
PASS=0
FAIL=0
CURL="curl -s --max-time 5"

check() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if echo "$actual" | grep -q "$expected"; then
    echo "  PASS: $label"
    ((PASS++))
  else
    echo "  FAIL: $label"
    echo "        expected to contain: $expected"
    echo "        got: $(echo "$actual" | head -3)"
    ((FAIL++))
  fi
}

sep() { echo; echo "=== $1 ==="; }

# ── Discovery ──────────────────────────────────────────────────────────────
sep "Discovery"
CODE=$($CURL -o /dev/null -w "%{http_code}" "$BASE/")
check "GET / → 200" "200" "$CODE"
BODY=$($CURL "$BASE/")
check "GET / contains apiName" "Smart Campus" "$BODY"
check "GET / contains rooms href" "/api/v1/rooms" "$BODY"

# ── Rooms — list ──────────────────────────────────────────────────────────
sep "Rooms — list"
BODY=$($CURL "$BASE/rooms")
CODE=$($CURL -o /dev/null -w "%{http_code}" "$BASE/rooms")
check "GET /rooms → 200" "200" "$CODE"
check "GET /rooms contains LIB-301" "LIB-301" "$BODY"
check "GET /rooms contains LAB-101" "LAB-101" "$BODY"

# ── Rooms — create ────────────────────────────────────────────────────────
sep "Rooms — create"
BODY=$($CURL -X POST "$BASE/rooms" -H "Content-Type: application/json" -d '{"name":"Cafeteria","capacity":120}')
CODE=$($CURL -o /dev/null -w "%{http_code}" -X POST "$BASE/rooms" -H "Content-Type: application/json" -d '{"name":"Cafeteria2","capacity":80}')
check "POST /rooms → 201" "201" "$CODE"
check "POST /rooms body has generated id" "ROOM-" "$BODY"
check "POST /rooms body has name" "Cafeteria" "$BODY"

# Create a room to delete later — extract id with grep+sed (no python3 pipe)
CREATE_RESP=$($CURL -X POST "$BASE/rooms" -H "Content-Type: application/json" -d '{"name":"TempRoom","capacity":5}')
CREATED_ROOM=$(echo "$CREATE_RESP" | grep -o '"id":"[^"]*"' | head -1 | sed 's/"id":"//;s/"//')
check "POST /rooms created room has id" "ROOM-" "$CREATED_ROOM"

# ── Rooms — duplicate id → 409 ────────────────────────────────────────────
sep "Rooms — duplicate id"
CODE=$($CURL -o /dev/null -w "%{http_code}" -X POST "$BASE/rooms" -H "Content-Type: application/json" -d '{"id":"LIB-301","name":"Dup","capacity":1}')
check "POST /rooms duplicate id → 409" "409" "$CODE"

# ── Rooms — get by id ─────────────────────────────────────────────────────
sep "Rooms — get by id"
BODY=$($CURL "$BASE/rooms/LIB-301")
CODE=$($CURL -o /dev/null -w "%{http_code}" "$BASE/rooms/LIB-301")
check "GET /rooms/LIB-301 → 200" "200" "$CODE"
check "GET /rooms/LIB-301 has capacity" "capacity" "$BODY"

CODE=$($CURL -o /dev/null -w "%{http_code}" "$BASE/rooms/GHOST-999")
check "GET /rooms/GHOST → 404" "404" "$CODE"
BODY=$($CURL "$BASE/rooms/GHOST-999")
check "GET /rooms/GHOST body has NOT_FOUND" "NOT_FOUND" "$BODY"

# ── Rooms — delete ────────────────────────────────────────────────────────
sep "Rooms — delete"
CODE=$($CURL -o /dev/null -w "%{http_code}" -X DELETE "$BASE/rooms/$CREATED_ROOM")
check "DELETE empty room → 204" "204" "$CODE"
CODE=$($CURL -o /dev/null -w "%{http_code}" -X DELETE "$BASE/rooms/$CREATED_ROOM")
check "DELETE already-gone room → 404" "404" "$CODE"
CODE=$($CURL -o /dev/null -w "%{http_code}" -X DELETE "$BASE/rooms/LIB-301")
check "DELETE non-empty room → 409" "409" "$CODE"
BODY=$($CURL -X DELETE "$BASE/rooms/LIB-301")
check "DELETE non-empty body has ROOM_NOT_EMPTY" "ROOM_NOT_EMPTY" "$BODY"

# ── Sensors — list & filter ───────────────────────────────────────────────
sep "Sensors — list and filter"
CODE=$($CURL -o /dev/null -w "%{http_code}" "$BASE/sensors")
check "GET /sensors → 200" "200" "$CODE"
BODY=$($CURL "$BASE/sensors")
check "GET /sensors contains TEMP-001" "TEMP-001" "$BODY"
check "GET /sensors contains CO2-007" "CO2-007" "$BODY"

BODY=$($CURL "$BASE/sensors?type=Temperature")
check "GET /sensors?type=Temperature returns TEMP-001" "TEMP-001" "$BODY"
check "GET /sensors?type=Temperature excludes CO2-007" "PASS" "$(echo "$BODY" | grep -q CO2-007 && echo FAIL || echo PASS)"

BODY=$($CURL "$BASE/sensors?type=CO2")
check "GET /sensors?type=CO2 returns CO2-007" "CO2-007" "$BODY"

# ── Sensors — get by id ───────────────────────────────────────────────────
sep "Sensors — get by id"
CODE=$($CURL -o /dev/null -w "%{http_code}" "$BASE/sensors/TEMP-001")
check "GET /sensors/TEMP-001 → 200" "200" "$CODE"
BODY=$($CURL "$BASE/sensors/TEMP-001")
check "GET /sensors/TEMP-001 has type Temperature" "Temperature" "$BODY"

CODE=$($CURL -o /dev/null -w "%{http_code}" "$BASE/sensors/NO-SUCH")
check "GET /sensors/NO-SUCH → 404" "404" "$CODE"
BODY=$($CURL "$BASE/sensors/NO-SUCH")
check "GET /sensors/NO-SUCH body has NOT_FOUND" "NOT_FOUND" "$BODY"

# ── Sensors — create ──────────────────────────────────────────────────────
sep "Sensors — create"
CODE=$($CURL -o /dev/null -w "%{http_code}" -X POST "$BASE/sensors" -H "Content-Type: application/json" -d '{"type":"Occupancy","roomId":"LIB-301"}')
check "POST /sensors valid → 201" "201" "$CODE"
BODY=$($CURL -X POST "$BASE/sensors" -H "Content-Type: application/json" -d '{"type":"Humidity","roomId":"LIB-301"}')
check "POST /sensors body has SENS- id" "SENS-" "$BODY"
check "POST /sensors default status ACTIVE" "ACTIVE" "$BODY"

NEW_SENSOR=$(echo "$BODY" | grep -o '"id":"[^"]*"' | head -1 | sed 's/"id":"//;s/"//')
ROOM_BODY=$($CURL "$BASE/rooms/LIB-301")
check "LIB-301 sensorIds updated after sensor create" "$NEW_SENSOR" "$ROOM_BODY"

CODE=$($CURL -o /dev/null -w "%{http_code}" -X POST "$BASE/sensors" -H "Content-Type: application/json" -d '{"type":"X","roomId":"NOPE"}')
check "POST /sensors bad roomId → 422" "422" "$CODE"
BODY=$($CURL -X POST "$BASE/sensors" -H "Content-Type: application/json" -d '{"type":"X","roomId":"NOPE"}')
check "POST /sensors bad roomId body has LINKED_RESOURCE_NOT_FOUND" "LINKED_RESOURCE_NOT_FOUND" "$BODY"

# ── Readings — post valid ─────────────────────────────────────────────────
sep "Readings — post valid"
CODE=$($CURL -o /dev/null -w "%{http_code}" -X POST "$BASE/sensors/TEMP-001/readings" -H "Content-Type: application/json" -d '{"value":24.7}')
check "POST /readings valid → 201" "201" "$CODE"
BODY=$($CURL -X POST "$BASE/sensors/TEMP-001/readings" -H "Content-Type: application/json" -d '{"value":24.7}')
check "POST /readings body has value 24.7" "24.7" "$BODY"
check "POST /readings body has server-generated id (UUID format)" "-" "$BODY"

# Verify currentValue side-effect
SENSOR_BODY=$($CURL "$BASE/sensors/TEMP-001")
check "TEMP-001 currentValue updated to 24.7" "24.7" "$SENSOR_BODY"

# ── Readings — get history ────────────────────────────────────────────────
sep "Readings — get history"
CODE=$($CURL -o /dev/null -w "%{http_code}" "$BASE/sensors/TEMP-001/readings")
check "GET /readings → 200" "200" "$CODE"
BODY=$($CURL "$BASE/sensors/TEMP-001/readings")
check "GET /readings contains value 24.7" "24.7" "$BODY"

# ── Readings — MAINTENANCE sensor → 403 ──────────────────────────────────
sep "Readings — MAINTENANCE sensor"
CODE=$($CURL -o /dev/null -w "%{http_code}" -X POST "$BASE/sensors/CO2-007/readings" -H "Content-Type: application/json" -d '{"value":400}')
check "POST /readings MAINTENANCE sensor → 403" "403" "$CODE"
BODY=$($CURL -X POST "$BASE/sensors/CO2-007/readings" -H "Content-Type: application/json" -d '{"value":400}')
check "POST /readings MAINTENANCE body has SENSOR_UNAVAILABLE" "SENSOR_UNAVAILABLE" "$BODY"

# ── Readings — unknown sensor → 404 ──────────────────────────────────────
sep "Readings — unknown sensor"
CODE=$($CURL -o /dev/null -w "%{http_code}" -X POST "$BASE/sensors/NO-SUCH/readings" -H "Content-Type: application/json" -d '{"value":1}')
check "POST /readings unknown sensor → 404" "404" "$CODE"

# ── Unmatched routes ──────────────────────────────────────────────────────
sep "Unmatched routes"
CODE=$($CURL -o /dev/null -w "%{http_code}" "$BASE/nonexistent")
check "GET /nonexistent → 404" "404" "$CODE"
BODY=$($CURL "$BASE/nonexistent")
check "GET /nonexistent body has NOT_FOUND" "NOT_FOUND" "$BODY"

# ── Summary ───────────────────────────────────────────────────────────────
echo
echo "================================"
echo "  PASSED: $PASS"
echo "  FAILED: $FAIL"
echo "================================"

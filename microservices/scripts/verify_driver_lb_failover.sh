#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"

register_and_get_token() {
  local role="$1"
  local name="$2"
  local phone
  phone="9$(python3 - <<'PY'
import random
print(''.join(str(random.randint(0, 9)) for _ in range(9)))
PY
)"

  local payload
  payload="$(printf '{"name":"%s","phone":"%s","password":"Pass@123","role":"%s"}' "$name" "$phone" "$role")"

  curl -fsS -X POST "$GATEWAY_URL/auth/register" \
    -H "Content-Type: application/json" \
    -d "$payload"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd docker
require_cmd curl
require_cmd python3
require_cmd uuidgen

mkdir -p secrets/local
[[ -f secrets/local/auth_db_password ]] || printf "logistics\n" > secrets/local/auth_db_password
[[ -f secrets/local/booking_db_password ]] || printf "logistics\n" > secrets/local/booking_db_password
[[ -f secrets/local/driver_db_password ]] || printf "logistics\n" > secrets/local/driver_db_password
[[ -f secrets/local/jwt_secret ]] || printf "replace-with-a-64-byte-jwt-secret\n" > secrets/local/jwt_secret

cleanup() {
  docker compose down >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "[1/6] Starting stack with 2 driver-service instances..."
docker compose up -d --build \
  postgres-driver postgres-booking redis discovery-service tracking-service driver-service booking-service api-gateway \
  --scale driver-service=2

echo "[2/6] Waiting for gateway and driver-service registration..."
for _ in {1..90}; do
  if curl -fsS "$GATEWAY_URL/actuator/health/readiness" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

for _ in {1..90}; do
  DRIVER_COUNT="$(curl -fsS "http://localhost:8761/eureka/apps/DRIVER-SERVICE" | python3 -c 'import sys,re; print(len(re.findall(r"<instance>", sys.stdin.read())))')"
  if [[ "$DRIVER_COUNT" -ge 2 ]]; then
    break
  fi
  sleep 2
done

if [[ "${DRIVER_COUNT:-0}" -lt 2 ]]; then
  echo "Driver instances did not register to Eureka as expected" >&2
  exit 1
fi

echo "[2.5/6] Creating probe user token..."
PROBE_AUTH_JSON="$(register_and_get_token DRIVER "LB Probe")"
PROBE_TOKEN="$(printf '%s' "$PROBE_AUTH_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')"

echo "[3/6] Probing load distribution via /driver/instance..."
DIST_FILE="$(mktemp)"
for _ in {1..30}; do
  curl -fsS "$GATEWAY_URL/driver/instance" \
    -H "Authorization: Bearer $PROBE_TOKEN" \
    -H "Connection: close" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("instance","unknown"))' >> "$DIST_FILE"
done

sort "$DIST_FILE" | uniq -c
UNIQUE_INSTANCES="$(sort "$DIST_FILE" | uniq | wc -l | tr -d ' ')"
if [[ "$UNIQUE_INSTANCES" -lt 2 ]]; then
  echo "Expected traffic across at least 2 driver instances, got $UNIQUE_INSTANCES" >&2
  exit 1
fi

echo "[4/6] Registering and enabling drivers through gateway..."
for idx in 1 2; do
  driver_auth_json="$(register_and_get_token DRIVER "Driver User $idx")"
  driver_token="$(printf '%s' "$driver_auth_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')"

  register_body="$(printf '{"name":"Driver %s","vehicleType":"BIKE","vehicleNumber":"KA01AB12%02d"}' "$idx" "$idx")"
  driver_json="$(curl -fsS -X POST "$GATEWAY_URL/driver/register" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $driver_token" \
    -d "$register_body")"
  driver_id="$(printf '%s' "$driver_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')"

  curl -fsS -X POST "$GATEWAY_URL/driver/$driver_id/online?online=true" \
    -H "Authorization: Bearer $driver_token" >/dev/null

  curl -fsS -X POST "$GATEWAY_URL/driver/$driver_id/location" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $driver_token" \
    -d '{"latitude":12.9716,"longitude":77.5946}' >/dev/null
done

echo "[5/6] Sending booking traffic through gateway to exercise booking->driver calls..."
BOOKING_AUTH_JSON="$(register_and_get_token USER "Booking User")"
BOOKING_TOKEN="$(printf '%s' "$BOOKING_AUTH_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')"
for i in {1..20}; do
  curl -fsS -X POST "$GATEWAY_URL/booking/create" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $BOOKING_TOKEN" \
    -H "Idempotency-Key: lb-traffic-$i" \
    -d '{"pickup":"Koramangala","dropAddress":"Indiranagar","vehicleType":"BIKE"}' >/dev/null
done

echo "[6/6] Killing one driver instance and validating failover availability..."
VICTIM_ID="$(docker compose ps -q driver-service | head -n 1)"
if [[ -z "$VICTIM_ID" ]]; then
  echo "Unable to find a running driver-service container to stop" >&2
  exit 1
fi

docker rm -f "$VICTIM_ID" >/dev/null
sleep 5

for _ in {1..15}; do
  curl -fsS "$GATEWAY_URL/driver/instance" \
    -H "Authorization: Bearer $PROBE_TOKEN" \
    -H "Connection: close" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("instance","unknown"))' >/dev/null
done

echo "Load balancing distribution and single-instance failover checks passed."
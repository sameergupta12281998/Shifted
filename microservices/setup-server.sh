#!/usr/bin/env bash
# =============================================================================
# setup-server.sh  —  One-time server provisioning + secret generation
# Run as root (or sudo) on the fresh VPS after cloning the repo.
# =============================================================================
set -euo pipefail

SECRETS_DIR="$(cd "$(dirname "$0")" && pwd)/secrets/local"

# ---------------------------------------------------------------------------
# 1.  Install Docker if not present
# ---------------------------------------------------------------------------
if ! command -v docker &>/dev/null; then
  echo "[1/4] Installing Docker..."
  apt-get update -qq
  apt-get install -y -qq ca-certificates curl gnupg lsb-release
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -qq
  apt-get install -y -qq docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin
  systemctl enable --now docker
  echo "[1/4] Docker installed."
else
  echo "[1/4] Docker already installed — skipping."
fi

# ---------------------------------------------------------------------------
# 2.  Generate secrets (skip if files already exist)
# ---------------------------------------------------------------------------
echo "[2/4] Generating secrets in $SECRETS_DIR ..."
mkdir -p "$SECRETS_DIR"

gen_secret() {
  local file="$SECRETS_DIR/$1"
  local length="${2:-32}"
  if [[ ! -f "$file" ]]; then
    LC_ALL=C tr -dc 'A-Za-z0-9!@#%^&*()-_=+' </dev/urandom \
      | head -c "$length" > "$file"
    echo "  Generated: $1"
  else
    echo "  Skipped  : $1 (already exists)"
  fi
}

gen_secret auth_db_password    32
gen_secret booking_db_password 32
gen_secret driver_db_password  32
gen_secret jwt_secret          64   # JWT secret must be ≥ 64 bytes for HS512

chmod 600 "$SECRETS_DIR"/*
echo "[2/4] Secrets ready."

# ---------------------------------------------------------------------------
# 3.  Open firewall (ufw)
# ---------------------------------------------------------------------------
echo "[3/4] Configuring firewall..."
if command -v ufw &>/dev/null; then
  ufw allow 22/tcp   comment "SSH"
  ufw allow 80/tcp   comment "HTTP (nginx redirect)"
  ufw allow 443/tcp  comment "HTTPS"
  ufw allow 8080/tcp comment "API Gateway"
  ufw --force enable
  echo "[3/4] ufw rules applied."
else
  echo "[3/4] ufw not found — configure firewall manually."
fi

# ---------------------------------------------------------------------------
# 4.  Build and start all services
# ---------------------------------------------------------------------------
echo "[4/4] Building and starting services (this takes a few minutes)..."
cd "$(dirname "$0")"
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d

echo ""
echo "============================================================"
echo "  Deployment complete!"
echo "  API Gateway is listening on http://$(curl -sf4 ifconfig.me 2>/dev/null || echo '<SERVER-IP>'):8080"
echo ""
echo "  Test it:"
echo "    curl http://<SERVER-IP>:8080/auth/register \\"
echo "      -H 'Content-Type: application/json' \\"
echo "      -d '{\"email\":\"test@test.com\",\"password\":\"pass123\",\"role\":\"CUSTOMER\"}'"
echo "============================================================"

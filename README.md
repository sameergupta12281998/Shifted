# Microservices Baseline

This folder contains the Porter-like logistics platform as a Spring Boot microservices stack.

## Services
- `discovery-service` handles service discovery.
- `api-gateway` handles edge routing, JWT validation, and trusted principal propagation.
- `auth-service` handles account registration, login, and JWT issuance.
- `booking-service` handles booking lifecycle, driver offer orchestration, and retry scheduling.
- `driver-service` handles driver profiles, availability, and accept or reject offer flows.
- `tracking-service` handles live location updates.
- `admin-service` exposes governance APIs.

## API Contract Updates
- Booking creation no longer accepts a client-supplied `userId` in the request body.
- The authenticated principal is derived from the gateway-injected headers after JWT validation.
- `POST /booking/create` now requires an `Idempotency-Key` header on every request.
- Reusing the same `Idempotency-Key` for the same authenticated user returns the previously created booking instead of creating a duplicate.

Example booking request through the gateway:

```http
POST /booking/create
Authorization: Bearer <user-token>
Idempotency-Key: booking-20260402-001
Content-Type: application/json

{
	"pickup": "Koramangala",
	"dropAddress": "Indiranagar",
	"vehicleType": "BIKE"
}
```

## Local Startup
Local Docker startup uses mounted secret files instead of plain environment variables for database passwords and the JWT secret.

1. Create the local secret files listed in `secrets/local/README.md`.
2. Run `docker compose up --build` from this directory.
3. Access the gateway at `http://localhost:8080`.

---

## Public Deployment (VPS — DigitalOcean / Hetzner / Linode)

### Recommended server spec
| Provider | Plan | RAM | vCPU | Cost |
|---|---|---|---|---|
| Hetzner | CX32 | 8 GB | 4 | ~€8/mo |
| DigitalOcean | Droplet 8 GB | 8 GB | 2 | ~$48/mo |
| Linode | Linode 4 GB | 4 GB | 2 | ~$24/mo |

> Minimum: **4 GB RAM** (10 Docker containers + 3 Postgres + Redis run in parallel).

### Steps

**1. Create a VPS** running Ubuntu 22.04 or 24.04.

**2. SSH in and clone the repo:**
```bash
ssh root@<SERVER-IP>
git clone https://github.com/<your-org>/<your-repo>.git
cd <your-repo>/microservices
```

**3. Run the setup script** (installs Docker, generates secrets, opens firewall, starts all services):
```bash
chmod +x setup-server.sh
sudo ./setup-server.sh
```

The script:
- Installs Docker + Docker Compose plugin
- Auto-generates cryptographically random passwords for all databases and a 64-byte JWT secret
- Opens ports 22, 80, 443 and 8080 via `ufw`
- Runs `docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d`

**4. Test the API from anywhere:**
```bash
curl http://<SERVER-IP>:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123","role":"CUSTOMER"}'
```

---

### Optional: Custom domain + HTTPS (Let's Encrypt)

**1. Point your domain's A record to the server IP.**

**2. Install nginx and Certbot:**
```bash
apt install -y nginx certbot python3-certbot-nginx
```

**3. Copy the nginx config and replace `YOUR_DOMAIN`:**
```bash
cp nginx/porterlike.conf /etc/nginx/sites-available/porterlike
sed -i 's/YOUR_DOMAIN/api.yourdomain.com/g' /etc/nginx/sites-available/porterlike
ln -s /etc/nginx/sites-available/porterlike /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

**4. Issue an SSL certificate:**
```bash
certbot --nginx -d api.yourdomain.com
```

After this, your API is reachable at `https://api.yourdomain.com/auth/register` with a valid TLS certificate. HTTP automatically redirects to HTTPS.

---

### What is exposed publicly?

| Service | Public? | Notes |
|---|---|---|
| api-gateway | ✅ Port 8080 (or 443 via nginx) | Only public entry point |
| auth-service | ❌ Internal only | Routed via gateway |
| booking-service | ❌ Internal only | Routed via gateway |
| driver-service | ❌ Internal only | Routed via gateway |
| tracking-service | ❌ Internal only | Routed via gateway |
| admin-service | ❌ Internal only | Routed via gateway |
| discovery-service (Eureka) | ❌ Internal only | Service registry |
| PostgreSQL (×3) | ❌ Internal only | DB ports unexposed in prod |
| Redis | ❌ Internal only | Cache port unexposed in prod |

Postgres containers read `POSTGRES_PASSWORD_FILE`, and Spring services import secrets from `/run/secrets/`.

## Deployment Secrets
For deployment environments, mount your external secret manager output into a directory such as `/mnt/secrets/porterlike/` and set `APP_SECRET_PATH=/mnt/secrets/porterlike/`.

The services read these secret file names from that directory:
- `auth_db_password`
- `booking_db_password`
- `driver_db_password`
- `jwt_secret`

This works with CSI-mounted secrets or any runtime that projects secret files onto the filesystem.

## API Docs
- OpenAPI JSON is exposed per service at `/v3/api-docs`.
- Swagger UI is exposed per service at `/swagger-ui/index.html`.
- Through the gateway, use service route prefixes such as:
	- `http://localhost:8080/auth/v3/api-docs`
	- `http://localhost:8080/booking/v3/api-docs`
	- `http://localhost:8080/driver/v3/api-docs`
	- `http://localhost:8080/tracking/v3/api-docs`

## Postman
- Import `postman/PorterLike-Logistics.postman_collection.json`.
- The collection includes auth, booking, driver handshake, and tracking flows through `gatewayUrl=http://localhost:8080`.
- The booking request in the collection automatically sends an `Idempotency-Key` header and no longer sends `userId` in the body.

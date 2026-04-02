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
Local Docker startup now uses mounted secret files instead of plain environment variables for database passwords and the JWT secret.

1. Create the local secret files listed in `secrets/local/README.md`.
2. Run `docker compose up --build` from this directory.
3. Access the gateway at `http://localhost:8080`.

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

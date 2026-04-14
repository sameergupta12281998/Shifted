# API Endpoints

This document lists the available HTTP endpoints for the microservices stack.

## Base URLs

- Gateway: `http://167.86.103.144:8080`
- Auth service: `http://167.86.103.144:8081`
- Booking service: `http://167.86.103.144:8082`
- Driver service: `http://167.86.103.144:8083`
- Admin service: `http://167.86.103.144:8084`
- Tracking service: `http://167.86.103.144:8085`
- Pricing service: `http://167.86.103.144:8086`
- Notification service: `http://167.86.103.144:8087`
- Discovery service: `http://167.86.103.144:8761`

## Flutter App Setup

Create two separate Flutter applications:

- `porter_user_app` for customers or end users
- `porter_driver_app` for drivers

Target both Android and iOS from each Flutter codebase.

### Recommended Project Structure

- Use separate Flutter projects if you want independent branding, release cycles, and app store publishing.
- Keep a shared internal API contract document so both apps use the same backend payloads and headers.
- Define different app IDs or bundle identifiers for each app.

Suggested identifiers:

- User app Android package: `com.porterlike.user`
- User app iOS bundle ID: `com.porterlike.user`
- Driver app Android package: `com.porterlike.driver`
- Driver app iOS bundle ID: `com.porterlike.driver`

### Create The Apps

```bash
flutter create porter_user_app
flutter create porter_driver_app
```

### Backend Base URL Configuration

Do not hardcode one host value for every platform.

- Android emulator: use `http://10.0.2.2:8080`
- iOS simulator: use `http://167.86.103.144:8080`
- Physical device: use `http://<YOUR-MAC-IP>:8080`

Suggested environment constants:

```dart
class ApiConfig {
	static const String androidEmulatorBaseUrl = 'http://10.0.2.2:8080';
	static const String iosSimulatorBaseUrl = 'http://167.86.103.144:8080';
	static const String localNetworkBaseUrl = 'http://192.168.x.x:8080';
}
```

### User App Features And Endpoint Usage

The user app should integrate these backend flows:

- Register user: `POST /auth/register` with `role=USER`
- Login user: `POST /auth/login`
- Create booking: `POST /booking/create`
- Get booking details: `GET /booking/{id}`
- Cancel booking: `POST /booking/cancel/{id}`
- Track assigned driver: `GET /tracking/driver/{driverId}`

User app request notes:

- Send `Authorization: Bearer <token>` after login
- Send `Idempotency-Key` for every booking creation request
- Do not send `userId` in booking create body

### Driver App Features And Endpoint Usage

The driver app should integrate these backend flows:

- Register auth account: `POST /auth/register` with `role=DRIVER`
- Login driver: `POST /auth/login`
- Register driver profile: `POST /driver/register`
- Set online status: `POST /driver/{id}/online?online=true`
- Approve driver verification (admin): `POST /driver/{id}/verify/approve`
- Reject driver verification (admin): `POST /driver/{id}/verify/reject`
- Update location: `POST /driver/{id}/location`
- Check nearby drivers or diagnostics if needed: `GET /driver/nearby`
- Fetch offer details: `GET /driver/offers/{offerId}`
- Accept offer: `POST /driver/{id}/offers/{offerId}/accept`
- Reject offer: `POST /driver/{id}/offers/{offerId}/reject`
- Complete trip: `POST /driver/{id}/complete/{bookingId}`

Driver app request notes:

- Send `Authorization: Bearer <driverToken>` after login
- Drivers must be `APPROVED` before they can switch to online mode
- Driver app should push location updates periodically while online
- Offer polling or realtime updates can be added on top of the offer endpoints listed here

### Flutter Integration Recommendations

- Use one API client layer per app with shared request models where possible
- Store JWT securely using `flutter_secure_storage`
- Use `dio` or `http` for API requests
- Use `json_serializable` or manual DTO parsing for request and response models
- Separate auth state, booking state, and driver availability state into independent providers or blocs

Recommended packages:

- `dio`
- `flutter_secure_storage`
- `provider`, `riverpod`, or `flutter_bloc`
- `geolocator`
- `google_maps_flutter` if live map tracking is needed

### Android Requirements

- Add internet permission in `android/app/src/main/AndroidManifest.xml`
- If testing on a real device over HTTP, allow cleartext traffic for local development if needed

Android manifest snippet:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

If local HTTP calls are blocked on Android, add to the `application` tag:

```xml
android:usesCleartextTraffic="true"
```

### iOS Requirements

- For local HTTP development, configure App Transport Security exceptions in `ios/Runner/Info.plist`
- For real device testing, ensure the iPhone and backend machine are on the same network

Example development-only ATS setting:

```xml
<key>NSAppTransportSecurity</key>
<dict>
	<key>NSAllowsArbitraryLoads</key>
	<true/>
</dict>
```

### Suggested Development Flow

1. Start backend services locally.
2. Register a user account from the user app.
3. Register a driver account from the driver app.
4. Create the driver profile from the driver app.
5. Set the driver online and begin location updates.
6. Create a booking from the user app.
7. Accept the booking from the driver app.
8. Track the trip from the user app using tracking endpoints.

## Gateway Route Prefixes

The API gateway forwards these paths:

- `/auth/**`
- `/booking/**`
- `/driver/**`
- `/admin/**`
- `/tracking/**`
- `/pricing/**`
- `/notifications/**`

## Auth Service

Gateway base: `http://167.86.103.144:8080/auth`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register a new account |
| POST | `/auth/login` | Login and get JWT token |
| GET | `/auth/me` | Get current authenticated user profile |

### Notes

- `POST /auth/register` supports roles such as `USER` and `DRIVER`.

## Booking Service

Gateway base: `http://167.86.103.144:8080/booking`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/booking/create` | Create a booking |
| GET | `/booking/my` | List current user's bookings |
| GET | `/booking/{id}` | Get booking by ID |
| POST | `/booking/cancel/{id}` | Cancel booking by ID |

### Notes

- Protected endpoints require `Authorization: Bearer <token>`.
- `POST /booking/create` requires `Idempotency-Key: <unique-value>`.
- Gateway injects authenticated principal headers after JWT validation.

## Driver Service

Gateway base: `http://167.86.103.144:8080/driver`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/driver/register` | Register driver profile |
| POST | `/driver/{id}/online?online=true` | Set driver online or offline |
| POST | `/driver/{id}/verify/approve` | Approve driver verification (admin only) |
| POST | `/driver/{id}/verify/reject` | Reject driver verification (admin only) |
| POST | `/driver/{id}/location` | Update driver location |
| GET | `/driver/nearby?vehicleType=BIKE&limit=5` | List nearby drivers |
| GET | `/driver/instance` | Get current service instance info |
| POST | `/driver/{id}/assign/{bookingId}` | Assign booking to driver |
| POST | `/driver/{id}/offer/{bookingId}?ttlSeconds=30` | Create booking offer for driver |
| GET | `/driver/offers/{offerId}` | Get offer status/details |
| POST | `/driver/{id}/offers/{offerId}/accept` | Accept offer |
| POST | `/driver/{id}/offers/{offerId}/reject` | Reject offer |
| POST | `/driver/{id}/complete/{bookingId}` | Mark trip as complete |

### Notes

- Most driver endpoints require `Authorization: Bearer <driverToken>`.
- Going online requires driver verification status `APPROVED`.
- `/driver/instance` is typically used for infra/debug visibility.

## Tracking Service

Gateway base: `http://167.86.103.144:8080/tracking`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/tracking/location` | Update tracking location |
| GET | `/tracking/driver/{driverId}` | Get latest location for a driver |

## Pricing Service

Gateway base: `http://167.86.103.144:8080/pricing`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/pricing/estimate` | Estimate fare using vehicle type, distance, and duration |

### Notes

- Supported vehicle types in current scaffold: `BIKE`, `MINI_TRUCK`.
- Pricing values are configurable using environment variables.

## Notification Service

Gateway base: `http://167.86.103.144:8080/notifications`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/notifications/send` | Send a notification using the configured provider |

### Notes

- Current provider is a stub queue response intended for integration wiring.
- This service is designed to be consumed by booking and payment lifecycle events.

## Admin Service

Gateway base: `http://167.86.103.144:8080/admin`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/ping` | Check current admin service instance |
| GET | `/admin/health/services` | Get downstream services health summary |

### Notes

- Admin endpoints require an authenticated admin token.

## Platform Endpoints

### API Gateway Actuator

| Method | Endpoint | Description |
|---|---|---|
| GET | `/actuator/health` | Health status |
| GET | `/actuator/info` | App info |
| GET | `/actuator/prometheus` | Prometheus metrics |

Gateway URLs:

- `http://167.86.103.144:8080/actuator/health`
- `http://167.86.103.144:8080/actuator/info`
- `http://167.86.103.144:8080/actuator/prometheus`

### Discovery Service

| Method | Endpoint | Description |
|---|---|---|
| GET | `/` | Eureka dashboard |
| GET | `/eureka` | Eureka registry endpoint |
| GET | `/actuator/health` | Health status |
| GET | `/actuator/info` | App info |
| GET | `/actuator/prometheus` | Prometheus metrics |

Direct URLs:

- `http://167.86.103.144:8761/`
- `http://167.86.103.144:8761/eureka`
- `http://167.86.103.144:8761/actuator/health`
- `http://167.86.103.144:8761/actuator/info`
- `http://167.86.103.144:8761/actuator/prometheus`

## OpenAPI And Swagger URLs

### Gateway URLs

- `http://167.86.103.144:8080/auth/v3/api-docs`
- `http://167.86.103.144:8080/auth/swagger-ui/index.html`
- `http://167.86.103.144:8080/booking/v3/api-docs`
- `http://167.86.103.144:8080/booking/swagger-ui/index.html`
- `http://167.86.103.144:8080/driver/v3/api-docs`
- `http://167.86.103.144:8080/driver/swagger-ui/index.html`
- `http://167.86.103.144:8080/tracking/v3/api-docs`
- `http://167.86.103.144:8080/tracking/swagger-ui/index.html`
- `http://167.86.103.144:8080/pricing/v3/api-docs`
- `http://167.86.103.144:8080/notifications/v3/api-docs`
- `http://167.86.103.144:8080/notifications/swagger-ui/index.html`
- `http://167.86.103.144:8080/pricing/swagger-ui/index.html`
- `http://167.86.103.144:8080/admin/v3/api-docs`
- `http://167.86.103.144:8080/admin/swagger-ui/index.html`

### Direct Service URLs

- `http://167.86.103.144:8081/v3/api-docs`
- `http://167.86.103.144:8081/swagger-ui/index.html`
- `http://167.86.103.144:8082/v3/api-docs`
- `http://167.86.103.144:8082/swagger-ui/index.html`
- `http://167.86.103.144:8083/v3/api-docs`
- `http://167.86.103.144:8083/swagger-ui/index.html`
- `http://167.86.103.144:8084/v3/api-docs`
- `http://167.86.103.144:8084/swagger-ui/index.html`
- `http://167.86.103.144:8085/v3/api-docs`
- `http://167.86.103.144:8085/swagger-ui/index.html`
- `http://167.86.103.144:8087/v3/api-docs`
- `http://167.86.103.144:8087/swagger-ui/index.html`
- `http://167.86.103.144:8086/v3/api-docs`
- `http://167.86.103.144:8086/swagger-ui/index.html`

## Request Payloads And Sample Responses

The examples below are based on the current DTO contracts in the codebase.

### Auth APIs

#### `POST /auth/register`

Request body:

```json
{
	"name": "Rahul User",
	"phone": "9876543210",
	"password": "password123",
	"role": "USER"
}
```

Sample response (`201`):

```json
{
	"token": "<jwt-token>",
	"expiresIn": 3600,
	"role": "USER",
	"userId": "4c4f84d3-4abf-4be8-9f27-0f8c7eaa6d62"
}
```

#### `POST /auth/login`

Request body:

```json
{
	"phone": "9876543210",
	"password": "password123"
}
```

Sample response (`200`):

```json
{
	"token": "<jwt-token>",
	"expiresIn": 3600,
	"role": "USER",
	"userId": "4c4f84d3-4abf-4be8-9f27-0f8c7eaa6d62"
}
```

#### `GET /auth/me`

Headers:

- `Authorization: Bearer <token>`

Request body: none

Sample response (`200`):

```json
{
	"userId": "4c4f84d3-4abf-4be8-9f27-0f8c7eaa6d62",
	"name": "Rahul User",
	"phone": "9876543210",
	"role": "USER"
}
```

### Booking APIs

Headers for protected booking calls:

- `Authorization: Bearer <userToken>`

#### `POST /booking/create`

Required extra header:

- `Idempotency-Key: booking-001`

Request body:

```json
{
	"pickup": "Koramangala",
	"dropAddress": "Indiranagar",
	"vehicleType": "BIKE"
}
```

Sample response (`201`):

```json
{
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"userId": "4c4f84d3-4abf-4be8-9f27-0f8c7eaa6d62",
	"driverId": null,
	"pickup": "Koramangala",
	"dropAddress": "Indiranagar",
	"vehicleType": "BIKE",
	"status": "SEARCHING"
}
```

#### `GET /booking/my`

Request body: none

Sample response (`200`):

```json
[
	{
		"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
		"userId": "4c4f84d3-4abf-4be8-9f27-0f8c7eaa6d62",
		"driverId": null,
		"pickup": "Koramangala",
		"dropAddress": "Indiranagar",
		"vehicleType": "BIKE",
		"status": "CREATED"
	}
]
```

#### `GET /booking/{id}`

Request body: none

Sample response (`200`):

```json
{
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"userId": "4c4f84d3-4abf-4be8-9f27-0f8c7eaa6d62",
	"driverId": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"pickup": "Koramangala",
	"dropAddress": "Indiranagar",
	"vehicleType": "BIKE",
	"status": "ACCEPTED"
}
```

#### `POST /booking/cancel/{id}`

Request body: none

Sample response (`200`):

```json
{
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"userId": "4c4f84d3-4abf-4be8-9f27-0f8c7eaa6d62",
	"driverId": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"pickup": "Koramangala",
	"dropAddress": "Indiranagar",
	"vehicleType": "BIKE",
	"status": "CANCELLED"
}
```

### Driver APIs

Headers for protected driver calls:

- `Authorization: Bearer <driverToken>`

#### `POST /driver/register`

Request body:

```json
{
	"name": "Raj Driver",
	"vehicleType": "BIKE",
	"vehicleNumber": "KA01AB1234"
}
```

Sample response (`201`):

```json
{
	"id": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"name": "Raj Driver",
	"vehicleType": "BIKE",
	"vehicleNumber": "KA01AB1234",
	"online": false,
	"available": true,
	"currentBookingId": null,
	"latitude": null,
	"longitude": null
}
```

#### `POST /driver/{id}/online?online=true`

Request body: none

Sample response (`200`):

```json
{
	"id": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"name": "Raj Driver",
	"vehicleType": "BIKE",
	"vehicleNumber": "KA01AB1234",
	"online": true,
	"available": true,
	"currentBookingId": null,
	"latitude": 12.9716,
	"longitude": 77.5946
}
```

#### `POST /driver/{id}/location`

Request body:

```json
{
	"latitude": 12.9716,
	"longitude": 77.5946
}
```

Sample response (`200`):

```json
{
	"id": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"name": "Raj Driver",
	"vehicleType": "BIKE",
	"vehicleNumber": "KA01AB1234",
	"online": true,
	"available": true,
	"currentBookingId": null,
	"latitude": 12.9716,
	"longitude": 77.5946
}
```

#### `GET /driver/nearby?vehicleType=BIKE&limit=5`

Request body: none

Sample response (`200`):

```json
[
	{
		"id": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
		"name": "Raj Driver",
		"vehicleType": "BIKE",
		"vehicleNumber": "KA01AB1234",
		"online": true,
		"available": true,
		"currentBookingId": null,
		"latitude": 12.9716,
		"longitude": 77.5946
	}
]
```

#### `POST /driver/{id}/offer/{bookingId}?ttlSeconds=30`

Request body: none

Sample response (`200`):

```json
{
	"offerId": "offer-9f4a6bce3e",
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"driverId": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"status": "PENDING",
	"expiresAt": "2026-04-12T11:32:45Z"
}
```

#### `GET /driver/offers/{offerId}`

Request body: none

Sample response (`200`):

```json
{
	"offerId": "offer-9f4a6bce3e",
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"driverId": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"status": "PENDING",
	"expiresAt": "2026-04-12T11:32:45Z"
}
```

#### `POST /driver/{id}/offers/{offerId}/accept`

Request body: none

Sample response (`200`):

```json
{
	"offerId": "offer-9f4a6bce3e",
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"driverId": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"status": "ACCEPTED",
	"expiresAt": "2026-04-12T11:32:45Z"
}
```

#### `POST /driver/{id}/offers/{offerId}/reject`

Request body: none

Sample response (`200`):

```json
{
	"offerId": "offer-9f4a6bce3e",
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"driverId": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"status": "REJECTED",
	"expiresAt": "2026-04-12T11:32:45Z"
}
```

#### `POST /driver/{id}/complete/{bookingId}`

Request body: none

Sample response (`200`):

```json
true
```

### Tracking APIs

Headers for protected tracking calls:

- `POST /tracking/location` is an internal service endpoint. Direct calls must include `X-Internal-Service-Token`.
- `GET /tracking/driver/{driverId}` requires authenticated access. Through the gateway this comes from `Authorization: Bearer <token>` and is propagated as `X-Authenticated-*` headers.

#### `POST /tracking/location`

Notes:

- Internal-only endpoint used by `driver-service`.
- Returns `403` when `X-Internal-Service-Token` is missing or invalid.

Request body:

```json
{
	"driverId": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"latitude": 12.9716,
	"longitude": 77.5946
}
```

Sample response (`202`):

```json
{
	"driverId": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"latitude": 12.9716,
	"longitude": 77.5946,
	"updatedAtEpochMs": 1712917965000
}
```

#### `GET /tracking/driver/{driverId}`

Notes:

- Requires authenticated caller context.
- Returns `403` when authenticated identity headers are missing.

Request body: none

Sample response (`200`):

```json
{
	"driverId": "a8d6f3e1-c846-4ed7-9b7f-0e5a980676f8",
	"bookingId": "d8df6f0f-b651-4675-860b-64a4bc3a4a4e",
	"latitude": 12.9716,
	"longitude": 77.5946,
	"updatedAtEpochMs": 1712917965000
}
```

### Pricing APIs

#### `POST /pricing/estimate`

Request body:

```json
{
	"vehicleType": "BIKE",
	"distanceKm": 10,
	"durationMinutes": 15
}
```

Sample response (`200`):

```json
{
	"vehicleType": "BIKE",
	"distanceKm": 10.0,
	"durationMinutes": 15.0,
	"baseFare": 40.0,
	"distanceFare": 120.0,
	"timeFare": 15.0,
	"totalFare": 175.0,
	"currency": "INR"
}
```

### Notification APIs

#### `POST /notifications/send`

Request body:

```json
{
	"recipientId": "user-123",
	"channel": "SMS",
	"title": "Booking Update",
	"message": "Driver assigned"
}
```

Sample response (`202`):

```json
{
	"notificationId": "b2c0f8aa-31ea-42f5-b06b-376457f01ca4",
	"status": "QUEUED",
	"provider": "stub"
}
```

### Admin APIs

Headers for protected admin calls:

- `Authorization: Bearer <adminToken>`

#### `GET /admin/ping`

Request body: none

Sample response (`200`):

```json
{
	"instance": "admin-service-7b7f4f4dcb-j4g5p",
	"status": "UP"
}
```

#### `GET /admin/health/services`

Request body: none

Sample response (`200`):

```json
[
	{
		"service": "auth-service",
		"status": "UP",
		"details": {
			"status": "UP"
		}
	},
	{
		"service": "booking-service",
		"status": "UP",
		"details": {
			"status": "UP"
		}
	}
]
```

### Actuator APIs (Gateway)

#### `GET /actuator/health`

Request body: none

Sample response (`200`):

```json
{
	"status": "UP"
}
```

#### `GET /actuator/info`

Request body: none

Sample response (`200`):

```json
{}
```

#### `GET /actuator/prometheus`

Request body: none

Sample response (`200`, text/plain):

```text
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 1.23456E7
...
```
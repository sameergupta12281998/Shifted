# API Endpoints

Complete reference for all HTTP endpoints, WebSocket services, and request/response payloads.

---

## Base URLs

| Service | Direct URL | Gateway Path |
|---|---|---|
| API Gateway | `http://localhost:8080` | — |
| Discovery Service | `http://localhost:8761` | — |
| Auth Service | `http://localhost:8081` | `/auth/**` |
| Booking Service | `http://localhost:8082` | `/booking/**` |
| Driver Service | `http://localhost:8083` | `/driver/**` |
| Admin Service | `http://localhost:8084` | `/admin/**` |
| Tracking Service | `http://localhost:8085` | `/tracking/**` |
| Pricing Service | `http://localhost:8086` | `/pricing/**` |
| Notification Service | `http://localhost:8087` | `/notifications/**` |
| Payment Service | `http://localhost:8088` | `/payments/**` |
| Matching Service | `http://localhost:8089` | `/matching/**` |
| Rating Service | `http://localhost:8090` | `/ratings/**` |
| Analytics Service | `http://localhost:8091` | `/analytics/**` |
| Fraud Detection Service | `http://localhost:8092` | `/fraud/**` |

All examples below use the **gateway** URL (`http://localhost:8080`). Replace with your host/IP for remote servers.

---

## Authentication

All protected endpoints require the header:

```
Authorization: Bearer <token>
```

The gateway validates the JWT and injects these headers to downstream services:

- `X-Authenticated-User-Id` — UUID of the authenticated user
- `X-Authenticated-Role` — role string (`USER`, `DRIVER`, `ADMIN`)

---

## 1. Auth Service

### POST `/auth/register`

Register a new user or driver account.

**Request:**

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sameer Gupta",
    "phone": "9876543210",
    "password": "secret123",
    "role": "USER"
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `name` | String | @NotBlank, max 120 | Full name |
| `phone` | String | @NotBlank, regex `^[0-9]{10,15}$` | Phone number |
| `password` | String | @NotBlank, min 6, max 64 | Password |
| `role` | String | @NotBlank | `USER` or `DRIVER` |

**Response** `201 Created`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400000,
  "role": "USER",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

### POST `/auth/login`

Login and receive a JWT token.

**Request:**

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "password": "secret123"
  }'
```

**Request Body:**

| Field | Type | Validation |
|---|---|---|
| `phone` | String | @NotBlank |
| `password` | String | @NotBlank |

**Response** `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400000,
  "role": "USER",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

### GET `/auth/me`

Get current authenticated user profile.

**Request:**

```bash
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer <token>"
```

**Response** `200 OK`:

```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "Sameer Gupta",
  "phone": "9876543210",
  "role": "USER"
}
```

---

## 2. Booking Service

### POST `/booking/create`

Create a new booking. Requires `Idempotency-Key` header.

**Request:**

```bash
curl -X POST http://localhost:8080/booking/create \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-request-id-123" \
  -d '{
    "pickup": "Koramangala, Bangalore",
    "dropAddress": "Indiranagar, Bangalore",
    "vehicleType": "BIKE"
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `pickup` | String | @NotBlank | Pickup address |
| `dropAddress` | String | @NotBlank | Drop-off address |
| `vehicleType` | String | @NotBlank | `BIKE` or `MINI_TRUCK` |

**Required Headers:**

| Header | Description |
|---|---|
| `Authorization` | Bearer JWT token |
| `Idempotency-Key` | Unique key to prevent duplicate bookings |

**Response** `201 Created`:

```json
{
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "driverId": null,
  "pickup": "Koramangala, Bangalore",
  "dropAddress": "Indiranagar, Bangalore",
  "vehicleType": "BIKE",
  "status": "CREATED"
}
```

**Kafka Events Published:** `booking.created`

---

### GET `/booking/{id}`

Get booking details by ID.

**Request:**

```bash
curl http://localhost:8080/booking/b1c2d3e4-f5a6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer <token>"
```

**Response** `200 OK`:

```json
{
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "pickup": "Koramangala, Bangalore",
  "dropAddress": "Indiranagar, Bangalore",
  "vehicleType": "BIKE",
  "status": "ASSIGNED"
}
```

---

### GET `/booking/my`

List all bookings for the current authenticated user.

**Request:**

```bash
curl http://localhost:8080/booking/my \
  -H "Authorization: Bearer <token>"
```

**Response** `200 OK`:

```json
[
  {
    "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "driverId": null,
    "pickup": "Koramangala, Bangalore",
    "dropAddress": "Indiranagar, Bangalore",
    "vehicleType": "BIKE",
    "status": "CREATED"
  }
]
```

---

### POST `/booking/cancel/{id}`

Cancel a booking.

**Request:**

```bash
curl -X POST http://localhost:8080/booking/cancel/b1c2d3e4-f5a6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer <token>"
```

**Response** `200 OK`:

```json
{
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "driverId": null,
  "pickup": "Koramangala, Bangalore",
  "dropAddress": "Indiranagar, Bangalore",
  "vehicleType": "BIKE",
  "status": "CANCELLED"
}
```

**Kafka Events Published:** `booking.cancelled`

---

## 3. Driver Service

### POST `/driver/register`

Register a driver profile. Requires auth account with `role=DRIVER`.

**Request:**

```bash
curl -X POST http://localhost:8080/driver/register \
  -H "Authorization: Bearer <driverToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ravi Kumar",
    "vehicleType": "BIKE",
    "vehicleNumber": "KA01AB1234"
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `name` | String | @NotBlank | Driver name |
| `vehicleType` | String | @NotBlank | `BIKE` or `MINI_TRUCK` |
| `vehicleNumber` | String | @NotBlank | Vehicle registration number |

**Response** `201 Created`:

```json
{
  "id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "name": "Ravi Kumar",
  "vehicleType": "BIKE",
  "vehicleNumber": "KA01AB1234",
  "verificationStatus": "UNVERIFIED",
  "online": false,
  "available": false,
  "currentBookingId": null,
  "latitude": null,
  "longitude": null
}
```

---

### POST `/driver/{id}/verify/approve`

Approve driver verification. **Admin only.**

**Request:**

```bash
curl -X POST http://localhost:8080/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890/verify/approve \
  -H "Authorization: Bearer <adminToken>"
```

**Response** `200 OK`:

```json
{
  "id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "name": "Ravi Kumar",
  "vehicleType": "BIKE",
  "vehicleNumber": "KA01AB1234",
  "verificationStatus": "APPROVED",
  "online": false,
  "available": false,
  "currentBookingId": null,
  "latitude": null,
  "longitude": null
}
```

---

### POST `/driver/{id}/verify/reject`

Reject driver verification. **Admin only.**

**Request:**

```bash
curl -X POST http://localhost:8080/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890/verify/reject \
  -H "Authorization: Bearer <adminToken>"
```

**Response** `200 OK`: Same shape as approve, with `"verificationStatus": "REJECTED"`.

---

### POST `/driver/{id}/online?online={true|false}`

Set driver online or offline. Driver must be `APPROVED` to go online.

**Request:**

```bash
curl -X POST "http://localhost:8080/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890/online?online=true" \
  -H "Authorization: Bearer <driverToken>"
```

**Query Parameters:**

| Param | Type | Description |
|---|---|---|
| `online` | boolean | `true` to go online, `false` to go offline |

**Response** `200 OK`:

```json
{
  "id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "name": "Ravi Kumar",
  "vehicleType": "BIKE",
  "vehicleNumber": "KA01AB1234",
  "verificationStatus": "APPROVED",
  "online": true,
  "available": true,
  "currentBookingId": null,
  "latitude": null,
  "longitude": null
}
```

---

### POST `/driver/{id}/location`

Update driver GPS coordinates.

**Request:**

```bash
curl -X POST http://localhost:8080/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890/location \
  -H "Authorization: Bearer <driverToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 12.9716,
    "longitude": 77.5946
  }'
```

**Request Body:**

| Field | Type | Validation |
|---|---|---|
| `latitude` | Double | @NotNull |
| `longitude` | Double | @NotNull |

**Response** `200 OK`: Returns full `DriverResponse`.

---

### GET `/driver/nearby?vehicleType={type}&limit={n}`

List nearby available drivers.

**Request:**

```bash
curl "http://localhost:8080/driver/nearby?vehicleType=BIKE&limit=5"
```

**Query Parameters:**

| Param | Type | Default | Description |
|---|---|---|---|
| `vehicleType` | String | — | `BIKE` or `MINI_TRUCK` |
| `limit` | int | 10 | Max results |

**Response** `200 OK`:

```json
[
  {
    "id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
    "name": "Ravi Kumar",
    "vehicleType": "BIKE",
    "vehicleNumber": "KA01AB1234",
    "verificationStatus": "APPROVED",
    "online": true,
    "available": true,
    "currentBookingId": null,
    "latitude": 12.9716,
    "longitude": 77.5946
  }
]
```

---

### GET `/driver/instance`

Get current service instance info (debug/infra).

**Request:**

```bash
curl http://localhost:8080/driver/instance
```

**Response** `200 OK`:

```json
{
  "instanceId": "driver-service:8083",
  "port": "8083"
}
```

---

### POST `/driver/{id}/assign/{bookingId}`

Direct-assign a booking to a driver (internal).

**Request:**

```bash
curl -X POST http://localhost:8080/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890/assign/b1c2d3e4-f5a6-7890-abcd-ef1234567890
```

**Response** `200 OK`:

```json
true
```

---

### POST `/driver/{id}/offer/{bookingId}?ttlSeconds={n}`

Create a booking offer for a driver with TTL.

**Request:**

```bash
curl -X POST "http://localhost:8080/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890/offer/b1c2d3e4-f5a6-7890-abcd-ef1234567890?ttlSeconds=30"
```

**Query Parameters:**

| Param | Type | Default | Description |
|---|---|---|---|
| `ttlSeconds` | int | 30 | Seconds before offer expires |

**Response** `200 OK`:

```json
{
  "offerId": "offer-abc-123",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "status": "PENDING",
  "expiresAt": "2026-04-20T15:30:00Z"
}
```

---

### GET `/driver/offers/{offerId}`

Get offer status and details.

**Request:**

```bash
curl http://localhost:8080/driver/offers/offer-abc-123
```

**Response** `200 OK`:

```json
{
  "offerId": "offer-abc-123",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "status": "PENDING",
  "expiresAt": "2026-04-20T15:30:00Z"
}
```

---

### POST `/driver/{id}/offers/{offerId}/accept`

Accept a booking offer.

**Request:**

```bash
curl -X POST http://localhost:8080/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890/offers/offer-abc-123/accept \
  -H "Authorization: Bearer <driverToken>"
```

**Response** `200 OK`:

```json
{
  "offerId": "offer-abc-123",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "status": "ACCEPTED",
  "expiresAt": "2026-04-20T15:30:00Z"
}
```

**Kafka Events Published:** `booking.assigned` (via booking-service callback)

---

### POST `/driver/{id}/offers/{offerId}/reject`

Reject a booking offer.

**Request:**

```bash
curl -X POST http://localhost:8080/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890/offers/offer-abc-123/reject \
  -H "Authorization: Bearer <driverToken>"
```

**Response** `200 OK`: Same shape, with `"status": "REJECTED"`.

---

### POST `/driver/{id}/complete/{bookingId}`

Mark a trip as complete.

**Request:**

```bash
curl -X POST http://localhost:8080/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890/complete/b1c2d3e4-f5a6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer <driverToken>"
```

**Response** `200 OK`:

```json
true
```

---

## 4. Tracking Service

### POST `/tracking/location`

Update driver location and publish via WebSocket.

**Request:**

```bash
curl -X POST http://localhost:8080/tracking/location \
  -H "Content-Type: application/json" \
  -d '{
    "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
    "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
    "latitude": 12.9716,
    "longitude": 77.5946
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `driverId` | String | @NotBlank | Driver UUID |
| `bookingId` | String | optional | Active booking UUID |
| `latitude` | Double | @NotNull | GPS latitude |
| `longitude` | Double | @NotNull | GPS longitude |

**Response** `202 Accepted`:

```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "updatedAtEpochMs": 1713600000000
}
```

**Side Effects:** Publishes `TrackingSnapshot` to WebSocket topic `/topic/tracking/{driverId}`.

---

### GET `/tracking/driver/{driverId}`

Get latest cached location for a driver.

**Request:**

```bash
curl http://localhost:8080/tracking/driver/d1e2f3a4-b5c6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer <token>"
```

**Response** `200 OK`:

```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "updatedAtEpochMs": 1713600000000
}
```

---

### WebSocket — Real-Time Tracking

The tracking service publishes live location updates via STOMP over WebSocket.

**Connection URL (direct to tracking-service):**

```
ws://localhost:8085/ws
```

**STOMP Configuration:**

| Setting | Value |
|---|---|
| Endpoint | `/ws` |
| Broker prefix | `/topic` |
| App destination prefix | `/app` |
| CORS | All origins allowed (`*`) |

**Subscribe to driver location updates:**

```
SUBSCRIBE /topic/tracking/{driverId}
```

**Published payload (on each POST /tracking/location):**

```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "updatedAtEpochMs": 1713600000000
}
```

**Example — JavaScript (SockJS + STOMP):**

```javascript
const socket = new SockJS('http://localhost:8085/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
  stompClient.subscribe(
    '/topic/tracking/d1e2f3a4-b5c6-7890-abcd-ef1234567890',
    function (message) {
      const snapshot = JSON.parse(message.body);
      console.log('Location:', snapshot.latitude, snapshot.longitude);
    }
  );
});
```

**Example — Dart (Flutter with stomp_dart_client):**

```dart
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'dart:convert';

final stompClient = StompClient(
  config: StompConfig(
    url: 'ws://localhost:8085/ws',
    onConnect: (StompFrame frame) {
      stompClient.subscribe(
        destination: '/topic/tracking/$driverId',
        callback: (StompFrame frame) {
          final snapshot = jsonDecode(frame.body!);
          print('Location: ${snapshot['latitude']}, ${snapshot['longitude']}');
        },
      );
    },
  ),
);
stompClient.activate();
```

**Example — Postman WebSocket:**

1. Open Postman > New > WebSocket Request
2. Enter URL: `ws://localhost:8085/ws`
3. Connect, then send STOMP CONNECT frame:
   ```
   CONNECT
   accept-version:1.2
   host:localhost

   ^@
   ```
4. Subscribe:
   ```
   SUBSCRIBE
   id:sub-0
   destination:/topic/tracking/d1e2f3a4-b5c6-7890-abcd-ef1234567890

   ^@
   ```
5. Messages arrive as `TrackingSnapshot` JSON whenever `POST /tracking/location` is called.

---

## 5. Pricing Service

### POST `/pricing/estimate`

Estimate fare based on vehicle type, distance, and duration.

**Request:**

```bash
curl -X POST http://localhost:8080/pricing/estimate \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleType": "BIKE",
    "distanceKm": 8.5,
    "durationMinutes": 25.0
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `vehicleType` | String | @NotBlank | `BIKE` or `MINI_TRUCK` |
| `distanceKm` | double | @DecimalMin("0.1") | Trip distance in km |
| `durationMinutes` | double | @DecimalMin("0.0") | Trip duration in minutes |

**Response** `200 OK`:

```json
{
  "vehicleType": "BIKE",
  "distanceKm": 8.5,
  "durationMinutes": 25.0,
  "baseFare": 30.0,
  "distanceFare": 85.0,
  "timeFare": 25.0,
  "totalFare": 140.0,
  "currency": "INR"
}
```

---

## 6. Payment Service

### POST `/payments/create-order`

Create a payment order for a booking.

**Request:**

```bash
curl -X POST http://localhost:8080/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
    "amount": 140.00,
    "method": "UPI"
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `bookingId` | String | @NotBlank | Booking UUID |
| `amount` | BigDecimal | @NotNull, @DecimalMin("0.01") | Amount in INR |
| `method` | String | @NotBlank | `UPI`, `CARD`, `CASH`, `WALLET` |

**Response** `201 Created`:

```json
{
  "orderId": "order-xyz-789",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "amount": 140.00,
  "currency": "INR",
  "method": "UPI",
  "status": "CREATED",
  "provider": "STUB"
}
```

---

### POST `/payments/confirm`

Confirm payment after provider callback.

**Request:**

```bash
curl -X POST http://localhost:8080/payments/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-xyz-789",
    "providerRef": "razorpay_ref_abc"
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `orderId` | String | @NotBlank | Payment order ID |
| `providerRef` | String | optional | Provider transaction reference |

**Response** `200 OK`:

```json
{
  "paymentId": "pay-123-456",
  "orderId": "order-xyz-789",
  "status": "PAID"
}
```

---

### POST `/payments/refund`

Request a refund.

**Request:**

```bash
curl -X POST http://localhost:8080/payments/refund \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "pay-123-456",
    "amount": 140.00,
    "reason": "Driver did not arrive"
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `paymentId` | String | @NotBlank | Payment ID |
| `amount` | BigDecimal | @NotNull, @DecimalMin("0.01") | Refund amount |
| `reason` | String | optional | Reason for refund |

**Response** `202 Accepted`:

```json
{
  "refundId": "refund-aaa-111",
  "paymentId": "pay-123-456",
  "amount": 140.00,
  "status": "QUEUED"
}
```

---

### GET `/payments/bookings/{bookingId}`

Get all payment orders for a booking.

**Request:**

```bash
curl http://localhost:8080/payments/bookings/b1c2d3e4-f5a6-7890-abcd-ef1234567890
```

**Response** `200 OK`:

```json
[
  {
    "orderId": "order-xyz-789",
    "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
    "amount": 140.00,
    "currency": "INR",
    "method": "UPI",
    "status": "PAID",
    "provider": "STUB"
  }
]
```

---

### POST `/payments/webhooks/provider`

Webhook endpoint for payment provider callbacks.

**Request:**

```bash
curl -X POST http://localhost:8080/payments/webhooks/provider \
  -H "Content-Type: application/json" \
  -d '{
    "event": "payment.captured",
    "order_id": "order-xyz-789",
    "payment_id": "pay_abc123"
  }'
```

**Request Body:** Free-form `Map<String, Object>` — depends on provider.

**Response** `200 OK`: Empty body.

---

## 7. Notification Service

### POST `/notifications/send`

Send a notification (SMS, push, email).

**Request:**

```bash
curl -X POST http://localhost:8080/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "channel": "SMS",
    "title": "Booking Confirmed",
    "message": "Your booking has been confirmed. Driver is on the way."
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `recipientId` | String | @NotBlank | User/driver UUID |
| `channel` | String | @NotBlank | `SMS`, `PUSH`, or `EMAIL` |
| `title` | String | @NotBlank | Notification title |
| `message` | String | @NotBlank | Notification body |

**Response** `202 Accepted`:

```json
{
  "notificationId": "notif-abc-123",
  "status": "QUEUED",
  "provider": "STUB"
}
```

**Kafka Consumers:** This service automatically sends notifications when Kafka events arrive:

| Kafka Topic | Notification |
|---|---|
| `booking.created` | SMS to user: "Booking created, finding a driver" |
| `booking.assigned` | PUSH to driver: "New booking, head to pickup point" |
| `booking.completed` | SMS to user: "Booking complete, please rate your driver" |
| `booking.cancelled` | SMS to user: "Booking cancelled" |

---

## 8. Admin Service

### GET `/admin/ping`

Check admin service instance. **Admin only.**

**Request:**

```bash
curl http://localhost:8080/admin/ping \
  -H "Authorization: Bearer <adminToken>"
```

**Response** `200 OK`:

```json
{
  "status": "ok",
  "service": "admin-service",
  "timestamp": "2026-04-20T12:00:00Z"
}
```

---

### GET `/admin/health/services`

Get health status of all downstream services. **Admin only.**

**Request:**

```bash
curl http://localhost:8080/admin/health/services \
  -H "Authorization: Bearer <adminToken>"
```

**Response** `200 OK`:

```json
[
  {
    "service": "auth-service",
    "status": "UP",
    "details": {}
  },
  {
    "service": "booking-service",
    "status": "UP",
    "details": {}
  }
]
```

---

## 9. Matching Service

### POST `/matching/drivers/register`

Register or update a driver's geolocation in the matching pool.

**Request:**

```bash
curl -X POST http://localhost:8080/matching/drivers/register \
  -H "Content-Type: application/json" \
  -d '{
    "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
    "vehicleType": "BIKE",
    "latitude": 12.9716,
    "longitude": 77.5946
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `driverId` | String | @NotBlank | Driver UUID |
| `vehicleType` | String | @NotBlank | `BIKE` or `MINI_TRUCK` |
| `latitude` | double | required | GPS latitude |
| `longitude` | double | required | GPS longitude |

**Response** `204 No Content`: Empty body.

**Kafka Consumers:** This endpoint is also triggered automatically by `driver.location.updated` Kafka events.

---

### DELETE `/matching/drivers/{driverId}/unavailable?vehicleType={type}`

Mark a driver as unavailable in the matching pool.

**Request:**

```bash
curl -X DELETE "http://localhost:8080/matching/drivers/d1e2f3a4-b5c6-7890-abcd-ef1234567890/unavailable?vehicleType=BIKE"
```

**Query Parameters:**

| Param | Type | Description |
|---|---|---|
| `vehicleType` | String | `BIKE` or `MINI_TRUCK` |

**Response** `204 No Content`: Empty body.

**Kafka Consumers:** Also triggered automatically by `booking.assigned` Kafka events.

---

### POST `/matching/find`

Find nearest available drivers using Redis GEO (haversine scoring).

**Request:**

```bash
curl -X POST http://localhost:8080/matching/find \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 12.9716,
    "longitude": 77.5946,
    "vehicleType": "BIKE",
    "radiusKm": 5.0,
    "limit": 10
  }'
```

**Request Body:**

| Field | Type | Validation | Default | Description |
|---|---|---|---|---|
| `latitude` | Double | @NotNull | — | Pickup latitude |
| `longitude` | Double | @NotNull | — | Pickup longitude |
| `vehicleType` | String | @NotBlank | — | `BIKE` or `MINI_TRUCK` |
| `radiusKm` | Double | optional | 5.0 | Search radius in km |
| `limit` | Integer | optional | 10 | Max candidates to return |

**Response** `200 OK`:

```json
{
  "drivers": [
    {
      "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
      "vehicleType": "BIKE",
      "latitude": 12.9720,
      "longitude": 77.5950,
      "distanceKm": 0.45,
      "score": 95.5
    }
  ],
  "total": 1
}
```

---

## 10. Rating Service

### POST `/ratings`

Submit a rating for a completed booking. Idempotent — duplicate submits return existing rating.

**Request:**

```bash
curl -X POST http://localhost:8080/ratings \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
    "fromUserId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "toUserId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
    "roleTarget": "DRIVER",
    "score": 5,
    "comment": "Very fast delivery!"
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `bookingId` | UUID | @NotNull | Booking UUID |
| `fromUserId` | UUID | @NotNull | Rater user UUID |
| `toUserId` | UUID | @NotNull | Rated user UUID |
| `roleTarget` | String | @NotBlank | `DRIVER` or `USER` |
| `score` | int | @Min(1), @Max(5) | Rating score 1-5 |
| `comment` | String | optional | Free text comment |

**Response** `201 Created`:

```json
{
  "id": "r1a2b3c4-d5e6-7890-abcd-ef1234567890",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "fromUserId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "toUserId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "roleTarget": "DRIVER",
  "score": 5,
  "comment": "Very fast delivery!"
}
```

---

### GET `/ratings/drivers/{driverId}`

Get rating stats for a driver.

**Request:**

```bash
curl http://localhost:8080/ratings/drivers/d1e2f3a4-b5c6-7890-abcd-ef1234567890
```

**Response** `200 OK`:

```json
{
  "subjectId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "roleTarget": "DRIVER",
  "averageScore": 4.7,
  "totalRatings": 42
}
```

---

### GET `/ratings/users/{userId}`

Get rating stats for a user (rider).

**Request:**

```bash
curl http://localhost:8080/ratings/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

**Response** `200 OK`:

```json
{
  "subjectId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "roleTarget": "USER",
  "averageScore": 4.9,
  "totalRatings": 15
}
```

---

## 11. Analytics Service

### GET `/analytics/summary`

Get platform analytics summary (last 24 hours window).

**Request:**

```bash
curl http://localhost:8080/analytics/summary
```

**Response** `200 OK`:

```json
{
  "totalBookings": 1250,
  "bookingsLast24h": 87,
  "completedBookings": 1100,
  "cancelledBookings": 45,
  "revenueTotal": 187500.00,
  "revenueLast24h": 12350.00,
  "activeDriversLast24h": 34,
  "totalPaymentsCompleted": 1080
}
```

**Kafka Consumers:** Analytics service consumes all domain events and stores them:

| Kafka Topic | Data Captured |
|---|---|
| `booking.created` | Booking count, vehicle type |
| `booking.assigned` | Assignment tracking, driver ID |
| `booking.completed` | Completion count, vehicle type |
| `booking.cancelled` | Cancellation count |
| `payment.completed` | Revenue amount, payment count |
| `driver.location.updated` | Active driver tracking |
| `driver.registered` | New driver count |
| `user.registered` | New user count |

---

## 12. Fraud Detection Service

### POST `/fraud/check/booking`

Run fraud checks on a booking.

**Request:**

```bash
curl -X POST http://localhost:8080/fraud/check/booking \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "pickupLatitude": 12.9716,
    "pickupLongitude": 77.5946,
    "dropLatitude": 12.9716,
    "dropLongitude": 77.5946,
    "estimatedFare": 140.00,
    "vehicleType": "BIKE"
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `bookingId` | String | @NotBlank | Booking UUID |
| `userId` | String | @NotBlank | User UUID |
| `pickupLatitude` | Double | @NotNull | Pickup GPS latitude |
| `pickupLongitude` | Double | @NotNull | Pickup GPS longitude |
| `dropLatitude` | Double | @NotNull | Drop GPS latitude |
| `dropLongitude` | Double | @NotNull | Drop GPS longitude |
| `estimatedFare` | BigDecimal | optional | Estimated fare |
| `vehicleType` | String | optional | `BIKE` or `MINI_TRUCK` |

**Response** `200 OK`:

```json
{
  "subjectId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "riskLevel": "HIGH",
  "flagged": true,
  "reason": "same-location: pickup and drop within 50m"
}
```

**Booking Fraud Rules:**

| Rule | Trigger |
|---|---|
| Same-location | Pickup and drop within 50 meters |
| Rate abuse | More than 10 bookings in last hour |
| GPS out of bounds | Coordinates outside India (6-38°N, 68-98°E) |

**Kafka Consumer:** Auto-triggers on every `booking.created` event.

---

### POST `/fraud/check/payment`

Run fraud checks on a payment.

**Request:**

```bash
curl -X POST http://localhost:8080/fraud/check/payment \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "pay-123-456",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "amount": 75000.00,
    "method": "UPI",
    "ipAddress": "103.21.58.1"
  }'
```

**Request Body:**

| Field | Type | Validation | Description |
|---|---|---|---|
| `paymentId` | String | @NotBlank | Payment ID |
| `userId` | String | @NotBlank | User UUID |
| `amount` | BigDecimal | @NotNull | Payment amount |
| `method` | String | optional | `UPI`, `CARD`, etc. |
| `ipAddress` | String | optional | Client IP address |

**Response** `200 OK`:

```json
{
  "subjectId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "riskLevel": "CRITICAL",
  "flagged": true,
  "reason": "large-amount: payment exceeds 50000 threshold"
}
```

**Payment Fraud Rules:**

| Rule | Trigger |
|---|---|
| Large amount | Payment exceeds ₹50,000 |
| Repeat offender | User previously flagged for fraud |

---

### GET `/fraud/reports/pending`

Get all unreviewed fraud reports.

**Request:**

```bash
curl http://localhost:8080/fraud/reports/pending
```

**Response** `200 OK`:

```json
[
  {
    "id": "fr-001-abc",
    "subjectId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "riskLevel": "HIGH",
    "reason": "same-location: pickup and drop within 50m",
    "reviewed": false,
    "createdAt": "2026-04-20T12:00:00Z"
  }
]
```

---

### POST `/fraud/reports/{reportId}/review`

Mark a fraud report as reviewed.

**Request:**

```bash
curl -X POST http://localhost:8080/fraud/reports/fr-001-abc/review
```

**Response** `200 OK`:

```json
{
  "id": "fr-001-abc",
  "subjectId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "riskLevel": "HIGH",
  "reason": "same-location: pickup and drop within 50m",
  "reviewed": true,
  "createdAt": "2026-04-20T12:00:00Z"
}
```

---

## Kafka Event Topics

Domain events flowing through Kafka:

| Topic | Producer | Consumers |
|---|---|---|
| `booking.created` | booking-service | notification, analytics, fraud-detection, matching |
| `booking.assigned` | booking-service | notification, analytics, matching |
| `booking.completed` | booking-service | notification, analytics |
| `booking.cancelled` | booking-service | notification, analytics |
| `payment.completed` | payment-service | analytics |
| `driver.location.updated` | driver-service | matching, analytics |
| `driver.registered` | driver-service | analytics |
| `user.registered` | auth-service | analytics |

---

## Platform Endpoints

### Actuator (all services)

Every service exposes:

| Method | Path | Description |
|---|---|---|
| GET | `/actuator/health` | Health status |
| GET | `/actuator/health/readiness` | Readiness probe |
| GET | `/actuator/health/liveness` | Liveness probe |
| GET | `/actuator/info` | App info |
| GET | `/actuator/prometheus` | Prometheus metrics |

### Discovery Service (Eureka)

| Method | Path | Description |
|---|---|---|
| GET | `http://localhost:8761/` | Eureka dashboard |
| GET | `http://localhost:8761/eureka/apps` | All registered services |

---

## OpenAPI / Swagger UI

Each service exposes interactive API docs:

| Service | Swagger UI | API Docs JSON |
|---|---|---|
| Auth | `http://localhost:8081/swagger-ui/index.html` | `http://localhost:8081/v3/api-docs` |
| Booking | `http://localhost:8082/swagger-ui/index.html` | `http://localhost:8082/v3/api-docs` |
| Driver | `http://localhost:8083/swagger-ui/index.html` | `http://localhost:8083/v3/api-docs` |
| Admin | `http://localhost:8084/swagger-ui/index.html` | `http://localhost:8084/v3/api-docs` |
| Tracking | `http://localhost:8085/swagger-ui/index.html` | `http://localhost:8085/v3/api-docs` |
| Pricing | `http://localhost:8086/swagger-ui/index.html` | `http://localhost:8086/v3/api-docs` |
| Notification | `http://localhost:8087/swagger-ui/index.html` | `http://localhost:8087/v3/api-docs` |
| Payment | `http://localhost:8088/swagger-ui/index.html` | `http://localhost:8088/v3/api-docs` |
| Matching | `http://localhost:8089/swagger-ui/index.html` | `http://localhost:8089/v3/api-docs` |
| Rating | `http://localhost:8090/swagger-ui/index.html` | `http://localhost:8090/v3/api-docs` |
| Analytics | `http://localhost:8091/swagger-ui/index.html` | `http://localhost:8091/v3/api-docs` |
| Fraud Detection | `http://localhost:8092/swagger-ui/index.html` | `http://localhost:8092/v3/api-docs` |

Via gateway:

- `http://localhost:8080/auth/swagger-ui/index.html`
- `http://localhost:8080/booking/swagger-ui/index.html`
- `http://localhost:8080/driver/swagger-ui/index.html`
- `http://localhost:8080/admin/swagger-ui/index.html`
- `http://localhost:8080/tracking/swagger-ui/index.html`
- `http://localhost:8080/pricing/swagger-ui/index.html`
- `http://localhost:8080/notifications/swagger-ui/index.html`
- `http://localhost:8080/payments/swagger-ui/index.html`
- `http://localhost:8080/matching/swagger-ui/index.html`
- `http://localhost:8080/ratings/swagger-ui/index.html`
- `http://localhost:8080/analytics/swagger-ui/index.html`
- `http://localhost:8080/fraud/swagger-ui/index.html`

---

## Postman Collection

Import the collection from:

```
microservices/postman/PorterLike-Logistics.postman_collection.json
```

---

## Error Response Format

All services return errors in this format:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: pickup must not be blank",
  "timestamp": "2026-04-20T12:00:00Z"
}
```

Common status codes:

| Code | Meaning |
|---|---|
| 400 | Validation error or bad request |
| 401 | Missing or invalid JWT |
| 403 | Insufficient role/permissions |
| 404 | Resource not found |
| 409 | Conflict (duplicate idempotency key, already rated) |
| 429 | Rate limit exceeded (120 req/min general, 20 req/min auth) |
| 500 | Internal server error |
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
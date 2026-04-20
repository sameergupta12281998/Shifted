# Driver App Plan and Requirements

This document defines the build plan and API requirements for the driver mobile app.
Source of truth for backend APIs: `microservices/API_ENDPOINTS.md`.

## 1. Scope

Build a driver app to:
- register/login as DRIVER
- create and manage driver profile
- go online or offline
- push location updates
- receive and act on booking offers
- complete trips
- rate users

## 2. Base Configuration

- Gateway base URL: `http://localhost:8080`
- Auth header for protected APIs: `Authorization: Bearer <driverToken>`
- Driver must be `APPROVED` to go online

## 3. Delivery Plan

## Phase 1 - Driver Auth and Profile
- Register driver auth account and login.
- Create driver profile.
- Read profile status and verification state.

## Phase 2 - Availability and Location
- Toggle online or offline state.
- Post periodic location updates while online.
- Recover offline state on app restart.

## Phase 3 - Offer Handling
- Fetch offer details.
- Accept or reject offer.
- Show offer TTL countdown in UI.

## Phase 4 - Active Trip and Completion
- Sync booking state.
- Complete assigned booking.
- Submit user rating after completion.

## 4. API Requirements (Driver App)

## 4.1 Authentication

### POST `/auth/register`
Register auth account with role DRIVER.

Request body:
```json
{
  "name": "Ravi Kumar",
  "phone": "9876543210",
  "password": "secret123",
  "role": "DRIVER"
}
```

Response `201`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400000,
  "role": "DRIVER",
  "userId": "d-auth-user-id"
}
```

### POST `/auth/login`
Request body:
```json
{
  "phone": "9876543210",
  "password": "secret123"
}
```

Response `200`: token payload as above.

### GET `/auth/me`
Headers: `Authorization`.

Response `200`: authenticated profile object.

## 4.2 Driver Profile and Verification

### POST `/driver/register`
Headers: `Authorization`, `Content-Type: application/json`.

Request body:
```json
{
  "name": "Ravi Kumar",
  "vehicleType": "BIKE",
  "vehicleNumber": "KA01AB1234"
}
```

Response `201`:
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

### POST `/driver/{id}/verify/approve`
Admin-only API, but driver app depends on this state becoming APPROVED.

Response `200`: `verificationStatus` becomes `APPROVED`.

### POST `/driver/{id}/verify/reject`
Admin-only API.

Response `200`: `verificationStatus` becomes `REJECTED`.

## 4.3 Availability and Location

### POST `/driver/{id}/online?online={true|false}`
Headers: `Authorization`.

Query params:
- `online=true` to go online
- `online=false` to go offline

Response `200`: full driver object with updated `online` and `available` fields.

### POST `/driver/{id}/location`
Headers: `Authorization`, `Content-Type: application/json`.

Request body:
```json
{
  "latitude": 12.9716,
  "longitude": 77.5946
}
```

Response `200`: full driver object.

### POST `/tracking/location` (optional if using tracking-service directly)
Request body:
```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "latitude": 12.9716,
  "longitude": 77.5946
}
```

Response `202`:
```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "updatedAtEpochMs": 1713600000000
}
```

## 4.4 Offer Lifecycle

### POST `/driver/{id}/offer/{bookingId}?ttlSeconds={n}`
Usually internal orchestration endpoint; driver app consumes resulting offer.

Response `200`:
```json
{
  "offerId": "offer-abc-123",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "status": "PENDING",
  "expiresAt": "2026-04-20T15:30:00Z"
}
```

### GET `/driver/offers/{offerId}`
Get offer details.

Response `200`: offer object.

### POST `/driver/{id}/offers/{offerId}/accept`
Headers: `Authorization`.

Response `200`: offer object with `status: "ACCEPTED"`.

### POST `/driver/{id}/offers/{offerId}/reject`
Headers: `Authorization`.

Response `200`: offer object with `status: "REJECTED"`.

## 4.5 Trip Completion

### POST `/driver/{id}/complete/{bookingId}`
Headers: `Authorization`.

Response `200`:
```json
true
```

## 4.6 Ratings (Driver to User)

### POST `/ratings`
Driver rates user after trip.

Request body:
```json
{
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "fromUserId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "toUserId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "roleTarget": "USER",
  "score": 5,
  "comment": "Polite customer"
}
```

Response `201`: created rating object.

### GET `/ratings/users/{userId}`
Read user rating summary.

Response `200`:
```json
{
  "subjectId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "roleTarget": "USER",
  "averageScore": 4.9,
  "totalRatings": 15
}
```

## 5. Non-Functional Requirements

- Location update cadence: every 3 to 8 seconds while online/active trip.
- Battery-aware background location strategy.
- Strong error handling for offer accept/reject race conditions.
- Keep local state consistent after app crash or reconnect.
- Follow backend error format:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2026-04-20T12:00:00Z"
}
```

## 6. Acceptance Checklist

- Driver can register/login and create profile.
- Driver can go online only after approval.
- Driver can send live location updates.
- Driver can view offer details and accept/reject in time.
- Driver can complete trip and rate user.

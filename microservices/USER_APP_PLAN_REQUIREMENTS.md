# User App Plan and Requirements

This document defines the build plan and API requirements for the customer (USER) mobile app.
Source of truth for backend APIs: `microservices/API_ENDPOINTS.md`.

## 1. Scope

Build a mobile app for end users to:
- register/login
- create bookings
- view and cancel bookings
- track assigned driver
- estimate fare
- pay for trips
- rate drivers

## 2. Base Configuration

- Gateway base URL: `http://localhost:8080`
- Auth header for protected APIs: `Authorization: Bearer <token>`
- Booking create must include: `Idempotency-Key: <unique-value>`

## 3. Delivery Plan

## Phase 1 - Auth and Session
- Implement register, login, me profile.
- Persist JWT securely.
- Add token refresh fallback by re-login when token expires.

## Phase 2 - Booking Flow
- Booking creation with idempotency key.
- Booking details and booking history (`/booking/my`).
- Booking cancellation.

## Phase 3 - Live Trip
- Poll driver location by driverId using tracking API.
- Show trip state (CREATED, ASSIGNED, COMPLETED, CANCELLED).

## Phase 4 - Pricing, Payments, Ratings
- Fare estimate before booking confirmation.
- Create payment order and confirm payment.
- Submit driver rating after completion.

## 4. API Requirements (User App)

## 4.1 Authentication

### POST `/auth/register`
Purpose: create user account.

Request body:
```json
{
  "name": "Sameer Gupta",
  "phone": "9876543210",
  "password": "secret123",
  "role": "USER"
}
```

Response `201`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400000,
  "role": "USER",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
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

Response `200`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400000,
  "role": "USER",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### GET `/auth/me`
Headers: `Authorization`.

Response `200`:
```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "Sameer Gupta",
  "phone": "9876543210",
  "role": "USER"
}
```

## 4.2 Booking

### POST `/booking/create`
Headers: `Authorization`, `Idempotency-Key`, `Content-Type: application/json`.

Request body:
```json
{
  "pickup": "Koramangala, Bangalore",
  "dropAddress": "Indiranagar, Bangalore",
  "vehicleType": "BIKE"
}
```

Response `201`:
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

### GET `/booking/{id}`
Headers: `Authorization`.

Response `200`: booking object.

### GET `/booking/my`
Headers: `Authorization`.

Response `200`: array of booking objects.

### POST `/booking/cancel/{id}`
Headers: `Authorization`.

Response `200`: booking object with `status: "CANCELLED"`.

## 4.3 Tracking

### GET `/tracking/driver/{driverId}`
Headers: `Authorization`.

Response `200`:
```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "updatedAtEpochMs": 1713600000000
}
```

## 4.4 Pricing

### POST `/pricing/estimate`
Request body:
```json
{
  "vehicleType": "BIKE",
  "distanceKm": 8.5,
  "durationMinutes": 25.0
}
```

Response `200`:
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

## 4.5 Payments

### POST `/payments/create-order`
Request body:
```json
{
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "amount": 140.0,
  "method": "UPI"
}
```

Response `201`:
```json
{
  "orderId": "order-xyz-789",
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "amount": 140.0,
  "currency": "INR",
  "method": "UPI",
  "status": "CREATED",
  "provider": "STUB"
}
```

### POST `/payments/confirm`
Request body:
```json
{
  "orderId": "order-xyz-789",
  "providerRef": "razorpay_ref_abc"
}
```

Response `200`:
```json
{
  "paymentId": "pay-123-456",
  "orderId": "order-xyz-789",
  "status": "PAID"
}
```

### GET `/payments/bookings/{bookingId}`
Response `200`: array of payment order objects.

### POST `/payments/refund`
Request body:
```json
{
  "paymentId": "pay-123-456",
  "amount": 140.0,
  "reason": "Driver did not arrive"
}
```

Response `202`:
```json
{
  "refundId": "refund-aaa-111",
  "paymentId": "pay-123-456",
  "amount": 140.0,
  "status": "QUEUED"
}
```

## 4.6 Ratings

### POST `/ratings`
User rates driver after completed booking.

Request body:
```json
{
  "bookingId": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "fromUserId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "toUserId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "roleTarget": "DRIVER",
  "score": 5,
  "comment": "Very fast delivery!"
}
```

Response `201`: created rating object.

### GET `/ratings/drivers/{driverId}`
Read driver rating summary.

Response `200`:
```json
{
  "subjectId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "roleTarget": "DRIVER",
  "averageScore": 4.7,
  "totalRatings": 42
}
```

## 5. Non-Functional Requirements

- Secure token storage.
- Retry with exponential backoff for transient network failures.
- Idempotent booking create to prevent duplicate bookings.
- Real-time friendly polling for tracking every 2 to 5 seconds while trip is active.
- Graceful error handling using backend error format:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2026-04-20T12:00:00Z"
}
```

## 6. Acceptance Checklist

- User can register and login.
- User can create booking with idempotency key.
- User can view, cancel, and track booking.
- User can estimate fare and complete payment flow.
- User can submit rating for driver.

# Shifted Production Readiness Review

This document reviews the existing Shifted backend against a production-grade Porter-like logistics platform.

## Scope

System reviewed:

- `auth-service`
- `booking-service`
- `driver-service`
- `tracking-service`
- `admin-service`
- `api-gateway`
- `discovery-service`

Primary API reference used:

- `API_ENDPOINTS.md`

---

## Existing API Surface

### Auth

- `POST /auth/register`
- `POST /auth/login`

### Booking

- `POST /booking/create`
- `GET /booking/{id}`
- `POST /booking/cancel/{id}`

### Driver

- `POST /driver/register`
- `POST /driver/{id}/online?online=true`
- `POST /driver/{id}/location`
- `GET /driver/nearby?vehicleType=BIKE&limit=5`
- `GET /driver/instance`
- `POST /driver/{id}/assign/{bookingId}`
- `POST /driver/{id}/offer/{bookingId}?ttlSeconds=30`
- `GET /driver/offers/{offerId}`
- `POST /driver/{id}/offers/{offerId}/accept`
- `POST /driver/{id}/offers/{offerId}/reject`
- `POST /driver/{id}/complete/{bookingId}`

### Tracking

- `POST /tracking/location`
- `GET /tracking/driver/{driverId}`

### Admin

- `GET /admin/ping`
- `GET /admin/health/services`

### Platform

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`
- Eureka discovery endpoints

---

## Feature Checklist Review

### ✅ Implemented Features

#### 1. Authentication (JWT)

- JWT validation exists at gateway level.
- Login and registration are implemented.
- Role propagation to downstream services exists using injected headers.
- Roles observed: `USER`, `DRIVER`, `ADMIN`.

Evidence:

- `platform/api-gateway/src/main/java/com/porterlike/platform/gateway/security/JwtAuthenticationFilter.java`
- `services/auth-service/src/main/java/com/porterlike/services/auth/api/AuthController.java`

#### 2. User Features

- Basic user registration exists.
- Basic login exists.
- Users can create bookings.
- Users can fetch booking details by booking ID.
- Users can cancel bookings.

#### 3. Driver Features

- Driver auth account creation exists using `role=DRIVER`.
- Driver profile registration exists.
- Driver online or offline availability toggle exists.
- Driver location update exists.
- Driver offer accept or reject exists.
- Driver trip completion exists.

#### 4. Booking Lifecycle

- Booking creation exists.
- Booking cancellation exists.
- Booking completion exists through driver completion flow.
- Idempotent booking creation is implemented using `Idempotency-Key`.

#### 5. Real-Time Tracking

- Tracking update endpoint exists.
- Driver latest location read endpoint exists.
- Redis-backed location cache exists.
- WebSocket topic-based publish model exists on tracking service.
- Tracking write path is now restricted to internal service calls.
- Tracking read path now requires authenticated user context.

#### 6. Basic Admin Panel APIs

- Admin ping endpoint exists.
- Service health aggregation endpoint exists.

---

### ⚠️ Partial Implementation

#### 1. User Features

What exists:

- Registration
- Login
- Booking creation
- Booking status retrieval by booking ID

What is missing:

- User profile update
- Saved addresses
- Booking history list
- Favorite locations
- Support and complaint flows
- Ratings and feedback

#### 2. Driver Features

What exists:

- Driver profile registration
- Online and offline management
- Location updates
- Offer lifecycle

What is missing:

- Driver earnings
- Daily job summary
- Driver profile update
- Driver ratings
- Driver availability schedules
- Trip history list

#### 3. Auto Driver Matching

What exists:

- Candidate filtering by vehicle type
- Availability-based filtering
- Retry scheduler with backoff
- Offer creation flow

What is missing:

- Nearest-driver ranking
- ETA-based selection
- Demand-supply balancing
- Surge-aware driver allocation
- Driver score or rating inputs
- Queue-based dispatch orchestration

Current observation:

- Matching is operational, but not production-grade.
- Current selection is not truly geospatial.

#### 4. Admin Panel

What exists:

- Health and ping endpoints

What is missing:

- User management
- Driver approval workflows
- Booking operations dashboard
- Revenue dashboards
- Refund tools
- Support operations
- Fraud review
- Audit trail review

#### 5. Security And Rate Limiting

What exists:

- JWT auth at gateway
- Role checks in services
- Basic validation annotations
- Idempotency key for booking create

What is missing:

- ~~Rate limiting~~ ✅ Implemented (RateLimitingFilter: 120 req/min general, 20 req/min for /auth/**)
- Login throttling
- Refresh token and revocation flow
- Audit log trail
- Fine-grained permission model
- Abuse prevention

#### 6. Error Handling And Fallback

What exists:

- Global exception handlers across services
- HTTP status mapping for common failures

What is missing:

- Circuit breakers
- Retry policies with jitter
- Dead-letter handling
- Graceful fallback strategies
- Trace correlation across services

---

### ❌ Missing Features

#### 1. Driver Verification System ✅ Phase 1 Implemented

- ~~No KYC workflow~~ ✅ UNVERIFIED → PENDING → APPROVED/REJECTED state machine
- ~~No license verification~~ ✅ Admin approve/reject endpoints added
- ~~No identity verification~~ ✅ Gated: drivers cannot go online without APPROVED status
- ~~No approval or rejection pipeline~~ ✅ `POST /driver/{id}/verify/approve|reject` (admin-only), 23/23 tests pass

#### 2. Vehicle Verification System

- No RC verification
- No insurance verification
- No fitness inspection records
- No expiration reminders

#### 3. Pricing Engine ✅ Phase 1 Implemented

- ~~No fare estimate API~~ ✅ `POST /pricing/estimate` via pricing-service (port 8086)
- ~~No distance or duration pricing~~ ✅ Configurable base + per-km + per-minute rates (BIKE & MINI_TRUCK)
- No surge multiplier logic
- No cancellation fee logic
- No pricing rules by city or category

#### 4. Payment System ✅ Phase 2 Implemented

- ~~No online payment integration~~ ✅ `POST /payments/create-order`, `POST /payments/confirm` (stub provider, pluggable)
- ~~No cash payment tracking~~ ✅ Supported via `method=CASH`
- ~~No invoice generation~~ Pending (Phase 3)
- ~~No refund workflow~~ ✅ `POST /payments/refund` → QUEUED status
- ~~No payment status tracking~~ ✅ `GET /payments/bookings/{bookingId}` returns full order history

#### 5. Notifications ✅ Phase 1 Implemented

- ~~No SMS notifications~~ ✅ `POST /notifications/send` via notification-service (port 8087, stub provider)
- ~~No push notifications~~ Stub supports channel field (SMS/PUSH/EMAIL); real provider pluggable
- ~~No email notifications~~ Stub supports channel field (SMS/PUSH/EMAIL); real provider pluggable
- No booking status communication pipeline (event-driven trigger not yet wired)

#### 6. Ratings And Reviews

- No rider-to-driver rating
- No driver-to-rider rating
- No quality score in matching

#### 7. Support And Dispute Resolution

- No ticketing system
- No escalation APIs
- No refund review workflow

#### 8. Production Observability

- No distributed tracing
- No business event audit service
- No end-to-end request correlation

#### 9. Event-Driven Architecture

- No Kafka or RabbitMQ integration
- No domain events for booking, payment, notifications, or audit

---

## Module-by-Module Comparison Against Porter-Like Systems

| Module | Shifted Status | Production Expectation | Gap |
|---|---|---|---|
| Authentication (JWT) | Implemented | JWT + refresh + revocation + abuse controls | Medium |
| User Features | Basic | Full profile, history, saved places, support | High |
| Driver Features | Core | Earnings, ratings, compliance, trip summaries | High |
| Driver Verification | ✅ Phase 1 Done | State machine, admin approve/reject, setOnline gate | Critical |
| Vehicle Verification | Missing | RC, insurance, inspection, expiry checks | Critical |
| Booking Lifecycle | Core | Full lifecycle incl. fees, support, retries | Medium |
| Auto Driver Matching | Partial | Geospatial, ETA-aware, quality-aware | Critical |
| Real-Time Tracking | Partial+ | WebSocket + internal write gate + authenticated reads; history + TTL still pending | Medium |
| Pricing Engine | ✅ Phase 1 Done | Config-driven fare engine (BIKE/MINI_TRUCK), /pricing/estimate | Critical |
| Payment System | ✅ Phase 2 Done | Stub payment-service, create-order/confirm/refund/webhook | Critical |
| Notifications | ✅ Phase 1 Done | Stub notification-service, /notifications/send, provider-agnostic | Critical |
| Admin Panel | Minimal | Full operations console | High |
| Security & Rate Limiting | ✅ Rate Limiting Done | RateLimitingFilter (120/min general, 20/min auth), audit/revocation pending | High |
| Error Handling & Fallback | Partial | Circuit breakers, queues, fallback logic | High |

---

## Improvement Plan For Missing Features

### 1. Driver Verification System

Why it is important:

- Mandatory for safety, trust, and compliance.
- Drivers should not go online before approval.

Required APIs:

- `POST /verification/drivers/{driverId}/documents`
- `GET /verification/drivers/{driverId}`
- `POST /verification/drivers/{driverId}/submit`
- `POST /verification/drivers/{driverId}/approve`
- `POST /verification/drivers/{driverId}/reject`

Suggested DB schema:

```sql
driver_verifications (
  id uuid primary key,
  driver_id uuid not null,
  status varchar(32) not null,
  reviewed_by uuid null,
  rejection_reason text null,
  created_at timestamp not null,
  updated_at timestamp not null
)

driver_documents (
  id uuid primary key,
  driver_id uuid not null,
  document_type varchar(64) not null,
  file_url text not null,
  status varchar(32) not null,
  expires_at timestamp null,
  created_at timestamp not null
)
```

Suggested microservice:

- `verification-service`

### 2. Vehicle Verification System

Why it is important:

- Protects the platform from uninsured or non-compliant vehicles.

Required APIs:

- `POST /verification/vehicles/{driverId}/documents`
- `GET /verification/vehicles/{driverId}`
- `POST /verification/vehicles/{driverId}/approve`
- `POST /verification/vehicles/{driverId}/reject`

Suggested DB schema:

```sql
vehicle_documents (
  id uuid primary key,
  driver_id uuid not null,
  vehicle_number varchar(32) not null,
  document_type varchar(64) not null,
  file_url text not null,
  status varchar(32) not null,
  expires_at timestamp null,
  created_at timestamp not null
)
```

Suggested microservice:

- `verification-service`

### 3. Pricing Engine

Why it is important:

- Revenue cannot be generated reliably without estimated and final fare computation.

Required APIs:

- `POST /pricing/estimate`
- `POST /pricing/bookings/{bookingId}/finalize`
- `GET /pricing/bookings/{bookingId}`
- `POST /pricing/rules`

Suggested DB schema:

```sql
pricing_rules (
  id uuid primary key,
  city varchar(64) not null,
  vehicle_type varchar(32) not null,
  base_fare numeric(10,2) not null,
  per_km numeric(10,2) not null,
  per_minute numeric(10,2) not null,
  surge_multiplier numeric(5,2) not null,
  active boolean not null,
  created_at timestamp not null
)

booking_fares (
  id uuid primary key,
  booking_id uuid not null,
  estimated_fare numeric(10,2) null,
  final_fare numeric(10,2) null,
  currency varchar(8) not null,
  breakdown_json jsonb not null,
  created_at timestamp not null
)
```

Suggested microservice:

- `pricing-service`

### 4. Payment System

Why it is important:

- A Porter-like product needs digital payment, settlement, refunds, and invoice support.

Required APIs:

- `POST /payments/create-order`
- `POST /payments/confirm`
- `POST /payments/refund`
- `GET /payments/bookings/{bookingId}`
- `POST /payments/webhooks/provider`

Suggested DB schema:

```sql
payments (
  id uuid primary key,
  booking_id uuid not null,
  amount numeric(10,2) not null,
  currency varchar(8) not null,
  method varchar(32) not null,
  provider varchar(32) not null,
  provider_ref varchar(128) null,
  status varchar(32) not null,
  created_at timestamp not null,
  updated_at timestamp not null
)

refunds (
  id uuid primary key,
  payment_id uuid not null,
  amount numeric(10,2) not null,
  reason text null,
  status varchar(32) not null,
  created_at timestamp not null
)
```

Suggested microservice:

- `payment-service`

### 5. Notifications

Why it is important:

- Users and drivers need booking, assignment, arrival, payment, and failure notifications.

Required APIs:

- `POST /notifications/send`
- `POST /notifications/templates`
- `GET /notifications/users/{userId}`
- `POST /notifications/webhooks/delivery`

Suggested DB schema:

```sql
notification_logs (
  id uuid primary key,
  recipient_id uuid not null,
  recipient_type varchar(32) not null,
  channel varchar(16) not null,
  template_code varchar(64) not null,
  payload_json jsonb not null,
  status varchar(32) not null,
  created_at timestamp not null,
  delivered_at timestamp null
)
```

Suggested microservice:

- `notification-service`

### 6. Rating And Review System

Why it is important:

- Production matching quality improves when ratings influence allocation and fraud detection.

Required APIs:

- `POST /ratings`
- `GET /ratings/drivers/{driverId}`
- `GET /ratings/users/{userId}`

Suggested DB schema:

```sql
ratings (
  id uuid primary key,
  booking_id uuid not null,
  from_user_id uuid not null,
  to_user_id uuid not null,
  score int not null,
  comment text null,
  created_at timestamp not null
)
```

Suggested microservice:

- `rating-service`

### 7. Support And Dispute Resolution

Why it is important:

- Refunds, complaints, abuse reports, and failed trip cases require an operational workflow.

Required APIs:

- `POST /support/tickets`
- `GET /support/tickets/{id}`
- `POST /support/tickets/{id}/assign`
- `POST /support/tickets/{id}/resolve`

Suggested DB schema:

```sql
support_tickets (
  id uuid primary key,
  booking_id uuid null,
  user_id uuid not null,
  category varchar(64) not null,
  priority varchar(32) not null,
  status varchar(32) not null,
  description text not null,
  assigned_to uuid null,
  created_at timestamp not null,
  updated_at timestamp not null
)
```

Suggested microservice:

- `support-service`

### 8. Security And Rate Limiting

Why it is important:

- Gateway abuse, brute force login attempts, and token compromise are major production risks.

Required APIs:

- No public business APIs required for basic rate limiting.
- Optional admin APIs:
  - `GET /admin/security/rate-limits`
  - `POST /admin/security/blocks`

Suggested DB schema:

```sql
auth_sessions (
  id uuid primary key,
  user_id uuid not null,
  refresh_token_hash text not null,
  expires_at timestamp not null,
  revoked boolean not null,
  created_at timestamp not null
)

audit_logs (
  id uuid primary key,
  actor_id uuid null,
  action varchar(128) not null,
  resource_type varchar(64) not null,
  resource_id varchar(128) null,
  metadata_json jsonb not null,
  created_at timestamp not null
)
```

Suggested microservice:

- keep rate limiting in `api-gateway`
- optional `audit-service` for centralized logging

### 9. Error Handling And Fallback

Why it is important:

- A production booking system must remain usable even if one downstream dependency slows or fails.

Required APIs:

- Usually internal only.
- Optional operational APIs:
  - `GET /admin/resilience/events`
  - `GET /admin/jobs/retries`

Suggested DB schema:

```sql
failed_jobs (
  id uuid primary key,
  job_type varchar(64) not null,
  payload_json jsonb not null,
  failure_reason text not null,
  retry_count int not null,
  next_retry_at timestamp null,
  created_at timestamp not null
)
```

Suggested microservice:

- `worker-service` or shared async workers

---

## Advanced Suggestions

### Driver Matching Algorithm (Nearest Driver)

Current gap:

- Matching is availability-based, not truly nearest-driver based.

Recommended algorithm:

1. Store latest driver coordinates in Redis and PostGIS.
2. Filter by vehicle type, verification status, and online status.
3. Compute nearest candidates using geospatial query.
4. Score candidates using weighted formula:

   `score = distance_weight + acceptance_rate_weight + driver_rating_weight + idle_time_weight`

5. Offer in ranked order with short TTL.
6. If no response, move to next candidate asynchronously.

Suggested tech:

- PostGIS for geospatial queries
- Redis for hot location cache
- Kafka for booking created and offer expired events

### WebSocket For Real-Time Tracking

Current state:

- Tracking service already publishes over WebSocket.

Recommended improvements:

- Authorize subscriptions so only allowed users can subscribe to a booking’s driver topic.
- Add booking-scoped channel such as `/topic/bookings/{bookingId}/tracking`.
- Add fallback polling endpoint for unstable mobile networks.
- Add key TTL and history retention.

### Redis For Location Caching

Current state:

- Redis is used for latest driver location.

Recommended improvements:

- Add TTL to stale location keys.
- Separate cache key namespaces by environment and region.
- Store geohash for quick locality grouping.
- Add periodic cleanup job.

### Kafka For Event-Driven Architecture

Recommended domain events:

- `user.registered`
- `driver.registered`
- `driver.verified`
- `booking.created`
- `booking.assigned`
- `booking.cancelled`
- `booking.completed`
- `offer.created`
- `offer.accepted`
- `payment.completed`
- `notification.requested`

Benefits:

- Decouples notifications, analytics, payments, and audits from booking flow.
- Makes retries, replay, and operational observability much easier.

---

## Recommended Implementation Phases

### Phase 1: Production Safety And Compliance

- Driver verification
- Vehicle verification
- Rate limiting
- JWT refresh and revocation
- Notification service for OTP and booking updates
- Circuit breakers and retry policies

### Phase 2: Revenue Enablement

- Pricing service
- Payment service
- Invoice generation
- Refund flow

### Phase 3: Dispatch And Experience Quality

- Geospatial matching
- Ratings and reviews
- Better tracking access control
- Driver earnings and trip summaries

### Phase 4: Operational Excellence

- Admin operations dashboard
- Support service
- Audit service
- Analytics and reporting

### Phase 5: Scale Architecture

- Kafka event backbone
- Async workers
- Distributed tracing
- Multi-city support
- Intelligent demand forecasting

---

## Final Assessment

### What Shifted already does well

- Clean separation of core services
- JWT-authenticated gateway flow
- Core booking lifecycle exists
- Driver offer workflow exists
- Redis plus WebSocket tracking foundation exists
- Booking idempotency is a strong production-oriented design choice

### What blocks Porter-like production readiness

- No pricing
- No payments
- No verification workflow
- No notifications
- No rate limiting
- No operational admin tooling
- Matching is not geospatial or intelligence-driven
- Resilience model is still basic

### Overall Verdict

Shifted is a solid MVP backend for a logistics or ride-booking platform, but it is not yet a production-grade Porter-equivalent system.

Best classification:

- `MVP-ready`: Yes
- `Pilot-city ready`: With Phase 1 fixes
- `Revenue-ready`: Only after Phase 2
- `Porter-like production-ready`: After Phases 1 through 4, with Phase 5 for scale
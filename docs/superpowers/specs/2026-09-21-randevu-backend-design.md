# Randevu Pazaryeri — Backend Design

**Date:** 2026-09-21  
**Scope:** Phase 1–7 (payment excluded)  
**Architecture:** Modular monolith, Spring Boot, PostgreSQL

## Decisions (approved)

| Topic | Choice |
|-------|--------|
| Scope | Phase 1–7; Phase 8 payment deferred |
| Confirmation | Business `autoConfirm` (default `true`) |
| Timezone | Per-Business `timezone` (e.g. `Europe/Istanbul`); store Instant/`timestamptz` |
| Cancellation | Customer until 2h before start; Provider/Admin anytime |
| Employee | Profile only (no login); nullable `userId` for future |
| Double-booking | PostgreSQL `EXCLUDE` on `tstzrange` + app validation in transaction |

## Domain packages

`auth`, `user`, `business`, `employee`, `service`, `availability`, `appointment`, `review`, `notification`, `common`, `config`

Each domain: controller → service → repository / entity / dto / mapper / exception as needed.

## Core entities

- **User** — CUSTOMER | PROVIDER | ADMIN; email unique; BCrypt password
- **RefreshToken** — hashed token, revoke on logout
- **Business** — owner, slug unique, status, timezone, autoConfirm
- **Category** — seedable; M:N with Business
- **Employee** — belongs to Business; optional userId
- **ServiceOffer** (table `services`) — duration, price; M:N Employee
- **WorkingHour** — multiple intervals per dayOfWeek per employee
- **TimeOff** — lunch windows or vacation ranges
- **Appointment** — PENDING/CONFIRMED/CANCELLED/COMPLETED/NO_SHOW; exclusion constraint on active statuses
- **Favorite** — unique (customerId, businessId)
- **Review** — unique appointmentId; rating 1–5; only COMPLETED
- **Notification** — in-app; channel-ready design

Active appointments (block slots): `PENDING`, `CONFIRMED`.

## API (base `/api/v1`)

Public: register/login/refresh, business search/detail, availability, public reviews.  
Auth: me, logout, notifications.  
Customer: appointments, cancel, favorites, reviews.  
Provider: own businesses, employees, services, hours, time-off, appointment lifecycle.  
Admin: users, business approval, categories.

Response envelope: `{ success, data [, pagination] }`.  
Errors: `{ timestamp, status, code, message }`.

## Availability

Query: serviceId, optional employeeId, date.  
Compute against working hours − time off − active appointments, in business timezone.  
If no employeeId: return slots grouped by employee who provides the service.

## Booking concurrency

1. Validate employee/service/link/hours/time-off/past  
2. Set status from `autoConfirm`  
3. Insert; DB EXCLUDE `(employee_id WITH =, during WITH &&) WHERE status IN ('PENDING','CONFIRMED')`  
4. Map constraint violation → `APPOINTMENT_SLOT_UNAVAILABLE`

## Infra

- Java 21, Spring Boot 3.x, Maven, Flyway (`ddl-auto=validate`), OpenAPI  
- Docker Compose: PostgreSQL + Redis (Redis unused until later; config ready)  
- Env: `DATABASE_*`, `JWT_*`  
- Seed: Studio Nova + test users (customer/provider/admin)

## Testing (minimum)

Appointment: valid, past, outside hours, no service link, time-off, conflict, concurrent, cancel, complete.  
Auth: register, login, bad password, refresh, unauthorized.  
Business: create own; cannot modify another's.

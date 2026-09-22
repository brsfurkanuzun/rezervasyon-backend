# Randevu Backend Implementation Plan

> **For agentic workers:** Implement task-by-task. Checkbox tracking.

**Goal:** Production-ready modular monolith backend for appointment marketplace (Phase 1–7).

**Architecture:** Spring Boot modular packages; Flyway-owned schema; JWT+refresh; PostgreSQL EXCLUDE for double-booking.

**Tech Stack:** Java 21, Spring Boot 3.4+, Spring Security, JPA, PostgreSQL, Flyway, Redis (ready), OpenAPI, Maven.

## Global Constraints

- Entity never returned from API — DTOs only
- No business logic in controllers
- `ddl-auto=validate`
- Secrets via env vars
- BCrypt passwords; hashed refresh tokens

## Tasks

- [ ] T1 Scaffold + Docker + Flyway V1–Vn schema
- [ ] T2 Common (ApiResponse, exceptions, advice) + config (Security, OpenAPI, Redis stub)
- [ ] T3 User + Auth (register/login/refresh/logout/me)
- [ ] T4 Business + Category + ownership
- [ ] T5 Employee + Service + M:N + working hours + time off
- [ ] T6 Availability calculator
- [ ] T7 Appointment + EXCLUDE + cancel/complete
- [ ] T8 Search, detail, favorites
- [ ] T9 Reviews + notifications
- [ ] T10 Admin endpoints
- [ ] T11 Seed data + critical tests
- [ ] T12 README + verify build

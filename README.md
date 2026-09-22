# Randevu Pazaryeri Backend

Modular monolith Spring Boot API for an appointment marketplace.

## Stack

- Java 21, Spring Boot 3.5, Spring Security (JWT + refresh), JPA, PostgreSQL, Flyway, OpenAPI, Maven

## Quick start

```bash
# from repo root
docker compose up -d

cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

> Postgres is exposed on **host port 5433** (5432 is often already taken by a local install). Redis stays on 6379.

Swagger: http://localhost:8080/swagger-ui.html

### Seed users (profile `dev`)

| Email | Password | Role |
|-------|----------|------|
| admin@randevu.local | Password123! | ADMIN |
| provider@randevu.local | Password123! | PROVIDER |
| customer@randevu.local | Password123! | CUSTOMER |

Business: **Studio Nova** (`studio-nova`)

## Environment

| Variable | Default |
|----------|---------|
| DATABASE_URL | jdbc:postgresql://localhost:5433/randevu |
| DATABASE_USERNAME | randevu |
| DATABASE_PASSWORD | randevu |
| JWT_SECRET | (change in production) |
| JWT_ACCESS_EXPIRATION | 900000 |
| JWT_REFRESH_EXPIRATION | 604800000 |
| CORS_ALLOWED_ORIGINS | http://localhost:3000,http://localhost:5173 |
| REDIS_HOST / REDIS_PORT | localhost / 6379 (optional; not required at runtime yet) |

## Modules

`auth`, `user`, `business`, `employee`, `serviceoffer`, `availability`, `appointment`, `review`, `favorite`, `notification`, `admin`, `common`, `config`

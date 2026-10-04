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
| CORS_ALLOWED_ORIGINS | https://resplz.com,https://www.resplz.com,https://partner.resplz.com + localhost dev origins |
| REDIS_HOST / REDIS_PORT | localhost / 6379 (optional; not required at runtime yet) |
| CLOUDINARY_CLOUD_NAME | (empty — image uploads disabled until set) |
| CLOUDINARY_API_KEY | (empty) |
| CLOUDINARY_API_SECRET | (empty — secret, server-side only) |
| UPLOAD_MAX_FILE_SIZE | 5MB |
| UPLOAD_MAX_REQUEST_SIZE | 6MB (multipart envelope; keep slightly above the file limit) |
| IMAGE_STORAGE_ROOT | resplz (top-level storage folder) |
| UPLOAD_RATE_LIMIT_MAX / UPLOAD_RATE_LIMIT_WINDOW | 30 / 10m per user (in-memory) |

In production (Railway) set these under **Railway → backend service → Variables**. Never commit real values:
`.env` is git-ignored and `backend/.env.example` lists the keys with empty values. For local runs you can copy it to
`backend/.env`; Spring imports that file automatically when it exists.

## Image uploads

Images are stored in Cloudinary behind the `ImageStorageService` abstraction (`image.storage` package); only
`CloudinaryImageStorageService` knows about Cloudinary, so another provider (S3/R2) can be added without touching
controllers or business services. PostgreSQL keeps metadata only (`images` table), never the file itself.

- Accepted: JPEG, PNG, WebP, AVIF (configurable) up to 5 MB, verified by Content-Type **and** file signature. SVG is rejected.
- Stored as size-capped WebP with automatic quality under `resplz/businesses/{id}/profile|cover|gallery`,
  `resplz/services/{id}` and `resplz/users/{id}/avatar`. Object names are random UUIDs; client file names are ignored.
- Profile, cover, service image and avatar are single slots: a new upload replaces the old one only after it is stored.
- Without Cloudinary variables the API still starts; upload calls return `500 STORAGE_ERROR`.

| Method | Path | Who |
|--------|------|-----|
| POST | `/api/v1/images/upload` (`file`, `folder`, `ownerId`) | depends on folder |
| POST | `/api/v1/users/me/avatar` | signed-in user |
| POST | `/api/v1/businesses/{businessId}/images` (`file`, `folder`) | owner (gallery: team) |
| POST | `/api/v1/businesses/{businessId}/gallery` | owner or team member |
| GET | `/api/v1/businesses/{businessId}/images` | public |
| DELETE | `/api/v1/businesses/{businessId}/images/{imageId}` | owner, uploader on the team, admin |
| POST | `/api/v1/services/{serviceId}/image` | business owner |
| DELETE | `/api/v1/images/{imageId}` | owner, uploader, admin |

## Modules

`auth`, `user`, `business`, `employee`, `serviceoffer`, `availability`, `appointment`, `review`, `favorite`, `notification`, `admin`, `common`, `config`

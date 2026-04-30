# Deoham-BE

Spring Boot 3.5 / Java 17 backend for Deoham. Auth via Supabase (Kakao OAuth → Supabase JWT). Postgres on Supabase, Redis cache, S3 file storage.

## Prerequisites

- JDK 17 (Temurin recommended)
- Docker (for local Postgres+Redis via `compose.yaml`, and Testcontainers in tests)

## Run locally

```bash
./gradlew bootRun
```

Spring Boot's docker-compose support auto-starts Postgres+Redis from `compose.yaml`. Default profile is `local`.

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

## Configuration

Local defaults are in `src/main/resources/application-local.yml`. To hit a real Supabase project locally, override:

```bash
SUPABASE_JWT_ISSUER=https://<ref>.supabase.co/auth/v1\
SUPABASE_JWT_JWKS_URI=https://<ref>.supabase.co/auth/v1/.well-known/jwks.json\
  ./gradlew bootRun
```

Production env vars are listed in `CLAUDE.md`.

## Build

```bash
./gradlew clean build         # full build + tests (needs Docker for Testcontainers)
./gradlew bootJar -x test     # JAR only
docker build -t deoham-be .   # container image
```

## Layout

See `CLAUDE.md` for architecture, package layout, auth flow, and Supabase connection notes.

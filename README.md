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

Secrets (Kakao OAuth keys, Gemini API key, etc.) are kept out of the yml files and loaded from a local `.env` file, gitignored and never committed:

```bash
cp .env.example .env   # then fill in the real values
./gradlew bootRun      # bootRun reads .env automatically (see build.gradle)
```

`KAKAO_REST_API_KEY`, `KAKAO_CLIENT_SECRET`, and `GEMINI_API_KEY` (get one at https://aistudio.google.com/apikey) have no default and are required — the app fails to start without them (`@NotBlank` validation on `GeminiProperties`, and unresolved `${...}` placeholders for the Kakao keys). Everything else in `.env.example` is optional and falls back to the default in `application-local.yml`.

`docker compose` (`compose.yaml` / `compose-local.yaml`) reads the same `.env` via `env_file`, so there's one source of truth for local secrets regardless of how you run the app.

Production env vars are listed in `CLAUDE.md`.

## Build

```bash
./gradlew clean build         # full build + tests (needs Docker for Testcontainers)
./gradlew bootJar -x test     # JAR only
docker build -t deoham-be .   # container image
```

## Layout

See `CLAUDE.md` for architecture, package layout, auth flow, and Supabase connection notes.

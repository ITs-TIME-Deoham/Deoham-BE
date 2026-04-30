# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

- Spring Boot 3.5.14, Java 17, Gradle
- PostgreSQL (Supabase) + Flyway
- Redis
- Spring Security 6 + OAuth2 Resource Server (Supabase JWT, ES256/JWKS)
- SpringDoc OpenAPI 2.8.x
- AWS SDK v2 (S3 + STS) for file storage
- Testcontainers (Postgres + Redis) for integration tests

## Common commands

```bash
# Run app locally (auto-starts Postgres+Redis via spring-boot-docker-compose)
./gradlew bootRun

# Build JAR (skips tests)
./gradlew bootJar -x test

# Full build incl. tests (requires Docker for Testcontainers)
./gradlew clean build

# Run a single test class / method
./gradlew test --tests com.deoham.DeohamApplicationTests
./gradlew test --tests 'com.deoham.SomeTest.someMethod'

# Run with prod profile against env-configured infra
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

App URL: `http://localhost:8080`. Swagger UI: `/swagger-ui.html`. Health: `/actuator/health`.

## Architecture

### Auth flow (option A — Supabase as identity provider, no own JWT)

1. Frontend signs in via Supabase (Kakao OAuth).
2. Frontend sends Supabase access token as `Authorization: Bearer <jwt>` to this backend.
3. Spring's OAuth2 resource server validates the JWT against Supabase's JWKS endpoint (asymmetric ES256 verification).
4. Three validators are chained (see `SecurityConfig.jwtDecoder`):
   - default (exp/nbf), `JwtIssuerValidator(issuer)`, custom `audienceValidator` (handles both string and array `aud` forms).
5. `SupabaseJwtAuthenticationConverter` maps the `role` claim to `ROLE_<role>` authority and sets `sub` (UUID) as principal name.
6. Service code calls `SupabaseAuthenticationUtils.currentPrincipal()` to get a typed `SupabasePrincipal(userId, email, role)`.

The backend never issues its own tokens. Logout = client drops the Supabase token.

### Profiles

- `application.yml` — defaults (port, JPA settings, JSON, Swagger, Supabase/CORS/S3 keys with placeholders).
- `application-local.yml` — points at the docker-compose Postgres+Redis on localhost; verbose logging.
- `application-prod.yml` — fully env-driven; disables `spring.docker.compose`; tunes Hikari + access logs.

`spring.profiles.active` defaults to `local`. Production must set `SPRING_PROFILES_ACTIVE=prod`.

### Required env vars (prod)

- `SPRING_DATASOURCE_URL` — Supabase Postgres JDBC URL
- `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_DATA_REDIS_PASSWORD`, `SPRING_DATA_REDIS_SSL`
- `SUPABASE_JWT_ISSUER` — e.g. `https://<project-ref>.supabase.co/auth/v1`
- `SUPABASE_JWT_JWKS_URI` — e.g. `https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json`
- `SUPABASE_JWT_AUDIENCE` — defaults to `authenticated`
- `AWS_S3_REGION`, `AWS_S3_BUCKET`, `AWS_S3_PRESIGNED_TTL`
- AWS credentials via standard chain (env, IAM role, etc.)
- `CORS_ALLOWED_ORIGINS` — comma-separated

### Supabase Postgres connection notes

- **Connection target**: prefer the **Supavisor session-mode pooler** (`aws-0-<region>.pooler.supabase.com:5432`, username `postgres.<project-ref>`) over the direct `db.<project-ref>.supabase.co:5432` host (the latter is IPv6-only on newer projects). Spring + HikariCP needs persistent stateful connections, so **session mode is correct — do not use port 6543 (transaction mode), it breaks prepared statements / `SET` and Hibernate.**
- `?sslmode=require` in the JDBC URL.
- Flyway has `baseline-on-migrate: true` because Supabase ships with `auth`, `storage`, `realtime` schemas. Our migrations only touch `public` (default).

### JWT signing keys (Supabase)

Project must use **asymmetric signing keys (ES256)** in Supabase dashboard → Project Settings → JWT Keys. Legacy HS256 (shared secret) projects need the security config swapped to a `MacAlgorithm.HS256` decoder — not what's wired up today.

### Package layout

```
com.deoham
├── DeohamApplication
└── global
    ├── config         # SecurityConfig, RedisConfig, OpenApiConfig, S3Config + *Properties
    ├── security       # SupabaseJwt*, SupabasePrincipal, RestAuthenticationEntryPoint, RestAccessDeniedHandler
    ├── exception      # ErrorCode, BusinessException, GlobalExceptionHandler
    └── response       # ApiResponse<T>
```

Domain code goes under `com.deoham.<feature>` (e.g. `com.deoham.user`) — controller + service + repository + entity per feature, not layered globally. (No domain modules exist yet.)

### Response envelope

All controller responses go through `ApiResponse<T>`:
- success: `{ "success": true, "data": ..., "error": null }`
- failure: `{ "success": false, "data": null, "error": { "code": "...", "message": "..." } }`

Filter-thrown auth errors go through `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` (same envelope). Everything else flows through `GlobalExceptionHandler`.

### Schema management

- Owned by Flyway. `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never alters the DB.
- Migrations: `src/main/resources/db/migration/V<N>__<description>.sql`.

## Things that bite

- **Don't enable transaction-mode pooler (port 6543)** for the JDBC URL — Hibernate/HikariCP break.
- **`audience` validator must accept both `String` and `Collection<String>`** — Supabase emits `aud: "authenticated"` (single string), but the JWT spec allows arrays; defensive check is in `SecurityConfig.audienceValidator`.
- **Filter-time auth failures don't hit `@RestControllerAdvice`** — they hit the `AuthenticationEntryPoint` / `AccessDeniedHandler`. Both paths must produce the same `ApiResponse` shape.
- **`open-in-view: false`** is set — lazy associations outside `@Transactional` will throw. Keep transaction boundaries explicit.

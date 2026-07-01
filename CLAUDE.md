# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

- Spring Boot 3.5.14, Java 17, Gradle
- PostgreSQL + Flyway
- Redis
- Spring Security 6 + OAuth2 Resource Server (self-issued HS256 JWT)
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

### Auth flow (standard OAuth + self-issued JWT)

1. Frontend initiates social login (e.g., Kakao OAuth) via `/api/auth/**`.
2. Backend handles the OAuth callback, creates/updates the user record.
3. Backend issues its own HS256 JWT signed with `JWT_SECRET`.
4. Frontend sends the JWT as `Authorization: Bearer <jwt>` on every request.
5. Spring's OAuth2 resource server validates the JWT signature + expiry (`SecurityConfig.jwtDecoder`).
6. `AppJwtAuthenticationConverter` maps the `role` claim to `ROLE_<role>` authority and sets `sub` (UUID) as principal name.
7. Service code calls `AuthenticationUtils.currentPrincipal()` to get a typed `AuthPrincipal(userId, email, role)`.

### Profiles

- `application.yml` — defaults (port, JPA settings, JSON, Swagger, JWT/CORS/S3 keys with placeholders).
- `application-local.yml` — points at the docker-compose Postgres+Redis on localhost; verbose logging.
- `application-prod.yml` — fully env-driven; disables `spring.docker.compose`; tunes Hikari + access logs.

`spring.profiles.active` defaults to `local`. Production must set `SPRING_PROFILES_ACTIVE=prod`.

### Required env vars (prod)

- `SPRING_DATASOURCE_URL` — Postgres JDBC URL
- `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_DATA_REDIS_PASSWORD`, `SPRING_DATA_REDIS_SSL`
- `JWT_SECRET` — HS256 signing secret (minimum 32 chars / 256 bits)
- `AWS_S3_REGION`, `AWS_S3_BUCKET`, `AWS_S3_PRESIGNED_TTL`
- AWS credentials via standard chain (env, IAM role, etc.)
- `CORS_ALLOWED_ORIGINS` — comma-separated

### Package layout

```
com.deoham
├── DeohamApplication
└── global
    ├── config         # SecurityConfig, RedisConfig, OpenApiConfig, S3Config + *Properties
    ├── security       # JwtProperties, AppJwtAuthenticationConverter, AuthPrincipal, AuthenticationUtils,
    │                  # RestAuthenticationEntryPoint, RestAccessDeniedHandler, StompAuthChannelInterceptor
    ├── exception      # ErrorCode, BusinessException, GlobalExceptionHandler
    └── response       # ApiResponse<T>
```

Domain code goes under `com.deoham.<feature>` (e.g. `com.deoham.user`) — controller + service + repository + entity per feature, not layered globally.

### Response envelope

All controller responses go through `ApiResponse<T>`:
- success: `{ "success": true, "data": ..., "error": null }`
- failure: `{ "success": false, "data": null, "error": { "code": "...", "message": "..." } }`

Filter-thrown auth errors go through `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` (same envelope). Everything else flows through `GlobalExceptionHandler`.

### Schema management

- Owned by Flyway. `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never alters the DB.
- Migrations: `src/main/resources/db/migration/V<N>__<description>.sql`.

## Things that bite

- **Filter-time auth failures don't hit `@RestControllerAdvice`** — they hit the `AuthenticationEntryPoint` / `AccessDeniedHandler`. Both paths must produce the same `ApiResponse` shape.
- **`open-in-view: false`** is set — lazy associations outside `@Transactional` will throw. Keep transaction boundaries explicit.
- **`JWT_SECRET` must be ≥ 32 chars** — NimbusJwtDecoder with HmacSHA256 requires a 256-bit key minimum.

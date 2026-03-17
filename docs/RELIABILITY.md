# Reliability

This document defines the current runtime and completion expectations for the template. It is the durable reference for how the app is expected to start, validate, and run across local development, testing, and production-style container usage.

## Runtime Baseline
- Default profile: `dev`
- Local app URL: `http://localhost:8080`
- Local database: PostgreSQL on `localhost:5433`
- Database name/user/password in dev: `template`
- Production profile: `prod`
- Production datasource config: `DB_URL`, `DB_USER`, `DB_PASSWORD`
- Production port: `PORT` or `8080`

This template assumes PostgreSQL everywhere. Local development and tests should not drift onto an embedded database path.

## Local Development Flow
The standard local loop is:
1. Start PostgreSQL with `docker compose up -d`
2. Start the app with `./mvnw spring-boot:run`
3. Use `/actuator/health` to confirm the app is alive

The Compose setup uses `postgres:17-alpine`, maps host port `5433` to container port `5432`, and keeps local data in the `postgres_data` volume.

## Test Reliability Expectations
- Docker is required for tests because the repo uses Testcontainers.
- `./mvnw -q verify` is the canonical full validation command.
- Persistence tests should exercise real PostgreSQL behavior through Testcontainers.
- Runtime wiring should still be validated by starting the application after `verify`.

The repo standard is explicit: passing tests alone are not enough if the app fails to boot with real wiring.

## Health And Observability
- Actuator is enabled.
- Web exposure is intentionally limited to `health` and `info`.
- `/actuator/health` is the standard smoke-check endpoint.

This template currently favors a minimal operational surface rather than a full local observability stack. That is acceptable as long as startup, health, and core validation remain deterministic.

## Production-Style Runtime
The documented production container flow is:
- build a Docker image for the app
- run with `SPRING_PROFILES_ACTIVE=prod`
- pass datasource configuration via environment variables
- expose the application port

GitHub Actions runs `./mvnw verify`, startup smoke validation, and offline OpenAPI drift checks, and the deploy workflow is shaped as staging first and production second.

## Completion Standard
For repo changes that affect code, runtime wiring, or the harness:
- use the right test tier for the change
- run `scripts/harness/full-check` or the equivalent `./mvnw -q verify` plus startup validation flow
- use `scripts/harness/run-app` for Codex-side local startup when you want the stable harness entrypoint
- confirm the application starts successfully

## Known Limits
- Local reliability currently depends on Docker being available.
- Local startup remains part of the completion standard even though CI already runs startup smoke; CI cannot prove every developer machine matches the documented local runtime path.
- The runtime harness is still intentionally minimal. Richer diagnostics beyond health, startup, and generated summaries remain deferred.

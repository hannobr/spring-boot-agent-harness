# Architecture

This repository is an agent-first Java 25 / Spring Boot 4 template. The design goal is to keep business modules bounded, runtime behavior inspectable, and repo validation deterministic for both humans and coding agents.

## Topology

- Application root: `nl.jinsoo.template`
- Module model: top-level packages are business modules, not horizontal technical layers
- Ships a **notepad** reference module demonstrating the full vertical slice pattern; `init-template` removes it when forking

The goal is to make each module the default unit of change for both humans and coding agents.

## Stack

- Java 25
- Spring Boot 4.0.3
- Spring Modulith 2.0.3
- Spring MVC + validation
- Spring Security OAuth2 Resource Server
- Spring Data JDBC
- Flyway
- PostgreSQL
- Testcontainers
- Actuator

Build-time quality gates are part of the architecture, not an optional add-on. The repo uses Spotless, PMD, SpotBugs, JaCoCo, ArchUnit, Error Prone + NullAway (JSpecify null safety), Spring Modulith verification, OpenAPI drift checks, and startup smoke validation.

## Module Shape

### Public surface

For each module, the root package is the published API only. Typical public root-package types are:

- `*API` interfaces
- domain records
- domain exceptions
- command records that intentionally cross the module boundary

### Hidden implementation

Implementation stays outside the public surface:

- `internal/`: use cases, ports, wiring
- `persistence/`: JDBC entities, Spring Data repositories, persistence adapters
- `rest/`: controllers, request/response DTOs, exception handlers

Other modules may depend only on the published root-package API, never on `internal/`, `persistence/`, or `rest/`.

Detailed module structure rules, creation checklist, and cross-module patterns are in [`.claude/rules/modulith.md`](.claude/rules/modulith.md).

## Persistence

- Storage is PostgreSQL only
- Schema evolution is Flyway-only
- Spring Data JDBC is the persistence model; JPA is intentionally forbidden

Persistence rules:

- keep database identifiers lowercase
- keep domain/entity mapping in adapters
- do not use another module's tables directly
- update migrations and module contracts together when ownership changes

## Runtime

- Local development profile: `dev`
- Local database: PostgreSQL on `localhost:5433` via `docker compose`
- Production profile: `prod`, driven by `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `JWT_ISSUER_URI`, and `CORS_ORIGINS`
- Virtual threads enabled (`spring.threads.virtual.enabled=true`)
- Actuator exposure: `health` and `info`
- Security posture today: JWT bearer token auth via Spring Security OAuth2 Resource Server, health/info public, CSRF disabled, and configurable local-browser CORS for `/api/**`
- Dev token workflow: `scripts/generate-token.sh` for future protected endpoints

Enforceable security rules and extension guidance are in [`.claude/rules/security.md`](.claude/rules/security.md). Extended rationale is in [`docs/design-docs/security-rationale.md`](docs/design-docs/security-rationale.md).

The runtime harness is intentionally simple: local Docker Compose for development, Testcontainers for tests.

## Validation

### Architecture enforcement

- [ArchitectureRulesTest.java](src/test/java/nl/jinsoo/template/ArchitectureRulesTest.java) enforces:
  - no JPA imports
  - no field-level `@Autowired` or `@Value` injection
  - non-public `internal` classes
  - `@NullMarked` on all packages (JSpecify null safety)
- [ModularityVerificationTest.java](src/test/java/nl/jinsoo/template/ModularityVerificationTest.java) runs `ApplicationModules.verify()` to enforce Modulith structure

### Test pyramid

The repository standard is:

- unit tests without Spring
- persistence slice tests with `@DataJdbcTest` and Testcontainers
- REST slice tests with `@WebMvcTest`
- module tests with `@ApplicationModuleTest`
- full integration tests with `@SpringBootTest`

Spring Boot 4 testing APIs, fakes vs. mocks guidance, and anti-patterns are in [`.claude/rules/testing.md`](.claude/rules/testing.md).

### Completion

The current repo standard treats these as mandatory completion steps:

- `./mvnw -q verify`
- successful application startup via `scripts/harness/smoke-startup`
- committed OpenAPI spec aligned with the running app via `scripts/harness/check-openapi-drift --offline`

## Delivery And Repo Memory

- Onboarding and quick start: [README.md](README.md)
- Agent entrypoint: [CLAUDE.md](CLAUDE.md)
- Durable design index: [docs/design-docs/index.md](docs/design-docs/index.md)
- Enforced module contracts: [.claude/rules/modules](.claude/rules/modules)
- Governing architecture decision: [ADR-001-v2-agent-first-modularity.md](docs/architecture/decisions/ADR-001-v2-agent-first-modularity.md)
- Durable planning artifacts: [docs/exec-plans/active](docs/exec-plans/active) and [docs/exec-plans/completed](docs/exec-plans/completed)
- Planning rules: [PLANS.md](docs/PLANS.md)
- Learnings and gotchas: [LEARNINGS.md](docs/learnings/LEARNINGS.md)

This root map is intentionally brief. More detailed docs, local instructions, and harness scripts should narrow the search space further instead of turning this file into a second monolith.

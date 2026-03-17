---
paths:
  - "src/**/*.java"
  - "pom.xml"
---

# Code style and conventions

Priority when principles conflict: YAGNI > KISS > SOLID > DRY.

## Java conventions

- Records for value objects and DTOs. Sealed interfaces for type hierarchies.
- Constructor injection only. No field-level `@Autowired` or `@Value`.
- `Optional` as return type only, never as parameter.
- Composition over inheritance. No mapping frameworks (no MapStruct, ModelMapper).
- Parse, don't validate: convert raw input to domain shapes at adapter boundaries.
- Work inside-out: domain model first, application logic, then adapters, configuration last.
- Domain records and use cases stay free of Spring/framework annotations. Transaction boundaries belong on Facade/Service classes (see `transactions.md`).
- Readability is key. Favor clear, straightforward code over clever abstractions.
- No abbreviations except universally understood ones (ID, URL, HTTP).
- DTO classes: `*DTO` suffix. Domain classes: no suffix. Use cases: verb phrase (e.g. `CreateUserUseCase`).

## Class cohesion

When a class accumulates private methods that share no instance state — they only take parameters and return results — extract them into separate classes grouped by responsibility. The test: if you can move related private methods to a new class without passing `this`, they don't belong here.

## Lombok

Allowed annotations: `@Slf4j`, `@With`, `@Builder`. Nothing else — no `@Data`, `@Getter`, `@Setter`, `@Value`, `@AllArgsConstructor`, `@NoArgsConstructor`. Records and sealed types handle what those do.

## Logging

Always use Lombok `@Slf4j`. Never manual `LoggerFactory.getLogger()`.

- **INFO** — flow visibility (entry/exit of public methods, key decisions).
- **WARN** — recoverable issues (fallback, retry, degraded mode).
- **ERROR** — unrecoverable failures. Always include the exception as last argument.

Log entry and exit of every public method (except trivial getters, DTOs, records). Structured prefix: `[ClassName.methodName]`. Always use SLF4J `{}` placeholders, no string concatenation. No log-level guard checks — SLF4J placeholders are already lazy.

No logging in: domain layer, records/DTOs, tight loops. Never log secrets/PII.

## Dependencies

Always check Maven Central for latest stable version before adding a dependency.

### Spring Boot 4 starter renames

| Old | New |
|-----|-----|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-oauth2-client` | `spring-boot-starter-security-oauth2-client` |
| `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` |

Explicit starters (no longer auto-included): `spring-boot-starter-flyway`, `spring-boot-starter-restclient`, `spring-boot-starter-webclient`, `spring-boot-starter-security-test`.

### Database stack

Use `@ServiceConnection` for Testcontainers datasources. Never `@DynamicPropertySource`. Flyway mandatory for schema changes — migrations in `db/migration/`, never edit applied migrations.

### Jackson 3

Maven group: `tools.jackson` (not `com.fasterxml.jackson`).

### Spring Boot 4.x gotchas

1. Spring Data JDBC 4.x: explicit `@Id` required on entity ID fields.
2. ArchUnit: requires 1.4.1+ for Java 25.

## Security basics

- No secrets in source code. Use environment variables or Spring config properties.
- Least privilege for DB access in production.

## Operability

- Expose `/actuator/health` (and `/actuator/info` when useful).
- External service adapters must configure connect/read timeouts. Retry with backoff for transient failures only.

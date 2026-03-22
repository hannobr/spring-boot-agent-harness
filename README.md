# Spring Boot 4 Template

Java 25 Spring Boot 4 starter with an example CRUD module, providing an harness for claude code (codex also works, but is lagging behind in options for creating such a harness)

## What's included

- **Java 25 + Spring Boot 4** with virtual threads
- **Spring Modulith** vertical modules with enforced boundaries
- **Spring Data JDBC** (no JPA)
- **PostgreSQL + Flyway** migrations
- **Reference module** with a notes API (`POST /api/notes`, `GET /api/notes/{id}`)
- **Quality gates** via Spotless, PMD, SpotBugs, JaCoCo, ArchUnit, Error Prone + NullAway
- **Null safety** via JSpecify 1.0 annotations enforced at compile time
- **JWT auth** via Spring Security OAuth2 Resource Server
- **OpenAPI/Swagger** at `/swagger-ui.html`

## Prerequisites

**Docker** must be running (PostgreSQL runs in a container).

**Java 25** via [SDKMAN!](https://sdkman.io):

```bash
curl -s "https://get.sdkman.io" | bash
sdk install java 25-open
```

Maven is included via the wrapper (`./mvnw`).

## Quick start

```bash
git clone <repo-url> && cd <repo-name>
scripts/harness/run-app
```

Starts PostgreSQL and the app. Open [Swagger UI](http://localhost:8080/swagger-ui.html) to try the notes API.

## Make it yours

Rewrites packages, Maven coordinates, Docker config, and removes the sample notepad module:

```bash
scripts/harness/init-template \
  --group-id com.yourorg \
  --artifact-id your-service \
  --base-package com.yourorg.yourservice
```

Use `--dry-run` to preview changes first.

## Development

```bash
scripts/harness/fast-check           # Compile + doc-lint
./mvnw -q verify                     # Tests + quality gates
scripts/harness/full-check           # Verify + smoke startup + OpenAPI drift check
./mvnw spotless:apply                # Auto-format code
scripts/harness/new-module           # Scaffold a new module
```

## Learn more

- [ARCHITECTURE.md](ARCHITECTURE.md) for module structure, persistence rules, and test pyramid
- [CLAUDE.md](CLAUDE.md) for [Claude Code](https://claude.com/claude-code) integration

## License

[MIT](LICENSE)

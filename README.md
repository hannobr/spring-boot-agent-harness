# Spring Boot 4 Template

Agent-first Java 25 / Spring Boot 4 template with Spring Modulith, Spring Data JDBC, Flyway, PostgreSQL, and deterministic validation.

## Quick Start

```bash
scripts/harness/run-app
```

Or manually:

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Swagger UI: http://localhost:8080/swagger-ui.html

Optional once you add a protected `/api/...` endpoint:

```bash
./scripts/generate-token.sh [subject]
```

## Architecture

Spring Modulith vertical modules with enforced boundaries. Top-level packages under `nl.jinsoo.template` are business modules.

Each module follows one of two patterns:

| Pattern | When to use | Structure |
|---------|-------------|-----------|
| **flat** | Simple modules (≤5 classes, no REST, trivial persistence) | All classes in root package, package-private visibility |
| **standard** | Modules with business logic, persistence, and REST | `internal/`, `persistence/`, `rest/` subpackages |

Create a new module with `scripts/harness/new-module`. See [ARCHITECTURE.md](ARCHITECTURE.md) for the full architecture map.

## Development

```bash
scripts/harness/fast-check           # Compile + doc-lint
./mvnw -q verify                     # Tests + quality gates
scripts/harness/full-check           # Verify + smoke startup + OpenAPI drift
scripts/harness/generate-openapi     # Refresh docs/generated/openapi.json after API changes
./mvnw spotless:apply                # Format code
```

## Use as Template

```bash
scripts/harness/init-template \
  --group-id com.yourorg \
  --artifact-id your-service \
  --base-package com.yourorg.yourservice
```

Use `--dry-run` to preview changes before applying.

## Testing

| Tier | Annotation | Docker? |
|------|-----------|---------|
| Unit | None | No |
| Slice (persistence) | `@DataJdbcTest` | Yes |
| Slice (REST) | `@WebMvcTest` | No |
| Module | `@ApplicationModuleTest` | Yes |
| Integration | `@SpringBootTest` | Yes |

## Using with Claude Code

This repo is optimized for [Claude Code](https://claude.com/claude-code). The `.claude/rules/` directory encodes architecture rules, module contracts, and quality standards that Claude follows automatically.

See [CLAUDE.md](CLAUDE.md) for build commands and conventions.

## License

[MIT](LICENSE)

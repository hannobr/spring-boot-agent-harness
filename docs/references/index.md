# References Index

This directory is for curated reference pointers that help explain the stack this template uses.

## Repo-Local Authority First
Before reaching for external references, start with:
- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [README.md](../../README.md)
- [ADR-001-v2-agent-first-modularity.md](../architecture/decisions/ADR-001-v2-agent-first-modularity.md)
- [LEARNINGS.md](../learnings/LEARNINGS.md)
- [PLANS.md](../PLANS.md)

## High-Value Reference Areas
- Spring Boot 4 / Spring Framework 7 behavior that differs from earlier versions
- Spring Modulith module boundaries and module tests
- Spring Data JDBC patterns and limits
- Flyway database support and migration behavior
- Testcontainers and PostgreSQL usage for tests

## Repo Rule For External Knowledge
When external docs and repo reality diverge:
- follow the repo's enforced behavior first
- capture the mismatch in [LEARNINGS.md](../learnings/LEARNINGS.md) if it is reusable

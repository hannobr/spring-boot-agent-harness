# Core Beliefs

These are the stable engineering beliefs this template is designed to enforce. They are derived from the accepted architecture decision, the current Claude-side rules, and the repo's existing validation model. Future Codex-facing docs should align with them rather than inventing parallel doctrine.

## 1. Business modules are the primary boundary
Top-level packages under `nl.jinsoo.template` represent business capabilities, not horizontal layers. A module is the default unit of change, ownership, and validation.

Implications:
- prefer business-capability packages such as `ordering`, `fulfillment`, and `billing` over `controller/`, `service/`, or `repository/` root trees
- keep module boundaries explicit and small
- treat the module root package as the published API only

## 2. Public API is small; implementation is hidden
Cross-module access should happen only through root-package API types. `internal/`, `persistence/`, and `rest/` are implementation details hidden by design and by Modulith verification.

Implications:
- put `*API` interfaces, domain records, command records, and domain exceptions in the module root
- keep use cases, adapters, repositories, DTOs, and wiring out of the public surface
- add or update module contracts whenever the published surface changes

## 3. Inside-out development beats framework-first coding
The template favors domain and use-case design first, then adapter implementation, then wiring. Framework concerns should stay at the edges.

Implications:
- keep domain/use-case code free of Spring annotations and HTTP concerns
- parse and validate input at boundaries
- keep mapping inside adapters instead of spreading it through the module
- keep controllers thin and delegating

## 4. PostgreSQL + Flyway + Spring Data JDBC is the persistence baseline
This template is deliberately not JPA-based. Schema evolution is explicit, reviewed, and versioned through Flyway migrations, while persistence logic stays in Spring Data JDBC adapters.

Implications:
- do not introduce JPA
- do not use H2 or another embedded database as the standard path
- treat Flyway migrations as the only supported schema change mechanism
- keep database identifiers lowercase and keep entity/domain translation in persistence adapters

## 5. Deterministic validation is part of the architecture
Verification is not a postscript. The repo is designed so architecture, tests, and runtime checks are objective and repeatable.

Implications:
- ArchUnit and Modulith verification are mandatory, not optional documentation
- every meaningful change must end in the right test tier plus `./mvnw -q verify`
- runtime startup validation is part of completion, not an optional manual confidence boost

## 6. Tests must match the real layer being changed
The template favors a test pyramid with clear responsibilities instead of relabeling shallow tests as deeper assurance.

Implications:
- unit tests stay outside Spring
- persistence slice tests use `@DataJdbcTest` with Testcontainers
- REST slice tests use `@WebMvcTest`
- module tests use `@ApplicationModuleTest`
- end-to-end HTTP/database validation uses `@SpringBootTest`

## 7. Durable repo knowledge beats chat memory
If a rule, lesson, plan, or architecture decision matters, it should live in the repo.

Implications:
- non-trivial work gets a persisted execution plan
- reusable debugging outcomes go into `docs/learnings/LEARNINGS.md`
- architectural commitments go into ADRs and design docs
- generated summaries should exist when they reduce rediscovery cost

## 8. Agent support must be additive and portable
This repository already has a strong Claude-oriented setup. The Codex layer must align with it, not fork it.

Implications:
- do not weaken or contradict the Claude-configured architecture, testing, or planning rules
- keep root contracts short and link-heavy
- prefer repo-local docs and scripts over tool-specific magic

## 9. Entropy is expected, so cleanup must be explicit
Docs, plans, contracts, and generated artifacts drift unless the repo has a maintenance loop.

Implications:
- keep docs cross-linked and current
- record tech debt when shortcuts are taken
- use a visible quality/freshness score to make harness drift noticeable
- prefer generated summaries and lintable structure over manual policing where practical

## 10. The template should teach by example
The module patterns, rules, and scaffolding tools encode the golden path. New modules created from `scripts/harness/new-module` and the module contract template should reproduce the same architectural discipline from day one.

Implications:
- document both flat and standard module patterns clearly
- keep the module contract template, path-local `AGENTS.md`, and scaffolding consistent with the stated rules
- the first module added to this template should serve as the reference implementation

# ADR-001 v2: Agent-First Functional Modularity for Spring Boot Backends

**Status:** Accepted  
**Date:** 2026-03-09  
**Authors:** Architecture Team  
**Applies to:** Java/Spring Boot backend services that are expected to be maintained by both humans and coding agents

---

## Context

Our current architecture discussion focuses mainly on **human-readable modularity**: vertical slices, package-private internals, and Spring Modulith verification. That is necessary, but no longer sufficient.

Modern coding workflows are already shifting toward:

- **asynchronous coding agents** that work in background environments or cloud sandboxes,
- **parallel task execution** across branches/worktrees/sandboxes,
- **repo-scoped and path-scoped instructions** for agent behavior,
- **resumable long-running tasks** that survive context resets,
- **PR-centric delivery** with deterministic validation,
- **module-local ownership and evaluation contracts**.

A codebase that is only modular for humans still performs poorly for agents if:

- instructions live only in tribal knowledge or long ADR prose,
- validation commands are ambiguous or non-deterministic,
- module ownership is unclear,
- modules have broad implicit dependencies,
- tasks cannot be resumed from durable artifacts,
- one agent must understand the entire repo to change one slice.

The architectural question is therefore not only:

> "How should we structure code so humans can understand it?"

It is also:

> "How should we structure code and repository contracts so multiple coding agents can safely, deterministically, and repeatedly work on bounded parts of the system?"

---

## Decision

We will adopt **agent-first functional modularity** as the primary architectural model.

This means:

> **The business capability remains the primary code boundary, and every module must also be a bounded execution unit for coding agents.**

In practice, this has two layers:

1. **Code modularity** — business capabilities are implemented as vertical modules with enforced boundaries.
2. **Repository execution contracts** — each module and the repo root expose explicit, machine-readable instructions, dependency rules, and validation commands that let agents work locally without reconstructing architecture from scratch.

This ADR replaces the weaker idea that functional decomposition alone is enough.

---

## Why this decision

### 1. Vertical decomposition is still the right base layer

Horizontal packaging (`controllers/`, `services/`, `repositories/`, `models/`) still causes scattered change sets, weak ownership, and accidental coupling. A business capability should own its own internal implementation and expose only a narrow external surface.

### 2. Spring Modulith is useful, but it is not the whole answer

Spring Modulith gives us structural verification, module-level documentation, and focused integration testing. We will use it where applicable. But Modulith does **not** solve:

- agent instruction locality,
- resumability,
- deterministic task handoff,
- machine-readable module contracts,
- or bounded parallel execution.

### 3. Coding agents are becoming asynchronous, parallel, and path-sensitive

The coding-agent ecosystem is converging on a model where agents clone or mount a repo, work in isolated environments, read repo-local instructions, execute deterministic commands, and deliver changes through branches/PRs. That means architecture must optimize for **small search spaces**, **local instructions**, and **repeatable validation**.

### 4. Reliability remains bounded

Coding agents are improving quickly, but they are still brittle on ambiguous, long-horizon, real-repo tasks. That means architecture must actively reduce ambiguity instead of assuming the model will infer everything.

---

## Decision details

## Layer A — Application structure

### Rule A1: Top-level packages represent business capabilities

Forbidden:

```text
com.example.app
├── controller/
├── service/
├── repository/
└── model/
```

Required shape:

```text
com.example.app
├── ordering/
├── payments/
├── inventory/
├── customers/
└── platform/
```

Each top-level package is an **application module** representing one coherent business capability.

### Rule A2: A module is the default unit of change

A class belongs to exactly one owning module.

If a class appears to serve multiple modules, that is a design smell. Default action: split the class, not the boundary.

### Rule A3: Internal-by-default visibility

Inside a module, the default is package-private. Public types are exceptions.

Allowed public surface:

- module API interfaces,
- boundary DTOs/events that intentionally cross module boundaries,
- externally required configuration/bootstrapping types.

Everything else should be package-private unless there is a strong reason not to.

### Rule A4: Module interaction happens through explicit boundaries only

Allowed:

1. **Direct synchronous dependency** on another module's published API.
2. **Event-based interaction** when no synchronous response is required.

Forbidden:

- reaching into another module's internal classes,
- direct database access into another module's owned tables,
- using `shared`, `common`, or `util` as an escape hatch for business logic.

### Rule A5: `shared` is a quarantine zone, not a convenience zone

A `shared` or `platform` module may contain only:

- framework bootstrapping,
- generic technical utilities with no business ownership,
- primitive shared kernel concepts that are genuinely cross-cutting.

It must not become the place where unresolved ownership goes to die.

### Rule A6: Internal architecture is chosen per module, not globally

We do **not** mandate one internal style everywhere.

Per module, choose the lightest structure that fits reality:

- simple CRUD slice → flat,
- moderate business logic → light layering,
- complex policy/integration-heavy slice → ports/adapters internally.

The system is vertically modular first. Onion/hexagonal patterns are optional **inside** a module, not global top-level package laws.

### Rule A7: Spring Modulith is the default enforcement layer for Spring Boot codebases

For Spring Boot applications, we will use Spring Modulith to:

- verify module boundaries,
- prevent cycles,
- make dependency structure inspectable,
- support module-focused integration testing,
- and generate module documentation from code.

This is the default, not because Modulith is magical, but because enforcement beats documentation.

### Rule A8: Explicit dependencies beat implicit reachability

Where module dependencies matter, declare them explicitly rather than relying on the default openness of every published module API.

A module should depend only on the modules and named interfaces it actually needs.

### Rule A9: Events are preferred for decoupled cross-module reactions

Where a consumer does not need to participate in the caller's immediate response, publish an event and handle it after commit.

Do not use synchronous calls by default just because they are easier to type.

---

## Layer B — Repository execution contracts for agents

This is the part the earlier ADR was missing.

### Rule B1: The repository root must contain an agent instruction file

The repo root must provide a single canonical agent instruction file such as:

- `AGENTS.md`, or
- another tool-specific file that is generated from the same source.

This file must define at least:

- project purpose,
- canonical build/test commands,
- code style constraints,
- safety boundaries,
- how plans are persisted,
- what an agent must do before opening a PR.

### Rule B2: Modules may define path-local instructions

Each module may include local agent instructions that override or extend the root rules for that slice.

Typical contents:

- local invariants,
- allowed dependencies,
- local validation commands,
- migration cautions,
- data ownership rules,
- test expectations,
- representative examples.

The goal is that an agent working inside `ordering/` does **not** need the entire repo in active context to behave correctly.

### Rule B3: Every module must publish a deterministic validation contract

Each module must document the exact commands an agent can run to validate work in that module.

At minimum, define:

- fast local checks,
- module integration test command,
- full verification command if broader impact is possible.

Bad:

```text
Run the relevant tests.
```

Good:

```text
./mvnw -q -pl :ordering test
./mvnw -q test -Dtest=*Ordering* 
./mvnw -q -DskipITs=false verify
```

The exact commands depend on the build layout, but they must be concrete.

### Rule B4: Plans and resumability artifacts are mandatory for non-trivial work

If work is expected to take more than a very small change, the agent must persist a plan artifact before major edits.

The plan must include:

- objective,
- touched modules,
- assumptions,
- ordered steps,
- validation steps,
- rollback/risk notes,
- completion criteria.

If the task spans multiple sessions, the agent must update the artifact as the durable handoff state.

Conversation context is not a reliable source of truth.

### Rule B5: Ownership metadata must be local and machine-readable

Each module should expose small, parseable metadata, for example:

- owner/team,
- purpose,
- public APIs,
- allowed dependencies,
- owned tables/topics/events,
- validation commands.

This can be Markdown with constrained headings or a small sidecar manifest. The exact format is less important than consistency.

### Rule B6: Parallel work must compose safely

A module boundary should make parallel agent work feasible.

To support that, the repo should favor:

- narrow public APIs,
- low fan-out dependencies,
- low shared mutable configuration,
- minimal cross-module schema ownership,
- stable module-local tests.

If two agents cannot safely work in parallel on adjacent slices, the modularity is weaker than it looks.

### Rule B7: PR completion criteria must be objective

A PR is not "done" because the diff looks reasonable.

For agent-authored work, done means:

- declared plan completed,
- validation commands passed,
- architectural rules preserved,
- changed modules documented if boundaries changed,
- and no unresolved assumptions hidden in prose.

---

## Reference implementation shape

```text
repo/
├── AGENTS.md
├── docs/
│   └── execution-plans/
├── pom.xml
├── src/main/java/com/example/app/
│   ├── Application.java
│   ├── ordering/
│   │   ├── package-info.java
│   │   ├── AGENTS.md
│   │   ├── OrderingApi.java
│   │   ├── OrderingFacade.java
│   │   ├── Order.java
│   │   ├── OrderPlaced.java
│   │   ├── internal/
│   │   └── infra/
│   ├── payments/
│   │   ├── package-info.java
│   │   ├── AGENTS.md
│   │   ├── PaymentsApi.java
│   │   └── ...
│   └── platform/
│       └── ...
└── src/test/java/com/example/app/
    ├── ModularityVerificationTest.java
    ├── ordering/
    │   └── OrderingModuleTest.java
    └── payments/
        └── PaymentsModuleTest.java
```

Notes:

- `package-info.java` is where explicit Modulith dependencies may live.
- Module-local `AGENTS.md` files are optional but recommended for non-trivial modules.
- We allow internal subpackages (`internal`, `infra`, `web`, `persistence`) if the owning module remains clear.

---

## Mandatory repository standards

### 1. Verification test

Every Spring Boot app must include a module verification test.

```java
@Test
void verifyModularStructure() {
    ApplicationModules.of(Application.class).verify();
}
```

### 2. Module-focused integration tests

Critical modules must include `@ApplicationModuleTest` coverage, using the appropriate bootstrap mode for the slice under test.

### 3. Generated module documentation

The module graph/documentation should be generated from code and committed or otherwise published in CI artifacts.

### 4. Root agent instructions

Root-level agent instructions are mandatory.

### 5. Deterministic commands

`build`, `test`, and `verify` commands must be explicit and copy-pasteable.

### 6. Execution plans for non-trivial work

Large changes require persisted plan artifacts, not just chat history.

---

## Migration guidance

### Phase 1 — Fix the application shape

- move from horizontal packages to vertical modules,
- reduce visibility,
- identify public APIs,
- stop cross-module internal access,
- add verification tests.

### Phase 2 — Add agent repository contracts

- add root `AGENTS.md`,
- add module-local instructions where needed,
- define deterministic validation commands,
- add plan artifact format,
- add ownership metadata.

### Phase 3 — Optimize for parallel agent work

- reduce broad shared config,
- make module tests independent and fast,
- declare dependencies explicitly,
- minimize cross-module schema writes,
- document event flows and ownership.

---

## Consequences

### Positive

- Better local reasoning for both humans and agents.
- Lower search-space per task.
- Safer parallel work across modules/branches/sandboxes.
- Less architecture drift because boundaries are enforced.
- Faster recovery from interrupted or resumed agent sessions.
- Better suitability for PR-based and background-agent workflows.

### Negative / trade-offs

- More up-front structure and documentation work.
- Requires discipline to keep instruction files and validation commands current.
- Some teams will overproduce module boundaries if not careful.
- `shared` abuse remains a constant risk.
- Modulith alone may create false confidence if repository contracts are neglected.

---

## Non-goals

- This ADR does not require microservices.
- This ADR does not ban onion or hexagonal internals.
- This ADR does not assume agents are reliable enough to replace human review.
- This ADR does not standardize one universal module-internal layout.

---

## Acceptance criteria

This ADR can move to **Accepted** only when the repository demonstrates all of the following:

- top-level code is organized by business capability,
- module boundaries are verified in CI,
- root agent instructions exist,
- validation commands are explicit,
- non-trivial work leaves durable plan artifacts,
- module ownership and dependency boundaries are documented locally,
- and at least one real feature has been implemented end-to-end using this model.

---

## References

- Spring Modulith reference documentation
- OpenAI Codex docs on `AGENTS.md`, prompting, and cloud execution
- GitHub Copilot coding agent documentation
- Anthropic docs on subagents and context engineering
- METR work on task-completion horizons
- SWE-bench-Live as a live issue-resolution benchmark

---
name: dod
description: >
  Determine the right machine-checkable verification strategy before implementing
  a feature, bug fix, refactor, or migration. Auto-invoke when the task involves
  backend logic, data validation, transaction handling, security, regulatory rules,
  database changes, API contracts, bug fixes, or any change to production code paths.
  Also invoke when the user mentions "definition of done", "exit condition",
  "verification", "how should we test this", or "what tests do we need".
---

# Definition of Done — Verification Strategy Skill

ultrathink

You are operating in a regulated banking environment with a Java/Spring Boot stack.
Your job is NOT to enforce rigid TDD. Your job is to identify the cheapest
machine-checkable exit condition that is reliable enough for the risk level of the
current task, state it explicitly, and ensure it runs after implementation.

## Core Principle

Claude Code operates in a loop: write code → run something → check output → adjust.
The tighter and more unambiguous the "run something → check output" step, the less
you drift. A failing test is the gold standard, but it is not the only option.

## Step 1 — Assess Risk Tier

Before writing any code, classify the task into one of four tiers.

### CRITICAL — Write a failing test first, no exceptions

Tasks where a silent bug has financial, legal, or security consequences:

- Transaction logic, money movement, balance calculations
- Authentication and authorization changes (Spring Security)
- Regulatory and compliance logic (KYC, AML, PSD2, GDPR data handling)
- Data validation on financial inputs (amounts, IBANs, BSNs)
- Encryption, hashing, credential handling, secret rotation
- Audit trail and logging of regulated events

Exit condition: one or more JUnit 5 tests that assert the exact expected behavior,
confirmed failing before implementation, passing after.

```
# Concrete verification command
mvn test -pl <module> -Dtest=<TestClass>#<testMethod> -Dspring.profiles.active=test
```

### HIGH — Verify existing coverage, tighten if gaps exist

Tasks where bugs are caught in integration but are expensive to fix late:

- New REST endpoints with business logic
- Database migrations (Flyway/Liquibase scripts)
- Refactoring modules that have existing test coverage
- Integration with external services or message brokers
- State machine transitions
- Spring Modulith module boundary changes

Exit condition: run existing relevant tests first to confirm green baseline. If
coverage gaps exist for the specific change, write a targeted test. If an integration
test is needed, write it. Always run ArchUnit after structural changes.

```
# Baseline check
mvn test -pl <module> -Dspring.profiles.active=test

# ArchUnit verification for structural changes
mvn test -pl <module> -Dtest="*ArchitectureTest*"

# Spring Modulith verification
mvn test -pl <module> -Dtest="*ModularityTests*"
```

### MEDIUM — Compilation + existing suite + smoke check

Tasks where the type system and existing tests provide sufficient coverage:

- Adding a new Spring Modulith module (scaffolding)
- DTO and mapper changes covered by existing integration tests
- Configuration and property changes
- Dependency version bumps
- Adding or modifying Spring profiles

Exit condition: full compilation with NullAway/JSpecify checks passing, existing
test suite green, and if applicable a manual curl or httpie smoke check.

```
# Compile with static analysis
mvn compile -pl <module>

# Run full module tests
mvn test -pl <module>

# Optional smoke check (state the expected response)
curl -s http://localhost:8080/actuator/health | jq .status
# Expected: "UP"
```

### LOW — Compilation is the exit condition

Tasks where failure is immediately visible and cheap to fix:

- Scaffolding and boilerplate generation
- README, Javadoc, and documentation updates
- Logging additions (non-audit)
- IDE configuration, .gitignore, editor config
- Exploratory prototypes and throwaway spikes
- CSS and cosmetic frontend changes

Exit condition: `mvn compile` succeeds. No further verification needed.

```
mvn compile -pl <module>
```

## Step 2 — State the Exit Condition Before Coding

After assessing the tier, produce a brief statement in this format before writing
any implementation code:

```
TASK: <one-line description>
RISK TIER: <CRITICAL | HIGH | MEDIUM | LOW>
EXIT CONDITION: <exact command(s) and expected outcome>
RATIONALE: <one sentence on why this tier>
```

If the user has already provided tests or a clear exit condition, acknowledge it
and proceed. Do not repeat what they already know.

### Persist the exit condition

Immediately after stating the exit condition, write it to the state file so it
survives context compression. Use a single Bash tool call:

```bash
cat > .claude/dod-current.json << 'DODEOF'
{
  "task": "<one-line description>",
  "risk_tier": "<CRITICAL|HIGH|MEDIUM|LOW>",
  "exit_condition": "<exact command(s)>",
  "expected_outcome": "<what success looks like>",
  "stated_at": "<ISO 8601 UTC timestamp>"
}
DODEOF
```

Rules:
- **Overwrite, never delete.** Each new DoD invocation overwrites the file.
- The `stated_at` value must be the current UTC ISO 8601 timestamp.
- The `exit_condition` value must be the exact verification command, not prose.
- If the user provided tests or a clear exit condition that you acknowledged,
  still write the state file — the file is what the Stop hook checks.
- **Do not add `verified_at`** — that field is written only by
  `scripts/harness/full-check` when all checks pass.

## Step 3 — Implement Without Changing the Exit Condition

For CRITICAL and HIGH tasks where tests were written or identified first:
- Do not modify the test to make it pass in a way that weakens the assertion
- Do not delete or skip tests that inconveniently fail
- If a test assumption turns out to be wrong, stop and discuss with the user

## Step 4 — Run Verification and Report

After implementation, run the exact verification command stated in Step 2.
Report the result. If it fails, fix the implementation — not the test.

For CRITICAL tasks, also run:
```
# Check for test overfitting — does the test actually fail on bad input?
# Mutate one key value and confirm the test catches it
```

## Step 5 — Check for Collateral Damage

For HIGH and CRITICAL tasks, also run the broader module tests to confirm nothing
else broke:

```
mvn test -pl <module>
```

For structural changes (package moves, module boundary changes), run ArchUnit:
```
mvn test -Dtest="*ArchitectureTest*"
```

## Anti-Patterns to Avoid

- Writing a test that only asserts `assertNotNull(result)` — that is not a
  definition of done, that is a definition of "something happened"
- Spending 15 minutes writing a perfect test for a task that takes 30 seconds
  to implement and is immediately visible (scaffolding, config)
- Running the entire project test suite when only one module changed
- Generating tests that mirror the implementation rather than specifying behavior
- Treating this skill as bureaucracy — skip the formality for LOW tier tasks,
  just compile and move on

## Stack Reference

This project uses:
- Java 25, Spring Boot 4, Spring Modulith
- JSpecify + NullAway for null safety (compile-time checks count as verification)
- ArchUnit for architecture rule enforcement
- JUnit 5 + AssertJ for testing
- Flyway or Liquibase for database migrations
- Maven multi-module structure

Adapt commands to the actual module and test class names in the project.
When in doubt about which tier applies, go one tier higher — the cost of
a few extra minutes of testing is always less than the cost of a production
bug in banking software.

## Context From Supporting Files

For concrete examples of good exit condition statements per tier, see the
examples.md file in this skill's directory.

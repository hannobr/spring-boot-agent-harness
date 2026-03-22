---
status: active
---

# PLAN-0002: null safety with JSpecify + NullAway

## Goal
- Add compile-time null-safety enforcement using JSpecify 1.0 annotations and NullAway via Error Prone.
- Ensure all existing and future modules are `@NullMarked` by default.
- Update project rules, agents, scaffolding, and documentation so the null-safety contract is self-enforcing.

## Non-goals
- Enabling NullAway's experimental `JSpecifyMode=true` (full generics checking). Start with standard mode; revisit when NullAway stabilizes generic support.
- Adding null-safety to test sources. NullAway checks `src/main/java` only.
- Migrating to Error Prone's full bug-pattern suite. This plan adds Error Prone solely as NullAway's runtime host. PMD and SpotBugs remain the primary static analysis tools.

## Approach
- Add Error Prone as a compiler plugin and NullAway as an annotation processor via `maven-compiler-plugin`.
- Add `org.jspecify:jspecify:1.0.0` as a compile dependency for the annotation classes.
- Configure NullAway in `OnlyNullMarked` mode so only `@NullMarked` packages are checked — enabling gradual adoption.
- Add `@NullMarked` to every existing `package-info.java` and fix any violations in the notepad module.
- Add an ArchUnit rule enforcing that every `package-info.java` has `@NullMarked`.
- Update rules, agents, scaffolding, and docs so the null-safety contract propagates to all future work.

## Steps

### 1. Maven dependencies and compiler configuration
- [x] Add `jspecify` 1.0.0 dependency (compile scope)
- [x] Add `error-prone` compiler plugin configuration to `maven-compiler-plugin`
- [x] Add `nullaway` as annotation processor path alongside Lombok
- [x] Configure NullAway flags: `OnlyNullMarked=true`
- [x] Add `.mvn/jvm.config` with `--add-exports`/`--add-opens` for Error Prone (in-process, no fork)
- [x] Verify `./mvnw -q compile` passes (Error Prone + NullAway bootstrapped)

### 2. Annotate existing packages
- [x] Add `@NullMarked` to `notepad/package-info.java` (alongside existing `@ApplicationModule`)
- [x] Create `package-info.java` with `@NullMarked` in root package and all notepad subpackages (`internal/`, `persistence/`, `rest/`)
- [x] Scan notepad module for null-safety violations — none found, compile passes clean
- [x] Verify `./mvnw -q compile` still passes

### 3. ArchUnit rule
- [x] Add `allPackagesMustBeNullMarked` test to `ArchitectureRulesTest`
- [x] Verify `./mvnw -q test -Dtest="ArchitectureRulesTest"` passes

### 4. Update project rules and documentation
- [x] `.claude/rules/code-style.md` — add null-safety section covering `@NullMarked`, `@Nullable` usage, banned annotation sets, interaction with `Optional`
- [x] `.claude/rules/modulith.md` — update "Creating a new module" step 1 to include `@NullMarked` on `package-info.java` and subpackages
- [x] `.claude/agents/audit.md` — add NullAway and `@NullMarked` ArchUnit enforcement to "What NOT to check" list
- [x] `.claude/rules/modules/MODULE-TEMPLATE.md` — mention `@NullMarked` in "Rules for changes" section
- [x] `.claude/rules/modules/notepad.md` — add `@NullMarked` rule to match updated template (audit finding)
- [x] `CLAUDE.md` — update Architecture line to mention Error Prone + NullAway + JSpecify

### 5. Update scaffolding
- [x] `scripts/harness/new-module` — add `@NullMarked` to generated root `package-info.java`
- [x] `scripts/harness/new-module` — generate `package-info.java` with `@NullMarked` in subpackages for standard modules
- [x] `scripts/harness/new-module` — add `@NullMarked` rule to generated module contract "Rules for changes" section (audit finding)

### 6. Validation
- [x] `scripts/harness/full-check` passes
- [x] Audit agent passes (2 MEDIUM findings fixed)

## Contract updates (if this plan changes a module's REST boundary)
N/A — no REST boundary changes.

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| Error Prone may conflict with Java 25 preview features or current compiler flags | Error Prone 2.48.0 is tested against JDK 26 EA. Pin the version explicitly. If issues arise, use `-Xplugin:ErrorProne` with specific flags to disable problematic checks. |
| NullAway false positives on Spring-generated code (proxies, AOP) | `OnlyNullMarked` mode limits scope to annotated packages. Spring Boot 4 already ships JSpecify annotations on its own APIs, so interop is clean. |
| Lombok-generated code may confuse NullAway | Lombok's annotation processor runs before NullAway sees the code. Known to work — Uber runs both. If issues arise, use NullAway's `-XepOpt:NullAway:ExcludedFieldAnnotations` to suppress on `@Builder`/`@With` fields. |
| SpotBugs may duplicate or conflict with NullAway null-checking | SpotBugs' null-related checks are weaker and may produce noise now that NullAway handles this. Monitor for duplicates; add exclusions to `spotbugs-exclude.xml` if needed. Not blocking for this plan. |

## Definition of done
- [ ] `./mvnw -q compile` runs Error Prone + NullAway without failures
- [ ] Every `package-info.java` has `@NullMarked` (enforced by ArchUnit)
- [ ] ArchUnit test `allPackageInfoFilesMustBeNullMarked` passes
- [ ] `.claude/rules/code-style.md` has null-safety section
- [ ] `.claude/rules/modulith.md` step 1 includes `@NullMarked`
- [ ] `.claude/agents/audit.md` excludes NullAway-enforced checks
- [ ] `scripts/harness/new-module` generates `@NullMarked` in `package-info.java`
- [ ] `CLAUDE.md` Architecture line updated
- [ ] Audit agent passes
- [ ] `scripts/harness/full-check` passes

## Decision log

| Date | Decision | Rationale |
|---|---|---|
| 2026-03-22 | JSpecify 1.0 + NullAway (standard mode) over Checker Framework | Industry consensus: Spring team recommends this stack (Spring I/O 2025, Spring blog Nov 2025). Under 10% build overhead vs 2-5x for Checker Framework. Spring Boot 4 already ships JSpecify annotations. |
| 2026-03-22 | `OnlyNullMarked=true` mode | Enables gradual adoption — only packages with `@NullMarked` are checked. Combined with ArchUnit enforcement of `@NullMarked` on all packages, this means full coverage while keeping an escape hatch via `@NullUnmarked` for edge cases. |
| 2026-03-22 | ArchUnit rule for `@NullMarked` rather than relying on convention | Hard gate > documentation. Without the rule, unmarked packages silently skip null-checking. The ArchUnit rule makes it a build failure. |
| 2026-03-22 | Error Prone added as NullAway host only, not replacing PMD/SpotBugs | PMD and SpotBugs are already configured with project-specific rulesets. Error Prone's bug patterns overlap partially. Replacing them is a separate concern — this plan is scoped to null-safety only. |
| 2026-03-22 | `AnnotatedPackages` and `OnlyNullMarked` are mutually exclusive | NullAway rejects both flags simultaneously. Dropped `AnnotatedPackages` in favor of `OnlyNullMarked` which aligns with JSpecify standard and gives better IDE support. |
| 2026-03-22 | In-process Error Prone (`.mvn/jvm.config`) over forked compilation | Forked javac swallowed all error output making debugging impossible. In-process via `--add-exports`/`--add-opens` in `.mvn/jvm.config` gives visible errors and is the more common Maven setup. |
| 2026-03-22 | Every subpackage gets its own `@NullMarked` `package-info.java` | Java package annotations do not propagate to subpackages. Without `@NullMarked` on `internal/`, `persistence/`, `rest/`, NullAway silently skips them in `OnlyNullMarked` mode. |

## Tech debt introduced
- Error Prone is added solely as NullAway's host. Its ~400 other bug-pattern checks are active by default. If any produce noise, individual checks can be disabled via `-Xep:CheckName:OFF`. A future plan could curate the Error Prone ruleset deliberately.
- NullAway `JSpecifyMode` (full generics checking) is deferred. Type-argument nullability on generics is not enforced until that mode is enabled.

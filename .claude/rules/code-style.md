---
paths:
  - "src/**/*.java"
  - "pom.xml"
---

# Code style and conventions

Formatting is tool-driven: Spotless and `google-java-format` are the source of truth for whitespace, wrapping, and import layout. Do not hand-tune formatting against the formatter.

When principles conflict: YAGNI > KISS > readability > DRY.
Use SOLID as design heuristics, not as a precedence rule.

## Scope

- This file covers stable source-style and `pom.xml` conventions.
- Runtime, security, persistence, testing, and transaction rules live in the dedicated rule files for those concerns.
- Do not put volatile version notes or one-off framework gotchas here. Record those in `docs/learnings/LEARNINGS.md`.

## Java source rules

- One top-level type per file.
- No wildcard imports.
- Keep overloads contiguous.
- Use braces for `if`, `else`, `for`, `while`, and `do`, even for single-line bodies.
- Prefer switch expressions and exhaustive switches when the domain is closed (for example enums and sealed hierarchies).
- Use uppercase `L` for `long` literals.
- Keep `@SuppressWarnings` and tool-specific suppressions as narrow as possible, and explain why they are needed.
- Favor clear, direct code over clever abstractions, reflection tricks, or meta-programming.
- Composition over inheritance.

## Modeling and framework boundaries

- Prefer records for immutable data carriers whose state is the API: value objects, commands, query results, and request/response DTOs.
- Use sealed interfaces or classes only when the set of variants is intentionally closed and exhaustive handling matters.
- Constructor injection only. No field-level `@Autowired`.
- Do not field-inject `@Value`; prefer constructor arguments or dedicated configuration properties.
- `Optional` is for return types only. Never use `Optional` as a parameter, field, record component, or DTO property.
- Parse raw input into strong domain types at adapter boundaries.
- Domain records and use cases stay free of Spring and framework annotations.
- Transaction boundaries belong on Facade/Service classes; see `transactions.md`.
- No mapping frameworks. Write explicit mappings at boundaries.
- Work inside-out: domain model first, application logic second, adapters and wiring last.

## Naming

- Packages: lowercase, no underscores.
- Types: `UpperCamelCase`. Methods, fields, parameters, and locals: `lowerCamelCase`.
- Methods should read like verbs or verb phrases.
- Constants use `UPPER_SNAKE_CASE` only for deeply immutable `static final` values.
- Avoid abbreviations unless they are standard in this domain: `ID`, `URL`, `HTTP`, `JSON`, `JWT`, `UUID`, `SQL`, `API`.
- Avoid one-letter names except for very small scopes such as loop indices, short lambdas, or conventional exception variables.

Repo-specific naming:

- Public module interfaces: `*API`
- DTO classes: `*DTO`
- Use cases: verb phrase + `UseCase`
- Domain types: no technical suffix
- Test classes: end with `Test`

## Class cohesion

- A class should own one coherent responsibility.
- If a group of private methods shares no instance state and can be moved without passing `this`, extract it.
- Prefer one level of abstraction per method.
- Keep the high-level flow near the top of the class; place helpers below their callers unless overload grouping dictates otherwise.

## Lombok

- Prefer explicit Java over Lombok magic.
- Allowed annotations: `@Slf4j`, `@With`, `@Builder`.
- Disallowed annotations: `@Data`, `@Getter`, `@Setter`, `@Value`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@RequiredArgsConstructor`, and similar boilerplate-hiding shortcuts.

## Logging

- Use Lombok `@Slf4j` when a class genuinely needs logging. Never use `LoggerFactory.getLogger()` manually.
- Prefer event-based logs over boilerplate method entry/exit logs.
- Good log points: state changes at module boundaries, external calls, retries and fallbacks, scheduled/background work, security-relevant events, and unexpected conditions.
- `DEBUG`: local diagnostic detail.
- `INFO`: important lifecycle or business-significant events.
- `WARN`: recoverable or degraded behavior.
- `ERROR`: failures that require attention. Include the exception as the last argument.
- Use SLF4J `{}` placeholders. Never concatenate log messages.
- Log-level guards are unnecessary for simple placeholder logs, but are allowed when argument construction is expensive or side-effectful.
- Use a stable prefix such as `[ClassName.methodName]` on manually written log messages.
- Prefer framework observability and tracing for cross-request flow visibility instead of manual entry/exit noise.
- Never log secrets, passwords, access tokens, connection strings, key material, raw authorization headers, or PII.
- Do not log from the domain layer, records/DTOs, or tight loops unless there is a compelling operational reason.

## Null safety

This project uses JSpecify annotations enforced by NullAway at compile time.

- Every `package-info.java` must have `@org.jspecify.annotations.NullMarked`.
- Non-null is the default inside `@NullMarked` scope.
- Use `@Nullable` only where `null` is genuinely part of the contract.
- Never use `@Nullable Optional<T>`.
- Never use older nullness annotation sets such as JSR-305, JetBrains, or FindBugs/SpotBugs annotations.
- Subpackages do not inherit `@NullMarked`. Every package with Java source files needs its own `package-info.java`.
- Use `@NullUnmarked` only as a temporary escape hatch. Keep its scope minimal and document why it is needed.

## pom.xml conventions

- Prefer Spring Boot starters and BOM-managed versions.
- Do not add a `<version>` for dependencies already managed by the Spring Boot parent/BOM unless there is a documented reason.
- Override managed versions only for an unmanaged dependency, a verified security or compatibility need, or a deliberate repo-wide BOM import.
- Keep `dependencyManagement` for BOM imports and true repo-wide version control, not routine per-dependency pinning.
- Keep test-only dependencies in `test` scope.
- Remove redundant or duplicate dependencies instead of layering around them.

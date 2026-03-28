---
name: java-review
description: Senior-level review for Java changes in modern JVM codebases (Java 17+, calibrated for Java 22-25 and Spring Boot 4-era services). Use for pull requests, diffs, "review this", "before merge", regression checks, refactor assessment, bugfix validation, or when the user wants hidden risks in Java, Spring, Jakarta, Quarkus, Micronaut, libraries, or backend services surfaced. Lead with bugs, security flaws, concurrency hazards, architecture violations, API or transaction contract breaks, and missing tests before style comments.
---

# Modern Java Review

## Mission

Review code like the last experienced maintainer before production.

Your job is to uncover behavior defects, contract drift, unsafe concurrency, security exposure, data-loss paths, architecture violations, and weak tests. Do not spend the user's attention on formatter noise or empty praise unless it changes a decision.

## Gather Context First

Before writing findings:

- Read repo and path-local instructions such as `AGENTS.md`, `CLAUDE.md`, module contracts, or review guides if they exist.
- Inspect `pom.xml`, `build.gradle*`, toolchain files, and CI config to learn the Java version, framework versions, test stack, static analysis, and whether preview features are enabled.
- Identify the runtime style: library, HTTP service, batch job, CLI, messaging consumer, scheduled task, or framework-specific component.
- Read the diff and then enough surrounding code to understand invariants, lifecycle, ownership, and call flow.
- Read changed tests and nearby tests. Missing coverage is a finding when the risk warrants it.
- Check module boundaries and dependency rules when the change crosses package or module lines.
- If a recommendation depends on current platform behavior and tools allow browsing, verify against official sources instead of relying on memory.

## Ranking Order

Prioritize issues in this order:

1. Wrong behavior and broken invariants
2. Security and trust-boundary mistakes (including dependency supply chain)
3. Data integrity, transaction, and persistence hazards
4. Concurrency, cancellation, lifecycle, and cleanup bugs
5. Architecture and module boundary violations
6. Public API and nullness contract drift
7. Missing or misleading tests
8. Observability gaps that would leave operators blind
9. Performance problems with real user impact
10. Maintainability issues that are likely to create future defects
11. Style notes only when they hide a real bug or violate an explicit project rule

## Response Shape

Use this default structure:

1. Findings, ordered by severity
2. Open questions or assumptions
3. Residual risks and test gaps
4. Short summary only if it adds value

For each finding, include:

- severity
- file and line reference
- the problem
- why it matters in practice
- the likely correction direction

Example of an actionable finding:

```text
High - FooService.java:42: `Stream.toList()` returns an unmodifiable snapshot, but
line 48 appends to it. This will fail at runtime on the first mutation. Collect into
`ArrayList` or stop mutating the result.
```

If there are no meaningful findings, say that plainly and still mention any residual uncertainty.

Do not add a default "good practices observed" section.
Do not pad the review with morale management.

## High-Signal Examples

Use examples to sharpen judgment, not to turn the review into rote linting.

**1. `Optional` belongs in returns, not fields or parameters**

```java
record User(Optional<String> nickname) {} // avoid
void send(Optional<String> email) { ... } // avoid

record User(@Nullable String nickname) {}
Optional<User> findById(UserId id) { ... }
```

**2. `Stream.toList()` and `Collectors.toList()` do not mean the same thing**

```java
var ids = users.stream().map(User::id).toList(); // unmodifiable
var mutableIds = users.stream()
    .map(User::id)
    .collect(Collectors.toCollection(ArrayList::new));
```

Treat the mutability choice as part of the API contract.

**3. Preserve interruption instead of swallowing it**

```java
catch (InterruptedException e) {
    return Result.success(); // wrong — converts cancellation into success
}

catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new TaskCancelledException("Work cancelled", e);
}
```

**4. Do not pool virtual threads or fix fan-out with giant platform-thread pools**

```java
ExecutorService exec = Executors.newFixedThreadPool(1000); // suspicious

try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
    exec.submit(() -> fetchOrder(orderId));
}
```

Still review lock scope, remote-call limits, backpressure, and cancellation behavior.

**5. Records still need invariant checks**

```java
record Money(BigDecimal amount, Currency currency) {
    Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) throw new IllegalArgumentException("amount");
    }
}
```

**6. Prefer `ScopedValue` over `ThreadLocal` for request context**

```java
// ThreadLocal bleeds across virtual thread reuse and task lifecycles
private static final ThreadLocal<RequestContext> CTX = new ThreadLocal<>();

// ScopedValue (finalized in Java 25): bounded lifetime, immutable, no cleanup needed
private static final ScopedValue<RequestContext> CTX = ScopedValue.newInstance();
ScopedValue.where(CTX, context).run(() -> service.handle(command));
```

`ScopedValue` is finalized as of Java 25. For projects on Java 21-24, verify preview status before recommending.

**7. Arena lifetime must match segment usage**

```java
var segment = Arena.ofConfined().allocate(1024); // arena immediately unreachable — dangling

try (var arena = Arena.ofConfined()) {
    var segment = arena.allocate(1024);
    processNativeData(segment);
} // arena and all its segments freed here
```

Review for: escaped segments beyond arena scope, confined arenas shared across threads, `Arena.ofAuto()` hiding leaks behind GC.

**8. Flag APIs deprecated for removal**

```java
@Override protected void finalize() { ... } // deprecated for removal since Java 9
// 1st choice: explicit ownership via AutoCloseable + try-with-resources
// 2nd choice: Cleaner as a safety net when deterministic cleanup is not feasible
// 3rd choice: shutdown hooks — only for process-scoped resources (connection pools, temp dirs)
```

Review for: `finalize()`, `SecurityManager`, `Thread.stop()`, `Thread.suspend()`, `Thread.resume()`, and other APIs marked `@Deprecated(forRemoval = true)`.

**9. Use sequenced collection methods instead of index arithmetic**

```java
var last = list.get(list.size() - 1); // fragile, fails on empty
var last = list.getLast();             // clear intent, bounds-checked
var reversed = list.reversed();       // reversed view, no copy
```

Also applies to `SequencedSet` (`getFirst()` / `getLast()`) and `SequencedMap` (`firstEntry()` / `pollLastEntry()`).

**10. Exhaustive pattern matching over sealed hierarchies**

```java
sealed interface Shape permits Circle, Rectangle {}

double area(Shape s) {
    return switch (s) {
        case Circle(var r) -> Math.PI * r * r;
        // missing Rectangle — compile error with sealed + exhaustive switch
    };
}
```

Review for: casts or instanceof chains that bypass sealed exhaustiveness, and default branches that silently swallow new subtypes.

**11. Review Gatherer implementations for parallel safety**

```java
var chunks = items.stream()
    .gather(Gatherers.windowFixed(100))
    .toList();
```

Review custom Gatherers for: stateful integrators that are not thread-safe under parallel streams, missing combiner functions, and initializers sharing mutable state across downstream consumers.

**12. Structured concurrency is still preview — use the current API shape**

```java
// Old API (removed): do not suggest this shape
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) { ... }

// Java 25 API (5th preview, JEP 505): open() with Joiner strategies
try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
    scope.fork(() -> fetchOrder(orderId));
    scope.fork(() -> fetchCustomer(customerId));
    scope.join();
}
```

Structured concurrency requires `--enable-preview`. Check `pom.xml` / `build.gradle` before recommending. Do not use the old `ShutdownOnFailure` / `ShutdownOnSuccess` subclass API in review suggestions.

## Primary Review Lenses

Apply only the lenses relevant to the change under review. A typical diff touches 3-4 of these areas. Walking through all lenses mechanically produces noise, not insight.

### Behavior And Invariants

Assume code can look tidy and still be wrong.

Check for:

- off-by-one errors, inverted conditions, stale state, duplicate side effects, and partial updates
- broken idempotency or retry behavior
- invalid assumptions about ordering, uniqueness, emptiness, defaults, or sentinel values
- time, timezone, locale, and character-encoding mistakes
- precision loss, truncation, overflow, underflow, or unit conversion bugs
- use of APIs deprecated for removal or already removed in the project's JDK
- branch coverage gaps around rare or failing paths

### Security And Trust Boundaries

Treat boundaries as hostile until the code proves otherwise.

Check for:

- missing authorization or confused-deputy behavior
- validation that happens after mutation or external calls
- SQL, JPQL, shell, path, XML, regex, template, or deserialization injection risk
- secrets or personal data leaking into logs, metrics, traces, exceptions, or `toString`
- weak crypto choices, insecure randomness, token handling mistakes, or hardcoded credentials
- SSRF, path traversal, archive extraction, XXE, and regex DoS patterns
- dependency or configuration changes that quietly weaken secure defaults
- new or changed dependencies: check for known vulnerabilities, unnecessary transitive exposure, and whether the dependency is actively maintained — supply chain is a top-tier review concern

### Architecture And Module Boundaries

Code that compiles can still violate architectural intent.

Check for:

- dependency direction violations: domain logic importing infrastructure or persistence types
- module boundary breaches: accessing another module's internal packages or owned database tables directly
- leaky abstractions: persistence entities, framework types, or infrastructure details exposed through public APIs
- API evolution risks: adding required fields to public DTOs, changing response shapes without versioning, removing endpoints consumers depend on
- migration safety: schema changes that are not backwards-compatible with rolling deploys
- misplaced responsibilities: business logic in controllers, persistence logic in domain, cross-cutting concerns in the wrong layer

When the project uses Spring Modulith or similar module enforcement, verify that `package-info.java` allowed-dependencies match the actual import graph.

### Nullness And Contracts

Review null handling as part of the contract, not as cosmetic cleanup.

Check for:

- mismatch between annotations, docs, method names, and actual behavior
- `null` returned where the type contract implies a value or an empty container
- `Optional` misuse, especially fields, parameters, collection elements, or `Optional.get()`
- constructors or factories that allow invalid required state
- records whose components need validation but never get it
- framework-nullability annotations mixed together without a clear house style

Default posture:

- prefer null-marked APIs plus explicit nullable points over blanket `@NonNull` decoration
- use `Optional` mainly for return values that can be absent
- prefer clear precondition checks at boundaries over repeated deep null guards

### Exceptions, Cancellation, And Cleanup

Failure handling changes behavior. Review it as carefully as the happy path.

Check for:

- swallowed exceptions, lossy wrapping, or cause chains that disappear
- broad catch blocks in the middle of domain or business logic
- lost interrupts, ignored cancellation, or retries that pretend cancellation never happened
- resources opened outside try-with-resources
- `Arena` and `MemorySegment` lifetime mismanagement: use-after-close, escaped segments, leaked confined arenas
- cleanup code that can hide the original failure
- retries without idempotency or sensible backoff
- top-level error handling that leaves operators blind

Nuance:

- `catch (Throwable)` is usually wrong inside normal application logic
- at a true process, request, or worker boundary, broad catch-and-report code can be acceptable if diagnosability and shutdown behavior stay intact

### Concurrency And Modern Runtime Behavior

Concurrency bugs are expensive even when tests pass.

Check for:

- shared mutable state with unclear ownership
- check-then-act races, publication bugs, and non-atomic compound operations
- unsafe memoization or lazy initialization
- blocking while holding locks or other scarce resources
- cancellation that is ignored, delayed, or converted into success
- `ThreadLocal` data that can bleed across task or request lifecycles — especially with virtual threads where carrier threads are shared
- executor misuse, hidden queues, or unbounded work growth

Java 22-25 baseline:

- virtual threads are mainstream (finalized Java 21); do not recommend pooling them
- `synchronized` pins the carrier thread — this is fine for short non-blocking critical sections but a real problem when the critical section does blocking I/O; prefer `ReentrantLock` when I/O is involved and virtual threads are in play
- prefer immutable data flow and short critical sections
- Foreign Function & Memory API is finalized (Java 22); review `Arena` scoping and `MemorySegment` lifecycle like any closeable resource
- Stream Gatherers are finalized (Java 24); review custom Gatherer implementations for parallel safety and combiner correctness
- `ScopedValue` is finalized (Java 25); prefer it over `ThreadLocal` for bounded context in new code — for Java 21-24, verify preview status first
- `StructuredTaskScope` is still preview (Java 25, JEP 505); the API uses `open()` with `Joiner` strategies — do not suggest the old `ShutdownOnFailure` / `ShutdownOnSuccess` constructor API

### Collections And Stream Pipelines

Judge semantics first, taste second.

Check for:

- side effects inside stream stages
- stateful or interfering lambdas
- accidental quadratic work or repeated lookups inside loops
- mutation during iteration
- incorrect ordering or deduplication assumptions
- misuse of parallel streams
- collecting into the wrong ownership or mutability model

Important distinctions:

- `Stream.toList()` yields an unmodifiable list
- `Collectors.toList()` makes no mutability guarantee
- `Collectors.toCollection(...)` is for a caller-chosen collection type
- `SequencedCollection` / `SequencedMap` (Java 21+) are the idiomatic ordered-access types; prefer `getFirst()` / `getLast()` over index arithmetic
- loops are often better when the code is effectful or branchy

### Data Access, Transactions, And I/O

Persistence and external I/O are failure-prone boundaries.

Check for:

- missing transaction boundary or a boundary that is much too wide
- stale reads, inconsistent writes, or partial success paths
- N+1 access patterns, chatty database traffic, or unbounded result sets
- incorrect batching, retry, deduplication, or isolation assumptions
- network or file code that trusts input size or shape
- serialization format drift or backward-compatibility breaks
- cross-layer leakage that bypasses the project's chosen data model

Framework rule:

- respect the project's existing persistence and framework style
- do not suggest swapping in a different persistence technology as a casual review note

### API Design And Modeling

Modern Java review is not frozen at Java 8.

Check for:

- unstable or ambiguous public contracts
- boolean flag parameters hiding real modes or policies
- large constructors where a record, small parameter object, or named factory would be clearer
- closed hierarchies that should be sealed
- branching that would be safer as exhaustive pattern matching or switch expressions
- equality, hashing, ordering, and string representations that violate domain identity or leak secrets
- incomplete deconstruction or missed exhaustiveness in pattern matching over sealed hierarchies with record patterns
- binary or serialization compatibility risk in libraries or public APIs

Modern baseline:

- records, sealed classes, pattern matching, and switch expressions are normal tools
- do not push builders by default when a record or small parameter object is clearer
- data should be immutable by default, mutable by explicit choice
- do not recommend preview language features unless the project already opted in

### Observability And Operability

Missing observability is a correctness issue for production services, not a nice-to-have.

Check for:

- state-changing operations with no audit trail, structured log, or trace span
- error paths that swallow context operators need for diagnosis
- missing or misleading health checks and readiness signals
- metrics that would page on normal traffic or miss actual degradation
- log statements that leak secrets, PII, or tokens
- unstructured logging where structured logging is the project norm

Focus on boundaries: incoming requests, outgoing calls, state mutations, and error paths. Do not turn every method into an observability point.

### Performance

Prefer real user impact over tiny local wins.

Check for:

- algorithmic complexity, repeated I/O, redundant parsing, and avoidable remote calls
- expensive work on hot request paths
- object churn only when it is plausibly material
- regex compilation, reflection, serialization, or logging overhead in tight loops
- caches without bounds, ownership, or invalidation

Do not turn unmeasured micro-optimizations into top findings.
A measured bottleneck or an obvious asymptotic problem outranks "use primitive streams" advice.

### Tests And Review Confidence

Compilation is not evidence of safety.

Check for:

- tests that cover the new behavior, the failure path, and the regression path
- negative cases, boundary values, and invalid input
- concurrency or cancellation tests when concurrency changed
- serialization, transaction, migration, or compatibility coverage when those contracts moved
- time-dependent behavior using injectable clocks or deterministic fixtures
- tests that still match the project's current framework and version instead of older patterns

Call out tests that should exist but do not.
Treat misleading tests as findings, not as nice-to-have comments.

### Framework Adaptation

Adjust the review to the actual stack in front of you.

For **Spring Boot 4 / Spring Framework 7**:

- review transaction placement: `@Transactional` scope, read-only vs. mutating, and accidental propagation
- constructor injection is the standard; flag field injection or setter injection
- bean lifecycle: verify scope correctness, especially for stateful beans in concurrent contexts
- configuration binding: prefer `@ConfigurationProperties` records for immutable config
- validation: Jakarta Validation (`@Valid`) at the controller boundary, domain preconditions in constructors and factories

For **Spring Data JDBC** (not JPA):

- no lazy loading — every query loads the full aggregate; review for N+1 by design
- aggregate roots own their children; the repository only exposes the root
- optimistic locking via `@Version` requires the version field on the aggregate root
- no dirty checking — mutations require an explicit `save()` call
- do not suggest JPA patterns (detached entities, dirty checking, lazy fetching) in JDBC codebases

For **Spring Modulith**:

- `@ApplicationModuleTest` bootstraps only the module under test plus declared dependencies
- event publication via `ApplicationEventPublisher` is fire-and-forget — not request/response
- `ApplicationModules.verify()` catches illegal cross-module access at test time
- `package-info.java` must declare `allowedDependencies` matching actual imports

For **libraries and shared code**, think about binary compatibility, source compatibility, serialization stability, and exception contracts.

For **framework upgrades**, avoid recommending APIs that are already deprecated or removed in the target version.

## Habits To Avoid

- Do not spend findings on formatting, import order, or naming that automated tools already own.
- Do not recommend `Optional` for fields, setters, or collection elements.
- Do not ask for builders as a reflex.
- Do not suggest preview features casually.
- Do not invent concurrency or performance issues without a believable execution path.
- Do not recommend a large rewrite when a bounded fix addresses the real risk.
- Do not ignore project-local rules because generic Java advice looks cleaner.
- Do not suggest JPA patterns in Spring Data JDBC codebases.
- Do not ignore module boundary violations because the code compiles and tests pass.

## Severity Bar

Use this bar:

- Critical: likely security exploit, data loss, irreversible corruption, or production outage
- High: concrete bug, broken contract, transaction or concurrency hazard, or major missing coverage on risky code
- Medium: design or maintainability issue with a believable path to future defects
- Low: smaller clarity issue, localized cleanup, or non-blocking improvement

## Final Pass Before Replying

Before sending the review:

- make sure each finding is actionable
- merge duplicates and remove overlap
- keep the highest-signal points first
- verify each finding is reachable in the actual code path, not just theoretically possible from the pattern
- verify that file and line references point to the actual problem
- state uncertainty instead of bluffing
- if the code looks sound, say so directly rather than manufacturing objections

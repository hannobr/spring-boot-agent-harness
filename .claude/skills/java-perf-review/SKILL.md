---
name: java-perf-review
description: Performance-focused review for Java 25+ / Spring Boot 4+ services. Use when the user asks to review performance, find bottlenecks, optimize throughput, reduce latency, tune the JVM, check virtual thread usage, review database access patterns, or audit startup/memory behavior. Also trigger when the user says "why is this slow", "performance review", "perf audit", "optimize", "bottleneck", or wants to understand resource usage in a Java/Spring codebase. This complements java-review (which covers correctness) by going deep on runtime efficiency.
---

# Java 25 + Spring Boot 4 Performance Review

## Mission

Find performance issues that matter in production — bottlenecks that waste latency, throughput, memory, or money. Ignore micro-optimizations that don't move the needle. Focus on patterns that cause measurable user-facing or cost impact.

This skill is calibrated for **Java 25 LTS** and **Spring Boot 4.x / Spring Framework 7** running on modern JVMs with virtual threads. Many patterns apply broadly, but JVM flags and framework advice target this stack specifically.

## Gather Context First

Before writing findings:

- Read `CLAUDE.md`, module contracts, and any performance-related docs in the repo.
- Check `pom.xml` / `build.gradle` for: Java version, Spring Boot version, connection pool library, caching dependencies, observability stack (Micrometer, OpenTelemetry), and whether AOT/native-image is configured.
- Identify the workload profile: I/O-bound service, CPU-intensive processor, batch job, or mixed.
- Check `application.yaml` / `application.properties` for: thread configuration, connection pool settings, cache TTLs, and JVM-related properties.
- Look for existing performance tests, benchmarks, or load test configurations.
- Read the code under review with an eye for hot paths — request handlers, event processors, scheduled jobs, and startup routines.

## Review Categories

Apply only categories relevant to the code under review. A typical review touches 3-5 of these. Walking through all of them mechanically produces noise.

### 1. Virtual Threads — Use Them Right

Java 25 and Spring Boot 4 make virtual threads the default for request handling. They're transformative for I/O-bound workloads but come with real pitfalls.

**Check for:**

- **Carrier thread pinning**: `synchronized` blocks that contain blocking I/O (network calls, JDBC, file I/O) pin the virtual thread to a carrier, destroying scalability. Use `ReentrantLock` instead when the critical section does I/O. Short, non-blocking `synchronized` sections (incrementing a counter, swapping a reference) are fine.

- **Pooling virtual threads**: Never pool virtual threads in a fixed-size executor. They're cheap — create one per task with `Executors.newVirtualThreadPerTaskExecutor()`. A `newFixedThreadPool(N)` filled with virtual-thread work is a red flag.

- **Missing backpressure on downstream resources**: Virtual threads make concurrency nearly free, but your database, HTTP APIs, and message brokers don't scale the same way. Without a `Semaphore`, connection pool limit, or rate limiter, you'll overwhelm backends. The connection pool is your concurrency limiter now, not the thread pool.

- **ThreadLocal abuse**: Each virtual thread gets its own ThreadLocal copy. With millions of virtual threads, ThreadLocal-heavy code causes memory explosion. Prefer `ScopedValue` (finalized in Java 25) for request-scoped context.

- **CPU-bound work on virtual threads**: Virtual threads don't help CPU-intensive work — they share carrier threads (typically = CPU cores). CPU-bound tasks should use platform thread pools sized to core count.

- **Blocking inside reactive pipelines**: Mixing virtual threads with reactive (WebFlux/R2DBC) pipelines is worse than either alone. Pick one model per call chain.

**Example — pinning fix:**
```java
// BAD: synchronized + blocking I/O pins the carrier thread
synchronized (lock) {
    var result = httpClient.send(request, BodyHandlers.ofString()); // pinned!
}

// GOOD: ReentrantLock allows unmounting during the blocking call
lock.lock();
try {
    var result = httpClient.send(request, BodyHandlers.ofString());
} finally {
    lock.unlock();
}
```

**Detecting pinning at runtime:** Use `-Djdk.tracePinnedThreads=short` during testing or JFR event `jdk.VirtualThreadPinned` in production.

### 2. Database and Persistence

Database access is the #1 source of latency in most Spring services. Review it carefully.

**Check for:**

- **N+1 query patterns**: Especially critical with Spring Data JDBC — there's no lazy loading. Every association load is explicit. Loading a list of parents and then querying children per-parent in a loop is O(N) queries. Use a single query with a JOIN or an `IN` clause.

- **Missing connection pool tuning**: HikariCP defaults (`maximumPoolSize=10`) are often too low for virtual-thread workloads where many requests block concurrently on DB. But setting it too high saturates the database. Rule of thumb: start at `2 * CPU cores + 1` for the database server, measure, and adjust. Set `connectionTimeout` to fail fast (2-5s) rather than queue indefinitely.

- **Unbounded result sets**: Queries without `LIMIT` or pagination that return thousands of rows. Look for `findAll()` calls on tables that grow.

- **Transaction scope too wide**: `@Transactional` wrapping entire request handlers holds a connection for the full request duration, including LLM calls or HTTP client calls. Narrow transactions to the actual database work.

- **Missing indices**: Look for `WHERE` clauses, `ORDER BY`, and `JOIN` conditions on columns that aren't indexed. Check Flyway migrations for index definitions.

- **Batch insert/update missing**: Inserting rows in a loop instead of using batch operations. Spring Data JDBC supports `saveAll()` for batch inserts.

- **Connection leak patterns**: Try-with-resources not used for manual connection or statement handling. Connections acquired in `@PostConstruct` or static initializers.

**Example — transaction scope:**
```java
// BAD: holds DB connection during external HTTP call
@Transactional
public Dossier assembleDossier(UUID conversationId) {
    var facts = repo.findFacts(conversationId);    // DB
    var analysis = llmClient.analyze(facts);        // 10+ second LLM call — connection held!
    return repo.save(new Dossier(analysis));         // DB
}

// GOOD: narrow transaction scope
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public Dossier assembleDossier(UUID conversationId) {
    var facts = readFacts(conversationId);           // own transaction
    var analysis = llmClient.analyze(facts);         // no connection held
    return saveDossier(analysis);                    // own transaction
}
```

### 3. JVM and GC Configuration

Java 25 ships significant performance features. Make sure they're enabled.

**Check for:**

- **GC selection**: Generational ZGC (`-XX:+UseZGC`) is the recommended GC for Java 25 services. It offers sub-millisecond pause times with generational collection. G1 is still fine for most workloads. Serial GC in containers is a red flag unless it's a tiny single-threaded batch job.

- **Compact Object Headers**: `-XX:+UseCompactObjectHeaders` (product feature in Java 25) shrinks object headers from 12-16 bytes to 8 bytes, reducing heap usage by ~10-20%. No downside for most workloads. Should be on by default for new services.

- **Heap sizing in containers**: Check for `-Xmx` / `-Xms` set appropriately for the container memory limit. Leave headroom for off-heap (thread stacks, native memory, direct buffers). A good starting point: `-Xmx` = 75% of container memory. For ZGC, provision 25% headroom above live data set.

- **Missing CDS (Class Data Sharing)**: CDS archives reduce startup time by 20-40% by pre-loading class metadata. Spring Boot 4 has built-in CDS support (`-Dspring.context.checkpoint=onRefresh` for CRaC, or build-time CDS with `ProcessAotAhead`). Check if CDS is configured for containerized deployments.

- **AOT compilation**: Spring Boot 4 supports AOT processing that pre-computes bean definitions at build time. Check if `-Dspring.aot.enabled=true` is used in production profiles. Combined with CDS, it significantly reduces startup.

- **Container CPU awareness**: The JVM auto-detects container CPU limits since Java 10. But check for explicit `-XX:ActiveProcessorCount` overrides that might be wrong.

### 4. HTTP and Network

**Check for:**

- **Blocking HTTP clients on the request thread**: Using `RestTemplate` synchronously in a virtual-thread context is fine (it will unmount). But check that connection pools for HTTP clients are appropriately sized. `RestClient` (Spring Framework 7) is the modern choice.

- **Missing connection reuse**: Creating a new `HttpClient` or `RestClient` per request instead of sharing an instance. Connection setup is expensive (TCP + TLS handshake).

- **No timeouts configured**: HTTP clients without `connectTimeout` and `readTimeout` will hang indefinitely on unresponsive backends. This is a production availability risk, not just a performance issue.

- **Unbounded response bodies**: Reading external API responses without size limits. A malicious or buggy upstream can OOM your service.

- **Missing compression**: `Accept-Encoding: gzip` not set on outgoing requests, or `server.compression.enabled=true` not set for responses to clients.

- **Sequential external calls that could be parallel**: Multiple independent HTTP calls made sequentially when they could use virtual threads to parallelize.

**Example — parallelizing independent calls:**
```java
// BAD: sequential, 3x latency
var orders = orderClient.fetch(userId);      // 200ms
var profile = profileClient.fetch(userId);    // 150ms
var prefs = prefsClient.fetch(userId);        // 100ms
// total: ~450ms

// GOOD: parallel with virtual threads, 1x max latency
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var ordersFuture = executor.submit(() -> orderClient.fetch(userId));
    var profileFuture = executor.submit(() -> profileClient.fetch(userId));
    var prefsFuture = executor.submit(() -> prefsClient.fetch(userId));
    var orders = ordersFuture.get();    // ~200ms total for all three
    var profile = profileFuture.get();
    var prefs = prefsFuture.get();
}
```

### 5. Startup Time

Startup matters for scaling, deployments, and developer feedback loops.

**Check for:**

- **Eager initialization of expensive resources**: Loading ML models, pre-warming caches, or establishing all connections at startup when they could be lazy. Use `@Lazy` for beans that aren't needed immediately.

- **Classpath scanning scope too wide**: `@SpringBootApplication` scanning the entire package tree including test utilities or unused modules. Narrow the scan with `scanBasePackages`.

- **Missing Spring AOT and CDS**: For containerized services, the combination of AOT processing + CDS archive + AppCDS can cut startup from 8-12s to 2-4s (JVM) or sub-200ms (native image).

- **Flyway migrations on startup in production**: Consider separating schema migration from application startup for large migration sets.

- **Component scanning with heavy `@PostConstruct`**: Beans that do network calls, file I/O, or heavy computation in `@PostConstruct` block startup.

### 6. Memory and Object Allocation

**Check for:**

- **String concatenation in loops**: Using `+` or `String.format()` in tight loops instead of `StringBuilder`. Modern JVMs optimize simple concatenation via `invokedynamic`, but repeated concatenation in loops still creates garbage.

- **Autoboxing in hot paths**: `Map<String, Integer>` forcing boxing/unboxing millions of times. Consider primitive-specialized collections for hot paths.

- **Large object graphs kept alive**: Caches without size bounds or TTL, static collections that grow unbounded, event listeners that are never deregistered.

- **Excessive DTO copying**: Creating intermediate DTOs at every layer boundary when a direct mapping would suffice. Records help here — they're lightweight, but unnecessary copies still cost.

- **Missing `@JsonCreator` / immutable deserialization**: Jackson creating objects via reflection + setters when direct constructor binding is available. With records, Jackson 3 does this automatically.

- **Compact Object Headers not enabled**: With Java 25, `-XX:+UseCompactObjectHeaders` saves ~4 bytes per object. On heaps with millions of small objects, this is material.

### 7. Caching

**Check for:**

- **Missing caching on repeated expensive operations**: Database queries or API calls that return the same result for the same input within a time window.

- **Unbounded caches**: `Map`-based caches without eviction. Use Caffeine with `maximumSize` and `expireAfterWrite`.

- **Cache invalidation bugs**: Caching mutable objects (modifications to the cached reference affect all readers). Cache immutable records or defensive copies.

- **Over-caching**: Caching results that are cheap to compute or rarely reused. Cache overhead (memory, invalidation complexity) can exceed the savings.

- **Missing cache metrics**: No visibility into hit rates, eviction counts, or cache size. Without metrics, you can't tell if the cache is helping or hurting.

### 8. Async and Event Processing

**Check for:**

- **Fire-and-forget without error handling**: Virtual threads or `@Async` tasks that swallow exceptions silently. Failures disappear without logging or metrics.

- **Unbounded work queues**: Event listeners or async processors that accept work without backpressure. Under load, the queue grows until OOM.

- **Sequential event processing that could be parallel**: Processing a batch of events one by one when they're independent.

- **Missing dead letter / retry strategy**: Events that fail processing are lost permanently.

- **Blocking the event publisher**: Event handlers that do heavy work synchronously on the publisher's thread, blocking the caller.

### 9. Observability for Performance

**Check for:**

- **No request duration metrics**: Missing Micrometer timers on critical endpoints. Without them, you can't detect degradation.

- **Missing database query timing**: No instrumentation on repository calls. Slow queries hide behind aggregate request latency.

- **No JVM metrics exposed**: GC pause time, heap usage, thread counts, and connection pool stats not exported to monitoring.

- **Missing tracing spans on external calls**: Distributed traces that skip HTTP client calls, database queries, or message sends — making it impossible to attribute latency.

- **Log overhead in hot paths**: `log.debug()` with expensive argument construction that runs even when debug is disabled. Use `log.atDebug().log(...)` or guard with `log.isDebugEnabled()`.

## Response Shape

Structure your review as:

1. **Executive summary**: One paragraph — what's the overall performance posture? Is this code likely to perform well under expected load, or are there structural issues?

2. **Findings**, ordered by impact:
   - **Impact level**: Critical / High / Medium / Low
   - **Category**: (e.g., Virtual Threads, Database, JVM Config)
   - **File and line reference**
   - **The problem**: What's happening and why it's slow/wasteful
   - **Measured or estimated impact**: How much latency/memory/throughput this costs (even a rough estimate helps prioritize)
   - **Recommended fix**: Concrete code change or configuration adjustment

3. **Quick wins**: Changes that are easy to implement and have clear payoff

4. **Architecture observations**: Structural patterns that limit performance at scale (these are harder to fix but important to flag)

5. **Measurement recommendations**: What to instrument or benchmark to validate the findings

**Example finding:**
```
High - Database - DossierAssembler.java:92-97
Two independent LLM calls (generatePlainLanguageAnswer + generatePracticalNextSteps)
run sequentially, each taking 5-10s. Since they share the same inputs and are
independent, they can run in parallel with virtual threads.
Estimated impact: saves 5-10s per dossier assembly (~50% of LLM time).
Fix: wrap both calls in Executors.newVirtualThreadPerTaskExecutor() and Future.get().
```

## Severity Bar

- **Critical**: Production outage risk — connection pool exhaustion, OOM, thread starvation, carrier pinning under load
- **High**: Measurable latency or throughput problem — N+1 queries, sequential calls that should be parallel, missing indices, wrong GC
- **Medium**: Suboptimal but functional — missing caches, oversized transactions, untuned pool sizes, missing compression
- **Low**: Minor inefficiency or missing best practice — startup optimization, compact headers not enabled, minor allocation patterns

## What NOT to Flag

- Micro-optimizations without evidence of impact (final on local variables, stream vs loop for small collections)
- Framework internals you can't change (Spring's own allocation patterns)
- Theoretical performance issues without a plausible hot path
- Style preferences disguised as performance advice
- Recommending reactive/WebFlux when the codebase uses virtual threads — they're different models, not better/worse for I/O-bound work

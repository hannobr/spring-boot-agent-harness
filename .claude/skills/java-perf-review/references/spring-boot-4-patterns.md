# Spring Boot 4 Performance Patterns

Read this reference when reviewing Spring Boot 4 / Spring Framework 7 specific performance patterns.

## Table of Contents
1. Virtual Threads Configuration
2. Connection Pool Tuning
3. HTTP Client Performance
4. Transaction Patterns
5. Caching Patterns
6. Startup Optimization
7. Observability Setup

## 1. Virtual Threads Configuration

Spring Boot 4 enables virtual threads by default when on Java 21+:

```yaml
spring:
  threads:
    virtual:
      enabled: true  # default in Spring Boot 4
```

This affects:
- Tomcat/Jetty request handling threads
- `@Async` task execution
- `@Scheduled` task execution
- Spring MVC async request processing

**What it does NOT affect:**
- Your own `ExecutorService` instances — you must create them as virtual-thread executors yourself
- Database connection pools — HikariCP still uses platform threads internally
- External HTTP client connection pools

## 2. Connection Pool Tuning (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # start at 2 * DB CPU cores + 1
      minimum-idle: 5              # keep warm connections ready
      connection-timeout: 3000     # fail fast: 3 seconds
      idle-timeout: 600000         # 10 minutes
      max-lifetime: 1800000        # 30 minutes (below DB wait_timeout)
      leak-detection-threshold: 30000  # 30s — detect connection leaks
```

**Virtual threads interaction**: With virtual threads, thousands of requests may arrive concurrently, all competing for `maximum-pool-size` connections. The pool becomes your concurrency limiter. If `connection-timeout` is too high, virtual threads pile up waiting; if `maximum-pool-size` is too high, the database drowns.

**Sizing formula**:
- `maximum-pool-size` = connections your database can handle / number of service instances
- Start conservative, measure under load, increase if connection wait time is high but DB CPU is low

## 3. HTTP Client Performance

**RestClient (Spring Framework 7 — preferred)**:
```java
@Bean
RestClient restClient(RestClient.Builder builder) {
    return builder
        .baseUrl("https://api.example.com")
        .requestFactory(clientHttpRequestFactory())
        .build();
}

@Bean
ClientHttpRequestFactory clientHttpRequestFactory() {
    var factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(Duration.ofSeconds(10));
    return factory;
}
```

**Key patterns:**
- Share `RestClient` instances (they're thread-safe)
- Always set timeouts — both connect and read
- With virtual threads, synchronous `RestClient` is fine — no need for `WebClient`
- For parallel calls to different services, use virtual thread executor

**Anti-pattern — creating client per request:**
```java
// BAD: TCP + TLS handshake per request
var client = RestClient.create();
var result = client.get().uri(url).retrieve().body(String.class);

// GOOD: shared client with connection reuse
private final RestClient sharedClient; // injected
var result = sharedClient.get().uri(url).retrieve().body(String.class);
```

## 4. Transaction Patterns

**Narrow your transactions:**
```java
// BAD: entire method is transactional, holds connection during external call
@Transactional
public Order processOrder(OrderRequest req) {
    var order = orderRepo.save(toOrder(req));
    notificationService.sendEmail(order);  // HTTP call — connection held!
    return order;
}

// GOOD: transaction only covers DB work
public Order processOrder(OrderRequest req) {
    var order = saveOrder(toOrder(req));    // @Transactional method
    notificationService.sendEmail(order);   // no connection held
    return order;
}

@Transactional
Order saveOrder(Order order) {
    return orderRepo.save(order);
}
```

**Read-only transactions:**
```java
@Transactional(readOnly = true)  // enables DB-level optimizations
public List<Order> findRecentOrders(UUID userId) {
    return orderRepo.findByUserId(userId);
}
```

**Propagation for orchestrators:**
```java
// For methods that coordinate multiple services (some DB, some external)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public Dossier assembleDossier(UUID conversationId) {
    // Each step manages its own transaction
}
```

## 5. Caching Patterns

**Caffeine cache (preferred for local caching):**
```java
@Bean
CacheManager cacheManager() {
    var caffeine = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .recordStats();  // enable metrics
    return new CaffeineCacheManager("classifications") {
        @Override
        protected Cache<Object, Object> createNativeCaffeineCache(String name) {
            return caffeine.build();
        }
    };
}

// Usage
@Cacheable("classifications")
public UitspraakClassification classifyUitspraak(String ecli) {
    return llmClient.classify(ecli);  // expensive LLM call, cached
}
```

**Cache metrics with Micrometer:**
```java
@Bean
CacheMetricsRegistrar cacheMetrics(MeterRegistry registry, CacheManager mgr) {
    // Exposes cache.gets, cache.puts, cache.evictions, cache.size
    return new CacheMetricsRegistrar(registry, mgr);
}
```

## 6. Startup Optimization

**Priority order (most to least impact):**

1. **Spring AOT** — pre-computes bean definitions at build time
   ```xml
   <plugin>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-maven-plugin</artifactId>
       <executions>
           <execution>
               <id>process-aot</id>
               <goals><goal>process-aot</goal></goals>
           </execution>
       </executions>
   </plugin>
   ```

2. **CDS archive** — class data sharing for faster class loading
   ```bash
   # Build and extract CDS training run
   java -Dspring.context.exit=onRefresh -XX:DumpLoadedClassList=app.classlist -jar app.jar
   java -Xshare:dump -XX:SharedClassListFile=app.classlist -XX:SharedArchiveFile=app.jsa -jar app.jar
   # Run with CDS
   java -Xshare:on -XX:SharedArchiveFile=app.jsa -jar app.jar
   ```

3. **Lazy bean initialization** (development only — not recommended for production)
   ```yaml
   spring:
     main:
       lazy-initialization: true  # dev only!
   ```

4. **GraalVM Native Image** — sub-200ms startup, 50% memory reduction
   ```bash
   ./mvnw -Pnative native:compile
   ```

## 7. Observability Setup for Performance

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true  # p50, p95, p99 latencies
    enable:
      jvm: true
      process: true
      hikaricp: true
```

**Key metrics to monitor:**
- `http.server.requests` — request latency by endpoint
- `hikaricp.connections.active` — active DB connections
- `hikaricp.connections.pending` — threads waiting for a connection
- `jvm.gc.pause` — GC pause duration (should be sub-ms with ZGC)
- `jvm.memory.used` — heap and non-heap memory
- `jvm.threads.live` — platform thread count (should be stable)

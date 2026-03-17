---
paths:
  - "src/test/**"
---

# Testing rules

3-tier test pyramid: ~70% unit, ~20% slice, ~10% integration.

## Spring Boot 4 APIs -- use these, not the deprecated versions

| Use | Not (deprecated) |
|-----|------------------|
| `@MockitoBean` | ~~`@MockBean`~~ |
| `@MockitoSpyBean` | ~~`@SpyBean`~~ |
| `MockMvcTester` | ~~raw `MockMvc`~~ |
| `RestTestClient` | ~~`TestRestTemplate`~~ |

### Import paths (Spring Boot 4.0.3+)

```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.client.RestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
```

## Tier 1: Unit tests (domain + application)

No Spring context. No containers. Fastest tests.

- **Domain**: test business rules, invariants, state transitions. No mocks of domain objects.
- **Application (use cases)**: prefer in-memory fakes for port interfaces when the use case has real logic (branching, validation, multi-step flows). Fakes test behavior — save actually stores, find retrieves what was saved. Mockito is acceptable for simple delegation use cases where a fake adds no value.

```java
// In-memory fake — use for use cases with business logic
class InMemoryOrderPersistence implements OrderPersistencePort {
    private final Map<Long, Order> store = new HashMap<>();
    // implement save, findById, etc. — behaves like a real store
}

// Mockito mock — acceptable for simple pass-through use cases
var port = mock(OrderPersistencePort.class);
when(port.save(any())).thenReturn(expectedOrder);
```

### When to use fakes vs mocks

| Use case complexity | Recommended | Why |
|---|---|---|
| Has branching, validation, or multi-step logic | In-memory fake | Fake catches logic bugs that mocks miss |
| Simple delegation (one-liner) | Either — Mockito is fine | Both test the same thing; mock is less code |
| Calls multiple port methods that must be consistent | In-memory fake | Fake ensures save → find returns the saved value |

## Tier 2: Slice tests (one adapter layer)

### Persistence: `@DataJdbcTest` + Testcontainers

```java
@DataJdbcTest
@Import({OrderRepositoryAdapter.class, TestcontainersConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
```

- Uses real PostgreSQL via Testcontainers — tests against the same database engine as production.
- `@AutoConfigureTestDatabase(replace = NONE)` prevents Spring from replacing the datasource with an embedded DB.
- Flyway migrations run automatically against the container, so schema is always up to date.
- Test round-trip persistence, custom queries, edge cases.

### REST controller: `@WebMvcTest`

```java
@WebMvcTest(OrderController.class)
class OrderControllerSliceTest {
    @Autowired MockMvcTester mvc;
    @MockitoBean CreateOrderUseCase createOrderUseCase;
}
```

- `@WebMvcTest` is a slice test, NOT an integration test.
- Use `@MockitoBean` to mock use case dependencies.
- Test: HTTP status codes, response body, request validation, error responses (RFC 9457).
- Do NOT test business logic here.
- No Testcontainers needed — no database involved.

## Tier 2.5: Module tests (`@ApplicationModuleTest`)

```java
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class PaymentModuleTest {
    @Autowired private PaymentAPI paymentAPI;
}
```

- Tests module wiring through the public API interface — not REST endpoints.
- Defaults to `BootstrapMode.STANDALONE`: only the module under test is loaded. Use `@ApplicationModuleTest(BootstrapMode.DIRECT_DEPENDENCIES)` when the module depends on other modules.
- Needs `@Import(TestcontainersConfiguration.class)` for DB access.
- Every module must have at least one `@ApplicationModuleTest` (ADR-001).
- Covers: create/find/update through public API, exception paths, module wiring correctness.

## Tier 3: Integration tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
```

- Real HTTP with `RestTestClient` against PostgreSQL via Testcontainers.
- Minimal: happy-path per endpoint group, key error paths, cross-cutting concerns.
- Do NOT duplicate what slice tests already cover.

## Testcontainers best practices

- **Shared configuration**: Use a single `TestcontainersConfiguration` class with `@TestConfiguration(proxyBeanMethods = false)` and `@Bean @ServiceConnection PostgreSQLContainer<?>`.
- **Explicit dependency**: Each test class that needs a database uses `@Import(TestcontainersConfiguration.class)` — making the container dependency visible.
- **Container lifecycle**: Spring Boot's `@ServiceConnection` manages container startup/shutdown. No manual `@Container`/`@Testcontainers` annotations needed.
- **Tests only**: Testcontainers is used exclusively for tests. Local development uses `docker compose up -d` + `./mvnw spring-boot:run`.
- **Docker required**: Docker must be running for `./mvnw test` and `./mvnw verify`. Unit tests (tier 1) are container-free.

## Test fixtures

One fixture class per aggregate root. Static factory methods, deterministic values, placed in `src/test/java` same package as domain model.

## New production class -> required tests

- New domain model: unit tests (creation, state transitions, validation)
- New use case: unit tests (fakes for complex logic, mocks acceptable for simple delegation)
- New repository adapter: `@DataJdbcTest` slice test with Testcontainers
- New controller: `@WebMvcTest` slice test
- New endpoint group: `@SpringBootTest(RANDOM_PORT)` integration test with Testcontainers

## JaCoCo coverage gaps

When JaCoCo reports insufficient coverage, NEVER optimize for the metric. Do NOT look for the "easiest" way to bump the number (e.g. covering trivial getters, adding meaningless assertions, or targeting low-hanging fruit just because it moves the percentage).

Instead, follow this process:

1. **Identify untested behavior** — read the production code and ask: which business rules, error paths, edge cases, and state transitions have no corresponding test?
2. **Prioritize by risk** — rank the gaps by: (a) likelihood of bugs, (b) blast radius if broken, (c) complexity of the logic. High-risk untested code comes first.
3. **Write the most important missing tests first** — these are the tests you would want even if there were no coverage tool. Follow the test pyramid: prefer unit tests for domain/application logic, slice tests for adapters.
4. **Let coverage rise as a side effect** — the number improves because meaningful tests were added, not because the metric was gamed.

Coverage is a lagging indicator of test quality, not a target to be gamed.

## What NOT to do

- No `@SpringBootTest` for domain/application tests.
- No `@MockBean`/`@SpyBean` (deprecated). No `TestRestTemplate` (deprecated). No raw `MockMvc` (prefer `MockMvcTester`).
- No mocking domain objects. Test real behavior.
- No duplicating slice test coverage in integration tests.
- No embedded H2 for testing — use Testcontainers with PostgreSQL for production parity.

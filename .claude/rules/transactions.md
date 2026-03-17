---
paths:
  - "src/**/internal/*Facade*.java"
  - "src/**/*Service*.java"
---

# Transaction boundaries

## Where @Transactional goes

Every public method on a Facade or Service class (the module API implementation) must have `@Transactional`:

- **Write methods**: `@Transactional`
- **Read methods**: `@Transactional(readOnly = true)`

Import: `org.springframework.transaction.annotation.Transactional` (not `jakarta.transaction.Transactional`).

## Why NOT on use cases

This template keeps use cases as plain Java objects for framework-free testability. They are instantiated with `new` inside `@Configuration` classes:

```java
// OrderModuleConfiguration.java
var createUseCase = new CreateOrderUseCase(persistence, inventoryAPI);
return new OrderFacade(createUseCase, findUseCase);  // ← Spring bean
```

`@Transactional` relies on Spring AOP proxies. Spring only creates proxies for beans it manages. Since use cases are created with `new`, any `@Transactional` on them is **silently ignored** — no error, no warning, no transaction.

The Facade/Service is returned from a `@Bean` method, so Spring wraps it in a proxy and `@Transactional` works.

This is a design tradeoff. An equally valid approach is making use cases Spring beans (`@Component`) and putting `@Transactional` directly on them. This template chose framework-free use cases, so the Facade carries the transaction boundary.

## Transaction propagation

Spring's default propagation is `REQUIRED`: nested calls join the outer transaction. For example, if `OrderFacade.create()` calls `InventoryAPI.reserve()`, the reservation joins the order's transaction. If the order save fails after the reservation, both roll back.

## Pitfalls

- **Self-invocation bypass**: calling another method on `this` within the same class skips the proxy. Each public method needs its own `@Transactional` annotation.
- **Only RuntimeExceptions roll back** by default. This codebase only throws runtime exceptions, so the default is correct.
- **readOnly = true** is an optimization hint to the JDBC driver and PostgreSQL (enables read-only transaction mode), not enforcement.

## Enforcement

The `moduleApiImplementationsMustHaveTransactionalMethods` ArchUnit rule in `ArchitectureRulesTest` enforces that every public method on a module API implementation has `@Transactional`. The build fails if a method is missing the annotation.

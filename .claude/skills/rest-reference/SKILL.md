---
name: rest-reference
description: "Reference material for REST API patterns that are needed infrequently: HTTP method/status semantics, caching, conditional requests, idempotency, rate limiting, and API versioning/deprecation. Use when implementing these specific features — not needed for routine controller work."
---

# REST reference patterns

On-demand reference for patterns not needed in every controller. The core rules live in `.claude/rules/rest.md` — read those first. This skill covers the deeper details for specific topics.

## How to use

Read only the reference file relevant to the current task:

| Topic | Reference | When needed |
|-------|-----------|-------------|
| HTTP methods, status codes, validation, content negotiation, problem details | `references/http-semantics.md` | Building any endpoint beyond trivial CRUD |
| Caching, freshness, conditional requests, optimistic concurrency | `references/caching-concurrency.md` | Adding cache headers or `ETag`-based concurrency |
| Idempotency keys and retry safety | `references/idempotency.md` | Non-idempotent `POST`/`PATCH` that clients may retry |
| Rate limiting headers | `references/rate-limiting.md` | Implementing request throttling |
| API versioning, deprecation, sunset | `references/api-versioning.md` | Introducing breaking changes or retiring endpoints |
| Pagination, filtering, sorting | `references/collection-endpoints.md` | Exposing unbounded or filterable collections |

# Caching, freshness, and conditional requests

## Caching and freshness

- For cacheable `GET`/`HEAD`, set explicit `Cache-Control` and validators (`ETag`/`Last-Modified`).
- Conditional requests return `304 Not Modified` when validators match.
- `ETag` for optimistic concurrency must reflect application state, not a shallow response hash. Do not treat `ShallowEtagHeaderFilter` output as a concurrency token.
- `HEAD` must expose the same caching/validator headers as the corresponding `GET`.
- Personalized or auth-scoped responses must not become cacheable by shared intermediaries unless `Cache-Control` makes it safe.

## Conditional requests and concurrency

- If lost updates matter, expose `ETag` on representations and require `If-Match` on mutating writes.
- `If-Match`/`If-Unmodified-Since` failures: `412 Precondition Failed`.
- If the contract requires a precondition on every write: `428 Precondition Required` when missing.
- `409 Conflict` for business/state conflicts that are not failed request preconditions.
- `PATCH` operations must be atomic. Never partially persist a patch.
- Prefer `ETag` over leaking raw persistence `version` fields in public DTOs.

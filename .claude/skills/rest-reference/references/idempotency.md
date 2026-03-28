# Idempotency and retries

- `PUT` and `DELETE` must remain idempotent in implementation, not only by HTTP method name.
- For non-idempotent `POST`/`PATCH` likely to be retried, support the `Idempotency-Key` header (IETF draft-07, Standards Track, widely adopted by Stripe, Adyen, and others).
- Key format: UUID v4 or equivalent random identifier.
- Document uniqueness scope, expiry window, request fingerprint rules, replay behavior, and error semantics.
- Error responses: missing key on required endpoint -> `400`; key reused with different payload -> `422`; concurrent request with same key in-flight -> `409`.
- Duplicate idempotent requests return the previous successful response, not re-execute the side effect.

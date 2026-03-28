# HTTP method and status semantics

- `GET` and `HEAD` are safe and read-only. Empty collections return `200 OK` with an empty array, not `204`.
- `POST` creating a resource returns `201 Created` with `Location`. Async processing returns `202 Accepted` with a status monitor link.
- `PUT` uses full-replacement semantics. Return `200 OK` with a body or `204 No Content`.
- `PATCH` uses partial-update semantics. Apply atomically and document the accepted patch media type.
- `DELETE` returns `204 No Content`. Choose one 404 contract (return 404 or silently succeed) and document it.
- Unsupported methods: `405 Method Not Allowed` with `Allow` header.
- Do not tunnel writes through `GET` or use `POST` as a generic RPC escape hatch.
- Long-running work should become a job/operation resource that clients poll.

## Default status mapping

| Scenario | Status | Notes |
|---|---|---|
| Validation failure (binding, type, Bean Validation) | `400` | Before domain parsing |
| Missing, malformed, expired credentials | `401` | Preserve `WWW-Authenticate: Bearer ...` |
| Authenticated but lacks required role/scope | `403` | |
| Missing resource | `404` | Module-specific problem type |
| Business rule violation, duplicate, state conflict | `409` | Conflict without failed precondition |
| Failed precondition (`If-Match`, `If-Unmodified-Since`) | `412` | Concurrency guard |
| Semantically invalid after parsing | `422` | Well-formed but unprocessable |
| Required precondition missing | `428` | Client must resend with `If-Match` |
| Rate limited | `429` | Include `Retry-After`, `RateLimit`, `RateLimit-Policy` |
| Unexpected failure | `500` | Log server-side, generic client message |

## Input validation and content negotiation

- Malformed JSON, type mismatches, missing required parameters, and Bean Validation failures are `400`.
- Syntactically valid content that cannot be processed semantically is `422`.
- Unsupported request media types are `415`. Unsupported `Accept` is `406`. Oversized payloads are `413`.
- Declare `consumes`/`produces` explicitly when anything other than JSON would be ambiguous. Do not widen to `*/*`.
- `application/json` for normal responses, `application/problem+json` for error responses.
- Prefer strong transport types over `Map`, `JsonNode`, or framework request objects.

## Problem detail contract (RFC 9457)

- `type`: stable URI. Defaults to `about:blank` (Spring's default). Set module-specific URIs for domain exceptions. IANA Problem Type Registry exists for widely applicable types.
- `title`: stable per problem type, localizable. When `type` is `about:blank`, match the HTTP status phrase.
- `detail`: occurrence-specific, client-actionable. Never leak stack traces, SQL, class names, or topology.
- `instance`: include an occurrence identifier when possible.
- Extensions: machine-readable only. Clients must not parse `detail`. Extension field names: start with a letter, only letters/digits/underscores, 3+ characters.

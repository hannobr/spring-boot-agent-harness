# API lifecycle and versioning

- Prefer additive, backwards-compatible changes. Do not make breaking changes on a stable contract.
- When a breaking change is unavoidable, use Spring Framework 7's built-in API versioning: `@GetMapping(version = "1.2")` with `ApiVersionStrategy`. Version ranges (`"1.2+"`) express "this handler serves 1.2 and all future versions until superseded".
- Do not expose minor or patch versions in URIs or media types.
- When deprecating: mark deprecated in OpenAPI and emit `Deprecation` (RFC 9745) + `Link` relation to migration docs. `Deprecation` uses `@timestamp` format (e.g., `Deprecation: @1688169599`).
- When retiring: also emit `Sunset` (RFC 8594) using HTTP-date format. `Sunset` timestamp must not be earlier than `Deprecation`.
- Spring Framework 7's deprecation handler can automatically emit `Deprecation`, `Sunset`, and `Link` headers when configured with `ApiVersionStrategy`.
- Permanently removed endpoints: `410 Gone` is preferable to ambiguous `404` when it helps clients migrate.

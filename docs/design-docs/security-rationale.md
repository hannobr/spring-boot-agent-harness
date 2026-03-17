# Security Rationale

Extended context and rationale for the template's security posture. Enforced rules are at [`.claude/rules/security.md`](../../.claude/rules/security.md) — that file is the source of truth for agent-facing security guidance.

## Current Posture
- Spring Security is configured explicitly through a `SecurityFilterChain`.
- JWT bearer token authentication via OAuth2 Resource Server.
- CORS is enabled for local browser clients on `/api/**` (configurable via `cors.allowed-origins`).
- `/actuator/health` and `/actuator/info` are public.
- `/v3/api-docs/**` and `/swagger-ui/**` are public (OpenAPI spec and Swagger UI). Disabled in the `prod` profile via `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false`.
- All other endpoints require a valid JWT bearer token.
- Session creation policy is `STATELESS`.
- CSRF is disabled.

## Authentication

### Dev profile
- HMAC-based JWT validation using a static signing key (`jwt.secret-key` in `application-dev.yaml`).
- Generate tokens with `scripts/generate-token.sh [subject]` (requires Python 3 + PyJWT).
- Use the token with any protected endpoint you add under `/api/...`.

### Prod profile
- JWT validation via external Identity Provider configured through `JWT_ISSUER_URI` environment variable.
- Spring Boot auto-configures JWK-based `JwtDecoder` from the issuer URI.

## Error Handling
- Spring MVC Problem Details are enabled.
- API errors should use standardized `ProblemDetail` responses.
- HTTP concerns belong in adapters and exception handlers, not in domain exceptions or use-case code.

## Configuration And Secrets
- Production datasource configuration comes from environment variables.
- JWT issuer URI and CORS origins come from environment variables in prod.
- Avoid hard-coding production secrets into the repo.
- Treat profile-driven runtime differences as configuration, not as a reason to fork business logic.

## Extension Rules
When extending the template:
- preserve the explicit security configuration model rather than falling back to framework defaults
- keep public monitoring endpoints narrow
- be deliberate when changing session or CSRF behavior
- update docs and validation expectations when the security model changes
- keep security and HTTP behavior out of domain/use-case code

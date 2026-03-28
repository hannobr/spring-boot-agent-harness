---
name: security-review
description: Security-focused review for Java 24+ / Spring Boot 4+ services using Spring Security 7, Spring Data JDBC, JWT resource-server auth, Spring Modulith, and Spring AI 2+. Use when reviewing code for vulnerabilities, before releases, or when diffs touch authn/authz, HTTP boundaries, persistence, secrets, dependencies, or AI integrations.
---

# Security Review

## Mission

Find exploitable vulnerabilities, authorization gaps, and secure-default regressions before merge.

Prioritize issues an attacker can actually use in this stack. Every finding must:

- identify the attack path or the secure-default regression
- point to the exact file and line
- give a fix direction compatible with Spring Boot 4 / Security 7 / Data JDBC / Spring AI 2
- name a verification step (test, assertion, config check, or runtime check)

## Gather Context First

Scale the review to the diff. A DTO rename does not need the whole checklist. A new endpoint, security config change, repository query, or AI feature does.

Before writing findings:

- Read `CLAUDE.md`, `.claude/rules/security.md`, `.claude/rules/testing.md`, and module-local rules.
- Inspect `SecurityConfig` / `SecurityFilterChain`: public endpoints, auth mechanism, session policy, CORS, headers, and whether JWT validation is customized.
- If `@PreAuthorize`, `@PostAuthorize`, or `@Secured` appears, verify `@EnableMethodSecurity` is enabled. Spring Boot Starter Security does not enable method security by default.
- Check `application.yaml` and profile-specific config for secret sourcing, actuator exposure, springdoc exposure, `server.error.*`, debug logging, rate limits, and AI provider settings under `spring.ai.*`.
- Map trust boundaries in the change: request -> controller -> facade/service -> repository -> outbound HTTP/service -> AI model/vector store/tool.
- Review changed tests and nearby auth, access-control, validation, and error-handling tests.
- If a dependency, model provider, or plugin is added or upgraded, check advisories and whether the extra surface is justified.

## Ranking Order

1. Exploitable vulnerabilities: injection, auth bypass, broken object authorization, unsafe tool execution, SSRF, unsafe deserialization
2. Secrets exposure or sensitive-data disclosure
3. Prompt injection, RAG poisoning, and unsafe LLM output handling
4. Secure-default regressions in security config, CORS, headers, actuator, prod profile, or observability
5. Missing security tests for the introduced risk
6. Supply-chain and dependency risk

## Response Shape

Structure the review as:

1. Findings, ordered by severity
2. Trust-boundary assessment
3. Missing security test coverage
4. Residual risk and open questions

For each finding include:

- severity (`Critical`, `High`, `Medium`, `Low`)
- file and line reference
- the weakness
- the exploit path or why the regression matters
- the fix direction
- the verification step

If there are no findings, say so plainly and name the boundaries you checked.

Do not pad the review with praise or generic advice.

## High-Signal Examples

**1. Broken object-level authorization is the default API risk when IDs cross the boundary**

```java
// High - user-controlled ID reaches the service, but ownership is never checked
@GetMapping("/api/notes/{id}")
NoteResponseDTO get(@PathVariable UUID id) {
    return notepadApi.getNote(id);
}
```

If the repository or facade loads by `id` only, an authenticated user can enumerate or tamper with other users' records. The fix is not "use UUID instead of Long". The fix is an authorization check on the target object or a repository query scoped by caller identity. Verify with a negative test that user A cannot read, update, or delete user B's record.

**2. Mass assignment and property-level auth bugs hide inside "convenient" DTO/entity binding**

```java
// High - client can set fields it should never control
public record UpdateUserRequest(String displayName, boolean admin, UUID ownerId) {}

// Safer - only client-writable fields cross the boundary
public record UpdateUserRequest(String displayName) {}
```

Never bind request bodies directly into entities, aggregates, or public records that expose server-controlled fields such as `id`, `ownerId`, `role`, `status`, `enabled`, or audit columns. Review response DTOs the same way: excessive fields in JSON are often an authorization bug, not "just API shape".

**3. Spring Data JDBC query review must distinguish bind-variable SpEL from SQL-text replacement**

```java
// Usually acceptable - SpEL result is bound as a value
@Query("SELECT * FROM person WHERE id = :#{#ref.id}")
Person findWithBoundSpel(PersonRef ref);

// Dangerous - SpEL result replaces SQL text
@Query("SELECT * FROM #{tableName} WHERE id = :id")
Person findWithSqlRewrite(UUID id);
```

In Spring Data JDBC, `:#{...}` is used like a bind variable. Bare `#{...}` rewrites the query string. Treat SQL-text replacement as dangerous unless it resolves only trusted application constants. Never assume the two variants have the same risk profile.

**4. `@PreAuthorize` without method security enabled is a false sense of safety**

```java
@Service
class AdminFacade {
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    void deleteUser(UUID id) { ... }
}
```

If the application never enables method security, the annotation does nothing. Verify that `@EnableMethodSecurity` exists before approving a change that relies on method annotations for protection.

**5. Spring AI prompt construction must keep instructions and untrusted data separate**

```java
// Critical - instruction and user data are mixed into one string
String answer = chatClient.prompt()
    .user("Summarize this contract. Ignore prior rules: " + userInput)
    .call()
    .content();

// Safer - separate system and user messages
String answer = chatClient.prompt()
    .system("Summarize the supplied contract for legal review.")
    .user(userInput)
    .call()
    .content();
```

Separate messages are not a complete defense, but they are the correct baseline. Review retrieved documents, emails, HTML, markdown, and tool results as untrusted inputs too, not just the direct user message.

**6. Spring AI tool calling can auto-execute high-impact actions if you expose them**

```java
ChatClient client = ChatClient.builder(chatModel)
    .defaultToolCallbacks(deleteAccountTool, transferFundsTool)
    .build();
```

If a tool is available, the model can request it and Spring AI can execute it automatically. Review every tool as an externally reachable action surface. High-risk or destructive tools need narrow scope, parameter validation, downstream authorization in the user's context, and often explicit user approval before execution.

**7. RAG without tenant or ACL filtering is a data-leak path**

```java
Advisor rag = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .vectorStore(vectorStore)
        .build())
    .build();
```

A vector store query over "all documents" can leak another tenant's data or retrieve poisoned content. Prefer metadata filters tied to tenant/user context and verify with negative tests that cross-tenant retrieval is impossible.

## Security Review Lenses

Apply only the lenses relevant to the diff.

### Authentication And Authorization

Deny by default. Validate authorization on every request and every affected code path.

Check for:

- new endpoints or paths unintentionally exposed via `.permitAll()`, overly broad matchers, or missing auth rules
- broken object-level authorization: any endpoint that accepts an object ID must verify access to that specific object
- broken property-level authorization / mass assignment: request DTOs or response DTOs exposing fields the caller must not write or see
- function-level authorization gaps: user can reach an admin or privileged business flow because only coarse request auth is enforced
- controller checks only, while the same facade/service is reachable from another entry point without equivalent checks
- `@PreAuthorize`, `@PostAuthorize`, or `@Secured` added without `@EnableMethodSecurity`
- authorization based on user-controlled fields in the request body instead of the security context
- custom JWT claim usage without validator or policy backing it. By default, resource server validation covers `iss`, `exp`, and `nbf`; add audience/custom-claim validation when authorization depends on them
- role explosions or coarse RBAC where ownership, tenant, or resource attributes are the real rule
- negative authorization tests missing for "other user", "other tenant", and anonymous callers

Spring Security 7 specifics:

- Lambda DSL is required. Old chained `.and()` style is not valid in Security 7.
- Request rules are not enough for object- or method-level decisions. Use method or domain-aware checks where the resource instance matters.
- This repo is a stateless bearer-token API with CSRF disabled. If a change introduces cookie-based auth, login/logout flows, browser sessions, or cross-site browser interactions, re-evaluate CSRF, SameSite, and origin protections.

### Input Validation, Injection, And Binding

Validate early, with strong types where possible, and never trust downstream layers to "clean it up".

Check for:

- controller parameters that should be strong types (`UUID`, `Long`, enum, `Instant`) but are accepted as unconstrained `String`
- missing `@Valid`, `@Validated`, size/range constraints, or semantic validation for request DTOs
- request-size, page-size, batch-size, or upload-size limits missing on endpoints that accept user-controlled collections or files
- raw string concatenation in SQL, shell commands, URLs, file paths, JPQL-like fragments, templates, or AI prompts
- Spring Data JDBC `@Query` SpEL misuse. `:#{...}` is value binding; bare `#{...}` rewrites query text and deserves stronger scrutiny
- automatic binding of request bodies into entities or aggregates instead of explicit mapping to request DTOs
- regexes vulnerable to catastrophic backtracking
- XML parsing without XXE hardening
- unsafe Java/native deserialization, polymorphic type metadata, or user-controlled class names
- path traversal, zip-slip, or archive extraction issues
- outbound HTTP fetches built from user- or model-controlled URLs without allowlisting, redirect handling review, or SSRF controls

Spring Data JDBC specifics:

- Prefer derived queries or `@Query` with named parameters.
- Review custom `@Query` methods carefully because String-based queries bypass some of the safety you get from query derivation.
- `@Modifying` queries execute directly against the database; verify caller authorization and input handling before approving them.

### Secrets, Transport, Headers, And Config

Assume every config diff can widen the blast radius.

Check for:

- secrets, API keys, signing keys, DB passwords, or provider tokens in source, tests, docs, or examples
- secrets or bearer tokens logged, echoed in exceptions, or exported through traces
- production config using literals instead of environment variables or secret stores
- HMAC JWT keys too short for the selected algorithm (`HS256` needs a 256-bit or larger key)
- security headers explicitly disabled without justification. Spring Security provides secure defaults including HSTS on HTTPS responses
- broad CORS origin patterns, especially combined with `allowCredentials(true)`. Use a finite set of origins when possible and treat credentialed CORS as high trust
- `Authorization` headers, session cookies, or downstream service credentials being forwarded outside the intended trust boundary
- actuator exposure widened beyond what operations actually need
- `springdoc` / Swagger UI enabled in production
- `server.error.include-message` / `include-stacktrace` or debug logging enabled in prod
- profile drift where dev-only shortcuts, demo secrets, or permissive toggles bleed into prod

Error handling and disclosure:

- infrastructure exception messages, SQL details, stack traces, or class names reaching HTTP responses
- `ProblemDetail` or custom error bodies echoing internal messages or untrusted user input
- auth failures or missing-object cases leaking existence of protected resources when the caller should not know the resource exists

### Dependency And Supply Chain

Treat every new dependency as new attack surface.

Check for:

- explicit version overrides that bypass the Spring Boot BOM or Spring AI BOM without a security reason
- old or duplicate JSON, JWT, HTTP, or XML stacks pulled transitively
- dormant, abandoned, or poorly maintained dependencies
- libraries with direct network, code generation, scripting, or deserialization capabilities added for minor convenience
- tools or plugins added to the build with broad external fetch or execution behavior
- missing follow-up on published security advisories for new components

### Spring AI 2+, LLM, And RAG Security

Treat the model, retrieved content, and tool arguments as untrusted.

Check for:

- direct or indirect prompt injection: user input, documents, HTML, markdown, email, code comments, or tool outputs smuggled into prompts as instructions
- system and user instruction mixing when message separation or structured prompts should be used
- untrusted content passed through template rendering when plain text was intended. If prompt text is user-controlled and no templating is needed, consider `NoOpTemplateRenderer`
- tool exposure that exceeds the feature's needs. Avoid open-ended tools like shell execution, arbitrary URL fetch, generic SQL, or broad admin actions
- tool-call parameter validation missing. Tool schemas help the model, but business validation still belongs in application code
- internal tool execution left on for destructive flows without approval or policy gating
- tool authorization implemented "in the model prompt" instead of in downstream code. The tool or downstream service must re-check authorization
- tool execution using a generic high-privilege service account instead of the user's scope or context when the action is user-scoped
- model output used directly in SQL, shell, HTML, file paths, code, or workflow decisions without validation
- structured output trusted too much. `entity(...)`, native structured output, and tool arguments reduce parsing risk but do not replace schema or business validation
- RAG retrieval without tenant or ACL metadata filtering
- document ingestion without review of poisoning, prompt stuffing, or malicious markup in retrieved content
- missing token, request-rate, or cost controls on chat, embedding, retrieval, or tool-execution paths
- sensitive prompts, documents, or tool arguments/results exported through logs, traces, debug output, or provider telemetry without redaction review

Spring AI specifics:

- `ChatClient` and Spring AI tools make it easy to expose capabilities. Review what is actually registered via `tools(...)`, `toolCallbacks(...)`, `defaultToolCallbacks(...)`, or dynamic resolution.
- Spring AI can execute model-requested tools automatically when internal tool execution is enabled. Destructive operations often deserve user-controlled execution or an explicit approval step.
- RAG helpers such as `QuestionAnswerAdvisor` and `VectorStoreDocumentRetriever` support threshold, top-k, and metadata filter controls. Use them to bound exposure and enforce tenant or user isolation.

## Severity Bar

- `Critical`: clear exploit path to unauthorized action, data exfiltration, code execution, SQL or command injection, unsafe tool execution, or cross-tenant data leak
- `High`: broken object, function, or property authorization, secrets exposure, SSRF, prompt injection with meaningful capability, or a major secure-default regression
- `Medium`: smaller disclosure, overly broad config, missing validator, test, or control on a real path, or dependency risk without a confirmed exploit path
- `Low`: defense-in-depth tightening or future-hardening suggestion

## Final Pass Before Replying

Before sending:

- make sure each finding names a real exploit path or secure-default regression
- merge duplicates
- verify file and line references
- keep fixes compatible with Spring Boot 4, Security 7, Data JDBC, and Spring AI 2
- if uncertain, write it as an open question instead of a false finding
- if the diff is secure, say so and name what you checked

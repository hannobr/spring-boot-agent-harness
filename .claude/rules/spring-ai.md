---
paths:
  - "src/**/*.java"
  - "pom.xml"
  - "src/main/resources/application.yaml"
---

# Spring AI rules

This project uses **Spring AI 2+** for all AI/ML features. This is a project-wide mandate.

## Version

- Use the latest Spring AI 2.x release compatible with the project's Spring Boot version.
- Current: `spring-ai-bom` 2.0.0-M3 (milestone — requires Spring Milestones repository).
- When a GA release becomes available, upgrade immediately.

## Dependencies

Use Spring AI starters (managed by the BOM — no explicit version in `<dependency>`):

| Concern | Artifact |
|---------|----------|
| OpenAI models | `spring-ai-starter-model-openai` |
| PGVector store | `spring-ai-starter-vector-store-pgvector` |

Add other starters as needed from the BOM. Never pin individual Spring AI artifact versions.

## Configuration

- Bind AI-related settings under `spring.ai.*` in `application.yaml`.
- API keys via environment variables (`${OPENAI_API_KEY}`). Never commit secrets.
- Module-specific tuning (batch sizes, delays) under the module's own config prefix (e.g., `jurido.search.embedding.*`).

## PGVector

- Let Spring AI manage the `vector_store` table (`initialize-schema: true`). Do NOT create it via Flyway.
- The `vector` PostgreSQL extension must be created via Flyway before Spring AI initializes.

## Testing

- **Unit/slice/module tests:** `@MockitoBean EmbeddingModel` — never call OpenAI in automated tests.
- **Integration tests:** WireMock stubbing the OpenAI API via `spring.ai.openai.base-url`.
- Testcontainers must use a pgvector-enabled image (`pgvector/pgvector:pg17`).

## What NOT to do

- Do NOT use Spring AI 1.x — it targets Spring Boot 3.x and is incompatible with this project.
- Do NOT add `spring-ai-rag` or retrieval advisors unless building a RAG module.
- Do NOT build abstraction layers over embedding providers — use Spring AI's built-in abstractions directly.

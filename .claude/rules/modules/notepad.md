---
paths:
  - "src/**/notepad/**"
---

# Notepad module contract

> Reference implementation — delete this module when starting real work.

## Purpose
Demonstrates the full vertical slice pattern: domain, use cases, persistence, REST, and tests at every tier.

## Complexity
standard

## Public API (root package)

| Type | Role |
|------|------|
| `NotepadAPI` | Module API interface |
| `Note` | Domain record |
| `NoteNotFoundException` | Domain exception |

## Hidden packages (implementation details)

| Package | Contains |
|---------|----------|
| `internal/` | `NotepadFacade`, `CreateNoteUseCase`, `FindNoteByIdUseCase`, `NotePersistencePort`, `NotepadModuleConfiguration` |
| `persistence/` | `NoteEntity`, `NoteRepository`, `NoteRepositoryAdapter` |
| `rest/` | `NoteController`, `CreateNoteRequestDTO`, `NoteResponseDTO`, `NoteExceptionHandler` |

## Allowed dependencies
None

## Cross-module communication
- **Direct API calls**: None — this module has no dependencies.
- **Events**: None published or consumed.

## Owned resources
- **Database table(s):** `notes` (`V2__create_notes_table.sql`)
- **REST endpoints:** `POST /api/notes`, `GET /api/notes/{id}`
- **Events published:** None
- **Events consumed:** None

## Consumer surface

| Endpoint | Method | Request body | Success response | Error responses |
|----------|--------|-------------|-----------------|-----------------|
| `/api/notes` | POST | `CreateNoteRequestDTO` (title, body) | 201 + `NoteResponseDTO` | 400 validation |
| `/api/notes/{id}` | GET | N/A | 200 + `NoteResponseDTO` | 404 not found |

### Behavioral notes
- Create is not idempotent — each call creates a new note.
- `createdAt` is server-assigned; client cannot set it.
- 404 returns RFC 9457 ProblemDetail with `title: "Note Not Found"`.

## Validation commands
```bash
# Fast: unit tests only (no Docker needed)
./mvnw -q test -Dtest="NoteTest,CreateNoteUseCaseTest,FindNoteByIdUseCaseTest,NoteExceptionHandlerTest"

# Slice tests (Docker required)
./mvnw -q test -Dtest="NoteRepositoryAdapterTest,NoteControllerSliceTest"

# Module test (Docker required)
./mvnw -q test -Dtest="NotepadModuleTest"

# Integration test (Docker required)
./mvnw -q test -Dtest="NotepadIntegrationTest"

# Full verification
./mvnw -q verify
```

## Rules for changes in this module
- Every `package-info.java` must have `@org.jspecify.annotations.NullMarked`. New subpackages need their own `package-info.java`.
- New public types in root package must be added to the "Public API" table above
- New internal classes must follow existing patterns per `.claude/rules/modulith.md`
- No other module may directly access this module's owned tables
- Update this contract when adding public API types, endpoints, tables, or dependencies
- Keep `package-info.java` in sync with the "Allowed dependencies" section above
- Update the consumer surface section when adding, changing, or removing endpoints or response shapes

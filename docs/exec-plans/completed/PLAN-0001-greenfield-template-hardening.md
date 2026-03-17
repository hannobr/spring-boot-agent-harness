---
status: completed
---

# PLAN-0001: greenfield template hardening

## Goal
- Turn the simplified repository into a clean, self-consistent Java 25 / Spring Boot 4 greenfield backend template that validates cleanly and gives both Codex and Claude Code deterministic repo guidance.

## Non-goals
- Reintroducing the deleted banking demo, frontend, or historical execution artifacts.
- Adding real business modules before the template contract, scaffold, and validation surface are reliable.

## Approach
- Align the runtime, harness scripts, and docs around the current greenfield shape instead of the removed demo surface.
- Tighten validation so the repo enforces the same rules it documents.
- Remove or ignore local/generated Claude artifacts that do not belong in a reusable template.
- Add the smallest useful test coverage and generated artifacts needed for a fully passing baseline.

## Steps
- [x] Update the plan as work starts and key decisions land.
- [x] Fix runtime and harness issues: JWT helper, OpenAPI drift enforcement, local startup flow, and compose/runtime rough edges.
- [x] Tighten architecture enforcement and add tests so `./mvnw -q verify` passes from the current greenfield baseline.
- [x] Clean template noise from `.claude`, repo ignores, and stale docs so the Codex/Claude guidance matches reality.
- [x] Regenerate derived artifacts and run the full validation sequence, including runtime startup.

## Contract updates (if this plan changes a module's REST boundary)
- [x] Module contract consumer surface reviewed/updated
- [x] OpenAPI annotations match contract

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| The worktree already contains large user-authored deletions from the old demo surface. | Only work forward from the current simplified baseline and avoid restoring removed files. |
| Template docs may drift again if validation remains weaker than documentation. | Make the harness fail on missing OpenAPI artifacts and add explicit enforcement for documented DI rules. |
| Local Docker state from the old schema can create false negatives during startup review. | Prefer a fresh compose volume name and verify startup against the updated runtime path. |

## Definition of done
- `./mvnw -q verify` passes.
- OpenAPI generation and drift validation are deterministic and committed.
- The runtime startup path works with the documented commands.
- The docs, harness, and `.claude` layer describe the current greenfield template, not the removed demo.

## Decision log

| Date | Decision | Rationale |
|---|---|---|
| 2026-03-16 | Keep the repo as a clean greenfield backend with no starter business module yet. | The immediate problem is contract and validation integrity, not lack of demo domain code. |
| 2026-03-16 | Remove local/generated Claude artifacts from the template and ignore them going forward. | They add noise, leak machine-local state, and weaken portability for both Codex and Claude Code. |
| 2026-03-16 | Add a baseline Flyway migration and a fresh compose volume name. | This removes zero-migration ambiguity and avoids reusing stale local demo-era schema state during startup validation. |

## Tech debt introduced
- None yet.

# Module Contracts

Back to the map:
- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [docs/design-docs/index.md](../design-docs/index.md)

This directory contains human-facing module contract templates and notes for the business modules in `src/main/java/nl/jinsoo/template/`.

## Purpose
Module contracts make ownership explicit. They document:
- what a module owns
- what its public API is
- what dependencies it is allowed to take
- what resources it owns
- how to validate changes within it

## Current Source Of Truth
The repo currently enforces module contract presence under `.claude/rules/modules/`.

Use this `docs/modules/` directory for:
- template/reference material
- longer-form human notes when a module needs them

Do not assume files here are the mechanically enforced source of truth.
Keep these human-facing notes aligned with [README.md](../../README.md) and [ARCHITECTURE.md](../../ARCHITECTURE.md) so the repo-level story does not drift from the module-level story.

## Future Modules
When adding a new top-level module package:
- add the module package under `src/main/java/nl/jinsoo/template/`
- add or update `package-info.java`
- add the enforced contract at `.claude/rules/modules/<module-name>.md`
- optionally add a longer-form note here if the module earns it (canonical template at [`.claude/rules/modules/MODULE-TEMPLATE.md`](../../.claude/rules/modules/MODULE-TEMPLATE.md))
- add tests for the appropriate layers

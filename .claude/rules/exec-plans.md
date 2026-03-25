---
paths:
  - "docs/exec-plans/**"
---

# Execution plan lifecycle

## Completion checklist

When ALL steps in a plan's checklist are done AND the definition-of-done criteria pass:

1. Mark every `- [ ]` as `- [x]` in the plan file
2. **Move the plan file** from `docs/exec-plans/active/` to `docs/exec-plans/completed/`
3. Commit the move as part of the final commit (or as a dedicated commit)

The plan is NOT complete until the file lives in `completed/`. Marking checkboxes without moving the file is incomplete work.

## When committing planned work

Before creating commits for work tracked by an exec plan, verify:
- Is there an active plan in `docs/exec-plans/active/` for this work?
- If yes, has every step been completed?
- If yes, move the plan to `completed/` and include the move in the commit

Never commit planned work while the plan still sits in `active/`.

## Creating plans

Use `scripts/harness/new-exec-plan` to scaffold new plans. See `docs/PLANS.md` for full guidance on when to create plans vs epics.

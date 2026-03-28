---
paths:
  - "src/main/**"
---

# Definition of Done — edit gate

Before modifying production code, confirm you have stated a machine-checkable
exit condition for this task. If you have not, invoke the `dod` skill now.

This rule exists because the dod skill auto-triggers on task description
keywords, which may miss ad-hoc edits. This path-based rule ensures the
exit condition is stated before code changes, not only checked at session end.

## State file: `.claude/dod-current.json`

The `dod` skill writes a state file containing the task, risk tier, exit
condition, and a `stated_at` timestamp. This file survives context compression —
the Stop hook reads it to verify that the exit condition was run.

### Lifecycle

1. **dod skill** writes the file when stating the exit condition (Step 2).
2. **Stop hook** checks for the file and its `verified_at` field. Surfaces a
   warning if missing or unverified.
3. **`scripts/harness/full-check`** stamps `verified_at` when all checks pass.

The file is overwritten on each new DoD invocation. It is gitignored.

### If you see a Stop hook warning about this file

- If the file is missing: invoke the `dod` skill to assess and write it.
- If `verified_at` is missing: run the exit condition command, then run
  `scripts/harness/full-check` to stamp verification.

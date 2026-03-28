---
paths:
  - "docs/exec-plans/**"
---

# Execution plans

## Epics

When work spans multiple plans, persist an epic file BEFORE creating any child plans.

Named `EPIC-NNNN-topic.md`. Use next available sequence number (glob both `active/` and `completed/` to find the highest existing `EPIC-NNNN`).

### Required sections

- Context (why this epic exists)
- Key architectural decisions (table: Decision, Choice, Rationale)
- Execution plans (list of child PLANs with dependencies and parallelism)
- Definition of done
- Tech debt introduced

### Ordering constraint

1. Persist the EPIC file to `docs/exec-plans/active/` first.
2. Then create child PLAN files (they reference the epic).
3. When all child plans are completed, move the EPIC to `docs/exec-plans/completed/`.

## Creating plans and epics

**MANDATORY**: Always use `scripts/harness/new-exec-plan` to create plan and epic files. Never create them by hand.

```bash
# Create a plan
scripts/harness/new-exec-plan plan <topic-slug>

# Create a plan under an epic
scripts/harness/new-exec-plan plan <topic-slug> EPIC-XXXX

# Create an epic
scripts/harness/new-exec-plan epic <topic-slug>

# Dry run (preview filename without creating)
scripts/harness/new-exec-plan --dry-run plan <topic-slug>
```

The script auto-assigns the next sequence number and generates a template with every required section. Fill in the template — do not skip sections, do not restructure the file.

## Plan file format

Named `PLAN-NNNN-topic.md`. Sequence number is assigned by the script.

### Required sections

- Goal
- Non-goals
- Approach
- Steps (checklist -- mark `- [x]` as completed)
- Decision log (append-only -- record every non-trivial choice and why)
- Risks & mitigations
- Definition of done
- Tech debt introduced (list workarounds/deferred work, or "None")

## Definition of done quality bar

Every item in a plan's "Definition of done" section must be machine-checkable:
a command you can run that produces a pass/fail result. Prose descriptions are
not acceptable — state the verification command and expected outcome.

Good:
```
- [ ] `./mvnw -q test -Dtest=OrderControllerSliceTest` — all 6 tests pass
- [ ] `curl -s localhost:8080/api/orders | jq length` returns 0 after DELETE
```

Bad:
```
- [ ] All endpoints work end-to-end
- [ ] Test coverage at all 4 tiers
```

The two pre-populated items (audit agent + full-check) are universal baselines.
Task-specific items must be added for every plan — they verify the specific
behavior this plan delivers, not just that nothing broke.

For epics, DoD items are coarser (e.g., "all child PLANs completed + full-check
on integrated result") but must still reference a runnable check.

## Execution discipline

While executing a plan, treat the plan file as a living document:

1. **Before starting a step**: mark it `- [~]` (in progress).
2. **After completing a step**: mark it `- [x]`.
3. **Decision log**: append a row immediately when making a non-trivial choice (library version, alternative approach, workaround). Do not batch these for later.
4. **If a step is skipped or changed**: update the step text and add a decision log entry explaining why.

## Finalization

When all steps are done:

1. Verify every step is `- [x]` or explicitly marked skipped with rationale.
2. Check decision log for tech debt — append to `docs/exec-plans/TECH-DEBT-TRACKER.md` if any.
3. Set front-matter `status: completed`.
4. Move file from `docs/exec-plans/active/` to `docs/exec-plans/completed/`.

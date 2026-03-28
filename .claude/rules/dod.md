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

---
name: commit
description: Create atomic, well-grouped git commits from the current working tree. Use this skill whenever the user says "commit", "/commit", "commit my changes", "make commits", "commit all", or any variation of asking to commit their work. Also trigger when the user says things like "save my progress" or "wrap this up" in a git context.
user_invocable: true
---

# Atomic Commit Skill

You are creating a series of small, logical, atomic commits from the current working tree. Each commit should represent exactly one coherent change — something a reviewer could understand in isolation.

## Why atomic commits matter

A good commit history is a communication tool. Each commit tells a story: "here is one thing that changed, and here is why." When commits are atomic, `git bisect` works, reverts are safe, and code review is pleasant. When commits are kitchen-sink dumps, all of that breaks down.

## Step 1: Understand the full picture

Run these in parallel to understand what you're working with:

```bash
git status          # all changed, staged, and untracked files
git diff            # unstaged changes (full content)
git diff --cached   # staged changes (if any)
git log --oneline -10  # recent commit style to match
```

Read through ALL the changes carefully. You need to understand every file's change before you can group them.

## Step 2: Plan the commit groups

Group changes by logical cohesion — files that are part of the same conceptual change belong together. Here's how to think about it:

**What makes a good group:**
- A bug fix + its test = one commit
- A new feature's production code + its tests + its migration = one commit
- A rename/refactor that touches many files but does one thing = one commit
- Config/infra changes unrelated to feature work = separate commit

**What does NOT belong together:**
- An unrelated formatting fix lumped with a feature
- A dependency update mixed with a bug fix
- Test fixture updates that serve a different change than the main code

**Ordering matters:** Put foundational changes first. Schema migrations before code that uses them. Dependency additions before code that imports them. This way each commit compiles and passes tests on its own (when possible).

**Typical group patterns** (not exhaustive — use judgment):
1. Build/dependency/config changes
2. Schema migrations + domain model changes
3. Core feature or bug fix (production code + tests)
4. Test fixture or test infrastructure updates
5. Documentation, specs, contract updates

Aim for 2-7 commits for a typical feature. Don't over-split (one commit per file is too granular) and don't under-split (everything in one commit defeats the purpose).

## Step 3: Write commit messages

Follow the commit message conventions visible in `git log`. If no clear convention exists, use this format:

```
<summary line: imperative mood, what and why, under 72 chars>

<optional body: explain motivation, context, or non-obvious decisions>

Co-Authored-By: Claude <noreply@anthropic.com>
```

**Summary line rules:**
- Imperative mood: "Fix bug" not "Fixed bug" or "Fixes bug"
- Focus on **why** over **what** when the diff makes the "what" obvious
- Be specific: "Fix UTF-8 BOM causing XML parse failure" not "Fix bug"
- Under 72 characters — if you can't fit it, the commit is probably doing too much

**When to add a body:**
- The "why" isn't obvious from the diff
- There are non-obvious design decisions worth explaining
- The change has broader context (e.g., "Part of the bulk harvest resilience work")

Skip the body for self-explanatory changes like "Update docker-compose credentials" or "Add missing test for edge case."

## Step 4: Execute the commits

For each group, stage the specific files and commit. Use HEREDOC for the message to preserve formatting:

```bash
git add <file1> <file2> ...
git commit -m "$(cat <<'EOF'
Summary line here

Optional body here.

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

**Important:**
- Stage specific files by name — never use `git add -A` or `git add .`
- Never stage files that look like secrets (`.env`, credentials, tokens)
- If a file has both related and unrelated changes, note this to the user rather than partially staging with `git add -p`

## Step 5: Verify

After all commits, run:
```bash
git status   # should be clean (or only intentionally untracked files)
git log --oneline -<N>  # show the commits you just made
```

Report the results: how many commits were created, what's left (if anything), and whether any untracked files were intentionally skipped.

## Edge cases

- **No changes:** If the working tree is clean, say so and stop.
- **Only staged changes:** Respect what the user already staged — commit those first, then ask about unstaged changes.
- **Sensitive files:** If you spot `.env`, credentials, or tokens in the changeset, warn the user and exclude them.
- **Single logical change:** If all changes are one coherent unit, one commit is fine. Don't split artificially.

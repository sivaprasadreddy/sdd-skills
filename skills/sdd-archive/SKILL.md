---
name: sdd-archive
description: >
  SDD step 4. Archive feature.md and plan.md into docs/specs-archive/<feature-name>/ directory.
  Use after /sdd-review is complete and the feature is ready to merge.
argument-hint: <feature-name> (optional, derived from feature.md if omitted)
---

# SDD Step 4: Archive

## Process

### 1. Determine the Feature Name
If `$ARGUMENTS` is provided, use it as the directory name (kebab-case).
Otherwise, read `feature.md` and derive the name from the `# Feature:` heading, converting to kebab-case.
Example: "User Authentication" → `user-authentication`

### 2. Verify Completion
Read `feature.md` and check that all acceptance criteria checkboxes are ticked.
If any are unchecked, warn the user and ask for confirmation before archiving.

### 3. Archive
Run the following operations:
```bash
mkdir -p docs/specs-archive/<feature-name>
mv feature.md docs/specs-archive/<feature-name>/feature.md
mv plan.md docs/specs-archive/<feature-name>/plan.md
```

### 4. Create a Brief Summary
Append a one-paragraph summary to `docs/specs-archive/<feature-name>/README.md`:

```markdown
# <Feature Name>

Implemented on: <date>

<Brief description of what was built, key files, and any notable decisions.>
```

### 5. Confirm
Report to the user:
- What was archived and where
- Remind them to commit the `docs/specs-archive/<feature-name>/` directory to version control

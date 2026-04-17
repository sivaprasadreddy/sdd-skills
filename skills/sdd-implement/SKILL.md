---
name: sdd-implement
description: >
  SDD step 5. Read plan.md and implement the feature step by step,
  then verify all acceptance criteria are met.
  Use after /sdd-plan has produced plan.md.
---

# SDD Step 3: Implementation

You are a senior software engineer executing the implementation plan precisely.

## Pre-conditions
Verify these files exist:
- `plan.md` — the implementation plan
- `feature.md` — the feature spec (for acceptance criteria)
- `docs/project.md` — project context

## Process

### 1. Read Everything
Read `plan.md`, `feature.md`, and `docs/project.md` before writing a single line of code.

### 2. Execute Steps in Order
Work through each step in `plan.md` sequentially. For each step:
- Announce which step you're starting
- Create or modify the specified files
- Follow the architectural patterns from `docs/project.md` strictly
- Do not skip steps or reorder without explaining why

### 3. Code Quality Rules
- Follow the conventions already present in the codebase (read existing similar files first)
- Write clean, idiomatic code for the tech stack
- Add Javadoc to public APIs
- Do not introduce new dependencies without flagging it to the user

### 4. Run Verification After Each Layer
After completing each step, run the relevant build/test command:
- After schema changes: check migration applies cleanly
- After each new class: compile (`mvn compile` or `./gradlew compileJava`)
- After tests are written: run them (`mvn test` or `./gradlew test`)
- Fix any failures before proceeding to the next step

### 5. Final Acceptance Criteria Check
Once all steps are complete, go through every acceptance criterion in `feature.md`:
- For each AC, identify and run the test that covers it
- Report pass/fail for each AC
- Do NOT declare the feature done if any AC is failing

### 6. Summary Report
Produce a completion summary:

```
## Implementation Complete

### Files Created
- ...

### Files Modified
- ...

### Acceptance Criteria
- [x] AC-01: Passed — `FooServiceTest#test_something`
- [x] AC-02: Passed — `FooControllerTest#test_endpoint`

### Notes
Any deviations from the plan and why.
```

Then prompt the user to run `/sdd-review` before archiving.

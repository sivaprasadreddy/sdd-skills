---
name: sdd-plan
description: >
  SDD step 4. Read feature.md and produce a detailed implementation plan.md
  tailored to the project's tech stack and architecture.
  Use after /sdd-analyse has produced feature.md.
---

# SDD Step 2: Implementation Planning

You are acting as a senior software engineer creating a precise, actionable implementation plan.

## Pre-conditions
Verify both files exist before proceeding:
- `feature.md` — the feature spec (if missing, tell the user to run `/sdd-analyse` first)
- `docs/project.md` — project context

## Process

### 1. Read Both Files
Read `feature.md` and `docs/project.md` in full.

### 2. Identify the Tech Stack
From `docs/project.md`, note:
- Primary language and framework (e.g., Java 25 + Spring Boot 4.x)
- Build tool (Maven or Gradle)
- Database and ORM
- Messaging systems
- Testing frameworks
- Any architecture patterns (e.g., Hexagonal, DDD, Layered)

### 3. Produce plan.md

Create `plan.md` in the project root with this structure:

```markdown
# Implementation Plan: <Feature Name>

## Overview
Brief description of the implementation approach.

## Architecture Decisions
- Key design choices and their rationale
- Patterns to follow (aligned with docs/project.md)

## Implementation Steps

### Step 1: <Database/Schema Changes>
- [ ] Create/alter table: `...`
- [ ] Add migration: `src/main/resources/db/migration/V<n>__<description>.sql`
- Files to create/modify: ...

### Step 2: <Domain Layer>
- [ ] Create entity: `...`
- [ ] Create value objects: `...`
- [ ] Define repository interface: `...`
- Files: ...

### Step 3: <Application/Service Layer>
- [ ] Create use case / service: `...`
- [ ] Define DTOs / command objects: `...`
- Files: ...

### Step 4: <Infrastructure/Adapter Layer>
- [ ] Implement repository: `...`
- [ ] External integrations: `...`
- Files: ...

### Step 5: <API / Presentation Layer>
- [ ] REST controller: `...`
- [ ] Request/response models: `...`
- [ ] OpenAPI annotations: `...`
- Files: ...

### Step 6: <Tests>
- [ ] Unit tests: `...`
- [ ] Integration tests (Testcontainers): `...`
- [ ] API tests: `...`
- Files: ...

## Acceptance Criteria Mapping
| AC | Verified By |
|----|-------------|
| AC-01: ... | `<TestClass#testMethod>` |
| AC-02: ... | `<TestClass#testMethod>` |

## Risks & Mitigations
- Risk: ... → Mitigation: ...

## Estimated Complexity
Low / Medium / High — brief justification
```

After writing the file, present a summary of the plan and ask the user to approve before proceeding to `/sdd-implement`.

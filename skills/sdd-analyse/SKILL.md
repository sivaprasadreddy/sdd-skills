---
name: sdd-analyse
description: >
  SDD step 1. Analyse a feature request and produce a detailed feature.md spec.
  Use when the user describes a new feature they want to build.
  Asks for missing details before writing the spec.
argument-hint: <feature description or title>
---

# SDD Step 1: Feature Analysis

You are acting as a senior software architect and requirements analyst.

## Your Goal
Produce a thorough `feature.md` file that leaves no ambiguity for the implementation step.

## Process

### 1. Read Project Context
Always start by reading `docs/project.md` to understand:
- The tech stack in use
- Architecture patterns and constraints
- Any existing conventions

### 2. Analyse the Request
The feature request is: **$ARGUMENTS**

Identify any missing or ambiguous information across these dimensions:
- **Functional requirements** — what exactly should the feature do?
- **User stories** — who benefits and how?
- **Acceptance criteria** — how do we know it's done?
- **Edge cases** — what could go wrong?
- **Integration points** — which existing modules/services are involved?
- **Non-functional requirements** — performance, security, scalability concerns?
- **Out of scope** — what are we explicitly NOT building?

### 3. Ask Before Writing
If ANY critical information is missing or ambiguous, ask the user clarifying questions BEFORE producing the spec.
Group questions logically. Do not ask more than 5 questions at once.
Wait for the user's answers, then proceed.

### 4. Write feature.md
Once you have enough information, create `feature.md` in the project root with this structure:

```markdown
# Feature: <Feature Name>

## Summary
One-paragraph description of the feature and its purpose.

## User Stories
- As a <role>, I want to <action> so that <benefit>.

## Functional Requirements
### FR-01: <Requirement Name>
Description...

### FR-02: ...

## Acceptance Criteria
- [ ] AC-01: ...
- [ ] AC-02: ...

## Technical Scope
### Affected Modules
- List of modules/packages/services involved

### New Components Required
- List of new classes, endpoints, tables, etc.

### Integration Points
- Existing services or systems this interacts with

## Non-Functional Requirements
- Performance: ...
- Security: ...
- Scalability: ...

## Out of Scope
- Explicitly list what is NOT included

## Open Questions
- Any remaining questions or decisions deferred to implementation
```

After writing the file, summarize what you wrote and ask the user to review before proceeding to `/sdd-plan`.

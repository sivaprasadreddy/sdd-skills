# AI Agent Context

This file provides guidance to AI Agents when working with code in this repository.

## What This Repo Is

A collection of Agent skills that implement **Spec Driven Development (SDD)** — a structured AI-assisted workflow where every feature starts with a written spec, proceeds through an explicit plan, and is closed out after code review and archival.

The skills live in `skills/<skill-name>/SKILL.md`. Each SKILL.md is a markdown prompt with a YAML frontmatter header (`name`, `description`, `argument-hint`) that AI Agent uses to register and invoke the skill.

## SDD Workflow

```
/sdd-init  →  /sdd-feature  →  /sdd-refine*  →  /sdd-plan  →  /sdd-implement  →  /sdd-review  →  /sdd-archive
(once)                          (optional,
                                 repeatable)
```

| Skill            | Reads                                      | Produces                            |
|------------------|--------------------------------------------|-------------------------------------|
| `/sdd-init`      | Project codebase                           | `docs/project.md`                   |
| `/sdd-feature`   | `docs/project.md`                          | `feature.md`                        |
| `/sdd-refine`    | `feature.md`, `docs/project.md`, `plan.md` | Updated `feature.md`                |
| `/sdd-plan`      | `feature.md`, `docs/project.md`            | `plan.md`                           |
| `/sdd-implement` | `plan.md`, `feature.md`, `docs/project.md` | Implemented code, `impl-summary.md` |
| `/sdd-review`    | `docs/project.md`, `feature.md`, `plan.md` | `review.md`, updated `feature.md`   |
| `/sdd-archive`   | `feature.md`, `plan.md`, `impl-summary.md` | `docs/specs-archive/<name>/`        |

## Key Files and Their Roles

- `skills/<name>/SKILL.md` — skill definition files; each is a self-contained prompt
- `docs/project.md` — template that consuming projects must fill in; all skills read this to understand the target project's tech stack, architecture, and conventions
- `feature.md` / `plan.md` — ephemeral files that exist in the consuming project root during active development, then get archived

## Skill File Format

Each `SKILL.md` must begin with this frontmatter:

```yaml
---
name: sdd-<step>
description: >
  One-sentence description used for skill discovery and invocation.
argument-hint: <argument description>  # omit if skill takes no arguments
---
```

The body is a markdown prompt.

## Installation

```bash
npx skills add https://github.com/sivaprasadreddy/sdd-skills
```

This installs sdd-skills at project or user level depending on your selection.

## docs/project.md Contract

All skills depend on the consuming project having a populated `docs/project.md`. The file must describe:
- Tech stack (language, framework, build tool, database, ORM, messaging, testing libraries)
- Architecture pattern (Layered, Hexagonal, DDD, etc.) and package structure
- Conventions (package naming, REST base path, error handling, auth defaults)
- Approved dependencies

Skills fail gracefully when this file is missing by telling the user to create it.

## Archive Layout

After `/sdd-archive`, the consuming project will have:
```
docs/specs-archive/<feature-name>/
├── feature.md
├── plan.md
├── review.md         ← present if /sdd-review was used
├── impl-summary.md   ← present if /sdd-implement was used
└── README.md
```

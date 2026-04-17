# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Repo Is

A collection of Claude Code skills that implement **Spec Driven Development (SDD)** — a structured AI-assisted workflow where every feature starts with a written spec, proceeds through an explicit plan, and is closed out after code review and archival.

The skills live in `skills/<skill-name>/SKILL.md`. Each SKILL.md is a markdown prompt with a YAML frontmatter header (`name`, `description`, `argument-hint`) that Claude Code uses to register and invoke the skill.

## SDD Workflow

```
/sdd-init  →  /sdd-analyse  →  /sdd-refine*  →  /sdd-plan  →  /sdd-implement  →  /sdd-review  →  /sdd-archive
(once)                          (optional,
                                 repeatable)
```

| Skill            | Reads                                      | Produces                     |
|------------------|--------------------------------------------|------------------------------|
| `/sdd-init`      | Project codebase                           | `docs/project.md`            |
| `/sdd-analyse`   | `docs/project.md`                          | `feature.md`                 |
| `/sdd-refine`    | `feature.md`, `docs/project.md`, `plan.md` | Updated `feature.md`         |
| `/sdd-plan`      | `feature.md`, `docs/project.md`            | `plan.md`                    |
| `/sdd-implement` | `plan.md`, `feature.md`, `docs/project.md` | Implemented code             |
| `/sdd-review`    | `docs/project.md`, `feature.md`, `plan.md` | Review report                |
| `/sdd-archive`   | `feature.md`, `plan.md`                    | `docs/specs-archive/<name>/` |

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

The body is a markdown prompt. Use `$ARGUMENTS` anywhere the user's argument should be interpolated.

## Installation

```bash
npx skills add https://github.com/sivaprasadreddy/sdd-skills
```

This installs skills into `.claude/skills/` of the target project.

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
└── README.md
```

# SDD IDE Plugin — Brainstorm

## Overview

The SDD workflow today lives entirely in an AI agent's chat interface. A dedicated IDE plugin would surface the workflow as a first-class, visual experience — giving developers pipeline visibility, one-click step navigation, inline spec/plan editing, and live status without ever leaving their editor.

Target IDEs: **JetBrains IDEs** (IntelliJ, PyCharm, GoLand, etc.) and **VS Code** (via extension API).

---

## Core Concepts Translated to IDE

| SDD Concept | IDE Surface |
|---|---|
| `docs/project.md` | Project context panel (read-only summary + edit button) |
| `feature.md` | Spec editor panel with structured sections |
| `plan.md` | Interactive step checklist panel |
| Acceptance Criteria checkboxes | Visual progress bar + checklist |
| `/sdd-*` commands | Toolbar buttons + right-click context menu |
| Review findings | Inline code annotations (like inspections) |
| Archive directory | Feature history timeline |

---

## Plugin Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│  IDE Main Window                                                     │
│                                                                     │
│  ┌──────────────┐  ┌────────────────────────────────────────────┐  │
│  │  Project     │  │  Editor (code files)                       │  │
│  │  Explorer    │  │                                            │  │
│  │              │  │                                            │  │
│  │              │  │                                            │  │
│  └──────────────┘  └────────────────────────────────────────────┘  │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  SDD PANEL  (bottom tool window)                            │   │
│  │  ┌──────────┬──────────┬──────────┬───────────┬──────────┐  │   │
│  │  │ Pipeline │   Spec   │   Plan   │  Review   │ History  │  │   │
│  │  └──────────┴──────────┴──────────┴───────────┴──────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

The SDD Panel docks at the bottom (or can be detached). Five tabs cover the full lifecycle.

---

## Mockup 1 — Pipeline Tab

The **home screen** of the plugin. Shows current workflow state at a glance.

```
┌─────────────────────────────────────────────────────────────────────┐
│  SDD PANEL                                          [?] [Settings]  │
│  Pipeline  │  Spec  │  Plan  │  Review  │  History                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  CURRENT FEATURE:  "JWT Authentication with Refresh Tokens"         │
│                                                                     │
│  ●──────────●──────────●──────────◌──────────◌──────────◌          │
│  Init     Analyse   Plan      Implement   Review    Archive         │
│  ✓          ✓         ✓       IN PROGRESS                           │
│                                                                     │
│  IMPLEMENTATION PROGRESS                                            │
│  ████████████████████░░░░░░░░░░░░░░  58%  (7 / 12 ACs passing)     │
│                                                                     │
│  Plan Steps                              Status                     │
│  ─────────────────────────────────────────────────────────────     │
│  ✓  1. Create DB migration (users table)  Done                      │
│  ✓  2. Define User entity + UserRole enum Done                      │
│  ✓  3. UserRepository with email lookup  Done                       │
│  ✓  4. JwtService (sign / verify tokens) Done                       │
│  ▶  5. AuthService (login, refresh)      In Progress                │
│  ○  6. AuthController (/login, /refresh) Pending                    │
│  ○  7. SecurityConfig (filter chain)     Pending                    │
│  ○  8. Integration tests                 Pending                    │
│                                                                     │
│  [▶ Continue Implement]   [⏸ Pause]   [↩ Re-run Step 5]            │
│                                                                     │
│  Last action: 14:32 — AuthService skeleton created                  │
└─────────────────────────────────────────────────────────────────────┘
```

**Key elements:**
- Step-by-step progress dots (filled = done, half = active, empty = pending)
- AC pass count shown as a progress bar
- Each plan step shows status; active step is highlighted
- Action buttons drive the AI agent forward or restart a step
- Timestamp of last AI action

---

## Mockup 2 — Spec Tab (feature.md editor)

A **structured, section-aware editor** for the feature spec. Not raw markdown — each section is a collapsible, editable card.

```
┌─────────────────────────────────────────────────────────────────────┐
│  SDD PANEL                                                          │
│  Pipeline  │  Spec  │  Plan  │  Review  │  History                  │
├─────────────────────────────────────────────────────────────────────┤
│  FEATURE SPEC                      [Edit Raw]  [Refine...]  [⟳]    │
│                                                                     │
│  JWT Authentication with Refresh Tokens          v3  •  2026-04-27 │
│                                                                     │
│  ▼  SUMMARY                                                         │
│  │  Add JWT-based stateless authentication with short-lived         │
│  │  access tokens (15 min) and long-lived refresh tokens (7 days).  │
│                                                                     │
│  ▼  ACCEPTANCE CRITERIA                  [7 / 12 ✓]  ████████░░░   │
│  │  ✓  AC-01  POST /auth/login returns access + refresh tokens      │
│  │  ✓  AC-02  Access token expires after 15 minutes                 │
│  │  ✓  AC-03  Refresh token valid for 7 days                        │
│  │  ✓  AC-04  POST /auth/refresh issues new token pair              │
│  │  ✓  AC-05  Invalid credentials return 401                        │
│  │  ✗  AC-06  Revoked refresh token returns 401                     │
│  │  ✗  AC-07  Rate limit: max 5 failed attempts / minute            │
│  │  ✗  AC-08  Expired access token returns 401 with WWW-Authenticate│
│  │  ...  (4 more)                                         [Show all]│
│                                                                     │
│  ▶  USER STORIES                         (collapsed)                │
│  ▶  FUNCTIONAL REQUIREMENTS              (collapsed)                │
│  ▶  OUT OF SCOPE                         (collapsed)                │
│  ▶  OPEN QUESTIONS                  ⚠ 2 unresolved                  │
│                                                                     │
│  REVISION HISTORY                                                   │
│  v3  2026-04-27  Added rate limiting requirement (AC-07)            │
│  v2  2026-04-25  Clarified token expiry times                       │
│  v1  2026-04-24  Initial specification                              │
└─────────────────────────────────────────────────────────────────────┘
```

**Key elements:**
- Section cards collapse/expand independently
- AC list shows checked/unchecked state with live count and mini progress bar
- "Refine..." button opens a dialog to describe what to change and fires `/sdd-refine`
- Revision history always visible at the bottom
- "Edit Raw" drops into a plain markdown editor for power users
- Open questions section badges when unresolved items exist

---

## Mockup 3 — Plan Tab

An **interactive checklist** of the implementation plan with file links.

```
┌─────────────────────────────────────────────────────────────────────┐
│  SDD PANEL                                                          │
│  Pipeline  │  Spec  │  Plan  │  Review  │  History                  │
├─────────────────────────────────────────────────────────────────────┤
│  IMPLEMENTATION PLAN                          [Edit Raw]  [Re-plan] │
│                                                                     │
│  Approach: Layered architecture — schema → domain → service →       │
│            infrastructure → API → tests                             │
│                                                                     │
│  ✓  Step 1 — Database Migration                                     │
│  │  ✓  Create migration V3__add_users_table.sql                     │
│  │  ✓  Create migration V4__add_refresh_tokens_table.sql            │
│  │     📄 src/main/resources/db/migration/V3__add_users_table.sql   │
│                                                                     │
│  ✓  Step 2 — Domain Model                                           │
│  │  ✓  User.java entity with @Table("users")                        │
│  │  ✓  UserRole.java enum (ROLE_USER, ROLE_ADMIN)                   │
│  │  ✓  RefreshToken.java entity                                     │
│  │     📄 src/main/java/com/example/domain/User.java                │
│                                                                     │
│  ▶  Step 5 — AuthService  ← ACTIVE                                  │
│  │  ✓  authenticate(email, password): TokenPair                     │
│  │  ○  refreshToken(token): TokenPair                               │
│  │  ○  revokeToken(token): void                                     │
│  │     📄 src/main/java/com/example/service/AuthService.java        │
│                                                                     │
│  ○  Step 6 — AuthController                                         │
│  │  ○  POST /auth/login → AuthResponse                              │
│  │  ○  POST /auth/refresh → AuthResponse                            │
│  │  ○  POST /auth/logout → 204                                      │
│                                                                     │
│  ACCEPTANCE CRITERIA COVERAGE MAP                                   │
│  AC-01 → testLogin_returnsTokenPair()          ✓ passing            │
│  AC-02 → testAccessToken_expiresAfter15min()   ✓ passing            │
│  AC-06 → testRevokedToken_returns401()         ✗ not written yet    │
└─────────────────────────────────────────────────────────────────────┘
```

**Key elements:**
- Steps expand to show sub-tasks; each sub-task has a checkbox
- File links open the relevant file directly in the editor
- Active step highlighted with arrow
- AC-to-test coverage map at the bottom — clicking a test name jumps to the test file

---

## Mockup 4 — Review Tab

Shows review findings as **structured inspection results**, similar to IDE problem lists.

```
┌─────────────────────────────────────────────────────────────────────┐
│  SDD PANEL                                                          │
│  Pipeline  │  Spec  │  Plan  │  Review  │  History                  │
├─────────────────────────────────────────────────────────────────────┤
│  CODE REVIEW                        [Run Review]  [Export Report]   │
│                                                                     │
│  Last run: 2026-04-27 15:10                                         │
│  Verdict: 🟡  MERGE AFTER MINOR FIXES                               │
│                                                                     │
│  Findings                                               Filter ▾    │
│  ─────────────────────────────────────────────────────────────────  │
│  🔴 CRITICAL  (0)                                                   │
│                                                                     │
│  🟠 MAJOR  (1)                               [Expand all]           │
│  ▼  AuthService.java:84  — Missing @Transactional on revokeToken()  │
│  │  revokeToken() performs two writes without a transaction          │
│  │  boundary. A partial failure leaves tokens in an inconsistent    │
│  │  state.                                                          │
│  │  [Jump to file]  [Ask AI to fix]                                 │
│                                                                     │
│  🟡 MINOR  (3)                                                      │
│  ▶  AuthController.java:31  — 200 returned instead of 201 on login  │
│  ▶  JwtService.java:12  — Magic numbers (900, 604800) should be     │
│  │  named constants                                                  │
│  ▶  UserRepository.java:8  — findByEmail() should return            │
│  │  Optional<User>, not User                                        │
│                                                                     │
│  🔵 INFO  (2)                                                       │
│  ▶  AuthService.java:55  — Consider extracting token-pair builder   │
│  ▶  Add Micrometer counter for failed login attempts                │
│                                                                     │
│  ACCEPTANCE CRITERIA COVERAGE                                       │
│  ✓  AC-01 through AC-05  — Tests present and passing                │
│  ✗  AC-06  — No test for revoked token scenario                     │
│  ✗  AC-07  — Rate limiting not implemented                          │
└─────────────────────────────────────────────────────────────────────┘
```

**Key elements:**
- Verdict badge matches the four-tier system (✅ / 🟡 / 🟠 / 🔴)
- Findings grouped by severity, expandable
- Each finding links directly to the relevant file and line
- "Ask AI to fix" button fires the AI agent targeting that specific finding
- AC coverage matrix shows which criteria still need test coverage
- Inline annotations also appear in the editor gutter (like IDE inspections)

---

## Mockup 5 — History Tab (Feature Archive)

A **timeline of completed features** pulled from `docs/specs-archive/`.

```
┌─────────────────────────────────────────────────────────────────────┐
│  SDD PANEL                                                          │
│  Pipeline  │  Spec  │  Plan  │  Review  │  History                  │
├─────────────────────────────────────────────────────────────────────┤
│  FEATURE HISTORY                                     [Search...]    │
│                                                                     │
│  2026-04-27  ●  JWT Authentication with Refresh Tokens              │
│  │              12 ACs  •  8 files changed  •  Review: 🟡           │
│  │              [View Spec]  [View Plan]  [View Review]             │
│                                                                     │
│  2026-04-20  ●  User Profile Management                             │
│  │              9 ACs  •  5 files changed  •  Review: ✅            │
│  │              [View Spec]  [View Plan]  [View Review]             │
│                                                                     │
│  2026-04-15  ●  Email Verification Flow                             │
│  │              6 ACs  •  4 files changed  •  Review: ✅            │
│              [View Spec]  [View Plan]  [View Review]                │
│                                                                     │
│  ───────────────────────────────────────────────────────────────   │
│  PROJECT CONTEXT (docs/project.md)                                  │
│                                                                     │
│  Tech Stack:  Java 21 / Spring Boot 3.3 / PostgreSQL / JUnit 5     │
│  Architecture: Layered (controller → service → repository)          │
│  REST Base: /api/v1                                                 │
│                                                                     │
│  API Surface (12 endpoints)                                [Expand] │
│  Architecture Decisions (5)                                [Expand] │
│  Approved Dependencies (8)                                 [Expand] │
│                                                                     │
│  [Edit project.md]                                                  │
└─────────────────────────────────────────────────────────────────────┘
```

**Key elements:**
- Chronological list of archived features with AC count and review verdict
- Each entry links to archived spec, plan, and review report
- Bottom section surfaces the live `docs/project.md` summary — tech stack, API surface, decisions
- "Edit project.md" opens the file directly

---

## Mockup 6 — New Feature Dialog

Triggered by **File > New Feature** or the "+" button on the Pipeline tab.

```
┌────────────────────────────────────────────────────────────┐
│  New Feature                                           [×] │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Describe the feature you want to build:                   │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Add password reset via email with a secure         │   │
│  │  time-limited token (expires after 1 hour).         │   │
│  │                                                     │   │
│  └────────────────────────────────────────────────────┘   │
│                                                            │
│  Workflow Mode                                             │
│  ◉  Full pipeline  (analyse → plan → implement → review)  │
│  ○  Spec only      (analyse → refine → hand off)          │
│  ○  Yolo           (full pipeline, minimal questions)      │
│                                                            │
│  Implementation Mode                                       │
│  ◉  Standard   ○  TDD (strict Red → Green → Refactor)     │
│                                                            │
│  [Cancel]                              [▶ Start Analysis]  │
└────────────────────────────────────────────────────────────┘
```

**Key elements:**
- Free-text description field
- Workflow mode selector (full pipeline, spec-only for BA handoff, or yolo)
- Implementation mode toggle between standard and TDD
- "Start Analysis" fires `/sdd-analyse` and switches to the Spec tab

---

## Mockup 7 — Inline Gutter Annotations

While the editor is open, the plugin annotates code with SDD context **in the gutter** (left margin), similar to how code coverage or Git blame appear.

```
  AuthService.java

  84  │ 🟠│  public void revokeToken(String token) {
  85  │   │      refreshTokenRepository.deleteByToken(token);
  86  │   │      auditLogRepository.save(new AuditLog(...));
  87  │   │  }
```

- 🔴/🟠/🟡/🔵 gutter icons mark lines with review findings
- Hovering shows the finding text inline (tooltip)
- Clicking the icon opens the Review tab filtered to that finding

Additionally, a **sticky breadcrumb** in the editor status bar shows:

```
  SDD: JWT Auth  •  Step 5/8  •  AC: 7/12  •  Review: 🟡  |  [Open SDD Panel]
```

---

## Mockup 8 — Refine Dialog

Triggered by the **"Refine..."** button on the Spec tab.

```
┌────────────────────────────────────────────────────────────┐
│  Refine Spec                                           [×] │
├────────────────────────────────────────────────────────────┤
│  Current spec:  JWT Authentication with Refresh Tokens     │
│                                                            │
│  What needs to change?                                     │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Add rate limiting — max 5 failed login attempts    │   │
│  │  per minute per IP address. Return 429 Too Many    │   │
│  │  Requests.                                         │   │
│  └────────────────────────────────────────────────────┘   │
│                                                            │
│  Impact assessment                                         │
│  ☑  Update feature.md                                     │
│  ☑  Assess impact on plan.md                              │
│  ☐  Automatically re-plan if steps are invalidated        │
│                                                            │
│  [Cancel]                                   [▶ Refine]    │
└────────────────────────────────────────────────────────────┘
```

After running, the plugin shows a diff summary of what changed in `feature.md` and which plan steps (if any) are now invalidated — with a prompt to re-plan.

---

## Key Plugin Capabilities Summary

### AI Agent Integration
- The plugin drives the AI agent (Claude Code, Copilot, Cursor, etc.) by constructing and sending skill commands
- Streams agent output into a log pane within the SDD panel
- Parses agent output to extract plan step completions, AC check-offs, and review findings
- Supports "pause and resume" — user can interrupt mid-implementation and pick up later

### File Synchronization
- Watches `feature.md`, `plan.md`, and `docs/project.md` for external changes
- Re-parses and refreshes panel content whenever these files are modified
- AC checkbox state synced bidirectionally (check in panel ↔ updates markdown file)

### Navigation
- Every file reference in plan steps is a clickable link that opens the file in the editor
- Review findings jump to exact line numbers
- Plan step → test method → AC traceability is navigable

### Status Bar Integration
- Persistent SDD status indicator in the IDE status bar
- Shows: current feature name, active step, AC progress, and review verdict
- Click to open/focus the SDD panel

### Project Setup Wizard
- First-time setup detects missing `docs/project.md` and offers to run `/sdd-init`
- Walks through the init process with a guided wizard UI
- Displays detected tech stack for confirmation before writing

---

## Implementation Notes

### JetBrains Plugin
- **Tool Window** API for the docked SDD panel
- **FileEditorManagerListener** for gutter annotations
- **VirtualFileListener** for watching `feature.md` / `plan.md` changes
- **StatusBarWidget** for the status bar indicator
- Markdown parsing via `intellij-markdown` library
- AI agent integration via **AI Assistant plugin** extension points or terminal subprocess

### VS Code Extension
- **TreeDataProvider** for the pipeline/plan views in the side bar
- **WebviewPanel** for richer spec and review UIs
- **TextEditorDecorationType** for gutter annotations
- **FileSystemWatcher** for file sync
- **StatusBarItem** for the status indicator
- Terminal integration via `vscode.window.createTerminal` to run skill commands

---

## Phased Delivery

| Phase | Scope |
|---|---|
| **Phase 1 — MVP** | Pipeline tab (progress view), Plan tab (checklist with file links), Status bar indicator |
| **Phase 2 — Spec editing** | Spec tab with structured AC editor, Refine dialog, revision history |
| **Phase 3 — Review integration** | Review tab with grouped findings, gutter annotations, "Ask AI to fix" |
| **Phase 4 — History & project context** | History tab, project.md summary, feature archive browser |
| **Phase 5 — Deep AI integration** | Streaming agent output, pause/resume, automatic AC check-off parsing |

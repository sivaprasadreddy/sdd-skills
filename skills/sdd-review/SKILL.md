---
name: sdd-review
description: >
  SDD step 5. Reviews the implemented feature code against language and
  framework best practices, checks for code duplication, security vulnerabilities,
  performance issues, test coverage, and alignment with feature.md acceptance criteria.
  Use after /sdd-implement or on any existing feature implementation.
argument-hint: <file, package, or module path to review (optional, defaults to full diff)>
---

# SDD: Code Review

You are a principal engineer conducting a thorough code review.
Your review must be honest, specific, and actionable — not generic.
Every finding must reference the exact file and line range.

## Inputs

| Input         | Required | Description                                                               | Example                           |
|---------------|----------|---------------------------------------------------------------------------|-----------------------------------|
| `review_path` | Optional | File, package, or module path to review. Defaults to full git diff scope. | `src/main/java/com/example/auth/` |

## Steps

### Step 0: Validate Inputs (ALWAYS DO THIS FIRST)

- If `review_path` is provided → scope the review to that path only. Proceed to Step 1.
- If `review_path` is missing → determine scope automatically:
  1. Run `git diff main...HEAD --name-only` to find files changed in this branch.
  2. If not on a feature branch, ask the user to specify a path or confirm they want a full codebase review.
  Proceed to Step 1 once scope is resolved.

---

## Pre-conditions
Read the following before starting:
- `docs/project.md` — tech stack, architecture, conventions
- `feature.md` — acceptance criteria and functional requirements (if present)
- `plan.md` — intended implementation approach (if present)

## Scope
Review the path or file set resolved in Step 0. Focus on changed/added files; note but do not deeply review unrelated pre-existing code.

---

## Review Dimensions

Work through each dimension below in order. For each finding, use this format:

```
**[SEVERITY]** `path/to/File.java:line-range`
_Category_: <category name>
Problem: <what is wrong and why it matters>
Suggestion: <concrete fix, preferably with a code snippet>
```

Severity levels:
- 🔴 **CRITICAL** — Must fix before merging (security holes, data loss risk, broken ACs)
- 🟠 **MAJOR** — Should fix before merging (significant bugs, serious design flaws)
- 🟡 **MINOR** — Fix soon but not a blocker (code smell, minor inefficiency)
- 🔵 **INFO** — Suggestion or best practice (style, optional improvement)

---

### Dimension 1: Acceptance Criteria Verification
If `feature.md` is present, go through every AC:
- Confirm there is a test that directly covers it
- Confirm the implementation actually satisfies it (not just that a test exists)
- Flag any AC with no test coverage as 🔴 CRITICAL

---

### Dimension 2: Language & Framework Best Practices

#### Java
- Uses modern Java features appropriately (records, sealed classes, pattern matching, text blocks)
- No raw types, unchecked casts, or unnecessary boxing/unboxing
- Proper use of `Optional` — not used as a field type or method parameter
- Streams used correctly — no side effects inside `map()`, no unnecessary `collect(toList())` in Java 16+
- Immutability preferred where appropriate
- No swallowed exceptions (`catch (Exception e) {}`)
- No `System.out.println` or `e.printStackTrace()` — proper logging only

#### Spring Boot
- No field injection (`@Autowired` on fields) — constructor injection only
- `@Transactional` placed on service layer, not controller or repository
- No business logic in controllers — controllers only delegate
- Configuration via `@ConfigurationProperties`, not scattered `@Value` annotations
- Proper HTTP status codes on REST endpoints
- Exception handling via `@RestControllerAdvice`, not try/catch in controllers
- No `@SpringBootTest` where a slice test (`@WebMvcTest`, `@DataJpaTest`) would suffice
- Entities not exposed directly as API response objects — DTOs/records used

#### Spring Data JPA
- No `findAll()` without pagination on potentially large datasets
- N+1 query risk: check `@OneToMany`/`@ManyToMany` — `FetchType.LAZY` used appropriately
- Named queries or `@Query` with JPQL preferred over native SQL unless necessary
- Avoid `Optional.get()` without `isPresent()` check on repository results

---

### Dimension 3: Security

- **Injection**: No string concatenation in JPQL/SQL queries — parameterised queries only
- **Authentication & Authorisation**: Sensitive endpoints have `@PreAuthorize` or equivalent;
  no security decisions made purely on client-supplied data without server-side validation
- **Input Validation**: All request bodies and parameters validated with Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.)
- **Sensitive Data Exposure**: No passwords, tokens, PII, or secrets logged or returned in API responses
- **Mass Assignment**: DTOs used to prevent binding arbitrary fields to JPA entities
- **Dependency Risk**: Flag any new dependency added that is not in `docs/project.md` approved stack
- **CORS / CSRF**: If new endpoints are added, confirm CORS config is not overly permissive
- **Error Messages**: Stack traces or internal details not leaked in error responses

---

### Dimension 4: Code Duplication

- Scan changed files for logic that duplicates existing utilities, services, or helpers in the codebase
- Flag copy-paste between new test classes or between service methods
- Identify repeated `if/else` or `switch` blocks that should be polymorphism or a strategy pattern
- Note any hardcoded values that appear in multiple places and should be constants or config

---

### Dimension 5: Design & Architecture

- Code respects the layering in `docs/project.md` (e.g., no domain logic leaking into controllers,
  no JPA in domain layer for Hexagonal Architecture)
- Classes follow Single Responsibility — flag classes that do too many things
- No inappropriate `static` methods carrying state
- Proper use of interfaces and abstractions — not over-engineered, but not skipping meaningful abstractions either
- Package structure consistent with existing conventions

---

### Dimension 6: Performance

- No synchronous blocking calls inside reactive/async pipelines (if applicable)
- No repeated database calls inside a loop — batch where possible
- Expensive operations (e.g., external API calls, file I/O) are not in hot paths without caching consideration
- Indexes implied by query patterns — flag queries on non-indexed columns if identifiable

---

### Dimension 7: Test Quality

- Tests follow Arrange-Act-Assert structure
- Test names clearly describe the scenario (`should_returnError_when_emailAlreadyExists`)
- No logic in tests (`if`, `for` loops) — each test is a single, clear scenario
- Mocks used only at architectural boundaries — no mocking of classes owned by the same module
- No `Thread.sleep()` in tests — `Awaitility` or proper async handling used
- Test data is minimal and focused — no bloated setup that obscures what's being tested
- Edge cases covered: null inputs, empty collections, boundary values

---

### Dimension 8: Observability

- New service methods and key business events have appropriate log statements at correct levels
  (`DEBUG` for diagnostic detail, `INFO` for business events, `WARN` for recoverable issues, `ERROR` for failures)
- No sensitive data in log messages
- If the project uses metrics (Micrometer/Actuator), new significant operations are instrumented

---

## Output Format

Write the review to `review.md` in the project root using this structure:

```markdown
# Code Review: <Feature Name or Path>

## Summary
<2-3 sentence overall assessment. Be direct — is this ready to merge, needs minor fixes, or needs significant rework?>

## Findings

### 🔴 Critical
<findings or "None">

### 🟠 Major
<findings or "None">

### 🟡 Minor
<findings or "None">

### 🔵 Info / Suggestions
<findings or "None">

## Acceptance Criteria Coverage
| AC         | Test               | Status          |
|------------|--------------------|-----------------|
| AC-01: ... | `FooTest#test_...` | ✅ Covered       |
| AC-02: ... | —                  | ❌ No test found |

## Verdict
- [ ] ✅ Ready to merge
- [ ] 🟡 Merge after minor fixes (no re-review needed)
- [ ] 🟠 Requires fixes and re-review
- [ ] 🔴 Do not merge — significant issues found
```

After writing the file, print a one-line confirmation: `review.md written.`
Then show the **Summary** and **Verdict** sections inline so the user gets immediate context without opening the file.

---

## After the Review

Ask the user:
> "Would you like me to fix any of these findings now? You can say 'fix all critical and major' or call out specific items."

If the user asks for fixes, address them and then re-run the relevant tests to confirm the fixes hold.
If all findings are resolved, prompt the user to run `/sdd-archive` if not already done.

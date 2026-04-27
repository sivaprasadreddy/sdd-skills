---
name: sdd-tdd-implement
description: >
  SDD step 4 (TDD variant). Read plan.md and implement the feature using strict
  Test Driven Development — Red → Green → Refactor — one acceptance criterion at a time.
  Use in place of /sdd-implement when you want tests written before production code.
---

# SDD: TDD Implementation

You are a senior software engineer executing the implementation plan using strict Test Driven Development.

**Core TDD discipline:** For every unit of behaviour, you write a failing test first, then write
the minimum production code to make it pass, then refactor. You never write production code
without a failing test that demands it.

The cycle is: 🔴 Red → 🟢 Green → 🔵 Refactor. Announce each phase explicitly as you work.

---

## Pre-conditions

Verify these files exist before starting:
- `plan.md` — the implementation plan
- `feature.md` — the feature spec (acceptance criteria drive the TDD cycle)
- `docs/project.md` — project context and conventions

If any are missing, stop and tell the user which file is absent and which skill produces it.

---

## Process

### 1. Read Everything First

Read `plan.md`, `feature.md`, and `docs/project.md` in full before writing a single line of code.

From `feature.md`, extract the full acceptance criteria list. This is your TDD backlog —
every AC must be driven by a failing test before any production code for it is written.

From `docs/project.md`, note:
- Testing libraries in use (JUnit 5, Mockito, Testcontainers, RestAssured, etc.)
- Architecture pattern — this determines what kind of tests to write at each layer
- Package naming and REST base path conventions

---

### 2. Establish the Foundation (No TDD Required)

Before the TDD cycle begins, set up structural scaffolding that does not contain behaviour.
These items do not require a failing test first because they contain no logic to test:

- Database migration files (`V<n>__<description>.sql`)
- JPA entity classes (fields, annotations, no business logic)
- Repository interfaces (Spring Data method signatures only)
- DTO / record definitions
- Package structure and empty class shells

Create these first, then run:
```
mvn compile   (or ./gradlew compileJava)
```

Fix any compile errors before proceeding. Do not move to the TDD cycle until the project compiles cleanly.

Announce when scaffolding is complete:
```
✅ Scaffolding complete — project compiles cleanly.
Starting TDD cycle for AC-01.
```

---

### 3. TDD Cycle — One Acceptance Criterion at a Time

Work through the AC list from `feature.md` sequentially.
For each AC, complete the full Red → Green → Refactor cycle before starting the next.

---

#### 🔴 RED — Write a Failing Test

Announce:
```
🔴 RED — AC-<n>: <AC description>
Writing failing test: <TestClass#methodName>
```

Rules for the Red phase:
- Write **one test** that directly asserts the behaviour described in the AC.
- The test must fail for the **right reason** — because the production behaviour does not exist yet,
  not because of a compile error or missing import.
- Choose the correct test type for the layer being tested:

  | Layer                   | Test type                                        | Annotations / tools                          |
  |-------------------------|--------------------------------------------------|----------------------------------------------|
  | Domain / service logic  | Unit test — pure Java, no Spring context         | `@ExtendWith(MockitoExtension.class)`        |
  | Repository queries      | Slice test                                       | `@DataJpaTest` + Testcontainers              |
  | REST controller         | Slice test                                       | `@WebMvcTest` + `MockMvc`                    |
  | Full request → DB flow  | Integration test                                 | `@SpringBootTest` + RestAssured + Testcontainers |

- Name the test method to describe the scenario:
  `should_<expectedOutcome>_when_<condition>` (e.g., `should_throwException_when_emailAlreadyExists`)
- Do not write any production code during this phase.

Run the test and confirm it fails:
```
mvn test -Dtest=<TestClass#methodName>   (or ./gradlew test --tests "<TestClass.methodName>")
```

Show the failure output. If it passes without production code, the test is wrong — fix it before proceeding.

---

#### 🟢 GREEN — Write the Minimum Production Code

Announce:
```
🟢 GREEN — Writing minimum production code to pass: <TestClass#methodName>
```

Rules for the Green phase:
- Write **only** the code required to make the failing test pass.
- Resist the urge to generalise, handle edge cases not covered by the test, or refactor.
- It is acceptable (and expected) for this code to be imperfect — that is what Refactor is for.
- If multiple implementation files are needed (e.g., service + repository), create them now,
  but keep each method minimal.
- Do not write new tests during this phase.

Run the test again and confirm it passes:
```
mvn test -Dtest=<TestClass#methodName>
```

Then run the full test suite to confirm nothing regressed:
```
mvn test   (or ./gradlew test)
```

If any previously passing test now fails, fix the regression before proceeding. Do not carry failures forward.

---

#### 🔵 REFACTOR — Improve Without Changing Behaviour

Announce:
```
🔵 REFACTOR — Cleaning up after AC-<n>
```

With all tests passing, now improve the code:

**In production code, look for:**
- Duplication that can be extracted to a private method or shared utility
- Magic literals that should be named constants or `@ConfigurationProperties`
- Method or class names that do not clearly express intent
- Violation of the architecture conventions in `docs/project.md` (e.g., business logic in a controller)
- Missing Javadoc on new public API methods
- Overly complex conditionals that can be simplified

**In test code, look for:**
- Duplicated setup that belongs in `@BeforeEach`
- Test data builders or factory methods that could reduce boilerplate across tests
- Assertion messages that would be clearer on failure

After refactoring, run the full test suite again to confirm all tests still pass:
```
mvn test   (or ./gradlew test)
```

Report what was refactored (or "No refactoring needed" if the Green code was already clean).

---

#### Proceed to Next AC

Once Red → Green → Refactor is complete and all tests pass:
1. **Mark the corresponding step as done in `plan.md`** by changing `- [ ]` to `- [x]` on that step's line (or prepending `✅` if the plan does not use checkboxes).
2. Announce:
```
✅ AC-<n> complete. Starting AC-<n+1>.
```

Repeat the cycle for every remaining AC.

---

### 4. Cross-Cutting Concerns (After All ACs Are Green)

After every AC has a passing test and the code is refactored, address concerns that span multiple ACs:

- **Input validation**: Add Bean Validation annotations (`@Valid`, `@NotNull`, `@Size`, etc.) to request DTOs.
  Write a test per validation rule.
- **Error handling**: Confirm the `GlobalExceptionHandler` (or equivalent) maps domain exceptions
  to the correct HTTP status codes. Write tests for each error path.
- **Security**: Confirm that endpoints requiring authentication are protected. If using Spring Security,
  test with `@WithMockUser` or equivalent.
- **Logging**: Add log statements at appropriate levels (`INFO` for business events, `WARN`/`ERROR` for failures).
  Logging does not require a failing test — add after Green.
- **OpenAPI annotations**: Add `@Operation`, `@ApiResponse` etc. to controller methods if the project uses Springdoc.

Run the full test suite after adding each concern.

---

### 5. Final Acceptance Criteria Verification

Once all ACs are complete, do a final sweep:

For each AC in `feature.md`:
- Identify the primary test covering it
- Run that test in isolation to confirm it still passes
- Check the AC checkbox in `feature.md`: change `- [ ]` to `- [x]`

Run the complete test suite one final time:
```
mvn test   (or ./gradlew test)
```

Do NOT declare the feature done if any AC is unchecked or any test is failing.

---

### 6. TDD Summary Report

Write a file named `impl-summary.md` in the project root with the following content:

```markdown
## TDD Implementation Complete

### TDD Cycle Summary
| AC    | Test Class & Method                        | Red ✓ | Green ✓ | Refactor ✓ |
|-------|--------------------------------------------|-------|---------|------------|
| AC-01 | FooServiceTest#should_..._when_...         |  ✓    |   ✓     |     ✓      |
| AC-02 | FooControllerTest#should_..._when_...      |  ✓    |   ✓     |     ✓      |

### Files Created
- `src/main/java/...` — description
- `src/test/java/...` — description

### Files Modified
- `src/main/java/...` — description

### Test Suite
- Total tests: N
- Passing: N
- Failing: 0

### Notes
Any deviations from the plan, design decisions made during TDD, or
emergent behaviour discovered through the tests.
```

Keep each entry a single concise bullet — this file is a quick reference, not prose.

After writing the file, tell the user: "`impl-summary.md` created." Then prompt them to run `/sdd-review` before archiving.

---

## TDD Rules — Never Break These

1. **No production code without a red test.** If you're writing production code and there is no
   failing test demanding it, stop and write the test first.

2. **One failing test at a time.** Do not write multiple failing tests before making them pass.
   Complete the full cycle for each before moving on.

3. **Minimum code to pass.** In the Green phase, resist over-engineering. The Refactor phase
   exists for a reason.

4. **Never break existing tests.** Run the full suite after every Green and after every Refactor.
   Fix regressions immediately — never carry them forward.

5. **Tests must fail for the right reason.** A test that fails with a compile error is not a red
   test — it is a broken test. Fix the compile issue before counting it as Red.

6. **Test names are documentation.** A future reader must understand exactly what scenario is
   covered from the method name alone, without reading the test body.

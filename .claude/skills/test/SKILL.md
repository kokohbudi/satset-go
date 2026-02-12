---
name: test
description: Test generation and test running command. Creates and executes tests for code.
user-invocable: true
argument-hint: [file, feature, or 'coverage']
allowed-tools: Read, Glob, Grep, Bash, Write, Edit
---

# /test - Test Generation and Execution

$ARGUMENTS

---

## Purpose

This command generates tests, runs existing tests, or checks test coverage.

### Before Starting

Read and apply knowledge from:
- `.agent/agents/test-engineer.md` for testing methodology
- `.agent/skills/testing-patterns/SKILL.md` for patterns

---

## Sub-commands

```
/test                - Run all tests
/test [file/feature] - Generate tests for specific target
/test coverage       - Show test coverage report
/test watch          - Run tests in watch mode
```

---

## Behavior

### Generate Tests

When asked to test a file or feature:

1. **Analyze the code**
   - Identify functions and methods
   - Find edge cases
   - Detect dependencies to mock

2. **Generate test cases**
   - Happy path tests
   - Error cases
   - Edge cases
   - Integration tests (if needed)

3. **Write tests**
   - Use project's test framework
   - Follow existing test patterns
   - Mock external dependencies

---

## Output Format

### For Test Generation

```markdown
## Tests: [Target]

### Test Plan
| Test Case | Type | Coverage |
|-----------|------|----------|
| Should create user | Unit | Happy path |
| Should reject invalid email | Unit | Validation |
| Should handle db error | Unit | Error case |

### Generated Tests

`tests/[file].test.ts`

[Code block with tests]

---

Run with: `npm test` or `./mvnw test`
```

### For Test Execution

```
Running tests...

auth.test.ts (5 passed)
user.test.ts (8 passed)
order.test.ts (2 passed, 1 failed)

Failed:
  x should calculate total with discount
    Expected: 90
    Received: 100

Total: 15 tests (14 passed, 1 failed)
```

---

## Examples

```
/test src/services/auth.service.ts
/test user registration flow
/test coverage
/test fix failed tests
```

---

## Key Principles

- **Test behavior not implementation**
- **One assertion per test** (when practical)
- **Descriptive test names**
- **Arrange-Act-Assert pattern**
- **Mock external dependencies**

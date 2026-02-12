---
name: tdd-workflow
description: Test-Driven Development workflow principles. RED-GREEN-REFACTOR cycle.
user-invocable: true
argument-hint: [feature | module | function]
allowed-tools: Read, Glob, Grep, Write, Edit, Bash
---

# /tdd-workflow - Test-Driven Development

$ARGUMENTS

---

## Purpose

Guide TDD workflow using the RED-GREEN-REFACTOR cycle. Write failing tests first, make them pass, then refactor.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/tdd-workflow/SKILL.md` for TDD principles
- `.agent/agents/test-engineer.md` for testing methodology

---

## Behavior

When `/tdd-workflow` is triggered:

1. **RED phase** - Write a failing test
   - Define expected behavior
   - Write the simplest test that fails
   - Verify it fails for the right reason

2. **GREEN phase** - Make it pass
   - Write minimum code to pass
   - Don't over-engineer
   - Just make the test green

3. **REFACTOR phase** - Clean up
   - Remove duplication
   - Improve naming
   - Keep tests passing

4. **Repeat**
   - Add next behavior as a test
   - Follow the three laws of TDD

---

## Examples

```
/tdd-workflow implement user registration
/tdd-workflow add validation to payment module
/tdd-workflow refactor with test safety net
/tdd-workflow guide TDD for REST endpoint
```

---

## Key Principles

- **Test behavior not implementation**
- **One test at a time**
- **Arrange-Act-Assert pattern**
- **When to use TDD** - Not everything needs TDD

---
name: lint-and-validate
description: Automatic quality control, linting, and static analysis procedures. Use after code modifications.
user-invocable: true
argument-hint: [file | project | fix]
allowed-tools: Read, Glob, Grep, Bash
---

# /lint-and-validate - Code Quality Control

$ARGUMENTS

---

## Purpose

Automatic quality control, linting, and static analysis. Ensures syntax correctness and project standards compliance after every code modification.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/lint-and-validate/SKILL.md` for linting procedures
- `.agent/agents/qa-automation-engineer.md` for QA expertise

---

## Behavior

When `/lint-and-validate` is triggered:

1. **Detect ecosystem**
   - Node.js/TypeScript, Python, Java, or other
   - Find existing lint configuration

2. **Run quality checks**
   - Ecosystem-specific linting (ESLint, Pylint, Checkstyle, etc.)
   - Type checking
   - Static analysis

3. **The Quality Loop**
   - Run checks → Fix issues → Re-run until clean
   - Error handling rules

4. **Validate**
   - Run `.agent/skills/lint-and-validate/scripts/lint_runner.py` if applicable
   - Run `.agent/skills/lint-and-validate/scripts/type_coverage.py` for type coverage

---

## Examples

```
/lint-and-validate run all checks
/lint-and-validate fix linting errors
/lint-and-validate check type coverage
/lint-and-validate validate before commit
```

---

## Strict Rule

> No code committed without passing checks.

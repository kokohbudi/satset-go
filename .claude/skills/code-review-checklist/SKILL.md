---
name: code-review-checklist
description: Code review guidelines covering code quality, security, and best practices.
user-invocable: true
argument-hint: [file or PR to review]
allowed-tools: Read, Glob, Grep
---

# /code-review-checklist - Code Review

$ARGUMENTS

---

## Purpose

Systematic code review covering correctness, security, performance, code quality, testing, and documentation. Includes AI & LLM review patterns.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/code-review-checklist/SKILL.md` for review guidelines
- `.agent/agents/security-auditor.md` for security review expertise

---

## Behavior

When `/code-review-checklist` is triggered:

1. **Quick review checklist**
   - Correctness: Does the code do what it's supposed to?
   - Security: Any vulnerabilities?
   - Performance: Obvious inefficiencies?
   - Code Quality: Clean, readable, maintainable?
   - Testing: Adequate test coverage?
   - Documentation: Updated where needed?

2. **AI & LLM review patterns**
   - Prompt engineering review
   - Anti-patterns to flag

3. **Provide feedback**
   - Be specific about what needs to change
   - Explain why, not just what
   - Suggest alternatives when possible

---

## Examples

```
/code-review-checklist src/controllers/AuthController.java
/code-review-checklist review this PR for security issues
/code-review-checklist check code quality of utils module
```

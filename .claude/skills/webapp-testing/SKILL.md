---
name: webapp-testing
description: Web application testing principles. E2E, Playwright, deep audit strategies.
user-invocable: true
argument-hint: [URL | feature | E2E | audit]
allowed-tools: Read, Glob, Grep, Write, Edit, Bash
---

# /webapp-testing - Web Application Testing

$ARGUMENTS

---

## Purpose

Web application testing including E2E testing with Playwright, deep audit strategies, visual testing, API testing, and CI integration.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/webapp-testing/SKILL.md` for web testing principles
- `.agent/agents/qa-automation-engineer.md` for QA expertise

---

## Behavior

When `/webapp-testing` is triggered:

1. **Deep audit approach**
   - Discovery first — understand the application
   - Testing pyramid for web

2. **E2E test implementation**
   - Playwright principles (Page Object Model, fixtures)
   - Visual testing
   - Test organization

3. **API testing**
   - API testing principles
   - Integration with E2E

4. **CI and optimization**
   - CI integration
   - Parallelization strategies
   - Run `.agent/skills/webapp-testing/scripts/playwright_runner.py` if applicable

---

## Examples

```
/webapp-testing E2E tests for login flow with Playwright
/webapp-testing deep audit of checkout feature
/webapp-testing visual regression testing setup
/webapp-testing CI pipeline for E2E tests
```

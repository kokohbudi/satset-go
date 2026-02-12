---
name: python-patterns
description: Python development principles and decision-making. Framework selection, async patterns, type hints, project structure.
user-invocable: true
argument-hint: [framework | pattern | architecture]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /python-patterns - Python Development Guidance

$ARGUMENTS

---

## Purpose

Expert guidance on Python development including framework selection (FastAPI/Django/Flask), async patterns, type hints, and project structure.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/python-patterns/SKILL.md` for Python principles
- `.agent/agents/backend-specialist.md` for backend expertise

---

## Behavior

When `/python-patterns` is triggered:

1. **Understand context**
   - What type of application?
   - Sync vs async requirements?
   - Framework preference?

2. **Guide decisions**
   - Framework selection (FastAPI, Django, Flask)
   - Project structure principles
   - Type hints strategy

3. **Apply best practices**
   - Django principles (2025 async support)
   - FastAPI principles
   - Background tasks selection
   - Error handling and testing

---

## Examples

```
/python-patterns FastAPI vs Django for this project
/python-patterns async patterns for data pipeline
/python-patterns project structure for monolith
/python-patterns type hints best practices
```

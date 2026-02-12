---
name: clean-code
description: Pragmatic coding standards - concise, direct, no over-engineering, no unnecessary comments.
user-invocable: true
argument-hint: [file or function to review]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /clean-code - Clean Code Review

$ARGUMENTS

---

## Purpose

Applies pragmatic coding standards focused on simplicity, readability, and maintainability. Reviews code for SRP, DRY, KISS, YAGNI violations.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/clean-code/SKILL.md` for clean code principles (PRIORITY: CRITICAL)
- `.agent/agents/code-archaeologist.md` for code analysis expertise

---

## Behavior

When `/clean-code` is triggered:

1. **Analyze code**
   - Read the target file/function
   - Check naming conventions
   - Check function length (max 20 lines)
   - Check SRP compliance

2. **Apply principles**
   - SRP: Single Responsibility
   - DRY: Don't Repeat Yourself
   - KISS: Keep It Simple
   - YAGNI: You Ain't Gonna Need It

3. **Review checklist**
   - Before editing: THINK FIRST
   - Naming rules
   - Function rules
   - Code structure patterns

4. **Self-check before completing**
   - Verify improvements don't add complexity
   - Run verification scripts by agent type if applicable

---

## Examples

```
/clean-code src/services/UserService.java
/clean-code review this function for readability
/clean-code refactor duplicated code in auth module
/clean-code check naming conventions in project
```

---

## Key Principles

- **Concise, direct** - No over-engineering
- **No unnecessary comments** - Code should be self-documenting
- **Think before editing** - Understand context first

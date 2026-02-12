---
name: powershell-windows
description: PowerShell Windows patterns. Critical pitfalls, operator syntax, error handling.
user-invocable: true
argument-hint: [command | script | task]
allowed-tools: Read, Glob, Grep, Write, Edit, Bash
---

# /powershell-windows - PowerShell Guidance

$ARGUMENTS

---

## Purpose

Expert guidance on PowerShell for Windows including critical pitfalls, operator syntax, error handling, and script patterns.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/powershell-windows/SKILL.md` for PowerShell patterns

---

## Behavior

When `/powershell-windows` is triggered:

1. **Critical pitfalls first**
   - Operator syntax rules (parentheses required)
   - Unicode/emoji restriction (ASCII only)
   - Null check patterns

2. **Apply patterns**
   - String interpolation
   - Error handling
   - File paths
   - Array operations
   - JSON operations (CRITICAL: Depth parameter)

3. **Script writing**
   - Script template
   - Common errors and fixes
   - Best practices

---

## Examples

```
/powershell-windows script for file management
/powershell-windows JSON parsing with depth
/powershell-windows error handling patterns
/powershell-windows common pitfalls to avoid
```

---
name: bash-linux
description: Bash/Linux terminal patterns. Critical commands, piping, error handling, scripting.
user-invocable: true
argument-hint: [command | script | task]
allowed-tools: Read, Glob, Grep, Write, Edit, Bash
---

# /bash-linux - Bash/Linux Guidance

$ARGUMENTS

---

## Purpose

Expert guidance on Bash/Linux terminal operations including critical commands, piping, error handling, and script writing.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/bash-linux/SKILL.md` for Bash/Linux patterns
- `.agent/agents/devops-engineer.md` for DevOps expertise

---

## Behavior

When `/bash-linux` is triggered:

1. **Understand context**
   - What operation needed?
   - macOS or Linux?
   - One-off or reusable script?

2. **Apply patterns**
   - Operator syntax (`;`, `&&`, `||`, `|`)
   - File operations essential commands
   - Process management
   - Text processing tools
   - Environment variables
   - Networking commands

3. **Script writing**
   - Script template with error handling
   - Best practices for reliability
   - Safety checks

---

## Examples

```
/bash-linux find and delete old log files
/bash-linux script for automated backups
/bash-linux process management commands
/bash-linux piping and text processing
```

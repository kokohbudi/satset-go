---
name: intelligent-routing
description: Automatic agent selection and intelligent task routing. Analyzes user requests and selects best specialist agent(s).
user-invocable: true
argument-hint: [task description]
allowed-tools: Read, Glob, Grep
---

# /intelligent-routing - Smart Agent Routing

$ARGUMENTS

---

## Purpose

Analyzes user requests and automatically selects the most appropriate specialist agent(s) without requiring explicit mentions. Acts as an intelligent Project Manager.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/intelligent-routing/SKILL.md` for routing rules and agent selection matrix
- `.agent/agents/orchestrator.md` for orchestration patterns

---

## Behavior

When `/intelligent-routing` is triggered:

1. **Analyze request**
   - Extract keywords and domains
   - Assess complexity (SIMPLE/MODERATE/COMPLEX)

2. **Select agents**
   - Use agent selection matrix from knowledge pack
   - Match by keywords and domain detection rules
   - Apply tier-based routing

3. **Route and execute**
   - Load selected agent knowledge files
   - Apply combined expertise to the task
   - Handle edge cases

---

## Examples

```
/intelligent-routing add user authentication
/intelligent-routing optimize database queries
/intelligent-routing fix CSS layout bug on mobile
/intelligent-routing deploy to production
```

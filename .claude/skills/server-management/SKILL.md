---
name: server-management
description: Server management principles and decision-making. Process management, monitoring strategy, and scaling decisions.
user-invocable: true
argument-hint: [monitoring | scaling | process | troubleshoot]
allowed-tools: Read, Glob, Grep, Bash
---

# /server-management - Server Management Guidance

$ARGUMENTS

---

## Purpose

Expert guidance on server management including process management (PM2/systemd/Docker), monitoring, log management, scaling decisions, and troubleshooting.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/server-management/SKILL.md` for server management principles
- `.agent/agents/devops-engineer.md` for DevOps expertise

---

## Behavior

When `/server-management` is triggered:

1. **Understand context**
   - What server infrastructure?
   - Current pain points?
   - Scale requirements?

2. **Guide decisions**
   - Process management (PM2/systemd/Docker)
   - Monitoring strategy (what to monitor, alert severity)
   - Scaling decisions (vertical/horizontal/auto)

3. **Apply best practices**
   - Log management principles
   - Health check patterns
   - Security hardening
   - Troubleshooting priority

---

## Examples

```
/server-management setup monitoring for production
/server-management PM2 vs systemd vs Docker
/server-management scaling strategy for growing traffic
/server-management troubleshoot high CPU usage
```

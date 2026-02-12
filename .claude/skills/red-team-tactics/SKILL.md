---
name: red-team-tactics
description: Red team tactics principles based on MITRE ATT&CK. Attack phases, detection evasion, reporting.
user-invocable: true
argument-hint: [target | scope | phase | technique]
allowed-tools: Read, Glob, Grep
---

# /red-team-tactics - Red Team Guidance

$ARGUMENTS

---

## Purpose

Red team tactics guidance based on MITRE ATT&CK framework. Covers reconnaissance, initial access, privilege escalation, lateral movement, and reporting. For authorized security testing only.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/red-team-tactics/SKILL.md` for red team principles
- `.agent/agents/penetration-tester.md` for pentesting expertise

---

## Behavior

When `/red-team-tactics` is triggered:

1. **Verify authorization**
   - Confirm this is authorized testing (CTF, pentest engagement, security research)

2. **Apply MITRE ATT&CK methodology**
   - Reconnaissance principles
   - Initial access vectors
   - Privilege escalation (Windows/Linux)
   - Defense evasion techniques
   - Lateral movement principles

3. **Specialized areas**
   - Active Directory attacks
   - Ethical boundaries enforcement

4. **Report findings**
   - Structured reporting principles

---

## Examples

```
/red-team-tactics reconnaissance techniques for web app
/red-team-tactics privilege escalation on Linux
/red-team-tactics Active Directory attack paths
/red-team-tactics write pentest report findings
```

---

## Ethical Boundaries

- Only for authorized testing contexts
- Follow responsible disclosure
- Document all findings professionally

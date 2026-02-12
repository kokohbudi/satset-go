---
name: behavioral-modes
description: AI operational modes (brainstorm, implement, debug, review, teach, ship, orchestrate). Adapts behavior based on task type.
user-invocable: true
argument-hint: [brainstorm | implement | debug | review | teach | ship]
allowed-tools: Read, Glob, Grep
---

# /behavioral-modes - Set AI Operational Mode

$ARGUMENTS

---

## Purpose

Switch between AI operational modes to adapt behavior based on task type. Each mode has different priorities, tools, and communication styles.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/behavioral-modes/SKILL.md` for mode definitions and triggers
- `.agent/agents/orchestrator.md` for multi-mode coordination

---

## Available Modes

| Mode | Behavior | Use When |
|------|----------|----------|
| **BRAINSTORM** | Explore options, no code | Planning, ideation |
| **IMPLEMENT** | Write code, build features | Active development |
| **DEBUG** | Systematic investigation | Fixing issues |
| **REVIEW** | Analyze, critique, improve | Code review |
| **TEACH** | Explain, guide, educate | Learning |
| **SHIP** | Deploy, release, finalize | Production release |
| **EXPLORE** | Discover, map, understand | Codebase navigation |

---

## Behavior

When `/behavioral-modes` is triggered:

1. **Detect requested mode** from arguments
2. **Load mode-specific behavior** from knowledge pack
3. **Apply mode constraints** (e.g., BRAINSTORM = no code)
4. **Switch communication style** to match mode

---

## Examples

```
/behavioral-modes implement
/behavioral-modes debug
/behavioral-modes review
/behavioral-modes teach me about React hooks
```

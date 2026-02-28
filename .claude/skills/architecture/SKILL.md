---
name: architecture
description: Architectural decision-making framework. Requirements analysis, trade-off evaluation, ADR documentation.
user-invocable: true
argument-hint: [system | decision | ADR | pattern]
allowed-tools: Read, Glob, Grep
---

# /architecture - Architecture Decision Framework

$ARGUMENTS

---

## Purpose

Architectural decision-making framework for requirements analysis, trade-off evaluation, pattern selection, and ADR (Architecture Decision Record) documentation.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/architecture/SKILL.md` for architectural principles
- `.agent/agents/project-planner.md` for planning expertise

Based on the request, selectively read:
- `.agent/skills/architecture/context-discovery.md` - Requirements discovery
- `.agent/skills/architecture/trade-off-analysis.md` - Trade-off framework
- `.agent/skills/architecture/pattern-selection.md` - Pattern decision trees
- `.agent/skills/architecture/patterns-reference.md` - Architecture patterns reference
- `.agent/skills/architecture/examples.md` - Real-world examples

---

## Behavior

When `/architecture` is triggered:

1. **Context discovery**
   - Ask the right questions about requirements
   - Understand constraints and quality attributes

2. **Trade-off analysis**
   - Evaluate options systematically
   - Document pros/cons with evidence

3. **Pattern selection**
   - Use decision trees for pattern selection
   - Match patterns to requirements

4. **Document decisions**
   - ADR format for architectural decisions
   - Include context, decision, and consequences

---

## Examples

```
/architecture should we use microservices or monolith?
/architecture ADR for choosing message queue
/architecture evaluate caching strategy options
/architecture design system architecture for SaaS
```

---

## Core Principle

> "Simplicity is the ultimate sophistication"

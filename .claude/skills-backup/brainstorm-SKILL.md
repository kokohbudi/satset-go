---
name: brainstorm
description: Structured brainstorming for projects and features. Explores multiple options before implementation.
user-invocable: true
argument-hint: [topic or feature]
allowed-tools: Read, Glob, Grep
---

# /brainstorm - Structured Idea Exploration

$ARGUMENTS

---

## Purpose

This command activates BRAINSTORM mode for structured idea exploration. Use when you need to explore options before committing to an implementation.

---

## Behavior

When `/brainstorm` is triggered:

1. **Load knowledge** from `.agent/agents/project-planner.md` and `.agent/skills/brainstorming/SKILL.md`

2. **Understand the goal**
   - What problem are we solving?
   - Who is the user?
   - What constraints exist?

3. **Generate options**
   - Provide at least 3 different approaches
   - Each with pros and cons
   - Consider unconventional solutions

4. **Compare and recommend**
   - Summarize tradeoffs
   - Give a recommendation with reasoning

---

## Output Format

```markdown
## Brainstorm: [Topic]

### Context
[Brief problem statement]

---

### Option A: [Name]
[Description]

**Pros:**
- [benefit 1]
- [benefit 2]

**Cons:**
- [drawback 1]

**Effort:** Low | Medium | High

---

### Option B: [Name]
[Description]

**Pros:**
- [benefit 1]

**Cons:**
- [drawback 1]
- [drawback 2]

**Effort:** Low | Medium | High

---

### Option C: [Name]
[Description]

**Pros:**
- [benefit 1]

**Cons:**
- [drawback 1]

**Effort:** Low | Medium | High

---

## Recommendation

**Option [X]** because [reasoning].

What direction would you like to explore?
```

---

## Examples

```
/brainstorm authentication system
/brainstorm state management for complex form
/brainstorm database schema for social app
/brainstorm caching strategy
```

---

## Key Principles

- **No code** - this is about ideas, not implementation
- **Visual when helpful** - use diagrams for architecture
- **Honest tradeoffs** - don't hide complexity
- **Defer to user** - present options, let them decide

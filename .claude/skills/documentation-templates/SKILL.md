---
name: documentation-templates
description: Documentation templates and structure guidelines. README, API docs, code comments, and AI-friendly documentation.
user-invocable: true
argument-hint: [README | API | changelog | ADR]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /documentation-templates - Documentation Generator

$ARGUMENTS

---

## Purpose

Generate documentation using structured templates including README, API docs, code comments, changelog, ADR, and AI-friendly documentation (llms.txt, MCP-ready).

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/documentation-templates/SKILL.md` for documentation templates
- `.agent/agents/documentation-writer.md` for documentation expertise

---

## Behavior

When `/documentation-templates` is triggered:

1. **Understand what to document**
   - README for project overview?
   - API docs per endpoint?
   - Changelog update?
   - ADR for architecture decision?

2. **Apply template**
   - README structure template
   - API documentation per-endpoint template
   - Code comment guidelines (JSDoc/TSDoc)
   - Changelog template (Keep a Changelog)
   - ADR template

3. **AI-friendly docs**
   - llms.txt format
   - MCP-ready documentation

---

## Examples

```
/documentation-templates generate README for this project
/documentation-templates API docs for user endpoints
/documentation-templates create CHANGELOG entry
/documentation-templates ADR for database choice
/documentation-templates generate llms.txt
```

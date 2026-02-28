---
name: thymeleaf-patterns
description: Best practices for Thymeleaf template engine with Spring Boot. Layouts, fragments, security integration, utility objects.
user-invocable: true
argument-hint: [layout | fragment | form | security]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /thymeleaf-patterns - Thymeleaf Development Guidance

$ARGUMENTS

---

## Purpose

Expert guidance on Thymeleaf template engine with Spring Boot including layout dialects, fragment composition, Spring Security integration, and modern JS integration (Alpine.js/HTMX).

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/thymeleaf-patterns/SKILL.md` for Thymeleaf best practices
- `.agent/agents/frontend-specialist.md` for frontend expertise
- `.agent/agents/backend-specialist.md` for Spring Boot expertise

---

## Behavior

When `/thymeleaf-patterns` is triggered:

1. **Understand context**
   - Layout structure question?
   - Fragment/component design?
   - Security integration?

2. **Apply patterns**
   - Component-based via server-side inclusion
   - Project structure (layouts/components/pages)
   - Decorator pattern with thymeleaf-layout-dialect
   - Parametrized fragments as components

3. **Modern integration**
   - Alpine.js for client interactivity
   - HTMX for dynamic updates
   - Spring Security integration
   - URL and asset management

---

## Examples

```
/thymeleaf-patterns layout with header/footer/sidebar
/thymeleaf-patterns reusable form component with fragments
/thymeleaf-patterns Spring Security in templates
/thymeleaf-patterns Alpine.js integration for dynamic UI
```

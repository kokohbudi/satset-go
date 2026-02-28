---
name: i18n-localization
description: Internationalization and localization patterns. Detecting hardcoded strings, managing translations, locale files, RTL support.
user-invocable: true
argument-hint: [locale | translation | RTL | setup]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /i18n-localization - Internationalization Guidance

$ARGUMENTS

---

## Purpose

Expert guidance on internationalization (i18n) and localization (L10n) including detecting hardcoded strings, managing translations, locale files, and RTL support.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/i18n-localization/SKILL.md` for i18n principles and patterns
- `.agent/agents/frontend-specialist.md` for frontend expertise

---

## Behavior

When `/i18n-localization` is triggered:

1. **Understand context**
   - Which framework? (React, Next.js, Python, Spring Boot)
   - Setting up from scratch or adding languages?
   - RTL support needed?

2. **Guide implementation**
   - i18n vs L10n concepts
   - Framework-specific patterns
   - File structure for translations
   - RTL support with CSS logical properties

3. **Validate**
   - Run `.agent/skills/i18n-localization/scripts/i18n_checker.py` to detect hardcoded strings

---

## Examples

```
/i18n-localization setup for Next.js with multiple languages
/i18n-localization detect hardcoded strings in project
/i18n-localization add RTL support for Arabic
/i18n-localization translation management best practices
```

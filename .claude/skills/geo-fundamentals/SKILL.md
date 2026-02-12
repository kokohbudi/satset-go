---
name: geo-fundamentals
description: Generative Engine Optimization for AI search engines (ChatGPT, Claude, Perplexity).
user-invocable: true
argument-hint: [content | site | audit | entity]
allowed-tools: Read, Glob, Grep
---

# /geo-fundamentals - Generative Engine Optimization

$ARGUMENTS

---

## Purpose

Generative Engine Optimization (GEO) for AI search engines including ChatGPT, Claude, Perplexity, and Gemini. Optimize content to be cited by AI.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/geo-fundamentals/SKILL.md` for GEO principles
- `.agent/agents/seo-specialist.md` for SEO/GEO expertise

---

## Behavior

When `/geo-fundamentals` is triggered:

1. **Understand the landscape**
   - SEO vs GEO differences
   - AI engine landscape (Perplexity, ChatGPT, Claude, Gemini)
   - RAG retrieval factors

2. **Optimize content**
   - Content that gets cited by AI
   - GEO content checklist
   - Entity building strategies

3. **Technical implementation**
   - AI crawler access configuration
   - Structured data for AI consumption

4. **Validate**
   - Run `.agent/skills/geo-fundamentals/scripts/geo_checker.py` if applicable

---

## Examples

```
/geo-fundamentals optimize content for AI citations
/geo-fundamentals audit site for GEO compliance
/geo-fundamentals entity building strategy
/geo-fundamentals configure AI crawler access
```

---

## Related Skills

- `/seo-fundamentals` for traditional SEO

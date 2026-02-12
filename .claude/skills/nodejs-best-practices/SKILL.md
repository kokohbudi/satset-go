---
name: nodejs-best-practices
description: Node.js development principles and decision-making. Framework selection, async patterns, security, and architecture.
user-invocable: true
argument-hint: [framework | pattern | architecture]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /nodejs-best-practices - Node.js Development Guidance

$ARGUMENTS

---

## Purpose

Expert guidance on Node.js development including framework selection, async patterns, security practices, and architectural decisions.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/nodejs-best-practices/SKILL.md` for Node.js principles
- `.agent/agents/backend-specialist.md` for backend expertise

---

## Behavior

When `/nodejs-best-practices` is triggered:

1. **Understand context**
   - What type of application?
   - What runtime? (Node.js, Bun, Deno)
   - ESM vs CommonJS?

2. **Guide framework selection**
   - Hono, Fastify, NestJS, Express (2025 recommendations)
   - Architecture patterns (layered structure)

3. **Apply best practices**
   - Error handling principles
   - Async patterns
   - Validation and security
   - Testing strategy

---

## Examples

```
/nodejs-best-practices which framework for a REST API?
/nodejs-best-practices async error handling patterns
/nodejs-best-practices project structure for microservices
/nodejs-best-practices Bun vs Node.js for this project
```

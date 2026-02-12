---
name: api-patterns
description: API design principles and decision-making. REST vs GraphQL vs tRPC selection, response formats, versioning, pagination.
user-invocable: true
argument-hint: [REST | GraphQL | tRPC | endpoint design]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /api-patterns - API Design Guidance

$ARGUMENTS

---

## Purpose

Expert guidance on API design patterns. Helps choose the right API style and implement best practices for response formats, versioning, authentication, and documentation.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/api-patterns/SKILL.md` for API design principles and content map
- `.agent/agents/backend-specialist.md` for backend expertise

Based on the request, selectively read:
- `.agent/skills/api-patterns/api-style.md` - Choosing API type (REST/GraphQL/tRPC)
- `.agent/skills/api-patterns/rest.md` - REST endpoint design
- `.agent/skills/api-patterns/graphql.md` - GraphQL schema design
- `.agent/skills/api-patterns/trpc.md` - tRPC patterns
- `.agent/skills/api-patterns/response.md` - Response formats
- `.agent/skills/api-patterns/versioning.md` - API versioning
- `.agent/skills/api-patterns/auth.md` - Authentication patterns
- `.agent/skills/api-patterns/rate-limiting.md` - Rate limiting strategies
- `.agent/skills/api-patterns/documentation.md` - API documentation
- `.agent/skills/api-patterns/security-testing.md` - Security testing

---

## Behavior

When `/api-patterns` is triggered:

1. **Understand context**
   - What type of API? (REST, GraphQL, tRPC)
   - Who are the consumers? (web, mobile, third-party)
   - What constraints exist? (performance, security)

2. **Provide decision-making guidance**
   - Reference relevant knowledge files
   - Present options with tradeoffs
   - Recommend based on context

3. **Design assistance**
   - Help structure endpoints/schema
   - Define response formats
   - Plan versioning strategy

4. **Validate**
   - Check against OWASP API Security Top 10
   - Run `.agent/skills/api-patterns/scripts/api_validator.py` if applicable

---

## Examples

```
/api-patterns REST endpoint for user management
/api-patterns should I use GraphQL for this dashboard?
/api-patterns design pagination for large datasets
/api-patterns JWT vs session authentication
```

---

## Key Principles

- **Ask before assuming** - Understand context before recommending
- **Context-driven decisions** - No default to REST for everything
- **Security first** - Always consider auth, rate limiting, validation

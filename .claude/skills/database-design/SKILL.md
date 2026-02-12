---
name: database-design
description: Database design principles and decision-making. Schema design, indexing strategy, ORM selection, serverless databases.
user-invocable: true
argument-hint: [schema | table | query | optimization]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /database-design - Database Design Guidance

$ARGUMENTS

---

## Purpose

Expert guidance on database design, helping choose the right database, design schemas, optimize queries, and plan migrations.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/database-design/SKILL.md` for database principles and content map
- `.agent/agents/database-architect.md` for database expertise

Based on the request, selectively read:
- `.agent/skills/database-design/database-selection.md` - PostgreSQL vs Neon vs Turso vs SQLite
- `.agent/skills/database-design/orm-selection.md` - Drizzle vs Prisma vs Kysely
- `.agent/skills/database-design/schema-design.md` - Normalization, PKs, relationships
- `.agent/skills/database-design/indexing.md` - Index types, composite indexes
- `.agent/skills/database-design/optimization.md` - N+1, EXPLAIN ANALYZE
- `.agent/skills/database-design/migrations.md` - Safe migrations, serverless DBs

---

## Behavior

When `/database-design` is triggered:

1. **Understand context**
   - What data needs to be stored?
   - What queries will be frequent?
   - What scale is expected?

2. **Guide decisions**
   - Database selection based on context
   - ORM recommendation
   - Schema design with normalization

3. **Optimize**
   - Indexing strategy
   - Query optimization
   - Migration planning

4. **Validate**
   - Run `.agent/skills/database-design/scripts/schema_validator.py` if applicable

---

## Examples

```
/database-design schema for user-role management
/database-design should I use PostgreSQL or SQLite?
/database-design optimize slow query with EXPLAIN ANALYZE
/database-design plan migration from Prisma to Drizzle
```

---

## Key Principles

- **ASK user for database preferences** when unclear
- **Choose database/ORM based on CONTEXT** not defaults
- **Don't default to PostgreSQL** for everything

---
name: enhance
description: Add or update features in existing application. Used for iterative development.
user-invocable: true
argument-hint: [feature or change description]
allowed-tools: Read, Glob, Grep, Bash, Write, Edit
---

# /enhance - Update Application

$ARGUMENTS

---

## Task

This command adds features or makes updates to existing application.

### Before Starting

Read and apply knowledge from relevant agents based on the change domain:
- `.agent/agents/frontend-specialist.md` for UI changes
- `.agent/agents/backend-specialist.md` for API/logic changes
- `.agent/agents/database-architect.md` for schema changes

### Steps:

1. **Understand Current State**
   - Analyze existing codebase structure
   - Understand existing features, tech stack

2. **Plan Changes**
   - Determine what will be added/changed
   - Detect affected files
   - Check dependencies

3. **Present Plan to User** (for major changes)
   - Show what files will be created/modified
   - Get approval before proceeding

4. **Apply**
   - Call relevant agent knowledge
   - Make changes
   - Test

5. **Verify**
   - Run tests
   - Check for regressions

---

## Usage Examples

```
/enhance add dark mode
/enhance build admin panel
/enhance integrate payment system
/enhance add search feature
/enhance edit profile page
/enhance make responsive
```

---

## Caution

- Get approval for major changes
- Warn on conflicting requests
- Commit each change with git

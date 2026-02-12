---
name: create
description: Create new application. Triggers App Builder skill and starts interactive dialogue with user.
user-invocable: true
argument-hint: [app type or description]
allowed-tools: Read, Glob, Grep, Bash, Write, Edit
---

# /create - Create Application

$ARGUMENTS

---

## Task

This command starts a new application creation process.

### Before Starting

Read and apply knowledge from:
- `.agent/agents/project-planner.md` for task breakdown
- `.agent/skills/app-builder/SKILL.md` for scaffolding
- `.agent/skills/brainstorming/SKILL.md` for Socratic Gate

### Steps:

1. **Request Analysis**
   - Understand what the user wants
   - If information is missing, ask clarifying questions (Socratic Gate)

2. **Project Planning**
   - Use `project-planner` agent knowledge for task breakdown
   - Determine tech stack
   - Plan file structure
   - Create plan file and proceed to building

3. **Application Building (After Approval)**
   - Orchestrate with `app-builder` skill
   - Coordinate expert agents:
     - `database-architect` -> Schema
     - `backend-specialist` -> API
     - `frontend-specialist` -> UI

4. **Preview**
   - Start preview when complete
   - Present URL to user

---

## Usage Examples

```
/create blog site
/create e-commerce app with product listing and cart
/create todo app
/create Instagram clone
/create crm system with customer management
```

---

## Before Starting

If request is unclear, ask these questions:
- What type of application?
- What are the basic features?
- Who will use it?

Use defaults, add details later.

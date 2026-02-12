---
name: orchestrate
description: Coordinate multiple agent knowledge domains for complex tasks. Use for multi-perspective analysis, comprehensive reviews, or tasks requiring different domain expertise.
user-invocable: true
argument-hint: [complex task description]
allowed-tools: Read, Glob, Grep, Bash, Write, Edit
---

# /orchestrate - Multi-Agent Orchestration

You are now in **ORCHESTRATION MODE**. Your task: coordinate specialized agent knowledge to solve this complex problem.

## Task to Orchestrate
$ARGUMENTS

---

## CRITICAL: Minimum Agent Requirement

> ORCHESTRATION = MINIMUM 3 DIFFERENT AGENT KNOWLEDGE DOMAINS
>
> **Validation before completion:**
> - Count invoked agent domains
> - If `agent_count < 3` -> STOP and apply more agent knowledge
> - Single agent = FAILURE of orchestration

### Agent Selection Matrix

| Task Type | REQUIRED Agents (minimum) |
|-----------|---------------------------|
| **Web App** | frontend-specialist, backend-specialist, test-engineer |
| **API** | backend-specialist, security-auditor, test-engineer |
| **UI/Design** | frontend-specialist, seo-specialist, performance-optimizer |
| **Database** | database-architect, backend-specialist, security-auditor |
| **Full Stack** | project-planner, frontend-specialist, backend-specialist, devops-engineer |
| **Debug** | debugger, explorer-agent, test-engineer |
| **Security** | security-auditor, penetration-tester, devops-engineer |

---

## STRICT 2-PHASE ORCHESTRATION

### PHASE 1: PLANNING (Sequential)

| Step | Agent Knowledge | Action |
|------|----------------|--------|
| 1 | `project-planner` | Create plan |
| 2 | (optional) `explorer-agent` | Codebase discovery if needed |

> NO OTHER AGENTS during planning! Only project-planner and explorer-agent.

### CHECKPOINT: User Approval

After plan is complete, ASK user for approval before proceeding.

> DO NOT proceed to Phase 2 without explicit user approval!

### PHASE 2: IMPLEMENTATION (After approval)

| Parallel Group | Agents |
|----------------|--------|
| Foundation | `database-architect`, `security-auditor` |
| Core | `backend-specialist`, `frontend-specialist` |
| Polish | `test-engineer`, `devops-engineer` |

---

## Available Agents (read from `.agent/agents/`)

| Agent | Domain | Use When |
|-------|--------|----------|
| `project-planner` | Planning | Task breakdown, planning |
| `explorer-agent` | Discovery | Codebase mapping |
| `frontend-specialist` | UI/UX | React, Vue, CSS, HTML |
| `backend-specialist` | Server | API, Node.js, Python, Java |
| `database-architect` | Data | SQL, NoSQL, Schema |
| `security-auditor` | Security | Vulnerabilities, Auth |
| `penetration-tester` | Security | Active testing |
| `test-engineer` | Testing | Unit, E2E, Coverage |
| `devops-engineer` | Ops | CI/CD, Docker, Deploy |
| `mobile-developer` | Mobile | React Native, Flutter |
| `performance-optimizer` | Speed | Profiling |
| `seo-specialist` | SEO | Meta, Schema, Rankings |
| `documentation-writer` | Docs | README, API docs |
| `debugger` | Debug | Error analysis |
| `game-developer` | Games | Unity, Godot |

---

## Orchestration Protocol

### Step 1: Analyze Task Domains
Identify ALL domains this task touches.

### Step 2: Read Agent Files
Read relevant `.agent/agents/{agent-name}.md` files for each domain.

### Step 3: Load Agent Skills
Check agent frontmatter for required skills and read `.agent/skills/{skill}/SKILL.md`.

### Step 4: Execute Based on Phase
Apply combined agent knowledge to the task.

### Step 5: Verification (MANDATORY)
Run appropriate verification scripts from `.agent/scripts/`.

### Step 6: Synthesize Results
Combine all outputs into unified report.

---

## Output Format

```markdown
## Orchestration Report

### Task
[Original task summary]

### Agents Applied (MINIMUM 3)
| # | Agent | Focus Area | Status |
|---|-------|------------|--------|
| 1 | project-planner | Task breakdown | Done |
| 2 | frontend-specialist | UI implementation | Done |
| 3 | test-engineer | Verification | Done |

### Key Findings
1. **[Agent 1]**: Finding
2. **[Agent 2]**: Finding
3. **[Agent 3]**: Finding

### Deliverables
- [ ] Plan created
- [ ] Code implemented
- [ ] Tests passing
- [ ] Verified

### Summary
[One paragraph synthesis of all agent work]
```

---

## EXIT GATE

Before completing orchestration, verify:

1. **Agent Count:** `invoked_agents >= 3`
2. **Verification:** At least basic checks ran
3. **Report Generated:** Orchestration Report with all agents listed

> **If any check fails -> DO NOT mark orchestration complete.**

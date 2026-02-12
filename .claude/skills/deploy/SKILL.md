---
name: deploy
description: Deployment command for production releases. Pre-flight checks and deployment execution.
user-invocable: true
argument-hint: [check | preview | production | rollback]
allowed-tools: Read, Glob, Grep, Bash
---

# /deploy - Production Deployment

$ARGUMENTS

---

## Purpose

This command handles production deployment with pre-flight checks, deployment execution, and verification.

### Before Starting

Read and apply knowledge from:
- `.agent/agents/devops-engineer.md` for deployment best practices
- `.agent/skills/deployment-procedures/SKILL.md` for procedures

---

## Sub-commands

```
/deploy            - Interactive deployment wizard
/deploy check      - Run pre-deployment checks only
/deploy preview    - Deploy to preview/staging
/deploy production - Deploy to production
/deploy rollback   - Rollback to previous version
```

---

## Pre-Deployment Checklist

Before any deployment:

```markdown
## Pre-Deploy Checklist

### Code Quality
- [ ] No compilation errors
- [ ] Linting passing
- [ ] All tests passing

### Security
- [ ] No hardcoded secrets
- [ ] Environment variables documented
- [ ] Dependencies audited

### Performance
- [ ] Bundle size acceptable
- [ ] No debug statements
- [ ] Assets optimized

### Documentation
- [ ] README updated
- [ ] CHANGELOG updated
- [ ] API docs current

### Ready to deploy? (y/n)
```

---

## Deployment Flow

```
/deploy
   |
   v
Pre-flight checks
   |
Pass? --No--> Fix issues
   |
  Yes
   |
   v
Build application
   |
   v
Deploy to platform
   |
   v
Health check & verify
   |
   v
Complete
```

---

## Platform Support

| Platform | Command | Notes |
|----------|---------|-------|
| Vercel | `vercel --prod` | Auto-detected for Next.js |
| Railway | `railway up` | Needs Railway CLI |
| Fly.io | `fly deploy` | Needs flyctl |
| Docker | `docker compose up -d` | For self-hosted |

---

## Examples

```
/deploy
/deploy check
/deploy preview
/deploy production
/deploy rollback
```

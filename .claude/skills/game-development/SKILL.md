---
name: game-development
description: Game development orchestrator. Routes to platform-specific skills based on project needs.
user-invocable: true
argument-hint: [2D | 3D | web | mobile | PC]
allowed-tools: Read, Glob, Grep, Write, Edit, Bash
---

# /game-development - Game Development Guidance

$ARGUMENTS

---

## Purpose

Game development orchestrator that routes to platform-specific knowledge based on project needs. Covers 2D/3D, web/mobile/PC, and specialty areas.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/game-development/SKILL.md` for game dev principles and routing
- `.agent/agents/game-developer.md` for game development expertise

Based on platform/dimension, selectively read from:
- `.agent/skills/game-development/2d-games/` - 2D game patterns
- `.agent/skills/game-development/3d-games/` - 3D game patterns
- `.agent/skills/game-development/web-games/` - Browser games
- `.agent/skills/game-development/mobile-games/` - Mobile games
- `.agent/skills/game-development/pc-games/` - Desktop games
- `.agent/skills/game-development/vr-ar/` - VR/AR experiences
- `.agent/skills/game-development/game-design/` - Game design
- `.agent/skills/game-development/game-art/` - Game art
- `.agent/skills/game-development/game-audio/` - Game audio
- `.agent/skills/game-development/multiplayer/` - Multiplayer systems

---

## Behavior

When `/game-development` is triggered:

1. **Route by platform**
   - Web, mobile, PC, or VR/AR?
   - 2D or 3D?

2. **Core principles**
   - Game loop design
   - Design patterns for games
   - Performance budget
   - AI selection and collision strategy

3. **Specialty areas**
   - Multiplayer networking
   - Art pipeline
   - Audio integration

---

## Examples

```
/game-development 2D platformer with Phaser
/game-development 3D FPS with Unity
/game-development multiplayer lobby system
/game-development mobile puzzle game design
```

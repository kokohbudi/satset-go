---
name: performance-profiling
description: Performance profiling principles. Measurement, analysis, and optimization techniques.
user-invocable: true
argument-hint: [page | endpoint | bundle | audit]
allowed-tools: Read, Glob, Grep, Bash
---

# /performance-profiling - Performance Analysis

$ARGUMENTS

---

## Purpose

Performance profiling and optimization including Core Web Vitals, bundle analysis, runtime profiling, and identifying common bottlenecks.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/performance-profiling/SKILL.md` for profiling principles
- `.agent/agents/performance-optimizer.md` for performance expertise

---

## Behavior

When `/performance-profiling` is triggered:

1. **Measure first**
   - Core Web Vitals (LCP, INP, CLS)
   - 4-step profiling workflow

2. **Analyze**
   - Bundle analysis
   - Runtime profiling
   - Common bottlenecks by symptom

3. **Optimize**
   - Quick win priorities
   - Targeted fixes based on profiling data

4. **Validate**
   - Run `.agent/skills/performance-profiling/scripts/lighthouse_audit.py` for web audit

---

## Examples

```
/performance-profiling audit homepage Core Web Vitals
/performance-profiling analyze bundle size
/performance-profiling slow API endpoint profiling
/performance-profiling identify rendering bottlenecks
```

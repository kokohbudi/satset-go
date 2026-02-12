---
name: mobile-design
description: Mobile-first design thinking for iOS and Android. Touch interaction, performance patterns, platform conventions.
user-invocable: true
argument-hint: [iOS | Android | feature | audit]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /mobile-design - Mobile Design Guidance

$ARGUMENTS

---

## Purpose

Mobile-first design thinking for iOS and Android apps including touch interaction, performance patterns, and platform-specific conventions.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/mobile-design/SKILL.md` for mobile design principles
- `.agent/skills/mobile-design/mobile-design-thinking.md` (CRITICAL - read first)
- `.agent/agents/mobile-developer.md` for mobile development expertise

Based on the request, selectively read:
- `.agent/skills/mobile-design/touch-psychology.md` - Touch interaction
- `.agent/skills/mobile-design/mobile-performance.md` - Performance
- `.agent/skills/mobile-design/mobile-navigation.md` - Navigation patterns
- `.agent/skills/mobile-design/mobile-typography.md` - Typography
- `.agent/skills/mobile-design/mobile-color-system.md` - Color system
- `.agent/skills/mobile-design/platform-ios.md` - iOS-specific
- `.agent/skills/mobile-design/platform-android.md` - Android-specific
- `.agent/skills/mobile-design/mobile-backend.md` - Backend integration
- `.agent/skills/mobile-design/mobile-testing.md` - Testing
- `.agent/skills/mobile-design/mobile-debugging.md` - Debugging

---

## Behavior

When `/mobile-design` is triggered:

1. **ASK before assuming**
   - Platform (iOS/Android/both)?
   - Framework (React Native/Flutter/native)?
   - Navigation pattern?

2. **Touch-first philosophy**
   - Design for fingers, not cursors
   - Touch target sizes and feedback

3. **Platform conventions**
   - iOS: HIG compliance
   - Android: Material Design

4. **Performance**
   - React Native/Flutter critical rules
   - Mobile anti-patterns to avoid

5. **CHECKPOINT mandatory before mobile work**
   - Run `.agent/skills/mobile-design/scripts/mobile_audit.py` if applicable

---

## Examples

```
/mobile-design navigation pattern for e-commerce app
/mobile-design iOS vs Android design differences
/mobile-design optimize React Native performance
/mobile-design touch-friendly form design
```

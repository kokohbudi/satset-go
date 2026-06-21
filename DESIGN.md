---
name: Satset Go
description: Fast, trustworthy digital-counter (PPOB) marketplace for resellers, admins, and buyers.
colors:
  primary: "#ea580c"
  primary-focus: "#f97316"
  accent: "#f59e0b"
  secondary: "#64748b"
  neutral: "#1f2937"
  base-100: "#ffffff"
  base-200: "#f8fafc"
  base-300: "#e2e8f0"
  base-content: "#1e293b"
  info: "#0ea5e9"
  success: "#22c55e"
  warning: "#f59e0b"
  error: "#ef4444"
typography:
  display:
    fontFamily: "Inter, ui-sans-serif, system-ui, -apple-system, sans-serif"
    fontSize: "clamp(2rem, 4vw, 3rem)"
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "normal"
  body:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "normal"
  label:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: "0.05em"
rounded:
  sm: "8px"
  md: "12px"
  lg: "16px"
  full: "9999px"
spacing:
  sm: "12px"
  md: "24px"
  lg: "32px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#ffffff"
    rounded: "{rounded.sm}"
    padding: "12px 20px"
  button-primary-hover:
    backgroundColor: "{colors.primary-focus}"
    textColor: "#ffffff"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    rounded: "{rounded.sm}"
    padding: "12px 20px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.base-content}"
    rounded: "{rounded.sm}"
    padding: "12px 20px"
  button-danger:
    backgroundColor: "{colors.error}"
    textColor: "#ffffff"
    rounded: "{rounded.sm}"
    padding: "12px 20px"
  card:
    backgroundColor: "{colors.base-100}"
    rounded: "{rounded.lg}"
    padding: "{spacing.md}"
  input:
    backgroundColor: "transparent"
    textColor: "{colors.base-content}"
    rounded: "{rounded.sm}"
    padding: "16px 14px 8px"
  nav-link-active:
    backgroundColor: "{colors.primary}"
    textColor: "#ffffff"
    rounded: "{rounded.md}"
    padding: "12px 16px"
---

# Design System: Satset Go

## 1. Overview

**Creative North Star: "The Satset Counter"**

*Satset* is Indonesian slang for snappy, no-waiting. This is the digital version of the neighbourhood pulsa counter: you walk up, you transact, you trust it, you leave. The system serves three people at once — a reseller running their store on a phone, an admin curating a catalog from a desk, a buyer topping up in ten seconds — and every one of them is handling money. So the visual language is built on two non-negotiables: it must feel **fast** (no decorative drag, the common path is obvious) and it must feel **safe** (amounts are clear, states are final, nothing looks flimsy or playful).

Warmth comes from one committed color — a confident burnt orange (#ea580c) — carried on a clean cool-neutral surface, with Inter doing all the typographic work across weights. The orange is identity, not noise: it marks the primary action and the active place, and otherwise stays out of the way. Depth is quiet and tonal; the interface layers with light surfaces and soft shadows rather than shouting with heavy chrome.

This system explicitly rejects four looks: the **generic SaaS template** (gradient hero-metric walls, identical icon-grid cards, tracked-uppercase eyebrows on every section); the **legacy PPOB panel** (tiny text, table-soup, neon, zero whitespace); anything **childish or over-gamified** that would undermine trust for money handling; and the **cold enterprise gray** dashboard with no brand warmth.

**Key Characteristics:**
- One brand color (burnt orange), earned as emphasis, never wallpaper.
- Cool off-white surfaces (#f8fafc / #ffffff) with quiet tonal depth.
- Inter throughout, hierarchy by weight and size, not by font-switching.
- Mobile-first: reseller and buyer flows are genuinely good at phone size.
- Trust signals first: clear amounts, clear states, visible focus.

## 2. Colors

A single warm brand accent on a cool-neutral slate base — warmth is the identity, neutrals are the workspace.

### Primary
- **Burnt Orange** (#ea580c): The one brand voice. Primary buttons, active nav, key icons, focus accents. This is the only color that means "act here" or "you are here".
- **Ember Orange** (#f97316): The hover/focus brightening of the primary, and the input focus border + ring.

### Secondary
- **Slate Steel** (#64748b): Muted secondary actions, secondary text, neutral controls. Never competes with the orange.

### Tertiary
- **Amber** (#f59e0b): Sparingly, as a warm accent distinct from the primary, and as the `warning` semantic. Don't let it read as a second brand color.

### Neutral
- **Ink Slate** (#1e293b): Primary body text (`base-content`) and the dark surface in `omnip-dark`.
- **Graphite** (#1f2937): The dark `neutral` role.
- **Paper White** (#ffffff): Card and dialog surfaces (`base-100`).
- **Cool Mist** (#f8fafc): App body background (`base-200`) — a true cool off-white, not a warm cream.
- **Divider Gray** (#e2e8f0): Borders, dividers, rails (`base-300`).

### Semantic
- **Sky** (#0ea5e9 info), **Green** (#22c55e success), **Amber** (#f59e0b warning), **Red** (#ef4444 error): status only — badges, alerts, validation. Never decorative.

### Named Rules
**The One Orange Rule.** Burnt orange appears on ≤10% of any screen — primary action, active state, one icon accent. If two oranges fight for attention on the same view, one of them is wrong.

**The Cool-Neutral Rule.** The base is cool slate (#f8fafc), never a warm cream/sand/beige. Warmth is carried by the orange and the copy, not by tinting the background.

## 3. Typography

**Display / Body / Label Font:** Inter (with `ui-sans-serif, system-ui, -apple-system` fallback)

**Character:** One family, full range. Inter is neutral, legible at small sizes, and trustworthy without personality cosplay — exactly right for dense admin tables and fast money screens. Hierarchy is built from weight and size, never from a second typeface.

### Hierarchy
- **Display** (700, `clamp(2rem, 4vw, 3rem)`, lh 1.1, -0.02em): Page-level marketing/landing headlines only. Capped at 3rem — the app does not shout.
- **Headline** (700, 1.5rem, lh 1.2): Page titles, section headers inside the app shell.
- **Title** (600, 1.125rem, lh 1.3): Card titles, dialog headers, table-group labels.
- **Body** (400, 1rem, lh 1.5): Default text and form values. Cap prose at 65–75ch.
- **Label** (500, 0.75rem, +0.05em, often uppercase): Stat captions, field labels, table column heads. Small-and-quiet, never a decorative eyebrow.

### Named Rules
**The One Family Rule.** Inter does everything. Adding a second typeface for "personality" is forbidden; differentiate with weight (400 / 500 / 600 / 700) and size.

**The Eyebrow Restraint Rule.** Uppercase tracked labels are allowed only as functional captions (a stat's metric name, a column head). An uppercase kicker floating above every section is the SaaS tell — banned.

## 4. Elevation

Quiet and tonal. Surfaces are light (#ffffff) on a slightly darker body (#f8fafc); separation comes first from that tonal step and from 1px `#e2e8f0` borders, with soft shadows added only as emphasis or state. The system is flat-leaning, not heavily lifted — depth is a whisper, not a drop-shadow contest.

### Shadow Vocabulary
- **Resting** (`box-shadow: 0 1px 3px rgba(0,0,0,0.06)`): Default card/stat-card surface. Barely there.
- **Raised** (`box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1)`): Standard cards, active sidebar link, on hover.
- **Brand glow** (`box-shadow: 0 10px 15px -3px rgba(234,88,12,0.30)`): Primary and danger buttons — a colored shadow in the action's own hue, signalling "this is the button".
- **Overlay** (`box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25)`): Modals and confirm dialogs only.

### Named Rules
**The Flat-At-Rest Rule.** Surfaces rest flat or near-flat. Shadow deepens as a response to state (hover lifts a card with `-translate-y-1` + Raised shadow), it is not the default decoration.

## 5. Components

### Buttons
- **Shape:** Gently rounded (8px, daisyUI default).
- **Primary:** Solid burnt orange (#ea580c), white text, `gap` for icon + label, plus the Brand-glow shadow (`shadow-primary/30`). Padding ~12px 20px. The single most prominent thing on its screen.
- **Hover / Focus:** Brightens to ember (#f97316), shadow deepens (`shadow-primary/40`), 200ms ease. Focus must show a visible ring.
- **Secondary:** Outline in primary (transparent fill, orange text + border) — for the alternative action next to a primary.
- **Ghost:** No fill, base-content text, hover tints `base-200`. For low-emphasis/toolbar actions.
- **Danger:** Solid red (#ef4444) with red-glow shadow. Destructive only (delete, suspend) — confirm dialog defaults to this.
- **Icon button:** Circular ghost, hovers to `primary/10` + orange. For table row actions (view/edit/delete).

### Cards / Containers
- **Corner Style:** 16px (`rounded-2xl`) for stat/feature cards; 12px for standard cards.
- **Background:** Paper white (#ffffff) on the Cool Mist body.
- **Shadow Strategy:** Resting at rest → Raised on hover, often with a 1px lift (`-translate-y-1`). See Elevation.
- **Border:** 1px `#e2e8f0` at 50% opacity; on hover, standard cards may shift border toward `primary/30`.
- **Internal Padding:** 24px (`spacing.md`) standard; 16px for compact info cards.
- **Never nest cards.** A card inside a card is always a layout mistake here.

### Inputs / Fields
- **Style:** Floating-label pattern. 1px border (`base-content/0.2`), 8px radius, transparent fill; the label sits inside and floats up on focus/value.
- **Focus:** Border shifts to ember (#f97316) with a 2px orange ring (`rgba(249,115,22,0.15)`). 200ms ease.
- **Error:** Border + ring go red (#ef4444); the label turns red; a `label-text-alt` error message appears below.
- **Select:** Same shell with a custom chevron; floating label behaves identically.

### Navigation
- **Sidebar:** Vertical link list (rounded 12px rows). Default: ghost; hover tints `primary/10` + orange text; **active**: solid orange fill, white text, Raised shadow. Collapsible, state persisted to `localStorage`, off-canvas under 1024px.
- **Bottom nav:** Mobile-only (`<lg`), fixed; the reseller/buyer primary navigation on phones.

### Status Badge (signature)
A pill with a small pulsing dot + label, color-coded by state: Aktif (success green), Nonaktif (ghost gray), Pending (warning amber), Suspended (error red). The dot is the at-a-glance trust signal in tables.

### Toast & Confirm
Toasts dock bottom-right, color-coded by type, auto-dismiss (success 3s / error 4s). Destructive actions route through a centered confirm dialog (Overlay shadow, scrim `black/50`) whose confirm button defaults to Danger.

## 6. Do's and Don'ts

### Do:
- **Do** keep burnt orange (#ea580c) to ≤10% of any screen — primary action + active state only (the One Orange Rule).
- **Do** build the body on cool off-white (#f8fafc); carry warmth through the orange and the copy.
- **Do** use Inter at multiple weights for hierarchy; one family only.
- **Do** make the primary action unmistakable with the orange fill + brand-glow shadow.
- **Do** show clear amounts, clear states, and visible focus on every money-bearing screen — trust is the feature.
- **Do** design the reseller/buyer flows mobile-first; touch targets ≥44px.
- **Do** add a `prefers-reduced-motion: reduce` fallback to every transition (currently missing in the codebase — fix on touch).

### Don't:
- **Don't** ship the generic SaaS template look: no gradient hero-metric walls, no identical icon-grid card rows, no tracked-uppercase eyebrow above every section.
- **Don't** regress to the legacy PPOB panel: no tiny text, no table-soup, no neon, no zero-whitespace density.
- **Don't** go childish or over-gamified — no cartoon mascots, no heavy gamification; this handles money.
- **Don't** fall into cold enterprise gray — a gray-on-gray dashboard with no orange is off-brand.
- **Don't** use a warm cream/sand/beige background; the base is cool slate (the Cool-Neutral Rule).
- **Don't** introduce a second typeface or a second competing brand color (amber is semantic warning, not a second orange).
- **Don't** nest cards, and don't use `border-left`/`border-right` >1px as a colored accent stripe.

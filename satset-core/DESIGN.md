# satsetgo Design System

Semua redesign halaman ikuti rules ini. Direction B (Counter Light) — aktif sejak 2026-06-26.

---

## Palette

| Token | Hex | Pakai untuk |
|---|---|---|
| `brand-orange-600` | `#ea580c` | Primary CTA, accent, highlights |
| `brand-orange-500` | `#f97316` | Hover state |
| `brand-orange-50` | `#fff7ed` | Tinted bg (badge, chip) |
| `brand-orange-700` | `#c2410c` | Text on light tinted bg |
| `ink` | `#1e293b` | Body text, dark backgrounds |
| `mist` | `#f8fafc` | Page background |
| `line` | `#e2e8f0` | Borders, dividers |
| `muted` | `#475569` | Secondary text, labels |
| `faint` | `#64748b` | Placeholder, tertiary text |

DaisyUI `primary` = `#ea580c`. Tailwind tokens: `satset-core/tailwind.config.js`.

---

## Typography

- Font: **Inter** satu-satunya — jangan tambah font kedua
- Hero heading: `clamp()` max ≤ 5rem, `tracking-[-0.03em]`, `text-balance`
- Section heading: `clamp(1.6rem, 3.2vw, 2.4rem)`, `font-extrabold`
- Body max-width: 65ch
- Tabular numbers: `font-variant-numeric: tabular-nums` (class `.tnum`)

---

## Layout

- Max content width: `max-w-6xl`
- Section padding: `py-12 sm:py-20 lg:py-24`
- Cards: `rounded-2xl border border-line` — no side-stripe, no identical grid
- Table bukan card kalau data tabular
- Overflow table: wrap dengan `overflow-x-auto`

---

## Logo System

**Mark**: kotak oranye rounded + lightning bolt SVG putih  
**Wordmark**: lowercase `satset` + `go` (warna berubah sesuai background)

| Context | Mark bg | "satset" | "go" |
|---|---|---|---|
| Di atas orange field | `bg-white`, bolt `text-brand-orange-700` | `text-white` | `text-white/75` |
| Di atas dark / ink | `bg-brand-orange-600` | `text-white` | `text-brand-orange-400` |
| Di atas light / card | `bg-brand-orange-600` | `text-base-content` | `text-primary` |

**Snippet (light bg — nav/sidebar):**
```html
<a href="/" class="flex items-center gap-2.5">
  <span class="w-8 h-8 rounded-lg bg-brand-orange-600 grid place-items-center">
    <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z"/>
    </svg>
  </span>
  <span class="font-bold tracking-tight">satset<span class="text-brand-orange-600">go</span></span>
</a>
```

**Snippet (dark bg — footer/CTA):**
```html
<a href="/" class="flex items-center gap-2.5">
  <span class="w-8 h-8 rounded-lg bg-brand-orange-600 grid place-items-center">
    <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z"/>
    </svg>
  </span>
  <span class="font-bold tracking-tight text-white">satset<span class="text-brand-orange-400">go</span></span>
</a>
```

**Jangan**: huruf kapital ("Satset Go"), lettermark "S", `bg-primary-gradient`.

---

## Nav (sticky, marketing pages)

```html
<nav class="sticky top-0 z-40 bg-mist/85 backdrop-blur border-b border-line">
```

Mobile hamburger wajib pakai Alpine `x-data="{ open: false }"`.

---

## Theme Locking

Marketing pages (landing, login) → dikunci ke `omnip-light`:
```html
<script>(function(){ document.documentElement.setAttribute('data-theme','omnip-light'); })();</script>
```
App pages (dashboard, admin) → ikut tema user.

---

## Thymeleaf

- Login link: `th:href="@{/oauth2/authorization/keycloak}"`
- Inline JS di template: `th:inline="none"` pada tag `<script>`
- Icon fragments: `th:replace="~{components/icons :: icon-bolt(class='...')}"`

---

## CSS Page-specific

Taruh di `<style>` block, jangan di `output.css`:

```css
[x-cloak] { display: none !important; }
.tnum     { font-variant-numeric: tabular-nums; }
.receipt  {
  -webkit-mask:
    radial-gradient(7px 7px at 14px bottom, transparent 6.5px, #000 7px) repeat-x left bottom / 28px 14px,
    linear-gradient(#000,#000) top/100% calc(100% - 8px) no-repeat;
  mask:
    radial-gradient(7px 7px at 14px bottom, transparent 6.5px, #000 7px) repeat-x left bottom / 28px 14px,
    linear-gradient(#000,#000) top/100% calc(100% - 8px) no-repeat;
}
@keyframes rise { from{opacity:0;transform:translateY(12px)} to{opacity:1;transform:none} }
.rise { animation: rise .55s cubic-bezier(.22,1,.36,1) backwards; }
@media (prefers-reduced-motion: reduce) { .rise { animation: none; } }
```

---

## Bans Aktif

Jangan pernah muncul di halaman manapun:

- `gradient-text` — `background-clip: text` + gradient
- Hero metrics palsu — 500+, 1M+, 99.9%
- Identical icon-card grid — 6+ kartu ukuran sama persis
- Uppercase tracked eyebrow di tiap section — `text-xs uppercase tracking-wider` sebagai kicker tiap section
- `float-animation` / floating CSS mockup
- Huruf kapital pada logo wordmark

---

## Build & Deploy

```bash
# Setelah edit template atau tambah Tailwind class baru:
cd satset-core
npm run build:css
cp src/main/resources/static/css/output.css target/classes/static/css/output.css
cp src/main/resources/templates/<file>.html target/classes/templates/<file>.html
```

Server baca dari `target/classes/` — selalu copy setelah edit.

---

## File Referensi

| File | Keterangan |
|---|---|
| `satset-core/src/main/resources/templates/landing.html` | Produksi — Direction B aktif |
| `satset-core/src/main/resources/templates/components/sidebar.html` | Sidebar app (Keycloak-driven) |
| `docs/landing-previews/b-counter-light.html` | Dummy sumber Direction B |
| `satset-core/tailwind.config.js` | Token warna + DaisyUI themes |
| `satset-core/src/main/resources/application.yml` | `spring.thymeleaf.cache: false` |

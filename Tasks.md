# SatSetGo - Task Board

> **Owner**: August (Senior PM)
> **Last Updated**: 2026-03-10
> **Sprint**: W-series — W-7 & W-8 remaining

---

## 🔥 CURRENT SPRINT

### Wallet Service Separation — W-series
> Design: `TechSpecs.md` → "Wallet Service — Technical Design"
> DB: schema `wallet` di `satset_go` DB yang sama. Multi-module Maven.

- [x] **W-0**: Keputusan repo structure → Multi-module Maven, DB schema terpisah ✅
- [x] **W-SETUP**: Restructure project → multi-module Maven (satset-core + satset-wallet) ✅
- [x] **W-1**: `satset-wallet` module skeleton — Spring Boot, SecurityConfig, actuator ✅
- [x] **W-2**: Wallet domain model — `WalletAccount`, `WalletMutation` domain records + JPA entities ✅
- [x] **W-3**: Wallet use cases + domain service — `WalletUseCase` (6 methods) + `WalletDomainService` ✅
- [x] **W-4**: Wallet REST endpoints — 6 endpoints: `GET /balance`, `POST /debit`, `POST /credit`, `POST /refund`, `GET /mutations`, `POST /accounts` ✅
- [x] **W-5**: Core: `WalletClientAdapter` (RestClient + OAuth2 client_credentials) + `WalletCreationAdapter` ✅
- [x] **W-6**: Core: `WalletClientAdapter` aktif via `WALLET_CLIENT_ENABLED=true` di `.env` ✅
- [ ] **W-7**: Data migration — seed `wallet_accounts` dari Core DB ke satset-wallet schema
- [ ] **W-8**: Integration test E2E — purchase flow Saga (Core → Wallet debit → Provider → Wallet refund), Testcontainers 2-service

### Secure Inter-Service Auth — TX-series
> **Goal**: Token Exchange (RFC 8693) — user context (userId, orgId) terbawa tamper-proof ke wallet.
> Token relay & custom header ditolak. Token Exchange dipilih — Keycloak signed, aud=satset-wallet.
> Claims: `org_wallet_id` + `user_id`. Cached 240s (buffer dari KC 5 menit).
> Design: `TechSpecs.md` → "Secure Inter-Service Auth"

- [ ] **TX-0**: Enable Token Exchange di Keycloak — fitur preview + permission policy `satset-core` → `satset-wallet`
- [ ] **TX-1**: `satset-wallet` SecurityConfig — `.hasAuthority("SCOPE_wallet:internal")` di `/internal/wallet/**`
- [ ] **TX-2**: `KeycloakTokenExchangeService` di satset-core — POST token exchange ke Keycloak
- [ ] **TX-3**: Cache hasil exchange token — Caffeine key `userId:audience`, TTL 240s
- [ ] **TX-4**: `WalletClientAdapter` — ganti OAuth2 client_credentials → token exchange service
- [ ] **TX-5**: Verify E2E — user login → beli → token exchange → wallet terima token + `SCOPE_wallet:internal`

---

## 📦 BACKLOG

### Balance Top-up
- [-] MockPaymentGateway — masuk ke satset-wallet (bukan Core)
- [-] Top-up UI di Core — call Wallet credit endpoint
- [-] Store Mutations UI — riwayat mutasi saldo di `/transactions`, fetch dari Wallet API

### Admin Organization Management (Post-MVP)
- [-] Task 7-11: KC Org API read/update, AdminOrgService, Admin Org UI, members modal, edit business data
- [-] User segregation backoffice vs reseller

### Admin User Management Cleanup
- [-] `/admin/user-management` — filter hanya backoffice users
- [-] Refactor User Search — migrasi Keycloak Admin API → DB lokal (database-level pagination)

### Revenue & Pricing (Post-MVP)
- [-] Reseller Tier & Dynamic Pricing (Bronze/Silver/Gold/Platinum)
- [-] Markup per Store
- [-] Komisi Upline (Rebate System)

### Technical Debt
- [ ] **C-1**: Separate domain models dari JPA `@Entity` — pure domain class + JPA entity + mapper per bounded context
- [ ] **L-8**: Pagination untuk product listing (low urgency, tunggu >100 produk)
- [ ] **INF-series**: HikariCP Virtual Thread stress test — validate apakah perlu migrasi ke Agroal

### Future
- [-] White-label Storefront, Dashboard Analytics, API Key Reseller, Bulk Transaction
- [-] Auto-switch Supplier, Dispute Management, Audit Log
- [-] Postpaid Inquiry, Real Provider Integration
- [-] Promo Engine, Notification, Gamification, Referral

---

## ✅ DONE (Summary)

| Series | Keterangan | Selesai |
|--------|-----------|---------|
| MVP (Task 1-17) | Foundation, Onboarding, Purchase flow, Integration test | 2026-02-25 |
| H-series | Port boundary cleanup (3 items) | 2026-02-25 |
| M-series | Config + correctness + hygiene (10 items) | 2026-02-25 |
| L-series | Code hygiene — L-1..L-7, L-9 (L-8 pending) | 2026-03-01 |
| AP-series | Admin Product Management — backend + frontend + tests + KC roles | 2026-03-02 |
| AP-N series | Catalog drill-down navigation | 2026-03-02 |
| OJ-series | JWT Org ID — CLOSED (DB lookup cukup, tidak perlu JWT) | 2026-03-03 |
| MR-series | Mandatory Role Assignment (Path C) | 2026-03-03 |
| C-2 | Decouple cross-context JPA FK → UUID references (339 tests pass) | 2026-03-06 |
| L-1 | Unit test coverage 68% → 94% instruction (+34 tests) | 2026-03-05 |
| WR-series | Wallet Refactor — WalletAccount entity, ports, BalanceDomainService (413 tests pass) | 2026-03-06 |
| W-SETUP + Phase A-C | Multi-module Maven, Hexagonal fix, Test fix (413 tests pass) | 2026-03-07 |
| WI-series | Single-save store creation (pre-generate UUID) | 2026-03-07 |
| W-1..W-6 | satset-wallet service + WalletClientAdapter aktif | 2026-03-10 |

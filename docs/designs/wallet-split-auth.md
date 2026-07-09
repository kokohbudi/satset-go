# Wallet Split — Auth & Integrity Design

**Status:** Not built. Trigger = the day `WalletService` moves out of `satset-core` into its own deployable.
**Today:** wallet is **in-process**. `TransactionController → WalletGateway → WalletService → JPA → Postgres`, all one JVM. No network, no serialization, nothing to sign or tamper. This doc only matters once that changes.

## What stays the same
`WalletGateway` is already the anti-corruption boundary (maps enums + exceptions, hides wallet-internal types). When wallet goes remote, **only `WalletGateway`'s body changes** — swap the in-process `walletService.debit(...)` for an HTTP call behind config. Callers (`TransactionDomainService`, onboarding) don't change.

## Auth: OAuth2 client-credentials (Keycloak)
1. `satset-core` holds a Keycloak **service-account client** (confidential). Fetch an access token via `client_credentials` grant.
2. Send `Authorization: Bearer <JWT>` to the wallet service.
3. Wallet validates the JWT signature against Keycloak's **JWKS** (RS256 public key) + checks `iss`, `aud`, `exp`, and a scope/role (e.g. `wallet:write`).

Spring: `RestClient` + `ServletOAuth2AuthorizedClientExchangeFilterFunction` (or `@RegisteredOAuth2AuthorizedClient`) handles token fetch, cache, and refresh. Don't hand-roll token management.

## The tampering nuance (why "Keycloak signature" is not the whole answer)
- A Keycloak JWT is signed by **Keycloak** and proves *who the caller is*. It does **NOT** sign your request **body**. A Bearer token alone does not stop payload tampering.
- **Payload integrity in transit → TLS/HTTPS. Mandatory, not optional.** This is what actually stops a man-in-the-middle from altering `amount` or `walletId`.
- Token can't be forged (RS256) → caller authenticity ✓. TLS → body integrity ✓. Those two cover the normal threat model.

## Replay / double-spend
- Already have the hook: every op carries a `referenceId` (UUID) — use it as an **idempotency key** on the wallet side (unique constraint on `(referenceId, referenceType)`; the ledger already dedups on this). Retries and replays collapse to one mutation.
- Pessimistic lock on the wallet row stays wallet-side; unchanged by the split.

## Making `walletId` session-bound & untamperable (the core requirement)
Goal: the `walletId` wallet acts on must (a) truly belong to the caller's session and (b) be impossible to alter in transit.

**Keycloak signs tokens, not your request fields.** So don't send `walletId` as a free-form body field and hope to sign it — put it **inside the JWT as a claim**. Then it's RS256-signed by Keycloak: tampering breaks the signature, and the client can't invent a `walletId` that isn't in its token.

### Steps
1. **Keycloak protocol mapper** — map user attribute `wallet_id` (or `store_id`) → access-token claim `wallet_id`. Same attribute-driven mechanism already used for sidebar attrs and the client-roles ID-token mapper. Resulting token:
   ```json
   { "sub": "...", "preferred_username": "reseller01",
     "wallet_id": "WL-0000042", "aud": "wallet", "exp": 1750000000 }
   ```
2. **Call model — must preserve user identity** (pick one):
   - *Forward the user's access token* to wallet (`Authorization: Bearer <user token>`), or
   - *Token exchange* (RFC 8693): satset-core exchanges the user token for a `aud=wallet` token that Keycloak re-signs with the `wallet_id` claim.
   - ❌ *Not* pure service-account (`client_credentials`): drops user identity → `walletId` falls back to the body → the exact tamper risk we're removing.
3. **Wallet side (source of truth = the claim):**
   ```
   validate JWT (JWKS sig, iss, aud=wallet, exp)
   walletId := token.claim("wallet_id")
   if request also carries walletId and bodyWalletId != tokenWalletId → 403
   operate only on the claim's walletId
   ```
   Wallet never trusts a body/URL `walletId`; at most it cross-checks it against the claim and rejects mismatch.

### What this gives vs what still needs covering
- `walletId` integrity + session binding → **the signed claim** (done, no custom signing).
- Other fields (`amount`, `description`) integrity in transit → **TLS** (mandatory).
- Token theft/replay → short TTL, `aud=wallet`, TLS, and `referenceId` idempotency (already present).

> Not editing `src/test/resources/satset-go-realm-full.json` (test realm fixture) speculatively — add the `wallet_id` mapper there only when the split work actually starts, so test expectations don't drift early.

## Only if non-repudiation is required (probably not)
If a signed audit trail of the *body* is needed beyond transport security, add a **detached JWS** or **HMAC** header over the canonical request body, keyed per-client. Adds key management. Skip unless a compliance requirement names it — TLS + Bearer + idempotency is the default and is enough for internal service-to-service.

## Checklist when the split happens
- [ ] Wallet service validates Keycloak JWT (JWKS, iss/aud/exp/scope)
- [ ] `wallet_id` protocol mapper added to Keycloak realm (user attr → token claim)
- [ ] Wallet reads `walletId` from the claim, not the body; rejects body/claim mismatch
- [ ] TLS enforced both directions (no plaintext balance ops, ever)
- [ ] `satset-core` service-account client in Keycloak realm
- [ ] `referenceId` unique constraint enforced wallet-side (idempotency)
- [ ] `WalletGateway` HTTP impl selected by config profile; in-process impl kept for tests
- [ ] Timeouts + retry (idempotent-safe) + circuit breaker on the client

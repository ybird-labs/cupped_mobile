# Process: Local-first Brew-log Data Layer

## Goal

Implement the **shared KMP data-layer-only** local-first brew-log foundation.

The run should research and decide the local read-model shape, then implement SQLDelight-backed local reads/writes, ride-along draft refs, and outbox invariants using ATDD/TDD.

## Agreed scope

### Include

- Research/ADR for the local brew-log read model.
- Shared KMP data layer only.
- SQLDelight labeled queries and typed adapters where consumed.
- Repository/use-case rewrite from synchronous REST-style create to local-first persistence.
- Offline create flow when no cached beans exist: create a new ride-along bean draft.
- Offline reuse of pending draft beans already used in brew logs, via stable `bean_client_id`.
- Existing/cached bean references.
- Durable outbox insert/coalescing in the same transaction as local row writes.
- ATDD/TDD tests for local-write invariants.
- Architecture/security/scope review.
- Follow-up issue drafts for deferred production gates.

### Exclude

- UI integration in Compose/Android/iOS.
- Client SyncEngine network push/pull.
- Server endpoints.
- API contract changes.
- iOS SQLCipher CocoaPods/linker work.
- OS background sync.
- Standalone offline bean/recipe CRUD.

## Important decisions captured

1. **Bean picker behavior**
   - Offline with cached beans: choose cached existing bean or create new draft.
   - Offline with no data: create new bean draft and still log.
   - Pending draft beans already used in brew logs can be reused locally.
   - Online search is future UI/network behavior; selected remote beans become existing canonical refs.

2. **Draft beans are ride-along, not standalone CRUD**
   - A new bean exists through brew-log payload state in v1.
   - Reuse is projected from pending brew logs, not from a first-class offline bean table.

3. **iOS SQLCipher is a separate production gate**
   - Android SQLCipher is implemented.
   - iOS SQLCipher is not wired.
   - iOS release fail-closes rather than opening plaintext.
   - Do not wire production iOS UI to this DB-backed flow until SQLCipher is completed.

4. **API contract/client-generated ID is not blocking this run**
   - Local IDs are generated and stored now.
   - Server/API support for client-generated `BrewLogCreateRequest.id` is required before network push/sync, not for this local data-layer run.

## Phases

### Phase 0 — Brownfield scan

- Read architecture/research docs.
- Inspect current brew-log domain, use cases, SQLDelight schema, tests, and DI.
- Produce current-state scan and risk list.

**Checkpoint:** review scan before moving into model research.

### Phase 0.5 — Brewer contract/resource research

Before model research, use the generated OpenAPI contract as the mobile API boundary and inspect Brewer directly to explain contract drift, implementation behavior, and future-sync gaps that the current contract does not cover.

Answer:

- Is brew-log ownership profile-scoped or user-scoped?
- What does current Brewer REST create/list/update/delete support?
- Does Brewer currently support client-generated IDs?
- Does Brewer currently support inline bean/recipe drafts, idempotent mutations, cursors, tombstones, or sync events?
- Which Brewer resource shapes should the mobile local cache model now?
- Which mobile draft fields do not match Brewer server shapes?

Tracked Phase 0 review artifact:

- `phase-0-review.md`

### Phase 1 — Local read-model research / ADR

Compare:

- mutate existing `BrewLog`,
- introduce `LocalBrewLog`,
- introduce screen/read projections,
- sealed refs for existing/draft beans and recipes.

Also decide using direct Brewer implementation evidence:

- authenticated **profile id** source for local rows/outbox ownership,
- stable per-install `client_id` source,
- whether existing SQLDelight `user_id` columns should be renamed to `profile_id` before repository logic lands,
- how to handle current Brewer REST constraints: optional client id exists in Brewer source, but canonical `bean_id` is still required and no sync API/idempotent mutation replay exists yet,
- test source-set strategy for SQLDelight-backed repository tests,
- draft JSON serialization strategy, including mobile draft fields that do not currently match Brewer resource shapes.

Decision gate: user approves the model/identity decision before implementation planning.

### Phase 2 — ATDD acceptance spec

Write executable acceptance scenarios for:

- no-cache offline new bean draft create,
- cached existing bean create,
- pending draft bean reuse,
- pending-create update/delete coalescing,
- blocked_error edit re-arm,
- local SQLDelight observation.

Decision gate: user approves scenarios and exclusions.

### Phase 3 — TDD red phase

- Write failing tests first.
- Tests should prove behavior and invariants.
- Run `./gradlew :shared:allTests`; red is expected after tests are added.

**Checkpoint:** review failing tests before implementation.

### Phase 4 — Implementation / green phase

- Add SQLDelight queries/adapters as needed.
- Implement local-first repository/use-case semantics.
- Persist brew log + outbox atomically.
- Support ride-along existing/new refs and reusable pending draft refs.
- Run `./gradlew :shared:allTests` until green.

**Checkpoint:** review green implementation before architecture/security review.

### Phase 5 — Review and refinement

- Architecture/security review.
- Scope guard: ensure no UI, server/API, SyncEngine network, or iOS SQLCipher changes slipped in.
- Up to two refinement iterations if quality is below target.

### Phase 6 — Final verification

- `./gradlew :shared:allTests`
- API boundary grep/check.
- Scope oracle review.
- Follow-up issue drafts.
- Final user approval gate.

## Quality target

Target quality score: **90/100**.

Blockers:

- API DTO leakage into brew-log domain/UI APIs.
- Confusing `user_id` vs `profile_id` ownership semantics. Cupped scopes brew logs to the authenticated profile; existing DB columns are currently named `user_id`, so Phase 1 must decide whether to rename storage columns to `profile_id` now or keep a legacy storage name with explicit mapping.
- Non-atomic brew_log/outbox writes.
- No tests for NewDraft ride-along and outbox coalescing.
- UI/server/SyncEngine/iOS SQLCipher scope creep.
- Any fail-open storage/security behavior.

## Suggested later issue: iOS SQLCipher

Create an issue later, not during this run, for:

> Wire SQLCipher on iOS release builds and mirror Android wipe-then-rotate recovery.

Acceptance criteria:

- iOS links SQLCipher correctly with SQLDelight native driver.
- Release builds never open plaintext DB.
- Existing encrypted DB with missing/undecryptable key follows a deliberate recovery path.
- Logout/account-switch wipe + re-key behavior is tested.

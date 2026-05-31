# Process: Local-first Brew-log Data Layer

## Goal

Implement the **shared KMP data-layer-only** local-first brew-log foundation.

The run should research and decide the local read-model shape, then implement SQLDelight-backed local reads/writes, optimistic bean refs for brew-log dependency creation, recipe draft ride-along handling if still needed, and outbox invariants using ATDD/TDD.

## Agreed scope

### Include

- Research/ADR for the local brew-log read model.
- Shared KMP data layer only.
- SQLDelight labeled queries and typed adapters where consumed.
- Repository/use-case rewrite from synchronous REST-style create to local-first persistence.
- Offline create flow when no cached beans exist: create an optimistic local bean ref with stable client-generated bean `id`, then create the brew log against that local ref.
- Offline reuse of pending optimistic beans already used in brew logs, via stable `bean_client_id` / client-generated bean `id`.
- Existing/cached bean references.
- Durable outbox insert/coalescing in the same transaction as local row writes.
- ATDD/TDD tests for local-write invariants.
- Architecture/security/scope review.
- Follow-up issue drafts for deferred production gates.

### Exclude

- UI integration in Compose/Android/iOS.
- Client SyncEngine network push/pull.
- Server endpoints.
- Additional API contract changes beyond consuming the pinned `api-spec-v0.1.4` OpenAPI update.
- iOS SQLCipher CocoaPods/linker work.
- OS background sync.
- Standalone offline bean/recipe CRUD UI or general-purpose bean management outside brew-log dependency creation.

## Important decisions captured

1. **Bean picker/data behavior**
   - Offline with cached beans: choose cached existing bean or create a new optimistic local bean.
   - Offline with no data: create a new optimistic local bean with a client-generated id and still log.
   - Pending optimistic beans already used in brew logs can be reused locally.
   - Online search is future UI/network behavior; selected remote beans become existing canonical refs.

2. **Beans now support optimistic local creation, but not standalone CRUD scope**
   - Updated OpenAPI `api-spec-v0.1.4` includes `/api/v1/beans` `POST` with optional client-generated `id`.
   - A new bean needed for a brew log can be modeled as an optimistic local bean resource/dependency instead of only opaque brew-log payload state.
   - Phase 1 must decide whether this becomes a pending row in `bean_cache` plus outbox mutation, or remains embedded as draft JSON with a clear promotion path.
   - This does **not** expand the run into standalone bean-management UI or general offline bean CRUD.

3. **iOS SQLCipher is a separate production gate**
   - Android SQLCipher is implemented.
   - iOS SQLCipher is not wired.
   - iOS release fail-closes rather than opening plaintext.
   - Do not wire production iOS UI to this DB-backed flow until SQLCipher is completed.

4. **API contract now supports optimistic IDs for brew logs and beans**
   - The pinned OpenAPI contract is `api-spec-v0.1.4`.
   - `BrewLogCreateRequest.id` is available for client-generated brew-log IDs.
   - `BeanCreateRequest.id` is available for client-generated bean IDs.
   - Network push/sync remains out of scope, but the local model should be shaped so future push can create optimistic beans before/with brew logs without inventing new IDs later.

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
- Does Brewer currently support optimistic bean create via client-generated `BeanCreateRequest.id`, inline bean/recipe drafts, idempotent mutations, cursors, tombstones, or sync events?
- Which Brewer resource shapes should the mobile local cache model now?
- Which mobile draft fields do not match Brewer server shapes?

Tracked Phase 0 review artifact:

- `phase-0-review.md`

### Phase 1 — Local read-model research / ADR

Compare:

- mutate existing `BrewLog`,
- introduce `LocalBrewLog`,
- introduce screen/read projections,
- sealed refs for existing/optimistic-new beans and existing/draft recipes.

Also decide using direct Brewer implementation evidence:

- authenticated **profile id** source for local rows/outbox ownership,
- stable per-install `client_id` source,
- whether existing SQLDelight `user_id` columns should be renamed to `profile_id` before repository logic lands,
- how to handle current Brewer REST constraints: optional client-generated IDs now exist for brew-log and bean create, canonical `bean_id` is still required for brew-log create, and no sync API/idempotent mutation replay exists yet,
- whether pending new beans should be first-class optimistic local resources (`bean_cache` + outbox dependency) or embedded draft JSON with a later promotion path,
- test source-set strategy for SQLDelight-backed repository tests,
- draft/optimistic bean serialization strategy, including mobile draft fields that do not currently match Brewer resource shapes.

Decision gate: user approves the model/identity decision before implementation planning.

### Phase 2 — ATDD acceptance spec

Write executable acceptance scenarios for:

- no-cache offline optimistic new bean create + brew-log create against that local bean id,
- cached existing bean create,
- pending optimistic bean reuse,
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
- Support existing refs and reusable pending optimistic bean refs; keep recipe drafts ride-along unless Phase 1 finds current API support for optimistic recipe creation.
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
- No tests for optimistic new-bean dependency creation/reuse and outbox coalescing.
- UI/server/SyncEngine/iOS SQLCipher scope creep, or additional API contract changes beyond the pinned spec update.
- Any fail-open storage/security behavior.

## Suggested later issue: iOS SQLCipher

Create an issue later, not during this run, for:

> Wire SQLCipher on iOS release builds and mirror Android wipe-then-rotate recovery.

Acceptance criteria:

- iOS links SQLCipher correctly with SQLDelight native driver.
- Release builds never open plaintext DB.
- Existing encrypted DB with missing/undecryptable key follows a deliberate recovery path.
- Logout/account-switch wipe + re-key behavior is tested.

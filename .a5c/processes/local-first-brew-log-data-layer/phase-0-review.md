# Phase 0 Review: Local-first Brew-log Data Layer

## Purpose

This is the read-only Phase 0 review artifact for the planned local-first brew-log data-layer work.

No application source code is changed by this phase. The goal is to make the next phase safe by documenting the current state, Brewer backend constraints, and decisions that must guide Phase 1.

## Current mobile state

- SQLDelight schema foundations exist for:
  - `brew_logs`
  - `brew_log_server_shadow`
  - `sync_outbox`
  - `sync_state`
  - reference caches
- Android SQLCipher/database key wiring exists.
- iOS SQLCipher is not wired and remains tracked separately in issue #50.
- `BrewLogRepository` is still server-first/synchronous in shape.
- `SubmitBrewLogUseCase` still rejects `SelectedBean.NewDraft` and `SelectedRecipe.NewDraft`.
- No local-first repository implementation exists.
- No SQLDelight DML/query layer exists yet.
- No local read model/projection exists yet.
- No brew-log repository/use-case tests exist yet.

## Brewer backend findings

Manual research was performed against `https://github.com/ybird-labs/brewer` and should be interpreted alongside the generated OpenAPI contract. The generated OpenAPI remains the mobile API boundary; Brewer source is used to identify drift, implementation behavior, and future-sync gaps.

Key findings:

1. Brew logs are **profile-scoped**, not generic user-scoped.
2. Brewer reads `x-profile-id` when present and otherwise uses the authenticated user's default profile.
3. Brewer source currently accepts optional client-generated brew-log `id` on REST create.
4. The mobile OpenAPI contract has been updated from `api-spec-v0.1.2` to `api-spec-v0.1.4`; this resolves the earlier drift where `BrewLogCreateRequest.id` was missing from the checked-in spec.
5. Current Brewer REST create still requires canonical `bean_id`.
6. Current Brewer REST is not a sync API: no event cursor, mutation replay, tombstones, `sync_version`, or `/api/v1/sync/...` endpoints exist yet.
7. Current REST response does not round-trip raw `latitude`/`longitude`, even though create accepts them.
8. Mobile `BeanDraft` / `RecipeDraft` shapes do not fully match current Brewer server resource shapes.

## Decision recorded

Use **`profile_id`** as the local ownership and future sync scope for brew logs.

Phase 1 should evaluate and plan SQLDelight ownership column renames before repository implementation:

- `brew_logs.user_id` → `profile_id`
- `brew_log_server_shadow.user_id` → `profile_id`
- `sync_outbox.user_id` → `profile_id`
- `bean_cache.user_id` / `recipe_cache.user_id` → likely `scope_profile_id`

If auth account identity is needed later, it should be named separately, e.g. `account_user_id`, and should not be used as brew-log ownership scope.

## Phase 1 must answer

- What local read model should represent pending brew logs?
- Should the app introduce `LocalBrewLog` / projection models separate from server-confirmed `BrewLog`?
- How should existing vs draft bean/recipe refs be represented?
- Where does `currentProfileId` come from in mobile today?
- Where does stable per-install `clientId` come from?
- Should SQLDelight `user_id` columns be renamed now?
- How should local draft JSON handle fields not currently supported by Brewer REST?
- Which tests belong in `commonTest` vs `androidUnitTest` for SQLDelight-backed behavior?

## Explicit non-goals for this phase

- No UI integration.
- No SyncEngine push/pull implementation.
- No server/API changes.
- No iOS SQLCipher wiring.
- No standalone offline bean/recipe CRUD.

## Related files

- `local-first-brew-log-data-layer.process.md`
- `local-first-brew-log-data-layer.mermaid.md`
- `local-first-brew-log-data-layer.mjs`
- `inputs.json`

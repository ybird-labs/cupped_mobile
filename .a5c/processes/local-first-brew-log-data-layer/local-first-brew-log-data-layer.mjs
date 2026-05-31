/**
 * @process local-first-brew-log-data-layer
 * @description High-risk KMP data-layer-only local-first brew-log foundation with research, ATDD/TDD, architecture review, and verification gates.
 * @inputs { feature: string, targetQuality?: number, maxRefinementIterations?: number }
 * @outputs { success: boolean, artifacts: object, verification: object }
 *
 * @process methodologies/spec-kit-brownfield
 * @process methodologies/atdd-tdd/atdd-tdd
 * @process babysitter/tdd-quality-convergence
 * @process specializations/mobile-development/offline-first-architecture
 * @process methodologies/process-hardening/process-hardening-patterns
 * @skill tdd /Users/jeancarlobarrios/.agents/skills/tdd/SKILL.md
 * @agent planner builtin/planner
 * @agent worker builtin/worker
 * @agent reviewer builtin/reviewer
 * @agent oracle builtin/oracle
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const feature = inputs.feature || 'Shared KMP data-layer-only local-first brew-log foundation';
  const targetQuality = inputs.targetQuality || 90;
  const maxRefinementIterations = inputs.maxRefinementIterations || 2;

  const repoScan = await ctx.task(repoScanTask, { feature });

  await ctx.breakpoint({
    title: 'Phase 0 checkpoint: brownfield scan complete',
    question: 'Review the current-state scan and approve moving into local read-model research?',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/brownfield/repo-scan.md', format: 'markdown', label: 'Repo scan' }
      ]
    },
    tags: ['phase-checkpoint', 'brownfield']
  });

  const research = await ctx.task(localReadModelResearchTask, {
    feature,
    repoScan,
    decisionToValidate: 'Use a separate local read model/projection for pending brew logs and reusable optimistic bean refs; do not mutate server-confirmed BrewLog into a sync-state carrier unless evidence supports it.',
    constraints: scopeConstraints()
  });

  await ctx.breakpoint({
    title: 'Architecture decision gate: local read model',
    question: 'Approve the researched local read-model decision before implementation planning?',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/research/local-read-model-decision.md', format: 'markdown', label: 'Local read-model ADR' }
      ]
    },
    tags: ['architecture', 'approval-gate']
  });

  const acceptance = await ctx.task(acceptanceSpecTask, {
    feature,
    research,
    constraints: scopeConstraints(),
    requiredScenarios: acceptanceScenarios()
  });

  await ctx.breakpoint({
    title: 'ATDD acceptance gate',
    question: 'Approve the acceptance scenarios and exclusions for this data-layer-only run?',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/specs/local-first-brew-log-acceptance.md', format: 'markdown', label: 'Acceptance spec' }
      ]
    },
    tags: ['atdd', 'approval-gate']
  });

  const testPlan = await ctx.task(testFirstPlanTask, { feature, research, acceptance });

  const redTests = await ctx.task(writeFailingTestsTask, { feature, research, acceptance, testPlan });
  const initialTestRun = await ctx.task(sharedTestTask, { expectPass: false, reason: 'ATDD/TDD red phase after writing failing local-first tests' });

  await ctx.breakpoint({
    title: 'Phase 3 checkpoint: red tests written',
    question: 'Review the failing tests/red phase and approve implementation work?',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/specs/test-first-plan.md', format: 'markdown', label: 'Test-first plan' }
      ]
    },
    tags: ['phase-checkpoint', 'tdd-red']
  });

  const implementation = await ctx.task(implementDataLayerTask, {
    feature,
    research,
    acceptance,
    redTests,
    constraints: scopeConstraints()
  });

  let verification = await ctx.task(sharedTestTask, { expectPass: true, reason: 'Green phase after data-layer implementation' });

  await ctx.breakpoint({
    title: 'Phase 4 checkpoint: implementation green',
    question: 'Review the green implementation summary and approve architecture/security review?',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/final/verification-summary.md', format: 'markdown', label: 'Test verification' }
      ]
    },
    tags: ['phase-checkpoint', 'tdd-green']
  });

  let review = await ctx.task(architectureSecurityReviewTask, {
    feature,
    research,
    acceptance,
    implementation,
    verification,
    targetQuality
  });

  let iteration = 0;
  while ((!review.approved || (review.qualityScore || 0) < targetQuality) && iteration < maxRefinementIterations) {
    iteration += 1;
    await ctx.breakpoint({
      title: `Refinement gate ${iteration}`,
      question: `Review found quality ${review.qualityScore || 'unknown'}/${targetQuality}. Approve refinement iteration ${iteration}?`,
      context: {
        runId: ctx.runId,
        files: [
          { path: 'artifacts/review/architecture-security-review.md', format: 'markdown', label: 'Review' }
        ]
      },
      tags: ['quality', 'approval-gate']
    });

    const refinement = await ctx.task(refineImplementationTask, {
      feature,
      research,
      acceptance,
      implementation,
      review,
      iteration
    });
    verification = await ctx.task(sharedTestTask, { expectPass: true, reason: `Verification after refinement ${iteration}` });
    review = await ctx.task(architectureSecurityReviewTask, {
      feature,
      research,
      acceptance,
      implementation: refinement,
      verification,
      targetQuality
    });
  }

  const finalChecks = await ctx.parallel.all([
    () => ctx.task(sharedTestTask, { expectPass: true, reason: 'Final shared allTests gate' }),
    () => ctx.task(apiBoundaryCheckTask, { feature }),
    () => ctx.task(scopeGuardTask, { feature, constraints: scopeConstraints() })
  ]);

  const followUps = await ctx.task(followUpIssueDraftTask, {
    feature,
    topics: [
      'iOS SQLCipher production wiring and wipe-then-rotate recovery',
      'API contract/server support for client-generated BrewLogCreateRequest.id before SyncEngine push',
      'Client SyncEngine push/pull and foreground triggers after local data-layer foundation'
    ]
  });

  await ctx.breakpoint({
    title: 'Final delivery gate',
    question: 'Approve the data-layer-only local-first brew-log foundation as complete?',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/final/verification-summary.md', format: 'markdown', label: 'Verification summary' },
        { path: 'artifacts/follow-ups/ios-sqlcipher-issue-draft.md', format: 'markdown', label: 'iOS SQLCipher follow-up issue draft' }
      ]
    },
    tags: ['final-approval', 'quality-gate']
  });

  return {
    success: true,
    feature,
    artifacts: {
      readModelDecision: 'artifacts/research/local-read-model-decision.md',
      acceptanceSpec: 'artifacts/specs/local-first-brew-log-acceptance.md',
      testPlan: 'artifacts/specs/test-first-plan.md',
      review: 'artifacts/review/architecture-security-review.md',
      verification: 'artifacts/final/verification-summary.md',
      followUps: 'artifacts/follow-ups/ios-sqlcipher-issue-draft.md'
    },
    verification: { finalChecks, review, followUps },
    metadata: { processId: 'local-first-brew-log-data-layer', targetQuality }
  };
}

function scopeConstraints() {
  return [
    'Shared KMP data layer only: shared/src/commonMain, shared/src/commonTest, shared/src/androidUnitTest as needed.',
    'No UI integration in composeApp, androidApp, or iosApp.',
    'No SyncEngine network push/pull implementation in this run.',
    'No server endpoint implementation and no additional API contract mutation beyond the pinned api-spec-v0.1.4 update in this run.',
    'No iOS SQLCipher CocoaPods/linker work in this run; document as a production enablement gate.',
    'Generated OpenAPI DTOs must remain at the network/API boundary and must not leak into domain/UI APIs.',
    'Preserve coroutine cancellation; do not swallow CancellationException.',
    'Server pulls must never overwrite unsynced local profile intent; tests must encode this invariant where touched.',
    'Bridge protocol messages must not carry raw credentials; no credential access or external side effects.'
  ];
}

function acceptanceScenarios() {
  return [
    'Given no network and no cached beans, when the user logs a brew with a new bean, then the data layer persists an optimistic local bean with a client-generated id, persists the brew log against that id, and enqueues durable create intent(s).',
    'Given a pending optimistic bean already used in one brew log, when another brew log is created offline, then the bean can be reused via the same bean_client_id/client-generated id without standalone offline bean CRUD UI.',
    'Given cached existing beans, when offline, then the data layer can persist a log referencing an existing cached/canonical bean.',
    'Given an online/remote bean is selected by a future search UI, when persisted by the data layer, then it is treated as an existing canonical bean reference.',
    'Given a pending create is edited, then the outbox remains a single active create intent and local_revision increments.',
    'Given a pending create is deleted, then the local row and outbox intent are removed as a local no-op.',
    'Given a sync_error/blocked_error row is edited, then the same active outbox row is re-armed to pending and last_error is cleared.',
    'Given local rows are observed, then reads come from SQLDelight local state/projections rather than synchronous REST create/list calls.'
  ];
}

export const repoScanTask = defineTask('repo-scan-local-first-brew-log', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Scan current brew-log data layer and constraints',
  agent: {
    name: 'planner',
    prompt: {
      role: 'KMP brownfield architecture analyst',
      task: 'Scan the repo and summarize current brew-log data-layer state, exact files touched, existing tests, SQLDelight schema, and risks before planning implementation.',
      context: args,
      instructions: [
        'Read OFFLINE_FIRST_BREW_LOG_ARCHITECTURE.md and relevant shared brew-log/domain/database files.',
        'Confirm current implementation gaps against Phase 3/4 of the architecture document.',
        'Do not modify code.',
        'Write artifacts/brownfield/repo-scan.md.'
      ],
      outputFormat: 'JSON with summary, files, risks, and artifact paths'
    },
    outputSchema: { type: 'object', required: ['summary', 'files', 'risks', 'artifacts'] }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['analysis', 'brownfield', 'kmp']
}));

export const localReadModelResearchTask = defineTask('research-local-read-model', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Research local read-model shape for pending brew logs',
  agent: {
    name: 'planner',
    prompt: {
      role: 'Domain/data architecture researcher',
      task: 'Research and decide the best local read-model shape for Cupped pending brew logs and optimistic bean refs.',
      context: args,
      instructions: [
        'Compare mutating BrewLog vs introducing LocalBrewLog/read projections vs sealed refs.',
        'Account for offline no-cache create-new flow, cached existing beans, reusable optimistic new beans from pending logs, and future online remote search.',
        'Use Cupped/Brewer terminology: brew logs are scoped to the authenticated profile id, not a generic user id.',
        'Use the generated OpenAPI contract as the mobile API boundary, and use direct Brewer implementation evidence from https://github.com/ybird-labs/brewer to explain/resolve contract drift or missing future-sync details.',
        'Explicitly decide whether existing SQLDelight columns named user_id should be renamed to profile_id now, or kept as a legacy storage name with an explicit profile-id mapping.',
        'Account for Brewer current REST constraints: OpenAPI api-spec-v0.1.4 supports optional client-generated ids for brew-log create and bean create; brew-log create still requires bean_id; no sync API/event cursor/idempotent mutation replay exists yet.',
        'Decide whether pending new beans should be first-class optimistic local resources (bean_cache + outbox dependency) or embedded draft JSON with a later promotion path.',
        'Identify the source of authenticated profile id and stable per-install client_id needed by local rows/outbox.',
        'Recommend SQLDelight repository test source-set strategy and optimistic bean/draft JSON serialization strategy, including how to handle mobile BeanDraft/RecipeDraft fields that do not currently match Brewer resource shapes.',
        'Keep generated DTOs out of domain/UI APIs.',
        'Prefer the smallest model that can represent sync_status, last_sync_error, deleted tombstone, existing refs, and optimistic/draft refs honestly.',
        'Write artifacts/research/local-read-model-decision.md as an ADR with rejected alternatives.'
      ],
      outputFormat: 'JSON with decision, rationale, rejectedAlternatives, risks, artifacts'
    },
    outputSchema: { type: 'object', required: ['decision', 'rationale', 'rejectedAlternatives', 'risks', 'artifacts'] }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['research', 'architecture', 'domain-model']
}));

export const acceptanceSpecTask = defineTask('write-atdd-acceptance-spec', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Write ATDD acceptance specification',
  agent: {
    name: 'planner',
    prompt: {
      role: 'ATDD specification writer',
      task: 'Convert agreed scope into executable acceptance scenarios for shared KMP data-layer tests.',
      context: args,
      instructions: [
        'Use Given/When/Then language.',
        'Mark must-have vs follow-up scenarios.',
        'Explicitly list exclusions: UI, SyncEngine, server changes, additional API contract changes beyond the pinned api-spec-v0.1.4 update, iOS SQLCipher wiring.',
        'Write artifacts/specs/local-first-brew-log-acceptance.md.'
      ],
      outputFormat: 'JSON with scenarios, exclusions, risks, artifacts'
    },
    outputSchema: { type: 'object', required: ['scenarios', 'exclusions', 'risks', 'artifacts'] }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['atdd', 'specification']
}));

export const testFirstPlanTask = defineTask('plan-tdd-tests', (args, taskCtx) => ({
  kind: 'skill',
  title: 'Plan TDD tests for local-first data layer',
  skill: {
    name: 'tdd',
    context: {
      task: 'Create a red-green-refactor test plan for the local-first brew-log data-layer foundation.',
      input: args,
      instructions: [
        'Plan tests before implementation.',
        'Prioritize repository/use-case and SQLDelight invariant tests.',
        'Include local create, optimistic new-bean dependency creation, pending bean reuse, outbox uniqueness/coalescing, and Flow read projection tests.'
      ]
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['tdd', 'planning']
}));

export const writeFailingTestsTask = defineTask('write-failing-local-first-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: 'RED: write failing local-first data-layer tests',
  agent: {
    name: 'worker',
    prompt: {
      role: 'KMP TDD implementer',
      task: 'Write failing tests for the approved local-first brew-log data-layer scenarios. Do not implement production behavior yet beyond minimal compile scaffolding if needed.',
      context: args,
      instructions: [
        'Use shared/common tests where platform-neutral; use androidUnitTest only for JVM SQLDelight/migration constraints when needed.',
        'Assert behavior, not implementation details, except for durable DB/outbox invariants.',
        'Keep tests deterministic with injected dispatchers/time/id providers where introduced.',
        'Return actual files changed and red-test results.'
      ],
      outputFormat: 'JSON with filesModified, testsAdded, notes'
    },
    outputSchema: { type: 'object', required: ['filesModified', 'testsAdded', 'notes'] }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['tdd', 'red', 'implementation']
}));

export const implementDataLayerTask = defineTask('implement-local-first-data-layer', (args, taskCtx) => ({
  kind: 'agent',
  title: 'GREEN: implement local-first brew-log data layer',
  agent: {
    name: 'worker',
    prompt: {
      role: 'Senior Kotlin Multiplatform data-layer engineer',
      task: 'Implement the shared KMP local-first brew-log data-layer foundation to satisfy the approved tests and acceptance spec.',
      context: args,
      instructions: [
        'Implement SQLDelight labeled queries and typed enum adapters where useful for consumed columns.',
        'Rewrite repository/use-case semantics to local-first shared data layer without UI or SyncEngine network work.',
        'Support existing bean refs, optimistic new-bean refs, and reuse of pending optimistic refs via client-generated ids.',
        'Ensure DB row + outbox intent are written atomically in one transaction.',
        'Implement outbox active uniqueness/coalescing behavior required by accepted scope.',
        'Preserve coroutine cancellation and avoid hard-coded dispatchers where async behavior is introduced.',
        'Do not modify generated DTOs or leak API DTOs into domain APIs.'
      ],
      outputFormat: 'JSON with filesModified, behaviorImplemented, testsRun, caveats'
    },
    outputSchema: { type: 'object', required: ['filesModified', 'behaviorImplemented', 'testsRun', 'caveats'] }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['implementation', 'kmp', 'sqldelight']
}));

export const refineImplementationTask = defineTask('refine-local-first-data-layer', (args, taskCtx) => ({
  kind: 'agent',
  title: 'REFACTOR: address review findings',
  agent: {
    name: 'worker',
    prompt: {
      role: 'Senior KMP refactoring engineer',
      task: 'Address architecture/security/test review findings without expanding scope.',
      context: args,
      instructions: [
        'Make the smallest safe changes that resolve blocking review findings.',
        'Do not add UI, SyncEngine network work, server changes, additional API contract changes beyond the pinned spec update, or iOS SQLCipher wiring.',
        'Keep tests passing and update tests only when behavior expectations were wrong.'
      ],
      outputFormat: 'JSON with filesModified, reviewItemsAddressed, testsRun, remainingRisks'
    },
    outputSchema: { type: 'object', required: ['filesModified', 'reviewItemsAddressed', 'testsRun', 'remainingRisks'] }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['tdd', 'refactor', 'quality']
}));

export const architectureSecurityReviewTask = defineTask('architecture-security-review', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Review architecture, persistence, and security boundaries',
  agent: {
    name: 'reviewer',
    prompt: {
      role: 'Adversarial KMP architecture and persistence reviewer',
      task: 'Review the implementation against the architecture invariant and project rules.',
      context: args,
      instructions: [
        'Check that server/API DTOs do not leak into domain/UI APIs.',
        'Check that local writes preserve user intent and outbox invariants.',
        'Check that no UI, SyncEngine network, server changes, additional API contract changes beyond the pinned spec update, or iOS SQLCipher wiring slipped into scope.',
        'Check auth/storage/logging safety: no secrets logged, no fail-open storage behavior introduced.',
        'Score quality 0-100 and write artifacts/review/architecture-security-review.md.'
      ],
      outputFormat: 'JSON with approved, qualityScore, blockingIssues, recommendations, artifacts'
    },
    outputSchema: { type: 'object', required: ['approved', 'qualityScore', 'blockingIssues', 'recommendations', 'artifacts'] }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['review', 'architecture', 'security']
}));

export const sharedTestTask = defineTask('run-shared-tests', (args, taskCtx) => ({
  kind: 'shell',
  title: 'Run shared KMP tests',
  shell: {
    command: './gradlew :shared:allTests',
    timeoutSeconds: 1800
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['verification', 'gradle', 'shared']
}));

export const apiBoundaryCheckTask = defineTask('api-boundary-check', (args, taskCtx) => ({
  kind: 'shell',
  title: 'Check generated DTO boundary is not expanded',
  shell: {
    command: "rg -n 'BrewLogCreateRequest|Generated|Dto|DTO' shared/src/commonMain/kotlin/cafe/cupped/app/brewlog shared/src/commonMain/kotlin/cafe/cupped/app/db || true",
    timeoutSeconds: 120
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['verification', 'api-boundary']
}));

export const scopeGuardTask = defineTask('scope-guard-check', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Verify implementation stayed within agreed scope',
  agent: {
    name: 'oracle',
    prompt: {
      role: 'Scope and decision consistency oracle',
      task: 'Verify the implementation stayed inside the agreed data-layer-only scope and did not silently make deferred architecture decisions.',
      context: args,
      instructions: [
        'Inspect changed files and summarize any scope drift.',
        'Flag UI, SyncEngine network, server/API contract, or iOS SQLCipher changes as blockers unless explicitly approved.',
        'Confirm follow-up gates are documented.'
      ],
      outputFormat: 'JSON with inScope, blockers, warnings, summary'
    },
    outputSchema: { type: 'object', required: ['inScope', 'blockers', 'warnings', 'summary'] }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['verification', 'scope']
}));

export const followUpIssueDraftTask = defineTask('write-follow-up-issue-drafts', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Write follow-up issue drafts for deferred gates',
  agent: {
    name: 'planner',
    prompt: {
      role: 'Technical issue writer',
      task: 'Draft follow-up issues for deferred gates; do not create external GitHub issues.',
      context: args,
      instructions: [
        'Write an iOS SQLCipher production wiring issue draft with acceptance criteria.',
        'Write short follow-up notes for API client-generated ID and SyncEngine phases.',
        'Do not call external services or create GitHub issues.',
        'Write artifacts/follow-ups/ios-sqlcipher-issue-draft.md.'
      ],
      outputFormat: 'JSON with issueDraftPaths and summaries'
    },
    outputSchema: { type: 'object', required: ['issueDraftPaths', 'summaries'] }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/output.json` },
  labels: ['planning', 'follow-up']
}));

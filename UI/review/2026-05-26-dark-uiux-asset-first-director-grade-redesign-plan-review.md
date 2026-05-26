# Dark UI/UX Asset-First Director-Grade Redesign Plan Review

## Findings

### P0

- No P0 finding. The proposal does not ask to change gameplay rules, save/replay/profile schema, manifest schema, or core enum contracts.

### P1

- [P1] 方向正确，但这份文档已经是 execution plan，却没有先通过 `UI/pr` 的正式 PR 合同入口。
  Evidence: plan declares `Status: proposal / execution plan` and owner `client + assets + tools` (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:3-6`), then defines sprint tasks, artifacts, acceptance matrix and gate ladder (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:135-429`). `UI/pr/development-governance.md` requires each `UI/pr/dark-uiux-pr*.md` to have governance matrix, gate budget, canonical artifact and failure rule before execution (`UI/pr/development-governance.md:18-37`, `UI/pr/development-governance.md:58-71`). Current plan intentionally does not update the PR index (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:125-133`).
  Impact: Sprint 0/1 can remain proposal work, but Sprint 2+ would create formal resources and runtime changes without a formal `ownerPr`, indexed golden namespace, screen-matrix routing, acceptanceContractLint coverage, or PR close semantics. This is exactly the class of work that previously drifted into large renderer/golden churn.
  Fix direction: add a blocking `Sprint -1: Promotion Gate` before any resource/runtime mutation. It should choose one formal route: `PR-08 Director Grade Asset Reset`, `PR-02-2 demo parity reroll`, or a bounded PR-05/PR-06 resource repair. Once chosen, update `UI/pr/README.md`, `screen-coverage-matrix.md`, acceptance lint rules if needed, and use that owner in every target key, golden label and report artifact.

- [P1] Sprint 2 resource ownership is currently wrong for the main first-screen ruins keys.
  Evidence: plan says ruins ground/wall should preferably reuse existing `r02-tiles-ground` / `r02-tiles-wall` owner (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:225-258`). Current repo contract says `tileset.ruins.ground_01`, `tileset.ruins.wall_01`, `actor.vanguard`, and `prop.stairs.down` are already `PR-02-2` owner keys with `r02-ui-demo-ruins-tiles` / `r03-ui-demo-actor-props` (`UI/sprite-sheets/owner-contracts/pr02-2-owner-keys.yaml`, `UI/sprite-sheets/key-registry.yaml`). `UI/pr/README.md` also records PR-02-2 as the `ui-demo-new` visual parity owner (`UI/pr/README.md:102-109`, `UI/pr/README.md:245-246`).
  Impact: executing Sprint 2 as written could silently migrate `tileset.ruins.*` from PR-02-2 to PR-05-style ownership, or duplicate the same visual authority under a new polish route. That would break final-full inventory, owner-scope coverage, and PR05 source rules.
  Fix direction: split Sprint 2 into two mutually exclusive paths:
  1. `Path A: PR-02-2 reroll existing first-screen cells` keeps sheetId/row/col/targetKey/outputPath stable and only refreshes raw sheet, processed PNG, contact sheet, hashes, coverage and evidence.
  2. `Path B: formal owner migration` updates PR-02-2 owner contract, PR05 source rules, final-full inventory, coverage tasks, manual records and evidence labels in one PR. Do not mix both.

- [P1] Final runtime parity marks packaged app whitebox as optional, but this surface is package-facing and PR07 already failed exactly there.
  Evidence: DG-07 says whitebox is `optional packaged app` (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:391-404`). Governance says `clientSmoke`, `goldenScreenshot`, resource lint and packaged whitebox do not replace each other (`UI/pr/development-governance.md:13-16`), and PR-07/package-facing UI must keep packaged app whitebox (`UI/pr/development-governance.md:39-56`). Current PR07 audit records a real packaged whitebox failure on 2026-05-24 due to visual quality (`UI/review/dark-uiux-final-doc-implementation-audit.md:15-33`, `UI/review/dark-uiux-final-doc-implementation-audit.md:79-83`).
  Impact: debug golden parity can pass while packaged rendering still fails the actual director gate. Making packaged evidence optional preserves the failure mode that triggered this plan.
  Fix direction: keep packaged whitebox `N/A` only for target comp / docs-only Sprint 0/1. For Sprint 4 close, make packaged whitebox required, with an explicit fallback policy if Computer Use cannot bind the libGDX window.

- [P1] Gate ladder must not lead UI/UX exploration; Gradle/program verification should be deferred until visual acceptance.
  Evidence: plan correctly says golden and heavy gates should not be daily aesthetic feedback (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:38-46`), but later still presents a Gradle-heavy ladder as the recommended execution sequence (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:406-429`).
  Impact: this is a UI/UX director-grade reset, not primarily a program-correctness bugfix. If every visual idea first pays `acceptanceContractLint`, resource lint, client tests, golden, maintainability and `verifyChanged`, the team will keep optimizing for green automation and hash churn instead of first-eye quality. That is why several days of patching can produce little visible improvement.
  Fix direction: split the workflow into `Visual Feedback Loop` and `Program Closure Loop`. In Visual Feedback Loop, stop Gradle tests by default; use target comp, contact sheet, static downscaled previews, runtime/package screenshots when cheap, and manual side-by-side review. Only after UI/UX director acceptance should the plan run Gradle/resource/golden/verifyChanged to validate program integration and regressions.

### P2

- [P2] Acceptance Matrix is useful, but it is not yet the repo-required dark UI/UX matrix.
  Evidence: plan matrix has `requirementId / Requirement / Owner / Fast Check / Owner Gate / Artifact / Whitebox` (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:391-404`). Governance requires `source`, gate budget, canonical artifact and failure rule, and forbids `TBD`-style gaps (`UI/pr/development-governance.md:18-37`, `UI/pr/development-governance.md:58-92`).
  Fix direction: add `source`, `dependencies`, `freshness`, `failureRule`, `canonicalArtifact`, and `promotionState` fields. For DG-02/DG-03, artifact must include specific owner coverage report path, not generic "coverage artifact".

- [P2] The target comp workflow needs a sliceability contract, not just a visual pass/fail.
  Evidence: Sprint 1 requires target comp acceptance and then cutting into resource families (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:181-219`). This is directionally right, but it does not force each visual element to prove it can become a legal sheet cell, manifest key, runtime consumer and test.
  Impact: a target comp can look good while being non-sliceable: baked shadows/text, non-tileable floors, nine-slice chrome that cannot be represented by the current manifest, or lighting that only works as a full-screen paintover.
  Fix direction: before `accepted`, require a table for every resource family: `ownerPr`, `sheetId`, `targetKey`, `category`, `displaySize`, `tiling/stretch strategy`, `consumer`, `consumerTest`, `fallbackKey`, `requiresNewSchema=no`, `packagedRisk`. Reject target comps that need baked UI text, atlas/region schema, or full-screen paintover to work.

- [P2] `director-grade-asset-readiness.json` is named but not specified.
  Evidence: plan lists `build/reports/verification/dark-uiux/director-grade-asset-readiness.json` as a long-term artifact (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:116-123`) but no schema, producer, consumer or gate is defined. Resource governance treats pipeline reports as authority-bearing artifacts (`docs/rule/ai-change-governance.md:276-284`).
  Fix direction: either remove the artifact from the plan, or define schema and producer now. Minimum fields: `schemaVersion`, `ownerPr`, `sourcePlan`, `resourceFamily`, `targetKey`, `sheetId`, `canonicalRawOutputPath`, `runtimeRawOutputPath`, `contactSheetPath`, `qaStatus`, `directorVerdict`, `requiredGates`, `blockingFindings`.

- [P2] New golden labels are not owner-routed.
  Evidence: plan introduces `director-grade-*` labels under `client/build/reports/golden/dark-uiux-director-grade/` (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:350-363`). Current README requires PR-owned golden labels and records exceptions explicitly (`UI/pr/README.md:22-38`, `UI/pr/README.md:237-253`).
  Fix direction: once promoted, use labels such as `dark-uiux-pr08-director-parity-1672x941`, or explicitly add `director-grade-*` as a documented exception with owner, evidence index and cleanup policy.

- [P2] Map-stage dark field / lighting needs a strict layer contract.
  Evidence: plan wants real map-stage background/shadow/vignette while preserving deterministic visibility fog (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:277-290`). That is the right approach, but it currently lacks layer order invariants.
  Impact: decorative darkness can accidentally hide actor/loot/telegraph readability or become a second visibility authority.
  Fix direction: define layer order explicitly: `base stage art -> semantic tile/floor/wall -> deterministic visibility fog -> gameplay overlays/actors/loot/telegraph -> focus/selection -> UI chrome`. Add tests that prove visibility semantics remain unchanged and decorative darkness cannot hide required gameplay markers.

- [P2] Current dirty worktree shows the old failure mode is still active.
  Evidence: current diff has 39 modified tracked files, about 10k insertions, including large changes in `TileRenderer.kt`, `DemoShellRenderer.kt`, `TileRendererCanvasTest.kt`, resources, game validation and tools.
  Impact: executing this plan on top of the current patch risks preserving the renderer-patch accumulation it is trying to stop.
  Fix direction: before Sprint 0 acceptance, freeze current evidence, then either quarantine this branch as failed attempt evidence or start the asset-first implementation from a clean branch. Do not continue by layering Sprint 2/3 over the existing renderer-heavy patch.

### P3

- [P3] `UI/targets/` is a new long-lived directory with no ownership note. Add a short README or document it in the formal PR so target comps do not become unindexed art dumps.
- [P3] Gate ladder uses broad `:client:test --tests 'com.ktome.client.render.*'`; prefer explicit focused tests per sprint, then clientSmoke/golden after resource direction is accepted.
- [P3] The plan should say that Sprint 0/1 are allowed to fail cheaply and should not update golden baselines. It implies this, but making it explicit will prevent premature hash acceptance.

## Requirement Alignment

- Requirement: stop renderer-first aesthetic patching and move to asset-first visual production.
  Evidence: plan diagnosis and stop/start rules (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:8-19`, `UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:90-110`); current PR07 audit shows renderer polish still failed director review (`UI/review/dark-uiux-final-doc-implementation-audit.md:79-83`).
  Conclusion: 一致.

- Requirement: preserve K-ToME module and resource authority boundaries.
  Evidence: plan lists canonical manifest, key registry, sheet plan, runtime manifest sync and no gameplay/schema changes (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:67-88`); `UI/pr/README.md` defines key registry and manifest authority (`UI/pr/README.md:121-148`).
  Conclusion: 部分一致. The authority chain is named correctly, but Sprint 2 owner routing conflicts with existing PR-02-2 ownership.

- Requirement: resource work must be executable through registry, sheet plan, manifest, coverage and evidence.
  Evidence: plan includes resource gates and canonical/runtime sync (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:243-258`, `UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:406-429`); governance requires formal resource artifacts and gate budget (`UI/pr/development-governance.md:32-56`, `UI/pr/development-governance.md:73-92`).
  Conclusion: 部分一致. The gates are directionally correct, but the executable PR promotion, exact owner reports and readiness schema are missing.

- Requirement: player-visible UI must be validated with real client evidence and whitebox when package-facing.
  Evidence: plan uses focused tests, golden and manual director review; DG-07 marks packaged app optional. Governance and PR07 evidence require packaged whitebox for package-facing UI.
  Conclusion: 部分一致.

- Requirement: target comp should not become a second truth.
  Evidence: plan says target comp must not be manifest truth and cannot contain baked text (`UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md:181-219`).
  Conclusion: 一致, with a recommended sliceability hardening before runtime integration.

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| Overall direction | Asset-first vertical slice, target comp first, runtime as compositor | 部分实现 | plan §0/§4, current PR07 failure audit | Strategy is correct, but execution status is not yet formalized as a PR contract | High |
| Resource ownership | Reuse existing owner or declare new owner | 偏离实现 | plan Sprint 2.1; PR-02-2 owner contract | Ruins first-screen keys are PR-02-2 today, not generic PR-05 `r02-tiles-*` work | High |
| Manifest / key registry | Canonical-first, runtime sync, no bare runtime keys | 部分实现 | plan §3/Task 2.2; README manifest authority | Correct contract names, missing formal readiness schema and exact owner route | Medium |
| Renderer role | Renderer only composes layout, fog, selection, telegraph, light | 部分实现 | plan Task 2.3/2.4; current diff stat | Correct target, but current dirty patch still contains large renderer-heavy changes | High |
| Golden / evidence | New labels and side-by-side review after direction accepted | 部分实现 | plan Sprint 4 | New labels lack PR owner namespace; packaged app evidence is optional | Medium |
| Governance / feedback loop | acceptanceContractLint, gate ladder, verifyChanged | 部分实现 | plan §7/§8; development-governance | Missing full repo-required governance section, gate budget and failure rules; Gradle-heavy verification should not lead visual exploration | High |

## 玩法与体验审查

### 核心循环

The plan correctly centers the map as the first-screen stage. This matters because K-ToME's loop is exploration -> tactical decision -> reward/build adjustment. If the map still reads as a dim procedural grid, the player never gets the "where am I and what is happening" anchor. Asset-first floor/wall/lighting is the right foundation.

### 战斗体验

The biggest risk is overcorrecting atmosphere and losing gameplay markers. The plan already states actor/loot/telegraph must remain more readable than materials. That should become a hard testable layer contract, especially for telegraph and selection overlays.

### 成长与构筑驱动

Right panel and bottom HUD material reset can improve build identity because equipment, inscriptions, backpack and active actions will read as one character console. Current screenshots still show the data is present but the material hierarchy is weaker than the reference. The plan addresses this at the right level.

### 探索与新鲜感

Target comp first is valuable because map-stage composition is not only tile texture quality. The current runtime evidence has repetitive room geometry, regular dark blocks and weak non-rectangular pressure. The plan should explicitly allow target comp to drive scenario staging only through validation/demo setup, not through gameplay rule changes.

### 新手体验与信息反馈

Text/readability constraints are present, but the target comp acceptance should include Chinese text stress cases, hotkey density and long log lines. Otherwise a visually good target can regress actual K-ToME readability.

### 系统耦合与体验断层

The plan is trying to reduce coupling by moving art quality out of renderer micro-patches. That is correct. The current weak point is ownership coupling: PR-02-2, PR-05, PR-06 and a future PR-08 have overlapping claims unless the promotion gate picks one route.

## 当前阶段必须解决的问题

- Problem: choose formal execution owner before resource/runtime work.
  Why now: without ownerPr and PR index routing, every later targetKey/golden/manual record can drift.
  Why not defer: resource paths and golden labels are cheap to name correctly now and expensive to migrate later.
  Fix direction: add `Sprint -1 Promotion Gate`; promote to PR-08 or narrow to PR-02-2 reroll.
  Priority: P1.

- Problem: repair Sprint 2 ownership for ruins map-stage keys.
  Why now: `tileset.ruins.*` are the core first-screen assets and already have a committed owner.
  Why not defer: replacing the wrong sheet/owner invalidates coverage evidence and final-full inventory.
  Fix direction: make PR-02-2 reroll the default for current `ui-demo-new` first-screen terrain, and require explicit migration if any key moves to another owner.
  Priority: P1.

- Problem: make packaged whitebox required for final runtime parity.
  Why now: PR07 failed in packaged UI review, not in abstract design.
  Why not defer: debug goldens cannot prove packaged app acceptance.
  Fix direction: DG-07 whitebox should be `required` for Sprint 4; only Sprint 0/1 target-comp docs may mark it `N/A` or `skipped`.
  Priority: P1.

- Problem: stop Gradle-heavy verification from driving the UI/UX exploration loop.
  Why now: the current failure is visual direction and material quality, not first-order program correctness.
  Why not defer: if the team pays heavy automation before each visual decision, iteration remains slow and incentives stay pointed at hash/test churn instead of UI quality.
  Fix direction: explicitly pause Gradle tests during target comp/contact sheet/screenshot review; run Gradle gates only after the UI/UX artifact is accepted or when a suspected code bug blocks visual inspection.
  Priority: P1.

- Problem: define sliceability before target comp acceptance.
  Why now: otherwise the plan can produce attractive concept art that cannot enter the current manifest/renderer contract.
  Why not defer: non-sliceable art will push the team back into renderer hacks.
  Fix direction: target comp record must include resource-family mapping and reject any visual element requiring new manifest schema unless a separate manifest epoch PR is approved.
  Priority: P2.

## Removal/Iteration Plan

## Safe to Remove Now

No current production code is safe to remove from this review alone. The review scope is the plan and adjacent evidence; the current large dirty diff was not audited line-by-line.

## Defer Removal

### Item: Renderer micro-rectangles and procedural material overlays

| Field | Details |
| --- | --- |
| Location | `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`, `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt` |
| Phase/Work Package | dark UI/UX director-grade reset; formal owner pending |
| Touched contract | client rendering / visual evidence / golden |
| Evidence | plan identifies renderer rectangles and fog layers as the current diminishing-return path |
| Preconditions | accepted target comp, owner-routed resource replacement, focused readability tests, golden and packaged parity evidence |
| Deletion or iteration steps | 1. classify current renderer art passes as gameplay-critical vs material simulation; 2. replace material simulation with bitmap resources; 3. keep only deterministic fog/light/selection/telegraph compositing; 4. rerun focused layer-order tests and golden |
| Affected harness/gates | `TileRendererCanvasTest`, `DemoShellRendererTest`, `:client:clientSmoke`, `:client:goldenScreenshot`, packaged whitebox |
| White-box check | side-by-side target comp vs runtime packaged shell |
| Rollback or fallback | restore previous renderer pass only if a specific gameplay marker becomes unreadable and no resource-level fix is available |

## Additional Suggestions

- Add a first-page decision table: `Recommended route = PR-08`, `Non-goal = no gameplay/schema change`, `Current dirty renderer patch = evidence only / not implementation base unless explicitly audited`.
- Keep Sprint 0 and Sprint 1 as cheap visual gates: no Gradle, no golden baseline updates, no manifest changes, only repo-relative manual records, target artifacts, contact sheets, side-by-side screenshots and explicit accept/reject notes.
- Add a rule that Gradle is paused during UI/UX exploration unless a program bug blocks inspection. Treat automation as closure after visual acceptance, not as the source of aesthetic feedback.
- Add one fixed "director rubric" table with pass/fail thresholds: map dominance, wall/floor material, dark-field shape, actor/loot/telegraph priority, right-panel unity, bottom console unity, Chinese readability, no baked text, no old-style residue, package parity.
- For each asset family, require one contact sheet plus one downscaled runtime-size preview. A beautiful 128px or 256px cell is not enough if the runtime uses it at 32px/48px.
- If shell chrome needs nine-slice or tiling behavior, document whether it can be done with existing single-PNG resources. If not, stop and open a manifest epoch PR instead of hiding stretch assumptions in renderer code.
- Treat `UI/UI-demo-new.png` as quality bar, not mapping truth. The actual mapping truth must remain `sheet-plan.yaml`, `key-registry.yaml`, canonical manifest and focused tests.

## Suggested Verification

Commands actually run for this review:

- `git status --short`
- `git diff --stat`
- Read plan and authority docs with `sed` / `rg`
- Opened current reference and golden screenshots for visual comparison

Recommended during UI/UX exploration:

- Do not run Gradle by default.
- Produce/update target comp or runtime screenshot evidence.
- Review contact sheets and downscaled previews for visual quality.
- Record side-by-side accept/reject notes in `UI/manual-records/` or `UI/review/`.
- Use `git diff --check` only for touched Markdown/report files if a document is edited.

Recommended after editing the plan only:

```bash
git diff --check -- UI/pr/dark-uiux-asset-first-director-grade-redesign-plan.md UI/review/2026-05-26-dark-uiux-asset-first-director-grade-redesign-plan-review.md
```

Recommended only after UI/UX director acceptance, when the accepted direction is being integrated into resource/runtime code:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew assetLint styleLint manifestLint resourcePipelineLint
./gradlew :tools:darkManifestCoveragePr02_2OwnerScope
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
./gradlew :client:clientSmoke
./gradlew :client:goldenScreenshot
./gradlew maintainabilityLint
./gradlew verifyChanged
```

If promoted to PR-08 or any package-facing execution PR, add packaged app verification:

```bash
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=<formal-scenario-id>
build/whitebox/<formal-scenario-id>/launch-packaged-app.sh
```

## Summary

The big direction is correct: the project has hit the ceiling of renderer-first visual patching, and an asset-first target-comp -> resource -> manifest -> runtime compositor loop is the right reset. The plan also names the right stable boundaries: no gameplay/schema changes, canonical manifest first, no baked text, no image-as-truth.

The plan is not yet safe to execute as written. It must first pick a formal PR/owner route, fix the PR-02-2 vs PR-05 ruins ownership mismatch, split UI/UX visual feedback from program verification, make packaged runtime parity required at close, and define sliceability/readiness artifacts precisely enough that good concept art cannot leak into renderer hacks or second authorities.

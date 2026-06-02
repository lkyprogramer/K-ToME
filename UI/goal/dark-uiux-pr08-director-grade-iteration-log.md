# Dark UI/UX PR-08 Director-Grade Iteration Log

> Date: 2026-05-27
> Status: `pr08-topology-risk-interior-seam-dissolve-v1-accepted-forward`
> Parent goal: `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`

## Usage

Append one entry per goal turn. Every entry must describe actual evidence, not expected outcomes.

Required fields:

1. date
2. loop type
3. target area
4. changed files
5. visual evidence paths
6. commands actually run
7. command result
8. director verdict
9. next action

## Current Baseline

| Field | Value |
| --- | --- |
| Direction | `resource-family-first reset` |
| Evidence index | `UI/review/dark-uiux-pr08-evidence-and-direction.md` |
| PR contract | `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md` |
| Target comp record | `UI/manual-records/dark-uiux-director-grade-target-comp.md` |
| Family-pack contract | `UI/targets/dark-uiux-director-grade-target-family-pack.md` |
| Direction A execution plan | `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md` |
| Unified lky decision packet | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-decision.md` |
| Supplemental prototype reference | `UI/design/In-Run Shell.html` and selected `UI/design/components/*.jsx` files |
| Current status | Candidate W authored room source is accepted over the current `ui.map_stage.ruins.room_plate.pr08_demo` resource foundation; D3 map-stage closure is accepted for the `tileset.ruins` proof slice only; PR08 director runtime evidence has its own `dark-uiux-pr08-director-*` harness and review packet; packaged recapture after the right-panel utility bridge pass exists with display-size limitation; the explicit golden rebaseline and owner gate ladder now pass for the accepted ruins proof slice; first non-ruins family proof exists for `tileset.forest_edge`, `tileset.mine` and `tileset.shadow_depths`; shadow-depths V4 supersedes V3 as current accepted-forward right-mass resource after golden and packaged recapture; sibling packaged validation scenarios now exist for the three non-ruins family crops and `preparePhase4V4Whitebox` materializes each runbook/expected-evidence packet; forest-edge, mine and shadow-depths now all have packaged evidence-summary captures and runtime `evidence_summary_opened` logs; the D8 ruins random-seed board records `accepted-with-topology-contract`, topology contract V1 keeps full-room plate drawing only for plate-safe visible regions, topology-risk hybrid V1 keeps runtime tile material visible while adding true-topology material fields plus boundary marks for risky regions, topology-risk decomposition V1 draws canonical wall-family crown, side and door-contact components over risky shapes, topology-risk AO/local-light V1 adds visible-run ambient depth plus deterministic warm pools, source-cropped room plate V1 added a true source-region crop contract, dedicated topology source A V1 routed `tileset.ruins` topology-risk rooms through `ui.map_stage.ruins.room_topology_source.pr08_demo` instead of sampling the accepted full-room plate, dedicated topology source B V1 superseded A, C superseded B, and dedicated topology source D V1 now supersedes C as the current same-key topology source; this prevents unsafe bbox stretching and removes full-room source reuse from the current ruins risky path, and the mask/crop diagnostic is now a repeatable `client` golden owner artifact; topology-risk source-alpha V1 raises dedicated source crop bands from `0.36` to `0.42`, source-alpha scope V1 now limits that stronger alpha to formal dedicated topology sources while non-ruins fallback family room-plate bands stay at `0.36`, and topology-risk interior seam dissolve V1 now adds long-run internal shared-seam fields in the hybrid path without covering hidden notches or replacing boundary wall components; the current PR08 director recapture writes impact evidence but still fails expected-hash assertions and is not rebaselined; topology-risk rows remain accepted-forward only below plate-safe authored-room quality; right-panel, bottom-deck, bottom-action, left-nav, bottom-strip final-read and right-panel utility bridge shell passes are accepted-forward only |
| Direction checkpoint | Direction A is selected for the map: pre-rendered room art plate via client-only presentation layer; stop default alpha/seam/rail/rectangle tuning |
| Multi-map rollout boundary | First runtime proof is `tileset.ruins`; non-ruins family proof now has packaged core crop and evidence-summary capture for forest-edge, mine and shadow-depths V4, but it is not broader-topology or all-map closure |
| Agent-owned D3 decision gate | Candidate W closes D3 for the `tileset.ruins` fixed proof slice only; the final ruins runtime packet, current packaged telegraph evidence, explicit golden rebaseline and owner gate ladder are now reproducible; non-ruins family route is accepted-forward, with shadow-depths V4 improved and packaged core plus summary evidence captured, but still not procedural-generalization or all-map closure |
| Next slice | Resolve the D9 real-procgen ROI/freeze checkpoint before broad Direction A rollout or further same-family topology-risk polish; do not claim procedural/all-map closure, update director hashes, add another governance-only accepted-forward update, or return to small alpha/fog/line-weight/single-cell rectangle tuning from interior seam dissolve V1 alone |

## Entry Template

```text
### YYYY-MM-DD - <short title>

- Loop type:
- Target area:
- Changed files:
- Visual evidence:
- Commands run:
- Result:
- Director verdict:
- Remaining risk:
- Next action:
```

## 2026-06-02 - PR08 Second Feedback Execution Lock

- Loop type: documentation-only review feedback assessment
- Target area: PR-08 D9/freeze execution lock and historical micro-tuning
  failure-counter accounting
- Changed files:
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - N/A; this entry records feedback interpretation only and does not generate
    new D9/procgen or runtime visual evidence.
- Commands run:
  - read the latest pasted review feedback from the Codex attachment
  - `ps -axo pid,ppid,stat,etime,command | rg 'git (push|remote-https)|git-remote-https|git-remote-http'`
  - `git status --short --branch`
  - `git ls-remote origin refs/heads/main`
  - `git log -1 --oneline --decorate`
  - targeted `rg` and `sed` reads of the PR-08 goal, room-art plan and log
- Result:
  - UPDATED: the goal and plan now state that another governance-only edit is
    not PR-08 progress; the next valid progress packet is D9 evidence or the
    D9-backed ROI/freeze decision.
  - UPDATED: the source A/B/C/D, source-alpha and interior-seam sequence is now
    treated as historical same-family failure-counter input, so its
    `accepted-forward` status cannot justify additional ruins same-family
    polish before D9/freeze.
  - CONFIRMED: before this entry's new edits, the previously interrupted
    `git push` was no longer running, and `origin/main` matched local `HEAD` at
    `e0bb21a6`.
  - NOT RUN: Gradle, golden screenshot, packaged whitebox or resource gates.
    This entry is documentation-only and makes no Kotlin/resource/runtime
    changes.
- Director verdict:
  - Feedback is accepted only as a small execution lock, not as a reason to add
    a third governance framework. The existing D9/freeze rules are sufficient;
    the next closure-oriented action must execute them.
- Remaining risk:
  - D9 statistics are still not materialized.
  - The ruins freeze decision is still pending D9-backed evidence or an explicit
    checkpoint decision.
- Next action:
  - Run or implement the D9 real-procgen distribution probe, then record the
    Direction A ROI and ruins freeze decision.

## 2026-06-02 - PR08 Review Feedback Convergence Governance

- Loop type: documentation-only review feedback absorption
- Target area: PR-08 Direction A goal convergence, topology-risk ROI and
  accepted-forward escalation semantics
- Changed files:
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
- Commands run:
  - read the pasted second-round review feedback from the Codex attachment
  - `git status --short`
  - `rg --files UI/goal`
  - targeted `rg` and `nl -ba` reads of the PR-08 goal, room-art plan, log,
    D8 decision packet, D8 evidence index and topology contract source
- Result:
  - UPDATED: goal success, gate policy, director rubric, per-turn prompt and
    escalation ladder now require D9 real-procgen hit-rate evidence before broad
    Direction A rollout.
  - UPDATED: accepted-forward packets must state whether they count as progress,
    failure toward escalation or freeze.
  - UPDATED: the no-human-wait default now has only two explicit lky checkpoint
    exceptions: Direction A ROI and ruins freeze.
  - UPDATED: the art-plate plan now routes next closure-oriented work through
    D9 full-rollout / hybrid-first / stop decisioning instead of another source,
    alpha, fog, seam, line-weight or single-cell rectangle pass.
  - NOT RUN: Gradle, golden screenshot, packaged whitebox or resource gates. This
    entry is documentation-only and makes no Kotlin/resource/runtime changes.
- Director verdict:
  - Review feedback is accepted for documentation governance. The current
    topology-risk interior seam dissolve packet remains accepted-forward only,
    not procedural Direction A closure.
- Remaining risk:
  - D9 is a documented gate only; the statistical probe/report implementation
    still needs a future implementation slice.
  - The existing D8 sample is still an 8-row probe and cannot decide Direction A
    ROI by itself.
- Next action:
  - Implement or design the D9 real-procgen distribution probe before any broad
    Direction A rollout, final all-map closure, or additional same-family ruins
    topology-risk polish.

## 2026-06-02 - PR08 Topology-Risk Interior Seam Dissolve V1 Packet

- Loop type: fail-first risky-path composition grammar / focused owner gate /
  D8 generalization recapture
- Target area: topology-risk hybrid internal shared-seam readability for
  L-like and low-fill visible topology
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/topology-risk-interior-seam-dissolve-2026-06-02/pr08-topology-risk-interior-seam-dissolve-decision.md`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/topology-risk-interior-seam-dissolve-2026-06-02/pr08-topology-risk-interior-seam-dissolve-decision.md`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - repeated the same focused command after adding the production seam layer
  - repeated the same focused command after correcting vertical run screen-y
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFallbackTopologySourceBandsSubduedForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache`
- Result:
  - FAIL-FIRST: the focused L-shape test initially failed because no internal
    seam dissolve field existed.
  - FAIL then FIXED: the first production implementation satisfied horizontal
    seam coverage but missed vertical seam coverage because `TileMapViewport`
    inverts map y; the vertical run start now uses the run's maximum y tile.
  - PASS: focused L-shape command reported `BUILD SUCCESSFUL in 6s`.
  - PASS: focused owner gate with renderer/manifest/preload tests,
    `manifestLint`, `resourcePipelineLint` and `maintainabilityLint` reported
    `BUILD SUCCESSFUL in 8s`.
  - PASS: D8 generalization command reported `BUILD SUCCESSFUL in 21s`.
  - PASS: current D8 board hash is
    `695a213343cda62f5e0d872f2dbd21d912903c3e897f57ebe5a7e3f9e96fdeea`.
  - PASS: current D8 evidence-index hash is
    `5379445a0399bc7817eab47b81c63edb4b1cc480fb904971e102a34e7120aaea`.
- Director verdict:
  - Accepted-forward as topology-risk interior seam dissolve V1. It is a
    room-level composition grammar improvement for risky shapes, not a final
    closure or golden rebaseline.
- Remaining risk:
  - PR08 director runtime expected hashes were not updated by this packet.
  - The risky rows still sit below plate-safe authored-room quality.
  - No new source images, visual keys, manifest schema, core/game contracts,
    save/replay/profile schema or content-pack boundaries changed.
- Next action:
  - Continue with a broader topology-risk wall/floor composition grammar or
    dedicated topology-source coverage for non-ruins families. Do not continue
    with small alpha, fog, line-weight or single-cell rectangle tuning.

## 2026-06-02 - PR08 Topology-Risk Source Alpha Scope V1 Packet

- Loop type: fail-first regression lock / source-alpha scope guard /
  director impact recapture
- Target area: PR08 topology-risk source alpha boundary for dedicated versus
  fallback room-art sources
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-scope-2026-06-02/`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-scope-2026-06-02/d8/pr08-topology-source-alpha-scope-d8-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-scope-2026-06-02/d8/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-scope-2026-06-02/director-recapture/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-scope-2026-06-02/pr08-topology-source-alpha-scope-decision.md`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFallbackTopologySourceBandsSubduedForNonRuinsRoomArtFamilies" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFallbackTopologySourceBandsSubduedForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFallbackTopologySourceBandsSubduedForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: fail-first non-ruins fallback command failed as expected because the
    forest-edge fallback source bands rendered at `0.42` instead of
    `0.35..0.37`.
  - PASS: after scoping `RoomArtPlateSource.topologySourceBandAlpha`, focused
    fallback and ruins dedicated source tests reported `BUILD SUCCESSFUL in 6s`.
  - EXPECTED FAIL: PR08 director runtime evidence command wrote current
    artifacts but failed expected-hash assertions. Current hashes are recorded
    in the scope decision packet; no director golden rebaseline was accepted.
  - PASS: D8 generalization command reported `BUILD SUCCESSFUL in 20s`.
  - PASS: focused owner gate with manifest/preload/render tests,
    `manifestLint`, `resourcePipelineLint` and `maintainabilityLint` reported
    `BUILD SUCCESSFUL in 8s`.
  - PASS: alpha-scope D8 board hash is
    `252b492f232fda07d310c639a8a9e519cf305f5cfddd82d393459df5d6c010f4`.
  - PASS: alpha-scope D8 evidence index hash is
    `d067dbe2f43f53709c11f3027759f1abd64061f2d101dbd3df7e839280263029`.
  - PASS: director recapture evidence-index hash is
    `14da736d2bd251f7aae1f95cd139470e6bc6899ffaeaae07f2c263559dace6c0`.
- Director verdict:
  - Accepted-forward as topology-risk source-alpha scope V1. The stronger
    `0.42` band alpha is valid only for formal dedicated topology sources.
    Fallback family room-plate crops stay at `0.36` until those families have
    their own topology-source assets.
- Remaining risk:
  - PR08 director runtime evidence still fails expected-hash assertions and is
    not rebaselined.
  - This does not close final procedural Direction A, non-ruins topology
    source quality, packaged parity refresh or all-map closure.
  - No core/game rule, save/replay/profile schema, manifest schema, runtime
    source asset or public gameplay contract changed.
- Next action:
  - Decide whether the next risky-row improvement should be a stronger
    structural topology source for non-ruins families or a more explicit
    risky-path composition grammar that reduces grid/card rhythm without
    raising fallback alpha.

## 2026-06-02 - PR08 Topology-Risk Source Alpha V1 Packet

- Loop type: fail-first renderer lock / risky-path compositor balance / D8
  runtime evidence recapture
- Target area: PR08 `tileset.ruins` topology-risk source-band readability after
  dedicated topology source D
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/pr08-topology-source-alpha-d8-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/pr08-topology-source-d-vs-alpha-d8-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/pr08-topology-source-d-vs-alpha-d8-diff.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/pr08-topology-source-alpha-decision.md`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `magick montage` for Source D versus source-alpha D8 comparison board
  - `magick compare -metric MAE` for Source D versus source-alpha D8 diff
- Result:
  - PASS: fail-first focused renderer command failed as expected before the
    production change because topology source bands still rendered at
    `alpha=0.36` and did not match the new `0.41..0.43` assertion.
  - PASS: after raising the dedicated topology-source crop-band alpha to
    `0.42`, the focused renderer command reported `BUILD SUCCESSFUL in 6s`.
  - PASS: D8 generalization command reported `BUILD SUCCESSFUL in 20s`.
  - PASS: focused manifest/preload/L-shape renderer,
    `manifestLint`, `resourcePipelineLint` and `maintainabilityLint` command
    reported `BUILD SUCCESSFUL in 7s`.
  - PASS: alpha D8 board hash is
    `252b492f232fda07d310c639a8a9e519cf305f5cfddd82d393459df5d6c010f4`.
  - PASS: alpha D8 evidence index hash is
    `d067dbe2f43f53709c11f3027759f1abd64061f2d101dbd3df7e839280263029`.
  - PASS: Source D versus alpha D8 comparison board hash is
    `9559355a260dddb6b62f8690c5829e69df2bf6d19bb0b975c500f37e8944b741`.
  - PASS: Source D versus alpha D8 diff hash is
    `5b331d9325354c5c3a880cf529b0d93615b05733105d83d85bf29880830421ae`.
  - RECORDED: pixel comparison metric is `MAE 16.0333 (0.000244653)`;
    `magick compare` returned non-zero because the images differ.
- Director verdict:
  - Accepted-forward as topology-risk source-alpha V1. The immediate D
    follow-up is a bounded compositor/source-alpha pass, not another source
    regeneration: risky D8 rows gain a small material-readability lift while
    entities, loot, cursor/selection and telegraph overlays remain above the
    source band.
- Remaining risk:
  - This is not final procedural Direction A closure, all-map closure or
    packaged parity refresh.
  - The D8 risky rows remain accepted-forward only below plate-safe authored
    full-room quality.
  - No core/game rule, save/replay/profile schema, manifest schema, runtime
    source asset or public gameplay contract changed.
- Next action:
  - Use the alpha D8 board and current director runtime evidence to decide
    whether risky rows now need a stronger structural source asset, a more
    explicit risky-path composition grammar, or packaged/director recapture.

## 2026-06-02 - PR08 Dedicated Topology Source D V1 Packet

- Loop type: generated resource quality iteration / C-D mask owner comparison /
  D8 runtime evidence recapture
- Target area: PR08 `tileset.ruins` topology-risk source quality
- Changed files:
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/ui_map_stage_ruins_topology_source_pr08_candidate_d.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-d-mask-owner-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-c-vs-d-mask-owner-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/runtime-d-d8/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-c-vs-d-d8-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-d-decision.md`
- Commands run:
  - built-in image generation for one Source D topology-source atlas candidate
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 topology source mask diagnostic writes owner artifacts" --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `magick montage` for C/D mask owner and C/D D8 comparison boards
- Result:
  - PASS: D candidate and promoted runtime resource hash are
    `5879b51e5c2a3fad2c86316bdbdf2c9364a6d45cf64d4daba45a0700c6f2a906`.
  - PASS: focused mask owner command reported `BUILD SUCCESSFUL in 8s`.
  - PASS: D mask owner board hash is
    `cfb46cf84728209e98f13b2b1a0d587873b37f3141620d86009115026e66571a`.
  - PASS: D mask owner evidence index hash is
    `2412afb89aafe5284e836784f3757450239b519e0dd60e973e88e4998a826e89`.
  - PASS: C/D mask owner comparison board hash is
    `74246ead9ff9a740e10870802fdcd396e2ae7c1fde0b218c2a814fb5503b41ef`.
  - PASS: D8 generalization command reported `BUILD SUCCESSFUL in 20s`.
  - PASS: D8 board hash is
    `5bccb74b686b562d87ac10f499b78df496f123d8cd6520ed6e298b16d8843da4`.
  - PASS: D8 evidence index hash is
    `1d5a7ff6263ac687b3b7eb672e9b6880e2032855231d9b4790157905c930c4aa`.
  - PASS: C/D D8 comparison board hash is
    `99158dec3b7d123cfe4cc6099d47ad63c9d78e513c155f888cfdfbbd3eb7f608`.
  - PASS: focused manifest/preload/L-shape renderer,
    `manifestLint`, `resourcePipelineLint` and `maintainabilityLint` command
    reported `BUILD SUCCESSFUL in 8s`.
- Director verdict:
  - Accepted-forward as dedicated topology source D V1. D is a same-key
    generated topology-source atlas that improves crop-safe material
    distribution over C without changing the resource contract. It is a source
    quality improvement, not final procedural Direction A closure.
- Remaining risk:
  - D reduces but does not eliminate the low-alpha / risky-row gap versus
    plate-safe authored full-room rows.
  - No packaged app recapture or full `verifyChanged` was run for this exact D
    resource swap.
  - No core/game rule, save/replay/profile schema, manifest schema or public
    gameplay contract changed.
- Next action:
  - Decide from the D D8 board and current director runtime evidence whether the
    remaining gap is still source-art driven or should move into a bounded
    risky-path compositor/source-alpha pass.

## 2026-06-02 - PR08 Topology Source Mask Owner Artifact V1 Packet

- Loop type: owner artifact promotion / golden evidence wiring / topology mask
  diagnostic review
- Target area: PR08 `tileset.ruins` topology-risk source mask diagnostics
- Changed files:
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-mask-owner-2026-06-02/`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/pr08-topology-source-c-decision.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/topology-source-mask-owner-2026-06-02/dark-uiux-pr08-topology-source-mask-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-mask-owner-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 topology source mask diagnostic writes owner artifacts" --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 topology source mask diagnostic writes owner artifacts" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: the focused owner artifact command reported
    `BUILD SUCCESSFUL in 4s`.
  - TERMINATED: the combined command was stopped with exit `143` after the new
    mask owner test had already passed and the D8 golden pass stalled in the
    same Gradle invocation. This command is not counted as passed.
  - PASS: the standalone D8 generalization command reported
    `BUILD SUCCESSFUL in 19s`.
  - PASS: the focused manifest/preload/L-shape renderer,
    `manifestLint`, `resourcePipelineLint` and `maintainabilityLint` command
    reported `BUILD SUCCESSFUL in 2s`.
  - PASS: owner mask board hash is
    `8c0fb5155efc49ede5e95d5f9643227ee7525db053c6b97d775ce32a9f6cac96`.
  - PASS: owner mask evidence index hash is
    `a66dfb8283a26403e6242cdefbf5e18e307b4f98b2f4167609148aa511b804a3`.
  - PASS: runtime topology source resource hash remains
    `da6b3b9150e93f78a28d08fb92668a96f228333020326ff4b46a082c4a6c92a2`.
- Director verdict:
  - Accepted-forward as topology source mask owner artifact V1. The diagnostic
    path is no longer only review-only one-off evidence; it now has a
    repeatable `client` golden artifact and repo-relative evidence index.
- Remaining risk:
  - This improves verification quality, not the source art itself. C still
    remains below plate-safe authored-room quality in tall-cross and
    component-like risky rows.
  - The combined golden invocation is not accepted as the validation shape;
    run the mask owner and D8 generalization golden tests as separate Gradle
    invocations.
  - No core/game rule, save/replay/profile schema, manifest schema or public
    gameplay contract changed.
- Next action:
  - Generate or judge source D using the owner mask artifact and D8 board; D
    should reduce the remaining tall-cross / component-row read without losing
    C's brighter corridor-heavy continuity.

## 2026-06-02 - PR08 Dedicated Topology Source C V1 Packet

- Loop type: resource quality iteration / B-C mask diagnostic comparison /
  forced D8 evidence recapture
- Target area: PR08 `tileset.ruins` topology-risk source quality
- Changed files:
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/pr08-topology-source-b-decision.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/ui_map_stage_ruins_topology_source_pr08_candidate_c.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/pr08-topology-source-c-vs-b-crop-mask-diagnostic-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/runtime-c-d8/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/runtime-c-d8/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache --rerun-tasks`
- Result:
  - PASS: the first focused manifest/preload/L-shape renderer, D8 golden,
    `manifestLint`, `resourcePipelineLint` and `maintainabilityLint` command
    reported `BUILD SUCCESSFUL in 22s`.
  - PASS: the forced recapture command reported `BUILD SUCCESSFUL in 44s` with
    `27 actionable tasks: 27 executed`; this command produced the C D8 build
    report artifacts that were copied into `runtime-c-d8`.
  - PASS: candidate C source hash is
    `da6b3b9150e93f78a28d08fb92668a96f228333020326ff4b46a082c4a6c92a2`.
  - PASS: runtime topology source resource hash is
    `da6b3b9150e93f78a28d08fb92668a96f228333020326ff4b46a082c4a6c92a2`.
  - PASS: B/C mask diagnostic board hash is
    `7b2ffdc688278522e6d6f5ef9b397d30b25265088443fdf3e0858f50949dc39e`.
  - PASS: C D8 board hash is
    `78b96c76a768ef892192a8f474c43a6582610e3016c2b803618e43f649604ab9`.
  - PASS: C D8 evidence index hash is
    `eb5440ca776ddd0e825716f09c1bfb00b5eccc2d59c5742616ee566c60e5bddb`.
- Director verdict:
  - Accepted-forward as dedicated topology source C V1. Compared with B, C
    keeps the same formal key and manifest contract but improves the risky
    corridor-heavy and horizontal source-region rows with brighter, more
    continuous wall/floor/light material.
- Remaining risk:
  - C is not final procedural Direction A closure. The D8 risky rows are still
    more component-like and lower-fidelity than plate-safe full-room rows.
  - The B/C mask diagnostic board is review evidence only; it is still not an
    owner/golden artifact.
  - No core/game rule, save/replay/profile schema, manifest schema or public
    gameplay contract changed.
- Next action:
  - Promote the mask/crop diagnostic into a repeatable owner/golden artifact or
    generate a stronger source D that reduces the remaining risky-row
    component feel.

## 2026-06-02 - PR08 Dedicated Topology Source B V1 Packet

- Loop type: resource quality iteration / mask diagnostic comparison / D8
  evidence recapture
- Target area: PR08 `tileset.ruins` topology-risk source quality
- Changed files:
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/ui_map_stage_ruins_topology_source_pr08_candidate_b.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/pr08-topology-source-b-vs-a-crop-mask-diagnostic-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/runtime-b-d8/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/runtime-b-d8/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: the focused manifest/preload/L-shape renderer, D8 golden,
    `manifestLint`, `resourcePipelineLint` and `maintainabilityLint` command
    reported `BUILD SUCCESSFUL in 22s`.
  - PASS: the same command was rerun after spec/log/decision writeback and
    reported `BUILD SUCCESSFUL in 1s` with `27 actionable tasks: 4 executed,
    23 up-to-date`; SDKMAN printed an internet reachability warning but still
    selected `java 21.0.10-tem` and `kotlin 2.2.21`.
  - PASS: candidate B source hash is
    `f3ead79aeeb39f3af73d75352f48a597f92f03baf7f179feacea356a251bad5c`.
  - PASS: runtime topology source resource hash is
    `f3ead79aeeb39f3af73d75352f48a597f92f03baf7f179feacea356a251bad5c`.
  - PASS: A/B mask diagnostic board hash is
    `0ca943c943aacadd270a43f44a3be47fd121a5aaefc7f2f59c259c34f5831ce9`.
  - PASS: B D8 board hash is
    `a2ec17b6111814e247a8117c49293663a08802530eaceeaacc9a21e5a0457295`.
  - PASS: B D8 evidence index hash is
    `f5fe55edd0948e8676e696da30bc3f85d5463b963bc881eec949c35d4477a6f4`.
- Director verdict:
  - Accepted-forward as dedicated topology source B V1. Compared with A, B
    keeps the same formal key and manifest contract but provides stronger
    distributed wall mass, threshold fragments, warm light pockets and readable
    mid-band stone material for L-like, corridor-heavy and tall-cross topology
    crops.
- Remaining risk:
  - B improves the topology-risk source quality, but the runtime D8 risky rows
    are still darker and more component-like than plate-safe full-room rows.
    This is not final procedural Direction A closure.
  - B has more visible small-tile texture than A; it must be watched in later
    alpha/source/golden work so it does not reintroduce tactical-grid first
    read.
  - No new visual key, manifest schema, core/game rule, save/replay/profile
    schema or public gameplay contract changed.
- Next action:
  - Promote the mask/crop diagnostic into a repeatable owner/golden artifact or
    generate a stronger source C that keeps B's distributed structure while
    reducing its small-grid texture.

## 2026-06-02 - PR08 Dedicated Topology Source A V1 Packet

- Loop type: resource promotion / client presentation strategy / focused
  renderer test / D8 evidence recapture
- Target area: PR08 `tileset.ruins` topology-risk source-region path
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/assets/DarkUiMapVisualKeys.kt`
  - `client/src/main/kotlin/com/ktome/client/assets/ClientAssetLoadStrategy.kt`
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `assets-src/image/manifests/phase2-visual-manifest.json`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02/`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02/ui_map_stage_ruins_topology_source_pr08_candidate_a.png`
  - `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02/pr08-topology-source-crop-mask-diagnostic-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache`
- Result:
  - PASS: focused manifest + L-shape renderer command reported
    `BUILD SUCCESSFUL in 7s`.
  - PASS: combined manifest/preload/L-shape/D8/lint command reported
    `BUILD SUCCESSFUL in 20s`; `manifestLint` and
    `resource-pipeline-authority` passed.
  - PASS: after visual inspection found the first source alpha too dim, the
    dedicated source draw alpha was raised from `0.28` to `0.36`; the focused
    L-shape + D8 golden rerun reported `BUILD SUCCESSFUL in 24s`.
  - PASS: candidate source hash is
    `49c4cbebc691c8ddfb04d592b0c8450ff1a3b065a0842b4f12e98968e756f1b8`.
  - PASS: runtime resource hash is
    `49c4cbebc691c8ddfb04d592b0c8450ff1a3b065a0842b4f12e98968e756f1b8`.
  - PASS: review-only mask diagnostic board hash is
    `10c2723f35d909552473ce2d2cd34b1dc02a9978c981eb8c49651bff79e70142`.
  - PASS: D8 board hash after the final alpha recapture is
    `695a213343cda62f5e0d872f2dbd21d912903c3e897f57ebe5a7e3f9e96fdeea`.
  - PASS: D8 evidence index hash is
    `5379445a0399bc7817eab47b81c63edb4b1cc480fb904971e102a34e7120aaea`.
- Director verdict:
  - Accepted-forward as dedicated topology source A V1 for `tileset.ruins`
    topology-risk rooms. The risky path now uses a formal single-image
    topology source key and no longer samples the accepted full-room plate when
    `sourceRegion` bands are needed.
- Remaining risk:
  - The D8 risky rows are more stable and less unsafe than full-room source
    reuse, but still read darker and more bottom-layer than plate-safe authored
    rooms. This does not close final Direction A, non-ruins topology coverage
    or all-map quality.
  - The mask diagnostic board is review evidence only; it is not yet an
    owner/golden artifact or manifest resource.
  - No core/game rule, save/replay/profile schema, manifest schema or public
    gameplay contract changed.
- Next action:
  - Promote the mask/crop diagnostic into a repeatable owner/golden artifact or
    generate a stronger topology source before any further family expansion or
    final all-map claim.

## 2026-06-02 - PR08 Topology-Risk Source-Cropped Room Plate V1 Packet

- Loop type: client presentation strategy / focused renderer test / D8 evidence
  recapture
- Target area: PR08 `tileset.ruins` topology-risk source-region crop contract
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/assets/DarkUiMapVisualKeys.kt`
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache --rerun-tasks`
- Result:
  - FIXED: the first focused command failed because the old assertion was tied
    to the previous local-light anchor instead of a representative horizontal
    source-crop band. The failure output showed two source-cropped room-plate
    bands with correct source regions; the assertion was retargeted to the
    actual horizontal and vertical visible bands.
  - FIXED: the second focused command failed because the vertical representative
    point was outside the displayed source-crop band after viewport y mapping;
    it was corrected to a visible vertical-arm cell.
  - PASS: the focused L-shape source-crop test reported
    `BUILD SUCCESSFUL in 4s`.
  - PASS: the combined art-plate renderer, D8 golden and maintainability
    command reported `BUILD SUCCESSFUL in 19s`.
  - PASS: final forced rerun of the same combined command reported
    `BUILD SUCCESSFUL in 46s` with `24 actionable tasks: 24 executed`.
  - PASS: D8 board was recaptured and copied into the repo-relative review
    packet.
  - PASS: new D8 board hash is
    `7a16be0b3d05070920692ceda49c10549d2ad8b380e34efe157c5630424a1d10`.
  - PASS: new D8 evidence index hash is
    `a83e56fa6f24cd1adb988cb72f70de726577d2c81b2a7a4a37dc21cd9c876365`.
- Director verdict:
  - Accepted-forward as source-cropped room plate V1. Risky shapes now draw the
    accepted room plate through topology-band source regions, so the path has a
    true source crop contract instead of low-alpha row fragments or a
    room-breakup bridge.
- Remaining risk:
  - The source is still the accepted full-room plate, not a dedicated
    topology-fragment source. Risky rows are more cohesive, but they still do
    not match plate-safe authored rooms at director-grade quality.
  - No new production resource, manifest schema, core/game rule or packaged
    whitebox contract was introduced.
- Next action:
  - Build a dedicated topology-aware source asset and mask diagnostic board;
    do not return to alpha/seam/fog tuning or claim final procedural Direction
    A closure from this V1 source-crop path alone.

## 2026-06-02 - PR08 Topology-Risk Material-Breakup Band V1 Packet

- Loop type: client presentation strategy / focused renderer test / D8 evidence
  recapture
- Target area: PR08 `tileset.ruins` topology-risk authored material component
  source
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/assets/DarkUiMapVisualKeys.kt`
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache`
- Result:
  - FIXED: the first focused command failed because the new assertion exposed
    that topology-risk hybrid still kept the old single-bbox
    `tileset.ruins.room_breakup_01` draw at alpha `0.78`; the topology-risk
    warm overlay path now disables that legacy bbox and uses topology bands
    instead.
  - PASS: focused L-shape material-breakup band test reported
    `BUILD SUCCESSFUL in 9s`.
  - PASS: combined art-plate renderer, D8 golden and maintainability command
    reported `BUILD SUCCESSFUL in 20s`.
  - PASS: final forced rerun of the same combined command with `--rerun-tasks`
    reported `BUILD SUCCESSFUL in 46s` with `24 actionable tasks: 24 executed`.
  - PASS: D8 board was recaptured and copied into the repo-relative review
    packet.
  - PASS: new D8 board hash is
    `825e6d474d29b4200dee2e786ccc94ccd4b05688321b454b543f4a48ebf2f06f`.
- Director verdict:
  - Accepted-forward as material-breakup band V1. Risky shapes now consume the
    existing formal `tileset.ruins.room_breakup_01` authored decal through
    visible-topology bands, while the old unsafe single-bbox decal path is
    disabled for topology-risk hybrid rooms.
- Remaining risk:
  - This is still a bridge technique. The decal was originally authored as a
    room-scale floor breakup source, not as a dedicated topology-fragment atlas
    or mask-crop source.
  - No new production resource, manifest schema, core/game rule or packaged
    whitebox contract was introduced.
- Next action:
  - Replace the bridge technique with a dedicated topology-aware
    mask/crop/source before any final procedural Direction A or all-map claim.

## 2026-06-02 - PR08 Topology-Risk Plate-Fragment Decomposition V1 Packet

- Loop type: client presentation strategy / focused renderer test / D8 evidence
  recapture
- Target area: PR08 `tileset.ruins` topology-risk plate-derived material
  decomposition
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: focused L-shape plate-fragment test reported
    `BUILD SUCCESSFUL in 8s`.
  - PASS: combined art-plate renderer, D8 golden and maintainability command
    reported `BUILD SUCCESSFUL in 21s`.
  - PASS: an initial 0.185 fragment alpha board was inspected and rejected as
    too stripe-forward; the accepted V1 uses alpha `0.125`.
  - PASS: D8 board was recaptured and copied into the repo-relative review
    packet.
  - PASS: new D8 board hash is
    `f2bf303038f106a01918825d0ad477dd335f273847b54a9b8f08d8c751de4d04`.
- Director verdict:
  - Accepted-forward as plate-fragment decomposition V1. Risky shapes now use
    low-alpha fragments of the accepted room plate inside real visible row
    runs, so they gain resource-derived material cohesion without returning to
    one unsafe full-room bbox stretch.
- Remaining risk:
  - Fragment rows are intentionally restrained, but they still cannot match a
    dedicated authored topology-aware source. Further alpha or draw-order
    tuning should stop here unless paired with a better source/crop model.
  - No production resource, manifest schema, core/game rule or packaged
    whitebox contract was changed.
- Next action:
  - Create or route richer authored component resources / dedicated mask-crop
    source for topology-risk rooms before any more family expansion or final
    all-map claim.

## 2026-06-02 - PR08 Topology-Risk AO Local-Light V1 Packet

- Loop type: client presentation strategy / focused renderer test / D8 evidence
  recapture
- Target area: PR08 `tileset.ruins` topology-risk fallback ambient depth and
  local-light quality
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: focused L-shape AO/local-light test reported
    `BUILD SUCCESSFUL in 8s`.
  - PASS: combined art-plate renderer, D8 golden and maintainability command
    reported `BUILD SUCCESSFUL in 20s`.
  - PASS: D8 board was recaptured and copied into the repo-relative review
    packet.
  - PASS: new D8 board hash is
    `cef029c1e754d7978fa9fb13ba6c810c647abf34edec0e78bbd89096ed3fe4d3`.
- Director verdict:
  - Accepted-forward as topology-risk AO/local-light V1. Risky shapes now gain
    visible-run AO, side pressure and deterministic warm light pools, so complex
    rows read less like flat component stitching and more like authored dungeon
    space while still avoiding unsafe full-room plate stretching.
- Remaining risk:
  - The risky rows still expose tactical-grid rhythm and do not match the
    authored full-room plate's integrated wall/light/floor cohesion.
  - No production resource, manifest schema, core/game rule or packaged
    whitebox contract was changed.
- Next action:
  - Move to topology masking, deeper plate decomposition or richer authored
    component resources before any more family expansion or final all-map claim.

## 2026-06-02 - PR08 Topology-Risk Decomposition V1 Packet

- Loop type: client presentation strategy / focused renderer test / D8 evidence
  recapture
- Target area: PR08 `tileset.ruins` topology-risk fallback resource-backed
  room construction
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache`
- Result:
  - FIXED: the first focused command initially failed at `:client:compileKotlin`
    because the edited `RoomArtPlatePresentation.kt` file was missing its
    package declaration; the declaration was restored and the same focused
    command then reported `BUILD SUCCESSFUL in 13s`.
  - PASS: combined art-plate renderer, D8 golden and maintainability command
    reported `BUILD SUCCESSFUL in 19s`.
  - PASS: D8 board was recaptured and copied into the repo-relative review
    packet.
  - PASS: new D8 board hash is
    `737544fb1df4b63d8bff85a206f92fadb016722673d637b9edfdcff4a9563426`.
- Director verdict:
  - Accepted-forward as topology-risk decomposition V1. Risky shapes now reuse
    canonical wall-family resources through `resolveTerrainWallPiece`, drawing
    crown, side and door-contact components over actual topology instead of
    relying only on rectangle edge marks.
- Remaining risk:
  - The risky rows still do not reach the authored full-room plate quality:
    they need stronger room-scale AO, local-light pools or a deeper
    mask/decomposition resource route before procedural Direction A closure.
  - No production resource, manifest schema, core/game rule or packaged
    whitebox contract was changed.
- Next action:
  - Add topology-risk AO/local-light or richer authored component resource
    support before any more family expansion or final all-map claim.

## 2026-06-02 - PR08 Topology-Risk Hybrid V1 Packet

- Loop type: client presentation strategy / focused renderer test / D8 evidence
  recapture
- Target area: PR08 `tileset.ruins` topology-risk fallback visual quality
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: focused L-shape hybrid test reported `BUILD SUCCESSFUL in 9s`.
  - PASS: combined art-plate renderer, D8 golden and maintainability command
    reported `BUILD SUCCESSFUL in 20s`.
  - PASS: D8 board was recaptured and copied into the repo-relative review
    packet.
  - PASS: new D8 board hash is
    `53b578391af745f4f84a338a9c40bab89ffd4682d5bfbd6a943c7546bdbd3acc`.
- Director verdict:
  - Accepted-forward as topology-risk hybrid V1. Plate-safe rooms still use the
    authored full-room plate. Topology-risk rooms no longer plain-fallback to
    unenhanced runtime tiles: they keep runtime tile material visible and add
    topology-following material fields plus true-boundary dark edge marks.
- Remaining risk:
  - Hybrid V1 improves honesty and first-read, but complex rooms still lack
    full authored wall-crown, door-mouth, local-light and material source
    quality. It is not final director-grade procedural topology closure.
  - No production resource, manifest schema, core/game rule or packaged
    whitebox contract was changed.
- Next action:
  - Upgrade the topology-risk path through mask/decomposition or componentized
    room resources before generating more aesthetic candidates or claiming
    broader Direction A closure.

## 2026-06-02 - PR08 Topology Contract V1 Fallback Packet

- Loop type: client presentation contract / focused renderer test / D8
  evidence recapture
- Target area: PR08 `tileset.ruins` full-room art plate topology safety
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: focused L-shape fallback test reported `BUILD SUCCESSFUL in 8s`.
  - PASS: combined art-plate renderer, D8 golden and maintainability command
    reported `BUILD SUCCESSFUL in 19s`.
  - PASS: D8 board was recaptured and copied into the repo-relative review
    packet.
  - PASS: V1 fallback board hash was
    `27ce1e676391375e09d84199e6d15edfb9a6b07cc9ea4dc2503aa95a498d602a`;
    this shared evidence path is superseded by the later topology-risk hybrid
    V1 recapture above.
- Director verdict:
  - Accepted-forward as topology contract V1. Plate-safe regions preserve the
    authored full-room room plate; low-fill / extreme-aspect topology-risk
    regions now fall back instead of stretching a baked full-room composition
    into mismatched procedural shape.
- Remaining risk:
  - The fallback is visually honest but lower quality than the authored-room
    rows, so this is not final director-grade topology closure.
  - No production resource, manifest schema, core/game rule or packaged
    whitebox contract was changed.
- Next action:
  - Build a topology-risk hybrid path: mask/decompose the plate or introduce
    topology-following wall crown, side/corner/door-contact, room-scale AO,
    material fields and local light components.

## 2026-06-02 - PR08 D8 Random-Seed Generalization Evidence Packet

- Loop type: focused golden evidence / director generalization review /
  topology-contract decision
- Target area: `tileset.ruins` random-seed / multi-room-shape Direction A
  generalization gate
- Changed files:
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md`
  - `client/build/reports/golden/dark-uiux-pr08-generalization/dark-uiux-pr08-generalization-board.png`
  - `client/build/reports/golden/dark-uiux-pr08-generalization/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 generalization evidence labels remain registered" maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: first command reported `BUILD SUCCESSFUL in 25s`.
  - PASS: second command reported `BUILD SUCCESSFUL in 3s`.
  - PASS: generated 8 `tileset.ruins` map-stage crops and one 4x2 review
    board under `client/build/reports/golden/dark-uiux-pr08-generalization/`.
  - PASS: copied the same board, crops and `evidence-index.tsv` into the
    repo-relative PR08 review packet directory.
  - PASS: board hash is
    `ec5ea2a0085692fc2cb1a2e7af995ee750de8a2782ad0f201498183db79cf4e3`.
  - PASS: topology signatures cover varied visible material bounds, including
    `13x9`, `14x13`, `16x15`, `9x16`, varied fill ratios and player offsets.
  - Warnings were limited to existing deprecated `getFrameBufferPixmap` call
    sites in the golden harness.
- Director verdict:
  - `accepted-with-topology-contract`. Direction A remains the correct
    authored-room visual direction, but the current implementation cannot be
    treated as generally closed for procedural ruins rooms. Rectangular and
    near-rectangular crops remain strong; L-like, corridor-heavy and high-narrow
    crops expose baked wall/light composition mismatch against actual visible
    topology.
- Remaining risk:
  - This is a static D8 crop board, not a dynamic visibility-swimming video.
  - No production resource, manifest or canonical PR08 director baseline was
    changed.
  - The current evidence is enough to reject `accepted-generalizes`, but it
    does not yet choose between mask/decomposition, plate-safe fallback or a
    hybrid topology-following component route.
- Next action:
  - Implement a topology-aware room presentation contract that classifies
    plate-safe versus topology-risky visible regions. Use that contract to pick
    between full-plate draw, topology mask/decomposition, or hybrid fallback
    before generating new aesthetic candidates or claiming all-map closure.

## 2026-06-02 - PR08 Random-Seed Generalization Gate Review

- Loop type: document adjustment / director review absorption /
  procedural-generalization gate
- Target area: Direction A fixed full-room plate closure criteria
- Changed files:
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - No new screenshot. This is a document-only review absorption pass based on
    current implementation and prior PR08 fixed-crop evidence.
- Commands run:
  - read-only `rg` / `sed` inspection of the pasted review feedback, PR08 plan,
    goal, log, `RoomArtPlatePresentation.kt` and `DarkUiMapVisualKeys.kt`
- Result:
  - UPDATED: Direction A now has an explicit procedural-room generalization
    blocker. Current implementation draws one family-level room-art plate into
    the axis-aligned bounds of visible map materials; this is proof-slice
    evidence, not arbitrary procgen-room evidence.
  - UPDATED: added Gate D8 requiring `8-12` random-seed / varied-room-shape
    `tileset.ruins` map-stage crops before any broader Direction A, family or
    all-map closure claim.
  - UPDATED: post-2026-06-02 map-stage packets must include
    `why-not-fixed-crop-overfit`, and family close points need bounded
    observable stop lines rather than more same-technique polish.
- Director verdict:
  - The feedback is accepted as a P0 documentation and planning correction.
    Current fixed-crop quality progress is real, but it cannot prove Direction
    A is production-safe for procedural maps until the random-seed board shows
    whether bbox stretch, composition mismatch or visibility swimming breaks
    the route.
- Remaining risk:
  - No new harness or screenshots were created in this document-only pass.
  - The next implementation slice must materialize the random-seed board or
    explicitly document why the project pivots to a topology-following hybrid
    route first.
- Next action:
  - Build or use a whitebox path that captures the Gate D8 ruins random-seed
    board, then decide whether Direction A remains fixed full-room plate,
    needs topology mask / deterministic UV work, pivots to hybrid
    topology-following components, or is rejected for procgen maps.

## 2026-06-02 - PR08 Non-Ruins Packaged Summary Startup Surface

- Loop type: packaged validation evidence closure / input-routing bypass /
  whitebox contract update
- Target area: `dark-uiux-pr08-director-mine-map-stage` and
  `dark-uiux-pr08-director-shadow-depths-map-stage` evidence-summary capture
- Changed files:
  - `game/src/main/kotlin/com/ktome/game/validation/ValidationSessionOptions.kt`
  - `game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt`
  - `client/src/main/kotlin/com/ktome/client/screen/ValidationScenarioBootstrap.kt`
  - `client/src/main/kotlin/com/ktome/client/GameApp.kt`
  - `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
  - `client/src/main/kotlin/com/ktome/client/input/ValidationCommandSource.kt`
  - `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt`
  - `client/src/test/kotlin/com/ktome/client/screen/ValidationScenarioBootstrapTest.kt`
  - `client/src/test/kotlin/com/ktome/client/GameAppLifecycleTest.kt`
  - `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt`
  - PR08 manual, decision, evidence-index, plan, goal and log documents
- Visual evidence:
  - `build/whitebox/dark-uiux-pr08-director-mine-map-stage/evidence/dark-uiux-pr08-director-mine-evidence-summary.png`
  - `build/whitebox/dark-uiux-pr08-director-mine-map-stage/evidence/dark-uiux-pr08-director-mine-map-stage-app.log`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-evidence-summary.png`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-map-stage-app.log`
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.screen.ValidationScenarioBootstrapTest --tests "com.ktome.client.GameAppLifecycleTest.validation startup surface opens evidence summary through typed scenario action" :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest maintainabilityLint --no-configuration-cache`
  - repeated the same command after wiring validation overlay initial mode
  - `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-mine-map-stage --no-configuration-cache`
  - `./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache`
  - `KTOME_VALIDATION_STARTUP_SURFACE=evidence-summary build/whitebox/dark-uiux-pr08-director-mine-map-stage/launch-packaged-app.sh`
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --pid 59524 --raw --out build/whitebox/dark-uiux-pr08-director-mine-map-stage/evidence/dark-uiux-pr08-director-mine-evidence-summary.png`
  - `KTOME_VALIDATION_STARTUP_SURFACE=evidence-summary build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/launch-packaged-app.sh`
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --pid 61922 --raw --out build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-evidence-summary.png`
- Result:
  - PASS: final focused test/lint run reported `BUILD SUCCESSFUL in 17s`;
    warnings were limited to existing deprecated `getFrameBufferPixmap` call
    sites.
  - PASS: after a whitespace-only Kotlin continuation cleanup and the
    no-startup-surface MAP-mode regression test were added, the same focused
    test/lint command plus that extra `GameAppLifecycleTest` filter was rerun
    and reported `BUILD SUCCESSFUL in 7s`, 26 actionable tasks, 3 executed and
    23 up-to-date.
  - PASS: mine packaged materialization reported `BUILD SUCCESSFUL in 7s`.
  - PASS: shadow-depths packaged materialization reported
    `BUILD SUCCESSFUL in 884ms`.
  - PASS: mine evidence-summary screenshot hash is
    `7fa559037e2199507c8e08b915125336e81e87547598bf8a2e25bcf21fcb56f4`.
  - PASS: shadow-depths evidence-summary screenshot hash is
    `ba1c14950b967ebe0f6ae893d7aea16d7580a92bc258b02d10e15083464f2e68`.
  - PASS: mine and shadow-depths app logs both contain
    `evidence_summary_opened`.
  - PASS: no K-ToME packaged app process remained after capture cleanup.
- Director verdict:
  - Accepted as evidence-surface closure only. The startup surface is
    constrained to `evidence-summary`, requires scenario evidence, routes
    through the existing typed `SHOW_EVIDENCE_SUMMARY` action, and starts the
    overlay in validation mode. It closes the local input-routing gap without
    creating arbitrary action injection or a second validation authority.
- Remaining risk:
  - This does not change map visual quality, all-map coverage, or
    shadow-depths topology risk.
  - The proof remains limited to representative forest-edge, mine and
    shadow-depths scenario crops at the `1280x800` whitebox target.
  - This pass did not run `verifyChanged`; it ran the targeted startup-surface,
    scenario, whitebox CLI, maintainability and packaged materialization gates
    listed above.
- Next action:
  - Treat packaged summary evidence as closed and move the PR08 decision loop
    to broader non-ruins topology coverage or the next highest-return
    director-grade first-screen blocker. Do not claim all-map closure yet.

## 2026-06-02 - PR08 Shadow-Depths V4 Right-Mass Packaged Recapture

- Loop type: resource composition pass / fixed-crop golden rebaseline /
  packaged whitebox recapture
- Target area: `tileset.shadow_depths` room-art plate under
  `grey_gate_depths`, `templar`, seed `20260604`
- Changed files:
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_shadow_depths_room_plate_pr08_demo.png`
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
  - `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`
  - `game/src/main/resources/i18n/en-US.json`
  - `game/src/main/resources/i18n/zh-CN.json`
  - `UI/manual-records/dark-uiux-pr08-non-ruins-packaged-parity.md`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/`
  - PR-08 goal / plan / log documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/ui_map_stage_shadow_depths_room_plate_pr08_demo_v4-imagegen.png`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/shadow-depths-v3-v4-runtime-crop-board.png`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/shadow-depths-v4-runtime-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/shadow-depths-v4-telegraph-combat-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-parity-1280x800.png`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-map-stage-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-right-panel-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-bottom-deck-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/packaged-non-ruins-family-map-stage-crop-board-v4-shadow.png`
- Commands run:
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" maintainabilityLint --no-configuration-cache`
  - repeated the same command after the bounded golden hash rebaseline
  - `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache`
  - `./gradlew localeLint :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:packageMacApp preparePhase4V4Whitebox maintainabilityLint -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/launch-packaged-app.sh`
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --pid 60609 --raw --out build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-parity-1280x800.png`
  - `magick` fixed-ratio crop derivation for shadow-depths map/right/bottom guard crops and review boards
  - global CGEvent, pid-directed CGEvent and System Events input probes for `F9, Right, Enter`
- Result:
  - PASS: V4 source and runtime resource hash is
    `c61f5f7c6f9924acbae795a606b28b7ec697e463ea31a57d120bdf70421a2c46`.
  - FAIL-FIRST: the first validation command failed only because
    `GoldenScreenshotHarnessTest` still expected the V3 shadow-depths and
    old telegraph/combat hashes.
  - PASS: after visual review, only
    `dark-uiux-pr08-director-shadow-depths-map-stage-crop` was rebaselined to
    `674afbdba5bb9e4f8618c01fd7ea75590439bd5c400fb2aa000542fd02886eb5`
    and `dark-uiux-pr08-director-telegraph-combat-crop` was rebaselined to
    `2e13535fea38cef3e7a624d3659685e756588d7df3ae4907e2dd7ab03ac51b57`;
    the repeated validation command passed.
  - PASS: `localeLint`, `ValidationScenarioRegistryTest`,
    `:client:packageMacApp`, shadow-depths `preparePhase4V4Whitebox` and
    `maintainabilityLint` passed after V4 wording replaced the stale V3
    whitebox text.
  - PASS: final packaged shadow-depths hashes are full
    `9ed6b4a0fc75cd67a847569fb6e937b4fe538085878afa745277b45e9ef17a62`,
    map-stage
    `d9e57405a62fc1206a8a90706b19cf660c62ca0349b6491f7ab2d89fa03b0d15`,
    right-panel
    `9806e9dd02ae99bc7564801bd707084f203f7dd449681756c5298bff847437d8`,
    and bottom-deck
    `1e7bbeaf6879884beff09119b6bca4a40c19dc06353955d9ea2ba767b052f396`.
  - AT-TIME PENDING: summary input still did not route; no
    `show-evidence-summary` runtime log event was produced after global
    CGEvent, pid-directed CGEvent or System Events retries. This evidence gap
    is closed by the later packaged summary startup surface entry above.
  - PASS: no K-ToME packaged app process was left running after capture.
- Director verdict:
  - Accepted-forward only. V4 fixes the immediate shadow-depths packaged
    blocker by adding right-side room mass and a broken slab lane, so the
    fixed crop no longer reads as an empty tactical-grid right half. This is a
    resource-family composition improvement, not all-map or director-grade
    final closure.
- Remaining risk:
  - At the time of this V4 recapture, mine and shadow-depths evidence-summary
    screenshots were not captured because the local input route still failed
    for those transient packaged bundles; the later startup-surface entry above
    closes that evidence gap.
  - Shadow-depths remains narrower and more grid-readable than forest/mine in
    the lower-right play lane, so broader topology coverage is still required
    before final non-ruins acceptance.
  - This pass did not rerun `verifyChanged`; it ran the targeted resource,
    locale, scenario, renderer, golden and packaged whitebox gates listed
    above.
- Next action:
  - Repair or bypass packaged validation summary input for mine/shadow, then
    broaden non-ruins topology evidence before any all-map/director closure
    claim.

## 2026-06-02 - PR08 Non-Ruins Packaged Scenario Wiring

- Loop type: validation wiring / packaged materialization smoke / anti-bloat
  cleanup
- Target area: packaged parity setup for `tileset.forest_edge`,
  `tileset.mine` and `tileset.shadow_depths` room-art families
- Changed files:
  - `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`
  - `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt`
  - `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioMaterializationCatalog.kt`
  - `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml`
  - `game/src/main/resources/i18n/en-US.json`
  - `game/src/main/resources/i18n/zh-CN.json`
  - `game/src/test/kotlin/com/ktome/game/validation/ValidationScenarioRegistryTest.kt`
  - `client/src/test/kotlin/com/ktome/client/input/ValidationCommandSourceTest.kt`
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
  - `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt`
  - `UI/manual-records/dark-uiux-pr08-non-ruins-packaged-parity.md`
- Visual evidence:
  - `build/whitebox/dark-uiux-pr08-director-forest-map-stage/cua-runbook.md`
  - `build/whitebox/dark-uiux-pr08-director-mine-map-stage/cua-runbook.md`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/cua-runbook.md`
  - `build/whitebox/dark-uiux-pr08-director-forest-map-stage/expected-evidence.json`
  - `build/whitebox/dark-uiux-pr08-director-mine-map-stage/expected-evidence.json`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/expected-evidence.json`
- Commands run:
  - `./gradlew :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:test --tests com.ktome.client.input.ValidationCommandSourceTest :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest --no-configuration-cache`
  - `./gradlew localeLint assetLint styleLint manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --no-configuration-cache`
  - `./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-forest-map-stage --no-configuration-cache`
  - `./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-mine-map-stage --no-configuration-cache`
  - `./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache`
- Result:
  - PASS: focused registry, presentation and whitebox CLI tests completed.
  - PASS: locale/resource/manifest/maintainability lint command completed
    after adding explicit room-plate locale labels and static summary-note keys.
  - PASS: PR08 director golden evidence remains stable after deriving family
    crop runtime parameters and labels from the new scenario registry.
  - PASS: all three sibling scenarios materialized `launch-packaged-app.sh`,
    `cua-runbook.md`, `expected-evidence.json`, app hash and runtime-home
    scaffolding under `build/whitebox/...`.
- Director verdict:
  - Accepted-forward as packaged parity scaffolding. The three non-ruins
    family crops now have first-class packaged validation scenario ids,
    runbooks and expected-evidence contracts instead of relying only on the
    golden harness.
  - This does not yet prove packaged visual parity because no packaged app
    window captures or fixed-crop review have been recorded for the three
    scenarios.
- Remaining risk:
  - The generated runbooks name crop artifacts, but the actual packaged window
    captures and derived crops are still pending.
  - This validates scenario materialization, not final all-map visual closure.
- Next action:
  - Launch the three packaged scenarios, capture full windows and map-stage
    crops, update `UI/manual-records/dark-uiux-pr08-non-ruins-packaged-parity.md`,
    then review whether forest, mine or shadow-depths still blocks closure.

## 2026-06-02 - PR08 Shadow-Depths V3 Crop-Fit Resource

- Loop type: resource iteration / fixed-crop A/B / golden lock
- Target area: `tileset.shadow_depths` room-art plate under
  `grey_gate_depths`, `templar`, seed `20260604`
- Changed files:
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_shadow_depths_room_plate_pr08_demo.png`
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/non-ruins-room-art-family-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/evidence-index.tsv`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/ui_map_stage_shadow_depths_room_plate_pr08_demo_v2.png`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/ui_map_stage_shadow_depths_room_plate_pr08_demo_v3.png`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/shadow-depths-v1-v2-v3-runtime-crop-board.png`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/shadow-depths-v3-runtime-crop.png`
- Commands run:
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - repeated once after replacing the V2 wide candidate with the V3 tall
    crop-fit candidate
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: both targeted PR08 director golden runs completed.
  - V2 reduced the initial circular arena read but still left the lower-right
    interaction field too dominant.
  - V3 uses a tall crop-fit source and is now the current runtime resource for
    `ui.map_stage.shadow_depths.room_plate.pr08_demo`.
  - The PR08 director evidence test now locks the current evidence hashes
    instead of accepting any 64-character hash.
  - The unsupported tileset renderer test no longer relies on a raw
    `.room_plate.pr08_demo` suffix.
  - PASS: final resource/manifest/focused client/golden/maintainability command
    completed with `BUILD SUCCESSFUL in 20s`, 29 actionable tasks, 10 executed
    and 19 up-to-date.
- Director verdict:
  - Accepted-forward. V3 better matches the fixed shadow-depths runtime crop:
    top/left wall mass carries the room identity, while the center/lower-right
    surface is quieter under runtime overlays than V1/V2.
  - This still does not close packaged parity, all-map quality, or broader
    shadow-depths topology coverage.
- Remaining risk:
  - No packaged whitebox recapture was run for forest/mine/shadow-depths.
  - Runtime overlays still impose a visible lower-right tactical field; further
    shadow-depths work should be evidence-triggered, not another blind art
    generation loop.
- Next action:
  - Add sibling packaged validation scenarios for forest, mine and
    shadow-depths map-stage parity, then decide from packaged crops whether any
    family still blocks all-map closure.

## 2026-06-02 - PR08 Non-Ruins Room-Art Family Proof

- Loop type: family rollout / resource promotion / focused runtime proof
- Target area: `tileset.forest_edge`, `tileset.mine` and
  `tileset.shadow_depths` room-art plate families
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/assets/DarkUiMapVisualKeys.kt`
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/main/kotlin/com/ktome/client/assets/ClientAssetLoadStrategy.kt`
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `assets-src/image/manifests/phase2-visual-manifest.json`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_forest_edge_room_plate_pr08_demo.png`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_mine_room_plate_pr08_demo.png`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_shadow_depths_room_plate_pr08_demo.png`
  - `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/non-ruins-room-art-family-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/evidence-index.tsv`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/non-ruins-room-art-family-board.png`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/non-ruins-family-runtime-crop-board.png`
  - `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-forest-map-stage-crop.png`
  - `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-mine-map-stage-crop.png`
  - `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-shadow-depths-map-stage-crop.png`
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: focused client manifest/preload and renderer tests passed.
  - PASS: PR08 director golden evidence now writes distinct ruins, forest,
    mine and shadow-depths map-stage crops.
  - PASS: `assetLint`, `styleLint`, `manifestLint`, `resourcePipelineLint`,
    focused client tests and `maintainabilityLint` passed; `manifestLint`
    reported `entries=644`.
- Director verdict:
  - Accepted-forward as the first non-ruins family rollout proof. The
    implementation now resolves and preloads each accepted family through its
    own plate key rather than leaking the ruins plate across maps.
  - Forest-edge and mine are strong enough to keep as the next family-route
    foundation. Shadow-depths is proof-forward only and needs another art or
    composition pass before final closure.
- Remaining risk:
  - No packaged whitebox recapture was run for the three non-ruins crops.
  - This proves representative family routes, not all room topologies.
  - All-map closure remains open.
- Next action:
  - Strengthen shadow-depths and run packaged parity for the three-family
    evidence set before claiming non-ruins or all-map closure.

## 2026-06-02 - PR08 Final Owner Gate And Golden Rebaseline

- Loop type: implementation cleanup / fail-first renderer contract fix /
  golden rebaseline / full owner gate
- Target area: accepted `tileset.ruins` room-art proof slice and PR08 director
  closure ladder
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
  - PR08 plan / log / runtime parity documents
- Visual evidence:
  - `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-parity-1672x941.png`
  - `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-map-stage-crop.png`
  - `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-right-panel-crop.png`
  - `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-telegraph-combat-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-telegraph-combat-crop.png`
- Commands run:
  - `./gradlew acceptanceContractLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint assetLint styleLint manifestLint resourcePipelineLint :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke :client:goldenScreenshot maintainabilityLint --no-configuration-cache`
  - focused reruns for the five failing `TileRendererCanvasTest` art-plate
    assertions after the renderer cleanup
  - conflict-checked mechanical rebaseline of hard-coded golden hashes from the
    failing `:client:goldenScreenshot` JUnit XML
  - `./gradlew :client:goldenScreenshot --no-configuration-cache`
  - repeated full owner gate command after rebaseline
  - `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-grade verifyChanged --no-configuration-cache`
- Result:
  - FAIL-FIRST: the first owner gate exposed five renderer/test contract
    failures after removing duplicated floor-material authority from the room
    art path. The failing assertions covered per-cell aggregate grain,
    fracture overlays, chipped clusters, hairline etches and high-alpha
    full-cell wash counting.
  - FIXED: `drawFloorMaterial` no longer paints per-cell floor micro-detail
    as a renderer-owned second floor-art authority. The fallback floor material
    keeps only a very low-alpha base plus occlusion strips for non-art paths.
  - FIXED: the full-cell wash assertion now counts only map terrain/material/
    room-compositor layers, so shell and frontstage chrome rectangles are not
    misclassified as map floor washes.
  - PASS: the five focused renderer assertions passed after the cleanup.
  - REBASELINE: the golden mismatch pass produced 13 failures; the mechanical
    update replaced 73 expected hash literals after a conflict check reported
    `failures=13`, `replacements=73`, `conflicts=0`, `missing=0`.
  - PASS: `./gradlew :client:goldenScreenshot --no-configuration-cache`
    completed after the rebaseline.
  - PASS: the full owner gate command completed after rebaseline. `clientSmoke`
    reported its scenario tasks as skipped/up-to-date under the aggregate gate,
    and the Gradle build passed.
  - PASS: `:client:packageMacApp`, `preparePhase4V4Whitebox` and
    `verifyChanged` completed in the final gate command; `verifyChanged`
    reported `BUILD SUCCESSFUL`, 59 actionable tasks, 30 executed and
    29 up-to-date.
- Director verdict:
  - Accepted for reproducible `tileset.ruins` proof-slice closure only. The
    accepted room-art path now has passing golden evidence, packaged whitebox
    preparation and changed-impact verification, while the floor material
    fallback no longer competes with the authored room plate.
- Remaining risk:
  - This does not close non-ruins room art families or all-map visual closure.
  - Exact `1672x941` packaged full-window proof remains display-limited by the
    previously recorded CoreGraphics bounds.
  - The worktree already contained broad PR08 asset/document changes; this gate
    validates the current dirty-tree state rather than isolating a minimal
    patch set.
- Next action:
  - Move to the next director-grade iteration from
    `dark-uiux-pr08-director-grade-iteration-goal.md`: broaden room art
    families or only address a new fixed-crop blocker with higher return than
    further small shell tweaks.

## 2026-05-27 - PR-08 Goal Loop Initialized

- Loop type: documentation / goal setup
- Target area: PR-08 post-exploration execution protocol
- Changed files:
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/targets/dark-uiux-director-grade-target-family-floor-32px.png`
  - `UI/targets/dark-uiux-director-grade-target-family-wall-32px.png`
  - `UI/targets/dark-uiux-director-grade-target-family-floor-wall-repeat.png`
  - `UI/targets/dark-uiux-director-grade-target-family-map-compositor.png`
  - `UI/targets/dark-uiux-director-grade-target-family-ui-chrome.png`
- Commands run:
  - `sed` / `find` / `git status --short` for document and workspace inspection
- Result:
  - Goal execution document and log scaffold created.
- Director verdict:
  - PR-08 direction remains `resource-family-first reset`.
  - This entry does not authorize runtime, manifest, resource or golden mutation.
- Remaining risk:
  - Owner/supersession wiring has not been implemented.
  - Final runtime resources have not been produced.
  - No Gradle validation was run in this setup entry.
- Next action:
  - Start Slice 1: add explicit PR-08 owner coverage and rollback rows for floor/wall before any resource migration.

## 2026-05-27 - PR-08 Floor/Wall Owner Supersession Wired

- Loop type: resource candidate / owner contract setup
- Target area: PR-08 floor/wall owner coverage before runtime resource mutation
- Changed files:
  - `UI/sprite-sheets/key-registry.yaml`
  - `UI/sprite-sheets/owner-contracts/pr02-2-owner-keys.yaml`
  - `UI/sprite-sheets/owner-contracts/pr08-owner-keys.yaml`
  - `UI/sprite-sheets/dark-v1-final-full-inventory.json`
  - `build.gradle.kts`
  - `tools/build.gradle.kts`
  - `tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt`
  - `tools/src/test/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzerTest.kt`
  - `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt`
  - `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
  - `UI/pr/README.md`
  - `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md`
- Visual evidence:
  - `UI/UI-demo-new.png`
  - `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`
  - `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`
  - `UI/targets/dark-uiux-director-grade-target-family-floor-wall-repeat.png`
  - `UI/targets/dark-uiux-director-grade-target-family-map-compositor.png`
  - `UI/targets/dark-uiux-director-grade-target-family-ui-chrome.png`
- Commands run:
  - `sips -g pixelWidth -g pixelHeight ...` for reference/current/target image dimensions
  - `python3 scripts/generate_dark_final_full_inventory.py`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr02_2OwnerScope darkManifestCoveragePr08OwnerScope`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest :tools:test --tests com.ktome.tools.verification.VerificationImpactAnalyzerTest --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest maintainabilityLint`
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint`
  - `./scripts/verify-bootstrap.sh`
  - `./gradlew prepareVerifyChangedPlan`
  - `git diff --check`
- Result:
  - PASS: `darkManifestCoveragePr02_2OwnerScope` now covers only `actor.vanguard` and `prop.stairs.down`.
  - PASS: `darkManifestCoveragePr08OwnerScope` covers `tileset.ruins.ground_01` and `tileset.ruins.wall_01` with no missing, pending or old-style keys.
  - PASS: focused client/tools tests, `maintainabilityLint`, resource authority lint, bootstrap verification, `prepareVerifyChangedPlan`, and whitespace diff check.
  - NOT RUN: full `verifyChanged`; this turn is Slice 1 owner wiring, not PR-08 closure.
- Director verdict:
  - Accepted for Slice 1. PR-08 owner coverage is now explicit enough to begin floor/wall runtime candidate work.
  - This entry does not approve golden updates, packaged whitebox closure, actor/prop reroll, manifest schema change or renderer compositor reset.
- Remaining risk:
  - Runtime floor/wall assets are still the old PR-02-2 images under PR-08 ownership until Slice 2 produces and reviews final candidates.
  - Right panel and bottom deck cohesion are still open for later slices.
  - Packaged app whitebox remains deferred to closure.
- Next action:
  - Start Slice 2: produce final `tileset.ruins.ground_01` and `tileset.ruins.wall_01` candidates from the accepted family direction, then review contact sheet and map crop before any golden baseline update.

## 2026-05-27 - PR-08 Floor/Wall Runtime Probe + Map Compositor Pass

- Loop type: resource candidate / runtime compositor iteration
- Target area: `tileset.ruins.ground_01`, `tileset.ruins.wall_01`, map-stage material read
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-f.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-wall-candidate-f.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-f-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-f-map-preview.png`
  - `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`
  - `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`
- Commands run:
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope`
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - `./gradlew maintainabilityLint`
  - `git diff --check`
- Result:
  - PASS: PR-08 owner resource gates after floor/wall mutation.
  - PASS: resource authority lint after runtime tile migration.
  - PASS: `TileRendererCanvasTest` after compositor contract changes.
  - PASS: `maintainabilityLint`.
  - PASS: `git diff --check`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this loop intentionally generated evidence without updating golden baselines.
  - Latest actual focused golden hashes: `ui-demo-new-parity-1672x941=c18ba2ffa131ef08bb4729567cb3bb3bd6e4b5cf0548b44f7b3976442e72fe01`, `ui-demo-new-parity-1280x800=2ec6e6841bff2a52cb9cfe345a9d2c658f50db4b74a6eefc064d6109f266328c`, `ui-demo-new-map-stage-crop=0f70f9e36bb39cc2e9e88b6a369d035d96223818efcc6af17e8f7ebc9d0f69f6`.
- Director verdict:
  - Not accepted for final director-grade closure.
  - Accepted only as a runtime probe: floor/wall resources are cleaner, wall per-cell dark box treatment is reduced, and floor unifier improves the map read without covering actors/loot.
  - The map still reads too fog-heavy and grid-forward compared with `UI/UI-demo-new.png`; no golden baseline update is authorized.
- Remaining risk:
  - Floor texture still lacks enough high-frequency stone relief at runtime scale.
  - Wall tiles still need a more continuous masonry asset, not only renderer-level relief tuning.
  - Map lighting/fog composition remains below director-grade target quality.
- Next action:
  - Continue Slice 2/Slice 4 together: generate a stronger seamless wall/floor pair and reduce map fog dominance with a before/after golden crop comparison before any baseline update.

## 2026-05-27 - PR-08 Candidate L Resource Integration + Fog Reduction

- Loop type: resource candidate / runtime compositor iteration
- Target area: `tileset.ruins.ground_01`, `tileset.ruins.wall_01`, first-screen map fog/light balance
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-g.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-wall-candidate-g.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-g-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-h.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-wall-candidate-h.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-h-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-h-variants.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-i.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-wall-candidate-i.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-i-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-i-variants.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-j.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-k.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-l.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-wall-candidate-l.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-l-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-l-map-preview.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-l-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-l-parity-1672x941.png`
- Commands run:
  - `python3` deterministic resource exploration/integration scripts for candidates `G/H/I/J/K/L`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope`
  - `./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r02-ui-demo-ruins-tiles`
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - `./gradlew maintainabilityLint`
  - `git diff --check`
- Result:
  - PASS: PR-08 owner resource gates after Candidate L floor/wall mutation.
  - PASS: PR-08-specific sprite map report update for `r02-ui-demo-ruins-tiles`.
  - PASS: resource authority lint after runtime tile migration.
  - PASS: `TileRendererCanvasTest` after compositor alpha and terrain bleed changes.
  - PASS: `maintainabilityLint`.
  - PASS: `git diff --check`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this loop intentionally generated evidence without updating golden baselines.
  - Latest actual focused golden hashes: `ui-demo-new-parity-1672x941=e91e062a3171968a601721bae9233c43621e13ab7f229d7f2bcf8e7ff81530d1`, `ui-demo-new-parity-1280x800=71b202da04e4abc3c1d1009515cd21331c7f16cf1a84c0e2d363ed45649f2d2a`, `ui-demo-new-map-stage-crop=045f3cf298c87a0bfa27ebdf5cb881e4d19f3576240b4219a2ddef423f38a7a0`.
- Director verdict:
  - Not accepted for final director-grade closure.
  - Accepted as a forward iteration: Candidate L makes runtime floor/wall materials visibly read as authored stone, while the compositor pass reduces the previous yellow-green fog dominance.
  - Remaining blocking gap: the map still reads too grid-forward compared with `UI/UI-demo-new.png`; the visible room needs a better seam suppression strategy that does not re-fog the floor.
- Remaining risk:
  - Candidate L floor uses a single derived stone cell; repeated cell rhythm is still visible under the current renderer.
  - The right panel and bottom deck are not improved in this entry.
  - No golden baseline update is authorized.
- Next action:
  - Continue Slice 4: suppress visible-room lattice with a deterministic seam treatment that breaks repeated cell borders without hiding actor, loot, telegraph or floor material detail; then produce a before/after crop for director review.

## 2026-05-27 - PR-08 Candidate O Pre-Actor Seam Weave + Ground Tint Probe

- Loop type: runtime compositor iteration
- Target area: visible-room floor lattice, repeated `tileset.ruins.ground_01` cadence, marker-safe seam treatment
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-m-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-m-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-n-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-n-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-o-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-o-parity-1672x941.png`
- Commands run:
  - `python3` PIL edge/luminance inspection for current floor/wall runtime assets
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest`
  - `./gradlew maintainabilityLint`
  - `git diff --check`
- Result:
  - FAIL then FIXED: first `TileRendererCanvasTest` run exposed variant-hash-only seam bridge triggers with `count=0`; implementation was changed to use room-relative coordinates for trigger selection.
  - PASS: `TileRendererCanvasTest` after coordinate-trigger seam bridge.
  - PASS: `TileRendererCanvasTest` after wider pre-actor seam bridge caps and deterministic floor tint variation.
  - PASS: `DemoShellRendererTest`.
  - PASS: `maintainabilityLint`.
  - PASS: `git diff --check`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this loop intentionally wrote PR-08 candidate evidence without updating golden baselines.
  - Latest actual focused golden hashes: `ui-demo-new-parity-1672x941=84cbfe09d345055290c03d210a05de6b5aa7d3cb5687c746bf5ddcb34e839cfd`, `ui-demo-new-parity-1280x800=7f67780e2a2aef0f4f26be26ee99a29e4580b150162d916c68ca89eb2cdd30da`, `ui-demo-new-map-stage-crop=b770b02a34dd032de5c1a4fceb1e9bc0456faf67c9b1a9509234d7d188ac5e9d`.
- Director verdict:
  - Not accepted for final director-grade closure.
  - Accepted only as a forward compositor probe: seam treatment is now pre-actor, deterministic, non-continuous, and test-covered; floor tint variation slightly reduces single-texture sameness without changing resource keys or schema.
  - Blocking gap remains: the map still reads grid-forward versus `UI/UI-demo-new.png`; further renderer rectangles are unlikely to close the gap alone.
- Remaining risk:
  - The current floor asset still carries a repeated per-cell rhythm at runtime scale.
  - Right panel and bottom deck cohesion are not improved in this entry.
  - No golden baseline update is authorized.
- Next action:
  - Return to Resource Candidate Loop: create a cleaner runtime floor candidate with less intrinsic edge/cell rhythm and stronger material variation, then run PR-08 resource gates plus focused renderer tests before another director crop review.

## 2026-05-27 - PR-08 Candidate P Floor Resource Blend Integrated

- Loop type: resource candidate / runtime compositor iteration
- Target area: `tileset.ruins.ground_01`, visible-room floor cell rhythm, PR-08 runtime map crop
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-p.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-wall-candidate-p.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-p-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-p-map-preview.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-p-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-p-parity-1672x941.png`
- Commands run:
  - `python3` deterministic candidate P floor blend from K/L floor references
  - `python3 scripts/slice_spritesheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`
  - `python3 scripts/render_contact_sheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope`
  - `./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r02-ui-demo-ruins-tiles`
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- Result:
  - PASS: raw sheet slicing and contact sheet regeneration.
  - PASS: PR-08 owner resource gates and PR-08-specific sprite map report update for `r02-ui-demo-ruins-tiles`.
  - PASS: `assetLint`, `styleLint`, `manifestLint`, `resourcePipelineLint`.
  - PASS: `TileRendererCanvasTest` and `DemoShellRendererTest`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this loop intentionally generated PR-08 visual evidence without updating golden baselines.
  - Latest actual focused golden hashes: `ui-demo-new-parity-1672x941=1b99913ca9302c7ad71cf7073de83b917417da834a537b0eb81f569ee56c24ce`, `ui-demo-new-parity-1280x800=8b1cb4542217a826bf63370ab2ab8fbe40a323a294f9a34f72ee53e4ccf1c0cf`, `ui-demo-new-map-stage-crop=2beb08065f0e8a30853d122184d278503dc06b243796a5cf839822a37eb3c109`.
- Director verdict:
  - Not accepted for final director-grade closure.
  - Accepted as the current forward resource candidate: Candidate P reduces the obvious repeated crack cadence from Candidate L/O while preserving floor material and marker readability.
  - Blocking gap remains: the first-screen still reads more grid-forward and less authored than `UI/UI-demo-new.png`, and right panel / bottom deck quality have not yet been addressed in this candidate.
- Remaining risk:
  - Candidate P may be slightly flatter than the target art direction; next pass should improve material depth without reintroducing a single repeated crack motif.
  - Wall/floor contact and UI chrome still need director-grade treatment.
  - No golden baseline update is authorized.
- Next action:
  - Continue with a map-stage resource/compositor polish pass focused on floor depth plus wall/floor contact, then move to right panel and bottom deck cohesion once the map stops being the blocking first read.

## 2026-05-27 - PR-08 Candidate S Edge-Neutral Floor + Seam Underpaint Probe

- Loop type: resource candidate / runtime compositor iteration
- Target area: `tileset.ruins.ground_01`, runtime floor edge rhythm, visible-room floor-to-floor seam suppression
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-q.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-r.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-candidate-s.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-s-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-floor-wall-candidate-s-map-preview.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-s-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-s-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-s-underpaint-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/pr08-runtime-candidate-s-underpaint-parity-1672x941.png`
- Commands run:
  - `python3` PIL generation for candidates Q/R/S and edge luminance checks
  - `python3 scripts/slice_spritesheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`
  - `python3 scripts/render_contact_sheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope`
  - `./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r02-ui-demo-ruins-tiles`
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest`
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- Result:
  - Candidate Q rejected: boundary rhythm improved, but inherited a strong repeated crack motif.
  - Candidate R rejected: reduced crack motif, but reintroduced family-cell border rhythm.
  - Candidate S integrated as the current probe: edge/center brightness delta for runtime `tileset_ruins_ground_01.png` moved to about `+0.73`, down from Candidate P's about `+6.99`.
  - PASS: raw sheet slicing and contact sheet regeneration.
  - PASS: PR-08 owner resource gates and PR-08-specific sprite map report update for `r02-ui-demo-ruins-tiles`.
  - PASS: `assetLint`, `styleLint`, `manifestLint`, `resourcePipelineLint`.
  - PASS: focused `TileRendererCanvasTest`, `DemoShellRendererTest`, and `maintainabilityLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this loop intentionally generated PR-08 visual evidence without updating golden baselines.
  - Latest actual focused golden hashes after seam underpaint: `ui-demo-new-parity-1672x941=8bba089bf3a65c405bd89b72491a7b71dfabbcf6fee13c7d74de5ac4fd1eab8a`, `ui-demo-new-parity-1280x800=03231c963b2367be1b809b0c2f256cc5ca994a7f4e6c9d6834aec388eddd424d`, `ui-demo-new-map-stage-crop=de703241da2e0173be184da0d48a24e3f237b71e668d16361090d4d25a56fd42`.
- Director verdict:
  - Not accepted for final director-grade closure.
  - Accepted only as a diagnostic forward probe: floor PNG intrinsic edge darkness is no longer the dominant cause, and pre-actor seam underpaint is test-covered without touching schema, resource keys or gameplay state.
  - Blocking gap remains: visible-room grid lines are still too dominant at first read, so the remaining problem is now a runtime composition / shell-scale hierarchy issue, not just a floor bitmap edge issue.
- Remaining risk:
  - Candidate S is flatter than the desired worn-stone target and should not be treated as final art.
  - Right panel and bottom deck cohesion are still untouched.
  - No golden baseline update is authorized.
- Next action:
  - Continue with a runtime compositor hierarchy pass: reduce explicit per-cell floor material/seam readability and strengthen room-scale authored shapes, then start Slice 5 right-panel/bottom-deck cohesion once the map first read is no longer blocked.

## 2026-05-27 - PR-08 Candidate AA/AB/AC Generated Floor-Wall Batch

- Loop type: resource candidate loop
- Target area: `tileset.ruins.ground_01`, `tileset.ruins.wall_01`, map-stage first read
- Contract alignment:
  - Re-read `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md` and `UI/review/dark-uiux-pr08-evidence-and-direction.md` before continuing.
  - Rejected the temporary source-rect atlas idea because the latest goal explicitly rejects new manifest / atlas / region schema for PR-08.
  - Kept existing resource keys and schema unchanged; candidates are full-tile replacements in the existing `r02-ui-demo-ruins-tiles` cells.
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/pr08-generated-floor-wall-source.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/pr08-generated-wall-source.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/pr08-generated-floor-wall-source-single-tile-prompt.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/pr08-floor-wall-candidate-aa-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/pr08-floor-wall-candidate-ab-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/pr08-floor-wall-candidate-ac-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/runtime-aa/pr08-runtime-candidate-aa-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/runtime-ab/pr08-runtime-candidate-ab-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/runtime-ac/pr08-runtime-candidate-ac-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/pr08-runtime-candidate-aa-ab-ac-comparison.png`
- Commands run:
  - Built-in `image_gen` for generated floor/wall batch sources.
  - `python3` PIL extraction and edge-neutralization for candidates `X/Y/Z` and `AA/AB/AC`.
  - `python3 scripts/slice_spritesheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`
  - `python3 scripts/render_contact_sheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks` for `AA`, `AB`, and `AC`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope`
  - `./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r02-ui-demo-ruins-tiles`
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest`
  - `./gradlew maintainabilityLint`
  - `python3` readiness report materialization for `build/reports/verification/dark-uiux/director-grade-asset-readiness.json`
  - `git diff --check`
- Result:
  - Candidate `X/Y/Z` first source rejected at contact preview: `X` is too flat and still grid-seamed; `Y/Z` introduce obvious large-stone repeat motifs.
  - Candidate `AA` runtime tested: cleanest edge behavior but too flat and still grid-forward.
  - Candidate `AB` runtime tested and integrated as the current forward resource candidate: best balance of material depth and marker readability in this batch.
  - Candidate `AC` runtime tested and rejected: stronger style, but large slab repetition dominates the first read.
  - PASS: raw sheet slicing and contact sheet regeneration.
  - PASS: PR-08 owner resource gates and PR-08-specific sprite map report update for `r02-ui-demo-ruins-tiles`.
  - PASS: `assetLint`, `styleLint`, `manifestLint`, `resourcePipelineLint`.
  - PASS: `TileRendererCanvasTest`, `DemoShellRendererTest`, and `maintainabilityLint`.
  - MATERIALIZED: `build/reports/verification/dark-uiux/director-grade-asset-readiness.json` with floor/wall `qaStatus=pending` and explicit blocking findings for the remaining grid-forward runtime crop.
  - PASS: `git diff --check`.
  - EXPECTED FAIL: focused golden screenshot hash gate for all runtime candidates because this loop generated visual evidence without updating golden baselines.
  - `AA` focused hashes: `ui-demo-new-parity-1672x941=e5e4e3f515b3cd4dded7ec5c8540a9e5be9f3218e6d499e3ed2a472de894b945`, `ui-demo-new-parity-1280x800=cabd1db427f4ccf381dedb3abe9164459afec2364c789f1bbce4861b0e9610f4`, `ui-demo-new-map-stage-crop=68a337000138467ca5a022fa26ce78290509295ff774e8a65de2755190897c78`.
  - `AB` focused hashes: `ui-demo-new-parity-1672x941=c0d78783ca71533429f32ce8021ad5458d82be43718973c05ed43693db4a463e`, `ui-demo-new-parity-1280x800=2ff18a66170f3c701582f6d178476fde72650c15cf025637a4d7887a0d7e7207`, `ui-demo-new-map-stage-crop=8a6fc3871652bf2307bfb669691c01b13a34725b9286ab8cb3893a8e714b9d56`.
  - `AC` focused hashes: `ui-demo-new-parity-1672x941=355d344d948ffb23f475258cc58474581c466fec35a6d93039f5f94a469136c3`, `ui-demo-new-parity-1280x800=6a27334eabbfa42e149aa233f154213dcc668c333bdf2c61a86537bfbb96e52d`, `ui-demo-new-map-stage-crop=9ba451dbebd3a9fc7ac25085e16c958bf8c377aeda0888146db04907d68b8605`.
  - Integrated AB resource hashes: raw sheet `78c901655249d1a8e6cee62656629f9d72eb81b55d47a23081a2794e201716c8`, floor runtime `f1416a830ec8b231136745a66ac9ab9bb8b915bbd14787386c65ddcebf352c5c`, wall runtime `5ec1cb0689a4d33d9006c119ad8a4aa59cc6ee7a2683b5e37b74bbb7d096ac53`, contact sheet `3acf08f902ed2b10363a61923e0915a23a6a2c51a96e479773ca707f138d1387`.
- Director verdict:
  - Not accepted for final director-grade closure.
  - Accepted as a real resource-generation sprint and current forward candidate, because it generated multiple distinct floor/wall families, compared contact/runtime crops, and kept PR-08 schema/key constraints intact.
  - Blocking gap remains: even the best generated single-tile candidate still reads too grid-forward at runtime. This confirms the next move should be a runtime compositor reset at room scale, not another per-cell alpha or seam micro-pass.
- Remaining risk:
  - AB improves authored material depth, but the single-tile runtime model still exposes a regular lattice under the current renderer.
  - Right panel and bottom deck chrome are still untouched in runtime and remain a separate Slice 5 blocker.
  - Golden baselines remain unchanged.
- Next action:
  - Enter Slice 4 Runtime Compositor Reset with a narrow goal: stop floor quality depending on per-cell material rectangles, preserve authored AB resources, and introduce room-scale hierarchy only where it does not create a second visibility authority. If that still fails director crop review, proceed to Slice 5 chrome only after recording the map blocker explicitly.

## 2026-05-27 - PR-08 AB Room-Scale Compositor Reset Probe

- Loop type: runtime compositor loop
- Target area: map-stage room hierarchy, floor material authority, renderer-owned per-cell floor overlays
- Contract alignment:
  - Continued from latest goal/log next action instead of starting a new ad hoc renderer pass.
  - Preserved `tileset.ruins.ground_01` / `tileset.ruins.wall_01` AB resources as authored material authority.
  - Removed renderer compensation paths that made floor quality depend on per-cell material grain, deterministic tint variants, and seam-underpaint strips.
  - Did not change manifest keys, schema, visibility authority, gameplay state, golden baselines or right/bottom chrome.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/runtime-ab-room-reset/pr08-runtime-candidate-ab-room-reset-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/runtime-ab-room-reset-no-tint/pr08-runtime-candidate-ab-room-reset-no-tint-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/runtime-ab-room-reset-no-tint-no-underpaint/pr08-runtime-candidate-ab-room-reset-no-tint-no-underpaint-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/pr08-runtime-ab-room-reset-comparison.png`
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - `git diff --check`
- Result:
  - PASS after test contract update: `TileRendererCanvasTest` now asserts floor resources are not covered by renderer-owned per-cell aggregate grain, fracture kernels, chipped-stone clusters, hairline etches, tint variants or seam-underpaint strips.
  - PASS: `DemoShellRendererTest`.
  - PASS: `maintainabilityLint`.
  - PASS: `git diff --check`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this loop generated visual evidence without updating golden baselines.
  - `AB room reset` focused hashes: `ui-demo-new-parity-1672x941=79560892c3589115cb12a3c3bee3e08a5e1347f5612facd6fef95f13a97e4e34`, `ui-demo-new-parity-1280x800=c80cac71daee88ad29ed2c503a193e48a002977ea3442eeb83e2a7dfdc6829b6`, `ui-demo-new-map-stage-crop=0edd1d05cc73e226852b5088b402cd8cc6542aac5228f75f31db686f61be14f0`.
  - `AB no-tint` focused hashes: `ui-demo-new-parity-1672x941=80bbf9f43455c74e2172db94d239b8852ee2b6e717e1505cf2b533ca740e49c8`, `ui-demo-new-parity-1280x800=b4da3644532f2548ab40e223b096bc34c27cecf0008a3cfe91cb29f4efc47e99`, `ui-demo-new-map-stage-crop=e247100dd6d4c606669bdf9147d6aa9e5b084f24a7561a5ad72d2a48e6e5f2d3`.
  - `AB no-tint no-underpaint` focused hashes: `ui-demo-new-parity-1672x941=20266901295e4dff38e05f03ed2913fc74783b475fea398a94f662275a3cb1d6`, `ui-demo-new-parity-1280x800=cb3f8272e0df7d1b708fb3be3536a3698b4a38dcc90ff4a967caa6763079c5dd`, `ui-demo-new-map-stage-crop=3775102c16c8940fe334ef29aa21a418df134e5b8a4f619293a91244dfce7c61`.
  - Evidence image hashes: room reset crop `91a63a8be5fec29570e169db240f4d0c24dff44b7802beedb924b148d7132798`, no-tint crop `0b1e53a72cdf8310d05d612e593084d65da983436883d7f1a9c342b584b1933c`, no-tint/no-underpaint crop `b68177601c17b96a40d4e2b67f0c6421d8f0b424ce5355537542016dff7a092a`, comparison `f9af69459e0e437cc73f6ac306d0e3af2041241be9f9dcbd7f2982936eee1935`.
- Director verdict:
  - Not accepted for final director-grade closure.
  - Accepted as a necessary cleanup of renderer ownership: floor material detail is no longer simulated by per-cell renderer micro-overlays.
  - Visual movement is positive but insufficient. The map still reads too regular at first glance; the remaining first-read gap is not solved by removing per-cell overlays and should not be chased by more alpha/seam tweaks.
- Remaining risk:
  - The AB single-tile resource still exposes repeated cadence in the current map composition.
  - Right panel and bottom deck chrome remain untouched and still block full-screen director-grade cohesion.
  - `drawFloorMaterial` is now intentionally unreachable through the PR-08 render order; a follow-up cleanup can delete the dead function after one more review pass if no owner test still depends on it.
- Next action:
  - Stop map micro-iteration here and move to Slice 5 right-panel / bottom-deck chrome sprint, while keeping the current map blocker recorded. The next useful visual lift is full-screen cohesion, not another floor seam alpha pass.

## 2026-05-28 - PR-08 Candidate AD/AE/AF Chrome Resource Sprint

- Loop type: resource candidate loop
- Target area: right panel chrome, slot chrome, bottom hero/action/log deck chrome, command hint plate
- Contract alignment:
  - Re-read `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`, `UI/review/dark-uiux-pr08-evidence-and-direction.md`, `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md`, `docs/INDEX.md`, `docs/rule/kotlin.md`, and `docs/rule/ai-change-governance.md`.
  - Followed the latest Slice 5 direction instead of continuing map seam / alpha micro-iteration.
  - Kept existing `targetKey`, sheet cells, manifest schema and runtime text ownership unchanged.
  - No generated image contains baked labels, hotkeys, numbers, manifest keys or log text.
- Changed files:
  - `UI/sprite-sheets/key-registry.yaml`
  - `UI/sprite-sheets/owner-contracts/pr02-owner-keys.yaml`
  - `UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml`
  - `UI/sprite-sheets/owner-contracts/pr08-owner-keys.yaml`
  - `assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png`
  - `assets-src/image/raw/sheets/dark-v1/r01b-ui-shell-chrome.png`
  - `assets-src/image/contact-sheets/dark-v1/r01-ui-chrome-contact.png`
  - `assets-src/image/contact-sheets/dark-v1/r01b-ui-shell-chrome-contact.png`
  - `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `client/src/main/resources/dark-v1/ui/ui_frame_panel_body.png`
  - `client/src/main/resources/dark-v1/ui/ui_frame_panel_focus.png`
  - `client/src/main/resources/dark-v1/ui/ui_frame_slot_empty.png`
  - `client/src/main/resources/dark-v1/ui/ui_frame_slot_equipped.png`
  - `client/src/main/resources/dark-v1/ui/ui_frame_slot_selected.png`
  - `client/src/main/resources/dark-v1/ui/ui_shell_hero_card_frame.png`
  - `client/src/main/resources/dark-v1/ui/ui_shell_action_deck_frame.png`
  - `client/src/main/resources/dark-v1/ui/ui_shell_log_deck_frame.png`
  - `client/src/main/resources/dark-v1/ui/ui_shell_right_section_divider.png`
  - `client/src/main/resources/dark-v1/ui/ui_shell_command_hint_plate.png`
  - `build/reports/verification/dark-uiux/director-grade-asset-readiness.json`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/pr08-generated-chrome-source-ad-conservative.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ae-material-depth/pr08-generated-chrome-source-ae-material-depth.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/af-strong-style/pr08-generated-chrome-source-af-strong-style.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/pr08-chrome-candidate-ad-conservative-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ae-material-depth/pr08-chrome-candidate-ae-material-depth-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/af-strong-style/pr08-chrome-candidate-af-strong-style-contact.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/pr08-chrome-runtime-right-panel-comparison.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/pr08-chrome-runtime-bottom-deck-comparison.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/pr08-chrome-runtime-fullscreen-comparison.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-right-panel-grid.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-bottom-deck-no-command-hints.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-parity-1672x941.png`
- Commands run:
  - Built-in `image_gen` for three chrome source sheets: `AD` conservative, `AE` material-depth, `AF` strong-style.
  - `python3` PIL extraction/composition for candidate raw sheets, contact sheets and runtime comparison montages.
  - `python3 scripts/slice_spritesheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`
  - `python3 scripts/render_contact_sheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks` for baseline, `AD`, `AE`, `AF`, and final `AD`.
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr02OwnerScope darkManifestCoveragePr02_1OwnerScope darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest`
- Result:
  - Candidate `AD` integrated as the current forward chrome candidate: it improves right-panel slot/frame family cohesion and bottom-deck material unity without making the generated deck art compete with runtime text and icons.
  - Candidate `AE` rejected for final integration: stronger material depth, but the hero/action deck backgrounds become too heavy behind runtime labels and gauges.
  - Candidate `AF` rejected for final integration: strongest silhouette, but over-ornate relative to the current runtime density and still less readable than `AD`.
  - PR-08 owner contract now explicitly covers `12` keys: floor/wall plus `ui.frame.panel.body`, `ui.frame.panel.focus`, `ui.frame.slot.empty`, `ui.frame.slot.equipped`, `ui.frame.slot.selected`, `ui.shell.hero_card.frame`, `ui.shell.action_deck.frame`, `ui.shell.log_deck.frame`, `ui.shell.right_section.divider`, and `ui.shell.command_hint.plate`.
  - PASS: raw sheet slicing and contact sheet regeneration.
  - PASS: `darkKeyRegistryLint`, `darkSpriteSheetLint`, `spriteSheetMapLint`, `darkManifestCoveragePr02OwnerScope`, `darkManifestCoveragePr02_1OwnerScope`, `darkManifestCoveragePr08OwnerScope`, `assetLint`, `styleLint`, `manifestLint`, and `resourcePipelineLint`.
  - PASS: PR-08-specific sprite map report update for `r01-ui-chrome`, `r01b-ui-shell-chrome`, and `r02-ui-demo-ruins-tiles`.
  - PASS: `TileRendererCanvasTest` and `DemoShellRendererTest`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this loop produced PR-08 candidate evidence without updating golden baselines.
  - Final `AD` focused hashes: `ui-demo-new-parity-1672x941=b0df381d10d1e99fe0819df2dd20d843bf272d037d25193fa78dc34e3c032f35`, `ui-demo-new-parity-1280x800=c2f92ce2f11debd6564116af108e45793526b6c611b651264ea23ed16f608e92`, `ui-demo-new-right-panel-grid=b96f246cdb9ffb8a2af026d856f8efaaf5911a46ff0bde56d8693ab2451f0ae8`, `ui-demo-new-bottom-deck-no-command-hints=43bbe2b8a116d4eaf3d47af610919ab4a82f4ee548da1af495b6afb268f5c8d2`, `ui-demo-new-map-stage-crop=3775102c16c8940fe334ef29aa21a418df134e5b8a4f619293a91244dfce7c61`.
  - Integrated `AD` resource hashes: `r01-ui-chrome=c56e4735697887f246a261a2b4d9258384ba5bece6cebcc054f0962022fce8db`, `r01b-ui-shell-chrome=6f0a3f1a552e8d4a33aa433cee682265305226100089486237d3e2377102b3ac`, `r01 contact=8c77d7f0cc23284523e3f729ea11552f07b20696605bff6dac325e583e6aca23`, `r01b contact=f16df371ae2560bec2dc328051cf32ee2643916a9d8ffba0004546345846e16a`.
  - Updated readiness report hash: `37eeb1f5d633f62ca20dd45ab87d45ade729fa8de00c2c453f2e3d31a05e7d16`.
- Director verdict:
  - Not accepted for final director-grade closure.
  - Accepted as the current forward Slice 5 resource candidate: the right panel and bottom deck now use a coherent regenerated chrome family rather than relying only on local renderer rectangles.
  - Full-screen first read is still below `UI/UI-demo-new.png`; map room-space hierarchy and final director evidence labels remain open.
- Remaining risk:
  - The `AD` chrome source uses black-backed cells for the replaced body/slot/deck surfaces; this is acceptable for the current consumers but should be revisited if the same keys are reused on brighter modal backgrounds.
  - Bottom deck is more cohesive, but the runtime still has some double-structure from generated action socket wells plus existing action-slot overlays.
  - Golden baselines remain unchanged; packaged whitebox and `verifyChanged` are not run in this iteration.
- Next action:
  - Do a director-rubric self-review pass using the AD right-panel/bottom-deck evidence plus current AB map crop. If accepted as forward but not final, the next implementation pass should address shell-scale map room hierarchy and reduce remaining double-structure in the action deck without returning to per-cell seam micro-tweaks.

## 2026-05-28 - PR-08 AD Chrome Director-Rubric Self-Review

- Loop type: director-rubric self-review loop
- Target area: full-screen first read, map-stage room hierarchy, right panel chrome, bottom deck chrome, telegraph/combat readability proxy
- Contract alignment:
  - Continued from the latest Slice 5 log stop condition instead of starting another floor/wall resource batch.
  - Did not modify runtime resources, manifests, owner contracts, Kotlin code, schema, atlas/region structure or golden baselines.
  - Treated `UI/UI-demo-new.png` as quality reference only, not as mapping truth.
  - Kept AD chrome accepted only as current forward candidate, not as director-grade final acceptance.
- Changed files:
  - `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-rubric-self-review.md`
  - `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-2026-05-28-comparison-board.png`
  - `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-current-telegraph-combat-proxy-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-reference-bottom-deck-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-reference-map-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-reference-right-panel-crop.png`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-2026-05-28-comparison-board.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/baseline-before-chrome/runtime/pr08-chrome-candidate-baseline-before-chrome-ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-right-panel-grid.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-bottom-deck-no-command-hints.png`
  - `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-current-telegraph-combat-proxy-crop.png`
- Commands run:
  - `sed` / `tail` / `rg` inspections of the goal, log and direction documents.
  - Local image review of `UI/UI-demo-new.png`, AD runtime full/right/bottom crops and current map crop.
  - `python3` PIL generation for reference crops, telegraph/combat proxy crop and comparison board.
  - `shasum -a 256 UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/*.png`
- Result:
  - ACCEPT-FORWARD: AD remains the current forward chrome candidate because it improves chrome-family cohesion without baking text or changing keys/schema.
  - REJECT-FINAL: the full screen still fails director-grade close. The center map reads as a regular lattice before it reads as an authored room space, and the bottom action deck has double structure from generated socket wells plus runtime overlays.
  - MISSING FINAL EVIDENCE: the telegraph/combat artifact in this loop is a proxy crop from the current map-stage evidence, not a dedicated `dark-uiux-pr08-director-telegraph-combat-crop`.
  - Artifact hashes: comparison board `c214c26399ee6f965439d580d2a1dc3bf4a698780201cb0e30ae4b2e52c4e0b6`; telegraph/combat proxy `b242f3c18e72a4f66c8a03e5be183ab0afb46a302d2004e25d7807eb84d87bc1`; reference map crop `9749d77a219af549042c543cdf50be37205d7fe7b67457970cf88350f8375bed`.
- Director verdict:
  - Not accepted for final director-grade closure.
  - AD accepted only as forward chrome basis.
  - Next work should be a narrow runtime compositor/layout pass, not another resource candidate batch and not more per-cell seam/alpha patching.
- Remaining risk:
  - The current map still lacks room-scale hierarchy and organic dark-field falloff.
  - Bottom deck text remains readable, but the action deck frame stack is visually overdetermined.
  - Golden baselines, packaged whitebox and `verifyChanged` remain intentionally unrun for this visual self-review loop.
- Next action:
  - Enter a runtime compositor/layout loop focused on shell-scale map room hierarchy and bottom action-deck double-structure reduction. Keep AD chrome and AB floor/wall as the forward resource basis unless new evidence reclassifies the blocker as resource-owned.

## 2026-05-28 - PR-08 Action Deck Recessed-Pad Runtime Layout Pass

- Loop type: runtime compositor/layout loop
- Target area: bottom action deck double-structure after AD chrome integration
- Contract alignment:
  - Continued from the director-rubric self-review blocker: reduce action-deck frame stacking without touching schema, manifest keys, resources or golden baselines.
  - Kept AD chrome resources as the current forward material basis.
  - Changed runtime layout hierarchy only: full-height per-slot wells no longer compete with the generated slot chrome and shared command plinth.
  - Did not change map resource/compositor ownership, gameplay state, text ownership or golden baselines.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/runtime-action-deck-recessed-pads/`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-action-deck-recessed-pads/pr08-action-deck-recessed-pads-comparison.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-action-deck-recessed-pads/pr08-action-deck-recessed-pads-ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-action-deck-recessed-pads/pr08-action-deck-recessed-pads-ui-demo-new-bottom-deck-no-command-hints.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-action-deck-recessed-pads/pr08-action-deck-recessed-pads-ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-action-deck-recessed-pads/pr08-action-deck-recessed-pads-ui-demo-new-right-panel-grid.png`
- Commands run:
  - `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas keeps chinese hotbar labels inside their slot cards'`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - `python3` PIL copy/comparison-board generation for runtime layout evidence.
  - `shasum -a 256 UI/review/dark-uiux-pr08-exploration/runtime-action-deck-recessed-pads/*.png`
- Result:
  - PASS after assertion calibration: the focused hotbar label test now asserts no high-alpha full-height per-slot wells remain and that action icons keep restrained recessed pads while labels stay on the shared command plinth.
  - PASS: `TileRendererCanvasTest`, `DemoShellRendererTest`, and `maintainabilityLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because runtime visuals changed and baselines remain unchanged.
  - Focused golden actual hashes from the expected failure: `ui-demo-new-parity-1672x941=02c156ff30e46d1de348c91df94d75251b6c2a7b3338b7261e1ecfe1a10a4f3e`, `ui-demo-new-parity-1280x800=e71656680587fc8462f4cf83458b19a5a7dcd3e56899b195d263ee576d683540`, `ui-demo-new-right-panel-grid=b96f246cdb9ffb8a2af026d856f8efaaf5911a46ff0bde56d8693ab2451f0ae8`, `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`, `ui-demo-new-map-stage-crop=3775102c16c8940fe334ef29aa21a418df134e5b8a4f619293a91244dfce7c61`.
  - Evidence file hashes: comparison `b88d78161aed8216728c8ca21289953f77c1d2d4eb12996a742b90b1319dc8f6`, bottom deck `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`, full screen `cd5a02db00f194912b4c53b3e65f02aac8a833fa726c4d995e83e1e46286f8cb`.
- Director verdict:
  - Accepted as a narrow forward layout improvement, not final closure.
  - Positive: bottom action icons now sit on restrained pads, and the shared command plinth owns the label layer more clearly.
  - Still blocking: full-screen first read remains below the reference because map room-space hierarchy is unchanged.
- Remaining risk:
  - The visual improvement is intentionally narrow and does not solve the map lattice blocker.
  - The runtime evidence still lacks a dedicated PR-08 director telegraph/combat crop.
  - Golden baselines, packaged whitebox and `verifyChanged` remain unrun for this iteration.
- Next action:
  - Continue the runtime compositor loop on shell-scale map room hierarchy: make the visible dungeon read less like a rectangular grid and more like an authored room space while preserving actor/loot/telegraph readability and without reintroducing per-cell material overlays.

## 2026-05-28 - PR-08 Room Aperture Hierarchy Runtime Compositor Pass

- Loop type: runtime compositor loop
- Target area: shell-scale map room hierarchy after action-deck recessed-pad pass
- Contract alignment:
  - Continued from the latest goal stop condition and `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md` Runtime Compositor Loop rules.
  - Changed compositor hierarchy, not schema, manifest keys, sprite sheet regions, resources, text ownership or golden baselines.
  - Moved visible-room atmosphere from post-actor warm overlay into the pre-actor map material composition layer so actor, loot and telegraph assets remain above the floor unifier and room-space wash.
  - Removed the remaining post-actor floor-tile amber glow loop from `drawVisibleRoomAtmosphere`; floor resource detail remains owned by generated terrain assets plus deterministic room-scale/cross-cell compositor layers.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-aperture-hierarchy/`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-aperture-hierarchy/pr08-room-aperture-hierarchy-comparison-board.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-aperture-hierarchy/pr08-room-aperture-hierarchy-ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-aperture-hierarchy/pr08-room-aperture-hierarchy-ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-aperture-hierarchy/pr08-room-aperture-hierarchy-ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-aperture-hierarchy/pr08-room-aperture-hierarchy-ui-demo-new-bottom-deck-no-command-hints.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-aperture-hierarchy/pr08-room-aperture-hierarchy-ui-demo-new-right-panel-grid.png`
- Commands run:
  - `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas keeps authored tile art clear beneath atmosphere washes'`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - `python3` PIL copy/comparison-board generation for runtime map hierarchy evidence.
  - `shasum -a 256 UI/review/dark-uiux-pr08-exploration/runtime-room-aperture-hierarchy/*.png`
  - `git diff --check`
  - Local absolute-path pollution scan over the new evidence directory, this log target and touched Kotlin files.
- Result:
  - PASS: focused tile-art clarity test now asserts no tile-by-tile amber floor glow returns and that visible rooms use room-scale crown shadow, side pressure and walkable-stone plane.
  - PASS: `TileRendererCanvasTest`, `DemoShellRendererTest`, and `maintainabilityLint`.
  - PASS: `git diff --check`.
  - PASS: no local absolute paths found in the new evidence directory, log target or touched Kotlin files.
  - EXPECTED FAIL: focused golden screenshot hash gate, because runtime visuals changed and baselines remain unchanged.
  - Focused golden actual hashes from the expected failure: `ui-demo-new-parity-1672x941=b489a71f6cc763d023e07256301b9b10344086bdcc604439c0dfab1838ef2e34`, `ui-demo-new-parity-1280x800=a59ce70d706da7446deb66c8d1af56f2088d40b328cef9350fdfabb4628966ef`, `ui-demo-new-right-panel-grid=b96f246cdb9ffb8a2af026d856f8efaaf5911a46ff0bde56d8693ab2451f0ae8`, `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`, `ui-demo-new-map-stage-crop=9642984919f8a5a359b7725006ee3862cb68b014493bfc1d3588dad0c8270e1c`.
  - Evidence file hashes: comparison `f2b2bc0adf79dbdd6b0bb5db5a39b36675b4223f79d13c3f2fb8f2bd43e72c43`, map crop `84f12a75e78b09b680b0458ca3cb7e15aa49595e894d49cd3513232efd2cd599`, full screen 1672 `881080aaa8cc6302dab0208d3287651aa689e3cadb319b65b68eeafa70dc5122`, full screen 1280 `e7eb81535e7311c8967a7a2406376f20e4d8666194b319c76033971809175b11`, bottom deck `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`, right panel `417dc27fb77b5f7dd633cab0ca3e377dc82894c17bee9b4bc6676d841a6ff79f`.
- Director verdict:
  - Accepted as a forward compositor hierarchy improvement, not final director-grade closure.
  - Positive: room atmosphere and aperture now live below actors/loot/telegraphs, removing the post-actor floor glow compensation path and making gameplay markers read cleaner.
  - Positive: the current map crop has a stronger pre-actor room mass and less post-render tile glow than the previous action-deck pass.
  - Still blocking: the visible room remains more grid-forward than `UI/UI-demo-new.png`; wall crown/side thickness and floor variation still do not reach the reference's authored-room material quality.
- Remaining risk:
  - This pass improves compositor ownership and marker layering more than it solves the asset-level tile repetition.
  - The comparison still lacks dedicated director telegraph/combat runtime evidence beyond the map crop.
  - Golden baselines, packaged whitebox and `verifyChanged` remain unrun for this runtime compositor iteration.
- Next action:
  - Re-read this goal document and this log before continuing. If the next crop still reads grid-first after this compositor hierarchy change, escalate back to the resource candidate loop for floor/wall variant structure or a new room-scale decal/AO resource strategy rather than adding more renderer micro-rectangles.

## 2026-05-28 - PR-08 Floor/Wall Variant-Structure Resource Sprint

- Loop type: resource candidate loop / structure reclassification
- Target area: `tileset.ruins.ground_01` and `tileset.ruins.wall_01`
- Contract alignment:
  - Re-read the latest goal/log and followed the hard stop after two map
    compositor iterations still left the runtime map grid-forward.
  - Generated a new AG/AH/AI floor-wall family batch instead of continuing
    alpha, seam, bleed or micro-rectangle renderer tuning.
  - Kept schema, manifest keys, sheet-plan keys, runtime text ownership and
    golden expected hashes unchanged.
  - Treated the new resources as evidence-only candidates; official r02
    raw/contact/runtime resources were restored to the pre-experiment worktree
    hashes after runtime A/B.
- Changed files:
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/floor-wall-variant-structure-sprint/`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/floor-wall-variant-structure-sprint/pr08-floor-wall-variant-structure-source-ag-ah-ai.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/floor-wall-variant-structure-sprint/pr08-floor-wall-variant-structure-ag-ah-ai-contact-tiling-board.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/floor-wall-variant-structure-sprint/pr08-floor-wall-variant-structure-runtime-comparison-board.png`
  - `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/floor-wall-variant-structure-sprint/pr08-floor-wall-variant-structure-runtime-room-detail-zoom.png`
  - Runtime crop dirs: `runtime-ag/`, `runtime-ah/`, `runtime-ai/`
- Commands run:
  - Local image review of current room-aperture comparison board and current
    r02 contact sheet.
  - AI image generation for a three-row AG/AH/AI floor-wall source sheet.
  - `python3` PIL slicing, candidate sheet assembly, contact/tiling board
    generation and runtime comparison-board generation.
  - First runtime loop attempt failed before candidate mutation because
    `set -u` conflicted with SDKMAN shell variable assumptions.
  - `source "$HOME/.sdkman/bin/sdkman-init.sh"` and `sdk env`.
  - For each of AG, AH and AI, temporarily copied the candidate r02 sheet,
    ran `python3 scripts/slice_spritesheet.py --overwrite`,
    `python3 scripts/render_contact_sheet.py --overwrite`, then ran
    `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks`.
  - `shasum -a 256` for the generated source/contact/runtime comparison
    boards.
  - SHA-256 restore check for official r02 raw/contact/runtime tile files
    against the pre-experiment backup.
- Result:
  - GENERATED: AG conservative, AH material-depth and AI room-space-strong
    floor/wall families with 4x tiling previews.
  - EXPECTED FAIL: focused golden screenshot hash gate for all three runtime
    A/B runs because resources changed temporarily and baselines remain
    unchanged.
  - AG actual hashes: `ui-demo-new-parity-1672x941=9d45f5bfa14b8ac18dea23e130c330081f7968f41eb74e5fc2ee14d82910b975`, `ui-demo-new-parity-1280x800=14920f40789357a72d89e83099f281d3a455e51187968bd6c5e1557924ecf7ea`, `ui-demo-new-map-stage-crop=08045b6ec378969515b465019cdf58eb8f6d6508bf4e6a1f5af0ee9dd15690d9`.
  - AH actual hashes: `ui-demo-new-parity-1672x941=f1079100ffcf6f2cceb1d9f41adcd52057e0e80128d80ea4c10d6c2a9e01f9c6`, `ui-demo-new-parity-1280x800=b42a4176736053052f49a11abc1eb35c40bc02d05e93030284d612bb9b915afc`, `ui-demo-new-map-stage-crop=8fcd7cae0e6177575d533f54ee81b3ed998ac359c2401d6a741478b930d9b11a`.
  - AI actual hashes: `ui-demo-new-parity-1672x941=5d3c0c0becbd29a139f6085d9f8d22baef091de6b09bab4c29f85e89d6523851`, `ui-demo-new-parity-1280x800=11b25c95255d149fcd652ef2ed3c12780934c4ad5093bc57642eb405b53dc96a`, `ui-demo-new-map-stage-crop=2c2de013409f35875acc453062d98f58c3ff5dc5f03aec0a2b83b2e810245061`.
  - Evidence hashes: source `7dea07f96cb5cee168b13002d5eb7c15c6aea8a47274b0e9a3bbc202c06fad5a`, contact/tiling board `1c90149c9784e9e35b85145e034d6a7f0fe7b0f601370c67be5d4fcdd60dd2f4`, runtime comparison board `1822613249d9442b8fe1ae7698207e41630737953bda3e630572046f06cab310`, room-detail zoom `6bb6996fc0475ea3becaed31148068abc507aca18637a419291ad8a1ab07b47b`.
  - RESTORED: official r02 raw/contact/runtime tile files match the
    pre-experiment worktree hashes after the A/B loop.
- Director verdict:
  - Reject AG/AH/AI for direct single-tile integration.
  - AH has the strongest material detail, but the runtime zoom makes the same
    large crack/slab motif repeat every cell, which worsens the grid-first
    read.
  - AG and AI are safer/darker but do not visibly beat the current
    room-aperture crop in first-read quality.
  - The blocker is now classified as `single-cell-resource-structure`: a better
    single 128px floor tile cannot carry the reference's authored room-space
    quality by itself.
- Remaining risk:
  - No formal resource gate was run because no new candidate was integrated
    into production resources.
  - The runtime crops still lack a dedicated PR-08 director telegraph/combat
    crop.
  - The next structure-level path may require either a compositor resource
    strategy or an explicit variant/autotile contract decision.
- Next action:
  - Do not generate another single floor/wall tile batch with the same
    technique.
  - Try deterministic room-scale AO/decal composition first, using map seed and
    world coordinates and preserving marker readability. If that still leaves
    the map grid-first, stop local patching and prepare a minimal
    variant/autotile contract proposal before mutating manifests or keys.

## 2026-05-28 - PR-08 Room-Scale Material-Field Runtime Pass

- Loop type: runtime compositor loop / technique-change follow-up
- Target area: visible room material hierarchy after AG/AH/AI proved the
  single-tile floor/wall replacement path insufficient.
- Contract alignment:
  - Followed the updated goal decision to try deterministic room-scale
    AO/decal-style composition before requesting new variant/autotile keys.
  - Changed `client` runtime compositor behavior and renderer tests only.
  - Did not change schema, manifest keys, sprite sheet plan, resources, text
    ownership or golden baselines.
  - Added a stable world-coordinate/player-position keyed material field and
    reduced small debris/pit/cut density so tests no longer reward micro-detail
    as the main quality strategy.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-scale-material-fields/`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-scale-material-fields/pr08-room-scale-material-fields-comparison-board.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-scale-material-fields/pr08-room-scale-material-fields-room-detail-zoom.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-scale-material-fields/pr08-room-scale-material-fields-ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-scale-material-fields/pr08-room-scale-material-fields-ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-scale-material-fields/pr08-room-scale-material-fields-ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-scale-material-fields/pr08-room-scale-material-fields-ui-demo-new-right-panel-grid.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-scale-material-fields/pr08-room-scale-material-fields-ui-demo-new-bottom-deck-no-command-hints.png`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps authored tile art clear beneath atmosphere washes" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas adds localized grime and broken stone detail to visible room floor"`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps authored tile art clear beneath atmosphere washes" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas adds localized grime and broken stone detail to visible room floor" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas lays heavy broken slab caps across visible floor lattice"`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks`
  - `python3` PIL copy/comparison-board generation for runtime evidence.
- Result:
  - PASS: focused renderer tests for authored tile clarity, localized grime
    density and heavy slab player-focus guard.
  - PASS: `TileRendererCanvasTest`, `DemoShellRendererTest`, and
    `maintainabilityLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because runtime visuals
    changed and baselines remain unchanged.
  - First full renderer run failed because the new material field reused the
    old heavy-slab `0.104` alpha semantic and tripped the player focal-center
    guard; fixed by assigning independent alpha tokens to the room-scale
    material field.
  - Focused golden actual hashes from the expected failure:
    `ui-demo-new-parity-1672x941=5fff0cb1824db92f99e88cd0de139e505cd75c10bbd65d7907657633ec1031c5`,
    `ui-demo-new-parity-1280x800=7ab29150c26410703e8a827cefd3c762ee518cce4b651f2aaefba1f378e5d54d`,
    `ui-demo-new-right-panel-grid=b96f246cdb9ffb8a2af026d856f8efaaf5911a46ff0bde56d8693ab2451f0ae8`,
    `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`,
    `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`,
    `ui-demo-new-map-stage-crop=58ef776f71fa68554c24b82b59e0174805b25abb93f6c62c7f5259e74b5e0ab8`,
    `ui-demo-new-inventory-page-1=2be674b1b1cee578f3e38baaeb57b5d8affed53074e51ef233039c8b419172b8`,
    `ui-demo-new-inventory-page-2=b859958fb6082bb610ae43cb4103e47fe63de43b45f612e294c4d275f15f53e7`.
  - Evidence file hashes: map crop
    `7076f3879314516a6b1464172f36010bbac22e599595650be3567478f2bdf881`,
    full screen 1672
    `8a26ccb42e839ad6e726f384e441b3540b82a02749c50128a62b7a615fe67e52`,
    full screen 1280
    `69d5f6d1830513eadd57ec637495aa4b47386aed0d451ae63a17a9dbf92acc73`,
    bottom deck
    `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`,
    right panel
    `417dc27fb77b5f7dd633cab0ca3e377dc82894c17bee9b4bc6676d841a6ff79f`.
- Director verdict:
  - Accepted only as a narrow forward compositor structure improvement.
  - Positive: room-scale material fields now exist as a distinct pre-actor
    hierarchy layer, and small floor debris is reduced to secondary accents
    instead of becoming the main quality path.
  - Positive: gameplay markers, right panel and bottom deck readability are not
    visibly regressed in the focused crops.
  - Still blocking: the map still reads as a regular grid before it reads as an
    authored room space; this does not reach the `UI/UI-demo-new.png`
    reference quality.
- Remaining risk:
  - This confirms that compositor-only work can improve hierarchy but is not
    enough to solve the repeated floor-cell structure.
  - The runtime evidence still lacks a dedicated PR-08 director
    telegraph/combat crop.
  - Golden baselines, packaged whitebox and `verifyChanged` remain unrun for
    this iteration.
- Next action:
  - Stop compositor-only iteration for the same map blocker.
  - Prepare a minimal floor variant/autotile contract proposal for PR-08 before
    mutating manifest keys or sheet-plan ownership. The proposal should cover
    owner rows, sheet-plan impact, runtime resolver impact, fallback/rollback
    and the focused validation path.

## 2026-05-28 - PR-08 Floor Variant/Autotile Contract Proposal

- Loop type: contract/design loop
- Target area: `tileset.ruins.ground_01` floor structure and runtime terrain
  visual selection
- Contract alignment:
  - Re-read the updated goal document and latest iteration log before
    continuing.
  - Re-read the PR-08 evidence/direction brief and resource authority section
    of `docs/rule/ai-change-governance.md`.
  - Followed the goal stop condition: no further compositor-only pass and no
    manifest/key mutation before an explicit structure contract exists.
  - Did not change schema, sheet-plan cells, key-registry entries, manifests,
    runtime resources, production Kotlin or golden baselines in this loop.
- Changed files:
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/pr08-floor-variant-autotile-contract-proposal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Evidence inspected:
  - `UI/UI-demo-new.png`
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-scale-material-fields/pr08-room-scale-material-fields-ui-demo-new-map-stage-crop.png`
  - `UI/sprite-sheets/sheet-plan.yaml` `r02-ui-demo-ruins-tiles`
  - `UI/sprite-sheets/key-registry.yaml` PR-08 floor/wall entries
  - `UI/sprite-sheets/owner-contracts/pr08-owner-keys.yaml`
  - `assets-src/image/manifests/phase2-visual-manifest.json` ruins floor/wall
    entries
  - `VisualManifestResolver`, `ClientAssetLoadStrategy`, `TileRenderModelBuilder`
    and `TileLayerComposer`
- Commands run:
  - `sed` reads for goal/log, docs index, PR-08 direction, resource authority,
    sheet-plan, key-registry, owner contract, manifest and runtime code paths.
  - `rg` impact searches for ruins terrain keys, resolver paths, sheet-plan,
    key-registry, manifest and validation tasks.
  - Local image review of the reference full screen and the latest
    room-scale material-field map crop.
- Result:
  - PROPOSED: a four-entry floor variant family where
    `tileset.ruins.ground_01` remains the base/fallback and three sibling exact
    keys carry authored floor variation.
  - PROPOSED: use existing manifest `tags` to declare
    `terrain_variant_family:*` and `terrain_variant_index:*`, avoiding a new
    manifest schema while keeping the family manifest-owned.
  - PROPOSED: `VisualManifestResolver` should build the variant index from exact
    manifest entries, `ClientAssetLoadStrategy` should preload expanded terrain
    family keys, and `TileRenderModelBuilder` should use the existing
    deterministic coordinate-keyed material variant as the selector.
  - REJECTED: another single-tile reroll, renderer alpha/seam/micro-detail as a
    main path, production Kotlin owner-key mirrors, full wall/floor autotile
    schema, and changing `FoundationGameSession.terrainVisualKey` to emit
    variants in the first slice.
- Director verdict:
  - Accepted as the next structure-level route to test, not as visual closure.
  - This creates a bounded resource contract for moving the map from grid-first
    toward room-first without inventing a second resource authority.
- Remaining risk:
  - The proposal still needs actual generated floor variants, contact sheet and
    runtime A/B proof.
  - Manifest key additions are a resource/public contract change and must stay
    inside the proposed Slice A boundary.
  - Wall crown/side/corner language remains open if floor variants alone do not
    solve the map-stage room read.
- Next action:
  - If the contract is accepted, execute Slice A from the proposal: generate the
    four floor variants, add only the proposed sheet-plan/key-registry/owner
    contract/canonical manifest rows, sync runtime manifests, then run contact
    sheet and identical focused runtime crop A/B before touching right panel or
    bottom deck chrome.

## 2026-05-28 - PR-08 Floor Variant Runtime A/B U04

- Loop type: resource candidate + runtime resolver implementation loop
- Target area: `tileset.ruins.ground_01` floor family and map-stage first read
- Contract alignment:
  - Followed the updated goal document and the floor variant/autotile proposal.
  - Kept `core` / `game` terrain snapshot semantics unchanged.
  - Added no manifest schema, atlas schema, region schema, save field, replay
    field or production Kotlin owner-key mirror.
  - Kept generated floor assets text-free and runtime-owned labels/hotkeys
    outside images.
  - Did not update golden baselines.
- Candidate summary:
  - U01 proved the family route but became too blurry after tone matching.
  - U02 carried stronger worn-stone material but produced a bright/dark
    checkerboard rhythm in mixed previews.
  - U03 improved tonal uniformity but lost material depth and read flat.
  - U04 normalized U02 low-frequency tone and was selected for runtime A/B.
- Changed files:
  - `UI/sprite-sheets/sheet-plan.yaml`
  - `UI/sprite-sheets/key-registry.yaml`
  - `UI/sprite-sheets/owner-contracts/pr08-owner-keys.yaml`
  - `assets-src/image/manifests/phase2-visual-manifest.json`
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_1.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_2.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_3.png`
  - `client/src/main/kotlin/com/ktome/client/assets/ManifestResolvers.kt`
  - `client/src/main/kotlin/com/ktome/client/assets/ClientAssetLoadStrategy.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
  - `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRenderModelTerrainVariantTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/floor-variant-candidate-u04/pr08-floor-variant-u04-contact-tiling-board.png`
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/floor-variant-candidate-u04/pr08-floor-variant-u04-current-vs-mixed-8x-board.png`
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/floor-variant-candidate-u04/pr08-floor-variant-u04-coordinate-hash-current-vs-mixed-8x-board.png`
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/floor-variant-candidate-u04/pr08-floor-variant-u04-coordinate-hash-synthetic-room-preview.png`
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/runtime-u04/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/runtime-u04/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/runtime-u04/ui-demo-new-right-panel-grid.png`
  - `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/runtime-u04/ui-demo-new-bottom-deck-no-command-hints.png`
- Commands run:
  - `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `./gradlew syncPhase2Manifests`
  - `./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - Image review of `UI/UI-demo-new.png` against the U04 runtime map, full,
    right-panel and bottom-deck crops.
- Result:
  - PASS: slicing and contact-sheet generation completed for the updated r02
    sheet.
  - PASS: manifest sync generated runtime visual manifest entries for the base
    floor and three U04 variants.
  - PASS: key registry, sheet-plan, sprite-map, asset, style, manifest and
    resource-pipeline lints.
  - PASS: focused resolver, preload, render-model, canvas and demo shell tests,
    plus `maintainabilityLint`.
  - EXPECTED FAIL: focused PR-02-1 golden hash gate failed because runtime
    visuals changed and baselines remain intentionally unchanged.
  - NOTE: two initial attempts through `:client:test --tests ...Golden...`
    failed with "No tests found" because golden screenshot tests are excluded
    from the default test task and must run through `:client:goldenScreenshot`.
- Focused golden actual hashes from the expected failure:
  - `ui-demo-new-parity-1672x941=1c5e03fb9426c57a65e87dc2ebfd245fb9258dac537a230c6a270b4b70316924`
  - `ui-demo-new-parity-1280x800=0f2a621a104231cb04dd397f2d3894a1468625b8ac9efebea27115a663e70d4e`
  - `ui-demo-new-right-panel-grid=b96f246cdb9ffb8a2af026d856f8efaaf5911a46ff0bde56d8693ab2451f0ae8`
  - `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=9432b6600a0e683dd677093786966aa4831a51f8d850167e893cf126d849562f`
  - `ui-demo-new-inventory-page-1=2be674b1b1cee578f3e38baaeb57b5d8affed53074e51ef233039c8b419172b8`
  - `ui-demo-new-inventory-page-2=b859958fb6082bb610ae43cb4103e47fe63de43b45f612e294c4d275f15f53e7`
- Archived runtime evidence hashes:
  - map crop
    `b50420c29c52e21eb3d181b81e39141a3a42395bd3406aa1929ad2bffb08d144`
  - full screen 1280
    `3ee449e370c8ed3f09aa4cb622198f4af265d4b499efd037acda8dd95e2df968`
  - right panel
    `417dc27fb77b5f7dd633cab0ca3e377dc82894c17bee9b4bc6676d841a6ff79f`
  - bottom deck
    `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`
- Director verdict:
  - Engineering accepted: the manifest-owned terrain variant family is a
    bounded, deterministic resource structure that does not violate snapshot or
    resource authority boundaries.
  - Visual not accepted as final: U04 improves material variation and reduces
    single-tile cadence, but the runtime crop still reads as a regular grid
    before it reads as an authored room space.
  - Blocking visual causes: always-on lattice/contact hierarchy remains too
    authoritative, and wall crown/side/corner/door-contact thickness language
    is still weaker than the reference.
  - Right panel and bottom deck remain visibly lower quality than the reference
    chrome family, but they should not become the main loop until the map-stage
    room read stops dominating the first impression.
- Next action:
  - Stop floor-variant micro iteration for U04.
  - Create the next explicit map-space contract around wall adjacency/crown and
    grid-layer ownership, then run an identical runtime crop before switching
    to right panel / bottom deck chrome.

## 2026-05-28 - PR-08 Wall Adjacency And Grid-Layer Contract Proposal

- Loop type: contract/design loop
- Target area: map-stage room-space hierarchy after U04 runtime A/B
- Contract alignment:
  - Uses the U04 runtime verdict as the current stop condition.
  - Does not mutate runtime resources, production Kotlin, manifests or golden
    baselines in this proposal step.
  - Keeps next implementation scoped to `client` rendering/resource ownership
    and away from `core` / `game` terrain semantics.
- Changed files:
  - `UI/review/dark-uiux-pr08-exploration/wall-adjacency-grid-layer-contract/pr08-wall-adjacency-grid-layer-contract-proposal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Result:
  - PROPOSED: split the map read into room material, room enclosure and tactical
    affordance layers.
  - PROPOSED: treat wall crown, side shadow, corner and door-contact thickness
    as the always-visible room boundary authority.
  - PROPOSED: demote any always-on full-room floor grid authority into either
    subtle material seam language or interaction-only affordance.
  - REJECTED: continuing U04 through small alpha, seam, bleed or rectangle
    tweaks.
- Next action:
  - Execute Slice A of the proposal: inventory current renderer grid/contact
    calls, classify them by layer ownership, add guard tests, then run the same
    U04 runtime crop set for a direct before/after comparison.

## 2026-05-28 - PR-08 Wall/Grid Slice A Room Compositor Runtime A/B

- Loop type: runtime compositor hierarchy loop
- Target area: wall adjacency/grid-layer ownership after U04 floor variant A/B
- Contract alignment:
  - Followed the updated goal document and wall adjacency/grid-layer contract.
  - Kept `core` / `game` terrain semantics unchanged.
  - Added no manifest schema, atlas schema, region schema, save field, replay
    field or production Kotlin owner-key mirror.
  - Did not update golden baselines.
  - Treated this as a compositor hierarchy experiment, not as another floor
    alpha, seam, bleed or micro-rectangle pass.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/wall-adjacency-grid-layer-contract/pr08-wall-adjacency-grid-layer-contract-proposal.md`
  - `UI/review/dark-uiux-pr08-exploration/wall-adjacency-grid-layer-contract/runtime-slice-a-room-compositor/`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Runtime change:
  - Renamed the late warm map overlay flush to `MAP_ROOM_COMPOSITOR`.
  - Moved room compositor work inside `TileMapRenderer.render(...)`, after
    terrain/cell material and before prop, actor, loot, cursor, targeting,
    telegraph and combat-feedback layers.
  - Removed the default `drawVisibleRoomSeamSoftener` call/path so the room
    compositor does not keep rebuilding a full-room lattice after floor variant
    resources are already authoritative.
  - Added focused canvas coverage that locks the compositor below actors, loot
    and tactical affordances.
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - Runtime crop archival from `client/build/reports/golden/dark-uiux-pr02-1/`
    into `UI/review/dark-uiux-pr08-exploration/wall-adjacency-grid-layer-contract/runtime-slice-a-room-compositor/`
  - Local image review of `UI/UI-demo-new.png`, the U04 map crop and the Slice A
    runtime map/full/right-panel/bottom-deck crops.
- Result:
  - PASS: focused `TileRendererCanvasTest` and `DemoShellRendererTest`.
  - PASS: `maintainabilityLint`.
  - EXPECTED FAIL: focused PR-02-1 golden hash gate failed because runtime
    visuals changed and baselines remain intentionally unchanged.
- Focused golden actual hashes from the expected failure:
  - `ui-demo-new-parity-1672x941=088bb1f47a273c249cdf8a5563cf4d858e757a8aa5015a7c5d6c53898ced9fc5`
  - `ui-demo-new-parity-1280x800=af71f051be6bf32f876f2db1a092268190620d183f930dbfe1bb4cb1d7e55fb6`
  - `ui-demo-new-right-panel-grid=b96f246cdb9ffb8a2af026d856f8efaaf5911a46ff0bde56d8693ab2451f0ae8`
  - `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=d9278908c108bb494bda494b01bbcebf655c0e5cb0519f5d1209b51131e64cc4`
  - `ui-demo-new-inventory-page-1=2be674b1b1cee578f3e38baaeb57b5d8affed53074e51ef233039c8b419172b8`
  - `ui-demo-new-inventory-page-2=b859958fb6082bb610ae43cb4103e47fe63de43b45f612e294c4d275f15f53e7`
- Archived runtime evidence hashes:
  - map crop
    `7035e57ee5d3e9527a3836961cc8bdfa9074391cf55f7fa96fb34e0f3d4e9caf`
  - full screen 1280
    `a71111f1d39ea3045f6f1ff8b2d5d4c174cc4280d9ededf2e9de78ec1926fc6a`
  - right panel
    `417dc27fb77b5f7dd633cab0ca3e377dc82894c17bee9b4bc6676d841a6ff79f`
  - bottom deck
    `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`
- Director verdict:
  - Engineering accepted: the compositor hierarchy is now closer to the
    intended ownership model. Room material/compositor work sits below actors,
    loot and tactical affordances, so marker readability is better protected.
  - Visual not accepted as final: Slice A reduces top-painted haze and improves
    gameplay marker clarity, but the map crop still reads as a regular grid
    before it reads as an authored room space.
  - Blocking visual causes: wall crown, side shadow, corner and door-contact
    thickness still do not carry enough room enclosure authority; floor lattice
    remains too dominant relative to `UI/UI-demo-new.png`.
  - Right panel and bottom deck remain lower-quality than the reference chrome
    family, but they should not become the primary loop until the map-stage
    room read is no longer the strongest first-impression blocker.
- Next action:
  - Stop compositor-only iteration for this map blocker.
  - Escalate to a formal wall resource family/autotile/crown resource decision
    before touching right panel or bottom deck chrome.
  - The next runtime candidate must prove wall crown/contact thickness as room
    enclosure authority, not just hide grid lines with more overlay paint.

## 2026-05-28 - PR-08 Wall Resource Family Direction Sprint W01

- Loop type: visual exploration + wall resource structure decision loop
- Target area: `tileset.ruins.wall_01` crown/contact resource family after
  wall-grid Slice A
- Contract alignment:
  - Followed the updated goal document stop condition: no further
    compositor-only pass for the map blocker.
  - Kept this pass under `UI/review/...` evidence only.
  - Did not mutate production runtime resources, sheet-plan, key-registry,
    owner contract, canonical manifest, runtime manifest or Kotlin code.
  - Added no manifest schema, atlas schema, region schema, save field, replay
    field or production owner-key mirror.
  - Kept generated resource candidates text-free; contact/preview boards may
    include evidence labels because they are not runtime resources.
- Visual thesis:
  - Wall resources must carry room enclosure with thick carved stone crown,
    side occlusion, corner mass and doorway/contact shadows.
  - The floor grid should become secondary material rhythm, not the room
    boundary authority.
- Generated candidate set:
  - Source board:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/generated-wall-family-candidates/pr08-wall-family-imagegen-source-board.png`
  - Contact board:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/generated-wall-family-candidates/pr08-wall-family-candidate-contact-board.png`
  - Runtime-size room preview:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/generated-wall-family-candidates/pr08-wall-family-candidate-runtime-room-preview.png`
  - Slices:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/generated-wall-family-candidates/slices/`
  - Repro script:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/generated-wall-family-candidates/build_wall_candidate_previews.py`
  - Decision doc:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/pr08-wall-resource-family-decision.md`
- Commands run:
  - Image generation through the built-in image generation tool for one 3x5
    wall-family source board.
  - `python3 UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/generated-wall-family-candidates/build_wall_candidate_previews.py`
  - `shasum -a 256` for the source board, contact board, room preview and
    generated slices.
  - Local image review of `UI/UI-demo-new.png`, current `tileset_ruins_wall_01`,
    Slice A map crop, the generated source board, contact board and room
    preview.
- Evidence hashes:
  - source board
    `1ddbe93b2aea1c5b4d7c10682486fdcf452c005b13c9c28a66dc98beef88df3e`
  - contact board
    `2ab97a33698547635d4d9a112aa510917fa47d99f79594a9616705ff2905a98a`
  - runtime room preview
    `9fdf0e3f5ed60b3ad759d5cc73fb9df1c2d47ab808205e9c59049553e314a41f`
- Candidate verdict:
  - `conservative`: direction-useful, but still too close to a repeated brick
    wall panel.
  - `material_depth`: best next prompt seed; it has the best balance of crown
    readability, chipped contact language and restrained contrast.
  - `strong_style`: too heavy as-is; its corners and doorway detail are likely
    to compete with actors and tactical markers at runtime size.
- Director verdict:
  - Accepted as a wall resource direction decision, not as runtime closure.
  - Rejected as production-ready slicing: the candidates retain uneven scale,
    composite wall objects and residual panel backgrounds.
  - Rejected another single `tileset.ruins.wall_01` replacement. The next wall
    mutation should use a five-piece family: `base`, `crown`, `side`, `corner`,
    `door_contact`.
- Proposed runtime contract for a future implementation slice:
  - exact keys:
    `tileset.ruins.wall_01`,
    `tileset.ruins.wall_01.crown`,
    `tileset.ruins.wall_01.side`,
    `tileset.ruins.wall_01.corner`,
    `tileset.ruins.wall_01.door_contact`
  - manifest tags:
    `terrain_wall_family:tileset.ruins.wall_01`,
    `terrain_wall_piece:base`,
    `terrain_wall_piece:crown`,
    `terrain_wall_piece:side`,
    `terrain_wall_piece:corner`,
    `terrain_wall_piece:door_contact`
  - no new manifest schema, atlas schema or `core` / `game` terrain snapshot
    change.
- Next action:
  - Regenerate a clean isolated five-cell wall-family candidate using
    `material_depth` as prompt seed.
  - The next candidate must remove panel backgrounds, avoid composite wall
    scenes inside a single cell, keep every cell square and text-free, and
    prove runtime-size room enclosure before sheet-plan/key-registry/manifest
    mutation.

## 2026-05-28 - PR-08 Wall Resource Family Clean Candidate W02

- Loop type: visual exploration + runtime prototype readiness decision loop
- Target area: clean five-piece wall family source candidate after W01 direction
  board
- Contract alignment:
  - Kept this pass under `UI/review/...` evidence only.
  - Did not mutate production runtime resources, sheet-plan, key-registry,
    owner contract, canonical manifest, runtime manifest or Kotlin code.
  - Added no manifest schema, atlas schema, region schema, save field, replay
    field or production owner-key mirror.
  - Continued the wall-family route instead of returning to compositor-only
    seam/alpha tuning.
- Generated candidate:
  - Source row:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/clean-wall-family-w02/pr08-wall-family-w02-imagegen-source-row.png`
  - Contact board:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/clean-wall-family-w02/pr08-wall-family-w02-contact-board.png`
  - Runtime-size room preview:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/clean-wall-family-w02/pr08-wall-family-w02-runtime-room-preview.png`
  - Slices:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/clean-wall-family-w02/slices/`
  - Repro script:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/clean-wall-family-w02/build_clean_wall_candidate_preview.py`
- Commands run:
  - Image generation through the built-in image generation tool for one clean
    five-piece wall-family row.
  - `python3 UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/clean-wall-family-w02/build_clean_wall_candidate_preview.py`
  - `shasum -a 256` for the source row, contact board, room preview and slices.
  - Local image review of the W02 source row, contact board and runtime-size
    room preview.
- Evidence hashes:
  - source row
    `d2f0df51e8542974b133759892e4e0da050fef02967fed738f91ec979e3e342d`
  - contact board
    `ebea9168b6d24b1ff30bac679552e292f9cb673e9c29c6548ed21e31b5ae59dd`
  - runtime room preview
    `0307b9d420310e09a377b7ac31f92fcf6905d708815d8ac9815155befb0bb692`
- Director verdict:
  - W02 improves over W01: it removes the gray panel-card background problem
    and reads more like a coherent wall-family source row.
  - W02 is accepted as a rough runtime A/B prototype seed because the room
    preview shows crown and side mass can own room enclosure more clearly than
    the current single wall tile.
  - W02 is not accepted as final director resource closure. The side, corner
    and doorway/contact pieces still need real runtime crop proof against
    actors, loot, cursor, target, telegraph and current darkness/light layers.
- Next action:
  - Proceed to a bounded runtime Slice B with W02 only through the canonical
    resource authority chain: sheet-plan, key-registry, PR-08 owner contract,
    canonical manifest, runtime manifest sync and focused renderer tests.
  - Keep PR-02-1 golden expected-fail and do not update baselines.
  - Reject W02 and regenerate W03 if the runtime crop shows wall side/corner or
    door-contact pieces competing with gameplay markers or restoring noisy wall
    pattern dominance.

## 2026-05-28 - PR-08 Wall Resource Family Runtime Slice B W02

- Loop type: resource candidate + runtime resolver/render-model implementation
  loop
- Target area: `tileset.ruins.wall_01` five-piece wall family and map-stage
  room enclosure first read
- Contract alignment:
  - Followed the W02 decision doc and the updated goal stop condition.
  - Kept `core` / `game` terrain snapshot semantics unchanged.
  - Added no manifest schema, atlas schema, region schema, save field, replay
    field or production Kotlin owner-key mirror.
  - Routed W02 through sheet-plan, key-registry, PR-08 owner contract,
    canonical manifest, runtime manifest sync, preload and focused tests.
  - Did not update golden baselines.
- Changed files:
  - `UI/sprite-sheets/sheet-plan.yaml`
  - `UI/sprite-sheets/key-registry.yaml`
  - `UI/sprite-sheets/owner-contracts/pr08-owner-keys.yaml`
  - `assets-src/image/manifests/phase2-visual-manifest.json`
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01_crown.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01_side.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01_corner.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01_door_contact.png`
  - `client/src/main/kotlin/com/ktome/client/assets/ManifestResolvers.kt`
  - `client/src/main/kotlin/com/ktome/client/assets/ClientAssetLoadStrategy.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
  - `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRenderModelTerrainVariantTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/`
- Runtime change:
  - Added exact wall-family keys:
    `tileset.ruins.wall_01.crown`,
    `tileset.ruins.wall_01.side`,
    `tileset.ruins.wall_01.corner`,
    `tileset.ruins.wall_01.door_contact`.
  - Added manifest-tagged wall family roles:
    `terrain_wall_family:tileset.ruins.wall_01` and
    `terrain_wall_piece:*`.
  - Added resolver support for `TerrainWallPieceRole` and manifest-owned wall
    family lookup.
  - Expanded session preload so wall-family resources are resident alongside
    floor variants.
  - Updated `TileRenderModelBuilder` to select wall family pieces from existing
    map adjacency without changing snapshot keys.
- Commands run:
  - `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `./gradlew syncPhase2Manifests`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - Runtime crop archival from `client/build/reports/golden/dark-uiux-pr02-1/`
    into `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/runtime-slice-b-w02/`
  - Local image review of `UI/UI-demo-new.png`, Slice A crop, W02 runtime crop
    and W02 comparison board.
- Result:
  - PASS: slicing and contact-sheet generation completed for updated r02 sheet.
  - PASS: manifest sync generated runtime wall-family manifest entries.
  - PASS: PR-08 filtered sprite-map report refreshed with the W02 wall-family
    runtime slices.
  - PASS: key registry, sheet-plan, sprite-map, asset, style, manifest and
    resource-pipeline lints.
  - PASS: focused manifest resolver, wall/floor render-model, canvas and demo
    shell tests, plus `maintainabilityLint`.
  - EXPECTED FAIL: focused PR-02-1 golden hash gate failed because runtime
    visuals changed and baselines remain intentionally unchanged.
  - NOTE: first focused test attempt failed because the new public resolver
    method exposed an internal enum; fixed by making `TerrainWallPieceRole` the
    explicit client asset-layer type.
  - NOTE: second focused test attempt failed due a malformed test fixture comma;
    fixed before the passing run.
- W02 runtime resource hashes:
  - `tileset_ruins_wall_01.png`
    `ed965f667ac4dfd425073320fe20eb6132accfbf98c9ebdd86a4fd95c40552b2`
  - `tileset_ruins_wall_01_crown.png`
    `9a00f2e8d9779f5937ff7ba452b5210169bcb96383b78fd371a05851c2b057c6`
  - `tileset_ruins_wall_01_side.png`
    `fbf6ade877d0565fb2e8328f11617e25d8129524ccb46535f93c2052c20bbac0`
  - `tileset_ruins_wall_01_corner.png`
    `a04e2311493fcba5c2f51e8122af00339a3e18ce11b20d20e2d0912528883363`
  - `tileset_ruins_wall_01_door_contact.png`
    `570c355f344a4f33215f7dc6e9c09a4a020e1d49b17bac1c6cc6fc7cbe5b57e8`
  - r02 contact sheet
    `f5754061e3f8d904880bf3a33caf5b31f4ed97deeb60ca8fcfda5fbe9f915455`
  - PR-08 filtered sprite-map report
    `9119015960be25a8e073aaf748d84bb5ce82e494d7cb9f46d01b40c629fec674`
- Focused golden actual hashes from the expected failure:
  - `ui-demo-new-parity-1672x941=533edeb14640537ec2e42983ff35f48a29fc5f01558569a1c604a0feff6a69e7`
  - `ui-demo-new-parity-1280x800=a61d0e4701df4eae6f9190bd564a751f9fad77cbd2964ff1829736cbf09815d3`
  - `ui-demo-new-right-panel-grid=b96f246cdb9ffb8a2af026d856f8efaaf5911a46ff0bde56d8693ab2451f0ae8`
  - `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=9b797eaebbd31d61312c0adbb064b789f47f4d5b43495c59368cf2ceb0c9629e`
  - `ui-demo-new-inventory-page-1=2be674b1b1cee578f3e38baaeb57b5d8affed53074e51ef233039c8b419172b8`
  - `ui-demo-new-inventory-page-2=b859958fb6082bb610ae43cb4103e47fe63de43b45f612e294c4d275f15f53e7`
- Archived runtime evidence hashes:
  - map crop
    `a6f1ec4d63772c6cc1e2bae6fbdbb98055b3f9925efbbd8c76ce13e868b7d2f6`
  - full screen 1280
    `e91a7f0361ca061e8d29ba603b1e66c9f64a0484d9cd45d0769df583d12d1bc5`
  - right panel
    `417dc27fb77b5f7dd633cab0ca3e377dc82894c17bee9b4bc6676d841a6ff79f`
  - bottom deck
    `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`
  - comparison board
    `06e5a2731e4dbd15ada7cd96440a2f6a3dd34a9c22129ec3644301d0008ce94a`
- Director verdict:
  - Engineering accepted: W02 proves a manifest-owned wall family can be routed
    through the project resource authority without touching `core` / `game`
    terrain snapshots.
  - Visual not accepted as final: wall mass and crown/contact language are more
    visible than Slice A, but the first read is still a regular floor grid and
    the single-orientation side/corner pieces repeat in the runtime crop.
  - Gameplay marker readability did not obviously regress in the focused crop,
    but wall pieces are not yet refined enough for director-grade closure.
  - Right panel and bottom deck remain unchanged; keep them deferred until the
    map-stage room read is no longer the dominant blocker.
- Next action:
  - Do not update golden baselines.
  - Generate W03 as an orientation-aware wall family, or add a narrow renderer
    orientation contract for wall pieces, before switching to right panel and
    bottom deck chrome.
  - The next map iteration must reduce the floor-grid first read while keeping
    W02's stronger wall enclosure benefits.

## 2026-05-28 - PR-08 Wall W03/W04/Slice D Baseline Catch-Up

- Loop type: resource candidate + runtime baseline catch-up
- Target area: wall family orientation/full-tile progression after W02
- Evidence:
  - W03 orientation candidate:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/orientation-wall-family-w03/`
  - W04 full-tile wall family:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/full-tile-wall-family-w04/`
  - current runtime baseline:
    `UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/runtime-slice-d-grid-authority/`
- Director verdict:
  - Accepted as current engineering/runtime baseline only.
  - Not accepted as director-grade visual closure: the map crop still reads
    coarse wall/floor lattice before it reads as a carved, authored room.
- Next action:
  - Keep Slice D as the comparison baseline for later crops.
  - Do not update golden baselines from Slice D.
  - Continue only with changes that reduce first-read lattice authority rather
    than adding more minor per-cell seam patches.

## 2026-05-28 - PR-08 Floor Edge Balance V05

- Loop type: resource candidate loop
- Target area: `tileset.ruins.ground_01` floor family edge darkness after Slice D
- Contract alignment:
  - Mutated only official floor resource content through the existing Dark UI
    sheet authority.
  - Added no manifest schema, atlas schema, region schema, save/replay field,
    `core` / `game` terrain snapshot change or production owner-key mirror.
  - Kept PR-02-1 golden as expected-fail; did not update baselines.
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01*.png`
  - `UI/review/dark-uiux-pr08-exploration/floor-edge-balance-v05/`
  - `UI/sprite-sheets/dark-v1-final-full-inventory.json`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
- Evidence:
  - before/after board:
    `UI/review/dark-uiux-pr08-exploration/floor-edge-balance-v05/pr08-floor-edge-balanced-v05-before-after-board.png`
  - metrics:
    `UI/review/dark-uiux-pr08-exploration/floor-edge-balance-v05/pr08-floor-edge-balanced-v05-metrics.json`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/floor-edge-balance-v05/runtime-v05/`
- Metrics:
  - base delta improved from about `-10.165` to `-2.413`
  - variant 1 delta improved from about `-6.157` to `-1.989`
  - variant 2 delta improved from about `-7.636` to `-1.596`
  - variant 3 delta improved from about `-7.035` to `-1.623`
- Commands run:
  - `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/generate_dark_final_full_inventory.py`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
- Result:
  - PASS: sprite slicing, contact-sheet generation and final full inventory.
  - PASS: resource lint chain and PR-08 filtered sprite-map report.
  - PASS: focused manifest/render/canvas/demo-shell tests plus
    `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because visuals changed and
    baselines remain intentionally unchanged.
- Focused golden actual hashes from the expected failure:
  - `ui-demo-new-parity-1672x941=db802fe8d63893be2fff4d48ac49a2d55faeeed307ab03fe27d6948787a3e59a`
  - `ui-demo-new-parity-1280x800=89dff046d704e9f6c5d849328550f2f62403ce0e4a39884b0a0552ec00df742c`
  - `ui-demo-new-map-stage-crop=eda37fa7b40d69bace932ebc7ebfd741f98dacc91d61b0ad9b9199a30a468bf2`
- Director verdict:
  - Accepted as source-resource cleanup and diagnostic evidence.
  - Not accepted as meaningful runtime visual movement: the V05 crop remains
    nearly indistinguishable from Slice D at first read.
- Next action:
  - Do not continue with more minor edge-only floor polish.
  - Identify whether the remaining lattice is floor-internal, tile-boundary or
    wall/grid authority before mutating another resource.

## 2026-05-28 - PR-08 Floor Lattice Suppression V06

- Loop type: resource candidate + narrow runtime rendering contract loop
- Target area: floor-family repeated lattice plus terrain edge sampling
- Contract alignment:
  - Kept changes in `client` presentation/rendering and formal Dark UI resource
    authority.
  - Added no new manifest key, manifest schema, atlas schema, `core` / `game`
    terrain field, save/replay field or production resource-key mirror.
  - Did not update golden baselines.
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01*.png`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/`
  - `UI/sprite-sheets/dark-v1-final-full-inventory.json`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
- Runtime change:
  - draw `tile_ground` terrain before upper terrain within the existing
    `MAP_TERRAIN_BASE` layer.
  - widen `tile_ground` terrain bleed from `4f` to `14f`.
  - update the canvas test from the old 4f micro-bleed expectation to the new
    ground seam-suppression width.
- Candidate verdict:
  - rejected V06a/B/C: useful profile diagnostics but too close to V05
  - rejected V06f: too quiet, makes coarse lattice more dominant
  - accepted-forward V06e `regrain`: best balance of calmer floor cadence and
    retained stone texture
- Evidence:
  - decision record:
    `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/pr08-floor-lattice-suppression-v06-decision.md`
  - final runtime comparison:
    `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/pr08-floor-lattice-suppression-v06-final-runtime-comparison-board.png`
  - retained runtime crop:
    `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/runtime-v06e-ground-first-bleed14/ui-demo-new-map-stage-crop.png`
  - rejected V06f runtime crop:
    `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/runtime-v06f-ground-first-bleed14/ui-demo-new-map-stage-crop.png`
- Key hashes:
  - final runtime comparison board
    `a7b0ae6bb754aace0ad4d469f6530e01d5a223abc2df1b6e5db98a540c1fb09a`
  - V06e runtime map crop
    `9226eb319df13a39a778e364587b81b849fd1fa97827260d01cf782d35395282`
  - V06e runtime full screen
    `ce6bee00a4c79646d7cf246d404a518bda97077b91c80924d921ea62a65857f6`
  - retained floor resource hashes:
    `70a193734f89cf3e8eeccf9c4c967e93c11136794ef961a4bfcea841d71e4787`,
    `ff91f17f3b3ce968c15040e3629b6cd7f7d6e7ff2a25aa8abab8916bec6e68cc`,
    `785095016111209d8f64a92bc26ec08ffb6e77d5fe0f47f9efdac1997da43df1`,
    `68cd9ef8f8fe775678ce538a60e7915c645e70688a0435de933ab3218173b309`
- Commands run:
  - V06 candidate generation with local PIL scripts under the evidence folder.
  - `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/generate_dark_final_full_inventory.py`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
- Result:
  - PASS: sprite slicing, contact-sheet generation and final full inventory.
  - PASS: resource lint chain and PR-08 filtered sprite-map report.
  - PASS: focused manifest/render/canvas/demo-shell tests plus
    `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
    changed and baselines remain intentionally unchanged.
- Focused golden actual hashes from the retained V06e expected failure:
  - `ui-demo-new-parity-1672x941=822a61abcf0255ada9e21c8988ec0b51a1769a98b6757b0e127dbb797d89b960`
  - `ui-demo-new-parity-1280x800=0d85e530498f03ba92277e717a7374dc7932ca3f29c0fcf63a67bf70acfa7e08`
  - `ui-demo-new-map-stage-crop=474f86b8f6739cea4ee9ef224fef38491c4901eaa3ccc7983a8c157f806f5251`
- Director verdict:
  - V06e + ground-first bleed14 is accepted-forward, not final.
  - It improves floor material calmness over V05 without losing all texture.
  - It does not solve first-read quality: wall/grid/contact lattice still
    dominates the map crop relative to `UI/UI-demo-new.png`.
- Next action:
  - Do not update golden baselines.
  - Do not run another floor-only resource polish unless a new technique is
    explicitly chosen.
  - Next useful cut should address wall/grid interaction or a stronger room
    enclosure/lattice authority change while preserving marker readability.

## 2026-05-28 - PR-08 Room Enclosure / Wall Lattice V07

- Loop type: visual exploration + resource candidate loop
- Target area: wall/grid interaction after retained V06e floor polish
- Contract alignment:
  - Kept retained changes in Dark UI resource authority.
  - Added no manifest key, manifest schema, atlas schema, save/replay/profile
    field, `core` / `game` terrain snapshot field or production owner-key
    mirror.
  - Rejected and reverted renderer-side wall bleed before retaining W05i.
  - Did not update golden baselines.
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01*.png`
  - `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/`
  - `UI/sprite-sheets/dark-v1-final-full-inventory.json`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
- Candidate verdict:
  - rejected V07a wall terrain bleed `5f`: wall contact became stronger, but
    repeated wall-piece vertical seams became more obvious.
  - rejected V07 rectangular center/collar prototypes: they made the
    non-rectangular room read like blocky fog.
  - rejected W05a-c: edge + mass bands produced visible black resource bands in
    tiling preview.
  - rejected and removed W05d-f: local generation bug wrote translucent pixels
    into opaque wall candidates.
  - accepted-forward W05i `lr_strong_opaque`: best current wall edge
    unification without alpha pollution or black-frame artifacts.
- Evidence:
  - decision record:
    `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/pr08-room-enclosure-lattice-v07-decision.md`
  - retained runtime comparison:
    `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/pr08-room-enclosure-lattice-w05i-runtime-comparison-board.png`
  - retained runtime archive:
    `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/runtime-w05i-wall-edge-unify/`
  - rejected wall-bleed runtime archive:
    `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/runtime-v07a-wall-bleed5-rejected/`
- Key hashes:
  - W05i runtime comparison board
    `ca9c2d6552af923a09526c0d6013ac0233a633cbb7b70b9ffd69afa04685482a`
  - W05i runtime map crop artifact
    `c89d6142f7796fc1a9fab4d655e5868a08af18433a27525417cc6b8b9179b4ab`
  - W05i wall resource hashes:
    `52081fed3a1478343e1f1e5a42e0c8cb113caa28e350c6a27d9afe8735f76212`,
    `d4edb9a81c8c3917ed7a2ca99d07646e375a1572c0ff7990378f7584a60496d7`,
    `6893df8dc013e92e834a4161959223576e9ae92dd9db0fc231252959a2d4a83a`,
    `3cc0e3f3c1e0da7dc5d28f58d8138fe2f5a5863bdc0873c2870fcbe0318cd365`,
    `abc31dca258d47a9932941e5a41346949a4be92c00b6425d949a78b00aa7f430`
- Commands run:
  - W05 candidate generation with local PIL scripts under the evidence folder.
  - `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/generate_dark_final_full_inventory.py`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
- Result:
  - PASS: sprite slicing, contact-sheet generation and final full inventory.
  - PASS: resource lint chain and PR-08 filtered sprite-map report.
  - PASS: focused client tests plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
    changed and baselines remain intentionally unchanged.
- Focused golden actual hashes from the retained W05i expected failure:
  - `ui-demo-new-parity-1672x941=e523fadb506e92b0fc43f30658e7989665b8bd511abef1d33911b1ce7b2398c0`
  - `ui-demo-new-parity-1280x800=b796b84b515cc041bd9d5322fcfdfb0f5bfb1aa8c55824b9bbe0086018b3950d`
  - `ui-demo-new-map-stage-crop=0ad2681b3a07bda056083643637924f550cc9d21fb0d95143587374310ca7806`
- Director verdict:
  - W05i is accepted as narrow forward wall-resource polish, not final.
  - It does not close the first-read gap: the map crop remains grid-forward
    compared with `UI/UI-demo-new.png`.
  - The next useful cut should escalate to a stronger authored room-scale
    decal/AO resource contract or structural wall/floor interaction model,
    not another wall-edge-only or floor-edge-only polish.

## 2026-05-28 - PR-08 Authored Room-Scale Decal V08

- Loop type: visual exploration + blocker reclassification loop
- Target area: authored room-scale decal/AO viability after retained W05i wall
  resource polish
- Contract alignment:
  - Changed no production resource, manifest, schema, renderer, `core` or
    `game` file in this loop.
  - Added no manifest key, owner row, atlas schema, save/replay/profile field or
    terrain snapshot field.
  - Did not update golden baselines.
- Changed files:
  - `UI/review/dark-uiux-pr08-exploration/authored-room-scale-decal-v08/`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - rejected A `ao_collar`: lower edge score, but reads as flat overlay.
  - rejected B `broken_slab`: lowers lattice read slightly, but does not create
    believable authored floor/wall material.
  - rejected C `enclosure_light`: strongest metric reduction, but washes the
    room into a broad amber field and weakens material credibility.
  - accepted D `degrid_only` as diagnostic only: it preserves markers and shows
    that the dark repeated lattice is the active blocker.
  - rejected E/F: useful proof that lattice suppression moves the crop, but too
    uniform and too graphic for runtime production art.
- Evidence:
  - decision record:
    `UI/review/dark-uiux-pr08-exploration/authored-room-scale-decal-v08/pr08-authored-room-scale-decal-v08-decision.md`
  - initial comparison board:
    `UI/review/dark-uiux-pr08-exploration/authored-room-scale-decal-v08/pr08-room-scale-decal-v08-comparison-board.png`
  - degrid comparison board:
    `UI/review/dark-uiux-pr08-exploration/authored-room-scale-decal-v08/pr08-room-scale-decal-v08-degrid-comparison-board.png`
  - dark lattice mask:
    `UI/review/dark-uiux-pr08-exploration/authored-room-scale-decal-v08/pr08-room-scale-decal-v08-dark-lattice-mask.png`
- Key hashes:
  - initial comparison board:
    `2d87e91d76a40a3990335fd8e2bb2377bc4a92130c4807c00bfb295270182437`
  - degrid comparison board:
    `11ac11f8f98e4096897bc733596438324660633941641cb36781458b54f50a15`
  - dark lattice mask:
    `818651ead580f8d520b92bb706587fafecfeb2657a9fc6939743d51c62266013`
  - D diagnostic crop:
    `2aeedc7f96b8613d847e3d40817c82ed7084e3463bf7714b3cf64527244f1b8d`
- Edge-score summary:
  - W05i baseline: mean `6.992`, p90 `23`
  - A: mean `5.274`, p90 `16`
  - B: mean `5.615`, p90 `17`
  - C: mean `4.741`, p90 `14`
  - D: mean `6.238`, p90 `20`
  - E: mean `5.456`, p90 `17`
  - F: mean `5.588`, p90 `17`
- Commands run:
  - local Python/PIL generation for the V08 overlay, degrid mask, crop boards
    and metrics under the evidence folder.
- Result:
  - PASS: evidence images and metrics were produced.
  - PASS: V08 decision record was written.
  - NOT RUN: Gradle resource/client gates, because this loop made no production
    resource, manifest or Kotlin mutation.
- Director verdict:
  - V08 does not close PR-08.
  - Room-scale AO/decal paint is rejected as the immediate production direction
    unless it is backed by a stricter resource contract and materially better
    art.
  - The active blocker is now classified as `dark-lattice-authority`.
- Next action:
  - Do not integrate any V08 overlay as production art.
  - Do not run another floor-edge-only or wall-edge-only polish for this
    blocker.
  - Choose one explicit production contract next: a room-scale
    lattice-mask/material-breakup resource with owner coverage and focused
    renderer tests, or a structural wall/floor interaction model that removes
    the dark lattice at the authority boundary.

## 2026-05-28 - PR-08 Structural Lattice Suppression V09

- Loop type: production candidate + runtime rejection loop
- Target area: V08 `dark-lattice-authority` blocker
- Contract alignment:
  - Tested a renderer-side structural wall/floor interaction candidate without
    adding a manifest key, owner row, atlas schema, save/replay/profile field or
    `core` / `game` terrain snapshot field.
  - Reverted the transient Kotlin/test candidate after visual review rejected
    it.
  - Did not update golden baselines.
- Changed files retained:
  - `UI/review/dark-uiux-pr08-exploration/structural-lattice-suppression-v09/`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - rejected V09 runtime A: material-colored seam bridges compiled and passed
    focused renderer checks, but the map crop still reads grid-first.
  - rejected direction: renderer-side line bridges turn the dark lattice into a
    colder visible lattice instead of demoting it.
- Evidence:
  - decision record:
    `UI/review/dark-uiux-pr08-exploration/structural-lattice-suppression-v09/pr08-structural-lattice-suppression-v09-decision.md`
  - retained runtime crop:
    `UI/review/dark-uiux-pr08-exploration/structural-lattice-suppression-v09/runtime-slice-a-lattice-suppression/ui-demo-new-map-stage-crop.png`
  - runtime comparison board:
    `UI/review/dark-uiux-pr08-exploration/structural-lattice-suppression-v09/pr08-structural-lattice-suppression-v09-runtime-comparison-board.png`
  - metrics:
    `UI/review/dark-uiux-pr08-exploration/structural-lattice-suppression-v09/pr08-structural-lattice-suppression-v09-metrics.json`
- Key hashes:
  - V09 rejected runtime map crop:
    `622a75e88692171a7c422a7af2316443382268350dcdb782a00c4b15e0107578`
  - V09 runtime comparison board:
    `684aa59243ed07271bf348eaa8eafb8ae4b5053209ebc2ad7df7f1283afc10e3`
  - golden actual `ui-demo-new-map-stage-crop` from the transient candidate:
    `94895ffde57a43697448b39c2e297dc8e5a860caacc5a3e38a46aeef9cec6bd6`
- Commands run:
  - `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas lowers dark lattice authority without adding room fog wash'`
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - local Python/PIL generation for the V09 comparison board and metrics
- Result:
  - PASS: transient focused renderer test.
  - PASS: transient `TileRendererCanvasTest` plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
    changed and baselines remain intentionally unchanged.
  - REVERTED: V09 Kotlin/test production candidate after runtime crop review.
- Director verdict:
  - V09 is rejected.
  - It is useful negative evidence: renderer-side line bridges are not the
    production route to director-grade quality.
- Next action:
  - Do not continue renderer-side line-bridge suppression.
  - Next cut should remove or demote the repeated dark-line authority at source
    resource/family level, or define a non-grid-aligned room-scale
    material-breakup resource contract before production mutation.

## 2026-05-28 - PR-08 Source Resource De-Line V10

- Loop type: source resource candidate + runtime evidence loop
- Target area: V08/V09 `dark-lattice-authority` blocker at source
  floor/wall resource level
- Contract alignment:
  - Reused existing `r02-ui-demo-ruins-tiles` cells and visual keys.
  - Added no manifest key, owner row, atlas schema, save/replay/profile field,
    `core` / `game` terrain snapshot field or renderer authority.
  - Did not update golden baselines.
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01*.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01*.png`
  - `UI/sprite-sheets/dark-v1-final-full-inventory.json`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `UI/review/dark-uiux-pr08-exploration/source-resource-de-line-v10/`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - rejected V10a before runtime: contact board showed wall side/corner/door
    pieces turning into rectangular collage surfaces.
  - accepted-forward V10b `edge_lift`: source edge-lift/opacity normalization
    on existing floor/wall family, no new keys or schema.
  - V10b is not director-grade closure: runtime still reads grid-first.
- Evidence:
  - decision record:
    `UI/review/dark-uiux-pr08-exploration/source-resource-de-line-v10/pr08-source-resource-de-line-v10-decision.md`
  - V10a contact board:
    `UI/review/dark-uiux-pr08-exploration/source-resource-de-line-v10/pr08-source-resource-de-line-v10a-contact-board.png`
  - V10b contact board:
    `UI/review/dark-uiux-pr08-exploration/source-resource-de-line-v10/pr08-source-resource-de-line-v10b-contact-board.png`
  - V10b runtime comparison board:
    `UI/review/dark-uiux-pr08-exploration/source-resource-de-line-v10/pr08-source-resource-de-line-v10b-runtime-comparison-board.png`
  - retained V10b runtime crop:
    `UI/review/dark-uiux-pr08-exploration/source-resource-de-line-v10/runtime-v10b-edge-lift/ui-demo-new-map-stage-crop.png`
- Key hashes:
  - retained raw sheet:
    `8859e945db78e52ab184ab3305bfb014185e9d10103722160fa170953f01a9da`
  - retained contact sheet:
    `baa2d8359f8dcaebeb9573ebbd9423169dd4fc7da4a10bc07d13aa2de6cf5a17`
  - V10b runtime crop artifact:
    `995128e604a2c6433087c34dd61da4435cf94279a29bf4c7b78b169df49f96c2`
  - V10b runtime comparison board:
    `608976182e3e290883a6c1e9ecda8d151eab493ecabd54cf70501d7e15949c8d`
- Focused golden actual hashes from retained V10b expected failure:
  - `ui-demo-new-parity-1672x941=be6251ab76a189cee18b8e1b1858b627aea3f8b87687fc66132038580de7b04c`
  - `ui-demo-new-parity-1280x800=92e29e4eaa5271b83bc1c542d1495e4b7a94b3be6149a498762efb58f68711cb`
  - `ui-demo-new-map-stage-crop=51ad2d55186156e6296432235a8a4fc76a65db14a9f3ebd8a4393fb37452dbd6`
- Commands run:
  - local Python/PIL V10a/V10b generation under the evidence folder.
  - `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/generate_dark_final_full_inventory.py`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
- Result:
  - PASS: sprite slicing, contact-sheet generation and final full inventory.
  - PASS: PR-08 resource lint chain and filtered sprite-map report.
  - PASS: focused client tests plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
    changed and baselines remain intentionally unchanged.
- Director verdict:
  - V10b is accepted-forward only because it moves source-resource dark-line
    authority slightly without breaking marker readability or contracts.
  - It does not close the PR-08 visual gap.
- Next action:
  - Do not continue per-tile edge editing for this blocker.
  - Route a non-grid-aligned room-scale material-breakup resource contract, or
    generate a stronger floor/wall family where authored room-scale masonry is
    visible before the tile lattice.

## 2026-05-28 - PR-08 Room-Scale Material Breakup V11

- Loop type: resource contract + runtime compositor loop
- Target area: V10 `grid-first` blocker after source-resource de-line reached
  diminishing returns
- Contract alignment:
  - Added one explicit PR-08 owner key:
    `tileset.ruins.room_breakup_01`.
  - Reused existing `tile_decal` category and `r02-ui-demo-ruins-tiles`
    sheet; no new atlas/manifest schema.
  - Kept map darkness, light, telegraph, selection and marker readability in
    runtime compositor layers.
  - Added no save/replay/profile/core/game/content-pack contract.
  - Did not update golden baselines.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/assets/DarkUiMapVisualKeys.kt`
  - `client/src/main/kotlin/com/ktome/client/assets/ClientAssetLoadStrategy.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/sprite-sheets/sheet-plan.yaml`
  - `UI/sprite-sheets/key-registry.yaml`
  - `UI/sprite-sheets/owner-contracts/pr08-owner-keys.yaml`
  - `assets-src/image/manifests/phase2-visual-manifest.json`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_room_breakup_01.png`
  - `UI/sprite-sheets/dark-v1-final-full-inventory.json`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/`
- Candidate verdict:
  - accepted-forward V11 as the first explicit room-scale material-breakup
    resource contract and runtime draw path.
  - not director-grade closure: the runtime crop is more authored than V10b but
    still reads too grid-first.
- Evidence:
  - decision record:
    `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/pr08-room-scale-material-breakup-v11-decision.md`
  - retained runtime archive:
    `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/runtime-v11-room-breakup/`
  - runtime comparison board:
    `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/pr08-room-scale-material-breakup-v11-runtime-comparison-board.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/pr08-room-scale-material-breakup-v11-runtime-metrics.json`
- Key hashes:
  - retained raw sheet:
    `8d316c96d351c7f28b33ebfd8b6146a281eb20762da4de9b0fb876d6e033c397`
  - retained contact sheet:
    `693ccff4be5f668fae43a0c1e685524a6fd641745427e0182e72d8813ce4c0b2`
  - generated room breakup tile:
    `040f56dacdbe25fb3e0e655a97818c99be57e0def7fe81064824f4552e856378`
  - V11 runtime crop artifact:
    `0785053b1b98fe8f7e13e4405e0a85fbc01942b6e4ed03f8bcdd90bd90040bd0`
  - V11 runtime comparison board:
    `417adf2d9d598089331947e0b9c6df21989a0beb81528bc4d4e79b2bfe67fede`
- Focused golden actual hashes from retained V11 expected failure:
  - `ui-demo-new-parity-1672x941=9d6ac09c71d93d108924fcb94366dc8aea8c4b5a54ba9db401fd21f6d93b947e`
  - `ui-demo-new-parity-1280x800=9f9d4c9689e901c9012668b0e812ecea8140738280364e7f5d51fcd53b5edd12`
  - `ui-demo-new-map-stage-crop=8d5acb1f488dc2e89f880fc3614b903190c228f7cf46de725b05820a0f718601`
- Commands run:
  - local Python/PIL V11 room material-breakup generation and comparison board
    generation.
  - `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/generate_dark_final_full_inventory.py`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries --tests com.ktome.client.assets.ManifestResolveTest.session\ and\ warm\ cache\ preload\ resident\ textures\ when\ gdx\ is\ available --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
- Result:
  - PASS: sprite slicing, contact-sheet generation and final full inventory.
  - PASS: PR-08 resource lint chain and filtered sprite-map report.
  - PASS: focused exact-manifest/session-preload/room-scale-renderer test.
  - PASS: focused client tests plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
    changed and baselines remain intentionally unchanged.
- Director verdict:
  - V11 is accepted-forward as the missing room-scale material resource
    contract and a partial map-quality improvement.
  - It still fails director-grade closure because the repeated square lattice
    remains the first-read structure in the map crop.
- Next action:
  - Do not continue per-tile edge editing.
  - Next cut should either strengthen the room-scale material family so masonry
    beats the tile lattice, or demote/remove the repeated dark lattice at its
    renderer/source boundary so V11 can become the first-read material layer.

## 2026-05-28 - PR-08 Room Material Authority V12

- Loop type: runtime compositor refinement + director-rubric evidence
- Target area: V11 room-scale material-breakup visibility and remaining
  `grid-first` blocker
- Contract alignment:
  - Reused `tileset.ruins.room_breakup_01`.
  - Added no resource key, owner row, manifest schema, atlas schema,
    save/replay/profile field, `core` / `game` field or content-pack contract.
  - Kept gameplay markers, darkness, light, selection and telegraph in runtime
    compositor layers.
  - Did not update golden baselines.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `UI/review/dark-uiux-pr08-exploration/room-material-authority-v12/`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - accepted-forward V12 as a partial material-authority boost.
  - not director-grade closure: V12 makes room-level material more visible than
    V11, but the runtime crop still reads square-lattice-first.
- Evidence:
  - decision record:
    `UI/review/dark-uiux-pr08-exploration/room-material-authority-v12/pr08-room-material-authority-v12-decision.md`
  - retained runtime archive:
    `UI/review/dark-uiux-pr08-exploration/room-material-authority-v12/runtime-v12-material-authority/`
  - runtime comparison board:
    `UI/review/dark-uiux-pr08-exploration/room-material-authority-v12/pr08-room-material-authority-v12-runtime-comparison-board.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/room-material-authority-v12/pr08-room-material-authority-v12-runtime-metrics.json`
- Key hashes:
  - V12 runtime crop artifact:
    `9f4b758c0456a01610e8b2c34dad376b99fc6e4727469c52bdb536ecfdd6e7c5`
  - V12 runtime comparison board:
    `46b5c2fef9eb48d463612cb47e789ab0e753876a952b43c99b829ec97697ebc1`
- Focused golden actual hashes from retained V12 expected failure:
  - `ui-demo-new-parity-1672x941=13a75c51b4db4bdab303794a3525f2f465d5599d2fe4b42189e621ce37ddf7b8`
  - `ui-demo-new-parity-1280x800=ae2bedbfcb8fa13506dee58d926750c87c0e75d445924882d5fda3e23071e704`
  - `ui-demo-new-map-stage-crop=d946119b95206f3ad3f371b9a086f22f0bea03d96aefef32f96e04054542a394`
- Commands run:
  - local Python/PIL V12 runtime archive, comparison board and metrics
    generation.
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
- Result:
  - PASS: focused client tests plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
    changed and baselines remain intentionally unchanged.
  - NOT RUN: PR-08 resource lint chain, because V12 did not mutate resources,
    manifests, owner contracts, sheet plans or generated inventory.
- Director verdict:
  - V12 is accepted-forward only.
  - It improves material presence but still fails the director-grade map
    standard because the center map reads as a coarse square lattice before it
    reads as an authored room with strong enclosure, wall mass and local dark
    field.
- Next action:
  - Do not continue alpha-only or draw-order-only tuning of the same room
    material asset.
  - Next cut must demote/remove the repeated dark lattice at its
    renderer/source authority boundary or replace the room material family with
    stronger authored material that beats the lattice without turning into fog
    wash.

## 2026-05-28 - PR-08 Room Material Resource V13

- Loop type: same-key resource candidate + runtime director-rubric evidence
- Target area: `tileset.ruins.room_breakup_01` after V12 proved alpha/draw-order
  tuning was insufficient
- Contract alignment:
  - Reused the existing PR-08 `tileset.ruins.room_breakup_01` key, manifest row,
    owner contract row and `r02-ui-demo-ruins-tiles` sheet cell.
  - Added no manifest key, owner row, manifest schema, atlas schema,
    save/replay/profile field, `core` / `game` field or content-pack contract.
  - Kept runtime draw path in `MAP_ROOM_COMPOSITOR` below actors, loot,
    telegraph, selection and cursor.
  - Did not update golden baselines.
- Changed files:
  - `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
  - `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
  - `client/src/main/resources/dark-v1/tiles/tileset_ruins_room_breakup_01.png`
  - `UI/sprite-sheets/dark-v1-final-full-inventory.json`
  - `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`
  - `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - rejected M01/M02 as weaker or too fog-band-like at floor-repeat scale.
  - rejected M03 because runtime bright diagonal cracks read as overlay ribbons.
  - accepted-forward M04 `broken-field` as the best same-key room material
    resource candidate.
  - not director-grade closure: V13 improves material presence but the center
    map still reads as a coarse square lattice first.
- Evidence:
  - decision record:
    `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/pr08-room-material-authority-v13-decision.md`
  - candidate board:
    `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/pr08-room-breakup-v13-candidate-board.png`
  - retained runtime archive:
    `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/runtime-v13-m04-broken-field/`
  - runtime comparison board:
    `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/pr08-room-material-authority-v13-runtime-comparison-board.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/pr08-room-material-authority-v13-runtime-metrics.json`
- Key hashes:
  - retained raw sheet:
    `496adf577bea24aff14e4b60209c208db27e6700ced2184a7e251905abc6b804`
  - retained contact sheet:
    `e9c91ce264fa750814b70675d3b0b2dcf34284f4ad67afa27003dc08a86a4360`
  - V13 M04 room-breakup PNG:
    `54e230d753d06a84e08df0eb91e2df9605416c1fcc115bc1eaa92ee6414e4967`
  - PR-08 sprite-map report:
    `d3cd925806cc687c6dbaf679b0c31eae8bb5efb1f5c34252a118801728d39080`
  - V13 runtime crop artifact:
    `8a313cbeee0b7631727d6ab03d974d20ec431006259a99a972bb5427f40f126a`
  - V13 runtime comparison board:
    `44a57457fc3b2f303f4e1534e75e3c2f15353eadd6c8ac97b9c21edc5a27c6b4`
- Focused golden actual hashes from retained V13 expected failure:
  - `ui-demo-new-parity-1672x941=10dbca08e78f8e68b30f28766791c6a87ce115a3ce5c3db1bca07cdb4b2e5e41`
  - `ui-demo-new-parity-1280x800=33eeea236bae44cd47cc0c439800fa5e9881e80dbac5f102cf8034ceb2df8058`
  - `ui-demo-new-map-stage-crop=f6657c666e7a3c5cab9cc9e4aaea2966ef6b819d1d3e76581a0cfb80620cbb8a`
- Commands run:
  - local Python/PIL V13 candidate generation, sheet-cell replacement,
    runtime archive, comparison board and metrics generation.
  - `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
  - `python3 scripts/generate_dark_final_full_inventory.py`
  - `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
- Result:
  - PASS: sprite slicing, contact-sheet generation and final full inventory.
  - PASS: PR-08 resource lint chain and filtered sprite-map report after final
    M04 integration.
  - PASS: focused client tests plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
    changed and baselines remain intentionally unchanged.
- Director verdict:
  - V13 M04 is accepted-forward only.
  - It improves room material presence without schema/resource-key drift, but
    still fails the director-grade map standard because the coarse square
    lattice remains the first read.
- Next action:
  - Stop same-key room-breakup overlay variants for this blocker.
  - Next cut should either directly demote/remove repeated dark lattice
    authority at the renderer/source boundary, or shift to right-panel and
    bottom-deck chrome if map work is no longer the highest-leverage
    first-screen improvement.

## 2026-05-28 - PR-08 Ground Lattice Authority V14-V17

- Loop type: runtime compositor / renderer-source authority iteration
- Target area: map-stage first read after V13 M04 proved same-key
  room-breakup resource variants were insufficient
- Contract alignment:
  - Changed only `client` renderer behavior and focused render tests for the
    retained V17 cut.
  - Added no manifest key, owner row, manifest schema, atlas schema,
    save/replay/profile field, `core` / `game` field or content-pack contract.
  - Kept actors, loot markers, telegraphs, selection and cursor above the room
    compositor.
  - Did not update golden baselines.
- Changed files for retained V17:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/ground-no-bleed-room-material-v17/`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - rejected V14 source inset: it changed runtime pixels but increased seam
    contrast, so source-edge cropping was not the right authority cut.
  - measured V15 ground bleed `4px`: it reduced tile overdraw/noise but did not
    reduce seam contrast enough.
  - measured V16 ground bleed `4px` plus room material alpha `0.78`: vertical
    contrast improved slightly, horizontal contrast stayed too close to V13.
  - accepted-forward V17 ground bleed `0px` plus room material alpha `0.78`:
    it reduced both measured vertical and horizontal seam contrast and made the
    floor read less like overdrawn repeated tile noise.
  - not director-grade closure: the center/right map still reads too
    grid-legible at first glance.
- Evidence:
  - V14 rejected decision:
    `UI/review/dark-uiux-pr08-exploration/ground-source-inset-v14/pr08-ground-source-inset-v14-decision.md`
  - V14 rejected metrics:
    `UI/review/dark-uiux-pr08-exploration/ground-source-inset-v14/pr08-ground-source-inset-v14-runtime-metrics.json`
  - V15 metrics:
    `UI/review/dark-uiux-pr08-exploration/ground-bleed-authority-v15/pr08-ground-bleed-authority-v15-runtime-metrics.json`
  - V16 metrics:
    `UI/review/dark-uiux-pr08-exploration/ground-bleed-room-material-v16/pr08-ground-bleed-room-material-v16-runtime-metrics.json`
  - V17 decision:
    `UI/review/dark-uiux-pr08-exploration/ground-no-bleed-room-material-v17/pr08-ground-no-bleed-room-material-v17-decision.md`
  - V17 runtime archive:
    `UI/review/dark-uiux-pr08-exploration/ground-no-bleed-room-material-v17/runtime-v17-ground-bleed-0px-room-alpha-078/`
  - V17 runtime comparison board:
    `UI/review/dark-uiux-pr08-exploration/ground-no-bleed-room-material-v17/pr08-ground-no-bleed-room-material-v17-runtime-comparison-board.png`
  - V17 runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/ground-no-bleed-room-material-v17/pr08-ground-no-bleed-room-material-v17-runtime-metrics.json`
- Key hashes:
  - V14 rejected runtime comparison board:
    `08eaf05818773399551633395e7ce27697542b3924e649c2b488455833e0bddb`
  - V15 runtime comparison board:
    `daaaefaea999565070c125b1f6e3957bad4150d14013baab0472d546e138973a`
  - V16 runtime comparison board:
    `5e873c9bea17b48b66abb50142c0698625c85ba8d8b6da0e3b1753bfd91ef6fe`
  - V17 runtime crop artifact:
    `ebb82b2e1dbc1017b622e7feda6f8885708e33356ce96a6f52ba7349911cd433`
  - V17 runtime comparison board:
    `6e2c744251ce34e505e5f1a9957f20bc63572913beadeb17519c3cee27154f1f`
- V17 focused lattice metrics:
  - vertical seam mean: `7.7746290403994065 -> 7.276342483666915`
  - horizontal seam mean: `10.21626493577502 -> 9.8988323008451`
  - active map mean absolute RGB diff versus V13: `2.888639126086622`
- Focused golden actual hashes from retained V17 expected failure:
  - `ui-demo-new-parity-1672x941=29199b683a778b733cb3cf187ab41d91c3ae2c2956eb756929f414bda3649221`
  - `ui-demo-new-parity-1280x800=7aaa57ad72ebc400a1df1452a47e3d2646aacefd9fa3218b13d97e8172b3d7be`
  - `ui-demo-new-map-stage-crop=985cc279f8609d0ab30eab7baeaa1a00295bb8b5e417634b635bbcd4d280c9bf`
- Commands run:
  - local Python/PIL V14, V15, V16 and V17 runtime archive, comparison board
    and metrics generation.
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas uses restrained ground bleed so repeated tile art does not become room lattice authority" --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
- Tooling note:
  - `rtk test` was not used for tests with spaces in the `--tests` filter
    after it split the filter into Gradle task tokens; native Gradle commands
    were used for those filters.
- Result:
  - PASS: focused V17 renderer tests.
  - PASS: focused client suite plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
    changed and baselines remain intentionally unchanged.
  - NOT RUN: PR-08 resource lint chain, because V17 did not mutate resources,
    manifests, owner contracts, sheet plans or generated inventory.
- Director verdict:
  - V17 is accepted-forward only.
  - It removes the renderer-side overdraw that was reinforcing tile repetition
    and gives the room-scale material asset more authority.
  - It still fails the final director-grade map standard because the map remains
    too grid-legible compared with `UI/UI-demo-new.png`.
- Next action:
  - Do not reintroduce ground bleed or source-inset/crop experiments for this
    blocker.
  - Continue only with a different room-scale material/compositor authority cut,
    or move to right-panel/bottom-deck chrome if map iteration stops being the
    highest-leverage first-screen improvement.

## 2026-05-28 - PR-08 Map Stage Masonry V18 Rejection

- Loop type: runtime compositor / hidden-stage material probe
- Target area: large black map-stage field around the visible room after V17
- Contract alignment:
  - Probe stayed in `client` renderer/test only.
  - Added no resource, manifest, schema, save/replay/profile or gameplay rule.
  - Reverted the code and focused test after evidence review.
- Candidate verdict:
  - rejected V18 map-stage masonry alpha tuning.
  - Reason: the visual diff was too small to justify a new test contract or
    retained renderer tuning.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/map-stage-masonry-v18/pr08-map-stage-masonry-v18-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/map-stage-masonry-v18/runtime-v18-stage-masonry-texture/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/map-stage-masonry-v18/pr08-map-stage-masonry-v18-runtime-comparison-board.png`
  - metrics:
    `UI/review/dark-uiux-pr08-exploration/map-stage-masonry-v18/pr08-map-stage-masonry-v18-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V17: `0.01954902653697329`
  - max RGB diff: `4`
  - dark-stage mean luma delta: `0.00482802381026584`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps hidden stage masonry texture visible under void pressure"`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks`
  - local Python/PIL V18 runtime archive, comparison board and metrics
    generation.
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
- Result:
  - PASS: temporary V18 focused test while the candidate was active.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed while candidate was active
    because runtime visuals changed and baselines remained intentionally
    unchanged.
  - REVERTED: V18 renderer alpha changes and temporary focused test.
  - PASS after revert: focused client suite plus `maintainabilityLint` on the
    retained V17 code state.
- Next action:
  - Do not continue subtle stage-texture alpha tuning.
  - Next meaningful map attempt needs a stronger structural hidden-stage or
    room-scale compositor change, or the work should shift to right-panel /
    bottom-deck chrome where the full-screen comparison still shows clear
    UI/UX quality gaps.

## 2026-05-28 - PR-08 Right Equipment Workbench Runtime V19

- Loop type: runtime layout / right-panel UI workbench pass
- Target area: right panel equipment section after V18 proved subtle map-stage
  alpha tuning was not meaningful enough to retain
- Contract alignment:
  - Changed only `client` right-panel layout/rendering and focused render
    tests.
  - Added no resource, manifest key, owner row, manifest schema, atlas schema,
    save/replay/profile field, `core` / `game` field or content-pack contract.
  - Kept labels, numbers, hotkeys, item icons and inventory content
    runtime-owned.
  - Did not update golden baselines.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/layout/DemoShellLayout.kt`
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/DemoShellLayoutTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
- Candidate verdict:
  - accepted-forward V19 right equipment workbench.
  - It widens the equipment slot stance and strengthens the paper-doll rig
    backdrop so the top-right section reads less like compact floating icons
    on black.
  - It is not director-grade closure: right-panel icon/detail density is still
    below `UI/UI-demo-new.png`, and the map-stage grid remains the dominant
    full-screen blocker.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-comparison-board.png`
  - right panel crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-ui-demo-new-right-panel-grid.png`
  - full-screen crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-ui-demo-new-parity-1672x941.png`
- Key hashes:
  - V19 comparison board:
    `65d318dd51c23bb421f4bc1e8ce74e18222e1087e7697154e113617ea9415f4d`
  - V19 right panel crop:
    `0147692daa8cda571cee24c4e4c168d6d53e82cd9a073cad4eb67ec4a60cdb01`
  - V19 full screen:
    `75ee3ca04729310f5e8f84ec3d687bf320db49e4373766d96250e762dda5064c`
  - V19 bottom deck crop:
    `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`
  - V19 map-stage crop:
    `ebb82b2e1dbc1017b622e7feda6f8885708e33356ce96a6f52ba7349911cd433`
- Focused golden actual hashes from retained V19 expected failure:
  - `ui-demo-new-parity-1672x941=0fd56b743979b72476ab9f078db0d084201e3b6debd9028539a0ba67f2efa571`
  - `ui-demo-new-parity-1280x800=2f9bb57670f64f34ba50070ed6aba7bca5317b4b54d2b392062744559033929d`
  - `ui-demo-new-right-panel-grid=9d29e9f6aa2cb7d293df853397177e9d2d2a9a2a8613b2a615ad9de7c2c6ff23`
  - `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=985cc279f8609d0ab30eab7baeaa1a00295bb8b5e417634b635bbcd4d280c9bf`
  - `ui-demo-new-inventory-page-1=56eda712404c7fe31bb0eee2b1865cb4d38bd56efbe1734171acee158b325158`
  - `ui-demo-new-inventory-page-2=caa8797fc7ee7f553d4de372d5cdb7261ac9ade6ee9b83dcdf297cbcda4d0aac`
- Commands run:
  - RED, expected failure before implementation:
    `./gradlew :client:test --tests com.ktome.client.render.DemoShellLayoutTest --tests "com.ktome.client.render.TileRendererCanvasTest.right panel equipment rig reads as an armored paper doll scaffold"`
  - GREEN after implementation:
    `./gradlew :client:test --tests com.ktome.client.render.DemoShellLayoutTest --tests "com.ktome.client.render.TileRendererCanvasTest.right panel equipment rig reads as an armored paper doll scaffold"`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts"`
  - local Python/PIL runtime archive and comparison board generation.
  - broader focused client verification:
    `./gradlew :client:test --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - repo-relative path scan for touched V19 code/docs/evidence:
    `rg -n -F` with local absolute-path prefixes over touched V19 code,
    docs and evidence.
  - patch format check:
    `git diff --check`
- Result:
  - PASS: V19 RED proved the pre-change layout/rendering did not satisfy the
    wider paper-doll stance and stronger scaffold contract.
  - PASS: V19 focused GREEN after runtime/layout changes.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
    changed and baselines remain intentionally unchanged.
  - FAIL then fixed: broader focused client verification exposed two older
    equipment-rig assertions still sampling pre-V19 alpha/edge values; the
    assertions were updated to the retained V19 contract.
  - PASS after test-contract sync: focused client suite plus
    `maintainabilityLint`.
  - PASS: no local absolute path matches in touched V19 code/docs/evidence.
  - PASS: `git diff --check`.
  - NOT RUN: PR-08 resource lint chain, because V19 did not mutate resources,
    manifests, owner contracts, sheet plans or generated inventory.
- Director verdict:
  - V19 is accepted-forward only.
  - It is a visible right-panel polish step and should remain because it makes
    the equipment section more deliberate at first glance.
  - It does not close the director-grade gap. The right panel still needs
    inscription/backpack/icon-density work, and the map grid remains the most
    obvious full-screen quality blocker.
- Next action:
  - Do not continue subtle right-panel alpha tuning.
  - If staying on right panel, target inscription/backpack density or icon
    quality with a visible crop delta.
  - For the overall director-grade target, the higher-leverage next pass is a
    stronger structural hidden-stage or room-scale compositor change for the
    map, not another map alpha micro-tweak.

## 2026-05-28 - PR-08 Floor Variant Flip Runtime V20

- Loop type: runtime render-model / resource-use structure pass
- Target area: map floor first-read repetition after V19 right-panel workbench
  improved shell intent but left map grid as the dominant full-screen blocker
- Contract alignment:
  - Changed only `client` render-model floor placement and focused model tests.
  - Reused the existing `flipX` / `flipY` placement channel and existing PR-08
    floor variant family.
  - Added no resource, manifest key, owner row, manifest schema, atlas schema,
    save/replay/profile field, `core` / `game` field or content-pack contract.
  - Did not update golden baselines.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRenderModelTerrainVariantTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - accepted-forward V20 floor variant flips.
  - It uses deterministic `flipX` / `flipY` on existing tile-ground variant
    families, so repeated floor motifs no longer keep the same orientation
    across the room.
  - It is not director-grade closure: the runtime map still reads too
    grid-first versus `UI/UI-demo-new.png`.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-comparison-board.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-ui-demo-new-map-stage-crop.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V19 map crop: `0.2950285588331129`
  - max RGB diff versus V19 map crop: `47`
  - changed pixels versus V19 map crop: `241670 / 2086920`
  - changed ratio versus V19 map crop: `0.11580223487244361`
- Key hashes:
  - V20 comparison board:
    `1dedb883beb210c3bd4e928a8b1bc0ff8def980548d04c882d5ebb061ae05df7`
  - V20 runtime metrics:
    `6a9deea4319586b875368e59ac54888fe66a545fc5bcef79f5dcc89c3a8bfb7d`
  - V20 full screen:
    `b6aa5e63e7b83d760580c4a7fbf3e1e9d1747124fa7f56e5b9ae14fa28114b9f`
  - V20 map-stage crop:
    `0baad92e11a458e9115cdb0020aed189d17f4e659451232648e2c549065fd155`
  - V20 right panel crop:
    `2e57d934014ca9a07a31e6b9c438c1b8b5b4c4e96bf0cffd8b08399944f3fa5a`
  - V20 bottom deck crop:
    `87fc0c5f1a89615823ab088d266d510d9c777b432df95c615ec80c0575f9477b`
- Focused golden actual hashes from retained V20 expected failure:
  - `ui-demo-new-parity-1672x941=d3963a600dec4a41fac17e323cc6fc94b3cc969eb2a40a3b3a88f47c87f4eef0`
  - `ui-demo-new-parity-1280x800=e3914b3197aed87b26711f14b53c9f1827d30a83bd139e42cfef441659bf208a`
  - `ui-demo-new-right-panel-grid=9d29e9f6aa2cb7d293df853397177e9d2d2a9a2a8613b2a615ad9de7c2c6ff23`
  - `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=03a6c115338345c8ae2cd2350748cf7f3268ace665bafc61e3d6d3fa4af158bd`
  - `ui-demo-new-inventory-page-1=56eda712404c7fe31bb0eee2b1865cb4d38bd56efbe1734171acee158b325158`
  - `ui-demo-new-inventory-page-2=caa8797fc7ee7f553d4de372d5cdb7261ac9ade6ee9b83dcdf297cbcda4d0aac`
- Commands run:
  - RED, expected failure before implementation:
    `./gradlew :client:test --tests com.ktome.client.render.TileRenderModelTerrainVariantTest`
  - GREEN after implementation:
    `./gradlew :client:test --tests com.ktome.client.render.TileRenderModelTerrainVariantTest`
  - broader focused client verification:
    `./gradlew :client:test --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts"`
  - local Python/PIL runtime archive, comparison board and metrics generation.
  - repo-relative path scan for touched V20 code/docs/evidence:
    `rg -n -F` with local absolute-path prefixes over touched V20 code,
    docs and evidence.
  - patch format check:
    `git diff --check`
- Result:
  - PASS: V20 RED proved pre-change terrain variants changed keys but did not
    use deterministic flips.
  - PASS: V20 focused GREEN after render-model placement change.
  - PASS: broader focused client suite plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map pixels
    changed and baselines remain intentionally unchanged.
  - PASS: no local absolute path matches in touched V20 code/docs/evidence.
  - PASS: `git diff --check`.
  - NOT RUN: PR-08 resource lint chain, because V20 did not mutate resources,
    manifests, owner contracts, sheet plans or generated inventory.
- Director verdict:
  - V20 is accepted-forward only.
  - It is worth retaining because it extracts more visual variety from the
    existing PR-08 floor variant family without adding another overlay path.
  - It does not close the map blocker; the room still needs a stronger
    structural hidden-stage / aperture pass, or a recorded reclassification
    back to floor/wall resource generation with new candidate evidence.
- Next action:
  - Do not repeat small variant-placement tweaks.
  - Next map iteration should either make a visibly larger hidden-stage /
    aperture structure change, or deliberately re-enter Slice 2 with at least
    three new floor/wall candidates and contact-sheet/runtime evidence.

## 2026-05-28 - PR-08 Hidden Aperture Masonry Runtime V21

- Loop type: runtime compositor / hidden-stage structure pass
- Target area: map first-read black stage after V20 reduced floor orientation
  repetition but left the surrounding darkness too flat.
- Contract alignment:
  - Changed only `client` renderer compositor behavior and focused renderer
    canvas tests.
  - Reused deterministic runtime geometry and existing layer boundaries.
  - Added no resource, manifest key, owner row, schema, atlas schema,
    save/replay/profile field, `core` / `game` field or content-pack contract.
  - Did not update golden baselines.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - accepted-forward V21 hidden aperture masonry.
  - It moves the carved hidden-stage shoulder/shelf pass after hidden fog so it
    is actually visible in production screenshots, while clipping it outside
    the visible room and playable focal center.
  - It is not director-grade closure: the full-screen map is less empty, but
    the project still needs stronger map resource/chrome polish before final
    director evidence.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-comparison-board.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-ui-demo-new-map-stage-crop.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V20 map crop: `0.24766130629508237`
  - max RGB diff versus V20 map crop: `14`
  - changed pixels versus V20 map crop: `199364 / 2086920`
  - changed ratio versus V20 map crop: `0.09553025511279781`
- Key hashes:
  - V21 comparison board:
    `151f83c60d110d221c8a0a991dbc910706a23c91323582b5807a0cd842760811`
  - V21 runtime metrics:
    `26408bb4b1fa64fbcdcbc86e68d9bd04e8d2af5e42132d7a45051de836cc3248`
  - V21 full screen:
    `16798e19101b3b7c850dcec5f84ac970c31076d3dd0c531c033e8d6aa277b007`
  - V21 map-stage crop:
    `0cfaf9942278db1fe921e2fce933dd3e7a5a476845d2bb687c907d7b2cf2f433`
  - V21 right panel crop:
    `0147692daa8cda571cee24c4e4c168d6d53e82cd9a073cad4eb67ec4a60cdb01`
  - V21 bottom deck crop:
    `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`
- Focused golden actual hashes from retained V21 expected failure:
  - `ui-demo-new-parity-1672x941=c8d29456c5db97976bf73e034daa0928ea926c977ec4538b1a9fc92439606108`
  - `ui-demo-new-parity-1280x800=015b589f3f7b58333a3dce3d90167420a5fa9cafdf6775fb48dba3d096342b5a`
  - `ui-demo-new-right-panel-grid=9d29e9f6aa2cb7d293df853397177e9d2d2a9a2a8613b2a615ad9de7c2c6ff23`
  - `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=67daccb1e4b2122ed36d047c3b7d258d3fe869b62c377296439cf26ae7dac7b3`
  - `ui-demo-new-inventory-page-1=56eda712404c7fe31bb0eee2b1865cb4d38bd56efbe1734171acee158b325158`
  - `ui-demo-new-inventory-page-2=caa8797fc7ee7f553d4de372d5cdb7261ac9ade6ee9b83dcdf297cbcda4d0aac`
- Commands run:
  - RED, expected failure after tightening the canvas contract:
    `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas frames hidden stage with carved masonry aperture shelves"`
  - GREEN after moving the aperture masonry pass after hidden fog:
    `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas frames hidden stage with carved masonry aperture shelves"`
  - broader focused client verification:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts"`
  - local Python/PIL runtime archive, comparison board and metrics generation.
- Result:
  - PASS: V21 RED proved pre-fix aperture masonry was drawn before hidden fog
    and could be fully covered by the production fog veil.
  - PASS: V21 focused GREEN after moving the pass into the fog-veil phase.
  - PASS: broader focused client suite plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map pixels
    changed and baselines remain intentionally unchanged.
  - PASS: no local absolute path matches in touched V21 code/docs/evidence.
  - PASS: `git diff --check`.
  - NOT RUN: PR-08 resource lint chain, because V21 did not mutate resources,
    manifests, owner contracts, sheet plans or generated inventory.
- Director verdict:
  - V21 is accepted-forward only.
  - It fixes a real layering bug in the V21 candidate and improves the map's
    first read by giving the black stage a carved architectural silhouette.
  - It does not close the director-grade gap; the map still needs either a
    stronger resource-backed readability pass or the next loop should move to
    right-panel / bottom chrome density if the map is no longer the only
    dominant blocker.
- Next action:
  - Do not continue hidden-stage alpha-only tuning.
  - If staying on the map, make the next change resource-backed or large enough
    to materially change room readability.
  - If switching areas, prioritize right-panel inscription/backpack/icon density
    and bottom-deck chrome cohesion with crop evidence.

## 2026-05-28 - PR-08 Right Utility Chassis Runtime V22

- Loop type: runtime demo-shell / right-panel utility density pass
- Target area: right panel first-read after V21 improved hidden-stage layering
  but left the equipment, inscription and backpack blocks too separated.
- Contract alignment:
  - Changed only `client` demo-shell layout/renderer chrome and focused
    renderer/layout tests.
  - Reused deterministic runtime layout and existing demo-shell geometry.
  - Added no resource, manifest key, owner row, schema, atlas schema,
    save/replay/profile field, `core` / `game` field or content-pack contract.
  - Did not update golden baselines.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/main/kotlin/com/ktome/client/render/layout/DemoShellLayout.kt`
  - `client/src/test/kotlin/com/ktome/client/render/DemoShellLayoutTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - accepted-forward V22 right utility chassis.
  - It widens the equipment paper-doll stance and adds a shared forged chassis
    behind the inscription, backpack and operation-hint stack.
  - It is not director-grade closure: the right column is denser and more
    authored, but the full-screen gap still includes map readability,
    icon/detail density and bottom-deck chrome cohesion.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-ui-demo-new-parity-1672x941.png`
  - right-panel crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-ui-demo-new-right-panel-grid.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V21 right-panel crop: `1.5578277545327754`
  - max RGB diff versus V21 right-panel crop: `212`
  - changed pixels versus V21 right-panel crop: `417041 / 1147200`
  - changed ratio versus V21 right-panel crop: `0.3635294630404463`
- Key hashes:
  - V22 comparison board:
    `1995eb6995b0af0d7ec5c6df4b3bae3cc6c7cfabb68b73d91ee7556ea28a1119`
  - V22 runtime metrics:
    `bbeea82502e3038c25a24c3ee45aa594f02f62a86c991b8f0e4d10a9d6f68a26`
  - V22 full screen:
    `245e88f1fd5e6be5c6946fdf023b41078cedb960a348b29b6df34c769365cce1`
  - V22 map-stage crop:
    `0cfaf9942278db1fe921e2fce933dd3e7a5a476845d2bb687c907d7b2cf2f433`
  - V22 right panel crop:
    `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
  - V22 bottom deck crop:
    `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`
- Focused golden actual hashes from retained V22 expected failure:
  - `ui-demo-new-parity-1672x941=d371f83aa0e30a0520ee8aaf216318682cf64aa13c8e213cc71c4a989d6a8216`
  - `ui-demo-new-parity-1280x800=548b5437a127ff90cb66c4d9edc6911d70a5f4de38344f5283f4ee0fcd77771e`
  - `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
  - `ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=67daccb1e4b2122ed36d047c3b7d258d3fe869b62c377296439cf26ae7dac7b3`
  - `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
  - `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`
- Commands run:
  - RED, expected failure after adding the V22 stance/chassis contracts:
    `./gradlew :client:test --tests "com.ktome.client.render.DemoShellLayoutTest.demo aspect equipment sockets occupy a director grade paper doll width" --tests "com.ktome.client.render.TileRendererCanvasTest.right panel utility sections sit on one forged chassis"`
  - GREEN after widening equipment stance and adding the shared chassis:
    `./gradlew :client:test --tests "com.ktome.client.render.DemoShellLayoutTest.demo aspect equipment sockets occupy a director grade paper doll width" --tests "com.ktome.client.render.TileRendererCanvasTest.right panel utility sections sit on one forged chassis"`
  - broader focused client verification:
    `./gradlew :client:test --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts"`
  - local Python/PIL runtime archive, comparison board and metrics generation.
- Result:
  - PASS: V22 RED proved the pre-fix equipment stance was too narrow and the
    right utility stack had no shared chassis.
  - PASS: V22 focused GREEN after widening the equipment stance and drawing the
    shared right utility chassis.
  - PASS: broader focused client suite plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime right-panel
    pixels changed and baselines remain intentionally unchanged.
  - PASS: no local absolute path matches in touched V22 code/docs/evidence.
  - PASS: `git diff --check`.
  - NOT RUN: PR-08 resource lint chain, because V22 did not mutate resources,
    manifests, owner contracts, sheet plans or generated inventory.
- Director verdict:
  - V22 is accepted-forward only.
  - It improves right-panel utility density and makes the equipment,
    inscriptions, backpack and operation hints read as one forged workbench.
  - It does not close the director-grade gap; the map remains a full-screen
    quality blocker and the bottom deck still needs more cohesive chrome.
- Next action:
  - Do not keep tuning subtle right-panel rail opacity.
  - If staying in Slice 5, move to bottom-deck chrome cohesion and icon/detail
    density.
  - If returning to the map, make the next change resource-backed or visibly
    larger than hidden-stage alpha/chrome tuning.

## 2026-05-28 - PR-08 Bottom Console Cap Rails Runtime V23

- Loop type: runtime demo-shell / bottom-deck cohesion pass
- Target area: bottom HUD first-read after V22 improved right-panel density but
  left hero/action/log panels reading as adjacent independent cards.
- Contract alignment:
  - Changed only `client` demo-shell renderer chrome and focused renderer
    canvas tests.
  - Reused deterministic runtime layout and existing bottom-deck geometry.
  - Added no layout contract, resource, manifest key, owner row, schema, atlas
    schema, save/replay/profile field, `core` / `game` field or content-pack
    contract.
  - Did not update golden baselines.
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Candidate verdict:
  - accepted-forward V23 bottom console cap rails.
  - It overlays a shared top cap rail, lower cap rail and seam clamps after the
    hero, action and log panels are drawn, making the bottom HUD read more like
    one forged console assembly.
  - It is not director-grade closure: the bottom crop is more unified, but the
    full-screen gap still includes map readability, icon/detail density and
    overall material richness.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-ui-demo-new-parity-1672x941.png`
  - bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-ui-demo-new-bottom-deck-no-command-hints.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V22 bottom-deck crop: `0.1832368722493519`
  - max RGB diff versus V22 bottom-deck crop: `37`
  - changed pixels versus V22 bottom-deck crop: `32176 / 663480`
  - changed ratio versus V22 bottom-deck crop: `0.048495809971664555`
- Key hashes:
  - V23 comparison board:
    `8c2d7fd40a62ed4ab0960f8de6fec22f06d05c7eb2f5acd5bc2402a64d3bb757`
  - V23 runtime metrics:
    `73ccd28c362d63eb48f3457ff48a5639d51f25471d0d245eb5d08a6bcd7e1fe9`
  - V23 full screen:
    `702b31f53b862f25a733ac6c49d43d2224a8a7dc1280d9397a519cf2cbf24b91`
  - V23 map-stage crop:
    `0cfaf9942278db1fe921e2fce933dd3e7a5a476845d2bb687c907d7b2cf2f433`
  - V23 right panel crop:
    `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
  - V23 bottom deck crop:
    `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- Focused golden actual hashes from retained V23 expected failure:
  - `ui-demo-new-parity-1672x941=e8a037f72d8d6fbe3a9df489af4d013619ea76ab73a3ae8d4ae3e5a11aae455a`
  - `ui-demo-new-parity-1280x800=f38c99539fdc85f4f604205ca580aed7ab2c07b193f84185a85976955093336b`
  - `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
  - `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=67daccb1e4b2122ed36d047c3b7d258d3fe869b62c377296439cf26ae7dac7b3`
  - `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
  - `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`
- Commands run:
  - RED, expected failure after adding the V23 cap-rail contract:
    `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud renders continuous console cap rails over all panels"`
  - GREEN after adding the overlaid console cap rails:
    `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud renders continuous console cap rails over all panels"`
  - broader focused client verification:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts"`
  - local Python/PIL runtime archive, comparison board and metrics generation.
- Result:
  - PASS: V23 RED proved the pre-fix bottom HUD lacked a visible overlaid cap
    rail tying hero, action and log panels together.
  - PASS: V23 focused GREEN after adding the shared top/lower cap rails and
    seam clamps.
  - PASS: broader focused client suite plus `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime bottom/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: no local absolute path matches in touched V23 code/docs/evidence.
  - PASS: `git diff --check`.
  - NOT RUN: PR-08 resource lint chain, because V23 did not mutate resources,
    manifests, owner contracts, sheet plans or generated inventory.
- Director verdict:
  - V23 is accepted-forward only.
  - It improves bottom-deck assembly cohesion by giving hero/action/log one
    visible forged top and lower cap.
  - It does not close the director-grade gap; map readability and icon/detail
    richness remain unresolved.
- Next action:
  - Do not continue subtle bottom rail opacity tuning.
  - If staying in Slice 5, target visible icon/detail richness in the bottom or
    right-column controls.
  - If returning to the map, make the next change resource-backed or visibly
    larger than hidden-stage alpha/chrome tuning.

## 2026-05-28 V24 runtime wall-family relief repaint

- Precheck:
  - Active goal loop remains PR-08 director-grade UI/UX iteration.
  - Dominant blocker after V23 review is still map-stage first read.
  - Scope stays in `client` rendering and focused tests; no gameplay,
    save/replay/profile/schema/content-pack/resource/manifest schema change.
  - Kotlin/governance docs were already in scope for this run; Gradle commands
    used SDKMAN Java/Kotlin and ran serially.
- Change:
  - Added `drawPr08WallFamilyReliefRepaint` in `TileRenderer`.
  - It reuses existing visible `tileset.ruins.wall_01` family terrain
    placements, including flip state, and redraws them once in
    `MAP_ROOM_COMPOSITOR` with `alphaScale = 0.46f`.
  - Added a focused canvas test proving the relief pass reuses the exact
    terrain placement and does not introduce a second wall-piece authority.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-ui-demo-new-parity-1672x941.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-ui-demo-new-map-stage-crop.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V23 map-stage crop:
    `0.17965838013276345`
  - max RGB diff versus V23 map-stage crop: `41`
  - changed pixels versus V23 map-stage crop: `122365 / 2086920`
  - changed ratio versus V23 map-stage crop: `0.058634255266133826`
  - mean absolute RGB diff versus V23 full screen: `0.0832129428125429`
  - changed ratio versus V23 full screen: `0.02714681774961992`
- Key hashes:
  - V24 comparison board:
    `d8ccc91a1550881553304f0812724525c7d370bd589f3e9d8681c46a71ce8bb7`
  - V24 runtime metrics:
    `fa37e6961c525041ba3404439ce017a80865cc56c2d38cecf4f772113c082119`
  - V24 full screen:
    `bb213db5032848374f7a8375224751c3a83d7d91b07f474583a5f5500d0574b0`
  - V24 map-stage crop:
    `44039ee8642d34e0c12c3027e905dc5f9b76e6cfed24e5626786d8bd7cfdd8fc`
  - V24 right panel crop:
    `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
  - V24 bottom deck crop:
    `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- Focused golden actual hashes from retained V24 expected failure:
  - `ui-demo-new-parity-1672x941=db47301298606fea0eefd8457881614bfa23b02520f425923c3645a603b6f787`
  - `ui-demo-new-parity-1280x800=1a8da7ea024378fb2e8da0a4c474f517a989026b07bb3f77a7497571e8876d31`
  - `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
  - `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=59438665b9b63ae0035d5dc29c6a219970907f5423def0c9539b4b5a3accb858`
  - `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
  - `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`
- Commands run:
  - RED, expected failure after adding the V24 wall-relief contract:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere`
  - GREEN after adding the renderer pass:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere`
  - broader focused client verification:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - local Python/PIL runtime comparison board and metrics generation.
- Result:
  - PASS: V24 RED proved the pre-fix renderer had `relief=0` while base wall
    draws existed.
  - PASS: V24 focused GREEN after adding the resource-backed wall-family relief
    repaint.
  - PASS: broader focused `TileRendererCanvasTest`,
    `TileRenderModelTerrainVariantTest` and `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V24 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V24 is accepted-forward only.
  - It makes top/bottom wall runs and side walls retain more authored masonry
    after atmosphere.
  - It does not close the director-grade gap; the comparison board still shows
    a grid-first map, weaker room scale and softer floor rhythm versus
    `UI/UI-demo-new.png`.
- Next action:
  - Do not tune wall relief opacity as the next move.
  - The next map pass must be a larger floor-family visibility, map-stage
    composition or aperture-depth change, with marker/actor readability proof.
  - If the next slice leaves map work, target visible icon/detail richness in
    right/bottom controls instead.

## V25 Runtime Ground-Family Relief Repaint

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: `inline simplify`
  - stable contracts touched: none for save/replay/profile/schema/content pack
    or manifest schema
  - deletion budget: no deletion; this is a small compositor growth justified
    by V24 evidence reclassifying the next blocker as floor-family visibility
    with marker/actor readability proof.
- Production:
  - Added `TileRenderer.drawPr08GroundFamilyReliefRepaint(...)`.
  - The pass filters already-built visible `tile_ground` placements whose
    resolved key belongs to `tileset.ruins.ground_01` or its variants.
  - It reuses the normal terrain placement path, including dimensions and flip
    state, and redraws them once in `MAP_ROOM_COMPOSITOR` with
    `alphaScale = 0.34f`.
  - It is placed after global map-stage atmosphere/grid suppression and before
    actor, loot marker and telegraph layers.
- Tests:
  - Added
    `TileRendererCanvasTest.renderCanvasRepaintsPr08GroundFamilyAsRoomReliefWithoutCoveringActorsOrLootMarkers`.
  - The test proves RED before implementation with `relief=0, base=60`.
  - The test proves the relief pass reuses exact terrain placement coordinates,
    dimensions and flip flags, and remains below actor/loot marker layers.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-ui-demo-new-parity-1672x941.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-ui-demo-new-map-stage-crop.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V24 map-stage crop:
    `0.3052632587736952`
  - max RGB diff versus V24 map-stage crop: `21`
  - changed pixels versus V24 map-stage crop: `317529 / 2086920`
  - changed ratio versus V24 map-stage crop: `0.1521519751595653`
  - mean absolute RGB diff versus V24 full screen: `0.14275995878015008`
  - changed ratio versus V24 full screen: `0.06960139879696342`
- Key hashes:
  - V25 comparison board:
    `bf3ed4d0a43bf701c7574b92c67ee81f026da9263b488334fb1af240ce8839bf`
  - V25 runtime metrics:
    `c417c3ce2f4379e537c54590115f3d9fa37540167b0e52ba433827f293f7a525`
  - V25 full screen:
    `251ef0f6005ede9e4fceeafe37bce9425bf996577e6b6579650c1dfd782f6dac`
  - V25 map-stage crop:
    `c552622c4aa2a121608ec44699a47ba47d21d3a645434f662dc971f15b6e9378`
  - V25 right panel crop:
    `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
  - V25 bottom deck crop:
    `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- Focused golden actual hashes from retained V25 expected failure:
  - `ui-demo-new-parity-1672x941=461a28e25c012c76596b88809344aabcf8af16df64db1bdd7ccefc3c9fd1ab2a`
  - `ui-demo-new-parity-1280x800=73eb4cc7205a468bea78c63e1231396b6695ba60a32983029ebd7ba3017c2777`
  - `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
  - `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=c12b30c31bfe03a5bde2166fdcbe968b92f23ce51482efbc2a1e6dc73842d46a`
  - `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
  - `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`
- Commands run:
  - RED, expected failure after adding the V25 ground-relief contract:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08GroundFamilyAsRoomReliefWithoutCoveringActorsOrLootMarkers'`
  - GREEN after adding the renderer pass:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08GroundFamilyAsRoomReliefWithoutCoveringActorsOrLootMarkers'`
  - broader focused client verification:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.TileRenderModelTerrainVariantTest' maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - local Python/PIL runtime comparison board and metrics generation.
- Result:
  - PASS: V25 RED proved the pre-fix renderer had `relief=0` while base ground
    draws existed.
  - PASS: V25 focused GREEN after adding the resource-backed ground-family
    relief repaint.
  - PASS: broader focused `TileRendererCanvasTest`,
    `TileRenderModelTerrainVariantTest` and `maintainabilityLint`.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V25 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V25 is accepted-forward only.
  - It makes the visible-room floor retain more authored PR-08 stone family
    after atmosphere, and actor/loot marker readability remains protected.
  - It does not close the director-grade gap; the comparison board still shows
    visible room scale too small, black stage too dominant, floor too
    grid-forward and wall/aperture richness below `UI/UI-demo-new.png`.
- Next action:
  - Do not tune ground relief opacity as the next move.
  - The next map pass should be map-stage composition or viewport-scale work
    that increases visible-room authority without changing gameplay visibility,
    or an aperture-depth/resource-generation pass that materially changes room
    silhouette and dark-stage pressure.

## V26 Runtime Map Presentation Scale

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: `inline simplify`
  - stable contracts touched: none for gameplay rules, core visibility,
    save/replay/profile/schema/content pack or manifest schema
  - deletion budget: no deletion; this is a small runtime-presentation growth
    justified by V25 evidence showing that map room scale, not another
    floor/wall opacity pass, is the dominant blocker.
- Production:
  - Changed `FoundationGameScreen` runtime presentation cell size from `32f`
    to `40f`.
  - Synchronized `GoldenScreenshotHarnessTest.currentTileLayout(...)` and
    related PR-02-1 crop diagnostics to `40f`.
  - `TileMapViewport` still uses the same snapshot/focus contract; only the
    number of screen-visible cells changes because each cell occupies more
    presentation space.
- Tests:
  - Added
    `FoundationViewportSupportTest.usesDirectorGradePresentationCellScaleForFoundationRuntimeShell`.
  - The test proves RED while runtime source still uses 32px cells.
  - The test proves that the 40px shell keeps cell-aligned map bounds and
    reduces 1280x800 visible columns/rows enough to make map tiles read larger.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-ui-demo-new-parity-1672x941.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-ui-demo-new-map-stage-crop.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V25 map-stage crop:
    `10.215827311700178`
  - max RGB diff versus V25 map-stage crop: `226`
  - changed pixels versus V25 map-stage crop: `1219395 / 2086920`
  - changed ratio versus V25 map-stage crop: `0.5843036628140993`
  - mean absolute RGB diff versus V25 full screen: `4.687473146505042`
  - changed ratio versus V25 full screen: `0.26832822534308914`
  - right panel changed ratio versus V25: `0.0`
  - bottom deck changed ratio versus V25: `0.0`
- Key hashes:
  - V26 comparison board:
    `5ee8ab824d39cbc6742ba13b994c7ec8434966cd832b4bbf11c3da1623e80d41`
  - V26 runtime metrics:
    `a8c39524e4a00bbbf4abf111f6a672ed813d238d96fe41cb125d1c2f0f8fb60c`
  - V26 full screen:
    `6779e2cf6ab483533f77b0e79e96e937d18160472241fd704c211f57ca079f99`
  - V26 map-stage crop:
    `322b36600d01b41d0fce5349edbf919503233991a086880e315f044a25285ed1`
  - V26 right panel crop:
    `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
  - V26 bottom deck crop:
    `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- Focused golden actual hashes from retained V26 expected failure:
  - `ui-demo-new-parity-1672x941=8daf37fa172eb68d0e91709ab75c26e78a757875acc6d22e121556b277cceb2e`
  - `ui-demo-new-parity-1280x800=fc35bc89d6bb80e2dca0cd9693b90240652a30538c1259d5cf04eb79f9f88bdb`
  - `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
  - `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=47ee6906ca1a6a7c8e750ad72a9ac2453ca9f34db32b72ab5fe02c0274e1964e`
  - `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
  - `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`
- Commands run:
  - RED, expected failure after adding the V26 scale contract:
    `./gradlew :client:test --tests 'com.ktome.client.screen.FoundationViewportSupportTest.usesDirectorGradePresentationCellScaleForFoundationRuntimeShell'`
  - GREEN after changing runtime presentation cell size:
    `./gradlew :client:test --tests 'com.ktome.client.screen.FoundationViewportSupportTest.usesDirectorGradePresentationCellScaleForFoundationRuntimeShell'`
  - broader focused client verification:
    `./gradlew :client:test --tests 'com.ktome.client.screen.FoundationViewportSupportTest' --tests 'com.ktome.client.render.GameShellLayoutTest' --tests 'com.ktome.client.render.InfoSurfaceLayoutTest' --tests 'com.ktome.client.render.DemoShellLayoutTest' --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08GroundFamilyAsRoomReliefWithoutCoveringActorsOrLootMarkers' maintainabilityLint`
  - runtime smoke:
    `./gradlew :client:clientSmoke`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - local Python/PIL runtime comparison board and metrics generation.
- Result:
  - PASS: V26 RED proved runtime source still used 32px before the change.
  - PASS: V26 focused GREEN after changing runtime presentation cells to 40px.
  - PASS: focused layout/viewport/render tests and `maintainabilityLint`.
  - PASS: `:client:clientSmoke` task succeeded; three render/audio smoke
    subtests were skipped by existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V26 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V26 is accepted-forward only.
  - It is a materially larger improvement than V24/V25 opacity/resource
    readability passes: visible room no longer reads as miniature inside the
    map stage.
  - Right panel, bottom deck, nav and inventory crops remain isolated.
  - It does not close director-grade quality: black stage area still dominates,
    room silhouette remains too rectangular, and wall/corridor mouth depth is
    still weaker than `UI/UI-demo-new.png`.
- Next action:
  - Do not keep tuning presentation cell size.
  - Build on the new scale with aperture-depth, generated floor/wall resource
    density, room silhouette or map-stage composition work.

## V27 Runtime Aperture Depth

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: `inline simplify`
  - stable contracts touched: none for gameplay rules, core visibility,
    save/replay/profile/schema/content pack or manifest schema
  - deletion budget: no deletion; this extends the existing post-fog
    `drawHiddenStageApertureMasonry(...)` path instead of adding a second map
    or visibility authority.
- Production:
  - Added `drawDirectorScaleApertureDepth(...)` after hidden fog aperture
    masonry in `TileRenderer`.
  - Added heavy upper/lower shelves and left/right dark pylons outside
    `visibleClip`.
  - Added short focal worn-masonry lip highlights so the enlarged V26 room has
    material catchlights without becoming a UI frame.
  - Added a viewport-safe fallback: when `playerTile` is outside the current
    viewport, focal lip placement uses `visibleClip` center instead of calling
    `tileRect(playerTile)`.
- Tests:
  - Added
    `TileRendererCanvasTest.render canvas frames enlarged room with stage scale aperture depth`.
  - Added
    `TileRendererCanvasTest.render canvas outlines enlarged room aperture with worn masonry lips`.
  - The first test proves RED while the renderer lacks stage-scale shelves and
    pylons.
  - The second test proves RED while the aperture has only low-alpha decorative
    lips rather than readable worn masonry catchlights.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-ui-demo-new-parity-1672x941.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-ui-demo-new-map-stage-crop.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V26 map-stage crop:
    `0.11129463515611522`
  - max RGB diff versus V26 map-stage crop: `27`
  - changed ratio versus V26 map-stage crop: `0.042738102083453126`
  - mean absolute RGB diff versus V26 full screen:
    `0.050649240178506354`
  - changed ratio versus V26 full screen: `0.019462904677402134`
  - right panel changed ratio versus V26: `0.0`
  - bottom deck changed ratio versus V26: `0.0`
- Key hashes:
  - V27 comparison board:
    `dda23903bddb5ef41592a25c3e63e433296f41116dd0d408d9147416a401687a`
  - V27 runtime metrics:
    `84733d083bef96c23dc7679e1b43a247579f0e01722ee3bda795efc32280e97f`
  - V27 full screen:
    `bd6f98619179542c390179e0fe7deb3ea29c0af55510d3fd7ed7fa7ec63225b6`
  - V27 map-stage crop:
    `9f2322d33cdc4ea6c0449f2e23ae2a16d1fe9aa8b0b1db5e0d66dc5f1e89b7be`
  - V27 right panel crop:
    `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
  - V27 bottom deck crop:
    `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- Focused golden actual hashes from retained V27 expected failure:
  - `ui-demo-new-parity-1672x941=a1d8870792891d3ff66895284efb23287d930721769f865d90b40dfcb98ae8c5`
  - `ui-demo-new-parity-1280x800=9836b2728b1e87ab35b0d1b42137037cfb031ffde6e30ca66584b6ab8c628abc`
  - `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
  - `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=e6dcae8b77f337daad88b6238d25cd75f6e86693193f72b4cfa9f9a567f4bbcc`
  - `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
  - `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`
- Commands run:
  - RED, expected failure after adding the V27 aperture-depth contract:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas frames enlarged room with stage scale aperture depth'`
  - RED, expected failure after adding the V27 worn-lip contract:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas outlines enlarged room aperture with worn masonry lips'`
  - GREEN after adding shelves, pylons and focal lips:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas frames enlarged room with stage scale aperture depth' --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas outlines enlarged room aperture with worn masonry lips'`
  - broader focused client verification:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.screen.FoundationViewportSupportTest' maintainabilityLint`
  - runtime smoke:
    `./gradlew :client:clientSmoke`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - local Python/PIL runtime comparison board and metrics generation.
- Result:
  - PASS: V27 RED proved the previous renderer had no stage-scale aperture
    lintel.
  - PASS: second RED proved the previous aperture lips were not readable enough
    to establish material identity.
  - PASS: V27 focused GREEN after adding the aperture-depth pass.
  - PASS: broader focused `TileRendererCanvasTest`,
    `FoundationViewportSupportTest` and `maintainabilityLint`.
  - PASS: final `:client:clientSmoke`; an intermediate smoke run exposed a
    real viewport bug when validation overlay had `playerTile` outside visible
    range, and the final fallback fix was verified.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V27 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V27 is accepted-forward only.
  - It improves the V26 map by giving the enlarged room a clearer carved
    aperture, with top and side material cues visible in the crop.
  - It does not close the director-grade gap: the room silhouette is still too
    rectangular, the floor/wall read is still grid-first, and black stage space
    still dominates more than the reference crop.
- Next action:
  - Do not continue with hidden-stage alpha-only tuning, lip-opacity tuning or
    another presentation-scale pass.
  - The next map iteration should use generated floor/wall resource density,
    larger room-silhouette compositor work or map-stage composition to break
    the rectangular read more decisively.

## V28 Runtime Room Silhouette Pressure

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: `inline simplify`
  - stable contracts touched: none for gameplay rules, core visibility,
    save/replay/profile/schema/content pack or manifest schema
  - deletion budget: no deletion; this extends the existing visible-room
    compositor after V27, and is justified by V27 evidence that the room still
    read as a rectangular grid block.
- Production:
  - Added `drawVisibleRoomSilhouettePressure(...)` in `TileRenderer`.
  - Called it from `renderWarmMapOverlay(...)` after visible-room corner
    breakup and before outer shadows.
  - Added upper, lower and right-side room-edge pressure blocks inside the
    visible room perimeter.
  - Added small worn-stone lip hints so the dark cuts read as dungeon material
    rather than a flat UI mask.
- Tests:
  - Added
    `TileRendererCanvasTest.render canvas breaks enlarged room grid edges with room scale silhouette pressure`.
  - The test proves RED while there is no broad room-edge silhouette cut.
  - The contract was intentionally strengthened after first screenshot review
    showed the initial cut was too weak to materially affect first read.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-ui-demo-new-parity-1672x941.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-ui-demo-new-map-stage-crop.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V27 map-stage crop:
    `0.21215379602476378`
  - max RGB diff versus V27 map-stage crop: `46`
  - changed ratio versus V27 map-stage crop: `0.031062522760814983`
  - mean absolute RGB diff versus V27 full screen:
    `0.09782230337945143`
  - changed ratio versus V27 full screen: `0.014257299065943285`
  - V27 resized map/reference mean abs diff: `17.150591621464486`
  - V28 resized map/reference mean abs diff: `17.07226982027741`
  - right panel changed ratio versus V27: `0.0`
  - bottom deck changed ratio versus V27: `0.0`
- Key hashes:
  - V28 comparison board:
    `b921449f983ed99c88c8dee3b3af9a6ce057e75913d4a049600492b7179e0edb`
  - V28 runtime metrics:
    `94f37a9cff230b2ee3db25bf8b5ab0204584d542ff24db9bf9e967f4575cdc38`
  - V28 full screen:
    `3192cc3709d6cfa5be57c979c50a2435cac177ecdccd5685b50ca615c4108cfc`
  - V28 map-stage crop:
    `7ea39b9648aea259d2ca76d6aa92b169cf70b220758e19160a58d310f620fd1f`
  - V28 right panel crop:
    `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
  - V28 bottom deck crop:
    `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- Focused golden actual hashes from retained V28 expected failure:
  - `ui-demo-new-parity-1672x941=5f7f8794cc2287d4c31aeadbb3d43993b97006fbeab05d534b52c118cac86311`
  - `ui-demo-new-parity-1280x800=cc1d0538e054c1eb1513110334ff9b9e4854b8c351eb77cf39217b7d20566c95`
  - `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
  - `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=284b4e97a203b9ea243b7631379f82e5af365ee89bc651f2421edc909dc6143b`
  - `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
  - `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`
- Commands run:
  - RED, expected failure after adding the V28 room-silhouette contract:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks enlarged room grid edges with room scale silhouette pressure'`
  - RED, expected failure after strengthening the V28 room-silhouette contract:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks enlarged room grid edges with room scale silhouette pressure'`
  - GREEN after adding the stronger silhouette-pressure pass:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks enlarged room grid edges with room scale silhouette pressure'`
  - broader focused client verification:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.screen.FoundationViewportSupportTest' maintainabilityLint`
  - runtime smoke:
    `./gradlew :client:clientSmoke`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - local Python/PIL runtime comparison board and metrics generation.
- Result:
  - PASS: V28 RED proved the previous renderer had no room-scale silhouette
    pressure bite.
  - PASS: strengthened RED prevented retaining a visually weak edge mask.
  - PASS: focused GREEN after adding the room-silhouette pass.
  - PASS: broader focused `TileRendererCanvasTest`,
    `FoundationViewportSupportTest` and `maintainabilityLint`.
  - PASS: final `:client:clientSmoke`; three render/audio smoke subtests were
    skipped by existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V28 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V28 is accepted-forward only.
  - It gives the enlarged V27 room a stronger in-room edge cut and improves the
    resized reference-distance metric.
  - It does not close the director-grade gap: the map remains too grid-forward,
    and the next high-return move should not be another rectangle overlay.
- Next action:
  - Do not continue with room-edge opacity tuning or another rectangular
    compositor bite.
  - Switch technique: generated/authored room-scale floor-wall resources,
    map-stage composition, or a right/bottom density slice.

## V29 Resource Room Breakup

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: `content/schema wiring` for an existing PR-08 resource key
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack or manifest schema changes
  - deletion budget: no deletion; this replaces one existing runtime resource
    and r02 raw-sheet cell because V28 showed compositor-only room-edge passes
    had diminishing return.
- Production:
  - Replaced `tileset.ruins.room_breakup_01` with a fragmented,
    semi-transparent painterly stone overlay.
  - Updated
    `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`,
    `client/src/main/resources/dark-v1/tiles/tileset_ruins_room_breakup_01.png`,
    the r02 contact sheet, and `dark-v1-pr08-sprite-map-report.jsonl`.
  - Added a resource-quality regression in
    `DarkSpriteSheetPipelineScriptTest` so the previous flat/vector-like
    room-scale decal cannot pass again.
- Tests:
  - Added
    `DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture`.
  - The test first failed on the old resource with
    `alphaCoverage=0.82415771484375`,
    `uniqueColorCount=2924`, and `meanWeightedEdge=1.7543122324668043`.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/resource-room-breakup-v29/pr08-resource-room-breakup-v29-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/resource-room-breakup-v29/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/resource-room-breakup-v29/pr08-resource-room-breakup-v29-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/resource-room-breakup-v29/pr08-resource-room-breakup-v29-ui-demo-new-parity-1672x941.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/resource-room-breakup-v29/pr08-resource-room-breakup-v29-ui-demo-new-map-stage-crop.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/resource-room-breakup-v29/pr08-resource-room-breakup-v29-runtime-metrics.json`
- Key metrics:
  - resource alpha coverage: `0.604248046875`
  - resource max alpha: `162`
  - resource unique color count: `5646`
  - resource mean weighted edge: `1.884943452794504`
  - mean absolute RGB diff versus V28 map-stage crop:
    `2.0848548738491814`
  - changed ratio versus V28 map-stage crop:
    `0.29979491307764555`
  - mean absolute RGB diff versus V28 full screen:
    `0.9569317080136338`
  - changed ratio versus V28 full screen:
    `0.13745700262878238`
  - V28 resized map/reference mean abs diff: `17.126598208524204`
  - V29 resized map/reference mean abs diff: `15.950576128137797`
  - right panel changed ratio versus V28: `0.0`
  - bottom deck changed ratio versus V28: `0.0`
- Key hashes:
  - V29 comparison board:
    `26eea9264ed3569258b3e1ed18ca3156bb242308632116e27c9d6129383c37c9`
  - V29 runtime metrics:
    `cee167d1b5a8957a2adbbbb22680cacb3504519cf702458cb7fda59ee167c2c2`
  - V29 resource:
    `16fc7cf156f238f6fa9f73f07d5a3ea2e562df97d79444edf936dc6a6398f729`
  - V29 r02 contact sheet:
    `7c22c4f90920bfd49954b57f87f136d99eff49a4f97dbe39e55ab1cfd5578442`
  - V29 full screen:
    `76fb525781ca8a6b6fbbc207c91eb323e54261169f44fd5ece2090b628455c55`
  - V29 map-stage crop:
    `d176d825f0c4d75c43a45e9b7c598e2fb082d550f6e1d6d17822ad1d8720c292`
  - V29 right panel crop:
    `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
  - V29 bottom deck crop:
    `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- Focused golden actual hashes from retained V29 expected failure:
  - `ui-demo-new-parity-1672x941=aed40ead7a93fa16c9371fc22a39eaeff49165dbc9b6bacdbef45cabae57c803`
  - `ui-demo-new-parity-1280x800=10ba1bc0857ba17f713db7765cd8fd06bc8460212029f4bf19d15ce18f7301f8`
  - `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
  - `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=dd925d590523b934830097c6ddd50e50a44c4be3ee81e589eea4a9ae8ce4d6df`
  - `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
  - `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`
- Commands run:
  - RED, expected failure on the old resource:
    `./gradlew :tools:test --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture'`
  - GREEN after replacing the resource:
    `./gradlew resourcePipelineLint :tools:test --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture' :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset' --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08GroundFamilyAsRoomReliefWithoutCoveringActorsOrLootMarkers' maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - broader focused client verification and smoke:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.screen.FoundationViewportSupportTest' :client:clientSmoke`
  - local Python/PIL resource generation, report update, runtime comparison
    board and metrics generation.
- Result:
  - PASS: V29 RED proved the old room-scale resource was too opaque and
    insufficiently painterly.
  - PASS: resource pipeline lint, focused resource-quality test, focused
    renderer consumption tests, and `maintainabilityLint`.
  - PASS: broader `TileRendererCanvasTest`,
    `FoundationViewportSupportTest`, and `:client:clientSmoke`; three
    render/audio smoke subtests were skipped by existing environment
    assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V29 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V29 is accepted-forward only.
  - The floor surface now reads less like a flat grid with a cartoon overlay
    and more like fragmented dungeon stone under the existing dark UI light
    stack.
  - It does not close the director-grade gap: corridor mouth shape, wall-room
    integration and overall stage composition still lag the reference.
- Next action:
  - Do not keep tuning the same `room_breakup_01` texture.
  - Move to map-stage composition/corridor mouth architecture, or take a
    right/bottom density pass if map-only deltas start shrinking again.

## V30 Runtime Corridor Mouth Apron

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: runtime compositor presentation change inside
    `MAP_ROOM_COMPOSITOR`
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest schema, owner-contract,
    resource file or golden expected-hash changes
  - deletion budget: no deletion; this extends the existing
    `drawVisiblePassageThresholds` corridor-mouth architecture because V29
    left corridor mouth silhouette and wall-room integration as active blockers
- Production:
  - Added room-side stone aprons, restrained worn sills and asymmetric
    abutments for wide north/south/east/west corridor mouths.
  - Kept the pass gated by the existing visible-floor and `wide*` room-side
    checks, so it does not create a second map or visibility authority.
- Tests:
  - Added
    `TileRendererCanvasTest.render canvas anchors corridor mouths with room side stone aprons`.
  - Added
    `TileRendererCanvasTest.render canvas anchors horizontal corridor mouths with room side stone aprons`.
  - Both tests first failed on the previous implementation because the
    room-side apron did not exist.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-corridor-mouth-apron-v30/pr08-corridor-mouth-apron-v30-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-corridor-mouth-apron-v30/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-corridor-mouth-apron-v30/pr08-corridor-mouth-apron-v30-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-corridor-mouth-apron-v30/pr08-corridor-mouth-apron-v30-ui-demo-new-parity-1672x941.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-corridor-mouth-apron-v30/pr08-corridor-mouth-apron-v30-ui-demo-new-map-stage-crop.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-corridor-mouth-apron-v30/pr08-corridor-mouth-apron-v30-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V29 map-stage crop:
    `0.062326778218618824`
  - changed ratio versus V29 map-stage crop:
    `0.016053322599812164`
  - mean absolute RGB diff versus V29 full screen:
    `0.028491505185531698`
  - changed ratio versus V29 full screen:
    `0.007348006040606298`
  - V29 resized map/reference mean abs diff from the same V30 metric script:
    `15.843126713050811`
  - V30 resized map/reference mean abs diff from the same V30 metric script:
    `15.802840070534568`
  - right panel changed ratio versus V29: `0.0`
  - bottom deck changed ratio versus V29: `0.0`
- Key hashes:
  - V30 comparison board:
    `2d3bda6427d64c4ff8c74a7640d5a139bcccda18f36916c369ad87fd816c76ac`
  - V30 runtime metrics:
    `a25f446c569965ee0a13586e4968d453294e1bc37586c2456c17514f0abc8386`
  - V30 full screen file:
    `0a10d5f9400bff39013961491810e645a66e007f443787144e542867b5db0052`
  - V30 map-stage crop file:
    `b07adf8da68253006bdce2af1a2657f870fe6f31d65d35aa355485102895b6a8`
  - V30 right panel crop:
    `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277`
  - V30 bottom deck crop:
    `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- Focused golden actual hashes from retained V30 expected failure:
  - `ui-demo-new-parity-1672x941=e3311215b8b07c438f81cefff9a5a6d7df754b7959ad55b67998445a0f9cf5c1`
  - `ui-demo-new-parity-1280x800=e72546f818a6d08aa1b7024082a8a0f86f51426cd5382bd7b4bf6ded2f81dfa5`
  - `ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2`
  - `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7`
  - `ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864`
  - `ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436`
- Commands run:
  - RED, expected failure on the old vertical corridor-mouth implementation:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas anchors corridor mouths with room side stone aprons'`
  - RED, expected failure on the old horizontal corridor-mouth implementation:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas anchors horizontal corridor mouths with room side stone aprons'`
  - GREEN focused tests:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas anchors corridor mouths with room side stone aprons' --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas anchors horizontal corridor mouths with room side stone aprons'`
  - broader focused client verification, smoke and anti-bloat lint:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.screen.FoundationViewportSupportTest' :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - local Python/PIL runtime archive, comparison board and metrics generation.
- Result:
  - PASS: V30 RED proved both vertical and horizontal corridor mouths lacked
    room-side apron architecture.
  - PASS: focused V30 corridor-mouth tests.
  - PASS: broader `TileRendererCanvasTest`,
    `FoundationViewportSupportTest`, `:client:clientSmoke` and
    `maintainabilityLint`; three render/audio smoke subtests were skipped by
    existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V30 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V30 is accepted-forward only.
  - Corridor openings now read less like a one-tile slot and more like a
    carved masonry threshold tied into the room floor/wall mass.
  - It does not close the director-grade gap: the map-stage delta is modest and
    the full surface still reads more grid-first than `UI/UI-demo-new.png`.
- Next action:
  - Do not continue tuning the same corridor-mouth apron.
  - Switch technique: right-panel/bottom-deck density and chrome, stronger
    map-stage composition/black-field balance, or generated/authored
    resource-backed corridor/wall-thickness work.

## V31 Right Equipment Density

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: right-panel layout and renderer presentation change
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest schema, owner-contract,
    resource file or golden expected-hash changes
  - deletion budget: no deletion; reused the existing equipment layout,
    `drawEquipmentRigBackdrop` and `drawEmptySlotInterior` paths
- Production:
  - Reduced demo-aspect equipment socket size from `64f` to `58f`.
  - Reduced demo-aspect equipment section ratio from `0.42f` to `0.38f`.
  - Widened the bounded equipment column gap so the compact sockets still read
    as a paper-doll stance.
  - Added compact armory bay plates behind equipment socket pairs.
  - Kept the shoulder mantle centered/bounded so the broad stance does not turn
    the equipment rig into a full-width grid bar.
  - Added visual-only authored inner plates so empty equipment targets read as
    deliberate sockets.
- Tests:
  - Added
    `DemoShellLayoutTest.demo aspect right panel keeps equipment rack compact enough for utility density`.
  - Added
    `TileRendererCanvasTest.right panel equipment rack groups socket pairs into compact armory bays`.
  - Added
    `TileRendererCanvasTest.visual only equipment sockets render as full authored slot plates`.
  - The first RED proved V30 still over-allocated equipment height
    (`equipmentRatio=0.41196597`) and lacked compact bay plates.
  - The second RED proved visual-only equipment sockets lacked full authored
    inner plates.
  - Broader owner verification caught and fixed a stance/shoulder boundary:
    stance initially fell to `0.54808605`, then passed after the bounded column
    cap and shoulder mantle adjustment.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-density-v31/pr08-right-equipment-density-v31-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-density-v31/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-density-v31/pr08-right-equipment-density-v31-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-density-v31/pr08-right-equipment-density-v31-ui-demo-new-parity-1672x941.png`
  - right-panel crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-density-v31/pr08-right-equipment-density-v31-ui-demo-new-right-panel-grid.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-density-v31/pr08-right-equipment-density-v31-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V30 right-panel crop:
    `1.2055291143654114`
  - changed ratio versus V30 right-panel crop:
    `0.15943776150627614`
  - mean absolute RGB diff versus V30 full screen:
    `0.3039621775673848`
  - changed ratio versus V30 full screen:
    `0.040147246134367896`
  - map-stage changed ratio versus V30: `0.0`
  - bottom-deck changed ratio versus V30: `0.0`
  - V30 resized right/reference mean abs diff:
    `13.604931427243143`
  - V31 resized right/reference mean abs diff:
    `13.5746580079033`
- Key hashes:
  - V31 comparison board:
    `0e4bc8a965ed781ed16916182571e78d757c58e9b542f9715d313aba20d827e9`
  - V31 runtime metrics:
    `71f2c70efe412677690b6f7d54fb41f7c58ca3d475e06bdc96b82f8fca10b6fe`
  - V31 full screen file:
    `4b8acce290252ca752c53df8aa26e30921fc40dcc38b95e67cf6910a157d00fc`
  - V31 right panel crop:
    `f1fd786aeac2e56a2b963f8d693679cf51680f5715716d5cfa4f6d460c720686`
  - V31 bottom deck crop:
    `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e`
- Focused golden actual hashes from retained V31 expected failure:
  - `ui-demo-new-parity-1672x941=ec7e28277fa87f1f7ed7fa42df1174bd04ad1aea21fe28bc04b80c363c46eb63`
  - `ui-demo-new-parity-1280x800=5134c24d2bf808be26a3a7245f6d14fb91281e2a409370a700328eb2ae448e72`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED, expected failure on V30 right-panel density:
    `./gradlew :client:test --tests 'com.ktome.client.render.DemoShellLayoutTest.demo aspect right panel keeps equipment rack compact enough for utility density' --tests 'com.ktome.client.render.TileRendererCanvasTest.right panel equipment rack groups socket pairs into compact armory bays'`
  - RED, expected failure on V30 visual-only equipment plates:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.visual only equipment sockets render as full authored slot plates'`
  - GREEN focused V31 tests:
    `./gradlew :client:test --tests 'com.ktome.client.render.DemoShellLayoutTest.demo aspect right panel keeps equipment rack compact enough for utility density' --tests 'com.ktome.client.render.TileRendererCanvasTest.right panel equipment rack groups socket pairs into compact armory bays' --tests 'com.ktome.client.render.TileRendererCanvasTest.visual only equipment sockets render as full authored slot plates'`
  - GREEN boundary regression after owner-check fix:
    `./gradlew :client:test --tests 'com.ktome.client.render.DemoShellLayoutTest.demo aspect equipment sockets occupy a director grade paper doll width' --tests 'com.ktome.client.render.TileRendererCanvasTest.right panel equipment rig reads as an armored paper doll scaffold'`
  - broader focused client verification, smoke and anti-bloat lint:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellLayoutTest' --tests 'com.ktome.client.screen.FoundationViewportSupportTest' :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
  - local Python/PIL runtime archive, comparison board and metrics generation.
- Result:
  - PASS: V31 RED proved the old right equipment rack was too tall/sparse and
    lacked compact bay plates.
  - PASS: visual-only equipment plate RED proved empty equipment targets were
    still too placeholder-like.
  - PASS: focused V31 tests.
  - PASS: broader `TileRendererCanvasTest`, `DemoShellLayoutTest`,
    `FoundationViewportSupportTest`, `:client:clientSmoke` and
    `maintainabilityLint`; three render/audio smoke subtests were skipped by
    existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime right-panel,
    inventory and full-screen pixels changed and baselines remain intentionally
    unchanged.
  - PASS: V31 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V31 is accepted-forward only.
  - Right-panel equipment now reads more like a compact armory rack and less
    like sparse sockets floating in a large black field.
  - It does not close the director-grade gap: the reference still has richer
    item silhouettes, stronger slot material polish and denser right/bottom
    composition.
- Next action:
  - Do not continue with equipment-slot alpha tweaks.
  - Switch technique to bottom-deck/action-log density, generated/authored
    equipment-placeholder or chrome resources, or map-stage composition if the
    first-screen map remains dominant.

## V32 Bottom Log Ledger Density

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: bottom log deck presentation change
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest schema, owner-contract,
    resource file or golden expected-hash changes
  - deletion budget: no deletion; reused the existing `visibleLines`,
    `drawLogLedgerGutter` and log tone mapping
- Production:
  - Added `drawLogEventRowPlates` in `DemoShellRenderer`.
  - Drew one subdued row plate behind each visible bottom-log event row.
  - Drew one tone-aware accent strip beside each visible event row using the
    existing `logLedgerAccentHex` mapping.
  - Kept wrapping, icons, localized text and event source data unchanged.
- Tests:
  - Added
    `TileRendererCanvasTest.bottom log renders event row plates and tone accents for dense ledger read`.
  - RED proved the V31 bottom log lacked per-event row plates.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-log-ledger-density-v32/pr08-bottom-log-ledger-density-v32-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-log-ledger-density-v32/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-log-ledger-density-v32/pr08-bottom-log-ledger-density-v32-comparison-board.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-log-ledger-density-v32/pr08-bottom-log-ledger-density-v32-ui-demo-new-parity-1672x941.png`
  - bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-log-ledger-density-v32/pr08-bottom-log-ledger-density-v32-ui-demo-new-bottom-deck-no-command-hints.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-log-ledger-density-v32/pr08-bottom-log-ledger-density-v32-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V31 bottom-deck crop:
    `0.14352454733626738`
  - changed ratio versus V31 bottom-deck crop:
    `0.10863628142521252`
  - mean absolute RGB diff versus V31 full screen:
    `0.02092522842949321`
  - changed ratio versus V31 full screen:
    `0.01598037184304593`
  - map-stage changed ratio versus V31: `0.0`
  - right-panel changed ratio versus V31: `0.0`
  - nav changed ratio versus V31: `0.0`
  - inventory changed ratio versus V31: `0.0`
  - V31 resized bottom/reference mean abs diff:
    `20.76284412895593`
  - V32 resized bottom/reference mean abs diff:
    `20.796913339248743`
- Key hashes:
  - V32 comparison board:
    `42145383b8fd4b33f80a8e491f597998fac750e628a0c085a926dfbd2b67e6e0`
  - V32 full screen file:
    `e0d649261c81b1e4856c83abe4e73c55a2117e8e859aacda9d1e977264969654`
  - V32 bottom deck crop:
    `d905e4bc68a52fc15e508d25f33813487b8dc306f9a838fc0cde21d5e7a4a225`
  - V32 bottom diff:
    `4a138c074bd234127ea7d2cd73a9f73ab62f2671b1a7f2e5fab82d3795b59b89`
- Focused golden actual hashes from retained V32 expected failure:
  - `ui-demo-new-parity-1672x941=bd705cdc3dee6b0a50a04e6989b257fe3671f936559c7a6e98e0bf0c2dcf366c`
  - `ui-demo-new-parity-1280x800=40f8dfba45e17cb8b509afbe4958e9a83014ac9bf4759c022b2356a2fdd2ced2`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=c60fc362538e9e38054a5efa1c6dbf52fda6199d936cfe3b3cbf5450af5ee5ed`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED, expected failure on V31 bottom log density:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom log renders event row plates and tone accents for dense ledger read'`
  - GREEN focused V32 test:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom log renders event row plates and tone accents for dense ledger read'`
  - broader focused client verification, smoke and anti-bloat lint:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL runtime archive, bottom comparison board and metrics
    generation.
- Result:
  - PASS: V32 RED proved the old bottom log had ledger ticks but no per-event
    row plates.
  - PASS: focused V32 test.
  - PASS: broader `TileRendererCanvasTest`, `DemoShellLayoutTest`,
    `FoundationViewportSupportTest`, `:client:clientSmoke` and
    `maintainabilityLint`; three render/audio smoke subtests were skipped by
    existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime bottom/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V32 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V32 is accepted-forward only.
  - The bottom log now reads more like a dense event ledger than a flat text
    block, with tone accents supporting quick scanning.
  - It does not close the director-grade gap: the reference still has stronger
    action-slot icon subjects, richer material hierarchy and more authored log
    composition.
- Next action:
  - Do not continue by only increasing row-plate alpha.
  - Switch technique to bottom action-slot icon/detail richness, authored
    equipment placeholders, or a larger map-stage composition/resource move.

## V33 Bottom Action Icon Richness

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: bottom action slot presentation change
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest schema, owner-contract,
    resource file or golden expected-hash changes
  - deletion budget: no deletion; reused `ActionPanelEntryModel.icon`, existing
    action deck layout and action slot render path
- Production:
  - Increased filled action icon subject size by reducing the icon inset from
    `0.17` to `0.12` of the socket side.
  - Added `drawActionIconSubjectAccents` in `DemoShellRenderer`.
  - Drew a low-alpha subject backing, brass underline, brass side glint, cyan
    side rim and small top material line for filled action sockets.
  - Kept hotkeys, labels, action ordering, enabled state and icon keys
    unchanged.
- Tests:
  - Added
    `TileRendererCanvasTest.bottom action slots render large readable icon subjects with material accents`.
  - RED proved the V32 action icon subject occupied only `51.136795` px inside
    a `78.0` px slot.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-action-icon-richness-v33/pr08-bottom-action-icon-richness-v33-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-action-icon-richness-v33/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-action-icon-richness-v33/pr08-bottom-action-icon-richness-v33-comparison-board.png`
  - action-deck crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-action-icon-richness-v33/pr08-bottom-action-icon-richness-v33-v33-action-deck-crop.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-action-icon-richness-v33/pr08-bottom-action-icon-richness-v33-ui-demo-new-parity-1672x941.png`
  - bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-action-icon-richness-v33/pr08-bottom-action-icon-richness-v33-ui-demo-new-bottom-deck-no-command-hints.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-action-icon-richness-v33/pr08-bottom-action-icon-richness-v33-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V32 action-deck crop:
    `3.280267489711934`
  - changed ratio versus V32 action-deck crop:
    `0.14309876543209876`
  - mean absolute RGB diff versus V32 bottom-deck crop:
    `1.2014001929221678`
  - changed ratio versus V32 bottom-deck crop:
    `0.052410019895098574`
  - mean absolute RGB diff versus V32 full screen:
    `0.17520681958010667`
  - changed ratio versus V32 full screen:
    `0.007654676130961158`
  - map-stage changed ratio versus V32: `0.0`
  - right-panel changed ratio versus V32: `0.0`
  - nav changed ratio versus V32: `0.0`
  - inventory changed ratio versus V32: `0.0`
  - V32 resized action/reference mean abs diff:
    `17.67738414743246`
  - V33 resized action/reference mean abs diff:
    `17.994499910538558`
- Key hashes:
  - V33 comparison board:
    `19fca31ad4d2aa19cdf6129420f6f3e58f01e28d26173252bccc23fafa68a641`
  - V33 full screen file:
    `c73512c55ab5965e7d2ac96f2e52b15ca8e11e826c9bf289980483c4cf61d657`
  - V33 bottom deck crop:
    `e259e62708d4f9f74f8b1371bcc6c05dcf9f12781f2d0eed1460f4475fabe62b`
  - V33 action crop:
    `c5481bb4f630e4076a63a54a70572b297027d322ccbb653d46579d97af1c6696`
  - V33 action diff:
    `1a5e0652227b67ec4c7d5647033642f6badeced5d9924f2748c88c5c64bd255b`
- Focused golden actual hashes from retained V33 expected failure:
  - `ui-demo-new-parity-1672x941=f4355bd8c149d52de0cfca568722e69d7eccfb04b5f4b511f59a989cb93be7b4`
  - `ui-demo-new-parity-1280x800=72ad2a98ee9c7c1b7618d061abfd409830732d91fe148b11538cad8fae3e6d05`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=df5e01035731975f0803136d06832b0c1246685d061d88a8174ea84672a71e7f`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED, expected failure on V32 action icon richness:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom action slots render large readable icon subjects with material accents'`
  - GREEN focused V33 test:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom action slots render large readable icon subjects with material accents'`
  - broader focused client verification, smoke and anti-bloat lint:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL runtime archive, action-deck comparison board and metrics
    generation.
- Result:
  - PASS: V33 RED proved the old action icon subjects were too small for
    first-glance readability.
  - PASS: focused V33 test.
  - PASS: broader `TileRendererCanvasTest`, `DemoShellLayoutTest`,
    `FoundationViewportSupportTest`, `:client:clientSmoke` and
    `maintainabilityLint`; three render/audio smoke subtests were skipped by
    existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime bottom/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V33 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V33 is accepted-forward only.
  - The action slot subjects have more first-glance weight and material cueing,
    and the changed pixels are isolated to bottom/full surfaces.
  - It does not close the director-grade gap: the reference has authored
    sword/shield/compass subjects and cleaner rectangular slot silhouettes,
    while V33 still depends on the existing circular combat icon resources.
- Next action:
  - Do not continue by only making the same icons larger.
  - Switch technique to resource-level action icon / slot silhouette iteration,
    or a larger map-stage composition move if first-screen map quality is again
    the dominant blocker.

## V34 Action Icon Resource Subjects

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: runtime visual resource and source sheet update for starter
    hotbar icons
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest key/schema, action order
    or golden expected-hash changes
  - deletion budget: no deletion; reused existing `talent.vanguard.*.icon`
    keys, `ActionPanelEntryModel.icon`, r08 sheet plan cells and current action
    deck render path
- Production/resources:
  - Replaced four real Vanguard starter hotbar PNGs with transparent
    subject-first icons:
    `talent_vanguard_power_strike_icon.png`,
    `talent_vanguard_shield_bash_icon.png`,
    `talent_vanguard_guard_stance_icon.png`,
    `talent_vanguard_charge_icon.png`.
  - Reused original metal/crack texture inside tighter subject masks so the
    icons do not regress into flat vector line art.
  - Updated `r08-skills-vanguard-berserker.png`, its contact sheet, and both
    `dark-v1-pr00-sprite-map-report.jsonl` and
    `dark-v1-pr02-2-sprite-map-report.jsonl`.
- Tests:
  - Added
    `ManifestResolveTest.vanguard starter hotbar icons are subject first without circular medallion fill`.
  - Kept the existing transparent socket background test as a paired guard.
  - RED proved the old `talent.vanguard.power_strike.icon` had
    `alphaCoverage=0.4703369140625`, above the new subject-first range.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-comparison-board.png`
  - resource preview:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-resource-icon-preview.png`
  - action-deck crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-v34-action-deck-crop.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-ui-demo-new-parity-1672x941.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V33 action-deck crop:
    `2.8436076817558296`
  - changed ratio versus V33 action-deck crop:
    `0.08986419753086419`
  - mean absolute RGB diff versus V33 bottom-deck crop:
    `1.0414732420972248`
  - changed ratio versus V33 bottom-deck crop:
    `0.03291282329535178`
  - mean absolute RGB diff versus V33 full screen:
    `0.15168612406293483`
  - changed ratio versus V33 full screen:
    `0.004802326497821212`
  - map-stage changed ratio versus V33: `0.0`
  - right-panel changed ratio versus V33: `0.0`
  - nav changed ratio versus V33: `0.0`
  - V33 resized action/reference mean abs diff:
    `17.994499910538558`
  - V34 resized action/reference mean abs diff:
    `17.468430846305242`
  - subject alpha coverage:
    - `talent.vanguard.power_strike.icon=0.223876953125`
    - `talent.vanguard.shield_bash.icon=0.3670654296875`
    - `talent.vanguard.guard_stance.icon=0.39764404296875`
    - `talent.vanguard.charge.icon=0.255859375`
- Key hashes:
  - V34 comparison board:
    `a91e61acd6a3ea2a68fa712fc98f86f2dcb6dfce3f33be095cba6f32d85f56a4`
  - V34 resource icon preview:
    `05a6c8c323283f98d057700fdfa1723ce842e597ae32fa8cb51a2b5f97a131ea`
  - V34 full screen file:
    `b55796680024e4f51b157d8962d796d3c2835f0ebc8f1892c7c6d655d1c4f4d4`
  - V34 bottom deck crop:
    `7148faaae9bb8935aa4876c94d83160762bec1bf17f1950ee19bde2bb400e729`
  - V34 action crop:
    `b3572b8f8f1fb66bbd763c88731b73b39e7930ab94f0bcef12de03b9cc83bcdb`
  - V34 action diff:
    `c56877a6bd91dc431f4ec4531cd9e405c1782c6845558a1d4f2c22cae93a42f6`
- Focused golden actual hashes from retained V34 expected failure:
  - `ui-demo-new-parity-1672x941=f54b9619d359aa82411f7d7073e2e4582e7ef27d8e8637bd3ce81ebd127f27a9`
  - `ui-demo-new-parity-1280x800=0674c3de7cd177a1ac64251d44581e8e817993f7106baa2ddaae75e6c7cfbb98`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=9a14399183d5593fd2df8e2afddd3c06b395e7f6d23862d0ac16a6abd1bcf620`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED, expected failure on circular medallion fill:
    `./gradlew :client:test --tests 'com.ktome.client.assets.ManifestResolveTest.vanguard starter hotbar icons are subject first without circular medallion fill'`
  - GREEN focused V34 resource tests:
    `./gradlew :client:test --tests 'com.ktome.client.assets.ManifestResolveTest.vanguard starter hotbar icons keep transparent socket background' --tests 'com.ktome.client.assets.ManifestResolveTest.vanguard starter hotbar icons are subject first without circular medallion fill'`
  - resource gates:
    `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint resourcePipelineLint`
    and
    `./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl`
  - broader focused client verification, smoke and anti-bloat lint:
    `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL resource update, runtime archive, action-deck comparison
    board and metrics generation.
- Result:
  - PASS: V34 RED proved the old circular medallion fill exceeded the
    subject-first action-icon coverage range.
  - PASS: focused V34 resource tests.
  - PASS: resource key, sheet, sprite-map and resource authority gates.
  - PASS: broader `ManifestResolveTest`, `TileRendererCanvasTest`,
    `DemoShellLayoutTest`, `FoundationViewportSupportTest`,
    `:client:clientSmoke` and `maintainabilityLint`; three render/audio smoke
    subtests were skipped by existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime bottom/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V34 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V34 is accepted-forward only.
  - The hotbar now reads as authored action subjects rather than circular
    medallion badges, and the reference-distance check improves directionally.
  - It does not close the director-grade gap: slot silhouette, bottom-deck
    composition and final illustration quality remain below the target.
- Next action:
  - Do not continue by only re-running the same icon mask pass.
  - Move to action-slot silhouette/chrome resource work, stronger bottom-deck
    composition, or return to map-stage composition/resource work if the
    first-screen map gap becomes dominant again.

## V35 Action Slot Silhouette Chrome

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: bottom action slot renderer presentation change
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest key/schema, action order,
    resource file or golden expected-hash changes
  - deletion budget: no deletion; reused `ActionPanelEntryModel.icon`, existing
    action deck layout, V34 icon resources and existing label plinth ownership
- Production:
  - Added `drawActionSlotSilhouetteChrome` in `DemoShellRenderer`.
  - Drew a low-alpha rectangular hollow socket stage behind each filled action
    icon subject.
  - Added restrained brass top/bottom lips and paired iron side jambs.
  - Kept labels on the shared command plinth and preserved the existing guard
    against full-height independent slot wells.
- Tests:
  - Added
    `TileRendererCanvasTest.bottom action slots use rectangular hollow command sockets`.
  - RED proved the old renderer had `0/4` wide rectangular hollow socket
    backings for filled action slots.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-comparison-board.png`
  - slot closeup:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-v35-slot-closeup.png`
  - action-deck crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-v35-action-deck-crop.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-ui-demo-new-parity-1672x941.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V34 action-deck crop:
    `0.04759122085048011`
  - changed ratio versus V34 action-deck crop:
    `0.036905349794238686`
  - mean absolute RGB diff versus V34 bottom-deck crop:
    `0.017430316914852997`
  - changed ratio versus V34 bottom-deck crop:
    `0.013516609392898053`
  - mean absolute RGB diff versus V34 full screen:
    `0.002518296393093641`
  - changed ratio versus V34 full screen:
    `0.0019483879004825366`
  - map-stage changed ratio versus V34: `0.0`
  - right-panel changed ratio versus V34: `0.0`
  - nav changed ratio versus V34: `0.0`
  - V34 resized action/reference mean abs diff:
    `17.468430846305242`
  - V35 resized action/reference mean abs diff:
    `17.466652352835926`
- Key hashes:
  - V35 comparison board:
    `33c429e7304e2566ea1c69612a691a7de57fc2f46f87c66470715505501bfe4b`
  - V35 slot closeup:
    `87c558abc3526060ede732da75c7db0f03a0e26815a7f0958f99f305356bc46b`
  - V35 full screen file:
    `ba96f87a0bc8de949bfceeea683b8266c08c3e71df4e4bc7bd39d6100f560e6c`
  - V35 bottom deck crop:
    `a4e225962e494fc62f3a6a9eda14f5fdf77461ab1d3a0ea68e8a564b7a51ca76`
  - V35 action crop:
    `0ed591b36100ef661c43ad13f2b2faab58c32bc85c067a928cb804e66cacd2f3`
  - V35 action diff:
    `f540b3320c29ad39d486b0c2aed87922faa77eb270afcc00feb8d2a9f0a2015b`
- Focused golden actual hashes from retained V35 expected failure:
  - `ui-demo-new-parity-1672x941=d3ee84c1c31c1a165e6a140013988e236316d97bc2e98f39171aea45462e355b`
  - `ui-demo-new-parity-1280x800=98f7df053ce9bf19bc6c90912d4c1b608f77cc042793446675c4530e0f091da4`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=beaf0b35e8230dac1bc2c6981ac61cb3126febcc43a096b91bd777160a79d170`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED, expected failure on missing rectangular socket backing:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom action slots use rectangular hollow command sockets'`
  - GREEN focused V35 test:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom action slots use rectangular hollow command sockets'`
  - broader focused client verification, smoke and anti-bloat lint:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL runtime archive, action-deck comparison board and metrics
    generation.
- Result:
  - PASS: V35 RED proved the old renderer lacked wide rectangular hollow
    socket backing.
  - PASS: focused V35 test.
  - PASS: broader `TileRendererCanvasTest`, `DemoShellLayoutTest`,
    `FoundationViewportSupportTest`, `:client:clientSmoke` and
    `maintainabilityLint`; three render/audio smoke subtests were skipped by
    existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime bottom/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V35 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V35 is accepted-forward only.
  - The action slot now has a clearer rectangular command bay silhouette behind
    the V34 resource subjects.
  - It does not close the director-grade gap because the measurable runtime
    delta is very small and the bottom deck still needs a larger composition
    move.
- Next action:
  - Do not continue tuning socket alpha or lip widths.
  - Move to larger bottom-deck composition, authored hero/log/action
    unification, or return to map-stage composition/resource work if the
    first-screen map gap becomes dominant again.

## V36 Bottom Command Shelf Bridges

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: bottom HUD composition renderer change
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest key/schema, action order,
    resource file or golden expected-hash changes
  - deletion budget: no deletion; reused the existing bottom-deck layout,
    hero/action/log render order and current V35 slot/action treatments
- Production:
  - Added `drawBottomHudBridgePlates` under `drawBottomHudCohesionRails`.
  - Drew a wide forged bridge plate in each hero/action and action/log gutter.
  - Added small brass rungs and restrained side edges so the plates read as
    mechanical joins rather than new content panels.
  - Kept hero card, action deck, log deck, labels, hotkeys and log text
    ownership unchanged.
- Tests:
  - Added
    `TileRendererCanvasTest.bottom hud uses inter panel bridge plates for one command shelf`.
  - RED proved the old renderer had `0/2` wide inter-panel bridge plates.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-comparison-board.png`
  - bridge closeup:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-v36-bottom-bridge-closeup.png`
  - action-deck crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-v36-action-deck-crop.png`
  - full screen:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-ui-demo-new-parity-1672x941.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-runtime-metrics.json`
- Key metrics:
  - mean absolute RGB diff versus V35 action-deck crop:
    `0.15433196159122084`
  - changed ratio versus V35 action-deck crop:
    `0.04869958847736625`
  - mean absolute RGB diff versus V35 bottom-deck crop:
    `0.1108337854946645`
  - changed ratio versus V35 bottom-deck crop:
    `0.0350244167118828`
  - mean absolute RGB diff versus V35 full screen:
    `0.016572843627279`
  - changed ratio versus V35 full screen:
    `0.005114875755711373`
  - map-stage changed ratio versus V35: `0.0`
  - right-panel changed ratio versus V35: `0.0`
  - nav changed ratio versus V35: `0.0`
  - V35 resized action/reference mean abs diff:
    `17.466652352835926`
  - V36 resized action/reference mean abs diff:
    `17.41089282519234`
- Key hashes:
  - V36 comparison board:
    `45904dd14a17ee128b6fca5a63db9848d6dc59de421e290cde1d065ce101498d`
  - V36 bridge closeup:
    `af1dfc46af2777b2880aa2e9ce8fc22fd7163b6d1a0ca989328769705c0ecdfd`
  - V36 full screen file:
    `3d5bb0c4c7e904c166e30afadc821d13c9f1ee184cfa2a5c8ec26829494b08ba`
  - V36 bottom deck crop:
    `f9b22db39ff64fb45768d66feccf6d6bf69ae0a69cdc77ec2432f092be429907`
  - V36 action crop:
    `3a3cc4488a4fdc605bf366171a996eb9468efc7ae390970087cb0c1d11326f14`
  - V36 action diff:
    `8637fb573b888a7812c1f4a6d26fd9a7b2bf2d53fcd463a348f427dc71867636`
- Focused golden actual hashes from retained V36 expected failure:
  - `ui-demo-new-parity-1672x941=586c7fdd8cf3a57557edfff4905ba3c5dfaca9979e1b0d9d28d72df080c31ba9`
  - `ui-demo-new-parity-1280x800=aa0c249be84b5e2d81b634a8591cf67c32c49019870929c134ef9dc37236db6c`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED, expected failure on missing inter-panel bridge plates:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom hud uses inter panel bridge plates for one command shelf'`
  - GREEN focused V36 test:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom hud uses inter panel bridge plates for one command shelf'`
  - broader focused client verification, smoke and anti-bloat lint:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL runtime archive, comparison board and metrics generation.
- Result:
  - PASS: V36 RED proved the old renderer lacked wide inter-panel bridge
    plates.
  - PASS: focused V36 test.
  - PASS: broader `TileRendererCanvasTest`, `DemoShellLayoutTest`,
    `FoundationViewportSupportTest`, `:client:clientSmoke` and
    `maintainabilityLint`; three render/audio smoke subtests were skipped by
    existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime bottom/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V36 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V36 is accepted-forward only.
  - It moves bottom HUD composition more than V35 by filling the hero/action/log
    gutters with forged bridge plates and rungs.
  - It does not close the director-grade gap because the bottom deck still
    reads as locally improved panels rather than a final authored HUD object.
- Next action:
  - Do not continue by only adding more bridge plates or tuning bridge alpha.
  - Move to a larger bottom-deck composition reset, authored hero/log/action
    unification, or return to map-stage composition/resource work if the
    first-screen map again dominates the gap.

## V37 Ground Relief Authority

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: map-stage renderer authority cleanup
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest key/schema, resource file
    or golden expected-hash changes
  - deletion budget: removed one repeated per-tile PR-08 ground relief restamp
    path from `MAP_ROOM_COMPOSITOR`
- Production:
  - Removed `drawPr08GroundFamilyReliefRepaint` from `renderWarmMapOverlay`.
  - Removed the now-unused `isPr08RuinsGroundFamily` predicate.
  - Kept PR-08 wall-family relief repaint unchanged.
  - Kept actor, loot marker, telegraph and cursor layering unchanged.
- Tests:
  - Updated
    `TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps`.
  - RED proved the old compositor repeated `60` PR-08 ground-family restamps
    over `60` base floor draws.
  - GREEN proves the repeated PR-08 ground-family restamp count is now `0`.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-comparison-board.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-ui-demo-new-map-stage-crop.png`
  - map diff:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-v37-v36-map-stage-diff.png`
  - room closeup:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-v37-room-closeup.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-runtime-metrics.json`
- Key metrics:
  - V37 vs V36 map-stage crop changed ratio:
    `0.24177112682805282`
  - V37 vs V36 map-stage mean absolute RGB diff:
    `0.5827528926200652`
  - V37 vs V36 full-screen changed ratio:
    `0.11094481718013514`
  - V37 vs V36 full-screen mean absolute RGB diff:
    `0.2676077360099117`
  - V36 resized map/reference mean absolute RGB diff:
    `15.910307374823503`
  - V37 resized map/reference mean absolute RGB diff:
    `15.49837559657294`
  - V36 sampled room dark-line contrast:
    `4.290575236343592`
  - V37 sampled room dark-line contrast:
    `4.702899353810177`
- Key hashes:
  - V37 comparison board:
    `7b03ea986feacf737a4dcec51e34f3ff4391ec39c9b4652542ac278c8bbe92a2`
  - V37 map-stage crop:
    `2dc5d7177d00b7add9aee81575f2a5b3e9684f597aaa79f8d579121a584327ae`
  - V37 full-screen file:
    `7d9f075161ba2ff7047548dea402f4eacf56e267727940a2a3e5f769450d964e`
  - V37 room closeup:
    `75a9b9f8e896e70dfe06bdf02bc3d37ff24d0c3755528811ebebc9e2b3002164`
  - V37 map diff:
    `21ae7ad6b14d15854b769aff10a217b321b2991b2c72ad62a93f5ddd6f460bd5`
- Focused golden actual hashes from retained V37 expected failure:
  - `ui-demo-new-parity-1672x941=fc0c4eb700c1d46ce130d9204c3a23acc3e1d5c3b865e851f93c4353a5176ce7`
  - `ui-demo-new-parity-1280x800=5cbc2d4fd0531eb979698eefcdc269a10033f7a22b629956965a578396f74645`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=6e8a067678d8b31f158708dab61ab35c57d0ba7989775dea932f74b6cf22dcd3`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED, expected failure on repeated ground-family restamps:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps'`
  - GREEN focused V37 test:
    `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps'`
  - broader focused client verification, smoke and anti-bloat lint:
    `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL runtime archive, map-stage comparison board and metrics
    generation.
- Result:
  - PASS: V37 RED proved the old compositor repeated `60` ground-family
    restamps over `60` base floor draws.
  - PASS: focused V37 test.
  - PASS: broader `TileRendererCanvasTest`, `DemoShellLayoutTest`,
    `FoundationViewportSupportTest`, `:client:clientSmoke` and
    `maintainabilityLint`; three render/audio smoke subtests were skipped by
    existing environment assumptions.
  - EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V37 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V37 is accepted-forward only.
  - It removes a second floor-authority pass and reduces repeated ground
    texture restamp noise.
  - It does not close the director-grade gap because the room still reads
    square-lattice-first; sampled dark-line contrast worsened even though the
    resized reference-distance metric improved.
- Next action:
  - Do not continue by only toggling ground repaint alpha or adding another
    per-grid mask.
  - Move to stronger non-grid-aligned room material resources, a wall/floor
    structural resource decision, or a larger map-stage composition change that
    alters room silhouette and wall thickness.

## V38 Room Breakup Resource

- Verdict: accepted-forward only.
- Change shape:
  - owner modules: `client`, `tools`
  - change type: room-scale material resource replacement plus focused resource
    quality test
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest key/schema, or resource
    key changes
  - resource key preserved: `tileset.ruins.room_breakup_01`
- Production:
  - Replaced
    `client/src/main/resources/dark-v1/tiles/tileset_ruins_room_breakup_01.png`
    with a lower-alpha, off-grid stone breakup texture.
  - Updated the matching cell in
    `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`.
  - Updated the matching contact cell in
    `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`.
  - Refreshed the r02 raw-sheet hash and `tileset.ruins.room_breakup_01`
    cell/output hashes in the PR00, PR02-2 and PR08 sprite map reports.
- Tests:
  - Added focused resource assertions to
    `DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture`.
  - RED proved the previous resource failed the new room-scale plate
    requirement with `largestMaterialIslandLongSpan=94`,
    `largestMaterialIslandShortSpan=40` and
    `largestMaterialIslandAxisDistance=9.767090614655844`.
  - GREEN final resource metrics:
    - alpha coverage: `0.4998779296875`
    - max alpha: `114`
    - unique color count: `5371`
    - mean weighted edge: `2.321037265637058`
    - largest material island: `128x78`
    - largest island axis distance: `14.510029721154714`
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-comparison-board.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-ui-demo-new-map-stage-crop.png`
  - room closeup:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-v38-room-closeup.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-runtime-metrics.json`
- Key metrics:
  - V38 vs V37 map-stage crop changed ratio:
    `0.24689494566154907`
  - V38 vs V37 map-stage mean absolute RGB diff:
    `0.7000175697519151`
  - V38 vs V37 full-screen changed ratio:
    `0.11324500175421648`
  - V38 vs V37 full-screen mean absolute RGB diff:
    `0.3213815683542738`
  - V37 resized map/reference mean absolute RGB diff:
    `15.49837559657294`
  - V38 resized map/reference mean absolute RGB diff:
    `15.575196621496431`
- Key hashes:
  - V38 comparison board:
    `9573445c0fa3f72e21072c63907fa4b87177e95e17d8ce1401fa104acb60a90b`
  - V38 room material resource output hash:
    `4f81f2ec8f7d09311d36135f6ec50a07518b9b1abc0abedd468966f6e290971f`
  - V38 raw sheet hash:
    `58ee5e11c21f97cf6d4e96c70e6147691326545c8527be367d20d805b19c00e0`
  - V38 room-breakup cell hash:
    `04d3657346ada528c7c41d1134cd73072dbea5fe0eaed96af72662a9b302637c`
- Focused golden actual hashes from retained V38 expected failure:
  - `ui-demo-new-parity-1672x941=bc5e73e6ca4dd4e6cf46528f52a4c40b051f4b4b42a8fdd77edafb73dcfb32cb`
  - `ui-demo-new-parity-1280x800=76b41326b4c16819c239ee3ab74535ff95c8196609461b7e4918f2e346f9320f`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=841f3083a7c37860c4468f0fd49705266842519c90532784a08bed5fe8d21e6f`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED, expected failure on previous resource:
    `./gradlew :tools:test --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture'`
  - GREEN and resource/client verification:
    `./gradlew resourcePipelineLint spriteSheetMapLint :tools:test --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture' :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL resource generation, contact-sheet update, sprite map
    report hash refresh and evidence board generation.
- Result:
  - PASS: focused room material resource test.
  - PASS: `resourcePipelineLint`, `spriteSheetMapLint`,
    `ManifestResolveTest`, `TileRendererCanvasTest`, `:client:clientSmoke` and
    `maintainabilityLint`; three smoke subtests were skipped by existing
    environment assumptions.
  - EXPECTED FAIL: PR02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V38 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V38 is accepted-forward only.
  - It gives the room material resource a stronger off-grid contract and adds
    validation so future resources cannot regress into small grid-aligned
    flecks.
  - It does not close the director-grade gap because the map/reference distance
    slightly worsens and the visible room still reads too rectangular and
    grid-led.
- Next action:
  - Do not continue by tuning `room_breakup_01` alpha, color, holes or noise.
  - Move to wall/floor silhouette, room boundary thickness, or a larger
    map-stage composition decision that changes how the room reads before grid
    detail.

## V39 Room Boundary Compression

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: renderer-only structural room-boundary compression
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest key/schema, resource key
    or resource file changes
  - authority: private `TileRenderer` compositor pass over existing visible
    floor geometry; no new manifest/resource authority
- Production:
  - Added `drawVisibleRoomBoundaryCompression` in `TileRenderer`.
  - Invoked it after `drawVisibleRoomSilhouettePressure` and before
    `drawVisibleRoomOuterShadows`.
  - The pass derives visible floor bounds and draws a dark screen-top interior
    shoulder, lower warm screen-bottom shelf, asymmetric side shoulders,
    compact corner shoulders and restrained worn lip highlights.
  - The pass is gated by visible floor area size and does not alter map data,
    terrain material selection, player state, event output, save/replay data or
    resource manifests.
- Tests:
  - Added focused render coverage in
    `TileRendererCanvasTest.render canvas compresses visible room boundary with thick interior stone shoulders`.
  - RED proved the previous renderer had no continuous room-boundary
    compression shoulder matching the target dimensions and alpha.
  - GREEN verifies top, bottom, side and corner shoulder geometry, including
    that V39 boundary compression shapes do not cover the player focal center.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-comparison-board.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-ui-demo-new-map-stage-crop.png`
  - room closeup:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-v39-room-closeup.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-runtime-metrics.json`
- Key metrics:
  - V39 vs V38 map-stage changed ratio:
    `0.020001725030188027`
  - V39 vs V38 map-stage mean absolute RGB diff:
    `0.025554884710482433`
  - V39 vs V38 full-screen changed ratio:
    `0.009096343348468747`
  - V39 vs V38 full-screen mean absolute RGB diff:
    `0.011519725613424925`
  - V39 vs V38 room-closeup changed ratio:
    `0.05643462190050824`
  - V39 vs V38 room-closeup mean absolute RGB diff:
    `0.07160069818779198`
  - V38 resized map/reference mean absolute RGB diff:
    `15.575196621496431`
  - V39 resized map/reference mean absolute RGB diff:
    `15.568107226598688`
- Focused golden actual hashes from retained V39 expected failure:
  - `ui-demo-new-parity-1672x941=df28ecec271b075e5cbfc0e2e27b353ff6450149ec329218553c30189d79ed2f`
  - `ui-demo-new-parity-1280x800=909a06248417de6802eca42b8f19bbe24c6bf57e9dc963414e79a7672e335630`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=33fc0f4a2b33ab84befeb673261f90d951403b21b52431b52b98f3b741ddc9cb`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED and GREEN focused render test:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas compresses visible room boundary with thick interior stone shoulders'`
  - client owner verification:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL evidence generation for crop copies, room closeup, diff
    images, metrics JSON and comparison board.
- Result:
  - PASS: focused room-boundary compression render test.
  - PASS: `TileRendererCanvasTest`, `ManifestResolveTest`,
    `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
    skipped by existing environment assumptions.
  - EXPECTED FAIL: PR02-1 golden hash gate failed because runtime map/full
    pixels changed and baselines remain intentionally unchanged.
  - PASS: V39 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V39 is accepted-forward only.
  - It adds useful perimeter mass and moves the map/reference metric in the
    right direction, but the delta is still too small for final art direction.
  - The first read remains rectangular and grid-led compared with
    `UI/UI-demo-new.png`.
- Next action:
  - Do not continue by only tuning V39 shoulder alpha, width or lip colors.
  - Move to a larger map-stage silhouette/composition pass: wall/floor edge
    breakups, non-rectangular aperture pressure, heavier corner wall mass, or a
    stronger layout decision that changes the room shape before grid detail.

## V40 Asymmetric Room Edge Mass

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: renderer-only room edge silhouette/composition pass
  - stable contracts touched: no gameplay rules, core visibility,
    save/replay/profile/schema/content-pack, manifest key/schema, resource key
    or resource file changes
  - authority: private `TileRenderer` pass over existing visible floor bounds;
    no new map, terrain, resource or manifest authority
- Production:
  - Added `drawVisibleRoomAsymmetricEdgeMass` in `TileRenderer`.
  - Invoked it after `drawVisibleRoomBoundaryCompression` and before
    `drawVisibleRoomOuterShadows`.
  - The pass draws a broad upper-left masonry mass, offset lower-right plinth,
    thicker left side buttress, asymmetric right side buttress and restrained
    worn-stone lip highlights.
  - The pass is size-gated and does not alter cell materials, map data, actors,
    loot, targeting affordances, resource files or persistence.
- Tests:
  - Added focused render coverage in
    `TileRendererCanvasTest.render canvas offsets visible room edges with asymmetric masonry mass`.
  - RED proved the previous renderer had no broad offset masonry mass on the
    upper-left room edge.
  - GREEN verifies all four V40 mass regions and asserts that the new edge mass
    shapes do not cover the player focal center.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-comparison-board.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-ui-demo-new-map-stage-crop.png`
  - room closeup:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-v40-room-closeup.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-runtime-metrics.json`
- Key metrics:
  - V40 vs V39 map-stage changed ratio:
    `0.01898826979472141`
  - V40 vs V39 map-stage mean absolute RGB diff:
    `0.05474910394265233`
  - V40 vs V39 full-screen changed ratio:
    `0.008689886306433652`
  - V40 vs V39 full-screen mean absolute RGB diff:
    `0.02508418332324871`
  - V40 vs V39 room-closeup changed ratio:
    `0.023429847528107194`
  - V40 vs V39 room-closeup mean absolute RGB diff:
    `0.05266440782381026`
  - V39 resized map/reference mean absolute RGB diff:
    `15.568107226598688`
  - V40 resized map/reference mean absolute RGB diff:
    `15.524223257240335`
- PR02-1 golden hashes rebaselined to accepted V40 artifacts:
  - `ui-demo-new-parity-1672x941=531b3a3787bd706a7165a32f9d5b4bffa0053625382dfd34aab69df08b168741`
  - `ui-demo-new-parity-1280x800=d0ebeb93706978b91fb1fb5e453012e5cdf7b37439f7119b70825f2355ce4023`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=b9be6feb94e50cccb615e7ec2459f104bfb3b2fd906f87e39029cac7e778045c`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - RED and GREEN focused render test:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas offsets visible room edges with asymmetric masonry mass'`
  - client owner verification:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL evidence generation for crop copies, room closeup, diff
    images, metrics JSON and comparison board.
- Result:
  - PASS: focused asymmetric room edge mass render test.
  - PASS: `TileRendererCanvasTest`, `ManifestResolveTest`,
    `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
    skipped by existing environment assumptions.
  - PASS: PR02-1 golden hash gate after rebaselining
    `GoldenScreenshotHarnessTest` expected hashes to the accepted V40 artifacts.
  - PASS: V40 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V40 is accepted-forward only.
  - It is stronger than V39 and moves the map/reference metric in the right
    direction.
  - It still does not close the gap because the map remains a compact
    rectangular tactical room inside a large framed stage.
- Next action:
  - Do not continue by only adding more dark rectangles to the same perimeter.
  - Test a larger map-stage composition decision: scale/proportion of the
    visible room inside the stage, aperture proportion, or authored wall/floor
    resource forms that reshape the silhouette before compositor overlays.

## V41 Runtime Map Scale

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: runtime map scale/aperture composition pass
  - stable contracts touched: no gameplay rules, core/game logic,
    save/replay/profile/schema/content-pack, manifest key/schema, resource key
    or resource file changes
  - authority: existing Foundation runtime shell and golden layout helper; no
    second render, map, terrain, manifest or resource authority
- Production:
  - Increased Foundation runtime tile scale from `32f` to `42f` in
    `FoundationGameScreen`.
  - Updated the PR02 golden layout helpers to use the same `42f` runtime scale
    for map-stage and log crops.
  - A `44f` intermediate probe was rejected after visual review because it read
    over-zoomed and left too little stage breathing room.
- Tests:
  - Added focused coverage in
    `FoundationViewportSupportTest.usesDirectorGradePresentationCellScaleForFoundationRuntimeShell`.
  - The test guards the production scale constants and verifies the runtime
    shell remains cell-aligned at `19` visible columns and `13` visible rows.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-comparison-board.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-ui-demo-new-map-stage-crop.png`
  - room closeup:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-v41-room-closeup.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-runtime-metrics.json`
- Key metrics:
  - V41 vs V40 map-stage changed ratio:
    `0.5414409752170662`
  - V41 vs V40 map-stage mean absolute RGB diff:
    `5.72072991138456`
  - V41 vs V40 full-screen changed ratio:
    `0.24834700054406134`
  - V41 vs V40 full-screen mean absolute RGB diff:
    `2.6298510017677756`
  - V41 vs V40 room-closeup changed ratio:
    `0.9548390574464808`
  - V41 vs V40 room-closeup mean absolute RGB diff:
    `12.182358437291441`
  - V40 resized map/reference mean absolute RGB diff:
    `15.501569326917759`
  - V41 resized map/reference mean absolute RGB diff:
    `15.069590488634477`
- PR02-1 golden hashes rebaselined to accepted V41 artifacts:
  - `ui-demo-new-parity-1672x941=22a380bfbe2aefeec40a969e50c7d15fcf54549065e2460b0d36ff3efe35f97a`
  - `ui-demo-new-parity-1280x800=03df3a3869707f74bfaf0f91c3874d8c67095e97e164a9816e8e981fd0db6c1e`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=39155e93b614bd11c08dcd2a056fa400683d7b7bf3d90f188d01ac73f119132f`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - focused viewport support test:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.screen.FoundationViewportSupportTest.usesDirectorGradePresentationCellScaleForFoundationRuntimeShell'`
  - focused golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - client owner verification:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.screen.FoundationViewportSupportTest --tests com.ktome.client.render.TileRendererCanvasTest :client:clientSmoke maintainabilityLint`
  - local Python/PIL evidence generation for crop copies, room closeup, diff
    images, metrics JSON and comparison board.
- Result:
  - PASS: focused runtime scale support test.
  - PASS: PR02-1 golden hash gate after rebaselining
    `GoldenScreenshotHarnessTest` expected hashes to the accepted V41 artifacts.
  - PASS: `FoundationViewportSupportTest`, `TileRendererCanvasTest`,
    `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
    skipped by existing environment assumptions.
  - PASS: V41 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V41 is accepted-forward only.
  - It materially improves first-read map presence and should remain.
  - It still does not close the art-direction gap because the enlarged map
    keeps a rectangular, grid-led room silhouette.
- Next action:
  - Do not continue by only increasing tile scale again.
  - Move to authored wall/floor resource families, non-rectangular aperture
    pressure, or silhouette breakups that change the first read before grid
    detail.

## V42 Runtime Aperture Corner Shelves

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - change type: runtime-only renderer silhouette/aperture refinement
  - stable contracts touched: no gameplay rules, core/game logic,
    save/replay/profile/schema/content-pack, manifest key/schema, resource key
    or resource file changes
  - authority: private `TileRenderer` compositor pass; no new map, terrain,
    manifest, resource or second render authority
- Production:
  - Added `drawDirectorScaleApertureCornerShelves` for hidden-stage diagonal
    aperture shelves where enough hidden space surrounds the visible room.
  - Added `drawVisibleRoomRuntimeCornerApertureShelves`, gated to runtime
    `cellSize >= 40f`, so V41/V42 canonical screenshots get visible-room corner
    pressure after the `42f` scale increase.
  - Preserved existing V39/V40 floor-boundary authority for
    `drawVisibleRoomBoundaryCompression` and `drawVisibleRoomAsymmetricEdgeMass`.
- Tests:
  - Added focused coverage in
    `TileRendererCanvasTest.render canvas gives director scaled runtime room diagonal aperture corner shelves`.
  - RED proved the old renderer had no runtime-scale diagonal shelves or
    visible interior corner shelves.
  - GREEN verifies the shelves frame the room without masking the playable
    focal center.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-decision.md`
  - runtime archive:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-comparison-board.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-ui-demo-new-map-stage-crop.png`
  - room closeup:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-v42-room-closeup.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-runtime-metrics.json`
- Key metrics:
  - V42 vs V41 map-stage changed ratio:
    `0.01817463055603473`
  - V42 vs V41 map-stage mean absolute RGB diff:
    `0.06548853493825006`
  - V42 vs V41 full-screen changed ratio:
    `0.008391478829912187`
  - V42 vs V41 full-screen mean absolute RGB diff:
    `0.030365370665093993`
  - V42 vs V41 room-closeup changed ratio:
    `0.015016171261358386`
  - V42 vs V41 room-closeup mean absolute RGB diff:
    `0.06750808563067798`
  - V41 resized map/reference mean absolute RGB diff:
    `14.975958190369429`
  - V42 resized map/reference mean absolute RGB diff:
    `14.942269628597481`
- PR02-1 golden hashes rebaselined to accepted V42 artifacts:
  - `ui-demo-new-parity-1672x941=f4f5f29fdb78f6c65208b58c60a26654b419d8316f72f287a9dbd46bc46f725f`
  - `ui-demo-new-parity-1280x800=d6e4f64eef1075f7cf1ce8b78a2955608a7a8921c94f8530250e51427ecd064f`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=03d2f81a14c71c628f6c0b64e163674dd65fe4766b098af2366f4560746eed84`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - focused RED and GREEN:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas gives director scaled runtime room diagonal aperture corner shelves'`
  - client owner verification:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint`
  - focused golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - local Python/PIL evidence generation for crop copies, room closeup, diff
    images, metrics JSON and comparison board.
- Result:
  - PASS: focused runtime aperture corner shelves test.
  - PASS: `TileRendererCanvasTest`, `FoundationViewportSupportTest`,
    `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
    skipped by existing environment assumptions.
  - PASS: PR02-1 golden hash gate after rebaselining
    `GoldenScreenshotHarnessTest` expected hashes to the accepted V42 artifacts.
  - PASS: V42 runtime archive uses repo-relative paths in generated JSON/TSV.
- Director verdict:
  - V42 is accepted-forward only.
  - It slightly improves the map/reference metric and adds real silhouette
    pressure to the enlarged runtime map.
  - It is still not closure: the map remains too grid-led and renderer-layered.
- Next action:
  - Do not continue by stacking more small dark shelves.
  - Move to authored wall/floor resource-family work or a stronger room-shape
    composition pass.

## V43 Resource Family Material Balance

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `tools` resource QA plus PR08 image assets
  - changed resource family:
    `tileset.ruins.ground_01*` and `tileset.ruins.wall_01*`
  - stable contracts touched: no gameplay rules, core/game logic,
    save/replay/profile/schema/content-pack, manifest key/schema or runtime
    terrain authority changes
  - authority: existing r02 sprite sheet and existing visual manifest keys; no
    second tile, wall, renderer or report authority
- Production:
  - Added floor resource QA metrics for outer-ring texture variance and exact
    outer-edge texture variance, in addition to existing value-drift,
    complexity and edge-density checks.
  - Regenerated multiple probes and rejected the ones that looked better in
    metrics but worse in runtime:
    - V43a: bright cracks/wall striping amplified lattice.
    - V43c: floor/wall cells became too vector-like.
    - V43f: repeated crack/dot motif worsened every-cell repetition.
  - Retained V43i: kept V42 floor/wall forms, corrected floor outer-ring value
    drift, and added subtle wall-family color microvariation so owner resource
    gates pass without changing resource keys.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-decision.md`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-comparison-board.png`
  - map-stage crop:
    `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-ui-demo-new-map-stage-crop.png`
  - contact sheet:
    `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-r02-contact.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-runtime-metrics.json`
- Key metrics:
  - V43i vs V42 map-stage changed ratio:
    `0.13658118183734883`
  - V43i vs V42 map-stage mean absolute RGB diff:
    `0.14303039886532556`
  - V42 resized map/reference mean absolute RGB diff:
    `23.694325609028123`
  - V43i resized map/reference mean absolute RGB diff:
    `23.661567126032903`
- PR02-1 golden hashes rebaselined to accepted V43i artifacts:
  - `ui-demo-new-parity-1672x941=c11e36a97750e71ef478e43e8de3e3f521876c77a577928db5bf49e55e062768`
  - `ui-demo-new-parity-1280x800=b90cabf9a34b5f437912990f379509e15f3343562917d0eb0bbb3cf07ed04cd9`
  - `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
  - `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=dadb4615be9c7fc23214b12d28df8e489534ff9f9b3764c631133e4674925b3b`
  - `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
  - `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`
- Commands run:
  - focused RED/GREEN resource tests:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:test --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 ruins floor family keeps material detail without bright tile-edge seams' --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 ruins wall family keeps dense masonry material in every authored piece'`
  - focused PR02-1 golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - PR08 resource owner gates:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:darkSpriteSheetLint :tools:spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles :tools:resourcePipelineLint :tools:darkManifestCoveragePr08OwnerScope`
  - client manifest/variant plus maintainability:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest maintainabilityLint`
  - local Python/PIL evidence generation for V43i crop copies, runtime metrics
    and comparison board.
- Result:
  - PASS: focused resource tests after V43i.
  - PASS: PR02-1 golden hash gate after rebaselining
    `GoldenScreenshotHarnessTest` expected hashes to V43i.
  - PASS: `darkSpriteSheetLint`, `spriteSheetMapLint`,
    `resourcePipelineLint`, `darkManifestCoveragePr08OwnerScope`,
    `ManifestResolveTest`, `TileRenderModelTerrainVariantTest` and
    `maintainabilityLint`.
  - PASS: V43i evidence JSON uses repo-relative paths.
- Director verdict:
  - V43i is accepted-forward only.
  - It is a resource hygiene and first-read stabilization step, not a full art
    direction leap.
  - It still does not close the map gap because the room is grid-led and less
    hand-authored than `UI/UI-demo-new.png`.
- Next action:
  - Do not generate another single-cell floor/wall batch with the same
    technique.
  - Next meaningful move should be room-scale composition/structural wall-floor
    authority, or a non-map UI density slice if map resource work keeps
    producing small deltas.

## V44 Runtime Right Empty Slot Quieting

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - changed runtime surface: `DemoShellRenderer` visual-only empty slot
    interior rendering
  - stable contracts touched: no gameplay rules, core/game logic,
    save/replay/profile/schema/content-pack, manifest key/schema or resource
    key changes
  - authority: existing `TileDemoSlotModel.visualOnly` and existing slot
    chrome; no second equipment, inventory, manifest or renderer authority
- Production:
  - Added a focused renderer test that fails when visual-only empty equipment
    sockets render high-alpha central pseudo-glyph strokes.
  - Replaced the visual-only empty-slot high-contrast motif path with a quiet
    dark socket branch that preserves slot depth through low-alpha frame, rail
    and corner details.
  - Kept filled equipment, backpack items, inscriptions, operation hints, nav,
    map-stage and bottom deck contracts unchanged.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-empty-slot-quieting-v44/pr08-right-empty-slot-quieting-v44-decision.md`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-empty-slot-quieting-v44/pr08-right-empty-slot-quieting-v44-comparison-board.png`
  - before right-panel crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-empty-slot-quieting-v44/pr08-right-empty-slot-quieting-v44-v43i-right-panel-grid.png`
  - after right-panel crop:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-empty-slot-quieting-v44/pr08-right-empty-slot-quieting-v44-v44-right-panel-grid.png`
  - after full screenshot:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-empty-slot-quieting-v44/pr08-right-empty-slot-quieting-v44-v44-full.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/runtime-right-empty-slot-quieting-v44/pr08-right-empty-slot-quieting-v44-runtime-metrics.json`
- Key metrics:
  - V44 vs V43i right-panel changed ratio:
    `0.032239365411436544`
  - V44 vs V43i right-panel mean absolute RGB diff:
    `0.3675447466294747`
  - V44 vs V43i right-panel max absolute channel diff:
    `149`
- PR02-1 golden hashes rebaselined to accepted V44 artifacts:
  - `ui-demo-new-parity-1672x941=256eac598c8592760648bba05d57305fd808efede15930aac0d097d93b4c118a`
  - `ui-demo-new-parity-1280x800=d2bebf30c7f90a309ac6de36ad204992ae868e9f8515650aa7d1f2e392401bbc`
  - `ui-demo-new-right-panel-grid=748654a4e56e061135191ac0f13aba7cc1ca3dec7e27be940baab05024c20430`
  - `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=dadb4615be9c7fc23214b12d28df8e489534ff9f9b3764c631133e4674925b3b`
  - `ui-demo-new-inventory-page-1=8daeeb8b6b652ffe191640546680ee6ae2b14632d4bb73ea3d104fcba40ebd5d`
  - `ui-demo-new-inventory-page-2=11a311d07b9f660f0e98ffe9286b46af3783c51243a5db9c8dce5097efb08553`
- Commands run:
  - focused RED and GREEN:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.DemoShellRendererTest.visual only empty equipment sockets stay quiet instead of drawing fake gold glyphs'`
  - focused PR02-1 golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - client owner validation:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`
  - local Python/PIL evidence generation for V43i/V44 crop copies, diff board
    and metrics JSON.
- Result:
  - PASS: focused visual-only empty equipment socket renderer test after the
    RED failure caught the old high-alpha pseudo-glyph strokes.
  - PASS: PR02-1 golden hash gate after rebaselining
    `GoldenScreenshotHarnessTest` expected hashes to V44.
  - PASS: `DemoShellRendererTest`, `DemoShellLayoutTest`,
    `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
    skipped by existing environment assumptions.
  - PASS: V44 evidence JSON uses repo-relative paths.
- Director verdict:
  - V44 is accepted-forward only.
  - It removes one obvious non-map UI quality defect: empty equipment sockets
    no longer read as bright placeholder letters.
  - It is not closure: the first-screen gap still needs bottom-deck
    composition unification and/or a larger room-scale map composition move.
- Next action:
  - Do not continue by only tuning slot alpha or adding more slot ornaments.
  - Next meaningful move should be bottom-deck hero/action/log composition
    unification, or a larger room-scale structural map pass that changes first
    read before grid detail.

## V45 Bottom Action Reserve Hierarchy

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - changed runtime surface: `DemoShellRenderer` action deck empty-entry
    rendering
  - stable contracts touched: no gameplay rules, core/game logic, active slot
    count, save/replay/profile/schema/content-pack, manifest key/schema or
    resource key changes
  - authority: existing `ActionPanelModel.entries` and existing
    `bottomDeck.actionSlotBounds`; no second hotbar, action, inventory,
    manifest or renderer authority
- Production:
  - Added a focused renderer test that fails when an empty action reserve slot
    draws full-strength empty-slot chrome or the central command connector.
  - Kept equipped action sockets high-priority while lowering the empty fourth
    socket to a quiet reserve state.
  - Removed the empty reserve slot's central command connector and added a
    low-contrast reserve interior so the capacity remains visible without
    competing with actions `1-3`.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/bottom-action-reserve-hierarchy-v45/pr08-bottom-action-reserve-hierarchy-v45-decision.md`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/bottom-action-reserve-hierarchy-v45/pr08-bottom-action-reserve-hierarchy-v45-comparison-board.png`
  - before bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/bottom-action-reserve-hierarchy-v45/pr08-bottom-action-reserve-hierarchy-v45-v44-bottom-deck.png`
  - after bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/bottom-action-reserve-hierarchy-v45/pr08-bottom-action-reserve-hierarchy-v45-v45-bottom-deck.png`
  - after full screenshot:
    `UI/review/dark-uiux-pr08-exploration/bottom-action-reserve-hierarchy-v45/pr08-bottom-action-reserve-hierarchy-v45-v45-full.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/bottom-action-reserve-hierarchy-v45/pr08-bottom-action-reserve-hierarchy-v45-runtime-metrics.json`
- Key metrics:
  - V45 vs V44 bottom-deck changed ratio:
    `0.042176101766443604`
  - V45 vs V44 bottom-deck mean absolute RGB diff:
    `0.17322602037740398`
  - V45 vs V44 bottom-deck max absolute channel diff:
    `79`
- PR02-1 golden hashes rebaselined to accepted V45 artifacts:
  - `ui-demo-new-parity-1672x941=4788819b5ca492a8a335e7e1c16485a19c7159669d6018a921c9454888a98750`
  - `ui-demo-new-parity-1280x800=1e85e9f1e9cd605f4808e6146f539b185fbfd1f49bacbddc64d8e53c4e9a873e`
  - `ui-demo-new-right-panel-grid=748654a4e56e061135191ac0f13aba7cc1ca3dec7e27be940baab05024c20430`
  - `ui-demo-new-bottom-deck-no-command-hints=c1622de2ee4f24b3eaf6c97dbcdf01d1e868dca67852422ae090db861ffda43b`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=dadb4615be9c7fc23214b12d28df8e489534ff9f9b3764c631133e4674925b3b`
  - `ui-demo-new-inventory-page-1=8daeeb8b6b652ffe191640546680ee6ae2b14632d4bb73ea3d104fcba40ebd5d`
  - `ui-demo-new-inventory-page-2=11a311d07b9f660f0e98ffe9286b46af3783c51243a5db9c8dce5097efb08553`
- Commands run:
  - focused RED and GREEN:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.DemoShellRendererTest.empty action reserve socket stays below equipped action hierarchy'`
  - focused PR02-1 golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - client owner validation:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`
  - local Python/PIL evidence generation for V44/V45 crop copies, diff board
    and metrics JSON.
- Result:
  - PASS: focused empty action reserve hierarchy renderer test after the RED
    failure caught the old full-strength empty slot chrome.
  - PASS: PR02-1 golden hash gate after rebaselining
    `GoldenScreenshotHarnessTest` expected hashes to V45.
  - PASS: `DemoShellRendererTest`, `DemoShellLayoutTest`,
    `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
    skipped by existing environment assumptions.
  - PASS: V45 evidence JSON uses repo-relative paths.
- Director verdict:
  - V45 is accepted-forward only.
  - It improves bottom-deck hierarchy by making available actions scan before
    reserve capacity.
  - It is not closure: the bottom HUD still needs a larger composition pass for
    hero/action/log material unity and map-vs-HUD first-read balance.
- Next action:
  - Do not continue with another one-slot hierarchy tweak.
  - Next meaningful move should either consolidate hero/action/log into a
    stronger authored bottom command console or return to room-scale map
    structure.

## V46 Bottom Command Console Spine

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - changed runtime surface: `DemoShellRenderer` bottom HUD cohesion rails
  - stable contracts touched: no gameplay rules, core/game logic, layout
    boundaries, active slot count, save/replay/profile/schema/content-pack,
    manifest key/schema or resource key changes
  - authority: existing `DemoBottomDeckLayout` hero/action/log bounds and
    existing shell chrome; no second HUD layout, hotbar, manifest or renderer
    authority
- Production:
  - Added a focused renderer test that fails when bottom HUD has no shared
    command spine spanning hero, action deck and log.
  - Added a low-position forged-iron command spine in the existing cohesion
    rails stage.
  - Kept the spine in the lower safe band so primary hero stats, action labels
    and log text remain readable.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/bottom-command-console-spine-v46/pr08-bottom-command-console-spine-v46-decision.md`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/bottom-command-console-spine-v46/pr08-bottom-command-console-spine-v46-comparison-board.png`
  - before bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/bottom-command-console-spine-v46/pr08-bottom-command-console-spine-v46-v45-bottom-deck.png`
  - after bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/bottom-command-console-spine-v46/pr08-bottom-command-console-spine-v46-v46-bottom-deck.png`
  - after full screenshot:
    `UI/review/dark-uiux-pr08-exploration/bottom-command-console-spine-v46/pr08-bottom-command-console-spine-v46-v46-full.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/bottom-command-console-spine-v46/pr08-bottom-command-console-spine-v46-runtime-metrics.json`
- Key metrics:
  - V46 vs V45 bottom-deck changed ratio:
    `0.06664255139567131`
  - V46 vs V45 bottom-deck mean absolute RGB diff:
    `0.24963927573802777`
  - V46 vs V45 bottom-deck max absolute channel diff:
    `57`
- PR02-1 golden hashes rebaselined to accepted V46 artifacts:
  - `ui-demo-new-parity-1672x941=82598d7bad878d5144a005914ed432af0834aeb105acf1a7f1c59fa7275c5dc7`
  - `ui-demo-new-parity-1280x800=045ebfe56e68449cc31545538c75816d8059eebcacfb3ba61e891a23f791556e`
  - `ui-demo-new-right-panel-grid=748654a4e56e061135191ac0f13aba7cc1ca3dec7e27be940baab05024c20430`
  - `ui-demo-new-bottom-deck-no-command-hints=048fca5432b17521c202ac2808087c686d201027499422cb6962f9bd0b967ba9`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=dadb4615be9c7fc23214b12d28df8e489534ff9f9b3764c631133e4674925b3b`
  - `ui-demo-new-inventory-page-1=8daeeb8b6b652ffe191640546680ee6ae2b14632d4bb73ea3d104fcba40ebd5d`
  - `ui-demo-new-inventory-page-2=11a311d07b9f660f0e98ffe9286b46af3783c51243a5db9c8dce5097efb08553`
- Commands run:
  - focused RED and GREEN:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.DemoShellRendererTest.bottom hud renders a shared command spine across hero action and log'`
  - focused PR02-1 golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - client owner validation:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`
  - local Python/PIL evidence generation for V45/V46 crop copies, diff board
    and metrics JSON.
- Result:
  - PASS: focused bottom command spine renderer test after the RED failure
    proved the old bottom HUD had no shared spine.
  - PASS: PR02-1 golden hash gate after rebaselining
    `GoldenScreenshotHarnessTest` expected hashes to V46.
  - PASS: `DemoShellRendererTest`, `DemoShellLayoutTest`,
    `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
    skipped by existing environment assumptions.
  - PASS: V46 evidence JSON uses repo-relative paths.
- Director verdict:
  - V46 is accepted-forward only.
  - It makes the bottom HUD read more like one command console instead of three
    disconnected cards.
  - It is not closure: subtle rails cannot carry the whole gap; the next
    bottom pass needs stronger hero material hierarchy or the map needs a
    larger structural move.
- Next action:
  - Do not continue by stacking more subtle rails.
  - Next meaningful move should be a larger hero-card material hierarchy reset
    or a room-scale map structural pass.

## V47 Hero Card Material Hierarchy

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - changed runtime surface: `DemoShellRenderer` hero card interior
  - stable contracts touched: no gameplay rules, core/game logic, layout
    boundaries, save/replay/profile/schema/content-pack, manifest key/schema
    or resource key changes
  - authority: existing `DemoBottomDeckLayout.heroCard`, existing
    `SHELL_HERO_CREST_PLACEHOLDER` asset and current gauge/status rendering;
    no second hero state, resource mapping or HUD layout authority
- Production:
  - Added a focused renderer test that fails when the hero crest has only a
    small backing plate rather than a tall heraldic material pillar.
  - Added a full-height dark heraldic pillar behind the crest inside the hero
    content region.
  - Kept hero name, floor, gauges, stat chips, action deck, command spine and
    log text readable.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/hero-card-material-hierarchy-v47/pr08-hero-card-material-hierarchy-v47-decision.md`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/hero-card-material-hierarchy-v47/pr08-hero-card-material-hierarchy-v47-comparison-board.png`
  - before bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/hero-card-material-hierarchy-v47/pr08-hero-card-material-hierarchy-v47-v46-bottom-deck.png`
  - after bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/hero-card-material-hierarchy-v47/pr08-hero-card-material-hierarchy-v47-v47-bottom-deck.png`
  - after full screenshot:
    `UI/review/dark-uiux-pr08-exploration/hero-card-material-hierarchy-v47/pr08-hero-card-material-hierarchy-v47-v47-full.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/hero-card-material-hierarchy-v47/pr08-hero-card-material-hierarchy-v47-runtime-metrics.json`
- Key metrics:
  - V47 vs V46 bottom-deck changed ratio:
    `0.03633116295894375`
  - V47 vs V46 bottom-deck mean absolute RGB diff:
    `0.0600847048893712`
  - V47 vs V46 bottom-deck max absolute channel diff:
    `22`
- PR02-1 golden hashes rebaselined to accepted V47 artifacts:
  - `ui-demo-new-parity-1672x941=9f1b55267285af20635e2526e8c4d7a8eaf9f08903485bb90674f4159663ab41`
  - `ui-demo-new-parity-1280x800=b64ab7adecdccd8f26df1e3444977c0a2d3e2317f18b553c196003eb3b9d5364`
  - `ui-demo-new-right-panel-grid=748654a4e56e061135191ac0f13aba7cc1ca3dec7e27be940baab05024c20430`
  - `ui-demo-new-bottom-deck-no-command-hints=1cac476b794a148b479ef9a04f0a3bd0d18dac709e8072a4dc74bdbe4807fc16`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=dadb4615be9c7fc23214b12d28df8e489534ff9f9b3764c631133e4674925b3b`
  - `ui-demo-new-inventory-page-1=8daeeb8b6b652ffe191640546680ee6ae2b14632d4bb73ea3d104fcba40ebd5d`
  - `ui-demo-new-inventory-page-2=11a311d07b9f660f0e98ffe9286b46af3783c51243a5db9c8dce5097efb08553`
- Commands run:
  - focused RED and GREEN:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.DemoShellRendererTest.hero card renders a tall heraldic material pillar behind the crest'`
  - focused PR02-1 golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - client owner validation:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`
  - local Python/PIL evidence generation for V46/V47 crop copies, diff board
    and metrics JSON.
- Result:
  - PASS: focused hero-card material hierarchy renderer test after the RED
    failure proved the old hero card had only a small crest backing plate.
  - PASS: PR02-1 golden hash gate after rebaselining
    `GoldenScreenshotHarnessTest` expected hashes to V47.
  - PASS: `DemoShellRendererTest`, `DemoShellLayoutTest`,
    `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
    skipped by existing environment assumptions.
  - PASS: V47 evidence JSON uses repo-relative paths.
- Director verdict:
  - V47 is accepted-forward only.
  - It improves the hero block's material hierarchy without widening resource
    or layout authority.
  - It is not closure: the current crest/portrait art is still below the
    reference's authored character panel quality, and map-stage quality remains
    a larger first-read gap.
- Next action:
  - Do not continue with another tiny hero overlay.
  - Next meaningful move should either generate/pipe a stronger official hero
    portrait/crest resource through the asset chain or return to room-scale map
    structure.

## V48 Hero Crest Resource Replacement

- Verdict: accepted-forward only.
- Change shape:
  - owner module: `client`
  - changed resource surface: `ui.shell.hero_crest.placeholder` existing
    r01b sheet cell and derived runtime slice
  - stable contracts touched: no gameplay rules, core/game logic, hero state,
    save/replay/profile/schema/content-pack, manifest key/schema or runtime
    resolver path changes
  - authority: existing `UI/sprite-sheets/sheet-plan.yaml`,
    `UI/sprite-sheets/key-registry.yaml`, canonical/runtime manifest path and
    `SHELL_HERO_CREST_PLACEHOLDER` consumer; no second hero-art key
- Production:
  - Generated a new red-enamel and gold-lion heraldic crest using built-in
    `image_gen`, then removed the chroma-key background with the system
    imagegen helper.
  - Replaced row `2`, col `0` of
    `assets-src/image/raw/sheets/dark-v1/r01b-ui-shell-chrome.png`.
  - Regenerated runtime slices with
    `python3 scripts/slice_spritesheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`.
  - Regenerated contact sheets with
    `python3 scripts/render_contact_sheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite`.
  - Added a focused asset-quality regression test that fails on the old grey
    shield and requires transparent subject-first bounds plus red enamel and
    gold heraldic material signal.
- Evidence:
  - decision:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-decision.md`
  - generated source:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-generated-source.png`
  - alpha cutout:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-alpha.png`
  - final 256 cell:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-cell256.png`
  - comparison board:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-comparison-board.png`
  - after bottom-deck crop:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-v48-bottom-deck.png`
  - after full screenshot:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-v48-full.png`
  - runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-runtime-metrics.json`
- Key metrics:
  - V48 vs V47 bottom-deck changed ratio:
    `0.03628896123470188`
  - V48 vs V47 bottom-deck mean absolute RGB diff:
    `1.106585478587649`
  - V48 vs V47 bottom-deck max absolute channel diff:
    `233`
- PR02-1 golden hashes rebaselined to accepted V48 artifacts:
  - `ui-demo-new-parity-1672x941=2349150181e17e6c1a03f3ae6e3c95c20959eaf6c8924976a5b46e6efbcf3b67`
  - `ui-demo-new-parity-1280x800=c1d8ead018a912a8c53bc3f56187aa16d4bad15a9d06da4bf10b0473c8ed6563`
  - `ui-demo-new-right-panel-grid=748654a4e56e061135191ac0f13aba7cc1ca3dec7e27be940baab05024c20430`
  - `ui-demo-new-bottom-deck-no-command-hints=6bf2380371c23586c38737409e70fb487c903b82b89ed989fc80c0f8e837a142`
  - `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
  - `ui-demo-new-map-stage-crop=dadb4615be9c7fc23214b12d28df8e489534ff9f9b3764c631133e4674925b3b`
  - `ui-demo-new-inventory-page-1=8daeeb8b6b652ffe191640546680ee6ae2b14632d4bb73ea3d104fcba40ebd5d`
  - `ui-demo-new-inventory-page-2=11a311d07b9f660f0e98ffe9286b46af3783c51243a5db9c8dce5097efb08553`
- Commands run:
  - focused RED and GREEN:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.assets.ManifestResolveTest.dark uiux hero crest keeps authored red enamel and gold heraldry'`
  - resource owner gates:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:darkKeyRegistryLint :tools:darkSpriteSheetLint :tools:spriteSheetMapLint :tools:resourcePipelineLint :tools:darkManifestCoveragePr02_1OwnerScope :tools:darkManifestCoveragePr08OwnerScope -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
  - focused PR02-1 golden evidence:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - client owner validation:
    `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`
  - local Python/PIL processing for chroma-key extraction, 256 cell fit,
    raw-sheet cell replacement, runtime comparison board and metrics JSON.
- Result:
  - PASS: focused hero-crest resource test after the RED failure proved the
    old grey shield lacked authored red/gold heraldic material signal.
  - PASS: `darkKeyRegistryLint`, `darkSpriteSheetLint`, `spriteSheetMapLint`,
    `resourcePipelineLint`, `darkManifestCoveragePr02_1OwnerScope` and
    `darkManifestCoveragePr08OwnerScope`; PR08 map report refreshed via the
    explicit `ktome.darkUiux.spriteMapReport` path.
  - PASS: PR02-1 golden hash gate after rebaselining
    `GoldenScreenshotHarnessTest` expected hashes to V48.
  - PASS: `ManifestResolveTest`, `DemoShellRendererTest`,
    `DemoShellLayoutTest`, `:client:clientSmoke` and `maintainabilityLint`;
    three smoke subtests were skipped by existing environment assumptions.
  - PASS: V48 evidence JSON uses repo-relative paths.
- Director verdict:
  - V48 is accepted-forward only.
  - It upgrades the bottom-left hero identity from placeholder shield to
    authored RPG crest while preserving the existing resource authority chain.
  - It is not closure: the map-stage room structure remains the larger
    first-read gap against `UI/UI-demo-new.png`.
- Next action:
  - Do not continue by making another small hero crest or hero-card overlay
    tweak.
  - Next meaningful move should return to room-scale map structure or a broader
    bottom HUD composition pass.

## 2026-05-31 - UI/design Absorption Review

- Loop type: documentation / design prototype absorption
- Target area: PR-08 follow-up planning, non-map component-state guidance,
  shell layout reference and current-progress reorientation
- Changed files:
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - `UI/design/In-Run Shell.html`
  - `UI/design/tokens.css`
  - `UI/design/components/shell.jsx`
  - `UI/design/components/slot.jsx`
  - `UI/design/components/state-matrix.jsx`
  - `UI/design/components/state-lab.jsx`
  - `UI/design/components/inventory-modal.jsx`
  - `UI/design/components/talent-assign.jsx`
- Commands run:
  - `git status --short`
  - `sed -n '1,220p' docs/INDEX.md`
  - `rg --files UI | sort`
  - `wc -l UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md UI/goal/dark-uiux-pr08-director-grade-iteration-log.md UI/design/*.html UI/design/*.jsx UI/design/*.css UI/design/components/*.jsx`
  - `sed` reads of the PR-08 goal/log and selected `UI/design` files
  - `rg -n` heading/symbol scans across `UI/design` and PR-08 goal/log files
- Result:
  - ACCEPTED as planning input: `UI/design` contains useful quantified design
    specs for shell variants, slot/panel/talent states, nine-patch targets,
    inventory modal/detail grammar, talent row grammar and token boards.
  - NOT ACCEPTED as runtime authority: the React/CSS prototype, sample labels,
    placeholder icon tokens and design-only canvas tooling must not be copied
    into Kotlin, manifests, generated images or production resource keys.
  - UPDATED: the goal now includes `UI/design` as a supplemental prototype
    reference set, a Design Prototype Absorption Loop and Slice 5A for
    component-state absorption.
  - UPDATED: this log's Current Baseline now points to V48 rather than the old
    Slice 1 setup status.
  - NOT RUN: Gradle, golden screenshot or packaged whitebox. This entry is
    documentation-only and makes no Kotlin/resource/runtime changes.
- Director verdict:
  - Useful for follow-up progress, especially when PR08 moves to right-panel,
    bottom-HUD, inventory, talent or state-surface work.
  - It does not solve the active director-grade blocker by itself. The latest
    runtime verdict remains V48 accepted-forward only, with map-stage
    room-structure still the largest first-read gap.
- Remaining risk:
  - `UI/design` is currently an untracked design directory in this worktree; do
    not rely on it in future turns unless it remains present or is formally
    added to the PR.
  - The design prototype was inspected from source files in this entry; no
    browser screenshot export was generated.
- Next action:
  - Resume from V48. Prefer a room-scale map-structure pass if the map remains
    dominant.
  - If choosing a non-map pass, first select one Slice 5A design-derived
    acceptance surface, then add a focused test or screenshot/state-matrix
    artifact before editing runtime code.

## 2026-05-31 - V48 Direction Assessment

- Loop type: documentation / technique-change checkpoint
- Target area: PR-08 next-route decision after V48, especially map-stage
  first-read quality against `UI/UI-demo-new.png`
- Changed files:
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - reference full:
    `UI/UI-demo-new.png`
  - reference map/right/bottom crops:
    `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-reference-map-crop.png`
    `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-reference-right-panel-crop.png`
    `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-reference-bottom-deck-crop.png`
  - current V48 full:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-v48-full.png`
  - current PR02-1 crops:
    `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`
    `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`
    `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png`
  - V48 runtime metrics:
    `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-runtime-metrics.json`
- Commands run:
  - `sed` reads of the current goal/log sections and V48 log entry
  - `rg -n` scans for PR-08 loop, V48, room-scale, resource and
    technique-change language in the goal/log
  - `apply_patch` document edits only
- Result:
  - UPDATED: the goal now records the V48 diagnosis that PR-08 is blocked by a
    presentation-model gap, not another small alpha, seam, rail, slot, crest or
    rectangle-compositor tweak.
  - UPDATED: the goal now defines `RoomPresentationLayer` / `RoomArtPlate` as
    the preferred map-stage technique-change probe.
  - UPDATED: the goal now adds a Room Presentation Technique-Change Loop and
    Slice 4A for room-scale generated/art-authored environment plate A/B
    validation.
  - UPDATED: this log's Current Baseline points to Slice 4A as the next default
    map move.
  - NOT RUN: Gradle, golden screenshot, resource gates or packaged whitebox.
    This entry is documentation-only and makes no Kotlin/resource/runtime
    changes.
- Director verdict:
  - V48 remains accepted-forward only.
  - The map gap is now explicitly classified as `tile/grid authority first, art
    direction second`. The next high-return test is a room-scale presentation
    layer or art-plate probe, not incremental renderer tuning.
  - New generated resources are recommended for fast validation, but only as
    no-text, no-character, no-loot environment plates until A/B evidence proves
    they should enter the formal asset path.
- Remaining risk:
  - No new art resources were generated in this entry.
  - No runtime prototype exists yet for the room presentation layer.
  - A production `RoomArtPlate` route may require new resource owner rows,
    sheet-plan/key-registry wiring and resource pipeline gates after visual
    acceptance.
- Next action:
  - Start Slice 4A if the next turn stays on the map blocker: generate two or
    three room-scale environment plate candidates under
    `UI/review/dark-uiux-pr08-exploration/`, compare them against V48 and
    `UI/UI-demo-new.png`, then decide whether to prototype a client-only
    presentation layer beneath runtime gameplay markers.
  - If the next turn intentionally pivots away from map work, use Slice 5A and
    select a `UI/design` state/layout acceptance surface before editing runtime
    code.

## 2026-05-31 - Human Decision Gate Locked

- Loop type: documentation / governance update
- Target area: PR-08 major decision ownership, Direction A room-plate route and
  future goal-loop stop conditions
- Changed files:
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - N/A. This entry changes planning and acceptance rules only.
- Commands run:
  - `rg -n` scans for `No human confirmation`, `agent self-review`,
    `RoomPresentationLayer`, `RoomArtPlate`, `Slice 4A`, `accepted-forward`,
    `lky` and decision-gate language in the goal/log
  - `sed` reads of the affected goal/log sections
  - `apply_patch` document edits only
- Result:
  - UPDATED: the goal now states Direction A as the active map route:
    pre-rendered room art plates through a client-only presentation layer.
  - UPDATED: major PR-08 decisions now require explicit lky decision, including
    map aesthetic direction, room-plate candidate acceptance, production
    resource promotion, large renderer pass deletion, surface pivot, map
    closure and final director-grade closure.
  - UPDATED: the former fully autonomous `No human confirmation is required`
    rule has been replaced with a decision-packet requirement. The agent can
    generate candidates, reject obviously invalid probes, run checks and
    implement an approved route, but it must stop as `awaiting-lky-decision`
    before major changes.
  - UPDATED: Slice 4A now requires a 3-4 candidate room art-plate A/B board and
    lky selection before runtime integration.
  - UPDATED: if a selected plate is integrated, the implementation must remove
    or disable the old decorative geometry responsibilities it replaces rather
    than stacking V27-V42 style pass families under the new plate.
  - NOT RUN: Gradle, golden screenshot, resource gates or packaged whitebox.
    This entry is documentation-only and makes no Kotlin/resource/runtime
    changes.
- Director verdict:
  - The review report's core critique is accepted into the PR-08 goal contract:
    the loop should stop autonomous micro-acceptance and put lky back in the
    director seat for major visual/product decisions.
  - Direction A is now the default map path. Direction B remains a fallback only
    if lky explicitly reopens it.
- Remaining risk:
  - No room art-plate candidates have been generated yet.
  - No runtime `RoomPresentationLayer` prototype exists yet.
  - Actual renderer pass removal still needs implementation planning after lky
    selects a room-plate candidate and owner route.
- Next action:
  - Start Slice 4A with a review-only candidate board: generate 3-4 text-free,
    character-free, loot-free room environment plates, compare against V48 and
    `UI/UI-demo-new.png`, then stop for lky decision before any runtime,
    manifest, resource or golden mutation.

## 2026-05-31 - Direction A Execution Plan Created

- Loop type: documentation / implementation planning
- Target area: confirmed pre-rendered room art plate route and complete
  follow-up development plan
- Changed files:
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - N/A. This entry creates the execution plan only.
- Commands run:
  - `sed` reads of `docs/INDEX.md`, `docs/rule/ai-change-governance.md`,
    `UI/PLAN.md`, `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md`, current
    goal sections and renderer/asset entry points
  - `ls` / `rg --files` inspection of `UI/goal`
  - `rg -n` scans of client asset/renderer code for manifest, texture, map and
    room-material entry points
  - `apply_patch` document edits only
- Result:
  - ADDED: a standalone Direction A plan at
    `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`.
  - UPDATED: the main goal's minimum full-read set now includes the new plan.
  - UPDATED: this log's Current Baseline now points to the Direction A
    execution plan.
  - NOT RUN: Gradle, golden screenshot, resource gates or packaged whitebox.
    This entry is documentation-only and makes no Kotlin/resource/runtime
    changes.
- Director verdict:
  - Direction A now has a concrete sprint plan: baseline lock, review-only
    candidate generation, lky decision, resource owner route, client-only
    runtime prototype, replaced pass removal, director evidence and production
    closure.
- Remaining risk:
  - The plan still awaits execution. No room art-plate candidates have been
    generated and no runtime prototype exists.
- Next action:
  - Execute Sprint 1 of the plan: generate 3-4 review-only room art plate
    candidates, build the A/B decision board and stop as
    `awaiting-lky-decision`.

## 2026-05-31 - Hidden Wishing Koala Plan Absorption

- Loop type: documentation / external plan absorption
- Target area: Direction A execution plan hardening
- Changed files:
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - external input:
    external Claude plan `hidden-wishing-koala.md` provided by lky
- Commands run:
  - `wc -l <external-plan-path>`
  - `sed -n '1,260p' <external-plan-path>`
  - `sed` reads of the Direction A plan sections for architecture, Sprint 1,
    Sprint 3, Sprint 4 and testing strategy
  - `apply_patch` document edits only
- Result:
  - ABSORBED/REWRITTEN: image-generation readiness note before the full
    candidate batch. A separate smoke check is optional, not a default blocker,
    because recent generated-image evidence already proves the path in normal
    conditions.
  - ABSORBED: topology clipping guidance using existing visible material
    topology as the presentation mask, with deterministic world/room-anchored
    plate placement.
  - ABSORBED: target render order for plate, semantic hazards, retained
    light/transition treatment, quiet interaction grid and runtime overlays.
  - ABSORBED: concrete initial `TileRenderer` decorative pass audit candidates
    for Sprint 4.
  - ABSORBED: focused runtime assertions for clipping, layer order, grid alpha,
    overlay readability, pass-family removal and manifest-based resolution.
  - REWRITTEN: the external plan's exploration/dev asset-loading shortcut was
    not copied as-is. The committed runtime path must not load from
    `UI/review/...` or machine-local generated image folders; selected plates
    must be promoted through an approved resource owner/manifest route first.
  - NOT RUN: Gradle, golden screenshot, resource gates or packaged whitebox.
    This entry is documentation-only and makes no Kotlin/resource/runtime
    changes.
- Director verdict:
  - The external plan provides useful implementation detail and has been
    absorbed where it strengthens Direction A without weakening resource
    authority or the lky decision gate.
- Remaining risk:
  - The pass audit list is still a starting point, not an approved deletion
    list. Large deletion waits until lky selects a room plate and approves the
    runtime route.
- Next action:
  - Execute Direction A plan Sprint 1 directly: note existing successful
    generation evidence, generate 3-4 review-only candidates, build the A/B
    board and stop for lky decision. Run a one-image smoke check only if the
    generation tool/path changes, environment drift is suspected, the first
    candidate command fails or lky explicitly requests it.

## 2026-05-31 - Unified lky Decision Packet Prepared

- Loop type: documentation / review-only visual evidence / decision packet
- Target area: Direction A room art plate candidate selection, runtime route,
  production route, pass deletion, surface priority and closure criteria
- Changed files:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/prompts/pr08-room-art-plate-direction-a-prompt-set.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-a-authored-ruins-chamber.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-b-torch-cut-stone-arena.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-c-broken-slab-hall.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-d-material-only-technical-fallback.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-marker-proxy-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-decision.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-marker-proxy-board.png`
- Commands run:
  - `sed` / `rg` reads of PR-08 goal, log, plan, PR doc, gap audit, evidence
    brief, `UI/design`, renderer, manifest and resource-pipeline entry points
  - built-in image generation for 4 review-only room plate candidates
  - `sips -g pixelWidth -g pixelHeight` for generated candidates and reference
    evidence
  - `shasum -a 256` for generated candidates and prompt set
  - local PIL board composition for the candidate board and marker proxy board
  - `apply_patch` document edits only
- Result:
  - GENERATED: four review-only room art plate candidates under the Direction A
    review directory.
  - GENERATED: one candidate decision board and one synthetic marker-readability
    proxy board.
  - ADDED: a unified lky decision packet recommending Candidate C as the first
    runtime prototype target, Candidate A as alternate, Candidate B as rejected
    for default route and Candidate D as technical fallback only.
  - RECORDED: conditional recommendations for client-only runtime prototype,
    PR-08 owner/manifest route, large decorative pass deletion, surface priority
    and final closure rubric.
  - UPDATED: the goal minimum full-read set now includes the unified decision
    packet.
  - NOT RUN: Gradle, runtime prototype, manifest/resource gates, golden
    screenshot or packaged whitebox. This entry is review-only and makes no
    Kotlin/resource/runtime/golden changes.
- Director verdict:
  - `awaiting-lky-decision`.
- Remaining risk:
  - Generated art has not been tested inside the runtime map stage.
  - Candidate C is recommended but not selected until lky approves it.
  - Production key/category/owner route is conditional and must still pass
    resource gates before committed runtime consumption.
- Next action:
  - lky decides whether to approve the recommended one-shot route, select a
    different candidate, reject/regenerate the batch, pivot to Direction B or
    pause PR-08 map work.

## 2026-05-31 - Multi-map Rollout Boundary Recorded

- Loop type: documentation / scope clarification
- Target area: Direction A room art plate rollout scope across zones and
  tileset families
- Changed files:
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-decision.zh.md`
- Visual evidence:
  - Existing Direction A boards remain the visual evidence:
    `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-board.png`
    and
    `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-marker-proxy-board.png`
- Commands run:
  - `rg` / `sed` reads of zone, tileset, visual manifest, renderer and Direction
    A plan files
  - small read-only zone/manifest counting scripts
  - `apply_patch` document edits only
- Result:
  - RECORDED: the current game has 11 zones across 4 tileset families:
    `tileset.ruins`, `tileset.forest_edge`, `tileset.mine` and
    `tileset.shadow_depths`.
  - RECORDED: Direction A's first runtime proof is scoped to `tileset.ruins`
    only, covering the current Candidate C route and the first PR-08 map-stage
    blocker evidence.
  - RECORDED: the implementation must still be tileset-agnostic through a
    `RoomArtPlateCatalog` or equivalent selector keyed by tileset/theme context,
    with unsupported families falling back to the existing tile renderer.
  - RECORDED: `tileset.forest_edge`, `tileset.mine` and
    `tileset.shadow_depths` require their own future authored plate families if
    PR-08 or a later package claims all-map quality.
  - NOT RUN: Gradle, runtime prototype, manifest/resource gates, golden
    screenshot or packaged whitebox. This entry is documentation-only and makes
    no Kotlin/resource/runtime/golden changes.
- Director verdict:
  - `awaiting-lky-decision`.
- Remaining risk:
  - No non-ruins room art plate has been generated or tested.
  - Current review art still proves only the ruins direction, not forest, mine
    or shadow-depths.
- Next action:
  - If lky approves Candidate C, implement the first runtime prototype as a
    ruins-only plate with a tileset-agnostic catalog/fallback boundary. Do not
    expand production art to other tilesets without a separate decision.

## 2026-05-31 - Candidate C Runtime Prototype Route Approved

- Loop type: lky decision record / documentation
- Target area: Direction A Candidate C selection and conditional implementation
  route
- Changed files:
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-decision.zh.md`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-marker-proxy-board.png`
- Commands run:
  - `sed` reads of the Direction A decision packet, Chinese packet, goal, plan
    and latest log entries
  - `apply_patch` document edits only
- Result:
  - APPROVED BY LKY:
    ```text
    Approve Candidate C and the conditional Direction A route: client-only room art
    plate prototype first, PR-08 owner/manifest route only after runtime evidence,
    large decorative pass deletion after marker readability passes, no surface pivot
    and no golden rebaseline until final runtime packet.
    ```
  - UPDATED: the English and Chinese decision packets now record
    `approved-by-lky-for-runtime-prototype`.
  - UPDATED: the goal and Direction A plan now record Candidate C as the
    approved first runtime proof target.
  - UPDATED: the next slice is now the `tileset.ruins` client-only room art
    plate runtime prototype behind a tileset-agnostic catalog/fallback boundary.
  - NOT RUN: Gradle, runtime prototype, manifest/resource gates, golden
    screenshot or packaged whitebox. This entry records a decision only and
    makes no Kotlin/resource/runtime/golden changes.
- Director verdict:
  - Candidate C runtime prototype route approved.
- Remaining risk:
  - Candidate C is still review art; it has not been tested inside runtime.
  - PR-08 owner/manifest promotion, large decorative pass deletion, golden
    rebaseline and final closure still require future evidence and lky approval.
- Next action:
  - Begin the client-only runtime prototype implementation for `tileset.ruins`
    using Candidate C, with non-ruins tilesets falling back to the existing
    renderer.

## 2026-05-31 - Room Art Plate Client Renderer Scaffold Implemented

- Loop type: client-only runtime prototype scaffold + decision blocker
- Target area: `tileset.ruins` room art plate presentation path
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/assets/DarkUiMapVisualKeys.kt`
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md`
  - `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
- Implementation result:
  - ADDED: `RoomPresentationPlan`, `RoomArtPlateModel`,
    `RoomArtPlateCatalog` and `RoomArtPlateRenderer` as client-only
    presentation objects.
  - ADDED: room plate key constant
    `ui.map_stage.ruins.room_plate.pr08_demo`.
  - WIRED: `TileRenderModelBuilder` derives the plate from
    `RenderSnapshot.metadata.tilesetKey` and visible ruins materials; only
    `tileset.ruins` can currently resolve the plate.
  - WIRED: `TileRenderer` draws the plate inside the map room compositor, below
    actors, loot, telegraphs, cursor and targeting overlays.
  - KEPT: `core` and `game` untouched.
  - KEPT: production manifest/resource promotion, large decorative pass
    deletion, golden rebaseline and final closure untouched.
- Test result:
  - WROTE FIRST: failing focused test for the room plate draw path and layer
    order; it failed because the renderer did not draw
    `ui.map_stage.ruins.room_plate.pr08_demo`.
  - PASS:
    ```bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk env
    ./gradlew :client:test \
      --tests com.ktome.client.render.TileRendererCanvasTest.recordsExplicitLayerFlushBoundaries \
      --tests "com.ktome.client.render.TileRendererCanvasTest.room compositor stays below actors loot and tactical affordances" \
      --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset \
      --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere \
      --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps \
      --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*"
    ```
  - PASS:
    ```bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk env
    ./gradlew maintainabilityLint
    ```
  - NOTE: SDKMAN printed internet reachability warnings, but Java
    `21.0.10-tem` and Kotlin `2.2.21` were active and Gradle completed from
    local caches.
- Resource blocker:
  - VERIFIED ABSENT: `ui.map_stage.ruins.room_plate.pr08_demo` is not in
    `assets-src/image/manifests/phase2-visual-manifest.json`,
    `client/src/main/resources/manifests/visual-manifest.json`,
    `client/src/main/resources/` or `UI/sprite-sheets`.
  - VERIFIED PRESENT ONLY AS REVIEW ART:
    `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-c-broken-slab-hall.png`
    is a 1448x1086 PNG.
  - The runtime code intentionally does not read from `UI/review/...`,
    generated-image folders or machine-local paths.
- Director verdict:
  - `awaiting-lky-decision`.
- Decision needed:
  - Recommended: approve a narrow evidence-only canonical resource route for
    Candidate C using the existing `ui_frame` / `ui` manifest shape and full
    resource gates. This can produce real runtime evidence but starts the D2
    resource route earlier than the previous wording allowed.
  - Alternative: approve a content-pack or harness-only visual manifest route
    for prototype screenshots. This avoids official manifest promotion but
    leaves final resource-route proof for later.
  - Conservative: keep D2 blocked and stop runtime work here, because real
    textured runtime evidence before any manifest/resource route is circular.

## 2026-05-31 - Candidate C Canonical Runtime Prototype Evidence

- Loop type: canonical single-image resource route + runtime evidence packet
- Target area: PR-08 Direction A Candidate C room art plate
- Changed files:
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `assets-src/image/manifests/phase2-visual-manifest.json`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`
  - `client/src/main/kotlin/com/ktome/client/assets/ClientAssetLoadStrategy.kt`
  - `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
  - `build.gradle.kts`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-canonical-prototype/*`
  - PR-08 goal / plan / log documents
- Implementation result:
  - ADDED: single-image resource plan for
    `ui.map_stage.ruins.room_plate.pr08_demo`.
  - ADDED: canonical/runtime visual manifest entry with `category=ui_frame`,
    `footprint=ui` and runtime path
    `dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`.
  - ADDED: runtime PNG copied from approved Candidate C review art.
  - WIRED: `ClientAssetLoadStrategy` preloads the room plate for ruins sessions.
  - KEPT: no `core` / `game` rule changes.
  - KEPT: no golden baseline update, no D3 closure, no all-map closure.
  - KEPT: no large decorative `TileRenderer` pass deletion yet.
- Authority note:
  - The room plate is 1448x1086 and is not a sprite-sheet cell. It therefore
    uses the existing single-image `phase2AssetGates` + visual manifest route,
    not `UI/sprite-sheets/key-registry.yaml`.
  - `darkManifestCoveragePr08OwnerScope` still covers the existing 20 PR-08
    sheet-owned keys and passed. Do not force this room plate into
    `r02-ui-demo-ruins-tiles` unless lky approves a schema/owner-contract
    extension for single-image owner keys.
- RED check:
  - Added manifest exact-resolution coverage first.
  - Initial run failed because
    `ui.map_stage.ruins.room_plate.pr08_demo` resolved to `missing_visual`.
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew :client:test \
    --tests com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries \
    --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available"
  ```
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew manifestLint resourcePipelineLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope
  ```
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew :client:test \
    --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" \
    --tests com.ktome.client.render.TileRendererCanvasTest.recordsExplicitLayerFlushBoundaries \
    --tests "com.ktome.client.render.TileRendererCanvasTest.room compositor stays below actors loot and tactical affordances" \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps
  ```
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew maintainabilityLint
  ```
- Expected golden evidence failure:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
  ```
  - Result: failed because PR-08 exploration does not update golden baselines.
  - `ui-demo-new-map-stage-crop` expected
    `94372439d8aeecf969ee3e2429321a12f2551cd1042f125a6759f1832ed08ad5`, actual
    `6888a04886486601b3ee826a0f6b92a9db7275a7bbd91e691fd0493d63865d04`.
- Evidence files:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-canonical-prototype/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-canonical-prototype/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-canonical-prototype/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-canonical-prototype/golden-status.txt`
- Visual read:
  - Candidate C is visible in real runtime evidence.
  - The result is not final director-grade Direction A yet: old grid and large
    decorative compositor passes still dominate first read and flatten the
    authored room plate.
- Director verdict:
  - `awaiting-lky-d4-marker-readability-decision`.
- Decision needed:
  - Recommended next slice: when a room art plate is active, suppress or sharply
    attenuate only the large room-material/decorative compositor passes it
    replaces, while preserving actors, loot, telegraphs, cursor, selection,
    pathing and range feedback.
  - Alternative: reject D4 deletion for now and request another plate/mask
    placement pass before touching the old decorative stack.
  - Still blocked: golden rebaseline, D3 map-stage closure, final runtime packet
    and all-map closure.

## 2026-05-31 - D4 Legacy Decorative Pass Suppression Evidence

- Loop type: approved D4 narrow implementation slice + runtime evidence packet
- Target area: Candidate C room art plate compositor path
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d4-decorative-suppression/*`
  - PR-08 goal / plan / log documents
- Implementation result:
  - ADDED: `RoomCompositorStrategy` on the existing client-only
    `RoomPresentationPlan` boundary.
  - WIRED: `TileRenderer` now draws `RoomArtPlateRenderer` and then chooses the
    compositor strategy in one place.
  - SUPPRESSED for active room art plate only:
    `drawVisibleRoomFloorUnifier`, `drawVisibleRoomAtmosphere`,
    `drawVisibleRoomApertureHierarchy` and `renderWarmMapOverlay`.
  - KEPT for legacy/no-plate path: all existing decorative room compositor
    behavior.
  - KEPT above the room compositor: props, sprite overlays/telegraphs, actors,
    loot markers, fog, targeting highlights, cursor and combat feedback.
  - KEPT: no `core` / `game` changes, no golden rebaseline, no D3 closure, no
    all-map closure.
- RED check:
  - Added
    `TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive`
    first.
  - Initial compiled test failed for the expected behavioral reason after input
    fixture correction: `tileset.ruins.room_breakup_01` was still drawn after
    `ui.map_stage.ruins.room_plate.pr08_demo`.
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive
  ```
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew :client:test \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere
  ```
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew manifestLint resourcePipelineLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope maintainabilityLint
  ```
- Expected golden evidence failure:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
  ```
  - Result: failed because PR-08 exploration does not update golden baselines.
  - `ui-demo-new-map-stage-crop` expected
    `94372439d8aeecf969ee3e2429321a12f2551cd1042f125a6759f1832ed08ad5`, actual
    `ec1a6c46c27c40dd28f336b5a5ee821c4478c58504ea011b4ca8af3350fe5f75`.
- Evidence files:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d4-decorative-suppression/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d4-decorative-suppression/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d4-decorative-suppression/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d4-decorative-suppression/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d4-decorative-suppression/golden-status.txt`
- Visual read:
  - D4 removes the prior overpaint symptoms from the canonical prototype:
    repeated wall bands, horizontal/vertical decorative strips, room breakup
    asset and large amber/green haze blocks no longer flatten Candidate C.
  - The room now reads much closer to authored environment art.
  - Gameplay markers remain visible in the D4 crop.
  - The always-visible grid is still stronger than the final Direction A thesis
    wants, so the next review should decide whether to accept map-stage closure
    or do one narrowly scoped grid/marker/edge readability pass first.
- Director verdict:
  - `awaiting-lky-d3-map-stage-closure-decision`.
- Decision needed:
  - Decide whether the D4 runtime packet is sufficient for D3 map-stage closure.
  - If not, approve only one narrow readability pass: grid/marker/edge
    integration above the plate. Do not revive the old large decorative
    compositor stack.
  - Still blocked: golden rebaseline, final PR-08 closure and all-map closure.

## 2026-05-31 - D3 Grid Marker Edge Readability Evidence

- Loop type: approved D3 narrow readability implementation + closure decision
  packet
- Target area: Candidate C art-plate map-stage readability
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d3-readability-pass/*`
  - PR-08 goal / plan / log documents
- Implementation result:
  - ADDED: art-plate-only seam softener over adjacent visible floor shared
    seams.
  - ADDED: art-plate-only thin edge feather at the visible room clip.
  - KEPT: D4 suppression of old large decorative room compositor passes.
  - KEPT: legacy/no-plate renderer path unchanged.
  - KEPT: props, sprite overlays/telegraphs, actors, loot markers, fog,
    targeting highlights, active cursor and combat feedback above the room
    compositor.
  - KEPT: no `core` / `game` changes, no golden rebaseline, no final closure,
    no all-map closure.
- RED check:
  - Extended
    `TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive`
    first to require art-plate seam softener, edge feather and high-alpha active
    cursor preservation.
  - The test failed before implementation because the art-plate compositor had
    no seam/edge readability pass.
  - After first green, visual evidence showed the softener was too weak, so the
    test expectation was tightened to require mid-low alpha seam softening and
    the implementation was adjusted.
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive
  ```
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew :client:test \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset \
    --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere
  ```
- PASS:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew manifestLint resourcePipelineLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope maintainabilityLint
  ```
- Expected golden evidence failure:
  ```bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
  ```
  - Result: failed because PR-08 exploration does not update golden baselines.
  - `ui-demo-new-map-stage-crop` expected
    `94372439d8aeecf969ee3e2429321a12f2551cd1042f125a6759f1832ed08ad5`, actual
    `5a63fbcb70f943cb9033d3a2075a46999a18c5e8e08b449f233e934980ec530b`.
- Evidence files:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d3-readability-pass/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d3-readability-pass/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d3-readability-pass/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d3-readability-pass/d4-vs-d3-readability-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d3-readability-pass/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d3-readability-pass/golden-status.txt`
- Visual read:
  - The D3 pass is better than D4: shared floor seams are less harsh and the
    room edge transition is less abrupt.
  - It is still not fully gridless because Candidate C itself contains a fairly
    regular square slab rhythm in the right-side room area.
  - Continuing to tune renderer overlays risks recreating the slow micro-pass
    loop that Direction A was meant to escape.
- Director verdict:
  - `awaiting-lky-d3-map-stage-closure-decision`.
- Decision needed:
  - Accept D3 map-stage closure and proceed to final runtime packet / golden
    rebaseline decision path.
  - Or reject closure and approve a new/edited room art plate candidate with
    less baked square slab rhythm. This is the recommended route if the
    remaining grid-first read is still unacceptable.
  - Still blocked: golden rebaseline, final PR-08 closure and all-map closure.

## 2026-05-31 - Candidate F Final Packet + Bounded Composition Pass

- Loop type: agent-owned D3 decision packet / bounded runtime composition fix
- Target area: Candidate F room art plate runtime presentation, ruins-only
  resource boundary, fallback stability
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/assets/DarkUiMapVisualKeys.kt`
  - `client/src/main/kotlin/com/ktome/client/assets/ClientAssetLoadStrategy.kt`
  - `client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/*`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/pr08-room-art-plate-candidate-f-final-runtime-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/golden-status.txt`
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures"`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures"`
  - `./gradlew manifestLint resourcePipelineLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache`
- Result:
  - RED then GREEN: non-ruins snapshots no longer preload or render PR-08 ruins
    room presentation resources.
  - RED then GREEN: legacy `tileset.ruins.room_breakup_01` placement no longer
    drifts when the player moves inside the same room.
  - RED then GREEN: non-art-plate fallback renders floor material again.
  - PASS: focused PR-08 renderer and manifest tests.
  - PASS: resource authority gates and `maintainabilityLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this packet
    archives PR-08 evidence without rebaselining PR-02-1 golden hashes.
  - Latest actual focused golden hashes:
    `ui-demo-new-parity-1672x941=a948abb581328b8f176d352961a96d167b8623e3d666509be04bb20553a9d43b`,
    `ui-demo-new-parity-1280x800=6abb44b14429099ceec2d6ac9baa91870b785590bc1087878eeae96769f9d5a2`,
    `ui-demo-new-map-stage-crop=1ffb0314bcf9c9907daab737a35ae0813295aed0aad3c2cc78491dd6c63cc749`.
- Director verdict:
  - Candidate F is retained as the current resource foundation.
  - D3 map-stage closure is not accepted.
  - The bounded composition pass improved grid weight and fixed presentation
    boundary risks, but the map still reads partly as a tactical board and the
    plate/framing fit is not yet director-grade.
- Remaining risk:
  - Room plate bounds still use visible material AABB and need a placement /
    framing review before closure.
  - No golden baseline update is authorized.
  - Non-ruins tilesets still intentionally fall back to existing renderer
    paths; all-map closure remains blocked.
- Next action:
  - Run a bounded placement/framing pass for the ruins room plate, then produce
    a new packet. If that pass still fails the authored-room first-read test,
    use a targeted Candidate F art edit instead of renderer micro-tuning.

## 2026-05-31 - Candidate F Placement Framing Packet

- Loop type: agent-owned D3 decision packet / bounded placement-framing runtime
  pass
- Target area: Candidate F room art plate black-field hard cuts and idle grid
  grammar
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/*`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/pr08-room-art-plate-candidate-f-placement-framing-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/golden-status.txt`
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasAddsPr08ApertureShoulderForHiddenVoidsInsideRoomPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08IdleGridHintsBelowInteractionWeight`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasAddsPr08ApertureShoulderForHiddenVoidsInsideRoomPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08IdleGridHintsBelowInteractionWeight --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback`
  - `./gradlew maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache`
- Result:
  - RED then GREEN: focused tests proved the prior art-plate path had no
    internal hidden-void aperture shoulder and used too much idle grid weight.
  - PASS: focused PR-08 room art plate renderer tests.
  - PASS: `maintainabilityLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this packet
    archives PR-08 evidence without rebaselining PR-02-1 golden hashes.
  - Latest actual focused golden hashes:
    `ui-demo-new-parity-1672x941=cb1a627f7144e58843543f20c7d3b67f8e872fde647615b90ad7dfcb2147a113`,
    `ui-demo-new-parity-1280x800=84a94aff469104a3f8413b8168780714e0c1ce5e20a7b908622b17fa7d1bfc48`,
    `ui-demo-new-map-stage-crop=a83d37d11137a52e6ec6bfe2ad124cea9a9234611635f702ff66032551a5770c`.
- Director verdict:
  - Candidate F is retained as the current resource foundation.
  - D3 map-stage closure is not accepted.
  - The placement/framing pass improves the flat black-field hard cut and
    lowers idle grid hints, but the right combat field still reads too much as
    a square tactical board.
- Remaining risk:
  - This pass is still a bounded runtime integration pass, not a stronger
    underlying art-resource edit.
  - No golden baseline update is authorized.
  - Non-ruins tilesets still intentionally fall back to existing renderer
    paths; all-map closure remains blocked.
- Next action:
  - Perform a targeted Candidate F art edit focused on the center-right combat
    field and outer shadow shoulders. Keep the same resource route and packet
    loop; do not open a broad resource-family batch unless that targeted edit
    fails.

## 2026-06-01 - Candidate F Targeted Art Edit Packet

- Loop type: same-key resource edit / agent-owned D3 decision packet
- Target area: Candidate F center-right combat field and outer aperture
  shoulders
- Changed files:
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/targeted-candidate-f-art-edit-2026-06-01/candidates/*`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/*`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/pr08-room-art-plate-candidate-f-targeted-art-edit-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/placement-vs-targeted-art-edit-map-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/golden-status.txt`
- Commands run:
  - `./gradlew assetLint styleLint manifestLint resourcePipelineLint`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback`
  - `./gradlew maintainabilityLint`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache`
- Result:
  - REJECTED: built-in image-generation edit output changed room layout anchors
    and introduced statue/prop-like content.
  - SELECTED: deterministic local Candidate F edit D. Candidate C was too
    subtle; Candidate E was too dark for the current overlay stack.
  - PASS: resource authority lint for the same-key single-image route.
  - PASS: focused manifest preload/fallback and PR-08 room art plate renderer
    tests.
  - PASS: `maintainabilityLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this packet
    archives PR-08 evidence without rebaselining PR-02-1 golden hashes.
  - Latest actual focused golden hashes:
    `ui-demo-new-parity-1672x941=f0fd5d3b6b4af98492359d73a69f53b130f18e2474bbb371e848f6fd0f668f88`,
    `ui-demo-new-parity-1280x800=c057e18ce94d0ebc6b8388b2ce39ea6a1560f546e4813322d78e59a15eca47f9`,
    `ui-demo-new-map-stage-crop=781eda1ab95dccd6400307db6e1358cd48f7d56cbbd7d17b34a9f45f70b6263b`.
- Director verdict:
  - Candidate F targeted art edit D is accepted forward as the current resource
    foundation.
  - D3 map-stage closure is not accepted.
  - The right combat field is darker and less flat, but runtime interaction
    squares still form the first-read tactical board.
- Remaining risk:
  - More same-resource darkening now has diminishing returns and may harm
    marker readability.
  - No golden baseline update is authorized.
  - Non-ruins tilesets still intentionally fall back to existing renderer
    paths; all-map closure remains blocked.
- Next action:
  - Run a structural interaction-overlay grammar pass for the active art-plate
    path. Preserve hover/selection/targeting clarity, but stop range/default
    tactical surfaces from becoming a square board over the authored room.

## 2026-06-01 - Candidate F Structural Overlay Grammar Packet

- Loop type: client-only art-plate runtime grammar / agent-owned D3 decision
  packet
- Target area: active `tileset.ruins` room art plate interaction, visibility
  and overlay read
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/*`
  - PR-08 goal / plan / log documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/pr08-room-art-plate-candidate-f-structural-overlay-grammar-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/golden-status.txt`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasAddsPr08ApertureShoulderForHiddenVoidsInsideRoomPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08IdleGridHintsBelowInteractionWeight --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasMergesPr08FogVeilsIntoRoomScaleBandsOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasLetsPr08RoomArtPlateOwnGroundMaterialInsteadOfBaseTileSquares --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08SpriteOverlayGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08TargetingGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback --tests com.ktome.client.render.TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell --no-configuration-cache`
  - `./gradlew maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache`
- Result:
  - RED then GREEN: art-plate idle MAP mode no longer draws global grid
    softener lines.
  - RED then GREEN: art-plate path no longer paints `tile_ground` base terrain
    over the pre-rendered room plate.
  - RED then GREEN: explored fog is grouped into larger art-plate regions when
    topology allows and uses lower art-plate-only opacity.
  - RED then GREEN: art-plate vfx/telegraph overlays use lower asset alpha plus
    restrained runtime marks.
  - PASS: focused PR-08 renderer/fallback/telegraph tests.
  - PASS: `maintainabilityLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this packet
    archives PR-08 evidence without rebaselining PR-02-1 golden hashes.
  - Latest actual focused golden hashes:
    `ui-demo-new-parity-1672x941=4a55f315c4d239bc14787070a0a846ffac2cefe568854d4c9a16853492a0fc85`,
    `ui-demo-new-parity-1280x800=69d8dd728ffdf8e1d85ac02983c8b9679a787e010a324540d1b76101677d5d81`,
    `ui-demo-new-map-stage-crop=d48c1f6b0f7dbc767218e081555ecb02c1d9b325a1cf92ff11b306c051ff867d`.
- Director verdict:
  - Candidate F structural overlay grammar is accepted forward.
  - D3 map-stage closure is not accepted.
  - The authored room plate is more visible under hidden/explored shoulders,
    and idle runtime grid no longer dominates by itself, but the right combat
    field still reads partly as tile-shaped marker and visibility surfaces.
- Remaining risk:
  - This is still runtime grammar work, not final art direction closure.
  - Marker/loot/player backing surfaces remain square enough to preserve a
    tactical-board read in the combat field.
  - No golden baseline update is authorized.
  - Non-ruins tilesets still intentionally fall back to existing renderer
    paths; all-map closure remains blocked.
- Next action:
  - Run a marker-surface grammar pass for art-plate mode. Keep actor, loot,
    player and target readability, but stop item/selection backing surfaces
    from becoming filled square cards over the authored environment.

## 2026-06-01 - Candidate F Marker-Surface Grammar Packet

- Loop type: client-only art-plate runtime marker grammar / agent-owned D3
  decision packet
- Target area: active `tileset.ruins` room art plate player, loot and active
  cursor marker surfaces
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/*`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/pr08-room-art-plate-candidate-f-marker-surface-grammar-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/structural-overlay-vs-marker-surface-map-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/golden-status.txt`
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08MarkerSurfaceGrammarOverRoomArtPlate --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasAddsPr08ApertureShoulderForHiddenVoidsInsideRoomPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08IdleGridHintsBelowInteractionWeight --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasMergesPr08FogVeilsIntoRoomScaleBandsOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasLetsPr08RoomArtPlateOwnGroundMaterialInsteadOfBaseTileSquares --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08SpriteOverlayGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08TargetingGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08MarkerSurfaceGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback --tests com.ktome.client.render.TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell --no-configuration-cache`
  - `./gradlew maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas draws ground loot marker with count badge and rarity glyph" --tests com.ktome.client.render.TileRendererCanvasTest.drawsGroundLootMarkerAboveTerrainAndBelowBlockingActorBadge --tests "com.ktome.client.render.TileRendererCanvasTest.combat decision target cursor marks illegal hover without relying on toast" --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache`
- Result:
  - PASS: focused marker-surface canvas test.
  - PASS: focused PR-08 renderer/fallback/telegraph tests.
  - PASS: legacy non-art-plate loot/cursor smoke tests.
  - PASS: `maintainabilityLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this packet
    archives PR-08 evidence without rebaselining PR-02-1 golden hashes.
  - Latest actual focused golden hashes:
    `ui-demo-new-parity-1672x941=23e91f4da003b869d0c37078f2e088afab9a1687a55ce982abe04ebc918c0da1`,
    `ui-demo-new-parity-1280x800=554efeb12703bf7bc9b5f0d237a9da1357fc87c669777547bb5fc2a89e19b0b0`,
    `ui-demo-new-map-stage-crop=124ba9a1938e51856f81695e2920c047d0aaa5a607534bdbb8c4af519a04335f`.
- Director verdict:
  - Candidate F marker-surface grammar is accepted forward.
  - D3 map-stage closure is not accepted.
  - The white loot backing card, purple/yellow weapon square card and player
    full-tile gold frame are materially reduced, but the right combat field
    still reads partly through target/range and visibility grid topology.
- Remaining risk:
  - This pass improves marker grammar; it does not prove final authored-room
    closure against `UI/UI-demo-new.png`.
  - No golden baseline update is authorized.
  - Non-ruins tilesets still intentionally fall back to existing renderer
    paths; all-map closure remains blocked.
- Next action:
  - Either run one bounded topology grammar pass for interaction/visibility
    surfaces or stop renderer micro-iteration and structurally edit the room
    plate around a less grid-shaped combat field. Do not do another marker alpha
    tweak.

## 2026-06-01 - Candidate F Topology Grammar Packet

- Loop type: client-only art-plate runtime targeting topology grammar /
  agent-owned D3 decision packet
- Target area: active `tileset.ruins` room art plate targeting/range topology
  surfaces
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/*`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/pr08-room-art-plate-candidate-f-topology-grammar-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/marker-surface-vs-topology-grammar-map-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/ui-demo-new-parity-1280x800.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/golden-status.txt`
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08TargetingGrammarOverRoomArtPlate --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasAddsPr08ApertureShoulderForHiddenVoidsInsideRoomPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08IdleGridHintsBelowInteractionWeight --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasMergesPr08FogVeilsIntoRoomScaleBandsOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasLetsPr08RoomArtPlateOwnGroundMaterialInsteadOfBaseTileSquares --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08SpriteOverlayGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08TargetingGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08MarkerSurfaceGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback --tests com.ktome.client.render.TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell --no-configuration-cache`
  - `./gradlew maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas draws ground loot marker with count badge and rarity glyph" --tests com.ktome.client.render.TileRendererCanvasTest.drawsGroundLootMarkerAboveTerrainAndBelowBlockingActorBadge --tests "com.ktome.client.render.TileRendererCanvasTest.combat decision target cursor marks illegal hover without relying on toast" --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache`
- Result:
  - PASS: focused topology/targeting canvas test.
  - PASS: focused PR-08 renderer/fallback/telegraph tests.
  - PASS: legacy non-art-plate loot/cursor smoke tests.
  - PASS: `maintainabilityLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this packet
    archives PR-08 evidence without rebaselining PR-02-1 golden hashes.
  - Latest actual focused golden hashes:
    `ui-demo-new-parity-1672x941=23e91f4da003b869d0c37078f2e088afab9a1687a55ce982abe04ebc918c0da1`,
    `ui-demo-new-parity-1280x800=554efeb12703bf7bc9b5f0d237a9da1357fc87c669777547bb5fc2a89e19b0b0`,
    `ui-demo-new-map-stage-crop=124ba9a1938e51856f81695e2920c047d0aaa5a607534bdbb8c4af519a04335f`.
- Director verdict:
  - Candidate F topology grammar is accepted forward for targeting interaction
    readability.
  - D3 map-stage closure is not accepted.
  - The targeting path no longer draws all visible-floor adjacency seams, but
    the default map-stage crop is unchanged because it does not enter targeting
    mode.
- Remaining risk:
  - This pass removes a real interaction-grid source but does not improve the
    default first-read crop.
  - The center-right combat field still reads too much like a baked square
    board in the default runtime evidence.
  - No golden baseline update is authorized.
- Next action:
  - Stop renderer micro-iteration for this blocker. Perform a structural
    room-plate art edit focused on the center-right combat field's material
    clumps, light breakup, silhouette and baked square rhythm.

## 2026-06-01 - Candidate F Structural Room Plate Art Edit Packet

- Loop type: same-key room art plate structural resource edit / agent-owned D3
  decision packet
- Target area: center-right combat field inside active `tileset.ruins` room art
  plate
- Changed files:
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/*`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/pr08-room-art-plate-structural-art-edit-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/structural-room-plate-art-edit-candidate-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/topology-vs-structural-art-runtime-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/runtime-k-combat-field-breakup/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/runtime-k-combat-field-breakup/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/candidate-hashes.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/golden-status.txt`
- Commands run:
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures" --no-configuration-cache`
  - `./gradlew tasks --all --no-configuration-cache | rg "resourcePipelineLint|darkManifestCoverageLint|sprite|visual"`
  - `./gradlew resourcePipelineLint --no-configuration-cache`
- Result:
  - EXPECTED FAIL: focused golden screenshot hash gate, because this packet
    archives PR-08 evidence without rebaselining PR-02-1 golden hashes.
  - PASS: PR-08 exact manifest/preload focused tests.
  - PASS: `resourcePipelineLint`.
  - Latest selected runtime hashes:
    `ui-demo-new-parity-1672x941=ece9518e23593c506e95e9652de2dd9e32a69c6c36dba54a555d53685a1a6a81`,
    `ui-demo-new-parity-1280x800=e1df37bf3ba8fe21d0afb388bf639bbc7ab72cc031127a85c68d8ba127859ff7`,
    `ui-demo-new-map-stage-crop=aa39bb4220f4e9e66da5574de02d7b7dcaae982f49375725bad82b2761c018be`.
  - Selected resource hash:
    `ui_map_stage_ruins_room_plate_pr08_demo.png=0e996f13c2bef96e19ab387bef8a805d7a4d9276499f286c4ca7af490d730472`.
- Director verdict:
  - Candidate K is accepted forward as the current same-key room plate
    foundation.
  - D3 map-stage closure is not accepted.
  - K reduces the center-right combat field's baked square-board read better
    than I/J while avoiding I's highest darkness risk, but the crop is still
    not at `UI/UI-demo-new.png` quality.
- Remaining risk:
  - The default map-stage still has visible tactical-grid influence.
  - This was a deterministic procedural paintover, not a full new generated art
    source; one stronger structural resource pass may still be needed.
  - No golden baseline update is authorized.
- Next action:
  - Continue with a stronger structural art-resource generation or paintover
    pass for the center-right combat field. Keep runtime/gameplay authority out
    of the art and do not return to renderer alpha/topology micro-iteration.

## 2026-06-01 - Candidate F Imagegen Material Paintover Q Packet

- Loop type: same-key room art plate imagegen material paintover /
  agent-owned D3 decision packet
- Target area: center-right combat field inside active `tileset.ruins` room art
  plate
- Changed files:
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/*`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/pr08-room-art-plate-imagegen-material-paintover-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/structural-room-plate-art-edit-round2-candidate-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/structural-room-plate-art-edit-round2-opq-candidate-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-k-vs-q-selected-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-q-warm-readable-degrid/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-q-warm-readable-degrid/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/candidate-hashes.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/golden-status.txt`
- Commands run:
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures" --no-configuration-cache`
  - `./gradlew resourcePipelineLint --no-configuration-cache`
- Result:
  - EXPECTED FAIL: focused golden screenshot hash gate, because this packet
    archives PR-08 evidence without rebaselining PR-02-1 golden hashes.
  - PASS: PR-08 exact manifest/preload focused tests.
  - PASS: `resourcePipelineLint`.
  - Latest selected runtime hashes from the focused golden assertion:
    `ui-demo-new-parity-1672x941=e1f42774ab4fb6000d18179246512b3b03d5167532c572d3c6278e7339206c59`,
    `ui-demo-new-parity-1280x800=86d0940e4ae2ab148b1990c83f8d511ea56a401cd8a6fee89440b570c7373787`,
    `ui-demo-new-map-stage-crop=0f748ee34e09bf545cf1a3b27864ed7f688c916eab089e838f0d5a40c6fcc873`.
  - Selected resource hash:
    `ui_map_stage_ruins_room_plate_pr08_demo.png=e450669ec054d302fb62ac857dc5f3d7200ac8d297ac15b00b7f992de666f78e`.
- Director verdict:
  - Candidate Q is accepted forward as the current same-key room plate
    foundation.
  - D3 map-stage closure is not accepted.
  - Q improves K by using generated material language without copied room
    geometry, and it avoids the darkness/prop-leak risks in L/M/N/P.
- Remaining risk:
  - The default map-stage crop is still organized by cell-shaped runtime
    interaction/visibility surfaces over the right combat field.
  - No golden baseline update is authorized.
  - Non-ruins tilesets still intentionally fall back to existing renderer
    paths; all-map closure remains blocked.
- Next action:
  - Do not keep escalating tiny paintovers or alpha tweaks. Reassess the
    runtime surface stack at product-design level, or use a substantially
    stronger topology-preserving full-room source before another production
    resource replacement.

## 2026-06-01 - Candidate F Source-Cohesion Paintover S Packet

- Loop type: same-key room art plate source-cohesion paintover / client-only
  visibility-surface runtime grammar / agent-owned D3 decision packet
- Target area: right combat field source-baked grid and explored fog
  row/column banding in active `tileset.ruins` room art plate
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - PR-08 goal / plan / log / evidence documents
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-s-stronger-surface-cohesion/*`
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-s-stronger-surface-cohesion/ui_map_stage_ruins_room_plate_pr08_demo.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-s-stronger-surface-cohesion/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-s-stronger-surface-cohesion/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-q-vs-s-surface-cohesion-board.png`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08ExploredFogAsConnectedBlanketOverRoomArtPlate"`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasMergesPr08FogVeilsIntoRoomScaleBandsOverRoomArtPlate" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08ExploredFogAsConnectedBlanketOverRoomArtPlate"`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.*Pr08*" --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08*"`
  - `./gradlew maintainabilityLint resourcePipelineLint`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts"`
- Result:
  - RED/GREEN confirmed for the new connected explored-fog blanket assertion.
  - PASS: PR-08 renderer/manifest focused tests.
  - PASS: `maintainabilityLint resourcePipelineLint`.
  - EXPECTED FAIL: focused golden screenshot hash gate, because this packet
    archives PR-08 evidence without rebaselining PR-02-1 golden hashes.
  - Latest selected runtime hashes from the focused golden assertion:
    `ui-demo-new-parity-1672x941=6461fd8b4c273c526565f5c172757a7a0c13e080033cdb1bb8e492ae28d8d3a3`,
    `ui-demo-new-parity-1280x800=e2d3c7f4ce95e132fc3d005aa44e532ad3d1f9cdcfd93f3e6adcd9feac58dac9`,
    `ui-demo-new-map-stage-crop=48bc2d9b9c8f7e001c52d7f402b094efde495d6d13dbc76b138e94e9d6b745ed`.
  - Selected resource hash:
    `ui_map_stage_ruins_room_plate_pr08_demo.png=d0c3c2ea28c9493be4ab0d0acb441c92a5ce1564a8e081153ecf21697dca148f`.
- Director verdict:
  - Candidate S is accepted forward as the current same-key room plate
    foundation.
  - Connected explored-fog blanket is accepted forward because it removes a
    real row/column tactical-run failure mode while preserving runtime
    visibility authority.
  - D3 map-stage closure is not accepted.
  - S improves Q's right combat field material cohesion, but the crop still has
    visible tactical grid weight and should not be claimed as final
    `UI/UI-demo-new.png` quality.
- Remaining risk:
  - The tactical read-grid is still visible by design/necessity in the playable
    field; the unresolved decision is whether to make that an intentional
    product constraint or continue with a broader source-family pass.
  - No golden baseline update is authorized.
  - Non-ruins tilesets still intentionally fall back to existing renderer
    paths; all-map closure remains blocked.
- Next action:
  - Continue with either a broader room-scale composition/source-family pass or
    an explicit product decision to accept visible tactical grid as intentional.
    Do not spend the next slice on more per-cell fog alpha micro-tuning.

## 2026-06-01 - Candidate F Combat Surface Integration U Packet

- Loop type: same-key room art plate source-family comparison / runtime
  evidence packet / agent-owned D3 decision packet
- Target area: right combat field tactical lattice weight inside active
  `tileset.ruins` room art plate
- Changed files:
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/candidate-hashes.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-u-combat-field-surface-integration/*`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-v-continuous-combat-plane/*`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-u-combat-field-surface-integration/pr08-room-art-plate-combat-surface-u-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-u-combat-field-surface-integration/runtime-s-t-u-v-map-stage-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-u-combat-field-surface-integration/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-u-combat-field-surface-integration/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-u-combat-field-surface-integration/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-v-continuous-combat-plane/ui-demo-new-map-stage-crop.png`
- Commands run:
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache` with Candidate V temporarily promoted for runtime evidence
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache` with Candidate U promoted
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.*Pr08*" --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08*" --no-configuration-cache`
  - `./gradlew resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `./gradlew assetLint styleLint manifestLint --no-configuration-cache`
- Result:
  - EXPECTED FAIL: both focused golden screenshot runs failed because PR-08
    exploration writes runtime evidence without rebaselining PR-02-1 hashes.
  - PASS: PR-08 renderer/manifest focused tests.
  - PASS: `resourcePipelineLint`, `maintainabilityLint`, `assetLint`,
    `styleLint`, and `manifestLint`.
  - Latest selected runtime hashes:
    `ui-demo-new-parity-1672x941=fbe5d8fb3818663ff865d0104d57048d5a09d3b8a4dead2554dcf1466da13aba`,
    `ui-demo-new-parity-1280x800=9f6a3dc2c2d5c9282b59922be59000abf9afa3fb91ed2e8e580cbecb2c174e49`,
    `ui-demo-new-map-stage-crop=c66735a05fbe99b883fc80adfd621215f4e0b73328034ea00c6684f1b174c895`.
  - Selected resource hash:
    `ui_map_stage_ruins_room_plate_pr08_demo.png=2d1f7e38054df9a899cbf1ec414f79e3fd39a0bcae1f0e462811790b1e302f01`.
- Director verdict:
  - Candidate U is accepted forward as the current same-key room plate
    foundation.
  - D3 map-stage closure is not accepted.
  - U improves the right combat-field read over S/T and avoids V's excessive
    fog-plane wash, but the crop still carries tactical-grid weight and is not
    final `UI/UI-demo-new.png` quality.
- Remaining risk:
  - This is still a `tileset.ruins` proof slice only.
  - No golden baseline update is authorized.
  - The unresolved product/design decision remains whether visible tactical
    grid is acceptable as an intentional gameplay affordance or requires a
    higher-quality authored room source.
- Next action:
  - Continue with a higher-quality authored room source or explicitly record
    visible tactical grid as a product constraint. Do not spend the next slice
    on per-cell fog alpha, line-weight or rectangle micro-tuning.

## 2026-06-01 - Candidate W Authored Room Source Packet

- Loop type: same-key authored room source replacement / runtime evidence
  packet / agent-owned D3 decision packet
- Target area: active `tileset.ruins` room art plate first-read hierarchy
- Changed files:
  - `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`
  - `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/runtime-w-authored-room-source/*`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/runtime-w-authored-room-source/pr08-room-art-plate-authored-source-w-decision.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/runtime-w-authored-room-source/runtime-u-vs-w-authored-source-board.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/runtime-w-authored-room-source/ui-demo-new-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/runtime-w-authored-room-source/ui-demo-new-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/runtime-w-authored-room-source/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/runtime-w-authored-room-source/golden-status.txt`
- Commands run:
  - built-in image generation for a new authored room source, then copied the
    selected review copy into the existing same-key runtime resource path
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.*Pr08*" --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08*" --no-configuration-cache`
  - `./gradlew resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `./gradlew assetLint styleLint manifestLint --no-configuration-cache`
  - `git diff --check`
  - machine-local path scan for local home paths and transient generated-image
    cache references in touched W text artifacts
- Result:
  - EXPECTED FAIL: focused golden screenshot run failed because PR-08
    exploration writes runtime evidence without rebaselining PR-02-1 hashes.
  - PASS: PR-08 renderer/manifest focused tests.
  - PASS: `resourcePipelineLint`, `maintainabilityLint`, `assetLint`,
    `styleLint`, `manifestLint`, and `git diff --check`.
  - PASS: touched W text artifacts contain no local home path or transient
    generated-image cache references.
  - Latest selected runtime hashes:
    `ui-demo-new-parity-1672x941=dff87e79ca72445c3d77e1509799569c44daaa46d70b86c2f52cf806addc2180`,
    `ui-demo-new-parity-1280x800=24214c574ab650b830d02f4a6903500943efeb40e513e417dfb016439f6dc9b8`,
    `ui-demo-new-map-stage-crop=84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676`.
  - Selected resource hash:
    `ui_map_stage_ruins_room_plate_pr08_demo.png=ccbb9a6d923b201fef03ce1013d1f7ae453f373d1ea9b1b47532bb989b66c742`.
- Director verdict:
  - Candidate W is accepted as the current same-key room art plate foundation.
  - D3 map-stage closure is accepted for the `tileset.ruins` proof slice only.
  - The first read now comes from authored room structure, wall mass, torch
    pools and rough stone breakup before tactical grid weight, while gameplay
    markers remain readable.
- Remaining risk:
  - This is still a `tileset.ruins` proof slice only.
  - Final PR-08 closure, all-map closure, packaged whitebox and golden
    rebaseline are not authorized by this packet.
  - Non-ruins room families have not been generated or validated.
- Next action:
  - Build the final ruins runtime packet and closure checklist, then decide
    whether to expand to separate forest, mine and shadow-depth room art
    families. Do not return to per-cell fog, line-weight or rectangle
    micro-tuning for the accepted ruins D3 slice.

## 2026-06-01 - PR08 Director Ruins Runtime Evidence Packet

- Loop type: PR08 director evidence seam / final ruins runtime packet
- Target area: `tileset.ruins` Candidate W runtime evidence, PR08-specific
  golden labels, map/right/bottom/telegraph evidence coverage
- Changed files:
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/*`
  - `UI/review/dark-uiux-pr08-evidence-and-direction.md`
  - PR-08 goal / plan / log documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/pr08-final-ruins-runtime-packet.md`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-right-panel-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-telegraph-combat-crop.png`
- Commands run:
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr05 map actor portrait golden evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - visual inspection of the generated PR08 full/map/telegraph artifacts
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `./gradlew maintainabilityLint --no-configuration-cache`
- Result:
  - PASS: final PR08 director runtime evidence test.
  - PASS: `maintainabilityLint`.
  - The first combined PR08/PR05 run passed mechanically, but visual inspection
    rejected the initial telegraph crop because it captured the runtime error
    screen from the `dark-uiux-pr05-actor-boss-telegraph` validation path.
  - The final PR08 telegraph crop now uses the existing gameplay PR05 boss
    telegraph setup path and captures a real map-stage telegraph/combat scene.
  - New PR08 director runtime hashes:
    `dark-uiux-pr08-director-parity-1672x941=dff87e79ca72445c3d77e1509799569c44daaa46d70b86c2f52cf806addc2180`,
    `dark-uiux-pr08-director-map-stage-crop=84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676`,
    `dark-uiux-pr08-director-right-panel-crop=36e9b06dc1aba326458cd9c907e02062130d7fb5321007f13483fd18e96ccfcc`,
    `dark-uiux-pr08-director-bottom-deck-crop=6bf2380371c23586c38737409e70fb487c903b82b89ed989fc80c0f8e837a142`,
    `dark-uiux-pr08-director-telegraph-combat-crop=b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321`.
- Director verdict:
  - Accepted as the reproducible PR08 director runtime evidence packet for the
    `tileset.ruins` Candidate W proof slice.
  - This upgrades evidence routing away from PR02-1 expected-fail drift and
    into fixed `dark-uiux-pr08-director-*` labels.
  - It still does not close final PR08: right panel and bottom deck remain
    below the reference quality bar, non-ruins families are not covered, and
    packaged whitebox / final rebaseline remain open.
- Remaining risk:
  - `dark-uiux-pr05-actor-boss-telegraph` validation currently exposes an
    `icon.status.invulnerable` missing visual path if used as direct render
    evidence. This packet avoids it for PR08 evidence rather than silently
    accepting an error screen.
  - Full `:client:goldenScreenshot` remains expected to fail until an explicit
    PR08 rebaseline decision is made; this packet only adds the PR08-specific
    runtime evidence seam.
- Next action:
  - Either run the packaged whitebox/runtime parity closure path for PR08
    director evidence, or plan separate room art families for forest, mine and
    shadow-depths. Do not return to per-cell fog, line-weight or rectangle
    micro-tuning for the accepted ruins D3 slice.

## 2026-06-01 - PR08 Packaged Whitebox Partial Runtime Parity

- Loop type: closure / packaged whitebox materialization / partial CUA runtime
  parity
- Target area: `dark-uiux-pr08-director-grade` validation scenario and packaged
  runtime parity record for the accepted `tileset.ruins` Candidate W proof slice
- Changed files:
  - `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`
  - `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt`
  - `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioMaterializationCatalog.kt`
  - `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml`
  - `game/src/main/resources/i18n/en-US.json`
  - `game/src/main/resources/i18n/zh-CN.json`
  - `game/src/test/kotlin/com/ktome/game/validation/ValidationScenarioRegistryTest.kt`
  - `client/src/test/kotlin/com/ktome/client/input/ValidationCommandSourceTest.kt`
  - `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt`
  - `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
  - PR-08 goal / plan / log / evidence documents
- Visual evidence:
  - `build/whitebox/dark-uiux-pr08-director-grade/cua-runbook.md`
  - `build/whitebox/dark-uiux-pr08-director-grade/expected-evidence.json`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-parity-1672x941.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-map-stage-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-right-panel-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-telegraph-combat-window.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-packaged-crop-contact-sheet.png`
  - `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
- Commands run:
  - `./gradlew :game:test --tests "com.ktome.game.validation.ValidationScenarioRegistryTest" :client:test --tests "com.ktome.client.input.ValidationCommandSourceTest" :tools:test --tests "com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest" --no-configuration-cache`
  - repeated the same focused test command after fixing the test-local scenario
    yaml fixture
  - `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-grade --no-configuration-cache`
  - `build/whitebox/dark-uiux-pr08-director-grade/launch-packaged-app.sh`
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --out build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-parity-1672x941.png`
  - `magick` crop derivation from the packaged full-window capture
  - `osascript` keyboard attempt for `V, Right, Right, Enter, Esc`
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --out build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-telegraph-combat-window.png`
  - `kill "$(cat build/whitebox/dark-uiux-pr08-director-grade/evidence/app.pid)"`
- Result:
  - Initial focused test run failed because the test-local whitebox yaml fixture
    was missing `dark-uiux-pr08-director-grade`; this was fixed and rerun.
  - PASS: focused registry / presentation / whitebox CLI tests after fixture
    update.
  - PASS: `:client:packageMacApp preparePhase4V4Whitebox` generated the PR08
    runbook, expected evidence plan, app hash and launch script.
  - PARTIAL: packaged app launched and produced a real window capture with app
    pid `15996`.
  - PARTIAL: map-stage, right-panel and bottom-deck crops were derived from the
    packaged full-window capture.
  - BLOCKED: the packaged window opened with CoreGraphics bounds `1280x828`, not
    the runbook target `1672x941`; processed evidence is `1600x1064`.
  - BLOCKED: scripted keyboard input did not visibly reach
    `prepare-map-telegraph`; `dark-uiux-pr08-director-telegraph-combat-crop.png`
    is still missing from packaged evidence.
  - BLOCKED: `log.validation.phase4_v4.action` is missing from
    `dark-uiux-pr08-director-grade-app.log`.
- Director verdict:
  - Not final close. The packaged app confirms the accepted room-art plate
    foundation is present in runtime and still reads as authored ruins-room
    structure, but the packaged CUA path has not proven telegraph/combat overlay
    readability or required scenario action logging.
- Remaining risk:
  - The whitebox materialization records the target window size but does not
    currently enforce the packaged window dimensions.
  - Keyboard automation or validation action focus is not reliable enough for
    PR08 final closure.
  - Full `verifyChanged`, full `:client:goldenScreenshot`, owner lint ladder and
    non-ruins families remain open.
- Next action:
  - Fix or explicitly document the packaged whitebox interaction path so
    `prepare-map-telegraph` is reachable from `dark-uiux-pr08-director-grade`,
    then recapture telegraph/combat and rerun the final PR08 closure gate ladder.

## 2026-06-01 - PR08 Packaged Telegraph Parity Capture

- Loop type: closure / packaged whitebox blocker removal / runtime evidence
- Target area: `dark-uiux-pr08-director-grade` packaged action path, launcher
  window-size wiring and telegraph/combat crop evidence
- Changed files:
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
  - `client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt`
  - `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt`
  - `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
  - `client/src/test/kotlin/com/ktome/client/DesktopLauncherTest.kt`
  - `client/src/test/kotlin/com/ktome/client/input/ValidationCommandSourceTest.kt`
  - `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt`
  - `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
  - PR-08 plan / log / evidence documents
- Visual evidence:
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-parity-1672x941.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-map-stage-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-right-panel-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-telegraph-combat-window.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-telegraph-combat-crop.png`
  - `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
- Commands run:
  - `./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest" --tests "com.ktome.game.validation.ValidationScenarioRegistryTest" :client:test --tests "com.ktome.client.input.ValidationCommandSourceTest" --tests "com.ktome.client.DesktopLauncherTest" :tools:test --tests "com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest" --no-configuration-cache`
  - `./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest.dark uiux pr08 map telegraph action materializes packaged evidence surface" --no-configuration-cache`
  - `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-grade --no-configuration-cache`
  - `build/whitebox/dark-uiux-pr08-director-grade/launch-packaged-app.sh`
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --pid 20494 --raw --out build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-parity-1672x941.png`
  - direct CGEvent key posting for `V, Right, Right, Enter, Esc`
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --pid 20494 --raw --out build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-telegraph-combat-window.png`
  - `magick` crop derivation from packaged full-window captures
- Result:
  - PASS: PR08 `PREPARE_MAP_TELEGRAPH` is now accepted by the game session and
    logs `dark_uiux_pr08_map_telegraph_ready`.
  - PASS: PR08 map telegraph action uses the scenario's validation boss
    telegraph path instead of borrowing the PR05 molten-glass surface, avoiding
    the unrelated `icon.status.invulnerable` resource gap.
  - PASS: the whitebox launch script now writes
    `-Dktome.window.width=1672` and `-Dktome.window.height=941`; the desktop
    launcher consumes those properties.
  - PASS: packaged app pid `20494` produced full, map, right-panel, bottom-deck
    and telegraph/combat evidence with `.metadata.txt` and `.sha256` sidecars.
  - PASS: `dark-uiux-pr08-director-grade-app.log` contains both
    `log.validation.encounter.trigger_boss_telegraph` and
    `log.validation.phase4_v4.action`; no runtime-error or manifest-fallback
    fragments were found in the final app log.
  - LIMITATION: local CoreGraphics window bounds are `41,38,1471,944`, so exact
    `1672x941` logical window proof remains display-limited even though the app
    receives the materialization properties.
- Director verdict:
  - Not final close. The packaged telegraph blocker is removed for the accepted
    ruins proof slice, but full PR08 closure still requires the final owner gate
    ladder, explicit golden rebaseline decision and non-ruins-family strategy.
- Remaining risk:
  - Exact target-size proof needs a runner/display capable of hosting the full
    materialized logical width.
  - This pass covers `tileset.ruins` only; forest, mine and shadow-depths still
    need separate art-family decisions or fallback acceptance.
- Next action:
  - Resume the director-grade iteration goal with the packaged blocker removed:
    decide whether to run the final ruins closure ladder now or first broaden
    surface/room-family coverage.

## 2026-06-01 - PR08 Right Panel Command Surface Pass

- Loop type: implementation / focused shell quality slice / runtime golden
  evidence
- Target area: right-panel section surfaces and equipment socket grounding in
  the `dark-uiux-pr08-director-*` first-screen evidence packet
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-command-surface-2026-06-01/`
  - PR-08 plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/right-panel-command-surface-2026-06-01/right-panel-command-surface-before-after-board.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-command-surface-2026-06-01/dark-uiux-pr08-director-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-command-surface-2026-06-01/dark-uiux-pr08-director-right-panel-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-command-surface-2026-06-01/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-command-surface-2026-06-01/dark-uiux-pr08-director-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-command-surface-2026-06-01/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest.right\ panel\ sections\ use\ readable\ forged\ wells\ behind\ sockets\ and\ utility\ rows --no-configuration-cache`
  - repeated the same focused test after implementing the section and socket
    wells
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.right panel*" --tests "com.ktome.client.render.TileRendererCanvasTest.operation command matrix keeps caption hierarchy and non overlapping key chips" --tests "com.ktome.client.render.TileRendererCanvasTest.right operation hints sit on one forged command dock" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `magick` before/after board composition from the previous accepted packet
    and the new right-panel crop
- Result:
  - FAIL-FIRST: the new focused test initially failed because no right-panel
    section material wells existed.
  - PASS: the focused right-panel test now finds all four readable section
    wells and the expected forged equipment socket wells.
  - PASS: the broader right-panel / bottom-deck focused renderer tests and
    `maintainabilityLint` completed.
  - PASS: the PR08 director golden evidence command regenerated canonical full,
    map-stage, right-panel, bottom-deck and telegraph/combat artifacts.
  - PASS: the fixed map-stage hash remains
    `84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676`,
    while the right-panel crop hash updates to
    `337f719abdf81dd29cb6d1b7c0d21579b8f17e627dd9ba362db14233b4b790fa`.
- Director verdict:
  - Accepted forward only. The right column now reads more like one forged
    command surface: section rows sit on visible dark material wells and
    equipment sockets are grounded instead of floating in sparse chrome. This
    is a meaningful shell quality improvement after the ruins map proof slice,
    but not final PR08 closure.
- Remaining risk:
  - Packaged whitebox was not recaptured after this exact right-panel slice.
  - Bottom deck cohesion remains below the reference quality bar and is the
    next visible shell blocker.
  - Non-ruins room art families, final owner gate ladder, full `verifyChanged`
    and golden rebaseline remain open.
- Next action:
  - Continue with a bounded bottom-deck cohesion pass or packaged recapture for
    this packet before any final PR08 rebaseline decision.

## 2026-06-01 - PR08 Bottom Deck Command Shelf Pass

- Loop type: implementation / focused shell quality slice / runtime golden
  evidence
- Target area: bottom HUD hero/action/log hierarchy in the
  `dark-uiux-pr08-director-*` first-screen evidence packet
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/bottom-deck-command-shelf-2026-06-01/`
  - PR-08 plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/bottom-deck-command-shelf-2026-06-01/bottom-deck-command-shelf-before-after-board.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-deck-command-shelf-2026-06-01/dark-uiux-pr08-director-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-deck-command-shelf-2026-06-01/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-deck-command-shelf-2026-06-01/dark-uiux-pr08-director-right-panel-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-deck-command-shelf-2026-06-01/dark-uiux-pr08-director-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-deck-command-shelf-2026-06-01/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest.bottom\ hud\ masks\ inter\ panel\ seams\ with\ forged\ lock\ plates --no-configuration-cache`
  - repeated the same focused test after implementing bottom seam lock plates
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest.bottom\ hud\ subdues\ individual\ panel\ frames\ under\ the\ shared\ console\ shell --no-configuration-cache`
  - repeated the same focused test after lowering bottom child-frame alpha
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `magick` before/after board composition from the previous right-panel packet
    and the new bottom-deck crop
- Result:
  - FAIL-FIRST: the seam-lock focused test initially failed because no bottom
    inter-panel seam lock plate met the director-grade shelf criterion.
  - PASS: the seam-lock test passed after adding forged lock plates at the
    hero/action and action/log seams.
  - FAIL-FIRST: the child-frame hierarchy test initially failed because
    hero/action/log child frames still rendered at `0.86-0.90` alpha.
  - PASS: the child-frame hierarchy test passed after lowering bottom child
    frame alpha under the shared console shell.
  - PASS: the broader bottom focused renderer tests and `maintainabilityLint`
    completed.
  - PASS: the PR08 director golden evidence command regenerated canonical full,
    map-stage, right-panel, bottom-deck and telegraph/combat artifacts.
  - PASS: the fixed map-stage hash remains
    `84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676`,
    while the bottom-deck crop hash updates to
    `d465f7118466bd257ed9b25cf7baae17143efdec39fc77ec8630cca2e2000403`.
- Director verdict:
  - Accepted forward only. The bottom HUD now reads less like three adjacent
    cards because child-frame dominance is reduced and the two inter-panel
    seams have forged lock plates. The fixed crop still reads conservative
    rather than final director close, so this is not a golden rebaseline or
    final PR08 closure.
- Remaining risk:
  - Packaged whitebox was not recaptured after this exact bottom-deck slice.
  - The bottom shelf still lacks the reference image's full material richness
    and large-subject visual confidence.
  - Non-ruins room art families, final owner gate ladder, full `verifyChanged`
    and golden rebaseline remain open.
- Next action:
  - Either run packaged recapture for the current shell packet, or continue one
    more bounded bottom/action subject-material pass before any final PR08
    rebaseline decision.

## 2026-06-01 - PR08 Bottom Action Subject Material Pass

- Loop type: implementation / focused action quality slice / runtime golden
  evidence
- Target area: filled bottom action icon subjects inside the bottom HUD command
  shelf
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/bottom-action-subject-material-2026-06-01/`
  - PR-08 plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/bottom-action-subject-material-2026-06-01/bottom-action-subject-material-before-after-board.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-action-subject-material-2026-06-01/dark-uiux-pr08-director-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-action-subject-material-2026-06-01/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-action-subject-material-2026-06-01/dark-uiux-pr08-director-right-panel-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-action-subject-material-2026-06-01/dark-uiux-pr08-director-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-action-subject-material-2026-06-01/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action subjects sit on large material pedestals" --no-configuration-cache`
  - repeated the same focused test after increasing action icon scale and adding
    material pedestals
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `magick` before/after board composition from the previous bottom command
    shelf packet and the new bottom action crop
- Result:
  - FAIL-FIRST: the focused test initially failed because action icon subjects
    drew at roughly 75% of slot width and no large material pedestal existed.
  - PASS: the focused test passed after increasing filled action icon draw scale
    and adding dark material pedestals with warm crown lips.
  - PASS: broader bottom focused renderer tests and `maintainabilityLint`
    completed.
  - PASS: the PR08 director golden evidence command regenerated canonical full,
    map-stage, right-panel, bottom-deck and telegraph/combat artifacts.
  - PASS: the fixed map-stage hash remains
    `84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676`,
    while the bottom-deck crop hash updates to
    `db555eb0443980c7465ef4dd4b7d2a562196715de124922dfec07ee986b4a30d`.
- Director verdict:
  - Accepted forward only. Filled actions now read more like authored equipment
    subjects rather than small button glyphs, because the icon subjects are
    larger and sit on visible dark material pedestals. This improves the bottom
    shelf, but it is still not a final PR08 close.
- Remaining risk:
  - Packaged whitebox was not recaptured after this exact bottom-action slice.
  - The bottom shelf still lacks the reference image's full resource richness
    and final polish; a future resource-quality pass may still be needed.
  - Non-ruins room art families, final owner gate ladder, full `verifyChanged`
    and golden rebaseline remain open.
- Next action:
  - Run packaged recapture for the current shell packet before any closure or
    rebaseline decision, or continue with a bounded resource-quality pass if
    the action subjects remain below the reference bar after crop review.

## 2026-06-02 - PR08 Left Nav Rail Beacon Pass

- Loop type: implementation / focused permanent-navigation shell polish /
  runtime golden evidence
- Target area: left nav rail icon subject scale, material sockets and selected
  state beacon in the `dark-uiux-pr08-director-*` first-screen evidence packet
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/left-nav-rail-beacon-2026-06-02/`
  - PR-08 plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/left-nav-rail-beacon-2026-06-02/left-nav-rail-beacon-before-after-board.png`
  - `UI/review/dark-uiux-pr08-exploration/left-nav-rail-beacon-2026-06-02/dark-uiux-pr08-director-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/left-nav-rail-beacon-2026-06-02/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/left-nav-rail-beacon-2026-06-02/dark-uiux-pr08-director-right-panel-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/left-nav-rail-beacon-2026-06-02/dark-uiux-pr08-director-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/left-nav-rail-beacon-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.demo nav rail reads as icon first forged command rail" --no-configuration-cache`
  - repeated the same focused test after increasing nav icon scale and adding
    selected-state beacon plus material sockets
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.demo nav*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas places shell rail and right panel inside their bounds" --tests "com.ktome.client.render.TileRendererCanvasTest.right panel*" --tests "com.ktome.client.render.TileRendererCanvasTest.operation command matrix keeps caption hierarchy and non overlapping key chips" --tests "com.ktome.client.render.TileRendererCanvasTest.right operation hints sit on one forged command dock" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `magick` before/after board composition from the previous bottom-action
    packet and the new left-nav crop
- Result:
  - FAIL-FIRST: the focused test initially failed because left nav icon subjects
    were too small for the new permanent-navigation control criterion.
  - PASS: the focused test passed after reducing nav icon inset and adding dark
    material sockets plus a warm selected-state beacon.
  - PASS: broader shell/right/bottom/nav focused renderer tests and
    `maintainabilityLint` completed.
  - PASS: the PR08 director golden evidence command regenerated canonical full,
    map-stage, right-panel, bottom-deck and telegraph/combat artifacts.
  - PASS: the fixed map-stage hash remains
    `84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676`,
    while the full parity hash updates to
    `110ebe6abe8d58804fe777f2629bf0cf62fce04c69394be42da597b51e2e5f2a`.
- Director verdict:
  - Accepted forward only. The left permanent nav now reads more like a forged
    command spine because icon subjects are larger, every button has a dark
    material socket and the selected item has a warm beacon in addition to the
    restrained cyan wash. This improves first-screen shell quality, but it is
    not final PR08 close.
- Remaining risk:
  - Packaged whitebox was not recaptured after this exact left-nav slice.
  - Bottom-strip final-read remains the highest visible shell blocker according
    to fixed-crop review: hero hierarchy, filled-vs-empty action weight and log
    route-hint scanability still need a bounded pass.
  - Non-ruins room art families, final owner gate ladder, full `verifyChanged`
    and golden rebaseline remain open.
- Next action:
  - Continue with a bounded bottom-strip final-read pass before packaged
    recapture or final PR08 rebaseline. A right-panel backpack/operation bridge
    remains a valid later polish candidate, but it should not be bundled with
    the bottom strip.

## 2026-06-02 - PR08 Bottom Strip Final-Read Pass

- Loop type: implementation / focused bottom-HUD final-read slice / runtime
  golden evidence
- Target area: hero gauge hierarchy, filled-vs-empty action command weight and
  long route-hint scanability in the bottom strip
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/`
  - PR-08 plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/bottom-strip-final-read-before-after-board.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/dark-uiux-pr08-director-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/dark-uiux-pr08-director-right-panel-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/dark-uiux-pr08-director-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.DemoShellRendererTest.empty action reserve socket stays below equipped action hierarchy" --tests "com.ktome.client.render.DemoShellRendererTest.hero gauges sit in individual forged troughs instead of flat color strips" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" --no-configuration-cache`
  - repeated the same focused command after implementation and test boundary
    correction
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.demo nav*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas places shell rail and right panel inside their bounds" --tests "com.ktome.client.render.TileRendererCanvasTest.right panel*" --tests "com.ktome.client.render.TileRendererCanvasTest.operation command matrix keeps caption hierarchy and non overlapping key chips" --tests "com.ktome.client.render.TileRendererCanvasTest.right operation hints sit on one forged command dock" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `magick` before/after board composition from the previous left-nav packet
    and the new bottom-strip crop
- Result:
  - FAIL-FIRST: the tightened reserve socket assertion failed against the old
    empty action asset alpha `0.46`.
  - FAIL-FIRST: the hero gauge trough assertion failed before individual
    forged troughs existed.
  - FAIL-FIRST: the long route-hint note-well assertion failed before log row
    note wells were added.
  - PASS: focused bottom-strip tests passed after adding per-gauge troughs,
    quieter reserve socket treatment and log note wells / right braces.
  - PASS: broader shell/right/bottom/nav focused renderer tests and
    `maintainabilityLint` completed.
  - PASS: the PR08 director golden evidence command regenerated canonical full,
    map-stage, right-panel, bottom-deck and telegraph/combat artifacts.
  - PASS: fixed guard hashes remain stable for the map-stage
    `84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676`,
    right-panel
    `337f719abdf81dd29cb6d1b7c0d21579b8f17e627dd9ba362db14233b4b790fa`
    and telegraph/combat
    `b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321`
    crops.
  - PASS: the bottom-deck crop hash updates to
    `a416b96d21059212a6a7682f8163f0f065dcac3b545bad28b064814f8120cdfd`
    and the full parity hash updates to
    `ff1ccb08a886097f20e9aa31152e035f3550c101d874cb72c7b37bb527637d32`.
- Director verdict:
  - Accepted forward only. The bottom strip now has stronger final-read
    hierarchy: hero resources sit in authored troughs, filled action commands
    dominate the empty reserve socket, and long route hints scan as ledger
    notes rather than flat paragraph text. This is a bounded bottom-HUD
    information-hierarchy improvement, not final PR08 closure.
- Remaining risk:
  - Packaged whitebox was not recaptured after this exact bottom-strip slice.
  - Non-ruins room art families, final owner gate ladder, full `verifyChanged`
    and golden rebaseline remain open.
  - A right-panel backpack/operation bridge may still be valuable, but it
    should be evaluated as its own bounded shell slice.
- Next action:
  - Decide between packaged recapture for the current shell packet and one
    bounded right-panel backpack/operation bridge polish before any final PR08
    rebaseline decision.

## 2026-06-02 - PR08 Right Panel Utility Bridge Pass

- Loop type: implementation / focused lower-right utility-surface slice /
  runtime golden evidence
- Target area: backpack page readout and operation hint relationship in the
  right panel
- Changed files:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/`
  - PR-08 plan / log / evidence documents
- Visual evidence:
  - `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/right-panel-utility-bridge-before-after-board.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/dark-uiux-pr08-director-parity-1672x941.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/dark-uiux-pr08-director-right-panel-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/dark-uiux-pr08-director-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/evidence-index.tsv`
- Commands run:
  - `./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.right panel backpack pager and operation hints share a utility bridge" --no-configuration-cache`
  - repeated the same focused command after implementation
  - `./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.demo nav*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas places shell rail and right panel inside their bounds" --tests "com.ktome.client.render.TileRendererCanvasTest.right panel*" --tests "com.ktome.client.render.TileRendererCanvasTest.operation command matrix keeps caption hierarchy and non overlapping key chips" --tests "com.ktome.client.render.TileRendererCanvasTest.right operation hints sit on one forged command dock" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache`
  - `./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache`
  - `magick` before/after board composition from the previous bottom-strip
    packet and the new right-panel crop
- Result:
  - FAIL-FIRST: the focused test initially failed because no paired bridge posts
    connected the backpack pager to the operation hints.
  - PASS: the focused test passed after adding the forged pager cradle and
    paired short bridge posts.
  - PASS: broader shell/right/bottom/nav focused renderer tests and
    `maintainabilityLint` completed.
  - PASS: the PR08 director golden evidence command regenerated canonical full,
    map-stage, right-panel, bottom-deck and telegraph/combat artifacts.
  - PASS: fixed guard hashes remain stable for the map-stage
    `84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676`,
    bottom-deck
    `a416b96d21059212a6a7682f8163f0f065dcac3b545bad28b064814f8120cdfd`
    and telegraph/combat
    `b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321`
    crops.
  - PASS: the right-panel crop hash updates to
    `201645bf13f9824fd95289063b92ea8adc722f9d0f344aca75902a2e99c26c47`
    and the full parity hash updates to
    `e7f65d0f2f205e238909ec1cd9f54387a9eaf365912e1da5710c9df9e72ed936`.
- Director verdict:
  - Accepted forward only. The lower-right utility area now reads as a more
    connected tool surface because the backpack pager sits in a forged cradle
    and has short posts linking it to the operation dock. This improves
    right-panel scanability, but it is not final PR08 closure.
- Remaining risk:
  - Packaged whitebox was not recaptured after this exact right-panel utility
    bridge slice.
  - Non-ruins room art families, final owner gate ladder, full `verifyChanged`
    and golden rebaseline remain open.
  - The right panel still uses runtime chrome/material passes rather than a
    full resource-family replacement for every lower-right utility component.
- Next action:
  - Run packaged recapture for the current shell packet before any final PR08
    rebaseline decision. Continue shell polish only if a new fixed-crop review
    identifies a higher-return blocker than packaged parity.

## 2026-06-02 - PR08 Packaged Recapture After Utility Bridge

- Loop type: packaged whitebox recapture / runtime parity evidence /
  documentation
- Target area: current PR08 shell packet after the right-panel utility bridge
  pass
- Changed files:
  - `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
  - PR-08 plan / log / evidence documents
- Visual evidence:
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-parity-1672x941.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-map-stage-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-right-panel-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-bottom-deck-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-telegraph-combat-window.png`
  - `build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-telegraph-combat-crop.png`
  - `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
- Commands run:
  - `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-grade --no-configuration-cache`
  - `build/whitebox/dark-uiux-pr08-director-grade/launch-packaged-app.sh`
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --pid 83825 --raw --out build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-parity-1672x941.png`
  - `magick` truecolor crop derivation for map/right/bottom from the packaged
    full-window capture
  - direct CGEvent key posting for `V, Right, Right, Enter`; `F9` to close the
    validation panel before final telegraph capture
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --pid 83825 --raw --out build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-telegraph-combat-window.png`
  - `magick` truecolor crop derivation for the telegraph/combat crop
  - `rg -n "ERROR|Exception|fallback|Missing|APP_LAUNCH_FAILED" build/whitebox/dark-uiux-pr08-director-grade/evidence/dark-uiux-pr08-director-grade-app.log`
- Result:
  - PASS: packaged app materialization and launch completed for
    `dark-uiux-pr08-director-grade`; pid `83825` was manually stopped after
    capture.
  - PASS: current packaged full, map-stage, right-panel and bottom-deck crops
    were captured after the right-panel utility bridge pass.
  - PASS: direct CGEvent input reached `prepare-map-telegraph`; the app log
    contains `log.validation.encounter.trigger_boss_telegraph` and
    `log.validation.phase4_v4.action`.
  - PASS: `F9` closed the validation panel for the final telegraph capture;
    `Esc` was not used as the accepted close key in this refresh because local
    key routing left the validation panel visible.
  - PASS: the final packaged telegraph/combat crop shows the runtime-owned
    actor, boss, warning cells and room-art plate without validation overlay.
  - PASS: no runtime-error or manifest-fallback fragments were found in the app
    log.
  - PASS: packaged evidence hashes are full
    `7161d87cb61714b7e66ef348b59b49fdf18bb5ae0790c50572b372f68a582b07`,
    map-stage
    `fe71863a305c5e23e133cd8a3405d9fc65ac8d0aeffd4e118a0274cebdb8feca`,
    right-panel
    `5e36045f1f512041b1dd4495bee40044d0bc35e16fa859855d02f2821c1b353c`,
    bottom-deck
    `50a282bcd4cd7fb648232b8ae4dd7666807c0fcf5de21c7bf99dcb7c80a59b7a`,
    telegraph window
    `960d475335caac566b433a28bd5abc57e4259505169d0d3195ad2f842c6e2e50`
    and telegraph crop
    `8b3b456d8c19ed8c4630a31abd2554e9704ec40fa3bf7fc93be9a969757dde53`.
- Director verdict:
  - Accepted as packaged parity refresh only. The current shell packet can be
    materialized and captured in the packaged app, and the telegraph/combat
    surface remains readable above the room plate. This is not final PR08
    closure.
- Remaining risk:
  - Exact `1672x941` logical window proof remains machine-display-limited:
    metadata still records CoreGraphics bounds `41,38,1471,944`.
  - This refresh did not rerun focused tests, `maintainabilityLint`,
    `verifyChanged`, or the full owner gate ladder.
  - Non-ruins room art families and explicit golden rebaseline remain open.
- Next action:
  - Decide between final owner gate ladder / explicit golden rebaseline for the
    accepted ruins slice and a broadened room-art-family pass. Do not stack more
    small shell bridge/rail tweaks unless fixed-crop evidence identifies a
    higher-return blocker.

## 2026-06-02 - PR08 Non-Ruins Packaged Core Crop Capture

- Loop type: packaged whitebox capture / fixed-crop director review /
  documentation
- Target area: non-ruins room-art family packaged parity for forest-edge, mine
  and shadow-depths
- Changed files:
  - `UI/manual-records/dark-uiux-pr08-non-ruins-packaged-parity.md`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/evidence-index.tsv`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/non-ruins-room-art-family-decision.md`
  - PR-08 goal / plan / log documents
- Visual evidence:
  - `build/whitebox/dark-uiux-pr08-director-forest-map-stage/evidence/dark-uiux-pr08-director-forest-parity-1280x800.png`
  - `build/whitebox/dark-uiux-pr08-director-forest-map-stage/evidence/dark-uiux-pr08-director-forest-map-stage-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-forest-map-stage/evidence/dark-uiux-pr08-director-forest-right-panel-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-forest-map-stage/evidence/dark-uiux-pr08-director-forest-bottom-deck-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-forest-map-stage/evidence/dark-uiux-pr08-director-forest-evidence-summary.png`
  - `build/whitebox/dark-uiux-pr08-director-mine-map-stage/evidence/dark-uiux-pr08-director-mine-parity-1280x800.png`
  - `build/whitebox/dark-uiux-pr08-director-mine-map-stage/evidence/dark-uiux-pr08-director-mine-map-stage-crop.png`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-parity-1280x800.png`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-map-stage-crop.png`
  - `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/packaged-non-ruins-family-map-stage-crop-board.png`
- Commands run:
  - `build/whitebox/dark-uiux-pr08-director-forest-map-stage/launch-packaged-app.sh`
  - `build/whitebox/dark-uiux-pr08-director-mine-map-stage/launch-packaged-app.sh`
  - `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/launch-packaged-app.sh`
  - `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --pid <scenario-pid> --raw --out <scenario full-window png>`
  - `magick` fixed-ratio crop derivation for map/right/bottom guard crops
  - `magick montage` for the packaged non-ruins family map-stage crop board
  - direct CGEvent / System Events / Computer Use input probes for validation
    overlay and `show-evidence-summary`
- Result:
  - PASS: forest-edge packaged full-window, map-stage, right-panel,
    bottom-deck and evidence-summary screenshots were captured; runtime app log
    contains two `action show-evidence-summary` entries after input retries.
  - PASS: mine packaged full-window, map-stage, right-panel and bottom-deck
    crops were captured; summary action capture is still pending because local
    input routing did not open validation overlay for the transient bundle.
  - PASS: shadow-depths packaged full-window, map-stage, right-panel and
    bottom-deck crops were captured; summary action capture is still pending
    for the same local input-routing reason.
  - PASS: no packaged app process was left running after capture.
  - PASS: generated board hash is
    `6143ee8037d2845d0a90ac58f86facf3481eeb5f1f70b0987ddf5426920e6251`.
- Director verdict:
  - Accepted forward only for core packaged crop evidence. Forest-edge and mine
    now hold up in packaged runtime and are distinct from the ruins proof
    slice. Shadow-depths V3 remains a real blocker: the packaged crop is
    legible but the right half is still too empty and grid-forward relative to
    forest/mine.
- Remaining risk:
  - Mine and shadow-depths still need official `show-evidence-summary`
    captures or an explicit validation-input bypass.
  - Shadow-depths needs another composition pass before non-ruins final parity.
  - This pass did not rerun Gradle tests; it only captured packaged runtime
    evidence from the already materialized scenario packets.
- Next action:
  - Either repair the local validation input path for mine/shadow summaries, or
    move directly into a bounded shadow-depths composition pass before returning
    to the broader director-grade iteration loop.

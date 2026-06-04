# Dark UI/UX PR-08 Evidence And Direction Brief

> Date: 2026-05-27
> Scope: `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md`
> Status: `direction-locked`

## Direct Conclusion

PR-08 的问题已经确定：primary root cause 是 `resource-gap`，secondary blockers 是 `lighting-gap` 和 renderer overlay budget。

后续大改造方向锁定为 `resource-family-first reset`：

1. floor/wall 资源先走 PR-08 formal supersession 和 owner coverage。
2. map darkness、warm light、selection、hazard、telegraph 留在 deterministic compositor，不烙进 tile 资源。
3. right panel、slot、bottom deck 走 reusable `ui_frame` chrome，不生成 baked text / baked item icon / full-screen UI paintover。
4. actor / prop 暂缓，只有在新 floor/wall/compositor 后 marker readability 仍失败时才进入 PR-08 supersession。
5. runtime integration、manifest sync、golden update 必须等 resource readiness、owner gate 和 focused renderer tests 接好后再做。

2026-06-03 D10 addendum:

1. `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` promotes the next PR-08 fix route from map-stage micro-polish to retained UI / theme authority.
2. Official libGDX Scene2D / Stage / Skin / Table is the adopted retained UI route.
3. KTX is deferred to a compatibility spike; Skin Composer and TexturePacker are authoring tools only; VisUI and gdx-skins are not runtime dependencies for D10.
4. Main menu, shell, inventory/detail, shop, talent tree, combat/frontstage, bottom deck and nav rail require retained UI adaptation plans and evidence labels.
5. D10 is phase-gated as `D10-P0 ~ D10-P8`; migrated surfaces must not keep old immediate UI as a long-term compatibility route.
6. This does not close the non-ruins topology-risk map-stage blocker; it changes the authority boundary so future map-stage work lives inside `MapStageActor` rather than another hand-written UI shell.

## Evidence Chain

| Step | Artifact | Result | Decision impact |
| --- | --- | --- | --- |
| PR contract | `UI/pr/dark-uiux-pr08-director-grade-asset-reset.md` | PR-08 必须先 subtractive spike、target sliceability、owner coverage，再进入 runtime reset | 禁止直接大改 renderer 或直接更新 golden |
| D10 retained UI authority | `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` | UI shell/layout/focus/theme should move to Scene2D retained UI through D10-P0~P8 phase gates | 不再把 right/bottom/main menu/talent/inventory/shop/combat/frontstage 继续写成 `TileCanvas` UI framework |
| Quality target | `UI/UI-demo-new.png` | 只作为第一眼质量参考，不是 mapping truth | 不得从 reference 反推 manifest / sheet / runtime key |
| Current runtime evidence | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 当前 runtime 仍低于 director-grade target | PR-08 必须重新验收 director labels |
| Current map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 暴露 repeated floor/wall、hard grid、弱暗场 | floor/wall/compositor 是主战场 |
| Subtractive full screen | `UI/review/dark-uiux-pr08-exploration/subtractive-ui-demo-new-parity-1672x941.png` | 禁用 decorative overlay 后 map 更清楚但不够好 | overlay cleanup 不是充分方案 |
| Subtractive map crop | `UI/review/dark-uiux-pr08-exploration/subtractive-ui-demo-new-map-stage-crop.png` | 原始 tile repetition 更明显，氛围变平 | 必须资源/合成重置 |
| Gap audit | `UI/review/dark-uiux-director-grade-gap-audit.md` | root-cause verdict = `resource-gap` | 大方向从“堆 renderer”转为“asset/compositor reset” |
| Target attempt 1 | `UI/targets/dark-uiux-director-grade-target-1672x941.png` | mood 好，但像整屏 ruin board，不可 runtime slice | rejected as implementation source |
| Attempt 1 tile truth | `UI/targets/dark-uiux-director-grade-target-32px-tile-truth.png` | 重复后出现明显模式 | 不接受整屏 crop 当 tile truth |
| Target attempt 2 | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt2.png` | map 更接近 orthographic，但含 baked English log text | rejected |
| Attempt 2 tile truth | `UI/targets/dark-uiux-director-grade-target-32px-tile-truth-attempt2.png` | sampled content 仍重复且不干净 | 证明需要 dedicated family pack |
| Target attempt 3 | `UI/targets/dark-uiux-director-grade-target-1672x941-attempt3.png` | 无明显 baked text，composition 最可用 | accepted only as mood/layout reference |
| Attempt 3 tile truth | `UI/targets/dark-uiux-director-grade-target-32px-tile-truth-attempt3.png` | crop 混入光照、毒池、道具、墙边黑场 | monolithic comp 不能当资源 authority |
| Family pack | `UI/targets/dark-uiux-director-grade-target-family-pack.md` | `accepted-for-direction` | 后续以 resource family 为最小变更单元 |
| Floor family | `UI/targets/dark-uiux-director-grade-target-family-floor-32px.png` | clean 8x4 floor direction; still needs final polish | floor 可进入 PR-08 supersession planning |
| Wall family | `UI/targets/dark-uiux-director-grade-target-family-wall-32px.png` | clean 8x4 wall direction; wall mass 区分 floor | wall 可进入 PR-08 supersession planning |
| Repeat sheet | `UI/targets/dark-uiux-director-grade-target-family-floor-wall-repeat.png` | 由 32px family cells 重排，不是整屏 crop | sliceability 方向成立 |
| Map compositor target | `UI/targets/dark-uiux-director-grade-target-family-map-compositor.png` | 黑场、光池、selection、hazard、telegraph 分层明确 | compositor 不应成为资源第二真源 |
| UI chrome target | `UI/targets/dark-uiux-director-grade-target-family-ui-chrome.png` | panel/divider/slot/deck/log surface 可分离且无 text | UI chrome 可按 reusable frame family 处理 |
| Manual record | `UI/manual-records/dark-uiux-director-grade-target-comp.md` | Verdict = `accepted-for-resource-family-direction` | 方向 accepted，但 runtime integration 未放行 |

## Measured Runtime Evidence

Subtractive spike 期间的 diagnostic instrumentation 已移除；以下数值只作为 PR-08 方向证据：

| Metric | Current measured value | PR-08 direction |
| --- | ---: | --- |
| `overlayFunctionCount` | 22 major decorative/material functions | renderer 必须收敛回 compositor，而不是继续加 micro-rectangles |
| `warmOverlaySubpassCount` | 20 | broad amber haze 要压缩成少量确定性 light layer |
| `materialRectCountPerVisibleCell` | 16.375 | floor/wall material 应进入资源，不应靠每格大量 rect 模拟 |
| `totalRectDraws` | 3784 | resource migration 后应明显下降；不是固定 gate，但要作为回归观察 |
| `assetDraws` | 212 | 资源路径存在，但资源内容不足以支撑 director-grade |

## Rejected Directions

| Direction | Verdict | Reason |
| --- | --- | --- |
| Pure overlay cleanup | rejected | Subtractive map 更清楚但变平，不能达到 `UI/UI-demo-new.png` 级别 |
| More renderer micro-rectangles | rejected | 当前 material/warm overlay budget 已经过高，继续堆会制造第二套视觉逻辑 |
| Monolithic full-screen paintover | rejected | attempt 1/2/3 均证明整屏 comp 不能提供 clean 32px floor/wall truth |
| Silent PR-02-2 reroll | rejected | PR-08 close evidence 用 `dark-uiux-pr08-director-*`，静默复用 PR-02-2 会造成 owner drift |
| New manifest schema / atlas / region schema | rejected for PR-08 | PR-08 合同要求 `requiresNewSchema=no`，schema epoch 必须另拆 |
| Actor/prop upfront reroll | deferred | 当前 blocker 是 floor/wall/compositor/chrome；actor/prop 只有 marker readability 失败时才升级 |
| Baked UI text / hotkeys / log content in images | rejected | text 必须 runtime-owned，资源中不得有中文、英文、数字、hotkey、manifest key |

## Runtime Iteration Evidence

| Iteration | Artifact | Verdict | Decision impact |
| --- | --- | --- | --- |
| V10 source de-line | `UI/review/dark-uiux-pr08-exploration/source-resource-de-line-v10/pr08-source-resource-de-line-v10-decision.md` | accepted-forward only | source edge-lift helps slightly but still reads grid-first; stop per-tile edge editing |
| V11 room material breakup | `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/pr08-room-scale-material-breakup-v11-decision.md` | accepted-forward infrastructure + partial visual improvement | explicit room-scale material resource is now approved, but director-grade closure still requires demoting the repeated dark lattice |
| V12 room material authority | `UI/review/dark-uiux-pr08-exploration/room-material-authority-v12/pr08-room-material-authority-v12-decision.md` | accepted-forward partial material-authority boost | later room-compositor draw and stronger alpha improve material presence, but grid-first read remains; stop alpha/draw-order tuning |
| V13 room material resource | `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/pr08-room-material-authority-v13-decision.md` | accepted-forward stronger room material resource | M04 replaces the room-breakup art and improves material presence, but the coarse lattice remains first-read; stop same-key overlay variants |
| V14 ground source inset | `UI/review/dark-uiux-pr08-exploration/ground-source-inset-v14/pr08-ground-source-inset-v14-decision.md` | rejected | source cropping changed pixels but increased seam contrast; source-edge cropping is not the right authority cut |
| V15 ground bleed 4px | `UI/review/dark-uiux-pr08-exploration/ground-bleed-authority-v15/pr08-ground-bleed-authority-v15-runtime-metrics.json` | measured only | reducing overdraw lowered tile noise but did not reduce seam contrast enough to accept as the final cut |
| V16 ground bleed 4px + room alpha 0.78 | `UI/review/dark-uiux-pr08-exploration/ground-bleed-room-material-v16/pr08-ground-bleed-room-material-v16-runtime-metrics.json` | measured only | stronger room-scale material improved vertical seams slightly but left horizontal lattice too close to V13 |
| V17 no ground bleed + room alpha 0.78 | `UI/review/dark-uiux-pr08-exploration/ground-no-bleed-room-material-v17/pr08-ground-no-bleed-room-material-v17-decision.md` | accepted-forward only | zero ground bleed plus stronger room material reduces both vertical and horizontal seam contrast, but map is still not director-grade closure |
| V18 map-stage masonry alpha | `UI/review/dark-uiux-pr08-exploration/map-stage-masonry-v18/pr08-map-stage-masonry-v18-decision.md` | rejected | raising existing stage texture alpha produced only a near-invisible diff; hidden-stage depth needs a stronger structural technique |
| V19 right equipment workbench | `UI/review/dark-uiux-pr08-exploration/runtime-right-equipment-workbench-v19/pr08-right-equipment-workbench-v19-decision.md` | accepted-forward only | wider equipment slot stance and stronger rig backdrop improve right-panel intent, but icon/detail density and map-stage grid still block director-grade closure |
| V20 floor variant flips | `UI/review/dark-uiux-pr08-exploration/runtime-floor-variant-flip-v20/pr08-floor-variant-flip-v20-decision.md` | accepted-forward only | deterministic `flipX` / `flipY` placement for existing floor variant families reduces repeated motif orientation without new resources, but map-stage grid remains first-read blocker |
| V21 hidden aperture masonry | `UI/review/dark-uiux-pr08-exploration/runtime-hidden-aperture-masonry-v21/pr08-hidden-aperture-masonry-v21-decision.md` | accepted-forward only | fog-veil-layer masonry shoulders/shelves make the hidden stage read more like an authored aperture; still not director-grade closure |
| V22 right utility chassis | `UI/review/dark-uiux-pr08-exploration/runtime-right-utility-chassis-v22/pr08-right-utility-chassis-v22-decision.md` | accepted-forward only | wider equipment paper-doll stance and a shared right-column utility chassis improve panel density; still not director-grade closure |
| V23 bottom console cap rails | `UI/review/dark-uiux-pr08-exploration/runtime-bottom-console-cap-rails-v23/pr08-bottom-console-cap-rails-v23-decision.md` | accepted-forward only | shared overlaid top/lower cap rails make the bottom HUD read more like one forged console; still not director-grade closure |
| V24 wall-family relief repaint | `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-decision.md` | accepted-forward only | existing PR-08 wall-family resources retain more authored masonry after atmosphere; still not director-grade closure |

## 2026-05-31 Direction A Addendum

| Step | Artifact | Verdict | Decision impact |
| --- | --- | --- | --- |
| Candidate C canonical route | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-canonical-prototype/ui-demo-new-map-stage-crop.png` | route proved, not closure | A manifest-backed room plate can render in the real runtime, but old decorative passes and default grid still dominated first read |
| D4 decorative suppression | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d4-decorative-suppression/ui-demo-new-map-stage-crop.png` | accepted-forward only | Large legacy room material passes can be disabled under an active plate while keeping markers above the compositor |
| D3 readability pass | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-c-d3-readability-pass/d4-vs-d3-readability-board.png` | accepted-forward only | Seam/edge readability improved, but Candidate C still carried too much square slab rhythm |
| V2 Candidate F promotion | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/pr08-room-art-plate-v2-runtime-decision.md` | candidate-f-promoted | Candidate F became the current `ui.map_stage.ruins.room_plate.pr08_demo` resource because it best reduced baked square rhythm among E/F/G |
| Candidate F final packet | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/pr08-room-art-plate-candidate-f-final-runtime-decision.md` | candidate-f-retained-d3-not-closed | Current resource route and gates are valid, but D3 closure is rejected because map-stage first read still depends on tactical grid/framing and plate placement fit |
| Candidate F placement/framing packet | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/pr08-room-art-plate-candidate-f-placement-framing-decision.md` | candidate-f-placement-framing-pass-d3-not-closed | Black-field aperture cuts and idle grid weight improved, but D3 closure is still rejected because the right combat field reads tactical-board-first |
| Candidate F targeted art edit | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/pr08-room-art-plate-candidate-f-targeted-art-edit-decision.md` | candidate-f-targeted-art-edit-d3-not-closed | Same-key art edit D improves center-right material rhythm and aperture shoulders, but D3 closure is still rejected because active interaction squares remain first-read |
| Candidate F structural overlay grammar | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/pr08-room-art-plate-candidate-f-structural-overlay-grammar-decision.md` | candidate-f-structural-overlay-grammar-d3-not-closed | Idle all-room grid, ground overpaint, explored veil and vfx/telegraph weight improved, but tile-shaped marker and visibility surfaces still blocked closure |
| Candidate F marker-surface grammar | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/pr08-room-art-plate-candidate-f-marker-surface-grammar-decision.md` | candidate-f-marker-surface-grammar-d3-not-closed | Player, loot and active cursor surfaces no longer read as full square cards, but target/range and visibility topology still keep the right combat field grid-shaped |
| Candidate F topology grammar | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/pr08-room-art-plate-candidate-f-topology-grammar-decision.md` | candidate-f-topology-grammar-d3-not-closed | Targeting no longer draws all visible-floor adjacency seams and legal target feedback is target-local, but the default crop hash is unchanged; next move is structural room-plate art edit |
| Candidate F structural art edit K | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01/pr08-room-art-plate-structural-art-edit-decision.md` | candidate-f-structural-art-edit-k-d3-not-closed | Candidate K is now the same-key room plate resource and reduces the center-right square-board read better than I/J, but D3 closure remains rejected |
| Candidate F imagegen material paintover Q | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/pr08-room-art-plate-imagegen-material-paintover-decision.md` | candidate-f-imagegen-material-paintover-q-d3-not-closed | Candidate Q is now the same-key room plate resource; it uses generated material language without copied geometry and improves K, but D3 closure remains rejected |
| Candidate F source-cohesion paintover S | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-s-stronger-surface-cohesion/ui-demo-new-map-stage-crop.png` | candidate-f-source-cohesion-s-d3-not-closed | Candidate S is now the same-key room plate resource and connected explored-fog blankets remove row/column tactical-run splits, but D3 closure remains rejected because the crop still carries visible tactical read-grid weight |
| Candidate F combat-field surface integration U | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-u-combat-field-surface-integration/pr08-room-art-plate-combat-surface-u-decision.md` | candidate-f-combat-surface-u-d3-not-closed | Candidate U is now the same-key room plate resource; S/T/U/V runtime evidence selected U because it best balances right-field lattice demotion and authored stone readability, but D3 closure remains rejected |
| Candidate W authored room source | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/runtime-w-authored-room-source/pr08-room-art-plate-authored-source-w-decision.md` | candidate-w-authored-source-ruins-d3-closed-final-not-closed | Candidate W is now the same-key room plate resource; it accepts D3 map-stage closure for the `tileset.ruins` proof slice only because the crop reads as authored stone room first while markers remain readable |
| PR08 director ruins runtime packet | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/pr08-final-ruins-runtime-packet.md` | candidate-w-ruins-pr08-runtime-packet-final-not-closed | Dedicated `dark-uiux-pr08-director-*` evidence now exists for full, map-stage, right-panel, bottom-deck and telegraph/combat crops; this supersedes PR02-1 drift as the PR08 runtime packet but does not approve all-map closure, packaged whitebox or golden rebaseline |
| PR08 packaged telegraph parity capture | `UI/manual-records/dark-uiux-director-grade-runtime-parity.md` | pr08-packaged-whitebox-telegraph-captured-size-limited | `dark-uiux-pr08-director-grade` now exists in validation registry, presentation catalog, whitebox materialization and yaml parity; packaged app launched, produced real full-window/map/right/bottom crops, reached `prepare-map-telegraph`, wrote `log.validation.phase4_v4.action`, and captured telegraph/combat evidence; exact `1672x941` logical window proof remains display-limited |
| PR08 right-panel command-surface pass | `UI/review/dark-uiux-pr08-exploration/right-panel-command-surface-2026-06-01/pr08-right-panel-command-surface-decision.md` | pr08-right-panel-command-surface-accepted-forward | Right-panel section wells and equipment socket wells improve command-surface material hierarchy after the ruins map proof slice and packaged telegraph path; bottom deck, packaged recapture for this exact crop, non-ruins families and final rebaseline remain open |
| PR08 bottom-deck command-shelf pass | `UI/review/dark-uiux-pr08-exploration/bottom-deck-command-shelf-2026-06-01/pr08-bottom-deck-command-shelf-decision.md` | pr08-bottom-deck-command-shelf-accepted-forward | Bottom HUD child frames are subdued under the shared console shell and hero/action/log seams receive forged lock plates; this improves the bottom command shelf but does not approve packaged recapture, non-ruins families or final rebaseline |
| PR08 bottom-action subject-material pass | `UI/review/dark-uiux-pr08-exploration/bottom-action-subject-material-2026-06-01/pr08-bottom-action-subject-material-decision.md` | pr08-bottom-action-subject-material-accepted-forward | Filled bottom actions now use larger icon subjects, dark material pedestals and warm crown lips so action slots read more like authored equipment subjects; packaged recapture, non-ruins families and final rebaseline remain open |
| PR08 left-nav rail beacon pass | `UI/review/dark-uiux-pr08-exploration/left-nav-rail-beacon-2026-06-02/pr08-left-nav-rail-beacon-decision.md` | pr08-left-nav-rail-beacon-accepted-forward | Left permanent navigation now uses larger icon subjects, per-button dark material sockets and a warm selected-state beacon so the rail reads as a forged command spine; bottom-strip final-read, packaged recapture, non-ruins families and final rebaseline remain open |
| PR08 bottom-strip final-read pass | `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/pr08-bottom-strip-final-read-decision.md` | pr08-bottom-strip-final-read-accepted-forward | Hero gauges now sit in individual forged troughs, the empty action reserve socket is quieter than equipped commands, and long route-hint log rows use note wells plus right braces; packaged recapture, non-ruins families and final rebaseline remain open |
| PR08 right-panel utility bridge pass | `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/pr08-right-panel-utility-bridge-decision.md` | pr08-right-panel-utility-bridge-accepted-forward | The backpack page readout now sits in a forged cradle and paired bridge posts connect it to the operation command dock, so the lower-right utility area reads less like stacked cards; packaged recapture, non-ruins families and final rebaseline remain open |
| PR08 packaged recapture after utility bridge | `UI/manual-records/dark-uiux-director-grade-runtime-parity.md` | pr08-packaged-recapture-after-utility-bridge-size-limited | The packaged app materializes the current shell packet, captures full/map/right/bottom surfaces, reaches `prepare-map-telegraph`, and records a clean telegraph/combat crop after closing validation with `F9`; exact full-width window proof, non-ruins families and final rebaseline remain open |
| PR08 D8 random-seed generalization board | `UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/pr08-random-seed-generalization-decision.md` | pr08-topology-risk-source-alpha-scope-v1-accepted-forward | Eight `tileset.ruins` seed/floor/profession crops prove the authored-room direction remains valuable only with a topology contract; topology contract V1 keeps full-room plate drawing only for plate-safe regions, hybrid/decomposition/AO/source-crop passes built the risky path, dedicated topology source A removed full-room source reuse, B improved distributed wall/light/floor structure, C improved brightness, D superseded C as the current same-key topology source, the bounded source-alpha pass raises dedicated source crop bands from `0.36` to `0.42`, and the scope guard keeps fallback family full-room plate bands at `0.36` |
| PR08 dedicated topology source A | `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02/pr08-topology-source-a-decision.md` | pr08-ruins-dedicated-topology-source-a-v1-accepted-forward | Candidate A is promoted as a formal single-image ruins topology source with canonical/runtime manifest and preload coverage; the mask diagnostic board remains review-only and proves hidden notches are not covered by source bands, but the D8 risky rows are still below plate-safe authored-room quality |
| PR08 dedicated topology source B | `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/pr08-topology-source-b-decision.md` | pr08-ruins-dedicated-topology-source-b-v1-accepted-forward | Candidate B supersedes A on the same formal key; A/B mask diagnostics show stronger room fragments in L-like, corridor-heavy and tall-cross crops, and B D8 recapture passes focused/golden/lint validation, but the risky rows remain accepted-forward only |
| PR08 dedicated topology source C | `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/pr08-topology-source-c-decision.md` | pr08-ruins-dedicated-topology-source-c-v1-accepted-forward | Candidate C supersedes B on the same formal key; B/C mask diagnostics and forced D8 recapture show brighter, more continuous material in corridor-heavy and horizontal topology crop bands, but risky rows remain below plate-safe authored-room quality |
| PR08 dedicated topology source D | `UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-d-decision.md` | pr08-ruins-dedicated-topology-source-d-v1-accepted-forward | Candidate D supersedes C on the same formal key; D is a generated crop-safe topology-source atlas that passes the repeatable mask owner artifact, D8 runtime golden and focused manifest/resource/renderer gates while improving L-like, corridor-heavy and tall-cross material distribution; topology-risk rows remain accepted-forward only |
| PR08 topology source mask owner artifact | `UI/review/dark-uiux-pr08-exploration/topology-source-mask-owner-2026-06-02/pr08-topology-source-mask-owner-decision.md` | pr08-topology-source-mask-owner-artifact-v1-accepted-forward | The mask/crop diagnostic is generated by `GoldenScreenshotHarnessTest` as a repeatable `client` golden owner artifact with repo-relative evidence index; it closed the review-only diagnostic gap and was reused to judge Source D, but it does not by itself close source-quality or procedural Direction A |
| PR08 topology-risk source alpha | `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/pr08-topology-source-alpha-decision.md` | pr08-topology-risk-source-alpha-v1-accepted-forward | The dedicated topology-source crop-band alpha is raised from `0.36` to `0.42` after fail-first renderer coverage, D8 recapture and focused owner gates; it improves risky-row material readability without changing the visual key, runtime source asset, manifest schema, core/game contracts or overlay authority |
| PR08 topology-risk source alpha scope | `UI/review/dark-uiux-pr08-exploration/topology-source-alpha-scope-2026-06-02/pr08-topology-source-alpha-scope-decision.md` | pr08-topology-risk-source-alpha-scope-v1-accepted-forward | `RoomArtPlateSource` now carries the source-band alpha selected by catalog authority: formal dedicated topology sources use `0.42`, while non-ruins fallback room-plate crops stay at `0.36`; this prevents ruins source-alpha from amplifying forest/shadow fallback plate grid rhythm, and the PR08 director recapture is recorded as impact evidence but not rebaselined |
| PR08 topology-risk interior seam dissolve | `UI/review/dark-uiux-pr08-exploration/topology-risk-interior-seam-dissolve-2026-06-02/pr08-topology-risk-interior-seam-dissolve-decision.md` | pr08-topology-risk-interior-seam-dissolve-v1-accepted-forward | `RoomArtPlateRenderer` now draws long-run internal shared-seam fields only inside visible topology-risk hybrid rooms; the fail-first L-shape test proves horizontal and vertical seam coverage while guarding hidden notches, and focused owner/D8 gates pass without changing resources, manifests, schemas or gameplay contracts |
| PR08 D9 real-procgen ROI/freeze packet | `UI/review/dark-uiux-pr08-exploration/d9-real-procgen-roi-2026-06-03/pr08-d9-real-procgen-roi-decision.md` | pr08-d9-real-procgen-roi-hybrid-first-freeze-v1-accepted | D9 now samples 26 playable foundation-route starts through `RoomArtPlateTopologyContract`: 14 `FULL_PLATE_SAFE`, 10 `TOPOLOGY_RISK_LOW_FILL`, 2 `TOPOLOGY_RISK_DISCONNECTED`. Direction A ROI is accepted as `hybrid-first-convergence`, not pure full-room rollout or stop; ruins same-family local polish is frozen unless a new D9/packaged/director artifact proves a fresh blocker |
| PR08 non-ruins topology-source hybrid V1 | `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-2026-06-03/pr08-non-ruins-topology-source-decision.md` | pr08-non-ruins-topology-source-hybrid-v1-accepted-forward | Adds family-owned `forest_edge`, `mine` and `shadow_depths` topology-source keys, PNGs, canonical/runtime manifest entries, preload coverage and renderer proof that topology-risk non-ruins rooms crop dedicated topology sources instead of fallback full-room plates. This is hybrid coverage progress after D9, not packaged parity, all-map closure or golden rebaseline |
| PR08 forest-edge D9 risky runtime crop | `UI/review/dark-uiux-pr08-exploration/forest-edge-d9-risky-runtime-2026-06-03/pr08-forest-edge-d9-risky-runtime-decision.md` | pr08-forest-edge-d9-risky-runtime-topology-source-v1-accepted-forward | Captures actual runtime map-stage evidence for D9 risky seed `2026060908` (`greenwood_fringe`, floor 2, rogue): `tileset.forest_edge`, `TOPOLOGY_RISK_LOW_FILL`, `14x11`, `93` visible cells, `fillPermille=603`, and dedicated source `ui.map_stage.forest_edge.room_topology_source.pr08_demo`. This closes the missing risky forest-edge runtime board, not packaged parity, all-map closure or golden rebaseline |
| PR08 shadow-depths D9 disconnected runtime crop | `UI/review/dark-uiux-pr08-exploration/shadow-depths-d9-disconnected-runtime-2026-06-03/pr08-shadow-depths-d9-disconnected-runtime-decision.md` | pr08-shadow-depths-d9-disconnected-runtime-topology-source-v1-accepted-forward | Captures actual runtime map-stage evidence for D9 disconnected seed `2026060920` (`underground_river`, floor 2, rogue): `tileset.shadow_depths`, `TOPOLOGY_RISK_DISCONNECTED`, `16x12`, `129` visible cells, `fillPermille=671`, `connectedComponents=2`, and dedicated source `ui.map_stage.shadow_depths.room_topology_source.pr08_demo`. This broadens runtime-risk evidence to disconnected topology, not packaged parity, all-map closure or golden rebaseline |
| PR08 topology-risk ground ownership | `UI/review/dark-uiux-pr08-exploration/topology-risk-ground-ownership-2026-06-03/pr08-topology-risk-ground-ownership-decision.md` | pr08-topology-risk-ground-ownership-v1-accepted-forward | Compares the D9 forest-edge low-fill and shadow-depths disconnected runtime boards, then moves topology-risk hybrid ground ownership to the authored source path: ground tiles and floor material are suppressed, wall/runtime layers remain, tileset-aware mantle fields are added, and dedicated topology-source bands render at `0.50`. This improves first-read quality for risky rows, not packaged parity, all-map closure or golden rebaseline |
| PR08 topology-risk interaction grammar | `UI/review/dark-uiux-pr08-exploration/topology-risk-interaction-grammar-2026-06-03/pr08-topology-risk-interaction-grammar-decision.md` | pr08-topology-risk-interaction-grammar-v1-accepted-forward | `RoomCompositorStrategy` now extends restrained art-plate sprite, target, player, loot, fog and cursor grammar to topology-risk hybrid rooms, while legacy interaction fallback stays limited to `LEGACY_TILE_DECORATION`; focused L-shape renderer proof and D9 forest/shadow recaptures pass. This is interaction grammar convergence, not packaged parity, all-map closure or golden rebaseline |
| PR08 non-ruins topology-source V2 | `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v2-2026-06-03/pr08-non-ruins-topology-source-v2-decision.md` | pr08-non-ruins-topology-source-v2-limited-accepted-forward | Replaces `forest_edge`, `mine` and `shadow_depths` topology-source PNGs in place with V2 crop-safe center-material-richness sources, keeps ruins dedicated alpha frozen at `0.50`, and exposes non-ruins dedicated bands at `0.62`; source-level richness improves, but D9 runtime whole-crop brightness remains nearly flat, so next ROI is aperture/framing or runtime scale/visibility pressure, not another source-art pass |
| PR08 topology-risk aperture pressure | `UI/review/dark-uiux-pr08-exploration/topology-risk-aperture-pressure-2026-06-03/pr08-topology-risk-aperture-pressure-decision.md` | pr08-topology-risk-aperture-pressure-v1-accepted-forward | Adds band-scale aperture pressure, worn-stone catchlight lips and a very low-alpha material lift after dedicated topology-source bands in topology-risk hybrid rooms; focused hidden-notch renderer proof and D9 forest/shadow recaptures pass, with crop hashes `f8b075977e4732582f646c8bc4f0a5f21e8870c2a8081dc629db93f5a0717154` and `de64b54c586371093833912cb1c290008cfaeb5fcfa070358ccb6530bf518636`. This is room-scale framing progress, not packaged parity, all-map closure or golden rebaseline |
| PR08 topology-risk wall-run veil | `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-run-veil-2026-06-03/pr08-topology-risk-wall-run-veil-decision.md` | pr08-topology-risk-wall-run-veil-v1-accepted-forward | Dims topology-risk hybrid wall terrain, adds horizontal/vertical run-level wall veils, skips the second PR08 wall-family relief repaint in hybrid rooms, and samples non-critical wall-family component anchors more sparsely. Focused wall-card proof and D9 forest/shadow recaptures pass, with crop hashes `f42006ed133fcdaf3cf8e8604592931a1ae67c2f6cd8595d31a0237f1ecf92a8` and `f3068cdff05cdb24bfd9758dd5b3857043a260cb8a5ecc408b47e2d5677fd6f0`. This reduces repeated wall-card edge noise, but repeated 32px wall texture remains visible, so next ROI is wall-resource variation or stronger wall-mass representation |
| PR08 topology-risk wall-mass slab | `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-mass-slab-2026-06-03/pr08-topology-risk-wall-mass-slab-decision.md` | pr08-topology-risk-wall-mass-slab-v1-accepted-forward | Replaces most topology-risk hybrid runtime wall cards with sparse deterministic anchors and adds heavier horizontal/vertical boundary wall-mass slabs before the lighter wall-run veil layer. Focused sparse-anchor proof and D9 forest/shadow recaptures pass, with crop hashes `5b8d8a22592fd863618f6c5cabbbd0d4946f6eb391ad6911d714531cef7cca3c` and `9b3e8acd80a3174246812707af0162927c641778137c6d1a8031c70582b7733d`. Edge mean drops again, but square topology-source/wall rhythm remains visible, so next ROI is resource-level topology-source or wall-family variation |
| PR08 non-ruins topology-source V3 | `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v3-2026-06-03/pr08-non-ruins-topology-source-v3-decision.md` | pr08-non-ruins-topology-source-v3-limited-forward | Replaces `forest_edge`, `mine` and `shadow_depths` topology-source PNGs in place with stronger broad-flow imagegen/postprocessed atlases. Source-level brightness and material hierarchy improve, but D9 runtime metrics remain flat against wall-mass slab (`forest_edge` edge `1.339 -> 1.359`, `shadow_depths` `2.034 -> 2.034`), so source-only iteration is not the main remaining lever |
| PR08 non-ruins wall-family variation | `UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-variation-2026-06-03/pr08-non-ruins-wall-family-variation-decision.md` | pr08-non-ruins-wall-family-variation-v1-accepted-forward | Adds distinct `base`, `crown`, `side`, `corner` and `door_contact` wall-family resources for `forest_edge`, `mine` and `shadow_depths` through the existing `terrain_wall_family:*` manifest contract. Fail-first resolver proof, D9 forest/shadow recaptures, focused renderer regression and resource/maintainability gates pass; edge metric improves from source-only V3 `1.359 -> 1.156` and `2.034 -> 1.686`, with final crop hashes `f9c13929e8429caa62d2bbd6e80557eeb51d8b8da95b304022517aeb4dc97e3b` and `293c813de4527cfacc1d85e11fbfa4c451d3084281a13d4f7fb72747ab8d4199` |
| PR08 non-ruins wall-family packaged parity | `UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-packaged-parity-2026-06-03/pr08-non-ruins-wall-family-packaged-parity-decision.md` | pr08-non-ruins-wall-family-packaged-parity-v1-rejected-backed-out | Rejects topology-source-only packaged parity for forest/shadow, then rejects and backs out family-ghost recovery because the packaged result reads as chopped source strips, debug floor and auxiliary wall rails. The alpha `0.38` and source-then-ghost probes are also rejected. Next ROI is broader map-stage presentation, not source-only PNG, alpha or ghost-band tuning |

Current actual hashes from the PR08 right-panel utility bridge packet are
recorded in
`UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/evidence-index.tsv`.
The PR08-specific focused golden command passes for this runtime packet. Full
PR08 closure still requires an explicit rebaseline decision and final owner gate
ladder. Packaged whitebox now reaches `prepare-map-telegraph`; after the
right-panel utility bridge pass, packaged evidence has been recaptured for the
current shell packet. The remaining packaged parity limitation is exact
`1672x941` logical window proof on the local display. The updated golden full
parity hash is
`e7f65d0f2f205e238909ec1cd9f54387a9eaf365912e1da5710c9df9e72ed936`; the
selected runtime map-stage hash remains
`84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676`; the updated
right-panel crop hash is
`201645bf13f9824fd95289063b92ea8adc722f9d0f344aca75902a2e99c26c47`; the
bottom-deck crop hash remains
`a416b96d21059212a6a7682f8163f0f065dcac3b545bad28b064814f8120cdfd`.
The packaged recapture hashes are full
`7161d87cb61714b7e66ef348b59b49fdf18bb5ae0790c50572b372f68a582b07`,
map-stage `fe71863a305c5e23e133cd8a3405d9fc65ac8d0aeffd4e118a0274cebdb8feca`,
right-panel `5e36045f1f512041b1dd4495bee40044d0bc35e16fa859855d02f2821c1b353c`,
bottom-deck `50a282bcd4cd7fb648232b8ae4dd7666807c0fcf5de21c7bf99dcb7c80a59b7a`,
and telegraph/combat crop
`8b3b456d8c19ed8c4630a31abd2554e9704ec40fa3bf7fc93be9a969757dde53`.

## Accepted Direction Matrix

| Area | Decision | Owner route | Must not do | Required proof before integration |
| --- | --- | --- | --- | --- |
| Ruins floor | reroll/polish under PR-08 supersession | `ownerPr=PR-08`, supersedes `PR-02-2` key `tileset.ruins.ground_01` | 不从 full-screen comp crop；不烙光照/毒池/selection | PR-08 owner coverage, resource readiness, repeat contact sheet, resolver test |
| Ruins wall | reroll/polish under PR-08 supersession | `ownerPr=PR-08`, supersedes `PR-02-2` key `tileset.ruins.wall_01` | 不做成 floor border；不引入 region schema | PR-08 owner coverage, resource readiness, wall readability crop |
| Room material breakup | accepted-forward under PR-08 | `tileset.ruins.room_breakup_01` as `tile_decal`, drawn once in `MAP_ROOM_COMPOSITOR` | 不做 per-cell seam repair；不承载 visibility/light/telegraph authority | exact manifest test, owner coverage, single room-scale draw test, runtime crop review |
| Actor `actor.vanguard` | defer | keep PR-02-2 unless readability fails | 不因为风格一致性提前重画 | marker readability test after floor/wall/compositor integration |
| Prop `prop.stairs.down` | defer | keep PR-02-2 unless readability fails | 不把方向 marker 或 text 烙进 prop | prop readability test only if rerolled |
| Map darkness/light | compositor first | client layer contract; no darkness/light resource key approved | 不把黑场、光池、telegraph 作为 floor/wall authority | layer-order test and crop review proving markers remain readable |
| Map-stage backdrop | probe existing resource first | PR-02-1 upstream or PR-08 supersession only if proven needed | 不让 backdrop 成为 visibility authority | crop review against PR-08 director map-stage |
| Right panel body/dividers | reusable chrome family | existing PR-02/PR-02-1 chrome first; PR-08 only if gap remains | 不生成 baked labels、numbers、item art | focused crop test and shell key binding test |
| Slot chrome | reusable frame states | existing `ui.frame.slot.*` or PR-08 supersession if insufficient | 不把 item icon 烙进 slot | slot state assertions |
| Bottom deck chrome | unified bottom console | existing `ui.shell.*` first; PR-08 only if gap remains | 不做三块互相割裂 panel；不烙 log text | bottom-deck crop and text stress review |
| UI authority | retained UI route under D10 | official Scene2D Stage / Skin / Table; `MapStageActor` embeds `TileRenderer` | 不采用 VisUI/gdx-skins runtime；不在 first host/Skin slice 引入 KTX；不让 Skin JSON/atlas 成第二 truth | Scene2D host tests, Skin bridge tests, screen adaptation goldens and packaged retained shell evidence |

## Implementation Order

Do not start from runtime visual tweaking. The next implementation pass should follow this order:

0. D10 retained UI authority packet
   - promote `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md`
   - execute `D10-P0 ~ D10-P8` phase gates in order
   - establish official Scene2D host / Skin bridge with no KTX dependency in the kernel phase
   - hard-cut migrated surfaces instead of preserving old immediate UI compatibility routes
   - adapt main menu, shell, inventory/detail, shop, talent tree, combat/frontstage, bottom deck and nav rail through retained UI plans
1. Owner/contract wiring
   - add explicit PR-08 owner coverage for any superseded floor/wall keys
   - record PR-02-2 rollback path before migration
   - keep actor/prop rows deferred unless readability fails
2. Final resource candidate production
   - produce final floor/wall runtime candidate assets from the accepted family direction
   - produce contact sheets and hashes
   - keep text, light, poison, selection, telegraph out of the tile resources
3. Resource readiness
   - generate `build/reports/verification/dark-uiux/director-grade-asset-readiness.json`
   - run `darkSpriteSheetLint`, `spriteSheetMapLint`, `assetLint`, `styleLint`, `manifestLint`, `resourcePipelineLint` as applicable
4. Runtime compositor reset
   - reduce renderer to `base stage art -> semantic tile/floor/wall -> deterministic visibility fog -> gameplay overlays/actors/loot/telegraph -> focus/selection -> UI chrome`
   - add focused layer-order and marker-readability tests before any golden update
5. Director evidence close
   - create `dark-uiux-pr08-director-*` golden set
   - write `UI/manual-records/dark-uiux-director-grade-runtime-parity.md`
   - run packaged whitebox and `verifyChanged`

## Non-Goals For The Next Slice

1. No gameplay, save, replay, profile, schema or manifest schema change.
2. No full-screen map art replacing semantic tile rendering.
3. No second visibility authority in resources.
4. No second resource mapping table in renderer or tests.
5. No golden hash update to hide director rubric failure.
6. No UI text baked into generated images.
7. No PR-07 close artifact rewrite.

## Open Risks

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| Floor sheet border rhythm | family direction is clean but repeated floor can still look grid-heavy | final asset polish plus map-stage crop review before golden update |
| Wall over-busyness | wall material can compete with actor/telegraph at 32px | marker readability crop and focused render test |
| Existing chrome may be insufficient | right/bottom panels are independent from map overlay and remained unchanged in subtractive spike | probe existing PR-02/PR-02-1 chrome first, reroll only with evidence |
| Owner drift | first-screen keys currently belong to PR-02-2 | PR-08 supersession rows and rollback path before resource mutation |
| Tool false confidence | lint can pass while visual direction is wrong | keep manual record, contact sheet QA and director crop review as blocking artifacts |

## Validation Already Run

Recorded in `UI/review/dark-uiux-director-grade-gap-audit.md`:

1. Subtractive spike golden command ran with `KTOME_DARK_UIUX_PR08_SUBTRACTIVE_SPIKE=1`; it failed as expected after writing exploratory PNGs because hashes changed.
2. Canonical PR-02-1 golden command reran without spike flag and passed, restoring canonical build evidence.
3. Temporary diagnostic instrumentation was removed after collecting overlay counts.

Recorded in the current direction pass:

1. Target/family PNG dimensions were checked locally.
2. `git diff --check` passed for the edited Markdown/prompt files.
3. UI docs and targets were scanned for local absolute path patterns; no matches were found.

No Gradle task was rerun after the family-pack documentation update because this pass only added design evidence and Markdown records. Runtime/resource integration will require the gates listed above.

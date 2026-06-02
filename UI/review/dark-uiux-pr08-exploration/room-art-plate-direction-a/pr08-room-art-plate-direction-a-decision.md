# PR-08 Direction A Unified lky Decision Packet

> Date: 2026-05-31
> Status: `approved-by-lky-for-runtime-prototype`
> Scope: PR-08 map-stage blocker, Direction A pre-rendered room art plate

This packet records lky's approval of Candidate C and the conditional Direction
A runtime prototype route. It does not approve production assets, manifest
changes, golden rebaseline or final PR-08 closure by itself.

Approved decision:

```text
Approve Candidate C and the conditional Direction A route: client-only room art
plate prototype first, PR-08 owner/manifest route only after runtime evidence,
large decorative pass deletion after marker readability passes, no surface pivot
and no golden rebaseline until final runtime packet.
```

## 1. Direct Recommendation

Approve Direction A as the active map path, select Candidate C as the first
runtime prototype target, and pre-approve the conditional implementation route
below:

1. Use Candidate C, `Broken slab hall`, as the first room art plate prototype.
2. Build a client-only `RoomPresentationPlan` / `RoomArtPlateRenderer` path.
3. Derive clipping from existing visible material topology, not a second map
   truth.
4. Make the static grid nearly invisible; keep hover, selection, targeting,
   pathing and range as runtime overlays above the plate.
5. Promote the selected plate only through a PR-08 owner route and existing
   manifest schema. Do not load committed runtime code from `UI/review/...` or
   generated-images directories.
6. After the prototype is visible and marker readability passes, approve
   deleting or disabling the decorative `TileRenderer` pass families that the
   plate replaces.
7. Do not pivot to right panel, bottom HUD, inventory or talent work until this
   map plate probe is accepted or explicitly rejected.

Final map closure and golden rebaseline still require a later runtime evidence
packet. They cannot be accepted from generated review art alone.

## 2. Decision Checklist For lky

Recommended choices:

| Decision | Recommended lky decision | Why |
| --- | --- | --- |
| D0 candidate | Select Candidate C | Best balance of authored room, readable center, moderate warm light and irregular slab field |
| D0 alternate | Keep Candidate A as alternate | Strongest wall mass, but busier rubble and darker edges may compete with markers |
| D0 reject | Reject Candidate B for default route | Good mood, but too symmetric and torch-heavy; likely to fight runtime marker contrast |
| D0 fallback | Keep Candidate D only as technical fallback | Useful for renderer architecture, but it does not deliver authored-room quality |
| D1 runtime prototype | Approve, conditional on C | Client-only prototype is the shortest path to prove whether room plates solve the grid-first blocker |
| D2 production route | Conditional approval only | Production promotion waits for runtime evidence and gates; route should use existing schema |
| D3 map closure | Do not accept yet | Needs runtime full screenshot, map crop and marker/telegraph crop |
| D4 large pass removal | Pre-approve after prototype passes | The plate must replace old decorative geometry, not stack below it |
| D5 surface priority | Stay on map | V48 made hero/bottom better, but map remains the first-read blocker |
| D6 fallback route | Direction B only if C/prototype fails | Tactical-grid fallback is a stop-loss route, not the selected aesthetic |
| D7 multi-map rollout | Approve ruins-first, tileset-agnostic architecture | The first proof should cover `tileset.ruins`, but the renderer contract must support other tileset families through catalog/fallback |

If lky wants a one-line approval, use:

```text
Approve Candidate C and the conditional Direction A route: client-only room art
plate prototype first, PR-08 owner/manifest route only after runtime evidence,
large decorative pass deletion after marker readability passes, no surface pivot
and no golden rebaseline until final runtime packet.
```

## 3. Candidate Evidence

Generated artifacts:

| Artifact | Path |
| --- | --- |
| Prompt set | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/prompts/pr08-room-art-plate-direction-a-prompt-set.md` |
| Decision board | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-board.png` |
| Marker proxy board | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-marker-proxy-board.png` |
| Candidate A | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-a-authored-ruins-chamber.png` |
| Candidate B | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-b-torch-cut-stone-arena.png` |
| Candidate C | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-c-broken-slab-hall.png` |
| Candidate D | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-d-material-only-technical-fallback.png` |

Candidate review:

| Candidate | Verdict | Notes |
| --- | --- | --- |
| A: Authored ruins chamber | viable alternate | Strong wall mass and Diablo-like room edge. Risk: dense rubble and high wall detail may make clipping and marker readability harder. |
| B: Torch-cut stone arena | reject for default route | Strong atmosphere, but too symmetrical and too torch-dominant. It risks making the runtime telegraph/readability fight baked fire pools. |
| C: Broken slab hall | recommended | Most useful central play field, strong but not excessive enclosure, irregular slabs without a hard grid. Marker proxy remains readable. |
| D: Material-only technical fallback | fallback only | Good texture sample for renderer architecture, but it abandons the authored-room thesis. |

Artifact integrity:

| Artifact | Dimensions | SHA-256 |
| --- | --- | --- |
| Candidate A | 1448x1086 | `1a5c939d541e23c2348ac258dbba5ebed95c952d877fbe0fc31b69e211711ea2` |
| Candidate B | 1448x1086 | `e77154a08a6b10e05f190289f7b009f2155d671419a858c2cb89c0c16e46bef2` |
| Candidate C | 1448x1086 | `557bdd513a5004993140339153f35071f11a63710b5d2c30297e445c23c1bae3` |
| Candidate D | 1448x1086 | `777f869cb61044a4e1619087824fe5e28ce07dbb015a7d389d0600ef5891fce6` |
| Decision board | 1452x1301 | `0af7a4c0e0b8146dea9612d1a4fc08bfa1267fc20afd8016eb9543d229cbbd2c` |
| Marker proxy board | 1174x1042 | `233fcee88c62c7b82406b781c52dc2640b42a96a1b29ae83309c9212456ade27` |
| Prompt set | n/a | `fcad7f27e4217a67cdd9114631ad8e73eaf98ebada11c195f62485018166773f` |

## 4. Evidence From Existing PR-08 Work

The existing PR-08 evidence says the map blocker is structural, not a missing
small alpha tweak:

1. `UI/review/dark-uiux-director-grade-gap-audit.md` selects
   `resource-gap` as primary root cause.
2. The subtractive spike improved clarity but exposed repeated floor/wall tiles,
   hard grid structure and weak authored darkness.
3. Diagnostic evidence recorded `overlayFunctionCount=22`,
   `warmOverlaySubpassCount=20`, `materialRectCountPerVisibleCell=16.375` and
   `totalRectDraws=3784`; continuing rectangle/pass tuning is the wrong default.
4. V10-V48 records are mostly `accepted-forward only`; V48 improves hero crest
   quality but leaves the map-stage room-structure gap as the largest first-read
   blocker.
5. `UI/design` is useful for later shell/right/bottom decisions: it provides
   variant A/B/C shell sizing, state matrices, slot sizing and token roles. It
   does not replace the room-plate map decision and should not be copied into
   runtime as React/CSS truth.

## 5. D1 Runtime Prototype Recommendation

Recommended architecture:

1. Add `RoomPresentationPlan` or equivalent client-only model derived from
   viewport, visible room bounds and deterministic room signature.
2. Add `RoomArtPlateRenderer` rather than adding another private pass cluster to
   `TileRenderer`.
3. Use existing runtime visible material topology:
   - `TileRenderModel.mapCellMaterials`
   - `visibleMaterialPoints(frame)`
   - `visibleRoomClip(frame)`
4. Place the plate before actors, loot, telegraphs, cursor, selection and combat
   feedback.
5. Keep `core` and `game` untouched.

Reasoning:

1. The plate is presentation, not terrain/visibility truth.
2. Existing topology helpers already define what is visible in the room; using
   them avoids second-authority risk.
3. A separate renderer gives a deletion target for the old decorative pass
   families instead of growing `TileRenderer` further.

## 6. D2 Production Resource Route Recommendation

Recommended production route after runtime proof:

| Field | Recommendation |
| --- | --- |
| tentative key | `ui.map_stage.ruins.room_plate.pr08_demo` |
| owner | `PR-08` |
| category | `ui_frame` unless the pipeline gains an approved room-plate category in a separate contract |
| footprint | `ui` |
| raw path family | `dark-v1/ui/` or another approved PR-08 presentation-resource path |
| source record | preserve original review candidate and cleanup notes under the Direction A review directory |
| runtime manifest | generated/synced from canonical manifest, not hand-maintained |
| consumer | `RoomArtPlateRenderer` / map-stage presentation layer |
| consumer test | manifest exact-resolution test plus focused renderer layer-order test |

Do not:

1. pretend the full-room plate is `tile_ground`;
2. replace `tileset.ruins.room_breakup_01` with a full-room plate;
3. add a new manifest schema, atlas/region schema or runtime resource table;
4. load committed runtime code from `UI/review/...`, local generated image
   folders or machine-absolute paths.

Evidence:

1. The current asset pipeline allowlist supports `ui_frame`, `tile_ground`,
   `tile_wall`, `tile_decal`, `vfx_plate` and related existing categories, but
   no existing `room_plate` category.
2. `VisualManifestEntry` already carries `key`, `category`, `rawOutputPath`,
   `footprint`, `pivotX`, `pivotY`, `tags` and optional `tintColorHex`; no schema
   change is needed for a presentation image.
3. Existing PR-08 owner wiring already covers floor/wall/room-breakup rows; the
   room plate should be a separate presentation key, not another tile-family
   alias.

## 7. D4 Decorative Pass Removal Recommendation

Pre-approve deletion/disable after the selected plate is visible and marker
readability passes.

Likely replaced by plate:

1. room material painting and floor unification;
2. wall mass simulation and raised wall relief;
3. dark corner/silhouette pressure;
4. story decals, slab fields and painterly breakup;
5. broad decorative dark veil and hidden-stage grid suppression;
6. default always-visible grid presentation.

Initial audit candidates:

1. `drawVisibleRoomFoundationGlaze`
2. `drawVisibleRoomFloorUnifier`
3. `drawVisibleRoomAtmosphere`
4. `drawVisibleRoomApertureHierarchy`
5. `drawHiddenStageGridSuppression`
6. `drawVisibleWallRelief`
7. `drawVisibleWallMassBands`
8. `drawVisibleWallRaisedFaces`
9. `drawVisibleWallCrownBlocks`
10. `drawVisibleWallMasonryCourses`
11. `drawVisibleWallFootRubble`
12. `drawPr08WallFamilyReliefRepaint`
13. `drawVisibleRoomCornerBreakup`
14. `drawVisibleRoomSilhouettePressure`
15. `drawVisibleRoomBoundaryCompression`
16. `drawVisibleRoomAsymmetricEdgeMass`
17. `drawVisibleRoomMacroStructuralPlates`
18. `drawVisibleRoomRuntimeCornerApertureShelves`
19. `drawVisibleRoomOuterShadows`
20. `drawVisibleRoomContactShadows`
21. `drawVisibleRoomStoryDecals`
22. `drawVisibleRoomMaterialBreakupAsset`
23. `drawVisibleRoomSlabVariation`
24. `drawVisibleRoomGridDissolve`
25. `drawVisibleRoomStaggeredStoneRhythm`
26. `drawVisibleRoomCrossCellSlabFields`
27. `drawVisibleRoomScaleMaterialFields`
28. `drawVisibleRoomLocalizedStoneDamage`
29. `drawVisibleRoomSilhouetteBreakup`
30. `drawVisibleRoomPainterlyBreakup`
31. `drawVisibleRoomTacticalClarityPlane`

Likely retained:

1. terrain base draw for semantic cells;
2. non-floor hazard/material semantics;
3. deterministic visibility/fog truth;
4. actors, props, loot, telegraphs and combat feedback;
5. cursor, selection, targeting, path/range overlays;
6. small grounding shadows or light passes only where they improve marker
   readability.

## 8. Surface Priority Recommendation

Stay on the map-stage blocker until one of these happens:

1. lky rejects the room-plate batch;
2. Candidate C runtime prototype fails marker readability or looks worse than
   current V48;
3. a second candidate batch also fails;
4. lky explicitly chooses the tactical-grid fallback or a non-map surface.

Reasoning:

1. V48 and `UI/design` give useful right/bottom/shell direction, but the map is
   still the first-read blocker.
2. Pivoting before D0/D1 would repeat the previous whack-a-mole pattern.
3. If the room plate route fails, `UI/design` Variant C can guide a later
   wide-map tactical fallback, while Variant B can guide inscription-priority
   right-panel work.

## 9. D7 Multi-map Rollout Recommendation

Approve a ruins-first rollout, not a one-off map hack and not an all-map art
rewrite inside this slice.

Current content evidence:

1. `game/src/main/resources/data/zones/index.yaml` currently defines 11 zones
   across 4 tileset families.
2. `tileset.ruins` is used by `shattered_outpost` and `elven_ruins`.
3. `tileset.forest_edge` is used by `greenwood_fringe` and `bandit_camp`.
4. `tileset.mine` is used by `deep_iron_pit` and `molten_core`.
5. `tileset.shadow_depths` is used by `grey_gate_depths`,
   `underground_river`, `crystal_cavern`, `abyssal_temple` and
   `abyssal_heart`.
6. The current visual manifest has a richer PR-08 `tileset.ruins` family:
   floor variants, wall pieces and `room_breakup_01`. The other tileset
   families currently have simpler ground/wall pairs.

Recommended boundary:

1. The first runtime prototype and Candidate C production route cover
   `tileset.ruins` only.
2. The renderer/model architecture must be tileset-agnostic: use a
   `RoomArtPlateCatalog` or equivalent selector keyed by `tilesetKey`, optional
   biome/theme tags and deterministic room signature.
3. Non-ruins families must fall back to the existing tile renderer until they
   have their own accepted plate family.
4. Do not apply the ruins plate to `tileset.forest_edge`, `tileset.mine` or
   `tileset.shadow_depths`.
5. Do not declare all-map visual closure from a ruins-only screenshot. If PR-08
   claims global map quality, it needs at least one evidence crop per tileset
   family or an explicit follow-up gap record.

Suggested follow-up order:

1. `tileset.ruins`: current Direction A proof and first runtime prototype.
2. `tileset.forest_edge`: forest edge / trail / ambush authored plate family.
3. `tileset.mine`: forge / slag / ore-cart authored plate family.
4. `tileset.shadow_depths`: split or vary depths, cavern and temple readings
   before treating the family as complete.

## 10. Final Closure Rubric

Do not accept final closure until runtime evidence proves:

1. full screenshot and map crop read as an authored room, not a grid-first board;
2. actor, enemy, loot, telegraph, cursor, selection and path/range overlays are
   clearer than the baked environment;
3. static grid is quiet or absent, while interactive states remain visible;
4. replaced decorative pass families are deleted or disabled in the room-plate
   path;
5. production resource ownership is traceable through PR-08 owner route,
   canonical manifest, runtime manifest sync and resource gates;
6. PR-08-specific director labels are used for golden evidence;
7. packaged whitebox and `verifyChanged` are run or real blockers are recorded.
8. ruins-first closure is not mislabeled as all-map closure; other tileset
   families either have evidence or explicit follow-up gap records.

## 11. Validation Performed For This Packet

Performed:

1. Read PR-08 plan, goal, log, PR doc, gap audit, evidence brief and `UI/design`
   files.
2. Read renderer/model/manifest entry points relevant to room plate routing:
   `TileRenderer.kt`, `TileRenderModel.kt`, `DarkUiMapVisualKeys.kt`,
   `ManifestModels.kt`, `ManifestResolvers.kt`, `key-registry.yaml`,
   `pr08-owner-keys.yaml`, canonical/runtime visual manifests and asset
   pipeline category allowlist.
3. Generated 4 review-only room plate candidates using built-in image
   generation.
4. Built a decision board and synthetic marker-readability proxy board.
5. Recorded image dimensions and SHA-256 hashes.
6. Verified this packet, the prompt set, goal/log and Direction A plan have no
   machine-absolute paths, no unresolved placeholder markers and no trailing
   whitespace.
7. Ran `git diff --check` on the touched goal/log/plan/packet files with no
   reported whitespace errors.
8. Read zone, tileset, manifest and renderer evidence for the multi-map
   rollout boundary.

Not performed:

1. No Kotlin implementation changes.
2. No runtime prototype.
3. No manifest, sheet-plan, resource, golden or packaged whitebox mutation.
4. No Gradle task was run for this packet because it is review-only
   documentation and generated evidence.

## 12. Current Status

`approved-by-lky-for-runtime-prototype`

The next implementation step is the client-only runtime prototype:

1. use Candidate C as the review-selected prototype source;
2. build the `tileset.ruins` runtime proof behind a tileset-agnostic
   catalog/fallback boundary;
3. keep PR-08 owner/manifest promotion blocked until runtime evidence exists;
4. keep large decorative pass deletion blocked until marker readability passes;
5. keep golden rebaseline and final closure blocked until the final runtime
   packet is accepted.

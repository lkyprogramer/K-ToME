# PR08 Non-Ruins Topology Source Decision

Date: 2026-06-03

## Verdict

`pr08-non-ruins-topology-source-hybrid-v1-accepted-forward`

This packet materializes family-owned topology-source assets for the current
non-ruins room-art families:

```text
ui.map_stage.forest_edge.room_topology_source.pr08_demo
ui.map_stage.mine.room_topology_source.pr08_demo
ui.map_stage.shadow_depths.room_topology_source.pr08_demo
```

The change converts non-ruins topology-risk rooms from fallback full-room plate
source cropping to dedicated family topology-source cropping. This follows the
D9 `hybrid-first-convergence` decision: full-room plates stay for
`FULL_PLATE_SAFE` rooms, while `TOPOLOGY_RISK_*` rooms use a topology-aware
source atlas and the existing hybrid composition grammar.

## Evidence

Review packet:

```text
UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-2026-06-03/
```

Visual board:

```text
UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-2026-06-03/non-ruins-topology-source-board.png
```

Evidence index:

```text
UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-2026-06-03/evidence-index.tsv
```

Accepted runtime resources:

```text
client/src/main/resources/dark-v1/ui/ui_map_stage_forest_edge_room_topology_source_pr08_demo.png
client/src/main/resources/dark-v1/ui/ui_map_stage_mine_room_topology_source_pr08_demo.png
client/src/main/resources/dark-v1/ui/ui_map_stage_shadow_depths_room_topology_source_pr08_demo.png
```

Artifact hashes:

```text
ui_map_stage_forest_edge_room_topology_source_pr08_demo.png 37b89f7cc36bb0b6ce380bda82b3fc39d9fc6c64df8d91f5c270f1ebeec8728c
ui_map_stage_mine_room_topology_source_pr08_demo.png edba8f215bf4009c90325d33d89e4318b0626bb50c637e17e616766b2686d7d0
ui_map_stage_shadow_depths_room_topology_source_pr08_demo.png 345ad931b002858ccb06dddc240b177a848565fb8ac393aabfc805f6a4e799a4
non-ruins-topology-source-board.png c28083299bfc61e59fd68f885ce52ca9e4436efad48e6d37c828f27e1e0f4d0d
```

## Root Cause Class

`resource ownership / topology-source coverage`

D9 proved mixed topology: 14 safe rows and 12 topology-risk rows across playable
foundation-route samples. Before this packet, non-ruins topology-risk rooms
could still source-crop the family full-room plate, so L-shaped or low-fill
regions could inherit composition from a complete authored room perimeter.

## Why Not Micro-Tuning

Alpha, fog, seam, line-weight or rectangle tweaks cannot solve this blocker
because the weak point is source ownership: topology-risk bands need a
crop-safe family source, not a different opacity on the full-room plate. This
packet changes the resource route and manifest-backed visual key for the risky
path while leaving the existing hybrid grammar intact.

## Contract

1. The owner remains `client`.
2. `core`, `game`, save, replay, profile, combat, pathing, content pack and
   schema contracts are unchanged.
3. Runtime manifest remains generated from the canonical visual manifest via
   `syncPhase2Manifests`.
4. The renderer continues to use `RoomArtPlateTopologyContract` as the single
   topology decision authority.
5. This packet is accepted-forward for non-ruins topology-source coverage only.
   It does not claim all-map closure, packaged parity, director golden
   rebaseline or final PR08 quality closure.

## Validation

Fail-first command run before implementation:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --no-configuration-cache
```

Result:

```text
FAILED: expected 2 dedicated topology-source bands for forest-edge, observed 0.
```

Post-change commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew syncPhase2Manifests --no-configuration-cache
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --no-configuration-cache
./gradlew resourcePipelineLint maintainabilityLint --no-configuration-cache
```

Result:

```text
BUILD SUCCESSFUL in 1s
BUILD SUCCESSFUL in 15s
BUILD SUCCESSFUL in 8s
```

## Rollback

Revert the three topology-source keys from `DarkUiMapVisualKeys`, the canonical
manifest, the generated runtime resources, the PR08 room-art source spec and
the focused test/preload assertions; then rerun `syncPhase2Manifests` so the
runtime manifest drops the keys.

## Next Action

Capture a runtime/topology-risk board for at least one D9 risky `forest_edge`
seed and compare full-room fallback versus dedicated topology source in the
actual map-stage crop before any golden rebaseline or all-map closure claim.

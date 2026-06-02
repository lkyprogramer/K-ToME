# PR-08 Candidate F Placement/Framing Runtime Packet Decision

> Date: 2026-05-31
> Status: `candidate-f-placement-framing-pass-d3-not-closed`
> Scope: PR-08 Direction A `tileset.ruins` room art plate runtime composition after Candidate F final packet

## Direct Decision

Do not accept D3 map-stage closure yet.

The bounded placement/framing pass is accepted as forward progress: it adds a
client-only post-fog aperture shoulder for hidden/explored cells adjacent to
the visible room and lowers idle room-grid hints below interaction weight. The
pass improves the flat black-field hard cut without touching shell chrome,
resource authority, non-ruins tilesets, gameplay rules, save/replay contracts
or manifest schema.

The pass does not close D3. The map crop still reads too much as a tactical
board in the right combat field, and the authored-room first read is still
weaker than the target reference.

## Root Cause Class

`placement/framing + default grid grammar`

Candidate F remains the correct current resource foundation. The remaining
blocker is no longer broad resource-family quality; it is how the runtime
composes the plate, black field and always-visible tactical grammar.

## Implemented Runtime Changes

1. In the art-plate path only, hidden/explored cells next to visible room
   materials now receive a low-alpha post-fog aperture shoulder. This softens
   internal black-field hard cuts without revealing hidden terrain or applying
   the ruins plate to other tilesets.
2. Idle art-plate grid hints now draw below interaction weight. Targeting /
   cursor interaction keeps the stronger readability budget.
3. Focused white-box tests now cover internal hidden-void aperture shoulders
   and idle grid-hint weight.

## Changed Files

1. `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
2. `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
3. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/*`
4. PR-08 goal / plan / log / evidence documents

## Evidence

| Evidence | Path |
| --- | --- |
| Golden status | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/golden-status.txt` |
| Evidence index | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/evidence-index.tsv` |
| Full 1672x941 parity | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-parity-1672x941.png` |
| Full 1280x800 parity | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-parity-1280x800.png` |
| Map-stage crop | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-map-stage-crop.png` |
| Right panel crop | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-right-panel-grid.png` |
| Bottom deck crop | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-bottom-deck-no-command-hints.png` |
| Nav rail crop | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-nav-rail-crop.png` |
| Inventory page 1 | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-inventory-page-1.png` |
| Inventory page 2 | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-placement-framing-pass/ui-demo-new-inventory-page-2.png` |

## Golden Actual Hashes

These are the actual hashes reported by the focused golden command. They are
not golden baseline updates.

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `cb1a627f7144e58843543f20c7d3b67f8e872fde647615b90ad7dfcb2147a113` |
| `ui-demo-new-parity-1280x800` | `84a94aff469104a3f8413b8168780714e0c1ce5e20a7b908622b17fa7d1bfc48` |
| `ui-demo-new-right-panel-grid` | `36e9b06dc1aba326458cd9c907e02062130d7fb5321007f13483fd18e96ccfcc` |
| `ui-demo-new-bottom-deck-no-command-hints` | `6bf2380371c23586c38737409e70fb487c903b82b89ed989fc80c0f8e837a142` |
| `ui-demo-new-nav-rail-crop` | `c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6` |
| `ui-demo-new-map-stage-crop` | `a83d37d11137a52e6ec6bfe2ad124cea9a9234611635f702ff66032551a5770c` |
| `ui-demo-new-inventory-page-1` | `021a21d9494e20ad4374d4209e2e66d38b7e0280d55f1845ca01b33377c039cf` |
| `ui-demo-new-inventory-page-2` | `3f93fa09695715e8ab612fbd28d62675e82f1a8a6b8dc0e1ba8430ce8a3a7a8f` |

## Commands Run

RED then GREEN:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasAddsPr08ApertureShoulderForHiddenVoidsInsideRoomPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08IdleGridHintsBelowInteractionWeight
```

PASS:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test \
  --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasAddsPr08ApertureShoulderForHiddenVoidsInsideRoomPlate \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08IdleGridHintsBelowInteractionWeight \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback
```

PASS:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew maintainabilityLint
```

EXPECTED FAIL:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

## Director Read

Accepted forward:

1. The black-field hard cut is less flat than the previous Candidate F final
   packet.
2. Idle grid hints are lower and no longer use the same alpha budget as
   interaction mode.
3. The change stays in `client/render` and remains art-plate specific.

Not accepted for D3 closure:

1. The right combat field still reads as a square tactical board before it
   reads as authored stone.
2. The room is still visually constrained by a mostly AABB placement model.
3. The target reference still has stronger room mass, darker authored aperture
   and less default grid grammar.

## Rejected Alternatives

1. Do not move this into `DemoShellRenderer`; that would globalize a ruins
   art-plate concern into shell chrome.
2. Do not copy legacy `renderWarmMapOverlay()` passes into the art-plate path.
3. Do not rebaseline PR-02-1 golden hashes from this packet.
4. Do not apply the ruins room plate to non-ruins tilesets.

## Next Action

Use a targeted Candidate F art edit focused on the center-right combat field
and outer shadow shoulders. Keep the same resource route and packet loop; do
not open a broad new resource family unless the targeted edit fails.

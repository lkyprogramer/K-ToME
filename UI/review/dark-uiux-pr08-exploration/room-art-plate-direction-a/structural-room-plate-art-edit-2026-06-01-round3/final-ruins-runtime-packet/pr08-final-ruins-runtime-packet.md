# PR08 Final Ruins Runtime Packet

Date: 2026-06-01

## Scope

This packet promotes the Candidate W `tileset.ruins` proof slice into a
dedicated PR08 director runtime evidence set.

It does not approve all-map closure, non-ruins room families, packaged whitebox
closure, or PR08 golden rebaseline.

## Evidence

| Label | SHA-256 | Artifact |
| --- | --- | --- |
| `dark-uiux-pr08-director-parity-1672x941` | `dff87e79ca72445c3d77e1509799569c44daaa46d70b86c2f52cf806addc2180` | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-parity-1672x941.png` |
| `dark-uiux-pr08-director-map-stage-crop` | `84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676` | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-map-stage-crop.png` |
| `dark-uiux-pr08-director-right-panel-crop` | `36e9b06dc1aba326458cd9c907e02062130d7fb5321007f13483fd18e96ccfcc` | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-right-panel-crop.png` |
| `dark-uiux-pr08-director-bottom-deck-crop` | `6bf2380371c23586c38737409e70fb487c903b82b89ed989fc80c0f8e837a142` | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-bottom-deck-crop.png` |
| `dark-uiux-pr08-director-telegraph-combat-crop` | `b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321` | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/dark-uiux-pr08-director-telegraph-combat-crop.png` |

Canonical generated index:

- `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round3/final-ruins-runtime-packet/evidence-index.tsv`

## Validation

Ran:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

Result:

- PASS: PR08 director runtime evidence test writes all five `dark-uiux-pr08-director-*` artifacts and the generated index.
- PASS: `maintainabilityLint`.
- Manual visual check: full, map-stage and telegraph/combat crops are nonblank; telegraph/combat crop is a real gameplay telegraph capture, not the runtime error screen.

## Decision

Candidate W remains accepted for `tileset.ruins` D3 map-stage proof-slice
closure. The new PR08 evidence seam is accepted as the reproducible runtime
packet for this proof slice.

Final PR08 closure remains blocked on explicit golden/rebaseline decision,
packaged whitebox evidence, and non-ruins room-family coverage or documented
scope deferral.

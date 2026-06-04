# PR08 Forest Edge D9 Risky Runtime Decision

## Verdict

`pr08-forest-edge-d9-risky-runtime-topology-source-v1-accepted-forward`

## Direct Conclusion

The D9 risky `tileset.forest_edge` runtime crop gap is closed for one real playable seed:

- seed: `2026060908`
- zone: `greenwood_fringe`
- floor: `2`
- profession: `rogue`
- topology decision: `TOPOLOGY_RISK_LOW_FILL`
- visible topology: `14x11`, `93` visible cells, `fillPermille=603`, `aspectPermille=1272`, `connectedComponents=1`
- dedicated topology source: `ui.map_stage.forest_edge.room_topology_source.pr08_demo`

This proves the accepted non-ruins topology-source routing is visible in an actual runtime map-stage crop for a D9 risky `forest_edge` sample. It does not approve all-map closure, packaged parity, final director-grade quality, or golden rebaseline.

## Evidence

| Artifact | Path | Hash |
| --- | --- | --- |
| Runtime map-stage crop | `UI/review/dark-uiux-pr08-exploration/forest-edge-d9-risky-runtime-2026-06-03/dark-uiux-pr08-forest-edge-d9-risky-runtime-crop.png` | `03e2a3ae1df11224145bde645a573fadea80bfe7b26b92a94e410d25f6bfa5eb` |
| Director review board | `UI/review/dark-uiux-pr08-exploration/forest-edge-d9-risky-runtime-2026-06-03/dark-uiux-pr08-forest-edge-d9-risky-runtime-board.png` | `bf3e88f52d2bee0cecc621af2f5d6c360b87b839f4c430af048dc7087ab4df0f` |
| Evidence index | `UI/review/dark-uiux-pr08-exploration/forest-edge-d9-risky-runtime-2026-06-03/evidence-index.tsv` | N/A |

Source hashes recorded in the evidence index:

- topology source PNG: `37b89f7cc36bb0b6ce380bda82b3fc39d9fc6c64df8d91f5c270f1ebeec8728c`
- room plate PNG: `34f0beadfe3ae8c2a54ac3fb14a1dcf12b744c0df635b237e141eda5106d20a7`

## Validation

Ran:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache
```

Result: `BUILD SUCCESSFUL in 10s`.

The validation also exposed that the old LWJGL golden harness could hang in macOS hidden-window event loop / native cleanup after the capture was already complete. The harness now uses a finite single-shot update loop for golden captures and avoids the native cleanup paths that block this owner task locally.

## Non-Goals

- No PR08 director hash rebaseline.
- No packaged app parity claim.
- No all-map or all-tileset closure.
- No return to ruins alpha, fog, line-weight, single-cell rectangle, seam, or same-key source polishing.

## Next Action

Use this board as the first runtime D9 risky non-ruins comparison point for the director-grade loop. The next packet should either extend runtime-risk evidence to another high-value D9 family/shape or move to packaged/director review only if it can improve first-read UI/UX quality against `UI/UI-demo-new.png`.

# PR08 Shadow Depths D9 Disconnected Runtime Decision

## Verdict

`pr08-shadow-depths-d9-disconnected-runtime-topology-source-v1-accepted-forward`

## Direct Conclusion

The D9 disconnected `tileset.shadow_depths` runtime-risk crop is now captured as a repeatable golden evidence packet:

- seed: `2026060920`
- zone: `underground_river`
- floor: `2`
- profession: `rogue`
- topology decision: `TOPOLOGY_RISK_DISCONNECTED`
- visible topology: `16x12`, `129` visible cells, `fillPermille=671`, `aspectPermille=1333`, `connectedComponents=2`
- dedicated topology source: `ui.map_stage.shadow_depths.room_topology_source.pr08_demo`

This extends the D9 runtime-risk proof beyond the earlier `forest_edge` low-fill sample into a different risk class: disconnected visible topology. It proves the dedicated topology-source route is exercised by a real runtime map-stage crop for `shadow_depths`, but it does not approve all-map closure, packaged parity, final director-grade quality, or golden rebaseline.

## Evidence

| Artifact | Path | Hash |
| --- | --- | --- |
| Runtime map-stage crop | `UI/review/dark-uiux-pr08-exploration/shadow-depths-d9-disconnected-runtime-2026-06-03/dark-uiux-pr08-shadow-depths-d9-disconnected-runtime-crop.png` | `291d43c0076c30151dbfa80ccaf618e6d03e37dde1e304395a796f5d76773525` |
| Director review board | `UI/review/dark-uiux-pr08-exploration/shadow-depths-d9-disconnected-runtime-2026-06-03/dark-uiux-pr08-shadow-depths-d9-disconnected-runtime-board.png` | `4a578042ff53b00d5631457b48c6631f48c9bd060f42931eb51a5a8f87408055` |
| Evidence index | `UI/review/dark-uiux-pr08-exploration/shadow-depths-d9-disconnected-runtime-2026-06-03/evidence-index.tsv` | N/A |

Source hashes recorded in the evidence index:

- topology source PNG: `345ad931b002858ccb06dddc240b177a848565fb8ac393aabfc805f6a4e799a4`
- room plate PNG: `c61f5f7c6f9924acbae795a606b28b7ec697e463ea31a57d120bdf70421a2c46`

## Validation

Ran:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache
```

Result: `BUILD SUCCESSFUL in 9s`.

## Non-Goals

- No PR08 director hash rebaseline.
- No packaged app parity claim.
- No all-map or all-tileset closure.
- No source-art replacement in this packet.
- No return to ruins alpha, fog, line-weight, single-cell rectangle, seam, or same-key source polishing.

## Next Action

Use the `forest_edge` low-fill board and this `shadow_depths` disconnected board together as runtime-risk comparison evidence. The next implementation packet should improve first-read quality on the weakest recurring runtime-risk artifact, not add another governance-only accepted-forward note.

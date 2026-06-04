# Dark UI/UX PR08 D9 Real-Procgen ROI Probe

- sampleCount: 26
- fullPlateSafe: 14
- fullPlateSafePermille: 538
- topologyRisk: 12
- evidenceSuggestedRoiDecision: hybrid-first-convergence

## Decision Distribution

| decision | count |
| --- | ---: |
| FULL_PLATE_SAFE | 14 |
| TOPOLOGY_RISK_DISCONNECTED | 2 |
| TOPOLOGY_RISK_LOW_FILL | 10 |

## Tileset Distribution

| tileset | count | safe | risk |
| --- | ---: | ---: | ---: |
| tileset.forest_edge | 4 | 1 | 3 |
| tileset.mine | 4 | 2 | 2 |
| tileset.ruins | 4 | 2 | 2 |
| tileset.shadow_depths | 14 | 9 | 5 |

The evidence-suggested ROI decision is structural input for the PR08 decision packet; it uses no unstated hit-rate threshold and does not rebaseline golden hashes or close all-map Direction A by itself.

# Dark UI/UX Target Comps

`UI/targets/` stores temporary PR-08 target compositions and their sliceability evidence.

These files are design evidence, not resource authority:

1. They do not replace `UI/sprite-sheets/sheet-plan.yaml`.
2. They do not replace `UI/sprite-sheets/key-registry.yaml`.
3. They do not replace canonical or runtime visual manifests.
4. They must not be consumed directly by Kotlin runtime code.
5. They must not be used as golden baselines.

## Required PR-08 Files

| File | Meaning | Status |
| --- | --- | --- |
| `dark-uiux-director-grade-target-1672x941.png` | First-screen target comp matching the K-ToME shell layout | attempt 1 generated; needs reroll |
| `dark-uiux-director-grade-target-32px-tile-truth.png` | 32px tile-truth inset proving floor/wall strategy is sliceable | attempt 1 generated; failed sliceability |
| `dark-uiux-director-grade-target-1672x941.prompt.txt` | Prompt used for attempt 1 target comp generation | recorded |
| `dark-uiux-director-grade-target-1672x941-attempt2.png` | Stronger orthographic reroll candidate | rejected: baked text; partial map direction |
| `dark-uiux-director-grade-target-32px-tile-truth-attempt2.png` | Attempt 2 tile-truth sample | rejected: repeated sampled content still visible |
| `dark-uiux-director-grade-target-1672x941-attempt2.prompt.txt` | Prompt used for attempt 2 generation | recorded |
| `dark-uiux-director-grade-target-1672x941-attempt3.png` | Strongest monolithic composition reference | directional reference only; tile-truth failed |
| `dark-uiux-director-grade-target-32px-tile-truth-attempt3.png` | Attempt 3 tile-truth sample | rejected: crop sampling mixes compositor/prop/wall-edge content |
| `dark-uiux-director-grade-target-1672x941-attempt3.prompt.txt` | Prompt used for attempt 3 generation | recorded |
| `dark-uiux-director-grade-target-family-pack.md` | Floor/wall/UI chrome resource-family target pack | accepted for direction |
| `dark-uiux-director-grade-target-family-floor-32px.png` | Clean 32px floor family target | accepted for direction |
| `dark-uiux-director-grade-target-family-wall-32px.png` | Clean 32px wall family target | accepted for direction |
| `dark-uiux-director-grade-target-family-floor-wall-repeat.png` | Repeat sheet built from 32px family cells | accepted for direction |
| `dark-uiux-director-grade-target-family-map-compositor.png` | Separate darkness/light/telegraph compositor target | accepted for direction |
| `dark-uiux-director-grade-target-family-ui-chrome.png` | UI chrome target for panel/slot/deck/log surfaces | accepted for direction |
| `dark-uiux-director-grade-target-family-prompt.txt` | Prompt set used for the family target pack | recorded |

The target comp may be generated, manually composited, or produced in a design tool. It must keep the current runtime layout family: left rail, dominant center map, right panel, and bottom HUD.

## Acceptance Rules

Before a target comp can be marked `accepted`, `UI/manual-records/dark-uiux-director-grade-target-comp.md` must record:

1. Director verdict: `accepted`, `rejected`, or `needs-reroll`.
2. Side-by-side comparison against `UI/UI-demo-new.png` and current runtime evidence.
3. A 32px tile-truth inset using the same floor/wall strategy as the comp.
4. Resource-family sliceability table with owner, sheet, key, category, runtime display size, consumer and test.
5. Explicit rejection of baked text, full-screen paintover, atlas/region schema dependency, or new manifest schema dependency.

## Naming And Cleanup

Use repo-relative paths only. Do not place generated-image session ids, local absolute paths, or tool cache names in this directory.

Rejected target comps may be kept only when they are referenced by the manual record with a rejection reason. Unreferenced candidate images should be removed before PR close.

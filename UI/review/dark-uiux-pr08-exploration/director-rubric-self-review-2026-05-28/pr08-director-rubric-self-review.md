# PR-08 Director Rubric Self-Review - 2026-05-28

> Scope: AD chrome candidate plus current AB map/runtime compositor evidence
> Verdict: `accept-forward-not-final`

## Direct Conclusion

AD is accepted only as the current forward chrome candidate. It improves the
right panel and bottom deck family language enough to keep, but the full screen
still fails director-grade close against `UI/UI-demo-new.png`.

The next implementation pass should not start another floor/wall resource
batch and should not return to per-cell alpha, seam or micro-rectangle edits.
The active blockers are now:

1. shell-scale map room hierarchy: the center map still reads as a regular
   lattice before it reads as an authored room space.
2. action deck double-structure: generated socket wells and runtime overlays
   compete inside the bottom action console.
3. final evidence gap: there is no dedicated PR-08 director telegraph/combat
   golden crop yet; the current proxy crop is adequate for self-review but not
   final closure.

## Evidence

| Evidence | Path | Status |
| --- | --- | --- |
| Reference full screen | `UI/UI-demo-new.png` | target reference only |
| Review comparison board | `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-2026-05-28-comparison-board.png` | generated for this self-review |
| Reference map crop | `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-reference-map-crop.png` | approximate review crop, not runtime truth |
| Reference right-panel crop | `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-reference-right-panel-crop.png` | approximate review crop, not runtime truth |
| Reference bottom-deck crop | `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-reference-bottom-deck-crop.png` | approximate review crop, not runtime truth |
| Previous chrome baseline full screen | `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/baseline-before-chrome/runtime/pr08-chrome-candidate-baseline-before-chrome-ui-demo-new-parity-1672x941.png` | previous-best chrome comparison |
| Current AD full screen | `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-parity-1672x941.png` | current candidate |
| Current AD map crop | `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-map-stage-crop.png` | current candidate |
| Current AD right-panel crop | `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-right-panel-grid.png` | current candidate |
| Current AD bottom-deck crop | `UI/review/dark-uiux-pr08-exploration/generated-resource-sprint/chrome-resource-sprint/ad-conservative/runtime-final/pr08-chrome-candidate-ad-final-ui-demo-new-bottom-deck-no-command-hints.png` | current candidate |
| Current telegraph/combat proxy crop | `UI/review/dark-uiux-pr08-exploration/director-rubric-self-review-2026-05-28/pr08-director-self-review-current-telegraph-combat-proxy-crop.png` | proxy only; dedicated director crop still required |

## Rubric Result

| Area | Result | Finding |
| --- | --- | --- |
| First read | fail | The map remains dominant by placement, but it reads as a regular grid before it reads as a room. |
| Floor | fail | AB is better than earlier candidates, but the runtime crop still exposes repeated square cadence. |
| Wall | partial | Wall mass is more legible than the earliest PR-08 state, but it still lacks the thick masonry relief seen in the reference. |
| Darkness | fail | The dark field remains too rectangular and stage-like; it does not yet create the reference's organic room falloff. |
| Light | partial | Local warm pools guide attention, but broad haze still softens material and does not fully replace room-level AO. |
| Player/enemy/loot | pass for iteration | Markers are readable in the proxy crop, so actor/prop reroll remains deferred. |
| Telegraph | partial | The proxy crop keeps danger and selection readable, but final close still needs a dedicated PR-08 director telegraph/combat crop. |
| Right panel | accept-forward | AD makes slot and panel surfaces more coherent, but the equipment area still has a lot of empty structural field. |
| Bottom deck | partial | AD improves the shared material bed, but action sockets and runtime overlays now create double frames. |
| Text | pass for iteration | Runtime Chinese labels remain legible; no generated text or hotkey content was baked into AD. |
| Resource contract | pass for iteration | Existing keys/schema/sheets were preserved and PR-08 owner supersession is explicit for touched chrome keys. |

## Candidate Decision

AD should stay integrated for now. Reverting it would lose real chrome-family
cohesion, while AE and AF were already rejected for overpowering runtime
labels/icons. However, AD is not enough to promote `dark-uiux-pr08-director-*`
golden acceptance.

Do not update golden baselines from this state.

## Next Implementation Direction

Run a narrow runtime compositor/layout pass:

1. strengthen room-scale map hierarchy without reintroducing per-cell material
   overlays or a second visibility authority.
2. reduce action-deck double framing so the bottom console reads as one layer.
3. keep AD chrome resources and AB floor/wall as the current forward resource
   basis unless new evidence proves a resource-owned blocker.
4. after the pass, capture the same full/map/right/bottom evidence plus a real
   telegraph/combat crop before considering director golden promotion.

## Artifact Hashes

| Artifact | SHA-256 |
| --- | --- |
| `pr08-director-self-review-2026-05-28-comparison-board.png` | `c214c26399ee6f965439d580d2a1dc3bf4a698780201cb0e30ae4b2e52c4e0b6` |
| `pr08-director-self-review-current-telegraph-combat-proxy-crop.png` | `b242f3c18e72a4f66c8a03e5be183ab0afb46a302d2004e25d7807eb84d87bc1` |
| `pr08-director-self-review-reference-bottom-deck-crop.png` | `6f3553071a10f3376da30f33802077919b9c34f03b5100a6fb8842808a1b783f` |
| `pr08-director-self-review-reference-map-crop.png` | `9749d77a219af549042c543cdf50be37205d7fe7b67457970cf88350f8375bed` |
| `pr08-director-self-review-reference-right-panel-crop.png` | `37e31cf92929f446a4544597e375368750ef2f8c650a044eca4ea3072fd0a1d5` |

# Dark UI/UX PR-08 Non-Ruins Packaged Parity

> Date: 2026-06-02
> Scenarios:
> `dark-uiux-pr08-director-forest-map-stage`,
> `dark-uiux-pr08-director-mine-map-stage`,
> `dark-uiux-pr08-director-shadow-depths-map-stage`
> Status: `PACKAGED_CROPS_AND_SUMMARY_CAPTURED_V4_SHADOW`

## Summary

This record tracks packaged-app parity for the first accepted non-ruins
room-art families. The three sibling scenarios are scoped to map-stage family
parity at `1280x800`:

1. `tileset.forest_edge`: `greenwood_fringe`, `rogue`, seed `20260602`
2. `tileset.mine`: `deep_iron_pit`, `vanguard`, seed `20260603`
3. `tileset.shadow_depths`: `grey_gate_depths`, `templar`, seed `20260604`

Each scenario requires a full packaged window capture, a map-stage crop, right
panel and bottom deck guard crops, an evidence-summary capture, and the app log.
The right and bottom crops are regression guards for shell hierarchy; the
director-grade decision remains centered on the map-stage family crop.

## Current Status

The validation scenarios, presentation titles, materialization window sizes,
whitebox YAML entries, and CLI expected-evidence contracts are wired. All three
packaged app windows were launched and captured at the `1280x800` whitebox
target; full-window, map-stage, right-panel and bottom-deck crops now exist for
forest-edge, mine and shadow-depths. Shadow-depths was recaptured after the V4
right-mass room-art plate replaced V3.

The `show-evidence-summary` action is now captured and logged for forest-edge,
mine and shadow-depths. Forest-edge used the normal packaged interaction path.
Mine and shadow-depths use the constrained
`KTOME_VALIDATION_STARTUP_SURFACE=evidence-summary` startup route because local
Mac keyboard/Computer Use routing did not reliably open the validation overlay
for those transient packaged bundles. Failed input probe screenshots remain
outside the official expected-evidence filenames to avoid false closure.

## Materialization Smoke

| Scenario | Command result | Generated contract |
| --- | --- | --- |
| `dark-uiux-pr08-director-forest-map-stage` | `PASS` | `build/whitebox/dark-uiux-pr08-director-forest-map-stage/cua-runbook.md`, `build/whitebox/dark-uiux-pr08-director-forest-map-stage/expected-evidence.json` |
| `dark-uiux-pr08-director-mine-map-stage` | `PASS` | `build/whitebox/dark-uiux-pr08-director-mine-map-stage/cua-runbook.md`, `build/whitebox/dark-uiux-pr08-director-mine-map-stage/expected-evidence.json` |
| `dark-uiux-pr08-director-shadow-depths-map-stage` | `PASS` | `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/cua-runbook.md`, `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/expected-evidence.json` |

This smoke pass proves the packaged validation packets can be generated. The
captures below add the first real packaged visual result for the three family
crops.

## Packaged Capture Results

| Scenario | Full window | Map-stage crop | Right panel crop | Bottom deck crop | Evidence summary |
| --- | --- | --- | --- | --- | --- |
| `dark-uiux-pr08-director-forest-map-stage` | `42d00bfde594d65a5e154a2b5adb231907c60138c346fd50b249855f471cb294` | `0950115319c99cfecac56babdbb94690421d9a90a633461f8b134c9943ab9868` | `c97dc87c2246f89e8be2a6009fd884bb7f8ccfde3e35a3638aac0eaf4b2d821f` | `0452a57d5d0a6c3d3c6f29a105ccba8e053de5d86decb67914074e7f64cfd354` | `PASS`, screenshot `1abd4612048c045602d18a35103cbc9a54c74d50e34696babd3f55dd40fcd0bb`; runtime log contains `action show-evidence-summary` |
| `dark-uiux-pr08-director-mine-map-stage` | `8e459c0cc671037b249b091b2f3bb41f307dc1b3681cdddaa3f8ff0b3a898160` | `1a81bacfe7d63245ebc833c47d6c378baddfe970d03d182d8f4ece229faa335d` | `5c909bad145f6d8501b72978b6237db0cc39c5078580bd4e63b7046d70978b1b` | `411dd453346b919ede2f715c5ebb0b6882e50be1825ae73ed16801780456574f` | `PASS`, screenshot `7fa559037e2199507c8e08b915125336e81e87547598bf8a2e25bcf21fcb56f4`; runtime log contains `action show-evidence-summary` |
| `dark-uiux-pr08-director-shadow-depths-map-stage` | `9ed6b4a0fc75cd67a847569fb6e937b4fe538085878afa745277b45e9ef17a62` | `d9e57405a62fc1206a8a90706b19cf660c62ca0349b6491f7ab2d89fa03b0d15` | `9806e9dd02ae99bc7564801bd707084f203f7dd449681756c5298bff847437d8` | `1e7bbeaf6879884beff09119b6bca4a40c19dc06353955d9ea2ba767b052f396` | `PASS`, screenshot `ba1c14950b967ebe0f6ae893d7aea16d7580a92bc258b02d10e15083464f2e68`; runtime log contains `action show-evidence-summary` |

Packaged fixed-crop review board:

`UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/packaged-non-ruins-family-map-stage-crop-board.png`

Hash: `6143ee8037d2845d0a90ac58f86facf3481eeb5f1f70b0987ddf5426920e6251`.

Packaged fixed-crop review board after the shadow-depths V4 right-mass pass:

`UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/packaged-non-ruins-family-map-stage-crop-board-v4-shadow.png`

Hash: `d7676cfd8bd9d3eadaaba83fe6d21b05d99342674f327063c6690307cc47bef9`.

## Required Packaged Evidence

| Scenario | Required evidence prefix |
| --- | --- |
| `dark-uiux-pr08-director-forest-map-stage` | `build/whitebox/dark-uiux-pr08-director-forest-map-stage/evidence/dark-uiux-pr08-director-forest-*` |
| `dark-uiux-pr08-director-mine-map-stage` | `build/whitebox/dark-uiux-pr08-director-mine-map-stage/evidence/dark-uiux-pr08-director-mine-*` |
| `dark-uiux-pr08-director-shadow-depths-map-stage` | `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-*` |

## Director Verdict

`ACCEPTED_FORWARD_CORE_PACKAGED_CROPS_V4_SHADOW`.

Forest-edge and mine packaged map-stage crops prove that the family-specific
room-art route survives packaged runtime capture and does not reuse the ruins
proof slice. Forest-edge has the strongest first read: mossy stone, roots,
torch pools, player, stairs and fog hierarchy all remain legible. Mine also
holds as a distinct industrial family with readable timber, ore, lantern pools
and tactical overlays.

Shadow-depths V4 is accepted-forward as the current packaged core crop. It no
longer has the V3 blocker where the right half read as an empty tactical grid:
the packaged fixed crop now has a secondary right-side wall return, violet
shadow seams and a broken slab lane under the runtime grid. Treat it as a
specific blocker resolved, not final non-ruins parity. It remains narrower and
more grid-readable than forest-edge and mine in the lower-right play lane, so
broader topology coverage is still required before final all-map acceptance.

Do not claim all-map closure, final non-ruins closure, or director-grade PR08
closure from this packet. The mine/shadow summary-action capture gap is now
closed through the constrained startup surface, but final closure still
requires broader topology coverage and a director-grade all-map acceptance pass.

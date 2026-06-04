# Dark UI/UX PR-08 Non-Ruins Packaged Parity

> Date: 2026-06-02
> Scenarios:
> `dark-uiux-pr08-director-forest-map-stage`,
> `dark-uiux-pr08-director-mine-map-stage`,
> `dark-uiux-pr08-director-shadow-depths-map-stage`
> Status: `PACKAGED_WALL_FAMILY_GHOST_V1_REJECTED_BACKED_OUT`

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

2026-06-03 update: packaged parity was rerun after the non-ruins wall-family
variation packet. The topology-source-only packaged route was rejected for
forest-edge and shadow-depths. The family-ghost recovery was also rejected and
backed out because it made the map read as chopped source strips, debug floor
and auxiliary rails rather than authored-room art.

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

## 2026-06-03 Wall-Family Packaged Recovery

Rejected topology-source-only board:

`UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-packaged-parity-2026-06-03/packaged-non-ruins-wall-family-topology-source-only-rejected-board.png`

Hash: `30661acf94ffb51800c7f6319165de33a897a8c7afb83f31734db7175fc9feac`.

Rejected family-ghost board:

`UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-packaged-parity-2026-06-03/packaged-non-ruins-wall-family-family-ghost-v1-board.png`

Hash: `19a40a9e61fa60b8c1cace69745364b3cd571541c88b793cfdf929bc9c01d0b2`.

| Scenario | Full window | Map-stage crop | Right panel crop | Bottom deck crop | Note |
| --- | --- | --- | --- | --- | --- |
| `dark-uiux-pr08-director-forest-map-stage` | `98094de847aaa520046efa3b5a6806e68007aeeed616d18d0b60153c1d77cf27` | `60c95a8d5ee7e13fb8b8e95b85cbcf2ec897ed8702ed8f69b8d519972afae574` | `497dc5498293d8a33b1d5b2c1270d896c35b92a8a2c9c47261c36a948be13335` | `1792f55bf0f07e151e8e32840624e654a955255c4948b56edeb4f2af81df87e0` | rejected; still reads as debug floor / chopped source strips |
| `dark-uiux-pr08-director-mine-map-stage` | `b279d5cd142ceb393ecf122563caea9fa261d0d4b9420c7b20aa93f6b2a4c31f` | `dcc8920a762ce554be4cd8c54afeed859ee06b8aad04a5fd7d454682aef4fef3` | `da2334c175c393e3bbaf4ac62c57068db8c11352c86a62ce843baa3f3e81897e` | `09643fd61cc6feebe4251e338341d5551eabf2872e7a9be972c930187e7731d1` | rejected as a branch result; mine holding is not enough to accept the route |
| `dark-uiux-pr08-director-shadow-depths-map-stage` | `b91a353a8e60926d1e7d600f4118a1ee3312384b3ef77c16cc8d0a01dc9c1d0b` | `3c5f1f494c434f7624dd00f5811ef7b76ec958e40e24aac8adc45b590ebc132c` | `815fc8ca280661fc23d1e5ce70e5806285daf7c77821fdee699985abb526e1dd` | `261c14c775f87239b3b4c86aa4cc40f4ab1229c12e64d6fc3448cff9e51b4669` | clean recapture after moving mouse out of map; hover-tooltip polluted capture replaced |

Detailed hashes and app log hashes are recorded in
`UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-packaged-parity-2026-06-03/evidence-index.tsv`.

## Required Packaged Evidence

| Scenario | Required evidence prefix |
| --- | --- |
| `dark-uiux-pr08-director-forest-map-stage` | `build/whitebox/dark-uiux-pr08-director-forest-map-stage/evidence/dark-uiux-pr08-director-forest-*` |
| `dark-uiux-pr08-director-mine-map-stage` | `build/whitebox/dark-uiux-pr08-director-mine-map-stage/evidence/dark-uiux-pr08-director-mine-*` |
| `dark-uiux-pr08-director-shadow-depths-map-stage` | `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-*` |

## Director Verdict

`PRIOR_CORE_PACKAGED_CROP_REFERENCE_V4_SHADOW`; superseded for the current
wall-family validation branch by
`PACKAGED_WALL_FAMILY_GHOST_V1_REJECTED_BACKED_OUT`.

Earlier forest-edge and mine packaged map-stage crops remain reference evidence
that the family-specific room-art route can render in packaged runtime without
reusing the ruins proof slice. They are not current packaged acceptance proof
after the 2026-06-03 wall-family validation branch failed. Forest-edge had the
strongest first read in that prior baseline: mossy stone, roots, torch pools,
player, stairs and fog hierarchy remained legible. Mine also held as a distinct
industrial family with readable timber, ore, lantern pools and tactical
overlays.

Shadow-depths V4 remains a prior reference crop that fixed the V3 blocker where
the right half read as an empty tactical grid: the packaged fixed crop had a
secondary right-side wall return, violet shadow seams and a broken slab lane
under the runtime grid. Treat it as historical blocker evidence, not current
non-ruins packaged parity. It remains narrower and more grid-readable than
forest-edge and mine in the lower-right play lane, so broader topology coverage
and a stronger map-stage presentation are still required before final all-map
acceptance.

Do not claim all-map closure, final non-ruins closure, or director-grade PR08
closure from this packet. The mine/shadow summary-action capture gap is now
closed through the constrained startup surface, but final closure still
requires broader topology coverage and a director-grade all-map acceptance pass.

The 2026-06-03 family-ghost recovery does not upgrade the non-ruins route and
must not be treated as accepted-forward. Forest-edge and shadow-depths need a
broader map-stage presentation change before any director-grade closure claim.

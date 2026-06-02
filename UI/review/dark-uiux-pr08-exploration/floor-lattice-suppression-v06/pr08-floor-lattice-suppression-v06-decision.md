# PR-08 Floor Lattice Suppression V06 Decision

> Date: 2026-05-28
> Status: `accept-forward-not-final`
> Scope: `tileset.ruins.ground_01` floor family plus ground terrain seam bleed

## Direct Conclusion

V06e `regrain` is accepted only as the current forward floor-family polish. It
keeps more stone texture than V06f while reducing some repeated floor-cell
structure from V05.

This is not director-grade closure. The runtime crop still reads grid-first
because wall/grid/contact authority remains stronger than room-scale material
read.

## Candidate Verdict

| Candidate | Verdict | Reason |
| --- | --- | --- |
| V06a moderate | rejected | too close to V05; repeated floor cadence remains visible |
| V06b strong | rejected | improves profile balance but still keeps too much repeated tile structure |
| V06c material | rejected | useful diagnostic, but not enough runtime change |
| V06d profile flat | rejected | calmer, but not materially better than V06e |
| V06e regrain | accept-forward | best balance of quieter lattice and retained stone texture |
| V06f quiet | rejected | suppresses texture too much; the coarse wall/grid lattice becomes more dominant |

## Runtime Decision

The retained runtime state is:

1. V06e floor family in `r02-ui-demo-ruins-tiles`.
2. `tile_ground` terrain bleed widened from `4f` to `14f`.
3. terrain base draw order tightened to ground terrain first, upper terrain
   second, so wider floor bleed does not become the final wall authority.

This is an engineering and visual forward step only. The remaining map blocker
is not floor texture noise; it is the coarse wall/floor lattice read. The next
iteration should address wall/grid interaction, not another floor-only polish.

## Evidence

| Evidence | Path |
| --- | --- |
| Candidate contact board A-C | `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/pr08-floor-lattice-suppression-v06-contact-board.png` |
| Candidate contact board D-F | `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/pr08-floor-lattice-suppression-v06d-f-contact-board.png` |
| Final runtime comparison | `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/pr08-floor-lattice-suppression-v06-final-runtime-comparison-board.png` |
| V06e runtime crop | `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/runtime-v06e-ground-first-bleed14/ui-demo-new-map-stage-crop.png` |
| V06f rejected crop | `UI/review/dark-uiux-pr08-exploration/floor-lattice-suppression-v06/runtime-v06f-ground-first-bleed14/ui-demo-new-map-stage-crop.png` |

## Artifact Hashes

| Artifact | SHA-256 |
| --- | --- |
| final runtime comparison board | `a7b0ae6bb754aace0ad4d469f6530e01d5a223abc2df1b6e5db98a540c1fb09a` |
| candidate contact board A-C | `c8243d9b9333c5a883cca3de4325a49f0eed3a6c8df07306cb604d59c1081cc9` |
| candidate contact board D-F | `ae761bf193555af59bd165b18bf7e21167cadf2ee45a0dde4af27ec24bf521ef` |
| V06e runtime map crop | `9226eb319df13a39a778e364587b81b849fd1fa97827260d01cf782d35395282` |
| V06e runtime full screen | `ce6bee00a4c79646d7cf246d404a518bda97077b91c80924d921ea62a65857f6` |

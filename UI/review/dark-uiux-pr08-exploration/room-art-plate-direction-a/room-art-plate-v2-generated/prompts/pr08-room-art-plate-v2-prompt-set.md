# PR-08 Direction A Room Art Plate V2 Prompt Set

> Date: 2026-05-31
> Status: review-only prompt evidence
> Scope: edited/generated successors to Candidate C after D3 showed remaining baked square slab rhythm

These prompts are for review-only image generation and runtime evidence
prototyping. They are not final golden baselines and do not by themselves close
D3 map-stage review.

Shared intent:

1. Use Candidate C as composition reference: same 4:3 room plate, top-down /
   slightly isometric tactical camera, thick ruin walls, corridor mouths, warm
   torch pools and dark corners.
2. Replace the current floor read with a less square, less orthogonal and less
   grid-forward authored room material.
3. Keep the center playable field calm enough for runtime actors, loot markers,
   cursor, target highlights and telegraphs.
4. Do not include characters, monsters, corpses, loot, treasure chests, traps,
   icons, UI chrome, text, numbers, labels, watermark, cursor, selection ring,
   telegraph or health bars.

## Candidate E: No Square Rhythm

Primary request: preserve Candidate C's room silhouette, camera angle, corridor
mouths, thick masonry wall mass, dark perimeter and warm torch pools, but
materially improve the floor by removing the baked square slab / tactical-grid
rhythm.

Floor treatment: large irregular non-orthogonal stone plates, hand-broken
cracks, varied polygonal slabs, dust and subtle rubble; avoid straight
north-south/east-west seam runs; avoid repeated cell-sized square tiles; avoid a
visible grid.

## Candidate F: Continuous Stone Field

Primary request: produce a stronger authored-room successor where the floor
reads as one continuous ancient stone field instead of a tactical tile board.
Preserve thick walls, corridor mouths, dark corner falloff and warm torches, but
make the central floor less square, less orthogonal and less repetitive.

Floor treatment: blended room-scale stone field with irregular fractured
plates, diagonal crack families, subtle dust bands, chipped stone islands and
painterly surface wear; no cell-sized square slabs, no repeating rectangular
masonry grid, no clean straight seam columns or rows.

## Candidate G: Bedrock Room Field

Primary request: make a more radical replacement plate that solves the
grid-first problem at the source: the center floor should read as hand-authored
broken bedrock and worn flagstone islands, not as repeated square slabs.

Floor treatment: broad organic fractures, uneven stone islands, dust gradients,
scattered micro rubble, softened transitions, very few long straight horizontal
or vertical seams; no equal-size tile rhythm; no tactical board grid; no square
slab array.

## Generated Files

| Candidate | File | SHA-256 |
| --- | --- | --- |
| E | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/candidates/pr08-room-art-plate-e-no-square-rhythm.png` | `3c20ec159165f2ccb40f6741dc950027a69271bf9dc9c0cb1e7cb9b2079736f2` |
| F | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/candidates/pr08-room-art-plate-f-continuous-stone-field.png` | `21e520fe073a80e6f34f32badd86ec3a5ed181c10eddcbe42a0cb11b497579af` |
| G | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/candidates/pr08-room-art-plate-g-bedrock-room-field.png` | `d5702cc8a9d1e89e51081a18526a166af4b2e5c7b10b4575c115709e9bd9eca1` |

Initial raw-image read:

1. E improves Candidate C but still has visible large rectangular floor masses.
2. F is the best balanced candidate for runtime: continuous floor field,
   reduced square rhythm, still calm enough for markers.
3. G is the most aggressive square-rhythm removal, but may be too busy under
   runtime overlays.

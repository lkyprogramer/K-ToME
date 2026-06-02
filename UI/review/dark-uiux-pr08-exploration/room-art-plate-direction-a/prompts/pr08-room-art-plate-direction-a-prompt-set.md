# PR-08 Direction A Room Art Plate Prompt Set

> Date: 2026-05-31
> Status: review-only prompt evidence
> Scope: `UI/goal/dark-uiux-pr08-pre-rendered-room-art-plate-plan.md` Sprint 1

These prompts are for review-only image generation. They are not manifest keys,
sheet-plan rows, runtime resources, golden baselines or production acceptance.

Shared constraints for every candidate:

1. no characters, monsters, corpses, loot, chest, stairs icon, cursor, selection
   ring, attack telegraph, health bar, UI chrome, text, numbers, labels,
   watermark or logo;
2. top-down or slightly isometric tactical camera, suitable as a single map-stage
   room background plate beneath runtime overlays;
3. dark fantasy worn-stone ruins, low saturation, warm local torch pools, thick
   walls, irregular floor slabs, corridor mouths and dark corner falloff;
4. enough tonal headroom for K-ToME runtime actors, loot markers, telegraphs,
   cursor, selection and path/range overlays to remain readable;
5. no full-screen UI paintover, no baked gameplay semantics and no resource key
   text.

## Candidate A: Authored Ruins Chamber

Use case: stylized-concept
Asset type: review-only K-ToME room art plate
Primary request: create a top-down dark-fantasy dungeon room background plate,
one authored ruins chamber with thick worn masonry walls, irregular stone slabs,
subtle rubble, four corridor mouths and warm torch pools baked into the room
material.
Scene/backdrop: ancient ruined dungeon room, rectangular tactical footprint with
slightly broken edges, heavy wall mass and non-rectangular dark corners.
Composition: center floor remains clear enough for gameplay markers; walls and
corners carry most of the visual weight; corridor mouths are readable but not
symbolic.
Style: Diablo-like authored environment, painterly pixel-adjacent texture,
low-saturation charcoal stone, ember highlights, restrained cyan absent.
Avoid: characters, monsters, loot, props with gameplay meaning, UI, text,
numbers, labels, cursor, telegraph, health bars, floor grid lines, watermarks.

## Candidate B: Torch-Cut Stone Arena

Use case: stylized-concept
Asset type: review-only K-ToME room art plate
Primary request: create a slightly isometric top-down dungeon room plate with a
warm torch-lit cross pattern, thick blackened stone walls, worn floor slabs,
dark edge falloff and subtle floor damage.
Scene/backdrop: enclosed tactical chamber, masonry wall shoulders, corridor
mouths at north/east/south/west, baked light pools that do not hide markers.
Composition: stronger light near wall torches, calmer mid-floor with authored
slab rhythm, soft darkness outside the playable room boundary.
Style: dark roguelike, forged iron and worn stone mood, warm light balanced
against cold void, high material quality without noisy micro-detail.
Avoid: characters, monsters, loot, text, numbers, labels, UI frame, grid,
telegraph, cursor, selection ring, explicit traps, blood symbols, watermark.

## Candidate C: Broken Slab Hall

Use case: stylized-concept
Asset type: review-only K-ToME room art plate
Primary request: create a top-down authored dungeon room background with a
larger broken-slab floor field, asymmetrical wall thickness, chipped masonry,
fallen stone dust and soft warm light from offscreen sconces.
Scene/backdrop: old stone hall with corridor mouths, dark corner pockets and
uneven floor plate rhythm, designed to replace grid-first tile material.
Composition: asymmetric but still readable as a tactical map rectangle; the
floor is more continuous than per-cell; walls form a clear room enclosure.
Style: dark fantasy dungeon concept art, painterly stone, low contrast central
field, stronger boundary mass, no modern UI language.
Avoid: characters, monsters, loot, signs, readable runes, text, digits, UI,
cursor, telegraph, item silhouettes, hard grid, watermark.

## Candidate D: Material-Only Technical Fallback

Use case: stylized-concept
Asset type: review-only technical fallback floor/background plate
Primary request: create a top-down dark ruins material plate with irregular
stone slabs, subtle cracks, low-saturation grit and very quiet warm lighting,
without authored wall mass.
Scene/backdrop: continuous floor material intended only to test renderer
architecture if full-room wall plates fail.
Composition: calm repeat-resistant central material, no strong semantic props,
no perimeter gameplay signals.
Style: worn stone, ember-dark fantasy, subdued contrast, marker-safe.
Avoid: walls as gameplay truth, characters, loot, cursor, telegraph, text,
numbers, labels, grid lines, UI, watermark.

Important: Candidate D is not the default Direction A target. It is a technical
fallback only if lky rejects full-room authored candidates.

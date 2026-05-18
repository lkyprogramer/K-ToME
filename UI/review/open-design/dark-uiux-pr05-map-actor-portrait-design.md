# Dark UI/UX PR05 Map Actor Portrait Design Notes

## 1. Status And Authority

This document is an Open Design helper for `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md`.

It is not a resource contract. It exists to help PR05 reviewers and asset authors discuss art direction, prompt variants, and contact-sheet QA before any resource enters the K-ToME pipeline.

Hard authority boundaries:

| Topic | Authority | This file may help with | This file must not do |
| --- | --- | --- | --- |
| PR05 scope, gates, owner sheets, evidence labels | `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md` | restate visual intent and QA emphasis | change required sheet ids, gates, golden labels, manual record fields, or rollback rules |
| Dark UI style | `UI/ART_STYLE_BIBLE.md` | derive art direction language | redefine `ktome-dark-fantasy-sprite-ui-v1`, palette, material rules, or sheet constraints |
| Open Design boundary | `UI/review/open-design/ktome-dark-ui-design.md` | reuse auxiliary-only rules | become a second source of truth |
| Sheet mapping | `UI/sprite-sheets/sheet-plan.yaml` | provide prompt-fragment ideas for existing sheet families | define `row`, `col`, `targetKey`, `outputName`, `rawSheetPath`, or category |
| Resource truth | canonical manifest then synced runtime manifest | remind reviewers to check manifest coverage | invent manifest keys or runtime fallback rules |

Final resource flow remains:

```text
sheet-plan.yaml -> generated prompt -> raw sheet -> contact sheet QA -> cut/runtime PNG -> canonical manifest -> sync runtime manifest -> owner-scope coverage
```

## 2. PR05 Design Job

PR05 replaces map, actor, bestiary, and portrait visual families after the UI chrome and Talent Assign surface are already established.

The design problem is not "make every image more detailed." The useful goal is:

1. terrain and walls must show walkability and boundaries at gameplay scale;
2. props must read as interactable or environmental without text labels;
3. VFX and telegraphs must remain visible when actors occupy the same cell;
4. actor sprites must have class/faction silhouette at small size;
5. bestiary icons must remain readable as compact UI icons;
6. portraits and zone visuals must share the same dark-fantasy era as tiles, props, and actors;
7. every accepted asset must still satisfy the machine pipeline, not just visual taste.

## 3. Round-Level Art Direction

| Round | Sheet family | Art direction | Main readability risk |
| --- | --- | --- | --- |
| Round 2 | `r02-tiles-ground`, `r02-tiles-wall`, `r02-tiles-decal` | top-down tactical surfaces; ground is walkable and lower contrast, wall is blocking and edge-heavy, decal is readable but subordinate | ground and wall become same-value dark noise |
| Round 3 | `r03-props-interactable`, `r03-props-environment`, `r03-vfx-telegraph` | props are single-object silhouettes; interactables use small ember/cyan cues; telegraphs use clear shapes without hiding actors | prop/detail overdraw competes with actors and loot markers |
| Round 4 | `r04-actors-player`, `r04-actors-humanoid`, `r04-actors-monster`, `r04-actors-boss` | compact top-down or three-quarter gameplay tokens, strong silhouette, faction identity first, detail second | actors become portraits or tiny full-body paintings unreadable at map scale |
| Round 5 | `r05-bestiary-humanoid-icons`, `r05-bestiary-creature-icons`, `r05-boss-icons` | icon crops emphasize head/crest/symbol silhouette; bestiary category should be obvious at 32px | icon and actor language diverge into different eras |
| Round 6 | `r06-portraits-classes`, `r06-portraits-trees`, `r06-portraits-zones` | portrait-quality identity images with the same iron/stone/ember/cyan material language; no marketing poster composition | portraits become too bright, cinematic, or disconnected from map resources |

## 4. Shared Style Rules

All PR05 image work should preserve:

1. `ktome-dark-fantasy-sprite-ui-v1` style tag.
2. Low-saturation charcoal base.
3. Forged iron, worn stone, old leather, brass, soot, ember, and restrained cyan accents.
4. No text, numbers, labels, UI keycaps, watermarks, or target-key-like symbols baked into images.
5. One centered subject per cell.
6. Readable silhouette at `32x32` for icons and actors.
7. Category-specific perspective:
   - tiles: top-down tactical surface;
   - props: map-object token;
   - actors: compact gameplay sprite, not portrait;
   - bestiary icons: compact UI icon;
   - portraits: larger identity art, not map sprite.

## 5. Prompt Variant Kit

These are prompt fragments for design exploration. They are not final prompt files. Final prompt files must be generated from `sheet-plan.yaml` and the owning PR pipeline.

### 5.1 Global Prefix

Use this as the stable baseline language:

```text
Style tag: ktome-dark-fantasy-sprite-ui-v1.
Dark fantasy roguelike tactical game art, charcoal black, forged iron, worn stone, old leather, ember highlights, restrained cyan rim light, low saturation, strong readable silhouette, no text, no numbers, no labels, no watermark.
```

### 5.2 Variant A: Tactical Readability

Best for tiles, props, and actors when first-pass generated art is too painterly.

```text
Prioritize gameplay readability over detail. Strong outer silhouette, clear material boundary, limited internal texture, readable at 32x32, centered subject with transparent-safe margin, no decorative clutter.
```

Trade-off: less ornate, but more likely to pass contact-sheet QA.

### 5.3 Variant B: Material Cohesion

Best when tile/prop/portrait families start drifting apart.

```text
Unify the era through worn stone, black iron, soot, old leather, and small ember scratches. Keep cyan only as a thin edge accent. Avoid parchment beige, glossy plastic, clean vector art, and bright heroic high fantasy.
```

Trade-off: stronger global consistency, but can become too dark if not checked at small size.

### 5.4 Variant C: Actor Silhouette

Best for Round 4 actor sheets.

```text
Compact top-down three-quarter gameplay sprite, faction readable from silhouette first, weapon or body shape close to the figure, no long thin details, no portrait pose, no full illustration background, transparent background.
```

Trade-off: improves map readability, but sacrifices facial/detail identity.

### 5.5 Variant D: Telegraph Overlay Safety

Best for Round 3 VFX/telegraph sheets.

```text
Readable warning shape designed to sit under or around a dark actor sprite. Keep the center partially open, use a clean ring, wedge, sigil, or lane shape, restrained red/ember/cyan edge, transparent background, no dense filled glow.
```

Trade-off: less spectacular, but safer when actor, VFX, and telegraph overlap.

### 5.6 Variant E: Portrait Era Match

Best for Round 6 portraits.

```text
Dark fantasy identity portrait with the same forged iron, worn stone, ember, and cyan-edge era as the gameplay UI. Character or zone identity is clear, background is restrained, no promotional hero poster, no bright clean studio lighting.
```

Trade-off: ties portraits to UI era, but needs review to avoid losing class/zone specificity.

## 6. Family-Specific Direction

### 6.1 Tiles

Ground:

1. lower contrast than actors and props;
2. clear walkable surface;
3. subtle texture, not noise;
4. no strong highlight in the center of every cell.

Wall:

1. stronger boundary and blocking silhouette;
2. darker vertical/edge cues;
3. must not look like a prop or actor token.

Decal / terrain interaction:

1. readable state shape;
2. subordinate to actor and telegraph;
3. no full-cell high-saturation fill unless it is a hazard.

### 6.2 Props

Interactable props need one of:

1. ember focal point;
2. cyan rim or rune accent;
3. strong object silhouette;
4. a material cue that separates it from ordinary terrain.

Environmental props should be quieter. If both types look equally bright, players cannot tell what is usable.

### 6.3 VFX And Telegraphs

Telegraph design must be tested as a layer, not as an isolated icon.

Rules:

1. shape readable under/around actor;
2. center not fully opaque;
3. boss warning visually stronger than ordinary VFX;
4. ordinary VFX should not drown boss telegraph;
5. no text, arrows, exclamation marks, or UI labels baked into the art.

Suggested shape language:

| Telegraph type | Safer shape |
| --- | --- |
| single-cell danger | ring / cracked sigil / ember outline |
| cone or sweep | wedge arc with transparent center |
| lane | broken rune lane with visible tile beneath |
| boss warning | heavier sigil edge plus red/ember accent |
| zone pressure | subtle atmospheric plate, lower opacity |

### 6.4 Actors

Actor priority:

1. silhouette;
2. faction/class identity;
3. weapon/body shape;
4. palette accent;
5. interior detail.

Actor rejection triggers:

1. reads as a portrait, not a map sprite;
2. thin weapon or limb disappears at 32px;
3. shape is just a dark blob on dark ground;
4. silhouette is too similar across factions;
5. actor has a baked shadow that conflicts with tile lighting.

### 6.5 Bestiary Icons

Bestiary icons should connect to actor identity but are allowed to be more emblematic.

Good icon focus:

1. head silhouette;
2. faction crest;
3. creature skull/horn/claw shape;
4. boss emblem with clear outline.

Avoid:

1. full-body tiny actor repeated as icon;
2. portrait crop with unreadable face;
3. UI frame baked into the image unless the sheet/category owns the frame.

### 6.6 Portraits And Zone Visuals

Portraits are larger identity art, but they must still belong to the same visual era.

Profession portraits:

1. class identity clear at first glance;
2. material language matches equipment/actor sheet;
3. no bright studio portrait look.

Talent tree portraits:

1. symbolic branch identity;
2. no text;
3. no gameplay-rule diagram;
4. readable iconography tied to tree theme.

Zone visuals:

1. place identity first;
2. dark UI-compatible contrast;
3. not a full scenic poster;
4. same era as tiles and props for that zone.

## 7. Contact-Sheet QA Rubric

Use this before cutting runtime PNG. Scores are review aids; pass/fail remains owned by the PR05 pipeline and manual record.

| Criterion | Pass | Revise | Reject |
| --- | --- | --- | --- |
| Grid integrity | every cell has one centered subject, no crossing cell boundaries | minor safe-margin inconsistency | merged cells, clipped subject, or subject crossing grid |
| No baked text | no letters, numbers, labels, hotkeys, watermarks | ambiguous rune that might read as a letter | readable text, logo, target key, row/col, or watermark |
| Style tag fit | dark fantasy, iron/stone/leather/ember/cyan language | mostly correct but too bright/clean | sci-fi, anime/chibi, gacha shine, glossy plastic, office vector |
| Silhouette | recognizable at gameplay scale | identifiable only at full contact-sheet size | unreadable blob or over-detailed smear |
| `32x32` readability | subject identity survives downscale | category survives but specific identity weak | cannot tell category or state |
| Category perspective | tile/prop/actor/icon/portrait uses correct viewpoint | mixed viewpoint but recoverable | actor looks like portrait, tile looks like poster, icon looks like full scene |
| Family consistency | same sheet shares light direction, material, outline, and density | one or two cells drift | sheet reads as multiple unrelated art generations |
| Layer safety | asset will not hide higher-priority gameplay information | needs overlay test | likely hides actor, loot marker, telegraph, or walkability |
| Manifest readiness | can plausibly map to existing category/output after cut | requires owner clarification | implies new key/category not in PR05 contract |

Minimum recommendation:

1. Pass all hard criteria: grid integrity, no baked text, category perspective, manifest readiness.
2. No `Reject` in silhouette or `32x32` readability for actor/icon/telegraph cells.
3. Any `Revise` that affects player-visible owner keys must be listed in the manual record or fixed before PR close.

## 8. Overlay QA For Telegraph And Actors

Contact sheets alone are insufficient for telegraph and actor assets. The QA pass should include a simple overlay mental check before runtime golden evidence:

| Overlay case | What to check |
| --- | --- |
| actor over ground | actor silhouette remains visible on dark ground and lighter ground |
| actor over decal | actor still reads; decal does not become actor outline |
| boss over warning sigil | warning remains visible around boss body |
| ordinary VFX plus telegraph | boss/telegraph priority is still visually obvious |
| loot marker near actor | marker is not mistaken for actor equipment or telegraph |
| wall near actor | actor does not merge into wall edge |

Design rule: telegraph should create a readable shape around or beneath the actor, not a dense visual block on top of the actor.

## 9. Cross-Family Consistency Checks

| Pair | Consistency question |
| --- | --- |
| tile and prop | does the prop look physically present on the same terrain era? |
| prop and actor | can interactable props be distinguished from actor silhouettes? |
| actor and bestiary icon | does the icon feel like the same faction/creature family as the actor? |
| boss actor and boss icon | do both share identity without duplicating the same crop? |
| zone visual and tiles | does the portrait/zone visual imply the same place as the map resource? |
| profession portrait and actor | does the portrait elevate the actor identity without contradicting armor/material language? |
| talent tree portrait and PR04 Talent Assign | does the tree portrait complement the Talent Assign UI without adding rule text or diagrams? |

If a pair fails, the fix belongs in prompt/art direction or asset QA. Do not patch it by renderer-side recoloring unless the owning PR explicitly changes renderer tokens and tests.

## 10. Open Design Review Prompts

Use Open Design to ask review questions like:

```text
Review this PR05 contact sheet against K-ToME dark UI rules. Focus on silhouette, 32px readability, category perspective, no baked text, and consistency with forged iron/worn stone/ember/cyan material language. Do not propose new manifest keys or sheet mappings.
```

```text
Compare these actor and telegraph references for gameplay overlay safety. The telegraph must remain readable when a dark actor occupies the same cell, without becoming a filled neon effect. Return revise/reject notes, not implementation changes.
```

```text
Suggest prompt wording variants for the same existing PR05 sheet family. Keep output compatible with sheet-plan-driven generation: no new cells, no new target keys, no raw paths, no baked labels.
```

## 11. Handoff To The Real Pipeline

After a design or QA note is accepted:

1. Translate it into the owning PR05 prompt-generation or QA wording.
2. Keep all sheet ids, cells, target keys, output names, and categories in `sheet-plan.yaml` / owner inventory.
3. Generate raw sheets only through the scripted prompt flow.
4. Review contact sheets before cutting runtime PNG.
5. Update canonical manifest first.
6. Sync runtime manifest.
7. Run owner-scope coverage and evidence gates.

Do not close PR05 on "looks good in Open Design." PR05 closes only when the K-ToME pipeline proves the resource set is mapped, cut, manifested, covered, and visible in runtime evidence.

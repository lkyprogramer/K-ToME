# Dark UI/UX PR06 Skills Status Quest Manifest Design Notes

## 1. Status And Authority

This document is an Open Design helper for `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`.

It is auxiliary design input only. It helps reviewers and asset authors reason about same-screen visual consistency across status, skill, quest, fallback, hidden, debug, and validation-overlay visuals. It is not a manifest, sheet-plan, golden, or coverage contract.

Hard authority boundaries:

| Topic | Authority | This file may help with | This file must not do |
| --- | --- | --- | --- |
| PR06 scope, stage, stop conditions, final-full coverage | `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md` | restate the visual quality problem | change final-full close gate, owner scope, sheet ids, or evidence requirements |
| Screen labels and evidence coverage | `UI/pr/screen-coverage-matrix.md` | explain what the PR06 overview sample should prove visually | add or rename canonical golden/manual labels |
| Dark style | `UI/ART_STYLE_BIBLE.md` and `UI/review/open-design/ktome-dark-ui-design.md` | apply derived color, material, spacing, and anti-pattern language | redefine `ktome-dark-fantasy-sprite-ui-v1` or palette authority |
| Sheet mapping | `UI/sprite-sheets/sheet-plan.yaml` | provide prompt-fragment and QA guidance for existing families | define `sheetId`, `row`, `col`, `targetKey`, `outputName`, inventory keys, or raw sheet paths |
| Resource truth | canonical manifest, runtime manifest, final-full inventory, and PR06 coverage reports | remind reviewers which visual problems coverage cannot catch | invent manifest keys, fallback rules, exclusions, or coverage exceptions |
| Runtime behavior | `client` presenters/renderers and PR06 implementation | describe desired readability and state separation | introduce UI state, localization, resolver behavior, or validation-overlay logic |

Final resource flow remains:

```text
sheet-plan.yaml -> generated prompt -> raw sheet -> contact sheet QA -> cut/runtime PNG -> canonical manifest -> sync runtime manifest -> final-full coverage
```

Open Design output can guide critique at the left side of that flow. PR06 can only close through the authoritative pipeline and gates.

## 2. PR06 Design Job

PR06 is the point where dark UI/UX moves from individual screen replacement to full manifest completeness. The design risk changes:

1. A single icon can look good alone but break the era when placed next to status badges, quest markers, and fallback visuals.
2. Coverage can prove every key exists while the combined screen still reads like mixed icon packs.
3. Fallback or missing visuals can become either too polished to signal a problem or too crude to belong in the dark-v1 UI.
4. Validation-overlay warnings can technically render while visually fighting the game HUD.
5. Skill, talent, quest, status, and debug icon families can collapse into the same shape language if reviewed separately.

Useful PR06 design evidence is therefore a unified same-screen sample, not only contact sheets by family.

Target statement:

```text
A PR06 overview sample should show status + skill + talent/tree + quest marker + fallback/missing/debug visual language in one dark-v1 frame, proving that the manifest is visually coherent after final-full coverage.
```

## 3. Same-Screen Unified Sample

The recommended PR06 Open Design sample is a single review composition that approximates the `dark-uiux-pr06-status-quest-skill-overview` evidence goal without becoming that evidence.

### 3.1 Required Content

Include all of these in one frame:

| Area | Visual content | What it proves |
| --- | --- | --- |
| Status strip | active buff, debuff, damage/status condition, timer or stack badge | state badges remain readable and do not look like skill icons |
| Skill row | at least one active skill icon and one unavailable or cooldown-like skill state | action icons retain subject identity at compact size |
| Talent/tree row | one learned/active, one learnable, one locked or unavailable node treatment | tree state grammar remains distinct from skill command grammar |
| Quest marker | objective marker or quest summary icon near map/HUD text | quest language is directional/objective-focused, not status-like |
| Profession/tree identity | class or tree icon adjacent to skill/talent context | profession family matches skill and talent era |
| Fallback/missing visual | visible fallback or missing visual placeholder in a non-primary slot | fallback is deliberate, readable, and dark-v1, but not mistaken for approved content |
| Hidden/debug visual | subtle hidden/secret/debug marker if represented by PR06 scope | internal/state aid visuals do not become player-facing hero art |
| Validation warning | a compact warning row or badge concept | validation feedback is visually bounded and does not overwhelm gameplay information |

The sample may use annotation labels outside the art when used in a review document. Source image cells must not contain baked text, numbers, hotkeys, manifest keys, or labels.

### 3.2 Layout Grammar

Use a dense operational layout:

```text
top status strip
left or center skill/talent cluster
right quest summary marker
small fallback/debug strip or validation-overlay segment
map or dark panel background behind the set
```

Rules:

1. The first read should be gameplay state, not decorative ornament.
2. Status icons and quest markers must stay smaller and quieter than primary action/skill icons unless the state is blocking.
3. Fallback/missing visuals should be visible but not rewarded with rare-item framing.
4. Validation warning rows should be bounded and calm; they are evidence and debug surfaces, not a second HUD.
5. The sample must still be legible if downscaled to the canonical evidence size used by PR06.

### 3.3 What The Sample Must Catch

The unified sample should reveal:

1. skill icons that look like status badges;
2. quest markers that look like damage type icons;
3. fallback images that look like real rewards;
4. debug/hidden icons that read as official quest objectives;
5. too many cyan glows competing for focus;
6. status badges losing their outline at 32px;
7. inconsistent perspective between icon families;
8. old bright/vector/painterly cells still leaking into player-visible UI.

## 4. Icon Taxonomy

PR06 needs visual families that share the dark-v1 era while remaining functionally distinct.

| Family | Primary read | Preferred shape language | Accent budget | Must not be confused with |
| --- | --- | --- | --- | --- |
| Skill icons | player action or ability subject | weapon, hand, spell core, stance, compact emblem | one domain accent plus small edge light | status badge or passive talent state |
| Talent/tree icons | progression identity and node subject | crest, branch, rune node, class motif | restrained cyan/gold/green based on state frame, not icon subject alone | skill command button |
| Status icons | active condition affecting actor | simple condition symbol, wound, shield, flame, frost, curse | small state color and strong silhouette | quest marker or skill icon |
| Damage/status type icons | damage channel or condition type | elemental or physical silhouette with hard outline | color tied to type but low saturation | active effect badge |
| Telegraph (enemy intent) | enemy action preview or danger cue | ranged arc, aim line, impact marker, directional strike shape anchored at actor/map tile | danger-linked ember/red/violet restraint; not focus cyan | player buff/debuff, selected row, or status HUD item |
| Quest markers | objective and direction | marker, sigil pin, banner, route glyph, objective frame | ember/gold or muted cyan only when active | status icon, map prop, debug marker |
| Profession/tree identity | class family and tree branch | class crest, weapon/tool motif, tree emblem | domain accent, not rare-item glow | quest marker or reward icon |
| Talent portrait visual | larger talent identity or narrative visual | half-figure, scene fragment, mood lighting, larger crest composition | subdued narrative lighting; no command-button frame | enlarged skill icon |
| Frozen profession panel header | dev/debug-only frozen profession context | muted crest or fallback frame with dev-only marker | gray/iron with restrained warning edge | playable profession identity |
| Fallback/missing visuals | absent, unresolved, or intentionally missing visual | cracked placeholder, empty frame, masked eye, broken rune | warning-muted red/gold edge, no hero polish | approved gameplay content |
| Hidden/secret/debug visuals | special/internal visibility state | veiled sigil, low-contrast marker, debug-safe outline | quiet cyan or gray, never primary gold | quest objective or reward |
| Validation overlay icons | report/warning state | row marker, compact alert, coverage/fallback badge | red/gold for severity, gray for metadata | gameplay status |

### 4.1 Shared Era Requirements

All families should keep:

1. charcoal or transparent-safe negative space;
2. forged iron or worn stone rim language when framed;
3. low saturation and controlled highlights;
4. consistent light direction;
5. clear silhouette at `32x32`;
6. no clean vector sticker look;
7. no bright mobile-RPG rarity frame unless the owning surface explicitly needs rarity.

Outline/silhouette pixel budget:

1. At 32px source resolution, primary outline should be at least 2px and the main silhouette should occupy at least 60% of the bounding box.
2. Sub-32px runtime paths must preserve at least a 1px readable outline after downscale; cells that rely on soft antialiasing instead of hard outline must be regenerated or given a size-specific variant.
3. Contact-sheet QA must include the runtime scales named by the PR06 sheet contract, not only the 32px reference.

### 4.2 Separation Rules

Use shape first, color second:

| Pair | Required separation |
| --- | --- |
| Skill vs status | skill is an action subject; status is a condition badge with simpler symbol and tighter frame |
| Skill vs talent | skill reads as usable action; talent reads as progression node or passive identity |
| Quest vs status | quest marker carries direction/objective grammar; status carries actor condition grammar |
| Status vs mutation | status uses transient/open condition badge language; mutation uses closed scar/etching or permanent body-change framing |
| Status vs damage type | status reads as active actor condition; damage type reads as elemental/physical channel with hard outline and no timed badge frame |
| Mutation vs damage type | mutation reads as permanent body-change/scar; damage type reads as reusable combat channel symbol |
| Telegraph vs status | telegraph reads as enemy intent / impact preview; status reads as actor-owned condition |
| Quest vs fallback | quest marker is intentional objective; fallback is intentionally unresolved or missing |
| Debug vs player-facing | debug/hidden marker is subdued and utility-like; player-facing markers get clearer hierarchy |
| Talent state vs focus | learned/learnable/locked state must remain visible when focus highlight is present |

## 5. State Badge Guidance

State badges should be reviewable as a system, not as one-off overlays.

| State role | Visual rule | Rejection trigger |
| --- | --- | --- |
| Active beneficial | green or ember accent plus stable icon frame | reads as selected/focused instead of active |
| Harmful or blocking | red/blood edge with strong badge shape | becomes a full red tile or unreadable smear |
| Timed or stacked | tiny attached badge area, stable dimensions | number/stack marker forces source art to contain text |
| Locked/unavailable | gray/iron lock treatment, readable subject retained | subject becomes invisible or looks disabled by accident |
| Learnable | cyan affordance separated from focus ring | learnable and focused become the same glow |
| Learned active | green/state marker distinct from selection | selected row is mistaken for learned state |
| Quest active | objective marker with warmer route/goal accent | looks like damage/status icon |
| Quest activate | brief ember confirmation, shorter than complete, then muted marker | reads the same as progress or steals rare-item attention |
| Quest advance | short row pulse without ember/cyan accent | reads as completion or generic progress |
| Quest complete | short ember-gold completion accent, then returns to muted objective marker | completion reads the same as activate/progress or becomes rare-item glow |
| Cooldown | desaturated skill icon plus ring/badge overlay showing remaining turns | skill disappears, becomes unreadable, or looks locked forever |
| Missing/fallback | broken/empty placeholder with warning restraint | too polished, too bright, or too crude for dark-v1 |
| Mutation | closed frame, scar/etching, body-change motif | reads as a temporary buff/debuff or removable status |
| Damage type | elemental/physical silhouette with hard outline | reads as an active status, skill cooldown, or quest marker |

Do not require these exact state names in implementation. The point is to preserve visual separation for whatever typed states PR06 already owns.

### 5.1 Fallback, Missing, Hidden, And Debug Semantics

These four buckets must not collapse into one visual language:

| Bucket | Required read | Visual rule |
| --- | --- | --- |
| `missing_visual` | unresolved development fallback | include text-marked or unmistakable sentinel treatment; do not rely on color alone |
| fallback visual | deliberate replacement path | restrained warning treatment, reportable through manifest/fallback evidence |
| hidden / secret | gameplay discovery | dark-fantasy valid secret language, not a broken placeholder |
| debug-only | development utility | subdued utility/debug treatment; packaged app should not expose it as player content |

Dark fantasy ornament can make cracked frames and broken runes look like approved content. When the intended meaning is "missing" or "debug", use a secondary marker or textual sentinel in the runtime-facing resource/evidence path rather than expecting visual mood alone to carry the contract.

### 5.2 Accent Priority

Same-screen reviews should treat ember-gold, cold-cyan, and danger telegraph tones as attention budget, not decoration. Ember quest markers, rare/reserve states, and titles should not all compete at full strength in one frame; cyan focus/learnable state must remain distinct from actor-anchored danger telegraph indicators. If a PR06 overview sample shows more than four strength-2-or-higher attention cues at once, send it back to sheet QA or tone balancing before cutting runtime cells.

## 6. Prompt Variant Kit

These fragments are for design exploration and review. Final prompts must still be generated from `UI/sprite-sheets/sheet-plan.yaml` and the PR06 resource pipeline.

### 6.1 Global Prefix

```text
Style tag: ktome-dark-fantasy-sprite-ui-v1.
Dark fantasy roguelike tactical UI icons, charcoal black, forged iron, worn stone, old leather, ember highlights, restrained cyan rim light, low saturation, strong readable silhouette at 32px, centered subject, transparent background, no text, no numbers, no labels, no watermark.
```

### 6.2 Variant A: Unified PR06 Overview Sample

Use for a review mockup that places all PR06 visual families together.

```text
Single dark roguelike UI review frame showing a compact status strip, skill icons, talent/progression nodes, quest objective marker, fallback missing-visual placeholder, hidden/debug marker, and a small validation warning strip. All elements share forged iron, worn stone, ember, muted red, green, and restrained cyan accents. Dense operational UI, readable hierarchy, no marketing layout, no decorative clutter.
```

Trade-off: best for consistency review, but not suitable as a source sheet prompt because it mixes many categories.

### 6.3 Variant B: Status And Damage Readability

```text
Compact condition icons and damage/status badges for a dark roguelike HUD. Simple silhouette first, type identity second, small stable frame, readable at 32px, no detailed painting, no full UI button chrome, no quest-marker shape.
```

Use when status icons become too painterly or too similar to skill icons.

### 6.4 Variant C: Skill And Talent Separation

```text
Skill icons read as usable actions with a clear subject: weapon strike, guarded stance, spell core, holy mark, shadow cut, or tactical movement. Talent icons read as progression emblems or node subjects. Keep both in the same dark-v1 era, but separate action-command language from progression-node language.
```

Use when skills and talents collapse into one generic rune pack.

### 6.5 Variant D: Quest And Profession Markers

```text
Quest and profession icons for a dark tactical roguelike UI. Quest markers use objective, route, banner, or sigil-pin language. Profession and tree icons use class crests and branch motifs. Keep markers compact, low saturation, no text, no map arrow labels, no bright mobile-game quest stickers.
```

Use when quest markers look like status effects or reward icons.

### 6.6 Variant E: Fallback, Missing, Hidden, Debug

```text
Fallback and missing-visual placeholders in dark-v1 style: cracked iron frame, empty shadow center, broken rune, muted warning edge, clearly unresolved but still polished enough to belong in the UI. Hidden and debug markers are subdued utility symbols, not hero rewards, not quest objectives, not colorful production icons.
```

Use when fallback cells are either too ugly for the UI or too polished to signal missing content.

### 6.7 Variant F: Validation Overlay Compatibility

```text
Compact validation overlay row markers for dark UI: bounded warning strips, small alert badges, readable metadata area, red or ember only for severity, gray for secondary data, no full-screen alarm styling, no noisy background texture.
```

Use when validation or fallback warning presentation competes with the player HUD.

## 7. Contact-Sheet QA Rubric

Use this rubric before accepting raw cells into the cut/manifest path.

| Check | Pass | Revise | Block |
| --- | --- | --- | --- |
| Taxonomy | family identity is obvious without labels | family is readable only with nearby context | skill/status/quest/fallback are interchangeable |
| Status / mutation / damage separation | transient badge, body-change marker, and channel symbol stay distinct | two families need nearby context | all three collapse into the same small badge |
| Cross-sheet family weight | r08 skill, r09 status/mutation, and r09 damage cells keep distinct silhouette weight side-by-side | one family pair needs redraw | cross-sheet cells look like a single generic icon family |
| 32px readability | subject silhouette survives at 32px | major subject survives but detail is noisy | icon becomes a blob, smear, or random texture |
| Sub-32px readability | 12/16/24px runtime samples preserve family identity and outline | one runtime size needs outline reinforcement | tooltip/log/floating-text size collapses into unreadable noise |
| Same-era consistency | dark-v1 materials and lighting match nearby families | minor accent or perspective drift | looks like another game, era, or art source |
| State badge clarity | state is separate from focus/selection | state works only at large size | state can be mistaken for focus, rarity, or quest marker |
| Quest marker function | objective/direction read is clear | marker is clear but too similar to UI badge | marker looks like status, damage type, reward, or debug |
| Fallback semantics | unresolved state is clear and restrained | too quiet or slightly too polished | looks like approved content or broken placeholder from another era |
| Hidden/debug restraint | utility state is visible but secondary | too subtle or slightly too bright | becomes primary objective/reward visual |
| No baked text | no letters, numbers, labels, keys, watermark | suspicious mark but not text-like | contains readable text, hotkey, manifest key, or label |
| Sheet safety | single centered subject, safe margins | margin or crop needs cut attention | subject spills, multiple merged subjects, or background artifact |
| Old-style residue | no bright/vector/painterly legacy cell | one cell needs side-by-side check | obvious cross-era collage risk |

PR06 acceptance should prefer contact-sheet comments that identify the exact taxonomy failure, not comments like "make it better."

## 8. Same-Screen Review Checklist

When reviewing the unified PR06 sample, answer these in order:

1. Can a player separate status condition, skill action, talent progression, and quest objective within one glance?
2. Does any family require text labels to be understood?
3. Does focus/selection visually override learned/locked/active state?
4. Does fallback/missing visual clearly mean "not normal content" without looking out of place?
5. Does any debug/hidden marker look like a main objective or reward?
6. Are cyan, gold, red, green, and violet accents role-bound instead of sprayed everywhere?
7. Are icons readable at `32x32` and still coherent at the evidence screenshot scale?
8. Is the validation warning surface bounded and secondary?
9. Are there any old-style bright, vector, painterly, beige, or mobile-RPG cells mixed into dark-v1?
10. Would final-full manifest coverage passing still leave a visible design mismatch? If yes, send it back to sheet QA before cutting.

## 9. PR06 Pipeline Handoff

Open Design can produce:

1. same-screen visual critique notes;
2. prompt fragment variants;
3. contact-sheet reject reasons;
4. side-by-side comparison references;
5. taxonomy language for reviewer comments.

Open Design must hand off before authority begins:

```text
design critique -> PR06-owned sheet-plan prompt generation -> raw sheet -> contact sheet QA -> cut -> manifest -> runtime sync -> final-full coverage -> golden/manual evidence
```

PR06 implementation should still prove:

1. final-full inventory is the expected key source;
2. final-full coverage has no player-visible old-style cells;
3. fallback and missing visuals are registered through the official fallback path;
4. quest summary consumes the typed quest marker path, not text-only rendering;
5. status icon resolution reports unknown or missing visuals instead of silently dropping them;
6. validation-overlay long lists remain bounded and readable;
7. `dark-uiux-pr06-status-quest-skill-overview` shows status, skill/talent, and quest marker together;
8. PR07 receives a clean enough surface for final screen-wide review.

## 10. Open Design Critique Prompts

Use these prompts to review generated candidates or mockups. They are not final image prompts.

### 10.1 Taxonomy Critique

```text
Review this PR06 dark-v1 UI sample for taxonomy clarity. Identify any icon that could be mistaken for another family: skill, talent/tree, status, damage type, quest marker, profession/tree, fallback/missing, hidden/debug, or validation warning. For each issue, name the visual cause and the smallest correction.
```

### 10.2 Same-Era Critique

```text
Review this PR06 same-screen sample for dark-v1 era consistency. Look for cross-era collage signals: bright vector sticker art, painterly hero-icon lighting, beige parchment drift, clean mobile-game rarity frames, mismatched outline weight, inconsistent perspective, or overuse of cyan/gold/red. Return only concrete visual mismatches.
```

### 10.3 Fallback Semantics Critique

```text
Review the fallback/missing/hidden/debug visuals in this PR06 sample. Do they clearly signal unresolved or utility state while still belonging to the same dark-v1 UI? Flag any visual that looks like approved gameplay content, quest objective, reward, or a broken out-of-era placeholder.
```

### 10.4 Coverage-Risk Critique

```text
Assume the manifest coverage report is green. What visible design problems in this PR06 sample would still make the player experience feel unfinished or inconsistent? Focus on hierarchy, family identity, state badge clarity, and fallback readability.
```

## 11. Non-Goals

This file does not:

1. create or rename PR06 golden labels;
2. define final-full inventory contents;
3. define new target keys or fallback exclusions;
4. replace contact-sheet QA;
5. replace `darkManifestCoverageLint final-full`;
6. replace `goldenScreenshot`, `clientSmoke`, or PR06 manual evidence;
7. permit direct runtime resource edits outside the established resource pipeline;
8. authorize any player-visible missing/rejected cell to ship because it looks acceptable in Open Design.

## 12. Difficulty Visual Roadmap

Future `difficulty.<id>.icon` keys should remain inside the dark-v1 era. Difficulty escalation should use material complexity and accent shape, not brighter red, heavier gore, or mobile-RPG shine.

Suggested progression language:

| Difficulty tier | Visual escalation |
| --- | --- |
| normal | plain worn stone or iron mark |
| hard | single crack, thorn, or edge notch |
| nightmare | cursed iron, ember crack, or double-spike motif |
| madness / insane | ringed or fractured sigil with controlled accent; still low saturation |

This is guidance for later design review only. It does not add PR06 keys or change the manifest contract.

Contrast budget for future difficulty tiers:

1. Across all difficulty tiers, total contrast delta must stay within one stop (roughly 1:4 luminance ratio).
2. Silhouette and outline weight must remain a dark-v1 era constant inside the same tier.
3. Escalation uses accent shape, fracture, ring, thorn, or material detail; it must not use hue rotation or saturation escalation as the primary difficulty signal.

`difficulty.<id>.icon` key introduction condition: only add a new difficulty key when `core` or `game` introduces a new player-visible difficulty value and that value appears in character selection, dungeon entry, validation setup, or another player-visible surface. Do not pre-create unused difficulty icon keys.

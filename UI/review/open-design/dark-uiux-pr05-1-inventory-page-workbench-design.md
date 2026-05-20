# Dark UI/UX PR05-1 Inventory Page Workbench Design Notes

## 1. Status And Authority

This document is an auxiliary design helper for `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`.

It is not an implementation contract. It helps reviewers and implementers discuss the inventory workbench's player-facing hierarchy, readability, state language, and evidence quality before the PR is closed.

It must be read together with `UI/review/open-design/ktome-dark-ui-design.md`, which summarizes the original dark UI/UX design requirements, color roles, density rules, component states, anti-patterns, and Open Design authority boundaries.

Hard authority boundaries:

| Topic | Authority | This file may help with | This file must not do |
| --- | --- | --- | --- |
| PR05-1 scope, gates, artifacts, failure rules | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` | clarify review intent and player-facing design risks | change requirements, gates, stop rules, golden/manual evidence, or rollback boundary |
| Dark UI sequence and screen ownership | `UI/pr/README.md`, `UI/pr/screen-coverage-matrix.md` | keep inventory review aligned with the PR sequence and screen matrix | add, rename, or remove owner PRs, labels, or coverage requirements |
| Original dark UI design requirements | `UI/PLAN.md`, `UI/ART_STYLE_BIBLE.md`, `UI/review/open-design/ktome-dark-ui-design.md` | reuse density, material, color, and anti-pattern language | redefine `ktome-dark-fantasy-sprite-ui-v1`, palette, style rules, or redesign scope |
| Reference image status | `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md` and `UI/dark-uiux-pr03-inventory-page-reference.png` | explain which visual lessons are reviewable | turn the reference image into runtime asset, manifest truth, item rule truth, or locale truth |
| Runtime behavior | `client` presentation, render, input, locale, golden, and smoke code owned by the PR | describe player-facing expectations to review | compute rules, create a second inventory authority, or infer rules from localized text, icon keys, asset paths, or image content |
| Resource and manifest truth | `UI/sprite-sheets/sheet-plan.yaml`, `UI/sprite-sheets/key-registry.yaml`, canonical/runtime visual manifests, and resource pipeline gates | remind reviewers that PR05-1 should not create formal runtime resources | create keys, sheet rows, manifest entries, fallback rules, or resource pipeline exceptions |

If this file conflicts with any authority above, ignore this file and fix the authoritative document or implementation instead.

## 2. Why This Helper Exists

PR05-1 has meaningful design ambiguity even though it does not generate formal runtime assets.

The PR turns an unusable large inventory modal into a full-screen inventory workbench. The hard part is not copying the reference image; it is preserving K-ToME's dark run shell while making selection, equipment sockets, a 6x4 grid, details, comparison rows, actions, pagination, empty state, and footer help readable at gameplay density.

This helper exists to keep design review focused on those interpretation risks:

1. whether the workbench still feels inside the current run instead of a detached debug tool;
2. whether the player can identify selection, focus, hover preview, target slot, available action, and disabled action without guessing;
3. whether visual-only equipment sockets are clearly non-rules and do not imply new typed equipment slots;
4. whether screenshots and manual evidence prove actual UI semantics rather than only showing a polished frame;
5. whether the reference image is used as layout and hierarchy input, not as item stats, resource mapping, or text source.

## 3. PR Design Job

The workbench should let the player answer these questions in one glance:

1. Which inventory cell is committed selection, and which cell is only hover or keyboard focus.
2. What the selected item is, what typed slot or item family it belongs to, and what actions are currently available.
3. What currently equipped item would be affected, if the selected item can equip into an existing typed slot.
4. How much of the visible page is real content, empty capacity, or disabled visual affordance.
5. How to move, equip or use, drop, change page, and return without leaving keyboard-first flow.

The design job is therefore:

```text
Create a dense dark-fantasy inventory workbench that keeps the run shell present, makes the 6x4 backpack grid the main operational surface, and exposes typed detail, compare, action, pagination, and empty states without inventing rules or resources.
```

## 4. Review Axes

| Axis | What to check | Failure signal |
| --- | --- | --- |
| Run context | The map shell, left rail, and bottom HUD remain low-light context behind or around the workbench | inventory reads as a separate debug application or covers all run identity |
| Three-column hierarchy | Equipment, backpack grid, and detail/compare panes have distinct roles and stable visual weight | the detail pane competes with the grid, or equipment sockets look like unrelated cards |
| Selection grammar | Committed selection, keyboard focus, hover preview, target cue, and disabled state are distinguishable | hover appears to overwrite selection, or focus/learnable/rare/target cues all use the same glow |
| Grid density | The 6x4 grid has fixed cell dimensions, visible empty cells, stable overlays, and readable item icons | badges, focus ring, long names, or pagination resize cells or hide item identity |
| Equipment socket honesty | Visual-only sockets look intentionally disabled and never imply new rules | locked visual sockets look equip-ready, comparable, or like future gameplay already exists |
| Detail and compare truth | Detail, compare, and action rows only express typed presentation data | renderer appears to infer rule meaning from localized text, icon key fragments, filenames, or reference-image sample stats |
| Footer usability | Key hints, page/capacity text, and return path stay visible and compact | footer overlaps the grid, disappears under detail rows, or makes `D` look like movement |
| Dark-v1 cohesion | Material, color, and contrast follow `ktome-dark-fantasy-sprite-ui-v1` | sci-fi HUD lines, neon fills, glassmorphism, bright mobile rarity frames, or beige parchment dominate |
| Evidence usefulness | Golden/manual evidence shows meaningful states and viewport constraints | screenshots are visually attractive but do not prove compare rows, pagination, empty slot, disabled socket, or no-overlap behavior |

## 5. Evidence And QA Checklist

| Evidence area | Must prove | Failure if |
| --- | --- | --- |
| Full workbench | Three-column layout, shell context, grid, details, footer, and stable density all coexist | the screenshot only proves a static mock frame or hides the underlying run context |
| Equippable comparison | Selected equippable item shows current equipment target cue and typed compare rows | comparison contains fake sample values, unsupported deltas, or a visual-only socket target |
| Non-equippable item | Consumable, material-like, key-like, or similar item shows valid action rows without equip affordance | every item gets the same equip path or unsupported compare pane |
| Empty state | Empty cells and empty selection state are visible and calm | empty space looks broken, clickable as real item content, or filled with fake tooltip text |
| Pagination | More-than-one-page inventory shows page count, stable focus coordinate, and capacity text | page changes reorder identity ambiguously or hide selected/empty state |
| Min-window | Compact viewport keeps footer, grid, selected cell, and detail text legible | long zh-CN or en-US text overlaps, clips critical action labels, or pushes footer offscreen |
| Keyboard path | Manual evidence records open, move, select/equip-or-use, drop state, page, and return | evidence only uses mouse hover or cannot prove map movement is blocked |
| Reference exclusion | No weight, burden, filter, search, top economy strip, or unsupported real slot appears | reference image details leak into production UI without typed source |
| Style anti-patterns | No sci-fi HUD, neon flood, glassmorphism, baked text in icons, or overbright rare framing | dark-v1 style is present only in colors while layout and affordance language drift |

## 6. Critique Prompts

Use these prompts for review of screenshots, manual evidence, or Open Design mockups. They are critique prompts, not implementation prompts.

```text
Review this inventory workbench evidence against PR05-1. Focus on whether committed selection, keyboard focus, hover preview, target cue, disabled action, and pagination are visually distinguishable without adding new gameplay rules.
```

```text
Check whether the left equipment area honestly separates existing typed equipment slots from visual-only disabled sockets. Flag any socket that looks equip-ready, comparable, or rule-authoritative without a typed source.
```

```text
Evaluate whether the 6x4 grid, detail/compare pane, and footer remain readable at standard and compact viewport sizes. Flag any text overlap, clipped long item name, badge collision, or focus ring that changes layout.
```

```text
Compare the workbench against the PR05-1 reference image contract. Identify useful layout or hierarchy lessons, and separately list any reference-only burden, filter, economy, example stat, or fake text detail that must not enter implementation.
```

```text
Review the evidence for dark-v1 cohesion. Flag sci-fi HUD treatment, high-saturation neon, glassmorphism, baked text, over-decorated cards, or a palette that makes every state look rare or selected.
```

## 7. Ownership And Pipeline Reminder

PR05-1 is a `client/docs` UI workbench PR. It does not declare a new Dark UI sprite `ownerPr`, does not generate formal runtime assets, and must not modify `sheet-plan.yaml`, `key-registry.yaml`, canonical atlas intent, or visual manifests just to satisfy the workbench design.

The reference PNG and prompt are development inputs only:

1. use them for layout, density, material, focus-ring, and hierarchy discussion;
2. do not load them from runtime Kotlin;
3. do not derive item stats, equipment slots, action availability, locale text, icon keys, or manifest keys from them;
4. if any visual element later becomes a formal runtime resource, route it through the normal sheet plan, source sheet, contact sheet, manifest, sync, and resource lint pipeline in a separate scoped change.

For implementation review, prefer typed presentation ownership:

1. one workbench presentation model should own grid, selection, pagination, detail, compare, action, and footer data;
2. renderer should consume display rows and states, not reconstruct rule meaning;
3. input should keep hover preview separate from committed selection;
4. evidence should prove behavior from real scenario state, not a static copy of the reference image.

## 8. Non-Goals

This file does not:

1. add inventory sorting, filtering, search, drag-and-drop, stash, crafting, salvage, or economy UI;
2. add item weight, burden, carry capacity, or new equipment slot rules;
3. define item stats, loot, shop, save, replay, profile, content pack, or manifest contracts;
4. add or rename golden labels, manual record fields, whitebox scenarios, Gradle gates, or acceptance requirements;
5. create resource keys, sheet rows, visual manifest entries, raw image paths, or fallback rules;
6. override `UI/pr/dark-uiux-pr05-1-inventory-page-workbench.md`, `UI/pr/README.md`, `UI/pr/screen-coverage-matrix.md`, `UI/PLAN.md`, `UI/ART_STYLE_BIBLE.md`, resource pipeline authorities, implementation tests, golden/manual evidence, or whitebox records.

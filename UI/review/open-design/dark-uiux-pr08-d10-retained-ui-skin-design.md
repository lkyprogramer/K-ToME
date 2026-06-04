# Dark UI/UX PR08 D10 Retained UI Skin Design Notes

## 1. Status And Authority

This document is an auxiliary Open Design helper for `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md`.

It exists to answer one design question before implementation starts:

```text
Which parts of the D10 retained UI migration can proceed with existing assets,
and which parts likely need new or re-sliced dark-v1 Skin resources to reach the UI/UI-demo-new.png quality bar?
```

It must be read together with `UI/review/open-design/ktome-dark-ui-design.md`, which summarizes the dark UI/UX style goal, color roles, density rules, component states, anti-patterns, and Open Design authority boundaries.

This helper is not an implementation contract and not a resource contract.

Hard authority boundaries:

| Topic | Authority | This file may help with | This file must not do |
| --- | --- | --- | --- |
| D10 scope, phase gates and runtime migration | `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md` | clarify visual/resource decision criteria for D10 implementation | change D10 phase order, gates, evidence, dependencies or stop rules |
| Dark UI style language | `UI/review/open-design/ktome-dark-ui-design.md`, `UI/ART_STYLE_BIBLE.md` | translate the dark-v1 material language into retained UI review guidance | redefine the dark-v1 era or palette |
| Resource pipeline | `UI/pr/README.md`, `UI/sprite-sheets/key-registry.yaml`, `UI/sprite-sheets/sheet-plan.yaml`, canonical/runtime manifests and resource lint | identify when a component probably needs a formal resource | create keys, sheet rows, manifest entries, raw paths or fallback rules |
| Runtime UI behavior | `client` implementation, focused tests, golden/manual evidence and packaged whitebox | describe player-facing quality and review risks | override presenter, input, gameplay, snapshot, validation or whitebox authority |

If this helper conflicts with a real authority above, ignore this helper and fix or revise it.

## 2. Does D10 Need New Art Resources?

Short answer:

```text
D10 does not need new art resources to start the retained UI architecture.
D10 does need a resource/design decision path if it is expected to close the UI/UI-demo-new.png texture gap.
```

The retained UI migration solves layout, state, focus, modal, tooltip and style reuse. It does not automatically create dark-fantasy material quality. If D10 only introduces Scene2D tables and default-looking widgets, it moves the architecture forward but does not reach the PR-08 director-grade visual bar.

Therefore:

1. `D10-P0` and `D10-P1` should not generate formal resources.
2. `D10-P2` can migrate standalone screens using existing dark-v1 panel/control assets if they are sufficient.
3. `D10-P3` through `D10-P7` should review whether each retained widget family needs new or re-sliced Skin drawables.
4. Any accepted new resource must go through the existing PR-08 resource owner route. This helper never authorizes direct PNG drops, Skin JSON truth, atlas path truth or manifest shortcuts.

## 3. PR Design Job

D10's design job is not to invent a second visual era. It is to make the existing dark-v1 direction enforceable through retained UI components:

```text
The player should see one dense dark tactical interface whose shell, right panel,
bottom deck, modal, tooltip, inventory, talent and action surfaces share the same
material language and interaction states.
```

The reviewer should be able to answer:

1. Are Stage/Table/Skin surfaces structurally stable?
2. Are component states visible without hand-written renderer branches?
3. Do retained widgets consume dark-v1 material assets instead of generic Scene2D defaults?
4. Is any new art request justified by a concrete surface/state gap?
5. Does the evidence prove runtime texture quality, not only actor-tree correctness?

## 4. Resource Need By D10 Phase

| Phase | Can start without new art? | Likely resource/design need | Decision rule |
| --- | --- | --- | --- |
| `D10-P0` authority freeze | yes | none | docs-only; no runtime resources |
| `D10-P1` Stage/Skin kernel | yes | test-only in-memory/generated fixture drawables, or existing resolver-backed drawables | no committed/generated repo assets in P1; prove ownership and state wiring first |
| `D10-P2` standalone screens | mostly yes | shared panel body, title band, action button states, footer/keycap treatment | generate only if existing dark-v1 chrome cannot support readable main menu/validation/outcome states |
| `D10-P3` focus/modal/tooltip | maybe | focus ring, modal panel, tooltip panel, disabled/blocked state overlays | new resources are justified if color-only or default Scene2D states fail keyboard/modal readability |
| `D10-P4` retained shell and map stage | likely no for final quality | map-stage frame, right panel frame, bottom deck recess, nav rail background, shell separators | treat this as the first serious resource decision point; generic tables cannot close director-grade shell texture |
| `D10-P5` inventory/equipment/shop | likely no for final quality | equipment slots, inventory cells, selected/hover/disabled states, stack badge, item detail panel, compare row accents | new/re-sliced assets are justified when state density or repeated cells look flat, rare-looking, or inconsistent |
| `D10-P6` talent tree | likely no for final quality | node states, connector segments, learned/locked/available markers, active slot modal chrome | new resources are justified if node hierarchy cannot be read at dense scale |
| `D10-P7` combat/frontstage/bottom/nav closure | maybe | action buttons, command deck pads, reward/frontstage card frames, log deck dividers | generate only for remaining surfaces whose packaged evidence still looks generic |
| `D10-P8` dead-route removal | no | only targeted polish deltas already justified by P4-P7 evidence | do not use final cleanup as a broad new art batch |

## 5. Retained Skin Taxonomy

This taxonomy is design vocabulary, not a manifest key list.

| Family | Required states | Visual quality bar | Resource risk |
| --- | --- | --- | --- |
| shell panels | base, framed, recessed, active host | forged iron edge, charcoal body, subtle worn texture, no flat gray cards | flat default panels make the retained shell look like a debug UI |
| map-stage frame | base, focused, targeting/inspection emphasis | central map stays dominant; frame supports first-read without covering tile/fog/telegraph | overdecorated frame steals attention from map contents |
| buttons/actions | normal, hover, focused, pressed, disabled, destructive/confirm | compact dark material with clear state delta and keyboard affordance | color-only state fails accessibility and keyboard review |
| slots/cells | empty, filled, selected, hover preview, disabled, quality/rarity emphasis | repeated cells stay dense and legible; selected is clear without looking like every item is rare | gold/cyan overuse collapses item state language |
| tooltip/modal | normal, blocking modal, warning, compare/detail | layered above shell without looking pasted on; text remains primary | ornamental corners reduce readability |
| talent nodes/connectors | locked, learnable, learned reserve, learned active, selected | graph can be read at compact scale; connector state is visible | node art becomes too icon-like and hides tree structure |
| nav rail and bottom deck | inactive, active, focused, unavailable | icon rail is operational, bottom deck feels recessed and stable | nav icons look like unrelated mobile-app buttons |
| fallback/missing/debug | fallback, missing, hidden/debug | visibly unresolved/utility while still dark-v1 | too polished means missing resource looks approved |

## 6. Resource Decision Gate

Before requesting new art, an implementation packet should answer these questions:

1. Which retained component family is failing: shell, map-stage, slot, modal, tooltip, talent, combat, nav or bottom deck?
2. Is the failure structural, state-related, or texture/material-related?
3. Can an existing manifest-backed dark-v1 asset satisfy the state if sliced or styled differently?
4. Is the missing need a drawable/state problem, or just Table spacing and typography?
5. Will the new asset be repeated enough to justify formal resource pipeline work?
6. Which evidence crop will prove that the new asset improved runtime quality?

New art is justified when at least one of these is true:

1. A migrated retained UI surface would otherwise ship with default Scene2D-looking widgets.
2. Existing assets cannot express required interactive states such as focused, selected, disabled, modal blocking or stack badge.
3. Stretching an existing panel produces visible scale artifacts in golden or packaged crops.
4. A repeated dense surface such as inventory cells or talent nodes loses hierarchy without purpose-built state drawables.
5. `UI/UI-demo-new.png` comparison shows the right panel, bottom deck or map-stage frame lacks material depth after layout is otherwise correct.

New art is not justified when:

1. The issue is only a missing retained layout or actor-tree test.
2. The issue can be fixed with spacing, typography, wrapping or state routing.
3. The request would create a new style era instead of dark-v1 continuity.
4. The asset would bypass key registry, sheet plan, canonical manifest, runtime manifest, resolver tests or `resourcePipelineLint`.

## 7. Evidence And QA Checklist

| Evidence | Must prove | Failure if |
| --- | --- | --- |
| actor-tree report | retained structure, z-order, focus ownership and modal layering | structure is correct but visual state cannot be inspected |
| retained shell golden | map/right/bottom/nav geometry and Skin style consumption | layout is stable but looks like flat/default widgets |
| packaged map-stage scenarios | runtime packaging loads the same Skin assets and route metadata | debug golden passes but packaged app shows missing or stale resources |
| Skin style report | required style families exist and are resolver-backed | styles are hardcoded colors/raw paths or duplicate font/resource ownership |
| contact sheet, only when resources are generated | cells are dark-v1, state-specific, machine-cut and readable at runtime size | contact sheet looks attractive but cannot survive slicing or small display |
| side-by-side review against `UI/UI-demo-new.png` | material depth, hierarchy and density move toward the target quality bar | retained UI is structurally correct but still generic |

## 8. Open Design Critique Prompts

Use these prompts for review, not as direct implementation prompts.

### 8.1 Resource Necessity Critique

```text
Review this D10 retained UI screenshot or mockup. Identify which visual problems are true resource gaps versus layout, spacing, typography or state-routing issues. For each resource gap, name the component family, required state, evidence crop that should prove the fix, and whether existing dark-v1 assets could be reused or re-sliced.
```

### 8.2 Skin Cohesion Critique

```text
Review these retained UI surfaces for dark-v1 Skin cohesion. Look for default Scene2D styling, flat gray panels, over-bright cyan/gold, generic mobile button frames, inconsistent border weight, or resource families that look from another era. Return concrete corrections by component family.
```

### 8.3 Dense Surface Critique

```text
Review the inventory, equipment, shop or talent retained surface for dense operational readability. Check selected, hover, disabled, learned/locked, stack badge, compare/detail and modal states. Flag any state that relies only on color, shifts layout, or makes repeated cells visually noisy.
```

### 8.4 Packaged Evidence Critique

```text
Compare packaged-app evidence with debug/golden evidence for the same D10 retained surface. Look for missing Skin drawables, atlas/slice drift, scale artifacts, stale old-route chrome, clipped text, old immediate UI residue, or resource load differences that would make packaged whitebox incomplete.
```

## 9. Ownership And Pipeline Reminder

D10 may identify resource needs, but the resource flow remains:

```text
design decision -> key registry -> sheet plan -> raw sheet/prompt if needed -> cut/contact sheet -> canonical manifest -> sync runtime manifest -> resolver/Skin test -> resourcePipelineLint -> golden/packaged evidence
```

Rules:

1. `Skin` is a consumer of manifest-backed assets, not a resource truth.
2. Skin Composer can help preview states, but its exported JSON is not runtime truth.
3. KTX, if later accepted, is only a Kotlin DSL/productivity layer and does not change visual resource ownership.
4. New or re-sliced art should be tied to a D10 phase and evidence crop, not requested as a broad mood refresh.
5. The PR-08 D10 execution contract decides whether a phase can proceed; this helper only improves design review and resource triage.

## 10. Non-Goals

This file does not:

1. create resource keys, sheet rows, manifest entries, raw image paths, atlas regions or fallback rules;
2. require new art before `D10-P1`;
3. authorize committing generated images outside the K-ToME resource pipeline;
4. replace `UI/pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md`;
5. override `UI/review/open-design/ktome-dark-ui-design.md`, `UI/ART_STYLE_BIBLE.md`, `UI/PLAN.md`, resource gates, golden/manual evidence or packaged whitebox;
6. change gameplay, save, replay, profile, snapshot, input, combat, talent legality, inventory semantics or content-pack contracts;
7. decide KTX adoption or dependency changes.

# Dark UI/UX PR04-01 Playable Profession Passive Talents Design Notes

## 1. Status And Authority

This document is an auxiliary design review helper for `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`.

PR04-01 is not a resource-generation PR. It changes gameplay/content and Talent Assign presentation for playable profession passive talents. This file therefore focuses on passive-talent UX, build readability, right-detail hierarchy, action affordance safety, and whitebox evidence review.

Hard authority boundaries:

| Topic | Authority | This file may help with | This file must not do |
| --- | --- | --- | --- |
| PR04-01 scope, slices, acceptance matrix, tests, scenario ids | `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md` | explain design intent and review risks | add, remove, or rename implementation tasks, tests, gates, or scenario ids |
| Talent Assign layout and panel grammar | `UI/pr/dark-uiux-pr04-profession-tree-ui.md` | preserve PR04 list-tree, detail pane, state marker, and action grammar | redesign Talent Assign layout or override PR04 reference fidelity |
| Passive runtime semantics | `core` / `game` model and PR04-01 contract | describe what players need to understand | define passive resolver behavior, formula semantics, schema, save/load, or content-pack rules |
| Localization and detail text | typed snapshot, locale bundles, and PR04-01 token policy | review whether labels are understandable and consistent | invent final locale keys or let client parse text as rule truth |
| Visual resources | PR06 skill/tree icon rebaseline and manifest pipeline | remind reviewers that PR04-01 does not own icon repainting | generate new icons, manifest keys, raw sheets, or fallback assets |
| Evidence | PR04-01 whitebox/golden/manual contracts | give a design rubric for screenshot review | replace `clientSmoke`, `goldenScreenshot`, scenario materialization, or manual records |

If this document conflicts with the PR04-01 contract, ignore this document and fix the PR04-01 contract or implementation.

## 2. Why PR04-01 Needs A Design Helper

PR04-01 is a gameplay/content bridge, but it has a visible UX risk: passive talents can become technically correct while still unreadable or unconvincing in the Talent Assign panel.

The useful design problem is:

```text
Make passive talents feel like meaningful long-term profession identity, not disabled active skills, tiny stat footnotes, or hidden backend modifiers.
```

This helper is useful because PR04-01 touches five player-facing questions at once:

1. What makes a talent visibly passive?
2. Why is this passive worth learning instead of another active button?
3. What changes now at current rank?
4. What improves next rank?
5. Why does this row not show reserve / active-slot management?

Without a dedicated design lens, implementation can pass schema/runtime tests while the UI still leaves players guessing.

## 3. PR04-01 Design Job

PR04-01 should preserve the PR04 Talent Assign structure and add passive clarity inside that structure.

Design target:

```text
The player can focus a passive row, read its current and next-rank benefit, understand the trigger or condition when present, and see that it does not occupy or manage an active slot.
```

What must feel different from active skills:

1. PASSIVE rows should not imply a cast action.
2. PASSIVE details should emphasize always-on, conditional, trigger, resource, or status payoff.
3. PASSIVE actions should not show reserve or active-slot replacement affordances.
4. PASSIVE next preview should explain build direction, not only list raw numbers.
5. PASSIVE category should remain visible when the row is selected, locked, learnable, or learned.

What should stay the same:

1. PR04 list-tree layout.
2. PR04 row ordering and state marker grammar.
3. PR04 right-detail block order.
4. PR04 modal and active-slot contract for non-passive talents.
5. PR06 ownership of formal skill/tree icon rebaseline.

## 4. Passive UX Taxonomy

PR04-01 has two broad passive types. The UI should make both legible without introducing a second rules language.

| Type | Player read | Detail priority | Evidence risk |
| --- | --- | --- | --- |
| Static identity passive | "This permanently shapes my build." | current rank stat/resistance/speed/damage identity, then next-rank delta | looks like a small equipment affix instead of a talent |
| Trigger/conditional passive | "This rewards a specific loop." | trigger or condition first, payoff second, next-rank delta third | player misses when the effect applies |

Useful subtypes:

| Subtype | Example read | Required clarity |
| --- | --- | --- |
| durability anchor | max HP, defense, resistance | which survival axis improves |
| damage identity | attack, damage type, vs status | which damage loop improves |
| resource loop | on kill restore | trigger, resource, amount |
| status payoff | while guarded, vs marked, vs taunted | condition/status id and reward |
| hybrid stance | melee + spell + defense | why multiple axes belong together |
| risk curve | low-health payoff | threshold and risk/reward read |

The UI should not flatten all subtypes into generic "Passive: numbers go up" text.

## 5. Right Detail Hierarchy

PR04-01 details should answer questions in this order:

1. What is this talent?
2. Is it passive?
3. What does current rank or rank 1 preview do?
4. What condition or trigger must be true?
5. What improves at next rank?
6. What action is available now?

Recommended detail grammar:

```text
title / talent identity
type line: Passive
rank line: current rank or preview rank
current effect lines
next preview toggle / next effect deltas
actions block without reserve or slot-management rows
```

Rules:

1. Put `Passive` near the top of the detail pane, not buried after effects.
2. For conditional passives, show the condition before the numeric reward.
3. For trigger passives, show the trigger verb before the reward.
4. For multi-effect passives, split effects into stable lines instead of one dense sentence.
5. Percent and multiplier labels must be understandable without reading implementation names.
6. Do not use locale text as rule truth; numbers and ids come from typed args.
7. Do not hide passive action suppression by simply omitting the whole action area if the rest of Talent Assign normally has one; the absence of active-slot affordance should be deliberate and consistent.

## 6. Action Affordance Rules

The most important interaction distinction is that PASSIVE does not enter active-slot management.

| Focused row | Allowed read | Forbidden read |
| --- | --- | --- |
| learnable passive | learn or rank-up as a passive | reserve, equip slot, choose active slot |
| learned passive | learned permanent/conditional effect | cast, toggle, reserve, replace slot |
| locked passive | prerequisite / unavailable passive | disabled active button |
| trigger passive | trigger condition + payoff | manual cast action |
| conditional passive | condition + stat/damage payoff | temporary buff button |

Review failures:

1. `R Reserve` appears while a PASSIVE row is focused.
2. Active-slot modal opens after learning a PASSIVE.
3. A PASSIVE row looks disabled because it lacks active controls.
4. PASSIVE selected state is visually indistinguishable from ACTIVE selected state and the type line is not visible.
5. The right-detail action block leaves a blank hole that looks like a rendering bug.

## 7. Passive Attractiveness Review

Use this as a design review rubric for the final 12 passive conversions. It does not replace the PR04-01 data table.

| Check | Pass | Revise | Block |
| --- | --- | --- | --- |
| Rank 1 value | player can explain why learning it matters now | benefit is visible but weakly framed | reads like a tiny invisible stat |
| Rank 3 direction | build path is clearer than rank 1 | still mostly linear numbers | no meaningful reason to keep investing |
| Rank 5 identity | feels like profession identity or strong loop payoff | strong but generic | power creep or still uninteresting |
| Static + dynamic pairing | each profession gets one identity anchor and one loop payoff | pair exists but one side is weak | two passives do the same job |
| Active verb preservation | converted active has an obvious replacement active verb elsewhere | minor loop concern | profession loses key tactical button identity |
| Detail explanation | detail explains current value and next-rank delta | one line is ambiguous | player cannot tell when or why it works |

Blocking design symptoms:

1. A passive only adds one small number and no second payoff.
2. A converted active removes an important tactical decision without replacement.
3. A trigger passive does not name the trigger.
4. A conditional passive does not name the condition.
5. A passive's best payoff is only visible in data, not in Talent Assign detail.

## 8. Whitebox Evidence Review

PR04-01 whitebox evidence should prove the UX, not just artifact existence.

Required visual proofs:

| Scenario family | Must show | Design failure if missing |
| --- | --- | --- |
| static passive detail | focused passive row, type line, static benefits, no active-slot modal | static passive reads like generic row or active skill |
| static passive preview | next-rank deltas for the same focused passive | player cannot evaluate rank investment |
| trigger passive detail | trigger phrase plus resource/damage payoff | trigger loop is invisible |
| trigger passive preview | next-rank trigger payoff delta | progression value is unclear |
| conditional passive detail | condition/status plus payoff | conditional gameplay is hidden |
| action suppression | pressing passive reserve/action key does not show reserve/slot modal | passive still behaves like active-slot talent |

Evidence review rules:

1. The screenshot must identify the intended focused passive row by stable talent id through runbook or expected evidence.
2. Locked inspection is not enough for primary acceptance unless the PR04-01 contract explicitly allows it for that scenario.
3. A screenshot of the wrong talent is worse than no screenshot because it creates false confidence.
4. The evidence file name should match the semantic state being proved.
5. Manual records must state if packaged whitebox was skipped and what replacement evidence was used.

## 9. Design Critique Prompts

Use these prompts during review. They are not implementation prompts.

### 9.1 Passive Readability Critique

```text
Review this PR04-01 Talent Assign screenshot. Can the player immediately tell the focused row is PASSIVE, what it currently does, what improves next rank, and why no reserve or active-slot action appears? List concrete issues only.
```

### 9.2 Build Value Critique

```text
Review this passive talent design as a Roguelike build choice. Does rank 1 matter, does rank 3 change investment judgment, and does rank 5 express profession identity? Flag any passive that reads like a small hidden equipment affix.
```

### 9.3 Trigger/Conditional Critique

```text
Review this passive detail panel for trigger and condition clarity. Is the trigger or condition visible before the payoff, and are typed ids such as status, resource, or damage type understandable after localization? Flag ambiguity that would make players misplay the loop.
```

### 9.4 Evidence Critique

```text
Review this PR04-01 whitebox evidence set. Confirm each screenshot focuses the expected talent id and proves static detail, static preview, trigger detail, trigger preview, conditional detail, and passive action suppression. Flag stale, wrong-target, or artifact-only evidence.
```

## 10. Pipeline And Ownership Reminder

Open Design can help with:

1. passive UX readability review;
2. right-detail hierarchy critique;
3. build-value critique;
4. screenshot evidence critique;
5. wording-level ambiguity identification.

Open Design must not become:

1. a passive schema authority;
2. a data table authority;
3. a source of final locale keys;
4. a resource generation path;
5. a replacement for focused tests, golden evidence, whitebox runbooks, or manual records.

PR04-01 remains a gameplay/content bridge. Its implementation must still close through the PR04-01 contract, owner gates, typed snapshot boundary, and repo-relative evidence records.

## 11. Non-Goals

This file does not:

1. choose the 12 converted talents;
2. tune passive numbers;
3. define passive resolver behavior;
4. define content-pack passive behavior;
5. define locale token names;
6. add new icons or sprite sheets;
7. change PR04 Talent Assign layout;
8. replace PR04-01 whitebox scenarios;
9. mark packaged whitebox complete;
10. permit client-side gameplay calculation.

# Dark UI/UX PR07 Golden Whitebox Polish Design Review Notes

## 1. Status And Authority

This document is an Open Design helper for `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md`.

It defines a design-review lens for PR07 final polish: hierarchy, detail, functionality, consistency, AI-slop signals, and screen coverage evidence quality. It is not a golden, whitebox, packaging, or manifest authority.

Hard authority boundaries:

| Topic | Authority | This file may help with | This file must not do |
| --- | --- | --- | --- |
| PR07 scope, packaged-app requirement, stop conditions | `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md` | describe final design quality checks | weaken packaged-app whitebox, final-full coverage, or PR07 completion rules |
| Screen evidence matrix | `UI/pr/screen-coverage-matrix.md` | propose how to review the evidence index for design completeness | add, rename, or remove canonical screen labels |
| Golden and smoke verification | Gradle tasks and harness contracts | explain what visual issues a green gate might not catch | replace `goldenScreenshot`, `clientSmoke`, or `verifyChanged` |
| Packaged app whitebox | PR07 runbook, packaged build, manual records, evidence directory | define comparison questions for packaged vs debug output | mark packaged whitebox complete without running it |
| Manifest/resource truth | final-full coverage, canonical manifest, runtime manifest, sheet-plan pipeline | flag design inconsistency after coverage | authorize direct resource edits or manifest exceptions |
| Dark UI style | `UI/ART_STYLE_BIBLE.md` and Open Design base reference | provide critique vocabulary | redefine palette, style tag, or component-state ownership |

PR07 can use this file as a merge-readiness design review checklist. PR07 still closes only through the authoritative gates and records defined by the PR07 document.

## 2. PR07 Design Job

PR07 is not a large resource-generation PR. Its useful role is final visual and operational closure after PR04-PR06 have replaced the major surfaces.

Design goal:

```text
Before merge, prove that the dark-v1 UI is not only covered by manifests and golden screenshots, but also coherent, functional, readable, and free of obvious AI-generated or cross-era polish defects across all required screens.
```

PR07 should catch problems that automation can miss:

1. a golden screenshot can be stable while hierarchy is confusing;
2. packaged app can launch while a modal looks cramped at the evidence size;
3. manifest coverage can pass while two icon families look like different games;
4. fallback visuals can be present but visually imply approved content;
5. validation overlays can be technically correct but too noisy for final review;
6. generated art can contain AI-slop signals that are not encoded in file names or manifests;
7. screen coverage can list artifacts without proving the user-visible surface was actually inspected.

## 3. Final Review Axes

Use these axes for PR07 design review. A finding is useful only if it points to a concrete screen, visual family, or evidence artifact.

### 3.1 Hierarchy

Review question:

```text
Does each screen make the player's current decision, state, or problem visually obvious before decorative detail?
```

Check:

1. first read is the gameplay decision or outcome;
2. modal title, selected row, focused control, and warning state do not compete;
3. status and quest signals do not overpower map/actor readability;
4. validation overlays are bounded and scannable;
5. victory/defeat/outcome screens do not use marketing-page composition;
6. loading/error screens communicate state without becoming full-page decoration;
7. nested card-like panels are not creating false hierarchy;
8. focus and selection can be identified without relying only on color.

Block if:

1. the primary action or current state is ambiguous;
2. focus can be mistaken for learned/active/invalid state;
3. warning/error content is visually hidden;
4. modal or overlay content overlaps in a way that obscures controls.

### 3.2 Detail

Review question:

```text
Does visual detail support gameplay readability instead of becoming noise?
```

Check:

1. material detail follows forged iron, worn stone, soot, leather, ember, and restrained cyan roles;
2. icon interiors stay readable at compact sizes;
3. small badges remain stable and do not resize rows;
4. text is warm, readable, and not pure-white glare;
5. surface texture does not reduce contrast for status, quest, or validation rows;
6. source art does not contain baked text, labels, numbers, hotkeys, or watermark-like marks;
7. actor, map, portrait, skill, status, and quest details share outline weight and light direction.

Block if:

1. detail hides the subject at `32x32`;
2. a generated cell looks like a random texture crop;
3. baked text or fake glyphs appear in production-facing images;
4. visual noise makes validation or error information hard to read.

### 3.3 Functionality

Review question:

```text
Can a player operate the screen confidently with the displayed controls, states, and feedback?
```

Check:

1. disabled, unavailable, selected, focused, active, warning, and fallback states are distinct;
2. keycap/button copy is not baked into art and follows runtime/localized text;
3. long validation lists remain bounded;
4. min-window or evidence-size screenshots retain readable controls;
5. modal stack and error surfaces make back/close/retry semantics visually plausible;
6. quest marker and objective summary read as actionable game context;
7. fallback/missing visuals do not invite the player to treat unresolved content as a real reward;
8. packaged app output still presents the same functional state as debug/golden evidence.

Block if:

1. a control appears enabled but is unavailable;
2. a missing/fallback visual appears as normal content;
3. keyboard or modal affordance is contradicted by the visual state;
4. packaged app evidence differs from debug evidence in a player-visible way without explanation.

### 3.4 Consistency

Review question:

```text
Do all required screens look like one dark-v1 product rather than a series of isolated replacements?
```

Check:

1. color roles are stable across game shell, talent/skills, status, quest, validation, loading, error, and outcome screens;
2. panel material and edge language repeat without becoming monotonous;
3. icon families remain distinct but share era and lighting;
4. map/actor/portrait resources do not drift from UI chrome;
5. fallback/debug visuals share the era but remain lower hierarchy;
6. screenshots do not reveal legacy bright, vector, ASCII, or painterly leftovers;
7. screen coverage exceptions are explicit and not used to hide missing review.

Block if:

1. any player-visible rejected or old-style cell remains in PR07;
2. two major surfaces look like different art directions;
3. a Conditional/Required screen lacks credible visual evidence;
4. an exception is used where PR07 should route back to PR06.

### 3.5 AI-Slop Signals

Review question:

```text
Would a player or reviewer see any image, icon, or layout as uncurated AI output?
```

Common signals:

1. nonsensical icon subject or impossible object shape;
2. fake text, pseudo-runes that look like letters, stray watermark marks;
3. inconsistent perspective within one sheet;
4. over-rendered glossy hero lighting on tiny functional icons;
5. repeated ornamental noise replacing a clear subject;
6. mismatched outline weight between adjacent icons;
7. broken symmetry where symmetry is required for UI frames;
8. subject cropped by the cell edge;
9. busy background baked into transparent-icon families;
10. inconsistent material era: clean plastic, bright anime sticker, parchment beige, mobile-game rarity frame, or high-saturation fantasy glow.

Block if:

1. the issue is player-visible in any Required PR07 surface;
2. the issue appears in a canonical icon family and would multiply across screens;
3. the issue can make a status, skill, quest, fallback, or validation symbol ambiguous;
4. the issue requires more than a single rejected-cell polish fix, meaning PR06 resource completeness was not actually done.

## 4. Screen Coverage Evidence Index Review

PR07's `dark-uiux-pr07-final-all-screens` is an evidence index, not one screenshot. The design review should inspect the index as a coverage artifact.

Recommended review table:

| Surface | Matrix status | Golden/manual evidence | Packaged evidence | Focused test or gate | Coverage artifact | Design status | Blocking note |
| --- | --- | --- | --- | --- | --- | --- | --- |
| game shell / main play | Required | golden or manual label | packaged capture or record | client smoke/golden | final-full coverage | Pass/Revise/Block | concrete issue |
| talent / skill / status / quest | Required | PR04/PR06 evidence label | packaged capture or record | focused harness/golden | final-full coverage | Pass/Revise/Block | concrete issue |
| validation overlay | Required or Conditional | PR07 validation label | packaged capture or record | validation overlay test/gate | fallback warning artifact | Pass/Revise/Block | concrete issue |
| loading / runtime error | Required or Conditional | PR07 runtime label | packaged capture or record | targeted startup/error path | N/A if not resource-owned | Pass/Revise/Block | concrete issue |
| victory / defeat / outcome | Required or Conditional | PR07 outcome label | packaged capture or record | outcome path test/gate | N/A if not resource-owned | Pass/Revise/Block | concrete issue |
| accessibility/settings | Conditional | PR07 label or exception | packaged capture or exception | focused test or documented skip | N/A if not resource-owned | Pass/Revise/Block | concrete issue |

Rules:

1. Required surfaces need evidence or a blocking issue.
2. Conditional surfaces need evidence or a documented reason they were not reachable/applicable.
3. A manual record without a referenced artifact should not count as visual inspection.
4. A packaged app record should bind to the packaged run, not an older debug session.
5. If a player-visible rejected cell appears in PR07 evidence, route back to PR06 instead of polishing around it.

## 5. Packaged Vs Debug Comparison

PR07 should compare packaged-app evidence against debug/golden/manual evidence because packaging can expose missing resources, path mistakes, scaling changes, or runtime-home differences.

Review pairs:

| Pair | Compare |
| --- | --- |
| debug golden vs packaged screenshot | same surface, same hierarchy, no missing resources, no old fallback |
| debug validation overlay vs packaged validation overlay | same bounded rows, same warning severity, no absolute paths |
| debug status/quest/skill sample vs packaged surface | same icon families and state badges |
| debug loading/error vs packaged loading/error | same chrome, readable text, no blank or legacy surface |
| final-full coverage vs packaged visuals | green coverage does not hide unresolved visual mismatches |

Block if packaged evidence shows:

1. missing runtime PNG that debug did not show;
2. old-style fallback where final-full expected dark-v1;
3. different scaling that breaks readability;
4. modal overlap or clipped controls;
5. evidence generated from the wrong run or stale process.

## 6. PR07 Scorecard

Use this compact scorecard during final design review.

| Axis | Pass | Revise | Block |
| --- | --- | --- | --- |
| Hierarchy | primary state/action is obvious on all reviewed surfaces | one surface has mild emphasis drift | user decision, warning, or current state is unclear |
| Detail | detail supports readability at evidence scale | noisy but localized detail issue | icons or controls become unreadable |
| Functionality | states and controls look operable and truthful | one state needs clearer treatment | state/control/fallback semantics are misleading |
| Consistency | dark-v1 era is coherent across screens | minor family drift | screen or icon family looks from another era |
| AI-slop | no obvious generated-art artifacts | isolated rejected-cell polish issue | player-visible fake text, impossible subject, bad crop, or noisy hallucinated detail |
| Evidence index | every Required/Conditional surface is covered or explicitly excepted | evidence exists but a note/artifact is weak | missing/stale/unbound evidence |
| Packaged parity | packaged and debug/golden evidence match at player-visible level | minor non-blocking packaging note | packaged app exposes missing resource, clipping, old visual, or stale record |

Recommended status language:

```text
Pass: no design blocker; proceed to authoritative PR07 gates.
Revise: localized polish issue; fix only if within PR07's rejected-cell/single-point scope.
Block: route to PR06 or the owning earlier PR if the issue proves incomplete manifest/resource replacement.
```

## 7. Open Design Critique Prompts

Use these prompts against PR07 evidence screenshots, packaged captures, or side-by-side comparison boards. They are critique prompts, not resource prompts.

### 7.1 Final Screen Quality Critique

```text
Review this PR07 final dark-v1 UI evidence set for hierarchy, detail, functionality, consistency, and AI-slop signals. Return only concrete findings that name the screen/surface, the visible issue, why it matters to player operation or final polish, and whether it is Pass, Revise, or Block.
```

### 7.2 Evidence Index Critique

```text
Review this PR07 screen coverage evidence index. Check whether each Required or Conditional surface has a credible golden/manual artifact, packaged-app evidence or explicit exception, focused verification reference, and relevant coverage artifact. Flag stale, missing, unbound, or vague evidence.
```

### 7.3 Packaged Parity Critique

```text
Compare packaged-app evidence with debug/golden evidence for the same PR07 surfaces. Look for missing resources, scaling drift, clipped UI, old-style fallback, stale runtime evidence, or player-visible differences that would make packaged whitebox incomplete.
```

### 7.4 AI-Slop Critique

```text
Inspect the visible art and UI details for AI-slop signals: fake text, impossible icon subject, bad crop, inconsistent outline weight, random ornamental texture, noisy pseudo-runes, perspective mismatch, over-rendered tiny icons, or cross-era material drift. Do not flag stylistic preference unless it affects readability, consistency, or trust.
```

### 7.5 Golden-Is-Green But Design-Is-Weak Critique

```text
Assume goldenScreenshot and manifest coverage are green. What visible design issues in this evidence set would still make the UI feel unfinished, inconsistent, or hard to operate? Focus on problems automation may not encode.
```

## 8. Final Review Workflow

Recommended PR07 design-review sequence:

1. Read PR07 scope and the screen coverage matrix.
2. Confirm final-full coverage belongs to PR06 and is not being bypassed by PR07.
3. Inspect the `dark-uiux-pr07-final-all-screens` evidence index.
4. Compare packaged-app evidence with debug/golden/manual evidence for the same surfaces.
5. Run the scorecard for each Required and Conditional surface.
6. Classify findings:
   - `Pass`: no action;
   - `Revise`: localized PR07 polish only;
   - `Block`: route back to the owning PR, usually PR06 for player-visible missing/rejected resources.
7. Update the PR07 manual record with what was actually inspected.
8. Leave authoritative gate status to PR07 validation commands and records.

## 9. Non-Goals

This file does not:

1. replace `goldenScreenshot`;
2. replace `clientSmoke`;
3. replace final-full manifest coverage;
4. replace packaged-app whitebox;
5. mark manual records complete;
6. define packaged-app launch commands or runtime-home paths;
7. authorize stale evidence;
8. allow PR07 to hide PR06 resource completeness failures;
9. permit large new resource batches under a final-polish label;
10. change the screen coverage matrix.

## 10. PR07 Completion Reminder

This Open Design review can improve merge quality, but it is not sufficient for merge.

PR07 should still close only when:

1. required golden/manual evidence exists;
2. packaged-app whitebox is complete or explicitly not required by PR07 rules;
3. final-full coverage is already clean;
4. the evidence index covers every Required/Conditional surface or records a valid exception;
5. localized polish issues are fixed within PR07 scope;
6. player-visible rejected or old-style cells are absent;
7. actual validation commands and manual records are reported truthfully.

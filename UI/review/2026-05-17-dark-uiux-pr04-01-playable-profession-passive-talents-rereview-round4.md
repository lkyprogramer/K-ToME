# PR04-01 Playable Profession Passive Talents Round 4 Review

Review target: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`

Review date: 2026-05-17

Review stance: senior Roguelike / ToME-like gameplay systems, profession design, UI/UX contract, implementation ambiguity, and verification readiness review.

This is a re-review after the previous feedback pass. The document has closed several earlier gaps, especially around equipment source identity, `HpRegenPerTurn` misuse, passive attractiveness, content-pack preflight path, and the white-box artifact naming drift. The remaining issues below are more precise, but they are still important because they can produce false-positive validation or inconsistent implementation.

## Findings

### P1 - Required CUA exact inputs currently focus the wrong talents

The white-box section now gives exact keyboard sequences, but the sequences do not match the current talent tree ordering.

Document locations:

- Static detail scenario: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`, white-box input sequence for `dark-uiux-pr04-01-passive-detail-static`
- Conditional detail scenario: same section, input sequence for `dark-uiux-pr04-01-passive-detail-conditional`
- Action suppression scenario: same section, input sequence for `dark-uiux-pr04-01-passive-action-suppression`

Current code evidence:

- `game/src/main/resources/data/talents/index.yaml` lists `vanguard_arms`, `vanguard_shield`, `vanguard_warcry`, `arcanist_flame`, `arcanist_frost`, and `arcanist_arcane` in fixed node order.
- `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` handles `Down` by moving the selected talent index by one.
- Existing validation scenarios already rely on this one-step index behavior.

Concrete mismatch:

- `unyielding` is not reached by `Down x8` from the initial Vanguard talent selection. With the current ordering, `unyielding` is deeper in the combined Vanguard tree order.
- `mana_surge` is not reached by `Down x6` from the initial Arcanist talent selection. That sequence lands in the Frost row area before the Arcane row target.
- `bulwark_march` is not reached by `Down x3` from the initial Vanguard talent selection. That sequence lands in the Arms row area rather than the Shield row passive.

Impact:

- The generated screenshots and `cua-runbook.md` can look valid while proving the wrong talent.
- A packaged white-box run can pass artifact presence checks but fail the actual design intent.
- This undermines the document's strongest improvement from the last round: executable white-box evidence.

Required correction:

- Either replace the key counts with counts derived from the current `index.yaml` order, or avoid fragile counts by adding a scenario setup contract that focuses a talent by stable `talentId`.
- Add an automated guard that each PR04-01 validation scenario records the expected focused talent id before screenshot capture. Artifact existence alone is not enough.
- If keyboard-only navigation is intentionally required, add a tiny mapping table in the doc: scenario id, profession, expected starting node, exact number of `Down` presses, expected focused `talentId`.

### P2 - Scenario setup still does not define how locked deep passives become learnable and screenshot-ready

The document now specifies scenario ids and exact inputs, but it still does not fully define the state that `F9` / `Enter` is expected to materialize before those inputs run.

Relevant ambiguity:

- `unyielding`, `mana_surge`, and `bulwark_march` are not shallow starter talents in the current tree structure.
- Existing validation infrastructure uses scenario actions and session preparation hooks to place the game into a deterministic state.
- The doc names required scenarios, but does not define the exact prepared ranks, points, focus state, or locked/unlocked assumptions for each scenario.

Impact:

- One implementer may grant prerequisite ranks in `FoundationGameSession`.
- Another may rely on keyboard navigation from a normal new run.
- A third may force the talent detail panel to show locked passives without making them learnable.

These are materially different behaviors. They affect whether the screenshot proves passive detail rendering, learnability, passive action suppression, or merely locked-node inspection.

Required correction:

- For each scenario id, add a setup contract with:
  - profession
  - initial selected/focused talent id
  - prerequisite ranks already granted
  - unspent talent points
  - whether the target passive is locked, learnable, or already learned
  - expected first detail title and passive kind lines
- Explicitly state whether screenshot evidence is allowed to inspect locked passive detail. If yes, say that locked inspection is a valid UI contract. If no, scenario setup must unlock the target first.

### P2 - Stat label token namespace may create a second locale truth

The document now adds an excellent full coverage table for `StatModifier`, but the token namespace decision is still ambiguous.

Document behavior:

- It lists required labels such as `ui.stat.max_hp`, `ui.stat.hp_regen`, `ui.stat.attack_bonus`, `ui.stat.attack_multiplier_bonus`, and others.

Current implementation context:

- Existing locale data already has `ui.stat.str`, `ui.stat.dex`, `ui.stat.con`, and `ui.stat.wil`.
- Existing modifier labels for inspect/detail-style UI are under tokens such as `ui.inspect.mod.hp_regen`, `ui.inspect.mod.cast_speed`, and related UI-specific labels.
- Current client/session presentation code already uses a mixed family of existing stat and inspect labels.

Impact:

- The doc can be implemented in two incompatible ways:
  - add a new `ui.stat.*` family for every modifier, leaving older `ui.inspect.mod.*` labels alive;
  - reuse/migrate existing inspect labels and only add missing entries.
- If both remain active without a rule, passive details, inspect panels, and future content-pack label validation may drift.

Required correction:

- Add one explicit decision:
  - either "PR04-01 migrates passive detail labels to canonical `ui.stat.*` tokens and keeps existing inspect labels only for old panels";
  - or "PR04-01 reuses existing `ui.inspect.mod.*` labels where present and only adds missing labels".
- Add locale validation coverage for every chosen token, not just the data-side effect kinds.
- If a migration is intended, include the affected presenter/client files in scope so implementation does not silently fork labels.

### P2 - UI detail examples are stale relative to the final passive data table

The UI detail examples still show values that no longer match the final Task A passive data.

Example mismatch:

- The sample detail for `Unyielding` shows `Max HP +12`, `Defense +2`, and a next-rank delta of `+12 -> +16`.
- The final Task A table defines `Unyielding` rank 2 as `maxHp +16`, `defense +2`, and `ResistanceBonus PHYSICAL +2`; rank 3 is `maxHp +22`, `defense +3`, and `ResistanceBonus PHYSICAL +3`.

Impact:

- A developer implementing from the example can omit the physical resistance line.
- A tester can assert against the example values and reject the correct data.
- The required detail screenshot may prove a stale contract instead of the final contract.

Required correction:

- Rewrite the sample detail block to use the final rank row exactly.
- Include the `Physical resistance` line in the example.
- Include one next-rank delta that demonstrates a non-`StatModifier` passive line, preferably `ResistanceBonus PHYSICAL`.

### P2 - Slice boundary still conflicts with the `pain_fuel` schema test placement

The acceptance matrix attaches `SchemaV2LoaderTest.loadsPainFuelHpRegenAsStatModifierNotHpRegenPerTurn` to `M01`, but the task split says `04-01a` must not change official talent data.

Why this matters:

- `pain_fuel` is part of the Task A official passive data rewrite.
- `M01` is described as contract/parity work before official talent runtime/data rewrite.
- Task 3 already lists the same `pain_fuel` test under the passive data rewrite validation list.

Impact:

- The execution order is ambiguous: either Task 1 must touch official data despite saying it cannot, or the `M01` test cannot exist until Task 3.
- This can cause review churn because the matrix and task section disagree on ownership.

Required correction:

- Move the `pain_fuel` test fully to `M03`, or change the `M01` version to use a synthetic fixture talent that proves `hpRegen` maps to `StatModifier.hpRegen`.
- Keep official profession data tests under the slice that actually rewrites official profession data.

### P3 - Task 6 validation list omits the exact-input scenario registry guard named in the matrix

The matrix names `ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosUseExactInputSequences`, but the Task 6 validation list does not include it.

Impact:

- The report matrix says exact input coverage is required.
- The implementer following Task 6's command checklist may not add or run that guard.

Required correction:

- Add the registry test to Task 6 validation.
- After fixing the P1 input mismatch, make that test assert the expected target `talentId`, not only that input arrays are non-empty.

### P3 - Multiple `DamageTypeBonus` ordering is not fully specified

The display table defines deterministic handling for `ResistanceBonus` by damage type order, but `DamageTypeBonus` can also occur multiple times on the same passive.

Concrete case:

- `mana_surge` grants `DamageTypeBonus FIRE`, `DamageTypeBonus COLD`, and `DamageTypeBonus LIGHTNING`.
- The display rules say these should be separate lines, but do not say whether the order follows YAML order, enum order, or display grouping order.

Impact:

- Snapshot tests and screenshots can differ if implementation uses input order in one place and enum order in another.
- Content-pack overlays can produce unstable detail ordering if later data authors reorder YAML entries.

Required correction:

- Add the same deterministic ordering rule for `DamageTypeBonus` that already exists for resistance.
- Prefer `DamageType` enum order for both bonus and resistance lines unless the project already has a canonical UI display order.

### P3 - Preview toggle scenario assumes an initial expanded/collapsed state without declaring it

The action-suppression scenario uses `P, P, R, R` style checks, but does not declare the initial preview panel state after scenario setup.

Impact:

- If the scenario starts with preview already expanded, the first `P` collapses rather than expands.
- The evidence may still produce two screenshots, but the before/after interpretation becomes fragile.

Required correction:

- Add `previewExpanded=false` or `previewExpanded=true` to the scenario setup contract.
- In screenshot evidence, name the files by semantic state rather than only by key step, for example `passive-action-preview-expanded.png`.

## Requirement Alignment

The current document is much stronger than the previous round in these areas:

- It no longer asks implementers to invent unstable equipment runtime instance ids.
- It correctly treats `pain_fuel` HP regeneration as `StatModifier.hpRegen`, not `HpRegenPerTurn`.
- It explicitly preserves the no-resource-cost, no-cooldown, no-active-action passive identity.
- It adds a passive attractiveness floor, which is necessary for a ToME-like class fantasy pass.
- It names canonical white-box artifacts under `cua-runbook.md` instead of the old path.
- It restores content-pack preflight to the current verification output path.

The remaining misalignments are mainly execution-contract issues:

- exact white-box input sequences do not match current tree data;
- scenario state preparation is underspecified;
- locale token authority is still ambiguous;
- sample UI values lag behind final data.

## 功能 / 系统一致性矩阵

| Area | Current review result | Risk |
| --- | --- | --- |
| Passive model contract | Mostly aligned | Needs slice/test ownership cleanup for `pain_fuel` |
| Equipment passive parity | Aligned | Current source identity correction is good |
| Official passive data | Mostly aligned | UI examples still stale |
| Talent Assign passive detail | Partially aligned | Stat label namespace and detail ordering need one authority |
| Passive action suppression | Conceptually aligned | Scenario setup and preview initial state still ambiguous |
| White-box evidence | Not yet reliable | Exact input sequences select wrong targets |
| Content-pack interaction | Aligned enough for this PR | Preflight artifact path is now corrected |
| Developer execution clarity | Improved but not complete | Remaining ambiguities can cause divergent implementation |

## 玩法与体验审查

The passive attractiveness floor is the strongest gameplay improvement in this revision. It prevents passives from becoming invisible "small numbers" while still avoiding active-skill complexity. That is aligned with ToME-like profession design: passive talents should change build posture, threat selection, durability curve, or damage identity even when they do not add a button.

The main UX risk is now evidence quality rather than design intent. If white-box captures the wrong talent, the team can approve a profession passive pass without ever seeing the intended passive detail panel. For a playable profession pass, that is a serious acceptance flaw because player-facing trust depends on clear "what changed, when, and why" detail rendering.

The second UX risk is label drift. If `ui.stat.*` and `ui.inspect.mod.*` both become semi-authoritative, passive details will feel inconsistent across talent detail, inspect panels, and future content-pack content. This is small in code, but visible to players and expensive to clean up later.

## 当前阶段必须解决的问题

Before implementation is considered ready, the document should be patched for:

1. Correct PR04-01 white-box target selection.
2. Explicit scenario setup state for all PR04-01 validation scenarios.
3. One canonical locale token policy for passive modifier labels.
4. Fresh UI examples that match the final passive data rows.
5. Matrix/task consistency for the `pain_fuel` schema test.

The first two are required because they determine whether validation evidence is meaningful. The others are smaller, but they prevent predictable implementation divergence.

## Removal / Iteration Plan

Recommended doc-only cleanup sequence:

1. Update the white-box scenario table with expected `talentId` and corrected navigation or focus-by-id setup.
2. Add a scenario setup subsection that defines prepared ranks, points, lock state, preview state, and target focus.
3. Rewrite the `Unyielding` and next-rank examples from the final Task A values.
4. Move or rename the `pain_fuel` test in the acceptance matrix.
5. Add a one-sentence canonical token policy for stat/modifier labels.
6. Add deterministic multi-line ordering for `DamageTypeBonus`.

This does not require expanding PR scope. It is contract clarification, not new feature work.

## Additional Suggestions

- Consider adding a generated or tested "scenario focus assertion" to the validation registry: scenario id -> expected focused talent id after input replay.
- In the white-box runbook, record the focused `talentId` next to each screenshot file. This would make manual review faster and prevent false evidence.
- For passive detail screenshots, prefer one static passive, one conditional passive, and one multi-damage-type passive. That covers the most likely formatting regressions with only three examples.
- If the team keeps keyboard-only CUA navigation, include the talent tree index order in the report artifact. It will make future tree reordering failures obvious.

## Suggested Verification

I did not run Gradle or packaged app verification for this review because this pass is a document review and the target implementation is not being changed here.

Suggested document-level checks after patching:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :tools:test --tests com.ktome.tools.validation.ValidationScenarioRegistryTest
./gradlew :client:packageMacApp :tools:preparePhase4V4Whitebox
```

Suggested implementation checks once PR04-01 code lands:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests com.ktome.game.data.SchemaV2LoaderTest
./gradlew :client:test --tests com.ktome.client.render.TalentAssignDetailPresenterTest
./gradlew :tools:test --tests com.ktome.tools.contentpack.ContentPackPreflightTest
./gradlew verifyChanged
```

For manual/package white-box acceptance, the runbook must verify:

- screenshot target title equals expected passive `talentId` / localized name;
- static passive lines show final rank values;
- conditional passive lines show trigger condition and inactive/active state;
- passive nodes do not expose cast/cooldown/resource/action prompts;
- `R` does not respec or mutate learned passive state when focused on a passive node.

## Summary

The revised document is close to implementable, but it is not yet safe to treat as an executable acceptance contract. The largest blocker is the white-box exact input mismatch: current sequences appear to select the wrong talents, which would make the required screenshots misleading. Fixing that, plus scenario setup and locale-token authority, should make the plan materially clearer without expanding scope.

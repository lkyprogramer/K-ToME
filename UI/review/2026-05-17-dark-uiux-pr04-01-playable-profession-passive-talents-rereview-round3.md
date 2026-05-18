# PR04-01 Playable Profession Passive Talents Round 3 Review

Review target: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`

Review stance: Roguelike / ToME-like gameplay design director, system design owner, and implementation-readiness reviewer.

Review date: 2026-05-17

Scope: This round reviews the revised PR04-01 document against current K-ToME repo contracts and implementation surfaces. I did not edit the PR document or production code in this review.

## Findings

### [P1] `HpRegenPerTurn` and Task A `hpRegen` are still semantically conflated

The passive kind coverage table says `HpRegenPerTurn` has official PR04-01 talent data via "Task A regen stat line" (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:224-235`). But the Task A data uses decimal `hpRegen +0.2` through `pain_fuel` (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:470-475`), while current `HpRegenPerTurn` is an integer direct equipment passive (`core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:93-95`) and `StatModifier.hpRegen` is a `Double` (`core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:140-158`).

This matters because the implementation can choose two incompatible interpretations:

- Encode `pain_fuel` as `PassiveEffect.HpRegenPerTurn(amount = ?)`, which forces rounding or changes timing semantics.
- Encode it as `PassiveEffect.StatModifier(StatModifier(hpRegen = 0.2))`, which contradicts the coverage table saying `HpRegenPerTurn` is covered by official talent data.

Required fix: explicitly state that `pain_fuel.hpRegen` is `StatModifier.hpRegen`, not `HpRegenPerTurn`. Keep `HpRegenPerTurn` as equipment parity only unless PR04-01 introduces an integer per-turn heal talent. Add a negative/positive assertion such as `SchemaV2LoaderTest.loadsPainFuelHpRegenAsStatModifierNotHpRegenPerTurn`.

### [P1] The line template table cannot render PR04-01's own `attackMultiplierBonus` data

The fixed stat order for `StatModifier` only lists `maxHp`, `attack`, `defense`, `accuracy`, `evasion`, `speed`, `critChance`, `talentPower`, `castSpeedRating`, and `hpRegen` (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:420-423`). But PR04-01 official data uses `attackMultiplierBonus` in both `pain_fuel` and `last_stand` (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:474,487`), and the display rules even define how to show it (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:765-769`).

Current repo schema/runtime surfaces also support more fields than the template table names: `str`, `dex`, `con`, `wil`, `maxStamina`, `staminaRegen`, `attackMultiplierBonus`, and `defenseMultiplierBonus` are present in `StatModifier` / `SchemaStatModifier` (`core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:140-158`, `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt:867-886`).

Required fix: make the template table cover every stat field that PR04-01 can load, or define a fail-fast unsupported-stat list. At minimum, add `attackMultiplierBonus` before implementation starts and require assertions for `pain_fuel` and `last_stand` detail lines.

### [P2] Equipment `sourceId = runtime item instance id` is not implementable from the current resolver shape

The source glossary requires equipment `sourceId` to be the runtime item instance id (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:215-222`). Current `EquippedPassiveSource` stores `item`, `passive`, and `affixId`, but no `EntityId` or runtime item id (`core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt:10-14`). `equippedPassives` iterates `itemId` and then discards it before building sources (`core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt:47-55`). `ItemInstance` itself also has no runtime id field (`core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:308-325`).

This leaves an implementation trap: developers may either invent an unstable id, silently use `item.baseId`, or widen resolver signatures without a documented test surface.

Required fix: choose one contract:

- Carry `EntityId` into the new source-agnostic `PassiveSource` and document `sourceId = "item:<EntityId.value>"`, with a test proving two identical base items produce distinct source ids.
- Or explicitly keep 04-01 equipment `sourceId` behavior equivalent to the legacy equipment cue identity and reserve runtime instance ids for a later PR.

The document already says legacy equipment cue keys must remain behavior-identical; the source identity glossary needs to match that implementation path.

### [P2] Whitebox scenario definitions are improved, but still not executable enough for deterministic materialization

The whitebox table now lists three scenario ids, seeds, professions and evidence paths (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:787-815`). However, the "required steps" are still prose such as "open Talent Assign, select `unyielding`, learn rank 1" rather than exact `ValidationScenarioEvidenceStep.input` sequences.

Current repo contracts are stricter:

- `ValidationScenarioEvidenceStep` requires `mode`, `input`, `expectedVisibleResult`, and `evidenceFile` (`game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt:31-43`).
- `ValidationScenarioRuntimeSpec` requires preset, seed, locale, profession, race, zone, floor, routeIndex, and contentPackMode (`game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt:139-158`).
- The materialization catalog requires window width and height (`tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioMaterializationCatalog.kt:6-14`).
- Existing registry tests reject vague runbook phrases and assert exact key sequences for prior PR scenarios (`game/src/test/kotlin/com/ktome/game/validation/ValidationScenarioRegistryTest.kt:79-86`, `game/src/test/kotlin/com/ktome/game/validation/ValidationScenarioRegistryTest.kt:89-106`).

Required fix: add a registry-ready table per PR04-01 scenario with exact runtime fields, window size, `requiredEvidenceFiles`, `manualRecordPath`, and exact keyboard input strings. The implementation tests should reject vague terms like `open`, `select`, `verify`, and `scenario fixture grants` inside generated CUA steps.

### [P2] Acceptance artifact paths do not match current tool outputs

Two artifact paths in the acceptance matrix point to files that current tooling does not produce.

`M14` expects `build/whitebox/<scenario>/runbook.md` (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:77`), but `Phase4V4WhiteboxScenarioCli` writes `cua-runbook.md`, `manual-record-template.md`, and `expected-evidence.json` (`tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt:56-75`).

`M13` expects `build/reports/content-pack/preflight-summary.json` (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:76`), but the current tools output directory is `tools/build/reports/verification/content-pack/preflight`, and the file name is `content-pack-preflight-summary.json` (`tools/build.gradle.kts:195`, `tools/src/main/kotlin/com/ktome/tools/contentpack/ContentPackPreflightRunner.kt:37-65`).

Required fix: update the acceptance matrix and canonical artifacts to the actual repo-relative outputs:

- `build/whitebox/<scenario>/cua-runbook.md`
- `tools/build/reports/verification/content-pack/preflight/content-pack-preflight-summary.json`

This is small, but it will otherwise cause reviewers to look for missing artifacts even when the gate passed.

### [P2] Task B visible evidence only proves the trigger/resource case, not conditional or damage/resistance passive readability

The required Task B evidence is `mana_surge` and proves `OnKillResourceRestore` (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:811-815`). The action-suppression scenario selects `bulwark_march`, but its required proof is about `R` not opening active-slot UI, not about `ConditionalStatBonus` detail readability (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:789-793,811-815`).

For a ToME-like passive system, conditional passives are the highest-risk readability surface: players need to understand whether "while guarded", "below 30% HP", damage-vs-status, and resistance bonuses are currently passive identity, future delta, or active ability text leftovers. Automated tests can cover mapping, but at least one player-visible conditional or damage/resistance passive should have whitebox evidence unless the doc explicitly declares it automation-only.

Required fix: either add a fourth scenario for `last_stand` or `bulwark_march` detail, or extend `dark-uiux-pr04-01-passive-action-suppression` so the evidence also proves the conditional line and next-rank delta before pressing `R`.

### [P3] Snapshot snippets omit serialization annotations even though they live on the render snapshot boundary

The doc places `TalentPassiveDetailSnapshot`, `PassiveDetailLineSnapshot`, `PassiveDetailDeltaLineSnapshot`, `PassiveDetailLineKindSnapshot`, and `PassiveDetailLineToneSnapshot` in `core.snapshot` (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:356-407`), but the code snippets omit `@Serializable`.

Current `RenderSnapshot.kt` uses `@Serializable` for render snapshot DTOs and nested description values (`core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:9-10`, `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:249-257`). If developers copy the snippet literally, snapshot serialization, golden inspection, or contract tests may fail later in a non-obvious way.

Required fix: mark these new snapshot DTOs and enums as `@Serializable` in the document, or explicitly say they are not serialized and explain why they can still live in `core.snapshot`.

### [P3] Whitebox Gradle command snippets miss the repository Java/SDKMAN precondition

The materialization commands are shown as raw `./gradlew ...` calls (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:795-801`). The repo-level contract requires `source "$HOME/.sdkman/bin/sdkman-init.sh"` and `sdk env` before any Gradle execution.

Required fix: either include the preamble in the command block or add an explicit line above it: "Run from repo root after `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env`; Gradle tasks must be executed serially."

### [P3] `terrainAffinityTalentPassiveRefreshesAfterMovement` needs fixture-only wording

The acceptance matrix requires `FoundationGameSessionTest.terrainAffinityTalentPassiveRefreshesAfterMovement` (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:70`), while the passive coverage table says `TerrainAffinityBonus` has no official talent in PR04-01 (`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:235-237`).

That can be valid as equipment parity or a synthetic fixture test, but the doc should say so. Otherwise implementers may add an official terrain-affinity passive just to satisfy the test name, which would expand content scope.

Required fix: rename or annotate the test as fixture-only, for example `PassiveEffectResolverTest.fixtureTerrainAffinityRefreshesAfterMovement`, or add one sentence that no official PR04-01 talent uses terrain affinity.

## Requirement Alignment

| Area | Round 3 status | Notes |
| --- | --- | --- |
| Passive source-agnostic model | Partial | Direction is correct, but `sourceId` runtime identity is still under-specified for equipment. |
| Static passive conversion | Partial | Portfolio and active-slot suppression are now much clearer; `hpRegen` / `HpRegenPerTurn` and `attackMultiplierBonus` remain blocking precision gaps. |
| Trigger / conditional passives | Partial | Automated test list is broad; player-visible whitebox evidence still only proves the trigger/resource shape. |
| Talent Assign passive detail | Partial | Owner boundary is fixed; line templates need full stat coverage and serialization clarity. |
| PASSIVE action affordance | Aligned | `R Reserve` suppression and runtime rejection are now concrete enough. |
| Content-pack scope | Mostly aligned | Runtime talent overlays are now explicitly unsupported; artifact path needs correction. |
| Screen coverage matrix | Aligned | PR04-01 labels are now discoverable in `UI/pr/screen-coverage-matrix.md`. |
| Whitebox materialization | Partial | Scenario ids and evidence labels exist; exact runtime/input/window/runbook contract still needs tightening. |

## Gameplay And UX Review

The revised design is much stronger than the previous version. It now preserves tactical verb density by converting identity/bias talents rather than core starter buttons, keeps three starter active talents per playable profession, and prevents PASSIVE talents from entering active-slot management.

The remaining UX risk is explainability for non-static passives. Static lines like `maxHp +12` are low risk. Conditional passives like `SELF_HAS_STATUS=GUARD`, low-health attack multipliers, and damage-vs-status effects need one player-facing proof that the detail pane reads as passive rules, not as stale active-skill copy. This is why the whitebox evidence should include at least one conditional/damage line, not only `mana_surge`.

## Current Must Fix Before Implementation

1. Resolve the `hpRegen` vs `HpRegenPerTurn` contradiction.
2. Add `attackMultiplierBonus` and any other supported `StatModifier` fields to the passive detail stat order, or define fail-fast unsupported fields.
3. Decide how equipment runtime source identity is represented before replacing `EquippedPassiveSource`.
4. Correct the `M13` and `M14` artifact paths to current tool output names.
5. Turn the three whitebox scenarios into registry-ready specs with exact inputs, runtime fields, evidence files, manual record path, and window sizes.

## Additional Suggestions

- Add a small "stat display coverage" table that maps every `StatModifier` field to label key, value formatting, percent/int/decimal formatting, and whether PR04-01 official data uses it.
- Add a single test that snapshots `pain_fuel` and `last_stand` passive detail lines. These two talents cover both decimal regen and attack multiplier formatting, so they are better regression anchors than only `unyielding`.
- Keep `ContentPackSchemaTest` naming only if it will really exist. If the implementation uses existing `DataLoaderContentPackTest` / preflight tests instead, align the acceptance matrix before coding.
- Add a rollback note for `passiveEffects`: rollback should remove official PR04-01 passive data but keep resolver parity tests if the source-agnostic model lands first.

## Suggested Verification

Not run in this review. These are the suggested commands after the document is corrected and implementation starts:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :core:test --tests '*PassiveEffectResolver*' :game:test --tests '*Passive*' --tests '*Talent*'
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.ui.talent.DescriptionPresenterTest --tests com.ktome.client.input.InputHandlerTest
./gradlew contractLint localeLint :client:clientSmoke :client:goldenScreenshot maintainabilityLint
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression
./gradlew verifyChanged
```

If the whitebox scenarios are expanded to include conditional detail evidence, add that scenario to the same serial materialization sequence.

## Summary

Round 3 verdict: the revised PR04-01 document is close to implementation-ready, but not yet precise enough to hand to developers without drift. The remaining issues are smaller than the previous rounds, but several are contract-level: one official passive cannot be rendered by the current table, one regen effect can be encoded under the wrong passive kind, two artifact paths are wrong, and the whitebox scenarios still need exact tool-consumable inputs.

Fixing the items above should be done in the PR document before implementation proceeds, because they affect schema shape, runtime source identity, UI detail projection, and acceptance evidence.

# Dark UI/UX PR-04-01 Playable Profession Passive Talents

**阶段**: `dark-uiux-pr04-01-playable-profession-passive-talents`
**优先级**: `P0`
**工作量**: `XL`
**前置条件**: PR-04 Profession Tree UI 已完成 typed Talent Assign panel；`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md` 的职业树学习、rank up、active slot choice 和 passive draft 不占槽语义已落地；当前代码可找到 `TalentCategory.PASSIVE`、`TalentSidebarPresenter`、`DescriptionPresenter`、`PassiveEffectResolver`、`StatsCalculator`。
**PR 类型**: `gameplay/content bridge`。本 PR 放在 `UI/pr` 是因为 PR04 Talent Assign 暴露了被动天赋密度与详情展示缺口；实现 owner 以 `core/game/tools` 为主，`client` 只消费 typed presentation boundary。
**资源生成结论**: 不生成图片资源，不生成音频资源；skill icon、tree icon、profession icon 继续消费现有 manifest key。PR04-01 不新增 reference-crop 资源，正式全量 skill icon rebaseline 仍归 PR-06。
**上游视觉合同**: [Dark UI/UX PR-04 Profession Tree UI](dark-uiux-pr04-profession-tree-ui.md)
**上游 gameplay 合同**: [Phase4 v4 PR-01 Profession Tree Run Choice](../../docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md)

## Open Design 辅助参考

开发 PR04-01 时可在完成本 PR 预检后读取以下辅助设计输入：

1. [K-ToME Dark UI Design Reference For Open Design](../review/open-design/ktome-dark-ui-design.md)：统一 color roles、spacing、component states 与 anti-pattern 语言。
2. [Dark UI/UX PR04-01 Playable Profession Passive Talents Design Notes](../review/open-design/dark-uiux-pr04-01-playable-profession-passive-talents-design.md)：辅助被动天赋 UX、构筑价值、右侧详情层级、PASSIVE action affordance 和白盒证据审查。

这些文档只用于设计理解和 review，不能覆盖本 PR 的 passive schema、数值表、slice 顺序、acceptance matrix、focused tests、whitebox scenario、manual evidence、content-pack 边界或 PR06 icon rebaseline 责任。

## Design Direction

PR04-01 的目标是补齐 PR04 Talent Assign 面板暴露出的 gameplay/content 缺口：当前职业树虽然 UI 上支持 `ACTIVE / PASSIVE / SUSTAINED`，但可玩职业技能实际缺少明确被动，玩家会把所有 talent 都理解成主动技能，主动槽压力、长期构筑身份和右侧详情说明都会失真。

本 PR 把 6 个可玩职业的天赋树改成 ToME 式混合结构：

1. `ACTIVE` 是主动动作，消耗行动、冷却和资源；是否进入主动槽由 PR04 active-slot 规则决定。
2. `SUSTAINED` 是持续开关或有机会成本的能力；本 PR 不新增 sustained 语义，但既有 `SUSTAINED` 若进入主动槽也继续由 PR04 active-slot 规则处理。
3. `PASSIVE` 是永久或条件性职业能力，学习后立即生效，不占主动槽，不需要施放。

完成后，玩家在 Talent Assign 面板中必须能读懂：

1. 哪些节点是被动。
2. 当前 rank 的被动收益是什么。
3. 下一级具体提升哪些数值。
4. 被动不会触发 active slot replacement modal。
5. 6 个可玩职业都至少有 2 个明确被动，而不是只有文案暗示。

明确不吸收：

1. 不新增职业，不解冻 `shadowblade / warden`。
2. 本轮最终方案不新增天赋节点、不重排树布局，这是玩法价值评估后的选择，不是兼容性限制；如果实现阶段削弱到低于 §3.0 的被动吸引力地板，必须新增或重写被动，而不是保留弱被动。
3. 不把被动效果写进 client、localized text 或 renderer。
4. 不为了显示方便引入第二套 passive engine。
5. 不借本 PR 做全量技能图标重绘。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md) 的 gameplay/content bridge 例外。执行前先跑 `acceptanceContractLint`，再按 `04-01a -> 04-01b -> 04-01c` 顺序跑 schema/runtime focused tests、client detail focused tests、contract / locale / maintainability gate 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Execution Slices

PR04-01 是一个 umbrella 文档，但实现必须拆成三个顺序 slice。不得在前一 slice 未过 gate 时进入下一 slice。

| slice | goal | production modules | stop rule | required gate |
| --- | --- | --- | --- | --- |
| `04-01a-passive-contract-parity` | 让新 passive model 成为现有装备 passive 的行为保真超集；不改官方 talent data | `core`, `game` | equipment passive parity 任一测试失败时停止，不允许引入 talent passive runtime | `:core:test --tests '*PassiveEffectResolver*'`, `:game:test --tests '*Passive*'` |
| `04-01b-static-profession-passives` | 接入 `TalentLevelEffect.passiveEffects`、Task A 六个静态被动、rank replacement、active slot suppression、typed current/next detail | `core`, `game`, `client` | PASSIVE 详情或 active-slot suppression 未闭环时停止，不允许实现 Task B | `:game:test --tests '*Talent*'`, `:client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.ui.talent.DescriptionPresenterTest` |
| `04-01c-trigger-conditional-passives` | Task B 六个 resolver-backed 被动、source-aware combat trace、coverage lint blocking | `game`, `tools`, `client` | source identity、on-kill、condition、coverage artifact 任一缺失时停止，不允许宣称 PR complete | `:game:test --tests '*Passive*'`, `contractLint`, `:client:clientSmoke`, `:client:goldenScreenshot` |

`04-01b` 有意同时触碰 `core/game/client`：Task A official data 不能在 typed detail snapshot 和 PASSIVE action suppression 缺失时落地，否则主干会出现可学习但不可解释、仍带 active-slot affordance 的被动节点。该 slice 的唯一停止点是 Task A 可玩、可展示、不可入主动槽。

Rollback invariant:

1. `04-01a` 可独立保留，前提是 equipment passive behavior parity 全绿。
2. `04-01b` 失败时，Task A talent data、typed passive detail 和 coverage report-only 必须一起回滚或一起保留。
3. `04-01c` 失败时，Task B data 和 trigger wiring 必须一起回滚；coverage lint rule 保留 report-only，不删除。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox | crossPrDependency | removalOwner |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `UI04-01-M01-passive-schema-contract` | §2.1 / §2.2 passive typed schema | `core` / `game` | `NEW: SchemaV2LoaderTest.loadsTalentPassiveEffects`, `NEW: SchemaV2LoaderTest.loadsFixtureHpRegenStatModifierWithoutHpRegenPerTurn`, `NEW: TalentSchemaTest.passiveTalentsDeclarePassiveEffectsOnly` | `:game:test` | `game/build/reports/tests/test/index.html` | `N/A` | `TalentCategory.PASSIVE` | `N/A` |
| `UI04-01-M02-source-agnostic-resolver` | §2.3 passive source authority | `core` / `game` | `NEW: PassiveEffectResolverTest.resolvesEquipmentAndTalentSourcesTogether`, `NEW: PassiveEffectResolverTest.preservesPassiveSourceKindInTriggers`, `NEW: PassiveEffectResolverTest.equipmentPassiveSourceIdUsesLegacyTemplateIdentity`, `NEW: PassiveEffectResolverTest.equipmentPassiveParityCoversAllExistingKinds`, `NEW: PassiveEffectResolverTest.talentPassiveCollectionContainsExactlyOneEntryPerTalentIdAfterRankUp`, `NEW: PassiveEffectResolverTest.respecFromRank5ToRank1KeepsOnlyRank1Effects`, `NEW: FoundationGameSessionTest.preservesLegacyEquipmentPassiveCueKeys`, `NEW: FoundationGameSessionTest.passiveTraceStableKeyReplacesPreviousRankOnRankUp` | `:core:test --tests '*PassiveEffectResolver*'`, `:game:test` | `core/build/reports/tests/test/index.html`, `game/build/reports/tests/test/index.html` | `N/A` | existing equipment passive resolver | `N/A` |
| `UI04-01-M03-static-passive-runtime` | §4 Task A static passives | `game` | `NEW: FoundationGameSessionTest.learningStaticPassiveUpdatesDerivedStatsWithoutSlotChange`, `NEW: SchemaV2LoaderTest.loadsPainFuelHpRegenAsStatModifierNotHpRegenPerTurn` | `:game:test` | `game/build/reports/tests/test/index.html` | `N/A` | PR04 typed category display | `N/A` |
| `UI04-01-M04-rank-replace-semantics` | §2.4 runtime rank semantics | `core` / `game` | `NEW: FoundationGameSessionTest.passiveRankUpgradeReplacesPreviousRankEffects` | `:game:test` | `game/build/reports/tests/test/index.html` | `N/A` | talent rank model | `N/A` |
| `UI04-01-M05-active-slot-exclusion` | §2.4 active slot boundary | `game` / `client` | `NEW: FoundationGameSessionTest.passiveTalentNeverOpensActiveSlotChoice`, `NEW: TalentSidebarPresenterTest.passiveTalentDoesNotRenderActiveSlotChoiceAction` | `:game:test`, `:client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest` | `game/build/reports/tests/test/index.html`, `build/reports/tests/client/test/index.html` | `N/A` | PR04 active slot modal contract | `N/A` |
| `UI04-01-M06-playable-passive-coverage` | §3 coverage classification | `tools` | `NEW: contractLint.playableProfessionsHaveTwoPassiveTalents`, `NEW: contractLint.pr04_01ConvertedTalentsAreExactlyPassive` | `contractLint` | `tools/build/reports/tests/contractLint/index.html` | `N/A` | profession classification in README / data | `N/A` |
| `UI04-01-M07-trigger-and-conditional-passives` | §4 Task B long-term passives | `game` | `NEW: FoundationGameSessionTest.talentPassiveOnKillRestoreUsesTalentSource`, `NEW: FoundationGameSessionTest.conditionalTalentPassiveRefreshesWhenStatusExpires`, `NEW: FoundationGameSessionTest.healthThresholdTalentPassiveRecomputesAfterDamageAndHealing`, `NEW: FoundationGameSessionTest.deathblowMarkedSourceRequiresShadowstepRankTwoRoute`, `NEW: FoundationGameSessionTest.manaSurgeOnKillRestoreTriggersOnNonElementalKill`, `NEW: FoundationGameSessionTest.manaSurgeDamageTypeBonusDoesNotApplyToNonElementalDamage`, `NEW: FoundationGameSessionTest.beaconOfZealHolyDamageSourceIsReachableFromStarterTemplarLoadout`, `NEW: FoundationGameSessionTest.fluxAnchorLightningSourceIsReachableFromStarterSpellbladeLoadout`, `NEW: FoundationGameSessionTest.fixtureTerrainAffinityPassiveRefreshesAfterMovement`, `NEW: FoundationGameSessionTest.respecRemovesTalentPassiveDerivedStats`, `NEW: PassiveEffectResolverTest.equipmentAndTalentStatModifiersDoNotDoubleAccumulateAfterRefresh` | `:game:test`, `:core:test --tests '*PassiveEffectResolver*'` | `game/build/reports/tests/test/index.html`, `core/build/reports/tests/test/index.html` | `N/A` | unified passive resolver | `N/A` |
| `UI04-01-M08-save-load-derived-passives` | §2.4 save/load boundary | `game` | `NEW: FoundationGameSessionTest.saveLoadDerivesPassiveEffectsFromTalentRanks` | `:game:test` | `game/build/reports/tests/test/index.html` | `N/A` | no save schema migration | `N/A` |
| `UI04-01-M09-detail-current-and-next` | §2.5 / §6 typed passive detail contract | `core` / `game` / `client` | `NEW: SessionSnapshotMapperTest.passiveDetailSnapshotContainsCurrentAndNextLines`, `NEW: SessionSnapshotMapperTest.passiveDetailLineModelUsesDeclaredTemplateArgs`, `NEW: SessionSnapshotMapperTest.passiveDetailProjectionHandlesEveryKnownPassiveKind`, `NEW: SessionSnapshotMapperTest.passiveDetailProjectionFailsWhenKnownKindHasNoTemplateMapping`, `NEW: SessionSnapshotMapperTest.painFuelPassiveDisplayValueMatchesEffectiveHpRegenAfterDr`, `NEW: SessionSnapshotMapperTest.arcaneOverloadCastSpeedDisplayUsesEffectiveValue`, `NEW: SessionSnapshotMapperTest.lastStandPassiveDetailRendersHpThresholdConditionalLines`, `NEW: SessionSnapshotMapperTest.bulwarkMarchPassiveDetailLineKindsMatchConditionRenderingPolicy`, `NEW: SessionSnapshotMapperTest.unyieldingRank0NextPreviewShowsRank1ToRank2Deltas`, `NEW: SessionSnapshotMapperTest.passiveDetailStatOrderMatchesDocumentedTable`, `NEW: TalentSidebarPresenterTest.passiveDetailShowsCurrentAndNextPassiveEffects`, `NEW: TalentSidebarPresenterTest.passiveDetailLineCountPerTalentDoesNotExceedTen`, `NEW: DescriptionPresenterTest.passiveEffectsRenderAsKeywordLines`, `NEW: LocaleLintTest.passiveDetailStatModifierLabelsExist`, `NEW: LocaleLintTest.passiveDetailNonStatLabelsExist` | `:game:test`, `:client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.ui.talent.DescriptionPresenterTest`, `localeLint`, `:client:clientSmoke`, `:client:goldenScreenshot` | `game/build/reports/tests/test/index.html`, `build/reports/tests/client/test/index.html`, `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-detail-static.png`, `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-next-preview.png`, `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-detail-trigger.png`, `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-next-preview.png`, `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-conditional-detail-before-r.png`, `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-preview-expanded.png` | `required` | PR04 right detail order | `N/A` |
| `UI04-01-M10-frozen-exclusion-report` | §3 coverage classification | `tools` / `docs` | `NEW: contractLint.passiveTalentCoverageReportsFrozenExclusions` | `contractLint` | `tools/build/reports/tests/contractLint/index.html` | `N/A` | `shadowblade / warden` frozen classification | `N/A` |
| `UI04-01-M11-doc-vs-implementation` | §10 self-audit | `docs` | `acceptanceContractLint` | `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` | PR04-01 doc contract | `N/A` |
| `UI04-01-M12-passive-action-affordance` | §2.6 PASSIVE action matrix | `client` / `game` | `NEW: TalentSidebarPresenterTest.passiveActionsDoNotShowReserveOrActiveSlotManagement`, `NEW: TalentSidebarPresenterTest.passiveFocusedFooterDoesNotShowReserveHint`, `NEW: TalentSidebarPresenterTest.passiveLockedByLevelShowsLockedActionsOnly`, `NEW: InputHandlerTest.passiveTalentAssignRShortcutDoesNotEmitRespecCommand`, `NEW: InputHandlerTest.passiveLockedByPrereqRShortcutDoesNotEmitRespecCommand`, `NEW: FoundationGameSessionTest.passiveRShortcutLeavesDraftLoadoutAndRanksUnchanged`, `NEW: FoundationGameSessionTest.equipTalentToSlotRejectsPassive` | `:client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest`, `:game:test --tests '*FoundationGameSessionTest*'`, `:client:clientSmoke`, `:client:goldenScreenshot` | `build/reports/tests/client/test/index.html`, `game/build/reports/tests/test/index.html`, `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-no-active-slot-modal.png` | `required` | PR04 actions block | `N/A` |
| `UI04-01-M13-content-pack-contract` | §5 content-pack / overlay contract | `game` / `tools` | `NEW: SchemaV2LoaderTest.rejectsUnknownTalentPassiveEffectKind`, `NEW: DataLoaderContentPackTest.talentOverlayRegistryRemainsUnsupported`, `NEW: ContractLintTest.passiveEffectsCannotDefineNewCoreKinds` | `:game:test`, `contractLint`, `verifyContentPackPreflight` | `game/build/reports/tests/test/index.html`, `tools/build/reports/tests/contractLint/index.html`, `tools/build/reports/verification/content-pack/preflight/content-pack-preflight-summary.json` | `N/A` | content pack schema v2 | `N/A` |
| `UI04-01-M14-whitebox-scenario-materialization` | §7 whitebox scenario materialization | `game` / `tools` / `client` | `NEW: ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosAreRegistered`, `NEW: ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosUseExactInputSequences`, `NEW: ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosProjectPreferredFocusThroughSnapshot`, `NEW: ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosDeclareStructuredTypedAndVisibleAssertions`, `NEW: ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosDeclareLogEvidenceAndForbiddenFragments`, `NEW: RenderSnapshotContractTest.pr04_01ValidationFocusRequestIsSerialized`, `NEW: InputHandlerTest.enterTalentAssignConsumesValidationPreferredFocusOnce`, `NEW: Phase4V4WhiteboxScenarioCliTest.pr04_01PassiveTalentScenariosGenerateRunbooks`, `NEW: Phase4V4WhiteboxScenarioCliTest.pr04_01ExpectedEvidenceIncludesFocusAssertionsAndLogContracts` | `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail`, `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail`, `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression` | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/cua-runbook.md`, `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/cua-runbook.md`, `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/cua-runbook.md`, `build/whitebox/dark-uiux-pr04-01-static-passive-detail/expected-evidence.json`, `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/expected-evidence.json`, `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/expected-evidence.json`, `UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md` | `required` | Phase4 v4 whitebox registry parity | `N/A` |
| `UI04-01-M15-screen-coverage-matrix` | §7 / §10 screen coverage closure | `docs` / `tools` | `acceptanceContractLint` | `verifyChanged` | `UI/pr/screen-coverage-matrix.md`, `UI/pr/README.md` | `N/A` | UI/pr screen coverage governance | `N/A` |
| `UI04-01-M16-passive-attractiveness-floor` | §3.0 passive attractiveness contract | `game` / `docs` | `NEW: TalentSchemaTest.passiveTalentsMeetAttractivenessFloor`, `NEW: TalentSchemaTest.passiveRank5EvAnchorDeclaredWithinTwentyPercent`, `NEW: TalentSchemaTest.passiveEvDrRoutesDeclaredForRatingFields`, `NEW: TalentSchemaTest.conditionalPassiveUptimeIsDeclaredInDoc`, `NEW: TalentSchemaTest.conditionalPassivesDeclareTriggerOwnerInDoc`, `NEW: TalentSchemaTest.sameDimensionPassiveStackPerProfessionDoesNotExceedSoftCap`, `NEW: TalentSchemaTest.passiveOnKillResourceRestoreRank1IsAtLeastFivePercentOfStartingPool`, `NEW: TalentSchemaTest.passiveHighValuePayoffClassificationIsComplete`, `NEW: TalentSchemaTest.starterLoadoutFitsInActiveSlotCountAfterPr04_01Conversion`, `NEW: SessionSnapshotMapperTest.passiveDetailExplainsEveryHighValuePassivePayoff` | `:game:test`, `acceptanceContractLint` | `game/build/reports/tests/test/index.html`, `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md` | `N/A` | long-run build value floor | `N/A` |
| `UI04-01-M17-hp-regen-core-formula-migration` | §2.3 hpRegen core formula migration | `core` / `game` / `client` | `NEW: StatsCalculatorTest.hpRegenUsesEffectiveHpRegenAfterPassiveAggregation`, `NEW: PassiveEffectResolverTest.hpRegenStatModifierAndHpRegenPerTurnCollapseIntoSingleDerivedStat`, `NEW: PassiveEffectResolverTest.passiveHpRegenSumFedThroughEffectiveHpRegenBeforeDisplay`, `NEW: FoundationGameSessionTest.hpRegenTickAppliesEffectiveValueOnceAndRespectsHealDisabledStatus`, `NEW: FoundationGameSessionTest.legacyEquipmentHpRegenCuePolicyIsPreservedAfterSinglePathMigration`, `NEW: SessionSnapshotMapperTest.painFuelPassiveDisplayValueMatchesEffectiveHpRegenAfterDr` | `:core:test --tests '*StatsCalculator*' --tests '*PassiveEffectResolver*'`, `:game:test --tests '*FoundationGameSessionTest*'` | `core/build/reports/tests/test/index.html`, `game/build/reports/tests/test/index.html` | `N/A` | unified passive resolver | `N/A` |
| `UI04-01-M18-deterministic-order-and-budget` | §2.5 / §6 ordering and line budget | `tools` / `client` / `game` | `NEW: contractLint.damageTypeEnumOrderPinned`, `NEW: contractLint.passiveLineCountBudget`, `NEW: SessionSnapshotMapperTest.passiveDetailStatOrderMatchesDocumentedTable`, `NEW: TalentSidebarPresenterTest.passiveDetailFitsViewportAtMinimumWindow` | `contractLint`, `:game:test`, `:client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest` | `tools/build/reports/tests/contractLint/index.html`, `game/build/reports/tests/test/index.html`, `build/reports/tests/client/test/index.html` | `required` | PR04 right detail viewport | `N/A` |

PR04-01 新增测试统一使用 `camelCase`。`ownerGate` 列中逗号分隔的 task 是 AND，必须全部执行，不是 OR。`fastCheck` 中的 `contractLint.rule#subRuleId` 使用 `#` 表示 subRuleId；无 `#` 时表示整条 lint rule。

### Gate Budget

`durationSource` 固定指 `build/verification/verify-changed/full-task-duration-summary.{json,md}`；首次 `verifyChanged` 之前，每个 gate 的耗时来源都写 `N/A before first verifyChanged`。

| Gate group | Tasks | Trigger | Freshness / evidence rule | Duration source |
| --- | --- | --- | --- | --- |
| contract preflight | `acceptanceContractLint` | PR04-01 文档新增 schema、runtime、UI detail、coverage 合同 | 必须先于实现跑；失败先修文档，不用 runtime test 代替 | `durationSource` or `N/A before first verifyChanged` |
| schema/runtime fast lane | `:core:test --tests '*PassiveEffectResolver*' --tests '*StatsCalculator*'`, `:game:test --tests '*Passive*' --tests '*Talent*'` | passive schema、rank、learn/upgrade、save/load、trigger/condition 语义变化 | 新增或改 `PassiveEffect`、`TalentLevelEffect`、`FoundationGameSession` passive source 时必须刷新；`04-01a` 必须先证明 equipment parity；hp regen 只能走 §2.3 单一 derived path | `durationSource` or `N/A before first verifyChanged` |
| client detail fast lane | `:client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.ui.talent.DescriptionPresenterTest` | Talent Assign 右侧详情、当前/下一等级被动展示变化 | client 只验证展示，不允许计算 gameplay passive | `durationSource` or `N/A before first verifyChanged` |
| lint and contract | `contractLint`, `localeLint` | 新增 passive coverage rule、passive attractiveness floor、content-pack passive rule、locale token、description keyword 行 | coverage artifact 必须列出 `releaseBlocking`、`pr04_01BlockingDevPlayable`、`excludedFrozen`；unknown passive kind 必须 fail fast；passive value floor 不能退回单字段小数值；EV declaration、conditional uptime、trigger owner、same-dimension soft cap、DamageType enum order、converted talent id 精确集合和 passive line count budget 都是 blocking lint | `durationSource` or `N/A before first verifyChanged` |
| client evidence | `:client:clientSmoke`, `:client:goldenScreenshot` | PASSIVE detail 视觉、right detail 节奏、active slot modal suppression 可见变化 | 必须执行；packaged whitebox skipped/executed manual record 必须填写 §7 固定字段 | `durationSource` or `N/A before first verifyChanged` |
| whitebox materialization | `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail`, `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail`, `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression`, `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-effective-hp-regen-detail` | PR04-01 玩家可见 PASSIVE detail / action 行为变化 | 四个 scenarioId 必须同时存在于 `ValidationScenarioRegistry`、`phase4-v4-scenarios.yaml` 和 `Phase4V4WhiteboxScenarioMaterializationCatalog`; `trigger-passive-detail` 额外覆盖 `arcane_overload.castSpeedRating` effective decimal，`effective-hp-regen-detail` 覆盖 `pain_fuel.hpRegen` | `durationSource` or `N/A before first verifyChanged` |
| governance / final closure | `maintainabilityLint`, `verifyChanged` | 新增 source-agnostic passive model、resolver 泛化、public presentation line | `maintainabilityLint` 新增 finding 不能用 baseline 掩盖；`verifyChanged` 不替代 owner gate | `durationSource` or `N/A before first verifyChanged` |

### Canonical Artifact

| Artifact | Path | Owner | Rule |
| --- | --- | --- | --- |
| PR04-01 doc | `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md` | PR04-01 | 本 PR 的执行合同；实现必须做 doc-vs-implementation self-audit |
| upstream PR04 UI doc | `UI/pr/dark-uiux-pr04-profession-tree-ui.md` | PR04 | Talent Assign panel、detail order、active slot modal 和 icon/fallback 视觉合同 |
| focused game test report | `game/build/reports/tests/test/index.html` | PR04-01 | 覆盖 passive schema、runtime、resolver、save/load |
| focused client test report | `build/reports/tests/client/test/index.html` | PR04-01 | 覆盖 passive detail current/next、active slot suppression |
| static passive detail evidence | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-detail-static.png` | PR04-01 | 证明 `unyielding` rank 1 当前收益可见 |
| static passive preview evidence | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-next-preview.png` | PR04-01 | 证明 `unyielding` 下一级生命、防御、物理抗性增量可见 |
| static passive preview collapsed evidence | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-preview-collapsed-after-toggle.png` | PR04-01 | 证明展开再折叠预览后仍聚焦 `unyielding` 且无 PASSIVE `R` shortcut / footer `RESERVE` affordance |
| static passive after-learn evidence | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-after-learn-no-slot-modal.png` | PR04-01 | 证明学习 `unyielding` 后不打开 `ACTIVE_TALENT_SLOT_CHOICE` |
| static passive app log evidence | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-app.log` | PR04-01 | 证明白盒执行保留 log；不得出现 PASSIVE respec / reserve / slot command |
| trigger passive detail evidence | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-detail-trigger.png` | PR04-01 | 证明 `mana_surge` trigger/resource 被动详情可见 |
| trigger passive preview evidence | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-next-preview.png` | PR04-01 | 证明 `mana_surge` 下一级资源恢复和元素伤害增量可见 |
| trigger passive panel entry evidence | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-panel-entry.png` | PR04-01 | 证明预览折叠后仍聚焦 `mana_surge` 且无 active-slot modal |
| trigger passive after-learn evidence | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-after-learn-no-slot-modal.png` | PR04-01 | 证明学习 `mana_surge` 后不打开 `ACTIVE_TALENT_SLOT_CHOICE` |
| trigger passive app log evidence | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-app.log` | PR04-01 | 证明白盒执行保留 log；不得出现 PASSIVE respec / reserve / slot command |
| passive action suppression evidence | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-no-active-slot-modal.png` | PR04-01 | 证明 PASSIVE 选中态按 `R` 不发出 `PlayerCommand.RespecTalentTree` / reserve / slot command，不打开 active slot modal，且 footer 不显示 `RESERVE` |
| conditional passive detail evidence | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-conditional-detail-before-r.png` | PR04-01 | 证明 `bulwark_march` 条件被动和 TAUNT damage payoff 可见 |
| conditional passive preview evidence | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-preview-expanded.png` | PR04-01 | 证明 `bulwark_march` 下一级防御、速度、TAUNT damage 增量可见 |
| passive action log screenshot evidence | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-log-no-reserve.png` | PR04-01 | 证明返回 shell/log 后无 respec、reserve confirmation、slot replacement 或 rollback error 文案 |
| passive action app log evidence | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-suppression-app.log` | PR04-01 | 证明按 `R` 后 log 中无 PASSIVE respec / reserve / slot command 且无 `log.talent.draft_rollback` |
| expected evidence manifests | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/expected-evidence.json`, `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/expected-evidence.json`, `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/expected-evidence.json` | PR04-01 | 生成并列出 §7 的全部 `requiredEvidenceFiles`、`requiredLogEventKeys`、`forbiddenLogFragments`、`expectedFocusedTalentId`、structured typed assertions 和 structured localized visible assertions；M14 拥有文件存在性合同 |
| whitebox manual record | `UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md` | PR04-01 | 记录四个 scenario 的执行结果；packaged whitebox executed/skipped 都必须填写 §7 固定字段 |
| contract lint report | `tools/build/reports/tests/contractLint/index.html` | PR04-01 | 覆盖 playable passive count 和 frozen exclusion |
| content-pack preflight summary | `tools/build/reports/verification/content-pack/preflight/content-pack-preflight-summary.json` | tools | 证明 talent overlay registry unsupported 与 content-pack preflight 未漂移 |
| verify duration | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | tools | 用于 Gate Budget 复盘；不存在时记录 `N/A before first verifyChanged` |

### Failure Rule

如果 PR04-01 失败，按以下顺序定位，不得用 UI fallback 或数值绕行掩盖：

1. `TalentLevelEffect.passiveEffects` 是否是 typed schema，并由 data loader 映射到 runtime model。
2. `TalentLoadout.talentLevels` 是否只按当前 rank 派生 passive，不累计历史 rank。
3. `PassiveEffectResolver` 是否同时接收 `EQUIPMENT` 与 `TALENT` source，且 trace/log 保留 source kind。
4. `StatsCalculator` 是否只消费统一 passive stat modifier 组件，没有 equipment/talent 双轨统计。
5. learn / rank up / save / load 是否都会刷新派生属性。
6. `ACTIVE_TALENT_SLOT_CHOICE` 是否只对 `ACTIVE / SUSTAINED` 生效，PASSIVE 不打开 modal。
7. PASSIVE actions 是否移除了 `R` shortcut command、`PlayerCommand.RespecTalentTree`、footer `RESERVE` hint 和 active slot management。
8. `TalentSidebarPresenter` / `DescriptionPresenter` 是否只展示 typed passive detail snapshot，不重新计算规则。
9. content-pack loader / lint 是否对 unknown passive kind fail fast。
10. coverage lint 是否正确区分 `releaseBlocking`、`pr04_01BlockingDevPlayable`、`excludedFrozen`，而不是把 frozen 空树显示成缺失。

不得通过以下方式修绿：

1. 把 PASSIVE 伪装成 `ACTIVE` self buff。
2. 在 client 根据 talent id 写死被动描述。
3. 复制一套 talent-only passive resolver。
4. 降低敌人强度或增加 starting points 来掩盖数值变化。
5. 删除 active slot gate 或降低 PR04 modal 断言。

## 1. 阶段目标

1. 6 个可玩职业每个至少 2 个明确 `PASSIVE` talent。
2. 被动学习后立即改变角色能力或战斗 resolver 输出，不占主动槽。
3. 右侧详情显示当前 rank 被动收益和下一 rank 增量。
4. 装备被动与天赋被动共享 resolver，避免第二真源。
5. contract lint 能防止可玩职业回退到全主动技能。
6. PR04 Talent Assign 面板能消费 PASSIVE 结果，不需要知道具体数值公式。

职业覆盖口径固定：

| 分类 | 职业 | 本 PR 口径 |
| --- | --- | --- |
| release playable | `vanguard`, `arcanist`, `rogue`, `templar` | blocking coverage，必须每职业 2 个 passive |
| dev playable | `berserker`, `spellblade` | PR04-01 blocking coverage，必须每职业 2 个 passive；仍保持 release report-only，不进入 release-facing blocking metric 分母 |
| excluded frozen | `shadowblade`, `warden` | 不计入分母，coverage artifact 必须列出 excluded reason |

## 2. 硬依赖合同

### 2.1 Passive typed model

新增 source-agnostic passive model，长期替代装备专属命名。正式 resolver 输入必须是 source-agnostic。

Physical package decision: PR04-01 keeps the unified passive model and resolver in the existing `core.item` package as a legacy physical location because current equipment passive models and `StatModifier` already live there. This is an intentional PR04-01 boundary, not an item-only semantic decision. The model must not reference item-only mutable state, and this PR must not introduce a second runtime hierarchy such as `TalentPassiveEffect` or `EquipmentPassiveEffect`. Moving these files to a neutral `core.passive` package is out of scope for PR04-01 and requires a separate cleanup PR after equipment parity is green.

Serialized schema kind `StatModifier` maps to Kotlin `PassiveEffect.StatModifierEffect`; YAML does not rename the kind.

```kotlin
sealed interface PassiveEffect {
    data class OnHitStatusProc(
        val statusId: String,
        val chance: Double,
        val duration: Int,
        val magnitude: Double = 0.0,
    ) : PassiveEffect
    data class OnKillResourceRestore(
        val resourceType: ResourceType,
        val amount: Int,
    ) : PassiveEffect
    data class StatModifierEffect(
        val modifier: com.ktome.core.item.StatModifier,
    ) : PassiveEffect
    data class ConditionalStatBonus(
        val condition: PassiveCondition,
        val statModifier: com.ktome.core.item.StatModifier,
        val statusId: String? = null,
    ) : PassiveEffect
    data class TerrainAffinityBonus(
        val terrainTag: TerrainTag,
        val statModifier: com.ktome.core.item.StatModifier,
    ) : PassiveEffect
    data class DamageVsTag(
        val tag: String,
        val bonusPercent: Double,
    ) : PassiveEffect
    data class DamageVsStatus(
        val statusId: String,
        val bonusPercent: Double,
    ) : PassiveEffect
    data class DamageTypeBonus(
        val type: DamageType,
        val bonusPercent: Double,
    ) : PassiveEffect
    data class ResistanceBonus(
        val damageType: DamageType,
        val amount: Int,
    ) : PassiveEffect
    data class HpRegenPerTurn(
        val amount: Int,
    ) : PassiveEffect
}

enum class PassiveSourceKind {
    EQUIPMENT,
    TALENT,
}

data class PassiveSource(
    val kind: PassiveSourceKind,
    val sourceId: String,
    val sourceTemplateId: String,
    val affixId: String? = null,
    val talentRank: Int? = null,
    val passive: PassiveEffect,
)
```

Source identity glossary:

| source | `sourceId` | `sourceTemplateId` | `affixId` | `talentRank` | stable-key rule |
| --- | --- | --- | --- | --- | --- |
| base equipment passive | `equipment:<itemBaseId>` | item `baseId` | `null` | `null` | keep existing equipment cue key literal |
| affix equipment passive | `equipment:<itemBaseId>:affix:<affixId>` | item `baseId` | affix id | `null` | keep existing equipment cue key literal and append existing affix segment where already present |
| special-template equipment passive | `equipment:<itemBaseId>:special:<specialTemplateId>` if template id exists, otherwise `equipment:<itemBaseId>` | item `baseId` | affix id if present | `null` | preserve existing `sourceSpecialTemplateId` in trigger trace |
| talent passive | talent id | talent id | `null` | learned rank | use PR04-01 talent passive key format from §2.3 |

Equipment `sourceId` is deliberately legacy-stable in PR04-01. Current `EquippedPassiveSource` does not carry `EntityId` or a runtime item instance id, and `ItemInstance` has no runtime id field. PR04-01 must not invent unstable instance ids; distinct source ids for two identical equipped base items are excluded from this PR and require an explicit inventory identity PR.

Supported `PassiveCondition` variants are frozen for PR04-01:

| PassiveCondition | Semantics | Required paired field |
| --- | --- | --- |
| `HP_BELOW_50` | player health fraction is `< 0.50` | `N/A` |
| `HP_BELOW_30` | player health fraction is `< 0.30` | `N/A` |
| `HP_ABOVE_80` | player health fraction is `> 0.80` | `N/A` |
| `SELF_HAS_STATUS` | actor currently has `statusId` | non-null `statusId` |

Official PR04-01 talent data uses only `HP_BELOW_30` and `SELF_HAS_STATUS`. `HP_BELOW_50` and `HP_ABOVE_80` remain valid because they already exist in `core.item.PassiveCondition`, but official data must not add new uses without updating §3.0 value audit and §7 typed assertions. Content packs cannot declare new `PassiveCondition` values.

Passive kind coverage:

| kind | official PR04-01 talent data | equipment parity | UI detail required |
| --- | --- | --- | --- |
| `StatModifier` | Task A | loader-mapped; no current equipment passive data uses `StatModifier` as an equipment passive | yes |
| `ConditionalStatBonus` | Task B | yes | yes |
| `OnKillResourceRestore` | Task B | yes | yes |
| `DamageVsStatus` | Task B | yes | yes |
| `DamageTypeBonus` | Task B | yes | yes |
| `ResistanceBonus` | Task A and Task B | yes | yes |
| `HpRegenPerTurn` | no official talent in PR04-01 | yes | projection must support equipment parity; both `HpRegenPerTurn` and Task A `pain_fuel` `StatModifier.hpRegen` collapse into the same `DerivedStats.hpRegen` display line |
| `OnHitStatusProc` | no official talent in PR04-01 | yes | projection must include a PR04-01 template for equipment parity; official talent data cannot use it in this PR |
| `TerrainAffinityBonus` | no official talent in PR04-01 | yes | projection must include a PR04-01 template for fixture-only resolver coverage; official talent data cannot use it in this PR |
| `DamageVsTag` | no official talent in PR04-01 | yes | projection must include a PR04-01 template for equipment parity; official talent data cannot use it in this PR |

Authority rules：

1. `core` owns passive typed model and deterministic resolver semantics.
2. `game` owns schema loading, official talent data, session wiring and passive source collection.
3. `client` owns presentation only; it must not know how `PassiveEffect` changes stats or damage.
4. `tools` owns coverage lint and report artifacts.
5. `PassiveEffect` must be a behavior-preserving superset of the current equipment passive family: `OnHitStatusProc`, `OnKillResourceRestore`, `ConditionalStatBonus`, `TerrainAffinityBonus`, `DamageVsTag`, `DamageVsStatus`, `HpRegenPerTurn`, `DamageTypeBonus`, and `ResistanceBonus`.
6. `sourceId` is the passive source identity used inside the resolver. In PR04-01, equipment source ids are legacy-stable template ids because the current equipment source shape has no runtime item instance id; talent source ids are talent ids. `sourceTemplateId` is the stable gameplay/template identity used in logs and traces. For equipment, `sourceTemplateId` is the item base id; for talent, it is the talent id. For affixes, `affixId` remains separate.
7. During `04-01a`, any existing `EquipmentPassive` schema surface is limited to loader input compatibility; loader boundary must convert it to `PassiveEffect`. Runtime resolver accepts only `PassiveSource`.
8. `04-01a` cannot complete until every existing equipment passive kind has a parity test proving old behavior and stable-key identity are preserved.

### 2.2 Talent schema contract

`TalentLevelEffect` gains:

```kotlin
val passiveEffects: List<PassiveEffect> = emptyList()
```

YAML shape:

```yaml
levelEffects:
  1:
    passiveEffects:
      - kind: StatModifier
        statModifier: { maxHp: 8, defense: 1 }
      - kind: ConditionalStatBonus
        condition: SELF_HAS_STATUS
        statusId: GUARD_STANCE_BUFF
        statModifier: { defense: 1 }
      - kind: DamageTypeBonus
        damageType: HOLY
        bonusPercent: 0.03
      - kind: OnKillResourceRestore
        resourceType: MANA
        amount: 2
```

Lint rules:

1. `category: PASSIVE` must pair with `kind: PASSIVE`.
2. `PASSIVE` must declare non-empty `passiveEffects` for every rank from `1..maxPoints`.
3. `PASSIVE` must have `cooldown: 0`.
4. `PASSIVE` must have no `resourceCosts`.
5. `PASSIVE` `callbacks` field has exactly two valid serialized states: field absent or `callbacks: []`; non-empty `callbacks` are forbidden.
6. `PASSIVE` must have no `telegraphRef`.
7. `PASSIVE` must not use active-only fields: `damageMultiplier`, `knockback`, `rangeBonus`, `healFraction`, `resourceRestoreFraction`, `associatedEffects`, `cleanseEffect`, `effectOps`.
8. `ACTIVE / SUSTAINED` must not declare `passiveEffects` in PR04-01. Hybrid active+passive talents require a separate explicit contract before they can be added.

### 2.3 Passive resolver contract

`PassiveEffectResolver` must accept `List<PassiveSource>` and return source-aware results. PR04-01 uses the conservative stable-key strategy: equipment cue keys keep their existing literal format; new source-kind key format is introduced only for talent passives.

1. Damage adjustment sources retain `PassiveSourceKind`.
2. On-hit / on-kill triggers retain `sourceId`, `sourceTemplateId`, `sourceKind`, nullable `affixId`, and nullable `talentRank`.
3. Stat adjustment combines equipment and talent passive modifiers deterministically.
4. Resistance profile combines base resistance, equipment passive resistance and talent passive resistance through the same path.
5. `OnHitStatusProc`, `TerrainAffinityBonus`, and `DamageVsTag` are equipment parity blockers even if Task B does not assign them to talent data.

Stable key decision table:

| source/effect | PR04-01 key contract |
| --- | --- |
| equipment `HpRegenPerTurn` | keep `passive:hp_regen:<itemBaseId>` |
| equipment `OnHitStatusProc` | keep `passive:on_hit_status:<itemBaseId>:<statusId>` |
| equipment `OnKillResourceRestore` | keep `passive:on_kill_resource_restore:<itemBaseId>:<resourceType>` |
| equipment damage bonus vs tag | keep `passive:damage_bonus:<itemBaseId>:vs_tag:<tag>` |
| equipment damage bonus vs status | keep `passive:damage_bonus:<itemBaseId>:vs_status:<statusId>` |
| equipment damage type bonus | keep `passive:damage_bonus:<itemBaseId>:type:<damageType>` |
| talent stat/resistance/passive detail only | no frontstage cue unless it changes a visible combat/log event |
| talent trigger/damage cue | use `passive:talent:<talentId>:<effectKind>:rank<rank>` |

Trace contract:

1. Existing equipment trace fields remain readable: `sourceItemBaseId`, `sourceAffixId`, `sourceSpecialTemplateId`.
2. PR04-01 adds source-kind/template identity for talent passive traces without renaming existing equipment trace fields in the same slice.
3. No replay/log schema migration is allowed for existing equipment passive cue keys in PR04-01.
4. Talent passive cue keys are current-rank only. On learn, rank-up, respec or save-load refresh, any old `passive:talent:<talentId>:<effectKind>:rank<oldRank>` key for the same talent must be absent from the next frontstage trace frame.
5. Runtime passive trigger logs use source-aware event keys. On-kill talent resource restore emits `log.passive.on_kill_resource_restore` with `sourceKind=TALENT`, `sourceTemplateId=<talentId>` and `talentRank=<rank>`. Conditional activation/deactivation emits `log.passive.conditional_bonus_activated` / `log.passive.conditional_bonus_deactivated` with the same source identity fields.

Forbidden:

1. `resolveTalentPassiveDamageAdjustment` as a parallel implementation.
2. Reusing `EquippedPassiveSource.item.baseId` for talent source identity.
3. Treating talent id strings as localized display text.
4. Collecting passive effects by scanning UI detail rows.

Aggregation and hp regen single-path contract:

1. Equipment passive source aggregation is kind-specific and fixed by the table below. Resolver output must preserve source-aware trace rows where a visible trigger/log event exists.
2. Talent passive sources are unique by `talentId`: at most one `PassiveSourceKind.TALENT` source exists per learned talent, and it must use the current learned rank only.
3. Rank-up, respec and save/load materialization replace the old talent-rank passive source before derived stats are recomputed. The old and new rank for the same `talentId` must never be visible in the same derived-stat frame.

Aggregation per kind:

| passive kind | aggregation across sources | trace / log policy |
| --- | --- | --- |
| `StatModifier` | additive per stat field before derived-stat calculation | no frontstage trigger row unless a changed stat is rendered in detail |
| `ConditionalStatBonus` | additive per `(condition, statusId?, stat field)` while the condition is active | activation/deactivation logs preserve source identity |
| `TerrainAffinityBonus` | additive per `(terrainTag, stat field)` while actor stands on matching terrain | fixture-only trace rows preserve source identity |
| `ResistanceBonus` | additive per `damageType`; no resistance cap is introduced in PR04-01 | source-aware detail and trace rows retain source identity |
| `DamageTypeBonus` | additive percent per `damageType`; final damage multiplier uses `1.0 + sum(bonusPercent)` | one damage trace row per contributing source |
| `DamageVsStatus` | additive percent per `statusId`; final damage multiplier uses `1.0 + sum(bonusPercent)` | one damage trace row per contributing source |
| `DamageVsTag` | additive percent per `tag`; final damage multiplier uses `1.0 + sum(bonusPercent)` | one damage trace row per contributing source |
| `OnKillResourceRestore` | additive effective restore across sources because each source restores its own `amount` on the same player kill | one source-aware log row per source that restored a positive amount |
| `OnHitStatusProc` | independent roll per source; chances are not maxed, summed or collapsed | one source-aware trigger row per successful source roll |
| `HpRegenPerTurn` | additive raw amount, then collapses into the single hp regen path below | same final UI line as `StatModifier.hpRegen` |

`PassiveEffectResolverTest.equipmentAndTalentDamageTypeBonusUsesAdditivePercentNotMultiplicative`, `PassiveEffectResolverTest.twoEquippedSourcesOnKillRestoreMergeAdditively`, and `PassiveEffectResolverTest.twoOnHitStatusProcSourcesRollIndependently` own this table.

4. `StatModifier.hpRegen` and `HpRegenPerTurn.amount` are not parallel healing systems. PR04-01 migrates hp regen into one core combat formula, not a display-only projection. Both inputs contribute to a raw hp regen sum, and `DerivedStats.hpRegen` is the effective value after `DiminishingReturns.effectiveHpRegen(rawHpRegen)`:

```text
rawHpRegen =
  profile.baseHpRegen
  + effectiveStats.con * 0.2
  + sum(passive StatModifier.hpRegen)
  + sum(passive HpRegenPerTurn.amount as Double)

DerivedStats.hpRegen = DiminishingReturns.effectiveHpRegen(rawHpRegen)
```

5. `StatsCalculator` owns the raw-to-effective hp regen route. `FoundationGameSession` must not independently heal `EquipmentPassive.HpRegenPerTurn` after this migration; it reads the already-derived stat and applies one turn-start heal.
6. Turn-start healing amount is `DerivedStats.hpRegen.roundToInt().coerceAtLeast(0)`. If the actor has a heal-disabled or healing-suppression status in current rules, the same runtime branch suppresses the whole turn-start hp regen amount.
7. Legacy `log.passive.hp_regen` cue policy is deterministic after the single-path migration:
   - Keep the existing equipment cue key format `passive:hp_regen:<itemBaseId>`.
   - Emit the legacy cue only when at least one equipment `HpRegenPerTurn` source contributed raw hp regen and the final turn-start heal restored `> 0` HP.
   - Attribute effective healing to equipment hp regen sources by proportional raw contribution:

```text
equipmentSourceEffectiveHeal =
  turnStartHealAmount * equipmentSourceRawHpRegen / totalRawHpRegen
```

   - When multiple equipment hp regen sources exist, round per-source cue amounts with largest-remainder allocation so the sum of equipment cue amounts equals the equipment-attributable healed amount. Talent `StatModifier.hpRegen` does not reuse the equipment cue key; it is visible through typed detail and source-aware derived stat tests.
8. UI detail renders one `hpRegen` line after aggregation and DR. It displays the same effective value used by turn-start healing, not the raw sum, and must not show separate "equipment hp regen" and "talent hp regen" rows for the same final derived stat.
9. Official talent schema loading fails fast if the same passive rank declares both `HpRegenPerTurn` and `StatModifier.hpRegen`. Authors must choose one hp regen display route so future content cannot create duplicate hp regen detail rows.

### 2.4 Runtime contract

Runtime rules:

1. Learned passive sources are derived from `TalentLoadout.talentLevels`.
2. Rank `N` uses exactly `levelEffects[N].passiveEffects`; rank `1..N-1` are not accumulated.
3. Learning, rank-up, respec, save load, equipment change, status change, health threshold change and terrain movement all refresh relevant passive stat context.
4. Status expiry and re-application must re-emit passive projection when the focused talent has `ConditionalStatBonus`; `GUARD_STANCE_BUFF` expiry / re-application is the PR04-01 blocking example.
5. PASSIVE talent never enters `slotToTalentId`.
6. `EquipTalentToSlot` rejects PASSIVE.
7. Learning PASSIVE never opens `ACTIVE_TALENT_SLOT_CHOICE`.
8. Save/replay persists talent id/rank, not resolved passive values.

Required refresh tests:

1. `FoundationGameSessionTest.conditionalTalentPassiveRefreshesWhenStatusExpires`
2. `FoundationGameSessionTest.healthThresholdTalentPassiveRecomputesAfterDamageAndHealing`
3. `FoundationGameSessionTest.respecRemovesTalentPassiveDerivedStats`
4. `PassiveEffectResolverTest.equipmentAndTalentStatModifiersDoNotDoubleAccumulateAfterRefresh`
5. `FoundationGameSessionTest.fixtureTerrainAffinityPassiveRefreshesAfterMovement`
6. `PassiveEffectResolverTest.terrainAffinityPassiveRefreshesWhenContextTerrainChanges`

`TerrainAffinityBonus` has no official PR04-01 talent. The terrain movement tests are fixture-only coverage for source-agnostic resolver completeness and must not add an official terrain-affinity profession passive in this PR.

### 2.5 UI presentation contract

PR04-01 consumes PR04 detail order:

1. hero icon + header
2. rank / cost / prerequisite
3. current rank detail
4. next rank preview
5. actions

Snapshot owner:

1. `TalentPassiveDetailSnapshot`, `PassiveDetailLineSnapshot`, `PassiveDetailDeltaLineSnapshot`, `PassiveDetailLineKindSnapshot`, and `PassiveDetailLineToneSnapshot` live in `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`.
2. `game` builds these snapshots in `FoundationGameSession` / `SessionSnapshotMapper` projection.
3. `game` owns semantic-to-label projection for stat/resource/damage/status/condition/terrain/tag args.
4. `client` maps `PassiveDetailLineToneSnapshot` to `TalentPreviewToneToken` and renders `labelKey + valueToken`; it must not inspect raw passive ids or maintain passive arg label mappings.
5. `diagnosticEffectKind` and `diagnosticArgs` are diagnostic only for tests, whitebox and typed assertions. Client display must not read them.

Game/session boundary must project passive detail before client rendering:

```kotlin
@Serializable
data class TalentPassiveDetailSnapshot(
    val currentLines: List<PassiveDetailLineSnapshot>,
    val nextLines: List<PassiveDetailDeltaLineSnapshot>,
)

@Serializable
enum class PassiveDetailLineKindSnapshot {
    STAT_MODIFIER,
    ON_KILL_RESOURCE_RESTORE,
    CONDITIONAL_STAT_BONUS,
    DAMAGE_VS_STATUS,
    DAMAGE_TYPE_BONUS,
    RESISTANCE_BONUS,
    HP_REGEN_PER_TURN,
    ON_HIT_STATUS_PROC,
    TERRAIN_AFFINITY_BONUS,
    DAMAGE_VS_TAG,
)

@Serializable
enum class PassiveDetailLineToneSnapshot {
    SECONDARY,
    POSITIVE,
    WARNING,
}

@Serializable
data class PassiveDetailLineSnapshot(
    val lineKind: PassiveDetailLineKindSnapshot,
    val labelKey: String,
    val valueToken: RenderTextTokenSnapshot,
    val diagnosticArgs: Map<String, String>,
    val sortKey: String,
    val tone: PassiveDetailLineToneSnapshot,
    val diagnosticEffectKind: String,
)

@Serializable
data class PassiveDetailDeltaLineSnapshot(
    val lineKind: PassiveDetailLineKindSnapshot,
    val labelKey: String,
    val valueToken: RenderTextTokenSnapshot,
    val diagnosticArgs: Map<String, String>,
    val sortKey: String,
    val tone: PassiveDetailLineToneSnapshot,
    val diagnosticEffectKind: String,
)
```

Projection rules:

1. `game` builds `TalentPassiveDetailSnapshot` from `TalentDef.levelEffects[N].passiveEffects`.
2. Delta calculation happens at `game` / snapshot projection boundary, not in renderer.
3. `client` localizes `line.labelKey` and renders `line.valueToken`; it does not inspect `PassiveEffect` classes, raw ids or `diagnosticArgs` for gameplay or display decisions.
4. Unknown serialized passive kind fails fast at schema loader / contract lint boundaries. Projection must be exhaustive over every known `PassiveEffect` subtype and fail if a known kind has no detail template mapping; covered by `SessionSnapshotMapperTest.passiveDetailProjectionHandlesEveryKnownPassiveKind` and `SessionSnapshotMapperTest.passiveDetailProjectionFailsWhenKnownKindHasNoTemplateMapping`.
5. `valueToken.arguments` uses `valueKey` for localizable domain labels (`statId`, `damageType`, `resourceType`, `condition`, `terrainTag`, monster `tag`, `statusId`) and `value` for already formatted numeric strings such as `+10`, `+3%`, `0`, `+1.0`.
6. `diagnosticArgs` preserves raw ids and raw formatted values (`statId=maxHp`, `damageType=PHYSICAL`, `resourceType=MANA`, `condition=SELF_HAS_STATUS`, `statusId=GUARD_STANCE_BUFF`, `before`, `after`) for owner assertions only.
7. Locale text owns labels only. Numeric values, status ids, damage types and resource types come from typed args projected by `game`.
8. Multi-stat effects become one line per non-zero stat, so row order is never derived from `Map` iteration.
9. All passive detail DTOs and enums in `core.snapshot` are `@Serializable` to match `RenderSnapshot` inspection, golden and contract tooling.

Line template table:

| passive kind | current value token | delta value token | required token args | ordering rule |
| --- | --- | --- | --- | --- |
| `StatModifier` | one line per non-zero stat | one line per changed stat | `statId`, `value`; delta adds `before`, `after` | fixed stat order from the stat display coverage table |
| `HpRegenPerTurn` | one shared hp regen line after §2.3 aggregation | one changed shared hp regen line | `amount`; delta adds `before`, `after` | same display line as `StatModifier.hpRegen`; do not create a second hp regen row |
| `OnKillResourceRestore` | one trigger line | one changed amount line | `amount`, `resourceType` | single line |
| `ConditionalStatBonus` | one `CONDITIONAL_STAT_BONUS` line per non-zero stat, each prefixed by `condition` / `statusId?` in typed args; no separate header line | one changed conditional stat line per changed stat | `condition`, `statusId?`, `statId`, `value`; delta adds `before`, `after` | fixed stat order after condition args |
| `DamageVsStatus` | one damage line | one changed percent line | `statusId`, `percent` | single line |
| `DamageTypeBonus` | one damage line | one changed percent line | `damageType`, `percent` | damage type enum order when multiple `DamageTypeBonus` lines exist |
| `ResistanceBonus` | one resistance line | one changed amount line | `damageType`, `value` | damage type enum order |
| `OnHitStatusProc` | one trigger line | one changed chance/duration line | `statusId`, `chancePercent`, `duration`, `magnitude?` | single line |
| `TerrainAffinityBonus` | one `TERRAIN_AFFINITY_BONUS` line per non-zero stat, each prefixed by `terrainTag` in typed args; no separate header line | one changed terrain stat line per changed stat | `terrainTag`, `statId`, `value`; delta adds `before`, `after` | fixed stat order after terrain args |
| `DamageVsTag` | one damage line | one changed percent line | `tag`, `percent` | single line |

Line density contract:

1. Right detail must render no more than 10 passive effect lines in current detail and no more than 10 lines in next preview for one talent at one rank.
2. The current official PR04-01 data must not clip inside the PR04 right-detail content area: `mana_surge` renders 4 current lines, `balance_point` renders 4 current lines, `bulwark_march` rank 1 renders exactly 3 current lines (`GUARD_STANCE_BUFF` defense, `GUARD_STANCE_BUFF` speed, `TAUNT` damage), `last_stand` rank 1 renders exactly 2 current lines (`HP_BELOW_30` attack damage, `HP_BELOW_30` defense), and no official talent exceeds 5 current lines. If localized text still exceeds the visible viewport, the existing PR04 scrollbar must scroll the detail content.
3. Any passive data added after PR04-01 that would exceed 10 lines must combine homogeneous typed rows in `SessionSnapshotMapper` before the renderer, using one summary line with typed aggregate args such as `damageTypes` and `percent`. The renderer must not decide aggregation by measuring text.
4. `TalentSidebarPresenterTest.passiveDetailLineCountPerTalentDoesNotExceedTen` and `contractLint.passiveLineCountBudget` own this budget.

Stat display coverage:

Locale token policy is fixed for PR04-01:

1. Primary attributes reuse existing `ui.stat.*` labels.
2. HUD-visible derived stats reuse existing `ui.hud.*.short` labels, matching current item inspect stat modifier rendering.
3. Modifier-only passive detail labels reuse existing `ui.inspect.mod.*`; missing modifier-only labels must be added under `ui.inspect.mod.*` in both `en-US` and `zh-CN`.
4. Do not create a parallel full `ui.stat.*` namespace for passive modifiers.
5. The table row order below is the final stat render order for `StatModifier`, `ConditionalStatBonus` and `TerrainAffinityBonus`. Do not sort by locale label, YAML authoring order or map iteration order.

| statId | format | official PR04-01 data | required label key |
| --- | --- | --- | --- |
| `str` | signed integer | no | `ui.stat.str` |
| `dex` | signed integer | no | `ui.stat.dex` |
| `con` | signed integer | no | `ui.stat.con` |
| `wil` | signed integer | `devotion` | `ui.stat.wil` |
| `maxHp` | signed integer | `unyielding`, `devotion` | `ui.hud.hp.short` |
| `maxStamina` | signed integer | no | `ui.hud.stamina.short` |
| `attack` | signed integer | no | `ui.hud.attack.short` |
| `defense` | signed integer | `unyielding`, `balance_point`, `bulwark_march`, `last_stand` | `ui.hud.defense.short` |
| `accuracy` | signed integer | `killer_instinct` | `ui.hud.accuracy.short` |
| `evasion` | signed integer | no | `ui.hud.evasion.short` |
| `speed` | signed integer | `bulwark_march` | `ui.hud.speed.short` |
| `castSpeedRating` | signed decimal effective value, one fractional digit | `arcane_overload`, `balance_point` | `ui.inspect.mod.cast_speed` |
| `hpRegen` | signed decimal, one fractional digit | `pain_fuel` as `StatModifier.hpRegen` | `ui.inspect.mod.hp_regen` |
| `staminaRegen` | signed decimal, one fractional digit | no | `ui.inspect.mod.stamina_regen` |
| `critChance` | signed percent | `killer_instinct` | `ui.inspect.mod.crit` |
| `talentPower` | signed percent | `arcane_overload`, `devotion`, `balance_point` | `ui.inspect.mod.talent` |
| `attackMultiplierBonus` | signed percent displayed as attack damage bonus | `killer_instinct`, `pain_fuel`, `balance_point`, `last_stand` | `ui.inspect.mod.attack_multiplier_bonus` |
| `defenseMultiplierBonus` | signed percent displayed as defense multiplier bonus | no | `ui.inspect.mod.defense_multiplier_bonus` |

The passive detail projector supports every field in `StatModifier` / `SchemaStatModifier`. Unsupported stat fields are not allowed in PR04-01; adding a new stat field requires updating this table, locale labels and mapper tests in the same change.

Effective value route for stat display and EV:

| statId | PR04-01 calculation route | UI detail displays |
| --- | --- | --- |
| `castSpeedRating` | raw rating enters `DiminishingReturns.effectiveCastSpeed(raw)` | effective cast speed rounded to one decimal; raw rating may appear only as diagnostic text outside player-facing detail |
| `hpRegen` | raw regen sum enters `DiminishingReturns.effectiveHpRegen(rawHpRegen)` | effective hp regen rounded to one decimal, matching turn-start healing |
| `critChance` | direct probability added to base crit chance and clamped by `StatsCalculator`; no crit-rating DR in PR04-01 | direct percent value |
| `evasion` | existing derived-stat integer route; official PR04-01 data does not modify evasion | signed integer |

`DiminishingReturns.effectiveCritRating` and `DiminishingReturns.effectiveEvasion` remain existing helpers but are not used by PR04-01 passive data. Any future PR that routes `critChance` through crit rating or routes evasion through DR must update this table, §3.0 EV declarations, and mapper tests in the same change.

PASSIVE detail rules:

1. Type line renders `Passive`.
2. Active-only lines are hidden: cooldown, range, targeting, knockback, resource cost, active slot choice.
3. Current rank detail renders passive effects as keyword-style lines.
4. Next rank preview renders exact deltas with preview tone.
5. If current rank is 0, current detail previews rank 1 passive effects: these are the values gained if the player learns now.
6. If current rank is 0, next preview displays rank 1 -> rank 2 deltas: these are the values gained on the next rank after learning. If `maxPoints == 1`, next preview is hidden.
7. Description text cannot be the only source of numeric passive effects.

### 2.6 PASSIVE action matrix

Right-pane action affordances are category-specific:

| category | state | allowed actions | forbidden actions |
| --- | --- | --- | --- |
| `PASSIVE` | `LOCKED` any subreason: `PREREQ`, `LEVEL`, `POINTS`, `FROZEN` | `Esc Back`; locked reason if present | `R` shortcut, `PlayerCommand.RespecTalentTree`, reserve draft mutation, active slot replacement, footer `RESERVE` hint |
| `PASSIVE` | `LEARNABLE` rank 0 | `Enter Learn`, `Esc Back` | `R` shortcut, `PlayerCommand.RespecTalentTree`, reserve draft mutation, active slot replacement, footer `RESERVE` hint |
| `PASSIVE` | learned rank `< maxRank` | `Enter Rank Up`, `Esc Back` | `R` shortcut, `PlayerCommand.RespecTalentTree`, reserve draft mutation, active slot replacement, footer `RESERVE` hint |
| `PASSIVE` | learned rank `== maxRank` | `Esc Back`; max-rank status | `R` shortcut, `PlayerCommand.RespecTalentTree`, reserve draft mutation, active slot replacement, footer `RESERVE` hint |
| `ACTIVE` / `SUSTAINED` | learnable or learned | existing PR04 learn/rank/reserve/slot behavior | `N/A` |

`R` is category-gated by the currently focused node. Current PR04 code maps Talent Assign `R` to `PlayerCommand.RespecTalentTree`; PR04-01 changes PASSIVE focus so `R` is ignored for PASSIVE and emits no `PlayerCommand.RespecTalentTree`, no `ConfirmTalentDraftToReserve`, no `EquipTalentToSlot`, no reserve draft mutation and no `ACTIVE_TALENT_SLOT_CHOICE`. Runtime `EquipTalentToSlot` rejects PASSIVE even if a command is constructed manually.

Footer / legend policy is focused-node dynamic: when the focused node is `PASSIVE`, `TalentSidebarPresenter.footerHints()` must not include `TalentAssignFooterHintKind.RESERVE` even if the same visible tree also contains `ACTIVE` or `SUSTAINED` nodes. `RESERVE` may appear again only after focus moves to an `ACTIVE` or `SUSTAINED` node that supports the upstream PR04 behavior.

No-mutation assertion surface:

1. `InputHandlerTest.passiveTalentAssignRShortcutDoesNotEmitRespecCommand` owns command suppression, including the current `PlayerCommand.RespecTalentTree` path.
2. `InputHandlerTest.passiveLockedByPrereqRShortcutDoesNotEmitRespecCommand` owns locked-prereq command suppression.
3. `TalentSidebarPresenterTest.passiveFocusedFooterDoesNotShowReserveHint` owns footer hint suppression.
4. `FoundationGameSessionTest.passiveRShortcutLeavesDraftLoadoutAndRanksUnchanged` owns state suppression.
5. The state test must snapshot and compare `TalentAllocationDraft.pendingRanks`, `TalentAllocationDraft.previousPendingRanks`, `TalentLoadout.slotToTalentId`, and learned talent ranks before and after pressing `R` on a focused PASSIVE row.

`TalentAllocationDraft.pendingRanks`, `TalentAllocationDraft.previousPendingRanks`, and `TalentLoadout.slotToTalentId` are upstream PR04 Talent Assign state fields. PR04-01 consumes them only for no-mutation assertions and must not redefine their ownership.

Mandatory tests:

1. `TalentSidebarPresenterTest.passiveActionsDoNotShowReserveOrActiveSlotManagement`
2. `TalentSidebarPresenterTest.passiveFocusedFooterDoesNotShowReserveHint`
3. `InputHandlerTest.passiveTalentAssignRShortcutDoesNotEmitRespecCommand`
4. `InputHandlerTest.passiveLockedByPrereqRShortcutDoesNotEmitRespecCommand`
5. `TalentSidebarPresenterTest.passiveLockedByLevelShowsLockedActionsOnly`
6. `FoundationGameSessionTest.passiveRShortcutLeavesDraftLoadoutAndRanksUnchanged`
7. `FoundationGameSessionTest.equipTalentToSlotRejectsPassive`

## 3. Data Coverage

### 3.0 Passive attractiveness contract

Round 3 玩法评估结论：旧表中的 `maxHp +8`、`talentPower +0.04`、`hpRegen +0.2` 这类单纯小数值被动，不足以和一个主动按钮竞争。PR04-01 的最终被动表必须改成“常驻身份 + 条件/触发回报”的组合，满足类 ToME / Roguelike 长局构筑经验：被动不是少按一个键，而是让玩家的轮转、资源、状态目标或风险曲线发生长期变化。

Attractiveness floor:

1. 每个 playable 职业的 2 个被动必须一静一动：一个提供常驻身份锚点，一个提供条件、触发、资源或状态 payoff。
2. Rank 1 必须已经可感知，不能只是 UI 数字装饰；至少影响生存、伤害、资源循环、速度/施法节奏或状态 payoff 之一。
3. Rank 3 必须足以改变加点判断：玩家能明确知道继续点它会改善哪条战斗 loop。
4. Rank 5 必须提供职业身份级收益，数值目标是接近一个中等装备词条组合或一个主动 buff 的长期平均价值。
5. 任何被动如果只提供单字段小幅加成，必须新增第二条效果或改成触发/条件 payoff。
6. 被动详情必须显示“为什么值得点”：常驻收益、触发条件、触发收益和下一 rank 增量都要可见。

PR04-01 不新增节点的原因是 12 个既有转换目标都有明确职业身份，重写效果后能满足吸引力地板。实现不得回退到 Round 2 的弱数值表。

Passive EV, uptime and trigger-owner contract:

`rank5NormalizedEv` is a design-owned declared relative value score for comparing passives against each other and against a medium active buff over long-run play. It is not a runtime formula and PR04-01 does not freeze a cross-stat EV weight table. PR04-01 uses `100` as the target anchor, accepts declared values in `[80, 120]`, and fails lint outside that range. Conditional and trigger passives must declare a trigger owner and estimated uptime so the value is not hidden in prose.

Machine-checkable EV lint is intentionally limited and named as declaration lint:

1. `TalentSchemaTest.passiveRank5EvAnchorDeclaredWithinTwentyPercent` and `acceptanceContractLint` parse this table and fail if a listed passive has declared `rank5NormalizedEv < 80` or `> 120`.
2. `TalentSchemaTest.passiveEvDrRoutesDeclaredForRatingFields` fails if any passive using `castSpeedRating` or `hpRegen` lacks an explicit effective-value route in §2.5 / this section.
3. `TalentSchemaTest.conditionalPassiveUptimeIsDeclaredInDoc` and `TalentSchemaTest.conditionalPassivesDeclareTriggerOwnerInDoc` fail on missing trigger owner, missing uptime, or vague owners such as only "combat".
4. Machine lint does not claim to recompute the whole balance EV from YAML. The doc-vs-implementation self-audit must include a manual EV audit row for every passive, using the declared score, uptime and trigger-owner notes as review evidence.

EV declarations use effective values, not raw authoring values, for fields with a DR route. `castSpeedRating` contributes `effectiveCastSpeed(raw) - baseEffectiveCastSpeed`, `hpRegen` contributes `effectiveHpRegen(rawHpRegenWithPassive) - effectiveHpRegen(rawHpRegenWithoutPassive)`, `critChance` contributes direct probability because PR04-01 keeps it as a direct percent field, and `attackMultiplierBonus` / `talentPower` stack additively by same dimension. These are design anchors for balance review; runtime calculations remain owned by `StatsCalculator` and combat resolver.

| profession | passive | role | trigger owner | estimated uptime | rank5NormalizedEv | blocking value check |
| --- | --- | --- | --- | --- | --- | --- |
| Vanguard | `unyielding` | evergreen frontline identity | `N/A` | `1.00` | `104` | rank 1 gives hp, defense and physical resistance |
| Vanguard | `bulwark_march` | guarded movement and taunt payoff | `guard_stance` grants `GUARD_STANCE_BUFF`; taunt loop owns `TAUNT` | `0.50` | `98` | detail shows guarded defense, speed and TAUNT damage |
| Arcanist | `arcane_overload` | evergreen spell rotation | `N/A` | `1.00` | `108` | rank 1 gives both spell power and cast speed |
| Arcanist | `mana_surge` | elemental kill-chain | `OnKillResourceRestore MANA`: any player kill; `DamageTypeBonus`: FIRE/COLD/LIGHTNING damage events | `0.35` | `100` | detail shows on-kill MANA plus FIRE/COLD/LIGHTNING damage |
| Rogue | `killer_instinct` | evergreen crit/execution bias | `N/A` | `1.00` | `102` | rank 1 gives accuracy, crit and attack damage |
| Rogue | `deathblow` | marked-target execution | `DamageVsStatus MARKED`: `shadowstep` rank >= 2 `shadowstep_marked` or `shadow_bind` rank >= 4 `shadow_bind_marked`; `OnKillResourceRestore ENERGY`: any player kill | `0.35` | `96` | detail shows MARKED damage plus ENERGY restore |
| Templar | `devotion` | evergreen holy growth | `N/A` | `1.00` | `100` | rank 1 gives hp, WIL and talent power |
| Templar | `beacon_of_zeal` | holy kill-chain | `DamageTypeBonus HOLY`: holy damage events from `holy_strike` / `holy_aura`; `OnKillResourceRestore POSITIVE_ENERGY`: any player kill | `0.25` | `96` | rank 1 POSITIVE_ENERGY restore is at least `+2` |
| Berserker | `pain_fuel` | evergreen blood offense | `N/A` | `1.00` | `100` | rank 1 gives attack damage and shared hp regen |
| Berserker | `last_stand` | low-health risk payoff | `HP_BELOW_30` condition from current health ratio | `0.12` | `94` | rank 1 low-health damage is at least `+10%` |
| Spellblade | `balance_point` | evergreen hybrid stance | `N/A` | `1.00` | `104` | rank 1 touches spell, melee, defense and cast rhythm |
| Spellblade | `flux_anchor` | lightning kill-chain | `DamageTypeBonus LIGHTNING`: lightning damage from `arcane_edge` / `flux_burst`; `OnKillResourceRestore MANA`: any player kill | `0.35` | `98` | detail shows LIGHTNING damage/resistance plus MANA restore |

EV lint rules:

1. `TalentSchemaTest.passiveRank5EvAnchorDeclaredWithinTwentyPercent` and `acceptanceContractLint` must parse this table and fail if a listed passive has declared `rank5NormalizedEv < 80` or `> 120`.
2. `TalentSchemaTest.conditionalPassiveUptimeIsDeclaredInDoc` must fail if any `ConditionalStatBonus`, `DamageVsStatus` or `OnKillResourceRestore` official passive lacks a non-`N/A` uptime.
3. `TalentSchemaTest.conditionalPassivesDeclareTriggerOwnerInDoc` must fail if trigger owner is vague, missing, or only says "combat".
4. Rank 1 effective value for each passive must be at least half of the other passive in the same profession; no profession is allowed to have one obvious trap passive at rank 1.
5. `TalentSchemaTest.passiveEvDrRoutesDeclaredForRatingFields` must fail if any `castSpeedRating` / `hpRegen` passive lacks the effective route declaration above or if implementation tests show UI/detail values are rendered from raw authoring values.

High-value payoff classification:

PR04-01 marks these `PassiveEffect` outputs as high-value payoff. Detail projection and tests must explain every official occurrence with a dedicated typed template and numeric args:

1. `OnKillResourceRestore` for any resource type.
2. `ConditionalStatBonus` for any condition.
3. `DamageVsStatus`.
4. `DamageTypeBonus`.
5. `ResistanceBonus`.
6. `StatModifier.attackMultiplierBonus`.
7. `StatModifier.hpRegen`.
8. `StatModifier.castSpeedRating`.
9. `StatModifier.talentPower`.

Other supported fields and equipment-parity kinds remain visible but supportive. `SessionSnapshotMapperTest.passiveDetailExplainsEveryHighValuePassivePayoff` and `TalentSchemaTest.passiveHighValuePayoffClassificationIsComplete` must parse this list rather than infer "high value" from EV score, class name, or prose.

Resource pool baseline:

| resource | owning PR04-01 profession(s) | current profession YAML pool | typical active spend from current talent YAML | rank 1 restore floor |
| --- | --- | --- | --- | --- |
| `MANA` | `arcanist`, `spellblade` | Arcanist `60/60`; Spellblade `52/52` | Arcanist common spells `9-18`; Spellblade common mana skills `4-16` | `mana_surge +3` is `5%` of Arcanist pool; `flux_anchor +3` is `5.8%` of Spellblade pool |
| `ENERGY` | `rogue` | Rogue `100/100` with `PER_TURN +5` and `ON_HIT +8` | Rogue active spend `8-24` | `deathblow +4` is `4%` of pool and must be paired with MARKED damage at rank 1 |
| `POSITIVE_ENERGY` | `templar` | Templar starts `32/100`, regenerates on damage taken / hit and decays out of combat | Templar active spend `10-18` | `beacon_of_zeal +2` is `6.25%` of starting current energy and must remain at least `+2` |

`TalentSchemaTest.passiveOnKillResourceRestoreRank1IsAtLeastFivePercentOfStartingPool` is enforced for `MANA` and `POSITIVE_ENERGY`; `ENERGY` is exempt from the 5% pool threshold only when the same rank also has `DamageVsStatus MARKED >= +8%`, as `deathblow` does. `MANA` has no paired-payoff exemption in PR04-01. Any future resource passive that fails this table must raise rank 1 restore or add a separately documented exemption and matching test name before implementation.

Value audit:

| 职业 | passive | old risk | final decision |
| --- | --- | --- | --- |
| Vanguard | `unyielding` | 旧 `maxHp/defense` 偏装备词条化 | 增强为生命、防御、物理抗性三段式 frontline identity |
| Vanguard | `bulwark_march` | 只给 guarded speed/defense 时不够像构筑 payoff | 追加 `DamageVsStatus TAUNT`，让 guard + taunt loop 有进攻回报 |
| Arcanist | `arcane_overload` | 单纯 `talentPower +0.04` 不值得替代主动技能 | 增强为 spell power + cast speed，强化全 spell rotation |
| Arcanist | `mana_surge` | 只回少量 mana 不够有吸引力 | 改成 on-kill mana + elemental damage bonus，形成 kill-chain caster loop |
| Rogue | `killer_instinct` | accuracy + crit 可读但偏线性 | 加入 attack damage bonus，确保 crit build 有常驻收益 |
| Rogue | `deathblow` | 原 marked bonus 可以成立但数值偏保守 | 提升 marked damage 与 on-kill energy，变成 execution loop |
| Templar | `devotion` | maxHp + low talentPower 偏弱，且和 `beacon_of_zeal` HOLY resistance 重叠 | 改成 maxHp + WIL + talentPower，让 faith identity 提供成长，不重复 HOLY resistance |
| Templar | `beacon_of_zeal` | holy damage/resistance 数值偏低 | 加入 positive-energy on-kill restore，支撑 holy kill-chain |
| Berserker | `pain_fuel` | `hpRegen +0.2` 与 `HpRegenPerTurn` 容易混淆且过弱 | 明确为 `StatModifier.hpRegen`，提高 attack damage 与 regen |
| Berserker | `last_stand` | low-health bonus 有身份但原值保守 | 提高 low-health attack/defense，成为可赌血的高价值被动 |
| Spellblade | `balance_point` | 小额三属性分散，缺少 hybrid payoff | 提升 spell/melee/defense 三向收益，形成 hybrid stance |
| Spellblade | `flux_anchor` | lightning + mana 值偏低 | 提升 lightning damage、mana on kill 与 lightning resistance |

### 3.1 Task A - static passive set

Task A must be implemented first and independently testable.

| 职业 | tree | talentId | new category/kind | rank 1 | rank 2 | rank 3 | rank 4 | rank 5 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Vanguard | `vanguard_warcry` | `unyielding` | `PASSIVE` / `PASSIVE` | `maxHp +10`, `defense +1`, `ResistanceBonus PHYSICAL +1` | `maxHp +16`, `defense +2`, `ResistanceBonus PHYSICAL +2` | `maxHp +22`, `defense +3`, `ResistanceBonus PHYSICAL +3` | `maxHp +30`, `defense +4`, `ResistanceBonus PHYSICAL +4` | `maxHp +40`, `defense +5`, `ResistanceBonus PHYSICAL +6` |
| Arcanist | `arcanist_arcane` | `arcane_overload` | `PASSIVE` / `PASSIVE` | `talentPower +0.06`, `castSpeedRating +1` | `talentPower +0.09`, `castSpeedRating +2` | `talentPower +0.12`, `castSpeedRating +3` | `talentPower +0.15`, `castSpeedRating +4` | `talentPower +0.20`, `castSpeedRating +5` |
| Rogue | `rogue_agility` | `killer_instinct` | `PASSIVE` / `PASSIVE` | `accuracy +2`, `critChance +0.04`, `attackMultiplierBonus +0.03` | `accuracy +3`, `critChance +0.06`, `attackMultiplierBonus +0.05` | `accuracy +4`, `critChance +0.08`, `attackMultiplierBonus +0.07` | `accuracy +5`, `critChance +0.10`, `attackMultiplierBonus +0.09` | `accuracy +7`, `critChance +0.14`, `attackMultiplierBonus +0.12` |
| Templar | `templar_faith` | `devotion` | `PASSIVE` / `PASSIVE` | `maxHp +8`, `wil +1`, `talentPower +0.03` | `maxHp +14`, `wil +2`, `talentPower +0.05` | `maxHp +20`, `wil +3`, `talentPower +0.07` | `maxHp +26`, `wil +4`, `talentPower +0.09` | `maxHp +34`, `wil +5`, `talentPower +0.12` |
| Berserker | `berserker_bloodwar` | `pain_fuel` | `PASSIVE` / `PASSIVE` | `attackMultiplierBonus +0.05`, `hpRegen +0.4` | `attackMultiplierBonus +0.08`, `hpRegen +0.6` | `attackMultiplierBonus +0.11`, `hpRegen +0.8` | `attackMultiplierBonus +0.14`, `hpRegen +1.0` | `attackMultiplierBonus +0.18`, `hpRegen +1.2` |
| Spellblade | `spellblade_elemental_flux` | `balance_point` | `PASSIVE` / `PASSIVE` | `talentPower +0.04`, `attackMultiplierBonus +0.03`, `defense +1`, `castSpeedRating +2` | `talentPower +0.06`, `attackMultiplierBonus +0.05`, `defense +2`, `castSpeedRating +3` | `talentPower +0.08`, `attackMultiplierBonus +0.07`, `defense +3`, `castSpeedRating +4` | `talentPower +0.10`, `attackMultiplierBonus +0.09`, `defense +4`, `castSpeedRating +6` | `talentPower +0.12`, `attackMultiplierBonus +0.12`, `defense +5`, `castSpeedRating +8` |

### 3.2 Task B - resolver-backed passive set

Task B is mandatory in this PR and shares the same completion gate as Task A.

| 职业 | tree | talentId | passive effects | rank 1 | rank 2 | rank 3 | rank 4 | rank 5 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Vanguard | `vanguard_shield` | `bulwark_march` | `ConditionalStatBonus SELF_HAS_STATUS=GUARD_STANCE_BUFF`, `DamageVsStatus TAUNT` | `defense +2`, `speed +1`, `TAUNT damage +4%` | `defense +3`, `speed +2`, `TAUNT damage +6%` | `defense +4`, `speed +3`, `TAUNT damage +8%` | `defense +5`, `speed +4`, `TAUNT damage +10%` | `defense +6`, `speed +5`, `TAUNT damage +12%` |
| Arcanist | `arcanist_arcane` | `mana_surge` | `OnKillResourceRestore MANA`, `DamageTypeBonus FIRE`, `DamageTypeBonus COLD`, `DamageTypeBonus LIGHTNING` | `MANA +3`, elemental damage `+3%` | `MANA +4`, elemental damage `+4%` | `MANA +5`, elemental damage `+5%` | `MANA +7`, elemental damage `+6%` | `MANA +9`, elemental damage `+8%` |
| Rogue | `rogue_assassination` | `deathblow` | `DamageVsStatus MARKED`, `OnKillResourceRestore ENERGY` | `MARKED damage +8%`, `ENERGY +4` | `MARKED damage +12%`, `ENERGY +6` | `MARKED damage +16%`, `ENERGY +8` | `MARKED damage +20%`, `ENERGY +10` | `MARKED damage +25%`, `ENERGY +12` |
| Templar | `templar_faith` | `beacon_of_zeal` | `DamageTypeBonus HOLY`, `ResistanceBonus HOLY`, `OnKillResourceRestore POSITIVE_ENERGY` | `HOLY damage +5%`, `HOLY resistance +3`, `POSITIVE_ENERGY +2` | `HOLY damage +7%`, `HOLY resistance +4`, `POSITIVE_ENERGY +3` | `HOLY damage +9%`, `HOLY resistance +6`, `POSITIVE_ENERGY +4` | `HOLY damage +12%`, `HOLY resistance +8`, `POSITIVE_ENERGY +5` | `HOLY damage +15%`, `HOLY resistance +10`, `POSITIVE_ENERGY +7` |
| Berserker | `berserker_bloodwar` | `last_stand` | `ConditionalStatBonus HP_BELOW_30` | `attackMultiplierBonus +0.10`, `defense +2` | `attackMultiplierBonus +0.14`, `defense +3` | `attackMultiplierBonus +0.18`, `defense +5` | `attackMultiplierBonus +0.24`, `defense +6` | `attackMultiplierBonus +0.30`, `defense +8` |
| Spellblade | `spellblade_elemental_flux` | `flux_anchor` | `DamageTypeBonus LIGHTNING`, `ResistanceBonus LIGHTNING`, `OnKillResourceRestore MANA` | `LIGHTNING damage +5%`, `LIGHTNING resistance +2`, `MANA +3` | `LIGHTNING damage +7%`, `LIGHTNING resistance +3`, `MANA +4` | `LIGHTNING damage +9%`, `LIGHTNING resistance +4`, `MANA +5` | `LIGHTNING damage +12%`, `LIGHTNING resistance +5`, `MANA +6` | `LIGHTNING damage +15%`, `LIGHTNING resistance +7`, `MANA +8` |

Task B trigger availability map:

| passive | required trigger / condition | owner that makes it self-reachable | implementation proof |
| --- | --- | --- | --- |
| `bulwark_march` | `SELF_HAS_STATUS=GUARD_STANCE_BUFF`, damage vs `TAUNT` | `guard_stance` rank >= 1 grants `GUARD_STANCE_BUFF`; Vanguard taunt loop owns `TAUNT` application | `FoundationGameSessionTest.conditionalTalentPassiveRefreshesWhenStatusExpires` |
| `mana_surge` | player kill plus elemental caster loop | On-kill MANA restore triggers on any player kill with no damage-type filter; Arcanist elemental attacks own FIRE / COLD / LIGHTNING damage events for the damage bonus | `FoundationGameSessionTest.talentPassiveOnKillRestoreUsesTalentSource`; `FoundationGameSessionTest.manaSurgeOnKillRestoreTriggersOnNonElementalKill`; `FoundationGameSessionTest.manaSurgeDamageTypeBonusDoesNotApplyToNonElementalDamage` |
| `deathblow` | damage vs `MARKED`, player kill | On-kill ENERGY restore triggers on any player kill; MARKED damage requires `shadowstep` rank >= 2 `shadowstep_marked` or `shadow_bind` rank >= 4 `shadow_bind_marked` | `FoundationGameSessionTest.deathblowMarkedSourceRequiresShadowstepRankTwoRoute` |
| `beacon_of_zeal` | HOLY damage, HOLY resistance, player kill | On-kill POSITIVE_ENERGY restore triggers on any player kill; Templar `holy_strike` / `holy_aura` own HOLY damage events for the damage bonus | `FoundationGameSessionTest.beaconOfZealHolyDamageSourceIsReachableFromStarterTemplarLoadout` |
| `last_stand` | `HP_BELOW_30` | current player health ratio owns condition state | `FoundationGameSessionTest.healthThresholdTalentPassiveRecomputesAfterDamageAndHealing` |
| `flux_anchor` | LIGHTNING damage, LIGHTNING resistance, player kill | On-kill MANA restore triggers on any player kill; Spellblade `arcane_edge` / `flux_burst` own LIGHTNING damage events for the damage bonus | `FoundationGameSessionTest.fluxAnchorLightningSourceIsReachableFromStarterSpellbladeLoadout` |

Same-dimension stacking audit:

| profession | same-dimension passive stack at rank 5 | cap / decision |
| --- | --- | --- |
| Vanguard | `defense +5`, PHYSICAL resistance `+6`; conditional `defense +6`, speed `+5`, TAUNT damage `+12%` | no same attack multiplier stack; guarded stats are conditional and acceptable |
| Arcanist | `talentPower +0.20`, cast speed `+5`; elemental damage `+8%` | spell throughput is split between general power, cast rhythm and typed elemental damage; EV table caps it at `108` |
| Rogue | attack damage `+12%`; MARKED damage `+25%` | general and marked-only multipliers are separate dimensions; marked uptime gate is `0.35` |
| Templar | `devotion` has no HOLY resistance; `beacon_of_zeal` owns HOLY resistance `+10` | duplicate HOLY resistance removed; faith growth uses WIL and talentPower |
| Berserker | evergreen attack damage `+18%`; low-health attack damage `+30%` | combined same-dimension peak is `+48%`, below PR04-01 passive soft cap `+50%` |
| Spellblade | attack damage `+12%`, talentPower `+12%`, cast speed `+8`; LIGHTNING damage `+15%` | hybrid channels are intentionally split; no single generic damage dimension exceeds `+15%` outside attack bonus |

`TalentSchemaTest.sameDimensionPassiveStackPerProfessionDoesNotExceedSoftCap` owns this table. PR04-01 soft caps are `attackMultiplierBonus <= +50%` when all same-profession passives are simultaneously active, `talentPower <= +25%`, and damage-type bonus per `DamageType <= +20%`. Same-dimension stacking is additive percent: `attackMultiplierBonus +0.18` and `+0.30` produce `base * (1 + 0.48)`, not `base * 1.18 * 1.30`.

### 3.3 Profession portfolio audit

The selected 12 conversions are fixed for PR04-01. Implementation must not swap candidates without revising this document and rerunning `acceptanceContractLint`.

Active slot rule: `PLAYER_ACTIVE_TALENT_SLOT_COUNT` is `4`. After PR04-01 conversion, every starter loadout still has `3` active talents equipped, so each profession has one empty active slot for the next learned active talent. A fifth active talent must use the existing PR04 active slot replacement flow; PASSIVE talents must never consume that slot.

| 职业 | before | after | activeSlotCount | starter active count after conversion | portfolio rule |
| --- | --- | --- | --- | --- | --- |
| Vanguard | `16 ACTIVE / 0 PASSIVE / 0 SUSTAINED` | `14 ACTIVE / 2 PASSIVE / 0 SUSTAINED` | `4` | `3` | keeps melee, guard, charge, taunt, warcry buttons; passive pair provides durability and guarded movement |
| Arcanist | `16 ACTIVE / 0 PASSIVE / 0 SUSTAINED` | `14 ACTIVE / 2 PASSIVE / 0 SUSTAINED` | `4` | `3` | keeps elemental, blink, shield and control buttons; passive pair converts resource/burst support into spell mastery plus kill-chain mana |
| Rogue | `16 ACTIVE / 0 PASSIVE / 0 SUSTAINED` | `14 ACTIVE / 2 PASSIVE / 0 SUSTAINED` | `4` | `3` | keeps backstab, stealth, roll, execution and poison/utility buttons; passive pair anchors crit identity and marked-target payoff |
| Templar | `16 ACTIVE / 0 PASSIVE / 0 SUSTAINED` | `14 ACTIVE / 2 PASSIVE / 0 SUSTAINED` | `4` | `3` | keeps strike, heal, shield, aura and cleanse buttons; passive pair anchors holy growth and holy damage/resource identity |
| Berserker | `12 ACTIVE / 0 PASSIVE / 0 SUSTAINED` | `10 ACTIVE / 2 PASSIVE / 0 SUSTAINED` | `4` | `3` | keeps rush, hew, frenzy, slam and rupture buttons; passive pair moves pain/last-stand identity into risk-state payoff |
| Spellblade | `12 ACTIVE / 0 PASSIVE / 0 SUSTAINED` | `10 ACTIVE / 2 PASSIVE / 0 SUSTAINED` | `4` | `3` | keeps arcane edge, lunge, parry, blink strike and sigil buttons; passive pair anchors hybrid balance and lightning kill-chain payoff |

Conversion audit:

| talentId | active identity before PR04-01 | why conversion is valid | tactical verb retained by |
| --- | --- | --- | --- |
| `unyielding` | panic self-buff | Vanguard already has active guard buttons; this becomes evergreen frontline durability | `guard_stance`, `iron_wall`, `taunt` |
| `bulwark_march` | guarded movement / guard buff | conditional `SELF_HAS_STATUS=GUARD_STANCE_BUFF` preserves the guarded-march loop without adding a button and is reachable from `guard_stance` rank 1 | `guard_stance`, `charge`, `linebreaker` |
| `arcane_overload` | self resource/burst support | Arcanist needs a spell mastery identity more than another self-buff button | `fireball`, `meteor`, `void_breach`, `arcane_shield` |
| `mana_surge` | active mana recovery | kill-chain mana is a clearer passive resource loop; no active resource button remains in this tree by design | `blink`, `arcane_shield`, elemental attacks |
| `killer_instinct` | active crit buff | Rogue crit identity becomes always readable in the tree | `backstab`, `stealth`, `roll`, `eviscerate` |
| `deathblow` | active finisher | `execution` remains the active finisher; `deathblow` becomes marked-target passive payoff | `execution`, `poison_blade`, `shadowstep` |
| `devotion` | active faith buff | Templar faith becomes a persistent identity anchor | `holy_light`, `holy_shield`, `sanctuary`, `purify` |
| `beacon_of_zeal` | active aura / holy buff | holy damage/resistance is better as long-term zeal payoff; active AoE remains elsewhere | `holy_aura`, `consecration`, `judgment_hammer` |
| `pain_fuel` | active heal / empower | Berserker pain identity becomes always visible and avoids another panic self-buff | `blood_rush`, `kill_frenzy`, `slaughter_drive` |
| `last_stand` | active panic button | low-health conditional payoff preserves last-stand identity without slot pressure | `blood_rush`, `pursuit_drive`, `fault_line` |
| `balance_point` | active resource/buff | Spellblade equilibrium identity belongs in passive build bias | `spell_parry`, `mana_lunge`, `flux_reversal` |
| `flux_anchor` | active resource restore | lightning on-kill loop is a clearer hybrid resource cadence | `arcane_edge`, `flux_burst`, `blink_strike` |

### 3.4 Data rewrite rules

For every talent converted to PASSIVE:

1. Set `category: PASSIVE`.
2. Set `kind: PASSIVE`.
3. Set `cooldown: 0`.
4. Set `castTime: INSTANT`.
5. Set `targeting: SELF`.
6. Remove `resourceCosts`.
7. Remove `callbacks` or keep `callbacks: []`; non-empty `callbacks` are forbidden for PASSIVE.
8. Remove `telegraphRef`.
9. Replace active `levelEffects` with `passiveEffects`.
10. Update `tags` and `keywords` to include `passive` and remove misleading active-only tags such as `buff` when the talent no longer applies a timed buff.
11. Update `descKey` localized text in `en-US` and `zh-CN` so the wording says permanent / passive / on kill / while guarded instead of "activate/cast".

## 4. Implementation Tasks

### Task 1 - Passive model and schema

Slice: `04-01a-passive-contract-parity`. This task must not change official talent data.

Production scope:

- `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt`
- `core/src/main/kotlin/com/ktome/core/talent/TalentModels.kt`
- `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt`
- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`

Required changes:

1. Add source-agnostic `PassiveEffect`, `PassiveSourceKind`, `PassiveSource` with `sourceTemplateId` in the existing `core.item` legacy physical package.
2. Add `TalentLevelEffect.passiveEffects`.
3. Add schema parser for `passiveEffects`; serialized kind `StatModifier` maps to Kotlin `PassiveEffect.StatModifierEffect`.
4. Map all existing equipment passive schema kinds to `PassiveEffect`: `OnHitStatusProc`, `OnKillResourceRestore`, `ConditionalStatBonus`, `TerrainAffinityBonus`, `DamageVsTag`, `DamageVsStatus`, `HpRegenPerTurn`, `DamageTypeBonus`, `ResistanceBonus`.
5. Add fail-fast loader errors for unsupported passive kind or invalid passive fields.
6. Add a negative fixture for unknown passive kind.
7. Do not add a second runtime passive hierarchy under talent or equipment packages; equipment and talent sources must share the same `PassiveEffect` sealed model.

Validation:

- `SchemaV2LoaderTest.loadsTalentPassiveEffects`
- `SchemaV2LoaderTest.loadsFixtureHpRegenStatModifierWithoutHpRegenPerTurn`
- `TalentSchemaTest.passiveTalentsDeclarePassiveEffectsOnly`
- `PassiveEffectResolverTest.equipmentPassiveParityCoversAllExistingKinds`

### Task 2 - Resolver and runtime source collection

Slice: `04-01a-passive-contract-parity` for equipment parity; `04-01b-static-profession-passives` for talent source collection.

Production scope:

- `core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt`
- `core/src/main/kotlin/com/ktome/core/stats/StatsCalculator.kt`
- `core/src/main/kotlin/com/ktome/core/ecs/Components.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`

Required changes:

1. Change resolver inputs from equipment-only source to `PassiveSource`.
2. Rename `EquipmentPassiveStatModifier` to `PassiveStatModifier` in `core/src/main/kotlin/com/ktome/core/ecs/Components.kt`; production runtime code must not reference `EquipmentPassiveStatModifier` after `04-01a`.
3. Collect equipment passive sources and talent passive sources separately, then merge at resolver boundary.
4. Refresh player derived stats after learn, rank up, respec and save/load materialization.
5. Preserve existing equipment passive behavior and tests.
6. Keep equipment source stable keys behavior-identical by using `sourceTemplateId=item.baseId`.
7. Use `sourceTemplateId=talentId` and `talentRank=rank` for talent passives.
8. Migrate every existing equipment-passive caller to `PassiveSource`: `FoundationGameSession` passive collection, `StatsCalculator` passive stat input, combat damage adjustment, on-hit/on-kill trigger emission, log/trace emission, and save/load materialization refresh. No `EquippedPassiveSource` compatibility wrapper remains on a production runtime path after `04-01a`.
9. Production and loader code construct `PassiveSource` directly. The only allowed non-production adapter is a test fixture builder helper. No `EquippedPassiveSource` compatibility adapter is allowed in `FoundationGameSession`, `StatsCalculator`, combat trigger paths, or save/load materialization. Any retained fixture helper must be annotated `@Deprecated("Remove in PR04-02 or core.passive-cleanup")` and covered by `ContractLintTest.internalPassiveSourceAdaptersAreAnnotatedDeprecatedWithRemovalOwner`.
10. Migrate hp regen from dual equipment/talent behavior into the §2.3 core formula: `StatsCalculator` feeds `DiminishingReturns.effectiveHpRegen(rawHpRegen)`, and `FoundationGameSession` applies only `DerivedStats.hpRegen` at turn start.
11. Preserve the legacy equipment hp regen cue policy from §2.3 after migration; do not keep a second direct-heal branch only to preserve `log.passive.hp_regen`.

Validation:

- `PassiveEffectResolverTest.resolvesEquipmentAndTalentSourcesTogether`
- `PassiveEffectResolverTest.preservesPassiveSourceKindInTriggers`
- `PassiveEffectResolverTest.equipmentPassiveParityCoversAllExistingKinds`
- `PassiveEffectResolverTest.talentPassiveCollectionContainsExactlyOneEntryPerTalentIdAfterRankUp`
- `PassiveEffectResolverTest.respecFromRank5ToRank1KeepsOnlyRank1Effects`
- `PassiveEffectResolverTest.hpRegenStatModifierAndHpRegenPerTurnCollapseIntoSingleDerivedStat`
- `PassiveEffectResolverTest.passiveHpRegenSumFedThroughEffectiveHpRegenBeforeDisplay`
- `StatsCalculatorTest.hpRegenUsesEffectiveHpRegenAfterPassiveAggregation`
- `FoundationGameSessionTest.preservesLegacyEquipmentPassiveCueKeys`
- `FoundationGameSessionTest.legacyEquipmentHpRegenCuePolicyIsPreservedAfterSinglePathMigration`
- `FoundationGameSessionTest.hpRegenTickAppliesEffectiveValueOnceAndRespectsHealDisabledStatus`
- `FoundationGameSessionTest.learningStaticPassiveUpdatesDerivedStatsWithoutSlotChange`
- `FoundationGameSessionTest.passiveTraceStableKeyReplacesPreviousRankOnRankUp`
- `FoundationGameSessionTest.saveLoadDerivesPassiveEffectsFromTalentRanks`

### Task 3 - Task A static data

Slice: `04-01b-static-profession-passives`.

Production scope:

- `game/src/main/resources/data/talents/index.yaml`
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`

Required changes:

1. Convert the 6 Task A talents to PASSIVE.
2. Apply exact rank table from §3.1 and the attractiveness floor from §3.0.
3. Update localized descriptions and detail tokens.
4. Ensure active slot modal is suppressed for these talents.
5. Encode `pain_fuel.hpRegen` as `StatModifier.hpRegen`; do not encode it as `HpRegenPerTurn`.

Validation:

- `FoundationGameSessionTest.passiveRankUpgradeReplacesPreviousRankEffects`
- `TalentSidebarPresenterTest.passiveTalentDoesNotRenderActiveSlotChoiceAction`
- `SchemaV2LoaderTest.loadsPainFuelHpRegenAsStatModifierNotHpRegenPerTurn`
- `SessionSnapshotMapperTest.painFuelPassiveDisplayValueMatchesEffectiveHpRegenAfterDr`

### Task 4 - Task B resolver-backed data

Slice: `04-01c-trigger-conditional-passives`.

Production scope:

- same as Task 3 plus resolver trigger call sites in `FoundationGameSession`

Required changes:

1. Convert the 6 Task B talents to PASSIVE.
2. Apply exact rank table from §3.2 and the attractiveness floor from §3.0.
3. Wire `OnKillResourceRestore`, `DamageVsStatus`, `DamageTypeBonus`, `ResistanceBonus`, `ConditionalStatBonus`.
4. Make logs/traces distinguish `TALENT` from `EQUIPMENT`.
5. Multi-effect Task B passives must preserve all listed effects at each rank; implementation cannot drop damage/resistance/resource components to simplify projection.

Validation:

- `FoundationGameSessionTest.talentPassiveOnKillRestoreUsesTalentSource`
- `FoundationGameSessionTest.conditionalTalentPassiveRefreshesWhenStatusExpires`
- `FoundationGameSessionTest.healthThresholdTalentPassiveRecomputesAfterDamageAndHealing`
- `FoundationGameSessionTest.deathblowMarkedSourceRequiresShadowstepRankTwoRoute`
- `FoundationGameSessionTest.manaSurgeOnKillRestoreTriggersOnNonElementalKill`
- `FoundationGameSessionTest.manaSurgeDamageTypeBonusDoesNotApplyToNonElementalDamage`
- `FoundationGameSessionTest.beaconOfZealHolyDamageSourceIsReachableFromStarterTemplarLoadout`
- `FoundationGameSessionTest.fluxAnchorLightningSourceIsReachableFromStarterSpellbladeLoadout`
- `FoundationGameSessionTest.fixtureTerrainAffinityPassiveRefreshesAfterMovement`
- `FoundationGameSessionTest.respecRemovesTalentPassiveDerivedStats`
- `FoundationGameSessionTest.talentPassiveDamageBonusUsesUnifiedResolver`
- `PassiveEffectResolverTest.equipmentAndTalentStatModifiersDoNotDoubleAccumulateAfterRefresh`
- `SessionSnapshotMapperTest.lastStandPassiveDetailRendersHpThresholdConditionalLines`

### Task 5 - Talent Assign passive detail

Slice: `04-01b-static-profession-passives` introduces typed detail; `04-01c-trigger-conditional-passives` extends it for trigger/conditional lines.

Production scope:

- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt`
- `client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt`
- `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt`
- `client/src/main/kotlin/com/ktome/client/ui/talent/TalentAssignPanelModel.kt`
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`

Required changes:

1. Add `TalentPassiveDetailSnapshot`, `PassiveDetailLineSnapshot`, `PassiveDetailDeltaLineSnapshot`, `PassiveDetailLineKindSnapshot`, and `PassiveDetailLineToneSnapshot` in `core.snapshot`.
2. Build current/next passive detail in `game` from `TalentDef.levelEffects[rank].passiveEffects`; client receives only snapshot lines.
3. Implement the §2.5 line template table for every supported passive kind; multi-stat passives emit one line per non-zero stat in fixed stat order. `ConditionalStatBonus` uses one `CONDITIONAL_STAT_BONUS` line per stat and must not emit an extra condition header row.
4. Render passive effect lines in current rank detail from typed passive detail lines.
5. Render next rank delta with preview tone from typed delta lines; at rank 0 current detail shows rank 1 values and next preview shows rank 1 -> rank 2 deltas.
6. Hide active-only fields for PASSIVE.
7. Keep PR04 block order intact.
8. Do not compute passive gameplay values in renderer or by parsing locale text.
9. Keep `TalentPreviewToneToken` client-only; `core` and `game` use `PassiveDetailLineToneSnapshot`.
10. Apply the PASSIVE action matrix: no `R` shortcut command, no `PlayerCommand.RespecTalentTree`, no active slot management rows, and no footer `RESERVE` hint while the focused row is PASSIVE.
11. Pressing `R` on a PASSIVE row in normal Talent Assign focus emits no command and causes no reserve draft mutation, no respec mutation, no slot mutation and no learned-rank mutation.
12. Apply the §2.5 locale token policy exactly: primary labels use `ui.stat.*`, HUD-visible derived labels use `ui.hud.*.short`, and modifier-only labels use `ui.inspect.mod.*`.
13. Enforce §2.5 line density: passive current/detail and next preview each stay at or below 10 passive effect lines; overflow content must use the PR04 right-detail scrollbar and the scrollbar must respond to input.
14. Render multi-damage-type lines in `DamageType` enum order and multi-stat lines in the stat display coverage order.
15. Render `hpRegen` and `castSpeedRating` using the effective-value route from §2.5; player-facing detail must not display raw DR input values for those stats.

Validation:

- `SessionSnapshotMapperTest.passiveDetailSnapshotContainsCurrentAndNextLines`
- `SessionSnapshotMapperTest.passiveDetailLineModelUsesDeclaredTemplateArgs`
- `SessionSnapshotMapperTest.passiveDetailProjectionHandlesEveryKnownPassiveKind`
- `SessionSnapshotMapperTest.passiveDetailProjectionFailsWhenKnownKindHasNoTemplateMapping`
- `SessionSnapshotMapperTest.passiveDetailStatOrderMatchesDocumentedTable`
- `SessionSnapshotMapperTest.painFuelPassiveDisplayValueMatchesEffectiveHpRegenAfterDr`
- `SessionSnapshotMapperTest.arcaneOverloadCastSpeedDisplayUsesEffectiveValue`
- `SessionSnapshotMapperTest.lastStandPassiveDetailRendersHpThresholdConditionalLines`
- `SessionSnapshotMapperTest.bulwarkMarchPassiveDetailLineKindsMatchConditionRenderingPolicy`
- `SessionSnapshotMapperTest.unyieldingRank0NextPreviewShowsRank1ToRank2Deltas`
- `TalentSidebarPresenterTest.passiveDetailShowsCurrentAndNextPassiveEffects`
- `TalentSidebarPresenterTest.passiveDetailLineCountPerTalentDoesNotExceedTen`
- `TalentSidebarPresenterTest.passiveDetailFitsViewportAtMinimumWindow`
- `TalentSidebarPresenterTest.passiveActionsDoNotShowReserveOrActiveSlotManagement`
- `TalentSidebarPresenterTest.passiveFocusedFooterDoesNotShowReserveHint`
- `DescriptionPresenterTest.passiveEffectsRenderAsKeywordLines`
- `LocaleLintTest.passiveDetailStatModifierLabelsExist`
- `LocaleLintTest.passiveDetailNonStatLabelsExist`
- `InputHandlerTest.passiveTalentAssignRShortcutDoesNotEmitRespecCommand`
- `FoundationGameSessionTest.passiveRShortcutLeavesDraftLoadoutAndRanksUnchanged`
- `FoundationGameSessionTest.equipTalentToSlotRejectsPassive`

### Task 6 - Coverage lint and docs closure

Slice: `04-01c-trigger-conditional-passives`.

Production scope:

- `tools/src/main/kotlin/com/ktome/tools/**`
- `tools/src/test/kotlin/com/ktome/tools/**`
- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationAction.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationSessionOptions.kt`
- `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
- `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml`
- `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioMaterializationCatalog.kt`
- `game/src/test/kotlin/com/ktome/game/validation/ValidationScenarioRegistryTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt`
- `UI/pr/README.md`
- `UI/pr/screen-coverage-matrix.md`
- this PR document

Required changes:

1. Add playable passive coverage lint.
2. Report `releaseBlocking=[vanguard, arcanist, rogue, templar]`.
3. Report `pr04_01BlockingDevPlayable=[berserker, spellblade]`.
4. Report `excludedFrozen=[shadowblade, warden]`.
5. Fail if any `releaseBlocking` or `pr04_01BlockingDevPlayable` profession has fewer than 2 PASSIVE talents.
6. Fail if PASSIVE talent has no passive effect at any rank.
7. Keep the lint rule available in report-only mode for rollback; do not delete it during rollback.
8. Register all four PR04-01 whitebox scenario ids in `ValidationScenarioRegistry`.
9. Mirror the same four scenario ids in `phase4-v4-scenarios.yaml`.
10. Add materialization catalog entries that generate deterministic `cua-runbook.md`, `manual-record-template.md`, and `expected-evidence.json` for the same four scenario ids.
11. Add registry-owned deterministic scenario setup support for `targetTalentId`, `initialFocusedTalentId`, exact `playerLevel`, granted prerequisite ranks, exact `setUnspentTalentPoints`, `targetRank=0`, `clearPendingTalentDraft=true`, `clearActiveSlotChoiceModal=true`, `resetTalentLoadoutSlotsForTargetOwner=true`, and `previewExpanded=false`; primary focus must not depend on repeated `Down` counts, while secondary same-tree evidence must record exact key input and target assertions.
12. Project `initialFocusedTalentId` through `RenderUiStateSnapshot.validationTalentFocusRequest` and consume it once in `InputHandler.enterTalentAssign()` before the first focused evidence screenshot.
13. Add `ValidationScenarioEvidenceStep.expectedFocusedTalentId`; write it into `expected-evidence.json` and generated runbooks for every PR04-01 evidence step that captures a focused passive detail.
14. Add PR04-01 log evidence files and required log keys to `ValidationScenarioEvidenceSpec`; `expected-evidence.json` must list them from registry-owned evidence data, not from a tools-only side table.
15. Update `UI/pr/screen-coverage-matrix.md` and `UI/pr/README.md` so PR04-01 evidence is discoverable before PR07.
16. Add `contractLint.pr04_01ConvertedTalentsAreExactlyPassive` with the exact 12 talent ids from §3.1 / §3.2; fail if any listed id remains active or if any extra talent is converted without revising this doc.
17. Add `contractLint.damageTypeEnumOrderPinned` and pin `DamageType` order to `PHYSICAL -> FIRE -> COLD -> LIGHTNING -> HOLY -> SHADOW`.
18. Add `contractLint.passiveLineCountBudget` and fail if official PR04-01 current/detail or next preview lines exceed §2.5 budget.
19. Add EV declaration / uptime / trigger-owner checks from §3.0 and same-dimension stack checks from §3.2.

Validation:

- `contractLint.playableProfessionsHaveTwoPassiveTalents`
- `contractLint.passiveTalentCoverageReportsFrozenExclusions`
- `contractLint.pr04_01ConvertedTalentsAreExactlyPassive`
- `contractLint.damageTypeEnumOrderPinned`
- `contractLint.passiveLineCountBudget`
- `TalentSchemaTest.passiveRank5EvAnchorDeclaredWithinTwentyPercent`
- `TalentSchemaTest.passiveEvDrRoutesDeclaredForRatingFields`
- `TalentSchemaTest.conditionalPassiveUptimeIsDeclaredInDoc`
- `TalentSchemaTest.conditionalPassivesDeclareTriggerOwnerInDoc`
- `TalentSchemaTest.sameDimensionPassiveStackPerProfessionDoesNotExceedSoftCap`
- `TalentSchemaTest.passiveOnKillResourceRestoreRank1IsAtLeastFivePercentOfStartingPool`
- `TalentSchemaTest.passiveHighValuePayoffClassificationIsComplete`
- `TalentSchemaTest.passiveStatModifierFieldsDocumentDrRouting`
- `TalentSchemaTest.starterLoadoutFitsInActiveSlotCountAfterPr04_01Conversion`
- `TalentSchemaTest.conditionalPassiveTriggerOwnerReferencesExistingTalentOrBreakpointId`
- `TalentSchemaTest.bulwarkMarchUptimeMatchesGuardStanceDurationCooldownRatio`
- `SchemaV2LoaderTest.rejectsUnknownTalentPassiveEffectKind`
- `DataLoaderContentPackTest.talentOverlayRegistryRemainsUnsupported`
- `ContractLintTest.passiveEffectsCannotDefineNewCoreKinds`
- `ContractLintTest.internalPassiveSourceAdaptersAreAnnotatedDeprecatedWithRemovalOwner`
- `ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosAreRegistered`
- `ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosUseExactInputSequences`
- `ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosProjectPreferredFocusThroughSnapshot`
- `ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosDeclareStructuredTypedAndVisibleAssertions`
- `ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosDeclareLogEvidenceAndForbiddenFragments`
- `RenderSnapshotContractTest.pr04_01ValidationFocusRequestIsSerialized`
- `InputHandlerTest.enterTalentAssignConsumesValidationPreferredFocusOnce`
- `Phase4V4WhiteboxScenarioCliTest.pr04_01PassiveTalentScenariosGenerateRunbooks`
- `Phase4V4WhiteboxScenarioCliTest.pr04_01ExpectedEvidenceIncludesFocusAssertionsAndLogContracts`
- `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail`
- `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail`
- `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression`

## 5. Public Interface / Contract Changes

| Contract | Change | Compatibility |
| --- | --- | --- |
| `TalentLevelEffect` | adds `passiveEffects` | additive runtime model change; old talents default empty |
| talent YAML schema | adds `levelEffects.*.passiveEffects` | additive for non-passive talents; PASSIVE requires it |
| passive resolver | accepts source-agnostic passive sources | behavior-preserving for equipment, expanded for talent |
| `EquippedPassiveSource` | production runtime paths are replaced by `PassiveSource`; only test fixture builder helpers may retain adapter behavior | breaking internal API change; any retained non-production adapter must be `@Deprecated` with removal owner `PR04-02` or `core.passive-cleanup` |
| ECS stat modifier | `EquipmentPassiveStatModifier` is renamed to `PassiveStatModifier` | breaking runtime component rename inside this PR; covered by focused tests and no production reference to `EquipmentPassiveStatModifier` remains after `04-01a` |
| `RenderSnapshot` passive detail enums | adds serializable passive line kind / tone variants | future enum additions require the existing RenderSnapshot version/fallback policy to be updated in the same PR; PR04-01 does not allow silent client fallback for unknown passive line kinds |
| `RenderUiStateSnapshot.validationTalentFocusRequest` | adds one-shot validation focus request for PR04-01 whitebox scenarios | additive inspection-only UI state; consumed once by `InputHandler.enterTalentAssign()` and not persisted to save/replay |
| Talent Assign detail | PASSIVE current/next effect lines | UI visible change; covered by client tests/golden if layout changes |
| contract lint | playable passive coverage rule | new blocking gate for PR04-01 |
| content pack / overlay schema | official talent YAML accepts `passiveEffects`; PR04-01 does not add a runtime content-pack talent overlay registry | `registry: talent` overlays must fail with `content-pack.overlay.registry-unsupported`; unknown passive kind / new core damage type / new resource type fail fast |

No save schema migration is allowed. Save/load persists learned talent ranks only; passive values are derived from current content data on load.

Talent data revision policy:

1. PR04-01 intentionally does not introduce `talentDataRevision`, save migration, or legacy numeric preservation. Current content data is the authority when a run is loaded.
2. After PR04-01 ships, any passive numeric rebalance that changes §3.1 / §3.2 must be a separate PR that either adds an explicit talent data revision / player-facing release note contract or states why current-run derivation remains acceptable.
3. Any data-revision PR must not silently change saved passive outcomes while reusing the PR04-01 acceptance matrix unchanged.

Content-pack rules:

1. Official talent YAML can use `passiveEffects` only with existing `PassiveEffect` kinds and existing core enums.
2. PR04-01 does not add runtime support for content-pack talent overlays.
3. Any content-pack overlay entry with `registry: talent` must fail fast with `content-pack.overlay.registry-unsupported`.
4. Content packs cannot define new `DamageType`, `ResourceType`, `PassiveCondition`, `TerrainTag`, or passive effect kind.
5. Unknown passive effect kind must fail in official schema / loader / contract lint tests.
6. Fixture coverage is fixed to one official valid passive talent fixture and one invalid unknown passive kind fixture; no valid content-pack talent overlay fixture is part of PR04-01.

## 6. UI Detail Requirements

PASSIVE current detail example:

```text
类型：被动
当前等级：2/5
最大生命 +16
防御 +2
物理抗性 +2
```

PASSIVE next preview example:

```text
下一级
最大生命 +16 -> +22
防御 +2 -> +3
物理抗性 +2 -> +3
```

Display rules:

1. Percent fields display as percentages: `critChance +0.05` becomes `Crit Chance +5%`.
2. Multiplier bonus fields display as additive percent: `attackMultiplierBonus +0.10` becomes `Attack Damage +10%`.
3. `castSpeedRating` and `hpRegen` display effective values after the §2.5 DR route, not raw authoring values.
4. `OnKillResourceRestore` displays as `On kill: restore <amount> <resource>`.
5. `DamageVsStatus` displays as `Damage vs <status>: +N%`.
6. `DamageTypeBonus` displays as `<damageType> damage: +N%`; `mana_surge` renders separate FIRE / COLD / LIGHTNING lines in `DamageType` enum order, not YAML authoring order. PR04-01 pins the enum display order to `PHYSICAL -> FIRE -> COLD -> LIGHTNING -> HOLY -> SHADOW`.
7. `ResistanceBonus` displays as `<damageType> resistance: +N`.
8. `ConditionalStatBonus` displays one line per changed stat with condition first in the same line; it does not display a separate condition header.
9. `statusId`, `damageType`, `resourceType` and `terrainTag` values use canonical ids such as `GUARD_STANCE_BUFF`, `MARKED`, `TAUNT`, `HOLY`, `MANA`; localization maps those ids to display text.
10. Unknown serialized passive effect kind must fail during loader / contract lint. `TalentPassiveDetailSnapshot` projection must not silently drop any known `PassiveEffect` subtype; missing template mapping for a known subtype is a projection failure.
11. English / Chinese locale tokens own labels and phrase order only; typed args own all numbers and ids.
12. Current detail and next preview must each stay at or below 10 passive effect lines. When any localized viewport still overflows, the PR04 right-detail scrollbar must be visible and functional; visual tests must not accept clipped passive effects as a pass.
13. PR04-01 contains only positive stat modifiers. The first PR that introduces a negative passive stat modifier must update this section with negative-number localization and render rules before adding that data.

## 7. Manual / Whitebox Expectations

PR04-01 changes player-visible Talent Assign detail and action affordances. `:client:clientSmoke` and `:client:goldenScreenshot` are default gates.

Whitebox scenarios prove player-visible detail, next preview and PASSIVE action suppression. They do not replace runtime trigger tests: `FoundationGameSessionTest.talentPassiveOnKillRestoreUsesTalentSource`, `FoundationGameSessionTest.conditionalTalentPassiveRefreshesWhenStatusExpires` and `FoundationGameSessionTest.healthThresholdTalentPassiveRecomputesAfterDamageAndHealing` are still blocking because they prove combat-time passive behavior.

Packaged app whitebox skip is allowed only when `UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md` records:

1. `scenarioId`,
2. `skipReason`,
3. `blockingCondition` with the exact failed or unavailable command,
4. `replacementEvidence` with exact command, exit code and artifact path,
5. `residualRisk`,
6. `ownerDecision`,
7. exact `clientSmoke` / `goldenScreenshot` commands and results.

Whitebox scenario materialization contract:

Registry-ready runtime specs:

| scenarioId | preset | seed | locale | profession | race | zone | floor | routeIndex | contentPackMode | window |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `dark-uiux-pr04-01-static-passive-detail` | `MAPGEN_DIFF` | `202605170401` | `ZH_CN` | `vanguard` | `human` | `greenwood_fringe` | `2` | `-1` | `NONE` | `1280x840` |
| `dark-uiux-pr04-01-trigger-passive-detail` | `MAPGEN_DIFF` | `202605170402` | `ZH_CN` | `arcanist` | `human` | `greenwood_fringe` | `2` | `-1` | `NONE` | `1280x840` |
| `dark-uiux-pr04-01-passive-action-suppression` | `MAPGEN_DIFF` | `202605170403` | `ZH_CN` | `vanguard` | `human` | `greenwood_fringe` | `2` | `-1` | `NONE` | `1280x840` |
| `dark-uiux-pr04-01-effective-hp-regen-detail` | `MAPGEN_DIFF` | `202605170404` | `ZH_CN` | `berserker` | `human` | `greenwood_fringe` | `2` | `-1` | `NONE` | `1280x840` |

Every scenario must exist in:

1. `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`
2. `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml`
3. `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioMaterializationCatalog.kt`

Scenario setup / focus owner:

```kotlin
data class ValidationScenarioTalentSetupSpec(
    val targetTalentId: String,
    val initialFocusedTalentId: String,
    val playerLevel: Int,
    val prerequisiteRanks: Map<String, Int>,
    val setUnspentTalentPoints: Int,
    val targetRank: Int = 0,
    val clearPendingTalentDraft: Boolean = true,
    val clearActiveSlotChoiceModal: Boolean = true,
    val resetTalentLoadoutSlotsForTargetOwner: Boolean = true,
    val expectedTargetState: String = "LEARNABLE",
    val previewExpanded: Boolean,
)

sealed interface ValidationScenarioTypedAssertion

data class FocusedTalentAssertion(
    val talentId: String,
    val category: String,
    val rank: Int,
    val state: String,
) : ValidationScenarioTypedAssertion

data class PassiveLineAssertion(
    val lineKind: String,
    val statId: String? = null,
    val damageType: String? = null,
    val resourceType: String? = null,
    val statusId: String? = null,
    val condition: String? = null,
    val value: String,
    val orderIndex: Int? = null,
) : ValidationScenarioTypedAssertion

data class LocalizedTextAssertion(
    val locale: String,
    val key: String? = null,
    val visibleTextPolicy: String,
    val evidenceFile: String,
)

data class ValidationScenarioEvidenceStep(
    val mode: String,
    val input: String,
    val expectedVisibleResult: String,
    val evidenceFile: String,
    val expectedFocusedTalentId: String? = null,
    val typedAssertions: List<ValidationScenarioTypedAssertion> = emptyList(),
    val localizedVisibleAssertions: List<LocalizedTextAssertion> = emptyList(),
)

data class ValidationScenarioEvidenceSpec(
    val requiredEvidenceFiles: List<String>,
    val requiredExternalEvidenceFiles: List<String> = emptyList(),
    val cuaSteps: List<ValidationScenarioEvidenceStep>,
    val manualRecordPath: String,
    val requiredLogEventKeys: List<String> = emptyList(),
    val forbiddenLogFragments: List<String> = emptyList(),
    val scenarioNoteLabelKey: String? = null,
)
```

`ValidationScenarioDef` is the single owner of PR04-01 setup and evidence metadata: it gains nullable `talentSetup: ValidationScenarioTalentSetupSpec?`, and `ValidationScenarioDef.toSessionOptions()` copies that setup into `ValidationSessionOptions`. `FoundationGameSession` consumes `ValidationSessionOptions.scenarioTalentSetup` inside the Phase4 v4 preparation hook. `Phase4V4WhiteboxScenarioMaterializationCatalog` and `Phase4V4WhiteboxScenarioCli` only materialize registry-owned data; they must not define a tools-only focus map, setup map or assertion side table.

Focus-by-id crosses the game/client boundary through `RenderSnapshot`, not by mutating client local state from `FoundationGameSession`:

```kotlin
@Serializable
data class TalentAssignPreferredFocusSnapshot(
    val talentId: String,
    val treeId: String,
    val ownerType: String = "PROFESSION",
    val treeOwnerId: String = "",
    val consumeOnceToken: String,
)
```

`RenderUiStateSnapshot` gains `validationTalentFocusRequest: TalentAssignPreferredFocusSnapshot? = null`. `FoundationGameSession` projects it from `ValidationSessionOptions.scenarioTalentSetup.initialFocusedTalentId` after the setup hook has reset ranks, draft state and active-slot modal state. `InputHandler.enterTalentAssign(snapshot)` consumes this request once per `consumeOnceToken`, resolves it to the existing `TalentTreeSelectionIdentity`, writes the modal local selection state, and then ignores the same token on subsequent opens. If the target talent cannot be resolved after setup, materialization fails before generating the runbook.

`expected-evidence.json` must be generated from `ValidationScenarioDef.evidence` plus `talentSetup` and must include `expectedFocusedTalentId`, structured `typedAssertions`, structured `localizedVisibleAssertions`, `requiredLogEventKeys`, and `forbiddenLogFragments` for PR04-01 steps.

Scenario setup contract:

| scenarioId | targetTalentId | initialFocusedTalentId | playerLevel | prerequisite ranks granted before evidence | setUnspentTalentPoints | targetRank | reset state | expectedTargetState at first Talent Assign screenshot | previewExpanded |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `dark-uiux-pr04-01-static-passive-detail` | `unyielding` | `unyielding` | `5` | `intimidation=2` | `1` | `0` | clear draft, clear active-slot modal, reset loadout slots for owner | `LEARNABLE`, rank `0`, not locked | `false` |
| `dark-uiux-pr04-01-trigger-passive-detail` | `mana_surge` | `mana_surge` | `5` | `arcane_shield=2`, `blink=2` | `1` | `0` | clear draft, clear active-slot modal, reset loadout slots for owner | `LEARNABLE`, rank `0`, not locked | `false` |
| `dark-uiux-pr04-01-passive-action-suppression` | `bulwark_march` | `bulwark_march` | `4` | `guard_stance=3` | `1` | `0` | clear draft, clear active-slot modal, reset loadout slots for owner | `LEARNABLE`, rank `0`, not locked | `false` |
| `dark-uiux-pr04-01-effective-hp-regen-detail` | `pain_fuel` | `pain_fuel` | `4` | `kill_frenzy=2` | `1` | `0` | clear draft, clear active-slot modal, reset loadout slots for owner | `LEARNABLE`, rank `0`, not locked | `false` |

The preparation hook must run a post-setup assertion before materialization writes files: target talent rank equals `targetRank`, target state equals `expectedTargetState`, no pending rank mutation exists, no active-slot-choice modal is open, and `validationTalentFocusRequest.talentId == initialFocusedTalentId`. A failed post-setup assertion fails materialization; it must not emit a runbook that depends on the human operator noticing the wrong state.

Implementation path is fixed: PR04-01 scenarios must use scenario setup / bootstrap support for `targetTalentId` and `initialFocusedTalentId`. Initial focus must not rely on repeated `Down` counts from the current tree order; secondary same-tree coverage may use exact key input only when the target row and typed assertions are recorded in `ValidationScenarioEvidenceStep`. `expected-evidence.json` and the generated `cua-runbook.md` must record `expectedFocusedTalentId` for every evidence step that captures a focused passive row. Locked inspection is not acceptance evidence for PR04-01; all primary screenshots use learnable or learned PASSIVE targets so action suppression and next-rank preview are observable without ambiguity.

First-detail contract:

| scenarioId | expected title key | typed assertions | localized visible assertions |
| --- | --- | --- | --- |
| `dark-uiux-pr04-01-static-passive-detail` | `talent.vanguard.unyielding.name` | `FocusedTalentAssertion(talentId=unyielding, category=PASSIVE, rank=0, state=LEARNABLE)`; `PassiveLineAssertion(lineKind=STAT_MODIFIER, statId=maxHp, value=+10, orderIndex=0)`; `PassiveLineAssertion(lineKind=STAT_MODIFIER, statId=defense, value=+1, orderIndex=1)`; `PassiveLineAssertion(lineKind=RESISTANCE_BONUS, damageType=PHYSICAL, value=+1, orderIndex=2)` | `LocalizedTextAssertion(locale=ZH_CN, key=talent.vanguard.unyielding.name, visibleTextPolicy=title-visible, evidenceFile=evidence/passive-detail-static.png)`; localized type line renders Passive; visible numeric values match typed assertions |
| `dark-uiux-pr04-01-trigger-passive-detail` | `talent.arcanist.mana_surge.name` | `FocusedTalentAssertion(talentId=mana_surge, category=PASSIVE, rank=0, state=LEARNABLE)`; `PassiveLineAssertion(lineKind=ON_KILL_RESOURCE_RESTORE, resourceType=MANA, value=+3, orderIndex=0)`; `PassiveLineAssertion(lineKind=DAMAGE_TYPE_BONUS, damageType=FIRE, value=+3%, orderIndex=1)`; `PassiveLineAssertion(lineKind=DAMAGE_TYPE_BONUS, damageType=COLD, value=+3%, orderIndex=2)`; `PassiveLineAssertion(lineKind=DAMAGE_TYPE_BONUS, damageType=LIGHTNING, value=+3%, orderIndex=3)` | `LocalizedTextAssertion(locale=ZH_CN, key=talent.arcanist.mana_surge.name, visibleTextPolicy=title-visible, evidenceFile=evidence/passive-detail-trigger.png)`; on-kill resource wording is localized; FIRE / COLD / LIGHTNING lines appear in `DamageType` enum order |
| `dark-uiux-pr04-01-passive-action-suppression` | `talent.vanguard.bulwark_march.name` | `FocusedTalentAssertion(talentId=bulwark_march, category=PASSIVE, rank=0, state=LEARNABLE)`; `PassiveLineAssertion(lineKind=CONDITIONAL_STAT_BONUS, condition=SELF_HAS_STATUS, statusId=GUARD_STANCE_BUFF, statId=defense, value=+2, orderIndex=0)`; `PassiveLineAssertion(lineKind=CONDITIONAL_STAT_BONUS, condition=SELF_HAS_STATUS, statusId=GUARD_STANCE_BUFF, statId=speed, value=+1, orderIndex=1)`; `PassiveLineAssertion(lineKind=DAMAGE_VS_STATUS, statusId=TAUNT, value=+4%, orderIndex=2)` | `LocalizedTextAssertion(locale=ZH_CN, key=talent.vanguard.bulwark_march.name, visibleTextPolicy=title-visible, evidenceFile=evidence/passive-conditional-detail-before-r.png)`; condition text is localized; visible numeric defense/speed/TAUNT values match typed assertions |
| `dark-uiux-pr04-01-effective-hp-regen-detail` | `talent.berserker.pain_fuel.name` | `FocusedTalentAssertion(talentId=pain_fuel, category=PASSIVE, rank=0, state=LEARNABLE)`; `PassiveLineAssertion(lineKind=STAT_MODIFIER, statId=hpRegen, value=+0.4, orderIndex=0)`; `PassiveLineAssertion(lineKind=STAT_MODIFIER, statId=attackMultiplierBonus, value=+5%, orderIndex=1)` | `LocalizedTextAssertion(locale=ZH_CN, key=talent.berserker.pain_fuel.name, visibleTextPolicy=title-visible, evidenceFile=evidence/passive-hp-regen-effective-detail.png)`; hp regen line uses the §2.5 effective display route |

Required evidence files:

| scenarioId | `requiredEvidenceFiles` |
| --- | --- |
| `dark-uiux-pr04-01-static-passive-detail` | `evidence/passive-static-preview-collapsed-after-toggle.png`, `evidence/passive-detail-static.png`, `evidence/passive-static-next-preview.png`, `evidence/passive-static-after-learn-no-slot-modal.png`, `evidence/passive-static-app.log` |
| `dark-uiux-pr04-01-trigger-passive-detail` | `evidence/passive-trigger-panel-entry.png`, `evidence/passive-detail-trigger.png`, `evidence/passive-cast-speed-effective-detail.png`, `evidence/passive-trigger-next-preview.png`, `evidence/passive-trigger-after-learn-no-slot-modal.png`, `evidence/passive-trigger-app.log` |
| `dark-uiux-pr04-01-passive-action-suppression` | `evidence/passive-conditional-detail-before-r.png`, `evidence/passive-action-preview-expanded.png`, `evidence/passive-no-active-slot-modal.png`, `evidence/passive-action-log-no-reserve.png`, `evidence/passive-action-suppression-app.log` |
| `dark-uiux-pr04-01-effective-hp-regen-detail` | `evidence/passive-hp-regen-effective-detail.png`, `evidence/passive-hp-regen-effective-preview.png`, `evidence/passive-hp-regen-preview-collapsed.png`, `evidence/passive-hp-regen-after-learn-no-slot-modal.png`, `evidence/passive-hp-regen-app.log` |

Log assertion contract:

| scenarioId | requiredLogEventKeys | forbiddenLogFragments |
| --- | --- | --- |
| `dark-uiux-pr04-01-static-passive-detail` | `log.validation.phase4_v4.action`, `log.talent.learned` | `PlayerCommand.RespecTalentTree`, `RespecTalentTree`, `ConfirmTalentDraftToReserve`, `EquipTalentToSlot`, `ACTIVE_TALENT_SLOT_CHOICE` |
| `dark-uiux-pr04-01-trigger-passive-detail` | `log.validation.phase4_v4.action`, `log.talent.learned` | `PlayerCommand.RespecTalentTree`, `RespecTalentTree`, `ConfirmTalentDraftToReserve`, `EquipTalentToSlot`, `ACTIVE_TALENT_SLOT_CHOICE` |
| `dark-uiux-pr04-01-passive-action-suppression` | `log.validation.phase4_v4.action` | `PlayerCommand.RespecTalentTree`, `RespecTalentTree`, `ConfirmTalentDraftToReserve`, `EquipTalentToSlot`, `ACTIVE_TALENT_SLOT_CHOICE`, `log.talent.draft_rollback` |
| `dark-uiux-pr04-01-effective-hp-regen-detail` | `log.validation.phase4_v4.action`, `log.talent.learned` | `PlayerCommand.RespecTalentTree`, `RespecTalentTree`, `ConfirmTalentDraftToReserve`, `EquipTalentToSlot`, `ACTIVE_TALENT_SLOT_CHOICE` |

Source-aware combat trigger logs are blocking focused tests, not PR04-01 CUA evidence: `FoundationGameSessionTest.talentPassiveOnKillRestoreUsesTalentSource`, `FoundationGameSessionTest.manaSurgeOnKillRestoreTriggersOnNonElementalKill`, and `FoundationGameSessionTest.manaSurgeDamageTypeBonusDoesNotApplyToNonElementalDamage` own `log.passive.on_kill_resource_restore` and damage-bonus behavior. The PR04-01 CUA scenarios do not spawn or kill enemies, so their `requiredLogEventKeys` must not list combat trigger keys.

CUA steps must be registry-ready `ValidationScenarioEvidenceStep` rows. The `input` string must contain exact key names, not prose. `ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosUseExactInputSequences` must fail if a PR04-01 step contains vague phrases such as `open`, `select`, `verify`, `scenario fixture grants`, or `if available`. `ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosProjectPreferredFocusThroughSnapshot` must fail if the initial focused detail evidence step omits `expectedFocusedTalentId`, records an id different from the setup table, or lacks a matching `validationTalentFocusRequest`; secondary same-tree effective-display steps must record their own exact `expectedFocusedTalentId`.

CUA key glossary:

| key | fixed meaning in PR04-01 CUA runbook |
| --- | --- |
| `F9` | open Phase4 v4 validation preparation menu |
| `Enter` from validation prep | confirm the registry-owned scenario setup |
| `Esc` from validation prep | return to map mode |
| `T` | open Talent Assign panel |
| `P` | toggle next-rank preview expansion for the focused talent |
| `R` | current PR04 respec/reserve-related shortcut; suppressed for PASSIVE and must not emit `PlayerCommand.RespecTalentTree` |
| `Enter` in Talent Assign | learn or rank up the focused talent when allowed |
| `Down` / `Up` in Talent Assign | move focus to the adjacent row inside the current talent tree; only used for secondary same-tree effective-display evidence |

| scenarioId | mode | input | expectedVisibleResult | evidenceFile |
| --- | --- | --- | --- | --- |
| `dark-uiux-pr04-01-static-passive-detail` | `Keyboard (initial UI mode: MAP; setup focuses unyielding)` | `F9, Enter, Esc, T` | `unyielding` row is focused; right detail shows `Passive`, rank `0`, rank 1 max HP, defense, physical resistance, and no active-slot modal. | `evidence/passive-detail-static.png` |
| `dark-uiux-pr04-01-static-passive-detail` | `Keyboard (same focused row)` | `P` | Next preview is expanded and lists rank 1 to rank 2 deltas for max HP, defense and physical resistance. | `evidence/passive-static-next-preview.png` |
| `dark-uiux-pr04-01-static-passive-detail` | `Keyboard (same focused row)` | `P` | Preview returns to collapsed state after a prior preview expansion; Talent Assign remains focused on `unyielding` and PASSIVE actions/footer remain free of `R` shortcut affordance. | `evidence/passive-static-preview-collapsed-after-toggle.png` |
| `dark-uiux-pr04-01-static-passive-detail` | `Keyboard (same focused row)` | `Enter` | Learning `unyielding` does not open `ACTIVE_TALENT_SLOT_CHOICE`; learned detail still renders PASSIVE current effects. | `evidence/passive-static-after-learn-no-slot-modal.png` |
| `dark-uiux-pr04-01-trigger-passive-detail` | `Keyboard (initial UI mode: MAP; setup focuses mana_surge)` | `F9, Enter, Esc, T` | `mana_surge` row is focused; right detail shows `Passive`, rank `0`, on-kill MANA restore and FIRE / COLD / LIGHTNING damage lines. | `evidence/passive-detail-trigger.png` |
| `dark-uiux-pr04-01-trigger-passive-detail` | `Keyboard (same focused row)` | `P` | Next preview is expanded and lists MANA restore plus FIRE / COLD / LIGHTNING damage deltas in `DamageType` enum order. | `evidence/passive-trigger-next-preview.png` |
| `dark-uiux-pr04-01-trigger-passive-detail` | `Keyboard (same focused row)` | `P` | Preview returns to collapsed state; Talent Assign remains focused on `mana_surge` and no active-slot modal is open. | `evidence/passive-trigger-panel-entry.png` |
| `dark-uiux-pr04-01-trigger-passive-detail` | `Keyboard (same tree after trigger preview)` | `Down` | `arcane_overload` row is focused; right detail renders cast speed as the §2.5 effective decimal value, not the raw rating. | `evidence/passive-cast-speed-effective-detail.png` |
| `dark-uiux-pr04-01-trigger-passive-detail` | `Keyboard (return to trigger row)` | `Up, Enter` | Learning `mana_surge` does not open `ACTIVE_TALENT_SLOT_CHOICE`; detail remains a PASSIVE rule summary. | `evidence/passive-trigger-after-learn-no-slot-modal.png` |
| `dark-uiux-pr04-01-passive-action-suppression` | `Keyboard (initial UI mode: MAP; setup focuses bulwark_march)` | `F9, Enter, Esc, T` | `bulwark_march` row is focused; detail shows `SELF_HAS_STATUS=GUARD_STANCE_BUFF`, defense/speed bonus, and `TAUNT` damage payoff. | `evidence/passive-conditional-detail-before-r.png` |
| `dark-uiux-pr04-01-passive-action-suppression` | `Keyboard (same focused row)` | `P` | Next preview is expanded and lists guarded defense, speed and `TAUNT` damage deltas. | `evidence/passive-action-preview-expanded.png` |
| `dark-uiux-pr04-01-passive-action-suppression` | `Keyboard (same focused row)` | `R` | Pressing `R` on PASSIVE does not emit `PlayerCommand.RespecTalentTree`, does not open `ACTIVE_TALENT_SLOT_CHOICE`, does not show footer `RESERVE`, and does not emit reserve/slot command. | `evidence/passive-no-active-slot-modal.png` |
| `dark-uiux-pr04-01-passive-action-suppression` | `Keyboard (same focused row)` | `Esc` | Returning from Talent Assign restores shell/log state without respec, reserve confirmation, slot replacement or rollback error text. | `evidence/passive-action-log-no-reserve.png` |
| `dark-uiux-pr04-01-effective-hp-regen-detail` | `Keyboard (initial UI mode: MAP; setup focuses pain_fuel)` | `F9, Enter, Esc, T` | `pain_fuel` row is focused; right detail renders hp regen as the §2.5 effective decimal value and does not open an active-slot modal. | `evidence/passive-hp-regen-effective-detail.png` |
| `dark-uiux-pr04-01-effective-hp-regen-detail` | `Keyboard (same focused row)` | `P` | Next preview is expanded and lists hp regen plus attack damage deltas using typed passive detail rows. | `evidence/passive-hp-regen-effective-preview.png` |
| `dark-uiux-pr04-01-effective-hp-regen-detail` | `Keyboard (same focused row)` | `P` | Preview returns to collapsed state after a prior preview expansion; Talent Assign remains focused on `pain_fuel`. | `evidence/passive-hp-regen-preview-collapsed.png` |
| `dark-uiux-pr04-01-effective-hp-regen-detail` | `Keyboard (same focused row)` | `Enter` | Learning `pain_fuel` does not open `ACTIVE_TALENT_SLOT_CHOICE`; learned detail remains PASSIVE. | `evidence/passive-hp-regen-after-learn-no-slot-modal.png` |

Materialization commands:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-effective-hp-regen-detail
```

Run these commands from repo root and execute Gradle serially. The CLI output contract is `cua-runbook.md`, `manual-record-template.md`, `expected-evidence.json`, and `app-executable.sha256` under `build/whitebox/<scenarioId>/`.

Manual execution record path:

```text
UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md
```

Required evidence scenarios:

| Evidence | Path | Required proof |
| --- | --- | --- |
| Task A passive detail | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-detail-static.png` | `unyielding` row selected, type line and rank 1 static effects |
| Task A passive preview | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-next-preview.png` | `unyielding` next-rank max HP, defense and physical resistance deltas are visible |
| Task A passive preview collapsed | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-preview-collapsed-after-toggle.png` | `unyielding` remains focused after preview collapse and PASSIVE footer/action affordances remain suppressed |
| Task A passive after learn | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-after-learn-no-slot-modal.png` | learning `unyielding` does not open `ACTIVE_TALENT_SLOT_CHOICE` and learned detail remains PASSIVE |
| Task A passive log | `build/whitebox/dark-uiux-pr04-01-static-passive-detail/evidence/passive-static-app.log` | required log keys present; forbidden PASSIVE respec / reserve / slot fragments absent |
| Task B passive panel entry | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-panel-entry.png` | `mana_surge` remains focused after preview collapse and no active-slot modal is open |
| Task B passive detail | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-detail-trigger.png` | `mana_surge` row selected, trigger/resource effect line and elemental damage lines |
| Task B cast speed effective detail | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-cast-speed-effective-detail.png` | `arcane_overload` row selected and `castSpeedRating` displays the effective decimal value `+1.0`, not raw `+1` |
| Task B passive preview | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-next-preview.png` | `mana_surge` next-rank MANA restore and elemental damage deltas are visible |
| Task B passive after learn | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-after-learn-no-slot-modal.png` | learning `mana_surge` does not open `ACTIVE_TALENT_SLOT_CHOICE` and learned detail remains PASSIVE |
| Task B passive log | `build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/evidence/passive-trigger-app.log` | required log keys present; forbidden PASSIVE respec / reserve / slot fragments absent |
| Conditional passive detail | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-conditional-detail-before-r.png` | `bulwark_march` row selected, condition line and TAUNT damage payoff |
| Conditional passive preview | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-preview-expanded.png` | `bulwark_march` next-rank guarded defense, speed and TAUNT damage deltas are visible |
| Active slot suppression | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-no-active-slot-modal.png` | `bulwark_march` row selected, pressing `R` emits no `PlayerCommand.RespecTalentTree`, does not open `ACTIVE_TALENT_SLOT_CHOICE`, does not show footer `RESERVE`, and does not mutate draft, slot or learned-rank state |
| Active slot suppression log screenshot | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-log-no-reserve.png` | shell/log screenshot shows no respec, reserve confirmation, slot replacement or rollback error text |
| Active slot suppression app log | `build/whitebox/dark-uiux-pr04-01-passive-action-suppression/evidence/passive-action-suppression-app.log` | required log keys present; forbidden PASSIVE respec / reserve / slot fragments and `log.talent.draft_rollback` absent |
| Effective hp regen detail | `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-effective-detail.png` | `pain_fuel` row selected and `hpRegen` displays through the §2.5 effective display route |
| Effective hp regen preview | `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-effective-preview.png` | `pain_fuel` next-rank hp regen and attack damage deltas are visible |
| Effective hp regen preview collapsed | `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-preview-collapsed.png` | `pain_fuel` remains focused after preview collapse and PASSIVE footer/action affordances remain suppressed |
| Effective hp regen after learn | `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-after-learn-no-slot-modal.png` | learning `pain_fuel` does not open `ACTIVE_TALENT_SLOT_CHOICE` and learned detail remains PASSIVE |
| Effective hp regen app log | `build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/evidence/passive-hp-regen-app.log` | required log keys present; forbidden PASSIVE respec / reserve / slot fragments absent |

Generated whitebox output must not be committed. `UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md` is required when packaged whitebox is executed or skipped, and it must use repo-relative paths.

## 8. Cross-PR Dependencies

| Dependency | Type | Rule |
| --- | --- | --- |
| PR04 Talent Assign | hard upstream | PR04-01 must preserve presenter-owned layout and active slot modal rules |
| PR06 skill icon rebaseline | downstream | PR04-01 does not create icon resources; PR06 still owns formal skill icon replacement |
| PR07 golden polish | downstream | PR07 final all-screens index must include the four PR04-01 PASSIVE evidence labels from `UI/pr/screen-coverage-matrix.md` |
| Phase4 v4 PR01 profession tree run choice | hard upstream | learn/rank semantics and active slot choice boundary are gameplay baseline |

## 9. Rollback Plan

If PR04-01 causes regression:

1. If `04-01a` fails, revert source-agnostic resolver migration and keep existing equipment passive path unchanged.
2. If `04-01a` passes but `04-01b` fails, keep resolver parity only if equipment passive tests remain behavior-identical; revert Task A talent data, typed passive detail, and talent passive runtime together.
3. If `04-01c` fails, revert Task B data and trigger wiring together; Task A remains only when `04-01b` gates are green.
4. Keep passive coverage lint rule in report-only mode after rollback; disable only the blocking threshold.
5. Refresh PR04 Talent Assign golden only after gameplay rollback is complete.

Rollback must not leave mixed state where a talent is `PASSIVE` but has no `passiveEffects`.
Rollback must not leave schema/loader supporting `passiveEffects` while unknown passive kinds are silently accepted.

## 10. Doc-vs-Implementation Self-Audit

PR04-01 close requires filling these fields in PR description or review note:

| Field | Required value |
| --- | --- |
| releaseBlocking professions | `vanguard, arcanist, rogue, templar` |
| pr04_01BlockingDevPlayable professions | `berserker, spellblade` |
| excludedFrozen professions | `shadowblade, warden` |
| passive count by profession | each releaseBlocking and pr04_01BlockingDevPlayable profession has `>= 2` |
| Task A talents implemented | `unyielding, arcane_overload, killer_instinct, devotion, pain_fuel, balance_point` |
| Task B talents implemented | `bulwark_march, mana_surge, deathblow, beacon_of_zeal, last_stand, flux_anchor` |
| passive attractiveness floor | every implemented passive matches §3.0 final table; no talent is reduced to a single weak stat line |
| passive EV declaration | every implemented passive has §3.0 declared `rank5NormalizedEv` in `[80, 120]` |
| manual EV audit | PR description or review note records one balance note per passive explaining why the declared score, uptime and trigger owner make it worth choosing against active talents |
| DR route in EV declaration | `castSpeedRating` and `hpRegen` declarations use effective values from §2.5; `critChance` remains direct percent |
| resource restore baseline | rank 1 on-kill resource restores satisfy §3.0 pool baseline or documented paired-payoff exemption |
| trigger owner and uptime | every conditional/trigger passive has §3.0 trigger owner and estimated uptime |
| same-dimension stack cap | §3.2 stack audit remains true; Berserker combined `attackMultiplierBonus` peak stays `<= +50%` |
| high-value payoff coverage | detail and tests cover every effect kind and stat listed in §3.0 High-value payoff classification |
| resolver authority | equipment and talent passives use the same `PassiveEffectResolver` |
| passive source aggregation | equipment sources add; talent sources are unique by `talentId`; rank-up/respec/save-load replace old rank sources |
| hp regen core formula migration | `StatsCalculator` owns `DiminishingReturns.effectiveHpRegen`, `FoundationGameSession` applies `DerivedStats.hpRegen` once per actor turn, and legacy equipment hp regen cues follow §2.3 attribution |
| aggregation per kind | resolver aggregation matches §2.3 per-kind table, including independent `OnHitStatusProc` rolls and additive on-kill restore |
| ECS passive stat component | `EquipmentPassiveStatModifier` no longer exists on production runtime paths; the unified component is named `PassiveStatModifier` |
| passive model package | PR04-01 keeps source-agnostic passive model in the existing `core.item` legacy physical package and introduces no `core.passive`, `TalentPassiveEffect`, or `EquipmentPassiveEffect` runtime hierarchy |
| stat modifier schema mapping | serialized `kind: StatModifier` maps to Kotlin `PassiveEffect.StatModifierEffect` with payload type `com.ktome.core.item.StatModifier` |
| equipment parity | all 9 existing equipment passive kinds have behavior parity tests |
| equipment source identity | PR04-01 keeps equipment `sourceId` legacy-stable and does not invent runtime item instance ids |
| stable key strategy | equipment passive cue keys keep legacy literal formats; talent trigger/damage cues use `passive:talent:<talentId>:<effectKind>:rank<rank>` |
| trace cleanup | old talent rank cue keys are absent from the next frontstage trace frame after learn, rank-up, respec or save-load refresh |
| passive trigger logs | source-aware runtime logs emit `log.passive.on_kill_resource_restore`, `log.passive.conditional_bonus_activated` and `log.passive.conditional_bonus_deactivated` with `sourceKind`, `sourceTemplateId` and `talentRank` |
| active slot behavior | PASSIVE never opens `ACTIVE_TALENT_SLOT_CHOICE` |
| passive actions | PASSIVE never shows footer `RESERVE`, `R` shortcut affordance or active slot management; pressing `R` on a PASSIVE row emits no `PlayerCommand.RespecTalentTree`, no reserve/slot command, and leaves `TalentAllocationDraft.pendingRanks`, `TalentAllocationDraft.previousPendingRanks`, `TalentLoadout.slotToTalentId`, and learned ranks unchanged |
| locked PASSIVE action subreasons | `LOCKED` PASSIVE with `PREREQ`, `LEVEL`, `POINTS`, or `FROZEN` subreason behaves like locked and never emits respec, reserve or slot commands |
| rank semantics | current rank replaces previous passive values |
| save/load | passive effects are derived from talent ranks after load |
| UI detail | current and next passive effects are visible from typed passive detail snapshot |
| effective detail display | `castSpeedRating` and `hpRegen` player-facing passive detail values are projected through `DiminishingReturns.effectiveCastSpeed` / `DiminishingReturns.effectiveHpRegen`; focused tests cover official talents and a synthetic hpRegen value that differs after DR |
| UI detail owner | `core.snapshot` owns passive detail snapshots; `game` projects them; `client` maps `PassiveDetailLineToneSnapshot` to `TalentPreviewToneToken` |
| passive line templates | every supported passive kind has declared current/delta template args and deterministic ordering |
| conditional line structure | `ConditionalStatBonus` renders one condition-prefixed line per changed stat and emits no separate condition header row |
| passive detail density | current detail and next preview each render `<= 10` passive effect lines and overflow uses a working right-detail scrollbar |
| rank 0 preview semantics | rank 0 current detail shows rank 1 effects; rank 0 next preview shows rank 1 -> rank 2 deltas |
| deterministic display order | stat rows follow §2.5 stat table order; damage type rows follow `PHYSICAL -> FIRE -> COLD -> LIGHTNING -> HOLY -> SHADOW` |
| negative stat modifier policy | PR04-01 official passive data contains no negative stat modifiers; future negative modifiers require §6 render rule update |
| passive projection coverage | loader owns unknown serialized kind fail-fast; projection tests cover every known `PassiveEffect` subtype and fail on missing known-kind template mapping |
| locale token policy | passive detail labels reuse `ui.stat.*` for primary attributes, `ui.hud.*.short` for HUD-visible derived stats and `ui.inspect.mod.*` for modifier-only labels; no parallel `ui.stat.*` modifier namespace exists |
| callbacks rule | PASSIVE talents serialize `callbacks` as absent or `callbacks: []`; non-empty callbacks are forbidden |
| content pack | official talent YAML supports `passiveEffects`; `registry: talent` content-pack overlays remain unsupported and unknown passive kind fails fast |
| frozen coverage | frozen professions listed as excluded, not missing |
| whitebox scenario setup | all four PR04-01 scenario ids use exact `playerLevel`, exact `setUnspentTalentPoints`, `targetRank=0`, draft/modal/loadout reset, and fail-fast post-setup assertions before materialization |
| whitebox focus boundary | all four PR04-01 scenario ids project `initialFocusedTalentId` through `RenderUiStateSnapshot.validationTalentFocusRequest`; `InputHandler.enterTalentAssign()` consumes the focus request once by `consumeOnceToken` |
| whitebox scenarios | all four PR04-01 scenario ids are registered, present in YAML, materialized into `cua-runbook.md` outputs, and use registry-owned focus-by-id setup with `expectedFocusedTalentId`, structured typed assertions, structured localized visible assertions, log evidence, and forbidden log fragments |
| screen coverage matrix | `UI/pr/screen-coverage-matrix.md` and `UI/pr/README.md` include PR04-01 evidence entries |
| starter active slot count | every PR04-01 profession has `starter active count after conversion = 3` and `activeSlotCount = 4` |
| conversion audit parity | conversion audit remains true after implementation; tactical verbs listed in §3.3 are still retained by active talents |
| talent data revision | no `talentDataRevision` or save migration is introduced in PR04-01; numeric rebalance after PR04-01 requires a separate explicit policy PR |
| golden / whitebox | `clientSmoke` and `goldenScreenshot` result recorded; packaged whitebox executed/skipped manual record uses §7 fixed fields |
| tests run | exact commands and results |
| tests not run | exact reason |

## 11. Open Risks

1. The 12 converted talents are fixed by §3.1 and §3.2. If implementation review rejects one conversion, the PR must revise this document and rerun `acceptanceContractLint` before substituting a different talent.
2. Resolver migration is blocked until `04-01a` proves behavior parity for all 9 existing equipment passive kinds.
3. Slice rollback must preserve portfolio shape: if Task B is reverted, Task A cannot be advertised as the final playable passive pass because every included profession requires one evergreen/static passive and one conditional/trigger/resource passive.
4. PR04 visual detail evidence is mandatory through `clientSmoke` and `goldenScreenshot`; packaged app whitebox executed/skipped record must use §7 fixed fields.
5. Dev playable coverage is PR04-01 blocking only. It does not promote `berserker / spellblade` into release-facing blocking metrics.
6. Passive value floor is blocking. If focused tests or play review show the final table still cannot compete with active skill choices, the implementation must strengthen or add passive effects in this PR rather than deferring the gameplay fix.
7. PR04-01 derives passive values from current content data on load and does not add `talentDataRevision`. Future passive numeric rebalance must be a separate policy-bearing PR, not a silent edit to this contract.
8. `bulwark_march` uses `GUARD_STANCE_BUFF` rather than `GUARD` so rank 1 is reachable from current `guard_stance` data. If `guard_stance` duration/cooldown or status ids change, §3.0 uptime and §3.2 trigger map must be recalculated in the same PR.
9. `deathblow` MARKED damage depends on `shadowstep` rank >= 2 `shadowstep_marked` or `shadow_bind` rank >= 4 `shadow_bind_marked`. If those MARKED sources are removed, the same PR must either name a replacement trigger source in §3.2 with an implementation proof test or remove the marked-damage component from `deathblow`.
10. PR04-01 does not redraw skill icons. The 12 converted PASSIVE talents keep their existing active-style icons until PR06 skill icon rebaseline; the type line and passive detail text are the mitigation for PR04-01.
11. A non-production `EquippedPassiveSource` fixture helper may exist only with the deprecation owner from §4 Task 2. Leaving it unowned after PR04-01 is a contract failure.

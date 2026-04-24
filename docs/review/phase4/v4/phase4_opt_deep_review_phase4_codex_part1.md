# phase4 深度审查报告（Part 1/4）

## 1. 执行摘要

1. 当前 Phase4 不是“功能完成但体验未闭环”。更准确的判断是：**基本可玩，工程验证闭环强，已经具备耐玩雏形，但仍不具备无条件进入 Phase5 的干净完成态**。
2. 与 `docs/review/phase4/v3` 中的旧问题相比，当前仓库已经修掉多项硬缺口：关键路径节奏已通过、动态掉落池覆盖已达 100%、有机隐藏内容不再是仅 scripted pass、Boss variant action trace divergence 已通过、版本与 content pack range 已对齐到 `0.4.0`。
3. 当前最严重的问题已经从“系统没做”转为“系统做了但关键体验指标仍靠 approved debt 放行”：`professionCapstoneAdoptionFloor.reportOnly` 和 `nonWeaponBuildPayoffFloor.reportOnly` 均为 `2/4 APPROVED_DEBT`，而这正是 Phase4 的构筑与奖励驱动核心。
4. 另一个更上游的构筑问题是：职业树和铭文虽然有 schema、数据、商店和 UI 入口，但当前体验仍接近“开局给一组完整基础动作和 4 个默认铭文，后续升级主要升 rank 数值”。这削弱了 run 内 build 选择，不应被推给 Phase5；具体改造方案已补到 `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md`。
5. Phase4 的主循环已经能跑通：探索、遭遇、奖励、成长、隐藏发现、Boss telegraph、content pack overlay 与 UI/反馈面都存在；但“再来一局”的强驱动力仍主要卡在奖励采用率、职业树选择、capstone 实际采用、非武器收益、铭文替换和隐藏探索主动性上。
6. UI/UX 五个 PR 的代码、自动化与完成态已经不再作为本轮改造项：`ModalStack`、`PaneFocusController`、`QualityPresentation`、`StatusPresentationModel`、`TelegraphPresentationModel`、`CombatDecisionFrame` 等核心表面已落地，本报告不再把 UI/UX 完成态列为 Phase4 P0 风险。
7. 隐藏内容从 v3 的“验证假绿”已经明显改善：`leadDiscoveryRate=46.6%`、`secretConversionRate=43.9%`、`secretZoneEntryRate=20.5%`、failing zone 为 none。但 zone 级 Search 行为不均：`abyssal_temple searchUseRate=0.0%`、`deep_iron_pit=1.5%`，说明部分隐藏内容仍更像被事件/路径推到玩家面前，而不是玩家主动学会搜索。
8. Boss variant 现在不再是旧报告里的“trace 几乎一样”：`variantTraceDivergenceRatio=100%`、`minVariantActionTraceDivergenceScore=1.000`。但 `phaseGraphUnchanged=true`、`structuralDiffCount=0` 仍说明它是 Phase4 可接受的 variant 层，不是长期 Boss replayability 的终局方案。
9. 当前 Phase4 可以作为下一阶段的基础，但进入 Phase5 主线前必须先处理 P0：run 内职业树/铭文构筑轴、构筑/非武器 payoff approved debt。否则 Phase5 的内容扩张会建在“构筑看似有、实际采用不稳定”的地基上。

## 2. 审阅范围与依据

### 2.1 参考文档

本次审阅按 Phase4 完成态取证，重点阅读和交叉引用以下文档：

| 范围 | 证据锚点 | 本次使用方式 |
| --- | --- | --- |
| Phase4 总路线 | `docs/phase4/roadmap.md` | 确认 P4-X/A/B/C、P4-W1~W5、`phase4Report` canonical gate 与 producer inventory 要求 |
| Phase4 主设计 | `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` | 提取 procgen、loot、hidden、content pack、Boss、terrain、anti-cheese 与“不引入 Lua/Mod SDK”等阶段边界 |
| Phase4 验证清单 | `docs/phase4/2026-03-13-phase4-verification-checklist.md` | 对照 owner metrics、white-box 与 `reportPhase4` 约束 |
| Phase4 v3 review | `docs/review/phase4/v3/*.md`、`docs/review/phase4/v3/PR/*.md` | 作为历史问题清单，并用当前实现/报告逐项确认哪些已经修复、哪些仍残留 |
| UI/UX PR 总入口 | `docs/opt/ui-pr/README.md` | 确认 PR01~PR05 范围、`clientSmoke/goldenScreenshot/verifyChanged` 与完成态边界 |
| UI/UX PR01~PR05 | `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr0*.md` | 对照 main menu、modal/input/look、item presentation、status/explain、telegraph/combat decision 完成态 |
| 职业树/铭文改造补充 | `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md` | 固定职业树选择、起始技能收缩、铭文安装/替换、验收指标与 Phase4/Phase5 边界 |

### 2.2 参考实现与产物

| 范围 | 证据锚点 | 结论用途 |
| --- | --- | --- |
| Phase4 聚合报告 | `tools/build/reports/verification/phase4/report-phase4-summary.md` | 当前 verdict 为 `PASS_WITH_DEBT`，failed task 为 0，approved debt 为 2 |
| Long-run lab | `build/reports/harness/long-run-summary.md` | 判断构筑采用、奖励采用、affix synergy、终局武器分布、死亡分布 |
| Organic hidden probe | `tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md` | 判断有机隐藏发现、Search 行为、secret entry 与 zone 分布 |
| Boss harness | `tools/build/reports/phase4/whitebox/boss/boss-harness.md` | 判断 Boss phase/variant/terrain preference 是否成立 |
| UI 代码 | `client/src/main/kotlin/com/ktome/client/**` | 核实 PR01~PR05 的正式实现是否存在 |
| UI 测试/工具 lint | `client/src/test/kotlin/com/ktome/client/**`、`tools/src/test/kotlin/com/ktome/tools/**` | 核实 combat decision、telegraph、keyword、AI intent leak 等自动化约束 |
| Phase4 数据 | `game/src/main/resources/data/objectives/index.yaml`、`game/src/main/resources/data/loot/index.yaml`、`examples/content-packs/sample.flooded_relics/manifest.yaml` | 核实目标锚点、动态 loot pool、content pack 版本边界 |
| 版本与 BuildInfo | `gradle.properties`、`client/build.gradle.kts`、`client/src/main/kotlin/com/ktome/client/build/BuildInfo.kt` | 核实 v3 版本漂移问题是否已关闭 |

### 2.3 审阅方法

1. 先按 `docs/phase4/` 建立 Phase4 承诺项：procgen、loot、terrain、hidden、content pack、Boss、verification、UI/UX 前台化。
2. 再按 `docs/review/phase4/v3` 建立旧问题清单，只保留当前仓库仍有证据支撑的问题，不复用已经过期的批评。
3. 对 `docs/opt/ui-pr` 逐 PR 对照实现与测试，确认 UI/UX 已不再作为本轮 P0 风险。
4. 最后从玩家体验链路重构：探索 -> 遭遇 -> 决策 -> 奖励 -> 构筑变化 -> 隐藏发现 -> 下一局动机。

本轮审阅没有重新执行 Gradle 或 packaged app，只读取当前仓库中的源码、文档和已有报告产物。报告中的验证结论均来自已有 artifact，不把理论推断当作已运行结果。

## 3. Phase4 设计实现一致性矩阵

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| Phase4 canonical report | Phase4 完成态必须由聚合 gate 汇总，不用零散任务口头判定 | 已实现但带债 | `docs/phase4/roadmap.md`；`tools/build/reports/verification/phase4/report-phase4-summary.md` | `phaseVerdict=PASS_WITH_DEBT`、`failedTaskCount=0`、`approvedDebtCount=2`。不能称为 clean pass。 | High |
| Procgen / zone topology | P4-W1 要让地图拓扑、关键路径和 solvability 可度量，避免路线塌缩 | 已实现 | `tools/build/reports/verification/phase4/report-phase4-summary.md`；`game/src/main/resources/data/objectives/index.yaml` | 关键路径指标已通过：`criticalPathCombatFloorSatisfied=100.0%`，各 mandatory zone `avgObjectiveAcquireTurn >= 7.167`。旧 v3 “开局/楼梯附近塌缩”不再成立。 | Low |
| Critical path design hooks | mandatory zone 应不只是路线存在，还要有可感知机制支撑阶段体验 | 部分实现 | `tools/build/reports/verification/phase4/report-phase4-summary.md` Critical Path Design Audit | 报告仍列出 `trail_pressure`、`beacon_warning`、`ore_cart`、`sealed_gate`、`ferry_crossing`、`void_pressure` 等 `mechanicsWithoutDedicatedRuntimeHook`。如果这些是玩法承诺，则仍是伪完成；如果只是 flavor，应在文档/报告中降级说明。 | Medium |
| Solvability / reveal / search contract | 地图必须可解，隐藏 reveal 需要可证明成功/失败 taxonomy | 已实现 | `tools/build/reports/verification/phase4/report-phase4-summary.md` Solvability WhiteBox | `revealSuccessCaseCount=40` 且全部有 reveal/backtrack proof，失败 taxonomy 也有 6 个 `FAILED_CHECK`。 | Low |
| Terrain interaction | Phase4 至少冻结 WATER/OIL/ICE 等 terrain tag，并让 encounter rate 可验 | 已实现 | `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`；`tools/build/reports/verification/phase4/report-phase4-summary.md` | `terrainInteractionEncounterRate.aggregate=16.6%`，相对 baseline +31.95%，per-zone lower bound 全部通过。 | Low |
| Dynamic loot pool | 掉落不能靠固定清单假装多样性，动态池需覆盖并有 meaningful swap | 已实现 | `game/src/main/resources/data/loot/index.yaml`；`tools/build/reports/verification/phase4/report-phase4-summary.md` | `dynamicPoolCoverage=100.0% (10/10)`，多处 cadence/reward 已使用 `TAG_WEIGHTED`。旧 v3 “dynamic pool 不成立”已关闭。 | Low |
| Secret reward local identity | Secret reward 应有本地身份，不能与普通 cadence/reward 过度重叠 | 已实现 | `tools/build/reports/verification/phase4/report-phase4-summary.md` Local Reward Identity | `sameZoneSecretVsCadenceMaxOverlap=0.400`、`sameZoneSecretVsRewardMaxOverlap=0.400`、authority violation 为 0。 | Low |
| Build identity / capstone adoption | Phase4 的奖励与构筑必须让职业差异和 capstone payoff 真实发生 | 部分实现 | `tools/build/reports/verification/phase4/report-phase4-summary.md`；`build/reports/harness/long-run-summary.md`；`game/src/main/resources/data/build-identity/index.yaml` | 总体 `professionCapstoneAdoptionRate=25.0%` 只是踩线；per-prof floor 为 `2/4 APPROVED_DEBT`，arcanist 和 rogue adoption 均 `0/1`；`terminalWeaponBaseDiversity=3`、`crossProfessionTopWeaponDominance=50.0% top=long_sword` 均压线，templar 与 vanguard 终盘都收敛到 `long_sword`。这是当前最关键体验债。 | Critical |
| 职业树 run 内构筑 | Phase3 已要求正式职业树、`TalentAllocationDraft`、断点成长、升级预览和前置关系；Phase4 应建立在有构筑深度的长局之上 | 偏离实现 | `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`；`docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md`；`game/src/main/kotlin/com/ktome/game/TalentProgression.kt`；`game/src/main/kotlin/com/ktome/game/factory/EntityFactory.kt` | 当前按 `unlockLevel` 自动把树节点加入 loadout，新解锁 talent 默认 `rank=1`；补充方案已固定为“3 个 starter + learnable 不自动 learned + rank 0 -> 1 学习消耗点数 + Tier 门槛”。 | Critical |
| 铭文/符文构筑 | 铭文是 ToME 类核心自定义轴；Phase3 设计为最多 4 槽、掉落/任务/Boss 来源，后续经济可扩展 | 偏离实现 | `docs/2026-03-13-core-systems-design-and-phase-supplements.md`；`docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md`；`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`；`core/src/main/kotlin/com/ktome/core/inscription/InscriptionManager.kt`；`game/src/main/resources/data/shops/index.yaml` | `ensurePlayerInscriptions` 开局直接装备四槽；`InscriptionManager.canEquip` 满 4 槽即拒绝。补充方案已固定为“开局 2 铭文 + 空槽安装 + 满槽替换”。 | Critical |
| Non-weapon payoff | 非武器奖励应能支撑 build shift，而不是全部回到主武器 DPS | 部分实现 | `tools/build/reports/verification/phase4/report-phase4-summary.md`；`build/reports/harness/long-run-summary.md` | `nonWeaponBuildPayoffRate=25.0%` 只是踩线；per-prof floor 为 `2/4 APPROVED_DEBT`，arcanist 和 rogue non-weapon 均 `0/1`。arcanist 终局 ARMOR 采用 `unique_furnace_plate`，这是 vanguard capstone，说明 armor identity 被跨职业 reward 吞掉。 | High |
| Terminal weapon diversity | 终局不能被单一武器压扁，不同职业应有 terminal identity | 部分实现 | `tools/build/reports/verification/phase4/report-phase4-summary.md` | `terminalWeaponBaseDiversity=3` 通过，但 `crossProfessionTopWeaponDominance=50.0% top=long_sword` 正好卡上限；templar 与 vanguard 都落在 long_sword 语义。 | Medium |
| Milestone reward adoption | 战斗/探索奖励应经常改变下一步选择，而不是大量无效掉落 | 部分实现 | `build/reports/harness/long-run-summary.md` | `milestoneRewardAdoptionDistribution={adopted=46, notAdopted=57}`，未采用多于采用；slot 分布 `ARMOR=8, OFF_HAND=49, WEAPON=46`，护甲奖励存在感偏弱。 | Medium |
| Affix synergy | affix 应强化构筑方向并制造“这局不同”的记忆点 | 部分实现 | `build/reports/harness/long-run-summary.md` | `affixSynergyActivationDistribution={of_shadow=5}`，synergy activation 过度集中；reward 分布也由 `of_piercing=11`、`of_shadow=6` 主导。 | Medium |
| Organic hidden loop | 不依赖 scripted primer，玩家有机探索也应能发现并进入 secret zone | 已实现但体验不均 | `tools/build/reports/phase4/hidden/organic-hidden-probe-summary.md`；`tools/build/reports/verification/phase4/report-phase4-summary.md` | 总体 `leadDiscoveryRate=46.6%`、`secretConversionRate=43.9%`、failing zone none；但 `abyssal_temple searchUseRate=0.0%`、`deep_iron_pit=1.5%`，且 `zoneDiscoveryDistribution` 中 `abyssal_temple=52.0%`，hidden lead 过度集中。 | Medium |
| Frontstage action cues | 隐藏、terrain、重要行动提示要进入正式前台信息面并有 owner metrics | 已实现 | `client/src/main/kotlin/com/ktome/client/render/FrontstagePresentationText.kt`；`tools/build/reports/verification/phase4/report-phase4-summary.md` | `frontstageHighPriorityCueRetainedRate=100%`、`frontstageSecretCueVisibilityRate=100%`、TTL parity 通过。 | Low |
| Boss phase identity | Boss 至少有可读 phase、telegraph、variant divergence 和 reward/terrain ledger | 已实现但结构浅 | `game/src/main/resources/data/boss-variants/index.yaml`；`tools/build/reports/phase4/whitebox/boss/boss-harness.md` | `variantTraceDivergenceRatio=100%`，但每对均 `phaseGraphUnchanged=true`、`structuralDiffCount=0`；variant 数据只有 `actionWeights`，没有 `phaseOverrides`。当前不是伪未实现，但 variant 记忆点偏薄。 | High |
| Content pack overlay | Phase4 只做到 content pack / overlay 级扩展，不引入 Lua/runtime script host；sample pack 应以 `ADD` 为主验证玩家扩展路径，非 ADD 语义由 fixture 覆盖 | 已实现但 sample 示范偏离 | `docs/phase4/roadmap.md`；`docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`；`examples/content-packs/sample.flooded_relics/manifest.yaml` | sample pack `gameVersionRange: ">=0.4.0 <0.5.0"` 已对齐；但 manifest 中仍有 2 个 `REPLACE` 主路径条目。功能可用，示范姿势需要改成 ADD-first，避免下游 pack 作者复制 REPLACE-heavy 模式。 | Low |
| UI PR01 foundation/menu | 建立 UI token、InfoSurfaceLayout、main menu、build hash 与 continue states | 已实现 | `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt`；`client/src/main/kotlin/com/ktome/client/render/layout/InfoSurfaceLayout.kt`；`client/src/main/kotlin/com/ktome/client/build/BuildInfo.kt` | 代码与 build-info 注入存在，未发现与 PR01 目标冲突。 | Low |
| UI PR02 input/modal/look | 输入模式、modal stack、Look Mode、保存阻断和 force-switch feedback 需要正式化 | 已实现 | `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`；`client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt`；`client/src/main/kotlin/com/ktome/client/ui/layout/PaneFocusController.kt` | Modal/input/look 已进入正式路径，相关 i18n key 与测试存在。 | Low |
| UI PR03 item presentation/states | special tier、item quality、ground loot marker、modal card、empty/error/loading 状态要正式化 | 已实现 | `client/src/main/kotlin/com/ktome/client/ui/item/QualityPresentation.kt`；`client/src/main/kotlin/com/ktome/client/ui/item/GroundLootMarkerModel.kt`；`client/src/main/kotlin/com/ktome/client/ui/state/*.kt` | `specialTierId` 与 `specialTemplateId` 同步校验存在；旧 `ui.inspect.empty.tile` 被测试禁止回归。 | Low |
| UI PR04 status/explain/readability | Status/telegraph/description/explain/accessibility 应形成正式 presentation surface | 已实现 | `client/src/main/kotlin/com/ktome/client/ui/status/StatusPresentationModel.kt`；`client/src/main/kotlin/com/ktome/client/telegraph/TelegraphPresentationModel.kt` | Status、telegraph、description、explain、accessibility presentation 已进入正式路径；不再列为本轮改造项。 | Low |
| UI PR05 telegraph/combat decision | Telegraph 三位一体、combat decision ACTION/METHOD/TARGET、非法目标与缺事实反馈正式化 | 已实现 | `client/src/main/kotlin/com/ktome/client/ui/combat/CombatDecisionFrame.kt`；`tools/src/test/kotlin/com/ktome/tools/lint/AiIntentLeakRuleTest.kt` | Combat decision 与 anti intent leak 边界已落地；不再列为本轮改造项。 | Low |
| Anti-AI intent leak | PR05 明确不展示普通敌人 intent，不从 `aiTypeId` 推断下一步行动 | 已实现 | `tools/src/test/kotlin/com/ktome/tools/lint/AiIntentLeakRuleTest.kt`；`client/src/main/kotlin/com/ktome/client/ui/combat/CombatDecisionFrame.kt` | lint 与 combat decision surface 均未引入 `AIPlanSnapshot`。这是正确边界，不是缺功能。 | Low |

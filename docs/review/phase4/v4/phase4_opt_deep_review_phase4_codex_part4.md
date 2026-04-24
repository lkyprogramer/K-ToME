# phase4 深度审查报告（Part 4/4）

## 6. 优化建议（按优先级）

### 6.1 P0：必须立刻处理

#### P0-1：补 run 内职业树选择与铭文替换闭环

| 项 | 内容 |
| --- | --- |
| 问题本质 | 当前成长轴更像“自动获得技能 + rank 数值提升”，铭文则开局直接四槽满配，缺少 ToME 类应有的职业树和铭文构筑选择。 |
| 影响范围 | `TalentProgression`、`TalentLoadout` 初始化、升级/分配 UI、`TalentAllocationDraft` 接线、`InscriptionManager`、shop purchase flow、run summary/build hash。 |
| 优化目标 | 玩家在前 30 分钟内必须做出 2 类非装备构筑选择：职业树/技能断点选择、铭文安装/替换选择。 |
| 具体改法 | 详细方案固定在 `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md`。执行口径为：1. 四个基础职业不新增技能数量，每职业开局只保留 3 个 starter。2. `unlockLevel` 只让节点进入 learnable，不再自动写入 `TalentLoadout.talentLevels`。3. `TalentAllocationDraft` 支持 rank 0 -> 1 学习和 rank N -> N+1 升级，二者均消耗 1 点职业天赋点。4. Tier 2/Tier 3 必须同时满足等级、前置 rank、树内投入。5. 开局铭文固定为 2 个，最大 4 槽保持不变。6. 商店 inscription offer 在空槽时安装，在满槽时进入替换界面，不能直接拒绝。7. `RunSummary`、long-run、`phase4Report` 必须记录 talent choice event、breakpoint event、inscription install/replace event。 |
| 预期收益 | 升级和商店从数值/补给入口变成构筑入口，直接增强“这局我走另一套 build”的动力。 |
| 风险 | 起始 answer 砍太多会破坏 Solo-Clear；必须保留每职业输出、自保、位移/逃生、Boss answer 的最低安全线。 |
| 同步项 | 需要同步 Phase3/Phase4 review 文档、职业/天赋数据、铭文数据、商店 UI/i18n、longRunLab 和 `phase4Report` 构筑指标。 |

#### P0-2：把 capstone / non-weapon payoff 从 approved debt 变成真实 gate 或真实修复

| 项 | 内容 |
| --- | --- |
| 问题本质 | Phase4 最核心的构筑 payoff 仍靠 `reportOnly` approved debt 放行。 |
| 影响范围 | `game/src/main/resources/data/build-identity/index.yaml`、`game/src/main/resources/data/loot/index.yaml`、`game/src/main/resources/data/items/index.yaml`、职业 capstone source/weight、adopt 评分、longRunLab build-identity schema、`Phase4AggregationInputRunner` / `Phase4ReportRunner` gate 映射。 |
| 优化目标 | arcanist、rogue、templar、vanguard 每个职业至少 1 个 capstone adoption 和 1 个 non-weapon payoff 样本通过；`terminalWeaponBaseDiversity >= 4`；`crossProfessionTopWeaponDominance <= 40%`；不再以 `APPROVED_DEBT` 作为 Phase4 完成态。 |
| 具体改法 | 1. 把 `professionCapstoneAdoptionFloor.reportOnly` 和 `nonWeaponBuildPayoffFloor.reportOnly` 升级为 blocking owner metric，要求 4/4 职业 `adoption>=1` 且 `nonWeapon>=1`。2. 将 `terminalWeaponBaseDiversity` 从 `>=3` 收紧到 `>=4`，将 `crossProfessionTopWeaponDominance` 从 `<=50%` 收紧到 `<=40%`。3. 修 templar/vanguard `long_sword` 撞车：templar 的终局身份必须优先落在 `artifact_eclipsed_relic / unique_vesper_chainmail / unique_voidlit_seal` 这组 holy/protection 非武器 capstone 上，vanguard 继续保留 forge/furnace/maul 线；loot source 和 adopt 评分不得让两者终盘都以 `long_sword` 作为主身份。4. 修 arcanist armor identity：新增或提升 arcanist-aligned ARMOR/robe 候选，并降低 `unique_furnace_plate` 对非 vanguard 的采用优先级；`unique_deepcurrent_lens / artifact_river_echo` 在 arcanist 上必须能压过普通 `emerald_charm`。5. 修 rogue OFF_HAND capstone：`artifact_briar_heart` 对 rogue 必须获得 artifact + profession-match adopt 加成，能压过默认 `basic_shield`。6. 更新 long-run fixture，让每个职业的 capstone/non-weapon payoff 都有可复现样本。 |
| 预期收益 | 构筑从“看得到”升级为“会使用”，直接增强重开动机。 |
| 风险 | 调整过度会造成固定路线最优解，尤其是 arcanist/rogue 可能被某个 off-hand/capstone 锁死。需要同时看 terminal diversity 和 top weapon dominance。 |
| 同步项 | 需要同步 Phase4 verification checklist、owner report schema/说明、loot data、可能的 i18n/item description。 |

### 6.2 P1：建议本阶段尽快处理

#### P1-1：提升奖励采用率，而不是继续增加奖励数量

| 项 | 内容 |
| --- | --- |
| 问题本质 | 当前掉落能产出，但采用不足：milestone reward `notAdopted=57` 高于 `adopted=46`。 |
| 影响范围 | loot profiles、special templates、affix weights、reliquary purchase pool、reward report metrics。 |
| 优化目标 | milestone reward adoption 至少反转为 adopted > notAdopted；ARMOR 占 milestone slot 的比例提升到 `>=20%`；WEAPON/OFF_HAND/ARMOR 三类任一 slot 不得超过 `50%`；头部 5 个 affix 曝光占比降到 `<=40%`。 |
| 具体改法 | 1. 对每个职业增加一个“本职业当前常见 build 会真正想换”的 non-weapon/capstone reward。2. 对 route/cache/support/boss source 增加轻量 anti-duplicate 和 slot-rotation，连续两个 boss/support milestone 不得给同一 slot。3. 提升 armor/off-hand 的独特效果权重，尤其是能改变资源、防御节奏或位移策略的效果。4. 将 `milestoneRewardSlotBalance` 纳入 report：ARMOR `>=20%`，WEAPON `>=25%`，OFF_HAND `>=25%`，三 slot 均 `<=50%`。5. 对 `sentinel / of_strength / vampiric / of_life` 头部通用 affix 下调曝光，对 `of_smite` 以及职业专属尾部 affix 提升曝光；`of_shadow`、`of_piercing` 之外的 synergy affix 必须有最低观察面。 |
| 预期收益 | 每次奖励更可能改变下一步选择，探索和战斗回报感增强。 |
| 风险 | build-aware bias 不能变成第二套规则真源；应复用已冻结的 tag/schema，而不是在 tools 或 client 手写职业适配逻辑。 |
| 同步项 | loot data、whiteBoxLoot、longRunLab owner metrics、item description/i18n。 |

#### P1-2：修正隐藏探索的主动 Search 学习曲线

| 项 | 内容 |
| --- | --- |
| 问题本质 | 总体 hidden 指标过线，但 `abyssal_temple`、`deep_iron_pit` 的 Search 使用率过低，玩家主动探索习惯不稳定。 |
| 影响范围 | hidden event / secret zone data、frontstage cue、Search prompt logic、organicHiddenProbe。 |
| 优化目标 | 每个 secret-bearing zone 都有最低 Search 行为触发或明确替代交互；`zoneLeadDiscoveryMaxShare <= 40%`；每个 zone 的 `secretEntry / lead >= 25%` 进入 report-only floor。 |
| 具体改法 | 1. 为 `abyssal_temple` 增加必须由玩家可见异常触发的 Search/Interact 分支，避免 97% lead 但 0% Search；objective 完成前必须保留 1 个 secret entry detour node。2. 为 `deep_iron_pit` 的 slag/ore cue 增加更明确的 searchPromptAvailable 条件。3. 提升 `underground_river` 的 lead density，让每个 zone 的 lead share 保持在 15%~40%。4. 把 report 中 `searchUseRate` 从纯 supporting evidence 提升为 per-zone warning threshold，并新增 `zoneLeadDiscoveryMaxShare` 与 `perZoneSecretConversionFloor.reportOnly`。5. 在 frontstage cue 上区分“发现线索”和“可以主动搜索”的文案/图标语义。 |
| 预期收益 | 玩家形成可迁移的探索习惯，hidden content 从随机惊喜变成可学习的高价值玩法。 |
| 风险 | Search 过强会让 secret 变成 checklist；需要保持失败、风险或替代路径。 |
| 同步项 | hidden data、frontstage i18n、organic probe report schema、manual white-box hidden notes。 |

#### P1-3：把 zone 机制名词绑定到最小 runtime effect，或明确降级为 flavor

| 项 | 内容 |
| --- | --- |
| 问题本质 | `mechanicsWithoutDedicatedRuntimeHook` 中的多个机制名词会让文档体验和实际体验脱节。 |
| 影响范围 | objective data、zone data、frontstage cue、terrain/encounter hooks、Phase4 design docs。 |
| 优化目标 | 每个 mandatory zone 至少有 1 个玩家可感知、可报告的 zone identity hook；无法实现的机制名词不要写成玩法承诺。 |
| 具体改法 | 1. 为每个 zone 选一个最小 hook：例如 `trail_pressure` 影响巡逻/线索刷新，`slag_alert` 影响 OIL/FIRE hazard cue，`ferry_crossing` 触发一次可见 crossing choice，`void_pressure` 增加 frontstage warning 与资源压力。2. 在 report 中把仍未实现的名词标为 `flavorOnly` 或 `plannedButNonBlocking`，避免它们被误读成 runtime contract。3. 不做大系统，只做可度量的微机制和可见反馈。 |
| 预期收益 | zone 不只是名字不同，而是路线、风险或反馈真的不同。 |
| 风险 | 一次性实现所有机制会扩大 diff；应每个 zone 只选一个最能代表身份的 hook。 |
| 同步项 | `docs/phase4/*`、objective/zone data、report schema、frontstage copy。 |

#### P1-4：把 Boss variant 的“轻量可验收”边界写清，并补一个高记忆点 variant 行为

| 项 | 内容 |
| --- | --- |
| 问题本质 | Boss variant 已通过 trace divergence，但 variant 数据只有 action weight 差分，没有 `phaseOverrides`；phase graph 没有结构变化，长期记忆点仍薄。 |
| 影响范围 | Boss data、Boss harness、telegraph/status presentation、reward ledger。 |
| 优化目标 | 不扩成 Phase5 tactical AI，但 3/3 boss variant 都必须贡献至少 1 个 data-level `phaseOverrides`，并在该 override 内声明阶段内独有 trigger/telegraph/action。 |
| 具体改法 | 1. 为 `molten_glass` 在 `phase_enraged` 增加 glass/oil 地形互动与独有 high-priority telegraph。2. 为 `grey_crown` 在 `phase_desperate` 增加 crown/war-call 类阶段触发。3. 为 `abyssal_eclipse` 在 `phase_abyssal` 增加 eclipse/void-field 类阶段触发。4. 新增 `bossVariantPhaseOverrideCoverage` owner metric，要求 `3/3` variants 覆盖；新增 `bossVariantPhaseOverrideActionDistinctCount.reportOnly` 记录每个 variant 的新动作数量。5. 在 Boss harness report 中区分 action trace divergence 与 structural phase divergence，避免指标被误读。 |
| 预期收益 | Boss 重复战有更明确记忆点，同时不越过 Phase4 边界。 |
| 风险 | 如果引入新 AI rule 或 phase graph mutation，可能越界到 Phase5。 |
| 同步项 | Boss data、telegraph i18n、boss harness summary、Phase4 review docs。 |

#### P1-5：补 long-run route diversity 证据链，避免调参过拟合单主线

| 项 | 内容 |
| --- | --- |
| 问题本质 | `LONG_RUN_SMOKE` 的 full_route 主路线同构较强，`zoneRouteHashDistribution` 中同一 hash 占 6 条；当前 evidence 能暴露问题，但不能充分证明多路线下的重复游玩差异。 |
| 影响范围 | longRunLab corpus、route hash summary、phase4Report owner metrics、后续 balance 调参。 |
| 优化目标 | `full_route` 样本扩到 12+；`branch_inclusive` 样本至少 3 条；`zoneRouteHashDiversity.topHashShare <= 40%`；每条 branch 样本覆盖不同 mandatory/secret 路线组合。 |
| 具体改法 | 1. 扩容 `LONG_RUN_SMOKE` seed 家族，增加 3 条 branch-inclusive 样本：greenwood -> underground_river、deep_iron -> grey_gate -> crystal_cavern、abyssal_temple。2. 新增 `zoneRouteHashDiversity` blocking 或 warning metric，统计 top route hash share。3. 新增 `branchInclusiveCount`，阈值 `>=3`。4. 新增 `topologyCategoryDiversityPerSmokeRun.reportOnly`，用于观察每轮 smoke 的拓扑类型覆盖。 |
| 预期收益 | Phase4 的构筑/奖励修复不会只对当前主路径过拟合，Phase5 的 replay/death analysis 也能继承更有代表性的基线。 |
| 风险 | 样本扩容会增加本地验证时间；应把宽样本放到 owner/nightly，`verifyChanged` 只跑受影响的最小 route diversity 子集。 |
| 同步项 | longRunLab fixture、phase4Report schema、verification checklist、nightly owner gate。 |

### 6.3 P2：可以排期但不应忽略

#### P2-1：把 sample content pack 改成 ADD-first 官方示范

当前 sample pack 的版本边界和 overlay contract 是健康的，但 `examples/content-packs/sample.flooded_relics/manifest.yaml` 仍有 2 个 `REPLACE` 主路径条目。Phase4 允许 `ADD + whole-entry REPLACE`，但官方 sample 的示范职责必须 ADD-first：hidden event / secret zone 的扩展优先改成 namespaced `ADD`，确实需要替换官方 entry 时必须在 YAML 注释中写明替换原因。保留 REPLACE/precedence/conflict 的验证职责应由 fixture pack 承担，而不是由玩家可见 sample pack 承担。

#### P2-2：补 sample pack 玩家可见度指标

content pack loader、lint、version range 和 sample manifest 已经成立，但 sample pack 对玩家是否“实际可见”仍偏工程证据。新增 `samplePackContentPlayerVisibilityRate.reportOnly`：开启 `sample.flooded_relics` 后，至少一次 run/harness 能看到 pack 增加的 loot、secret-zone 改动或 item identity；Validation overlay/main menu 同步展示 active pack id、pack 改动摘要、被触达的 pack content id。该指标不阻塞普通 base game，但用于防止 sample pack 只在 manifest 里成立。

## 7. 可延后到后续阶段的问题

| 问题 | 为什么可以延后 | 当前是否需要轻量兜底 |
| --- | --- | --- |
| 普通敌人 intent / AIPlanSnapshot | PR05 明确禁止从 `aiTypeId` 推断普通敌人下一步行动；Phase4 只做 Boss telegraph 与 combat affordance。普通敌人 intent 更适合 Phase5 tactical AI。 | 需要保留 `AiIntentLeakRule`，防止 client 侧提前伪造 intent。 |
| 完整 Boss phase graph mutation / tactical AI boss 重写 | 当前 Phase4 只要求 data-level variant 阶段语言，不进入完整 tactical AI 或全新 boss runtime。完整 phase graph mutation 会扩大规则和验证面。 | Phase4 必须先补 3/3 variant 的 `phaseOverrides`；完整 AI 决策 trace、普通敌人 intent 与大规模 boss runtime 重写留到 Phase5。 |
| 完整 Mod SDK / Lua runtime / 行为脚本宿主 | Phase4 明确只做 content pack / overlay 级扩展，不引入 runtime script host。 | 保持 content pack lint、version range、namespacing 和 fail-fast，避免 overlay 越权定义核心规则。 |
| 大规模新增职业或数值型 meta progression | Phase4 不新增新 profession，不做数值型 meta progression；当前问题是现有职业树/铭文和构筑 payoff 不稳，不是职业数量不足。 | 先修 run 内职业树选择、铭文替换、capstone/non-weapon payoff；不要用新职业掩盖旧职业构筑问题。 |
| 永久数值型局外成长 | 仓库长期合同不允许数值型 meta progression；Phase3 文档也明确仍是纯 run-based Roguelike。 | 可以做非数值型局外动力：run history、职业/进阶职业解锁、成就式记录、codex/图鉴、最佳 build 复盘；不要给永久属性加成。 |
| 高级 accessibility 设置页 | 当前已有 `AccessibilityToggle` 和 presentation model，Phase4 可以通过配置/启动参数兜底。 | 需要记录为 P2，并避免继续增加不可关闭的信息密度。 |

## 8. 最终结论

### 8.1 当前 Phase4 是否达到了文档预期

**大体达到了功能与工程验证预期，但没有达到 clean completion。**

理由很直接：`reportPhase4` 已经是 `PASS_WITH_DEBT`，不是 fail；但 approved debt 正好落在 Phase4 最核心的构筑/奖励体验面上。同时，职业树与铭文的 run 内选择结构仍未成型，`docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md` 已把这一问题升级为进入 Phase5 前必须关闭的 P0。因此不能把当前状态写成“Phase4 已完全完成且无遗留关键问题”。

### 8.2 当前版本是否已经“好玩”

**已经具备基本可玩性，但还不能称为稳定好玩。**

当前版本能让玩家探索、战斗、看到奖励、进入隐藏内容、理解 Boss telegraph 和 combat decision。它已经不是技术 demo。但好玩的核心在于“这局升级、铭文和掉落让我打法变了、下一局我想试另一种 build”。当前职业树自动获得、铭文开局满配、capstone/non-weapon payoff、milestone adoption 和 affix synergy 证据都说明这个驱动还不够稳。

### 8.3 当前版本是否具备“耐玩雏形”

**具备耐玩雏形。**

Procgen、hidden、dynamic loot、secret reward、Boss variant、content pack overlay、frontstage cue 和 UI explain surface 都是正确地基。尤其是 v3 的多个硬问题已经被修掉，说明项目不是“功能清单堆砌”。但耐玩雏形还需要通过奖励采用率和构筑差异来兑现。

### 8.4 是否适合进入下一阶段开发

**不建议无条件进入 Phase5。**

可以开始 Phase5 的设计准备或非冲突性探索，但主线进入 Phase5 前应先关闭 P0：

1. capstone/non-weapon payoff approved debt 必须转为 blocking 或被真实修复。
2. 职业树与铭文构筑必须按 `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md` 落地：3 个 starter、learnable 不自动 learned、学习新技能消耗点数、Tier 门槛、开局 2 铭文、满槽替换。
3. Boss variant 必须补 3/3 `phaseOverrides`，让 variant 不只靠 action weight 差分。
4. long-run 必须补 route diversity 证据链，避免 Phase5 基线继承单主线路径偏差。

如果这些不处理，Phase5 会在构筑驱动不稳、奖励采用不足的基础上扩系统，返工成本会明显上升。

### 8.5 进入下一阶段前必须先补的内容

1. **构筑采用 gate**：关闭 `professionCapstoneAdoptionFloor.reportOnly` 与 `nonWeaponBuildPayoffFloor.reportOnly` 两个 approved debts。
2. **职业树与铭文构筑**：按补充设计落地。四个基础职业不新增技能数量，每职业开局 3 个 starter；`unlockLevel` 只进入 learnable；学习新技能和升 rank 共用职业天赋点；Tier 2/Tier 3 受等级、前置 rank、树投入约束；开局 2 铭文；满槽购买进入替换流程。
3. **奖励采用调优**：让 milestone adopted > notAdopted，提升 armor/non-weapon 的 build-shifting 价值，避免 affix synergy 单点集中。
4. **隐藏探索主动性**：修 `abyssal_temple` 与 `deep_iron_pit` 的 Search 行为不均，让隐藏发现从“被推到眼前”变成“玩家主动学会搜索”。
5. **Boss variant 阶段语言**：3 个 boss variant 都补 `phaseOverrides`，并用 `bossVariantPhaseOverrideCoverage` 锁住。
6. **long-run route diversity**：补 `zoneRouteHashDiversity.topHashShare <= 40%`、`branchInclusiveCount >= 3`，让后续 balance 不是只对主路线过拟合。
7. **zone mechanism truthfulness**：对 `mechanicsWithoutDedicatedRuntimeHook` 逐项选择最小 runtime hook 或文档降级，不让机制名词成为第二真源。

最终判断：**当前 Phase4 是一个强工程闭环、基本可玩、但仍带关键体验债的完成候选态。它可以作为下一阶段的基础，但只有在 P0 关闭后，才适合被称为真正的 Phase4 完成态。**

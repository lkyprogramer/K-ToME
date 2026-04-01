# Phase 4 PR 级开发文档深度复审意见

**日期**: `2026-04-01`  
**审阅视角**: 资深游戏设计 / 开发总监  
**审阅范围**: `docs/phase4/roadmap.md`、`docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`、`docs/phase4/2026-03-13-phase4-verification-checklist.md`、`docs/phase4/2026-03-13-phase4-pr-01` ~ `pr-09`  
**上游基线**:
1. `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`
2. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
3. `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
4. `AGENTS.md`

## 1. 直接结论

这套 `Phase 4` 文档已经从“方向说明”升级到了“可落地执行稿”，质量明显高于早期版本。优点很明确：

1. 已经把 `Phase 4` 的主目标从“多做系统”修正成“提高 replayability”。
2. `Mapgen / Solvability / Loot / Hidden Content / Content Pack` 五条线已经有了清晰的 PR 级切分。
3. 大部分关键 contract 已开始走 typed schema、harness、白盒验证和 root alias 的正式路径。
4. 旧 review 中最危险的几处空洞，像 `最小非 BSP planner`、`pity`、`ACTIVE_SEARCH`、`phase4Report`、`最小可行 pack`，已经被吸收进主文档。

但它还**没有到“可以放心按文档直接开工”的状态**。当前最主要的问题，不是某个字段少了，而是有几条**跨 PR 的横切合同仍然没有被明确冻结**。如果直接进入实现，最容易在 `PR-03 / PR-05 / PR-07 / PR-08` 四个点出现二次返工。

我当前的总体判断是：

1. `PR-01 ~ PR-09` 的拆分大方向成立，不需要推翻。
2. 在真正开工前，应该先补一个很短但高约束的 `P4-X Cross-Cutting Contracts` 文档，先冻结横切合同，再回写 9 份 PR 文档。
3. 若不先做这一步，后续最可能失控的不是算法实现，而是 `save/replay`、`reward economy`、`ACTIVE_SEARCH`、`overlay semantics` 这四块。

---

## 2. 优先级最高的修订意见

### S-1：必须先补一份 `P4-X Cross-Cutting Contracts`，否则 9 份 PR 文档会各自长出“局部正确、整体撕裂”的实现

这是我最强的建议。

当前 9 份 PR 文档都写得很认真，但它们默认了几个重要前提已经成立，而这些前提实际上还没有被任何一份文档正式冻结：

1. 生成内容的存档 / 回放边界。
2. 奖励与风险预算的统一口径。
3. `ACTIVE_SEARCH` 作为新动作的正式运行时语义。
4. content pack 的 runtime metadata 和 test metadata 的分层。

这些东西横跨 `core / game / client / tools`，如果分别放到 `PR-03 / PR-04 / PR-07 / PR-08` 各自吸收，最后一定会变成多套口径。

建议新增一份非常短的前置文档，只做四件事：

1. 冻结 `Phase 4` run-state persistence contract。
2. 冻结 `reward/threat` 的跨系统预算词汇。
3. 冻结 `SearchAction` 的动作经济和可追溯语义。
4. 冻结 `pack runtime manifest` 与 `pack test fixture metadata` 的边界。

这份文档不需要长，也不应该替代 `PR-01 ~ PR-09`。它的价值在于给实现团队一个“全局不能越线”的总开关。

---

### S-2：`Phase 4` 缺少生成内容的存档 / 回放合同，当前只定义了 harness 可复现，还没有定义 run-state 如何持久化

这个问题贯穿 `PR-01 / PR-03 / PR-04 / PR-07 / PR-08 / PR-09`。

当前文档已经反复强调：

1. `GeneratedFloor` 要保留 `seed / topology / terrainTags`。
2. `PityTracker` 可以进入 `save / replay / session`。
3. `hiddenContentHarness` 要记录 `returnBridgeNodeId / revealCause`。
4. `contentPackHarness` 要记录 `pack manifest version`。

但是没有任何地方明确：

1. floor 运行时是“纯 seed 重算”，还是“保存已物化的关键生成结果”。
2. `hidden entrance` 的 reveal 状态、`ACTIVE_SEARCH` 的已执行状态、`secret zone` 是否已访问，这些状态放在 `SaveData` 的哪一层。
3. 开局后若 pack 被禁用、顺序变化、版本变化，run save 是否 fail-fast，replay 是否 fail-fast。
4. `GeneratedFloor.map` 只是兼容字段，还是也会成为 save 的一部分。
5. `topologyFingerprint`、`pack manifest version`、`content pack ids` 是否进入 replay header。

如果这块不先冻结，`Phase 4` 代码实现时一定会出现这类分裂：

1. `tools` 认为可重放。
2. `save/load` 实际上只存 seed。
3. `client` 又偷偷缓存 reveal 状态。
4. `pack` 变更后 replay 复不出来。

建议在 `P4-X` 里显式写死：

1. `run save` 至少保存：`buildId`、`contentSchemaVersion`、`activePackIds`、`activePackManifestVersions`、`floorSeed`、`topologyFingerprintVersion`、`revealedEntranceIds`、`visitedSecretZoneIds`、`PityTracker`。
2. `ProfileData` 明确不保存任何 run 内生成内容状态。
3. replay header 至少带：`phaseId`、`buildId`、`content pack ids`、`manifest versions`、`seed corpus id`。
4. pack 环境不匹配时直接 fail-fast，不做静默回退。

这是 `Phase 5 replayHarness` 能否少返工的前置条件，不是可以后补的文档细节。

---

### S-3：当前没有统一的“奖励预算 / 风险预算”总账，多个 PR 在分别往 run economy 里加钱和加压

这是我认为最容易在实现后造成数值失控的点。

现在奖励和风险被分散在多份文档里：

1. `PR-02` 有 `VaultDef.rewardBudget` 和 `threatBudget`。
2. `PR-04` 有 `ZoneLootConfig.rarityBonus / qualityBonus`。
3. `PR-05` 有 `affixBudget`、`UNIQUE / ARTIFACT`、pity。
4. `PR-06` 有 `EliteMutationDef` 和 `BossVariantDef.lootProfileOverride`。
5. `PR-07` 有 `rewardProfileId`、`guaranteedContent`。

问题不在于这些设计各自不合理，而在于**它们目前没有一个统一结算面**。这会导致两个很典型的后果：

1. 体验上，optional reward 和高压 encounter 可能在同一层叠加过满，run 后半段掉落暴涨。
2. 工程上，每个 PR 都会觉得自己只是“多加了一点奖励”，但全局合起来超过了 `Phase 4` 应有的增长斜率。

建议在 `P4-X` 新增两套横切货币：

1. `FloorRewardBudget`
2. `EncounterThreatBudget`

最少需要把这些来源收敛到统一词汇里：

1. `vault.rewardBudget`
2. `zoneLootConfig`
3. `bossVariant.lootProfileOverride`
4. `hiddenEvent.rewards`
5. `secretZone.guaranteedContent`
6. `elite mutation` 的强度抬升
7. `boss variant` 的战斗压力抬升

更直接一点说：

1. `rewardBudget` 不是只给 vault 用的。
2. `threatBudget` 也不应该只活在 mapgen 文档里。
3. `Phase 4` 需要一个统一的“本层可发放多少额外回报、可注入多少额外威胁”的总账。

如果这块不做，`lootBalanceLab` 和 `hiddenContentHarness` 最后只能证明局部系统正常，不能证明整局经济还健康。

---

### S-4：`ACTIVE_SEARCH` 已经不是一个 discovery condition 了，它实际上是一个新玩家动作，但文档还没有按“动作”建模

`PR-03` 和 `PR-07` 已经正确地把“主动搜索”提了出来，这是明显进步。但它目前仍停留在 `DiscoveryRuleType` 层，缺少动作级 contract。

当前没有被回答的问题：

1. 搜索是否消耗标准 `1000` 能量动作，还是 `interact` 的变体。
2. 搜索是否会触发怪物反应、仇恨更新、噪声、日志 token。
3. 搜索失败后是否允许连点刷判定，还是需要房间 / 节点级 cooldown。
4. 搜索动作进入 replay 时记录什么：输入事件、动作结果、还是 reveal 结果。
5. `client` 的搜索提示是上下文动作还是全局按钮；没有搜索目标时是否允许执行。

如果不在文档里把它定义成正式动作，最终最可能发生的是：

1. `core` 把它当条件。
2. `client` 把它做成按钮。
3. `tools` 只记录 reveal 成功。
4. replay 和日志无法精确说明玩家到底做了什么。

建议改法：

1. 在 `Phase 4` 明确引入 `SearchAction`，而不是只保留 `ACTIVE_SEARCH`。
2. 规定它消耗一次标准行动经济，至少要写入 `GameEvent` / `LogTokenEvent`。
3. 明确它只在带 `searchable` 标签的节点 / 房间 / entrance 上有效，避免全图无脑试探。
4. 将 `DiscoveryRule` 改为“搜索动作 + 判定规则”的组合，而不是把动作和判定混在一个 enum 里。

这是一个设计和工程都必须落地的问题，不是 UI 细节。

---

### S-5：`UNIQUE / ARTIFACT` 仍然被建模成“先参与统一 rarity roll，再因来源或模板缺失降级”，这条主线过于别扭

这点是 `PR-04 / PR-05` 当前最值得重构的地方。

目前文档的逻辑是：

1. `RarityTier` 包含 `UNIQUE / ARTIFACT`。
2. 某些来源可以 roll 到它们。
3. 但如果模板池缺失或来源不允许，就降级回 `RARE`。
4. pity 只在真正发放时重置。

这条链条能运行，但它会带来一堆额外复杂度：

1. 玩家层面会出现“系统其实 roll 到了高稀有，只是没发出来”的暗箱感。
2. 设计层面会出现“概率表上的 jackpot”和“实际可发放 jackpot”两套口径。
3. 工程层面要处理模板缺失、来源过滤、pity 不重置、trace 解释的组合状态。

更干净的方案是改成两段式：

1. 第一段只 roll `NORMAL / MAGIC / RARE`。
2. 第二段只在 eligible source 上，执行 `UNIQUE / ARTIFACT upgrade roll`。

这样有三个好处：

1. `LootBudget` 的主概率模型更稳定。
2. `jackpot` 的来源过滤和模板存在性变成前置 eligibility，不再需要“命中后降级”。
3. pity 只需要关注“upgrade roll 是否命中”，语义更直观。

如果团队不想动公式结构，至少也要补一个 `eligibilityMask` 或 `availableSpecialTiers` 的前置过滤层，而不是把不可发放 tier 直接塞进总权重再事后降级。

---

### S-6：`OverlayOp` 的语义仍然过宽，`APPEND / DENY` 在 `Phase 4` 里继续保留会显著拉高 loader 复杂度

这件事不是“我不喜欢复杂功能”，而是现在这套文档对 `APPEND / DENY` 的定义仍然不够工程化。

当前的问题：

1. `APPEND` 没有明确限定哪些字段可 append。
2. `APPEND` 没有定义针对 list 内重复项的去重 / 顺序 / conflict 规则。
3. `DENY` 没有明确限定哪些 registry 可以 deny，哪些只能 fail-fast。
4. 一旦把 `APPEND` 做成通用能力，实际上就是在做一套轻量 patch DSL。

在 `Phase 4` 里，这个收益和复杂度比并不划算。

我的建议是二选一：

1. 保守方案：`Phase 4` runtime 只正式支持 `ADD + whole-entry REPLACE`，`APPEND / DENY` 只保留在 fixture/lint 层，不进入官方 sample pack 主路径。
2. 激进方案：如果坚持保留 `APPEND / DENY`，必须补一份 `Overlay Allowed Targets` 表，逐 registry 指定可用 op 和可 append 字段，禁止“对任意内容随便 append”。

当前文档更接近“想要能力”而不是“可以直接实现的 merge contract”。

---

## 3. 系统级优化建议

### A-1：`SolvabilityGraph` 与 `SecretZone` 仍有多处 stringly typed，建议尽快升级为 typed ref

风险最大的字段：

1. `grants: Set<String>`
2. `requiredKeys: Set<String>`
3. `returnBridgeNodeId: String`
4. `guaranteedContent: List<String>`
5. `OverlayEntry.registry: String`

这些字段都跨 loader、runtime、harness、report 使用。继续放 `String`，短期省事，长期必然出错。

建议至少引入：

1. `NodeId`
2. `RequirementRef`
3. `RegistryRef`
4. `ContentRef`

这能明显降低 `PR-03 / PR-07 / PR-08` 的 fail-fast 成本。

---

### A-2：把 `mentalPower` 直接当成探索 / 感知值，设计上过于偏科

`PR-03` 把发现检定写成 `mentalPower >= difficulty`，实现简单，但玩家体验会很硬。

问题在于：

1. 这会让“探索能力”被单一属性绑死。
2. 非 mental build 会天然错过一整类 hidden content。
3. 它和未来 `PerceptionState`、装备探测 affix、职业特性之间很难衔接。

更好的做法是：

1. 在 `Phase 4` 定义一个轻量 `PerceptionScore` 或 `AwarenessScore`。
2. 它默认由 `mentalPower` 主导，但允许装备、buff、职业被动叠加。
3. 文档层面把 discovery rule 写成消费 `PerceptionScore`，而不是绑定某个现存属性名。

这样 `Phase 5` 要做 AI 感知、stealth、death analysis 时不会再回头拆。

---

### A-3：`ZoneMapgenProfile` 里继续携带 `ZoneLootConfig`，会让 mapgen contract 和奖励 contract 继续粘连

这个设计不是不能做，但现在已经开始显出耦合：

1. `PR-02` 冻结地图配置时，已经在提前冻结掉落 hint。
2. `PR-04` 又要消费它。
3. 后面 `PR-07` 和 `PR-06` 又各自加奖励入口。

建议拆成两个 sibling profile：

1. `ZoneMapgenProfile`
2. `ZoneRewardProfile`

zone schema 可以同时引用二者，但文档语义要分开。这样以后改掉落斜率不会碰 mapgen 文件，改地形混合也不会误伤 loot contract。

另外，这里已经出现了一处非常具体的权威漂移：`PR-02` 给部分 zone 写出的 `rarityBonus` 数值，和主 `Phase 4` 文档中的冻结表并不一致。这个问题不能靠“实现时自行对齐”解决，必须先把 PR 文档和主文档统一，否则等于默认允许双份权威存在。

---

### A-4：`EliteMutation` 和 `BossVariant` 缺少显式 threat delta，后续难以把 encounter 压力纳入统一预算

现在 `PR-06` 已经很好地约束了：

1. mutation 类型
2. 楼层范围
3. zone 范围
4. 互斥关系

但还缺一个真正用于 balancing 的字段：

1. `threatCost`
2. 或 `encounterDeltaScore`

原因很简单：

1. “这个 mutation 有多强”不能只靠设计师感觉。
2. 如果以后要做 `bossHarness`、`longRunLab`、`DeathAnalysis`，需要一条结构化解释线。
3. `BossVariant` 也需要知道自己相对 base encounter 增加了多少压力，而不是只知道“多了哪些内容”。

建议给 `EliteMutationDef` 和 `BossVariantDef` 都增加显式的 threat 标尺，并让 `Phase 4` harness 能输出 encounter pressure 汇总。

这里还要额外指出一个更硬的边界问题：`PR-06` 当前把 `BossVariantDef` 扩展到了 `phaseOverrides`，但主 `Phase 4` 文档的冻结口径仍然是“变体只覆盖 mutation、loot、表现 key 和少量动作权重”。如果团队决定保留 `phaseOverrides`，那必须先回写主文档；如果不准备回写，就应该把 `PR-06` 收回到主文档已允许的白名单内。现在这不是风格分歧，而是直接的合同冲突。

---

### A-5：`PR-06` 的验证入口选错了，Boss variant 和 terrain interaction 不应该主要挂在 `hiddenContentHarness` 下面

这是验证策略层的问题。

当前 `PR-06` 的自动化命令是：

1. `:core:test`
2. `hiddenContentHarness`
3. `clientSmoke`
4. `goldenScreenshot`

问题在于：

1. terrain interaction 本质上属于 `combat`。
2. boss variant 本质上属于 `boss encounter`。
3. 把它们挂在 `hiddenContentHarness` 下面，会让报告语义变脏。

建议改成：

1. terrain interaction 增加独立 isolated harness 或至少一个 `terrainInteractionBatch`。
2. boss variant 扩展既有 `bossHarness`，而不是绕过它。
3. 若要保留 `hiddenContentHarness`，它只消费 `PR-06` 的结果，不承担 `PR-06` 的主验证职责。

否则 `Phase 4` 虽然新增了系统，但真正该强化的 Phase 3 骨干 harness 反而被跳过了。

---

### A-6：`ContentPackManifest.harnessSeeds` 放在 runtime manifest 里，是 `tools` 语义侵入 `game` contract

这是一个很典型的边界污染点。

`harnessSeeds` 是 QA / reproducibility 元数据，不是 pack 在运行时的内容描述。

把它放在 `manifest.yaml` 里的问题是：

1. 运行时 manifest 被迫知道测试语义。
2. sample pack 和正式 pack 的 contract 混在一起。
3. 未来要发布 pack 时，用户会看到一堆只对 harness 有意义的字段。

建议改成 sidecar：

1. `manifest.yaml` 只保留 runtime contract。
2. `harness.yaml` 或 `tools/fixtures/content-packs/<packId>.yaml` 存测试 seed、fixture 顺序、双包场景。

这是非常值得立即修的一个边界问题。

---

### A-7：`sample.flooded_relics` 的内容范围仍然偏大，建议把“演示 pack”与“回归夹具 pack”明确分开

`PR-09` 现在希望一个 sample pack 同时证明：

1. hidden event
2. secret zone
3. unique / artifact
4. elite mutation overlay
5. dual-pack precedence

这对演示来说很热闹，但对验证来说是坏事。因为一旦 harness 挂掉，很难知道是 loader、资源、reward bridge 还是 hidden logic 出的问题。

建议拆成两类 pack：

1. `official sample pack`
   - 只演示最核心、最可见的一条内容扩展路径
2. `fixture packs`
   - 只服务 loader/lint/harness
   - 覆盖 `REPLACE / APPEND / DENY / precedence / conflict`

不要把“演示玩家价值”和“验证工程边界”混在同一个 sample pack 里。

---

### A-8：`MapgenPipeline.run(request, profile)` 暴露了不该暴露的调用边界

当前 `PR-01` 把 `MapgenPipeline` 冻结成 `run(request, profile)`，这看起来只是传参方便，但它会带来边界泄漏：

1. `core` runtime contract 被迫感知 `ZoneMapgenProfile`。
2. `game` 的 registry / fallback / future pack overlay 解析逻辑会被调用方直接承担。
3. 后面要做 pack 化 mapgen 或 zone profile override 时，容易让 `profile resolver` 在多个模块长出来。

更稳的边界应该是：

1. `MapgenPipeline.run(request)` 是正式入口。
2. `ZoneMapgenProfileResolver` 由 `game` 或 pipeline 内部持有。
3. 调用方不直接传完整 profile。

这能明显减少 `PR-01 / PR-02 / PR-08` 后续的耦合面积。

---

### A-9：`returnBridgeNodeId` 不应该作为静态 schema 的核心字段直接存在

`PR-07` 当前把 `returnBridgeNodeId` 设计成内容定义中的字符串，并要求加载期 fail-fast。

问题在于：

1. `nodeId` 本质上是 mapgen 实例产物，不是稳定内容主键。
2. static schema 里直接写 runtime node id，会让 content 定义和 floor 实例耦死。
3. 这也会让 `SecretZoneDef.entryRule` 和 `PR-03` 的 hidden entrance / discovery rule 出现双真源。

更合理的做法是：

1. 静态内容只声明 `entranceBindingId`、`returnBridgePolicy`、`anchorTag` 或等价锚点。
2. mapgen / solvability 实例化阶段绑定真实 node。
3. harness 和 proof 验证实例级连通性，不让静态 schema 直接引用实例 node id。

这会比继续在 schema 里堆字符串更稳。

---

### A-10：`UNIQUE >= 12 / ARTIFACT >= 4` 的门槛太偏“数量”，不够偏“覆盖”

`PR-05` 给了最小模板数量，这是好事，但这还不够。

更关键的是覆盖面，而不是数量本身。

建议把 gate 改成“数量 + 覆盖”双门槛：

1. 不同 zone 至少有可见差异。
2. 不同 build archetype 至少有对应奖励。
3. 不同掉落来源要有语义差异。

否则很容易出现：

1. 模板数够了。
2. 但全是同一风格的 stat stick。
3. 实际对 replayability 的贡献远低于文档预期。

---

### A-11：`DiscoveryRule.secondaryCondition` 用递归结构表达组合条件，后续可读性和 lint 性都会变差

递归不是错，但这里的组合关系其实很有限。

如果 `DiscoveryRule` 后续只需要：

1. 单条件
2. `AND`
3. 少量 `OR`

那更好的做法是：

1. `conditions: List<DiscoveryPredicate>`
2. `combinator: AND / OR`

而不是让一个 `DiscoveryRule` 套一个 `secondaryCondition`，那样后面做 lint、白盒展示、日志解释都不够直观。

---

### A-12：content pack 目前只定义了 precedence happy path，失败面和诊断模型还不够

`PR-08 / PR-09` 已经写了 `dependencies`、precedence 和双包场景，这是对的。但 loader contract 还缺少失败面：

1. 缺失依赖如何报错。
2. 依赖环如何报错。
3. `versionRange` 冲突如何报错。
4. 同优先级覆盖同一 target 时如何解释。
5. namespace 冲突如何输出可读诊断。

如果不把这些写进 lint / harness 输出，最终会出现“能失败，但不知道为什么失败”的 pack loader。

---

### A-13：各 PR 已经接受了统一报告头，但每份 PR 文档还没有把“版本字段”和“指纹版本”写实

主 `Phase 4` 文档已经有 `HarnessReportHeader`，这是非常好的收敛点。但 PR 级文档里还缺少两类 version：

1. `topologyFingerprintVersion`
2. `lootFormulaVersion / overlayContractVersion / secretRuleVersion`

原因不是为了形式化，而是为了让：

1. golden 重录有边界。
2. 统计漂移有版本线。
3. `Phase 5` 的 `DeathAnalysis / replay / QA` 不必靠 commit 猜口径。

建议每条大 contract 至少有一个轻量 version id，哪怕一开始只是整数。

---

## 4. 最大胆但值得考虑的重构方案

### R-1：把官方内容也视为“base pack”，官方内容和外部 pack 走同一套装配流水线

这是高成本方案，不是我要求马上做的事，但它确实是最干净的长期解。

如果团队愿意做大改，我建议考虑：

1. official content 在装配层先标准化成 `base pack`。
2. 外部 pack 与官方 pack 使用同一 merge pipeline。
3. `contentPackHarness` 不再只测外部 pack，而是测“base pack + overlay pack”的统一链路。

收益：

1. 不会有“官方内容一套 loader，外部 pack 另一套 loader”的双轨问题。
2. overlay、manifest、i18n/visual/audio merge 的行为更容易一致。
3. `Phase 5` 发布、QA、localization 的入口更统一。

代价：

1. Phase 4 工作量会上升。
2. 需要更早收口 base registry 装配。

如果不做这个重构，也至少要做到：

1. official registry assembly 和 pack overlay merge 在代码路径上尽可能共享 resolver 和校验逻辑。

---

## 5. 逐 PR 追加意见

| PR | 当前最强点 | 必须补的点 |
| --- | --- | --- |
| `PR-01` | 已经补上“最小非 BSP planner”，不再是空包装 | 写清 `GeneratedFloor.map` 的移除时点；补 `topologyFingerprintVersion`；把 `MapgenPipeline` 收回到不泄漏 `profile` 的正式边界；明确 save/replay 至少保存哪些 floor-level 元数据 |
| `PR-02` | topology 到 biome/vault 的流水线顺序清晰 | `ZoneLootConfig` 建议拆到 sibling profile；`rewardBudget / threatBudget` 建议升级为可跨系统复用的统一货币；修正文档中与主文档冲突的 zone bonus 数值；不要在 mapgen PR 里过早冻结完整 `rewardBudget -> LootBudget` 转换 |
| `PR-03` | 已吸收回溯 proof 和主动搜索 | 把 `ACTIVE_SEARCH` 从 discovery 条件升级成动作 contract；把 `mentalPower` 换成独立 `PerceptionScore`；typed refs 取代原始字符串；统一 hidden trigger taxonomy，避免和主文档、`PR-07` 分叉 |
| `PR-04` | pity 已经不再停留在 review 口头建议层 | 重构 `UNIQUE / ARTIFACT` 的 eligibility；补 `save/replay` 的最小状态字段；避免“命中后降级”成为主路径 |
| `PR-05` | affix cost 和 `castSpeed` DR 接线思路正确 | 把模板门槛从“数量”升级为“数量 + 覆盖”；加入 `affixFamily / exclusiveGroup` 的限制，避免组合退化成纯数值堆叠 |
| `PR-06` | 明确禁止第二套 AI / telegraph，很重要 | 增加 `threatCost`；主验证入口切到 `bossHarness` 和独立 terrain harness；`lootProfileOverride` 需纳入统一奖励总账；若保留 `phaseOverrides` 必须先回写主文档，否则应收回到当前白名单 |
| `PR-07` | reward bridge、return bridge、可读性意识都很对 | `SearchAction` 的输入、能量、日志、重放、冷却语义要补；`guaranteedContent` 和 `returnBridgeNodeId` 要 typed 化；最好改成 anchor/policy 绑定，而不是静态 node id |
| `PR-08` | 已经把 sample pack 与 fixture pack 的职责分开看待 | `harnessSeeds` 移出 runtime manifest；收窄 `APPEND / DENY`；补 dependency cycle、version range 冲突和 deterministic pack order 的失败面诊断；若保留 `APPEND` 需补 `fieldPath / mergePolicy / dedupeKey` |
| `PR-09` | 认识到了需要真实 sample pack，而不是只停在接口层 | sample pack 再缩小；第二夹具和主 sample 分离；优先证明一条清晰的玩家可见路径，而不是一次把所有能力都塞进去；明确 pack 资源最终落在 pack 目录而不是污染 base canonical manifest |

---

## 6. 建议的执行顺序调整

我建议把真正的开工顺序改成下面这样：

1. 先补 `P4-X Cross-Cutting Contracts`。
2. 回写 `roadmap.md`、`phase4-procgen-loot-and-content-pack.md`、`verification-checklist.md`。
3. 回写 `PR-01 ~ PR-09` 中涉及横切合同的段落。
4. 再进入实现。

实现节奏建议：

1. `PR-01` 和 `PR-04` 仍可并行起步。
2. `PR-02` 紧跟 `PR-01`，但要先吃掉统一 reward/threat 词汇。
3. `PR-03` 必须等 `SearchAction` 和 persistence 口径冻结后再写。
4. `PR-05` 必须在特殊 tier eligibility 收口后再扩模板池。
5. `PR-06` 可以提早并行，但不能绕开 `bossHarness / combat` 主验证线。
6. `PR-07` 必须建立在 `PR-03 + PR-05 + PR-06` 的正式 event / reward / action contract 之上。
7. `PR-08` 必须先完成 runtime/test metadata 分层，再写 loader。
8. `PR-09` 最后做，而且 sample pack 只演示最核心路径。

这不是在拖慢节奏，而是在避免“看起来并行，实际在把返工延后”。

---

## 7. 建议同步回写的文档

这轮 review 之后，我建议至少同步回写以下文档：

1. `docs/phase4/roadmap.md`
2. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
3. `docs/phase4/2026-03-13-phase4-verification-checklist.md`
4. 新增 `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
5. `docs/phase4/2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md`
6. `docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md`
7. `docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md`
8. `docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md`

原因很简单：真正需要补的不是“多一条建议”，而是让这些跨 PR 的合同从“评审意见”变回“执行输入”。

---

## 8. 最终判断

我的最终判断是：

1. 当前 `Phase 4` 文档集已经足够证明团队在正确方向上。
2. 但它还差最后一轮“横切合同收口”，才能从“高质量设计稿”升级成“低返工实现稿”。
3. 这轮最该补的不是更多内容，而是更少但更硬的边界：
   - save/replay
   - reward/threat ledger
   - search action
   - pack runtime/test split
   - special tier eligibility
   - overlay scope

如果这六件事先补好，我认为这套 `Phase 4` PR 级文档就可以直接进入实现阶段，并且大概率不会在 `PR-05 / PR-07 / PR-08` 再出现结构性返工。

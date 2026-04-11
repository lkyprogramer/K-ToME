# Phase 4 深度审查报告 v4 — Part 4：延后问题登记与最终结论

- **审查日期**：2026-04-11
- **审查分支**：`codex/phase4-opt-pr-06-terrain-uptake-tuning`
- **关联文档**：Part 1（一致性矩阵）/ Part 2（玩法体验总评）/ Part 3（P0/P1/P2 可执行建议）

本部分的目的：
1. **§1** 明确登记"**可以安全延后到 Phase 5** 的问题"——这些问题不影响 Phase 4 交付，但必须被 Phase 5 接收作为 input
2. **§2** 明确登记"**绝对不能延后** 的问题"——防止误将 P0/P1 扔给未来
3. **§3** 给出 Phase 4 的最终结论
4. **§4** 给出下一步行动建议（决策者视角：要不要发 Phase 4？要不要开 opt PR-07？）
5. **§5** 给出本次审查本身的自检（Claude 做错了什么，哪里判断有风险）

---

## 1. 可延后到 Phase 5 的问题登记

以下条目 **可以安全延后**，因为它们：
- 不影响 "Phase 4 核心循环可感知"
- 不被任何 P0 阻塞
- 延后不会让后续修复变难

### 1.1 DEFER-01：Content Pack 的 APPEND / DENY overlay 支持

**来源**：CI-11（P2），Part 1 §4

**现状**：`DataLoader.kt:523-530` 的 runtime 只支持 `ADD / REPLACE`，`APPEND / DENY` 在运行时直接抛 `content-pack.overlay.runtime-op-forbidden`；但 manifest lint 阶段可能支持全部 4 种 op。

**为何可延后**：
- ADD / REPLACE 已能覆盖 80%+ 的 mod 场景（新增物品、替换 loot profile）
- APPEND / DENY 是进阶用法，目标人群是高级 mod 作者
- Phase 4 没有依赖这两个 op 的 exit gate

**Phase 5 接收时的要求**：
- Schema 层 op 枚举完整
- Lint 层已识别 APPEND / DENY
- Runtime 层需要补：APPEND 对 list 字段的合并语义、DENY 对目标 id 的排除语义
- 需要新增 harness 用例覆盖

### 1.2 DEFER-02：Boss variant 数量扩展

**现状**：3 个 boss variant（molten_glass / grey_crown / abyssal_eclipse），覆盖 3 个 zone 的 boss；Phase 4 plan 原目标 "至少 3 个" 已达成。

**为何可延后**：
- 数量已满足原契约
- Boss 体验短板的根因是"单个 variant 的 flavor 深度"而不是"数量"——修完 P1-B（grey_crown terrain flavor）后体感已经能提上去
- Phase 4 exit gate 没有"每个 zone 必须有 2+ boss variant" 之类的要求

**Phase 5 接收时的要求**：
- 每个强制主线 zone 提供 ≥ 2 个 boss variant，提升重玩价值
- 引入 "boss-specific action weight profile" 让不同 variant 在 action selection 上有显著差异
- Boss variant 的 `preferredTerrainTags` 必须非空（Phase 5 硬性要求）

### 1.3 DEFER-03：Artifact 跨 zone 可获取性

**现状**：每个 artifact 都被 `allowedZones: [single_zone]` 锁死在单一 zone 的特定 source tier（BOSS / CHEST）。

**为何可延后**：
- 当前 `uniqueArtifactMeaningfulSwapRate = 100%` 说明拾装决策已经有意义
- artifact 的稀缺性对单局游戏体验有正向影响
- 跨 zone 获取会增加 loot 平衡测试的组合空间，现阶段不紧迫

**Phase 5 接收时的要求**：
- 设计 "artifact 跨 zone 低概率出现" 规则（比如 Boss drop 5% 概率从全局 artifact pool 抽）
- 新增 metric：artifact 完整获取率在多局游戏中的分布

### 1.4 DEFER-04：Material tier 渐进使用曲线

**现状**：4 种 material（IRON/STEEL/MITHRIL/ADAMANTITE）但 artifact 多数硬编码 STEEL；`fixedMaterialId` 字段让玩家无从看到 material tier 的进阶感。

**为何可延后**：
- 不影响 Phase 4 loot 数学框架的正确性
- material 作为装备修饰层，本身不承载核心玩法
- 修起来需要重写多个 artifact 的 material 绑定 + 调平衡

**Phase 5 接收时的要求**：
- 按 zone 级别定义 material distribution（shattered_outpost 主打 IRON，deep_iron_pit 解锁 STEEL，abyssal_temple 解锁 MITHRIL/ADAMANTITE）
- Material tier 对 affix cost / stat modifier 有显式加成

### 1.5 DEFER-05：Hidden primer 的 UX hint 系统

**现状**：`required_tag: hidden.primer.deep_iron.slag_cache` 这种 gating 存在于 event 定义中，但玩家不知道怎么获得 primer——当前只有 KILL_ELITE / OPEN_CHEST 等 trigger type 的通用 hint。

**为何可延后**：
- 不阻止玩家完成游戏主线
- 属于"探索体验优化"范畴，技术债较小
- 需要 client 侧 UI 支持（hint overlay、minimap icon），属于前端工作

**Phase 5 接收时的要求**：
- 每个 primer 有 "how to obtain" 文案
- 获得后在 minimap 或 journal 有视觉反馈

### 1.6 DEFER-06：4 个 zone 到 11 zone 的完整 content pack 示例

**现状**：仅 `sample.flooded_relics` content pack 存在，验证了 ADD/REPLACE 主路径。

**为何可延后**：
- 核心框架已验证
- mod 作者生态建设是长期工作
- 不影响 Phase 4 玩家体验

**Phase 5 接收时的要求**：
- 至少 2 个示例 content pack 覆盖 "小型变体扩展" 和 "新 zone 整体 mod"

### 1.7 DEFER-07：Hidden entrance binding 家族扩展

**来源**：CI-09 (P2)

**现状**：3 个 binding (hidden.branch / hidden.critical.adjacent / hidden.goal.adjacent) 均被使用，但 `hidden.branch` 占 3/5。

**为何可延后**：
- 3 种已达成 coverage metric 目标
- 扩展 binding 家族涉及 `NodeAnchorId` 语义、`TopologyGraph` 的 anchor 生成等底层改动
- 不紧急

**Phase 5 接收时的要求**：
- 引入 `hidden.shortcut`、`hidden.optional.branch.2` 等家族
- 每 binding 家族至少 1 个 secret zone 使用

---

## 2. 绝对不能延后的问题（防止误延后）

以下问题在本审查过程中，有潜在风险被误判为 "Phase 5 再说"——在此**显式登记反对延后理由**，以防未来争议。

### 2.1 NO-DEFER-01：mapgenProfileId 覆盖不全（CI-01 / P0-A）

**为何不能延后**：
- Phase 4 的核心承诺是 "HybridTopology 管道是新的标准"，而 7/11 zone 没用这个管道 = Phase 4 只对 36% 的 zone 生效
- 延后到 Phase 5 等于默认"Phase 4 的交付面 = 4 个 zone"，这与 opt plan 承诺严重不符
- 3 个强制主线区缺失直接破坏玩家体验曲线
- 每多一个 PR 延后，回填的技术债越深——因为 Phase 5 可能会进一步依赖 "所有 zone 都用 HybridTopology" 的假设

### 2.2 NO-DEFER-02：greenwood 单区 encounter rate 不达标（CI-03 / P0-C）

**为何不能延后**：
- greenwood 是玩家第一个 Phase 4 区，首印象不可复得
- 当前 aggregate pass 是结构性幸运，随便一次 tuning 就会破
- 修复成本小（新增 1 个 monster + 1 条 rule）

### 2.3 NO-DEFER-03：terrain metric zone 覆盖漂移（CI-02 / P0-B）

**为何不能延后**：
- 度量体系本身不正确 = 未来所有"gate 合规"结论都可疑
- 修复成本极小（改 harness 的 zone 选择逻辑）
- 不修这个会让 R-P0-1 的效果无法被验证

### 2.4 NO-DEFER-04：secret loot 身份重叠（CI-05 / P0-E）

**为何不能延后**：
- 秘境奖赏是 Phase 4 opt PR-05 的核心卖点；损失这一部分等于承认 Phase 4 的秘境系统失败
- 修复成本低（改 5 个 loot profile）
- 延后只会让玩家对秘境系统的信任度下降

### 2.5 NO-DEFER-05：preferredTerrain 链路只有 WATER 被观测到（CI-04 / P0-D）

**为何不能延后**：
- opt PR-06 的完整命题是 "3 种 terrain tag（WATER/ICE/OIL）都要让玩家感受到战术差异"，目前只 1/3 兑现
- 2/3 命题未验证 = opt PR-06 的"tactical uptake"卖点名不副实
- 修完 P0-A 后大概率自然修复（crystal_cavern / abyssal_temple 进入管道，OIL/ICE tile 真正生成），值得 opt PR-07 一并推进

---

## 3. 最终结论

### 3.1 Phase 4 当前状态（2026-04-11 18:00 本地时间）

- **Harness 层**：12/12 task PASS, 15/15 experience metric PASS, 0 failed gate
- **代码层**：opt PR 01~06 的全部承诺 code + YAML 已合入 working tree
- **度量层**：汇总 exposure +76.84% vs baseline, encounter +31.58% vs baseline
- **玩家体验层**：从本审查的"**Phase 4 真实覆盖率 ≈ 4/11 zone**"和"**greenwood 单区 11.4%**"看，**玩家在一局主线内仅有约 35% 的层是真正的 Phase 4 体验**

**数字很好，结构不够**。

### 3.2 Phase 4 是否可以发版？

**直接回答**：**不建议以当前状态宣布 "Phase 4 完结"。建议把 Phase 4 的 "closure" 时间点延后到 opt PR-07 合入之后**。

**论证**：

- **"gate pass = 完结"的标准太弱**：当前 gate 是聚合口径，体现的是 "样本平均值合格"，而不是 "所有玩家路径都合格"
- **"完结" 一旦宣布就绑定 Phase 5 的上下文**：Phase 5 的任何新内容都会建立在 "Phase 4 已完整交付" 的默认假设上，发现问题后回填的成本会显著上升
- **opt PR-07 的估算成本不高**：P0 部分约 5~8 人日，和已经投入的 opt PR 01~06 的体量相比属于"收尾成本"，完全值得
- **真实体验反馈没有进入 gate 体系**：本审查的 CI-01 / CI-02 / CI-03 / CI-04 / CI-05 五条 P0 都是在度量数字全绿之后被找出来的——这说明度量系统本身对这些问题**没有识别能力**

### 3.3 Phase 4 的交付评级

按 "Phase 4 opt plan 的 exit gate vs 玩家真实体验需求" 两个维度双重评估：

| 维度 | 评级 | 理由 |
|---|---|---|
| **按 exit gate 口径** | **B+** | 所有 PR 承诺的 affix/unique/artifact/mutation/boss/hidden/secret/terrain 量化目标全部达成 |
| **按 "Phase 4 真正生效于所有 zone" 口径** | **C+** | 4/11 zone 覆盖，3 个强制主线区缺失 |
| **按 "玩家每局体感新鲜度" 口径** | **C** | 前 3 层 + 中盘 4 层 + 终局 1 层 = 8 层属于 Phase 3 观感 |
| **按 "度量体系本身的正确性" 口径** | **C+** | 聚合 gate 掩盖单区不达标；度量 zone 列表与 migration 列表漂移 |
| **按 "opt PR-06 tactical uptake" 兑现度** | **C** | 只有 1/3 terrain tag 真正被观测触发 |
| **综合评级** | **C+ ~ B−** | **合规交付但体验未到位** |

### 3.4 一句话最终判断

> **Phase 4 已经把 "能跑通、能测量、能扩展" 的框架做完了，但还没把 "全地图都是 Phase 4" 的核心承诺兑现**。opt PR-07 是"**Phase 4 收尾**"而不是"**Phase 5 开场**"——这个区分很重要，不要把它说成 Phase 5 的一部分。

---

## 4. 下一步行动建议（决策者视角）

### 4.1 推荐路线：立即开 opt PR-07

**行动清单**：

1. **立即**：将本审查 Part 3 §4 中的 10 条 TASK 转为 GitHub issue 或 task board 项目
2. **本日内**：分派 TASK-01 (mapgenProfileId 补全) 作为最优先项，因为它是其他 P0 的前置条件
3. **PR-07 周期**：按 P0 → P1 顺序推进，最后执行 TASK-10 (full phase4Report regression)
4. **PR-07 合入后**：重新运行本审查的 CI-01~CI-18 矩阵，验证 P0 条目转为"已兑现"
5. **PR-07 合入后**：正式发布 "Phase 4 closure" 声明

**节奏**：若本周即开工，预计 **1~1.5 周** 内完成 P0，再 **3~5 天** 完成 P1（如选做），**总工期 2~2.5 周**。

### 4.2 备选路线：接受现状发布，Phase 5 吸收

**风险**：
- Phase 5 的工作必须先还 Phase 4 的技术债，影响 Phase 5 节奏
- 玩家真实体感下 "Phase 4 的新特性感"偏薄，可能影响早期体验反馈
- 度量体系的不对称（aggregate-only）会持续影响后续 opt PR 的判断力

**收益**：
- 提前一次里程碑节点，便于外部沟通 / 宣传

**判断**：**不推荐**。节约的时间会在 Phase 5 还回去，而且玩家体验上的第一印象是不可复得的资源。

### 4.3 建议的发版声明口径（若走推荐路线）

> "Phase 4 的框架改造（mapgen / elite / loot / hidden / content-pack / terrain 六大系统）已通过 12 个 harness task 和 15 条 experience metric 的全量验证。为确保 Phase 4 的新特性在所有主线 zone（shattered_outpost、grey_gate_depths、abyssal_heart 等）一致生效，我们将在 opt PR-07 收尾 mapgen 覆盖率、terrain metric 校准和 secret loot 身份差异化三项工作，之后正式宣布 Phase 4 闭环。预计周期 2 周。"

---

## 5. 审查自检（Claude 可能的盲区与风险）

本审查花了大量篇幅做"设计契约 ↔ 实现 ↔ 运行时"比对，但仍存在以下潜在误判风险，请 reviewer 交叉验证：

### 5.1 样本量误判风险

- **deep_iron_pit combatCount = 43** 样本偏小。我将其列为 P0-D 的一个数据点，但实际可能是 terrainInteractionBatch 的 seed 抽样数量对 deep_iron_pit 偏低——而不是该 zone 的 elite 配置有问题。修复 R-P0-4 前需要先确认：是 **"没采到"** 还是 **"采到但没匹配"**。
- `deep_iron_pit.preferredTerrainCombatCount = 0` 可能是小样本下的合理零，也可能是结构性 bug。本报告倾向于后者，但没有直接的 harness replay 证据。

### 5.2 "applyToTags 针对性稀释" 的影响程度可能被高估

CI-06 / P1-A 中我声明 "所有 12 个 mutation 都含 elite，结果任何 elite-eligible 怪物都可以拿任何 mutation"。但实际还受 `allowedZones`、`minFloor`、`tierWeight`、`incompatibleWith`、`terrainAffinityWeightBonus` 多层约束，真实组合空间远小于 "12 × context"。修复时应先用 `bossHarness` 的 `eliteMutationValidPairCount = 51` 做基线比对，避免过度约束导致组合数下降。

### 5.3 Content pack runtime op 支持范围的边界

CI-11 说 runtime 只支持 ADD / REPLACE。但我没查 `ContentPackRuntimeResolver` 和 `ContentPackLintChecker`（如存在）的源码，仅基于 `DataLoader.kt:523-530` 的 `parsePackOverlayPayload` 一段做出结论。如果 lint 层/runtime 层对 APPEND / DENY 有另外一条实现路径，本结论需要修正。

### 5.4 Secret loot 身份计算的口径

本审查依据 `lootProfileBaseItemOverlapMatrix` 中 `loot.abyssal_temple.cadence ∩ loot.abyssal_temple_warded_archive.secret = 0.818` 得出 "secret / normal 重叠严重"。但该 metric 仅计算 `base item` 层的交集，不计 affix / material / quality。理论上同样 base item + 不同 affix 也能制造体感差异。**修复 P0-E 时** 应先确认 "玩家感知的 loot 差异度" 是否只能由 base item 层决定——还是 affix 层也有贡献。

### 5.5 Phase 4 zone list 的覆盖口径是否应为全部 11 zone

本审查认定 Phase 4 "应该覆盖所有 11 个 zone"。但原 opt plan 可能只要求 "Phase 4 的 4 个 flagship zone"，其余 7 个可能是故意保留 BSP 作为"对照组"或"低难度区"。如果这是原设计意图，CI-01 就不是 P0 而是 P2——需要和产品所有者确认。**如果确认 4 个是设计意图**，则 CI-01 降级为"文档与代码一致但与玩家体感预期不符"，修复方向从"补 profile"改为"在文档里显式说明 Phase 4 覆盖范围 = 4 zone"并调整玩家预期。

### 5.6 度量正确性的"元问题"

本审查发现的很多问题（CI-02 zone 漂移、CI-03 聚合掩盖、CI-05 sanity 过宽）都是**度量体系本身的设计缺陷**，而不是实现错误。这提醒我们：**opt PR-07 不仅要修代码/数据，还要修 metric 的口径定义**。一个成熟的项目需要一轮 "metric definition review"，类似 TDD 的 test-first，把"哪些维度必须被度量""哪些 gate 必须是 per-unit 而不是 aggregate" 在 PR 设计时就明确下来。这本身是一个 Phase 5 级别的系统改进。

---

## 6. 本次审查的边界

- 本审查**没有直接运行** harness / gradle check / 完整游戏，结论基于 **已提交到 tree 的 `phase4-summary.md` 快照**（buildId `phase4-opt-pr05-dev`, 生成时间 `2026-04-11T08:18:05Z`）
- 本审查**没有审查 client 侧 UI / UX 代码**，对 terrain reaction、hidden primer、secret zone return bridge 等的玩家可见反馈的判断仅基于"后端数据是否支持"
- 本审查**没有跑 `tests/` 或 `*Test.kt`**，代码正确性结论依赖 `docs/review/phase4/opt/2026-04-11-phase4-opt-pr-06-post-dev-review.md` 的代码审查
- 本审查使用的所有 file path / line number 均基于 `codex/phase4-opt-pr-06-terrain-uptake-tuning` 分支 working tree，未 commit 的修改可能未被纳入

---

## 7. Part 4 小结

- **延后项**（DEFER-01 ~ DEFER-07）：7 条，可以安全进入 Phase 5
- **反延后项**（NO-DEFER-01 ~ NO-DEFER-05）：5 条，**必须进 opt PR-07**
- **最终结论**：**Phase 4 未完结，建议开 opt PR-07 收尾**
- **下一步**：按 Part 3 §4 的 10 条 TASK 立即开工
- **审查自检**：6 个潜在盲区已显式登记

**审查正式交付。**

---

## 附录：文件交叉索引

| Part | 文件 | 规模 | 主要内容 |
|---|---|---|---|
| 1 | `phase4_opt_deep_review_claude_v4_part1.md` | ~300 行 | 执行摘要 + 18 条 CI 矩阵 |
| 2 | `phase4_opt_deep_review_claude_v4_part2.md` | ~230 行 | 玩法体验总评（核心循环 / 战斗 / 奖励 / 探索 / 系统耦合 / 可感知性评分） |
| 3 | `phase4_opt_deep_review_claude_v4_part3.md` | ~350 行 | P0/P1/P2 清单 + R-P0-1 ~ R-P2-5 优化建议 + opt PR-07 任务板 |
| 4 | `phase4_opt_deep_review_claude_v4_part4.md` | 本文 | 延后项 + 反延后项 + 最终结论 + 自检 |

**所有 4 个 Part 均在 `docs/review/phase4/v2/` 目录下**。

**审查人**：Claude Opus 4.6  
**审查日期**：2026-04-11  
**审查分支**：`codex/phase4-opt-pr-06-terrain-uptake-tuning`  
**审查状态**：完成

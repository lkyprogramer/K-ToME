# Phase 4 Director Review — Round 1

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`（274 行）
2. `docs/phase4/2026-03-13-phase4-verification-checklist.md`（78 行）
3. `docs/phase4/roadmap.md`（17 行）

**参考权威文档**：

1. `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`（核心系统详细设计）
2. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（阶段补充文档）

**审阅方法论**：以 Phase 3 已通过终审的文档体系（681 行执行文档 + 124 行验证清单 + 完整权威链）为质量基线，对 Phase 4 进行同等标准的结构性、系统性和实现就绪度审阅。

---

## 0. 总体评估

Phase 4 文档体系存在 **系统性的规格深度不足** 问题。与 Phase 3 的执行文档（681 行，含详细 Kotlin 接口骨架、YAML 示例、数值锚点、并行开发指导、per-zone 规格）相比，Phase 4 的主文档仅有 274 行，覆盖了 5 个独立子系统（混合拓扑地图、锁钥匙验证、掉落预算 V2、精英突变与隐藏事件、内容包 overlay），**每个子系统的规格深度不足以直接作为实现输入**。

当前文档更接近「设计意向书」而非「执行文档」，离 Phase 3 终审水准尚有显著距离。

**问题统计**：

| 等级 | 数量 | 说明 |
| --- | --- | --- |
| P0 | 4 | 阻塞性缺口，不解决将导致实现阶段大规模歧义或返工 |
| P1 | 14 | 重要缺口，会导致实现歧义或系统间不一致 |
| P2 | 12 | 建议补充，不解决可以绕行但会降低系统质量 |

---

## 1. P0 — 阻塞性问题

### P0-1：ProcGen 系统无算法规格与数据结构

**位置**：§4.1 混合拓扑地图

**问题**：混合拓扑地图是 Phase 4 最复杂的新系统（对标 Phase 3 的战斗公式），但当前文档仅提供了一张"最低能力表"对比 Phase 1 BSP，没有给出任何实现层面的规格：

1. **无生成算法描述**：BSP→混合拓扑的升级路径是什么？Graph Grammar？WFC（Wave Function Collapse）？手写拓扑模板 + 随机拼接？无法判断。
2. **无 Kotlin 接口骨架**：Phase 3 为每个核心系统都提供了 `data class` 和 `interface` 定义。Phase 4 的 mapgen 没有任何代码锚点。
3. **无 room/vault/pattern room 的数据 schema**：什么是 pattern room？vault 的规格是什么？如何在数据文件中定义它们？
4. **无环路生成策略**：从"树形单路径"到"含环路的多路径"是本阶段最大的拓扑跃迁，但完全没有描述如何生成和控制环路。
5. **无 biome family 定义**："同层允许双 biome 混合"的规则是什么？biome 的元数据 schema 是什么？

**影响**：开发团队无法基于当前文档开始 P4-W1 的实现，必须先补齐算法选型和接口定义。

**建议**：参照 Phase 3 §4 的详细程度，至少补齐以下内容：

```text
1. MapgenPipeline 接口骨架（Seed → 拓扑图 → 房间实例化 → 走廊/环路 → biome 着色 → 校验）
2. Room/Vault/PatternRoom 的 Kotlin 数据结构 + YAML schema
3. BiomeFamily 定义与混合规则
4. 环路生成算法的选型与约束（最小环路长度、最大环路数、环路-主路径比例等）
5. 至少一个完整的 vault YAML 示例
```

---

### P0-2：Loot Budget V2 无数学模型

**位置**：§4.3 Loot 生态 V2

**问题**：Phase 4 声明掉落质量必须由"预算驱动"，并提到了 `iLvl/qLvl/rarity budget/affix budget` 四个核心概念，但：

1. **无预算计算公式**：`iLvl` 和 `qLvl` 如何计算？与 `sourceLevel`、`sourceTier`、`playerLevel` 的关系是什么？
2. **无 affix weight/cost 体系**：affix 如何消耗预算？一个 +10 火抗前缀和一个 +5 护甲穿透前缀的预算成本分别是多少？谁来定义这个映射？
3. **无稀有度概率模型**：从 `NORMAL → MAGIC → RARE → UNIQUE → ARTIFACT` 的概率阶梯是什么？`magicFindBonus` 如何修正这些概率？
4. **无 Unique/Artifact 选择逻辑**：Unique 是预定义的还是随机生成的？Artifact 何时掉落？
5. **`SourceTier` 的 rarity bonus 值无设计依据**：ELITE `0.15`、BOSS `0.40` 等数值从何而来？

**影响**：这是 Roguelike 重复游玩性的核心引擎。无数学模型的情况下，`LootBalanceLab` 没有验收标准，掉落平衡无法自动化验证。

**建议**：

```text
1. 定义 iLvl/qLvl 计算公式（参考 Diablo-like: iLvl = sourceLevel + tierBonus + randomVariance）
2. 定义 affix budget 体系（总预算 = f(iLvl, rarity)，每个 affix 消耗固定 cost）
3. 定义稀有度概率阶梯表 + magicFind 修正公式
4. 定义 LootBalanceLab 的量化验收标准（如：1000 次掉落统计下 unique 出现率在 [X%, Y%] 范围内）
5. 提供 Kotlin 数据结构：LootBudget, AffixCost, RarityTier, LootRollContext
```

---

### P0-3：Lock-Key DAG 无形式化模型

**位置**：§4.2 Lock-Key 与可解性验证

**问题**：锁钥匙系统声明使用 DAG，但：

1. **无图数据结构定义**：节点是什么（房间？区域？门？）、边代表什么（可达性？依赖关系？）
2. **无 Key/Lock 类型枚举**：钥匙类型有哪些？物理钥匙？开关？boss 击杀？技能检定？
3. **无可解性验证算法**：什么是"可解"？单纯的可达性？还是资源充分性（玩家是否有能力获取钥匙）？
4. **主线 vs 可选内容的边界模糊**：§4.2 约束 4 说"Boss 门后不允许藏必需钥匙"，但什么算"必需"？如果一个钥匙只解锁 optional 宝箱，它算必需吗？
5. **无隐藏入口的发现模型**：隐藏入口是通过什么机制被发现的？感知检定？特定物品？地形交互？

**影响**：可解性是 ProcGen 地图的安全底线。没有形式化模型，`SolvabilityHarness` 无法实现。

**建议**：

```text
1. 定义 SolvabilityGraph 数据结构（Node = Zone/Room, Edge = Connection/Dependency）
2. 定义 KeyType/LockType 枚举
3. 定义可解性算法（建议：拓扑排序 + 前向可达性证明）
4. 区分 CRITICAL_PATH / OPTIONAL / SECRET 三级路径分类
5. 定义隐藏入口的发现条件 schema
```

---

### P0-4：Content Pack Overlay 无 schema 定义

**位置**：§4.5 Content Pack Overlay

**问题**：这是 Phase 4 独有的全新子系统（Phase 2/3 均无前置），但当前仅有 3 条冻结口径和文件路径建议：

1. **"overlay 只能扩内容定义"——但什么是 overlay？** YAML 文件覆盖？数据合并？选择性替换？
2. **无 manifest schema**：content pack 的 manifest 长什么样？有哪些必需字段？
3. **无 schema lint 规则定义**：lint 检查什么？引用完整性？字段类型？值域约束？
4. **无 headless harness 接口**：harness 如何运行？接受什么输入？输出什么报告？
5. **无 overlay 加载优先级和冲突解决策略**：两个 pack 定义了同一个 monster template 怎么办？

**影响**：Content Pack 是 Phase 4 的平台化产出，直接影响后续的内容扩展能力。无 schema 的情况下，示例 content pack 无从写起。

**建议**：

```text
1. 定义 ContentPackManifest 数据结构
2. 定义 overlay 语义（merge/replace/append/deny 四种操作）
3. 定义加载优先级与冲突解决规则
4. 定义 schema lint 规则清单
5. 定义 headless harness 接口和输出报告格式
6. 提供一个最小但完整的示例 content pack YAML
```

---

## 2. P1 — 重要问题

### P1-1：全文档无任何 Kotlin 接口骨架

**问题**：Phase 3 为 `CombatFormula`、`StatusEffectDef`、`TalentDef`、`AIProfile`、`BossEncounterDef`、`ProfessionDef` 等所有核心系统提供了 Kotlin 接口骨架。Phase 4 的 5 个子系统没有提供任何代码级数据结构。

**影响**：开发者无法判断"pattern room 的数据结构是什么"、"solvability graph 的节点/边模型是什么"、"affix budget 的字段有哪些"。

**建议**：为以下核心类型补齐 Kotlin 骨架：

- `MapgenConfig`, `RoomDef`, `VaultDef`, `PatternRoomDef`, `BiomeFamilyDef`
- `SolvabilityGraph`, `SolvabilityNode`, `KeyLockDef`, `SolvabilityProof`
- `LootBudget`, `AffixDef`, `AffixCost`, `RarityTier`, `LootRollResult`
- `EliteMutationDef`, `HiddenEventDef`, `SecretZoneDef`
- `ContentPackManifest`, `OverlayEntry`, `SchemaLintResult`

---

### P1-2：无 YAML 示例

**问题**：参考文档为伤害通道、元素交互、AI profile、怪物模板等系统都提供了详细的 YAML 配置示例。Phase 4 没有任何 YAML 示例。

**建议**：至少为以下数据提供示例：

- vault 定义
- biome family 配置
- affix 定义与预算配置
- elite mutation 定义
- hidden event 定义
- content pack manifest

---

### P1-3：精英突变系统规格空白

**位置**：§4.4 精英突变与隐藏内容

**问题**：仅 4 行冻结口径，无任何具体设计：

1. 有哪些突变类型？（数值膨胀、附加技能、AI 升级、affix 附加、光环、免疫？）
2. 突变如何生成？随机抽取？与 zone/biome 关联？
3. 突变与战斗系统的交互点在哪？修改 `MonsterTemplateV2` 的哪些字段？
4. 突变的可读性如何保证？（怪物名称前缀？状态图标？日志提示？）
5. 突变对 AI 行为的影响是什么？

**建议**：定义 `EliteMutationDef` schema，枚举首批突变类型，说明与 `MonsterTemplateV2` 和 `AIProfile` 的集成方式。

---

### P1-4：隐藏事件系统完全未定义

**位置**：§4.4 精英突变与隐藏内容

**问题**：hidden event 被列为 Phase 4 范围（§3.1 第 4 项），但文档中没有任何关于隐藏事件的设计：

1. 隐藏事件的触发条件是什么？（地形交互？怪物掉落？概率触发？quest chain？）
2. 隐藏事件的奖励模型是什么？
3. 隐藏事件与可解性验证的关系是什么？
4. 数据 schema 是什么？

**建议**：定义 `HiddenEventDef` schema，明确触发条件类型枚举、奖励模型和与 solvability 的关系。

---

### P1-5：地形标签系统未定义

**问题**：Phase 4 引入 `WATER/OIL/ICE` 地形交互（来自核心文档 §8.5 元素交互矩阵的 P4-A 条目），但 Phase 4 文档没有定义：

1. 地形标签枚举（`TerrainTag`）
2. 地形标签与 mapgen 的集成（哪些 biome 包含哪些地形？）
3. 地形标签与 `CombatPipeline` step 9 回调的连接点
4. 地形标签的持续时间和状态转换规则（冻结的冰面 3 回合后变回水面？）

这些在核心文档中有设计锚点（ElementInteractionRule + TERRAIN_TRANSFORM 效果），但 Phase 4 应当明确落地路径。

---

### P1-6：与 Phase 3 战斗系统的衔接缺失

**问题**：Phase 4 §4.3 提到"Phase 4 与元素系统的连接也必须写死：LIGHTNING+WATER、FIRE+OIL、COLD+WATER/ICE"。这些交互已在核心文档 §8.5 中定义了完整的 `ElementInteractionRule` 和 YAML 配置。但 Phase 4 文档：

1. 没有引用核心文档中已有的交互规则定义
2. 没有说明这些交互如何集成到 Phase 3 已冻结的 `CombatPipeline` 回调系统中
3. 没有区分"规则定义"和"地形生成支持"两个独立的实现任务

**建议**：明确区分"在 `CombatPipeline` 中激活已定义的 `TARGET_ON_TERRAIN` 类型交互规则"与"在 mapgen 中生成带有 WATER/OIL 标签的地形"两项任务。

---

### P1-7：工作包无依赖关系与并行指导

**问题**：Phase 3 明确定义了 4 条并行开发 lane 和工作包间的依赖图（如"P3-W1 结束后，P3-W2 与 P3-W3 允许并行推进"）。Phase 4 的 P4-W1~W5 只列出了内容范围，没有：

1. 依赖关系（P4-W2 依赖 P4-W1 的地图生成，P4-W3 依赖 P4-W1 的 zone 定义吗？）
2. 并行执行建议
3. 各工作包的预估工作量或时间框架

**建议**：补充工作包依赖图和并行策略，至少标明关键路径。

---

### P1-8：Boss 变体与 Phase 3 BossEncounterDef 的关系未说明

**位置**：§4.4 P4-W4 "Boss 变体"

**问题**：Phase 3 已冻结了 `BossEncounterDef` 和 `BossPhaseDef` 的完整 schema。Phase 4 提到"Boss 变体不得破坏基础 encounter contract"，但没有定义：

1. 什么是"Boss 变体"？同一 Boss 的不同 phase 组合？不同 affix 的 Boss 实例？
2. 变体如何在 `BossEncounterDef` 中表达？新增字段？继承覆盖？
3. 变体与精英突变的区别是什么？

---

### P1-9：验证清单缺乏量化门槛

**位置**：Phase 4 Verification Checklist 全文

**问题**：Phase 3 的验证清单包含具体量化标准（如覆盖率百分比、soak 时长、具体的 golden seed 数量）。Phase 4 的验证清单使用了大量定性描述：

| Phase 4 原文 | 应当量化为 |
| --- | --- |
| "mapgen smoke 无崩溃、无空图、无主线死局" | 多少个 seed？通过率阈值？超时限制？ |
| "loot budget 无明显越界" | 什么是"越界"？偏离预算 ±X%？unique 出现率区间？ |
| "solvability harness 批量 seed 通过" | 批量 = 多少？100？1000？10000？ |
| "hidden content 可被触发并可被验证" | 在多少 seed 的多少次 run 中被触发至少一次？ |

**建议**：为每项验证补充量化门槛。参考值：

```text
- mapgen smoke: ≥500 seed，0 崩溃，0 不可解主线，生成时间 <2s/map
- solvability harness: ≥1000 seed，100% 主线可达
- loot budget: 10000 次 roll，各 rarity tier 偏离预期 <±5%
- hidden content: ≥100 seed 中至少 N% 的 run 可触发至少 1 个隐藏事件
```

---

### P1-10：Secret Zone 规格完全缺失

**问题**：§4.4 和验证清单都提到了 "secret zone"，但文档中没有任何关于 secret zone 的定义：

1. secret zone 是什么？隐藏的额外区域？普通区域的变体？
2. 进入条件是什么？
3. 与 lock-key DAG 的关系是什么？
4. 内容（怪物、宝箱、事件）如何配置？

---

### P1-11：元素亲和度系统设计缺失

**问题**：核心文档 §4.5 Phase 4 部分明确提到"元素亲和度系统（可选）：连续使用同一元素的天赋提升该元素的伤害/效果"。但 Phase 4 文档完全没有提及这个系统。

**建议**：要么在 Phase 4 文档中给出设计，要么显式标注为"P4 非目标，推迟至 P5 或后续版本"。不应在核心文档中声明引入、但在阶段文档中静默忽略。

---

### P1-12：MagicFind 来源与上限未定义

**位置**：§4.3 `LootBalanceLab` 输入上下文

**问题**：`magicFindBonus` 被列为必需输入，但没有定义：

1. MagicFind 的来源（装备 affix？天赋？药水？区域 modifier？）
2. MagicFind 的计算公式（加法叠加？乘法？有无上限？）
3. MagicFind 对掉落概率的具体影响方式

---

### P1-13：Phase 4 Zone 定义缺失

**问题**：核心文档 GAP-11 明确指出"4 个 Phase 2 Zone 只有名字没有层数/面积/怪物池/机制"。Phase 3 理应补充了部分 Zone。但 Phase 4 作为"高重复游玩价值"的阶段，没有定义：

1. Phase 4 引入哪些新 Zone（如果有的话）？
2. 已有 Zone 如何升级以支持混合拓扑和 biome family？
3. 每个 Zone 的 biome 配置、地形标签分布、精英/Boss 池是什么？

---

### P1-14：Content Pack 与 i18n/visual/audio key 的集成边界未定义

**位置**：§4.5 冻结口径 3

**问题**：冻结口径说"manifest、i18n、visual/audio key 也必须纳入 pack 校验"，但没有定义：

1. Content pack 的 i18n 文件结构是什么？
2. Visual/audio key 如何在 pack 中声明和解析？
3. 缺失 key 的处理策略是什么？fallback？错误？警告？

---

## 3. P2 — 建议补充

### P2-1：无并行开发 lane 定义

Phase 3 定义了 Rules / Client / Content / Tools-QA 四条 lane。Phase 4 的系统构成不同，但同样存在可并行的维度（ProcGen vs Loot vs Content Pack）。建议定义 Phase 4 的开发 lane。

### P2-2：Golden Seed 基线更新策略缺失

Phase 4 大幅改变了 mapgen，这意味着所有与地图相关的 golden seed 基线都需要更新。文档应明确：

1. 哪些现有 golden seed 会被影响？
2. 基线更新的 checkpoint 是什么？
3. 新增 golden seed 的数量和覆盖范围。

### P2-3：非目标范围不够精确

§3.2 只说了"不引入 Lua runtime"、"不做完整脚本宿主"。但应当排除所有嵌入式脚本引擎（Lua、GraalJS、WASM、Python），避免后续歧义。同时应明确 content pack 的表达边界：pack 可以定义新怪物吗？新天赋？还是只能重新配置已有的 template？

### P2-4：SourceTier rarity bonus 缺乏数值理据

`NORMAL 0.00 / ELITE 0.15 / BOSS 0.40 / CHEST 0.10` 这组数值缺少设计推导过程。建议补充简要理据（如"BOSS 的 0.40 意味着 Level 10 Boss 掉落的物品 iLvl 等效于 Level 14 NORMAL 怪的掉落"）。

### P2-5：Verification Checklist 缺少 Terrain Interaction 的隔离验证

Checklist §2.3 定义了 Terrain Interaction Batch，但这些测试与 mapgen 测试耦合。建议增加不依赖 mapgen 的纯战斗场景地形交互验证（固定地图 + 固定地形标签 + 固定 seed 战斗）。

### P2-6：Phase 4 出口门禁与 Phase 5 入口的映射不清

Phase 4 §7 出口门禁有 4 条，但 Phase 5 的前置条件只说"Phase 4 出口全部满足"。建议在 Phase 4 出口门禁中显式映射到 Phase 5 的哪些系统将依赖这些产出。例如：

- "ProcGen 稳定" → Phase 5 的 soak/长局压力测试需要稳定的 mapgen
- "Content Pack 可装载" → Phase 5 的 localization QA 需要 content pack 支持

### P2-7：Content Pack 的版本兼容性策略未提及

Content pack 必须有版本号和兼容性策略。当 base game 从 v0.4.0 升级到 v0.4.1 时，旧 pack 是否仍然兼容？schema 版本号如何管理？

### P2-8：风险止损措施过于抽象

§8 的风险止损都是"如果 X 失败，回到 Y"，但没有具体的降级方案。例如"如果 ProcGen 差异只来自装饰层，必须回到拓扑元数据设计"——但降级后的 mapgen 具体长什么样？建议为每个风险提供具体的 fallback 设计。

### P2-9：缺少跨 Phase 的系统冻结时间线

Phase 4 §2.1 列出了 5 项必须冻结的系统，但没有说明每项在哪个 checkpoint（P4-A? P4-B? P4-C?）冻结。Phase 3 对此有明确的 checkpoint 划分。

### P2-10：Roadmap 文件过于简单

`docs/phase4/roadmap.md` 仅 17 行，只有主题描述和文档索引。相比 Phase 3 的 roadmap，缺少检查点定义、里程碑描述和进入/退出标准的摘要。

### P2-11：缺少 Phase 4 的资源/人力预算讨论

Phase 3 在 §3.3 中讨论了并行开发线与执行原则。Phase 4 应当对工作量有类似的预估指导，特别是 ProcGen（高技术风险）和 Content Pack（新平台能力）是否需要不同的人力分配策略。

### P2-12：LootBalanceLab 与 Phase 3 已有工具的关系未说明

Phase 3 是否已经建立了部分 balance 工具？如果有，Phase 4 的 `LootBalanceLab` 是扩展还是重写？这种继承关系应当在文档中明确。

---

## 4. 跨文档一致性检查

### 4.1 与核心系统详细设计文档的对齐

| 核心文档声明 | Phase 4 文档状态 | 问题 |
| --- | --- | --- |
| §8.5: P4-A 引入 LIGHTNING+WATER / FIRE+OIL / COLD+WATER 交互 | §4.3 提及但未引用核心文档的 InteractionRule | 应引用核心文档的规则 ID |
| §8.7: Phase 4 启用地形联动交互 | 无地形标签系统定义 | **P1-5** |
| §8.7: Phase 4 让 elite affix, artifact proc 接入元素交互 registry | Phase 4 未提及 elite affix 和 artifact proc 的元素交互接入 | 遗漏 |
| §12.6: Phase 4 不再增加新职业，只扩内容和 build 差异 | Phase 4 未声明此约束 | 应在非目标中明确 |
| §7.7: Phase 4 的 `castSpeed` 收益递减正式启用 | Phase 4 未提及 | 遗漏或应补入 P4 范围说明 |

### 4.2 与阶段补充文档的对齐

| 补充文档声明 | Phase 4 文档状态 | 问题 |
| --- | --- | --- |
| §4.5: Phase 4 引入元素亲和度系统（可选） | 完全未提及 | **P1-11** |
| §4.5: Phase 4 的 affix 定义 YAML 扩展元素词条 | 未出现具体 affix 定义 | 应补入 P4-W3 范围 |
| §4.5: mapgen 引入元素地形标签 | 未定义地形标签 schema | **P1-5** |
| §4.5: CombatPipeline step 9 接入地形交互检查 | 未说明集成方式 | **P1-6** |

---

## 5. 结构性建议

### 5.1 文档扩展到 Phase 3 同等深度

Phase 4 的主文档应当从 274 行扩展到 500~700 行，增加以下内容：

1. **每个子系统的 Kotlin 接口骨架**（参照 Phase 3 的做法）
2. **每个数据驱动系统的 YAML 示例**
3. **数学模型和公式**（特别是 Loot Budget）
4. **工作包依赖图和并行策略**
5. **与 Phase 3 已冻结系统的集成点说明**

### 5.2 Verification Checklist 量化升级

Phase 4 的验证清单应当从 78 行扩展到 120~150 行，增加：

1. 每项验证的量化门槛
2. 与 Phase 3 已有 golden seed / harness 的继承关系
3. 新增 harness 的具体输入/输出规格

### 5.3 补充 Roadmap 的检查点定义

`roadmap.md` 应当定义 Phase 4 的 A/B/C 检查点及其出口标准，与 Phase 3 保持一致的结构。

---

## 6. 总结

Phase 4 的设计意图是清晰的——把"可完成长局"推进到"高重复游玩价值长局"。5 个子系统的选择（ProcGen、Solvability、Loot Budget、Elite/Hidden、Content Pack）恰当地覆盖了这个目标。但当前文档的规格深度远不足以直接作为实现输入：

1. **4 个 P0 问题**均指向同一根因：核心子系统缺少数据结构、算法选型和数学模型
2. **14 个 P1 问题**中大部分可以在 P0 补齐后自然解决
3. 文档整体需要从"设计意向书"升级为"执行文档"

**建议**：在进入 P4-W1 代码实施前，完成所有 P0 的修复，并至少处理 P1-1 ~ P1-9 中的关键项。

---

*审阅人：资深游戏设计与开发总监视角*
*审阅版本：Round 1*
*下一步：等待文档修订后进行 R2 审阅*

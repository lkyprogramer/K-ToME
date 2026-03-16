# Phase 4 Director Review — Round 2

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`（909 行，R1 时 274 行）
2. `docs/phase4/2026-03-13-phase4-verification-checklist.md`（172 行，R1 时 78 行）
3. `docs/phase4/roadmap.md`（79 行，R1 时 17 行）
4. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（变更 ~564 行 diff）

**审阅焦点**：R1 修复确认 + 新发现。

---

## 0. R1 修复确认

### P0 修复状态

| R1 编号 | 问题 | 状态 | 说明 |
| --- | --- | --- | --- |
| P0-1 | ProcGen 无算法规格与数据结构 | ✅ 已修复 | §4.1 现有 6 步流水线、Kotlin 骨架（7 个类型）、环路约束、biome family 表、YAML 示例 |
| P0-2 | Loot Budget V2 无数学模型 | ✅ 已修复 | §4.3 现有 5 条正式公式、RarityTier/SourceTier/LootBudget 数据结构、affix cost 体系、YAML 示例 |
| P0-3 | Lock-Key DAG 无形式化模型 | ✅ 已修复 | §4.2 现有 PathClass/KeyType/DiscoveryRuleType 枚举、SolvabilityGraph/SolvabilityProof 结构、验证算法描述 |
| P0-4 | Content Pack Overlay 无 schema | ✅ 已修复 | §4.5 现有 ContentPackManifest（13 字段）、OverlayOp/OverlayEntry、冲突规则、i18n 集成、版本兼容策略、完整 YAML 示例 |

### P1 修复状态

| R1 编号 | 问题 | 状态 |
| --- | --- | --- |
| P1-1 | 全文无 Kotlin 骨架 | ✅ 5 个子系统均有骨架 |
| P1-2 | 无 YAML 示例 | ✅ vault / biome / hidden entrance / hidden event / affix / pack manifest 均有示例 |
| P1-3 | 精英突变空白 | ✅ §4.4.1 MutationKind + EliteMutationDef + 可读性合同 |
| P1-4 | 隐藏事件未定义 | ✅ §4.4.3 HiddenTriggerType + HiddenEventDef + YAML 示例 |
| P1-5 | 地形标签未定义 | ✅ §4.1.2 TerrainTag enum |
| P1-6 | Phase 3 战斗系统衔接缺失 | ✅ §4.3.5 和 §4.6 显式引用 CombatPipeline step 9 和 ElementInteractionRule |
| P1-7 | 工作包无依赖关系 | ✅ §3.3 + §5.2 有依赖表和并行策略 |
| P1-8 | Boss 变体关系未说明 | ✅ §4.4.2 BossVariantDef + baseEncounterId + 边界说明 |
| P1-9 | 验证清单无量化门槛 | ✅ Checklist 全面量化（500/1000/10000 seed、±5% 偏差等） |
| P1-10 | Secret zone 未定义 | ✅ §4.2.3 SecretZoneDef + 冻结口径 |
| P1-11 | 元素亲和度缺失 | ✅ §2.1 和 §3.2 显式标注为 optional lab |
| P1-12 | MagicFind 来源未定义 | ✅ §4.3.3 |
| P1-13 | Phase 4 zone 缺失 | ✅ §4.1.4 zone 升级表 |
| P1-14 | i18n/visual/audio 集成未定义 | ✅ §4.5.2 |

### P2 修复状态

| R1 编号 | 状态 | 说明 |
| --- | --- | --- |
| P2-1 并行 lane | ✅ | §3.3 四条 lane |
| P2-2 Golden seed | ✅ | §5.3 |
| P2-3 非目标精确化 | ✅ | §3.2 排除所有脚本引擎 |
| P2-4 SourceTier 理据 | ✅ | §4.3.1 解释段落 |
| P2-5 Terrain 隔离验证 | ✅ | Checklist §2.4 |
| P2-6 出口-入口映射 | ✅ | §7 |
| P2-7 Pack 版本兼容 | ✅ | §4.5.3 |
| P2-8 风险止损具体化 | ✅ | §8 |
| P2-9 检查点时间线 | ✅ | §2.1 + §5.1 |
| P2-10 Roadmap 扩展 | ✅ | 79 行含检查点/依赖/进出条件 |
| P2-11 人力建议 | ✅ | §3.4 |
| P2-12 LootBalanceLab 关系 | ✅ | §4.3.4 |

**R1 总计 30 项（4 P0 + 14 P1 + 12 P2）全部已确认修复。**

---

## 1. R2 新发现

### 新发现统计

| 等级 | 数量 |
| --- | --- |
| P0 | 0 |
| P1 | 5 |
| P2 | 10 |

---

### P1-R2-1：`TopologyGraph` 在 Phase 4 文档中被引用但从未定义字段

**位置**：§4.1.1、§4.1.2

**问题**：`TopologyGraph` 是 mapgen 流水线的核心中间表示——`TopologyPlanner.plan()` 的输出、`GeneratedFloor.topology` 的类型。但 Phase 4 文档没有定义它的字段结构。开发者无法判断 `TopologyGraph` 包含什么：节点列表？边列表？邻接表？环路标记？

同时，以下被 `GeneratedFloor` 引用的类型也未定义：

- `HiddenEntranceInstance`
- `VaultPlacement`
- `Point`（`terrainTags: Map<Point, Set<TerrainTag>>` 中使用）

**建议**：补齐 `TopologyGraph` 的最小字段定义。`HiddenEntranceInstance` / `VaultPlacement` / `Point` 可以简要标注（如"复用 `core.map.Point`"），但 `TopologyGraph` 作为流水线核心必须有结构。

---

### P1-R2-2：`ZoneMapgenProfile` 被引用但未定义

**位置**：§4.1.1

**问题**：`TopologyPlanner.plan(profile: ZoneMapgenProfile, seed: Long)` 中的 `ZoneMapgenProfile` 从未定义。这是每个 zone 的 mapgen 配置入口，决定了 zone 可以生成什么拓扑、多少环路、使用哪些 biome family、允许哪些 vault。

§4.1.4 有一张 zone 升级表，但那是人类可读的描述，不是 `ZoneMapgenProfile` 的数据结构。

**建议**：定义 `ZoneMapgenProfile` 数据结构，至少包含：`zoneId`、`allowedBiomeFamilies`、`loopCountRange`、`vaultPool`、`terrainTagWeights`、`roomTagFilter`。

---

### P1-R2-3：Loot 公式中 `zoneRarityBonus` 和 `zoneQualityBonus` 未定义

**位置**：§4.3.1 公式 3 和 4

**问题**：

- 公式 3：`rarityScore = sourceTier.rarityBonus + zoneRarityBonus + effectiveMagicFind * 0.50`
- 公式 4：`qLvl = clamp(iLvl + rarityTier.qualityBonus + zoneQualityBonus, iLvl, iLvl + 6)`

`zoneRarityBonus` 和 `zoneQualityBonus` 出现在公式中但从未定义。它们的来源是什么？每个 zone 配置？固定值？值域范围是什么？

**建议**：在 zone 升级表（§4.1.4）中补充每个 zone 的 `rarityBonus` 和 `qualityBonus`，或定义一个 `ZoneLootConfig` 数据结构。

---

### P1-R2-4：核心文档与 Phase 4 文档存在 5 处类型名不一致

**位置**：核心文档 §9.3.1 ~ §9.3.3 vs Phase 4 §4.1 ~ §4.5

**问题**：核心文档在更新时引入了简化版的 Phase 4 数据结构，但与 Phase 4 执行文档的类型名存在不一致：

| 概念 | 核心文档 | Phase 4 文档 | 差异 |
| --- | --- | --- | --- |
| 地图生成结果 | `MapgenResult` | `GeneratedFloor` | 不同类名，不同字段集 |
| 可解性图节点 | `nodes: List<String>` | `nodes: List<SolvabilityNode>` | 核心文档是原始字符串，Phase 4 是 typed |
| 可解性图边 | `edges: List<String>` | `edges: List<SolvabilityEdge>` | 同上 |
| 掉落上下文 | `LootGenerationContext` | `LootRollContext` | 不同类名 |
| overlay 操作枚举 | `OverlayMode` | `OverlayOp` | 不同枚举名 |

核心文档 §9.3.2 已声明"iLvl / qLvl / affixBudget 的正式计算以 Phase 4 文档为执行权威"，这是正确的权威链。但类型名不一致会在实现时造成混淆。

**建议**：两种修复路径（选一即可）：

1. 核心文档 §9.3 明确标注："本节骨架为设计锚点级草图，实现以 Phase 4 执行文档的完整类型为准"
2. 统一类型名（推荐），使核心文档的简化版使用与 Phase 4 相同的类名

---

### P1-R2-5：稀有度 roll 的选择算法未描述

**位置**：§4.3.1

**问题**：§4.3.1 定义了 `RarityTier` 的 `baseWeight` 和 `rarityScore` 对权重的修正倍率，但没有描述将修正后权重转化为最终稀有度选择的算法。具体来说：

1. 是加权随机（归一化权重 → 累积分布 → 一次 roll）？
2. 还是分级阈值（依次判断是否达到 ARTIFACT 阈值、UNIQUE 阈值...）？
3. 当 `UNIQUE` 或 `ARTIFACT` 被选中但 zone 没有对应模板时，是降级到 `RARE` 还是重新 roll？

**建议**：补充一段 2~3 行的选择算法描述。建议采用归一化加权随机，并说明无可用模板时的降级策略。

---

### P2-R2-1：`RoomDef.minSize` / `maxSize` 语义模糊

**位置**：§4.1.2

**问题**：`RoomDef` 定义了 `minSize: IntRange` 和 `maxSize: IntRange`，但 `IntRange` 表示什么？是 `width` 的范围？还是 `width × height` 的面积范围？如果是尺寸范围，为什么同时有 `minSize` 和 `maxSize` 两个 `IntRange`？

**建议**：改为明确的字段，如 `widthRange: IntRange` 和 `heightRange: IntRange`，或补充一行注释说明语义。

---

### P2-R2-2：`PatternRoomDef.patternId` 语义未解释

**位置**：§4.1.2

**问题**：`PatternRoomDef` 引用了 `patternId`，但"pattern"的含义从未解释。它是手绘的固定布局模板？还是生成规则？还是家具/陷阱的放置方案？§4.1.2 冻结口径 1 说"在标准房间边界内应用固定布局模式"，但这只是自然语言描述，`patternId` 指向什么格式的数据未明确。

**建议**：补充一句说明 `patternId` 指向的资源类型和格式（如"指向 `data/mapgen/patterns/{patternId}.yaml`，定义了房间内的 tile 覆盖和 entity 放置规则"）。

---

### P2-R2-3：`VaultDef.templateId` 同样缺少格式说明

**位置**：§4.1.2

**问题**：`VaultDef.templateId` 指向什么？手工制作的完整 tile 布局？还是参数化模板？vault 的实际地图内容如何存储？

**建议**：与 P2-R2-2 一并说明 vault 模板的存储格式。

---

### P2-R2-4：`GeneratedFloor` 缺少溯源字段

**位置**：§4.1.1

**问题**：`GeneratedFloor` 不包含 `seed`、`zoneId`、`floorIndex` 等字段。这些信息在 `MapgenRequest` 中，但生成结果不保留它们，在后续调试和 reproducibility 场景下需要额外传递上下文。

**建议**：在 `GeneratedFloor` 中增加 `seed: Long`、`zoneId: String`、`floorIndex: Int` 字段，或增加一个包装类型如 `MapgenOutput(request: MapgenRequest, floor: GeneratedFloor)`。

---

### P2-R2-5：`HiddenEventDef.conditions` 为 `List<String>` 过于宽松

**位置**：§4.4.3

**问题**：`HiddenEventDef.conditions` 是 `List<String>`，但没有定义有效的 condition 字符串格式。YAML 示例中使用了 `terrainTag: WATER` 和 `zoneId: underground_river` 这样的键值对，但 Kotlin 类型是纯 `String`。

与 Phase 3 的 AI DSL conditions（`self_hp_pct_lte`、`talent_ready` 等结构化条件）形成对比，hidden event 的 conditions 缺少类型安全。

**建议**：将 `conditions` 改为 typed 结构（如 `List<HiddenEventCondition>`），或至少定义允许的 condition key 枚举。

---

### P2-R2-6：`EliteMutationDef.statModifiers` 和 `grantedTalents` 为 `List<String>`

**位置**：§4.4.1

**问题**：`statModifiers: List<String>` 和 `grantedTalents: List<String>` 引用了外部 ID，但没有说明这些 ID 指向哪个 registry。`statModifiers` 是 `StatModifier` 的 ID？还是一种"stat + amount"的编码字符串？

同理，`BossVariantDef.grantedMutations: List<String>` 引用 mutation ID，但没有说明这些必须来自 `EliteMutationDef` registry 还是可以是 boss-specific 的。

**建议**：补充一句说明引用来源，如"引用 `data/elites/mutations.yaml` 中定义的 mutation ID"。

---

### P2-R2-7：Verification Checklist 未测试 magicFind 极端值

**位置**：Checklist §2.3

**问题**：Loot Balance Batch 测试组合包含 `magicFind=0.00, 0.15, 0.25, 0.10`，但未测试：

- `magicFind=0.0`（无加成，已覆盖）
- `magicFind=1.0`（上限，未覆盖）
- `magicFind > 1.0`（应被 clamp，需验证 clamp 行为）

**建议**：增加一组 `magicFind=1.0` 的测试，确认上限行为。可选增加 `magicFind=1.5` 验证 clamp。

---

### P2-R2-8：Content Pack `overlays` manifest 列表不支持目录自动发现

**位置**：§4.5.1

**问题**：`ContentPackManifest.overlays` 需要逐条列出每个 overlay entry。对于包含大量新怪物/物品的 pack，manifest 会变得冗长。是否支持目录级自动发现（如"扫描 `data/monsters/` 下所有 YAML 并全部作为 `ADD`"）？

**建议**：如果不支持，应在文档中明确说明"每个 overlay entry 必须显式列出，不支持目录自动发现"。如果后续可能支持，标注为 Phase 5 候选能力。

---

### P2-R2-9：Hidden content 触发率门槛缺少设计依据

**位置**：Checklist §1 条目 4

**问题**：hiddenContentHarness 要求"至少 30% 的 run 触发 1 个 hidden event"和"至少 10% 发现 1 个 secret zone"。这些阈值的设计依据是什么？如果每个 zone 只有 1 个 hidden event 和 1 个 secret zone，30% 和 10% 分别意味着什么 discovery rule 难度？

**建议**：补充一句设计理据，如"30% 确保平均每 3~4 局至少有一次隐藏发现体验，10% 确保 secret zone 具有稀缺感但不至于在长期游玩中完全不可见"。

---

### P2-R2-10：`SolvabilityEdge.discoveryRule` 为 null 时的语义未说明

**位置**：§4.2.1

**问题**：`SolvabilityEdge` 的 `discoveryRule: DiscoveryRule?` 是可空的，但文档没有说明 null 的含义。合理推断是"该边无需发现条件，默认可达"，但应当显式声明。

**建议**：在冻结定义中增加一句"`discoveryRule` 为 null 表示该边默认可通行，不需要任何发现条件"。

---

## 2. 跨文档一致性检查

### 2.1 核心文档更新评估

核心文档的 Phase 4 相关更新（§9.3.1 ~ §9.3.3）整体方向正确：

1. ✅ 引入了 `TerrainTag`、`MapgenRequest`、`MapgenPipeline`、`SolvabilityGraph` 骨架
2. ✅ 引入了 `SourceTier`（含 `itemLevelBonus` / `affixBudgetBonus` 扩展字段）和 `LootBudget`
3. ✅ 引入了 `ContentPackManifest` 和 `OverlayMode`
4. ✅ 正确声明了"iLvl / qLvl / affixBudget 的正式计算以 Phase 4 文档为执行权威"
5. ✅ 补充了 `castSpeed` 收益递减、元素亲和度 optional lab 等约束

但存在 **P1-R2-4** 中列出的 5 处类型名不一致。

### 2.2 核心文档其他更新

以下非 Phase 4 的更新也已检查，均为正向改进：

| 变更 | 评估 |
| --- | --- |
| `CallbackResult` 从 `CONTINUE/CONSUMED/CANCEL_ACTION` 改为 `CONTINUE/CANCEL/ABSORB` | ✅ 与 Phase 3 对齐，语义更清晰 |
| `ClassDef` → `ProfessionDef`，`ClassTier` → `ProfessionTier` | ✅ 术语统一 |
| `startingEquipment` → `startingKit` | ✅ 与路线图对齐 |
| `RequireClassCleared` → `RequireProfessionCleared` | ✅ 跟随上述重命名 |
| `ResourceType` 增加 HP 注释 | ✅ 明确边界 |
| Boss schema `behaviorScript` → `aiProfileId`，`delay` → `previewTurns` | ✅ 与 Phase 3 BossEncounterDef 对齐 |
| `ProfileData` / `RunSummary` 新增 | ✅ 局间持久化有正式 schema |
| 洗点规则更新为"Phase 3 默认免费，Phase 4+ 可引入限制" | ✅ 合理的阶段性演进 |
| 世界分支树调整（盗贼营地位置、灰门王座合并） | ✅ 结构更清晰 |
| AI 脚本 Boss 行为不确定性约束（§7.3 第 6 条） | ✅ 重要的 trace 完整性保障 |

### 2.3 与 Phase 3/5 文档的衔接

| 检查点 | 状态 |
| --- | --- |
| Phase 4 入口正确引用 Phase 3 出口 | ✅ |
| Phase 4 出口显式映射到 Phase 5 输入 | ✅ §7 |
| Phase 4 不重复 Phase 3 已冻结的系统定义 | ✅ 只引用不重写 |
| Phase 4 不提前做 Phase 5 的工作（tactical AI 等） | ✅ §3.2 非目标清晰 |

---

## 3. R2 总结

Phase 4 文档体系经过 R1 → R2 的修订，已从"设计意向书"实质性升级为可驱动实现的执行文档。核心改进：

1. **文档量从 274 行增长到 909 行**（+232%），达到了 Phase 3 同等级别的规格深度
2. **4 个 P0 阻塞问题全部修复**，每个子系统现在都有 Kotlin 骨架、数据模型和 YAML 示例
3. **验证清单从 78 行扩展到 172 行**，全部量化
4. **Roadmap 从 17 行扩展到 79 行**，有完整的检查点、依赖和进出条件
5. **核心文档同步更新**，建立了正确的权威链

R2 发现的 5 个 P1 和 10 个 P2 主要集中在：

- **未定义的中间类型**（`TopologyGraph`、`ZoneMapgenProfile`）—— P1 级
- **跨文档类型名不一致**（`MapgenResult` vs `GeneratedFloor` 等 5 处）—— P1 级
- **公式中的未定义变量**（`zoneRarityBonus`、`zoneQualityBonus`）—— P1 级
- **类型精度不足**（`List<String>` 应为 typed 结构）—— P2 级
- **边缘测试覆盖**（magicFind 极端值）—— P2 级

**放行建议**：R2 无 P0 问题。P1-R2-1 ~ P1-R2-3（未定义类型和变量）应在进入 P4-W1 代码实现前修复；P1-R2-4 ~ P1-R2-5 可在 P4-A checkpoint 前修复。P2 问题不阻塞实现起步，可在 W1/W2 实现过程中渐进处理。

---

*审阅人：资深游戏设计与开发总监视角*
*审阅版本：Round 2*
*下一步：待 P1 修复后进行 R3 确认审阅*

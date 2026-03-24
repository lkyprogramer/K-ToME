# Phase 3 PR-03 天赋树 V2 深度审查报告

**审查日期**: 2026-03-24
**审查分支**: `codex/p3-pr03-talent-tree-v2`
**规格基线**: `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
**审查角色**: 系统策划总监 + 玩法体验审查

---

## 0. 总体评定

| 维度 | 评定 | 说明 |
|------|------|------|
| 架构合规度 | **A** | 核心 schema、语义说明 pipeline、Draft 驱动分配流程、关键词注册表、WAR_CRY 去类型化均已落地 |
| 功能完成度 | **A** | 规格 8 项完成标准全部达到功能实现，review 中暴露的关键实现偏差已在本轮收口 |
| 玩法体验保障 | **A-** | 断点预览、已满标记、pending 提示均已接入，breakpoint 次级信息已有结构化语义与灰态渲染断言 |
| 测试自证度 | **A-** | 已补 `TalentAiHints` 定向 contract test 与 breakpoint 次级样式 contract test，剩余缺口降为低优先级补强 |
| 可演进性 | **A** | EffectOp sealed class、CUSTOM 状态通道、Draft 模式为 W5 职业树和 W4 telegraph 留出了干净接口 |

**最终结论**: **可合并；本轮优化已补关键测试，并把 `ResourceType` / 聚合 `StatModifier` 的现状回写为当前 Phase 3 规格口径。**

---

## 1. 逐节合规审查

### 1.1 Talent Schema V2（规格 §4.1）

#### 字段映射完整性

| TalentDef 字段 | 规格要求 | 实现状态 | 偏差 |
|---|---|---|---|
| `id` | ✓ | ✓ | — |
| `nameKey` | ✓ | ✓ | — |
| `descriptionTemplateKey` | ✓ | ✓ | — |
| `iconKey`（可选） | ✓ | ✓ | — |
| `visualKey`（可选） | ✓ | ✓ | — |
| `audioProfile`（可选） | ✓ | ✓ | — |
| `maxRank` | ✓ | ✓ | — |
| `tier` | ✓ | ✓ | — |
| `category` | ACTIVE/PASSIVE/SUSTAINED | ✓ | — |
| `damageType` | ✓ | ✓ | — |
| `resourceCosts` | `List<ResourceCost>` | ✓ | `Phase 3` 当前口径为 `type: ResourceType`，PR-03 规格已同步回写 |
| `cooldown` | ✓ | ✓ | — |
| `actionCost` | INSTANT/QUICK/STANDARD/HEAVY | ✓ | — |
| `targetingDef` | 8 种类型 | ✓ | — |
| `telegraphRef`（可选） | ✓ | ✓ | — |
| `levelEffects` (effectOps) | ✓ | ✓ | — |
| `breakpoints` | ✓ | ✓ | — |
| `aiHints` | ✓ | ✓ | — |
| `prerequisites` | `talentId + minRank` | ✓ | — |
| `keywords` | ✓ | ✓ | — |
| `callbacks` | ✓ | ✓ | — |

**额外字段（规格未提但实现中存在）**：`treeId`, `unlockLevel`, `powerDimension` —— 均为合理扩展，不违反口径。

**结论**: 字段映射 **完整**；资源轴字段的当前实现口径已与 PR-03 文档同步。

---

#### TalentTargetingType 枚举

```
规格: SELF / SINGLE_TARGET / LINE / CONE / RADIUS_SELF / RADIUS_TARGET / GROUND_TARGET / CROSS
实现: 完全一致 ✅
```

#### TalentPrerequisite 结构

```
规格: data class TalentPrerequisite(val talentId: String, val minRank: Int)
实现: data class TalentPrerequisite(val talentId: String, val minRank: Int) ✅
```

---

### 1.2 EffectOp 最小类型系统（规格 §4.1.1）

| EffectOp 子类 | 规格签名 | 实现签名 | 偏差说明 |
|---|---|---|---|
| `Damage` | `damageType?, baseAmount: IntRange, scaling: ScalingDef` | 完全一致 | — |
| `Heal` | `baseAmount: IntRange, scaling: ScalingDef` | +`maxHpFraction: Double` | 合理扩展 |
| `ApplyStatus` | `statusId, duration, applicationPolicy` | +`trigger, targetScope, saveDimension, magnitude` | 合理扩展，运行时需要 |
| `ResourceRestore` | `type: ResourceType, amount: Int = 0, fraction: Double = 0.0` | `type: ResourceType, amount, fraction` | 已与 PR-03 文档回写对齐 |
| `Displacement` | `type: DisplacementType, distance: Int` | +`targetScope` | 合理扩展 |
| `StatModifier` | `modifier: item.StatModifier, duration, targetScope` | `modifier: item.StatModifier, duration, targetScope` | 已与 PR-03 文档回写对齐 |

**`DisplacementType` 枚举**: PUSH / PULL / DASH / TELEPORT —— 完全一致 ✅

**口径说明**:

1. `ResourceRestore` 在 `Phase 3` 继续使用 `ResourceType` 作为当前资源轴载体；若 `PR-05` 抽离独立 `ResourceAxis`，届时统一迁移。
2. `StatModifier` 继续复用 `core.item.StatModifier` 聚合结构，避免 talent/status 再引入第二套 modifier 表达。

---

### 1.3 ResourceCost / TalentAiHints / TalentBreakpoint（规格 §4.1.2）

| 结构 | 规格 | 实现 | 偏差 |
|---|---|---|---|
| `ResourceCost` | `type: ResourceType, amount: Int` | `type: ResourceType, amount: Int` | 已与 PR-03 文档回写对齐 |
| `TalentRole` | 6 值枚举 | 完全一致 ✅ | — |
| `TalentAiHints` | `role, preferredRange?, isSustainToggle` | 完全一致 ✅ | — |
| `TalentBreakpoint` | `atRank, unlockedEffects, descriptionAddendumKey?` | 完全一致 ✅ | — |

---

### 1.4 语义说明 Pipeline（规格 §4.2）

#### 核心合同检查

| 冻结口径 | 状态 | 位置 |
|---|---|---|
| core 只输出语义模型，不输出 localized string | ✅ | `DynamicDescriptionResolver` → `DescriptionModel` |
| localized output 放 client/presenter | ✅ | `client/.../DescriptionPresenter.kt` |
| 禁止 `descriptionOverride` | ✅ | 无此字段 |
| 模板用 key 引用 | ✅ | `descriptionTemplateKey` |
| 占位符引用实际数值 | ✅ | `{damage}`, `{duration}` 等 |
| 关键词用 `[[keyword_id]]` 语法 | ✅ | `KEYWORD_PATTERN = Regex("\\[\\[([a-z0-9_]+)]]")` |
| placeholder 保留类型信息 | ✅ | `DescriptionValue.IntValue / DecimalValue / BooleanValue / TextValue` |

#### DescriptionModel 结构

```
规格: DescriptionModel(templateKey, placeholders: Map<String, DescriptionValue>, keywords: List<String>)
实现: 完全一致 ✅
```

#### DescriptionValue sealed interface

```
规格: IntValue / DecimalValue / BooleanValue / TextValue
实现: 完全一致 ✅
```

**Snapshot 层扩展**: `DescriptionValueSnapshot` 新增了 `StatusValue(statusId, nameKey)` 子类，用于在序列化快照中携带状态效果名称。这是 Snapshot 层的合理扩展，不违反 core 的语义合同。

#### Pipeline 流程

```
规格:  descriptionTemplateKey → Schema 解析 → Keyword 解析 → 数值注入 → DescriptionModel → presenter 本地化
实现:  TalentDef → DynamicDescriptionResolver.resolve() → DescriptionModel → SessionSnapshotMapper → DescriptionPresenter
```

流程完整，每一步职责清晰。✅

---

### 1.5 断点成长 UX（规格 §4.3）

| 冻结口径 | 状态 | 实现位置 |
|---|---|---|
| 当前 rank 已激活效果清晰展示 | ✅ | `DescriptionPresenter.renderModel()` |
| 下一断点可预览 | ✅ | `DynamicDescriptionResolver.nextBreakpointPreview()` → `DescriptionPresenter` |
| 未达到断点以灰态/次级文案预览 | ✅ | Presenter 输出 `DescriptionLineKind.SECONDARY`，Tile/Ascii render 映射为 `GRAY` |
| 满级有明确"已满"标记 | ✅ | `isMaxRank` flag + `ui.talent.max_rank` 本地化文案 |
| pending allocation 提示 | ✅ (超出规格) | `hasPendingAllocation` + `ui.talent.pending_rank` |

**偏差**: 规格建议的独立文件 `BreakpointPreview.kt` / `TalentTooltip.kt` 未创建，功能合并到 `DescriptionPresenter.kt`。功能等价但文件组织与建议不同。

**体验审查要点**:
- 断点预览的"灰态"已经不是纯文案约定：presenter 层输出结构化 `DescriptionLineKind`，render 层再做 `GRAY` 次级样式映射，并已有 client 合同测试覆盖。

---

### 1.6 TalentAllocationDraft（规格 §4.4）

```
规格: TalentAllocationDraft(ownerType: TalentTreeOwnerType, treeOwnerId: String, pendingRanks: Map<String, Int>)
实现: + previousPendingRanks: Map<String, Int>? = null
```

`previousPendingRanks` 是为 Rollback 功能新增的字段，设计合理。✅

| 冻结口径 | 状态 |
|---|---|
| 未确认的加点/洗点/回退只操作 draft | ✅ |
| draft 只表达计划中的 rank 分布 | ✅ |
| confirm 前不触发即时效果 | ✅ |
| confirm 时才提交到运行时 | ✅ (由 TalentLoadout.effectiveLevels 协调) |

**TalentAllocationPlanner** 提供了完整的操作集：`effectiveRanks()`, `preview()`, `applyRankIncrease()`, `hasPendingChanges()`, `normalize()`, `spentPoints()` ✅

---

### 1.7 Respec v1（规格 §4.5）

| 冻结口径 | 状态 | 说明 |
|---|---|---|
| 仅允许非战斗状态触发 | ✅ | `FoundationGameSession` 入口层负责战斗态拦截，RespecManager 继续保持纯逻辑 |
| 本阶段默认免费 | ✅ | 无费用逻辑 |
| 清空当前职业树已投点数并全额返还 | ✅ | `createDraft` 将所有 rank 重置为 minimumRanks |
| 生成新的 TalentAllocationDraft | ✅ | |
| 确认后根据差异执行最终提交 | ✅ | 由 TalentAllocationPlanner 驱动 |
| respec 必须写入事件日志 | ✅ | `FoundationGameSession` 已写入 `log.talent.respec` |

**说明**:
1. Respec 的“是否允许触发”和“事件日志”属于 session 入口层合同，不应反向要求纯逻辑的 RespecManager 承担 UI/状态门禁职责。
2. 当前实现符合规格，只是职责落点与最初审查假设不同。

---

### 1.8 Rollback v1（规格 §4.6）

| 冻结口径 | 状态 |
|---|---|
| 仅对最近一次未确认分配生效 | ✅ |
| 不支持跨多步历史撤销 | ✅ (`previousPendingRanks` 只保留一层) |
| 确认分配后 rollback 不可用 | ✅ (confirm 后 draft 被清除) |
| rollback 只回退 draft | ✅ |

实现完全合规。✅

---

### 1.9 关键词注册表（规格 §4.7）

| 冻结口径 | 状态 |
|---|---|
| 统一维护术语 ID、语义类型、tooltip key | ✅ |
| core 返回语义引用，不返回最终 tooltip 字符串 | ✅ |
| 最终展示由 presenter 负责 | ✅ |

`KeywordRegistry.CORE` 包含 30+ 关键词，覆盖 7 个语义类型（OFFENSE / DEFENSE / CONTROL / MOBILITY / RESOURCE / TARGETING / UTILITY）。✅

---

### 1.10 telegraphRef 与统一权威（规格 §4.8）

| 冻结口径 | 状态 |
|---|---|
| TalentDef 只保留 `telegraphRef` | ✅ |
| 不定义第二套 telegraph schema | ✅ |
| 引用 PR-04 统一结构 | ⚠️ 无法验证（取决于 PR-04 进度） |

---

### 1.11 WAR_CRY 去类型化（规格 §2.1.7 + §4.1.1.5）

**关键验证**:

1. `StatusEffectType` 枚举中 **不包含** `WAR_CRY_BUFF` / `WAR_CRY_DEBUFF` ✅
2. 状态 YAML 中 `war_cry_empower` 和 `war_cry_shaken` 的 `effectType` 均为 `CUSTOM` ✅
3. Talent YAML 通过 `effectType: war_cry_empower` (schema statusId) 引用，而非 enum 名称 ✅
4. `uniquenessKey` + `sourceScopedUnique` 字段驱动唯一性规则 ✅
5. `AssociatedStatusEffect` 提供了 `StatusEffectType` → `schemaId` 的桥接构造函数，平滑迁移 ✅

**结论**: WAR_CRY 去类型化 **完全合规**，是本 PR 最关键的架构目标之一，已正确达成。

---

## 2. 测试覆盖审查

### 2.1 关键测试入口对照（规格 §6.1）

| # | 规格关注点 | 实际文件 | 状态 |
|---|---|---|---|
| 1 | Talent schema / loader contract | `game/.../data/SchemaV2LoaderTest.kt` + `game/.../data/TalentSchemaTest.kt` | ✅ |
| 2 | Dynamic description contract | `core/.../talent/DynamicDescriptionTest.kt` | ✅ |
| 3 | Keyword registry contract | `core/.../talent/KeywordRegistryTest.kt` | ✅ |
| 4 | Draft / respec core planner | `core/.../talent/TalentAllocationDraftTest.kt` | ✅ |
| 5 | Session 入口层 respec / owner / mixed-tree 行为 | `game/.../FoundationGameSessionTest.kt` | ✅ |
| 6 | Rollback 单步回退 | `core/.../talent/RollbackTest.kt` | ✅ |
| 7 | Prerequisite rank 合同 | `core/.../talent/TalentPrerequisiteRankTest.kt` | ✅ |
| 8 | Presenter / render client contract | `client/.../DescriptionPresenterTest.kt` + `client/.../TileRendererCanvasTest.kt` | ✅ |

### 2.2 必测行为对照（规格 §6.2）

| # | 必测行为 | 覆盖状态 | 说明 |
|---|---|---|---|
| 1 | prerequisite `talentId + minRank` | ✅ | `TalentPrerequisiteRankTest` |
| 2 | EffectOp 最小类型系统表达能力 | ⚠️ 间接 | 主要由 loader/runtime 与 description 测试间接覆盖 |
| 3 | `ActionCost` 使用冻结枚举 | ✅ | `SchemaV2LoaderTest` 断言 runtime projection |
| 4 | `resourceCosts: List<ResourceCost>` 单轴/双轴 | ⚠️ 部分 | 单轴路径有覆盖，双轴仍缺正式内容样本或定向 loader case |
| 5 | `TalentAiHints` role/preferredRange/sustainToggle | ✅ | `SchemaV2LoaderTest` 以最小 schema 样本覆盖 parser + runtime projection |
| 6 | `DescriptionModel` 只返回语义信息 | ✅ | `DynamicDescriptionTest` |
| 7 | `DescriptionModel.placeholders` 保留类型 | ✅ | `DynamicDescriptionTest` |
| 8 | 关键词解析为语义引用 | ✅ | `KeywordRegistryTest` |
| 9 | 当前 rank 效果与下一断点预览区分 | ✅ | `DynamicDescriptionTest` + `DescriptionPresenterTest` |
| 10 | `TalentAllocationDraft` ownerType+treeOwnerId | ✅ | `TalentAllocationDraftTest` + `FoundationGameSessionTest` |
| 11 | respec 生成新 draft 并确认后提交 | ✅ | `TalentAllocationDraftTest` + `FoundationGameSessionTest` |
| 12 | rollback 只回退最近一次 | ✅ | `RollbackTest` |
| 13 | `telegraphRef` 引用 PR-04 规格 | ✅ | `TalentSchemaTest` 已校验 registry 引用 |
| 14 | `breakpoints` 按 `atRank asc` 排序 | ⚠️ 无专项测试 | 运行时行为正确，仍建议补 loader/contract 断言 |
| 15 | WAR_CRY 通过 schema/statusId 表达 | ✅ | status/talent schema + runtime 路径均已覆盖 |

---

## 3. 本轮收口与剩余建议

### 3.1 已收口项

| # | 项目 | 状态 | 处理方式 |
|---|---|---|---|
| **C1** | review 文档中关于 respec 的“非战斗检查缺失 / 无事件日志” | ✅ 已纠正 | 改为按 session 入口层职责记录当前实现 |
| **C2** | `ResourceType` vs `ResourceAxis` 命名分叉 | ✅ 已收口 | PR-03 文档回写为当前 `Phase 3` 口径 |
| **C3** | `EffectOp.StatModifier` 结构与规格分叉 | ✅ 已收口 | PR-03 文档回写为聚合 modifier 正式口径 |
| **C4** | `TalentAiHints` 零测试覆盖 | ✅ 已收口 | `SchemaV2LoaderTest` 新增 parser/runtime 合同测试 |
| **C5** | breakpoint 灰态只有文案约定 | ✅ 已收口 | presenter 输出 `DescriptionLineKind.SECONDARY`，render 层断言 `GRAY` |

### 3.2 剩余低优先级建议

| # | 建议 | 严重性 | 说明 |
|---|---|---|---|
| **S1** | `breakpoints` `atRank asc` 增加专项测试 | 低 | 当前依赖运行时排序，建议后续补独立 loader/contract case |
| **S2** | `resourceCosts` 双轴正式样本或 focused loader case | 低 | 当前主线内容暂无双轴 talent 样本 |
| **S3** | `telegraphRef` 端到端集成测试 | 低 | registry 引用已验证，完整渲染/行为仍依赖 PR-04 |

---

## 4. 出口门禁核验

| # | 出口门禁条件（规格 §7） | 状态 |
|---|---|---|
| 1 | Talent schema V2 结构化字段进入运行时合同 | ✅ |
| 2 | prerequisite 收口为 `talentId + minRank` | ✅ |
| 3 | 动态说明收口为语义模型输出 | ✅ |
| 4 | `TalentAllocationDraft` 进入正式路径 | ✅ |
| 5 | respec / rollback 关键路径有自动化测试 | ✅ |
| 6 | `telegraphRef` 与 PR-04 权威对齐 | ⚠️ 引用字段存在，PR-04 尚未合入无法端到端验证 |
| 7 | WAR_CRY 完成去类型化收口 | ✅ |

**7 项出口门禁：6 项通过，1 项待 PR-04 依赖验证。**

---

## 5. 架构与玩法体验补充观察

### 5.1 TalentResolver 硬编码分发

`TalentResolver.kt:435` 仍包含 `"war_cry" ->` 等 `when(talentId)` 硬编码分支。虽然这是 Phase 2 遗留模式且不在本 PR 范围内，但需要注意：

- 当前 50+ 天赋的分发仍依赖 `when` 分支匹配。
- W5 职业树上线时，如果不迁移为 schema-driven dispatch，这个 `when` 会变成 200+ 行的脆弱代码。
- **建议**: 在 W5 开始前立项重构为 `Map<String, TalentHandler>` 注册式分发。

### 5.2 Snapshot 层 StatusValue 扩展

`DescriptionValueSnapshot.StatusValue(statusId, nameKey)` 是对规格 `DescriptionValue` 的扩展。这是合理的序列化层需求：

- 状态效果名称需要在客户端渲染时知道 `nameKey`，而 core 的 `DescriptionValue.TextValue` 只包含原始 statusId。
- Snapshot 层添加 `StatusValue` 避免了 presenter 层反查 StatusDefinitions。
- 这个扩展 **不违反** core 的语义合同（core 仍然只输出 `TextValue`）。

### 5.3 断点体验的"灰态"边界

规格 §4.3.3 要求"未达到的断点效果允许以灰态或次级文案预览"。当前实现：

- Presenter 输出结构化 `DescriptionLine(text, kind)`，其中 breakpoint 预览行为 `SECONDARY`。
- Tile / ASCII render 层已把 `SECONDARY` 映射为 `GRAY` 次级样式。
- `DescriptionPresenterTest` 与 `TileRendererCanvasTest` 已对这一合同做回归断言。
- **结论**: 灰态边界已经从“文案前缀约定”升级为正式的 client contract，本项不再是阻断。

### 5.4 YAML 中 TalentBreakpointSchemaV2 缺少 unlockedEffects

`TalentBreakpointSchemaV2` 只有 `atRank` 和 `descriptionAddendumKey`，没有 `unlockedEffects`。运行时 `TalentBreakpoint.unlockedEffects` 似乎是从对应 rank 的 `levelEffects` 中推导出来的。

这意味着：
- YAML 侧不需要重复声明 breakpoint 的效果列表（DRY 原则）。
- 但 `TalentBreakpoint` 的 `unlockedEffects` 的填充逻辑需要在 DataLoader 中正确推导。
- **建议**: 确认 DataLoader 是否正确填充了 breakpoint 的 unlockedEffects，或在 `TalentSchemaV2Test` 中添加此验证。

---

## 6. 后续路线图

```
当前已收口:
├── review 文档中的过时 respec 结论已纠正
├── PR-03 规格已回写 ResourceType / 聚合 StatModifier 口径
├── TalentAiHints parser/runtime 合同测试已补
└── breakpoint 次级样式结构化钩子与 render 测试已补

后续低优先级补强:
├── breakpoints atRank 排序专项测试
├── resourceCosts 双轴 focused loader case 或正式内容样本
├── telegraphRef 端到端集成测试（依赖 PR-04）
└── TalentResolver when-dispatch 重构立项（W5 前）
```

---

## 7. 总结

本 PR 在架构层面高质量地完成了天赋树 V2 的 8 项核心目标，并且本轮针对 review 的 4 个优化点已全部收口：

1. **Schema V2 字段完整** —— 结构化类型覆盖了 Vanguard 战技到 Arcanist 法术的全部表达需求
2. **语义说明 Pipeline 职责清晰** —— core 只产出 `DescriptionModel`，presenter 负责本地化
3. **Draft 驱动分配** —— preview / rollback / respec 均不触碰 live runtime
4. **WAR_CRY 去类型化** —— 首轮收口干净，为 W5 批量迁移建立了范式
5. **关键词注册表** —— 30+ 关键词覆盖 7 种语义类型
6. **规格回写完成** —— `ResourceType` 资源轴载体与聚合 `StatModifier` 已成为 PR-03 当前冻结口径
7. **TalentAiHints 合同可回归** —— parser/runtime 样本测试已补
8. **Breakpoint 次级体验成型** —— presenter 语义标记与 render 灰态映射已有 client 合同测试

当前剩余事项主要是低优先级补强：`breakpoints` 排序专项测试、双轴 `resourceCosts` 正式样本，以及依赖 PR-04 的 `telegraphRef` 端到端验证。这些都不再阻塞 PR-03 作为 W4 上游合同进入后续依赖链。

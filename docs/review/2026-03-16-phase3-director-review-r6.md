# Phase 3 Director Review — Round 6

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`（679 行）
2. `docs/phase3/2026-03-13-phase3-verification-checklist.md`（123 行）
3. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（核心权威文档）

**审阅焦点**：R5 修复确认后的终极微观扫描——字段命名精确性、数值格式一致性、文档间遗漏的最小缝隙。

---

## 0. R5 修复确认

| R5 编号 | 问题 | 状态 |
| --- | --- | --- |
| P1-1 | 核心 §7.4 Boss schema 改用 `aiProfileId` + `weight` 两层结构 | ✅ §7.4.1 line 2634 + §7.4.2 line 2676 已对齐 Phase 3 |
| P2-1 | Boss 预算 3~4 | ✅ 核心 §7.6 line 2773 改为 "3~4 个" |
| P2-2 | Telegraph 阈值 ">=" | ✅ 核心 §6.1.3 line 2346 改为"大于等于" |
| P2-3 | WAR_CRY_BUFF 唯一效果 | ✅ Phase 3 §4.2 line 180-181 + Checklist §2.4 line 77 |
| P2-4 | Boss YAML 改用 `molten_giant` | ✅ Phase 3 §4.5 line 347 |
| P2-5 | ProfileData / RunSummary schema | ✅ 核心 §9.2.2 lines 3145-3170 + Phase 3 §4.4 line 277 引用闭环 |
| P2-6 | WAR_CRY_BUFF 验证项进 checklist | ✅ Checklist §2.4 line 77 |
| P2-7 | 怪物预算基线引用 | ✅ Phase 3 §4.6 lines 507-511 |

**结论：R5 的 1 P1 + 7 P2 全部已确认修复。**

附加确认：核心 §7.4 开头新增权威声明（line 2619）——"若本节示例与 `docs/phase3/*` 的冻结 schema 存在细节差异，一律以 Phase 3 执行文档 4.5 为权威"——正确建立了权威链。

---

## 1. R6 新发现

### 严重程度分布

| 等级 | 数量 |
| --- | --- |
| P0 | 0 |
| P1 | 0 |
| P2 | 5 |

---

### P2-1 Weight 数值格式不一致：Phase 3 用整数，核心用浮点

**位置**：Phase 3 §4.5 lines 374/381/391 vs 核心 §7.4.2 lines 2690/2702/2709/2715

Phase 3 YAML 示例：

```yaml
weight: 30   # 整数
weight: 70   # 整数
weight: 80   # 整数
```

核心文档 §7.4.2 YAML 示例：

```yaml
weight: 0.50   # 浮点，四个 action 合计 1.00
weight: 0.30
weight: 0.15
weight: 0.05
```

**影响**：

- 整数格式（Phase 3）天然适配"先过滤后加权"的选择模式——过滤后直接按剩余 weight 求和归一化。
- 浮点格式（核心文档）暗示预归一化概率，过滤掉某些 action 后需要重新归一化，否则选择概率和不为 1。
- 虽然核心 §7.4 已声明"差异以 Phase 3 为准"，但实现者若先读核心文档的示例，可能直接用浮点格式来写后续的 Boss AIProfile YAML 数据。

**建议操作**：将核心 §7.4.2 的 weight 改为整数格式，与 Phase 3 统一。或在核心 §7.4.2 注释说明"weight 为相对权重，运行时按候选集合求和归一化"。

### P2-2 Phase 3 §4.5 内部字段命名不一致：`hpPercent` vs `hpThreshold/hpEnd`

**位置**：Phase 3 §4.5 line 337 vs line 397

同一节内：

- Line 337 正确列出了 YAML 字段名："`hpThreshold / hpEnd / requiredStatus / turnCount`"
- Line 397 的 `BossPhaseDef` 需求清单却写了 "`hpPercent`"——该名称从未出现在任何 YAML schema 中

**影响**：实现者读到 line 397 可能以为需要新建一个 `hpPercent` 字段，实际应沿用 `hpThreshold/hpEnd`。

**建议操作**：Line 397 的 `hpPercent` 改为 `hpThreshold / hpEnd`，与 line 337 和 YAML 示例对齐。

### P2-3 Checklist §2.4 缺少 OVERCHARGE 的 LIGHTNING 消耗触发验证

**位置**：Checklist §2.4 line 74

OVERCHARGE 出现在"刷新语义"验证列表中，但它的关键行为不仅是刷新——还包括"下一次成功承受 `LIGHTNING` 伤害后被消耗"（Phase 3 §4.2 line 192 / 核心 §8.1.3 line 2852）。当前 checklist 只测了刷新，没有测消耗触发。

**建议操作**：在 §2.4 追加一条：

- "`OVERCHARGE` 在承受 `LIGHTNING` 伤害后被消耗，非 `LIGHTNING` 伤害不触发消耗"

### P2-4 Phase 3 §6.4 白盒要求与 Checklist §3 不完全同步

**位置**：Phase 3 §6.4 lines 644 vs Checklist §3

Phase 3 §6.4 item 3 要求"用 `Vanguard`、`Arcanist`、`Rogue`、`Templar` **各完成一次长局**"，但 Checklist §3 没有等价要求——只有 §3.1（1 场 Boss 战）和 §3.3（至少 1 个进阶职业走完长局路线）。

反方向也有缺口：Checklist §3 包含 i18n 术语检查（§3.4）、断点成长 UI（§3.5）、STEALTH 场景（§3.6）、TAUNT 场景（§3.7），这些在 §6.4 中没有对应。

两份清单互为补充但不是子集关系，执行时可能遗漏某一侧独有的检查项。

**建议操作**（任选其一）：
- 方案 A：让 Checklist §3 成为白盒验证的**唯一执行清单**，将 §6.4 中 Checklist 未覆盖的"4 基础职业各完成一次长局"合并进 Checklist §3
- 方案 B：在 §6.4 末尾加一句"完整白盒验证步骤以 `phase3-verification-checklist.md §3` 为权威，本节仅列出最低验证意图"

### P2-5 §4.1 "首批元素交互"列表漏标 LIGHTNING 的排除理由

**位置**：Phase 3 §4.1 line 100

"首批元素交互只启用 `FIRE/COLD/HOLY/SHADOW` 的核心规则"——列出了 5 个元素伤害通道中的 4 个，唯独 `LIGHTNING` 被省略，无解释。

LIGHTNING 的 OVERCHARGE 效果在 §4.2 作为状态规则处理，不是元素交互规则。但读到 §4.1 的人可能误以为 LIGHTNING 在 Phase 3 不参与任何元素相关机制。

**建议操作**：在 line 100 末尾追加简短说明，例如："；`LIGHTNING` 在本阶段不参与跨元素交互，其关联效果 `OVERCHARGE` 作为状态规则在 `§4.2` 处理。"

---

## 2. 全量对齐最终矩阵

| 检查项 | 结果 | 备注 |
| --- | --- | --- |
| Zone 拓扑（11 个节点） | ✅ | 两份文档完全一致 |
| Zone 等级范围 | ✅ | 11 行完全一致 |
| 灰门王座定位 | ✅ | 均为内部 Boss 房 |
| 状态叠加矩阵（6 类 + 互斥 + 净化） | ✅ | 含 WAR_CRY_BUFF，完全一致 |
| HASTE/SLOW 公式 | ✅ | |
| DoT tick 时序 | ✅ | |
| STEALTH/TAUNT AI 合同（5 条） | ✅ | |
| 净化策略 + 不可净化集合 | ✅ | |
| 铭文规则 + 清单权威 | ✅ | |
| CombatPipeline 12 步交叉引用 | ✅ | |
| 进阶职业解锁 + 通关定义 | ✅ | |
| Respec 规则 | ✅ | |
| Affix V1 预算 | ✅ | |
| Telegraph 阈值 (>=30% / >=50%) | ✅ | |
| Boss 两层 schema (aiProfileId + weight) | ✅ | 含权威声明 |
| Boss 名册 (3 个) + 预算 (3~4) | ✅ | |
| 怪物预算基线引用 | ✅ | |
| ProfileData / RunSummary schema | ✅ | 分文件管理，冻结边界明确 |
| Weight 数值格式 | ⚠️ | P2-1 整数 vs 浮点 |
| BossPhaseDef 字段名 | ⚠️ | P2-2 hpPercent vs hpThreshold |
| OVERCHARGE 消耗触发验证 | ⚠️ | P2-3 checklist 缺失 |
| §6.4 vs Checklist §3 同步 | ⚠️ | P2-4 互有缺漏 |
| LIGHTNING 元素交互排除说明 | ⚠️ | P2-5 缺理由 |

---

## 3. 总体评价

六轮迭代的审阅结果汇总：

| 轮次 | P0 | P1 | P2 | 焦点 |
| --- | --- | --- | --- | --- |
| R1 | 3 | 8 | 7 | 结构性缺失与合同空白 |
| R2 | 2 | 9 | 7 | 系统对齐与权威链 |
| R3 | 0 | 3 | 8 | 状态矩阵与 AI 合同 |
| R4 | 0 | 3 | 7 | 实现就绪度（拓扑/schema/量化门槛） |
| R5 | 0 | 1 | 7 | 核心文档自身一致性 |
| R6 | 0 | 0 | 5 | 微观精度（格式/命名/验证缝隙） |

**结论**：

- **P0 / P1 全部清零**。自 R4 起无 P0，自 R6 起无 P1。
- 剩余 5 个 P2 全部属于命名精度、格式约定和验证清单的最后对齐，不影响任何工作包的启动或实现方向。
- Phase 3 文档体系（执行文档 + checklist + 核心权威文档）已达到**可直接进入代码实现的质量水平**。三份文档的权威链清晰：核心文档提供系统框架 → Phase 3 执行文档冻结本阶段具体 schema → Checklist 提供逐项验收标准。

**建议**：以上 5 个 P2 可以在正式启动 W1 前用一次小的文档补丁统一收口（预计 10 分钟），之后正式进入代码实施。

# Phase 3 Director Review — Round 5

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`（670 行）
2. `docs/phase3/2026-03-13-phase3-verification-checklist.md`（122 行）
3. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（核心权威文档）

**审阅焦点**：在 R4 收口后，以实现者视角做最终一致性扫描——重点是跨文档的 schema 对齐、数值边界精确性和隐含预算缺口。

---

## 0. R4 修复确认

| R4 编号 | 问题 | 状态 |
| --- | --- | --- |
| P1-1 | `bandit_camp` 拓扑统一到 `greenwood_fringe` 下 | ✅ Phase 3 §4.6 line 468 + 核心 §9.2.3 line 3164 一致 |
| P1-2 | BossEncounter + AIProfile 两层结构与 YAML 示例 | ✅ Phase 3 §4.5 lines 340-389 已改为联合示例 |
| P1-3 | Checklist Long Run Lab smoke/full 分组 | ✅ Checklist §2.3 lines 59-65 已拆为共同项 + full 额外项 |
| P2-1 | 可选 zone 等级范围 | ✅ Phase 3 §4.6 table lines 480-492 补齐 |
| P2-2 | 最小 Boss 名册 | ✅ Phase 3 §4.6 table lines 494-502 |
| P2-3 | CombatPipeline 12 步交叉引用 | ✅ Phase 3 §4.1 line 132 |
| P2-4 | 灰门王座改为 `grey_gate_depths` 内部 Boss 房 | ✅ Phase 3 line 471 + 核心 line 3176 |
| P2-5 | 可选 zone 最低内容约束 | ✅ Phase 3 §4.6 lines 504-509 |
| P2-6 | Coverage gate 补上 `core.talent` / `core.world` | ✅ Phase 3 §7 lines 654-655 |
| P2-7 | 白盒 STEALTH/TAUNT 场景 | ✅ Checklist §3 lines 102-107 |

**结论：R4 的 3 P1 + 7 P2 全部已确认修复。R1 ~ R4 所有 P0 / P1 项目已关闭。**

---

## 1. R5 新发现

### 严重程度分布

| 等级 | 数量 |
| --- | --- |
| P0 | 0 |
| P1 | 1 |
| P2 | 7 |

---

### P1-1 核心文档 §7.4.1-7.4.2 Boss 行为模型与 Phase 3 冻结 schema 冲突

**问题**：

核心文档的 Boss 定义存在**内部矛盾**和**与 Phase 3 的跨文档冲突**：

1. 核心 §7.4.1（line 2634）Boss phase 使用 `behaviorScript: "dungeon_lord_phase1"`——字符串引用
2. 核心 §7.4.2（lines 2674-2727）Boss 行为脚本使用 `behaviors[].priority + condition -> action`——确定性模型
3. 但核心 §7.3.2（line 2615）明确规定 Boss phase 内应使用**加权选择**
4. Phase 3 §4.5（lines 363-389）冻结的 YAML 使用 `aiProfileId`（结构化引用）+ `actions[].weight`（加权模型）

内部矛盾：核心文档 §7.4.2 的 Boss 行为示例用了 `priority` 确定性模型，但 §7.3.2 自己的规则说 Boss 应该用加权选择。

跨文档冲突：Phase 3 正确采用了加权模型，但核心文档的 Boss 示例仍然在误导阅读顺序（Phase 3 doc header 要求先阅读核心文档）。

**风险**：实现者先读核心文档 §7.4，会按 `priority` 模型实现 Boss AI，然后到 Phase 3 §4.5 发现 schema 完全不同，导致返工。

**建议操作**：

- 核心文档 §7.4.1 的 `behaviorScript` 字段改为 `aiProfileId`，与 Phase 3 对齐
- 核心文档 §7.4.2 的 Boss 行为脚本示例改为 `weight` 加权模型，与 §7.3.2 自身规则一致
- 或在核心 §7.4 开头加一句权威声明："Phase 3 起，Boss 行为的完整两层 schema 以 Phase 3 执行文档 §4.5 为权威，本节仅保留概念示例"

---

### P2-1 Boss 数量预算：核心 §7.6 写 4 个，Phase 3 最小名册 3 个

**位置**：核心 line 2784 vs Phase 3 lines 494-502

核心文档 §7.6 怪物模板预算表写明 Phase 3 应有 `4 个 Boss`。Phase 3 最小名册只列了 3 个（`molten_giant / dungeon_lord / abyssal_guardian`）。

Phase 3 使用了"最小名册"措辞，隐含可以追加。但从游戏节奏来看，`dungeon_lord`（Lv7-10）到 `abyssal_guardian`（Lv15）之间有 5 级跨度，`underground_river`（Lv10-12）或 `abyssal_temple`（Lv12-15）缺少中间 Boss 支撑。

**建议操作**：
- 方案 A：在名册中追加 1 个 Lv10-13 的 Boss（如 `underground_river` 或 `abyssal_temple` 区域 Boss），与核心文档对齐
- 方案 B：将核心 §7.6 的 Phase 3 Boss 预算从 4 改为 3，并在设计意图中说明 Lv10-15 靠精英组合而非 Boss 支撑节奏

### P2-2 Telegraph 阈值边界：核心"超过 30%"vs Phase 3">= 30%"

**位置**：核心 §6.1.3 line 2346 vs Phase 3 §4.5 line 335

- 核心文档："所有 Boss 技能中伤害**超过**玩家最大 HP 30% 的"——语义为 `>30%`
- Phase 3 文档："单次技能预期伤害 **>= 30%** 玩家最大 HP 时"——语义为 `>=30%`

技术上差异极小（恰好 30% 的技能是否需要 telegraph），但两份文档的字面表述不同。

**建议操作**：统一为 `>=30%`（更保守），同步修改核心 §6.1.3。

### P2-3 Phase 3 状态矩阵缺少 `WAR_CRY_BUFF` 的"唯一效果"类别

**位置**：核心 §8.1.2 line 2846 vs Phase 3 §4.2 lines 152-184

核心文档 §8.1.2 定义了 6 种叠加类别，第 6 种是"**唯一效果**：同一来源只能存在一个，不同来源可共存"，适用于 `WAR_CRY_BUFF`。

Phase 3 §4.2 的叠加矩阵只覆盖了前 5 种类别（不叠加刷新 / 独立叠层 / 上限封顶 / 取较强 / 后来者覆盖），没有提及"唯一效果"类别。

`WAR_CRY_BUFF` 是 Vanguard 的 Phase 2 遗留状态，Phase 3 四基础职业正式树会继承它。缺少该类别意味着 W2a（状态生命周期）的实现者可能遗漏这条规则。

**建议操作**：Phase 3 §4.2 补入第 6 条叠加类别或在脚注中说明"Phase 2 已冻结的 `WAR_CRY_BUFF` 唯一效果规则继续有效，见核心 §8.1.2"。

### P2-4 Boss YAML 示例使用 `iron_golem_boss`，不在最小名册内

**位置**：Phase 3 §4.5 line 344 vs §4.6 lines 494-502

YAML 联合示例的 Boss ID 是 `iron_golem_boss`，但最小名册里是 `molten_giant / dungeon_lord / abyssal_guardian`。示例角色和正式名册不对应。

**建议操作**：将示例 Boss 替换为名册中的某个 Boss（推荐 `molten_giant`，它是最简单的两 phase Boss），或在示例上方加注"以下为结构示例，正式 Boss 名册见 §4.6"。

### P2-5 局间持久化数据缺少 schema 定义

**位置**：Phase 3 §4.6 lines 434-438 + §4.4 line 274

Phase 3 明确承诺局间持久化：
- 进阶职业解锁
- 已发现 profile / 历史 run summary

并且 §4.4 line 274 要求"不得写在当前 run save 内；必须与 profile 或等价账号本地档分离"。

但当前三份文档中均无局间持久化的 data class / schema 定义。核心文档的 `SaveDataV2`（§9.1.1.1）只覆盖局内状态。

**建议操作**：在 Phase 3 §4.6 或核心 §9.2.2 中补一个最小 `ProfileData` skeleton：

```kotlin
@Serializable
data class ProfileData(
    val profileVersion: Int,
    val unlockedClasses: Set<String>,
    val runHistory: List<RunSummary>,
)
```

至少冻结数据粒度和存储边界。

### P2-6 Checklist §2.4 缺少 `WAR_CRY_BUFF` 唯一效果验证

**位置**：Checklist §2.4 lines 67-83

Status Matrix 验证覆盖了刷新、叠层、封顶、取较强、后来者覆盖、互斥等规则，但没有涵盖"同一来源唯一 / 不同来源共存"的 `WAR_CRY_BUFF` 场景。

**建议操作**：在 §2.4 追加一项：
- "`WAR_CRY_BUFF` 的唯一效果规则：同一施法者重复施加只刷新，不同施法者可共存"

### P2-7 核心 §7.6 怪物预算（40 普通 / 12 精英）未被 Phase 3 明确采纳

**位置**：核心 line 2784 vs Phase 3 全文

核心 §7.6 为 Phase 3 设定了 40 普通怪 / 12 精英怪 / 4 Boss 的总预算。Phase 3 文档只在 Boss 层面给出了最小名册，未对普通怪和精英怪的总数做任何承诺或限定。

W5（Class Formalization）和 W6（Long-Run World）的内容 lane 需要知道怪物总量预算来分配各 zone 的怪物池规模。

**建议操作**：
- Phase 3 §3.1 或 §4.6 补一句内容预算概述："Phase 3 怪物模板总预算参照核心 §7.6：约 40 种普通怪、12 种精英怪、3~4 个 Boss；最终数量以实际 zone 填充需求为准。"
- 或在 W6a 的 scope 描述中引用核心 §7.6 作为预算基线。

---

## 2. 全量对齐检查矩阵

以下列出本轮逐项检查的核心对齐点及结果：

| 检查项 | Phase 3 位置 | 核心位置 | 结果 |
| --- | --- | --- | --- |
| Zone 拓扑结构 | §4.6 lines 464-476 | §9.2.3 lines 3160-3174 | ✅ 完全一致 |
| Zone 等级范围（11 个） | §4.6 lines 480-492 | §9.2.3 lines 3161-3173 | ✅ 完全一致 |
| 灰门王座定位 | §4.6 line 471 | §9.2.3 line 3176 | ✅ 均为 grey_gate_depths 内部 Boss 房 |
| 状态叠加矩阵（6 类） | §4.2 lines 152-184 | §8.1.2 lines 2837-2846 | ⚠️ Phase 3 缺第 6 类（P2-3） |
| HASTE/SLOW 公式 | §4.2 line 182 | §8.1.3 line 2855 | ✅ 完全一致 |
| STEALTH 打破条件 | §4.2 line 183 | §8.1.3 line 2857 | ✅ 完全一致 |
| STEALTH/TAUNT AI 合同（5 条） | §4.5 lines 407-413 | §7.5.2 lines 2769-2775 | ✅ 完全一致 |
| 净化策略 | §4.2 lines 191-198 | §8.1.4 lines 2888-2893 | ✅ 完全一致 |
| DoT tick 时序 | §4.2 line 150 | §8.1.1 lines 2824-2828 | ✅ 完全一致 |
| 铭文规则 | §4.4 lines 260-264 | §8.2.2 lines 2940-2946 | ✅ 完全一致 |
| 铭文清单权威 | §4.4 lines 307-314 | §8.2.3 lines 2948-2960 | ✅ Phase 3 正确指向核心权威 |
| CombatPipeline 12 步 | §4.1 line 132 | §3.2.3 lines 647-732 | ✅ 交叉引用已建立 |
| 进阶职业解锁 | §4.4 lines 255-258 | §9.2.2 lines 3143-3152 | ✅ 完全一致 |
| Respec 规则 | §4.3 lines 228-235 | §9.2.2 (隐含) | ✅ Phase 3 为执行权威 |
| Affix V1 预算 | §4.6 lines 513-518 | §9.2.3 lines 3178-3185 | ✅ 完全一致 |
| Telegraph 阈值 | §4.5 line 335 | §6.1.3 lines 2346-2347 | ⚠️ 边界 ">30%" vs ">=30%"（P2-2） |
| Boss 行为 schema | §4.5 lines 330-389 | §7.4.1-7.4.2 | ❌ 字段名和模型冲突（P1-1） |
| Boss 数量预算 | §4.6 lines 494-502 | §7.6 line 2784 | ⚠️ 3 vs 4（P2-1） |
| Checklist smoke/full 分组 | §2.3 lines 59-65 | — | ✅ 已正确拆分 |
| Checklist STEALTH/TAUNT 白盒 | §3 lines 102-107 | — | ✅ 已覆盖 |
| Checklist 铭文验证 | §2.5 lines 85-91 | — | ✅ 已覆盖 |

---

## 3. 总体评价

Phase 3 文档体系经过 R1-R5 五轮迭代后：

- **R1~R4 的全部 P0 / P1 项已关闭**，核心系统合同（状态矩阵、AI DSL 两层结构、zone 拓扑、STEALTH/TAUNT 合同、longRunLab 量化门槛、telegraph 交叉引用、coverage gate）全部收口。
- **R5 唯一的 P1 是核心文档自身的内部矛盾**（§7.4 Boss 示例与 §7.3.2 Boss 规则互斥），本质上是核心文档需要追赶 Phase 3 的冻结精度，不影响 Phase 3 文档的执行力。
- **7 个 P2** 分属三个类型：
  - 跨文档精度对齐（Boss 预算、telegraph 边界、WAR_CRY_BUFF 类别、怪物总量预算）
  - 示例可读性（Boss YAML 用非名册 ID）
  - 缺省定义（局间持久化 schema）

**结论**：Phase 3 执行文档已达到可以启动 W1 的质量水平。P1-1 建议在启动 W4a 之前修复核心文档，避免 Boss AI 实现方向偏差。其余 P2 可以在各对应工作包开始时收口，不阻塞整体启动。

---

## 4. 建议优先级排序

| 优先级 | 操作 | 建议时机 |
| --- | --- | --- |
| 1 | 修复核心 §7.4.1-7.4.2 Boss schema 对齐 Phase 3 | W4a 启动前 |
| 2 | 决定 Boss 数量：补第 4 个或调低核心预算 | W6a 起稿前 |
| 3 | 补 `ProfileData` 最小 schema | W5 起稿前 |
| 4 | Phase 3 §4.2 补 WAR_CRY_BUFF + Checklist 补验证项 | W2a 启动前 |
| 5 | 其余 P2（telegraph 边界、YAML 示例、怪物预算引用） | 各 W 自然窗口内 |

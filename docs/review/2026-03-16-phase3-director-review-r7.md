# Phase 3 Director Review — Round 7

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`（681 行）
2. `docs/phase3/2026-03-13-phase3-verification-checklist.md`（124 行）
3. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（核心权威文档）

**审阅焦点**：R6 修复确认后的收尾扫描——验证列表完整性、引用路径一致性、跨文档枚举覆盖的最后缝隙。

---

## 0. R6 修复确认

| R6 编号 | 问题 | 状态 |
| --- | --- | --- |
| P2-1 | Weight 数值格式统一为整数 | ✅ 核心 §7.4.2 lines 2690/2702/2709/2715 全部改为整数（50/30/15/5），line 2718 新增归一化说明 |
| P2-2 | `hpPercent` 改为 `hpThreshold / hpEnd` | ✅ Phase 3 §4.5 line 397 已对齐 |
| P2-3 | OVERCHARGE LIGHTNING 消耗验证 | ✅ Checklist §2.4 line 80 已补入 |
| P2-4 | §6.4 声明 checklist §3 为权威 | ✅ Phase 3 §6.4 line 648 已添加权威声明 |
| P2-5 | LIGHTNING 元素交互排除说明 | ✅ Phase 3 §4.1 line 100 已补入 OVERCHARGE 走状态规则的说明 |

**结论：R6 的 5 P2 全部已确认修复。**

附加确认：核心 §7.4.2 line 2718 新增的说明（"weight 是相对权重，不要求预先归一化；运行时对通过条件过滤后的候选动作集合重新求和归一化"）与 Phase 3 §4.5 line 339 的语义完全一致。

---

## 1. R7 新发现

### 严重程度分布

| 等级 | 数量 |
| --- | --- |
| P0 | 0 |
| P1 | 0 |
| P2 | 3 |

---

### P2-1 Checklist §2.4 刷新语义验证列表缺少 STUN / SLOW / FREEZE / SILENCE

**位置**：Checklist §2.4 line 74

当前列表：

```
ROOT / HASTE / MARKED / BANE / CURSE / WEAKEN / OVERCHARGE / STEALTH
```

Phase 3 §4.2 lines 154-166 定义了 12 种"不叠加、只刷新持续时间"的状态：`STUN / ROOT / HASTE / SLOW / FREEZE / SILENCE / MARKED / BANE / CURSE / WEAKEN / OVERCHARGE / STEALTH`。

Checklist 缺少 4 种：

| 缺失状态 | 被其他测试项部分覆盖？ | 缺口程度 |
| --- | --- | --- |
| `STUN` | 否——无任何 checklist 项单独验证 STUN 刷新 | 中 |
| `SLOW` | 部分——line 79 的 HASTE/SLOW 抵消测试覆盖了共存场景，但未覆盖 SLOW 单独重复施加的刷新 | 低 |
| `FREEZE` | 部分——line 78 的 FREEZE/BURN 覆盖测试覆盖了互斥场景，但未覆盖 FREEZE 单独重复施加的刷新 | 低 |
| `SILENCE` | **否**——`SILENCE` 在整个 checklist 中完全不存在 | 高 |

**影响**：`SILENCE` 是 Phase 3 正式主线状态（§4.2 line 147），但 checklist 未提及任何 SILENCE 相关的验证项。实现者可能遗漏对 SILENCE 刷新行为的回归测试。

**建议操作**：将 line 74 补全为全部 12 种状态：`STUN / ROOT / HASTE / SLOW / FREEZE / SILENCE / MARKED / BANE / CURSE / WEAKEN / OVERCHARGE / STEALTH`。

### P2-2 §6.4 的 checklist 引用使用了绝对文件系统路径

**位置**：Phase 3 §6.4 line 648

当前链接：

```markdown
[phase3-verification-checklist.md](/Users/luo/Documents/github/K-ToME/docs/phase3/2026-03-13-phase3-verification-checklist.md)
```

最近的提交 `6efa012` 已明确将索引页改为相对路径。§6.4 这条引用仍在使用绝对文件系统路径，会在其他开发者克隆仓库后失效。

**影响**：链接在非本机环境下不可用。

**建议操作**：改为同目录相对路径：

```markdown
[phase3-verification-checklist.md](2026-03-13-phase3-verification-checklist.md)
```

### P2-3 Checklist §2.4 不可净化集合验证不完整

**位置**：Checklist §2.4 line 82

当前文本：

```
不可净化集合包含 STEALTH 与 Boss phase 锁定状态
```

Phase 3 §4.2 lines 196-200 和核心 §8.1.4 line 2884 定义的完整不可净化集合为：

- `KNOCKBACK`（瞬时效果）
- `INVULNERABLE`
- `STEALTH`
- Boss phase 锁定状态

Checklist 漏了 `KNOCKBACK` 和 `INVULNERABLE`。虽然 `KNOCKBACK` 是瞬时效果（净化不太会发生），但 `INVULNERABLE` 作为 buff 可能出现在 Boss 或怪物身上——玩家的驱散技能不应能移除它，这需要显式验证。

**建议操作**：将 line 82 补全为：`不可净化集合包含 KNOCKBACK、INVULNERABLE、STEALTH 与 Boss phase 锁定状态`。

---

## 2. 全量对齐最终矩阵

| 检查项 | 结果 | 备注 |
| --- | --- | --- |
| Zone 拓扑（11 个节点） | ✅ | 三份文档完全一致 |
| Zone 等级范围 | ✅ | 11 行完全一致 |
| 灰门王座定位 | ✅ | 均为内部 Boss 房 |
| 状态叠加矩阵（6 类 + 互斥 + 净化） | ✅ | 含 WAR_CRY_BUFF，完全一致 |
| HASTE/SLOW 公式 | ✅ | |
| DoT tick 时序 | ✅ | |
| STEALTH/TAUNT AI 合同（5 条） | ✅ | |
| 净化策略 + 不可净化集合 | ✅ | 文档间一致 |
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
| Weight 数值格式 | ✅ | 已统一为整数 + 归一化说明 |
| BossPhaseDef 字段名 | ✅ | 已统一为 hpThreshold / hpEnd |
| OVERCHARGE 消耗触发验证 | ✅ | Checklist 已补入 |
| §6.4 vs Checklist §3 权威链 | ✅ | §6.4 已声明 checklist 为权威 |
| LIGHTNING 元素交互排除说明 | ✅ | §4.1 已补入 |
| 刷新语义验证列表完整性 | ⚠️ | P2-1 缺 STUN/SLOW/FREEZE/SILENCE |
| §6.4 引用路径格式 | ⚠️ | P2-2 绝对路径 |
| 不可净化集合验证完整性 | ⚠️ | P2-3 缺 KNOCKBACK/INVULNERABLE |

---

## 3. 总体评价

七轮迭代的审阅结果汇总：

| 轮次 | P0 | P1 | P2 | 焦点 |
| --- | --- | --- | --- | --- |
| R1 | 3 | 8 | 7 | 结构性缺失与合同空白 |
| R2 | 2 | 9 | 7 | 系统对齐与权威链 |
| R3 | 0 | 3 | 8 | 状态矩阵与 AI 合同 |
| R4 | 0 | 3 | 7 | 实现就绪度（拓扑/schema/量化门槛） |
| R5 | 0 | 1 | 7 | 核心文档自身一致性 |
| R6 | 0 | 0 | 5 | 微观精度（格式/命名/验证缝隙） |
| R7 | 0 | 0 | 3 | 验证列表枚举完整性与路径规范 |

**结论**：

- **P0 / P1 连续 5 轮清零**（P0 自 R3 起、P1 自 R6 起）。
- 剩余 3 个 P2 全部是 checklist 的枚举补全和一处链接格式修正，不影响任何工作包的启动或实现方向。
- 从 R1 的 18 项（3P0 + 8P1 + 7P2）到 R7 的 3 项（0P0 + 0P1 + 3P2），文档体系经过 7 轮审阅已达到**生产就绪**水平。

**建议**：3 个 P2 可在一次提交中完成（纯文本追加，无需重新审阅），之后正式进入 W1 代码实施。

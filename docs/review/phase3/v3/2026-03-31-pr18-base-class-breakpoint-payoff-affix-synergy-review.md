# PR-18 Base Class Breakpoint Payoff & Affix Synergy — 深度审查报告

**审查日期**: 2026-03-31  
**审查角色**: 资深 Roguelike 游戏设计总监 / 系统策划总监 / 玩法体验审查  
**对照文档**: `docs/review/phase3/v3/2026-03-30-phase3-v3-pr-18-base-class-breakpoint-payoff-and-affix-synergy.md`  
**分支**: `codex/p3-v3-pr18-base-class-breakpoint-payoff-affix-synergy`  
**改动规模**: 135 files changed, +5736 / -494  
**`./gradlew check` 状态**: **全绿**

---

## 一、执行摘要

PR-18 的核心目标是"用少量高价值改动让四个基础职业的 build 差异更容易被玩家真实感知"。从实现完成度来看，**文档要求的主体功能均已落地**，且整体设计质量较高——breakpoint payoff 全部走"新增机制型效果"而非简单数值膨胀，affix-tag synergy 通过 `DamageVsStatus` + 语义标签映射实现了最小可用主链。

**总体一致性评分：88/100**

主要扣分项集中在：测试覆盖的局部盲区（preview-reality 交叉验证缺失）、AffixBuildTags 单元测试只覆盖了一个职业、以及 LongRunLab 对 affix synergy 的观测尚未闭环。

---

## 二、逐项一致性矩阵

### 2.1 完成标准对照

| # | 文档要求 | 实现状态 | 一致性 | 备注 |
|---|---------|---------|--------|------|
| 1 | 每个基础职业至少 1 个明确 breakpoint payoff | ✅ 达标 | **100%** | Vanguard 2 个、Arcanist 1 个、Rogue 2 个、Templar 2 个，共 7 个 |
| 2 | payoff 能通过 DescriptionModel / breakpoint preview 清晰展示 | ⚠️ 部分达标 | **75%** | i18n 文案完整(14+14)，DescriptionPresenterTest 仅覆盖 2/7 个 payoff 的预览渲染 |
| 3 | 至少一层 affix/buildTags 联动影响玩法选择 | ✅ 达标 | **95%** | `DamageVsStatus` 被动 + 语义标签映射 + 黑名单冲突，主链完整 |
| 4 | 不引入新资源轴，不改已冻结职业身份 | ✅ 达标 | **100%** | 严格遵守 |

### 2.2 W18a — Base-Class Breakpoint Payoff

| 检查项 | 文档要求 | 实现状态 | 一致性 |
|--------|---------|---------|--------|
| Vanguard payoff 围绕 GUARD/armor_break/taunt/hold_line | `guard_stance` R4 解锁 GUARD；`taunt` R4 解锁 GUARD | ✅ **100%** |
| Arcanist payoff 围绕 burn/freeze/teleport/mana tempo | `blink` R4 解锁 resourceRestoreFraction (mana tempo) | ✅ **100%** |
| Rogue payoff 围绕 stealth/marked/crit/execute | `shadowstep` R2 解锁 MARKED；`shadow_bind` R4 解锁 MARKED | ✅ **100%** |
| Templar payoff 围绕 bane/holy shield/cleanse/sustain | `holy_mark` R3 解锁 BANE；`purify` R4 解锁 HOLY_SHIELD_BUFF | ✅ **100%** |
| 优先增强现有 breakpoint，而非新增树或 active talent | 全部复用现有 talent 的 levelEffects 结构 | ✅ **100%** |
| rank 3-4 产生"玩法变化"而非单纯数值更大 | 全部 7 个 breakpoint 都是新增效果类型（状态/资源恢复），无纯数值型 | ✅ **100%** |
| payoff 描述可区分"已生效"vs"下一断点将生效" | i18n 有 `breakpoint.preview.*` 系列模板 + `breakpoint.flavor.*` 系列文案 | ✅ **95%** |

**职业 Breakpoint 完整矩阵**：

| 职业 | Talent | 断点 Rank | 解锁内容 | 玩法变化类型 |
|------|--------|-----------|---------|-------------|
| Vanguard | guard_stance | R4 | +GUARD 状态 (0.15, 2t) | 姿态→真实顶线窗口 |
| Vanguard | taunt | R4 | +GUARD 状态 (0.10, 2t) | 嘲讽→自我防御组合 |
| Arcanist | blink | R4 | +resourceRestoreFraction 0.10 | 传送→法力循环 |
| Rogue | shadowstep | R2 | +MARKED 状态 (2t) | 位移→标记→处决链 |
| Rogue | shadow_bind | R4 | +MARKED 状态 (3t) | 定身→标记双debuff |
| Templar | holy_mark | R3 | +BANE 状态 (0.12, 2t) | 标记→灾厄易伤叠加 |
| Templar | purify | R4 | +HOLY_SHIELD_BUFF (0.10, 2t) | 净化→护盾防御窗口 |

> **设计评价**：7 个 breakpoint 全部走"新增效果类型"路线，没有一个是纯数值放大。Rogue 的 shadowstep R2 断点尤其巧妙——让 marked 在早期就能成为 build 核心，与 shadow_bind R4 形成"前哨+主力"的双断点链。Templar 的 holy_mark R3 + purify R4 也形成了"进攻标记→防御护盾"的分化节奏。**设计质量超出文档最低要求。**

### 2.3 W18b — Affix-Tag Synergy

| 检查项 | 文档要求 | 实现状态 | 一致性 |
|--------|---------|---------|--------|
| guard/armor_break 联动 | `of_piercing` DamageVsStatus:ARMOR_BREAK +12%；`sentinel`/`warforged` 标签含 guard/hold_line | ✅ **100%** |
| burn/freeze 联动 | `of_flames` DamageVsStatus:BURN +12%；`of_frost` DamageVsStatus:FREEZE +12%；fire/cold 互斥黑名单 | ✅ **100%** |
| stealth/crit/marked 联动 | `of_shadow` DamageVsStatus:MARKED +12%；`of_precision` DamageVsStatus:MARKED +15%；标签含 stealth/crit/execute | ✅ **100%** |
| holy/bane/cleanse 联动 | `of_smite` DamageVsStatus:BANE +15%；`of_cleansing` HpRegenPerTurn；`hallowed` 标签含 holy_shield/cleanse | ✅ **100%** |
| affix 为特定 buildTags 提供更高权重 | AffixBuildTags 通过 talentRanks → 语义标签映射，影响 AffixSelectionContext | ✅ **95%** |
| 不发明第二套 build 偏好真源 | 复用现有 EquipmentPassive 体系，仅新增 DamageVsStatus 子类 | ✅ **100%** |
| 复用 EquipmentPassive 优先 | DamageVsStatus 作为最小扩展加入 sealed interface | ✅ **100%** |
| passive 冲突防护 | AffixGenerator 新增 passive-to-passive 互斥逻辑 | ✅ **100%** |

**Affix 被动效果矩阵**：

| Affix | 被动类型 | 目标状态 | 加成 | 职业倾向 |
|-------|---------|---------|------|---------|
| of_precision | DamageVsStatus | MARKED | +15% | Rogue |
| of_piercing | DamageVsStatus | ARMOR_BREAK | +12% | Vanguard |
| of_flames | DamageVsStatus | BURN | +12% | Arcanist |
| of_frost | DamageVsStatus | FREEZE | +12% | Arcanist |
| of_smite | DamageVsStatus | BANE | +15% | Templar |
| of_shadow | DamageVsStatus | MARKED | +12% | Rogue |
| of_cleansing | HpRegenPerTurn | — | +1/turn | Templar |

> **设计评价**：affix synergy 的设计非常克制。没有搞复杂的套装系统，而是通过"状态条件伤害"这一个简洁机制，把天赋断点（施加状态）→ 装备被动（对该状态加伤）串联成自然的 build loop。fire/cold、holy/shadow 的黑名单互斥也避免了无脑堆叠。**符合"方向性增强，不做系统重写"的止损口径。**

### 2.4 W18c — QA And Build Observation

| 检查项 | 文档要求 | 实现状态 | 一致性 | 偏差说明 |
|--------|---------|---------|--------|---------|
| breakpoint preview 与实际效果一致 | ⚠️ 间接覆盖 | **60%** | TalentSchemaTest 验证了 7 个断点定义，DescriptionPresenterTest 验证了 2 个预览渲染，但**缺少两者的交叉验证** |
| affix synergy 不出现非法组合 | ✅ 达标 | **90%** | PassiveEffectResolverTest 验证被动机制正确性；AffixGenerator 有 passive 冲突防护 |
| 基础职业在 smoke 中走出不同倾向 build | ✅ 达标 | **85%** | SmokeBotTalentAllocationTest 验证断点天赋优先分配；LongRunLabTest 验证 build hash 变化 |
| LongRunLab 能观测 payoff 是否被拿到 | ✅ 达标 | **95%** | breakpointPayoffObservationCount、talent distribution、effect distribution 均有断言 |
| LongRunLab 能观测 payoff 是否改变 buildHash | ✅ 达标 | **95%** | breakpointPayoffBuildHashChangeCount >= 4 断言存在 |

---

## 三、冻结口径合规检查

| 冻结口径 | 是否违反 | 说明 |
|----------|---------|------|
| 不靠大扩容 talent 数量 | ✅ 未违反 | 未新增任何 talent，仅修改现有 talent 的 levelEffects |
| 每职业最多补 1-2 个 payoff | ✅ 未违反 | Vanguard 2、Arcanist 1、Rogue 2、Templar 2 |
| 复用现有主链 (TalentBreakpoint/typed effect op/DescriptionModel/buildTags) | ✅ 未违反 | 全部复用 |
| affix synergy 重点是让已有 affix 更有方向性 | ✅ 未违反 | 通过 buildTags 语义映射实现 |
| 不引入新核心枚举或第二套装备规则解释 | ✅ 未违反 | 仅新增 DamageVsStatus 作为 EquipmentPassive 的最小扩展 |

---

## 四、出口门禁检查

| 门禁 | 状态 | 说明 |
|------|------|------|
| 四个基础职业各有至少一个 build-defining payoff | ✅ **通过** | Vanguard(2), Arcanist(1), Rogue(2), Templar(2) |
| affix synergy 进入正式主链 | ✅ **通过** | DamageVsStatus 被动 + AffixBuildTags 语义映射 + PassiveEffectResolver 结算 |
| 文案、说明、测试同步更新 | ⚠️ **有瑕疵** | en-US/zh-CN 均已同步(14条对14条)；测试有局部盲区(见下) |
| `./gradlew check` 保持绿色 | ✅ **通过** | BUILD SUCCESSFUL |

---

## 五、关键偏差与问题识别

### 5.1 [P1] DescriptionPresenter 预览覆盖不足

**偏差程度**: 中等  
**文档要求**: "payoff 的描述必须在 UI 中可区分：已生效 / 下一断点将生效"  
**实际情况**: DescriptionPresenterTest 仅测试了 2/7 个 breakpoint 的预览渲染（charge R5 stun、blink R4 mana restore），遗漏了 guard_stance、taunt、shadowstep、shadow_bind、holy_mark、purify 的预览。

**风险**: 如果 i18n 模板变量与 YAML 数据结构不匹配，5 个未测试的预览可能在客户端渲染出错或显示占位符。

**修复建议**:
```
在 DescriptionPresenterTest 中补充以下测试用例：
- guard_stance R4 → GUARD 预览
- taunt R4 → GUARD 预览
- shadowstep R2 → MARKED 预览
- shadow_bind R4 → MARKED 预览
- holy_mark R3 → BANE 预览
- purify R4 → HOLY_SHIELD_BUFF 预览
```

### 5.2 [P1] Preview-Reality 交叉验证缺失

**偏差程度**: 中等  
**文档要求**: "breakpoint preview 与实际效果一致"  
**实际情况**: TalentSchemaTest 验证了断点定义的正确性，DescriptionPresenterTest 验证了预览渲染的正确性，但**没有测试把两者关联**——即"预览说的效果 = 实际解锁的效果"。

**风险**: 如果有人修改了 YAML 中的断点效果但忘了更新 i18n 文案，或反过来，这类不一致无法被自动捕获。

**修复建议**:
```
新增一个 cross-validation test（可放在 TalentSchemaTest 或单独文件），
逻辑为：对 7 个 breakpoint talent，分别：
1. 从 YAML 中读取断点 rank 和效果定义
2. 从 i18n 中读取对应的 breakpoint.flavor key
3. 断言 key 存在且包含正确的效果关键词（如 "guard"、"marked"、"mana" 等）
```

### 5.3 [P2] AffixBuildTagsTest 只覆盖 Arcanist

**偏差程度**: 轻微  
**文档要求**: "基础职业在 smoke 中能走出不同倾向的 build"  
**实际情况**: `AffixBuildTagsTest` 只有一个测试方法 `profession build tags include axes trees and unlocked talent semantics`，且仅测试了 Arcanist 的 build tag 生成。Vanguard、Rogue、Templar 的 tag 生成逻辑未被直接测试。

**风险**: 如果某职业的 talent YAML 标签配置有误，其 build tag 可能不包含预期的语义标签（如 Vanguard 缺少 `guard`），导致 affix synergy 不生效。

**修复建议**:
```
为 AffixBuildTagsTest 补充 3 个测试用例，分别验证：
- Vanguard talentRanks={guard_stance:4} → buildTags 包含 guard, hold_line
- Rogue talentRanks={shadowstep:2} → buildTags 包含 marked, crit, execute
- Templar talentRanks={holy_mark:3} → buildTags 包含 bane, holy
```

### 5.4 [P2] LongRunLab 未观测 Affix Synergy 实际触发

**偏差程度**: 轻微  
**文档要求**: "affix synergy 进入正式生成/结算路径"  
**实际情况**: LongRunLabTest 观测了 breakpoint payoff 的解锁和 buildHash 变化，但**未观测 affix synergy 是否在战斗结算中实际触发**。即没有统计"DamageVsStatus 被动在实际战斗中触发了多少次"或"带 synergy affix 的装备是否被优先选取"。

**风险**: 可能出现"系统上可用但实际上永远不被触发"的死代码场景。

**修复建议**:
```
在 HeadlessRunHarness 或 ScenarioReport 中增加：
- affixSynergyActivationCount: 记录 DamageVsStatus 被动在战斗中被匹配的次数
- 在 LongRunLabTest 中断言 >= 1，确认 affix synergy 不是死代码
```

### 5.5 [P3] Rogue shadowstep R2 断点的设计意图需确认

**偏差程度**: 可忽略，仅为设计备注  
**文档建议**: "每职业至少让 1 个 tree 在 rank 3~4 时产生玩法变化"  
**实际情况**: Rogue 的 shadowstep 在 **R2** 就触发断点（MARKED），远早于文档建议的 R3-4 窗口。

**评估**: 这不是 bug。从玩法设计角度看，R2 断点让 Rogue 能更早形成"标记→处决"的 build identity，与 shadow_bind R4 形成双层递进。但需要确认这是刻意设计而非疏忽——如果是刻意的，建议在设计文档中补充说明。

---

## 六、超出预期的正向发现

### 6.1 AffixGenerator passive 冲突防护

文档未明确要求，但实现中在 AffixGenerator 增加了"同一件装备不能叠加多个 passive affix"的逻辑。这是一个高质量的防御性设计，避免了 affix 堆叠导致的数值失控。

### 6.2 StatusSemanticTags 语义映射体系

`AffixBuildTags.statusSemanticTags()` 的实现超出了"简单标签映射"的预期——它把状态效果 ID 映射为一组语义标签（如 `marked → [marked, crit, execute]`），让 affix 选择能感知到 build 的深层意图而非只看表面标签。这是一个有远见的设计。

### 6.3 Tag 规范化 (normalizeBuildTag)

`mark → marked`、`cleansing → cleanse` 的规范化处理避免了因 YAML 中标签拼写不一致导致的 synergy 断裂，是一个容易被忽视但非常重要的基建工作。

### 6.4 breakpointEffectKind 分类体系

FoundationGameSession 中的 `breakpointEffectKind()` 把 EffectOp 映射为可读的 kind 字符串（如 `apply_status:guard`、`resource_restore:mana`），为后续的观测和分析提供了干净的分类轴。

---

## 七、玩法体验审查（设计总监视角）

### 7.1 Build 感知层评估

**核心问题回答：玩家能否感知到"我这局是另一种玩法"？**

| 职业 | 断点前体验 | 断点后体验 | 感知差异度 |
|------|-----------|-----------|-----------|
| Vanguard | "我更肉了" | "我能主动开启一段顶线窗口" | ★★★★ 高 |
| Arcanist | "blink 是保命技" | "blink 变成法力循环的一部分" | ★★★☆ 中高 |
| Rogue | "我打得更疼了" | "我能标记→处决，有明确的击杀链" | ★★★★★ 非常高 |
| Templar | "我能治疗" | "我能标记→灾厄叠加 / 净化→护盾窗口" | ★★★★ 高 |

**结论**：四个职业的 breakpoint 都成功将体验从"数值更大"转向"新增机制/新增操作模式"。Rogue 的双断点链（R2 标记 + R4 定身标记）尤其出色，提供了清晰的 build 递进感。

### 7.2 Affix Synergy 的玩家感知评估

当前 affix synergy 的感知路径：

```
玩家升级天赋到断点 → 解锁新状态效果(如 MARKED)
  → 获得装备时 → affix 带有 DamageVsStatus:MARKED +15%
    → 战斗中对 marked 敌人自动触发额外伤害
      → 玩家感知："这把武器配合我的标记天赋特别好用"
```

**优点**: 联动是自然发生的，不需要玩家刻意"配装"。  
**不足**: 当前缺少"告知玩家为什么这把武器更好"的 UI 提示。affix 的被动效果在 tooltip 中是否清晰展示了"对 MARKED 目标 +15%"？如果不清晰，玩家可能感知不到这是 synergy，只是觉得"这把武器伤害高"。

### 7.3 风险评估

| 风险项 | 文档识别 | 实际状况 | 评估 |
|--------|---------|---------|------|
| payoff 做太大改变平衡曲线 | ✅ 已识别 | 加成值控制在 0.10-0.18 范围，持续 2-3 回合 | ✅ 风险可控 |
| affix synergy 做太多长成 Phase 4 生态 | ✅ 已识别 | 仅新增 DamageVsStatus 一个子类，未引入套装/组合规则 | ✅ 风险可控 |

---

## 八、修复优先级与行动建议

### 8.1 P1 — 建议在合并前修复

| # | 问题 | 修复方式 | 预估工作量 |
|---|------|---------|-----------|
| 1 | DescriptionPresenter 预览覆盖 2/7 | 在 DescriptionPresenterTest 补 5 个预览渲染测试 | 小 |
| 2 | Preview-reality 交叉验证缺失 | 在 TalentSchemaTest 或新文件中加 key 存在性 + 关键词断言 | 小 |

### 8.2 P2 — 建议在 PR-19 前修复

| # | 问题 | 修复方式 | 预估工作量 |
|---|------|---------|-----------|
| 3 | AffixBuildTagsTest 仅覆盖 Arcanist | 补充 Vanguard/Rogue/Templar 3 个测试用例 | 小 |
| 4 | LongRunLab 未观测 affix synergy 触发 | 在 HeadlessRunHarness 添加 synergy 触发计数器 | 中 |

### 8.3 P3 — 可延后到 Phase 4

| # | 问题 | 说明 |
|---|------|------|
| 5 | shadowstep R2 断点设计意图确认 | 确认后在设计文档补充说明即可 |
| 6 | affix synergy 的 UI 感知（tooltip 是否清晰展示被动加成条件） | 属于 UI 呈现层问题，与本 PR 的系统层无关 |

---

## 九、最终结论

PR-18 是一个**高质量的系统增强 PR**。它在严格遵守冻结口径的前提下，成功地为四个基础职业注入了差异化的 build identity。设计上的亮点包括：

1. **全部 7 个 breakpoint 都是机制型而非数值型**——这是最关键的设计决策，直接决定了 build 感知层的质量。
2. **affix synergy 走"状态条件伤害"路线**——用一个简洁的机制把天赋断点和装备被动串联起来，避免了过度设计。
3. **基建工作扎实**（tag 规范化、effectKind 分类、passive 冲突防护），为后续扩展留出了干净的接口。

**建议**: 修复 2 个 P1 测试覆盖问题后可合并。P2 问题可在后续 PR 中补齐。

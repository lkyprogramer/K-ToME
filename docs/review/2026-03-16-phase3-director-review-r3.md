# Phase 3 深度评审报告（第三轮）

> **日期**：2026-03-16
> **评审者视角**：资深游戏设计与开发总监
> **评审对象**：修订后的 `docs/phase3/` 全套文档 + `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
> **本轮定位**：R2 报告的 P0 跨文档冲突已全部修复，状态规则大面积补全。本轮聚焦**堆叠矩阵遗漏、未落地的 R2 P1/P2 项、以及首次发现的新问题**。
> **结论等级**：🔴 P0 阻塞 / 🟠 P1 重要 / 🟡 P2 建议 / ✅ 已修复

---

## 0. 上轮修复确认

| R2 ID | 问题 | 状态 |
|---|---|---|
| R2-P0-01 | 铭文清单两文档零重叠 | ✅ Phase 3 主文档不再维护独立清单，权威回收到补充设计文档 8.2.3；Phase 3 只保留类别覆盖要求 |
| R2-P0-02 | Respec 规则直接矛盾 | ✅ 补充设计文档 6.2.2 已改为"Phase 3 默认免费；Phase 4+ 可再引入限次或道具成本"，与 Phase 3 主文档一致 |
| R2-P1-01 | DSL 双 schema 冲突 | ✅ 补充设计文档 7.3.4 第 6 条已显式说明双模型设计：普通怪用 `priority + condition`，Boss 可在条件过滤后做 `weight` 加权选择并写入 trace |
| R2-P1-02 | 净化默认策略矛盾 | ✅ 补充设计文档 8.1.4 的 `CleanseOrder` 默认值已改为 `LONGEST_REMAINING` |
| R2-P1-03 | 不可净化集合不一致 | ✅ 两份文档已统一为 `KNOCKBACK / INVULNERABLE / STEALTH / Boss phase 锁定状态` |
| R2-P1-04 | GUARD / MARKED 未在堆叠矩阵 | ✅ Phase 3 主文档：GUARD 在"取较强值"类别，MARKED 在"不叠加、刷新"类别。补充设计文档 8.1.2 同步 |
| R2-P1-05 | P3 新增 7 状态缺堆叠规则 | ✅ 两份文档均已补入：BANE/CURSE/WEAKEN/OVERCHARGE/STEALTH 归入"不叠加、刷新"；INVULNERABLE 归入"取较强值"；TAUNT 独立为"后来者覆盖" |
| R2-P1-06 | 4.2 节重复编号 | ✅ 已修正为 1–4 连续编号 |
| R2-P1-07 | EQUILIBRIUM 放错位置 | ✅ 已移至 4.4 节 Spellblade 段落；4.6 节改为交叉引用 |
| R2-P2-01 | DoT tick 时机未定义 | ✅ 第 149 行明确"目标回合开始、行动前 tick，STUN/FREEZE 不阻止 tick"；Checklist 2.4 同步覆盖 |

---

## 1. 堆叠矩阵仍有遗漏

### 1.1 🟠【R3-P1-01】ROOT 在两份文档的堆叠矩阵中均缺失

**位置**

- Phase 3 主文档第 146 行将 `ROOT` 列入 P2→P3 升级状态清单
- 补充设计文档第 1820 行 `StatusEffectType` 枚举包含 `ROOT`
- 补充设计文档第 2882 行净化策略明确 `ROOT` 与 `STUN` 同为优先清除的硬控

但是——两份文档的堆叠矩阵（Phase 3 主文档 4.2 节的分类列表 + 补充设计文档 8.1.2 的表格）都没有 `ROOT` 的条目。

**后果**

实现者无法确定两个 ROOT 源同时施加时的行为。从语义上看 ROOT 与 STUN 类似（硬控），应该是"不叠加、刷新持续时间"。

**修复建议**

在两份文档的堆叠矩阵中，将 `ROOT` 加入"不叠加、刷新持续时间"类别（与 STUN / FREEZE / SILENCE 同组）。

---

### 1.2 🟡【R3-P2-01】HASTE 缺少自身堆叠分类

**位置**

- Phase 3 主文档第 146 行将 `HASTE` 列入 P2→P3 升级状态清单
- HASTE 仅出现在互斥/覆盖第 6 组（与 SLOW 的交互），未被分配到任何堆叠类别

**问题**

如果玩家同时获得两个不同来源的 HASTE（例如天赋 buff + 铭文），规则是什么？

从 `speedModifier` 净值公式（`sum(hasteModifiers) + sum(slowModifiers)`）来看，`sum()` 暗示多个 HASTE 修正可以共存并叠加。但 SLOW 被归入"不叠加、刷新持续时间"类别，逻辑上 HASTE 也应是同类——此时 `sum()` 实际只有一个元素。

**修复建议**

方案 A：如果 HASTE 也是"不叠加、刷新"，则在分类 1 中补入 HASTE，并将公式措辞简化为 `effectiveSpeed = baseSpeed + hasteModifier + slowModifier`（去掉暗示多源的 `sum`）。

方案 B：如果有意允许多源 HASTE 叠加（例如天赋给 +200 速度、铭文再给 +100 速度），则应新增一条明确说明 HASTE 为"独立叠层"或"可叠加"，并且要标明与 SLOW 的不对称性。

建议采用方案 A 以保持和 SLOW 的对称性。

---

## 2. R2 P1 级未落地项

### 2.1 🟠【R3-P1-02】STEALTH / TAUNT 对 AI 决策的影响仍未写入 DSL 规格

**位置**

Phase 3 主文档第 4.5 节（AIProfile DSL 与 BossEncounter）

**问题**

R2-P1-08 指出 STEALTH 和 TAUNT 是 Phase 3 新增的、直接影响 AI 行为的两个状态，DSL 规格中应有对应处理说明。当前版本仍无相关内容。

DSL 已有 `useLastKnownPosition: true` 字段，但没有将其与 STEALTH 场景关联。TAUNT 的强制目标切换行为完全缺失。

**修复建议**

在 4.5 节冻结口径或 DSL 示例后补入：

```
STEALTH/TAUNT 与 AI 的交互合同：

1. 目标进入 STEALTH 后，AI 的目标引用变为无效。
   行为：AI 移动到目标最后已知位置（useLastKnownPosition）。
   到达后仍未发现目标，回退到 defaultBehavior（如 PATROL）。
   Boss 遇到 STEALTH 时，不切换 phase，在当前 phase 内
   执行无目标 fallback（如使用 AoE 技能扫描）。

2. AI 被 TAUNT 命中后：
   在 TAUNT 持续期间，强制将攻击目标设为嘲讽源。
   若嘲讽源不在攻击范围内，AI 必须移动靠近，不得攻击其他目标。
   TAUNT 过期后，恢复原有目标选择逻辑。
```

---

### 2.2 🟠【R3-P1-03】Boss telegraph 伤害阈值缺少交叉引用

**位置**

Phase 3 主文档第 4.5 节第 3 条冻结口径

**问题**

R2-P1-09 指出补充设计文档定义了关键规则（伤害 ≥30% 最大 HP → ≥1 回合 telegraph，≥50% → ≥2 回合），但 Phase 3 主文档只说"Boss 高伤技能必须有 telegraph"，无具体阈值也无交叉引用。W4 实现者只看 Phase 3 主文档时会遗漏。

**修复建议**

在第 328–329 行（冻结口径第 3 条）后补入：

> telegraph 的伤害阈值与预览回合数见补充设计文档第 6.1.3 节（≥30% 最大 HP → ≥1 回合预览，≥50% → ≥2 回合）。

---

## 3. R2 P2 级未落地项

### 3.1 🟡【R3-P2-02】Zone 描述性文字仍中英混用

**位置**

Phase 3 主文档第 423–427 行

```
shattered_outpost -> greenwood_fringe -> deep_iron_pit -> grey_gate_depths
精灵遗迹 / 熔岩核心 / 盗贼营地 等可选支线
地下河 -> 水晶洞穴 -> 深渊神殿 -> 深渊之心 的 Phase 3 扩展终线
```

**问题**

第 1 条使用英文 ID，第 2–3 条使用中文名。Zone graph（第 432–443 行）已全部使用英文 ID，但描述性列表仍不一致。这在数据驱动设计中会导致实现者混用命名。

**修复建议**

统一为英文 ID 加括号中文标注：

```
1. shattered_outpost -> greenwood_fringe -> deep_iron_pit -> grey_gate_depths（主线）
2. elven_ruins（精灵遗迹）/ molten_core（熔岩核心）/ bandit_camp（盗贼营地）等可选支线
3. underground_river（地下河）-> crystal_cavern（水晶洞穴）-> abyssal_temple（深渊神殿）-> abyssal_heart（深渊之心）的 P3 扩展终线
```

---

### 3.2 🟡【R3-P2-03】Phase 3 扩展 zone 缺少等级范围表

**位置**

Phase 3 主文档第 4.6 节

**问题**

补充设计文档 9.2.3 已定义了 Phase 3 新 zone 的等级范围（underground_river Lv10–12，crystal_cavern Lv11–13，abyssal_temple Lv12–15，abyssal_heart Lv15），但 Phase 3 主文档只有连接拓扑图，完全没有等级范围。

**修复建议**

在连接关系图后补入等级范围表，与补充设计文档 9.2.3 对齐：

```
| Zone ID | 中文名 | 等级范围 |
|---|---|---|
| shattered_outpost | 破碎前哨 | Lv1–3 |
| greenwood_fringe | 绿林边缘 | Lv3–5 |
| deep_iron_pit | 深铁矿坑 | Lv5–8 |
| grey_gate_depths | 灰门深渊 | Lv8–10 |
| underground_river | 地下河 | Lv10–12 |
| crystal_cavern | 水晶洞穴 | Lv11–13 |
| abyssal_temple | 深渊神殿 | Lv12–15 |
| abyssal_heart | 深渊之心 | Lv15 |
```

---

### 3.3 🟡【R3-P2-04】longRunLab `full` 模式缺少量化验收标准

**位置**

Phase 3 主文档出口门禁第 4 条（第 571 行）

**问题**

"`full` 模式的死亡分布和平均时长落在预期范围"——"预期范围"仍未定义。

**修复建议**

在出口门禁第 4 条或第 6.2 节补入第一版量化参考（后续可调整）：

- headless run 的等价回合数上限：建议 3000 回合（约对应 4–6 小时人类游玩）
- 4 基础职业 × 3 种族的 12 个组合中，至少 8 个可到达 `abyssal_temple`
- 死亡分布：50% 以上的 run 应在 `deep_iron_pit` 之后死亡（说明前期难度曲线不至于过陡）

---

### 3.4 🟡【R3-P2-05】Verification Checklist 缺少铭文验证段

**位置**

`docs/phase3/2026-03-13-phase3-verification-checklist.md`

**问题**

Checklist 覆盖了 CombatTrace、Boss、Long Run、Status Matrix，但完全没有铭文系统的验证项。铭文已进入 Phase 3 build 轴（4.4 节第 257–261 行），应有对应门禁。

**修复建议**

在 "2.4 Status Matrix" 之后增加 "2.5 Inscription Verification"：

```
### 2.5 Inscription Verification

1. 验证装备第 5 个铭文时被系统拒绝
2. 验证同类装备第 3 个时被拒绝
3. 验证铭文冷却独立于天赋冷却
4. 验证净化类铭文遵循默认净化策略（硬控优先 → LONGEST_REMAINING）
5. 验证铭文在非战斗和战斗状态均可正常使用
```

---

### 3.5 🟡【R3-P2-06】风险与止损缺少 DSL 规格漂移场景

**位置**

Phase 3 主文档第 8 节（第 579–584 行）

**问题**

当前只有 4 条风险项。缺少一个高概率风险：AIProfile DSL 在 W4 实现过程中规格发生大幅变动，导致已基于 DSL 起稿的 Boss/精英数据需要返工。

**修复建议**

增加第 5 条：

> 如果 AIProfile DSL 在 W4a 实现中规格发生超出最小冻结的扩展，必须先冻结新规格并通知 Content Lane 暂停 Boss/精英数据起稿，直到新规格经过至少 2 个精英怪的端到端验证。

---

## 4. 新发现的问题

### 4.1 🟡【R3-P2-07】补充设计文档 HASTE/SLOW 公式措辞与堆叠分类存在张力

**位置**

- Phase 3 主文档第 179 行
- 补充设计文档第 2847 行

**问题**

`effectiveSpeed = baseSpeed + sum(hasteModifiers) + sum(slowModifiers)` 中的 `sum()` 暗示多个同类修正可以共存。但 SLOW 被归入"不叠加、刷新持续时间"类别，意味着同时只有一个 SLOW 生效。

如果确实不叠加，`sum()` 是误导性措辞（实际只有一个值）。如果允许叠加，则 SLOW 不应在"不叠加"类别。

**修复建议**

如果 HASTE/SLOW 都是"不叠加、刷新"（推荐），将公式简化为：

```
effectiveSpeed = baseSpeed + hasteModifier + slowModifier
```

去掉 `sum()` 以消除歧义。

---

### 4.2 🟡【R3-P2-08】"通关任意难度"表述仍可优化

**位置**

Phase 3 主文档第 270 行

**问题**

当前写法："'通关任意难度'的技术定义固定为：击败深渊之心并生成 run summary。"

虽然给出了技术定义，但"通关任意难度"在 Phase 3 只有 Normal 一种难度的背景下仍然是一个会引起困惑的短语（看起来像是有多种难度可选）。

**修复建议**

改为更直接的表述：

> 通关的技术定义固定为：击败 `深渊之心` 并生成 run summary。Phase 3 只有 Normal 难度；若后续阶段引入更多难度选项，任意难度通关均满足解锁条件。

---

## 5. 总结：本轮行动清单

| 优先级 | 行动项 | 涉及文件 |
|---|---|---|
| 🟠 P1 | ROOT 补入两份文档堆叠矩阵"不叠加、刷新"类别 | Phase 3 主文档 4.2 + 补充设计 8.1.2 |
| 🟠 P1 | STEALTH / TAUNT 与 AI 决策的交互合同补入 DSL 规格 | Phase 3 主文档 4.5 |
| 🟠 P1 | Boss telegraph 伤害阈值交叉引用补入 Phase 3 主文档 | Phase 3 主文档 4.5 |
| 🟡 P2 | HASTE 补入堆叠分类 + 公式去掉 `sum()` 歧义 | Phase 3 主文档 4.2 + 补充设计 8.1.3 |
| 🟡 P2 | Zone 描述统一英文 ID + 中文标注 | Phase 3 主文档 4.6 |
| 🟡 P2 | Zone 等级范围表补入 Phase 3 主文档 | Phase 3 主文档 4.6 |
| 🟡 P2 | longRunLab full 模式补入量化验收参考 | Phase 3 主文档 7 |
| 🟡 P2 | Verification Checklist 补入铭文验证段 | Phase 3 Checklist |
| 🟡 P2 | 风险与止损增加 DSL 规格漂移场景 | Phase 3 主文档 8 |
| 🟡 P2 | HASTE/SLOW 公式去 sum() 歧义 | 两份文档 |
| 🟡 P2 | "通关任意难度"措辞优化 | Phase 3 主文档 4.4 |

---

## 6. 评审结论

**文档质量已显著提升**。R2 报告中的两个 P0 跨文档冲突（铭文清单、respec 规则）已完全修复，10 个 P1 级问题中有 8 个已落地。两份文档之间的一致性从"严重分裂"提升到了"基本对齐"。

本轮**没有 P0 阻塞项**。

剩余的 3 个 P1 和 8 个 P2 多为**局部补全**性质——ROOT 缺堆叠分类、STEALTH/TAUNT 缺 AI 交互说明、telegraph 缺交叉引用、以及一批可在短时间内收口的细节打磨。这些不会阻塞 W1（战斗公式）的启动，但必须在 **W2（状态生命周期）和 W4（AIProfile DSL）启动前**全部完成。

一句话总结：**文档体系已具备执行底稿的质量。当前需要的是一轮精准的局部补全，而非架构级调整。完成本轮行动清单后，Phase 3 可以正式进入代码落地阶段。**

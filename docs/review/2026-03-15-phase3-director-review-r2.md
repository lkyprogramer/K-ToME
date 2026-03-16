# Phase 3 深度评审报告（第二轮）

> **日期**：2026-03-15
> **评审者视角**：资深游戏设计与开发总监
> **评审对象**：修订后的 `docs/phase3/` 全套文档 + `docs/2026-03-13-core-systems-design-and-phase-supplements.md` 的 Phase 3 相关修订
> **本轮定位**：上一轮 P0 阻塞项已全部得到实质性修复。本轮聚焦**两份文档之间的内部一致性、状态规则完备性、DSL 规格冲突**以及可直接落地的细节打磨。
> **结论等级**：🔴 P0 阻塞 / 🟠 P1 重要 / 🟡 P2 建议 / ✅ 已修复

---

## 0. 上一轮修复确认

| 上轮 ID | 问题 | 状态 |
|---|---|---|
| P0-ARCH-01 | 工作包串行 → 并行四线 | ✅ 第 3.3 节与第 5 节已引入四条 lane 并行说明，W2/W4/W6 给出了可并行拆分口径 |
| P0-ARCH-02 | AIProfile DSL 无规格 | ✅ 第 4.5 节已补入 YAML 示例和 BossPhaseDef 覆盖清单 |
| P0-ARCH-03 | 局间合同空白 | ✅ 第 4.6 节已补入最小局间合同、通关定义、持久化边界 |
| P0-SYS-01 | 状态 P2/P3 边界模糊 | ✅ 第 4.2 节已分两条明确列出升级状态和新增状态 |
| P1-SYS-02 | HASTE/SLOW 量级 | ✅ 已切换为 speedModifier 净值模型 |
| P1-SYS-03 | SHIELD/REGEN 比较维度 | ✅ 已定义 magnitude 优先 + remainingTurns 次级 |
| P1-SYS-04 | Boss Phase 技术规格 | ✅ BossPhaseDef 已给出切换条件/时机/副作用四要素 |
| P1-SYS-05 | Shadowblade/Warden 最小设计 | ✅ 已各有 5 项冻结 |
| P1-SYS-06 | 种族数量矛盾 | ✅ 已点名 human/elf/dwarf |
| P1-SYS-07 | 铭文无内容 | ✅ 已列出 8 个基础条目 |
| P1-SYS-08 | 经济循环缺设计 | ✅ 已补入最小经济模型 |
| P1-TECH-01 | 公式写法差异 | ✅ 说明 1 已标注"等价展开，以补充设计文档为权威" |
| P1-TECH-02 | Golden 版本化 | ✅ 已补入 phase:P3 标记要求 |
| 其余 P2 级 | 各项细节 | ✅ 大部分已吸收 |

---

## 1. 文档内部一致性问题（跨文件冲突）

### 1.1 🔴【R2-P0-01】铭文清单在两份文档中完全不同

**位置**

- Phase 3 主文档第 4.4 节（第 284–293 行）列出 8 个铭文：

  `healing_seal / warding_seal / flame_brand / frost_brand / purge_mark / phase_step / iron_skin_mark / bane_sigil`

- 核心系统设计文档第 8.2.3 节（第 2929–2939 行）列出 9 个铭文：

  `healing_light / healing_surge / healing_miracle / phase_door / controlled_phase / iron_shield / diamond_shield / purge / greater_purge`

**问题**

两个清单**零 ID 重叠**，功能定位也不同：Phase 3 主文档以"覆盖各元素 + 净化 + 位移 + 驱邪"为导向，核心系统文档以"分阶治疗 + 分阶护盾 + 传送 + 净化"为导向并引入了 Tier 分级。同一组设计权威文档给出两套互不兼容的内容清单，实现时必然出现分歧。

**修复建议**

选定一份为权威，另一份删除或标注为"已废弃"。建议以核心系统文档的 9 条 + Tier 体系为权威（设计更完整），Phase 3 主文档改为引用并声明"首批铭文清单见补充设计文档第 8.2.3 节"。如果要保留 Phase 3 主文档版本中 `flame_brand`、`frost_brand`、`bane_sigil` 这类攻击型/元素型铭文，则应合并到核心系统文档的 `OFFENSE` 类别中，统一管理。

---

### 1.2 🔴【R2-P0-02】Respec 规则直接矛盾

**位置**

- Phase 3 主文档第 4.3 节（第 213–216 行）：

  > respec 第一版固定为：仅允许在非战斗状态触发；**本阶段默认免费**；清空当前职业树已投点数并全额返还

- 核心系统设计文档第 6.2.2 节（第 2390 行）：

  > 洗点：**每局免费 1 次，之后需要消耗稀有道具**

一个是"完全免费"，一个是"首次免费，后续收费"。这会直接影响构筑试错体验和经济系统设计。

**修复建议**

Phase 3 以"构筑验证"为首要目标，建议采用 Phase 3 主文档的"本阶段默认免费"方案，并在核心系统文档中将第 2390 行改为"Phase 3 默认免费，Phase 4 或后续阶段可引入消耗限制"。

---

### 1.3 🟠【R2-P1-01】AIProfile DSL 存在两套不兼容的 schema

**位置**

- Phase 3 主文档第 4.5 节（第 315–344 行）的 DSL 示例使用：

  ```yaml
  ai_profile:
    phases:
      - actions:
          - type: "USE_ABILITY"
            weight: 70        # 概率权重
  ```

- 核心系统设计文档第 7.3.2 节（第 2472–2535 行）的 Layer 2 脚本使用：

  ```yaml
  monster:
    ai:
      behaviors:
        - priority: 80        # 确定性优先级
          condition: { type: AND, ... }
          action: { type: USE_TALENT }
  ```

两套 schema 的**决策模型完全不同**：Phase 3 主文档用 `weight`（概率加权随机选择），核心系统文档用 `priority`（确定性优先级匹配，从高到低检查条件，执行第一个满足者）。这不是命名差异，是两种本质不同的 AI 决策策略。

**后果**

- 普通怪用优先级 + 条件匹配（可预测、可调试、适合 Golden Seed 回归）
- Boss 用概率加权（增加不可预测性）

如果这是刻意的双模型设计，需要显式说明。如果不是，需要统一。

**修复建议**

建议的统一方案：
1. 普通怪/精英怪统一使用核心系统文档的 `priority + condition` 确定性模型（Layer 2 主路径）。
2. Boss `phases` 内部的 `actions` 可以使用 `weight` 加权随机，但必须在 DSL 规格中显式声明"Boss phase 内的行为选择走加权随机而非确定性优先级"。
3. 在 Phase 3 主文档的 DSL 示例中补一句说明，解释这里用 `weight` 的原因以及与核心系统文档 Layer 2 `priority` 模型的关系。

---

### 1.4 🟠【R2-P1-02】净化默认策略矛盾

**位置**

- Phase 3 主文档第 179 行：

  > 否则清除**剩余持续时间最长**的负面状态

- 核心系统设计文档第 2853 行：

  ```kotlin
  val priorityOrder: CleanseOrder = CleanseOrder.MOST_RECENT
  ```

  `MOST_RECENT`（最近施加的优先）是代码级默认值，而 Phase 3 主文档的设计意图是 `LONGEST_REMAINING`。

**修复建议**

将核心系统文档第 2853 行的默认值改为 `CleanseOrder.LONGEST_REMAINING` 以与 Phase 3 的设计意图对齐。或者在 Phase 3 主文档中增加说明：净化配置默认值在骨架代码中为 `MOST_RECENT`，Phase 3 的推荐策略覆盖为 `LONGEST_REMAINING`。

---

### 1.5 🟠【R2-P1-03】不可净化效果集合不一致

**位置**

- Phase 3 主文档第 180–183 行：不可净化 = `INVULNERABLE / KNOCKBACK / Boss phase 锁定`
- 核心系统设计文档第 2872 行：不可净化 = `KNOCKBACK / INVULNERABLE / STEALTH`

Phase 3 主文档漏掉了 `STEALTH`，核心系统文档漏掉了"Boss phase 锁定"。

**修复建议**

统一为：`KNOCKBACK / INVULNERABLE / STEALTH / Boss phase 锁定状态`。STEALTH 确实不应该被净化（它是增益而非减益，且有独立的解除机制）。两份文档都需要更新。

---

## 2. 状态规则完备性问题

### 2.1 🟠【R2-P1-04】GUARD 和 MARKED 未出现在堆叠矩阵中

**问题**

Phase 3 第 4.2 节的堆叠矩阵分 5 类规则，覆盖了 `STUN/SLOW/FREEZE/SILENCE/BLEED/BURN/POISON/ARMOR_BREAK/SHIELD/REGEN` 和互斥组合。

但 `GUARD` 和 `MARKED` 被列入"P2 升级为 P3 正式主线"（第 146 行），却**没有出现在任何堆叠规则类别中**。

核心系统文档（第 2829–2830 行）明确定义：
- `GUARD`：取较强值（与 SHIELD/REGEN 同类）
- `MARKED`：不可叠加，刷新持续时间

**修复建议**

在 Phase 3 主文档的堆叠矩阵中补入：
- 第 1 类（不叠加、刷新持续时间）增加 `MARKED`
- 第 4 类（取较强值）增加 `GUARD`

---

### 2.2 🟠【R2-P1-05】P3 新增的 7 个状态缺少堆叠规则定义

**问题**

Phase 3 新增 `BANE / CURSE / WEAKEN / OVERCHARGE / INVULNERABLE / STEALTH / TAUNT`，但堆叠矩阵完全没有覆盖它们。实现者需要逐一回答：

| 状态 | 需要回答的问题 |
|---|---|
| `BANE` | 不同来源的 BANE 叠加？还是刷新？多个 BANE 在消耗时全部触发还是只触发一个？ |
| `CURSE` | 两个 CURSE 是否叠加降低属性？全局 debuff 是否有下限？ |
| `WEAKEN` | 与 CURSE 是否独立生效？WEAKEN + CURSE 是否可以让目标攻击力降到 0？ |
| `OVERCHARGE` | 多个来源的 OVERCHARGE 叠加增伤？还是取较强？ |
| `INVULNERABLE` | 多个来源（不死鸟 + Boss phase）如何共存？持续时间取较长？ |
| `STEALTH` | 多次进入隐身是否刷新？隐身期间再施放隐身技能是否延长？ |
| `TAUNT` | 被两个来源嘲讽时攻击谁？后来者覆盖？还是距离优先？ |

**修复建议**

在堆叠矩阵中补充第 6 类或将新状态分类到现有类别：

```
不叠加、刷新持续时间：增加 BANE / CURSE / WEAKEN / OVERCHARGE / STEALTH
取较强值：增加 INVULNERABLE
后来者覆盖：TAUNT（被嘲讽目标只有一个）
```

并在补充说明中明确：
- `CURSE + WEAKEN` 独立生效，总属性削减不设显式下限（由属性自然下限 0 兜底）
- `OVERCHARGE` 不叠加，刷新持续时间（因为效果是"下次闪电伤害 +25%"，触发一次即消耗）
- `TAUNT` 后来者覆盖前一个嘲讽源

---

### 2.3 🟡【R2-P2-01】DoT tick 时机未定义

**问题**

BLEED/BURN/POISON 的 tick 发生在什么时候？

- **方案 A**：目标回合开始时 tick（ToME 原版方案）
- **方案 B**：施加者回合开始时 tick
- **方案 C**：全局回合边界 tick

这会影响：
- Golden Seed 回归：tick 顺序会改变 HP 快照
- Boss 多 phase 状态交互：Boss 进入新 phase 清除状态时，已 tick 的 DoT 是否算
- 眩晕时 DoT 是否跳过

**建议**

明确为方案 A（目标回合开始时 tick）：
> DoT 在受影响实体的回合开始时、行动前结算。若该实体被 STUN/FREEZE，DoT 仍然 tick。

---

## 3. 结构与编辑质量问题

### 3.1 🟠【R2-P1-06】第 4.2 节有重复编号

**位置**

第 147–149 行：

```
2. 下列状态在 Phase 3 新增...
2. sustain、mark、ward、zone effect...
3. 清理、驱散、免疫和持续 tick...
```

第二条应编号为 `3`，第三条应为 `4`。

---

### 3.2 🟠【R2-P1-07】EQUILIBRIUM 规格放错了位置

**问题**

Spellblade 的 EQUILIBRIUM 边界定义放在第 4.6 节"世界分支与长局结构"中（第 425–431 行），与该节主题（zone、经济、affix）完全无关。

**修复建议**

移至第 4.4 节"职业与 build 深度"中 Shadowblade/Warden 最小设计稿之后，或在 Spellblade 的对应位置。在 4.6 节原位置改为一条交叉引用。

---

### 3.3 🟡【R2-P2-02】Zone 命名中英混用

**问题**

连接关系图（第 411–423 行）使用英文 ID（`elven_ruins`、`bandit_camp`、`molten_core`），但正文第 405–407 行的描述性文字使用中文（`精灵遗迹`、`熔岩核心`、`盗贼营地`、`地下河`、`水晶洞穴`、`深渊神殿`、`深渊之心`）。

在数据驱动设计中，应该统一使用稳定的英文 `id`，中文只出现在 `nameKey` 引用中。否则后续实现者会在 YAML 和代码中混用中英。

**修复建议**

将第 405–407 行的中文名改为英文 ID 一致形式，必要时加中文标注：

```
1. shattered_outpost -> greenwood_fringe -> deep_iron_pit -> grey_gate_depths（主线）
2. elven_ruins / molten_core / bandit_camp（可选支线）
3. underground_river -> crystal_cavern -> abyssal_temple -> abyssal_heart（P3 扩展终线）
```

---

### 3.4 🟡【R2-P2-03】Zone 等级范围未在 Phase 3 文档中出现

**问题**

核心系统设计文档第 9.2.3 节定义了 Phase 3 扩展 zone 的等级范围：

| Zone | 等级范围 |
|---|---|
| underground_river | Lv10–12 |
| crystal_cavern | Lv11–13 |
| abyssal_temple | Lv12–15 |
| abyssal_heart | Lv15 |

但 Phase 3 主文档只给出了连接拓扑图，完全没有等级范围。这对内容设计者、怪物数值配表、掉落校准都是必要信息。

**修复建议**

在连接关系图旁或后方补入一张等级范围表，至少覆盖 Phase 3 扩展的 4 个新 zone。

---

### 3.5 🟡【R2-P2-04】"通关任意难度"表述可能引起误解

**问题**

Phase 3 的进阶职业解锁条件写"通关任意难度"，但核心系统文档明确 Phase 2 只有 `Normal` 一种正式难度。Phase 3 也没有定义额外难度。因此"通关任意难度"在 Phase 3 实际就是"通关 Normal"。

**修复建议**

改为"通关（当前阶段只有 Normal 一种难度；如果后续阶段扩展难度选项，解锁条件仍为任意难度即可）"或直接简化为"使用该基础职业完成一次通关"。

---

## 4. 遗漏与补强建议

### 4.1 🟠【R2-P1-08】AIProfile DSL 缺少 STEALTH 和 TAUNT 的处理说明

**问题**

STEALTH 和 TAUNT 是 Phase 3 新增的两个直接影响 AI 决策的状态：
- 玩家进入 STEALTH 后，AI 应如何行动？切换到巡逻？搜索最后已知位置？
- AI 被 TAUNT 后，是否强制攻击嘲讽源？如果嘲讽源不在攻击范围呢？

Phase 3 主文档的 DSL 示例没有展示任何与这两个状态相关的条件或行为。

**修复建议**

在 DSL 规格说明中补入：
- `STEALTH` 处理：目标进入 STEALTH 后，AI 应移动到最后已知位置 (`useLastKnownPosition: true` 已有此字段)。到达后若仍未发现目标，切换到搜索/巡逻行为。Boss 应有显式的"搜索模式"phase 或 fallback。
- `TAUNT` 处理：被 TAUNT 的 AI 在 TAUNT 持续期间，强制将攻击目标设为嘲讽源。若嘲讽源不在攻击范围，必须移动靠近而非攻击其他目标。

---

### 4.2 🟠【R2-P1-09】Boss telegraph 伤害阈值缺少显式交叉引用

**问题**

核心系统设计文档定义了关键规则：
> 伤害超过玩家最大 HP 30% 的 Boss 技能必须有至少 1 回合 telegraph；超过 50% 的必须 2 回合。

Phase 3 主文档只说"Boss 高伤技能必须有 telegraph"，没有重复这些具体阈值，也没有交叉引用。当 W4 实现者只读 Phase 3 主文档时，可能遗漏 30%/50% 规则。

**修复建议**

在 Phase 3 主文档 4.5 节冻结口径的第 2 条后补入：
> Boss 技能 telegraph 的伤害阈值与回合数见补充设计文档 6.1.3 节：≥30% 最大 HP → ≥1 回合预览，≥50% → ≥2 回合。

---

### 4.3 🟡【R2-P2-05】longRunLab `full` 模式缺少量化验收标准

**问题**

出口门禁第 4 条要求"`full` 模式的死亡分布和平均时长落在预期范围"，但"预期范围"没有定义。

**建议**

补入第一版量化参考（可在后续迭代调整）：
- headless run 的等价回合数上限：建议 3000 回合（约对应 4–6 小时人类游玩）
- 4 基础职业 × 3 种族的 12 个组合中，至少 8 个可到达 `abyssal_temple`
- 死亡分布：50% 以上的 run 应在 `deep_iron_pit` 之后死亡（而非在前两个 zone 就全灭，说明难度曲线太陡）

---

### 4.4 🟡【R2-P2-06】Verification Checklist 缺少铭文相关测试

**问题**

Verification Checklist 覆盖了 CombatTrace、Boss、Long Run、Status Matrix、断点成长 UX，但完全没有提及铭文系统的验证：
- 铭文的装备/卸载是否正确
- 铭文冷却是否独立于天赋冷却
- 同类铭文 2 上限是否生效
- 铭文与净化/护盾状态的交互

**修复建议**

在 Verification Checklist 的 "2.4 Status Matrix" 之后增加 "2.5 Inscription Verification"：
1. 验证装备 5 个铭文时第 5 个被拒绝
2. 验证同类 3 个被拒绝
3. 验证铭文 CD 独立于天赋 CD
4. 验证 `purge_mark` / `purge` 铭文净化遵循默认策略

---

### 4.5 🟡【R2-P2-07】风险与止损缺少"DSL 规格不稳定"场景

**问题**

第 8 节风险与止损列出了 4 项，但缺少一个很可能发生的风险：**AIProfile DSL 在 W4 实现过程中规格发生大幅变动**，导致已经基于 DSL 起稿的 Boss/精英数据需要返工。

**建议**

增加第 5 条：
> 如果 AIProfile DSL 规格在 W4a 实现中发生超出最小规格的扩展，必须先冻结新规格并通知 Content Lane 暂停 Boss/精英数据起稿，直到新规格经过至少 2 个精英怪的验证。

---

## 5. 总结：本轮行动清单

| 优先级 | 行动项 | 涉及文件 |
|---|---|---|
| 🔴 P0 | 统一铭文清单，选定一份为权威并消除另一份 | Phase 3 主文档 + 核心系统设计 |
| 🔴 P0 | 统一 respec 规则：Phase 3 默认免费 vs 首次免费+后续收费 | Phase 3 主文档 + 核心系统设计 6.2.2 |
| 🟠 P1 | DSL 双 schema 问题：明确普通怪 `priority` vs Boss `weight` 是否为双模型设计，并加文字说明 | Phase 3 主文档 4.5 + 核心系统设计 7.3 |
| 🟠 P1 | 净化默认策略对齐：`LONGEST_REMAINING` vs `MOST_RECENT` | Phase 3 主文档 + 核心系统设计 8.1.4 |
| 🟠 P1 | 不可净化集合统一：补入 `STEALTH`，补入 Boss phase 锁定 | 两份文档均需更新 |
| 🟠 P1 | GUARD / MARKED 补入堆叠矩阵 | Phase 3 主文档 4.2 |
| 🟠 P1 | P3 新增 7 个状态补全堆叠规则 | Phase 3 主文档 4.2 |
| 🟠 P1 | 修复 4.2 节重复编号（两个 `2.`） | Phase 3 主文档 |
| 🟠 P1 | EQUILIBRIUM 规格移至 4.4 节 | Phase 3 主文档 |
| 🟠 P1 | AIProfile DSL 补入 STEALTH/TAUNT 处理说明 | Phase 3 主文档 4.5 |
| 🟠 P1 | Boss telegraph 伤害阈值交叉引用 | Phase 3 主文档 4.5 |
| 🟡 P2 | 明确 DoT tick 时机（建议：目标回合开始时） | Phase 3 主文档 4.2 |
| 🟡 P2 | Zone 命名统一为英文 ID | Phase 3 主文档 4.6 |
| 🟡 P2 | 补入 Phase 3 扩展 zone 的等级范围表 | Phase 3 主文档 4.6 |
| 🟡 P2 | "通关任意难度"措辞澄清 | Phase 3 主文档 4.4 |
| 🟡 P2 | longRunLab full 模式补入量化参考 | Phase 3 主文档 7 + Checklist |
| 🟡 P2 | Verification Checklist 补入铭文验证 | Phase 3 Checklist |
| 🟡 P2 | 风险与止损增加 DSL 规格不稳定场景 | Phase 3 主文档 8 |

---

## 6. 评审结论

这一轮修订的质量很高——上一轮的 **全部 P0 阻塞项和绝大部分 P1 项都已经得到实质性修复**，四条 lane 并行、AIProfile DSL 最小规格、局间合同、状态边界厘清等核心问题已经有了清晰可落地的文字。

本轮发现的最高风险集中在**两份文档之间的内部一致性**上——铭文清单完全不同、respec 规则矛盾、DSL schema 冲突、净化策略默认值不一致。这些问题的根因是同一系统在两份文档中各自独立演进，需要一次性做一轮"跨文件对齐扫描"来收口。

其次是**状态堆叠矩阵的完备性**。Phase 3 新增了 7 个状态，但堆叠规则完全空白。这不是可以推迟到实现时再决定的事情——堆叠语义会影响 Golden Seed 基线、AI 行为测试和 Boss 设计，必须在 W2 启动前冻结。

一句话总结：**文档的骨架和工程纪律已经就位，现在需要的是一次跨文件的一致性收口 + 状态规则补全**。完成这两步后，Phase 3 的文档体系就可以作为可靠的实现底稿交付执行。

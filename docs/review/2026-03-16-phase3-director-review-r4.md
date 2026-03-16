# Phase 3 深度评审报告（第四轮）

> **日期**：2026-03-16
> **评审者视角**：资深游戏设计与开发总监
> **评审对象**：修订后的三份文档
> **本轮定位**：R3 的所有 P1/P2 项已全部落地。本轮转向**实现级深度挖掘**——zone 拓扑对齐、DSL schema 可落地性、Boss 内容清单、Checklist 结构准确性等前几轮未触及的维度。
> **结论等级**：🔴 P0 阻塞 / 🟠 P1 重要 / 🟡 P2 建议 / ✅ 已修复

---

## 0. R3 修复确认

| R3 ID | 问题 | 状态 |
|---|---|---|
| R3-P1-01 | ROOT 缺失堆叠矩阵 | ✅ 两份文档均补入"不叠加、刷新"类别 |
| R3-P1-02 | STEALTH / TAUNT 缺 AI 交互合同 | ✅ Phase 3 主文档 4.5 节补入 5 条冻结规则；补充设计文档 7.5.2 同步 |
| R3-P1-03 | Boss telegraph 伤害阈值缺交叉引用 | ✅ 第 330 行直接写明阈值（≥30% → 1 回合，≥50% → 2 回合） |
| R3-P2-01 | HASTE 缺堆叠分类 | ✅ 补入"不叠加、刷新"；HASTE/SLOW 公式改为单值模型 |
| R3-P2-02 | Zone 中英混用 | ✅ 统一为英文 ID + 中文标注 |
| R3-P2-03 | Zone 等级范围表缺失 | ✅ 补入 8 个 zone 的等级范围表 |
| R3-P2-04 | longRunLab 量化标准 | ✅ 6.2 节和出口门禁均补入（3000 回合、8/12 组合、50% 分布） |
| R3-P2-05 | Checklist 缺铭文验证 | ✅ 新增 2.5 Inscription Verification（5 条） |
| R3-P2-06 | 风险与止损缺 DSL 漂移 | ✅ 第 615 行补入第 5 条 |
| R3-P2-07 | HASTE/SLOW 公式 sum 歧义 | ✅ 两份文档均改为 `hasteModifier - slowModifier` |
| R3-P2-08 | "通关任意难度"措辞 | ✅ 改为"Phase 3 只有 Normal 难度"明确表述；补充设计文档 9.2.2 同步 |

**R1–R3 的所有 P0 和 P1 项至此全部关闭。**

---

## 1. Zone 拓扑与内容清单

### 1.1 🟠【R4-P1-01】bandit_camp 的父节点在两份文档中不同

**位置**

- Phase 3 主文档第 443–445 行的连接关系图：

  ```
  shattered_outpost
    -> greenwood_fringe
      -> elven_ruins (optional)
      -> bandit_camp (optional)     ← greenwood_fringe 的子节点
  ```

- 补充设计文档第 3160–3168 行的世界分支结构：

  ```
  破碎前哨 (Lv1-4)
      ├── 绿林边缘 (Lv3-6)
      │       ├── 精灵遗迹 [可选]
      │       └── 深铁矿坑
      └── 盗贼营地 [可选] (Lv3-5)  ← 破碎前哨 的子节点
  ```

**问题**

`bandit_camp` 是从 `shattered_outpost` 直接分支还是从 `greenwood_fringe` 分支？这决定了：
- 玩家多早能接触这个可选区域（出生后 vs 完成第一个主线 zone 后）
- 等级适配（Lv3-5 更接近 shattered_outpost 的尾段，放在 greenwood_fringe 子节点下等级可能偏低）
- AI 巡逻/追击的跨 zone 边界逻辑

**修复建议**

从设计角度看，`bandit_camp` 的 Lv3-5 等级范围与 `shattered_outpost`（Lv1-4）的尾段衔接更自然（补充设计文档的拓扑），而放在 `greenwood_fringe`（Lv3-6）下也说得通。需要选定一个并统一。建议以 Phase 3 主文档为权威（因为它是执行底稿），同时更新补充设计文档 9.2.3 的拓扑图。

---

### 1.2 🟡【R4-P2-01】Phase 3 zone 等级范围表缺少 3 个可选 zone

**位置**

Phase 3 主文档第 457–466 行的 zone 等级范围表只列出了 8 个 zone（主线 + crystal_cavern），缺少：

| Zone ID | 中文名 | 等级范围（补充设计文档 9.2.3） |
|---|---|---|
| `elven_ruins` | 精灵遗迹 | Lv5-7 |
| `bandit_camp` | 盗贼营地 | Lv3-5 |
| `molten_core` | 熔岩核心 | Lv7-9 |

**问题**

这些可选 zone 在连接关系图中已出现，但等级范围表未覆盖。内容设计者配怪、配掉落时缺少参考。

**修复建议**

将 3 个可选 zone 补入等级范围表。可以用分隔或缩进标注为可选。

---

### 1.3 🟡【R4-P2-02】Phase 3 Boss 名册未列出

**位置**

Phase 3 主文档全文

**问题**

Phase 3 在多处提到"至少 2 个不同类型的 Boss"（出口门禁第 5 条），但**从未点名任何一个 Boss**。目前只从连接关系图可以推断 `abyssal_heart` 有最终 Boss，从补充设计文档 Phase 2 zone specs（第 3109–3110 行）可知 `deep_iron_pit` 有熔岩巨人、`grey_gate_depths` 有地牢主宰。

Phase 3 扩展了 4 个新 zone，其中 `abyssal_heart` 显然需要最终 Boss。但整个 Phase 3 的 Boss 清单——有几个、在哪些 zone、哪些是新增的、哪些继承自 Phase 2——没有明确汇总。

**修复建议**

在 4.6 节或 4.5 节补入 Phase 3 Boss 最小名册：

```
Phase 3 Boss 清单（最低交付）：

| Boss ID | Zone | 类型 | 来源 |
|---|---|---|---|
| molten_giant | deep_iron_pit | 中间 Boss | 继承 Phase 2 |
| dungeon_lord | grey_gate_depths | 区域 Boss | 继承 Phase 2 |
| abyssal_guardian | abyssal_temple 或 abyssal_heart | 最终 Boss | Phase 3 新增 |

bossHarness 至少覆盖上述 Boss 中的 2 个。
```

---

## 2. DSL Schema 可落地性

### 2.1 🟠【R4-P1-02】Phase 3 的 AI DSL YAML 示例与补充设计文档的 Boss Schema 结构不兼容

**位置**

- Phase 3 主文档第 337–366 行的 DSL 示例：

  ```yaml
  ai_profile:
    id: "iron_golem_standard"
    phases:
      - id: "phase_full"
        condition: "hp_percent > 0.50"    # 字符串表达式
        actions:
          - type: "USE_ABILITY"
            weight: 70                     # 内联 action + weight
        onEnterTelegraph:                  # 内联 telegraph 对象
          shape: "CIRCLE"
  ```

- 补充设计文档第 2621–2670 行的 Boss 定义 Schema：

  ```yaml
  boss:
    id: "dungeon_lord"
    phases:
      - id: "phase_1"
        hpThreshold: 1.0                  # 数值字段
        hpEnd: 0.70                        # 数值字段
        behaviorScript: "dungeon_lord_phase1"  # 外部脚本引用
        onEnter:                           # 事件动作列表
          - type: TELEGRAPH
            talentId: "ground_slam"
  ```

**问题**

这两套 YAML 结构在以下维度存在实质性差异：

| 维度 | Phase 3 示例 | 补充设计文档 Schema |
|---|---|---|
| phase 切换条件 | `condition: "hp_percent > 0.50"` 字符串 | `hpThreshold` + `hpEnd` 数值对 |
| 行为定义 | 内联 `actions[]` + `weight` | 外部 `behaviorScript` 引用 |
| telegraph | `onEnterTelegraph` 内联对象 | `onEnter` 动作列表中的 `type: TELEGRAPH` |
| 语义层级 | `ai_profile`（AI 行为） | `boss`（Boss 遭遇定义） |

**后果**

W4 实现者面对两份互不兼容的 YAML 结构，无法确定：
1. Boss encounter 是一层定义（`ai_profile` 包办遭遇+行为），还是两层定义（`boss` 定义遭遇框架 + `behaviorScript` 引用独立 AI 脚本）？
2. Phase 切换用字符串表达式解析器还是结构化字段？
3. Telegraph 用内联对象还是 onEnter 事件？

**修复建议**

需要明确 **Boss encounter 是两层结构**：

1. **`BossEncounter` 层**（补充设计文档的 `boss` schema）：定义 Boss 元信息、phase 列表、切换阈值、onEnter 事件。这是 Boss 的"剧本"。
2. **`AIProfile` 层**（Phase 3 的 `ai_profile` schema）：每个 phase 引用的行为脚本。Boss phase 内部的 `actions[].weight` 在此层。

在 Phase 3 主文档的 DSL 示例中：
- 要么明确标注"此示例展示的是 AIProfile 层，BossEncounter 层的结构见补充设计文档 7.4.1"
- 要么将示例改写为两层结构的联合示例，展示 BossEncounter 如何引用 AIProfile

---

## 3. Checklist 结构问题

### 3.1 🟠【R4-P1-03】Long Run Lab 的量化门槛被错误归入"两种模式都要检查"

**位置**

Checklist 第 59–64 行：

```
3. 两种模式都要检查：
   - 无卡死、无不可达主线
   - 长局主支线和可选支线都可达
   - headless run 的等价回合数不超过 3000
   - 4 基础职业 × 3 种族的 12 个组合中，至少 8 个能到达 abyssal_temple
   - 50% 以上失败 run 发生在 deep_iron_pit 之后
```

**问题**

`smoke` 模式的定义（第 53–55 行）是"固定职业、种族、profile、world seed，验证 run 能到达终局状态"——这是单组合、单 seed 的验证。而第 63–64 行的"12 个组合中至少 8 个"和"50% 以上失败 run"显然只对 `full` 模式有意义。

将 full-only 的统计指标放在"两种模式都要检查"下，会导致 CI 实现时误解——要么在 smoke 模式上跑不可能满足的矩阵，要么忽略这些条目。

**修复建议**

拆分为：

```
3. 两种模式共同检查项：
   - 无卡死、无不可达主线
   - 长局主支线和可选支线都可达
   - headless run 的等价回合数不超过 3000

4. full 模式额外检查项：
   - 4 基础职业 × 3 种族的 12 个组合中，至少 8 个能到达 abyssal_temple
   - 50% 以上失败 run 发生在 deep_iron_pit 之后
```

---

## 4. 其他深度发现

### 4.1 🟡【R4-P2-03】CombatPipeline 12 步缺少交叉引用

**位置**

Phase 3 主文档第 126 行

**问题**

"`CombatPipeline` 固定为 `12` 步有序管线，child trace、callback 优先级和 miss/cleanse/elemental interaction 的挂点必须保持可追踪。"

补充设计文档 3.2.3 节完整定义了这 12 步（步骤 1 命中判定 → 步骤 12 经验与掉落），但 Phase 3 主文档没有列出也没有交叉引用。W1 实现者如果只看 Phase 3 主文档，不知道这 12 步具体是什么。

**修复建议**

在第 126 行末尾补入交叉引用：

> 12 步管线的完整定义见补充设计文档 3.2.3 节。

---

### 4.2 🟡【R4-P2-04】补充设计文档有"灰门王座"Boss 子区域，Phase 3 拓扑图未提及

**位置**

- 补充设计文档第 3167 行：`灰门深窟 (Lv7-10)` → `灰门王座 [Boss] (Lv10)`
- Phase 3 连接关系图第 448 行：`grey_gate_depths` → `underground_river`

**问题**

补充设计文档将"灰门王座"作为 grey_gate_depths 内的 Boss 子区域单独列出。Phase 3 的拓扑图直接从 grey_gate_depths 连到 underground_river，没有这个子节点。

从游戏设计角度，grey_gate_depths 的 Boss 房（地牢主宰）在 Phase 2 是终局 Boss，在 Phase 3 变为中途 Boss。这个 Boss 战仍然存在，只是不再是终点。

Phase 3 不需要将 Boss 房列为独立 zone，但当前两份文档的拓扑图结构不同可能导致混淆。

**修复建议**

两种方案均可：
1. 在补充设计文档中移除"灰门王座"作为独立子节点，将其视为 grey_gate_depths 内部的 Boss 房间（与 Phase 3 拓扑对齐）
2. 在 Phase 3 拓扑图中加注释：`grey_gate_depths` 包含区域 Boss 房（继承 Phase 2 的"灰门王座"）

---

### 4.3 🟡【R4-P2-05】Phase 3 未定义可选 zone 的 Boss / 精英配置

**位置**

Phase 3 主文档 4.6 节

**问题**

可选 zone（elven_ruins、bandit_camp、molten_core、crystal_cavern）在拓扑图中出现，但没有任何关于这些 zone 内部应包含什么内容（Boss？精英？特殊机制？奖励？）的说明。

补充设计文档只在 Phase 2 zone specs（第 3103–3110 行）定义了前 4 个主线 zone 的怪物池和 Boss。Phase 3 扩展的 zone 和可选 zone 的内容定义完全空白。

**修复建议**

在 4.6 节 zone 等级范围表后补入最小内容期望：

```
可选 zone 最小内容约束：

1. 每个可选 zone 至少包含 1 个独有精英怪或独有奖励（否则没有探索动力）
2. 可选 zone 不应包含主线 Boss 或主线解锁道具
3. 可选 zone 的怪物池等级必须与其 zone 等级范围对齐
4. Phase 3 不要求可选 zone 有独立叙事任务，但必须有至少 1 个固定奖励节点
```

---

### 4.4 🟡【R4-P2-06】出口门禁的 coverage gate 缺少 `core.talent` 和 `core.world`

**位置**

Phase 3 主文档出口门禁第 7 条（第 604–607 行）

```
- core.combat >= 85%
- core.status >= 80%
- core.ai >= 75%
```

**问题**

6.1 节"必测模块"列出了 7 个模块，其中包含 `core.talent` 和 `core.world`。但出口门禁的 coverage gate 只覆盖了 3 个。`core.talent`（天赋树 V2、respec/rollback）和 `core.world`（zone 连接、经济循环）都是 Phase 3 的新增核心模块，缺少 coverage 门槛可能导致这些模块的测试覆盖率不受保护。

**修复建议**

补入 `core.talent` 和 `core.world` 的推荐门槛：

```
- core.combat >= 85%
- core.status >= 80%
- core.ai >= 75%
- core.talent >= 80%
- core.world >= 70%
```

---

### 4.5 🟡【R4-P2-07】Checklist 白盒验证缺少 STEALTH / TAUNT 场景

**位置**

Checklist 第 3 节 Manual White-Box Verification（第 92–100 行）

**问题**

Phase 3 在 4.5 节投入了 5 条 STEALTH/TAUNT AI 交互合同，但白盒验证完全没有覆盖这两个场景。当前白盒验证只涉及 telegraph、respec、进阶职业长局和双语术语，缺少：
- 用隐匿类职业（Rogue/Shadowblade）测试 AI 在玩家进入 STEALTH 后的行为
- 用有嘲讽技能的职业（Vanguard/Warden）测试 TAUNT 对 Boss/精英的强制转火

**修复建议**

在白盒验证中补入第 6 条：

```
6. 用 Rogue 的隐匿技能触发一次 STEALTH，确认：
   - AI 停止追踪并移动到最后已知位置
   - STEALTH 被 AoE 实际伤害打破后 AI 恢复追踪
7. 用 Vanguard 的嘲讽对精英使用 TAUNT，确认：
   - 精英在 TAUNT 期间只攻击嘲讽源
   - TAUNT 结束后恢复正常目标选择
```

---

## 5. 总结：本轮行动清单

| 优先级 | 行动项 | 涉及文件 |
|---|---|---|
| 🟠 P1 | bandit_camp 父节点统一（greenwood_fringe vs shattered_outpost） | Phase 3 主文档 4.6 + 补充设计 9.2.3 |
| 🟠 P1 | AI DSL 示例标注为 AIProfile 层，补入 BossEncounter 层的交叉引用，明确两层关系 | Phase 3 主文档 4.5 |
| 🟠 P1 | Checklist Long Run Lab 拆分 smoke/full 共同项与 full-only 项 | Checklist 2.3 |
| 🟡 P2 | 可选 zone 等级范围补入 Phase 3 zone 表 | Phase 3 主文档 4.6 |
| 🟡 P2 | Phase 3 Boss 最小名册列出 | Phase 3 主文档 4.5 或 4.6 |
| 🟡 P2 | CombatPipeline 12 步交叉引用 | Phase 3 主文档 4.1 |
| 🟡 P2 | "灰门王座" Boss 子区域拓扑对齐 | 补充设计 9.2.3 |
| 🟡 P2 | 可选 zone 最小内容约束 | Phase 3 主文档 4.6 |
| 🟡 P2 | 出口门禁 coverage gate 补入 core.talent / core.world | Phase 3 主文档 7 |
| 🟡 P2 | 白盒验证补入 STEALTH / TAUNT 场景 | Checklist 3 |

---

## 6. 评审结论

**R1–R3 的全部 P0 和 P1 项已关闭。本轮仍然没有 P0 阻塞项。**

本轮挖掘到的问题已经从"文档一致性"层面下沉到了**实现可落地性**层面：

1. **Zone 拓扑冲突**（bandit_camp 挂在哪个节点）是一个必须在内容起稿前确定的拓扑问题，不解决会导致 zone YAML 和 zone graph 代码分歧。
2. **Boss encounter 两层 schema 的关系**是 W4 启动前必须明确的架构决策——Phase 3 的 DSL 示例和补充设计文档的 Boss schema 是同一系统的不同层次，但当前读法容易误解为两套互斥方案。
3. **Checklist 的 smoke/full 分组**是一个 CI 实现时会直接遇到的歧义。

三个 P1 都是**在本轮不解决则 W4/W6 会卡实现**的问题，建议在进入代码阶段前完成。P2 级别的 7 项是对已有框架的精度补强，可以在对应工作包启动时顺手补入。

一句话总结：**Phase 3 的文档体系已经达到"高质量执行底稿"水平。剩余问题集中在拓扑对齐、schema 层次澄清和 checklist 精度三个点上，全部属于短时间可收口的局部调整。完成后可以正式启动 W1。**

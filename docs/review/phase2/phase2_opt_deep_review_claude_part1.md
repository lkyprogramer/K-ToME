# Phase2 优化方案完成态深度审查报告 — Part 1

> **审查对象**：K-ToME Phase2 全部设计文档 + 后续优化改造方案 `2026-03-21-phase2-post-review-optimization-pr-plan.md`
> **审查日期**：2026-03-22
> **审查视角**：资深 Roguelike 游戏设计总监 / 系统策划总监 / 玩法体验审查负责人
> **审查基线**：当前 `codex/p2-opt-stage-a-resource-contract` 分支工作树状态

---

## 1. 执行摘要

以下是对当前 Phase2 完成态的 10 条核心结论：

1. **优化方案 Stage A（资源合同重构）已基本完成**。`SaveSnapshot` 和 `RenderSnapshot` 中的 stamina 专用字段已统一为泛化的 `ResourcePools`；`SaveContractVersion` 升至 3.1；`PlayerResourceService` 已建立为职业资源管理的统一入口；`statGrowth` 已在升级时自动应用。这是整个优化方案中完成度最高的阶段。

2. **Stage B（DamageType 激活 + 反馈）已实现核心框架**。5 个怪物有了非零抗性数据；天赋槽位上已挂载 `damageTypeIconKey`；但抗性覆盖率仅 5/24（20.8%），远不足以让玩家在实际游玩中感受到"属性克制是值得考虑的战术维度"。

3. **Stage C（奖励辨识度 & 物品身份）已落地 4 个签名被动效果**。`PassiveEffectResolver` 实现了 `DamageVsTag`、`HpRegenPerTurn`、`DamageTypeBonus`、`ResistanceBonus` 四种被动；4 件装备携带被动效果；掉落/拾取有消息日志反馈。但被动装备仅 4 件且全部为固定品质配件，缺乏随机词缀与被动的联动。

4. **Stage D（遭遇分化 & 可学习失败）已完整实现**。`AiTriggerTracker` 组件、AI 触发器定义（`onCombatStart`/`hpBelowRatio`）、`RunSummary` 数据结构、`OutcomeSummaryPresenter` 展示层均已到位；死亡/胜利画面可展示最后事件序列。

5. **Stage E（词缀/材料/难度正式化）未启动，符合计划中"可选"定位**。当前仅 1 个难度（normal），无词缀正式化路径。

6. **内容地板达标**：24 怪物、24 基础物品、4 材料、9 词缀、4 职业、4 区域（8 层地下城）、2 Boss 遭遇、6 掉落配置表、12 AI 行为配置——均已满足 Phase2 设计文档中的数量合同。

7. **核心循环在结构上成立，但"好玩"层面存在明显短板**：战斗决策密度不足、构筑分化实质单一、奖励正反馈偏弱、探索新鲜感受制于 4 区域 × 2 层的内容容量。

8. **4 职业资源机制已差异化**（STAMINA / MANA / ENERGY / POSITIVE_ENERGY），但差异化程度尚未传导到"玩法风格截然不同"的体验层面——根因在于天赋解锁时序和 AI 交互深度不足。

9. **测试合同较为健全**：RenderSnapshot 14 项合同测试、Save 版本兼容性 4 项、TalentResolver 19 项覆盖 26 个天赋、CombatResolver 9 项、ContractLint 完整交叉引用校验。SoloClearLab 已覆盖全部 4 职业 × 3 场景等级。

10. **整体判断：Phase2 处于"功能合同完成、体验骨架搭建完毕、但血肉尚未丰满"的状态**。优化方案 Stage A~D 的技术性改造已高质量落地，但这些改造更多是"让系统正确"而非"让游戏好玩"。当前版本可以跑通一局完整流程，但尚不具备让玩家主动发起第二局的驱动力。

---

## 2. 审阅范围与依据

### 2.1 参考文档

| 文档 | 路径 | 用途 |
|------|------|------|
| Phase2 总览 | `docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md` | Phase2 目标与方法论 |
| PR-01 序列化与版本纪律 | `docs/phase2/2026-03-13-phase2-pr-01-serialization-and-version-discipline.md` | Save Schema V2, 版本合同 |
| PR-02 核心语义合同 | `docs/phase2/2026-03-13-phase2-pr-02-core-semantic-contracts.md` | 1000 能量制, ResourcePool, DamageType |
| PR-03 国际化与 Schema V2 | `docs/phase2/2026-03-13-phase2-pr-03-locale-and-schema-v2.md` | i18n, Schema V2, lint 工具 |
| PR-04 快照与清单 | `docs/phase2/2026-03-13-phase2-pr-04-snapshot-and-manifest.md` | RenderSnapshot, 视觉/音频清单 |
| PR-05 最小 Tile 外壳 | `docs/phase2/2026-03-13-phase2-pr-05-minimal-tile-shell.md` | Tile 渲染器, HUD, 背包/检视 |
| PR-06 最小官方切片 | `docs/phase2/2026-03-13-phase2-pr-06-minimal-official-slice.md` | 2 职业 + 1 区域 + 1 Boss |
| PR-07 短局扩展 | `docs/phase2/2026-03-13-phase2-pr-07-short-run-expansion.md` | 4 职业, 4 区域, 24 怪 + 24 物品 |
| 验证清单 | `docs/phase2/2026-03-13-phase2-verification-checklist.md` | 自动化与手工验证步骤 |
| Roadmap | `docs/phase2/roadmap.md` | Phase2 入口与当前检查点 |
| PR-07 执行计划 | `docs/phase2/2026-03-20-phase2-pr-07-post-review-execution-plan.md` | Sprint 拆分与问题识别 |
| **后续优化方案** | `docs/review/phase2/2026-03-21-phase2-post-review-optimization-pr-plan.md` | **本次审查重点** |
| 前次深度审查 | `docs/review/phase2/phase2_deep_review_claude_part1~4.md` | 前次审查发现 |

### 2.2 关键代码路径

| 系统 | 核心文件 |
|------|----------|
| ECS 组件 | `core/.../ecs/Components.kt` |
| 资源管理 | `core/.../resource/StaminaPools.kt`, `game/.../PlayerResourceService.kt` |
| 战斗解算 | `core/.../combat/CombatResolver.kt`, `core/.../combat/DamageType.kt` |
| 天赋系统 | `core/.../talent/TalentResolver.kt`, `core/.../talent/TalentModels.kt` |
| 物品系统 | `core/.../item/ItemModels.kt`, `ItemGenerator.kt`, `InventoryManager.kt`, `PassiveEffectResolver.kt` |
| 经验系统 | `core/.../progression/ExperienceSystem.kt` |
| 属性计算 | `core/.../stats/StatsCalculator.kt` |
| 存档合同 | `core/.../save/SaveSnapshot.kt`, `SaveContractVersion.kt` |
| 渲染快照 | `core/.../snapshot/RenderSnapshot.kt` |
| 游戏主循环 | `game/.../FoundationGameSession.kt` |
| 模块初始化 | `game/.../GameModule.kt` |
| 实体工厂 | `game/.../factory/EntityFactory.kt`, `BossFactory.kt` |
| 数据加载 | `game/.../data/DataLoader.kt`, `schema/SchemaModels.kt` |
| 客户端入口 | `client/.../GameApp.kt` |
| Tile 渲染 | `client/.../render/TileRenderModel.kt` |
| ASCII 渲染 | `client/.../render/AsciiRenderModel.kt` |
| 结算画面 | `client/.../screen/OutcomeSummaryPresenter.kt`, `VictoryScreen.kt`, `GameOverScreen.kt` |
| 主菜单 | `client/.../screen/MainMenuScreen.kt`, `MainMenuController.kt` |

### 2.3 数据资源路径

| 数据 | 路径 |
|------|------|
| 怪物定义 | `game/.../resources/data/monsters/index.yaml` |
| 物品定义 | `game/.../resources/data/items/index.yaml` |
| AI 行为 | `game/.../resources/data/ai/index.yaml` |
| 区域 / 职业 / 天赋 / Boss | `game/.../resources/data/` 各子目录 |
| 国际化 | `game/.../resources/i18n/en-US.json`, `zh-CN.json` |
| 视觉清单 | `assets-src/image/manifests/phase2-visual-manifest.json` |
| 音频清单 | `assets-src/audio/manifests/phase2-audio-manifest.json` |

### 2.4 审阅方法

1. 逐条对照优化方案 Stage A~E 的每个 PR 描述，在代码中寻找对应实现证据
2. 交叉验证数据资源（怪物/物品/AI/职业/区域）的完整性与一致性
3. 从玩家体验链路角度重构核心循环，识别断层
4. 以测试合同为辅助证据，验证系统边界行为
5. 综合前次审查发现，确认哪些问题已修复、哪些仍然存在

---

## 3. Phase2 优化方案设计实现一致性矩阵

### 3.1 Stage A：资源合同重构

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|----------|---------|---------|
| PR-A1: SaveSnapshot stamina 字段移除 | `EntitySnapshot` 不再包含独立 `stamina`/`maxStamina` 字段，统一到 `resourcePools` | ✅ 已实现 | `SaveSnapshot.kt` L171: `resourcePools: List<ResourcePoolSnapshot>`；无独立 stamina 字段 | 无偏差 | — |
| PR-A1: RenderSnapshot stamina 字段泛化 | `PlayerStatusSnapshot` 使用泛化 `currentResource`/`maxResource` | ✅ 已实现 | `RenderSnapshot.kt` L139-142: 泛化字段 + `resourceTypeId` + `resourceLabelKey` | 无偏差 | — |
| PR-A1: SaveContractVersion 升级 | 合同版本号反映结构变更 | ✅ 已实现 | `SaveContractVersion.kt` L20: `CURRENT = SaveContractVersion(3, 1)` | 无偏差 | — |
| PR-A2: 删除 Stamina 组件 | 不再有独立的 `Stamina` ECS 组件 | ✅ 已实现 | `Components.kt` 中无 `Stamina` 数据类定义 | `StaminaPools` 作为 `ResourcePools` 的 STAMINA 类型访问器保留，这是合理设计而非偏差 | — |
| PR-A2: PlayerResourceService 建立 | 统一的职业资源管理入口 | ✅ 已实现 | `PlayerResourceService.kt` L17-151: `ensureInitialized`, `sync`, `onTurnStart`, `onSuccessfulHit`, `onDamageTaken` | 完整覆盖 STAMINA/MANA/ENERGY/POSITIVE_ENERGY 四种资源的初始化、回合恢复、事件恢复 | — |
| PR-A3: statGrowth 自动应用 | 升级时根据职业 schema 自动增长四维属性 | ✅ 已实现 | `FoundationGameSession.kt` L2220-2233: `applyStatGrowth()` 方法；四职业均有差异化 statGrowth 数据 | 支持多级跳升（`repeat(levelsGained)`） | — |
| PR-A3: 经验系统完善 | 每级 +2 属性点，奇数级 +1 天赋点，上限 20 级 | ✅ 已实现 | `ExperienceSystem.kt` L17-51: 完整升级逻辑含满血/满资源恢复 | 无偏差 | — |
| DerivedStats 中 maxStamina/staminaRegen | 保留在规则层作为数值字段（不属于 A 阶段删除范围） | ✅ 符合设计 | `StatsCalculator.kt`: `maxStamina = baseStamina + wil*5 + modifiers`; `staminaRegen = 3.0 + modifiers` | 方案明确说明这些是规则层字段 | — |

**Stage A 结论**：✅ **完整完成，无偏差**。资源合同从"stamina 专用"成功重构为"职业泛化"模型，合同版本正确升级，向后兼容性通过严格版本匹配保证。

---

### 3.2 Stage B：DamageType 激活 + 反馈

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|----------|---------|---------|
| PR-B1: 怪物抗性数据填充 | 怪物应拥有有意义的属性抗性值 | ⚠️ 部分实现 | `monsters/index.yaml`: 仅 5/24 怪物有非零抗性 | 覆盖率 20.8%，不足以让属性克制成为可感知的战术维度；剩余 19 个怪物全部为零抗性 | **Medium** |
| PR-B1: 属性伤害类型可视化 | 天赋槽位展示伤害类型图标 | ✅ 已实现 | `RenderSnapshot.kt` L168: `damageTypeIconKey`; `FoundationGameSession.kt` 中 `damageTypeIconKey()` 映射 6 种类型到图标键 | 图标键已注册，Tile 渲染层可展示 | — |
| PR-B1: CombatResolver 抗性计算 | 元素伤害受抗性影响（夹到 [-25, 75]） | ✅ 已实现 | `CombatResolver.kt`: 抗性夹范围计算，`finalDamage * (1 - resistance/100)` | 物理伤害走防御减免，元素走抗性减免，逻辑正确 | — |
| PR-B2: 升级反馈增强 | 升级事件应有明确的日志/UI 反馈 | ✅ 已实现 | `FoundationGameSession.kt` L2186: 升级事件写入 `messageLog`；`OutcomeSummaryPresenter` 展示最终等级 | 升级时有消息日志，但缺乏"升级瞬间"的视觉/音效强调 | **Low** |
| PR-B2: 属性成长反馈 | 玩家能感知 statGrowth 带来的变化 | ⚠️ 弱实现 | statGrowth 应用后无独立日志条目告知玩家"力量 +3, 敏捷 +2..." | 属性自动增长是静默的，玩家无法感知每次升级获得了什么属性提升 | **Medium** |

**Stage B 结论**：⚠️ **框架完成，但数据覆盖和反馈体验不足**。DamageType 系统在代码层面完全工作，但 80% 的怪物没有抗性数据，导致这个系统在实际游玩中几乎是隐形的。升级时 statGrowth 的静默应用是一个体验盲区。

---

### 3.3 Stage C：奖励辨识度 & 物品身份

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|----------|---------|---------|
| PR-C1: 4 个签名被动效果 | 实现最小类型化被动效果合同 | ✅ 已实现 | `PassiveEffectResolver.kt`: `DamageVsTag`, `HpRegenPerTurn`, `DamageTypeBonus`, `ResistanceBonus` 四种被动 | 完整的被动解算管线 | — |
| PR-C1: 被动装备数据 | 物品数据中应有携带被动的装备 | ✅ 已实现 | `items/index.yaml`: `bandit_trophy`（DamageVsTag）, `emerald_charm`（HpRegenPerTurn）, `furnace_talisman`（DamageTypeBonus）, `seal_reliquary`（ResistanceBonus） | 每种被动类型恰好 1 件装备，合同达标 | — |
| PR-C1: 被动效果与 StatsCalculator 集成 | 被动效果参与属性/伤害计算 | ✅ 已实现 | `PassiveEffectResolver.kt`: `resolveDamageAdjustment()`, `hpRegenPerTurn()`, `resistanceBonuses()` | 被动通过独立解算器工作，与 StatsCalculator 的常规修正器管线并行 | — |
| PR-C2: 掉落反馈 | 怪物死亡掉落物品时有消息反馈 | ✅ 已实现 | `FoundationGameSession.kt` L2102-2135: `log.loot.monster_drop_quality`, `log.boss.reward.dropped` | 掉落有品质标签的消息 | — |
| PR-C2: 拾取反馈 | 拾取物品时有消息反馈 | ✅ 已实现 | `FoundationGameSession.kt` L1591+: PickUp 命令处理含日志 | 拾取有物品名称的消息 | — |
| 被动装备数量 | 优化方案未明确要求超过 4 件 | 合同达标但体验单薄 | 24 件基础物品中仅 4 件有被动（16.7%） | 被动装备全部是固定品质配件（ACCESSORY 类），武器和防具均无被动效果，这压缩了"装备选择"的决策空间 | **Medium** |

**Stage C 结论**：✅ **合同完成**。4 种被动效果类型 + 4 件签名装备 + 掉落/拾取反馈均已到位。但被动效果仅存在于 4 件固定配件上，武器和防具缺乏被动效果联动，使得"装备选择"仍然主要是看数值高低。

---

### 3.4 Stage D：遭遇分化 & 可学习失败

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|----------|---------|---------|
| PR-D1: AI 触发器系统 | Elite/Boss 拥有简单脚本化 AI 触发器 | ✅ 已实现 | `Components.kt` L109-113: `AiTriggerTracker`; `ai/index.yaml`: 6 个带触发器的 AI 配置（`onCombatStart`, `hpBelowRatio`） | 触发器支持战斗开始和血量阈值两种条件 | — |
| PR-D1: 触发器日志 | 触发器激活时有战斗日志 | ✅ 已实现 | `ai/index.yaml` 中 Boss 级触发器配有 `logMessageKey` | dungeon_lord 和 bandit_captain 触发器有日志消息键 | — |
| PR-D1: 触发器消耗追踪 | 同一触发器不会重复激活 | ✅ 已实现 | `AiTriggerTracker.consumedTriggerIds: MutableSet<String>` | 已消耗的触发器 ID 被记录，不会重复触发 | — |
| PR-D2: RunSummary 数据结构 | 完整的单局游玩摘要 | ✅ 已实现 | `GameView.kt` L103-120: `RunSummary` 含 16 个字段（outcome, floor, turns, level, zone, killer, resources, lastEvents） | 完整覆盖死亡/胜利两种场景 | — |
| PR-D2: OutcomeSummaryPresenter | 结算画面展示 RunSummary | ✅ 已实现 | `OutcomeSummaryPresenter.kt` L7-43: 生成本地化展示行 | 展示楼层、区域、回合、等级、资源、击杀者、最后事件序列 | — |
| PR-D2: 死亡回顾 | 玩家能看到导致死亡的最后事件 | ✅ 已实现 | `OutcomeSummaryPresenter.kt` L39-42: `lastEvents` 渲染; `FoundationGameSession.kt` L165: `recentSummaryEvents` | 最后事件队列被收集并在死亡画面展示 | — |
| PR-D2: 胜利回顾 | 胜利画面展示通关信息 | ✅ 已实现 | `VictoryScreen.kt` L24: 使用 `OutcomeSummaryPresenter` | 胜利时展示通关楼层、回合数、最终等级 | — |

**Stage D 结论**：✅ **完整完成，无偏差**。AI 触发器系统、RunSummary、死亡/胜利回顾均已按方案落地。这是继 Stage A 之后完成度最高的阶段。

---

### 3.5 Stage E：可选项（词缀/材料/难度正式化）

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|----------|---------|---------|
| 词缀正式化 | 可选：词缀系统正式化路径 | ❌ 未启动 | `items/index.yaml` 中有 9 个词缀定义（3 前缀 + 4 后缀），但无正式化合同 | 符合"可选"定位，当前词缀通过 ItemGenerator 随机附加 | — |
| 材料正式化 | 可选：材料系统正式化路径 | ❌ 未启动 | 4 种材料（IRON/STEEL/MITHRIL/ADAMANTITE）已有数据但无正式合同 | 符合"可选"定位 | — |
| 难度正式化 | 可选：多难度系统 | ❌ 未启动 | 仅 1 个难度 `normal`（所有乘数为 1.0） | 符合"可选"定位，但对耐玩性有影响 | **Low** |

**Stage E 结论**：❌ **未启动，符合计划预期**。方案明确标注 Stage E 为可选项。

---

### 3.6 前次审查问题修复状态

| 前次审查发现 | 优化方案回应 | 当前状态 | 证据 |
|-------------|-------------|---------|------|
| DamageType 六通道无实际差异 | Stage B: 填充怪物抗性 | ⚠️ 部分修复 | 5/24 怪物有抗性，覆盖率不足 |
| STAMINA 双轨资源管理 | Stage A: 统一资源合同 | ✅ 已修复 | PlayerResourceService + ResourcePools 统一 |
| statGrowth 未自动应用 | Stage A: PR-A3 | ✅ 已修复 | `applyStatGrowth()` 方法已实现 |
| 怪物掉落完全缺失 | 前次 P2-C 已修复 | ✅ 已修复 | 6 个 loot profile, 所有怪物有 loot 配置 |
| 天赋解锁时序严重失衡 | 前次修复为奇数级 +1 天赋点 | ✅ 已修复 | `ExperienceSystem.kt`: `level % 2 == 1` 时 +1 天赋点 |
| Death Recap 框架缺乏完整数据 | Stage D: RunSummary 增强 | ✅ 已修复 | 16 字段 RunSummary + lastEvents 展示 |
| AI 预警/电报功能不存在 | Stage D: AI 触发器系统 | ✅ 已实现 | `onCombatStart`/`hpBelowRatio` 触发器 + 日志消息 |

---

### 3.7 一致性总评

| 阶段 | 设计项总数 | 完整实现 | 部分实现 | 未实现 | 完成率 |
|------|----------|---------|---------|--------|--------|
| Stage A | 7 | 7 | 0 | 0 | **100%** |
| Stage B | 5 | 3 | 2 | 0 | **60%** |
| Stage C | 6 | 5 | 1 | 0 | **83%** |
| Stage D | 7 | 7 | 0 | 0 | **100%** |
| Stage E | 3 | 0 | 0 | 3 | **0%**（可选） |
| 前次问题修复 | 7 | 6 | 1 | 0 | **86%** |

**整体评估**：优化方案的技术性改造（Stage A + D）完成度极高。Stage B 和 C 的框架完整但数据填充和体验深度不足——这不是"没做完"的问题，而是"做到了合同最低线、但离好玩还有距离"的问题。

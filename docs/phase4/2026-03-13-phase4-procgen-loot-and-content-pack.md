> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
> `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`

# Phase 4 - ProcGen, Loot & Content Pack

**阶段**: `Phase 4`  
**版本目标**: `v0.4.x`  
**优先级**: `P1`  
**前置条件**: `Phase 3` 出口全部满足  
**对应问题**: Phase 3 已经能形成稳定长局，但不同 run 之间的地图、掉落、精英遭遇和隐藏内容差异还不够，且内容扩展还没有正式 overlay/harness 路径。

---

## 1. 阶段目标

把游戏从“可完成长局”推进到“高重复游玩价值长局”，同时让内容扩展进入可验证的数据包路径。

完成标准：

1. 混合拓扑地图、pattern room、vault、环路和 biome family 进入正式主线。
2. `lock-key DAG`、隐藏入口、任务拓扑具备自动化可解性验证。
3. `affix / unique / artifact / elite mutation / hidden event` 形成成体系掉落与遭遇生态。
4. `content pack overlay + schema lint + harness` 能在不改 `core` 的前提下装载新增内容。

## 2. 当前问题

1. Phase 3 的世界结构仍偏“固定骨架 + 内容拼接”。
2. 掉落系统足以驱动构筑，但还不能支撑长期重复游玩差异。
3. 隐藏区域、锁钥匙和 secret logic 缺少自动化可解性验证。
4. 新内容仍主要靠主仓库直写，没有正式 overlay/harness 路径。

### 2.1 本阶段必须冻结的系统

1. `map family / pattern room / vault` 元数据。
2. `lock-key DAG` 与可解性校验接口。
3. `iLvl / qLvl / rarity budget / affix budget`。
4. `elite mutation` 与 hidden event contract。
5. `content pack overlay schema` 与 headless harness。

## 3. 范围与非目标

### 3.1 范围

1. ProcGen 深化与 map family。
2. 锁钥匙、隐藏入口、探索奖励与可解性验证。
3. Loot 生态 V2 与预算模型。
4. 精英突变、Boss 变体和隐藏事件。
5. 数据包 overlay、lint、headless harness 与示例 content pack。

### 3.2 非目标

1. 不引入 Lua runtime。
2. 不做完整脚本宿主。
3. 不为了内容包而放宽 `core` 规则边界。

## 4. 技术方案

### 4.1 混合拓扑地图

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/mapgen/*
game/src/main/resources/data/mapgen/*.yaml
core/src/test/kotlin/com/ktome/core/mapgen/*
```

冻结口径：

1. 地图生成必须支持 room、loop、pattern room、vault、biome family 组合。
2. 差异来自拓扑与 content seed，而不是只换贴图。
3. 生成器必须可注入固定 seed，且关键不变量可断言。

混合生成器最低能力表：

| 维度 | Phase 1 BSP | Phase 4 混合生成器 |
| --- | --- | --- |
| 房间形状 | 矩形 | 矩形 + L 形 + 圆形 + 不规则 |
| 走廊 | L 型连接 | 弯曲走廊 + 隐藏通道 |
| 拓扑 | 树形单路径 | 含环路的多路径 |
| 特殊房间 | 无 | vault / 陷阱房 / 宝藏房 |
| biome | 单一 | 同层允许双 biome 混合 |

### 4.2 Lock-Key 与可解性验证

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/world/solvability/*
tools/src/main/kotlin/com/ktome/tools/solvability/*
```

冻结口径：

1. 锁钥匙和任务拓扑必须显式表达成 DAG 或等价图结构。
2. 隐藏内容允许需要发现，但不允许形成不可解主线死局。
3. `SolvabilityHarness` 必须能在批量 seed 下跑可达性验证。

最小可解性约束固定为：

1. 从入口到出口至少存在一条可达主路径。
2. 所有钥匙 / 开关都必须在被需求之前可获取。
3. 所有可选区域都必须至少有一条能回主线的路径。
4. Boss 门后不允许藏任何主线必需物品或必需钥匙。

### 4.3 Loot 生态 V2

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/loot/*
game/src/main/resources/data/items/*.yaml
tools/src/main/kotlin/com/ktome/tools/balance/*
```

冻结口径：

1. 掉落质量必须由预算驱动，而不是手写散点数值。
2. `affix`、`unique`、`artifact` 必须共用一套预算词汇，不允许各写一套。
3. 允许极端强力物品，但必须有明确 rarity/cost/phase 约束。

`LootBalanceLab` 的输入上下文至少固定为：

1. `sourceLevel`
2. `sourceTier`
3. `zoneId`
4. `playerLevel`
5. `magicFindBonus`
6. `seed`

`SourceTier` 第一版稀有度加成固定为：

| tier | rarity bonus |
| --- | --- |
| `NORMAL` | `0.00` |
| `ELITE` | `0.15` |
| `BOSS` | `0.40` |
| `CHEST` | `0.10` |

Phase 4 与元素系统的连接也必须写死：

1. `LIGHTNING + WATER`
2. `FIRE + OIL`
3. `COLD + WATER / ICE`

这些交互必须进入 batch/harness，而不是只留白盒观察。

### 4.4 精英突变与隐藏内容

建议文件与模块：

```text
game/src/main/resources/data/elites/*.yaml
game/src/main/resources/data/events/*.yaml
```

冻结口径：

1. 精英突变必须能解释，不允许只做无来源数值膨胀。
2. hidden event、secret zone、特殊奖励都必须可通过 harness 触发和验证。
3. Boss 变体不得破坏基础 encounter contract。

### 4.5 Content Pack Overlay

建议文件与模块：

```text
game/src/main/kotlin/com/ktome/game/contentpack/*
tools/src/main/kotlin/com/ktome/tools/contentpack/*
examples/content-packs/*
```

冻结口径：

1. overlay 只能扩内容定义，不能改 `core` 规则语义。
2. 所有外部 pack 必须通过 schema lint、引用解析和 headless harness。
3. manifest、i18n、visual/audio key 也必须纳入 pack 校验。

## 5. 推荐 PR / 工作包拆分

### P4-W1 Map Family & Pattern Rooms

1. biome family
2. loop / pattern room / vault
3. 地图生成不变量测试

### P4-W2 Solvability Harness

1. lock-key DAG
2. hidden entrance 规则
3. 批量 seed 可解性验证

### P4-W3 Loot Budget V2

1. `iLvl/qLvl/rarity budget`
2. affix/unique/artifact 预算统一
3. LootBalanceLab

### P4-W4 Elite Mutation & Hidden Events

1. 精英突变包
2. Boss 变体
3. hidden event / secret zone

### P4-W5 Content Pack Overlay

1. overlay loader
2. schema lint
3. headless content harness
4. 示例 content pack

## 6. 测试与自证

### 6.1 必测模块

1. `core.mapgen`
2. `core.world.solvability`
3. `core.loot`
4. `game.contentpack`
5. `tools.solvability`
6. `tools.balance`

### 6.2 必测行为

1. 多 seed 地图可达、可读、无主线死局。
2. 锁钥匙和隐藏入口逻辑能被 harness 发现和证明。
3. 掉落预算与阶段匹配，不出现明显爆表或废条目泛滥。
4. 精英突变与隐藏事件可以稳定触发和验证。
5. 示例 content pack 可被独立加载并通过 lint/harness。

### 6.3 自动化命令

```bash
./gradlew test
./gradlew :core:test
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew lootBalanceLab
./gradlew hiddenContentHarness
./gradlew contentPackHarness
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

### 6.4 白盒验证

1. 连续运行多个不同 seed 的新局。
2. 观察：
   - 地图拓扑和秘密区域确有差异
   - 但主线仍可达
3. 在至少两套不同 build 下走完长局关键路径，确认掉落和精英遭遇差异可感知。
4. 装载示例 content pack，确认新增内容可进入主线且不破坏 base game。

## 7. 出口门禁

1. 多 seed 下 mapgen 与 solvability harness 稳定。
2. loot 预算和阶段匹配通过实验室验收。
3. 精英突变、hidden event、secret zone 都可被自动化与白盒验证。
4. 示例 content pack 可在不改 `core` 的前提下装载和验证。

## 8. 风险与止损

1. 如果 ProcGen 差异只来自装饰层，必须回到拓扑元数据设计，不继续堆 tile。
2. 如果可解性验证不稳定，优先缩减隐藏逻辑和锁钥匙复杂度。
3. 如果 loot 预算出现多套平行词汇，必须先统一预算词典，再补内容。
4. 如果 overlay 开始尝试改规则语义，必须立即收回到 content-only 边界。

## 9. 当前状态

1. Phase 4 总体目标、系统设计和本文已对齐。
2. 本文可作为 ProcGen/Loot/Content Pack 的实现说明书与 PR 底稿。
3. 当前仍未开始实际代码实现，实验室和 harness 仍需建设。

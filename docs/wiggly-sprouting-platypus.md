# 类 ToME 回合制 Roguelike 新项目完整规划

## Context

用户希望开发一个类似 Tales of Maj'Eyal 的回合制 Roguelike 新项目。要求：
1. 第一步实现后简单能玩
2. 长期整体玩法接近 ToME
3. 拓展性强，支持 mod
4. 最大程度适配 AI 辅助开发，AI 可自行验证

经过对 ToME/DCSS/Cogmind/Hades/Slay the Spire/Caves of Qud/Brotato 等游戏的技术栈调研，以及从社区资源、AI 适配度、AI 自验证三个维度的对比分析，确认技术栈为 **Kotlin + libGDX + Lua**。

---

## 一、设计支柱

1. **回合制，每个输入都是决策** — 玩家做一个动作，世界推进一步
2. **选择关门** — 天赋点稀缺、build 不可逆、每次选择都有代价
3. **信息不隐藏** — 玩家可以检视任何敌人、看到任何数值
4. **Permadeath 为核心** — 死亡永久，可选 Adventure 模式提供有限复活
5. **Mod-first 架构** — 游戏本身就是引擎的一个 module，用户 mod 用同一套 API

---

## 二、技术栈（已确认）

### Kotlin + libGDX + Lua

| 层 | 技术 | 职责 |
|----|------|------|
| 核心逻辑 | **纯 Kotlin 库**（零引擎依赖） | ECS、回合调度、战斗、属性、天赋、寻路、FOV、地图生成、物品、AI 决策 |
| 渲染/IO | **libGDX**（薄层） | 窗口、渲染、输入、音频、文件 IO |
| Mod 脚本 | **Lua (LuaJ)** | 天赋行为、AI 行为、事件 hook、内容定义扩展 |
| 数据定义 | **YAML** | 怪物/物品/天赋/地图模板的声明式定义 |
| 构建 | **Gradle (Kotlin DSL)** | 多模块构建 |
| 测试 | **JUnit 5 + kotest** | 核心逻辑全覆盖，`gradle test` 一行验证 |

### 为什么选这个栈

**AI 适配度调研结论**：

1. **训练数据**：Java/Kotlin 在 AI 模型训练数据中代表性极高；GDScript 被明确指出"underrepresented in training data"
2. **自验证**：纯 Kotlin 核心库用 JUnit 测试，`gradle test` 一行命令，无需 GUI、无需游戏引擎——Claude Code 可以直接执行验证
3. **Caves of Qud 启示**："Zero game logic was trapped behind Unity"——游戏逻辑完全不依赖引擎，16 小时就能从 Unity 迁移到 Godot。我们从第一天就采用这个架构

### 核心架构原则：逻辑层零引擎依赖

```
┌─────────────────────────────────────────────┐
│              Mod Layer (Lua + YAML)          │  ← 用户 mod
├─────────────────────────────────────────────┤
│           Game Module (Lua + YAML)           │  ← "官方游戏"也是 module
├─────────────────────────────────────────────┤
│           Core Library (纯 Kotlin)            │  ← JUnit 可测，零引擎依赖
│  ECS · TurnScheduler · FOV · AStar           │
│  Combat · Stats · Talents · Items · AI       │
│  MapGen · Save · EventBus · LuaBridge        │
├─────────────────────────────────────────────┤
│           Rendering Shell (libGDX)           │  ← 薄层，未来可替换
│  Window · AsciiRenderer · Input · Audio      │
├─────────────────────────────────────────────┤
│           libGDX / LWJGL                     │  ← 平台抽象
└─────────────────────────────────────────────┘
```

**AI 验证链**: Core Library 的每个公开函数都有 JUnit 测试 → Claude Code 写完代码后执行 `gradle test` → 立即知道逻辑是否正确 → 不需要启动游戏、不需要 GUI。

---

## 三、项目结构

```
tome-like/
├── build.gradle.kts
├── settings.gradle.kts
│
├── core/                              ← 纯 Kotlin 库（零引擎依赖）
│   ├── src/main/kotlin/core/
│   │   ├── ecs/
│   │   │   ├── World.kt               ← ECS 世界容器
│   │   │   ├── Entity.kt              ← EntityId + Component 管理
│   │   │   └── System.kt              ← System 接口
│   │   ├── turn/
│   │   │   └── TurnScheduler.kt       ← 能量系统回合调度
│   │   ├── fov/
│   │   │   └── Shadowcasting.kt       ← 视野计算
│   │   ├── pathfinding/
│   │   │   └── AStar.kt               ← A* 寻路
│   │   ├── map/
│   │   │   ├── GameMap.kt             ← 网格数据结构
│   │   │   └── BSPGenerator.kt        ← BSP 房间生成
│   │   ├── combat/
│   │   │   ├── CombatResolver.kt      ← 伤害计算
│   │   │   └── StatusEffects.kt       ← Buff/Debuff 系统
│   │   ├── talent/
│   │   │   ├── TalentRegistry.kt      ← 天赋注册表
│   │   │   └── TalentResolver.kt      ← 天赋效果结算
│   │   ├── item/
│   │   │   ├── ItemGenerator.kt       ← 随机物品生成
│   │   │   └── Inventory.kt           ← 背包管理
│   │   ├── ai/
│   │   │   └── AIDecision.kt          ← AI 行为决策树
│   │   ├── event/
│   │   │   └── EventBus.kt            ← 事件发布/订阅
│   │   ├── save/
│   │   │   └── SaveManager.kt         ← 序列化/反序列化
│   │   └── lua/
│   │       └── LuaBridge.kt           ← Lua 脚本桥接
│   │
│   └── src/test/kotlin/core/          ← JUnit 测试（核心验证）
│       ├── ecs/WorldTest.kt
│       ├── turn/TurnSchedulerTest.kt
│       ├── fov/ShadowcastingTest.kt
│       ├── combat/CombatResolverTest.kt
│       ├── talent/TalentResolverTest.kt
│       ├── map/BSPGeneratorTest.kt
│       ├── item/ItemGeneratorTest.kt
│       └── ai/AIDecisionTest.kt
│
├── game/                              ← 游戏模块（数据 + 胶水逻辑）
│   ├── src/main/kotlin/game/
│   │   ├── GameModule.kt              ← 模块入口（注册所有内容）
│   │   ├── systems/                   ← 游戏特定 System
│   │   └── factory/                   ← 从 YAML 创建实体
│   │
│   └── src/main/resources/data/       ← YAML 数据定义
│       ├── monsters.yaml
│       ├── items.yaml
│       ├── talents.yaml
│       └── mapgen.yaml
│
├── client/                            ← libGDX 渲染层（薄层）
│   └── src/main/kotlin/client/
│       ├── DesktopLauncher.kt
│       ├── render/
│       │   ├── AsciiRenderer.kt       ← ASCII 终端模拟
│       │   └── TileRenderer.kt        ← 后续 Tile 渲染（Phase 5）
│       ├── input/
│       │   └── InputHandler.kt        ← 键盘输入映射
│       ├── audio/
│       │   └── AudioManager.kt
│       └── screen/
│           ├── GameScreen.kt          ← 主游戏画面
│           ├── InventoryScreen.kt
│           ├── TalentScreen.kt
│           └── MessageLog.kt
│
├── mods/                              ← mod 目录（Phase 4 启用）
│   └── example_mod/
│       ├── mod.yaml
│       └── scripts/
│
└── scripts/                           ← Lua 游戏脚本
    ├── talents/
    │   ├── shield_bash.lua
    │   └── war_cry.lua
    └── ai/
        ├── chase.lua
        └── kite.lua
```

---

## 四、Phase 1 — MVP：能玩的回合制 Roguelike

**目标**: 完整游戏循环。进入地牢 → 探索 → 战斗 → 捡装备 → 升级 → 死亡或通关。

### 4.1 交付清单

| 系统 | 内容 | 模块归属 |
|------|------|---------|
| **渲染** | ASCII 文本模式（80×50 终端模拟） | `client/` |
| **回合系统** | 能量调度器（speed=100 标准，200=双速） | `core/turn/` |
| **移动** | 8 方向网格移动，碰撞式攻击 | `core/ecs/` |
| **视野** | Shadowcasting FOV | `core/fov/` |
| **地图生成** | BSP 房间 + L 型走廊，5 层 80×50 | `core/map/` |
| **战斗** | ATK vs DEF + 随机偏移，暴击，闪避 | `core/combat/` |
| **怪物** | 5 种：Rat / Skeleton / Archer / Orc / Boss | `game/data/` |
| **怪物 AI** | 3 种：chase / kite / patrol | `core/ai/` |
| **物品** | 武器 × 材质、护甲、药水、卷轴 | `core/item/` |
| **玩家** | 1 职业（战士），4 主动天赋 | `game/data/` |
| **属性** | STR/DEX/CON/WIL → 攻击/闪避速度/HP/资源 | `core/` |
| **天赋** | 4 个主动（猛击/冲锋/盾击/战吼），1-5 级 | `core/talent/` |
| **资源** | Stamina，自然回复 3/turn | `core/` |
| **升级** | 经验值 + 属性分配 + 天赋点 | `core/` |
| **UI** | 侧边栏（HP/属性/装备）+ 消息日志 | `client/` |
| **存档** | JSON 单存档，死亡删档（Permadeath） | `core/save/` |
| **胜利** | 击杀第 5 层 Boss → 结算画面 | `game/` |

### 4.2 核心公式

**回合调度**（能量系统）：
```
每 tick：所有实体 energy += speed
当 energy >= 100：消耗 100，获得行动机会
速度 100 = 标准（每轮 1 次），200 = 每轮 2 次，50 = 每 2 轮 1 次
```

**战斗公式**（首版极简）：
```
命中率 = 0.85 + (attackerAccuracy - targetEvasion) * 0.01
暴击率 = 0.05 + dex * 0.002
伤害 = attack + random(-2, 2)
减免 = max(0, damage - defense)
最终 = max(1, 减免后伤害)
暴击伤害 = 最终 × 1.5
```

**属性派生**：
```
STR → attack(+2), carry capacity(+5)
DEX → evasion(+1), speed(+0.5), crit(+0.2%)
CON → maxHP(+8), HP regen(+0.2/turn)
WIL → maxStamina(+5), talent power(+1%)
```

### 4.3 天赋设计（首版 4 个）

| 天赋 | 消耗 | 冷却 | 范围 | Lv1 效果 | Lv5 效果（质变） |
|------|------|------|------|---------|----------------|
| **猛击** | 8 Stamina | 3 turn | 1 格 | 1.5× 伤害 | 2.5× 伤害 + 击退 1 格 + 破甲 3 turn |
| **冲锋** | 12 Stamina | 6 turn | 3-5 格 | 移动到目标 + 1.2× 攻击 | 移动到目标 + 1.8× 攻击 + 眩晕 2 turn |
| **盾击** | 10 Stamina | 5 turn | 1 格 | 1.2× 伤害 + 眩晕 2 turn | 1.6× 伤害 + 眩晕 3 turn + 击退 2 格 |
| **战吼** | 15 Stamina | 10 turn | 3 格圆 | +20% ATK 5 turn | +35% ATK 8 turn + 敌人 -20% DEF 5 turn |

**关键**: Lv5 不只是数值缩放，而是解锁新能力（击退、破甲、眩晕扩展）。

### 4.4 Phase 1 任务拆分

```
Week 1: 引擎基础 + 可验证
  [1] Gradle 多模块项目搭建（core/ + game/ + client/）
  [2] ECS 核心（World / Entity / Component / System）+ 单元测试
  [3] 网格地图数据结构 + BSP 生成 + 单元测试
  [4] FOV Shadowcasting + 单元测试
  [5] ASCII 渲染器（libGDX BitmapFont 终端模拟）
  [6] 玩家移动（8 方向）+ 碰撞检测
  验证: gradle test 全绿 + 屏幕上 @ 在地牢中移动

Week 2: 战斗与 AI
  [7] 能量系统回合调度器 + 单元测试
  [8] 碰撞式近战攻击 + 伤害公式 + 单元测试
  [9] 怪物 AI（chase/kite/patrol）+ 单元测试
  [10] YAML 怪物定义加载 + 怪物生成
  [11] 死亡处理 + 经验值 + 升级
  [12] 消息日志 UI
  验证: gradle test 全绿 + 能和怪物战斗并升级

Week 3: 物品与天赋
  [13] 物品系统（拾取/装备/卸下）+ 单元测试
  [14] 随机物品生成（基础 + 材质 modifier）+ 单元测试
  [15] 库存 UI
  [16] 天赋系统（4 个战士天赋）+ 单元测试
  [17] Stamina 资源 + 天赋冷却
  [18] 天赋 UI（热键 1-4）
  验证: gradle test 全绿 + 能拾取装备、使用天赋

Week 4: 完整循环
  [19] 楼梯 + 5 层地牢
  [20] Boss 战（第 5 层，多天赋 Boss）
  [21] 属性分配 UI + 天赋点分配 UI
  [22] Permadeath + JSON 存档/读档
  [23] 胜利/失败画面 + 结算统计
  [24] 主菜单 + 新游戏流程
  验证: gradle test 全绿 + 完整 run 可通关

Week 5: 打磨
  [25] 数值 balance pass
  [26] 测试覆盖率检查（核心系统 >80%）
  [27] Bug 修复
  [28] 发布 v0.1.0
```

### 4.5 Phase 1 出口标准

1. `gradle test` 全部通过，核心逻辑测试覆盖率 > 80%
2. 能从主菜单开始新游戏
3. 5 层地牢中探索、战斗、升级、换装备
4. 4 个天赋各自有明确战术价值
5. 被怪物杀死 → Game Over；杀死 Boss → Victory
6. 存档/读档正常，Permadeath 正确删档
7. 至少 3 种可观察到的不同 AI 行为

---

## 五、Phase 2 — 系统深化（3-5 个月）

### 2.1 多职业（4 个）

| 职业 | 资源 | 天赋类别（4 类 × 4 天赋 = 16 天赋/职业） | 战术定位 |
|------|------|------|------|
| **战士** | Stamina（自然回复） | 武器精通 / 护甲精通 / 战术 / 力量 | 正面硬刚 |
| **法师** | Mana（缓慢回复） | 火焰 / 寒冰 / 奥术 / 护盾 | 范围输出 + 控制 |
| **盗贼** | Stamina（击杀回复） | 匕首 / 暗影 / 陷阱 / 敏捷 | 暴击 + 位移 |
| **牧师** | 正能量（受伤回复） | 神圣 / 驱邪 / 治愈 / 守护 | 生存 + 辅助 |

天赋点极度稀缺：50 级约 68 点，全满需要 320 点，只能精通 ~20%。

### 2.2 状态效果系统（"开关型"而非"数值型"）

| 效果 | 改变玩家能力 | 反制 |
|------|------------|------|
| **Stun** | 跳过回合 | 高 Physical Save |
| **Root/Pin** | 不能移动，能攻击 | 位移天赋 / 铭文 |
| **Silence** | 不能用天赋 | 等待 / 驱散铭文 |
| **Blind** | 视野缩到 1 格 | 等待 / 治愈铭文 |
| **Confuse** | 移动方向随机化 | 高 Mental Save |
| **Slow** | 速度 -30~50% | 装备 / 天赋 |
| **Bleed/Poison/Burn** | 每回合持续伤害 | 各自对应治疗 |

### 2.3 铭文系统

4 个铭文槽，每个铭文有冷却但不消耗：

| 铭文 | 效果 | 对抗 |
|------|------|------|
| 治愈铭文 | 回复 HP + 短暂回复增强 | 低 HP |
| 驱散铭文 | 清除 1 个 debuff + 2 秒免疫 | 控制效果 |
| 护盾铭文 | 吸收 N 点伤害 | 爆发伤害 |
| 相位铭文 | 短距传送 + 短暂隐身 | 包围 |
| 野性铭文 | 持续回复 + 属性增强 | 消耗战 |

### 2.4 装备前缀/后缀

```
物品 = 基础类型 × 材质 × 前缀(0-1) × 后缀(0-1)

示例：秘银长剑 [嗜血的] [速度之]
= 长剑 + 秘银(+30%伤害) + 嗜血(击杀回HP) + 速度(+15%攻速)
```

品质：白(0 affix) / 绿(1) / 蓝(2) / 紫(unique) / 橙(artifact,改变天赋行为)

### 2.5 多地牢 + 世界地图

节点式世界地图，3-5 个主题地牢（各 5-10 层），等级推荐但不硬锁。

---

## 六、Phase 3 — 内容与世界（4-6 个月）

- 职业扩展到 8-12 个（进阶职业需通关解锁）
- 种族系统（人类/精灵/矮人/兽人/亡灵，各自 1 个种族天赋类别）
- 世界地图 5+ 地牢 + 隐藏地牢
- Prodigy 系统（Level 25/42 各选 1 个极强被动）
- 完整通关 8-15 小时

---

## 七、Phase 4 — Mod 系统（2-3 个月）

### 7.1 Mod 能力

| 能力 | 方式 |
|------|------|
| 新增怪物/物品/天赋 | YAML 数据文件 |
| 天赋行为脚本 | Lua |
| 修改现有数据 | YAML overlay |
| 新增事件 hook | Lua `register_hook()` |
| 新增 AI 行为 | Lua |
| 全转换 (Total Conversion) | 替换整个 game module |

### 7.2 Mod 加载器

```kotlin
class ModLoader(private val modsDir: Path) {
    fun loadMod(manifest: ModManifest) {
        // 1. 加载 YAML 数据（覆盖或追加）
        // 2. 加载 Lua 脚本（注册 hook）
        // 3. 加载资产（sprite / sound）
        // 4. 验证兼容性
    }
}
```

### 7.3 Lua Bridge 示例

```lua
-- talents/chain_lightning.lua
function on_talent_used(event)
    if event.talent == "chain_lightning" then
        local targets = find_enemies_in_range(event.source, 5)
        local primary = targets[1]
        deal_damage(event.source, primary, event.damage)
        -- 连锁跳跃
        local secondary = find_nearest_enemy(primary, 3)
        if secondary then
            deal_damage(event.source, secondary, event.damage * 0.6)
        end
    end
end
register_hook("talent:used", on_talent_used)
```

---

## 八、Phase 5 — 打磨与社区（持续）

- ASCII → Tile 图形化（保留 ASCII 选项）
- 音频系统（环境/战斗/UI 音效）
- 难度阶梯（Normal/Nightmare/Insane/Madness）
- 成就系统（解锁新职业/种族）
- 死因分析（伤害来源分解、最后 N 回合回放）
- 在线排行榜（可选）

---

## 九、AI 验证策略

这是本项目的核心竞争力之一：AI 可以自验证几乎所有游戏逻辑。

### 9.1 测试分层

| 层 | 测试方式 | AI 可执行 | 覆盖比例 |
|----|---------|----------|---------|
| **core/** 纯逻辑 | JUnit 5 (`gradle test`) | 直接执行 | ~80% 代码量 |
| **game/** 数据加载 | JUnit 5（YAML 解析验证） | 直接执行 | ~10% |
| **client/** 渲染 | 手动目视检查 | 不适用 | ~10% |

### 9.2 每个核心系统的测试契约

```
TurnSchedulerTest:
  - speed=100 每 100 tick 行动 1 次
  - speed=200 每 100 tick 行动 2 次
  - speed=50 每 200 tick 行动 1 次
  - 多实体交替行动顺序正确

CombatResolverTest:
  - ATK > DEF 造成正伤害
  - ATK <= DEF 至少造成 1 点伤害
  - 暴击伤害 = base × 1.5
  - 闪避率 0% → 100% 命中
  - 闪避率 100% → 0% 命中

ShadowcastingTest:
  - 空地图全可见
  - 墙后不可见
  - 拐角遮挡正确
  - 视野半径限制正确

BSPGeneratorTest:
  - 所有房间在地图边界内
  - 所有房间不重叠
  - 所有房间通过走廊连通（flood fill 验证）
  - 种子确定性（同 seed 同结果）

TalentResolverTest:
  - 冷却中不可使用
  - 资源不足不可使用
  - 范围外目标不可选择
  - 各级效果数值正确

ItemGeneratorTest:
  - 材质 modifier 正确应用
  - affix 数量符合品质规则
  - 种子确定性

AIDecisionTest:
  - chase AI: 向目标移动
  - kite AI: 攻击范围内攻击，否则保持距离
  - patrol AI: 沿路径巡逻，发现敌人切换行为
```

### 9.3 CI 命令

```bash
# AI 写完代码后的标准验证流程
gradle test                    # 全部单元测试
gradle test --tests "core.*"   # 只跑核心逻辑
gradle jacocoTestReport        # 覆盖率报告
```

---

## 十、风险与止损

| 风险 | 止损 |
|------|------|
| Phase 1 做太多无法完成 | 严格守住"5 层 + 1 职业 + 5 怪"的 MVP 线 |
| ECS 过度工程 | Phase 1 用最简实现（HashMap<KClass, Any>） |
| Lua 集成复杂度 | Phase 1-2 不引入 Lua，用 Kotlin 硬编码；Phase 3 开始迁移 |
| 数值平衡失控 | 每个 Phase 结束做 balance pass |
| 美术资源缺乏 | ASCII 不依赖美术；tile 阶段用免费 roguelike tileset |
| 范围蔓延 | 每个 Phase 有出口标准，不满足不进入下一 Phase |

---

## 十一、立即开始的前 5 步

1. `gradle init` 创建 Kotlin 多模块项目（core/ + game/ + client/）
2. 实现 ECS 核心 + JUnit 测试
3. 实现网格地图 + BSP 生成 + JUnit 测试
4. 实现 ASCII 渲染器（libGDX BitmapFont）
5. 实现玩家移动 + FOV

完成后你就能看到 `@` 在有视野限制的地牢中移动，且 `gradle test` 全绿。

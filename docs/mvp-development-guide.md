# K-ToME Phase 1 — MVP 完整开发文档

> **版本**: v0.1.0-draft
> **目标**: 完整游戏循环。进入地牢 → 探索 → 战斗 → 捡装备 → 升级 → 死亡或通关。
> **前置文档**: [wiggly-sprouting-platypus.md](./wiggly-sprouting-platypus.md)

---

## 目录

1. [项目搭建](#1-项目搭建)
2. [Core 模块设计](#2-core-模块设计)
3. [Game 模块设计](#3-game-模块设计)
4. [Client 模块设计](#4-client-模块设计)
5. [阶段实现计划](#5-阶段实现计划)
6. [测试策略](#6-测试策略)
7. [出口标准](#7-出口标准)

---

## 1. 项目搭建

### 1.1 Gradle 多模块结构

```
K-ToME/
├── build.gradle.kts          ← 根构建脚本
├── settings.gradle.kts       ← 模块声明
├── gradle.properties         ← 版本号集中管理
├── core/                     ← 纯 Kotlin 库（零引擎依赖）
│   └── build.gradle.kts
├── game/                     ← 游戏数据 + 胶水逻辑（依赖 core）
│   └── build.gradle.kts
├── client/                   ← libGDX 渲染层（依赖 core + game）
│   └── build.gradle.kts
└── docs/
```

### 1.2 gradle.properties

```properties
javaVersion=21
kotlinVersion=2.2.21
libgdxVersion=1.14.0
junitVersion=5.14.2
kotestVersion=6.0.2
jacksonVersion=2.20.1
group=com.ktome
version=0.1.0-SNAPSHOT
```

**Gradle Wrapper 建议固定为 `8.14.3`**：Java 21 运行 Gradle 需要 `8.5+`，而 Kotlin `2.2.21` 对 Gradle `7.6.3-8.14` 提供完全支持，因此 `8.14.3` 是当前更稳妥的兼容上界。JDK 基线同步提升到 `21`。

### 1.3 settings.gradle.kts

```kotlin
rootProject.name = "K-ToME"
include("core", "game", "client")
```

### 1.4 根 build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version properties["kotlinVersion"] as String apply false
    id("jacoco")
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")

    repositories {
        mavenCentral()
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:${rootProject.properties["junitVersion"]}")
        "testImplementation"("io.kotest:kotest-assertions-core:${rootProject.properties["kotestVersion"]}")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
    }

    kotlin {
        jvmToolchain((rootProject.properties["javaVersion"] as String).toInt())
    }
}
```

### 1.5 core/build.gradle.kts

```kotlin
dependencies {
    // 零引擎依赖。仅允许以下依赖：
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${rootProject.properties["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${rootProject.properties["jacksonVersion"]}")
}
```

**硬性约束**: `core` 模块禁止依赖 libGDX、Lua、任何 GUI/渲染库。只允许纯 JVM 库（集合、序列化、测试）。

### 1.6 game/build.gradle.kts

```kotlin
dependencies {
    implementation(project(":core"))
}
```

### 1.7 client/build.gradle.kts

```kotlin
dependencies {
    implementation(project(":core"))
    implementation(project(":game"))
    implementation("com.badlogicgames.gdx:gdx:${rootProject.properties["libgdxVersion"]}")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:${rootProject.properties["libgdxVersion"]}")
    implementation("com.badlogicgames.gdx:gdx-platform:${rootProject.properties["libgdxVersion"]}:natives-desktop")
}
```

### 1.8 包命名规范

| 模块 | 根包 |
|------|------|
| core | `com.ktome.core` |
| game | `com.ktome.game` |
| client | `com.ktome.client` |

---

## 2. Core 模块设计

Core 模块是整个项目的心脏，包含所有游戏逻辑，零引擎依赖，100% JUnit 可测。

### 2.1 ECS（Entity-Component-System）

#### 设计决策

Phase 1 采用最简 ECS：不做 Archetype 分组、不做缓存优化。用 `HashMap<KClass<*>, Any>` 存储组件即可。当性能成为瓶颈时再优化。

#### 核心接口与类

```kotlin
// === EntityId ===
@JvmInline
value class EntityId(val id: Int)

// === World ===
class World {
    // 实体管理
    fun createEntity(): EntityId
    fun destroyEntity(id: EntityId)
    fun isAlive(id: EntityId): Boolean

    // 组件管理
    fun <T : Any> addComponent(id: EntityId, component: T)
    fun <T : Any> removeComponent(id: EntityId, type: KClass<T>)
    fun <T : Any> getComponent(id: EntityId, type: KClass<T>): T?
    fun <T : Any> hasComponent(id: EntityId, type: KClass<T>): Boolean

    // 查询
    fun entitiesWith(vararg types: KClass<*>): List<EntityId>

    // System 管理
    fun addSystem(system: GameSystem)
    fun update()  // 按注册顺序依次调用所有 System.update()
}

// === GameSystem ===
interface GameSystem {
    val priority: Int get() = 0  // 越小越先执行
    fun update(world: World)
}
```

#### 扩展函数（便捷写法）

```kotlin
inline fun <reified T : Any> World.get(id: EntityId): T? = getComponent(id, T::class)
inline fun <reified T : Any> World.has(id: EntityId): Boolean = hasComponent(id, T::class)
inline fun <reified T : Any> World.add(id: EntityId, component: T) = addComponent(id, component)
```

#### MVP 组件清单

以下是 Phase 1 需要的全部 Component 类型：

```kotlin
// --- 基础 ---
data class Position(var x: Int, var y: Int)
data class Renderable(val glyph: Char, val foreground: Color, val background: Color = Color.BLACK)
data class Name(val value: String)
data class BlocksMovement(val value: Boolean = true)

// --- 战斗与属性 ---
data class Stats(
    var str: Int = 10,    // 力量
    var dex: Int = 10,    // 敏捷
    var con: Int = 10,    // 体质
    var wil: Int = 10     // 意志
)

data class DerivedStats(
    var attack: Int = 0,
    var defense: Int = 0,
    var accuracy: Int = 0,
    var evasion: Int = 0,
    var speed: Int = 100,
    var critChance: Double = 0.05,
    var maxHp: Int = 0,
    var maxStamina: Int = 0,
    var hpRegen: Double = 0.0,
    var staminaRegen: Double = 3.0,
    var talentPowerBonus: Double = 0.0
)

data class Health(var current: Int, var max: Int)
data class Stamina(var current: Int, var max: Int)
data class Energy(var current: Int = 0)
data class Speed(var value: Int = 100)

// --- 升级 ---
data class Experience(
    var current: Int = 0,
    var level: Int = 1,
    var unspentStatPoints: Int = 0,
    var unspentTalentPoints: Int = 0
)

// --- 物品 ---
data class Item(
    val itemType: ItemType,
    val material: Material? = null,
    val prefix: Affix? = null,
    val suffix: Affix? = null,
    val stats: ItemStats = ItemStats()
)

data class ItemStats(
    var attackBonus: Int = 0,
    var defenseBonus: Int = 0,
    var speedBonus: Int = 0,
    var maxHpBonus: Int = 0
)

data class Inventory(
    val items: MutableList<EntityId> = mutableListOf(),
    val capacity: Int = 20
)

data class Equipment(
    val slots: MutableMap<EquipSlot, EntityId?> = mutableMapOf()
)

// --- AI ---
data class AIBehavior(val type: AIType)

// --- 天赋 ---
data class TalentSet(
    val talents: MutableMap<String, TalentInstance> = mutableMapOf()
)

data class TalentInstance(
    val talentId: String,
    var level: Int = 1,
    var cooldownRemaining: Int = 0
)

// --- 状态效果 ---
data class StatusEffects(
    val effects: MutableList<StatusEffect> = mutableListOf()
)

data class StatusEffect(
    val type: StatusEffectType,
    var duration: Int,
    var magnitude: Double = 0.0
)

// --- 标记 ---
class PlayerTag                // 标记玩家实体
class MonsterTag               // 标记怪物实体
class ItemTag                  // 标记地面物品
data class StairDown(val targetFloor: Int)
data class StairUp(val targetFloor: Int)
```

#### 枚举定义

```kotlin
enum class AIType { CHASE, KITE, PATROL }

enum class EquipSlot { WEAPON, ARMOR, HELMET, BOOTS, RING_LEFT, RING_RIGHT }

enum class ItemType { WEAPON, ARMOR, HELMET, BOOTS, RING, POTION, SCROLL }

enum class Material(val attackMod: Double, val defenseMod: Double, val label: String) {
    IRON(1.0, 1.0, "铁"),
    STEEL(1.15, 1.1, "钢"),
    MITHRIL(1.3, 1.2, "秘银"),
    ADAMANTITE(1.5, 1.35, "精金")
}

enum class StatusEffectType {
    STUN,        // 跳过回合
    BLEED,       // 每回合持续伤害
    ARMOR_BREAK, // 减少防御
    SLOW         // 减速
}
```

#### 测试要求（WorldTest）

| 测试用例 | 描述 |
|---------|------|
| `createEntity` | 创建实体返回唯一 ID |
| `destroyEntity` | 销毁后 `isAlive` 返回 false |
| `addAndGetComponent` | 添加组件后可正确获取 |
| `removeComponent` | 移除组件后 `getComponent` 返回 null |
| `entitiesWith` | 只返回同时拥有指定组件的实体 |
| `destroyedEntityNotInQuery` | 已销毁实体不出现在查询结果中 |
| `systemUpdateOrder` | System 按 priority 顺序执行 |
| `entityIdReuse` | 销毁后的 ID 不会被复用（避免悬挂引用） |

---

### 2.2 回合调度器（TurnScheduler）

#### 算法：能量系统

```
循环：
  1. 遍历所有参与调度的实体
  2. 每个实体 energy += speed
  3. 如果 energy >= 100：
     - energy -= 100
     - 该实体获得行动机会
     - 如果是玩家：暂停调度，等待输入
     - 如果是怪物：执行 AI 决策
  4. 如果同一 tick 多个实体达到 100：按 entityId 排序（稳定、确定性）
```

#### 接口

```kotlin
class TurnScheduler {
    fun addEntity(id: EntityId, speed: Int)
    fun removeEntity(id: EntityId)
    fun updateSpeed(id: EntityId, speed: Int)

    /**
     * 推进时间直到下一个实体可以行动。
     * @return 获得行动机会的实体 ID
     */
    fun tick(): EntityId

    /**
     * 批量推进，返回本次 tick 中所有获得行动机会的实体（按优先级排序）。
     */
    fun tickAll(): List<EntityId>
}
```

#### 速度参考值

| 速度 | 含义 | 示例 |
|------|------|------|
| 50 | 半速 | 2 个 tick 行动 1 次 |
| 100 | 标准 | 每 tick 行动 1 次 |
| 130 | 快速 | 约每 0.77 tick 行动 1 次 |
| 200 | 双速 | 每 tick 行动 2 次 |

#### 测试要求（TurnSchedulerTest）

| 测试用例 | 描述 |
|---------|------|
| `speed100ActsEveryTick` | speed=100 每 tick 行动 1 次 |
| `speed200ActsTwicePerTick` | speed=200 每 tick 行动 2 次 |
| `speed50ActsEveryOtherTick` | speed=50 每 2 tick 行动 1 次 |
| `multipleEntitiesOrdering` | 同 tick 内多实体按 entityId 排序 |
| `removeEntityStopsActions` | 移除后不再获得行动 |
| `speedChangeAffectsNextTick` | 速度变更立即生效 |
| `deterministicWithSameSetup` | 相同初始状态产生相同行动序列 |

---

### 2.3 地图系统（GameMap + BSP 生成）

#### GameMap 数据结构

```kotlin
class GameMap(val width: Int, val height: Int) {
    // 每个格子的 tile 类型
    val tiles: Array<Array<TileType>>

    // FOV 相关
    val visible: Array<BooleanArray>     // 当前视野内
    val explored: Array<BooleanArray>    // 曾经看到过

    fun isInBounds(x: Int, y: Int): Boolean
    fun isWalkable(x: Int, y: Int): Boolean
    fun isTransparent(x: Int, y: Int): Boolean
    fun setTile(x: Int, y: Int, tile: TileType)
    fun getTile(x: Int, y: Int): TileType

    // 查找
    fun findRandomFloorTile(rng: Random): Pair<Int, Int>?
}

enum class TileType(
    val walkable: Boolean,
    val transparent: Boolean,
    val glyph: Char,
    val description: String
) {
    WALL(false, false, '#', "墙壁"),
    FLOOR(true, true, '.', "地板"),
    DOOR_OPEN(true, true, '/', "开门"),
    DOOR_CLOSED(false, false, '+', "关门"),
    STAIR_DOWN(true, true, '>', "下行楼梯"),
    STAIR_UP(true, true, '<', "上行楼梯"),
    VOID(false, false, ' ', "虚空")
}
```

#### BSP 地图生成算法

Phase 1 的地图生成器使用 **Binary Space Partition (BSP)** 算法：

```
输入: width, height, seed
输出: GameMap（房间 + L 型走廊连接）

算法步骤:
1. 初始化 width × height 全为 WALL
2. 创建根节点 BSPNode(0, 0, width, height)
3. 递归分割：
   - 如果节点面积 > maxLeafSize：
     - 随机选择水平或垂直切割（长边优先）
     - 切割位置 = 30%-70% 之间随机
     - 生成两个子节点
   - 否则该节点为叶子节点
4. 在每个叶子节点内放置房间：
   - 房间大小 = 节点区域 - 随机 padding(1-3)
   - 最小房间: 5×5
5. 连接：自底向上遍历 BSP 树
   - 取两个兄弟节点的房间中心
   - 用 L 型走廊连接（先水平后垂直，或反之，随机选择）
6. 放置楼梯：
   - 上行楼梯：在离入口最近的房间
   - 下行楼梯：在离入口最远的房间
```

#### BSP 配置参数

```kotlin
data class BSPConfig(
    val width: Int = 80,
    val height: Int = 50,
    val maxLeafSize: Int = 20,   // 叶子节点最大面积
    val minRoomSize: Int = 5,    // 房间最小边长
    val roomPadding: Int = 1,    // 房间与叶子节点边界的最小间距
    val seed: Long               // 确定性种子
)
```

#### 接口

```kotlin
class BSPGenerator {
    fun generate(config: BSPConfig): GameMap

    // 暴露中间结果用于测试
    data class Room(val x: Int, val y: Int, val width: Int, val height: Int) {
        val centerX: Int get() = x + width / 2
        val centerY: Int get() = y + height / 2
    }
}
```

#### 多层地牢管理

```kotlin
class DungeonManager(private val config: BSPConfig, private val totalFloors: Int = 5) {
    private val floors: MutableMap<Int, GameMap> = mutableMapOf()
    private val baseSeed: Long

    fun getFloor(floor: Int): GameMap  // 懒加载，seed = baseSeed + floor
    fun getStairDownPosition(floor: Int): Pair<Int, Int>
    fun getStairUpPosition(floor: Int): Pair<Int, Int>
}
```

#### 测试要求（BSPGeneratorTest）

| 测试用例 | 描述 |
|---------|------|
| `allRoomsInBounds` | 所有房间在地图边界内 |
| `noRoomOverlap` | 房间之间不重叠 |
| `allRoomsConnected` | Flood fill 验证所有地板格子相连 |
| `seedDeterminism` | 同 seed 生成相同地图 |
| `differentSeedsDifferentMaps` | 不同 seed 生成不同地图 |
| `minimumRoomSize` | 所有房间 >= 5×5 |
| `stairsPlaced` | 楼梯正确放置在地板上 |
| `walkablePathBetweenStairs` | 上下楼梯之间存在可行走路径 |

---

### 2.4 视野系统（FOV — Shadowcasting）

#### 算法

使用 **Recursive Shadowcasting**（8 象限对称）：

```
输入: origin(x, y), radius, isTransparent(x, y) -> Boolean
输出: Set<Pair<Int, Int>>（可见坐标集合）

核心思想:
- 将视野分为 8 个象限
- 每个象限内，从距离 1 开始逐行扫描
- 维护当前扫描线的起始斜率和终止斜率
- 遇到墙壁时收缩可见范围
- 遇到墙壁→地板的边界时开始新的扫描段
```

#### 接口

```kotlin
object Shadowcasting {
    /**
     * 计算从 (originX, originY) 出发、半径 radius 内的所有可见坐标。
     * @param isTransparent 判断 (x, y) 是否透明（允许光线通过）
     * @return 可见坐标集合（包含原点）
     */
    fun computeFOV(
        originX: Int,
        originY: Int,
        radius: Int,
        isTransparent: (x: Int, y: Int) -> Boolean
    ): Set<Pair<Int, Int>>
}
```

#### FOV 参数

| 参数 | MVP 值 | 说明 |
|------|--------|------|
| 默认视野半径 | 8 | 玩家标准视野 |
| 最大视野半径 | 15 | 上限 |
| 暗处渲染 | explored 但 !visible 的格子灰暗显示 | |

#### 测试要求（ShadowcastingTest）

| 测试用例 | 描述 |
|---------|------|
| `emptyMapAllVisible` | 无墙地图中半径内全可见 |
| `wallBlocksSight` | 墙壁后面的格子不可见 |
| `originAlwaysVisible` | 原点永远可见 |
| `radiusLimitsVision` | 超出半径的格子不可见 |
| `cornerOcclusion` | 拐角遮挡正确 |
| `symmetry` | 如果 A 能看到 B，则 B 能看到 A（对称性） |
| `adjacentToWallVisible` | 紧邻墙壁的格子可见（但墙壁后不可见） |

---

### 2.5 寻路（A*）

#### 接口

```kotlin
object AStar {
    /**
     * A* 寻路。
     * @param start 起点
     * @param goal 终点
     * @param isWalkable 判断 (x, y) 是否可行走
     * @param width 地图宽度（边界检查）
     * @param height 地图高度
     * @param maxSteps 最大搜索步数（防止对大地图的性能退化），默认 1000
     * @return 路径（不包含起点，包含终点），无路径返回 null
     */
    fun findPath(
        startX: Int, startY: Int,
        goalX: Int, goalY: Int,
        isWalkable: (x: Int, y: Int) -> Boolean,
        width: Int, height: Int,
        maxSteps: Int = 1000
    ): List<Pair<Int, Int>>?
}
```

#### 实现要点

- 启发式函数：Chebyshev 距离（8 方向移动）
- 支持 8 方向移动（对角移动不能穿墙角：对角方向要求两个相邻正交方向至少一个可通行）
- 返回路径不包含起点
- 无可达路径返回 null

#### 测试要求（AStarTest）

| 测试用例 | 描述 |
|---------|------|
| `directPath` | 无障碍直线路径 |
| `pathAroundWall` | 绕墙行走 |
| `noPathBlocked` | 完全封闭返回 null |
| `diagonalMovement` | 支持对角移动 |
| `noDiagonalThroughWallCorner` | 不穿墙角对角移动 |
| `maxStepsLimit` | 超过 maxSteps 返回 null |

---

### 2.6 战斗系统（CombatResolver）

#### 战斗流程

```
1. 命中判定
   hitChance = 0.85 + (attackerAccuracy - targetEvasion) * 0.01
   hitChance = clamp(hitChance, 0.05, 0.95)  // 5% 保底命中，5% 保底闪避
   if random() > hitChance → MISS

2. 暴击判定
   critChance = 0.05 + attackerDex * 0.002
   critChance = clamp(critChance, 0.0, 0.50)
   isCrit = random() < critChance

3. 伤害计算
   baseDamage = attackerAttack + random(-2, 2)
   reduced = max(0, baseDamage - targetDefense)
   finalDamage = max(1, reduced)              // 最少 1 点伤害
   if isCrit: finalDamage = (finalDamage * 1.5).toInt()

4. 应用伤害
   target.hp -= finalDamage
   if target.hp <= 0 → 触发死亡事件

5. 返回战斗结果
```

#### 接口

```kotlin
data class CombatResult(
    val attacker: EntityId,
    val target: EntityId,
    val hit: Boolean,
    val crit: Boolean,
    val damage: Int,
    val targetKilled: Boolean
)

class CombatResolver(private val rng: Random) {
    /**
     * 解算一次近战攻击。
     * 不直接修改 World 状态，只返回结果。由调用方根据结果更新状态。
     */
    fun resolveMelee(
        attackerAttack: Int,
        attackerAccuracy: Int,
        attackerDex: Int,
        targetDefense: Int,
        targetEvasion: Int,
        targetCurrentHp: Int
    ): CombatResult

    // 便捷方法：从 World 中读取组件并解算
    fun resolveMelee(world: World, attacker: EntityId, target: EntityId): CombatResult
}
```

#### 设计决策

- `CombatResolver` 接受 `Random` 注入，使战斗结果在测试中完全确定性可控。
- 不在 `CombatResolver` 内部修改 World 状态——解耦计算与副作用，便于测试和事件分发。

#### 测试要求（CombatResolverTest）

| 测试用例 | 描述 |
|---------|------|
| `highAttackVsLowDefense` | ATK > DEF 造成正伤害 |
| `lowAttackVsHighDefense` | ATK <= DEF 至少造成 1 点伤害 |
| `critDamageIs150Percent` | 暴击伤害 = base × 1.5 |
| `zeroEvasionAlwaysHit` | 高 accuracy vs 0 evasion → 接近 100% 命中 |
| `highEvasionReducesHitRate` | 高 evasion 降低命中率 |
| `guaranteedMinHitChance` | 再高的 evasion 也有 5% 保底命中 |
| `guaranteedMaxHitChance` | 再高的 accuracy 也有 5% 保底闪避 |
| `targetKilledWhenHpDepleted` | HP 降至 0 时 targetKilled = true |
| `deterministicWithSameRng` | 相同 Random seed 产生相同结果 |

---

### 2.7 属性派生系统

#### 基础属性 → 派生属性 公式

```
attack      = baseAttack + str * 2 + equipmentAttackBonus
defense     = baseDefense + equipmentDefenseBonus
accuracy    = baseAccuracy + dex * 1
evasion     = baseEvasion + dex * 1
speed       = baseSpeed + dex * 0.5 (取整)
critChance  = 0.05 + dex * 0.002
maxHp       = baseHp + con * 8
maxStamina  = baseStamina + wil * 5
hpRegen     = baseHpRegen + con * 0.2
staminaRegen = 3.0  (固定)
talentPower = 1.0 + wil * 0.01
```

#### 基础值（1级战士）

```kotlin
val BASE_ATTACK = 5
val BASE_DEFENSE = 2
val BASE_ACCURACY = 10
val BASE_EVASION = 5
val BASE_SPEED = 100
val BASE_HP = 50
val BASE_STAMINA = 40
val BASE_HP_REGEN = 1.0
```

#### 接口

```kotlin
object StatsCalculator {
    /**
     * 根据基础属性 + 装备 计算全部派生属性。
     * 纯函数，无副作用。
     */
    fun calculate(stats: Stats, equipment: Equipment, world: World): DerivedStats
}
```

#### 升级规则

```
经验值公式: nextLevelExp = level * 100 + (level - 1) * 50
升级获得:
  - 2 个属性点（自由分配到 STR/DEX/CON/WIL）
  - 1 个天赋点（每 2 级）
  - HP/Stamina 全回满

最大等级: 20（Phase 1）
```

| 等级 | 所需经验 | 累计经验 | 属性点 | 天赋点 |
|------|---------|---------|--------|--------|
| 1→2 | 150 | 150 | 2 | 0 |
| 2→3 | 250 | 400 | 2 | 1 |
| 3→4 | 350 | 750 | 2 | 0 |
| 4→5 | 450 | 1200 | 2 | 1 |
| ... | ... | ... | ... | ... |
| 19→20 | 1950 | 21000 | 2 | 1 |

---

### 2.8 天赋系统

#### 数据模型

```kotlin
data class TalentDef(
    val id: String,
    val name: String,
    val description: String,
    val maxLevel: Int = 5,
    val staminaCost: Int,
    val cooldown: Int,           // 回合数
    val range: Int,              // 格数，0 = 自身，1 = 邻格
    val areaRadius: Int = 0,     // AOE 半径，0 = 单体
    val levelEffects: Map<Int, TalentLevelEffect>  // 每级效果
)

data class TalentLevelEffect(
    val damageMultiplier: Double = 1.0,
    val knockback: Int = 0,
    val stunDuration: Int = 0,
    val armorBreakDuration: Int = 0,
    val buffDuration: Int = 0,
    val buffMagnitude: Double = 0.0,
    val debuffMagnitude: Double = 0.0,
    val debuffDuration: Int = 0
)
```

#### 4 个战士天赋详细定义

**猛击 (Power Strike)**

```yaml
id: power_strike
name: 猛击
staminaCost: 8
cooldown: 3
range: 1
levels:
  1: { damageMultiplier: 1.5 }
  2: { damageMultiplier: 1.7 }
  3: { damageMultiplier: 1.9 }
  4: { damageMultiplier: 2.2 }
  5: { damageMultiplier: 2.5, knockback: 1, armorBreakDuration: 3 }
```

**冲锋 (Charge)**

```yaml
id: charge
name: 冲锋
staminaCost: 12
cooldown: 6
range: 5        # 3-5 格
minRange: 3
levels:
  1: { damageMultiplier: 1.2 }
  2: { damageMultiplier: 1.3 }
  3: { damageMultiplier: 1.5 }
  4: { damageMultiplier: 1.6 }
  5: { damageMultiplier: 1.8, stunDuration: 2 }
```

**盾击 (Shield Bash)**

```yaml
id: shield_bash
name: 盾击
staminaCost: 10
cooldown: 5
range: 1
levels:
  1: { damageMultiplier: 1.2, stunDuration: 2 }
  2: { damageMultiplier: 1.3, stunDuration: 2 }
  3: { damageMultiplier: 1.4, stunDuration: 2 }
  4: { damageMultiplier: 1.5, stunDuration: 3 }
  5: { damageMultiplier: 1.6, stunDuration: 3, knockback: 2 }
```

**战吼 (War Cry)**

```yaml
id: war_cry
name: 战吼
staminaCost: 15
cooldown: 10
range: 0         # 自身释放
areaRadius: 3    # 3 格 AOE
levels:
  1: { buffDuration: 5, buffMagnitude: 0.20 }        # +20% ATK
  2: { buffDuration: 5, buffMagnitude: 0.25 }
  3: { buffDuration: 6, buffMagnitude: 0.28 }
  4: { buffDuration: 7, buffMagnitude: 0.32 }
  5: { buffDuration: 8, buffMagnitude: 0.35, debuffMagnitude: 0.20, debuffDuration: 5 }
```

#### TalentResolver 接口

```kotlin
class TalentResolver {
    /**
     * 检查天赋是否可用。
     * @return null 表示可用；否则返回不可用原因
     */
    fun canUse(
        world: World,
        user: EntityId,
        talentId: String,
        targetX: Int, targetY: Int
    ): String?  // "冷却中" / "体力不足" / "超出范围" / "目标无效"

    /**
     * 执行天赋效果。
     * @return 天赋使用结果（用于消息日志和事件分发）
     */
    fun resolve(
        world: World,
        user: EntityId,
        talentId: String,
        targetX: Int, targetY: Int,
        rng: Random
    ): TalentResult
}

data class TalentResult(
    val talentId: String,
    val user: EntityId,
    val targets: List<EntityId>,
    val effects: List<TalentEffectResult>
)

sealed class TalentEffectResult {
    data class Damage(val target: EntityId, val amount: Int, val crit: Boolean) : TalentEffectResult()
    data class Knockback(val target: EntityId, val dx: Int, val dy: Int) : TalentEffectResult()
    data class StatusApplied(val target: EntityId, val type: StatusEffectType, val duration: Int) : TalentEffectResult()
    data class Buff(val target: EntityId, val type: String, val duration: Int, val magnitude: Double) : TalentEffectResult()
    data class Movement(val entity: EntityId, val toX: Int, val toY: Int) : TalentEffectResult()
}
```

#### TalentRegistry

```kotlin
class TalentRegistry {
    private val talents: MutableMap<String, TalentDef> = mutableMapOf()

    fun register(talent: TalentDef)
    fun get(id: String): TalentDef?
    fun getAll(): Map<String, TalentDef>
}
```

#### 测试要求（TalentResolverTest）

| 测试用例 | 描述 |
|---------|------|
| `cannotUseOnCooldown` | 冷却中返回错误 |
| `cannotUseWithoutStamina` | 体力不足返回错误 |
| `cannotUseOutOfRange` | 超出范围返回错误 |
| `powerStrikeDamageMultiplier` | 猛击各级伤害倍率正确 |
| `powerStrikeLv5KnockbackAndArmorBreak` | Lv5 附带击退和破甲 |
| `chargeMoveToTarget` | 冲锋将玩家移动到目标旁 |
| `chargeMinRangeCheck` | 冲锋距离不足不可用 |
| `shieldBashStun` | 盾击正确附加眩晕 |
| `warCryBuffSelf` | 战吼正确 buff 自身攻击力 |
| `warCryLv5DebuffEnemies` | Lv5 战吼同时 debuff 范围内敌人 |
| `cooldownDecrementsPerTurn` | 每回合冷却减 1 |
| `staminaConsumedOnUse` | 使用后扣除体力 |

---

### 2.9 物品系统

#### 物品生成规则

```
物品 = 基础类型 × 材质(可选) × 品质决定词缀数

品质滚动（按地牢层数加权）:
  白色(0 词缀): 基础权重 60, 每层 -5
  绿色(1 词缀): 基础权重 30, 每层 +3
  蓝色(2 词缀): 基础权重 10, 每层 +2

材质滚动（按地牢层数加权）:
  铁:       层 1-5 均可
  钢:       层 2+ 开始出现
  秘银:     层 3+ 开始出现
  精金:     层 4+ 开始出现
```

#### 词缀系统

```kotlin
data class Affix(
    val id: String,
    val name: String,
    val type: AffixType,  // PREFIX or SUFFIX
    val statModifiers: ItemStats,
    val minFloor: Int = 1   // 最早出现楼层
)

enum class AffixType { PREFIX, SUFFIX }
```

**Phase 1 词缀表**：

| 词缀 | 类型 | 效果 | 最低层数 |
|------|------|------|---------|
| 锐利的 | PREFIX | attack +3 | 1 |
| 坚固的 | PREFIX | defense +2 | 1 |
| 嗜血的 | PREFIX | 击杀回复 5 HP | 3 |
| 迅捷的 | PREFIX | speed +10 | 2 |
| ...力量之 | SUFFIX | attack +2, str 等效 +1 | 1 |
| ...速度之 | SUFFIX | speed +15 | 2 |
| ...生命之 | SUFFIX | maxHp +15 | 1 |
| ...坚韧之 | SUFFIX | defense +3 | 2 |

#### 物品基础定义

```kotlin
data class ItemBaseDef(
    val id: String,
    val name: String,
    val type: ItemType,
    val slot: EquipSlot,
    val baseStats: ItemStats,
    val glyph: Char
)
```

**Phase 1 基础物品**：

| ID | 名称 | 类型 | 基础攻击 | 基础防御 | 字符 |
|----|------|------|---------|---------|------|
| short_sword | 短剑 | WEAPON | 5 | 0 | `)`  |
| long_sword | 长剑 | WEAPON | 8 | 0 | `)` |
| battle_axe | 战斧 | WEAPON | 11 | 0 | `)` |
| leather_armor | 皮甲 | ARMOR | 0 | 3 | `[` |
| chain_mail | 锁甲 | ARMOR | 0 | 6 | `[` |
| plate_armor | 板甲 | ARMOR | 0 | 10 | `[` |
| iron_helmet | 铁盔 | HELMET | 0 | 2 | `]` |
| leather_boots | 皮靴 | BOOTS | 0 | 1 | `]` |
| healing_potion | 治疗药水 | POTION | - | - | `!` |
| scroll_teleport | 传送卷轴 | SCROLL | - | - | `?` |

#### 接口

```kotlin
class ItemGenerator(private val rng: Random) {
    /**
     * 根据地牢层数生成随机物品。
     */
    fun generate(floor: Int): Item

    /**
     * 使用确定性种子生成。
     */
    fun generate(floor: Int, seed: Long): Item
}
```

#### Inventory 接口

```kotlin
class InventoryManager {
    fun pickUp(world: World, entity: EntityId, item: EntityId): Boolean  // false = 满了
    fun drop(world: World, entity: EntityId, itemIndex: Int): Boolean
    fun equip(world: World, entity: EntityId, itemIndex: Int): Boolean
    fun unequip(world: World, entity: EntityId, slot: EquipSlot): Boolean
    fun useConsumable(world: World, entity: EntityId, itemIndex: Int): Boolean
}
```

#### 测试要求（ItemGeneratorTest）

| 测试用例 | 描述 |
|---------|------|
| `materialAppliesCorrectly` | 材质 modifier 正确应用到 stats |
| `affixCountMatchesQuality` | 词缀数量符合品质规则 |
| `seedDeterminism` | 相同 seed 生成相同物品 |
| `floorAffectsMaterialPool` | 高层出现高级材质 |
| `floorAffectsQualityWeight` | 高层蓝色物品概率更高 |
| `consumableUsageRemovesItem` | 使用消耗品后从背包移除 |
| `equipmentGoesToCorrectSlot` | 装备放入正确槽位 |
| `inventoryCapacityEnforced` | 背包满时无法拾取 |

---

### 2.10 怪物 AI 系统

#### 3 种 AI 行为

**Chase（追击型）**
```
if 目标在视野内:
    用 A* 寻路向目标移动
    if 相邻: 近战攻击
else:
    随机漫游（向随机可行走方向移动一步）
```

**Kite（风筝型）**
```
if 目标在攻击范围(2-3格)内:
    远程攻击（Phase 1 简化为近战）
elif 目标太近(1格):
    远离目标移动（选择使距离增大的方向）
elif 目标在视野内但不在攻击范围:
    向目标移动
else:
    随机漫游
```

**Patrol（巡逻型）**
```
if 目标在视野内:
    切换为 chase 模式
else:
    沿预设路径点循环移动
    if 到达当前路径点: 切换到下一个路径点
```

#### 接口

```kotlin
class AISystem : GameSystem {
    override val priority: Int = 50

    override fun update(world: World) // 在 TurnScheduler 分配行动时调用
}

class AIDecisionMaker {
    /**
     * 为给定 AI 实体做出行动决策。
     * @return 要执行的 Action
     */
    fun decide(
        world: World,
        entity: EntityId,
        playerPos: Position,
        gameMap: GameMap
    ): Action
}

sealed class Action {
    data class Move(val dx: Int, val dy: Int) : Action()
    data class MeleeAttack(val target: EntityId) : Action()
    data class UseTalent(val talentId: String, val targetX: Int, val targetY: Int) : Action()
    object Wait : Action()
}
```

#### 测试要求（AIDecisionTest）

| 测试用例 | 描述 |
|---------|------|
| `chaseMovesTowardTarget` | chase AI 向目标移动 |
| `chaseAttacksWhenAdjacent` | chase AI 相邻时攻击 |
| `chaseWandersWhenNoTarget` | 目标不在视野时随机移动 |
| `kiteRetreatsTooClose` | kite AI 目标太近时后退 |
| `kiteAttacksInRange` | kite AI 在攻击范围内攻击 |
| `patrolFollowsWaypoints` | patrol AI 沿路径点移动 |
| `patrolSwitchesToChase` | patrol AI 发现目标后切换为追击 |

---

### 2.11 事件系统（EventBus）

#### 设计

轻量级同步事件总线，用于解耦系统间通信。

```kotlin
class EventBus {
    fun <T : GameEvent> subscribe(eventType: KClass<T>, handler: (T) -> Unit)
    fun <T : GameEvent> unsubscribe(eventType: KClass<T>, handler: (T) -> Unit)
    fun publish(event: GameEvent)
}

// 内联便捷函数
inline fun <reified T : GameEvent> EventBus.on(noinline handler: (T) -> Unit) =
    subscribe(T::class, handler)
```

#### Phase 1 事件清单

```kotlin
sealed class GameEvent

// 实体事件
data class EntityDeathEvent(val entity: EntityId, val killer: EntityId?) : GameEvent()
data class EntitySpawnEvent(val entity: EntityId) : GameEvent()

// 战斗事件
data class DamageDealtEvent(
    val attacker: EntityId, val target: EntityId,
    val damage: Int, val crit: Boolean
) : GameEvent()
data class MissEvent(val attacker: EntityId, val target: EntityId) : GameEvent()

// 物品事件
data class ItemPickedUpEvent(val entity: EntityId, val item: EntityId) : GameEvent()
data class ItemEquippedEvent(val entity: EntityId, val item: EntityId, val slot: EquipSlot) : GameEvent()
data class ItemUsedEvent(val entity: EntityId, val item: EntityId) : GameEvent()

// 天赋事件
data class TalentUsedEvent(val entity: EntityId, val talentId: String) : GameEvent()

// 进度事件
data class LevelUpEvent(val entity: EntityId, val newLevel: Int) : GameEvent()
data class FloorChangedEvent(val entity: EntityId, val newFloor: Int) : GameEvent()
data class ExpGainedEvent(val entity: EntityId, val amount: Int) : GameEvent()

// 游戏状态事件
data class GameOverEvent(val victory: Boolean) : GameEvent()
```

---

### 2.12 存档系统（SaveManager）

#### 设计决策

- Phase 1 使用 JSON 序列化（Jackson）
- 单存档文件
- Permadeath：死亡时删除存档
- 存档时机：切换楼层、手动保存（Ctrl+S）、退出游戏

#### 存档数据结构

```kotlin
data class SaveData(
    val version: String,           // "0.1.0"
    val timestamp: Long,
    val currentFloor: Int,
    val dungeonSeed: Long,         // 用于重新生成地牢
    val playerData: PlayerSaveData,
    val entityData: List<EntitySaveData>,  // 当前层的所有非玩家实体
    val exploredMaps: Map<Int, ExploredMapData>  // 已探索的楼层记录
)

data class PlayerSaveData(
    val position: Position,
    val stats: Stats,
    val health: Health,
    val stamina: Stamina,
    val experience: Experience,
    val inventory: List<ItemSaveData>,
    val equipment: Map<EquipSlot, ItemSaveData?>,
    val talents: Map<String, TalentInstance>
)

data class EntitySaveData(
    val type: String,              // 怪物/物品模板 ID
    val position: Position,
    val health: Health?,
    val components: Map<String, Any>  // 额外组件序列化
)
```

#### 接口

```kotlin
class SaveManager(private val saveDir: Path) {
    fun save(world: World, gameState: GameState): Boolean
    fun load(): SaveData?
    fun hasSave(): Boolean
    fun deleteSave()  // Permadeath 调用
}
```

---

## 3. Game 模块设计

Game 模块负责将 Core 的通用引擎组装成具体的游戏内容。

### 3.1 YAML 数据格式

#### monsters.yaml

```yaml
monsters:
  - id: rat
    name: 老鼠
    glyph: "r"
    color: "#8B4513"
    stats: { str: 3, dex: 8, con: 4, wil: 1 }
    baseHp: 12
    baseAttack: 3
    baseDefense: 0
    speed: 110
    ai: CHASE
    expReward: 15
    spawnFloors: [1, 2]
    spawnWeight: 10

  - id: skeleton
    name: 骷髅
    glyph: "s"
    color: "#FFFFFF"
    stats: { str: 8, dex: 5, con: 6, wil: 2 }
    baseHp: 25
    baseAttack: 6
    baseDefense: 3
    speed: 90
    ai: CHASE
    expReward: 30
    spawnFloors: [1, 2, 3]
    spawnWeight: 8

  - id: archer
    name: 骷髅弓手
    glyph: "a"
    color: "#CCCCCC"
    stats: { str: 5, dex: 10, con: 5, wil: 3 }
    baseHp: 18
    baseAttack: 7
    baseDefense: 1
    speed: 100
    ai: KITE
    expReward: 35
    spawnFloors: [2, 3, 4]
    spawnWeight: 6

  - id: orc
    name: 兽人
    glyph: "o"
    color: "#00AA00"
    stats: { str: 12, dex: 6, con: 10, wil: 3 }
    baseHp: 45
    baseAttack: 10
    baseDefense: 5
    speed: 90
    ai: CHASE
    expReward: 60
    spawnFloors: [3, 4, 5]
    spawnWeight: 5

  - id: boss_warlord
    name: 兽人督军
    glyph: "W"
    color: "#FF0000"
    stats: { str: 18, dex: 10, con: 16, wil: 8 }
    baseHp: 120
    baseAttack: 18
    baseDefense: 10
    speed: 100
    ai: CHASE
    expReward: 500
    spawnFloors: [5]
    spawnWeight: 0     # 不随机生成，脚本放置
    boss: true
    talents:
      - power_strike: 3    # Boss 拥有 3 级猛击
      - war_cry: 2
```

#### items.yaml

```yaml
weapons:
  - id: short_sword
    name: 短剑
    glyph: ")"
    color: "#C0C0C0"
    slot: WEAPON
    baseAttack: 5
    materials: [IRON, STEEL, MITHRIL, ADAMANTITE]
    dropFloors: [1, 2, 3, 4, 5]
    dropWeight: 10

  - id: long_sword
    name: 长剑
    glyph: ")"
    color: "#C0C0C0"
    slot: WEAPON
    baseAttack: 8
    materials: [IRON, STEEL, MITHRIL, ADAMANTITE]
    dropFloors: [2, 3, 4, 5]
    dropWeight: 7

  - id: battle_axe
    name: 战斧
    glyph: ")"
    color: "#C0C0C0"
    slot: WEAPON
    baseAttack: 11
    materials: [STEEL, MITHRIL, ADAMANTITE]
    dropFloors: [3, 4, 5]
    dropWeight: 4

armors:
  - id: leather_armor
    name: 皮甲
    glyph: "["
    color: "#8B4513"
    slot: ARMOR
    baseDefense: 3
    dropFloors: [1, 2, 3]
    dropWeight: 8

  - id: chain_mail
    name: 锁甲
    glyph: "["
    color: "#808080"
    slot: ARMOR
    baseDefense: 6
    dropFloors: [2, 3, 4]
    dropWeight: 5

  - id: plate_armor
    name: 板甲
    glyph: "["
    color: "#C0C0C0"
    slot: ARMOR
    baseDefense: 10
    dropFloors: [4, 5]
    dropWeight: 3

consumables:
  - id: healing_potion
    name: 治疗药水
    glyph: "!"
    color: "#FF0000"
    effect: HEAL
    magnitude: 30       # 回复 30 HP
    dropFloors: [1, 2, 3, 4, 5]
    dropWeight: 15

  - id: scroll_teleport
    name: 传送卷轴
    glyph: "?"
    color: "#00FFFF"
    effect: TELEPORT     # 随机传送到当前层某个地板格
    dropFloors: [1, 2, 3, 4, 5]
    dropWeight: 8
```

#### talents.yaml

```yaml
talents:
  - id: power_strike
    name: 猛击
    description: "用全力挥出一击。高等级附带击退和破甲。"
    staminaCost: 8
    cooldown: 3
    range: 1
    areaRadius: 0
    maxLevel: 5
    levels:
      1: { damageMultiplier: 1.5 }
      2: { damageMultiplier: 1.7 }
      3: { damageMultiplier: 1.9 }
      4: { damageMultiplier: 2.2 }
      5: { damageMultiplier: 2.5, knockback: 1, armorBreakDuration: 3 }

  - id: charge
    name: 冲锋
    description: "冲向远处敌人发动攻击。高等级附带眩晕。"
    staminaCost: 12
    cooldown: 6
    range: 5
    minRange: 3
    areaRadius: 0
    maxLevel: 5
    levels:
      1: { damageMultiplier: 1.2 }
      2: { damageMultiplier: 1.3 }
      3: { damageMultiplier: 1.5 }
      4: { damageMultiplier: 1.6 }
      5: { damageMultiplier: 1.8, stunDuration: 2 }

  - id: shield_bash
    name: 盾击
    description: "用盾牌猛击敌人，造成伤害并眩晕。"
    staminaCost: 10
    cooldown: 5
    range: 1
    areaRadius: 0
    maxLevel: 5
    levels:
      1: { damageMultiplier: 1.2, stunDuration: 2 }
      2: { damageMultiplier: 1.3, stunDuration: 2 }
      3: { damageMultiplier: 1.4, stunDuration: 2 }
      4: { damageMultiplier: 1.5, stunDuration: 3 }
      5: { damageMultiplier: 1.6, stunDuration: 3, knockback: 2 }

  - id: war_cry
    name: 战吼
    description: "发出战吼，增强自身攻击力。高等级同时削弱范围内敌人。"
    staminaCost: 15
    cooldown: 10
    range: 0
    areaRadius: 3
    maxLevel: 5
    levels:
      1: { buffDuration: 5, buffMagnitude: 0.20 }
      2: { buffDuration: 5, buffMagnitude: 0.25 }
      3: { buffDuration: 6, buffMagnitude: 0.28 }
      4: { buffDuration: 7, buffMagnitude: 0.32 }
      5: { buffDuration: 8, buffMagnitude: 0.35, debuffMagnitude: 0.20, debuffDuration: 5 }
```

#### mapgen.yaml

```yaml
dungeon:
  totalFloors: 5
  floorConfig:
    default:
      width: 80
      height: 50
      maxLeafSize: 20
      minRoomSize: 5
      roomPadding: 1
    floor5:                  # Boss 层特殊配置
      width: 80
      height: 50
      maxLeafSize: 30        # 更大的房间
      minRoomSize: 8

  monsterDensity:            # 每层怪物数量范围
    floor1: { min: 5, max: 8 }
    floor2: { min: 6, max: 10 }
    floor3: { min: 8, max: 12 }
    floor4: { min: 10, max: 14 }
    floor5: { min: 6, max: 8, boss: boss_warlord }

  itemDensity:               # 每层物品数量范围
    floor1: { min: 3, max: 5 }
    floor2: { min: 3, max: 6 }
    floor3: { min: 4, max: 7 }
    floor4: { min: 4, max: 8 }
    floor5: { min: 5, max: 8 }
```

### 3.2 YAML 加载器

```kotlin
class DataLoader(private val resourcePath: String = "/data") {
    fun loadMonsters(): Map<String, MonsterDef>
    fun loadItems(): ItemDataBundle       // weapons + armors + consumables
    fun loadTalents(): Map<String, TalentDef>
    fun loadMapConfig(): DungeonConfig
}
```

### 3.3 实体工厂

```kotlin
class EntityFactory(
    private val world: World,
    private val monsterDefs: Map<String, MonsterDef>,
    private val itemDefs: ItemDataBundle,
    private val talentDefs: Map<String, TalentDef>
) {
    fun createPlayer(x: Int, y: Int): EntityId
    fun createMonster(monsterId: String, x: Int, y: Int): EntityId
    fun createItem(item: Item, x: Int, y: Int): EntityId
    fun createStairDown(x: Int, y: Int, targetFloor: Int): EntityId
    fun createStairUp(x: Int, y: Int, targetFloor: Int): EntityId
}
```

### 3.4 GameState

```kotlin
class GameState(
    val world: World,
    val eventBus: EventBus,
    val turnScheduler: TurnScheduler,
    val dungeonManager: DungeonManager,
    val entityFactory: EntityFactory,
    val combatResolver: CombatResolver,
    val talentResolver: TalentResolver,
    val inventoryManager: InventoryManager,
    val saveManager: SaveManager
) {
    var currentFloor: Int = 1
    var playerId: EntityId = EntityId(-1)
    var gameOver: Boolean = false
    var victory: Boolean = false
    var turnCount: Int = 0

    fun newGame(seed: Long)
    fun changeFloor(newFloor: Int)
    fun processPlayerAction(action: Action)
    fun isPlayerTurn(): Boolean
}
```

### 3.5 楼层生成流程

```
fun populateFloor(floor: Int, gameMap: GameMap):
  1. 确定怪物数量 = random(monsterDensity[floor].min, monsterDensity[floor].max)
  2. 确定物品数量 = random(itemDensity[floor].min, itemDensity[floor].max)

  3. 怪物生成:
     for i in 1..monsterCount:
       - 从 spawnFloors 包含当前层的怪物中按 spawnWeight 加权随机选择
       - 在随机地板格子上放置（避开玩家和已有实体的位置）

  4. Boss 放置 (仅 floor 5):
     - 在离上行楼梯最远的房间中心放置 Boss

  5. 物品生成:
     for i in 1..itemCount:
       - 调用 ItemGenerator.generate(floor) 生成随机物品
       - 在随机地板格子上放置

  6. 楼梯放置:
     - 上行楼梯: 固定位置（玩家出生点附近）
     - 下行楼梯: 在最远房间（floor < 5 时）
```

---

## 4. Client 模块设计

Client 模块是 libGDX 的薄包装层，职责仅限于渲染、输入和音频。不包含任何游戏逻辑。

### 4.1 ASCII 渲染器

#### 屏幕布局（80×50 终端模拟）

```
┌────────────────────────────────────────────────────────────┬──────────────┐
│                                                            │  [侧边栏]    │
│                                                            │  HP: 80/100  │
│                                                            │  ST: 35/40   │
│                    地牢视图区域                               │  Lv: 5       │
│                    (60 × 40 字符)                           │  Exp: 450    │
│                                                            │  Floor: 3/5  │
│                                                            │              │
│                                                            │  STR: 14     │
│                                                            │  DEX: 11     │
│                                                            │  CON: 12     │
│                                                            │  WIL: 10     │
│                                                            │              │
│                                                            │  [装备]       │
│                                                            │  武器: 钢长剑 │
│                                                            │  护甲: 锁甲   │
│                                                            │              │
│                                                            │  [天赋]       │
│                                                            │  1.猛击 [就绪] │
│                                                            │  2.冲锋 [CD:3]│
│                                                            │  3.盾击 [就绪] │
│                                                            │  4.战吼 [CD:7]│
├────────────────────────────────────────────────────────────┴──────────────┤
│  [消息日志 - 最近 5 条]                                                    │
│  你攻击了骷髅，造成 12 点伤害。                                              │
│  骷髅攻击了你，造成 5 点伤害。                                               │
│  你拾取了 钢长剑 [锐利的]。                                                  │
│  你感到力量涌入身体！(升级到 Lv.5)                                           │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

#### 字符与颜色约定

| 实体 | 字符 | 颜色 |
|------|------|------|
| 玩家 | `@` | 白色 |
| 墙壁 | `#` | 深灰 |
| 地板 | `.` | 浅灰 |
| 上楼梯 | `<` | 黄色 |
| 下楼梯 | `>` | 黄色 |
| 关门 | `+` | 棕色 |
| 开门 | `/` | 棕色 |
| 老鼠 | `r` | 棕色 |
| 骷髅 | `s` | 白色 |
| 弓手 | `a` | 灰色 |
| 兽人 | `o` | 绿色 |
| Boss | `W` | 红色 |
| 武器 | `)` | 银色 |
| 防具 | `[` | 银色 |
| 药水 | `!` | 红色 |
| 卷轴 | `?` | 青色 |
| 已探索未可见 | 原字符 | 暗淡（alpha 0.3） |

#### 渲染器接口

```kotlin
class AsciiRenderer(
    private val font: BitmapFont,  // 等宽字体
    private val cellWidth: Float,
    private val cellHeight: Float
) {
    fun render(
        batch: SpriteBatch,
        gameMap: GameMap,
        world: World,
        cameraX: Int, cameraY: Int,  // 视口中心（通常跟随玩家）
        viewWidth: Int, viewHeight: Int
    )

    fun renderSidebar(batch: SpriteBatch, gameState: GameState)
    fun renderMessageLog(batch: SpriteBatch, messages: List<String>)
}
```

### 4.2 输入处理

#### 键位映射

| 按键 | 动作 |
|------|------|
| `↑` / `k` / `Numpad 8` | 向上移动 |
| `↓` / `j` / `Numpad 2` | 向下移动 |
| `←` / `h` / `Numpad 4` | 向左移动 |
| `→` / `l` / `Numpad 6` | 向右移动 |
| `y` / `Numpad 7` | 左上 |
| `u` / `Numpad 9` | 右上 |
| `b` / `Numpad 1` | 左下 |
| `n` / `Numpad 3` | 右下 |
| `.` / `Numpad 5` | 等待一回合 |
| `g` | 拾取脚下物品 |
| `i` | 打开背包 |
| `e` | 打开装备界面 |
| `>` | 下楼 |
| `<` | 上楼 |
| `1` - `4` | 使用天赋 1-4 |
| `x` | 检视模式（查看敌人信息） |
| `T` | 天赋分配界面 |
| `Ctrl+S` | 保存游戏 |
| `Esc` | 菜单 / 取消 |

#### 天赋使用流程

```
1. 玩家按 1-4
2. 检查天赋是否可用（canUse）
3. 如果需要选择目标（range > 0）：
   - 进入目标选择模式
   - 显示有效范围高亮
   - 方向键移动光标
   - Enter 确认 / Esc 取消
4. 如果不需选目标（range = 0，如战吼）：
   - 立即执行
5. 执行天赋 → 消耗回合
```

#### 接口

```kotlin
class InputHandler(private val gameState: GameState) {
    enum class InputMode {
        NORMAL,            // 正常移动/攻击
        TARGET_SELECT,     // 天赋目标选择
        INVENTORY,         // 背包界面
        EQUIPMENT,         // 装备界面
        TALENT_ASSIGN,     // 天赋分配
        STAT_ASSIGN,       // 属性分配
        EXAMINE,           // 检视模式
        GAME_OVER          // 游戏结束画面
    }

    var currentMode: InputMode = InputMode.NORMAL

    fun handleInput(keycode: Int): Action?
}
```

### 4.3 Screen 结构

```kotlin
// libGDX Screen 实现
class GameScreen(private val gameState: GameState) : Screen {
    // 主游戏画面，包含地牢视图 + 侧边栏 + 消息日志
}

class MainMenuScreen : Screen {
    // 新游戏 / 继续 / 退出
}

class GameOverScreen(private val victory: Boolean, private val stats: GameStats) : Screen {
    // 胜利/失败画面 + 统计
}

class InventoryScreen(private val gameState: GameState) : Screen {
    // 背包界面（叠加在 GameScreen 上）
}

class TalentScreen(private val gameState: GameState) : Screen {
    // 天赋分配界面
}

class StatScreen(private val gameState: GameState) : Screen {
    // 属性分配界面
}
```

### 4.4 消息日志

```kotlin
class MessageLog(private val maxMessages: Int = 100) {
    fun add(message: String, color: Color = Color.WHITE)
    fun getRecent(count: Int): List<Pair<String, Color>>
    fun clear()
}
```

消息颜色约定：

| 颜色 | 用途 |
|------|------|
| 白色 | 普通信息 |
| 红色 | 受到伤害 |
| 绿色 | 治疗/正面效果 |
| 黄色 | 升级/重要发现 |
| 灰色 | 环境描述 |
| 青色 | 天赋使用 |

### 4.5 游戏主循环

```kotlin
// 简化的主循环逻辑（在 GameScreen.render() 中）
fun gameLoop() {
    if (gameState.gameOver) return

    if (gameState.isPlayerTurn()) {
        // 等待玩家输入
        val action = inputHandler.handleInput(currentKeycode)
        if (action != null) {
            gameState.processPlayerAction(action)
        }
    } else {
        // AI 行动（可一帧处理多个 AI）
        while (!gameState.isPlayerTurn() && !gameState.gameOver) {
            val entityId = gameState.turnScheduler.tick()
            if (world.has<AIBehavior>(entityId)) {
                val aiAction = aiDecisionMaker.decide(...)
                executeAction(entityId, aiAction)
            }
        }
    }

    // 渲染
    renderer.render(...)
}
```

---

## 5. 阶段实现计划

Phase 1 整体拆分为 5 个子阶段，每个子阶段有明确的里程碑和出口标准。前一阶段的出口标准必须全部满足后，方可进入下一阶段。

> **详细执行文档入口**: `docs/phase1/roadmap.md`
>
> `docs/phase1/` 下已按 `1-1.0 ~ 1-5.0` 拆分为可执行的阶段开发文档。后续实现应以这些阶段文档中的测试门禁、自证命令和白盒验收步骤为准，而不是只依赖本节的摘要表格。

### Phase 1-1.0: 引擎基础 + 可验证

> **目标**: 搭建项目骨架，实现核心引擎（ECS、地图、视野），`@` 在地牢中移动。
> **前置依赖**: 无
> **产出版本**: 无发布版本（内部里程碑）

| # | 任务 | 详细内容 | 验收标准 |
|---|------|---------|---------|
| 1.1 | Gradle 多模块搭建 | 创建 root/core/game/client 三个模块，配置依赖关系和版本号 | `gradle build` 成功，`core` 模块无 libGDX 依赖 |
| 1.2 | ECS 核心 | 实现 `World`、`EntityId`、组件管理、查询、`GameSystem` | `WorldTest` 全部通过（8+ 测试用例） |
| 1.3 | 地图数据结构 + BSP 生成 | 实现 `GameMap`、`TileType`、`BSPGenerator` | `BSPGeneratorTest` 全部通过（8+ 测试用例） |
| 1.4 | FOV Shadowcasting | 实现 `Shadowcasting.computeFOV()` | `ShadowcastingTest` 全部通过（7+ 测试用例） |
| 1.5 | ASCII 渲染器 | 使用 libGDX BitmapFont 实现等宽字符渲染 | 屏幕显示 80×50 字符网格 |
| 1.6 | 玩家移动 | `@` 在地图上 8 方向移动，碰撞墙壁，摄像机跟随 | 手动验证：`@` 在地牢中移动，FOV 正确更新 |

**Phase 1-1.0 出口标准**:
- [ ] `gradle test` 全绿（ECS + 地图 + FOV）
- [ ] 屏幕上 `@` 在有视野限制的 BSP 地牢中 8 方向移动
- [ ] `core` 模块零引擎依赖（`gradle :core:dependencies` 验证）

---

### Phase 1-2.0: 战斗与 AI

> **目标**: 实现回合制战斗循环。怪物出现在地牢中，具有不同 AI 行为，可战斗、可升级。
> **前置依赖**: Phase 1-1.0 全部完成
> **产出版本**: 无发布版本（内部里程碑）

| # | 任务 | 详细内容 | 验收标准 |
|---|------|---------|---------|
| 2.1 | 能量系统回合调度器 | 实现 `TurnScheduler`，支持不同速度实体 | `TurnSchedulerTest` 全部通过（7+ 测试用例） |
| 2.2 | 战斗系统 | 实现 `CombatResolver`，碰撞式近战攻击 | `CombatResolverTest` 全部通过（9+ 测试用例） |
| 2.3 | A* 寻路 | 实现 `AStar.findPath()` | `AStarTest` 全部通过（6+ 测试用例） |
| 2.4 | 怪物 AI | 实现 `AIDecisionMaker`（chase/kite/patrol） | `AIDecisionTest` 全部通过（7+ 测试用例） |
| 2.5 | YAML 数据加载 | 实现 `DataLoader`，加载 monsters.yaml | YAML 解析测试通过，5 种怪物正确加载 |
| 2.6 | 怪物生成 + 实体工厂 | 实现 `EntityFactory`，在地牢中按配置生成怪物 | 地牢中出现怪物，不同层不同怪物种类 |
| 2.7 | 死亡 + 经验值 + 升级 | 实现死亡事件、经验值获取、升级逻辑 | 杀死怪物获得经验，达到阈值升级 |
| 2.8 | 消息日志 | 实现 `MessageLog`，战斗消息显示在底部 | 战斗信息实时显示 |

**Phase 1-2.0 出口标准**:
- [ ] `gradle test` 全绿（新增回合调度 + 战斗 + 寻路 + AI 测试）
- [ ] 地牢中有怪物，玩家可与之战斗
- [ ] 至少 3 种可观察到的不同 AI 行为（chase 追击、kite 风筝、patrol 巡逻）
- [ ] 杀死怪物获得经验值，达到阈值自动升级
- [ ] 消息日志实时显示战斗信息

---

### Phase 1-3.0: 物品与天赋

> **目标**: 实现装备系统和天赋系统。玩家可拾取/穿戴装备、使用 4 个战士天赋。
> **前置依赖**: Phase 1-2.0 全部完成
> **产出版本**: 无发布版本（内部里程碑）

| # | 任务 | 详细内容 | 验收标准 |
|---|------|---------|---------|
| 3.1 | 物品系统 | 实现 `InventoryManager`（拾取/装备/卸下/使用） | `InventoryManagerTest` 通过 |
| 3.2 | 物品生成 | 实现 `ItemGenerator`（基础 + 材质 + 词缀） | `ItemGeneratorTest` 全部通过（8+ 测试用例） |
| 3.3 | YAML 物品加载 | 加载 items.yaml，在地牢中生成物品 | 地板上出现物品，可拾取 |
| 3.4 | 背包/装备 UI | 实现 `InventoryScreen`，支持查看/装备/使用 | 按 `i` 打开背包，可装备武器和防具 |
| 3.5 | 属性派生 | 实现 `StatsCalculator`，装备影响属性 | 装备武器后攻击力变化，装备防具后防御变化 |
| 3.6 | 天赋系统 | 实现 `TalentResolver`、`TalentRegistry` | `TalentResolverTest` 全部通过（12+ 测试用例） |
| 3.7 | Stamina + 冷却 | 天赋消耗 Stamina，每回合自然回复，冷却计时 | 体力不足/冷却中时天赋不可用 |
| 3.8 | 天赋使用 UI | 热键 1-4 使用天赋，目标选择模式 | 按 1-4 使用天赋，需要目标的天赋进入选择模式 |

**Phase 1-3.0 出口标准**:
- [ ] `gradle test` 全绿（新增物品生成 + 背包 + 天赋测试）
- [ ] 地牢地板上随机出现物品，按 `g` 可拾取
- [ ] 装备武器/防具后属性面板数值正确变化
- [ ] 4 个天赋（猛击/冲锋/盾击/战吼）均可正常使用
- [ ] 天赋冷却和 Stamina 消耗机制正确运作
- [ ] 消耗品（药水/卷轴）使用后从背包移除

---

### Phase 1-4.0: 完整循环

> **目标**: 串联所有系统形成完整游戏循环。多层地牢、Boss 战、存档、胜负判定。
> **前置依赖**: Phase 1-3.0 全部完成
> **产出版本**: 无发布版本（内部里程碑）

| # | 任务 | 详细内容 | 验收标准 |
|---|------|---------|---------|
| 4.1 | 楼梯 + 多层地牢 | 5 层地牢，楼梯切换，`DungeonManager` | 踩 `>` 下楼，踩 `<` 上楼 |
| 4.2 | Boss 战 | 第 5 层放置 Boss，Boss 有天赋 AI | Boss 使用猛击和战吼 |
| 4.3 | 属性分配 UI | 升级时分配属性点 | 升级后弹出属性分配界面 |
| 4.4 | 天赋分配 UI | 每 2 级分配天赋点 | 按 T 打开天赋树，分配点数 |
| 4.5 | 存档系统 | JSON 存档 + Permadeath 删档 | Ctrl+S 存档，死亡删档，切层自动存档 |
| 4.6 | 胜利/失败画面 | 死亡 → Game Over，杀 Boss → Victory + 统计 | 正确触发并显示结算 |
| 4.7 | 主菜单 | 新游戏 / 继续 / 退出 | 有存档显示继续，无存档灰掉 |

**Phase 1-4.0 出口标准**:
- [ ] `gradle test` 全绿（新增存档序列化/反序列化测试）
- [ ] 从主菜单开始新游戏，5 层地牢可上下楼切换
- [ ] 第 5 层 Boss 具有天赋 AI（使用猛击和战吼）
- [ ] 被杀 → Game Over 画面；击杀 Boss → Victory + 统计画面
- [ ] Ctrl+S 手动存档、切层自动存档、死亡自动删档
- [ ] 退出后重新启动可从存档继续
- [ ] 完整游戏循环可通关

---

### Phase 1-5.0: 打磨与发布

> **目标**: 数值平衡、补全 UI、测试覆盖率达标、修复 Bug，发布 v0.1.0。
> **前置依赖**: Phase 1-4.0 全部完成
> **产出版本**: **v0.1.0**

| # | 任务 | 详细内容 | 验收标准 |
|---|------|---------|---------|
| 5.1 | 数值平衡 | 完整通关测试 3+ 次，调整怪物/物品/经验数值 | 通关时间 30-60 分钟，死亡率合理 |
| 5.2 | 检视模式 | 按 `x` 进入检视模式，查看怪物属性/物品信息 | 可查看任何可见实体的详细信息 |
| 5.3 | 测试覆盖率 | `gradle jacocoTestReport`，核心系统 > 80% | Jacoco 报告显示 core/ 覆盖率 > 80% |
| 5.4 | Bug 修复 | 修复测试中发现的问题 | 所有已知 P0/P1 Bug 修复 |
| 5.5 | 发布 v0.1.0 | 打包、README、发布 | 可下载运行的 jar |

**Phase 1-5.0 出口标准**（同时也是 Phase 1 整体出口标准）:
- [ ] `gradle test` 全部通过
- [ ] core/ 测试覆盖率 > 80%
- [ ] 完整通关用时 30-60 分钟
- [ ] 4 个天赋各有明确战术价值
- [ ] 无 P0/P1 Bug
- [ ] 可下载运行的 v0.1.0 jar 包

---

## 6. 测试策略

### 6.1 测试分层

| 层 | 目标 | 方式 | 覆盖率要求 |
|----|------|------|-----------|
| `core/` 纯逻辑 | 全部核心算法 | JUnit 5 + kotest assertions | > 80% |
| `game/` 数据加载 | YAML 解析与实体创建 | JUnit 5 | > 60% |
| `client/` 渲染 | 视觉正确性 | 手动测试 | N/A |

### 6.2 测试文件结构

```
core/src/test/kotlin/com/ktome/core/
├── ecs/
│   └── WorldTest.kt
├── turn/
│   └── TurnSchedulerTest.kt
├── fov/
│   └── ShadowcastingTest.kt
├── pathfinding/
│   └── AStarTest.kt
├── map/
│   └── BSPGeneratorTest.kt
├── combat/
│   └── CombatResolverTest.kt
├── talent/
│   └── TalentResolverTest.kt
├── item/
│   ├── ItemGeneratorTest.kt
│   └── InventoryManagerTest.kt
├── ai/
│   └── AIDecisionTest.kt
├── stats/
│   └── StatsCalculatorTest.kt
└── save/
    └── SaveManagerTest.kt

game/src/test/kotlin/com/ktome/game/
├── DataLoaderTest.kt
└── EntityFactoryTest.kt
```

### 6.3 测试编写规范

1. **每个测试方法测试一个行为**，命名格式: `methodName_condition_expectedResult`
2. **使用固定 Random seed** 保证确定性
3. **测试不依赖执行顺序**
4. **使用 kotest assertions** 提高可读性：
   ```kotlin
   result.damage shouldBeGreaterThan 0
   result.hit shouldBe true
   map.tiles[5][3] shouldBe TileType.FLOOR
   ```

### 6.4 AI 验证命令

```bash
# 全部测试
./gradlew test

# 只跑核心模块
./gradlew :core:test

# 跑特定测试类
./gradlew :core:test --tests "com.ktome.core.combat.CombatResolverTest"

# 覆盖率报告
./gradlew jacocoTestReport
# 报告在 core/build/reports/jacoco/test/html/index.html

# 快速验证循环
./gradlew :core:test && echo "ALL GREEN" || echo "TESTS FAILED"
```

---

## 7. 出口标准

Phase 1 完成需满足以下全部条件：

| # | 标准 | 验证方式 |
|---|------|---------|
| 1 | `gradle test` 全部通过 | 自动化 |
| 2 | core/ 测试覆盖率 > 80% | `gradle jacocoTestReport` |
| 3 | 主菜单可开始新游戏 | 手动 |
| 4 | 5 层地牢探索、战斗、升级、换装备 | 手动通关 |
| 5 | 4 个天赋各有明确战术价值 | 手动验证 |
| 6 | 被杀 → Game Over | 手动 |
| 7 | 击杀 Boss → Victory + 统计 | 手动 |
| 8 | 存档/读档正常 | 手动 |
| 9 | Permadeath 正确删档 | 手动 |
| 10 | 至少 3 种可观察到的不同 AI 行为 | 手动 |
| 11 | 完整通关用时 30-60 分钟 | 手动 |
| 12 | 无 P0/P1 Bug | 测试 |

---

## 附录 A: 颜色定义

```kotlin
object GameColors {
    // 使用十六进制，由 AsciiRenderer 转换为 libGDX Color
    val PLAYER      = "#FFFFFF"
    val WALL         = "#555555"
    val FLOOR        = "#AAAAAA"
    val STAIR        = "#FFD700"
    val DOOR         = "#8B4513"
    val FOG          = "#333333"  // 已探索未可见

    val MONSTER_RAT  = "#8B4513"
    val MONSTER_SKEL = "#FFFFFF"
    val MONSTER_ARCH = "#CCCCCC"
    val MONSTER_ORC  = "#00AA00"
    val MONSTER_BOSS = "#FF0000"

    val ITEM_WEAPON  = "#C0C0C0"
    val ITEM_ARMOR   = "#C0C0C0"
    val ITEM_POTION  = "#FF0000"
    val ITEM_SCROLL  = "#00FFFF"

    val MSG_NORMAL   = "#FFFFFF"
    val MSG_DAMAGE   = "#FF4444"
    val MSG_HEAL     = "#44FF44"
    val MSG_LEVELUP  = "#FFD700"
    val MSG_INFO     = "#AAAAAA"
    val MSG_TALENT   = "#00FFFF"
}
```

## 附录 B: 经验值表

| 怪物 | 经验值 |
|------|--------|
| 老鼠 (rat) | 15 |
| 骷髅 (skeleton) | 30 |
| 骷髅弓手 (archer) | 35 |
| 兽人 (orc) | 60 |
| 兽人督军 (boss) | 500 |

| 等级 | 升级所需 | 累计所需 |
|------|---------|---------|
| 1→2 | 150 | 150 |
| 2→3 | 250 | 400 |
| 3→4 | 350 | 750 |
| 4→5 | 450 | 1200 |
| 5→6 | 550 | 1750 |
| 6→7 | 650 | 2400 |
| 7→8 | 750 | 3150 |
| 8→9 | 850 | 4000 |
| 9→10 | 950 | 4950 |
| 10→11 | 1050 | 6000 |
| 11→12 | 1150 | 7150 |
| 12→13 | 1250 | 8400 |
| 13→14 | 1350 | 9750 |
| 14→15 | 1450 | 11200 |
| 15→16 | 1550 | 12750 |
| 16→17 | 1650 | 14400 |
| 17→18 | 1750 | 16150 |
| 18→19 | 1850 | 18000 |
| 19→20 | 1950 | 19950 |

## 附录 C: 数值预估（1 级战士 vs 各怪物）

用于 balance 参考，基于 STR=12, DEX=10, CON=12, WIL=10，装备短剑+皮甲。

| 对手 | 玩家攻击力 | 对手防御 | 预期伤害 | 对手HP | 击杀回合 | 对手攻击力 | 玩家防御 | 受到伤害 | 玩家HP |
|------|---------|---------|---------|-------|---------|---------|---------|---------|-------|
| 老鼠 | 29 | 0 | ~27-31 | 12 | 1 | 3 | 5 | 1 | 146 |
| 骷髅 | 29 | 3 | ~24-28 | 25 | 1 | 6 | 5 | ~1-3 | 146 |
| 弓手 | 29 | 1 | ~26-30 | 18 | 1 | 7 | 5 | ~2-4 | 146 |
| 兽人 | 29 | 5 | ~22-26 | 45 | 2 | 10 | 5 | ~5-7 | 146 |
| Boss | 29 | 10 | ~17-21 | 120 | 6-7 | 18 | 5 | ~11-15 | 146 |

> 注：以上为粗略估算。实际需结合命中率、暴击率、天赋使用做完整模拟。数值平衡将在 Week 5 详细调整。

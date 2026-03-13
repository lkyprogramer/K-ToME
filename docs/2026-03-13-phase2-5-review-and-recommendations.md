# K-ToME Phase 2~5 开发文档审阅报告

**审阅对象**: `docs/K-ToME_Phase2_to_Phase5_PR_Development_Guide_v2_SinglePlayer_Tile_i18n.md`
**审阅日期**: 2026-03-13
**审阅视角**: 资深游戏开发总监
**参考基线**: Phase 1 完成代码、`wiggly-sprouting-platypus.md`、Phase 1 路线图、`Roguelike 游戏开发指导文档.md`

---

## 总体评价

这份 v2 文档在以下方面做得优秀：

1. **硬约束驱动**：单机唯一玩家、职业单通合同、中文优先 i18n、Tile-first、资源流水线前置——这五条铁律定义清晰，能有效防止后续开发摇摆。
2. **PR 粒度拆分**：每个 PR 都有目标、依赖、改动、验证、验收、门禁六段结构，比大多数游戏项目的开发文档规范得多。
3. **跨阶段回归矩阵**：Golden Seed、Locale、SoloClearLab、资源覆盖率、性能基线五套回归体系是长期质量的保障。
4. **覆盖关系声明清晰**：明确说了和旧文档的关系，不会产生两份文档互相矛盾的混乱。

但文档存在若干结构性问题和遗漏，如果不解决，会在实际开发中造成严重的进度风险、返工和实现歧义。以下按严重程度从高到低逐一展开。

---

## 一、结构性问题（P0 — 必须解决）

### 1.1 Phase 2 范围失控：12 个 PR 实质上是一次全面重写

Phase 2 同时要完成：

| 工作域 | PR | 本质 |
| --- | --- | --- |
| 引擎架构重写 | PR01 (能量1000+事件总线+回调) | 核心引擎 |
| 本地化基建 | PR02 (i18n框架) | 新基建 |
| 渲染契约重建 | PR03 (RenderSnapshot) | 新基建 |
| 渲染引擎重写 | PR04 (TileRenderer) | 从零开始 |
| 工具链建设 | PR05 (Gemini图像流水线) | 全新领域 |
| 工具链建设 | PR06 (音频流水线) | 全新领域 |
| UI 全量重写 | PR07 (7个屏幕) | 从零开始 |
| 关卡系统重写 | PR08 (Tile-first地图) | 大幅改造 |
| 内容生产 | PR09 (4职业48天赋) | 大量创作 |
| 内容翻译 | PR10 (全量双语言) | 大量翻译 |
| 数据迁移 | PR11 (Save V2 + ASCII迁移) | 技术债 |
| 收口封版 | PR12 (240+Tile, 180+Icon, 90+SFX, 4BGM) | 大量资源 |

**这不是"深化"，这是推倒 Phase 1 的可玩产物并完全重建。** 在这 12 个 PR 的执行过程中（可能持续 4-8 个月），游戏处于**不可玩状态**。这违背了 Phase 1 路线图中"每个阶段都必须形成一个可演示、可截图、可录屏的真实切片"的核心原则。

**建议**：

将 Phase 2 拆为 **Phase 2A（架构升级）** 和 **Phase 2B（内容与表现）**，中间插入一个"可玩检查点"：

```
Phase 2A — 架构基线（v0.2.0-alpha）
  P2A-PR01: 1000能量调度 + 事件总线 + Hook/Callback（core only，Phase 1 玩法不破坏）
  P2A-PR02: i18n 框架 + locale-lint（先支撑现有内容的双语言化）
  P2A-PR03: RenderSnapshot 契约 + VisualKey/AudioKey schema
  P2A-PR04: 序列化引擎迁移（GSON → kotlinx.serialization）
  P2A-PR05: Save V2 数据模型（ID + token，不存文案）
  ── 检查点：Phase 1 内容可用新架构跑通，仍是 ASCII，但事件流/i18n/存档已切新管线 ──

Phase 2B — Tile 切换与内容填充（v0.2.0）
  P2B-PR01: TileRenderer 基线 + 图集分层
  P2B-PR02: 图像资源流水线 V1（Gemini + atlas）
  P2B-PR03: 音频流水线 V1（cue + routing）
  P2B-PR04: UI 全量重写
  P2B-PR05: Tile-first 地图与交互体系
  P2B-PR06: 4职业 V1
  P2B-PR07: 双语言内容覆盖清盘
  P2B-PR08: 封版 v0.2.0
  ── 检查点：Tile 版可通关，双语言，4 职业 ──
```

这样做的收益：
- Phase 2A 完成后游戏仍可玩（ASCII），核心架构已经就位
- Phase 2B 的内容生产可以在稳定的架构上进行，不会因为底层还在动而返工
- 两个子阶段可以分别评估进度和风险

### 1.2 缺少 Phase 1 → Phase 2 的具体迁移分析

文档说"保留旧文档有效技术思想"但没有针对 **Phase 1 实际代码** 做逐项迁移分析。以下是关键的断裂点：

| Phase 1 现状 | Phase 2 目标 | 缺失的迁移说明 |
| --- | --- | --- |
| `TurnScheduler` 阈值 = 100 | 阈值 = 1000 | 只要改常量？还是需要重新设计调度数据结构？现有速度公式 `speed = baseSpeed + dex/2` 如何映射到 1000 制？ |
| 序列化用 GSON + `SaveSnapshot` (version=2) | 要求用 ID + token 存储 | GSON → kotlinx.serialization 迁移？还是继续用 GSON？文档完全未提序列化引擎选型 |
| 无事件总线（事件类定义了但未中央调度） | 事件总线 + Hook/Callback | 现有 `DamageDealtEvent`、`EntityDeathEvent` 等如何接入新总线？Session 中的 ad-hoc 事件处理怎么迁移？ |
| `AsciiRenderer` 是唯一可玩渲染器 | 废弃 ASCII | AsciiRenderer 何时删除？调试模式如何处理？ |
| 消息日志 = `Deque<String>` 裸中文 | LogEventKey + Args | 需要全面改造 `FoundationGameSession` 中约 30+ 处 `addMessage()` 调用 |
| `SplitMix64RandomSource` 用于战斗确定性 | 需支持回放 | 回放需要的不只是随机种子，还有输入序列记录。何时引入？ |
| 4 个天赋硬编码在 `talents.yaml` | 48 个天赋，需要执行器/telegraph/VFX | TalentResolver 的 resolve 逻辑需要大幅扩展，当前设计是否支持？ |

**建议**：在 Phase 2 开头新增一节 **"Phase 1 → Phase 2 技术债与迁移清单"**，逐项列出需要迁移的系统、迁移策略（原地升级 vs 重写 vs 包装）、影响范围和验证方式。

### 1.3 序列化引擎选型是空白

这是一个影响全局的技术决策，但文档完全回避了：

- 旧技术白皮书明确推荐 **kotlinx.serialization**（编译期代码生成，无反射，ProGuard 友好）
- Phase 1 实际使用的是 **GSON**（`SaveManager` 中 `GsonBuilder().setPrettyPrinting().create()`）
- v2 文档提到 `saveVersion = 2` 并要求存 ID/token 而非文案，但**没有指定用什么序列化框架**

这不是细节问题。序列化引擎影响：
- 所有数据类的注解方式（`@Serializable` vs GSON 约定）
- 多态组件的处理策略（密封类 + classDiscriminator vs GSON TypeAdapter）
- 存档版本迁移的工具链
- 性能（反射 vs 编译期）
- 与 mod 数据的兼容性

**建议**：在 Phase 2A 中新增一个专门的 PR 处理序列化迁移，明确选型为 kotlinx.serialization（与旧白皮书一致），并定义迁移步骤：
1. 引入 kotlinx.serialization 依赖
2. 为所有 core 数据类添加 `@Serializable`
3. 定义多态组件的 sealed class 层次
4. 迁移 `SaveSnapshot` 到新框架
5. 保留 GSON 兼容层用于读取旧存档

### 1.4 PR 依赖链过于串行，无并行空间

Phase 2 的 12 个 PR 几乎是完全串行的：

```
PR01 → PR02 → PR03 → PR04 → ... → PR12
```

这意味着：
- 没有并行开发机会
- 任何一个 PR 阻塞，整个 Phase 停滞
- 资源流水线（PR05/06）必须等渲染契约（PR03）和渲染器（PR04）完成后才能开始

**建议**：识别可并行的工作流。例如：
- **PR05（图像流水线）** 的工具链开发不依赖 TileRenderer，可以和 PR01-PR03 并行
- **PR06（音频流水线）** 同理
- **PR09（职业设计）** 的 core 逻辑（天赋数值、AI施法规则）可以先于渲染层开发
- **i18n 文案撰写**（中英文翻译）可以和技术实现并行

在文档中画出真实的依赖图（DAG），标注哪些可以并行。

---

## 二、关键设计遗漏（P1 — 应该补充）

### 2.1 缺少关键系统的接口骨架

文档描述了很多"要做什么"，但对于指导 AI 辅助开发来说，缺少"具体长什么样"。以下系统应该给出 Kotlin 接口/数据类骨架：

**事件总线与回调系统**（P2-PR01 中最核心的新增）：

```kotlin
// 建议文档给出这级别的设计骨架

/** 回调优先级：数值越小越先执行 */
typealias CallbackPriority = Int

/** 游戏事件基类 */
sealed interface GameEvent {
    val turn: Int
    val sourceId: EntityId?
}

/** 回调注册表 */
interface CallbackRegistry {
    fun <T : GameEvent> register(
        eventType: KClass<T>,
        priority: CallbackPriority,
        owner: EntityId,
        handler: (T) -> CallbackResult
    ): CallbackHandle

    fun unregister(handle: CallbackHandle)
    fun unregisterAll(owner: EntityId)
}

/** 回调结果 — 控制事件管道继续还是中断 */
enum class CallbackResult { CONTINUE, CANCEL, ABSORB }
```

**RenderSnapshot 完整定义**（当前文档只给了顶层字段，缺少子视图定义）：

```kotlin
data class CellView(
    val x: Int, val y: Int,
    val groundKey: String,
    val decalKey: String?,
    val fogState: FogState,         // VISIBLE / EXPLORED / HIDDEN
    val dangerLevel: DangerLevel?
)

data class ActorView(
    val entityId: EntityId,
    val position: Point,
    val visualKey: String,
    val facing: Direction,
    val healthPercent: Float,
    val statusIcons: List<String>,
    val isPlayer: Boolean
)
```

**建议**：为 Phase 2 的每个核心新增系统（事件总线、RenderSnapshot、i18n bundle、VisualManifest、AudioManifest、TalentDef V2）提供 Kotlin 接口级骨架。这不是过度设计，而是给 AI 开发助手和人工开发者一个明确的实现锚点。

### 2.2 旧技术白皮书中的有价值设计未被吸收

`Roguelike 游戏开发指导文档.md` 中有大量详细的技术设计，v2 文档声称保留但实际上丢失了关键细节：

| 旧白皮书内容 | v2 文档处理 | 问题 |
| --- | --- | --- |
| 1000 能量制的详细排序规则（能量 desc → UUID 次级键） | PR01 提了 `energy desc -> initiative desc -> entityId asc` | **initiative 是什么？** 旧白皮书用 UUID，v2 新增了 initiative 但未定义。需要明确 initiative 的来源和计算方式 |
| 正负能量双轨制与能量阈值衰减模型 | 未提及 | 如果圣堂武士用正能量、奥术师用法力值，资源模型需要在 Phase 2 就定义清楚，不能等 Phase 3 |
| callbackOnActBase / callbackOnMeleeHit / callbackOnRest / callbackOnDamageTaken 四个核心回调基元 | PR01 列了 `onAct/onMove/onHit/onDamageTaken/onKill/onQuestStepChanged` | 命名不一致，旧白皮书的载荷定义（每个回调携带什么数据）更详细，应该整合 |
| 密封类 + @Polymorphic + classDiscriminator 的序列化方案 | 完全未提 | 见 1.3 节 |
| JsonContentPolymorphicSerializer 用于 Mod 数据解析 | 完全未提 | Phase 4 的 Mod 系统需要这个，应该在架构节提前声明 |
| 6 条伤害通道（物理/火焰/寒冰/闪电/神圣/暗蚀） | Phase 3 才引入 | 如果 Phase 2 的 4 个职业包含奥术师（火焰/寒冰/闪电）和圣堂武士（神圣），伤害通道必须在 Phase 2 就引入 |
| 非线性收益递减公式（对数衰减/平方根平滑） | Phase 3 | 正确的时序，但应该给出具体函数形式 |
| Park-Miller 分布 + 分离控制行为算法 | Phase 4 | 可以接受，但应该在 Phase 2 的地图生成器中预留扩展点 |
| Utility AI + Behavior Tree 双轨制 | Phase 5 | Phase 2 的 4 个职业的怪物 AI 用什么？当前的 CHASE/KITE/PATROL 足够支撑精英和 Boss 吗？ |

**建议**：在 v2 文档的每个 Phase 开头，新增一节 **"从旧白皮书吸收的技术设计"**，明确列出哪些被采纳（含具体设计细节）、哪些被推迟（含原因）、哪些被废弃（含替代方案）。

### 2.3 伤害类型通道必须前移到 Phase 2

v2 文档将伤害通道系统放在 Phase 3（P3-PR01），但 Phase 2 的职业设计包含：

- **奥术师 Arcanist**：火焰/寒冰/奥术 → 明显需要元素伤害类型
- **圣堂武士 Templar**：神圣/驱邪 → 需要神圣伤害类型
- **游荡者 Rogue**：毒刃（Phase 3 影刃客的前身？）→ 可能需要毒属性

如果 Phase 2 只有物理伤害一种通道，奥术师和圣堂武士的 12 个天赋就无法有真正的元素差异，只能做成"不同颜色的物理伤害"——这会导致 Phase 3 引入伤害通道时的大规模返工。

**建议**：在 Phase 2A 中引入基础伤害类型枚举和 `DamageInstance(amount, type)` 结构。不必在 Phase 2 就做完抗性/穿透/收益递减，但伤害通道的**数据管道**必须从第一天就通。

```kotlin
enum class DamageType {
    PHYSICAL, FIRE, COLD, LIGHTNING, HOLY, SHADOW
}

data class DamageInstance(
    val amount: Int,
    val type: DamageType,
    val source: EntityId,
    val isCrit: Boolean = false
)
```

### 2.4 AI 策略在 Phase 2-3 存在真空期

Phase 1 的 AI 是简单的 CHASE/KITE/PATROL 三种行为。Phase 5 才引入 Utility AI + 行为树。那 Phase 2-4 的精英怪和 Boss（数量在不断增长）用什么 AI？

- Phase 2 有 4 个 Boss + 8 个精英模板
- Phase 3 有 11+ Zone 的完整怪物生态
- Phase 4 有随机首领

这些都需要比 CHASE/KITE/PATROL 更复杂的行为，但又达不到 Phase 5 的 Utility AI 水平。

**建议**：在 Phase 2 引入一个中间层级的 AI 框架——**脚本化行为配置**（不是 Lua，而是数据驱动的行为序列）：

```yaml
# monsters/orc_shaman.yaml
ai:
  type: SCRIPTED
  behaviors:
    - condition: "self.hp_percent < 0.3"
      action: HEAL_SELF
      priority: 100
    - condition: "target.distance <= 2"
      action: RETREAT
      priority: 90
    - condition: "skill.fireball.ready && target.distance <= 5"
      action: USE_SKILL
      skill: fireball
      priority: 80
    - condition: "target.visible"
      action: KITE
      preferred_range: 4
      priority: 50
    - default: PATROL
```

这样 Phase 2-4 的怪物可以通过数据配置获得更丰富的行为，而不需要等 Phase 5 的完整 AI 系统。Phase 5 的 Utility AI 可以作为更智能的替代品，但不再是唯一的高级 AI 方案。

### 2.5 缺少 AI 辅助开发的适配说明

原始项目规划中 "AI 可自行验证" 是核心竞争力。Phase 1 路线图也反复强调这一点。但 v2 文档几乎完全丢失了这个视角。

以下问题需要回答：

1. **Headless 测试如何适配 Tile 渲染？** Phase 1 可以通过 `./gradlew test` 验证几乎所有逻辑，因为 core 零引擎依赖。Phase 2 引入了 TileRenderer、UI 重写、音频路由——这些如何 headless 测试？
2. **Golden Screenshot 如何实现？** 文档多次提到但没有技术方案。libGDX headless backend 的能力有限。
3. **AI 生成的天赋/怪物/装备如何自验证？** 48 个天赋的数值平衡、48 个怪物的行为合理性——有没有自动化验证框架？
4. **资源流水线（Gemini）如何与 AI 开发工作流集成？** AI 助手如何触发图像生成？如何验证生成结果？

**建议**：新增一节 **"AI 辅助开发工作流（Phase 2 起）"**，涵盖：
- 哪些验证仍然走 `./gradlew test`（core 逻辑、数据解析、序列化）
- 哪些验证需要 headless smoke（RenderSnapshot 一致性、资源 manifest 完整性）
- 哪些验证需要 golden screenshot（UI 布局、Tile 渲染）
- 如何实现 golden screenshot（推荐方案：libGDX headless backend + 内存帧缓冲 + 像素比较）
- AI 开发助手的标准验证命令清单

### 2.6 职业资源模型未统一定义

Phase 2 引入 4 个职业，但资源模型定义分散且不完整：

- 战卫：Stamina（Phase 1 已有）
- 奥术师：Mana（新资源类型）
- 游荡者：Stamina + 击杀回复模式（改变回复规则）
- 圣堂武士：正能量（全新资源类型，受击回复？）

这意味着 Phase 2 需要实现：
1. 通用资源系统（不止 Stamina 一种）
2. 资源回复策略可配置（自然回复、击杀回复、受击回复、条件回复）
3. UI 适配多种资源类型的显示

但文档中没有统一定义这个系统。P2-PR09（4 职业）依赖的是 PR01 的事件总线和 PR07 的 UI，但资源系统本身没有专门的 PR。

**建议**：在 P2-PR01 中或新增一个独立 PR，定义通用资源系统：

```kotlin
data class ResourcePool(
    val type: ResourceType,
    val current: Int,
    val max: Int,
    val regenPolicy: RegenPolicy
)

enum class ResourceType { STAMINA, MANA, POSITIVE_ENERGY, HATE, EQUILIBRIUM }

sealed interface RegenPolicy {
    data class PerTurn(val amount: Int) : RegenPolicy
    data class OnKill(val amount: Int) : RegenPolicy
    data class OnDamageTaken(val percent: Float) : RegenPolicy
    data class Conditional(val condition: String, val amount: Int) : RegenPolicy
}
```

---

## 三、内容预算与可行性（P1 — 应该调整）

### 3.1 Phase 2 内容预算超出单阶段合理范围

| 资源类型 | 数量 | 预估工作量（即使有 Gemini） | 问题 |
| --- | --- | --- | --- |
| 4 职业 × 12 天赋 = 48 天赋 | 48 | 每个天赋需要：设计 + 数值 + 实现 + 测试 + icon + VFX + SFX + 双语言描述。按每天 2 个天赋算，需 24 工作日 | **偏大但可接受** |
| 怪物模板 | 48 | 36 普通 + 8 精英 + 4 Boss | Phase 1 只有约 10 个。从 10 跳到 48 是巨大的内容跳跃 |
| 地形/道具 Tile | >= 240 | 4 zone × 60 = 240 | 即使用 Gemini 生成，后处理 pipeline 尚未建立 |
| 技能/状态/物品 Icon | >= 180 | 48 天赋 + 48 状态 + 42 物品 + 各类 UI | 合理但前提是 pipeline 稳定 |
| VFX 套件 | >= 30 套 | 程序化特效需要先建立 VFX 框架 | **Phase 1 没有任何 VFX 基建** |
| SFX | >= 90 条 | 音频来源？合成？购买？Gemini 不做音频 | **音频来源完全未说明** |
| BGM/Ambience | 4+8 | 专业配乐或 AI 音乐生成？ | **来源未说明** |

**关键风险**：图像流水线（Gemini）和音频流水线在 Phase 2 中是**从零搭建**的，但内容预算是按照**流水线已经稳定运行**来估算的。这是经典的先有鸡还是先有蛋的问题。

**建议**：

1. **Phase 2 的内容预算削减为 50%**，作为"可通关最低限"：
   - 4 职业各 8 天赋（32 个），Phase 3 再扩到 24
   - 24 个怪物模板（足够 4 个 zone），Phase 3 再扩
   - 120 个 Tile（基础集），Phase 3 扩展 biome 变体
   - 60 个 Icon（核心天赋 + 物品）
   - 15 套 VFX（覆盖核心交互）
   - 45 条 SFX（覆盖核心反馈）
   - 2 首 BGM + 4 条环境循环（标题 + 探索 + Boss + 胜败）

2. **明确音频来源策略**：购买音效包？使用开源音效库（如 freesound.org）？AI 合成？还是人工创作？这是一个被完全忽略的关键决策。

### 3.2 Phase 3 的内容预算接近小型商业游戏

Phase 3 要求 176 个天赋、120 个怪物、180 个装备条目、300 个新 Tile、220 个 Icon、120 条新 SFX、7 首音乐。

即使是全职团队，这也是数月的内容生产量。文档没有给出时间估算或者人力假设。

**建议**：
- 为每个 Phase 的内容预算附上"最小可发布集"和"完整目标集"两档
- 标注哪些内容可以在后续 Phase 中增量补充
- 考虑引入"内容里程碑"，而不是在 Phase 结尾一次性填充

---

## 四、技术细节问题（P2 — 建议解决）

### 4.1 能量系统升级路径不清

Phase 1 现状：
```kotlin
// TurnScheduler.kt
const val ACTION_THRESHOLD = 100
// 速度派生：speed = baseSpeed + dex / 2（数值量级 ~50-120）
```

Phase 2 目标：阈值 = 1000，排序键 = `energy desc -> initiative desc -> entityId asc`

问题：
1. 现有速度值直接乘 10？还是重新设计速度公式？
2. `initiative` 是什么？旧白皮书用 UUID，v2 用 initiative。二者不同
3. 玩家习惯了 `speed = 100` 表示标准速度，改成 1000 后 UI 怎么显示？
4. 旧白皮书提到的"非标准回合消耗"（如 750 能量的快速法术）——这个在 Phase 2 的 4 职业中就需要吗？

**建议**：明确以下设计决策：
- 速度公式：`effectiveSpeed = baseSpeed * 10`（简单缩放）或全新公式
- initiative 定义：建议就用 `entityId`（确定性排序），删除 initiative 概念以减少复杂度
- 标准行动消耗 = 1000，快速行动 = 750，慢速行动 = 1250（给出具体天赋的消耗映射表）

### 4.2 Hook/Callback 系统与现有事件类型的整合

Phase 1 定义了 5 种事件：
```kotlin
// event/ 目录下
DamageDealtEvent, EntityDeathEvent, ExperienceGainedEvent, LevelUpEvent, MissEvent
```

但这些事件目前是 ad-hoc 使用的（在 `FoundationGameSession` 中直接构造和处理），没有中央调度。

P2-PR01 要引入事件总线 + Hook/Callback，需要明确：
1. 现有 5 种事件是否保留？还是重新定义？
2. Hook 和 Event 的关系：Hook 订阅 Event？还是 Hook 独立于 Event？
3. 回调的返回值是否能修改事件数据（例如 `onDamageTaken` 能不能减少伤害）？
4. 回调中抛异常怎么处理？是否隔离？

旧白皮书定义了 `callbackOnActBase`、`callbackOnMeleeHit` 等具体回调，并说明了载荷。v2 文档只列了名字 `onAct/onMove/onHit/...` 但没有载荷定义。

**建议**：在文档中给出回调系统的完整枚举和载荷定义表，对齐旧白皮书的详细程度。

### 4.3 RenderSnapshot 的更新频率和同步模型

文档定义了 `RenderSnapshot` 但未说明：
1. 每帧生成一个新的？还是增量更新？
2. core 输出 snapshot 的时机是什么？每个 entity 行动后？还是只在玩家行动后？
3. 动画和过渡效果（如移动插值、伤害数字飘字）由谁驱动？snapshot 只包含静态状态，动画由 client 自行补间？

**建议**：明确 core→client 的数据流模型。推荐：
- core 在每次 "世界状态变更" 后输出一个新的 snapshot
- client 在 snapshot 之间做视觉插值（移动动画、浮字动画等）
- snapshot 不包含动画状态，只包含逻辑状态

### 4.4 Gemini 图像流水线的降级策略

文档将 Gemini 作为图像生成的唯一方案，但 `gemini-3.1-flash-image-preview` 是预览模型。

风险：
- 模型可能被废弃或改名
- 生成质量可能不满足"中土高幻想"风格要求
- API 调用有成本限制
- 对于某些资产类型（如像素级精确的 UI icon），AI 生成可能不如手工或程序化生成

**建议**：
1. `AssetGenerationProvider` 接口已经定义了（好的设计），但应该明确至少两种 provider 实现：`GeminiProvider` 和 `ManualImportProvider`（手工导入预处理好的图片）
2. 对于 UI icon，优先考虑程序化生成或购买/使用开源 icon 集，Gemini 不适合这类精确资产
3. 补充成本估算：240+ Tile 的 Gemini API 调用成本

### 4.5 缺少状态效果系统的 Phase 2 基础版

Phase 2 的 4 职业需要：眩晕、击退、破甲、减速、流血、燃烧、冰冻、护盾等效果。Phase 1 的 `EffectTracker` 和 `ActiveEffect` 已经有基础骨架，但只支持 `STUN`、`ARMOR_BREAK`、`BUFF`、`DEBUFF` 四种 `StatusEffectType`。

Phase 2 的 48 个天赋显然需要更多状态类型，但 v2 文档将状态系统 V2 放在了 **Phase 3（P3-PR02）**。

这意味着 Phase 2 的奥术师和圣堂武士的天赋要么：
- 只使用 Phase 1 的 4 种简陋状态类型（体验大打折扣）
- 在 Phase 2 中就扩展状态系统（但文档没有计划这个）

**建议**：在 Phase 2 中扩展 `StatusEffectType` 枚举和 `EffectTracker` 能力，至少支持：

```kotlin
enum class StatusEffectType {
    // Phase 1
    STUN, ARMOR_BREAK, BUFF, DEBUFF,
    // Phase 2 新增
    ROOT, SILENCE, BLIND, CONFUSE, SLOW,
    BLEED, BURN, POISON, FREEZE,
    SHIELD, REGEN, HASTE,
    KNOCKBACK  // 位移效果
}
```

Phase 3 再做状态系统的深层重构（生命周期、互斥组、净化优先级等）。

---

## 五、文档风格与表达问题（P2 — 建议优化）

### 5.1 部分描述过于抽象，缺乏落地指引

以下 PR 的"主要改动"描述过于高层，无法直接指导实现：

- **P2-PR01** "调度器升级为 1000 能量制" — 但没有说具体改什么文件、什么函数、公式怎么变
- **P2-PR03** "建立 VisualManifest 与 AudioManifest" — 但没有 manifest 的 schema 定义
- **P2-PR08** "冻结 Phase 2 四个 zone" — 给了 zone 名字但没有 zone 的设计规格（层数、面积、怪物分布、特殊机制）

**建议**：对于每个 PR 的"主要改动"，增加：
- **涉及的具体文件路径**（基于 Phase 1 已有的项目结构）
- **需要新增的类/接口**
- **需要修改的类/接口**
- **数据文件的 schema 变更**

### 5.2 zone 设计需要具体规格

Phase 2 冻结了 4 个 zone（破碎前哨 / Greenwood Fringe / 深铁矿坑 / 灰门深窟），但只有名字没有设计。

**建议**：为每个 zone 提供最低设计规格：

```yaml
# 示例：破碎前哨
zone_id: shattered_outpost
name_key: zone.shattered_outpost
floors: 3
map_size: 80x50
biome: ruins
recommended_level: 1-5
monster_pool: [rat, skeleton, bone_archer, bandit]
elite_pool: [veteran_skeleton, bandit_captain]
boss: null  # 非 Boss zone
special_mechanics: null
ambient_profile: ruins_wind
music_profile: exploration_dark
lore_key: zone.shattered_outpost.lore
```

### 5.3 旧技术白皮书与 v2 文档的关系应更清晰

当前文档说"旧文档中关于 core 零引擎依赖、测试门禁、数据驱动、Hook/Callback、ProcGen、Affix、AI 的有效技术思想仍保留"，但对于开发者来说，不清楚何时参考哪份文档。

**建议**：
- 在 v2 文档中为每个 Phase 标注"参考旧白皮书的 X 节获取详细技术设计"
- 或者直接将旧白皮书的关键技术细节内联到 v2 文档的对应 PR 中
- 明确声明：**v2 文档是执行权威，旧白皮书是技术参考**

---

## 六、遗漏项清单（建议补充）

| 编号 | 遗漏项 | 应该在哪个阶段 | 说明 |
| --- | --- | --- | --- |
| M01 | 序列化引擎选型与迁移方案 | Phase 2A | GSON → kotlinx.serialization |
| M02 | Phase 1 → Phase 2 代码迁移清单 | Phase 2 开头 | 逐系统的迁移策略 |
| M03 | 伤害类型通道基础定义 | Phase 2A | 4 职业需要元素伤害 |
| M04 | 通用资源系统定义 | Phase 2A | 多职业多资源类型支持 |
| M05 | 中间 AI 层（脚本化行为配置） | Phase 2 | 弥补 Phase 1 简单 AI 和 Phase 5 高级 AI 之间的空白 |
| M06 | AI 辅助开发工作流 | Phase 2 全局 | headless 测试、golden screenshot 技术方案 |
| M07 | 音频来源策略 | Phase 2 全局 | 购买 / 开源 / AI 合成 / 人工创作 |
| M08 | Gemini 降级策略 | Phase 2 全局 | 手工导入作为备选 provider |
| M09 | 状态效果扩展基础版 | Phase 2 | 48 天赋需要更多状态类型 |
| M10 | Zone 设计规格 | Phase 2 | 4 个 zone 的具体设计 |
| M11 | 关键系统的 Kotlin 接口骨架 | 各 Phase | 事件总线、RenderSnapshot、Manifest 等 |
| M12 | 内容预算的"最小可发布集" | 各 Phase | 区分必须和理想 |
| M13 | 并行工作流标注 | Phase 2 | PR 之间的真实 DAG |
| M14 | 性能预算与基线（Phase 2 起就建立） | Phase 2 | 不应等到 Phase 5 |
| M15 | 错误处理与崩溃恢复策略 | Phase 2 全局 | 存档损坏、资源缺失、脚本异常的处理方式 |
| M16 | 输入方案定义 | Phase 2 | 键盘 + 鼠标？手柄？当前只有键盘 |

---

## 七、值得保留和强化的优秀设计

以下设计已经非常好，建议保持甚至强化：

1. **Solo-Clear Contract** — 这是防止设计偏差的硬墙，建议为每个职业建立自动化的 SoloClear 测试套件（固定 seed + 固定 build + 自动下操作指令 + 判定通关）。
2. **PR 模板六段结构**（背景/目标/范围/非目标/验证/门禁）— 非常规范，直接可用。
3. **资源可追溯性要求**（AssetSpec → prompt → raw → processed → atlas → manifest）— 在 AI 生成时代这非常重要。
4. **Golden Seed 回归体系** — 但需要给出第一个 Golden Seed 的具体定义。
5. **Locale Session Lock** — 进局锁语言是正确的设计，避免了大量运行时切换的复杂性。
6. **mod-first 但不过早引入** — Phase 4 才做 Mod SDK 是正确的时序。
7. **存档不存文案只存 token** — 这让语言切换和存档恢复变得优雅。

---

## 八、推荐的修订优先级

按紧迫度排序的修订计划：

### 第一批（阻塞性，必须在 Phase 2 开发前解决）

1. **拆分 Phase 2 为 2A/2B** — 见 1.1 节
2. **补充 Phase 1 → Phase 2 迁移清单** — 见 1.2 节
3. **确定序列化引擎选型** — 见 1.3 节
4. **伤害类型通道前移到 Phase 2** — 见 2.3 节
5. **定义通用资源系统** — 见 2.6 节

### 第二批（重要，应在 Phase 2A 开发中同步补充）

6. **整合旧白皮书技术细节** — 见 2.2 节
7. **补充关键系统 Kotlin 接口骨架** — 见 2.1 节
8. **调整内容预算** — 见 3.1 节
9. **定义中间 AI 层** — 见 2.4 节
10. **明确音频来源策略** — 见 3.1 节

### 第三批（优化，可在开发过程中逐步完善）

11. **标注并行工作流** — 见 1.4 节
12. **AI 辅助开发工作流** — 见 2.5 节
13. **Zone 设计规格** — 见 5.2 节
14. **Gemini 降级策略** — 见 4.4 节
15. **输入方案定义** — 见遗漏项 M16

---

## 附录 A：推荐的 Phase 2A PR 清单（重构建议）

```
Phase 2A — 架构基线（v0.2.0-alpha）
目标：在不破坏 Phase 1 可玩性的前提下，完成所有核心架构升级。
出口：Phase 1 内容可在新架构上跑通（ASCII 仍可用），事件流/i18n/序列化/存档已切新管线。

P2A-PR01: 序列化引擎迁移
  - 引入 kotlinx.serialization
  - 迁移所有 core 数据类
  - 定义多态组件 sealed class
  - 保留 GSON 兼容层读取旧存档
  - 测试：旧存档读取 + 新存档往返验证

P2A-PR02: 1000 能量调度 + 通用资源系统
  - TurnScheduler 阈值 100 → 1000
  - 引入 ResourcePool / ResourceType / RegenPolicy
  - 迁移现有 Stamina 到新资源系统
  - 测试：速度公式、资源回复、确定性排序

P2A-PR03: 伤害类型通道 + 事件总线 + Hook/Callback
  - 定义 DamageType 枚举与 DamageInstance
  - 引入 CallbackRegistry / GameEvent / CallbackResult
  - 迁移现有战斗逻辑到事件驱动
  - 测试：事件确定性、回调优先级、异常隔离

P2A-PR04: i18n 框架 + locale-lint
  - 建立 zh-CN / en-US 双语言包
  - 迁移现有裸字符串到 key
  - locale-lint 工具
  - 测试：key 覆盖率、占位符一致性

P2A-PR05: RenderSnapshot 契约 + Visual/Audio Key Schema
  - 定义 RenderSnapshot 及所有子视图类型
  - 定义 VisualManifest / AudioManifest schema
  - 内容对象增加 visualKey / audioKey / iconKey
  - 测试：snapshot 哈希稳定性、manifest 解析

P2A-PR06: Save V2 + 状态效果扩展
  - 新 SaveSnapshot 基于 kotlinx.serialization
  - 存 ID + token，不存文案
  - 扩展 StatusEffectType 支持 12+ 状态
  - 旧存档迁移脚本
  - 测试：存读往返、旧存档迁移、状态效果生命周期

── 检查点：ASCII 模式仍可通关，但底层已切新架构 ──
```

## 附录 B：推荐的并行工作流视图

```
时间 →  ─────────────────────────────────────────────→

核心架构线     P2A-01 → P2A-02 → P2A-03 → P2A-05 → P2A-06
                                    ↓
i18n 线                    P2A-04 ──────────────────→ P2B-07
                                                          ↓
渲染线         ──────────────────── P2B-01 → P2B-04 → P2B-05
                                      ↑
资源工具线     P2B-02 (图像pipeline) ─┘
               P2B-03 (音频pipeline) ─────────→ ┘
                                                  ↓
内容线         ──────── P2B-06 (职业设计 core 部分) → P2B-08 (封版)

图例：
  → 串行依赖
  ↓ 软依赖（可先行开发 core 部分）
  ─ 可并行
```

---

## 结语

这份 v2 文档的战略方向是正确的：单机优先、中文优先、Tile-first、资源流水线前置、职业单通合同。这些硬约束能有效防止开发摇摆。

主要问题在于 **Phase 2 的范围控制和实施路径**。当前的 12-PR 串行方案会导致长期不可玩状态和巨大的集成风险。通过拆分为 2A/2B、补充迁移清单、调整内容预算、增加并行空间，可以将风险降低到可控范围。

技术细节上最关键的遗漏是**序列化选型**和**伤害类型通道前移**，这两个决策影响全局且越晚定越难改。

内容预算方面建议务实地采用"最小可发布集 + 增量扩充"策略，而不是在每个 Phase 末尾试图一次性填满所有资源槽位。

如果以上建议被采纳，这份文档将具备直接指导开发的能力。

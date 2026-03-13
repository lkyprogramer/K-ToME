# **类 ToME 回合制 Roguelike 核心架构与中后期开发技术白皮书 (Phase 2 \- Phase 5\)**

## **研发基线与架构演进概述**

本项目在 Phase 1 阶段已成功确立了基于 Java 21、Kotlin 2.2.21 与 libGDX 1.14.0 的技术基线 1。核心架构严格遵循了模块化隔离原则，纯 Kotlin 编写的 core 模块实现了零引擎依赖，确保了游戏核心逻辑（实体组件系统 ECS、BSP 地图生成、基础视野 FOV）与 client 渲染层的彻底解耦 1。通过构建可运行的客户端外壳与自我验证的战斗/成长循环，Phase 1 输出了具备 5 层地下城通关能力的最小可行性产品 (MVP)，并达成了核心包 80% 至 85% 的严格测试覆盖率指标 1。

本开发指导文档将全面定义 Phase 2 至 Phase 5 的高阶技术架构与具体工程实现路径。涵盖从复杂组件持久化、1000 能量调度模型、基于回调的深层天赋系统，到多层次地图生成、Diablo 式物品词缀经济学，以及最终的效用 AI 与图形渲染优化。全生命周期的系统设计均基于高测试覆盖率、确定性验证以及白盒可追溯的严格工程标准 1。文档以 Pull Request (PR) 级别的粒度对各阶段任务进行拆分，旨在为研发团队提供无缝对接的直接代码编写指导。

## **Phase 2: 核心系统深化与持久化架构 (Advanced Core & Persistence)**

Phase 2 的核心工程目标是将 Phase 1 的原型框架升级为支持复杂状态流转、高并发事件处理以及无损存档的成熟引擎级底层。这一阶段的技术决策将直接决定中后期游戏内容爆发式增长时的系统稳定性与可维护性。

### **PR 2.1: 确定性 1000 能量调度模型与流体回合体系**

传统的回合制游戏往往采用简单的交替行动逻辑，而类 ToME 架构要求实现极致细腻的“流动回合 (Fluid Turns)”体验。为了实现这一目标，系统必须在 core 模块中重构粗放的行动系统，引入基于 1000 能量阈值的严格优先级调度算法 2。

该算法的核心机制设定为：1 个标准全局回合等价于 1000 点能量 2。在底层的 Tick 循环中，所有的游戏内实体（玩家、怪物、独立运转的机关甚至持续性法术区域）基于其动态计算的 Speed 属性在每个逻辑刻内累积能量。当任意优先队列顶端的实体能量池达到或超过 1000 阈值时，引擎暂停全局时间流逝，将控制权（行动权）移交至该实体 2。该调度架构的最大技术优势在于极高的时间分辨率与战术深度。例如，如果玩家施放一个被天赋修正为仅需消耗 75% 标准回合的法术（即消耗 750 能量），该实体在行动结束后能量池剩余 250 点，在下一轮竞速中，该玩家将比消耗了 1000 能量的怪物更快地再次积累到 1000 能量，从而在宏观回合内实现“一回合多次行动”的战术速度压制 2。

在具体代码实现中，必须通过一个具有确定性排序规则的优先队列 (Priority Queue) 维护所有行动者 3。排序键值不仅包括实体现有能量，在能量同等时，需引入实体的唯一 UUID 作为次级键值，以保证在多目标同等速度下的行动顺序具备绝对的跨设备可重现性，这对于后续的回放系统与自动化测试至关重要。

进一步分析能量消耗模型，系统需要处理极为复杂的非标准资源逻辑，例如正负能量双轨制机制或特定职业的仇恨值 3。为了避免出现玩家在非战斗状态下通过原地空挥技能来无意义刷取资源的现象，资源调度器内部必须集成“能量阈值衰减模型”。当实体脱离战斗状态一定时间后，极端的资源数值（例如 \+50 的正能量或 \-50 的负能量）将自动向零点安全线回归 3。而在极端阈值区间时，特定的增益法术（如治疗光束或毁灭暗影）将触发效能的百分比缩放，甚至彻底失效，从而迫使玩家在战斗中进行精准的资源阈值管理 3。

### **PR 2.2: Kotlin 多态组件序列化与无损存档拓扑架构**

在 ECS 架构下实现无损的存档与读档功能，面临着组件类型高度碎片化与多态特征交织的严峻工程挑战。传统的 Java 原生 Serializable 接口在此类场景下暴露出严重的缺陷：其对反射机制的深度依赖引发了不可接受的性能瓶颈，且由于缺乏严格的 serialVersionUID 约束控制，游戏版本迭代中微小的字段修改极易导致旧版本存档的反序列化彻底崩溃 4。

基于上述痛点，Phase 2 规定全面引入官方标准的 kotlinx.serialization 框架作为项目唯一合法的核心序列化引擎 5。该方案通过编译期代码生成 (Code Generation) 技术彻底摒弃了运行时的反射损耗，不仅在速度上具备压倒性优势，更天然契合了 Android/libGDX 平台的 ProGuard 混淆安全策略 4。

在处理 ECS 实体挂载的千变万化的组件实例时，序列化拓扑必须处理多态数据结构。系统架构要求开发人员必须采用密封类 (Sealed Classes) 或带有 @Polymorphic 注解的抽象基类对所有基础数据传输对象 (DTO) 及组件进行严格建模 6。当保存游戏状态时，JSON 流中必须嵌入类鉴别器 (Class Discriminator)（本项目规范其默认属性字段名为 type），以指示序列化解析引擎在反序列化期间应当映射到哪一个具体的子类实例 7。

此外，在引入对外部 Mod 数据或松散耦合网络数据的解析场景时，常常会遇到缺乏严格类型标识符的 JSON 输入。为解决这一问题，系统架构内部需定制 JsonContentPolymorphicSerializer 拦截器 8。该拦截器允许反序列化逻辑分为两阶执行：首阶将不可知的 JSON 树整体提取为通用的 JsonElement 内存抽象；次阶逻辑则基于该 JSON 的形态特征（例如是否包含特定武器伤害键值或特定法术冷却键值）动态推断并分配正确的子序列化器，从而在不修改第三方数据结构的前提下实现多态组件的安全注入 8。

### **PR 2.3: 广义事件总线与生命周期回调钩子注入 (Hooks & Callbacks)**

硬编码的条件判断（如 if (hasShield) { damage \-= 10 }）在复杂的 Roguelike 引擎中是代码腐化的根源。为了支撑中后期成百上千种状态、天赋与装备特效的网状交互，系统必须剥离直接调用，引入一套基于发布-订阅模式的广义钩子 (Hooks) 与回调总线 (Callbacks) 机制 9。

这一设计哲学大量借鉴了 T-Engine 4 底层架构的精髓，将核心战斗演算类、行动调度类以及世界状态更迭类抽象为一个个遍布触发节点的管道 9。实体及其挂载的组件可以将自身的动态响应函数挂载到这些管道的特定生命周期节点上。当引擎主循环执行至对应节点时，将按优先级遍历并触发所有已注册的回调函数，从而实现无侵入式的业务逻辑注入。

为了规范开发，下表定义了 core 模块中必须实现且覆盖测试的核心回调基元及其标准载荷：

| 回调基元标准命名格式 | 引擎内部触发节点时机与业务应用场景剖析 | 必须携带的数据载荷与上下文要求 |
| :---- | :---- | :---- |
| callbackOnActBase | 在实体获得当前回合的最高优先级行动权，且即将在控制台或 AI 层展开决策逻辑之前触发。该钩子是处理所有持续性伤害（DoT，如毒素、流血衰减）以及生命/能量自然恢复效果的唯一合法入口 11。 | 当前行动实体对象的引用，当前时间戳偏移量。 |
| callbackOnMeleeHit | 在底层物理命中判定通过，承受近战物理攻击命中后，进入具体伤害结算前触发。主要用于实现伤害反弹 (Damage Reflection)、护甲破裂等受击被动反馈机制 11。 | 攻击方实体引用，原始未修正伤害数值字典。特别注意：目标引用应指向攻击方而非受击方 11。 |
| callbackOnRest | 玩家尝试进入大地图或安全区原地连续休息状态时触发。系统依靠此钩子验证周遭视野内的威胁程度，以决定是否立即阻断休息进程 11。 | 当前环境实体状态图谱，实体当前生命值与资源百分比。 |
| callbackOnDamageTaken | 在最终执行实体生命值扣除的前置顺位触发。它是实现各种动态护盾吸收 (Damage Shields)、抵挡致命一击的免死机制或将元素伤害按比例转化为魔法值等机制的核心节点 11。 | 经过所有护甲抗性结算后的最终预期伤害，伤害类型枚举，施暴源对象。 |

该回调系统的成功落地将极大地降低战斗结算核心类的圈复杂度。未来新增任何复杂的进阶职业或稀有怪物，研发团队无需触碰核心计算模块的代码，仅需编写对应天赋组件的回调脚本并将其附着于实体之上，即可无缝融入整个战斗生态 10。

## **Phase 3: 数据驱动的深层战斗与能力演化体系 (Deep Combat & Talents)**

Phase 3 的工程重担在于为枯燥的基础移动与平砍注入深度策略维度。该阶段需要构建出一套多层级、可嵌套的天赋树工程系统，并结合带有边际收益递减的非线性数值核心，从数学底层确保游戏在中后期极度自由的构建组合下，依然能够维持精妙的数值平衡。

### **PR 3.1: 抽象能力树与基于回调的天赋数据模式 (Gameplay Ability System)**

天赋 (Talents) 与技能库是本项目最核心的游玩资产。为了避免硬编码带来的代码臃肿，系统在架构设计上全盘吸收了现代引擎玩法能力系统 (Gameplay Ability System, GAS) 的数据驱动哲学 12。所有的天赋定义不再是派生自单一父类的繁杂类结构，而是被抽象为由数据配置（采用 YAML 或序列化的 JSON 进行描述）实例化的标准化模块 12。

在数据架构层面，每一个被 ActorTalents:loadDefinition 解析器加载的天赋模块都包含一套极其严密的元数据约束 14。天赋的核心标识依赖唯一的内部 short\_name，其在 UI 层的展示则映射为 name 属性 15。关于成长深度，系统通常设定 points 属性来限制玩家在该节点上能投入的最大技能点数（基准通常为 5 点上限） 15。而 require 字段则是一个复杂的对象结构，定义了点亮或升级该天赋所需的前置技能依赖图谱以及严苛的基础属性阈值门槛。例如，著名的“重甲专精 (Heavy Armour Training)”可能被配置为：在激活 1 级时仅仅要求角色达到 16 点力量，而在投入第 5 点以获取极限护甲加成时，力量要求则非线性跃升至 44 点 16。

为了确保系统稳定性与逻辑自洽，架构中必须明确一项关键的设计原则：关于前置属性门槛的条件验证，引擎仅在玩家进行“加点分配 (Allocation)”或“穿戴需求特定天赋的装备”的瞬间进行检查。倘若玩家在通过增益药水或临时装备凑够了属性门槛并激活了天赋，后续当药水失效导致属性回落时，系统绝不应当剥夺或惩罚已成功分配并激活的天赋节点 17。这种设计不仅极大降低了底层状态机反复校验属性的运算压力，也为玩家提供了更广阔的极限属性组合拼图空间 17。

在逻辑执行引擎侧，天赋的运作被严格解耦为数个标准的函数指针映射。对于需要主动释放的法术型天赋，其主要施法逻辑封装在 activate 函数内；而对于那些驻留于后台提供光环或状态的持续型天赋 (Sustained Talents)，则必须成对地提供 activate 与 deactivate 逻辑，以便在状态开关转换时精准地实施或撤销对基础属性的修正，并维持后台资源的持续扣减 15。此外，诸如永久提升属性上限的被动技能，则依靠在分配点数时触发 on\_learn 并在洗点回收时触发 on\_unlearn 函数来维护底层数值池的纯净性 15。

### **PR 3.2: 动态收益边际递减与非线性战斗公式重构**

在绝大多数缺乏严谨数值规划的 RPG 游戏后期，未经节制的数值线性叠加往往会引发不可挽回的战力崩坏问题。本项目深刻汲取了类 ToME 系统的教训，针对所有涉及对抗性的核心属性体系——包括但不限于精准度对抗闪避率、物理强度对抗物理豁免 (Physical Power vs Physical Saves)、法术强度对抗法术豁免体系，全面废除简单的线性加减乘除，全盘重构并引入经过高度校准的非线性战斗统计算法与边际收益递减模型 18。

在底层机制剖析中，物理强度 (Physical Power) 作为一个通用数值池，它不仅仅在常规战斗中决定武器面板基础伤害的最终转化乘区，更是在玩家或怪物试图向目标施加包括震慑、流血、击退在内的诸多负面物理状态时，充当进行状态命中检定的对抗基数 18。为了阻止玩家通过单一属性的无限堆高来达成彻底免疫敌方控制或做到绝对闪避的“破局 (Break the game)”现象，所有生存侧属性，诸如防御评级 (Defense)、攻击精准度 (Accuracy) 以及三围抗性豁免 (Physical/Mental/Spell Saves)，在架构底层均必须受到极其严格的收益递减机制约束 19。

在具体的数学计算实现上，玩家在装备属性面板观察到的附加数值被称为“原始属性值 (Raw Value)”。在进入核心战斗演算模块前，这个原始数值必须流经一个精心设计的对数衰减函数或平方根平滑函数，最终转换为“有效属性值 (Effective Value)”。这意味着在角色发展的最初阶段，提升 10 点原始防御可能直接转化为 10 点有效防御；但随着基础数值攀升至极高段位，玩家可能需要堆叠高达 50 甚至 100 点的原始防御，才能在有效防御评级上再挤出微不足道的 1 点增益 19。这一极其重要的安全阀门机制，确保了哪怕在最高难度层级，敌方低阶怪物群体的集火攻击依然能凭借统计学概率撕开玩家的防线造成实质性威胁；同时也促使玩家在游戏后期放弃无脑单堆某一抗性，转而寻求降低暴击伤害惩罚 (Crit Shrug Off) 等更多维度的生存策略 19。

另一方面，对于主动伤害技能的缩放成长，系统坚决避免直接使用固定数值。所有的天赋强度演算依赖于“有效天赋等级 (Effective Talent Level)”概念，该数值由玩家实际投入的点数基础结合武器精通或职业专精系数相乘得出 21。玩家能够通过搜寻提供 Talent Mastery 词缀的极品装备，使某个分类的技能等级打破系统默认的 5 级理论上限，向更高的境界进发 21。然而，技能数值同样遵循收益递减曲线，这就要求设计人员在配置技能 YAML 表时设立特殊的断点 (Breakpoints) 机制。例如，某个技能在 1 级至 2 级时只提供微小的伤害加成，但在达到 3 级断点时发生机制质变从而获得广域 AoE 溅射效果，在达到 5 级断点时大幅缩减冷却时间 21。这种基于断点的收益衰减模型，将赋予玩家在有限的技能资源下极其丰富的决策拉扯体验。

### **PR 3.3: 动态仇恨网络焦点与战术潜行博弈逻辑**

为了从根本上拔高战术层面的博弈深度，仅依靠数值体系的碰撞是远远不够的。针对传统 Roguelike 游戏中普遍采用的基于绝对全局视野坐标的粗暴寻路算法，Phase 3 的 AI 底层将迎来一次革命性的重构，全面引入“仇恨系统 (Hate System)”与区域动态感知焦点机制 23。

在重构后的体系中，潜行状态 (Stealth) 的底层逻辑被彻底剥离了单一的“绝对隐形/暴露”二元判定。潜行成为一种可成长的检定属性，玩家当前的潜行等级将实时与周围敌人的感知能力进行数学对抗。若检定判定成功，玩家在敌人的感知雷达中将处于脱战未激活状态 25。

然而，这种潜行并非意味着全知全能的隐身。如果潜行中的玩家在某个坐标处对敌人造成了不可预见的伤害，或者发动了伴随高强度噪音的毁灭性法术，系统将在该事件发生的地块坐标上生成一个隐形的“仇恨焦点 (Hate Focus)”锚点 25。周遭受到惊动或伤害的敌对 AI 系统并不会立刻作弊般地获取玩家真实的当前绝对坐标，而是基于自身的寻路算法，迅速向这个最后已知的仇恨焦点进行压制性移动与火力覆盖包围 25。这种“基于推断而非基于全知全能”的逻辑设计，打破了诸多同类作品中由于潜行机制被一次偶然识破就招致全图怪物无限追杀的糟糕体验，为偏向刺客流派的构筑保留了真实的战术位移拉扯空间与重新潜伏的机会，极大地深化了探索过程中的紧张感与沉浸感。

## **Phase 4: 多维拓扑地图生成与经济掉落生态 (Advanced ProcGen & Loot)**

地下城未知环境的探索震慑感与击杀强敌后获取极品战利品的成就感，共同构成了 Roguelike 循环中最为牢固的核心驱动力。Phase 4 的技术焦点在于重构引擎的地图生成器，并全面接入复杂的装备分级与词缀生成算法，从而为玩家源源不断地提供不可预测的深度体验。

### **PR 4.1: 基于 Park-Miller 偏移与混合拓扑的不完美迷宫生成器**

回顾 Phase 1 的开发过程，纯粹的二叉空间分割 (BSP) 算法虽然能够快速、稳定地切割生成结构严密且相互连通的矩形房间，但由于其拓扑过于规整，极易导致玩家在几层探索后产生极其严重的审美疲劳与空间体验单一化 1。基于此缺陷，系统环境生成器将在本阶段演进为以图案拼接 (Pattern-based Architecture) 为基础，结合随机连通性算法生成的不完美迷宫 (Imperfect Mazes) 混合型方案 26。

全新生成逻辑的第一阶段负责确定宏观聚落分布。引擎首先通过引入 Park-Miller 正态分布统计算法，精确推算出当前目标楼层应当生成的房间总数以及各自的预期面积 28。使用该分布模型的绝妙之处在于，它在底层数学上刻意倾斜了概率权重，强制使得狭小房间生成的数量呈指数级多于宽阔的大厅，这种分布特征高度契合了地下遗迹或洞穴系统真实的聚落生态分布学 28。

在生成坐标的次级阶段，针对初始密集抛洒且相互重叠的巨量矩形房间，系统将部署群体分离控制行为 (Separation Steering Behavior) 算法。该物理模拟算法会将重叠的房间模型在二维平面上互相缓慢推开，直到整个网格平面上不存在任何一处非预期的边缘交叉点；随后，算法自动搜寻因排斥产生的空间裂隙，并使用 1x1 的标准网格元素填充这些缝隙，从而平滑地交织出极具自然延伸感的走廊网络 28。

为了打破 BSP 生成法所造成的“完美迷宫”（即从起点至终点的图论路径中，任意两点间仅有一条绝对路径，整个迷宫拓扑可拍平为一棵树）所带来的枯燥感，生成器将在地图的节点拓扑连通图中，强制且随机地刻蚀出大量的冗余物理连接与循环回路 (Loops/Cycles) 27。这种设计能够允许玩家在被怪物群堵截时拥有绕后的战术迂回空间。

更为重要的是，本阶段将正式引入由关卡设计师预制的“图案库 (Patterns)”元数据系统。特定的大型房间结构被封装为具备独立入向链接节点 (Inlinks) 和出向链接节点 (Outlinks) 的微缩剧本空间（例如，内部含有独特触发陷阱阵列与暗门宝箱的高级 Vault）。生成引擎将主动解析这些图案的元数据，并尝试将这些具备极高战略价值的高风险高回报区域无缝缝合至主迷宫拓扑图的深层盲端尽头 26。对于部分包含了锁与钥匙机制 (Lock and Key) 的特殊剧情楼层，过程生成算法引擎在落图之前，必须首先在抽象的有向无环图 (DAG) 拓扑层面进行极其严苛的可解性验证计算，从数学源头上确保“开启障碍的钥匙”绝对被投放生成在“被锁住的门”之前必定可以访问的逻辑子集中，彻底杜绝逻辑死局的产生风险 29。

### **PR 4.2: 复杂词缀池、生成管线与基于 Prolog 约束的动态装备学**

极具拓展深度的战利品系统是玩家重复爆肝游玩的最大驱动内核。构建一套能够支撑成百上千种属性动态随机组合、且逻辑严密不崩溃的类 Diablo/ToME 高度可扩展词缀生成 (Affix Generation) 逻辑，是本阶段装备学的开发重心 30。

系统架构要求在游戏核心静态内存中预先装载并维护一套庞大的基础物品模型库与词缀数据库。每一次战利品生成，引擎必须严格遵循以下参数所构筑的多重限制防火墙体系 31：

| 引擎参数底层代号 | 英文全称释义 | 核心逻辑运转机制与系统制约功能深度剖析 |
| :---- | :---- | :---- |
| iLvl | Item Level | 物品绝对等级。该等级是一个动态继承值，通常直接继承自掉落该物品的怪物自身等级 (mLvl)，或是开启宝箱时所在的区域地牢等级 (aLvl) 31。该等级构成了决定该基础物品在随后生成中能够抽取到何种高级前缀或后缀的终极天花板约束。 |
| qLvl | Quality Level | 品质介入等级。静态定义了某个特定的基础物品类型或者某条极其变态的词缀，能够在游戏全流程中最早合法现身的阶段节点。在判定生成时，一旦检定到生成来源的 iLvl 低于词缀硬编码的 qLvl，该词缀将被彻底隔离出抽取池，绝对不会生成，从而防止低级区域掉落破坏平衡的终极神器 31。 |
| Rarity | Rarity / Tier | 整体稀有度分级。在最初投骰决定，划分物品呈现为普通（白板）、魔法（蓝色，通常配置 1-2 条词缀）、稀有（黄色，支持 4-6 条词缀复合），或是传奇怪物独占的神器级别（携带不可被随机生成的唯一特性属性） 31。 |
| Alvl | Affix Level | 词缀等级阈值。独立于物品等级体系之外的子属性架构。在引擎计算当前可用的最终合法词缀池时，会基于极其复杂的公式将其与 iLvl 进行比对过滤，动态剔除超出当前允许范围的超阶词缀及毫无意义的绝对低阶词缀，保证生成的物品具备实际装备价值 31。 |

战利品生成的完整流程遵循一套不可逆的严格管道机制：第一步，基于怪物所属的 Treasure Class (TC) 基础权重，判定基础物品的基底模型类别（如一把基础宽剑）；第二步，根据怪物的精英类别判定该基底的稀有度提升跃迁概率 32。当引擎确立该物品晋升为“魔法”或“稀有”品质的瞬间，逻辑控制流将回退至一套类似 Prolog 语言风格的内部模式匹配引擎，该引擎会基于复杂的过滤网络，从完全符合当前 qLvl 约束与类型限制前提的词缀数据库中，随机抽取对应数量的合法前缀与后缀，并分别赋予其一个在设定极值范围内的随机浮动数值 30。

### **PR 4.3: 随机神器与缝合首领的涌现性设计引擎 (Randarts & Randbosses)**

为打破游戏中后期因怪物种类穷尽而带来的探索平淡期，增加关卡内部的极端意外性与不可预知挑战，引擎系统在此阶段引入两套极为大胆的涌现性生成机制：随机神器 (Randarts) 与随机首领 (Randbosses) 生成器。

对于随机神器 (Randarts) 的流水线构建，其核心生成规律设定为四步走策略：首先，随机锁定一种特定等级区间内的基础物品类型与其材质模板；其次，将其原本的固有基础属性参数在生成时强制放大提拉 20%（为防止物理计算崩盘，武器白字伤害属性仅限制提升 10%）；接着，赋予该物品一到两个从常规魔法词缀库中抽取出的强大前缀或后缀效果；最后一步，也是质变的一步，生成器将分析该神器的生成元素亲和主题，并强制向其中注入一条极其特殊的全局被动能力或是高概率的战斗技能触发词缀（例如，造成巨额的法术暴击增伤，或是赋予受击时概率回复巨量生命值的机制） 22。

对于更具挑战性的随机首领 (Randbosses) 创造，系统展现出更为狂野的设计。引擎不再使用预设的怪物模板，而是将其直接设计为不可理喻的双职业混合变异体 35。在构建期间，引擎会从当前游戏内所有可用的玩家英雄职业池中，完全不考虑相性地随机挑选两个完全不同的职业模板，粗暴地将两者对应的高阶天赋树合二为一，并为该实体分配能够完美匹配其崭新技能体系的高阶极品武器库 35。由于这种生成是纯粹的动态随机拼接，不仅不受人工平衡性的制约，反而极易因机制碰撞产生具有致命连锁 Combo 的逆天怪物。例如，系统可能偶然融合出一个携带着奥术之刃与刺客流氓体系双重特性的首领 Boss，这个怪物在实战中可能会同时开启奥术风暴 (Arcane Storm) 的不可规避巨额范围法术伤害，同时又运用隐身突刺的物理爆发贴脸进行毁灭打击，这种涌现性的遭遇战将迫使身经百战的玩家在直面这些不可名状的缝合强敌时，必须倾尽所能地思考应对与撤退的战略预案 35。

## **Phase 5: 战术级 AI 与高阶图形管线性能优化 (Tactical AI & Polish)**

开发周期的最终冲刺阶段，其宏观重心在于将原本僵化执行追逐指令的寻路怪物群体，脱胎换骨般转化为具备真实压迫感与群体协同的战术对手。同时，必须在底层代码级别彻底排除 Kotlin 在桌面端或移动 Android 平台 Dalvik/ART 运行时，由于垃圾回收堆积 (GC Pauses) 或渲染管线满载而引发的性能骤降，确保最高烈度下的团战依然如丝般顺滑。

### **PR 5.1: 融合效用 AI 与行为树执行的双轨制战术智能体架构**

面对不同位阶与职能设定的各类敌人，如果统一采用传统的有限状态机 (FSM) 架构来编写，其状态图谱很快便会因为交错纵横的条件跳转而变得过于单薄且极其难以维护。现代顶级 Roguelike 引擎的战术智能，必须通过深层融合效用 AI (Utility AI) 评估机制与行为树 (Behavior Trees) 执行流程，来妥善处理复杂的瞬息战场评估与高频动作执行 36。

系统工程实践分析表明，效用 AI 极其适合处理开放动态空间内“宏观层面我要决定做什么”的高级动机决策系统 37。在每个时间切片中，智能体系统会通过庞大的环境数据采集探针，大量收集（距离玩家的绝对格数距离、智能体自身的当前生命值百分比、不同玩家单位当前的火力威胁度、自身强力技能的冷却就绪状态等指标），随后将这些离散指标输入至内部设定的多条“考量曲线 (Consideration Curves)”函数中，计算得出当时每一种可能采取的潜在行动的绝对效用积分值 36。

举例说明，一个扮演“深潜者 (Deep One)”的坦克怪物，它被赋予的底层动机优先级并非仅仅是寻找最近的目标造成物理平砍伤害，它真正的恐怖之处在于通过移动寻找最优走位，将自己置身于玩家阵型的几何辐射中心区域，以此最大化其独有的大范围群体精神鞭笞技能的伤害与理智值削弱覆盖面 38。在计算效用时，当环境探针侦测到该怪物周围一定半径内同时存在多名扎堆的玩家实体时，其“移动至敌群中心区域”这一动作选项的考量函数得分将呈现指数级飙升，使其毫不犹豫地冲入敌阵施放 AoE 攻击 38。相对的，面对以撕裂防线为己任的“疯子 (Maniac)”类信徒怪物，其核心效用函数则被设计为高度挂钩玩家团队中精神理智值最低的角色。一旦扫描到目标，其效用动机将驱使它主动承受伤害越过玩家前排防线，使用物理锁链技能将理智濒临崩溃的脆弱玩家单位直接拖离阵营阵型，实施孤立打击 38。

然而，尽管效用 AI 在寻找最高优先级动机的选择算法上无比强大且符合直觉，但开发人员在纯使用该技术时极易陷入为了让怪物走好一步路而“无止境手动调整函数曲线系数”的研发泥潭 36。因此，本阶段在具体战术执行层面（解决“我决定做某事后，具体该如何去走完流程”），则全权移交接力给底层的行为树系统 (Behavior Trees) 托管执行。例如，效用 AI 计算后决定了怪物当前必须执行“残血规避并采取游击战术”这一顶级动机指令，随后被激活的行为树系统便会负责按树形节点逐一展开具体的：脱离近战距离寻路、搜寻周遭地形掩体以规避障碍物、利用转角阻断玩家的法术直线视野遮挡以及随后利用掩护进行远程还击射击这一整套闭环战术逻辑 39。这种宏观感性决策与微观精准执行分层的双轨制 AI 架构，不仅在表现层极大地拔高了怪物的群体拟真度与危险气息，更在工程代码层级保持了各子模块极高的隔离度与可维护性。

### **PR 5.2: 视野视线算法的终极较量与选型剖析：阴影投射 vs 优化射线追踪**

在类 ToME 这样基于地块单元格进行探索计算的游戏中，视线 (Line of Sight) 阻隔以及广域视野 (Field of View, FOV) 的光照范围重组与计算，无疑是占据每个标准行动回合中最为沉重的 CPU 运算开销负担。如何架构该模块的设计逻辑，将迫使技术总监在极致的微秒级执行性能与无死角的绝对精确展现之间做出最残酷的权衡平衡。

当前在业界主流的开源底层算法主要分为两大阵营派系：向外光栅化扫描的阴影投射 (Shadowcasting) 与向内暴力追溯的光线投射 (Raycasting) 40。

| 底层算法核心工程思想 | 该算法带来的显著技术优势 | 面临的技术劣势与潜在的致命逻辑异常 |
| :---- | :---- | :---- |
| **阴影投射 (Shadowcasting)** | 算法原理为从玩家所在的中心原点向外扩展，依次延展出扇形的光照扫描区域，当触及障碍物墙体时对扇形进行切割或遮挡计算。其最大的优势在于时间复杂度极低，执行速度快若闪电，理论上其优秀实现能够做到确保探测范围内的几乎每一个方块仅仅被引擎的扫描针评估一次 41。 | 当引擎在处理对角线走向的深邃隧道或是遍布诸如圆柱体、细小林木等碎片化障碍物的旷野区域时，极易产生令人无法理解的视野不对称现象，甚至引发致盲的角落盲点异常 (Blind Corners) 现象 41。更甚者，其光束在穿透半开的门缝结构后的物理散射范围往往大得极其不自然 41。 |
| **射线追踪 (Raycasting)** | 基于计算机图形学中著名的 Bresenham 直线算法等暴力形式作为基石，强制从中心原点向被划定为最大可视范围边界（Bounding Box）边缘的每一个目标点位单元格，各自独立发射一条物理射线。射线不断前推，直至中途撞击到定义为不透明的墙体结构或是耗尽了允许的最大视野距离范围方告停止运算 42。 | 显而易见，由于射线在靠近原点的位置存在极其庞大且密集的计算重叠与路径冗余，当处于缺乏遮挡物阻碍的极端宽阔平原地带时，其造成的性能损耗开销将被成倍放大 42。 |

结合本作实际开发阶段中的高压实机体验与覆盖率测试反馈，本项目在系统最底层的 FOV 生成选型上，最终拍板敲定建议全面采用针对网格坐标系进行过深度遍历改造与优化的暴力射线追踪算法 (Optimized Raycasting) 43。虽然在极限空旷区域下，其冗余的计算开销要大于阴影投射，但作为等价交换，其在极端畸形地貌边缘产生的光照视野结果最为宽容、对称且高度符合玩家现实生活中的直观视觉预期。它能够凭借无死角的细密射线，精准捕获并照亮那些在奇怪形状地貌边缘、通过极其狭窄的缝隙透出的残片视野。这对于一款需要极其精确研判敌方站位战术的硬核游戏，以及在未来扩展规划中大概率可能引入可透视的半透明墙体结构 (Semi-transparent Cells, 诸如水晶墙或玻璃门) 的复杂地形来说，是属于不可妥协的核心支柱特性 42。

为了彻底抵消这种暴力算法带来的无用性能挥霍开销，FOV 计算引擎层被死死限制住执行权限：该计算函数全量被缓存，并引入脏数据标识 (Dirty Flags) 系统。唯有当玩家的控制原点坐标确实发生了跨越实体地块的位移，或者是玩家当前理论视野感知范围内的某些关键大型环境遮挡物状态在回合中发生了物理改变（例如强行撞开或者使用爆破法术彻底摧毁掉了一扇实体的木门）时，缓存机制才会被激活并标记为脏数据，进而在下一帧的重绘中触发全局 FOV 层的全量重新射线计算刷新操作，其余大部分待机时间均直接调用上一次计算的光照遮挡缓存图谱，从而彻底解决了该算法的效率顽疾。

### **PR 5.3: libGDX 内存对象池机制与管线级图形渲染重度调优**

在由 client 模块全权掌管的画面渲染展现层中，尽管 libGDX 引擎自身已经极其尽责地尽可能利用底层的封装抽象手法，屏蔽掉了绝大多数诸如上下文切换等晦涩难懂的底层 OpenGL 图形库 API 细节，但在面对游戏步入后期、呈现动辄数十个怪物同屏混战、巨量法术粒子特效喷发交织以及每秒上百条伤害数据 UI 飘字刷新的宏大战斗场景下，其默认的绘制管线极易因为负荷过载而瞬间击穿客户端要求持平在 60 FPS 的性能容忍底线 44。对此，必须强制引入更为系统、深层次的专业性能探针分析与结构性优化手段。

在排查瓶颈的手段上，开发人员被要求在主渲染循环的顶部区域，手动实例化并长期启用 GLProfiler 分析工具组件（通过代码内硬调用 glProfiler.enable() 函数）。如此一来，系统便可以在游戏暗盘静默运行时，不间断地监控并记录所有隐藏在冰山底部的底层 GL 调用脉冲次数与偶发错误 44。在进行性能压测时，需要高度关注的两个红线核心指标分别为 Draw Calls（每帧累加的全局管线绘制调用频次）与 Texture Bindings（由显存至内存的底层纹理句柄绑定切换次数）。经过多轮实机测试论证，在每一个渲染帧流转的极短周期内，不同纹理单元之间来回频繁的加载与切换卸载动作，是不可争议地死死拖慢且锁死整个 GPU 图形渲染流水线执行速率的最核心致命元凶 44。

为了对症下药，系统架构在资源加载的规范层面做出了极其严苛的硬性要求：项目中所有的底层环境地砖贴图、敌方单位动画精灵图切片以及极其细碎的 UI 状态栏图标资源，在开发期的打包阶段，均必须依靠强大的离线打包工具链 TexturePacker，根据其视觉逻辑的分组，被无损且紧凑地离线压榨并打包拼接为若干张最高尺寸控制在 ![][image1] 像素、且必须严格遵循 2 的次幂比例尺寸的巨幅合并纹理图集 (TextureAtlas) 44。针对游戏内诸如地表、实体层与上层 UI 面板这三个不同 Z 轴渲染层级的先后绘制顺序关系进行切分，明确划分出专属的合集贴图；在运行时的绘制循环代码层面，必须确保在调用同一个 SpriteBatch 实例的 begin() 方法与 end() 方法的这一个不间断的完整生命渲染周期内，全部完成并只绘制塞属于同一个指定纹理图集池中包含的所有目标对象切片，依靠此种大批量合并同类项的批处理强制打包渲染模式，将帧间昂贵且无意义的纹理 ID 切换次数极限压缩降至接近物理底线的个位数区间 45。

除此之外，另一个悬挂在系统头顶的达摩克利斯之剑则是由高频对象分配引发的瞬时内存震荡问题，这在深度依赖 JVM（Java 虚拟机）托管特性的语言（不论是 Java 还是 Kotlin）中尤为致命。尤其是在遇到跨越大范围房间的 FOV 视野全量重新扫线计算、成百上千条战斗微观判定日志的高频并发生成追加，以及海量流体法术粒子特效密集创建并播放时，这种放任自流、海量且不加节制的短生命周期临时对象内存分配（new Object()），将毫无悬念地快速塞满年轻代内存空间，进而在最为关键的战斗节骨眼上，频繁触发使得整个游戏主线程强行挂起冻结的恶性垃圾回收停顿暂停事件 (Garbage Collection Pauses, 尤为多见于 Android 移动端 Dalvik/ART 运行时下) 46。

针对这类在游戏运转期间会呈现出呈指数级爆发式高频创建而后又被迅速丢弃销毁的数据载体组件或是渲染精灵载体组件，引擎底层的重构指导方案是必须全面且强制性地接入并应用 libGDX 引擎自身内置且久经考验的 Pool 对象内存池复用接口管理机制 46。同时，在更为宏观的上层视口渲染空间控制策略上，对于那些坐标位移距离摄像机视口追踪镜头中心极远、或者是经由视线算法判定完全处于当前玩家不可观测视野范围黑洞之外盲区深处的那些敌方实体，系统在底层应当果断引入基于粗略空间欧氏距离的高效剔除剔除检测机制 (Distance Check & Frustum Culling)。在判定其隐形后，不但应当立即跳过剥夺其在屏幕上的绘制定点占用，更应当干脆利落地彻底切断并停止其附带的极其耗费 CPU 运算算力的骨骼动画矩阵实时变换更新循环。经过针对此类手段的交叉调优应用论证，此套严密的渲染约束规范能够将每一帧那长久以来令人绝望的、动辄高达 2000 毫秒级别的可怕更新渲染耗时峰值，犹如遭遇断崖式斩击一般，被稳稳地按压降低至 120 毫秒以内的绝对安全冗余区间之中，从而以一种极度强硬的工业级姿态，牢牢保障了本款游戏产品在面对后期那如同修罗场般的巨型同屏怪海决战时，于跨越各个年代、涵盖高低性能跨度极其巨大的各类软硬件平台上，均能够始终维持在绝对顺滑且稳定运行的最终目标之上 47。

#### **Works cited**

1. 2026-03-12-phase1-roadmap.md  
2. How does a reduction in usage speed work? :: Tales of Maj'Eyal General Discussion, accessed on March 13, 2026, [https://steamcommunity.com/app/259680/discussions/1/3823032344640711828/](https://steamcommunity.com/app/259680/discussions/1/3823032344640711828/)  
3. Anorithil resource system \- ToME: the Tales of Maj'Eyal, accessed on March 13, 2026, [https://forums.te4.org/viewtopic.php?t=42928](https://forums.te4.org/viewtopic.php?t=42928)  
4. The Android Developer's Guide to Not Messing Up Serialization | by Siamak Mahmoudi | ProAndroidDev, accessed on March 13, 2026, [https://proandroiddev.com/the-android-developers-guide-to-not-messing-up-serialization-3d27a41460c1](https://proandroiddev.com/the-android-developers-guide-to-not-messing-up-serialization-3d27a41460c1)  
5. Serialization | Kotlin Documentation, accessed on March 13, 2026, [https://kotlinlang.org/docs/serialization.html](https://kotlinlang.org/docs/serialization.html)  
6. PolymorphicSerializer | kotlinx.serialization – Kotlin Programming Language, accessed on March 13, 2026, [https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic-serializer/](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic-serializer/)  
7. Intro to Polymorphism with Kotlinx.Serialization \- Livefront, accessed on March 13, 2026, [https://livefront.com/writing/intro-to-polymorphism-with-kotlinx-serialization/](https://livefront.com/writing/intro-to-polymorphism-with-kotlinx-serialization/)  
8. JsonContentPolymorphicSerializer | kotlinx.serialization – Kotlin Programming Language, accessed on March 13, 2026, [https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/)  
9. Hooks \- Tales of Maj'Eyal, accessed on March 13, 2026, [https://te4.org/wiki/Hooks](https://te4.org/wiki/Hooks)  
10. An introduction to event hooks and callbacks in IDA with hook notification points \- YouTube, accessed on March 13, 2026, [https://www.youtube.com/watch?v=D6ESSBNMwvQ](https://www.youtube.com/watch?v=D6ESSBNMwvQ)  
11. Callback \- Tales of Maj'Eyal, accessed on March 13, 2026, [https://te4.org/wiki/Callback](https://te4.org/wiki/Callback)  
12. Unreal Engine's Gameplay Ability System — Part 21: Skill Tree Setup — Tutorial \- YouTube, accessed on March 13, 2026, [https://www.youtube.com/watch?v=B\_YQblpl21o](https://www.youtube.com/watch?v=B_YQblpl21o)  
13. Template:TalentReq \- Tales of Maj'Eyal, accessed on March 13, 2026, [https://te4.org/wiki/Template:TalentReq](https://te4.org/wiki/Template:TalentReq)  
14. Class engine.generator.interface.ActorTalents, accessed on March 13, 2026, [https://te4.org/docs/t-engine4/1.4.8/classes/engine.generator.interface.ActorTalents.html](https://te4.org/docs/t-engine4/1.4.8/classes/engine.generator.interface.ActorTalents.html)  
15. T4 Modules Howto Guide/Talents \- Tales of Maj'Eyal, accessed on March 13, 2026, [https://te4.org/wiki/T4\_Modules\_Howto\_Guide/Talents](https://te4.org/wiki/T4_Modules_Howto_Guide/Talents)  
16. Talent \- Tales of Maj'Eyal, accessed on March 13, 2026, [https://te4.org/wiki/Talent](https://te4.org/wiki/Talent)  
17. The More You Know: Little-known quirks and game mechanics \- ToME: the Tales of Maj'Eyal, accessed on March 13, 2026, [https://forums.te4.org/viewtopic.php?t=43659](https://forums.te4.org/viewtopic.php?t=43659)  
18. are there actually two types of physical power? \- ToME: the Tales of Maj'Eyal, accessed on March 13, 2026, [https://forums.te4.org/viewtopic.php?t=33563](https://forums.te4.org/viewtopic.php?t=33563)  
19. General Mechanics Reference & Guide : r/ToME4 \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/ToME4/comments/tzd8r4/general\_mechanics\_reference\_guide/](https://www.reddit.com/r/ToME4/comments/tzd8r4/general_mechanics_reference_guide/)  
20. \[ToME\] Rescaled combat stats guide : r/roguelikes \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/roguelikes/comments/3nokaw/tome\_rescaled\_combat\_stats\_guide/](https://www.reddit.com/r/roguelikes/comments/3nokaw/tome_rescaled_combat_stats_guide/)  
21. Effective Talent level : r/ToME4 \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/ToME4/comments/1qefnkn/effective\_talent\_level/](https://www.reddit.com/r/ToME4/comments/1qefnkn/effective_talent_level/)  
22. Randart generation \- ToME: the Tales of Maj'Eyal, accessed on March 13, 2026, [https://forums.te4.org/viewtopic.php?t=23105](https://forums.te4.org/viewtopic.php?t=23105)  
23. Ideas for New Mechanics and Challenges in ToME 4 \- ToME: the Tales of Maj'Eyal, accessed on March 13, 2026, [https://forums.te4.org/viewtopic.php?t=56019](https://forums.te4.org/viewtopic.php?t=56019)  
24. The Defining Features of ToME4 \- ToME: the Tales of Maj'Eyal, accessed on March 13, 2026, [https://forums.te4.org/viewtopic.php?t=28895](https://forums.te4.org/viewtopic.php?t=28895)  
25. Stealth Mechanic Clarifications \- ToME: the Tales of Maj'Eyal, accessed on March 13, 2026, [https://forums.te4.org/viewtopic.php?t=45048](https://forums.te4.org/viewtopic.php?t=45048)  
26. Procedural Room Generation Explained \- MagicalTimeBean, accessed on March 13, 2026, [https://www.magicaltimebean.com/2014/11/procedural-room-generation-explained/](https://www.magicaltimebean.com/2014/11/procedural-room-generation-explained/)  
27. Rooms and Mazes: A Procedural Dungeon Generator \- Bob Nystrom \- stuffwithstuff, accessed on March 13, 2026, [https://journal.stuffwithstuff.com/2014/12/21/rooms-and-mazes/](https://journal.stuffwithstuff.com/2014/12/21/rooms-and-mazes/)  
28. Procedural Dungeon Generation Algorithm Explained : r/gamedev \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/gamedev/comments/1dlwc4/procedural\_dungeon\_generation\_algorithm\_explained/](https://www.reddit.com/r/gamedev/comments/1dlwc4/procedural_dungeon_generation_algorithm_explained/)  
29. An introduction to procedural lock and key dungeon generation \- The Shaggy Dev, accessed on March 13, 2026, [https://shaggydev.com/2021/12/17/lock-key-dungeon-generation/](https://shaggydev.com/2021/12/17/lock-key-dungeon-generation/)  
30. Incremental Learning of Affix Segmentation \- ACL Anthology, accessed on March 13, 2026, [https://aclanthology.org/C12-1116.pdf](https://aclanthology.org/C12-1116.pdf)  
31. Item Generation Tutorial \- Diablo Wiki, accessed on March 13, 2026, [https://diablo2.diablowiki.net/Item\_Generation\_Tutorial](https://diablo2.diablowiki.net/Item_Generation_Tutorial)  
32. // Item Generation Notes ( How the game works ) Still accurate? \- General Discussion \- Diablo 2 Resurrected Forums, accessed on March 13, 2026, [https://us.forums.blizzard.com/en/d2r/t/item-generation-notes-how-the-game-works-still-accurate/40726](https://us.forums.blizzard.com/en/d2r/t/item-generation-notes-how-the-game-works-still-accurate/40726)  
33. \[Text\] A Guide to the Basic Terms of Item Generation by Khegan : r/Diablo \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/Diablo/comments/pimx3a/text\_a\_guide\_to\_the\_basic\_terms\_of\_item/](https://www.reddit.com/r/Diablo/comments/pimx3a/text_a_guide_to_the_basic_terms_of_item/)  
34. Randarts... \- ToME: the Tales of Maj'Eyal, accessed on March 13, 2026, [https://forums.te4.org/viewtopic.php?t=24037](https://forums.te4.org/viewtopic.php?t=24037)  
35. How does randboss generation work? : r/ToME4 \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/ToME4/comments/16v6vv8/how\_does\_randboss\_generation\_work/](https://www.reddit.com/r/ToME4/comments/16v6vv8/how_does_randboss_generation_work/)  
36. Utility AI vs BT for enemies : r/gamedev \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/gamedev/comments/196392u/utility\_ai\_vs\_bt\_for\_enemies/](https://www.reddit.com/r/gamedev/comments/196392u/utility_ai_vs_bt_for_enemies/)  
37. Utility AI / restructuring the AI system \- Programming \- Thrive Development Forum, accessed on March 13, 2026, [https://forum.revolutionarygamesstudio.com/t/utility-ai-restructuring-the-ai-system/919](https://forum.revolutionarygamesstudio.com/t/utility-ai-restructuring-the-ai-system/919)  
38. Indie AI Programming: From behaviour trees to utility AI \- Game Developer, accessed on March 13, 2026, [https://www.gamedeveloper.com/programming/indie-ai-programming-from-behaviour-trees-to-utility-ai](https://www.gamedeveloper.com/programming/indie-ai-programming-from-behaviour-trees-to-utility-ai)  
39. What is your favorite AI behavior framework? : r/unrealengine \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/unrealengine/comments/1ivpgmh/what\_is\_your\_favorite\_ai\_behavior\_framework/](https://www.reddit.com/r/unrealengine/comments/1ivpgmh/what_is_your_favorite_ai_behavior_framework/)  
40. Which is faster: raycasting or shadowcasting? \- Game Development Stack Exchange, accessed on March 13, 2026, [https://gamedev.stackexchange.com/questions/11037/which-is-faster-raycasting-or-shadowcasting](https://gamedev.stackexchange.com/questions/11037/which-is-faster-raycasting-or-shadowcasting)  
41. Roguelike Vision Algorithms \- Adam Milazzo's Personal Site, accessed on March 13, 2026, [https://www.adammil.net/blog/v125\_Roguelike\_Vision\_Algorithms.html](https://www.adammil.net/blog/v125_Roguelike_Vision_Algorithms.html)  
42. Reducing the Number of Required Iterations in Ray-Casting : r/roguelikedev \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/roguelikedev/comments/pmrkvi/reducing\_the\_number\_of\_required\_iterations\_in/](https://www.reddit.com/r/roguelikedev/comments/pmrkvi/reducing_the_number_of_required_iterations_in/)  
43. What the Hero Sees: Field-of-View for Roguelikes \- Bob Nystrom \- stuffwithstuff, accessed on March 13, 2026, [https://journal.stuffwithstuff.com/2015/09/07/what-the-hero-sees/](https://journal.stuffwithstuff.com/2015/09/07/what-the-hero-sees/)  
44. Profiling \- libGDX, accessed on March 13, 2026, [https://libgdx.com/wiki/graphics/profiling](https://libgdx.com/wiki/graphics/profiling)  
45. The LibGDX performance guide \- Yair Morgenstern \- Medium, accessed on March 13, 2026, [https://yairm210.medium.com/the-libgdx-performance-guide-1d068a84e181](https://yairm210.medium.com/the-libgdx-performance-guide-1d068a84e181)  
46. why game is running slow in libgdx? \- Stack Overflow, accessed on March 13, 2026, [https://stackoverflow.com/questions/17347883/why-game-is-running-slow-in-libgdx](https://stackoverflow.com/questions/17347883/why-game-is-running-slow-in-libgdx)  
47. How I optimized my LibGDX Game \- Reddit, accessed on March 13, 2026, [https://www.reddit.com/r/libgdx/comments/c0bv7f/how\_i\_optimized\_my\_libgdx\_game/](https://www.reddit.com/r/libgdx/comments/c0bv7f/how_i_optimized_my_libgdx_game/)

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGUAAAAXCAYAAAASloEFAAAD2klEQVR4Xu2YR8gVSRSFrzmLWUGRwYSiC8NixIAbF25cKIwuFDeCAXUhrg2grgwo6oAw6oAiCGLAhYxhMTMMo7MyC+rAL+oYMec03mNVvXffebf+pP5u+oPD63tudb+qruqq6hYpKCgoKCj4+kxQPVD9rzqlal6ZLrFc9VT1UjWXch7TJVzTY4eE3BvVbMo1Jacl1OM/1STKJQZJudwJyuXItXuy6q2E/FHKldii+tnEuOE4YYDxwCXVcRNfUP1lYg9cx6vcLam8/gEJ12tKWqvemXi9hLpym9BRtg0jKfa4Jn6ZKapfTDxY/HKfzR8dzxbuTHECXhc2I3ek+jpgtGofeYDLfWueqzaSd1dCPbobD/FCEwOMdMwoHph1rovfHs/br9pujQ7i3zj2zlCcgGd7PjFVtVSqrwM2qV6TB7icB6bVrmwS89jIkOrW03iLopcGTa8Y49dyLPoe71WLxc/D40F8UPUrebJaNZ48vpkcJ3J+uuleflz0Pqg6RW+W6mGpRO1ghPdmM4JpEfN/fcATu468VRLqtiHGK2LM7BLfv6xqK7V3CoTOt169QMGPFHsne36NqkU89vLgqpRzv8e4IWDd60seFuqh5DUUDAzUqU2MD8WY2SbV/igJswDIdcpEKbf7VfztWFEiwzkJhdsbL3dz2cefrjQx5y0vpJyHulWm68R2DDpkmMk1hv4S6mE3PX9Ej9kswbcDww7iXKeAJVLZ7jWV6Wqw4KOgnWdB7uayb3czgPMJeMNVzVRPYuyVqwt0DDpkBCcaAf5/J3l7o89sleC3jPHfUp6KQa5TjqhOxmM8Vandc0olCCygKJAeXUvupln/vIT51OKdV6NaS958CeUwLTQETDePJYzyL+GZahmbkl9T0jsWGCjVC7XXKUMcr1302P8MdjWc2G2OUWnOA3hY3MCfjtIf4hgVBd51AEYQ5tn6gg5Jawg6pp/JNYQrEnaLltR2bIBQ39p2XzOkut33Yj7F4KJU3tNE6pgq7HyYwO4ogT/2ToQ3hk2DNwoQ9yAP7BF/e+1hOySBjuHFvy4wnYwl76eoBOqLLxMW7ABr2y16095hyb8gc9nSK78nC+IFJsZ2kssw3nWwkGIvb8EOhMvlwOcgTBkej1R92MyAT0bc3iSM3sRvUllfrIMo84PxGHz5QBn7uQrrDzx+ov9VzbQGRhZXKImnkvSY/aM6K+FdBBX0wPes26obUXhTTnt/gGNc6378xbVamXwOfBrhRjGr2MjA7bVi0F7sFvH2jTy+X3lMk9CmmxLajU0IzkvY+52mOJxTUFBQUFBQUFDwXfgERuVRRIawxqcAAAAASUVORK5CYII=>
# K-ToME 客户端 UI/UX 优化设计文档（参考驱动，非竞品复刻）

## 1. 文档定位

本文是 `docs/opt` 下的客户端体验设计文档，目标不是总结某个外部站点“做得像不像”，而是把一次参考调研和一份 review 报告，收敛成 **K-ToME 自身后续可执行的 UI/UX 改造设计**。

它负责：

1. 记录本次参考调研与 review 报告中，哪些反馈经仓库真源核实后成立。
2. 明确哪些外部做法对 K-ToME 有帮助，哪些不应照搬。
3. 以 `client` 当前实现和 `Phase 2 / Phase 3` 合同为前提，定义后续体验优化方向。
4. 给出文件落点、分阶段落地建议、风险边界和验证矩阵。
5. 执行级 PR 拆分、退出条件、验证与风险，统一以下游 plan 文档为准：
   - [2026-04-21-client-ui-ux-optimization-development-plan.md](./2026-04-21-client-ui-ux-optimization-development-plan.md)

它不负责：

1. 不覆盖 [2026-03-13-phase2-to-phase5-final-roadmap.md](../2026-03-13-phase2-to-phase5-final-roadmap.md) 的 phase 编号、出口标准和执行顺序。
2. 不新增第二份 `RenderSnapshot`、`VisualManifest`、`iconKey`、`LogTokenEvent`、`telegraph` 权威。
3. 不把单一外部参考对象升级成模板。
4. 不直接写运行时代码或渲染实现细节。

## 2. 权威约束映射

本设计必须服从以下已冻结路线与合同：

1. `P2-W3`
   - 正式文本和日志必须走 i18n key / log token，不能回到裸字符串路径。
2. `P2-W4`
   - `RenderSnapshot`、`VisualManifest`、`AudioManifest` 是表现层唯一正式输入。
3. `P2-W5`
   - 最小 TileRenderer、最小 HUD / 背包 / 检视、golden screenshot 基线必须成立。
4. `P2-W6 ~ P2-W7`
   - Tile 模式短局要能完整支撑职业、怪物、物品和 locale 覆盖。
5. `P3-W2`
   - 状态 / 持续 / UI 状态语义扩展必须可自证。
6. `P3-W3`
   - 动态说明、关键词、成长预览要能被 `client` 稳定消费。
7. `P3-W4`
   - 高伤技能预警、Boss telegraph、脚本化 AI 的可见性必须强化，而不是藏在日志角落。

对应路线图里的稳定字段与输入面：

1. `iconKey` 是正式 UI 图标字段，不是可选附加项。
2. `logProfile` 是日志/tooltip/战斗词条映射入口，不应只停在 schema 层。
3. `client` 只能消费规则层给出的 typed facts，不能在表现层重算一套“真实战斗结果”。

## 3. 调研输入与 review 核实结论

### 3.1 处理原则

本次文档改造遵循：

1. 先读 review，再核实，不盲从。
2. 只吸收那些被当前仓库真源、阶段合同和用户目标共同支持的反馈。
3. 对不符合 K-ToME 介质、输入模式和架构边界的建议，明确降级或剔除。

### 3.2 参考输入

本次输入包括：

1. 外部参考站点：[深渊纪事](https://abyss-chronicle.top/)
2. review 报告：[2026-04-20-client-ui-ux-reference-driven-optimization-design-review.md](/Users/luo/Documents/github/K-ToME/docs/review/2026-04-20-client-ui-ux-reference-driven-optimization-design-review.md)
3. 当前设计文档旧版
4. 当前客户端真源：
   - [FoundationGameScreen.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt)
   - [TileRenderer.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt)
   - [InputHandler.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/input/InputHandler.kt)
   - [MainMenuScreen.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt)
   - [MainMenuController.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt)
   - [StatusHudRenderer.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/ui/status/StatusHudRenderer.kt)
   - [StatusIconResolver.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt)
   - [DesktopLauncher.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt)

### 3.3 已核实、确实有帮助的反馈

以下反馈经核实后成立，并应吸收进本文：

1. **原文档过度锚定单一竞品**
   - 上一版文档把“深渊纪事”的若干表现形式写得过于像模板，容易给实施者错误信号。
   - 这和用户目标不一致。用户要的是“适合 K-ToME 的更好 UI/UX”，不是照抄某个 Web 游戏。
2. **原文档过度强调固定三栏**
   - 当前 [TileRenderer.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt) 的布局仍是“地图 + sidebar + 底部 cards”的结构，底部 cards 里才拆 `info / log / focus`。
   - 因此“固定三栏”不应作为文档里的预设结果，而应改为“稳定的信息面”设计目标。
3. **原文档缺少 K-ToME 自身介质优势**
   - K-ToME 是桌面、libGDX、键盘优先、Tile 地图、回合制驱动。
   - 这意味着它可以做出比 Web 参考更好的空间化 HUD、键盘提示、回合节拍和 modal panel。
4. **原文档缺少细粒度视觉/交互规范**
   - 当前 [TileRenderer.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt) 里已有大量硬编码颜色和布局常量。
   - 如果文档不定义 client-local 的 design token / spacing / motion / focus 基线，后续实现很容易“结构对、观感错”。
5. **原文档缺少对“哪些不能做”的明确边界**
   - 例如：不能为了对称展示而引入规则层尚不存在的玩家肢体系统。
   - 不能把 design token 反向推入 `RenderSnapshot` 或 manifest。
   - 不能把外部产品的社交、网页交互路径当作桌面路径强搬。

### 3.4 明确不吸收的反馈

以下内容不直接吸收，或只保留原则不保留原建议形式：

1. **把三栏作为唯一正式布局**
   - 不采纳。
   - K-ToME 应定义“信息面稳定存在”，但布局可根据视口和 `UiMode` 自适应为：
     - map-dominant + side panel + bottom context
     - wide-screen split layout
     - modal / drawer overlay
2. **把深渊纪事当主模板继续深挖**
   - 不采纳。
   - 外部参考只能作为样本之一，不能盖过 ToME4、Caves of Qud、Jupiter Hell 这类更接近桌面 roguelike 语境的参照系。
3. **在文档里默认新增玩家肢体伤害 UI 体系**
   - 不采纳。
   - 若规则层没有该 contract，`client` 不得自造。
4. **把渲染风格切换（Classic / Tile / Simplified）列为当前阶段主目标**
   - 不采纳。
   - 这会把 golden、a11y、视觉验收面成倍放大，当前阶段不值得开口。

### 3.5 参考方法修订

后续 UI 文档和实现不再用“某个竞品怎么做”驱动，而改为：

1. 先定义 K-ToME 的目标体验问题。
2. 再用多个参考对象提取局部方法：
   - ToME4：tooltip、关键词、动态说明结构
   - Caves of Qud / Jupiter Hell：桌面 roguelike 的布局和信息密度
   - Slay the Spire：意图/预警的决策可见性
   - Darkest Dungeon：角色卡与状态排布
   - Hades II：图标 + 计数 + 节奏感的状态表达
   - PoE2：高级 tooltip 与装备对比
3. 最后再约束到 K-ToME 的输入方式、Tile 地图和 phase 合同。

### 3.6 行业参考吸收结论

这轮新增的行业参考，不是为了把文档再变成“列举名作”，而是为了更准确地回答：**K-ToME 应该向哪些成熟 roguelike 学方法，且分别解决什么问题。**

| 参考对象 | 可吸收的核心点 | 对 K-ToME 的帮助 | 当前定位 |
| --- | --- | --- | --- |
| ToME4 | 机制透明、关键词高亮、动态说明、成长预览 | 直接支撑 `P3-W3` 的 tooltip / 关键词 / 构筑说明，是最贴近项目精神前身的主参考 | 主参考 |
| Caves of Qud | `Look Mode`、键盘优先检视、高密度桌面信息编排 | 适合把当前 `Inspect` 从单点检视提升成正式地图检视路径 | 当前阶段可吸收 |
| Jupiter Hell | 现代桌面 roguelike 的布局比例、日志与状态密度控制 | 帮助文档从“网页栏位想象”回到桌面 roguelike 语境 | 当前阶段可吸收 |
| Slay the Spire | 常态化意图/预警、完美信息式决策 | 帮助 K-ToME 把 telegraph 从 Boss 特例扩成可理解的常态战斗语言 | `P3-W4` 主参考 |
| Into the Breach | 敌方行动直接绘制在地图上，空间即 UI | 强化 K-ToME 的 Tile 地图优势，指导 map telegraph 与 diegetic HUD | `P3-W4` 主参考 |
| DCSS | 反 grind、autoexplore、死亡回放 | 帮助识别哪些交互只是重复劳动而非挑战 | 长期高价值候选 |
| PoE2 | Advanced tooltip、装备对比框、双密度信息展示 | 适合背包/检视后期增强，但不应前置成当前阶段阻塞项 | 中长期候选 |
| Hades II / Balatro | 主题色纪律、微动效节奏、反馈层级 | 适合 design token 与 Phase 5 打磨，不适合当前阶段过早美术化 | 打磨参考 |

这些行业参考最终共同支持四条稳定原则：

1. **机制透明**
   - 玩家失败应主要来自决策失误，而不是信息缺失。
2. **空间即 UI**
   - 能直接画在地图上的风险和意图，不必先塞进面板。
3. **反疲劳**
   - 重复输入不应伪装成挑战。
4. **微交互克制打磨**
   - 反馈要准，不要吵。

### 3.7 当前阶段不直接吸收的行业做法

结合这些行业参考后，以下方向仍然只作为长期储备，不进入当前文档主线：

1. **渲染风格实时切换**
   - 尽管 Dwarf Fortress Steam 版是成熟案例，但对 K-ToME 当前阶段会显著放大 golden、a11y 和表现维护成本。
2. **死亡即元进度**
   - Hades 系的元叙事/元成长不是当前项目主线。
3. **超大规模 passive tree 或完整 build planner**
   - PoE2 / ToME4 的某些规模只适合在内容量足够后参考，当前不提前设计。
4. **强叙事权重的主界面呈现**
   - 叙事是增色层，不应压过机制透明与地图主体验。

## 4. 当前 K-ToME 客户端现状

### 4.1 已有基础

当前仓库已经有可继续收敛的正式基础：

1. [FoundationGameScreen.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt)
   - 正式渲染入口已稳定在 `FoundationGameScreen -> TileRenderer`。
   - `client` 继续消费 `RenderSnapshot`，没有直接持有规则权威状态。
2. [TileRenderer.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt)
   - 已存在地图区、sidebar 区、底部 info/log/focus cards。
   - 已支持 `MAP / SHOP / INVENTORY / INSPECT / TARGETING / VALIDATION` 等 `UiMode`。
   - 当前布局常量表明它不是“正式三栏”，而是 map + sidebar + bottom cards：
     - `sidebarWidth` 约束在 `340f..420f`
     - `infoWidth / logWidth / focusWidth` 仍是底部 card 的横向拆分
3. [InputHandler.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/input/InputHandler.kt)
   - 已有 overlay mode 状态机，说明背包 / 检视 / targeting 不是临时调试态。
4. [StatusIconResolver.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt)
   - 状态图标已消费 `effect.iconKey`，说明 icon pipeline 已有骨架。
5. `game/src/main/resources/i18n/*.json`
   - 已存在 `ui.sidebar.*`、`ui.inspect.*`、`log.inventory.*`、`log.warning.telegraph` 等键位。
6. `ClientSmokeHarnessTest`
   - 已覆盖 inventory / inspect / boss telegraph / HUD token 等 smoke 面。

### 4.2 当前主要缺口

相对 K-ToME 自身的目标体验，当前仍有以下缺口：

1. **首页仍偏开发态**
   - [MainMenuScreen.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt) 和 [MainMenuController.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt) 已有职业/种族与基本入口，但缺少：
     - 首屏快速开始
     - 当前 build 能力速览
     - 新老玩家差异化首屏
     - 常驻键位与 locale 可见性
2. **信息面仍未正式化**
   - 当前 UI 更接近“地图 + 辅助面板”，而不是明确的“世界 / 上下文 / 角色动作 / modal”信息面架构。
3. **日志表达仍偏线性**
   - 现有日志 tone 已能按 messageKey 着色，但还没有形成：
     - 危险 / 奖励 / 状态 / 战斗解释 / 探索提示的稳定层级
     - 房间锚点
     - 聚合/折叠
     - 叙事块与机制块边界
4. **战斗决策仍偏输入状态**
   - 当前 `UiMode.TARGETING` 更像输入模式，而不是完整的“动作 -> 方式 -> 目标 -> 解释”决策结构。
5. **背包 / 商店 / 检视缺少统一语言**
   - 这些面板还没有统一的卡片结构、品质语义、差值比较与禁用原因表达。
6. **缺 client-local design token**
   - 当前颜色、间距、布局常量分散在 renderer 内。
   - 如果不先收口 token，后续 UI 优化很容易继续散乱。
7. **K-ToME 自身优势还没被放大**
   - Tile 地图
   - 键盘优先
   - 回合节拍
   - 桌面 modal / overlay
   这些都还没成为设计中心。

## 5. 设计目标

本轮 UI/UX 优化的目标不是“更像参考网站”，而是：

1. **让 K-ToME 的规则深度更容易被看懂**
   - 玩家能更快理解当前局面、风险和选项。
2. **让正式 contract 真正进入体验层**
   - `iconKey / visualKey / logProfile / telegraph / dynamic description` 不再停在 schema 或局部面板。
3. **让局内体验服务于桌面 roguelike，而不是 Web 阅读流**
   - 地图与空间感仍是主角。
   - 日志、按钮、卡片只负责放大规则理解，不反客为主。
4. **让 K-ToME 的输入方式更友好**
   - 键盘路径、热键、聚焦、禁用原因、inspect 路径都要显性化。
5. **让 phase 2 / 3 的视觉演进可连续收敛**
   - `P2-W5` 先立基础信息面和 design token。
   - `P3-W2 ~ P3-W4` 再强化状态、动态说明和 telegraph。

### 5.1 主线设计哲学选型

结合本次行业参考，K-ToME 的主线设计哲学应固定为：

1. **机制透明派为主**
   - 以 ToME4 / StS / Rift Wizard 的“规则可读、结果可解释”为核心。
2. **空间即 UI 为辅**
   - 以 Into the Breach / Jupiter Hell 的地图可见性和 Tile 空间表达放大 K-ToME 自身优势。
3. **反疲劳为底线**
   - 用 DCSS 的思路约束重复劳动，不把 grind 误当挑战。
4. **微交互派为后期打磨层**
   - Hades II / Balatro 的时间曲线、反馈节奏用于 Phase 5 打磨，不前置成本。

明确不选为当前主线的，是：

1. 叙事融合派
2. 纯美术风格驱动派
3. 单一竞品模板复刻

## 6. 设计原则

### 6.1 单一权威

所有 UI 表现都必须从既有正式输入派生：

1. 地图 / actor / item / overlay 来自 `RenderSnapshot`。
2. 图标来自 `iconKey -> VisualManifest`。
3. 文本来自 `nameKey / descKey / log token / tooltip model`。
4. 战斗解释来自规则层或 snapshot 已暴露的 typed facts。

不允许：

1. 在 `client` 再维护一套 action effectiveness 规则表。
2. 在 `client` 再造一套商店、物品、状态平行说明真源。
3. 通过拼字符串替代正式日志 / 说明 contract。

### 6.2 面稳定，不预设唯一栏位形态

K-ToME 需要稳定的是**信息面职责**，不是必须永远固定三栏。

局内必须长期存在的四类信息面：

1. **世界面**
   - 地图、路径、空间 telegraph、交互地块
2. **上下文面**
   - 日志、历史、危险提示、侦察结果
3. **角色/目标/动作面**
   - 玩家状态、目标状态、当前可执行动作
4. **modal / drawer 面**
   - 背包、检视、商店、事件详情、构筑详情

表现形式可按场景变化：

1. 宽屏下可 split layout。
2. 标准视口下可 map-dominant + side panel + bottom context。
3. 背包 / 商店 / 检视可用 modal / drawer，不必硬塞 sidebar。

### 6.3 图标优先，但必须可回退

优先使用 icon + 短标签提高扫描速度，但必须满足：

1. 没有图标时仍能回退为可理解文本。
2. 图标不承担独占语义，关键风险仍需有文字说明。
3. locale 切换下不能退化成“只剩图标猜意思”。

### 6.4 逐层展开，而不是一次塞完

战斗、事件、商店、检视等复杂交互统一采用：

1. 第一层：当前动作或当前对象
2. 第二层：方式 / 消耗 / 风险 / 适用条件
3. 第三层：目标 / 部位 / 范围 / 预期结果

### 6.5 键盘优先

K-ToME 是键盘优先客户端，因此：

1. 热键应可见，不应只存在于底部帮助文本。
2. 焦点必须可见，hover 不能是唯一提示路径。
3. modal / drawer 打开后，输入焦点必须锁定，不与地图态混用。
4. `ESC / Backspace / F / Tab / ?` 等高频键位要有统一语义。

### 6.6 地图与空间感优先

K-ToME 不是纯文本阅读流，Tile 地图是主体验之一。

因此：

1. 空间 telegraph 应优先落在地图面。
2. 交互地块可用轻量地图内提示，不必所有东西都塞进 sidebar。
3. 中心区域不应被纯文本面板长期占满。

### 6.7 动效克制，并以回合为节拍

动效必须服务于回合节奏和规则理解，而不是持续发光。

允许：

1. 回合推进时的轻量节拍提示
2. 伤害/治疗飞字
3. 状态新增/消失的短动画
4. 高风险 telegraph 倒数脉冲

不允许：

1. 常驻背景流光
2. 无信息价值的持续呼吸边框
3. 高频 idle animation 吃掉帧预算

### 6.8 design token 只属于 client

颜色、间距、字号、动效、圆角等设计 token 应集中管理，但：

1. 它们只属于 `client` 表现层。
2. 不应进入 `RenderSnapshot`。
3. 不应成为 manifest / content schema 的新规则依赖。

### 6.9 不把单一参考对象升级成模板

外部参考只提供局部启发：

1. 深渊纪事：解释型按钮、事件房节奏、图标覆盖
2. ToME4：tooltip 与关键词结构
3. Jupiter Hell / Caves of Qud：桌面 roguelike 布局与密度
4. Slay the Spire：预警 / 决策可见性
5. Darkest Dungeon / Hades II：状态与角色卡层级

## 7. 目标体验蓝图

### 7.1 首页 / 开局页

首页要优先回答四件事：

1. 我能不能立刻开一局？
2. 当前 build 的正式玩法深度到哪一层？
3. 这个职业 / 种族 / 配置有什么玩法差异？
4. 如果我是第一次进局，最安全的入口是什么？

目标结构：

1. **首焦点**
   - 首次进入优先落在 `快速开始` 或 `继续游戏` 的单个明确按钮，而不是先进入职业轮播。
2. **角色创建区**
   - 保留职业 / 种族选择，但要突出玩法标签、资源类型和当前可玩状态。
3. **build 能力速览**
   - 不是“内容清单”，而是“已可玩 vs 总量”的当前 build 摘要。
4. **常驻帮助区**
   - locale 切换、关键键位、短局建议必须显性可见。
5. **新老玩家差异化**
   - 首启更偏 onboarding；
   - 有存档时更偏继续、最近局面与当前角色上下文。

### 7.2 局内信息面架构

不再把“左/中/右三栏”写成唯一解，而改成以下信息面：

#### A. 世界面

承载：

1. 地图
2. 路径 / 焦点
3. 空间 telegraph
4. 可交互地块提示

要求：

1. 地图仍是主体验面，不降级成背景图。
2. 关键空间风险优先落在地图，而不是先落到日志里。
3. 地面掉落不能只在 sidebar / inspect 中可见。
4. 有掉落物的格子应显示轻量拾取提示图标或徽标。
5. 同一格存在多件掉落时，地图提示应带数量 badge。
   - 当前 phase 至少显示“同格掉落件数”。
   - 若未来引入正式 item stack contract，再把 badge 切到真实 stack quantity。

#### B. 上下文面

承载：

1. 最近日志
2. 房间锚点 / 世界锚点
3. 危险提示
4. 侦察结果
5. 必要的叙事块

要求：

1. 机制日志与叙事块必须分层。
2. 日志要支持聚合、折叠和回溯锁。
3. 危险消息与普通消息在视觉上必须有权重差。

#### C. 角色/目标/动作面

承载：

1. 玩家卡
2. 目标卡
3. 当前动作卡

要求：

1. 玩家状态、目标状态、动作解释要形成稳定阅读顺序。
2. 状态和 telegraph 不能只堆在一行文字里。
3. 动作按钮必须能表达：
   - 作用
   - 消耗
   - 风险
   - 禁用原因

#### D. modal / drawer 面

优先用于：

1. 背包
2. 商店
3. 检视
4. 事件详情
5. 构筑/天赋说明

要求：

1. 这些内容不强行塞进窄 sidebar。
2. 打开后输入焦点锁定。
3. 关闭后能回到原地图上下文。

### 7.3 K-ToME 特有机会

相对 Web 参考，K-ToME 有几条更适合自身的方向：

1. **空间即 UI**
   - telegraph、路径、交互点优先做地图内提示。
   - 这条更接近 Into the Breach / Jupiter Hell，而不是深渊纪事。
2. **回合即节拍**
   - 动效和提示可以跟回合推进对齐，而不是持续流动。
3. **按键即 affordance**
   - 当前可执行动作直接显示热键 badge，而不是把所有键位都挤到窗口标题栏。
   - 这条更接近 Caves of Qud / Dwarf Fortress Steam 的桌面输入哲学。
4. **modal 更适合背包/商店/检视**
   - 桌面与 GPU 渲染比 Web 更适合做居中 panel / drawer。
5. **本地快照与 seed 能成为分享维度**
   - 不是网页分享按钮，而是截图、seed、局面回放。
6. **Inspect 应升级为正式 Look Mode**
   - 当前 `Inspect` 不应只停在“当前格读信息”。
   - 应逐步支持地图自由光标检视，并让地图、目标卡、检视面同步更新当前光标下的单位、地块、物品、telegraph 与关键状态。
   - 这样能把 CoQ 式的检视能力转成 K-ToME 自己的桌面 Tile 优势。

### 7.4 事件房 / 商店 / 奖励房统一卡片模型

这些交互统一采用：

1. 标题
2. 一句话摘要
3. 风险 / 收益 / 条件
4. 2~4 个动作按钮
5. 每个按钮可选：
   - 图标
   - 资源代价
   - 结果倾向
   - 禁用原因

这样可复用到：

1. 短局事件房
2. 商店买卖
3. 祭坛/特殊地块
4. Phase 4 隐藏事件与探索奖励层

### 7.5 战斗决策结构

战斗统一走三层：

1. **动作层**
   - 攻击 / 防御 / 天赋 / 物品 / 位移 / 观察
2. **方式层**
   - 攻击方式、资源消耗、命中倾向、风险修正、已知克制
3. **目标层**
   - 目标、部位、范围、预计结果

战斗面必须回答：

1. 当前能做什么？
2. 为什么这个动作好/不好？
3. 为什么这个动作能点/不能点？
4. 如果继续下去，风险来自哪？

允许展示：

1. typed reason
2. 风险标签
3. 高/中/低 命中与收益
4. 区间式预期结果

不允许：

1. `client` 私算一套与规则层不一致的最终伤害。

补充行业参考导向：

1. 普通敌人的下一步意图，也应逐步从“隐藏信息”过渡为“可见信息”。
2. Boss telegraph 应兼具：
   - StS 式的意图明确性
   - FFXIV 式的倒数/施放条感知
   - Into the Breach 式的地图空间表达

### 7.6 背包 / 商店 / 检视统一展示语言

三者共享：

1. 图标
2. 名称
3. 品质语义
4. 核心效果摘要
5. 当前动作
6. 禁用原因
7. 可选的对比信息

检视页优先回答：

1. 它是什么？
2. 为什么值得看？
3. 跟我当前 build 的关系是什么？

装备展示补充约束：

1. 所有可装备物必须有可解析的 `iconKey`，缺 icon 视为内容未完成，不作为正式可验收体验。
2. 当前正式品质 contract 以 `NORMAL / MAGIC / RARE` 为基础，另有 `UNIQUE / ARTIFACT` 作为 special tier。
3. `client` 必须按名称色、边角标或列表强调稳定区分这些档位。
4. 若未来要支持 `SET / LEGENDARY / MYTHIC` 等额外档位，必须先扩 `core/game` 合同，再扩 UI；不允许在 `client` 先自造品质梯度。
5. 品质色必须通过 client-local token 固化，且只作用于名称、边角标和对比摘要，不污染整行细节文案。

补充行业参考导向：

1. 基础信息优先一屏可扫读。
2. 高密度详情可采用 advanced mode 或二级面板，而不是默认全部展开。
3. 装备对比、关键词子 tooltip 属于可选增强，不作为当前 phase 阻塞项。

### 7.7 状态与 telegraph

状态与 telegraph 必须做到：

1. 一眼区分 `BUFF / DEBUFF / 区域类 / 高风险 telegraph`
2. badge 优先显示最有价值的剩余信息：
   - 回合
   - 层数
   - stack/cap
3. 同一 telegraph 在地图、目标卡、日志前缀上保持同 icon / 同主色 / 同语义
4. Boss telegraph 的权重明显高于普通状态

补充行业参考导向：

1. 状态表达更接近 Hades II 的图标 + 层数 + 分类纪律。
2. 普通意图表达更接近 StS 的清晰度。
3. 空间风险表达更接近 Into the Breach 的地图直接可见性。

### 7.8 细粒度视觉与微交互规范

本轮文档补入最小但明确的细粒度规范：

1. **数字**
   - 资源和属性数字优先等宽显示，避免跳动。
2. **品质色**
   - 品质色只承担名称/边角标记，不污染整行文本。
3. **聚焦**
   - 所有可聚焦元素必须有稳定的焦点戒指，不依赖 hover。
4. **禁用态**
   - 禁用按钮不应消失，应保留并显示禁用原因。
5. **战斗节拍词**
   - 例如 `暴击 / 闪避 / 破甲 / 预警` 这类关键短词要有独立强调层级。
6. **伤害/治疗反馈**
   - 用短时飞字或近焦点反馈，而不是只写进日志。
7. **日志块边界**
   - 叙事块与机制块必须视觉分离。
8. **高对比度与色盲回退**
   - 不能只靠颜色传达危险/奖励语义。

这些规范吸收的是行业经验，而不是行业美术：

1. Balatro 的时间曲线纪律
2. Hades II 的状态反馈节奏
3. Cogmind 的颜色语义严格性

## 8. 契约与组件改造建议

### 8.1 client-local design token 层

建议新增一个只属于 `client` 的 design token 层，承载：

1. 颜色语义
2. 间距
3. 字号 / 字重
4. 圆角 / 描边
5. 动效时长与节奏

文件载体可以是：

1. `client/src/main/kotlin/com/ktome/client/ui/UiDesignTokens.kt`
2. 或 `client/src/main/resources/ui/design-tokens.json`

载体形式可后续决定，但边界必须固定：

1. 它属于 `client`。
2. 它不是规则 contract。
3. 不进入 `RenderSnapshot` 和 manifest。

### 8.2 信息面 view model

建议把当前 renderer 继续拆成更稳定的展示模型：

1. `LogPresentationModel`
   - 分类 / 锚点 / 聚合 / 折叠 / 叙事块
2. `PlayerCardModel`
   - 资源、核心状态、装备摘要、关键 build 标签
3. `TargetCardModel`
   - 名称、标签、状态、部位/重点风险
4. `ActionPanelModel`
   - 动作、方式、目标、禁用原因
5. `ModalPanelModel`
   - 背包 / 商店 / 检视 / 事件详情

### 8.3 `iconKey` 消费矩阵

建议把 `iconKey` 扩展为以下消费面：

| 语义对象 | 当前状态 | 建议目标 |
| --- | --- | --- |
| 状态效果 | 已有基础 | 强化 badge、分类和 tooltip |
| 物品 | 部分存在 | 背包 / 商店 / 掉落 / 检视统一消费 |
| 天赋/技能 | 有 schema 字段 | 进入 hotbar、targeting、动态说明 |
| 事件/交互物 | 有 visual 资源基础 | 进入事件卡、地块提示、交互按钮 |
| 怪物/职业/种族 | 内容资源已存在 | 首页、目标卡、图鉴统一消费 |
| 日志前缀 | 基本缺失 | 对危险、奖励、状态变化增加轻量 icon 前缀 |

补充落地约束：

1. 所有可装备物与 special template item 都必须能解析到正式 `iconKey`。
2. `MapCellSnapshot.items` 的存在应直接驱动地图内掉落提示，而不是只在 sidebar / inspect 里列出文字。
3. 当前 `RenderSnapshot / InventoryEntrySnapshot / ItemRenderSnapshot` 还没有正式 item stack quantity 字段。
   - 当前阶段先做“同格多件掉落可见 + 数量 badge”。
   - 若未来要支持真正的装备堆叠或背包 stack，需要先扩上游 item contract。

### 8.4 解释型按钮基座

统一按钮模型至少支持：

1. 图标
2. 标题
3. 副标题
4. 消耗/收益摘要
5. 风险标签
6. 禁用原因
7. 热键 badge

优先应用于：

1. 战斗动作
2. 事件选项
3. 商店买卖
4. 背包动作

## 9. 分阶段落地建议

### 9.1 `P2-W5.0`：client-local token 与信息面骨架前置

新增一个前置收口层：

1. 建立 client-local design token 层
2. 把当前“地图 + sidebar + bottom cards”收口为更明确的信息面骨架
3. 去掉开发态窗口标题里的操作说明依赖
4. 重拍最小 HUD / 空态 / locale golden

这一步不是要求一次改成“三栏成品”，而是给后续 UI 组件统一地基。

### 9.2 `P2-W5a`：首页首屏优化

聚焦：

1. 快速开始与继续入口
2. 新老玩家差异化首屏
3. 构筑差异的可见性
4. locale 与键位的显性入口

### 9.3 `P2-W5b`：局内信息面稳定化

聚焦：

1. 日志模型拆出
2. 玩家卡 / 目标卡 / 动作卡分层
3. map-dominant 布局与 modal / drawer 路径
4. 统一键盘焦点与退出语义

### 9.4 `P2-W5c`：`iconKey` 首轮消费

聚焦：

1. 状态图标徽章化
2. 所有可装备物的 `iconKey` 完整覆盖，并进入装备槽 / 背包 / 检视 / recent reward / 地面掉落态
3. 品质色 token 化，并按当前 `NORMAL / MAGIC / RARE + UNIQUE / ARTIFACT` 正式展示
4. 地图掉落提示进入 world surface；同格多件掉落显示数量 badge

### 9.5 `P2-W6 ~ P2-W7`：内容扩张时保持体验一致

要求：

1. 新增怪物、物品、房间时同步补齐图标和检视摘要。
2. locale 扩张时同步补截图和可读性验收。
3. 不允许随着内容膨胀退化成纯文字列表。

### 9.6 `P3-W2`：状态语义可见化

聚焦：

1. 状态分类
2. badge 规则
3. 高风险状态层级
4. 区域效果和持续效果的一致表达

### 9.7 `P3-W3`：动态说明与关键词消费收口

聚焦：

1. 天赋 / 装备 / 词缀 / 地块说明统一进入解释型检视
2. 关键词不只存在 tooltip，也要进入主要阅读路径
3. 动态说明与图标、资源、构筑标签共同显示

### 9.8 `P3-W4`：telegraph 与战斗二级决策面板

聚焦：

1. 动作 -> 方式 -> 目标 的战斗流
2. telegraph 的地图/目标卡/日志一致性
3. 观察 / 侦察结果反向影响按钮解释
4. 高风险行为的明显可见性

### 9.9 长期高价值候选（非当前阶段承诺）

这些内容来自行业参考，确实有帮助，但不作为当前阶段必须交付项：

1. `Look Mode` 的进一步增强
   - 例如更完整的地图自由检视和多层 tooltip。
2. `Autoexplore / Autotravel`
   - ROI 很高，但需要单独评估是否与当前短局 pacing 一起规划。
3. `Advanced Tooltip`
   - 适合在装备、天赋、词缀密度上来后补。
4. `Death Replay / Turn Log`
   - 对 post-mortem 学习非常有价值，但不是当前 UI 基础骨架前置。
5. `Seed / Snapshot Share`
   - 适合长期社区化与调试支持。

## 10. 建议文件落点

### 10.1 首页与角色创建

1. [MainMenuScreen.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt)
2. [MainMenuController.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt)
3. `client/src/main/kotlin/com/ktome/client/ui/creation/PlayerCreationPanel.kt`

### 10.2 局内布局与渲染

1. [FoundationGameScreen.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt)
2. [TileRenderer.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt)
3. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
4. [InputHandler.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/input/InputHandler.kt)

### 10.3 状态 / telegraph / 说明

1. [StatusHudRenderer.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/ui/status/StatusHudRenderer.kt)
2. [StatusIconResolver.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt)
3. `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt`
4. `client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt`

### 10.4 contract 与资源

1. `core` / `game` 中与 `iconKey / visualKey / logProfile` 相关的 schema 与 snapshot
2. `game/src/main/resources/i18n/*.json`
3. `client/src/main/resources/manifests/visual-manifest.json`
4. 新增 client-local token 文件（格式待定）

## 11. 验证与验收矩阵

### 11.1 自动化验证

后续实现时建议至少补齐：

1. `ClientSmokeHarnessTest`
   - 首页
   - 地图态
   - 地面掉落提示态
   - 背包态
   - 检视态
   - 事件态
   - 战斗态
   - telegraph 同屏态
2. golden screenshot
   - `zh-CN / en-US`
   - 首页
   - map-dominant HUD
   - 地图掉落图标 / 多件掉落数量 badge
   - inventory
   - inspect
   - 战斗动作面板
   - telegraph overlay
3. `locale-lint`
   - 副标题、禁用原因、风险说明不得裸字符串回退
4. `contract-lint`
   - 新增 icon / visual key 必须在 manifest 中可解析
   - 可装备物与 special template item 不允许缺 `iconKey`

### 11.2 白盒验收

每次大 UI 变更至少人工验收：

1. 新玩家 10 秒内能否知道如何开始一局。
2. 进入房间后，是否一眼能分辨“地图 / 当前焦点 / 我能做什么 / 最近发生了什么”。
3. 战斗里是否能快速看懂当前风险、目标状态和推荐动作。
4. 背包 / 检视 / 商店是否共享同一套阅读习惯。
5. 高风险 telegraph 是否足够醒目，而不是埋在普通状态里。
6. 英文界面是否仍保持层级和可读性。
7. 键盘焦点是否始终可见，关闭 modal 后是否回到原上下文。

### 11.3 可量化的最小体验指标

本文只保留与当前阶段相容、且不至于伪精确的指标：

1. `zh-CN / en-US` 双语下，首页、背包、检视、战斗动作面板不得出现裸字符串或明显爆版。
2. 战斗持续 20 回合的样本里，日志可见区不得因为重复低价值消息完全淹没危险提示。
3. 高风险 telegraph 在地图和目标卡上必须同时可见。
4. 顶层窗口标题不再承担主要操作说明职责。

## 12. 风险与非目标

### 12.1 高风险点

1. 为了做解释型战斗而在 `client` 复制规则推导。
2. 为了补视觉层次而破坏 `RenderSnapshot` / manifest 单一权威。
3. 为了模仿外部站点，把地图退化成背景板。
4. 图标覆盖不完整，导致一半图标一半纯文本，形成新的阅读噪音。
5. 把 design token 变成规则 contract。
6. 为了对称展示而引入规则层尚不存在的新玩法语义。
7. 为了动效效果牺牲帧预算与可读性。
8. 在 `client` 先发明 `SET / LEGENDARY` 等未冻结品质档位，或伪造 item stack quantity 语义。

### 12.2 明确非目标

1. 不追求“完整复刻深渊纪事”。
2. 不把“固定三栏”当作唯一成品形态。
3. 不在当前阶段引入正式渲染风格切换。
4. 不把 AI 叙事提升为主界面第一权重。
5. 不把网页社交分享路径直接搬到桌面客户端主界面。
6. 不把行业参考里的叙事融合、元成长或超大规模构筑系统误当作当前阶段 UI 阻塞项。

### 12.3 明确禁止

1. 不允许在 `client` 再造一套 action effectiveness 规则表。
2. 不允许为了 UI 便利增加裸字符串路径。
3. 不允许为了“更酷”牺牲 locale 可读性或 golden 稳定性。
4. 不允许把 ASCII 风格重新升级成正式玩家路径。

## 13. 结论

这次文档改造后的结论很简单：

1. K-ToME 的后续 UI/UX 优化，应该以 **项目自身介质与玩法表达** 为中心，而不是以某个外部样本的布局为中心。
2. 外部参考真正有价值的，是：
   - 解释型按钮
   - 分层决策
   - 图标全链路消费
   - 日志与操作联动
3. 对 K-ToME 更关键的，是把这些原则翻译成：
   - map-dominant 的桌面 roguelike 体验
   - 键盘优先的操作可见性
   - 回合节拍驱动的反馈层级
   - client-local design token 与稳定的信息面骨架

如果后续要开始实际实现，优先顺序应是：

1. `P2-W5.0` 先立 client-local token 与信息面骨架；
2. 然后做首页和局内主要信息面的正式化；
3. 再逐步扩到 `iconKey`、状态、动态说明、telegraph 和战斗二级决策。

这条路线是在吸收参考，而不是模仿参考。

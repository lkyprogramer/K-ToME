> 执行前必须先完整阅读并接受：
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
> `docs/phase2/roadmap.md`
> `docs/phase2/2026-03-13-phase2-verification-checklist.md`
> `docs/phase3/roadmap.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`
> `docs/opt/2026-04-20-client-ui-ux-reference-driven-optimization-design.md`
> `docs/review/2026-04-21-client-ui-ux-optimization-design-review-v2.md`

# K-ToME 客户端 UI/UX 优化执行计划

**阶段**: `Phase 2 / P2-W5 ~ P2-W7 + Phase 3 / P3-W2 ~ P3-W4`  
**优先级**: `P0`  
**工作量评估**: `L+`（`15~21` 人日，按 8 个串行 PR 估算）  
**前置条件**: `2026-04-20-client-ui-ux-reference-driven-optimization-design.md` 继续作为体验/原则真源；本文只负责执行级拆包、出口和验证  
**对应问题**:

1. 现有 UI/UX 设计文档已经完成体验方向收口，但仍然偏“设计说明书”，还不是可以直接排期和按 PR 落地的执行手册。
2. `client` 当前仍是 `map + sidebar + bottom cards` 的演进态，尚未完成正式的信息面、输入语义、modal 栈和开发级验证切面。
3. `iconKey`、装备品质、地图掉落可见性、状态/说明/telegraph 三条线已经有 contract 基础，但缺少明确的 PR 切分、退出条件和失败兜底。
4. 外部 review v2 提到的执行层问题里，有些方向正确但具体钩子不应照抄，例如：
   - 关键词体系应优先复用现有 `KeywordRegistry` / `DescriptionPresenter`
   - telegraph 应优先扩现有 `PendingTelegraphState -> OverlayRenderSnapshot -> TelegraphRenderer` 链路，而不是先发明一整套 `AIPlanSnapshot`
5. 当前短期内玩家只有 1 人，因此 UX telemetry、行为统计、长期观察口不进入近期强制交付，只保留开发调试所需的最小 fail-fast 日志和 artifact。

---

## 0. 当前状态快照

基于当前仓库真源，这轮执行计划建立在以下已存在事实之上：

1. `TileRenderer` 仍是 `地图 + sidebar + bottom cards` 的布局，不是固定三栏。
2. `InputHandler` 已有扁平 `OverlayState + reconcileMode(snapshot)` 状态机，说明 modal / inspect / targeting 不是临时调试面；但这也意味着 `PR-03` 不是“给现有 overlay mode 套一层壳”，而是要显式完成 active/passive mode 的 owner 划分。
3. `StatusHudRenderer` / `StatusIconResolver` 已开始消费状态图标与 badge 文本，但分类、优先级和跨面一致性仍不完整。
4. `MapCellSnapshot.items` 已经存在，`ItemRenderSnapshot` 已有：
   - `iconKey`
   - `qualityTierId`
   - `qualityNameKey`
   这意味着“地图掉落提示”和“品质展示”已经有正式输入面。
5. 当前正式品质合同不是传统 ARPG 的全梯度，而是：
   - `RarityTier.NORMAL / MAGIC / RARE`
   - `SpecialTier.UNIQUE / ARTIFACT`
6. 当前 snapshot 没有正式 item stack quantity 字段。
   - `MapCellSnapshot.items.size` 只能证明“同格有几件掉落物”
   - 不能代表“某件物品的可堆叠数量”
7. `KeywordRegistry`、`DescriptionModel`、`DescriptionPresenter` 已存在。
   - 这轮执行计划不再创建第二套 `KeywordDictionary`
8. 现有 telegraph 正式链路已经存在：
   - `core/game` 运行时状态：`PendingTelegraphState`
   - `snapshot` 消费面：`RenderSnapshot.overlays: List<OverlayRenderSnapshot>`
   - `client` 表现面：`TelegraphRenderer`
   - 这轮执行计划优先扩这条链路，不先造新的 AI 计划快照家族
9. 当前仓库已经有可复用的验证入口：
   - `ClientSmokeHarnessTest`
   - `GoldenScreenshotHarnessTest`
   - `localeLint`
   - `contractLint`
   - `maintainabilityLint`

---

## 1. 阶段目标

本文档的目标不是再补一轮“概念层建议”，而是把现有 UI/UX 设计文档升级为可执行路线。

完成标准：

1. 现有战略文档中的蓝图条目，都能映射到明确的 PR / 工作包。
2. 每个 PR 都具备：
   - 目标
   - 范围
   - 任务拆解
   - 退出条件
   - 推荐验证
   - 风险与最小切片
3. `client` 的共用执行基座先冻结：
   - design token
   - 信息面骨架
   - 输入语义
   - modal 栈
   - 图标/品质/掉落的表现基线
4. `Phase 3` 的状态、动态说明、telegraph 不再停留在概念描述，而是有正式依赖关系与 cutover 顺序。
5. review v2 中真正成立的 P0/P1 问题全部被吸收到执行计划，但不引入不必要的第二 contract。
6. 短期内不把用户 telemetry / 行为统计当成近期阻塞项。

---

## 2. 为什么必须拆成一组 PR

### 2.1 这不是单点 UI polish，而是四条执行线需要分别冻结

后续开发至少同时涉及四条线：

1. `client` 共用 UI 基座
2. 首页 / 局内信息面 / modal 输入语义
3. 图标 / 品质 / 掉落可见性
4. 状态 / 动态说明 / telegraph

如果把它们混成一个“大 UI 重构 PR”，结果会是：

1. 难以判断某个回归是布局问题、输入问题还是 contract 问题
2. golden 重拍范围失控
3. `client` 很容易长出第二套语义或临时兼容路径

### 2.2 `Phase 2` 的基座工作必须先于 `Phase 3` 的说明与 telegraph

原因是：

1. 没有 token、信息面骨架和 modal 栈，`P3-W2 ~ P3-W4` 的说明/telegraph 再丰富也只会继续塞进当前 renderer 的局部补丁。
2. `Phase 3` 的玩家可读性提升，需要一个已经稳定的“展示容器”，否则每次新增状态或说明都会反复改布局。
3. 先冻结 `client` 的执行基座，后面的说明、关键词、telegraph 才能变成“接线”，而不是“再造结构”。

### 2.3 图标/品质/掉落可见性是一条独立的 contract 收口线

这条线的核心不是“好不好看”，而是：

1. `iconKey` 是否完整覆盖
2. 当前正式品质合同如何进入 UI
3. 地图掉落是否从“sidebar 内文字”升级到“世界面可见”
4. 是否错误地在 `client` 先发明了新品质档位或 item stack 语义

因此它应该独立成一组 PR，而不和 layout/input/telegraph 混在一起。

### 2.4 review v2 里的后置项不能抢主线

以下方向有价值，但近期不应抢主线：

1. 本地 UX telemetry / 用户行为统计
2. 教学关 / tutorial 正式体系
3. Advanced Tooltip
4. Death Replay / Turn Log
5. Seed / Snapshot Share

它们统一进入长期候选，不进入当前 8 个 PR 的阻塞链。

---

## 3. 必须冻结的合同

1. `RenderSnapshot`、`VisualManifest`、`AudioManifest` 继续是 `client` 唯一正式输入。
2. `client` 不得新建第二套战斗、品质、掉落、telegraph、说明真源。
3. 当前正式品质合同继续固定为：
   - `RarityTier.NORMAL / MAGIC / RARE`
   - `SpecialTier.UNIQUE / ARTIFACT`
4. 若未来要支持 `SET / LEGENDARY / MYTHIC` 等额外品质档位，必须先扩 `core/game` 合同，再扩 UI；当前计划中禁止在 `client` 先自造。
5. 所有可装备物与 special template item 都必须能解析到正式 `iconKey`。
6. `MapCellSnapshot.items` 在当前阶段只承担：
   - “该格是否有掉落物”
   - “该格有多少件掉落物”
   不能被解释为正式 item stack quantity。
7. 真正的装备堆叠 / 背包 stack contract，不在当前计划范围内；若未来需要，必须先扩 `ItemRenderSnapshot` / inventory contract。
8. 关键词体系优先复用：
   - `KeywordRegistry`
   - `DescriptionModel`
   - `DescriptionPresenter`
   禁止平行再造 `KeywordDictionary`。
9. telegraph 体系优先复用：
   - `PendingTelegraphState`
   - `OverlayRenderSnapshot`
   - `TelegraphRegistry`
   - `TelegraphRenderer`
   不把“普通敌人 intent”前置成必须先落一整套新 snapshot 家族；当前 `client` 只消费已暴露到 snapshot 的 telegraph/overlay typed fact。
10. design token 只属于 `client`。
    - 不进入 `RenderSnapshot`
    - 不进入 `VisualManifest`
    - 不进入内容 schema
11. 输入统一语义一旦冻结，后续各 UiMode 都必须复用，不允许局部特例不断长出。
12. 短期内不引入面向真实玩家运营的行为统计或数据采集；所有新增日志都必须有直接调试价值。

---

## 4. 范围与非目标

### 4.1 范围

1. `P2-W5.0`：client-local token、信息面骨架、开发态 chrome 清理
2. `P2-W5a`：首页首屏与快速开始路径
3. `P2-W5b`：局内信息面、modal 栈、Look Mode 基础版、统一输入语义
4. `P2-W5c`：图标全链路、品质展示、地面掉落世界面提示
5. `P2-W6 ~ P2-W7`：内容扩张期的一致性 gate、卡片复用、错误/空态/加载态
6. `P3-W2`：状态语义、视觉层级与 a11y 基线落地
7. `P3-W3`：动态说明、关键词消费、解释型检视
8. `P3-W4`：telegraph 与战斗三层决策面

### 4.2 非目标

1. 不在当前计划里引入正式渲染风格切换。
2. 不在当前计划里引入新的装备品质档位。
3. 不在当前计划里引入正式装备堆叠 / 背包 stack contract。
4. 不把 tutorial 体系作为当前阶段阻塞项。
5. 不把 telemetry / 用户行为统计 / 长周期观察口作为近期阻塞项。
6. 不承诺当前阶段做 OS 级屏幕阅读器集成。
   - 当前只做键盘路径、焦点可见、高对比度、色盲回退、字号/动效可调这类实际可落地的 a11y 基线
7. 不把普通敌人 intent 全量可见当成 `P3-W4` 的前置门槛。
   - 当前主线仍是既有 telegraph 的地图/目标卡/日志一致化

---

## 5. 设计总览

### 5.1 蓝图—PR 映射矩阵

| 蓝图条目 | PR-01 | PR-02 | PR-03 | PR-04 | PR-05 | PR-06 | PR-07 | PR-08 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 首页首焦点 / 快速开始 / 继续游戏 |  | ✓ |  |  |  |  |  |  |
| 首页 build 能力速览 / 常驻帮助区 | ✓ | ✓ |  |  |  |  |  |  |
| 信息面骨架（世界 / 上下文 / 角色动作 / modal） | ✓ |  | ✓ |  |  |  |  |  |
| 统一键盘语义 / modal 栈 / Look Mode 基础版 |  |  | ✓ |  |  |  |  |  |
| 事件 / 商店 / 奖励房统一卡片模型 |  |  |  |  | ✓ |  |  |  |
| 图标全链路 / 品质色 / 地图掉落提示 |  |  |  | ✓ | ✓ |  |  |  |
| 错误态 / 空态 / 加载态 |  |  | ✓ |  | ✓ |  |  |  |
| 状态层级 / badge 规则 / 高风险视觉分离 |  |  |  |  |  | ✓ |  | ✓ |
| 动态说明 / 关键词 / 解释型检视 |  |  |  |  |  |  | ✓ |  |
| 战斗三层决策 / telegraph 三位一体 |  |  |  |  |  |  |  | ✓ |
| a11y / 音效 / 性能基线 | ✓ |  | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

### 5.2 统一输入语义表

以下语义一旦进入实现，就视为当前计划内的正式输入约定：

| 键 | 地图态 | modal / drawer 态 | targeting / 三层决策态 | 原则 |
| --- | --- | --- | --- | --- |
| `ESC` | 退出当前瞬态上下文；若已在根地图态，则不再承担“操作说明”职责 | 关闭最上层 modal | 直接退出到地图态 | 全退 |
| `Backspace` | 无 | 关闭当前详情层并回上一层 modal | 回上一层决策面 | 退一层 |
| `Tab` | 在世界面 / 上下文面 / 角色动作面之间切换焦点 | 在当前 modal 可交互项之间循环 | 在候选目标或候选方式之间切换 | 焦点切换 |
| `?` | 打开地图态帮助 overlay | 打开当前面板帮助 | 打开当前动作解释 | 上下文帮助 |
| `i` | 打开背包 | 无 | 无 | 背包快捷入口 |
| `x` | 进入 Look Mode 基础版 | 无 | 无 | 检视模式 |
| `Enter` | 与楼梯/交互物确认交互 | 确认当前焦点 | 确认当前动作/方式/目标 | 肯定性提交 |
| `F` / `.` | 等待一回合 | 无 | 无 | 空等 |
| `1-9` | 使用 active talent / inscription / 面板数字项 | 对应数字项选择 | 对应动作/方式/目标编号 | 数字直达 |
| `Ctrl+S` | 保存 | 保存并保持当前上下文 | 禁用 | 系统级命令 |

补充约束：

1. modal 栈深度最大为 `3`：
   - `Inventory -> Item Detail -> Compare`
   - 超过后必须压平，而不是继续套娃
2. 关闭 modal 后恢复：
   - 打开前的地图光标
   - 打开前的焦点区域
3. `ESC` 与 `Backspace` 不能语义重叠：
   - `ESC` 是全退
   - `Backspace` 是退一层

### 5.2a ModalStack 与 UiMode 归属

`PR-03` 的 `ModalStack` 不直接保存原始 `UiMode`，而是保存 client-local `ModalFrame`：

1. `frame.kind`
   - `INVENTORY`
   - `LOADOUT_EDIT`
   - `TALENT_ASSIGN`
   - `INSPECT`
   - `TARGETING`
   - 后续 `PR-08` 的 `COMBAT_DECISION`
2. `frame.localState`
   - 该 frame 所需的最小局部游标或焦点状态
   - 例如 `inventorySelection / inspectCursor / targetingCursor`
3. `ModalStack` 只负责主动打开的 client-local frame，不直接托管 snapshot 被动强制 mode。

当前阶段按以下 owner 划分：

| UiMode | 类型 | owner | 是否进入 ModalStack | 当前阶段规则 |
| --- | --- | --- | --- | --- |
| `MAP` | 根态 | `InputHandler` | 否 | 永远是基线落点 |
| `WORLD_MAP` | 被动态 | `reconcileMode(snapshot)` | 否 | `activeRouteSelection != null` 时强制接管 |
| `SHOP` | 被动态 | `reconcileMode(snapshot)` | 否 | `activeShop != null` 时强制接管 |
| `STAT_ASSIGN` | 被动态 | `reconcileMode(snapshot)` | 否 | `hasPendingStatAllocation(snapshot)` 时强制接管 |
| `INVENTORY` | 主动态 | `ModalStack` | 是 | 由 `i` 或等价入口 push |
| `LOADOUT_EDIT` | 主动态 | `ModalStack` | 是 | 由 `L` 或等价入口 push |
| `TALENT_ASSIGN` | 主动态 | `ModalStack` | 是 | 由 `T` 或等价入口 push |
| `INSPECT` | 主动态 | `ModalStack` | 是 | `PR-03` 直接升级现有 `UiMode.INSPECT`，不新增 `LOOK` 枚举 |
| `TARGETING` | 主动态 | `ModalStack` | 是 | 当前仍保留为目标选择 frame |
| `VALIDATION` | 开发态 | validation owner | 否 | 不纳入正式 `ModalStack` 体系 |

强约束：

1. `ModalStack` 深度上限 `3` 只统计主动态 frame。
2. `?` 打开的帮助说明属于当前 frame 的子面板，不额外占一层 stack。
3. 当前阶段若 `WORLD_MAP / SHOP / STAT_ASSIGN` 这类被动态在主动态 frame 上方被触发，**被动态优先接管并清空 active stack 回 MAP 基线**；`PR-03` 不做“隐藏栈恢复”，避免同时维护两套 owner。
4. `VALIDATION` 继续独立，不与正式 UI/UX 路径共享 stack 语义。

### 5.2b 键位迁移决议

`PR-03` 不是从空白键位开始设计，而是要把现有 `F / I / L / X` 迁到统一语义下。

| 键位 | 当前语义 | 当前计划决议 | 备注 |
| --- | --- | --- | --- |
| `ESC` | 局部存在 | 升级为正式全退键 | 主路径强制统一 |
| `Backspace` | talent rollback 等局部行为 | 升级为正式退一层键 | 仅用于 stack/phase 回退，不承担全退 |
| `F` | overlay 通用关闭 | `P2-W5b` 保留为 legacy close alias | `ESC` 成为主入口；`F` 只做兼容别名，不再写进新帮助主文案 |
| `I` | MAP 打开背包；SHOP 关闭商店 | 保留 | 作为“背包开关 / 商店对称关闭”快捷键保留 |
| `L` | MAP 打开 Loadout；Loadout 内关闭 | 保留 | `ESC` 加入后，`L` 仍保留为对称开关 |
| `X` | MAP 进入 INSPECT；SHOP/WORLD_MAP 里兼作下移 | 保留 MAP→INSPECT；移除非 inspect 的下移职责 | 列表下移统一回 `DOWN / S` |

当前阶段明确废除的歧义：

1. `X` 不再承担 `SHOP / WORLD_MAP` 中的下移选择职责。
2. `F` 不再是新设计里的唯一 overlay 关闭键。
3. `INSPECT` 继续使用 `X` 打开/关闭，直到 `PR-03` 完成 Look Mode cutover。

### 5.3 共享 UI 基座

当前计划统一以以下 client-local 基座为中心展开：

1. `UiDesignTokens`
   - 颜色、字号、间距、动效、圆角、描边
2. `InfoSurfaceLayout`
   - 作为 `TileLayoutMetrics` 的上游 strategy
   - `PR-01` 先只落 `MapDominant`
   - `WideSplit / ModalOverlay` 在类型上预留，不在 `PR-01` 一次展开
3. `LogPresentationModel`
   - 分类、重要度、折叠、锚点
4. `PlayerCardModel / TargetCardModel / ActionPanelModel`
5. `ModalStack`
6. `GroundLootMarkerModel`
7. `QualityPresentation`
   - 品质名称色
   - 品质边角标
   - 与正式品质合同的映射

固定要求：

1. 这些模型都属于 `client` 表现层。
2. 不能反向污染 `RenderSnapshot`。
3. 所有“typed fact”都来自现有 snapshot / manifest / registry。

### 5.4 错误态 / 空态 / 加载态基线

这些态不再允许散落在局部字符串里临时处理，必须作为正式 UX 面补齐：

1. **资源缺失态**
   - `iconKey` / `visualKey` 缺失时，显示统一 fallback 资源和文本
   - 不允许悄悄隐藏整行或整块内容
2. **空态**
   - 背包为空
   - 当前格无可检视目标
   - 日志为空
   - 商店无货
3. **加载态**
   - 首次进入客户端的资源 bootstrap
   - 进入大 modal 前的必要数据准备
4. **错误态**
   - manifest 版本不匹配
   - save 恢复失败
   - snapshot 不完整

统一要求：

1. 错误态必须给出返回路径：
   - `Retry`
   - `Back To Menu`
   - `Copy Error Detail`（开发期优先）
2. 空态必须给出下一步引导，不接受只显示 `Empty`
3. 加载态必须是短时过渡，不能成为长期遮罩

### 5.5 a11y、音效、性能与观测边界

#### a11y 基线

当前阶段必须落的只有：

1. 键盘路径完整
2. 焦点可见
3. 高对比度 / 色盲回退
4. 字号档位
5. `Reduce Motion`

当前阶段不承诺：

1. OS 级 screen reader 完整集成

#### 音效作为信息层

当前阶段只立规则，不单独开一条重 PR：

1. UI 操作音
2. 战斗反馈音
3. telegraph 预警音
4. 环境氛围层

固定边界：

1. 所有音效都走 `AudioManifest`
2. `client` 不硬编码原始资源路径

#### 性能基线

以下是本计划的验收预算，不代表当前已有实测数据：

1. 目标帧率：`60 fps`
2. 密集战斗同屏场景下，总帧时间目标 `P95 < 16.6ms`
3. modal 打开到首个稳定可交互帧的视觉响应目标 `< 100ms`
4. 不允许新增常驻 idle animation 作为默认表现

#### 观测边界

1. 当前阶段不做用户行为 telemetry
2. 只保留开发调试所需的：
   - fail-fast 报错
   - golden artifact
   - smoke artifact
   - 必要截图/hash

### 5.6 图片与音频资源开发基线

当前计划不是“默认每个 PR 都去补一批新素材”，而是把真正需要资源补充的 PR 约束到现有正式管线上。

固定原则：

1. 新增图片和音频一律复用现有资源目录、生成脚本、处理脚本、manifest 与 lint；禁止为本轮 UI/UX 计划再造第二套素材流程。
2. 只有当交互清晰度、可读性或正式 key 覆盖确实依赖新资源时，才允许在对应 PR 中补图片 / 音频 plan；纯 layout、token、输入语义 PR 默认先复用现有资源。
3. 任何 PR 只要引入新的 `iconKey`、`visualKey`、`audioProfile`、`cueFamily` 或 runtime `sourcePath`，该 PR 就必须同时提交对应 plan、manifest wiring 和 lint 接线；不允许功能已进入 UI，但资源计划仍停留在 TODO。
4. 图片不是项目尾声的“美术收尾”，音效也不是单独后置的大补丁；如果某个 PR 的正式体验依赖资源补充，就必须在该 PR 内收口到可验证状态。
5. 图片批量生成前必须具备 `GEMINI_API_KEY`。未提供 key 时，只允许继续编写 plan、prompt、manifest 接线和 fallback 路线，不得把“计划已写好”描述成“图片已完成”。
6. 当前阶段的音效补充坚持最小必要 cue 原则；不为了单人阶段的 UI polish 提前扩张大规模 ambience、variation bank 或行为统计钩子。

图片管线标准：

1. 计划文件统一放在 `assets-src/image/specs/`；本计划建议命名为 `phase2-uiux-prNN-gemini-plan.yaml` 或 `phase3-uiux-prNN-gemini-plan.yaml`。
2. 生成报告统一落到 `assets-src/image/manifests/`；建议文件名为 `phase?-uiux-prNN-generation-report.jsonl`。
3. 处理报告统一落到 `assets-src/image/manifests/`；建议文件名为 `phase?-uiux-prNN-processing-report.jsonl`。
4. 图片 canonical source manifest 仍以 `assets-src/image/manifests/phase2-visual-manifest.json` 为准，runtime manifest 仍以 `client/src/main/resources/manifests/visual-manifest.json` 为准。
5. 标准执行顺序固定为：先写 plan，再运行 `./scripts/generate_assets.sh`，再运行 `python3 scripts/process_assets.py`，最后运行 `python3 scripts/sync_phase2_manifests.py` 完成 manifest 同步。
6. 只要新增了 PR 级 image plan，就必须在同一 PR 更新 root `build.gradle.kts`，把该 plan 接到 `assetLint / styleLint / manifestLint` 的 `--extra-plan`。
7. 图片 plan 验收最小集合固定为：`./gradlew assetLint`、`./gradlew styleLint`、`./gradlew manifestLint`。

音频管线标准：

1. 计划文件统一放在 `assets-src/audio/specs/`；本计划建议命名为 `phase2-uiux-prNN-audio-plan.yaml` 或 `phase3-uiux-prNN-audio-plan.yaml`。
2. 如果该 PR 需要脚本生成 raw cue，再追加 `phase?-uiux-prNN-audio-generation-plan.yaml`，但 generation plan 仍服务于同一个正式 audio plan。
3. 音频 canonical source manifest 仍以 `assets-src/audio/manifests/phase2-audio-manifest.json` 为准，runtime manifest 仍以 `client/src/main/resources/manifests/audio-manifest.json` 为准。
4. 音频标准执行顺序固定为：先写 audio plan，再把 raw 素材导入 `assets-src/audio/raw` 或通过窄作用域脚本生成 raw cue，再运行 `python3 scripts/process_audio.py --filter-plan <plan>`，最后运行 `python3 scripts/sync_phase2_manifests.py`。
5. 只要新增了 PR 级 audio plan，就必须在同一 PR 更新 root `build.gradle.kts`，把该 plan 接到 `audioLint` 的 `--extra-plan`；如果新增 key 还影响 manifest 对齐，也必须同步验证 `manifestLint`。
6. 音频 plan 验收最小集合固定为：`./gradlew audioLint`；若该 PR 同步触碰图片 manifest 或 runtime 路径，再补 `./gradlew manifestLint`。

Companion 资源切片规则：

1. 同一个功能 PR 可以携带一个很小的 companion image/audio slice，但不能把它膨胀成新的 mega asset PR。
2. 若缺少 `GEMINI_API_KEY` 或 raw 音频输入而无法立即产出正式资源，只允许在 runtime 仍能稳定回退到既有 fallback 的前提下先提交 plan 和 wiring；真正启用新 key 的 UI 路径，不得在资源未落地前提前开放。
3. 所有 companion 资源切片都必须围绕 K-ToME 自身的信息可读性服务，不以“模仿参考网站的版式或氛围”作为目标。

---

## 6. PR 拆分总览

| PR | 对应包 | 主题 | 主要模块 | 资源计划 | 进入下一 PR 的门槛 |
| --- | --- | --- | --- | --- | --- |
| `PR-01` | `P2-W5.0` | token + 信息面骨架 | `client/render`, `client/ui` | 默认不新增图片/音频 | design token 与布局骨架冻结，开发态 chrome 清理完成 |
| `PR-02` | `P2-W5a` | 首页首屏优化 | `MainMenu*` | 默认复用；必要时窄补首页 cue | 首焦点、快速开始、build 摘要、常驻帮助稳定 |
| `PR-03` | `P2-W5b` | 局内信息面 + 统一输入语义 + Look Mode 基础版 | `TileRenderer`, `InputHandler`, `FoundationGameScreen` | 默认不批量补图；仅允许极小 companion cue | modal 栈、键位语义、焦点恢复、信息面分层稳定 |
| `PR-04` | `P2-W5c` | 图标全链路 + 品质 + 地面掉落世界面提示 | `RenderSnapshot`, `TileRenderModel`, manifests, content | 必须补装备图标；音频按缺口窄补 | 装备 icon 完整、品质色冻结、地图掉落可见 |
| `PR-05` | `P2-W6 ~ P2-W7` | 内容扩张一致性、事件/商店卡片复用、错误/空态/加载态 | `client`, `game i18n`, `tools lint` | 仅在共享卡片缺 formal key 时窄补 | content-ui gate 与 shared modal card 成型 |
| `PR-06` | `P3-W2` | 状态语义、badge 规则与高风险视觉层级 | `StatusHud*`, `TargetCard*`, `TelegraphRenderer` | 可能需要状态/telegraph 小批资源 | 状态分层和 telegraph 权重成立 |
| `PR-07` | `P3-W3` | 动态说明、关键词消费与解释型检视 | `DescriptionPresenter`, `KeywordRegistry`, inspect panels | 默认不新增音频；图片仅在关键词 chip 不可复用时窄补 | 说明进入主要阅读路径，关键词 contract 不再只停在 tooltip |
| `PR-08` | `P3-W4` | telegraph 三位一体与战斗三层决策面 | `telegraph`, `combat panel`, `InputHandler` | 可能需要战斗 affordance 图标和少量预警 cue | Boss telegraph + 战斗决策层完成当前阶段收口 |

固定规则：

1. 当前 PR 未完成前，不并行推进下一 PR。
2. 当前 PR 的 golden / smoke / lint / doc 口径未收口前，不进入下一 PR。
3. `PR-01 ~ PR-04` 是 `Phase 2` 地基链，任何一个未完成都会放大 `Phase 3` 的返工成本。

### 6.1 硬前置依赖

当前 8 个 PR 不是简单串行，而是存在明确硬前置：

1. `PR-01 -> PR-03`
   - `PR-03` 的帮助/焦点/telegraph/主菜单色值都应建立在 token 基座上。
2. `PR-03 -> PR-04`
   - 地面掉落 marker 与 Look Mode 光标必须在同一信息面体系内工作。
3. `PR-01 -> PR-06`
   - `PR-06` 的状态层级和 telegraph 权重不应再引入新的硬编码色。
4. `PR-03 -> PR-07`
   - 解释型检视和关键词详情要复用 `ModalStack` 与退一层语义。
5. `PR-01 + PR-03 + PR-04 -> PR-08`
   - `PR-08` 需要 token 基座、统一输入/stack 语义，以及 `PR-04` 已冻结的 icon/掉落可读性。
6. `PR-04 -> PR-05`
   - 共享卡片模型默认复用已经补齐的 item/icon 资源，不先自造第二套临时图标。

建议估时：

1. `PR-01`：`1.5` 人日
2. `PR-02`：`1` 人日
3. `PR-03`：`3~4` 人日
4. `PR-04`：`2~2.5` 人日
5. `PR-05`：`1.5` 人日
6. `PR-06`：`1.5~2` 人日
7. `PR-07`：`1.5~2` 人日
8. `PR-08`：`2.5~4` 人日

---

## 7. PR-01：Token 与信息面骨架前置

### 7.1 目标

建立 `client` 的正式表现基座，让后续所有 UI 改动都不再直接堆在 `TileRenderer` 的硬编码色值和散乱布局常量上。

### 7.2 范围

涉及文件建议：

1. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt`
   - `client/src/main/kotlin/com/ktome/client/render/layout/InfoSurfaceLayout.kt`
2. 修改：
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
   - `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/creation/PlayerCreationPanel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/status/StatusHudRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt`

### 7.3 任务拆解

1. 建立 `UiDesignTokens`
   - 颜色 token
   - 字号/数字 token
   - 间距 token
   - 动效 token
2. 把当前 renderer 中触碰到的核心硬编码颜色替换为 token 引用。
   - 至少覆盖 `TelegraphRenderer.fallbackColorHex / tileTone / asciiTone`
   - 至少覆盖 `MainMenuScreen` 当前主文案与 `PlayerCreationPanel` 状态色
3. 提供 `MapDominant / WideSplit / ModalOverlay` 三种布局骨架。
   - `PR-01` 只要求 `MapDominant` 真正接管现有 `TileLayoutMetrics`
   - `WideSplit / ModalOverlay` 先以 sealed strategy 预留
4. 去掉窗口标题里的开发态操作说明，把标题改回会话信息或纯应用名。
5. 冻结基本 a11y token：
   - focus ring
   - high contrast text
   - disabled alpha

**资源计划**

1. 本 PR 默认不新增图片或音频 plan。
2. `UiDesignTokens`、focus ring、disabled/empty/error 表现优先用 token、描边、shape 和既有 fallback 资源表达，不为此启动 Gemini 批量出图。
3. 如果本 PR 暴露出“现有 fallback icon / audio cue 完全无法支撑 fail-fast 可读性”的缺口，只允许登记到后续资源型 PR；不把 `PR-01` 膨胀成资源补丁包。

### 7.4 退出条件

1. 新增 UI 代码不再写入裸 hex 颜色。
2. `TileRenderer`、`StatusHudRenderer`、`MainMenu` 至少 3 个面开始消费 token。
3. `TelegraphRenderer` 不再自行维护裸 hex fallback 色。
4. 窗口标题不再承担操作说明职责。
5. 信息面骨架具备正式类型，而不是继续靠局部常量隐式表达。

### 7.5 推荐验证

```bash
./gradlew localeLint
./gradlew contractLint
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest"
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
```

若本 PR 命中 non-trivial Kotlin 结构重排，再补：

```bash
./gradlew maintainabilityLint
```

### 7.6 风险与最小切片

风险：

1. token 设计过度，一次引入太多层级
2. 布局骨架改动过大，golden 爆炸

最小切片：

1. 先完成 token + 标题去开发态
2. `InfoSurfaceLayout` 先只封装现有 map/sidebar/cards，不强行一次改造全部结构

---

## 8. PR-02：首页首屏优化

### 8.1 目标

把首页从“开发态入口”改成正式的开局面，解决首焦点、快速开始、继续游戏、build 能力速览和常驻帮助区。

### 8.2 范围

涉及文件建议：

1. `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt`
2. `client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt`
3. `client/src/test/kotlin/com/ktome/client/screen/MainMenuControllerTest.kt`
4. 新增：
   - `client/src/main/kotlin/com/ktome/client/screen/MainMenuSummaryModel.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/MainMenuFocusPolicy.kt`

### 8.3 任务拆解

1. 冻结首页首焦点：
   - 首启优先 `快速开始`
   - 有存档优先 `继续游戏`
   - `MainMenuController` 必须显式接收“是否存在 continue 入口”的初始焦点策略，而不是仅在 `pollAction` 时临时判断
2. 增加 build 能力摘要：
   - 当前可玩职业/种族
   - 当前支持的短局深度
3. 增加常驻帮助区：
   - locale
   - 关键键位
   - 新玩家安全入口
4. 新老玩家差异化首屏：
   - 首启态
   - 有存档态

**资源计划**

1. 本 PR 默认复用既有标题、按钮、manifest key 与 `audio.ui.*` cue，不单开首页大图或背景氛围批次。
2. 只有当首页的 `快速开始 / 继续游戏 / 帮助` 在当前视觉体系里仍然缺少稳定识别点时，才允许增加窄作用域资源 plan：
   - 图片：`assets-src/image/specs/phase2-uiux-pr02-gemini-plan.yaml`
   - 音频：`assets-src/audio/specs/phase2-uiux-pr02-audio-plan.yaml`
3. 该 companion plan 的资源范围必须严格收敛到首页 affordance，例如按钮徽标、帮助徽标、状态徽标；禁止引入大背景插画或纯装饰资源。
4. 若新增音频，只允许围绕 `confirm / cancel / help-open` 这类首页关键信号做最小补充；若现有 `audio.ui.confirm / cancel / hover` 足够，则不新增音频。

### 8.4 退出条件

1. 首页首焦点路径固定且可 smoke。
2. `快速开始` 与 `继续游戏` 至少有一个始终是首屏显著主按钮。
3. locale 与主要键位不再隐藏在次级文案中。
4. 首页不再要求玩家先读完整屏说明才能开始一局。

### 8.5 推荐验证

```bash
./gradlew :client:test --tests "com.ktome.client.screen.MainMenuControllerTest"
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew localeLint
```

### 8.6 风险与最小切片

风险：

1. 首页摘要过多，重新变成信息墙

最小切片：

1. 首焦点
2. 快速开始 / 继续游戏
3. 常驻帮助区

build 能力摘要可以延后精修，不阻塞 PR 完成。

---

## 9. PR-03：局内信息面、输入语义与 Look Mode 基础版

### 9.1 目标

把局内 UI 从“多个 mode 的局部拼接”提升成有正式语义的信息面系统，并冻结输入语义、modal 栈和基础 Look Mode。

### 9.2 范围

涉及文件建议：

1. `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
2. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
3. `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
4. `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`
5. `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt`
6. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/panel/LogPresentationModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/panel/PlayerCardModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/panel/TargetCardModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/panel/ActionPanelModel.kt`

### 9.3 任务拆解

1. 形式化四面：
   - 世界面
   - 上下文面
   - 角色/动作面
   - modal 面
2. 固化 `UiMode` owner 划分到 `InputHandler`：
   - `WORLD_MAP / SHOP / STAT_ASSIGN` 继续由 `reconcileMode(snapshot)` 强制接管
   - `INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / INSPECT / TARGETING` 收敛到 `ModalStack`
   - `VALIDATION` 继续独立
3. 引入 `ModalStack`，限制活跃 frame 深度为 `3`。
4. 当前阶段若被动态接管，直接清空 active stack 回 `MAP` 基线；`PR-03` 不做隐藏栈恢复。
5. 把当前 `UiMode.INSPECT` 直接升级为基础 Look Mode，不新增 `LOOK` 枚举：
   - 自由移动检视光标
   - 世界面 / 目标卡 / 检视面同步当前光标信息
6. 明确焦点恢复与 `ESC / Backspace / Tab / ? / F / I / L / X` 迁移后的行为。
7. `PR-08` 的战斗三层决策面在当前计划中采用**单一 `CombatDecisionFrame` 内部 phase 机**，不是 3 个独立 stack frame；`PR-03` 只需为后续 frame 类型预留接入点。
8. 补齐空态：
   - no visible target
   - empty inventory
   - empty shop

**资源计划**

1. 本 PR 默认不启动批量图片生成；modal chrome、Look Mode 光标、焦点框、帮助提示优先由 token、shape 和现有 icon 组合完成。
2. 只有当 `Inspect / Look Mode / Help Overlay` 的正式入口确实缺少稳定图标时，才允许补一个极小 image companion plan：`assets-src/image/specs/phase2-uiux-pr03-gemini-plan.yaml`。
3. 音频默认复用现有 `audio.ui.*` 家族；只有当 modal 打开/关闭/报错在交互上必须和通用 confirm/cancel 区分时，才允许补 `assets-src/audio/specs/phase2-uiux-pr03-audio-plan.yaml`。
4. 禁止为了 panel 装饰、drawer 花边或 Look Mode 氛围，提前批量补一套新图或新音。

### 9.4 退出条件

1. `ESC` 与 `Backspace` 的语义不再冲突。
2. 现有 `F / I / L / X` 的兼容/废弃决议已经固化，不再在 `InputHandler` 各分支临时判断。
3. modal 关闭后可以回到原地图光标与焦点区。
4. `Tab` 焦点链不会越界或卡死。
5. 基础 Look Mode 可在地图自由移动，不要求完整版多层 tooltip。
6. `LogPresentationModel` 至少具备分类、重要度、空态和回退文案。

### 9.5 推荐验证

```bash
./gradlew :client:test --tests "com.ktome.client.input.InputHandlerTest"
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew localeLint
```

若本 PR 涉及大规模 Kotlin 结构调整，再补：

```bash
./gradlew maintainabilityLint
```

### 9.6 风险与最小切片

风险：

1. `InputHandler` 状态机会继续膨胀
2. Look Mode 与 targeting 冲突

最小切片：

1. 先冻结统一输入语义
2. 先做 modal 栈和焦点恢复
3. Look Mode 只做自由光标 + 单层同步，不做多层 tooltip

---

## 10. PR-04：图标全链路、品质展示与地图掉落提示

### 10.1 目标

把 `iconKey`、当前正式品质合同和地图掉落可见性，收口成正式的执行面。

### 10.2 范围

涉及文件建议：

1. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
2. `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
3. `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
4. `client/src/main/kotlin/com/ktome/client/assets/RenderSnapshotAssetAudit.kt`
5. `client/src/main/resources/manifests/visual-manifest.json`
6. `game/src/main/resources/data/items/index.yaml`
7. `assets-src/image/specs/*`
8. `assets-src/image/manifests/*`
9. 如需音频 companion：
   - `assets-src/audio/specs/*`
   - `assets-src/audio/manifests/*`
10. `build.gradle.kts`
11. 如需 lint 扩张：
   - `tools/src/test/kotlin/com/ktome/tools/lint/*`

### 10.3 任务拆解

1. 所有可装备物和 special template item 的 `iconKey` 完整覆盖。
2. 建立 `QualityPresentation`：
   - 当前 snapshot 正式轴只直接承载 `qualityTierId = NORMAL / MAGIC / RARE`
   - `specialTemplateId != null` 的 special item 允许叠加 client-local accent，但不在本 PR 发明新的 snapshot 字段
3. 品质色只作用于：
   - 名称
   - 边角标
   - 对比摘要
   - `RarityTier` 决定基础 name color / corner glyph
   - special accent 只作为可选强调，不反向定义第二正式品质轴
4. 地图掉落提示规则：
   - 单件：显示该件物品的 icon
   - 多件：显示 `items.firstOrNull()?.iconKey + 数量 badge`
   - 数量上限：`9+`
5. 当前阶段不引入真实 stack quantity contract。
6. 如果玩家站在掉落格上，掉落提示仍必须可见，不能被角色覆盖到完全消失。
   - 当前推荐方案固定为 actor sprite 上方的 corner marker / badge
   - 不做角色半透明，也不把完整 item icon 直接压到 actor 正中
7. `ItemRenderSnapshot.iconKey` 当前保持 nullable；本 PR 选择 **lint + content coverage 路径** 收口，而不是在 snapshot schema 上强改 nonnull。

**资源计划**

1. 本 PR 是当前 8 个 PR 里第一个必须带正式 image plan 的 PR；建议命名为 `assets-src/image/specs/phase2-uiux-pr04-gemini-plan.yaml`。
2. 必须进入 image plan 的资源只包含：
   - 当前正式可装备物仍缺失的 item icon
   - special template item 需要的正式 icon fallback
   - 地图地面掉落提示确实无法通过现有 icon + token 组合表达的 overlay/marker
3. 品质色、品质边角标、数量 badge 优先用 `UiDesignTokens` 和程序化绘制完成；不要为 `NORMAL / MAGIC / RARE / UNIQUE / ARTIFACT` 分别批量生成装饰图片。
4. 图片交付必须同时包含：
   - generation report：`assets-src/image/manifests/phase2-uiux-pr04-generation-report.jsonl`
   - processing report：`assets-src/image/manifests/phase2-uiux-pr04-processing-report.jsonl`
   - source manifest 与 runtime manifest 的同步更新
   - `build.gradle.kts` 中 `assetLint / styleLint / manifestLint` 的 `--extra-plan`
5. 音频不是本 PR 的默认阻塞项；只有当拾取/掉落提示在当前 `audio.item.*` / `audio.ui.*` 家族里确实无法表达时，才允许补 `assets-src/audio/specs/phase2-uiux-pr04-audio-plan.yaml`，并把范围严格限制在 `pickup / drop / high-value pickup` 级别的最小 cue。

### 10.4 退出条件

1. `contract-lint` 能覆盖可装备物 / special template item 的 `iconKey` 完整性。
2. 当前正式品质档位都能在 UI 上稳定区分。
3. 地图态不再只能靠 sidebar / inspect 才知道脚下或周围有掉落物。
4. 文档和实现都明确区分：
   - 同格多件掉落数量
   - 真正的 item stack quantity
5. special item 的强调如果存在，也明确是 `specialTemplateId` 驱动的 client-local accent，不是假装已经有第二正式品质字段。

### 10.5 推荐验证

```bash
./gradlew contractLint
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest"
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
```

### 10.6 风险与最小切片

风险：

1. content 侧 `iconKey` 补齐量大
2. 把同格掉落数量误写成正式 stack 语义

最小切片：

1. 先完成装备图标完整覆盖
2. 先完成品质展示
3. 地图掉落数量 badge 以 `items.size` 为准，不扩上游 contract

---

## 11. PR-05：内容扩张一致性、共享卡片模型与错误/空态

### 11.1 目标

解决“内容一扩张，UI 又退化回纯文字列表”的问题，同时把事件 / 商店 / 奖励房卡片模型、错误态、空态和加载态正式化。

### 11.2 范围

涉及文件建议：

1. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
2. `client/src/main/kotlin/com/ktome/client/render/RoutePreviewText.kt`
3. `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`
4. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/card/ModalCardModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/state/UiEmptyState.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/state/UiErrorState.kt`
5. 可能涉及：
   - `game/src/main/resources/i18n/*.json`
   - `tools` 侧新增 `content-ui-lint`

### 11.3 任务拆解

1. 统一事件 / 商店 / 奖励房卡片结构。
2. 补齐空态与错误态规范。
3. 冻结最小加载态。
4. 首选扩 `contractLint` / `localeLint` 的现有 owner 面，增加 content-ui 子规则：
   - 新内容必须有 `iconKey`
   - 新 UI 文案必须走 locale
   - 新卡片字段必须满足必填项
   - 只有当现有 lint owner 无法表达跨模块校验时，才新增独立 `contentUiLint`
5. 增加 UI-affecting PR checklist。

**资源计划**

1. 本 PR 默认不做大批量素材扩张，先复用 `PR-04` 已经补齐的 icon 体系与现有 UI cue。
2. 只有当共享卡片模型在 `事件 / 商店 / 奖励房 / 错误态 / 空态 / 加载态` 中缺少正式头图标或状态图标时，才允许增加窄作用域 image plan：`assets-src/image/specs/phase2-uiux-pr05-gemini-plan.yaml`。
3. 该 PR 的图片 scope 仅限共享卡片头部 icon、空态/错误态正式 fallback icon、必要的商店/奖励房标识；禁止引入大幅插画背景或每个事件单独做一张图。
4. 音频同理：只有当前 `audio.ui.*` 不足以区分 `购买成功 / 购买失败 / 打开卡片 / 关键错误` 时，才允许补 `assets-src/audio/specs/phase2-uiux-pr05-audio-plan.yaml`。
5. 若触发 companion 资源计划，必须和 `content-ui-lint` / checklist 一起落地，保证“新增卡片类型 = 新 formal key 已接上 manifest/lint”，而不是事后再补。

### 11.4 退出条件

1. 事件 / 商店 / 奖励房不再各自拼一套不同的行文风格。
2. 常见空态和错误态都有明确回退路径。
3. 内容扩张不会再天然退化成纯文字列表。
4. PR checklist 与 lint 至少落一个正式阻塞面。

### 11.5 推荐验证

```bash
./gradlew localeLint
./gradlew contractLint
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
```

### 11.6 风险与最小切片

风险：

1. `content-ui-lint` 一次收得太严，影响内容开发节奏

最小切片：

1. 先落 PR checklist
2. 再把 lint 从 warning 升为 error

---

## 12. PR-06：状态语义、badge 规则与高风险视觉层级

### 12.1 目标

把状态系统、badge 展示和高风险提示从“已有基础”推进到正式可维护的层级表达。

### 12.2 范围

涉及文件建议：

1. `client/src/main/kotlin/com/ktome/client/ui/status/StatusHudRenderer.kt`
2. `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt`
3. `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt`
4. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
5. 如触发 companion 资源补充：
   - `assets-src/image/specs/*`
   - `assets-src/audio/specs/*`
   - `assets-src/image/manifests/*`
   - `assets-src/audio/manifests/*`
   - `build.gradle.kts`
6. 如需 snapshot 扩张：
   - `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`
   - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`

### 12.3 任务拆解

1. 冻结 status badge 规则：
   - 回合
   - stack/cap
   - 分类
2. 固定高风险 telegraph 与普通状态的视觉权重差。
3. 明确当前阶段的分组策略：
   - 状态继续复用现有 `BUFF / DEBUFF / NEUTRAL`
   - `TELEGRAPH` presentation group 只来自 `snapshot.overlays`
   - `ZONE_EFFECT` presentation group 只来自 `MapCellSnapshot.terrainOverride`
   - 两者都属于 client-local presentation group，而不是新的 status contract
4. 落实 a11y 基线到状态/telegraph 面：
   - 颜色以外的形状或图标区别
   - `Reduce Motion`

**资源计划**

1. 本 PR 很可能需要一批很小的状态/telegraph companion 资源；建议命名：
   - 图片：`assets-src/image/specs/phase3-uiux-pr06-gemini-plan.yaml`
   - 音频：`assets-src/audio/specs/phase3-uiux-pr06-audio-plan.yaml`
2. 图片只服务于重复出现、需要正式 key 的状态/预警标识，例如：
   - 高风险 telegraph marker
   - zone hazard badge
   - 当前 status icon 集无法表达的分类图标
3. 颜色、描边、闪烁节奏、badge 数字仍优先由 token 和 renderer 负责；不要把本应是表现层节奏的问题反向固化成大量静态图片。
4. 音频默认先复用现有 `audio.boss.warning`、`audio.ui.*` 和既有战斗 cue；只有当前高风险预警在听觉层面仍无法形成清晰分离时，才补最小 warning cue 计划。
5. 禁止把 ambience、地图背景音或职业氛围音扩张混进本 PR 的状态/telegraph companion 计划。

### 12.4 退出条件

1. 状态 badge 规则不再随面板不同而漂移。
2. 高风险 telegraph 视觉上明显高于普通状态。
3. 不为了满足 UI 层级而反向污染状态 contract。
4. 色盲 / 高对比度场景下，高风险与普通状态仍可区分。

### 12.5 推荐验证

```bash
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest"
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
```

### 12.6 风险与最小切片

风险：

1. 为了 UI 层级强行改动规则层状态 contract

最小切片：

1. 先固定 status badge 规则和 telegraph 视觉权重
2. presentation group 先在 `client` 内部完成，不要求一次扩 snapshot 枚举

---

## 13. PR-07：动态说明、关键词消费与解释型检视

### 13.1 目标

让关键词、动态说明和构筑解释从现有 contract 真正进入主要阅读路径，但不新造第二套关键词体系。

### 13.2 范围

涉及文件建议：

1. `client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt`
2. `core/src/main/kotlin/com/ktome/core/talent/KeywordRegistry.kt`
3. `core/src/main/kotlin/com/ktome/core/talent/DescriptionModel.kt`
4. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
5. `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`

### 13.3 任务拆解

1. 复用现有 `KeywordRegistry`，不新建第二关键词字典。
2. 扩展 `DescriptionPresenter`，让关键词进入：
   - 检视面板
   - 战斗动作说明
   - 背包 / 商店说明
3. 把动态说明、icon、资源、构筑标签放到同一个解释型检视里。
4. 若需要 lint，优先围绕现有 `KeywordRegistry` 做一致性校验，不新增第二 registry。
5. `Advanced Tooltip` 仍然留在长期候选，不前置到本 PR。

**资源计划**

1. 本 PR 默认不新增音频 plan；关键词消费和解释型检视首先解决的是结构化信息可读性，不是音效丰富度。
2. 图片也默认不批量扩张；优先复用已有 `iconKey`、状态 icon、资源轴 icon 和文本强调样式。
3. 只有当关键词 chip / 构筑标签在多个主阅读面里都需要稳定的正式视觉锚点，且当前 manifest 中不存在可复用 key 时，才允许补 `assets-src/image/specs/phase3-uiux-pr07-gemini-plan.yaml`。
4. 禁止为每个关键词或每条说明单独生成装饰图；本 PR 的目标是统一说明消费入口，而不是做一套新的装饰体系。

### 13.4 退出条件

1. 关键词不再只存在于少数 talent 说明的局部渲染里。
2. `DescriptionPresenter` 成为统一入口，而不是每个 renderer 自己处理关键词。
3. 动态说明已进入主要阅读路径，而不是只能靠 hover 或日志。
4. 不引入第二套 keyword contract。

### 13.5 推荐验证

```bash
./gradlew :client:test --tests "com.ktome.client.ui.talent.DescriptionPresenterTest"
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew localeLint
```

### 13.6 风险与最小切片

风险：

1. 说明文本过长，主阅读路径重新变成文本墙

最小切片：

1. 先统一关键词渲染入口
2. 先让关键词进入检视面板和动作说明
3. advanced tooltip 继续后置

---

## 14. PR-08：telegraph 三位一体与战斗三层决策面

### 14.1 目标

把 `P3-W4` 真正落成可执行的玩家面：

1. 动作层
2. 方式层
3. 目标层

同时完成 telegraph 在地图、目标卡、日志三处的一致化。

### 14.2 范围

涉及文件建议：

1. `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt`
2. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
3. `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
4. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/combat/CombatDecisionPanel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/combat/ActionHintModel.kt`
5. 如触发 companion 资源补充：
   - `assets-src/image/specs/*`
   - `assets-src/audio/specs/*`
   - `assets-src/image/manifests/*`
   - `assets-src/audio/manifests/*`
   - `build.gradle.kts`
6. 如需窄扩 contract：
   - `core/src/main/kotlin/com/ktome/core/combat/CombatResolutionTrace.kt`
   - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`

### 14.3 任务拆解

1. 复用当前 telegraph contract，完成：
   - 地图 overlay
   - 目标卡
   - 日志前缀
   三处一致化
2. 新增战斗三层决策面：
   - 动作层
   - 方式层
   - 目标层
   - 当前实现选择单一 `CombatDecisionFrame` 内部 phase 机，而不是 3 个独立 `ModalStack` 栈帧
3. `ESC / Backspace` 严格服从统一语义：
   - `ESC` 退出到地图
   - `Backspace` 回上一层
4. 若规则层已有可消费 typed hint，则进入 `ActionHintModel`。
5. 当前 PR **不消费普通敌人 intent**：
   - `ActorRenderSnapshot.aiTypeId` 只作为 AI 类型标签，不代表下一步意图
   - scripted / boss telegraph 继续只走 `PendingTelegraphState -> OverlayRenderSnapshot`
   - 若未来规则层暴露普通敌人的单步 intent 字段，再由独立 PR 接入

**资源计划**

1. 本 PR 允许补一批很小的战斗 affordance companion 资源；建议命名：
   - 图片：`assets-src/image/specs/phase3-uiux-pr08-gemini-plan.yaml`
   - 音频：`assets-src/audio/specs/phase3-uiux-pr08-audio-plan.yaml`
2. 图片范围只覆盖多次复用、需要稳定 formal key 的战斗层 UI 锚点，例如：
   - 动作层 icon
   - 方式层区分 icon
   - 目标层锁定/危险标记
3. 音频范围只覆盖三类信号：
   - telegraph 升级或锁定预警
   - 动作确认
   - 非法/禁用提交
4. 现有 `audio.ui.*`、`audio.boss.warning`、战斗基础 cue 足够时，优先复用；不要借本 PR 扩张成 spell-by-spell 的专属音效工程。
5. 若本 PR 引入了新的 combat visual/audio key，必须在同一 PR 完成 plan、manifest、`build.gradle.kts --extra-plan` 与 golden/smoke 收口。

### 14.4 退出条件

1. Boss telegraph 在地图、目标卡、日志三处同 icon / 同主色 / 同语义。
2. 战斗三层决策面可完整走通，不再只是 `UiMode.TARGETING` 的输入态。
3. `ESC / Backspace` 行为稳定。
4. 普通敌人 intent 不作为本 PR 的阻塞项，也不允许在 `client` 先自造一套“伪 intent”。

### 14.5 推荐验证

```bash
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest"
```

若本 PR 扩到了 non-trivial Kotlin contract wiring，再补：

```bash
./gradlew maintainabilityLint
```

### 14.6 风险与最小切片

风险：

1. 把“普通敌人 intent 全量可见”错误上升成当前 PR 的硬前置
2. 战斗三层面板和现有 targeting 流程并存太久，长出兼容垃圾

最小切片：

1. 先完成 Boss / scripted telegraph 的三位一体
2. 先完成三层战斗面
3. 普通敌人 intent 只作为上游 ready 后的附加项

---

## 15. 长期候选与触发条件

这些方向保留，但不进入当前 8 个 PR 的阻塞链：

| 候选 | 前置依赖 | 触发条件 | 说明 |
| --- | --- | --- | --- |
| Look Mode 完整增强 | `PR-03` | 基础 Look Mode 已稳定，检视信息仍不足 | 增加多层 tooltip、更多对象面 |
| Autoexplore / Autotravel | `PR-03` | 路径与 modal 语义稳定；玩家感知重复移动疲劳 | 高 ROI，但不是当前必须 |
| Advanced Tooltip | `PR-07` | 词缀和说明密度显著增加 | 当前不前置 |
| Death Replay / Turn Log | `PR-08` | 玩家开始反馈“死了不知道为什么” | 属于 post-mortem 学习，不是元进度 |
| Tutorial 体系 | `PR-02`, `PR-03` | 首页和局内基础体验稳定后 | 当前不阻塞 |
| Seed / Snapshot Share | `PR-03` | 本地截图/seed 路径稳定 | 偏长期 |
| UX telemetry / 行为统计 | 全链稳定后 | 玩家规模扩大或出现实际运营需求 | 当前短期内明确后置 |

---

## 16. 全局验证纪律

### 16.1 每个 PR 的最小验证集合

1. `localeLint`
2. `contractLint`
3. 该 PR 直接命中的 `:client:test --tests "..."`
4. `ClientSmokeHarnessTest`
5. `GoldenScreenshotHarnessTest`
6. 若命中 non-trivial Kotlin 结构/边界或治理接线，再补 `maintainabilityLint`

### 16.2 golden 管理要求

1. 当前仓库的 `goldenScreenshot` 正式路径仍以 `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt` 的**哈希断言场景**为准，不额外发明第二套目录制 baseline。
2. 命中本计划的 UI 变更场景，golden label / save-folder-name 统一使用 `uiux-prNN-*` 前缀，避免继续增长模糊命名。
3. 当前正式 screenshot harness 的固定分辨率基线是 `1280x800`，除非整仓显式重录，否则本文档不再写第二个分辨率真相。
4. 双语至少覆盖：
   - `zh-CN`
   - `en-US`

### 16.3 UI-affecting PR checklist

所有命中当前计划的实现 PR，默认至少核对：

1. 新增 UI 状态已被 smoke 覆盖
2. 新增 UI 状态已被 golden 覆盖
3. 新增内容已有 `iconKey`
4. 新增 UI 文案走 locale
5. 新增 UI 组件使用 token，而不是局部硬编码
6. 新增键位行为符合统一语义表
7. 新增 disabled 行为有禁用原因

### 16.4 a11y / 性能验收基线

每个命中可视面的 PR 都必须至少人工复核：

1. 键盘完整可达
2. 焦点可见
3. 高对比度 / 色盲回退可读
4. 没有新增不必要的常驻动效
5. 密集场景下没有明显掉帧或输入延迟恶化

### 16.5 资源管线验收基线

凡是当前计划中的 PR 触发了 companion 图片或音频补充，必须额外满足以下条件：

1. 新 image plan 已经落到 `assets-src/image/specs/`，并且命名、report、manifest 路径都按当前 PR 收口。
2. 新 audio plan 已经落到 `assets-src/audio/specs/`，如果用了 raw 生成脚本或窄作用域导入，也要把 generation / processing 输入范围写清楚。
3. `build.gradle.kts` 已把该 PR 的 image/audio plan 接到对应 `--extra-plan`，避免 root `assetLint / styleLint / audioLint / manifestLint` 仍然漏检。
4. `python3 scripts/sync_phase2_manifests.py` 已被纳入该 PR 的资源交付流程，source manifest 与 runtime manifest 不允许继续漂移。
5. 图片 companion 资源至少通过：`./gradlew assetLint`、`./gradlew styleLint`、`./gradlew manifestLint`。
6. 音频 companion 资源至少通过：`./gradlew audioLint`；若音频 plan 同时引入新的 runtime path 或影响 manifest 对齐，再补 `./gradlew manifestLint`。
7. 新资源 key 已被 `ClientSmokeHarnessTest` 和命中的 golden 场景消费到，而不是只存在于 plan / manifest 中无人使用。
8. 若资源因外部前置条件暂时未生成，PR 中必须明确：
   - 哪些 key 仍走 fallback
   - 为什么当前 fallback 仍可用
   - 真正开放该 UI 路径前还缺哪一步资源交付

---

## 17. 风险与对策

### 17.1 review 建议再次变成第二 contract

风险：

1. 把 review 里的具体命名直接升级成实现合同

对策：

1. 关键词复用现有 `KeywordRegistry`
2. telegraph 复用现有 `PendingTelegraphState -> OverlayRenderSnapshot -> TelegraphRenderer`
3. 当前计划先扩现有 contract，不新建平行家族

### 17.2 `InputHandler` 状态继续膨胀

风险：

1. W5b / W8 都可能继续给 `InputHandler` 塞特例

对策：

1. 先冻结统一输入语义
2. 先抽 `ModalStack`
3. 所有新 mode 都必须解释与根地图态的关系

### 17.3 品质和掉落语义越界

风险：

1. `client` 先发明新品质档位
2. 同格多件掉落被错误当成真正 stack

对策：

1. 当前正式品质只认现有 contract
2. 多件掉落数量 badge 只认 `MapCellSnapshot.items.size`

### 17.4 golden 范围失控

风险：

1. PR 切分不清导致每次都要重拍整套 golden

对策：

1. golden 统一按 `uiux-prNN-*` 场景标签和 save-folder 前缀切分
2. 每个 PR 只承担自己直接触碰的界面集合

### 17.5 telemetry scope creep

风险：

1. 开发期间又把用户行为统计、长期观察口塞回主线

对策：

1. telemetry 明确放到长期候选
2. 当前阶段只保留开发调试所需 artifact

---

## 18. 完成后才能进入下一组工作

以下条件全部满足，本文档才算进入“可执行状态”：

1. 当前执行计划被视为 `docs/opt` 下的 UI/UX 执行真源。
2. 原设计文档继续保留为体验/原则真源，不再混用。
3. 后续实现默认按 `PR-01 -> PR-08` 串行推进。
4. 当前 PR 的：
   - 目标
   - 任务
   - 退出条件
   - 验证
   - 风险
   已完成收口后，才进入下一 PR。
5. 长期候选不再以“顺手一起做”的方式混入当前阶段 PR。

一句话原则：

**先把单人玩家当前最直接能感知到的 UI/UX 面做对，再考虑后置的统计、分享和长周期观察能力。**

# Phase 4 UI/UX PR 级开发文档 Review 报告

**评审日期**：2026-04-21
**评审范围**：`docs/opt/ui-pr/` 下 5 份 PR 开发文档（`phase4-uiux-pr01` ~ `phase4-uiux-pr05`）
**交叉参照**：`docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`、`docs/opt/ui-pr/README.md`

---

## 0. 总体结论

1. 5 份 PR 文档整体把源计划的 8 包压成 5 包，合并逻辑基本合理：`PR-01` 合源 PR-01+02，`PR-03` 合源 PR-04+05，`PR-04` 合源 PR-06+07，这三次合并都解决了重复改 `MainMenuScreen` / visual manifest / status renderer 的重复工时。
2. 所有 PR 的 §1 完成标准和 §8 出口门禁基本对齐源计划的 Exit 条件，没有发现整段丢失的原始要求。真正的风险集中在两类：
   - **跨 PR 冻结条款的语义冲突**：`PR-02` 的 `INSPECT + Backspace -> MAP` 与 `PR-04` `ExplainPane` 打开时 `Backspace -> 关闭 ExplainPane` 是潜在矛盾。
   - **跨 PR 延期落地的 TODO 没有显式 checklist**：`BuildInfo.shortHash` 在 `PR-01` 用 `unknown` 占位、`PR-03` 补完；`ITEM_COMPARE / COMBAT_DECISION` 在 `PR-02` 是 deferred stub、`PR-05` 真正实装。这些跨 PR 悬挂项没有统一追踪清单。
3. 所有 PR 的人工白盒流程都以 `docs/opt/ui-pr/manual-records/` 为记录目录，但仓库里还没有模板 / schema，一线实施时记录格式会漂移。
4. 部分文档写法沿用"前置文档全量重读"的模式（前置列 6~7 个），但没把上游文档"哪些条款是本 PR 的硬依赖"抽出来作为单独 checklist，对实施者的阅读成本偏高。

下面按"通用问题 → 每份 PR 具体问题 → 跨 PR 交叉风险 → 优先级建议"四段给出改进意见。

---

## 1. 通用问题（跨所有 PR 共性）

### 1.1 跨 PR 延期项缺乏统一追踪

- `PR-01` §4.4 预留 `[ktome/<build-hash>]` 占位，把正式注入下放给 `PR-03`，但没有在任一文档中维护一份显式的 "跨 PR deferred" 清单。
- `PR-02` §4.1 规定 `ITEM_COMPARE / COMBAT_DECISION` 以 `ModalFrame.kind` 形式先行落地，真正消费面在 `PR-03` (`ModalCardModel`) 和 `PR-05` (`CombatDecisionFrame`)。
- 建议在 `docs/opt/ui-pr/README.md` 增加一张 `跨 PR 延期项与收口 PR` 表，最少包含：条目、首次引入 PR、收口 PR、收口前的临时实现形态、验收方式。否则存在"某个 deferred stub 在 PR-02 落完没人再回收"的风险。

### 1.2 人工白盒记录模板缺失

- 每份 PR §6.3 都把记录路径指向 `docs/opt/ui-pr/manual-records/phase4-uiux-prNN-*.md`，但仓库当前没有该目录，也没有记录模板。
- 风险：不同实施者会写出不同 schema 的记录，后续审查、对比、回溯困难。
- 建议：在本次评审修复动作中，先补一个 `docs/opt/ui-pr/manual-records/_template.md`，最小包含 `环境 / 输入序列 / 预期行为 / 实际行为 / 证据路径 / 签收结论` 六字段，并在 README 里固定引用路径。

### 1.3 "若触发 X，再补 Y" 的条件判定口径不够硬

- 多份 PR（PR-01 §6.2、PR-03 §6.2、PR-05 §6.2）写 `若命中 non-trivial Kotlin 结构重排，再补 maintainabilityLint`，但 "non-trivial" 没有可操作标准，容易被绕过。
- 建议换成 `若新增/删除/移动的 Kotlin 文件数 ≥ 5，或修改的公共 sealed/interface 类型 ≥ 2，必须补 maintainabilityLint`，把判定交给代码变更量而非实施者主观判断。

### 1.4 locale key 命名未显式列出

- `force-switch`（PR-02）、`save.blocked-in-combat-decision`（PR-05）、`continue.corrupted.detail`（PR-01）、`combat.no-legal-target`（PR-05）、`combat.illegal-target`（未列）—— 这些 key 散落在各 PR §4 中，但没有集中清单。
- localeLint 不会替我们检查"命名风格一致"。建议每份 PR 在 §4 末尾补一小节 `新增 locale key 列表`，至少写 `key / 所属面 / 示例文本`。

### 1.5 前置 PR 的硬依赖条款未抽出

- 每份 PR 顶部的 `执行前必须先完整阅读并接受`段列了 6~7 个文档。这属于阅读义务，但对"本 PR 真正依赖上游哪几条冻结"没有做抽取。
- 建议每份 PR 在 `前置条件` 下面增加 `硬依赖条款` 子列表，例如 `PR-02` 依赖 `PR-01 §4.1 color.focus.ring token`、`PR-01 §4.3 MainMenuSummaryModel 字段`。

### 1.6 a11y 控件来源不清

- `PR-04` §6.3 的人工白盒要求"打开高对比/色盲回退/Reduce Motion 或等价开发开关"，但这些开关由哪个 PR 落没有在任一文档中写明。源 §5.5 a11y 基线把字号档位、Reduce Motion 都列为"当前阶段必须落"，但 5 份 PR 里没有任何一个显式认领。
- 建议：在 `PR-01` 或 `PR-04` 中显式增加 a11y 控件的最小面（只要求开关/切换接口和 token 接入），否则 `PR-04 §6.3` 的人工验证会流于形式。

### 1.7 "skipped golden 不能替代人工白盒" 的重复表述

- `PR-01~05 §6.4` 都写了几乎相同的一句话。可以抽到 README §执行纪律 里一次性声明，PR 内只引用不复述，减少维护面。

---

## 2. 每份 PR 的具体不足与改进意见

### 2.1 PR-01（Client Foundation and Main Menu）

#### 2.1.1 范围过载与估时
- §1 完成标准 6 项横跨 token / layout strategy / MainMenu 三态 / DesktopLauncher 标题 / Telegraph danger 色 / 人工证据。源计划估时 `2+1=3 人日`，合并后工作量和耦合上限偏高。
- 建议：在 §3 中把 "token 基础 + layout strategy" 与 "MainMenu 三态" 显式分成两个可并行子包，允许实施者分成两次 commit 提交，但仍合成同一个 PR 发起评审。文档里只需写 `可拆成两阶段提交`，避免合并后整块提交难以 review。

#### 2.1.2 `BuildInfo.shortHash` 跨 PR 占位的冲突
- §4.4 一边要求 `UiErrorPayload` 必须含 `[ktome/<build-hash>]`，一边声明 `BuildInfo.shortHash` 正式注入留给 `PR-03` —— 这意味着 `PR-01` 必须硬编码 `[ktome/unknown]` 或挤一个临时 string 常量。
- 后续风险：`PR-01` 的 `MainMenuScreenTextTest`、`Copy Error Detail` 相关测试如果 assert `unknown`，`PR-03` 改正时会变成"修改既有断言"而不是"补齐缺口"。
- 建议二选一：
  1. 把 `BuildInfo.shortHash` 前置到 `PR-01` 基础设施里，避免 occupier/replacer 两次触碰。
  2. 在 `PR-01` 测试里把 `build-hash` 字段留作通配/不断言，`PR-03` 再补精确断言；并在文档里显式注明这一约定。

#### 2.1.3 `DesktopLauncher` 标题 `<save-slot>` 字段的数据来源未定义
- §2.1 口径固定为 `K-ToME · <locale> · <seed>[· <save-slot>]`，但 save-slot 何时由谁回写 launcher 标题没有说明。若由会话线程异步更新 Swing/LWJGL title，存在线程安全和冷启动闪烁风险。
- 建议在 §4 或 §5.4 补一段 `标题更新入口`，至少写：更新调用点（比如 `FoundationGameScreen.onSessionLoaded`）、降级路径（seed 不可得 -> 省略；save-slot 不可得 -> 省略；release 构建无法访问会话信息 -> 回退 `K-ToME`）、单测建议用纯 formatter（如 `DesktopLauncherTitleFormatterTest`）覆盖。

#### 2.1.4 `Continue UNAVAILABLE` 子原因未枚举
- §2.1 定义三态 `AVAILABLE / ABSENT / UNAVAILABLE(reasonKey, copyPayload)`，但 `UNAVAILABLE` 子原因（save 版本不兼容、save IO 失败、save schema 损坏等）没有列出。
- 文档只给出 `ui.menu.continue.corrupted.detail` 一个 locale key，不足以覆盖不同 reasonCode。建议显式列出 `reasonCode` 枚举（`CORRUPTED / VERSION_MISMATCH / IO_ERROR / SCHEMA_MISMATCH`）和对应 locale key 前缀。

#### 2.1.5 `StatusHudRenderer` 首批消费 token 的清单不完整
- §5.3 只说 "保持现有语义，只把颜色/徽标入口收敛到 token"。但具体哪些 `when` 分支 / `Color.*` 裸值要迁移，没有列表。
- 建议在 §5.3 用一张 "当前硬编码颜色 → 目标 token" 小表（至少覆盖 `statusAccentColor / statusBadgeColor / danger / warning / buff / debuff`），否则实施者极易漏掉一两个分支，下一 PR 追加补丁。

#### 2.1.6 单测覆盖未列 `MainMenuFocusPolicyTest`
- §3.1 测试列表里有 `MainMenuControllerTest`，但 `MainMenuFocusPolicy` 是独立新增类，焦点决策是 pure function 性质，应该独立单测。现只通过 controller 集成测试覆盖，回归成本高。
- 建议在 §3.1 测试列表显式补 `MainMenuFocusPolicyTest`。

#### 2.1.7 窗口标题与 Look Mode 帮助 overlay 的边界
- §5.4 "开发态键位说明只能出现在首页帮助区、局内帮助 overlay 或 validation 面"。但 `局内帮助 overlay` 的 owner 目前没有；`PR-02` 的 `?` 键语义尚未冻结到帮助 overlay。
- 建议显式交叉引用 `PR-02 §4.3 MAP: ? 行为待冻结`，避免 `PR-01` 误把开发态键位说明写进 desktop launcher 后 `PR-02` 又要反向回写。

### 2.2 PR-02（In-Game Info, Input, Modal, Look）

#### 2.2.1 Ctrl+S 未出现在 `InputHandlerTest` truth table
- §4.3 truth table 10 个 mode 全部未列 `Ctrl+S`；但 §5.1 "`Ctrl+S` 在 map/modal 保存并保持上下文；targeting/combat decision 按文档阻断或预留阻断点"要求实现该语义。
- 建议：
  - 把 `Ctrl+S` 作为独立行加入 `MAP / INVENTORY / LOADOUT_EDIT / TARGETING / VALIDATION` 各 mode。
  - 明确 "保持上下文" 是否意味着保存后 `ModalStack` 不变；若不变，需要在测试里断言 `modalStack.snapshot() == before`。

#### 2.2.2 `deferred ITEM_COMPARE / COMBAT_DECISION` 的测试 schema 没规定
- §4.1 "`ITEM_COMPARE` 可先 no-op，但必须有 `ModalStackTest` 或 `InputHandlerTest` 覆盖深度"。但"no-op"的期望行为没写：push 时是否真的入栈？pop 时 state 是什么？
- 建议显式声明：`ITEM_COMPARE / COMBAT_DECISION` 在 `PR-02` 阶段 push 时仍要占一层栈深度、`render` 返回空视图、`pollCommand` 对所有键返回 "no-op"，并写对应 assertion 模板。

#### 2.2.3 `PaneFocusController` 状态与被动态切换的交互
- §4.2 定义 3 个锚点 `WORLD / CONTEXT / CHARACTER_ACTION`，modal 打开暂停地图锚点、关闭恢复。
- 但 §4.5 被动态接管清空 active stack 回 `MAP` 时，`PaneFocusController` 的地图锚点焦点应该停在哪个位置？强制回到 `WORLD`？保持 stack 打开前的值？
- 建议补一条：被动态接管时锚点默认回 `WORLD`，因为玩家随后要处理 world map/shop，这样避免 route 选择时焦点却停在角色动作面。

#### 2.2.4 `F` 键在 MAP 与 overlay 的语义描述有误导
- §2.1 / §5.1 写 "`F` 只保留 legacy close alias"，但 §4.3 truth table 里 `MAP + F = 无`、`INVENTORY + F = close`。整体语义正确（legacy close 仅在 overlay 生效），但在一级表述里容易被读成"F 不再有任何 close 能力"。
- 建议在 §2.1 把"legacy close alias"加定语：`F 只在 overlay/modal 内作为 close 兼容别名；在 MAP 根态为 no-op`。

#### 2.2.5 `ui.message.force-switch.*` 的子 key 未列
- §4.5 / §5.3 都引用了通配 `force-switch.*`，但 `WORLD_MAP / SHOP / STAT_ASSIGN` 三种被动态是对应三个独立 key 还是共享一个 + 参数化？
- 建议显式声明：`ui.message.force-switch.world-map / shop / stat-assign`；参数化位置用命名占位（比如 `{reason}`）。

#### 2.2.6 focus ring token 未显式引用
- §5.2 "对地图三类焦点锚点绘制可见 focus ring"。PR-01 提供了 `color.focus.ring`，但 PR-02 文档没显式引用该 token。
- 建议改为 `绘制 focus ring，颜色/宽度消费 UiDesignTokens.color.focus.ring / focus.ring.width`，强制复用。

#### 2.2.7 Look Mode 空态文案来源
- §4.4 "空地显示空态，不显示 `null` 或裸 key"。`UiEmptyState` 类型在 `PR-03` §3.1 才新增；`PR-02` 阶段空态文案放在哪里？直接 hardcode locale key？
- 建议显式声明：`PR-02` 阶段允许使用临时 `RenderTextTokenSnapshot("ui.inspect.empty.tile")`，`PR-03` 上 `UiEmptyState` 后再迁移；该迁移必须作为 `PR-03` 的 task。

#### 2.2.8 Ctrl+S 的 blocked toast key 未在 PR-02 冻结
- §2.1 "`Ctrl+S` 是系统级保存；战斗决策中阻断的完整路径由 PR-05 消费本 PR 预留点"。但 blocked toast 所需的 locale key `ui.message.save.blocked-in-combat-decision`（见 `PR-05 §2.1`）不在本 PR 出现。
- 如果 `PR-02` 已经为 combat decision 预留 frame kind，`Ctrl+S` 在该 frame 上的阻断是不是也能在 `PR-02` 一起冻结？否则 `PR-05` 会回来改 `InputHandler` 的 `Ctrl+S` 分支。
- 建议：把 "`Ctrl+S` 在 `COMBAT_DECISION` frame 上 no-op 并发出 toast"的 test case 在 `PR-02` 先落 test stub（toast 消息为空），`PR-05` 再补 locale key 和 actual message。

### 2.3 PR-03（Item, Content Presentation, UI States）

#### 2.3.1 `ItemIconKeyCoverageRule` vs `ContentUiLintRule` 的优先级写反
- §3.1 "新增" 列表里并列列出 `ItemIconKeyCoverageRule / ContentUiLintRule`，看起来要一次新增两个 lint。但 §4.5 明确写 "优先扩现有 `contractLint / localeLint` owner 面；只有跨模块校验无法表达时才新增 `contentUiLint`"，与源 §10.3 + §11.3 一致。
- 问题：§3.1 的措辞会引导实施者默认去新增 `ContentUiLintRule`，与 §4.5 的"最后才允许"相矛盾。
- 建议把 §3.1 的 `ContentUiLintRule` 改写为 `若 §4.5 判定现有 owner 无法表达，再新增 tools 侧 ContentUiLintRule`。

#### 2.3.2 `UiErrorPayload` 的 contextKeyValuePairs 顺序未定义
- §4.4 字段列表是一个 list，但没有规定 key 的排序规则（插入序 / 字典序 / 按重要度）。`Copy Error Detail` 的 payload 稳定性关系到 issue report 的一致性，也会影响 `PR-03 §6.1` 和后续回归测试 assert 的方式。
- 建议显式声明：`contextKeyValuePairs` 按 builder 的插入序输出；测试通过固定插入序断言输出。

#### 2.3.3 `BuildInfo.shortHash` 注入失败的可观测性
- §4.4 "失败回退 `unknown`"，但没有规定是否打 warn 日志 / 产生 `ResourceFallbackAudit` 条目。在 CI tarball 构建下静默 `unknown` 会使故障不可观测。
- 建议：失败时写入 `client` 启动日志一条 `BuildInfo.shortHash resolution failed, fell back to 'unknown'` 的 warn，并在 smoke 阶段可通过 log tail 抓取。

#### 2.3.4 `GroundLootMarkerModel` head item 排序的 `qualityTierId` 倒序映射歧义
- §4.2 head item 排序规则 `RARE > MAGIC > NORMAL`，直觉正确，但 snapshot 的 `qualityTierId` 是字符串 id，不是枚举；实现者需要知道稳定映射表。
- 建议补一张 `qualityTierId 字符串 -> 排序权重` 的映射表（`normal=0 / magic=1 / rare=2`），并指定 client-local 常量所在位置；若规则层已有，直接引用。

#### 2.3.5 `ModalCardModel` 的 action key 枚举未列
- §4.3 `primaryActionKey / secondaryActionKey: String` 没定义允许的值；事件/商店/奖励房/ExplainPane 都要共用，值集不收敛会导致各 card 自造 action key 字符串。
- 建议改成 `primaryAction / secondaryAction: ModalCardAction`，其中 `ModalCardAction` 是 sealed enum（`Confirm / Cancel / Buy / Sell / EnterRoute / ReadMore / Close` 等），统一收口。

#### 2.3.6 `UiEmptyState / UiLoadingState` 字段未定义
- §3.1 只写新增文件，§4 没给字段。`UiEmptyState` 至少需要 `titleKey / detailKey / primaryCtaKey?`；`UiLoadingState` 至少需要 `messageKey / showsSpinner: Boolean / allowsCancel: Boolean`。
- 建议在 §4 补一小段 `空态 / 加载态 / 错误态 model shape`，明确字段和用例。

#### 2.3.7 加载态 hard cap 没有可测指标
- §2.1 "加载态只允许短时过渡，不得形成长期输入遮罩"，源 §5.4 给了 `~200ms` 参考。本文档未复述，测试也没有 hard cap assertion。
- 建议至少在 `ClientSmokeHarnessTest` 加一个 `loading screen transitions within 500ms` 的软断言，`PR-05` 再根据实际情况收紧。

#### 2.3.8 `iconKey` 不可解析的测试入口
- §4.5 阻塞规则含 "icon 不可解析"，但 §8 出口门禁只写 "item icon coverage / content UI lint 阻塞缺失"。
- 建议在 §8 增加一条 "`ItemIconKeyCoverageRule`（或 `contractLint` 新分支）能覆盖：`iconKey` 不能在 `visual-manifest.json` 中解析时 lint error"。

#### 2.3.9 audio plan 的完整报告产物缺省
- §3.1 可选 `assets-src/audio/specs/phase4-uiux-pr03-audio-plan.yaml`，但对应 `phase4-uiux-pr03-audio-generation-report.jsonl` / `-audio-processing-report.jsonl` 未列。
- 若触发 audio plan，会在资源 lint 和 `sync_phase2_manifests` 阶段暴露漏步。建议把 audio plan 对应的 3 个文件（plan / generation / processing）写全。

#### 2.3.10 人工白盒 preset `LOOT_LAB / seed 20260413` 是否已存在
- §6.3 引用 validation preset `LOOT_LAB`，但仓库是否已有该 preset 未验证。若不存在，`PR-03` 需要顺手落 preset，但 §3.1 没列对应范围文件。
- 建议：显式在 §3.1 加一条 "如 `LOOT_LAB` preset 缺失，则在 `tools/src/main/kotlin/.../validation/...` 下补齐，并在 fixtures 中加入该 preset seed 20260413 的期望断言"。

### 2.4 PR-04（Status, Description, Readability）

#### 2.4.1 `ExplainPane + Backspace` 与 PR-02 truth table 的冲突
- PR-02 §4.3 `INSPECT + Backspace -> 回 MAP`。
- PR-04 §4.4 "`Backspace` 关闭 ExplainPane 或回上一 inspect sub-view；`ESC` 仍全退"。
- 两者在 ExplainPane 已打开的场景下行为不一致。
- 建议：在 `PR-04 §4.4` 显式声明需要修订 `PR-02 §4.3` 对应行为为 "若 ExplainPane 已打开，`Backspace` 关闭 ExplainPane；否则按 PR-02 冻结行为"，并在 `InputHandlerTest` 里新增 case 覆盖这一优先级。同时在 `PR-02` 文档上补 forward-compatible note。

#### 2.4.2 `StatusPresentationModel.priority` 计算规则未定义
- §4.1 给了 `priority: Int` 字段，但计算来源没规定。是按 `danger level > remainingTurns > stackCount`，还是按分类优先（DEBUFF > BUFF > NEUTRAL）？
- 这个字段直接决定 `renderCompact(...)` 的排序和 badge 权重，是 `PR-04` 可读性目标的核心。
- 建议在 §4.1 补一张 priority 计算矩阵（分类权重 + telegraph danger 叠加 + 剩余回合倒序），或明确给出一个 pure function 签名让 `StatusPresentationModelTest` 可覆盖。

#### 2.4.3 `StatusPresentationGroup.TELEGRAPH / ZONE_EFFECT` 与 PR-05 `TelegraphPresentationModel` 的边界
- PR-04 把 `TELEGRAPH` 作为 `StatusPresentationGroup` 的一项，来源是 `overlay`。
- PR-05 §4.1 又抽 `TelegraphPresentationModel`，服务地图 overlay / 目标卡 / 日志前缀。
- 两者在字段上（icon / danger / preview turns / cells）可能重复建模。如果 `PR-04` 先落一个 group，`PR-05` 再落一个独立 presentation model，会产生两套 "telegraph 视图"。
- 建议：在 `PR-04 §4.1` 显式声明 `StatusPresentationModel` 里的 `TELEGRAPH` 分支只承载 "HUD/目标卡 compact 入口所需的最小字段（typeId / nameKey / iconKey / badge / danger priority）"，而地图 overlay / 日志前缀所需的完整字段由 `PR-05 TelegraphPresentationModel` 负责；或把 compact 视图作为 `TelegraphPresentationModel.toStatusPresentation()` 的投影，避免两套建模。

#### 2.4.4 `keywordRegistryLint` 是可选，但"唯一 authority"如何强制？
- §4.5 明示 lint 是可选；§8 出口门禁写 "`KeywordRegistry` 仍是唯一 keyword authority"，但如果不新增 lint，靠什么防止后人新建第二 registry？
- 建议二选一：
  1. 把 `keywordRegistryLint` 从可选改为必须（至少"若 DescriptionPresenter 实际消费 keyword id 则必须开启 lint"）。
  2. 在 PR description / CODEOWNERS / 评审 checklist 里显式要求 reviewer 检查"未新增第二 keyword 家族"，并落 checklist 条目。

#### 2.4.5 a11y 开关落地未认领
- §6.3 4. a11y 要求打开 "高对比/色盲回退/Reduce Motion 或等价开发开关"。但 5 份 PR 中没有一份显式落 a11y 控件入口。
- 建议：在 `PR-04 §3.1` 追加 `client/src/main/kotlin/com/ktome/client/ui/settings/AccessibilityToggle.kt` 最小面（即便只是一个 env flag / 启动参数），否则 `PR-04 §6.3.4` 无法真正执行。

#### 2.4.6 `DescriptionPresenter` combat action 入口的依赖倒挂
- §4.3 扩展入口 6 个，其中 `combat action 说明` 在 `PR-05 CombatDecisionPanel` 才有真正消费方。
- 风险：`PR-04` 落 presenter + 单元测试，但 combat action 维度只能用 fixture，端到端覆盖要等 `PR-05`。若 `PR-04` 没在 exit 条件里声明这一点，容易被误认为已经完备。
- 建议在 `§8` 出口门禁显式声明："`DescriptionPresenter` 的 combat action 分支在 `PR-04` 只做 pure unit test；端到端覆盖由 `PR-05` `CombatDecisionPanel` 落地时补充"，并在 `PR-05` 的 §3 反向登记为引入项。

#### 2.4.7 `StatusPresentationModel` 字段 `badgeText: String` 的 locale 义务
- §4.1 `badgeText: String` 示意是已渲染的 UI 文本；但 `PR-04` 同时要求 HUD/目标卡/inspect 三面一致。如果 badge 文本直接是字符串，则必须在 builder 内统一做 locale 替换，不能留到 renderer 各自处理。
- 建议：字段改为 `badge: RenderTextTokenSnapshot`，或显式规定 `badgeText` 由单一 builder 产生（例如 `StatusPresentationBuilder.formatBadge(...)`）。

### 2.5 PR-05（Telegraph and Combat Decision Surface）

#### 2.5.1 `TARGET + Backspace` 在 "单一方式跳过 METHOD" 场景的回退目标
- §4.2 状态图：`若 action.methodOptions.size == 1 -> TARGET`、`TARGET + Backspace -> METHOD 或 ACTION`。
- 这里 "或" 让实现者产生二义：若 TARGET 是从被跳过的 METHOD phase 进来，Backspace 应回 `METHOD` 还是 `ACTION`？
- 合理做法是：若跳过过 METHOD，Backspace 直接回 `ACTION`。否则 UX 上会弹出一个空 METHOD 列表，玩家无法再前进。
- 建议：把 §4.2 `TARGET` 的 `Backspace` 行为细化为 "`Backspace -> 若来自单一方式跳过路径，则回 ACTION；否则回 METHOD`"，并在 `InputHandlerTest` 里加 2 个对应 case。

#### 2.5.2 "非法目标" vs "无合法目标" 的反馈路径
- §6.1 必测行为 5. "非法动作" 与 "无合法目标" 是两条不同路径，但状态图 §4.2 只处理 `legalTargets.isEmpty()` 无合法目标 + toast `combat.no-legal-target`。
- 玩家在合法 target 列表存在但选中了非法位置（例如射程外的 tile），如何反馈？是保持 TARGET phase + 另一个 toast `combat.illegal-target`？还是 reject + 振动/声效？
- 建议在 §4.2 补一条独立分支 `TARGET + 尝试确认非法位置 -> 保持 phase + toast ui.message.combat.illegal-target`，并在 locale key 列表中登记。

#### 2.5.3 `CombatResolutionTrace` 窄扩 contract 的条件不明
- §3.1 "如需窄扩 contract" 列 `CombatResolutionTrace.kt / FoundationGameSession.kt`。但 "如需" 的触发条件（比如"TelegraphPresentationModel 要显示 resolution hint 但 snapshot 不具备"）没定义。
- 建议：要么在 §4 补一段 "当前 PR 的 TelegraphPresentationModel / ActionHintModel 已能用现有 snapshot 覆盖的字段清单 -> 不触发窄扩"；要么显式要求 "若要扩 contract，必须先回到 phase4 roadmap 更新 owner"（实际上 §6.4 已经提，但没连回 §3.1）。

#### 2.5.4 `ActionHintModel` 的字段 schema 未定义
- §4.3 只说 "聚合 typed hint、禁用原因、resource/cooldown/range"。
- 下游 `CombatDecisionPanel` 要消费这些字段来渲染 ACTION / METHOD / TARGET phase 的 hint 区。字段不明确会导致 panel 反过来推字段。
- 建议补最小字段：`availability / resourceCosts / cooldownTurns / rangeSummary / legalTargetCount / disabledReason / telegraphLinkage`，并给出 pure builder 签名。

#### 2.5.5 `Ctrl+S` blocked 的 locale key 和 toast owner
- §4.4 "`Ctrl+S` 在 frame 内阻断并 toast `ui.message.save.blocked-in-combat-decision`"。
- §8 出口门禁没有显式要求 "localeLint 覆盖该 key"；`PR-04` 也没有统一的 toast surface。
- 建议在 §8 出口门禁补一条 "新增 locale key（至少 `ui.message.combat.no-legal-target / illegal-target / ui.message.save.blocked-in-combat-decision`）已进入 zh-CN 及 en-US"。

#### 2.5.6 "普通敌人 intent 没有被伪造" 的人工验证标准过弱
- §6.3 6 写 "确认普通敌人没有显示伪 intent；`ActorRenderSnapshot.aiTypeId` 只作为 AI 类型标签"，但没给出可操作判定。
- 建议改为可勾选项：
  - 目标卡中不出现 "下一步:" / "预测:" / "即将:" 等前缀。
  - 日志前缀不含 boss telegraph 以外来源的 AI 动作预测。
  - `AsciiRenderModel / TileRenderModel` 中搜索 `aiTypeId` 的消费点只在"类型标签"一处。

#### 2.5.7 telegraph 三位一体所有权与 PR-04 的字段边界未交叉
- §5.1 `TelegraphPresentationModel` 放 `client/telegraph/`；但 PR-04 `StatusPresentationModel.group == TELEGRAPH` 也承载 HUD 入口。
- 建议与 2.4.3 对齐：两份 model 的字段边界需要文档层面显式写一次"`TelegraphPresentationModel` 是完整视图，`StatusPresentationModel[group=TELEGRAPH]` 是其 compact 投影"，避免 combat panel 与 status HUD 用两套数据 source-of-truth。

#### 2.5.8 `Space` 在 phase 内是否等价 `Enter`
- §4.2 `CombatDecisionFrame` 状态图只写 `Enter`；`Tab` 表没写 `Space`。但源 §5.2 "`Space` 等价 `Enter`"。
- 若 `Space` 在 combat decision phase 不等价，会和 `PR-02` `MAP + Space -> wait` 以及 `TARGETING + Space -> 确认目标` 的语义漂移冲突。
- 建议：§4.2 状态图显式加 `Space = Enter` 的等价声明；或把 phase 键位表 §4.2 表格第三列 "Tab" 改成 "Tab / Space 确认" 区分。

#### 2.5.9 golden label 前缀复用
- §5.4 要求 golden label `phase4-uiux-pr05-*`。但 `PR-02` 已为 deferred `COMBAT_DECISION` frame 留了接入点；若 `PR-02` 曾录了 `phase4-uiux-pr02-combat-decision-stub` 之类 label，`PR-05` 需显式声明 "`PR-05` 上线时删除该 stub label 并改为 `phase4-uiux-pr05-*`"。
- 建议：在 `PR-05 §8` 出口门禁增加 "停止 `PR-02` 遗留的任何 combat-decision-stub golden label 并重录"。

---

## 3. 跨 PR 交叉风险

| # | 风险条目 | 涉及 PR | 建议动作 |
| --- | --- | --- | --- |
| 1 | `INSPECT + Backspace` 在 ExplainPane 打开态下的语义不一致 | PR-02 §4.3 vs PR-04 §4.4 | `PR-04` 交付时同步修订 `PR-02` 文档的 truth table，加入 "ExplainPane 打开优先级"一条 |
| 2 | `BuildInfo.shortHash` 跨 PR 占位 | PR-01 §4.4, PR-03 §4.4 | 要么把 `BuildInfo` 前置到 PR-01；要么 PR-01 测试禁止 assert `unknown` 文本 |
| 3 | `telegraph` 视图双重建模 | PR-04 §4.1 vs PR-05 §4.1 | 把 `StatusPresentationModel[group=TELEGRAPH]` 明确定义为 `TelegraphPresentationModel` 的投影 |
| 4 | `Ctrl+S` 阻断 locale key | PR-02 §5.1 deferred, PR-05 §4.4 | PR-02 先落 no-op test stub，PR-05 补 locale key & 实际 toast |
| 5 | deferred `ITEM_COMPARE / COMBAT_DECISION` stub 的测试 schema | PR-02 §4.1, PR-03, PR-05 | PR-02 显式声明 "push 仍占栈深度 / render 空视图 / pollCommand no-op"，并要求 PR-03/PR-05 收口前保留该 stub |
| 6 | `DescriptionPresenter.combat action` 端到端覆盖 | PR-04 §4.3, PR-05 combat panel | PR-04 §8 声明 combat 分支"仅 unit test"；PR-05 §3 反向登记 |
| 7 | 人工白盒 preset / seed 的供给方 | PR-02 `MAPGEN_DIFF/20260401`, PR-03 `LOOT_LAB/20260413`, PR-04/05 `BOSS_VARIANT/20260412` | 每个 PR 在 §3.1 显式声明 preset 是否已有；若无需在 PR 内顺带落地 |
| 8 | a11y 控件入口的认领 | PR-01~05 均提及但无人落地 | PR-04 或 PR-01 认领最小 a11y toggle 模块 |
| 9 | golden label 前缀切分 | 所有 PR | README 增加一张 "label 前缀所有权" 表，避免 PR-02 deferred label 渗透到 PR-05 |

---

## 4. 优先级建议

按"必须在 PR-01 启动前修订文档"、"可以在执行过程中同步补强"、"可以随出 PR 时追补"三档分级。

### P0 - 启动 PR-01 前必须修订的文档条款
1. **`BuildInfo.shortHash` 的跨 PR 语义** —— 建议直接前置到 PR-01，以避免 PR-03 改动既有测试断言（§2.1.2）。
2. **`Continue UNAVAILABLE` 的 reasonCode 枚举与对应 locale key**（§2.1.4）。
3. **`ExplainPane + Backspace` 对 PR-02 truth table 的修订预案**（§2.4.1 / §3 表格 #1）—— 在 PR-02 的 truth table 里显式写 "若 ExplainPane 打开（PR-04 落地后），Backspace 关闭 ExplainPane"。
4. **人工白盒记录模板** `docs/opt/ui-pr/manual-records/_template.md` 的最小 schema 必须先行建立（§1.2）。
5. **README 追加 "跨 PR deferred 清单" 和 "golden label 前缀所有权" 两张表**（§1.1、§3 表格 #9）。

### P1 - 启动对应 PR 前必须修订
1. PR-02：`Ctrl+S` 进入 `InputHandlerTest` truth table；`force-switch.*` 子 key 列出（§2.2.1 / §2.2.5）。
2. PR-03：`ContentUiLintRule` 仅在跨模块 lint 无法表达时新增（§2.3.1）；`UiErrorPayload.contextKeyValuePairs` 排序规则显式化（§2.3.2）；`ModalCardModel` action 改枚举（§2.3.5）。
3. PR-04：`StatusPresentationModel.priority` 计算矩阵（§2.4.2）；`StatusPresentationModel[group=TELEGRAPH]` 边界与 PR-05 对齐（§2.4.3）；a11y 控件认领（§2.4.5）。
4. PR-05：`TARGET + Backspace` 在单一方式场景的回退细化（§2.5.1）；非法目标反馈路径（§2.5.2）；`ActionHintModel` 字段 schema（§2.5.4）。

### P2 - 可在 PR 开发期间同步补强
1. 把"skipped golden 不能替代人工白盒"的重复表述下沉到 README，各 PR 只引用（§1.7）。
2. "若触发 X，再补 Y" 的条件改成基于可度量数据（文件数、类型数）的硬判定（§1.3）。
3. 每份 PR 在 `前置条件` 节下补 `硬依赖条款` 子列表（§1.5）。
4. PR-03 `LOOT_LAB / PR-04 BOSS_VARIANT` 等 validation preset 的存在性核对（§2.3.10）。
5. PR-01 `StatusHudRenderer` 的 hex→token 迁移清单（§2.1.5）。
6. PR-04 `keywordRegistryLint` 从可选改为必选或补 CODEOWNERS 静态检查（§2.4.4）。

---

## 5. 附录 - 源计划映射完整性检查

- PR-01（源 §7 PR-01 + §8 PR-02）：源 Exit 条件 9 条 → 本 PR §1 + §8 覆盖 8 条；"locale 与主要键位不再隐藏在次级文案中"（源 §8.4.3）被 `常驻帮助区全部冻结` 吸收，口径基本一致。无整条丢失。
- PR-02（源 §9 PR-03）：源 Exit 条件 7 条 → 本 PR §1 + §8 覆盖 7 条；`LogPresentationModel` 最小字段面（分类/重要度/空态/回退文案）在 §5.2 被提及但没进 §8 出口门禁。建议补。
- PR-03（源 §10 PR-04 + §11 PR-05）：源 Exit 条件 9 条 → 本 PR §1 + §8 覆盖 9 条，但 `ItemIconKeyCoverageRule` 在 `contractLint` owner 下 / 还是新 lint 的决策未显式复述源计划 "优先扩 owner" 的立场（§2.3.1 已提）。
- PR-04（源 §12 PR-06 + §13 PR-07）：源 Exit 条件 8 条 → 本 PR §1 + §8 覆盖 7 条；`Advanced Tooltip 仍后置` 在 §2.1 被声明为本 PR 冻结口径但 §8 出口门禁没复述。影响不大。
- PR-05（源 §14 PR-08）：源 Exit 条件 4 条 → 本 PR §1 + §8 完整覆盖；新增 `TelegraphPresentationModel / CombatDecisionFrame / CombatDecisionPanel / ActionHintModel` 已对齐 §3.1。

结论：5 份 PR 合并后没有出现整条 Exit 条件被吞掉的情况，主要残留问题集中在"条款拆得不够细"和"跨 PR 边界没有显式 handoff"。上述 P0 / P1 修订动作执行完毕后，文档就具备可直接进入 PR-01 开发的成熟度。

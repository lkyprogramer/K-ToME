# Dark UI/UX Screen Coverage Matrix

本文件是 dark UI/UX 大改版的全量界面覆盖清单。它解决一个具体问题：`UI/PLAN.md` 和 PR 文档不能只覆盖局内主界面，还必须把首页、职业树、铭文商店、验证模式、结算页、错误页、loading 和 fallback/debug 面逐项纳入统一替换验收。

本文件是人类审计入口，不替代以下真源：

1. 资源 key 真源：`UI/sprite-sheets/key-registry.yaml`
2. sheet / cell 真源：`UI/sprite-sheets/sheet-plan.yaml`
3. canonical visual manifest：`assets-src/image/manifests/phase2-visual-manifest.json`
4. runtime visual manifest：`client/src/main/resources/manifests/visual-manifest.json`
5. 实际 UI 行为真源：`client` 当前代码、focused tests、`clientSmoke`、`goldenScreenshot`、packaged app 白盒记录

## 1. 覆盖完成定义

每个玩家可见或验证可见 UI 面都必须满足以下条件之一：

1. **Required**：有 owner PR、明确改造范围、focused test 或 golden/manual evidence、PR-07 final check。
2. **Conditional**：当前版本只在特定模式出现，但只要触发条件存在，就必须有 owner PR 和白盒路径。
3. **Debug/Fallback**：明确只作为 debug/fallback 保留，不作为正式玩家路径；PR-07 必须证明它不会覆盖 Tile 正式路径。

禁止把“在 PR-07 最后看一眼”当作覆盖。PR-07 只能收口证据和轻量 polish；如果发现本矩阵 Required 面没有 owner PR，必须回到对应 PR 补合同或新开修订 PR。

## 2. Screen / Surface Coverage Matrix

| UI 面 | 代码 / 合同入口 | 必须替换的 UX 内容 | Owner PR | 必填证据 |
| --- | --- | --- | --- | --- |
| 首页 / 主菜单 | `MainMenuScreen`、`MainMenuController`、`MainMenuFocusPolicy`、`MainMenuSummaryModel` | 首屏品牌/标题、主操作、继续游戏状态、新开局入口、验证模式入口、语言切换提示、紧凑帮助文案、键盘焦点状态 | PR-01 + PR-02 + PR-07 | `dark-uiux-pr01-home-main-menu` golden/manual；`MainMenuFocusPolicyTest`、`MainMenuControllerTest`、`MainMenuScreenTextTest`；PR-07 packaged app 首页截图 |
| 角色创建 / 职业选择 | `PlayerCreationPanel`、profession/race/zone selection state | 职业/种族/区域选择、locked/disabled 状态、summary、开始按钮、长文本截断、locale 切换后的重绘 | PR-01 + PR-06 + PR-07 | `dark-uiux-pr01-home-new-run`；`MainMenuScreenTextTest`；PR-06 profession icon/fallback coverage |
| 继续游戏异常 / 复制详情 | `ContinueAvailability`、`ContinueUnavailablePayload`、`ContinueUnavailablePayloadFormatter` | 不可继续原因、复制详情动作、禁用态、焦点态、错误内容不挤压主菜单 | PR-01 + PR-07 | `dark-uiux-pr01-continue-unavailable`；`ContinueUnavailablePayloadFormatterTest`；manual record |
| 验证模式入口 | `MainMenuAction.ValidationMode`、`GameApp.showValidationSetup` | 主菜单中验证模式入口必须与正式操作同风格，不能像 debug 链接或临时按钮 | PR-01 + PR-07 | `dark-uiux-pr01-validation-entry`；`GameAppLifecycleTest` validation entry case |
| 验证模式 Setup 页 | `ValidationSetupScreen`、`ValidationSetupController`、`ValidationScenarioPresentationCatalog` | preset/scenario 列表、active pack summary、locale 文案、选中态、禁用态、返回/启动操作、长 scenario 描述、窗口缩放 | PR-01 + PR-02 + PR-07 | `dark-uiux-pr07-validation-setup`；`ValidationSetupControllerTest`；`ClientSmokeHarnessTest` validation setup smoke；packaged app evidence |
| 验证模式运行时 overlay | validation scenario runtime、`ValidationPackSummaryText`、whitebox scenario | 验证标识、content pack summary、scenario evidence、overlay 不遮挡 HUD/日志/地图/任务 | PR-06 + PR-07 | `dark-uiux-pr07-validation-overlay`；validation overlay smoke/golden；`dark-uiux-pr07-final-ui` scenario evidence |
| 局内主 shell | `FoundationGameScreen`、`TileRenderer`、`GameShellLayout` | 左栏、地图、右栏、底部 HUD、日志、快捷键提示、地图视口、窗口缩放 | PR-01 + PR-02 + PR-07 | `dark-uiux-pr01-shell-1280x800`、`dark-uiux-pr01-shell-min-window`、`GameShellLayoutTest`、`TileRendererCanvasTest` |
| Loading / runtime error state | `UiLoadingState`、`UiErrorState`、`FoundationGameScreen.renderLoadingState`、`FoundationGameScreen.renderErrorState` | loading 文案、recoverable/unrecoverable error、返回/退出动作、debug detail 折叠/复制、暗黑 token 背景 | PR-01 + PR-02 + PR-07 | `dark-uiux-pr07-runtime-loading`、`dark-uiux-pr07-runtime-error`；`UiLoadingStateTest`、`UiErrorPayloadTest` |
| 全局错误页 | `UiErrorScreen` | asset/manifest/runtime 启动错误页、错误标题、详情、返回/退出动作、复制路径；不能保留旧红底临时页 | PR-01 + PR-02 + PR-07 | `dark-uiux-pr07-ui-error-screen`；manual record；asset failure injection record |
| 胜利结算页 | `VictoryScreen`、`OutcomeSummaryPresenter` | 胜利标题、run summary、奖励/历史、返回主菜单、继续流程、中文/英文布局 | PR-01 + PR-07 | `dark-uiux-pr07-outcome-victory`；`OutcomeSummaryPresenterTest`；golden outcome set |
| 失败结算页 | `GameOverScreen`、`OutcomeSummaryPresenter` | 死因、floor、run summary、重开/返回、失败语气和可读性；不能保留旧红色占位风格 | PR-01 + PR-07 | `dark-uiux-pr07-outcome-defeat`；`OutcomeSummaryPresenterTest`；golden outcome set |
| 装备面板 | `TileRenderModel` equipment section、`EquipmentSlotLabels` | 装备 slot、已装备/空/选中态、quality frame、tooltip、固定 hitbox | PR-03 + PR-07 | `dark-uiux-pr03-equipment-slots`；`EquipmentInventoryPresenterTest`；`TileRendererCanvasTest` |
| 背包 grid | inventory snapshot / renderer | 空态、满格、stack count、quality、disabled/unusable、tooltip、scroll/overflow 策略 | PR-03 + PR-07 | `dark-uiux-pr03-inventory-empty`、`dark-uiux-pr03-inventory-stacked` |
| 铭文商店 / Shop buy-sell | `UiMode.SHOP`、`ShopPanelSnapshot`、`ShopOfferSnapshot`、`DescriptionPresenter.presentShopItemLines` | shop header、buy/sell 双列、offer card、price/affordability、inscription tag、disabled reason、空商店、tooltip | PR-03 + PR-07 | `dark-uiux-pr03-inscription-shop`；`InputHandlerTest` shop cases；`DescriptionPresenterTest` shop item lines；manual record |
| 铭文满槽替换 modal | `inscriptionReplacementPrompt`、`ShopFocus`、`PlayerCommand.BuyShopOffer` replacement hotkey | 满槽提示、候选槽位、`1-6`/取消、购买前确认、不会吞金币/碎片、焦点和返回路径 | PR-03 + PR-07 | `dark-uiux-pr03-shop-full-slot-replace`；`InputHandlerTest` replacement prompt cases；manual record |
| 职业树侧栏 | `TalentSidebarPresenter`、`TalentTreeNodeSnapshot` | 三树、四态、节点连线、预览、locked/learnable/reserve/active、长描述截断 | PR-04 + PR-06 + PR-07 | `dark-uiux-pr04-talent-sidebar-start`、`dark-uiux-pr06-talent-icon-rebaseline` |
| 主动槽选择 modal | `ACTIVE_TALENT_SLOT_CHOICE`、`InputHandler` | `1-4` 替换、`R` reserve、`Esc` 取消、数字键边界、modal chrome、焦点态 | PR-04 + PR-07 | `dark-uiux-pr04-active-slot-choice`；`InputHandlerTest` |
| 技能 / 状态 / 任务面板 | status/quest/skill presentation models | skill icon、status icon、duration/stack、quest marker、fallback、tooltip/readability | PR-06 + PR-07 | `dark-uiux-pr06-status-quest-skill-overview`；`StatusPresentationModelTest`、`StatusIconResolverTest` |
| 战斗选择 / 行动提示 | `CombatDecisionPanel`、`ActionHintModel`、frontstage cue | action/method/target、locked/invalid、资源不足、free-cursor targeting、telegraph linkage | PR-02 + PR-05 + PR-06 + PR-07 | combat focused tests；`dark-uiux-pr07-combat-decision` manual/golden |
| Look / Inspect / Explain pane | inspect/explain presenters、modal card/layout | inspect card、keyword explanation、status/passive lines、tooltip/modal 层级 | PR-01 + PR-06 + PR-07 | `dark-uiux-pr07-look-inspect`；Explain/Description focused tests |
| 世界路线 / route selection | `UiMode.WORLD_MAP`、route preview/reward presentation | route options、reward preview、locked/unavailable、zone portrait、路线焦点 | PR-05 + PR-06 + PR-07 | `dark-uiux-pr07-world-route-selection`；`RoutePreviewTextTest`；manual record |
| 属性分配 / stat assign | `UiMode.STAT_ASSIGN` | 属性卡、可分配点数、确认/取消、禁用态、不会与 HUD 重叠 | PR-01 + PR-07 | `dark-uiux-pr07-stat-assign`；manual record |
| 被动 / reward / frontstage 选择 | reward presentation、frontstage presentation | passive、reward、milestone、route reward 的卡片化、可选/已选/不可选状态 | PR-03 + PR-06 + PR-07 | `dark-uiux-pr07-reward-frontstage`；reward focused tests/manual |
| 地图 / tile / actor / portrait / VFX | tile/actor/portrait/telegraph render path | 地面、墙、prop、interactable、actor、boss、portrait、telegraph、层级和遮挡 | PR-05 + PR-07 | `dark-uiux-pr05-map-layer-stack`、`dark-uiux-pr05-actor-boss-telegraph` |
| 设置 / 无障碍 | `AccessibilityToggle`、相关设置入口 | toggle、选中态、说明、compact layout、不会依赖颜色唯一传达信息 | PR-01 + PR-07 | `dark-uiux-pr07-accessibility-settings`；`AccessibilityToggleTest` |
| ASCII fallback / debug path | `AsciiRenderModel`、`AsciiRenderer` | 保留语义完整性，但明确不是正式玩家路径；不得阻止 Tile dark UI 验收 | PR-01 + PR-07 | `AsciiRenderModelTest`；PR-07 final audit 中标记 `Debug/Fallback` |
| Desktop title / launcher visible text | `DesktopLauncherTitleFormatter` | 窗口标题、版本/locale 简洁表达；不写 raw 本机路径 | PR-01 + PR-07 | `DesktopLauncherTitleFormatterTest`；manual screenshot metadata |

## 3. 必填 Golden / Manual Label Inventory

以下 label 是最低要求。实现可以增加更细 label，但不能删除这些覆盖点。

| Label | Owner | 覆盖内容 |
| --- | --- | --- |
| `dark-uiux-pr01-home-main-menu` | PR-01 | 首页主菜单、主操作、语言/帮助、焦点态 |
| `dark-uiux-pr01-home-new-run` | PR-01 | 角色创建/职业选择/开始状态 |
| `dark-uiux-pr01-continue-unavailable` | PR-01 | 不可继续状态与复制详情 |
| `dark-uiux-pr01-validation-entry` | PR-01 | 首页验证模式入口 |
| `dark-uiux-pr01-shell-1280x800` | PR-01 | 标准窗口局内 shell |
| `dark-uiux-pr01-shell-min-window` | PR-01 | 最小窗口局内 shell |
| `dark-uiux-pr02-standalone-screen-chrome` | PR-02 | 首页、验证 setup、结算、错误页共享 chrome/key 消费 |
| `dark-uiux-pr03-inventory-empty` | PR-03 | 空背包/空装备 |
| `dark-uiux-pr03-inventory-stacked` | PR-03 | 堆叠物品、数量 badge、quality |
| `dark-uiux-pr03-inscription-shop` | PR-03 | shop buy/sell、铭文 offer、价格/禁用态 |
| `dark-uiux-pr03-shop-full-slot-replace` | PR-03 | 铭文满槽替换 modal |
| `dark-uiux-pr04-talent-sidebar-start` | PR-04 | 职业树初始态 |
| `dark-uiux-pr04-active-slot-choice` | PR-04 | 主动槽 modal |
| `dark-uiux-pr05-map-layer-stack` | PR-05 | 地图层级、prop、actor |
| `dark-uiux-pr05-actor-boss-telegraph` | PR-05 | boss/telegraph 遮挡与可读性 |
| `dark-uiux-pr06-status-quest-skill-overview` | PR-06 | 状态、任务、技能 icon/readability |
| `dark-uiux-pr06-talent-icon-rebaseline` | PR-06 | 职业树 icon/fallback 收口 |
| `dark-uiux-pr07-validation-setup` | PR-07 | 验证模式 setup 页 |
| `dark-uiux-pr07-validation-overlay` | PR-07 | 验证模式运行时 overlay |
| `dark-uiux-pr07-outcome-victory` | PR-07 | 胜利结算 |
| `dark-uiux-pr07-outcome-defeat` | PR-07 | 失败结算 |
| `dark-uiux-pr07-runtime-loading` | PR-07 | loading state |
| `dark-uiux-pr07-runtime-error` | PR-07 | runtime recoverable/unrecoverable error |
| `dark-uiux-pr07-ui-error-screen` | PR-07 | 独立错误页 |
| `dark-uiux-pr07-accessibility-settings` | PR-07 | 设置/无障碍 surface |
| `dark-uiux-pr07-final-all-screens` | PR-07 | 本矩阵所有 Required/Conditional 面的最终索引证据 |

## 4. PR-07 Final Audit Rules

PR-07 的最终 `UI/review/dark-uiux-final-doc-implementation-audit.md` 必须包含本矩阵的逐项状态：

| 状态 | 含义 | PR-07 处理 |
| --- | --- | --- |
| `covered` | 有 owner PR、测试或 golden/manual、资源 coverage 无旧风格 residue | 可关闭 |
| `covered-with-exception` | 只在 debug/fallback 或非 macOS 环境受限 | 必须写明豁免原因和后续触发条件 |
| `partial` | UI 已改但缺 golden/manual、缺 focused test 或缺 coverage artifact | 不允许关闭 PR-07，除非降级为明确后续 PR 且不影响玩家主路径 |
| `missing` | 没有 owner PR 或没有实现/证据 | 不允许关闭 PR-07 |
| `not-applicable` | 当前版本没有入口或已被上游正式删除 | 必须引用删除/冻结合同 |

`dark-uiux-pr07-final-all-screens` 不是一张万能截图，而是一个 evidence index：它必须列出每个 Required/Conditional 面对应的 golden label、manual record、focused test、coverage artifact 和 packaged app evidence 路径。

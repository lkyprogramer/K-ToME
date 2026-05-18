# Dark UI/UX PR-04 Profession Tree UI

**阶段**: `dark-uiux-pr04-profession-tree-ui`
**优先级**: `P0`
**工作量**: `L`
**前置条件**: PR-01、PR-01-1、PR-02、PR-02-1、PR-03 串行完成；`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md` 的职业树语义已落地，且目标分支能找到 `phase4-v4-pr01` scenario、`TalentSidebarPresenter` 和 `ACTIVE_TALENT_SLOT_CHOICE`。
**资源生成结论**: PR04 为唯一 Talent Assign 参考图新增 PR04-scoped reference-crop skill icon / hero icon / modal chrome 证据资源；职业树全量 skill / tree / portrait icon 与正式通用 frame rebaseline 仍放到 PR-06。
**唯一开发参考图**: `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png`
**参考图 prompt**: `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.prompt.txt`
**参考图 SHA256**: `f586d29586cd8b7b60c3eadc5f1a06dfac9ae28e2b5bd2b88a21fa64c78a85b6`
**参考图尺寸**: `1280x840`

## Open Design 辅助参考

开发 PR04 时可在完成本 PR 预检后读取以下辅助设计输入：

1. [K-ToME Dark UI Design Reference For Open Design](../review/open-design/ktome-dark-ui-design.md)：统一 color roles、spacing、component states 与 anti-pattern 语言。
2. [Dark UI/UX PR04 Reference Detailing](../review/2026-05-17-dark-uiux-pr04-reference-detailing.md)：围绕唯一参考图细化 Talent Assign panel 的视觉拆解、信息层级和实现注意点。

这些文档只用于设计理解、review 和局部细化，不能覆盖本 PR 的参考图、验收矩阵、golden/manual evidence、manifest/resource 管线或 `UI/pr/screen-coverage-matrix.md`。

## 0.0 UI/UX Direction

PR04 的唯一目标是把当前 Talent Assign 从旧文字侧栏升级为 **ToME 式树状天赋分配面板**。开发、golden、manual evidence 和 review 都以 `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png` 为唯一视觉参考，尽量一比一还原。旧参考图、上一版复杂 workbench 图、完整装备/背包/属性同屏图都只作为历史废弃材料，不能覆盖本 PR 的布局、控件顺序或玩家信息层级。

“尽量一比一还原”在 PR04 中含义如下：

1. `1280x840` canonical viewport 下，主面板必须呈现参考图的同一结构：顶部 title + 职业/种族点数，左侧树状列表，右侧详情，底部 help / legend。
2. 左侧必须是三系全展开的树状列表，不是大 node graph、tier band grid、卡片墙、web dashboard 或完整游戏 shell。
3. 每个技能行必须包含 state marker、24px 级别 skill icon、中文技能名和 rank text；row 高度稳定，selected row 使用 cyan horizontal highlight。
4. 右侧必须按参考图顺序展示：hero icon + header、等级/消耗/前置、当前等级详情、下一等级预览、操作按钮。
5. 未学习技能的当前等级详情展示“预览 1 级”；已学习技能展示当前 rank 的真实效果。不能只展示下一等级预览。
6. 参考图中的状态 marker `[+] / [x] / [*] / [r]` 是正式 UI 表达，但其来源必须是 typed state，由 presenter 输出；renderer 不得解析 marker 文本反推状态。
7. 正式实现允许响应式缩放、裁剪和 scroll，但不能改变信息顺序、控件语义或改成另一套布局。

明确不吸收：

1. 不实现完整装备、背包、属性、地图、行动栏同屏。
2. 不实现复杂 node graph、拖拽节点、hover tooltip、动画 modal、网页式卡片布局。
3. 不新增第五规则状态，不新增互斥分支系统。
4. 不为 PR04 批量生成全职业正式新资源；只允许为 canonical Talent Assign 参考图新增 PR04-scoped reference-crop icon / modal chrome 证据资源，PR06 统一做正式全量 rebaseline。
5. 不把参考图里的数字当作 gameplay 真值；具体系数、消耗、冷却、范围必须来自当前 typed snapshot / schema / description model。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再跑 talent UI focused tests、resource / manifest dependency checks、client evidence、maintainability gate 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox | crossPrDependency | removalOwner |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `UI04-M00a-tree-data-foundation` | §2.6 / §4 data foundation gate | `game` | `EXISTING: VanguardTreeTest/ArcanistTreeTest/RogueTreeTest/TemplarTreeTest`, `EXISTING: TalentSchemaTest.\`profession tier two and tier three nodes declare specified prerequisite ranks\`` | `:game:test` | `game/build/reports/tests/test/index.html` | `N/A` | profession/talent schema data | `N/A` |
| `UI04-M00b-tree-coverage-audit` | §2.6 / §4 data foundation gate | `tools` | `EXISTING: Phase2ContentCoverageTest.\`phase2 content floor covers four professions route zones and minimum content matrix\`` | `:tools:contractLint --tests com.ktome.tools.lint.Phase2ContentCoverageTest` | `tools/build/reports/tests/contractLint/index.html` | `N/A` | profession/talent schema data | `N/A` |
| `UI04-M01-reference-fidelity` | §0.0 / §6 canonical panel layout | `client` / `docs` | `NEW: TileRendererCanvasTest.darkUiuxPr04TalentAssignPanelMatchesReferenceStructure`, `NEW: acceptanceContractLint.pr04ReferenceImageFidelity` | `:client:goldenScreenshot`, `verifyChanged` | `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png`, `client/build/reports/golden/` | `required` | reference image is canonical | `N/A` |
| `UI04-M02-presenter-authority` | §2.2 presentation contract | `client` | `NEW: TalentSidebarPresenterTest.pr04BuildsTalentAssignPanelModel`, `NEW: TalentSidebarPresenterTest.talentAssignPanelModelDoesNotMixNodeAndNonNodeStateFields`, `NEW: TalentSidebarPresenterTest.pr04HeaderPointTextsComeFromSnapshotNotPresenterRecompute`, `NEW: TalentSidebarPresenterTest.pr04SectionNodeCountTextUsesVisibleOverTotal`, `NEW: TalentSidebarPresenterTest.pr04StateMarkerAndStateIconKeyAreMutuallyExclusivePerRow`, `NEW: TalentSidebarPresenterTest.pr04FooterHelpTextIsPanelLevelAndDoesNotDuplicateActionsBlock` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `N/A` | `phase4-v4-pr01` | `N/A` |
| `UI04-M03-list-tree-layout` | §2.2 / §6 list tree layout | `client` | `NEW: TalentSidebarPresenterTest.pr04SectionsRowsContainOnlyTalentNodes`, `NEW: TalentSidebarPresenterTest.pr04RowsFollowSnapshotOrderAndPrerequisiteIndentation`, `prerequisiteConnectorMode=implemented: NEW: TalentSidebarPresenterTest.pr04ConnectorPrefixComesFromTypedPrerequisites`, `prerequisiteConnectorMode=deferred: NEW: TalentSidebarPresenterTest.pr04RowsHaveEmptyConnectorPrefixWhenPrerequisiteSourceDeferred` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `required` | typed talent prerequisite projection or explicit §9 deferral | `N/A` |
| `UI04-M03b-edge-state-projection` | §2.3 edge semantics | `client` | `NEW: TalentSidebarPresenterTest.pr04EdgeStateReflectsBothEndpointStates` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `required` | typed talent prerequisite projection | `N/A` |
| `UI04-M04-render-panel-model` | §5.1 renderer model | `client` | `NEW: TileRendererCanvasTest.darkUiuxPr04DrawsStateMarkerSkillIconNameRankAndSelectedRow`, `NEW: TileRendererCanvasTest.darkUiuxPr04KeepsRightDetailAboveNextPreview` | `:client:goldenScreenshot` | `client/build/reports/golden/` | `required` | PR-02 frame / font / icon resolver | `PR-06 icon rebaseline` |
| `UI04-M04b-selection-identity` | §2.4 selection identity | `client` | `NEW: InputHandlerTest.talentTreeSelectionRestoresByIdentityWhenSnapshotOrderChanges`, `NEW: TalentSidebarPresenterTest.selectedNodeUsesFullTreeOwnerIdentity`, `NEW: InputHandlerTest.talentTreeSelectionFallsBackToFirstVisibleNodeWhenIdentityDisappears`, `NEW: InputHandlerTest.modalCloseFlushesTalentSelectionIdentityToOverlayState` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `N/A` | `TalentTreeSelectionIdentity` | `N/A` |
| `UI04-M04c-legend-composition` | §6.6 legend composition | `client` | `NEW: TalentSidebarPresenterTest.pr04LegendIncludesFourStateToneAndFocusAndIncludesPendingOnlyWhenAnyRowIsPending` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `required` | presenter legend model | `N/A` |
| `UI04-M05-input-number-isolation` | §3 modal/input state machine | `client` | `NEW: InputHandlerTest.talentTreeSidebarIgnoresNumberHotkeysOutsideActiveSlotChoice` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `N/A` | `phase4-v4-pr01` | `N/A` |
| `UI04-M06-active-slot-choice-modal` | §3 active slot choice modal | `client` | `NEW: TalentSidebarPresenterTest.pr04ActiveSlotChoiceModalHasFiveItemsAndCancelIsFooter`, `NEW: TalentSidebarPresenterTest.pr04ActiveSlotChoiceModalMarksAllFourSlotsAsReplaceTargetsAndFocusedOnFirst`, `NEW: TalentSidebarPresenterTest.pr04ActiveSlotChoiceModalOnlyOpensWhenSnapshotSignalsSlotChoiceRequired`, `NEW: InputHandlerTest.activeSlotChoiceModalOpensWithSlotOneFocused`, `NEW: InputHandlerTest.activeSlotChoiceNumbersReplaceSlotsAndCloseModal`, `NEW: InputHandlerTest.activeSlotChoiceReserveConfirmsToReserve`, `NEW: InputHandlerTest.activeSlotChoiceEscRollsBackDraft`, `NEW: InputHandlerTest.talentSlotChoiceIgnoresNonContractKeys` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `required` | `phase4-v4-pr01` | `N/A` |
| `UI04-M07-preview-current-rank-detail` | §6.4 preview pane | `client` | `NEW: TalentSidebarPresenterTest.pr04PreviewShowsCurrentRankDetailBeforeNextRankPreview`, `NEW: TalentSidebarPresenterTest.pr04UnlearnedTalentCurrentDetailUsesPreviewRankOne`, `NEW: TalentSidebarPresenterTest.pr04PreviewBlocksFollowFixedOrdering`, `NEW: TalentSidebarPresenterTest.pr04DetailBlockPrimaryAndBodyFieldsMatchKindTable`, `NEW: TalentSidebarPresenterTest.pr04CurrentRankDetailPrefersDescriptionPresenterOverRawSchemaFields` | `localeLint`, `contractLint`, `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `required` | `DescriptionPresenter` / `descriptionModel` | `N/A` |
| `UI04-M08-race-tree-no-filter` | §2.4 runtime tree contract | `client` | `NEW: TalentSidebarPresenterTest.rendersRaceTreeRowsWithoutFilteringSnapshotTrees`, `EXISTING: InputHandlerTest.\`talent assign respec follows focused tree owner\`` | `contractLint` | `build/reports/tests/client/test/index.html` | `N/A` | `RenderUiStateSnapshot.talentTrees` | `N/A` |
| `UI04-M09-resource-dependency` | §2.5 / §6 icon resource rules | `assets` / `client` | `NEW: TileRendererCanvasTest.darkUiuxPr04SkillIconFallbackRendersWithoutCrashing`, `NEW: acceptanceContractLint.pr04SkillIconFallbackRecordedInManualEvidence` | `manifestLint`, `:client:goldenScreenshot` | `assets-src/image/manifests/phase2-visual-manifest.json`, `client/src/main/resources/manifests/visual-manifest.json` | `N/A` | PR-02 frame / PR-06 icon rebaseline | `PR-06` |
| `UI04-M10-upstream-whitebox-materialization` | §8.2 PR04 whitebox preparation | `tools` | `EXISTING: Phase4V4WhiteboxScenarioCliTest.\`pr01 scenario generates profession tree evidence names from the typed registry\`` | `preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-profession-tree-ui` | `build/whitebox/dark-uiux-pr04-profession-tree-ui/expected-evidence.json`, `build/whitebox/dark-uiux-pr04-profession-tree-ui/cua-runbook.md` | `required` | `dark-uiux-pr04-profession-tree-ui` | `N/A` |
| `UI04-M11-manual-label-capture` | §8.2 PR04 dark label mapping | `docs` | `NEW: acceptanceContractLint.pr04ManualLabelMapping#labelMapping` | packaged whitebox manual record | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | `required` | `UI04-M10-upstream-whitebox-materialization` | `N/A` |
| `UI04-M12-min-window-manual-record` | §8.3 min-window manual evidence | `docs` | `NEW: acceptanceContractLint.pr04ManualLabelMapping#minWindow` | packaged whitebox manual record | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | `required` | `UI04-M04-render-panel-model` | `N/A` |
| `UI04-M13-right-companion-coexistence` | §6.7 co-existence layout | `client` | `NEW: TileRendererCanvasTest.darkUiuxPr04TalentAssignPanelDoesNotCoverRightCompanionOrBottomLog` | `:client:goldenScreenshot` | `client/build/reports/golden/` | `required` | PR-02-1 shell / PR-03 right panel | `N/A` |
| `UI04-M14-right-companion-manual-evidence` | §8.2 / §8.3 manual evidence | `docs` | `NEW: acceptanceContractLint.pr04ManualLabelMapping#rightCompanion` | packaged whitebox manual record | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | `required` | `UI04-M13-right-companion-coexistence` | `N/A` |
| `UI04-M15-overflow-and-input-bounds` | §6.3 / §6.5 overflow and pointer boundary | `client` | `NEW: TileRendererCanvasTest.darkUiuxPr04OverflowsListWithoutLayoutShift`, `mousePointerMode=implemented: NEW: InputHandlerTest.talentTreeMouseClickSelectsNodeWhenHitRegionExists`, `mousePointerMode=deferred: NEW: InputHandlerTest.talentTreeKeyboardOnlyWhenMouseDeferred` | `:client:clientSmoke`, `:client:goldenScreenshot` | `build/reports/tests/client/test/index.html`, `client/build/reports/golden/` | `required` | PR-02-1 shell hitbox model | `N/A` |
| `UI04-M16-removal-and-rebaseline-plan` | §9 removal / iteration plan | `docs` | `acceptanceContractLint` | `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` | PR-06 icon rebaseline, PR-07 final all-screens | `PR-06` |

PR04 新增测试统一使用 `camelCase`；`EXISTING` 测试保留原仓库命名风格，但 Kotlin backtick 方法名必须在文档中保留 backtick。`ownerGate` 列中逗号分隔的 task 是 AND，必须全部执行，不是 OR。`acceptanceContractLint.rule#subRuleId` 使用 `#` 表示 subRuleId；无 `#` 时表示整条 lint rule。条件型 fastCheck 只能引用 §10 self-audit 字段（当前为 `mousePointerMode` 和 `prerequisiteConnectorMode`），不能写成无法机器解析的自由文本。

### Gate Budget

`durationSource` 固定指 `build/verification/verify-changed/full-task-duration-summary.{json,md}`；首次 `verifyChanged` 之前，每个 gate 的耗时来源都写 `N/A before first verifyChanged`，不得使用 `same as above` 这类位置相关引用。

| Gate group | Tasks | Trigger | Freshness / evidence rule | Duration source |
| --- | --- | --- | --- | --- |
| data foundation | `:game:test --tests com.ktome.game.data.VanguardTreeTest --tests com.ktome.game.data.ArcanistTreeTest --tests com.ktome.game.data.RogueTreeTest --tests com.ktome.game.data.TemplarTreeTest --tests com.ktome.game.data.TalentSchemaTest`, `:tools:contractLint --tests com.ktome.tools.lint.Phase2ContentCoverageTest` | list-tree UI implementation starts | Must pass before PR04 draws prerequisite indentation; failing counts, empty playable trees, out-of-tree prerequisites, weak tier prereq ranks, or missing starters are upstream data bugs, not UI bugs | `durationSource` or `N/A before first verifyChanged` |
| contract preflight | `acceptanceContractLint` | PR 文档新增 presentation、whitebox、cross-PR 和 removal 合同 | 必须先于实现跑；失败先修文档，不用 client test 代替 | `durationSource` or `N/A before first verifyChanged` |
| fast lane | `:client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.screen.ValidationScenarioBootstrapTest`, `:tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest` | presenter/model/input/whitebox 语义变化 | 新增或改 public presentation model、list tree row、detail block、modal command、hit region、row layout 时必须刷新对应 focused test | `durationSource` or `N/A before first verifyChanged` |
| resource / manifest dependency | `manifestLint`, `contractLint`, `localeLint` | PR04 消费 PR02 frame、PR04 reference-crop talent icon / modal chrome 和 talent locale 文案 | PR04 只新增 `dark.uiux.pr04.talent.vanguard.*` 与 `dark.uiux.pr04.talent_assign.chrome.*` reference-crop 证据 key；缺 key 必须走 canonical manifest / `syncPhase2Manifests`，不在生产 Kotlin 裸字符串 raw path fallback | `durationSource` or `N/A before first verifyChanged` |
| client evidence | `:client:clientSmoke`, `:client:goldenScreenshot` | player-visible talent assign panel、active slot modal、right companion restoration、min-window compact readability | `dark-uiux-pr04-*` golden / manual evidence 必须刷新；PR06 rebaseline 前旧 icon 只允许作为 §9 临时状态 | `durationSource` or `N/A before first verifyChanged` |
| governance / final closure | `maintainabilityLint`, `verifyChanged` | 新增 presentation model、renderer shared row layout 或 model family 拆分超过 5 个 Kotlin 文件 | `maintainabilityLint` 新增 finding 不能用 baseline 掩盖；`verifyChanged` 是最终 closure，不替代 owner gate | `durationSource` or `N/A before first verifyChanged` |
| packaged whitebox | `:client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-profession-tree-ui` | PR04 requires manual whitebox evidence | generated files 不提交；manual record 必须记录 runbook、expected evidence、screenshot/app hash 和 residual risk | manual record 写入实际运行耗时或 `N/A not run` |

### Canonical Artifact

| Artifact | Path | Owner | Rule |
| --- | --- | --- | --- |
| canonical reference image | `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png` | PR04 | 唯一视觉开发参考；prompt 和 sha256 必须在 PR04 文档/PR 描述中保持可追踪 |
| focused test report | `build/reports/tests/client/test/index.html`, `tools/build/reports/tests/test/index.html` | PR04 | 必须覆盖 presenter、input、renderer、manifest dependency、scenario bootstrap / materialization |
| client golden | `client/build/reports/golden/` | PR04 | 至少包含 `dark-uiux-pr04-talent-assign-panel-start`、`dark-uiux-pr04-active-slot-choice`；min-window 与 right-companion 由 packaged manual evidence 或等价 hash/report 覆盖；旧 `talent-sidebar-*` 名称只作为 §9 alias |
| PR04 manual record | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | PR04 | 必须引用 generated `build/whitebox/dark-uiux-pr04-profession-tree-ui/cua-runbook.md`、`build/whitebox/dark-uiux-pr04-profession-tree-ui/expected-evidence.json`、app sha256、runtime home、screenshot path / sha256、skip rule |
| upstream gameplay manual record | `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md` | upstream v4 PR01 | 只作为 gameplay baseline；PR04 不覆写它 |
| verify duration | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | tools | 用于 Gate Budget 复盘；不存在时记录 `N/A before first verifyChanged` |

### Failure Rule

如果职业树 UI 失败，按以下顺序定位，不得直接改规则层：

1. `TalentAssignPanelModel` 是否完整表达参考图的信息顺序和 row/detail/action 结构。
2. `TalentSidebarPresenter` 是否只从 `RenderUiStateSnapshot`、typed schema projection 和 localizer 生成 presentation fields。
3. `TileRenderModel` / `TileRenderer` 是否只消费 presentation fields，且没有根据 localized text、marker、role、icon key 或 row index 重新推导状态、前置、owner 或 learnability。
4. `InputHandler` 是否在 `ACTIVE_TALENT_SLOT_CHOICE` 内唯一消费 `1-4 / R / Esc`。
5. Existing skill icons / frame keys 是否能通过 manifest 解析；fallback icon 只能 crash-safe，不算 passing evidence。
6. `RenderUiStateSnapshot.talentTrees` 是否被 client 原样渲染；client 不得过滤 race tree。

不得通过修改 `TalentProgression.learnableTalentIds`、starter 数、Tier 门槛、owner metric、longRun 分母、`phase4-v4-pr01` gameplay setup 或日志事件断言来修 PR04 UI。

## 1. 阶段目标

1. 把职业树 UI 接入新的暗黑 UI 框架，覆盖 title、职业/种族点数、三系树状列表、skill icon、state marker、selected row、右侧当前等级详情、下一等级预览、操作按钮和底部 legend。
2. 保留 `TalentSidebarPresenter` 作为 presentation authority；renderer 只渲染 presenter 输出，不重拼 rank、lock reason、状态文案、前置路径或 talent rule。
3. 展示 `LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE` 四态、职业点数、种族点数、锁定原因、学习预览、rank before/after 和 breakpoint preview。
4. `ACTIVE_TALENT_SLOT_CHOICE` modal 使用参考图同源的暗黑 panel chrome：`1-4` 替换、`R` 入 reserve、`Esc` 取消，并把 reserve 作为 typed 第 5 行显示，而不是只画底部提示文字。
5. 数字键在 talent sidebar 打开但 active slot modal 未打开时不改变选择；仅 active slot modal 消费 `1-4`。
6. 阶段范围限定：PR04 close 时 canonical Vanguard Talent Assign 首屏可使用 `dark.uiux.pr04.talent.vanguard.*` 与 `dark.uiux.pr04.talent_assign.chrome.*` reference-crop 证据资源；非 canonical 职业树 icon / tree portrait / skill icon 仍可指向现有 manifest entry，PR06 才统一切到 dark-v1 skill/tree/frame 资源并 rebaseline PR04 golden。
7. PR04 的体验验收以“玩家是否能像参考图一样读懂树、当前效果、下一点收益和槽位后果”为准；不能只证明文本行被画出来。

“三棵职业树”只定义 PR04 manual/golden 的最小必验面：当前已选职业的三棵 profession tree。runtime 渲染不得过滤 `RenderUiStateSnapshot.talentTrees`。如果 snapshot 包含 `ownerType=RACE` 的 tree，PR04 必须按同一 list row / detail contract 渲染，并使用 `raceTalentPoints` 作为点数来源。PR04 不新增 race tree 规则，不新增 client-side owner 过滤。全仓职业覆盖口径以 [repo README](../../README.md) 的职业覆盖分类为准：4 个 release playable、2 个 dev playable/report-only、2 个 frozen excluded。

## 2. 硬依赖合同

### 2.1 Snapshot 与 typed enum

1. `TalentTreeNodeSnapshot.category: TalentCategory` 与 `TalentTreeNodeSnapshot.state: TalentNodeStateSnapshot` 都是 typed enum。
2. PR04 四态 UI 只消费 `state`；主动槽选择是否需要 replacement modal 只消费 `category in ACTIVE/SUSTAINED`。
3. client 不做字符串 `valueOf` 解析，不从 localized text、marker、rank 文案或 icon key 推断 state/category。
4. 修改 `TalentSidebarPresenter` 时禁止新增会触发 `TalentProgression` 派生计算的字段或方法；只允许新增纯展示态、layout hint 或已存在 snapshot 的投影字段。
5. 当前上游正式状态只有 `LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE` 四态。参考图中的 `[x]` 只代表 locked，不新增第五规则状态。
6. prerequisite indentation / connector prefix 只能来自 typed snapshot、schema projection 或 presenter 的确定性 layout policy；renderer 不得从显示文本、localized lock reason、节点 icon 或颜色反推这些语义。

### 2.2 Talent Assign presentation contract

`TalentSidebarPresenter` 必须输出 renderer 足够消费的 exact presentation model。PR04 的主模型是一个 **single presenter-owned list tree panel family**。现有 `TalentSidebarLine` 不再作为正式 renderer authority：要么删除，要么作为 debug/adapter-only 输出，并在 §9 写清 exit rule。

新增 client presentation family：

```kotlin
import com.ktome.core.talent.TalentTreeOwnerType

enum class TalentTreeNodeToneToken {
    TALENT_LOCKED,
    TALENT_LEARNABLE,
    TALENT_RESERVE,
    TALENT_ACTIVE,
}

enum class TalentAssignRowKind {
    TALENT_NODE,
}

enum class TalentTreeEdgeState {
    UNSATISFIED,
    SATISFIED,
    ACTIVE,
}

enum class TalentLegendItemKind {
    STATE_TONE,
    FOCUS,
    PENDING_OVERLAY,
}

enum class TalentAssignFooterHintKind {
    SELECT,
    SWITCH_TREE,
    LEARN,
    RESERVE,
    CLOSE,
}

data class TalentAssignPanelModel(
    val referenceImagePath: String,
    val header: TalentAssignHeaderModel,
    val sections: List<TalentAssignSectionModel>,
    val detail: TalentDetailPaneModel?,
    val legend: TalentAssignLegendModel,
    val footerHints: List<TalentAssignFooterHintModel>,
    val activeSlotChoiceModal: ActiveSlotChoiceModalModel?,
)

data class TalentAssignHeaderModel(
    val title: String,
    val professionPointText: String,
    val racePointText: String?,
)

data class TalentAssignSectionModel(
    val treeId: String,
    val ownerType: TalentTreeOwnerType,
    val treeOwnerId: String,
    val displayName: String,
    val nodeCountText: String,
    val iconKey: String?,
    val rows: List<TalentAssignTreeRowModel>,
    val edges: List<TalentTreeEdgeProjection>,
    val scroll: TalentAssignSectionScrollModel,
)

data class TalentAssignTreeRowModel(
    val kind: TalentAssignRowKind,
    val talentId: String,
    val treeId: String,
    val ownerType: TalentTreeOwnerType,
    val treeOwnerId: String,
    val indentLevel: Int,
    val connectorPrefix: String,
    val stateMarkerText: String,
    val stateIconKey: String?,
    val skillIconKey: String?,
    val displayName: String,
    val rankText: String,
    val toneToken: TalentTreeNodeToneToken,
    val pendingOverlay: Boolean,
    val focused: Boolean,
)

data class TalentTreeEdgeProjection(
    val fromTalentId: String,
    val toTalentId: String,
    val state: TalentTreeEdgeState,
)

data class TalentAssignSectionScrollModel(
    val verticalOffset: Int,
    val hasVerticalOverflow: Boolean,
)

data class TalentAssignLegendModel(
    val items: List<TalentAssignLegendItem>,
)

data class TalentAssignLegendItem(
    val kind: TalentLegendItemKind,
    val iconKey: String?,
    val label: String,
    val markerText: String?,
    val toneToken: TalentTreeNodeToneToken?,
)

data class TalentAssignFooterHintModel(
    val kind: TalentAssignFooterHintKind,
    val keyText: String,
    val labelText: String,
)
```

Authority rules：

1. `TalentSidebarPresenter` owns all display text, row ordering, section meta, connector prefix, indent level, marker text, rank text, focus, legend item, detail block and active-slot modal item ordering.
2. `TileRenderModel` / `TileRenderer` only map the model to draw calls. They may choose pixel bounds, clipping and truncation, but may not derive state/prerequisite/category from text, color, icon key, role or row index.
3. `TalentTreeNodeToneToken` has exactly four values, one for each `TalentNodeStateSnapshot` state. Pending is not a fifth state and not a fifth tone; it is only `pendingOverlay=true`.
4. `TalentAssignSectionModel.rows` contains only talent node rows. Section headers are rendered from `TalentAssignSectionModel.displayName` and `TalentAssignSectionModel.nodeCountText`; they never appear as rows, never receive focus, and never own hit regions.
5. `professionPointText` comes from `RenderUiStateSnapshot.uiState.playerStatus.talentPoints` through the snapshot boundary; `racePointText` comes from `RenderUiStateSnapshot.uiState.playerStatus.raceTalentPoints` and is non-null when the runtime tree set contains a race tree or race points must be visible. Presenter formats these values, but never recomputes learnability or point banks.
6. `nodeCountText` is `${visibleNodeCount}/${totalNodeCount}` for the section. It is not learned-rank progress and not point investment. In the canonical Vanguard reference, fully visible sections render `武器系  6/6`, `护卫系  5/5`, `战吼系  5/5`.
7. `stateMarkerText` is presenter-owned and must be one of localized equivalents of `[x]`, `[+]`, `[*]`, `[r]` for `LOCKED / LEARNABLE / LEARNED_ACTIVE / LEARNED_RESERVE` in the canonical reference. PR04 uses marker-text-only state chrome: `stateMarkerText` is always non-null and `stateIconKey` is always null in the canonical implementation path. Renderer draws the marker and never parses it.
8. `stateIconKey` is reserved for a future icon-only variant. It must be null in PR04. If a future owner switches to icon-only, that owner must first revise this model contract, add a new reference/golden contract, and update the self-audit. PR04 must add `TalentSidebarPresenterTest.pr04StateMarkerAndStateIconKeyAreMutuallyExclusivePerRow`.
9. `skillIconKey` is required for `TALENT_NODE` rows when `TalentTreeNodeSnapshot.iconKey` is non-null. Fallback skill icon may prevent a crash but cannot pass PR04 reference fidelity evidence unless manual record marks the fallback and PR06 owner.
10. `rankText` is always presenter-provided through a shared rank formatter. Use `rank/maxRank` when live and preview are equal, `committed→rank/maxRank` when there is pending allocation. Renderer must not append rank suffixes.
11. The same rank formatter must also produce `RANK_AND_COST.primaryText`, so row `rankText` and detail rank transition cannot drift by using different arrows, spaces or max-rank wording.
12. `connectorPrefix` is a display projection of typed prerequisites. Missing typed prerequisite source means no connector drawing unless §9 has an explicit deferral entry; renderer must not infer connectors from lock reason text.
13. `focused=true` means keyboard focus / selected row. Equipped/active slot membership must not reuse `focused`.
14. `footerHints` is panel-level navigation help only, e.g. `SELECT(↑↓, 选择)` / `SWITCH_TREE(←→, 切换系)` / `CLOSE(Esc, 关闭)`. It must be structured key/label pairs from presenter/localizer; renderer must not parse localized prose. It must not duplicate the selected talent `ACTIONS` block, which contains talent-specific commands such as `Enter 学习` or disabled equivalents.
15. `TalentTreeOwnerType` must be typed in new PR04 models. Existing snapshot DTO strings are parsed once at presentation boundary; new code must not propagate raw owner strings deeper.

Row grammar is fixed:

```text
<indent spaces><connectorPrefix><stateMarkerText> <24px skillIcon> <displayName> <rankText>
```

Section header row rendering is fixed as:

```text
<displayName>  <nodeCountText>
```

The two spaces are intentional. `displayName` uses cyan tone, `nodeCountText` uses ember-gold tone, and renderer must not add a colon, prefix, suffix or learned-rank interpretation.

Example canonical rows:

```text
[*] [icon] 猛击 1/5
  └─ [+] [icon] 横扫 0/5
      └─ [x] [icon] 破阵 0/5
```

### 2.3 Prerequisite indentation and edge semantics

PR04 不使用 tier band / column grid。Claude round2 中关于 `tierIndex`、`columnIndex`、双层 tier focus、per-tier horizontal scroll 的问题，通过删除 grid model 并改为 reference image 的 list tree model 解决。

Presenter 的 indentation algorithm 固定：

1. 以 `TalentTreeSnapshot.nodes` 顺序作为 row order；client 不按 id/name/localized text 重排。
2. root node `indentLevel=0`，`connectorPrefix=""`。
3. 子节点根据 `TalentSchemaV2.requirements.talentPrereqs` 的同树前置关系投影为 tree depth。单一前置链按父节点 depth + 1；多个前置时按最深 satisfied prerequisite + 1，并在 `connectorPrefix` 中只显示主前置线，其他前置在 detail pane `PREREQUISITE` block 展示。
4. 同一父节点多个 child 时，presenter 根据 `TalentTreeSnapshot.nodes` 相对顺序决定 `├─` / `└─`。
5. 缺 typed prerequisite source 时，不允许 renderer 猜线；PR04 必须停止 connector 实现或在 §9 写 deferred exit rule。

`TalentTreeEdgeState` 用于 presenter 判断 connector tone：

| State | Trigger | Renderer treatment |
| --- | --- | --- |
| `UNSATISFIED` | prerequisite minRank 未达 | muted locked tone connector |
| `SATISFIED` | prerequisite minRank 已达，但当前节点还不是 learned active/reserve | subdued active tone connector |
| `ACTIVE` | prerequisite 与当前节点都是 `LEARNED_ACTIVE` 或 `LEARNED_RESERVE` | ember-gold connector |

Required tests: `NEW: TalentSidebarPresenterTest.pr04SectionsRowsContainOnlyTalentNodes`, `NEW: TalentSidebarPresenterTest.pr04RowsFollowSnapshotOrderAndPrerequisiteIndentation`, `NEW: TalentSidebarPresenterTest.pr04EdgeStateReflectsBothEndpointStates`。When `prerequisiteConnectorMode=implemented`, also require `NEW: TalentSidebarPresenterTest.pr04ConnectorPrefixComesFromTypedPrerequisites`; when `prerequisiteConnectorMode=deferred`, require `NEW: TalentSidebarPresenterTest.pr04RowsHaveEmptyConnectorPrefixWhenPrerequisiteSourceDeferred` and cite the §9 `missing typed prerequisite projection` row.

### 2.4 Tree ordering、selection identity 与 race tree

1. runtime tree order = `RenderUiStateSnapshot.talentTrees` order；client 不排序。Race tree 的位置由 snapshot owner 决定，client 既不前置也不后置。
2. node order = `TalentTreeSnapshot.nodes` order；client 不排序。
3. PR04 不新增 per-tree collapse state；“折叠/展开”仅指现有 `talentTreePreviewExpanded` 对 detail pane 的展开/收起。若后续需要 per-tree collapse，必须新增 typed state owner 和测试，不得在 renderer 临时保存 map。
4. PR04 新增 client-only type `client/src/main/kotlin/com/ktome/client/ui/talent/TalentTreeSelectionIdentity.kt`：

```kotlin
data class TalentTreeSelectionIdentity(
    val talentId: String,
    val treeId: String,
    val ownerType: TalentTreeOwnerType,
    val treeOwnerId: String,
)
```

5. `InputHandler` owns selection restoration：`OverlayState.talentTreeSelectionIdentity: TalentTreeSelectionIdentity?` and `ModalFrameLocalState.talentTreeSelectionIdentity: TalentTreeSelectionIdentity?` are the persisted UI identity. `talentTreeSelection: Int` may remain as an internal navigation cache only.
6. `ModalFrameLocalState.talentTreeSelectionIdentity` is modal-lifecycle local state. `OverlayState.talentTreeSelectionIdentity` is the durable overlay identity after a modal closes. Close flow is one-way: modal local identity is flushed to overlay identity; snapshot update then restores selection from overlay identity. Client code must not merge both owners or prefer flat index when identity exists.
7. Snapshot 更新后先用 `TalentTreeSelectionIdentity` 恢复 selected node；找不到 identity 时 clamp 到 first visible node and refresh identity。不得只按 `talentId` 或 flat index 判断 selected。
8. `RenderUiStateSnapshot.talentTrees` 是 runtime 展示输入，client 不得过滤 `ownerType=RACE`。
9. profession tree 和 race tree 使用同一 row contract、tone contract、detail contract 和 respec owner contract。
10. `raceTalentPoints` 只用于 header / points display，不在 client 重算 race learnability。

Required tests: `NEW: InputHandlerTest.talentTreeSelectionRestoresByIdentityWhenSnapshotOrderChanges`, `NEW: TalentSidebarPresenterTest.selectedNodeUsesFullTreeOwnerIdentity`, `NEW: InputHandlerTest.talentTreeSelectionFallsBackToFirstVisibleNodeWhenIdentityDisappears`, `NEW: InputHandlerTest.modalCloseFlushesTalentSelectionIdentityToOverlayState`, `NEW: TalentSidebarPresenterTest.rendersRaceTreeRowsWithoutFilteringSnapshotTrees`。

### 2.5 Resource dependency

PR04 依赖 PR02 已交付的 `ui.frame.modal.body`、`ui.frame.tooltip.body`、`ui.frame.slot.*`，以及 PR04-scoped reference-crop talent icon / modal chrome key 可经 canonical manifest 解析。非 canonical skill/tree icon 缺正式资源时记录 fallback 并回 PR06 icon rebaseline owner scope。PR04 不在生产 Kotlin 中写 raw path、裸字符串 fallback 或第二套 icon inventory。

### 2.6 Profession tree data foundation audit

PR04 的 list-tree UI 只能建立在现有 profession / talent schema 已经能构成树的前提上。当前仓库真源是 `game/src/main/resources/data/professions/index.yaml` 与 `game/src/main/resources/data/talents/index.yaml`；UI 参考图不能替代数据地基。

截至本文件更新时，职业树地基如下：

| Coverage class | Professions | tree shape (nodes per tree) | tier distribution (nodes per tier, summed across trees) | UI decision |
| --- | --- | --- | --- | --- |
| release playable | `vanguard`, `arcanist`, `rogue`, `templar` | 每个职业 `3` 棵 profession tree，`16` 个 talent nodes；tree shape 为 `6/5/5` | `T1=6 / T2=6 / T3=4` | PR04 manual/golden 的完整 baseline。必须能展示三棵树、前置 connector、节点状态、当前等级详情、下一等级预览和槽位后果 |
| dev playable / report-only | `berserker`, `spellblade` | 每个职业 `3` 棵 profession tree，`12` 个 talent nodes；tree shape 为 `4/4/4` | `T1=6 / T2=3 / T3=3` | 可以用同一 UI contract 渲染为 compact tree；不能把它当 release baseline 证明四个基础职业已覆盖 |
| frozen excluded | `shadowblade`, `warden` | 每个职业有 `3` 个 tree id，但 tree nodes 为 `0`，starter talents / inscriptions 为空 | `N/A` | 不构成完整可玩 tree。若 UI 暴露这些职业，只能显示 locked/frozen fallback，不得画成空的可玩职业树 |

Tier distribution example for release playable professions: `T1 = 2(6-node tree) + 2(5-node tree) + 2(5-node tree) = 6`; `T2 = 2 + 2 + 2 = 6`; `T3 = 2 + 1 + 1 = 4`.

PR04 对“完整 tree”的定义：

1. 当前 release playable 职业必须保持 `3` 棵 profession tree 和 `16` 个 nodes；如果设计上要改成 `12`、`18` 或其他规模，必须先改 profession/talent 数据合同和测试，再改 PR04 UI 验收口径。
2. 每棵非空 tree 必须满足：node id 不重复、每个 node 对应的 `TalentSchemaV2.treeId` 与 tree id 一致、至少有一个 root talent、存在 early-game talent。
3. 每条前置边只能来自 `TalentSchemaV2.requirements.talentPrereqs`，且必须同树、指向更早的 node，不能自环、不能跨树、不能形成 cycle。
4. tier2 / tier3 节点必须声明足够的前置 rank：当前 `TalentProgression.talentNodeTier` 对 `<=4` 节点树使用 `2/1/1` 分布，对 `>4` 节点树使用 `2/2/rest` 分布；tier2 节点至少有 `minRank >= 2` 的 prerequisite，tier3 节点至少有 `minRank >= 3` 的 prerequisite。
5. UI 的 `indentLevel / connectorPrefix / TalentTreeEdgeProjection` 可以由 presenter 从 typed schema projection 确定性投影；renderer 不得从 node 顺序、颜色、锁定文案或 localized text 再推导一次规则语义。
6. 如果以上任一基础失败，PR04 停止 list-tree connector drawing，先修 `game` 数据、schema test 或 gameplay snapshot projection。不能用 placeholder 节点、fake connector、隐藏 locked node、降低 golden 断言等方式把 UI 做绿。

## 3. Modal 与输入状态机

### 3.1 Active slot choice modal

| State | Input | Command | Modal stack effect | Overlay mode after command | Test |
| --- | --- | --- | --- | --- | --- |
| `ACTIVE_TALENT_SLOT_CHOICE` | `1..4` | `PlayerCommand.ConfirmTalentDraftReplacingSlot(slot)` | close all modal frames | command owner decides next snapshot; PR04 不在 client 猜测结果 | `InputHandlerTest.activeSlotChoiceNumbersReplaceSlotsAndCloseModal` |
| `ACTIVE_TALENT_SLOT_CHOICE` | `R` | `PlayerCommand.ConfirmTalentDraftToReserve` | close all modal frames | command owner decides next snapshot | `InputHandlerTest.activeSlotChoiceReserveConfirmsToReserve` |
| `ACTIVE_TALENT_SLOT_CHOICE` | `Esc` | `PlayerCommand.RollbackTalentDraft` | close all modal frames | command owner decides next snapshot | `InputHandlerTest.activeSlotChoiceEscRollsBackDraft` |
| `ACTIVE_TALENT_SLOT_CHOICE` | `Backspace` | `null` | PR04 不主动消费；如果 root modal stack 已有 Backspace close 行为，必须测试它不产生 `PlayerCommand` | stays in modal or root close, owned by existing modal stack | `InputHandlerTest.talentSlotChoiceIgnoresNonContractKeys` |
| `ACTIVE_TALENT_SLOT_CHOICE` | `T` / `Tab` / movement keys | `null` | no selection leak, no map move | stays in modal | `InputHandlerTest.talentSlotChoiceIgnoresNonContractKeys` |
| `TALENT_ASSIGN` without active slot modal | `1..4` | `null` | no selection change | stays `TALENT_ASSIGN` | `InputHandlerTest.talentTreeSidebarIgnoresNumberHotkeysOutsideActiveSlotChoice` |

Modal trigger ownership is fixed: PR04 client code does not decide gameplay legality from "four slots full + ACTIVE/SUSTAINED" on its own. `ACTIVE_TALENT_SLOT_CHOICE` opens only when upstream snapshot / overlay state signals that slot choice is required after the learn/reserve draft flow. The upstream signal is `RenderUiStateSnapshot.activeTalentSlotChoiceRequirement: TalentActiveSlotChoiceRequirementSnapshot?`, produced by the game/session boundary from the current talent draft and active-slot loadout. Presenter may derive strip/modal presentation from this typed snapshot field; it must not run a second learnability or slot legality calculation in `InputHandler`.

### 3.2 Active slot strip contract

Reference image primary state只显示右侧 action block：`Enter 学习`、`R 预留`、`Esc 返回`。当 active/sustained talent 需要选择槽位时，PR04 使用同一 dark panel style 打开局部 active slot choice modal；不得引入不同视觉语言的复杂窗口。

新增 client presentation model：

```kotlin
enum class TalentActiveSlotStripState {
    FILLED,
    EMPTY,
    PENDING_DIRECT_FILL,
    PENDING_REPLACE_TARGET,
}

data class TalentActiveSlotStripItem(
    val slot: Int,
    val talentId: String?,
    val primaryIconKey: String?,
    val primaryLabel: String,
    val secondaryLabel: String?,
    val state: TalentActiveSlotStripState,
    val focused: Boolean,
)
```

规则：

1. slot count 固定 `PLAYER_ACTIVE_TALENT_SLOT_COUNT = 4`，PR04 不改该常量。
2. slot order 固定 `1..4`；renderer 直接用 `slot` 渲染数字角标，不新增 `hotkeyDigit`。
3. filled slot 来源为 `RenderUiStateSnapshot.talents.slot`。
4. empty slot = `1..4 - talents.slot`；四槽未满时 pending active/sustained talent confirm 不打开 `ACTIVE_TALENT_SLOT_CHOICE`，strip 以 `PENDING_DIRECT_FILL` 标出目标空槽。
5. 当 upstream snapshot / overlay state indicates slot-choice-required for a pending active/sustained talent, open active slot choice；modal strip 中 1-4 slots 全部使用 `PENDING_REPLACE_TARGET` 表示可替换目标。`focused` 只表达当前键盘焦点，不表达是否可替换。
6. passive draft never sets pending active slot state and never opens `ACTIVE_TALENT_SLOT_CHOICE`。
7. 当 `activeSlotChoiceModal != null` 时，strip reserve hint 必须为 `null`，避免与 modal 的 `RESERVE_ACTION` 行重复。modal 关闭后由 presenter 重新填充。
8. pending 不能再作为独立 Boolean 字段；pending 语义由 `state=PENDING_DIRECT_FILL / PENDING_REPLACE_TARGET` 与 `focused` 表达，避免 renderer 写出重复或矛盾状态。
9. Required evidence: `dark-uiux-pr04-active-slot-choice` covers four filled slots, pending replace affordance, reserve row and cancel footer。

### 3.3 Active slot choice visual model

Active slot choice modal 必须是 typed list，不是四行文本加一条 footer。参考图里的 `R 预留` 在 K-ToME 中对应 `ConfirmTalentDraftToReserve`，PR04 必须把它作为第 5 个可见 item 呈现。

```kotlin
enum class ActiveSlotChoiceModalItemKind {
    SLOT_FILLED,
    SLOT_EMPTY,
    SLOT_REPLACE_TARGET,
    RESERVE_ACTION,
}

data class ActiveSlotChoiceModalModel(
    val title: String,
    val items: List<ActiveSlotChoiceModalItem>,
    val cancelHintText: String,
)

data class ActiveSlotChoiceModalItem(
    val hotkeyText: String,
    val kind: ActiveSlotChoiceModalItemKind,
    val slot: Int?,
    val iconKey: String?,
    val primaryLabel: String,
    val secondaryLabel: String?,
    val focused: Boolean,
)
```

`ActiveSlotChoiceModalModel.items` 长度固定为 5：

```kotlin
items = [
    slot 1 (kind in SLOT_FILLED / SLOT_EMPTY / SLOT_REPLACE_TARGET),
    slot 2 (kind in SLOT_FILLED / SLOT_EMPTY / SLOT_REPLACE_TARGET),
    slot 3 (kind in SLOT_FILLED / SLOT_EMPTY / SLOT_REPLACE_TARGET),
    slot 4 (kind in SLOT_FILLED / SLOT_EMPTY / SLOT_REPLACE_TARGET),
    reserve (kind = RESERVE_ACTION, slot = null),
]
cancelHintText = "Esc 取消"
```

Rules:

1. `SLOT_FILLED` shows existing active talent icon and rank/status label.
2. `SLOT_EMPTY` shows empty slot chrome and localized empty label.
3. Modal 打开时，所有原本 `SLOT_FILLED` 的 1-4 slot item 都升级为 `SLOT_REPLACE_TARGET`，表示它们都是可替换目标；`focused=true` 表示当前键盘焦点，默认只在 slot 1。Replacement chrome uses ember-gold edge; focus ring uses cyan and must be visually separate.
4. `RESERVE_ACTION` never has `slot`; it uses reserve marker/chrome from the same state language as the main panel.
5. `Esc` is a footer/hint, not a selectable slot item.
6. Input command truth remains §3.1; visual item ordering cannot introduce new commands.
7. Required tests: `TalentSidebarPresenterTest.pr04ActiveSlotChoiceModalMarksAllFourSlotsAsReplaceTargetsAndFocusedOnFirst`, `TalentSidebarPresenterTest.pr04ActiveSlotChoiceModalOnlyOpensWhenSnapshotSignalsSlotChoiceRequired`, `InputHandlerTest.activeSlotChoiceModalOpensWithSlotOneFocused`。

## 4. 前置检查与 Stop Condition

PR04 开工前必须先执行只读检查：

```bash
rg -n "TalentSidebarPresenter|TalentTreeNodeSnapshot|ACTIVE_TALENT_SLOT_CHOICE" client core game
rg -n "tier|prerequisite|talentPrereqs|TalentAssignPanelModel|TalentAssignTreeRowModel|TalentTreeEdgeProjection" core game client
rg -n "ui.frame.modal.body|ui.frame.tooltip.body|icon.skill|icon.tree" UI/pr UI/sprite-sheets client assets-src
```

Tree foundation gate 必须在 UI layout 实现前跑；这是 gameplay data gate，不是 renderer gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests com.ktome.game.data.VanguardTreeTest --tests com.ktome.game.data.ArcanistTreeTest --tests com.ktome.game.data.RogueTreeTest --tests com.ktome.game.data.TemplarTreeTest --tests com.ktome.game.data.TalentSchemaTest :tools:contractLint --tests com.ktome.tools.lint.Phase2ContentCoverageTest
```

1. `TalentSidebarPresenter`、`TalentTreeNodeSnapshot`、`ACTIVE_TALENT_SLOT_CHOICE`、`phase4-v4-pr01` scenario 都存在，且 §2.6 data foundation gate 通过：继续本 PR，只做 UI chrome / layout / presentation。
2. 任一上游 gameplay contract 不存在：停止 PR04，先合入或切到上游职业树分支。
3. release playable 职业缺树、缺节点、tree shape 漂移且没有对应设计更新、前置边不可遍历、tier2 / tier3 prerequisite rank 不足：停止 PR04，先修 `game` 数据和测试；对应 acceptance requirement `UI04-M00a-tree-data-foundation` / `UI04-M00b-tree-coverage-audit`，不得用 UI fallback 掩盖。
4. frozen profession 的空 tree 被 runtime 当成可玩完整树：停止 PR04，先修 profession unlock / selection / snapshot owner 口径；UI 只允许显示 locked/frozen fallback。
5. frame key 或 skill icon runtime manifest 无法解析：先确认是否 PR02 frame owner 或 PR06 icon rebaseline owner；PR04 不能裸写 raw path。
6. presentation 缺字段但可以从现有 `TalentTreeNodeSnapshot` / `TalentTreeSnapshot` / typed schema projection 纯投影得到：在 `client` presentation boundary 补字段；不在 `core/game` 重算 learnability。
7. prerequisite connector 缺 typed source：停止 connector 实现，先在上游 snapshot/schema projection 中补 prerequisite ids，或把 connector drawing 明确降级为 deferred；禁止 renderer 从 localized lock reason 文本猜线。
8. 禁止为了 UI 临时 mock 第二套 talent snapshot、第二套 tree owner 过滤、第二套 state marker authority 或第二套 prerequisite authority。

## 5. 影响范围与实现顺序

### 5.1 Added / Modified / Deleted

| Kind | Path | Owner | Input | Output | Failure semantics | Test / evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Modified | `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt` | `client` | `RenderUiStateSnapshot`, `OverlayState`, typed schema projection | `TalentAssignPanelModel` with header, list sections, rows, detail blocks, legend and modal items | no rule recomputation; no localized-text parsing; no renderer-derived prerequisite | `TalentSidebarPresenterTest.pr04BuildsTalentAssignPanelModel` |
| Added | `client/src/main/kotlin/com/ktome/client/ui/talent/TalentAssignPanelModel.kt` | `client` | presenter projections | typed talent assign panel model family | single presentation family; no parallel renderer authority | `TalentSidebarPresenterTest.pr04RowsFollowSnapshotOrderAndPrerequisiteIndentation` |
| Added | `client/src/main/kotlin/com/ktome/client/ui/talent/TalentTreeSelectionIdentity.kt` | `client` | selected `TalentTreeNodeSnapshot` | client-only selection identity | no rule recomputation; not serialized into save/replay | `InputHandlerTest.talentTreeSelectionRestoresByIdentityWhenSnapshotOrderChanges` |
| Modified | `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt` | `client` | modal-local talent selection | `talentTreeSelectionIdentity` persisted with modal frame | flat index is cache only; identity restore wins | `InputHandlerTest.talentTreeSelectionRestoresByIdentityWhenSnapshotOrderChanges` |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | `client` | `TalentAssignPanelModel` | render model with panel bounds, row bounds, detail bounds, modal bounds, hit regions | fallback icon renders crash-safe, but evidence passes only when manual record cites fallback and PR06 owner; no state/prerequisite remap | `TileRendererCanvasTest.darkUiuxPr04DrawsStateMarkerSkillIconNameRankAndSelectedRow` |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | `client` | render model | draw reference-like panel, list sections, row icons, connector prefixes, right detail pane, legend and modal without overlap | stable row height; truncate long text; no bottom-log or right-companion overlap at min window | `TileRendererCanvasTest.darkUiuxPr04TalentAssignPanelDoesNotCoverRightCompanionOrBottomLog` |
| Modified | `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` | `client` | modal stack + key input | typed `PlayerCommand` for active slot choice | no number hotkey leak outside active slot modal | `InputHandlerTest.activeSlotChoice*`, `InputHandlerTest.talentTreeSidebarIgnoresNumberHotkeysOutsideActiveSlotChoice` |
| Modified | `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt` | `client` | existing talent tone tokens | consume `talent-locked/learnable/reserve/active`, selected row cyan, panel borders | no new color family unless style bible changes | `TileRendererCanvasTest.darkUiuxPr04UsesTalentToneTokensForFourStates` |
| Modified | `client/src/test/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenterTest.kt` | `client` | synthetic snapshot | state/category/race/detail assertions | fail on renderer-only marker, missing skill icon or missing current-rank detail | focused test |
| Modified | `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt` | `client` | modal stack fixtures | `1-4/R/Esc` command assertions | fail on command leak or stale modal | focused test |
| Modified | `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | `client` | deterministic canvas snapshot | icon draw, tone, layout and overlap assertions | fail on overlap, text order drift or missing icon draw | focused test / golden |
| Added | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | `docs` | generated whitebox evidence | PR04 manual evidence | required unless §8 skip rule is explicitly triggered | manual record |
| Deleted / adapter-only | `TalentSidebarLine` formal renderer authority | `client` | existing sidebar output | no formal PR04 renderer authority | if kept, adapter/debug only with §9 exit rule | `TalentSidebarPresenterTest.talentAssignPanelModelDoesNotMixNodeAndNonNodeStateFields` |

`TalentAssignPanelModel.kt` 可以承载本文档列出的 model family。若实现选择按 model family 拆分到多个文件并超过 5 个新 Kotlin 文件，必须跑 `maintainabilityLint` 并在 self-audit 记录文件数。

### 5.2 Implementation Order

1. Preflight upstream contracts：确认 §2.6 tree data foundation gate、PR03 close/base、`phase4-v4-pr01` scenario、PR02 frame resolver test、screen coverage labels。
2. Freeze reference fidelity：把 `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png` 的 panel proportions、row grammar、detail ordering、bottom legend 写入 renderer model tests。
3. Extend presenter contract：新增 `TalentAssignPanelModel` family，输出 header、sections、rows、connector prefix、detail blocks、legend、active slot choice modal items；正式 row 文本中的 marker 由 typed state 生成。
4. Add identity model：新增 `TalentTreeSelectionIdentity`，让 `InputHandler` / `ModalFrameLocalState` 按 identity 恢复 selection，flat index 只作为 cache。
5. Add active slot modal model：新增 `TalentActiveSlotStripItem` / `TalentActiveSlotStripState` / `ActiveSlotChoiceModalItem`，固定 replacement affordance、reserve 第五行和 `Esc` footer。
6. Add presenter/input tests：先补四态、category/state 分离、race tree 不过滤、prerequisite indentation、rank/current detail、selection identity、modal command 和 non-contract key 黑名单测试。
7. Update render model and renderer：把 `TalentAssignPanelModel` 映射到 renderer model；画 title、points、section headers、state markers、skill icons、connector prefixes、selected row、right detail pane、bottom legend 和 modal；保证 focus ring 不改变 state identity。
8. Add canvas tests：覆盖 reference structure、skill icon draw、right current detail ordering、active slot modal reserve row、right companion post-close restoration、min-window compact readability、long zh text truncation。
9. Run resource dependency checks：确认 frame/icon resolver；如果 fallback 出现在必验截图，manual record 必须标为 residual risk 并指向 PR06。
10. Refresh golden and whitebox：刷新 `dark-uiux-pr04-*` labels；PR04 manual record 引用 upstream generated runbook / expected evidence，并填写 §8.2 dark label mapping。
11. Final self-audit：按 §10 对 doc-vs-implementation、reference fidelity、cross-PR evidence、removal/rebaseline owner 和未运行项逐条确认。

## 6. UI 改造范围

### 6.1 Canonical panel layout

Reference viewport `1280x840` 下的主布局固定：

| Region | Reference rule | Implementation rule |
| --- | --- | --- |
| modal panel | 近 90% width / height；dark brown-black body，thin iron border，minimal corner ornament | renderer 可以按 viewport 缩放，但 canonical golden 必须保留同等比例和边框层级 |
| title | 左上 `天赋分配`，ember-gold | presenter/localizer 输出 title；renderer 不硬编码中文 |
| points line | `职业天赋点: 3     种族天赋点: 1` | 职业点和种族点固定在 title 下方；不能藏到 description |
| left list | 约 58% width；三系全展开；row = marker + icon + name + rank | 不使用 graph/grid/cards；connector prefix 使用 typed prerequisite projection |
| right detail | 约 42% width；hero icon + header + current detail + next preview + actions | `CURRENT_RANK_DETAIL` 必须在 `NEXT_RANK_PREVIEW` 上方 |
| footer | 左侧 keyboard help，右侧 legend | help/legend 固定可见；overflow 不能挤走 footer |

### 6.2 Left list tree

必须展示 exactly three Vanguard example sections in canonical reference evidence:

```text
武器系 6/6
[*] [icon] 猛击 1/5
  └─ [+] [icon] 横扫 0/5
      └─ [x] [icon] 破阵 0/5
          └─ [x] [icon] 震地猛击 0/5
[*] [icon] 冲锋 1/5
  └─ [x] [icon] 碎甲 0/5

护卫系 5/5
[*] [icon] 盾击 1/5
  └─ [+] [icon] 嘲讽 0/5
[*] [icon] 格挡姿态 1/5
  ├─ [r] [icon] 铁壁 1/5
  └─ [x] [icon] 壁垒推进 0/5

战吼系 5/5
[*] [icon] 战吼 1/5
  ├─ [+] [icon] 集结战旗 0/5
  │   └─ [x] [icon] 战场号令 0/5
[*] [icon] 威压 1/5
  └─ [x] [icon] 不屈 0/5
```

This section is a canonical visual target, not a rule source. Actual runtime labels / counts come from snapshot and locale. If runtime selected profession is not Vanguard, the same row grammar and proportions apply to that profession's trees. Section `6/6` style counts mean visible nodes over total nodes, while row `1/5` style ranks mean current rank over max rank; presenter and renderer must keep those two meanings visually separated through section-header tone and spacing.

### 6.3 Skill icons and state markers

1. 每个 `TALENT_NODE` row 必须绘制 small skill icon。无 icon 的 node 必须使用 manifest fallback icon，并在 evidence 中标记 fallback；不能留空洞。
2. `stateMarkerText` uses `[+] / [x] / [*] / [r]` in zh-CN canonical evidence and appears before skill icon.
3. State marker tone:
   - `[+]` learnable: cyan
   - `[x]` locked: muted gray
   - `[*]` learned active: green
   - `[r]` learned reserve: ember-gold
4. Selected row is a single cyan horizontal highlight bar. It must not turn the row into a large card.
5. Row height is stable; icon/hover/focus/rank changes cannot resize rows or shift following rows.

### 6.4 Detail pane blocks

Preview / detail pane 使用 fixed ordered blocks。Presenter must output blocks in this exact order, and renderer must not reorder:

```kotlin
enum class TalentDetailBlockKind {
    HERO_ICON,
    HEADER,
    RANK_AND_COST,
    PREREQUISITE,
    PREREQUISITE_FAILED,
    CURRENT_RANK_DETAIL,
    NEXT_RANK_PREVIEW,
    ACTIONS,
}

enum class TalentPreviewToneToken {
    PRIMARY,
    SECONDARY,
    POSITIVE,
    WARNING,
    LOCKED,
}

data class TalentDetailPaneModel(
    val talentId: String,
    val blocks: List<TalentDetailBlock>,
)

data class TalentDetailBlock(
    val kind: TalentDetailBlockKind,
    val iconKey: String?,
    val primaryText: String,
    val secondaryText: String?,
    val bodyLines: List<TalentPreviewLine>,
    val toneToken: TalentPreviewToneToken,
)

data class TalentPreviewLine(
    val label: String?,
    val value: String,
    val iconKey: String?,
    val toneToken: TalentPreviewToneToken,
)
```

Block field mapping is fixed. Presenter tests must assert the fields below, not only the rendered text order.

| Block kind | `iconKey` | `primaryText` | `secondaryText` | `bodyLines` |
| --- | --- | --- | --- | --- |
| `HERO_ICON` | selected talent icon key, required when selected talent has icon | empty string | null | empty |
| `HEADER` | null | talent display name | localized state chip, e.g. `可学习` / `已激活` / `已预留` / `已锁定` | empty |
| `RANK_AND_COST` | optional resource / point icon | rank transition from shared rank formatter, e.g. `等级 1 → 等级 2` | point cost and owner point type, e.g. `消耗 1 职业天赋点` | empty |
| `PREREQUISITE` | null | localized prerequisite header, e.g. `需学习前置` | null | one line per prerequisite: `label=前置技能名`, `value=等级要求` |
| `PREREQUISITE_FAILED` | null | localized failed prerequisite header, e.g. `未满足前置条件` | null | missing-rank breakdown lines when available |
| `CURRENT_RANK_DETAIL` | null | `当前等级详情（预览 1级）`, `当前等级详情（当前 X级）`, or `当前等级详情（预览 X级）` | null | one labeled line per available field: type/category, range/target, area/radius, resource cost, cooldown, coefficient/value, special status/knockback/teleport/slot note |
| `NEXT_RANK_PREVIEW` | null | `下一等级预览` or max-rank state title | null | delta lines only, including next breakpoint/unlock when available |
| `ACTIONS` | null | empty string | null | one line per current selected talent action: `Enter`, `R`, `Esc`, or disabled equivalents |

`TalentPreviewLine.label` is a localized short label such as `范围`, `冷却`, `伤害`, `状态`, or `Enter`; `value` is the localized value such as `5格`, `6回合`, `120% 武器伤害`, or `学习`；`iconKey` is only used when an existing manifest icon is available and must not become a second source of action/state truth.

`CURRENT_RANK_DETAIL` rules:

1. 未学习节点显示“当前等级详情（预览 1级）”。
2. 已学习节点显示“当前等级详情（当前 X级）”。
3. Pending allocation uses preview rank if `rank != committedRank` and labels it as preview.
4. Required fields when data exists: type/category, range or target, area/radius, resource cost, cooldown, primary coefficient/value, special status/knockback/teleport/slot note.
5. Source composition is layered but non-duplicating. Presenter always projects the reference-style structured summary fields from typed snapshot/model data (`category`, `range`, `minRange`, `descriptionModel.radius`, `resourceCost`, `resourceLabelKey`, `maxCooldown`) when those fields exist, then calls `DescriptionPresenter.presentSurfaceLines(localizer, node.descriptionModel, TALENT_ACTIVE)` for the effect/description lines. If `DescriptionPresenter` returns no effect lines, the same structured summary remains the complete current-detail fallback. The two paths must not append duplicate effect/description lines.
6. Renderer cannot calculate formula text or coefficients.
7. Required tests: `TalentSidebarPresenterTest.pr04DetailBlockPrimaryAndBodyFieldsMatchKindTable`, `TalentSidebarPresenterTest.pr04CurrentRankDetailPrefersDescriptionPresenterOverRawSchemaFields`。

`NEXT_RANK_PREVIEW` rules:

1. Only shows delta from investing one additional point and next breakpoint/unlock.
2. Cannot replace `CURRENT_RANK_DETAIL`.
3. At max rank, show max-rank state instead of fake next rank.
4. Damage / value deltas come from `TalentTreeNodeSnapshot.nextRankDescriptionModel`, generated by `game` from the same typed `TalentDef` / `levelEffects` authority as `descriptionModel`; renderer and presenter must not hardcode reference-image numbers.

Flavor text is not part of the canonical reference. If a future implementation adds flavor, presenter may add it only when a non-empty locale value exists and must keep it below required effect data. Renderer must not fall back to generic flavor text.

### 6.5 Overflow, scroll and pointer boundary

Overflow 必须稳定，不得靠窗口尺寸挤压 row 到不可读：

| Case | Behavior |
| --- | --- |
| tree count `<= 3` profession trees | canonical reference evidence 全部展开显示 |
| tree count `> 3` 或 race tree 导致超高 | left list viewport 启用垂直滚动；title、points、right detail、bottom legend 不被滚出可见区 |
| long localized talent name | row text truncate with ellipsis or move full text to detail pane；row width/height 不变 |
| min-window | 优先保留 selected row、right current detail、footer actions、bottom legend；非 selected section 可裁剪但必须有 scroll affordance |

Pointer boundary:

1. Keyboard-first remains mandatory.
2. Tree row 如果进入 `TileRenderModel` hit region，则左键点击等价于移动 focus 到该 row；第二次确认仍走 `Enter/Space` 或既有 confirm input，不新增隐藏命令。
3. Active slot choice modal 的 pointer 点击只允许命中 typed modal item；命中 slot item 等价对应 `1-4`，命中 reserve item 等价 `R`，命中 cancel footer 等价 `Esc`。
4. 如果当前 hit region 基础设施不足，PR04 必须在 self-audit 写 `mousePointerMode=deferred`、保留 keyboard-first evidence，并把 pointer support 挂到 PR07 polish；不得在 renderer 内临时读取 Gdx pointer 坐标。

### 6.6 Legend

Bottom legend uses explicit item kind:

| Legend item | Kind | Source |
| --- | --- | --- |
| `[+] 可学习` | `STATE_TONE` | `TALENT_LEARNABLE` |
| `[x] 锁定` | `STATE_TONE` | `TALENT_LOCKED` |
| `[*] 已激活` | `STATE_TONE` | `TALENT_ACTIVE` |
| `[r] 预留` | `STATE_TONE` | `TALENT_RESERVE` |
| current row highlight | `FOCUS` | selected row chrome |
| pending chrome if present | `PENDING_OVERLAY` | present only when any visible row has `pendingOverlay=true` |

Required test: `NEW: TalentSidebarPresenterTest.pr04LegendIncludesFourStateToneAndFocusAndIncludesPendingOnlyWhenAnyRowIsPending`。

### 6.7 Co-existence Layout

PR04 primary evidence is the isolated Talent Assign panel from the canonical image. Runtime integration must still preserve PR-02-1 / PR-03 shell state around the full-screen Talent Assign flow:

1. Talent Assign 打开时以 canonical full-screen panel 为准，不把 PR03 right companion 混入主参考画面。
2. Talent Assign 关闭后，PR03 right companion panel、bottom log 和 shell hotbar 必须恢复且未被 PR04 状态污染。
3. Active slot choice modal anchored to talent assign panel；不能浮到 inventory grid 上方。
4. Min-window evidence 必须证明 Talent Assign 自身 footer / legend / selected row 和 right current detail 在 compact viewport 下仍可读，不出现 panel 内部重叠。
5. Manual record 追加 `dark-uiux-pr04-right-companion-coexistence` 截图，用于证明退出 Talent Assign 后 right companion 与 bottom log 恢复；旧 `dark-uiux-pr04-coexistence-with-equipment-and-inventory` 名称只作为 alias。

## 7. Cross-PR Dependency

| PR04 requirement | Dependency | Type | Verification |
| --- | --- | --- | --- |
| `UI04-M00a-tree-data-foundation` | profession/talent schema data can form traversable trees | hard upstream | `VanguardTreeTest`, `ArcanistTreeTest`, `RogueTreeTest`, `TemplarTreeTest`, `TalentSchemaTest.\`profession tier two and tier three nodes declare specified prerequisite ranks\`` |
| `UI04-M00b-tree-coverage-audit` | content floor coverage and non-frozen profession route checks | hard upstream | `Phase2ContentCoverageTest.\`phase2 content floor covers four professions route zones and minimum content matrix\`` through `:tools:contractLint` |
| `UI04-M01-reference-fidelity` | canonical generated reference image exists and is repo-relative | hard PR04 | image path, prompt path and sha256 in §0.0 and PR description |
| `UI04-M02-presenter-authority` | `phase4-v4-pr01` | hard upstream | scenario registered; `TalentTreeNodeSnapshot.state/category/iconKey/descriptionModel` exist |
| `UI04-M03-list-tree-layout` | typed talent prerequisite projection | hard upstream or PR04 preflight stop | presenter can output indentation and connectors without renderer inference |
| `UI04-M04-render-panel-model` | PR02 frame / text / icon resolver | hard upstream | `manifestLint`, renderer canvas tests |
| `UI04-M09-resource-dependency` | existing skill icons before PR06 | allowed temporary dependency | fallback is visible but not passing evidence unless recorded; PR06 owns rebaseline |
| `UI04-M09-resource-dependency` | PR02 owner test rename, if not already done | hard upstream / PR02 owner | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`; PR04 must not rename PR02 owner tests inside PR04 scope |
| `UI04-M11-manual-label-capture` | `UI04-M10-upstream-whitebox-materialization` | hard upstream | manual capture labels map to upstream runbook and actual screenshot paths |
| `UI04-M08-race-tree-no-filter` | `RenderUiStateSnapshot.talentTrees` includes profession + race when present | hard upstream | `TalentSidebarPresenterTest.rendersRaceTreeRowsWithoutFilteringSnapshotTrees` |
| `UI04-M12-min-window-manual-record` | PR01 / PR01-1 shell min-window | hard upstream | use same minimum viewport family; add PR04 label |
| `UI04-M16-removal-and-rebaseline-plan` | PR06 `dark-uiux-pr06-talent-icon-rebaseline` | downstream rebaseline | PR06 must refresh PR04 talent assign panel / active slot golden after skill/tree icon replacement |
| `UI04-M16-removal-and-rebaseline-plan` | PR07 `dark-uiux-pr07-final-all-screens` | downstream audit | PR07 final index must link PR04 labels and show no old-style residue |

PR04 branch base must be after PR03 close. If implementation deliberately starts from a different base, the PR description and §10 self-audit must state the exception, branch/base ref, and why no PR03 conflict exists.

## 8. 验证与白盒

### 8.1 Gate ladder

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests com.ktome.game.data.VanguardTreeTest --tests com.ktome.game.data.ArcanistTreeTest --tests com.ktome.game.data.RogueTreeTest --tests com.ktome.game.data.TemplarTreeTest --tests com.ktome.game.data.TalentSchemaTest :tools:contractLint --tests com.ktome.tools.lint.Phase2ContentCoverageTest
./gradlew acceptanceContractLint
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.screen.ValidationScenarioBootstrapTest :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest
./gradlew manifestLint contractLint localeLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint
./gradlew verifyChanged
```

`Phase2ContentCoverageTest` 必须通过 `:tools:contractLint` 运行；`Phase4V4WhiteboxScenarioCliTest` 通过 `:tools:test` 运行。两者 owner 和用途不同，不互相替代。

### 8.2 Whitebox preparation

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-profession-tree-ui
```

`preparePhase4V4Whitebox` generated output 不提交；PR04 manual record 必须记录以下字段：

| Field | Required value |
| --- | --- |
| manual record path | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` |
| upstream scenario | `dark-uiux-pr04-profession-tree-ui` |
| generated runbook | `build/whitebox/dark-uiux-pr04-profession-tree-ui/cua-runbook.md` |
| generated expected evidence | `build/whitebox/dark-uiux-pr04-profession-tree-ui/expected-evidence.json` |
| runtime home | `build/whitebox/dark-uiux-pr04-profession-tree-ui/runtime-home` |
| evidence dir | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence` |
| platform | `macOS` / `Linux` / `Windows`, exact runtime platform |
| app executable hash | macOS: `.app/Contents/MacOS/<executable>` sha256；Linux: `bin/<executable>` sha256；Windows: `<executable>.exe` sha256；manual record 必须记录 platform，sha256 只在同 platform 内可比 |
| screenshot sha256 evidence type | PNG file sha256, computed with `shasum -a 256 <path>` on macOS/Linux or `Get-FileHash <path> -Algorithm SHA256` on Windows |
| PR04 screenshot labels | `dark-uiux-pr04-talent-assign-panel-start`, `dark-uiux-pr04-active-slot-choice`, `dark-uiux-pr04-talent-assign-min-window-log-visible`, `dark-uiux-pr04-right-companion-coexistence` |
| legacy aliases | `dark-uiux-pr04-talent-sidebar-start`, `dark-uiux-pr04-talent-sidebar-min-window-log-visible`, `dark-uiux-pr04-coexistence-with-equipment-and-inventory` |
| required gameplay log keys | `N/A` for PR04 visual whitebox; upstream gameplay baseline remains `phase4-v4-pr01` |
| locale | exact runtime locale, default `zh-CN` unless implementation explicitly validates another locale |
| viewport | exact capture viewport; min-window evidence uses PR01/PR01-1 canonical min-window token / actual runtime value, not a hard-coded local assumption |
| residual risk | explicit `N/A` or concrete limitation |

`preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-profession-tree-ui` 生成 PR04 dark labels 的 runbook 和 expected evidence。Manual capture must save the PR04 files under that evidence dir and record this mapping:

| PR04 label | Required screenshot path | Evidence type | Source upstream evidence / step | Required proof |
| --- | --- | --- | --- | --- |
| `dark-uiux-pr04-talent-assign-panel-start` | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr04-talent-assign-panel-start.png` | manual packaged-app capture | PR04 Talent Assign first capture | screenshot sha256, viewport, locale, visible state marker, skill icons, current-rank detail above next preview; at least three profession sections visible, and if the capture run includes a race tree it must also be visible or reachable through the same list scroll |
| `dark-uiux-pr04-active-slot-choice` | `client/build/reports/golden/dark-uiux-pr04/dark-uiux-pr04-active-slot-choice.png` | automated golden; packaged capture may be added when key routing is reliable | PR04 active slot choice capture | screenshot sha256, `1-4/R/Esc` visible, four filled slots + replacement affordance + reserve row; golden must assert this image differs from panel-start |
| `dark-uiux-pr04-talent-assign-min-window-log-visible` | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr04-talent-assign-min-window-log-visible.png` | manual packaged-app capture | manual resize from Talent UI state | screenshot sha256, exact viewport, selected row/current detail/footer/legend remain readable and not overlapped |
| `dark-uiux-pr04-right-companion-coexistence` | `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr04-right-companion-coexistence.png` | manual packaged-app capture | Talent Assign close / right companion restoration capture | screenshot sha256, equipment/inventory/status companion and bottom log are visible after leaving Talent Assign |

Golden/canvas evidence may use the same semantic labels under `client/build/reports/golden/`, but manual screenshot paths and golden report paths must not be conflated in the manual record.

If a future PR adds `prepareDarkUiuxWhitebox`, it may replace the manual mapping table only after it produces the same `dark-uiux-pr04-*` paths and preserves the upstream `phase4-v4-pr01` traceability.

Skip rule：`whitebox=required` 默认必须执行 packaged app whitebox。只有当前机器无法启动 packaged app、CUA 不可用、或用户明确取消人工白盒时才允许 skip；manual record 必须写明原因、替代证据、未覆盖风险和后续 owner。不能用 `goldenScreenshot` 静默替代 packaged app whitebox。

### 8.3 Manual whitebox steps

1. `dark-uiux-pr04-profession-tree-ui` 场景下打开 Talent UI，首屏状态以 generated `expected-evidence.json` 为准；人工记录必须覆盖 learned starter、learnable、locked、reserve/active 状态、skill icon、selected row、当前等级详情与下一等级预览。
2. 学习 rank 0 技能时预览能解释点数消耗、rank before/after、当前 1 级效果和 breakpoint。
3. 对同一 talent 继续投入直到 active slot choice evidence 被达成；PR04 visual whitebox 不要求 gameplay log key，upstream gameplay log 仍由 `phase4-v4-pr01` manual record 覆盖。
4. 四槽满时 active-slot modal 的 `1-4 / R / Esc` 文案、焦点和输入行为正确；每个输入都要能在 command/log/evidence 中追溯。
5. 新 UI 不改变 `log.talent.learned / rank_up / breakpoint_chosen` 的产生条件。
6. 窄窗口验收使用 PR01/PR01-1 最小窗口口径；manual record 必须写实际 viewport 和来源，不能把某个本机尺寸当长期合同。
7. 窄窗口 expected：Talent Assign compact viewport keeps selected row、right current detail、footer actions and bottom legend readable without internal overlap；bottom log restoration is covered by the post-close right companion evidence。
8. 必填证据：canonical reference image path + sha256、`dark-uiux-pr04-talent-assign-panel-start`、`dark-uiux-pr04-active-slot-choice`、`dark-uiux-pr04-talent-assign-min-window-log-visible`、`dark-uiux-pr04-right-companion-coexistence`、PR04 generated runbook / expected evidence。

Manual record 的 log evidence table 必须使用以下字段：

| logKey | source | evidencePath | sha256 | residualRisk |
| --- | --- | --- | --- | --- |
| `log.talent.learned` | `phase4-v4-pr01-generated` | upstream gameplay manual record | upstream-only | PR04 visual whitebox records `N/A` |
| `log.talent.rank_up` | `phase4-v4-pr01-generated` | upstream gameplay manual record | upstream-only | PR04 visual whitebox records `N/A` |
| `log.talent.breakpoint_chosen` | `phase4-v4-pr01-generated` | upstream gameplay manual record | upstream-only | PR04 visual whitebox records `N/A` |

## 9. Removal / Iteration Plan

| Temporary surface | Allowed in PR04 | Removal / rebaseline owner | Regression scan | Exit rule |
| --- | --- | --- | --- | --- |
| `dark.uiux.pr04.talent.vanguard.*` reference-crop evidence icons | yes, only for canonical PR04 Talent Assign fidelity | PR06 `dark-uiux-pr06-talent-icon-rebaseline` | `resourcePipelineLint`, `manifestLint`, `goldenScreenshot`, manual record | PR06 replaces evidence crops with formal dark-v1 skill/tree resources and refreshes PR04 golden |
| `dark.uiux.pr04.talent_assign.chrome.*` reference-crop modal chrome evidence | yes, only for canonical PR04 Talent Assign frame/corner fidelity | PR06 formal dark-v1 frame rebaseline | `resourcePipelineLint`, `manifestLint`, `goldenScreenshot`, manual record | PR06 replaces scoped chrome crops with formal shared frame resources when the common frame sheet is rebaselined |
| existing `icon.skill.*` / `icon.tree.*` / `tree.*` / `portrait.*` painterly assets in talent UI | yes | PR06 `dark-uiux-pr06-talent-icon-rebaseline` | `goldenScreenshot`, manifest coverage, `screen-coverage-matrix` final audit | PR06 refreshes `dark-uiux-pr04-talent-assign-panel-start` and `dark-uiux-pr04-active-slot-choice`; PR07 final index shows no old-style residue |
| legacy screenshot labels `dark-uiux-pr04-talent-sidebar-*` | alias only | PR07 final all-screens audit | `screen-coverage-matrix`, manual record | canonical labels use `talent-assign-*`; aliases only preserve traceability for downstream docs |
| `TalentSidebarLine` formal renderer authority | no | PR04 | `TalentSidebarPresenterTest.talentAssignPanelModelDoesNotMixNodeAndNonNodeStateFields` | renderer consumes `TalentAssignPanelModel`; if `TalentSidebarLine` remains, it is adapter/debug only and not golden authority |
| `TalentSidebarLine.selected: Boolean` single-field focus | yes as legacy adapter only | PR04 | presenter focused tests | new code path uses `focused` on typed rows and modal items; legacy boolean cannot own selection identity |
| state marker `[x] [+] [r] [*]` as renderer-derived text | no | PR04 | `TalentSidebarPresenterTest.pr04BuildsTalentAssignPanelModel` | marker text is presenter output from typed state; renderer never derives or parses it |
| missing skill icon fallback | crash-safe only, not passing evidence unless recorded | PR06 resource owner | `manifestLint`, golden/manual screenshot review | fallback in required screenshots must be recorded as residual risk and assigned to PR06 |
| `talentTreeSelection: Int` flat index | yes as internal cache | PR04 | selection identity focused tests | flat index no longer owns selected identity; `TalentTreeSelectionIdentity` restore wins after snapshot changes |
| missing typed prerequisite projection | no for connector drawing; yes for no-connector fallback only with explicit stop/deferral record | upstream v4 snapshot/schema projection or PR04 preflight | `TalentSidebarPresenterTest.pr04ConnectorPrefixComesFromTypedPrerequisites` or `TalentSidebarPresenterTest.pr04RowsHaveEmptyConnectorPrefixWhenPrerequisiteSourceDeferred` | renderer never infers connectors from lock reason text; if deferred, self-audit must cite this row and PR07/owner exit rule |
| fifth `EXCLUDED_BY_BRANCH` / mutual exclusion state | no | future gameplay/snapshot owner if mutually exclusive branches are introduced | enum/snapshot tests, legend/state tests | PR04 keeps four-state contract; no client-only excluded state |
| full reserve skill list modal | no; PR04 only renders `R` reserve action row inside active slot choice modal | future UI owner if reserve management needs dedicated list | active slot modal tests, reserve talent presentation tests | reserve management remains accessible through existing reserve presentation and modal `RESERVE_ACTION` |
| pointer click support for talent rows | yes only if hit regions are produced from render model; otherwise explicit `mouse=deferred` self-audit entry | PR04 if implemented, PR07 polish if deferred | `InputHandlerTest.talentTreeMouseClickSelectsNodeWhenHitRegionExists` or `InputHandlerTest.talentTreeKeyboardOnlyWhenMouseDeferred` | keyboard-first evidence remains blocking; no renderer-side direct Gdx pointer reads |
| generated whitebox build outputs | no commit | PR04 | manual record references repo-relative generated paths and sha256 | generated files can be regenerated; manual record stores exact evidence |

## 10. Doc-vs-Implementation Self-Audit

PR04 收口前必须在 PR 描述或 review note 中逐条填写：

| Item | Required answer |
| --- | --- |
| base ref / target branch | branch base is after PR03 close, or exception with reason |
| unrelated dirty diff | list excluded files or `N/A` |
| canonical reference image | path, prompt path, sha256, and statement that no other UI reference overrides it |
| reference fidelity | `1280x840` golden/manual evidence matches title/points/list/detail/footer structure from reference image |
| upstream scenario | `phase4-v4-pr01` gameplay registry remains the upstream baseline; PR04 whitebox scenario `dark-uiux-pr04-profession-tree-ui` is generated and verified |
| frame/icon dependency | frame keys and skill icons resolved; fallback icons listed with PR06 owner or `N/A` |
| presentation authority | presenter owns exact `TalentAssignPanelModel` fields; renderer does not recompute state/prerequisite/owner |
| tree visual model | list tree only; no grid, tier band, column node graph or card layout |
| section header source | section headers are rendered from `TalentAssignSectionModel.displayName` + two spaces + `nodeCountText`; section headers are not rows and cannot receive focus |
| list-tree model | presenter owns row order, indent, connector prefix, state marker, skill icon, rank text and selected row |
| state marker mode | marker-text-only for PR04; `stateMarkerText` non-null and `stateIconKey=null` for all rows |
| prerequisiteConnectorMode | `implemented` with typed source and connector tests, or `deferred` with empty connector prefix test |
| prerequisite source | typed prerequisite source exists, or connector drawing is explicitly deferred with §9 row `missing typed prerequisite projection` cited as exit rule |
| modal state machine | `1-4/R/Esc` exact command tests passed |
| active slot modal trigger | modal opens only from upstream snapshot / overlay slot-choice-required signal; client does not recompute four-slot gameplay legality |
| active slot modal focus | all four filled slots are `SLOT_REPLACE_TARGET`; initial focus is slot 1; focus ring is separate from replacement chrome |
| selection identity | `TalentTreeSelectionIdentity` fields and snapshot restore tests passed |
| active slot modal item model | `1-4` slot rows, `R` reserve row and `Esc` cancel footer are rendered as typed modal surface |
| detail pane hierarchy | hero icon, header, rank/cost, prerequisite, current rank detail, next rank preview and actions match §6.4 |
| detail field mapping | every `TalentDetailBlockKind` uses the §6.4 primary/secondary/bodyLines table; `CURRENT_RANK_DETAIL` appears above `NEXT_RANK_PREVIEW` |
| detail source mode | `DescriptionPresenter.presentTalentTreeNodeLines` supplies complete detail lines, or raw snapshot/schema fallback is used alone; no duplicate mixed path |
| footer vs actions | `footerHints` is panel-level structured navigation help and does not duplicate selected talent `ACTIONS` |
| rank formatting | row rank text and `RANK_AND_COST` transition come from the same formatter/helper |
| race tree behavior | runtime does not filter or reorder `ownerType=RACE` trees |
| PR04 dark labels | `dark-uiux-pr04-*` label mapping table exists in manual record and points to actual screenshot paths / sha256 |
| min-window behavior | PR04 min-window label exists, viewport is recorded, and compact readability evidence covers selected row, current detail, footer actions and bottom legend |
| right companion coexistence | PR-03 equipment / inventory / status companion and bottom log are restored after leaving the full-screen Talent Assign panel |
| mousePointerMode | `implemented` through typed hit regions or `deferred` with keyboard-only test and residual risk |
| PR06 rebaseline | old icon surface linked to PR06 owner |
| PR07 final audit | PR04 labels listed for final all-screens index |
| validation not run | every skipped command/manual step has reason and residual risk |

### Claude Round 2 Review Disposition

| Finding | Verification result | Disposition | Contract anchor |
| --- | --- | --- | --- |
| A-1 focus enum cannot express tier + cell focus | confirmed against old doc; new reference has only row focus, no tier band | resolved by replacing grid focus with `focused: Boolean` on list rows | §2.2, §6.2 |
| A-2 pending duplicated in tone enum + boolean | confirmed | resolved: four state tones only; pending is `pendingOverlay` / strip state | §2.2, §3.2 |
| A-3 race tree placement conflicts with snapshot order | confirmed | resolved: client never reorders; race position owned by snapshot | §2.4 |
| A-4 `TalentSidebarLine` fate unclear | confirmed against current code and doc | resolved: formal authority moves to `TalentAssignPanelModel`; legacy line is delete/adapter-only with exit rule | §2.2, §5.1, §9 |
| A-5 / A-6 tierIndex and columnIndex undefined | confirmed in old grid doc | resolved by deleting grid/tier/column model; canonical UI is list tree | §2.3, §6.2 |
| A-7 edge state semantics undefined | confirmed | resolved with trigger/treatment table | §2.3 |
| A-8 preview ordering conflicts with reference | confirmed | resolved with fixed detail block order matching current reference image | §6.4 |
| A-9 prerequisite and failed prerequisite coexistence undefined | confirmed | resolved: prerequisite always lists requirement; failed appends warning when unmet | §6.4 |
| A-10 duplicated slot/hotkey fields | confirmed | resolved: strip item keeps only `slot`; modal item keeps display `hotkeyText` | §3.2, §3.3 |
| B-1 / B-2 owner label and repeated point chip ambiguity | confirmed | resolved: header owns profession/race point text; sections no longer duplicate owner point chip | §2.2, §6.1 |
| B-3 raw `ownerType: String` | confirmed in current snapshot DTO and old doc | resolved for new PR04 models by using `TalentTreeOwnerType`; snapshot parsing occurs at boundary | §2.2, §2.4 |
| B-4 selected/focused naming mixed | confirmed | resolved: new models use `focused` for keyboard focus; equipped/pending use separate state | §2.2, §3.2, §3.3 |
| B-5 / D-11 cancel hint item ambiguity | confirmed | resolved: modal items length fixed 5; `Esc` is footer field | §3.3 |
| B-6 per-tier horizontal scroll impossible with list reference | confirmed obsolete | resolved by removing horizontal tier scroll from canonical model | §2.3, §6.5 |
| B-7 reserve hint duplication | confirmed | resolved: strip reserve hint null when modal open; modal owns reserve row | §3.2 |
| B-8 rank text duplicated | confirmed as implementation drift risk | resolved by requiring a shared rank formatter for row rank text and `RANK_AND_COST` transition text | §2.2, §6.4 |
| B-9 block iconKey usage unclear | confirmed | resolved with per-block iconKey rule table | §6.4 |
| B-10 flavor source unclear | confirmed | resolved by making flavor non-canonical and optional only with non-empty locale value | §6.4 |
| C-1 manual label lint reused | confirmed | resolved with subRuleId suffixes | §0 |
| C-2 game/tools owner mixed | confirmed | resolved by splitting M00a and M00b | §0 |
| C-3 reference boundary was non-executable | confirmed | resolved: reference is canonical with `pr04ReferenceImageFidelity` | §0.0, §0 |
| C-4 golden/manual evidence label conflation | confirmed | resolved: evidence type is explicit; paths remain distinct | §8.2 |
| C-5 sidebar naming stale | confirmed | resolved: canonical labels renamed to `talent-assign-*`, legacy aliases retained in §9 | §8.2, §9 |
| C-6 mouse deferred vs required test | confirmed | resolved with `mousePointerMode` self-audit field and conditional mouse / keyboard-only fastCheck | §0, §6.5, §10 |
| C-7 stop condition not anchored | confirmed | resolved with M00 references in stop condition | §4 |
| C-8 new test naming mixed | confirmed | resolved with camelCase footnote | §0 |
| C-9 tools task path ambiguity | verified: `Phase2ContentCoverageTest` is `@Tag("contractLint")`; `Phase4V4WhiteboxScenarioCliTest` is `:tools:test` | resolved with explicit note | §8.1 |
| D findings | verified as cleanup-level issues | absorbed concrete leftovers: duration source wording, PR04 dark label boundaries, prerequisite deferral cite path, tier distribution breakdown, sha256 evidence type and no-grid self-audit; obsolete grid-specific items removed | §0, §2.6, §8.2, §10 |

### Claude Round 3 Review Disposition

| Finding | Verification result | Disposition | Contract anchor |
| --- | --- | --- | --- |
| A-1 `SECTION_HEADER` row ambiguity | confirmed in §2.2: rows and section meta both could own headers | resolved: rows contain only `TALENT_NODE`; section headers come only from `TalentAssignSectionModel.displayName` + `nodeCountText`; section headers cannot focus or hit-test | §2.2, §6.2, §10 |
| A-2 `SLOT_REPLACE_TARGET` focused vs all slots | confirmed in §3.2/§3.3 wording conflict | resolved: all four filled slots become replacement targets; `focused` is separate cyan keyboard focus, initially slot 1 | §3.2, §3.3, §10 |
| A-3 `stateMarkerText` vs `stateIconKey` | confirmed: both fields existed with no precedence | resolved: PR04 is marker-text-only; `stateIconKey=null`; mutual exclusion test required | §2.2, §6.3, §10 |
| A-4 scattered required tests missing from matrix | confirmed by comparing §2.3/§2.4/§6.6 to §0 | resolved by adding edge projection, selection identity, legend, detail field mapping and modal focus tests to Acceptance Matrix | §0 |
| A-5 detail block field mapping | confirmed: `primaryText` / `secondaryText` / `bodyLines` semantics were underdefined | resolved with per-block field mapping table and `DescriptionPresenter` fallback precedence | §6.4 |
| B-1 point text source | confirmed from `PlayerStatusSnapshot.talentPoints/raceTalentPoints` | resolved with explicit header source mapping; client must not recompute point banks | §2.2 |
| B-2 `nodeCountText` meaning | confirmed ambiguous; Claude's learned/total suggestion conflicts with canonical `6/6` example | resolved as visible nodes / total nodes, not rank or learned progress | §2.2, §6.2 |
| B-3 footer help vs actions | confirmed duplicate-risk | resolved: footer is panel navigation as structured hint rows; `ACTIONS` is selected-talent command list | §2.2, §6.4, §10 |
| B-4 modal trigger ownership | confirmed client-vs-gameplay ambiguity | resolved: PR04 consumes upstream slot-choice-required state; no second gameplay legality calculation in input | §3.1, §3.2, §10 |
| B-5 modal initial focus | confirmed unspecified | resolved: slot 1 focused on modal open | §3.3 |
| B-6 modal local vs overlay identity | confirmed as owner priority gap | resolved: modal local flushes one-way into overlay identity on close | §2.4 |
| B-7 current-rank detail source priority | confirmed first-match vs cumulative ambiguity | resolved: `DescriptionPresenter` complete path wins; raw fallback used only when presenter path empty/unavailable | §6.4 |
| B-8 connector deferred test | confirmed matrix mismatch | resolved with `prerequisiteConnectorMode` self-audit field and implemented/deferred fastCheck split | §0, §2.3, §9, §10 |
| B-9 section header render format | confirmed unspecified | resolved: `${displayName}  ${nodeCountText}` with fixed tones and two-space separator | §2.2 |
| B-10 lint subrule delimiter | confirmed mixed but intentional for whole-rule vs subrule | resolved with footnote: `#` only for subRuleId; no `#` means whole rule | §0 |
| C-1 PascalCase test references | confirmed in §5.1/§9 | resolved to camelCase test references | §9 |
| C-2 PR02 test rename in PR04 matrix | confirmed cross-PR boundary issue | moved to Cross-PR Dependency; PR04 matrix no longer owns the rename | §0, §7 |
| C-3 fallback evidence test name | confirmed misleading pass/fail semantics | split into renderer crash-safe test and manual-evidence lint | §0, §9 |
| C-4 mouse conditional parsing | confirmed inline prose was weak | resolved through `mousePointerMode` self-audit field | §0, §6.5, §10 |
| C-5 backtick test names | confirmed against current Kotlin tests | resolved in matrix and cross-PR dependency references | §0, §7 |
| C-6 legend pending conditional | confirmed naming mismatch | resolved with conditional pending test name | §0, §6.6 |
| D cleanup items | verified as useful where concrete | absorbed only executable pieces: race-tree label note, screenshot sha256 type, tier example, self-audit no-grid entry and prerequisite cite path | §2.6, §8.2, §10 |

## 11. 非目标

1. 不改职业树规则。
2. 不改 run summary / owner report。
3. 不改 `phase4-v4-pr01` whitebox scenario 的玩法状态；只允许更新 presentation/layout 相关截图或 PR04 manual evidence，不允许改 talent 状态、learnable、owner metric 或日志事件断言。
4. 不生成新音频。
5. 不批量生成 skill/tree/portrait icon。
6. 不重新设计职业树数据结构，不新增第二套 talent snapshot。
7. 不新增 per-tree collapse state；如未来需要，另起明确 owner 和状态机。
8. 不新增新的 dark UI whitebox Gradle task；PR04 使用 existing `preparePhase4V4Whitebox` 入口和 `dark-uiux-pr04-profession-tree-ui` scenario。若未来需要新的自动化入口，必须另行扩展 tools 合同。
9. 不新增 `EXCLUDED_BY_BRANCH` / mutual-exclusion 规则状态。
10. 不新增完整 reserve skill list modal；PR04 只负责 active slot choice modal 内的 `R` reserve action row。
11. 不照抄任何非 canonical reference image 的装备、背包、属性同屏复杂度；只验证 Talent Assign 全屏面板，以及退出后现有 shell/right companion 恢复。
12. 不在 PR04 引入 grid/tier/column node graph；这是已被当前唯一参考图取代的旧方向。

## 12. 回滚边界

本 PR 回滚只应影响职业树 UI chrome、layout、presenter/model、renderer、focused tests、golden 和 PR04 manual record。如果回滚需要改 `core` 或 `game` 职业树规则，说明 PR 范围已经越界。PR04 回滚不得删除 PR02 frame/resource keys，不得覆盖 upstream `phase4-v4-pr01` gameplay manual record，不得把 PR06/PR07 rebaseline 责任提前混入本 PR。

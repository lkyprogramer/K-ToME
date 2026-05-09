# Dark UI/UX PR-04 Profession Tree UI Review

目标文档：`UI/pr/dark-uiux-pr04-profession-tree-ui.md`

审查依据：
- `docs/review/rule/pr-level-review-standard.md`
- `UI/PLAN.md`
- `UI/pr/README.md`
- `UI/pr/development-governance.md`
- `UI/pr/screen-coverage-matrix.md`
- `UI/ART_STYLE_BIBLE.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md`
- 当前代码：`TalentSidebarPresenter`、`TileRenderModel`、`TileRenderer`、`InputHandler`、`ValidationScenarioRegistry`

审查范围：
- 当前工作区在 `main...origin/main`，存在 PR-02 相关未提交改动，本轮只把它们当作当前状态线索，不把无关 dirty diff 作为 PR04 合同。
- 本轮重点是 PR04 文档是否能作为“无需猜测”的开发执行文档，而不是审查 PR04 实现是否已经完成。

结论：当前 PR04 文档不能判定为可直接进入实现。没有发现 P0 级硬边界破坏，但存在多个 P1/P2 缺口：状态/icon/tone 合同不唯一、active slot modal 状态机不完整、whitebox artifact 不可机械追溯、race tree 与“三棵职业树”口径冲突、跨 PR 依赖和删除/rebaseline 责任没有结构化落表。

## Findings

### P0

无。

### P1

#### P1-1 状态 icon / tone / rank 文本没有唯一 presentation contract，会让 renderer 重造职业树状态规则

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:47-53` 要求 `TalentSidebarPresenter` 负责 tree header、node row、preview、footer，且 renderer 不重拼节点文案。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:81-86` 又要求 Node row 同时展示 skill icon、`ui.state.locked/learnable/active/reserve.icon`、rank chip、四态 tone。
- 当前 `TalentSidebarLine` 只有 `text / role / iconKey / selected`，见 `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:25-30`；`iconKey` 已被 talent/tree icon 使用，不能同时承载 state icon。
- 当前 renderer model 只把 `line.iconKey` 解析为单个 `TileTextRow.icon`，见 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:1185-1193`；当前 tone 映射在 `TileRenderModel.talentSidebarTone`，见 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:1706-1724`。
- `UI/ART_STYLE_BIBLE.md:73-78` 冻结了职业树四态 tone；当前 `UiDesignTokens` 已有 `UiTalentToneColors`，见 `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt:257-262`。

问题：
文档没有冻结 state icon 从哪里来、`TalentSidebarLine` 是否要新增 `stateIconKey` / `stateToneToken` / `rankText`、renderer 是否允许根据 `TalentSidebarLineRole` 再映射状态。开发者有两条看似合理但互相冲突的实现路径：在 presenter 输出完整状态表现，或在 renderer 根据 role/state 重建 icon/tone。后一种会违反 presentation authority，并且后续 PR06 icon rebaseline 时无法确定谁拥有 fallback。

影响：
- `TalentSidebarPresenter`、`TileRenderModel`、`TileRenderer`、`TileRendererCanvasTest`、`TalentSidebarPresenterTest` 都会受影响。
- 四态 tone 可能继续使用当前 `TileTextTone.GREEN/WHITE/GOLD`，而不是 style bible 的 `talent-learnable/reserve/active`。
- `ui.state.*` 资源可能根本没有被实际 drawAsset 消费，只在文档里出现。

修复方向：
在 PR04 文档新增“TalentSidebar presentation contract”表，至少冻结：

| Field | Owner | Rule |
| --- | --- | --- |
| `TalentSidebarLine.text` | `TalentSidebarPresenter` | 已本地化展示文本；renderer 不拼接 rank / lock reason |
| `TalentSidebarLine.primaryIconKey` | `TalentSidebarPresenter` | tree / talent icon，可空；renderer 只解析 |
| `TalentSidebarLine.stateIconKey` | `TalentSidebarPresenter` | 仅 node row 使用，四态固定映射到 `ui.state.locked.icon` / `ui.state.learnable.icon` / `ui.state.reserve.icon` / `ui.state.active.icon` |
| `TalentSidebarLine.toneToken` | `TalentSidebarPresenter` 或 typed enum | 四态固定映射到 `UiDesignTokens.color.talent.*`，选中态只叠加 focus ring，不改变 state identity |
| `TalentSidebarLine.rankText` | `TalentSidebarPresenter` | `rank/maxRank` 或 `committed->rank/maxRank`，空值规则固定 |

同时明确 renderer 只消费这些 presentation fields，不再根据 `TalentNodeStateSnapshot` 或 localized text 重建状态。

推荐测试：
- `TalentSidebarPresenterTest.pr04MapsNodeStatesToStateIconAndTalentTone`
- `TileRendererCanvasTest.darkUiuxPr04DrawsTalentNodePrimaryAndStateIcons`
- `TileRendererCanvasTest.darkUiuxPr04UsesTalentToneTokensForFourStates`

#### P1-2 `ACTIVE_TALENT_SLOT_CHOICE` modal 的 enter/update/exit 状态机没有冻结，当前测试也没有覆盖 `1-4 / R / Esc`

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:39-40` 只写 `1-4` 替换、`R` 入 reserve、`Esc` 取消，以及数字键只在 modal 消费。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:86` 只说“保留输入语义”，`UI/pr/dark-uiux-pr04-profession-tree-ui.md:114-117` 只给人工白盒口径。
- 当前 `InputHandler` 的真实命令语义是：`Esc -> RollbackTalentDraft`，`R -> ConfirmTalentDraftToReserve`，`1-4 -> ConfirmTalentDraftReplacingSlot(slot)`，见 `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1339-1351`。
- 当前测试只覆盖 pending active/sustained talent 会打开该 modal，见 `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1340-1352`；未覆盖 replace/reserve/cancel 命令。
- review 标准要求 UI/client PR 覆盖 modal/overlay/focus、enter/update/exit 和 failure semantics，见 `docs/review/rule/pr-level-review-standard.md:98-116`。

问题：
“Esc 取消”可以被实现成只关闭 frame、回滚 draft、或返回 talent assign 根 frame；“数字键只在 modal 消费”也没有说明按下 `1-4` 后是否 close all frames、保留 `TALENT_ASSIGN`、还是回到 map。当前代码已有具体行为，但 PR04 文档没有把它冻结为开发合同。

影响：
- 实现者可能只更新视觉 chrome，不补输入测试，导致 modal 看起来正确但命令语义漂移。
- PR04 close 后 `InputHandlerTest` 仍可能没有证明 `1-4/R/Esc` 的关键路径。
- 人工白盒只能看到文案，不能证明命令实际发出正确 `PlayerCommand`。

修复方向：
在 PR04 文档新增 modal 状态机表：

| State | Input | Command | Modal stack effect | Overlay mode after command | Test |
| --- | --- | --- | --- | --- | --- |
| `ACTIVE_TALENT_SLOT_CHOICE` | `1..4` | `ConfirmTalentDraftReplacingSlot(slot)` | close all modal frames | command owner decides next snapshot | `InputHandlerTest.activeSlotChoiceNumbersReplaceSlotsAndCloseModal` |
| `ACTIVE_TALENT_SLOT_CHOICE` | `R` | `ConfirmTalentDraftToReserve` | close all modal frames | command owner decides next snapshot | `InputHandlerTest.activeSlotChoiceReserveConfirmsToReserve` |
| `ACTIVE_TALENT_SLOT_CHOICE` | `Esc` | `RollbackTalentDraft` | close all modal frames | command owner decides next snapshot | `InputHandlerTest.activeSlotChoiceEscRollsBackDraft` |
| `TALENT_ASSIGN` without active slot modal | `1..4` | `null` | no selection change | stays `TALENT_ASSIGN` | `InputHandlerTest.talentTreeSidebarIgnoresNumberHotkeysOutsideActiveSlotChoice` |

同时写明 `Backspace`、`T`、`Tab` 在该 modal 内是否被忽略、关闭一层、或沿用 root talent assign 行为。

推荐测试：
- `InputHandlerTest.activeSlotChoiceNumbersReplaceSlotsAndCloseModal`
- `InputHandlerTest.activeSlotChoiceReserveConfirmsToReserve`
- `InputHandlerTest.activeSlotChoiceEscRollsBackDraft`
- `InputHandlerTest.talentTreeSidebarIgnoresNumberHotkeysOutsideActiveSlotChoice`

#### P1-3 白盒证据只有目录和 label，没有 exact artifact、manual record 字段、skip rule，无法机械验收

证据：
- Acceptance Matrix 的 `UI04-M03` 写 `validation scenario bootstrap tests`、`preparePhase4V4Whitebox when whitebox is required`、artifact `UI/manual-records/`，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:19`。
- `whitebox=required`，但 owner gate 又写成 “when whitebox is required”，条件重复且不确定。
- 白盒命令只写 `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01`，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:104-108`。
- 人工白盒只列了场景说明和 label，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:112-119`。
- 当前 v4 scenario 的真实 evidence files 和 manual record path 在 `ValidationScenarioRegistry` 中固定，见 `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt:81-119`；历史 manual record 的字段包含 app path、runtime home、scenario id、seed、CUA steps、screenshot path、log path、sha256 和 conclusion，见 `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md:3-41`。
- review 标准要求 manual white-box 有 scenario、steps、expected evidence 和 skip rule，见 `docs/review/rule/pr-level-review-standard.md:410-421`。

问题：
开发者不知道 PR04 是复用 v4 manual record，还是新增 `UI/manual-records/dark-uiux-pr04-*.md`；不知道 `expected-evidence.json` 是生成产物路径还是需要提交；也不知道截图 label 如何映射到 `phase4-v4-pr01-*` 文件名。`UI/manual-records/` 只是目录，不是可验收 artifact。

影响：
- PR04 的白盒 evidence 可能散落到 v4 manual record、UI manual record、build output 或 PR 描述中，PR07 final audit 无法机械追溯。
- `phase4-v4-pr01` gameplay evidence 与 PR04 dark UI evidence 会混成一份记录，后续无法判断截图变化是 UI layout 变更还是玩法场景变更。

修复方向：
在 PR04 文档冻结以下 artifact：

| Artifact | Path | Owner | Rule |
| --- | --- | --- | --- |
| PR04 manual record | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | PR04 | 必须引用 generated `build/whitebox/phase4-v4-pr01/cua-runbook.md`、`expected-evidence.json`、app sha256、runtime home、截图 sha256 |
| v4 scenario source record | `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md` | upstream v4 PR01 | 只作为 gameplay baseline，不被 PR04 覆写 |
| PR04 screenshot labels | `dark-uiux-pr04-talent-sidebar-start`、`dark-uiux-pr04-active-slot-choice` | PR04 | 在 manual record 中映射到实际 screenshot path 和 sha256 |
| generated evidence plan | `build/whitebox/phase4-v4-pr01/expected-evidence.json` | generated | 不提交，但 manual record 必须记录实际读取路径和匹配结果 |

同时把 `UI04-M03.fastCheck` 改成具体测试名，例如 `ValidationScenarioBootstrapTest.phase4V4Pr01StartsAtFastSection`、`Phase4V4WhiteboxScenarioCliTest.materializesPhase4V4Pr01EvidencePlan`。

推荐测试：
- `Phase4V4WhiteboxScenarioCliTest.materializesPhase4V4Pr01EvidencePlan`
- `ValidationScenarioBootstrapTest.phase4V4Pr01StartsAtFastSection`
- 新增或扩展 `acceptanceContractLint`：`whitebox=required` 的行 artifact 不能只是目录。

#### P1-4 “三棵职业树”口径遗漏 race tree，和当前 snapshot authority 不一致

证据：
- PR04 文档解释“三棵职业树”仅指当前已选职业的三棵树，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:43`。
- 同一文档又要求 header 显示职业天赋点和种族天赋点，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:81`。
- 上游 v4 PR01 要求 `TalentProgressionRequest` 解析 profession tree 与 race tree，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:131-135`。
- 当前 `TalentTreeOwnerType` 包含 `PROFESSION / RACE`，见 `core/src/main/kotlin/com/ktome/core/talent/TalentAllocationDraft.kt:3-6`。
- 当前 `FoundationGameSession.buildTalentTreeSnapshots` 输出 `profession.talentTrees + race?.talentTrees.orEmpty()`，见 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:5182-5215`。

问题：
如果开发者按“只显示当前职业三棵树”实现，race tree snapshot 会被 UI 层过滤掉，`raceTalentPoints` 只显示但无可操作 surface。反过来，如果开发者按当前 code 输出所有 `uiState.talentTrees`，又会和文档中的“三棵职业树”表述不一致。

影响：
- client 层可能引入过滤规则，变成第二套 tree ownership authority。
- race talent path 会出现“有点数、无入口”的 player-facing 断裂。
- `InputHandler` 已支持 focused tree owner 的 respec，见 `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1260-1301`；文档不承认 race tree 会让这条现有行为变成无主行为。

修复方向：
把 §1 的解释改成：

> PR04 manual/golden 的最小必验面是当前职业三棵 profession tree；runtime 渲染不得过滤 `RenderUiStateSnapshot.talentTrees`。如果 snapshot 包含 `ownerType=RACE` 的 tree，PR04 必须按同一 node row/presentation contract 渲染，并使用 `raceTalentPoints` 作为点数来源。PR04 不新增 race tree 规则，不新增 client-side owner 过滤。

同时在 Acceptance Matrix 增加 race tree owner case，或者明确 `UI04-M01` 覆盖 `TalentTreeOwnerType.RACE` 的 existing focused test。

推荐测试：
- `TalentSidebarPresenterTest.rendersRaceTreeRowsWithoutFilteringSnapshotTrees`
- `InputHandlerTest.talentAssignRespecFollowsFocusedTreeOwner` 保留为 PR04 fastCheck。

#### P1-5 当前 PR 依赖没有冻结到执行顺序和 cross-PR evidence，开发者无法确定基准分支

证据：
- PR04 文档前置条件只写 PR-01、PR-02 完成，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:6`。
- 系列 README 固定串行顺序为 `PR-00 -> PR-01 -> PR-01-1 -> PR-02 -> PR-03 -> PR-04 -> PR-05 -> PR-06 -> PR-07`，见 `UI/pr/README.md:19-35`。
- PR04 依赖 PR02 的 `ui.frame.*`、`ui.state.*` key，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:51-52`；PR02 文档把这些 key 冻结在 §4.2/§4.3，见 `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:139-178`。
- PR04 又把 skill/tree icon final rebaseline 交给 PR06，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:110`。
- review 标准要求跨 PR 依赖和删除责任能追到 `crossPrDependency` 或 `removalOwner`，见 `docs/review/rule/pr-level-review-standard.md:164-178`、`docs/review/rule/pr-level-review-standard.md:203-228`。

问题：
PR04 文档没有说明 PR03 是否必须已经合入才能开工，也没有把 PR02 key dependency、PR06 icon rebaseline、PR07 final audit 用 requirementId 连接起来。开发者可能基于 PR02 后直接开 PR04，和系列串行规则冲突；也可能把 PR06 的 rebaseline 责任只写在正文，后续无法机械追踪。

影响：
- 基准分支选择不稳定，尤其当前工作区已有 PR02 dirty diff 时，reviewer 无法判断 PR04 应基于 main、PR02 branch、还是 PR03 close 后分支。
- PR06 rebaseline 可能漏掉 PR04 golden，PR07 final evidence index 也无法知道 PR04 旧 painterly icon 是被允许的临时状态。

修复方向：
新增 Cross-PR Dependency 表，至少包含：

| PR04 requirement | Dependency | Type | Verification |
| --- | --- | --- | --- |
| `UI04-M01` | `phase4-v4-pr01` | hard upstream | `phase4-v4-pr01` scenario registered; `TalentSidebarPresenter` and `ACTIVE_TALENT_SLOT_CHOICE` exist |
| `UI04-M02` | `PR-02` `ui.frame.*` / `ui.state.*` | hard upstream | PR02 resolver test and manifest lint pass |
| `UI04-M02` | `PR-03` close | series order | PR04 branch base is after PR03 or document explains no branch conflict exception |
| `UI04-M02` | `PR-06` `dark-uiux-pr06-talent-icon-rebaseline` | downstream rebaseline | removalOwner/rebaselineOwner = PR06 |
| `UI04-M03` | `PR-07` final evidence index | downstream audit | `dark-uiux-pr07-final-all-screens` must link PR04 labels |

推荐测试：
- `acceptanceContractLint` 增加 PR04 cross dependency presence check，或者在 PR 文档 self-audit 中列为人工 blocking check。

### P2

#### P2-1 影响范围不是 Added / Modified / Deleted 清单，并漏掉实际 owner 文件

证据：
- PR04 当前只有“区域 / 预期改动”两列表，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:68-77`。
- review 标准要求 PR 文档列出 Added / Modified / Deleted 三栏文件清单，见 `docs/review/rule/pr-level-review-standard.md:139-152`。
- 实际 talent sidebar tone/model 映射在 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:1185-1193` 和 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:1706-1724`，但文档没有列。
- 当前 sidebar row draw path 在 `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:690-713`；如果要画 primary icon + state icon，`TileTextRow` 或 draw row layout 也要改。

问题：
只列 `TileRenderer.kt` 会让开发者漏掉 `TileRenderModel.kt`、`TileTextRow`、可能新增的 presentation model/test helper，以及 PR04 manual record 文件。Deleted 为空也必须显式写，避免旧 glyph/golden/fallback 保留责任不清。

修复方向：
将 §4 改为：

| Kind | Path | Owner | Input | Output | Failure semantics | Test |
| --- | --- | --- | --- | --- | --- | --- |
| Modified | `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt` | client | `RenderUiStateSnapshot`, `OverlayState` | `TalentSidebarLine` with state icon/tone | no rule recomputation | `TalentSidebarPresenterTest...` |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | client | `TalentSidebarLine` | `TileTextRow` icon/tone/selection | missing visual fallback via resolver | `TileRendererCanvasTest...` |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | client | `TileTextRow` | draw primary/state icons without overlap | truncate, stable row height | `TileRendererCanvasTest...` |
| Modified | `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` | client | modal stack + key input | typed `PlayerCommand` | no number hotkey leak | `InputHandlerTest...` |
| Added | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | docs | generated evidence | manual evidence | required; no skip without reason | manual self-audit |
| Deleted | `N/A` | docs | N/A | N/A | no deletion in PR04; PR06 owns icon rebaseline | crossPrDependency PR06 |

#### P2-2 Tree section 的折叠/展开、选择、ordering 和 identity 规则没有定义

证据：
- PR04 文档要求 Tree section 有 tree icon、tree name、当前选中态、折叠/展开状态，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:82`。
- 当前 modal local state 只有 `talentTreeSelection` 和全局 `talentTreePreviewExpanded`，见 `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt:20-33`。
- 当前输入只支持上下移动、左右跨 tree、`P` 切 preview，不支持 per-tree collapse，见 `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1306-1335`。
- 当前 presenter flatMap 所有 tree nodes 后按 flat index 选中，见 `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:58-76`。

问题：
“折叠/展开状态”没有说明是每棵树独立状态、全局 preview state、还是只做视觉 disclosure；也没有说明 tree ordering、折叠后 selection 是否保持 node identity、左右切树按 row index 还是首个 learnable 节点。

影响：
- 实现者可能新增 client-only tree collapse state，但没有 owner、identity、exit/reset 规则。
- golden 在 tree node 数量变化、race tree 出现、locale 文本变长时会漂移。

修复方向：
在 PR04 文档冻结：
- runtime tree order = `RenderUiStateSnapshot.talentTrees` order；client 不排序。
- node order = `TalentTreeSnapshot.nodes` order；client 不排序。
- PR04 如要 per-tree collapse，必须新增 `TalentSidebarCollapseState(treeId -> expanded)` 或明确延后；默认选择建议：本 PR 不新增 per-tree collapse，只保留 existing `talentTreePreviewExpanded`。
- selection identity = `talentId + treeId + ownerType + treeOwnerId`；flat index 仅为 local navigation cache，snapshot 更新后按 identity 恢复，否则 clamp 到 first visible node。

推荐测试：
- `InputHandlerTest.talentTreeColumnNavigationPreservesRowWhenTreesHaveDifferentNodeCounts`
- `TalentSidebarPresenterTest.renderOrderFollowsSnapshotTreeAndNodeOrder`

#### P2-3 Active slot strip 缺 source、ordering、empty slot 和 pending allocation 规则

证据：
- PR04 文档要求 Active slot strip 展示 4 个主动槽、空槽高亮、reserve 提示，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:85`。
- 当前 modal 进入条件依赖 `snapshot.uiState.talents.size >= PLAYER_ACTIVE_TALENT_SLOT_COUNT` 和 pending active/sustained tree node，见 `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1910-1918`。
- 当前 tests helper 默认 active slots 1-4，见 `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt:1594-1610`。

问题：
文档没有说明 slot strip 的数据源是 `snapshot.uiState.talents`、pending draft、还是 `TalentLoadout` 投影；也没有说明空槽条件、slot order、pending new active talent 与 existing slot 的显示关系。四槽未满时是否显示空槽高亮并直接 confirm，四槽满时是否显示 replace modal，目前只能从代码推断。

修复方向：
新增 Slot Strip Contract：
- slot count 固定 `PLAYER_ACTIVE_TALENT_SLOT_COUNT = 4`，PR04 不改该常量。
- slot order 固定 `1..4`。
- filled slot 来源为 `RenderUiStateSnapshot.talents.slot`。
- empty slot 是 `1..4 - talents.slot`，四槽未满时 pending active talent confirm 不打开 `ACTIVE_TALENT_SLOT_CHOICE`。
- four-slot-full 且 pending node `category in ACTIVE/SUSTAINED` 时打开 active slot choice；passive 不打开。
- reserve prompt 来源为 `snapshot.uiState.reserveTalents` 和 pending node command path，不用 client 自算 learnability。

推荐测试：
- `TalentSidebarPresenterTest.activeSlotStripShowsFilledEmptyAndReserveHint`
- `InputHandlerTest.pendingActiveTreeTalentConfirmsDirectlyWhenEmptySlotExists`

#### P2-4 Gate Budget 不符合 series governance，缺 freshness、resource/manifest/golden 口径和耗时来源

证据：
- PR04 Gate Budget 只有 heavy task 列表和触发原因，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:22-24`。
- series governance 要求 Gate Budget 声明 heavy tasks、触发原因、resource/manifest/golden freshness、最近耗时来源或 `full-task-duration-summary` 读取方式，见 `UI/pr/development-governance.md:58-71`。

问题：
PR04 是 golden/whitebox PR，但没有说明 golden freshness 怎么判定、PR02 key 是否需要 resolver freshness、`full-task-duration-summary` 是读取旧文件还是由本轮 `verifyChanged` 产出。

修复方向：
把 Gate Budget 改为四段：
1. Heavy tasks: `:client:clientSmoke`, `:client:goldenScreenshot`, `maintainabilityLint`, `verifyChanged`, optional packaged whitebox.
2. Freshness: PR04 修改 `TalentSidebarPresenter` / `TileRenderModel` / `TileRenderer` 后必须刷新 `dark-uiux-pr04-*` golden/manual evidence；PR06 rebaseline 前旧 icon 混用只允许记录为 PR06 dependency。
3. Resource/manifest: PR04 不新增 formal key；但必须证明 PR02 `ui.frame.*` / `ui.state.*` keys resolve through canonical/runtime manifest.
4. Duration: 本轮最终以 `build/verification/verify-changed/full-task-duration-summary.{json,md}` 为耗时来源；若不存在，记录 `N/A before first verifyChanged`。

#### P2-5 验证命令顺序和覆盖面不足，不能证明 PR04 的 blocking 行为

证据：
- PR04 §7 命令没有把 `acceptanceContractLint` 放在开头，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:96-108`。
- `UI/pr/README.md:32-33` 固定 gate ladder 为 `acceptanceContractLint -> fast lane -> resource gate -> client evidence -> maintainabilityLint -> verifyChanged`。
- `docs/verification/README.md:22-37` 也说明 `acceptanceContractLint` 不替代 fast lane/owner gate，最终 closure 是 `verifyChanged`。
- PR04 文档依赖 PR02 keys，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:52`，但 §7 只跑 `assetLint`，没有列 resolver/manifest/dark key 检查。

问题：
当前命令可能在 focused tests 失败后才发现文档合同不合格；也可能在 `assetLint` 通过但 `ui.state.*` formal key 未进入 runtime manifest 时漏检。

修复方向：
把 §7 拆成 gate ladder：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest
./gradlew manifestLint contractLint localeLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint
./gradlew verifyChanged
```

如果 PR04 不改 manifest，仍要声明 PR02 key resolver 的 exact test 或 owner gate，例如 `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` 是 upstream dependency check，而不是 PR04 owner gate。

#### P2-6 Acceptance Matrix 粒度过粗，多个 MUST 无法追到 exact test、artifact 或 owner gate

证据：
- `UI04-M01` 用一行覆盖所有 §2 硬依赖，fastCheck 只有类名，artifact 是 `build/reports/tests/`，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:17`。
- `UI04-M02` 覆盖整个 §5 UI 改造范围，artifact 是 `client/build/reports/golden/`，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:18`。
- review 标准要求 blocking requirement 至少追到 focused unit/lint/owner harness/golden/manual/final verifyChanged，且 artifact repo-relative，见 `docs/review/rule/pr-level-review-standard.md:154-186`。

问题：
`TalentTreeNodeSnapshot.category` typed enum、`DescriptionPresenter.presentTalentTreeNodeLines`、数字键隔离、PR02 key fallback、rank preview、breakpoint preview、窄窗口不遮挡日志都被塞进 4 行矩阵，后续 reviewer 无法判断哪个 MUST 缺 test。

修复方向：
把矩阵拆到至少 8 行：
- `UI04-M01-category-typed-contract`
- `UI04-M02-presenter-authority`
- `UI04-M03-state-icon-tone-contract`
- `UI04-M04-input-number-isolation`
- `UI04-M05-active-slot-choice-modal`
- `UI04-M06-preview-lock-breakpoint-lines`
- `UI04-M07-golden-talent-sidebar`
- `UI04-M08-whitebox-phase4-v4-pr01`
- `UI04-M09-pr06-rebaseline-dependency`

每行写 exact fastCheck method、ownerGate、artifact path、whitebox value、crossPrDependency/removalOwner。

#### P2-7 窄窗口验收没有固定 viewport、断言和证据 label

证据：
- PR04 人工白盒要求“切到窄窗口时，职业树 panel 不遮挡底部日志的最新一条关键反馈”，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:118`。
- `screen-coverage-matrix` 对职业树侧栏要求长描述截断，见 `UI/pr/screen-coverage-matrix.md:42`；全局 label inventory 只有 `dark-uiux-pr04-talent-sidebar-start` 和 `dark-uiux-pr04-active-slot-choice`，见 `UI/pr/screen-coverage-matrix.md:71-72`。

问题：
没有固定窄窗口尺寸、日志样本、判定文本、截图 label 或自动断言。开发者不知道是用 `1280x800`、最小 shell 窗口、还是 packaged app 手动 resize；也不知道“最新一条关键反馈”的来源是 `log.talent.learned`、`rank_up` 还是 breakpoint。

修复方向：
新增验收条目：
- viewport: `1024x720` 或当前项目最小支持窗口尺寸，必须和 PR01/PR01-1 shell min-window 口径一致。
- evidence label: `dark-uiux-pr04-talent-sidebar-min-window-log-visible`。
- expected: bottom log latest line contains one of `log.talent.learned / log.talent.rank_up / log.talent.breakpoint_chosen` and is not overlapped by talent panel bounds.
- automated check: `TileRendererCanvasTest.darkUiuxPr04TalentSidebarDoesNotOverlapBottomLogAtMinWindow`，如果当前 canvas model不能直接测 overlap，manual record 必须列 residual risk。

#### P2-8 PR06 icon rebaseline 是真实 removal/iteration 责任，但没有 `removalOwner` 或禁止混入规则

证据：
- PR04 文档说 PR04 close 时职业树 icon、tree portrait、skill icon 仍可指向现有 manifest entry，PR06 才切 dark-v1 skill/tree 资源，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:41`。
- §7 又说 PR06 切换 skill/tree icon 后必须 rebaseline 相关 golden，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:110`。
- review 标准要求 old path/fallback/compat 都有删除或禁止复活规则，见 `docs/review/rule/pr-level-review-standard.md:217-228`。
- `screen-coverage-matrix` 已有 PR06 label `dark-uiux-pr06-talent-icon-rebaseline`，见 `UI/pr/screen-coverage-matrix.md:71-76`。

问题：
这是合法的分阶段临时状态，但当前文档没有把“旧 painterly icon 允许到 PR06”为一个可追踪的 iteration/removal plan。PR06 可能只换资源，不知道必须刷新 PR04 golden；PR07 final audit 也无法知道旧 icon 是否仍允许存在。

修复方向：
在 PR04 增加 Removal/Iteration Plan：

| Temporary surface | Allowed in PR04 | Removal/Rebaseline owner | Regression scan | Exit rule |
| --- | --- | --- | --- | --- |
| existing `icon.skill.*` / `icon.tree.*` / `tree.*` / `portrait.*` painterly assets in talent UI | yes | PR06 `UI06-M??` / `dark-uiux-pr06-talent-icon-rebaseline` | `goldenScreenshot`, manifest coverage, `screen-coverage-matrix` final audit | PR06 rebaseline refreshes `dark-uiux-pr04-talent-sidebar-start` and `dark-uiux-pr04-active-slot-choice`; PR07 final index shows no old-style residue |

#### P2-9 `TalentTreeNodeSnapshot.category` wording容易让开发者把状态和类别混淆

证据：
- PR04 §1 的四态是 `LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE`，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:38`。
- PR04 §2 又写 `TalentTreeNodeSnapshot.category` 是 typed enum，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:47`。
- 当前代码中四态字段是 `state: TalentNodeStateSnapshot`，类别字段是 `category: TalentCategory`，见 `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:408-420`。

问题：
`category` 确实是 typed enum，但 PR04 主要 UI 四态应读 `state`，不是 `category`。当前 wording 会诱导 renderer 用 `category` 判断 locked/active/reserve，或把 `TalentCategory.ACTIVE/PASSIVE/SUSTAINED` 当作四态。

修复方向：
把 §2 第 1 条改成：

> `TalentTreeNodeSnapshot.category: TalentCategory` 与 `TalentTreeNodeSnapshot.state: TalentNodeStateSnapshot` 都是 typed enum；PR04 四态 UI 只消费 `state`，主动槽选择是否需要 slot 只消费 `category in ACTIVE/SUSTAINED`。client 不做字符串 `valueOf` 解析，也不从 localized text 推断状态。

推荐测试：
- `TalentSidebarPresenterTest.stateAndCategoryAreRenderedBySeparateContracts`

### P3

#### P3-1 文档没有在开头记录当前工作树 review 范围，复审时容易误读 PR02 dirty diff

证据：
- review 标准要求 review 前确认基准 ref、HEAD 和工作树状态，见 `docs/review/rule/pr-level-review-standard.md:41-55`。
- 本轮实际工作区存在 PR02 相关 dirty files；目标 PR04 文档没有 review-time scope template。

修复方向：
PR 文档本体不一定要记录当前 dirty state，但建议在 PR04 的 doc-vs-implementation self-audit 模板中增加：
- base ref / target branch
- upstream PR close sha 或 branch
- unrelated dirty diff 是否排除

#### P3-2 Canonical Artifact 描述仍是泛称，后续检索体验较差

证据：
- PR04 写 “talent focused test report、client golden、whitebox manual record”，见 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:26-28`。

修复方向：
改成 exact artifact：
- `build/reports/tests/client/test/index.html` 或 Gradle实际 test report path。
- `client/build/reports/golden/` 下对应 `dark-uiux-pr04-*` label 的 hash/report。
- `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md`。
- `build/verification/verify-changed/full-task-duration-summary.{json,md}`。

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR sequence and governance | `UI/pr/README.md:21-35`, `UI/pr/development-governance.md:39-56` | 部分一致；PR04继承 governance，但缺 PR03 serial dependency 与结构化 crossPrDependency |
| Presentation authority | `UI/pr/dark-uiux-pr04-profession-tree-ui.md:47-53`, `TalentSidebarPresenter.kt:25-30` | 部分一致；presenter authority 写对，但 state icon/tone output field 未冻结 |
| Four node states | `RenderSnapshot.kt:362-368`, `RenderSnapshot.kt:408-420` | 部分一致；代码有 typed state，但 PR04 文档把 category/state 表述混在一起 |
| Active slot modal input | `InputHandler.kt:1339-1351` | 部分一致；真实行为存在，但 PR04 文档和 tests 未冻结完整状态机 |
| PR02 UI chrome/state keys | `UI/pr/dark-uiux-pr04-profession-tree-ui.md:52`, `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:139-178` | 部分一致；依赖写到正文，但未进入 PR04 matrix / verifier |
| Race tree support | `FoundationGameSession.kt:5207`, `TalentAllocationDraft.kt:3-6` | 不一致；PR04 最小必验可只看三棵职业树，但 runtime 文档不能暗示过滤 race tree |
| Whitebox evidence | `ValidationScenarioRegistry.kt:81-119`, PR04 `§8` | 部分一致；scenario存在，但 PR04 artifact path、record字段、skip rule不完整 |
| Golden/rebaseline | `screen-coverage-matrix.md:71-76`, PR04 `§7` | 部分一致；label存在，但 PR06 rebaseline责任未结构化 |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前代码/文档状态 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |
| `client/ui/talent` | Presenter 是职业树 presentation authority | `TalentSidebarLine` 不足以表达 primary icon + state icon + tone token | 字段合同缺失 | P1 |
| `client/render` | Renderer 绘制职业树 panel/node/modal | 实际转换在 `TileRenderModel`，draw row 在 `TileRenderer` | 影响范围漏文件 | P2 |
| `client/input` | 数字键只在 active slot modal 消费 | 代码已有 `ConfirmTalentDraft*` 命令 | 文档未冻结 exit/fallback；测试缺命令断言 | P1 |
| `core/game snapshot` | PR04 不改规则，只消费 snapshot | snapshot 包含 profession + race tree | 文档“三棵职业树”可能误导过滤 race tree | P1 |
| `assets/manifest` | PR04复用现有资源，依赖 PR02 keys | PR02 keys是 upstream contract | PR04未声明 resolver/manifest dependency check | P2 |
| `whitebox` | phase4-v4-pr01 scenario evidence | registry 和 manual record exist | PR04自己的 manual record和label映射不清 | P1 |
| `docs/governance` | Acceptance Matrix + Gate Budget | 结构存在 | 粒度、artifact、crossPrDependency、duration/freshness不足 | P2 |

## 玩法与体验审查

PR04 是 player-facing surface。当前文档的设计目标正确：职业树需要展示成长路径、learnable/locked/active/reserve、学习预览和主动槽选择。但文档还不能保证实际玩家体验：

1. 状态颜色、状态 icon、技能 icon 如果没有双 icon / tone contract，玩家可能无法区分“可学”和“已学 reserve”。
2. `ACTIVE_TALENT_SLOT_CHOICE` 如果只更新 chrome、不锁命令测试，玩家可能看到 `1-4/R/Esc` 但命令发错或取消语义漂移。
3. race tree 若被 UI 层过滤，玩家会看到 race talent points 却找不到入口。
4. 窄窗口不遮挡日志是关键反馈体验，但当前没有尺寸、label、断言，无法验收。

## 当前阶段必须解决的问题

合并或开工前必须修：
- P1-1：冻结 `TalentSidebarLine` / `TileTextRow` 的 state icon、primary icon、tone token、rank text 合同。
- P1-2：冻结 active slot choice modal 状态机，并补 exact `InputHandlerTest`。
- P1-3：冻结 PR04 manual record path、expected evidence 映射、whitebox skip rule。
- P1-4：明确 runtime 不过滤 race tree，三棵职业树只是 PR04 minimum evidence。
- P1-5：补 Cross-PR Dependency 表，说明 PR03串行基准、PR02 key dependency、PR06 rebaseline owner、PR07 final audit。

可以作为 PR04 文档同轮 P2 修正：
- Added / Modified / Deleted 文件清单。
- Gate Budget freshness / duration source。
- Verification ladder 顺序和 exact tests。
- 窄窗口 golden/manual evidence。
- Acceptance Matrix 拆细。

## Removal/Iteration Plan

当前文档没有 Deleted 清单，也没有 formal removal plan。建议明确：

| Surface | Current PR04 decision | Owner | Regression |
| --- | --- | --- | --- |
| Old painterly `icon.skill.*` / `icon.tree.*` / `tree.*` / `portrait.*` in talent UI | PR04允许临时复用 | PR06 | `dark-uiux-pr06-talent-icon-rebaseline` + `goldenScreenshot` |
| ASCII glyph `[x] [+] [r] [*]` in talent text | PR04应删除或降级为 non-authority debug text；正式状态由 `ui.state.*` icon + tone 表达 | PR04 | `TalentSidebarPresenterTest.pr04MapsNodeStatesToStateIconAndTalentTone` |
| PR04 generated whitebox build outputs | 不提交，只由 manual record引用 | PR04 | manual record path + sha256 |
| PR04 golden labels | PR04 owns initial baseline；PR06 owns icon rebaseline | PR04/PR06 | PR07 final evidence index |

## Additional Suggestions

1. 在 PR04 文档加 “Implementation Order”：
   - Step 1: preflight upstream contracts and PR02 key resolver.
   - Step 2: extend presenter/model contract with state icon/tone.
   - Step 3: update renderer row layout and canvas tests.
   - Step 4: update input modal tests.
   - Step 5: run client evidence and whitebox.
2. 把 `acceptanceContractLint` 扩展为能检查 `whitebox=required` 行的 artifact 不能只是目录。
3. 把 PR04 的 `dark-uiux-pr04-*` labels 同步写进 PR07 final audit checklist，避免 PR06 rebaseline 后遗忘。

## Open Questions

1. PR04 是否要实现 per-tree collapse？如果是，需要新增 typed collapse state；如果不是，§5 的“折叠/展开状态”应改为“本 PR 不新增 per-tree collapse，只保留 preview expanded/collapsed”。
2. `ui.state.*` icon 是否必须和 talent icon 同行双图标显示？如果 UI 空间不足，应在文档中指定降级策略：优先保留 state icon + talent name，skill icon 可在 PR06 后进入 tooltip/preview。
3. PR04 是否要求 packaged app 白盒，还是只需要 generated runbook + manual record？当前 `whitebox=required` 需要明确是否必须实际启动 app。

## Suggested Verification

文档修正后建议运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest
./gradlew manifestLint contractLint localeLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint
./gradlew verifyChanged
```

白盒：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01
```

手工记录必须落到 `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md`，并引用 generated `expected-evidence.json`、CUA runbook、screenshot sha256、app sha256 和 residual risk。

## Executed Validation

已运行：

- `git diff --check -- UI/review/2026-05-09-dark-uiux-pr04-pr-level-standard-review.md`：通过，无 whitespace/error 输出。
- `awk 'BEGIN{c=0} /^```/{c++} END{if (c % 2 != 0) {print "unbalanced fences: " c; exit 1} print "balanced fences: " c}' UI/review/2026-05-09-dark-uiux-pr04-pr-level-standard-review.md`：通过，`balanced fences: 6`。
- absolute-path pattern scan for `UI/review/2026-05-09-dark-uiux-pr04-pr-level-standard-review.md`：通过，无本机绝对路径命中。
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint`：通过，`BUILD SUCCESSFUL in 1s`，`21 actionable tasks: 1 executed, 20 up-to-date`。该任务触发 `syncPhase2Manifests`，随后检查 `client/src/main/resources/manifests/visual-manifest.json`、`client/src/main/resources/manifests/audio-manifest.json`、`assets-src/image/manifests/phase2-visual-manifest.json`、`assets-src/audio/manifests/phase2-audio-manifest.json` 无 diff。

## Summary

PR04 文档已经有正确的大方向和主要边界：只改 client presentation/layout，不改职业树规则，不批量生图，复用 `TalentSidebarPresenter` 和 `phase4-v4-pr01`。但按 2026-05-09 的 PR 级 review standard，它还缺“开发者无需猜测”的关键层：字段合同、状态机、跨 PR dependency、whitebox artifact 和 removal/rebaseline 责任。先把上述 P1/P2 修进 PR 文档，再进入实现会显著降低返工和验收漂移。

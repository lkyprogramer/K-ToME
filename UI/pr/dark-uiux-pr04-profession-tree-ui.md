# Dark UI/UX PR-04 Profession Tree UI

**阶段**: `dark-uiux-pr04-profession-tree-ui`
**优先级**: `P0`
**工作量**: `M`
**前置条件**: PR-01、PR-01-1、PR-02、PR-03 串行完成；`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md` 的职业树语义已落地，且目标分支能找到 `phase4-v4-pr01` scenario、`TalentSidebarPresenter` 和 `ACTIVE_TALENT_SLOT_CHOICE`。
**资源生成结论**: 默认复用现有资源，不批量生图；职业树 skill / tree / portrait icon 正式重绘和 rebaseline 放到 PR-06。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再跑 talent UI focused tests、resource / manifest dependency checks、client evidence、maintainability gate 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox | crossPrDependency | removalOwner |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `UI04-M01-category-state-contract` | §2.1 typed snapshot contract | `client` | `NEW: TalentSidebarPresenterTest.stateAndCategoryAreRenderedBySeparateContracts` | `contractLint` | `build/reports/tests/client/test/index.html` | `N/A` | `phase4-v4-pr01` | `N/A` |
| `UI04-M02-presenter-authority` | §2.2 presentation contract | `client` | `NEW: TalentSidebarPresenterTest.pr04MapsNodeStatesToStateIconAndTalentTone`, `NEW: TalentSidebarPresenterTest.pr04NonNodeRowsDoNotCarryStateFields` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `N/A` | `PR-02 ui.state.*` | `N/A` |
| `UI04-M03-render-row-model` | §5.1 TileRenderModel / TileRenderer row model | `client` | `NEW: TileRendererCanvasTest.darkUiuxPr04DrawsTalentNodePrimaryAndStateIcons`, `NEW: TileRendererCanvasTest.darkUiuxPr04UsesTalentToneTokensForFourStates` | `:client:goldenScreenshot` | `client/build/reports/golden/` | `required` | `PR-02 ui.frame.* / ui.state.*` | `PR-06 icon rebaseline` |
| `UI04-M04-input-number-isolation` | §3 modal/input state machine | `client` | `NEW: InputHandlerTest.talentTreeSidebarIgnoresNumberHotkeysOutsideActiveSlotChoice` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `N/A` | `phase4-v4-pr01` | `N/A` |
| `UI04-M05-active-slot-choice-modal` | §3 active slot choice modal | `client` | `NEW: InputHandlerTest.activeSlotChoiceNumbersReplaceSlotsAndCloseModal`, `NEW: InputHandlerTest.activeSlotChoiceReserveConfirmsToReserve`, `NEW: InputHandlerTest.activeSlotChoiceEscRollsBackDraft` | `:client:clientSmoke` | `build/reports/tests/client/test/index.html` | `required` | `phase4-v4-pr01` | `N/A` |
| `UI04-M06-preview-lock-breakpoint-lines` | §6 preview pane | `client` | `NEW: TalentSidebarPresenterTest.pr04PreviewShowsRankCostBreakpointAndLockReasonLines` | `localeLint`, `contractLint` | `build/reports/tests/client/test/index.html` | `required` | `phase4-v4-pr01` | `N/A` |
| `UI04-M07-race-tree-no-filter` | §2.4 runtime tree contract | `client` | `NEW: TalentSidebarPresenterTest.rendersRaceTreeRowsWithoutFilteringSnapshotTrees`, `EXISTING: InputHandlerTest.\`talent assign respec follows focused tree owner\`` | `contractLint` | `build/reports/tests/client/test/index.html` | `N/A` | `phase4-v4-pr01` | `N/A` |
| `UI04-M08a-pr02-state-key-exact-resolve` | §2.5 PR-02 key dependency | `assets` | `RENAME existing ManifestResolveTest.\`dark ui dry-run frame keys resolve through exact manifest entries\` to ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries and extend it to all PR02 state keys` | `manifestLint` | `assets-src/image/manifests/phase2-visual-manifest.json`, `client/src/main/resources/manifests/visual-manifest.json` | `N/A` | `PR-02 UI02-M04` | `N/A` |
| `UI04-M08b-state-key-consumed-by-client` | §2.2 `stateIconKey` renderer rule | `client` | `NEW: TileRendererCanvasTest.darkUiuxPr04FailsEvidenceWhenStateIconsFallback` | `:client:goldenScreenshot` | `build/reports/tests/client/test/index.html`, `client/build/reports/golden/` | `N/A` | `PR-02 UI02-M04` | `N/A` |
| `UI04-M09a-upstream-whitebox-materialization` | §8.2 upstream whitebox preparation | `tools` | `EXISTING: Phase4V4WhiteboxScenarioCliTest.\`pr01 scenario generates profession tree evidence names from the typed registry\`` | `preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01` | `build/whitebox/phase4-v4-pr01/expected-evidence.json`, `build/whitebox/phase4-v4-pr01/cua-runbook.md` | `required` | `phase4-v4-pr01` | `N/A` |
| `UI04-M09b-pr04-manual-label-capture` | §8.2 PR04 dark label mapping | `docs` | `NEW: acceptanceContractLint.pr04ManualLabelMapping` | packaged whitebox manual record | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | `required` | `UI04-M09a-upstream-whitebox-materialization` | `N/A` |
| `UI04-M10a-min-window-rendering` | §6 min-window behavior | `client` | `NEW: TileRendererCanvasTest.darkUiuxPr04TalentSidebarDoesNotOverlapBottomLogAtMinWindow` | `:client:goldenScreenshot` | `client/build/reports/golden/` | `required` | `PR-01 shell min-window` | `N/A` |
| `UI04-M10b-min-window-manual-record` | §8.3 min-window manual evidence | `docs` | `NEW: acceptanceContractLint.pr04ManualLabelMapping` | packaged whitebox manual record | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | `required` | `UI04-M10a-min-window-rendering` | `N/A` |
| `UI04-M11-rebaseline-removal-plan` | §9 removal / iteration plan | `docs` | `acceptanceContractLint` | `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` | `PR-06 dark-uiux-pr06-talent-icon-rebaseline`, `PR-07 dark-uiux-pr07-final-all-screens` | `PR-06` |

### Gate Budget

`durationSource` 固定指 `build/verification/verify-changed/full-task-duration-summary.{json,md}`；首次 `verifyChanged` 之前，每个 gate 的耗时来源都写 `N/A before first verifyChanged`，不得使用 `same as above` 这类位置相关引用。

| Gate group | Tasks | Trigger | Freshness / evidence rule | Duration source |
| --- | --- | --- | --- | --- |
| contract preflight | `acceptanceContractLint` | PR 文档新增 presentation、whitebox、cross-PR 和 removal 合同 | 必须先于实现跑；失败先修文档，不用 client test 代替 | `durationSource` or `N/A before first verifyChanged` |
| fast lane | `:client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.screen.ValidationScenarioBootstrapTest`, `:tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest` | presenter/model/input/whitebox 语义变化 | 新增或改 public presentation model、modal command、row layout 时必须刷新对应 focused test | `durationSource` or `N/A before first verifyChanged` |
| resource / manifest dependency | `manifestLint`, `contractLint`, `localeLint` | PR04 消费 PR02 `ui.frame.*`、`ui.state.*` 和 talent locale 文案 | PR04 不新增 formal key；若缺 key，回 PR02 owner scope 修 registry/manifest，不在 PR04 裸字符串 fallback | `durationSource` or `N/A before first verifyChanged` |
| client evidence | `:client:clientSmoke`, `:client:goldenScreenshot` | player-visible talent sidebar、active slot modal、min-window log overlap | `dark-uiux-pr04-*` golden / manual evidence 必须刷新；PR06 rebaseline 前旧 icon 只允许作为 §9 临时状态 | `durationSource` or `N/A before first verifyChanged` |
| governance / final closure | `maintainabilityLint`, `verifyChanged` | 新增 presentation model、renderer shared row layout 或 5 个以上 Kotlin 文件 | `maintainabilityLint` 新增 finding 不能用 baseline 掩盖；`verifyChanged` 是最终 closure，不替代 owner gate | `durationSource` or `N/A before first verifyChanged` |
| packaged whitebox | `:client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01` | PR04 requires manual whitebox evidence | generated files 不提交；manual record 必须记录 runbook、expected evidence、screenshot/app hash 和 residual risk | manual record 写入实际运行耗时或 `N/A not run` |

### Canonical Artifact

canonical evidence 固定为：

| Artifact | Path | Owner | Rule |
| --- | --- | --- | --- |
| focused test report | `build/reports/tests/client/test/index.html`, `tools/build/reports/tests/test/index.html` | PR04 | 必须覆盖 presenter、input、renderer、manifest dependency、scenario bootstrap / materialization |
| client golden | `client/build/reports/golden/` | PR04 | 至少包含 `dark-uiux-pr04-talent-sidebar-start`、`dark-uiux-pr04-active-slot-choice`、`dark-uiux-pr04-talent-sidebar-min-window-log-visible` 的 evidence 或等价 hash/report |
| PR04 manual record | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | PR04 | 必须引用 generated `build/whitebox/phase4-v4-pr01/cua-runbook.md`、`build/whitebox/phase4-v4-pr01/expected-evidence.json`、app sha256、runtime home、screenshot path / sha256、skip rule |
| upstream gameplay manual record | `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md` | upstream v4 PR01 | 只作为 gameplay baseline；PR04 不覆写它 |
| verify duration | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | tools | 用于 Gate Budget 复盘；不存在时记录 `N/A before first verifyChanged` |

### Failure Rule

如果职业树 UI 失败，按以下顺序定位，不得直接改规则层：

1. `TalentSidebarPresenter` 是否输出完整 presentation fields。
2. `TileRenderModel` / `TileRenderer` 是否只消费 presentation fields，且没有根据 localized text 或 role 重新推导规则状态。
3. `InputHandler` 是否在 `ACTIVE_TALENT_SLOT_CHOICE` 内唯一消费 `1-4 / R / Esc`。
4. PR02 `ui.frame.*` / `ui.state.*` key 是否能通过 canonical/runtime manifest 解析。
5. `RenderUiStateSnapshot.talentTrees` 是否被 client 原样渲染；client 不得过滤 race tree。

不得通过修改 `TalentProgression.learnableTalentIds`、starter 数、Tier 门槛、owner metric、longRun 分母、`phase4-v4-pr01` gameplay setup 或日志事件断言来修 PR04 UI。

## 1. 阶段目标

1. 把职业树 UI 接入新的暗黑 UI 框架，覆盖 tree header、node row、preview pane、active slot strip 和 active slot choice modal。
2. 保留 `TalentSidebarPresenter` 作为 presentation authority；renderer 只渲染 presenter 输出，不重拼 rank、lock reason、状态文案或 talent rule。
3. 展示 `LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE` 四态、职业点数、种族点数、锁定原因、学习预览、rank before/after 和 breakpoint preview。
4. `ACTIVE_TALENT_SLOT_CHOICE` modal 使用新 UI chrome：`1-4` 替换、`R` 入 reserve、`Esc` 取消。
5. 数字键在 talent sidebar 打开但 active slot modal 未打开时不改变选择；仅 active slot modal 消费 `1-4`。
6. 阶段范围限定：PR04 close 时职业树 icon、tree portrait、skill icon 仍可指向现有 manifest entry；PR06 才统一切到 dark-v1 skill/tree 资源并 rebaseline PR04 golden。

“三棵职业树”只定义 PR04 manual/golden 的最小必验面：当前已选职业的三棵 profession tree。runtime 渲染不得过滤 `RenderUiStateSnapshot.talentTrees`。如果 snapshot 包含 `ownerType=RACE` 的 tree，PR04 必须按同一 node row / presentation contract 渲染，并使用 `raceTalentPoints` 作为点数来源。PR04 不新增 race tree 规则，不新增 client-side owner 过滤。全仓职业覆盖口径以 [repo README](../../README.md) 的职业覆盖分类为准：4 个 release playable、2 个 dev playable/report-only、2 个 frozen excluded。

## 2. 硬依赖合同

### 2.1 Snapshot 与 typed enum

1. `TalentTreeNodeSnapshot.category: TalentCategory` 与 `TalentTreeNodeSnapshot.state: TalentNodeStateSnapshot` 都是 typed enum。
2. PR04 四态 UI 只消费 `state`；主动槽选择是否需要 replacement modal 只消费 `category in ACTIVE/SUSTAINED`。
3. client 不做字符串 `valueOf` 解析，不从 localized text、glyph、rank 文案或 icon key 推断 state/category。
4. 修改 `TalentSidebarPresenter` 时禁止新增会触发 `TalentProgression` 派生计算的字段或方法；只允许新增纯展示态、layout hint 或已存在 snapshot 的投影字段。

### 2.2 TalentSidebar presentation contract

`TalentSidebarPresenter` 必须输出 renderer 足够消费的 exact presentation model。PR04 直接修改现有 `TalentSidebarLine`，不新增并行的 `TalentSidebarNodeLine` / `TalentSidebarPresentationLine` 类型：

```kotlin
enum class TalentSidebarToneToken {
    TALENT_LOCKED,
    TALENT_LEARNABLE,
    TALENT_RESERVE,
    TALENT_ACTIVE,
}

data class TalentSidebarLine(
    val text: String,
    val role: TalentSidebarLineRole,
    val primaryIconKey: String? = null,
    val stateIconKey: String? = null,
    val toneToken: TalentSidebarToneToken? = null,
    val rankText: String? = null,
    val selected: Boolean = false,
)
```

`TalentSidebarToneToken` 由 `client.ui.talent` 拥有；renderer 只做 token 到 `UiDesignTokens.color.talent.*` 的映射，不拥有四态判断。

| Field | Owner | Source | Rule | Renderer rule |
| --- | --- | --- | --- | --- |
| `text` | `TalentSidebarPresenter` | locale + snapshot | 已本地化展示文本；不得包含 ASCII 状态 glyph；rank / lock reason 不由 renderer 拼接 | 只绘制文本，可按宽度 truncate |
| `role` | `TalentSidebarPresenter` | line purpose | 只表达 row 类型和 accessibility / layout hint；不得作为状态唯一真源 | 可用于 row spacing，不得重新计算 state |
| `primaryIconKey` | `TalentSidebarPresenter` | `tree.iconKey` / `node.iconKey` | tree / talent icon，可空；PR04 默认复用现有 `icon.skill.*` / `icon.tree.*` / `tree.*` / `portrait.*` | 只通过 `VisualManifestResolver` 解析和 drawAsset |
| `stateIconKey` | `TalentSidebarPresenter` | `TalentNodeStateSnapshot.state` | 仅 node row 使用，固定映射 `LOCKED -> ui.state.locked.icon`、`LEARNABLE -> ui.state.learnable.icon`、`LEARNED_RESERVE -> ui.state.reserve.icon`、`LEARNED_ACTIVE -> ui.state.active.icon` | 必须 exact resolve：`fallbackUsed=false` 且 `matchedByPrefix=false`；缺 exact key 是 PR02 dependency failure |
| `toneToken` | `TalentSidebarPresenter` | `TalentNodeStateSnapshot.state` | 固定映射 `TALENT_LOCKED / TALENT_LEARNABLE / TALENT_RESERVE / TALENT_ACTIVE`；选中态只叠加 focus ring，不改变 state identity | 只消费 `UiDesignTokens.color.talent.*`，不得临场拍颜色或从 `role/text/iconKey` 反推状态 |
| `rankText` | `TalentSidebarPresenter` | `rank` / `committedRank` / `maxRank` | 显示 `rank/maxRank`；存在 pending allocation 时显示 before/after 语义，如 `committed->rank/maxRank` | 单独 chip 或 compact suffix，不参与 state 判断 |
| `selected` | `TalentSidebarPresenter` | selection identity | 由 selected identity 判断；flat index 只能作为 navigation cache | 只绘制 focus ring / selected chrome |

正式状态由 `stateIconKey + toneToken + rankText` 表达。历史 ASCII glyph `[x] [+] [r] [*]` 只能作为 debug-only trace，不得出现在正式 talent row 文本里。

Nullability 与 identity 固定如下：

| Field | Node row | Non-node row | Selection identity | Golden identity |
| --- | --- | --- | --- | --- |
| `text` | required | required | excluded | included as visible text |
| `role` | required | required | excluded | included for layout class only |
| `primaryIconKey` | nullable | nullable | excluded | included when non-null |
| `stateIconKey` | required | `null` | excluded | included and exact-resolved |
| `toneToken` | required | `null` unless a future explicit non-node token is added | excluded | included |
| `rankText` | required | `null` | excluded | included |
| `selected` | required | required | excluded; derived from `TalentTreeSelectionIdentity` | included only as focus ring |

### 2.3 Tree ordering、collapse 与 selection identity

1. runtime tree order = `RenderUiStateSnapshot.talentTrees` order；client 不排序。
2. node order = `TalentTreeSnapshot.nodes` order；client 不排序。
3. PR04 不新增 per-tree collapse state；“折叠/展开”仅指现有 `talentTreePreviewExpanded` 对 preview pane 的展开/收起。若后续需要 per-tree collapse，必须新增 typed state owner 和测试，不得在 renderer 临时保存 map。
4. PR04 新增 client-only type `client/src/main/kotlin/com/ktome/client/ui/talent/TalentTreeSelectionIdentity.kt`：

```kotlin
data class TalentTreeSelectionIdentity(
    val talentId: String,
    val treeId: String,
    val ownerType: String,
    val treeOwnerId: String,
)
```

5. `InputHandler` owns selection restoration：`OverlayState.talentTreeSelectionIdentity: TalentTreeSelectionIdentity?` and `ModalFrameLocalState.talentTreeSelectionIdentity: TalentTreeSelectionIdentity?` are the persisted UI identity. `talentTreeSelection: Int` may remain as an internal navigation cache only.
6. Snapshot 更新后先用 `TalentTreeSelectionIdentity` 恢复 selected node；找不到 identity 时 clamp 到 first visible node and refresh identity。不得只按 `talentId` 或 flat index 判断 selected。
7. selected row 的 focus ring 不改变 state icon 和 state tone。
8. Required tests: `NEW: InputHandlerTest.talentTreeSelectionRestoresByIdentityWhenSnapshotOrderChanges`, `NEW: TalentSidebarPresenterTest.selectedNodeUsesFullTreeOwnerIdentity`, `NEW: InputHandlerTest.talentTreeSelectionFallsBackToFirstVisibleNodeWhenIdentityDisappears`。

### 2.4 Race tree 口径

1. `RenderUiStateSnapshot.talentTrees` 是 runtime 展示输入，client 不得过滤 `ownerType=RACE`。
2. profession tree 和 race tree 使用同一 row contract、tone contract、preview contract 和 respec owner contract。
3. `raceTalentPoints` 只用于 header / points display，不在 client 重算 race learnability。
4. PR04 的 manual/golden 最小场景可以只覆盖当前职业三棵 profession tree；focused test 必须覆盖 race tree 不被过滤。

### 2.5 PR02 resource dependency

PR04 依赖 PR02 已交付的 `ui.frame.modal.body`、`ui.frame.tooltip.body`、`ui.frame.slot.*` 与 `ui.state.*` key。缺任一 key 时回 PR02 owner scope 补 `key-registry` / manifest / resolver test，不在 PR04 临时写 raw path、裸字符串 fallback 或第二套 UI state icon。

`ui.state.*` 是 blocking exact-entry dependency：`ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` 必须覆盖 `ui.state.locked.icon`、`ui.state.learnable.icon`、`ui.state.active.icon`、`ui.state.reserve.icon`，并断言 `fallbackUsed=false`、`matchedByPrefix=false`。runtime resolver 可以返回 `missing_visual` 防止崩溃，但 PR04 fastCheck / ownerGate / manual record 必须把它标为 blocking failure，不能把 fallback icon 当通过证据。

## 3. Modal 与输入状态机

### 3.1 Active slot choice modal

| State | Input | Command | Modal stack effect | Overlay mode after command | Test |
| --- | --- | --- | --- | --- | --- |
| `ACTIVE_TALENT_SLOT_CHOICE` | `1..4` | `PlayerCommand.ConfirmTalentDraftReplacingSlot(slot)` | close all modal frames | command owner decides next snapshot; PR04 不在 client 猜测结果 | `InputHandlerTest.activeSlotChoiceNumbersReplaceSlotsAndCloseModal` |
| `ACTIVE_TALENT_SLOT_CHOICE` | `R` | `PlayerCommand.ConfirmTalentDraftToReserve` | close all modal frames | command owner decides next snapshot | `InputHandlerTest.activeSlotChoiceReserveConfirmsToReserve` |
| `ACTIVE_TALENT_SLOT_CHOICE` | `Esc` | `PlayerCommand.RollbackTalentDraft` | close all modal frames | command owner decides next snapshot | `InputHandlerTest.activeSlotChoiceEscRollsBackDraft` |
| `ACTIVE_TALENT_SLOT_CHOICE` | `Backspace` | `null` | no local close unless implementation already routes it through root modal close with an explicit test | stays in modal | add focused assertion if behavior changes |
| `ACTIVE_TALENT_SLOT_CHOICE` | `T` / `Tab` / movement keys | `null` | no selection leak, no map move | stays in modal | add focused assertion if behavior changes |
| `TALENT_ASSIGN` without active slot modal | `1..4` | `null` | no selection change | stays `TALENT_ASSIGN` | `InputHandlerTest.talentTreeSidebarIgnoresNumberHotkeysOutsideActiveSlotChoice` |

### 3.2 Active slot strip contract

PR04 固定 active slot strip 的 player-facing surface：位于 Talent panel 内，紧跟 points / active-slot-choice action block 之后、tree sections 之前。`ACTIVE_TALENT_SLOT_CHOICE` modal 打开时复用同一 strip，并在 slot item 上显示 replacement target affordance；bottom HUD hotbar 不能作为 PR04 active slot strip 的唯一证据。

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
    val labelText: String,
    val state: TalentActiveSlotStripState,
    val pending: Boolean,
    val selected: Boolean,
)
```

规则：

1. slot count 固定 `PLAYER_ACTIVE_TALENT_SLOT_COUNT = 4`，PR04 不改该常量。
2. slot order 固定 `1..4`。
3. filled slot 来源为 `RenderUiStateSnapshot.talents.slot`。
4. empty slot = `1..4 - talents.slot`；四槽未满时 pending active/sustained talent confirm 不打开 `ACTIVE_TALENT_SLOT_CHOICE`，strip 以 `PENDING_DIRECT_FILL` 标出目标空槽。
5. four-slot-full 且 pending node `category in ACTIVE/SUSTAINED` 时打开 active slot choice；modal strip 中 1-4 slots 使用 `PENDING_REPLACE_TARGET` 表示可替换目标。
6. passive draft never sets pending active slot state and never opens `ACTIVE_TALENT_SLOT_CHOICE`。
7. reserve prompt 来源于 pending node command path 和 existing reserve presentation；client 不自算 learnability。
8. Required evidence: `dark-uiux-pr04-talent-sidebar-start` covers filled/empty strip when scenario has an empty slot；`dark-uiux-pr04-active-slot-choice` covers four filled slots, pending replace affordance, and reserve hint。
9. Required tests: `NEW: TileRendererCanvasTest.darkUiuxPr04ActiveSlotStripShowsFilledEmptyPendingAndReserveHint`, `EXISTING: InputHandlerTest.\`pending active or sustained tree talent opens active slot choice when loadout is full\``, `NEW: InputHandlerTest.pendingActiveTalentDoesNotOpenReplacementModalWhenAnySlotIsEmpty`。

## 4. 前置检查与 Stop Condition

PR04 开工前必须先执行只读检查：

```bash
rg -n "TalentSidebarPresenter|TalentTreeNodeSnapshot|ACTIVE_TALENT_SLOT_CHOICE" client core game
rg -n "ui.state.locked.icon|ui.state.learnable.icon|ui.state.active.icon|ui.state.reserve.icon" UI/pr UI/sprite-sheets client assets-src
```

1. `TalentSidebarPresenter`、`TalentTreeNodeSnapshot`、`ACTIVE_TALENT_SLOT_CHOICE`、`phase4-v4-pr01` scenario 都存在：继续本 PR，只做 UI chrome / layout / presentation。
2. 任一上游 gameplay contract 不存在：停止 PR04，先合入或切到上游职业树分支。
3. PR02 key 不存在或 runtime manifest 无法解析：停止 PR04，回 PR02 resource owner scope 修 key registry / manifest / resolver test。
4. presentation 缺字段：在 `client` presentation boundary 补字段；不在 `core/game` 新增职业树语义字段。
5. 禁止为了 UI 临时 mock 第二套 talent snapshot、第二套 tree owner 过滤或第二套 state glyph authority。

## 5. 影响范围与实现顺序

### 5.1 Added / Modified / Deleted

| Kind | Path | Owner | Input | Output | Failure semantics | Test / evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Modified | `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt` | `client` | `RenderUiStateSnapshot`, `OverlayState` | talent lines with primary icon, state icon, tone token, rank text, selected identity | no rule recomputation; no localized-text parsing | `TalentSidebarPresenterTest.pr04MapsNodeStatesToStateIconAndTalentTone` |
| Added | `client/src/main/kotlin/com/ktome/client/ui/talent/TalentTreeSelectionIdentity.kt` | `client` | selected `TalentTreeNodeSnapshot` | client-only selection identity | no rule recomputation; not serialized into save/replay | `InputHandlerTest.talentTreeSelectionRestoresByIdentityWhenSnapshotOrderChanges` |
| Modified | `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt` | `client` | modal-local talent selection | `talentTreeSelectionIdentity` persisted with modal frame | flat index is cache only; identity restore wins | `InputHandlerTest.talentTreeSelectionRestoresByIdentityWhenSnapshotOrderChanges` |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | `client` | `TalentSidebarLine` | `TileTextRow` / row model with primary icon, exact state icon, tone token, selection | talent primary icon may use resolver fallback; state icon exact resolve failure is blocking evidence failure; no state remap | `TileRendererCanvasTest.darkUiuxPr04DrawsTalentNodePrimaryAndStateIcons` |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | `client` | row model | draw primary/state icons, rank chip, focus ring and text without overlap | stable row height; truncate long text; no bottom-log overlap at min window | `TileRendererCanvasTest.darkUiuxPr04TalentSidebarDoesNotOverlapBottomLogAtMinWindow` |
| Modified | `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` | `client` | modal stack + key input | typed `PlayerCommand` for active slot choice | no number hotkey leak outside active slot modal | `InputHandlerTest.activeSlotChoice*`, `InputHandlerTest.talentTreeSidebarIgnoresNumberHotkeysOutsideActiveSlotChoice` |
| Modified | `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt` | `client` | existing talent tone tokens | consume `talent-locked/learnable/reserve/active` | no new color family unless style bible changes | `TileRendererCanvasTest.darkUiuxPr04UsesTalentToneTokensForFourStates` |
| Modified | `client/src/test/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenterTest.kt` | `client` | synthetic snapshot | state/category/race/preview assertions | fail on glyph text or missing state icon/tone | focused test |
| Modified | `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt` | `client` | modal stack fixtures | `1-4/R/Esc` command assertions | fail on command leak or stale modal | focused test |
| Modified | `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | `client` | deterministic canvas snapshot | icon draw, tone, layout and overlap assertions | fail on overlap or missing icon draw | focused test / golden |
| Added | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | `docs` | generated whitebox evidence | PR04 manual evidence | required unless §8 skip rule is explicitly triggered | manual record |
| Deleted | `N/A` | `docs` | `N/A` | `N/A` | PR04 删除旧 ASCII glyph authority by behavior; no tracked file deletion expected | §9 removal plan; PR06 owns icon rebaseline |

### 5.2 Implementation Order

1. Preflight upstream contracts：确认 PR03 close/base、`phase4-v4-pr01` scenario、PR02 `ui.frame.*` / `ui.state.*` resolver test、screen coverage labels。
2. Extend presenter contract：修改现有 `TalentSidebarLine` exact fields，输出 `primaryIconKey`、`stateIconKey`、`toneToken`、`rankText`、selection identity；删除正式 row 文本里的 ASCII state glyph。
3. Add identity model：新增 `TalentTreeSelectionIdentity`，让 `InputHandler` / `ModalFrameLocalState` 按 identity 恢复 selection，flat index 只作为 cache。
4. Add active slot strip model：新增 `TalentActiveSlotStripItem` / `TalentActiveSlotStripState`，固定 Talent panel strip 和 modal replacement affordance。
5. Add presenter/input tests：先补四态、category/state 分离、race tree 不过滤、rank/preview/lock reason、selection identity、active slot strip 和 modal command 测试。
6. Update render model and renderer：把 presenter fields 映射到 renderer row model；画 primary icon + exact state icon + rank chip + active slot strip；保证 focus ring 不覆盖 state identity。
7. Add canvas tests：覆盖双 icon、四态 tone、active slot strip、min-window bottom log not overlapped、long zh text truncation。
8. Run resource dependency checks：确认 PR02 keys exact resolve；如果四个 `ui.state.*` 任一 fallback，则停止 PR04 并回 PR02。
9. Refresh golden and whitebox：刷新 `dark-uiux-pr04-*` labels；PR04 manual record 引用 upstream generated runbook / expected evidence，并填写 §8.2 dark label mapping。
10. Final self-audit：按 §10 对 doc-vs-implementation、cross-PR evidence、removal/rebaseline owner 和未运行项逐条确认。

## 6. UI 改造范围

1. Talent panel header：职业名、职业天赋点、种族天赋点；race points 只展示，不触发 client-side learnability 计算。
2. Tree section：tree icon、tree name、当前选中态；PR04 不新增 per-tree collapse。
3. Node row：primary icon、`ui.state.locked/learnable/active/reserve.icon` 状态 icon、rank chip、四态 tone、focus ring。
4. Preview pane：cost、rank before/after、breakpoint、锁定原因 tokenized lines；说明文本来自 `DescriptionPresenter.presentTalentTreeNodeLines`。
5. Active slot strip：Talent panel 内固定 4 个主动槽、filled/empty/pending/replacement target、reserve 提示；不改变 hotbar 规则，bottom HUD 不作为唯一证据。
6. Active slot choice modal：新暗黑 modal chrome，保留 §3 输入语义。
7. Min-window behavior：在 PR01/PR01-1 最小窗口口径下，talent panel 不遮挡 bottom log 最新关键反馈。

## 7. Cross-PR Dependency

| PR04 requirement | Dependency | Type | Verification |
| --- | --- | --- | --- |
| `UI04-M01-category-state-contract` | `phase4-v4-pr01` | hard upstream | `phase4-v4-pr01` scenario registered; `TalentTreeNodeSnapshot.state/category` exist |
| `UI04-M02-presenter-authority` | `TalentSidebarPresenter` current implementation | hard upstream | presenter test starts from existing talent sidebar behavior |
| `UI04-M03-render-row-model` | PR02 `ui.frame.*` / `ui.state.*` | hard upstream | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`, `manifestLint` |
| `UI04-M08a-pr02-state-key-exact-resolve` | PR02 `ui.state.*` canonical/runtime manifest entries | hard upstream | all four state icons exact-resolve with no fallback |
| `UI04-M09b-pr04-manual-label-capture` | `UI04-M09a-upstream-whitebox-materialization` | hard upstream | manual capture labels map to upstream runbook and actual screenshot paths |
| `UI04-M07-race-tree-no-filter` | `RenderUiStateSnapshot.talentTrees` includes profession + race when present | hard upstream | `TalentSidebarPresenterTest.rendersRaceTreeRowsWithoutFilteringSnapshotTrees` |
| `UI04-M10a-min-window-rendering` | PR01 / PR01-1 shell min-window | hard upstream | use same minimum viewport family; add PR04 label |
| `UI04-M11-rebaseline-removal-plan` | PR06 `dark-uiux-pr06-talent-icon-rebaseline` | downstream rebaseline | PR06 must refresh PR04 talent sidebar / active slot golden after skill/tree icon replacement |
| `UI04-M11-rebaseline-removal-plan` | PR07 `dark-uiux-pr07-final-all-screens` | downstream audit | PR07 final index must link PR04 labels and show no old-style residue |

PR04 branch base must be after PR03 close. If implementation deliberately starts from a different base, the PR description and §10 self-audit must state the exception, branch/base ref, and why no PR03 conflict exists.

## 8. 验证与白盒

### 8.1 Gate ladder

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.screen.ValidationScenarioBootstrapTest :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest
./gradlew manifestLint contractLint localeLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint
./gradlew verifyChanged
```

资源说明：

1. PR04 不新增 formal key 时不跑 owner-scope dark coverage 作为 blocking gate；但 PR02 `ui.frame.*` / `ui.state.*` resolver 和 manifest freshness 必须通过。
2. 如果 PR04 实现新增任何 visual key，必须先回 PR02/PR06 resource owner 文档修 key registry / sheet plan / manifest，再决定是否扩大 PR04 范围。

### 8.2 Whitebox preparation

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01
```

`preparePhase4V4Whitebox` generated output 不提交；PR04 manual record 必须记录以下字段：

| Field | Required value |
| --- | --- |
| manual record path | `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` |
| upstream scenario | `phase4-v4-pr01` |
| generated runbook | `build/whitebox/phase4-v4-pr01/cua-runbook.md` |
| generated expected evidence | `build/whitebox/phase4-v4-pr01/expected-evidence.json` |
| runtime home | `build/whitebox/phase4-v4-pr01/runtime-home` |
| evidence dir | `build/whitebox/phase4-v4-pr01/evidence` |
| app executable hash | value from `build/whitebox/phase4-v4-pr01/app-executable.sha256` |
| PR04 screenshot labels | `dark-uiux-pr04-talent-sidebar-start`, `dark-uiux-pr04-active-slot-choice`, `dark-uiux-pr04-talent-sidebar-min-window-log-visible` |
| required gameplay log keys | `log.talent.learned`, `log.talent.rank_up`, `log.talent.breakpoint_chosen` |
| locale | exact runtime locale, default `zh-CN` unless implementation explicitly validates another locale |
| viewport | exact capture viewport; min-window evidence defaults to `1024x720` unless PR01/PR01-1 final min-window differs |
| residual risk | explicit `N/A` or concrete limitation |

`preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01` 只生成 upstream gameplay baseline。PR04 不新增新的 whitebox task；PR04 dark labels are manual capture labels mapped to the upstream runbook steps. Manual capture must save the PR04 files under the same evidence dir and record this mapping:

| PR04 label | Required screenshot path | Source upstream evidence / step | Required proof |
| --- | --- | --- | --- |
| `dark-uiux-pr04-talent-sidebar-start` | `build/whitebox/phase4-v4-pr01/evidence/dark-uiux-pr04-talent-sidebar-start.png` | `phase4-v4-pr01-talent-tree-start.png` / first Talent UI capture | screenshot sha256, viewport, locale, visible state icon exact resolve |
| `dark-uiux-pr04-active-slot-choice` | `build/whitebox/phase4-v4-pr01/evidence/dark-uiux-pr04-active-slot-choice.png` | `phase4-v4-pr01-reserve-active-slot.png` / active slot choice capture | screenshot sha256, `1-4/R/Esc` visible, four filled slots + replacement affordance + reserve hint |
| `dark-uiux-pr04-talent-sidebar-min-window-log-visible` | `build/whitebox/phase4-v4-pr01/evidence/dark-uiux-pr04-talent-sidebar-min-window-log-visible.png` | manual resize from Talent UI state | screenshot sha256, exact viewport, latest bottom log line visible and not overlapped |

If a future PR adds `prepareDarkUiuxWhitebox`, it may replace the manual mapping table only after it produces the same `dark-uiux-pr04-*` paths and preserves the upstream `phase4-v4-pr01` traceability.

Skip rule：`whitebox=required` 默认必须执行 packaged app whitebox。只有当前机器无法启动 packaged app、CUA 不可用、或用户明确取消人工白盒时才允许 skip；manual record 必须写明原因、替代证据、未覆盖风险和后续 owner。不能用 `goldenScreenshot` 静默替代 packaged app whitebox。

### 8.3 Manual whitebox steps

1. `phase4-v4-pr01` 场景下打开 Talent UI，首屏状态以 generated `expected-evidence.json` 为准；人工记录必须覆盖 learned starter、active/reserve/empty slot、learnable 与 locked 节点，不在本文硬编码具体数量。
2. 学习 rank 0 技能时预览能解释点数消耗、rank before/after 和 breakpoint。
3. 对同一 talent 继续投入直到 generated `expected-evidence.json` 要求的 rank-up / breakpoint evidence 被达成；如果该 log key 只由 upstream generated evidence 覆盖，manual record 必须写 `source=phase4-v4-pr01-generated` 并引用具体 log path / sha256。
4. 四槽满时 active-slot modal 的 `1-4 / R / Esc` 文案、焦点和输入行为正确；每个输入都要能在 command/log/evidence 中追溯。
5. 新 UI 不改变 `log.talent.learned / rank_up / breakpoint_chosen` 的产生条件。
6. 窄窗口验收使用 PR01/PR01-1 最小窗口口径，PR04 推荐固定 `1024x720`；如果项目最终最小窗口不同，manual record 必须写实际尺寸和原因。
7. 窄窗口 expected：bottom log latest line contains one of `log.talent.learned / log.talent.rank_up / log.talent.breakpoint_chosen` and is not overlapped by talent panel bounds。
8. 必填证据：`dark-uiux-pr04-talent-sidebar-start`、`dark-uiux-pr04-active-slot-choice`、`dark-uiux-pr04-talent-sidebar-min-window-log-visible`、`phase4-v4-pr01` generated evidence。

Manual record 的 log evidence table 必须使用以下字段：

| logKey | source | evidencePath | sha256 | residualRisk |
| --- | --- | --- | --- | --- |
| `log.talent.learned` | `pr04-manual` or `phase4-v4-pr01-generated` | repo-relative log path | required | `N/A` or limitation |
| `log.talent.rank_up` | `pr04-manual` or `phase4-v4-pr01-generated` | repo-relative log path | required | `N/A` or limitation |
| `log.talent.breakpoint_chosen` | `pr04-manual` or `phase4-v4-pr01-generated` | repo-relative log path | required | `N/A` or limitation |

## 9. Removal / Iteration Plan

| Temporary surface | Allowed in PR04 | Removal / rebaseline owner | Regression scan | Exit rule |
| --- | --- | --- | --- | --- |
| existing `icon.skill.*` / `icon.tree.*` / `tree.*` / `portrait.*` painterly assets in talent UI | yes | PR06 `dark-uiux-pr06-talent-icon-rebaseline` | `goldenScreenshot`, manifest coverage, `screen-coverage-matrix` final audit | PR06 refreshes `dark-uiux-pr04-talent-sidebar-start` and `dark-uiux-pr04-active-slot-choice`; PR07 final index shows no old-style residue |
| ASCII glyph `[x] [+] [r] [*]` in talent row text | no as formal UI; debug trace only if explicitly documented | PR04 | `TalentSidebarPresenterTest.pr04MapsNodeStatesToStateIconAndTalentTone` | formal row uses `stateIconKey + toneToken`; glyph text not used as state authority and must not appear in golden/manual visible text |
| `ui.state.*` runtime fallback to `missing_visual` | crash-safe only, not passing evidence | PR02 resource owner before PR04 close | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`, `manifestLint`, `TileRendererCanvasTest.darkUiuxPr04FailsEvidenceWhenStateIconsFallback` | exact resolve for all four state icons; fallback is blocking failure |
| `talentTreeSelection: Int` flat index | yes as internal cache | PR04 | selection identity focused tests | flat index no longer owns selected identity; `TalentTreeSelectionIdentity` restore wins after snapshot changes |
| generated whitebox build outputs | no commit | PR04 | manual record references repo-relative generated paths and sha256 | generated files can be regenerated; manual record stores exact evidence |
| PR04 golden labels before PR06 icon replacement | yes | PR04 initial, PR06 rebaseline | `goldenScreenshot`, PR07 final all-screens index | PR06 rebaseline updates PR04 labels or documents unchanged hash |

## 10. Doc-vs-Implementation Self-Audit

PR04 收口前必须在 PR 描述或 review note 中逐条填写：

| Item | Required answer |
| --- | --- |
| base ref / target branch | branch base is after PR03 close, or exception with reason |
| unrelated dirty diff | list excluded files or `N/A` |
| upstream scenario | `phase4-v4-pr01` registry and generated evidence verified |
| PR02 key dependency | `ui.frame.*` / `ui.state.*` resolver and manifest checks passed; state icons exact resolve with no fallback |
| presentation authority | presenter owns exact `TalentSidebarLine` fields and `TalentSidebarToneToken`; renderer does not recompute state |
| modal state machine | `1-4/R/Esc` exact command tests passed |
| selection identity | `TalentTreeSelectionIdentity` fields and snapshot restore tests passed |
| active slot strip | Talent panel placement, model fields, and modal replacement affordance tests passed |
| race tree behavior | runtime does not filter `ownerType=RACE` trees |
| PR04 dark labels | `dark-uiux-pr04-*` label mapping table exists in manual record and points to actual screenshot paths / sha256 |
| min-window behavior | PR04 min-window label exists, viewport is recorded, and log overlap evidence is recorded |
| PR06 rebaseline | old icon surface linked to PR06 owner |
| PR07 final audit | PR04 labels listed for final all-screens index |
| validation not run | every skipped command/manual step has reason and residual risk |

### Review Follow-Up Status

| Previous finding | Current status | Contract anchor |
| --- | --- | --- |
| state icon / tone / rank presentation contract | resolved by exact model contract | §2.2 |
| active slot modal state machine | resolved | §3.1 |
| whitebox artifact / manual record | resolved by upstream materialization plus PR04 manual label mapping | §8.2 |
| race tree 被“三棵职业树”误导 | resolved | §1, §2.4 |
| PR dependency / PR06 rebaseline | resolved | §7, §9 |
| active slot strip surface | resolved by Talent panel placement and model | §3.2, §6 |
| selection identity flat-index authority | resolved by `TalentTreeSelectionIdentity` contract | §2.3, §9 |
| multi-owner / source anchor / duration source ambiguity | resolved by split requirements and stable `durationSource` | §0 |

## 11. 非目标

1. 不改职业树规则。
2. 不改 run summary / owner report。
3. 不改 `phase4-v4-pr01` whitebox scenario 的玩法状态；只允许更新 presentation/layout 相关截图或 PR04 manual evidence，不允许改 talent 状态、learnable、owner metric 或日志事件断言。
4. 不生成新音频。
5. 不批量生成 skill/tree/portrait icon。
6. 不重新设计职业树数据结构，不新增第二套 talent snapshot。
7. 不新增 per-tree collapse state；如未来需要，另起明确 owner 和状态机。
8. 不新增新的 dark UI whitebox Gradle task；PR04 只用 `phase4-v4-pr01` upstream materialization 加 manual label mapping。若未来需要自动化 dark label materialization，必须另行扩展 tools 合同。

## 12. 回滚边界

本 PR 回滚只应影响职业树 UI chrome、layout、presenter/model、renderer、focused tests、golden 和 PR04 manual record。如果回滚需要改 `core` 或 `game` 职业树规则，说明 PR 范围已经越界。PR04 回滚不得删除 PR02 resource keys，不得覆盖 upstream `phase4-v4-pr01` gameplay manual record，不得把 PR06/PR07 rebaseline 责任提前混入本 PR。

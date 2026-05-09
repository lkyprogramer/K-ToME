# Dark UI/UX PR-04 Profession Tree UI Re-Review Round 2

目标文档：`UI/pr/dark-uiux-pr04-profession-tree-ui.md`

上一轮报告：`UI/review/2026-05-09-dark-uiux-pr04-pr-level-standard-review.md`

审查依据：
- `docs/review/rule/pr-level-review-standard.md`
- `UI/PLAN.md`
- `UI/ART_STYLE_BIBLE.md`
- `UI/pr/README.md`
- `UI/pr/development-governance.md`
- `UI/pr/screen-coverage-matrix.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md`
- 当前代码与测试：`TalentSidebarPresenter`、`TileRenderModel`、`TileRenderer`、`InputHandler`、`ModalStack`、`ManifestResolveTest`、`ValidationScenarioBootstrapTest`、`Phase4V4WhiteboxScenarioCliTest`

审查范围：
- 当前分支为 `main...origin/main`，工作区存在多份 UI PR 文档和工具链改动。本报告只审查 PR04 文档是否已经达到“无需猜测即可开发”的执行合同级别；其它 dirty diff 只作为当前 repo surface 线索。
- 本轮按增量复审规则重新核验上一轮 P0/P1/P2，并对整份 PR04 文档做 Pass 0、Pass 7、Pass 8 sanity check。
- `acceptanceContractLint` 已通过，但它只证明矩阵骨架存在，不证明字段、状态机、artifact 和跨 PR label 语义已经无歧义。

结论：PR04 文档相比上一轮已经大幅接近可开发合同。上一轮 P1 中的 race tree、modal 状态机、cross-PR 和 whitebox 字段已经部分或基本吸收；但当前版本仍不能判定为“无需猜测”。主要剩余风险是：public presentation model 仍允许实现命名和 authority 分叉、`ui.state.*` 缺失时 fallback 与 stop condition 冲突、PR04 dark 白盒 label 没有可执行产出路径、selection identity 需要改状态模型但未冻结 exact type/file、active slot strip 的具体 UI surface 仍不唯一。

## Findings

### P0

无。

### P1

#### P1-1 presentation contract 仍允许字段命名和 tone authority 分叉，public model 不能算冻结

状态：上一轮 `P1-1` 部分解决，但仍有阻塞残留。

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:86` 写明字段命名可在实现时微调。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:88-96` 定义了 `primaryIconKey / stateIconKey / toneToken / rankText / selected` 语义，但 `toneToken` 的 Owner 写成 `TalentSidebarPresenter 或 typed enum`。
- `docs/review/rule/pr-level-review-standard.md:79-88` 要求 public contract 冻结类型名、字段名、identity、source of truth 和 fallback。
- `docs/review/rule/pr-level-review-standard.md:90-96` 明确要求把没有优先级和冲突处理的“或”标为问题。
- `docs/review/rule/pr-level-review-standard.md:410-415` 要求所有 public contract 都有 exact names，identity 字段有 included / excluded 说明。
- 当前代码仍只有 `TalentSidebarLine(text, role, iconKey, selected)`，见 `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:25-30`；renderer 当前通过 `role` 映射 tone，见 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:1185-1193`、`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:1706-1727`。
- `UI/ART_STYLE_BIBLE.md:73-88` 冻结职业树四态 token，并禁止 renderer 临场拍颜色。

问题：
文档语义已经对了，但仍把 public presentation model 的 exact name 留给实现者决定。`toneToken` 的 owner 也不是唯一：可以理解成 presenter 直接输出字符串 token，也可以理解成新增 typed enum 后由 renderer 映射 token。两种实现都可能声称符合文档，但测试、renderer row model、PR06 rebaseline 和 `maintainabilityLint` 对 public model 的判定会不同。

影响：
- 开发者不知道应修改现有 `TalentSidebarLine`，还是新增 `TalentSidebarNodeLine` / `TalentSidebarPresentationLine` 等新类型。
- renderer 可能继续依赖 `TalentSidebarLineRole.NODE_*` 推断 tone，形成第二 authority。
- 后续 PR06 替换 talent icon 时，不知道 golden drift 属于 icon 资源、state icon、tone token 还是 rank chip 变化。

修复方向：
把 §2.2 改成 exact model contract，不再写“字段命名可微调”。建议直接冻结：

| Contract | Required wording |
| --- | --- |
| data class | `TalentSidebarLine(text, role, primaryIconKey, stateIconKey, toneToken, rankText, selected)` |
| tone enum | `TalentSidebarToneToken { TALENT_LOCKED, TALENT_LEARNABLE, TALENT_RESERVE, TALENT_ACTIVE }`，由 `client.ui.talent` 拥有 |
| nullability | `primaryIconKey/stateIconKey/rankText/toneToken` 对非 node row 必须为 `null`；node row 的 `stateIconKey/toneToken/rankText` 必填 |
| renderer rule | renderer 只把 `toneToken` 映射到 `UiDesignTokens.color.talent.*`，不得从 `role/text/iconKey` 反推 state |
| selected rule | `selected` 只影响 focus ring；不得覆盖 `toneToken` identity |

推荐测试：
- `TalentSidebarPresenterTest.pr04MapsNodeStatesToStateIconAndTalentTone`
- `TalentSidebarPresenterTest.pr04NonNodeRowsDoNotCarryStateFields`
- `TileRendererCanvasTest.darkUiuxPr04UsesTalentToneTokensForFourStates`

#### P1-2 `ui.state.*` 缺失时同时写了 fallback 和 stop condition，会隐藏关键资源失败

状态：上一轮 `P1-1` 的 resource fallback 部分仍未解决。

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:93` 对 `stateIconKey` 写的是“缺 key 时走 manifest resolver fallback”。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:117` 又要求缺任一 PR02 key 时回 PR02 owner scope，不在 PR04 临时写 fallback。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:151-155` 的 Stop Condition 也要求 PR02 key 不存在或 runtime manifest 无法解析时停止 PR04。
- `docs/review/rule/pr-level-review-standard.md:85-88` 要求 source of truth 和 fallback 触发条件唯一。
- `docs/review/rule/pr-level-review-standard.md:180-186` 明确禁止用 `goldenScreenshot` 替代 resource manifest lint。
- 当前工作区中 `ui.state.locked.icon / learnable / active / reserve` 只在 UI PR 文档中出现，未在当前 `UI/sprite-sheets/key-registry.yaml`、canonical manifest 或 runtime manifest 中出现；这说明当前分支尚未满足 PR04 的 PR02 stop condition。

问题：
“renderer fallback”与“缺 key 停止 PR04”是两套失败语义。若实现者按 line 93 处理，`ui.state.*` exact entry 缺失时仍会 fallback 到 `missing_visual`，PR04 UI 可以跑起来但玩家看不到四态 icon，PR02 owner scope 也不会被强制修复。

影响：
- 四态 state icon 可能在截图里显示 fallback / missing visual，却被误判为 PR04 UI layout 通过。
- `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` 的目标会被削弱，因为 PR04 文档允许 renderer fallback。
- PR06/PR07 很难区分“允许的旧 painterly skill icon”与“不允许的 missing state icon”。

修复方向：
把 `stateIconKey` 的 renderer rule 改成：

> `stateIconKey` 必须通过 exact manifest entry resolve，`fallbackUsed=false` 且 `matchedByPrefix=false`。缺 exact `ui.state.*` key 是 PR02 dependency failure，必须停止 PR04 并回 PR02 owner scope 修 registry / sheet plan / canonical manifest / runtime manifest / resolver test；PR04 renderer 不把 missing state icon 当合法 fallback。

如果运行时仍必须防崩溃，可以补充：

> runtime resolver 可返回 `missing_visual` 作为 crash-safe rendering fallback，但 PR04 fastCheck / ownerGate 必须 fail；manual record 必须把它记录为 blocking failure，不能作为通过证据。

推荐测试：
- `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` 必须覆盖四个 `ui.state.*`，并断言 exact resolve。
- `TileRendererCanvasTest.darkUiuxPr04FailsEvidenceWhenStateIconsFallback` 或等价 canvas/assertion，不允许 fallback icon 被当成通过。

#### P1-3 PR04 dark 白盒 label 没有可执行产出路径，`phase4-v4-pr01` materialization 只会生成 upstream label

状态：上一轮 `P1-3` 部分解决，但仍缺最关键的 label materialization 路径。

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:47-49` 把 `dark-uiux-pr04-talent-sidebar-start`、`dark-uiux-pr04-active-slot-choice`、`dark-uiux-pr04-talent-sidebar-min-window-log-visible` 列为 PR04 canonical golden / manual evidence。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:232-236` 指定唯一 whitebox preparation 命令为 `preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01`。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:249`、`UI/pr/dark-uiux-pr04-profession-tree-ui.md:263` 要求 PR04 screenshot labels 使用 `dark-uiux-pr04-*`。
- `UI/pr/screen-coverage-matrix.md:72-74` 也把这些 `dark-uiux-pr04-*` label 固定为最低要求。
- 现有 `Phase4V4WhiteboxScenarioCliTest` 对 `phase4-v4-pr01` 的断言是 `phase4-v4-pr01-talent-tree-start.png`、`phase4-v4-pr01-reserve-active-slot.png` 等 upstream label，见 `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt:85-112`。

问题：
文档现在要求两套证据：upstream `phase4-v4-pr01` generated evidence 和 PR04 dark UI labels。但唯一给出的 materialization 命令只会生成 upstream scenario runbook / expected evidence，不会自动产生 `dark-uiux-pr04-*` label。开发者需要猜：是手工截图时重命名、扩展 materialization catalog、增加 golden label，还是由 PR07 final index 补？

影响：
- PR04 manual record 可能引用不到实际存在的 `dark-uiux-pr04-*` screenshot path / sha256。
- PR07 final all-screens index 无法机械确认 PR04 labels 是 PR04 生成、PR06 rebaseline 更新，还是人工临时命名。
- 白盒会停留在“可以打开 upstream 场景”，但不能证明 dark UI 的职业树侧栏、active slot modal、min-window log label 已产出。

修复方向：
在 §8.2 / §8.3 增加 PR04 dark label capture 规则，二选一但必须唯一：

1. **推荐**：新增 PR04 UI evidence materialization 入口，例如 `prepareDarkUiuxWhitebox -Pktome.darkUiux.pr=PR-04 -Pktome.whitebox.scenario=phase4-v4-pr01`，产出 `build/whitebox/dark-uiux-pr04-profession-tree-ui/expected-evidence.json` 和 `dark-uiux-pr04-*` runbook labels。
2. **最小改法**：明确 `preparePhase4V4Whitebox` 只生成 upstream gameplay baseline；PR04 manual record 另设“manual capture labels”表，逐项写 `dark-uiux-pr04-* -> actual screenshot path -> sha256 -> capture step -> source upstream step`。

同时把 `UI04-M09` 拆成 upstream materialization 与 PR04 dark capture 两条 requirement，避免一条 ownerGate 同时证明两件事。

推荐测试：
- `Phase4V4WhiteboxScenarioCliTest.pr01ScenarioGeneratesProfessionTreeEvidenceNamesFromTheTypedRegistry` 继续证明 upstream label。
- 新增 `DarkUiuxWhiteboxEvidenceTest.pr04ProfessionTreeLabelsMapToPhase4V4Pr01Steps` 或等价测试，证明 `dark-uiux-pr04-*` label 可 materialize / 可映射。

#### P1-4 selection identity 写成了合同，但没有冻结 exact state type 和修改面，开发者仍可能保留 flat index authority

状态：上一轮 `P2-2` 部分解决，但从二轮标准看仍影响核心交互路径。

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:102-106` 要求 runtime tree order / node order 不排序，并写明 selection identity = `talentId + treeId + ownerType + treeOwnerId`，flat index 只用于 local navigation cache。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:163-172` 的 Added / Modified / Deleted 清单没有列出 `OverlayState`、`ModalFrameLocalState` 或 exact selection key model。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:177` 只说 presenter 输出 selection identity，没有说明 InputHandler / overlay state 如何保存、恢复、clamp。
- 当前 `OverlayState` 只有 `talentTreeSelection: Int`，见 `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:58-70`；当前 handler 私有状态也是 `Int`，见 `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:127-137`。
- 当前 modal local state 也只有 `talentTreeSelection: Int`，见 `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt:20-27`。
- 当前 presenter 使用 flat index 取 selected talent id，再只按 `talentId` 判断 selected，见 `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:58-74`。
- `docs/review/rule/pr-level-review-standard.md:111-116` 要求 UI PR 覆盖 identity changed / unchanged 和 focus / modal stack 扫描方向。

问题：
文档已经认识到 flat index 不能作为 identity，但没有把它落成 exact state owner。开发者可以只在 presenter 内用 `talentId` 做比较，也可以新增 overlay identity，也可以保持现状只写测试名。三种实现对 race tree、同 talentId 跨 owner、snapshot 更新后恢复 selection 的行为不同。

影响：
- race tree 或不同 tree 中出现相同 `talentId` 时，selected row 可能错亮。
- snapshot 插入/删除 tree node 后，flat index 可能指向另一个节点，用户按 Enter 会投资错误 talent。
- active slot modal open/close 后 local state 仍可能只恢复旧 flat index。

修复方向：
在 §2.3 / §5.1 冻结 exact client-only model 和文件：

| Contract | Required wording |
| --- | --- |
| type | `TalentTreeSelectionIdentity(talentId: String, treeId: String, ownerType: String, treeOwnerId: String)` |
| owner | `client.input` owns selection restoration; presenter only receives resolved selected identity / boolean |
| state fields | `OverlayState.talentTreeSelectionIdentity: TalentTreeSelectionIdentity?` and `ModalFrameLocalState.talentTreeSelectionIdentity: TalentTreeSelectionIdentity?` if modal-local persistence is needed |
| cache | `talentTreeSelection: Int` may remain internal navigation cache only; it must be recomputed from identity after snapshot changes |
| fallback | if identity missing or not found, clamp to first visible node and refresh identity |
| tests | cover duplicate `talentId` across owner/tree, identity unchanged after node insertion, identity missing fallback |

推荐测试：
- `InputHandlerTest.talentTreeSelectionRestoresByIdentityWhenSnapshotOrderChanges`
- `TalentSidebarPresenterTest.selectedNodeUsesFullTreeOwnerIdentity`
- `InputHandlerTest.talentTreeSelectionFallsBackToFirstVisibleNodeWhenIdentityDisappears`

### P2

#### P2-1 active slot strip 仍没有唯一 UI surface 和 presentation model，容易被实现到不同位置

状态：上一轮未完全显性暴露；这是 PR04 扩写 active slot strip contract 后的新剩余细节。

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:66` 把 `active slot strip` 列入阶段目标。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:134-140` 定义 filled / empty / pending / reserve hint，但没有指定该 strip 位于 talent panel、bottom HUD hotbar、active slot choice modal 还是 loadout panel。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:192` 继续写 active slot strip，但 §5.1 只列 `TileRenderModel` / `TileRenderer` 泛化修改，没有列 exact model 字段。
- 当前 active slot 可视化主要来自 `snapshot.uiState.talents.associateBy(TalentSlotSnapshot::slot)` 后构造 loadout rows，见 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:942-961`；bottom HUD hotbar 也消费 active talent slot，见 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:388-393` 和 `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:445-449`。
- `UI/pr/screen-coverage-matrix.md:42-43` 把职业树侧栏和主动槽选择 modal 分成两个 surface。

问题：
`active slot strip` 是玩家在确认 active/sustained talent 时必须理解的关键 UI，但文档没有冻结放置位置和数据流。实现者可能在 bottom HUD hotbar 上加 pending marker，也可能在 talent sidebar 里画 4 槽 strip，也可能只在 modal 文案里列 `1-4`。

影响：
- `dark-uiux-pr04-active-slot-choice` golden 不能判断 strip 缺失是 bug 还是设计选择。
- 空槽未满时“不打开 modal”的路径缺少对应 UI evidence；四槽满时 replacement target 的视觉选择也不唯一。
- PR07 final all-screens index 无法判断 active slot strip 是否属于职业树侧栏证据还是 modal 证据。

修复方向：
在 §3.2 或 §6 增加 exact surface contract：

| Field | Required value |
| --- | --- |
| placement | choose one: `Talent panel active slot strip above preview` / `bottom HUD hotbar overlay` / `ACTIVE_TALENT_SLOT_CHOICE modal slot list` |
| model | exact row/model fields, e.g. `TalentActiveSlotStripModel(slot, talentId?, primaryIconKey?, state, pending, selected)` |
| source | filled slot from `RenderUiStateSnapshot.talents[].slot`; empty slots from `1..PLAYER_ACTIVE_TALENT_SLOT_COUNT` minus filled slot ids |
| pending rule | pending active/sustained node is shown only while draft exists; passive draft never sets pending active slot state |
| evidence | `dark-uiux-pr04-active-slot-choice` must include all four filled slots and pending/reserve hint; `dark-uiux-pr04-talent-sidebar-start` must cover empty slot if scenario has one |

推荐测试：
- `TileRendererCanvasTest.darkUiuxPr04ActiveSlotStripShowsFilledEmptyPendingAndReserveHint`
- `InputHandlerTest.pendingActiveTalentDoesNotOpenReplacementModalWhenAnySlotIsEmpty`

#### P2-2 Acceptance Matrix 的 multi-owner 行无法机械分派 owner gate 和修复责任

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:24-27` 使用 `assets / client`、`client / docs`、`docs / client` 作为 owner。
- `UI/pr/development-governance.md:22-30` 把 `owner` 收敛为 `client / assets / tools / docs`，没有定义 slash-combined owner 的 gate 分派规则。
- `docs/review/rule/pr-level-review-standard.md:168-176` 要求 owner、ownerGate、artifact、whitebox、removalOwner、crossPrDependency 可追踪。
- `docs/review/rule/pr-level-review-standard.md:180-186` 还要求不能用一个 gate 替代另一个 owner gate。

问题：
slash owner 会让一行 requirement 同时承担资源、client、docs 或 tools 责任，但 ownerGate 只有一个字段。比如 `UI04-M09` 同时写 `client / docs`，ownerGate 是 `preparePhase4V4Whitebox`，无法判断 docs manual record 的 owner gate 是什么；`UI04-M08` 同时写 `assets / client`，但 artifact 是 client test report，未覆盖 registry / manifest canonical artifact。

影响：
- 修复失败时无法机械分派到 PR02 resource owner、PR04 client owner、PR04 docs owner 或 tools materialization owner。
- `acceptanceContractLint` 当前较浅，能通过并不代表矩阵可执行。

修复方向：
把 multi-owner 行拆成单 owner requirement，或增加 `primaryOwner` / `supportingOwner` 字段。推荐拆分：
- `UI04-M08a-pr02-state-key-exact-resolve` owner=`assets`，ownerGate=`manifestLint`，artifact=`assets-src/image/manifests/phase2-visual-manifest.json` + runtime manifest。
- `UI04-M08b-pr02-state-key-consumed-by-client` owner=`client`，fastCheck=`ManifestResolveTest...` 或 canvas test，artifact=`build/reports/tests/client/test/index.html`。
- `UI04-M09a-upstream-whitebox-materialization` owner=`tools`。
- `UI04-M09b-pr04-manual-record` owner=`docs`。
- `UI04-M10a-min-window-rendering` owner=`client`；`UI04-M10b-min-window-manual-record` owner=`docs`。

#### P2-3 existing test anchors 使用了不存在的 camelCase 名称，开发者无法直接 grep 到真源

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:24-25` 引用 `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`、`Phase4V4WhiteboxScenarioCliTest.pr01ScenarioGeneratesProfessionTreeEvidenceNamesFromTheTypedRegistry`、`ValidationScenarioBootstrapTest.validSystemPropertyStartsDirectScenarioValidationSessionOptions`。
- 当前 `ManifestResolveTest` 尚无 `darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` 方法；现有相关方法名为 backtick 形式 `dark ui dry-run frame keys resolve through exact manifest entries`，见 `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt:103-115`。
- 当前 `Phase4V4WhiteboxScenarioCliTest` 的 PR01 方法名是 backtick 形式 `pr01 scenario generates profession tree evidence names from the typed registry`，见 `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt:84-112`。
- 当前 `ValidationScenarioBootstrapTest` 的方法名是 backtick 形式 `valid system property starts direct scenario validation session options`，见 `client/src/test/kotlin/com/ktome/client/screen/ValidationScenarioBootstrapTest.kt:16-46`。
- `docs/review/rule/pr-level-review-standard.md:20-24`、`docs/review/rule/pr-level-review-standard.md:410-417` 要求明确测试文件、测试名、artifact。

问题：
文档没有区分“已有测试锚点”和“PR04 需要新增/重命名的测试”。如果 PR04 要求把 backtick 测试改成 camelCase 或新增具体方法，需要在实现顺序里明确；否则开发者按文档 grep 会找不到这些锚点。

影响：
- 实现者可能以为测试已存在而漏补。
- reviewer 无法机械确认 `fastCheck` 是否已经落地。
- `--tests` 命令使用 class 级过滤时可以跑通，但 Acceptance Matrix 的 method-level anchor 仍不可追踪。

修复方向：
把 test anchor 写成以下格式之一：
- existing: `Phase4V4WhiteboxScenarioCliTest.\`pr01 scenario generates profession tree evidence names from the typed registry\``
- new: `NEW: ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`
- rename: `RENAME existing \`dark ui dry-run frame keys resolve through exact manifest entries\` -> darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`

同理，所有 PR04 新测试名应标注 `NEW:`，避免和当前代码混淆。

#### P2-4 Gate Budget 的 `same as above` 不能作为长期 artifact / duration source

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:33-38` 的 Gate Budget 中多行 Duration source 写 `same as above`。
- `docs/review/rule/pr-level-review-standard.md:173` 要求 artifact evidence repo-relative。
- `docs/review/rule/pr-level-review-standard.md:416-421` 要求 resource / manifest / locale / report 字段都有 repo-relative artifact，跨 PR 依赖和删除责任可追踪。
- `UI/pr/development-governance.md:58-65` 要求每个 PR 声明最近耗时来源或 `build/verification/verify-changed/full-task-duration-summary.{json,md}` 的读取方式。

问题：
`same as above` 对人类可读，但不是长期可搜索、可复制、可 lint 的 artifact source。后续若插入一行或重排表格，`same as above` 指向会漂移。

影响：
- PR 描述或 self-audit 无法机械摘取每个 gate 的 duration source。
- review/automation 想检查“所有重型 gate 是否有 duration source”时需要解析自然语言上下文。

修复方向：
每行重复写完整 repo-relative source，或在表格前定义 `durationSource = build/verification/verify-changed/full-task-duration-summary.{json,md}`，表格中引用稳定 token `durationSource`。不要使用 `same as above`。

#### P2-5 manual whitebox 要求 `log.talent.breakpoint_chosen`，但步骤没有唯一动作证明它会出现

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:250` 把 `log.talent.learned`、`log.talent.rank_up`、`log.talent.breakpoint_chosen` 都列为 required gameplay log keys。
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:257-263` 的 manual steps 只要求学习 rank 0 技能、查看 preview、四槽满时测试 modal、确认新 UI 不改变日志产生条件。
- upstream v4 PR01 明确要求 rank up / breakpoint 路径，见 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:403-410`。

问题：
学习 rank 0 技能不一定触发 `log.talent.breakpoint_chosen`。PR04 如果只复用 upstream generated evidence 可以覆盖它，但 PR04 manual steps 没有写“由 upstream generated evidence 证明”还是“PR04 人工步骤必须再执行 rank-up/breakpoint”。这会让 manual record 填写时出现猜测。

影响：
- manual record 可能声明 required log key 覆盖，但实际截图/日志只包含 learned，不包含 rank_up 或 breakpoint。
- PR04 dark UI 可能没有验证 breakpoint preview 在新 layout 中可读。

修复方向：
在 §8.3 增加一条明确步骤：

> 对同一 talent 再投入 1 点直到 generated `expected-evidence.json` 要求的 breakpoint preview 被达成；manual record 必须引用对应 log line 或说明该 log key 已由 upstream `phase4-v4-pr01` generated evidence 覆盖，并写 `source=upstream-generated`。

同时把 manual record 字段补为：
- `logKey`
- `source` = `pr04-manual` / `phase4-v4-pr01-generated`
- `evidencePath`
- `sha256`
- `residualRisk`

#### P2-6 §5 source 和 Acceptance Matrix source 有局部章节漂移

证据：
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:19` 的 `UI04-M03-render-row-layout` source 写 `§5 node row and panel layout`。
- 当前 §5 是 “影响范围与实现顺序”，node row 与 panel layout 的需求在 `UI/pr/dark-uiux-pr04-profession-tree-ui.md:186-194` 的 §6。
- `docs/review/rule/pr-level-review-standard.md:168-176` 要求 `source` 指向 PR 文档章节或完成定义。

问题：
这不改变实现方向，但会降低矩阵可核对性。开发者或 reviewer 点击 source 时会先到文件清单，而不是 node row/panel 的实际 UI requirement。

修复方向：
把 `UI04-M03-render-row-layout` source 改为 `§6 UI 改造范围 / §5.1 TileRenderer.kt row model`，或者拆为两个 requirement：
- `UI04-M03a-render-row-model` -> §5.1
- `UI04-M03b-node-row-layout` -> §6

### P3

#### P3-1 PR04 文档已消除大多数上一轮阻塞项，但缺一张“上一轮 findings 吸收状态”小表

证据：
- `docs/review/rule/pr-level-review-standard.md:435-450` 要求增量复审区分已解决、部分解决、未解决、新引入问题、仍需冻结的实现细节。
- 当前 PR04 文档本身没有记录上一轮 review 的吸收状态；本报告已记录，但后续执行者只看 PR04 文档时看不到哪些是刻意修复过的合同。

影响：
这不是开发阻塞，但会让下一次复审需要重新从历史报告推断吸收状态。

修复方向：
可在 §10 self-audit 后补一个 `Review follow-up status` 小节，列出上一轮 P1/P2 的吸收状态；或在 PR 描述模板里固定填写，不必放入长期 PR04 合同。

## Previous Findings Status

| Previous finding | Current status | Notes |
| --- | --- | --- |
| `P1-1` state icon / tone / rank presentation contract | 部分解决 | 已新增 field table；仍需冻结 exact model name、toneToken owner、state icon fallback 语义 |
| `P1-2` active slot modal state machine | 基本解决 | 已新增 `1-4/R/Esc/Backspace/Tab` table；剩余问题转为 active slot strip surface |
| `P1-3` whitebox artifact / manual record | 部分解决 | 已新增字段和 skip rule；仍缺 `dark-uiux-pr04-*` label materialization |
| `P1-4` race tree 被“三棵职业树”误导 | 已解决 | 文档明确 runtime 不过滤 `ownerType=RACE` |
| `P1-5` PR dependency / PR06 rebaseline | 基本解决 | 已新增 cross-PR table；multi-owner 分派仍需拆行 |
| previous P2 impact scope / removal plan / gate budget | 部分解决 | Added/Modified/Deleted、Removal Plan 已补；还剩 duration source、source anchor、test anchor 精确度问题 |

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR04 必须保留 `TalentSidebarPresenter` presentation authority，renderer 不重建规则状态 | `UI/pr/dark-uiux-pr04-profession-tree-ui.md:66-68`、`UI/pr/dark-uiux-pr04-profession-tree-ui.md:88-98`；当前 renderer 仍按 `role` 映射 tone，见 `TileRenderModel.kt:1706-1727` | 部分一致：方向正确，但 exact public model / tone authority 未冻结 |
| PR04 不改职业树规则，不碰 `core/game` rule | `UI/pr/dark-uiux-pr04-profession-tree-ui.md:151-155`、`UI/pr/dark-uiux-pr04-profession-tree-ui.md:292-304` | 一致：文档边界清晰 |
| active slot modal 必须冻结 `1-4/R/Esc` 命令语义 | `UI/pr/dark-uiux-pr04-profession-tree-ui.md:121-130`；当前真实命令见 `InputHandler.kt:1339-1351` | 一致：本轮已足够指导命令测试 |
| PR04 dark UI evidence 必须有 golden/manual label | `UI/pr/screen-coverage-matrix.md:72-74`、`UI/pr/dark-uiux-pr04-profession-tree-ui.md:249-263`；当前 whitebox materialization 生成 upstream label，见 `Phase4V4WhiteboxScenarioCliTest.kt:85-112` | 部分一致：label 要求清楚，但产出路径未冻结 |
| race tree 不得被 client 过滤 | `UI/pr/dark-uiux-pr04-profession-tree-ui.md:108-113`；runtime snapshot 可包含 profession + race | 一致：文档已修正上一轮问题 |
| Acceptance Matrix 必须可机械分派 owner / gate / artifact | `UI/pr/dark-uiux-pr04-profession-tree-ui.md:15-27`、`UI/pr/development-governance.md:22-30` | 部分一致：字段齐全，但 multi-owner 和 test anchor 精确度仍不足 |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前代码/文档状态 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |
| `client.ui.talent` | presenter 输出完整状态表现字段 | 当前代码仍是 `text/role/iconKey/selected`；PR04 文档新增语义但允许命名微调 | exact model 未冻结，tone owner 分叉 | P1 |
| `client.render` | renderer 只消费 presentation fields，不能根据 role/text 推导 state | 当前 `TileRenderModel` 仍按 role 映射 tone；这是 PR04 将要改的面 | 文档没有给 renderer exact row model / nullability | P1 |
| `client.input` / `ModalStack` | selection identity 由 `talentId+treeId+ownerType+treeOwnerId` 决定 | 当前状态仍以 flat index 存储 | 文档未冻结 exact identity state type / fields | P1 |
| resource / manifest | `ui.state.*` 必须来自 PR02 exact key | PR04 文档同时写 stop condition 与 renderer fallback | fallback failure semantics 冲突 | P1 |
| whitebox / manual evidence | PR04 必须有 `dark-uiux-pr04-*` label | 当前 `phase4-v4-pr01` materialization 只证明 upstream label | 缺 PR04 label materialization / mapping 规则 | P1 |
| active slot strip | filled / empty / pending / reserve hint 可见 | 文档未指定 strip placement 和 row model | 实现位置不唯一 | P2 |
| Acceptance Matrix | owner/gate/artifact 可追踪 | 骨架通过 `acceptanceContractLint` | multi-owner/test name/source anchor 仍需收紧 | P2 |

## 玩法与体验审查

PR04 是 player-facing UI PR。当前文档已覆盖职业树主路径：玩家能看到四态、点数、preview、lock reason、active slot modal，并且不改变职业树规则。剩余体验风险集中在两个地方：

1. 四态 icon fallback 如果被当成合法渲染，玩家会失去 locked / learnable / reserve / active 的视觉区分，只剩 tone 或文本。
2. active slot strip 未冻结 placement，玩家在“四槽满 -> 新 active talent -> replace/reserve/cancel”路径上可能看不到自己正在替换哪个槽。

这两个问题不能简单推到 PR06/PR07，因为 PR04 的核心价值就是让职业树和 active slot modal 变成可读、可验证的暗黑 UI surface。PR06 可以替换 skill/tree icon，但不能替 PR04 定义 state icon fallback、slot replacement UI 和 PR04 dark label。

## 当前阶段必须解决的问题

合并 PR04 文档前必须处理：

1. 冻结 `TalentSidebarLine` exact fields、tone enum/source、node row nullability 和 renderer 消费规则。
2. 统一 `ui.state.*` failure semantics：exact resolve 缺失必须 fail owner gate；runtime fallback 只能防崩溃，不能作为通过证据。
3. 明确 `dark-uiux-pr04-*` label 是如何从 `phase4-v4-pr01` upstream scenario 产出或映射到 PR04 manual record。
4. 冻结 selection identity 的 exact client state type / owner / fallback，避免 flat index 继续成为 authority。
5. 定义 active slot strip placement 和 model，保证 `dark-uiux-pr04-active-slot-choice` 能验收 filled/empty/pending/reserve hint。

可作为文档质量但不阻塞架构方向的问题：
- multi-owner 行拆分。
- test anchor 标注 existing / new / rename。
- Gate Budget 去掉 `same as above`。
- source anchor 从 §5 修正到 §6。

## Removal/Iteration Plan

当前 Removal Plan 的大方向正确：PR04 禁止正式 ASCII glyph authority，PR06 负责 icon rebaseline，generated whitebox output 不提交。

需要补的细节：

| Surface | Current plan | Missing detail | Required fix |
| --- | --- | --- | --- |
| ASCII glyph `[x] [+] [r] [*]` | PR04 删除正式 UI authority | 没有说明 debug-only trace 的存放位置、是否进入 test/golden/manual | 写明 debug-only trace 不进入 renderer text/golden/manual；如保留，只能在 test/debug log 中出现 |
| `ui.state.*` fallback | line 93 允许 resolver fallback | 和 stop condition 冲突 | 改成 exact key missing = blocking failure |
| PR04 golden before PR06 | PR04 initial, PR06 rebaseline | PR04 label materialization 不明确 | 先定义 PR04 label 的产出/映射，再交给 PR06 rebaseline |
| selection flat index | 文档说 flat index 仅 cache | 未列删除/降级计划 | 写入 §9：flat index 不再是 identity authority；PR04 必须以 selection identity 测试防回归 |

## Additional Suggestions

1. 在 §2.2 旁边加一个短小 `Nullability / identity table`，列出每个 presentation field 是否进入 row identity、golden identity、selection identity。
2. 在 §8.3 manual record 字段中增加 `locale` 和 `viewport`，因为 PR04 涉及中文文本截断和 `1024x720` 最小窗口证据。
3. 在 `UI/pr/screen-coverage-matrix.md` 中把 PR04 的 `active slot strip` 明确归属到“职业树侧栏”或“主动槽选择 modal”，避免 PR07 final index 重复或漏掉。
4. 若继续使用 class-level `--tests` 命令，建议在文档里把 method-level fastCheck 与 Gradle command 分开：矩阵写 method anchor，Gate ladder 写 class-level command。

## Open Questions

1. PR04 是否愿意新增 client-only `TalentTreeSelectionIdentity` public/internal model？如果不新增，文档必须解释如何在现有 flat index 状态下满足 identity changed / unchanged。
2. `dark-uiux-pr04-*` label 是由 `goldenScreenshot` 直接产出，还是由 packaged whitebox/manual capture 产出？当前文档两者都写了，但没有唯一 materialization path。
3. active slot strip 是 talent panel 内的新 surface，还是复用底部 hotbar并增加 pending marker？这会影响 `TileRenderModel` / `TileRenderer` 的具体改动范围。

## Suggested Verification

文档修完后建议按以下顺序重新跑：

```bash
git diff --check -- UI/pr/dark-uiux-pr04-profession-tree-ui.md UI/review/2026-05-09-dark-uiux-pr04-pr-level-standard-rereview-round2.md
for f in UI/pr/dark-uiux-pr04-profession-tree-ui.md UI/review/2026-05-09-dark-uiux-pr04-pr-level-standard-rereview-round2.md; do awk 'BEGIN{c=0} /^```/{c++} END{print FILENAME ":FENCE_OPEN=" c%2}' "$f"; done
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n --type md "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr04-profession-tree-ui.md UI/review/2026-05-09-dark-uiux-pr04-pr-level-standard-rereview-round2.md
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

PR04 实现时建议补跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.screen.ValidationScenarioBootstrapTest
./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest
./gradlew manifestLint contractLint localeLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint
./gradlew verifyChanged
```

## Executed Validation

已运行：

- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint`：通过，`BUILD SUCCESSFUL in 1s`，`21 actionable tasks: 1 executed, 20 up-to-date`。该任务触发 `syncPhase2Manifests`，未发现 manifest 文件新增 diff。
- `git diff --check -- UI/review/2026-05-09-dark-uiux-pr04-pr-level-standard-rereview-round2.md`：通过，无 whitespace/error 输出。
- fence count check：通过，`balanced fences: 4`。
- absolute path scan for `UI/review/2026-05-09-dark-uiux-pr04-pr-level-standard-rereview-round2.md`：通过，无本机绝对路径命中。

## Summary

PR04 文档已经从“方向正确但不可实施”提升到“主体可实施但仍需冻结关键合同”。当前剩余问题不是大方向，而是会直接影响实现者落代码的精确细节：exact presentation model、state icon failure semantics、PR04 label materialization、selection identity state owner、active slot strip placement。把这些补齐后，PR04 才能真正做到按文档开发、不靠实现者猜测。

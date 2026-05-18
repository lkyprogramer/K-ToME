# Dark UI/UX PR-04 Profession Tree UI — Deep Review Round 3

**评审对象**: `UI/pr/dark-uiux-pr04-profession-tree-ui.md`（859 行版本，含 §10 "Claude Round 2 Review Disposition" 表）
**对照**: `UI/review/2026-05-16-dark-uiux-pr04-deep-review-round2.md` 中的 P0/P1/P2/P3 项
**评审视角**: 资深 Roguelike / ToME 系统策划总监 + 客户端 presenter/renderer 落地负责人
**评审目的**: 在文档进入实现前，把所有“两个开发者会写出不同代码”的实现层面 binary 决策收敛到 typed contract。

---

## TL;DR

- **Round 2 大方向反馈已基本吸收**：grid model 删除、list tree 化、`focused: Boolean`、`TALENT_PENDING` 拆分到 boolean、modal items=5 + cancel footer、edge 三态、preview block 固定 ordering、camelCase 注脚、ownerType typed enum、acceptance matrix subRuleId、tools 路径区分——全部落到 §10 disposition 表里。**方向上没有再需要重做的事**。
- **新发现集中在"row/section/header 字段语义"和"acceptance matrix 与 §2.x/§6.x 测试列表的 drift"**：仍有几个 P0 binary 决策会让两个开发者写出结构不同的 presenter 输出；并有约 5 条 NEW test 仅在 §2.x / §6.x 散文中提到，但 `§0 Acceptance Matrix` 没有对应 requirement / fastCheck 锚点。
- **预计 1–2 小时即可清完**。建议在 acceptance contract lint 真正落地前修完 §A，否则 lint 落地后改这些字段会反过来推翻 lint 规则。

---

## A. P0 · 仍可能让 renderer 自由发挥的契约空白（必改）

### A-1 `SECTION_HEADER` row 字段语义未定义；与 `TalentAssignSectionModel.displayName / nodeCountText` 关系矛盾

`TalentAssignTreeRowModel` 字段含 `talentId? / stateMarkerText? / stateIconKey? / skillIconKey? / rankText? / toneToken? / pendingOverlay / focused` 全 nullable，可表达 SECTION_HEADER。但 `TalentAssignSectionModel` 也已经声明 `displayName: String` 和 `nodeCountText: String`。

文档同时支持两种设计，没指定走哪条：

| 设计 A | 设计 B |
| --- | --- |
| `sections[*].rows` 不含 SECTION_HEADER；renderer 把 `SectionModel.displayName + nodeCountText` 作为 section header 自动绘制；`TalentAssignRowKind` 中 `SECTION_HEADER` 实际上**永远不出现** | `sections[*].rows[0].kind = SECTION_HEADER`，`displayName="武器系 6/6"` 由 presenter 拼好；`SectionModel.displayName / nodeCountText` 沦为缓存或调试字段 |

后果：实现者 A 写完成 (A)，实现者 B 写成 (B)，**presenter 单元测试会跑出完全不同的 row count、indentation 结构、focused row index**，但两者都通过 §6.2 视觉对照。

**修复建议**（推荐 (A)，因为它把 section meta 与 talent rows 分层更清晰）：

1. §2.2 model 注释明确：`TalentAssignRowKind` 只剩 `TALENT_NODE` 一种，删除 `SECTION_HEADER` enum 项。`sections[*].rows` 全部是 talent node rows。
2. §6.2 renderer rule 明确：section header 行由 renderer 用 `SectionModel.displayName + " " + SectionModel.nodeCountText` 渲染，**不进入 row 列表**，因此 selection identity / focus / hit region 也不针对 section header。
3. focused test 补一行：`NEW: TalentSidebarPresenterTest.pr04SectionsRowsContainOnlyTalentNodes`。
4. §10 self-audit 补一行：`section header source: section meta only; rows list contains only TALENT_NODE`。

如果坚持 (B)，必须在 §2.2 写明：`SECTION_HEADER` 行的 `displayName` 必须等于 `SectionModel.displayName + nodeCountText` 拼接结果；`SectionModel.displayName/nodeCountText` 在 (B) 模型下应改成 `derivedDisplayName / derivedNodeCountText` 并明文标记为缓存。两者不能并存留歧义。

### A-2 `SLOT_REPLACE_TARGET` 是仅 modal focused 那一项，还是所有 4 个 filled slot

- §3.2.5：`four-slot-full 且 pending node category in ACTIVE/SUSTAINED 时打开 active slot choice；modal strip 中 1-4 slots 使用 PENDING_REPLACE_TARGET 表示可替换目标`（暗示所有 4 个 slot 都是 replacement target）
- §3.3 Rules.3：`SLOT_REPLACE_TARGET is the focused replacement candidate and must use ember-gold edge chrome`（暗示仅 focused 那一项）

两条规则方向相反。后果：

- 实现 A：modal 4 个 slot row 全部 `kind=SLOT_REPLACE_TARGET`，`focused=true` 的那一行额外画 ember-gold；
- 实现 B：modal 4 行中只有 `focused=true` 的那一行 `kind=SLOT_REPLACE_TARGET`，其余 3 行 `kind=SLOT_FILLED`。

两种写法 `pr04ActiveSlotChoiceModalHasFiveItemsAndCancelIsFooter` 都通过，但 modal 视觉 chrome 完全不同。

**修复建议**（推荐让 `kind` 表达"是否处于替换流程"，`focused` 表达"当前键盘焦点"）：

1. §3.3 Rules.3 改为：`Modal 打开时，所有原本 SLOT_FILLED 的 1-4 slot row kind 升级为 SLOT_REPLACE_TARGET 表示可替换；focused 表示当前键盘焦点。键盘焦点单独绘制 cyan ring，与 replacement chrome 视觉分离。`
2. §3.2 strip `TalentActiveSlotStripState.PENDING_REPLACE_TARGET` 与 modal `SLOT_REPLACE_TARGET` 是同一概念两个名字。建议复用 modal 的 typed kind，避免 strip 与 modal 各维护一套 enum。
3. focused test 补一行：`NEW: TalentSidebarPresenterTest.pr04ActiveSlotChoiceModalMarksAllFourSlotsAsReplaceTargetsAndFocusedOnFirst`。

### A-3 `stateMarkerText` 与 `stateIconKey` 同时存在时 renderer 画哪个

§2.2 Authority rules：

- rule 4: `stateMarkerText is presenter-owned and must be one of localized equivalents of [x], [+], [*], [r]`
- rule 5: `stateIconKey is optional ... If implementation also draws exact PR02 ui.state.* icons, exact resolve failure is blocking evidence failure`

两个都可以存在。当 `stateMarkerText="[+]"` 且 `stateIconKey="ui.state.learnable"` 都非空时，renderer 画哪个？两个并排？只画 icon？只画 marker？

后果：两个开发者实现完全不同的 row chrome。golden screenshot 不可复用。

**修复建议**：明文规定二选一，且由 presenter 控制：

> Presenter 必须在 `stateMarkerText` 和 `stateIconKey` 中**只填一个**。如果实现选择走 icon-only 路径，所有 row 的 `stateMarkerText` 必须为 `null`；如果走 marker-only，所有 row 的 `stateIconKey` 必须为 `null`。两者**不允许同时非空**。renderer 看到非空的那个字段就绘制对应表达，看到 null 字段就略过。

补 focused test：`NEW: TalentSidebarPresenterTest.pr04StateMarkerAndStateIconKeyAreMutuallyExclusivePerRow`。

并在 §10 self-audit 加一行：`state marker mode: marker-text-only / icon-only`。

### A-4 §2.3 / §2.4 / §6.6 列出的 NEW test 没有出现在 §0 Acceptance Matrix

`§0 Acceptance Matrix` 是 owner/gate/artifact 的契约真源。但下列 NEW test 在 §2.x / §6.x 散文里被标为 "Required tests"，**却在 §0 表里找不到对应 requirement/fastCheck**：

| 散文位置 | NEW test 名 | §0 Matrix 锚点 |
| --- | --- | --- |
| §2.3 末尾 | `TalentSidebarPresenterTest.pr04EdgeStateReflectsBothEndpointStates` | ❌ 缺失 |
| §2.4 末尾 | `InputHandlerTest.talentTreeSelectionRestoresByIdentityWhenSnapshotOrderChanges` | ❌ 缺失（无 selection identity 行） |
| §2.4 末尾 | `TalentSidebarPresenterTest.selectedNodeUsesFullTreeOwnerIdentity` | ❌ 缺失 |
| §2.4 末尾 | `InputHandlerTest.talentTreeSelectionFallsBackToFirstVisibleNodeWhenIdentityDisappears` | ❌ 缺失 |
| §6.6 末尾 | `TalentSidebarPresenterTest.pr04LegendHasFourStateToneItemsPlusFocusAndPendingEntries` | ❌ 缺失（UI04-M02 / M03 / M04 都没列） |

后果：

1. acceptanceContractLint 无法 enforce 这些测试存在。
2. owner failure rule 无法定位（gate 失败时 owner 不知道指向哪一条 requirement）。
3. PR04 closure 时 self-audit 不知道是否要勾这几行。

**修复建议**：

- 新增 `UI04-M03b-edge-state-projection`（owner=`client`，fastCheck=`NEW: TalentSidebarPresenterTest.pr04EdgeStateReflectsBothEndpointStates`，source=§2.3）。
- 新增 `UI04-M04b-selection-identity`（owner=`client`，fastCheck= 4 个 selection identity test，source=§2.4）。
- 在 UI04-M04 或新加 `UI04-M04c-legend-composition` 中收纳 `pr04LegendHasFourStateToneItemsPlusFocusAndPendingEntries`，source=§6.6。
- 或者反向操作：将 §2.x / §6.x 末尾的"Required tests"块 inline 加上"对应 acceptance requirement: UI04-MXX"的锚点指针，让 grep `UI04-M` 即可找全。

### A-5 `CURRENT_RANK_DETAIL` 的 `primaryText / secondaryText / bodyLines` 字段映射没规范

```kotlin
data class TalentDetailBlock(
    val kind: TalentDetailBlockKind,
    val iconKey: String?,
    val primaryText: String,
    val secondaryText: String?,
    val bodyLines: List<TalentPreviewLine>,
    val toneToken: TalentPreviewToneToken,
)
```

§6.4.4：`Required fields when data exists: type/category, range or target, area/radius, resource cost, cooldown, primary coefficient/value, special status/knockback/teleport/slot note`

但这些字段进 `primaryText`、`secondaryText` 还是 `bodyLines[*]`？

- 实现 A：所有都进 `bodyLines`，`primaryText="当前等级详情（当前 2 级）"`
- 实现 B：`primaryText="范围 5格"`, `secondaryText="冷却 6 回合"`, `bodyLines=[damage line, status line]`
- 实现 C：把整个详情拼成一个 `primaryText` 多行字符串，`bodyLines=[]`

参考图的视觉是"每条数据一行 + label + value"，更接近 (A) + (B) 的混合。

**修复建议**：把每个 block 的字段映射写成 table。建议：

| Block kind | `iconKey` | `primaryText` | `secondaryText` | `bodyLines` |
| --- | --- | --- | --- | --- |
| `HERO_ICON` | hero icon key | `null/""` | `null` | empty |
| `HEADER` | `null` | talent display name | localized state chip (`可学习` / `已学习` / `已激活` / `已预留` / `已锁定`) | empty |
| `RANK_AND_COST` | resource icon `null` | rank transition (`等级 1 → 等级 2`) | point cost (`消耗 1 职业天赋点`) | empty |
| `PREREQUISITE` | `null` | `需学习前置` localized header | `null` | one line per prerequisite: `label=前置技能名, value=等级要求` |
| `PREREQUISITE_FAILED` | `null` | `未满足前置条件` localized | `null` | optional: missing-rank breakdown lines |
| `CURRENT_RANK_DETAIL` | `null` | section title (`当前等级详情（当前 2 级）` 或 `当前等级详情（预览 1 级）`) | `null` | each required field as a line: `range`, `cooldown`, `damage`, `status` 等 |
| `NEXT_RANK_PREVIEW` | `null` | section title (`下一等级预览`) | `null` | delta lines only |
| `ACTIONS` | `null` | `null/""` | `null` | one line per action: `Enter` `R` `Esc` |

`TalentPreviewLine` 的 `label / value / iconKey` 用法补 example。

补 focused test：`NEW: TalentSidebarPresenterTest.pr04DetailBlockPrimaryAndBodyFieldsMatchKindTable`。

---

## B. P1 · 字段语义 / 字段来源 / 命名 ambiguity（强烈建议改）

### B-1 `professionPointText / racePointText` 数据来源未声明

§2.2 `TalentAssignHeaderModel` 只暴露文本字段。但 presenter 从 `RenderUiStateSnapshot` 哪个字段取值？是 `talents.points`？`professionPoints`？`raceTalentPoints`？

§2.4.9 写："`raceTalentPoints` 只用于 header / points display" —— 暗示存在 `raceTalentPoints` 字段，但没指出 source path。

**修复建议**：在 §2.2 表追加 source 映射注：

> `professionPointText` 来源 `RenderUiStateSnapshot.talents.points` 或等价 typed 字段（owner=upstream snapshot）；presenter 不在 client 重算。`racePointText` 当 snapshot 含 `ownerType=RACE` tree 时从 `RenderUiStateSnapshot.raceTalentPoints` 取，且仅当 race tree 存在时非 null。

补 focused test：`NEW: TalentSidebarPresenterTest.pr04HeaderPointTextsComeFromSnapshotNotPresenterRecompute`。

### B-2 `nodeCountText` 的 "X/Y" 含义未定义

§6.2 example：`武器系 6/6`、`护卫系 5/5`、`战吼系 5/5`。

但 "6/6" 是什么？
- learned / total（已学/共）？
- visible / total（可见/共）？
- invested points / max（投入点/可投点）？

如果 player 还没学任何 talent，是 `0/6` 还是 `6/6`？

**修复建议**：明确语义。推荐：

> `nodeCountText` 格式为 `${learnedOrAllocatedNodeCount}/${totalNodeCount}`，分子是该 tree 内 `state ∈ {LEARNED_RESERVE, LEARNED_ACTIVE}` 的节点数，分母是该 tree 总节点数。pending allocation 不计入分子，避免与 row 的 `pendingOverlay` 重复表达。

补 focused test：`NEW: TalentSidebarPresenterTest.pr04SectionNodeCountTextUsesLearnedOverTotal`。

### B-3 `footerHelpText` 与 `ACTIONS` block 重复且渲染位置不明

§2.2: `val footerHelpText: String,` —— panel 底部 footer 左侧文字。
§6.1 footer 行: `左侧 keyboard help，右侧 legend`。
§6.4 ACTIONS block: `Enter 学习, R 预留, Esc 返回`。

两者内容高度重叠："Enter 学习 / R 预留 / Esc 返回" 既是 detail pane 底部 actions，也是 panel footer keyboard help。

**修复建议**：明确分工。推荐：

- `ACTIONS` block 只显示当前 selected talent 适用的 actions（rank 满时 `Enter 学习` 改为 `已满级`）；
- `footerHelpText` 显示 panel 级 keyboard help（如 `↑↓ 选择  Tab 切换树  Esc 返回`），与 selected talent 无关；
- 两者文本必须不同义，避免视觉重复。

补 focused test：`NEW: TalentSidebarPresenterTest.pr04FooterHelpTextIsPanelLevelAndDoesNotDuplicateActionsBlock`。

### B-4 modal trigger ownership 未明（client vs gameplay）

§3.1 表的状态机以 `ACTIVE_TALENT_SLOT_CHOICE` overlay state 为前提，但谁让 client 进入这个 state？

- 路径 A：client `InputHandler` 收到 `Enter` 并发现 `category in ACTIVE/SUSTAINED` + four-slot-full 时主动切到 `ACTIVE_TALENT_SLOT_CHOICE`；
- 路径 B：client 发 `PlayerCommand.RequestLearnTalent`，upstream snapshot 返回 `pendingSlotChoice=true`，client 据此打开 modal。

PR04 是 UI PR，按 §11 "不改职业树规则"应只消费 modal state，不新增 trigger logic。但 §3.2.5 描述暗示 client 端判断逻辑。

**修复建议**：明文规定路径 B：

> client 不在 `InputHandler` 中判断"四槽满+ACTIVE/SUSTAINED"。`ACTIVE_TALENT_SLOT_CHOICE` 由 upstream snapshot / overlay state 触发；client 收到 `OverlayMode.ACTIVE_TALENT_SLOT_CHOICE` 后才渲染 modal。strip `PENDING_REPLACE_TARGET` 状态由 presenter 从 typed snapshot 字段（如 `pendingDraft.requiresSlotChoice`）派生。

补 focused test：`NEW: TalentSidebarPresenterTest.pr04ActiveSlotChoiceModalOnlyOpensWhenSnapshotSignalsSlotChoiceRequired`。

### B-5 `modal first-focused item` 算法未定义

modal 打开时 `items[0..4]` 哪一项 `focused=true`？slot 1？最后一个 filled？reserve？implement 决定？

**修复建议**：明确初始 focus 是 slot 1：

> modal 打开时 `items[0].focused=true`（slot 1）；player 通过 `↑↓` 或 `1-4/R` 切换 focus。后续逻辑由 input handler 维护。

补 focused test：`NEW: InputHandlerTest.activeSlotChoiceModalOpensWithSlotOneFocused`。

### B-6 `OverlayState.talentTreeSelectionIdentity` 与 `ModalFrameLocalState.talentTreeSelectionIdentity` 之间优先级

§2.4.5 列出两个 owner 都持 `talentTreeSelectionIdentity`，但当二者不一致时（modal 关闭后 modal local 缓存的 identity vs overlay 全局 identity）哪个赢？

**修复建议**：

> ModalFrameLocalState.talentTreeSelectionIdentity 是 modal 生命周期内的快照；OverlayState.talentTreeSelectionIdentity 是 modal 关闭后由 modal 写回的最新 identity。modal 关闭时 ModalFrameLocalState → OverlayState；snapshot 更新时按 OverlayState.identity 恢复 selection。client 不能同时读两个 owner 做 merge。

补 focused test：`NEW: InputHandlerTest.modalCloseFlushesTalentSelectionIdentityToOverlayState`。

### B-7 `CURRENT_RANK_DETAIL` 数据 source priority 列表是 first-match 还是 cumulative

§6.4.5：`Source priority: DescriptionPresenter.presentTalentTreeNodeLines / descriptionModel / resourceCost / resourceLabelKey / range / minRange / currentCooldown / maxCooldown / typed schema projection`

实现者 A 会读成 "first non-null wins"，B 会读成 "全部累加 bodyLines"。

**修复建议**：

> presenter 优先调用 `DescriptionPresenter.presentTalentTreeNodeLines`；如果该方法返回完整 detail lines，则不再读 `resourceCost / range / cooldown` 等原始字段。仅当 `presentTalentTreeNodeLines` 返回空或不可用时，presenter 才从原始字段 fall back 拼 bodyLines。两条路径不同时使用。

补 focused test：`NEW: TalentSidebarPresenterTest.pr04CurrentRankDetailPrefersDescriptionPresenterOverRawSchemaFields`。

### B-8 连线 (connector) deferred 路径与 `UI04-M03` 测试名 unconditional

`§0 UI04-M03 fastCheck: NEW: TalentSidebarPresenterTest.pr04ConnectorPrefixComesFromTypedPrerequisites`。

§4.7 / §9 / §10 都允许 "missing typed prerequisite projection → connector deferred"。但 acceptance matrix 没把 connector test 也做 conditional（如 mouse 那条）。

**修复建议**：在 `UI04-M03` fastCheck 列加 conditional：

> `NEW: TalentSidebarPresenterTest.pr04ConnectorPrefixComesFromTypedPrerequisites` if typed prerequisite source available; else `NEW: TalentSidebarPresenterTest.pr04RowsHaveEmptyConnectorPrefixWhenPrerequisiteSourceDeferred`.

并在 §6.5 mouse=deferred 同款 self-audit 增一行 `prerequisite connector mode: implemented / deferred`。

### B-9 `nodeCountText` 文本与 `SectionModel.displayName` 拼接关系

承 A-1，如选 (A) 方案：renderer 把 `SectionModel.displayName + " " + nodeCountText` 渲染为 section header。但 `nodeCountText` 中含 `/`，可能被 locale 视为非 RTL 数字。是 string concat 还是用 `${displayName} ${nodeCountText}` 模板？renderer 是否需要在 nodeCountText 前补冒号或空格？

**修复建议**：在 §6.2 加一行渲染规则：

> Section header 行渲染格式为 `${displayName}  ${nodeCountText}`（两空格分隔），nodeCountText 使用 ember-gold tone，displayName 使用 cyan tone。renderer 不允许改格式。

### B-10 acceptanceContractLint subrule 分隔符不统一（`.` vs `#`）

`acceptanceContractLint.pr04ReferenceImageFidelity` 用 `.`；`acceptanceContractLint.pr04ManualLabelMapping#labelMapping` 用 `#`。

**修复建议**：统一用 `#` 表达 subRuleId，`.` 仅表达 task→rule namespace：

- `acceptanceContractLint.pr04ManualLabelMapping#labelMapping`
- `acceptanceContractLint.pr04ManualLabelMapping#minWindow`
- `acceptanceContractLint.pr04ManualLabelMapping#rightCompanion`
- `acceptanceContractLint.pr04ReferenceImageFidelity`（无 subRuleId 即整个规则）

或反向：subRuleId 也用 `.`，写成 `acceptanceContractLint.pr04ManualLabelMapping.labelMapping`。任选一个，全文一致。

---

## C. P2 · acceptance matrix / 命名一致性问题

### C-1 §5.1 表的 test 名 PascalCase 与 §0 矩阵 camelCase 不一致

- §0 UI04-M02 fastCheck: `NEW: TalentSidebarPresenterTest.talentAssignPanelModelDoesNotMixNodeAndNonNodeStateFields`（首字母小写）
- §5.1 表最后一行 test/evidence: `TalentAssignPanelModelDoesNotMixNodeAndNonNodeStateFields`（PascalCase）

§0 末尾刚说"NEW 测试统一 camelCase"。§5.1 表里这个 test 应改为 `talentAssignPanelModelDoesNotMixNodeAndNonNodeStateFields`。

### C-2 `UI04-M09` fastCheck `RENAME existing ManifestResolveTest ... to ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` 跨 PR 边界

这条改名属于 PR02 owner 范围（不是 PR04 的 frame key），不应放在 PR04 acceptance matrix 中。即便要带，也应该让 PR04 仅声明"已上游 rename 完成"作为依赖，不在本 PR 内改测试名。

**修复建议**：把 RENAME 条目移到 §7 Cross-PR Dependency 表，作为 PR04 hard upstream。`UI04-M09` 内只保留 `NEW: TileRendererCanvasTest.darkUiuxPr04SkillIconFallbackIsVisibleButNotPassingEvidence`。

### C-3 `darkUiuxPr04SkillIconFallbackIsVisibleButNotPassingEvidence` 测试名语义自相矛盾

测试名暗示"fallback 可见但不算 passing evidence"。但单元测试只有 pass/fail 两态，不能既 visible 又 not passing。

**修复建议**：把语义拆为两条：

- `NEW: TileRendererCanvasTest.darkUiuxPr04SkillIconFallbackRendersWithoutCrashing` —— renderer pass；
- `NEW: acceptanceContractLint.pr04SkillIconFallbackRecordedInManualEvidence` —— 当 manual record 含 fallback talent 时必须 cite PR06 owner 才算 passing。

renderer test 与 evidence governance 应该不互相替代。

### C-4 `UI04-M15` fastCheck 用 inline conditional 文本，lint 无法机器解析

```
NEW: InputHandlerTest.talentTreeMouseClickSelectsNodeWhenHitRegionExists if mouse implemented; else NEW: InputHandlerTest.talentTreeKeyboardOnlyWhenMouseDeferred
```

acceptanceContractLint 是结构化 lint，inline 条件无法解析。需要让 conditional 落到 typed 字段。

**修复建议**：在 PR04 self-audit / PR description 中加一个 `mousePointerMode: implemented | deferred` 字段；lint 根据该字段二选一 expected test。或将该 fastCheck 拆为两条 sub-requirement（M15a-mouse-implemented / M15b-mouse-deferred），用 `appliesWhen` 列声明前置条件。

### C-5 EXISTING test 中含空格的方法名 backtick 不一致

- `EXISTING: TalentSchemaTest.profession tier two and tier three nodes declare specified prerequisite ranks`（无 backtick）
- `EXISTING: InputHandlerTest.\`talent assign respec follows focused tree owner\``（有 backtick）

Kotlin 含空格的 method name 必须 backtick 化才能调用。文档作为 spec 应统一带 backtick：

**修复建议**：所有 EXISTING test 中含空格的方法名一律加 backtick。修订上述 `TalentSchemaTest` 行为：

> `EXISTING: TalentSchemaTest.\`profession tier two and tier three nodes declare specified prerequisite ranks\``

### C-6 §6.6 测试 `pr04LegendHasFourStateToneItemsPlusFocusAndPendingEntries` 与 "conditional pending" 不一致

§6.6 legend item 表：`pending chrome if present | PENDING_OVERLAY | pendingOverlay=true`

`if present` 暗示 conditional。但测试名"Plus Focus And Pending Entries"暗示 pending entry 总是存在。

**修复建议**：

- 测试改名：`pr04LegendIncludesFourStateToneAndFocusAndIncludesPendingOnlyWhenAnyRowIsPending`
- 或 §6.6 改规则：legend 始终包含 PENDING_OVERLAY item（即便当前没有 pending row），保持 legend 长度恒定（6 项）。

### C-7 `nodeCountText` 6/6 与 `rankText` X/5 视觉相似但语义不同

`nodeCountText` 6/6 vs `rankText` 1/5：玩家会把两套 X/Y 读成同一回事（learned / max）。

**修复建议**：考虑给 `nodeCountText` 加图标或前缀（`✦ 6/6` 或 `共 6/6`）以视觉区分，避免 user 把 section count 误读成 rank count。这是 UX/i18n 级问题，不一定本 PR 修，但 §6.2 应至少留 note。

### C-8 `RANK_AND_COST` block 与 row `rankText` 两套 presenter 拼接逻辑

cell `rankText="2/5"` vs `RANK_AND_COST` block `primaryText="等级 2 → 等级 3"`。两者来源同一 talent 但用 presenter 内两套 format 逻辑。

**修复建议**：抽取共享 helper `TalentRankFormatter`：

```kotlin
object TalentRankFormatter {
    fun rowRankText(current: Int, max: Int, preview: Int?): String =
        if (preview != null && preview != current) "$current→$preview/$max" else "$current/$max"
    fun rankTransitionText(current: Int, preview: Int, localeArrow: String = "→"): String =
        "等级 $current $localeArrow 等级 $preview"
}
```

并在 §2.2 / §6.4 文档中注释引用同一 helper，避免两位实现者拼出不同 separator。

### C-9 `localeLint` 在 `UI04-M07 ownerGate` 列表中的 task 路径

`UI04-M07 ownerGate: localeLint, contractLint, :client:clientSmoke`

`localeLint` 与 `contractLint` 都不带冒号前缀，是 root task；`:client:clientSmoke` 带前缀。是顺序执行还是 OR？

**修复建议**：在 §0 footnote 加一行：

> ownerGate 列以逗号分隔时表示"全部必跑"（AND），不表示 OR。task 名带冒号表示 module task，不带表示 root task。

---

## D. P3 · 文档可读性收尾

### D-1 `Gate Budget` 表 Duration source 列字面值为 placeholder

每行 "Duration source" 列都写 `durationSource or N/A before first verifyChanged` —— 这是 placeholder 文本而非实际填写值。

**修复建议**：在表前加一句：

> 表中 Duration source 列写"`durationSource`"是指代 §0 顶部声明的 path `build/verification/verify-changed/full-task-duration-summary.{json,md}`；PR description 引用本 PR 时无需 expand。

### D-2 §1 "三棵职业树最小必验面" vs runtime 4+ tree 渲染

§1 末尾："'三棵职业树' 只定义 PR04 manual/golden 的最小必验面" + "runtime 渲染不得过滤"。但 `dark-uiux-pr04-talent-assign-panel-start` 必须显示几棵树？3 棵 profession 即可？还是 if-race-tree-exists 必须显示 4？

**修复建议**：在 §8.2 PR04 label 表内 `dark-uiux-pr04-talent-assign-panel-start` 行 `Required proof` 列加 note：

> 至少显示 3 棵 profession tree；如果 capture run 含 race tree，必须额外可见。

### D-3 §10 self-audit 表"prerequisite source" 行加入 deferred path 后，§9 表"missing typed prerequisite projection" 行的 `Exit rule` 与 §10 联动不清

§10：`prerequisite source: typed prerequisite source exists, or connector drawing is explicitly deferred with §9 exit rule`
§9：`missing typed prerequisite projection ... renderer never infers connectors from lock reason text; if deferred, self-audit must cite this row and PR07/owner exit rule`

闭环已有，但 §10 表里应直接写 cite path（"§9 row: missing typed prerequisite projection"），避免实现者搜半天。

### D-4 `:client:goldenScreenshot` 在 fastCheck 列偶发出现

`UI04-M04 ownerGate: :client:goldenScreenshot` —— goldenScreenshot 是 evidence-level gate，理论上 fastCheck 列应是 NEW/EXISTING test，ownerGate 才是 task path。但部分 requirement 把 goldenScreenshot 直接放在 fastCheck（用作 evidence anchor）。

**修复建议**：审查 §0 表，把所有 `:client:goldenScreenshot` 仅出现在 `ownerGate` 列；fastCheck 列里只放 test 名。

### D-5 §2.6 `tier distribution` 行的算式不直观

`tree shape 6/5/5` + `tier distribution T1=6 / T2=6 / T3=4`：读者要心算才能验证 (6 nodes 树是 T1=2/T2=2/T3=2) + (5 nodes 树 ×2)。

**修复建议**：在 §2.6 表后加一行 example breakdown：

> 例如 release playable 单职业总计：T1 = 2(6n) + 2(5n) + 2(5n) = 6；T2 = 2(6n) + 2(5n) + 2(5n) = 6；T3 = 2(6n) + 1(5n) + 1(5n) = 4。

### D-6 §3.2 vs §3.3 中 `state=PENDING_REPLACE_TARGET` 与 `kind=SLOT_REPLACE_TARGET` 同义不同名

承 A-2 修复后，建议直接复用 modal kind enum；§3.2 表头 `TalentActiveSlotStripState` 与 §3.3 `ActiveSlotChoiceModalItemKind` 合并为 single enum，减少认知负担。

### D-7 §0 acceptance matrix 中 `UI04-M02-presenter-authority` 缺少对 `TalentTreeSelectionIdentity` 的 source

UI04-M02 source = §2.2 presentation contract，但 `TalentTreeSelectionIdentity` 在 §2.4 定义。如果新增 `UI04-M04b-selection-identity`（见 A-4），source 应明确指向 §2.4，不要混入 §2.2 description。

### D-8 `app executable hash` 行已加 platform note，但 sha256 `evidence type` 列没写

§8.2 表的 `manual record fields` 没有"sha256 evidence type"列。各 PR04 label 行 `Required proof` 列里说 `screenshot sha256` —— 是 png file sha256 还是 in-image binary hash？

**修复建议**：明确"png file sha256，用 `shasum -a 256 <path>` 或 `Get-FileHash <path> -Algorithm SHA256`"。

### D-9 §11 非目标 12 "不在 PR04 引入 grid/tier/column node graph"

OK 但 §10 self-audit 没对应 audit 条目，建议加一行 `tree visual model: list tree (no grid/tier/column)` 让 self-audit 显式 enforce。

### D-10 §10 disposition 表行 D findings 仅写"verified as cleanup-level issues; absorbed where still relevant"

具体哪些 D findings 已 absorb、哪些被废弃（如 D-2 数字不匹配在新表头下已不适用）？建议给一个具体列表，便于 PR07 final audit traceability。

---

## E. 推荐 patch 落地顺序

按风险与成本：

1. **A-1 / A-2 / A-3 / A-5** —— 直接决定 presenter 输出结构的 binary 决策，必须 implement 前修。
2. **A-4** —— acceptance matrix 与 §2.x/§6.x 散文 test 列对齐；其他 patch 完之后顺手补齐 4-5 个 requirement 行。
3. **B-1 ~ B-8** —— 字段 source / 命名 / 优先级，逐条修，能让 presenter 单测断言更具体。
4. **B-9 / B-10** —— 文本拼接与 lint subrule 命名统一。
5. **C-1 ~ C-9** —— acceptance matrix 与命名细节。
6. **D-1 ~ D-10** —— 收尾。

预计：A+B 一小时，C+D 一小时。**A 改完 PR04 可以 safely 进入 implementation phase**；B 不改 implementer 会问 review；C/D 不改不阻塞但 acceptanceContractLint 落地时需重做。

---

## F. 与 Round 2 对照

| Round 2 Finding | Round 3 状态 |
| --- | --- |
| A-1 dual focus enum | ✅ 已通过 grid model 删除 + `focused: Boolean` 解决 |
| A-2 pending 双重表达 | ✅ tone enum 4 值 + `pendingOverlay` boolean 单 source of truth |
| A-3 race tree placement | ✅ §2.4.1 明文 client 不排序 |
| A-4 `TalentSidebarLine` 命运 | ⚠️ §5.1 表 + §9 都允许 "deleted OR adapter-only"，实现者仍可二选一；建议 §5.1 表直接写 deleted（推荐）或 adapter-only（不推荐），不要并列 |
| A-5 tierIndex 方向 | ✅ grid model 删除，无 tier 维度 |
| A-6 columnIndex 推导 | ✅ 同上 |
| A-7 edge state 语义 | ✅ §2.3 表已明确 trigger / treatment |
| A-8 preview ordering | ✅ §6.4 enum 顺序固定 |
| A-9 prerequisite + failed 共存 | ✅ §6.4 表明文写 always lists + appended |
| A-10 hotkeyDigit 冗余 | ✅ strip 删 hotkeyDigit；modal 保留 hotkeyText 因为 R 不是 slot |
| B-1 ownerLabel 重叠 | ✅ 删除 |
| B-2 ownerPointText 重叠 | ✅ section 模型不再含 ownerPointText |
| B-3 ownerType 字符串 | ✅ `TalentTreeOwnerType` enum |
| B-4 selected/focused 命名 | ✅ 统一 `focused` |
| B-5 modal cancel | ✅ 5 items + cancelHintText footer |
| B-6 per-tier scroll | ✅ 改 vertical only |
| B-7 reserve hint 重复 | ✅ §3.2 rule 7 明文 |
| B-8 rankText 双份 | ⚠️ 仍存在两种拼接（cell + RANK_AND_COST），建议抽 helper（本轮 C-8） |
| B-9 block iconKey | ✅ §6.4 block rules 表 |
| B-10 FLAVOR 来源 | ✅ flavor 非 canonical，optional only |
| C-1 manual lint 双用 | ✅ subRuleId |
| C-2 game/tools owner | ✅ 拆 M00a / M00b |
| C-3 reference boundary lint | ✅ `pr04ReferenceImageFidelity` |
| C-4 golden/manual 标签 | ✅ §8.2 末尾明文 |
| C-5 sidebar naming | ✅ 全 rename 为 talent-assign-* |
| C-6 mouse=deferred 条件 test | ✅ inline conditional（但 lint 解析待修，本轮 C-4） |
| C-7 stop condition 锚定 | ✅ §4.3 cite M00a/b |
| C-8 测试命名风格 | ✅ camelCase 注脚 |
| C-9 tools task path | ✅ §8.1 注脚 |
| D-* | ✅ 多数已 absorb |

新增（仅 Round 3 发现）：

- A-1 (Round 3): SECTION_HEADER row vs SectionModel meta 二选一
- A-2 (Round 3): SLOT_REPLACE_TARGET 仅 focused 还是全部 4 槽
- A-3 (Round 3): stateMarkerText / stateIconKey 互斥规则
- A-4 (Round 3): §2.3/§2.4/§6.6 required test 未进 §0 matrix
- A-5 (Round 3): Detail block primaryText/secondaryText/bodyLines 字段映射

---

## G. 一句话给执行者

> Round 2 把 model 家族基本定型。Round 3 剩下的全是"row 字段语义 / acceptance matrix 与散文 test 列对齐 / block 字段映射"这种 binary 决策。**A-1 ~ A-5 五条必须在 implement 前 nail down**，否则两个开发者会写出 row 结构、modal kind 语义、detail block field map 完全不同的 presenter，且 §0 acceptance matrix 抓不住。B/C/D 是 1 小时内可清的精修。修完之后 PR04 可以放心进入 implement phase，acceptanceContractLint 与 presenter 单元测试都能严格 enforce contract。

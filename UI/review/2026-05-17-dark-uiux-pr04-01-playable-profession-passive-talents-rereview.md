# PR04-01 Playable Profession Passive Talents 复审报告（Round 2）

审查对象：`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`  
审查日期：2026-05-17  
审查定位：资深 Roguelike / 类 ToME 玩法系统、职业构筑、UI 体验与验证合同复审

## Findings

### [P1] 白盒合同仍不可执行：只有 evidence 路径，没有 scenario registry、固定运行上下文、精确键序列和 manual record 合同

新版文档已经把白盒证据路径列出来了，见 PR04-01 的 Canonical Artifact 与 Manual / Whitebox Expectations：`build/whitebox/dark-uiux-pr04-01-playable-profession-passive-talents/evidence/passive-detail-static.png`、`passive-detail-trigger.png`、`passive-no-active-slot-modal.png`（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:91-103`, `632-651`）。但这仍然不是当前仓库白盒体系可执行的合同。

当前工具链要求白盒场景必须通过 `-Pktome.whitebox.scenario` 指定，并且场景必须在 `ValidationScenarioRegistry` 中存在；CLI 对缺失或未知 scenario 会直接 fail fast（`tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt:32-36`）。现有 PR04 场景已经有固定 `scenarioId`、preset、seed、locale、profession、zone、required evidence files、CUA steps 和 `manualRecordPath`（`game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt:127-182`），同时 YAML parity 只列到了 `dark-uiux-pr04-profession-tree-ui`，没有 PR04-01（`tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml:1-13`）。

影响：实现者可以跑过 `clientSmoke` / `goldenScreenshot`，但无法用标准 `preparePhase4V4Whitebox` 物化 PR04-01 的白盒 runbook；“selected Task A PASSIVE row / selected Task B PASSIVE row”也过于模糊，可能只截一个最容易的静态被动，漏掉 trigger、conditional、rank delta、`R` 输入抑制等核心体验。

必须补充：

1. 新增明确场景合同，例如 `scenarioId = dark-uiux-pr04-01-playable-profession-passive-talents`。
2. 在 Task 6 的 production scope 中列出 `ValidationScenarioRegistry.kt`、`phase4-v4-scenarios.yaml` 和 `ValidationScenarioRegistryTest.kt`。
3. 固定 runtime：preset、seed、locale、profession/race、zone/floor、contentPackMode。
4. 固定 manual record path，例如 `UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md`。
5. 把三张截图拆成精确 CUA steps：具体从哪个职业、哪个树、哪个 talentId、按哪些键进入、按哪些键学习/升阶、按 `R` 后应看到什么。
6. 如果一个 scenario 无法同时覆盖 Task A 与 Task B 的不同职业，被动验证应拆成多个 scenario 或写清切换职业/fixture 的方式。

建议的白盒命令合同：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-playable-profession-passive-talents
```

### [P1] `TalentPassiveDetailSnapshot` 的 owner 边界不清：文档把 game/session boundary DTO 写成引用 client-only tone token

PR04-01 要求 “Game/session boundary must project passive detail before client rendering”，并新增：

```kotlin
data class PassiveEffectPresentationLineSnapshot(
    val effectKind: String,
    val labelKey: String,
    val valueArgs: Map<String, DescriptionValueSnapshot>,
    val toneToken: TalentPreviewToneTokenSnapshot,
)
```

见 `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:299-320`。问题是当前 `TalentPreviewToneToken` 是 `client` 的 UI model（`client/src/main/kotlin/com/ktome/client/ui/talent/TalentAssignPanelModel.kt:41-47`），而现有跨边界 snapshot 类型位于 `core.snapshot`，例如 `DescriptionValueSnapshot` 和 `DescriptionModelSnapshot`（`core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:229-254`）。PR04-01 Task 5 的 production scope 只列了 client 文件，却要求新增 game/session boundary DTO（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:526-539`）。

这会迫使实现者在三种错误之间选一个：

1. 让 `game` 或 `core` 引用 client tone token，违反 `client` 只能消费 typed snapshot 的 bridge 规则（`UI/pr/development-governance.md:111-120`）。
2. 在 `core` 新建同名 `TalentPreviewToneTokenSnapshot`，但不说明与 client token 的映射权威，形成第二套 tone 语义。
3. 把被动详情 DTO 放在 client，导致 `game/session boundary` 这一要求落空。

必须补充：

1. 明确 DTO 所属文件与模块。推荐把 `TalentPassiveDetailSnapshot`、line snapshot 和 tone enum 放入 `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt` 或同模块 snapshot 文件。
2. tone enum 不应命名为 client 现有 token 的 snapshot 复制品。建议使用更语义化的 `PassiveDetailLineToneSnapshot` / `DescriptionLineToneSnapshot`，由 client 映射到 `TalentPreviewToneToken`。
3. Task 5 production scope 必须加入 `core` snapshot 文件与 `game` snapshot projection / mapper 文件，例如 `FoundationGameSession.kt`、`SessionSnapshotMapper` 相关测试。
4. 若选择不把 tone 放进 snapshot，也要明确由 line kind / block kind 在 client 层映射，避免 renderer 反查 `PassiveEffect`。

### [P1] stable key 合同内部冲突：一边要求装备 passive 行为与 stable-key identity 保真，一边又定义了会改变现有 key 的新格式

PR04-01 要求 `04-01a` 在每个现有装备 passive kind 上证明旧行为与 stable-key identity 被保留（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:215-218`），Task 2 又要求 “Keep equipment source stable keys behavior-identical by using `sourceTemplateId=item.baseId`”（`469-475`）。但同一文档的 resolver contract 又规定日志和 trace 使用新格式：

```text
passive:<sourceKind>:<sourceTemplateId>:<effectKind>
```

见 `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:261-268`。

当前生产代码中的装备 passive cue key 并不是这个格式，例如：

1. `passive:hp_regen:${source.item.baseId}`（`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:11512-11518`）
2. `passive:on_hit_status:${source.item.baseId}:$statusId`（`11885-11894`）
3. `passive:on_kill_resource_restore:${trigger.source.item.baseId}:${trigger.resourceType.name}`（`11907-11915`）
4. `passive:damage_bonus:${source.item.baseId}:vs_tag:${passive.tag}`（`11943-11951`）

当前 `PassiveTriggerTrace` 也是 equipment-shaped：`sourceItemBaseId`、`sourceAffixId`、`sourceSpecialTemplateId`（`core/src/main/kotlin/com/ktome/core/combat/CombatResolutionTrace.kt:39-55`）。如果本 PR 直接改成 `sourceKind/sourceTemplateId/effectKind`，这已经不是 stable-key identity 保真；如果只在 talent passive 上使用新格式，文档又没有明确这个例外。

必须二选一：

1. **保守方案**：`04-01a` 保留所有装备 cue stable key 的字面格式，新格式只用于 talent passive；文档补一张 `EQUIPMENT old key -> TALENT new key` 对照表，并把 “behavior-identical” 定义到断言里。
2. **迁移方案**：承认这是 stable key 迁移，更新 parity 定义、frontstage cue 测试、trace/golden/whitebox 预期，并说明是否触碰 replay/log contract。

在两者未定前，`PassiveEffectResolverTest.equipmentPassiveParityCoversAllExistingKinds` 会不知道该断旧 key 还是新 key，开发时很容易把 cue 去重和日志证据做漂。

### [P2] content-pack / overlay 合同仍然过宽：文档说 overlays 可以添加 talent，但当前 runtime overlay loader 没有 `talent` registry

PR04-01 的 Public Interface 写明 “content-pack-owned talents can declare `passiveEffects`”，且 “Overlays can add namespaced content-pack talents with passive effects”（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:580-600`）。但当前 `DataLoader.applyContentPackOverlays` 只为 hidden event、secret zone、loot profile、monster、item/material/affix/special template、mutation、boss variant、action weight profile 建 overlay map（`game/src/main/kotlin/com/ktome/game/data/DataLoader.kt:668-805`），`parsePackOverlayPayload` 的 registry 分支也没有 `talent`（`945-1108`），sealed payload 同样没有 Talent payload（`1319-1367`）。

此外，PR04-01 的 M13 / Task 6 提到了 `contentPackLint.talentPassiveEffectsCannotDefineNewCoreKinds`（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:73-74`, `573-578`），但当前 root / tools Gradle task 中没有 `contentPackLint`，已有的是 `verifyContentPackPreflight`、`contentPackHarness`、`whiteBoxContentPack` 等 content-pack owner 入口（`build.gradle.kts:506-587`, `tools/build.gradle.kts:815-1041`）。

必须先收口 scope：

1. 若 PR04-01 不打算新增 talent overlay registry，则文档应改成 “content-pack talent passive schema 只做 fail-fast / fixture 预检，不允许 runtime overlay 添加 talent”，并删除或降级 “overlays can add talents”。
2. 若确实要支持 content-pack-owned talents，则 Task 6 必须显式列出 `DataLoader` overlay talent registry、fixture pack、content-pack preflight/harness/whitebox owner gate，并说明 `ADD` / `REPLACE` 是否允许作用于 official talents。
3. `contentPackLint` 要么改为现有任务 / subrule 名称，要么明确新增 Gradle task；若新增 task 或接线，按 `UI/pr/README.md:27-30` 必须补 `./scripts/verify-bootstrap.sh`。

### [P2] 被动详情 `valueArgs` / delta args 仍是 stringly map，未定义每个 passive kind 的 line template 与参数顺序

文档要求 passive detail snapshot 使用 `effectKind + labelKey + valueArgs: Map<String, DescriptionValueSnapshot>`，delta 使用 `beforeArgs/afterArgs`（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:307-320`）。后续 Display rules 只给了英文自然语言规则，例如 `On kill: restore <amount> <resource>`、`Damage vs <status>: +N%`（`621-630`）。

这仍然留了实现歧义：

1. `StatModifier(maxHp=12, defense=2)` 是一条 line 还是两条 line？
2. 多字段 stat modifier 的展示顺序由什么决定？`Map` 不能作为 UI 行顺序合同。
3. `ConditionalStatBonus` 是一条 “condition + stat summary” 还是 condition line + stat line？
4. `DamageTypeBonus` 的 `bonusPercent=0.03` 是在 game 里转成 `3`，还是 client 根据 `effectKind` 自己乘 100？
5. `beforeArgs/afterArgs` 对多 stat delta 如何配对？靠同名 key 还是一行一个 stat？
6. 中文/英文 phrase order 由 locale 负责，但 `labelKey + valueArgs` 没有 template key，容易让 presenter 拼字符串。

当前已有 `DescriptionModelSnapshot(templateKey, placeholders, keywords)` 这种 template 化结构（`core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:249-254`）。PR04-01 应优先复用或扩展它，而不是再造一组半结构化 map。

建议补一张硬表：

| passive kind | current line model | delta line model | required args | ordering rule | tests |
| --- | --- | --- | --- | --- | --- |
| `StatModifier` | one line per non-zero stat | one line per changed stat | `statId`, `value` / `before`, `after` | fixed stat order | presenter + mapper |
| `OnKillResourceRestore` | one trigger line | one changed amount line | `amount`, `resourceType` | N/A | mapper + locale |
| `ConditionalStatBonus` | condition prefix + stat line | same | `condition`, `statusId?`, stat args | condition first | mapper |

### [P2] PASSIVE 行为只覆盖了“不要显示 R Reserve”，没有锁住输入层和 runtime command 的拒绝语义

PR04-01 的 PASSIVE action matrix 明确禁止 `R Reserve` 和 active slot replacement（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:340-352`），Task 5 也要求 “no `R Reserve`, no active slot management rows”（`536-551`）。但这只覆盖了 presenter 展示。当前 presenter 的 actions block 是无条件加入 `R` 行的旧行为（`client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:733-759`），而文档没有明确玩家在 PASSIVE 行按 `R` 时应该发生什么。

必须补齐：

1. 输入行为：在 PASSIVE 选中态按 `R` 是 ignored、显示 locked hint，还是返回错误音？必须指定一个。
2. 状态不变断言：按 `R` 后不得产生 reserve draft、不得打开 `ACTIVE_TALENT_SLOT_CHOICE`、不得改变 `slotToTalentId`。
3. 自动化测试：除了 `TalentSidebarPresenterTest.passiveActionsDoNotShowReserveOrActiveSlotManagement`，还应有 input/controller 层测试，例如 `TalentAssignInputHandlerTest.passiveReserveShortcutDoesNotMutateDraft`，以及 runtime 层 `FoundationGameSessionTest.equipTalentToSlotRejectsPassive`。
4. 白盒步骤：在 PASSIVE 行按 `R` 并截图证明没有 modal，不能只截“未显示 R 文案”。

否则 UI 文案修了，键盘路径仍可能把 PASSIVE 放入 reserve / slot，这在类 ToME 技能面板里是很隐蔽但很伤体验的 bug。

### [P2] Runtime refresh 合同覆盖了 status / health threshold / terrain movement，但测试清单只覆盖了很窄的组合

Runtime contract 写明：learn、rank-up、respec、save/load、equipment change、status change、health threshold change、terrain movement 都会刷新相关 passive stat context（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:277-287`）。但 Task 2 / Task 4 的测试清单主要是 learn、save/load、on-kill、health+status 的一个组合（`477-481`, `520-524`），没有锁住这些状态边界：

1. status 消失后，`ConditionalStatBonus(SELF_HAS_STATUS)` 是否会撤销？
2. HP 从低血线恢复到高血线后，health threshold passive 是否会撤销？
3. 进入/离开 terrain tag 后，`TerrainAffinityBonus` 是否刷新？
4. respec 后，被动派生 stat 是否清掉？
5. equipment 与 talent passive 同时存在时，刷新顺序是否确定且不重复累计？

建议补充测试名或缩小合同：

```text
FoundationGameSessionTest.conditionalTalentPassiveRefreshesWhenStatusExpires
FoundationGameSessionTest.healthThresholdTalentPassiveRecomputesAfterDamageAndHealing
FoundationGameSessionTest.terrainAffinityTalentPassiveRefreshesAfterMovement
FoundationGameSessionTest.respecRemovesTalentPassiveDerivedStats
PassiveEffectResolverTest.equipmentAndTalentStatModifiersDoNotDoubleAccumulateAfterRefresh
```

如果 PR04-01 不准备把 terrain / health threshold talent passive 做到可验证，应把 runtime rule 改成 “resolver supports; official Task B only verifies status/health subset”，不要写成全部 runtime MUST。

### [P2] Gate Budget 还没有把 screen coverage matrix 与 whitebox scenario materialization 纳入关闭条件

`UI/pr/README.md` 要求所有玩家可见或验证可见 UI 面必须映射到 screen coverage matrix（`UI/pr/README.md:33-37`）。PR04-01 会新增 PASSIVE detail、PASSIVE action suppression、可能新增 golden label，但 Cross-PR Dependencies 只写 “PR07 final all-screens index must include PASSIVE detail coverage if PR04 golden changes”（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:653-660`），没有把当前 PR 的矩阵更新或显式豁免列为 Task 6 / self-audit 项。

这会让 PR04-01 关闭时只记录 `clientSmoke/goldenScreenshot`，而 screen coverage matrix 仍停留在 PR04 Talent Assign 的旧覆盖。建议：

1. Task 6 增加 `UI/pr/screen-coverage-matrix.md` 的更新或 “no new screen; existing PR04 Talent Assign row adds passive-detail evidence” 的明确结论。
2. golden label 固定为 `dark-uiux-pr04-01-*`，不要混入旧 `dark-uiux-pr04-*` 证据标签。
3. self-audit 的 `golden / whitebox` 行补充 “screen coverage matrix row updated / intentionally unchanged with reason”。

### [P3] `04-01b` 同时触碰 `core/game/client`，但没有解释为什么无法再拆

Execution Slices 中 `04-01b-static-profession-passives` 的 production modules 是 `core`, `game`, `client`（`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:46-50`）。bridge exception 允许跨 owner，但工作包规则仍要求超过两个生产模块时说明为什么无法继续拆。

这不是阻断项，因为 PR04-01 已经显式声明为 gameplay/content bridge，并且三段 slice 方向正确。但建议加一句：

```text
04-01b intentionally touches core/game/client atomically because PASSIVE data conversion must not land without the typed detail snapshot and action suppression; stopping point is a playable Talent Assign panel with Task A only.
```

这样可以防止后续实现把 Task A data 先改完但 UI detail/action 还没闭环，导致主干短期可玩性下降。

## Requirement Alignment

| Requirement | 当前对齐度 | 说明 |
| --- | --- | --- |
| 6 个可玩职业每职业至少 2 个 PASSIVE | Aligned | §3 与 self-audit 已列 Task A / Task B talentId，方向清楚。 |
| PASSIVE 不占主动槽、不打开 active slot modal | Partially aligned | presenter 展示合同充分，但输入层 `R` / runtime command 拒绝语义未完整锁住。 |
| 装备与天赋使用统一 passive resolver | Partially aligned | source-agnostic model 已补齐，但 stable key / trace 字段迁移策略冲突。 |
| 当前 rank / 下一级 passive detail typed 展示 | Partially aligned | DTO 方向正确，但 owner 边界和 line model 仍模糊。 |
| content-pack fail fast | Partially aligned | unknown kind fail-fast 目标正确，但 overlay talent registry 与 Gradle gate 名称不匹配。 |
| 白盒证据闭环 | Not aligned | 缺 scenarioId、registry/YAML、manual record、固定运行上下文和 exact steps。 |
| PR04 上游 UI 合同保持 | Mostly aligned | block order 与 active slot modal 依赖已写清；还需 screen coverage matrix 收口。 |

## 功能 / 系统一致性矩阵

| 系统面 | 期待 owner | 当前文档状态 | 风险 |
| --- | --- | --- | --- |
| Passive typed model | `core` | 已定义 sealed model 与 source kind | stable key 与 trace identity 未最终定版 |
| Talent schema / official data | `game` | `passiveEffects`、Task A/B 表已明确 | content-pack scope 过宽时会拖入 overlay registry |
| Runtime stat / trigger resolver | `core` / `game` | source-agnostic resolver 方向正确 | refresh trigger 测试覆盖不足，容易 stale / double accumulation |
| Talent Assign presentation | `game` snapshot -> `client` render | typed detail snapshot 方向正确 | tone token 和 line args owner 模糊 |
| PASSIVE action affordance | `client` + `game` command guard | 展示矩阵明确 | 输入层 shortcut 与 runtime reject 未列完整 |
| Content-pack validation | `game` / `tools` | 目标写到了 | 当前 loader/task 真源不支持文档声称的 talent overlay |
| Whitebox | `game` scenario registry + `tools` CLI + `client` evidence | evidence path 写到了 | 缺可物化的 scenario contract |

## 玩法与体验审查

新版文档比上一版明显更接近一个可开发的 ToME 式职业被动改造方案：`ACTIVE / SUSTAINED / PASSIVE` 的角色分工清楚；6 个 playable 职业至少两个被动的目标合理；Task A 静态构筑身份 + Task B trigger/conditional/resource 被动的组合，比单纯把数值塞成被动更符合 Roguelike 长局构筑。

仍需小心的体验风险：

1. PASSIVE 详情必须让玩家看见“当前收益”和“下一级具体变化”，不能只变成描述文案。尤其 `StatModifier` 多字段、`ConditionalStatBonus`、`OnKillResourceRestore` 需要稳定模板，否则中文/英文 UI 会出现细节不一致。
2. PASSIVE 不占主动槽是核心体验收益。只隐藏 `R Reserve` 文案不够，键盘输入和 runtime command 必须硬拒绝。
3. Task B 的 trigger/conditional 被动是职业身份的主要来源。白盒不能只截图静态 `maxHp/defense`，必须至少覆盖一个 trigger/resource 被动和一个 conditional 被动。
4. 被动 rank replacement 不能累加旧 rank，这一点文档写清了；实现时必须同时覆盖 learn、rank-up、respec、save/load，否则玩家会在长局里遇到属性幽灵加成。
5. stable key 迁移如果没有定版，会直接影响前台 cue 去重。类 ToME 战斗日志本来就密集，重复或丢失 passive cue 都会明显伤害可读性。

## 当前阶段必须解决的问题

进入实现前，至少应先修以下文档点：

1. 写出 PR04-01 白盒 scenario materialization 合同，并接入 registry / YAML / test。
2. 定版 passive detail snapshot 的 owner、文件路径、tone token 映射方式。
3. 定版装备 passive stable key 是保留旧格式，还是显式迁移。
4. 定版 content-pack scope：是否真的支持 talent overlay registry。
5. 补足 PASSIVE 输入层 `R` 行为和 runtime command reject 的测试要求。
6. 补足 status/health/terrain/respec refresh 的测试或缩小 runtime MUST。
7. 把 screen coverage matrix / golden label / manual record 纳入 close checklist。

## Removal / Iteration Plan

建议先做一次 doc-only 返修，不要直接进入 Kotlin 实现：

1. 在 §7 下面新增 `Whitebox Scenario Materialization` 小节，写 scenarioId、runtime、CUA steps、manualRecordPath、required evidence files。
2. 在 Task 5 拆清 `core snapshot -> game mapper -> client presenter` 三段 owner，并把相关文件加进 production scope。
3. 在 §2.3 增加 stable key decision table，明确 equipment old key 保留或迁移。
4. 在 §5 把 content-pack scope 改成二选一：不支持 talent overlay，或完整新增 talent overlay owner gate。
5. 在 Acceptance Matrix M09/M12/M13 中补齐新测试、owner gate 和 artifact。
6. 返修后先跑 `acceptanceContractLint`，通过后再开始 `04-01a`。

## Additional Suggestions

1. 把 `PassiveSource` 的 `sourceId` / `sourceTemplateId` 加一张 glossary：equipment base item、equipment instance、affix、special template、talent rank 各自怎么填。现在仅靠一句话说明，后续 trace 很容易分叉。
2. 对 `PassiveEffect` 的九种 kind 增加 “official data 是否使用 / equipment parity only / UI detail required” 三列，避免 Task B 不使用的 kind 被 mapper 漏实现。
3. `TalentPassiveDetailSnapshot` 不建议暴露 raw `effectKind: String` 给 client 决策。可以保留为诊断字段，但展示分支应依赖 typed `lineKind` 或 template key。
4. 白盒截图建议固定三类 talentId，而不是 “selected row”：一个静态 stat passive、一个 on-kill/resource passive、一个 conditional/status passive。
5. 如果 packaged app whitebox skipped，manual record 里应使用 `whitebox=skipped`、skip reason、replacement evidence、residual risk；不要只写在 PR description 里，否则后续审计找不到 repo-owned 记录。

## Suggested Verification

本次复审没有执行 Gradle / 测试命令，只做了文档与代码真源只读核对。建议 PR04-01 文档返修后按以下顺序验证：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

实现 `04-01a` 后：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :core:test --tests '*PassiveEffectResolver*' :game:test --tests '*Passive*'
```

实现 `04-01b` 后：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests '*Talent*' :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.ui.talent.DescriptionPresenterTest
```

实现 `04-01c` 后：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew contractLint localeLint maintainabilityLint
./gradlew clientSmoke goldenScreenshot
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-playable-profession-passive-talents
./gradlew verifyChanged
```

若最终选择新增或改动 content-pack overlay / Gradle task 接线，还必须补：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./scripts/verify-bootstrap.sh
./gradlew verifyContentPackPreflight contentPackHarness whiteBoxContentPack
```

## Summary

Round 2 文档已经修复了上一轮最核心的方向性问题：PR 类型、三段 slice、rollback invariant、passive typed model、coverage classification、Task A/B 固定 talent 集合都更清楚了。

但目前还不建议直接进入实现。剩余问题集中在“合同能不能被开发者稳定执行”：白盒 scenario 仍不可物化，snapshot owner 边界仍可能让 `game/core` 泄漏 client token，stable key 保真与新 key 格式冲突，content-pack scope 超过当前 loader 真源。先把这些写实，后续 Kotlin 实现会少很多返工。

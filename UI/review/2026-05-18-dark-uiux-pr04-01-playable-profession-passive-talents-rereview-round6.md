# Dark UI/UX PR04-01 Playable Profession Passive Talents Re-review Round 6

审查对象: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`

审查时间: 2026-05-18

结论: **部分一致**。本轮文档已经修复了上一轮最关键的 data owner、callbacks 禁用、unknown kind 投影边界、证据清单和 PASSIVE action matrix 的大量歧义，但仍有 3 个 P1 和 4 个 P2 会让实现阶段出现偏差、测试自证不足或数值合同自相矛盾。尤其是白盒 focus-by-id 仍没有打通到 client boundary，`hpRegen` 单路径实际是全局战斗公式变化，`flux_anchor` 的 rank 1 MANA 恢复量违反文档自己声明的 5% 规则。

## Findings

### [P1] 白盒 `initialFocusedTalentId` 仍没有可实现的 client boundary

**证据**

- PR 文档新增 `ValidationScenarioTalentSetupSpec.initialFocusedTalentId`，并声明 `FoundationGameSession` 消费 `ValidationSessionOptions.scenarioTalentSetup`: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:1165-1197`。
- 文档要求 CUA 用 `F9, Enter, Esc, T` 后直接聚焦目标 talent，且不得依赖重复 `Down` 次数: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:1207`。
- 当前 client 的 Talent Assign 进入逻辑只从本地 `talentTreeSelection` / `talentTreeSelectionIdentity` 恢复焦点: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1676-1690`。
- 当前恢复逻辑只读取 modal local identity 或 client 缓存 identity；没有读取 session / snapshot 提供的 preferred talent id: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:2196-2215`。
- 当前仓库中没有 `initialFocusedTalentId` / `scenarioTalentSetup` 的生产实现锚点。

**问题**

文档把 focus owner 放在 `ValidationScenarioDef -> ValidationSessionOptions -> FoundationGameSession`，但实际 UI 焦点是 client-local state。`FoundationGameSession` 可以准备等级、前置 rank 和点数，但不能直接设置 `InputHandler` 的选中行。实现者按当前文档做完 game/tools scope 后，CUA 仍可能截图到上一次 client 本地焦点或树中第一个节点，`expectedFocusedTalentId` 只能事后发现失败，不能保证进入时选中正确目标。

**建议**

补一个明确的跨层边界，二选一即可：

1. 在 `RenderSnapshot.uiState` 或 validation overlay snapshot 增加 `preferredTalentTreeSelectionIdentity` / `initialFocusedTalentId`，`InputHandler.enterTalentAssign()` 在打开 Talent Assign 时消费一次并写入 modal local state。
2. 不改 snapshot，则 CUA runbook 必须生成 deterministic key navigation，并在每一步截图前执行 typed focus assertion；同时文档不能再说 setup 已经 focus 目标 talent。

推荐方案 1，因为 PR04 已经把 talent focus identity 作为 typed UI state 使用，继续通过 snapshot boundary 传递最符合现有架构。

### [P1] `hpRegen` 单路径合同是全局战斗公式变更，当前文档仍按局部 passive display 处理

**证据**

- 文档规定 `StatModifier.hpRegen` 和 `HpRegenPerTurn.amount` 都进入 `rawHpRegen`，再统一走 `DiminishingReturns.effectiveHpRegen(rawHpRegen)`: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:395-405`。
- 文档规定 turn-start healing 每回合只应用一次 `DerivedStats.hpRegen`: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:407-408`。
- 当前 `StatsCalculator` 的 `hpRegen` 是线性值，不走 DR: `core/src/main/kotlin/com/ktome/core/stats/StatsCalculator.kt:129`。
- `DiminishingReturns.effectiveHpRegen()` 当前存在但没有被生产公式使用: `core/src/main/kotlin/com/ktome/core/combat/DiminishingReturns.kt:26`。
- 当前装备 `HpRegenPerTurn` 在 turn start 单独直接治疗，并记录 `log.passive.hp_regen`: `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:11505-11523`。

**问题**

这不是单纯的被动详情显示修正，而是同时改变了三件事：

1. 所有 actor 的 `DerivedStats.hpRegen` 从线性公式改成 DR 公式。
2. 现有装备 `HpRegenPerTurn` 从独立每回合治疗改成并入 derived stat。
3. 现有 `log.passive.hp_regen` 的来源、金额和触发时机可能变化。

文档只新增了 M17 和几个 focused tests，但没有把它提升为 core combat formula migration，也没有要求现有装备 hp regen 行为的 before/after baseline。实现阶段很容易为了通过 `pain_fuel` 展示测试而改掉全局治疗曲线，导致旧装备、怪物或长期战斗 sustain 失衡。

**建议**

把 M17 明确改成 core formula migration，并补足这些阻塞条件：

- `StatsCalculatorTest` 覆盖无 passive、仅 `StatModifier.hpRegen`、仅 `HpRegenPerTurn`、二者共存的 effective value。
- `FoundationGameSessionTest` 覆盖 turn-start 只治疗一次，且装备旧 cue key / source identity 行为可预期。
- 明确 `log.passive.hp_regen` 是否保留、金额取 effective 后的总治疗还是只记录 passive source contribution。
- 如果不想做全局公式迁移，则 `hpRegen` DR 只能用于 detail projection，不应声明 `DerivedStats.hpRegen` 和 turn-start runtime 都改路径。

### [P1] `flux_anchor` rank 1 MANA 恢复量违反文档自己的 5% 阈值

**证据**

- `flux_anchor` rank 1 设计值是 `MANA +2`: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:733`。
- Spellblade MANA 池是 `52/52`: `game/src/main/resources/data/professions/index.yaml:187-189`。
- 文档资源表承认 `flux_anchor +2` 只有 `3.8%`，但说因为有 LIGHTNING damage/resistance 所以接受: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:686`。
- 同一段下一行又声明 `TalentSchemaTest.passiveOnKillResourceRestoreRank1IsAtLeastFivePercentOfStartingPool` 对 `MANA` 和 `POSITIVE_ENERGY` 强制执行，只有 `ENERGY` 可在 MARKED payoff 下豁免: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:690`。

**问题**

这是直接自相矛盾。按测试名和规则执行，`flux_anchor +2` 必然失败；按表格豁免执行，又会让测试名、规则和文档说法不一致。开发者无法知道应该改数值、改测试，还是加一个 MANA paired-payoff exception。

**建议**

优先把 `flux_anchor` rank 1 MANA 恢复提高到 `+3`，约为 Spellblade MANA 池的 5.8%，这样不需要新增例外。若设计上坚持 `+2`，则必须把第 690 行改成明确的 MANA paired-payoff 例外，并同步改测试名，例如 `passiveOnKillResourceRestoreRank1MeetsResourceFloorOrDeclaredPairedPayoffException`，避免 lint 合同说一套做一套。

### [P2] PASSIVE `R` 抑制漏掉了当前真实的 `RespecTalentTree` 命令

**证据**

- 文档说 PASSIVE 选中时按 `R` 被忽略，不发 `ConfirmTalentDraftToReserve`、`EquipTalentToSlot`、`ACTIVE_TALENT_SLOT_CHOICE`: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:600-607`。
- Log forbidden fragments 也只列了 reserve / slot / active slot choice 和 rollback: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:1229-1231`。
- 当前 `InputHandler` 中 Talent Assign 模式按 `R` 返回的是 `PlayerCommand.RespecTalentTree`，不是 reserve command: `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt:1349-1352`。

**问题**

文档把 `R` 口径写成了 Reserve 抑制，但当前代码里 `R` 的真实命令是 respec tree。实现者可能只写断言 “没有 reserve / slot command”，然后 PASSIVE 上按 `R` 仍然触发整棵树 respec。这个问题不会被当前 forbidden fragments 捕获，也不一定会被只比较 reserve draft 的状态测试捕获。

**建议**

在 PASSIVE action matrix、InputHandler tests、log forbidden fragments 和 state test 中显式加入：

- 不得发出 `PlayerCommand.RespecTalentTree`。
- 不得改变 learned ranks、pending ranks、previous pending ranks、loadout slots。
- UI footer / right-pane 不得提示 `R` 对 PASSIVE 有任何可执行含义。

如果 PR04 的正式语义确实把 `R` 作为 respec 而非 reserve，则 PR04-01 文档需要先统一术语，不能继续写 `R Reserve`。

### [P2] EV lint 仍是表格自证，缺少可复算公式

**证据**

- 文档定义 `rank5NormalizedEv` 是 design-owned relative score，不是 runtime formula: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:637-641`。
- 文档要求 `TalentSchemaTest.passiveRank5EvAnchorWithinTwentyPercent` 解析表格并检查 `[80, 120]`: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:660`。
- 文档又要求 DR 字段按 effective route 计算 EV contribution: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:641,664`。
- 但文档没有给出 maxHp、defense、resistance、resource restore、damage bonus、uptime、条件 payoff 之间的权重公式或 normalize 方法。

**问题**

当前写法无法实现真正的 “公式合理性” 校验。测试如果只解析文档表格，就只能证明作者填的 `rank5NormalizedEv` 在 80-120 之间，不能证明数据表实际值合理；如果测试想从 talent YAML 复算 EV，又没有权重表和 normalize anchor。

**建议**

把 EV 合同拆成两个层次：

1. **机器可复算 lint**: 明确权重表和公式，例如 `effectiveHpDelta * hpWeight + defense * defenseWeight + damageBonus * expectedUptime * damageWeight + restoreAmount / spendBaseline * resourceWeight`，并注明 DR 字段的 effective delta 入口。
2. **设计审阅注释**: `rank5NormalizedEv` 可以保留为人工设计分，但不能伪装成自动化平衡公式。

如果暂时不愿冻结权重公式，应把测试名改成 `passiveRank5EvAnchorDeclaredWithinTwentyPercent`，并新增人工 balance audit item，避免给开发者错误信号。

### [P2] `typedAssertions` / `localizedVisibleAssertions` 仍是 stringly contract

**证据**

- `ValidationScenarioEvidenceStep` 把 `typedAssertions` 和 `localizedVisibleAssertions` 定义成 `List<String>`: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:1174-1182`。
- 第一屏断言表里写的是 `kind=PASSIVE`、`statId=maxHp value=+10`、`damageType=FIRE percent=+3%` 这类自由文本: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:1211-1215`。

**问题**

这比上一轮“没有 owner”进步了，但仍然不是 typed assertion。测试最多能检查字符串存在、非空或与文档字面一致，不能可靠验证字段类型、数值、顺序、localized key 和 screenshot 之间的关系。后续实现很容易变成 tools 解析自由文本，形成新的第二套微型 DSL。

**建议**

把断言模型改成结构化类型，至少覆盖这几类：

- `FocusedTalentAssertion(talentId, category, rank, state)`
- `PassiveLineAssertion(kind, statId?, damageType?, resourceType?, statusId?, value, orderIndex?)`
- `LocalizedTextAssertion(locale, key?, visibleTextPolicy, evidenceFile)`

如果为了报告简洁仍要在 runbook 中输出人读文本，也应由结构化断言生成，而不是把字符串作为真源。

### [P2] Scenario setup 使用 `minimum*`，但验收要求的是 exact clean state

**证据**

- setup spec 使用 `minimumPlayerLevel`、`minimumUnspentTalentPoints`: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:1165-1172`。
- setup contract 表要求首张 Talent Assign 截图中目标是 `LEARNABLE`, rank `0`, not locked: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:1201-1205`。

**问题**

`minimum*` 适合准备可达性，不适合白盒截图验收。若 scenario 复用已有 profile/run state，目标 talent 已学、已有 pending rank、预览已展开、或多余点数触发不同 action text，仍可能满足 “minimum level/points”，但不满足首图 rank 0 learnable 的合同。

**建议**

setup spec 增加 exact reset 字段，并明确由 `FoundationGameSession` 在 preparation hook 内执行：

- `targetRank: 0`
- `clearPendingTalentDraft: true`
- `clearActiveSlotChoiceModal: true`
- `previewExpanded: false`
- `exactUnspentTalentPoints` 或 `setUnspentTalentPoints`
- `resetTalentLoadoutSlotsForTargetOwner` 的策略

如果保留 `minimum*`，至少要再加 post-setup assertion，失败时 materialization 直接 fail fast，而不是生成可能截图错误状态的 runbook。

### [P3] Required evidence scenarios 表没有覆盖 canonical/required files 全量清单

**证据**

- Canonical Artifact 列出了 static collapsed、static after-learn、trigger panel-entry、trigger after-learn、action log screenshot 等证据: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:116-131`。
- `requiredEvidenceFiles` 也列了这些文件: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:1219-1223`。
- 但 “Required evidence scenarios” 表只列了 10 行，漏掉了 `passive-static-preview-collapsed-after-toggle.png`、`passive-static-after-learn-no-slot-modal.png`、`passive-trigger-panel-entry.png`、`passive-trigger-after-learn-no-slot-modal.png`、`passive-action-log-no-reserve.png`: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:1282-1295`。

**问题**

主清单和 §7 执行清单不一致，会导致人工白盒执行者按后面的表漏收证据，或工具按前面的 JSON 清单要求证据时失败。

**建议**

让 “Required evidence scenarios” 表完全镜像 `requiredEvidenceFiles`，或删除该表，改成声明以 `expected-evidence.json.requiredEvidenceFiles` 为唯一执行清单。

### [P3] PASSIVE footer hint 未被明确纳入 action matrix

**证据**

- 文档 action matrix 说 PASSIVE 不显示 `R Reserve` 和 active slot replacement: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:588-600`。
- 当前 `TalentSidebarPresenter.footerHints()` 无条件加入 `RESERVE` footer hint: `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt:1026-1053`。
- Task 5 只写 “no `R Reserve`, no active slot management rows”，没有点名 footer/global legend: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:957-958`。

**问题**

开发者可能只隐藏右侧 action row，却保留 footer 的 `R Reserve` 提示。对玩家来说这仍是错误 affordance：当前选中 PASSIVE，底部却告诉他 `R` 可用。

**建议**

把 footer hint / legend 纳入 M12 验收：

- PASSIVE focused 时 footer 不显示 `RESERVE`。
- 若当前面板中仍有 active/sustained 节点，footer 是否全局显示 `R` 必须有明确 UX 决策；推荐按 focused node category 动态显示。
- `TalentSidebarPresenterTest.passiveActionsDoNotShowReserveOrActiveSlotManagement` 需要断言 footer hints，而不只断言 action rows。

## Requirement Alignment

| Requirement | 当前状态 | 说明 |
| --- | --- | --- |
| Source-agnostic passive model | 部分一致 | 文档已明确 legacy `core.item` 物理包、统一 `PassiveEffect`、禁止双 hierarchy；实现风险主要在 hpRegen 迁移影响面。 |
| PASSIVE action suppression | 部分一致 | reserve / slot 口径已补强，但漏掉当前真实 `RespecTalentTree` 命令和 footer hint。 |
| Passive detail current/next | 大体一致 | line template、DR display、line budget、known kind exhaustive mapping 已较完整；typed assertions 仍需结构化。 |
| Task A / Task B 数值表 | 部分一致 | 多数字段已达到可读构筑价值；`flux_anchor` rank 1 restore 与资源阈值冲突。 |
| Whitebox materialization | 部分一致 | data owner 已落到 `ValidationScenarioDef`，但 focus-by-id 缺 client boundary，evidence table 仍不完全一致。 |
| Content-pack / overlay 边界 | 一致 | unknown kind、content-pack 不扩 core enum、loader/contract-lint 责任已经清楚。 |
| Verification / gates | 部分一致 | gate 覆盖广，但 EV lint、hpRegen migration、R command suppression 需要补可执行断言。 |

## 功能/系统一致性矩阵

| 系统面 | 文档期望 | 当前 review 判断 | 必须修正 |
| --- | --- | --- | --- |
| Core passive aggregation | `PassiveSource` 合并装备与 talent source，按 kind 聚合 | 方向正确 | 明确 hpRegen 是否做全局公式迁移 |
| Derived stats | hpRegen 单一路径并走 DR | 风险高 | 补 StatsCalculator 和 turn-start baseline |
| Combat trigger source identity | TALENT / EQUIPMENT source-aware logs | 基本可实现 | 需要保留旧 equipment cue key 合同 |
| Talent Assign input | PASSIVE 上 `R` 完全 ignored | 不完整 | 禁止 `RespecTalentTree`，不只禁止 reserve/slot |
| Talent Assign focus | 白盒 setup 后直接聚焦目标 talent | 不可实现 | 增加 snapshot/client focus boundary 或改 runbook 导航 |
| UI detail evidence | current/next、collapse、after-learn、logs 全收 | 清单不一致 | §7 scenarios 表补全 |
| Balance lint | EV、uptime、trigger owner 自动检查 | 半自动 | 冻结 EV 复算公式或降级为声明检查 |

## 玩法与体验审查

总体方向比上一轮明显更接近类 ToME 的可玩被动：常驻身份 + 条件/触发 payoff 的结构是对的，Task A 和 Task B 分工也能让玩家在每个职业上看到至少一个稳定身份锚点和一个构筑 loop。

仍需要注意三点：

1. `flux_anchor` 的 `MANA +2` 在 Spellblade 52 点池上体感偏弱，而且文档规则也不允许它低于 5%。若不提高到 `+3`，玩家会把它理解成 “附带一点点回蓝的闪电被动”，而不是可围绕击杀链构筑的 payoff。
2. `hpRegen` DR 会改变 Berserker `pain_fuel` 的实际体感。如果 rank 5 只给 `hpRegen +1.2`，在高 CON / 装备 regen 场景下 effective delta 会进一步被压低；EV 公式必须用 effective delta 复算，否则 `pain_fuel` 可能再次回到 “看起来有数值，实际不改变 loop”。
3. PASSIVE 的 UI affordance 必须做到“玩家不会尝试把它放进槽”。只去掉 active slot modal 不够，footer hint、right-pane action、log、input command 都要统一。

## 当前阶段必须解决的问题

1. 补白盒 focus-by-id 的真实 client boundary，或修改 CUA runbook 策略，不能继续只把 focus owner 放在 `FoundationGameSession`。
2. 明确 `hpRegen` 是全局 formula migration 还是 detail-only effective display；若是前者，补全 core/game baseline。
3. 修正 `flux_anchor` rank 1 MANA 恢复阈值冲突。
4. PASSIVE `R` 抑制必须覆盖 `RespecTalentTree`。
5. EV lint 必须有机器可复算公式，或降级为 declared anchor 检查并承认需要人工 balance audit。

## Removal/Iteration Plan

| Item | 当前处理建议 | Owner |
| --- | --- | --- |
| `EquippedPassiveSource` 生产路径 | 文档已要求生产路径移除，仅保留 deprecated test fixture helper | 保持现状，implementation self-audit 必查 |
| `EquipmentPassiveStatModifier` | 文档要求 rename 为 `PassiveStatModifier` | 保持，但补 `StatsCalculator` call-site audit |
| hpRegen parallel system | 不应只做局部删除 | 先决定全局迁移，再删除独立 turn-start branch |
| stringly whitebox assertions | 不建议带入实现 | 本 PR 内改成结构化 assertion model |
| EV table-only lint | 不建议作为最终 gate | 本 PR 内补公式，或明确降级为 design declaration |

## Additional Suggestions

- 在 §2.3 hpRegen 段补一行 “legacy equipment `HpRegenPerTurn` visible cue policy”，否则实现者会不知道旧 `log.passive.hp_regen` 是否继续存在。
- 在 §2.6 统一 `R` 的术语。当前 PR04 代码里 `R` 是 `RespecTalentTree`，文档写 `R Reserve` 会误导测试设计。
- 在 `ValidationScenarioTalentSetupSpec` 中加入 `expectedTargetState`，让 materialization 在生成 runbook 前先检查 rank/state/focus，而不是把错误状态留到人工截图阶段发现。
- `passiveLineCountBudget` 建议同时检查 current detail 和 next preview 的 localized rendered line count；只数 typed line object 可能漏掉本地化换行导致的 viewport overflow。

## Suggested Verification

本轮只做文档和实现锚点审查，未运行 Gradle 测试。建议修改文档后优先跑以下最小验证：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint contractLint :core:test --tests '*StatsCalculator*' --tests '*PassiveEffectResolver*' :game:test --tests '*FoundationGameSessionTest*' :client:test --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest
```

白盒 materialization 仍需串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression
```

额外建议新增或点名这些断言：

- `InputHandlerTest.passiveTalentAssignReserveShortcutDoesNotEmitRespecCommand`
- `TalentSidebarPresenterTest.passiveFocusedFooterDoesNotShowReserveHint`
- `StatsCalculatorTest.hpRegenUsesEffectiveHpRegenAfterPassiveAggregation`
- `FoundationGameSessionTest.legacyEquipmentHpRegenCuePolicyIsPreservedAfterSinglePathMigration`
- `ValidationScenarioRegistryTest.pr04_01TalentSetupHasExactCleanTargetState`
- `TalentSchemaTest.fluxAnchorManaRestoreMeetsDeclaredResourceFloor`

## Summary

这版 PR04-01 文档已经从 “大方向正确但执行 owner 分散” 进化到 “多数 owner 已收口”。剩下的问题不多，但都很具体：focus-by-id 需要打到 client，hpRegen 需要按全局公式迁移处理，资源恢复阈值要自洽，`R` 要按真实命令抑制，EV lint 不能只检查表格数字。修完这些后，这份文档才适合作为 XL 级 gameplay/content bridge 的实现合同。

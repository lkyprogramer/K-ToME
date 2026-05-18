# PR04-01 可玩职业被动天赋 Round 5 复审报告

Review target: `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`

Review date: 2026-05-18

Review stance: 资深 Roguelike / 类 ToME 职业系统、被动天赋设计、UI 合同、实现歧义与白盒验收审查。

本轮是按照 Round 4 反馈修订后的再次复审。上一轮的主要问题已经基本关闭：白盒路径不再依赖脆弱的重复 `Down` 次数，场景已经声明目标被动 id 与 setup state，locale token 归属已经固定，`pain_fuel.hpRegen` 已回到 Task A / M03，`Unyielding` 示例值已和最终表一致，`DamageTypeBonus` 顺序已指定，`previewExpanded=false` 也已声明。

当前剩余问题不是大方向设计失败，而是开发执行口径仍有细小但会分叉实现的歧义。它们需要在编码前补清楚，否则两个开发者可能写出不同结构，却都认为自己遵守了文档。

## Findings

### P1 - `PassiveEffect.StatModifier` 与现有 `StatModifier` 同名，容易把合同实现错

文档证据：

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:172-185` 定义了 `sealed interface PassiveEffect`。
- 同一段写了 `data class StatModifier(val modifier: StatModifier) : PassiveEffect`。

当前代码证据：

- `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt:140-158` 已经存在 `data class StatModifier(...)`。

问题：

- 在 `PassiveEffect.StatModifier` 这个嵌套类型内部，构造参数 `val modifier: StatModifier` 对读者和实现者都有歧义。
- 如果开发者照抄，很容易误以为参数类型是新建的 `PassiveEffect.StatModifier`，而不是已有的 `com.ktome.core.item.StatModifier`。
- 即使最后可以通过全限定名让 Kotlin 编译，文档也没有说明 YAML 里的 `kind: StatModifier` 是否仍映射到一个改名后的 Kotlin 类型。

建议修正：

- Kotlin 类型改名为 `StatModifierEffect` 或 `StatModifierBonus`，同时声明序列化 kind 仍为 `StatModifier`。
- 或保留类名，但在文档代码块里明确写全限定 payload 类型：

```kotlin
data class StatModifier(
    val modifier: com.ktome.core.item.StatModifier,
) : PassiveEffect
```

- 如果改名，schema 章节必须补一句：`kind: StatModifier` 映射到 Kotlin 的 `StatModifierEffect`。

### P1 - 白盒 focus/setup 要求已经提出，但缺少真正的模型落点和文件 owner

文档新增的 setup 表是正确方向，但还没有变成可实现合同。

文档证据：

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:782-793` 是 Task 6 production scope。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:807-808` 要求支持 `targetTalentId`、`initialFocusedTalentId`、等级、前置 rank、未花费点数、`previewExpanded=false` 和 `expectedFocusedTalentId`。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:912-918` 给出了场景 setup 表。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:936` 要求 `ValidationScenarioRegistryTest.pr04_01PassiveTalentScenariosFocusExpectedTalentIds`。

当前代码证据：

- `game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt:31-36` 的 `ValidationScenarioEvidenceStep` 只有 `mode`、`input`、`expectedVisibleResult`、`evidenceFile`。
- `game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt:139-149` 的 runtime spec 没有 setup 字段。
- `game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt:160-187` 只校验证据文件和 CUA step，没有 focus id 合同。
- `game/src/main/kotlin/com/ktome/game/validation/ValidationAction.kt:170-173` 只有 `scenarioId` 和 `actionId`。
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1899-1932` 已经存在按 scenario id 分发的 preparation hook。
- `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioMaterializationCatalog.kt:6-15` 目前只有 id 和窗口尺寸。

问题：

- 文档说 `expectedFocusedTalentId` 必须写入 `expected-evidence.json`，但没有决定它属于 `ValidationScenarioEvidenceStep`、新的 setup spec、materialization catalog，还是 PR04-01 专属 side table。
- Task 6 scope 没列 `ValidationScenario.kt`、`ValidationAction.kt`、`FoundationGameSession.kt` 这些实际很可能必须改的文件。
- 如果没有单一数据 owner，开发者可能在 `tools` 里补一个白盒专用 focus map，而 runtime registry 不知道它。这样会重新引入第二真源。

建议修正：

- Task 6 production scope 补上：
  - `game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt`
  - `game/src/main/kotlin/com/ktome/game/validation/ValidationAction.kt`，如果 action payload 要扩展
  - `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
  - 若 setup 通过 session options 传递，则补对应 `ValidationSessionOptions` 文件
- 明确一个权威数据结构，例如：

```kotlin
data class ValidationScenarioTalentSetupSpec(
    val targetTalentId: String,
    val initialFocusedTalentId: String,
    val minimumPlayerLevel: Int,
    val prerequisiteRanks: Map<String, Int>,
    val minimumUnspentTalentPoints: Int,
    val previewExpanded: Boolean,
)

data class ValidationScenarioEvidenceStep(
    ...
    val expectedFocusedTalentId: String? = null,
)
```

- 明确 `expected-evidence.json` 必须从 registry-owned setup/evidence 数据生成，不能从 tools-only 表生成。

### P2 - source-agnostic passive model 的物理包归属仍不清楚

文档证据：

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:170` 说新增 source-agnostic passive model。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:258-264` 说 `core` 拥有 typed model 和 resolver 语义。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:631-637` 仍把 Task 1 scope 放在 `core/.../item/ItemModels.kt`。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:660` 仍使用 `core/.../item/PassiveEffectResolver.kt`。

问题：

- "source-agnostic" 和物理位置继续放在 `core.item` 下，语义上有拉扯。
- 留在 `core.item` 可以作为 PR04-01 的过渡选择，但文档没有说明这是有意的 legacy placement。
- 如果移到 `core.passive`，当前 scope 又没有列新增文件和迁移边界。

风险：

- 一种实现会把天赋被动概念继续放进 `core.item`。
- 另一种实现会新建 `core.passive` 并保留 item 兼容包装。
- 最差情况是为了省事同时留下 item/talent 两套小模型，破坏 unified passive authority。

建议修正：

- 二选一写死：
  - **本 PR 迁到中性包：** scope 增加 `core/src/main/kotlin/com/ktome/core/passive/PassiveModels.kt` 和 `PassiveEffectResolver.kt`。
  - **本 PR 暂留 legacy 包：** 明确 `core.item` 只是兼容期物理位置，公共模型仍是 source-agnostic，不能引用 item-only 状态。
- 增加验收规则：不得引入第二套 `TalentPassiveEffect` / `EquipmentPassiveEffect` runtime hierarchy。

### P2 - Canonical Artifact 没覆盖 §7 的全部 required evidence 文件

文档证据：

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:106-124` 是 Canonical Artifact 表。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:930-934` 额外要求了这些 evidence 文件：
  - `evidence/passive-static-panel-entry.png`
  - `evidence/passive-static-after-learn-no-slot-modal.png`
  - `evidence/passive-trigger-panel-entry.png`
  - `evidence/passive-trigger-after-learn-no-slot-modal.png`
  - `evidence/passive-action-log-no-reserve.png`

问题：

- Canonical Artifact 表列了 detail / preview 截图和一个 action suppression 截图，但没有列 §7 的全部 `requiredEvidenceFiles`。
- M09 / M12 / M14 把 detail、passive action、materialization 拆开了，但没有一句话说明 §7 完整 required evidence list 才是文件存在性的 canonical owner。

风险：

- 开发者可能生成了 §7 文件，但自审时只对 Canonical Artifact 表打勾。
- reviewer 只看 canonical 表时会漏掉 panel-entry 或 after-learn evidence。
- 未来 acceptance lint 如果只解析 canonical 表，会把 §7-only 文件当成非阻塞说明。

建议修正：

- 把 §7 的全部 required evidence 文件加入 Canonical Artifact。
- 或给每个 `expected-evidence.json` 加 canonical artifact 行，并声明它生成的 evidence list 是截图/文件存在性的完整真源。
- M14 明确拥有全部 §7 `requiredEvidenceFiles`；M09 / M12 只负责解释某些截图的语义。

### P2 - `app.log` 被现有工具链引用，但 PR04-01 没把它列入 required evidence

文档证据：

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:930-934` 只列截图和 `passive-action-log-no-reserve.png`。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:963` 说 CLI 输出 `expected-evidence.json`。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:971-981` 的 required proof 也都是截图。

当前代码证据：

- 现有场景会把 log 文件放进 `requiredEvidenceFiles`，例如 `ValidationScenarioRegistry.kt:27-35` 和 `ValidationScenarioRegistry.kt:83-90`。
- `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt:277-279` 要求失败时保留 `$evidenceDir/app.log`。
- `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt:315-356` 只把 `scenario.evidence.requiredEvidenceFiles` 写入 `expected-evidence.json`。

问题：

- action suppression 场景想证明没有 reserve/slot command、没有 rollback error text；这类断言只靠截图不够稳，应该有 log 证据或明确的 negative assertion 记录。
- 如果 `app.log` 不在 `requiredEvidenceFiles`，`expected-evidence.json` 不会 gate 它。
- 把截图命名为 `passive-action-log-no-reserve.png` 不能替代真实 log 文件。

建议修正：

- 每个 PR04-01 scenario 增加一个显式 log evidence，例如：
  - `evidence/passive-static-app.log`
  - `evidence/passive-trigger-app.log`
  - `evidence/passive-action-suppression-app.log`
- action suppression 场景补 `requiredLogEventKeys` 或 negative log assertion 策略；如果框架暂不支持 negative assertion，就明确 manual record 必填字段。

### P3 - `callbacks` 是“不存在”还是“空列表”仍有细节歧义

文档证据：

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:295-304` 说 `PASSIVE` must have no `callbacks`。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:618-620` 又说如果 schema 仍需要空列表，则保留 `callbacks: []`。

当前代码证据：

- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt:1754` 通过 optional string list 读取 callbacks。
- 当前 `game/src/main/resources/data/talents/index.yaml` 有大量 `callbacks: []`。

问题：

- "no callbacks" 对 lint 来说有两种含义：key 必须不存在，或 effective list 必须为空。
- 两者会导致不同的 YAML 改动量和不同的 `TalentSchemaTest.passiveTalentsDeclarePassiveEffectsOnly` 实现。

建议修正：

- 推荐明确为：`callbacks` 可以缺省，也可以是显式空列表；`PASSIVE` 禁止非空 callbacks。
- 然后把同一句规则写入 schema/lint 测试口径。

### P3 - unknown passive projection test 与 sealed typed model 的测试 seam 不匹配

文档证据：

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:438` 要求 projection 遇到 unknown passive kind fail fast，并由 `SessionSnapshotMapperTest.passiveDetailSnapshotFailsOnUnknownPassiveKind` 覆盖。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:644-645` 已要求 loader unknown kind fail fast。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:815` 也要求 `SchemaV2LoaderTest.rejectsUnknownTalentPassiveEffectKind`。

问题：

- 如果 `PassiveEffect` 是 sealed typed model，schema loader 又已经拒绝 unknown kind，那么 projection 正常不该收到 unknown kind。
- projection 层要测 unknown，必须引入 test-only fake、故意漏掉某个 known kind，或者保留 raw schema path。文档没有定义这个测试 seam。

风险：

- 开发者可能为了测试加一个 runtime `UNKNOWN` 分支，反而削弱 sealed model。
- 或者用反射/测试 hack 制造未知类型，增加维护成本。
- 或者 loader 和 projection 各写一套 unknown fail-fast，责任不清。

建议修正：

- unknown serialized kind 的 authority 放在 loader。
- projection 测试改成：
  - `SessionSnapshotMapperTest.passiveDetailProjectionHandlesEveryKnownPassiveKind`
  - 或 `SessionSnapshotMapperTest.passiveDetailProjectionFailsWhenKnownKindHasNoTemplateMapping`
- 如果坚持 projection unknown 测试，必须写明测试 seam，并禁止新增 runtime `UNKNOWN` passive kind。

### P3 - ZH_CN 白盒截图与英文/internal expected 文案混在一起

文档证据：

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:898-902` 三个 scenario 的 locale 都是 `ZH_CN`。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:922-926` 写了 `maxHp +10`、`On kill: MANA +3`、`SELF_HAS_STATUS=GUARD`。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:938-951` 的 expected visible result 使用英文说明。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:878-881` 又说 canonical id 会被 localized，typed args 拥有数值和 id。

问题：

- ZH_CN 截图里可见标签应该是中文，但表格混合了内部 key、英文短语和 canonical id。
- 现在不清楚这些字符串是 typed assertion metadata、人工说明，还是截图中应该逐字出现的 visible text。

建议修正：

- first-detail contract 拆成两列：
  - `typed assertion`: `statId=maxHp value=+10`、`resourceType=MANA amount=3`、`condition=SELF_HAS_STATUS statusId=GUARD`
  - `visible assertion`: ZH_CN 文案可见，数值一致
- 或明确表里的英文/internal 字符串只用于 expected-evidence metadata，不是截图逐字验收文本。

### P3 - action suppression 的“无 command / 无 mutation”还缺具体断言面

文档证据：

- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:762-763` 说 PASSIVE 行按 `R` 不发 command，也不改变 reserve draft。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:950-951` 说按 `R` 不打开 modal、不发 reserve/slot command。
- `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md:775` 命名了 `InputHandlerTest.passiveTalentAssignReserveShortcutDoesNotEmitCommand`。

问题：

- 截图能证明没有 modal、没有 `R Reserve` 行。
- `InputHandlerTest` 能证明没有 command。
- 但“没有 reserve draft mutation”到底比较哪些对象仍未说明：`TalentAllocationDraft`、`slotToTalentId`、pending ranks、learned ranks，还是全部。

建议修正：

- 增加一句测试断言：PASSIVE 行按 `R` 后，`TalentAllocationDraft`、`TalentLoadout.slotToTalentId`、learned ranks 均保持不变。
- 如果 `InputHandlerTest` 只能看 command，则把状态 mutation 断言交给 `FoundationGameSessionTest`。

## Round 4 Closure Check

| 上轮问题 | 本轮状态 |
| --- | --- |
| exact CUA input 会 focus 错天赋 | 已通过 focus-by-id / setup 要求解决 |
| deep passive locked/setup 未定义 | 大体解决；剩余问题是 setup 数据落在哪个模型和文件 |
| locale token namespace 漂移 | 已通过 `ui.stat.*` / `ui.hud.*.short` / `ui.inspect.mod.*` 策略解决 |
| UI detail 示例值陈旧 | `Unyielding` current/next 示例已修 |
| `pain_fuel` 测试 slice 冲突 | 已移到 Task A / M03 |
| Task 6 缺 exact-input registry guard | 已补 exact-input 和 focus expected id 测试名 |
| `DamageTypeBonus` 顺序未定义 | 已改为 enum order |
| preview 初始状态未定义 | 已声明 `previewExpanded=false` |

## 系统一致性矩阵

| Area | 复审结果 | 剩余风险 |
| --- | --- | --- |
| 被动玩法目标 | 通过 | 数值和职业身份感已足够支撑类 ToME 被动 pass |
| 装备被动 parity | 基本通过 | unified model 的物理包归属还要固定 |
| Talent schema | 基本通过 | `callbacks` 空列表/缺省规则要精确 |
| Runtime resolver | 基本通过 | `StatModifier` 同名陷阱会导致错误实现 |
| UI passive detail | 基本通过 | unknown-kind projection test 与 localized expected text 要收口 |
| Passive action suppression | 基本通过 | 需要声明无 mutation 的具体状态断言面 |
| Whitebox materialization | 部分通过 | setup/focus data owner 和完整 evidence contract 仍不完整 |
| 文档可发现性 | 通过 | helper refs 和 self-audit 已能从 PR 文档找到 |

## 玩法 / UX 审查

从 Roguelike 系统角度看，这版被动组合已经明显健康。每个覆盖职业都有一个稳定身份型被动和一个条件/触发型 payoff，这符合类 ToME 职业树的节奏：被动不应该只是“小数字”，而应该改变耐久曲线、伤害身份、资源节奏或战斗站位。

当前 UX 风险主要不在设计，而在证据质量。PR04-01 改的是玩家读到什么、以及玩家在 PASSIVE 行上不能做什么。如果白盒证据没有编码 focused talent id、localized visible text、no-modal state 和 no-draft mutation，就可能批准一组只证明“Talent Assign 打开了”的截图，而没有真正证明被动天赋体验。

## 开发前必须补清的问题

1. `kind: StatModifier` 的 Kotlin 类型名是什么，unified passive model 的物理包在哪里。
2. PR04-01 scenario setup 属于 registry model、session options、`FoundationGameSession`、materialization catalog，还是组合。
3. 哪个对象拥有 `expectedFocusedTalentId`，它是否从同一真源输出到 runbook 和 `expected-evidence.json`。
4. `callbacks: []` 对 PASSIVE 是否允许。
5. unknown passive kind 是 loader-only fail fast，还是 projection 也要有明确 missing-template seam。
6. first-detail expected line 是本地化截图文本，还是 typed assertion metadata。
7. PASSIVE 行按 `R` 后，哪些状态对象必须保持不变。

## 推荐修订顺序

1. 修正 `PassiveEffect.StatModifier` 命名/类型合同。
2. Task 6 补 production files，并定义 scenario setup / `expectedFocusedTalentId` 的 owner。
3. 让 Canonical Artifact 或 `expected-evidence.json` 覆盖 §7 全部 required evidence。
4. 增加 PR04-01 log evidence 和 no-command / no-mutation 断言口径。
5. 明确 `callbacks` 缺省与空列表规则。
6. 将 unknown-kind projection 测试改成 known-kind exhaustive / missing-template 测试，除非补明真实 test seam。
7. 白盒表格拆分 typed assertion 与 localized visible assertion。

## Suggested Verification

本轮没有运行 Gradle 或 packaged white-box。原因是本次是文档复审，我只通过仓库源码阅读确认新版 PR 合同是否能映射到当前实现面。

文档修订后建议至少跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

实现落地后建议 owner gates 包含：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :core:test --tests '*PassiveEffectResolver*' :game:test --tests '*Passive*' --tests '*Talent*'
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.ui.talent.DescriptionPresenterTest --tests com.ktome.client.input.InputHandlerTest
./gradlew contractLint localeLint maintainabilityLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-static-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-trigger-passive-detail
./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr04-01-passive-action-suppression
./gradlew verifyChanged
```

Gradle 必须串行执行。

## Summary

新版文档已经关闭上一轮的大部分设计和白盒导航问题，整体接近可实现。但在编码前还需要一次小范围精修：修掉 `StatModifier` 命名陷阱，定义 scenario setup / focus 数据 owner，让 evidence / log artifacts 成为 canonical，并去掉几个测试 seam 歧义。这些都是窄口径文档修正，但能避免实现阶段出现分叉模型和假阳性白盒证据。

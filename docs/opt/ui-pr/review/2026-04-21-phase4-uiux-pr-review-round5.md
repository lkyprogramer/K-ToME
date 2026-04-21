# Phase 4 UI/UX PR 文档深度 Review Round 5

**Review 时间**: 2026-04-21  
**Review 范围**:

1. `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`
2. `docs/opt/ui-pr/README.md`
3. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md`
4. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md`
5. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md`
6. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md`
7. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md`
8. `docs/opt/ui-pr/resource-fallback-audit-template.md`
9. `docs/opt/ui-pr/manual-records/_template.md`
10. 既有 review 报告 round1/round2/round3/round4

**总评**: round4 的 P1/P2 主问题大多已经闭合。`BuildInfo.shortHash`、`ItemRenderSnapshot.specialTierId`、fallback audit 模板、PR-04 keyword gate、PR-05 `missingFactReason` 都已经明显收紧。本轮未发现 P0/P1；剩余问题主要是 PR 文档进入实现前仍会造成执行偏差的 P2/P3 细节。

---

## Findings

### P2

#### 1. 上游源计划仍保留旧 8 PR 的资源文件命名，和当前 5 PR 执行文档冲突

**影响 PR**: PR-03、PR-04、PR-05，资源计划与 `--extra-plan` 接线  
**证据**:

- README 已声明当前目录不机械沿用源计划 8 个执行包，而是合并为 5 个 PR：`README.md:5-13`。
- README 只补了 `BuildInfo.shortHash` 与 `specialTierId` 的上游修订状态：`README.md:15-18`。
- 上游源计划 §6 仍把旧 `PR-04 / PR-05 / PR-06 / PR-07 / PR-08` 分别命名为 `phase4-uiux-pr04` 到 `phase4-uiux-pr08`：`docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:475-486`。
- 上游源计划旧 PR-04 的 image/audio plan 仍建议 `phase4-uiux-pr04-*`：`docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:946-959`，但当前 PR-03 文档要求同一类 item/card/state 资源使用 `phase4-uiux-pr03-*`：`PR-03:91-96`、`PR-03:448-473`。
- 上游源计划旧 PR-06/PR-07/PR-08 仍建议 `phase4-uiux-pr06/07/08-*`：`docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:1127-1129`、`:1203-1205`、`:1347-1349`，但当前 PR-04/PR-05 文档使用 `phase4-uiux-pr04/05-*`。

**问题**: 实施者按 PR 文档会生成 `phase4-uiux-pr03-*` 资源计划；按上游源计划会生成 `phase4-uiux-pr04/05/06/07/08-*`。这会直接影响 `build.gradle.kts --extra-plan`、asset/style/audio/manifest lint 的路径接线，属于可执行 contract 冲突。

**改进建议**:

1. 在上游源计划 §6 后新增“当前 5 PR 执行编号映射”，明确旧 PR 的资源 plan 名称在 `docs/opt/ui-pr` 中被重编号。
2. 或者直接把源计划所有 `assets-src/*/phase4-uiux-pr04~pr08-*` 的执行性路径改成当前 5 PR 名称：
   - 旧 PR-04 + PR-05 -> 当前 `phase4-uiux-pr03-*`
   - 旧 PR-06 + PR-07 -> 当前 `phase4-uiux-pr04-*`
   - 旧 PR-08 -> 当前 `phase4-uiux-pr05-*`
3. README 的“上游源计划修订状态”应补一条：源计划旧资源文件名只作历史拆分参考，执行以本目录每个 PR 的 §7 为准。

#### 2. 多个 PR 声明新增 locale key，但范围没有列出 i18n 文件

**影响 PR**: PR-01、PR-02、PR-04、PR-05  
**证据**:

- PR-01 §4.7 新增首页与 continue reason locale key：`PR-01:268-280`，但 §3.1 范围只列 client Kotlin、Gradle 和 generated resource，没有 `game/src/main/resources/i18n/*`：`PR-01:61-89`。
- PR-02 §4.5 新增 `force-switch.*`、`ui.inspect.empty.tile`、`ui.message.save.blocked-in-validation`：`PR-02:212-220`，但 §3.1 范围没有 i18n 文件：`PR-02:64-83`。
- PR-04 §4.7 新增 status/explain/a11y locale key：`PR-04:252-265`，但 §3.1 范围没有 i18n 文件：`PR-04:64-88`。
- PR-05 §4.4 新增 combat decision locale key：`PR-05:229-236`，但 §3.1 范围没有 i18n 文件：`PR-05:62-82`。
- 仓库正式 locale 文件位于 `game/src/main/resources/i18n/zh-CN.json` 与 `game/src/main/resources/i18n/en-US.json`。

**问题**: 这些 PR 都要求 `localeLint`，但 scope 没有把 locale 真源文件列入“修改”。实现时容易只写 renderer/test 文案断言，漏掉 `zh-CN/en-US` 实际条目，最后靠 lint 报错才返工。PR-03 已正确列出 `game/src/main/resources/i18n/*`，其他 PR 应保持一致。

**改进建议**:

1. PR-01、PR-02、PR-04、PR-05 的 §3.1 `修改` 中补 `game/src/main/resources/i18n/zh-CN.json` 与 `game/src/main/resources/i18n/en-US.json`，或统一写 `game/src/main/resources/i18n/*`。
2. 每个 PR 的 §8 出口门禁补一句：新增 locale key 已进入 `zh-CN/en-US`，并由 `localeLint` 覆盖。
3. PR-01 的首页帮助文案会被 PR-02 truth table 回写，建议 PR-02 scope 额外列出会承载该文案的 `MainMenuScreen` / `MainMenuSummaryModel` / locale key 文件。

#### 3. PR-02 仍要求回写 PR-01 首页帮助区，但 scope 没有包含首页相关文件

**影响 PR**: PR-02、PR-01 help handoff  
**证据**:

- PR-02 硬依赖条款要求“PR-01 首页帮助区中的键位说明必须以本 PR truth table 为准回写”：`PR-02:18-20`。
- PR-02 §3.1 修改面只列 `InputHandler`、`ValidationCommandSource`、`TileRenderer`、`TileRenderModel`、`FoundationGameScreen`：`PR-02:64-69`。
- PR-01 首页帮助 key 是 `ui.menu.help.primary-keys`：`PR-01:280`，首页 help 相关实现面在 PR-01 的 `MainMenuScreen / MainMenuSummaryModel`：`PR-01:64`、`PR-01:72-73`。

**问题**: PR-02 把“回写首页帮助区”列为硬依赖，但没有在范围、测试或出口门禁中承接这个动作。结果 truth table 冻结后，首页帮助文案可能仍停留在 PR-01 旧口径。

**改进建议**:

1. PR-02 §3.1 增加 `MainMenuScreen` / `MainMenuSummaryModel` 或至少 `game/src/main/resources/i18n/*` 的 help key 更新范围。
2. PR-02 §6.1 增加“首页帮助区 key 与 truth table 一致”的必测行为。
3. PR-02 §8 增加“`ui.menu.help.primary-keys` 已按最终 truth table 回写”的出口门禁。

#### 4. PR-03 对 unknown `qualityTierId` 同时写了 `warn artifact` 和 `fail-fast`

**影响 PR**: PR-03，品质/snapshot contract  
**证据**:

- PR-03 排序权重表写 unknown 使用 defensive `-1`，并“必须产生 contract error/warn artifact”：`PR-03:148-155`。
- 下一行又写 unknown `qualityTierId` 是 snapshot/contract 违法，必须 fail-fast：`PR-03:157`。
- 出口门禁也写 unknown `qualityTierId` 和非法 `specialTierId` 已由 contract lint/test fail fast，不走 fallback audit：`PR-03:492`。

**问题**: `warn artifact` 会重新打开软失败路径。对正式品质轴来说，unknown tier 不是视觉 fallback，而是 snapshot/content contract 违法；如果允许 warn，后续实现可能让 renderer defensive 排序后继续展示。

**改进建议**:

把 `PR-03:155` 改成“同时必须产生 contract error artifact，并使 owner lint/test fail fast”。如果确实需要 renderer 防御崩溃，只允许在错误上报之后进入 `UiErrorState` 或 debug-only fallback，不允许作为正常路径继续签收。

#### 5. PR-05 `legalTargetCount: Int` 无法表达“合法目标数量这个 fact 未暴露”

**影响 PR**: PR-05，ActionHintModel typed fact 语义  
**证据**:

- PR-05 允许 `ActionHintModel` 只消费已暴露 typed fact，包括 legal target count：`PR-05:166-172`。
- PR-05 又要求若现有 snapshot 无法提供某字段，则字段保持 `null` 或空集合，并通过 `missingFactReason` 表达未知：`PR-05:195`。
- 但 `ActionHintModel` 的 `legalTargetCount` 是非空 `Int`：`PR-05:183-192`。

**问题**: 对 resource cost/cooldown/range，`null` 或空集合可以表达“未知”；但 legal target count 只能填数字。如果实现填 `0`，会把“规则层未暴露合法目标数量”误显示成“没有合法目标”，正好违反文档自己强调的 unknown-vs-empty 语义。

**改进建议**:

1. 将 `legalTargetCount` 改成 `Int?`，或抽成 `LegalTargetSummary(count: Int?, isKnown: Boolean, missingReason: RenderTextTokenSnapshot?)`。
2. `ActionHintModelBuilderTest` 增加一组：`missing legal target count -> missingFactReason`，与 `known zero legal targets -> no-legal-target feedback` 区分。
3. 人工白盒的 missing fact fixture 也应覆盖 legal target count 缺失，而不仅是 resource cost、cooldown、telegraph linkage。

#### 6. PR-03 扩 `ItemRenderSnapshot.specialTierId`，但 focused test 清单没有点名 snapshot serialization/hash/contract tests

**影响 PR**: PR-03，RenderSnapshot stable contract  
**证据**:

- PR-03 明确新增 `ItemRenderSnapshot.specialTierId`，并同步 render snapshot tests：`PR-03:121-124`。
- PR-03 §3.1 测试清单只列 client UI tests、`FoundationGameSessionTest`、`ItemIconKeyCoverageRuleTest`：`PR-03:97-104`。
- PR-03 §6.2 运行了 broad `:core:test`，但没有把 `RenderSnapshotSerializationTest`、`RenderSnapshotHasherTest`、`RenderSnapshotContractTest` 这类 owner test 写成 selector 或清单项：`PR-03:344-358`。
- 仓库当前存在 `core/src/test/kotlin/com/ktome/core/snapshot/RenderSnapshotSerializationTest.kt`、`core/src/test/kotlin/com/ktome/core/snapshot/RenderSnapshotHasherTest.kt`、`game/src/test/kotlin/com/ktome/game/RenderSnapshotContractTest.kt`。

**问题**: broad `:core:test` 可以兜底，但 PR 文档的 focused test 清单没有把稳定 snapshot contract 变更的 owner tests 钉住。后续实现者可能只更新 UI tests 和 game session test，漏掉 serialization/hash/contract 基线。

**改进建议**:

1. PR-03 §3.1 测试清单增加 `RenderSnapshotSerializationTest`、`RenderSnapshotHasherTest`、`RenderSnapshotContractTest` 或等价 owner tests。
2. PR-03 §6.2 在 `:core:test` 之外补一条 focused selector，或者明确 `:core:test` 必须覆盖上述 snapshot owner tests，且 PR description 要列出更新点。
3. PR-03 §8 增加“snapshot serialization/hash/contract tests 已覆盖 `specialTierId` 的 presence、null、invalid special item case”。

### P3

#### 7. PR-03 special accent 映射表仍使用 `specialTemplateId != null + UNIQUE`，字段来源不够精确

**影响 PR**: PR-03  
**证据**:

- PR-03 字段前提已经要求 special 轴由 `specialTierId` 与 `specialTemplateId` 判断：`PR-03:121-124`。
- 但映射表仍写 `specialTemplateId != null + UNIQUE` / `specialTemplateId != null + ARTIFACT`：`PR-03:128-134`。

**问题**: 表格里的 `UNIQUE / ARTIFACT` 没明确来自 `specialTierId`。虽然前文能推断，但这正是上一轮反复修的易错点：实现者可能再次从 `specialTemplateId` 反查 content template 推断 tier。

**改进建议**:

把两行改成：

```text
specialTemplateId != null && specialTierId == UNIQUE
specialTemplateId != null && specialTierId == ARTIFACT
```

并补一行 `specialTemplateId != null && specialTierId == null`：fail fast，不渲染 accent。

#### 8. PR-04 a11y `JAVA_TOOL_OPTIONS` 命令对已存在 Gradle daemon 不够稳

**影响 PR**: PR-04 人工白盒  
**证据**:

- PR-04 固定 a11y 开关为 JVM `-Dktome.ui.a11y.*`：`PR-04:239-248`。
- 人工白盒建议用 `JAVA_TOOL_OPTIONS="..." ./gradlew :client:run`：`PR-04:356-364`。

**问题**: 如果 Gradle daemon 已经启动，`JAVA_TOOL_OPTIONS` 是否进入 `:client:run` 的应用 JVM 取决于 JavaExec/daemon 环境传递，人工白盒可复现性不如显式 app JVM args。这个问题不一定阻塞设计，但会让 a11y 截图复核偶发“开关没生效”。

**改进建议**:

1. 手工命令改成更稳的 `./gradlew --no-daemon :client:run` 搭配 `JAVA_TOOL_OPTIONS`，或文档指定 `:client:run` 会把 `ktome.ui.a11y.*` project property 转发到 application JVM。
2. 人工记录模板中的 `JVM 参数 / feature flags` 必须记录实际生效入口；PR-04 §6.3 应要求截图或日志证明三项开关读取为 `true`。

#### 9. PR-04 / PR-05 的 §3.1 测试清单与 §6.2 实际命令不完全一致

**影响 PR**: PR-04、PR-05  
**证据**:

- PR-04 §3.1 测试清单只列 5 个新增/专项测试：`PR-04:83-88`，但 §6.2 还运行 `TileRendererCanvasTest`、`DescriptionPresenterTest`、`InputHandlerTest`、`ClientSmokeHarnessTest`、`GoldenScreenshotHarnessTest`：`PR-04:311-315`。
- PR-05 §3.1 测试清单只列 combat/lint 专项测试：`PR-05:78-82`，但 §5.4/§6.2 还要求 `InputHandlerTest`、`TileRendererCanvasTest`、`ClientSmokeHarnessTest`、`GoldenScreenshotHarnessTest`：`PR-05:258-267`、`PR-05:298-301`。

**问题**: 命令本身是对的，但 scope 清单与命令不一致会削弱 checklist 可核对性。评审者如果只看 §3.1，会误以为输入、renderer、smoke、golden 是泛化回归，不是本 PR 的 owner 证据。

**改进建议**:

PR-04/PR-05 的 §3.1 测试清单补齐 §6.2 中所有被当作 owner gate 的测试，或者把 §3.1 改名为“新增专项测试”，再单独列“必须更新/运行的既有 owner tests”。

---

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| 上游源计划和 PR 级执行文档不能出现可执行路径冲突 | `README.md:5-18` 已声明 5 PR 与两处修订；源计划资源文件名仍保留旧 8 PR 编号：`docs/opt/...:948-959`, `:1127-1129`, `:1203-1205`, `:1347-1349` | 部分一致 |
| 新 UI 文案必须走 locale，且由 `localeLint` 覆盖 | 各 PR 都列 locale key 并运行 `localeLint`，但 PR-01/02/04/05 scope 没列 i18n 文件 | 部分一致 |
| `RenderSnapshot` 稳定字段变更必须有 owner tests | PR-03 已列 `RenderSnapshot.kt` 和 broad `:core:test`，但 focused test 清单未点名 serialization/hash/contract tests | 部分一致 |
| unknown typed fact 不得显示为“无成本/无风险/无目标” | PR-05 已加入 `missingFactReason`，但 `legalTargetCount: Int` 不能表达 unknown | 部分一致 |
| 资源 fallback audit 模板必须包含风险和 unblock 字段 | 模板已包含 `失效风险等级`、`补交付 unblock task`、是否开放正式玩家路径 | 一致 |
| PR-04 keyword gate 不允许本 PR 新增面 WARN 放行 | PR-04 已将新增/修改面覆盖不足改为 `BLOCKED`，并要求测试覆盖 | 一致 |

---

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前文档状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| PR 编号与资源计划 | 当前执行为 5 PR，资源 plan 应按当前 PR 命名 | 部分一致 | `README.md:5-18`, 源计划 `:475-486`, `:948-959`, `:1127-1349` | 资源计划路径仍沿用旧 8 PR 编号 | Medium |
| Locale / i18n | 新 UI 文案必须进入正式 `zh-CN/en-US` | 部分一致 | PR-01 `:268-280`, PR-02 `:212-220`, PR-04 `:252-265`, PR-05 `:229-236` | 多数 PR scope 未列 i18n 真源文件 | Medium |
| PR-02 输入/首页 handoff | 首页帮助区要回写 truth table | 部分一致 | PR-02 `:18-20`, `:64-69`; PR-01 `:280` | PR-02 scope 没覆盖首页帮助实现或 locale 文件 | Medium |
| PR-03 item quality/special | special tier 来自 snapshot fact，unknown fail fast | 部分一致 | PR-03 `:121-157`, `:492` | `warn artifact` 与 fail-fast 口径冲突；表格字段来源不够精确 | Medium |
| PR-05 combat hint | unknown fact 必须显式提示 | 部分一致 | PR-05 `:166-195`, `:260-267`, `:289` | `legalTargetCount` 非空 Int 无法表示 unknown | Medium |
| PR-04 a11y | 三项开关可人工复核 | 部分一致 | PR-04 `:239-248`, `:356-364` | `JAVA_TOOL_OPTIONS` 对 warm daemon 不够稳 | Low |

---

## 玩法与体验审查

### 新手体验与信息反馈

PR-02 要求首页帮助区随 truth table 回写，这是正确方向；但当前 scope 没覆盖首页 help 文案承载面。若漏改，玩家第一屏看到的键位说明会和局内真实语义不一致，这是小文案问题，但会直接破坏输入学习路径。

### 奖励驱动与掉落体验

PR-03 已把 special tier 从 client-local 推断改成 snapshot fact，这是正确修复。剩余风险在表格和 unknown 处理：`warn artifact` 与 fail-fast 混用，会让错误品质数据以“防御性排序”继续进入掉落 marker，玩家可能看到错误颜色或错误优先级。

### 战斗体验

PR-05 已补 `missingFactReason`，但 `legalTargetCount: Int` 会把“未知合法目标数”压成某个数字。战斗决策面最怕把未知显示成确定结果；这里需要在模型层保持 unknown-vs-empty 的可表达性。

### 系统耦合与体验断层

资源 plan 编号混用是当前最容易制造返工的执行断层：实现 PR 文档、上游计划和 Gradle `--extra-plan` 可能指向不同文件。这个问题不影响设计方向，但会影响 lint/golden/smoke 的资源消费证据。

---

## 当前阶段必须解决的问题

1. **P2: 资源 plan 编号冲突必须先修。**  
   这是构建接线和 lint 输入路径问题，不能等实现时临场解释。修复方向是源计划补当前 5 PR 的资源命名映射，或统一改成当前 PR 文档路径。

2. **P2: locale scope 缺口必须在 PR-01/02/04/05 中补齐。**  
   新增 UI 文案是当前 UI/UX PR 的核心交付，不列 i18n 真源会让 scope 与 `localeLint` 证据脱节。修复方向是补 `game/src/main/resources/i18n/*` 和出口门禁。

3. **P2: PR-05 `legalTargetCount` unknown 表达必须修。**  
   这是战斗决策面会误导玩家的 typed fact 问题。修复方向是 nullable/summary model，并补 unknown-vs-zero 测试。

4. **P2: PR-03 snapshot owner tests 必须点名。**  
   `specialTierId` 是稳定 `RenderSnapshot` 合同变更，不能只靠 broad `:core:test` 隐式覆盖。修复方向是把 serialization/hash/contract tests 加入测试清单和出口门禁。

---

## Removal/Iteration Plan

### Safe to Remove Now

当前 review 未发现需要立即删除的文档条目。

### Defer Removal / Iteration Required

| Item | Current State | Target State | Owner PR |
| --- | --- | --- | --- |
| 源计划旧 `phase4-uiux-pr06/07/08-*` 资源命名 | 仍保留在上游源计划中 | 补映射或改为当前 5 PR 命名 | 文档整理 / PR-03 前 |
| PR-02 `ui.inspect.empty.tile` | 已有 PR-03 删除门禁 | 实现 PR-03 时实际删除并加入 deprecated-key 断言 | PR-03 |
| PR-02 `COMBAT_DECISION` stub label | 已有 PR-05 清理门禁 | PR-05 重录为 `phase4-uiux-pr05-*` | PR-05 |

---

## Additional Suggestions

1. PR-03 的 `QualityPresentation` 映射表建议把 condition 写成代码式布尔表达式，避免自然语言再次模糊 `specialTierId` 来源。
2. PR-04 a11y 手工验证建议记录一条启动日志，例如 `AccessibilityToggle loaded highContrast=true colorBlindSafe=true reduceMotion=true`，便于证明开关真的进入 runtime。
3. PR-02 truth table 现在比 round4 明确很多，但 `owner-defined` 仍偏宽。后续实现时可以把 passive owner 的 `SHOP / WORLD_MAP / STAT_ASSIGN` 拆成三行，减少 `owner-defined` 解释空间。
4. 如果每份 PR 文档都保留 §3.1 “测试”清单，建议统一分成“新增测试”和“必须更新/运行的既有 owner tests”两栏。

---

## Suggested Verification

本轮只做文档 review，未运行 Gradle。修复本报告后建议做以下文档核对：

```bash
rg -n "phase4-uiux-pr0[6-8]|phase4-uiux-pr04-gemini-plan|phase4-uiux-pr05-gemini-plan" docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md docs/opt/ui-pr
rg -n "新增 locale key|固定 locale key|game/src/main/resources/i18n|zh-CN|en-US" docs/opt/ui-pr/*.md
rg -n "legalTargetCount: Int|missingFactReason|unknown `qualityTierId`|warn artifact|RenderSnapshotSerializationTest|RenderSnapshotHasherTest|RenderSnapshotContractTest" docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md docs/opt/ui-pr/2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md
find docs/opt/ui-pr -name ".DS_Store" -print
```

实现阶段仍按 README 的 per-PR gate 执行：focused tests 只是内循环，PR-close 必须跑 `clientSmoke goldenScreenshot verifyChanged`，涉及 Gradle/processResources/`--extra-plan` 时补 `./scripts/verify-bootstrap.sh`。

---

## Summary

当前文档已经具备进入实现前的主体结构，但仍有 6 个 P2 和 3 个 P3 需要收口。优先修资源 plan 编号、locale scope、PR-05 `legalTargetCount` unknown 表达、PR-03 snapshot owner tests。修完这些后，PR 文档基本可以作为串行开发合同使用。

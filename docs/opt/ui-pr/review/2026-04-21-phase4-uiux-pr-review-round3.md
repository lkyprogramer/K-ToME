# Phase 4 UI/UX PR 级开发文档 Review（第三轮）

**被审对象**: `docs/opt/ui-pr/` 下 `PR-01 ~ PR-05`、`README.md`、`manual-records/_template.md`、`resource-fallback-audit-template.md`  
**审阅日期**: `2026-04-21`  
**审阅范围**: 文档一致性、源计划映射、仓库真源锚点、测试/gate 闭环、跨 PR handoff  
**未审范围**: 未做 Kotlin 实现审查；未运行 Gradle 验证

## Findings

### P0

未发现上一轮意义上的整包丢失或稳定合同直接破坏。`round2` 中的 token 面、`UiErrorPayload` 形态、`ModalStack` fail-fast、priority 公式、`CombatDecisionFrame` 状态机、deferred 清理表、人工白盒模板等 P0 已基本闭合。

### P1

1. **PR-01 的 `BuildInfo.shortHash` 需要构建接线，但范围和验证命令没有包含构建文件与 bootstrap 验证。**  
   证据: PR-01 要求通过 `processResources` 或等价构建步骤注入 git short hash（`PR-01:204`），但 §3.1 范围只列 client Kotlin/测试文件，没有列 `client/build.gradle.kts` 或 root build wiring（`PR-01:61-85`）；§6.2 也没有 `./scripts/verify-bootstrap.sh`（`PR-01:332-337`）。仓库当前 `client/build.gradle.kts` 的 `processResources` 只有 manifest sync 依赖，尚无 hash 注入逻辑。  
   改进: PR-01 §3.1 明确把 `client/build.gradle.kts`、生成资源文件路径或等价 BuildInfo source 纳入范围；§6.2 增加“若改 Gradle/processResources，必须运行 `./scripts/verify-bootstrap.sh`”，并保留 `BuildInfoTest` 对注入与 fallback 的断言。

2. **多份 PR 的自动化命令没有执行自己列出的新增 direct unit tests。**  
   证据: PR-01 测试清单列 `MainMenuFocusPolicyTest / DesktopLauncherTitleFormatterTest / BuildInfoTest`（`PR-01:78-85`），但 §6.2 只运行 `TileRendererCanvasTest / MainMenuControllerTest / MainMenuScreenTextTest / ClientSmokeHarnessTest / GoldenScreenshotHarnessTest`（`PR-01:332-337`）。PR-02 §4.1 要求 `ModalStackTest` 覆盖深度和 pop（`PR-02:113-125`），但 §3.1 与 §6.2 都没列/运行它（`PR-02:77-81`, `PR-02:241-246`）。PR-05 §5.4 列 `CombatDecisionFrameTest / CombatDecisionPanelTest / ActionHintModelBuilderTest / AiIntentLeakRuleTest`（`PR-05:252-261`），但 §6.2 没运行这些 selector（`PR-05:288-293`）。  
   改进: 每个 PR 的 §6.2 必须与 §3.1 / §5.x 测试清单一一对齐；对 pure model/state-machine tests 用 direct selector 明确跑到，避免只靠 smoke/golden 间接覆盖。

3. **PR-03 要展示 `UNIQUE / ARTIFACT` special axis，但当前 `ItemRenderSnapshot` 不暴露 `specialTier`，文档又把“无新增 snapshot 字段”列为出口。**  
   证据: PR-03 要求 `specialTier: SpecialTier?(UNIQUE/ARTIFACT)` + `specialTemplateId` 支撑 special 轴（`PR-03:106-110`），出口又写“没有新增 snapshot 字段”（`PR-03:456-461`）。仓库真源中 `ItemRenderSnapshot` 只有 `specialTemplateId / qualityTierId / qualityNameKey`，没有 `specialTier`；`SpecialItemTemplate` 才持有 `specialTier`。如果 client 通过 `specialTemplateId` 反查 template，会违反文档自己写的“不能由 client 查询 template 自行推断”。  
   改进: 二选一并写死：A. 当前 PR 扩 `ItemRenderSnapshot.specialTierId`，同步 core/game tests、contract lint、snapshot contract 文档；B. 降级 special accent 目标，只显示 `specialTemplateId != null` 的单一 accent，不区分 `UNIQUE / ARTIFACT`。不能同时要求完整 special axis 和“无 snapshot 字段”。

4. **PR-04 引入 core 与 tools gate，但自动化命令没有覆盖 `:core:test` 与 `keywordRegistryLint`。**  
   证据: PR-04 范围修改 `core/talent/KeywordRegistry.kt`、`DescriptionModel.kt`，并新增 `keywordRegistryLint`（`PR-04:64-78`, `PR-04:207-212`）；出口也要求 `keywordRegistryLint` 作为 gate（`PR-04:398-405`）。但 §6.2 只运行 client tests、golden、locale/contract/maintainability（`PR-04:298-303`）。  
   改进: PR-04 §6.2 增加 `./gradlew :core:test`，并明确 `keywordRegistryLint` 的 Gradle 入口。如果它复用 `contractLint`，需要写明是哪一个 `contractLint` 分支和测试类负责；如果是新 task，需同步 root alias、verifyChanged routing 和 scope coverage。

5. **PR-03 / PR-04 如果新增 tools lint，文档没有要求暴露 root Gradle 入口和 verifyChanged 覆盖。**  
   证据: PR-03 可能新增 `ItemIconKeyCoverageRule` / `ContentUiLintRule`（`PR-03:76-84`），PR-04 明确新增 `keywordRegistryLint`（`PR-04:207-212`）。AGENTS 规则要求新的 harness/lint/smoke 尽快暴露 root Gradle 入口；当前各 PR §6.2 只跑现有 `localeLint / contractLint / maintainabilityLint`，没有说明新增 lint 是扩 existing owner 还是新 root task。  
   改进: 对每个 lint 写成三段：owner task、root alias、verifyChanged/ScopeCoverageLint 接线。如果只是扩 `contractLint`，就把具体 rule/test class 写进文档；如果新建 `contentUiLint / keywordRegistryLint`，就必须列 `build.gradle.kts`、`VerificationTaskRegistry`、`ScopeCoverageLintTest` 等接线面。

6. **PR-05 的 contract 扩张路径覆盖不足：如改 `FoundationGameSession`，只跑 `:core:test` 不够。**  
   证据: PR-05 §3.1 允许窄扩 `CombatResolutionTrace.kt` 和 `FoundationGameSession.kt`（`PR-05:72-74`）；§6.2 只在“扩到 combat contract wiring”时补 `:core:test`，没有补 `:game:test`、`FoundationGameSessionTest` 或 snapshot/session contract tests。  
   改进: PR-05 增加条件验证：若触及 `game/FoundationGameSession.kt` 或 snapshot/session mapper，必须运行 `./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"` 以及相关 render snapshot contract tests；若触及 `CombatResolutionTrace`，再补 `:core:test`。

7. **PR 执行纪律缺少 docs/opt 串行开发默认 review gates。**  
   证据: README 只要求每 PR 的自动化、golden、人工白盒完成后进入下一 PR（`README:17-22`），没有把 per-PR `ktome-diff-doc-review`、`ktome-code-review`、最终 `simplify-code-review-cleanup` 写入执行纪律。若后续实施者只按本目录执行，会跳过当前 docs/opt 工作流的 review gate。  
   改进: README 增加“每个 PR 完成后必须跑文档一致性 review 与代码 review，并解决发现后再进入下一 PR；全部 PR 完成后再跑最终三段 review/cleanup”的执行条款。若本 UI/UX 计划故意豁免，需要显式写明豁免理由。

8. **每个 PR 的命令偏向 `:client:test --tests ...`，但没有 PR-close 的 `verifyChanged`。**  
   证据: 仓库根已有 `verifyChanged` 与 `verifyChangedPreflight` root task（`build.gradle.kts:466-475`），AGENTS 也规定 shared PR CI 默认 preflight 是 `./gradlew verifyChanged`。5 份 PR 文档都只列 focused tests/lints/golden，未在出口或 README 中要求 PR-close `verifyChanged`。  
   改进: README 或每份 PR §8 增加：提交/开 PR 前至少运行 `./gradlew verifyChanged`；focused 命令用于开发内循环，不能替代 PR-close impact routing。

### P2

1. **PR-02 可选资源计划缺条件化资源 lint 命令。**  
   PR-02 §7 允许在 Look/Help 场景缺 formal icon/cue 时新增 image/audio plan，但 §6.2 没有条件化补 `assetLint / styleLint / manifestLint / audioLint`。建议补“若 §7 触发资源 plan，则执行资源 lint 与 manifest sync，并更新 build.gradle extra-plan”。

2. **PR-03 的 `QualityPresentation / GroundLootMarkerModel / ModalCardModel / Ui*State` 缺 direct unit test。**  
   当前只列 renderer/smoke/golden（`PR-03:327-337`）。建议新增并运行 `QualityPresentationTest`、`GroundLootMarkerModelTest`、`ModalCardModelTest`、`UiErrorPayloadTest`、`UiLoadingStateTest`，把排序、payload 顺序和状态模型行为从截图测试中抽出来。

3. **PR-04 的 `StatusPresentationModel / TelegraphPresentationModel / ExplainPaneModel / AccessibilityToggle` 缺 direct model tests。**  
   文档要求 priority、a11y 组合、ExplainPane composition，但 §6.2 只跑现有 renderer/presenter/input tests。建议新增并运行对应 model/builder tests，避免把 pure 规则塞进 golden。

4. **`clientSmoke` / `goldenScreenshot` 官方 root alias 没被文档使用。**  
   仓库根已有 `clientSmoke` 与 `goldenScreenshot` alias（`build.gradle.kts:262-265`, `build.gradle.kts:752-755`），client 子任务也设置了 tag 与 smoke report dir（`client/build.gradle.kts:310-329`）。当前 PR 文档用 `:client:test --tests "ClientSmokeHarnessTest"` 和 `:client:test --tests "GoldenScreenshotHarnessTest"`。建议改为开发期可用 selector，出口 gate 必须用 root alias，保证 report、tag、CI 路由口径一致。

5. **人工白盒模板已有，但 PR 文档没有要求“失败时留 trace/hash/输入脚本”。**  
   README 要求记录路径和 skipped golden 补证据（`README:45-49`），模板也给证据字段；但各 PR §6.3 的失败留证口径不统一。建议把 AGENTS 的失败留痕规则下沉到 README：失败必须保留输入序列、截图/录屏、golden hash 或 smoke artifact，不允许只写“未通过”。

### P3

1. `follow-ups.md` 只有空表，建议补一行“当前无 deferred follow-up；禁止把 P0/P1 出口门禁降级到本文件”的显式状态，避免后续有人直接填空白行。
2. PR-04 与 PR-05 共用 `BOSS_VARIANT / 20260412`，当前可接受，但人工记录文件名应强制区分 status/readability 与 combat-decision 证据，避免 PR-05 重录后 PR-04 的截图 hash 被误认为仍有效。

## Requirement Alignment

| 要求 | 证据 | 结论 |
| --- | --- | --- |
| 5 份 PR 覆盖源计划 8 个执行包 | README 合并表保留原 `PR-01+02`、`PR-04+05`、`PR-06+07`、`PR-08` 的映射 | 一致 |
| 不引入第二套 `RenderSnapshot / manifest / telegraph / keyword` 权威 | 各 PR 已多处写明 client-local、typed fact、KeywordRegistry authority、TelegraphPresentation 投影关系 | 部分一致：PR-03 specialTier 与当前 snapshot 不匹配，PR-04 lint gate 接线未闭合 |
| 每个 PR 有自动化、golden、人工白盒 | README 与各 PR §6/§8 均有相应章节 | 部分一致：自动化命令漏 direct tests、root aliases、verifyChanged 和新增 lint task |
| 资源计划走现有 pipeline，不新增第二素材流程 | 主计划与各 PR §7 已写 plan/report/extra-plan/lint 约束 | 部分一致：PR-02 optional resource path 缺条件化 lint 命令 |

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前文档状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| PR-01 BuildInfo / title | PR-01 前置唯一 build hash 来源，供错误复制和标题使用 | 部分一致 | `PR-01:198-207`, `PR-01:332-337` | 需要构建接线但未列 build 文件和 bootstrap 验证 | High |
| PR-02 ModalStack / focus | 冻结 modal stack、deferred frame、输入语义 | 部分一致 | `PR-02:70-81`, `PR-02:113-125`, `PR-02:241-246` | `ModalStackTest / PaneFocusControllerTest` 没进入测试范围和命令 | High |
| PR-03 Item / special axis | icon/quality/special/card/state 全链路 | 部分一致 | `PR-03:106-110`, `RenderSnapshot.kt:464-477`, `ItemModels.kt:243-252` | `specialTier` 不在 snapshot，文档又禁止 client 反查和新增 snapshot | High |
| PR-03 Content UI lint | 优先扩现有 owner，必要时新增 lint | 部分一致 | `PR-03:256-267`, `PR-03:327-345` | 新增 lint 时缺 root alias / verifyChanged / specific command | High |
| PR-04 Status / keyword | 状态 presentation、keyword authority、ExplainPane | 部分一致 | `PR-04:64-78`, `PR-04:207-212`, `PR-04:398-405` | core/tools 变更缺 `:core:test` 和 `keywordRegistryLint` 执行入口 | High |
| PR-05 Combat decision | Telegraph 三位一体、单 frame 三 phase | 部分一致 | `PR-05:252-293`, `PR-05:411-421` | direct tests 已列但命令没跑；contract 扩张缺 game/session 验证 | High |
| README 执行纪律 | 串行推进、golden、人工白盒、maintainability | 部分一致 | `README:17-23` | 缺 per-PR review skill gates 与 PR-close `verifyChanged` | Medium |

## 每个 PR 的不足与改进意见

### PR-01 · Client Foundation and Main Menu

不足:

- `BuildInfo.shortHash` 实现需要 build/processResources 接线，但范围和命令没有包含构建文件、生成资源和 bootstrap 验证。
- §6.2 没运行 `MainMenuFocusPolicyTest`、`DesktopLauncherTitleFormatterTest`、`BuildInfoTest`，与 §3.1 测试清单不一致。
- `clientSmoke / goldenScreenshot` 出口更适合用 root alias，当前 selector 命令可以作为开发内循环，但不应作为最终 gate。

改进:

- §3.1 加 `client/build.gradle.kts`、BuildInfo resource/template 路径；§6.2 增加 `BuildInfoTest / DesktopLauncherTitleFormatterTest / MainMenuFocusPolicyTest` selector。
- 若改 Gradle，增加 `./scripts/verify-bootstrap.sh`；PR-close 增加 `./gradlew verifyChanged`。
- 把 focused selector 与 root alias 分开写：开发内循环用 selector，出口 gate 用 `clientSmoke / goldenScreenshot`。

### PR-02 · In-Game Info, Input, Modal, Look

不足:

- `ModalStackTest` 被 §4.1 指名为必须，但 §3.1 和 §6.2 没列。
- `PaneFocusController` 是新增核心输入模型，缺 direct unit test。
- 可选 image/audio plan 缺条件化资源 lint 命令。

改进:

- §3.1 测试补 `ModalStackTest`、`PaneFocusControllerTest`；§6.2 直接运行。
- §7/§8 补“若新增资源 plan，则必须运行 `assetLint styleLint manifestLint` 或 `audioLint manifestLint`，并接 `build.gradle.kts --extra-plan`”。
- 出口 gate 补 root `clientSmoke / goldenScreenshot` 与 PR-close `verifyChanged`。

### PR-03 · Item, Content Presentation, UI States

不足:

- special axis 与当前 snapshot 合同冲突：`SpecialItemTemplate.specialTier` 存在于内容模板，但 `ItemRenderSnapshot` 不暴露；文档又禁止 client 反查 template，并把“无新增 snapshot 字段”列为出口。
- 多个新增 pure presentation/state model 没有 direct tests。
- 可能新增 `ContentUiLintRule`，但没有对应 Gradle 入口、verifyChanged 接线和执行命令。

改进:

- 明确选择：扩 snapshot 暴露 `specialTierId`，或降级为单一 special accent。若扩 snapshot，PR-03 需要把 core/game contract、contract lint、snapshot tests 纳入范围和验证。
- 补 `QualityPresentationTest / GroundLootMarkerModelTest / ModalCardModelTest / UiErrorPayloadTest / UiLoadingStateTest`。
- 若新增 `contentUiLint`，新增 root alias 和 impact routing；若复用 `contractLint`，写明具体 rule/test class。

### PR-04 · Status, Description, Readability

不足:

- 修改 `core/talent` 且新增 `keywordRegistryLint`，但 §6.2 没有 `:core:test` 和 `keywordRegistryLint`。
- `StatusPresentationModel / TelegraphPresentationModel / ExplainPaneModel / AccessibilityToggle` 缺 direct tests。
- `keywordRegistryLint` 的 task 归属和 root alias 未写，出口 gate 无法执行。

改进:

- §6.2 加 `./gradlew :core:test`，并明确 `keywordRegistryLint` 是新 root task还是 `contractLint` 子规则。
- 补 `StatusPresentationModelTest / TelegraphPresentationModelTest / ExplainPaneModelTest / AccessibilityToggleTest`。
- 若新增 tools lint，按 AGENTS 补 root alias、verifyChanged routing、scope coverage test。

### PR-05 · Telegraph and Combat Decision Surface

不足:

- §5.4 已列 direct tests，但 §6.2 没跑这些 direct tests。
- 若窄扩 `FoundationGameSession`，只补 `:core:test` 不足以证明 game/session/snapshot path 没漂移。
- `ActionHintModel` 允许字段为 `null`/空集合，但没有要求 UI 区分“规则层未暴露”与“合法但为空”的展示状态。

改进:

- §6.2 补 `CombatDecisionFrameTest / CombatDecisionPanelTest / ActionHintModelBuilderTest / AiIntentLeakRuleTest` selector。
- 若触及 `FoundationGameSession`，补 `:game:test --tests "com.ktome.game.FoundationGameSessionTest"` 与 render snapshot contract tests；若触及 `CombatResolutionTrace`，再补 `:core:test`。
- `ActionHintModel` 增加 `missingFactReason` 或 explicit display rule，避免 UI 把“未知”显示成“没有成本/没有冷却/没有风险”。

## 当前阶段必须解决的问题

1. **先修验证命令与测试清单不一致。**  
   这是当前 Phase 必须修，因为 PR 文档是后续实现的执行合同；如果 direct tests 没被命令跑到，后续 PR 会出现“文档说有测试，实际 gate 没跑”的假绿。

2. **先解决 PR-03 specialTier snapshot 合同。**  
   不能推迟到实现中临场决定，因为这决定是否触碰 `RenderSnapshot` 稳定合同；一旦实现者为了省事在 client 反查 template，就会引入第二真源。

3. **先把新增 lint 的 owner / Gradle / verifyChanged 接线写清。**  
   不能只写“新增 lint”，否则 tools gate 很容易成为本地孤岛，无法进入 root alias 和共享 CI。

4. **先把 docs/opt review gates 与 PR-close `verifyChanged` 加入 README。**  
   这不是实现细节，而是这组 PR 的执行纪律。文档现在只约束自动化/golden/人工白盒，少了最终 review/impact routing 闭环。

## Removal/Iteration Plan

当前 review 未发现必须删除的 PR 文档或整包拆分。建议保留 5 PR 合并结构。

需要迭代收口:

| 条目 | 位置 | 处理 |
| --- | --- | --- |
| `follow-ups.md` 空白占位 | `docs/opt/ui-pr/follow-ups.md` | 增加“当前无 deferred follow-up”说明；任何新增条目必须写 owner、关闭 PR、验证方式 |
| focused selector gate | 各 PR §6.2 | 保留为开发内循环，但增加 root alias 和 `verifyChanged` 作为出口 gate |
| optional resource plan | PR-02/04/05 §7 | 加条件化资源 lint 命令和 fallback audit 引用 |

## Additional Suggestions

- README 可以增加一张“每个 PR 必跑命令总览”，把 focused tests、root alias、资源条件、review gates 分列，避免每份 PR 重复维护时继续漂移。
- 手工白盒记录模板可以加“失败留痕”小节：失败时必须写输入序列、截图/录屏、hash、日志来源；这比散落在每份 PR §6.3 更稳定。
- PR-03 的 `ItemIconKeyCoverageRule` 可以优先落在现有 `contractLint` 下，除非确实需要跨 module presentation model 校验；这样比新增 `contentUiLint` 更符合 owner 收敛。

## Suggested Verification

本轮 review 未运行 Gradle；建议文档修订后执行以下文档级核对:

```bash
rg -n "BuildInfoTest|DesktopLauncherTitleFormatterTest|MainMenuFocusPolicyTest|ModalStackTest|PaneFocusControllerTest|QualityPresentationTest|GroundLootMarkerModelTest|UiErrorPayloadTest|StatusPresentationModelTest|CombatDecisionFrameTest|keywordRegistryLint|verifyChanged" docs/opt/ui-pr
rg -n "specialTier" core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md
```

实现阶段每个 PR 的出口建议至少包含:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew clientSmoke goldenScreenshot
./gradlew verifyChanged
```

再按每个 PR 的实际修改面补 focused tests、`localeLint / contractLint / maintainabilityLint`、资源 lints、`./scripts/verify-bootstrap.sh`、`:core:test` 或 `:game:test`。

## Summary

第三轮结论: 当前 5 份 PR 文档已经能表达目标体验和跨 PR handoff，上一轮大部分结构性问题已修。剩余高风险主要集中在 **文档执行闭环**，不是设计方向本身：测试清单与命令不一致、新 lint 没有 root/verifyChanged 接线、PR-03 specialTier 与 snapshot 合同冲突、PR-01 BuildInfo 需要 build wiring 但未列 scope。先修这些，文档才适合直接进入 PR-01 开发。

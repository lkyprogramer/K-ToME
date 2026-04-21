# Phase 4 UI/UX PR 文档深度 Review Round 4

**Review 时间**: 2026-04-21  
**Review 范围**:

1. `docs/opt/ui-pr/README.md`
2. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md`
3. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md`
4. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md`
5. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md`
6. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md`
7. `docs/opt/ui-pr/resource-fallback-audit-template.md`
8. `docs/opt/ui-pr/manual-records/_template.md`
9. 上游源计划 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`
10. 既有 review 报告 round1/round2/round3

**总评**: round3 指出的多数硬缺口已经被补齐，尤其是 PR-01 `BuildInfo.shortHash` 前置、focused test selector 覆盖、PR-04 `keywordRegistryLint` 命令、PR-05 `FoundationGameSession` 条件验证都有明显收口。当前仍不建议直接进入 PR-01 开发，原因不是方向错误，而是还有几处会导致实现者按不同权威执行的文档冲突，以及少数 gate 语义仍可被绕过。

---

## 1. Findings

### P1-1 上游计划仍把 `BuildInfo.shortHash` 归到旧 PR，和当前 PR-01/README 冲突

**影响 PR**: PR-01、PR-03、跨 PR 执行顺序  
**证据**:

- `docs/opt/ui-pr/README.md:46` 已声明 `BuildInfo.shortHash` 由 `PR-01` 引入并在 `PR-01` 收口。
- `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md:35`、`:201-215` 也明确 PR-01 直接落地 `BuildInfo.shortHash`、注入和 fallback warn。
- 但上游计划 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:380` 仍写 `<build-hash>` 由 `PR-05` 引入。
- 同一上游计划 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:1018-1026` 又把 `BuildInfo.kt` 放在旧 `PR-05` 的共享卡片/错误态章节里。

**问题**: PR 级文档已经修成“PR-01 是唯一来源”，但执行前置仍要求完整阅读上游计划。如果不修上游，实施者可能按旧计划把 BuildInfo 留到错误态 PR，再与 PR-01/PR-03 的 payload 需求冲突。

**改进建议**:

1. 修上游源计划，把 `BuildInfo.shortHash` 所有权从旧 `PR-05` 前移到当前 `phase4-uiux-pr01`。
2. 在上游计划 §5.4 和旧 §11.3 同步写明：后续共享卡片/错误态只消费 `PR-01 BuildInfo.shortHash`，不得再引入第二 hash reader。
3. 在 README 的合并来源表后补一句：源计划中旧 PR 编号涉及 BuildInfo 的描述已由 PR-01 覆盖，以上游修订后的文本为准。

### P1-2 PR-03 改为扩 `ItemRenderSnapshot.specialTierId`，但上游计划仍禁止本 PR 新增 snapshot 字段

**影响 PR**: PR-03，且触碰稳定 snapshot contract  
**证据**:

- PR-03 当前写明仓库真源没有 `specialTier`，本 PR 显式新增 `ItemRenderSnapshot.specialTierId`：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md:60`、`:117-123`、`:485`。
- 上游计划当前状态只列 `ItemRenderSnapshot` 已有 `iconKey / qualityTierId / qualityNameKey`：`docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:37-41`。
- 上游计划 §10.3 仍写 `specialTemplateId != null` 的 special item 允许 client-local accent，但“不在本 PR 发明新的 snapshot 字段”：`docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:901-905`。

**问题**: round3 建议已经迫使 PR-03 选择了“扩 snapshot”路径，这是合理的，但上游计划还停在“禁止新增 snapshot 字段”的旧路径。`RenderSnapshot` 是稳定 client 输入 contract，不能让 PR 文档和源计划同时成立。

**改进建议**:

1. 修上游计划 §10.3，明确当前最终选择是扩 `ItemRenderSnapshot.specialTierId`，不是 client 通过 `specialTemplateId` 反查模板。
2. 在 PR-03 §3.1 范围中追加需要同步的 contract 文档路径，例如 `docs/2026-03-13-core-systems-design-and-phase-supplements.md` 或 phase4 checklist 中对应 snapshot/contract 说明。
3. 在 PR-03 §6.2 之后补“snapshot contract 变更验证”：至少要求 `RenderSnapshotSerializationTest`、`RenderSnapshotHasherTest` 或等价 snapshot hash/golden owner test 明确覆盖 `specialTierId` 的存在、缺省和兼容失败路径。
4. 若决定不修上游，则 PR-03 必须退回 round3 的备选方案 B：只显示单一 special accent，不区分 `UNIQUE / ARTIFACT`。

### P2-1 `Resource Fallback Audit` 模板字段和上游强制列不一致

**影响 PR**: PR-02、PR-03、PR-04、PR-05，所有触发资源 fallback 的 PR  
**证据**:

- 上游计划要求 fallback 小表列为 `key / fallback-visualKey 或 fallback-audioCueId / 失效风险等级（low/medium/high）/ 补交付 unblock task`：`docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:1478-1483`。
- 当前模板列为 `asset key / 请求面 / fallback 类型 / fallback 行为 / 迁移计划 / 预期正式 PR`：`docs/opt/ui-pr/resource-fallback-audit-template.md:5-7`。

**问题**: 当前模板能描述 fallback 行为，但缺少上游要求的风险等级和 unblock task，且字段名没有 `fallback-visualKey / fallback-audioCueId`。PR description 即使完整填写当前模板，也不满足源计划的强制字段。

**改进建议**:

把模板改为至少包含以下列：

```markdown
| key | 请求面 | fallback-visualKey / fallback-audioCueId | fallback 行为 | 失效风险等级 | 补交付 unblock task | 关闭 PR / owner |
| --- | --- | --- | --- | --- | --- | --- |
```

并在模板约束中写明：`失效风险等级=high` 时不得开放依赖该 key 的正式玩家路径，除非同 PR 已有人工白盒和 smoke/golden 替代证据。

### P2-2 PR-02 输入 truth table 仍偏“按键覆盖清单”，不是完整行为真值表

**影响 PR**: PR-02，后续 PR-04/PR-05 依赖输入语义  
**证据**:

- PR-02 冻结了 `VALIDATION` 独立且 `I / J / K / L` 不再作为 validation inspect cursor：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md:57`。
- PR-02 §4.3 只是按 mode 列出需要覆盖的按键集合：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md:146-159`。
- §4.3 只有 `Ctrl+S` 规则写了明确期望：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md:161-166`。

**问题**: 对输入 owner 来说，“覆盖了某个键”不等于“冻结了该键的行为”。比如 `VALIDATION + I/J/K/L` 到底断言 no-op、转发给 validation command，还是保留旧行为，目前要靠读上下文推断。后续 PR-04/PR-05 再改 `Backspace / ? / TARGETING` 时，容易出现测试名覆盖了按键但断言语义漂移的情况。

**改进建议**:

1. 把 §4.3 改成 mode x key x expected result 的矩阵，至少覆盖 `mode remains / mode changes / stack changes / command emitted / toast/log / save allowed` 六类结果。
2. 对 `VALIDATION + I/J/K/L` 显式写为“不移动 validation inspect cursor，且不打开玩家 Look/Inspect”或实际目标行为。
3. 对 `TARGETING` 行补“PR-05 后该行只作为兼容壳，最终断言迁移到 `CombatDecisionFrame.TARGET`”的临时状态和删除条件。

### P2-3 PR-03 把 unknown `qualityTierId` 走 fallback audit，混淆了 contract 错误和资源 fallback

**影响 PR**: PR-03  
**证据**:

- PR-03 明确正式品质轴只有 `NORMAL / MAGIC / RARE` 和 special accent：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md:53-60`。
- `GroundLootMarkerModel` 对 unknown `qualityTierId` 定义为权重 `-1`，并“走 fallback audit”：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md:146-154`。
- fallback 模板本身是资源缺失场景模板，不是 snapshot enum 违法模板：`docs/opt/ui-pr/resource-fallback-audit-template.md:3-14`。

**问题**: unknown `qualityTierId` 是 snapshot/contract 违法或上游数据污染，不是“图标缺失”这种资源 fallback。把它交给 fallback audit 会降低 fail-fast 强度，也可能把错误渲染成低优先级普通物品。

**改进建议**:

1. `QualityPresentation` 对 unknown `qualityTierId` 应 fail fast 到 contract lint/test，或进入显式 `UiErrorState`/debug error，不应作为资源 fallback。
2. `GroundLootMarkerModel` 排序可保留 defensive fallback，但测试必须断言同时产生 contract error/warn artifact，不能静默按 `-1` 排序通过。
3. `ItemIconKeyCoverageRule` 或 `ContentUiLintRule` 增加品质 enum 合法性检查，覆盖 `qualityTierId` 和 `specialTierId`。

### P2-4 PR-04 `keywordRegistryLint` 的 WARN 退化路径会削弱必选 gate

**影响 PR**: PR-04  
**证据**:

- PR-04 声明只要 `DescriptionPresenter` 继续消费 keyword id，`keywordRegistryLint` 就是本 PR 必选 gate：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md:223`。
- 同一节允许 tools 侧无法符号扫描时退化为源码/fixture 扫描，覆盖不足时输出 WARN，并登记 follow-up：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md:225-229`。
- `follow-ups.md` 又写当前无 deferred follow-up，且禁止把 P0/P1 出口门禁降级到 follow-up：`docs/opt/ui-pr/follow-ups.md:3-5`。

**问题**: `keywordRegistryLint` 既是必选 gate，又允许 coverage 不足只 WARN 并登记 follow-up，这会让“唯一 keyword authority”变成可绕过的软门禁。未知 keyword fail fast 只覆盖一半问题，覆盖不足本身也应阻塞当前 PR。

**改进建议**:

1. 将“扫描覆盖面不足”从 WARN 改为 BLOCKED，除非该不足明确不影响本 PR 新增/修改的 keyword 消费面。
2. 如果必须保留 WARN，只允许针对非本 PR 修改的历史 keyword 面，并要求 follow-up 写 owner、关闭 PR、验证方式和不阻塞理由。
3. 在 §6.2 加一条验收条件：`KeywordRegistryLintTest` 必须包含“覆盖面不足时失败或产出 BLOCKED artifact”的用例。

### P2-5 PR-04 a11y 人工白盒流程没有给出实际带启动参数的运行方式

**影响 PR**: PR-04  
**证据**:

- PR-04 固定三个启动参数：`-Dktome.ui.a11y.highContrast=true`、`-Dktome.ui.a11y.colorBlindSafe=true`、`-Dktome.ui.a11y.reduceMotion=true`：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md:241-247`。
- PR-04 人工白盒启动命令仍是普通 `./gradlew :client:run`：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md:346-352`。
- a11y 流程只写“打开高对比/色盲回退/Reduce Motion 或等价开发开关”：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md:362-364`。

**问题**: 如果实现选择启动参数而不是正式设置页，当前人工流程没有告诉实施者如何把 `-D` 传进 Gradle run。结果是 a11y 可能只被单测覆盖，人工白盒记录无法复现。

**改进建议**:

在 §6.3 增加一个明确命令，例如：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:run \
  -Dktome.ui.a11y.highContrast=true \
  -Dktome.ui.a11y.colorBlindSafe=true \
  -Dktome.ui.a11y.reduceMotion=true
```

如果 Gradle `run` 不透传 JVM system properties，则文档必须写实际采用的传参方式，例如 `JAVA_TOOL_OPTIONS`、`applicationDefaultJvmArgs` 或 debug settings 面入口。

### P2-6 PR-05 `missingFactReason` 已建模，但缺少明确的测试和白盒场景约束

**影响 PR**: PR-05  
**证据**:

- PR-05 `ActionHintModel` 已增加 `missingFactReason`，并要求 unknown typed fact 不得渲染成“没有成本/没有冷却/没有风险”：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md:164-195`。
- 自动化命令运行 `ActionHintModelBuilderTest`，但文档没有列出该测试必须覆盖的 unknown-vs-empty 用例：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md:297-299`。
- 人工白盒边界场景覆盖禁用、无合法目标、非法目标、单一方式、Ctrl+S，但没有覆盖“规则层暂未暴露该信息”的 UI 展示：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md:362-368`。

**问题**: 文档已经解决了模型层“未知不等于无”的问题，但没有把它钉进 test matrix 和人工证据。实现者可能保留字段，却只测 happy path，最终 UI 仍可能把缺失 cooldown/cost/risk 显示成空白。

**改进建议**:

1. 在 PR-05 §6.1 checklist 增加：缺失 resource cost、cooldown、telegraph linkage 三类 typed fact 时，`missingFactReason` 可见，且不会显示“0 cost / ready / no risk”。
2. 在 §6.2 写明 `ActionHintModelBuilderTest` 必须覆盖 `null fact -> missingFactReason`、`empty legal target -> no-legal-target feedback`、`real zero cost -> 不显示 missingFactReason` 三组差异。
3. 在 §6.3 增加一个人工场景：使用缺少某个 typed hint 的 fixture 或 debug preset，记录面板上“规则层暂未暴露该信息”的展示。

### P3-1 PR-01 声明最小支持 `1024x768`，但自动化和人工流程只覆盖 `1280x800`

**影响 PR**: PR-01  
**证据**:

- PR-01 `InfoSurfaceLayout.MapDominant` 声明最小支持窗口尺寸为 `1024x768`：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md:144-150`。
- PR-01 人工白盒固定窗口尺寸只有 `1280x800`：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md:355-359`。
- PR-01 通过标准只写主要文本不爆版，但没有点名 `1024x768`：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md:397-402`。

**问题**: `1024x768` 是可见布局 contract，不应只作为设计声明存在。否则 PR-01 可能在标准 golden 分辨率通过，但最小支持尺寸爆版。

**改进建议**:

1. 增加 `InfoSurfaceLayoutTest` 或 `TileRendererCanvasTest` 的 `1024x768` case，至少断言 map/sidebar/bottom cards 不重叠。
2. 人工白盒增加一条 `1024x768` 快速复核，记录首页三态和局内 MapDominant 布局。
3. 如果当前阶段只承诺 `1280x800`，则把 `1024x768` 改为后续目标，不要写成“最小支持”。

### P3-2 PR-03 删除 `ui.inspect.empty.tile` 的要求没有进入出口门禁和 deprecated-key 断言

**影响 PR**: PR-03，承接 PR-02 临时 key 清理  
**证据**:

- README 明确 `PR-03` 合入时删除 `ui.inspect.empty.tile` 的 `zh-CN/en-US` 条目，并列入 locale deprecated-key 断言：`docs/opt/ui-pr/README.md:47`。
- PR-03 硬依赖条款写了必须迁移到 `UiEmptyState` 并删除临时 key：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md:19-21`。
- 但 PR-03 出口门禁没有提 deprecated-key 断言或 locale 删除验证：`docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md:478-485`。

**问题**: 顶部硬依赖容易被读到，但实施时通常按出口门禁收口。没有把删除临时 key 写入 §8，PR-03 可能完成 `UiEmptyState` 迁移却留下废弃 locale key。

**改进建议**:

在 PR-03 §8 增加一条：`ui.inspect.empty.tile` 已从 `zh-CN/en-US` 删除，并由 locale deprecated-key 断言或等价 `localeLint` case 防止回归。

### P3-3 `docs/opt/ui-pr/.DS_Store` 已存在，虽然被 `.gitignore` 忽略，但会污染文档目录审查

**影响 PR**: 目录卫生  
**证据**:

- 本地存在 `docs/opt/ui-pr/.DS_Store`。
- `.gitignore:2` 已忽略 `.DS_Store`。

**问题**: 不会进入 git，但会干扰 `find docs/opt/ui-pr`、人工打包、外部审查和目录 diff 视图。

**改进建议**:

在提交前删除本地 `docs/opt/ui-pr/.DS_Store`，并保持 `.gitignore` 现有规则即可。

---

## 2. Requirement Alignment

| 需求 / 约束 | 当前状态 | 风险 | 建议 |
| --- | --- | --- | --- |
| 每个 PR 都有 focused tests + root gate | 已显著改善；PR-01/02/03/04/05 都列了 focused tests 和 `clientSmoke goldenScreenshot verifyChanged` | 低 | 保持现状 |
| 新 lint 要有 owner/root/verifyChanged 接线 | PR-03/PR-04 已写 owner 与接线策略 | 中 | 修 PR-04 WARN 退化语义，避免必选 gate 软化 |
| `RenderSnapshot` 是 client 唯一正式输入 | PR-03 选择扩 `specialTierId`，方向合理 | 高 | 同步上游计划和 snapshot contract 文档，补 hash/serialization owner 验证 |
| fallback 不能变成第二真源 | 各 PR 都要求 Resource Fallback Audit | 中 | 修模板字段，区分资源 fallback 与 contract invalid |
| 人工白盒不能被 skipped golden 替代 | README 和各 PR 均已强调 | 中 | PR-04 a11y、PR-05 missing fact 需要更可执行的手工场景 |
| 跨 PR deferred 必须可追踪 | README 已有 deferred 表 | 中 | PR-03 把 `ui.inspect.empty.tile` 清理补进出口门禁 |

---

## 3. 功能 / 系统一致性矩阵

| PR | 当前一致性 | 主要不足 | 严重度 |
| --- | --- | --- | --- |
| PR-01 Client Foundation and Main Menu | 基础设施、BuildInfo、首页三态和 root gate 已补齐 | 上游源计划仍把 BuildInfo 放在旧 PR；`1024x768` 没验证 | P1 / P3 |
| PR-02 In-Game Info, Input, Modal, Look | ModalStack、PaneFocus、Look Mode、deferred stub 都有范围和测试 | 输入 truth table 仍不是完整行为矩阵 | P2 |
| PR-03 Item, Content Presentation, UI States | special axis 已明确选择扩 snapshot，测试命令也覆盖 core/game/tools/client | 上游仍禁止新增 snapshot 字段；资源 fallback 模板不一致；unknown quality 处理过软；临时 locale key 清理未进出口 | P1 / P2 / P3 |
| PR-04 Status, Description, Readability | core/test/tools gate 已补，keyword lint 从可选变成 gate | `keywordRegistryLint` 允许 WARN 退化；a11y 手工命令无法保证打开开关 | P2 |
| PR-05 Telegraph and Combat Decision Surface | combat frame、action/method/target、AI intent 禁止、game 条件验证已补齐 | `missingFactReason` 没进入明确 test/manual 场景 | P2 |

---

## 4. 玩法与体验审查

1. 当前 5 个 PR 的玩家路径切分整体合理：PR-01 首页与布局基座，PR-02 输入和 Look owner，PR-03 内容表现和状态模型，PR-04 可读性与解释入口，PR-05 战斗决策面。
2. 最大体验风险不再是“缺一个大功能”，而是 UI 对未知/缺失事实的表达。PR-03 unknown quality 和 PR-05 missing typed fact 都必须避免把“规则层没暴露”显示成“没有风险/没有成本/普通物品”。
3. a11y 已被纳入 PR-04，但如果没有可执行启动路径，人工白盒会退化成主观目测。PR-04 必须把高对比、色盲安全、减少动态效果变成可复现输入。
4. `1024x768` 最小窗口如果保留为 PR-01 contract，就需要真实视觉证据。否则玩家路径可能在标准 golden 下稳定，小窗却不可玩。

---

## 5. 当前阶段必须解决的问题

进入 PR-01 开发前建议先修以下文档问题：

1. **P1**: 修上游计划的 `BuildInfo.shortHash` 所有权，避免 PR-01 与旧 PR-05 冲突。
2. **P1**: 修上游计划的 `specialTierId` 口径，明确 PR-03 最终选择扩 `ItemRenderSnapshot`。
3. **P2**: 更新 `Resource Fallback Audit` 模板，使其满足上游强制列。
4. **P2**: PR-02 把输入 truth table 从按键覆盖清单升级为行为矩阵。
5. **P2**: PR-04 收紧 `keywordRegistryLint` coverage 不足时的 gate 语义。
6. **P2**: PR-04 写清 a11y 开关的人工启动方式。
7. **P2**: PR-05 把 `missingFactReason` 纳入自动化和人工白盒场景。

---

## 6. Removal / Iteration Plan

1. 删除或清理本地 `docs/opt/ui-pr/.DS_Store`。
2. PR-03 收口时删除 `ui.inspect.empty.tile` locale key，并补 deprecated-key 断言。
3. PR-05 收口时清理 `phase4-uiux-pr02-combat-decision-stub-*` golden label，并保留 migration 记录。
4. PR-04 如果 `keywordRegistryLint` 只能 WARN，应只允许对历史未修改面 WARN；当前 PR 新增/修改面必须 hard fail。
5. PR-03 如果无法同步上游 snapshot contract，应回退 special axis 目标，避免临时 client 反查 content template。

---

## 7. Additional Suggestions

1. README 可以新增一段“上游源计划修订状态”，专门列出从 8 PR 合并到 5 PR 后已改变 owner 的合同，例如 `BuildInfo.shortHash` 和 `specialTierId`。
2. 每个 PR 的 §6.2 可以统一增加一行“若本 PR 修改了本文档列出的新增/修改文件以外路径，必须补写原因和额外 owner gate”，避免实现时扩范围但文档不更新。
3. `manual-records/_template.md` 建议增加“启动命令 / JVM 参数 / feature flags”字段。PR-04 a11y 和后续 validation preset 都会用到。
4. 对所有 fallback audit，建议要求 PR description 写“是否开放正式玩家路径”。这比只写 fallback 行为更能阻止临时资源变长期方案。

---

## 8. Suggested Verification

本轮是文档 review，未运行 Gradle。文档修完后建议执行以下只读核对：

```bash
rg -n "BuildInfo.shortHash|specialTierId|不在本 PR 发明新的 snapshot 字段|Resource Fallback Audit|失效风险等级|unblock task" docs/opt docs/phase4 docs/2026-03-13-core-systems-design-and-phase-supplements.md
rg -n "ui.inspect.empty.tile|deprecated-key|keywordRegistryLint|missingFactReason|1024x768" docs/opt/ui-pr
rg -n "phase4-uiux-pr02-combat-decision-stub|phase4-uiux-pr05" docs/opt/ui-pr client/src/test tools/src/test
find docs/opt/ui-pr -name ".DS_Store" -print
```

实现 PR 前建议按 README 执行每 PR 的 doc/code review gate；实现完成后再跑对应 focused tests 和 root gates，不要用 focused tests 替代 `clientSmoke goldenScreenshot verifyChanged`。

---

## 9. Summary

当前 PR 文档已经从“可讨论的拆分方案”接近“可执行的开发合同”。剩余问题主要集中在三类：

1. 上游源计划没有同步 PR 文档的新决策。
2. 少数 gate 仍存在 WARN、fallback、unknown 这类可绕过路径。
3. 人工白盒个别步骤还不够可复现。

建议先修 P1/P2，再进入 PR-01 实施。P3 可在同一轮文档整理里顺手收掉。

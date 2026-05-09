# Dark UI/UX PR06 PR-Level Standard Review

目标文档：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`

评审依据：

- `docs/review/rule/pr-level-review-standard.md`
- `AGENTS.md`
- `UI/PLAN.md`
- `UI/pr/README.md`
- `UI/pr/development-governance.md`
- `UI/pr/screen-coverage-matrix.md`
- 当前仓库中与 dark manifest、status、talent、quest、validation overlay 相关的代码与测试

## 结论

PR06 目前还不能作为“无需猜测即可开发”的执行文档。它的目标方向正确，但在 final-full manifest 覆盖口径、quest icon 的真实消费路径、冻结职业 exclusion 表达、关键资源 family 的精确命名、跨 PR handoff artifact、验证 overlay 的布局约束上仍存在会直接影响实现的缺口。

本轮没有发现需要立即叫停整个 PR06 的 P0 问题；但至少 3 个 P1 必须在开发前修正文档，否则实现者可能做出“覆盖报告通过但玩家界面未真正替换”“final-full false green”“冻结资源缺口无证据”的结果。

## 预检摘要

- 命中阶段：Dark UI/UX PR06，资源与 client 表现联动 PR。
- 受影响模块：`assets-src`、`client`、`tools`、`UI` 文档与资源计划。
- 关键合同：canonical manifest first、runtime manifest sync、dark-v1 resource coverage、repo-relative artifact、no transient source path as contract、verifyChanged 不替代 owner gate。
- 主要非目标：本报告只审查 PR06 文档可实施性，不修改 PR06 文档、不生成资源、不改生产代码。
- 本轮未运行 Gradle。评审基于文档、代码与脚本静态核对；建议的验证命令在报告末尾单独列出。

## Findings

### P1-1 final-full 覆盖分母没有冻结，当前文档允许 false green

证据：

- PR06 要求 `final-full` 证明 skill、tree、status、quest 等 player-visible key 已覆盖：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:103-113`。
- `UI/PLAN.md:70-78` 和 `UI/pr/README.md:143-159` 定义 PR06/PR07 应使用 final-full，且不能以 ownerPr 过滤。
- 当前 `scripts/verify_dark_manifest_coverage.py:88-102` 的 final-full expected keys 实际来自 `UI/sprite-sheets/key-registry.yaml`，不是从 canonical manifest 全量推导。
- 当前 `UI/sprite-sheets/key-registry.yaml:1-31` 只有 PR00 key。如果 PR06 文档不要求先冻结 final-full expected inventory 并补齐 registry，coverage 可以只对已登记 key 通过，而未登记的旧风格 player-visible key 不会进入分母。

影响：

实现者可以只把 Round 8/9 已知条目写入 registry 并拿到通过的 final-full 报告，但 `phase2-visual-manifest.json` 中仍可能存在未迁移的 player-visible key。这个问题会直接破坏 PR06 “full manifest” 的名字和验收含义。

必须补充到 PR06：

1. 新增 “Final-full denominator freeze” 小节，说明 final-full 的 expected inventory 如何从 canonical manifest、game visual index、screen coverage matrix 推导。
2. 明确列出 PR06 必须纳入分母的 key family 和数量快照，至少包括 `icon.skill.*`、`talent.*.icon`、`talent.*.visual`、`icon.damage_type.*`、`icon.status.*`、`icon.mutation.*`、`icon.quest.*`、`zone.*.icon`、`icon.profession.*`、`icon.tree.*`、`tree.*`、`difficulty.normal.icon`。
3. 要求 `key-registry.yaml` 在 final-full 前必须拥有所有 expected key，禁止用“未登记”规避 missing。
4. 明确新增工具测试，例如 `DarkSpriteSheetPipelineScriptTest.finalFullFailsWhenPlayerVisibleManifestKeyIsMissingFromRegistry`。
5. 明确新增 client manifest 解析测试，例如 `ManifestResolveTest.darkUiuxPr06FinalFullKeyFamiliesResolveThroughDarkEntries`。

### P1-2 quest icon 是 PR06 目标，但文档没有真实消费路径

证据：

- PR06 目标包含 quest marker / quest icon：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:39-43`、`103-109`、`128-138`。
- `UI/pr/screen-coverage-matrix.md:44-49` 将 skills/status/quest、combat prompt、look/inspect、world route、reward/frontstage 都列入 PR06 相关屏幕面。
- 当前 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:490-612` 的 shell/quest summary 仍是文本路径，`TileTextRow` 虽支持 `iconKey`，但 quest summary 未说明如何绑定 `icon.quest.*`。
- PR06 影响范围只列了 status、talent、validation overlay 和 manifest resolver 测试，没有列出 quest presenter、`TileRenderModel` 或专门的 quest UI 测试：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:53-59`。

影响：

开发者可能只替换 manifest 中的 `icon.quest.*` 资源并通过 resolver 测试，但玩家实际界面不消费 quest icon。这样 `dark-uiux-pr06-status-quest-skill-overview` golden 也可能只能证明文本仍在，而不能证明 icon 上屏。

必须补充到 PR06：

1. 指定 quest icon 的消费位置：直接在 `TileRenderModel.buildShell` 接入，或新增 `QuestSummaryPresenter` 再由 shell 渲染消费。
2. 定义 objective/log/route 到 `icon.quest.*` 的映射规则、缺失时 fallback、排序与空状态行为。
3. 增加测试名称与断言面，例如 `QuestSummaryPresenterTest.usesQuestIconForActiveObjective` 或 `TileRenderModelTest.shellQuestSummaryCarriesQuestIconKey`。
4. 在 golden label 中明确 quest icon 可见性验收，不只写 “status quest skill overview”。

### P1-3 冻结职业 exclusion 是验收要求，但 coverage artifact 无法表达

证据：

- PR06 要求 `shadowblade / warden` 等冻结职业不进入 sprite 生成，且 exclusion 必须写入 coverage artifact：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:31`、`82-84`。
- 当前 coverage 脚本输出 `allowedCoverageExclusions` 固定为空数组：`scripts/verify_dark_manifest_coverage.py:180-195`。
- PR06 没有定义 exclusion 的数据来源、字段结构、允许 reason、对应 key 列表或审核责任人。

影响：

实现时只有两种坏选择：把冻结职业放进 registry 导致 final-full missing，或不放进 registry 导致 coverage 看不见它们。两者都不能证明“冻结是有意决策而非遗漏”。

必须补充到 PR06：

1. 定义 exclusion source，例如 `UI/sprite-sheets/dark-v1-final-full-inventory.json` 或 `key-registry.yaml` 中的 `coverageExclusion` 字段。
2. 定义字段：`key`、`family`、`reason`、`ownerPr`、`removalOwner`、`expiresAfterPr`、`visibleFallbackKey`。
3. 限定允许 reason，例如 `frozen-profession-not-player-visible`。
4. 要求 coverage report 输出非空 exclusion 时仍逐项校验字段完整性和 repo-relative evidence。
5. 增加工具测试，例如 `DarkSpriteSheetPipelineScriptTest.finalFullReportsAllowedFrozenProfessionExclusions`。

### P2-1 artifact 路径与 canonical 口径冲突

证据：

- PR06 把 coverage artifact 写成 `assets-src/image/manifests/dark-v1-manifest-coverage.json`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:107`。
- 当前 Gradle task 和脚本默认输出是 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`：`scripts/verify_dark_manifest_coverage.py:40-50`、`tools/build.gradle.kts:597-636`。
- 当前脚本报告包含 `generatedAt`：`scripts/verify_dark_manifest_coverage.py:136-148`。如果直接提交该报告，会产生时间戳 churn；如果不提交，PR06 又要求 artifact 路径在 `assets-src`。

影响：

开发者不知道哪个文件是 canonical evidence、哪个只是 build output。reviewer 也无法机械检查 PR06 的 artifact 是否应提交。这个问题会放大为 CI、本地自检、PR 描述三方口径不一致。

建议修订：

1. 明确 coverage report 的唯一 canonical 输出位置。若是 build report，就不要写成 `assets-src`。
2. 若需要 committed artifact，必须定义稳定化规则，至少移除或分离 `generatedAt`。
3. Acceptance Matrix 中所有 artifact 都写 repo-relative 精确路径，而不是 “dark manifest coverage report” 这种泛称。

### P2-2 key family 命名不精确，存在新增第二套 key 的风险

证据：

- PR06 的 key switch table 使用 family 级描述，且写了 “zone/profession/tree/difficulty icon”：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:128-138`。
- `UI/pr/README.md:83-87` 对 Round 9 family 又写成 `icon.zone.*`、`icon.difficulty.*`。
- 当前 canonical manifest 和 game visual index 中，zone/difficulty 实际 key 不是 `icon.zone.*` 或 `icon.difficulty.*`，而是 `zone.<id>.icon`、`difficulty.normal.icon`；damage type 是 `icon.damage_type.*`。
- PR06 验收只点名 `icon.skill.*`、`icon.tree.*`、`icon.status.*`、`icon.quest.*`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:103-109`，遗漏 damage type、mutation、profession、zone、difficulty 等目标 family。

影响：

实现者可能新增 `icon.zone.*` / `icon.difficulty.*` 第二套 key，或遗漏 `icon.damage_type.*`、`icon.mutation.*`、`icon.profession.*`。这会违反 manifest 单一权威和 no second source 原则。

建议修订：

1. 用 exact key family table 替换 family 级描述。
2. 每个 family 写清 current key shape、owner sheet、target output directory、consumer path、test path。
3. 明确禁止新增 parallel alias key；若确实要改 key schema，必须按 public contract change 处理，而不是埋在资源 PR 中。

### P2-3 PR03/PR05 rejected polish handoff 缺少输入 artifact 与 schema

证据：

- PR06 说要承接 PR03/PR05 rejected/polish cells：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:63-70`、`115-126`。
- PR-level review standard 要求跨 PR 依赖必须有 crossPrDependency/removalOwner/evidence path：`docs/review/rule/pr-level-review-standard.md:164-178`、`205-210`。
- PR06 没有写 PR03/PR05 输出 artifact 路径、字段、过滤规则、谁确认 rejected 转入 PR06、谁确认继续留给 PR07。

影响：

开发时会在“补哪些 rejected cells”“哪些仍留给 PR07”“哪些已不再 player-visible”之间反复猜测，且无法在 PR 描述中证明 handoff 完整。

建议修订：

1. 增加 handoff input 表，列出 PR03/PR05 的具体 artifact 路径。
2. 定义字段：`targetKey`、`sourceSheetId`、`qaStatus`、`rejectionReason`、`playerVisible`、`ownerPr`、`polishOwner`、`requiredByPr06`。
3. 定义 PR06 过滤规则：哪些 rejected 必须补，哪些只登记 exclusion，哪些由 PR07 final audit 接管。
4. Acceptance Matrix 增加 `crossPrDependency` 与 `removalOwner` 列。

### P2-4 validation overlay / active pack summary 缺少布局与截断合同

证据：

- PR06 要求 validation overlay / active pack summary 更换 dark background 并处理 old-style marker / missing sprite：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:55-57`、`103-109`、`160-164`。
- 当前 `ValidationPackSummaryText` 会把 pack ids、namespaces、overlay ops、touched content ids、warnings 直接 join 成长字符串。
- 当前 `ValidationScenarioEvidenceSummaryLines` 会输出多行 evidence/path/value 汇总。
- 现有 `ValidationPackSummaryTextTest` 只覆盖 key warning 分隔，不覆盖 active pack summary、长列表、路径压缩、最大行数或 fallback。

影响：

PR06 开发者必须自行发明 UI 行数、排序、截断、省略、路径展示规则。不同实现都可能通过普通单测，但 golden 会变成不稳定或不可读。

建议修订：

1. 定义 overlay summary 的最大行数、每行最大字符数、section 顺序、空值文案、排序与 deterministic truncation 规则。
2. 明确 repo-relative path 展示策略，禁止把机器绝对路径或 transient source path 写入 overlay。
3. 增加测试：`ValidationPackSummaryTextTest.activePackSummaryTruncatesLongPackLists`、`ValidationScenarioEvidenceSummaryLinesTest.compactsRepoRelativeEvidencePaths`。
4. golden label 必须包含长列表与 fallback 场景，而不是只截常规成功路径。

### P2-5 talent state icon 与 PR02/PR04 的 state glyph 合同未对齐

证据：

- PR06 要求 profession tree 覆盖 learned / learnable / locked / active 等状态可读性：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:88-92`。
- PR04 明确 node row 要有 PR02 的 `ui.state.locked/learnable/active/reserve.icon` 状态 glyph。
- 当前 `TalentSidebarPresenter` 仍通过文本前缀 `[x]`、`[+]`、`[r]`、`[*]` 表达状态，并传递 node icon key。
- PR06 没有说明是继续保留文本 glyph、改为 asset state badge、还是双轨兼容。

影响：

开发者可能把 skill icon、tree icon、state icon 混成一个字段，或者只 rebaseline golden 而没有真正消费 PR02 的 state glyph。状态可读性会在 hover/selected/locked 组合下漂移。

建议修订：

1. 明确 node row 的结构：skill icon、state badge、label、cost/requirement、selection marker 分别由哪个字段承载。
2. 明确 PR06 是否允许删除文本状态前缀；如果保留，说明它只是 accessibility/debug 辅助，不是 primary dark visual cue。
3. 增加 test/golden：locked、learnable、reserve、active 四状态必须同时出现在 `dark-uiux-pr06-talent-icon-rebaseline`。

### P2-6 Acceptance Matrix 不能机械验收

证据：

- PR06 Acceptance Matrix 中 artifact 写成 `dark manifest coverage report`、`dark coverage artifact`、`verifyChanged output`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:15-23`。
- `UI/pr/development-governance.md:20-30` 要求 artifact 必须是 repo-relative path。
- PR06 明确依赖 PR03/PR04/PR05，却没有在 matrix 中记录 crossPrDependency、removalOwner 或 blocked/unblocked 条件。

影响：

reviewer 无法只看矩阵判断每项是否完成；开发者也无法按矩阵逐项交付 evidence。这违反 PR-level review standard 对 “直接驱动开发” 的要求。

建议修订：

1. 为每个 milestone 写具体 artifact path，例如 build report、golden report、manual record、test report。
2. 增加 `crossPrDependency`、`removalOwner`、`ownerGate`、`manualEvidence` 列。
3. `UI06-M06` 不应只写 `verifyChanged output`，还要列出 owner gate 输出；verifyChanged 只能作为共享 preflight。

### P2-7 PR06 coverage mode 与 governance 文档冲突

证据：

- PR06 自身要求 `:tools:darkManifestCoverageLint -PdarkUiuxCoverageMode=final-full`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:142-156`。
- `UI/pr/README.md:143-159` 也写 PR06/PR07 使用 final-full。
- `UI/pr/development-governance.md:106-108` 却写 PR05 到 PR06 必须严格执行 owner-scope coverage。

影响：

开发和审查可能在 owner-scope 与 final-full 之间切换，导致同一 PR 的 gate 口径不可复现。更严重的是，owner-scope 会掩盖 PR06 应该发现的 full manifest 缺口。

建议修订：

1. 在 PR06 中明确：正式验收必须 final-full，不带 ownerPr。
2. 若确实需要阶段内临时 owner-scope，只能作为中间开发辅助，不能写入完成定义。
3. 同步修正 `UI/pr/development-governance.md` 中 PR06 的 coverage mode 说法。

### P3-1 PR 描述要求包含 Codex transient source folder，容易违反路径红线

证据：

- PR06 要求 PR description 列出 `Codex CLI transient source folder / source image summary`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:74-78`。
- `UI/pr/development-governance.md:86-92` 禁止把 transient source dir 和机器绝对路径作为 artifact。
- `UI/PLAN.md:188-190` 也说明 Codex generated dir 只是临时来源，不是长期合同。

影响：

实现者可能把本机临时路径写入 PR 描述或 artifact，违反 repo-relative path 规则。

建议修订：

1. 改成只记录 `sourceImageHash`、`sourceImageCount`、`promptPath`、`rawSheetPath`。
2. 如果必须提到临时目录，只允许写占位符 `<codex-generated-images-dir>`，不能写真实机器路径。
3. PR description template 中增加 “no absolute path / no transient source path” 检查项。

## 需要补进 PR06 的最小开发执行清单

为了让 PR06 变成可执行文档，建议至少补以下小节：

1. **Final-full Expected Inventory**
   - 精确列 key family、key shape、expected count、inventory source、生成命令、失败条件。
   - 明确 registry 必须覆盖所有 expected key，不能通过 omission 缩小分母。

2. **Quest Icon Consumer Contract**
   - 指定 consumer 文件、状态来源、映射规则、fallback、测试与 golden label。

3. **Frozen Profession Exclusion Contract**
   - 指定 exclusion source、字段、允许 reason、fallback icon、removal owner、coverage report 输出规则。

4. **Exact Key Switch Table**
   - 用 exact key 替换 `zone/profession/tree/difficulty icon` 这类 family 描述。
   - 明确 `zone.*.icon`、`difficulty.normal.icon` 不是 `icon.zone.*`、`icon.difficulty.*`。

5. **Cross-PR Handoff Inputs**
   - 列 PR03/PR05 rejected/polish artifact 路径和 schema。
   - 明确哪些必须在 PR06 解决，哪些转 PR07 final audit。

6. **Validation Overlay Layout Rules**
   - 定义长列表、路径、missing key、fallback warning 的行数、截断和排序。

7. **Acceptance Matrix Upgrade**
   - 每行补 repo-relative artifact path、owner gate、manual evidence、crossPrDependency、removalOwner。

## 建议验证入口

这些是修订 PR06 并进入实现后应运行的验证；本报告未执行这些命令。

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :tools:darkManifestCoverageLint -PdarkUiuxCoverageMode=final-full
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest
./gradlew :client:test --tests com.ktome.client.ui.status.StatusPresentationModelTest
./gradlew :client:test --tests com.ktome.client.render.ValidationPackSummaryTextTest
./gradlew :client:goldenScreenshot -PgoldenLabels=dark-uiux-pr06-status-quest-skill-overview,dark-uiux-pr06-talent-icon-rebaseline
./gradlew verifyChanged
```

如果 PR06 修订涉及 `build.gradle.kts`、`tools/build.gradle.kts` 或 coverage task wiring，还应补：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew maintainabilityLint
./scripts/verify-bootstrap.sh
```

## Open Questions

1. PR06 是否要真正把 quest icon 接入 shell/quest summary，还是只负责资源准备并把实际消费留给 PR07？当前文档写的是 PR06 目标，因此建议 PR06 必须接 consumer。
2. `icon.zone.*` / `icon.difficulty.*` 是 README 的笔误，还是准备引入新 key alias？若是后者，必须升级为 public manifest contract change。
3. final-full artifact 是否需要提交进仓库？如果需要，必须先解决 timestamp 与 canonical path；如果不需要，PR06 文档不要把 build output 写成 committed artifact。

## 评审结论

PR06 当前可以作为目标草案，但还不是合格的开发执行文档。优先修 P1-1、P1-2、P1-3；这三项不收口，后续资源生成、manifest lint、golden 与 manual evidence 都可能出现“看起来完成但玩家路径未闭环”的假完成。

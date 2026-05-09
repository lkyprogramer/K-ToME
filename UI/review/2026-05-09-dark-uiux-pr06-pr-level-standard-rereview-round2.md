# Dark UI/UX PR06 PR-Level Standard Re-Review Round 2

目标文档：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`

审查范围：

- 上游入口：`docs/review/rule/pr-level-review-standard.md`、`AGENTS.md`、`docs/INDEX.md`、`docs/rule/kotlin.md`、`docs/rule/ai-change-governance.md`、`UI/PLAN.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`UI/pr/screen-coverage-matrix.md`
- 当前代码锚点：`scripts/verify_dark_manifest_coverage.py`、`scripts/verify_sprite_sheet_map.py`、`tools/build.gradle.kts`、`build.gradle.kts`、`client` status / quest / validation overlay / talent 相关文件与测试
- 历史 review：`UI/review/2026-05-09-dark-uiux-pr06-pr-level-standard-review.md`
- 本轮重点：确认上轮 P1/P2 是否真正闭环，并继续找出会让实现者猜测 owner、gate、artifact、fallback、测试或验收口径的细节问题

## Findings

### P0

无。PR06 当前没有发现会立即破坏稳定合同或让主干不可验证的文档级 P0。

### P1

#### P1-1 上轮 P1-1 部分解决：final-full inventory 写进文档了，但还没有可执行的 gate 接线

证据：

- PR06 现在要求开工第一步生成 `UI/sprite-sheets/dark-v1-final-full-inventory.json`，并让 `darkManifestCoverageLint final-full` 的 `expectedKeySetSource` 指向 inventory 或能从 inventory 追到 registry：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:176-191`
- 当前 coverage 脚本 final-full 仍把 `expected_keys` 直接设置为 `registry_keys`：`scripts/verify_dark_manifest_coverage.py:88-102`
- 当前 report 中 `expectedKeySetSource` 固定写为 registry path：`scripts/verify_dark_manifest_coverage.py:140-151`
- 当前 Gradle task 只暴露 `ktome.darkUiux.coverageMode / ownerPr / requiredOwnerSheetIds`，没有 inventory path 或 expected inventory 输入：`tools/build.gradle.kts:613-652`
- 本轮实际运行 `./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full`，失败 report 的 `expectedKeySetSource` 仍是 `UI/sprite-sheets/key-registry.yaml`，`expectedKeySet` 只有当前 registry 的 4 个 PR00 dry-run key。

影响：

文档已经把 “final-full denominator freeze” 写成 PR06 的 blocking requirement，但实现者仍必须自己决定：

1. inventory 由哪个脚本生成；
2. inventory 是 `darkManifestCoverageLint` 的输入，还是只供人工审查；
3. `expectedKeySetSource` 到底应该写 inventory path 还是 registry path；
4. registry 漏登记 manifest key 时，gate 如何 fail fast。

这会保留上轮最核心风险：只要 registry 不完整，final-full 仍可能按缩小后的分母验收。

修复方向：

1. PR06 §2 影响范围必须显式加入 `scripts/verify_dark_manifest_coverage.py`、`tools/build.gradle.kts`、`DarkSpriteSheetPipelineScriptTest`。
2. PR06 §6.1 固定唯一机制，例如：
   - `scripts/verify_dark_manifest_coverage.py --expected-inventory UI/sprite-sheets/dark-v1-final-full-inventory.json`
   - `./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json`
3. final-full report 的 `expectedKeySetSource` 必须写 inventory path；`keyRegistryPath` 继续作为 per-key metadata source。
4. 新增测试：`DarkSpriteSheetPipelineScriptTest.finalFullUsesInventoryAsExpectedKeySetSource`、`DarkSpriteSheetPipelineScriptTest.finalFullFailsWhenInventoryKeyIsMissingFromRegistry`。

推荐测试：

- `DarkSpriteSheetPipelineScriptTest.finalFullUsesInventoryAsExpectedKeySetSource`
- `DarkSpriteSheetPipelineScriptTest.finalFullFailsWhenInventoryKeyIsMissingFromRegistry`

#### P1-2 上轮 P1-3 部分解决：frozen profession exclusion schema 写清了，但当前 schema 和脚本仍无法表达多 key exclusion

证据：

- PR06 要求 `shadowblade / warden` exclusion 写入 coverage artifact：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:128-147`
- PR06 inventory schema 使用 `families[].coverageExclusion` 单个对象：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:180-191`
- frozen profession 实际会跨多个 key family 和多个 key，例如 `icon.profession.*`、`icon.skill.*`、`talent.*.icon`、`talent.*.visual`、`icon.tree.*`、`tree.*`；单个 family-level `coverageExclusion` 对象无法表达同一 family 内多个 key 的不同 fallback、evidence 或 removal owner。
- 当前 coverage 脚本仍 hard-code `allowedCoverageExclusions: []`，没有读取、校验或输出 exclusion object：`scripts/verify_dark_manifest_coverage.py:186-201`
- PR06 说字段缺失、unknown reason、fallback key 不存在要让 `darkManifestCoverageLint final-full` 失败，但没有指定脚本输入、校验函数或测试名：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:147`

影响：

PR06 的 frozen profession 现在有了文档字段，但实现者仍无法机械落地。最容易出现两种错误实现：

1. 把 excluded key 从 inventory / registry 移除，导致 final-full 看不到它们；
2. 把整族 key 用一个 exclusion object 代表，丢失每个 key 的 fallback 与 evidence。

这会让 “冻结是有意决策，不是漏资源” 无法被 gate 证明。

修复方向：

1. 把 schema 改为 per-key exclusion，例如 `families[].keys[].coverageExclusion`，每个 key 的 exclusion 独立携带 `reason / visibleFallbackKey / evidencePath / removalOwner`。
2. `darkManifestCoverageLint` 从 inventory 读取 exclusion，输出到 `allowedCoverageExclusions`，并对每一项校验字段、reason、fallback key、repo-relative evidence path。
3. final-full 在计算 missing/pending/old-style 时必须区分：
   - expected but excluded key；
   - expected and missing key；
   - expected and covered key。
4. 新增测试：`DarkSpriteSheetPipelineScriptTest.finalFullReportsAllowedFrozenProfessionExclusions`、`DarkSpriteSheetPipelineScriptTest.finalFullRejectsMalformedCoverageExclusion`。

推荐测试：

- `DarkSpriteSheetPipelineScriptTest.finalFullReportsAllowedFrozenProfessionExclusions`
- `DarkSpriteSheetPipelineScriptTest.finalFullRejectsMalformedCoverageExclusion`

#### P1-3 上轮 P2-3 部分解决：PR03/PR05 handoff 仍没有 exact artifact path，PR06 开工时还要猜输入

证据：

- PR06 新增了 Cross-PR handoff input 表，但 `Input source` 仍是 “PR-03 item/equipment QA rejected report or manual record” 和 “PR-05 map/actor/portrait QA rejected report or manual record” 这种描述：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:118-126`
- PR03 已经在文档中固定了 artifact path：`assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`、`UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md`、`UI/manual-records/dark-uiux-pr03-fallback-key-injection.md`：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:30-50`
- PR05 也固定了 artifact path：`assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl`、`UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md`：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:57-73`
- 当前 repo 还没有 PR03/PR05 dark manual record 文件落盘；`UI/manual-records/` 当前只有 PR01 shell 记录。PR06 只说找不到上游 artifact 时补一份 handoff inventory，但没有固定该 inventory 的路径和字段。

影响：

PR06 明确以前置 PR03/04/05 完成为前提，但 handoff 输入没有落成精确路径。开发者开工时仍要自己判断应该读哪个报告、哪个 manual record、哪个 old phase report 是否可用。这个问题符合 PR-level standard 的 P1 条件：“PR 依赖未冻结，开发者无法确定实现顺序”。

修复方向：

1. 把 PR06 handoff 表改成 exact path 表，至少列：
   - `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`
   - `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md`
   - `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md`
   - `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl`
   - `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md`
2. 固定 “缺上游 artifact” 时的补充产物，例如 `UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json`。
3. 定义 handoff inventory schema，并明确它是否并入 `dark-v1-final-full-inventory.json`。

推荐测试 / 检查：

- `acceptanceContractLint` 增加 handoff path existence / repo-relative pattern check，或 PR06 close 前用脚本校验 handoff table 中的路径字段。

### P2

#### P2-1 quest icon consumer 合同仍给了两条实现路径，没有冻结当前 PR 的最小实现

证据：

- PR06 现在明确 quest summary 在 `TileRenderModel.buildShell / questSummaryText`，这是上轮问题的进展：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:211-223`
- 当前 `TileRenderModel` 的 quest summary 仍只从 `log.objective.*` token 提取文本，`TileTextRow` 支持 icon 但该 row 没有 icon：`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:490-612`
- PR06 的输入优先级是 “typed quest/objective icon metadata（如果当前 snapshot 已有） > current objective/log token 的 client-only mapping > generic icon”：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:217-220`

影响：

“如果当前 snapshot 已有” 会让实现者自行决定是否扩展 `RenderSnapshot` / quest metadata。扩 snapshot 是稳定合同变化，不能作为 PR06 内的隐含选项；client-only mapping 又没有给出 exact mapping table 或 owner 类型，容易变成 scattered string mapping。

修复方向：

1. PR06 必须选择一个当前 PR 的唯一最小路径。建议本 PR 固定为 generic `icon.quest.objective_marker`，不扩 `RenderSnapshot`。
2. 如果要做 token mapping，必须新增并命名一个 client-only resolver，例如 `QuestSummaryIconResolver`，列出 exact mapping：
   - `log.objective.progress -> icon.quest.objective_marker`
   - `log.objective.complete -> icon.quest.objective_marker` 或更具体 key
3. 若要扩 typed metadata，必须升级为 stable snapshot/public contract change，并补对应 owner doc/test，不能藏在资源 PR 里。

推荐测试：

- `TileRenderModelTest.shellQuestSummaryCarriesGenericQuestMarkerForObjectiveLog`
- `TileRenderModelTest.shellQuestSummaryDoesNotUseQuestIconForEmptyState`

#### P2-2 status icon null / missing 语义未冻结，开发者不知道是 drop、fallback 还是 fail

证据：

- PR06 要求 status/mutation family 全量切 dark-v1，并新增 `StatusIconResolverTest`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:193-207`
- 当前 `StatusIconResolver.resolveIcons` 对 `presentation.iconKey == null` 使用 `mapNotNull` 直接丢弃，不会 fallback，也不会 report：`client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt:19-33`
- 当前 `StatusPresentationBuilder.buildZoneEffect` 明确把 zone effect 的 `iconKey` 设为 `null`：`client/src/main/kotlin/com/ktome/client/ui/status/StatusPresentationModel.kt:52-63`
- PR06 没有说明 zone effect / telegraph / status missing icon 在 PR06 后应如何处理。

影响：

如果 PR06 只测试 `icon.status.*` 和 `icon.mutation.*` 的正常解析，zone effect 或 missing status icon 仍可能从 HUD 消失而不触发 report。玩家会丢失状态反馈，reviewer 也无法判断这是允许的 compact 行为还是资源缺口。

修复方向：

1. 在 PR06 §6.1 status family 下面补 nullability/fallback 规则：
   - 正式 status / mutation `iconKey` 不得为 null；
   - zone effect 若继续无 icon，必须在 inventory 中标 `manifest-only-with-reason` 或明确不计入 status icon coverage；
   - missing icon key 必须走 resolver fallback 并记录 report，而不是静默 drop。
2. `StatusIconResolverTest` 至少覆盖：
   - status icon resolves exact key；
   - mutation icon resolves exact key；
   - null icon status 是 allowed drop 还是 fail/fallback；
   - unknown icon key 触发 fallback log/report。

推荐测试：

- `StatusIconResolverTest.resolvesStatusAndMutationIcons`
- `StatusIconResolverTest.nullIconStatusUsesDocumentedDropOrFallbackRule`
- `StatusIconResolverTest.unknownStatusIconUsesRegisteredFallback`

#### P2-3 overlay compact 合同有布局数字，但没有映射到当前函数输出形状

证据：

- PR06 规定 validation overlay summary 主体最多 12 行、单行最多 96 chars、section order、排序和路径展示规则：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:225-246`
- 当前 `ValidationPackSummaryText` 的函数分别返回单个 `String`，没有统一的 12 行 aggregator：`client/src/main/kotlin/com/ktome/client/render/ValidationPackSummaryText.kt:7-68`
- 当前 `validationScenarioEvidenceSummaryLines` 返回 `List<String>`，会直接追加多个 evidence path 行，没有 path compaction：`client/src/main/kotlin/com/ktome/client/render/ValidationScenarioEvidenceSummaryLines.kt:6-46`

影响：

文档冻结了 UI 结果，但没有说哪个函数负责 row budget，也没有说明 `ValidationPackSummaryText` 是否需要改返回类型或新增 aggregator。实现者可能在 renderer 里临时截断，也可能在每个 formatter 内各自截断，造成多处第二套 compact 逻辑。

修复方向：

1. PR06 指定唯一 owner，例如新增 `ValidationOverlaySummaryPresenter`，输入 active pack summary + evidence summary，输出 bounded rows。
2. `ValidationPackSummaryText` 保持 leaf formatter，row budget 只在 presenter 层做；或反过来明确它改为返回 structured rows。
3. 测试名绑定到 owner 类型，而不是只列 leaf formatter。

推荐测试：

- `ValidationOverlaySummaryPresenterTest.boundsRowsAndLineLength`
- `ValidationOverlaySummaryPresenterTest.keepsEvidencePathsRepoRelative`

#### P2-4 §7 验证命令没有覆盖文档自己新增的 focused tests

证据：

- PR06 影响范围要求 `TileRenderModelTest` 或 `TileRendererCanvasTest` 覆盖 quest summary icon：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:93-96`
- PR06 §6.3 要求 `ValidationScenarioEvidenceSummaryLinesTest.compactsRepoRelativeEvidencePaths` 和 `rejectsOrRedactsMachineAbsolutePaths`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:239-244`
- PR06 §7 的大命令只列了 `StatusPresentationModelTest`、`StatusIconResolverTest`、`TalentSidebarPresenterTest`、`InputHandlerTest`、`ValidationPackSummaryTextTest`、`ManifestResolveTest`，没有列 quest focused test 或 `ValidationScenarioEvidenceSummaryLinesTest`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:281-295`

影响：

开发者按 §7 执行会漏掉 PR06 新增的两块关键行为：quest icon 上屏和 evidence path compact。golden 可能覆盖一部分，但 PR-level standard 明确不能用 golden 替代可自动化的核心行为测试。

修复方向：

把 §7 拆成三段并补齐：

```bash
./gradlew :client:test \
  --tests com.ktome.client.ui.status.StatusPresentationModelTest \
  --tests com.ktome.client.ui.status.StatusIconResolverTest \
  --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest \
  --tests com.ktome.client.render.TileRenderModelTest \
  --tests com.ktome.client.render.TileRendererCanvasTest \
  --tests com.ktome.client.render.ValidationPackSummaryTextTest \
  --tests com.ktome.client.render.ValidationScenarioEvidenceSummaryLinesTest \
  --tests com.ktome.client.assets.ManifestResolveTest
```

如果最终只新增 `TileRendererCanvasTest` 而不新增 `TileRenderModelTest`，文档也要把 “or” 收敛成实际文件名。

#### P2-5 `verifyChanged` 仍接 PR00 dry-run，PR06 需要说明 final-full 不是由 final verifyChanged 证明

证据：

- PR06 要求 close gate final-full，最终还要跑 `verifyChanged`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:11-24`
- root `verifyChangedTaskPaths` 当前仍包含 `:tools:darkManifestCoveragePr00DryRun`，不是 `:tools:darkManifestCoverageLint` final-full：`build.gradle.kts:86-96`
- root `darkManifestCoverageLint` 裸 task 的确存在，默认取 Gradle property 并可 final-full：`build.gradle.kts:463-467`、`tools/build.gradle.kts:613-652`

影响：

PR06 文档现在单独列了 final-full 命令，所以 owner gate 本身可执行；但 “最终 verifyChanged” 仍不会证明 final-full。reviewer 如果只看最后一个 `verifyChanged`，会误以为 dark coverage 已在默认 preflight 中收口。

修复方向：

1. PR06 §7 明确写：“`verifyChanged` 当前仍用于共享 preflight / impact routing，不替代前置 final-full coverage；PR 描述必须单独粘贴 final-full coverage command/result。”
2. 如果 PR06 要把 verifyChanged route 切到 final-full，需要把 `build.gradle.kts:94` 从 `darkManifestCoveragePr00DryRun` 切到 final-full 或做 phase-aware routing，并同步 `verify-bootstrap.sh` / routing tests。

### P3

#### P3-1 Acceptance Matrix Execution Addendum 漏了 `UI06-M06`

证据：

- Acceptance Matrix 有 `UI06-M06` governance inheritance：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:17-25`
- Execution Addendum 只列 `UI06-M01`、`UI06-M02`、`UI06-M03`、`UI06-M04`、`UI06-M05`、`UI06-M07`，漏掉 `UI06-M06`：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:27-36`

影响：

不阻塞开发，但矩阵审计时会出现 requirement id 不闭合。PR-level review standard 要求矩阵能机械追踪 crossPrDependency / removalOwner / manual evidence；没有附表行时 reviewer 需要猜 `UI06-M06` 是否故意 N/A。

修复方向：

补一行：

```text
UI06-M06 | development-governance.md / README gate ladder | N/A | N/A
```

#### P3-2 PR06 仍引用不存在的 coverage schema 文件链路，容易让实现者找错真源

证据：

- `UI/PLAN.md` 的 deliverables 仍列 `UI/sprite-sheets/coverage-schema.md`：`UI/PLAN.md:417-433`
- 当前 repo 中没有 `UI/sprite-sheets/coverage-schema.md`
- PR06 §6 又说 coverage artifact schema 以 PR-00 固定 schema 为准：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:248-250`

影响：

这是低风险文档索引问题，但 PR06 要扩展 inventory / exclusion 时，开发者可能不知道 schema 真源是 PR00 文档、脚本输出，还是一个尚未存在的 `coverage-schema.md`。

修复方向：

在 PR06 §6 加一句：“当前 schema 真源为 PR-00 文档与 `scripts/verify_dark_manifest_coverage.py`；`coverage-schema.md` 未落盘前不得引用其作为开发输入。” 或补上该 schema 文件。

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR06 必须 final-full，不得 owner-scope 缩小分母 | PR06 已写 close gate final-full；`acceptanceContractLint` 通过；显式 final-full gate 可运行并失败 | 部分一致：命令存在，但 expected set 仍来自 registry，不是 inventory |
| final-full expected inventory 冻结分母 | PR06 §6.1 已新增 schema；当前脚本/Gradle 没有 inventory 输入 | 部分一致 |
| frozen profession exclusion 可验证 | PR06 有字段表；当前脚本 hard-code 空数组，family-level schema 无法表达多 key | 部分一致 |
| quest icon player-visible consumer | PR06 指向 `TileRenderModel`；当前代码仍文本-only；文档未冻结唯一实现路径 | 部分一致 |
| validation overlay compact | PR06 有 max row/line/sort/path 规则；当前函数输出形状与 owner presenter 未冻结 | 部分一致 |
| artifact repo-relative 与 transient path 红线 | PR06 已移除真实 transient source folder，artifact path 已改 repo-relative | 一致 |
| key family exact naming | PR06 已明确 `zone.*.icon`、`difficulty.normal.icon`，禁止 `icon.zone.*` alias | 一致 |
| PR03/PR05 rejected handoff | PR06 有字段和决策规则，但没有 exact upstream artifact path | 部分一致 |

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| `tools` final-full coverage | inventory 冻结分母，report 指向 inventory | 部分实现 | `scripts/verify_dark_manifest_coverage.py:88-151` | 仍用 registry 作为分母和 source | P1 |
| `tools` allowed exclusions | frozen profession exclusion 进入 report 并逐项校验 | 未实现 | `scripts/verify_dark_manifest_coverage.py:186-201` | hard-code 空数组，无 schema 校验 | P1 |
| `assets` cross-PR handoff | PR03/05 rejected 输入可追踪 | 部分实现 | PR06 handoff 表、PR03/PR05 artifact 表 | PR06 未引用 exact path | P1 |
| `client` quest summary | quest row 消费 quest icon | 部分实现 | `TileRenderModel.kt:490-612` | 文档有方向，但唯一实现路径未冻结 | P2 |
| `client` status icons | status/mutation 全量 dark icon | 部分实现 | `StatusIconResolver.kt:19-33`、`StatusPresentationModel.kt:52-63` | null/missing 行为未冻结 | P2 |
| `client` validation overlay | 长文本 compact、路径 repo-relative | 部分实现 | `ValidationPackSummaryText.kt:7-68`、`ValidationScenarioEvidenceSummaryLines.kt:6-46` | row budget owner 未冻结 | P2 |
| `docs` acceptance matrix | requirement 可机械追踪 | 部分实现 | PR06 matrix/addendum | addendum 漏 M06 | P3 |

## 玩法与体验审查

### 核心循环

PR06 的核心体验目标是让玩家在战斗、成长、任务和验证模式中看到一致的暗黑视觉语言。文档已把 status、skill、quest、profession/tree、fallback 和 validation overlay 都纳入同一 PR，方向正确。

### 战斗体验

status/mutation/damage type 是战斗反馈关键。当前 PR06 对 key family 已写清，但 status null/missing 语义没有冻结，会导致某些状态从 HUD 静默消失或只在文本里存在。该问题需要在 PR06 内解决，不能推给 PR07 白盒。

### 成长与构筑驱动

职业树 node icon、state badge、文本前缀三者的职责已经比上轮清晰。剩余风险主要在 final-full inventory/exclusion：如果 frozen profession 和 dev-playable 职业的 key 分母不准，职业树资源会在 PR07 才暴露缺口。

### 探索与任务反馈

quest icon consumer 是玩家“下一步做什么”的直接提示。PR06 已明确 quest summary 不能只做 resolver，但仍需把 `log.objective.*` 到 icon 的最小规则冻结，否则实现者会在 typed metadata、client-only mapping 和 generic marker 之间摇摆。

### 新手体验与信息反馈

validation overlay 虽是验证路径，但它也是 debug client 的 player-facing UI。长 evidence path、active pack list 和 fallback warning 如果没有统一 compact owner，会继续遮挡 HUD/日志，影响问题定位。

## 当前阶段必须解决的问题

1. **final-full inventory gate 接线必须在 PR06 开发前写清。**
   - 这是 PR06 的核心 gate 分母，不能靠实现者临场决定。
   - 不能推迟到 PR07，因为 PR07 只能最终审计，不能重新定义 final-full 分母。

2. **frozen profession exclusion 必须改为 per-key 可验证合同。**
   - frozen 职业不是普通缺图；它需要被证明为有意排除。
   - 如果 PR06 不收口，PR07 会在 missing、pending、allowed fallback 三者之间猜测。

3. **PR03/PR05 handoff path 必须精确引用。**
   - PR06 的返修范围依赖上游 artifact。
   - 没有 exact path，开发者不能机械生成 `r09-rejected-polish`。

4. **quest/status/validation 的 focused tests 必须写进 §7 命令。**
   - PR06 是 player-facing resource + client consumer PR。
   - golden 不能替代可自动化的 consumer/fallback/compact 行为测试。

## Removal/Iteration Plan

### Defer Removal / Iteration: final-full denominator currently routes through registry only

| Field | Details |
| --- | --- |
| Location | `scripts/verify_dark_manifest_coverage.py:88-151` |
| Phase/Work Package | Dark UI/UX PR06 |
| Touched contract | dark-v1 manifest coverage report / final-full expected key set |
| Evidence | PR06 now requires inventory; script still derives expected keys from registry |
| Preconditions | Freeze `dark-v1-final-full-inventory.json` schema and Gradle property / script argument |
| Deletion or iteration steps | 1. Add inventory loader 2. Use inventory keys as expected set 3. Validate registry contains per-key metadata 4. Emit inventory path as `expectedKeySetSource` 5. Add script tests |
| Affected harness/gates | `darkManifestCoverageLint final-full`, `DarkSpriteSheetPipelineScriptTest`, `verifyChanged` if routing changes |
| White-box check | Not needed for denominator itself; player-facing evidence still covered by PR06 golden/manual |
| Rollback or fallback | Keep registry-only path only for PR00 dry-run / owner-scope, not for PR06 final-full |

## Additional Suggestions

- PR06 §7 建议拆成 “resource gates / sync+manifest / focused client tests / goldens / final verifyChanged” 五段，不要把所有任务压进一个超长 Gradle 命令。
- `StatusIconResolverTest`、`TileRenderModelTest`、`ValidationScenarioEvidenceSummaryLinesTest` 当前不存在；PR06 可以明确 “新增” 而不是只写测试名，避免 reviewer 误以为现有测试已经覆盖。
- PR06 可在 §6.1 inventory schema 中增加 `schemaOwner=PR-06` 和 `generatedBy` 字段，帮助后续 PR07 确认它不是手写临时 JSON。

## Open Questions

1. PR06 是否打算在本 PR 修改 `darkManifestCoverageLint` 支持 inventory？如果是，文档必须把脚本/Gradle/test 文件列入影响范围；如果不是，`dark-v1-final-full-inventory.json` 就只能是人工证据，不能作为 final-full 分母权威。
2. Quest icon 当前 PR 是否只使用 generic `icon.quest.objective_marker`？若不是，需要冻结 exact token-to-icon mapping。
3. Frozen profession exclusion 是否允许一个 key 以 `planned key` 形式存在但尚未进入 canonical manifest？如果允许，coverage 对 planned key 的 missing 语义要单独写清。

## Suggested Verification

本轮已运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：通过。说明 PR06 当前文档矩阵满足基础结构 lint。

本轮已运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full
```

结果：失败，当前 expected set 只来自 `UI/sprite-sheets/key-registry.yaml` 的 4 个 PR00 key；报告中 `allowedCoverageExclusions=[]`。这不是 PR06 文档失败本身，但证明当前 gate 尚未按 PR06 新增 inventory/exclusion 合同接线。

PR06 文档修订后建议再跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json
```

PR06 实现完成后建议跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew syncPhase2Manifests manifestLint
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew :client:test --tests com.ktome.client.ui.status.StatusPresentationModelTest --tests com.ktome.client.ui.status.StatusIconResolverTest --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.render.TileRenderModelTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.ValidationPackSummaryTextTest --tests com.ktome.client.render.ValidationScenarioEvidenceSummaryLinesTest --tests com.ktome.client.assets.ManifestResolveTest
./gradlew :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint verifyChanged
```

若 PR06 修改 coverage task wiring、Gradle task 或 bootstrap 脚本，还必须跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./scripts/verify-bootstrap.sh
```

未运行：

- `:tools:test`
- `:client:test`
- `:client:clientSmoke`
- `:client:goldenScreenshot`
- `maintainabilityLint`
- `verifyChanged`

原因：本轮是文档复审，只用 `acceptanceContractLint` 与 `darkManifestCoverageLint final-full` 验证结构和 gate 真实行为；没有修改生产代码或资源。

## Summary

PR06 比上一轮明显进步：artifact 路径、key family、quest consumer、overlay compact、transient path、final-full mode、talent state badge 都已经写进文档，`acceptanceContractLint` 也通过了。

剩余问题集中在“写了合同但还不能机械执行”：final-full inventory 没有接入 coverage gate，frozen profession exclusion 不能 per-key 输出和校验，PR03/PR05 handoff 仍缺 exact path。修掉这三项后，PR06 才能从“设计草案增强版”变成真正可执行的开发文档。


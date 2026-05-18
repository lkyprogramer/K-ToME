# Dark UI/UX PR-06 Skills Status Quest Full Manifest

**阶段**: `dark-uiux-pr06-skills-status-quest-full-manifest`
**优先级**: `P1`
**工作量**: `XL`
**前置条件**: PR-02-1 / PR-02-2 owner evidence、PR-03、PR-04、PR-05 完成。
**资源生成结论**: 生成 Round 8-9 与返修资源，完成玩家可见 manifest 收口。

## Open Design 辅助参考

开发 PR06 时可在完成本 PR 预检后读取以下辅助设计输入：

1. [K-ToME Dark UI Design Reference For Open Design](../review/open-design/ktome-dark-ui-design.md)：统一 color roles、spacing、component states 与 anti-pattern 语言。
2. [Dark UI/UX PR06 Skills Status Quest Manifest Design Notes](../review/open-design/dark-uiux-pr06-skills-status-quest-manifest-design.md)：辅助 status + skill + talent/tree + quest marker + fallback/missing/debug + validation warning 的同屏统一样张、icon taxonomy、state badge、contact-sheet QA 和 coverage-green-but-visual-inconsistent 风险检查。

这些文档只用于设计理解、review、prompt 草案和 contact-sheet QA 讨论，不能覆盖本 PR 的 final-full close gate、expected inventory、sheet-plan、manifest、coverage、golden/manual evidence、fallback resolver 或 validation overlay 实现合同。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)，并承担 dark manifest final-full 主体收口。执行前先跑 `acceptanceContractLint`，再跑 resource gate、status / quest / talent focused tests、final coverage 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

PR-06 close gate 固定为 `final-full`，不得带 `ownerPr` 缩小分母。`owner-scope` 只允许作为本地开发中间检查，不能出现在 PR 完成定义、PR 描述或 reviewer 验收口径里。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI06-M01` | §3 Round 8-9 resource scope | `assets` | `darkSpriteSheetLint`, `spriteSheetMapLint` | `assetLint`, `styleLint` | `assets-src/image/contact-sheets/dark-v1/`, `UI/sprite-sheets/sheet-plan.yaml`, `UI/sprite-sheets/key-registry.yaml` | `required` |
| `UI06-M02` | §4 职业树联动 | `client` | `TalentSidebarPresenterTest`, `InputHandlerTest` | `goldenScreenshot` | `build/reports/tests/`, `client/build/reports/golden/` | `required` |
| `UI06-M03` | §6 full manifest 验收标准 | `tools` | `darkKeyRegistryLint`, `ManifestResolveTest` | `manifestLint`, `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full` | `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` | `N/A` |
| `UI06-M04` | status / quest / skill presentation | `client` | `StatusPresentationModelTest`, `StatusIconResolverTest`, `TileRenderModelTest`, `TileRendererCanvasTest` | `clientSmoke`, `goldenScreenshot` | `build/reports/tests/`, `client/build/reports/golden/` | `required` |
| `UI06-M05` | PR-03/05 rejected cell 返修 | `assets` / `tools` | `dark-v1-final-full-inventory.json` diff check | `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full` | `UI/sprite-sheets/dark-v1-final-full-inventory.json`, `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` | `N/A` |
| `UI06-M06` | governance inheritance | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |
| `UI06-M07` | validation overlay / pack summary | `client` | `ValidationPackSummaryTextTest`, `ValidationScenarioEvidenceSummaryLinesTest`, validation smoke | `clientSmoke`, `goldenScreenshot` | `build/reports/tests/`, `client/build/reports/golden/` | `required` |

### Acceptance Matrix Execution Addendum

| requirementId | crossPrDependency | removalOwner | manualEvidence |
| --- | --- | --- | --- |
| `UI06-M01` | PR-00 sheet/key registry contract, PR-03/05 rejected QA artifacts | PR-06 owns Round 8-9 and `r09-rejected-polish` resource replacement; PR-07 may only polish non-player-visible leftovers explicitly recorded by PR-06 | contact sheet QA path and raw sheet hash in PR description |
| `UI06-M02` | PR-04 `TalentSidebarPresenter` state and PR-02 `ui.state.*` glyphs | PR-06 removes old painterly talent/tree icon usage from player-visible talent rows; PR-07 only rechecks final all-screen evidence | `dark-uiux-pr06-talent-icon-rebaseline` |
| `UI06-M03` | PR-00 `darkManifestCoverageLint` schema, PR-02-1 / PR-02-2 `ui-demo-new` owner evidence, and canonical/runtime manifest sync | PR-06 owns final-full denominator freeze and frozen profession exclusions | final-full coverage report path copied into PR description, not committed unless schema is made stable |
| `UI06-M04` | current `TileRenderModel.buildShell` quest summary path and status resolver tests | PR-06 owns quest icon consumer closure; PR-07 may only perform final packaged visual audit | `dark-uiux-pr06-status-quest-skill-overview` screenshot must show status, skill/talent, and quest marker/icon together |
| `UI06-M05` | PR-03/05 rejected/polish reports listed in §3 | PR-06 decides fixed vs allowed exclusion vs PR-07 polish handoff per row | `r09-rejected-polish` contact sheet QA |
| `UI06-M06` | `development-governance.md` / `README.md` gate ladder | N/A | N/A |
| `UI06-M07` | PR-01 validation setup layout and PR-07 packaged-app evidence | PR-06 owns runtime overlay text compaction; PR-07 owns packaged overlay confirmation | validation overlay screenshot/manual note with long pack/evidence input |

### Gate Budget

预计重型任务：resource lint 全套、`darkManifestCoverageLint final-full`、`:client:clientSmoke`、`:client:goldenScreenshot`、`localeLint`、`contractLint`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-06 完成玩家可见 manifest 全量收口。

### Canonical Artifact

canonical artifact 固定为：

1. `UI/sprite-sheets/sheet-plan.yaml`
2. `UI/sprite-sheets/key-registry.yaml`
3. `UI/sprite-sheets/dark-v1-final-full-inventory.json`
4. `assets-src/image/contact-sheets/dark-v1/`
5. `assets-src/image/manifests/phase2-visual-manifest.json`
6. `client/src/main/resources/manifests/visual-manifest.json`
7. `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`
8. `build/reports/tests/`
9. `client/build/reports/golden/`

第 7 项是 build report，不默认提交进仓库；PR 描述和 manual record 只引用 repo-relative report path、status、key counts 和 error 摘要。若后续决定提交 coverage artifact，必须先去掉或分离 `generatedAt` 等 volatile 字段。frozen 职业排除必须写入 coverage artifact，不能显示成 missing。

### Failure Rule

玩家可见 rejected cell 不得留给 PR-07 静默修补；若 final-full coverage 仍有 player-visible gap，必须回到 PR-06 或拆单独资源修复 PR。任何实现路径如果需要新增 manifest key、coverage 字段或 validation overlay 字段，必须同时更新 key registry、sheet plan、canonical manifest、runtime manifest、focused test 和本 PR 的 artifact 描述；不得只改 renderer 或只改 coverage 脚本让 gate 变绿。

### Implementation Order

1. **Inventory freeze**：生成并提交 `UI/sprite-sheets/dark-v1-final-full-inventory.json`，记录每个 player-visible family 的 `expectedCount`、`sourcePaths`、`ownerPr`、`sheetId`、`consumer`、`consumerTest` 和 `coverageExclusion`；必须包含仍在玩家主路径中的 PR-02、PR-02-1、PR-02-2、PR-03、PR-04、PR-05 owner keys，不能只统计 PR-06 新增 keys。
2. **Registry and sheet plan**：先补齐 `key-registry.yaml` 与 `sheet-plan.yaml`，再生成 prompt/raw/contact sheet；禁止先切 runtime PNG 再倒填 registry。
3. **Canonical manifest**：所有 `targetKey -> rawOutputPath` 先改 `assets-src/image/manifests/phase2-visual-manifest.json`，再跑 `syncPhase2Manifests` 更新 runtime manifest。
4. **Client consumers**：status、talent/tree、quest summary、validation overlay 必须各有实际 consumer 和 focused test；resolver-only 通过不能算 player-visible 完成。
5. **Coverage gate wiring**：先让 `darkManifestCoverageLint final-full` 支持 `expectedInventory` 输入，再跑 final-full coverage；inventory gate 未接入前，PR06 不能把 registry-only final-full 当 close 证据。
6. **Golden and manual evidence**：再跑 client focused tests/golden；如果 golden 暴露旧图或遮挡，回到 consumer/layout 修复，不用 rebaseline 掩盖。
7. **Self-audit**：PR close 前逐条核对 §6 的 key family、allowed exclusion、handoff 和 overlay layout；未完成项必须进入 PR 描述的 remaining risk，不允许留在本文件外的口头说明。

## 1. 阶段目标

1. 替换技能、状态、damage type、quest、zone、profession/tree icon。
2. 完成 manifest fallback、debug resource、missing、hidden 资源返修。
3. 将新视觉纪元覆盖到 `visual-manifest.json` 的玩家可见资源。
4. 把 PR-04 暂时复用的职业树 icon 切换到新风格资源。
5. 将验证模式运行时 overlay、active content pack summary、scenario evidence summary 的玩家可见 presentation 纳入 dark-v1 全量覆盖；setup 页 layout 仍归 PR-01，最终证据归 PR-07。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `UI/sprite-sheets/sheet-plan.yaml` | 增加 Round 8-9 和 rejected cell 返修 |
| `client/src/main/resources/dark-v1/icons/` | skill、status、damage、quest、zone、profession、tree icon |
| `assets-src/image/manifests/phase2-visual-manifest.json` | 先将玩家可见 key 的 canonical manifest 指向 dark-v1 |
| `client/src/main/resources/manifests/visual-manifest.json` | 由 `syncPhase2Manifests` 同步生成 runtime manifest |
| `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt` | 确认 status icon key 覆盖 |
| `client/src/main/kotlin/com/ktome/client/assets/TalentAssetReferences.kt` | 确认 talent/tree icon key 覆盖 |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | 将 shell quest summary 从纯文本行升级为可消费 quest marker/icon 的 row |
| `client/src/main/kotlin/com/ktome/client/render/ValidationPackSummaryText.kt` | 确认 validation overlay / active pack summary 文案 compact 且可读 |
| `client/src/main/kotlin/com/ktome/client/render/ValidationScenarioEvidenceSummaryLines.kt` | 确认 validation evidence summary 不遮挡 HUD/日志 |
| `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt` | 确认 validation scenario presentation 不引入旧风格 marker |
| `client/src/test/kotlin/com/ktome/client/ui/status/StatusIconResolverTest.kt` | 覆盖 `icon.status.*` 与 `icon.mutation.*` 双前缀解析 |
| `client/src/test/kotlin/com/ktome/client/render/TileRenderModelTest.kt` and `TileRendererCanvasTest.kt` | 覆盖 quest summary row 携带 quest icon key、空状态不带 icon、canvas/golden path 可见 |
| `client/src/test/kotlin/com/ktome/client/render/ValidationPackSummaryTextTest.kt` | 覆盖 active pack summary、manifest fallback 和长文本 |
| `client/src/test/kotlin/com/ktome/client/render/ValidationScenarioEvidenceSummaryLinesTest.kt` | 覆盖 evidence path compact、排序和最大行数 |
| `scripts/verify_dark_manifest_coverage.py` | 增加 `--expected-inventory` 输入、per-key exclusion 校验和 `expectedKeySetSource` 输出 |
| `tools/build.gradle.kts` | 将 `ktome.darkUiux.expectedInventory` 传给 `darkManifestCoverageLint`，并声明为 task input |
| `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt` | 覆盖 inventory 分母、registry 漏登记失败、allowed exclusion 输出和 malformed exclusion 失败 |

## 3. 资源范围

1. Round 8：`r08-skills-vanguard-berserker`、`r08-skills-templar-rogue`、`r08-skills-arcanist-spellblade`。
2. Round 9：`r09-status-damage`、`r09-quest-zone-profession`、`r09-fallback-debug`、`r09-rejected-polish`。
3. Round 7 剩余 item、affix、material 返修。
4. Round 2-6 中 QA rejected 的玩家可见资源返修。
5. `r09-status-damage` 同时覆盖 `icon.status.*` 和 `icon.mutation.*`；不新增 `icon_mutation` category。
6. `r09-fallback-debug` owner 固定为 PR-06，必须覆盖 `missing_visual`、hidden / resource-debug / fallback、locked/placeholder 主路径。
7. `r09-rejected-polish` owner 固定为 PR-06，只承接 PR-03/05/06 coverage artifact 已记录的 rejected cell；PR-07 只能返修这些记录，不重新拥有 sheet。
8. 当前 manifest fallback / hidden / resource-debug 主路径估算约 25-30 cell，`r09-fallback-debug` 的 64 cell 容量足够作为首选；如果开工前 inventory 盘点超过 64，必须拆出 `r09-fallback-debug-a/b` 并同步更新 `UI/PLAN.md`、`UI/pr/README.md`、sheet plan 和 key registry。

Raw sheet 生成交接：

1. 先按 PR-00 固定命令生成 Round 8-9 prompt 文件和 `prompt-index.json`。
2. 逐个执行 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --overwrite`，由脚本调用 Codex CLI 并复制最新生成图片到 `sheet-plan.yaml.rawSheetPath` 指定位置。
3. 文件名必须等于 `{sheetId}.png`，例如 `r08-skills-vanguard-berserker.png`、`r09-fallback-debug.png`、`r09-rejected-polish.png`。
4. Round 7、Round 2-6 rejected cell 返修也必须走同一 prompt 文件和 raw path 机制，不允许直接替换切分后的单 PNG。
5. PR 描述必须列出 prompt path、raw sheet path、raw sheet hash、source image count/hash 摘要、coverage artifact 和 contact sheet QA path。
6. 不得把 Codex CLI transient source folder 的真实路径写入 PR 描述、coverage artifact、manual record 或 manifest。确需说明来源时只能写 `<codex-generated-images-dir>` 占位符，并以复制后的 `rawSheetPath` 与 hash 作为合同。

Cross-PR handoff input：

| Input path | Required fields | PR-06 decision |
| --- | --- | --- |
| `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` and `UI/manual-records/ui-demo-new-visual-parity.md` | `ui-demo-new-*` golden labels, no right-panel ground loot, no bottom command hints, PR-02-1 / PR-02-2 coverage report paths | final-full inventory 必须把仍被 runtime 消费的 `ui.shell.*` 与 PR-02-2 demo map/actor/prop keys 作为 upstream-covered entries 纳入分母 |
| `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl` | `targetKey`, `sheetId`, `qaStatus`, `rejectionReason`, `playerVisible`, `ownerPr`, `evidencePath` | `playerVisible=true` 必须进入 `r09-rejected-polish` 或原 owner sheet 返修；`playerVisible=false` 只能登记为 PR-07 polish handoff |
| `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md` | checked contact sheet paths, rejected/pending rows, reviewer, residual risk | 缺记录时先补 PR-06 handoff inventory，不能按记忆决定 item/equipment 返修范围 |
| `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md` | injected key, fallback key, source/runtime manifest path, result, residual risk | fallback 仍玩家可见时必须进入 PR06 final inventory；纯 injection evidence 不算缺资源 |
| `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` | `targetKey`, `sheetId`, `qaStatus`, `rejectionReason`, `playerVisible`, `ownerPr`, `evidencePath` | player-visible actor/tile/portrait gap 不得静默转 PR-07；若不是 PR-06 sheet owner，必须拆资源修复 PR |
| `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md` | contact sheet paths, map/actor/portrait rejected rows, whitebox result, residual risk | 缺记录时先补 PR-06 handoff inventory，不能把 PR05 resource gap 合并进 Round 8-9 |
| PR-06 Round 8-9 contact sheet QA paths under `assets-src/image/contact-sheets/dark-v1/` | `targetKey`, `sourceSheetId`, `qaStatus`, `rejectionReason`, `playerVisible`, `replacementSheetId`, `evidencePath` | `pending` / `rejected` close 前必须为 0；非玩家可见 polish 才能由 PR-07 接管 |

`evidencePath` 必须是 repo-relative path。找不到上游 artifact 时，PR-06 不能凭记忆补资源；必须先补 `UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json`，记录缺失来源、conservative decision 和是否并入 `dark-v1-final-full-inventory.json`。

职业范围：

1. Release playable：`vanguard / arcanist / rogue / templar` 全量切换。
2. Dev playable/report-only：`berserker / spellblade` 全量切换。
3. Frozen excluded：`shadowblade / warden` 不生成完整 skill/talent sheet；若锁定职业卡在 UI 可见，必须使用 dark-v1 locked/fallback 表达，并写入 coverage exclusion。

Frozen profession exclusion schema：

| Field | Rule |
| --- | --- |
| `key` | canonical manifest key or planned key；不得用 family 通配符代替 |
| `family` | one of `icon.profession`, `icon.skill`, `talent.icon`, `talent.visual`, `icon.tree`, `tree` |
| `reason` | only `frozen-profession-not-player-visible` or `locked-card-uses-dark-fallback` |
| `ownerPr` | `PR-06` |
| `removalOwner` | `PR-07` or explicit future PR if frozen profession becomes playable |
| `expiresAfterPr` | `PR-07` unless a later playable-profession PR is named |
| `visibleFallbackKey` | dark-v1 key used if the locked card is visible |
| `evidencePath` | repo-relative manual record or coverage report path |

`allowedCoverageExclusions` 非空时必须逐项校验字段完整性；字段缺失、机器绝对路径、unknown reason、fallback key 不存在都必须让 `darkManifestCoverageLint final-full` 失败。

## 4. 职业树联动

1. `TalentSidebarPresenter` 的 node icon、tree icon 必须在本 PR 切换到新 skill/tree icon。
2. 不改 PR-04 的输入语义和 presentation authority。
3. 如果某个 talent 缺 icon，必须通过 `sheet-plan.yaml` 补齐，不能在 renderer 里硬编码 fallback path。
4. 职业树相关 icon 必须覆盖 learned、learnable、locked、active 四态下的可读性。
5. `icon.tree.*` 是职业树 section/header 小图标，`tree.*` 是 portrait/large visual；两套 key 保留，不在本 PR 合并。PR-06 必须分别列出切换表。
6. Node row 结构固定为：skill icon = `TalentSidebarLine.iconKey`；state badge = PR-02 `ui.state.locked/learnable/active/reserve.icon`；label/rank/cost/reason = text lines；selection marker = row `selected` / tone。不得把 skill icon、state badge、selected marker 混进同一个 manifest key。
7. PR-06 可以在 debug trace 或 accessibility metadata 中保留 `[x]`、`[+]`、`[r]`、`[*]`，但这些 ASCII 前缀不得出现在正式 talent row 可见文本里；primary dark visual cue 必须来自 state badge 与 tone；golden 必须证明 locked、learnable、reserve、active 四态同屏可读。

## 5. 非目标

1. 不改技能效果、状态结算、damage type 枚举或 quest 规则。
2. 不改音频资源，除非现有 lint 强制要求同步 manifest；如触发，必须补 `audioLint`。
3. 不把旧资源删干净作为本 PR 目标；可保留历史/debug resource fallback，但玩家主路径必须指向新风格。
4. 不改 validation scenario 选择、content pack 加载、scenario bootstrap 或 whitebox materialization 规则；只改 overlay / summary 的 presentation 与资源覆盖。

## 6. 验收标准

1. `visual-manifest.json` 玩家可见主路径不再指向旧风格资源，允许仅保留历史/debug resource fallback。
2. `darkManifestCoverageLint + ManifestResolveTest` 证明 §6.1 中所有 key family 全部可解析并已进入 dark-v1 玩家主路径；PR-02-1 shell keys 与 PR-02-2 `ui-demo-new` map/actor/prop keys 若仍被 runtime 消费，也必须进入 final-full 分母；未登记 key 不能通过 omission 缩小 final-full 分母。
3. contact sheet QA report 没有 `pending` 或 `rejected` 的玩家可见资源。
4. 职业树、HUD、背包、状态栏同时出现时，图标风格一致，不出现明显跨时代资源。
5. 产出 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`。该 report 默认不提交；PR 描述必须记录 repo-relative path、status、expected/covered/missing count 和 errors 摘要。
6. `assetLint / styleLint / manifestLint` 只作为旧资源合同和 canonical/runtime 一致性回归门禁，不作为 dark-v1 覆盖权威。
7. 验证模式 overlay / active pack summary 在 debug client 与 PR-07 packaged app 场景中不遮挡地图、HUD、日志或任务；content pack、scenario、evidence summary 不回退到旧风格 marker。

### 6.1 Final-full Expected Inventory

PR-06 开工后的第一步必须生成 `UI/sprite-sheets/dark-v1-final-full-inventory.json`，并让 `darkManifestCoverageLint final-full` 的 `expectedKeySetSource` 指向该 inventory。`key-registry.yaml` 仍是 per-key owner/fallback/consumer 真源；inventory 负责冻结 final-full 分母和 count snapshot，避免未登记 key 被漏掉。

唯一 gate 接线：

1. `scripts/verify_dark_manifest_coverage.py` 必须新增 `--expected-inventory UI/sprite-sheets/dark-v1-final-full-inventory.json`。
2. `tools/build.gradle.kts` 必须新增 `ktome.darkUiux.expectedInventory` property，并在 `darkManifestCoverageLint` 中把它传给脚本；该 property 必须进入 task input。
3. `final-full` 模式必须使用 inventory 的 exact keys 作为 `expectedKeySet`；registry 只校验每个 expected key 的 metadata。inventory key 缺 registry entry 时必须 fail fast。
4. `final-full` report 的 `expectedKeySetSource` 必须是 inventory path，`keyRegistryPath` 继续写 registry path。
5. `pr00-dry-run` 和 `owner-scope` 可以继续使用 registry 分母；PR-06 close gate 不允许使用这两个模式代替。
6. 必须新增工具测试：`DarkSpriteSheetPipelineScriptTest.finalFullUsesInventoryAsExpectedKeySetSource`、`DarkSpriteSheetPipelineScriptTest.finalFullFailsWhenInventoryKeyIsMissingFromRegistry`。

Inventory schema 至少包含：

| Field | Rule |
| --- | --- |
| `schemaVersion` | `dark-v1-final-full-inventory-v1` |
| `generatedFrom` | repo-relative source paths：canonical manifest、runtime manifest、game data indexes、screen coverage matrix、key registry |
| `families[].family` | exact family name from the table below |
| `families[].expectedCount` | generated count at PR-06 start; reviewer uses artifact count, not this document, as the numeric truth |
| `families[].keys[].key` | exact target key; no prefix-only entries |
| `families[].keys[].consumer` | primary consumer file or `manifest-only-with-reason` |
| `families[].keys[].consumerTest` | focused test or golden label |
| `families[].keys[].coverageExclusion` | null or object matching the frozen exclusion schema; exclusions are per key, not per family |
| `schemaOwner` | `PR-06` |
| `generatedBy` | script or manual command that created this inventory |

Exact family table：

| Family | Exact key shape | Inventory source | Owner sheet | Required consumer/test |
| --- | --- | --- | --- | --- |
| skill icon | `icon.skill.*`, `talent.*.icon` | game talent index and canonical manifest | `r08-skills-*` | `TalentAssetReferences` / `ManifestResolveTest` / talent golden |
| talent visual | `talent.*.visual` | game talent index and canonical manifest | `r08-skills-*` or explicit pending/exclusion | `TalentAssetReferences` / `ManifestResolveTest` |
| tree icon | `icon.tree.*` | game talent tree index and canonical manifest | `r09-quest-zone-profession` | `TalentSidebarPresenterTest` / talent golden |
| tree portrait | `tree.*` | game talent tree index and canonical manifest | `r09-quest-zone-profession` or existing portrait owner if already dark-v1 | `ManifestResolveTest` / route or talent golden |
| status icon | `icon.status.*`, `icon.mutation.*` | status definitions, mutation definitions and canonical manifest | `r09-status-damage` | `StatusIconResolverTest` |
| damage type icon | `icon.damage_type.*` | damage type manifest entries and canonical manifest | `r09-status-damage` | `ManifestResolveTest` or status/combat focused test |
| quest icon | `icon.quest.*`, including `icon.quest.objective_marker` if shell needs a generic marker | objective/quest/interactable data and canonical manifest | `r09-quest-zone-profession` | quest summary consumer test / `dark-uiux-pr06-status-quest-skill-overview` |
| zone icon | `zone.*.icon` | zone presentation data and canonical manifest | `r09-quest-zone-profession` | route/inspect consumer test or `ManifestResolveTest` |
| profession icon | `icon.profession.*` | profession index and canonical manifest | `r09-quest-zone-profession` | profession selection/talent consumer test or `ManifestResolveTest` |
| difficulty icon | `difficulty.normal.icon` and any future `difficulty.<id>.icon` | difficulty data and canonical manifest | `r09-quest-zone-profession` | validation/setup or `ManifestResolveTest` |
| fallback/debug/hidden | `missing_visual`, hidden/secret/fallback/debug player-visible keys | canonical manifest, hidden-content data and coverage report | `r09-fallback-debug` | fallback injection test/manual record |

禁止新增 `icon.zone.*` 或 `icon.difficulty.*` 作为 parallel alias。若确实要改 key schema，必须作为 public manifest contract change 单独声明，并同步更新 README、manifest lint、resolver tests 和 sample fixtures。

### 6.2 Quest Icon Consumer Contract

当前 shell quest summary 路径在 `TileRenderModel.buildShell` / `questSummaryText`，只从最新 `log.objective.*` token 渲染文本。PR-06 必须把这个 player-visible row 接到实际 visual consumer，不能只让 `icon.quest.*` 通过 manifest resolver。

执行细则：

1. Quest summary row 必须变成携带 icon 的 `TileTextRow`，icon 通过 visual resolver 从 `icon.quest.*` 解析；缺 key 时使用 registered fallback，并在 coverage/report 中可定位。
2. 不改 quest/world progress 规则；不得在 `core` 或 `game` 为 UI 临时新增第二套 quest state。
3. PR-06 固定最小路径为 generic `icon.quest.objective_marker`，不扩 `RenderSnapshot`、`WorldProgress` 或 quest schema。typed quest/objective metadata 如果未来需要，必须单独作为 stable snapshot / public contract change。
4. 新增 client-only owner `QuestSummaryIconResolver` 或等价小对象，唯一 mapping 表为：`log.objective.activate`、`log.objective.progress`、`log.objective.complete` -> `icon.quest.objective_marker`。该 resolver 只看 render log token key，不解析 localized text。
5. `icon.quest.armory_key`、`icon.quest.seal_key` 只能用于真实钥匙/目标语义，不能为了“有图标”硬塞到所有 objective log。
6. 空状态保留 `ui.shell.quest.none` 文案；空状态不消耗 quest icon，不计入 quest icon 上屏证明。
7. 测试至少覆盖 `TileRenderModelTest.shellQuestSummaryCarriesGenericQuestMarkerForObjectiveLog` 和 `TileRenderModelTest.shellQuestSummaryDoesNotUseQuestIconForEmptyState`。如果最终只落到 `TileRendererCanvasTest`，文档必须把本段测试名收敛成实际文件名。
8. Golden `dark-uiux-pr06-status-quest-skill-overview` 必须同时展示 status icon、skill/talent icon 和 quest marker/icon；只出现文本不算通过。

### 6.2.1 Status Icon Nullability And Fallback

PR-06 不允许 status icon 行为继续由 `mapNotNull` 静默决定。规则固定为：

1. 正式 status / mutation presentation 的 `iconKey` 不得为 null；缺失时必须进入 final inventory 的 missing/fallback 路径，并由 `StatusIconResolverTest` 失败或记录 fallback。
2. `StatusPresentationBuilder.buildZoneEffect` 当前 `iconKey = null` 不是 `icon.status.*` / `icon.mutation.*` 覆盖分母。若 PR-06 要把 zone effect 变成 status icon，必须新增明确 key 并进 inventory；若不做，inventory 中写 `manifest-only-with-reason=zone-effect-text-only-not-status-icon-family`。
3. unknown status icon key 必须走 registered fallback 并在 test/report 中可定位，不得静默 drop。
4. 必须补 `StatusIconResolverTest.resolvesStatusAndMutationIcons`、`StatusIconResolverTest.nullIconStatusUsesDocumentedDropOrFallbackRule`、`StatusIconResolverTest.unknownStatusIconUsesRegisteredFallback`。

### 6.3 Validation Overlay Layout Contract

`ValidationPackSummaryText` 与 `ValidationScenarioEvidenceSummaryLines` 当前会把 active pack、namespace、overlay ops、touched content ids 和 evidence path 直接 join 成长文本。PR-06 必须先冻结 compact owner 再改 UI：

PR-06 固定新增 `ValidationOverlaySummaryPresenter` 或等价 owner。它负责接收 active pack summary、key warning summary 和 evidence summary，输出 bounded rows；`ValidationPackSummaryText` 与 `validationScenarioEvidenceSummaryLines` 只保留 leaf formatter，不各自实现 row budget，避免多处截断逻辑。

| Item | Rule |
| --- | --- |
| section order | preset -> seed -> zone/floor -> active packs -> namespaces -> overlay ops -> touched content -> key warnings -> evidence summary |
| max rows | validation overlay summary 主体最多 12 行；超过时 fold 为 `+N more` 行 |
| max line length | 单行 display text 最多 96 chars；超过按 token 边界 deterministic truncation，不切断 repo-relative path segment 中的文件名 |
| sorting | pack ids、namespaces、overlay ops、touched content ids、warning keys 全部稳定排序 |
| empty state | 使用 locale token，不输出空字符串或 `null` |
| path display | 只显示 repo-relative path；禁止 macOS home、tmp、Windows drive absolute path 或 transient generated path |
| fallback warning | visual/audio/locale warning 分栏保留；不能合并成一串失去 owner 的文本 |

必须补测试：

1. `ValidationOverlaySummaryPresenterTest.boundsRowsAndLineLength`
2. `ValidationOverlaySummaryPresenterTest.keepsEvidencePathsRepoRelative`
3. `ValidationPackSummaryTextTest.overlayOpsAreSortedAndBounded`
4. `ValidationScenarioEvidenceSummaryLinesTest.compactsRepoRelativeEvidencePaths`
5. `ValidationScenarioEvidenceSummaryLinesTest.rejectsOrRedactsMachineAbsolutePaths`

Golden 必须包含长列表和 fallback warning 场景；只截常规成功路径不能证明 overlay 在真实验证模式下可读。

Coverage artifact 要求：

PR-06 使用 PR-00 固定的 final-full schema，不在本 PR 自定义第二套字段。final-full 至少要求 `expectedKeySet`、`coveredKeySet`、`missingKeys`、`oldStylePlayerVisibleKeys`、`pendingOrRejectedPlayerVisibleCells`、`fallbackKeyUsage`、`allowedFallbackKeys`、`allowedCoverageExclusions`、`sourceSheetIds`。若本 PR 扩展 `allowedCoverageExclusions` 字段内容，只能在 PR-00 schema 允许的对象结构内补字段，并同步工具测试。

当前 coverage schema 真源为 PR-00 文档和 `scripts/verify_dark_manifest_coverage.py` 的实际输出；`UI/sprite-sheets/coverage-schema.md` 未落盘前不得把它当作开发输入或 PR06 blocking artifact。

`allowedCoverageExclusions` 只从 inventory 的 per-key `coverageExclusion` 派生。final-full 计算必须区分：

1. expected and covered key
2. expected but allowed-excluded key
3. expected and missing key
4. expected and pending key
5. expected and old-style key

allowed-excluded key 不进入 `missingKeys` / `pendingOrRejectedPlayerVisibleCells`，但必须进入 `allowedCoverageExclusions`，并保留 `key`、`family`、`reason`、`visibleFallbackKey`、`evidencePath`、`removalOwner`、`expiresAfterPr`。必须新增工具测试：`DarkSpriteSheetPipelineScriptTest.finalFullReportsAllowedFrozenProfessionExclusions`、`DarkSpriteSheetPipelineScriptTest.finalFullRejectsMalformedCoverageExclusion`。

Fallback/debug final inventory 必须在开工前生成并随 PR 提交：

| Inventory bucket | Required action | Target sheet |
| --- | --- | --- |
| `missing_visual` | 生成 dark-v1 missing/fallback 主视觉 | `r09-fallback-debug` |
| `tile.hidden` / hidden entrance / secret zone | 生成 hidden/secret readable icon or portrait | `r09-fallback-debug` 或对应 zone sheet |
| `category=debug` player-visible starter/item visuals | 若玩家可见则迁移；纯 debug/history 可列入 allowed fallback | `r09-fallback-debug` 或 Round 7 owner sheet |
| debug-only tiles / missing visual sentinel | 不允许 client ASCII renderer 或 ASCII manifest 字段；纯 debug/history resource fallback 可列入 `allowedFallbackKeys` 并说明原因 | `r09-fallback-debug` 或 allowed fallback |
| PR-03/05 rejected cells | 只处理 coverage artifact 中列明的 rejected cell | `r09-rejected-polish` |

如果 final inventory 中 manifest fallback / debug resource / hidden / rejected cell 总数超过当前 sheet capacity，PR-06 必须先新增 `r09-fallback-debug-*` 或 `r09-rejected-polish-*` sheetId，并同步更新 `UI/PLAN.md`、`UI/pr/README.md` 和 key registry；不得把超额 key 留作 silent pending。
PR-06 close 前玩家可见 rejected cell 必须为 0；PR-07 只能处理 PR-06 coverage artifact 中记录的非玩家可见 polish 项或后验发现的单点问题。若 PR-07 发现玩家可见 rejected cell，视为 PR-06 未完成，不能在 PR-07 静默修补。

Manifest key 切换表必须至少包含：

| Key family | Required action |
| --- | --- |
| `icon.skill.*` / `talent.*.icon` | release/dev playable 全量切到 dark-v1 |
| `talent.*.visual` | release/dev playable 全量切到 dark-v1 或列入 explicit pending |
| `icon.tree.*` | tree header 小图标切到 dark-v1 |
| `tree.*` | tree portrait 切到 dark-v1 |
| `icon.status.*` / `icon.mutation.*` | 全量切到 dark-v1 |
| `icon.damage_type.*` | 全量切到 dark-v1 |
| `icon.quest.*` | 全量切到 dark-v1，并证明至少一个 quest summary consumer 上屏 |
| `zone.*.icon` | 全量切到 dark-v1；禁止新增 `icon.zone.*` alias |
| `icon.profession.*` | release/dev playable 全量切到 dark-v1；frozen 只能走 allowed exclusion |
| `difficulty.normal.icon` | 切到 dark-v1；未来新增 difficulty 使用 `difficulty.<id>.icon` |
| `missing_visual` / hidden / debug resource fallback | Round 9 polish 或 allowed fallback 说明 |

## 7. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew syncPhase2Manifests manifestLint
```

本 PR 的 dark gate 必须使用显式 final-full 命令：

```bash
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=final-full \
  -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json
```

focused client tests 必须单独跑，避免超长 Gradle 命令掩盖漏测：

```bash
./gradlew :client:test \
  --tests com.ktome.client.ui.status.StatusPresentationModelTest \
  --tests com.ktome.client.ui.status.StatusIconResolverTest \
  --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest \
  --tests com.ktome.client.input.InputHandlerTest \
  --tests com.ktome.client.render.TileRenderModelTest \
  --tests com.ktome.client.render.TileRendererCanvasTest \
  --tests com.ktome.client.render.ValidationOverlaySummaryPresenterTest \
  --tests com.ktome.client.render.ValidationPackSummaryTextTest \
  --tests com.ktome.client.render.ValidationScenarioEvidenceSummaryLinesTest \
  --tests com.ktome.client.assets.ManifestResolveTest
```

client evidence 与治理 gate：

```bash
./gradlew :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint
./gradlew verifyChanged
```

要求 `oldStylePlayerVisibleKeys=[]` 且 `pendingOrRejectedPlayerVisibleCells=[]`。不得用 owner-scope 代替本 PR close gate。`verifyChanged` 当前仍是共享 preflight / impact routing，不替代前置 final-full coverage；PR 描述必须单独粘贴 final-full coverage command/result。若 PR06 要把 `verifyChanged` dark route 从 `darkManifestCoveragePr00DryRun` 切到 final-full，必须同步更新 root routing、`tools/build.gradle.kts` 输入、`DarkSpriteSheetPipelineScriptTest` 和 `./scripts/verify-bootstrap.sh` 验证。

若改动 coverage task wiring、Gradle task、bootstrap 脚本或 lint schema，还必须运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew maintainabilityLint
./scripts/verify-bootstrap.sh
```

## 8. 人工白盒

1. 打开主 HUD、背包、状态栏、职业树、任务提示，确认 icon 风格统一。
2. 触发至少 3 类状态和 2 类技能预览，确认状态/技能图标不混淆。
3. 故意注入一个 missing key 的测试路径，确认 manifest fallback 风格可接受且 report 可定位。
4. 打开 validation overlay / active pack summary，确认 scenario、pack、evidence summary 可读且不遮挡 HUD/日志。
5. 用长 active pack list、长 touched content list 和 evidence path 触发 truncation，确认排序、折叠和 repo-relative path 展示稳定。
6. 必填证据：skill/status/quest/profession tree 同屏截图、manifest coverage artifact、fallback injection record、validation overlay 长列表截图、`dark-uiux-pr07-validation-overlay` 的 PR-07 evidence 引用。

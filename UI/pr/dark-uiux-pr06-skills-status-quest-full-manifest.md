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
| `UI06-M04` | status / quest / skill presentation + quest summary row consumer | `client` | `StatusPresentationModelTest`, `StatusIconResolverTest`, `TileRenderModelTest.shellQuestSummary*`, `TileRendererCanvasTest.shellQuestSummary*` | `clientSmoke`, `goldenScreenshot` | `build/reports/tests/`, `client/build/reports/golden/dark-uiux-pr06-status-quest-skill-overview.png` | `required` |
| `UI06-M05` | PR-03/05 rejected cell 返修 | `assets` / `tools` | `dark-v1-final-full-inventory.json` diff check | `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full` | `UI/sprite-sheets/dark-v1-final-full-inventory.json`, `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` | `N/A` |
| `UI06-M06` | governance inheritance | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |
| `UI06-M07` | validation overlay / pack summary | `client` | `ValidationPackSummaryTextTest`, `ValidationScenarioEvidenceSummaryLinesTest`, validation smoke | `clientSmoke`, `goldenScreenshot` | `build/reports/tests/`, `client/build/reports/golden/` | `required` |
| `UI06-M08` | cross-family same-screen visual coherence | `client` / `assets` | `TileRendererCanvasTest.sameScreenOverview*` or `SameScreenOverviewTest`, contact-sheet side-by-side QA | `goldenScreenshot` | `client/build/reports/golden/dark-uiux-pr06-status-quest-skill-overview.png`, `UI/manual-records/dark-uiux-pr06-overview-screenshot.md` | `required` |
| `UI06-M09` | sheet QA escalation policy | `assets` | QA rejection round-count summary per sheet | `darkSpriteSheetLint`, `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full` | `assets-src/image/contact-sheets/dark-v1/`, `UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md` | `required` |
| `UI06-M10` | long-session readability | `client` / `docs` | manual long-session fatigue record | PR-07 re-audit | `UI/manual-records/dark-uiux-pr06-long-session-fatigue.md` + `UI/manual-records/dark-uiux-pr07-long-session-reaudit.md` | `required` |
| `UI06-M11` | resource rework PR contract when rejection threshold is hit | `assets` / `tools` | `DarkSpriteSheetPipelineScriptTest` rework fixture | rework PR: `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope`; PR-06 main: `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full` | rework PR contact-sheet QA report and inventory history row; PR-06 main final-full report | `conditional` |
| `UI06-M12` | actor status runtime definition bridge | `core` | `StatusDefinitionsTest` | `:core:test --tests com.ktome.core.status.StatusDefinitionsTest` | `core/build/reports/tests/test/` | `N/A` |
| `UI06-M13` | official status/profession schema bridge | `game` | `StatusSchemaContractTest`, `ProfessionSchemaTest` | `:game:test --tests com.ktome.game.data.StatusSchemaContractTest --tests com.ktome.game.data.ProfessionSchemaTest` | `game/build/reports/tests/test/` | `N/A` |

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
| `UI06-M08` | PR-02-1 shell, PR-04 talent rows, PR-05 actor/portrait, PR-06 status/quest/skill resources | PR-06 owns same-screen coherence; PR-07 only re-audits final all-screen polish | overview golden and manual screenshot must be referenced in PR description |
| `UI06-M09` | PR-00 sheet/key registry contract and PR-03/05 rejected QA artifacts | PR-06 must split a resource repair PR before PR-07 when escalation threshold is hit | per-sheet reject count and split-PR decision recorded before golden/manual evidence |
| `UI06-M10` | PR-07 final all-screen polish and packaged-app evidence | PR-06 owns the long-session record; PR-07 consumes it as audit input and links any new fatigue finding back to PR-06 | at least 60 minutes of continuous play notes with fatigue/accent/readability findings, plus PR-07 re-audit path |
| `UI06-M11` | PR-00 sheet/key registry contract and PR-06 contact-sheet QA reports | PR-06 cannot close while a threshold-triggered resource rework PR is unmerged | rework PR owner-scope result, merged replacement inventory row, historical sheet ids, and PR-06 main final-full result |
| `UI06-M12` | `core` status runtime contract | PR-06 owns the status icon family bridge only; no status gameplay rule change | focused `:core:test` output |
| `UI06-M13` | official game schema and loader | PR-06 owns the status/profession visual-key bridge only; no gameplay schema expansion | focused `:game:test` output |

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
   - Status consumer must keep actor status in the HUD row, keep actor mutation visuals on their mutation/inspect surfaces, and must not treat zone effects or telegraphs as implicit status icons.
   - Quest consumer must add the icon-bearing shell row, the resolver mapping, empty-state exclusion, and focused tests in the same PR slice.
   - Validation overlay consumer must route compact/detailed row budgeting through one presenter owner instead of duplicating truncation in leaf formatters.
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
| `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt` | 移除 `mapNotNull` silent drop；正式 actor status 缺 icon 时请求 `icon.status.<typeId>` 并进入 registered manifest fallback，不新增分母外 synthetic key |
| `client/src/main/kotlin/com/ktome/client/ui/status/StatusPresentationModel.kt` | 冻结 `buildZoneEffect` 不属于当前 status icon HUD owner；若未来进入 HUD，必须新增明确 zone/effect key family 和 inventory 分母 |
| `client/src/main/kotlin/com/ktome/client/assets/TalentAssetReferences.kt` | 确认 talent/tree icon key 覆盖 |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | 将 shell quest summary 从纯文本行升级为可消费 quest marker/icon 的 row |
| `client/src/main/kotlin/com/ktome/client/render/ValidationPackSummaryText.kt` | 确认 validation overlay / active pack summary 文案 compact 且可读 |
| `client/src/main/kotlin/com/ktome/client/render/ValidationScenarioEvidenceSummaryLines.kt` | 确认 validation evidence summary 不遮挡 HUD/日志 |
| `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt` | 确认 validation scenario presentation 不引入旧风格 marker |
| `client/src/test/kotlin/com/ktome/client/ui/status/StatusIconResolverTest.kt` | 覆盖 actor status `icon.status.*` 解析和 null/unknown fallback；mutation 使用独立 actor mutation consumer test |
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
5. `r09-status-damage` 同时覆盖 `icon.status.*`、`icon.mutation.*` 和 `icon.damage_type.*`；不新增 `icon_mutation` category。
   - status、mutation、damage type 必须保留三套 visual grammar：status 使用 transient/open condition badge；mutation 使用 closed scar/etching 或 body-change framing；damage type 使用 elemental/physical silhouette with hard outline。三者可以共用 sheet，但 contact-sheet QA 必须做 side-by-side 区分。
6. `r09-fallback-debug` owner 固定为 PR-06，必须覆盖 `missing_visual`、hidden / resource-debug / fallback、locked/placeholder 主路径。
7. `r09-rejected-polish` owner 固定为 PR-06，只承接 PR-03/05/06 coverage artifact 已记录的 rejected cell；PR-07 只能返修这些记录，不重新拥有 sheet。
8. 当前 manifest fallback / hidden / resource-debug 主路径估算约 25-30 cell，`r09-fallback-debug` 的 64 cell 容量足够作为首选；如果开工前 inventory 盘点超过 64，必须拆出 `r09-fallback-debug-a/b` 并同步更新 `UI/PLAN.md`、`UI/pr/README.md`、sheet plan 和 key registry。
9. 任一 sheet 实际 cell 使用率达到 `>= 80%` 时，必须在 final-full inventory 中标记 `nearCapacityWarn=true`，并记录是否需要后续 `r0X-<family>-extra` sheet；该 warning 不阻塞 PR06 close，但不能省略。
10. 任一 sheet 累计 `>= 2` 轮 contact-sheet rejection，或同一 sheet 出现两次以上 player-visible cell QA reject，必须在进入 golden/manual evidence 前决定拆出独立资源修复 PR（命名规则 `r0X-<family>-rework`）。该修复 PR 必须在 PR-07 前合入，不能用 borderline cell 推动 PR06 close。

资源 QA 补充合同：

1. `r08-skills-*` 与 `r09-status-damage` 的 contact-sheet QA 必须做 cross-sheet side-by-side：随机抽 4 个 skill cell、4 个 status/mutation cell、4 个 damage type cell，并排验证 `32px` 下 silhouette weight 不收敛。
2. `r08-skills-*` 每张 sheet 必须保留 `>= 15%` expansion reserve cell，并在 QA 报告中显式标记。reserve 释放需要 design-director approval，不能用来临时塞无主资源。
3. `talent.*.visual` portrait 不得使用 merged cell。若需要 portrait 尺寸，必须放入 `large-sheet` 的 256px 单格，或拆出独立 `talent-visual` / `tree.*` owner sheet；若 PR-06 暂不交付该 portrait visual，必须进入 explicit pending / allowed exclusion，并写清 removal owner。`icon-sheet` 仍保持每个 128px cell 一个主体。
4. `icon.profession.*` 必须验证 `128 / 48 / 24` 三种尺寸的可读性。若单一 source 缩放不能通过，必须在 sheet plan 中定义 `icon.profession.<id>.lg/md/sm` 或等价多尺寸 variant，不能依赖 GPU 缩放兜底。
5. `r09-fallback-debug` / `r09-rejected-polish` contact-sheet QA 必须使用 four-quadrant layout：missing sentinel、hidden/secret、debug/resource fallback、rejected polish 分区摆放，确认 hidden/debug/missing/fallback 不互相误读。
6. Multi-size contact-sheet QA must cover runtime scale, not only 32px: `r08-skills-*` samples `16/24/32/48px`; `r09-status-damage` samples status/mutation/damage type at `16/24/32px`; `r09-quest-zone-profession` samples quest/zone icons at `12/16/24/32px`; profession keeps `128/48/24px`. Any sub-32px collapse requires outline reinforcement or explicit size variant keys before PR-06 close.
7. Skill cooldown overlay belongs to PR-06 same-screen contract: contact-sheet QA only proves subject identity, so the client/golden evidence must also show available / cooldown / passive / locked or not-yet-learned skill icon states without breaking dark-v1 era.

rework PR 合同：

1. 触发 §3.10 后，rework PR 使用新分支 / 新 PR，但 owner role 仍为 `assets`，并继承 PR-06 sheet owner。
2. rework PR close gate 必须跑 `owner-scope`，只验该 sheet 替换涉及的 keys；不得用 registry-only 结果代替。rework 合入 PR-06 主 PR 后，主 PR 必须重新跑全量 `final-full`。
3. rework PR 必须产出新 contact-sheet QA report，并替换 inventory 中对应 sheet row；原 sheetId 写入 `historicalSheetIds`。
4. rework PR 必须在 PR-06 主 PR close 前 merge；PR-07 只 audit rework 合入后的 final state，不重新接管 rework sheet。

Raw sheet 生成交接：

1. 先按 PR-00 固定命令生成 Round 8-9 prompt 文件和 `prompt-index.json`。
2. 逐个执行 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --overwrite`，由脚本调用 Codex CLI 并复制最新生成图片到 `sheet-plan.yaml.rawSheetPath` 指定位置。
3. 文件名必须等于 `{sheetId}.png`，例如 `r08-skills-vanguard-berserker.png`、`r09-fallback-debug.png`、`r09-rejected-polish.png`。
4. Round 7、Round 2-6 rejected cell 返修也必须走同一 prompt 文件和 raw path 机制，不允许直接替换切分后的单 PNG。
5. PR 描述必须列出 prompt path、raw sheet path、raw sheet hash、source image count/hash 摘要、coverage artifact 和 contact sheet QA path。
6. 不得把 Codex CLI transient source folder 的真实路径写入 PR 描述、coverage artifact、manual record 或 manifest。确需说明来源时只能写 `<codex-generated-images-dir>` 占位符，并以复制后的 `rawSheetPath` 与 hash 作为合同。raw sheet hash 算法固定为 `git hash-object <rawSheetPath>`，reviewer 必须能用同一命令复算；若生成脚本更换 generator 或输出路径策略，必须同步更新本段和脚本契约测试。

Cross-PR handoff normalization：

`UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json` 是必经 normalization artifact，不只是 missing-artifact fallback。它从 PR-02-1 / PR-02-2 evidence、PR-03/05 JSONL、manual records、coverage reports 和 owner contracts 合成 `playerVisible`、`ownerPr`、`evidencePath`、`qaStatus` 与 `conservativeDecision`，并记录每个字段的 derivation source。PR-06 final-full inventory 只能消费 normalized handoff；不得直接把旧 schema JSONL 当作 final-full handoff truth。

| Input path | Required fields | PR-06 decision |
| --- | --- | --- |
| `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` and `UI/manual-records/ui-demo-new-visual-parity.md` | `ui-demo-new-*` golden labels, no right-panel ground loot, no bottom command hints, PR-02-1 / PR-02-2 coverage report paths | final-full inventory 必须把仍被 runtime 消费的 `ui.shell.*` 与 PR-02-2 demo map/actor/prop keys 作为 upstream-covered entries 纳入分母 |
| `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl` | raw fields: `targetKey`, `sheetId`, `qaStatus`, `rejectionReason`, `reviewedAt`, `reviewer`; normalized handoff adds `playerVisible`, `ownerPr`, `evidencePath` | `playerVisible=true` 必须进入 `r09-rejected-polish` 或原 owner sheet 返修；`playerVisible=false` 只能登记为 PR-07 polish handoff |
| `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md` | checked contact sheet paths, rejected/pending rows, reviewer, residual risk | 缺记录时先补 PR-06 handoff inventory，不能按记忆决定 item/equipment 返修范围 |
| `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md` | injected key, fallback key, source/runtime manifest path, result, residual risk | fallback 仍玩家可见时必须进入 PR06 final inventory；纯 injection evidence 不算缺资源 |
| `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` | raw fields: `targetKey`, `sheetId`, `qaStatus`, `rejectionReason`, `reviewedAt`, `reviewer`; normalized handoff adds `playerVisible`, `ownerPr`, `evidencePath` | player-visible actor/tile/portrait gap 不得静默转 PR-07；若不是 PR-06 sheet owner，必须拆资源修复 PR |
| `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md` | contact sheet paths, map/actor/portrait rejected rows, whitebox result, residual risk | 缺记录时先补 PR-06 handoff inventory，不能把 PR05 resource gap 合并进 Round 8-9 |
| PR-06 Round 8-9 contact sheet QA paths under `assets-src/image/contact-sheets/dark-v1/` | `targetKey`, `sourceSheetId`, `qaStatus`, `rejectionReason`, `playerVisible`, `replacementSheetId`, `evidencePath` | `pending` / `rejected` close 前必须为 0；非玩家可见 polish 才能由 PR-07 接管 |

`evidencePath` 必须是 repo-relative path。normalized handoff 对 `DRY_RUN`、`reviewedAt=null`、缺 `playerVisible`、缺 `ownerPr` 或缺 `evidencePath` 的行必须 fail fast，除非登记 explicit `conservativeDecision` 并说明是否并入 `dark-v1-final-full-inventory.json`。找不到上游 artifact 时，PR-06 不能凭记忆补资源；也必须通过同一个 normalization artifact 记录缺失来源和 conservative decision。

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
| `playerVisibility` | one of `hidden` (preferred), `locked-with-coming-soon-label`, `locked-with-fallback-only` |
| `productMessagingKey` | locale token for the "unavailable / development preview" label；`playerVisibility != hidden` 时必填，且必须与普通 unlock/requirement 文案区分 |
| `hoverTooltipContentKey` | locked card hover tooltip locale token；`playerVisibility != hidden` 时必填，且不得复用正式 profession description |
| `hoverInteraction` | one of `none`, `dev-preview-tooltip`; release player-visible locked card 必须使用 `dev-preview-tooltip` 或隐藏 |
| `saveSlotDisposition` | one of `hidden`, `fallback-with-banner`, `disabled-with-explanation`; old save references frozen profession 时必填 |
| `deathSummaryDisposition` | one of `normal-render`, `fallback-with-banner`, `hide`; death/run summary 可能展示 frozen/dev playable profession 时必填 |

`allowedCoverageExclusions` 非空时必须逐项校验字段完整性；字段缺失、机器绝对路径、unknown reason、fallback key 不存在都必须让 `darkManifestCoverageLint final-full` 失败。`playerVisibility=locked-with-fallback-only` 是 forbidden disposition，必须由 `DarkSpriteSheetPipelineScriptTest.finalFullRejectsLockedWithFallbackOnlyDisposition` 或等价 coverage test 拦截。

## 4. 职业树联动

1. `TalentSidebarPresenter` 的 node icon、tree icon 必须在本 PR 切换到新 skill/tree icon。
2. 不改 PR-04 的输入语义和 presentation authority。
3. 如果某个 talent 缺 icon，必须通过 `sheet-plan.yaml` 补齐，不能在 renderer 里硬编码 fallback path。
4. 职业树相关 icon 必须覆盖 learned、learnable、locked、active 四态下的可读性。
5. `icon.tree.*` 是职业树 section/header 小图标，`tree.*` 是 portrait/large visual；两套 key 保留，不在本 PR 合并。PR-06 必须分别列出切换表。
6. Node row 结构固定为：skill icon = `TalentSidebarLine.iconKey`；state badge = PR-02 `ui.state.locked/learnable/active/reserve.icon`；label/rank/cost/reason = text lines；selection marker = row `selected` / tone。不得把 skill icon、state badge、selected marker 混进同一个 manifest key。
7. PR-06 可以在 debug trace 或 accessibility metadata 中保留 `[x]`、`[+]`、`[r]`、`[*]`，但这些 ASCII 前缀不得作为 talent name 文本的一部分回到正式 row。正式 row 仍必须保留一个 player-visible secondary cue：可以是 state badge 内的 1-char glyph，也可以是 badge 前的固定 marker slot；不能只靠 tone / color 区分 locked、learnable、reserve、active 四态。
8. state badge 的 silhouette 必须在 `32px`、focused/selected overlay、以及 color-blind simulation（protanopia / deuteranopia / tritanopia）下仍可区分；selected/focus highlight 不得覆盖 state badge 的位置、形状或主色。

## 5. 非目标

1. 不改技能效果、状态结算、damage type 枚举或 quest 规则。
2. 不改音频资源，除非现有 lint 强制要求同步 manifest；如触发，必须补 `audioLint`。
3. 不把旧资源删干净作为本 PR 目标；可保留历史/debug resource fallback，但玩家主路径必须指向新风格。
4. 不改 validation scenario 选择、content pack 加载、scenario bootstrap 或 whitebox materialization 规则；只改 overlay / summary 的 presentation 与资源覆盖。第三方 content pack visual asset 的 dark-v1 style certification 不在 PR-06 实装范围，但 PR-06 冻结 fallback disposition：未声明 `ktome-dark-fantasy-sprite-ui-v1` 或声明其它 styleTag 的 third-party player-visible visual，不得直接破坏 dark-v1 同屏一致性；必须走 dark-v1 stub/fallback 或命名 follow-up `UI07-content-pack-style-certification`。

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

1. 必须新增 deterministic generator，例如 `scripts/generate_dark_final_full_inventory.py` 或等价 Gradle task；手写 inventory 不能作为生成路径。
2. `scripts/verify_dark_manifest_coverage.py` 必须新增 `--expected-inventory UI/sprite-sheets/dark-v1-final-full-inventory.json`。
3. `tools/build.gradle.kts` 必须新增 `ktome.darkUiux.expectedInventory` property，并在 `darkManifestCoverageLint` 中把它传给脚本；该 property 必须进入 task input。
4. `final-full` 模式必须使用 inventory 的 exact keys 作为 `expectedKeySet`；registry 只校验每个 expected key 的 metadata。inventory key 缺 registry entry 时必须 fail fast。
5. `final-full` report 的 `expectedKeySetSource` 必须是 inventory path，`keyRegistryPath` 继续写 registry path。
6. `pr00-dry-run` 和 `owner-scope` 可以继续使用 registry 分母；PR-06 close gate 不允许使用这两个模式代替。
7. generator 必须写入 source digest / source snapshot metadata；当 canonical manifest、runtime manifest、game data indexes、screen coverage matrix、key registry 或 normalized handoff 变化且 inventory 未重生成时，`darkManifestCoverageLint final-full` 必须 fail stale inventory。
8. 必须新增工具测试：`DarkSpriteSheetPipelineScriptTest.finalFullUsesInventoryAsExpectedKeySetSource`、`DarkSpriteSheetPipelineScriptTest.finalFullFailsWhenInventoryKeyIsMissingFromRegistry`、`DarkSpriteSheetPipelineScriptTest.finalFullRejectsStaleInventorySourceDigest`、`DarkSpriteSheetPipelineScriptTest.finalFullInventoryGenerationIsDeterministic`、`DarkSpriteSheetPipelineScriptTest.finalFullRejectsMalformedManualOverride`。

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
| `families[].keys[].historicalSheetIds` | optional list；记录该 key 曾经 reside 的 sheetId，rework 替换时必填 |
| `families[].keys[].manualOverrideReason` | optional；只能解释 deterministic source 冲突，不能新增或删除 expected key |
| `families[].historicalSheetIds` | optional list；记录该 family owner sheet 的替换历史 |
| `sourceDigests` | generated source file digest map；key 为 repo-relative source path，value 为 stable digest |
| `schemaOwner` | `PR-06` |
| `generatedBy` | deterministic generator id and command line；不得写 manual-only command |

Exact family table：

| Family | Exact key shape | Inventory source | Owner sheet | Required consumer/test |
| --- | --- | --- | --- | --- |
| skill icon | `icon.skill.*`, `talent.*.icon` | game talent index, inscription active-ability icon index if it reuses `icon.skill.*`, and canonical manifest | `r08-skills-*` | `TalentAssetReferences` / inscription panel or shop consumer test / `ManifestResolveTest` / talent golden + skill cooldown overlay focused test |
| talent visual | `talent.*.visual` | game talent index and canonical manifest | `r08-skills-*` or explicit pending/exclusion | `TalentAssetReferences` / `ManifestResolveTest` |
| tree icon | `icon.tree.*` | game talent tree index and canonical manifest | `r09-quest-zone-profession` | `TalentSidebarPresenterTest` / talent golden |
| tree portrait | `tree.*` | game talent tree index and canonical manifest | `r09-quest-zone-profession` or existing portrait owner if already dark-v1 | `ManifestResolveTest` / route or talent golden |
| status icon | `icon.status.*` | status definitions and canonical manifest | `r09-status-damage` | `StatusDefinitionsTest` + `StatusSchemaContractTest` + `StatusIconResolverTest` + HUD overflow/fold + status badge overflow focused tests |
| mutation icon | `icon.mutation.*` | elite mutation definitions, actor mutation snapshots and canonical manifest | `r09-status-damage` | actor mutation inspect/target-card consumer test + `ManifestResolveTest` |
| damage type icon | `icon.damage_type.*` | damage type manifest entries and canonical manifest | `r09-status-damage` | `ManifestResolveTest` and weapon/equipment tooltip focused test and damage float focused test and resistance panel focused test |
| quest icon | `icon.quest.*`, including `icon.quest.objective_marker` if shell needs a generic marker | objective/quest/interactable data and canonical manifest | `r09-quest-zone-profession` | quest summary consumer test / `dark-uiux-pr06-status-quest-skill-overview` |
| zone icon | `zone.*.icon` | zone presentation data and canonical manifest | `r09-quest-zone-profession` | route/inspect consumer test or `ManifestResolveTest` |
| profession icon | `icon.profession.*` | profession index and canonical manifest | `r09-quest-zone-profession` | profession selection consumer test + talent header/log consumer test + character/save/death surface audit or explicit `surface-not-yet-implemented` inventory entry + `ManifestResolveTest` |
| difficulty icon | `difficulty.normal.icon` and any future `difficulty.<id>.icon` | difficulty data and canonical manifest | `r09-quest-zone-profession` | validation/setup or `ManifestResolveTest` |
| fallback/debug/hidden | `missing_visual`, hidden/secret/fallback/debug player-visible keys | canonical manifest, hidden-content data and coverage report | `r09-fallback-debug` | fallback injection test/manual record + disposition matrix |

禁止新增 `icon.zone.*` 或 `icon.difficulty.*` 作为 parallel alias。若确实要改 key schema，必须作为 public manifest contract change 单独声明，并同步更新 README、manifest lint、resolver tests 和 sample fixtures。

`icon.status.*` 与 `icon.skill.*` 的边界也必须硬冻结：即使某个 actor status 来自技能、职业或临时 buff，它的 `StatusDefinitions.iconKey`、status schema `visualKey/iconKey` 也不得复用 `icon.skill.*`。若想保持视觉亲缘，只能在 `r09-status-damage` 中建立 dedicated `icon.status.*` key，并让 manifest / status schema / runtime definition 三者同源。

Profession selection 的 consumer 起点固定为 `ProfessionPlayerCreationOption.iconKey`，该字段必须来自 profession schema `iconKey`，不能由 `PlayerCreationPanel` 或 client 侧按 id 拼接。若当前 UI 只完成数据通路而尚未绘制可见 icon，final-full inventory 必须对 selection rendering surface 写明 `manifest-only-with-reason=selection-icon-render-not-yet-implemented` 或命名同 PR06 owner 的补实现项；不能把纯文本 selection 当作 profession icon 上屏证明。

Race icon 不属于 `icon.profession.*` 分母。当前 race schema 复用 profession / monster icon 只能视为历史 placeholder，PR-06 final-full 不得把 race selection 计入 profession 或 monster icon coverage。若要让 race selection 有正式图标，必须新开 `icon.race.*` family 或明确 text-only/no-icon disposition，并补 `UI-FOLLOWUP-race-icon-family-contract`；不能继续借 `icon.profession.*` 来证明 race surface 已覆盖。

Inscriptions 当前复用 `icon.skill.*`，因此 PR-06 的 skill family inventory source 不能只扫描 talent index。只要 inscription 面板、shop 或 active loadout 仍使用 `icon.skill.*`，inventory 必须把 inscription icon key 纳入 `skill icon` family 的 source/consumer；若未来要拆独立视觉语法，必须新建 `icon.inscription.*` family 并同步 manifest lint / consumer tests。

### 6.2 Quest Icon Consumer Contract

当前 shell quest summary 路径在 `TileRenderModel.buildShell` / `questSummaryText`，只从最新 `log.objective.*` token 渲染文本。PR-06 必须把这个 player-visible row 接到实际 visual consumer，不能只让 `icon.quest.*` 通过 manifest resolver。

执行细则：

1. Quest summary row 必须变成携带 icon 的 `TileTextRow`，icon 通过 visual resolver 从 `icon.quest.*` 解析；缺 key 时使用 registered fallback，并在 coverage/report 中可定位。
2. 不改 quest/world progress 规则；不得在 `core` 或 `game` 为 UI 临时新增第二套 quest state。
3. Implementation substeps 固定为：新增 `QuestSummaryIconResolver` 映射 owner；让 shell quest row 携带 resolved icon；保留空状态无 icon；补 `TileRenderModelTest.shellQuestSummary*` 与 `TileRendererCanvasTest.shellQuestSummary*`，最后再进 overview golden。
4. PR-06 固定最小路径为 generic `icon.quest.objective_marker`，不扩 `RenderSnapshot`、`WorldProgress` 或 quest schema。typed quest/objective metadata 如果未来需要，必须单独作为 stable snapshot / public contract change。
5. 新增 client-only owner `QuestSummaryIconResolver` 或等价小对象，唯一 mapping 表为：`log.objective.activate`、`log.objective.progress`、`log.objective.advance`、`log.objective.complete` -> `icon.quest.objective_marker`。该 resolver 只看 render log token key，不解析 localized text。
6. `icon.quest.armory_key`、`icon.quest.seal_key` 只能用于真实钥匙/目标语义，不能为了“有图标”硬塞到所有 objective log。
7. 空状态保留 `ui.shell.quest.none` 文案；空状态不消耗 quest icon，不计入 quest icon 上屏证明。
8. 测试至少覆盖 `TileRenderModelTest.shellQuestSummaryCarriesGenericQuestMarkerForObjectiveLog` 和 `TileRenderModelTest.shellQuestSummaryDoesNotUseQuestIconForEmptyState`。如果最终只落到 `TileRendererCanvasTest`，文档必须把本段测试名收敛成实际文件名。
9. Golden `dark-uiux-pr06-status-quest-skill-overview` 必须同时展示 status icon、skill/talent icon 和 quest marker/icon；只出现文本不算通过。
10. generic marker 是 PR-06 的显式范围取舍，不得让 typed quest keys 成为孤儿资源。若 PR-06 不引入 typed quest icon mapping，必须在 PR 描述和 `UI/PLAN.md` 记录命名 follow-up `UI07-quest-typed-icon-mapping`；PR-07 只能 re-audit，不能静默改变 quest snapshot / schema。

Objective token visual weight：

| Token | Current code meaning | Row tone |
| --- | --- | --- |
| `log.objective.activate` | objective appears or becomes active | brief ember-gold confirmation `< 0.5s`, then returns to muted objective marker |
| `log.objective.progress` | objective step/progress updated | muted objective marker; no accent budget |
| `log.objective.advance` | objective enters next sub-phase, currently emitted on outpost depth/interactable progression | medium row pulse `< 0.4s`, no ember/cyan accent |
| `log.objective.complete` | objective completion | ember-gold completion accent for `0.5-1.0s`, then returns to muted marker |

Empty quest state must reserve text rhythm and quest icon slot dimensions, but it must not consume quest icon上屏证明. The empty slot may be transparent or use an explicit `icon.quest.empty` placeholder; if a placeholder key is used, it must enter §6.1 inventory and `r09-quest-zone-profession`. `activate` and `complete` cannot be visually identical to `progress`; if either transient accent cannot land in PR-06, the PR description must name `UI07-quest-activate-accent` or `UI07-quest-complete-accent` as an outstanding finding rather than silently inheriting it.

`log.objective.advance` emission contract:

1. Required: objective enters a new sub-phase the player should perceive as a stage change, such as outpost depth progression, interactable progression to the next state, or a multi-step quest moving to the next step.
2. Forbidden: counter increment within the same sub-phase, side-effect resolution, speculative pre-completion, or ambient flavor logs. Use `progress`, ambient log tokens, or `activate` instead.
3. Validation: every new `log.objective.advance` emission site must add a focused test naming the sub-phase boundary it represents.

### 6.2.1 Status Icon Nullability And Fallback

PR-06 不允许 status icon 行为继续由 `mapNotNull` 静默决定。规则固定为：

1. 当前 runtime `StatusIconResolver.resolveIcons` 只消费 actor `StatusEffectRenderSnapshot`，不消费 terrain / zone-effect inspect rows；zone effect 若未来进入 HUD，必须新增明确 owner、key family 和 inventory 分母。
2. 正式 actor status presentation 的 `iconKey` 不得为 null；缺失时必须请求 documented status family key（例如 `icon.status.<typeId>`）并通过 registered manifest prefix/fallback 记录，不允许新增 `status.icon.missing.*` 这类分母外 synthetic key。非空 actor status `iconKey` 也必须属于 `icon.status.*`，不得复用 `icon.skill.*`、`icon.mutation.*` 或 telegraph key。
3. Actor mutation 不走 `StatusIconResolver`；mutation icons 由 actor mutation / inspect consumer 使用 `icon.mutation.*`，缺失时必须由 mutation registry、manifest resolve 或 actor mutation consumer test 失败，而不是被 status resolver 自动改写为 `icon.status.*`。
4. `StatusPresentationBuilder.buildZoneEffect` 当前 `iconKey = null` 不是 `icon.status.*` / `icon.mutation.*` 覆盖分母。若 PR-06 要把 zone effect 变成 HUD row，必须新增明确 owner、key family 和 inventory；若不做，inventory 中写 `manifest-only-with-reason=zone-effect-text-only-not-status-icon-family`。
5. unknown status icon key 必须走 registered fallback 并在 test/report 中可定位，不得静默 drop。
6. status icon 行最大显示数量必须冻结为 `10`。超过上限时按 `DEBUFF > BUFF > NEUTRAL_OTHER` 的 actor-status priority，再按 `StatusPresentationBuilder.sorted` 的 priority / typeId 顺序保留前 10 项，并以 locale key `ui.status.fold.summary` 渲染固定末尾 slot 的 `+N more` badge；不得让后续 status 横向溢出或直接消失。
7. Telegraph 不属于 status HUD fold 合同。enemy intent / aim line / impact marker 必须保持 actor/map anchored owner；`StatusPresentationGroup.TELEGRAPH` 只能作为 telegraph overlay 排序/target-card presentation，不得作为 player status HUD membership。
8. `+N more` fold badge 必须支持 hover/tap 展开全部隐藏 status 的 detail tooltip；排序与 status row 一致。如 PR-06 内不实现 expand，必须命名 follow-up `UI07-status-fold-expand`，并让 fold badge 明确表现为 non-interactive，避免误导玩家。
9. Status badge rendering contract：stack count `2..99` 显示 `xN` 或 `N/cap`；`>=100` 显示 `x99+`；remaining turns `1..99` 显示 `Nt`，`>=100` 显示 `99+`；badge visible width 不得超过 4 compact characters。相同 `typeId` 的多实例状态若未来进入 snapshot，HUD 必须 group 为单一 entry，不得渲染重复 icon。
10. owner 分工固定：`StatusIconResolver` 只做 manifest resolve 与 fallback evidence，不做 fold / layout / interaction；HUD fold owned by `StatusHudIconRowModel` / `StatusHudPresenter` 或 `TileRenderModel` 的显式 status HUD row builder。locale owner 必须覆盖 `ui.status.fold.summary`、non-interactive hint、hover detail title/body。
11. 必须补 `StatusDefinitionsTest.runtimeStatusDefinitionsKeepStatusIconFamilySeparateFromSkills`、`StatusSchemaContractTest.statusSchemaVisualKeysStayInTheStatusIconFamily`、`StatusIconResolverTest.resolvesActorStatusIcons`、`StatusIconResolverTest.nullIconStatusUsesDocumentedStatusFamilyFallback`、`StatusIconResolverTest.unknownStatusIconUsesRegisteredFallback`，并补 `StatusPresentationModelTest.statusBadgeCapsLargeStacksAndLongDurations`、`StatusPresentationModelTest` 或 `StatusHudOverflowTest` 覆盖 fold case。

### 6.3 Validation Overlay Layout Contract

`ValidationPackSummaryText` 与 `ValidationScenarioEvidenceSummaryLines` 当前会把 active pack、namespace、overlay ops、touched content ids 和 evidence path 直接 join 成长文本。PR-06 必须先冻结 compact owner 再改 UI：

PR-06 固定新增 `ValidationOverlaySummaryPresenter` 或等价 owner。它负责接收 active pack summary、key warning summary 和 evidence summary，输出 bounded rows；`ValidationPackSummaryText` 与 `validationScenarioEvidenceSummaryLines` 只保留 leaf formatter，不各自实现 row budget，避免多处截断逻辑。

| Item | Rule |
| --- | --- |
| display mode | `validation.overlay.compact` 用于 in-run overlay，由 HUD overlay owner 调用；`validation.overlay.detailed` 用于 ValidationSetupScreen / dedicated validation panel，由 setup/panel owner 调用，但 row budget 仍由 `ValidationOverlaySummaryPresenter` 统一决定 |
| section order | compact: key warnings -> evidence summary -> preset -> seed -> zone/floor -> active packs -> namespaces -> overlay ops -> touched content；detailed: evidence summary -> key warnings -> preset -> seed -> zone/floor -> active packs -> namespaces -> overlay ops -> touched content；compact mode 必须让 warning/evidence 在 fold 前可见，detailed mode 优先让 evidence 可审计 |
| max rows | compact mode 主体最多 `min(12, floor(visibleOverlayRows * 0.25))`，且不得低于 6 行；detailed mode 最多 32 行或按 panel 可视高度自然展开，但不得复用 compact 的 12 行硬上限；超过时 fold 为 `+N more` 行 |
| max line length | 单行 display text 最多 96 visual columns；column width 使用 UAX #11 / wcwidth 等价口径，CJK Wide/Fullwidth 按 2 列、combining mark 按 0 列估算。若暂未实现 column-aware width，zh/ja/ko locale 使用 144 chars fallback，不得沿用 ASCII char count |
| sorting | pack ids、namespaces、overlay ops、touched content ids、warning keys 全部稳定排序 |
| empty state | 使用 locale token，不输出空字符串或 `null` |
| path display | 只显示 repo-relative path；禁止 macOS home、tmp、Windows drive absolute path 或 transient generated path。truncation 使用 keep-tail 策略，必须保留 repo-relative path 的文件名段 |
| fallback warning | visual/audio/locale warning 分栏保留；不能合并成一串失去 owner 的文本 |

`ValidationOverlaySummaryPresenter` 的 owner 范围固定如下，leaf formatter 不得各自实现第二套 budget：

| Responsibility | Owner |
| --- | --- |
| row count budget | `ValidationOverlaySummaryPresenter` |
| line length / visual column budget | `ValidationOverlaySummaryPresenter` |
| path compaction and repo-relative enforcement | `ValidationOverlaySummaryPresenter` |
| stable sorting of leaf values | leaf formatter |
| section order | `ValidationOverlaySummaryPresenter` |
| display mode selection and max row cap | caller selects compact/detailed; `ValidationOverlaySummaryPresenter` enforces the mode-specific cap |
| fallback warning column separation | leaf formatter |

必须补测试：

1. `ValidationOverlaySummaryPresenterTest.boundsRowsAndLineLength`
2. `ValidationOverlaySummaryPresenterTest.keepsEvidencePathsRepoRelative`
3. `ValidationOverlaySummaryPresenterTest.compactModeKeepsWarningsAndEvidenceBeforeFold`
4. `ValidationOverlaySummaryPresenterTest.usesColumnAwareBudgetForCjkText`
5. `ValidationPackSummaryTextTest.overlayOpsAreSortedAndBounded`
6. `ValidationScenarioEvidenceSummaryLinesTest.compactsRepoRelativeEvidencePaths`
7. `ValidationScenarioEvidenceSummaryLinesTest.rejectsOrRedactsMachineAbsolutePaths`

Golden 必须包含长列表和 fallback warning 场景；只截常规成功路径不能证明 overlay 在真实验证模式下可读。

Coverage artifact 要求：

PR-06 使用 PR-00 固定的 final-full schema，不在本 PR 自定义第二套字段。final-full 至少要求 `expectedKeySet`、`coveredKeySet`、`missingKeys`、`oldStylePlayerVisibleKeys`、`pendingOrRejectedPlayerVisibleCells`、`fallbackKeyUsage`、`allowedFallbackKeys`、`allowedCoverageExclusions`、`sourceSheetIds`。若本 PR 扩展 `allowedCoverageExclusions` 字段内容，只能在 PR-00 schema 允许的对象结构内补字段，并同步工具测试。

`allowedFallbackKeys` 只允许 K-ToME 内置 debug / hidden / history resource fallback。第三方 content pack 注入的 visual key 不得进入 `allowedFallbackKeys`；如果后续需要允许 third-party fallback，必须使用独立 `thirdPartyFallbackKeys` namespace，并作为 `UI07-third-party-fallback-key-namespace` 单独 schema follow-up。

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
| `missing_visual` | 生成 text-marked sentinel；development client 必须可明显识别为 unresolved visual，packaged app 若出现即 PR06 failure | `r09-fallback-debug` |
| `tile.hidden` / hidden entrance / secret zone | 生成 hidden/secret readable icon or portrait；必须与 missing sentinel visually distinct | `r09-fallback-debug` 或对应 zone sheet |
| `category=debug` player-visible starter/item visuals | 若玩家可见则迁移；纯 debug/history 在 packaged app 中不可见，development client 显示明确 `DEBUG` visual treatment | `r09-fallback-debug` 或 Round 7 owner sheet |
| debug-only tiles / missing visual sentinel | 不允许 client ASCII renderer 或 ASCII manifest 字段；纯 debug/history resource fallback 可列入 `allowedFallbackKeys` 并说明原因；missing sentinel 必须包含非颜色 textual marker 或 frame marker | `r09-fallback-debug` 或 allowed fallback |
| PR-03/05 rejected cells | 只处理 coverage artifact 中列明的 rejected cell | `r09-rejected-polish` |

`darkManifestCoverageLint final-full` 必须新增 packaged-app sentinel audit 输入或等价人工 evidence。输入 property 固定为 `-Pktome.darkUiux.packagedSentinelEvidence=<comma-separated repo-relative paths>`；report 字段固定为 `packagedSentinelEvidencePaths`、`packagedMissingVisualHits`、`packagedSentinelAuditStatus`。工具测试必须覆盖 no evidence、clean evidence、evidence references `missing_visual` 三种情况。若 PR-06 只冻结 sentinel semantics 而不执行 packaged audit，则 report 必须写 `packagedSentinelAuditStatus=deferred-to-pr07`，且文档/PR 描述不得声称 lint 已扫描 packaged app。

如果 final inventory 中 manifest fallback / debug resource / hidden / rejected cell 总数超过当前 sheet capacity，PR-06 必须先新增 `r09-fallback-debug-*` 或 `r09-rejected-polish-*` sheetId，并同步更新 `UI/PLAN.md`、`UI/pr/README.md` 和 key registry；不得把超额 key 留作 silent pending。
PR-06 close 前玩家可见 rejected cell 必须为 0；PR-07 只能处理 PR-06 coverage artifact 中记录的非玩家可见 polish 项或后验发现的单点问题。若 PR-07 发现玩家可见 rejected cell，视为 PR-06 未完成，不能在 PR-07 静默修补。

Manifest key 切换表必须至少包含：

| Key family | Required action |
| --- | --- |
| `icon.skill.*` / `talent.*.icon` | release/dev playable 全量切到 dark-v1 |
| `talent.*.visual` | release/dev playable 全量切到 dark-v1 或列入 explicit pending |
| `icon.tree.*` | tree header 小图标切到 dark-v1 |
| `tree.*` | tree portrait 切到 dark-v1 |
| `icon.status.*` | 全量切到 dark-v1，并证明 actor status HUD consumer 上屏 |
| `icon.mutation.*` | 全量切到 dark-v1，并证明 actor mutation inspect/target-card consumer 上屏 |
| `icon.damage_type.*` | 全量切到 dark-v1 |
| `icon.quest.*` | 全量切到 dark-v1，并证明至少一个 quest summary consumer 上屏 |
| `zone.*.icon` | 全量切到 dark-v1；禁止新增 `icon.zone.*` alias |
| `icon.profession.*` | release/dev playable 全量切到 dark-v1；frozen 只能走 allowed exclusion |
| `difficulty.normal.icon` | 切到 dark-v1；未来新增 difficulty 使用 `difficulty.<id>.icon`，并通过 material complexity / accent shape 表达梯度，不得靠更亮、更红、更血腥来表达难度递增 |
| `missing_visual` / hidden / debug resource fallback | Round 9 polish 或 allowed fallback 说明 |

### 6.4 Player-visible Disposition Matrix

PR-06 close gate 中的 "player-visible" 以本矩阵裁定；若 coverage、review 或 QA 对某个 cell 归属有争议，先回到本矩阵，不得临时按 PR 描述口径缩小分母。

| Surface / Cell | Player visibility | Coverage denominator | PR-06 disposition |
| --- | --- | --- | --- |
| Release playable skill / talent / tree | visible | in | required full dark-v1 |
| Dev playable skill / talent / tree | visible under debug/report-only path | in | required full dark-v1 |
| Frozen profession card | hidden preferred; otherwise visible with explicit unavailable label | excluded with reason | `hidden` or `locked-with-coming-soon-label` |
| Frozen profession skill / talent | not normally visible | excluded with reason | n/a |
| Validation overlay (debug client) | visible | in | compact + dark-v1 |
| Validation overlay (packaged app) | not visible by default | n/a | packaged appearance is PR-07 audit evidence |
| Hidden zone tile pre-discovery | not visible | n/a | n/a |
| Hidden zone tile post-discovery | visible | in | required dark-v1 |
| Debug-only tile toggled in development | development-visible | allowed fallback only with reason | must show debug treatment |
| `missing_visual` sentinel in development | visible intentional fallback | excluded with reason | text-marked sentinel |
| `missing_visual` sentinel in packaged app | should not appear | n/a; if appears, PR06 failure | text-marked sentinel for diagnosis |
| PR-03/05 rejected cell still player-visible | visible | in | `r09-rejected-polish` |
| PR-03/05 rejected cell not player-visible | not visible | n/a | PR-07 polish handoff only |

If PR-07 audit discovers a player-visible surface not covered by PR-06 inventory, PR-07 must escalate to PR-06 reopen rather than silently use fallback. Reopen resolution is binary: either PR-06 adds inventory entries, sheet QA, focused tests and evidence before PR-07 close, or the surface is reclassified as non-player-visible with explicit design-director review. PR-07 cannot add sheet or inventory ownership on its own.

PR-07 audit PR description must explicitly reference PR-06 final-full coverage artifact repo-relative path, status, expected/covered/missing counts, and the packaged app evidence path that proves no `missing_visual` sentinel leaked into release smoke/golden screenshots.

### 6.5 Accent Strength Budget

Dark-v1 使用 low-saturation palette，ember/cyan 是稀缺注意力资源。PR-06 same-screen overview 和 manual whitebox 必须检查同屏 accent 总量，不允许让所有可交互元素同时抢焦点。

| Accent | Role | Strength budget |
| --- | --- | --- |
| ember-gold quest active marker | current objective route / goal | strength 3, max one on screen; `complete` transient may use strength 3 for `0.5-1.0s` |
| ember-gold rare item or reserve talent | secondary emphasis | strength 2, max two visible |
| ember-gold title / confirmation | structural or transient emphasis | strength 1, confirmation `< 0.5s` |
| cold-cyan selected slot edge | primary focus chain | strength 3, max one focus chain |
| cold-cyan focused row | local interaction cue | strength 2, max two focused rows/tiles; not used for enemy intent |
| cold-cyan learnable badge / tooltip edge | low-intensity affordance | strength 1 |
| danger-red / danger-violet telegraph indicator | enemy intent preview | strength 2, anchored at enemy/map tile, max two simultaneous high-danger indicators; must not reuse player focus cyan or player status badge frame |

Strength definitions are operational: strength 1 = edge/outline only, strength 2 = readable small fill or glow on one row/tile, strength 3 = primary attention cue that wins first glance. Same-screen aggregate cap: all strength-2-or-higher ember/cyan/telegraph cues combined must stay `<= 4`, with at most one strength-3 chain outside a transient quest-complete moment. Color-blind simulation must verify that ember/cyan/danger accents remain separable by shape carrier, not color alone: objective accents use marker/sigil shapes, focus accents use edge/glow shapes, and telegraph accents use actor-anchored impact/aim shapes.

Manual evidence must record whether ember + cyan strength-2-or-higher cues exceed four visible items in the same HUD frame. If they do, the sheet/contact QA or row tone must be adjusted before PR06 close.

### 6.6 Frozen Profession Player Visibility

`shadowblade / warden` are frozen excluded, not unlockable player goals. PR-06 must choose one disposition before close:

1. Preferred: hide frozen professions from player-visible profession selection and keep exclusion only in coverage/report artifacts.
2. Allowed: show locked card only with explicit unavailable/development-preview locale token and reduced accent.
3. Forbidden: show a polished locked card with dark-v1 fallback only; that reads as a normal unlock requirement and misleads onboarding.

If a frozen profession card is visible, hover tooltip content must come from `hoverTooltipContentKey`, not the formal profession description. Tooltip copy must match `productMessagingKey` in meaning and must not imply a normal unlock requirement. Dev/debug talent panel access to frozen professions must render the `visibleFallbackKey` plus a `ui.dev-only.banner` or equivalent development-only marker; it must not reuse the official profession icon as if playable.

Cross-surface frozen/dev playable disposition：

| Surface | Frozen profession disposition |
| --- | --- |
| Profession selection (new run) | preferred hidden; allowed locked-with-coming-soon-label; forbidden locked-with-fallback-only |
| Save slot list | render `visibleFallbackKey` with locked/dev banner and explanatory tooltip, or disable with explanation if save policy requires fail-fast |
| Death summary / run end | dev playable renders normally plus development-preview marker; frozen renders fallback-with-banner |
| Achievement / statistics | historical entries stay readable with muted dev/frozen marker |
| Replay viewer | not applicable until replay UI exists; tracked as `UI07-replay-frozen-profession` |

Cross-surface profession identity contract: PR-06 owns the dark-v1 `icon.profession.*` sheet for every live profession identity surface. Future character sheet, save/load slot, death summary, minimap/legend or ally/minion surface must reuse the dark-v1 key or request explicit size variants; it must not introduce a parallel profession icon family. PR-07 audit may report drift but cannot create sheet/inventory entries on its own.

### 6.7 Long-session And Accessibility Evidence

PR-06 manual record must include a `>= 60min` single-session pass covering combat, inventory, quest, talent, and validation overlay transitions. The record must list at least three observed readability/fatigue points, even when the conclusion is "no issue found".

Minimum session structure:

1. At least 2 profession switches through profession selection -> talent panel -> combat.
2. At least 3 combat encounters across trash / elite / boss or nearest available intensity equivalents.
3. At least 2 inventory/equipment sorting moments.
4. At least 1 quest completion and 1 quest activation.
5. At least 1 validation overlay trigger in debug client.
6. At least 1 zone/biome transition. If a single run cannot cover all items, use multi-run within the same session; total session time still must be `>= 60min`.

The manual record must contain a timestamped play log, screenshots at minute 0 / 30 / 60, and PR reviewer acknowledgement that the long-session record was read and its fatigue findings are recorded.

Talent state and status icon evidence must include color-blind simulation checks for protanopia, deuteranopia, and tritanopia. The check is about four-state distinction and status/skill separation, not a broad accessibility certification.

Color-blind simulation is QA evidence, not a runtime feature. Runtime color-blind support such as mode toggles, pattern overlay, or alternate row tone is tracked as `UI07-color-blind-runtime-toggle`; until that exists, dark-v1 assets must remain distinguishable by silhouette, frame, marker slot, and pattern without relying on runtime recoloring.

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
  -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json \
  -Pktome.darkUiux.packagedSentinelEvidence=UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md
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
2. 触发至少 3 类状态、1 个 unknown status key、1 个 null-icon status、1 个 zone-effect inspect case 和 2 类技能预览，确认 actor status 数量不被 silent drop，zone effect 不被误路由为 HUD status icon，状态/技能图标不混淆。
3. 故意注入一个 missing key 的测试路径，确认 manifest fallback 使用 text-marked sentinel，report 可定位，且 hidden/debug/missing 三类视觉不会互相误读。
4. 打开 validation overlay / active pack summary，确认 compact mode 下 key warnings 和 evidence summary 位于 fold 前，scenario、pack、evidence summary 可读且不遮挡 HUD/日志。
5. 用 6 个 active pack、4 个 namespace、12 个 touched content 和 8 条 evidence path 触发 truncation，确认排序、折叠、repo-relative path keep-tail 和 CJK locale column budget 稳定。
6. 完成 `>= 60min` 单次 session，按 §6.7 覆盖职业切换、trash/elite/boss 或等价强度战斗、装备整理、quest activate/complete、validation overlay 和区域跃迁，把至少 3 处视觉疲劳 / accent 堆积 / 长 overlay 读屏问题记录到 `UI/manual-records/dark-uiux-pr06-long-session-fatigue.md`，并补 0/30/60 分钟截图。
7. 在 protanopia / deuteranopia / tritanopia 三种色弱模拟下，确认 talent locked / learnable / reserve / active 四态、status vs mutation、status vs skill 仍可分。
8. 同屏打开背包 + 任务 + 天赋 + 战斗 telegraph，检查 ember/cyan/danger strength-2-or-higher cue 总量不超过 4；telegraph 必须 actor/map anchored，不得混入 player status HUD。
9. 切换至少 3 个职业，列出当前 build 存在的所有 profession-identity surface（不限于 selection / talent / combat log），逐一确认 profession icon 视觉一致；不存在的 character sheet / save slot / death summary 等 surface 必须写入 follow-up。
10. 完成 quest activate 与 quest complete 事件，确认 shell row 两端点都有 transient feedback，且 duration / tone 与 progress/advance 可区分。
11. 对 frozen profession locked card、save slot / death summary 等可见路径执行 hover/inspect，确认 tooltip 使用 development-preview 或 frozen explanation，不复用正式职业描述。
12. 同屏对比 weapon/equipment tooltip damage type icon、damage float icon、status icon、skill icon，并在 `16/24/32px` 或对应 runtime 尺寸下确认 silhouette grammar 不收敛。
13. 检查 `+N more` status fold badge：若 PR-06 实现 expand，hover/tap 必须列出隐藏 status；若未实现，必须以 non-interactive 态呈现并记录 `UI07-status-fold-expand`。
14. 必填证据：skill/status/quest/profession tree 同屏截图、manifest coverage artifact、fallback injection record、validation overlay 长列表截图、long-session fatigue record、color-blind snapshot、damage float visual record、`dark-uiux-pr07-validation-overlay` 的 PR-07 evidence 引用。

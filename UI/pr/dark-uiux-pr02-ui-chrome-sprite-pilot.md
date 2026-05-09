# Dark UI/UX PR-02 UI Chrome Sprite Pilot

**阶段**: `dark-uiux-pr02-ui-chrome-sprite-pilot`
**优先级**: `P0`
**工作量**: `L`
**前置条件**: PR-00、PR-01、PR-01-1 完成。
**资源生成结论**: 生成 Round 1 最小可运行组：`r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons`。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再跑 resource gate、client evidence 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI02-M01` | §3 Round 1 sheet 内容 | `assets` | `darkSpriteSheetLint`, `spriteSheetMapLint` | `assetLint`, `styleLint` | `assets-src/image/contact-sheets/dark-v1/` | `required` |
| `UI02-M02` | §4 UI key registry 初始清单 | `tools` | `darkKeyRegistryLint` | `manifestLint` | `UI/sprite-sheets/key-registry.yaml` | `N/A` |
| `UI02-M03` | §6 验收标准 / manifest coverage | `tools` | `ManifestResolveTest` | `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons` | `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` | `N/A` |
| `UI02-M04` | §7 / §8 golden 与白盒证据 | `client` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`, `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets`, `StandaloneScreenLayoutTest.darkUiuxPr02StandaloneChromeConsumesManifestKeys` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/golden/`, `UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md` | `required` |
| `UI02-M05` | governance inheritance | `docs` / `tools` | `acceptanceContractLint` | `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |
| `UI02-M06` | §4 standalone screen shared chrome consumption | `client` / `tools` | `StandaloneScreenLayoutTest.darkUiuxPr02StandaloneChromeConsumesManifestKeys`, `MainMenuScreenTextTest` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/golden/`, `UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md` | `required` |

### Gate Budget

预计重型任务：`assetLint`、`styleLint`、`manifestLint`、`darkKeyRegistryLint`、`darkSpriteSheetLint`、`spriteSheetMapLint`、`darkManifestCoverageLint`、`:client:clientSmoke`、`:client:goldenScreenshot`、`verifyChanged`。触发原因是 PR-02 首次提交正式 dark UI chrome / HUD / standalone screen chrome sheet 到 manifest 与 golden。

freshness 要求：

1. `raw sheet hash`、切分 PNG、`sprite map report`、contact sheet QA、canonical manifest、runtime manifest、owner-scope coverage 和 golden output 必须来自同一批 `sheet-plan.yaml` / `key-registry.yaml`。
2. `syncPhase2Manifests` 必须在 `ManifestResolveTest`、client focused evidence、`darkManifestCoverageLint owner-scope`、`:client:goldenScreenshot` 和最终 `verifyChanged` 之前完成。
3. PR 描述或 manual record 必须摘录本轮 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` 的 `ownerSheetIds`、`ownerExpectedKeys` 数量、`ownerCoveredKeys` 数量、`ownerMissingKeys`、`scopeExternalPendingKeys`，不能只写 Gradle task green。
4. PR close 前读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。若该文件不存在，PR 描述写明“本轮未产生 verifyChanged duration summary”，不得伪造耗时基线。
5. 同一 resource gate 连续失败超过 2 次时，先补 `scripts/verify_sprite_sheet_map.py` / registry lint / focused resolver test 的最小断言，再重跑 full resource gate；不得把 `goldenScreenshot` 当资源切分调试循环。

### Canonical Artifact

canonical artifact 固定为以下 repo-relative 路径族：

1. `UI/sprite-sheets/sheet-plan.yaml`
2. `UI/sprite-sheets/key-registry.yaml`
3. `UI/sprite-sheets/prompts/dark-v1/{001-r01-ui-chrome,002-r01-ui-controls,003-r01-ui-hud-icons}.prompt.txt`
4. `UI/sprite-sheets/prompts/dark-v1/prompt-index.json`
5. `assets-src/image/raw/sheets/dark-v1/{r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons}.png`
6. `assets-src/image/contact-sheets/dark-v1/{r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons}-contact.png`
7. `assets-src/image/manifests/phase2-visual-manifest.json`
8. `client/src/main/resources/dark-v1/ui/`
9. `client/src/main/resources/manifests/visual-manifest.json`
10. `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`
11. `assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl`
12. `build/reports/verification/dark-uiux/codex-image-smoke-{sheetId}.json`
13. `client/build/reports/golden/`
14. `UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md`

raw PNG 只有在切分、QA、manifest 和 coverage 同批通过后才可作为可评审输入。`<codex-generated-images-dir>` 这类 Codex CLI transient source 只能出现在 smoke 摘要中，不能进入 manifest、coverage artifact、golden metadata 或 PR 合同。

### Failure Rule

资源 gate 失败时先修 key registry、sheet plan、manifest 或切分结果；不得通过手改 runtime manifest、跳过 owner-scope coverage 或只保留 raw PNG 让 PR 通过。

PR-02 formal owner 迁移规则：

1. PR-00 dry-run entries 只保留为 pipeline proof；PR-02 formal key 必须在 `key-registry.yaml` 中使用 `ownerPr: PR-02`，不能继续用 `PR-00` 充当本 PR 分母。
2. `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons` 必须在 `ownerExpectedKeys` 非空时才允许通过。
3. owner-scope artifact 的 `ownerSheetIds` 必须至少包含 `r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons`；少任一 sheet 必须由 coverage task fail fast，不能只靠 reviewer 人工读 artifact。
4. `ui.frame.panel.focus` 在 PR-02 中升级为 formal direct key；不得让 manifest 存在该 key 而 PR-02 表格缺席。

## 1. 阶段目标

1. 跑通第一组正式 sheet：`r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons`。
2. 交付 `raw sheet -> sliced PNG -> processed PNG -> visual-manifest -> golden` 完整闭环。
3. 新增 UI chrome / HUD key，并让 client 实际消费 panel/slot/modal/HUD 的最小可运行子集。
4. 新增 standalone screen chrome / control key，供首页、验证 setup、结算页、错误页、loading state 统一消费；这些 screen 不得继续使用旧硬编码纯色块作为主视觉。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `UI/sprite-sheets/sheet-plan.yaml` | 增加 Round 1 正式 cell |
| `assets-src/image/raw/sheets/dark-v1/` | 保存 raw sheet |
| `assets-src/image/contact-sheets/dark-v1/` | 保存带 cell 标注的 QA sheet |
| `assets-src/image/manifests/` | 保存 sprite report 或 mapping report |
| `assets-src/image/manifests/phase2-visual-manifest.json` | 先增加或更新 UI chrome key 的 canonical manifest entry |
| `client/src/main/resources/dark-v1/ui/` | 保存切分后的 runtime PNG |
| `client/src/main/resources/manifests/visual-manifest.json` | 由 `syncPhase2Manifests` 同步生成 runtime manifest |
| `client` renderer | 消费新 key，保留 manifest fallback |
| `client` standalone screens | 首页、验证 setup、结算、错误/loading 页消费共享 chrome/control key 或明确 manifest fallback |

## 2.1 实施执行顺序

开发必须按以下顺序推进，避免资源、manifest 和 client evidence 批次错位：

1. **preflight**：读取本文件、[README.md](./README.md)、[development-governance.md](./development-governance.md)、[screen-coverage-matrix.md](./screen-coverage-matrix.md)，执行 `acceptanceContractLint`。如果 PR-01-1 尚未合并，不开始 PR-02 实现。
2. **formal registry first**：先更新 `UI/sprite-sheets/key-registry.yaml`，新增 §4.2 / §4.3 / §4.4 的 `ownerPr: PR-02` entries。PR-00 dry-run entries 可以保留，但不能改成 PR-02 证据。
3. **sheet plan second**：更新 `UI/sprite-sheets/sheet-plan.yaml`，新增三张 sheet 和逐 cell `targetKey / outputName`；本 PR 不新增 alias cell。所有未列为 formal key 的 cell 必须写 `reserved: true` 和 `note`，不能留空让实现者猜。
4. **prompt generation**：运行 `scripts/generate_sheet_prompt.py`，确认 prompt 文件头中的 `Sheet ID`、`Expected output file`、`Canvas`、`Grid` 和 `Style tag` 与 sheet plan 完全一致。
5. **raw sheet generation**：逐个运行 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --smoke-report <buildReportPath> --overwrite`。如果生成图串格、带文字、水印、跨 cell 或风格漂移，重生成 raw sheet，不通过改 `row/col` 迁就错误图片。
6. **slice and QA**：运行切分、contact sheet 和 `spriteSheetMapLint`，人工检查 contact sheet 的 `row,col,targetKey` 与图像语义一致后，再进入 manifest patch。
7. **canonical manifest patch**：只改 `assets-src/image/manifests/phase2-visual-manifest.json` 作为真源；runtime manifest 只能由 `syncPhase2Manifests` 生成。
8. **sync before evidence**：执行 `syncPhase2Manifests manifestLint` 后，才运行 resolver tests、owner-scope coverage、client focused tests、golden 和 `verifyChanged`。
9. **client consumption**：`TileRenderer` / standalone screen chrome 的主路径必须通过 `VisualManifestResolver` 消费 PR-02 keys；允许保留 manifest fallback，但不允许继续只画 token 色块作为 PR-02 目标 surface 的唯一主视觉。
10. **evidence write-up**：PR 描述或 manual record 写明 prompt path、raw sheet path/hash、contact sheet QA path、canonical/runtime manifest diff、coverage artifact 摘录、focused test、golden label 和未覆盖 screen 的 PR-07 继承项。

实现期间任何新增 key 都必须先回到 §4 的表格补 `targetKey / sheetId / fallbackKey / consumer / consumerTest`，再改 registry / sheet plan；禁止先在 renderer 里写裸字符串。

## 3. Round 1 Sheet 内容

1. `r01-ui-chrome`: `large-sheet 1024x1024 / 4x4 / 256x256`，包含 panel、corner、edge、tooltip、modal、slot frame。
2. `r01-ui-controls`: `icon-sheet 1024x1024 / 8x8 / 128x128`，本 PR 只冻结 §4.3 中列出的 back/confirm/copy、validation/outcome/error/loading marker、backpack/equipment、combat action 和 talent state glyph。tab、button、empty state、debug marker、selection marker 若本 PR 不消费，必须写为 reserved / pending cell，不进入 PR-02 owner-scope 分母。
3. `r01-ui-hud-icons`: `icon-sheet 1024x1024 / 8x8 / 128x128`，包含 health、stamina、xp、gold、key、quest marker、log marker、warning。
4. 切分后的 runtime canvas 由 category policy 决定；HUD 显示 `32x32`、装备 slot 显示 `48x48/64x64` 是 renderer/layout 决策，不是 source sheet cell 尺寸。
5. 每个 cell 必须在 `sheet-plan.yaml` 写明 `targetKey` 和 `outputName`。
6. contact sheet 必须显示 `row,col,targetKey`，但 runtime PNG 不能烘焙文字。
7. `sheet-plan.yaml` 中没有 `targetKey` 的 cell 必须显式 `reserved: true`；不能让空 cell 在切分或 coverage 中被隐式跳过。

Raw sheet 生成交接：

1. 先按 PR-00 固定命令生成 `001-r01-ui-chrome.prompt.txt`、`002-r01-ui-controls.prompt.txt`、`003-r01-ui-hud-icons.prompt.txt` 和 `prompt-index.json`。
2. 逐个执行 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --smoke-report build/reports/verification/dark-uiux/codex-image-smoke-<sheetId>.json --overwrite`。
3. 脚本输出必须分别落到 `assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png`、`r01-ui-controls.png`、`r01-ui-hud-icons.png`。
4. raw PNG 放置完成后才运行切分、contact sheet 和 manifest coverage gate。
5. PR 描述必须列出 prompt path、raw sheet path、raw sheet hash、`scripts/codex-generate-image.py` 输出的 smoke report 摘要和 contact sheet QA path；smoke report 中的 source folder/source image 只作为 transient evidence，不作为 canonical path、manifest 或 coverage 输入。

## 4. UI Key Registry 与 Cell Contract

### 4.1 Owner Scope Rule

PR-02 owner-scope 只由 `key-registry.yaml.entries[].ownerPr == PR-02` 决定。Markdown 表格不是机器真源，但本节是实现合同；如果实现需要新增、删除或 alias 任一 key，必须同步更新本节、`key-registry.yaml`、`sheet-plan.yaml`、canonical/runtime manifest 和对应 test/golden evidence。

`ownerExpectedKeys` 必须覆盖以下三类：

1. §4.2 / §4.3 / §4.4 的 direct cell keys。
2. 任何实现阶段新增的 PR-02 formal key。

PR-00 dry-run keys 不计入 PR-02 完成；`PR-00` 与 `PR-02` 可以同时存在于 registry，但 coverage artifact 必须能区分两者。

### 4.2 `r01-ui-chrome` direct cells

| row | col | targetKey | outputName | fallbackKey | aliasOf | consumerTest |
| ---: | ---: | --- | --- | --- | --- | --- |
| 0 | 0 | `ui.frame.panel.body` | `dark-v1/ui/ui_frame_panel_body.png` | `missing_visual` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 0 | 1 | `ui.frame.panel.focus` | `dark-v1/ui/ui_frame_panel_focus.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 0 | 2 | `ui.frame.panel.corner_tl` | `dark-v1/ui/ui_frame_panel_corner_tl.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 0 | 3 | `ui.frame.panel.corner_tr` | `dark-v1/ui/ui_frame_panel_corner_tr.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 1 | 0 | `ui.frame.panel.corner_bl` | `dark-v1/ui/ui_frame_panel_corner_bl.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 1 | 1 | `ui.frame.panel.corner_br` | `dark-v1/ui/ui_frame_panel_corner_br.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 1 | 2 | `ui.frame.panel.edge_top` | `dark-v1/ui/ui_frame_panel_edge_top.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 1 | 3 | `ui.frame.panel.edge_right` | `dark-v1/ui/ui_frame_panel_edge_right.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 2 | 0 | `ui.frame.panel.edge_bottom` | `dark-v1/ui/ui_frame_panel_edge_bottom.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 2 | 1 | `ui.frame.panel.edge_left` | `dark-v1/ui/ui_frame_panel_edge_left.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 2 | 2 | `ui.frame.slot.empty` | `dark-v1/ui/ui_frame_slot_empty.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 2 | 3 | `ui.frame.slot.equipped` | `dark-v1/ui/ui_frame_slot_equipped.png` | `ui.frame.slot.empty` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 3 | 0 | `ui.frame.slot.selected` | `dark-v1/ui/ui_frame_slot_selected.png` | `ui.frame.slot.empty` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 3 | 1 | `ui.frame.tooltip.body` | `dark-v1/ui/ui_frame_tooltip_body.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 3 | 2 | `ui.frame.modal.body` | `dark-v1/ui/ui_frame_modal_body.png` | `ui.frame.panel.body` | `N/A` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 3 | 3 | `reserved` | `N/A` | `N/A` | `N/A` | `N/A` |

### 4.3 `r01-ui-controls` direct cells

| row | col | targetKey | outputName | fallbackKey | consumerTest |
| ---: | ---: | --- | --- | --- | --- |
| 0 | 0 | `ui.screen.validation.badge` | `dark-v1/ui/ui_screen_validation_badge.png` | `ui.hud.warning.icon` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 1 | `ui.screen.outcome.victory_marker` | `dark-v1/ui/ui_screen_outcome_victory_marker.png` | `ui.hud.quest_marker.icon` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 2 | `ui.screen.outcome.defeat_marker` | `dark-v1/ui/ui_screen_outcome_defeat_marker.png` | `ui.hud.warning.icon` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 3 | `ui.screen.error.marker` | `dark-v1/ui/ui_screen_error_marker.png` | `ui.hud.warning.icon` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 4 | `ui.screen.loading.marker` | `dark-v1/ui/ui_screen_loading_marker.png` | `ui.hud.log_marker.icon` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 5 | `ui.control.back.icon` | `dark-v1/ui/ui_control_back_icon.png` | `ui.hud.log_marker.icon` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 6 | `ui.control.confirm.icon` | `dark-v1/ui/ui_control_confirm_icon.png` | `ui.hud.quest_marker.icon` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 7 | `ui.control.copy.icon` | `dark-v1/ui/ui_control_copy_icon.png` | `ui.hud.log_marker.icon` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 1 | 0 | `ui.control.backpack.icon` | `dark-v1/ui/ui_control_backpack_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 1 | 1 | `ui.control.equipment.icon` | `dark-v1/ui/ui_control_equipment_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 1 | 2 | `ui.combat.action.icon` | `dark-v1/ui/ui_combat_action_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 1 | 3 | `ui.combat.method.icon` | `dark-v1/ui/ui_combat_method_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 1 | 4 | `ui.combat.target.icon` | `dark-v1/ui/ui_combat_target_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 1 | 5 | `ui.combat.lock.icon` | `dark-v1/ui/ui_combat_lock_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 1 | 6 | `ui.combat.invalid.icon` | `dark-v1/ui/ui_combat_invalid_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 1 | 7 | `ui.state.locked.icon` | `dark-v1/ui/ui_state_locked_icon.png` | `missing_visual` | `TalentSidebarPresenterTest`, `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 2 | 0 | `ui.state.learnable.icon` | `dark-v1/ui/ui_state_learnable_icon.png` | `missing_visual` | `TalentSidebarPresenterTest`, `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 2 | 1 | `ui.state.active.icon` | `dark-v1/ui/ui_state_active_icon.png` | `missing_visual` | `TalentSidebarPresenterTest`, `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 2 | 2 | `ui.state.reserve.icon` | `dark-v1/ui/ui_state_reserve_icon.png` | `missing_visual` | `TalentSidebarPresenterTest`, `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |

`r01-ui-controls` 其余 cell 必须显式 reserved。若 implementation 发现某个 tab/button/empty/debug/selection key 已被 current client 主路径消费，先补本节 direct cell，再进入 registry/manifest；不能只在 renderer 或 manifest 中临时加 key。

### 4.4 `r01-ui-hud-icons` direct cells

| row | col | targetKey | outputName | fallbackKey | consumerTest |
| ---: | ---: | --- | --- | --- | --- |
| 0 | 0 | `ui.hud.hp.icon` | `dark-v1/ui/ui_hud_hp_icon.png` | `missing_visual` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 0 | 1 | `ui.hud.stamina.icon` | `dark-v1/ui/ui_hud_stamina_icon.png` | `missing_visual` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 0 | 2 | `ui.hud.xp.icon` | `dark-v1/ui/ui_hud_xp_icon.png` | `missing_visual` | `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` |
| 0 | 3 | `ui.hud.gold.icon` | `dark-v1/ui/ui_hud_gold_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 4 | `ui.hud.key.icon` | `dark-v1/ui/ui_hud_key_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 5 | `ui.hud.quest_marker.icon` | `dark-v1/ui/ui_hud_quest_marker_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 6 | `ui.hud.log_marker.icon` | `dark-v1/ui/ui_hud_log_marker_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |
| 0 | 7 | `ui.hud.warning.icon` | `dark-v1/ui/ui_hud_warning_icon.png` | `missing_visual` | `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` |

`r01-ui-hud-icons` 其余 cell 必须显式 reserved。PR-02 先交付独立 HUD marker；如果后续 PR 需要让 item/quest 图标复用这些图，必须另行更新 registry、manifest 和对应 owner 文档，不得在 PR-02 里隐式生成第二套语义相同但风格略不同的图。

### 4.5 Grid Occupancy Contract

| sheetId | grid | direct cells | alias cells | reserved cells | total slots |
| --- | ---: | ---: | ---: | ---: | ---: |
| `r01-ui-chrome` | `4x4` | `15` | `0` | `1` | `16` |
| `r01-ui-controls` | `8x8` | `19` | `0` | `45` | `64` |
| `r01-ui-hud-icons` | `8x8` | `8` | `0` | `56` | `64` |

PR-02 不新增 registry-only alias key。Standalone screen chrome 直接消费 `ui.frame.panel.body`；如果后续确实需要 `ui.screen.home.*` exact key，必须先在本节新增 direct cell、row/col、outputName、manifest entry 和 focused test，再进入实现。

### 4.6 Client Consumer Contract

PR-02 实现必须冻结到具体消费路径和断言：

1. `TileRenderer` 或其拆分后的 chrome component 必须通过 `VisualManifestResolver` 解析并 `drawAsset` 消费 `ui.frame.panel.body`、`ui.frame.slot.empty`、`ui.frame.modal.body`、至少两个 `ui.hud.*` key。panel / slot / modal 可以保留 token overlay 或 outline，但主 chrome 不能只来自 `drawRect`。
2. `StandaloneScreenChrome` 必须直接消费 `ui.frame.panel.body` 作为首页 / validation setup 的共享 chrome source；不能只用 `whitePixel` 绘制主 panel。
3. focused tests 必须使用 recording canvas、test texture repository 或 resolver spy 断言 resolved key 被绘制；不接受只断言颜色、文本位置或 golden hash。
4. `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` 必须覆盖 §4.2-§4.4 的全部 PR-02 owner keys，且至少断言 `rawOutputPath` 前缀为 `dark-v1/ui/`。
5. 如果 outcome/error/loading screen 在 PR-02 不进入 golden，PR-02 manual record 必须列出 skip reason、替代 resolver evidence 和 PR-07 evidence index owner label。

`missing_visual` 只允许作为 PR-02 owner-scope 早期 manifest fallback；PR-06 final-full 必须用 dark-v1 fallback/polish 资源替换玩家主路径中的旧风格 fallback。

## 5. 非目标

1. 不批量替换 538 个资源。
2. 不引入 atlas/region manifest。
3. 不生成技能、装备、怪物、地图 tile 资源。
4. 不在 PR-02 重新设计首页、验证 setup、结算或错误页行为；PR-02 只交付这些 screen 可消费的共享 chrome/control 资源。

## 6. 验收标准

1. contact sheet 上 `row,col,targetKey` 与切分图片语义一致。
2. canonical/runtime `visual-manifest.json` 的 `rawOutputPath` 与 `sheet-plan.yaml.outputName` 完全一致。
3. `ownerExpectedKeys` 非空，`ownerSheetIds` 包含三张 Round 1 sheet，且 `ownerMissingKeys=[]`、`ownerPendingKeys=[]`、`ownerOldStyleKeys=[]`。
4. `ownerCoveredKeys` 必须与 `ownerExpectedKeys` 完全一致，`allowedOwnerFallbackKeys=[]`，证明 canonical/runtime manifest 都指向 `dark-v1/ui/*`。
5. manifest 中新增 PR-02 owner key 都能通过 `VisualManifestResolver` 解析。
6. focused client test 能证明至少一个 panel、一个 slot、一个 modal chrome 和至少两个 HUD/icon key 被 `drawAsset` 消费。
7. golden 能看到至少一个 panel/slot/modal chrome 和至少一个 HUD/icon 资源。
8. 首页、验证 setup、结算页、错误页至少各能解析共享 screen chrome/control key；未进入当前 golden 的 screen 必须在 PR-07 evidence index 中补证据。
9. 资源加载失败时使用正式 manifest fallback，不出现空白方块或 crash。

## 7. 验证

Gradle 命令必须串行执行。资源证据必须先 sync runtime manifest，再跑 resolver / coverage / golden：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew syncPhase2Manifests manifestLint
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint -Pktome.darkUiux.requireFullGrid=true -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.screen.StandaloneScreenLayoutTest
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew verifyChanged
```

PR close 不允许用裸 `darkManifestCoverageLint` 代替本 PR close gate。`build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` 必须满足：

1. `scopeMode=owner-scope`
2. `ownerPr=PR-02`
3. `ownerExpectedKeys` 非空
4. `ownerSheetIds` 包含 `r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons`
5. `ownerMissingKeys=[]`
6. `requiredOwnerSheetIds=["r01-ui-chrome","r01-ui-controls","r01-ui-hud-icons"]`
7. `ownerCoveredKeys == ownerExpectedKeys`
8. `ownerPendingKeys=[]`
9. `ownerOldStyleKeys=[]`
10. `allowedOwnerFallbackKeys=[]`

只强制 Round 1 UI chrome/HUD key；scope 外 pending 必须写入 coverage artifact。不得用裸 `darkManifestCoverageLint` 代替本 PR close gate。

## 8. 白盒证据

| label | scenario / steps | expected evidence | artifact | skip rule |
| --- | --- | --- | --- | --- |
| `dark-uiux-pr02-round1-chrome` | 固定 seed 进入局内 shell，打开 tooltip/modal 或等价 validation scenario | screenshot / recording canvas 显示 `ui.frame.panel.body`、`ui.frame.slot.empty`、`ui.frame.modal.body` 已 resolved and drawn | `client/build/reports/golden/` + `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets` | 不允许 skip；如果 modal 无法在当前 harness 触发，必须补 focused canvas test |
| `dark-uiux-pr02-hud-icons-pilot` | 固定 snapshot 下 HUD 显示 HP / stamina / XP / gold 或等价 HUD row | 至少两个 `ui.hud.*` key 来自 `r01-ui-hud-icons`，coverage artifact 中对应 key covered | golden + `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` | 如果 gold/key 未进入当前 snapshot，写 PR-07 pending，但 HP/stamina/XP 至少两个不可 skip |
| `dark-uiux-pr02-standalone-screen-chrome` | 首页或 validation setup，保留返回/确认/copy action 可见 | `ui.frame.panel.body` 作为共享 chrome source 被解析并绘制，screen action 继续消费 `ui.control.*` key | golden/manual + `StandaloneScreenLayoutTest.darkUiuxPr02StandaloneChromeConsumesManifestKeys` | outcome/error/loading 如果本 PR 不截图，manual record 必须列 skip reason、resolver evidence 和 PR-07 evidence label |
| `dark-uiux-pr02-contact-sheet-qa` | 打开三张 contact sheet，逐格核对 `row,col,targetKey` | 每个 direct cell 语义与 §4 表一致，无文字、水印、串格、跨格 | `assets-src/image/contact-sheets/dark-v1/*-contact.png` + sprite map report | 不允许 skip；失败时重生成 raw sheet，不改 row/col 迁就图片 |

contact sheet QA 路径、manifest diff 路径、coverage artifact 路径和 golden/manual label 必须写入 PR 描述，并同步写入 `UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md`。

Manual record 必须使用以下字段，不得只保留在 GitHub PR 文本中：

| Field | Rule |
| --- | --- |
| `promptIndexPath` | `UI/sprite-sheets/prompts/dark-v1/prompt-index.json` |
| `rawSheetPaths` | 三张 `assets-src/image/raw/sheets/dark-v1/{sheetId}.png` |
| `rawSheetHashes` | 每张 raw sheet 的 sha256 |
| `contactSheetPaths` | 三张 `assets-src/image/contact-sheets/dark-v1/{sheetId}-contact.png` |
| `spriteMapReportPath` | `assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl` |
| `canonicalManifestDiff` | canonical manifest diff 摘要或 commit hunk 引用 |
| `runtimeManifestDiff` | `syncPhase2Manifests` 后 runtime manifest diff 摘要 |
| `coverageReportPath` | `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` |
| `ownerExpectedKeys / ownerCoveredKeys / ownerMissingKeys / ownerPendingKeys / ownerOldStyleKeys` | 从 coverage artifact 机械摘录 |
| `skippedScreens` | `screen / reason / replacementResolverEvidence / pr07OwnerLabel / residualRisk` |
| `goldenLabels` | 本 PR 生成或复用的 golden label |
| `manualReviewer / reviewedAt` | 人工 QA 记录；未执行时写 `NOT_RUN` 和原因 |

## 9. 回滚边界

本 PR 可以通过回滚 `dark-v1/ui` runtime PNG、`sheet-plan.yaml` Round 1 cell、canonical manifest 新 key、同步生成的 runtime manifest 和 renderer 消费点完整回退；不得把新 key 写入 `core` 或 `game`。

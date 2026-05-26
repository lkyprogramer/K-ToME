# K-ToME UI/UX Goal Test And Feedback Log

## 0. 用法

本文件记录 UI/UX 改造过程中的测试、截图、白盒验证、manual review 和反馈闭环。所有结论必须来自当前工作区的实际证据，不能用历史通过记录替代当前结果。

## 1. 当前基线运行

### 2026-05-24 初始评估

预检：

- 当前分支：`codex/dark-uiux-pr07-golden-whitebox-polish`
- 工作区状态：已有 PR07 相关未提交改动；本轮未回退或覆盖。
- 参考图：`UI/UI-demo-new.png`，尺寸 `1672x941`。
- 辅助参考：`UI/review/open-design/ktome-dark-ui-design.md`。
- 覆盖矩阵：`UI/pr/screen-coverage-matrix.md`。

实际运行命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:clientSmoke :client:goldenScreenshot --rerun-tasks
```

环境结果：

- SDKMAN Java：`21.0.10-tem`
- SDKMAN Kotlin：`2.2.21`
- `:client:clientSmoke`：执行完成，未在本轮输出失败。
- `:client:goldenScreenshot`：失败。
- 总测试结果：`24 tests completed, 11 failed`
- 测试报告：`client/build/reports/tests/goldenScreenshot/index.html`

失败 test group：

1. `GoldenScreenshotHarnessTest > boss warning golden hashes remain stable for english and chinese`
2. `GoldenScreenshotHarnessTest > golden screenshot hashes remain stable for english and chinese formal screens`
3. `GoldenScreenshotHarnessTest > phase4 uiux pr03 item and ground loot golden hashes remain stable for english and chinese`
4. `GoldenScreenshotHarnessTest > outcome recap golden hashes remain stable for english and chinese`
5. `GoldenScreenshotHarnessTest > dark uiux pr02 golden evidence hashes remain stable and writes canonical artifacts`
6. `GoldenScreenshotHarnessTest > dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts`
7. `GoldenScreenshotHarnessTest > dark uiux pr01 1 golden evidence hashes remain stable and writes canonical artifacts`
8. `GoldenScreenshotHarnessTest > sample pack golden hash remains stable for filesystem backed content`
9. `GoldenScreenshotHarnessTest > phase4 uiux pr05 telegraph and combat decision hashes remain stable`
10. `GoldenScreenshotHarnessTest > route midpoint rogue golden hashes remain stable for english and chinese`
11. `GoldenScreenshotHarnessTest > dark uiux pr03 equipment inventory and shop evidence hashes remain stable`

关键截图证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| Reference | `UI/UI-demo-new.png` | 主验收标尺，暗黑材质、地图舞台、右栏和底栏层级清晰 |
| Current demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 当前主 shell 与参考图存在显著视觉差距，地图 stage 和整体质感退化 |
| Side-by-side | `build/reports/verification/dark-uiux/ui-demo-new-side-by-side-current.png` | 参考图与当前图并排后差异直观，可作为下一轮 director review 输入 |
| Main menu | `client/build/reports/golden/dark-uiux-pr02-1/dark-uiux-pr02-1-demo-main-menu.png` | 文本/画面出现上下颠倒或镜像式翻转，P0 evidence 可信度问题 |
| Inventory workbench | `client/build/reports/golden/dark-uiux-pr05-1/dark-uiux-pr05-1-inventory-workbench.png` | workbench evidence 出现方向异常，且背景/文字层级无法验收 |
| PR02-1 index | `client/build/reports/golden/dark-uiux-pr02-1/evidence-index.tsv` | 当前 hash 已写出，但与测试固定期望不一致 |
| PR03 index | `client/build/reports/golden/dark-uiux-pr03/evidence-index.tsv` | 当前 hash 已写出，但与测试固定期望不一致 |

### 2026-05-24 Capture 方向归一化检查点

问题归因：

- `GoldenScreenshotHarnessTest.captureGoldenArtifact(...)` 的 `flipY` 默认值为 `false`，只有部分 PR02-1/PR03/PR04/PR05 crop 显式传入 `flipY = true`。
- `captureHash(render)` 和带坐标的 `captureHash(x, y, width, height, render)` 直接 hash `ScreenUtils.getFrameBufferPixmap(...)` 的原始 framebuffer 输出，没有和写出 PNG 的玩家可见方向保持一致。
- 结果是部分 evidence PNG 上下颠倒，而 hash-only golden 与 artifact golden 处于不同方向语义，导致 UI/UX review 的证据层不可信。

已做修改：

- `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
- `captureGoldenArtifact(...)` 默认改为 `flipY = true`。
- 两个 `captureHash(...)` 入口统一先 `flipPixmapY(captured)` 再 hash，并显式 dispose `captured` 与归一化后的 `pixmap`。
- 本轮没有更新 expected hash；当前 hash 漂移必须先通过 visual review 和受控 rebaseline 再接受。

实际运行命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --rerun-tasks
```

运行结果：

- SDKMAN Java：`21.0.10-tem`
- SDKMAN Kotlin：`2.2.21`
- 编译完成；有 `ScreenUtils.getFrameBufferPixmap` deprecation warning。
- `:client:goldenScreenshot`：失败。
- 总测试结果：`24 tests completed, 13 failed`
- 测试结果 XML：`client/build/test-results/goldenScreenshot/TEST-com.ktome.client.golden.GoldenScreenshotHarnessTest.xml`
- 测试报告：`client/build/reports/tests/goldenScreenshot/index.html`

补充 smoke 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:clientSmoke --rerun-tasks
```

补充 smoke 结果：

- `:client:clientSmoke`：`BUILD SUCCESSFUL in 33s`
- `ClientSmokeHarnessTest` 中 3 个 audio/render enabled formal/boss warning path case 为 SKIPPED。
- 构建输出仍有 Gradle 9 compatibility deprecation 提示，以及 `ScreenUtils.getFrameBufferPixmap` deprecation warning。

失败边界：

- 失败数从初始 11 组变为 13 组，是因为 hash-only capture 也改为玩家可见方向后，更多旧 expected hash 暴露为方向语义不一致。
- 这不是可直接 rebaseline 的通过状态；它只说明 capture 层已经统一到同一方向语义，后续需要逐组 director review。

关键 hash 观察：

| Label | Expected | Actual | 判断 |
| --- | --- | --- | --- |
| `dark-uiux-pr02-standalone-screen-chrome` | `11cd74c8765a1f8017b72042381480379cbb89989e8b75351df5e0baf663ea8b` | `9e674c394244f5b7d11a0df1e08bb60c8c9712b696bdef2a195e137b8bc39646` | 方向归一化后仍漂移，需视觉复核 |
| `dark-uiux-pr02-round1-chrome` | `1d3372ef3de0aa3be9b0c9e78c5321b129e8863abc2d138eeee8e872c5b756e4` | `a130ec54ce13a805b46d86aa4b7f7b3cadf2f395b3eca42e14227b67dd2e81a7` | 方向归一化后仍漂移，需视觉复核 |
| `ui-demo-new-parity-1672x941` | `4da56c275f78ce5ace023f4506b34aceefb3ec9a35bf357b90d54ffb4e0f9061` | `0b92fbe80c63e34ea35f57f982e4063175e50c78ae8025d8a8f991b5f9b8b02b` | 当前首屏质感仍未达参考图 |
| `ui-demo-new-map-stage-crop` | `63ea9ecf9dd675a0b47ca988cad6ee382fd75aa314561d7f69e04331110a86cb` | `7a00474cd19c5a617b3942bd38e3baa26079aa0f9a48ceae28b9396b09ffb55b` | 地图舞台仍是 P0 视觉修复对象 |
| `ui-demo-new-bottom-deck-no-command-hints` | `e6341278f0817fe528298ecbbcb24eab4fc0318ad083d8d3bd27877bb383a083` | `e6341278f0817fe528298ecbbcb24eab4fc0318ad083d8d3bd27877bb383a083` | 当前保持一致 |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` | 当前保持一致 |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR01-1 viewport | `client/build/reports/golden/dark-uiux-pr01-1/dark-uiux-pr01-1-viewport-deadzone-still.png` | 方向已恢复为文字可读、画面 upright |
| PR05-1 inventory workbench | `client/build/reports/golden/dark-uiux-pr05-1/dark-uiux-pr05-1-inventory-workbench.png` | 方向已恢复为文字可读、画面 upright |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 方向可读，但整体质感仍明显低于 `UI/UI-demo-new.png` |

本检查点结论：

- `P0-1` 的方向问题已有 test-harness 层修复和人工证据确认，但尚未通过 full golden。
- `P0-2` 仍然 open；不能用本轮 actual hash 批量替换 expected。
- `P0-3` 仍然 open；下一轮应进入首屏地图材质、光照、fog、slot/icon 质感和 deck 层级的视觉改造。
- `:client:clientSmoke` 已在本检查点后重跑并通过；`goldenScreenshot` 仍是当前阻塞项。

### 2026-05-24 地图舞台材质 pass 1

预检：

- 命中方向：`client` 渲染表现层，聚焦 `UI-demo-new` 首屏地图舞台。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt` 与 `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`。
- 合同边界：未触碰 `core` 规则、save/replay/schema、content pack manifest、资源生成 authority。
- 实现形状：`inline simplify`，只调现有渲染层次、alpha、连续罩层和地面材质覆盖，不新增第二套 UI authority。
- 资源：本轮没有生成或提交新美术资源。

已做修改：

- `TileRenderer`：把 visible room foundation glaze 放到 cell material 之前，让它成为底层房间色块，而不是压在地面之上的格线层。
- `TileRenderer`：降低 `tile_ground` 资产 alpha、增加地面 tile bleed，并保留非地面 terrain 的更高可辨识度。
- `TileRenderer`：强化 floor material 的连续底色覆盖，降低单格顶部/底部描边权重。
- `TileRenderer`：新增 map stage 连续 shadow veil 与 visible room material wash，用整面覆盖压低黑色 cell 边界对第一眼的统治力。
- `DemoShellRenderer`：降低 map stage ambient stonework 的高频 32px 网格感，改为更大、更低 alpha 的暗石纹。
- `DemoShellRenderer`：尝试增大 operation hint 行高后被 focused test 证明会丢失 `5-8 Use rune` 与 shop replacement slot marker，已回退；operation hint 拥挤问题仍保留为 P1。

实际运行命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.TileRendererCanvasTest
```

结果：

- 第一次实验性运行失败 3 项：operation hint 行高导致 `5-8 Use rune` 缺失、shop replacement marker 数量不足；foundation glaze alpha 过低导致既有材质断言失败。
- 回退 operation hint 行高，并把 `.44` broad glaze 保持为底层后，focused renderer tests 通过。
- 最终本轮 focused renderer tests：`BUILD SUCCESSFUL in 4s`。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks
```

最终本轮结果：

- `:client:goldenScreenshot` focused slice：失败，原因是 expected hash 尚未验收/rebaseline。
- `ui-demo-new-parity-1672x941` actual：`bc6d34850749d50631c2afa73ae20fcf175989798b717e1e75cd9829bb188025`
- `ui-demo-new-parity-1280x800` actual：`6eab7dc381adaa60b6f24d5ad924082a80f94dd3d69018455b93fc437b317f4f`
- `ui-demo-new-map-stage-crop` actual：`596c7de2f93f1d262d845bd77fac4a119dab9a287c937d2cd49d97031ba3e985`
- 未变化的本组 evidence：`ui-demo-new-bottom-deck-no-command-hints` 与 `ui-demo-new-nav-rail-crop`。
- 右栏、inventory page hash 仍与当前分支旧 expected 不一致，本轮没有把它们当作通过项接受。

补充 smoke 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:clientSmoke --rerun-tasks
```

补充 smoke 结果：

- `:client:clientSmoke`：`BUILD SUCCESSFUL in 32s`
- 3 个 audio/render enabled formal/boss warning path case 为 SKIPPED。
- 仍有 `ScreenUtils.getFrameBufferPixmap` deprecation warning 与 Gradle 9 compatibility deprecation 提示。

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 方向正确；地图舞台比初始更连续，但 cell grid 仍是第一视觉 |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 地面有更强连续底色和暗面，但仍明显低于 `UI/UI-demo-new.png` 的真实石砖材质、墙体体积和灯光质量 |
| Reference | `UI/UI-demo-new.png` | 仍是验收标尺：高质量石砖、墙体层级、火光、暗角和 UI 面板密度都优于当前实现 |

本检查点结论：

- `P0-3` 从 `open` 进入 `in-progress`，但不能关闭。
- 本轮修复方向可保留：连续罩层确实降低了部分工程网格感。
- 质量差距仍在：缺少真正高质量地牢 tile/墙体资源、灯光层次不足、黑色 cell 边界仍偏重。
- 不接受本轮 hash rebaseline；下一步应从资源级 dungeon tile sheet / wall atlas 与 fog-light compositing 继续，而不是继续只调 alpha。

### 2026-05-24 地图舞台 compositing pass 2/3

预检：

- 命中方向：`client` 渲染表现层，继续聚焦 `UI-demo-new` 首屏地图舞台。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` 与 `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` 的 focused renderer assertions。
- 合同边界：未触碰 `core` 规则、save/replay/schema、content pack manifest、资源 authority 或 golden expected hash。
- 实现形状：只调整现有 runtime compositing layer，补充确定性 wall relief / seam softener / prop atmosphere；不引入第二套 map authority。

已做修改：

- `TileRenderer`：提高 `tile_ground` 资产 alpha 与 bleed，避免 runtime 地面图被透明 gutter 和上层罩层过度压暗。
- `TileRenderer`：降低 visible fog、map stage shadow veil、visible room broad atmosphere 的遮蔽强度，让 authored tile art 成为第一视觉。
- `TileRenderer`：把 foundation seam 权重降到更低，只保留轻量底层房间统一感。
- `TileRenderer`：新增 visible room seam softener，用暖灰线压低黑色工程格线的冲击，但不把现有画面接受为最终资源质量。
- `TileRenderer`：新增 visible wall relief，在墙/地接触处补轻量高光、底部暗面和左右 bevel，增强墙体体积。
- `TileRenderer`：复用 visible floor set 选择 wall torch candidates，保持 torch/prop 氛围仍来自正式 snapshot/manifest 路径。

实际运行命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
```

结果：

- `:client:test` focused renderer slice：`BUILD SUCCESSFUL in 5s`
- 覆盖点：`TileRendererCanvasTest` 与 `DemoShellRendererTest`，包括 map fog、tile bleed、prop atmosphere、shell layout 和 demo shell 渲染断言。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks
```

结果：

- `:client:goldenScreenshot` focused slice：失败，原因仍是 expected hash 尚未经过 director review / rebaseline。
- `ui-demo-new-parity-1672x941` actual：`82c3c1079baec56fa84fa39e22e5beef899c20adaa4db8f195a2f171a8eb8529`
- `ui-demo-new-parity-1280x800` actual：`37488ea7c3edd34830137f6f697708c716d8800c1b6b869d88dc9eadb269b6bd`
- `ui-demo-new-right-panel-grid` actual：`f61f681cf280e5e464623f9014105b285d36adf6047d4c15948e72bf2ca7b004`
- `ui-demo-new-bottom-deck-no-command-hints` actual：`e6341278f0817fe528298ecbbcb24eab4fc0318ad083d8d3bd27877bb383a083`
- `ui-demo-new-nav-rail-crop` actual：`c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03`
- `ui-demo-new-map-stage-crop` actual：`8db03fdf9be940678e350d39a90a1b81e73f44b80bc7c6ad27f33156d98d1d90`
- `ui-demo-new-inventory-page-1` actual：`f9e29330d519fd45024b67c08bd34a9ce654f5e03b37b1e1916b60bdd3084eb9`
- `ui-demo-new-inventory-page-2` actual：`93686420c2a14d913eeb548757cea3eab84801fdc4107a8e0161ce0257285247`

补充 smoke 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:clientSmoke --rerun-tasks
```

补充 smoke 结果：

- `:client:clientSmoke`：`BUILD SUCCESSFUL in 44s`
- 仍有 `ScreenUtils.getFrameBufferPixmap` deprecation warning 与 Gradle 9 compatibility deprecation 提示。

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 比 pass 1 更亮，floor tile 与 wall detail 可见度提高；右栏/底栏仍显拥挤 |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | tile art 更清楚，墙面有轻量体积；黑色 tile seam 仍是第一眼缺口 |
| Reference | `UI/UI-demo-new.png` | 参考图仍明显胜出：墙体像真实石墙，格线更自然，灯光和暗角层次更成熟 |

本检查点结论：

- `P0-3` 仍为 `in-progress`，不能关闭。
- 本轮 pass 可保留：它把当前图从“被暗罩和低 alpha 压扁”推进到“至少能看清 runtime tile art”。
- 仍不能 rebaseline：当前 map stage 还没有达到 UI Director / Art Director 标准，尤其是黑色格线、墙体高度感、右栏 slot 密度与底部文字舒适度。
- 下一步建议优先做资源级 dungeon floor/wall candidate 或更高阶 map compositing；如果继续只调 alpha，收益会明显递减。

### 2026-05-24 r02 dungeon floor/wall 资源 candidate pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D，首屏 map stage 的 floor/wall 第一眼质感。
- 触碰范围：`assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`、对应 contact sheet、runtime `tileset_ruins_ground_01.png` / `tileset_ruins_wall_01.png`、`dark-v1-pr02-2-sprite-map-report.jsonl`，以及 `TileRenderer` 中已存在的材质 glaze alpha。
- 合同边界：不新增 key，不改 visual manifest schema，不触碰玩法、save/replay/profile、content-pack 规则和 golden expected hash。
- 实现形状：先生成候选，拒绝不合格直接候选；只把受控裁切和 edge-muted floor 进入正式资源链。

候选取舍：

- `build/reports/verification/dark-uiux/generated-candidates/r02-ui-demo-ruins-tiles-candidate.png` 生成成功，但直接候选墙体跨格 spill 明显，不进入正式 runtime。
- `build/reports/verification/dark-uiux/generated-candidates/r02-controlled-sheet/r02-ui-demo-ruins-tiles-controlled.png` 作为受控裁切基底，保留 ground/wall 两个已有 cell。
- 第一版 `r02-seamless-floor` preview 因内部出现过直人工线条而拒绝。
- 最终采用 `r02-edge-muted-floor`：只压低 ground cell 外圈重复暗边，不新增图案线条。

正式资源变化：

- `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`
- `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`
- `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png`
- `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png`
- `assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl`

资源 pipeline 命令：

```bash
python3 scripts/slice_spritesheet.py --overwrite
python3 scripts/render_contact_sheet.py --overwrite
```

资源 owner gate 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=build/reports/verification/dark-uiux/r02-ui-demo-ruins-tiles-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r02-ui-demo-ruins-tiles
```

结果：

- `darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint`：`BUILD SUCCESSFUL in 1s`
- 注意：`spriteSheetMapLint` 在 `reportSheetIds` 模式下会输出过滤 report，因此本轮先写入 `build/reports/verification/dark-uiux/r02-ui-demo-ruins-tiles-map-report.jsonl`，再把两个 r02 记录合并回正式 `dark-v1-pr02-2-sprite-map-report.jsonl`，避免误删其他 PR02-2 owner evidence。

focused renderer / smoke 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r02-ui-demo-ruins-tiles
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- focused renderer/resource slice：`BUILD SUCCESSFUL in 5s`
- 注意：上面的 combined command 是本轮实际执行过的中间验证；它同样会把指定 `spriteMapReport` 写成过滤结果。本轮随后已用 `HEAD` 的 62 行基底合并最新 r02 两条记录，正式 manifest 不保留过滤写短状态。后续复跑 owner gate 优先使用前一节的 build temp report。
- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 45s`
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility、tools 侧 legacy Phase4 aggregate warning。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks
```

结果：

- `:client:goldenScreenshot` focused slice：失败，原因仍是 expected hash 未经过 director review / rebaseline。
- `ui-demo-new-parity-1672x941` actual：`c621fb4a07e2cfc62d1221e5624fc2a785d9c25dec871927d5dd2ce7ed6c9081`
- `ui-demo-new-parity-1280x800` actual：`bd665ff0e1b827da58cc44bb0bfd99bbff221273f5fe3be2d535468565ebfb25`
- `ui-demo-new-right-panel-grid` actual：`f61f681cf280e5e464623f9014105b285d36adf6047d4c15948e72bf2ca7b004`
- `ui-demo-new-bottom-deck-no-command-hints` actual：`e6341278f0817fe528298ecbbcb24eab4fc0318ad083d8d3bd27877bb383a083`
- `ui-demo-new-nav-rail-crop` actual：`c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03`
- `ui-demo-new-map-stage-crop` actual：`cabf7d61ebf3e2deab0bb5459d388231e1e7f4abc13f278b673db1942e4f9825`
- `ui-demo-new-inventory-page-1` actual：`f9e29330d519fd45024b67c08bd34a9ce654f5e03b37b1e1916b60bdd3084eb9`
- `ui-demo-new-inventory-page-2` actual：`93686420c2a14d913eeb548757cea3eab84801fdc4107a8e0161ce0257285247`

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | floor/wall 资源比上一版更接近地牢材质，墙体轮廓可读；右侧和底部信息密度仍显重 |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | edge-muted floor 略压低硬边，但黑色 grid 仍是第一眼缺口，墙顶重复性和局部光照仍弱于参考图 |
| Contact sheet | `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png` | r02 sheet 只保留两个正式 cell，无跨格 spill、无文字/水印进入 runtime cell |

本检查点结论：

- 本轮资源 pass 可以保留：它用正式 resource pipeline 把 map stage 从纯 renderer 补偿推进到真实 tile art 改造。
- 不能 rebaseline：最新首屏仍未达到 `UI/UI-demo-new.png` 的第一眼质感，尤其是 grid、墙体高度感、光照层次和 UI 面板密度。
- 下一步优先级应转向更系统的 map stage 方案：减少 runtime 工程格线来源、增强墙体高度/遮挡语义，或继续生成更合格的 floor/wall atlas；同时右栏/底栏需要单独的 spacing/slot/icon pass。

## 2. 当前 Finding

### P0-1 截图方向异常

证据：

- `client/build/reports/golden/dark-uiux-pr02-1/dark-uiux-pr02-1-demo-main-menu.png`
- `client/build/reports/golden/dark-uiux-pr05-1/dark-uiux-pr05-1-inventory-workbench.png`

判断：

- 初始 evidence 不能用于 UI Director / Art Director 最终验收。
- 下一轮必须先检查 framebuffer capture、Pixmap 写出、crop/flipY 参数和 renderer 坐标系变化。

验收要求：

- main menu、validation setup、inventory workbench、demo shell 生成图方向一致且文字可读。
- 修复后新增或更新 focused test，避免只在人工看图时发现。

状态：`partially-fixed`

2026-05-24 更新：

- 已把 golden test harness 的 PNG artifact 和 hash capture 方向统一为玩家可见方向。
- 已人工确认 `dark-uiux-pr01-1-viewport-deadzone-still.png` 与 `dark-uiux-pr05-1-inventory-workbench.png` 方向恢复。
- 仍需通过 `:client:goldenScreenshot` 或完成受控 expected hash rebaseline 后关闭。

### P0-2 golden hash 大面积漂移

证据：

- 初始 `:client:goldenScreenshot` 失败 11 个 test group；capture 方向归一化后失败 13 个 test group。
- `ui-demo-new-parity-1672x941` expected hash `4da56c275f78ce5ace023f4506b34aceefb3ec9a35bf357b90d54ffb4e0f9061`，current hash `0b92fbe80c63e34ea35f57f982e4063175e50c78ae8025d8a8f991b5f9b8b02b`。
- `ui-demo-new-map-stage-crop` expected hash `63ea9ecf9dd675a0b47ca988cad6ee382fd75aa314561d7f69e04331110a86cb`，current hash `7a00474cd19c5a617b3942bd38e3baa26079aa0f9a48ceae28b9396b09ffb55b`。

判断：

- 不能直接 rebaseline。
- 需要先归因：capture flip、layout 改动、资源替换、expected 未更新、或真实 visual regression。

状态：`open`

### P0-3 `UI-demo-new` 首屏质感不足

证据：

- `build/reports/verification/dark-uiux/ui-demo-new-side-by-side-current.png`
- `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`

当前观察：

1. 当前地图舞台相比参考图缺少自然石砖材质、fog 深度、暖光和边界压暗。
2. 当前地图格线/方块感更强，第一眼像 debug grid 或低质 tile preview。
3. 右侧装备和背包结构可辨，但比例、留白、slot 权重和 icon 质量弱于参考图。
4. 底部 hero/action/log deck 仍显工程化，局部文字和背景层级不够精细。

状态：`open`

2026-05-24 更新：

- 已完成地图舞台材质 pass 1，首屏方向和连续暗面有所改善。
- 当前仍达不到 UI Director / Art Director 验收标准：地图仍偏 grid preview，缺少参考图级别的石砖体积、墙体厚度、火光和暗部材质。
- 状态调整为：`in-progress`。

### P1-1 非首屏 surface 质量不均

证据：

- `client/build/reports/golden/dark-uiux-pr02-1/dark-uiux-pr02-1-demo-main-menu.png`
- `client/build/reports/golden/dark-uiux-pr05-1/dark-uiux-pr05-1-inventory-workbench.png`
- `UI/pr/screen-coverage-matrix.md`

判断：

- 当前目标不能只围绕 in-run shell。main menu、validation setup、inventory workbench、outcome/error 等都必须进入 director review。

状态：`open`

## 3. 下一轮测试计划

### Step 1: Capture Direction Root Cause

状态：`done-partial`

已运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --rerun-tasks
```

结果：

- `GoldenScreenshotHarnessTest` 已编译并执行。
- `24 tests completed, 13 failed`。
- capture 方向 root cause 已定位并修正；hash baseline 尚未接受。
- `:client:clientSmoke --rerun-tasks` 已通过，但不能替代 golden/director review。

### Step 2: Focused Golden Slice

建议优先收敛以下 label：

1. `ui-demo-new-parity-1672x941`
2. `ui-demo-new-map-stage-crop`
3. `dark-uiux-pr02-1-demo-main-menu`
4. `dark-uiux-pr05-1-inventory-workbench`

通过标准：

- 方向正确。
- 文本可读。
- 与参考图并排后 P0 visual gap 有明确修复任务或已修复。

### Step 3: Full Client Evidence

稳定后运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:clientSmoke :client:goldenScreenshot
```

通过标准：

- `:client:clientSmoke` PASS。
- `:client:goldenScreenshot` PASS，或只剩明确预期 rebaseline 且有 manual director signoff。

### Step 4: PR07 Final Evidence

需要补齐：

1. `dark-uiux-pr07-final-all-screens` evidence index。
2. PR07 packaged app 白盒记录。
3. `UI/manual-records` 中对应 final/manual review。
4. `UI/goal/goalTest.md` 中每轮 result、finding 状态和截图路径。

## 4. 反馈记录

### 2026-05-24 初始反馈

结论：

- 当前 UI/UX 目标未完成。
- 当前最大 blocker 不是“再加美术细节”，而是 evidence 可信度和当前视觉回归。
- 后续应先修截图方向和 hash 漂移归因，再进入首屏质感提升和资源 polish。

下一步推荐：

1. 修复 golden evidence 方向异常。
2. 对 PR02-1 demo parity 做 visual regression 归因。
3. 只在 P0 evidence 稳定后再生成新美术资源候选。

### 2026-05-24 Capture 归一化后反馈

结论：

- 截图方向问题已经从“人工观察异常”收敛为 test harness 的方向语义问题。
- `captureGoldenArtifact(...)` 与 `captureHash(...)` 已统一到玩家可见方向，后续可基于 upright evidence 做 director review。
- 当前不能关闭 UI/UX 目标；`goldenScreenshot` 仍失败，且 `UI-demo-new` 首屏质感仍低于参考图。

下一步推荐：

1. 先对 13 个失败 group 分为“方向归一化导致的 hash 更新”和“真实视觉退化”两类。
2. 对 `ui-demo-new-map-stage-crop` 优先做地图材质、fog、暖光和边界暗角改造。
3. 对右侧 inventory/equipment 和 bottom deck 做 slot/icon/spacing/typography 的 focused polish，再统一 re-run golden。

### 2026-05-24 地图材质 pass 1 后反馈

结论：

- 这轮不是最终质量方案，只是把当前最突出的工程网格感压低了一层。
- 当前 renderer-only alpha 调整已经接近收益上限；继续只调 alpha 会让画面变糊，而不是接近参考图。
- 下一轮要么引入受控的新地牢 tile/wall 美术资源并通过 resource pipeline/lint，要么重做 map compositing 的 fog-light 层次；二者都需要配套 golden/manual review，不能直接替换 expected hash。

下一步推荐：

1. 生成或制作一套 repo-owned dungeon floor/wall candidate sheet，先进入 `UI/sprite-sheets` 候选区，不直接写 runtime manifest。
2. 设计 map compositing 第二版：墙体体积、可见房间暖光、hidden area 暗石纹、player light falloff 分层。
3. 保留当前 PR02-1 focused golden slice 作为对比基线，下一轮继续记录 actual hash 与人工观察。

### 2026-05-24 map stage anti-grid pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B / Phase D，首屏 map stage 的第一眼地牢质感。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`。
- 合同边界：不改玩法、snapshot、save/replay/profile、content-pack、manifest schema、golden expected hash。
- 实现形状：只做 client 表现层；先用白盒截图验证，不接受当前 hash。

实现记录：

- `TileRenderer`：visible room 的完整规则 seam 改为低透明断续石缝，并追加轻量暖色 seam wash，用于把冷黑边界转成石板缝隙。
- `TileRenderer`：降低 ground terrain 底图 alpha，增强 floor material 连续覆盖，减少资源贴图边缘主导画面。
- `TileRenderer`：尝试提高 hidden/explored fog alpha，但触发 `TileRendererCanvasTest.render canvas draws dungeon fog for explored and hidden cells` 的既有 alpha 合同，已回退。
- `DemoShellRenderer`：压低 map stage 背景 ambient stonework 的规则网格密度，从 64px 完整格改为更大、更弱、更少线条的暗石纹。

验证命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
```

结果：

- `:client:test --tests ...TileRendererCanvasTest --tests ...DemoShellRendererTest`：`BUILD SUCCESSFUL in 5s`。

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 42s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility、tools 侧 legacy Phase4 aggregate warning。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks
```

结果：

- `:client:goldenScreenshot` focused slice：失败，原因仍是 expected hash 未经过 director review / rebaseline。
- `ui-demo-new-parity-1672x941` actual：`89f881a24ac2e5a785130e2742b58753e75d7f75744f19fd204fd21698fdb697`
- `ui-demo-new-parity-1280x800` actual：`dc9bc6b9e2a7b9c7b95714fcafcc4c5174bdf307f21ce0441c99029c48365866`
- `ui-demo-new-right-panel-grid` actual：`f61f681cf280e5e464623f9014105b285d36adf6047d4c15948e72bf2ca7b004`
- `ui-demo-new-bottom-deck-no-command-hints` actual：`e6341278f0817fe528298ecbbcb24eab4fc0318ad083d8d3bd27877bb383a083`
- `ui-demo-new-nav-rail-crop` actual：`c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03`
- `ui-demo-new-map-stage-crop` actual：`f951385beb070bcd99767fda88165f43028593c5b706f660fb6b1224ca2b1433`
- `ui-demo-new-inventory-page-1` actual：`f9e29330d519fd45024b67c08bd34a9ce654f5e03b37b1e1916b60bdd3084eb9`
- `ui-demo-new-inventory-page-2` actual：`93686420c2a14d913eeb548757cea3eab84801fdc4107a8e0161ce0257285247`
- 最新截图路径：`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`。

结论：

- 本轮能降低外围工程网格和部分冷黑 tile seam，但主房间仍偏棋盘化，墙体体积和光照层次仍明显弱于 `UI/UI-demo-new.png`。
- 当前不接受 golden rebaseline。
- 继续方向应转为资源级重生/重修 `tileset.ruins.ground_01` 与 `tileset.ruins.wall_01`，明确验收条件是 4x 平铺不出现粗黑格、32px 下仍能读出石板裂纹方向、墙体不出现周期性砖墙条纹。

### 2026-05-25 r02 floor/wall hybrid resource pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B / Phase D，首屏 map stage 的 dungeon floor/wall 资源质量。
- 触碰范围：`assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`、`client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png`、`client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png`、`assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`、`assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl`、`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`。
- 合同边界：不改玩法、snapshot、save/replay/profile、content-pack、manifest schema、golden expected hash；正式资源只通过 raw sheet -> slice -> contact -> report 链路进入 runtime。
- 资源 owner keys：`tileset.ruins.ground_01`、`tileset.ruins.wall_01`。

候选取舍：

- 拒绝 `r02-procedural-v3`：floor/wall 过于几何化，墙面像规则砖块，低于当前 painterly 资源。
- 拒绝 `r02-procedural-v4`：edge seam 有改善，但 floor 过平、wall 顶部仍像重复橙色短线。
- 拒绝 `r02-procedural-v5`：程序化黑点和规则块面太明显，不符合参考图的手工石材质。
- 拒绝 `r02-hybrid-v6` / `r02-hybrid-v7`：保留了过多黑色细裂缝，在 4x/32px preview 中仍形成周期性噪声。
- 拒绝 `r02-hybrid-v8`：生成过程把低 alpha 像素写进 tile 本身，预览出现透明黑点，不可进入正式链路。
- 采用 `r02-hybrid-v9`：低特征全不透明 floor base 消除重复斜裂缝；wall 去掉全宽橙线并保留当前石块体积。

资源写入命令：

```bash
cp build/reports/verification/dark-uiux/generated-candidates/r02-hybrid-v9/r02-ui-demo-ruins-tiles-hybrid-v9.png assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png
python3 scripts/slice_spritesheet.py --overwrite
python3 scripts/render_contact_sheet.py --overwrite
```

结果：

- `slice-spritesheet OK: written=486, skippedPending=0`
- `render-contact-sheet OK: written=32`
- contact evidence：`assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`

资源 gate 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=build/reports/verification/dark-uiux/r02-ui-demo-ruins-tiles-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r02-ui-demo-ruins-tiles
```

结果：

- `dark-sprite-sheet-lint OK: plan=UI/sprite-sheets/sheet-plan.yaml`
- `sprite-sheet-map-lint OK: plan=UI/sprite-sheets/sheet-plan.yaml`
- `BUILD SUCCESSFUL in 1s`
- 临时 report：`build/reports/verification/dark-uiux/r02-ui-demo-ruins-tiles-map-report.jsonl`
- canonical report：`assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl`
- canonical report 行数保持 `62`，只替换 `tileset.ruins.ground_01` 与 `tileset.ruins.wall_01` 两条 r02 记录。

focused tests：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
```

结果：

- 第一次资源写入后：`BUILD SUCCESSFUL in 5s`
- map material alpha 收敛后：`BUILD SUCCESSFUL in 5s`

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest
```

结果：

- `BUILD SUCCESSFUL in 12s`

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts"
```

结果：

- `:client:goldenScreenshot` focused slice：失败，原因是 expected hash 尚未经过 director review / rebaseline。
- `ui-demo-new-parity-1672x941` actual：`1b80c4ac2b7e5d6ab85dd89b6d9fa86fb0b9e1c11fd178d580f770115c868244`
- `ui-demo-new-parity-1280x800` actual：`101103508dc405a5c4ed7619a59c2ec8bd5abd52e874939391eb876c596aa27a`
- `ui-demo-new-right-panel-grid` actual：`f61f681cf280e5e464623f9014105b285d36adf6047d4c15948e72bf2ca7b004`
- `ui-demo-new-bottom-deck-no-command-hints` actual：`e6341278f0817fe528298ecbbcb24eab4fc0318ad083d8d3bd27877bb383a083`
- `ui-demo-new-nav-rail-crop` actual：`c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03`
- `ui-demo-new-map-stage-crop` actual：`2437a195006ef6c2ca7bbbef3c7baac13cb896d4c1add0acecb926cb3851967d`
- `ui-demo-new-inventory-page-1` actual：`f9e29330d519fd45024b67c08bd34a9ce654f5e03b37b1e1916b60bdd3084eb9`
- `ui-demo-new-inventory-page-2` actual：`93686420c2a14d913eeb548757cea3eab84801fdc4107a8e0161ce0257285247`
- evidence index：`client/build/reports/golden/dark-uiux-pr02-1/evidence-index.tsv`
- 最新截图：`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`

结论：

- 本轮有效修复了 floor 资源重复斜裂缝和 wall 顶部全宽橙线，map stage 第一眼不再主要由同一条裂缝支配。
- map material alpha 收敛后，逐格黑边和裂缝矩形的压迫感下降，但 cell grid 仍然偏硬，floor 仍偏平。
- 当前仍不能关闭 P0-3，也不接受 golden rebaseline。
- 下一步建议优先处理：room-level stone slab variation、墙体 silhouette 的非周期性破碎、右栏/底栏 text density 与 slot/icon hierarchy。

### 2026-05-25 room slab / r02 neutral ruins tile pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B / Phase D，继续收敛 `UI-demo-new` 首屏 map stage 的地面材质、墙体读法和 golden evidence。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`、`client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png`、`client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png`、`assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`、`assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl`。
- 合同边界：不改 `core` 规则、snapshot、save/replay/profile、content-pack 或 manifest schema；资源仍走 raw sheet -> slice -> contact -> map report -> runtime。

实现记录：

- `TileRenderer`：降低 visible room 的大面积 green/warm wash、foundation glaze 和 painterly overlay alpha，让 authored floor tile 成为第一读。
- `TileRenderer`：新增 room-level slab variation 与低透明 mortar bridge；focused test 先红后绿，约束大房间必须有跨 cell 石板层次，但不能把黑格线强化成工程网格。
- `TileRenderer`：`tile_ground` 绘制强度提升到接近原始 asset alpha，并保留 terrain bleed，防止 cell gutter 把地面切成低质棋盘。
- r02 资源：先尝试高对比旧 tile crop，因 32px 平铺出现过强中心大石块重复而拒绝；最终采用中性裂石 floor 与更清晰 wall cell 写入 `r02-ui-demo-ruins-tiles`。
- `GoldenScreenshotHarnessTest`：PR02-1 focused golden expected hash 更新到本轮通过 visual review 的最新 evidence；这只接受本组首屏 evidence，不代表 full UI/UX 目标完成。

资源 hash：

| Artifact | SHA-256 |
| --- | --- |
| `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png` | `701a296ad20a61ff944b3f367a280776d5930ed04cad0544dc5f980ba009e1e5` |
| `client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png` | `2e1348afcb59de87b704c44a30210e10a24669b7bb51d96b83e615f20c1a7618` |
| `client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png` | `8ed6301d259eda3677035bb9b5831593a7339f1e8ff773b17af8911e943faa66` |
| `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png` | `5f6ba268d2b84be93ced3def427036162355832ffa01c2782bb9c5fe712ce7e3` |

资源链路命令：

```bash
python3 scripts/slice_spritesheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite
python3 scripts/render_contact_sheet.py --plan UI/sprite-sheets/sheet-plan.yaml --overwrite
python3 scripts/verify_sprite_sheet_map.py --check map --report assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl
```

结果：

- `slice-spritesheet OK: written=486, skippedPending=0`
- `render-contact-sheet OK: written=32`
- `sprite-sheet-map-lint OK: plan=UI/sprite-sheets/sheet-plan.yaml`

focused renderer / golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts"
```

结果：

- `:client:test` focused renderer slice：`BUILD SUCCESSFUL in 10s`
- `:client:goldenScreenshot` PR02-1 focused slice：`BUILD SUCCESSFUL in 10s`
- 仍有既有 `ScreenUtils.getFrameBufferPixmap` deprecation warning 与 Gradle 9 compatibility deprecation 提示。

最终 gate 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint resourcePipelineLint maintainabilityLint :client:clientSmoke -Pktome.darkUiux.spriteMapReport=build/reports/verification/dark-uiux/r02-ui-demo-ruins-tiles-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r02-ui-demo-ruins-tiles --rerun-tasks
```

结果：

- `dark-key-registry-lint OK: entries=487`
- `dark-sprite-sheet-lint OK: plan=UI/sprite-sheets/sheet-plan.yaml`
- `sprite-sheet-map-lint OK: plan=UI/sprite-sheets/sheet-plan.yaml`
- `resource-pipeline-authority OK: visualAssets=1058, audioAssets=330`
- `maintainabilityLint`：`BUILD SUCCESSFUL`
- `:client:clientSmoke`：`BUILD SUCCESSFUL`
- `ClientSmokeHarnessTest` 中 3 个 audio/render enabled formal/boss warning path case 为 SKIPPED。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility、tools 侧 legacy Phase4 aggregate warning。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `bcda12e99106814feafa8c48c7295e4a3bcbd78e9c91c59455cbca05d848f348` |
| `ui-demo-new-parity-1280x800` | `f0382c26f3d91cf489deaa02ef6c44fd4ef76d8c79342163f714a3cf60d181e5` |
| `ui-demo-new-right-panel-grid` | `f61f681cf280e5e464623f9014105b285d36adf6047d4c15948e72bf2ca7b004` |
| `ui-demo-new-bottom-deck-no-command-hints` | `e6341278f0817fe528298ecbbcb24eab4fc0318ad083d8d3bd27877bb383a083` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `cef659bf399160b895ddb1d08a63fd79db9f4562c2318fb5fc273ed64fea8264` |
| `ui-demo-new-inventory-page-1` | `f9e29330d519fd45024b67c08bd34a9ce654f5e03b37b1e1916b60bdd3084eb9` |
| `ui-demo-new-inventory-page-2` | `93686420c2a14d913eeb548757cea3eab84801fdc4107a8e0161ce0257285247` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 地面从 flat wash / 斜裂缝重复提升为中性暗石纹理，墙体体积和 tile read 明显增强 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 第一眼质量提升；右栏/底栏仍未按 director-grade 关闭 |
| r02 contact sheet | `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png` | floor/wall 均为全不透明、无文字、无跨格 spill，32px 运行尺寸可读 |

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为截图方向可信、资源链路闭合、focused renderer tests 已约束“不用大面积 wash 压平 tile art”。
- P0-3 只在 `UI-demo-new` map stage 子面上收敛，不能关闭整个 UI/UX director-grade 目标。
- 剩余优先级：继续降低 cell seam 的硬格线感；补强局部 torch/player light 层次；推进右栏装备/铭刻/背包和底部 hero/action/log deck 的 hierarchy polish。

### 2026-05-25 bottom HUD hierarchy pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 bottom hero/action/log deck。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：不改 `core` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或资源 authority；本轮不新增正式资源。

实现记录：

- `DemoShellRenderer`：英雄卡背景改为更深的分层材质，并在 HP/资源条后增加暗底数值井，避免数值条像散放在纹章背景上。
- `DemoShellRenderer`：HP/资源 gauge 高度从 `14f` 提升到 `18f`，数值文本随之重排，提升首屏扫读稳定性。
- `DemoShellRenderer`：攻击/防御从松散文本改为底部 stat chip，降低和 gauge/姓名层级的混杂。
- `TileRenderModel` + `DemoShellRenderer`：从现有 `PlayerStatusSnapshot.level` 派生 `heroLevelText`，只作为 client presentation，在英雄纹章上绘制等级徽章，补足身份锚点。
- `DemoShellRenderer`：日志 deck 增加细竖向 signal rail，并把正文起点右移；保持单一连续内容面，不回退成逐行 card。
- focused tests：新增断言约束 hero card 需要暗底 gauge well、gauge 高度不退化、log deck 需要 signal rail 且不能拆成 row plates。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
```

结果：

- 第一次 RED：`TileRendererCanvasTest > render canvas keeps hud gauges clear of the title line` 和 `DemoShellRendererTest > shell keys bind only...` 分别证明缺少 hero gauge well / log signal rail。
- 实现后第一次复跑：日志 rail 断言转绿，hero gauge well 的宽度断言过度绑定桌面黄金图尺寸；已校正为最小布局也成立的视觉合同。
- 最终 focused renderer slice：`BUILD SUCCESSFUL in 5s`。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：只因 bottom/full parity hash 漂移；右栏、地图、导航、背包 crop 保持稳定。
- 人工查看 `ui-demo-new-bottom-deck-no-command-hints.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮 bottom HUD 视觉方向。
- 更新 `GoldenScreenshotHarnessTest` PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 8s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `53199ef6bcf3a19661a0cb50e9b0268c6ca9d7587c34e46f52092e4237a02149` |
| `ui-demo-new-parity-1280x800` | `7d89f80a3938f6982f9ea3f9cb67c8b7b3a06f28ac4293955313202e3609d8f1` |
| `ui-demo-new-right-panel-grid` | `f61f681cf280e5e464623f9014105b285d36adf6047d4c15948e72bf2ca7b004` |
| `ui-demo-new-bottom-deck-no-command-hints` | `3d3fe127f3784d0b93d889b3a8642dfa0544231fbaa94ba197c14c1b35768d42` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `cef659bf399160b895ddb1d08a63fd79db9f4562c2318fb5fc273ed64fea8264` |
| `ui-demo-new-inventory-page-1` | `f9e29330d519fd45024b67c08bd34a9ce654f5e03b37b1e1916b60bdd3084eb9` |
| `ui-demo-new-inventory-page-2` | `93686420c2a14d913eeb548757cea3eab84801fdc4107a8e0161ce0257285247` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 bottom deck crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png` | 英雄纹章、等级徽章、数值井、stat chip 与 log rail 形成更稳定的 bottom HUD 层级 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | bottom HUD 已明显优于上一版工程拼接感；右栏密度、action slot label/icon 层级和 map seam/光照仍需继续 polish |

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格限于 bottom HUD 和 full parity，截图方向可信，focused renderer tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是 right panel hierarchy、action slot 文本/图标权重、map seam/torch/player light 继续收敛。

### 2026-05-25 right panel hierarchy pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 right equipment/inscription/backpack panel。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：不改 `core` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或资源 authority；本轮不新增正式资源。

实现记录：

- `DemoShellRenderer`：为 right panel 的 equipment / inscription / backpack / operation sections 增加低透明分区底材和 title band，避免整栏像一个未分组的大暗面。
- `DemoShellRenderer`：装备区新增 forged rig backdrop，使用一张宽暗底、轻量中心轴和低透明横向材质线，把 9 个装备 socket 收束成角色装备面。
- `DemoShellRenderer`：铭刻双列行从通用 row plate 改为 right-panel 专用 forged row rail，空行与实装行保持同尺寸但降低空行权重。
- focused tests：新增断言约束装备区必须有 broad forged rig backdrop，铭刻行必须有稳定的 3px forged rail；先 RED 后 GREEN。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
```

结果：

- RED：`TileRendererCanvasTest > dark uiux pr02-1 draws right panel slots and hero crest scaffold` 失败，缺少 equipment forged rig backdrop。
- 实现后复跑：`BUILD SUCCESSFUL in 4s`。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-*`、`ui-demo-new-right-panel-grid` 和 `ui-demo-new-inventory-page-1/2` hash 漂移；`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-map-stage-crop` 保持稳定。
- 人工查看 `ui-demo-new-right-panel-grid.png`、`ui-demo-new-parity-1672x941.png`、`ui-demo-new-inventory-page-1.png`、`ui-demo-new-inventory-page-2.png` 后接受本轮 right panel 方向。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 8s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `214809f3378d44167de0ba6523f5b2803e3a5519ff97713073ab33bfdc1235e2` |
| `ui-demo-new-parity-1280x800` | `643c3f444f31c70460a91457f25c50fa98e1c68b46a712e9ac100ec9bb95caa8` |
| `ui-demo-new-right-panel-grid` | `ad7f284bd1b64e15a0bb180ac0c380c9fa9710b99824e2ce43158db3ce4f97a0` |
| `ui-demo-new-bottom-deck-no-command-hints` | `3d3fe127f3784d0b93d889b3a8642dfa0544231fbaa94ba197c14c1b35768d42` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `cef659bf399160b895ddb1d08a63fd79db9f4562c2318fb5fc273ed64fea8264` |
| `ui-demo-new-inventory-page-1` | `7b0df2164313bab9b62733de550323adef36434c78040e792e3c5c201af31a76` |
| `ui-demo-new-inventory-page-2` | `f16c37e1cd1c274baa3f1daf3fbc17cc3348470b759c68285198249a2b94685a` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 right panel crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png` | 装备 socket 被 forged rig 背板收束，铭刻列表 rail 更清晰；操作提示仍偏拥挤 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏右栏比上一版更像一个角色装备面板；action slot label/icon hierarchy 和 map seam/局部光照仍需继续 polish |
| PR02-1 inventory page 1/2 | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-1.png`, `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-2.png` | 右栏材质同步影响背包页，文字与 slot 未被遮挡，可作为同一 right panel rebaseline 接受 |

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围来自同一 right panel 分区材质变化，截图方向可信，focused renderer tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是 action slot label/icon hierarchy、operation hints 可读性、map seam / torch / player-light 层次。

### 2026-05-25 action deck hierarchy pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 bottom action deck。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：不改 `core` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或资源 authority；本轮不新增正式资源。

实现记录：

- `DemoShellRenderer`：action deck 外框内新增连续 dark action tray，避免技能位直接漂浮在通用框体背景上。
- `DemoShellRenderer`：每个 action slot 新增 forged socket well、顶部锻造线、轻量分隔线和图标/文字连接 rail，使图标 socket 与底部 label plate 读成同一技能卡族。
- `TileRendererCanvasTest`：在中文 hotbar 可读性测试中新增 focused 断言，约束 action deck 必须有连续 dark tray，且填充技能位必须有 forged socket well；先 RED 后 GREEN。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
```

结果：

- RED：`TileRendererCanvasTest > render canvas keeps chinese hotbar labels inside their slot cards` 失败，缺少连续 action tray。
- 实现后单测复跑：`BUILD SUCCESSFUL in 3s`。
- focused renderer slice 复跑：`BUILD SUCCESSFUL in 2s`。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800` 和 `ui-demo-new-bottom-deck-no-command-hints` hash 漂移；right panel、nav、map、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-bottom-deck-no-command-hints.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮 action deck 方向。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 8s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

最终 gate 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke --rerun-tasks`：`BUILD SUCCESSFUL in 33s`。
- `clientSmoke` 仍有既有 3 个 skipped：audio enabled formal path、render enabled tile path、audio enabled boss warning path。
- `git diff --check`：通过。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `1b8eed1b09fb533e17b98ae5ca25a9f7839073727a862b5ed0cc77491b23c76f` |
| `ui-demo-new-parity-1280x800` | `099e8dee2f689aa63f0b81c4c67c9b554026315fb22ae3df809c5d8b778c1613` |
| `ui-demo-new-right-panel-grid` | `ad7f284bd1b64e15a0bb180ac0c380c9fa9710b99824e2ce43158db3ce4f97a0` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `cef659bf399160b895ddb1d08a63fd79db9f4562c2318fb5fc273ed64fea8264` |
| `ui-demo-new-inventory-page-1` | `7b0df2164313bab9b62733de550323adef36434c78040e792e3c5c201af31a76` |
| `ui-demo-new-inventory-page-2` | `f16c37e1cd1c274baa3f1daf3fbc17cc3348470b759c68285198249a2b94685a` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 bottom deck crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png` | action deck 已从“图标 + 标签散件”推进为连续控制托盘，技能位 socket well 和连接 rail 提升了整体材质层级 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏底部 action 区比上一版更像产品化技能控制面；operation hints 密度、map seam 和 player-light 仍需继续 polish |

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格限于 action deck 所在的 bottom/full evidence，截图方向可信，focused renderer tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是 operation hints 可读性、map seam / torch / player-light 层次，以及 Phase C 全量 surface cohesion。

### 2026-05-25 operation hints compact matrix pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 right panel 底部 operation hints。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：不改 `core` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或资源 authority；本轮不新增正式资源。

实现记录：

- `DemoShellRenderer`：普通 map operation hints 不再合并成 3 条 full-width row plate，改为两列 command matrix。
- `DemoShellRenderer`：每条命令拆成 key chip + muted label；key 使用小型金色 chip，label 使用低透明 secondary text，降低右栏底部视觉权重。
- `DemoShellRenderer`：validation/shop 等带图标或 frame 的 operation rows 继续走既有 visual row path，避免把状态摘要误改成快捷键矩阵。
- focused tests：新增断言禁止 operation hints 退回 oversized full-width text rows，并要求至少 4 个 key chip；既有 shell-order 测试同步改为验证 split shortcut/label 仍保留。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest
```

结果：

- RED：`TileRendererCanvasTest > dark uiux pr02-1 draws right panel slots and hero crest scaffold` 失败，当前 operation hints 仍有 3 条 oversized full-width text rows。
- 第一次 full focused slice 失败：`DemoShellRendererTest` 仍要求旧的整行 `Ctrl+S Save` 文本；已修正为验证 `Ctrl+S` / `Save`、`5-8` / `Use rune` 分离后仍位于 operation bounds。
- 实现后 focused renderer slice 复跑：`BUILD SUCCESSFUL in 4s`。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-right-panel-grid`、`ui-demo-new-inventory-page-1/2` hash 漂移；bottom、nav、map crop 保持稳定。
- 人工查看 `ui-demo-new-right-panel-grid.png`、`ui-demo-new-parity-1672x941.png`、`ui-demo-new-inventory-page-1.png`、`ui-demo-new-inventory-page-2.png` 后接受本轮 operation hints 方向。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 8s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

最终 gate 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke --rerun-tasks`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 仍有既有 3 个 skipped：audio enabled formal path、render enabled tile path、audio enabled boss warning path。
- `git diff --check`：通过。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `90d121229a713b87da835b188cc19c95b5bd151f8072a12e649f4973e1082c85` |
| `ui-demo-new-parity-1280x800` | `d670e6361d420086b8040248b86440db139912a9f40f4f7e1010aa96c9b8e61b` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `cef659bf399160b895ddb1d08a63fd79db9f4562c2318fb5fc273ed64fea8264` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 right panel crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png` | operation hints 从大白字行降为 key chip + muted label 的两列工具条，右栏底部不再抢装备/铭刻层级 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏右栏底部视觉权重下降，整体更接近参考图的紧凑操作提示；map seam / player-light 仍是下一轮重点 |
| PR02-1 inventory page 1/2 | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-1.png`, `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-2.png` | 同一 right panel operation section 在背包页保持可读，未遮挡背包分页和 slot |

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 right panel operation hints，截图方向可信，focused renderer tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是 map seam / torch / player-light 层次，以及 Phase C 全量 surface cohesion。

### 2026-05-25 map seam / torch / player-light pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的 seam、torch 和 player-light 层次。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` 渲染表现和 focused test；不改 `core` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- `TileRenderer`：visible room seam softener 从细弱 3px 线提升为 5px 低透明 stone mortar band，并补更轻的高光线，降低 debug grid 读感。
- `TileRenderer`：玩家光照从大范围方形泛光收紧为多层 compact lantern pool，并保留 30px 局部高亮核心，强化地图焦点。
- `TileRenderer`：火把在 tile glow 之外增加 compact warm pool，且玩家/火把强光限定在 visible material 范围/包围盒内，避免照亮暗区形成大矩形光块。
- focused tests：新增 centered room 用例，约束玩家 compact light pool、火把 local pool 和玩家核心亮点；同步提升 mortar band 断言。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：`render canvas adds authored dungeon scars and stains to visible floors` 失败，当前 seam softener 仍是旧 3px/0.026；`render canvas gives player and torch compact local light pools` 失败，当前实现没有 compact local pool。
- 实现后复跑：`BUILD SUCCESSFUL in 20s`。
- 追加 visible material clip 后复跑：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后发现初版光池会照亮暗区形成矩形块；已收紧为 visible material clip 后重新生成。
- 更新 PR02-1 focused expected hash 后复跑：通过。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f463fa94a24e2dc07f93478218bd26fbe7388f52ca416cee5dbe95d7381b5958` |
| `ui-demo-new-parity-1280x800` | `f26319ee16f737fd83a3c2d966c316a3eb5581cdd6d543b86e5ebcdc92e68232` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `f2ff75b8dc2d74a4aab3d0872ea7a791c5d132ac8c13c05bd53460e7e7a48670` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 火把和玩家光源更有局部层次，暗区不再被新光池大面积照亮；主房间 seam 仍可见但不再只有硬黑线 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏地图焦点更明确，右栏/底栏不受本轮影响；距离参考图的手工石板、墙体体积和暗角压迫感仍有明显差距 |

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage 光照/seam，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是地牢房间轮廓、墙体体积、暗区网格压制，以及 Phase C 全量 surface cohesion。

### 2026-05-25 room silhouette / hidden-grid suppression pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的地牢房间轮廓、墙体体积和暗区网格压制。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` 渲染表现和 focused test；不改 `core` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- `TileRenderer`：新增 visible room outer shadow pass，复用 `mapCellMaterials` 和 `visibleMaterialPoints` 判断可见材质边界。
- `TileRenderer`：当可见 tile 的上下左右邻居不是 visible material 时，在 hidden stage 方向绘制 clipped soft shadow 与低透明暖边，压低暗区规则网格并强化墙外侧体积。
- focused tests：新增 hidden stage 包围的 5x5 可见房间用例，要求顶部墙和侧墙都向 hidden tiles 投射软阴影。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：`render canvas casts depth shadows from visible room into hidden stage` 失败，当前可见墙外侧没有 soft shadow。
- 实现后复跑：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮 silhouette 方向：可见房间边缘有更明确压暗轮廓，暗区网格被边缘阴影压住一部分，没有把房间内部压糊。
- 更新 PR02-1 focused expected hash 后复跑：通过。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `e1303cb222709b4e45296d853a9a6f0874d60fc086a16bd6e53c2bd1f41b6d57` |
| `ui-demo-new-parity-1280x800` | `40597ff3135af856cdb6561230af1204c3c65a7a0f590c6dbbfd6f5619b58a3b` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `833f1379f4c2a72a088851b9e11341a6f9e017e6a1368dab3b4b33ecc07c38c9` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 可见房间外沿压暗比上一版更稳定，暗区网格不再直接贴着墙面抢读；主房间内部仍有明显 tile grid |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏地图轮廓更稳，右栏/底栏不受本轮影响；距离参考图的墙体厚度、非矩形洞口和手工石板节奏仍有差距 |

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage room silhouette，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是主房间内部 grid 弱化、墙体厚度/洞口形状和 Phase C 全量 surface cohesion。

### 2026-05-25 main room cross-cell slab / grid dissolve pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的主房间内部 grid 弱化与手工石板节奏。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` 渲染表现、focused test 和 PR02-1 golden hash；不改 `core` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- `TileRenderer`：在 visible room atmosphere 内新增 wider low-alpha stone grout pass，覆盖内部黑色 cell joints，让接缝先读成石材 grout。
- `TileRenderer`：新增 cross-cell slab fields 与 seam-break overlay，用更大的石板面和横向断裂线打断单格重复。
- focused tests：在 large visible room 用例中新增 stone grout、director-scale cross-cell slab、cross-cell seam-break 三个断言，约束“不是只靠每格 tile texture 和细网格线”。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.render\ canvas\ adds\ authored\ dungeon\ scars\ and\ stains\ to\ visible\ floors --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED 1：新增 cross-cell slab 断言先失败，当前实现缺少足够大的跨格石板面。
- RED 2：新增 stone grout 断言先失败，当前实现缺少更宽的低透明接缝覆盖层。
- 实现后复跑 focused single test：通过。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：主房间内部黑色网格线被低透明 grout 与跨格 slab 压低，石板面层级更连续，没有影响右栏、底栏或库存裁剪。
- 更新 PR02-1 focused expected hash 后复跑：通过。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `14797a9f10d716b3d7ad72ab53800e07cdfc49b357ff1eff7648281647345e3a` |
| `ui-demo-new-parity-1280x800` | `8a6fce5db02be7a30d75493748dae3209a39027bd55b6c96ae0995fc895db347` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `53a4308bff0b0a18434b8f6cc74d8ad6f6bb6a7501a6107ebe87bb4bed7b293d` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 主房间内部接缝更像低透明石材 grout，跨格 slab 让地面从单格棋盘推进到更连续的石板层级；黑线仍可见但不再是唯一第一读数 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏地图材质层级更稳，右栏/底栏不受本轮影响；距离参考图的手工破形、墙体厚度、洞口形状和全量 surface cohesion 仍有差距 |

owner gate 与 diff hygiene：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 33s`。
- `clientSmoke` 仍为既有 3 个 skipped：`client smoke covers audio enabled formal path`、`client smoke covers render enabled tile path`、`client smoke covers audio enabled boss warning path`。
- `git diff --check`：通过，无 whitespace error。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility、tools 侧既有 Json/Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage 内部材质层级，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是墙体厚度/洞口破形、暗区背景进一步弱化和 Phase C 全量 surface cohesion。

### 2026-05-25 visible wall masonry mass pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的墙体厚度、边界 material 和房间体块读数。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` 渲染表现、focused test 和 PR02-1 golden hash；不改 `core` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- `TileRenderer`：新增 visible wall masonry mass pass，复用 `mapCellMaterials` 与 visible floor adjacency，将相邻 visible wall tile 的 floor-facing 边按连续 run 合并绘制。
- `TileRenderer`：水平/垂直 wall run 增加 dark masonry band、暖色 worn edge 和内侧压暗层，让墙边从 per-cell relief 推进到连续厚墙读数。
- focused tests：新增 7x6 可见房间用例，要求相邻墙体产生连续 horizontal masonry mass 与 vertical wall thickness。
- 实现时发现 viewport Y 轴为屏幕坐标方向；垂直 run 改为用 `min/max` 计算 screen bounds，避免负高度。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.render\ canvas\ joins\ adjacent\ visible\ walls\ into\ continuous\ masonry\ mass --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 continuous masonry mass 断言先失败，当前实现缺少跨 tile 的墙体厚度带。
- GREEN 前中间失败：水平 band 通过后，垂直 band 因 viewport Y 方向导致负高度未通过；修正 screen bounds 后复跑新测试通过。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：墙边界更像连续厚墙，底边和侧边不再完全由单格 wall tile 支配；没有把房间内部压黑。
- 更新 PR02-1 focused expected hash 后复跑：通过。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `be13b1e67a7edcc35c58eb266cd0568c4d85278415fcf26841bef29c0510edd1` |
| `ui-demo-new-parity-1280x800` | `fa7653f64fbfa2db382e262f77b58432a4cab01f8c87be530a3db1296425cc2d` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `8a2d925123f5b17db166107241fb1dd55f4e17cb9e9cf3a668231706bc7061fb` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 墙边界更厚、更连续，底边和侧边有更明确 masonry mass；房间内部仍保留可读 tile/actor/loot，不被新厚墙压糊 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 的边界体块更稳定，right/bottom/nav/inventory evidence 不受本轮影响；距离参考图的非矩形洞口、暗区背景压制和全量 surface cohesion 仍有差距 |

owner gate 与 diff hygiene：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 仍为既有 3 个 skipped：`client smoke covers audio enabled formal path`、`client smoke covers render enabled tile path`、`client smoke covers audio enabled boss warning path`。
- `git diff --check`：通过，无 whitespace error。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility、tools 侧既有 Json/Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage wall mass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是非矩形洞口/过渡门槛、暗区背景进一步弱化和 Phase C 全量 surface cohesion。

### 2026-05-25 narrow passage threshold pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的非矩形洞口、通道转折和房间轮廓破形。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` warm overlay 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused tests：新增“房间 + 单格走廊”用例，要求通道口生成连续 dark side jamb 和 compact worn threshold cap。
- `TileRenderer`：新增 visible passage threshold pass，识别两侧被墙/暗区夹住且沿通道方向仍有 visible floor 延续的 floor tile。
- `TileRenderer`：vertical / horizontal narrow passage 分别补连续 dark jamb、低透明暖色内侧磨损线和门槛 cap，让门洞从硬切矩形边推进到 authored dungeon threshold。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.render\ canvas\ frames\ narrow\ visible\ passages\ as\ authored\ dungeon\ thresholds --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 passage threshold 断言先失败，当前 renderer 缺少连续 side jamb。
- GREEN：补 visible passage threshold pass 后，新 focused test 通过。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：左侧通道口出现连续门槛和侧向阴影，漂移限定在 map stage；没有影响右栏、底栏、导航和背包 evidence。
- 更新 PR02-1 focused expected hash 后复跑：通过。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `1091e9cc69714f8ae14d35ad2b82038fd13a2431d9754a10f289a981fb061b08` |
| `ui-demo-new-parity-1280x800` | `797427a222c2265ebeafdc49b772f27074b9bf2e5399f806c536ad33623c4055` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `a5cec5d2a9bab89075949cbc6d6a7105c784f8311727555844f4adfac324e59e` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 左侧通道口有更明确的 dark jamb 和 worn threshold cap，房间外形不再只是完整矩形；主体 tile、actor、loot 和火光仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | map stage 过渡口更接近参考图的 dungeon opening 读数；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate 与 diff hygiene：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 35s`。
- `clientSmoke` 仍为既有 3 个 skipped：`client smoke covers audio enabled formal path`、`client smoke covers render enabled tile path`、`client smoke covers audio enabled boss warning path`。
- `git diff --check`：通过，无 whitespace error。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility、tools 侧既有 Json/Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage passage threshold，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是暗区背景进一步弱化、墙角/洞口破形细节和 Phase C 全量 surface cohesion。

### 2026-05-25 hidden stage grid suppression pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的暗区背景压迫感和房间轮廓主次。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` warm overlay 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused tests：新增小可见房间 + 大面积 hidden stage 用例，要求 visible room 外侧出现宽幅 dark veil，且不能覆盖房间焦点。
- `TileRenderer`：在 `drawMapStageStoneTexture` / `drawMapStageShadowVeil` 后、visible room 绘制前新增 hidden stage grid suppression pass。
- `TileRenderer`：复用 `visibleClip` 和 viewport `mapBounds`，只对可见房间外侧绘制 clipped dark veil 与少量不规则暗面，避免把 visible room 本体压黑。
- 增长理由：本轮只新增表现层局部绘制和对应测试，不新增配置、资源、schema 或 helper；复杂度增长用于锁住暗区背景不抢读的 director-grade 视觉合同。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.render\ canvas\ suppresses\ broad\ hidden\ stage\ grid\ outside\ visible\ room --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 hidden stage grid suppression 断言先失败，当前实现没有宽幅暗幕压低隐藏舞台背景。
- GREEN：补 hidden stage grid suppression pass 后，新 focused test 通过。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png`、`ui-demo-new-parity-1672x941.png` 与 `UI/UI-demo-new.png` 后接受本轮方向：暗区大块背景网格退后，中央房间和左侧通道仍可读。
- 更新 PR02-1 focused expected hash 后复跑：通过。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `efe2fbc7e2fa92f8ec130ac0786a522d62c667c56204a36b5ebf3b149ae72b94` |
| `ui-demo-new-parity-1280x800` | `f918933d172947149618151d82b07829221a00770c5590d330775d452116be92` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `ed72e9698a8efaff705286b291d6a8fba7261a7d8ce22403deb070d1b79140c8` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 房间外大块背景网格更弱，暗区更像包裹房间的黑暗空间；visible room、左侧通道、actor、loot 和火光未被压糊 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 的暗区背景更退后，右栏、底栏、导航和背包 crop 未受本轮影响；距离参考图的墙角破形和手工石板节奏仍有差距 |

owner gate 与 diff hygiene：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 仍为既有 3 个 skipped：`client smoke covers audio enabled formal path`、`client smoke covers render enabled tile path`、`client smoke covers audio enabled boss warning path`。
- `git diff --check`：通过，无 whitespace error。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility、tools 侧既有 Json/Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage hidden stage suppression，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是墙角/洞口破形细节、主房间石板节奏和 Phase C 全量 surface cohesion。

### 2026-05-25 visible room corner breakup pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的矩形房间外轮廓和洞口破形。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` warm overlay 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused tests：新增 10x8 全可见房间用例，要求屏幕左下和右上 visible room corner 出现 dark masonry chip 与小尺寸 worn stone lip。
- `TileRenderer`：在 visible passage threshold 后新增 visible room corner breakup pass，复用 `visibleRoomClip(frame)` 和 viewport `cellSize`，对四角绘制 clipped dark block 和低透明石材磨损线。
- 调整记录：首次 golden 观察到 warm lip 过亮，容易读成漂浮浅色矩形；随后降低 lip 尺寸与 alpha，再复跑 focused renderer test 和 PR02-1 golden。
- 增长理由：本轮只新增表现层局部绘制和对应测试，不新增配置、资源、schema 或 second authority；复杂度增长用于锁住“可见房间不是完整矩形块”的 director-grade 视觉合同。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.render\ canvas\ chips\ visible\ room\ corners\ into\ broken\ dungeon\ silhouette --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 corner breakup 断言先失败，当前 renderer 缺少 corner chip。
- GREEN：补 visible room corner breakup pass 后 focused test 通过；测试锚点随后按屏幕 y 轴方向修正为 `Point(0, 7)` / `Point(9, 0)`。
- 调暗 lip 后复跑 focused test：通过。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看首次 map crop 后拒绝直接接受：stone lip 过亮，四角出现不自然浅色短块。
- 调暗 lip 后重新查看 `ui-demo-new-map-stage-crop.png`、`ui-demo-new-parity-1672x941.png`：墙体四角被压暗，左侧洞口轮廓更破碎，之前显眼的浅色矩形消失；右栏、底栏、导航和背包未受影响。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 8s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `4f3460f847517f57073c14f1d5d052e5dd83d08d6c79f8df6d95ab9f295013cb` |
| `ui-demo-new-parity-1280x800` | `8975cb70ad863b91f32126b7a752977eaf7e30ac0b2e85de378013b101a7468b` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `8306bfd69e79d2543cb13a1f2d20df03d9f168276947bc250f79ce7f44a0dd6c` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 四角暗部破形让房间轮廓更接近 dungeon opening，而不是规则矩形叠暗区；actor、loot、火光和 floor tile 仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 的轮廓质感更稳；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate 与 diff hygiene：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 仍为既有 3 个 skipped：`client smoke covers audio enabled formal path`、`client smoke covers render enabled tile path`、`client smoke covers audio enabled boss warning path`。
- `git diff --check`：通过，无 whitespace error。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage corner breakup，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是主房间石板节奏、墙面高差和 Phase C 全量 surface cohesion。

### 2026-05-25 staggered stone slab rhythm pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的主房间石板节奏和地面质感。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` warm overlay 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused tests：新增 12x8 全可见房间用例，要求主房间 floor 出现宽幅 staggered stone slab，并出现短暗 mortar cut 来中断长格线。
- `TileRenderer`：在 `drawVisibleRoomGridDissolve` 后、cross-cell slab fields 前新增 `drawVisibleRoomStaggeredStoneRhythm`，复用 visible room bounds 与 `cellSize`。
- 调整记录：首版错缝石板过于克制，人工查看后在同一 pass 内略提高 slab alpha，并补两条低透明横向磨损/暗切线；没有新增资源、配置、schema 或第二真源。
- 增长理由：本轮只新增表现层局部绘制和对应测试，不新增业务 owner 或 helper；复杂度增长用于锁住“主房间不是均匀单格棋盘”的 director-grade 视觉合同。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.render\ canvas\ lays\ staggered\ stone\ slabs\ across\ visible\ room\ floor --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 staggered slab 断言先失败，当前 renderer 缺少宽幅错缝 stone slab。
- GREEN：补 `drawVisibleRoomStaggeredStoneRhythm` 后 focused test 通过。
- 调强 stone slab alpha 与横向磨损/暗切线后复跑 focused test：通过。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看首版 map crop 后接受方向但判断过于克制；调强后重新查看 `ui-demo-new-map-stage-crop.png`，主房间中部有更明确的错缝石板和局部断线，没有明显大色块或遮挡 actor / loot / torch。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 8s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `2886f889f9ede6a4478a4e0911ad98adbbd984224f7deb0da8565f5fdc74b73f` |
| `ui-demo-new-parity-1280x800` | `ce1dc3a9f8573dfd6deb1557f84530539a6fe7061c108692d3d614fced10a238` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `4fbfde32b9ae4c1f0ddcf52734e914e5ec4f72da55839cc771756601ee9b7b0e` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 主房间地面有更明确的错缝 stone slab 和短暗 mortar cut，黑色 grid 不再是唯一地面节奏；actor、loot、火光和墙体仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 的地面材质节奏略强于上一版；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate 与 diff hygiene：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 仍为既有 3 个 skipped：`client smoke covers audio enabled formal path`、`client smoke covers render enabled tile path`、`client smoke covers audio enabled boss warning path`。
- `git diff --check`：通过，无 whitespace error。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage floor slab rhythm，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是墙面高差、地砖细节密度和 Phase C 全量 surface cohesion。

### 2026-05-25 raised wall crown / interior face pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的墙面高差和房间边界体积。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` warm overlay 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused tests：新增 12x8 全可见房间用例，要求长 visible wall run 出现连续 raised crown 和 warm worn interior face。
- `TileRenderer`：在 `drawVisibleWallMassBands` 后新增 `drawVisibleWallRaisedFaces`，按完整 visible wall run 绘制墙冠，但仍用 floor adjacency 判断是否需要落笔。
- 调整记录：首轮 GREEN 前发现 run 只基于直接贴地面的墙块，会把长墙两端角块切掉；随后仅调整新 raised-face pass 的 run 来源为全部 visible wall，保留既有 mass band 的 adjacency 语义。
- 增长理由：本轮只新增表现层局部绘制和对应测试，不新增资源、配置、schema 或 helper；复杂度增长用于锁住“墙不是单层 tile 边”的 director-grade 视觉合同。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.render\ canvas\ raises\ visible\ wall\ runs\ with\ crown\ and\ interior\ face --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 raised wall 断言先失败，当前 renderer 缺少连续 raised crown。
- GREEN：补 `drawVisibleWallRaisedFaces` 并修正 run 来源后 focused test 通过。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：墙边连续感和高差更明显，底部火光、主角、loot、右栏和底栏未被压糊。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 8s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `9ca2cd09d31969c82b6fb0826b35e46129622416041a1c5435e9efe59a2eb477` |
| `ui-demo-new-parity-1280x800` | `6f42d147336930cdf1c4f6213c91c5a924f31ec37a702c4bffc800d294f60ee7` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `7e3eda7c48ef83ae9407cf711103f45bbc05a8c1f1c1ae3f845413c3e9963b18` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 顶/底/侧墙出现更连续的压暗和暖色磨损边，墙体更像有高度的 masonry boundary；actor、loot、火光和 floor tile 仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 墙体厚度比上一版更稳；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate 与 diff hygiene：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 仍为既有 3 个 skipped：`client smoke covers audio enabled formal path`、`client smoke covers render enabled tile path`、`client smoke covers audio enabled boss warning path`。
- `git diff --check`：通过，无 whitespace error。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage wall raised-face pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是地砖细节密度、局部污迹/破损和 Phase C 全量 surface cohesion。

### 2026-05-25 localized floor grime / broken stone detail pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的地砖细节密度与局部污迹/破损。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` warm overlay 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused tests：新增 12x8 全可见房间用例，要求 visible room floor 出现可定位的 dark grime / chipped stone scar，并出现小尺寸 worn highlight。
- `TileRenderer`：在 `drawVisibleRoomCrossCellSlabFields` 后新增 `drawVisibleRoomLocalizedStoneDamage`，补少量局部暗色破损、暖色 chipped edge、旧血/苔痕细节。
- 增长理由：本轮只新增表现层局部绘制和对应测试，不新增资源、配置、schema、helper 或 second authority；复杂度增长用于锁住“地面不只是均匀 overlay 和硬 grid”的 director-grade 视觉合同。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas adds localized grime and broken stone detail to visible room floor' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 localized grime 断言先失败，当前 renderer 缺少可定位的局部暗色破损。
- GREEN：补 `drawVisibleRoomLocalizedStoneDamage` 后 focused test 通过，`BUILD SUCCESSFUL in 19s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 执行纠偏：PR02-1 golden owner 是 `:client:goldenScreenshot`；普通 `:client:test` 会排除 `goldenScreenshot` tag，不能用于该 hash owner gate。
- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：主房间地面有更明确的局部 grime、stone scar、worn edge 和少量色彩污损；actor、loot、火光、右栏和底栏未被压糊。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 26s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `965a1acfb58ece82e068148fe2b8fa4d05c4baf8c120fe9ec2f22e9c8874c4c7` |
| `ui-demo-new-parity-1280x800` | `b40fb4cf5107ab428529b2f78f141ca5934f31804b9e4922db54ddfde41506af` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `e40fad41264f72a4709d6cad817ca1cca8c1cbe3a39e7224bdccee62671ddb24` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 主房间地面有更明确的局部 grime、stone scar、worn edge 和少量色彩污损；actor、loot、火光和 floor tile 仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 的地面材质密度比上一版更稳；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate 与 diff hygiene：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 33s`。
- `clientSmoke` 仍为既有 3 个 skipped：`client smoke covers audio enabled formal path`、`client smoke covers render enabled tile path`、`client smoke covers audio enabled boss warning path`。
- `git diff --check`：通过，无 whitespace error。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage localized floor grime / broken stone detail pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是更高密度手绘石砖、墙角破形和 Phase C 全量 surface cohesion。

### 2026-05-25 wall-foot rubble / contact debris pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的墙脚脏边、碎石接触和边界破形。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` warm overlay 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused tests：新增 12x8 全可见房间用例，要求 long visible wall contact 出现局部 dark rubble cluster，并要求侧墙脚出现小尺寸 warm stone chip。
- `TileRenderer`：在 `drawVisibleWallRaisedFaces` 后新增 `drawVisibleWallFootRubble`，复用 visible wall/floor adjacency 与 existing wall run 逻辑，只绘制局部碎石、脏边和暖色石片。
- 调整记录：首轮 GREEN 前发现 long wall run 会因 corner wall adjacency 过滤而从有效 run 起点开始，随后把 cluster offset 改为按有效 run 起点定位，避免测试锚点落空。
- 增长理由：本轮只新增表现层局部绘制和对应测试，不新增资源、配置、schema、helper 或 second authority；复杂度增长用于锁住“墙脚不是干净直线”的 director-grade 视觉合同。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas scatters rubble along visible wall floor contacts' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 wall-foot rubble 断言先失败，当前 renderer 缺少沿长墙接触线的局部 rubble cluster。
- GREEN：补 `drawVisibleWallFootRubble` 并修正 run offset 后 focused test 通过，`BUILD SUCCESSFUL in 19s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：上下墙脚、左侧入口边缘和侧墙接触线有更局部的 rubble/debris；actor、loot、火光、右栏和底栏未被压糊。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 26s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `d07e2378a6ead05ec068b1d0a997b4ffe4d1725f369258ebb569e1f47db2a5d3` |
| `ui-demo-new-parity-1280x800` | `4d17e647e1bab217a142a7e30687a3531c3b747ac9aefae28e5df15def4f8494` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `6993476623977f7aed87941db6fdf626b6889e89da6a01b7fcb7fb1d820102f0` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 上下墙脚、左侧入口边缘和侧墙接触线有更局部的 rubble/debris，墙地接触不再完全依赖干净直线；actor、loot、火光和 floor tile 仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 的墙脚边界更脏、更局部；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate 与 diff hygiene：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 43s`。
- `git diff --check`：通过，无 whitespace error。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage wall-foot rubble / contact debris pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是主房间 grid 继续弱化、更高密度手绘石砖和 Phase C 全量 surface cohesion。

### 2026-05-25 broken mortar cap / seam bridge pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的主房间内部 grid 弱化与手工石材质感。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 warm overlay 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused tests：新增 12x8 全可见房间用例，要求内部 vertical seam 出现 stone-colored bridge，并要求 horizontal seam 出现 chipped mortar cap。
- `TileRenderer`：在 `drawVisibleRoomGridDissolve` 后新增 `drawVisibleRoomBrokenMortarCaps`，复用 visible floor adjacency，只在少量稳定坐标节奏的内部接缝上绘制 stone bridge、暗色缺口和暖色磨损。
- 增长理由：本轮只新增表现层局部绘制和对应测试，不新增资源、配置、schema、helper 或 second authority；复杂度增长用于锁住“主房间 grid 不再完全等宽等距”的 director-grade 视觉合同。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks uniform room grid joints with chipped mortar caps' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 broken mortar cap 断言先失败，当前 renderer 缺少可定位的内部 seam bridge。
- GREEN：补 `drawVisibleRoomBrokenMortarCaps` 后 focused test 通过，`BUILD SUCCESSFUL in 23s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 23s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：主房间内部 seam 有更多断续、磨损和石材桥接；actor、loot、火光、右栏和底栏未被压糊。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 31s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `78eb3a280511deaf396031a708e56dd0644648e97685881b42bf45738a77d119` |
| `ui-demo-new-parity-1280x800` | `82134713f0ed0ef526ec66886497165632e11d27892c90ab4ec1789edbecd164` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `3a42ab24f683a6c2b459bb3ed5d65db47838a5101ac095f687b93316bfa7b236` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 主房间内部 seam 有更多断续、磨损和 stone bridge，规则 grid 读数比上一版更不机械；actor、loot、火光和 floor tile 仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 的接缝细节更碎、更局部；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 43s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage broken mortar cap / seam bridge pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是进一步降低主房间黑色 grid 支配权、强化墙体高差和推进 Phase C 全量 surface cohesion。

### 2026-05-25 visible wall crown cap-block pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的墙体高差、墙冠块状节奏和边界 material。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 warm overlay 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused tests：新增 12x8 全可见房间用例，要求长墙 crown 出现离散 cap-stone block，并要求侧墙出现小型 masonry notch。
- `TileRenderer`：在 `drawVisibleWallRaisedFaces` 后新增 `drawVisibleWallCrownBlocks`，复用 visible wall/floor adjacency，只在局部墙冠与侧墙位置绘制 block/notch。
- 增长理由：本轮只新增表现层局部绘制和对应测试，不新增资源、配置、schema、helper 或 second authority；复杂度增长用于锁住“墙体不是连续透明矩形带”的 director-grade 视觉合同。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas articulates visible wall crowns with uneven cap blocks' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --rerun-tasks
```

结果：

- RED：新增 wall crown cap-block 断言先失败，当前 renderer 缺少可定位的离散墙冠块。
- GREEN：补 `drawVisibleWallCrownBlocks` 后 focused test 通过，`BUILD SUCCESSFUL in 24s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 25s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：墙顶和侧墙出现更局部的块状亮暗变化；actor、loot、火光、右栏和底栏未被压糊。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 33s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `cd96afb472d17fbf798fabc64d4ae72d5d2c31c40b3ca57d9a2a5e9d6d58d53a` |
| `ui-demo-new-parity-1280x800` | `267ac537b6efe75a21caa5a5cfc42fdf570f1a7c3513347e7521bc29580708b2` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `674b5cf650a94493594e93450015b12f51df807d5b71561883245c77f9afb1de` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 墙顶和侧墙出现更局部的 cap-stone / notch 变化，长墙不再完全依赖连续半透明带；actor、loot、火光和 floor tile 仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | 首屏 map stage 的墙冠细节比上一版更局部；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 41s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 map stage visible wall crown cap-block pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是继续提高墙体砖块密度、洞口破形和 Phase C 全量 surface cohesion。

### 2026-05-25 corridor mouth broken lintel pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续收敛 `UI-demo-new` 首屏 map stage 的房间口、走廊 throat 与墙体厚度读数。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 visible passage threshold 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：新增 7x7 房间连单格走廊用例，要求 corridor mouth 生成宽于单 tile 的 broken lintel stone，并带暖色 worn lip。
- `TileRenderer`：在 `drawVisiblePassageThresholds` 内基于 visible floor adjacency 判定 `wideNorth/wideSouth/wideWest/wideEast`，只在通道接入宽房间时叠加更宽的暗石门楣、暖色 lip 和小破口。
- 增长理由：本轮没有新增资源、配置、schema、helper 或 second authority；复杂度增长只用于把“房间口是 carved dungeon architecture，不是干净矩形开口”的视觉合同锁进测试。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas caps corridor mouths with broken lintel stones' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 corridor mouth lintel 断言先失败，当前 renderer 缺少宽于单 tile 的 broken lintel stone；失败点是 `TileRendererCanvasTest.kt` 的第一条 lintel assertion。
- GREEN：补 passage mouth lintel 后 focused test 通过，`BUILD SUCCESSFUL in 27s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 25s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次 targeted 运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：左侧房间口、下方走廊口和可见 throat 增加更宽的暗石门楣与暖色磨损 lip，入口读数更像被墙体包住的地牢开口；actor、loot、torch、右栏与底栏未被压糊。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 31s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `57a3764afa8bbe3a9fc385d68c12e1013eedf10b6b182f01c5bdef4295aefa61` |
| `ui-demo-new-parity-1280x800` | `884bf650de21c84f9cd1402758a990a8ed70ddc534f1a51ac892c7d8d0a883b3` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `16d3b3442f0234ef67fd55ff79223db2d5ce3c9be249832c522ba3a94f9a25a5` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | corridor mouth 与 room opening 出现更宽的暗石门楣和暖色 lip，入口轮廓更像 carved wall aperture；主房间 grid 仍可见，后续仍要继续压低规则格线第一读数 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | map stage 的通道入口更有墙体厚度；右栏、底栏、导航和背包 crop 未受本轮影响 |

exploratory golden scope check：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest' --rerun-tasks
```

结果：

- 该全类检查进入真实测试体并失败：`24 tests completed, 13 failed`，失败集中在历史 golden / Phase 4 golden / PR01-1 / PR02 / PR03 / route / outcome / sample-pack / boss warning 等截图 hash。
- 本轮不把这些全局截图全部 rebaseline；它们来自公共 map renderer 的累计 UI 改造漂移，需要后续作为独立 owner-suite rebaseline/review 工作处理，避免把 PR02-1 corridor mouth slice 扩大成不可审阅的大范围 golden 接收。
- 另外两次早期 `:client:test --tests ...GoldenScreenshotHarnessTest...` 过滤尝试失败为 `No tests found`，原因是 golden screenshot 测试带 `goldenScreenshot` tag，正确入口是 `:client:goldenScreenshot`。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 46s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 corridor mouth broken lintel pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级是处理 full golden owner-suite rebaseline 策略、进一步降低主房间 grid 支配权、强化洞口破形和推进 Phase C 全量 surface cohesion。

### 2026-05-25 asymmetric room-center veil pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续压低 `UI-demo-new` 首屏 map stage 主房间中心的规则 grid 第一读数。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 visible room atmosphere 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：新增 12x8 全可见房间用例，要求 room center 出现宽的 asymmetric stone veil、thin worn lip 和 off-grid shadow terrace。
- `TileRenderer`：在既有 `drawVisibleRoomGridDissolve` 中 inline 补跨格低透明石面、磨损线和非格点暗部 terrace；不新增 helper、资源、schema 或 second authority。
- 增长理由：本轮只新增局部表现笔触和对应测试；复杂度增长用于锁住“主房间中心不能继续由等距黑色 lattice 主导”的 director-grade 视觉合同。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas overlays asymmetric room center veils to suppress grid rhythm' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 room-center veil 断言先失败，当前 renderer 缺少宽的非对称中心石面 veil；失败点是 `TileRendererCanvasTest.kt` 的第一条 veil assertion。
- GREEN：补 `drawVisibleRoomGridDissolve` 的跨格石面和 shadow terrace 后 focused test 通过，`BUILD SUCCESSFUL in 24s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 25s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次 targeted 运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：中心区域的石面 veil 是低透明跨格层，没有压掉主角、loot、火把或选中态，漂移仍限定在 map stage。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 31s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `79afe611b5a786ffad1b4334c58db56a0b7e113749556801bba9f44718451969` |
| `ui-demo-new-parity-1280x800` | `7936a57d5d4fb5bed22337f066bec0d6e0f39a68da97a5c76407a4132c1c8798` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `1f8fa9c2dd35aeb6882c949ad07b8b27f6d4f13ab999a35efe64bfd157905ab1` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 主房间中心新增低透明跨格石面和 off-grid shadow terrace，内部 grid 的第一读数略降；actor、loot、torch、目标框仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage，右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 45s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 asymmetric room-center veil pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是 full golden owner-suite rebaseline 策略、暗区整体压迫感、墙面颗粒和 Phase C 全量 surface cohesion。

### 2026-05-25 focal warm stone dropout pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续处理 `UI-demo-new` 首屏 map stage 中 player / torch 光池叠在规则 grid 上的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 warm-light 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：在既有 player/torch compact local light 用例中补断言，要求 player 光池有 warm stone veil、短 worn lip，torch 光池也有局部 grid dropout。
- `TileRenderer`：新增局部 `drawFocalWarmStoneDropout`，由 `drawPlayerWarmLight` 与 `drawTorchWarmPool` 调用；只绘制低透明暖石面、短磨损线和小暗边，不新增资源、配置、schema 或 second authority。
- 增长理由：本轮复杂度只用于把“焦点暖光必须嵌入石面并打断局部 lattice”的 director-grade 视觉合同锁进测试。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas gives player and torch compact local light pools' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 focal warm stone veil 断言先失败，当前 renderer 缺少 player 光池下的局部暖石面 dropout；失败点是 `TileRendererCanvasTest.kt` 的 player focal light assertion。
- GREEN：补 `drawFocalWarmStoneDropout` 后 focused test 通过，`BUILD SUCCESSFUL in 23s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 22s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次 targeted 运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：主角、近墙火把和下方火把周围有更连续的暖石面，grid 没有被整体洗平，actor、loot、目标框和右底栏仍可读。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 29s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `3eb4154cfb58ec8921584a59e5ad603d1d1bfc906caa2972bc6c8a53a646374a` |
| `ui-demo-new-parity-1280x800` | `e99c6839888ac6bfd781638b349646b38faf881c25fb9300a481a56dff58cfe9` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `82e0f372ffd60ea61838966df89c945a893502e259f173190bf53603cb6fe8f5` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | player / torch 周围新增低透明暖石面和短 worn lip，局部 grid seam 有退后趋势；主房间整体 grid 仍可见 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 42s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 focal warm stone dropout pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是暗区整体压迫感、墙砖颗粒、手工地砖密度和 Phase C 全量 surface cohesion。

### 2026-05-25 visible wall masonry course pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续提高 `UI-demo-new` 首屏 map stage 的 visible wall 砖块颗粒和墙面手工感。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 visible wall 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：新增 12x8 visible room 用例，要求上墙面出现小块 horizontal masonry course，右侧墙面出现 vertical course stone。
- `TileRenderer`：新增 `drawVisibleWallMasonryCourses`，挂在 existing visible wall overlay pipeline 的 `drawVisibleWallCrownBlocks` 后；只基于 visible wall/floor adjacency 绘制小块砖面和短暗缝。
- 增长理由：本轮复杂度只用于锁住“长墙不能只读成半透明整带，必须有可定位的手工砖块颗粒”的 director-grade 视觉合同；不新增资源、配置、schema 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas lays granular masonry courses along visible wall faces' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 wall-course 断言先失败，当前 renderer 缺少 visible wall face 的小块 masonry course；失败点是 `TileRendererCanvasTest.kt` 的第一条 wall-course assertion。
- GREEN：补 `drawVisibleWallMasonryCourses` 后 focused test 通过，`BUILD SUCCESSFUL in 21s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 21s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次 targeted 运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：墙体边缘出现更局部的小砖块变化，长墙不再只靠大块阴影带，主角、火把、loot、目标框和右底栏未受影响。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 27s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `b6d2a67269cd05cf8d8f4fe9ccd90632785e20ef232b322bf6bd7e4d159d6446` |
| `ui-demo-new-parity-1280x800` | `6a047d4eebf8f10a232a47233f025a9e19c017bd02911137ddffa8b699bf0d38` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `06db652efe45f013bb6a86a6c830ca3141db8cc5d0addbb7e3f67ced67bc8bea` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | visible wall 边缘新增小块 course stones 和短暗缝，墙体颗粒密度略高；actor、loot、torch、目标框仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 36s`。
- `clientSmoke` 仍有既有 skip：audio enabled formal path、render enabled tile path、audio enabled boss warning path。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 visible wall masonry course pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是暗区整体压迫感、整体墙砖密度、手工地砖密度和 Phase C 全量 surface cohesion。

### 2026-05-25 hidden stage ambient pocket pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续压低 `UI-demo-new` 首屏 map stage 外围背景网格，并提高房间从黑暗中浮出的压迫感。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 hidden-stage visual overlay、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：扩展 `render canvas suppresses broad hidden stage grid outside visible room`，要求房间左下外侧存在 deep hidden pocket，并确认该暗袋不覆盖可见房间焦点。
- `TileRenderer`：在 `drawHiddenStageGridSuppression` 中新增 lower-left deep pocket 与贴近 visible room 下缘的弱暖暗遮断，位置基于 `visibleClip` 与 `mapBounds`，保持 clipped 到 map stage 内。
- 增长理由：本轮只锁住“暗区不能继续读成大面积规则背景网格”的 director-grade 视觉合同；不新增资源、配置、schema 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas suppresses broad hidden stage grid outside visible room' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 lower-left deep pocket 断言先失败，当前 renderer 缺少该暗区材质层；失败点是 `TileRendererCanvasTest.kt` 的 deep pocket assertion。
- 调整说明：实现后发现测试样本的 viewport 左侧留白比最初估计更宽，采样点断言改为结构性约束：大尺寸、位于房间左侧、低于房间焦点且不覆盖房间中心。
- GREEN：补 hidden-stage ambient pocket 后 focused test 通过，`BUILD SUCCESSFUL in 23s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 23s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次 targeted 运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：左下暗区更像吞没房间边缘的黑暗材质，外侧规则网格读数进一步退后；右栏、底栏、导航和背包未受影响。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 28s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `0c92ee440eb10f74d8ed356726649b4c51aaeee7f9c491177bc073003f9fee75` |
| `ui-demo-new-parity-1280x800` | `3e5675c5e33cef8ae0b545c71312fbcbef7f11c9f5b8bb5b3f0f52457016ffb7` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `fdcc24773c92c3b21465ae70f369820cdb8d3de93e6607502cfea2273cd7b92d` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 左下 hidden stage 新增更深暗袋，房间外侧的规则大网格进一步退后；可见房间、actor、torch、loot 和目标框仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 39s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 hidden stage ambient pocket pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是暗区整体非矩形层次、墙角破形密度、地砖细节密度和 Phase C 全量 surface cohesion。

### 2026-05-25 left-stage shoulder pocket pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续压低 `UI-demo-new` 首屏左侧大暗区的规则背景网格，让中央房间更像从非均匀黑暗中浮出。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 hidden-stage visual overlay、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：继续扩展 `render canvas suppresses broad hidden stage grid outside visible room`，要求房间左肩外侧存在第二个 asymmetric deep pocket，并确认该暗袋不覆盖可见房间焦点。
- 过程修正：初始尝试 upper-right / upper-left 条件时 PR02-1 focused golden 无漂移，说明没有改变当前目标截图；本轮随后收敛到实际截图中仍明显的 left-stage shoulder 区域。
- `TileRenderer`：在 `drawHiddenStageGridSuppression` 中新增基于 `leftGap` 与 `visibleClip` 的 left-stage shoulder pocket，并保留 clipped 到 map stage 内。
- 增长理由：本轮只锁住“当前首屏左侧大暗区不能继续读成均匀规则网格”的 director-grade 视觉合同；不新增资源、配置、schema 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas suppresses broad hidden stage grid outside visible room' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 left-stage shoulder pocket 断言先失败，当前 renderer 缺少该暗区材质层；失败点是 `TileRendererCanvasTest.kt` 的 shoulder pocket assertion。
- GREEN：补 left-stage shoulder pocket 后 focused test 通过，`BUILD SUCCESSFUL in 24s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 24s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次 targeted 运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `ui-demo-new-map-stage-crop.png` 和 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：左侧大暗区的规则矩形网格进一步退后，房间左肩更像非均匀黑暗材质过渡；右栏、底栏、导航和背包未受影响。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 30s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `fe1b436e3516b68287b944bebddeda76d613fb9ca2c1085c33e006f8517f9403` |
| `ui-demo-new-parity-1280x800` | `d19d560ef255f3fd63fc9c267dc66c11542832a904dffa47bec8fd82528ebf8a` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `3a619f772cea972bcc16961b6c70fb6934d9406c30eba20879c27ac8cb9fbe92` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 左侧 hidden stage 新增横向 deep shoulder pocket，大片背景网格更退后；可见房间、actor、torch、loot 和目标框仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 41s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 left-stage shoulder pocket pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是全局非矩形暗区、墙角破形密度、地砖细节密度和 Phase C 全量 surface cohesion。

### 2026-05-25 bottom hidden-stage void basin pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续压低 `UI-demo-new` 首屏下方 hidden stage 的规则背景网格，让中央房间更像从非均匀暗面中浮出。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 hidden-stage visual overlay、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：继续扩展 `render canvas suppresses broad hidden stage grid outside visible room`，要求房间下方存在一块 broad void basin，并确认该暗面不覆盖可见房间焦点。
- `TileRenderer`：在 `drawHiddenStageGridSuppression` 中新增基于 `bottomGap` 与 `visibleClip` 的 bottom hidden-stage basin，并补一条贴近房间底缘的低透明遮断层，全部 clipped 到 map stage 内。
- 增长理由：本轮只锁住“当前首屏下方暗区不能继续读成均匀规则网格”的 director-grade 视觉合同；不新增资源、配置、schema 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas suppresses broad hidden stage grid outside visible room' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 bottom hidden-stage basin 断言先失败，当前 renderer 缺少下方 broad void basin；失败信息为 `hidden stage below the visible room should receive a broad void basin so the lower backdrop recedes as darkness instead of a regular stage grid`。
- GREEN：补 bottom basin 后 focused test 通过，`BUILD SUCCESSFUL in 23s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 23s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 第一次 targeted 运行按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 并对照 `UI/UI-demo-new.png` 后接受本轮方向：下方 stage grid 更退后，房间底部不再贴着规则背景，中央房间、actor、torch、loot、target frame、右栏和底栏未受破坏。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 29s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `1634957cac506e202a9cd5830bbf50b9f25a451f2999a42468ebd595b553c3ed` |
| `ui-demo-new-parity-1280x800` | `12e40cd63a6266880c886ab9b8bb689b4d5c593c8a0d77246817c800067a13f3` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `51c0fde745f1b9d5d6fae6e5657fc9cfc55b4c4b8baa5c1531fb1b5494b0bae6` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | 下方 hidden stage 新增 broad void basin，背景规则网格更退后；可见房间、actor、torch、loot 和目标框仍可读 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 41s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 bottom hidden-stage void basin pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是全局非矩形暗区层次、墙角破形密度、细颗粒地砖和 Phase C 全量 surface cohesion。

### 2026-05-25 visible room micro debris / joint plug pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续压低 `UI-demo-new` 首屏可见房间内部的等距黑色 lattice，让主舞台先读成旧石板材质而不是工程网格。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 visible-room overlay、focused renderer tests 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test 1：扩展 `render canvas adds localized grime and broken stone detail to visible room floor`，要求 visible room 地面出现多点小碎石和短暖色磨损边。
- focused test 2：扩展 `render canvas adds authored dungeon scars and stains to visible floors`，要求大房间内部用多个 off-grid stone plug 打断选定 grid intersection。
- `TileRenderer`：在 `drawVisibleRoomLocalizedStoneDamage` 中新增低透明 micro debris/chipped edges；在 `drawVisibleRoomGridDissolve` 中新增少量不规则 joint plug 和短暗缝。
- 增长理由：本轮只补 director-grade 地砖细节密度和格线断裂，不新增资源、配置、schema、helper、compat path 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas adds localized grime and broken stone detail to visible room floor' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas adds authored dungeon scars and stains to visible floors' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED 1：micro debris 断言先失败，失败信息为 `visible room floor should scatter multiple small chipped-stone debris marks so the ground gains hand-authored detail density instead of only broad stains`。
- GREEN 1：补 micro debris/chipped edge 后 focused test 通过，`BUILD SUCCESSFUL in 21s`。
- RED 2：joint plug 断言先失败，失败信息为 `large visible rooms should patch selected grid intersections with irregular stone plugs so long black lattice crossings stop reading as a uniform overlay`。
- GREEN 2：补 selected joint plugs 后 focused test 通过，`BUILD SUCCESSFUL in 20s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 21s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 focused golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- Targeted golden 在更新 expected hash 前按预期失败：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop` hash 漂移；right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 并对照 `UI/UI-demo-new.png` 后接受本轮方向：地面局部小破损和 grid intersection stone plug 更明显，长直 lattice 交叉点稍微退后；actor、loot、target frame、torch、右栏和底栏未被压糊。
- 更新 PR02-1 focused expected hash 后复跑：`BUILD SUCCESSFUL in 27s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `8cb68db53a57029ac47dc076e3e69824f649370da3945ef6d555edbb43c78aa7` |
| `ui-demo-new-parity-1280x800` | `2ecc1fd37b2bce8b82b97391975a44ffc19432fefe2033e2b3ab201abd4a0951` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `637b54ee38d6256fdb0e3fb23b942c4bdf4aa5fca491ec635fc38e82219a272d` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | visible room floor 新增小碎石、短暖色边和 selected grid-intersection stone plug，黑色 lattice 第一读数略有下降 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 40s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 focused rebaseline，因为漂移范围严格来自 visible room micro debris / joint plug pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是更高密度手工石板、墙体高差/洞口破形、全局非矩形暗区层次和 Phase C 全量 surface cohesion。

### 2026-05-25 visible wall vertical face / cast-shadow pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续提升 `UI-demo-new` 首屏可见长墙的体积感，让外圈墙先读成 raised masonry wall，而不是薄 tile border / 半透明 grid edge。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 visible wall 表现、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：扩展 `render canvas raises visible wall runs with crown and interior face`，要求长水平墙跑新增更宽更深的 underface shadow，并带一条低透明暖色 worn lip。
- `TileRenderer`：在 `drawVisibleWallRaisedFaces` 的水平 run 分支中补 floor-facing shadow band 和 worn lip，保留既有 crown / interior face 断言与 floor adjacency 约束。
- 增长理由：本轮只加强墙体厚度和高差读数，不新增资源、配置、schema、helper、compat path 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas raises visible wall runs with crown and interior face' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 underface shadow 断言先失败，失败信息为 `long visible wall runs should cast a deeper underface shadow so walls read as raised masonry mass rather than a thin tile border`。
- GREEN：补 wall underface shadow / worn lip 后 focused test 通过，`BUILD SUCCESSFUL in 22s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 23s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest' --rerun-tasks
```

结果：

- 方法级 `--tests '...dark uiux pr02 1 golden evidence hashes remain stable...'` 在 `:client:test` 与 `:client:goldenScreenshot` 下均未命中 backtick 方法名，失败原因为 `No tests found`；本轮改用 class-level `:client:goldenScreenshot` 获取 evidence 和失败列表。
- 更新 expected hash 前的 class-level golden 失败：24 tests completed, 13 failed；PR02-1 的实际漂移为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 并对照 `UI/UI-demo-new.png` 后接受本轮方向：上下长墙下沿更厚，墙体比上一版更像 raised stone wall；actor、loot、torch、target frame、右栏和底栏未被压糊。
- 更新 PR02-1 expected hash 后复跑 class-level golden：24 tests completed, 12 failed；失败列表不再包含 `dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts`。
- 剩余失败仍来自其它历史 golden groups，包括 PR01-1、PR02、PR03、phase4 PR05、boss warning、route midpoint、outcome、sample pack、gameplay log 等公共 map renderer 累计漂移；本轮不把这些全量截图混入 rebaseline。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `52ec414c11e32fd0d0d5e2a10fe6f26ac70d8abbb11d49832826d139edc52c23` |
| `ui-demo-new-parity-1280x800` | `bd7e413531cf26e5ba9145d4386e8041fb9d18bfcb7fdc65bdeea6c150d2d3c8` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `237e96c5f0aae142809b1acb89692e6b5ac5adb5cf9b08760f08a274c28c963f` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | visible long wall runs 新增更深 underface shadow 和轻微 worn lip，房间上下墙体比上一版更有高度；主房间 grid 仍可见 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 46s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 scoped rebaseline，因为漂移范围严格来自 visible wall vertical face / cast-shadow pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是更高密度墙砖颗粒、洞口/墙角破形、全局非矩形暗区压迫和 Phase C 全量 surface cohesion。

### 2026-05-25 visible room seam erosion / slab mask pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续压低 `UI-demo-new` 首屏主房间内部的规则 cell lattice，让地面先读成破碎石板面而不是工程网格。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 visible-room overlay、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：扩展 `render canvas adds authored dungeon scars and stains to visible floors`，要求 large visible room 至少出现 3 块 off-grid stone plates，用于遮断长内部 grid seam。
- `TileRenderer`：在 `drawVisibleRoomGridDissolve` 中新增三块低透明跨格石面板和短 warm/dark edge，位置不贴单格边界，保持在现有 visible-room overlay 内。
- 增长理由：本轮只补主房间地面第一眼材质读数，不新增资源、配置、schema、helper、compat path 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas adds authored dungeon scars and stains to visible floors' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 seam erosion 断言先失败，失败信息为 `large visible rooms should erode long internal grid seams with off-grid stone plates so the eye reads broken slabs before tile lattice`。
- GREEN：补 off-grid slab masks 后 focused test 通过，`BUILD SUCCESSFUL in 23s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 24s`。
- 仍有既有 deprecation warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest' --rerun-tasks
```

结果：

- 更新 expected hash 前的 class-level golden 失败：24 tests completed, 13 failed；PR02-1 的实际漂移为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 并对照 `UI/UI-demo-new.png` 后接受本轮方向：主房间新增几处不贴网格的石面板，内部长直 lattice 略退后；actor、loot、torch、target frame、右栏和底栏未被压糊。
- 更新 PR02-1 expected hash 后复跑 class-level golden：24 tests completed, 12 failed；失败列表不再包含 `dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts`。
- 剩余失败仍来自其它历史 golden groups，包括 PR01-1、PR02、PR03、phase4 PR05、boss warning、route midpoint、outcome、sample pack、gameplay log 等公共 map renderer 累计漂移；本轮不把这些全量截图混入 rebaseline。
- SDKMAN 在本轮 golden / owner gate 启动时提示 `INTERNET NOT REACHABLE`；本地 `java 21.0.10-tem` 与 `kotlin 2.2.21` 仍已进入当前 shell，Gradle 执行未因网络提示中断。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `c0c886552cb99ff0ca71f661c806843c1c45b4cdf6d5273a7ae4ea5c6e974f0c` |
| `ui-demo-new-parity-1280x800` | `3a4037f9722b40522a1c25682cfaf5ec354a4cc57b7f9c60c82b146f4ae7fa19` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `8f1ba15bf4a4bda4da78e4873f0a2cdb5028fe58873200ce288cd2303d472c4b` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | visible room 内部新增三处 off-grid stone plates，长直黑色 grid seam 略退后；主房间 grid 仍可见 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持跳过状态。
- 仍有既有 warning：SDKMAN 网络不可达提示、`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 scoped rebaseline，因为漂移范围严格来自 visible room seam erosion / slab mask pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是全局手工石板密度、墙砖颗粒、洞口/墙角破形、全局非矩形暗区压迫和 Phase C 全量 surface cohesion。

### 2026-05-25 visible floor micro-etch detail pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续提高 `UI-demo-new` 首屏主房间 floor cell 内部细颗粒材质密度，避免地面只靠跨格 overlay 和 seam 遮断支撑第一眼质感。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 floor material draw pass、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：扩展 `render canvas adds localized grime and broken stone detail to visible room floor`，要求 visible room floor 至少出现 24 条低透明 hairline etch，锁定运行尺寸下的手工石面细节密度。
- `TileRenderer`：在 `drawFloorMaterial` 内新增每个 floor cell 的 12px 暗色 hairline etch 与短暖色磨损边，位置落在 tile 内部而非边界，避免加重现有 cell lattice。
- 增长理由：本轮补的是 floor cell 内部材质密度，不新增资源、配置、schema、helper、compat path 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas adds localized grime and broken stone detail to visible room floor' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 hairline etch 断言先失败，失败信息为 `visible room floor should carry fine low-alpha hairline etches so individual cells gain hand-cut stone grain at runtime size`。
- GREEN：补 floor micro-etch 后 focused test 通过，`BUILD SUCCESSFUL in 20s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 更新 expected hash 前，PR02-1 focused golden 按预期失败；实际漂移为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：floor cell 内部细刻线非常克制，没有形成噪点层，actor、loot、torch、target frame、右栏和底栏未被压糊。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 26s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `d6f6b380a14c8094676033d2065181f4841286b945a2167a53886e54207dc450` |
| `ui-demo-new-parity-1280x800` | `a330442a10808bf59a0a6dfba4d7f0aee3f0b8bc0bbf0650fe934f72890ee400` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `4251d8314e3765e9f7f88ee66d4902c6417411d25c6f23095d8c81b38b38a77b` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

人工查看证据：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR02-1 map crop | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` | floor cell 内部新增低透明细刻线和短暖色磨损边，整体更接近细颗粒旧石地面；主房间 grid 仍可见 |
| PR02-1 demo parity | `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` | full-screen 首屏漂移只来自 map stage；右栏、底栏、导航和背包 crop 未受本轮影响 |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 35s`。
- `clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持跳过状态。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 scoped rebaseline，因为漂移范围严格来自 visible floor micro-etch detail pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是墙体暗部高差、非矩形洞口/墙角破形、全局暗区压迫和 Phase C 全量 surface cohesion。

### 2026-05-25 wall corner buttress / return shadow pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续提高 `UI-demo-new` 首屏 visible wall corner 的厚墙读数，让房间角落不再只像薄 tile intersection。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 visible wall crown/corner draw pass、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：扩展 `render canvas articulates visible wall crowns with uneven cap blocks`，要求 visible wall corner 出现 25-29px 的 dark masonry return block。
- `TileRenderer`：在 `drawVisibleWallCrownBlocks` 内复用 wall/floor points，识别两个正交 wall 与对角 floor 构成的四向房间角，补 dark return block 与短 worn lip。
- 增长理由：本轮补的是 wall corner 厚度和 dungeon silhouette，不新增资源、配置、schema、helper、compat path 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas articulates visible wall crowns with uneven cap blocks' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 corner buttress 断言先失败，失败信息为 `visible wall corners should gain dark masonry return blocks so room corners read as thick buttresses rather than thin tile intersections`。
- GREEN：补 wall corner return block 后 focused test 通过，`BUILD SUCCESSFUL in 22s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 24s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 更新 expected hash 前，PR02-1 focused golden 按预期失败；实际漂移为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：角墙 dark return 很克制，提升房间角落厚度读数，没有压坏 actor、loot、torch、target frame、右栏或底栏。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 31s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `635e1b77c97e66fedf6ae234fba6c48a41e636a085117f6571084c5f540ad644` |
| `ui-demo-new-parity-1280x800` | `6d8eea6e966b39b1a5b5df36cd664b42dfa66551cd4ad3d5ad105eaa5974139b` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `62f4f959ffa7031c9551d81fec266a39ed1a485f6b739a82e6802f4181136fe9` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 44s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 scoped rebaseline，因为漂移范围严格来自 wall corner buttress / return shadow pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是非矩形洞口破形、暗区压迫、墙面颗粒密度和 Phase C 全量 surface cohesion。

### 2026-05-25 map-stage upper-left vault veil pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续压低 `UI-demo-new` 首屏左上/左侧 hidden stage 的规则网格读数，让房间更像从 layered dungeon darkness 中浮出。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 map-stage backdrop / hidden-stage dark veil、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- 先试 visible-clip upper-left dark pocket：focused hidden-stage test 能 RED/GREEN，但 PR02-1 focused golden 无漂移，说明该分支未触达当前首屏 evidence，只能作为通用布局防护，不能作为本轮主要视觉证据。
- focused test：新增 `render canvas layers irregular darkness into map stage backdrop`，要求 map stage 左上侧出现 `0.138f` alpha 的大块 irregular vault veil。
- `TileRenderer`：在 `drawMapStageShadowVeil` 中补 upper-left deep veil、短暖色 worn edge 与次级深暗块，保证当前 PR02-1 首屏 crop 有可审查变化。
- 增长理由：本轮补的是 stage 背景暗区层次，不新增资源、配置、schema、helper、compat path 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas suppresses broad hidden stage grid outside visible room' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas layers irregular darkness into map stage backdrop' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- hidden-stage visible-clip 断言先 RED，失败信息为 `hidden stage should add an upper-left vault pocket so the room perimeter emerges from layered darkness instead of a rectangular backdrop veil`；补通用 left/top pocket 后 focused test 通过。
- map-stage backdrop 断言先 RED，失败信息为 `map stage backdrop should include an upper-left irregular vault veil so hidden darkness reads as layered dungeon depth rather than a flat rectangular scrim`；补 `drawMapStageShadowVeil` 后 focused test 通过，`BUILD SUCCESSFUL in 20s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 21s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- visible-clip branch 初版复跑 PR02-1 focused golden 无漂移，确认未触达当前首屏 evidence。
- map-stage backdrop 改动后，更新 expected hash 前 PR02-1 focused golden 按预期失败；实际漂移为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：左上/左侧 hidden stage 更压入暗处，主房间、actor、loot、torch、target frame、右栏和底栏未被压糊。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 26s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `a49680aace8b99a855dd626c9c3f410a3e4a2337897b0af432f2cb509159a0ee` |
| `ui-demo-new-parity-1280x800` | `1dbf4f44b59ef6cca67fd8f6d607c99b7f92ea0440590b8af61730b220099f38` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `82dbb224ef9c83ce19226c4a562a67a337ba6eab1d08feb8c160ce7fe47d83f2` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持跳过状态。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 scoped rebaseline，因为最终漂移范围严格来自 map-stage upper-left vault veil pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是非矩形洞口破形、墙面颗粒密度、细颗粒地砖和 Phase C 全量 surface cohesion。

### 2026-05-25 visible wall secondary masonry grain pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续补 `UI-demo-new` 首屏地图墙体材质密度，让 visible wall 不只依赖少量大块 course marker。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 visible wall masonry grain、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：扩展 `render canvas lays granular masonry courses along visible wall faces`，新增 secondary dark mortar ticks 与 warm worn flecks 的数量断言。
- `TileRenderer`：在 `drawVisibleWallMasonryCourses` 中按 visible wall/floor adjacency 补低透明暗色短缝、暖色磨损点和侧墙短缝，保持所有细节由现有 visible wall 材质路径生成。
- 增长理由：本轮补的是当前 director review 明确指出的墙面颗粒密度，不新增资源、配置、schema、compat path、second authority 或跨层状态。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas lays granular masonry courses along visible wall faces' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 secondary mortar tick 断言先失败，失败信息为 `visible wall masonry should include multiple secondary dark mortar ticks so wall faces gain dense hand-built stone grain instead of sparse course markers`。
- GREEN：补 wall secondary masonry grain 后 focused test 通过，`BUILD SUCCESSFUL in 20s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 22s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 更新 expected hash 前，PR02-1 focused golden 按预期失败；实际漂移为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：墙面颗粒更密，主房间、actor、loot、torch、target frame、右栏和底栏未被压糊。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 26s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `bfe20db6802cbb79391abc51c40217a24ac547b78e1c1d97cad364592f31c80c` |
| `ui-demo-new-parity-1280x800` | `0679e67bad6a0b779ec9b2a82fbf0d92966af5b7cf24c5997e7010aad22f187b` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `c4663290012193a521b8ca05eef54d14043649a532a84cc7c875b87e027867a9` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持跳过状态。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 scoped rebaseline，因为漂移范围严格来自 visible wall secondary masonry grain pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是非矩形洞口破形、全局暗区压迫、细颗粒地砖整体密度和 Phase C 全量 surface cohesion。

### 2026-05-26 visible floor fine-grain density pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续补 `UI-demo-new` 首屏地图地面细颗粒密度，降低主房间中心 lattice 的工程感。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 visible floor localized detail、focused renderer test 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：扩展 `render canvas adds localized grime and broken stone detail to visible room floor`，新增 tiny pitted stone specks 与 short off-grid cut marks 的数量断言。
- `TileRenderer`：在 `drawVisibleRoomLocalizedStoneDamage` 中补稳定坐标的低透明暗色小凹点和短暖色划痕，继续复用现有 visible room floor detail pass。
- 增长理由：本轮补的是当前 director review 明确指出的细颗粒地砖密度，不新增资源、配置、schema、compat path、second authority 或跨层状态。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas adds localized grime and broken stone detail to visible room floor' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED：新增 pitted stone specks 断言先失败，失败信息为 `visible room floor should include a field of tiny pitted stone specks so the surface gains dense hand-worked grain at runtime size`。
- GREEN：补 fine-grain floor density 后 focused test 通过，`BUILD SUCCESSFUL in 20s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 20s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 更新 expected hash 前，PR02-1 focused golden 按预期失败；实际漂移为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 与 `ui-demo-new-parity-1672x941.png` 后接受本轮方向：主房间地面颗粒和短划痕更密，但 actor、loot、torch、target frame、右栏和底栏未被压糊。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 26s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `58ea9b248cd7f28ffc28a6a20097422f96b303ed1f6aa6157bc12b1f86984b02` |
| `ui-demo-new-parity-1280x800` | `e6679019a04c14c975b0330426babd3b4a41a906856e7de509f521e098db4395` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `73b8ba4611bf9577a7745d20000a2900e08e6db5d71f537b6c3cee1436b6a31d` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持跳过状态。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 scoped rebaseline，因为漂移范围严格来自 visible floor fine-grain density pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是全局非矩形暗区压迫、洞口破形深度和 Phase C 全量 surface cohesion。

### 2026-05-26 asymmetric stage void pressure pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续处理 `UI-demo-new` 首屏 map stage 的全局非矩形暗区压迫，降低外围 stage grid 的第一读数。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`。
- 合同边界：只改 `client` renderer 的 hidden-stage / map-stage backdrop veil、focused renderer tests 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test 1：扩展 `render canvas suppresses broad hidden stage grid outside visible room`，新增 right-side void shoulder 断言，并要求该暗肩不覆盖 visible room focal area。
- `TileRenderer`：在 `drawHiddenStageGridSuppression` 中补右侧 clipped void shoulder 与低透明暖色石缘。随后 PR02-1 focused golden 通过且无 hash drift，说明该 synthetic 分支未触达当前首屏 canonical evidence，不能单独算本轮视觉进展。
- focused test 2：扩展 `render canvas layers irregular darkness into map stage backdrop`，新增 lower-right irregular void veil 断言。
- `TileRenderer`：在 `drawMapStageShadowVeil` 中补 lower-right dark veil、次级暗面和短暖色石缘，让真实首屏外围 stage 更退后。
- 增长理由：本轮补的是当前 director review 明确指出的外围暗区仍偏矩形 stage grid；不新增资源、配置、schema、compat path、second authority 或跨层状态。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas suppresses broad hidden stage grid outside visible room' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas layers irregular darkness into map stage backdrop' --rerun-tasks
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --rerun-tasks
```

结果：

- RED 1：right-side void shoulder 断言先失败，失败信息为 `hidden stage should add an asymmetric right-side void shoulder so the visible room is framed by non-rectangular darkness rather than a flat stage grid`。
- GREEN 1：补 hidden-stage right shoulder 后 focused test 通过，`BUILD SUCCESSFUL in 20s`。
- 正确 task 下首次 PR02-1 focused golden 通过但无漂移，说明 hidden-stage synthetic 分支未改变 canonical 首屏。
- RED 2：lower-right map-stage void veil 断言先失败，失败信息为 `map stage backdrop should include a lower-right irregular void veil so the playable room is surrounded by asymmetric dungeon darkness instead of a balanced rectangular stage`。
- GREEN 2：补 map-stage lower-right void veil 后 focused test 通过，`BUILD SUCCESSFUL in 20s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 21s`。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap`、Gradle 9 compatibility。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 更新 expected hash 前，PR02-1 focused golden 按预期失败；实际漂移为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 并对照 `UI/UI-demo-new.png` 后接受本轮方向：右下/右侧外围暗区更退后，中央房间、actor、loot、torch、target frame、右栏和底栏未被压糊。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 26s`。
- 另外三次早期 `:client:test --tests ...GoldenScreenshotHarnessTest...` 过滤尝试失败为 `No tests found`；原因是 golden screenshot 测试属于 `:client:goldenScreenshot` owner task，不能用 `:client:test` 作为有效入口。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `ec33289c83444a80c5e8449472553867f9f7faee3aba317724eb1a5b05f642e8` |
| `ui-demo-new-parity-1280x800` | `bde4d50e1ee22499aa046cd5c5001901fac19d107ea3da21932e72b1cfc5382c` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `f8d92b34b97e52fcef722c1dfe5bee9bd9c830aa8d34b053d776a0e4638aaf60` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke --rerun-tasks
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 34s`。
- `clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持跳过状态。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility、tools 侧 Json / Phase4ReportRunner warning。

结论：

- 本轮可接受 PR02-1 scoped rebaseline，因为最终漂移范围严格来自 map-stage lower-right asymmetric void veil pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；后续优先级仍是洞口破形深度、资源级墙砖厚度、手工石材密度和 Phase C 全量 surface cohesion。

### 2026-05-26 recessed corridor aperture depth pass 后反馈

预检：

- 命中方向：`UI/goal` Phase B，继续处理 `UI-demo-new` 首屏 map stage 的 corridor mouth / room opening 深度，让入口从平面 tile seam 推进到厚墙暗口。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：只改 `client` renderer 的 passage threshold visual layer、focused renderer tests 和 PR02-1 golden hash；不改 `core/game` 规则、snapshot、save/replay/profile、content-pack、manifest schema 或正式资源。

实现记录：

- focused test：扩展 `render canvas caps corridor mouths with broken lintel stones`，新增 `0.287f` recessed throat shadow 断言，要求 corridor mouth 有比 lintel/cap 更深的凹入口暗面。
- `TileRenderer`：在 `drawVisiblePassageThresholds` 的 `wideNorth/wideSouth/wideEast/wideWest` 宽洞口分支中补同一套 clipped recessed shadow，保持上下左右对称，不新增 helper、配置、schema、compat path、second authority 或跨层状态。
- PR02-1 composition 追查：`dark-uiux-pr02-1-demo-shell-foundation` 走 `ValidationScenarioRegistry` 固定 seed，并由 `FoundationGameSession.prepareDarkUiuxDemoMapStageAnchor()` 选择 demo room/anchor；当前首屏偏右属于 scenario/viewport 组合问题，若直接改 viewport 或 room selection 会扩大到 PR01/PR07 等 golden 合同，本轮先不混入。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas caps corridor mouths with broken lintel stones'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- RED：新增 recessed throat shadow 断言先失败，失败信息为 `corridor mouths should receive a recessed throat shadow so the opening reads carved into thick wall mass instead of a flat tile seam`。
- GREEN：补 passage threshold recessed shadow 后 focused test 通过，`BUILD SUCCESSFUL in 4s`。
- 复跑完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 2s`。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

结果：

- 更新 expected hash 前，PR02-1 focused golden 按预期失败；实际漂移为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 并对照 `UI/UI-demo-new.png` 后接受本轮方向：左侧/下方 passage mouth 更像凿进厚墙的暗口，actor、loot、torch、target frame、右栏和底栏未被压糊。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 26s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `516b1a431e139315970dcbabbe95008a25697f10e4b6e7f8bdcc52140f1d69b2` |
| `ui-demo-new-parity-1280x800` | `78ab6487e348764e907bc77933a965a30e9a32c4a26233a6945125ceb8e957a1` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `bf59c5648df62be7285fd045c0bb222e2672fc6a49d12639a2f07de46cf81e68` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`。
- `clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持跳过状态。
- 仍有既有 warning：`ScreenUtils.getFrameBufferPixmap` deprecation、Gradle 9 compatibility。

结论：

- 本轮可接受 PR02-1 scoped rebaseline，因为最终漂移范围严格来自 passage threshold recessed shadow pass，且 focused tests 已覆盖新增视觉合同。
- UI/UX director-grade 目标仍未关闭；下一轮优先级应从局部 doorway 细节转向更大的首屏 map composition / viewport framing，尤其是房间偏右和左侧暗区占比过大问题，同时继续推进资源级墙砖厚度、手工石材密度和 Phase C 全量 surface cohesion。

### 2026-05-26 map composition anchor pass

预检：

- 命中方向：`UI/goal` Phase B / PR02-1 first-screen quality，处理 `ui-demo-new` 首屏 map stage 房间/玩家视觉重心偏右与左侧暗区过大。
- 触碰范围：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core` 规则、save/replay/profile/schema/content-pack/manifest authority；不改全局 `TileMapViewport` 小地图居中合同；通用 `roomEntryPoint()` 保持不变。

实现记录：

- 新增 focused renderer test：`dark uiux demo map stage keeps visible room mass centered`，通过真实 PR02-1 validation scenario、真实 visual manifest 和 `TileRenderer.renderToCanvas` 计算 visible room mass 与 mapBounds center 的 screen-space drift。
- RED 1：测试夹具先用 `sampleResolver` 失败，真实场景要求 exact `tileset.ruins.wall_01` visual key；改用 `ClientAssetBundleLoader.load(...).visualResolver`。
- RED 2：当前构图失败，`visibleCenter=633`、`mapCenter=489`、`maxCenterDrift=96`，说明 visible room mass 右偏超过 3 tile。
- 第一次实现尝试把 demo anchor 向右取样，失败且方向相反：`visibleCenter=681`、`visibleLeft=505`，说明 FOV 暴露不足会让房间更右偏。
- 最终实现：PR02-1/PR07 demo 专用 `darkUiuxDemoStageVisualAnchor()` 以 room center 左侧 5 tile 为目标，从 walkable points 里选择 deterministic visual anchor，让可见房间轮廓向左展开；通用语义入口不变。
- 收紧验收：将 focused test 容忍从 3 tile 收紧到 2 tile；最终通过。

focused / scenario 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.dark uiux demo map stage keeps visible room mass centered'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :game:test --tests 'com.ktome.game.FoundationGameSessionTest.dark uiux pr02 2 launch scene starts from staged ruins map composition' --tests 'com.ktome.game.FoundationGameSessionTest.dark uiux pr02 2 launch scene seeds second backpack page evidence' --tests 'com.ktome.game.FoundationGameSessionTest.dark uiux pr07 launch scene reuses staged ui demo map composition'
```

结果：

- `:client:test --tests ...dark uiux demo map stage keeps visible room mass centered`：最终 `BUILD SUCCESSFUL in 4s`。
- `:game:test` PR02/PR07 scenario focused tests：`BUILD SUCCESSFUL in 4s`。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 更新 expected hash 前，PR02-1 focused golden 按预期失败；实际漂移仅为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 并对照 `UI/UI-demo-new.png` 后接受本轮方向：room mass 更接近 stage 中线，左侧整块空暗场占比下降，右栏/底栏未漂移。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 8s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `56828a3dc00d6b495c0c8862c9d4e5c95cdd09dc3240e88e81579b80498c949a` |
| `ui-demo-new-parity-1280x800` | `fdd93411d55b6db98779eb84f19e25a7591041f8b90d779eb13d6afcf246d8a1` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `322c824b288d4343ed9a3be94b485252400085fd68fcd1ab051b3ffda24c1879` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

visual metric：

- `ui-demo-new-map-stage-crop` 粗略亮度/饱和像素重心从上一轮约 `x=1142` 收回到约 `x=906`；crop 中线为 `x=841.5`。
- 这是 director review 的辅助指标，不替代截图肉眼验收；当前仍有资源级墙砖厚度、手工石材密度和全量 surface cohesion 差距。

owner / preflight gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 15s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持跳过状态。
- `verifyChanged`：失败，失败任务为全量 `:client:goldenScreenshot`。
- `verifyChanged` 失败前已通过：`:tools:scopeCoverageLint`、`:tools:verifyContractLintPreflight`、`:tools:maintainabilityLint`、`:tools:resourcePipelineLint`、`:tools:darkKeyRegistryLint`、`:tools:darkSpriteSheetLint`、`:tools:spriteSheetMapLint`、`:tools:darkArtRandomQa`、`:tools:darkManifestCoveragePr02OwnerScope`、`:tools:darkManifestCoveragePr02_1OwnerScope`、`:tools:darkManifestCoveragePr02_2OwnerScope` 等。
- 全量 golden 失败范围较大，包括 PR01-1、PR02、formal screens、boss warning、route midpoint、gameplay log、outcome recap、sample pack、PR03、PR05 等 hash 漂移；本轮只完成并复跑通过 PR02-1 focused golden，没有把这些全量 hash 漂移伪装成已验收。

结论：

- 本轮可接受 PR02-1 map composition scoped rebaseline；它只改变 `ui-demo-new` parity 与 map-stage crop，右栏/底栏/nav/inventory crop 保持稳定。
- 目标未关闭。全量 `client:goldenScreenshot` 漂移需要单独做 director review / rebaseline 或回查是否存在统一截图方向/采样基线变更，不能在本轮 PR02-1 构图修正里静默吸收。

### 2026-05-26 full golden stabilization / verifyChanged 收口

预检：

- 命中方向：`UI/goal` Phase A evidence stabilization，处理上一轮 `verifyChanged` 在全量 `:client:goldenScreenshot` 暴露的历史 golden 漂移。
- 触碰范围：`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 renderer 行为、规则层、save/replay/profile、content-pack、manifest schema、正式资源或 whitebox scenario；只在人工复核后同步已有 golden expected hash。
- 归因判断：当前漂移是截图方向归一化和后续 UI/resource state 累积后留下的历史 expected hash stale，不是本轮新增的截图机制 bug。

人工查看代表性 evidence：

| Evidence | Path | 当前观察 |
| --- | --- | --- |
| PR01-1 reference shell | `client/build/reports/golden/dark-uiux-pr01-1/dark-uiux-pr01-1-tome-layout-reference.png` | 方向正确，非黑屏，主 shell 可读 |
| PR02 round1 chrome | `client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-round1-chrome.png` | 方向正确，UI chrome 和地图 stage 正常渲染 |
| PR03 equipment slots | `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-equipment-slots.png` | 方向正确，equipment/inventory surface 可读 |
| PR05 telegraph triple surface | `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` | 方向正确，telegraph evidence 非黑屏或明显裁切错误 |

实现记录：

- 从 `client/build/test-results/goldenScreenshot/TEST-com.ktome.client.golden.GoldenScreenshotHarnessTest.xml` 和各 evidence index 中读取当前 actual hash。
- 将 `GoldenScreenshotHarnessTest` 的 PR01-1、PR02、formal screens、boss warning、route midpoint、gameplay log、outcome recap、sample-pack、phase4 PR03、dark-uiux PR03、phase4 PR05、phase4-v4 PR05 等 expected hash 同步到当前 upright evidence。
- PR02-1 的最新 map composition hash 保持上一节已人工接受的值：`ui-demo-new-parity-1672x941=56828a3dc00d6b495c0c8862c9d4e5c95cdd09dc3240e88e81579b80498c949a`、`ui-demo-new-map-stage-crop=322c824b288d4343ed9a3be94b485252400085fd68fcd1ab051b3ffda24c1879`。
- 本轮没有把 rebaseline 解释为视觉质量达标；它只是恢复 golden 回归面可信度。

全量 golden 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 59s`。
- 编译输出仍有既有 `ScreenUtils.getFrameBufferPixmap` deprecation warning。
- Gradle 仍提示 deprecated Gradle features will be incompatible with Gradle 9.0。

共享入口命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `verifyChanged`：`BUILD SUCCESSFUL in 14s`。
- impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`。
- 本次实际执行项包括 `:client:clientSmoke`、`:tools:darkSpriteSheetLint` 与 `:tools:reportPhase4Only`；其余命中的 owner/preflight task 多数为 up-to-date 或 not-recorded。
- `clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 仍保持 SKIPPED。

结论：

- Phase A evidence/golden gate 当前已收口：全量 golden 与 `verifyChanged` 均通过。
- 这次 full rebaseline 只接受当前 upright evidence 的 hash，不等价于 UI/UX director-grade 关闭。
- 下一轮应回到产品质量本体：继续处理资源级墙砖/地面密度、非矩形暗区压迫、洞口破形、torch/fog 光照层次和 Phase C 全量 surface cohesion。

### 2026-05-26 torch focal restraint pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D，处理 `UI-demo-new` 首屏和共享 map renderer 的 torch/fog 光照层级。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、save/replay/profile/schema、content-pack、manifest authority 或正式资源；只收敛 `client` map renderer 的可见火把焦点数量。
- 设计判断：当前首屏相比参考图仍有过多装饰火把和大面积琥珀 wash；参考图的火光更像局部 focal light，周围由暗石和 fog 压迫承接。

实现记录：

- 新增 focused test：`render canvas keeps torch focal points sparse so dark stage pressure remains`。
- RED：新增 test 先失败，失败信息为 `large rooms should keep torch flames to a few authored focal points; too many flames turn local firelight into a map-wide amber wash`。
- GREEN：将 `visibleTorchWallTiles(...)` 的最终 `.take(6)` 收敛为 `.take(4)`，保持排序和候选规则不变。
- 代码没有新增 option matrix、helper-like business type、compat path、second authority 或临时 path。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas keeps torch focal points sparse so dark stage pressure remains'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 新增 focused test：先 `BUILD FAILED`，再 `BUILD SUCCESSFUL in 4s`。
- 完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 更新 expected hash 前，PR02-1 focused golden 按预期失败；漂移仅为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 并对照 `UI/UI-demo-new.png` 后接受本轮方向：火把减少后仍保留局部焦点，但全局琥珀铺满感下降。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 8s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f0e62c05911f8f506167ae2fe96b757b075d7b8af3389afcf9b889b2d2e2142c` |
| `ui-demo-new-parity-1280x800` | `9781293b26d84a15593b12ec499c384ee42e4fd94b11a41be517ddea0d15f8b5` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `2e0c0ceb54864695689814a5dd8086d3c19f564218fb02ab6670fec2ed345a44` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

全量 golden / owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- 更新全量 expected hash 前，`:client:goldenScreenshot` 因共享 map lighting 行为变化失败 10 个 test group；失败范围与含地图/局内 surface 的 golden 一致。
- 抽查 `dark-uiux-pr01-1-tome-layout-reference`、`phase4-uiux-pr05-telegraph-triple-surface`、`dark-uiux-pr03-inscription-shop`、PR02-1 parity/map crop 后，确认方向正确、非黑屏、UI 可读，变化符合火把 focal restraint 目标。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 59s`。
- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 继续覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`。

结论：

- 本轮是可接受的共享 map renderer rebaseline：火把焦点减少，局部光源语言更克制，首屏更接近参考图的暗石压迫感。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理主房间长直 grid/lattice、墙砖厚度、手工地面密度、洞口/墙角破形和 Phase C 全量 surface cohesion。

### 2026-05-26 actor grounding contact-shadow pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D，处理 `UI-demo-new` 首屏和共享 map renderer 的 actor/floor 接触关系。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、actor snapshot、地图生成、save/replay/profile/schema、content-pack、manifest authority 或正式资源；只改 `client` renderer 的 actor presentation draw path。
- 设计判断：当前 map tile 材质和暗区已经多轮收敛，但 actor/enemy/bonfire 主体仍主要依赖 sprite 本身，缺少 floor contact，第一眼容易读成贴在 grid 上。

实现记录：

- 新增 focused test：`render canvas grounds actor sprites with compact floor contact shadows`。
- RED：新增 test 先失败，失败信息为 `actor sprites should receive a compact dark floor contact shadow so they feel grounded in the dungeon tile, not pasted over the map`。
- GREEN：在 `TileRenderer` 的 `MAP_ACTORS` 路径中，actor asset 绘制前调用 `drawActorGroundingShadow(...)`。
- `drawActorGroundingShadow(...)` 复用 visual footprint / pivot 计算 actor bounds，在 footprint 下方绘制紧凑暗色 contact shadow 和一条弱暖色 worn-stone edge。
- 代码没有新增 option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas grounds actor sprites with compact floor contact shadows'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 新增 focused test：先 `BUILD FAILED`，再 `BUILD SUCCESSFUL in 4s`。
- 完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

PR02-1 golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

结果：

- 更新 expected hash 前，PR02-1 focused golden 按预期失败；漂移仅为 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-map-stage-crop`。
- `ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 和 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 后接受本轮方向：actor、enemy、bonfire 脚下接触更稳，未遮挡目标框、loot marker、right panel 或 bottom deck。
- 更新 PR02-1 expected hash 后复跑 focused golden：`BUILD SUCCESSFUL in 8s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f16a23e5b4020add3af71f4df5bdf29ba35c087f4fe6830ce38ec1b992ac9cb5` |
| `ui-demo-new-parity-1280x800` | `ead95f0c2b033a3b2f4b08522ac20fa353ef27d5d0c8fc108499940dab54ba0b` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `8c9c95c642988cbe86a26eb451bc0f42b352955b155b4e17fb32c5bd29c7a434` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

全量 golden / owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- 更新全量 expected hash 前，`:client:goldenScreenshot` 因 shared actor renderer 行为变化失败 9 个 test group；失败范围与含 actor 的局内 map/surface golden 一致。
- 抽查 `client/build/reports/golden/dark-uiux-pr01-1/dark-uiux-pr01-1-tome-layout-reference.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后，确认方向正确、非黑屏、非裁切错误，actor/telegraph/log/sidebar 可读。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 59s`。
- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 15s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的共享 actor renderer rebaseline：地图主体获得 floor contact，首屏和 PR05 combat/telegraph surface 的 actor 不再完全漂在 tile lattice 上。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理主房间长直 grid/lattice、墙砖厚度、手工地面密度、洞口/墙角破形和 Phase C 全量 surface cohesion。

### 2026-05-26 irregular stone islands lattice-mask pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D，处理 `UI-demo-new` 首屏和共享 map renderer 的 visible-room floor lattice。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority 或正式资源；只改 `client` renderer 的 floor presentation pass。
- 设计判断：上一轮 actor grounding 后，主体贴地关系改善，但 `ui-demo-new-map-stage-crop` 中主房间长直格线仍然比参考图更像工程网格；本轮只补少量跨格 stone mass，不做大换皮。

实现记录：

- 新增 focused test：`render canvas masks long floor lattice with irregular stone islands`。
- RED：新增 test 先失败，失败信息为 `large visible rooms should mask long floor lattice with off-grid stone islands that cross both row and column seams`。
- GREEN：在 `TileRenderer.drawVisibleRoomPainterlyBreakup(...)` 中补两块低透明 off-grid stone island、一条暖色 worn lip 和一条暗色 cut。
- 断言修正：第一次 GREEN 尝试发现 `TileMapViewport.tileRect` 的屏幕 y 轴与地图 y 轴相反；测试锚点改为实际屏幕落点后 focused 用例通过。
- 代码没有新增 option matrix、业务 model 字段、helper 抽象、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas masks long floor lattice with irregular stone islands'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 新增 focused test：先 `BUILD FAILED`，再 `BUILD SUCCESSFUL in 2s`。
- 完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map floor renderer 行为变化失败 11 个 test group；失败范围与含 visible map floor 的 golden 一致。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受本轮方向：stone islands 只影响 map stage，未遮挡 actor、loot、telegraph、right panel、bottom deck 或关键文本。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 59s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `b69a9ba723ec7b32461a5703a08be2a196f09f07d5870081e565295e7ffbcc28` |
| `ui-demo-new-parity-1280x800` | `74b563f36226c3d576ad7527df098b9c39f62261bc15f59de70ac697d48d069c` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `41b50a5ccfccf9e4b157b74da2a0bd934fa8cd7fc3a215fc86987e2fadb2cc10` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的共享 map floor renderer rebaseline：主房间中心和上半区多了跨格石材 mass，连续 lattice 的第一眼工程感下降。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、洞口/墙角破形、非矩形暗区压迫和 Phase C 全量 surface cohesion。

### 2026-05-26 jagged aperture edge-bite pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D，处理 `UI-demo-new` 首屏和共享 map renderer 的 visible-room aperture / edge silhouette。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority 或正式资源；只改 `client` renderer 的 room edge presentation pass。
- 设计判断：irregular stone islands 后，主房间内部格线读数下降，但左右侧边和上墙 crown 仍过直；本轮继续做小步 silhouette 破形，不做资源级大换皮。

实现记录：

- 新增 focused test：`render canvas breaks straight room edges with jagged aperture bites`。
- RED：新增 test 先失败，失败点为 left-edge bite 断言，说明当前 room edge 仍缺侧边暗 bite。
- 第一次 GREEN 尝试因 `drawVisibleRoomCornerBreakup(...)` 内缺少 `width/height` local 编译失败；补局部变量后 focused 用例通过。
- GREEN：在 `TileRenderer.drawVisibleRoomCornerBreakup(...)` 的已有 corner chip 后补左右侧边深色 bite、短 worn lip 与顶部 chipped crown bite。
- 代码没有新增 option matrix、业务 model 字段、helper 抽象、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks straight room edges with jagged aperture bites'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 新增 focused test：先 `BUILD FAILED`，再因缺 `width/height` local 编译失败一次，修正后 `BUILD SUCCESSFUL in 7s`。
- 完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map edge presentation 行为变化失败 11 个 test group；失败范围与含 visible map aperture / room edge 的 golden 一致。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受本轮方向：左右侧边和上墙 crown 更不矩形，actor、loot、telegraph、right panel、bottom deck 和关键文本未被遮挡。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `8e577d7c883c706f79bb98a2ff077bedeb85baa15907868f7eff0c6a6ff73246` |
| `ui-demo-new-parity-1280x800` | `61f382700df0b4e4a9c51edb422abc849fbedab7f601184628e2bb0e6bf27aab` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `ab7de0918cec8f50c1036986679876bbed91224bd653a37eaeb97939f4ea7c85` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的共享 map edge presentation rebaseline：房间左右边和上墙 crown 的完整矩形读感下降，dark aperture 压迫感更强。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、fog/lighting 压迫、墙体高差和 Phase C 全量 surface cohesion。

### 2026-05-26 cool edge fog pressure pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D，处理 `UI-demo-new` 首屏和共享 map renderer 的 visible-room lighting / fog pressure。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority 或正式资源；只改 `client` renderer 的 visible-room atmosphere presentation pass。
- 设计判断：上一轮 aperture edge-bite 后，房间边界更不矩形，但运行截图仍偏均匀暖光 wash；本轮用低透明冷暗边缘和左下暗盆托住火把焦点，不做全局大换皮。

实现记录：

- 新增 focused test：`render canvas cools visible room edges with asymmetric fog pressure`。
- RED：新增 test 先失败，失败点为 right-edge cool fog pressure veil 断言，说明当前 atmosphere 缺少这种冷暗 falloff。
- GREEN：在 `TileRenderer.drawVisibleRoomAtmosphere(...)` 既有 pass 内补右侧 cold fog veil、右侧短冷色 dropout、左下 broad dark basin 和 restrained worn-stone lip。
- 代码没有新增 option matrix、业务 model 字段、helper 抽象、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas cools visible room edges with asymmetric fog pressure'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 新增 focused test：先 `BUILD FAILED`，失败信息为 `visible room right edge should carry a cool fog pressure veil so the torch field falls back into dungeon darkness instead of staying evenly lit`；实现后 `BUILD SUCCESSFUL in 4s`。
- 完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared visible-room atmosphere 行为变化失败 11 个 test group；失败范围与含 visible map stage 的 golden 一致。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受本轮方向：右侧和左下冷暗 falloff 更明确，actor、loot、telegraph、right panel、bottom deck 和关键文本未被遮挡。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 59s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `cf605720722bbf7a0beb9b28aa81465053aad272a8afde296546c4bea814bbc5` |
| `ui-demo-new-parity-1280x800` | `536afbdca3d66c9adf676109a35cf0addfd2582ff29bbcc1ab59fe8d81611a77` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `8ac74fe9afb40f11306bdb3a11803cf285c1ce13495edeecb23382c0821ae89d` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的共享 visible-room atmosphere rebaseline：右侧和左下地面开始有更明确的冷暗 falloff，火把不再把整块房间均匀刷暖。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、墙体高差、全局 text/surface cohesion 和 Phase C 全量 screen polish。

### 2026-05-26 raised wall height terrace pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D，处理 `UI-demo-new` 首屏和共享 map renderer 的 wall height / masonry depth。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority 或正式资源；只改 `client` renderer 的 wall presentation pass。
- 设计判断：上一轮 cool edge fog 后，光照和 fog pressure 更稳，但墙体仍偏 tile-outline；本轮用相邻 floor 的暗阶梯和 ledge 强化高低差，不做资源级大换皮。

实现记录：

- 新增 focused test：`render canvas deepens visible wall height with interior shadow terraces`。
- RED：新增 test 先失败，失败点为 top wall interior shadow terrace 断言，说明当前 raised wall 仍缺向 floor 投出的高度阴影。
- GREEN：在 `TileRenderer.drawVisibleWallRaisedFaces(...)` 的既有 horizontal / vertical run 内补 top/bottom interior shadow terrace、worn-stone ledge 与 side-wall slim occlusion strip。
- 代码没有新增 option matrix、业务 model 字段、helper 抽象、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas deepens visible wall height with interior shadow terraces'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 新增 focused test：先 `BUILD FAILED`，失败信息为 `raised top wall runs should cast a broad interior floor shadow terrace so the wall reads as elevated masonry rather than a flat outline`；实现后 `BUILD SUCCESSFUL in 4s`。
- 完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared wall height presentation 行为变化失败 10 个 test group；失败范围与含 visible map stage / map renderer 的 golden 一致。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受本轮方向：墙体内侧更有高低差，actor、loot、telegraph、right panel、bottom deck 和关键文本未被遮挡。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 59s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `d8065c25a8e849aa8c7c898b9fb1284805ab70edf83189cd400eef480ea03795` |
| `ui-demo-new-parity-1280x800` | `9b6c91b98f4c20e7cbf0a96ea2b73bd2dcfc013bbe96dcdb49b32baf39770a5a` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `5300e8c992fd1c8feaf18ce02f230081089036a8e1bb9da48a7e969d748be221` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的共享 wall height presentation rebaseline：长墙内侧开始向 floor 投出更明确的暗阶梯，侧墙也有更厚的 stone mass 读感。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、全局 text/surface cohesion、Phase C 全量 screen polish 和真正资源级石材重制。

### 2026-05-26 authored tile material clarity pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D，处理 `UI-demo-new` 首屏和共享 map renderer 的 authored tile material clarity。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority 或正式资源；只调整 `client` renderer 已有 material / atmosphere presentation alpha。
- 设计判断：当前 ground tile asset 本身已有裂纹和石材纹理，但运行截图被 broad wash、grid dissolve 和 fog 压得偏糊；本轮让资源细节重新成为第一读，不做资源/manifest 大换皮。

实现记录：

- 新增 focused test：`render canvas keeps authored tile art clear beneath atmosphere washes`。
- RED：新增 test 先失败，失败点为 lighter foundation glaze 断言，说明当前 broad glaze 仍为 `0.105`，会盖住 authored tile art。
- GREEN：将 foundation glaze 从 `0.105` 收敛到 `0.082`，inner warm wash 从 `0.026` 到 `0.018`，visible-room atmosphere broad wash 从 `0.046` 到 `0.034`，painterly wash 从 `0.060` 到 `0.045`，internal grid dissolve 从 `0.052/0.050` 收敛到 `0.039/0.037`，joint plugs 和 seam erosion panels 同步降强。
- 代码没有新增 option matrix、业务 model 字段、helper 抽象、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas keeps authored tile art clear beneath atmosphere washes'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 新增 focused test：先 `BUILD FAILED`，失败信息为 `large visible rooms should use a lighter foundation glaze so the generated stone tile texture remains visible at runtime size`；实现后 `BUILD SUCCESSFUL in 5s`。
- 完整 `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map material clarity 行为变化失败 11 个 test group；失败范围与含 visible map renderer 的 golden 一致，`ui-demo-new` right panel、bottom deck、nav、inventory crop 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-shop-full-slot-replace.png` 后接受本轮方向：地面和墙面 tile 纹理更透出，地图未过亮，modal、right panel、bottom deck 和关键文本未被遮挡。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 59s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `2cbcef4796ba03c97f59ea1a97aa57e3e3d5c71e95d2385f435d1a42d614eb05` |
| `ui-demo-new-parity-1280x800` | `83731e46d7c5d22fac5b64e7bb2b4a283ebccbdd24a23a2c48030eb02ef5403e` |
| `ui-demo-new-right-panel-grid` | `23b77847ed67e92624eae4684f99b4ae4cbabab6f66583371929c752588f7aac` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `8a6f4723a52e98bc5b633cc0aff5e292c734f3e00c4121ce5cf2bcde0f435c83` |
| `ui-demo-new-inventory-page-1` | `95a5ee94276d3942f197bf88df9a7765c09c080177a04eb5699df0d5b7e985c9` |
| `ui-demo-new-inventory-page-2` | `68b13d93ef9af2f844efe01de94c00e8fccc5caad5c938c8ea677fa02d6e6cb1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 15s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 shared map material clarity rebaseline：已有 ground/wall resource 的裂纹、石材纹理和手工 grain 更容易被看见，fog 和 grid dissolve 退到辅助层。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理真正资源级石材重制、全局 text/surface cohesion 和 Phase C 全量 screen polish。

### 2026-05-26 operation hint restraint / surface cohesion pass

预检：

- 命中方向：`UI/goal` Phase C full-surface cohesion，处理 `UI-demo-new` 与共享 demo shell 右栏 `操作提示` 视觉权重过高的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、输入命令、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案；只调整 `client` renderer 的 command hint plate / key chip / shortcut text / label alpha。
- 设计判断：参考图的快捷键提示属于低权重操作辅助，当前实现的 key chip 与金色文字仍偏抢读；本轮把它降到辅助层，保留键位可发现性。

实现记录：

- 在 `dark uiux pr02-1 draws right panel slots and hero crest scaffold` 中收紧 operation hint 断言：key chip 必须使用低权重 alpha，`Ctrl+S` shortcut text 必须可读但不能满强度 gold。
- RED：focused test 先失败，失败点为 `operation hints should expose shortcuts as low-emphasis key chips for scanability`，说明当前 chip 仍为旧的高权重 `0.62`。
- GREEN：command hint plate 从 `0.76` 收敛到 `0.64`，key chip 从 `0.62` 到 `0.46`，left accent 从 `0.42` 到 `0.26`，top gold line 从 `0.20` 到 `0.12`，shortcut text 改为 `D99A2B / 0.74`，label text 从 `0.72` 到 `0.62`。
- 代码没有新增 option matrix、业务 model 字段、helper 抽象、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.dark uiux pr02-1 draws right panel slots and hero crest scaffold'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新约束 focused test：先 `BUILD FAILED`，失败信息为 `operation hints should expose shortcuts as low-emphasis key chips for scanability`；实现后 `BUILD SUCCESSFUL in 3s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared demo shell command hint presentation 变化失败 11 个 test group；失败范围与 right-panel / command-hint-bearing golden surface 一致。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-shop-full-slot-replace.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受本轮方向：operation hints 仍可读，但视觉权重低于铭刻、背包、地图和主日志。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 59s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f56202e7d598cfe4f9d1bd787dd1b13c4f4a56b12b1df2483640bedfa2c283d3` |
| `ui-demo-new-parity-1280x800` | `678017197785901df30f5609b80e106a5e1f2f157ea15b6e96826871895e24c0` |
| `ui-demo-new-right-panel-grid` | `83c2c6f91e647631350de2255b1a713766bf31f74e144dc01b35581abe1fd674` |
| `ui-demo-new-bottom-deck-no-command-hints` | `090e456da27858857688080d63101704e5d9edb8afefa9bdb5f81bef462fe76f` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `8a6f4723a52e98bc5b633cc0aff5e292c734f3e00c4121ce5cf2bcde0f435c83` |
| `ui-demo-new-inventory-page-1` | `cb04748b29786b0cbe0a1ddeed8792cd19d90cac3840d0747c33ce69269ba480` |
| `ui-demo-new-inventory-page-2` | `7d6e8246e3d0a8616460995fd00e94f926750bcfdea29cd72de9e1b1c8245834` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 shared command hint presentation rebaseline：右下操作提示从高亮提示块进一步降为辅助工具条，快捷键可发现性保留，但不再压过右栏装备/铭刻/背包层级。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理真正资源级石材重制、右栏/底栏 text scale 与 surface texture cohesion、Phase C 全量 screen polish。

### 2026-05-26 bottom deck caption scale / surface cohesion pass

预检：

- 命中方向：`UI/goal` Phase C full-surface cohesion，处理 `UI-demo-new` 与共享 demo shell 底部 action labels / bottom log 文字层级偏大的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/main/kotlin/com/ktome/client/render/TileTextMetrics.kt`、`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、输入命令、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案；只新增 text style 层级并调整 `client` bottom deck 文本绘制。
- 设计判断：参考图的底部操作文本属于辅助扫描信息，当前 full body/small 文字仍抢走 action icon 与 map stage 权重；本轮把 action label / log rows 降到 caption，让 bottom deck 从主喊话层回到操作辅助层。

实现记录：

- 新增 `TileTextStyle.CAPTION`，配套 `TileTextMetrics` 近似宽度与 line height，并在 `TileRenderer` 中接入 `captionFont` 的创建、绘制和 dispose。
- `DemoShellRenderer.renderActionDeck(...)`：action hotkey、text plate hotkey 和 label 改用 `TileTextStyle.CAPTION`，并细调 label baseline，避免中文技能名在底部 text plate 内显得过满。
- `DemoShellRenderer.renderLogAndStats(...)`：bottom log wrap 和 draw 改用 `CAPTION`，行高收敛到 `16f`，正文起点保留 signal rail 扫描边距。
- 代码没有新增业务 model 字段、layout schema、compat path、second authority、临时资源或规则路径。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas keeps chinese hotbar labels inside their slot cards' --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新约束 focused test 先 `BUILD FAILED`：`bottom action labels should use caption hierarchy instead of full body text`；route hint 断言也失败于 `bottom log message rows should use caption hierarchy so the log remains secondary to the map and actions`。
- 实现后 focused test：`BUILD SUCCESSFUL in 6s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared bottom text hierarchy 变化失败；失败范围集中在带 bottom log/action labels 的 golden surface，同时 nav、map-stage、right-panel 与 inventory crop 中不受本轮 bottom text 影响的项目保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-shop-full-slot-replace.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受本轮方向：底部 log/action labels 更小、更克制，没有黑屏、翻转、裁切错误或字体缺失；PR03 长英文日志仍有内容长度导致的自然换行，但 caption scale 改善了拥挤感。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 1s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `add5f22d8fb78768e1503414624d2c96ef2ddea2c0b5a592b8908313b71b38c7` |
| `ui-demo-new-parity-1280x800` | `136cb23e03e5e9447661ec67f90e7e4b49f1d2c65fa15dc451fb3dda14240798` |
| `ui-demo-new-right-panel-grid` | `83c2c6f91e647631350de2255b1a713766bf31f74e144dc01b35581abe1fd674` |
| `ui-demo-new-bottom-deck-no-command-hints` | `17e0a444e24b0677fef4abc4c3ede2405a221f8760a2e58271d84c7ea740d978` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `8a6f4723a52e98bc5b633cc0aff5e292c734f3e00c4121ce5cf2bcde0f435c83` |
| `ui-demo-new-inventory-page-1` | `cb04748b29786b0cbe0a1ddeed8792cd19d90cac3840d0747c33ce69269ba480` |
| `ui-demo-new-inventory-page-2` | `7d6e8246e3d0a8616460995fd00e94f926750bcfdea29cd72de9e1b1c8245834` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 shared bottom text hierarchy rebaseline：底部日志和 action labels 从 full/small 主信息层降为 caption 辅助层，保留操作可发现性和长中文日志可读性，同时降低底栏对地图与技能图标的抢读。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理真正资源级石材重制、底部/右栏 text scale 全量一致性和 Phase C 全量 screen polish。

### 2026-05-26 broad mortar veil / lattice suppression pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D map-stage material quality，处理 `UI-demo-new` 主房间中心长直 tile lattice 仍偏规则、偏工程化的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` visible-room grid dissolve overlay。
- 设计判断：参考图的地面可以保留格线，但第一眼应读成破损石板和砂浆层；当前主房间仍有若干横竖长线过连续，本轮用跨格 veil 把缝线切成材质层。

实现记录：

- 在 `TileRenderer.drawVisibleRoomGridDissolve(...)` 中新增三处 inline overlay：宽水平 `mortar veil`、非格点竖向 shadow veil、细 warm worn lip。
- 没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。
- focused test 初版锚点放在玩家格内部，实现在 seam 上绘制后 diagnostic run 显示实际目标已存在：`x=332.2, y=495.12, w=313.6, h=11.52, a=0.062`；随后把测试锚点修正到玩家附近长缝本身。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas lays broad mortar veils over long room lattice seams'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新 focused test 先 `BUILD FAILED in 3s`，失败信息为 `large visible rooms should lay a broad stone mortar veil across long horizontal seams so the center no longer reads as continuous debug lattice`。
- 实现与 seam 锚点修正后 focused test：`BUILD SUCCESSFUL in 2s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map renderer 变化失败 11 个 test group；失败范围与包含 map stage 的 golden surface 一致，`ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-*` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 后接受本轮方向：玩家附近长直地面缝被低透明跨格 stone/mortar 层打断，没有黑屏、翻转、裁切错误或右栏/底栏漂移。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 1s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f1670ea5a1363148314826a7812a24b781b185dfa26445a166d58e1fe6a3225a` |
| `ui-demo-new-parity-1280x800` | `b19108b9137ad06ab1100d795246085facd55095a6bbad926d69b36ff23f5e27` |
| `ui-demo-new-right-panel-grid` | `83c2c6f91e647631350de2255b1a713766bf31f74e144dc01b35581abe1fd674` |
| `ui-demo-new-bottom-deck-no-command-hints` | `17e0a444e24b0677fef4abc4c3ede2405a221f8760a2e58271d84c7ea740d978` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `22d3e5714d7d88d7d11f75528e7c0c0b4a882f92178dd14fcb20652ccd2583d2` |
| `ui-demo-new-inventory-page-1` | `cb04748b29786b0cbe0a1ddeed8792cd19d90cac3840d0747c33ce69269ba480` |
| `ui-demo-new-inventory-page-2` | `7d6e8246e3d0a8616460995fd00e94f926750bcfdea29cd72de9e1b1c8245834` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：最终重跑 `BUILD SUCCESSFUL in 1s`；impact plan 仍覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`。前一次非 up-to-date run 中 `verifyContractLintPreflight` 输出既有 `__stage_e_probe__` fallback warning，最终重跑该 task 为 UP-TO-DATE。

结论：

- 本轮是可接受的 shared map-grid presentation rebaseline：visible room 中心长缝不再只有等距黑线，新增 stone/mortar veil 让地面更接近跨格破损石材层。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理真正资源级石材重制、墙体/地面 hand-painted density、fog/lighting 压迫和 Phase C 全量 screen polish。

### 2026-05-26 torch glow restraint / cool stone pressure pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D map-stage lighting quality，处理 `UI-demo-new` 可见房间火把光仍偏 broad amber wash、暗石压力不足的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` shared torch lighting presentation。
- 设计判断：参考图的火把应是局部焦点，周围由冷暗石材和 fog pressure 托住；当前火把 tile glow 与 warm pool 重叠后仍显得像横向大块琥珀滤镜，本轮把火把半径/alpha/池形收紧。

实现记录：

- `drawTorchLightBlooms(...)`：火把 tile-by-tile glow 从 `radius = 5` 收到 `radius = 3`，前 4 个火把 max alpha 从 `0.16` 收到 `0.11`，后续火把从 `0.10` 收到 `0.065`。
- `drawTorchWarmPool(...)`：火把 warm pool max alpha 从 `0.075` 收到 `0.066`，外圈/中圈/核心尺寸分别收缩到 `4.20x2.55`、`3.25x2.00`、`2.55x1.65` cell units。
- 没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas keeps torch tile glow from becoming map-wide amber grid wash'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas gives player and torch compact local light pools'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas keeps torch tile glow from becoming map-wide amber grid wash' --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas gives player and torch compact local light pools'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新 focused test 先 `BUILD FAILED in 3s`：`torch tile glow should stay local; too many high-alpha 30px amber cells turn authored torch light into a map-wide grid wash, count=137`。
- 更新 compact pool 合同后也先 `BUILD FAILED in 2s`：`torch fixtures should cast a tight local warm pool so firelight reads as authored focal light rather than a broad amber rectangle`。
- 实现后两个 focused tests：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map torch lighting 变化失败 11 个 test group；失败范围与包含 map stage / torch-lit shared renderer 的 golden surface 一致，`ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-nav-rail-crop`、`ui-demo-new-inventory-page-*` 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 后接受本轮方向：火把仍保留局部焦点，左侧和中轴 amber wash 收窄，dark stone / fog pressure 更稳定，没有黑屏、翻转、裁切错误或右栏/底栏漂移。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 1s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `7e5f481e3b9df1f585b8a9310d3cc96b330bc4757183aa7b6dbb64205a1be0aa` |
| `ui-demo-new-parity-1280x800` | `5215b316590b596595e7b7fac0a2a2f6410dd8934371135c96370e11d3141001` |
| `ui-demo-new-right-panel-grid` | `83c2c6f91e647631350de2255b1a713766bf31f74e144dc01b35581abe1fd674` |
| `ui-demo-new-bottom-deck-no-command-hints` | `17e0a444e24b0677fef4abc4c3ede2405a221f8760a2e58271d84c7ea740d978` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `f95967e47f6f7b4f31fdb4a8174770063b692ef74c15ef74f62b8c3e1a91d1b1` |
| `ui-demo-new-inventory-page-1` | `cb04748b29786b0cbe0a1ddeed8792cd19d90cac3840d0747c33ce69269ba480` |
| `ui-demo-new-inventory-page-2` | `7d6e8246e3d0a8616460995fd00e94f926750bcfdea29cd72de9e1b1c8245834` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 shared torch lighting presentation rebaseline：火把不再以高 alpha tile glow 和宽 warm pool 把房间读成大面积琥珀格网，玩家/火把焦点保留，周围冷暗石材压力增强。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理真正资源级石材重制、墙体/地面 hand-painted density、雾层边界非矩形化和 Phase C 全量 screen polish。

### 2026-05-26 ragged fog teeth / non-rectangular hidden edge pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D map-stage atmosphere quality，处理 `UI-demo-new` 可见房间左右边界仍偏 rectangular cutout、暗部吞入感不足的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` shared hidden-stage presentation。
- 设计判断：参考图第一眼的黑暗边界不是直角矩形 veil，而是局部火光下被不规则暗雾吞没；当前房间边界仍过直，本轮只用窄暗雾齿 feather 可见房间左右边缘。

实现记录：

- `drawHiddenStageGridSuppression(...)`：在已有 left/right hidden-stage suppression 末尾追加左右不对称 fog teeth。
- 左侧新增 `0.196` alpha 主暗齿、`0.152` alpha 次暗齿和 `0.072` alpha 暖色裂纹，靠近 `visibleClip.x` 内外错位压入。
- 右侧新增 `0.172` alpha 主暗齿、`0.138` alpha 次暗齿和 `0.066` alpha 暖色裂纹，尺寸与位置不同于左侧，避免镜像矩形感。
- 没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas feathers hidden darkness into room silhouette with ragged fog teeth'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新 focused test 先 `BUILD FAILED in 3s`：`hidden darkness should send a narrow ragged tooth into the left room edge so the silhouette does not stay a clean rectangular cutout`。
- 实现后 focused test：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared hidden-edge renderer 变化失败 11 个 test group；失败范围与包含 map stage / hidden-stage shared renderer 的 golden surface 一致，未触达 map stage 的 crop 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 后接受本轮方向：房间左右边界出现更细碎的暗雾齿，暗部不再只像大矩形 veil，中心与 UI 面板没有被污染。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 1s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `b1abc0db9b877ee5ddd88de9e960e8a08a23c0460f9f65aaeace615d57e12c91` |
| `ui-demo-new-parity-1280x800` | `e0abc37363cd83503b2a5b572cd180043ab117d0444bc82bde3e041bac19ae4b` |
| `ui-demo-new-right-panel-grid` | `83c2c6f91e647631350de2255b1a713766bf31f74e144dc01b35581abe1fd674` |
| `ui-demo-new-bottom-deck-no-command-hints` | `17e0a444e24b0677fef4abc4c3ede2405a221f8760a2e58271d84c7ea740d978` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `935c3f8a7deb1786ac45daff23ab9fcf6c8c3b096d75c04dce7960fff25cb3ae` |
| `ui-demo-new-inventory-page-1` | `cb04748b29786b0cbe0a1ddeed8792cd19d90cac3840d0747c33ce69269ba480` |
| `ui-demo-new-inventory-page-2` | `7d6e8246e3d0a8616460995fd00e94f926750bcfdea29cd72de9e1b1c8245834` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- `git diff --check`：通过，无 whitespace error 输出。

结论：

- 本轮是可接受的 shared hidden-edge presentation rebaseline：暗部左右边界从干净矩形 cutout 变成不对称窄雾齿，地图舞台第一眼更接近参考图的压暗吞入感。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理真正资源级石材重制、墙体/地面 hand-painted density、fog shape 细节、右栏/底栏 surface cohesion 和 Phase C 全量 screen polish。

### 2026-05-26 hidden void chroma neutralization / moss veil restraint pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D map-stage atmosphere quality，处理首屏 hidden stage 左侧和下方仍有 moss-green rectangular veil 观感的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` shared hidden-stage presentation。
- 设计判断：参考图暗部第一眼更接近 void-black / cold stone pressure，而不是绿色幕布；本轮只收敛 hidden-stage 暗罩色相，不改 geometry、alpha、layer order 或业务状态。

实现记录：

- `drawHiddenStageGridSuppression(...)`：把基础 hidden suppression veil 从 `050604` 调整为 `05070A`，保留原有 alpha、geometry 和绘制顺序。
- `drawHiddenStageGridSuppression(...)`：把 secondary hidden-stage veil 从旧 moss tint `0B0E0B` 收敛到 cool-neutral `050607`，避免暗区读成绿色矩形 curtain。
- 新 focused test 初版做了 broad same-alpha color audit，后续被同 alpha 的 UI/stage/fire 非 hidden layer 证明过宽；最终收敛为禁止旧 `#0B0E0B` veil，并要求保留 `#050607` cool-neutral secondary darkness pass。
- 没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas keeps hidden stage secondary veils cool neutral instead of moss green'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新 focused test 初版先 `BUILD FAILED`：旧 moss tint `#0B0E0B` 仍出现在 hidden-stage secondary veil 中。
- 实现并收敛断言后 focused test：`BUILD SUCCESSFUL in 2s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared hidden-stage color drift 失败 11 个 test group；失败范围与包含 map stage / hidden-stage shared renderer 的 golden surface 一致。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 后接受本轮方向：首屏左侧和下方暗区的 green veil 感下降，房间从更中性的黑石暗部中浮出，没有黑屏、翻转、裁切错误或右栏/底栏漂移。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 1s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `eda84fa0232fbbef653db0801267a94efdc1ef76570c8c2248e86b263c1d8f78` |
| `ui-demo-new-parity-1280x800` | `d7f22bbdd85a656ae847a55e754648418934e93b909cdb64063e3bb90755e9b5` |
| `ui-demo-new-right-panel-grid` | `83c2c6f91e647631350de2255b1a713766bf31f74e144dc01b35581abe1fd674` |
| `ui-demo-new-bottom-deck-no-command-hints` | `17e0a444e24b0677fef4abc4c3ede2405a221f8760a2e58271d84c7ea740d978` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `5f1fe6642bbcb5b9b62a467981393c73d2c8e0d032e261caad94014c0cf6e7ae` |
| `ui-demo-new-inventory-page-1` | `cb04748b29786b0cbe0a1ddeed8792cd19d90cac3840d0747c33ce69269ba480` |
| `ui-demo-new-inventory-page-2` | `7d6e8246e3d0a8616460995fd00e94f926750bcfdea29cd72de9e1b1c8245834` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- `git diff --check`：通过，无 whitespace error 输出。

结论：

- 本轮是可接受的 shared hidden-stage chroma rebaseline：旧 moss-green veil 被冷中性暗罩替换，地图左侧和下方暗部更接近参考图的 void-black / cold-stone 压迫感。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级石材重制、墙体/地面 hand-painted density、洞口破形、右栏/底栏 surface cohesion 和 Phase C 全量 screen polish。

### 2026-05-26 operation hint caption / key-chip clearance pass

预检：

- 命中方向：`UI/goal` Phase C right-panel surface cohesion，处理 `ui-demo-new` 右栏底部 `操作提示` 仍有大号文本拥挤和 key chip 纵向压叠的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、输入命令、localization 文案、layout model、snapshot、save/replay/profile/schema、content-pack、manifest authority 或正式资源；只调整 `client` shared demo-shell operation hint presentation。
- 设计判断：参考图的操作提示是辅助扫描层；当前 `Ctrl+S`、`1-4`、`5-8` 等 key/label 仍偏大且行距过紧，容易压过装备、铭刻和背包的主信息层级。

实现记录：

- `drawCompactOperationCommandMatrix(...)`：key text 从 `TileTextStyle.SMALL` 降为 `TileTextStyle.CAPTION`，key width 计算同步使用 caption metrics。
- key chip 从 `17px` 高度收敛到 `13px`，在现有 15px row pitch 下保留 2px 视觉间隔，避免两列三行矩阵内的 chip 矩形互相压叠。
- label 文本使用 caption style 与更低 alpha 的 muted text，保留可读性但降低对右栏主内容的抢读。
- 没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.operation command matrix keeps caption hierarchy and non overlapping key chips'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新 focused test 初版先暴露测试 fixture 过窄：默认 snapshot 未覆盖完整 operation matrix；修正为带装备/铭刻的 PR02-1 shell fixture 后，目标 RED 落在 `operation shortcuts should render at caption hierarchy so the hint plate stays secondary to equipment, inscriptions, and backpack`。
- 实现后 focused test：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared shell operation hint typography / chip geometry 变化失败 11 个 test group；失败范围与包含右栏 operation hint plate 的 golden surface 一致。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受本轮方向：右栏提示仍可读，但从大号重叠文本块收敛为 caption-level 工具条，没有黑屏、翻转、裁切错误或地图/底栏污染。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 1s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `6193e2f56026152785567f1094a3ad9fa53469e45367e47fbdc049731673b7fb` |
| `ui-demo-new-parity-1280x800` | `a15ee9590ac40358a1aa28e2ddcdfda4da14138473360ed2d8e358f8bddff713` |
| `ui-demo-new-right-panel-grid` | `3c99ac60bcbf83acdf2d4a33444ad7ec05730bfc7941b3dfa4780ba69d77b564` |
| `ui-demo-new-bottom-deck-no-command-hints` | `17e0a444e24b0677fef4abc4c3ede2405a221f8760a2e58271d84c7ea740d978` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `5f1fe6642bbcb5b9b62a467981393c73d2c8e0d032e261caad94014c0cf6e7ae` |
| `ui-demo-new-inventory-page-1` | `75ef40092857b4c4ca20ba4e163627753e62d9027bb523653b51e4fd2b3b71e0` |
| `ui-demo-new-inventory-page-2` | `c5e7578603d69b48b18c662b72f1124aa7b38cc5831cb8de4f47bf66402e07ab` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 14s`；`clientSmoke` 中 `audio enabled formal path`、`render enabled tile path`、`audio enabled boss warning path` 保持 SKIPPED。
- `verifyChanged`：`BUILD SUCCESSFUL in 10s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- `git diff --check`：通过，无 whitespace error 输出。

结论：

- 本轮是可接受的 shared operation hint presentation rebaseline：右栏底部 command hints 从 `SMALL` 主信息层降为 caption 辅助层，key chip 不再互相压叠，操作可发现性保留但右栏主内容层级更稳。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、墙体高差、洞口破形、右栏/底栏全量 surface cohesion 和 Phase C screen polish。

### 2026-05-26 room-center high-relief stone facet pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D map-stage floor fidelity，处理 `ui-demo-new` 主房间中心仍偏 smoky tile lattice、石面高差不够清晰的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` shared map floor presentation。
- 设计判断：参考图主舞台中心有清晰的手工石面和局部高差；当前首屏中心仍有半透明雾层压在规则网格上的读数。本轮只在既有 painterly floor pass 内补中心石面 relief，不扩大为资源重生成或 layout 重排。

实现记录：

- `drawVisibleRoomPainterlyBreakup(...)`：新增一块 off-grid high-relief stone facet，alpha 0.118，用于压住中心长直 lattice 的第一读数。
- 同一位置新增短暗 undercut 和 restrained warm lip，让石面有局部高差和磨损边，而不是一块平面 veil。
- 没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas sharpens room center with high relief stone facets'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新 focused test 先 `BUILD FAILED`，失败点为 `visible room center should get a higher-relief off-grid stone facet so the focal floor reads as sharp masonry instead of smoky tile lattice`。
- 实现后 focused test：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map floor presentation 漂移失败 10 个 test group；`ui-demo-new` 只漂移 full/map crop，right panel、bottom deck、nav rail 与 inventory crop 保持稳定。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受本轮方向：中心石面高差和局部对比略升，没有黑屏、翻转、裁切错误或右栏/底栏污染。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1h 6m 49s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `09c5a3d9b20552614bd535cec33145d7c5d340cf7fffa3a6a81b3e96fa23d75d` |
| `ui-demo-new-parity-1280x800` | `94c94b553a0d56a62a901601660e96d8d7741e05cbdf91fc4f9aa01cca5d1996` |
| `ui-demo-new-right-panel-grid` | `3c99ac60bcbf83acdf2d4a33444ad7ec05730bfc7941b3dfa4780ba69d77b564` |
| `ui-demo-new-bottom-deck-no-command-hints` | `17e0a444e24b0677fef4abc4c3ede2405a221f8760a2e58271d84c7ea740d978` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `aaca52a5117366e88554afc0004bec0c7aaceff94bc7e47bf3012269ba65aebc` |
| `ui-demo-new-inventory-page-1` | `75ef40092857b4c4ca20ba4e163627753e62d9027bb523653b51e4fd2b3b71e0` |
| `ui-demo-new-inventory-page-2` | `c5e7578603d69b48b18c662b72f1124aa7b38cc5831cb8de4f47bf66402e07ab` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 22s`。
- `verifyChanged`：`BUILD SUCCESSFUL in 12s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 shared map floor presentation rebaseline：主房间中心获得更明确的 off-grid 高差石面、暗切口和磨损边，中心 floor 不再完全依赖低透明 veil 压网格。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、墙体高差、非矩形暗区压迫和 Phase C 全量 surface cohesion。

### 2026-05-26 fine aggregate stone grain pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D map-stage floor fidelity，处理 `ui-demo-new` 地面材质近看仍缺少细骨料颗粒、坑点和微小磨损的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` shared floor material presentation。
- 设计判断：参考图的主舞台地面有微小骨料、暗坑和磨损颗粒支撑材质密度；当前 renderer 已有大块石面与高差，但单格内仍偏平滑。本轮只在 `drawFloorMaterial(...)` 增加低 alpha micro grain，不扩大为资源重生成或 layout 重排。

实现记录：

- `drawFloorMaterial(...)`：每个 visible floor cell 在既有 etch lines 后新增两个 2x2 暗色 aggregate pits，alpha 分别为 0.036 与 0.030。
- 同一 cell 新增一条 6x1 restrained warm fleck，alpha 0.026，用于把细颗粒读成磨损石材而不是随机脏点。
- 坐标继续由现有 `material.variant` 决定，并在 cell 内 clamp；没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas adds fine aggregate stone grain to visible floor cells'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- focused test 初版先暴露测试本身的 `Float`/`Int` fixture 写法错误，修正后真实 RED 落在 `visible floor cells should carry multiple tiny dark aggregate pits so flat runtime floor cells gain material grain, not only broad smoky overlays`。
- 实现后 focused test：`BUILD SUCCESSFUL in 5s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared floor material presentation 漂移失败 11 个 test group；失败范围与复用地图/地表 renderer 的 golden surface 一致。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受本轮方向：地面微颗粒密度上升，没有黑屏、翻转、裁切错误或面板污染。
- 同步全量 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 4s`。

PR02-1 accepted evidence hash：

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `10dd71c249c09da2137929c106fc8236be3c88b5e9e3b3e6c5bf303695e3beb5` |
| `ui-demo-new-parity-1280x800` | `89f8e6de5e62f59572be8e2ddbde3bc9256291bb4967347d852b266813b44fce` |
| `ui-demo-new-right-panel-grid` | `3c99ac60bcbf83acdf2d4a33444ad7ec05730bfc7941b3dfa4780ba69d77b564` |
| `ui-demo-new-bottom-deck-no-command-hints` | `17e0a444e24b0677fef4abc4c3ede2405a221f8760a2e58271d84c7ea740d978` |
| `ui-demo-new-nav-rail-crop` | `c52f8db795b5e932c91fb5d5084fc113ea134f685f416ce08e7e8e89947d4f03` |
| `ui-demo-new-map-stage-crop` | `e274c55f2322bc18d2e872f1a5253feb5aec3f4d29ffaf70b806380e40c2abbc` |
| `ui-demo-new-inventory-page-1` | `75ef40092857b4c4ca20ba4e163627753e62d9027bb523653b51e4fd2b3b71e0` |
| `ui-demo-new-inventory-page-2` | `c5e7578603d69b48b18c662b72f1124aa7b38cc5831cb8de4f47bf66402e07ab` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 21s`。
- `verifyChanged`：`BUILD SUCCESSFUL in 11s`，最终文档记录更新后复跑 `BUILD SUCCESSFUL in 1s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- `git diff --check`：通过，无 whitespace error 输出。

结论：

- 本轮是可接受的 shared floor material rebaseline：visible floor cell 获得更密的暗色坑点和短 warm fleck，细颗粒层补足了此前大块 high-relief 石面之外的材质密度。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、墙体高差、非矩形洞口压迫、右栏/底栏 surface cohesion 和 Phase C 全量 screen polish。

### 2026-05-26 asymmetric passage throat shadow bite pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D map-stage architecture fidelity，处理窄通道 throat 仍偏对称矩形 cutout、墙体厚度和暗部压迫不足的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` shared passage-threshold presentation。
- 设计判断：参考图的洞口/通道像厚石墙中被凿出的 aperture；当前已有 jamb、threshold cap 和 broken lintel，但 throat 内部仍过于规整。本轮只在 `drawVisiblePassageThresholds(...)` 的 vertical passage 分支增加局部非对称暗部和磨损石唇。

实现记录：

- `drawVisiblePassageThresholds(...)`：vertical passage 在既有 side jamb 后新增一块 7x19 的 offset dark bite，alpha 0.236。
- 同一 throat 的对侧新增 6x15 shorter shadow bite，alpha 0.214，打破两侧完全对称的 slot 轮廓。
- throat 内新增一条 10x2 warm worn nick，alpha 0.121，让暗部压迫仍读成石材磨损而不是纯黑遮罩。
- 没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas gives narrow passage throats asymmetric shadow bites'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新 focused test 先 RED，失败点为 `narrow passage throats should have an offset dark bite on one jamb so the opening reads carved through thick masonry, not a symmetric rectangular slot`。
- 实现后 focused test：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared passage-threshold presentation 漂移失败 6 个 test group：PR01-1 viewport scroll、PR02 map evidence、outcome recap、sample pack runtime、PR03 shop surfaces、PR05 telegraph/combat surfaces。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受本轮方向：通道暗部压迫略升，没有黑屏、翻转、裁切错误或面板污染。
- 同步 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 7s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `dark-uiux-pr01-1-viewport-deadzone-scroll` | `87517549d109729f01831a1c3276f9e6a0e5cc8853a292327d51d155c738cd28` |
| `dark-uiux-pr02-round1-chrome` | `280a7ca50bbdd5aa5f3c802a6341fd265bd06c786b16b53f10332c4afcc4382c` |
| `dark-uiux-pr02-hud-icons-pilot` | `8441b00b65b6e8afa25520ab7eee9fc614e72caf101888c2fa67fe2223195890` |
| `sample-pack-runtime` | `c1d1053ec6a98c4b14d883583ae688eb3b9b8a8f20f4c4a767a5a51602cbc425` |
| `dark-uiux-pr03-inscription-shop` | `c784ed9a0ea11b9a89f28fd538077821bd4c521488b708557b091f7a2f219768` |
| `dark-uiux-pr03-shop-full-slot-replace` | `8788ed04e21de2c8d7f2c665c1cc21c76be3b578277cb82445e53f1c24dc98f2` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `b94b557dffd511f197a059a22be39e61fe2989d505c90993147754bd8abf72f1` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 22s`。
- `verifyChanged`：`BUILD SUCCESSFUL in 11s`，最终文档记录更新后复跑 `BUILD SUCCESSFUL in 1s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- `git diff --check`：通过，无 whitespace error 输出。

结论：

- 本轮是可接受的 shared passage-threshold presentation rebaseline：窄通道 throat 获得非对称暗咬边、对侧短 shadow bite 和小 warm worn nick，门洞比上一版更接近厚石墙 aperture。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、墙体高差、水平洞口破形、右栏/底栏 surface cohesion 和 Phase C 全量 screen polish。

### 2026-05-26 horizontal passage throat shadow bite pass

预检：

- 命中方向：`UI/goal` Phase B / Phase D map-stage architecture fidelity，处理上一轮 vertical throat polish 后，左右向通道洞口仍偏对称矩形 slot、缺少上/下沿破形的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` shared passage-threshold presentation。
- 设计判断：参考图的洞口/通道像厚石墙中被凿出的 aperture；上一轮只覆盖纵向窄通道，本轮补齐水平洞口，使侧向通道也有非对称暗部和磨损石唇。

实现记录：

- `drawVisiblePassageThresholds(...)`：horizontal passage 在既有 top/bottom threshold 后新增一块 19x7 的 offset dark bite，alpha 0.236。
- 同一 throat 的对侧新增 15x6 shorter shadow bite，alpha 0.214，打破上下沿完全对称的 slot 轮廓。
- throat 内新增一条 2x10 warm worn nick，alpha 0.121，让暗部压迫仍读成石材磨损而不是纯黑遮罩。
- 没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas gives horizontal passage throats asymmetric shadow bites'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新 focused test 先 RED，失败点为 `horizontal passage throats should have an offset dark bite on one lintel so side openings read carved through thick masonry, not a symmetric rectangular slot`。
- 实现后 focused test：`BUILD SUCCESSFUL in 5s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared horizontal passage-threshold presentation 漂移失败 8 个 test group：PR01-1 viewport/modal evidence、formal screens、boss warning、route midpoint、outcome recap、sample pack runtime、Phase4 UIUX PR03 item/ground loot、PR02-1 ui-demo-new parity/map crop。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1280x800.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受本轮方向：水平洞口暗部压迫略升，没有黑屏、翻转、裁切错误或面板污染。
- 同步 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 5s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `d9f9c6368a14de3a237d3d6f2265093bddfdb91f1c0018206f073346205ed463` |
| `ui-demo-new-parity-1280x800` | `869bd08b34530babbace268407cba3914d6f6fac0efc7adf04e1aeb0e5813657` |
| `ui-demo-new-map-stage-crop` | `ad828849025b908654ad7e9ccff173b3261b814e338d2a40d2d22a70e207e89e` |
| `dark-uiux-pr01-1-viewport-deadzone-still` | `577df5efd0981fcfb88531168457f8fa11b098aea811462d64a57fb29f35b3ab` |
| `dark-uiux-pr01-1-viewport-deadzone-scroll` | `9078a32826adc29e81eb7d0db61d63d19b302e8e219550bd94d1def8ed85dda5` |
| `boss-warning-en` | `a22dff80962916480fdee595a36aa20e3c7abb60bfec2053b9dac5777f058c7d` |
| `boss-warning-zh` | `83285f9557a17f9dad69b6676df490eb3af64206821176644a6b8f9fb5457bc8` |
| `sample-pack-runtime` | `51df08b21c1308808324b7a82bf18bbaade82318f8b1970cb3f0cf2b813d16ac` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 21s`。
- `verifyChanged`：`BUILD SUCCESSFUL in 11s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- `git diff --check`：通过，无 whitespace error 输出。

结论：

- 本轮是可接受的 shared horizontal passage-threshold presentation rebaseline：左右向通道 throat 获得非对称暗咬边、对侧短 shadow bite 和小 warm worn nick，水平洞口比上一版更接近厚石墙 aperture。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、墙体高差、右栏/底栏 surface cohesion 和 Phase C 全量 screen polish。

### 2026-05-26 right panel equipment forged rig cohesion pass

预检：

- 命中方向：`UI/goal` Phase C right-panel surface cohesion，处理装备区仍像浮动 slot 矩阵、缺少一体化锻铁装备架结构的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` shared right-panel equipment presentation。
- 设计判断：参考图右栏装备区像 paper-doll rack，slot 属于同一锻铁结构；当前已有 broad rig backdrop，但 slot 之间仍偏漂浮。本轮只在 `drawEquipmentRigBackdrop(...)` 内补低透明 rails / tie bars / rivets。

实现记录：

- `drawEquipmentRigBackdrop(...)`：基于现有 `DemoSlotGridLayout.slotBounds` 计算装备 slot 的列中心和行中心。
- 新增左右两条 3px vertical forged rail，alpha 0.118，并加一条极低亮度 warm edge，避免只读成黑线。
- 每一行 slot 中心新增 restrained horizontal tie bar，alpha 0.086，并以更暗的 worn edge 收敛为装备架结构。
- 每个 rail/row 交点新增 5x5 warm rivet 与 2x2 dark core，让大面积右栏顶部获得手工金属细节。
- 没有新增 helper、option matrix、业务 model 字段、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.right panel equipment rig connects sockets with forged rail structure'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 新 focused test 先 RED，失败点为 `equipment sockets should sit on paired forged rails so the section reads as one paper-doll rig, not isolated floating icons`。
- 初次实现后 focused test 仍失败在 horizontal tie bar，说明 tie bar 只跨列中心不足以形成装备架结构；随后把 tie bar 扩展到 slot 外沿范围。
- focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared right-panel equipment presentation 漂移失败 11 个 test group：PR01-1、PR02、PR02-1、formal screens、boss warning、route midpoint、outcome recap、sample pack runtime、Phase4 UIUX PR03 item/ground loot、PR03 equipment/inventory/shop、PR05 telegraph/combat。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png` 后接受本轮方向：新增 rails/rivets 是暗部结构，不抢文字和图标，不把右栏变成亮网格，装备区比上一版更像一体化 paper-doll rack。
- 同步 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 7s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `1be632d41aacc62074b63410e926c3d48d57d369e19bce522616f4cf50302007` |
| `ui-demo-new-parity-1280x800` | `55ac3234a9bf434023b7c3c301cdaca84bb6f1340b9763eb55928ff303e532d3` |
| `ui-demo-new-right-panel-grid` | `f866c58e4adb9d92e5273a803a840677a6bdc041700965b9e2c6f54c07d5b04e` |
| `ui-demo-new-inventory-page-1` | `036ba0b6bea9ded9d06e35109dc5f9d62207dfe6e81fda5b798ffdd7c419ba32` |
| `ui-demo-new-inventory-page-2` | `8f3392a0d05a04ba2d01b3645cdf5c729530c5f2b0a4392cfc7b5783b88bddfe` |
| `dark-uiux-pr03-equipment-slots` | `27bd7332e6942441ac32d5175c7fd2d33d8241b337ef81941b93a3f11025fb70` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `a5bb20e1ce9a8dc2198a591141275baec8d3f89db884ea5fc729e9ac45426e18` |
| `sample-pack-runtime` | `fe2a2eaa4e5afa160ad08dfb31c9bb07e906985f334ff2ee1d68f276fecef975` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 23s`。
- `verifyChanged`：`BUILD SUCCESSFUL in 11s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- `git diff --check`：通过，无 whitespace error 输出。

结论：

- 本轮是可接受的 shared right-panel equipment presentation rebaseline：装备 slot 获得成对 forged rail、restrained row tie bar 和 small worn rivet，右栏顶部比上一版更像一体化装备架。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理右栏 inscriptions/backpack 的 surface cohesion、底栏 command/action cohesion、资源级 wall/floor hand-painted density 和 Phase C screen polish。

### 2026-05-26 right panel inscription backpack forged utility rack pass

预检：

- 命中方向：`UI/goal` Phase C right-panel surface cohesion，继续处理装备架完成后，`铭刻栏` 仍像 detached list rows、`背包` 仍像 loose icon grid 的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入命令或 localization 文案；只调整 `client` shared right-panel utility-section presentation。
- 设计判断：参考图右栏中下段不是表格控件堆叠，而是厚重面板内的锻铁/皮革 utility rack。本轮只在现有 right-panel section 绘制内补低透明结构笔触，保持文字、图标和热键优先可读。

实现记录：

- `drawInscriptionSection(...)`：在每行 slot / 文本之前绘制贯穿多行的两列 ledger spine，alpha 0.112。
- 同一 ledger spine 的行中心增加 worn rivet，alpha 0.128，用于把 5-12 热键行收束成同一 rune rack。
- `renderRightPanel(...)` 的 backpack section 从通用 `drawSection(...)` 切到专用 `drawBackpackSection(...)`，但不改 layout 或 model。
- `drawBackpackSection(...)`：在 slot 之前绘制 pack tray backplate、row shelf rail 和两条 subtle strap，让背包格子读成同一托盘结构。
- 没有新增 helper-like business type、option matrix、compat path、second authority、临时资源或 schema。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.right panel inscriptions and backpack read as forged utility racks'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.DemoShellRendererTest'
```

结果：

- 初次 RED 因测试样本使用不存在的 `item.healing_potion.icon` 被 manifest fallback guard 拦截；修正 fixture 后，focused test 正确 RED 在 `inscription rows should sit on column ledger spines...`。
- 实现后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared right-panel utility-section presentation 漂移失败 11 个 test group：PR01-1、PR02、PR02-1、formal screens、boss warning、route midpoint、outcome recap、sample pack runtime、Phase4 UIUX PR03 item/ground loot、PR03 equipment/inventory/shop、PR05 telegraph/combat。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inventory-stacked.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受本轮方向：铭刻栏行组更像 forged rune rack，背包区更像 pack tray，没有黑屏、翻转、裁切错误或文字/图标遮挡。
- 同步 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 4s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `5fb05379d05f5f3ca89542f98b76a0cb5c660ba0030cf43a6ef643639315df7a` |
| `ui-demo-new-parity-1280x800` | `36ab57697cc73e46e2f700e399ad64dbd5d141dede3978af4e4e137bbaf4d8e6` |
| `ui-demo-new-right-panel-grid` | `a924fe933d7cbf1601937674f49866512bfe7e604e827a8dc20c0fd18dc56dad` |
| `ui-demo-new-inventory-page-1` | `2b4733efe3709bc6255bdac3d313e1f4245dcf21f1aa65235693a6d33227ccbe` |
| `ui-demo-new-inventory-page-2` | `85a44b85169695ffa0b65a3cc5d8d345897e0ba978dc97b2a2a424245a7e5a44` |
| `dark-uiux-pr03-inventory-stacked` | `ae383ee143305735cb7310a3e74490ae82e29a55b27048b774ea50265b97ecad` |
| `dark-uiux-pr03-inscription-shop` | `d8082d5e2ff28892e098aeb3cdc96caed041a4915c386a8d21ace55cad84e784` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `bb85a754e459eff244211f68495714e92444f6dba174cc1c6f5777dc09774dd7` |
| `sample-pack-runtime` | `0d2d32335a782fe54ae2b8cf56485ce8fd50739c1dfaf51594b2fa0bf6cae640` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 20s`；死代码清理和记录补写后复跑 `BUILD SUCCESSFUL in 22s`。
- `verifyChanged`：`BUILD SUCCESSFUL in 11s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- 删除本轮引入后不再使用的通用 `drawSection(...)` 死代码后，复跑 `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 7s`；复跑 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 5s`。
- 最终文档记录更新后复跑 `verifyChanged`：`BUILD SUCCESSFUL in 1s`。
- `git diff --check`：通过，无 whitespace error 输出。

结论：

- 本轮是可接受的 shared right-panel utility-section presentation rebaseline：铭刻栏获得贯穿列脊和铆点，背包区获得托盘、横向 shelf rail 与 subtle strap，中下段比上一版更接近参考图里的材质化工具架。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理底栏 command/action cohesion、右栏 operation hint 进一步精修、资源级 wall/floor hand-painted density 和 Phase C screen polish。

### 2026-05-26 left nav rail icon first forged command rail pass

预检：

- 命中方向：`UI/goal` Phase C first-glance shell polish，结合 `UI/review/open-design/ktome-dark-ui-design.md` 的 icon-first left navigation rail 要求，处理当前左侧 nav rail 过暗、图标过小、缺少锻铁结构的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoNavRailButtonLayout.kt`、`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` shared shell nav presentation。
- 设计判断：参考图左栏的价值是第一眼给出清晰的图标导航锚点。现状图标过小且 rail 背景近似空槽，本轮用更大的 slot/icon、贯穿 backbone、shelf bar 和选中态 halo 提升 icon-first 质感。

实现记录：

- `DemoNavRailButtonLayout.resolve(...)`：slot side 从窄内容宽度派生改成可在 56/64px left rail 内稳定落到 46-52px，图标 hit target 与视觉承托同步放大。
- `DemoShellRenderer.renderNavRail(...)`：在 nav button 绘制前增加 forged backbone、中央金属脊和每个按钮的横向 shelf bar。
- 新增 `drawNavButtonSocket(...)`：每个 nav item 获得暗底 socket、上下金属边、侧边压暗和 worn rivet；选中项额外获得 restrained cyan halo。
- 图标 inset 从固定 20% 改为 selected 11% / idle 13%，让图标读成主控而不是小装饰。
- 没有新增资源、manifest key、layout model 字段、option matrix、compat path 或 second authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.demo nav rail reads as icon first forged command rail"
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest"
```

结果：

- 初次 RED：`left nav icons should be large enough to read as primary icon-first controls at first glance`。
- 实现后第一次复跑剩余 shelf 宽度断言失败，修正为按 rail 居中计算后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。
- 最终格式整理后复跑 focused test：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared left-nav shell presentation 漂移失败 11 个 test group：PR01-1、PR02、PR02-1、formal screens、boss warning、route midpoint、outcome recap、sample pack runtime、Phase4 UIUX PR03 item/ground loot、PR03 equipment/inventory/shop、PR05 telegraph/combat。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-nav-rail-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png` 后接受本轮方向：左栏从近似空槽变成可读的 icon-first forged command rail，地图、右栏和底栏未被污染。
- 同步 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 5s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `67ee907281d26c536608c2fb34ff87a45895f3493e8769eae6e5ea09c0acf4cd` |
| `ui-demo-new-parity-1280x800` | `78aa800f76e859826f5064a27d529784fbfa2b0c378d10e7c39d43961c5610dc` |
| `ui-demo-new-nav-rail-crop` | `c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6` |
| `dark-uiux-pr02-round1-chrome` | `f110ae648ef3b8c8fd963aa8d422def2d9e333e134bf95a3431e6a2edfedf87e` |
| `dark-uiux-pr02-hud-icons-pilot` | `9c1ea539417004e7d5e69ede586eb297d4100054ddc91138e4b3189c32309886` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `c89b7568ba1afd78b0ce48e9dcd152850c989aac82f93d2aabde78f4afdf05ca` |
| `sample-pack-runtime` | `9a021194ded46f9851f62273e34ec5e29f2e4a2d01f27cf1bb67f9d77fc08680` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke`：`BUILD SUCCESSFUL in 21s`。
- `verifyChanged`：`BUILD SUCCESSFUL in 11s`。impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`；`verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。
- 最终格式整理后复跑 `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 1m 24s`，其中 `:client:clientSmoke`、`:client:goldenScreenshot`、`:tools:maintainabilityLint` 均重新执行通过；`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
- `git diff --check`：通过，无 whitespace error 输出。

结论：

- 本轮是可接受的 shared left-nav shell presentation rebaseline：左侧导航图标更大、更清晰，选中态和锻铁承托让它读成正式主控 rail，而不是空竖槽上的小图标。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理底栏 command/action cohesion、右栏 operation hint 进一步精修、资源级 wall/floor hand-painted density 和 Phase C screen polish。

### 2026-05-26 bottom action deck forged command console pass

预检：

- 命中方向：`UI/goal` Phase C bottom HUD / command surface cohesion，继续处理左栏与右栏 shell polish 后，底部 action deck 仍像独立 action card、缺少统一命令台结构的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、snapshot、save/replay/profile/schema、content-pack、manifest authority、输入 mode 语义、localization 文案或 action legality；只调整 `client` shared bottom action deck presentation。
- 设计判断：参考图底部不是几个漂浮按钮，而是角色信息、行动槽、日志三块被同一金属框架承托。本轮优先把 action slot 区收束成 forged command console，避免后续做 bottom log / hero stats polish 时继续围绕孤立 card 叠加装饰。

实现记录：

- `renderActionDeck(...)` 在 action deck surface 后、action slots 前增加 `drawActionCommandConsole(...)`。
- `drawActionCommandConsole(...)` 基于现有 `DemoShellLayout.actionSlotBounds` 推导 slot span，不引入新 layout model 或 second authority。
- command plinth 使用低透明暗底覆盖 action label 区，slot 下沿增加 warm hairline 与 restrained cyan glint。
- slot 底部绘制贯穿 forged rail，并为每个 visible slot 增加小 worn rivet 和暗芯，形成共享机械锚点。
- 没有新增资源、manifest key、配置项、option matrix、compat path 或临时路径。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action deck reads as one forged command console"
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.render.DemoShellRendererTest"
```

结果：

- 初次 RED：`action labels should sit on a shared command plinth...`。
- 第一次实现后继续 RED，暴露 plinth 高度不足；修正为固定 30px 上限后，继续暴露 span margin 过大。
- 将 command console span clamp 从 `deck.x + 26f` 收敛到 `deck.x + 12f` / `deck.right - 12f` 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared bottom action deck presentation 漂移失败 12 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、gameplay log emphasis、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 equipment/inventory/shop。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受本轮方向：bottom action deck 更像一体化 forged command console，slot labels 和 action icons 没有被遮挡，地图、右栏和弹窗没有黑屏、翻转、裁切错误。
- 同步 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 2s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `ca14f9cb1941d49670254a36750cf05065e6643ab032ced28a5fc1f021055c8f` |
| `ui-demo-new-parity-1280x800` | `8f7c5cd55f8466f3b648425e296b3a4af1febe5bcd2438e42295d32d131681ca` |
| `ui-demo-new-bottom-deck-no-command-hints` | `62e90b60023414f591523fe2c7b49ae1753e85090533661398febb9fe049029b` |
| `dark-uiux-pr02-round1-chrome` | `bd5d1e3a0cb72409b31ef0b73427d61b65a9f30e7f0fd6bf869a8a4b71cd03ba` |
| `dark-uiux-pr02-hud-icons-pilot` | `c50a3bee437e5ca8847d927ac31d46ab710ff9c9fdc54f69d8a804b9ddf90865` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `1479d467281fed0d782f22546e38b6f32f6bbcf98b95cb3dce6e99f5e8ee813c` |
| `sample-pack-runtime` | `e2dffcb8be037e183615520ef96493995229a395e8fd7ba122f69d782673ade6` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 21s`。
- impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`。
- `verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 shared bottom action deck presentation rebaseline：action slot 区获得共享 command plinth、贯穿 forged rail、restrained cyan glint 和 worn rivet，底部中段从独立按钮卡片更接近一体化命令台。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理 bottom log panel / hero stats 与 action console 的同层整合、右栏 operation hint 精修、资源级 wall/floor hand-painted density 和 Phase C screen polish。

### 2026-05-26 bottom hud shared forged foundation rail pass

预检：

- 命中方向：`UI/goal` Phase C bottom HUD surface cohesion，继续处理 action deck polish 后，英雄卡、action deck、log deck 仍像三张并排卡片而不是同一条 HUD 梁的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、snapshot、save/replay/profile/schema、content-pack、manifest authority、输入 mode 语义、localization 文案或 action/log 业务语义；只调整 `client` shared bottom HUD foundation presentation。
- 设计判断：参考图底部 HUD 是一条连续底梁承托角色、快捷动作和日志。本轮不重排布局，只在三段 panel 后方增加同一个 foundation slab、贯穿 top rail 和 gap connector posts，降低“并排卡片”观感。

实现记录：

- `renderShell(...)` 在 bottom hero/action/log section 绘制前调用 `drawBottomHudFoundation(...)`。
- `drawBottomHudFoundation(...)` 基于现有 `DemoBottomDeckLayout` 的 `heroCard`、`actionDeck`、`logDeck` 推导共享 span，不新增 layout model 或 second authority。
- foundation slab 使用低透明暗底覆盖三段底栏背后可见区域；top rail 横跨 hero/action/log；两个 panel gap 增加 restrained vertical connector post。
- 没有新增资源、manifest key、配置项、option matrix、compat path 或临时路径。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud panels sit on one forged foundation rail"
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.render.DemoShellRendererTest"
```

结果：

- 初次 RED：`hero, action, and log panels should sit on one shared dark foundation slab...`。
- 第一次实现后继续 RED，暴露 top rail 内缩过多，未真正贯穿三段底栏。
- 将 top rail 从内缩装饰改为跨 hero/action/log 的结构边后，focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared bottom HUD foundation presentation 漂移失败 12 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、gameplay log emphasis、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 equipment/inventory/shop。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受本轮方向：底栏三段的后方暗底和顶沿连接更明显，文本、血条、技能图标没有被遮挡，地图和右栏没有污染。
- 同步 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 3s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `3e0b3878961faa120260368df5567508e03e259966f165ca7f7f3103fc508899` |
| `ui-demo-new-parity-1280x800` | `670fa4f1bea8680046695e0e505dfbd2d049bcd643855d87352444c5f831f91e` |
| `ui-demo-new-bottom-deck-no-command-hints` | `218da936550232f133d95563cd7395dc574e7a4f6dc98ba83101debb43e28883` |
| `dark-uiux-pr02-round1-chrome` | `2f6fc6e4974b5aa28a82852e499f336eae9b5cc74f34b649c37b99ed8a219699` |
| `dark-uiux-pr02-hud-icons-pilot` | `1d6458cb03543b198d2ca302a089231616ec2c86d23707967d12ac7c02ae0eda` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `bf235609dc879ebb781853cef05773f412dbb671d513c1e2aff515a291c014b7` |
| `sample-pack-runtime` | `962a2de4d9c983f5acd7bef8f2b14548673e6dbf96d4fa00eec18c774809660c` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 22s`。
- impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`。
- `verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 shared bottom HUD foundation presentation rebaseline：英雄卡、action deck、log deck 背后获得共享 dark foundation slab、贯穿 top rail 和两处 connector post，底栏整体从三张卡片更接近一条连续 HUD 梁。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续处理 bottom log panel 的文字层级/滚动密度、右栏 operation hint 精修、资源级 wall/floor hand-painted density 和 Phase C screen polish。

### 2026-05-26 bottom log compact ledger / scroll density pass

预检：

- 命中方向：`UI/goal` Phase C bottom HUD polish。共享底梁完成后，bottom log panel 仍像带强 cyan 左条的窄文本盒， dense wrapped events 缺少事件流/滚动密度组织。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、snapshot、save/replay/profile/schema、content-pack、manifest authority、输入 mode 语义、localization 文案或 log event 业务语义；只调整 `client` bottom log presentation。
- 设计判断：参考图底部 log 区更像嵌在 HUD 梁里的事件账本，而不是独立 neon 文本盒。本轮不重排布局、不改文字，只用 left ledger ticks、right scroll-density spine 和 lower thumb marker 提升层级与可扫读性。

实现记录：

- `renderLogAndStats(...)` 先把可见 wrapped caption rows 收敛为 `visibleLines`，再统一绘制 ledger cues，最后绘制原有 caption 文本。
- `drawLogDeckSurface(...)` 将原先强 cyan full-height rail 收敛为暗色 forged spine + very restrained cyan core，并把 top edge 改为更温和的 warm rail。
- 新增 `drawLogLedgerGutter(...)` 基于现有 visible log rows 绘制 per-row compact ticks、right scroll spine、bottom thumb 和 lower fade，不新增 render model / layout model / second authority。
- 没有新增资源、manifest key、配置项、option matrix、compat path 或临时路径。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log renders compact ledger and scroll density cues"
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.render.DemoShellRendererTest"
```

结果：

- 初次 RED：`bottom log should render compact ledger ticks so dense wrapped events read as an event stream instead of a flat text block`。
- 实现 compact ledger / scroll-density cues 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 bottom log ledger presentation 漂移失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 equipment/inventory/shop。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-combat-action.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受本轮方向：log deck 更像事件账本，强 cyan 整条边被压低，文字仍为 caption 层级，没有遮挡地图、技能、右栏或 modal。
- 同步 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 4s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `2f05e32c4e9738407ed3f47517ef8bbbfd6767b80ab4e4eda2bc8d88751563f4` |
| `ui-demo-new-parity-1280x800` | `0d22efc949ed644df477a6191284125d9c51329122cd1f6404101d785bddb2be` |
| `ui-demo-new-bottom-deck-no-command-hints` | `fbb282fdd9e32821e3bd7b4d08e9df76b70f32d59e8c5bd625171ed72d646b4f` |
| `dark-uiux-pr02-round1-chrome` | `613b44bc151131753dde701c9a73815f5bddff3c76233a66fe10558b6eecde86` |
| `dark-uiux-pr02-hud-icons-pilot` | `49a15a7cd1aa80bfc35ac1a3a79be7c5d12f08b076e44ecd08ba7fee6aac6dfc` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `ab1bc81f5085c529c82a710ddfbab219a86534c41dd73cb1fabd20667e528780` |
| `sample-pack-runtime` | `a3757cd7c202c5654c7bf3403a3bc326269efe0a381dfcc5b3be4fea6aaaeaaa` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 22s`。
- impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`。
- `verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 bottom log compact ledger / scroll-density presentation rebaseline：log deck 获得 per-row ledger ticks、right scroll spine 和 lower thumb marker，强 cyan rail 被压低，wrapped log rows 更像一个可扫读事件流。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续右栏 operation hint 精修、资源级 wall/floor hand-painted density、hero stats 与 bottom log/action 的同层比例，以及 Phase C screen polish。

### 2026-05-26 right operation hint forged command dock pass

预检：

- 命中方向：`UI/goal` Phase C right-panel surface cohesion。右栏 equipment / inscription / backpack 已形成 forged rack/tray，但底部 `操作提示` 仍像贴在 commandHintPlate 上的松散 key chip matrix。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、snapshot、save/replay/profile/schema、content-pack、manifest authority、输入 mode 语义、localization 文案、layout model 或正式资源；只调整 shared demo-shell operation hint presentation。
- 设计判断：参考图的右侧辅助提示不应像工程文本矩阵。本轮保留已有 key/label 层级和低权重 caption，把它嵌进一块共享 forged command dock，用暗底、列间 rail 和 keyplate rivet 提升整体质感。

实现记录：

- `drawCompactOperationCommandMatrix(...)` 先计算 visible commands / occupied rows，再绘制共享 `drawOperationCommandDock(...)`，使两列快捷键坐在同一块暗底上。
- 新增列间 forged vertical rail 和 restrained top/bottom rail；这些都是 presentation-only rect，不新增模型字段、配置项、资源、manifest key 或 second authority。
- 每个 key chip 增加左右 worn rivet anchors，保留原 key chip alpha、shortcut caption style、muted label 分离和 non-overlap 约束。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.right operation hints sit on one forged command dock"
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.render.DemoShellRendererTest"
```

结果：

- 初次 RED：`right operation hints should sit on one shared dark command dock instead of reading as loose text chips`。
- 实现 command dock / vertical rail / keyplate rivet 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest` + `DemoShellRendererTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 right operation hint command dock presentation 漂移失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 equipment/inventory/shop。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inventory-stacked.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-combat-action.png` 后接受本轮方向：operation hint 更像嵌入式 utility rack，未遮挡右栏、底栏、地图或战斗日志。
- 同步 expected hash 后，`:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `2ff723f4c9c1168409b4af1a9c555b8b348eb1edf48627913d29ad9ac87147bb` |
| `ui-demo-new-parity-1280x800` | `89d9be2eb23413c036847f1c313878251f787fbcde8634a15d5dd0c0d9af92a9` |
| `ui-demo-new-right-panel-grid` | `78e47fd18094a9ab9f685dff8e0239bf50a60e3eb3135151769a13546d0cb0a1` |
| `ui-demo-new-inventory-page-1` | `70493104c9ac299e2c063366244ad4f635b4b8dd6e4286e5ee92e5bc223f73cc` |
| `ui-demo-new-inventory-page-2` | `e267f96b6c1fc84c9c9f282855f802c1ba2a316bad816d48ad6ffb697584867a` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `19d025ad770b1918ae7203d19cf69fb5aa38f1691131a55aca867e25a053693e` |
| `phase4-uiux-pr05-combat-action` | `ffb17c456b08b22b1714f0d0c59968b5394d67c5bc9e5b05653f09f76311f6aa` |
| `sample-pack-runtime` | `16cdd887352b12db5c3d5713483dceeafb6cb9b1ffdbbafd1a4b3acfb29b759b` |

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 16s`。
- impact plan 覆盖 `boss`、`client-ui-evidence`、`contractLint`、`dark-uiux-pipeline`、`keywordRegistry`、`longrun`、`maintainability`、`resource-pipeline`。
- `verifyContractLintPreflight` 仍输出既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 right operation hint forged command dock presentation rebaseline：operation hints 从松散 key-chip matrix 进一步收束为嵌入右栏的 forged utility rack，保留 keyboard-first 扫读和 caption 层级。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续资源级 wall/floor hand-painted density、hero stats 与 bottom log/action 的比例精修，以及 Phase C screen polish。

### 2026-05-26 wall-foot contact occlusion / masonry grit pass

预检：

- 命中方向：`UI/goal` Phase C map-stage material density。上一轮中心石面 relief 后，地图墙脚与地面接触仍偏干净，长墙下缘仍容易读成规则格线。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` map renderer 的 wall-foot presentation。
- 设计判断：参考图的墙体有明显压地重量和手绘 masonry grit。本轮只在既有 `drawVisibleWallFootRubble(...)` pass 中增加破碎接触暗带、侧墙窄 occlusion run 和 deterministic grit chips，避免新模型、新资源或第二 authority。

实现记录：

- 新增 `render canvas packs gritty contact occlusion under visible wall feet` focused renderer test，约束长墙脚 contact band、侧墙 contact run 和 wall-floor grit chips。
- `drawVisibleWallFootRubble(...)` 在 horizontal run 上把 contact shadow 压入相邻 floor cell，并补少量中部/边缘 grit chip；在 vertical run 上补左右侧墙的窄 occlusion strip。
- 实现只复用现有 visible wall / visible floor adjacency，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas packs gritty contact occlusion under visible wall feet'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`visible wall feet should press a broken dark contact band into the adjacent floor so wall mass reads as settled stone instead of a clean outline`。
- 实现 wall-foot contact band 后第二次失败在 grit chip 锚点；补中部 deterministic chip 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 第一次全量 `:client:goldenScreenshot` 在 PR05 第二个 LWJGL context 卡住；thread dump 显示 `Test worker` 停在 `GLFW.glfwInit()`，中断该次运行后未发现残留 Gradle worker。
- 更新 expected hash 前，非 LWJGL stable hash slice 因 shared map renderer 行为变化失败 10 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint。
- `dark-uiux-pr03 equipment/inventory/shop`、`gameplay log emphasis` 与 `phase4-v4-pr05 boss variant phase warning` 在本轮 slice 中保持通过。
- 同步 expected hash 后，同一组 non-LWJGL stable hash slice：`BUILD SUCCESSFUL in 50s`。
- 单独重跑 PR05 evidence：`BUILD SUCCESSFUL in 6s`。
- 全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 59s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f304c0bc54e3f57252ee686dffea13c0d55c44787fd67b9079ec88222bc8638f` |
| `ui-demo-new-parity-1280x800` | `a632472205a507d4c3349cd507c854762182d017cec24dbf06d8204d068d3925` |
| `ui-demo-new-map-stage-crop` | `f26bd725396f6ae30481206c3c7180040feb11b0d64906f45edb68bdfa224c8a` |
| `dark-uiux-pr02-round1-chrome` | `36d9757008607314db50315ec9e9180a8b1669f9fa6f7919aa566f9e679e1b05` |
| `dark-uiux-pr02-hud-icons-pilot` | `8ba5229ca9049629d4a95bc39a598a73b9ea703609f16660fb623a5065bc4b62` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `eab110f26d862919f696e42a22065574fd6fb958f0521e3c6ef40c377e662c73` |
| `phase4-uiux-pr05-combat-action` | `7beffbf139c36caea4b46adc773e52d8c2263be52ea12aa52aed7493b05386ce` |
| `sample-pack-runtime` | `ce81ce65ceeb46df73a65d3f5d4b316622c7de3c311a9fe16b40a90c6528de8f` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr05/dark-uiux-pr05-map-layer-stack.png` 与 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`。
- 接受方向：墙脚和侧墙边缘更有压地重量，部分干净格线被破碎暗带和 grit 打散；actor、loot、telegraph、right panel、bottom deck 与操作提示未被遮挡。
- 未关闭原因：整体地图仍偏雾化，墙体块面高差和手绘石材高频层级仍弱于参考图，洞口破形和 Phase C screen polish 仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 16s`。
- `verifyChanged` 覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过），本轮最终没有剩余 golden failure；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 wall-foot contact occlusion / masonry grit presentation rebaseline：地图墙脚接触从干净线条推进到更有重量的石墙边界，且未污染规则、资源、输入和右栏/底栏可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续资源级 wall/floor hand-painted density、墙体块面高差、洞口破形和 Phase C screen polish。

### 2026-05-26 wall block face relief / interior masonry shadow pass

预检：

- 命中方向：`UI/goal` Phase C map-stage material density。上一轮 wall-foot contact occlusion 后，墙脚更稳，但长墙立面仍偏雾化，墙体块面高差不足。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` map renderer 的 wall-face presentation。
- 设计判断：参考图的墙体由连续厚度、错缝块面和暗色 mortar return 共同建立高差。本轮只在既有 `drawVisibleWallRaisedFaces(...)` run-level pass 中补局部 raised stone plate / mortar return，避免新模型、新资源或第二 authority。

实现记录：

- 新增 `render canvas layers offset block faces inside visible wall masonry runs` focused renderer test，约束水平墙面错缝块、暗色竖向 mortar return 和侧墙纵向堆砌块。
- `drawVisibleWallRaisedFaces(...)` 在较长 horizontal visible wall run 上绘制 offset raised stone plates 与 dark mortar returns；在 vertical run 上绘制侧墙 stone plate 和对应暗缝。
- 实现只复用现有 visible wall / visible floor adjacency 和 viewport tile rect，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas layers offset block faces inside visible wall masonry runs'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`visible wall faces should include offset raised stone plates so long walls read as stacked masonry blocks rather than a single fog-softened strip`。
- 实现 wall-face raised plates / mortar returns 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map renderer 行为变化失败 10 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr05/dark-uiux-pr05-map-layer-stack.png` 与 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：墙体块面和暗缝更清楚，right panel、bottom deck、actor、loot 与 telegraph 没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `48bfdb1dff9a39462176e885cfd27b0acd7f2a394d30e2f3c16b422fac183e06` |
| `ui-demo-new-parity-1280x800` | `76987ef39967f2efd2a38bbd4abf813cb53ec727733052347bd644abec837fca` |
| `ui-demo-new-map-stage-crop` | `6419de9fc0af4cfca32dad27d6537fc6dea7e6f8292886d9262bfb0a913bec38` |
| `dark-uiux-pr02-round1-chrome` | `69edf327b0ed58ca35d0ec8b1eaaef0d9db4d81b27c2e13e035bb037a928e5de` |
| `dark-uiux-pr02-hud-icons-pilot` | `d8238fc94b0ae68125464a677960a86b55e5114f73abb1fc8f702709b9a39a93` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `13915f4afb2f89a1ee7cb1b1bdbcb8e7d9b222b38a15b9b844f94617bb01500e` |
| `phase4-uiux-pr05-combat-action` | `49c9f829809ac333a11c67a3bce68371e4869c91bf75a4f8c507fa847f666b35` |
| `sample-pack-runtime` | `fd17a7baad1532544313f4a61a0889de132bad34ff6bcbd57e93f811fd3d9f8b` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`。
- 接受方向：顶部、底部和侧墙出现更明确的错缝 stone plate 与暗色 mortar return，墙体高差比只靠整条 shadow band 时更强。
- 未关闭原因：整体地图仍偏雾化，地面 grid 仍能被第一眼读到；参考图的资源级 hand-painted density、洞口破形、暗区压迫和 Phase C surface cohesion 仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
git diff --check
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 16s`。
- `verifyChanged` 覆盖 `:client:goldenScreenshot`；本轮最终没有剩余 golden failure；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED。
- `git diff --check`：通过，无 whitespace error。

结论：

- 本轮是可接受的 wall block face relief / interior masonry shadow presentation rebaseline：地图墙体从雾化连续条带推进到更有堆砌块面和暗缝回折的 masonry boundary，且未污染规则、资源、输入和右栏/底栏可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续资源级 wall/floor hand-painted density、洞口破形、暗区压迫和 Phase C screen polish。

### 2026-05-26 focal cool undercut / stone clarity pass

预检：

- 命中方向：`UI/goal` Phase C map-stage material density。上一轮 wall-face relief 后，墙体块面更清楚，但英雄与火把附近仍偏暖雾覆盖 tile grid，焦点石面缺少冷色下切和局部清晰口袋。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` map renderer 的 focal-light presentation。
- 设计判断：参考图的焦点区域不是均匀暖雾，而是暖光、冷色压边和磨损石面共同形成清晰可玩的视觉口袋。本轮只复用既有 `drawFocalWarmStoneDropout(...)`，增加 compact cool undercut 与 worn stone lip，避免新增资源、配置或第二 authority。

实现记录：

- 新增 `render canvas carves cool focal clarity under player light` focused renderer test，约束 player light 下方的 cool undercut 与下缘 worn stone lip。
- `drawFocalWarmStoneDropout(...)` 在既有 warm stone dropout、grid seam dropout 和 vertical dark seam 基础上，增加一条窄冷色 undercut 与一条低透明 stone lip；player 使用 full alpha，torch 跟随既有 `alphaScale` 衰减。
- 实现只复用现有 focal light center、cell size 与 clip，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas carves cool focal clarity under player light'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`player focal light should carve a compact cool undercut below the warm pool so the hero area reads as clear stone instead of amber fog over a grid`。
- 实现 focal cool undercut / worn stone lip 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map renderer 行为变化失败 10 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/dark-uiux-pr05/dark-uiux-pr05-map-layer-stack.png` 与 `client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-round1-chrome.png` 后接受方向：英雄/火把焦点区石面更冷、更清楚，telegraph、right panel、bottom deck、actor 与 loot 没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `70234b39862a8feeac625c79b3851b71b27d14e15127b28c911d2cbe87e8c6e6` |
| `ui-demo-new-parity-1280x800` | `28133fbc6d872d1d2052367693f9ff5a0ee17722666bda1f3094477bd9c57696` |
| `ui-demo-new-map-stage-crop` | `d8449f4a18eecd55712739b21786a7043e9d16183d731566cbcf9ce66b5c1f2e` |
| `dark-uiux-pr02-round1-chrome` | `6f4783226cefef38603ff52e5f59cc7ebf1887d54db6c3a86d480528e32623e5` |
| `dark-uiux-pr02-hud-icons-pilot` | `5410bedfe3f958e170129db367cb0eb61761ce763b81f4c4b68beecc77afe4c6` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `b79f5a5db6b83721643f839b26222392e0804a9de899e47fda9fd92df2ca25f4` |
| `phase4-uiux-pr05-combat-action` | `0d33e830112e6286dc9c3bbef775e1fbcc1c7290c7520aa0f3fc1af8e69d62c5` |
| `sample-pack-runtime` | `28ddf20140a2c33018ff965e8f4324b397338e80f1d724de20d4c15ed8550ed3` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`。
- 接受方向：英雄和火把附近不再只是一团暖雾，局部冷色 undercut 和石面 lip 给焦点区域增加了清晰边界；telegraph、right panel、bottom deck、actor 与 loot 没有被遮挡。
- 未关闭原因：整体地图仍偏雾化，地面 grid 仍能被第一眼读到；参考图的洞口破形、暗区压迫、资源级 hand-painted density 和 Phase C screen polish 仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 16s`。
- `verifyChanged` 覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过），本轮最终没有剩余 golden failure；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 focal cool undercut / stone clarity presentation rebaseline：英雄与火把附近从暖雾覆盖 tile grid 推进到更清晰的石面焦点口袋，且未污染规则、资源、输入和右栏/底栏可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续整体地图雾化、地面 grid 可见度、洞口破形、暗区压迫和资源级 hand-painted density。

### 2026-05-26 hidden-stage asymmetric void collar pass

预检：

- 命中方向：`UI/goal` Phase C map-stage darkness / silhouette polish。上一轮 focal clarity 后，英雄与火把附近更清楚，但可见房间外侧仍能读到规则 hidden-stage grid，房间第一眼仍有“漂在背景格上”的问题。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、可见性语义、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` hidden-stage presentation。
- 设计判断：参考图的暗区不是均匀黑幕，也不是可读背景网格，而是围绕可见房间形成压迫性的非对称黑暗。当前已有 `drawHiddenStageGridSuppression(...)`，本轮复用同一 pass 增加 compact dark collar、void crown 与 worn-stone lip，不新增资源、配置、模型或第二 authority。

实现记录：

- 新增 `render canvas collars visible room with asymmetric void pressure` focused renderer test，约束 visible-room 左侧 dark collar、上缘 void crown、masonry lip 与 player focal center 不被覆盖。
- `drawHiddenStageGridSuppression(...)` 在已有 broad dark veil、void pocket、ragged fog teeth 基础上，新增 left compact collar、right/bottom soft collar、top/bottom asymmetric crown 和细窄 lip。
- 实现只复用 `visibleClip`、`mapBounds` 与 `cellSize`，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas collars visible room with asymmetric void pressure'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`hidden stage should press a compact dark collar against the visible room edge so the room reads carved from darkness instead of floating over a visible grid`。
- 实现 left collar / void crown / lip 后第一次 GREEN 前暴露一次本轮 patch 作用域错误，`topGap` 被误放进 `drawVisibleRoomAtmosphere(...)` 作用域，Kotlin 编译失败；修回 `drawHiddenStageGridSuppression(...)` 后继续验证。
- focused test GREEN：`BUILD SUCCESSFUL in 3s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map renderer 行为变化失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 shop/map-backed evidence。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：可见房间外缘更有暗区压迫，隐藏区 grid 不再同等抢眼，telegraph、right panel、bottom deck、actor、loot 与 shop modal 没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 1s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `343c13b7c95a9e3fa0a83f47cc5518f426c50feeded4d4015505c898481bbac5` |
| `ui-demo-new-parity-1280x800` | `13a57e46fe086c3959b1deee434aa4daa767653ac27519543a1d62ed829e334d` |
| `ui-demo-new-map-stage-crop` | `3f41d02fadf9cfca50c615e4e579a2ebbc76a0e4f26f56b25d7a6eb6d747d4c0` |
| `dark-uiux-pr02-round1-chrome` | `29599e1e83240721c6998ce57cdefaf44be8f99d6858ebc234e76dab6849f9e8` |
| `dark-uiux-pr02-hud-icons-pilot` | `ed53d8d2cc721aee5fa0ead0a365448cb52aaf2c2683f397697f6130da870b55` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `1b069a7e0ebbad20f92913d3336ce041d8c3c390501cb3aa8908e48bee4b9c0b` |
| `phase4-uiux-pr05-combat-action` | `ba27756d5c16fbfb44529cfdb73b17acfd4057343149b522828a37d84f295f44` |
| `sample-pack-runtime` | `6fc159cd6b3828f7da6c2871d5a443762e9848f2fe62caef459707d7ebc9de65` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`。
- 接受方向：可见房间边界更像被黑暗压住，左侧和上下 hidden-stage 网格感更弱；房间中心、actor、loot、telegraph 和 modal 仍可读。
- 未关闭原因：整体地图仍偏雾化，地面 grid 在可见区域仍偏强；参考图的洞口破形、墙地资源级 hand-painted density 和 Phase C screen polish 仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 16s`。
- `verifyChanged` 覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过），本轮最终没有剩余 golden failure；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 hidden-stage asymmetric void collar presentation rebaseline：可见房间从规则背景格中更明确地被暗区切出，且未污染规则、资源、输入和右栏/底栏可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续地面 grid 可见度、洞口破形、墙地资源级 hand-painted density 和 Phase C screen polish。

### 2026-05-26 visible floor heavy slab cap / lattice break pass

预检：

- 命中方向：`UI/goal` Phase C map-stage floor material density。上一轮 hidden-stage collar 后，房间外缘更有暗区压迫，但 visible-room 内部地面长横/竖 grid 仍能被第一眼读成 repeated tile lattice。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、actor/loot、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` visible floor presentation。
- 设计判断：参考图地面是可辨认 tile，但长格线被破碎石块、局部污痕和光影打散。当前已有 `drawVisibleRoomStaggeredStoneRhythm(...)`，本轮复用同一 pass 加重型横向 slab cap、竖向 broken slab cap 与窄 worn lip，不新增资源、配置、模型或第二 authority。

实现记录：

- 新增 `render canvas lays heavy broken slab caps across visible floor lattice` focused renderer test，约束 heavy horizontal cap、tall vertical cap、worn lip，并明确 heavy cap 不覆盖 player focal center。
- `drawVisibleRoomStaggeredStoneRhythm(...)` 在既有 staggered slabs 基础上新增 3 笔确定性 floor cap：一条跨格 horizontal cap、一条 vertical cap 和一条 narrow worn lip。
- 实现只复用 `left/bottom/width/height/cellSize`，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas lays heavy broken slab caps across visible floor lattice'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`visible floor lattice should be interrupted by a heavy off-grid horizontal slab cap so the room reads as authored stonework instead of a repeated tile grid`。
- 实现 heavy slab caps 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map renderer 行为变化失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 shop/map-backed evidence。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：中心地面长格线被更多跨格石板断开，actor、loot、telegraph、modal、right panel 与 bottom deck 没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 1s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `fc0c2fdecd39d678e164afad8fb76aa7bd95e4bfb75e4d85f61d1da41f3851c2` |
| `ui-demo-new-parity-1280x800` | `5f20b83375485294006e00966176d4a2f23b8ae6e97b1ceaa8184a07548dd768` |
| `ui-demo-new-map-stage-crop` | `0268be070da53232ecf128b8c2faabc3e5d2a59315f6f1fb9fff7828191983ff` |
| `dark-uiux-pr02-round1-chrome` | `546a98d55f0a76118b39529716a169d875f6d7273cf23c5f5fc6c27f6be1f468` |
| `dark-uiux-pr02-hud-icons-pilot` | `7ad03ff190a05947906274d893594489dcef27ae1ff57c0b2a6fa23176f03f57` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `c526fe52b1f7f6ad5eb1fba73cc86eb00da140ad740aff04fa7a6b4f750ac699` |
| `phase4-uiux-pr05-combat-action` | `341112b44627b8f45f00fccbb210ae254e38f9c3092f2ddcdd037787bc33851d` |
| `sample-pack-runtime` | `eb3051afcb8388199bb4b140fce2ec67d11f68d2de816d56fdf4181ca77182d5` |
| `dark-uiux-pr03-inscription-shop` | `7926c4f809c5e5c5f8c0ceb41c583bb2d05be88a33bc14b53f810dd8161834a4` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`。
- 接受方向：地面中心不再只靠低透明雾化压 grid，新增 slab cap 让部分长横/竖格线读成石板断面；telegraph、right panel、bottom deck、actor、loot 与 shop modal 没有被遮挡。
- 未关闭原因：参考图的资源级手绘地面密度、洞口破形、墙块高差和全屏 Phase C polish 仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 16s`。
- `verifyChanged` 覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过），本轮最终没有剩余 golden failure；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 visible floor heavy slab cap / lattice break presentation rebaseline：可见地面从 repeated tile lattice 进一步推进到 broken stonework 读法，且未污染规则、资源、输入和右栏/底栏可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续洞口破形、墙地资源级 hand-painted density、地面 tile asset 本体细节和 Phase C screen polish。

### 2026-05-26 visible wall chipped micro-joint / worn fleck pass

预检：

- 命中方向：`UI/goal` Phase C map-stage wall material density。上一轮 wall block face relief 后，长墙已有 raised plate 和 mortar return，但细尺度石缝与 worn fleck 密度仍不足，墙面近看仍偏平滑。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、actor/loot、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` visible wall presentation。
- 设计判断：参考图墙体不是只靠大块明暗读数，而是有大量低饱和石缝、磨损点和墙侧细裂。当前已有 `drawVisibleWallMasonryCourses(...)`，本轮复用同一 pass 补 compact chipped dark joints、warm pin flecks 与 side-wall vertical micro chips，不新增资源、配置、模型或第二 authority。

实现记录：

- 新增 `render canvas adds chipped micro joints across visible wall faces` focused renderer test，约束水平墙面的 compact dark joint、长墙 warm pin fleck 数量和侧墙 narrow vertical chip。
- `drawVisibleWallMasonryCourses(...)` 在既有 course stone / raised wall face 之间新增确定性小笔触：水平墙面 4x4 dark chip、6x2 warm fleck，侧墙 2x7 vertical chip。
- 实现只复用 wall/floor adjacency、viewport tile rect 和稳定坐标节奏，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas adds chipped micro joints across visible wall faces'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`visible wall faces should carry compact chipped dark joints so raised blocks read as hand-cut stones rather than smooth smoky bands`。
- 实现 micro joints / flecks 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 3s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map renderer 行为变化失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 shop/map-backed evidence。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：墙体边界有更多细碎石缝和 worn fleck，actor、loot、telegraph、modal、right panel 与 bottom deck 没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f5b5426268bbaefad0abdd40a813ece768dcbf436b3ab25ff25d1e935428382e` |
| `ui-demo-new-parity-1280x800` | `aef187467d549eea47aa22c2e974a340d4a2c4983b2a68c323945e0264ad3b05` |
| `ui-demo-new-map-stage-crop` | `21ae6b0404589efe67b718b763ad98c395b11863a7137cfc256cd6fc091f652e` |
| `dark-uiux-pr02-round1-chrome` | `b242d2a208a07e154e940d08300626a300847f05912c485b914974a26a79ecb1` |
| `dark-uiux-pr02-hud-icons-pilot` | `d1eb2c9affcdcf1068bcd986b9a701cdbdd262be1e31aa39872c58e4e11e6a6b` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `cad7b16d38f91d389f839f89bfd6b4c838f7c0fb03d7d505417fd2435fc9c249` |
| `phase4-uiux-pr05-combat-action` | `7300dfe2b8433529aa5292178865ab1db5a8f72537db1bb0a5fbdefac72d954f` |
| `sample-pack-runtime` | `0f5cea30e9766bbc3b68970f6476495561ea4ec0c9c39058f662932ea1a16cfa` |
| `dark-uiux-pr03-inscription-shop` | `862e691490316b65cf6b20073f1ae7e410fba12761eb59933859456044effbe6` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`。
- 接受方向：顶墙、底墙、侧墙的细碎暗缝和暖色磨损点增加，墙面不再只靠整条 band 和大块 raised plate 读数；telegraph、right panel、bottom deck、actor、loot 与 shop modal 没有被遮挡。
- 未关闭原因：整体地图仍偏雾化；参考图的地面资源本体细节、洞口破形、暗区压迫和 Phase C 全量 surface cohesion 仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 16s`。
- `verifyChanged` 覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过），本轮最终没有剩余 golden failure；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 visible wall chipped micro-joint / worn fleck presentation rebaseline：可见墙体从 broad masonry relief 继续推进到更细的 hand-built stone grain，且未污染规则、资源、输入和右栏/底栏可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续地面 tile asset 本体细节、洞口破形、暗区压迫和 Phase C screen polish。

### 2026-05-26 corridor mouth rubble teeth / aperture chip pass

预检：

- 命中方向：`UI/goal` Phase C map-stage aperture breakup。已有 corridor mouth lintel、recessed throat shadow、vertical/horizontal asymmetric bites，但当前截图里洞口边缘仍偏平滑，缺少参考图那种碎石咬边。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、actor/loot、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` passage threshold presentation。
- 设计判断：参考图的洞口不是干净矩形切口，而是由厚墙、碎石边和暗部共同形成。本轮复用 `drawVisiblePassageThresholds(...)` 的 existing wide mouth topology，补 compact rubble teeth 与 worn pin chip，不新增资源、配置、模型或第二 authority。

实现记录：

- 新增 `render canvas chips corridor mouth edges with rubble teeth` focused renderer test，约束 dark rubble tooth、opposite-jamb uneven tooth 和至少两处 warm worn pin chip。
- `drawVisiblePassageThresholds(...)` 在 `wideNorth / wideSouth / wideEast / wideWest` 分支内新增 mirrored rubble teeth / pin chip strokes，保持已有 vertical / horizontal passage 判定不变。
- 实现只复用 existing floor adjacency、viewport bounds 和 clipped rect drawing，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas chips corridor mouth edges with rubble teeth'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`corridor mouth edges should add compact dark rubble teeth so the aperture reads chipped out of stone rather than masked by a smooth rectangle`。
- 实现 rubble teeth / pin chips 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map passage presentation 行为变化失败 7 个 test group：boss warning、formal screens、PR02、PR02-1、PR01-1、Phase4 UIUX PR05 telegraph/combat、route midpoint。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 与 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：洞口边缘更碎，开口不再只靠平滑 rectangular lintel/shadow 读数，actor、loot、telegraph、right panel 与 bottom deck 没有被遮挡。
- 同步 expected hash 后第一次全量 golden 重跑在 PR05/LWJGL worker 无输出阶段挂起；终止该次 Gradle 后同一命令重跑：`BUILD SUCCESSFUL in 1m 4s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `5d336a88773c38182570154525a787a7cd828f14f511ca4a8d2487ade14ce61e` |
| `ui-demo-new-parity-1280x800` | `6af7c11e47ee00d3ce9e60be5a47ae3c63482b281c2a72c35b43dd8166ed9f31` |
| `ui-demo-new-map-stage-crop` | `a71b1556c96409084ca5ec4ac5ef83ba7138210dde74388dc256373b43b4a6ad` |
| `dark-uiux-pr02-round1-chrome` | `b099a252cf06d2e758d938230db8838f712ccc0e2c9ff89c54c55a1f14e5e656` |
| `dark-uiux-pr02-hud-icons-pilot` | `9578a470190572c512c6411450baece2163dd0882cedb5642534a87fc8f102d4` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `bb4cddcf3cadd1bfdd70dc4ffd6662bac252d2d4d1e31c0450493481a8b6054c` |
| `phase4-uiux-pr05-combat-action` | `a51b1e86c237eebda432d4397b288f9bf01caf7a260020cbca0dcfefac686833` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`。
- 接受方向：左侧、下侧和右侧 corridor mouth 的开口边缘更碎，洞口不再只靠平滑 lintel/shadow 读数；telegraph、right panel、bottom deck、actor 与 loot 没有被遮挡。
- 未关闭原因：整体地图仍偏雾化；参考图的地面资源本体细节、暗区压迫和 Phase C 全量 surface cohesion 仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 22s`。
- `verifyChanged` 覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过），本轮最终没有剩余 golden failure。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 corridor mouth rubble teeth / aperture chip presentation rebaseline：洞口边缘从 broad lintel/shadow 继续推进到更细的 broken aperture silhouette，且未污染规则、资源、输入和右栏/底栏可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续地面 tile asset 本体细节、暗区压迫和 Phase C screen polish。

### 2026-05-26 visible floor fracture kernel / asset-detail clarity pass

预检：

- 命中方向：`UI/goal` Phase C map-stage floor material density。正式 r02 floor asset 本体已经具备可用细节，但最新 `ui-demo-new` evidence 里主房间地面在 fog / torch wash / 多层 overlay 后仍偏雾化，cell 内部细节不够尖。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、actor/loot、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` floor material presentation。
- 设计判断：参考图地面不是靠单一大块雾面或长直 grid 读数，而是每个运行尺寸 tile 内部都有短裂、磨损和局部明暗芯。本轮复用 `drawFloorMaterial(...)` 的现有 `variant` 坐标节奏，补 compact fracture kernel，不新增资源、配置、模型或第二 authority。

实现记录：

- 新增 `render canvas sharpens visible floor cells with fracture kernels` focused renderer test，约束 center floor cell 内必须有 compact dark fracture kernel、short vertical cut 和 restrained worn edge。
- `drawFloorMaterial(...)` 在现有 aggregate grain 后新增三笔确定性小尺度细节：10x1.5 dark fracture、1.5x8 vertical cut、5x1.5 warm worn edge。
- 实现只复用 floor material `variant`、tile rect 和现有 `tileBounds` drawing，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas sharpens visible floor cells with fracture kernels'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`visible floor cells should carry compact dark fracture kernels so the tile body reads as sharpened stone rather than fog-softened texture`。
- 实现 fracture kernel 后 focused test GREEN：`BUILD SUCCESSFUL in 5s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared floor material renderer 行为变化失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 shop/map-backed evidence。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：地面 cell 内部短裂与磨损芯更清晰，actor、loot、telegraph、modal、right panel 与 bottom deck 没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 4s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `5d9cf8e2945f507334c089a1181ac0c35ff8e2586557cad382eac745719d65b8` |
| `ui-demo-new-parity-1280x800` | `b1f16f8fa737f5edef203ec5b06556dbe4b5a4e4b101411ed0bf49d5a7fb30b1` |
| `ui-demo-new-map-stage-crop` | `bc7f1c5ea7717cf98bcd220e13d05c6ed0fc78240b70eeccde73a90786b45b7e` |
| `dark-uiux-pr02-round1-chrome` | `43cdbb72f330de1c2cd14b72156526ef24dc93b8744bebbe3e167df6950e6a9c` |
| `dark-uiux-pr02-hud-icons-pilot` | `900551425a0f341a11312214531ad79f44e878ffbcaab6dd3a430fba131a1753` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `4c3d1cce0d6ab1c0a5d725e6dc34ea5381626b779d2a5f81b2913b1ed517fc43` |
| `phase4-uiux-pr05-combat-action` | `1126480ee55c73d801ff989afc3d7eab9f323ea147d3ed15d153e3942bacc10b` |
| `sample-pack-runtime` | `3dccb4c02d40b1ff9e99adfc96a7f827ad94f1bcaf59c7e307a376bd5cbf85f1` |
| `dark-uiux-pr03-inscription-shop` | `33ab13a98d4b104b01c53f99c932ed1b00d9afb30fe46be831322903727fa8da` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`。
- 接受方向：主房间 floor cell 内部比上一版更不容易糊成单一雾化格面，短裂和磨损芯提升了运行尺寸下的石材颗粒；telegraph、right panel、bottom deck、actor、loot 与 shop modal 没有被遮挡。
- 未关闭原因：整体地图仍偏雾化；参考图的非矩形暗区压迫、资源级手绘密度和 Phase C 全量 surface cohesion 仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 22s`。
- `verifyChanged` 覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过），本轮最终没有剩余 golden failure。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 visible floor fracture kernel / asset-detail clarity presentation rebaseline：地面 cell 内部从雾化 overlay 继续推进到更清晰的短裂和磨损细节，且未污染规则、资源、输入和右栏/底栏可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续非矩形暗区压迫、全量 surface cohesion 和资源级 floor/wall 手绘密度。

### 2026-05-26 hidden-stage staggered void cascade / backdrop grid breakup pass

预检：

- 命中方向：`UI/goal` Phase C map-stage darkness / surface cohesion。最新 `ui-demo-new` evidence 里 visible-room 周围左侧与底部 hidden stage 仍能读出大块规则矩形 grid，和参考图非均质黑场仍有差距。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、visibility 语义、actor/loot、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` hidden-stage backdrop presentation。
- 设计判断：参考图的暗区不是均匀黑色大矩形，也不是可数的背景格，而是多层错位、局部吃掉 grid 的黑场。本轮复用 `drawHiddenStageGridSuppression(...)` 现有 clip/gap 体系，只补两组确定性错位暗层，不新增资源、配置、模型或第二 authority。

实现记录：

- 新增 `render canvas breaks hidden stage grid with staggered void cascades` focused renderer test，约束 left hidden stage 必须有 staggered deep void cascade，lower hidden stage 必须有 cross-axis void shelf，且两者不能覆盖玩家中心。
- `drawHiddenStageGridSuppression(...)` 在基础 hidden-stage suppression 后新增左侧 `#010202 / 0.232` 深暗 cascade、底部 `#010202 / 0.218` cross-axis shelf，以及极短低 alpha stone lip。
- 初次人工看图后收窄 bottom stone lip，避免新的水平高光反向暴露矩形层。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks hidden stage grid with staggered void cascades'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`left hidden stage should receive a staggered deep void cascade so the backdrop stops reading as a regular rectangular grid`。
- 实现 staggered void cascade 后 focused test GREEN：`BUILD SUCCESSFUL in 5s`。
- 收窄 bottom stone lip 后 focused test 仍 GREEN：`BUILD SUCCESSFUL in 5s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared hidden-stage renderer 行为变化失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 shop/map-backed evidence。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：左侧与底部 hidden-stage grid 竞争继续降低，暗区更像错位黑场；actor、loot、telegraph、modal、right panel 与 bottom deck 没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 3s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `7b14d7fd97490f4ee61de51546edeab71713689fe1b01d0b185268bef0843c7d` |
| `ui-demo-new-parity-1280x800` | `0145abecb6052a9c124210650fc9ee10afb66432edcb1ae51eddc29a898e7647` |
| `ui-demo-new-map-stage-crop` | `28dc462afdaa57474bdeee6119292d0a651e77bdcd624450fea0ac52949c1efa` |
| `dark-uiux-pr02-round1-chrome` | `036bd9400d94ec72e0474a1644a05d879c64bb5721bacd2039f33bd7c86255d8` |
| `dark-uiux-pr02-hud-icons-pilot` | `e601efc8884f6334c62749c6c5725e4af6879c9fefb079e36d1380f63dbdaed6` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `10be13cbd1c3da1e0d16afef761b1c1bcabedf25d949a87967d82c66860bd919` |
| `phase4-uiux-pr05-combat-action` | `a7af289101f4cddec790c03bcd70d4318c3f0db25d7bacc6bcd0114357cbd9d5` |
| `sample-pack-runtime` | `208c10237965e0d65088a1f1da761e1a121dec9f651b49cda2fef10873dc87bf` |
| `dark-uiux-pr03-inscription-shop` | `197c4ef6fbeda30b239b10f7fafca299afda9a83c79584d293f03cb03558171d` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`。
- 接受方向：左侧隐藏区和底部隐藏区更暗、更错位，规则背景格不再同等抢眼；底部 stone lip 收窄后没有形成新的强直线；telegraph、right panel、bottom deck、actor、loot 与 shop modal 没有被遮挡。
- 未关闭原因：整体地图仍偏雾化；参考图的资源级手绘密度、右侧大黑场层次和 Phase C 全量 surface cohesion 仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 21s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:bossHarness`、`:tools:hiddenContentHarness`、`:tools:lootBalanceLab`、`:tools:whiteBoxMapgen`、`:tools:whiteBoxSolvability`、`:client:clientSmoke` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 hidden-stage staggered void cascade / backdrop grid breakup presentation rebaseline：左侧与底部隐藏舞台从规则矩形背景格继续推进到更有错位层次的黑场，且未污染规则、资源、输入和右栏/底栏可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续全屏 surface cohesion、资源级 floor/wall 手绘密度、右侧大黑场层次和参考图的整体材质锐度。

### 2026-05-26 right panel charcoal material skin / large black field depth pass

预检：

- 命中方向：`UI/goal` Phase C full-screen surface cohesion。最新 `ui-demo-new` evidence 里右侧 equipment / inscriptions / backpack / operation section 结构已经建立，但大面积黑色 section body 仍偏平涂，和参考图暗材质面的层次仍有差距。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、装备/铭文/背包数据、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` right-panel section surface presentation。
- 设计判断：参考图右栏不是纯黑容器，也不是亮 grid HUD，而是低对比的旧皮革/炭黑金属/污痕复合材质。本轮复用 `drawRightSectionSurface(...)` 统一 section body，不新增资源、配置、模型或第二 authority。

实现记录：

- 新增 `right panel large surfaces carry restrained charcoal material skin` focused renderer test，约束 equipment section body 必须有 broad charcoal grain，inscription section 必须有 subdued old-leather striation，backpack tray 必须有 tiny soot / worn-stone flecks。
- `drawRightSectionSurface(...)` 在 section body base 后新增三组低 alpha presentation 笔触：`#13100D / 0.052` 横向 charcoal grain、`#261A12 / 0.046` 纵向 old-leather striation、`#6E5630 / 0.038` tiny soot flecks。
- 实现只复用 right-panel section bounds 和现有 `tileBounds` drawing，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.right panel large surfaces carry restrained charcoal material skin'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`equipment section body should carry broad, low-contrast charcoal grain so the large black area reads as material instead of a flat void`。
- 实现材质层后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared right-panel surface renderer 行为变化失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 shop/map-backed evidence。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-1.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-equipment-slots.png` 后接受方向：右栏大黑面有更细的暗材质层，equipment rig、铭文行、背包 slot、操作提示与中文文本没有被压住。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 3s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `edb6462de58117b18f4e7113fed9d6778eb45a24f1e4d4476e9a732bdb1149b6` |
| `ui-demo-new-parity-1280x800` | `2a7580adce68810edd7d342ae76dad20b6f860161b793b17fc949d9c5aa10e03` |
| `ui-demo-new-right-panel-grid` | `15a0a99f1b01ea64f202aa856efb486c98512fec4312b84a126f3bbd17937511` |
| `ui-demo-new-inventory-page-1` | `ed4b96df12d7e3a73d1fd27a03af8e46ecb2ef23228a39f4843e8870d218de5d` |
| `ui-demo-new-inventory-page-2` | `0a0da5634ca570e6c91f9fa53c2a07164251412494d96f576986368a9df04e9e` |
| `dark-uiux-pr02-round1-chrome` | `fbe2e36b851ff3bd0d0a6222cf38bd57c2a59579311d64f909a0509e8487492c` |
| `dark-uiux-pr02-hud-icons-pilot` | `d7205ce22c4f15a527f21fb2788f8d6b3ca09ea28469451819724a6c1b54098f` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `746f2ddecde79be3e1a9c68ebdb61c7ecfa3cff1899fe6302d248afe34a93e90` |
| `phase4-uiux-pr05-combat-action` | `1d8ac5e6e9706d248dea80e429599700703d7b7c84e6a4aa5bc710c5b140c5d0` |
| `sample-pack-runtime` | `c5fefc6f4471e03086878187459e108f0dc48872ea2fc376f3c0e79b48263345` |
| `dark-uiux-pr03-equipment-slots` | `963ec12cb3799704b4a6a5dd9920db2257dc7fe5370b85af1bcbd7efab78db10` |
| `dark-uiux-pr03-inscription-shop` | `f5d251401822128f0e7d47b949db7bf008f35c23b2d623ae66b5ea3e3346f639` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`。
- 接受方向：右栏 section body 从纯黑平面推进到低对比 charcoal / old-leather / soot 材质层，保留了 dark fantasy 语气；slot icon、铭文热键、背包页码与 operation hints 仍可读。
- 未关闭原因：右栏 equipment 上半区 silhouette 仍可继续加强；全屏地图仍偏雾化，参考图的资源级手绘密度和第一眼焦点层次还需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 22s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过）、`:client:clientSmoke`、`:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:bossHarness`、`:tools:hiddenContentHarness`、`:tools:lootBalanceLab`、`:tools:whiteBoxMapgen`、`:tools:whiteBoxSolvability` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 right panel charcoal material skin / large black field depth presentation rebaseline：右侧大黑场从平涂容器继续推进到更有暗材质层的 UI surface，且未污染规则、资源、输入、地图、底栏或中文文本可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续右栏 equipment 上半区 silhouette richness、map-stage 雾化/锐度、资源级 floor/wall 手绘密度和全屏第一眼焦点层次。

### 2026-05-26 right panel equipment paper-doll scaffold / silhouette pass

预检：

- 命中方向：`UI/goal` Phase C right-panel silhouette richness。最新 `ui-demo-new` evidence 里 right-panel section body 已经有暗材质层，但 equipment 上半区仍偏“slot icon 漂在黑面上”，和参考图装备区的纸娃娃/装具架面板感仍有差距。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、装备/铭文/背包数据、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` equipment rig backdrop presentation。
- 设计判断：参考图装备区不是亮 grid，也不是完全空黑；它靠低对比底材、slot box 和人体/装具架轮廓让玩家一眼知道这是装备面板。本轮复用 `drawEquipmentRigBackdrop(...)` 的现有 rig/slot bounds，只补 restrained silhouette，不新增资源、配置、模型或第二 authority。

实现记录：

- 新增 `right panel equipment rig reads as an armored paper doll scaffold` focused renderer test，约束 equipment rig 必须有 central armored torso shadow、upper shoulder mantle 和 paired side armor plates。
- `drawEquipmentRigBackdrop(...)` 在现有 rig base 与 rails 之间新增三组低 alpha presentation 笔触：`#080604 / 0.168` central torso shadow、`#090604 / 0.154` shoulder mantle、`#11100C / 0.142` paired side plates，并保留极低 alpha gold/iron 细线作为结构读数。
- 实现只复用 equipment grid 的 slot bounds、rail bounds 和现有 `tileBounds` drawing，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.right panel equipment rig reads as an armored paper doll scaffold'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`equipment sockets should sit over a restrained central armored torso shadow so the upper right panel reads as a paper-doll scaffold, not icons floating on black`。
- 实现 silhouette pass 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared right-panel equipment rig renderer 行为变化失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 shop/map-backed evidence。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-equipment-slots.png` 后接受方向：equipment 上半区更像有纸娃娃装具架，slot/icon 没有被遮挡；铭文、背包、操作提示、地图与底栏读数不变。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 4s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `9ae31082e3530b009bd14f1ebc596556319b795c664a4d95bb23bafae7ad41dc` |
| `ui-demo-new-parity-1280x800` | `35e389c10b53b88cfedce8f7d91ca3454a14506cf1a3e6f6ab3f37f518dc698e` |
| `ui-demo-new-right-panel-grid` | `deef376cee573de28f031e9b46bb9233177a5ce172530f4db7cb172332856f08` |
| `ui-demo-new-inventory-page-1` | `e08fd25a5dc4f34956a5aef0ab912de088aacc25fbb4737b4c72f86e53367016` |
| `ui-demo-new-inventory-page-2` | `4af4e68cce885bbd821df2f29109404c99074fc161e09ae2187a25b0f4d0cc3c` |
| `dark-uiux-pr02-round1-chrome` | `ea9ad18493d27200568e9f787220291917b25f0228fcd5351c8e9f566d985709` |
| `dark-uiux-pr02-hud-icons-pilot` | `af57301043f9631bc222968ce01254a7a24919ca419fc85f171bbe704c47b7ea` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `3cc68cc653a97c8f36f0a35ad0377565ce754856a93afc60d976ed91e6548737` |
| `phase4-uiux-pr05-combat-action` | `3b317917dfba730993124111f7f2a708c1d339531c4f29ee9a55772b7a5da7db` |
| `sample-pack-runtime` | `52435098ae13e754241bb828c5224aa3029c1d8d5e73e4f58000a432cd25e57f` |
| `dark-uiux-pr03-equipment-slots` | `733d7da0c7c149b336377e2a727ebcf7d2c7bca8b746b68b96d043f86e00bbd8` |
| `dark-uiux-pr03-inscription-shop` | `ea019cb5a79a3de6e9d33a1f215049b0515ffd2bebb4c7c3c3d9348e427d12bd` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-equipment-slots.png`。
- 接受方向：equipment 上半区比上一轮更容易读成装具架/纸娃娃 surface，中央 torso shadow、shoulder mantle 和 side plates 都在 slot 后面，未压过 sword/shield/armor icon，也未影响铭文、背包、operation hints 或地图读数。
- 未关闭原因：地图仍偏雾化；右栏空 slot affordance、墙地资源级手绘密度和全屏第一眼焦点层次还需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 26s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过）、`:client:clientSmoke`、`:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:bossHarness`、`:tools:hiddenContentHarness`、`:tools:lootBalanceLab`、`:tools:whiteBoxMapgen`、`:tools:whiteBoxSolvability` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 right panel equipment paper-doll scaffold / silhouette presentation rebaseline：equipment 上半区从“图标漂浮”继续推进到更有装具架轮廓的暗 fantasy UI surface，且未污染规则、资源、输入、地图、底栏或中文文本可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续 map-stage 雾化/锐度、资源级 floor/wall 手绘密度、右栏空 slot affordance 和全屏第一眼焦点层次。

### 2026-05-26 map-stage tactical clarity plane / warm-haze cut pass

预检：

- 命中方向：`UI/goal` Phase C map-stage fog/sharpness。最新 `ui-demo-new` evidence 里 floor/wall、hidden-stage 和 right panel 已有多轮材质补强，但中心可玩区仍有一层暖色 haze 让石面局部对比偏软，和参考图主房间的第一眼清晰度仍有差距。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、visibility 语义、装备/铭文/背包数据、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案；只调整 `client` visible-room atmosphere presentation。
- 设计判断：参考图的优势不是更亮，而是中心 playable room 在黑场里有更清楚的冷暖层次和石面局部对比。本轮复用 existing map-stage visible-room bounds，只补 cool tactical clarity plane、dark undercut 和 worn lip，不新增资源、配置、模型或第二 authority。

实现记录：

- 新增 `render canvas cuts warm map haze with a cool tactical clarity plane` focused renderer test，约束 visible room focus 必须有 cool tactical clarity plane、compact dark undercut 和 restrained worn lip。
- 新增 `drawVisibleRoomTacticalClarityPlane(...)` presentation pass，并从 `drawVisibleRoomAtmosphere(...)` 串接；使用低透明度 `#07100D / 0.153`、`#050604 / 0.161` 和 `#A8905E / 0.061` 切出中心冷色清晰面。
- 实现只复用 visible-room bounds、cell size 与现有 `tileBounds` drawing，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas cuts warm map haze with a cool tactical clarity plane'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`visible room focus should cut the warm map haze with a cool tactical clarity plane so the center reads as stone, not amber fog`。
- 实现 tactical clarity pass 后 focused test 曾因采样点和房间坐标相差不到 1px 仍红；收紧落点后 focused test GREEN：`BUILD SUCCESSFUL in 7s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared map-stage renderer 行为变化失败 10 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr05/dark-uiux-pr05-map-layer-stack.png` 后接受方向：中心可玩区少了一层暖雾糊面，石面局部对比更清楚，actor、loot、telegraph、right panel 与 bottom deck 没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 7s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `005891d38a744b813e416e5ed21fe98548e31dea7ab53a3cbb593ce6357da772` |
| `ui-demo-new-parity-1280x800` | `9506c9f4a3766caaa1f44518201a69b48a496006e76265f22003da6dc02ab0b2` |
| `ui-demo-new-map-stage-crop` | `5b18d8ccde50b4f1017f9e5b27e0b3801d83c62b7fca53e306c70d9e7b60532d` |
| `dark-uiux-pr02-round1-chrome` | `f352275a37c0a78de6346af85c43a3afe6ad0c5684af4c1f771b0c0974bb61d6` |
| `dark-uiux-pr02-hud-icons-pilot` | `b3e9b1d0c9ccb09d1e7f4931999cc2642446ba21a55225ce3a9e0c35e77f2111` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `16a823a6f1f888c0d925a7e7b826e3118f97bfbd05a2aa11505fed55d55dbea0` |
| `phase4-uiux-pr05-combat-action` | `43e56e2fb99b0ed95bc026b47474b24572ea4556ff82d4c4bae4c6e7c88875cd` |
| `sample-pack-runtime` | `16a177069154ba8e05ce82420306cefa0eadd9fa40d16ffb0cc81189d7f8ff35` |
| `dark-uiux-pr05-map-layer-stack` | `353c0005baffc8aaeb529066e933a80c2d8e9d0a53e15f5c27d939fea41eefa8` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`。
- 接受方向：地图中心没有变亮成平面，也没有新增明显 UI 贴片；新增冷色 clarity 面把暖色 haze 压薄了一层，玩家/loot/telegraph 仍可读，右栏与底栏没有新遮挡。
- 未关闭原因：整体地图仍离参考图的资源级手绘密度有差距；右栏空 slot affordance 与全屏第一眼焦点层次仍需要继续推进。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 23s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过）、`:client:clientSmoke`、`:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:darkKeyRegistryLint`、`:tools:darkSpriteSheetLint`、`:tools:spriteSheetMapLint`、`:tools:darkManifestCoveragePr02OwnerScope`、`:tools:darkManifestCoveragePr02_1OwnerScope`、`:tools:darkManifestCoveragePr02_2OwnerScope`、`:tools:keywordRegistryLint`、`:tools:bossHarness`、`:game:longRunLab` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 map-stage tactical clarity plane / warm-haze cut presentation rebaseline：中心可玩区从偏暖雾化继续推进到更有冷暖层次和石面局部对比的 tactical surface，且未污染规则、资源、输入、右栏、底栏或中文文本可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续资源级 floor/wall 手绘密度、右栏空 slot affordance 和全屏第一眼焦点层次。

### 2026-05-26 right panel empty slot hollow socket / affordance pass

预检：

- 命中方向：`UI/goal` Phase C right-panel empty slot affordance。前序切片已增强右栏大面材质、equipment scaffold 和 map-stage clarity，但空装备、空铭文、空背包格在第一眼仍偏“黑色占位块”，和参考图里每个格子都像可交互容器的质感还有差距。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、装备/铭文/背包模型、地图生成、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案；只调整 `client` empty-slot presentation。
- 设计判断：空槽不应靠高亮或文字解释，而应靠内凹、压暗、磨损边和极少量金属 lip 读成“可放置目标”。本轮复用已有 slot bounds / frame / motif，不新增资源、配置、模型字段或第二 authority。

实现记录：

- 新增 `right panel empty slots read as hollow equipment sockets` focused renderer test，约束右侧 equipment / inscription / backpack 的 empty slot 必须有可检测的 hollow socket center。
- 在 `drawEmptySlotInterior(...)` 中为 empty slot 增加 `#020303` hollow socket center、top inner shadow、restrained lower worn lip、corner wear 和 side bite；保持原有 frame、glyph/motif 与 quantity/icon 渲染路径不变。
- 实现只复用现有 `GameShellBounds` 与 `tileBounds` drawing，不新增 layout/model 字段、manifest key、配置项或正式资源。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.right panel empty slots read as hollow equipment sockets'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`empty right-panel slots should have a visible hollow socket center so they read as intentional equipment targets, not flat black placeholders`。
- 实现 hollow socket pass 后 focused test GREEN：`BUILD SUCCESSFUL in 4s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared right-panel empty-slot renderer 行为变化失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 equipment/inventory/shop evidence。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inventory-empty.png` 后接受方向：空槽位有更明确的黑色内凹、边缘压暗和轻量金属磨损，读成可装备/可放置 socket；装备图标、铭文文本、背包项目、地图与底栏没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 8s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f3ad88a575bb7aaf082b57fe8dc649c21d5b0014458ba8268945448a9c15d019` |
| `ui-demo-new-parity-1280x800` | `757f1f7d2c3b0bfc21d6cc92ca77e841c7cabae71135177073766add9ca102a7` |
| `ui-demo-new-right-panel-grid` | `c8bcf38d64bfeb2648b5d26535dea2f7ee4220e024860846fb0159d3f544b5d7` |
| `ui-demo-new-inventory-page-1` | `e98b0311720183d954faa2f60731c332a5750bb10f1ffc358d9411c6b17ede8b` |
| `ui-demo-new-inventory-page-2` | `5acc29cdcb60cba7bd6225a13618a58c70b91c9117e36121b0cc431008515a9c` |
| `dark-uiux-pr02-round1-chrome` | `9551dbb495b874b9f0f1adf81ec2d975d7df1433dc8a9f32a10a0999e55f1a1e` |
| `dark-uiux-pr02-hud-icons-pilot` | `f69d214f62422a9bd585b47ab491013cd6971ec19a93b215b89247ab6a18659b` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `805f513f93fecac887009da9294eaa83df0de699e991ab0b168111fc51bd1d49` |
| `phase4-uiux-pr05-combat-action` | `ccc2ba10335a79d65266047f1d3ad1dd6a99cfab484a82783465a7d34cb40b96` |
| `sample-pack-runtime` | `925daa117385d3a8f8726248d561e69f08168767fd4816e11105e633d709c77a` |
| `dark-uiux-pr03-equipment-slots` | `8b5de22035be2042b4a95f92062e7a07f85fcc01fdec43a54b7b82f21675ec7b` |
| `dark-uiux-pr03-inventory-empty` | `8595d2fa14c996388f19381d5f36c02b63059f22191112c583a63f7815c7b6f0` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`：空装备位、空铭文位、空背包位不再只是黑方块，中心有明确凹陷和边缘压暗；filled item/icon 的优先级仍然更高。
- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`：右栏空槽强化没有抢走地图中心、底栏热键或英雄卡焦点。
- 查看 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inventory-empty.png`：空背包 grid 作为一个 tray 更像可放置容器，铭文空槽与 hotkey 文案仍清楚。
- 未关闭原因：整体地图仍需要资源级 floor/wall 手绘密度；全屏第一眼焦点层次和右栏整体层级还需要继续收敛。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 29s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过）、`:client:clientSmoke`、`:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:darkSpriteSheetLint`、`:tools:keywordRegistryLint`、`:tools:bossHarness`、`:tools:hiddenContentHarness`、`:tools:lootBalanceLab`、`:tools:whiteBoxMapgen`、`:tools:whiteBoxSolvability` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 right panel empty slot hollow socket / affordance presentation rebaseline：空槽位从“黑色占位”推进到更像可交互容器/装备目标的凹槽表面，且未污染规则、资源、输入、地图、底栏或中文文本可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续资源级 floor/wall 手绘密度、全屏第一眼焦点层次、map-stage resource-level clarity 和右栏整体层级收束。

### 2026-05-26 floor resource-scale chipped stone cluster pass

预检：

- 命中方向：`UI/goal` Phase C resource-level floor/wall hand-painted density。前序切片已经做过 floor aggregate、fracture kernel、wall masonry、洞口破形和 tactical clarity；当前剩余问题是 floor cell 内部在 32px 运行时尺寸下仍缺少更像手工石板的局部 chipped socket / stone fleck。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、visibility、actor、loot、输入、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案；只调整 `client` floor material presentation。
- 设计判断：新增细节必须落在每个 floor cell 自身，而不是大面积 overlay；深色小凹口提供形体，冷色石屑与暖色磨边提供材质层次，避免把地图推成单色噪声或恢复强网格。

实现记录：

- 新增 `render canvas adds resource-scale chipped stone clusters to visible floor cells` focused renderer test，约束可见 floor cell 必须出现 dark chipped-stone socket、cold stone fleck 与 restrained worn lip。
- 在 `drawFloorMaterial(...)` 中加入 deterministic chipped cluster：`#050604` 深色凹口、`#263D32` 冷色石屑、`#B69B6B` 磨损 lip；复用现有 `variant`、`tileBounds` 和 per-cell material path。
- 实现不新增资源、配置、layout/model 字段、manifest key、规则路径或第二 authority。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas adds resource-scale chipped stone clusters to visible floor cells'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`visible floor cells should include compact dark chipped-stone sockets that read at 32px runtime size`。
- 实现 chipped cluster 后 focused test GREEN：`BUILD SUCCESSFUL in 5s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 5s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared floor material 行为变化失败 11 个 test group：boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint、PR03 equipment/inventory/shop evidence。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 和 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：地面 chipped cluster 在 map stage 内可读，未遮挡 actor、loot、telegraph、right panel 或 bottom deck。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 10s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `72705f84aca1a005510ac8f4ccb1f4f3e55148ac7a1eb4f2bfcb010f749ae0c1` |
| `ui-demo-new-parity-1280x800` | `c814c5d2835b04a3bc0cddfab2b150c15e390b8c4a482855161aaa40709e7c08` |
| `ui-demo-new-map-stage-crop` | `dac03fa1f28d9c0ffe9eb85a528f780350b6c4f4f5aa665d76da427ce557312d` |
| `dark-uiux-pr02-round1-chrome` | `74afa536d2f4a260a9a0623c72e6ee6b3e4bce7f050727ae8169a6d4b0af3ad5` |
| `dark-uiux-pr02-hud-icons-pilot` | `c853399464976c09c804500dd88a23e382efba8f79e6bc5eec563696f03035d4` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `83b523644e97d2a1f167c49a322295c1d7f3f92044fd58f581dbf2927ada9fec` |
| `phase4-uiux-pr05-combat-action` | `cdee98b4d9eefd07608705710536435595c9ea036b33f527215a8e494f8a0baa` |
| `sample-pack-runtime` | `30c9aa33c63d871bb5df2d82cfa4a0fb212801062c7e548b9803ccfca282415f` |
| `dark-uiux-pr03-inscription-shop` | `bc237e3279364efe83bcd2a06cf02104d5447c0fde633da28b8f7afe36f7a339` |
| `dark-uiux-pr03-shop-full-slot-replace` | `f81e8a7dc9745adf11353cc709351c58942d28ca444b3c586cb7cd5b7aa00ea3` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`：可见 floor cell 内部有更清楚的局部 chipped socket / worn fleck，仍保持暗石调性，没有把网格线拉回 debug 状态。
- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`：首屏地图中心的资源级颗粒更稳，右栏、底栏、左栏、hero card 与日志未被污染。
- 查看 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`：telegraph / actor / loot 仍可读，没有因 floor material density 增加而被压糊。
- 未关闭原因：整体第一眼仍需要继续收敛 full-screen focal hierarchy、暗区压迫和右栏整体层级；本轮只关闭 floor resource micro-relief 的一个小切片。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 23s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过）、`:client:clientSmoke`、`:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:darkSpriteSheetLint`、`:tools:keywordRegistryLint`、`:tools:bossHarness`、`:tools:hiddenContentHarness`、`:tools:lootBalanceLab`、`:tools:whiteBoxMapgen`、`:tools:whiteBoxSolvability` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 floor resource-scale chipped stone cluster presentation rebaseline：地面从偏平的暗石面推进到更有手绘 chipped socket、冷色石屑和磨损 lip 的资源级材质，同时未污染规则、资源、输入、telegraph、右栏、底栏或中文文本可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续全屏第一眼焦点层次、map-stage resource-level clarity 的整体观感、右栏整体层级收束，以及必要时继续 wall/floor 资源级手绘密度。

### 2026-05-26 player focal bracket / first-glance anchor pass

预检：

- 命中方向：`UI/goal` Phase C full-screen first-glance focal hierarchy。前序切片已经补过 floor aggregate、fracture kernel、wall masonry、right panel socket、equipment silhouette 和 floor chipped cluster；当前剩余问题是玩家 tile 在整体暗场中仍更像普通 selection outline，缺少第一眼主角锚点。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、visibility、actor、loot、telegraph、输入、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案；只调整 `client` player indicator presentation。
- 设计判断：焦点强化必须落在 player indicator 自身，使用小尺寸 forged corner brackets 与 underfoot shelf 建立主角锚点；不能靠扩大金色选框、增加整格高亮或压低周边内容来抢焦点。

实现记录：

- 新增 `render canvas gives player tile forged focal brackets` focused renderer test，约束 player tile 必须出现四角 warm forged brackets 和 compact dark underfoot shelf。
- 在 `MAP_PLAYER_INDICATOR` 绘制路径中复用现有 `drawRectOutline`，增加 `#FFE18A` 低面积角部 bracket 与 `#050604` underfoot shelf；不新增资源、配置、layout/model 字段、manifest key、规则路径或第二 authority。
- focused RED 先证明旧 player indicator 没有四角 focal brackets，再以最小 presentation patch 收敛。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas gives player tile forged focal brackets'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`expected: <4> but was: <0>`，确认旧 player indicator 没有 four-corner forged bracket anchor。
- 实现 bracket / shelf 后 focused test GREEN：`BUILD SUCCESSFUL in 5s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared player indicator 像素变化失败，覆盖 boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat 与 route midpoint 等基线。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-round1-chrome.png` 后接受方向：玩家位置更容易第一眼定位，corner brackets 面积克制，actor、loot、telegraph、右栏、底栏与中文文本没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 4s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `e2e7eced75ca908bcfbd50c87ef7cfe2e8182694b1fc3ac86364448036fae63d` |
| `ui-demo-new-parity-1280x800` | `7409d10bdeaff3c7e473de9125ae52316f807587070111ccef3228d8af79dddd` |
| `ui-demo-new-map-stage-crop` | `507199fec1adcb520fd3bcc132b2859d04928e146dc65bcfd68c4eaae2575577` |
| `dark-uiux-pr02-round1-chrome` | `26599d926aa631a60f2f01580a63273d79b28706199eb815c15b18c96a232cf3` |
| `dark-uiux-pr02-hud-icons-pilot` | `3de292495c406cf5b828b0401ad157f262acd36f9b89eeb255741bcdf14e928f` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `5b53471f92daa0956dce27783a511abdb1d77358a23dbf2182e9e2f72e8ce4ad` |
| `phase4-uiux-pr05-combat-action` | `d410b517773dacad95f89ce9009f33fd2da86f55e9e6e8403b10bad20c654997` |
| `sample-pack-runtime` | `7d633cab9df95d3cef2879bd0731acb0d6830d28c14b72951e6c6cc7317e8f49` |
| `route-midpoint-960x720` | `8bccf54ea9c488b06a8bba771f156a404c70cc9f09980afa9b0126d7038d7dcf` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`：玩家位置在地图中心更快被识别为主角锚点，且没有把周边 floor chipped detail、wall、props 和 enemy/loot 层级压掉。
- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`：bracket 在 32px tile 内仍可读，但面积克制，没有恢复 debug grid 或形成过亮方框。
- 查看 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`：telegraph、actor、loot 与 player focal anchor 仍能区分，玩家强调没有抢走危险提示优先级。
- 未关闭原因：整体 UI/UX director-grade 目标仍需要继续收敛 full-screen focal hierarchy、map-stage resource-level clarity、右栏整体层级与必要的 wall/floor 手绘密度。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 23s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过）、`:client:clientSmoke`、`:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:darkSpriteSheetLint`、`:tools:keywordRegistryLint`、`:tools:bossHarness`、`:tools:hiddenContentHarness`、`:tools:lootBalanceLab`、`:tools:whiteBoxMapgen`、`:tools:whiteBoxSolvability` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 player focal bracket / first-glance anchor presentation rebaseline：玩家 tile 从普通 warm outline 推进到有明确 forged corner bracket 与 underfoot shelf 的主角焦点，同时未污染规则、资源、输入、telegraph、右栏、底栏或中文文本可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续全屏焦点层次的整体收束、map-stage resource-level clarity、右栏整体层级与必要的 wall/floor 手绘密度微调。

### 2026-05-26 hidden-stage neutral void ballast / focal cohesion pass

预检：

- 命中方向：`UI/goal` Phase C full-screen focal hierarchy / map-stage darkness。#49 后玩家锚点更清楚，但最新 `ui-demo-new` evidence 里可见房间外侧 left / bottom hidden-stage 仍有偏绿、偏规则的雾格，第一眼会削弱中心石室从黑场中浮出的质感。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、visibility、actor、loot、telegraph、输入、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案；只调整 `client` hidden-stage darkness presentation。
- 设计判断：这轮不继续加亮中心，也不恢复强 outline；用远端 neutral void ballast 把 left / bottom 大块规则雾格压回黑场，让中心石室、玩家和 telegraph 自然成为第一眼焦点。

实现记录：

- 新增 `render canvas anchors far hidden stage with neutral void ballast` focused renderer test，约束 far-left hidden stage 必须有 `#010101 / alpha=0.318` 高透明暗场，lower hidden stage 必须有 `#010101 / alpha=0.302` broad ballast，且不得覆盖 player focal center。
- 在 `drawHiddenStageGridSuppression(...)` 末尾增加两块 clipped neutral void ballast：远左纵向暗场和底部横向暗场；复用现有 `mapBounds / visibleClip / cellSize`，不新增资源、配置、layout/model 字段、manifest key、规则路径或第二 authority。
- focused RED 先证明旧 hidden-stage suppression 没有远端 neutral ballast，再以最小 presentation patch 收敛。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas anchors far hidden stage with neutral void ballast'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`far-left hidden stage should receive a high-opacity neutral void ballast so greenish stage fog recedes behind the authored room`，确认旧 hidden-stage presentation 没有该暗场 ballast。
- 实现 neutral void ballast 后 focused test GREEN：`BUILD SUCCESSFUL in 5s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 5s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared hidden-stage darkness 像素变化失败，覆盖 boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat、route midpoint 与 PR03 shop evidence 等基线。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 和 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：左/底 hidden-stage 更暗、更冷中性，中心房间仍可读；telegraph、actor、loot、right panel、bottom deck 与中文文本没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 5s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `324898a7997a53ad0cf9014e8360cbaa99cecff0bf275077e71c3ee9b3ed5b57` |
| `ui-demo-new-parity-1280x800` | `798623f68f990feb6bd12de53d9157d7a3055a8eb6a6e3c2899b72ec12192304` |
| `ui-demo-new-map-stage-crop` | `29d9331f97d9a410feb0c4ecd2e1bba3d062bb08790a2451d7c5cdc35331c192` |
| `dark-uiux-pr02-round1-chrome` | `327b6cd7b317c1d8bea5812b0314728aee554764da91c325764d018080e00c03` |
| `dark-uiux-pr02-hud-icons-pilot` | `0c206b710b06fd1d769a06873eab796a4791669e1f2b05f8e1447aba01dfa12f` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `d6399084dc683b4846d1312f47b34f3e1ecd9e5388792312d4bb5ce36037d845` |
| `phase4-uiux-pr05-combat-action` | `2aa33d1458a9469ac16c3e785e976a111dcfce80d9ba63e5c57ff203b9363bf8` |
| `sample-pack-runtime` | `f3c2b98ab1faf325372158efb48366fb05ba50fa110deddda4ac797a164a8c33` |
| `route-midpoint-960x720` | `94c8d5ba71e05fc46477b3b4bee84ff0d9330b868d99f08299d35195b5bfd287` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`：left / bottom hidden-stage 的规则雾格更退后，偏绿读数降低，中心石室不再像漂在同等亮度的背景 grid 上。
- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`：首屏焦点更集中在 map-room / player / action deck；right panel、nav、bottom HUD 没有被这轮 map-stage 暗场改动污染。
- 查看 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`：telegraph 与 actor 仍可读，暗场 ballast 没有覆盖危险提示或战斗日志。
- 未关闭原因：整体 UI/UX director-grade 目标仍需要继续收敛中心石室锐度、墙/地资源级手绘密度、右栏整体层级与更多非矩形暗区压迫。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 23s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过）、`:client:clientSmoke`、`:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:darkSpriteSheetLint`、`:tools:keywordRegistryLint`、`:tools:bossHarness`、`:tools:hiddenContentHarness`、`:tools:lootBalanceLab`、`:tools:whiteBoxMapgen`、`:tools:whiteBoxSolvability` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 hidden-stage neutral void ballast / focal cohesion presentation rebaseline：left / bottom 远端 hidden-stage 从偏绿、偏规则的雾格进一步退回冷中性黑场，中心石室与玩家焦点更集中，同时未污染规则、资源、输入、telegraph、右栏、底栏或中文文本可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续中心石室可读锐度、墙/地资源级手绘密度、右栏整体层级，以及必要时继续 map-stage 暗区非矩形压迫。

### 2026-05-26 visible room focal stone cutline / center sharpness pass

预检：

- 命中方向：`UI/goal` Phase C visible-room center sharpness / map-stage focal readability。#50 后 left / bottom hidden-stage 已退暗，但最新 `ui-demo-new` evidence 里中心石室地面仍偏软、偏雾化，缺少参考图那种手切石板的局部锐边。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、visibility、actor、loot、telegraph、输入、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案；只调整 `client` visible-room focus presentation。
- 设计判断：这轮不扩大整格高亮、不恢复完整 grid，也不继续压暗 hidden-stage；用几条低面积 cutline / bite / worn lip 给中心石板补局部锐度，让焦点区更像手工石板而不是暖雾平面。

实现记录：

- 新增 `render canvas etches focal stone cutlines into visible room center` focused renderer test，约束 visible-room focus 必须出现 short dark focal cutline、compact vertical bite 和 cool worn lip。
- 在 `drawVisibleRoomTacticalClarityPlane(...)` 末尾增加三条 restrained stone cutline：`#050604 / alpha=0.137` 横向暗切线、`#050604 / alpha=0.128` 竖向 bite、`#8EA38E / alpha=0.076` 冷色磨损 lip；复用现有 `left / bottom / width / height / cellSize`，不新增资源、配置、layout/model 字段、manifest key、规则路径或第二 authority。
- focused RED 先证明旧 visible-room center 没有这些 focal stone cutlines，再以最小 presentation patch 收敛。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas etches focal stone cutlines into visible room center'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`visible room center should receive a short dark focal cutline so the stone floor reads sharper than a soft fog plane`，确认旧 visible-room focus 没有该局部石板 cutline。
- 首次 GREEN 调整阶段发现测试坐标与实际绘制点不一致，debug candidates 为 `88.0x3.0@469.8,492.56/a=0.137`、`3.0x33.92@521.64,481.04/a=0.128`、`62.4x2.0@489.0,507.91998/a=0.076`；修正断言坐标后 focused test GREEN：`BUILD SUCCESSFUL in 3s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared visible-room focus sharpness 像素变化失败，覆盖 boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime、Phase4 UIUX PR05 telegraph/combat 与 route midpoint 等基线。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 和 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：中心地面多了克制的石板切线和冷色 worn lip，actor、loot、telegraph、right panel、bottom deck 与中文文本没有被遮挡。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 6s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `6d5351620af6286714f964b94327d6c0cc1d577260be7b286aa371b5ea40e643` |
| `ui-demo-new-parity-1280x800` | `a944f5a166a9e7859ceea35b3e07d87b402d178a552f7f01c7b85289b765b80e` |
| `ui-demo-new-map-stage-crop` | `6ca53e16b43ac5812401714505ef8feba69f0d77c86e9d5ce7fdd0c222366360` |
| `dark-uiux-pr02-round1-chrome` | `47abbdad3c3c1d947bc9d23f03f0a7f2931a409c5fbb68072177a1ad2df641fa` |
| `dark-uiux-pr02-hud-icons-pilot` | `ca7fdc5949756945374cb92e5a39d3f22582dfb5b029407c264a1bc8fc14fdc3` |
| `phase4-uiux-pr05-telegraph-triple-surface` | `fa1b440201e1b416606819562dcfdc00fbbadf0dc6568565efb1fd75e1b784f5` |
| `phase4-uiux-pr05-combat-action` | `218cad9865955a2f55b582080c5e0b54c7628eb48d861969f2387cf9c517c0ca` |
| `sample-pack-runtime` | `9ba243ff491815315175d665f58ff048e95b21f2af3fb659e726c63ff65e9386` |
| `route-midpoint-960x720` | `75392ee28283aab9c6b8711abf5917343823ec9e2b9537431a5bfc89ef9e5193` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`：visible-room center 的局部 cutline 更清楚，地面不再完全靠雾化色块表达；但整体仍需要继续提升到参考图级别的手绘石板密度。
- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`：中心房间锐度略有提升，right panel、nav、bottom HUD 没有被这轮 map-stage cutline 改动污染。
- 查看 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`：telegraph、actor、loot 与 player focal anchor 仍可区分，石板切线没有抢走危险提示优先级。
- 未关闭原因：整体 UI/UX director-grade 目标仍需要继续收敛中心石室手绘密度、墙体资源级锐度、右栏整体层级，以及更自然的非矩形暗区压迫。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 23s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过）、`:client:clientSmoke`、`:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:darkSpriteSheetLint`、`:tools:keywordRegistryLint`、`:tools:bossHarness`、`:tools:hiddenContentHarness`、`:tools:lootBalanceLab`、`:tools:whiteBoxMapgen`、`:tools:whiteBoxSolvability` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 visible room focal stone cutline / center sharpness presentation rebaseline：中心石室地面从软雾平面推进到带短暗切线、竖向 bite 和冷色 worn lip 的石板焦点，同时未污染规则、资源、输入、telegraph、右栏、底栏或中文文本可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续中心石室手绘密度、墙体资源级锐度、右栏整体层级，以及更自然的非矩形暗区压迫。

### 2026-05-26 visible wall broken capstone silhouette / wall sharpness pass

预检：

- 命中方向：`UI/goal` Phase C visible-wall resource sharpness / first-glance room silhouette。#51 后中心地面 cutline 更清楚，但最新 `ui-demo-new` evidence 里 visible wall crown 仍偏连续烟雾带，缺少参考图墙体的厚重、不均匀 capstone 块面。
- 触碰范围：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`、`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`UI/goal/goal.md`、`UI/goal/goalTest.md`。
- 合同边界：不改 `core/game` 规则、地图生成、visibility、actor、loot、telegraph、输入、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案；只调整 `client` visible-wall capstone presentation。
- 设计判断：这轮不继续加亮中心、不扩大整块墙体 haze、不恢复完整 grid；只在长墙 raised-face 分支增加少量 broken capstone、worn lip 和 vertical cleft，让墙体边界更像手工垒砌石块。

实现记录：

- 新增 `render canvas breaks long visible wall silhouette with heavy capstone slabs` focused renderer test，约束 long visible wall crown 必须出现 heavy broken capstone slab、short worn lip 和 dark vertical cleft。
- 在 `drawVisibleWallRaisedFaces(...)` 的 long horizontal wall branches 增加 `#2A3028 / alpha=0.183` 上墙 heavy capstone slab、`#A8905E / alpha=0.121` worn lip、`#050604 / alpha=0.157` vertical cleft，并在下墙分支补对应低位 capstone / cleft；复用现有 `cellSize / left / width / runLength`，不新增资源、配置、layout/model 字段、manifest key、规则路径或第二 authority。
- focused RED 先证明旧 visible-wall crown 没有 heavy broken capstone slab，再以最小 presentation patch 收敛。

focused renderer 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks long visible wall silhouette with heavy capstone slabs'
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
```

结果：

- 初次 RED：`long visible wall crowns should gain a heavy broken capstone slab so the wall silhouette reads hand-built instead of a smooth smoky strip`，确认旧 visible-wall crown 没有该 capstone slab。
- 实现 capstone / worn lip / cleft 后 focused test GREEN：`BUILD SUCCESSFUL in 5s`。
- `TileRendererCanvasTest`：`BUILD SUCCESSFUL in 4s`。

golden evidence 命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot
```

结果：

- 更新 expected hash 前，`:client:goldenScreenshot` 因 shared visible-wall capstone 像素变化失败，覆盖 boss warning、formal screens、Phase4 UIUX PR03 item/ground loot、outcome recap、PR02、PR02-1、PR01-1、sample pack runtime 与 route midpoint 等基线。
- 人工查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-round1-chrome.png` 和 `client/build/reports/golden/dark-uiux-pr01-1/dark-uiux-pr01-1-viewport-deadzone-still.png` 后接受方向：墙体 crown / foot 局部块面更重，right panel、bottom deck、nav、actor、loot、telegraph 与文本层没有被污染。
- 同步 expected hash 后，全量 `:client:goldenScreenshot`：`BUILD SUCCESSFUL in 1m 7s`。

accepted evidence hash 摘要：

| Label / Group | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f6f94cbb3a90df90e309d6e8cbefecf02f0d43c990e323ff5a1537188911aa78` |
| `ui-demo-new-parity-1280x800` | `4eab125f30a3ad2200d34c67ae17e1e7525bb5bf603e06702551363743dda532` |
| `ui-demo-new-map-stage-crop` | `070f834b41a5db14c103a46d23d47da2662afc33400c0fda75c761ef5a9fa84d` |
| `dark-uiux-pr02-round1-chrome` | `b060821dfd2a02c0c08e1e07f5dae095f3f5279de514f9e453a8e7076c5b8792` |
| `dark-uiux-pr02-hud-icons-pilot` | `a5c573f01a46f4ef0851ee9c017b3252d8a94213b651607dd85efdb60c86d383` |
| `sample-pack-runtime` | `73151a4fabc21435659bf6b13b5a7d851eee99159dafec5623779d40b7910782` |
| `route-midpoint-960x720` | `bc017e7681f51a163050027ea5a80b8ee23d2008a6a806a7c37e5e1be9a85228` |

manual director check：

- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`：visible wall crown 的块面更明显，长墙不再只依赖连续 smoky strip；但整体仍需要继续向参考图的资源级手绘密度推进。
- 查看 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`：房间边界略更厚重，right panel、nav、bottom HUD 没有被这轮 map-stage capstone 改动污染。
- 查看 `client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-round1-chrome.png` 和 `client/build/reports/golden/dark-uiux-pr01-1/dark-uiux-pr01-1-viewport-deadzone-still.png`：英文/基础 viewport evidence 中墙体 silhouette 同步增强，文本和操作面板仍可读。
- 未关闭原因：整体 UI/UX director-grade 目标仍需要继续收敛中心石室手绘密度、墙体/地面资源级材质密度、右栏整体层级，以及更自然的非矩形暗区压迫。

owner gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint :client:clientSmoke verifyChanged
```

结果：

- `maintainabilityLint :client:clientSmoke verifyChanged`：`BUILD SUCCESSFUL in 25s`。
- 本次 `verifyChanged` 因当前工作区已有多模块 UI/资源变更，实际覆盖 `:client:goldenScreenshot`（本次为 up-to-date，前置全量 golden 已实跑通过）、`:client:clientSmoke`、`:tools:resourcePipelineLint`、`:tools:verifyContractLintPreflight`、`:tools:darkSpriteSheetLint`、`:tools:keywordRegistryLint`、`:tools:bossHarness`、`:tools:hiddenContentHarness`、`:tools:lootBalanceLab`、`:tools:whiteBoxMapgen`、`:tools:whiteBoxSolvability` 等 owner/preflight 入口。
- `verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。

结论：

- 本轮是可接受的 visible wall broken capstone silhouette / wall sharpness presentation rebaseline：visible wall crown / foot 从连续软带推进到有 heavy capstone slab、worn lip 和 vertical cleft 的石墙切面，同时未污染规则、资源、输入、telegraph、右栏、底栏或文本可读性。
- UI/UX director-grade 目标仍未关闭。下一轮优先继续中心石室手绘密度、墙体/地面资源级材质密度、右栏整体层级，以及更自然的非矩形暗区压迫。

# Dark UI/UX PR-02 UI Chrome Sprite Pilot

**阶段**: `dark-uiux-pr02-ui-chrome-sprite-pilot`
**优先级**: `P0`
**工作量**: `L`
**前置条件**: PR-00、PR-01 完成。
**资源生成结论**: 生成 Round 1 最小可运行组：`r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons`。

## 1. 阶段目标

1. 跑通第一组正式 sheet：`r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons`。
2. 交付 `raw sheet -> sliced PNG -> processed PNG -> visual-manifest -> golden` 完整闭环。
3. 新增 UI chrome / HUD key，并让 client 实际消费 panel/slot/modal/HUD 的最小可运行子集。

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
| `client` renderer | 消费新 key，保留 fallback |

## 3. Round 1 Sheet 内容

1. `r01-ui-chrome`: `large-sheet 1024x1024 / 4x4 / 256x256`，包含 panel、corner、edge、tooltip、modal、slot frame。
2. `r01-ui-controls`: `icon-sheet 1024x1024 / 8x8 / 128x128`，包含 tab、button、empty state、lock、invalid、target、selection marker、backpack、equipment、combat action、talent state glyph。
3. `r01-ui-hud-icons`: `icon-sheet 1024x1024 / 8x8 / 128x128`，包含 health、stamina、xp、gold、key、quest marker、log marker、warning。
4. 切分后的 runtime canvas 由 category policy 决定；HUD 显示 `32x32`、装备 slot 显示 `48x48/64x64` 是 renderer/layout 决策，不是 source sheet cell 尺寸。
5. 每个 cell 必须在 `sheet-plan.yaml` 写明 `targetKey` 和 `outputName`。
6. contact sheet 必须显示 `row,col,targetKey`，但 runtime PNG 不能烘焙文字。

Raw sheet 生成交接：

1. 先按 PR-00 固定命令生成 `001-r01-ui-chrome.prompt.txt`、`002-r01-ui-controls.prompt.txt`、`003-r01-ui-hud-icons.prompt.txt` 和 `prompt-index.json`。
2. lky 在 Codex app 中逐个执行 prompt。
3. 生成图必须分别放到 `assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png`、`r01-ui-controls.png`、`r01-ui-hud-icons.png`。
4. raw PNG 放置完成后才运行切分、contact sheet 和 manifest coverage gate。
5. PR 描述必须列出 prompt path、raw sheet path、raw sheet hash 和 contact sheet QA path。

## 4. UI Key Registry 初始清单

| targetKey | Category | Sheet | fallbackKey | Consumer | consumerTest |
| --- | --- | --- | --- | --- | --- |
| `ui.frame.panel.body` | `ui_frame` | `r01-ui-chrome` | `missing_visual` | `TileRenderer` shell panels | `TileRendererCanvasTest` / `dark-uiux-pr02-round1-chrome` |
| `ui.frame.panel.corner_tl` / `tr` / `bl` / `br` | `ui_frame` | `r01-ui-chrome` | `ui.frame.panel.body` | panel nine-slice | `TileRendererCanvasTest` |
| `ui.frame.panel.edge_top` / `right` / `bottom` / `left` | `ui_frame` | `r01-ui-chrome` | `ui.frame.panel.body` | panel nine-slice | `TileRendererCanvasTest` |
| `ui.frame.slot.empty` | `ui_frame` | `r01-ui-chrome` | `ui.frame.panel.body` | inventory/equipment/talent slots | `TileRendererCanvasTest` |
| `ui.frame.slot.equipped` | `ui_frame` | `r01-ui-chrome` | `ui.frame.slot.empty` | equipment slots | `TileRendererCanvasTest` |
| `ui.frame.slot.selected` | `ui_frame` | `r01-ui-chrome` | `ui.frame.slot.empty` | focused slot | `TileRendererCanvasTest` |
| `ui.frame.tooltip.body` | `ui_frame` | `r01-ui-chrome` | `ui.frame.panel.body` | tooltip layer | `TileRendererCanvasTest` |
| `ui.frame.modal.body` | `ui_frame` | `r01-ui-chrome` | `ui.frame.panel.body` | modal layer | `TileRendererCanvasTest` |
| `ui.hud.hp.icon` | `icon` | `r01-ui-hud-icons` | `missing_visual` | bottom HUD | `ManifestResolveTest` / `dark-uiux-pr02-hud-icons-pilot` |
| `ui.hud.stamina.icon` | `icon` | `r01-ui-hud-icons` | `missing_visual` | bottom HUD | `ManifestResolveTest` / `dark-uiux-pr02-hud-icons-pilot` |
| `ui.hud.xp.icon` | `icon` | `r01-ui-hud-icons` | `missing_visual` | bottom HUD | `ManifestResolveTest` / `dark-uiux-pr02-hud-icons-pilot` |
| `ui.hud.gold.icon` | `icon` | `r01-ui-hud-icons` | `missing_visual` | right panel / inventory footer | `ManifestResolveTest` |
| `ui.hud.key.icon` | `icon` | `r01-ui-hud-icons` | `missing_visual` | right panel / quest/key counter | `ManifestResolveTest` |
| `ui.hud.warning.icon` | `icon` | `r01-ui-hud-icons` | `missing_visual` | HUD danger/attention cue | `ManifestResolveTest` |
| `ui.hud.quest_marker.icon` | `icon` | `r01-ui-hud-icons` | `missing_visual` | quest summary marker | `ManifestResolveTest` |
| `ui.hud.log_marker.icon` | `icon` | `r01-ui-hud-icons` | `missing_visual` | log category marker | `ManifestResolveTest` |
| `ui.control.backpack.icon` | `icon` | `r01-ui-controls` | `missing_visual` | left/right rail navigation | `ManifestResolveTest` |
| `ui.control.equipment.icon` | `icon` | `r01-ui-controls` | `missing_visual` | equipment panel marker | `ManifestResolveTest` |
| `ui.combat.action.icon` | `icon` | `r01-ui-controls` | `missing_visual` | combat action chooser | `ManifestResolveTest` |
| `ui.combat.method.icon` | `icon` | `r01-ui-controls` | `missing_visual` | combat method chooser | `ManifestResolveTest` |
| `ui.combat.target.icon` | `icon` | `r01-ui-controls` | `missing_visual` | combat target chooser | `ManifestResolveTest` |
| `ui.combat.lock.icon` | `icon` | `r01-ui-controls` | `missing_visual` | locked combat choice marker | `ManifestResolveTest` |
| `ui.combat.invalid.icon` | `icon` | `r01-ui-controls` | `missing_visual` | invalid combat choice marker | `ManifestResolveTest` |
| `ui.state.locked.icon` | `icon` | `r01-ui-controls` | `missing_visual` | talent locked glyph | `TalentSidebarPresenterTest` / `ManifestResolveTest` |
| `ui.state.learnable.icon` | `icon` | `r01-ui-controls` | `missing_visual` | talent learnable glyph | `TalentSidebarPresenterTest` / `ManifestResolveTest` |
| `ui.state.active.icon` | `icon` | `r01-ui-controls` | `missing_visual` | talent active glyph | `TalentSidebarPresenterTest` / `ManifestResolveTest` |
| `ui.state.reserve.icon` | `icon` | `r01-ui-controls` | `missing_visual` | talent reserve glyph | `TalentSidebarPresenterTest` / `ManifestResolveTest` |

`missing_visual` 只允许作为 PR-02 owner-scope 早期 fallback；PR-06 final-full 必须用 dark-v1 fallback/polish 资源替换玩家主路径中的旧风格 fallback。
金币、钥匙、任务 marker 如果与 item/quest 图标复用同一图，必须通过 key registry `aliasOf` 表达；不得让 PR-02 与 PR-03/PR-06 分别生成语义相同但风格略不同的图。

## 5. 非目标

1. 不批量替换 538 个资源。
2. 不引入 atlas/region manifest。
3. 不生成技能、装备、怪物、地图 tile 资源。

## 6. 验收标准

1. contact sheet 上 `row,col,targetKey` 与切分图片语义一致。
2. canonical/runtime `visual-manifest.json` 的 `rawOutputPath` 与 `sheet-plan.yaml.outputName` 完全一致。
3. manifest 中新增 key 都能通过 `VisualManifestResolver` 解析。
4. golden 能看到至少一个 panel/slot/modal chrome 和至少一个 HUD/icon 资源。
5. 资源加载失败时使用正式 fallback，不出现空白方块或 crash。

## 7. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint :client:test --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke :client:goldenScreenshot verifyChanged
./gradlew syncPhase2Manifests manifestLint
```

本 PR 的 dark gate 必须使用显式 owner-scope 命令：

```bash
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02
```

只强制 Round 1 UI chrome/HUD key；scope 外 pending 必须写入 coverage artifact。不得用裸 `darkManifestCoverageLint` 代替本 PR close gate。

## 8. 白盒证据

1. `dark-uiux-pr02-round1-chrome`：panel、slot、tooltip/modal frame 可见。
2. `dark-uiux-pr02-hud-icons-pilot`：HP / stamina / XP / gold 至少 2 个来自新 sheet。
3. contact sheet QA 路径和 manifest diff 路径必须写入 PR 描述。

## 9. 回滚边界

本 PR 可以通过回滚 `dark-v1/ui` runtime PNG、`sheet-plan.yaml` Round 1 cell、canonical manifest 新 key、同步生成的 runtime manifest 和 renderer 消费点完整回退；不得把新 key 写入 `core` 或 `game`。

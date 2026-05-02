# Dark UI/UX PR-05 Map Actor Portrait Replacement

**阶段**: `dark-uiux-pr05-map-actor-portrait-replacement`
**优先级**: `P1`
**工作量**: `XL`
**前置条件**: PR-01、PR-02、PR-03、PR-04 完成。
**资源生成结论**: 生成 Round 2-6 资源，替换地图、actor、portrait 主视觉。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再跑 tile / actor resource gate、client layer tests、client evidence 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI05-M01` | §3 Round 2-6 resource scope | `assets` | `darkSpriteSheetLint`, `spriteSheetMapLint` | `assetLint`, `styleLint` | `assets-src/image/contact-sheets/dark-v1/` | `required` |
| `UI05-M02` | §4 tile / actor / VFX implementation | `client` | `TileLayerComposerTest`, `TileRendererCanvasTest` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/golden/` | `required` |
| `UI05-M03` | manifest / owner-scope coverage | `tools` | `ManifestResolveTest`, `darkKeyRegistryLint` | `manifestLint`, `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-05` | dark manifest coverage report | `N/A` |
| `UI05-M04` | §6 必测行为 | `client` | map layer and render focused tests | `clientSmoke`, `goldenScreenshot` | `build/reports/tests/` | `required` |
| `UI05-M05` | governance inheritance | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |

### Gate Budget

预计重型任务：resource lint 全套、owner-scope `darkManifestCoverageLint`、`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-05 替换地图、actor、portrait 和 VFX 主视觉，是资源面最大的 dark UI PR。

### Canonical Artifact

canonical artifact 固定为 Round 2-6 raw sheet、contact sheet QA、key registry、manifest coverage report、runtime manifest、golden output 和 manual record。contact sheet 未确认前不得把 runtime PNG 视为稳定合同。

### Failure Rule

地图层级或资源 coverage 失败时先修 sheet mapping、manifest key 或 layer composer；不得通过降低 golden 覆盖、保留旧风格 residue 或跳过 owner-scope coverage 来通过。

## 1. 阶段目标

1. 替换地图核心视觉：ground、wall、decal、prop、VFX。
2. 替换 player、职业、怪物、Boss actor sprite。
3. 替换职业、天赋树、区域 portrait。
4. 让主截图从“黑底小 tile”变成有暗黑地牢质感的正式游戏画面。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `UI/sprite-sheets/sheet-plan.yaml` | 增加 Round 2-6 sheet |
| `client/src/main/resources/dark-v1/tiles/` | 地面、墙、decal、prop、VFX |
| `client/src/main/resources/dark-v1/actors/` | player、monster、boss |
| `client/src/main/resources/dark-v1/portraits/` | profession、tree、zone portrait |
| `assets-src/image/manifests/phase2-visual-manifest.json` | 先更新 tile/actor/portrait canonical manifest |
| `client/src/main/resources/manifests/visual-manifest.json` | 由 `syncPhase2Manifests` 同步生成 runtime manifest |
| `client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt` | 确认层级和遮挡 |
| `client/src/test/kotlin/com/ktome/client/render/TileLayerComposerTest.kt` | 固定 ground/wall/decal/actor/VFX/telegraph 层级 |

## 3. 资源范围

1. Round 2：`r02-tiles-ground`、`r02-tiles-wall`、`r02-tiles-decal`。
2. Round 3：`r03-props-interactable`、`r03-props-environment`、`r03-vfx-telegraph`。
3. Round 4：`r04-actors-player`、`r04-actors-humanoid`、`r04-actors-monster`、`r04-actors-boss`。
4. Round 5：`r05-bestiary-humanoid-icons`、`r05-bestiary-creature-icons`、`r05-boss-icons`。
5. Round 6：`r06-portraits-classes`、`r06-portraits-trees`、`r06-portraits-zones`。

Per-round checkpoint：

| Checkpoint | Sheet IDs | Manifest key scope | 必填证据 | 回滚文件族 |
| --- | --- | --- | --- | --- |
| Round 2 Tiles | `r02-tiles-ground`, `r02-tiles-wall`, `r02-tiles-decal` | `tile.ground.*`, `tile.wall.*`, `tile.decal.*` | contact sheet QA、tile coverage summary、固定 seed ground/wall/decal golden | Round 2 raw sheet、sliced PNG、canonical/runtime manifest patch、QA report |
| Round 3 Props/VFX | `r03-props-interactable`, `r03-props-environment`, `r03-vfx-telegraph` | `prop.*`, `interactable.*`, `vfx.*`, `telegraph.*` | interactable/telegraph screenshot、coverage summary、layering test | Round 3 raw sheet、sliced PNG、manifest patch、QA report |
| Round 4 Actors | `r04-actors-player`, `r04-actors-humanoid`, `r04-actors-monster`, `r04-actors-boss` | `actor.*`, `actor.boss.*`, profession actors | actor silhouette screenshot、boss telegraph overlap check、coverage summary | Round 4 raw sheet、sliced PNG、manifest patch、QA report |
| Round 5 Bestiary | `r05-bestiary-humanoid-icons`, `r05-bestiary-creature-icons`, `r05-boss-icons` | monster icon、boss icon、boss variant icon | bestiary/contact sheet QA、boss icon manifest diff | Round 5 raw sheet、sliced PNG、manifest patch、QA report |
| Round 6 Portraits | `r06-portraits-classes`, `r06-portraits-trees`, `r06-portraits-zones` | `portrait.*`, `tree.*`, `zone.*.visual`, secret zone visuals | profession/tree/zone portrait contact sheet QA、UI screenshot | Round 6 raw sheet、sliced PNG、manifest patch、QA report |

每个 checkpoint 都必须保持主干可玩。失败时只能回滚当前 checkpoint 的 sheet/manifest/report 文件族，不能拖累已通过 checkpoint。
`zone.*.visual` 统一归 Round 6 `r06-portraits-zones`；Round 3 只处理可交互和环境 prop，不承接 zone portrait/large visual。

Raw sheet 生成交接：

1. 每个 checkpoint 开始前，只为当前 Round 生成 prompt 文件和 `prompt-index.json` 子集，不一次性要求 lky 生成 Round 2-6 全部 raw sheet。
2. lky 在 Codex app 中执行当前 checkpoint 的 prompt，并将 PNG 放到对应 `sheet-plan.yaml.rawSheetPath`。
3. 文件名必须等于 `{sheetId}.png`，例如 `r02-tiles-ground.png`、`r04-actors-boss.png`。
4. 当前 checkpoint 的 raw PNG 全部通过尺寸、grid、hash 和路径校验后，才能切分 runtime PNG。
5. 每个 checkpoint 的 PR 描述或阶段记录必须列出 prompt path、raw sheet path、raw sheet hash、contact sheet QA path 和 coverage summary。

## 4. 实现任务

1. 分批生成 raw sheet，每批先更新 contact sheet QA，再切分 runtime PNG。
2. source sheet 几何必须使用 `UI/PLAN.md` 的固定 sheet type；runtime 显示尺寸只在 renderer/layout 中处理。
3. 更新 manifest 后，确认 `TileRenderModel` 中已有 key 全部可解析。
4. VFX、telegraph、actor、ground loot marker 的层级必须在 `TileLayerComposer` 或等价测试中固定。

## 5. 非目标

1. 不改地图生成、碰撞、AI、Boss 规则。
2. 不改 profession tree UI。
3. 不引入 atlas 或 region manifest，无例外；性能疑虑只记录到 PR-07，由 PR-07 决定是否另开 atlas PR。
4. 不改音频资源。

## 6. 必测行为

1. 地面、墙、危险地表、可交互 prop、actor 层级清楚。
2. Boss warning / telegraph 不被普通 VFX 淹没。
3. actor 不遮挡地面掉落 marker 和职业树/modal 层。
4. 地图在暗色背景下仍能区分可走、不可走、可交互和危险区域。
5. actor Y-sort、ground loot marker 与 boss telegraph 的先后关系必须有 `TileLayerComposerTest` 或等价 canvas 层级断言。

## 7. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint :client:test --tests com.ktome.client.render.TileLayerComposerTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke :client:goldenScreenshot maintainabilityLint verifyChanged
./gradlew syncPhase2Manifests manifestLint
```

本 PR 的 dark gate 必须使用显式 owner-scope 命令：

```bash
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-05
```

每个 checkpoint 必须输出当前 Round 的 coverage summary；scope 外 pending 必须写入 coverage artifact。不得用裸 `darkManifestCoverageLint` 代替本 PR close gate。

如果仓库最终不新增独立 `TileLayerComposerTest`，必须在 `TileRendererCanvasTest` 中以同名测试覆盖 ground / wall / decal / actor / VFX / boss telegraph 六层先后；PR 文档和验证命令要同步调整。

## 8. 人工白盒

1. 选择包含墙、门、地面物品、怪物、telegraph 的固定 seed 截图。
2. 对比旧图，确认地图区域不再是大面积空黑。
3. 检查 Boss telegraph 和危险地形在暗色 UI 上仍具备第一眼可读性。
4. 必填证据：固定 seed、map/actor/telegraph/ground-loot 层级截图、每批 contact sheet QA、manifest diff。

## 9. 回滚边界

1. PR-05 允许按 Round 拆成内部批次或 mini PR，但每个 Round 内必须保持 raw sheet、切分 PNG、manifest patch、contact sheet QA 原子提交。
2. 如果 Round 5 bestiary 失败，不回滚已通过的 Round 2 tile；只回滚对应 sheetId、manifest entries 和 QA report。
3. 如果 Round 4 actor/map baseline 失败，不得继续进入 Round 5 bestiary；先修复或回滚 Round 4 文件族。
4. 允许部分非 player-visible key 暂留旧 path，但必须在 `dark-v1-manifest-coverage.json` 中标记为 pending，不得静默遗漏。

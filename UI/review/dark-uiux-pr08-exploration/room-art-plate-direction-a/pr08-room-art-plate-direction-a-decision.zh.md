# PR-08 Direction A lky 统一决策包

> 日期: 2026-05-31
> 状态: `approved-by-lky-for-runtime-prototype`
> 范围: PR-08 map-stage blocker, Direction A pre-rendered room art plate

这份中文决策包记录了 lky 对 Candidate C 和有条件 Direction A runtime prototype 路线的批准。它本身不批准生产资源、不批准 manifest 变更、不批准 golden rebaseline，也不代表 PR-08 最终关闭。

已批准决策：

```text
Approve Candidate C and the conditional Direction A route: client-only room art
plate prototype first, PR-08 owner/manifest route only after runtime evidence,
large decorative pass deletion after marker readability passes, no surface pivot
and no golden rebaseline until final runtime packet.
```

## 1. 直接建议

建议批准 Direction A 作为当前 map-stage 的主动路线，选择 Candidate C 作为第一个 runtime prototype 目标，并预先批准下面这条有条件的实施路线：

1. 使用 Candidate C, `Broken slab hall`, 作为第一张 room art plate 原型。
2. 新增 client-only 的 `RoomPresentationPlan` / `RoomArtPlateRenderer` 路径。
3. 裁剪区域从现有 visible material topology 推导，不建立第二套地图真相。
4. 静态网格降到几乎不可见；hover、selection、targeting、pathing、range 继续作为运行时 overlay 叠在 plate 上方。
5. 选中的 plate 只能通过 PR-08 owner route 和现有 manifest schema 进入生产资源。提交的运行时代码不得从 `UI/review/...` 或 generated-images 目录直接加载。
6. 原型可见并通过 marker readability 后，再批准删除或禁用被 plate 取代的 `TileRenderer` 装饰 pass family。
7. 在这个 map plate probe 被接受或明确拒绝前，不要转向 right panel、bottom HUD、inventory 或 talent 工作。

最终 map closure 和 golden rebaseline 仍需要后续 runtime evidence packet。仅凭生成的 review art 不能接受最终关闭。

## 2. 给 lky 的决策清单

推荐选择：

| 决策项 | 推荐 lky 决策 | 理由 |
| --- | --- | --- |
| D0 candidate | 选择 Candidate C | authored room、可读中心区域、适中暖光和不规则石板场的平衡最好 |
| D0 alternate | 保留 Candidate A 作为备选 | wall mass 最强，也最接近 Diablo 式房间边界；但碎石更密、边缘更暗，可能和 marker readability 竞争 |
| D0 reject | 默认路线拒绝 Candidate B | 氛围好，但过于对称且火把主导太强，后续 telegraph 和 marker 对比度容易被 baked fire pools 干扰 |
| D0 fallback | Candidate D 只保留为技术 fallback | 对验证 renderer 架构有用，但它没有达到 authored-room 目标 |
| D1 runtime prototype | 批准，前提是选 C | client-only prototype 是验证 room plate 是否解决 grid-first blocker 的最短路径 |
| D2 production route | 只做有条件批准 | 生产资源提升必须等待 runtime evidence 和 gate；路线应复用现有 schema |
| D3 map closure | 现在不要接受 | 还需要 runtime full screenshot、map crop、marker/telegraph crop |
| D4 large pass removal | prototype 通过后预批准 | plate 必须替换旧的装饰几何，而不是叠在旧几何下面继续膨胀 |
| D5 surface priority | 继续优先 map | V48 让 hero/bottom 变好，但 map 仍是第一眼 blocker |
| D6 fallback route | 只有 C/prototype 失败才走 Direction B | tactical-grid fallback 是止损路线，不是当前选定的美术方向 |
| D7 multi-map rollout | 批准 ruins-first，但架构必须 tileset-agnostic | 第一轮证明只覆盖 `tileset.ruins`，但 renderer contract 必须通过 catalog/fallback 支持其他 tileset family |

如果 lky 想用一句话批准，可使用：

```text
Approve Candidate C and the conditional Direction A route: client-only room art
plate prototype first, PR-08 owner/manifest route only after runtime evidence,
large decorative pass deletion after marker readability passes, no surface pivot
and no golden rebaseline until final runtime packet.
```

中文等价表述：

```text
批准 Candidate C 和有条件的 Direction A 路线：先做 client-only room art plate
prototype；只有 runtime evidence 通过后才走 PR-08 owner/manifest 生产资源路线；
marker readability 通过后再删除大批装饰 pass；不要转移到其他 UI surface；
最终 runtime packet 之前不做 golden rebaseline。
```

## 3. 候选图证据

已生成产物：

| 产物 | 路径 |
| --- | --- |
| Prompt set | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/prompts/pr08-room-art-plate-direction-a-prompt-set.md` |
| Decision board | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-board.png` |
| Marker proxy board | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/pr08-room-art-plate-direction-a-marker-proxy-board.png` |
| Candidate A | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-a-authored-ruins-chamber.png` |
| Candidate B | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-b-torch-cut-stone-arena.png` |
| Candidate C | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-c-broken-slab-hall.png` |
| Candidate D | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/candidates/pr08-room-art-plate-d-material-only-technical-fallback.png` |

候选图评审：

| 候选 | 结论 | 说明 |
| --- | --- | --- |
| A: Authored ruins chamber | 可行备选 | wall mass 和 Diablo-like room edge 最强。风险是碎石密度高、墙体细节重，后续 clipping 和 marker readability 更难。 |
| B: Torch-cut stone arena | 不建议作为默认路线 | 氛围强，但过于对称，火把存在感过高。运行时 telegraph/readability 很可能会和 baked fire pools 抢对比度。 |
| C: Broken slab hall | 推荐 | 中央 play field 最实用，围合感强但不过量，石板不规则且没有硬网格。marker proxy 仍保持可读。 |
| D: Material-only technical fallback | 只作 fallback | 适合验证 renderer architecture，但它放弃了 authored-room 目标。 |

产物完整性：

| 产物 | 尺寸 | SHA-256 |
| --- | --- | --- |
| Candidate A | 1448x1086 | `1a5c939d541e23c2348ac258dbba5ebed95c952d877fbe0fc31b69e211711ea2` |
| Candidate B | 1448x1086 | `e77154a08a6b10e05f190289f7b009f2155d671419a858c2cb89c0c16e46bef2` |
| Candidate C | 1448x1086 | `557bdd513a5004993140339153f35071f11a63710b5d2c30297e445c23c1bae3` |
| Candidate D | 1448x1086 | `777f869cb61044a4e1619087824fe5e28ce07dbb015a7d389d0600ef5891fce6` |
| Decision board | 1452x1301 | `0af7a4c0e0b8146dea9612d1a4fc08bfa1267fc20afd8016eb9543d229cbbd2c` |
| Marker proxy board | 1174x1042 | `233fcee88c62c7b82406b781c52dc2640b42a96a1b29ae83309c9212456ade27` |
| Prompt set | n/a | `fcad7f27e4217a67cdd9114631ad8e73eaf98ebada11c195f62485018166773f` |

## 4. 来自现有 PR-08 工作的证据

现有 PR-08 证据表明，map blocker 是结构性问题，不是再微调几个 alpha 就能解决的问题：

1. `UI/review/dark-uiux-director-grade-gap-audit.md` 将主因归为 `resource-gap`。
2. subtractive spike 提升了清晰度，但暴露了重复 floor/wall tiles、硬网格结构和 authored darkness 不足。
3. 诊断证据记录了 `overlayFunctionCount=22`, `warmOverlaySubpassCount=20`, `materialRectCountPerVisibleCell=16.375`, `totalRectDraws=3784`；继续做 rectangle/pass tuning 不应是默认路线。
4. V10-V48 记录大多只是 `accepted-forward only`；V48 改善了 hero crest 质量，但 map-stage room-structure gap 仍是第一眼最大 blocker。
5. `UI/design` 对后续 shell/right/bottom 决策有帮助：它提供了 Variant A/B/C shell sizing、state matrices、slot sizing 和 token roles。但它不能替代 room-plate map 决策，也不能直接复制成 runtime 的 React/CSS 真相。

## 5. D1 Runtime Prototype 建议

推荐架构：

1. 新增 `RoomPresentationPlan` 或等价 client-only model，从 viewport、visible room bounds 和 deterministic room signature 推导。
2. 新增 `RoomArtPlateRenderer`，不要继续往 `TileRenderer` 里塞另一组 private pass cluster。
3. 使用现有 runtime visible material topology：
   - `TileRenderModel.mapCellMaterials`
   - `visibleMaterialPoints(frame)`
   - `visibleRoomClip(frame)`
4. plate 绘制顺序放在 actors、loot、telegraphs、cursor、selection 和 combat feedback 之前。
5. `core` 和 `game` 不动。

理由：

1. plate 是 presentation，不是 terrain/visibility truth。
2. 现有 topology helper 已经定义了房间中哪些区域可见；复用它们可以避免第二 authority。
3. 独立 renderer 能给旧装饰 pass family 一个明确的删除目标，而不是继续让 `TileRenderer` 膨胀。

## 6. D2 Production Resource Route 建议

runtime proof 之后的推荐生产路线：

| 字段 | 建议 |
| --- | --- |
| tentative key | `ui.map_stage.ruins.room_plate.pr08_demo` |
| owner | `PR-08` |
| category | `ui_frame`，除非后续通过单独 contract 批准新增 room-plate category |
| footprint | `ui` |
| raw path family | `dark-v1/ui/` 或另一个经批准的 PR-08 presentation-resource 路径 |
| source record | 在 Direction A review 目录保留原始 review candidate 和清理说明 |
| runtime manifest | 从 canonical manifest 生成或同步，不手工维护 |
| consumer | `RoomArtPlateRenderer` / map-stage presentation layer |
| consumer test | manifest exact-resolution test 加 focused renderer layer-order test |

不要做：

1. 不要把 full-room plate 伪装成 `tile_ground`。
2. 不要用 full-room plate 替换 `tileset.ruins.room_breakup_01`。
3. 不要新增 manifest schema、atlas/region schema 或 runtime resource table。
4. 不要让提交的运行时代码从 `UI/review/...`、本机 generated image folder 或机器绝对路径加载。

证据：

1. 当前 asset pipeline allowlist 支持 `ui_frame`, `tile_ground`, `tile_wall`, `tile_decal`, `vfx_plate` 及相关既有 category，但没有现成的 `room_plate` category。
2. `VisualManifestEntry` 已经包含 `key`, `category`, `rawOutputPath`, `footprint`, `pivotX`, `pivotY`, `tags` 和可选 `tintColorHex`；presentation image 不需要改 schema。
3. 现有 PR-08 owner wiring 已覆盖 floor/wall/room-breakup rows；room plate 应该是单独的 presentation key，不应伪装成 tile-family alias。

## 7. D4 Decorative Pass Removal 建议

建议在选中的 plate 可见并通过 marker readability 后，预先批准删除或禁用相关装饰 pass。

很可能被 plate 取代的内容：

1. room material painting 和 floor unification；
2. wall mass simulation 和 raised wall relief；
3. dark corner/silhouette pressure；
4. story decals、slab fields 和 painterly breakup；
5. broad decorative dark veil 和 hidden-stage grid suppression；
6. 默认始终可见的 grid presentation。

初始审计候选：

1. `drawVisibleRoomFoundationGlaze`
2. `drawVisibleRoomFloorUnifier`
3. `drawVisibleRoomAtmosphere`
4. `drawVisibleRoomApertureHierarchy`
5. `drawHiddenStageGridSuppression`
6. `drawVisibleWallRelief`
7. `drawVisibleWallMassBands`
8. `drawVisibleWallRaisedFaces`
9. `drawVisibleWallCrownBlocks`
10. `drawVisibleWallMasonryCourses`
11. `drawVisibleWallFootRubble`
12. `drawPr08WallFamilyReliefRepaint`
13. `drawVisibleRoomCornerBreakup`
14. `drawVisibleRoomSilhouettePressure`
15. `drawVisibleRoomBoundaryCompression`
16. `drawVisibleRoomAsymmetricEdgeMass`
17. `drawVisibleRoomMacroStructuralPlates`
18. `drawVisibleRoomRuntimeCornerApertureShelves`
19. `drawVisibleRoomOuterShadows`
20. `drawVisibleRoomContactShadows`
21. `drawVisibleRoomStoryDecals`
22. `drawVisibleRoomMaterialBreakupAsset`
23. `drawVisibleRoomSlabVariation`
24. `drawVisibleRoomGridDissolve`
25. `drawVisibleRoomStaggeredStoneRhythm`
26. `drawVisibleRoomCrossCellSlabFields`
27. `drawVisibleRoomScaleMaterialFields`
28. `drawVisibleRoomLocalizedStoneDamage`
29. `drawVisibleRoomSilhouetteBreakup`
30. `drawVisibleRoomPainterlyBreakup`
31. `drawVisibleRoomTacticalClarityPlane`

很可能保留的内容：

1. semantic cells 的 terrain base draw；
2. 非 floor 的 hazard/material semantics；
3. deterministic visibility/fog truth；
4. actors、props、loot、telegraphs 和 combat feedback；
5. cursor、selection、targeting、path/range overlays；
6. 只在改善 marker readability 时保留的小型 grounding shadows 或 light passes。

## 8. Surface Priority 建议

继续卡住 map-stage blocker，直到发生以下任一情况：

1. lky 拒绝这批 room-plate candidates；
2. Candidate C runtime prototype 的 marker readability 失败，或视觉结果比当前 V48 更差；
3. 第二批 candidate 也失败；
4. lky 明确选择 tactical-grid fallback 或非 map surface。

理由：

1. V48 和 `UI/design` 对 right/bottom/shell 有帮助，但 map 仍是第一眼 blocker。
2. 在 D0/D1 之前转向其他 surface，会重复之前那种到处局部微调、没有突破 map 结构问题的模式。
3. 如果 room plate 路线失败，`UI/design` Variant C 可以指导后续 wide-map tactical fallback，Variant B 可以指导 inscription-priority right-panel 工作。

## 9. D7 Multi-map Rollout 建议

建议批准 ruins-first rollout：这不是一张地图的临时 hack，也不是在本 slice 内重做所有地图美术。

当前内容证据：

1. `game/src/main/resources/data/zones/index.yaml` 当前定义了 11 个 zone，分布在 4 个 tileset family。
2. `tileset.ruins` 用于 `shattered_outpost` 和 `elven_ruins`。
3. `tileset.forest_edge` 用于 `greenwood_fringe` 和 `bandit_camp`。
4. `tileset.mine` 用于 `deep_iron_pit` 和 `molten_core`。
5. `tileset.shadow_depths` 用于 `grey_gate_depths`, `underground_river`, `crystal_cavern`, `abyssal_temple`, `abyssal_heart`。
6. 当前 visual manifest 里 `tileset.ruins` 已经有更完整的 PR-08 family：floor variants、wall pieces 和 `room_breakup_01`。其他 tileset family 目前主要还是较简单的 ground/wall pairs。

推荐边界：

1. 第一轮 runtime prototype 和 Candidate C production route 只覆盖 `tileset.ruins`。
2. renderer/model 架构必须 tileset-agnostic：使用 `RoomArtPlateCatalog` 或等价 selector，按 `tilesetKey`、可选 biome/theme tags 和 deterministic room signature 选择 plate。
3. 非 ruins family 在拥有自己的 accepted plate family 之前，必须 fallback 到现有 tile renderer。
4. 不要把 ruins plate 套到 `tileset.forest_edge`、`tileset.mine` 或 `tileset.shadow_depths`。
5. 不要用 ruins-only screenshot 宣称 all-map visual closure。如果 PR-08 要声称全地图质量，需要每个 tileset family 至少一张 evidence crop，或者明确记录 follow-up gap。

建议后续顺序：

1. `tileset.ruins`: 当前 Direction A proof 和首个 runtime prototype。
2. `tileset.forest_edge`: forest edge / trail / ambush authored plate family。
3. `tileset.mine`: forge / slag / ore-cart authored plate family。
4. `tileset.shadow_depths`: 先拆出或至少区分 depths、cavern、temple 的阅读差异，再声明该 family 完成。

## 10. Final Closure Rubric

在 runtime evidence 证明以下事项之前，不要接受最终关闭：

1. full screenshot 和 map crop 读起来是 authored room，而不是 grid-first board。
2. actor、enemy、loot、telegraph、cursor、selection、path/range overlays 比 baked environment 更清楚。
3. static grid 安静或消失，但 interactive states 仍然可见。
4. 被替换的 decorative pass families 已在 room-plate path 中删除或禁用。
5. production resource ownership 能通过 PR-08 owner route、canonical manifest、runtime manifest sync 和 resource gates 追踪。
6. golden evidence 使用 PR-08-specific director labels。
7. packaged whitebox 和 `verifyChanged` 已运行，或真实 blocker 已记录。
8. ruins-first closure 不能被误标为 all-map closure；其他 tileset family 要么已有证据，要么有明确 follow-up gap record。

## 11. 本决策包已执行的验证

已执行：

1. 阅读 PR-08 plan、goal、log、PR doc、gap audit、evidence brief 和 `UI/design` 文件。
2. 阅读与 room plate routing 相关的 renderer/model/manifest 入口：
   `TileRenderer.kt`, `TileRenderModel.kt`, `DarkUiMapVisualKeys.kt`,
   `ManifestModels.kt`, `ManifestResolvers.kt`, `key-registry.yaml`,
   `pr08-owner-keys.yaml`, canonical/runtime visual manifests 和 asset pipeline category allowlist。
3. 使用内置 image generation 生成 4 张 review-only room plate candidates。
4. 生成 candidate decision board 和 synthetic marker-readability proxy board。
5. 记录图片尺寸和 SHA-256。
6. 验证本 packet、prompt set、goal/log 和 Direction A plan 没有机器绝对路径、没有未解决占位标记、没有行尾空白。
7. 对触及的 goal/log/plan/packet 文件运行 `git diff --check`，没有 whitespace 报错。
8. 阅读 zone、tileset、manifest 和 renderer 证据，用于 multi-map rollout 边界判断。

未执行：

1. 没有 Kotlin implementation changes。
2. 没有 runtime prototype。
3. 没有 manifest、sheet-plan、resource、golden 或 packaged whitebox mutation。
4. 没有为本 packet 运行 Gradle，因为本轮只有 review-only documentation 和 generated evidence。

## 12. 当前状态

`approved-by-lky-for-runtime-prototype`

下一步实现是 client-only runtime prototype：

1. 使用 Candidate C 作为 review-selected prototype source；
2. 在 tileset-agnostic catalog/fallback 边界下做 `tileset.ruins` runtime proof；
3. PR-08 owner/manifest promotion 继续阻塞，直到 runtime evidence 出来；
4. large decorative pass deletion 继续阻塞，直到 marker readability 通过；
5. golden rebaseline 和 final closure 继续阻塞，直到 final runtime packet 被接受。

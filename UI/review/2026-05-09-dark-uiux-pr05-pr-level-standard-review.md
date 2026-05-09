# Dark UI/UX PR-05 PR 级深度 Review

目标文档：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md`

审查标准：`docs/review/rule/pr-level-review-standard.md`

审查范围：
- 上游入口：`docs/INDEX.md`、`UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`UI/pr/screen-coverage-matrix.md`
- 当前代码锚点：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`、`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`、`client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt`、`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- 当前资源与工具锚点：`UI/sprite-sheets/sheet-plan.yaml`、`UI/sprite-sheets/key-registry.yaml`、`assets-src/image/manifests/phase2-visual-manifest.json`、`scripts/verify_dark_manifest_coverage.py`、`scripts/verify_sprite_sheet_map.py`、`tools/build.gradle.kts`
- 工作树状态：`main...origin/main`，当前有 PR-02/coverage tooling 相关未提交改动；本轮只读这些改动，不把它们当 PR-05 实现成果。

## Findings

### P0

无。

### P1

#### P1-1 地图 key scope 写成 `tile.*`，但当前运行时真实消费的是 `tileset.*`

证据：
- PR-05 checkpoint 把 Round 2 manifest key scope 写为 `tile.ground.*`、`tile.wall.*`、`tile.decal.*`：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:65-68`
- 系列 README 的 SheetId Ownership 也写成 `tile.ground.*`、`tile.wall.*`、`tile.decal.*`：`UI/pr/README.md:64-66`
- 当前 canonical manifest 中正式地图 key 是 `tileset.forest_edge.ground_01`、`tileset.forest_edge.wall_01`、`tileset.mine.ground_01`、`tileset.mine.wall_01` 等：`assets-src/image/manifests/phase2-visual-manifest.json:6914-7014`
- 当前 session 生成 `RenderSnapshot.mapCells[].terrainVisualKey` 时也使用 `${zone.tilesetKey}.ground_01` 和 `${zone.tilesetKey}.wall_01`：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:7882-7898`

问题：
按 PR-05 文档实现会创建或登记 `tile.ground.* / tile.wall.*` 这类当前 runtime 不消费的 key。`TileRenderModelBuilder` 只解析 snapshot 已给出的 `cell.terrainVisualKey`，即 `tileset.*` 或 terrain interaction VFX key；开发者即使生成了 `tile.ground.*` sheet cell，也不会替换真实地图地面/墙体。

影响：
- Round 2 的 owner-scope coverage 可能覆盖了错误命名空间，主地图仍显示旧 `phase2/p2-*` tileset。
- golden/manual 看到的地图质感不会变化，但资源 gate 可能看似通过。
- 后续 PR-06 final-full 会发现旧风格 residue，返工需要重改 key registry、sheet plan、manifest 和 tests。

修复方向：
把 PR-05 和 `UI/pr/README.md` 中 Round 2 scope 改为当前真实 key：
- `r02-tiles-ground`: `tileset.<tilesetId>.ground_01`，至少覆盖 `forest_edge`、`mine`、`ruins`、`shadow_depths`
- `r02-tiles-wall`: `tileset.<tilesetId>.wall_01`，同上
- `r02-tiles-decal`: 只承接真实 decal/VFX key，例如 `vfx.zone.effect.*`、`vfx.terrain.interaction.*`、`vfx.*warning*`；不要写不存在的 `tile.decal.*`，除非同 PR 明确改变 runtime key contract 并更新 `FoundationGameSession`、manifest 和 tests

推荐测试：
- `ManifestResolveTest.darkUiuxPr05TilesetOwnerKeysResolveThroughExactEntries`
- `TileLayerComposerTest.pr05TilesetGroundAndWallUseDarkV1ManifestEntries`
- `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-05 -Pktome.darkUiux.requiredOwnerSheetIds=...`

#### P1-2 PR-05 owner-scope gate 没有锁定 Round 2-6 全部 sheet，可能漏掉整轮资源仍通过

证据：
- PR-05 要生成 Round 2-6 共 15 张 sheet：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:57-61`
- PR-05 close gate 只传 `coverageMode=owner-scope` 和 `ownerPr=PR-05`，没有传 required sheet set：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:115-120`
- 当前 coverage script 已支持 `--required-owner-sheet-ids` 并会在 owner-scope 下 fail fast：`scripts/verify_dark_manifest_coverage.py:40-49`、`scripts/verify_dark_manifest_coverage.py:149-178`
- `tools/build.gradle.kts` 已暴露 `-Pktome.darkUiux.requiredOwnerSheetIds` 给 `darkManifestCoverageLint`：`tools/build.gradle.kts:597-636`
- PR-02 文档已经采用 required sheet set 作为 close gate：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:60-62`、`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:244-258`

问题：
PR-05 是资源面最大的 dark UI PR，单靠 `ownerPr=PR-05` 只能证明有 PR-05 owner entries 且 missing entry 处理符合脚本当前规则，不能证明 15 张 sheet 都进入 owner scope。开发者可能只完成 Round 2 tiles 或只完成 actor sheet，gate 仍无法机械证明缺少 Round 5/6。

影响：
- map、actor、bestiary、portrait 任何一大块都可能被遗漏。
- PR-06/PR-07 会继承一个看似通过但实际缺 sheet 的资源状态。
- reviewer 必须人工打开 coverage artifact 对 `ownerSheetIds`，违反 review 标准“不需要猜测”的要求。

修复方向：
PR-05 §7 增加两层命令：

```bash
# Per-checkpoint example, Round 2 only
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=owner-scope \
  -Pktome.darkUiux.ownerPr=PR-05 \
  -Pktome.darkUiux.requiredOwnerSheetIds=r02-tiles-ground,r02-tiles-wall,r02-tiles-decal

# PR close
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=owner-scope \
  -Pktome.darkUiux.ownerPr=PR-05 \
  -Pktome.darkUiux.requiredOwnerSheetIds=r02-tiles-ground,r02-tiles-wall,r02-tiles-decal,r03-props-interactable,r03-props-environment,r03-vfx-telegraph,r04-actors-player,r04-actors-humanoid,r04-actors-monster,r04-actors-boss,r05-bestiary-humanoid-icons,r05-bestiary-creature-icons,r05-boss-icons,r06-portraits-classes,r06-portraits-trees,r06-portraits-zones
```

同时写明 PR close artifact 必须满足：
- `scopeMode=owner-scope`
- `ownerPr=PR-05`
- `ownerExpectedKeys` 非空
- `ownerSheetIds` 覆盖上述 15 张 sheet
- `ownerMissingKeys=[]`
- `ownerCoveredKeys` 覆盖所有 PR-05 player-visible owner keys

推荐测试：
- `coverage lint fails owner scope when pr05 required sheet ids are missing`
- `coverage lint writes pr05 owner sheet ids and owner key counts`

#### P1-3 PR-05 没有冻结 exact target key inventory，开发者无法知道每张 sheet 应该放哪些 key

证据：
- PR-05 资源范围只有 sheetId 和粗粒度 scope，例如 `actor.*`、`profession actors`、`monster icon`、`boss variant icon`、`secret zone visuals`：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:63-71`
- Key Registry Contract 要求每个新增/替换资源都有 `targetKey / category / ownerPr / sheetId / fallbackKey / consumer / consumerTest / aliasOf`：`UI/pr/README.md:114-131`
- 当前 manifest 已有大量不同 namespace：actor 是 `actor.*`，bestiary 是 `icon.monster.*` 与 `boss.*.icon`，zone visual 中既有 `portrait` category 也有 `prop_environment` category：`assets-src/image/manifests/phase2-visual-manifest.json:7557-7588`、`assets-src/image/manifests/phase2-visual-manifest.json:7827-7858`
- 当前 `UI/sprite-sheets/key-registry.yaml` 只有 PR-00 dry-run entries，没有 PR-05 owner entries：`UI/sprite-sheets/key-registry.yaml:3-32`

问题：
PR-05 文档没有把 `key-registry.yaml` 的 PR-05 formal inventory 写成开发合同。仅靠 prefix 和自然语言无法决定：
- `actor.player`、release playable actor、dev playable actor、frozen actor placeholder 分别属于哪张 actor sheet
- `boss.*.visual` 是 actor sprite 还是 portrait/icon
- `icon.monster.*` 如何分到 humanoid/creature sheet
- `zone.*.visual` 旧 `prop_environment` category 是否要迁移为 `portrait`
- 哪些 key 允许 alias，哪些必须生成新图
- 哪些 key 是 player-visible close blocker，哪些可作为 scope 外 pending

影响：
不同开发者可以给出互不兼容但都“看起来符合文档”的 sheet plan。coverage artifact 的分母会漂移，contact sheet QA 无法按稳定清单核对，PR-06 rejected-cell 返修也无法知道继承哪些 PR-05 owner cells。

修复方向：
在 PR-05 增加 `Owner Key Inventory` 章节。最小结构：

| sheetId | required targetKey set source | category | ownerPr | fallbackKey rule | consumer | consumerTest/golden |
| --- | --- | --- | --- | --- | --- | --- |
| `r02-tiles-ground` | exact list of `tileset.*.ground_01` | `tile_ground` | `PR-05` | `missing_visual` until raw accepted, then no owner fallback | `TileRenderModelBuilder` | `ManifestResolveTest.darkUiuxPr05TilesetOwnerKeysResolveThroughExactEntries` |

如果清单太长，可以用 repo-owned generated inventory 文件，但 PR 文档必须给出路径、生成命令、schema 和 close gate。例如 `UI/sprite-sheets/pr05-owner-key-inventory.md` 或 JSON，且必须由 `darkKeyRegistryLint`/`darkManifestCoverageLint` 消费或校验。

推荐测试：
- `darkKeyRegistryLint` 增加 PR-05 owner inventory completeness fixture
- `ManifestResolveTest.darkUiuxPr05OwnerKeysResolveThroughExactEntries`

#### P1-4 `TileLayerComposerTest` 被指定为 layer authority，但当前文件不存在且 PR 文档没有冻结 test names / layer ordering

证据：
- PR-05 影响范围列出 `client/src/test/kotlin/com/ktome/client/render/TileLayerComposerTest.kt`，要求固定 ground/wall/decal/actor/VFX/telegraph 层级：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:52-53`
- PR-05 必测行为要求 actor Y-sort、ground loot marker 与 boss telegraph 的先后关系由 `TileLayerComposerTest` 或等价 composer-owner test 锁定：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:100-104`
- 当前仓库没有 `client/src/test/kotlin/com/ktome/client/render/TileLayerComposerTest.kt`
- 当前 `TileLayerComposer.compose` 只按 `terrainTiles -> propTiles -> overlayTiles -> actorTiles` 拼接：`client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt:3-10`
- `TileRenderModel` 中 `groundLootMarkers` 是独立字段，不在 `TileLayerComposer.compose` 的返回序列中：`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:193-199`

问题：
PR 文档把一个不存在的测试文件当作 owner gate，却没有写“新增测试文件”和 exact test methods。更严重的是，文档要求 composer 锁定 ground loot marker 与 boss telegraph/actor 的顺序，但当前 composer API 根本不包含 `groundLootMarkers`。开发者无法判断是要扩展 composer contract、保留 marker 在 renderer 里单独绘制，还是写 canvas-level 等价测试。

影响：
- 按文档直接跑 `:client:test --tests com.ktome.client.render.TileLayerComposerTest` 会找不到测试。
- 层级问题可能被塞进 `TileRendererCanvasTest` 的最终 wiring，丢失 PR-01-1 冻结的 composer owner。
- boss telegraph、actor 和 loot marker 的真实遮挡关系会变成实现者临场判断，玩家看到的危险提示可能被 actor/VFX 盖住。

修复方向：
PR-05 必须新增“Layer Contract”小节，明确选择之一：
1. 扩展 `TileLayerComposer` 的输入/输出，使其统一排序 terrain、prop、decal/VFX、telegraph、groundLootMarker、actor，并新增 `TileLayerComposerTest`。
2. 保持 `groundLootMarkers` 在 renderer 独立绘制，但文档必须写明 composer 只拥有 visual placement 顺序，ground loot 与 actor/telegraph 的遮挡由 `TileRendererCanvasTest` 的具体 test method 作为 canvas-owner 断言。

推荐最小测试名：
- `TileLayerComposerTest.composesTerrainPropsVfxTelegraphBeforeActors`
- `TileLayerComposerTest.keepsBossTelegraphAboveOrdinaryVfx`
- `TileRendererCanvasTest.drawsGroundLootMarkerAboveTerrainAndBelowBlockingActorBadge`
- `TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell`

#### P1-5 `owner-scope` 的通过条件没有禁止 PR-05 owner key 继续指向旧风格资源

证据：
- PR-05 目标是替换地图、actor、portrait 主视觉：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:35-40`
- README 要求 PR-05 owner-scope 当前 PR owner scope 必须完整：`UI/pr/README.md:147-149`
- PR-05 §7 只说每个 checkpoint 输出 coverage summary，scope 外 pending 写入 artifact：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:122`
- 当前 coverage script 在 owner-scope 下只把 missing key 加入 error；pending 或 old-style owner key 只会进入 `allowedOwnerFallbackKeys`/covered count差异，不会天然 fail：`scripts/verify_dark_manifest_coverage.py:103-134`、`scripts/verify_dark_manifest_coverage.py:164-178`

问题：
PR-05 没有定义“owner-scope 完整”的可机检条件。对正式资源替换 PR 来说，PR-05 owner keys 继续指向 `phase2/`、`phase3/`、`phase4/` 或 `debug/*` 都不应被视为 close gate 通过；但当前文档没有要求 `ownerCoveredKeys == ownerExpectedKeys`，也没有要求 owner old-style/pending 为空。

影响：
资源 PR 的主要目标可能只停留在 registry/sheet-plan 层，runtime manifest 仍用旧图。玩家截图还是旧风格，PR-06/PR-07 才爆出旧资源残留。

修复方向：
PR-05 §7 增加 artifact 断言：
- `ownerCoveredKeys` 数量必须等于 `ownerExpectedKeys`
- `ownerMissingKeys=[]`
- `allowedOwnerFallbackKeys=[]`，除非该 key 在本 PR 文档的 `Allowed Owner Fallbacks` 表中列明 `targetKey / reason / player-visible? / removalOwner / risk`
- 如果当前脚本缺少 `ownerOldStyleKeys` 字段，PR-05 应要求扩展 coverage artifact，而不是靠人工读 raw path

推荐测试：
- `coverage lint fails owner scope when pr05 owner key still points to old style`
- `coverage lint fails owner scope when pr05 owner key uses missing_visual without explicit allowed fallback`

### P2

#### P2-1 影响范围不是 Added / Modified / Deleted 执行清单，遗漏关键资源与报告文件族

证据：
- review 标准要求 Implementation Dry Run 有 Added / Modified / Deleted 三栏文件清单：`docs/review/rule/pr-level-review-standard.md`
- PR-05 只有“区域 / 预期改动”表：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:42-53`
- UI 总方案列出的资源管线交付物包括 `key-registry.yaml`、prompt、prompt-index、raw sheets、contact sheets、sprite report、canonical/runtime manifest、runtime PNG：`UI/PLAN.md:420-433`
- PR-05 影响范围没有列 `UI/sprite-sheets/key-registry.yaml`、`UI/sprite-sheets/prompts/dark-v1/*`、`assets-src/image/raw/sheets/dark-v1/*`、`assets-src/image/contact-sheets/dark-v1/*`、`assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` 或 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`

问题：
开发者无法逐文件执行，也无法在 code review 中确认是否漏了 prompt、raw、contact sheet、registry、coverage 或 report。PR-05 还包含 internal checkpoint/mini PR 机制，没有文件族清单会让回滚边界无法执行。

修复方向：
把 §2 改成三栏：
- Added：Round 2-6 raw PNG、contact sheets、prompt files、PR-05 owner registry entries、runtime PNG、PR-05 sprite report/golden/manual record
- Modified：`sheet-plan.yaml`、`key-registry.yaml`、canonical manifest、runtime manifest、`TileLayerComposer.kt` 或 renderer/canvas tests
- Deleted：若无删除写 `N/A`；若废弃旧 golden labels、旧 fixture、旧 manifest fallback，则列入 removalPlan

#### P2-2 manifest 同步命令顺序会让实现者先跑失败的 `manifestLint`

证据：
- PR-05 明确 canonical manifest 先更新，runtime manifest 由 `syncPhase2Manifests` 同步：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:50-51`
- §7 第一条长命令先跑 `manifestLint`，下一行才跑 `syncPhase2Manifests manifestLint`：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:108-113`
- README 明确新 key 最小闭环是 canonical manifest -> `syncPhase2Manifests` -> runtime manifest -> resolver/test -> coverage artifact：`UI/pr/README.md:133-139`

问题：
如果开发者按文档先更新 canonical manifest，第一条长命令里的 `manifestLint` 会在 runtime manifest 尚未同步时失败。这个失败不是行为回归，而是文档命令顺序错误。

修复方向：
把验证顺序改成：

```bash
./gradlew syncPhase2Manifests manifestLint
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint ...
./gradlew :client:test --tests ...
./gradlew :client:clientSmoke :client:goldenScreenshot maintainabilityLint verifyChanged
```

并声明 `client/src/main/resources/manifests/visual-manifest.json` 不允许手改，只能由同步任务生成。

#### P2-3 golden/manual evidence 没有使用系列固定 label，固定 seed 和 artifact 字段不足

证据：
- screen coverage matrix 要求 PR-05 至少产出 `dark-uiux-pr05-map-layer-stack` 和 `dark-uiux-pr05-actor-boss-telegraph`：`UI/pr/screen-coverage-matrix.md:50-50`、`UI/pr/screen-coverage-matrix.md:73-74`
- README evidence matrix 也列出同样 label：`UI/pr/README.md:226-233`
- PR-05 人工白盒只写“选择包含墙、门、地面物品、怪物、telegraph 的固定 seed 截图”和“必填证据”，没有 label、seed id、locale、viewport、scenario id、artifact path、expected evidence fields：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:126-131`

问题：
player-facing 资源替换没有可追溯证据标签，PR-07 final evidence index 无法机械继承 PR-05 结果。开发者也不知道 golden harness 应新增哪些 label，manual record 应写哪些字段。

修复方向：
PR-05 §8 增加 evidence 表：

| label | scenario/seed | viewport/locale | must contain | artifact | owner test |
| --- | --- | --- | --- | --- | --- |
| `dark-uiux-pr05-map-layer-stack` | exact validation scenario or fixed seed | `1280x800`, `zh-CN` and/or `en-US` | ground/wall/decal/prop/actor/loot marker | `client/build/reports/golden/...` and `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md` | `GoldenScreenshotHarnessTest...` |
| `dark-uiux-pr05-actor-boss-telegraph` | exact boss/telegraph scenario | same | boss actor, telegraph, VFX overlap | same | `TileLayerComposerTest...` / `TileRendererCanvasTest...` |

#### P2-4 Raw sheet 交接要求记录 transient source，但没有防止本机绝对路径进入 committed evidence

证据：
- PR-05 要求 PR 描述或阶段记录列出 Codex CLI transient source folder/source image 摘要：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:78-82`
- `scripts/codex-generate-image.py` 的 smoke report 会写 `sourceFolder`、`sourceImage` 和 `output`：`scripts/codex-generate-image.py:114-136`
- UI 总方案要求 Codex CLI generated images 目录只是 transient source，不进入 manifest/coverage artifact 或 PR 合同：`UI/PLAN.md:420-433`

问题：
如果开发者把 smoke report 或 source path 原样贴进 manual record / PR doc / committed artifact，会违反 repo-relative path 规则。PR-05 只说“摘要”，没有定义允许记录的字段和脱敏格式。

修复方向：
PR-05 §3 增加规则：
- `--smoke-report` 只允许写到 `build/reports/verification/dark-uiux/codex-image-smoke-<sheetId>.json`，不提交
- committed manual record 只能记录 `promptPath`、`rawSheetPath`、`rawSheetHash`、`sourceFolderLabel=<codex-generated-images-dir>/<session-summary>`、`sourceImageName`，禁止机器绝对路径或完整 transient path
- 报告自检必须执行 `rg -n '/(U[s]ers|tmp)/|[A-Za-z]:\\\\' <changed-docs-and-artifacts>`

#### P2-5 Acceptance Matrix artifact 太粗，不能直接定位 canonical evidence

证据：
- `UI05-M03` artifact 写成 `dark manifest coverage report`，不是 repo-relative path：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:19`
- `UI05-M04` artifact 写成 `build/reports/tests/`，没有测试类、测试名或具体 XML/HTML report：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:20`
- Canonical Artifact 章节只写“Round 2-6 raw sheet、contact sheet QA、key registry、manifest coverage report、runtime manifest、golden output 和 manual record”，没有具体路径：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:27-30`
- coverage script 默认报告路径是 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`：`scripts/verify_dark_manifest_coverage.py:40-50`

问题：
review 标准要求 artifact 可直接追溯。PR-05 当前 artifact 字段需要开发者猜路径，也无法让 acceptanceContractLint 扩展为结构化检查。

修复方向：
把 artifact 改为具体路径：
- `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`
- `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` 或当前工具实际 report path
- `assets-src/image/contact-sheets/dark-v1/<sheetId>-contact.png`
- `client/build/reports/tests/test/classes/com.ktome.client.render.TileLayerComposerTest.html`
- `client/build/reports/golden/`
- `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md`

#### P2-6 Gate Budget 缺少耗时来源、freshness 和失败复盘入口

证据：
- development governance 要求 Gate Budget 声明重型任务、触发原因、resource/manifest/golden freshness、最近耗时来源或 duration summary 读取方式：`UI/pr/development-governance.md:58-66`
- PR-05 Gate Budget 只列重型任务和触发原因：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:23-25`

问题：
PR-05 是资源面最大 PR，但没有说明何时重跑全部 golden、何时只跑 checkpoint focused gate、如何读取 `full-task-duration-summary`，也没有把“同一重型 gate 失败超过 2 次 / 单轮超过 90 分钟”的复盘路径写进本 PR。

修复方向：
补充：
- 开发循环只跑当前 checkpoint 的 resource lint + focused test
- 每个 checkpoint 结束跑 owner-scope coverage subset + targeted golden label
- PR close 跑全 15 sheet required owner scope、`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged`
- 读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}`；若不存在则在 PR 描述标记 first-run no baseline

#### P2-7 PR-05 preconditions 只有“PR-01 到 PR-04 完成”，没有机械检查

证据：
- PR-05 前置条件写“PR-01、PR-02、PR-03、PR-04 完成”：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:6`
- PR-05 依赖 PR-02 resource pipeline、PR-01-1 layer owner、PR-03 ground loot marker、PR-04 profession tree/modal 不被 actor 遮挡，但文档没有列出检查入口。

问题：
开发者无法判断“完成”的定义。尤其当前工作树显示 PR-02 文档和 coverage tooling 仍有未提交改动，本 PR 如果立即开发会继承一个移动中的 owner gate。

修复方向：
在 PR-05 增加 preflight checklist：
- `./gradlew acceptanceContractLint`
- `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint`
- `./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons`
- 确认 `TileLayerComposer` owner path / PR-01-1 manual record 存在
- 确认 `GroundLootMarkerModelTest` 和 PR-03 golden label 已存在
- 确认 PR-04 talent modal label 已存在

#### P2-8 回滚边界缺少 PR-06/PR-07 rejected cell handoff 的字段级合同

证据：
- PR-05 允许按 Round 拆内部批次或 mini PR，并允许部分 non-player-visible key 暂留旧 path 且写入 coverage artifact pending：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:133-138`
- PR-06 要承接 Round 2-6 rejected cell 返修：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`
- review 标准要求 replacement/fallback/removal 责任能追到 `removalOwner` 或 `crossPrDependency`。

问题：
PR-05 没有定义 rejected/pending cell 在 coverage artifact 中如何记录 `targetKey / sheetId / reason / playerVisible / removalOwner / crossPrDependency`。如果 PR-05 留下 pending，PR-06/PR-07 无法判断哪些是合法延期、哪些是漏做。

修复方向：
增加 `Deferred / Rejected Cell Handoff` 表：

| targetKey | sheetId | playerVisible | reason | temporary fallback | removalOwner | crossPrDependency | acceptance before PR close |
| --- | --- | --- | --- | --- | --- | --- | --- |

规则：PR-05 player-visible owner key 默认不得 pending；非 player-visible pending 必须指定 `removalOwner=PR-06` 或 `PR-07`。

### P3

#### P3-1 Round 5 evidence 文案只写 boss icon manifest diff，漏掉 humanoid/creature icon manifest diff

证据：
- Round 5 包含 `r05-bestiary-humanoid-icons`、`r05-bestiary-creature-icons`、`r05-boss-icons`：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:60-60`
- Round 5 必填证据只写 “bestiary/contact sheet QA、boss icon manifest diff”：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:70-70`

修复方向：
改为 “bestiary contact sheet QA、humanoid/creature/boss icon manifest diff、icon.monster.* / boss.*.icon resolver test”。

#### P3-2 文档标题和阶段名只覆盖 map/actor/portrait，实际 scope 还包含 bestiary icon 和 VFX/props

证据：
- 标题是 `Map Actor Portrait Replacement`：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:1`
- 实际资源范围包含 Round 3 props/VFX 和 Round 5 bestiary icons：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:57-61`

修复方向：
标题可以保留，但在 §1 第一段补一句：本 PR 的执行 scope 是 `tileset / prop / VFX / actor / bestiary icon / portrait`，标题中的 portrait replacement 包含 bestiary icon 视觉族。

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR-05 必须替换 Round 2-6 player-visible tile/prop/VFX/actor/portrait 资源 | PR-05 目标和资源范围明确：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:35-61`；但缺 exact owner key inventory，且地图 key prefix 与 runtime 不一致 | 部分一致 |
| 新资源必须通过 key registry -> sheet plan -> canonical manifest -> sync -> runtime manifest -> resolver/test -> coverage artifact | README 固定链路：`UI/pr/README.md:133-139`；PR-05 写了 manifest/sync，但 artifact 和执行顺序不完整：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:50-53`、`:108-122` | 部分一致 |
| owner-scope coverage 必须证明当前 PR owner scope 完整 | README：`UI/pr/README.md:147-149`；PR-05 未传 required sheet set，也未定义 `ownerCoveredKeys == ownerExpectedKeys` | 部分一致 |
| PR-05 不改变地图生成、碰撞、AI、Boss 规则 | PR-05 非目标：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:91-96`；当前建议修复都可限制在资源、manifest、client rendering/tests | 一致 |
| Boss telegraph / VFX / actor / loot marker 层级必须可自动化验证 | PR-05 必测行为：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:98-104`；当前 `TileLayerComposerTest.kt` 不存在，composer API 也没有覆盖 ground loot marker | 部分一致 |
| 玩家可见 evidence 必须能追到 golden/manual label | screen matrix 固定 PR-05 label：`UI/pr/screen-coverage-matrix.md:50-50`、`:73-74`；PR-05 白盒没有 label、seed、scenario、artifact path | 部分一致 |

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前代码/文档状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| Map tiles | Round 2 替换 ground/wall/decal | 当前 runtime 使用 `tileset.*.ground_01/wall_01` | `FoundationGameSession.kt:7882-7898`；`phase2-visual-manifest.json:6914-7014` | PR-05 写 `tile.ground.* / tile.wall.*`，会生成不被消费的 key | P1 |
| Props/VFX/telegraph | Round 3 替换 prop、interactable、VFX、telegraph | 当前 manifest 有 `prop.*` 和 `vfx.*`，telegraph 是 overlay visualKey | `phase2-visual-manifest.json`；`TileRenderModel.kt:254-265` | 没有 exact key list，也没定义普通 VFX 与 boss telegraph 的 ordering contract | P1 |
| Actor sprites | Round 4 替换 player、profession、monster、boss actors | 当前 actor visual 从 `snapshot.actors[].visualKey` 解析 | `TileRenderModel.kt:278-288` | 文档没冻结 actor/profession/dev/frozen key 分配，也没定义 Y-sort tie-break | P1 |
| Bestiary icons | Round 5 替换 monster/boss icons | 当前 manifest 用 `icon.monster.*`、`boss.*.icon` 等 namespace | manifest query evidence | 文档用自然语言 scope，缺 exact namespace 和 consumer tests | P1 |
| Portraits / zone visuals | Round 6 替换 profession/tree/zone portrait | 当前 `zone.*.visual` category 混有 `prop_environment` 与 `portrait` | `phase2-visual-manifest.json:7557-7588`、`:7827-7858` | category migration 和 footprint/pivot 规则未冻结 | P1 |
| Layer composer | 固定 ground/wall/decal/actor/VFX/telegraph 层级 | 当前 composer 只拼接 4 类 visual placements | `TileLayerComposer.kt:3-10` | `TileLayerComposerTest` 不存在；groundLootMarkers 不在 composer 输出中 | P1 |
| Coverage artifact | owner-scope 证明 PR-05 完整 | 工具支持 required sheet ids，但文档未使用 | `scripts/verify_dark_manifest_coverage.py:149-178` | 缺 15 sheet required gate 和 owner covered pass criteria | P1 |
| Whitebox/golden | 固定 seed 截图证明地图不再空黑、telegraph 可读 | screen matrix 有 label，PR-05 没引用 | `UI/pr/screen-coverage-matrix.md:73-74` | evidence 无法被 PR-07 final index 机械继承 | P2 |

## 玩法与体验审查

### 核心循环

PR-05 是探索与战斗第一屏的视觉替换，直接影响玩家是否能理解“哪里能走、哪里危险、哪里有可交互/掉落/敌人”。当前文档目标正确，但地图 key prefix 错误会导致实际地图仍使用旧资源，核心循环的视觉改善无法落地。

### 战斗体验

Boss telegraph、危险地形和 actor 遮挡是本 PR 最关键的战斗可读性路径。当前文档没有把 ordinary VFX、boss telegraph、actor、ground loot marker 的最终 draw order 写成 exact contract，也没有可执行 test method。这个缺口不能推到 PR-07，因为 PR-05 生成的 VFX/actor 资源尺寸、透明边距和 contrast 会直接决定 telegraph 是否可读。

### 探索与新鲜感

Round 2/3/6 的 tileset、prop 和 zone visual 是“黑底小 tile”能否变成暗黑地牢画面的主要来源。当前缺 exact inventory 和 evidence label，开发可能只完成局部资源替换，截图仍不具备明确探索主题。

### 新手体验与信息反馈

如果 ground/wall/hazard/interactable 的可读性只靠 manual 截图，没有固定 seed、viewport、locale 和 label，新玩家路径中的“危险区域”和“可交互对象”很难稳定验收。

### 系统耦合与体验断层

PR-05 必须只改资源、manifest 和 client presentation，不应改 `core/game` 规则。修复本报告问题时，应优先修正 key/manifest/coverage/test 合同，而不是在 `client` 中临时映射 `tile.* -> tileset.*`，否则会引入第二 authority。

## 当前阶段必须解决的问题

当前 PR 合并前必须修：
- P1-1：地图 key namespace 必须改到 `tileset.*` 或同 PR 显式更新 runtime contract。
- P1-2：PR close owner-scope 必须包含 Round 2-6 全部 required sheet ids。
- P1-3：必须提供 PR-05 exact owner key inventory 或 repo-owned generated inventory artifact。
- P1-4：必须冻结 layer ordering 的 owner test 文件、test methods 和 API 边界。
- P1-5：必须定义 owner-scope 对 old-style/pending/fallback 的 close pass criteria。

不能简单推迟的原因：
- 这些不是 polish，而是资源能否被 runtime 消费、gate 能否证明完整、player-facing telegraph 是否可读的基础合同。
- PR-06/PR-07 只能承接 rejected polish 和 final-full 收口，不应替 PR-05 猜测 Round 2-6 owner inventory 或地图 key contract。

可作为后续 PR 输入：
- P2-8 中非 player-visible pending/rejected cell 的 PR-06/PR-07 handoff。
- P3 标题和证据措辞调整。

## Removal/Iteration Plan

### Defer Removal: PR-05 旧风格 visual path residue

| Field | Details |
| --- | --- |
| Location | `assets-src/image/manifests/phase2-visual-manifest.json` PR-05 owner key entries |
| Phase/Work Package | Dark UI/UX PR-05 -> PR-06 final-full |
| Touched contract | VisualManifest, dark-v1 coverage artifact |
| Evidence | 当前 PR-05 相关 manifest entries 仍全部指向非 `dark-v1/` 输出；PR-05 文档允许部分 non-player-visible key pending |
| Preconditions | PR-05 exact owner key inventory 冻结；owner-scope artifact 能区分 covered/missing/pending/old-style |
| Deletion or iteration steps | 1. PR-05 player-visible owner keys 改为 `dark-v1/` output 2. 非 player-visible pending 写入 handoff 表 3. PR-06/PR-07 final-full 清空 residue |
| Affected harness/gates | `darkKeyRegistryLint`, `darkSpriteSheetLint`, `spriteSheetMapLint`, `darkManifestCoverageLint owner-scope/final-full`, `manifestLint`, `goldenScreenshot` |
| White-box check | `dark-uiux-pr05-map-layer-stack`, `dark-uiux-pr05-actor-boss-telegraph` |
| Rollback or fallback | 按 Round 回滚 raw sheet、sliced PNG、canonical/runtime manifest patch、QA report；不得新增 renderer-side legacy mapping |

## Additional Suggestions

- 把 PR-05 的 exact owner key inventory 生成脚本做成 report-only first：先从 manifest 和 key-registry 生成候选，再人工确认 sheetId/category/consumerTest，避免手写 100+ 行 key 表出错。
- 如果 PR-05 仍允许 mini PR，建议每个 checkpoint 在文档中给出 `requiredOwnerSheetIds` 子集和 expected evidence label，避免 mini PR 只交 raw PNG。
- Zone visual category 迁移建议单独列 `before category -> after category -> footprint/pivot`，因为当前 base zones 仍是 `prop_environment` 且 pivotY 为 `0.08`，直接改 portrait 会影响 UI/renderer 布局假设。

## Open Questions

- `zone.*.visual` 是否统一迁移为 `portrait` category？如果是，PR-05 必须同步声明 footprint/pivot 变化和 consumer tests。
- `boss.*.visual` 是否继续作为 actor sprite，还是转入 portrait/icon family？当前 PR-05 文档只说 boss actor sprite 和 boss icon，没有写 `boss.*.visual` 的归属。
- Ground loot marker 的层级 owner 是 `TileLayerComposer` 还是 `TileRendererCanvasTest`？当前 composer API 不包含 marker，需要文档做一次明确选择。

## Suggested Verification

本轮 review 未运行 Gradle gate，只做了只读文档、代码、manifest 和工具脚本核验。

建议 PR-05 文档修正后先运行：

```bash
git diff --check -- UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md UI/pr/README.md UI/PLAN.md
for f in UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md UI/pr/README.md UI/PLAN.md; do awk 'BEGIN{c=0} /^```/{c++} END{print FILENAME ":FENCE_OPEN=" c%2}' "$f"; done
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n --type md "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md UI/pr/README.md UI/PLAN.md
```

建议 PR-05 开发前置 gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-05 -Pktome.darkUiux.requiredOwnerSheetIds=r02-tiles-ground,r02-tiles-wall,r02-tiles-decal,r03-props-interactable,r03-props-environment,r03-vfx-telegraph,r04-actors-player,r04-actors-humanoid,r04-actors-monster,r04-actors-boss,r05-bestiary-humanoid-icons,r05-bestiary-creature-icons,r05-boss-icons,r06-portraits-classes,r06-portraits-trees,r06-portraits-zones
```

建议 PR-05 实现后 close gate：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew syncPhase2Manifests manifestLint
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-05 -Pktome.darkUiux.requiredOwnerSheetIds=r02-tiles-ground,r02-tiles-wall,r02-tiles-decal,r03-props-interactable,r03-props-environment,r03-vfx-telegraph,r04-actors-player,r04-actors-humanoid,r04-actors-monster,r04-actors-boss,r05-bestiary-humanoid-icons,r05-bestiary-creature-icons,r05-boss-icons,r06-portraits-classes,r06-portraits-trees,r06-portraits-zones
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileLayerComposerTest --tests com.ktome.client.render.TileRendererCanvasTest
./gradlew :client:clientSmoke :client:goldenScreenshot maintainabilityLint verifyChanged
```

## Summary

PR-05 当前不能判定为“开发者无需猜测即可实施”。最大阻塞不是文档长度，而是几个关键合同没冻结：地图 key namespace 与现有 runtime 不一致、Round 2-6 owner-scope 没有 required sheet fail-fast、exact owner key inventory 缺失、layer ordering owner test 不存在且 API 边界不清。修完这些后，PR-05 才能成为真正可执行的资源替换开发文档。

# Dark UI/UX PR-05 Map Actor Portrait Replacement

**阶段**: `dark-uiux-pr05-map-actor-portrait-replacement`
**优先级**: `P1`
**工作量**: `XL`
**前置条件**: PR-01、PR-01-1、PR-02、PR-02-1、PR-02-2 owner evidence、PR-03、PR-04 已按各自 close gate 完成。
**资源生成结论**: 生成 Round 2-6 资源，替换 `tileset / prop / VFX / actor / bestiary icon / portrait` 视觉族。标题中的 portrait replacement 包含 bestiary icon、profession portrait、talent tree portrait 和 zone visual。

## Open Design 辅助参考

开发 PR05 时可在完成本 PR 预检后读取以下辅助设计输入：

1. [K-ToME Dark UI Design Reference For Open Design](../review/open-design/ktome-dark-ui-design.md)：统一 color roles、spacing、component states 与 anti-pattern 语言。
2. [Dark UI/UX PR05 Map Actor Portrait Design Notes](../review/open-design/dark-uiux-pr05-map-actor-portrait-design.md)：辅助 art direction、prompt 变体、contact-sheet QA rubric、silhouette、32px 可读性、telegraph/actor 叠加与 tile/prop/portrait 同时代一致性检查。

这些文档只用于设计理解、review、prompt 草案和 contact-sheet QA 讨论，不能覆盖本 PR 的 sheet-plan、owner inventory、sprite map report、manifest、coverage、golden/manual evidence 或资源生成管线。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再跑 tile / actor resource gate、client layer tests、client evidence 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Preflight Checklist

进入 PR-05 开发前必须先完成以下机械检查。任一项失败时先修上游 PR 或治理文档，不得在 PR-05 中临时绕过。

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint \
  -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl
./gradlew :tools:darkManifestCoveragePr02OwnerScope :tools:darkManifestCoveragePr02_1OwnerScope :tools:darkManifestCoveragePr02_2OwnerScope
```

开发者还必须确认以下 upstream stop conditions。任一 artifact 缺失、字段为空或与当前文档不一致时，PR-05 不得开始 raw sheet 生成。

| Upstream dependency | Required gate or artifact | Stop condition |
| --- | --- | --- |
| PR-01-1 map sublayer owner | `./gradlew :client:test --tests com.ktome.client.render.TileLayerComposerTest --tests com.ktome.client.render.TileRendererCanvasTest` and `UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md` `Frame Ownership Self-Audit` | test class / method 不存在、manual record 缺少 map sublayer order 或 frame ownership self-audit |
| PR-02-1 / PR-02-2 demo shell and first-screen visual evidence | `ui-demo-new-parity-1672x941`, `ui-demo-new-parity-1280x800`, `ui-demo-new-map-stage-crop`, `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md`, `UI/manual-records/ui-demo-new-visual-parity.md`, `:tools:darkManifestCoveragePr02_1OwnerScope`, `:tools:darkManifestCoveragePr02_2OwnerScope` | `ui-demo-new-*` evidence 缺失、right panel ground loot 回归、bottom command hints 回归、`tileset.ruins.*` / `actor.vanguard` / `prop.stairs.down` 的 PR-02-2 owner evidence 缺失 |
| PR-03 map ground loot marker and inventory evidence | `dark-uiux-pr03-equipment-slots`, `dark-uiux-pr03-inventory-empty`, `dark-uiux-pr03-inventory-stacked`, `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md`, `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md` | golden/manual label 不存在、fallback injection record 缺 `coverageReportPath`、right-panel ground loot 回归，或 map ground loot marker 仍没有 focused/canvas evidence |
| PR-04 talent assign / modal evidence | `dark-uiux-pr04-talent-assign-panel-start`, `dark-uiux-pr04-active-slot-choice`, `dark-uiux-pr04-talent-assign-min-window-log-visible`, `dark-uiux-pr04-right-companion-coexistence`, `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` | modal / talent assign evidence 缺失，或 min-window log overlap 仍未关闭 |
| PR-02 resource pipeline | `:tools:darkManifestCoveragePr02OwnerScope` | owner-scope artifact 不含 PR-02 required sheet ids，或 PR-02 tooling diff 尚未收口 |

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI05-M01` | §3 Round 2-6 resource scope | `assets` | `darkSpriteSheetLint`, `spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` | `assetLint`, `styleLint` | `assets-src/image/contact-sheets/dark-v1/<sheetId>-contact.png`, `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` | `required` |
| `UI05-M02` | §4 tile / actor / VFX implementation | `client` | `TileLayerComposerTest`, `TileRendererCanvasTest` | `:client:clientSmoke`, `:client:goldenScreenshot` | `client/build/reports/golden/` | `required` |
| `UI05-M03` | manifest / owner-scope coverage | `tools` | `ManifestResolveTest`, `darkKeyRegistryLint` | `manifestLint`, PR-05 owner-scope command in §7 | `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` | `N/A` |
| `UI05-M04` | §6 必测行为 | `client` | map layer and render focused tests | `:client:clientSmoke`, `:client:goldenScreenshot` | `client/build/reports/tests/test/classes/com.ktome.client.render.TileLayerComposerTest.html`, `client/build/reports/tests/test/classes/com.ktome.client.render.TileRendererCanvasTest.html` | `required` |
| `UI05-M05` | governance inheritance | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |

### Gate Budget

预计重型任务：resource lint 全套、PR-05 owner-scope `darkManifestCoverageLint`、`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-05 替换地图、actor、portrait、bestiary icon 和 VFX 主视觉，是资源面最大的 dark UI PR。

执行预算：

1. 开发循环只跑当前 checkpoint 的 resource lint、focused resolver/layer test 和当前 sheet contact QA。
2. 每个 checkpoint 结束跑当前 Round 的 `requiredOwnerSheetIds` 子集、targeted golden label 和 manual record 追加。
3. PR close 跑 Round 2-6 全 16 张 sheet 的 required owner scope、`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged`。
4. 读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}` 作为最近耗时来源；文件不存在时在 PR 描述写 `first-run no baseline`，不得伪造 duration。
5. 同一重型 gate 失败超过 `2` 次或单轮验证超过 `90` 分钟时，先补 focused test / resource lint 或回滚当前 checkpoint 文件族，再重跑。

### Canonical Artifact

| Artifact family | Required repo-relative path |
| --- | --- |
| Owner inventory | `UI/sprite-sheets/pr05-owner-key-inventory.json` and generated readable summary `UI/sprite-sheets/pr05-owner-key-inventory.md` |
| Sheet plan / registry | `UI/sprite-sheets/sheet-plan.yaml`, `UI/sprite-sheets/key-registry.yaml` |
| Prompts | `UI/sprite-sheets/prompts/dark-v1/<order>-<sheetId>.prompt.txt`, `UI/sprite-sheets/prompts/dark-v1/prompt-index.json` |
| Raw sheets | `assets-src/image/raw/sheets/dark-v1/<sheetId>.png` |
| Contact sheets | `assets-src/image/contact-sheets/dark-v1/<sheetId>-contact.png` |
| Runtime PNG | `client/src/main/resources/dark-v1/**/<targetKey-derived-name>.png` |
| Canonical manifest | `assets-src/image/manifests/phase2-visual-manifest.json` |
| Runtime manifest | `client/src/main/resources/manifests/visual-manifest.json`, generated only by `syncPhase2Manifests` |
| Sprite map report | `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` |
| Coverage report | `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` |
| Golden / manual evidence | `client/build/reports/golden/`, `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md` |

contact sheet 未确认前不得把 runtime PNG 视为稳定合同。任何 committed artifact、manual record、report 或 fixture 都必须使用 repo-relative path；Codex CLI transient source 只能写摘要标签，不得写机器绝对路径。

### Failure Rule

地图层级或资源 coverage 失败时先修 sheet mapping、manifest key 或 layer contract；不得通过降低 golden 覆盖、保留旧风格 residue、增加 renderer-side key alias 或跳过 owner-scope coverage 来通过。

## 1. 阶段目标

1. 替换地图核心视觉：ground、wall、terrain decal、prop、interactable、VFX、telegraph。
2. 替换 player、职业、怪物、Boss actor sprite。
3. 替换 bestiary monster icon、boss icon、职业 portrait、天赋树 portrait、区域 visual。
4. 在 PR-02-1 `UI-demo-new` 首屏基础上，把正式地图、actor、portrait、bestiary、VFX 家族扩展到全局暗黑地牢视觉；不得退回黑底程序网格。
5. 保持 PR-05 只改资源、manifest 和 client presentation；不改变地图生成、战斗、AI、Boss 或掉落规则。
6. 技能/铭刻释放必须保持 ToME-like map-first targeting：热键直达地图瞄准，地图层显示 cursor、合法目标/非法 hover 标记，侧栏和底部动作条显示当前技能与确认提示；不得在 `TARGET` 阶段生成遮挡战场的居中 combat decision 弹窗。
7. 右侧装备、铭刻与背包 companion 面板必须支持低摩擦详情：鼠标 hover 直接显示装备/铭刻/物品详情；背包选中物品直接显示详情与同槽装备属性差异；`PageUp/PageDown` 在背包上下文用于翻页，不得被 map 移动抢占。

## 2. 影响范围

### Added

| 文件族 | 用途 |
| --- | --- |
| `scripts/generate_dark_uiux_pr05_owner_inventory.py` | report-only inventory generator；先从 manifest / sheet plan / key registry 生成候选，再人工确认 per-key source decision |
| `UI/sprite-sheets/pr05-owner-key-inventory.{json,md}` | PR-05 exact owner key inventory，冻结每个 cell 的 target key、consumer、category、row/col 和 evidence |
| `UI/sprite-sheets/prompts/dark-v1/*-r02-*.prompt.txt` 到 `*-r06-*.prompt.txt` | Round 2-6 prompt 文件和 prompt index |
| `assets-src/image/raw/sheets/dark-v1/r02-*.png` 到 `r06-*.png` | Round 2-6 raw sheet |
| `assets-src/image/contact-sheets/dark-v1/r02-*-contact.png` 到 `r06-*-contact.png` | contact sheet QA |
| `client/src/main/resources/dark-v1/tiles/**`、`props/**`、`vfx/**`、`actors/**`、`icons/**`、`portraits/**` | 切分后的 runtime PNG |
| `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` | PR-05 sprite map report |
| `client/src/test/kotlin/com/ktome/client/render/TileLayerComposerTest.kt` | visual placement layer owner test |
| `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md` | manual whitebox record |

### Modified

| 文件族 | 用途 |
| --- | --- |
| `UI/sprite-sheets/sheet-plan.yaml` | 增加 Round 2-6 sheet |
| `UI/sprite-sheets/key-registry.yaml` | 增加 PR-05 owner entries |
| `assets-src/image/manifests/phase2-visual-manifest.json` | 更新 PR-05 target key 到 `dark-v1/` output |
| `client/src/main/resources/manifests/visual-manifest.json` | 只能由 `syncPhase2Manifests` 同步生成 |
| `client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt` | 仅当现有 placement order 无法表达 PR-05 layer contract 时修改 |
| `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | 增加 marker/telegraph canvas wiring 断言 |
| `UI/pr/screen-coverage-matrix.md` | 仅当新增或改名 golden label 时同步 |

### Deleted

`N/A`。PR-05 不删除旧视觉文件作为主要交付。旧风格 residue 的清理只能通过 manifest target 替换、PR-06 rejected polish 或 PR-07 final-full 收口完成，不能删除仍被 runtime manifest 消费的历史文件。

## 3. 资源范围

PR close required sheet set 固定为 16 张 sheet。后文只引用本 code block，不再维护第二份数量口径：

```text
r02-tiles-ground,r02-tiles-wall,r02-tiles-decal,r03-props-interactable,r03-props-environment,r03-vfx-telegraph,r04-actors-player,r04-actors-humanoid,r04-actors-monster,r04-actors-boss,r05-bestiary-humanoid-icons,r05-bestiary-creature-icons,r05-boss-icons,r06-portraits-classes,r06-portraits-trees,r06-portraits-zones
```

Per-round checkpoint：

| Checkpoint | Sheet IDs | Manifest key scope | 必填证据 | 回滚文件族 |
| --- | --- | --- | --- | --- |
| Round 2 Tiles | `r02-tiles-ground`, `r02-tiles-wall`, `r02-tiles-decal` | `tileset.<tilesetId>.ground_01`, `tileset.<tilesetId>.wall_01`, `vfx.terrain.interaction.*` | contact sheet QA、Round 2 owner-scope coverage summary、`dark-uiux-pr05-map-layer-stack` golden | Round 2 raw sheet、sliced PNG、canonical/runtime manifest patch、QA report |
| Round 3 Props/VFX | `r03-props-interactable`, `r03-props-environment`, `r03-vfx-telegraph` | `prop.*`, `vfx.zone.effect.*`, `vfx.boss.warning.*`, `vfx.telegraph.warning.*`, `vfx.boss.variant.*` | interactable/telegraph screenshot、coverage summary、layering test | Round 3 raw sheet、sliced PNG、manifest patch、QA report |
| Round 4 Actors | `r04-actors-player`, `r04-actors-humanoid`, `r04-actors-monster`, `r04-actors-boss` | `actor.player`, release playable actors, humanoid actor families, monster actor families, `actor.boss.*` | actor silhouette screenshot、boss telegraph overlap check、coverage summary | Round 4 raw sheet、sliced PNG、manifest patch、QA report |
| Round 5 Bestiary | `r05-bestiary-humanoid-icons`, `r05-bestiary-creature-icons`, `r05-boss-icons` | `icon.monster.*`, `boss.*.icon` | bestiary contact sheet QA、humanoid/creature/boss icon manifest diff、resolver test | Round 5 raw sheet、sliced PNG、manifest patch、QA report |
| Round 6 Portraits | `r06-portraits-classes`, `r06-portraits-trees`, `r06-portraits-zones` | `portrait.*`, `tree.*`, `zone.*.visual`, `zone.secret.*.visual` | profession/tree/zone portrait contact sheet QA、UI screenshot、category / pivot audit | Round 6 raw sheet、sliced PNG、manifest patch、QA report |

每个 checkpoint 都必须保持主干可玩。失败时只能回滚当前 checkpoint 的 sheet/manifest/report 文件族，不能拖累已通过 checkpoint。`zone.*.visual` 统一归 Round 6 `r06-portraits-zones`；Round 3 只处理可交互、环境 prop 和 VFX/telegraph，不承接 zone portrait/large visual。

### Owner Key Inventory

PR-05 正式生成资源前必须先 materialize exact owner inventory。该 inventory 是开发输入，不是 reviewer 临场推测；后续 `key-registry.yaml`、`sheet-plan.yaml`、canonical manifest、resolver test 和 coverage artifact 必须与它一致。

PR-02-2 已拥有 `tileset.ruins.ground_01`、`tileset.ruins.wall_01`、`actor.vanguard`、`prop.stairs.down` 这 4 个 `ui-demo-new` 首屏 key。PR-05 不能在 owner inventory 中再次声明它们为 `ownerPr=PR-05`；如确实需要迁移 owner，必须先更新 PR-02-2 owner contract、PR-02-1/PR-02-2 coverage task、`ui-demo-new-*` golden/manual evidence 和本节 source rules，不能只改 registry 一处。

Inventory 必须是 per-key flat entries，而不是 sheet-level `targetKeys` 聚合。这样同一份 JSON 可以直接映射到 `key-registry.yaml` 和 `sheet-plan.yaml` cell 输入字段。

Inventory schema 固定为：

```json
{
  "schemaVersion": "dark-uiux-pr05-owner-key-inventory-v1",
  "ownerPr": "PR-05",
  "generatedFrom": [
    "assets-src/image/manifests/phase2-visual-manifest.json",
    "UI/sprite-sheets/sheet-plan.yaml",
    "UI/sprite-sheets/key-registry.yaml"
  ],
  "requiredOwnerSheetCount": 16,
  "requiredOwnerSheetIds": [
    "r02-tiles-ground",
    "r02-tiles-wall",
    "r02-tiles-decal",
    "r03-props-interactable",
    "r03-props-environment",
    "r03-vfx-telegraph",
    "r04-actors-player",
    "r04-actors-humanoid",
    "r04-actors-monster",
    "r04-actors-boss",
    "r05-bestiary-humanoid-icons",
    "r05-bestiary-creature-icons",
    "r05-boss-icons",
    "r06-portraits-classes",
    "r06-portraits-trees",
    "r06-portraits-zones"
  ],
  "entries": [
    {
      "targetKey": "tileset.forest_edge.ground_01",
      "sheetId": "r02-tiles-ground",
      "row": 0,
      "col": 0,
      "category": "tile_ground",
      "outputName": "dark-v1/tiles/tileset_forest_edge_ground_01.png",
      "fallbackKey": "missing_visual",
      "consumer": "FoundationGameSession.terrainVisualKey -> TileRenderModelBuilder",
      "consumerTest": "ManifestResolveTest.darkUiuxPr05TilesetOwnerKeysResolveThroughExactEntries",
      "aliasOf": null,
      "playerVisible": true,
      "ownerPr": "PR-05",
      "sourceManifestPath": "assets-src/image/manifests/phase2-visual-manifest.json",
      "sourceDecision": "runtime terrain key emitted as ${zone.tilesetKey}.ground_01"
    }
  ]
}
```

Materialization command：

```bash
python3 scripts/generate_dark_uiux_pr05_owner_inventory.py \
  --manifest assets-src/image/manifests/phase2-visual-manifest.json \
  --sheet-plan UI/sprite-sheets/sheet-plan.yaml \
  --key-registry UI/sprite-sheets/key-registry.yaml \
  --out UI/sprite-sheets/pr05-owner-key-inventory.json \
  --summary UI/sprite-sheets/pr05-owner-key-inventory.md
```

`scripts/generate_dark_uiux_pr05_owner_inventory.py` 是 report-only generator。它不得写 manifest、sheet plan 或 runtime PNG；人工确认只允许改 `sourceDecision`、`row/col`、`aliasOf` 和 `playerVisible`，不得改变 `targetKey` 所属 sheet 而不回写本节 source rules。

Inventory source rules：

| sheetId | required targetKey set source | category | consumer | owner test / evidence |
| --- | --- | --- | --- | --- |
| `r02-tiles-ground` | exact PR-05 owner keys: `tileset.forest_edge.ground_01`, `tileset.mine.ground_01`, `tileset.shadow_depths.ground_01`; `tileset.ruins.ground_01` remains upstream PR-02-2 unless the migration rule above is executed | `tile_ground` | `FoundationGameSession.terrainVisualKey`, `TileRenderModelBuilder` | `ManifestResolveTest.darkUiuxPr05TilesetOwnerKeysResolveThroughExactEntries`, `dark-uiux-pr05-map-layer-stack` |
| `r02-tiles-wall` | exact PR-05 owner keys: `tileset.forest_edge.wall_01`, `tileset.mine.wall_01`, `tileset.shadow_depths.wall_01`; `tileset.ruins.wall_01` remains upstream PR-02-2 unless the migration rule above is executed | `tile_wall` | same as above | same as above |
| `r02-tiles-decal` | exact keys: `vfx.terrain.interaction.ice`, `vfx.terrain.interaction.oil`, `vfx.terrain.interaction.oil_burning`, `vfx.terrain.interaction.water` | `tile_decal` | `FoundationGameSession.terrainVisualKey`, overlay/terrain renderer | `TileLayerComposerTest.composesTerrainPropsVfxTelegraphBeforeActors` |
| `r03-props-interactable` | exact PR-05 owner keys: `prop.alarm_bonfire`, `prop.armory_gate`, `prop.crystal_resonance_node`, `prop.heart_ward_focus`, `prop.hidden_entrance.return_bridge`, `prop.hidden_entrance.revealed`, `prop.ritual_altar`, `prop.river_ferry_anchor`, `prop.stairs.up`, `prop.supply_crate`, `prop.temple_ward_reliquary`; `prop.stairs.down` remains upstream PR-02-2 unless the migration rule above is executed | `prop_interactable` | map prop visual placements | `TileRendererCanvasTest.rendersPr05InteractablePropsWithDarkManifestEntries` |
| `r03-props-environment` | exact keys: `prop.mine_furnace` | `prop_environment` | map prop visual placements | same as above |
| `r03-vfx-telegraph` | `vfx_plate` exact keys: `vfx.boss.warning.sigil_01`, `vfx.telegraph.warning.sigil_01`, `vfx.boss.variant.abyssal_eclipse`, `vfx.boss.variant.grey_crown`, `vfx.boss.variant.molten_glass`; `tile_decal` exact keys: `vfx.zone.effect.crystal_shard_01`, `vfx.zone.effect.current_lane_01`, `vfx.zone.effect.void_pressure_01`, `vfx.zone.effect.ward_seal_01` | per-entry category must match this split; no mixed-category ambiguity in inventory entries | overlay visual placements / zone effect placements | `TileLayerComposerTest.keepsBossTelegraphAboveOrdinaryVfx`, `dark-uiux-pr05-actor-boss-telegraph` |
| `r04-actors-player` | exact PR-05 owner keys currently in manifest: `actor.player`, `actor.arcanist`, `actor.rogue`, `actor.templar`; `actor.vanguard` remains upstream PR-02-2 unless the migration rule above is executed；add `actor.berserker` / `actor.spellblade` only if PR-05 first adds canonical manifest entries for dev playable actors | `actor_sprite` | `snapshot.actors[].visualKey` | `TileRendererCanvasTest.keepsPr05ActorSpritesReadableOnDarkMap` |
| `r04-actors-humanoid` | exact family prefixes: `actor.bandit.*`, `actor.cultist.*`, `actor.orc.*`, `actor.warded_ruin.*`; inventory must expand these prefixes into explicit `entries[]` before resource generation | `actor_sprite` | actor visual placements | same as above |
| `r04-actors-monster` | exact family prefixes: `actor.beast.*`, `actor.undead.*`, `actor.abyssal.*`, `actor.crystal.*`, `actor.forge.*`, `actor.river.*`; inventory must expand these prefixes into explicit `entries[]` before resource generation | `actor_sprite` | actor visual placements | same as above |
| `r04-actors-boss` | exact keys: `actor.boss.*`, `boss.abyssal.guardian.visual`, `boss.cultist.dungeon_lord.visual`, `boss.orc.molten_giant.visual` | `actor_sprite` | boss actor visual placements and `game/src/main/resources/data/bosses/index.yaml` `visualKey` | `TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell` |
| `r05-bestiary-humanoid-icons` | exact family prefixes: `icon.monster.bandit.*`, `icon.monster.cultist.*`, `icon.monster.orc.*`, `icon.monster.warded_ruin.*`; inventory must expand to explicit keys | `icon` | bestiary / codex icon resolver | `ManifestResolveTest.darkUiuxPr05BestiaryIconsResolveThroughExactEntries` |
| `r05-bestiary-creature-icons` | exact family prefixes: `icon.monster.beast.*`, `icon.monster.undead.*`, `icon.monster.abyssal.*`, `icon.monster.crystal.*`, `icon.monster.forge.*`, `icon.monster.river.*`; inventory must expand to explicit keys | `icon` | bestiary / codex icon resolver | same as above |
| `r05-boss-icons` | exact keys: `boss.abyssal.guardian.icon`, `boss.cultist.dungeon_lord.icon`, `boss.orc.molten_giant.icon` | `icon` | boss/bestiary icon resolver and `game/src/main/resources/data/bosses/index.yaml` `iconKey` | same as above |
| `r06-portraits-classes` | exact keys: `portrait.arcanist`, `portrait.rogue`, `portrait.templar`, `portrait.vanguard` | `portrait` | profession / class portrait UI | `ManifestResolveTest.darkUiuxPr05PortraitKeysResolveThroughExactEntries` |
| `r06-portraits-trees` | exact prefix: `tree.*`; inventory must expand all current canonical `tree.*` keys into explicit `entries[]` | `portrait` | talent tree UI | same as above |
| `r06-portraits-zones` | exact prefix: `zone.*.visual`; inventory must expand all current canonical `zone.*.visual` and `zone.secret.*.visual` keys into explicit `entries[]` | `portrait` unless a listed key remains `prop_environment` by explicit category audit | zone / route / hidden content UI | `ManifestResolveTest.darkUiuxPr05ZoneVisualKeysResolveThroughExactEntries` |

Inventory pass criteria：

1. Every non-reserved PR-05 cell has `targetKey / category / ownerPr / sheetId / fallbackKey / consumer / consumerTest / aliasOf`.
2. `ownerPr` is exactly `PR-05` for keys that PR-05 must close. Keys intentionally deferred to PR-06 or PR-07 must not be left as PR-05 owner keys.
3. PR-05 close gate requires `requiredOwnerSheetCount=16`, `ownerCoveredKeys == ownerExpectedKeys`, `ownerMissingKeys=[]`, `ownerPendingKeys=[]`, `ownerOldStyleKeys=[]`, `allowedOwnerFallbackKeys=[]`, and `ownerSheetIds` covering all 16 required sheet ids.
4. `fallbackKey=missing_visual` is allowed only before runtime PNG acceptance. It must not survive PR close for PR-05 owner keys.
5. Do not create renderer-side aliases such as `tile.* -> tileset.*`; the runtime contract is the manifest key already emitted by game/session snapshot.

### Raw Sheet 生成交接

1. 每个 checkpoint 开始前，只为当前 Round 生成 prompt 文件和 `prompt-index.json` 子集，不一次性要求生成 Round 2-6 全部 raw sheet。
2. 开发者通过 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --smoke-report <buildReportPath> --overwrite` 执行当前 checkpoint 的 prompt，并由脚本将 Codex CLI 最新生成图片复制到对应 `sheet-plan.yaml.rawSheetPath`。
3. `--smoke-report` 只能写到 `build/reports/verification/dark-uiux/codex-image-smoke-<sheetId>.json`，不提交。
4. committed manual record 只能记录 `promptPath`、`rawSheetPath`、`rawSheetHash`、`sourceFolderLabel=<codex-generated-images-dir>/<session-summary>`、`sourceImageName`、`contactSheetPath`、`spriteMapReportPath` 和 coverage summary path。
5. 文件名必须等于 `{sheetId}.png`，例如 `r02-tiles-ground.png`、`r04-actors-boss.png`。
6. 当前 checkpoint 的 raw PNG 全部通过尺寸、grid、hash 和路径校验后，才能切分 runtime PNG。
7. 每个 checkpoint 的 PR 描述或阶段记录必须列出 prompt path、raw sheet path、raw sheet hash、transient source 摘要、contact sheet QA path 和 coverage summary。
8. 文档、manual record 和 committed artifact 自检必须执行绝对路径扫描，禁止把机器路径当合同。

## 4. 实现任务

### Implementation Order

1. Materialize `UI/sprite-sheets/pr05-owner-key-inventory.{json,md}`，人工确认每个 key 的 `sheetId/row/col/category/outputName/consumerTest/playerVisible/sourceDecision`。
2. 更新 `sheet-plan.yaml` 和 `key-registry.yaml`，确保 16 张 sheet 与 owner inventory 一致。
3. 按 Round 2 -> 6 生成 prompt、raw sheet、contact sheet；每轮先完成 QA，再切分 runtime PNG。
4. 更新 canonical manifest；随后只用 `syncPhase2Manifests` 生成 runtime manifest。
5. 补 resolver tests、layer tests、canvas tests 和 golden/manual evidence。
6. 跑当前 checkpoint owner-scope subset；所有 checkpoint 通过后跑 PR close gate。

### Layer Contract

PR-05 选择以下 layer authority，避免开发者临场决定：

1. `TileLayerComposer` 只拥有 `TileVisualPlacement` 顺序：`terrainTiles -> propTiles -> overlayTiles -> actorTiles`。PR-05 可新增 `TileLayerComposerTest` 固定该顺序，并断言 boss telegraph overlay 在 actor 前进入 placement stack。
2. `groundLootMarkers` 当前不是 `TileVisualPlacement`，不纳入 `TileLayerComposer.compose` 输出。它与 terrain、actor、boss telegraph 的最终绘制关系由 `TileRendererCanvasTest` 承接。
3. 当前 actor ordering 合同不是通用 Y-sort。PR-05 只能锁定现有不变量：actor placements 位于 overlay placements 之后，且 `TileRenderModelBuilder` 使用 `sortedBy { if (actor.isPlayer) 1 else 0 }` 保证 player actor 在相同 placement priority 下后绘制。若要改为真实 `y/x/entityId` sort，必须作为独立 renderer contract 变更先更新 PR-01-1 layer owner 文档和 focused tests。
4. 如果实现决定把 ground loot marker 迁入 composer，必须同 PR 修改 composer API、迁移测试 owner，并在本文档回写新的 ordering contract；不得只在 renderer 中写一次性 draw-order hack。

推荐最小 test names：

```text
TileLayerComposerTest.composesTerrainPropsVfxTelegraphBeforeActors
TileLayerComposerTest.keepsBossTelegraphAboveOrdinaryVfx
TileRendererCanvasTest.drawsGroundLootMarkerAboveTerrainAndBelowBlockingActorBadge
TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell
TileRendererCanvasTest.rendersPr05InteractablePropsWithDarkManifestEntries
ManifestResolveTest.darkUiuxPr05TilesetOwnerKeysResolveThroughExactEntries
ManifestResolveTest.darkUiuxPr05BestiaryIconsResolveThroughExactEntries
ManifestResolveTest.darkUiuxPr05PortraitKeysResolveThroughExactEntries
```

## 5. 非目标

1. 不改地图生成、碰撞、AI、Boss 规则。
2. 不改 profession tree UI 行为；只替换其 portrait/tree visual。
3. 不引入 atlas 或 region manifest，无例外；性能疑虑只记录到 PR-07，由 PR-07 决定是否另开 atlas PR。
4. 不改音频资源。
5. 不新增 `tile.ground.* / tile.wall.* / tile.decal.*` 这类 runtime 当前不消费的正式 key。

## 6. 必测行为

1. 地面、墙、危险地表、可交互 prop、actor 层级清楚。
2. Boss warning / telegraph 不被普通 VFX 淹没。
3. actor 不遮挡地面掉落 marker 和职业树/modal 层。
4. 地图在暗色背景下仍能区分可走、不可走、可交互和危险区域。
5. actor placement order、ground loot marker 与 boss telegraph 的先后关系必须由 §4 中的 layer owner tests 和 canvas tests 锁定；PR-05 不新增真实 Y-sort 规则。
6. PR-05 owner keys 全部解析到 `dark-v1/` output；不得保留 `phase2/`、`phase3/`、`phase4/`、`debug/`、`missing_visual` 或 pending output。

## 7. 验证

所有 Gradle 命令前必须执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

manifest 同步顺序固定为先同步再 lint。`client/src/main/resources/manifests/visual-manifest.json` 不允许手改。

```bash
./gradlew syncPhase2Manifests manifestLint
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint \
  -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl
```

Round 2 checkpoint example：

```bash
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=owner-scope \
  -Pktome.darkUiux.ownerPr=PR-05 \
  -Pktome.darkUiux.requiredOwnerSheetIds=r02-tiles-ground,r02-tiles-wall,r02-tiles-decal
```

PR close owner-scope gate：

```bash
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=owner-scope \
  -Pktome.darkUiux.ownerPr=PR-05 \
  -Pktome.darkUiux.requiredOwnerSheetIds=r02-tiles-ground,r02-tiles-wall,r02-tiles-decal,r03-props-interactable,r03-props-environment,r03-vfx-telegraph,r04-actors-player,r04-actors-humanoid,r04-actors-monster,r04-actors-boss,r05-bestiary-humanoid-icons,r05-bestiary-creature-icons,r05-boss-icons,r06-portraits-classes,r06-portraits-trees,r06-portraits-zones
```

Focused client tests：

```bash
./gradlew :client:test \
  --tests com.ktome.client.assets.ManifestResolveTest \
  --tests com.ktome.client.render.TileLayerComposerTest \
  --tests com.ktome.client.render.TileRendererCanvasTest
```

Close gate：

```bash
./gradlew :client:clientSmoke :client:goldenScreenshot maintainabilityLint verifyChanged
```

PR close artifact 必须满足：

1. `scopeMode=owner-scope`
2. `ownerPr=PR-05`
3. `requiredOwnerSheetCount=16`
4. `requiredOwnerSheetIds` 等于 16 张 required sheet set
5. `ownerSheetIds` 覆盖 16 张 required sheet set
6. `ownerExpectedKeys` 非空
7. `ownerCoveredKeys == ownerExpectedKeys`
8. `ownerMissingKeys=[]`
9. `ownerPendingKeys=[]`
10. `ownerOldStyleKeys=[]`
11. `allowedOwnerFallbackKeys=[]`

不得用裸 `darkManifestCoverageLint` 代替本 PR close gate。

## 8. 人工白盒

PR-05 至少产出以下 evidence。若 seed 或 scenario 后续调整，必须同时更新本表、manual record 和 screen coverage matrix。

| label | scenario / seed | viewport / locale | must contain | artifact | owner test |
| --- | --- | --- | --- | --- | --- |
| `dark-uiux-pr05-map-layer-stack` | `scenarioId=dark-uiux-pr05-map-layer-stack`, `seed=202605090501` | `1280x800`, `zh-CN`; optional `en-US` parity | ground/wall/decal/prop/actor/loot marker all visible and readable | `client/build/reports/golden/dark-uiux-pr05-map-layer-stack.*`, `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md` | `TileLayerComposerTest.composesTerrainPropsVfxTelegraphBeforeActors`, `TileRendererCanvasTest.drawsGroundLootMarkerAboveTerrainAndBelowBlockingActorBadge` |
| `dark-uiux-pr05-actor-boss-telegraph` | `scenarioId=dark-uiux-pr05-actor-boss-telegraph`, `seed=202605090502` | `1280x800`, `zh-CN`; optional `en-US` parity | boss actor, boss warning, ordinary VFX and telegraph overlap remain readable | `client/build/reports/golden/dark-uiux-pr05-actor-boss-telegraph.*`, same manual record | `TileLayerComposerTest.keepsBossTelegraphAboveOrdinaryVfx`, `TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell` |

新增或更新 scenario 时必须修改：

1. `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`
2. `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt`
3. `game/src/main/resources/i18n/zh-CN.json`
4. `game/src/main/resources/i18n/en-US.json`
5. `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
6. `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`

Manual record 必填字段：

```text
label
scenarioId
seed
viewport
locale
ownerPr
requiredOwnerSheetIds
coverageReportPath
spriteMapReportPath
contactSheetPaths
goldenArtifactPath
rawSheetHashes
sourceFolderLabel
sourceImageName
result
knownLimitations
```

路径自检：

```bash
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl
```

## 9. 回滚边界

1. PR-05 允许按 Round 拆成内部批次或 mini PR，但每个 Round 内必须保持 raw sheet、切分 PNG、manifest patch、contact sheet QA、coverage summary 原子提交。
2. 如果 Round 5 bestiary 失败，不回滚已通过的 Round 2 tile；只回滚对应 sheetId、manifest entries 和 QA report。
3. 如果 Round 4 actor/map baseline 失败，不得继续进入 Round 5 bestiary；先修复或回滚 Round 4 文件族。
4. PR-05 close 不允许 PR-05 owner key pending。确需延期的非 player-visible key 必须从 PR-05 owner inventory 移出，并在 handoff 表声明 future owner。

Deferred / Rejected Cell Handoff：

| targetKey | sourceSheetId | qaStatus | rejectionReason | playerVisible | ownerPr | polishOwner | evidencePath |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `N/A` until a real rejected cell exists | `N/A` | `N/A` | `N/A` | `N/A` | `PR-05` | `N/A` | `N/A` |

Handoff rules：

1. The table schema must match PR-06 `Cross-PR handoff input`; do not use `removalOwner`, `crossPrDependency` or free-form future owner fields.
2. `playerVisible=true` rows must name one concrete `polishOwner` and a repo-relative `evidencePath`; no `PR-06 or PR-07` ambiguity is allowed.
3. PR-05 close still requires no PR-05 owner `pending` / `old-style` / `missing` keys. Handoff rows are for rejected polish cells after QA, not for hiding incomplete PR-05 owner coverage.

Rollback must never add a renderer-side legacy mapping, weaken `darkManifestCoverageLint`, delete golden coverage, or mark `ownerCoveredKeys` manually.

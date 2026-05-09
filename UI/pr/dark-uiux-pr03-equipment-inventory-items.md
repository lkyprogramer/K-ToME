# Dark UI/UX PR-03 Equipment Inventory Items And Shop

**阶段**: `dark-uiux-pr03-equipment-inventory-items`
**优先级**: `P0`
**工作量**: `L`
**前置条件**: PR-02 完成。
**资源生成结论**: 生成 Round 7 的 item/equipment/material/affix 资源子集。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再按资源真源、client presenter/renderer、input/modal、golden/manual 和最终 `verifyChanged` 串行闭环。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI03-M01` | §3 Round 7 sheet cell inventory | `assets` | `darkSpriteSheetLint`, `spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl` | `assetLint`, `styleLint` | `UI/sprite-sheets/sheet-plan.yaml`, `assets-src/image/contact-sheets/dark-v1/`, `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl` | `required` |
| `UI03-M02` | §4 key registry / manifest coverage | `tools` | `darkKeyRegistryLint`, `ManifestResolveTest` | `syncPhase2Manifests`, `manifestLint`, `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03 -Pktome.darkUiux.requiredOwnerSheetIds=r07-items-base,r07-items-unique-artifact,r07-items-affix-material` | `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` | `N/A` |
| `UI03-M03` | §6.1 equipment slots layout / identity | `client` | `EquipmentInventoryPresenterTest`, `TileRendererCanvasTest` | `:client:clientSmoke`, `:client:goldenScreenshot` | `client/build/reports/golden/` label `dark-uiux-pr03-equipment-slots`; golden index entry required | `required` |
| `UI03-M04` | §6.2 inventory grid / tooltip / fallback | `client` | `EquipmentInventoryPresenterTest`, `DescriptionPresenterTest`, `ManifestResolveTest` | `:client:clientSmoke`, `:client:goldenScreenshot` | `client/build/reports/golden/` labels `dark-uiux-pr03-inventory-empty`, `dark-uiux-pr03-inventory-stacked`; golden index entries required | `required` |
| `UI03-M05` | §6.3 shop buy/sell cards | `client` | `ModalCardModelTest`, `DescriptionPresenterTest`, `InputHandlerTest` | `:client:clientSmoke`, `:client:goldenScreenshot` | `client/build/reports/golden/` label `dark-uiux-pr03-inscription-shop`; golden index entry required | `required` |
| `UI03-M06` | §6.4 inscription replacement modal input | `client` | `InputHandlerTest`, `ModalCardModelTest` | `:client:clientSmoke`, `:client:goldenScreenshot` | `client/build/reports/golden/` label `dark-uiux-pr03-shop-full-slot-replace`; golden index entry required | `required` |
| `UI03-M07` | §7 validation / locale / contract gates | `client` / `tools` | `localeLint`, `contractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |
| `UI03-M08` | §8 manual white-box and fallback injection | `docs` / `client` | manual evidence checklist | debug whitebox record | `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md`, `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md` | `required` |

### Gate Budget

预计重型任务：`assetLint`、`styleLint`、`manifestLint`、dark sprite / registry lints、`:client:clientSmoke`、`:client:goldenScreenshot`、`localeLint`、`contractLint`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-03 同时改 item resources、inventory UI、铭文商店、tooltip / empty state 和 manifest coverage。最近耗时读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}`；同一重型 gate 失败超过 2 次时先补 focused test 或 resource lint 断言，再重跑。

### Canonical Artifact

canonical artifact 固定为：

1. `UI/sprite-sheets/sheet-plan.yaml`
2. `UI/sprite-sheets/key-registry.yaml`
3. `UI/sprite-sheets/prompts/dark-v1/`
4. `assets-src/image/raw/sheets/dark-v1/r07-items-base.png`
5. `assets-src/image/raw/sheets/dark-v1/r07-items-unique-artifact.png`
6. `assets-src/image/raw/sheets/dark-v1/r07-items-affix-material.png`
7. `assets-src/image/contact-sheets/dark-v1/`
8. `assets-src/image/manifests/phase2-visual-manifest.json`
9. `client/src/main/resources/manifests/visual-manifest.json`
10. `client/src/main/resources/dark-v1/`
11. `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`
12. `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`
13. `client/build/reports/golden/`
14. `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md`
15. `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md`

fallback key injection record 固定字段：`scenarioId`、`injectedMissingKey`、`fallbackKey`、`sourceManifestPath`、`runtimeManifestPath`、`evidenceLabel`、`screenshotPath` 或 `reportPath`、`result`、`residualRisk`。所有路径必须 repo-relative。

### Failure Rule

inventory/shop golden 或 coverage 失败时先修 presenter、resource key 或 manifest mapping；不得用 fallback key 掩盖缺资源，也不得只靠人工白盒证明 grid / tooltip / shop replacement 行为。canonical manifest 变更后必须先 `syncPhase2Manifests` 再跑 resolver、golden 和 `verifyChanged`，避免用旧 runtime manifest 验收新资源合同。

## 1. 阶段目标

1. 右侧玩家面板正式展示装备格、背包 grid、资源计数。
2. 装备、物品、材料、affix 图标化，替换第一张截图里纯文字装备列表。
3. 空背包、空装备、不可用物品、tooltip 与选中态有统一表现。
4. 铭文商店 / shop 进入同一装备物品 UX family：buy/sell 双列、offer card、价格、affordability、空态、tooltip、满槽替换 modal 必须统一暗黑化。
5. Round 7 分批生成 item/equipment/material/affix sheet，但不修改掉落、商店经济或铭文规则。

本 PR 默认不扩展 `InventoryEntrySnapshot`、`ShopOfferSnapshot` 或 `ShopPanelSnapshot`。因此真实 stack count、purchased state、shop threatened state、pre-rendered disabled reason 不是 blocking requirement；如果实现期确实要交付这些状态，必须在本 PR 内显式新增 typed snapshot / presentation field、声明 version/schema 影响，并补对应 owner test。

## 2. 影响范围

| Action | 路径 / 合同 | 预期改动 |
| --- | --- | --- |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | 补装备/背包 presentation model；不能把规则判断写进 renderer |
| Modified | `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | 绘制装备格、背包 grid、tooltip、shop card 和 replacement modal |
| Modified | `client/src/main/kotlin/com/ktome/client/render/EquipmentSlotLabels.kt` | 只维护 `slotId -> labelKey/localized label`，不维护 icon key 或 frame key |
| Added | `client/src/main/kotlin/com/ktome/client/render/EquipmentInventoryPresenter.kt` 或等价现有 presenter | 负责 slot frame key、selected/empty/equipped 状态、tooltip anchor、grid cell model |
| Modified | `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` | 保持 shop buy/sell、replacement prompt、`5-8` hotkey/cancel 语义 |
| Modified | `client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt` | 复用 shop item lines，不在 renderer 拼业务文案 |
| Modified | `client/src/main/kotlin/com/ktome/client/ui/card/ModalCardModel.kt` | shop offer 与 replacement modal 卡片化时复用已有 modal/card 模型 |
| Modified | `game/src/main/resources/data/items/index.yaml` | 只迁移 PR-03 列出的 official item `iconKey` / 必要 `visualKey`，不改规则、掉落权重、数值或 item model |
| Modified | `game/src/main/resources/data/visuals/index.yaml` | 登记 PR-03 item-specific visual keys，确保 `DataLoader` 能解析 official content key |
| Modified | `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt` | 增加 `dark-uiux-pr03-equipment-inventory-items` deterministic evidence scenario |
| Modified | `UI/sprite-sheets/sheet-plan.yaml` | 增加 Round 7 sheet 和全部非 reserved cell |
| Modified | `UI/sprite-sheets/key-registry.yaml` | 增加 PR-03 owner、fallback、consumer、consumerTest、alias |
| Added | `UI/sprite-sheets/prompts/dark-v1/<round7 prompt files>` | 由 prompt 生成脚本输出，不手写 |
| Added | `assets-src/image/raw/sheets/dark-v1/r07-items-base.png` | Round 7 base item raw sheet |
| Added | `assets-src/image/raw/sheets/dark-v1/r07-items-unique-artifact.png` | Round 7 unique / artifact raw sheet |
| Added | `assets-src/image/raw/sheets/dark-v1/r07-items-affix-material.png` | Round 7 affix / material / shop marker raw sheet |
| Added | `assets-src/image/contact-sheets/dark-v1/<round7 contact sheets>` | 人工 QA 只看 contact sheet |
| Modified | `assets-src/image/manifests/phase2-visual-manifest.json` | 先更新 canonical manifest |
| Modified | `client/src/main/resources/manifests/visual-manifest.json` | 只由 `syncPhase2Manifests` 同步生成 runtime manifest |
| Added | `client/src/main/resources/dark-v1/<split item/shop pngs>` | 切分后的 runtime PNG |
| Added | `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl` | sprite sheet map / QA report |
| Generated | `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` | owner-scope coverage report |
| Added | `game/src/test/kotlin/com/ktome/game/data/ItemVisualKeyContractTest.kt` | 覆盖 PR-03 official item `iconKey` 被真实 data / snapshot source 消费 |
| Added | `client/src/test/kotlin/com/ktome/client/render/EquipmentInventoryPresenterTest.kt` | 覆盖 slot order、identity、quantity absence、quality frame、tooltip anchor |
| Modified | `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | 覆盖 grid bounds、minimum window、fallback icon、stable hitbox |
| Modified | `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt` | 覆盖 shop focus、buy/sell、铭文满槽替换 `5-8` hotkey、取消与 `1-4` 不消费 |
| Modified | `client/src/test/kotlin/com/ktome/client/ui/talent/DescriptionPresenterTest.kt` | 覆盖 shop item tooltip / description line |
| Modified | `client/src/test/kotlin/com/ktome/client/ui/card/ModalCardModelTest.kt` | 覆盖 offer card、modal 行距、长文本和 non-color state marker |
| Added | `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md` | 人工白盒主记录 |
| Added | `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md` | 缺图注入记录 |

Deleted / replaced 清单：

| Path / Contract | Action | removalOwner | Regression Scan |
| --- | --- | --- | --- |
| 右栏纯文字装备/背包列表作为唯一 player-facing 表达 | replace with grid / presentation model；可保留 accessibility text，但不能作为唯一主路径 | PR-03 | `TileRendererCanvasTest` 确认 equipment/inventory grid bounds 存在 |
| `missing_visual` 玩家主路径 fallback | forbidden except explicit injection test | PR-03 | owner-scope coverage + fallback injection record |
| optional `EquipmentInventoryPresenterTest` fallback 路线 | delete optional branch；本 PR 必须新增该 focused test | PR-03 | validation command contains real `EquipmentInventoryPresenterTest` class |

## 3. Round 7 Sheet 内容

1. `r07-items-base`: 基础武器、护甲、药水、卷轴、职业起始装备。
2. `r07-items-unique-artifact`: unique、artifact、relic、boss reward。
3. `r07-items-affix-material`: affix marker、quality marker、craft material、shop marker；不得生成 `ui_frame`。
4. 每张 item sheet 固定为 `icon-sheet 1024x1024 / 8x8 / 128x128`。
5. 切分后的 icon runtime canvas 默认由 category policy 输出到 `160x160`；装备/背包 UI 显示 `48x48/64x64` 由 renderer/layout 决定。
6. 装备 slot frame 属于 `ui_frame`，优先复用 PR-02 的 `ui.frame.slot.*`，不得伪装成 item key。
7. 任何 icon 不允许烘焙数量、品质文字或快捷键数字；数量、品质和 hotkey 只能由 renderer 文本/边框层叠加。
8. 铭文 offer 复用 item/inscription 主体 icon；shop card frame 复用 PR-02 `ui.frame.panel.body`；价格 marker、affordability marker 和 replacement marker 使用 `ui.shop.*` 或 PR-02 `ui.control.*`，并必须进入 key registry。

### Round 7 Sheet Cell Inventory

下表是 PR-03 最小非 reserved cell 清单。实现时必须逐项落到 `UI/sprite-sheets/sheet-plan.yaml` 和 `UI/sprite-sheets/key-registry.yaml`；每张 sheet 未列出的 cell 默认 `reserved: true`，不能在实现时临时塞入未登记 key。

| sheetId | row | col | targetKey | category | outputName | subject | fallbackKey | consumer | consumerTest |
| --- | ---: | ---: | --- | --- | --- | --- | --- | --- | --- |
| `r07-items-base` | 0 | 0 | `item.short_sword.icon` | `icon_item` | `dark-v1/items/item_short_sword_icon.png` | short iron sword, worn leather grip, no text | `ui.empty.inventory.icon` | inventory/equipment item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 0 | 1 | `item.long_sword.icon` | `icon_item` | `dark-v1/items/item_long_sword_icon.png` | long steel sword, straight blade, no text | `ui.empty.inventory.icon` | inventory/equipment item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 0 | 2 | `item.arcane_staff.icon` | `icon_item` | `dark-v1/items/item_arcane_staff_icon.png` | dark wooden staff with small arcane crystal, no text | `ui.empty.inventory.icon` | inventory/equipment item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 0 | 3 | `item.hunter_bow.icon` | `icon_item` | `dark-v1/items/item_hunter_bow_icon.png` | compact hunter bow, wrapped limbs, no text | `ui.empty.inventory.icon` | inventory/equipment item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 0 | 4 | `item.war_maul.icon` | `icon_item` | `dark-v1/items/item_war_maul_icon.png` | heavy iron maul with ember wear, no text | `ui.empty.inventory.icon` | inventory/equipment item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 0 | 5 | `item.forgebreaker_pick.icon` | `icon_item` | `dark-v1/items/item_forgebreaker_pick_icon.png` | mining war pick with dark forge metal, no text | `ui.empty.inventory.icon` | inventory/equipment item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 1 | 0 | `item.basic_shield.icon` | `icon_item` | `dark-v1/items/item_basic_shield_icon.png` | battered round shield, iron rim, no text | `ui.empty.inventory.icon` | equipment off-hand icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 1 | 1 | `item.chain_mail.icon` | `icon_item` | `dark-v1/items/item_chain_mail_icon.png` | folded chain mail armor, no text | `ui.empty.inventory.icon` | equipment armor icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 1 | 2 | `item.leather_armor.icon` | `icon_item` | `dark-v1/items/item_leather_armor_icon.png` | dark leather armor vest, no text | `ui.empty.inventory.icon` | equipment armor icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 1 | 3 | `item.apprentice_robe.icon` | `icon_item` | `dark-v1/items/item_apprentice_robe_icon.png` | folded apprentice robe with muted arcane trim, no text | `ui.empty.inventory.icon` | equipment armor icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 2 | 0 | `item.healing_potion.icon` | `icon_item` | `dark-v1/items/item_healing_potion_icon.png` | red healing vial, wax seal, no text | `ui.empty.inventory.icon` | consumable inventory icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 2 | 1 | `item.mana_potion.icon` | `icon_item` | `dark-v1/items/item_mana_potion_icon.png` | blue mana vial, dim glow, no text | `ui.empty.inventory.icon` | consumable inventory icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 2 | 2 | `item.energy_tonic.icon` | `icon_item` | `dark-v1/items/item_energy_tonic_icon.png` | teal tonic bottle, no text | `ui.empty.inventory.icon` | consumable inventory icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 2 | 3 | `item.scroll_teleport.icon` | `icon_item` | `dark-v1/items/item_scroll_teleport_icon.png` | sealed teleport scroll with dark rune band, no text | `ui.empty.inventory.icon` | consumable inventory icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 2 | 4 | `item.consecrated_oil.icon` | `icon_item` | `dark-v1/items/item_consecrated_oil_icon.png` | small golden oil flask, no text | `ui.empty.inventory.icon` | consumable inventory icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 3 | 0 | `item.bandit_trophy.icon` | `icon_item` | `dark-v1/items/item_bandit_trophy_icon.png` | rough trophy charm and knife notch, no text | `ui.empty.inventory.icon` | off-hand item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 3 | 1 | `item.emerald_charm.icon` | `icon_item` | `dark-v1/items/item_emerald_charm_icon.png` | emerald charm pendant, no text | `ui.empty.inventory.icon` | off-hand item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 3 | 2 | `item.seal_reliquary.icon` | `icon_item` | `dark-v1/items/item_seal_reliquary_icon.png` | small reliquary seal box, no text | `ui.empty.inventory.icon` | off-hand item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 3 | 3 | `item.sanctified_seal.icon` | `icon_item` | `dark-v1/items/item_sanctified_seal_icon.png` | sanctified brass seal, no text | `ui.empty.inventory.icon` | off-hand item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-base` | 3 | 4 | `item.abyssal_heartstone.icon` | `icon_item` | `dark-v1/items/item_abyssal_heartstone_icon.png` | dark heartstone talisman, violet core, no text | `ui.empty.inventory.icon` | off-hand item icon | `EquipmentInventoryPresenterTest` |
| `r07-items-unique-artifact` | 0 | 0 | `item.unique.greenwood_watcher_blade.icon` | `icon_item` | `dark-v1/items/item_unique_greenwood_watcher_blade_icon.png` | unique greenwood sword with thorn guard, no text | `ui.empty.inventory.icon` | unique item icon | `ManifestResolveTest` |
| `r07-items-unique-artifact` | 0 | 1 | `item.unique.briarbound_bow.icon` | `icon_item` | `dark-v1/items/item_unique_briarbound_bow_icon.png` | briarbound bow, thorn string, no text | `ui.empty.inventory.icon` | unique item icon | `ManifestResolveTest` |
| `r07-items-unique-artifact` | 0 | 2 | `item.unique.furnace_plate.icon` | `icon_item` | `dark-v1/items/item_unique_furnace_plate_icon.png` | furnace-worn chest plate, no text | `ui.empty.inventory.icon` | unique item icon | `ManifestResolveTest` |
| `r07-items-unique-artifact` | 0 | 3 | `item.unique.voidlit_seal.icon` | `icon_item` | `dark-v1/items/item_unique_voidlit_seal_icon.png` | voidlit seal relic, purple edge glow, no text | `ui.empty.inventory.icon` | unique item icon | `ManifestResolveTest` |
| `r07-items-unique-artifact` | 0 | 4 | `item.unique.cinderveil_plate.icon` | `icon_item` | `dark-v1/items/item_unique_cinderveil_plate_icon.png` | cinderveil plate armor, ashen trim, no text | `ui.empty.inventory.icon` | unique item icon | `ManifestResolveTest` |
| `r07-items-unique-artifact` | 0 | 5 | `item.unique.deepcurrent_lens.icon` | `icon_item` | `dark-v1/items/item_unique_deepcurrent_lens_icon.png` | deepcurrent lens, dark water glass, no text | `ui.empty.inventory.icon` | unique item icon | `ManifestResolveTest` |
| `r07-items-unique-artifact` | 1 | 0 | `item.artifact.briar_heart.icon` | `icon_item` | `dark-v1/items/item_artifact_briar_heart_icon.png` | artifact thorn heart, no text | `ui.empty.inventory.icon` | artifact item icon | `ManifestResolveTest` |
| `r07-items-unique-artifact` | 1 | 1 | `item.artifact.forge_oath.icon` | `icon_item` | `dark-v1/items/item_artifact_forge_oath_icon.png` | artifact forge oath weapon seal, no text | `ui.empty.inventory.icon` | artifact item icon | `ManifestResolveTest` |
| `r07-items-unique-artifact` | 1 | 2 | `item.artifact.river_echo.icon` | `icon_item` | `dark-v1/items/item_artifact_river_echo_icon.png` | artifact river echo charm, no text | `ui.empty.inventory.icon` | artifact item icon | `ManifestResolveTest` |
| `r07-items-unique-artifact` | 1 | 3 | `item.artifact.eclipsed_relic.icon` | `icon_item` | `dark-v1/items/item_artifact_eclipsed_relic_icon.png` | eclipsed relic medallion, no text | `ui.empty.inventory.icon` | artifact item icon | `ManifestResolveTest` |
| `r07-items-affix-material` | 0 | 0 | `material.iron.icon` | `icon` | `dark-v1/items/material_iron_icon.png` | dark iron ingot material, no text | `ui.empty.inventory.icon` | material marker | `ManifestResolveTest` |
| `r07-items-affix-material` | 0 | 1 | `material.steel.icon` | `icon` | `dark-v1/items/material_steel_icon.png` | polished steel ingot material, no text | `ui.empty.inventory.icon` | material marker | `ManifestResolveTest` |
| `r07-items-affix-material` | 0 | 2 | `material.mithril.icon` | `icon` | `dark-v1/items/material_mithril_icon.png` | pale mithril shard material, no text | `ui.empty.inventory.icon` | material marker | `ManifestResolveTest` |
| `r07-items-affix-material` | 0 | 3 | `material.adamantite.icon` | `icon` | `dark-v1/items/material_adamantite_icon.png` | black adamantite bar material, no text | `ui.empty.inventory.icon` | material marker | `ManifestResolveTest` |
| `r07-items-affix-material` | 1 | 0 | `affix.sharp.icon` | `icon` | `dark-v1/items/affix_sharp_icon.png` | sharp edge affix marker, no text | `ui.empty.inventory.icon` | affix marker | `ManifestResolveTest` |
| `r07-items-affix-material` | 1 | 1 | `affix.sturdy.icon` | `icon` | `dark-v1/items/affix_sturdy_icon.png` | sturdy armor rivet affix marker, no text | `ui.empty.inventory.icon` | affix marker | `ManifestResolveTest` |
| `r07-items-affix-material` | 1 | 2 | `affix.of_strength.icon` | `icon` | `dark-v1/items/affix_of_strength_icon.png` | strength sigil affix marker, no text | `ui.empty.inventory.icon` | affix marker | `ManifestResolveTest` |
| `r07-items-affix-material` | 1 | 3 | `affix.of_life.icon` | `icon` | `dark-v1/items/affix_of_life_icon.png` | life drop affix marker, no text | `ui.empty.inventory.icon` | affix marker | `ManifestResolveTest` |
| `r07-items-affix-material` | 2 | 0 | `ui.shop.price.affordable` | `icon` | `dark-v1/ui/ui_shop_price_affordable.png` | coin marker with readable silhouette, no text | `ui.hud.gold.icon` | shop price marker | `ModalCardModelTest` |
| `r07-items-affix-material` | 2 | 1 | `ui.shop.price.unaffordable` | `icon` | `dark-v1/ui/ui_shop_price_unaffordable.png` | cracked coin marker, no text | `ui.hud.warning.icon` | shop price marker | `ModalCardModelTest` |
| `r07-items-affix-material` | 2 | 2 | `ui.shop.inscription.marker` | `icon` | `dark-v1/ui/ui_shop_inscription_marker.png` | small inscription rune marker, no text | `ui.hud.quest_marker.icon` | inscription offer marker | `DescriptionPresenterTest` |
| `r07-items-affix-material` | 2 | 3 | `ui.shop.replacement.slot_marker` | `icon` | `dark-v1/ui/ui_shop_replacement_slot_marker.png` | replacement slot marker, no text or digits | `ui.frame.slot.selected` | full-slot replacement modal | `InputHandlerTest` |

Column ownership:

| Column | Destination |
| --- | --- |
| `sheetId`, `row`, `col`, `targetKey`, `category`, `outputName`, `subject`, `reserved`, `aliasOf` | `UI/sprite-sheets/sheet-plan.yaml` |
| `targetKey`, `category`, `ownerPr=PR-03`, `sheetId`, `fallbackKey`, `consumer`, `consumerTest`, `aliasOf` | `UI/sprite-sheets/key-registry.yaml` |
| `outputName` | must match canonical/runtime visual manifest `rawOutputPath` after `syncPhase2Manifests` |

The table above is an implementation contract, not a literal YAML schema. Do not copy `fallbackKey`, `consumer`, or `consumerTest` into `sheet-plan.yaml`; `darkSpriteSheetLint` treats them as unsupported cell fields.

Runtime Data Consumption:

PR-03 uses the item-specific key path. The implementation must update official game data so the real `ItemRenderSnapshot.iconKey` consumed by equipment/inventory/shop points at the Round 7 target key. This is content wiring only; it must not change item stats, drop floors, shop price, loot budget, or save/schema models.

| baseItemId | Current consumed key family | PR-03 iconKey | visualKey rule | Test |
| --- | --- | --- | --- | --- |
| `hunter_bow` | `item.base.rogue.weapon.icon` | `item.hunter_bow.icon` | update `visualKey` to `item.hunter_bow.icon` only if no separate ground visual is added in this PR | `ItemVisualKeyContractTest` |
| `war_maul` | `item.base.vanguard.weapon.icon` | `item.war_maul.icon` | same as above | `ItemVisualKeyContractTest` |
| `forgebreaker_pick` | `item.base.vanguard.weapon.icon` | `item.forgebreaker_pick.icon` | same as above | `ItemVisualKeyContractTest` |
| `bandit_trophy` | `item.base.rogue.off_hand.icon` | `item.bandit_trophy.icon` | same as above | `ItemVisualKeyContractTest` |
| `emerald_charm` | `item.base.arcanist.off_hand.icon` | `item.emerald_charm.icon` | same as above | `ItemVisualKeyContractTest` |
| `seal_reliquary` | `item.base.arcanist.off_hand.icon` | `item.seal_reliquary.icon` | same as above | `ItemVisualKeyContractTest` |
| `sanctified_seal` | `item.base.templar.off_hand.icon` | `item.sanctified_seal.icon` | same as above | `ItemVisualKeyContractTest` |
| `energy_tonic` | `item.mana_potion.icon` | `item.energy_tonic.icon` | same as above | `ItemVisualKeyContractTest` |
| `consecrated_oil` | `item.healing_potion.icon` | `item.consecrated_oil.icon` | same as above | `ItemVisualKeyContractTest` |

`short_sword`、`long_sword`、`arcane_staff`、`basic_shield`、`chain_mail`、`leather_armor`、`apprentice_robe`、`healing_potion`、`mana_potion`、`scroll_teleport`、`abyssal_heartstone` and the listed unique/artifact templates already expose item-specific key families; implementation still must confirm they are present in `game/src/main/resources/data/visuals/index.yaml`, canonical manifest, runtime manifest and key registry.

Raw sheet 生成交接：

1. 先按 PR-00 固定命令生成 Round 7 prompt 文件和 `prompt-index.json`。
2. 逐个执行 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --smoke-report <buildReportPath> --overwrite`，由脚本调用 Codex CLI 并复制最新生成图片到 `sheet-plan.yaml.rawSheetPath` 指定位置。
3. 文件名必须分别为 `r07-items-base.png`、`r07-items-unique-artifact.png`、`r07-items-affix-material.png`。
4. raw PNG 缺失或尺寸不匹配时，本 PR 不进入切分和 manifest patch。
5. PR 描述必须列出 prompt path、raw sheet path、raw sheet hash、Codex CLI transient source 摘要、切分报告和 contact sheet QA path；transient source 只能作为摘要，不能写入长期合同。

## 4. Key Registry 要求

1. `item.*.icon` 只指向物品主体图。
2. empty slot、locked slot、selected/equipped slot 统一复用 PR-02 的 `ui.frame.slot.*`；如果视觉上复用同一图，必须在 key registry 用 `aliasOf` 表达。
3. quality frame 如需独立边框资源，必须归 `ui_frame` 并回到 PR-02/PR-00 registry 处理；PR-03 的 `r07-items-affix-material` 只生成 item/affix/material 主体或小 marker。
4. `fallbackKey` 必须是 dark style fallback，不允许直接使用旧风格 `missing_visual` 作为玩家主路径。
5. PR close 前输出 item/equipment key coverage summary，至少包含 `expectedItemKeys`、`coveredItemKeys`、`missingItemKeys`、`allowedFallbackKeys`。
6. `ui.shop.*` key 只表示 shop presentation，不得编码价格、库存、购买规则或 offer id。
7. `ui.shop.offer.frame` 是 alias-only key，默认指向 `ui.frame.panel.body`；不得在 PR-03 Round 7 sheet 中生成新 PNG，不得写入 `r07-items-affix-material` cell。
8. `ui.shop.inscription.marker` 表示 offer 类型 marker，不能替代 inscription 本体 icon；本体 icon 仍走 inscription snapshot 的 `iconKey`。
9. `ui.shop.price.affordable`、`ui.shop.price.unaffordable` 只是 marker/glyph；禁用原因必须来自 snapshot/presenter/locale token，不得烘焙到图片。
10. `ui.shop.offer.disabled` 在当前 PR deferred：除非同 PR 新增 typed disabled source，否则不得生成 Round 7 cell、manifest entry 或 client consumer。
11. `ui.shop.replacement.slot_marker` 只用于满槽替换 modal 的槽位视觉；hotkey `5-8 / Esc` 由 renderer 文本层叠加。

Existing key migration 固定如下：

| existingKey | action | targetKey / aliasOf | removalOwner | consumerTest |
| --- | --- | --- | --- | --- |
| `ui.empty.inventory.icon` | keep | `N/A` | `N/A` | `TileRendererCanvasTest` / `dark-uiux-pr03-inventory-empty` |
| `ui.empty.shop.icon` | keep | `N/A` | `N/A` | `TileRendererCanvasTest` / `dark-uiux-pr03-inscription-shop` |
| `ui.card.shop.header.icon` | keep | `N/A`; header icon is not `ui.shop.offer.frame` | `N/A` | `ModalCardModelTest` |
| `ui.card.reward.header.icon` | keep out of scope | `N/A` | PR-07 final audit | `ModalCardModelTest` |
| `ui.shop.offer.frame` | alias-only | `ui.frame.panel.body` | `N/A` | `ModalCardModelTest` |
| `ui.shop.offer.disabled` | deferred | requires future typed disabled source | future owner that adds typed source | `N/A` |

如果实现决定迁移某个 keep key，必须在同一 PR 改成本表的显式 `aliasOf` 或 `remove`，补 `rg -n "ui.card.shop.header.icon|ui.empty.shop.icon|ui.empty.inventory.icon"` regression scan，并更新 key registry、manifest、focused test 和 README evidence matrix。

## 5. 非目标

1. 不新增物品规则、掉落权重、loot budget 或数值。
2. 不新增额外 rarity / special tier。
3. 不改职业树 UI。
4. 默认不改 `core` item model，只消费现有 snapshot/manifest key。
5. 不改 shop refresh、shop rescue affordability、gold/shard 花费、inscription equip/replace 规则。
6. 不新增 shop screen 的鼠标拖拽或点击购买语义；当前阶段保持键盘/现有 input contract。
7. 不在 `client` 用 locale text、price label、offer label 或 visual key 反推购买规则。

## 6. 必测行为与状态来源

### 6.1 Equipment / Inventory Layout Contract

| Contract | Fixed Value |
| --- | --- |
| equipment slot order | `WEAPON`, `OFF_HAND`, `ARMOR` |
| equipment cell identity | `slotId`; visual asset key 不参与 identity |
| equipment cell model | `slotId`, `labelKey`, `frameKey`, `itemIconKey?`, `qualityTierId?`, `selected`, `emptyStateToken`, `tooltipAnchorId` |
| inventory cell identity | `InventoryEntrySnapshot.index`; item visual key、quality、名称和排序变化不能改 identity |
| inventory grid model | `columns`, `visibleRows`, `cellSizePx`, `gapPx`, `overflowPolicy`, `selectedInventoryIndex`, `itemIconKey?`, `quantityText?`, `qualityTierId`, `disabledReasonToken?` |
| layout baseline | `1280x800`: right panel fixed-width grid；minimum supported window: compact grid with stable cell bounds and scroll/overflow indicator |
| fallback | missing icon uses dark fallback visual and records key in coverage/report；missing item snapshot renders empty slot, not placeholder item |
| tooltip anchor | selected equipment slot anchors to `slotId`; selected inventory cell anchors to `InventoryEntrySnapshot.index` |

### 6.2 State Source Contract

| UI state | Source of truth | PR-03 rule |
| --- | --- | --- |
| item icon | `ItemRenderSnapshot.iconKey` then `visualKey` fallback through manifest resolver | renderer 不拼裸路径；缺 key 只走 resolver fallback |
| quality frame | `ItemRenderSnapshot.qualityTierId` | 只影响 frame/marker，不改变 hitbox、selection identity 或 item identity |
| inventory quantity badge | no field in current `InventoryEntrySnapshot` | 默认隐藏；`dark-uiux-pr03-inventory-stacked` 证明 repeated item cells、quality 和 future-safe badge anchor，不要求真实 stack count |
| unusable / disabled inventory item | no rule field in current snapshot | 不做 pre-rendered disabled state；只能显示 post-command feedback 或 existing token |
| shop affordability | `ShopOfferSnapshot.price <= RenderUiStateSnapshot.shardBalance` in client presenter | 只作为表现 marker，不成为购买规则；购买结果仍以 game command result 为准 |
| purchased / disabled / threatened offer | no field in current `ShopOfferSnapshot` / `ShopPanelSnapshot` | 默认不预渲染；购买失败、金币不足和 threatened 只通过 post-command log / token evidence 表达 |
| disabled reason copy | locale token from snapshot/presenter only | 图片不烘焙文案，renderer 不拼业务句子 |

若实现必须交付真实 stack count、purchased、disabled 或 threatened 预渲染状态，必须把本表改为新的 typed boundary，并同步更新 `RenderSnapshotContractTest`、focused presenter test、locale/contract lint 和 release note 风险说明。

### 6.3 Shop / Replacement Input Contract

1. 铭文 replacement modal 的候选槽位来自 `InscriptionReplacementPromptSnapshot.currentSlots`。
2. 可选 hotkey 固定为 `slot.hotkey`，当前核心合同为 `5-8`；renderer 只展示 snapshot hotkey，不重新编号。
3. 方向键只移动 selection，数字键 `5-8` 只更新 selection。
4. `Enter` / `Space` / `E` 才提交 `PlayerCommand.BuyShopOffer(..., replacementHotkey=<selected>)`。
5. `Esc` / `Backspace` 提交 `PlayerCommand.CancelInscriptionReplacementPurchase`。
6. 数字键 `1-4` 只属于 talent active slot 语义；在 shop replacement prompt 中不得触发 replacement。
7. 任何 confirmation 前不得扣 shard，也不得改 inscription loadout。

## 7. 实施顺序与验证

### Implementation Order

1. 执行 `acceptanceContractLint`，确认 PR 文档结构可被 gate 读取。
2. 按 §3 的 cell inventory 更新 `UI/sprite-sheets/sheet-plan.yaml`；未列 cell 写 `reserved: true`。
3. 更新 `UI/sprite-sheets/key-registry.yaml`，为每个 targetKey 写 `ownerPr=PR-03`、`fallbackKey`、`consumer`、`consumerTest`，为真实复用图写 `aliasOf`。
4. 生成 Round 7 prompt 和 `prompt-index.json`，再用 `scripts/codex-generate-image.py` 生成三张 raw sheet。
5. 运行切分、contact sheet 和 sprite map 校验；PR-03 sprite map report path 必须等于 `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`；人工只在 contact sheet 上验收语义和风格，不通过改 `row/col` 修错图。
6. 先更新 `assets-src/image/manifests/phase2-visual-manifest.json`，再运行 `syncPhase2Manifests` 生成 runtime manifest。
7. 新增 `EquipmentInventoryPresenter` / model 和 `EquipmentInventoryPresenterTest`，先锁 layout、identity、quantity absence、quality frame、tooltip anchor。
8. 改 `TileRenderer` 和 `TileRendererCanvasTest`，证明 grid bounds、fallback、minimum window 和 hitbox 稳定。
9. 改 shop card / description / input modal，补 `ModalCardModelTest`、`DescriptionPresenterTest`、`InputHandlerTest`。
10. 跑 clientSmoke/golden/manual；最后跑 `maintainabilityLint` 和 `verifyChanged`。
11. 完成 doc-vs-implementation self-audit：逐条对照 Acceptance Matrix、cell inventory、existing key migration、manual whitebox 表和 validation output。

### Commands

所有 Gradle 命令前必须执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

文档和资源合同快检：

```bash
./gradlew acceptanceContractLint
./gradlew darkKeyRegistryLint darkSpriteSheetLint
./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl
```

canonical -> runtime manifest 必须先同步，再进入 resolver、golden 和最终 gate：

```bash
./gradlew syncPhase2Manifests manifestLint
```

PR-03 owner-scope coverage 必须显式传三张 required sheet：

```bash
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03 -Pktome.darkUiux.requiredOwnerSheetIds=r07-items-base,r07-items-unique-artifact,r07-items-affix-material
```

focused client tests：

```bash
./gradlew :game:test --tests com.ktome.game.data.ItemVisualKeyContractTest :client:test --tests com.ktome.client.render.EquipmentInventoryPresenterTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.ui.talent.DescriptionPresenterTest --tests com.ktome.client.ui.card.ModalCardModelTest --tests com.ktome.client.assets.ManifestResolveTest
```

PR close 前 owner gates：

```bash
./gradlew assetLint styleLint :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint verifyChanged
```

如果本 PR 修改 `tools/build.gradle.kts`、Gradle task 接线或 bootstrap 相关脚本，追加：

```bash
./scripts/verify-bootstrap.sh
```

## 8. 人工白盒

| label | scenario / setup | input sequence | expected visible result | expected artifact | skip rule | residual risk |
| --- | --- | --- | --- | --- | --- | --- |
| `dark-uiux-pr03-equipment-slots` | primary scenario `dark-uiux-pr03-equipment-inventory-items`, seed `2026050903`, validation registry setup has weapon/off-hand/armor plus one intentionally empty slot pass | `./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr03-equipment-inventory-items`, then open equipment/inventory surface | `WEAPON` / `OFF_HAND` / `ARMOR` visible; at least one equipped item and one empty slot; selected slot frame differs from quality frame; tooltip anchor points to selected slot; hitbox unchanged by frame/badge | `UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md` + golden/manual screenshot path; record fields: `scenarioId`, `inputSequence`, `evidenceLabel`, `screenshotPath`, `logPath`, `cleanupStatus` | Only skipped on headless CI; must keep `EquipmentInventoryPresenterTest` and `TileRendererCanvasTest` evidence | Manual cannot prove every locale/window; focused tests cover bounds |
| `dark-uiux-pr03-inventory-empty` | same scenario, explicit empty-inventory evidence step produced by validation action `emptyInventorySurface` | Open inventory after the empty-inventory step | empty grid uses dark empty state; no placeholder item; tooltip/log/HUD do not overlap | same manual record + `client/build/reports/golden/` label | Only skipped on headless CI; golden required | Does not prove full inventory overflow |
| `dark-uiux-pr03-inventory-stacked` | same scenario, explicit repeated-item evidence step using two repeated `healing_potion` entries and one rare or unique item | Open inventory, move selection across repeated entries | repeated entries remain separate stable cells; quality frame visible; quantity badge anchor remains reserved/absent unless typed quantity exists | same manual record + golden/manual screenshot path | Only skipped on headless CI; focused presenter test required | True stack count remains out of scope until typed field exists |
| `dark-uiux-pr03-inscription-shop` | reuse `phase4-v4-pr02` shop setup for inscription replacement plus PR-03 visual manifest loaded | `F9, Enter`, then navigate buy/sell columns | buy/sell columns, offer card, price marker, affordability marker, inscription marker, tooltip and empty shop/sell states share dark style; non-color marker exists | same manual record + `client/build/reports/golden/` label | Only skipped if `phase4-v4-pr02` scenario unavailable; must record missing scenario and keep `InputHandlerTest` / `ModalCardModelTest` | Manual does not prove every offer type |
| `dark-uiux-pr03-shop-full-slot-replace` | reuse `phase4-v4-pr02` full-slot inscription setup | `F9, Right, Enter, Down, Down, Enter, 5, Enter`; repeat with `Esc` | replacement modal shows candidate/current slots/category delta/price and hotkeys `5-8`; `1-4` do not replace; no shard loss before confirm; cancel returns to shop | same manual record + screenshot/log path | Only skipped if scenario cannot materialize full slots; must keep `InputHandlerTest` replacement cases | Manual uses one chosen slot; tests cover range |
| `dark-uiux-pr03-fallback-key-injection` | primary path is resolver fixture copy under `client/src/test/resources/dark-uiux/pr03-missing-key-fixture/`; do not edit official runtime manifest | run focused resolver/report fixture test, then open the consuming surface only if screenshot evidence is needed | dark fallback visual appears; report records missing key, fallback key, source/runtime manifest fixture path and evidence label; no player path uses `missing_visual` | `UI/manual-records/dark-uiux-pr03-fallback-key-injection.md`, `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` | Cannot skip in resource PR close; if headless, use resolver/report artifact instead of screenshot | Does not replace real owner-scope coverage |

必填证据：`dark-uiux-pr03-equipment-slots`、`dark-uiux-pr03-inventory-empty`、`dark-uiux-pr03-inventory-stacked`、`dark-uiux-pr03-inscription-shop`、`dark-uiux-pr03-shop-full-slot-replace`、`UI/manual-records/dark-uiux-pr03-fallback-key-injection.md`。

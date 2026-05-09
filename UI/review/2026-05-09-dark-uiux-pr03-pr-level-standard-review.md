# Dark UI/UX PR-03 PR-Level Standard Review

目标文档：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md`

审查标准：`docs/review/rule/pr-level-review-standard.md`

审查时间：2026-05-09

## Precheck

- 当前分支：`main...origin/main`
- 工作树状态：存在与本轮审查目标无关的未提交改动：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`、`scripts/verify_dark_manifest_coverage.py`、`tools/build.gradle.kts`、`tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt`、`UI/review/2026-05-09-dark-uiux-pr02-pr-level-standard-rereview-round2.md`
- 本轮范围：只审查 PR-03 文档是否满足 PR 级 review 标准和 dark UI/UX 系列合同，不审查 PR-02 未提交改动是否正确。
- 命中系列：dark UI/UX PR 系列，执行顺序来自 `UI/pr/README.md`，PR-03 位于 PR-02 之后，目标是装备/背包 grid、item icon、铭文商店、quality、空态/tooltip。
- 受影响模块：`client`、`assets`、`tools`、`docs`。目标文档声明不改 `core` item model、不改 shop/loot 规则。
- 已运行验证：`source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint`，结果 `BUILD SUCCESSFUL`。该 gate 只能证明 Acceptance Matrix 结构存在，不能证明本文列出的语义合同已冻结。

## Findings

### P0

无。

### P1

#### P1-1 满槽铭文替换热键写成 `1-6`，与当前核心合同 `5-8` 冲突

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:91` 写明 replacement marker 的 hotkey 为 `1-6 / Esc`。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:122` 和 `:153` 再次要求 `1-6` 替换。
- `core/src/main/kotlin/com/ktome/core/inscription/InscriptionSlot.kt:3-5` 冻结当前铭文槽：`MAX_INSCRIPTION_SLOTS=4`、`INSCRIPTION_HOTKEY_START=5`。
- `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt:170-176` 当前白盒场景期望 replacement prompt 显示 hotkeys `5-8`，并用 `5, Enter` 完成替换。

影响：

开发者按文档实现会把数字键消费、modal copy、manual evidence 和 `InputHandlerTest` 写到错误热键范围。更严重的是，`1-4` 已经属于 talent active slot 语义，PR-03 若把 replacement 写成 `1-6`，会制造输入合同冲突。

修复方向：

把 PR-03 文档所有 replacement hotkey 改为 `5-8 / Esc`。新增一段硬合同：

```text
铭文 replacement modal 的候选槽位来自 `InscriptionReplacementPromptSnapshot.currentSlots`。
可选 hotkey 固定为 slot.hotkey，当前核心合同为 5-8；renderer 只展示 snapshot hotkey，不重新编号。
方向键只移动 selection，数字键 5-8 只更新 selection，Enter/Space/E 才提交 `PlayerCommand.BuyShopOffer(..., replacementHotkey=<selected>)`。
Esc/Backspace 提交 `PlayerCommand.CancelInscriptionReplacementPurchase`。
```

推荐测试：

- `InputHandlerTest.shopReplacementHotkeysUseSnapshotRangeFiveToEight`
- `InputHandlerTest.talentHotkeysOneToFourDoNotReplaceInShopPrompt`

#### P1-2 PR-03 要求 quantity badge / purchased / disabled shop state，但未冻结这些状态的数据来源

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:40`、`:70`、`:115-116` 要求数量 badge、品质 frame。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:120` 要求 affordable / unaffordable / purchased / disabled offer 有非颜色差异。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:123` 要求购买失败、金币不足、shop threatened、空商店、空 sell list 都有暗黑 empty/disabled state。
- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:441-445` 的 `InventoryEntrySnapshot` 只有 `index / item / equippedSlotId`，没有 `quantity`、`stackCount` 或 disabled state。
- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:530-537` 的 `ShopOfferSnapshot` 只有 `index / labelKey / price / offerFingerprint / tags / tagLabelKeys`，没有 `affordable`、`purchased`、`disabledReasonKey` 或 `threatened`。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:106-111` 又声明不新增物品规则、不改 shop 经济和购买规则。

影响：

文档同时要求展示多个新 UI 状态，又没有说明这些状态是从现有 snapshot 推导、由 `game` 扩展 snapshot、还是降级为本 PR 非目标。开发者只能猜，结果很容易把规则判断写进 `client`，或用 UI text / price 推断规则，违反 `client` 只做表现编排的边界。

修复方向：

在 PR-03 增加 `State Source Contract`，逐项冻结：

```text
inventory quantity badge:
- 当前 `InventoryEntrySnapshot` 无 quantity；若本 PR 不扩展 snapshot，则 quantity badge 只允许显示 `null/N/A`，并把 `stacked` evidence 改为重复 item selection/quality evidence。
- 若必须交付堆叠数量，本 PR 必须显式新增 `InventoryEntrySnapshot.quantity: Int = 1` 或等价 typed presentation field，并声明 schema/version 影响、owner test 和 save/replay 风险。

shop offer state:
- `affordable` 可由 `ShopOfferSnapshot.price <= RenderUiStateSnapshot.shardBalance` 在 presenter 层派生，但该派生只用于表现，不成为购买规则。
- `purchased / disabled / threatened / disabledReasonKey` 当前 snapshot 无字段；若本 PR 要展示，必须在 `ShopOfferSnapshot` 或 `ShopPanelSnapshot` 增加 exact fields 和 locale token contract。
- 如果不扩展 snapshot，文档必须删除 `purchased / shop threatened / disabled reason` 的 blocking 表述，改为 post-command failure message evidence。
```

推荐测试：

- `EquipmentInventoryPresenterTest.quantityBadgeIsAbsentWhenSnapshotHasNoQuantity`
- `ShopPresentationModelTest.affordabilityMarkerIsPresentationOnly`
- 若扩展 snapshot：`RenderSnapshotContractTest.shopOfferDisabledReasonIsTokenized`

#### P1-3 Round 7 资源没有冻结 cell/key 清单，`owner-scope` coverage 也没有要求三个 PR-03 sheet 全出现

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:64-72` 只列出 `r07-items-base`、`r07-items-unique-artifact`、`r07-items-affix-material` 的主题范围，没有任何 row/col、targetKey、outputName、subject 清单。
- `UI/PLAN.md:80-86`、`:141-186` 要求 `sheet-plan.yaml` 是唯一映射真源，必须包含 `sheetId / row / col / targetKey / category / outputName / subject`。
- `UI/pr/README.md:80-83` 把三个 Round 7 sheet 明确归属 PR-03。
- `scripts/verify_dark_manifest_coverage.py:80-99` 的 owner-scope 只从 registry 里找 `ownerPr=PR-03` 的 key；如果只登记一个 PR-03 key，它不会自动知道另外两个 sheet 整体缺失。
- `scripts/verify_dark_manifest_coverage.py:156-163` 和 `tools/build.gradle.kts:597-636` 已支持 `requiredOwnerSheetIds`，但 PR-03 文档命令 `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:137-138` 没有传。

影响：

资源开发无法从文档直接落到 prompt/sheet-plan。更隐蔽的是，只要 registry 中存在任意 `PR-03` key，`owner-scope` coverage 就可能在缺少某个 Round 7 sheet 的情况下没有报出“sheet 缺失”这一类问题，导致 PR close gate 误判。

修复方向：

PR-03 必须新增 `Round 7 Sheet Cell Inventory` 表，至少覆盖每个非 reserved cell：

```text
sheetId | row | col | targetKey | category | outputName | subject | fallbackKey | consumer | consumerTest
```

并把 owner-scope 命令改为：

```bash
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=owner-scope \
  -Pktome.darkUiux.ownerPr=PR-03 \
  -Pktome.darkUiux.requiredOwnerSheetIds=r07-items-base,r07-items-unique-artifact,r07-items-affix-material
```

推荐测试：

- `DarkSpriteSheetPipelineScriptTest.ownerScopeCoverageFailsWhenRequiredPr03SheetIsMissing`
- `DarkSpriteSheetPipelineScriptTest.pr03Round7CellsAllHaveRegistryAndConsumerTest`

#### P1-4 装备/背包 grid 的布局、focus、identity 和 overflow 状态机没有冻结

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:38-41` 要求正式展示装备格、背包 grid、资源计数、空态、tooltip、选中态。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:115-123` 只列行为目标，没有冻结 grid columns、slot order、cell size、gap、selection identity、scroll/overflow、tooltip anchor 或 min-window fallback。
- `UI/pr/screen-coverage-matrix.md:38-39` 明确装备面板必须覆盖固定 hitbox，背包 grid 必须覆盖 scroll/overflow 策略。
- 当前 `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:706-717`、`:884-938` 仍主要把装备/背包建成文本 row 加 icon，而不是可验收的 grid/hitbox presentation model。

影响：

同一个 PR 文档可以被实现成“右栏文本列表加 icon”、独立 grid、overlay modal grid 或 renderer 内部临时坐标，全部都看似符合“图标化”。这会让 `goldenScreenshot`、hitbox 测试和 manual evidence 无法稳定比较，也会增加 `client` 里第二套 selection/focus 状态的风险。

修复方向：

新增 `Equipment/Inventory Layout Contract`，冻结以下字段：

```text
equipment slot order: WEAPON, OFF_HAND, ARMOR
equipment slot model: slotId, labelKey, frameKey, itemIconKey?, qualityTierId?, selected, emptyStateToken, tooltipAnchorId
inventory grid model: columns, visibleRows, cellSizePx, gapPx, overflowPolicy, selectedInventoryIndex, itemIconKey, quantityText?, qualityTierId, disabledReasonToken?
identity: inventory cell identity = InventoryEntrySnapshot.index; equipment cell identity = slotId; visual asset keys do not participate in identity
fallback: missing icon uses dark fallback visual and records key in coverage/report; missing item snapshot renders empty slot, not placeholder item
min-window: choose exact compact behavior for 1280x800 and minimum supported window
```

推荐测试：

- `EquipmentInventoryPresenterTest.equipmentSlotOrderAndIdentityAreStable`
- `EquipmentInventoryPresenterTest.inventoryGridKeepsSelectedIndexAcrossIconFallback`
- `TileRendererCanvasTest.inventoryGridUsesStableCellBoundsAtMinimumWindow`

#### P1-5 PR-03 必填证据遗漏 `dark-uiux-pr03-equipment-slots`

证据：

- `UI/pr/screen-coverage-matrix.md:38` 要求装备面板证据为 `dark-uiux-pr03-equipment-slots`，覆盖装备 slot、已装备/空/选中态、quality frame、tooltip、固定 hitbox。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:154` 必填证据只列 `dark-uiux-pr03-inventory-empty`、`dark-uiux-pr03-inventory-stacked`、`dark-uiux-pr03-inscription-shop`、`dark-uiux-pr03-shop-full-slot-replace` 和 fallback key injection record。
- `UI/pr/README.md:230` 的 PR-03 evidence matrix 同样没有 `dark-uiux-pr03-equipment-slots`。

影响：

装备面板是 PR-03 阶段目标的第一项，但没有独立 evidence label。后续实现可以只截空背包或背包堆叠图，而不证明装备 slot 的已装备、空、选中、quality frame 和 hitbox 行为。

修复方向：

把 `dark-uiux-pr03-equipment-slots` 加入 PR-03 文档的 Acceptance Matrix、人工白盒、验证 label 和 PR README evidence matrix。该 label 至少要包含：

```text
WEAPON / OFF_HAND / ARMOR 三槽可见；
至少一个已装备 item、一个空 slot；
selected slot frame 与 item quality frame 可区分；
tooltip anchor 指向 selected slot；
hitbox 不被 quality frame、badge 或 slot frame 改写。
```

推荐测试：

- `EquipmentInventoryPresenterTest.equipmentSlotsExposeEmptyEquippedSelectedAndTooltipAnchors`
- `TileRendererCanvasTest.equipmentSlotHitboxesStayStableWithQualityFrame`

#### P1-6 验证顺序把 `syncPhase2Manifests` 放在完整 gate 之后，可能用旧 runtime manifest 跑 golden / verifyChanged

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:127-132` 先运行包含 `manifestLint`、`:client:clientSmoke`、`:client:goldenScreenshot`、`verifyChanged` 的长命令，然后才运行 `./gradlew syncPhase2Manifests manifestLint`。
- `UI/PLAN.md:62-68` 和 `UI/pr/README.md:133-139` 规定 canonical manifest 是真源，runtime manifest 只能由 `syncPhase2Manifests` 同步生成，新增 key 的闭环包括 canonical -> sync -> runtime -> resolver/test -> coverage。
- `docs/review/rule/pr-level-review-standard.md:493-499` 把 canonical/runtime 只改一侧、未通过 sync 或等价 owner gate 证明一致列为红线。

影响：

如果 PR-03 修改 canonical manifest，当前命令顺序可能先用旧 runtime manifest 跑 resolver、client smoke、golden 和 `verifyChanged`，最后才同步 runtime manifest。这样会把真正需要验证的资源映射漂移留到 gate 末尾，甚至导致最终 `verifyChanged` 没覆盖同步后的状态。

修复方向：

把验证章节改成分阶段顺序：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew syncPhase2Manifests manifestLint
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03 -Pktome.darkUiux.requiredOwnerSheetIds=r07-items-base,r07-items-unique-artifact,r07-items-affix-material
./gradlew :client:test --tests ...
./gradlew :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint verifyChanged
```

推荐测试：

- `VerificationImpactAnalyzerTest.darkManifestSyncRoutesBeforeClientGolden`
- 文档自检：`git diff --check -- UI/pr/dark-uiux-pr03-equipment-inventory-items.md`

### P2

#### P2-1 影响范围表漏掉 PR-03 实际必须改的资源真源和报告产物

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:44-60` 的影响范围只列了 client Kotlin、canonical/runtime manifest、`UI/sprite-sheets/sheet-plan.yaml` 和部分测试。
- 同文档 `:81-102` 要求 key registry，但影响范围没有 `UI/sprite-sheets/key-registry.yaml`。
- `UI/PLAN.md:243-258` 要求 prompt、raw sheet、切分 PNG、contact sheet、QA report 和 manifest 校验。
- `UI/pr/development-governance.md:73-85` 把 `sheet-plan.yaml`、`key-registry.yaml`、contact sheet、canonical/runtime manifest、coverage report、client golden、manual records 都列为 canonical artifact。

影响：

开发者按影响范围施工会漏掉 key registry、raw sheet、prompt index、切分报告、contact sheet QA、coverage JSON 和 fallback injection record。资源 PR 最容易因此出现“图片存在但 registry/manifest/gate 不闭环”的半成品。

修复方向：

把影响范围改成 Added / Modified / Deleted 三栏，至少新增：

```text
Modified:
- UI/sprite-sheets/sheet-plan.yaml
- UI/sprite-sheets/key-registry.yaml
- assets-src/image/manifests/phase2-visual-manifest.json
- client/src/main/resources/manifests/visual-manifest.json

Added:
- UI/sprite-sheets/prompts/dark-v1/<round7 prompt files>
- assets-src/image/raw/sheets/dark-v1/r07-items-base.png
- assets-src/image/raw/sheets/dark-v1/r07-items-unique-artifact.png
- assets-src/image/raw/sheets/dark-v1/r07-items-affix-material.png
- assets-src/image/contact-sheets/dark-v1/<round7 contact sheets>
- client/src/main/resources/dark-v1/<split item/shop pngs>
- build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json
- assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl or exact chosen report path
- UI/manual-records/dark-uiux-pr03-equipment-inventory-items.md
```

#### P2-2 artifact 字段和 fallback key injection record 没有 exact repo-relative path

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:18` 的 artifact 写成 `dark manifest coverage report`。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:30` 写 `manifest coverage report` 和 `fallback key injection record`，但未给文件路径。
- `scripts/verify_dark_manifest_coverage.py:49` 默认 report 是 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`。
- `tools/build.gradle.kts:602-636` 当前 root task 输出同一路径。
- `docs/review/rule/pr-level-review-standard.md:416-418` 要求 resource / manifest / report 字段都有 repo-relative artifact，manual white-box 有 scenario、steps、expected evidence 和 skip rule。

影响：

后续 PR 和 reviewer 无法机械定位 PR-03 coverage artifact 或 fallback 注入记录。尤其是 fallback key injection record 如果只写“必须 repo-relative”，没有路径、schema 和字段，实际实现时很容易退化成 PR 描述里的手写说明。

修复方向：

冻结路径和字段：

```text
coverage artifact: build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json
fallback injection record: UI/manual-records/dark-uiux-pr03-fallback-key-injection.md
fallback record required fields:
- scenarioId
- injectedMissingKey
- fallbackKey
- sourceManifestPath
- runtimeManifestPath
- evidenceLabel
- screenshotPath or reportPath
- result
- residualRisk
```

#### P2-3 optional `EquipmentInventoryPresenterTest` 与验证命令互相矛盾

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:57` 把 `EquipmentInventoryPresenterTest.kt` 列为预期测试。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:130` 的命令强制 `--tests com.ktome.client.render.EquipmentInventoryPresenterTest`。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:143` 又说如果仓库选择不新增该测试，则在 `TileRendererCanvasTest` 补等价断言。
- 当前 `client/src/test/kotlin/com/ktome/client` 下没有 `EquipmentInventoryPresenterTest.kt`。

影响：

文档同时给出强制和可选两种路线。开发者如果选择不新增 test，验证命令会变成错误合同；如果新增 test，后面的 fallback 条款又会让 reviewer 无法判断 `TileRendererCanvasTest` 是否必须覆盖同一行为。

修复方向：

二选一，不要保留双路线：

1. 推荐：正式新增 `EquipmentInventoryPresenterTest`，删除 “如果仓库选择不新增” 条款。
2. 如果坚持不新增：从命令中删除该 `--tests`，并把所有断言 exact name 迁移到 `TileRendererCanvasTest`。

#### P2-4 `EquipmentSlotLabels.kt` 被描述为 icon key 映射 owner，但当前文件只负责 label

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:50` 写 `EquipmentSlotLabels.kt` 保持 slot label 与 icon key 映射。
- `client/src/main/kotlin/com/ktome/client/render/EquipmentSlotLabels.kt:5-14` 当前只把 `WEAPON / OFF_HAND / ARMOR` 映射到 locale label。
- `UI/PLAN.md:335-341` 把 slot frame key 归为 `ui.frame.slot.*`，不要求在 label helper 内维护 item/icon 映射。

影响：

按文档改动会诱导开发者把视觉 key 映射塞进 label helper，形成 label 与资源 mapping 的混合 owner。后续 locale、manifest 和 renderer 维护都会变得不清晰。

修复方向：

把影响范围改为：

```text
EquipmentSlotLabels.kt: 只维护 slotId -> labelKey/localized label，不维护 icon key。
新增或复用 `EquipmentSlotPresentation`/`EquipmentInventoryPresenter`: 负责 slot frame key、selected/empty/equipped 状态、tooltip anchor。
item icon source: `ItemRenderSnapshot.iconKey`。
slot frame source: `ui.frame.slot.empty / equipped / selected` from PR-02 registry。
```

#### P2-5 `ui.shop.*` 新 key 与现有 companion key 没有桥接或删除计划

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:93-102` 建议新增 `ui.shop.offer.frame`、`ui.shop.price.*`、`ui.shop.offer.disabled`、`ui.shop.inscription.marker`、`ui.shop.replacement.slot_marker`。
- 当前代码 `client/src/main/kotlin/com/ktome/client/ui/UiCompanionVisualKeys.kt:4-8` 已有 `ui.empty.inventory.icon`、`ui.empty.shop.icon`、`ui.card.shop.header.icon`、`ui.card.reward.header.icon`。
- `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt:1866-1887` 的 resolver fixture 已经使用这些 PR-03 旧 companion keys。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md` 没有说明这些 existing keys 是保留、alias、迁移还是删除。

影响：

PR-03 可能同时保留 `ui.card.shop.header.icon` 和新增 `ui.shop.offer.frame`，但没有说明两者谁是 primary source、谁是 alias、谁参与 coverage。后续 PR-06/PR-07 收口 fallback 时会难以判断旧 key 是否残留。

修复方向：

新增 `Existing Key Migration` 表：

```text
existingKey | action | targetKey/aliasOf | removalOwner | consumerTest
ui.empty.inventory.icon | keep | N/A | N/A | TileRendererCanvasTest / dark-uiux-pr03-inventory-empty
ui.empty.shop.icon | keep | N/A | N/A | TileRendererCanvasTest / dark-uiux-pr03-inscription-shop
ui.card.shop.header.icon | keep or alias | ui.shop.offer.frame? | PR-03 or PR-07 | ModalCardModelTest
ui.card.reward.header.icon | out of scope or keep | N/A | PR-07 | ModalCardModelTest
```

如果决定迁移到 `ui.shop.*`，必须写入 `removalOwner` 和 regression scan；如果决定保留，必须把这些 key 纳入 PR-03 key registry 和 coverage summary。

#### P2-6 人工白盒只有步骤标题，没有 scenario、输入序列、expected evidence、skip rule

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:146-154` 只列人工观察项和 evidence label。
- `UI/pr/development-governance.md:30-37` 要求 whitebox 值与 skipped 原因。
- `docs/review/rule/pr-level-review-standard.md:417-418` 要求每个 manual white-box 都有 scenario、steps、expected evidence 和 skip rule。

影响：

开发者无法知道用哪个 seed、哪个 validation scenario、怎样制造满槽、怎样注入缺图、截图/日志放哪，以及失败时是否允许跳过。人工白盒会退化为“看起来确认过”，无法成为可复审证据。

修复方向：

把 §8 改成表：

```text
label | scenario/setup | input sequence | expected visible result | expected artifact | skip rule | residual risk
dark-uiux-pr03-equipment-slots | validation scenario or deterministic fixture | ... | WEAPON/OFF_HAND/ARMOR visible ... | UI/manual-records/... + screenshot | only skipped on headless CI, must keep golden | ...
dark-uiux-pr03-shop-full-slot-replace | phase4-v4-pr02 compatible setup or new dark-uiux scenario | F9, Right, Enter, Down, Down, Enter, 5, Enter | hotkeys 5-8, no shard loss before confirm | ... | ...
```

### P3

#### P3-1 标题与索引名称不一致

证据：

- `UI/pr/README.md:13` 链接名是 `PR-03 Equipment Inventory Items`。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:1` 标题是 `Dark UI/UX PR-03 Equipment Inventory Items And Shop`。

影响：

不改变实现路径，但降低搜索和报告引用的一致性。建议统一为 `Dark UI/UX PR-03 Equipment Inventory Items And Shop`，并同步 README 链接文字，或保持 README 名称并在目标文档 subtitle 中说明 shop 被 PR-03 纳入同一 UX family。

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR-03 执行顺序和依赖 | `UI/pr/README.md:21-35`、目标文档 `:6` | 部分一致。依赖 PR-02 已声明，但 Acceptance Matrix 没有 `crossPrDependency` 或等价字段。 |
| 资源生成 Round 7 | `UI/pr/README.md:80-83`、目标文档 `:64-79` | 部分一致。sheet 名称存在，但 cell/key inventory 不足以直接生成。 |
| 装备/背包 UI | 目标文档 `:38-41`、`:115-123`、`UI/pr/screen-coverage-matrix.md:38-39` | 不一致。缺布局、identity、overflow、equipment evidence label。 |
| 铭文商店 UI | 目标文档 `:41`、`:120-123`、`RenderSnapshot.kt:520-548` | 部分一致。shop modal 和 offer 输入存在，但 disabled/purchased/threatened state source 未冻结。 |
| 验证 gate | 目标文档 `:127-141`、`tools/build.gradle.kts:597-636` | 部分一致。声明了核心 gate，但 sync 顺序和 required sheet ids 不完整。 |
| 人工白盒 | 目标文档 `:146-154`、standard `:417-418` | 部分一致。label 有，但 scenario、输入序列、expected artifact、skip rule 不足。 |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前代码/文档状态 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |
| `core` inscription hotkey | replacement 使用 `1-6` | 核心合同是 4 slots, hotkeys `5-8` | 文档错误，会误导输入实现 | P1 |
| `core`/snapshot inventory | 要求 quantity badge / stacked | snapshot 无 quantity/stack 字段 | UI 状态数据源未冻结 | P1 |
| `game`/snapshot shop | 要求 purchased/disabled/threatened/disabled reason | `ShopOfferSnapshot` 无这些字段 | source of truth 不唯一 | P1 |
| `client` renderer/presenter | 要求 grid、hitbox、tooltip | 当前仍是 row-based presentation surface | 缺 layout/presentation model 合同 | P1 |
| `assets` Round 7 | 要求生成三张 item sheet | 目标文档无 cell/key inventory | 无法直接执行资源开发 | P1 |
| `tools` coverage | owner-scope PR-03 coverage | task 支持 requiredOwnerSheetIds，文档未用 | 可漏掉整张 sheet | P1 |
| `docs` evidence | PR-03 必填 evidence | 缺 `dark-uiux-pr03-equipment-slots` | 装备面板验收缺口 | P1 |

## 玩法与体验审查

PR-03 是 player-facing surface。当前文档方向正确：装备/背包、shop、tooltip、replacement modal 都属于高频扫描和重复决策路径。但文档没有把“玩家实际会怎样比较装备、识别可买/不可买、理解满槽替换风险、在最小窗口继续操作”落成状态机和 evidence。

必须优先补齐：

1. 装备/背包 grid 的稳定布局、selection、tooltip anchor 和 overflow。
2. shop offer 的 affordability/disabled reason 数据来源。
3. replacement modal 的正确 hotkey `5-8`、取消和确认路径。
4. equipment slots 独立 golden/manual evidence。

## 当前阶段必须解决的问题

合并或进入开发前必须先修：

1. P1-1 热键合同错误。
2. P1-2 quantity / shop state source of truth。
3. P1-3 Round 7 cell/key inventory 和 required sheet ids。
4. P1-4 grid layout / focus / identity / overflow 合同。
5. P1-5 equipment slots evidence label。
6. P1-6 validation order。

可以作为同轮文档整理修复：

1. P2-1 影响范围补全。
2. P2-2 exact artifact path。
3. P2-3 测试路线二选一。
4. P2-4 `EquipmentSlotLabels.kt` owner 纠偏。
5. P2-5 existing key migration/removal plan。
6. P2-6 whitebox 表格化。

## Removal/Iteration Plan

当前文档没有 Deleted 清单，也没有 removalOwner。建议补：

| Path / Contract | Action | Owner | Regression Scan |
| --- | --- | --- | --- |
| 旧文本装备列表表现 | replace with grid/presentation model | PR-03 | `TileRendererCanvasTest` 确认装备 rows 不再作为唯一 player-facing 表达 |
| `ui.card.shop.header.icon` 等 existing PR-03 companion keys | keep/alias/remove 必须二选一 | PR-03 或 PR-07 | `rg -n "ui.card.shop.header.icon|ui.empty.shop.icon|ui.empty.inventory.icon"` |
| `missing_visual` 玩家主路径 fallback | forbidden except explicit injection test | PR-03 | `darkManifestCoverageLint owner-scope` + fallback injection record |
| optional test fallback | delete one branch | PR-03 | validation command contains only real test classes |

## Additional Suggestions

1. 在目标文档中新增 `Implementation Order`：resource inventory -> key registry/sheet plan -> canonical manifest -> sync runtime -> presenter model -> renderer -> input/shop modal -> focused tests -> golden/manual。
2. 把 `UI03-M03` 拆成 `equipment slots`、`inventory grid`、`tooltip/detail` 三行，避免一个 matrix row 覆盖过多 player-facing 行为。
3. 把 `UI03-M06` 拆成 `shop buy/sell cards` 与 `replacement modal input` 两行，分别绑定 `ModalCardModelTest` 和 `InputHandlerTest`。
4. 在 PR 文档中明确 `clientSmoke` 和 `goldenScreenshot` 只证明运行时画面，不替代 resource lint、manifest coverage 或 focused presenter tests。

## Open Questions

1. PR-03 是否真的要交付 stack count？如果是，需要扩展 snapshot 或定义清晰的展示派生；如果不是，应从 blocking requirement 和 evidence label 中删除 “stacked/数量 badge”。
2. `purchased / shop threatened / disabled reason` 是否已有 game-side source？当前 `ShopOfferSnapshot` 未暴露。若不新增字段，文档应把这些状态降为 post-command feedback，而不是 offer card 预渲染状态。
3. `ui.card.shop.header.icon` 是否保留为正式 key，还是迁移到 `ui.shop.offer.frame`？这需要 PR-03 冻结，不能留到实现时判断。

## Suggested Verification

已运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：`BUILD SUCCESSFUL`。

文档修复后建议运行：

```bash
git diff --check -- UI/pr/dark-uiux-pr03-equipment-inventory-items.md UI/pr/README.md
awk 'BEGIN{c=0} /^```/{c++} END{print FILENAME ":FENCE_OPEN=" c%2}' UI/pr/dark-uiux-pr03-equipment-inventory-items.md UI/pr/README.md
rg -n --pcre2 '(^|[[:space:]("\"=])/(?:[^[:space:]/]+/){2,}[^[:space:]"\")]*|[A-Za-z]:\\\\' UI/pr/dark-uiux-pr03-equipment-inventory-items.md UI/pr/README.md
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

如果同时修改 `tools/build.gradle.kts` 或 coverage task 接线，追加：

```bash
./scripts/verify-bootstrap.sh
```

## Summary

PR-03 当前不是“无需猜测即可开发”的文档。最大问题不是缺少目标，而是多个关键合同没有冻结：铭文 replacement 热键写错、Round 7 资源 key/cell 不可执行、quantity/shop state 缺数据源、grid/focus/overflow 未建模、equipment slots 缺独立 evidence、验证顺序可能用旧 runtime manifest。建议先修 P1，再进入开发。

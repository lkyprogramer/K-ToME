# Dark UI/UX PR-03 Equipment Inventory Items And Shop

**阶段**: `dark-uiux-pr03-equipment-inventory-items`
**优先级**: `P0`
**工作量**: `L`
**前置条件**: PR-02 完成。
**资源生成结论**: 生成 Round 7 的 item/equipment/material/affix 资源子集。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再跑 equipment / inventory focused tests、resource gate、client evidence 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI03-M01` | §3 Round 7 item sheet 内容 | `assets` | `darkSpriteSheetLint`, `spriteSheetMapLint` | `assetLint`, `styleLint` | `assets-src/image/contact-sheets/dark-v1/` | `required` |
| `UI03-M02` | §4 key registry 要求 | `tools` | `darkKeyRegistryLint`, `ManifestResolveTest` | `manifestLint`, `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03` | dark manifest coverage report | `N/A` |
| `UI03-M03` | §6 equipment / inventory 必测行为 | `client` | `EquipmentInventoryPresenterTest`, `TileRendererCanvasTest` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/golden/` | `required` |
| `UI03-M04` | §7 验证命令 / locale contract | `client` / `tools` | `localeLint`, `contractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |
| `UI03-M05` | §8 人工白盒 | `docs` / `client` | manual evidence checklist | packaged or debug whitebox record | `UI/manual-records/` | `required` |
| `UI03-M06` | §6 铭文商店 / 满槽替换 modal | `client` | `InputHandlerTest`, `DescriptionPresenterTest`, `ModalCardModelTest` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/golden/` | `required` |

### Gate Budget

预计重型任务：`assetLint`、`styleLint`、`manifestLint`、dark sprite / registry lints、`:client:clientSmoke`、`:client:goldenScreenshot`、`localeLint`、`contractLint`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-03 同时改 item resources、inventory UI、铭文商店、tooltip / empty state 和 manifest coverage。

### Canonical Artifact

canonical artifact 固定为 Round 7 sheet/contact sheet、key registry、manifest coverage report、runtime manifest、client golden 和 `UI/manual-records/`。fallback key injection record 必须 repo-relative。

### Failure Rule

inventory/shop golden 或 coverage 失败时先修 presenter、resource key 或 manifest mapping；不得用 fallback key 掩盖缺资源，也不得只靠人工白盒证明 grid / tooltip / shop replacement 行为。

## 1. 阶段目标

1. 右侧玩家面板正式展示装备格、背包 grid、资源计数。
2. 装备、物品、材料、affix 图标化，替换第一张截图里纯文字装备列表。
3. 空背包、空装备、不可用物品、数量 badge、tooltip 与选中态有统一表现。
4. 铭文商店 / shop 进入同一装备物品 UX family：buy/sell 双列、offer card、价格、affordability、禁用原因、空态、tooltip、满槽替换 modal 必须统一暗黑化。
5. Round 7 分批生成 item/equipment/material/affix sheet，但不修改掉落、商店经济或铭文规则。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | 补装备/背包 presentation model |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | 绘制装备格、背包 grid、tooltip |
| `client/src/main/kotlin/com/ktome/client/render/EquipmentSlotLabels.kt` | 保持 slot label 与 icon key 映射 |
| `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` | 保持 shop buy/sell、replacement prompt、hotkey/cancel 语义 |
| `client/src/main/kotlin/com/ktome/client/ui/talent/DescriptionPresenter.kt` | 复用 shop item lines，不在 renderer 拼业务文案 |
| `client/src/main/kotlin/com/ktome/client/ui/card/ModalCardModel.kt` | shop offer 与 replacement modal 卡片化时复用已有 modal/card 模型 |
| `assets-src/image/manifests/phase2-visual-manifest.json` | 先更新 item/equipment icon canonical manifest |
| `client/src/main/resources/manifests/visual-manifest.json` | 由 `syncPhase2Manifests` 同步生成 runtime manifest |
| `UI/sprite-sheets/sheet-plan.yaml` | 增加 Round 7 cell |
| `client/src/test/kotlin/com/ktome/client/render/EquipmentInventoryPresenterTest.kt` | 覆盖数量 badge、quality frame、slot hitbox |
| `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt` | 覆盖 shop focus、buy/sell、铭文满槽替换 hotkey、取消与无 hotkey |
| `client/src/test/kotlin/com/ktome/client/ui/talent/DescriptionPresenterTest.kt` | 覆盖 shop item tooltip / description line |
| `client/src/test/kotlin/com/ktome/client/ui/card/ModalCardModelTest.kt` | 覆盖 offer card、modal 行距、长文本和 disabled reason |

## 3. Round 7 Sheet 内容

1. `r07-items-base`: 基础武器、护甲、药水、金币、钥匙、材料。
2. `r07-items-unique-artifact`: unique、artifact、relic、boss reward。
3. `r07-items-affix-material`: affix marker、quality marker、craft material。
4. 每张 item sheet 固定为 `icon-sheet 1024x1024 / 8x8 / 128x128`。
5. 切分后的 icon runtime canvas 默认由 category policy 输出到 `160x160`；装备/背包 UI 显示 `48x48/64x64` 由 renderer/layout 决定。
6. 装备 slot frame 属于 `ui_frame`，优先复用 PR-02 的 `ui.frame.slot.*`，不得伪装成 item key。
7. 任何 icon 不允许烘焙数量、品质文字或快捷键数字；数量和品质由 renderer 叠加。
8. 铭文 offer 复用 item/inscription 主体 icon；shop card、价格 marker、affordability marker 和 disabled marker 可以新增 `ui.shop.*` 或复用 PR-02 `ui.control.*`，但必须进入 key registry。

Raw sheet 生成交接：

1. 先按 PR-00 固定命令生成 Round 7 prompt 文件和 `prompt-index.json`。
2. 逐个执行 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --overwrite`，由脚本调用 Codex CLI 并复制最新生成图片到 `sheet-plan.yaml.rawSheetPath` 指定位置。
3. 文件名必须分别为 `r07-items-base.png`、`r07-items-unique-artifact.png`、`r07-items-affix-material.png`。
4. raw PNG 缺失或尺寸不匹配时，本 PR 不进入切分和 manifest patch。
5. PR 描述必须列出 prompt path、raw sheet path、raw sheet hash、Codex CLI transient source folder/source image 摘要、切分报告和 contact sheet QA path。

## 4. Key Registry 要求

1. `item.*.icon` 只指向物品主体图。
2. empty slot、locked slot、selected/equipped slot 统一复用 PR-02 的 `ui.frame.slot.*`；如果视觉上复用同一图，必须在 key registry 用 `aliasOf` 表达。
3. quality frame 如需独立边框资源，必须归 `ui_frame` 并回到 PR-02/PR-00 registry 处理；PR-03 的 `r07-items-affix-material` 只生成 item/affix/material 主体或小 marker。
4. `fallbackKey` 必须是 dark style fallback，不允许直接使用旧风格 `missing_visual` 作为玩家主路径。
5. PR close 前输出 item/equipment key coverage summary，至少包含 `expectedItemKeys`、`coveredItemKeys`、`missingItemKeys`、`allowedFallbackKeys`。
6. `ui.shop.*` key 只表示 shop presentation，不得编码价格、库存、购买规则或 offer id。
7. `ui.shop.inscription.marker` 表示 offer 类型 marker，不能替代 inscription 本体 icon；本体 icon 仍走 `item.*.icon`、`icon.*` 或后续 PR-06 归口。
8. `ui.shop.price.affordable`、`ui.shop.price.unaffordable`、`ui.shop.offer.disabled` 可以是 marker/glyph；禁用原因必须来自 snapshot/presenter/locale token，不得烘焙到图片。
9. `ui.shop.replacement.slot_marker` 只用于满槽替换 modal 的槽位视觉；hotkey `1-6 / Esc` 由 renderer 文本层叠加。

建议最小 shop key：

| targetKey | Category | Sheet | fallbackKey | Consumer | consumerTest |
| --- | --- | --- | --- | --- | --- |
| `ui.shop.offer.frame` | `ui_frame` | alias `r01-ui-chrome` | `ui.frame.panel.body` | shop offer card | `ModalCardModelTest` / `dark-uiux-pr03-inscription-shop` |
| `ui.shop.price.affordable` | `icon` | `r07-items-affix-material` | `ui.hud.gold.icon` | shop price marker | `ManifestResolveTest` |
| `ui.shop.price.unaffordable` | `icon` | `r07-items-affix-material` | `ui.hud.warning.icon` | shop disabled price marker | `ManifestResolveTest` |
| `ui.shop.offer.disabled` | `icon` | `r07-items-affix-material` | `ui.combat.invalid.icon` | disabled shop offer | `ManifestResolveTest` |
| `ui.shop.inscription.marker` | `icon` | `r07-items-affix-material` | `ui.hud.quest_marker.icon` | inscription offer marker | `DescriptionPresenterTest` |
| `ui.shop.replacement.slot_marker` | `icon` | `r07-items-affix-material` | `ui.frame.slot.selected` | full-slot replacement modal | `InputHandlerTest` / `dark-uiux-pr03-shop-full-slot-replace` |

## 5. 非目标

1. 不新增物品规则、掉落权重、loot budget 或数值。
2. 不新增额外 rarity / special tier。
3. 不改职业树 UI。
4. 不改 `core` item model，只消费现有 snapshot/manifest key。
5. 不改 shop refresh、shop rescue affordability、gold/shard 花费、inscription equip/replace 规则。
6. 不新增 shop screen 的鼠标拖拽或点击购买语义；当前阶段保持键盘/现有 input contract。

## 6. 必测行为

1. 装备格和背包格固定尺寸，不因文本、数量或 icon 加载变化跳动。
2. 数量 badge 不遮挡 item 主体；品质 frame 不影响点击/选中区域。
3. 缺 icon 时出现正式 fallback，不破坏整体暗黑风格。
4. Tooltip 文字来自现有 presenter/token，不在 renderer 拼业务文案。
5. Shop buy/sell 双列在 `1280x800` 和最小窗口下不与 HUD、日志、tooltip 或 replacement modal 重叠。
6. Affordable / unaffordable / purchased / disabled offer 必须有颜色以外的图形或文字差异；不得只靠红绿区分。
7. 铭文 offer 必须显示 offer 类型、价格、目标 inscription、冷却/效果摘要入口；长描述进入 tooltip/description 区，不撑大 offer card。
8. 满槽购买铭文时必须先显示 replacement modal；`1-6` 替换、取消、无 hotkey、确认后 command fingerprint 均沿用现有 input contract。
9. 购买失败、金币不足、shop threatened、空商店、空 sell list 都必须有暗黑 empty/disabled state，不得退回纯文本占位。

## 7. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint :client:test --tests com.ktome.client.render.EquipmentInventoryPresenterTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.ui.talent.DescriptionPresenterTest --tests com.ktome.client.ui.card.ModalCardModelTest --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint verifyChanged
./gradlew syncPhase2Manifests manifestLint
```

本 PR 的 dark gate 必须使用显式 owner-scope 命令：

```bash
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03
```

只强制 Round 7 item/equipment/affix/material、背包 UI key 与 `ui.shop.*` key；scope 外 pending 必须写入 coverage artifact。不得用裸 `darkManifestCoverageLint` 代替本 PR close gate。

如果仓库选择不新增 `EquipmentInventoryPresenterTest`，必须在 `TileRendererCanvasTest` 中补等价断言：badge 相对坐标稳定、quality frame 不参与选中 hitbox、slot frame 不遮挡 item 主体。
如果仓库选择不新增专门 shop presenter test，必须在 `TileRendererCanvasTest` 或 `ModalCardModelTest` 中补等价断言：offer card 固定尺寸、buy/sell 双列不重叠、replacement modal 位于 overlay 层且不遮挡确认文案。

## 8. 人工白盒

1. 打开背包和装备面板，确认空格、已装备、可拾取物品都有 icon。
2. 拾取多个可堆叠物品，确认数量 badge 位置稳定。
3. 对比普通、unique、artifact 图标，确认品质差异可见但不抢占地图视觉。
4. 缺失 icon 的测试资源必须显示 fallback，并在日志或 report 中可定位 key。
5. 打开铭文商店，确认 buy/sell 双列、offer card、价格、禁用原因、空态、tooltip 均为暗黑统一风格。
6. 在满槽铭文购买场景触发 replacement modal，确认 `1-6`、取消、选中态、目标槽位、购买结果反馈正确。
7. 必填证据：`dark-uiux-pr03-inventory-empty`、`dark-uiux-pr03-inventory-stacked`、`dark-uiux-pr03-inscription-shop`、`dark-uiux-pr03-shop-full-slot-replace`、fallback key injection record。

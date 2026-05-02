# Dark UI/UX PR-03 Equipment Inventory Items

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

### Gate Budget

预计重型任务：`assetLint`、`styleLint`、`manifestLint`、dark sprite / registry lints、`:client:clientSmoke`、`:client:goldenScreenshot`、`localeLint`、`contractLint`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-03 同时改 item resources、inventory UI、tooltip / empty state 和 manifest coverage。

### Canonical Artifact

canonical artifact 固定为 Round 7 sheet/contact sheet、key registry、manifest coverage report、runtime manifest、client golden 和 `UI/manual-records/`。fallback key injection record 必须 repo-relative。

### Failure Rule

inventory golden 或 coverage 失败时先修 presenter、resource key 或 manifest mapping；不得用 fallback key 掩盖缺资源，也不得只靠人工白盒证明 grid / tooltip 行为。

## 1. 阶段目标

1. 右侧玩家面板正式展示装备格、背包 grid、资源计数。
2. 装备、物品、材料、affix 图标化，替换第一张截图里纯文字装备列表。
3. 空背包、空装备、不可用物品、数量 badge、tooltip 与选中态有统一表现。
4. Round 7 分批生成 item/equipment/material/affix sheet，但不修改掉落规则。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | 补装备/背包 presentation model |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | 绘制装备格、背包 grid、tooltip |
| `client/src/main/kotlin/com/ktome/client/render/EquipmentSlotLabels.kt` | 保持 slot label 与 icon key 映射 |
| `assets-src/image/manifests/phase2-visual-manifest.json` | 先更新 item/equipment icon canonical manifest |
| `client/src/main/resources/manifests/visual-manifest.json` | 由 `syncPhase2Manifests` 同步生成 runtime manifest |
| `UI/sprite-sheets/sheet-plan.yaml` | 增加 Round 7 cell |
| `client/src/test/kotlin/com/ktome/client/render/EquipmentInventoryPresenterTest.kt` | 覆盖数量 badge、quality frame、slot hitbox |

## 3. Round 7 Sheet 内容

1. `r07-items-base`: 基础武器、护甲、药水、金币、钥匙、材料。
2. `r07-items-unique-artifact`: unique、artifact、relic、boss reward。
3. `r07-items-affix-material`: affix marker、quality marker、craft material。
4. 每张 item sheet 固定为 `icon-sheet 1024x1024 / 8x8 / 128x128`。
5. 切分后的 icon runtime canvas 默认由 category policy 输出到 `160x160`；装备/背包 UI 显示 `48x48/64x64` 由 renderer/layout 决定。
6. 装备 slot frame 属于 `ui_frame`，优先复用 PR-02 的 `ui.frame.slot.*`，不得伪装成 item key。
7. 任何 icon 不允许烘焙数量、品质文字或快捷键数字；数量和品质由 renderer 叠加。

Raw sheet 生成交接：

1. 先按 PR-00 固定命令生成 Round 7 prompt 文件和 `prompt-index.json`。
2. lky 在 Codex app 中执行 prompt，并将 PNG 放到 `sheet-plan.yaml.rawSheetPath` 指定位置。
3. 文件名必须分别为 `r07-items-base.png`、`r07-items-unique-artifact.png`、`r07-items-affix-material.png`。
4. raw PNG 缺失或尺寸不匹配时，本 PR 不进入切分和 manifest patch。
5. PR 描述必须列出 prompt path、raw sheet path、raw sheet hash、切分报告和 contact sheet QA path。

## 4. Key Registry 要求

1. `item.*.icon` 只指向物品主体图。
2. empty slot、locked slot、selected/equipped slot 统一复用 PR-02 的 `ui.frame.slot.*`；如果视觉上复用同一图，必须在 key registry 用 `aliasOf` 表达。
3. quality frame 如需独立边框资源，必须归 `ui_frame` 并回到 PR-02/PR-00 registry 处理；PR-03 的 `r07-items-affix-material` 只生成 item/affix/material 主体或小 marker。
4. `fallbackKey` 必须是 dark style fallback，不允许直接使用旧风格 `missing_visual` 作为玩家主路径。
5. PR close 前输出 item/equipment key coverage summary，至少包含 `expectedItemKeys`、`coveredItemKeys`、`missingItemKeys`、`allowedFallbackKeys`。

## 5. 非目标

1. 不新增物品规则、掉落权重、loot budget 或数值。
2. 不新增额外 rarity / special tier。
3. 不改职业树 UI。
4. 不改 `core` item model，只消费现有 snapshot/manifest key。

## 6. 必测行为

1. 装备格和背包格固定尺寸，不因文本、数量或 icon 加载变化跳动。
2. 数量 badge 不遮挡 item 主体；品质 frame 不影响点击/选中区域。
3. 缺 icon 时出现正式 fallback，不破坏整体暗黑风格。
4. Tooltip 文字来自现有 presenter/token，不在 renderer 拼业务文案。

## 7. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint :client:test --tests com.ktome.client.render.EquipmentInventoryPresenterTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint verifyChanged
./gradlew syncPhase2Manifests manifestLint
```

本 PR 的 dark gate 必须使用显式 owner-scope 命令：

```bash
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03
```

只强制 Round 7 item/equipment/affix/material 与背包 UI key；scope 外 pending 必须写入 coverage artifact。不得用裸 `darkManifestCoverageLint` 代替本 PR close gate。

如果仓库选择不新增 `EquipmentInventoryPresenterTest`，必须在 `TileRendererCanvasTest` 中补等价断言：badge 相对坐标稳定、quality frame 不参与选中 hitbox、slot frame 不遮挡 item 主体。

## 8. 人工白盒

1. 打开背包和装备面板，确认空格、已装备、可拾取物品都有 icon。
2. 拾取多个可堆叠物品，确认数量 badge 位置稳定。
3. 对比普通、unique、artifact 图标，确认品质差异可见但不抢占地图视觉。
4. 缺失 icon 的测试资源必须显示 fallback，并在日志或 report 中可定位 key。
5. 必填证据：`dark-uiux-pr03-inventory-empty`、`dark-uiux-pr03-inventory-stacked`、fallback key injection record。

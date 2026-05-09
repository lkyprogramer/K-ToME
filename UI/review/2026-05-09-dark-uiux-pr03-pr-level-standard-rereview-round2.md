# Dark UI/UX PR-03 PR-Level Standard Rereview Round 2

目标文档：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md`

审查标准：`docs/review/rule/pr-level-review-standard.md`

审查时间：2026-05-09

## Precheck

- 当前分支：`main...origin/main`
- 工作树状态：存在多份 UI/pr 文档、dark UI/UX 验证脚本和 review 报告的未提交改动。本轮只审查 `UI/pr/dark-uiux-pr03-equipment-inventory-items.md` 及其直接依赖真源，不回退或覆盖无关改动。
- 命中系列：dark UI/UX PR 系列，执行入口为 `UI/pr/README.md`，PR-03 位于 PR-02 之后，目标是装备/背包 grid、item icon、铭文商店、quality、空态/tooltip。
- 受影响模块：`client`、`assets`、`tools`、`docs`，以及本轮新发现会被 PR-03 实际触碰的 `game` 数据资源键。
- 触碰合同：visual manifest、key registry、sheet plan、item icon key、shop/replacement input、golden/manual evidence、dark manifest coverage、sprite map report。
- 已运行验证：`source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint`，结果 `BUILD SUCCESSFUL`。该 gate 证明文档结构可被现有 contract lint 读取，但不能证明本报告中的语义、资源 owner 和命令参数均正确。

## 上轮 P1 状态

| 上轮 ID | 状态 | 说明 |
| --- | --- | --- |
| `pr03-r1#P1-1` | 已解决 | replacement hotkey 已改为 `5-8`，并对齐 `InscriptionReplacementPromptSnapshot.currentSlots`。 |
| `pr03-r1#P1-2` | 基本解决 | quantity / purchased / disabled / threatened 的 snapshot source 已声明为默认非 blocking；但本轮仍发现部分 resource key 和 evidence 还在暗含 disabled/pre-rendered 状态，见 `P1-1`、`P2-3`。 |
| `pr03-r1#P1-3` | 部分解决 | Round 7 cell inventory 和 required sheet ids 已补；但 sprite map report 命令仍会写到 PR-00 默认路径，见 `P1-3`。 |
| `pr03-r1#P1-4` | 已解决 | equipment / inventory layout、identity、fallback、tooltip anchor 已结构化。 |
| `pr03-r1#P1-5` | 已解决 | `dark-uiux-pr03-equipment-slots` 已进入 Acceptance Matrix、manual whitebox 和 README evidence matrix。 |
| `pr03-r1#P1-6` | 已解决 | `syncPhase2Manifests` 已前置到 resolver/golden/verifyChanged 之前。 |

## Findings

### P0

无。

### P1

#### P1-1 `ui.shop.offer.frame` 被放进 Round 7 item sheet，和 sheet ownership / alias 合同冲突

证据：

- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:109` 定义 `r07-items-affix-material` 是 affix marker、quality marker、craft material、shop marker。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:165` 却把 `ui.shop.offer.frame` 放在 `r07-items-affix-material`，category 为 `ui_frame`，并生成 `dark-v1/ui/ui_shop_offer_frame.png`。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:179` 又写明 `r07-items-affix-material` 只生成 item/affix/material 主体或小 marker，frame 类资源应回到 PR-02/PR-00 registry。
- `UI/pr/README.md:80-82` 的 SheetId Ownership 写死 `r07-items-affix-material` 的 Cell Categories 是 `icon`，不是 `ui_frame`。
- `UI/PLAN.md:363` 已把 `ui.shop.offer.frame` 定义为 shop offer card frame，默认 alias 到 PR-02 panel frame。

问题：

同一个 key 同时被描述为 “Round 7 新生成 frame PNG” 和 “默认 alias 到 PR-02 panel frame”。开发者按当前 PR-03 实现，要么违反 `UI/pr/README.md` 的 sheet category owner，要么绕过 `UI/PLAN.md` 的 alias 语义生成第二份 frame 资源。

影响：

这会直接影响 `sheet-plan.yaml`、`key-registry.yaml`、manifest entry、`ModalCardModelTest` 和 coverage 分母。更严重的是，shop card frame 会变成 PR-03 的第二套 UI frame authority，和 PR-02 chrome/frame owner 产生漂移。

修复方向：

二选一，不能保留当前混合写法：

1. 推荐：从 PR-03 Round 7 cell inventory 删除 `ui.shop.offer.frame` 这一行，shop card 直接复用 `ui.frame.panel.body` 或 PR-02 已冻结的 frame key；`ModalCardModelTest` 验证消费的是 PR-02 frame key。
2. 如果必须保留 `ui.shop.offer.frame` 这个语义 key，则在 PR-03 文档中明确它是 alias，不生成新 PNG，并同步修 `UI/pr/README.md` / `UI/PLAN.md` / key registry / sheet plan 的 alias 落点。不能把它继续放在只允许 `icon` 的 `r07-items-affix-material`。

推荐验证：

- `darkKeyRegistryLint`
- `spriteSheetMapLint`
- `ModalCardModelTest.shopOfferFrameUsesPr02PanelAlias`

#### P1-2 Round 7 表新增了一批 item-specific icon key，但当前 item data 仍消费 generic/base key

证据：

- PR-03 cell inventory 要求生成 `item.hunter_bow.icon`、`item.war_maul.icon`、`item.forgebreaker_pick.icon`、`item.bandit_trophy.icon`、`item.emerald_charm.icon`、`item.seal_reliquary.icon`、`item.sanctified_seal.icon`、`item.energy_tonic.icon`、`item.consecrated_oil.icon` 等 key：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:125-140`。
- 当前 `hunter_bow` 仍使用 `item.base.rogue.weapon.icon`：`game/src/main/resources/data/items/index.yaml:2015-2019`。
- 当前 `war_maul` / `forgebreaker_pick` 仍使用 `item.base.vanguard.weapon.icon`：`game/src/main/resources/data/items/index.yaml:2033-2055`。
- 当前 `bandit_trophy`、`emerald_charm`、`seal_reliquary`、`sanctified_seal` 仍使用 `item.base.*.off_hand.icon`：`game/src/main/resources/data/items/index.yaml:2084-2156`。
- 当前 `energy_tonic` / `consecrated_oil` 仍分别复用 `item.mana_potion.icon` / `item.healing_potion.icon`：`game/src/main/resources/data/items/index.yaml:2201-2221`。
- PR-03 影响范围没有列出 `game/src/main/resources/data/items/index.yaml` 或 `game/src/main/resources/data/visuals/index.yaml`：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:66-95`。

问题：

PR-03 现在会生成一批看似完整的 item-specific icon key，但当前 gameplay snapshot 的 `ItemRenderSnapshot.iconKey` 不会消费其中一部分 key。文档没有说明是要同步改 `game` 数据里的 `iconKey / visualKey`，还是把 cell inventory 改回当前实际消费的 generic/base key。

影响：

如果只做 assets/client/tools，coverage 可以围绕 registry/manifest 变绿，但玩家背包和 shop 仍可能显示旧 generic 图标。这样 `EquipmentInventoryPresenterTest` 和 golden 也很容易只证明“resolver 能解析新 key”，不能证明真实 inventory/shop surface 消费了新 key。

修复方向：

PR-03 必须冻结一条路径：

1. 推荐：把 `game/src/main/resources/data/items/index.yaml` 和 `game/src/main/resources/data/visuals/index.yaml` 加入影响范围，明确本 PR 只更新 official content 的 visual/icon key，不改物品规则、掉落权重或数值；并把 focused test 增加为“真实 fixture 中这些 item 的 `ItemRenderSnapshot.iconKey` 等于 PR-03 targetKey”。
2. 如果不改 `game` 数据，则 Round 7 cell inventory 必须改成当前真实消费的 key，例如 `item.base.rogue.weapon.icon`、`item.base.vanguard.weapon.icon`、`item.mana_potion.icon` 等，并删除无人消费的 item-specific targetKey。

推荐验证：

- `DataLoader` 或 snapshot fixture test：`hunter_bow / war_maul / energy_tonic` 的 runtime `iconKey` 命中 PR-03 targetKey。
- `EquipmentInventoryPresenterTest` 使用真实 `InventoryEntrySnapshot` fixture，而不是手写未被 game data 消费的新 key。
- `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03 ...`

#### P1-3 `spriteSheetMapLint` 命令仍会写默认 PR-00 report，和 PR-03 canonical artifact 不一致

证据：

- PR-03 canonical artifact 和 Acceptance Matrix 固定 `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:17`、`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:44`、`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:87`。
- PR-03 命令只写 `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint`：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:274-277`。
- `spriteSheetMapLint` 的默认 report path 是 `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`：`tools/build.gradle.kts:541-555`。

问题：

开发者按 PR-03 文档执行命令，不会生成 PR-03 声明的 canonical sprite map report，而是继续写 PR-00 默认 report。当前文档把 artifact path 写对了，但命令没有把 artifact materialization 指到同一路径。

影响：

PR 描述、review、manual QA 和 `spriteSheetMapLint` 会看不同的 report。最坏情况下，PR-03 的 Round 7 QA report 缺失，而 PR-00 report 被覆盖或误用。

修复方向：

把 PR-03 命令改为：

```bash
./gradlew darkKeyRegistryLint darkSpriteSheetLint
./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl
```

同时把 Implementation Order 第 5 步写明：PR-03 sprite map 校验的 report path 必须等于 `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`。

推荐验证：

- `spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`
- `git diff --check -- assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`

### P2

#### P2-1 Round 7 表格没有说明哪些列进入 sheet-plan、哪些列进入 key-registry

证据：

- PR-03 写明 cell inventory 必须逐项落到 `UI/sprite-sheets/sheet-plan.yaml` 和 `UI/sprite-sheets/key-registry.yaml`：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:118`。
- 表格列包含 `fallbackKey`、`consumer`、`consumerTest`：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:120`。
- `sheet-plan.yaml` 允许的 cell input fields 只有 `row / col / targetKey / category / outputName / subject / reserved / aliasOf / note`：`scripts/dark_sprite_sheet_contract.py:29-36`。
- `verify_sprite_sheet_map.py` 会把额外 cell 字段报为 unsupported fields：`scripts/dark_sprite_sheet_contract.py:218-228`。

问题：

文档当前的“逐项落到 sheet-plan 和 key-registry”容易被实现者理解为把整张表复制到两个 YAML 中。这样 `fallbackKey / consumer / consumerTest` 一旦进入 `sheet-plan.yaml`，`darkSpriteSheetLint` 会失败。

影响：

这是典型的可实施性缺口：开发者不该靠读 Python 脚本才知道哪些列属于哪个真源。当前文字会造成 resource PR 的第一轮实现返工。

修复方向：

在 §3 表格后追加明确映射：

```text
sheet-plan.yaml receives: sheetId, row, col, targetKey, category, outputName, subject, reserved, aliasOf.
key-registry.yaml receives: targetKey, category, ownerPr=PR-03, sheetId, fallbackKey, consumer, consumerTest, aliasOf.
The table is an implementation contract, not a literal YAML schema.
```

推荐验证：

- `darkSpriteSheetLint`
- `darkKeyRegistryLint`

#### P2-2 manual whitebox 仍有几个“二选一”入口，没有冻结 deterministic scenario / injection fixture

证据：

- `dark-uiux-pr03-equipment-slots` 使用 “deterministic debug run”，但没有指定 validation scenario id、preset、seed 或 fixture：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:313`。
- `dark-uiux-pr03-inventory-empty` 同样只写 deterministic run，没有说明如何制造空/近空 inventory：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:314`。
- fallback injection 写成 “runtime manifest copy or resolver fixture” 二选一：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:318`。
- review 标准要求手工推演状态机时覆盖 debug / validation / manual probe 与正式 gameplay path 的隔离，无法推演唯一结果必须报问题：`docs/review/rule/pr-level-review-standard.md:141-152`。

问题：

manual 表已经比上轮完整，但仍把关键证据入口留给实现者选择。不同开发者可能用临时 debug run、修改 runtime manifest、改 resolver fixture 或手工截屏，都会“看似满足文档”。

影响：

后续复审无法机械判断 manual record 是否可复现；fallback injection 也可能污染正式 runtime manifest，或者只在 resolver fixture 中通过但没有覆盖真实 inventory/shop consumer。

修复方向：

每个 manual label 至少冻结一个 primary path：

- `scenarioId`：例如复用 `phase4-v4-pr02` 的 shop replacement 场景，或新增 `dark-uiux-pr03-equipment-inventory-items` validation scenario。
- `setupCommand` 或 fixture path：用于 empty inventory、repeated item、full inscription slots。
- fallback injection 只能选一个 primary：推荐 resolver fixture copy，禁止直接改正式 runtime manifest；如果必须改 manifest copy，明确 copy path 和 cleanup rule。
- manual record 必须记录 `scenarioId / inputSequence / evidenceLabel / screenshotPath / logPath / cleanupStatus`。

推荐验证：

- `InputHandlerTest` replacement prompt cases
- `TileRendererCanvasTest` empty/repeated inventory fixture cases
- fallback injection focused resolver test or dedicated report fixture test

#### P2-3 `ui.shop.offer.disabled` 资源仍缺少真实 consumer 状态

证据：

- PR-03 生成 `ui.shop.offer.disabled`，consumer 是 disabled shop offer marker：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:162`。
- 同一文档声明 `purchased / disabled / threatened offer` 在当前 `ShopOfferSnapshot / ShopPanelSnapshot` 没有字段，默认不预渲染：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:232`。
- 当前 `ShopOfferSnapshot` 只有 `index / labelKey / price / offerFingerprint / tags / tagLabelKeys`：`core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:530-537`。
- screen coverage matrix 仍要求 shop buy-sell 覆盖 disabled reason：`UI/pr/screen-coverage-matrix.md:40`。

问题：

PR-03 已正确把 disabled offer 预渲染降为非 blocking，但仍保留一个 `ui.shop.offer.disabled` key 并声明 consumer 是 disabled shop offer marker。当前没有 typed disabled offer state，这个 marker 到底用于什么状态仍不唯一。

影响：

资源开发会生成并登记一个没有明确 runtime consumer 的 key；client 实现可能为了使用它而重新在 presenter 里推断 disabled state，回到上轮已经避免的 source-of-truth 问题。

修复方向：

二选一：

1. 删除 `ui.shop.offer.disabled` 的 Round 7 cell，等 typed disabled offer source 出现后再引入。
2. 把 consumer 改成当前真实存在的表现状态，例如 “post-command insufficient-shards feedback marker”，并绑定具体 token、presenter/model 字段和测试名；不要叫 disabled offer marker。

推荐验证：

- `ModalCardModelTest` 只验证 affordability marker，不生成 disabled offer marker。
- 或新增明确的 post-command feedback presenter test。

### P3

#### P3-1 Acceptance Matrix 的 artifact 单元仍混用目录和 label，不是完全可定位 artifact

证据：

- `UI03-M03` artifact 写为 `client/build/reports/golden/`, `dark-uiux-pr03-equipment-slots`：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:19`。
- `UI03-M04`、`UI03-M05`、`UI03-M06` 同样混合目录和 label：`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:20-22`。
- review 标准要求 artifact 是 repo-relative evidence：`docs/review/rule/pr-level-review-standard.md:164-176`。

问题：

这不影响当前开发主路径，但后续 reviewer 仍要猜 golden label 对应的文件名、index 文件或 screenshot path。

修复方向：

在 Acceptance Matrix 中把 artifact 写成 “目录 + label + index/file rule”，例如：

```text
client/build/reports/golden/ (label: dark-uiux-pr03-equipment-slots; index entry required)
```

或在 `UI/pr/development-governance.md` 统一冻结 golden label 到文件路径的映射规则。

## Requirement Alignment

| Requirement | Current Status | Conclusion |
| --- | --- | --- |
| 上轮 P1 热键、state source、resource inventory、grid contract、equipment evidence、sync order | PR-03 已大幅补齐 | 基本一致 |
| Round 7 sheet ownership | `ui.shop.offer.frame` 与 `r07-items-affix-material` category/alias 冲突 | 不一致，P1 |
| 真实 item icon consumption | 多个新 targetKey 未被当前 item data 消费，影响范围未列 game data | 不一致，P1 |
| Sprite map report artifact | 文档路径是 PR-03，命令默认写 PR-00 | 不一致，P1 |
| Sheet-plan / key-registry column mapping | 文档表格列与 YAML schema 未明确拆分 | 部分一致，P2 |
| Manual / fallback evidence | 表格完整，但 deterministic scenario 和 injection fixture 未唯一 | 部分一致，P2 |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前真源 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |
| `assets` sheet ownership | `r07-items-affix-material` 生成 `ui.shop.offer.frame` | README 只允许 `icon`，UI/PLAN 要 alias 到 PR-02 frame | frame owner 冲突 | P1 |
| `game` item data | PR-03 生成 item-specific icons | 多个 item 仍用 generic/base iconKey | 新资源不会被真实 snapshot 消费 | P1 |
| `tools` sprite map | PR-03 canonical report path | `spriteSheetMapLint` 默认 PR-00 report | 证据产物跑偏 | P1 |
| `tools` sheet-plan schema | 表格包含 fallback/consumer/test | sheet-plan 不支持这些字段 | 实现者可能复制错 schema | P2 |
| `client` manual evidence | deterministic manual labels | scenario / fixture / injection path 未唯一 | 复审不可复现 | P2 |

## 玩法与体验审查

PR-03 的玩家目标已经比上一轮更清楚：装备/背包 grid、shop buy/sell、满槽替换和 fallback 都有具体 evidence label。剩余风险集中在“生成了视觉资源但真实 UI 没用到”以及“shop disabled marker 没有真实状态源”。这两类问题都会让玩家看到旧 generic icon 或看到不一致的 disabled/affordability 表达，不是纯文档洁癖。

当前最需要先收口的是 item key consumption：只要真实 `ItemRenderSnapshot.iconKey` 没切到 PR-03 targetKey，golden 看到的就不是 Round 7 资源改造。

## 当前阶段必须解决的问题

合并或进入实现前必须修：

1. `P1-1`：移除或重新归属 `ui.shop.offer.frame`，不能在 `r07-items-affix-material` 生成 `ui_frame`。
2. `P1-2`：冻结 item-specific key 的消费路径；要么同步改 `game` data，要么把 Round 7 targetKey 改为当前实际消费 key。
3. `P1-3`：给 `spriteSheetMapLint` 增加 PR-03 report path 参数，保证命令和 canonical artifact 一致。

同轮文档整理应修：

1. `P2-1`：补 sheet-plan / key-registry 列映射。
2. `P2-2`：冻结 deterministic scenario、fixture 和 fallback injection primary path。
3. `P2-3`：删除或重定义 `ui.shop.offer.disabled` 的当前 consumer。
4. `P3-1`：补 golden label 到 repo-relative artifact 的定位规则。

## Removal/Iteration Plan

| Item | Current Doc State | Required Adjustment |
| --- | --- | --- |
| `ui.shop.offer.frame` | 当前作为 PR-03 Round 7 generated `ui_frame` | 删除该 generated cell，或改成 PR-02 frame alias 并同步上游 owner 文档 |
| Generic/base item icon keys | 当前多个 runtime item 仍消费 generic key | PR-03 必须声明迁移到 item-specific key，或取消对应 item-specific targetKey |
| PR-00 sprite map default report | 当前 PR-03 命令未覆盖默认值 | 命令固定 `-Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl` |
| `ui.shop.offer.disabled` | 当前有资源 key 但无 typed disabled source | 删除/defer，或绑定到当前真实 post-command feedback source |

## Additional Suggestions

1. 在 PR-03 文档中新增 “Runtime Data Consumption” 小节，列出每个 item targetKey 对应的 `baseItemId -> iconKey/visualKey` 更新策略。
2. 将 Round 7 table 拆成两张表：`sheet-plan cells` 和 `key-registry entries`。目前一张大表适合 review，但不适合开发照抄。
3. 对 `dark-uiux-pr03-inscription-shop` 增加一个明确的 insufficient-shards / failed-buy feedback evidence，避免 disabled reason 只存在于 coverage matrix。

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
git diff --check -- UI/pr/dark-uiux-pr03-equipment-inventory-items.md UI/pr/README.md UI/PLAN.md
awk 'BEGIN { in_fence = 0 } /^```/ { in_fence = !in_fence } END { if (in_fence) { print "unclosed fence"; exit 1 } }' UI/pr/dark-uiux-pr03-equipment-inventory-items.md
./gradlew acceptanceContractLint
```

如果修复涉及 `spriteSheetMapLint` 命令或 `tools/build.gradle.kts`：

```bash
./scripts/verify-bootstrap.sh
```

如果修复涉及 item data consumption：

```bash
./gradlew :game:test :client:test --tests com.ktome.client.render.EquipmentInventoryPresenterTest --tests com.ktome.client.assets.ManifestResolveTest
```

## Summary

PR-03 已经解决上一轮的大部分阻塞缺口，文档整体接近可开发状态。但当前仍有 3 个会导致实现跑偏的 P1：`ui.shop.offer.frame` 的 sheet/category/alias owner 冲突，Round 7 item-specific keys 未接入当前真实 item data，`spriteSheetMapLint` 仍默认写 PR-00 report。修完这些后，PR-03 才能算真正做到“按文档实施不需要猜测”。

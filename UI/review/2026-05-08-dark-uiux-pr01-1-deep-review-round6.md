# Dark UI/UX PR-01-1 Deep Review Report Round 6

日期：2026-05-08

审查对象：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

审查口径：以 `UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`UI/pr/screen-coverage-matrix.md`、`UI/pr` 后续 PR 文档、当前 `client` 实现与测试为真源，对 PR-01-1 作为长期 UI 改造底座的可执行性做设计总监级 review。

结论：**Conditional Approve after P1/P2 document fixes**。PR-01-1 当前方向正确：它把 viewport、renderer 分层、overlay layer、modal ownership 和 ASCII 删除从 PR-01 主 shell 中拆出，符合后续深色 UI/UX 长期改造的底座定位。当前没有发现 P0 级错误，但有 2 个 P1 必须在实施前修正文档，否则 PR-03/PR-07 会被迫改写 PR-01-1 合同或在实现中引入坐标硬编码 / 第二套 overlay authority。

## Findings

### P1 - Overlay anchor source list is too narrow for downstream PR-03 / PR-07

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:119-130` 把 anchor source 限定为 tile center、ground item、inventory、equipment、engraving、hotbar、talent tree node、quest log row、modal explicit row，并要求新增 source 必须新 PR。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:40-41`、`:97-102`、`:118-123`、`:152-154` 明确要求 shop offer card、sell marker、full-slot replacement modal 和 item tooltip。
- `UI/pr/screen-coverage-matrix.md:31-52`、`UI/pr/dark-uiux-pr07-golden-whitebox-polish.md:154-160` 把 shop、world route、stat assign、reward/frontstage、combat decision、Look/Inspect、settings/validation overlay 都纳入最终 screen coverage。

影响：

当前 PR-01-1 的 anchor source 合同能覆盖地图、背包、装备、热键、天赋、任务和 modal row，但不能稳定承载后续需要 tooltip/selection/active overlay 的 **panel card / list row / option row** 类界面。PR-03 和 PR-07 如果照当前文档落地，要么必须修改 PR-01-1 anchor 合同，要么在各自 PR 内绕过 `TileOverlayLayer` 使用硬编码坐标。这会直接违背“no duplicate overlay authority”和长期 UI 一致性目标。

建议修正：

将 `TileOverlayAnchor` source 从“九个穷举来源”调整为有限但面向长期 UI 面的 typed anchor family，例如：

- `WORLD_TILE`：tile center、ground item、target cursor、inspect cursor。
- `PANEL_SLOT`：inventory slot、equipment slot、engraving slot、hotbar slot、talent node。
- `PANEL_ROW_OR_CARD`：shop offer/sell row、world route option、stat assign row、reward/frontstage card、combat decision row、validation overlay row、settings toggle。
- `QUEST_ROW`：quest log row。
- `MODAL_ROW`：modal explicit row。

同时保留硬约束：anchor 只能来自 layout/presenter 的 typed rect 或 tile coordinate，不能从 mouse hover、raw screen coordinate 或 per-screen tooltip renderer 派生。

### P1 - Manual white-box steps require a tooltip/hover path that the contract forbids

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:203-205` 明确 no mouse hover、no new screens、no PR02/03/04/05 content。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:301` 要求“当前可用交互触发 passive tooltip”并同时打开 item detail modal，验证 modal suppress passive tooltip。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:317` 要求在 inspect mode 中“悬停 viewport 四角实体”验证 tooltip flip。
- 当前 `InputHandler` 已有 keyboard-first `targetingCursor` / `inspectCursor` / modal stack 状态，但不是 mouse-hover interaction path。

影响：

这会让两个关键验收变成不可重复的人工动作：一是 active modal suppress passive tooltip，二是四角 tooltip flip。PR-01-1 自己又声明不引入 mouse hover 和后续内容屏幕，因此文档必须提供可执行的 keyboard/fixture 路径，否则实现者会在 client 中临时制造 hover 入口或跳过白盒证据。

建议修正：

- 将 `10.6` 改成 deterministic fixture 或 focused test：构造同一 `OverlayState` 同时包含 passive anchor 与 top active modal frame，断言只渲染 modal tooltip / modal card。
- 将 `10.13` 的“悬停”改为“通过 inspect cursor / validation fixture 将 focused anchor 移到 viewport 四角实体”。
- 在 `Must Test` 中新增 `TileOverlayLayerTest` 或等价测试用例：`active modal suppresses passive anchored tooltip`、`corner anchors flip inside shell bounds`。

### P2 - ASCII deletion verification does not fully cover the deletion checklist

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:188-199` 要求删除 client ASCII fallback、visual manifest `asciiGlyph/asciiColorHex`、`tileset_foundation_ascii`、`.ascii.*` keys，并覆盖 `assets-src`、`client` manifest、`examples`、`tools` fixtures。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:264-266` 的 `rg` 只覆盖 `client/src`、`assets-src`、`examples` 和一个 game tileset index 文件，未覆盖 `client/src/main/resources`、`tools/src/main/resources`、`game/src/main/resources/data/tilesets` 下所有相关 manifest/fixture。
- 当前仓库用更宽的静态扫描未发现这些 ASCII client fallback 残留，因此这是验证闭环缺口，不是当前代码已经存在泄漏。

影响：

PR-01-1 的删除口径是正确的，但验收命令不能完整证明删除口径。后续 PR 如果重新引入 manifest 字段、fixture key 或工具资源中的 `.ascii.*`，当前命令可能漏报。

建议修正：

把 §9 的 ASCII 检查改成一个覆盖删除清单的 repo-relative 扫描，并在文档中列出允许保留的非 client fallback 例外，例如 legacy test fixture / raw white-box map summary：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii\.' \
  client/src assets-src/image/manifests examples/content-packs tools/src/main/resources \
  game/src/main/resources/data/tilesets
```

### P2 - Test-name enforcement references an owner gate that is not yet documented to enforce it

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:276` 要求 test name convention 必须由 `acceptanceContractLint` 或等价 test-naming lint 验证。
- `docs/verification/README.md` 与 `UI/pr/README.md` 当前把 `acceptanceContractLint` 定义为 PR 文档结构 / acceptance matrix / budget / canonical artifact / failure rule 的轻量 contract lint，不是测试命名 lint。

影响：

如果文档继续声明“必须由 acceptanceContractLint 验证”，但没有同步要求扩展 lint 实现，测试命名会变成伪自动化 gate。后续 review 很难判断缺失测试是测试覆盖不足，还是只是命名没有按文档执行。

建议修正：

二选一：

- 明确 PR-01-1 需要扩展 `acceptanceContractLint` 或新增 `testNamingLint`，并把它列入 §9 verification。
- 或把当前表述降级为 self-audit：canonical manual record 必须列出每个 `Must Test` 对应的测试类/方法名，未自动 lint。

### P2 - ModalFrameKind and UiMode projection needs a stronger contract sentence

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:128` 说 `ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION / ACTIVE_TALENT_SLOT_CHOICE` 只能作为 sealed `ModalFrameKind`，不是新 `UiMode`。
- 当前 `client/src/main/kotlin/com/ktome/client/input/ModalStack.kt` 已有 `ITEM_DETAIL`、`ITEM_COMPARE`、`COMBAT_DECISION`、`ACTIVE_TALENT_SLOT_CHOICE`。
- 当前 `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` 将 `ITEM_DETAIL` / `ITEM_COMPARE` 投影到 `UiMode.INVENTORY`，将 `COMBAT_DECISION` 投影到 `UiMode.TARGETING`，并已有 `INSPECT` / `TARGETING` frame 与 mode 的并存关系。

影响：

文档意图是正确的：不要为每个 modal 创建新的全局 input mode。但当前表述容易被误读为 ModalFrameKind 不能投影到已有 UiMode，或只有列出的 4 种 frame 才受该规则管理。后续 PR 处理 shop replacement、combat decision、talent slot choice 时，容易出现第二套 input ownership。

建议修正：

在 §5.5 增加一句：

> 不新增新 `UiMode`；modal frame 可以继续投影到现有 `UiMode.INVENTORY` / `TARGETING` / `TALENT_ASSIGN` 等 input dispatch surface，但 overlay layer 与 visual z-order 的权威必须来自 `ModalStack.top()` 和 `OverlayRenderFrame`。

### P2 - Current implementation still has map-size-driven viewport support and matching tests

证据：

- `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt` 当前 `FoundationViewportSupport.worldWidth/Height(snapshot)` 仍由 `TileRenderer.worldWidth/Height(snapshot)` 派生，并在 `syncViewport` 中随 snapshot 尺寸更新 viewport world size。
- `client/src/test/kotlin/com/ktome/client/screen/FoundationViewportSupportTest.kt` 当前仍断言 snapshot 尺寸变化会更新 viewport world size。
- PR-01-1 文档要求 fixed shell coordinate、map draw rect 作为 shell 内部区域，不再把 map snapshot size 作为 viewport world size。

影响：

这是实施前的 readiness gap，不是 PR-01-1 文档方向错误。实现时必须同时替换 production path 和现有测试，否则旧测试会把 map-size viewport 合同重新固定下来。

建议修正：

在 PR-01-1 的 implementation notes 中显式列出：

- `FoundationViewportSupportTest` 必须改为固定 shell world size / map rect invariant 测试。
- `TileMapViewportTest` 覆盖 map rect、deadzone、camera clamp、out-of-map fog fill。
- 旧 snapshot-size viewport 更新断言必须删除或改写，不能作为兼容路径保留。

## Requirement Alignment

| Area | Review Result | Notes |
| --- | --- | --- |
| PR execution order | Aligned | PR-01-1 位于 PR-01 shell 和 PR-02 panel 之间，作为 renderer/viewport/overlay 强化层是合理切分。 |
| Art bible direction | Aligned | 文档持续引用 `UI-demo.png`、dark stone / ember / parchment / fixed shell，未引入浅色或 marketing UI 风格。 |
| Core/game boundary | Aligned | 文档明确不改 core/game，不把 rendering/layout 状态写回规则层。 |
| Fixed shell viewport | Aligned in design, not implemented yet | 设计正确；当前代码仍是 map-size viewport，需要 PR-01-1 实现时替换。 |
| Renderer split | Mostly aligned | `TileRenderer` orchestration + map/layer split方向正确；需要通过 tests 证明 SpriteBatch begin/end ownership。 |
| Overlay authority | Partially aligned | layer/modal ownership 正确，但 anchor source list 对后续屏幕不够。 |
| Downstream PR compatibility | Partially aligned | PR-02/03/04/05/07 基本可接，但 shop/world/reward/stat/combat decision anchors 需要前置合同。 |
| ASCII removal | Aligned in intent | 当前静态扫描未见残留；验收命令需扩大覆盖。 |
| Verification governance | Partially aligned | 命令清单基本符合 UI/pr ladder；test-name lint 声明需要落到实际 gate 或降级为 self-audit。 |

## Gameplay / UX Director Review

1. 核心 Roguelike 节奏：PR-01-1 的 map-first、deadzone、fixed shell 和 bottom log rule 能支持 ToME-like 视觉节奏，避免 UI 面板挤压 tactical readability。
2. 战斗可读性：targeting cursor、inspect cursor、active modal 优先级方向正确；但 combat decision / target choice row 必须成为 anchor family 的一等来源，否则战斗决策提示会在后续 PR 中漂移。
3. 物品与构筑体验：右侧 ground/equipment/engraving/backpack 和 bottom hotbar 的长期结构正确；shop offer、full-slot replacement、item compare tooltip 必须共用同一 overlay layer，不能等 PR-03 临时补坐标。
4. 探索与路线选择：out-of-map fog fill、map bounds ratio、camera clamp 是必要底座；world route option / quest row / reward card 也需要 typed row/card anchor，保证 PR-04/PR-07 不新建第二套 tooltip/render path。
5. 信息层级：active modal suppress passive tooltip 是正确 UX 规则，但必须用 deterministic fixture/test 证明，不能依赖“当前可用交互”这种人工描述。
6. 长期可维护性：文档已避免把 ASCII fallback、shape renderer、framebuffer、client-only state 重新引入，是长期 UI 改造的正确方向；剩余主要风险集中在 overlay anchor contract 和 validation closure。

## Current Phase Must Fix Before Implementation

1. 扩展 §5.4 anchor source 合同，至少覆盖 downstream PR-03 / PR-07 的 panel card / list row / option row 类 overlay anchor。
2. 重写 §10.6 与 §10.13 手工白盒步骤，改成 keyboard cursor / focused row / validation fixture 可复现路径。
3. 扩大 §9 ASCII deletion scan，使它覆盖 §6 deletion checklist 的全部资源和 fixture 范围。
4. 明确 test-name enforcement 是新增 lint 还是 manual self-audit，不能让 `acceptanceContractLint` 承担未实现的能力。
5. 在 implementation notes 中点名替换 `FoundationViewportSupportTest` 的旧 snapshot-size viewport 合同。

## Removal / Iteration Plan

PR-01-1 适合按以下顺序实施：

1. 文档先修：anchor family、manual fixture、ASCII scan、test-name lint/self-audit、viewport old-test replacement。
2. 代码再落：fixed shell viewport 和 map rect model，先替换 `FoundationViewportSupport` 的 world-size 语义。
3. Renderer split：提取 map / overlay / frame renderer，确保 `TileRenderer` 只 orchestration，不保存 frame aggregate。
4. Overlay layer：建立 typed anchor model 和 modal priority rule，先覆盖现有 inventory/equipment/talent/inspect/targeting，再为 downstream panel row/card 留出同一 contract。
5. 删除 ASCII fallback：删除 client fallback 后用 broadened `rg` 和 owner tests 证明没有 residual manifest/fixture leakage。

## Additional Suggestions

1. 在 §5.4 增加一张 `TileOverlayAnchorKind` 表，列出每个 kind 的 coordinate authority、适用 PR、是否允许 passive tooltip、是否允许 active modal。
2. 在 canonical manual record 模板中增加“overlay conflict evidence”小节，固定记录 active modal、passive tooltip、corner flip、bottom log non-overlap 的截图 / hash / seed。
3. 在 PR-07 文档中反向引用 PR-01-1 的 anchor family，而不是每个屏幕独立描述 tooltip anchor。
4. 对 renderer split 增加一个 frame ownership self-audit 清单：谁调用 `begin/end`、谁允许 `flush`、谁不能持有 `SpriteBatch`、谁不能创建 `ShapeRenderer/FrameBuffer`。

## Suggested Verification

本轮是文档与代码现状 review，未运行 Gradle。实施 PR-01-1 后建议至少运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint verifyChanged
```

实施中新增/改写的 focused tests 应至少覆盖：

- `TileMapViewportTest`
- `TileLayerComposerTest`
- `TileOverlayLayerTest`
- `FoundationViewportSupportTest`
- `ModalStackTest`
- `InputHandlerModalFrameTest`
- `StatusPresentationModelTest`
- `UiLoadingStateTest`
- `UiErrorPayloadTest`

ASCII 删除建议使用 broadened scan：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii\.' \
  client/src assets-src/image/manifests examples/content-packs tools/src/main/resources \
  game/src/main/resources/data/tilesets
```

## Final Assessment

PR-01-1 可以作为长期 dark UI/UX 改造的底座，但必须先把 overlay anchor 合同从“当前屏幕枚举”升级为“长期 UI surface typed family”，并把手工白盒从 hover/当前可用交互改为 deterministic keyboard/fixture 路径。否则 PR-03 的 shop/item tooltip、PR-04/PR-07 的 route/reward/frontstage/stat/combat decision 验证会持续反向修改底层 overlay 合同，增加 UI 改版过程中的耦合和回归风险。

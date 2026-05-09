# Dark UI/UX PR-02 PR-Level Standard Rereview Round 4

目标文档：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`

审查范围：

- 上游规则：`docs/review/rule/pr-level-review-standard.md`
- 系列入口：`UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`UI/pr/screen-coverage-matrix.md`
- 目标 PR：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`
- 当前实现锚点：`scripts/verify_dark_manifest_coverage.py`、`scripts/verify_sprite_sheet_map.py`、`tools/build.gradle.kts`、`tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt`、`client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt`
- 历史线索：`UI/review/2026-05-09-dark-uiux-pr02-pr-level-standard-rereview-round3.md`

预检摘要：

- 当前工作树包含 PR-02 之外的 `UI/pr/README.md`、PR-03~06、governance 与 screen matrix 改动；本报告只把它们作为 PR-02 的 cross-PR 一致性线索。
- 本轮复审命中 dark UI/UX PR 系列，影响面为 `docs` / `tools` / `assets` / `client`；未发现 `core` / `game` 规则层改动。
- 稳定合同触点：visual manifest、dark-v1 sheet plan、key registry、sprite map report、coverage artifact、white-box/manual evidence。
- 已执行验证见本文 `Executed Validation`。

Review 决策：`request changes`。上一轮的 alias、owner-scope pending/old-style false green、PR-02 sprite map path、manual record path 已明显改善；但 PR-02 的“固定 direct key 清单”仍没有机器级 gate，开发者仍可以少实现一批正式 key 而让当前 owner-scope close 条件看起来通过。这会直接影响 PR-02 是否真的交付 Round 1 chrome/HUD/control 资源闭环。

## Findings

### P0

无。

### P1

#### P1-1 PR-02 expected owner key set 仍未机器冻结，少实现 direct cells 也可能让 close gate 通过

证据：

- PR-02 文档声明 `ownerExpectedKeys` 必须覆盖 §4.2 / §4.3 / §4.4 的 direct cell keys：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:130-134`。
- PR-02 又冻结三张 sheet 的 expected counts：`r01-ui-chrome=15 direct`、`r01-ui-controls=19 direct`、`r01-ui-hud-icons=8 direct`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:199-205`。
- 但当前 coverage script 的 owner-scope 分母只来自 `key-registry.yaml.entries[].ownerPr == args.owner_pr`：`scripts/verify_dark_manifest_coverage.py:88-94`。
- 当前 required sheet gate 只检查 `ownerSheetIds` 是否包含三张 sheet：`scripts/verify_dark_manifest_coverage.py:153-167`。
- 当前 full-grid 校验只检查每个 `row,col` 是否被列出，不检查 direct / reserved / alias count 是否等于 PR-02 表格：`scripts/verify_sprite_sheet_map.py:100-118`。
- PR-02 close 条件要求 `ownerCoveredKeys == ownerExpectedKeys`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:257-266`，但如果开发者只在三张 sheet 各放少量 PR-02 key，`ownerExpectedKeys` 会被错误缩小，等式仍可能成立。

问题：

文档已经把 direct key 表写细了，但机器 gate 仍没有“PR-02 必须包含这 42 个 key”的固定分母。实现者如果漏掉 `ui.screen.error.marker`、`ui.hud.key.icon`、某个 panel edge 或 talent state icon，只要同时漏掉 registry、sheet-plan 和 manifest 中对应 entry，当前 coverage 分母就会随实现缩小。full-grid 只会看到这些槽位被 reserved，不会知道它们本应是 formal direct cells。

影响：

- PR-02 可能在三张 sheet 都存在、owner keys 都 covered、`ownerMissingKeys=[]` 的情况下，实际只交付 Round 1 的子集。
- `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` 如果按 registry 枚举，也会跟着漏掉固定表格中的缺失 key。
- 后续 PR-03/04/05/06 会把 PR-02 chrome/control/HUD 基线当作已完成输入，最终在 player-facing screen 才暴露缺 key。

修复方向：

必须把 PR-02 expected owner key set 变成机器可校验合同，不能只放在 Markdown 表格里。建议二选一：

1. 新增 PR-specific 结构化合同，例如 `UI/sprite-sheets/owner-contracts/pr02-owner-keys.yaml`，列出 `ownerPr`、`requiredSheetIds`、`requiredKeys`、`requiredDirectCountBySheet`、`requiredReservedCountBySheet`。
2. 或给 `darkManifestCoverageLint` / `darkSpriteSheetLint` 增加显式参数：`-Pktome.darkUiux.requiredOwnerKeys=...`、`-Pktome.darkUiux.requiredDirectCounts=r01-ui-chrome=15,r01-ui-controls=19,r01-ui-hud-icons=8`。

PR-02 close gate 应 fail fast：

- `requiredOwnerKeys - ownerExpectedKeys` 非空。
- `ownerExpectedKeys - requiredOwnerKeys` 非空，除非 PR 文档同步新增 key。
- `directCellCountBySheet` 不等于 `15 / 19 / 8`。
- `reservedCellCountBySheet` 不等于 `1 / 45 / 56`。

推荐测试：

- `DarkSpriteSheetPipelineScriptTest.coverageLintFailsOwnerScopeWhenRequiredOwnerKeyIsMissing`
- `DarkSpriteSheetPipelineScriptTest.sheetPlanFullGridFailsWhenDirectCellCountDoesNotMatchPrContract`
- `DarkSpriteSheetPipelineScriptTest.coverageLintReportsOwnerExpectedKeyCountBySheet`

### P2

#### P2-1 Direct cell 表缺少 `category`，实现 registry / sheet-plan 时仍需要推断

证据：

- PR-02 §4.2 表头是 `row / col / targetKey / outputName / fallbackKey / aliasOf / consumerTest`，没有 `category`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:139-156`。
- PR-02 §4.3 / §4.4 表头同样没有 `category`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:160-195`。
- `UI/PLAN.md` 把 `cells[].category` 定义为 sheet-plan 输入合同：`UI/PLAN.md:80-86`。
- `UI/pr/README.md` 的 key registry contract 要求每个 entry 有 `category`：`UI/pr/README.md:118-126`。
- 当前脚本会校验 sheet-plan category 合法性：`scripts/dark_sprite_sheet_contract.py:240-255`，并校验 registry category 与 sheet-plan category 一致：`scripts/verify_dark_key_registry.py:80-95`。

问题：

开发者需要从 sheet 名称、targetKey prefix 或 README 的 Cell Categories 推断 `ui.frame.*` 应写 `ui_frame`、`ui.*.icon` 应写 `icon`。这对 PR-02 当前表格看似简单，但实际会进入 `key-registry.yaml`、`sheet-plan.yaml`、manifest category 和 lint。PR-level 标准要求字段名和 artifact 合同冻结，不应让实现者按前缀猜 category。

影响：

- registry / sheet-plan category 不一致会导致 `darkKeyRegistryLint` 失败。
- manifest category 错误会导致 `spriteSheetMapLint` 或 `manifestLint` 失败。
- 后续新增 screen/control key 时，开发者可能把 `ui.screen.*` marker 写成 `ui_frame` 或 `icon` 之外的临时类别。

修复方向：

把 §4.2 / §4.3 / §4.4 表格统一补 `category` 列：

- `ui.frame.*`：`ui_frame`
- `ui.screen.*.marker`、`ui.control.*.icon`、`ui.combat.*.icon`、`ui.state.*.icon`、`ui.hud.*.icon`：`icon`
- reserved 行写 `N/A`

推荐测试：

- `DarkSpriteSheetPipelineScriptTest.keyRegistryLintFailsWhenPr02CategoryDoesNotMatchSheetPlan`
- 若新增 PR-specific owner key contract，则把 category 一并放入 contract 并校验。

#### P2-2 unused controls cell 同时写成 `reserved / pending cell`，与 Grid Occupancy Contract 的 45 个 reserved 冲突

证据：

- PR-02 §3 写：tab、button、empty state、debug marker、selection marker 若本 PR 不消费，必须写为 `reserved / pending cell`，不进入 PR-02 owner-scope 分母：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:108-114`。
- §4.5 又明确 `r01-ui-controls` 是 `19 direct / 0 alias / 45 reserved / 64 total`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:199-205`。
- §4.3 后文写 `r01-ui-controls` 其余 cell 必须显式 reserved：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:182`。
- `sheet-plan` reserved cell 按脚本规则不能定义 `targetKey / category / outputName`：`scripts/dark_sprite_sheet_contract.py:238-247`。

问题：

`pending cell` 在当前 pipeline 语义里通常意味着非 reserved cell 有 `targetKey`，但 output 或 manifest 仍是 pending/missing。它与 `reserved: true` 完全不同。PR-02 文档同时允许 `reserved / pending cell`，但又把 pending/alias 数量固定为 0；实现者会不知道未消费的 tab/button/debug/selection 到底应该：

1. 写 `reserved: true` 且不进入 registry；
2. 写 pending targetKey 但 owner 归后续 PR；
3. 写 PR-02 owner key 但 coverage 排除。

影响：

- full-grid gate 与 coverage 分母会因为 pending/reserved 选择不同而漂移。
- 后续 PR 可能继承同一个槽位时无法判断它是保留槽、待实现 key，还是 PR-02 漏实现项。

修复方向：

PR-02 应明确使用一种语义。按当前 §4.5 最小改法：

- 把 §3 的 `reserved / pending cell` 改为 `reserved: true cell`。
- 明确 reserved cell 不写 `targetKey / category / outputName / ownerPr`，不进入 key registry、manifest、coverage。
- 如果某个未消费 controls key 必须成为 future pending，则不得计入 PR-02 §4.5 的 45 reserved，需要新增 `futurePendingCells` 表，写明 `targetKey / futureOwner / reason / exclusionFromPr02Coverage`。

推荐测试：

- `DarkSpriteSheetPipelineScriptTest.sheetPlanFullGridRejectsReservedCellWithTargetKey`
- PR-specific owner contract test：`reservedCountBySheet["r01-ui-controls"] == 45`。

#### P2-3 Manual evidence 已有路径，但文档仍保留 PR description-only 逃逸口径

证据：

- Gate Budget 仍写 “PR 描述或 manual record 必须摘录...”：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:28-34`。
- 实施顺序仍写 “PR 描述或 manual record 写明...”：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:99-102`。
- §8 后文又要求 PR 描述和 `UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md` 同步写入：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:279-296`。
- close 条件新增了 `ownerPendingKeys=[]`、`ownerOldStyleKeys=[]`、`allowedOwnerFallbackKeys=[]`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:257-266`，但 Gate Budget 摘录字段仍只提 `ownerSheetIds / ownerExpectedKeys 数量 / ownerCoveredKeys 数量 / ownerMissingKeys / scopeExternalPendingKeys`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:32`。

问题：

同一文档一处允许 PR description-only，另一处要求 manual record 文件。对开发执行来说，这会导致两种合规解释：只写 GitHub PR 文本，或必须落仓库 manual record。PR-level 标准要求 manual evidence 有 repo-relative artifact 和字段记录规则，不能让 GitHub 文本成为唯一证据来源。

影响：

- outcome/error/loading skip reason 可能只存在 PR 描述，PR-07 evidence index 无法从仓库机械读取。
- coverage close 的关键字段 `ownerPendingKeys / ownerOldStyleKeys / allowedOwnerFallbackKeys` 可能没有被摘录，reviewer 只能手动打开 JSON。

修复方向：

统一口径：

- `UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md` 是 canonical manual evidence。
- PR 描述只摘录该 manual record 和 coverage artifact 的关键摘要。
- Gate Budget 摘录字段补齐：`requiredOwnerSheetIds`、`ownerExpectedKeys` 完整数量或 hash、`ownerCoveredKeys` 完整数量或 hash、`ownerMissingKeys`、`ownerPendingKeys`、`ownerOldStyleKeys`、`allowedOwnerFallbackKeys`、`scopeExternalPendingKeys`。
- 把所有 “PR 描述或 manual record” 改为 “manual record 必填，PR 描述引用/摘录”。

推荐测试：

- `acceptanceContractLint` 增加检查：PR-02 有 canonical manual record path，且不能出现 “PR 描述或 manual record” 作为 skip/evidence 口径。

#### P2-4 `StandaloneScreenChrome` 的资源消费 API 未冻结，实现者仍需决定 resolver/texture repository 如何接入

证据：

- PR-02 只要求 `StandaloneScreenChrome` 必须直接消费 `ui.frame.panel.body`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:209-217`。
- 当前 `StandaloneScreenChrome` 构造不接收 `VisualManifestResolver` 或 `ClientTextureRepository`，`draw` 只接收 `SpriteBatch` 和 `StandaloneChromeRequest`：`client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt:46-52`。
- 当前 chrome 通过 `whitePixel` + `drawPanel/drawRect` 画色块和边框：`client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt:70-101`。
- PR-02 focused test 要求用 recording canvas、test texture repository 或 resolver spy 断言 resolved key 被绘制：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:213-216`，但文档没有指定 standalone screen 对应的 test seam 或 draw abstraction。

问题：

`TileRenderer` 已有 `TileCanvas.drawAsset` seam，但 `StandaloneScreenChrome` 没有等价抽象。开发者实现时必须自行选择：

1. 给 `StandaloneScreenChrome` 构造函数加 `VisualManifestResolver` 和 `ClientTextureRepository`；
2. 在 `StandaloneChromeRequest` 里传 resolved asset；
3. 新增 standalone screen canvas/renderer adapter；
4. 直接在每个 screen 里解析并 draw。

这些选择会影响测试注入、resource lifecycle、texture ownership 和重复代码。当前 PR 文档没有冻结推荐方案，容易出现多个 screen 各自解析资源或测试只能靠 golden/hash 的实现。

影响：

- `StandaloneScreenLayoutTest.darkUiuxPr02StandaloneChromeConsumesManifestKeys` 的实现方式不唯一。
- 资源 lifecycle 如果放错层，可能引入 texture 重复创建或 dispose ownership 不清。
- 后续 Victory/GameOver/Validation/Error screen 复用 chrome 时可能各自维护一份资源解析路径。

修复方向：

在 §4.6 增加 standalone chrome API contract，至少冻结以下之一：

- 推荐：新增 `StandaloneChromeCanvas` / recording canvas，`StandaloneScreenChrome.draw(canvas, request)` 内部通过传入的 resolved assets 绘制；screen owner 负责从 `VisualManifestResolver + ClientTextureRepository` 解析一次并构建 request。
- 或：`StandaloneScreenChrome(visualResolver: VisualManifestResolver, textureRepository: ClientTextureRepository)`，并声明 `dispose()` ownership、fallback behavior、test spy 注入方式。

同时列出具体改动文件：

- `client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt`
- `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt`
- `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupScreen.kt`
- `client/src/main/kotlin/com/ktome/client/screen/VictoryScreen.kt`
- `client/src/main/kotlin/com/ktome/client/screen/GameOverScreen.kt`
- `client/src/main/kotlin/com/ktome/client/screen/UiErrorScreen.kt`

推荐测试：

- `StandaloneScreenLayoutTest.darkUiuxPr02StandaloneChromeConsumesManifestKeys`
- `StandaloneScreenLayoutTest.darkUiuxPr02ChromeUsesFallbackAssetWhenPanelBodyMissing`

#### P2-5 README 仍写 `screen frame alias for ui.screen.*`，与 PR-02 “不新增 alias key” 冲突

证据：

- `UI/pr/README.md` 的 SheetId Ownership 仍写 `r01-ui-chrome` expected scope 包含 `screen frame alias for ui.screen.*`：`UI/pr/README.md:59-63`。
- PR-02 明确写 “PR-02 不新增 registry-only alias key。Standalone screen chrome 直接消费 `ui.frame.panel.body`”：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:199-208`。

问题：

开发者先读 README，再读 PR-02 时会看到相反信号：README 暗示 PR-02 的 screen frame alias 是 sheet ownership 范围，PR-02 又禁止本 PR 新增 alias key。上一轮已经把 PR-02 内部 alias 冲突修掉，但系列入口仍保留旧口径。

影响：

- 后续实现可能重新引入 `ui.screen.home.*` registry-only alias，触发 `darkKeyRegistryLint` 冲突。
- PR-03/04/05 读 README 时可能误以为 PR-02 已交付 `ui.screen.*` frame key。

修复方向：

更新 README 对 `r01-ui-chrome` 的 Expected Key Prefix / Scope：

- 当前 PR-02：`ui.frame.*`; standalone screens consume shared `ui.frame.panel.body` directly.
- future exact `ui.screen.*` frame keys require a later PR to add direct cells / manifest entries / tests.

推荐测试：

- `acceptanceContractLint` 增加跨文档 smoke：PR-02 声明 no alias 时，README 不应仍把 `ui.screen.* alias` 写成当前 scope。

### P3

#### P3-1 `ownerExpectedKeys` 文案写“以下三类”，但实际只有两条

证据：

- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:130-134` 写 `ownerExpectedKeys` 必须覆盖以下三类，但下面只列了两条。

影响：

这是低风险文案问题，但会让 reviewer 误以为漏了一类 owner key 规则。

修复方向：

改为“以下两类”，或补第三类，例如“PR-specific required owner key contract 中列出的 required keys”。

#### P3-2 Acceptance Matrix 的 `UI02-M01` 仍写 bare resource task，建议补 PR-specific 参数

证据：

- Acceptance Matrix `UI02-M01` fastCheck 只写 `darkSpriteSheetLint`, `spriteSheetMapLint`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:15-20`。
- §7 的真正 PR-02 命令需要 `-Pktome.darkUiux.requireFullGrid=true` 和 `-Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:244-250`。

影响：

后文已经给出精确命令，所以不算阻塞。但 Acceptance Matrix 是开发者和 reviewer 的第一索引，最好直接呈现 PR-specific task 参数，减少 bare task 被误用。

修复方向：

把 `UI02-M01` fastCheck 改成：

- `darkSpriteSheetLint -Pktome.darkUiux.requireFullGrid=true`
- `spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl`

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR-02 依赖 PR-00 / PR-01 / PR-01-1 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:3-7`, `UI/pr/README.md:19-35` | 一致 |
| 上一轮 alias 冲突必须消除 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:199-208`, `scripts/verify_dark_key_registry.py:69-107` | 部分一致；PR-02 内部已改为 no alias，但 README 仍有旧 alias 表述 |
| owner-scope 必须 fail pending / old-style / empty owner | `scripts/verify_dark_manifest_coverage.py:95-131`, `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt:622-860` | 一致 |
| PR-02 必须交付三张 Round 1 sheet 的完整 direct cell set | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:137-205`, `scripts/verify_dark_manifest_coverage.py:88-94`, `scripts/verify_sprite_sheet_map.py:100-118` | 部分一致；文档列出了 direct keys，但 gate 未固定 required owner key set/count |
| PR-02 sprite map report path 必须 repo-relative 且 PR-specific | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:50`, `tools/build.gradle.kts:541-573` | 一致 |
| manual evidence 必须可追溯 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:279-296` | 部分一致；路径和字段已补，但上文仍保留 PR description-only 口径 |
| client focused evidence 必须能证明 drawAsset 消费 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:209-217`, `client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt:46-101` | 部分一致；TileRenderer 路径清楚，StandaloneScreenChrome API seam 未冻结 |

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| `tools` manifest coverage | PR-02 owner-scope 必须 fail empty / missing / pending / old-style | 已实现 | `scripts/verify_dark_manifest_coverage.py:95-131`; tools tests `:622-860` | 上一轮 false green 已解决 | N/A |
| `tools` required key inventory | PR-02 must include all §4.2-§4.4 direct keys | 部分实现 | PR doc `:130-205`; coverage script `:88-94` | 分母仍来自 registry 当前内容，未固定 42 key set | P1 |
| `assets` sheet-plan | 三张 sheet full grid，direct/reserved 数量固定 | 部分实现 | PR doc `:199-205`; sprite script `:100-118` | full-grid 只校验 missing slots，不校验 direct/reserved count | P1 |
| `assets` / registry category | 每个 cell / registry entry 必须有 category | 文档缺口 | PR doc `:139-195`; README `:118-126`; scripts `dark_sprite_sheet_contract.py:240-255` | PR-02 direct table 没有 category 列 | P2 |
| `client` standalone chrome | standalone screens 消费 shared chrome asset | 部分冻结 | PR doc `:209-217`; current class `StandaloneScreenLayout.kt:46-101` | 资源解析 API/test seam 未冻结 | P2 |
| `docs` cross-PR | README 与 PR-02 owner scope 一致 | 部分一致 | README `:59-63`; PR-02 `:199-208` | README 仍保留 screen alias 口径 | P2 |
| `docs` manual evidence | manual record 是 canonical evidence | 部分一致 | PR doc `:28-34`, `:279-296` | 上文仍写 PR 描述或 manual record | P2 |

## 玩法与体验审查

PR-02 直接影响玩家能否识别 UI 层级、HUD 资源、modal/slot 状态和 standalone screen 的正式 chrome。当前文档方向正确，特别是禁止继续只画 token 色块。但如果 P1-1 不修，PR-02 可能只交付少量 key，仍让 coverage 和 focused tests 看起来成立。玩家实际看到的结果会是：某些 HUD/control/marker 仍缺图或回旧 fallback，但 reviewer 以为 Round 1 已完整闭环。

当前体验风险集中在两个点：

1. 完整 key set 不机检，导致“看起来有三张 sheet”不等于“Round 1 controls/HUD/chrome 已完整交付”。
2. StandaloneScreenChrome API 未冻结，可能让首页、validation、outcome、error screen 各自走不同资源解析路径，后续 PR-07 很难统一证据。

## 当前阶段必须解决的问题

合并或进入正式实现前必须修：

1. P1-1：把 PR-02 fixed owner key set 和 direct/reserved counts 变成机器 gate，而不是只写在 Markdown 表格里。

PR-02 close 前必须修：

1. P2-1：给 direct cell 表补 `category`。
2. P2-2：把 unused controls 的 reserved / pending 口径统一。
3. P2-3：把 manual record 改为 canonical evidence，PR 描述只做摘要。
4. P2-4：冻结 `StandaloneScreenChrome` 的 resolver / texture / recording test seam。
5. P2-5：同步 README 的 `r01-ui-chrome` alias scope。

可同轮文档修正：

1. P3-1：修正“两类/三类”文案。
2. P3-2：Acceptance Matrix 补 PR-specific resource gate 参数。

## Removal/Iteration Plan

- PR-02 不新增 registry-only alias；如果未来需要 `ui.screen.home.*` exact key，必须作为 direct cell 新增，而不是恢复 registry-only alias。
- `missing_visual` 可以作为 fallbackKey 元数据，但 PR-02 close artifact 中 `ownerPendingKeys` 和 `allowedOwnerFallbackKeys` 必须为空。
- `dark-v1-pr00-sprite-map-report.jsonl` 保持 PR-00 dry-run 语义；PR-02 正式资源必须写 `assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl`。

## Additional Suggestions

1. Coverage report 可以增加 `ownerExpectedKeyCountBySheet`、`ownerCoveredKeyCountBySheet`、`requiredOwnerKeySetHash`，让 PR 描述不用手工列 42 个 key。
2. `acceptanceContractLint` 可以针对 `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md` 做 lightweight doc lint：禁止 `reserved / pending` 混写，禁止 required cell table 缺 `category`。
3. `StandaloneScreenChrome` 的 recording test 不建议用 SpriteBatch/headless texture 作为第一层断言；先抽一个小 canvas/request seam，再由 screen smoke/golden 证明真实渲染。

## Open Questions

1. PR-02 fixed key set 要放在哪里作为机器真源：PR-specific YAML、Gradle property，还是由 `sheet-plan.yaml` 增加 `contractOwnerPr / requiredDirectCount` 字段？需要选一个，避免再引入第二份长期真相。
2. `StandaloneScreenChrome` 是直接持有 resolver/repository，还是由各 screen 解析成 request asset 后传入？两者都可行，但 PR-02 文档必须固定一条。

## Suggested Verification

修正后建议执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest --rerun-tasks
./gradlew syncPhase2Manifests manifestLint
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint -Pktome.darkUiux.requireFullGrid=true -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.screen.StandaloneScreenLayoutTest
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew verifyChanged
```

新增 P1-1 对应的额外验收：

- 刻意删除一个 PR-02 required key 时，coverage 或 sheet-plan lint 必须 fail。
- 把一个 required direct cell 写成 reserved 时，full-grid/direct-count gate 必须 fail。
- PR-02 成功时，coverage artifact 必须能机械证明 required key set 全覆盖。

## Executed Validation

已执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint
```

结果：PASS。

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest --rerun-tasks
```

结果：PASS。该测试覆盖 owner-scope empty owner、required sheet id、pending owner、old-style owner、positive owner-scope dark output、full-grid missing slot 等工具层行为。

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-00 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome
```

结果：按预期 FAIL，报 `owner-scope pending keys` 与 `owner-scope old-style keys`，说明上一轮 owner-scope false green 已修。

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons
```

结果：按当前 PR-02 资源尚未实现状态 FAIL，报 `owner-scope coverage found no expected keys for PR-02` 与缺三张 required sheet。该失败符合当前开发阶段预期。

## Not Executed / Residual Risk

未执行 `:client:clientSmoke`、`:client:goldenScreenshot`、`verifyChanged`。原因：当前请求是 PR-02 文档复审，且 PR-02 formal resources / client consumption 尚未实现；这些 gate 当前不能证明 PR-02 player-facing outcome。

剩余风险：

- 当前 `acceptanceContractLint` 仍能通过本报告指出的 P1/P2 文档缺口，说明它只能作为结构快检，不足以替代本轮 PR-level review。
- P1-1 修复后必须重新跑 tools test，并增加故意漏 required key / wrong direct count 的负例。

## Summary

本轮相较 Round 3 有实质进展：alias 冲突已基本移除，owner-scope pending/old-style false green 已通过脚本和 tests 修复，PR-02 sprite map report 与 manual record path 也补到了文档和 Gradle 接线里。

仍然阻塞的是更细的可实施性：PR-02 的 42 个 fixed owner keys 和 direct/reserved counts 还没有被机器 gate 固定。只要这个分母仍来自当前 registry 内容，开发者漏掉 direct cell 时 gate 就可能跟着缩小分母。修掉 P1-1 后，再补 category、reserved/pending、manual evidence、StandaloneScreenChrome API 和 README alias 口径，PR-02 才能接近“真正不需要猜测即可开发”的执行文档。

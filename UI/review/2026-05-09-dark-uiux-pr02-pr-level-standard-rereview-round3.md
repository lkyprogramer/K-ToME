# Dark UI/UX PR-02 PR-Level Standard Rereview Round 3

目标文档：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`

审查范围：

- 上游规则：`docs/review/rule/pr-level-review-standard.md`
- 目标 PR：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`
- 当前代码锚点：`scripts/verify_dark_manifest_coverage.py`、`scripts/verify_dark_key_registry.py`、`scripts/verify_sprite_sheet_map.py`、`tools/build.gradle.kts`
- 历史线索：`UI/review/2026-05-09-dark-uiux-pr02-pr-level-standard-rereview-round2.md`
- 本轮重点：按 PR 级 review 标准重新核查“开发者是否可以不猜字段、状态、owner、测试、gate、artifact、fallback 或验收口径直接实施”

Review 决策：`request changes`。新版已经解决了上一轮“owner-scope 必须绑定三张 sheet”的大部分问题，但仍存在两个 P1：alias 合同与当前 lint 不兼容、owner-scope coverage 仍可 false green。当前文档还不能作为无猜测开发执行文档。

## Findings

### P0

无。

### P1

#### P1-1 Alias Registry Entries 与现有 key-registry/sheet-plan lint 合同冲突，按文档实现会无法通过 gate

证据：

- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:92` 要求 `sheet-plan.yaml` 新增逐 cell `targetKey / outputName / aliasOf`。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:197-204` 又把 `ui.screen.home.title_frame`、`ui.screen.home.action_frame` 定义为 `Alias Registry Entries`，只给了 `targetKey / sheetId / fallbackKey / aliasOf / Consumer / consumerTest`，没有 `row / col / outputName / category`。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:140-154` 已经把 `r01-ui-chrome` 4x4 网格的 16 个槽位占满为 15 个 direct cell + 1 个 reserved cell，没有给两个 alias cell 留槽。
- `scripts/verify_dark_key_registry.py:69-74` 明确禁止 key registry 中存在 sheet-plan 里没有的 targetKey。
- `scripts/dark_sprite_sheet_contract.py:282-286` 要求 aliasOf 目标存在于 sheet plan，且 alias cell 的 `outputName` 必须等于 alias target 的 `outputName`。

问题：

当前文字同时暗示两套互斥实现：

1. 如果开发者把 §4.5 当“registry-only alias”实现，`darkKeyRegistryLint` 会报 `key registry targetKeys absent from sheet plan`。
2. 如果开发者把 §4.5 当“sheet-plan alias cell”实现，文档没有 row/col/outputName/category，而且 `r01-ui-chrome` 没有足够空槽，开发者只能擅自挪动 direct cell、扩表或删除 reserved cell。

这不是实现细节缺失，而是 source of truth 和 gate 合同不一致。不同实现都可能声称符合文档，但只有部分能过当前工具，甚至可能完全无法过。

影响：

- `darkKeyRegistryLint`、`darkSpriteSheetLint`、`spriteSheetMapLint` 会在 PR-02 实现阶段直接阻断。
- `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries` 无法确定 alias key 应该来自独立 manifest entry、同 rawOutputPath entry，还是直接让 client 消费 `ui.frame.panel.body`。
- 首页 title/action chrome 的验收会漂移：可能是 exact key resolved，也可能只是 fallback/alias resolved。

修复方向：

在 PR 文档里选择并冻结唯一 alias materialization 合同：

1. 推荐收敛方案：如果不需要独立 `ui.screen.home.*` manifest key，删除 §4.5 两个 alias owner key，`StandaloneScreenChrome` 直接消费 `ui.frame.panel.body`，并把 §4.6 / §8 的断言改为 `ui.frame.panel.body` exact resolve。
2. 若必须保留 `ui.screen.home.*` exact key，则把 §4.5 改成真实 sheet-plan cells：补 `row / col / category / outputName`，调整 `r01-ui-chrome` 网格或 direct cell 分配，并明确 alias cell `outputName` 必须等于 `ui.frame.panel.body` 的 outputName。
3. 若想支持 registry-only alias，则必须同 PR 修改工具合同：`darkKeyRegistryLint` 允许 `aliasOf` registry-only entry，manifest/sync 必须 materialize exact alias entry，coverage 只在 alias exact entry 和 alias target 同为 `dark-v1/ui/*` 时计入 covered，并补工具测试。

推荐测试：

- `DarkKeyRegistryScriptTest.registryOnlyAliasRequiresManifestExactEntry`
- `DarkSpriteSheetPipelineScriptTest.aliasCellOutputNameMustMatchAliasTarget`
- `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`

#### P1-2 owner-scope coverage 仍可在没有任何 ownerCoveredKeys 的情况下 PASS，不能证明 PR-02 已落到 dark-v1 正式资源

证据：

- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:251-258` 的 close 条件只要求 `ownerExpectedKeys` 非空、三张 `ownerSheetIds` 存在、`ownerMissingKeys=[]` 和 `requiredOwnerSheetIds` 匹配，没有要求 `ownerCoveredKeys == ownerExpectedKeys`，也没有要求 `allowedOwnerFallbackKeys=[]` 或 old-style 为空。
- `scripts/verify_dark_manifest_coverage.py:103-124` 已经计算了 `pending_keys`、`old_style_keys`、`covered_keys`。
- `scripts/verify_dark_manifest_coverage.py:126-127` 在 owner-scope 下只把 `missing_keys` 作为 error。
- `scripts/verify_dark_manifest_coverage.py:149-177` 把 `ownerCoveredKeys` 和 `allowedOwnerFallbackKeys` 写入报告，但不会因为 owner keys 仍是 pending / old-style 而失败。
- 本轮探针命令已验证 false green 风险：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=owner-scope \
  -Pktome.darkUiux.ownerPr=PR-00 \
  -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome
```

结果：`BUILD SUCCESSFUL`，报告 `status=PASS`，但 `ownerCoveredKeys=[]`，`allowedOwnerFallbackKeys=["ui.frame.panel.body","ui.frame.panel.focus"]`。这个 PR-00 只是用现有 dry-run owner 复现 owner-scope 的非空路径；同一逻辑会让 PR-02 在 key 存在但资源仍 pending / old-style 时 false green。

问题：

PR-02 的目标是首次提交正式 dark UI chrome / HUD / standalone screen chrome sheet。如果 owner-scope 只证明 key 没缺失，而不证明 canonical/runtime manifest 都指向 `dark-v1/ui/*`，开发者可以在 `ownerMissingKeys=[]` 的情况下继续把正式 surface 指向 `debug/missing_visual.png` 或旧 `phase4/*` output。报告字段看起来完整，但没有证明玩家会看到新 dark UI 资源。

影响：

- `darkManifestCoverageLint` 不能作为 PR-02 close gate。
- `UI02-M03` 会误导 reviewer 只检查 key 缺失，而忽略资源是否真正 covered。
- golden 或 manual evidence 可能成为唯一发现旧视觉的入口，违反 PR-level 标准中 blocking 行为必须有 owner gate/artifact 的要求。

修复方向：

把 PR-02 close gate 改为强断言正式 covered：

1. `ownerCoveredKeys` 必须与 `ownerExpectedKeys` 完全相等，或至少新增 `ownerUncoveredKeys=[]` 并 fail fast。
2. `allowedOwnerFallbackKeys=[]`；如保留字段，重命名为 `ownerPendingKeys`，PR-02 close 必须为空。
3. 新增 `ownerOldStyleKeys=[]` 字段，owner-scope 下也要 fail fast。
4. 对 PR-02 owner key 同时检查 canonical manifest 和 runtime manifest 的 `rawOutputPath` 前缀均为 `dark-v1/ui/`。
5. `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:251-258` 必须补上述字段作为 close 条件。

推荐测试：

- `DarkSpriteSheetPipelineScriptTest.coverageLintFailsOwnerScopeWhenOwnerKeysArePending`
- `DarkSpriteSheetPipelineScriptTest.coverageLintFailsOwnerScopeWhenOwnerKeysUseOldStyleOutput`
- `DarkSpriteSheetPipelineScriptTest.coverageLintPassesOwnerScopeWhenRequiredSheetsHaveOnlyDarkOutputs`

### P2

#### P2-1 reserved cell 要求没有 exact grid occupancy 合同和 gate，开发者仍可遗漏大量空槽

证据：

- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:92`、`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:111` 要求所有未列为 formal key 的 cell 显式 `reserved: true`。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:180` 要求 `r01-ui-controls` 其余 cell reserved。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:195` 要求 `r01-ui-hud-icons` 其余 cell reserved。
- `scripts/dark_sprite_sheet_contract.py:238-270` 只验证已列出的 cell，不要求每张 sheet 的 `row*columns` 槽位全部出现在 plan 中。
- `scripts/verify_sprite_sheet_map.py:105`、`scripts/verify_sprite_sheet_map.py:128` 只遍历 `cells_by_sheet` 中已列出的 cell。

问题：

文档要求“不能留空让实现者猜”，但没有冻结每张 sheet 的 expected total/direct/reserved count，也没有 gate 证明所有 grid slot 都被 direct 或 reserved 覆盖。开发者可以只写 §4 表格中的 direct cell，不写 45 个 controls reserved cell 和 56 个 HUD reserved cell，当前 lint 仍可能只验证已列出的 cell。

影响：

- 后续 PR 可能把同一空槽理解成 pending、reserved 或可复用槽，导致 sheet-plan 漂移。
- contact sheet QA 无法证明整张 sheet 没有遗漏、串格或隐式空槽。
- PR-02 之后的 PR-03/PR-06 会继承不完整资源地图。

修复方向：

在 §3 或 §4 补 exact occupancy 表，并让 `darkSpriteSheetLint` fail fast：

| sheetId | grid | direct cells | alias cells | reserved cells | total slots |
| --- | ---: | ---: | ---: | ---: | ---: |
| `r01-ui-chrome` | `4x4` | 以修正后的 §4.2/§4.5 为准 | 以 alias 决策为准 | 补精确数 | `16` |
| `r01-ui-controls` | `8x8` | `19` | `0` | `45` | `64` |
| `r01-ui-hud-icons` | `8x8` | `8` | `0` | `56` | `64` |

并补工具行为：每张 sheet 必须满足 `listedSlots == rows * columns`，缺槽或重复槽都 fail；报告输出 `directCellCountBySheet`、`aliasCellCountBySheet`、`reservedCellCountBySheet`。

推荐测试：

- `DarkSpriteSheetPipelineScriptTest.sheetPlanFailsWhenAnyGridSlotIsOmitted`
- `DarkSpriteSheetPipelineScriptTest.sheetPlanReportsReservedCellCountBySheet`

#### P2-2 sprite map report 被 PR-02 当作证据，但当前 artifact path 仍是 PR-00 hard-coded

证据：

- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:30` 要求 `sprite map report` 与 raw sheet、manifest、coverage、golden 来自同一批资源。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:269` 把 `sprite map report` 列为 contact sheet QA artifact。
- `scripts/verify_sprite_sheet_map.py:28` 默认 report 是 `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`。
- `tools/build.gradle.kts:557` 的 `spriteSheetMapLint` output 也固定为 `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`。

问题：

PR-02 文档要求使用 sprite map report 作为证据，但没有冻结 PR-02 自己的 repo-relative report path；当前默认任务还会把所有 PR 的 map report 写到 `dark-v1-pr00-sprite-map-report.jsonl`。开发者要么污染 PR-00 dry-run 证据，要么自行猜一个 `--report` 参数，但 Gradle task/output tracking 不知道这个新路径。

影响：

- PR-02 的 raw hash、cellHash、outputHash 证据会被归档到错误 PR 名称下。
- `spriteSheetMapLint` 的 Gradle output 与 PR 文档证据不一致，incremental/CI 审计时很难机械追溯。
- 后续 PR 不能区分 PR-00 dry-run map 与 PR-02 formal map。

修复方向：

冻结 PR-02 artifact path，例如：

- `assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl`

同时二选一：

1. 新增 PR-specific Gradle task：`darkUiuxPr02SpriteSheetMapLint`，传 `--report assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl`，并把 outputs.file 指向同一路径。
2. 或给 `spriteSheetMapLint` 增加 `-Pktome.darkUiux.spriteMapReportName=dark-v1-pr02-sprite-map-report.jsonl`，PR-02 close 命令必须显式传入。

推荐测试：

- `DarkSpriteSheetPipelineScriptTest.spriteMapReportUsesConfiguredPrSpecificPath`
- Gradle task wiring test 或最小脚本测试覆盖 `--report` 自定义路径。

#### P2-3 manual record / skip evidence 没有固定文件路径和字段，outcome/error/loading 的继承证据不可机械追溯

证据：

- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:99` 写的是“PR 描述或 manual record”。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:214` 要求 outcome/error/loading 如果不进入 golden，manual record 必须列 skip reason、替代 resolver evidence 和 PR-07 evidence index owner label。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:271` 又要求 contact sheet QA、manifest diff、coverage artifact、golden/manual label 写入 PR 描述。
- 当前仓库已有 UI manual record 目录和先例：`UI/manual-records/dark-uiux-pr01-shell.md`，但 PR-02 文档没有指定对应文件。

问题：

PR-level 标准要求 manual evidence 有 repo-relative artifact 和字段记录规则。当前 “PR 描述或 manual record” 会让证据只存在 GitHub PR 文本中；本地开发、复审、后续 PR-07 继承时无法通过仓库文件机械追溯 skip reason 和替代证据。

影响：

- outcome/error/loading 未截图时，reviewer 只能人工读 PR 描述，无法从仓库证据判断 skip 是否合规。
- PR-07 polish/golden 继承项没有稳定 input。
- 如果 PR 描述后续被改写，仓库内无法复现当时的 manual evidence。

修复方向：

在 §2.1 / §8 补固定 manual record path 和字段：

- `UI/manual-records/dark-uiux-pr02-ui-chrome-sprite-pilot.md`

字段建议：

- `promptIndexPath`
- `rawSheetPaths`
- `rawSheetHashes`
- `contactSheetPaths`
- `spriteMapReportPath`
- `canonicalManifestDiff`
- `runtimeManifestDiff`
- `coverageReportPath`
- `ownerExpectedKeys`
- `ownerCoveredKeys`
- `ownerMissingKeys`
- `ownerPendingKeys`
- `ownerOldStyleKeys`
- `skippedScreens`：`screen / reason / replacementResolverEvidence / pr07OwnerLabel / residualRisk`
- `goldenLabels`
- `manualReviewer`
- `reviewedAt`

推荐测试：

- `acceptanceContractLint` 或专用 doc lint 检查 PR-02 manual record path 出现在 Acceptance Matrix / 白盒证据中。

### P3

#### P3-1 UI02-M04 的 fastCheck 仍是概括描述，建议换成 exact test list

证据：

- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:20` 的 `UI02-M04` fastCheck 写的是 `client asset focused tests`。
- 同文档 §4.6 / §8 已经给出了更具体的测试入口。

问题：

这不阻塞理解，但 Acceptance Matrix 是开发者最先查的 gate 表。`client asset focused tests` 仍需要跳到后文推断 exact tests。

修复方向：

把 `UI02-M04` fastCheck 改成：

- `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`
- `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets`
- `StandaloneScreenLayoutTest.darkUiuxPr02StandaloneChromeConsumesManifestKeys`

推荐测试：

- N/A，文档修正即可。

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR-02 依赖 PR-01-1 合并后才能实现 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:6-7`, `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:90` | 通过 |
| 三张 owner sheet 必须进入 PR-02 owner-scope | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:60-63`, `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:246-258` | 部分通过；required sheet ids 已补，但 covered 语义仍不阻断 pending/old-style |
| key registry / sheet-plan / manifest 单一权威 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:91-97`, `scripts/verify_dark_key_registry.py:69-74` | 不通过；alias-only 合同与 lint 冲突 |
| Round 1 sheet 逐 cell 可实施 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:140-204` | 部分通过；direct cell 详细，但 alias 和 reserved grid closure 不可机械实施 |
| 白盒证据可追溯 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:262-271` | 部分通过；scenario/expected 已写，但 manual record path/fields 缺失 |
| artifact repo-relative | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:38-50`, `tools/build.gradle.kts:557` | 部分通过；coverage path 已固定，sprite map report 仍 PR-00 hard-coded |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前代码/文档状态 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |
| `assets` sheet-plan | 三张 sheet 全部 formal/reserved，alias 可追溯 | direct cell 表较完整，但 alias 没有可执行 row/col/outputName，reserved count 未冻结 | 开发者需要猜 sheet occupancy | P1/P2 |
| `tools` key registry lint | key registry 与 sheet-plan 单一权威 | 当前 lint 禁止 registry-only key | §4.5 alias-only 写法不可通过 | P1 |
| `tools` coverage lint | owner-scope close 证明 PR owner keys 正式 covered | 当前 owner-scope 只 fail missing key，不 fail pending/old-style | coverage false green | P1 |
| `tools` sprite map report | PR-02 map report 是 canonical evidence | 默认 report path 仍是 `dark-v1-pr00-sprite-map-report.jsonl` | PR-02 证据路径不唯一 | P2 |
| `client` focused tests | recording canvas / resolver spy 证明 resolved key 被绘制 | 文档后文有测试名，Acceptance Matrix 仍概括 | 索引性不足 | P3 |
| `docs` manual evidence | skip reason、替代证据、PR-07 label 可追溯 | 只写 PR 描述或 manual record，没有固定文件和字段 | 后续 PR 无法机械继承 | P2 |

## 玩法与体验审查

PR-02 是 player-facing surface：panel、slot、modal、HUD icon、standalone screen chrome 都会直接影响玩家对战斗/背包/首页/错误状态的识别。文档的体验目标方向正确，尤其是禁止只用 `drawRect` / `whitePixel` 作为目标主视觉。但 P1-2 会让体验验收失真：玩家仍可能看到 missing visual 或旧风格资源，而 owner-scope gate 显示 PASS。必须先让 coverage gate 证明 canonical/runtime manifest 都进入 dark-v1 output，再让 golden/manual evidence 判断视觉质量。

## 当前阶段必须解决的问题

合并或进入正式实现前必须修：

1. P1-1：确定 alias materialization 合同，并同步文档和工具/测试。
2. P1-2：让 owner-scope coverage fail pending/old-style owner keys，并补 close 条件。

PR-02 close 前必须修：

1. P2-1：冻结 reserved cell count 和 full-grid occupancy gate。
2. P2-2：冻结 PR-02 sprite map report path，并让 Gradle task 输出同一路径。
3. P2-3：新增 PR-02 manual record path 和字段。

可在同轮文档修正中顺手完成：

1. P3-1：Acceptance Matrix 使用 exact tests。

## Removal/Iteration Plan

- 本轮未发现新增 Deleted 文件合同。
- `missing_visual` 只能作为 PR-02 early fallback；PR-02 close gate 不应把 owner key 的 pending/missing visual 视作 covered。PR-06 final-full 再负责玩家主路径旧 fallback 的全量替换。
- `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl` 应保留 PR-00 dry-run 语义，不应继续作为 PR-02 formal artifact。PR-02 需要独立 report path 或显式 reportName 参数。
- 如果 §4.5 alias 改成 registry-only alias，则后续 removal/iteration plan 还必须声明 alias entry 的删除/迁移 owner；否则后续 PR 会误以为 `ui.screen.home.*` 是独立 sprite cell。

## Additional Suggestions

1. 在 coverage report 中新增 `ownerCoveredKeyCount`、`ownerExpectedKeyCount`、`ownerPendingKeys`、`ownerOldStyleKeys`，减少 PR 描述手工统计。
2. 在 `DarkSpriteSheetPipelineScriptTest` 中补 positive path：三张 required sheets 都存在且所有 owner keys 都是 `dark-v1/ui/*` 时 PASS，防止后续修 P1-2 时把合法 PR-02 误杀。
3. 给 `prompt-index.json` 和 `codex-image-smoke-<sheetId>.json` 在 §2 canonical artifacts 中补精确路径；当前 §3 有命令，但 canonical artifact 表未列 smoke report 文件名。

## Open Questions

1. PR-02 是否真的需要 `ui.screen.home.title_frame` / `ui.screen.home.action_frame` 作为独立 manifest keys？如果只是语义区分，当前工具需要改；如果只是复用 chrome，建议直接消费 `ui.frame.panel.body`，避免新增 alias authority。
2. `spriteSheetMapLint` 是要演进为 PR-specific task，还是保留一个 task 但通过 Gradle property 切换 report path？两者都可以，文档必须二选一。

## Suggested Verification

修正文档和工具后建议执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest --rerun-tasks
./gradlew syncPhase2Manifests manifestLint
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.screen.StandaloneScreenLayoutTest
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons
```

PR-02 owner entries 尚未实现前，最后一条应失败；实现完成后必须 PASS，且 report 同时满足：

- `ownerCoveredKeys == ownerExpectedKeys`
- `ownerMissingKeys=[]`
- `ownerPendingKeys=[]`
- `ownerOldStyleKeys=[]`
- `allowedOwnerFallbackKeys=[]` 或该字段已删除/重命名
- `ownerSheetIds=["r01-ui-chrome","r01-ui-controls","r01-ui-hud-icons"]`
- `requiredOwnerSheetIds=["r01-ui-chrome","r01-ui-controls","r01-ui-hud-icons"]`

## Doc-Vs-Implementation Self-Audit

| Area | Current State | Audit Result |
| --- | --- | --- |
| `requiredOwnerSheetIds` wiring | 文档、Gradle property、coverage script、negative tests 已补 | 上一轮 P1 主体已部分解决 |
| owner-scope covered semantics | script 写出 `ownerCoveredKeys`，但不作为 fail 条件 | 未解决，P1 |
| alias contract | 文档新增 alias table，但未对齐现有 lint | 新引入/暴露 P1 |
| reserved cells | 文档要求 reserved，但工具不检查 full-grid occupancy | 未完全解决，P2 |
| sprite map report | 文档要求 report，工具默认仍 PR-00 report path | 未解决，P2 |

## Changed Surface Review

本轮工作树显示 PR-02 相关改动集中在：

- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`
- `scripts/verify_dark_manifest_coverage.py`
- `tools/build.gradle.kts`
- `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt`
- `UI/review/2026-05-09-dark-uiux-pr02-pr-level-standard-rereview-round2.md`

这些改动没有触碰 `core` / `game`。风险集中在 `tools` gate 语义和 `assets` artifact 合同，不是 gameplay rule 边界问题。

## Executed Validation

已执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew acceptanceContractLint
```

结果：PASS。

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest --rerun-tasks
```

结果：PASS。

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome,r01-ui-controls,r01-ui-hud-icons
```

结果：按当前未实现 PR-02 owner entries 的仓库状态失败，报错包含：

- `owner-scope coverage found no expected keys for PR-02`
- `owner-scope missing required sheet ids for PR-02`

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-00 -Pktome.darkUiux.requiredOwnerSheetIds=r01-ui-chrome
```

结果：PASS，但报告中 `ownerCoveredKeys=[]`，证明 owner-scope 当前可 false green。

## Not Executed / Residual Risk

未执行 `:client:clientSmoke`、`:client:goldenScreenshot`、`verifyChanged`。原因：当前任务是 PR-02 文档复审，且 PR-02 owner entries/raw sheets 尚未正式实现；这些 gate 现在不能证明 PR-02 player-facing outcome。剩余风险是 P1 修正后需要再跑一次完整 owner gate 和 client/golden 验证。

## Summary

新版 PR-02 文档比上一轮明显更接近可执行：三张 owner sheet、执行顺序、direct cell、coverage artifact、focused tests 和白盒矩阵都已经补齐。但它仍不能直接交给开发实施，因为 §4.5 alias 合同会和当前 lint 正面冲突，owner-scope coverage 也还不能证明资源真正进入 dark-v1 output。先修两个 P1，再补 P2 的 reserved grid、sprite map report path、manual record 字段，PR-02 才能达到“无需猜测即可开发”的标准。

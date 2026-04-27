# Dark UI/UX PR 文档第三轮深度 Review

日期：2026-04-27

范围：

- `UI/PLAN.md`
- `UI/ART_STYLE_BIBLE.md`
- `UI/pr/README.md`
- `UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md`
- `UI/pr/dark-uiux-pr01-start-screen-and-menu.md`
- `UI/pr/dark-uiux-pr02-hud-actionbar-inventory-shell.md`
- `UI/pr/dark-uiux-pr03-character-inventory-item-visuals.md`
- `UI/pr/dark-uiux-pr04-combat-feedback-and-effect-language.md`
- `UI/pr/dark-uiux-pr05-map-actors-bestiary-progression-visuals.md`
- `UI/pr/dark-uiux-pr06-loading-status-fallback-and-debug-visuals.md`
- `UI/pr/dark-uiux-pr07-final-assembly-and-packaged-whitebox.md`
- 已有 review 报告：`UI/review/*`
- 对照脚本与 manifest：`scripts/manifest-lint.py`、`scripts/sync_phase2_manifests.py`、`scripts/asset_pipeline_common.py`、`build.gradle.kts`、`client/src/main/resources/manifests/visual-manifest.json`

## 结论

第二轮报告里的主要方向性问题已经明显收敛：`r09-fallback-polish` ownership 已归 PR-06，PR-05 已补分轮 checkpoint，prompt 32/64 命名已统一，`darkManifestCoverageLint` 也已经被提升为独立 gate 概念。

但当前文档仍未达到可以直接串行实施的标准。剩余问题不是“有没有写到”，而是“写到后是否能被 PR 作者无歧义执行”。当前最需要先修的是：

1. `darkManifestCoverageLint` 的 mode/owner 参数没有形成可执行命令合同。
2. key registry 的真源与生成路径仍然不闭合。
3. PR-00 对 `manifest-lint.py` 和新增 UI key 的桥接方式仍停留在“需要决定”，不是可实现 contract。
4. `UI/PLAN.md` 与 PR-02/PR-03/PR-06 仍存在若干资源表、容量和 key 列表不一致。

这些问题会直接影响后续 PR 是否能独立验证、是否会在 canonical manifest / runtime manifest / sprite plan 之间产生第二真源。

## Findings

### P1-1 `darkManifestCoverageLint` 的模式被定义了，但每个 PR 的命令仍然不可区分

证据：

- `UI/PLAN.md:68-76` 定义了 `pr00-dry-run`、`owner-scope`、`final-full` 三种模式。
- `UI/pr/README.md:205-209` 的共享命令仍是：

```bash
./gradlew darkSpriteSheetLint spriteSheetMapLint darkManifestCoverageLint
```

- `UI/pr/dark-uiux-pr02-hud-actionbar-inventory-shell.md:86-90` 只在文字里说 `owner-scope + ownerPr=PR-02`，但命令还是裸 `darkManifestCoverageLint`。
- `UI/pr/dark-uiux-pr06-loading-status-fallback-and-debug-visuals.md:108-112` 才首次建议 `-Pktome.darkUiux.coverageMode=final-full`，但这个参数名没有在 PR-00 或 README 里被固定成合同。

问题：

同一个裸 task 在不同 PR 中语义不同：PR-00 dry-run、PR-02 owner-scope、PR-06 final-full。如果 PR-00 不固定 Gradle property 名称、默认值和失败语义，后续 PR 作者无法判断自己跑的是哪种 gate，也无法在 CI / `verifyChanged` 中稳定复用。

建议：

- 在 PR-00 中固定唯一命令协议，例如：

```bash
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=owner-scope \
  -Pktome.darkUiux.ownerPr=PR-02
```

- README 和所有 PR 文档都改为显式命令，不再使用裸 `darkManifestCoverageLint` 表示不同模式。
- PR-00 明确默认模式。如果默认不是 `final-full`，必须说明裸 task 只允许本地开发使用，CI / PR gate 必须带 mode。

### P1-2 key registry 的真源和生成路径仍不闭合

证据：

- `UI/pr/README.md:78-95` 要求 PR-00 交付 `UI/sprite-sheets/key-registry.yaml` 或从 sheet-plan 生成 `build/reports/dark-v1/key-registry.json`。
- `UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:54-67` 要求 registry 字段包含 `ownerPr`、`fallbackKey`、`consumer`、`consumerTest`、`aliasOf`。
- `UI/PLAN.md:78-88` 的 sheet-plan schema 只有 `sheetId`、`ownerPr`、`outputName`、`cells.targetKey`、`fallbackKey` 等少量字段，没有 `consumer`、`consumerTest`、`aliasOf` 的完整来源。
- 当前仓库中 `key-registry` 只存在文档引用，没有脚本或 Gradle task 名称。

问题：

文档说“yaml 或 generated json 均可”，但没有说明 generated json 的输入是否足够。按当前 sheet-plan schema，无法稳定生成 README 要求的完整 registry。后续 PR 如果选择 generated 路线，会立即遇到缺字段或各 PR 自己补字段的问题，最终形成第二真源。

建议：

- 二选一，不要保留模糊选择：
  - 方案 A：把 `UI/sprite-sheets/key-registry.yaml` 作为强制人工维护真源，sheet-plan 只引用它。
  - 方案 B：扩展 sheet-plan schema，补齐 `consumer`、`consumerTest`、`aliasOf`，并在 PR-00 固定 `generateDarkUiuxKeyRegistry` task。
- 如果保留 generated 路线，PR-00 必须写清输入文件、输出文件、失败条件和验证命令。

### P1-3 PR-00 仍把 manifest-lint 桥接写成待决事项

证据：

- `UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:111-113` 写的是 PR-00 必须决定旧 `manifestLint`、`darkManifestCoverageLint` 与 `VisualManifestResolveTest` 的边界。
- 当前 `scripts/manifest-lint.py` 会校验 canonical/runtime manifest 同步，并在 canonical visual manifest key 缺少 upstream spec 覆盖时失败。
- 当前 runtime manifest 中尚未存在 PR-02 计划新增的 `ui.frame.*`、`ui.hud.*`、`ui.control.*` key。

问题：

PR-02 开始就会新增 UI key，而现有 `manifest-lint.py` 是 canonical manifest 强约束。PR-00 如果只是说“决定边界”，没有给出具体桥接方式，PR-02 无法判断新增 key 应该走哪条合法路径：

- 扩展 `manifest-lint.py` 读取 dark registry / sheet-plan；
- 给 `asset-usage-spec.json` 补 canonical spec；
- 还是用 PR-00 dry-run fixture，不写入 canonical manifest。

建议：

- PR-00 必须把“决定”改成“实现并冻结”。
- 明确新增 UI key 的第一阶段路径：是否允许进入 canonical manifest、是否必须同步 asset usage spec、是否可通过 dark registry 作为 coverage source。
- 在 PR-02 文档中引用该路径，避免 PR-02 自己定义 manifest 例外。

### P2-1 `UI/PLAN.md` 的 sheet-plan 示例把 HUD sheet 和 control key 混在一起

证据：

- `UI/PLAN.md:144-168` 示例使用 `sheetId: r01-ui-hud-icons`。
- 示例 cells 却使用 `ui.combat.action.icon`、`ui.combat.method.icon`。
- `UI/pr/README.md:36-38` 与 `UI/pr/dark-uiux-pr02-hud-actionbar-inventory-shell.md:30-32` 将 `r01-ui-hud-icons` 和 `r01-ui-controls` 分成两个 sheet。

问题：

这是示例，但 PR-00 会把示例作为 dry-run 与 schema contract 的样板。如果样板本身把 sheetId 和 targetKey 所属族混用，后续自动生成 key registry、prompt index 或 manifest patch 时会把 owner/sheet/category 带偏。

建议：

- 把示例改成 `sheetId: r01-ui-controls`，或把 targetKey 改成 `ui.hud.*`。
- 在 sheet-plan schema 中增加校验：`sheetId` 和 `targetKey` prefix/category 必须匹配 sheet ownership table。

### P2-2 `UI/PLAN.md` 的 UI Key Additions 表仍少于 PR-02 的实际新增 key

证据：

- `UI/PLAN.md:305-323` 只列出 12 个 UI key。
- `UI/pr/dark-uiux-pr02-hud-actionbar-inventory-shell.md:47-63` 列出 15 个 key。
- PR-02 多出的 key 包括 `ui.hud.key.icon`、`ui.control.backpack.icon`、`ui.control.equipment.icon`。

问题：

`UI/PLAN.md` 是上游规划文档。如果上游 key 表少于 PR 文档，后续 PR-00 生成 registry 或 PR-06 final-full coverage 时，会出现“以 PLAN 为准还是以 PR-02 为准”的冲突。

建议：

- 同步 `UI/PLAN.md` 的 UI Key Additions 表到 PR-02 的 15 个 key。
- 或者明确该表只是 excerpt，不是 registry 真源；正式 key 列表只以 PR-00 key registry 为准。

### P2-3 `r09-fallback-polish` 的容量与 PR-06 要覆盖的 fallback/debug 范围不匹配

证据：

- `UI/PLAN.md:117` 统计 debug 类 key 为 14。
- 当前 manifest 中 hidden/fallback/debug/missing 相关 key 静态统计为 18 个。
- `UI/PLAN.md:301` 将 `r09-fallback-polish` 定为 large-sheet，容量 16。
- `UI/pr/dark-uiux-pr06-loading-status-fallback-and-debug-visuals.md:35` 要求该 sheet 覆盖 `missing_visual`、hidden/debug/fallback 主路径以及 rejected cell。

问题：

如果 `r09-fallback-polish` 同时承担 missing/hidden/debug/fallback 和 rejected cell，它的 16 格容量很可能不足。容量不足会导致 PR-06 要么临时扩 sheet，要么把部分 fallback 留在旧风格，从而破坏 final-full gate。

建议：

- 在 PR-06 前先做一张明确的 final inventory 表，列出每个 `fallbackKey`、debug key、hidden key、rejected cell 的目标 sheet。
- 如果超过 16，拆成 `r09-fallback-polish-a` / `r09-fallback-polish-b`，或把 rejected cell 归入独立 test fixture sheet。

### P2-4 PR-03 的 `r07-items-affix-material` 混入了 UI frame 语义

证据：

- `UI/pr/dark-uiux-pr03-character-inventory-item-visuals.md:30-35` 中 `r07-items-affix-material` 同时包含 affix marker、quality frame、craft material、empty slot、locked slot。
- 同一段又要求装备槽框架归 `ui_frame`，并复用 PR-02 的 `ui.frame.slot.*`。
- `UI/pr/README.md:55-57` 将 `r07-items-affix-material` 的 category 标为 `icon`。

问题：

item affix/material 和 slot frame 是不同资源族。把 empty/locked slot 放进 item sheet，会和 PR-02 的 `ui.frame.slot.empty`、`ui.frame.slot.locked` 形成重复资源或 alias 漂移。

建议：

- `r07-items-affix-material` 只保留 affix marker、quality marker、craft material。
- empty/locked slot 统一 alias 到 PR-02 `ui.frame.slot.*`，在 key registry 里用 `aliasOf` 表达。
- 如果 quality frame 是独立 frame，category 不应笼统写 `icon`，需要拆成 `ui_frame` 或单独 sheet。

### P2-5 PR-07 packaged app 白盒步骤只有 build，没有启动验收链路

证据：

- `UI/pr/dark-uiux-pr07-final-assembly-and-packaged-whitebox.md:59-77` 要求收集 packaged app 截图、日志、runtime home 和证据目录。
- 同段给出的命令只有 `./gradlew :client:packageMacApp`。

问题：

仓库级规则要求涉及安装包的改动必须补安装包启动与验收步骤。PR-07 当前只有打包命令，没有说明如何启动 packaged app、如何指定隔离 runtime home、截图和日志由哪个脚本产出、失败时保留什么证据。

建议：

- PR-07 增加一个明确的 packaged whitebox runbook，例如：
  - build package；
  - prepare isolated runtime home；
  - launch packaged app with fixed seed/profile；
  - collect screenshot/log/runtime evidence；
  - compare with debug client evidence。
- 如果已有 `preparePhase4V4Whitebox` 可复用，应明确复用方式；否则 PR-07 应新增 dark-uiux packaged whitebox helper。

### P2-6 coverage artifact 字段定义在 PLAN、README、PR-00、PR-06 中重复且不完全一致

证据：

- `UI/PLAN.md:76` 要求至少包含 `scopeMode`、`ownerPr`、`expectedKeySetSource`、`strictOldStyleResidue`。
- `UI/pr/README.md:113` 使用同一组字段。
- `UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:139` 又增加 `oldStyleResidue`、`pendingKeys`。
- `UI/pr/dark-uiux-pr06-loading-status-fallback-and-debug-visuals.md:74-89` 对 final-full artifact 又扩展了 `expectedKeySet`、`missingKeys`、`fallbackKeyUsage` 等。

问题：

字段扩展本身合理，但当前没有说明“基础字段”和“final-full 扩展字段”的层级关系。后续实现可能出现 PR-00 task 输出一套字段、PR-06 final-full 断言另一套字段。

建议：

- 在 PR-00 固定 artifact schema：
  - common fields；
  - owner-scope fields；
  - final-full fields。
- PLAN / README 只引用 PR-00 schema，不再各自维护字段列表。

### P3-1 PR-05 的 rollback 示例仍偏向 Round 5，Actor/Map 失败回滚边界不够明确

证据：

- `UI/pr/dark-uiux-pr05-map-actors-bestiary-progression-visuals.md:37-47` 已新增 Round 4 / Round 5 checkpoint。
- `UI/pr/dark-uiux-pr05-map-actors-bestiary-progression-visuals.md:98-102` rollback 示例仍主要描述 Round 5 bestiary 失败。

问题：

该问题已从上一轮的阻塞级下降为轻微文档缺口。checkpoint 已能隔离大部分风险，但 rollback 示例仍没有覆盖 Round 4 actor/map 失败时是否允许继续 Round 5。

建议：

- 在 rollback 段补一句：Round 4 actor/map baseline 失败时不得进入 Round 5；Round 5 失败不得回滚 Round 4 已通过资源。

### P3-2 PR-01 的测试路径仍使用宽泛 wildcard，不利于执行者核对 owner gate

证据：

- `UI/pr/dark-uiux-pr01-start-screen-and-menu.md` 的 impact / test 描述中仍出现较宽泛的 `client/src/test/kotlin/com/ktome/client/render/*` 类路径。

问题：

PR-01 作为第一个落地 PR，会被后续 PR 模仿。测试路径过宽不会直接造成错误，但会降低“哪些测试是 owner gate”的可核对性。

建议：

- 把 wildcard 改成具体 test class 或 task。
- 如果测试尚不存在，明确写成 “PR-01 must add ...”，不要让执行者自行猜路径。

## Requirement Alignment

| 要求 | 当前状态 | 结论 |
| --- | --- | --- |
| PR 拆分依赖清晰 | README 已有 dependency graph 和 sheet ownership | 基本满足 |
| PR-00 先冻结 pipeline/style/manifest contract | 已写出大部分 contract，但 manifest-lint 桥接和 key registry 仍未闭合 | 未满足 |
| 每个 PR 都能独立验证 owner-scope | 概念已存在，命令协议未固定 | 未满足 |
| PR-06 final-full 收口所有 fallback/debug/hidden | 文档目标正确，但容量和 inventory 风险未消除 | 部分满足 |
| PR-07 packaged app 白盒验收 | 证据要求完整，执行命令不足 | 部分满足 |
| 不引入第二真源 | 方向正确，但 key registry / sheet-plan / manifest coverage source 仍存在真源漂移风险 | 高风险 |

## 功能 / 系统一致性矩阵

| Surface | 当前文档状态 | 主要风险 | 必须收口点 |
| --- | --- | --- | --- |
| sprite sheet plan | schema 已定义 | 示例 key/sheet 错配，registry 生成字段不足 | 修示例；决定 registry 真源 |
| prompt contract | prompt 32/64 已统一 | 风险较低 | 保持 prompt index 与 sheet-plan 同源 |
| canonical manifest | canonical-first 已补 | 新 UI key 进入 manifest 的合法路径未固定 | PR-00 固定 manifest-lint bridge |
| runtime manifest | runtime generated 已写清 | 依赖 sync 与 lint 顺序 | PR-00 固定 Gradle task dependencies |
| darkManifestCoverageLint | 三种模式已定义 | 命令不可执行、mode 默认值不清 | 固定 Gradle properties / task variants |
| key registry | 字段目标明确 | 真源和生成路径不闭合 | yaml 强制或 generated task 强制 |
| packaged whitebox | 证据字段完整 | 缺启动/采集命令 | PR-07 补 runbook/helper |

## 玩法与体验审查

体验方向整体一致：暗黑 UI 的高对比框架、HUD/actionbar、装备/背包、战斗反馈、地图 Actor、loading/fallback/debug 收口都在向同一个视觉系统靠拢。当前没有看到明显的体验目标冲突。

仍需注意两个体验层风险：

1. PR-03 如果把 item icon、quality frame、slot frame 混在一个 sheet，最终 inventory 里会出现边框语言不统一。
2. PR-06 如果 fallback/debug 容量不足，final assembly 可能出现少量旧风格 residue，视觉上会比功能缺失更难排查，因为它只在边缘状态或 debug route 暴露。

## 当前阶段必须解决的问题

进入 PR-01/PR-02 实施前，必须先修：

1. PR-00 固定 `darkManifestCoverageLint` 的 Gradle 参数、默认模式、artifact schema。
2. PR-00 固定 key registry 真源：强制 yaml 或强制 generated task。
3. PR-00 固定新增 UI key 与 `manifest-lint.py` / canonical manifest / asset usage spec 的桥接方式。
4. 修正 `UI/PLAN.md` sheet-plan 示例和 UI Key Additions 表。

进入 PR-06 前，必须先修：

1. 重新盘点 `r09-fallback-polish` 容量。
2. 明确 fallback/debug/hidden/rejected cell 的 sheet 分配。
3. 将 final-full artifact schema 回写 PR-00，避免 PR-06 自定义一套输出。

进入 PR-07 前，必须先修：

1. 补 packaged app 启动与证据采集 runbook。
2. 明确 packaged app 与 debug client 的对比验收方式。

## Removal / Iteration Plan

建议按下面顺序迭代，避免后续 PR 带着不稳定 contract 实施：

1. 更新 PR-00：冻结 coverage mode CLI、artifact schema、key registry source、manifest-lint bridge。
2. 更新 README：只保留 PR-00 定义的命令和 schema 引用，删除重复字段定义。
3. 更新 PLAN：修 sheet-plan 示例、同步 PR-02 UI key 表、明确 `r09-fallback-polish` 是否拆分。
4. 更新 PR-02：所有 owner-scope 命令改为 PR-00 固定格式。
5. 更新 PR-03：拆分 item/material 与 ui_frame 语义，使用 `aliasOf` 复用 PR-02 slot frame。
6. 更新 PR-06：补 final inventory 和容量拆分方案。
7. 更新 PR-07：补 packaged app launch / runtime home / evidence collection runbook。

## Additional Suggestions

- PR-00 的输出文件建议统一放在 `build/reports/dark-v1/`，但所有文档中的可提交 artifact 路径必须保持 repo-relative。
- `ownerPr` 建议统一使用 `PR-00` / `PR-02` 这种字符串，不要混用 `pr02`、`PR02`、`PR-02`。
- `fallbackKey` 如果允许在 owner-scope 阶段指向 `missing_visual`，必须在 PR-06 final-full 明确禁止哪些 key 仍指向 `missing_visual`。
- 对 `aliasOf` 增加 lint：alias target 必须存在，alias 不能形成链式循环。
- 对 sheet capacity 增加 lint：sheet-plan cells 数量不得超过 declared capacity。

## Suggested Verification

文档修复后建议至少做以下静态验证：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=pr00-dry-run
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full
./gradlew manifestLint
./gradlew verifyChanged
```

PR-07 文档修复后还需要补一条 packaged app 验证链，命令名以实际 helper 为准，但必须覆盖：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp
# launch packaged app with isolated runtime home
# collect screenshot/log/runtime evidence
# compare packaged evidence with debug-client evidence
```

本轮 review 只做了文档与脚本静态核对，未运行 Gradle gate。当前报告的结论不等价于实现可通过。

## Summary

本轮修改已经把上一轮的大部分“方向性缺失”补上了，但 PR 文档仍需要一次 contract-level 收口。优先修 PR-00 和 README，不要让后续 PR 自己解释 coverage mode、key registry、manifest-lint bridge。等这些合同固定后，PR-01 到 PR-07 才具备真正的串行实施条件。

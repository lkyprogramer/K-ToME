# Dark UI/UX PR-06 Full Manifest 设计总监 Review Round 4

审查对象：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`

结论：**当前文档方向正确，但不应直接进入实现。** 它已经把 PR-06 定位为 dark-v1 玩家可见资源的 `final-full` 收口，并且覆盖了 status / quest / skill / validation overlay / profession disposition 等关键体验面；但仍有 4 个阻断级合同问题会导致实现阶段出现分母漂移、sheet 切分不可执行、bridge gate 漏测或跨 PR handoff 失真。建议先修本文档，再开工实现。

## 预检摘要

- 命中阶段：Dark UI/UX PR-06，职责是 skill / talent / status / mutation / quest / profession / fallback 的 full manifest 主体收口。
- 关键入口：`UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`UI/pr/screen-coverage-matrix.md`、目标 PR06 文档。
- 受影响模块：`assets` / `client` / `tools`，并实际触碰 `core` status definition 与 `game` status/profession schema 路径。
- 关键合同：PR06 close gate 必须是 `final-full` 且不得带 `ownerPr`；sheet plan 是 row/col 固定网格合同；bridge PR 触碰 `core/game` 时必须列 owner suite；coverage denominator 不允许回退成第二真相。
- 非目标：本报告不审美术生成质量本身，不替代后续 contact-sheet QA / golden / packaged whitebox。

## Findings

### P1 · `talent.*.visual` 的 2x2 cell merge 与全局 sheet 合同冲突

目标文档要求：如果 `talent.*.visual` portrait 与 `icon.skill.*` / `talent.*.icon` 同 sheet，`sheet plan` 必须标记 portrait-size group，例如 `2x2 cell merge`（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:138-145`）。

这和当前资源管线硬约束直接冲突：

- `UI/PLAN.md:178-187` 固定 `sheetId + row + col` 全局唯一、`targetKey` 对齐 manifest、非 reserved key 必须进入 key registry。
- `UI/ART_STYLE_BIBLE.md:109-120` 明确禁止跨格，要求固定网格、每个 cell 一个主体。
- `UI/ART_STYLE_BIBLE.md:122-128` 已有 `icon-sheet` 128px cell 与 `large-sheet` 256px cell 两种合法载体。

风险不是“格式表述不严谨”，而是会破坏切分脚本、contact-sheet QA、`rawOutputPath` 对齐和 manifest key 到单一 cell 的可审计关系。2x2 merge 一旦进入 sheet plan，后续 `slice_spritesheet` / `spriteSheetMapLint` / coverage report 都会被迫增加第二套裁切语义。

建议改法：

1. 删除 `2x2 cell merge` 作为可选实现。
2. `talent.*.visual` 需要 portrait 尺寸时，放入 `large-sheet` 的 256px 单格，或定义独立 `tree.*` / `talent.*.visual` owner sheet。
3. 如果 PR06 暂不交付 portrait visual，就进入 explicit pending / allowed exclusion，并写清 removal owner；不能通过 merged cell 绕过现有 pipeline。
4. 若确实要支持跨格 portrait，必须先作为独立 pipeline contract PR 修改 `UI/PLAN.md`、`ART_STYLE_BIBLE`、sheet-plan schema、切分脚本和工具测试；不应混入 PR06。

### P1 · Acceptance Matrix 漏列 `core/game` bridge owner 与 blocking gate

PR06 影响范围表已经列出 status resolver、status presentation、coverage script、Gradle wiring 和工具测试（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:101-122`），并在 status 合同里要求 `StatusDefinitionsTest`、`StatusSchemaContractTest`、`StatusIconResolverTest` 等（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:324-337`）。

但 Acceptance Matrix 目前只声明 `assets` / `client` / `tools` / `docs` owner（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:26-38`），没有把实际会被触碰的 `core` / `game` 列为 bridge owner。当前仓库证据已经说明它不是纯 UI 文档：

- `core/src/main/kotlin/com/ktome/core/status/StatusRuntime.kt:106-112` 的 `StatusEffectDef` 已有 `iconKey`。
- `game/src/main/resources/data/statuses/index.yaml:1-18` 在 official status data 中维护 `visualKey` / `iconKey`。
- `game/src/main/kotlin/com/ktome/game/PlayerCreationModels.kt:15-33` 的职业创建 option 已暴露并校验 `icon.profession.*`。
- `UI/pr/development-governance.md:111-120` 要求 bridge PR 触碰 `core/game`、official data、schema、resolver、snapshot、content pack 或 lint 时，必须在 Acceptance Matrix 分别列 owner、fastCheck、ownerGate 和 artifact。

缺口会导致 PR06 看起来通过 `clientSmoke` / `goldenScreenshot`，但 status schema、profession schema 或 core definition 合同没有作为 blocking gate 被验收。

建议改法：

1. 在 Acceptance Matrix 增加 `core` slice：status runtime definition / actor status icon family。Blocking gate 至少包括 `StatusDefinitionsTest`、相关 `:core:test` focused path 或等价 owner suite。
2. 增加 `game` slice：official status YAML schema、profession player creation icon key、schema loader。Blocking gate 至少包括 `StatusSchemaContractTest`、`ProfessionSchemaTest` 和 relevant `:game:test` focused path。
3. 如果 PR06 不准备触碰 `core/game`，则必须反向删掉文档中对 `StatusDefinitions`、status YAML、profession icon schema 的实现要求，把它们降为 explicit follow-up；不能一边要求实现，一边不列 owner gate。

### P1 · final-full inventory 仍可能变成人工第二真相

文档将 `dark-v1-final-full-inventory.json` 作为 PR06 开工第一步，并要求 final-full 的 `expectedKeySetSource` 指向 inventory（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:233-244`）。方向是正确的：registry 只做 per-key metadata，inventory 冻结 final-full 分母。

问题是 schema 允许 `generatedBy` 为 “script or manual command”（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:246-262`），但没有冻结 deterministic generator、source ordering、hash/drift check 和 stale inventory fail-fast。当前工具也仍是 registry-only：

- `scripts/verify_dark_manifest_coverage.py:41-52` 没有 `--expected-inventory` 参数。
- `scripts/verify_dark_manifest_coverage.py:131-145` 在非 owner-scope 下直接使用 registry keys 作为 expected keys。
- `scripts/verify_dark_manifest_coverage.py:212-218` 把 `expectedKeySetSource` 写成 registry path。
- `scripts/verify_dark_manifest_coverage.py:266-280` final-full report 仍固定 `allowedCoverageExclusions=[]`。
- `tools/build.gradle.kts:646-692` 尚未声明 `ktome.darkUiux.expectedInventory` property 或 task input。

如果 inventory 可以手写，PR06 只是把“registry 漏登记”换成“inventory 漏登记”，仍然无法防止玩家可见 key 被 omission 缩小分母。

建议改法：

1. 文档新增 deterministic inventory generator，例如 `scripts/generate_dark_final_full_inventory.py` 或 Gradle task。
2. generator 输入必须固定：canonical manifest、runtime manifest、game data indexes、screen coverage matrix、key registry、PR03/05 handoff normalization artifact。
3. inventory 必须包含 source file digest 或 generated snapshot metadata；`darkManifestCoverageLint final-full` 必须在 source 变化且 inventory stale 时 fail。
4. `generatedBy` 应收窄为 generator 命令；人工只允许写 `manualOverrideReason` / `reviewerNote`，不能成为生成路径。
5. 工具测试除当前文档列出的两个外，还应覆盖 stale inventory、source order deterministic、manual malformed override。

### P1 · PR03/PR05 handoff schema 与现有 artifact 不匹配

PR06 文档要求 PR03/PR05 sprite map report 提供 `targetKey`、`sheetId`、`qaStatus`、`rejectionReason`、`playerVisible`、`ownerPr`、`evidencePath`（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:164-176`）。

现有 artifact 不满足这个输入合同：

- `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl:1-3` 有 `targetKey`、`sheetId`、`qaStatus` 等，但没有 `playerVisible`、`ownerPr`、`evidencePath`。
- `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl:1-3` 同样缺这些字段，而且 `qaStatus=DRY_RUN`、`reviewedAt=null`、`reviewer=null`。

文档只说明“找不到上游 artifact 时”要补 `dark-v1-pr06-handoff-inventory.json`（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:176`），但没有覆盖“artifact 存在但 schema 过旧或未 reviewed”的情况。这个缺口会让 PR06 无法可靠判断哪些 PR03/05 rejected/pending cell 是 player-visible，最终可能把需要 PR06 返修的资源静默转给 PR07。

建议改法：

1. 把 `UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json` 从 missing-artifact fallback 升级为必经 normalization artifact。
2. normalization 必须从 PR03/05 JSONL、manual records、coverage report 和 owner contract 合成 `playerVisible`、`ownerPr`、`evidencePath`，并记录 derivation source。
3. 对 `DRY_RUN`、`reviewedAt=null`、缺 `playerVisible` 的行 fail fast，除非明确登记 conservative decision。
4. final-full inventory 必须只消费 normalized handoff，而不是直接读旧 schema JSONL。

### P2 · rework PR gate 混用了互斥的 `final-full` 与 `owner-scope`

文档开头已正确声明 PR06 close gate 固定为 `final-full`，不得带 `ownerPr` 缩小分母（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:20-22`）。但 Acceptance Matrix 和 rework PR 合同又写出 `darkManifestCoverageLint final-full owner-scope` / “final-full coverage 的 owner-scope 模式”（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:36-38`、`148-153`）。

`UI/pr/README.md:149-173` 定义的 coverage modes 是互斥的：`owner-scope` 需要 `ownerPr`，`final-full` 不允许 `ownerPr` 改变分母。

建议改法：

1. rework PR 自身用 `owner-scope` 验证 replacement sheet 涉及 keys。
2. rework 合入主 PR 后，PR06 主 PR 再跑全量 `final-full`。
3. 不要发明“final-full owner-scope”这个口径；它会让 reviewer 不知道该按全量分母还是局部分母验收。

### P2 · packaged-app sentinel audit 没有可执行输入合同

PR06 要求 `darkManifestCoverageLint final-full` 新增 packaged-app sentinel audit 输入，扫描 packaged app smoke/golden evidence 中是否出现 `missing_visual`（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:398-410`）。这是有价值的，因为 missing sentinel 是玩家可见失败信号。

但验证命令段只列出 expected inventory property、focused tests、client evidence 和 governance gate（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:521-553`），没有定义 packaged evidence 输入路径、Gradle property、report field、failure schema 或工具测试。当前 `darkManifestCoverageLint` task 也没有对应 input（`tools/build.gradle.kts:646-692`）。

建议改法：

1. 定义 property，例如 `-Pktome.darkUiux.packagedSentinelEvidence=UI/manual-records/...` 或 `client/build/reports/golden/...`。
2. report 增加 `packagedSentinelEvidencePaths`、`packagedMissingVisualHits`、`packagedSentinelAuditStatus`。
3. 工具测试覆盖 no evidence / evidence clean / evidence references `missing_visual` 三种结果。
4. 若 PR06 只准备把 packaged evidence 留给 PR07，则 coverage lint 不应声称会扫描 packaged app；应改成 PR06 产出 sentinel semantics，PR07 执行 packaged audit。

### P2 · status HUD fold 合同需要明确 renderer / locale owner

文档要求 actor status icon 行最多显示 10 个，超出后用 `ui.status.fold.summary` 渲染固定末尾 slot，并支持 hover/tap 或明确 non-interactive follow-up（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:324-337`、`564-579`）。

当前影响范围只列 `StatusIconResolver` 和 `StatusPresentationModel`（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:109-116`），而现有 resolver 返回的是完整列表，没有 fold/pagination model（`client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt:22-37`）。如果 fold owner 不明确，实现很容易落到 renderer 局部临时裁剪，导致 tooltip、排序、locale token 和 golden label 分散。

建议改法：

1. 在影响范围中明确 fold model owner，例如 `StatusHudPresenter` / `StatusHudIconRowModel` / `TileRenderModel` 的具体边界。
2. 明确 locale owner：`ui.status.fold.summary`、non-interactive copy、hover detail title/body。
3. 将 “resolver 只做 manifest resolve” 与 “HUD row 做 fold / interaction model” 分开，避免 asset resolver 成为 UI 布局 owner。

### P3 · PR02-2 作为分母来源但索引可发现性不足

PR06 多次要求把 PR02-2 `ui-demo-new` map/actor/prop keys 纳入 final-full 分母（`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:46-48`、`164-176`、`225-229`），`screen-coverage-matrix` 也把 PR02-2 作为首页/主 shell 证据的一部分（`UI/pr/screen-coverage-matrix.md:27-33`）。

但 `UI/pr/README.md:105-110` 的执行 PR 表只列到 PR-02-1 / PR-03 / PR-05 / PR-06，PR02-2 作为 evidence source 的定位不够清楚。建议在 PR06 的 Cross-PR handoff 表或 README 补一个显式 evidence anchor，避免执行者找不到 PR02-2 的 authoritative artifact。

## 设计与玩法体验判断

这份 PR06 文档有几个值得保留的强点：

1. `final-full` 不允许 `ownerPr` 缩小分母，这是防止“分包完成但玩家可见界面仍旧图”的正确方向。
2. status / mutation / damage type 三套视觉语法拆分清楚，避免玩家把 actor condition、body mutation 和 damage channel 误读成同一类 buff。
3. frozen profession disposition 写得足够产品化：隐藏优先，允许 dev-preview locked，不允许 polished locked fallback 误导为正常解锁目标。
4. validation overlay 的 compact/detailed presenter 集中化是必要抽象，可以避免多个 leaf formatter 各自截断造成信息丢失。
5. 60 分钟 long-session、accent strength budget、色弱模拟和 same-screen overview 对 Roguelike 长时阅读体验有实际价值，不是形式化白盒。

需要坚持的体验原则：

- PR06 是“玩家能否相信界面语义”的收口，不只是资源补图。状态、技能、任务、职业和验证 overlay 同屏时，第一眼优先级必须稳定。
- `missing_visual` 应该是开发失败哨兵，不应在 packaged / normal play 中成为可接受美术风格。
- status fold 不能把“看不见的危险 debuff”隐藏成无反馈；10 个上限可以接受，但隐藏项必须可解释、可展开或明确 follow-up。
- frozen/dev profession 的展示必须服务 onboarding 诚实性，不能靠漂亮卡片制造“只是还没解锁”的错觉。

## 修订建议顺序

1. 先修文档合同：移除 2x2 merge、补 `core/game` bridge matrix、明确 deterministic inventory generator、把 handoff normalization 升级为必经 artifact。
2. 再修工具链合同：`--expected-inventory`、Gradle input、allowed exclusion、stale inventory、packaged sentinel evidence 输入。
3. 再落资源与 manifest：Round 8-9 sheet plan、key registry、canonical/runtime manifest、contact-sheet QA。
4. 再落 client presentation：quest row、status HUD fold、validation overlay presenter、profession/frozen disposition。
5. 最后跑 final-full / golden / manual：不能先 golden rebaseline 再回头补 coverage。

## 建议验证入口

文档修完后，建议先跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

实现阶段最小闭环：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint \
  -Pktome.darkUiux.coverageMode=final-full \
  -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json
./gradlew :client:test \
  --tests com.ktome.client.ui.status.StatusPresentationModelTest \
  --tests com.ktome.client.ui.status.StatusIconResolverTest \
  --tests com.ktome.client.render.TileRenderModelTest \
  --tests com.ktome.client.render.TileRendererCanvasTest
./gradlew :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint
./gradlew verifyChanged
```

如果 coverage task wiring、Gradle input 或 bootstrap 相关文件发生变化，还需要补跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./scripts/verify-bootstrap.sh
```

## 最终判断

**建议状态：Request changes before implementation.**

不是因为 PR06 文档不够细，而是因为它已经很接近执行合同，任何语义松动都会直接进入工具链和资源生成。优先修掉上述 P1，再允许进入实现；P2 可以和 P1 同轮补齐，否则 PR06 close 时会再次卡在 review / evidence 解释上。

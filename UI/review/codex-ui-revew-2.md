# Dark UI/UX PR 文档深度 Review 2

日期：2026-04-27
对象：`UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/*.md`
对照真源：`AGENTS.md`、现有 `build.gradle.kts`、`scripts/*lint.py`、`scripts/sync_phase2_manifests.py`、`client` manifest loader/test。

## 总结

本轮修改已经吸收了上一版 review 的大部分硬问题：`dark-v1` 风格纪元、sheet-plan schema、Round 1 sheet owner、PR-05/06 sheetId、Berserker/Spellblade 覆盖、PR-06 coverage artifact、PR-07 packaged app 白盒都已经补进文档。

剩余风险集中在 **文档新增的 dark-v1 管线与现有仓库真实 manifest/lint/CI 合同之间还没完全闭环**。如果按当前 PR 文档直接开工，最可能卡在 PR-02：新增 UI key 和 dark-v1 path 后，`manifestLint`/`syncPhase2Manifests`/`verifyChanged` 的真实行为没有被文档定义清楚。

当前结论：**不建议直接进入 PR-02 资源生成；应先补 PR-00 的 manifest authority、coverage gate 分阶段语义、`verifyChanged` 接线和 key registry 可执行合同。**

## 已确认修复

1. `UI/PLAN.md:46-58` 已新增 multi-epoch sidecar strategy，承认现有 `assetLint / styleLint / manifestLint` 不覆盖 `UI/sprite-sheets/sheet-plan.yaml`。
2. `UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:30-37` 已把 PR-00 schema 对齐到 `UI/PLAN.md` 的 Mapping Spec，并把 QA 输出从 plan 输入中拆开。
3. `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:29-34`、`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:32-35` 已修正 source cell 与 runtime/display size 的混淆。
4. `UI/pr/README.md:33-62` 已新增 sheetId ownership table，PR-02/05/06 的 sheetId 基本回到同一套命名。
5. `UI/PLAN.md:72-82`、`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:36-40` 已明确 4 个 release playable、2 个 dev playable、2 个 frozen excluded 的覆盖口径。
6. `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md:53-71` 已把 packaged app 白盒改成默认必跑，并固定证据字段。

## Findings

### P1-1：PR 文档只写 runtime manifest，漏了 canonical manifest 真源，`manifestLint` 会覆盖或拒绝这些改动

证据：

- `UI/PLAN.md:313` 只列 `client/src/main/resources/manifests/visual-manifest.json` 作为更新对象。
- PR-02/03/05/06 影响范围也只写 runtime manifest：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:24`、`UI/pr/dark-uiux-pr03-equipment-inventory-items.md:23`、`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:24`、`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:22`。
- 现有 `manifestLint` 先依赖 `syncPhase2Manifests`：`build.gradle.kts:840-885`。
- `syncPhase2Manifests` 会把 `assets-src/image/manifests/phase2-visual-manifest.json` 同步覆盖到 runtime `client/src/main/resources/manifests/visual-manifest.json`：`scripts/sync_phase2_manifests.py:11-22`。
- `manifest-lint.py` 还要求 canonical/runtime manifest 的 key、field、styleTag、prefixRules 完全一致，并要求 canonical key 有上游 spec 覆盖：`scripts/manifest-lint.py:89-116`、`:159-174`、`:268-282`。

问题：

当前文档让实现者只改 runtime manifest，但真实仓库里 runtime manifest 不是单一真源。若 PR-02 只改 runtime manifest，`syncPhase2Manifests` 会覆盖它；若同步改 canonical manifest，现有 `manifestLint` 又会因为 dark-v1 key/path 没有进入旧 `assets-src/image/specs/*` 覆盖面而失败。文档目前没有定义这条桥。

建议修复：

1. 在 `UI/PLAN.md`、PR-02/03/05/06 的影响范围中同时列出 `assets-src/image/manifests/phase2-visual-manifest.json` 与 runtime manifest。
2. 明确 manifest 改动顺序：先更新 canonical manifest，再由 `syncPhase2Manifests` 生成 runtime manifest；禁止手改 runtime 后当作真源。
3. 在 PR-00 里定义旧 `manifestLint` 如何接受 dark-v1 新 key：要么扩 `manifest-lint.py` 识别 `sheet-plan.yaml`/dark coverage artifact，要么明确把 dark-v1 manifest 校验完全交给 `darkManifestCoverageLint`，并让旧 `manifestLint` 不误报新增 dark key。
4. 在 PR-02 dry-run 中加入一个新增 UI key 的 canonical/runtime 同步样例，证明 `manifestLint` 不会和新 dark gate 互相打架。

### P1-2：`darkManifestCoverageLint` 缺少 PR 分阶段语义，早期 PR 会“不可能通过”或“假通过”

证据：

- `UI/PLAN.md:56` 把 `darkManifestCoverageLint` 定义为校验 player-visible key coverage、allowed fallback、old-style residue 和 manifest path。
- PR-00 验证直接运行 `darkManifestCoverageLint`：`UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:93-99`，但 PR-00 又声明不生成正式 PNG、不改 manifest：`UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:77-82`。
- PR-02 也要求运行 `darkManifestCoverageLint`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:70-76`，但 PR-02 只覆盖 Round 1 UI chrome/HUD。
- PR-06 才要求 `oldStylePlayerVisibleKeys` 为空：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:64-75`。

问题：

同一个 gate 在 PR-00/PR-02/PR-03/PR-05/PR-06 的严格程度应该不同，但文档没有定义 mode、scope 或 target set。按 PR-06 的全量口径跑，PR-00/PR-02 必挂；按宽松口径跑，PR-06 又可能漏掉旧风格残留。

建议修复：

1. PR-00 定义 `darkManifestCoverageLint` 的阶段模式，例如 `pr00-dry-run`、`owner-scope`、`final-full`。
2. coverage artifact 增加 `scopeMode`、`ownerPr`、`expectedKeySetSource`、`strictOldStyleResidue` 字段。
3. PR-02/03/05 只允许 owner-scoped pending；PR-06 必须切到 final-full，并要求 `oldStylePlayerVisibleKeys=[]`、`pendingOrRejectedPlayerVisibleCells=[]`。
4. 每个 PR 文档的验证命令或配置要写明当前 mode，避免同名 task 在不同阶段语义漂移。

### P1-3：PR-00 仍允许新 gate 不进 `verifyChanged`，这会让共享 CI preflight 失去资源管线强制力

证据：

- 仓库规则要求共享 PR CI 默认 preflight 是 `./gradlew verifyChanged`，新增 verification/report/gate 接线应优先复用既有 impact routing：`AGENTS.md:196-200`。
- PR-00 写明可以先不把新 gate 挂进 `verifyChanged`：`UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:56-65`。
- `UI/pr/README.md:137-153` 把 dark gates 作为资源 PR 追加命令列出，但这仍是人工 close gate，不等于 `verifyChanged` 自动强制。

问题：

PR-00 的核心目标是新增 pipeline gate。如果这些 gate 不进入 `verifyChanged` 或等价 impact routing，PR-02 以后只跑默认 preflight 时仍可能绕过 `sheet-plan.yaml`、contact sheet、coverage artifact。K-ToME 仓库已经把 `verifyChanged` 作为共享默认入口，新增长期 gate 不应长期停留在手工命令层。

建议修复：

1. PR-00 出口条件改为：`verifyChanged` 在检测到 `UI/sprite-sheets/**`、`assets-src/image/raw/sheets/dark-v1/**`、`client/src/main/resources/dark-v1/**`、visual manifest、dark-v1 report 变更时，必须触发对应 dark gate。
2. 如果 PR-00 内部需要先提交 dry-run task，再接 `verifyChanged`，文档应拆成两个 stop condition：root task 可发现、impact routing 生效。
3. PR-02 不应依赖人工记忆执行 dark gate；它应能通过 `verifyChanged` 或明确的 CI job 被强制。

### P2-1：Key Registry 只在 README 中声明，PR-00 没有把它变成 deliverable 或 lint 合同

证据：

- `UI/pr/README.md:77-92` 要求 PR-00 定义 `UI/sprite-sheets/key-registry.yaml` 或等价派生视图，并包含 `ownerPr / fallbackKey / consumer / consumerTest / aliasOf`。
- PR-00 的 schema 只承接 Mapping Spec 字段：`UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:30-37`。
- PR-00 的 gate 只写 sheet-plan/map/coverage，没有写 key registry 字段校验：`UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:56-65`。
- PR-02 的初始清单也只有 `targetKey / Category / Sheet / Consumer`，没有 `ownerPr / fallbackKey / consumerTest`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:36-55`。

问题：

key registry 是防止 renderer 裸字符串、fallback 漂移、UI key 无 owner 的关键合同。现在它停在 README 约束，没有成为 PR-00 的交付物，也没有进入 `darkSpriteSheetLint` 或 `spriteSheetMapLint` 的 fail-fast 条件。

建议修复：

1. PR-00 明确交付 `UI/sprite-sheets/key-registry.yaml`，或明确生成 `build/reports/dark-v1/key-registry.json` 的脚本和输入。
2. `darkSpriteSheetLint` 必须校验每个非 reserved `targetKey` 有 `ownerPr`、`fallbackKey`、`consumer`、`consumerTest`。
3. PR-02 的 UI Key Registry 初始表补齐 `fallbackKey` 与 `consumerTest`，否则后续 PR 无法判断 key 是否真正被 UI 消费。

### P2-2：`r09-fallback-polish` 是双 owner，削弱 rollback 与 coverage artifact 的归属

证据：

- SheetId Ownership 表声明 owner，但 `r09-fallback-polish` 写成 `PR-06 / PR-07`：`UI/pr/README.md:29-62`。
- PR-06 已经要求 Round 9 包含 `r09-fallback-polish`，并覆盖 missing/hidden/debug/fallback 主路径：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:27-35`。
- PR-07 又允许修复 rejected cell：`UI/pr/dark-uiux-pr07-golden-whitebox-polish.md:26-31`。

问题：

fallback/missing/hidden 是 coverage 收口的一部分，不适合双 owner。否则 PR-06 coverage artifact 到底应该对 `r09-fallback-polish` 负责到什么程度、PR-07 返修是否需要重开 PR-06 的 coverage 结论，会变得模糊。

建议修复：

1. 将 `r09-fallback-polish` owner 固定为 PR-06。
2. PR-07 只保留 `polishAllowedForRejectedCells=true` 的返修权限，必须回写 PR-06 coverage artifact 的修订记录。
3. 如果确实需要 PR-07 独立资源 sheet，应拆出 `r09-polish-rejected` 或 `r10-polish-rejected`，不要让一个 sheetId 同时归两个 PR。

### P2-3：PR-05 仍是过大的 XL 资源 PR，只有“允许 mini PR”但没有 per-round stop condition

证据：

- PR-05 一次覆盖 Round 2-6：tiles、props/VFX、actors、bestiary、portraits：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:28-35`。
- PR-05 回滚边界只说允许按 Round 拆内部批次或 mini PR：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:74-78`。
- 仓库工作包规则要求工作包明确范围、验证与白盒同步交付，并尽量避免过大切分：`AGENTS.md:146-156`。

问题：

PR-05 同时改地图可读性、actor silhouette、Boss telegraph、portrait、bestiary icon，review 和 rollback 成本很高。文档虽然允许拆 mini PR，但没有定义每轮的进入/退出条件、owner evidence、coverage artifact 和 golden label。实际执行时容易变成一个巨大资源 PR，最后问题定位困难。

建议修复：

1. 在 PR-05 中增加 per-round checkpoint 表：Round 2 tile、Round 3 prop/VFX、Round 4 actor、Round 5 bestiary、Round 6 portrait。
2. 每个 checkpoint 固定：sheetIds、manifest key scope、contact sheet QA、coverage summary、golden/manual evidence、可回滚文件族。
3. 每个 checkpoint 必须保持主干可玩；失败时只能回滚当前 checkpoint 的 sheet/manifest/report，不能拖累已通过的前置轮次。

### P2-4：PR-06 仍把 `assetLint` 当作 dark-v1 全覆盖证明，但现有 `assetLint` 只证明旧 styleTag 资产计划

证据：

- PR-06 验收写 `assetLint` 证明 `icon.skill.*`、`icon.tree.*`、`icon.status.*`、`icon.quest.*` 全部可解析：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:56-62`。
- 现有 `asset-lint.py` 要求 plan `styleTag` 等于 `EXPECTED_STYLE_TAG`：`scripts/asset-lint.py:85-89`。
- `EXPECTED_STYLE_TAG` 当前是 `ktome-middle-fantasy-painterly-tile-v1`：`scripts/asset_pipeline_common.py:16`。
- 文档又明确 dark-v1 不作为旧 lint 的 `--extra-plan` 静默塞入：`UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:63-65`。

问题：

`assetLint` 可以继续保护旧资源 plan，但不能作为 dark-v1 全覆盖证明。PR-06 的关键证据应该来自 `darkManifestCoverageLint`、`ManifestResolveTest` 和 coverage artifact。否则实现者会误以为旧 lint 绿了就代表 dark-v1 全量替换成功。

建议修复：

1. 将 PR-06 验收第 2 条改成 `darkManifestCoverageLint + ManifestResolveTest` 证明 key family 全覆盖。
2. `assetLint/styleLint/manifestLint` 的定位写成“旧资源合同未回归”，不要写成 dark-v1 coverage authority。
3. coverage artifact 必须列出 exact key set，而不是只列 key prefix。

### P3-1：Sheet Inventory 的 prompt 概要仍混入 `32x32` / `64x64` 生成语义

证据：

- `UI/PLAN.md:210` 的 Tiles prompt 仍写 `seamless 32x32 pixel tiles`。
- `UI/PLAN.md:215` 的 Items prompt 仍写 `64x64 slots`。
- 上游 Sheet Types 已固定 source cell：`UI/PLAN.md:104-111`。
- `UI/ART_STYLE_BIBLE.md:137-141` 明确 HUD/背包/装备显示尺寸只属于 renderer/layout，不能写成 source sheet cell 尺寸。

问题：

PR-02/03 的 cell-size 问题已经修掉，但总方案的 prompt 摘要仍可能让生图时按 32/64 像素物理格子理解。这里不是立即阻塞，但会增加 raw sheet 返工概率。

建议修复：

把这些 prompt 摘要改成：

- Tiles：`readable when displayed at 32px, source cells remain 128x128`
- Items：`inventory icons readable in 48-64px UI slots, source cells remain 128x128`

## Requirement Alignment

| Requirement | 状态 | 证据 | 缺口 |
| --- | --- | --- | --- |
| `sheet-plan.yaml` 是唯一映射真源 | 部分一致 | `UI/PLAN.md:121-164`; PR-00 `:30-37` | key registry 派生视图和 lint 字段未落到 PR-00 deliverable |
| 新 dark-v1 gate 可执行 | 部分一致 | `UI/PLAN.md:53-57`; PR-00 `:56-65` | 缺 stage/scope mode，且未强制接入 `verifyChanged` |
| VisualManifest 不改 entry schema | 一致 | `UI/PLAN.md:41-42`; PR-00 `:67-75` | 仍需定义 canonical/runtime manifest 更新路径 |
| Round 1 UI chrome/HUD 归属 | 一致 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:27-35`; README `:35-37` | 无 |
| 职业覆盖范围 | 一致 | `UI/PLAN.md:72-82`; PR-06 `:36-40` | frozen exclusion 需由 final coverage artifact 强制 |
| PR-07 packaged whitebox | 一致 | PR-07 `:53-71` | 无 |
| 旧 lint 与 dark-v1 coverage 边界 | 部分一致 | `UI/PLAN.md:46-58`; PR-00 `:63-65` | PR-06 仍误用 `assetLint` 作为 dark-v1 key coverage 证明 |

## 功能/系统一致性矩阵

| Surface | 文档状态 | 主要风险 |
| --- | --- | --- |
| Client shell layout | 基本可执行 | PR-01 验证充分，后续需真实补 `GameShellLayoutTest` 或等价断言 |
| UI chrome sprite pilot | 方向正确但不可直接开工 | 新 UI key 进入 manifest 时 canonical/runtime/spec coverage 未定义 |
| Equipment/inventory | 基本合理 | 需 key registry 强制 fallback/consumerTest，避免 item/HUD key 混用 |
| Profession tree UI | 边界较清楚 | PR-04 明确 stop condition，需等上游职业树合同可读后再动 |
| Map/actor/portrait replacement | 范围过大 | PR-05 需要 per-round checkpoint，否则 review/rollback 风险高 |
| Skills/status/quest full manifest | 目标清楚但 gate 语义不完整 | final-full coverage 与 earlier owner-scope coverage 没有分层 |
| Golden/whitebox polish | 明显改进 | packaged app 证据路径已明确，需消费前面 coverage artifact |

## 当前阶段必须解决的问题

1. **PR-00 必须补 manifest authority 文档**：明确 canonical manifest、runtime manifest、`syncPhase2Manifests` 和 dark-v1 sheet-plan 的关系。
2. **PR-00 必须补 `darkManifestCoverageLint` stage/scope/final 语义**：否则 PR-00/PR-02/PR-06 使用同一 task 会冲突。
3. **PR-00 必须把 dark gate 接入 `verifyChanged` impact routing**，至少在 PR-02 前成为强制 CI/preflight。
4. **PR-00 必须交付 key registry 或可审计派生视图**，并让 lint 校验 owner/fallback/consumer/test。
5. **PR-05 开工前补 per-round checkpoint 表**，避免一次性 XL 资源 PR 失控。

## Removal / Iteration Plan

| Item | Current state | Required next step |
| --- | --- | --- |
| Runtime-only manifest editing | 文档仍主要指向 runtime manifest | 改为 canonical-first，同步 runtime；写清 `syncPhase2Manifests` |
| Manual-only dark gates | README 中是追加命令 | 接入 `verifyChanged` 或等价 impact routing |
| Ambiguous coverage strictness | 同名 task 覆盖 PR-00 到 PR-06 | 增加 mode/scope/final 语义和 artifact 字段 |
| Dual fallback sheet owner | `r09-fallback-polish` = PR-06 / PR-07 | 固定 PR-06 owner，PR-07 仅返修 rejected cell |
| Prompt size ambiguity | prompt 概要仍出现 32/64 source-like wording | 改成 display-readability wording |

## Suggested Verification

本次 review 是静态文档与现有脚本/Gradle 真源核对，未运行 Gradle。修完上述文档后建议先做这些静态验证：

```bash
rg -n "assets-src/image/manifests/phase2-visual-manifest.json|client/src/main/resources/manifests/visual-manifest.json" UI/PLAN.md UI/pr
rg -n "darkManifestCoverageLint|scopeMode|ownerPr|oldStylePlayerVisibleKeys|verifyChanged" UI/PLAN.md UI/pr build.gradle.kts
rg -n "key-registry|fallbackKey|consumerTest|ownerPr" UI/PLAN.md UI/pr
rg -n "seamless 32x32|64x64 slots" UI/PLAN.md UI/pr UI/ART_STYLE_BIBLE.md
```

PR-00 实现后建议最小验证：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew tasks --all | rg "darkSpriteSheetLint|spriteSheetMapLint|darkManifestCoverageLint"
./gradlew darkSpriteSheetLint spriteSheetMapLint darkManifestCoverageLint verifyChanged
./scripts/verify-bootstrap.sh
```

PR-02 第一个新增 UI key 合入前，必须额外证明：

```bash
./gradlew syncPhase2Manifests manifestLint :client:test --tests com.ktome.client.assets.ManifestResolveTest
```

期望结果不是只跑绿，而是能解释：新增 `ui.frame.*`/`ui.hud.*` key 的 canonical manifest、runtime manifest、sheet-plan、key registry 和 dark coverage artifact 如何互相对齐。

## Final Verdict

文档已经从“方向正确但管线缺合同”推进到“主要合同基本列齐”。现在剩下的是 PR-00 必须承担的执行级收口：manifest 真源、coverage gate 分阶段语义、`verifyChanged` 接线、key registry lint。完成这些以后，PR-02 到 PR-07 的拆分才真正具备可串行实现、可审查、可回滚和可复现验证的基础。

# Dark UI/UX PR-00 Style And Pipeline Contract

**阶段**: `dark-uiux-pr00-style-and-pipeline-contract`
**优先级**: `P0`
**工作量**: `M`
**前置条件**: [UI/PLAN.md](../PLAN.md)、[UI/ART_STYLE_BIBLE.md](../ART_STYLE_BIBLE.md)、canonical `assets-src/image/manifests/phase2-visual-manifest.json` 与 runtime `client/src/main/resources/manifests/visual-manifest.json` 可读。
**资源生成结论**: 不生成正式 runtime PNG；本 PR 必须交付暗黑雪碧图管线的 schema、dry-run、root lint gate 和 style epoch 策略。

## 0. 开发治理与验收矩阵

本 PR 是 [development-governance.md](./development-governance.md) 的 dark UI/UX pipeline canary。执行前必须先通过 `acceptanceContractLint`，再进入 pipeline lint、dry-run coverage、client evidence 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI00-M01` | §3 style / prompt / sheet plan contract | `docs` / `assets` | `acceptanceContractLint` | `styleLint`, `darkSpriteSheetLint` | `UI/sprite-sheets/sheet-plan.yaml` | `N/A` |
| `UI00-M02` | §3 key registry schema | `tools` | key registry focused tests | `darkKeyRegistryLint` | `UI/sprite-sheets/key-registry.yaml` | `N/A` |
| `UI00-M03` | §4 manifest authority | `tools` | manifest resolver tests | `manifestLint`, `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=pr00-dry-run` | `assets-src/image/manifests/phase2-visual-manifest.json` | `N/A` |
| `UI00-M04` | §5 gate 接线 / verifyChanged impact | `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |
| `UI00-M05` | §8 / §9 验收与验证命令 | `docs` | `acceptanceContractLint` | `assetLint`, `styleLint`, `manifestLint` | `build/reports/verification/` | `N/A` |

### Gate Budget

预计重型任务：`darkKeyRegistryLint`、`darkSpriteSheetLint`、`spriteSheetMapLint`、`darkManifestCoverageLint`、`assetLint`、`styleLint`、`manifestLint`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-00 建立后续资源 PR 的 pipeline 和 gate 合同。

### Canonical Artifact

canonical artifact 固定为 `UI/sprite-sheets/sheet-plan.yaml`、`UI/sprite-sheets/key-registry.yaml`、canonical/runtime visual manifest、dark coverage artifact 和 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。`/Users/luo/.codex/generated_images`、dry-run 临时 PNG 和本机绝对路径不得进入合同。

### Failure Rule

PR-00 的 gate 失败必须先修 schema、registry、sheet plan 或 manifest authority；不得通过跳过 dark coverage 或把 dry-run artifact 当正式资源让 PR 通过。

## 1. 阶段目标

1. 冻结 `ktome-dark-fantasy-sprite-ui-v1` 风格合同，作为后续 PR 的唯一视觉判断入口。
2. 定义 `UI/sprite-sheets/sheet-plan.yaml` 的 schema、命名、cell mapping、切分输出和 QA report。
3. 定义 prompt 生成、Codex CLI 生图脚本交接、切图、contact sheet、manifest diff、mapping verify 的输入输出合同。
4. 选择 Round 1 中任一 `r01-*` sheet 做 dry-run 示例，但本 PR 不提交正式 runtime 资源。
5. 新增或接线 root gate：`darkKeyRegistryLint`、`darkSpriteSheetLint`、`spriteSheetMapLint`、`darkManifestCoverageLint`。
6. 固定 Codex CLI 生图交接流程：脚本生成 prompt，`scripts/codex-generate-image.py` 调用 `codex exec` 并复制最新生成图片到 `rawSheetPath`，后续脚本再切分和验证。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `UI/PLAN.md` | 只回写上游合同变化，不复制 PR 执行细节 |
| `UI/ART_STYLE_BIBLE.md` | 冻结色调、材质、像素密度、sheet prompt 禁令 |
| `UI/pr/` | PR 级执行文档 |
| `UI/sprite-sheets/` | 新增 sheet plan schema 与 dry-run 示例 |
| `assets-src/image/manifests/phase2-visual-manifest.json` | 定义 canonical manifest 接入规则；生产 key 可用 fixture 验证，不要求本 PR 迁移玩家主路径 |
| `client/src/main/resources/manifests/visual-manifest.json` | 仅作为 `syncPhase2Manifests` 生成物验证，不作为手改真源 |
| `tools` 或 `scripts` | 新增 prompt/slice/contact/verify 脚本或等价 Kotlin task |
| `build.gradle.kts` | 暴露 dark-v1 root lint gate；如改动则补 `verify-bootstrap.sh` |

## 3. 实现任务

1. 新增 `UI/sprite-sheets/sheet-plan.yaml`，至少包含 Round 1 的 3-5 个 dry-run cell。
2. schema 直接承接 [UI/PLAN.md](../PLAN.md) 的 Mapping Spec：`sheetId / round / type / styleTag / rawSheetPath / outputRoot / promptBase / grid / cells[].row / col / targetKey / category / outputName / subject / reserved / aliasOf`。
3. QA 输出必须进入 JSONL/report，不写回 plan 输入：`qaStatus / rawSheetHash / cellRect / cellHash / outputHash / reviewer / reviewedAt / rejectionReason`。
4. `size / anchor / safeMarginPx` 如需支持，只能作为 category policy 或 cell override；不能替代 sheet-level `grid`。
5. 定义 `targetKey -> outputName -> canonical manifest entry -> runtime manifest entry` 的一一映射规则，禁止靠人工猜图。
6. 写清并实现脚本或等价 task 合同：`generate_sheet_prompt.py`、`slice_spritesheet.py`、`render_contact_sheet.py`、`verify_sprite_sheet_map.py`。
7. `generate_sheet_prompt.py` 必须输出编号 prompt 文件到 `UI/sprite-sheets/prompts/dark-v1/`，并输出 `prompt-index.json`。
8. 交付 dry-run fixture：至少 1 张 Round 1 sheet、3 个非 reserved cell、1 个 reserved cell、1 个 alias cell。
9. 交付 key registry 真源：`UI/sprite-sheets/key-registry.yaml`。
10. dry-run 必须包含 1 个新增 UI key 样例，证明 `sheet-plan -> canonical manifest -> syncPhase2Manifests -> runtime manifest -> resolver/gate` 不冲突；该样例可使用 test fixture，不要求迁移正式玩家主路径。
11. `verify_sprite_sheet_map.py` 必须能在 raw PNG 缺失时输出明确的 `missingRawSheet`，不能静默跳过。
12. PR-00 必须按 §4.5 实现并冻结新增 UI key 与 `manifest-lint.py` 的桥接方式，不能把该决策留给 PR-02。

脚本路径和职责固定：

| Script | Input | Output |
| --- | --- | --- |
| `scripts/generate_sheet_prompt.py` | `UI/sprite-sheets/sheet-plan.yaml` | `UI/sprite-sheets/prompts/dark-v1/*.prompt.txt` + `prompt-index.json` |
| `scripts/codex-generate-image.py` | prompt text + `rawSheetPath` | `assets-src/image/raw/sheets/dark-v1/{sheetId}.png` |
| `scripts/verify_dark_key_registry.py` | key registry + sheet plan | lint result + optional `build/reports/dark-v1/key-registry.json` |
| `scripts/slice_spritesheet.py` | sheet plan + raw sheet | `client/src/main/resources/dark-v1/**/*.png` |
| `scripts/render_contact_sheet.py` | sheet plan + sliced PNG | `assets-src/image/contact-sheets/dark-v1/*.png` |
| `scripts/verify_sprite_sheet_map.py` | sheet plan + manifest + reports | lint result + coverage JSON/JSONL |

Key registry 最小字段：

| Field | Required rule |
| --- | --- |
| `targetKey` | 等于 canonical/runtime manifest entry key |
| `category` | 属于 asset pipeline 白名单 |
| `ownerPr` | 对应 sheet plan/key registry 中的 owner PR |
| `sheetId` | 对应 SheetId Ownership |
| `fallbackKey` | 缺图时使用的 manifest key |
| `consumer` | 主要消费文件、presenter 或 renderer |
| `consumerTest` | focused test 或 golden label |
| `aliasOf` | 仅真实复用同一图时允许 |

`darkKeyRegistryLint` 必须校验：

1. 每个非 reserved `sheet-plan.yaml` cell 的 `targetKey` 在 key registry 中存在。
2. registry 的 `ownerPr / sheetId / category` 与 sheet plan 及其生成的 ownership report 完全一致。
3. `fallbackKey` 指向 canonical manifest 中存在的 key；owner-scope 阶段允许 `missing_visual`，final-full 阶段按 coverage schema 限制。
4. `consumer` 和 `consumerTest` 非空。
5. `aliasOf` 目标存在，且不允许 alias 链式循环。
6. sheet capacity 没有超出 declared capacity。

`UI/pr/README.md` 的 SheetId Ownership 表只是人类可读视图。机器真源只来自 `sheet-plan.yaml` 与 `key-registry.yaml`；如需校验 README 展示是否过期，必须先从结构化真源生成 ownership report，再与 Markdown 做只读一致性检查，不能让脚本解析 Markdown 表格来决定资源归属。

Raw sheet 获取流程：

1. prompt 只能由 `generate_sheet_prompt.py` 从 `sheet-plan.yaml` 生成。
2. prompt 文件名固定为 `{threeDigitOrder}-{sheetId}.prompt.txt`，例如 `001-r01-ui-chrome.prompt.txt`。
3. prompt 文件头必须写出 `Prompt ID / Sheet ID / Expected output file / Canvas / Grid / Cell / Style tag`。
4. raw PNG 只能由 `scripts/codex-generate-image.py` 调用 Codex CLI 生成并复制到 `Expected output file`，也就是 `sheet-plan.yaml.rawSheetPath`。
5. `scripts/codex-generate-image.py` 固定执行 `codex exec "<prompt>" --skip-git-repo-check`，然后从本次运行创建或触碰的 `/Users/luo/.codex/generated_images` 目录选取最新图片。
6. 正式 raw 目录 `assets-src/image/raw/sheets/dark-v1/` 中每个 `sheetId` 只能存在一个 `{sheetId}.png`。
7. raw PNG 必须 repo-relative；`/Users/luo/.codex/generated_images` 只能作为 transient source，不能进入 manifest、coverage artifact、manual record 或 PR 描述的 canonical path。
8. 生成失败先重生成 raw sheet；不得通过改 `row/col` 或手工猜裁修补语义错位。
9. 所有 raw sheet 入库前必须有 contact sheet QA record。
10. 示例命令：

```bash
scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/001-r01-ui-chrome.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png \
  --smoke-report build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-chrome.json \
  --timeout-seconds 300 \
  --overwrite
```

prompt 文件头示例：

```text
Prompt ID: 001-r01-ui-chrome
Sheet ID: r01-ui-chrome
Expected output file: assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png
Canvas: 1024x1024
Grid: 4 columns x 4 rows
Cell: 256x256
Style tag: ktome-dark-fantasy-sprite-ui-v1
```

`prompt-index.json` 至少包含：

| Field | Rule |
| --- | --- |
| `promptId` | `{threeDigitOrder}-{sheetId}` |
| `promptPath` | repo-relative prompt path |
| `promptHash` | prompt 文件内容 hash |
| `sheetId` | sheet plan 中的 sheetId |
| `rawSheetPath` | expected output file |
| `round` | sheet plan round |
| `grid` | columns/rows/cell size |
| `cellCount` | 非 reserved cell 数 |

prompt 编号必须稳定：未改动 sheet 重跑生成命令时，既有 `Prompt ID` 和 `promptHash` 不得变化；新增 sheet 只能追加编号，sheet 改名或删除必须在 PR 描述中写明迁移原因和影响的 raw/contact/coverage 文件族。

## 4. Manifest Authority

1. canonical manifest 真源固定为 `assets-src/image/manifests/phase2-visual-manifest.json`。
2. runtime manifest 固定为 `client/src/main/resources/manifests/visual-manifest.json`，由 `syncPhase2Manifests` 从 canonical 同步，不能手改后作为权威。
3. `sheet-plan.yaml.outputName` 必须与 canonical/runtime manifest 的 `rawOutputPath` 完全一致。
4. 旧 `manifestLint` 继续保护 canonical/runtime key、field、styleTag、prefixRules 一致性；dark-v1 player-visible 覆盖由 `darkManifestCoverageLint` 负责。
5. PR-00 必须实现旧 `manifestLint` 与 dark-v1 的桥接：新增 `--dark-key-registry UI/sprite-sheets/key-registry.yaml` 与 `--dark-sheet-plan UI/sprite-sheets/sheet-plan.yaml` 两个参数，使 registry/sheet-plan 覆盖的 canonical key 进入 upstream spec coverage 分母，但不要求 dark-v1 sheet plan 使用旧 `EXPECTED_STYLE_TAG`。
6. dry-run 样例必须证明新增 `ui.frame.*` 或 `ui.hud.*` key 可以完成 canonical update、runtime sync、resolver 解析和 dark gate coverage，不与 `manifestLint` 打架。
7. `manifestLint` 仍必须校验 canonical/runtime 完全一致；dark registry 只能补“新增 dark-v1 key 的上游覆盖来源”，不能绕过 category、rawOutputPath、runtime asset 或 prefixRules 一致性校验。
8. build 接线必须让 `manifestLint` 依赖 `syncPhase2Manifests`，并在 PR-00 合同中写明 dark registry 参数来源。
9. dark registry 不替代 `prefixRules`。如果新增 key 是显式 canonical entry，不要求新增 prefixRules；如果新增 UI key 需要依赖运行时前缀 fallback，PR-00 必须同步更新 canonical/runtime `prefixRules` 并补 resolver 测试。

## 5. Gate 接线

PR-00 必须让后续资源 PR 有可运行的验证入口：

1. `darkKeyRegistryLint`：校验 key registry、sheet ownership、fallback、consumer/test、alias 和 capacity。
2. `darkSpriteSheetLint`：校验 sheet-plan schema、sheet type、styleTag、grid、reserved/alias、repo-relative path。
3. `spriteSheetMapLint`：校验 raw sheet 尺寸、切分输出、contact sheet、QA report、alpha bbox、hash、manifest path。
4. `darkManifestCoverageLint`：校验 player-visible key 是否切到 `dark-v1`，并输出 coverage artifact。
5. `verifyChanged` impact routing 必须在 PR-00 出口前生效，命中 `UI/sprite-sheets/**`、`assets-src/image/raw/sheets/dark-v1/**`、`assets-src/image/contact-sheets/dark-v1/**`、`client/src/main/resources/dark-v1/**`、canonical/runtime visual manifest、`assets-src/image/manifests/dark-v1-*.json`、`assets-src/image/manifests/dark-v1-*.jsonl` 时触发对应 dark gate。
6. 旧 `assetLint / styleLint / manifestLint` 继续保护旧 plan；dark-v1 不作为旧 lint 的 `--extra-plan` 静默塞入，避免旧 `EXPECTED_STYLE_TAG` 误判。
7. 脚本测试必须覆盖 dry-run fixture；可使用 `python3 -m pytest scripts/tests/test_dark_sprite_sheet_pipeline.py` 或等价 Gradle task。
8. 最小脚本测试必须覆盖：`missingRawSheet`、alias 同 sheet/跨 sheet、alias 循环拒绝、capacity 超限、prompt 编号稳定、owner-scope 缺 `ownerPr` fail fast、final-full 残留旧风格 fail fast。
9. `scripts/codex-generate-image.py` 必须有 smoke 记录，证明它能从 `/Users/luo/.codex/generated_images/<latest-session>/` 取最新图片并复制到指定输出路径；该 smoke 输出放在 `build/` 或临时目录，不作为正式资源提交。

lint 输入边界固定如下：

| Gate | Primary Inputs | 必须失败的典型场景 |
| --- | --- | --- |
| `darkKeyRegistryLint` | `key-registry.yaml`、`sheet-plan.yaml` | targetKey 缺 registry、owner/category/sheet 不一致、alias 循环、capacity 超限 |
| `darkSpriteSheetLint` | `sheet-plan.yaml`、sheet type policy | grid 越界、reserved/alias 非法、path 非 repo-relative、styleTag 不匹配 |
| `spriteSheetMapLint` | sheet plan、raw sheet、切分 PNG、contact sheet、QA report、manifest | raw 缺失、尺寸/grid/hash 不符、alpha bbox 为空、manifest rawOutputPath 不一致 |
| `darkManifestCoverageLint` | canonical/runtime manifest、key registry、coverage artifact、owner mode | owner scope 缺 key、final-full 残留旧风格、pending/rejected 玩家可见 cell |

gate 分三个 stop condition：

1. root task 可发现并能跑通 `pr00-dry-run` fixture。
2. `verifyChanged` impact routing 对 dark-v1 路径、canonical/runtime manifest 和 coverage/report 变更生效。
3. `manifestLint` 能接受由 key registry + sheet plan 覆盖的新增 UI key，并继续拒绝缺少 registry/sheet-plan 来源的 canonical key。

`darkManifestCoverageLint` 必须支持三种模式：

| Mode | 使用阶段 | 必须行为 |
| --- | --- | --- |
| `pr00-dry-run` | PR-00 | 缺 raw PNG 或生产 manifest 覆盖时输出 pending/missing，不静默成功 |
| `owner-scope` | PR-02、PR-03、PR-05 | owner scope 内必须完整；scope 外 pending 必须写入 artifact |
| `final-full` | PR-06、PR-07 | `oldStylePlayerVisibleKeys=[]` 且 `pendingOrRejectedPlayerVisibleCells=[]` |

命令协议固定为：

```bash
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=pr00-dry-run
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full
```

默认语义：

1. 裸 `darkManifestCoverageLint` 等价 `coverageMode=final-full`。
2. `owner-scope` 必须显式传 `-Pktome.darkUiux.ownerPr=PR-xx`，缺失时 fail fast。
3. `pr00-dry-run` 不允许修改玩家主路径覆盖结论，只证明管线能解释 missing/pending。
4. `verifyChanged` 触发 dark gate 时必须由 impact routing 显式选择 mode，不能依赖裸 task 默认值。

PR-00 的 `verifyChanged` impact routing 必须显式选择 `darkManifestCoveragePr00DryRun`。这是本 PR 的 canary 出口，不等价于 final-full 验收；PR-02 / PR-03 / PR-05 资源 PR 关闭时改用对应 `owner-scope`，PR-06 / PR-07 关闭时才允许把默认 dark route 收敛到 `final-full`。

Coverage artifact schema 分层：

| Layer | Fields |
| --- | --- |
| common | `schemaVersion`, `styleTag`, `scopeMode`, `ownerPr`, `expectedKeySetSource`, `strictOldStyleResidue`, `generatedAt`, `sourceManifestPath`, `runtimeManifestPath`, `keyRegistryPath`, `sheetPlanPath` |
| owner-scope | `ownerSheetIds`, `ownerExpectedKeys`, `ownerCoveredKeys`, `ownerMissingKeys`, `scopeExternalPendingKeys`, `allowedOwnerFallbackKeys` |
| final-full | `expectedKeySet`, `coveredKeySet`, `missingKeys`, `oldStylePlayerVisibleKeys`, `pendingOrRejectedPlayerVisibleCells`, `fallbackKeyUsage`, `allowedFallbackKeys`, `allowedCoverageExclusions`, `sourceSheetIds` |

`pendingOrRejectedPlayerVisibleCells` 只记录 `missing_visual` / 空输出这类待生成或被拒绝的 fallback；`oldStylePlayerVisibleKeys` 只记录仍指向非 `dark-v1/` 且非 fallback 的旧风格输出。两者在 `final-full` 中都必须为空。

## 6. Style Epoch 策略

第一阶段选择 multi-epoch sidecar validation：

1. 不改 `VisualManifestEntry` schema。
2. 混合阶段不强行用顶层 `VisualManifest.styleTag` 表达每个 entry 的风格。
3. `ktome-dark-fantasy-sprite-ui-v1` 由 `sheet-plan.yaml`、sprite report、contact sheet、coverage artifact 持有。
4. PR-06 通过 `dark-v1-manifest-coverage.json` 证明玩家可见主路径已经迁移。
5. 顶层 manifest style epoch 的 schema 升级或 bump 必须另开后续 PR，不能混进资源替换 PR。

## 7. 非目标

1. 不生成正式 PNG。
2. 不把正式玩家主路径迁移到 dark-v1 资源；允许使用 fixture 或最小样例验证 canonical/runtime 同步合同。
3. 不改 client renderer。
4. 不引入 atlas、region manifest 或 runtime sheet slicing。

## 8. 验收标准

1. `sheet-plan.yaml` 示例中所有 path 使用 repo-relative。
2. prompt 示例不含文字烘焙、编号、水印、绝对路径或 UI 文案。
3. 文档能解释清楚每个 cell 如何映射到具体 UI key。
4. dry-run 输出即使不存在正式 PNG，也能被 mapping verify 识别为未生成状态，而不是静默通过。
5. root Gradle 能发现 `darkKeyRegistryLint / darkSpriteSheetLint / spriteSheetMapLint / darkManifestCoverageLint`。
6. 旧 styleTag 与新 styleTag 的共存策略在文档和 lint 中一致。
7. key registry 真源能列出 `ownerPr / fallbackKey / consumer / consumerTest`。
8. `verifyChanged` 对 dark-v1 路径、canonical/runtime manifest 和 coverage/report 变更触发 dark gate。
9. prompt 文件和 `prompt-index.json` 可复现，重复运行生成命令不会改变未改动 sheet 的 prompt hash。
10. raw PNG 缺失、命名错误、尺寸错误或放错目录时，`spriteSheetMapLint` 必须 fail fast，并指出 expected path。
11. `scripts/codex-generate-image.py` 能执行一次 smoke，并输出 source folder、source image、repo output 和 sha256。

## 9. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=pr00-dry-run
./gradlew verifyChanged
./gradlew syncPhase2Manifests manifestLint :client:test --tests com.ktome.client.assets.ManifestResolveTest
```

若新增 dry-run 脚本或 Gradle 接线：

```bash
python3 -m pytest scripts/tests/test_dark_sprite_sheet_pipeline.py
./scripts/verify-bootstrap.sh
```

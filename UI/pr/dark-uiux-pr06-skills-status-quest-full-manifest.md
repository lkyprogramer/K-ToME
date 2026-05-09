# Dark UI/UX PR-06 Skills Status Quest Full Manifest

**阶段**: `dark-uiux-pr06-skills-status-quest-full-manifest`
**优先级**: `P1`
**工作量**: `XL`
**前置条件**: PR-03、PR-04、PR-05 完成。
**资源生成结论**: 生成 Round 8-9 与返修资源，完成玩家可见 manifest 收口。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)，并承担 dark manifest final-full 前的主体收口。执行前先跑 `acceptanceContractLint`，再跑 resource gate、status / talent focused tests、final coverage 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI06-M01` | §3 Round 8-9 resource scope | `assets` | `darkSpriteSheetLint`, `spriteSheetMapLint` | `assetLint`, `styleLint` | `assets-src/image/contact-sheets/dark-v1/` | `required` |
| `UI06-M02` | §4 职业树联动 | `client` | `TalentSidebarPresenterTest`, `InputHandlerTest` | `goldenScreenshot` | `client/build/reports/golden/` | `required` |
| `UI06-M03` | §6 full manifest 验收标准 | `tools` | `darkKeyRegistryLint`, `ManifestResolveTest` | `manifestLint`, `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full` | dark manifest coverage report | `N/A` |
| `UI06-M04` | status / quest / skill presentation | `client` | `StatusPresentationModelTest`, `StatusIconResolverTest` | `clientSmoke`, `goldenScreenshot` | `build/reports/tests/` | `required` |
| `UI06-M05` | PR-03/05 rejected cell 返修 | `assets` / `tools` | coverage artifact diff check | `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full` | dark coverage artifact | `N/A` |
| `UI06-M06` | governance inheritance | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |
| `UI06-M07` | validation overlay / pack summary | `client` | `ValidationPackSummaryTextTest`, validation smoke | `clientSmoke`, `goldenScreenshot` | `client/build/reports/golden/` | `required` |

### Gate Budget

预计重型任务：resource lint 全套、`darkManifestCoverageLint final-full`、`:client:clientSmoke`、`:client:goldenScreenshot`、`localeLint`、`contractLint`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-06 完成玩家可见 manifest 全量收口。

### Canonical Artifact

canonical artifact 固定为 Round 8-9 / rejected polish contact sheet、key registry、final-full coverage artifact、runtime manifest、client golden 和 test report。frozen 职业排除必须写入 coverage artifact，不能显示成 missing。

### Failure Rule

玩家可见 rejected cell 不得留给 PR-07 静默修补；若 final-full coverage 仍有 player-visible gap，必须回到 PR-06 或拆单独资源修复 PR。

## 1. 阶段目标

1. 替换技能、状态、damage type、quest、zone、profession/tree icon。
2. 完成 manifest fallback、debug resource、missing、hidden 资源返修。
3. 将新视觉纪元覆盖到 `visual-manifest.json` 的玩家可见资源。
4. 把 PR-04 暂时复用的职业树 icon 切换到新风格资源。
5. 将验证模式运行时 overlay、active content pack summary、scenario evidence summary 的玩家可见 presentation 纳入 dark-v1 全量覆盖；setup 页 layout 仍归 PR-01，最终证据归 PR-07。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `UI/sprite-sheets/sheet-plan.yaml` | 增加 Round 8-9 和 rejected cell 返修 |
| `client/src/main/resources/dark-v1/icons/` | skill、status、damage、quest、zone、profession、tree icon |
| `assets-src/image/manifests/phase2-visual-manifest.json` | 先将玩家可见 key 的 canonical manifest 指向 dark-v1 |
| `client/src/main/resources/manifests/visual-manifest.json` | 由 `syncPhase2Manifests` 同步生成 runtime manifest |
| `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt` | 确认 status icon key 覆盖 |
| `client/src/main/kotlin/com/ktome/client/assets/TalentAssetReferences.kt` | 确认 talent/tree icon key 覆盖 |
| `client/src/main/kotlin/com/ktome/client/render/ValidationPackSummaryText.kt` | 确认 validation overlay / active pack summary 文案 compact 且可读 |
| `client/src/main/kotlin/com/ktome/client/render/ValidationScenarioEvidenceSummaryLines.kt` | 确认 validation evidence summary 不遮挡 HUD/日志 |
| `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt` | 确认 validation scenario presentation 不引入旧风格 marker |
| `client/src/test/kotlin/com/ktome/client/ui/status/StatusIconResolverTest.kt` | 覆盖 `icon.status.*` 与 `icon.mutation.*` 双前缀解析 |
| `client/src/test/kotlin/com/ktome/client/render/ValidationPackSummaryTextTest.kt` | 覆盖 active pack summary、manifest fallback 和长文本 |

## 3. 资源范围

1. Round 8：`r08-skills-vanguard-berserker`、`r08-skills-templar-rogue`、`r08-skills-arcanist-spellblade`。
2. Round 9：`r09-status-damage`、`r09-quest-zone-profession`、`r09-fallback-debug`、`r09-rejected-polish`。
3. Round 7 剩余 item、affix、material 返修。
4. Round 2-6 中 QA rejected 的玩家可见资源返修。
5. `r09-status-damage` 同时覆盖 `icon.status.*` 和 `icon.mutation.*`；不新增 `icon_mutation` category。
6. `r09-fallback-debug` owner 固定为 PR-06，必须覆盖 `missing_visual`、hidden / resource-debug / fallback、locked/placeholder 主路径。
7. `r09-rejected-polish` owner 固定为 PR-06，只承接 PR-03/05/06 coverage artifact 已记录的 rejected cell；PR-07 只能返修这些记录，不重新拥有 sheet。
8. 当前 manifest fallback / hidden / resource-debug 主路径估算约 25-30 cell，`r09-fallback-debug` 的 64 cell 容量足够作为首选；如果开工前 inventory 盘点超过 64，必须拆出 `r09-fallback-debug-a/b` 并同步更新 `UI/PLAN.md`、`UI/pr/README.md`、sheet plan 和 key registry。

Raw sheet 生成交接：

1. 先按 PR-00 固定命令生成 Round 8-9 prompt 文件和 `prompt-index.json`。
2. 逐个执行 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --overwrite`，由脚本调用 Codex CLI 并复制最新生成图片到 `sheet-plan.yaml.rawSheetPath` 指定位置。
3. 文件名必须等于 `{sheetId}.png`，例如 `r08-skills-vanguard-berserker.png`、`r09-fallback-debug.png`、`r09-rejected-polish.png`。
4. Round 7、Round 2-6 rejected cell 返修也必须走同一 prompt 文件和 raw path 机制，不允许直接替换切分后的单 PNG。
5. PR 描述必须列出 prompt path、raw sheet path、raw sheet hash、Codex CLI transient source folder/source image 摘要、coverage artifact 和 contact sheet QA path。

职业范围：

1. Release playable：`vanguard / arcanist / rogue / templar` 全量切换。
2. Dev playable/report-only：`berserker / spellblade` 全量切换。
3. Frozen excluded：`shadowblade / warden` 不生成完整 skill/talent sheet；若锁定职业卡在 UI 可见，必须使用 dark-v1 locked/fallback 表达，并写入 coverage exclusion。

## 4. 职业树联动

1. `TalentSidebarPresenter` 的 node icon、tree icon 必须在本 PR 切换到新 skill/tree icon。
2. 不改 PR-04 的输入语义和 presentation authority。
3. 如果某个 talent 缺 icon，必须通过 `sheet-plan.yaml` 补齐，不能在 renderer 里硬编码 fallback path。
4. 职业树相关 icon 必须覆盖 learned、learnable、locked、active 四态下的可读性。
5. `icon.tree.*` 是职业树 section/header 小图标，`tree.*` 是 portrait/large visual；两套 key 保留，不在本 PR 合并。PR-06 必须分别列出切换表。

## 5. 非目标

1. 不改技能效果、状态结算、damage type 枚举或 quest 规则。
2. 不改音频资源，除非现有 lint 强制要求同步 manifest；如触发，必须补 `audioLint`。
3. 不把旧资源删干净作为本 PR 目标；可保留历史/debug resource fallback，但玩家主路径必须指向新风格。
4. 不改 validation scenario 选择、content pack 加载、scenario bootstrap 或 whitebox materialization 规则；只改 overlay / summary 的 presentation 与资源覆盖。

## 6. 验收标准

1. `visual-manifest.json` 玩家可见主路径不再指向旧风格资源，允许仅保留历史/debug resource fallback。
2. `darkManifestCoverageLint + ManifestResolveTest` 证明 `icon.skill.*`、`icon.tree.*`、`icon.status.*`、`icon.quest.*` 全部可解析并已进入 dark-v1 玩家主路径。
3. contact sheet QA report 没有 `pending` 或 `rejected` 的玩家可见资源。
4. 职业树、HUD、背包、状态栏同时出现时，图标风格一致，不出现明显跨时代资源。
5. 产出 `assets-src/image/manifests/dark-v1-manifest-coverage.json`。
6. `assetLint / styleLint / manifestLint` 只作为旧资源合同和 canonical/runtime 一致性回归门禁，不作为 dark-v1 覆盖权威。
7. 验证模式 overlay / active pack summary 在 debug client 与 PR-07 packaged app 场景中不遮挡地图、HUD、日志或任务；content pack、scenario、evidence summary 不回退到旧风格 marker。

Coverage artifact 要求：

PR-06 使用 PR-00 固定的 final-full schema，不在本 PR 自定义第二套字段。final-full 至少要求 `expectedKeySet`、`coveredKeySet`、`missingKeys`、`oldStylePlayerVisibleKeys`、`pendingOrRejectedPlayerVisibleCells`、`fallbackKeyUsage`、`allowedFallbackKeys`、`allowedCoverageExclusions`、`sourceSheetIds`。

Fallback/debug final inventory 必须在开工前生成并随 PR 提交：

| Inventory bucket | Required action | Target sheet |
| --- | --- | --- |
| `missing_visual` | 生成 dark-v1 missing/fallback 主视觉 | `r09-fallback-debug` |
| `tile.hidden` / hidden entrance / secret zone | 生成 hidden/secret readable icon or portrait | `r09-fallback-debug` 或对应 zone sheet |
| `category=debug` player-visible starter/item visuals | 若玩家可见则迁移；纯 debug/history 可列入 allowed fallback | `r09-fallback-debug` 或 Round 7 owner sheet |
| debug-only tiles / missing visual sentinel | 不允许 client ASCII renderer 或 ASCII manifest 字段；纯 debug/history resource fallback 可列入 `allowedFallbackKeys` 并说明原因 | `r09-fallback-debug` 或 allowed fallback |
| PR-03/05 rejected cells | 只处理 coverage artifact 中列明的 rejected cell | `r09-rejected-polish` |

如果 final inventory 中 manifest fallback / debug resource / hidden / rejected cell 总数超过当前 sheet capacity，PR-06 必须先新增 `r09-fallback-debug-*` 或 `r09-rejected-polish-*` sheetId，并同步更新 `UI/PLAN.md`、`UI/pr/README.md` 和 key registry；不得把超额 key 留作 silent pending。
PR-06 close 前玩家可见 rejected cell 必须为 0；PR-07 只能处理 PR-06 coverage artifact 中记录的非玩家可见 polish 项或后验发现的单点问题。若 PR-07 发现玩家可见 rejected cell，视为 PR-06 未完成，不能在 PR-07 静默修补。

Manifest key 切换表必须至少包含：

| Key family | Required action |
| --- | --- |
| `icon.skill.*` / `talent.*.icon` | release/dev playable 全量切到 dark-v1 |
| `talent.*.visual` | release/dev playable 全量切到 dark-v1 或列入 explicit pending |
| `icon.tree.*` | tree header 小图标切到 dark-v1 |
| `tree.*` | tree portrait 切到 dark-v1 |
| `icon.status.*` / `icon.mutation.*` | 全量切到 dark-v1 |
| `icon.quest.*` / zone/profession/tree/difficulty icon | 全量切到 dark-v1 |
| `missing_visual` / hidden / debug resource fallback | Round 9 polish 或 allowed fallback 说明 |

## 7. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint :client:test --tests com.ktome.client.ui.status.StatusPresentationModelTest --tests com.ktome.client.ui.status.StatusIconResolverTest --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.ValidationPackSummaryTextTest --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint verifyChanged
./gradlew syncPhase2Manifests manifestLint
```

本 PR 的 dark gate 必须使用显式 final-full 命令：

```bash
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full
```

要求 `oldStylePlayerVisibleKeys=[]` 且 `pendingOrRejectedPlayerVisibleCells=[]`。不得用 owner-scope 代替本 PR close gate。

## 8. 人工白盒

1. 打开主 HUD、背包、状态栏、职业树、任务提示，确认 icon 风格统一。
2. 触发至少 3 类状态和 2 类技能预览，确认状态/技能图标不混淆。
3. 故意注入一个 missing key 的测试路径，确认 manifest fallback 风格可接受且 report 可定位。
4. 打开 validation overlay / active pack summary，确认 scenario、pack、evidence summary 可读且不遮挡 HUD/日志。
5. 必填证据：skill/status/quest/profession tree 同屏截图、manifest coverage artifact、fallback injection record、`dark-uiux-pr07-validation-overlay` 的 PR-07 evidence 引用。

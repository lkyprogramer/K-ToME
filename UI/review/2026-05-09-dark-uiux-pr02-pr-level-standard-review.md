# Dark UI/UX PR-02 PR 级 Review 报告

目标文档：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`

Review 标准：`docs/review/rule/pr-level-review-standard.md`

参考报告格式：`UI/review/2026-05-09-dark-uiux-pr01-1-pr-level-standard-rereview-round2.md`

审查时间：2026-05-09

审查范围：

- 上游入口：`docs/INDEX.md`、`AGENTS.md`、`UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`UI/pr/screen-coverage-matrix.md`
- 目标 PR：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`
- 当前代码 / 结构化真源锚点：`UI/sprite-sheets/sheet-plan.yaml`、`UI/sprite-sheets/key-registry.yaml`、`assets-src/image/manifests/phase2-visual-manifest.json`、`client/src/main/resources/manifests/visual-manifest.json`、`scripts/verify_dark_manifest_coverage.py`、`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt`、`client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
- 本轮重点：PR-02 文档是否满足 PR 级 review standard 的可开发合同要求，尤其是 owner-scope coverage、resource registry / sheet plan / manifest / client consumer / golden / whitebox 证据链是否可证伪

预检摘要：

- 当前分支：`codex/dark-uiux-pr01-client-shell-layout`
- 工作树状态：存在大量 PR-01/PR-01-1 相关 in-progress 改动；目标文档 `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md` 自身也有改动。本报告按“审查当前工作树版本的 PR-02 文档”处理，不把其它 in-progress diff 当作 PR-02 已实现证据。
- 命中的 PR 系列：`UI/pr` dark UI/UX，执行顺序为 `PR-00 -> PR-01 -> PR-01-1 -> PR-02 -> PR-03 -> PR-04 -> PR-05 -> PR-06 -> PR-07`。
- 受影响模块：`assets`、`tools`、`client`、`docs`。
- 稳定合同：visual manifest、key registry、sheet plan、dark-v1 coverage artifact、golden / whitebox evidence、client manifest consumer。
- 主要风险：owner-scope 空集通过、client 继续只画 token 色块、runtime manifest stale evidence、manual-only 或 label-only evidence。

## Findings

### P0

无。

### P1

#### P1-1 PR-02 owner-scope gate 现在可以在 `ownerExpectedKeys=[]` 时通过

证据：

- PR-02 把 owner-scope coverage 作为 close gate：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:19`、`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:144-151`
- PR-02 目标是 Round 1 三张 sheet：`r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:38-41`
- README 要求 PR-02 覆盖 Round 1 三张 sheet 的全部非 reserved cell，含首页 / 验证 / 结算 / error / loading 共享 key：`UI/pr/README.md:97-100`
- 当前结构化真源仍只有 PR-00 dry-run 小集合，且 registry entry 均为 `ownerPr: PR-00`：`UI/sprite-sheets/key-registry.yaml:4-31`
- 当前 sheet plan 也只有 `r01-ui-chrome` 的 4 个非 reserved / dry-run cell，没有 `r01-ui-controls` 与 `r01-ui-hud-icons`：`UI/sprite-sheets/sheet-plan.yaml:6-47`
- 实跑命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02
```

结果：`BUILD SUCCESSFUL`，但报告内容为 `ownerExpectedKeys=[]`、`ownerCoveredKeys=[]`、`status=PASS`。

问题：

PR-02 文档把 owner-scope coverage 当成 close gate，但当前 gate 对“目标 owner 没有任何 key”没有 fail fast。开发者可以在没有 PR-02 registry entry、没有三张 Round 1 sheet、没有 `ui.hud.*` / `ui.screen.*` / `ui.control.*` 的情况下拿到 green coverage。

影响：

- PR-02 可能被误判为“owner-scope 资源闭环已完成”，实际只是没有分母。
- 后续 PR-03 / PR-04 / PR-07 会继承错误基础，认为 `ui.frame.*`、`ui.hud.*`、`ui.screen.*` 已可消费。
- 这违反 PR 级 review standard 对 canonical owner evidence 的要求：canonical owner evidence 一旦缺字段或缺 artifact 必须 fail fast，不能 default-success。

修复方向：

PR-02 文档必须增加“从 PR-00 dry-run 到 PR-02 formal owner”的硬迁移条款，并让 close gate 可证伪：

1. PR-02 开始时，必须把 Round 1 formal key 的 `ownerPr` 写为 `PR-02`，或明确保留 `PR-00` dry-run fixture 的同时新增 PR-02 formal entries。
2. PR-02 owner-scope coverage 必须断言 `ownerExpectedKeys` 非空，且 `ownerSheetIds` 至少包含 `r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons`。
3. close gate 不允许只依赖裸 task 结果；PR 描述或 manual record 必须摘录 coverage artifact 中 `ownerExpectedKeys`、`ownerCoveredKeys`、`ownerMissingKeys`、`scopeExternalPendingKeys`。
4. 建议扩展 `verify_dark_manifest_coverage.py`：`owner-scope` 下 `expected_keys` 为空时 fail fast，错误信息包含 `ownerPr` 和 `expectedKeySetSource`。

推荐测试 / gate：

- `DarkSpriteSheetPipelineScriptTest.ownerScopeCoverageFailsWhenOwnerExpectedKeysEmpty`
- `DarkSpriteSheetPipelineScriptTest.ownerScopeCoverageRequiresExpectedOwnerSheetIds`
- `./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02`

#### P1-2 Client 消费合同过泛，当前代码仍可只画 token 色块而不消费新 chrome key

证据：

- PR-02 要求 client 实际消费 panel / slot / modal / HUD 最小可运行子集：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:38-41`
- PR-02 影响范围只写 `client renderer` 与 `client standalone screens`，没有列具体消费类、方法、测试断言：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:54-55`
- 当前 `TileRenderer` shell / HUD 主体仍通过 `drawRect` 画 token 色块：`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:247-281`、`client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt:370-373`
- 当前 `StandaloneScreenChrome` 通过 `whitePixel` 绘制 panel / outline：`client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt:46-63`、`client/src/main/kotlin/com/ktome/client/screen/StandaloneScreenLayout.kt:70-102`
- 当前 manifest resolver test 只覆盖 `ui.frame.panel.body` / `ui.frame.panel.focus` dry-run entry：`client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt:49-61`

问题：

文档的 client 消费要求停留在“renderer / standalone screens 消费新 key”这一层，未冻结具体 consumer path 和可证伪断言。实现者可以只做到 manifest key 可解析或 golden hash 更新，但 UI 主路径仍然画 token 色块，不使用 `ui.frame.*`、`ui.screen.*`、`ui.hud.*` 资源。

影响：

- PR-02 的核心体验目标会被削弱为“manifest 中有 key”，玩家实际仍看不到 dark UI chrome 资源。
- `goldenScreenshot` 只证明截图稳定，不一定证明 panel / slot / modal / HUD 来自新 sheet。
- 后续 PR-03 / PR-04 复用 slot、modal、tooltip frame 时，可能继续建立在 token 色块路径上，形成资源合同与 runtime 消费的第二真源。

修复方向：

PR-02 文档应补一节 `Client Consumer Contract`，至少冻结：

1. `TileRenderer` 或其子组件必须通过 `VisualManifestResolver` 解析并 `drawAsset` 消费 `ui.frame.panel.body`、`ui.frame.slot.empty`、`ui.frame.modal.body`、至少一个 `ui.hud.*`。
2. `StandaloneScreenChrome` 必须消费 `ui.screen.home.title_frame` / `ui.screen.home.action_frame` 或通过 `ui.frame.*` alias 消费共享 chrome；不能只画 `whitePixel` panel。
3. `TileRendererCanvasTest` / standalone screen focused test 必须用 recording canvas 或 test texture repository 断言这些 resolved key 被绘制，而不是只断言文本位置或颜色。
4. 如果某些 screen 在 PR-02 不进入 golden，必须在 PR-07 evidence index 中列具体 owner label 和剩余风险；但 PR-02 至少要有 focused resolver / drawAsset 证据。

推荐测试：

- `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets`
- `StandaloneScreenLayoutTest.darkUiuxPr02StandaloneChromeConsumesManifestKeys`
- `ManifestResolveTest.darkUiuxPr02Round1OwnerKeysResolveThroughExactEntries`

#### P1-3 验证顺序把 `syncPhase2Manifests` 放在主 close command 之后

证据：

- PR-02 主验证命令先运行 `manifestLint`、`ManifestResolveTest`、`:client:clientSmoke`、`:client:goldenScreenshot`、`verifyChanged`，之后才运行 `syncPhase2Manifests manifestLint`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:137-142`
- README 固定 manifest authority 链路：`key-registry -> sheet-plan -> canonical manifest -> syncPhase2Manifests -> runtime manifest -> resolver/test -> dark coverage artifact`：`UI/pr/README.md:133-141`
- PR-02 验收要求 canonical/runtime `visual-manifest.json` 的 `rawOutputPath` 与 `sheet-plan.yaml.outputName` 完全一致：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:128-133`
- 本轮执行 `acceptanceContractLint` 时，Gradle 先执行了 `syncPhase2Manifests`，并实际修改 / 同步了 runtime manifest；说明 runtime manifest 是可漂移产物，需要明确前置 sync。

问题：

如果先跑 resolver、golden、coverage，再跑 `syncPhase2Manifests`，前面的 evidence 可能读取 stale runtime manifest。最后的 `syncPhase2Manifests manifestLint` 即使通过，也不能证明前面 `ManifestResolveTest`、client evidence 和 owner-scope coverage 使用的是同批 canonical/runtime manifest。

影响：

- `ManifestResolveTest` 可能验证的是旧 runtime manifest。
- `goldenScreenshot` 可能捕获旧资源路径。
- `darkManifestCoverageLint` 同时读 canonical/runtime manifest，如果 runtime 副本未同步，会出现假失败或假通过。

修复方向：

调整 PR-02 §7 验证顺序：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew syncPhase2Manifests manifestLint
./gradlew assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02
./gradlew :client:clientSmoke :client:goldenScreenshot verifyChanged
```

如果为了耗时合并成单条命令，也必须确保 `syncPhase2Manifests` 在 resolver / client evidence / `verifyChanged` 前完成。

推荐测试 / gate：

- `acceptanceContractLint` 增加可选规则：资源 PR 的 `syncPhase2Manifests` 不得出现在 resolver / golden / coverage 之后。
- `./gradlew syncPhase2Manifests manifestLint :client:test --tests com.ktome.client.assets.ManifestResolveTest`

### P2

#### P2-1 §4 key 表把多个 public key 合并成一行，且没有逐 key 冻结 `row/col/outputName/aliasOf`

证据：

- §4 表中 `ui.frame.panel.corner_tl / tr / bl / br` 合并一行，`ui.frame.panel.edge_top / right / bottom / left` 也合并一行：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:76-85`
- §3 要求每个 cell 必须在 `sheet-plan.yaml` 写明 `targetKey` 和 `outputName`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:63`
- README 的 key registry contract 要求每个 `targetKey` 有 `ownerPr / sheetId / fallbackKey / consumer / consumerTest / aliasOf` 等字段：`UI/pr/README.md:114-131`

问题：

表格对人类可读，但不是可直接落地的 cell contract。开发者仍需要自行决定：

1. corner / edge 的具体 row/col。
2. 每个 key 的 `outputName`。
3. 是否 alias 到 `ui.frame.panel.body`，还是生成独立 cell。
4. `ui.screen.*` 是否是独立资源，还是 alias 到 `ui.frame.*` / `ui.control.*`。

影响：

- sheet-plan 与 key-registry 的实现存在多种“看似符合文档”的版本。
- contact sheet QA 无法机械判断漏 cell 还是允许 alias。
- 后续 PR-03 / PR-04 引用 slot / modal / tooltip frame 时，可能遇到 key 存在但资源并非预期语义。

修复方向：

在 PR-02 文档新增 `Round 1 Cell Contract` 表，逐 key 列出：

| sheetId | row | col | targetKey | outputName | fallbackKey | aliasOf | consumerTest |
| --- | --- | --- | --- | --- | --- | --- | --- |

如果不想在文档重复完整 `sheet-plan.yaml`，至少列出每个 sheet 的 required key set 和 alias policy，并要求 `darkSpriteSheetLint` / `darkKeyRegistryLint` 输出的 report 在 PR 描述中摘录。

#### P2-2 白盒证据只有 label，没有 scenario steps、expected evidence、skip rule 或 manual record path

证据：

- PR-02 白盒证据列了 `dark-uiux-pr02-round1-chrome`、`dark-uiux-pr02-hud-icons-pilot`、`dark-uiux-pr02-standalone-screen-chrome` 三个 label：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:153-158`
- governance 要求 player-visible UI 改动不能只靠 manual record，必须有 golden / clientSmoke / focused test；`whitebox=skipped` 必须写原因、替代证据和剩余风险：`UI/pr/development-governance.md:32-37`
- screen coverage matrix 要求 PR-02 standalone screen chrome 覆盖首页、验证 setup、结算、错误页共享 chrome/key 消费：`UI/pr/screen-coverage-matrix.md:66`

问题：

当前白盒段落没有说明：

1. 如何进入对应 screen / state。
2. 需要截图还是 manual record。
3. expected evidence 是 resolved key、drawn asset、contact sheet QA，还是 visual inspection。
4. 若某些 screen 不进入 PR-02 golden，如何记录 skip reason 与 PR-07 继承。

影响：

- reviewer 无法根据白盒记录判断 PR-02 是否真的让 chrome / HUD / standalone screen 消费新资源。
- PR-07 evidence index 可能只继承 label，不知道前置 PR-02 哪些 screen 已覆盖、哪些只是 pending。

修复方向：

将 §8 改为白盒 evidence matrix，例如：

| label | scenario / steps | expected evidence | artifact | skip rule |
| --- | --- | --- | --- | --- |
| `dark-uiux-pr02-round1-chrome` | 启动固定 seed 局内 shell，打开 tooltip/modal | screenshot + recording canvas shows `ui.frame.panel.body`, `ui.frame.slot.empty`, `ui.frame.modal.body` | `client/build/reports/golden/...` + manual record | 不允许 skip |
| `dark-uiux-pr02-hud-icons-pilot` | 固定 snapshot 下 HUD 显示 HP / stamina / XP / gold | 至少两个 `ui.hud.*` resolved key from `r01-ui-hud-icons` | golden + resolver test | 如果 gold/key 未进入当前 snapshot，写 PR-07 pending |
| `dark-uiux-pr02-standalone-screen-chrome` | 首页或 validation setup | visible screen chrome/control key | screenshot/manual | 未覆盖 outcome/error 时写 PR-07 evidence index |

#### P2-3 Gate Budget 缺 resource / manifest / golden freshness 与最近耗时来源

证据：

- PR-02 Gate Budget 只列重型任务和触发原因：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:24-26`
- governance 要求每个 PR 必须声明预计触发重型任务、触发原因、resource / manifest / golden freshness、最近耗时来源或 `build/verification/verify-changed/full-task-duration-summary.{json,md}` 的读取方式：`UI/pr/development-governance.md:58-65`

问题：

PR-02 是首个正式资源 PR，freshness 要求比 PR-01 更关键。当前文档没有写清：

1. raw sheet、sliced PNG、contact sheet、manifest、coverage artifact 是否必须同批生成。
2. golden 是否必须在最新 synced runtime manifest 后刷新。
3. 读取最近耗时的路径和超时复盘触发方式。

影响：

- 同一批资源可能混入 stale contact sheet 或 stale runtime manifest。
- `goldenScreenshot` 被当作调试循环反复跑，违背 governance 的 gate budget 纪律。
- reviewer 无法判断 PR 描述中的 evidence 是否同批。

修复方向：

在 Gate Budget 下补：

1. freshness：`raw sheet hash / sprite map report / contact sheet QA / canonical manifest / runtime manifest / owner-scope coverage / golden output` 必须来自同一批次；PR 描述列 hash 和 artifact path。
2. duration source：PR close 前读取 `build/verification/verify-changed/full-task-duration-summary.{json,md}`；若不存在，声明本轮首次建立 baseline。
3. retry rule：同一 resource gate 失败超过 2 次时，先补 `verify_sprite_sheet_map.py` / registry lint focused assertion，再重跑 full resource gate。

### P3

#### P3-1 PR-02 §7 主命令未显式包含 owner-scope coverage

证据：

- 主命令包含 `assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint :client:test ... verifyChanged`，未包含显式 owner-scope `darkManifestCoverageLint`：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:137-141`
- 下方单独说明必须使用显式 owner-scope 命令：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:144-151`

问题：

文档语义最终是正确的，但命令块会让执行者先复制主命令，漏掉真正的 PR-02 close gate。

修复方向：

把 owner-scope coverage 放进 close command 的主流程，或将 §7 分成 `fast lane`、`resource gate`、`client evidence`、`final closure` 四段，避免“主命令”和“必须命令”分离。

#### P3-2 Canonical Artifact 描述仍有泛称，不利于 PR 描述机械摘录

证据：

- Canonical Artifact 使用 “sheet plan、key registry、contact sheet QA、canonical/runtime visual manifest、owner-scope coverage report 和 golden output” 的泛称：`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:28-30`
- governance 已列出 canonical artifact 具体路径族：`UI/pr/development-governance.md:73-84`

问题：

泛称不会阻塞开发，但 PR 描述容易漏路径。PR-02 是资源 PR，建议列出 exact artifact path pattern。

修复方向：

补充路径：

- `UI/sprite-sheets/sheet-plan.yaml`
- `UI/sprite-sheets/key-registry.yaml`
- `assets-src/image/raw/sheets/dark-v1/r01-ui-*.png`
- `assets-src/image/contact-sheets/dark-v1/r01-ui-*-contact.png`
- `assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl` 或 PR-02 专属 report path
- `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`
- `client/build/reports/golden/`

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| PR-02 生成 Round 1 最小可运行组 `r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons` | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:7`、`UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:57-64`；当前 `sheet-plan.yaml` 只有 `r01-ui-chrome` dry-run cells | 部分一致 |
| PR-02 owner-scope coverage 证明当前 owner scope 完整 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:144-151`；实跑结果 `ownerExpectedKeys=[]` 仍 PASS | 不一致 |
| 新 key 必须进入 key registry / sheet plan / canonical manifest / runtime manifest / resolver / coverage artifact | `UI/pr/README.md:133-141`；当前 registry owner 仍为 PR-00，manifest 只有 `ui.frame.panel.body/focus` dry-run | 部分一致 |
| Client 实际消费 panel / slot / modal / HUD key | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:38-41`；当前 `TileRenderer` 与 `StandaloneScreenChrome` 仍可只画 token 色块 | 部分一致 |
| canonical/runtime manifest `rawOutputPath` 与 `sheet-plan.yaml.outputName` 一致 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:128-130`；文档命令顺序把 sync 放在 evidence 后 | 部分一致 |
| 白盒证据能验证 player-facing UI | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:153-158`；缺 steps / expected evidence / skip rule | 部分一致 |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前代码/文档状态 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |
| Resource registry | PR-02 owns Round 1 UI chrome / HUD / controls / standalone screen keys | 当前 registry 只有 PR-00 dry-run entries | owner-scope 分母为空仍 PASS | P1 |
| Sheet plan | PR-02 增加 Round 1 正式 cell，三张 sheet | 当前 sheet-plan 只有 `r01-ui-chrome` 少量 dry-run cell | 文档未冻结 formal migration / required owner key set | P1 |
| Manifest authority | canonical -> sync -> runtime -> resolver/test -> coverage | 文档验证顺序把 sync 放在 evidence 后 | stale runtime manifest 风险 | P1 |
| Client renderer | 主路径消费 panel / slot / modal / HUD assets | 当前消费点和 drawAsset 断言未冻结，代码仍可 drawRect | player-facing 效果不可证伪 | P1 |
| Standalone screens | 首页 / 验证 setup / 结算 / 错误页消费共享 chrome/control key | 文档只写 screen 列表，没有 exact key draw path | screen chrome evidence 过泛 | P2 |
| White-box / golden | 三个 label 证明 chrome / HUD / standalone screen | 只有 label，没有步骤、期望、skip rule | evidence 可诊断性不足 | P2 |
| Gate Budget | 声明 heavy tasks、freshness、duration source | 只列 heavy tasks 和原因 | governance 字段缺失 | P2 |

## 玩法与体验审查

### 核心循环

PR-02 的体验价值是把 PR-01/PR-01-1 的 layout / viewport foundation 升级为“真正的暗黑 UI chrome 资源语言”。如果 P1-1 和 P1-2 不修，玩家看到的仍可能是 token 色块或 `missing_visual`，而不是 Round 1 chrome / HUD resources。这个问题不能推给 PR-07，因为 PR-07 只能收口证据和 polish，不应首次发现 PR-02 资源未进入主路径。

### 战斗体验

PR-02 提前列了 `ui.combat.*`、`ui.control.*`、`ui.state.*`，但当前文档没有冻结哪些属于 PR-02 formal owner、哪些只是为后续 PR-05/06 提前占位。若不明确，combat decision UI 后续可能复用 PR-00 dry-run 或 old-style `phase4/uiux_pr05` 资源，造成战斗行动提示风格断层。

### 成长与构筑驱动

`ui.state.locked/learnable/active/reserve` 会影响职业树可读性，但 PR-02 同时又声明不生成技能、装备、怪物、地图 tile 资源。文档需要明确这些 talent state glyph 是否是 PR-02 formal owner，还是 PR-04/06 承接；否则职业树状态 icon 会跨 PR owner 漂移。

### 新手体验与信息反馈

首页、验证 setup、错误页、loading state 是新手最先看到的 UI。当前文档要求这些 screen 可解析共享 chrome/control key，但缺少可见性步骤；只验证 resolver 不能证明玩家真正看到统一 chrome。

### 系统耦合与体验断层

PR-02 横跨 `assets / tools / client / docs`。当前最危险的耦合点是 tools coverage 分母来自 key registry，而 key registry 仍可不包含 PR-02 key。这个 coupling 必须在 gate 层 fail fast，不能靠人工记忆检查。

## 当前阶段必须解决的问题

1. P1-1 必须当前 PR 修。owner-scope 空集通过会让 PR-02 的 canonical owner evidence 失效，属于 close gate 级问题。
2. P1-2 必须当前 PR 修。PR-02 是第一个正式资源 PR，必须证明资源进入 player-facing 主路径，而不是只进入 manifest。
3. P1-3 必须当前 PR 修。manifest sync 顺序决定所有 resolver / golden / coverage evidence 是否同批可信。
4. P2-1 建议当前 PR 修。逐 key / cell / output contract 是资源 PR 可开发性的基本前提。
5. P2-2、P2-3 建议当前 PR 修。白盒和 Gate Budget 都是 governance 明确字段，修文档成本低，能减少实现阶段返工。

## Removal/Iteration Plan

本轮没有建议删除生产代码。

需要在 PR-02 文档中补充一个 staged migration / removal plan，避免 PR-00 dry-run path 被误当成 PR-02 formal resource：

| Item | Details |
| --- | --- |
| Location | `UI/sprite-sheets/key-registry.yaml`、`UI/sprite-sheets/sheet-plan.yaml`、`assets-src/image/manifests/phase2-visual-manifest.json` |
| Current state | PR-00 dry-run entries 使用 `ownerPr: PR-00`，部分 `rawOutputPath=debug/missing_visual.png` |
| Target state | PR-02 formal entries 使用 `ownerPr: PR-02`，Round 1 owner-scope key set 非空并可解析 / 可绘制 |
| Preconditions | PR-02 raw sheet、sliced PNG、contact sheet QA、manifest sync、coverage artifact 同批通过 |
| Deletion or iteration steps | 1. 保留 PR-00 dry-run fixture 只用于 pipeline proof 2. 新增或迁移 PR-02 formal owner entries 3. coverage artifact 明确 PR-00 dry-run 与 PR-02 formal owner 的边界 4. PR-06 final-full 替换玩家主路径 `missing_visual` fallback |
| Affected gates | `darkKeyRegistryLint`、`darkSpriteSheetLint`、`spriteSheetMapLint`、`darkManifestCoverageLint owner-scope`、`ManifestResolveTest`、`:client:goldenScreenshot` |
| White-box check | PR-02 manual/golden evidence 必须显示至少一个 chrome、至少两个 HUD/icon、至少一个 standalone screen chrome/control key |
| Rollback | 回滚 `dark-v1/ui` runtime PNG、Round 1 sheet-plan cells、PR-02 registry entries、canonical/runtime manifest entries 和 renderer consumption points |

## Additional Suggestions

- `ui.frame.panel.focus` 已在当前 PR-00 dry-run 真源里出现，但 PR-02 §4 没列。建议决定它是 PR-01/PR-00 dry-run test-only，还是 PR-02 formal key；不要让 manifest 存在、PR-02 表却缺席。
- PR-02 §4 的 `ui.combat.*` 如果只是后续 PR-05/06 的 preview key，建议写明 `crossPrDependency`，否则它们看起来像 PR-02 必须完成的 formal owner scope。
- 如果 PR-02 不想一次性列满 `r01-ui-controls` 64 cell，可以把 reserved / pending cell 写入 `sheet-plan.yaml` 并要求 coverage artifact 列 `scopeExternalPendingKeys`；不要在文档表里省略后再靠实现判断。

## Open Questions

无需要阻塞用户确认的问题。推荐默认判断：

- PR-02 应该接管 Round 1 formal owner key，`ownerPr` 使用 `PR-02`。
- PR-00 dry-run entries 只能保留为 pipeline proof，不应作为 PR-02 close gate 分母。
- owner-scope 空集通过应视为 gate defect，而不是“当前还没开始生成资源”的可接受状态。

## Suggested Verification

本轮 review 已执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：

- `acceptanceContractLint` 通过，输出 `BUILD SUCCESSFUL`。

本轮还执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02
```

结果：

- `darkManifestCoverageLint` 通过，但报告显示 `ownerExpectedKeys=[]`、`ownerCoveredKeys=[]`、`ownerMissingKeys=[]`、`status=PASS`。该结果是 P1-1 的直接证据，不应作为 PR-02 合格证明。

本轮文档自检已执行：

```bash
git diff --check -- UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md docs/review/rule/pr-level-review-standard.md
for f in UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md docs/review/rule/pr-level-review-standard.md; do awk 'BEGIN{c=0} /^```/{c++} END{print FILENAME ":FENCE_OPEN=" c%2}' "$f"; done
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n --type md "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md docs/review/rule/pr-level-review-standard.md
```

结果：

- `git diff --check` 无输出。
- 两个文档 fenced code block 均为 `FENCE_OPEN=0`。
- 目标文档与 review standard 没有命中文档中的本机绝对路径模式。

修复本报告 findings 后建议重跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew syncPhase2Manifests manifestLint
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest
```

进入实现后还必须按 PR-02 文档补跑 `:client:clientSmoke`、`:client:goldenScreenshot` 和最终 `verifyChanged`。本轮未运行这些实现级 gate，不声明通过。

## Summary

PR-02 文档已经具备 Acceptance Matrix、Gate Budget、canonical artifact 和 failure rule 的基本结构，`acceptanceContractLint` 也能通过。但按 `docs/review/rule/pr-level-review-standard.md` 的可开发文档标准，它仍不能直接进入实现：最核心的问题是 PR-02 owner-scope coverage 当前可以在 0 个 owner key 下通过，client 消费合同也没有冻结到具体 drawAsset / focused test 断言，manifest sync 还被放在 evidence 之后。

这三个 P1 必须先修，否则 PR-02 会把“资源已进入主路径”变成不可证伪声明，后续 dark UI/UX PR 会在错误基础上继续扩张。

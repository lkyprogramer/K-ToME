> 执行前必须先完整阅读并接受：
> `docs/INDEX.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md`
> `docs/phase4/2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part1.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part4.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md`

# Phase4 v4 PR-07 Sample Pack ADD-first Visibility

**阶段**: `Phase 4 completion hardening / phase4-v4-pr07`
**优先级**: `P2`
**工作量**: `S`
**合并来源**: v4 P2-1、P2-2
**前置条件**: PR-00、PR-04 已完成；content pack loader、contentPackHarness、whiteBoxContentPack 已存在；`secretZoneSelector.primarySlot / secondarySlot` 已进入 hidden mapgen pipeline
**资源生成结论**: 不生成图片资源；不生成音频资源

## 0. 开发治理与验收矩阵

本 PR 串行依赖 PR-04 的 `secretZoneSelector.primarySlot / secondarySlot`。执行规则见 [development-governance.md](./development-governance.md)，通用验证阶梯见 [docs/verification/README.md](../../../verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../../rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `PR07-M01` | §5.1 manifest ADD-first | `game` | `ContentPackRuntimeResolverTest`, `DataLoaderContentPackTest` | `manifestLint`, `contentPackHarness` | `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | `required` |
| `PR07-M02` | §5.2 pack-local hidden branch binding | `game` | content pack binding tests | `whiteBoxContentPack` | `tools/build/reports/phase4/whitebox/content-pack/` | `required` |
| `PR07-M03` | §5.3 player visibility metric | `tools` | `WhiteBoxContentPackRunnerTest`, report tests | `whiteBoxContentPack`, `reportPhase4Only` | `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | `required` |
| `PR07-M04` | §5.4 validation overlay / main menu | `client` | `ValidationScenarioBootstrapTest`, presentation tests | `goldenScreenshot`, `clientSmoke` | `client/build/reports/golden/` | `required` |
| `PR07-M05` | manifest / resource reuse | `game` / `client` | `manifestLint`, `localeLint`, `assetLint`, `audioLint` | `contentPackHarness` | `build/reports/verification/` | `N/A` |
| `PR07-M06` | governance inheritance | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |

### Gate Budget

预计重型任务：`contentPackHarness`、`whiteBoxContentPack`、`goldenScreenshot`、`clientSmoke`、`reportPhase4Only`、`reportPhase4`、`verifyChanged`。触发原因是 PR-07 同时影响 sample pack manifest、hidden branch binding、player visibility metric 和 client 可见性。

### Canonical Artifact

sample pack visibility 的 canonical 证据必须进入 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`。`contentPackHarness` 与 `whiteBoxContentPack` 必须同批刷新；registry presence 不能替代 runtime touch。

### Failure Rule

如果 sample visibility 不达标，先修 ADD-first manifest、hidden branch binding 或 validation scenario；不得在 production path 引入 force-inject test backdoor，也不得把 `samplePackContentPlayerVisibilityRate.reportOnly` 当作 blocking 通过证据。

## 1. 玩家体验目标

本 PR 让官方 sample pack 成为正确示范：玩家装上 `sample.flooded_relics` 后能看到 ADD-first 新内容，pack 作者不会从官方样例学到 REPLACE-heavy 的覆盖式写法。

完成标准：

1. `examples/content-packs/sample.flooded_relics/manifest.yaml` 主路径不再包含 `REPLACE`。
2. sample pack 的 secret zone、hidden event、loot profile、special item template 全部使用 namespaced `ADD`。
3. `REPLACE / precedence / conflict` 保留在 fixture pack。
4. `samplePackContentPlayerVisibilityRate.reportOnly` 进入 white-box content pack report。
5. Validation overlay / main menu 展示 active pack id、pack 摘要、被触达的 pack content id。

## 2. 当前问题

1. sample manifest 中有 2 个主路径 `REPLACE`：
   - `secret_zone underground_river_crystal_rift`
   - `hidden_event hidden.event.underground_river.crystal_rift.reward`
2. Phase4 文档明确 sample pack 负责证明 `ADD` 主路径。
3. REPLACE-heavy sample 会把 pack 作者引向不可叠加覆盖。
4. 当前 sample pack 更像工程证明，玩家可见度指标仍弱。

## 3. 范围与非目标

### 3.1 范围

sample pack：

- `examples/content-packs/sample.flooded_relics/manifest.yaml`
- `examples/content-packs/sample.flooded_relics/data/secret-zones/**`
- `examples/content-packs/sample.flooded_relics/data/events/**`
- `examples/content-packs/sample.flooded_relics/data/loot/**`
- `examples/content-packs/sample.flooded_relics/data/items/**`
- `examples/content-packs/sample.flooded_relics/i18n/en-US.json`
- `examples/content-packs/sample.flooded_relics/i18n/zh-CN.json`
- `examples/content-packs/sample.flooded_relics/README.md`

runtime manifest schema cutover：

- `examples/content-packs/**/manifest.yaml`
- `tools/src/main/resources/fixtures/content-packs/packs/*/manifest.yaml`
- `game/src/main/kotlin/com/ktome/game/contentpack/ContentPackModels.kt`
- `game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt`
- `tools/src/main/kotlin/com/ktome/tools/contentpack/ContentPackHarnessRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/contentpack/WhiteBoxContentPackRunner.kt`

schema v2 测试更新：

- `game/src/test/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolverTest.kt`
- `game/src/test/kotlin/com/ktome/game/contentpack/DataLoaderContentPackTest.kt`
- 新增 repository runtime manifest inventory test，扫描 `examples/content-packs/**/manifest.yaml` 与 `tools/src/main/resources/fixtures/content-packs/packs/*/manifest.yaml`，断言所有 runtime manifest 的 `schemaVersion` 等于 `ContentPackManifest.SCHEMA_VERSION`；`tools/src/main/resources/fixtures/content-packs/*.yaml` sidecar harness spec 不参与该断言。

上游合同文档同步：

- `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
- `docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md`
- `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`

fixtures 与 tools：

- `game/src/main/kotlin/com/ktome/game/contentpack/ContentPackModels.kt`
- `game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt`
- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`
- `game/src/main/kotlin/com/ktome/game/hidden/HiddenContentMapgenPipeline.kt`
- `docs/review/phase4/opt/baselines/2026-04-24-phase4-sample-pack-add-first-owner-baseline.json`
- `tools/src/main/resources/fixtures/content-packs/sample.flooded_relics.yaml`
- `tools/src/main/resources/fixtures/content-packs/packs/fixture_sample_flooded_relics_override/**`
- `tools/src/main/kotlin/com/ktome/tools/contentpack/ContentPackHarnessRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/contentpack/WhiteBoxContentPackRunner.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/**`

client：

- `client/src/main/kotlin/com/ktome/client/screen/MainMenuSummaryModel.kt`
- `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupScreen.kt`
- `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupController.kt`

### 3.2 非目标

1. 不改 content pack runtime op 白名单。
2. 不移除 fixture pack 的 `REPLACE` 覆盖。
3. 不新增 sample pack 资源。
4. 不新增 Lua、脚本宿主或 Mod SDK。
5. 不让 sample pack 修改 core 规则语义。
6. 不做 v1 runtime manifest 兼容读，不提供 migration shim，不保留 legacy alias。

## 4. 资源要求

### 4.1 图片资源

不生成新图片资源。

执行要求：

1. 继续使用 sample pack 已有 `phase4/pr09` 图片。
2. 继续使用 `examples/content-packs/sample.flooded_relics/visual/visual-manifest.json`。
3. 不新增 image plan、generation report、processing report。

### 4.2 音频资源

不生成新音频资源。

执行要求：

1. 继续使用 sample pack 已有 `phase4/pr09` 音频。
2. 继续使用 `examples/content-packs/sample.flooded_relics/audio/audio-manifest.json`。
3. 不新增 audio plan、generation report、processing report。

## 5. 技术方案

### 5.1 Manifest 改造

sample pack 主路径固定改为 5 个 `ADD`：

| registry | id |
| --- | --- |
| `secret_zone` | `sample.flooded_relics.secret_zone.flooded_reliquary` |
| `hidden_event` | `sample.flooded_relics.hidden_event.flooded_reliquary.reward` |
| `loot_profile` | `sample.flooded_relics.loot.flooded_reliquary.secret` |
| `special_item_template` | `sample.flooded_relics.unique.floodtide_lantern` |
| `special_item_template` | `sample.flooded_relics.artifact.tideglass_echo` |

执行要求：

1. 所有新增 entry id 使用 `sample.flooded_relics.` 前缀。
2. 所有 locale / visual / audio key 使用 `sample_flooded_relics.*` 前缀。
3. sample pack 不覆盖 base `underground_river_crystal_rift`。
4. `fixture_sample_flooded_relics_override` 继续承担覆盖式 `REPLACE` 验证。
5. fixture pack id 固定为 `fixture_sample_flooded_relics_override`；目录名固定为 `fixture_sample_flooded_relics_override`，不得与 official sample pack id 混用。
6. sample pack 对照 fixture id 固定使用 `_`：`fixture_sample_flooded_relics_override`；本 PR 不全量迁移其他 fixture pack id，不改变现有非 sample fixture id 规范。

fixture rename 范围固定为：

1. 删除旧目录 `tools/src/main/resources/fixtures/content-packs/packs/fixture.sample_flooded_relics_override/`。
2. 新增目录 `tools/src/main/resources/fixtures/content-packs/packs/fixture_sample_flooded_relics_override/`。
3. `tools/src/main/resources/fixtures/content-packs/sample.flooded_relics.yaml` 的 `dualPackScenarios.fixturePackId` 与 `expectedOrder` 同步改为 `fixture_sample_flooded_relics_override`。
4. `ContentPackHarnessRunner`、`whiteBoxContentPack`、manifest lint fixture 和 report touched ids 中不得残留 `fixture.sample_flooded_relics_override`。
5. 旧 fixture pack id 命中时 fail fast，不保留 compat alias。

### 5.2 Pack-local hidden branch binding

sample pack 必须新增 pack-local hidden branch binding，使 ADD 内容进入 active pack 的玩家路径：

| 字段 | 固定值 |
| --- | --- |
| `bindingId` | `sample.flooded_relics.search.flooded_reliquary` |
| `zoneId` | `underground_river` |
| `secretZoneId` | `sample.flooded_relics.secret_zone.flooded_reliquary` |
| `hiddenEventId` | `sample.flooded_relics.hidden_event.flooded_reliquary.reward` |
| `anchorTag` | `hidden.primer.underground_river.crystal_rift` |
| `pathClass` | `OPTIONAL_SECRET` |
| `fixedSeedVisibilityCase` | `sample_flooded_relics_active_2026042437` |

实现合同：

1. `ContentPackModels.kt` 为 runtime manifest 增加 `extensions.hiddenBranchBindings`，该字段不属于 overlay `op`，不改变 `ADD / REPLACE` 白名单。
2. `ContentPackRuntimeResolver` 校验 binding 内所有 content id 都属于同一个 pack namespace。
3. `DataLoader` 在 pack active 时把 binding 附加到 merged schema 的 hidden/search binding 集合；无 active pack 时不产生 sample binding。
4. `HiddenContentMapgenPipeline` 只消费 merged schema，不知道 pack 文件路径。
5. `contentPackHarness` 对 active sample pack fixed-seed run 断言至少触达 `sample.flooded_relics.secret_zone.flooded_reliquary`、`sample.flooded_relics.hidden_event.flooded_reliquary.reward` 和 1 个 sample item id。
6. `whiteBoxContentPack` 把 touched content ids 写入 canonical artifact；不得用 registry presence 代替 runtime touch。

manifest 层级固定为：

```yaml
id: sample.flooded_relics
version: 1.0.0
schemaVersion: 2
gameVersionRange: ">=0.4.0 <0.5.0"
namespace: sample_flooded_relics
dependencies: []
localeBundles:
  - i18n/en-US.json
  - i18n/zh-CN.json
visualManifest: visual/visual-manifest.json
audioManifest: audio/audio-manifest.json
overlays:
  - targetRef:
      registry: secret_zone
      id: sample.flooded_relics.secret_zone.flooded_reliquary
    op: ADD
    sourceFile: data/secret-zones/sample.flooded_relics.secret_zone.flooded_reliquary.yaml
extensions:
  hiddenBranchBindings:
    - bindingId: sample.flooded_relics.search.flooded_reliquary
      zoneId: underground_river
      slot: secondary
      secretZoneId: sample.flooded_relics.secret_zone.flooded_reliquary
      hiddenEventId: sample.flooded_relics.hidden_event.flooded_reliquary.reward
      anchorTag: hidden.primer.underground_river.crystal_rift
      pathClass: OPTIONAL_SECRET
      fixedSeedVisibilityCase: sample_flooded_relics_active_2026042437
```

schemaVersion 规则：

1. 本 PR 将 `ContentPackManifest.SCHEMA_VERSION` 升级为 `2`，所有 runtime manifest 必须同步写为 `schemaVersion: 2`。
2. `schemaVersion: 1` 的 runtime manifest 进入 loader 时必须 fail fast，错误信息必须指出需要刷新 content pack manifest schema，不保留 dual-read、legacy alias、字段回填或降级加载。
3. `extensions.hiddenBranchBindings` 是 schema v2 的正式 root field；缺省该字段表示该 pack 没有 pack-local hidden branch binding。
4. fixture harness spec 的 `packId / dualPackScenarios` 不得混入 runtime manifest。
5. manifest loader 必须区分 runtime manifest 与 sidecar harness spec；sidecar spec 不参与 runtime schemaVersion 校验。
6. `manifestLint` 必须扫描仓库内所有 runtime manifest，断言 `schemaVersion` 与 `ContentPackManifest.SCHEMA_VERSION` 一致。
7. 上游 content pack 主文档、overlay loader 文档、cross-cutting contract 和 verification checklist 必须同步改为 schema v2 口径。

secret zone selection 规则：

1. sample pack hidden branch binding 使用 pack-local `secondarySecretSlot`，不与 base `underground_river_crystal_rift` 争夺主 secret slot。
2. active sample pack 的 fixed-seed visibility case `sample_flooded_relics_active_2026042437` 必须通过 manifest data 指定 secondary slot，不在 production 代码中加入 validation-only 分支。
3. `contentPackHarness`、`whiteBoxContentPack` 和 PR-07 validation scenario 只选择该 fixed-seed manifest case；`HiddenContentMapgenPipeline` 只读取 merged schema，不读取 system property、test flag 或 harness flag。
4. normal runtime 中 base crystal rift 与 sample flooded reliquary 同一 run 共存，二者 entry id 不得互相覆盖。
5. `underground_river` secret slot capacity 的权威 owner 是 PR-04 的 `secretZoneSelector.primarySlot / secondarySlot`；本 PR 只消费该 capacity。
6. 第三个 pack-local secret 绑定同一 zone 时 pack loader fail fast。
7. report 必须输出 `samplePackSecondarySecretSlotUsed=true` 与 `samplePackFixedSeedVisibilityCase=true`。

### 5.3 玩家可见度指标

新增 report-only 指标：

| 指标 | 阈值 | metricKind | producer | ownerBaseline | failSemantics |
| --- | ---: | --- | --- | --- | --- |
| `samplePackContentPlayerVisibilityRate.reportOnly` | `100% active sample fixed-seed runs with >= 1 runtime touch` | `reportOnlyOwner` | `whiteBoxContentPack` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-sample-pack-add-first-owner-baseline.json` | `warn only` |
| `samplePackTouchedContentIds` | `>= 3 ids in active sample fixed-seed run` | `supporting` | `whiteBoxContentPack` | `N/A` | `display only` |
| `samplePackAddOnlyMainPath` | `true` | `supporting` | `contentPackHarness` | `N/A` | `display only` |

ADD-only 主路径由 `manifestLint` 作为 fail-fast gate 负责；`samplePackAddOnlyMainPath` 只作为 runtime/supporting evidence 展示，不进入 owner gate。

`samplePackContentPlayerVisibilityRate.reportOnly` 定义：

1. 事件来源：`whiteBoxContentPack` active sample fixed-seed runtime trace。
2. 分子：`active sample fixed-seed run` 中触达至少 1 个 sample namespaced runtime content id 的 run 数。
3. 分母：启用 `sample.flooded_relics` 的 fixed-seed sample run 数，PR-07 固定为 `sample_flooded_relics_active_2026042437`。
4. 聚合公式：`touchedSampleRuns / activeSampleFixedSeedRuns`。
5. warning floor：`100%`；低于该值只触发 report-only warning，不进入 release-facing blocking gate。

触达定义：

1. 看到 sample secret zone。
2. 看到 sample hidden event。
3. 生成 sample loot profile。
4. 看到 `sample.flooded_relics.unique.floodtide_lantern`。
5. 看到 `sample.flooded_relics.artifact.tideglass_echo`。

### 5.4 Validation overlay / main menu

展示字段：

1. active pack id。
2. pack namespace。
3. overlay op summary。
4. touched content ids。
5. pack-local visual/audio/i18n key resolution status。

这些字段只读展示，不改变 runtime 规则。

最小 UI 完成态：

| 状态 | UI 展示 |
| --- | --- |
| 无 active pack | active pack id 显示 `N/A`，op summary 与 touched content ids 显示 empty state |
| active sample pack | 显示 pack id、namespace、op summary、touched content ids、key resolution status |
| key resolution warning | 显示 visual/audio/i18n warning；runtime 规则不变 |

`clientSmoke` 与 `goldenScreenshot` 必须覆盖无 active pack、active sample pack 两种状态。

key resolution summary 固定格式：

| 字段 | 含义 |
| --- | --- |
| `resolvedVisualKeys` | active pack visual key 解析成功数量 |
| `resolvedAudioKeys` | active pack audio key 解析成功数量 |
| `resolvedLocaleKeys` | active pack locale key 解析成功数量 |
| `overriddenKeys` | pack visual/audio manifest 对 base manifest 同名 key 的 file path overriding 数量；official sample pack 不覆盖 base manifest，固定为 `0` |
| `warningVisualKeys` | 未解析 visual key 列表 |
| `warningAudioKeys` | 未解析 audio key 列表 |
| `warningLocaleKeys` | 未解析 locale key 列表 |

sample pack README 必须写明：

1. 主路径只使用 `ADD`。
2. `REPLACE / precedence / conflict` 示例位于 `tools/src/main/resources/fixtures/content-packs/packs/fixture_sample_flooded_relics_override/`。
3. 新 content id 必须使用 `sample.flooded_relics.` namespace。
4. visual/audio/i18n key 使用 `sample_flooded_relics.*` prefix。
5. sample pack 不修改 core rule、damage type、resource type 或 base registry item。

## 6. 测试与自证

### 6.1 必测行为

1. official sample pack manifest 主路径只有 `ADD`。
2. `REPLACE / precedence / conflict` 只保留在 fixture pack。
3. `hiddenBranchBindings` 使 active sample pack fixed-seed run 触达 sample secret zone、hidden event、loot profile 和至少 1 个 sample item。
4. `samplePackContentPlayerVisibilityRate.reportOnly` 与 `samplePackTouchedContentIds` 进入 `whiteBoxContentPack` artifact。
5. Validation overlay / main menu 在 no-pack 状态显示 empty state。
6. Validation overlay / main menu 在 active sample pack 状态显示 pack id、namespace、op summary、touched content ids、key resolution status。
7. visual/audio/i18n key resolution warning 只显示为 UI warning，不改变 runtime 规则。
8. `manifestLint / assetLint / audioLint / localeLint` 证明 sample pack 复用资源全部可解析。
9. fixed-seed visibility case 必须通过 manifest data 使用 sample secondary secret slot，确保 sample content 在验证路径中可见。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew manifestLint contractLint localeLint assetLint audioLint :game:test contentPackHarness whiteBoxContentPack goldenScreenshot clientSmoke reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr07
```

必须保留以下自证产物：

1. `build/reports/tests/` 中 `ContentPackRuntimeResolverTest`、`DataLoaderContentPackTest` 与 runtime manifest inventory test 结果。
2. `tools/build/reports/` 中 `contentPackHarness`、`whiteBoxContentPack` producer 产物。
3. `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` canonical report 产物，且 `reportPhase4Only` 与 `reportPhase4` 对 sample pack metrics 读取同一 producer artifact。
3. `build/reports/verification/` 中 `manifestLint`、`contractLint`、`maintainabilityLint`、`verifyChanged` 产物。
4. `build/reports/tests/` 中 content pack runner、validation overlay、main menu summary、golden screenshot 相关测试结果。
5. `build/whitebox/phase4-v4-pr07/evidence/` 中人工白盒截图、日志、manual record。

### 6.3 人工白盒验证流程

本流程必须遵循 `docs/computer-use-whitebox-flow.md`。人工白盒必须使用 packaged app + Computer Use，不得用 IDE、Gradle run 或测试 harness 替代。

已有游戏 Validation Mode 改造要求：

1. 本 PR 必须接入 PR-00 的 `PHASE4_V4_FAST` section，scenario id 固定为 `phase4-v4-pr07`。
2. `prepare-primary-scene` 必须在现有游戏内 validation session 中展示 no-pack empty state 与 active sample pack summary 的切换结果，包含 active pack id、namespace、op summary、touched content ids、key resolution status。
3. `prepare-secondary-scene` 必须启用 `sample.flooded_relics` 并把玩家放到 sample secret route 可触发位置，正式 Search/Interact 后 touched content ids 包含 sample namespace。
4. `show-evidence-summary` 必须列出 no-pack、active sample pack、sample secret touch、touched ids、key warning 五组证据。
5. active pack、namespace、touched ids、key resolution 必须来自 content pack runtime resolver 与 validation summary，不得由 client 写死 sample 文案。

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. scenario id：`phase4-v4-pr07`
4. preset：`CONTENT_PACK`
5. seed：`2026042437`
6. runtime home：`build/whitebox/phase4-v4-pr07/runtime-home`
7. evidence 目录：`build/whitebox/phase4-v4-pr07/evidence`
8. manual record：`docs/review/phase4/v4-pr/manual-records/phase4-v4-pr07-sample-pack-add-first-visibility.md`

流程：

1. 打包并生成快速白盒材料：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr07
```

2. 执行 `build/whitebox/phase4-v4-pr07/launch-packaged-app.sh` 启动 packaged app，Computer Use 目标 app 固定为 `com.ktome.client`。
3. 按 `build/whitebox/phase4-v4-pr07/cua-runbook.md` 打开 validation overlay，执行 `PHASE4_V4_FAST / prepare-primary-scene`。
4. 截图记录 no-pack 状态 active pack id 为 `N/A`，op summary 与 touched content ids 为 empty state。
5. 截图记录 active sample pack 状态的 pack id、namespace、op summary 和 key resolution status。
6. 执行 `PHASE4_V4_FAST / prepare-secondary-scene`，触发 `sample.flooded_relics.search.flooded_reliquary` hidden branch binding。
7. 截图记录 sample secret zone、sample hidden event 和 sample item id 出现在玩家路径或 touched content ids。
8. 执行 `PHASE4_V4_FAST / show-evidence-summary`，确认 key resolution warning 场景记录来源不改变 runtime 规则。
9. 保存证据：
    - `phase4-v4-pr07-no-pack-empty-state.png`
    - `phase4-v4-pr07-active-sample-pack-summary.png`
    - `phase4-v4-pr07-sample-secret-touch.png`
    - `phase4-v4-pr07-touched-content-ids.png`
    - `phase4-v4-pr07-key-resolution-warning.png`
    - `phase4-v4-pr07-app.log`

通过标准：

1. sample pack 主路径向 pack 作者展示 ADD-first，而不是 REPLACE-heavy。
2. 玩家启用 sample pack 后能看到 active pack 状态和至少一个 sample content touch。
3. no-pack 与 active sample pack 两种 UI 状态都有证据。
4. manual record 写明 packaged app 路径、runtime home、seed、输入序列、截图路径和结论。

### 6.4 统一验证框架关系

本 PR 的 owner 证据来自 `contentPackHarness` 和 `whiteBoxContentPack`。`clientSmoke / goldenScreenshot / manual record` 证明玩家可见性；它们不能替代 content pack manifest、runtime resolver、资源 lint 和 ADD reachability 断言。

### 6.5 玩家体验 Golden Path

1. 玩家在 no-pack 状态进入 main menu 或 validation overlay，必须看到 active pack id 为 `N/A`、op summary empty、touched content ids empty。
2. 玩家启用 `sample.flooded_relics` 后，必须看到 active pack id、namespace、ADD-only op summary 和 key resolution summary。
3. 玩家进入 fixed-seed sample secret route 后，必须看到 sample secret zone 或 sample hidden event 的 touched content id。
4. 玩家获得 sample item 时，必须看到 `sample.flooded_relics.unique.floodtide_lantern` 或 `sample.flooded_relics.artifact.tideglass_echo` 进入 touched content ids。
5. pack 作者阅读 `examples/content-packs/sample.flooded_relics/README.md` 时，必须明确主路径 ADD-first 与 fixture REPLACE 的边界。

## 7. 验证命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew manifestLint contractLint localeLint assetLint audioLint :game:test contentPackHarness whiteBoxContentPack goldenScreenshot clientSmoke reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
```

## 8. 完成定义

1. official sample pack manifest 主路径只有 `ADD`。
2. fixture pack 继续覆盖 `REPLACE / precedence / conflict`。
3. `whiteBoxContentPack` 输出 `samplePackContentPlayerVisibilityRate.reportOnly`。
4. active sample pack fixed-seed run 至少触达 1 个 sample secret zone、1 个 sample hidden event 和 1 个 sample item id。
5. sample secondary secret slot 与 base crystal rift 不互相覆盖。
6. Validation overlay / main menu 能展示 active sample pack、no pack empty state、touched content ids 和 key resolution summary。
7. key resolution warning 显示为 UI warning，不改变 runtime 规则。
8. sample pack README 明确 ADD-first 主路径与 fixture REPLACE 边界。
9. `ContentPackManifest.SCHEMA_VERSION=2`，所有 runtime `manifest.yaml` 均为 `schemaVersion: 2`。
10. `schemaVersion: 1` runtime manifest fail fast，不进入 resolver merge。
11. 上游 Phase4 content-pack 合同文档全部同步 schema v2。
12. `:game:test + contentPackHarness + whiteBoxContentPack + reportPhase4Only + reportPhase4` 同批刷新通过。
13. `manifestLint contractLint localeLint assetLint audioLint clientSmoke goldenScreenshot verifyChanged` 同批通过。
14. 没有新增图片计划文件。
15. 没有新增音频计划文件。

# Phase4 v4 PR 开发文档二次深度审阅报告 R3

审阅对象：`docs/review/phase4/v4-pr/README.md` 与 `PR-00 ~ PR-07` 开发文档。
审阅范围不包含 `docs/review/phase4/v4-pr/review/` 下历史审阅报告本身。
审阅视角：Roguelike / 类 ToME 系统设计、Phase4 content pack 边界、owner metric 证据链、玩家体验可验证性。

## 0. 总结结论

本轮文档已明显吸收上一轮反馈：PR-00 前置化、PR-04/05/07 串行依赖、资源不新增、Computer Use packaged app 白盒、owner metric 字段表、Tier 口径、report-only cutover 等关键方向都已补齐。

但当前仍有若干会影响后续实现的细小问题，主要集中在：

1. 个别文档使用了与当前仓库真源不一致的 schema / id / fixture 路径。
2. 部分 owner report、baseline 与验证命令仍存在互相矛盾。
3. 少数玩法验证 corpus 或 metric 定义内部不自洽。
4. 玩家可见性测试在 PR-05 仍没有和文档声明完全对齐。

建议在进入 PR 实现前先修正文档，尤其是 P1 与 P2 项。否则实现时很容易引入第二套 schema、错误 ID 迁移、或“report 绿但玩家不可见”的验收漏洞。

## 1. Findings

### P1-01 PR-07 manifest 示例与当前 ContentPackManifest 真源冲突

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:152-169`
- `examples/content-packs/sample.flooded_relics/manifest.yaml:1-37`
- `game/src/main/kotlin/com/ktome/game/contentpack/ContentPackModels.kt:39-49`

**问题**

PR-07 将 manifest 层级固定写成：

```yaml
packId: sample.flooded_relics
ops:
  - op: ADD
    contentId: sample.flooded_relics.secret_zone.flooded_reliquary
extensions:
  hiddenBranchBindings:
    ...
```

但当前正式 manifest 真源是 `id / version / schemaVersion / namespace / overlays`，`ContentPackManifest` 也只有 `id` 与 `overlays`，没有 `packId` 或 `ops` 字段。`tools/src/main/resources/fixtures/content-packs/*.yaml` 中的 `packId` 是 harness fixture spec，不是 runtime content pack manifest。

这会把实现者引向两条危险路径：

1. 在 runtime manifest 中引入第二套 `packId / ops` schema。
2. 把 fixture spec 语义误合并进正式 content pack manifest。

**建议修正**

把 PR-07 示例改成当前 runtime manifest 形态：

```yaml
id: sample.flooded_relics
version: 1.0.0
schemaVersion: 1
namespace: sample_flooded_relics
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

同时明确：`extensions.hiddenBranchBindings` 是 `ContentPackManifest` 的 additive root field；若 schemaVersion 仍为 `1`，必须说明这是 v1 additive optional 字段，还是需要 bump schemaVersion。

### P1-02 PR-01 profession tree id 使用点号格式，和当前数据真源不一致

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:216-225`
- `game/src/main/resources/data/talents/index.yaml:1-64`
- `game/src/main/resources/data/professions/index.yaml:21,51`

**问题**

PR-01 固定树 ID 为 `vanguard.arms / arcanist.fire / ...`，但当前 talent tree 与 profession 数据使用下划线 ID：`vanguard_arms / vanguard_shield / vanguard_warcry / arcanist_flame / arcanist_frost / arcanist_arcane`。

如果按文档实现，会变成一次未声明的全局 content id rename，影响：

- profession `talentTrees`
- talent `treeId`
- i18n key
- visual/audio key
- save / run summary / owner metrics 中的 tree id
- longRunLab 与 soloClear 的 route/build 观测

PR-01 文档并未把“树 ID 全量重命名”列为目标，也没有列出迁移、删除旧 ID、fail-fast old schema 或 fixture 更新。

**建议修正**

优先把 PR-01 表格改回现有 ID：

```text
vanguard_arms / vanguard_shield / vanguard_warcry
arcanist_flame / arcanist_frost / arcanist_arcane
```

若确实想统一改成点号 namespace，则必须把 PR-01 扩成显式 schema/content id cutover PR，并补齐所有 data、i18n、asset/audio、save fail-fast、harness baseline 与 owner metric 字段迁移。这不建议放进当前 PR-01。

### P2-01 PR-06 long-run final corpus 存在重复 full_route

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md:99-112`

**问题**

`2026042406` 与 `2026042408` 的 route intent 完全相同：

```text
deep_iron -> grey_gate -> greenwood -> underground_river -> abyssal_temple
```

PR-06 的目标是“防止奖励和构筑调参过拟合单主线”，重复路线会削弱 route diversity corpus 的代表性。即使 `topZoneRouteHashShare <= 40%` 仍可能通过，设计语义也已经不再是 12 条 full_route 的有效扩容。

**建议修正**

保持 deep_iron 起点数量不变，将 `2026042408` 改成一个未出现的 deep_iron permutation，例如：

```text
deep_iron -> underground_river -> grey_gate -> greenwood -> abyssal_temple
```

并在 metric 说明中增加 `fullRouteIntentDistinctCount == 12` 的 supporting 字段，避免未来再次出现重复 route intent。

### P2-02 PR-07 fixture pack id / 目录重命名未声明现有 fixture 迁移

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:64-66`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:120-127`
- `tools/src/main/resources/fixtures/content-packs/sample.flooded_relics.yaml:21-24`
- `tools/src/main/resources/fixtures/content-packs/packs/fixture.sample_flooded_relics_override/manifest.yaml:1-19`

**问题**

PR-07 文档要求 fixture pack 固定为 `fixture_sample_flooded_relics_override`，目录也固定为 `fixture_sample_flooded_relics_override`。但当前仓库已有 fixture pack 是：

```text
fixture.sample_flooded_relics_override
tools/src/main/resources/fixtures/content-packs/packs/fixture.sample_flooded_relics_override/
```

并且 `sample.flooded_relics.yaml` 的 `dualPackScenarios` 仍引用 `fixture.sample_flooded_relics_override`。

文档目前没有声明这是一次 rename，也没有列出：

- 删除或移动旧目录
- 更新 `dualPackScenarios.fixturePackId`
- 更新 expected order
- 更新 ContentPackHarnessRunner 断言中的 fixture id
- 更新资源 manifest 中的 repo-relative path

**建议修正**

二选一：

1. 保持现有 fixture pack id 与目录，只补充 `ContentPackIdParser` 必须把完整 pack id 当字符串处理。
2. 如果坚持改成下划线，则把 PR-07 范围明确为 fixture pack rename，并列出所有旧路径删除、新路径新增、fixture spec、harness expected order 与报告字段更新。

当前文档已把 `.` 永远定义为 namespace 分隔，倾向第二种方案；但必须把 rename 写完整，否则实现会漏改 fixture spec。

### P2-03 PR-02 command 扩展了 offerFingerprint，但没有明确 RenderSnapshot 暴露 opaque token

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md:226-258`
- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:416-432`

**问题**

PR-02 要求 `BuyShopOffer(index, offerFingerprint, replacementHotkey)`，并要求 client 二次提交时携带原始 `offerFingerprint`。但文档没有明确 `ShopOfferSnapshot` 必须暴露 `offerFingerprint`，当前 snapshot 只有 `index / labelKey / price / tags / tagLabelKeys`。

如果不补这一条，client 只有两种坏选择：

1. 自己复算 fingerprint，导致 client 读取 shopId、offerDef、price、kind、stockVersion 等规则细节，违反 client 只消费 snapshot 的边界。
2. 二次提交时没有 fingerprint，replace flow 永远被 `StaleOffer` 或 command validation 卡住。

**建议修正**

在 PR-02 §5.3 或 §5.4 明确：

```kotlin
@Serializable
data class ShopOfferSnapshot(
    val index: Int,
    val labelKey: String,
    val price: Int,
    val tags: List<String> = emptyList(),
    val tagLabelKeys: List<String> = emptyList(),
    val offerFingerprint: String,
)
```

并写清：

- fingerprint 由 game/session 生成。
- client 只把它当 opaque token 传回。
- snapshot / command roundtrip 要有测试覆盖。

### P2-04 PR-03 baseline 文件清单与 metric 表不一致

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:70-77`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:390-403`

**问题**

PR-03 数据范围只列出：

```text
docs/review/phase4/opt/baselines/2026-04-12-phase4-terminal-build-identity-baseline.json
```

但新增 blocking metric 表实际引用了三类 baseline：

- `2026-04-24-phase4-terminal-build-identity-profession-baseline.json`
- `2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json`
- `2026-04-12-phase4-loot-local-reward-identity-baseline.json`

这会让实现者漏建或漏接新的 baseline 文件，也会让 `Phase4OwnerBaselineRegistry` 与 aggregation manifest 对不齐。

**建议修正**

PR-03 §3.1 的数据范围必须列出所有 baseline 文件，并说明每个 baseline 的 owner：

```text
docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-build-identity-profession-baseline.json
docs/review/phase4/opt/baselines/2026-04-24-phase4-terminal-milestone-slot-balance-baseline.json
docs/review/phase4/opt/baselines/2026-04-12-phase4-loot-local-reward-identity-baseline.json
```

并在 §7 增加“新增 baseline 必须进入 `Phase4OwnerBaselineRegistry` 与 `aggregation-manifest.yaml`”。

### P2-05 PR-03 修改 i18n 但验证命令缺少 localeLint

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:70-74`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:432-435`
- `docs/review/phase4/v4-pr/README.md:156-174`

**问题**

PR-03 范围包含 `game/src/main/resources/i18n/en-US.json` 与 `zh-CN.json`，但自动化命令只有 `audioLint contractLint maintainabilityLint verifyChanged`，没有 `localeLint`。

这与 README 的 i18n key discipline 不一致，也会允许 reward explanation / build identity 新文案缺 key 或中英文缺项时通过。

**建议修正**

把 PR-03 和 README owner suite 的命令改为包含 `localeLint`：

```bash
./gradlew localeLint :game:test :tools:whiteBoxLoot :tools:lootBalanceLab longRunLab verifyOwner reportPhase4Only reportPhase4 audioLint contractLint maintainabilityLint verifyChanged
```

如果 PR-03 实际会触碰 visual/icon keys，也应一并保留 `assetLint`。

### P2-06 PR-04 flavorOnlyMechanics 校验公式语义反了

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md:161-166`

**问题**

文档写：

```text
mechanicsWithoutDedicatedRuntimeHook 必须等于 flavorOnlyMechanics ∪ knownRuntimeHooks
```

但从命名与上文规则看，`mechanicsWithoutDedicatedRuntimeHook` 应该只包含“没有 runtime hook 的机制名词”。把 `knownRuntimeHooks` 并进去会让已注册 runtime hook 也被视为 without dedicated runtime hook，概念相反。

**建议修正**

改为两个断言：

```text
allMechanicTerms == runtimeHookIds ∪ flavorOnlyMechanics
runtimeHookIds ∩ flavorOnlyMechanics == empty
mechanicsWithoutDedicatedRuntimeHook == flavorOnlyMechanics
```

这样 validator 才能同时覆盖“漏分类”和“runtime hook 被误列为 flavor-only”两种错误。

### P2-07 PR-05 声明了 goldenScreenshot / clientSmoke 证据，但命令与测试范围未包含

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:65-71`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:212-216`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:279-281`
- `docs/review/phase4/v4-pr/README.md:175-184`

**问题**

PR-05 明确说 `goldenScreenshot / clientSmoke / manual record` 是玩家可见性证据，README 也把 PR-05 Boss variant warning / telegraph presentation 列为必须补 render snapshot 的 UI surface。但 PR-05 的测试范围只列 core/game/tools 测试，命令也没有 `goldenScreenshot`、`clientSmoke` 或 client render snapshot test。

这会出现典型 Phase4 问题：`bossHarness` 证明权重、phase 和 coverage 正确，但玩家界面没有 warning、telegraph 层级错、音画提示丢失，report 仍然绿。

**建议修正**

PR-05 测试范围新增：

```text
client/src/test/kotlin/com/ktome/client/render/**/*SnapshotTest.kt
```

命令改为：

```bash
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test bossHarness goldenScreenshot clientSmoke reportPhase4 maintainabilityLint verifyChanged
```

并在完成定义增加：Boss variant warning / telegraph presentation 的 snapshot 或 golden diff 必须包含三个 variant 的至少一个可见案例。

### P2-08 多个 PR 的 canonical report 自证路径仍写成旧或模糊路径

**位置**

- `docs/phase4/2026-03-13-phase4-verification-checklist.md:35-37`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:344-348`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:310-318`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md:248-255`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:219-223`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md:214-221`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:263-269`

**问题**

Phase4 checklist 已固定：

```text
tools/build/reports/verification/phase4/report-phase4-summary.{json,md}
```

为 canonical report 产物；旧：

```text
tools/build/reports/phase4/phase4-summary.{json,md}
```

只作为 legacy fallback。

但多个 PR 文档仍把自证产物写成泛化的 `build/reports/phase4/`。这会让验收者不知道应该看 root、tools module、canonical verification summary，还是 legacy fallback summary。

**建议修正**

统一写成三类产物：

```text
build/reports/tests/...
tools/build/reports/<producer-specific>/...
tools/build/reports/verification/phase4/report-phase4-summary.{json,md}
```

如果某 PR 还需要 legacy parity，再单独写：

```text
tools/build/reports/phase4/phase4-summary.{json,md} 仅作为 phase4LegacyReport fallback / parity evidence
```

### P3-01 PR-00 Gradle project property 与 packaged app system property 名称容易混淆

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md:232-239`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md:269-273`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md:354-358`

**问题**

PR-00 同时使用：

- Gradle project property：`-Pktome.whitebox.scenario`
- packaged app system property：`ktome.validation.scenario`

文档内容本身是合理的，但 §6.1 “未传入 `ktome.whitebox.scenario` 时 fail fast” 与 §5.3 “packaged app 读取 `ktome.validation.scenario`”没有明确二者转换关系。

**建议修正**

补一行固定合同：

```text
preparePhase4V4Whitebox 只读取 Gradle project property ktome.whitebox.scenario；
生成的 launch-packaged-app.sh 必须把该值写入 JVM system property ktome.validation.scenario；
packaged app runtime 不读取 ktome.whitebox.scenario。
```

### P3-02 PR-01 命令缺 reportPhase4Only，但同文档完成定义要求它输出指标

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:331`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:337-341`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md:447-453`

**问题**

PR-01 §6.1 / §7 明确要求 `reportPhase4Only` 与 `reportPhase4` 对新增 owner metrics 输出一致字段，但自动化命令只跑 `reportPhase4`。

**建议修正**

在 PR-01 和 README owner suite 中加入 `reportPhase4Only`，并保持 producer 先跑：

```bash
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab soloClearLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
```

### P3-03 PR-06 命令顺序建议把 reportPhase4Only 放在 reportPhase4 前

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md:214`
- `docs/review/phase4/v4-pr/README.md:110`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md:35,43`

**问题**

PR-06 命令当前是：

```bash
./gradlew :game:test longRunLab reportPhase4 reportPhase4Only scopeCoverageLint maintainabilityLint verifyChanged
```

Phase4 checklist 中 `reportPhase4Only` 是定向回归入口，`reportPhase4` 做 canonical vs legacy parity。建议把 `reportPhase4Only` 放在 `reportPhase4` 前，语义更稳定：先验证 artifact-only summary，再跑 parity/materialization。

**建议修正**

```bash
./gradlew :game:test longRunLab reportPhase4Only reportPhase4 scopeCoverageLint maintainabilityLint verifyChanged
```

### P3-04 PR-05 onEnterEventKey 命名模式会造成双重前缀歧义

**位置**

- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:138`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:153-155`

**问题**

命名模式写成：

```text
boss.variant.<variantId>.phase_override.entered
```

但 variant id 本身已经是 `boss.variant.molten_glass`。直接套用会得到：

```text
boss.variant.boss.variant.molten_glass.phase_override.entered
```

这不是致命问题，但会污染 event key 与 i18n key 风格。

**建议修正**

改为其中一种：

```text
boss.variant.<variantSlug>.phase_override.entered
```

或：

```text
boss.phase_override.entered.<variantId>
```

并定义 `<variantSlug>` 是去掉 `boss.variant.` 前缀后的稳定 slug。

### P3-05 README owner suite 表顺序与执行依赖图不一致

**位置**

- `docs/review/phase4/v4-pr/README.md:9-18`
- `docs/review/phase4/v4-pr/README.md:102-111`
- `docs/review/phase4/v4-pr/README.md:191-199`

**问题**

README 顶部与依赖图明确执行顺序为：

```text
PR-00 -> PR-01 -> PR-02 -> PR-03 -> PR-04 -> PR-05 -> PR-07 -> PR-06
```

但 owner suite 表按编号列出 PR-06 在 PR-07 前。虽然这不影响命令本身，但会增加人工执行误读风险。

**建议修正**

将 owner suite 表也改成依赖顺序，或在表标题写明“按 PR 编号排序，实际执行顺序以 §6 依赖图为准”。更推荐直接改成依赖顺序。

### P3-06 PR-03 超过 5 个 blocking owner metric，建议补 rollback 分组边界

**位置**

- `docs/review/phase4/v4-pr/README.md:128-155`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:388-403`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md:423-428`

**问题**

README 已要求单 PR 新增超过 5 个 blocking metric 时提供 metric group、baseline split、执行顺序与 rollback boundary。PR-03 已有 group 与 baseline split，但 rollback boundary 仍偏隐含。

**建议修正**

在 PR-03 §7 增加：

```text
Rollback boundary:
1. profession capstone adoption group 可单独回退，不影响 milestone slot balance group。
2. milestone slot balance group 可单独回退，不回退 profession cutover。
3. topFiveAffixExposureShare 仍归 whiteBoxLoot owner，可独立回退到原 loot-local baseline。
```

这样后续实现遇到 longRunLab 噪声或 slot balance 过严时，不需要整 PR 回退。

## 2. Requirement Alignment

| 维度 | 当前状态 | 仍需修正 |
| --- | --- | --- |
| PR-00 快速白盒 | 已前置；scenario id、packaged app、manual record、证据目录完整 | 明确 `ktome.whitebox.scenario` 到 `ktome.validation.scenario` 的转换关系 |
| PR-01 职业树 | Tier、BASE/ADVANCED/FROZEN、starter 口径已清晰 | tree id 必须回到当前数据真源，或显式升级为 content id cutover |
| PR-02 铭文商店 | 替换流程与 stale offer 保护方向正确 | `offerFingerprint` 必须从 `ShopOfferSnapshot` 暴露为 opaque token |
| PR-03 构筑奖励 | blocking cutover 与 owner metric 表已补强 | baseline 文件清单、`localeLint`、rollback boundary 仍需收紧 |
| PR-04 隐藏探索 | zone hook、frontstage cue、flavor-only 边界清晰 | flavor-only 校验公式需改正 |
| PR-05 Boss 变体 | PR-04 依赖、trigger owner、action emphasis 归属已补 | player-visible telegraph 的 client/golden 验证仍未进命令 |
| PR-06 长局路线 | PR-07 前置、route summary、scopeCoverageLint 已明确 | final corpus 有重复 route；report 命令顺序需统一 |
| PR-07 sample pack | ADD-first、fixture REPLACE 分离、active sample pack visibility 已明确 | manifest schema 示例和 fixture rename 仍未与当前真源对齐 |

## 3. 功能 / 系统一致性矩阵

| PR | core | game | client | tools | docs / assets | 主要风险 |
| --- | --- | --- | --- | --- | --- | --- |
| PR-00 | 低 | 中 | 高 | 高 | 高 | 快速白盒属性命名混淆导致 packaged app 启动协议不稳定 |
| PR-01 | 高 | 高 | 高 | 高 | 高 | tree id 与当前数据不一致会变成未声明全局迁移 |
| PR-02 | 高 | 高 | 高 | 中 | 中 | client 无法取得 offerFingerprint，可能被迫复算规则 token |
| PR-03 | 中 | 高 | 低 | 高 | 高 | baseline 清单不全与缺 localeLint 会让 owner cutover 证据不闭环 |
| PR-04 | 中 | 高 | 高 | 高 | 中 | flavor-only 公式错误会污染 runtime hook 审计 |
| PR-05 | 高 | 高 | 中 | 高 | 高 | 玩家可见 telegraph 没进 client/golden 验证 |
| PR-06 | 低 | 高 | 低 | 高 | 中 | corpus 重复削弱 route diversity，命令顺序不统一 |
| PR-07 | 低 | 高 | 高 | 高 | 高 | manifest schema 与 fixture rename 若不修，会引入第二套 content pack 语义 |

## 4. 玩法与体验审查

### 4.1 当前方向成立

1. PR-01 到 PR-03 的构筑链路已经从“数值堆叠”转向“run 内选择 -> 购买替换 -> 奖励采用”的 ToME 式体验闭环。
2. PR-04 到 PR-05 把隐藏探索与 Boss 变体从纯数据差异推进到玩家可见的前台 cue / warning / phase memory，方向符合 Phase4 后续开发目标。
3. PR-07 将 sample pack 从覆盖式示例转向 ADD-first，能显著降低未来作者学习错误模式的概率。
4. PR-00 快速白盒不替代 owner gate，只缩短抵达目标场景的时间，这个边界是正确的。

### 4.2 仍需防止的体验误差

1. PR-05 若不跑 client/golden，Boss phase language 可能只存在于 harness trace，玩家看不到明确 telegraph。
2. PR-02 若不把 fingerprint 作为 snapshot opaque token，铭文替换 UI 会把规则 token 计算泄漏到 client。
3. PR-06 route corpus 有重复路线，会降低后续 build / reward 指标对多路线体验的代表性。
4. PR-07 manifest 示例若按 `packId / ops` 实现，pack 作者会从官方文档学到错误 manifest 结构。

## 5. 当前阶段必须解决的问题

进入 PR 实现前必须先改：

1. PR-07 manifest 示例改回 `id / overlays` runtime schema，并明确 `extensions.hiddenBranchBindings` 的 schemaVersion 语义。
2. PR-01 tree id 改回现有下划线 ID，除非明确拆出全量 content id cutover。
3. PR-06 去掉重复 full route intent，并补 `fullRouteIntentDistinctCount`。
4. PR-02 为 `ShopOfferSnapshot` 补 `offerFingerprint` opaque token 合同。
5. PR-03 补齐 baseline 文件清单和 `localeLint`。
6. PR-04 修正 `mechanicsWithoutDedicatedRuntimeHook` 公式。
7. PR-05 把 `goldenScreenshot / clientSmoke / client render snapshot` 纳入测试范围和命令。

## 6. Removal / Iteration Plan

| 项 | 当前文档状态 | 建议动作 |
| --- | --- | --- |
| `packId / ops` runtime manifest 示例 | PR-07 中存在 | 删除或改成 fixture-only 示例；runtime 示例统一 `id / overlays` |
| 点号格式 profession tree id | PR-01 中存在 | 删除，改回当前下划线 ID |
| 重复 route intent | PR-06 中存在 | 替换 `2026042408` route |
| 模糊 `build/reports/phase4/` 验收路径 | 多个 PR 中存在 | 改成 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` |
| PR-05 仅 bossHarness 证明 telegraph | PR-05 命令中存在 | 加 `goldenScreenshot / clientSmoke / render snapshot` |
| fixture pack 下划线 rename | PR-07 半声明 | 补完整 rename 范围，或回退为现有 dot id |

## 7. Additional Suggestions

1. README owner suite 表建议按依赖顺序排序，减少 PR-07 / PR-06 执行误读。
2. 每个 PR 的“必须保留自证产物”建议统一模板：tests、producer artifact、canonical phase4 report、whitebox evidence 四类，不再使用泛化目录。
3. PR-03 的 12 个 blocking metrics 建议按 group 输出小节，避免 Markdown report 中形成一张难读的大表。
4. PR-07 的 `extensions.hiddenBranchBindings` 建议在文档里写清“active pack only merge，inactive pack 不进入 merged schema”，当前有这层意思，但可以作为 fail-fast 测试名明确写出。
5. PR-00 scenario registry 与 YAML parity 建议额外列出“PR-01~07 每新增 scenario 必须改 Kotlin registry + YAML + README scenario table”三方同步，降低漏改概率。

## 8. Suggested Verification

文档修正后建议重新审查以下命令块是否完全一致：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab soloClearLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
```

PR-03 建议：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint :game:test :tools:whiteBoxLoot :tools:lootBalanceLab longRunLab verifyOwner reportPhase4Only reportPhase4 audioLint contractLint maintainabilityLint verifyChanged
```

PR-05 建议：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test bossHarness goldenScreenshot clientSmoke reportPhase4 maintainabilityLint verifyChanged
```

PR-06 建议：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test longRunLab reportPhase4Only reportPhase4 scopeCoverageLint maintainabilityLint verifyChanged
```

PR-07 建议：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew manifestLint contractLint localeLint assetLint audioLint contentPackHarness whiteBoxContentPack goldenScreenshot clientSmoke reportPhase4 maintainabilityLint verifyChanged
```

## 9. 复审结论

这些 PR 文档已经具备进入实现前的主骨架，但还不应直接作为实现合同冻结。优先修正 P1/P2 项后，Phase4 v4 的后续开发拆分会更稳：

1. 不会引入第二套 content pack manifest schema。
2. 不会误改 profession tree ID 体系。
3. 不会让铭文商店 fingerprint 泄漏到 client 规则计算。
4. 不会让 Boss 变体只在 harness 中成立、玩家界面不可见。
5. 不会让 longRunLab route diversity 从文档层面先出现重复样本。

建议修正文档后，再以 README 的依赖链重新做一次轻量一致性扫描，重点只看：ID、schema 字段、baseline path、Gradle command、whitebox scenario id 五类硬合同。

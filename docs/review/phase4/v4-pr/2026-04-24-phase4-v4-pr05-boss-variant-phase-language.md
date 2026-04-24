> 执行前必须先完整阅读并接受：
> `docs/INDEX.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part3.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part4.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md`

# Phase4 v4 PR-05 Boss Variant Phase Language

**阶段**: `Phase 4 completion hardening / phase4-v4-pr05`
**优先级**: `P1`
**工作量**: `M`
**合并来源**: v4 P1-4
**前置条件**: PR-00、PR-04 已完成；Boss variant trace divergence 已通过，现有 variant visual/audio 资源已进入 manifest
**资源生成结论**: 不生成图片资源；不生成音频资源

## 1. 玩家体验目标

本 PR 不重写 Boss 系统，只补最小 data-level 阶段语言。玩家重复遇到 Boss variant 时，必须能从阶段触发、telegraph 和行动组合上记住“这次不是同一个 Boss”。

完成标准：

1. 3 个 boss variant 均有 `phaseOverrides`。
2. 每个 `phaseOverrides` 至少声明 1 个阶段、1 个 trigger、1 个 telegraph、1 个 action emphasis。
3. Boss harness 区分 action trace divergence 与 structural phase divergence。
4. `bossVariantPhaseOverrideSchemaCoverage=3/3` 且 `bossVariantPhaseOverrideRuntimeTriggerCoverage=3/3`。
5. 不引入 Phase5 tactical AI、普通敌人 intent、`AIPlanSnapshot`。

## 2. 当前问题

1. `boss-variants/index.yaml` 中 3 个 variant 只有 `actionWeightProfileId`。
2. white-box boss report 中每对都是 `phaseGraphUnchanged=true`。
3. 指标已经证明 action trace divergence，但不能证明阶段记忆点。
4. 不补最小 phase language 会让后续内容继续复制 action weight 差分。

## 3. 范围与非目标

### 3.1 范围

生产代码：

- `core/src/main/kotlin/com/ktome/core/ai/BossEncounter.kt`
- `core/src/main/kotlin/com/ktome/core/ai/BossPhaseManager.kt`
- `game/src/main/kotlin/com/ktome/game/model/BossDefinition.kt`
- `game/src/main/kotlin/com/ktome/game/factory/BossFactory.kt`
- `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt`
- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `tools/src/main/kotlin/com/ktome/tools/boss/**`
- `tools/src/main/kotlin/com/ktome/tools/phase4/**`

数据：

- `game/src/main/resources/data/boss-variants/index.yaml`
- `game/src/main/resources/data/bosses/index.yaml`
- `game/src/main/resources/data/telegraph/index.yaml`
- `game/src/main/resources/data/audio/index.yaml`
- `game/src/main/resources/data/visuals/index.yaml`
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`

测试：

- `core/src/test/kotlin/com/ktome/core/ai/BossPhaseManagerTest.kt`
- `game/src/test/kotlin/com/ktome/game/elites/MutationModelsTest.kt`
- `game/src/test/kotlin/com/ktome/game/BossVariantDataLoaderTest.kt`
- `game/src/test/kotlin/com/ktome/game/BossFactoryTest.kt`
- `game/src/test/kotlin/com/ktome/game/hidden/HiddenContentMapgenPipelineTest.kt`
- `client/src/test/kotlin/com/ktome/client/render/**/*SnapshotTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/boss/BossHarnessRunnerTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4ReportRunnerTest.kt`

测试职责固定为三层：

1. `core` 测 `TriggerExpression` 与 `activeTriggerIds` 的 `AllOf / AnyOf / Not` 匹配，不读取 zone 或 data loader 状态。
2. `game` 测 PR-04 zone hook fact materialization，把 `zone.trigger.*` 转成 encounter-local semantic trigger ids。
3. `bossHarness` 测 3 个 fixed variant 的端到端 override trigger、telegraph、action emphasis 和 report coverage。

### 3.2 非目标

1. 不改普通敌人 AI。
2. 不引入 `AIPlanSnapshot`。
3. 不引入 tactical AI decision trace。
4. 不引入 runtime script host。
5. 不重写完整 Boss phase graph runtime。
6. 不新增 Boss。

## 4. 资源要求

### 4.1 图片资源

不生成新图片资源。

执行要求：

1. `molten_glass` 使用已有 `vfx.boss.variant.molten_glass`。
2. `grey_crown` 使用已有 `vfx.boss.variant.grey_crown`。
3. `abyssal_eclipse` 使用已有 `vfx.boss.variant.abyssal_eclipse`。
4. 阶段 telegraph 继续复用 existing variant visual tint 与 `vfx.boss.warning.sigil_01`。

### 4.2 音频资源

不生成新音频资源。

执行要求：

1. `molten_glass` 使用已有 `audio.boss.variant.molten_glass`。
2. `grey_crown` 使用已有 `audio.boss.variant.grey_crown`。
3. `abyssal_eclipse` 使用已有 `audio.boss.variant.abyssal_eclipse`。
4. 通用 warning 继续使用 `audio.boss.warning`。

## 5. 技术方案

### 5.1 Schema

`BossVariantDef` 新增：

```kotlin
data class BossVariantPhaseOverride(
    val phaseId: String,
    val trigger: TriggerExpression,
    val telegraphSpecId: String,
    val actionEmphasisIds: List<String>,
    val onEnterEventKey: String,
)

sealed interface TriggerExpression {
    data class Ref(val triggerId: String) : TriggerExpression
    data class AllOf(val children: List<TriggerExpression>) : TriggerExpression
    data class AnyOf(val children: List<TriggerExpression>) : TriggerExpression
}
```

规则：

1. `phaseId` 必须存在于 base encounter。
2. `telegraphSpecId` 必须存在。
3. `actionEmphasisIds` 必须存在于 Boss 可用 talent/action 集。
4. 每个 variant 至少 1 个 override。
5. override 不改变 hp threshold。
6. `phaseOverrides` 属于 boss variant schema；core boss phase manager 只消费解析后的 semantic override，不知道 YAML 路径、content pack 或 registry source。
7. `DataLoader.parseBossVariants` 必须 fail fast 校验 phase、trigger reference、telegraph、action emphasis 和 on-enter event key。
8. `TriggerExpression.AllOf` 与 `TriggerExpression.AnyOf` 必须至少包含 2 个 child；空 trigger expression fail fast。
9. `onEnterEventKey` 是 `CoreEventBus` 事件 key，命名模式固定为 `boss.variant.<variantSlug>.phase_override.entered`；`variantSlug` 是去掉 `boss.variant.` 前缀后的稳定 slug，且必须匹配 `[a-z][a-z0-9_]*`；同批新增 `log.boss.phase_override_entered` 与中英文 i18n。

runtime 接线固定为：

1. `BossFactory` 将 `BossVariantPhaseOverride` 解析为 core semantic override，传入 `BossEncounter`。
2. `BossPhaseManager` 在 base phase threshold 达成后再评估 override trigger；override 不提前进入 base phase。
3. `BossPhaseEvaluationContext` 必须新增 `activeTriggerIds: Set<String>`，或新增等价的 `BossVariantTriggerFacts` 输入；`core` 只消费这些 semantic trigger id，不读取 zone hook、YAML、registry source、content pack 路径或 client presentation 状态。
4. `game` / zone hook runtime 负责 materialize `zone.trigger.oil_or_fire_seen`、`zone.trigger.void_pressure_active` 和 encounter-local trigger facts；`core` 只对 trigger id 执行 `AllOf` / `AnyOf` / `Not` 语义匹配。
5. override trigger 命中时，phase manager 发出 `onEnterEventKey`，播放 `telegraphSpecId`，并对 `actionEmphasisIds` 写入本 phase 的 action weight multiplier。
6. action emphasis multiplier 固定为 `1.5x`，作用域为 `encounter.phase` 实例；同一 phase 因治疗、吸血或阈值回弹再次进入时不重置 multiplier。
7. `1.5x` 倍率是 Phase4 v4 固定 runtime rule，不进入 content pack 可配置面，不暴露为 overlay scripting knob。
8. override trigger 未命中时，Boss 按 base phase 行为执行，report 记录 `phaseOverrideSkippedReason`。
9. `core` 只消费 semantic override 与 trigger resolution result，不读取 YAML、registry source 或 content pack 路径。

### 5.2 Variant 固定内容

| variant | phase | trigger expression | trigger references | triggerOwner | telegraph | action emphasis |
| --- | --- | --- | --- | --- | --- | --- |
| `boss.variant.molten_glass` | `phase_enraged` | `AllOf(hp_below_50, oil_or_fire_seen)` | `boss.trigger.hp_below_50`, `zone.trigger.oil_or_fire_seen` | `boss-encounter-schema`, `PR-04 zone-hook` | `molten_glass_phase_override_warning` | `linebreaker ∈ boss.molten_forgeborn.actions`, `earthshaker ∈ boss.molten_forgeborn.actions` |
| `boss.variant.grey_crown` | `phase_desperate` | `AllOf(hp_below_45, war_caller_active)` | `boss.trigger.hp_below_45`, `boss.trigger.war_caller_active` | `boss-encounter-schema` | `grey_crown_phase_override_warning` | `battlefield_command ∈ boss.grey_crown.actions`, `ritual_break ∈ boss.grey_crown.actions` |
| `boss.variant.abyssal_eclipse` | `phase_abyssal` | `AllOf(hp_below_40, void_pressure_active)` | `boss.trigger.hp_below_40`, `zone.trigger.void_pressure_active` | `boss-encounter-schema`, `PR-04 zone-hook` | `abyssal_eclipse_phase_override_warning` | `void_breach ∈ boss.abyssal_eclipse.actions`, `abyssal_consecration ∈ boss.abyssal_eclipse.actions` |

overlay 语义：

1. `phaseOverrides` 不是替换 base phase threshold；它是在 base phase 已进入后附加一次 variant-specific trigger。
2. `trigger` 为空时 fail fast；不得用空 trigger 表示总是触发。
3. `telegraphSpecId` 必须引用 `game/src/main/resources/data/telegraph/index.yaml` 中的 ADD entry。
4. `actionEmphasisIds` 只调整当前 phase 的 action weight，不新增 action。
5. `DataLoader` 必须从 `game/src/main/resources/data/bosses/index.yaml` 读取 base boss action 全集；`actionEmphasisIds` 不属于该集合时 fail fast。
6. `boss.trigger.war_caller_active` 由 `grey_crown` base encounter 状态机产生，含义固定为 war caller 伴生单位存活或 war caller aura 仍在当前 encounter 生效；该 trigger 不从 zone hook 或 client presentation 推导。

### 5.3 Telegraph specs

新增 telegraph spec id，但不新增资源。以下 spec 必须作为 `ADD` 写入 `game/src/main/resources/data/telegraph/index.yaml`，visual/audio 全部引用已有 manifest key：

| spec | visualKey | audioProfile |
| --- | --- | --- |
| `molten_glass_phase_override_warning` | `vfx.boss.variant.molten_glass` | `audio.boss.variant.molten_glass` |
| `grey_crown_phase_override_warning` | `vfx.boss.variant.grey_crown` | `audio.boss.variant.grey_crown` |
| `abyssal_eclipse_phase_override_warning` | `vfx.boss.variant.abyssal_eclipse` | `audio.boss.variant.abyssal_eclipse` |

### 5.4 Boss harness

Boss harness 必须输出：

| 字段 | 解释 |
| --- | --- |
| `variantTraceDivergenceRatio` | 现有 action trace 差异 |
| `phaseOverrideSchemaCoverage` | 3 个 variant 的 schema override 覆盖 |
| `phaseOverrideRuntimeTriggerCoverage` | 3 个 variant 的 runtime trigger 覆盖 |
| `phaseOverrideTelegraphCoverage` | override telegraph 是否触发 |
| `phaseOverrideActionDistinctCount` | override 后新增或强化的动作数量 |
| `phaseGraphUnchanged` | 继续保留，用于说明没有完整 graph mutation |

`phaseGraphUnchanged` 解释固定为：

1. `true` 表示 base boss phase graph 的 phase 节点与边未被新增、删除或重连。
2. Phase4 v4 只验证 data-level phase language，因此 `phaseGraphUnchanged=true` 与 `phaseOverrideRuntimeTriggerCoverage=3/3` 同时成立属于正确状态。
3. report 必须同时展示 `phaseOverrideSchemaCoverage`、`phaseOverrideRuntimeTriggerCoverage`、`phaseOverrideTelegraphCoverage` 和 `phaseGraphUnchangedReason=data_level_override_only`，避免被误解为 variant 没有阶段差异。
4. `phaseOverrideTelegraphCoverage` 分母固定为 3 个 variant 的 telegraph spec；3 个 spec 全部被 bossHarness runtime trace 触发才记为 `3/3`。

## 6. 测试与自证

### 6.1 必测行为

1. `boss-variants/index.yaml` 中 3 个 variant 均声明 `phaseOverrides`。
2. `BossVariantDef.phaseOverrides` 的唯一 schema owner 是 `game/src/main/kotlin/com/ktome/game/elites/MutationModels.kt`。
3. `DataLoader.parseBossVariants` 对 phase、trigger expression、trigger references、telegraph、action emphasis、on-enter event key fail fast。
4. 3 个 override telegraph 都能在 boss harness 中触发。
5. `bossVariantPhaseOverrideSchemaCoverage=3/3`，`bossVariantPhaseOverrideRuntimeTriggerCoverage=3/3`，`bossVariantPhaseOverrideTelegraphCoverage=3/3`。
6. `phaseGraphUnchanged` 继续作为说明，不能把本 PR 扩成完整 graph mutation。
7. packaged app 中玩家能看到 variant 专属 warning、阶段进入反馈和 action emphasis 差异。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test bossHarness goldenScreenshot clientSmoke reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr05
```

必须保留以下自证产物：

1. `build/reports/tests/` 中 `BossPhaseManagerTest`、`MutationModelsTest`、`BossVariantDataLoaderTest`、`BossHarnessRunnerTest` 的结果。
2. `tools/build/reports/` 中 `bossHarness` producer 产物。
3. `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` canonical report 产物，且 `reportPhase4Only` 与 `reportPhase4` 对 boss variant phase metrics 读取同一 producer artifact。
4. `client/build/reports/tests/goldenScreenshot/index.html` 中 `goldenScreenshot` 报告，必须包含 3 个 variant warning / telegraph presentation 的可见案例。
5. `client/build/test-results/goldenScreenshot/TEST-com.ktome.client.golden.GoldenScreenshotHarnessTest.xml` 与 `client/build/test-results/clientSmoke/TEST-com.ktome.client.ClientSmokeHarnessTest.xml` 中 client render snapshot 断言结果。
6. `build/reports/verification/` 中 `verifyChanged` 和 `maintainabilityLint` 产物。
7. `build/whitebox/phase4-v4-pr05/evidence/` 中人工白盒截图、日志、manual record。

### 6.3 人工白盒验证流程

本流程必须遵循 `docs/computer-use-whitebox-flow.md`。人工白盒必须使用 packaged app + Computer Use，不得用 IDE、Gradle run 或测试 harness 替代。

已有游戏 Validation Mode 改造要求：

1. 本 PR 必须接入 PR-00 的 `PHASE4_V4_FAST` section，scenario id 固定为 `phase4-v4-pr05`。
2. `prepare-primary-scene` 必须在现有游戏内 validation session 中生成 `boss.variant.molten_glass` 遭遇，并把 Boss 推进到 phase override trigger 前一拍。
3. `prepare-secondary-scene` 必须按 runbook 顺序轮转 `boss.variant.grey_crown` 与 `boss.variant.abyssal_eclipse`，每次都能直接触发对应 warning、telegraph 和 action emphasis。
4. `show-evidence-summary` 必须展示三种 variant 的 coverage、telegraph coverage 和截图清单。
5. Boss variant、phase trigger、telegraph、action emphasis 必须来自 game 层 boss/validation action，不得由 client 写死 warning 文案。

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. scenario id：`phase4-v4-pr05`
4. preset：`BOSS_VARIANT`
5. seed：`2026042435`
6. runtime home：`build/whitebox/phase4-v4-pr05/runtime-home`
7. evidence 目录：`build/whitebox/phase4-v4-pr05/evidence`
8. manual record：`docs/review/phase4/v4-pr/manual-records/phase4-v4-pr05-boss-variant-phase-language.md`

流程：

1. 打包并生成快速白盒材料：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr05
```

2. 执行 `build/whitebox/phase4-v4-pr05/launch-packaged-app.sh` 启动 packaged app，Computer Use 目标 app 固定为 `com.ktome.client`。
3. 按 `build/whitebox/phase4-v4-pr05/cua-runbook.md` 打开 validation overlay，执行 `PHASE4_V4_FAST / prepare-primary-scene`。
4. 将 Boss 生命推进到触发条件，截图记录 `molten_glass_phase_override_warning` 的 visual/audio cue、阶段进入日志和后续 action emphasis。
5. 执行 `PHASE4_V4_FAST / prepare-secondary-scene` 的 `grey_crown` 路径，截图记录 `grey_crown_phase_override_warning` 和 `battlefield_command / ritual_break` 行动倾向。
6. 执行 `PHASE4_V4_FAST / prepare-secondary-scene` 的 `abyssal_eclipse` 路径，截图记录 `abyssal_eclipse_phase_override_warning` 和 `void_breach / abyssal_consecration` 行动倾向。
7. 执行 `PHASE4_V4_FAST / show-evidence-summary`，截图记录 coverage 为 `3/3`。
8. 保存证据：
    - `phase4-v4-pr05-molten-glass-warning.png`
    - `phase4-v4-pr05-grey-crown-warning.png`
    - `phase4-v4-pr05-abyssal-eclipse-warning.png`
    - `phase4-v4-pr05-report-coverage.png`
    - `phase4-v4-pr05-app.log`

通过标准：

1. 三个 variant 在玩家视角都有不同阶段 warning。
2. 三个 variant 的 action emphasis 在战斗日志或行动表现中可分辨。
3. report 证明 coverage 与 telegraph coverage 均为 `3/3`。
4. manual record 写明 packaged app 路径、runtime home、seed、输入序列、截图路径和结论。

### 6.4 统一验证框架关系

本 PR 只新增 data-level phase language，不引入 Phase5 tactical AI。`bossHarness` 是 owner 证据，`goldenScreenshot / clientSmoke / manual record` 是玩家可见性证据；人工白盒不能替代 schema fail-fast、boss harness 和 report owner metrics。

client 可见性要求：

1. render snapshot 与 `goldenScreenshot` 报告必须覆盖 `molten_glass_phase_override_warning`、`grey_crown_phase_override_warning`、`abyssal_eclipse_phase_override_warning` 中每个 variant 至少一个可见案例。
2. warning / telegraph presentation 不得遮挡 player HP、Boss HP、行动日志和当前指令区。
3. `goldenScreenshot` 与 `clientSmoke` 不替代 `bossHarness`，但缺失时本 PR 不满足玩家可见性验收。

### 6.5 玩家体验 Golden Path

1. 玩家遭遇 `molten_glass` variant 时，Boss 进入 enraged phase 后必须出现 molten 专属 warning，而不是只有行动权重变化。
2. 玩家遭遇 `grey_crown` variant 时，必须看到 grey crown warning，并在随后的行动日志中看到 `battlefield_command / ritual_break` 倾向增强。
3. 玩家遭遇 `abyssal_eclipse` variant 时，必须看到 abyssal eclipse warning，并在随后的行动日志中看到 void 相关行动倾向增强。
4. 玩家复盘 validation summary 时，必须看到 schema coverage、runtime trigger coverage、telegraph coverage 和 `phaseGraphUnchangedReason=data_level_override_only`。
5. 任一 trigger reference 缺失时，启动数据加载必须 fail fast，不得进入运行时再静默跳过。

## 7. Report 与验收指标

新增 blocking 指标：

| 指标 | 阈值 | metricKind | producer | ownerBaseline | failSemantics |
| --- | ---: | --- | --- | --- | --- |
| `bossVariantPhaseOverrideSchemaCoverage` | `3/3` | `blockingOwner` | `bossHarness` | `docs/review/phase4/opt/baselines/2026-04-16-phase4-boss-phase-identity-owner-baseline.json` | `fail owner gate` |
| `bossVariantPhaseOverrideRuntimeTriggerCoverage` | `3/3` | `blockingOwner` | `bossHarness` | `docs/review/phase4/opt/baselines/2026-04-16-phase4-boss-phase-identity-owner-baseline.json` | `fail owner gate` |
| `bossVariantPhaseOverrideTelegraphCoverage` | `3/3` | `blockingOwner` | `bossHarness` | `docs/review/phase4/opt/baselines/2026-04-16-phase4-boss-phase-identity-owner-baseline.json` | `fail owner gate` |

新增 report-only 指标：

| 指标 | 事件来源 | 聚合公式 | 分母/范围 | warning floor | metricKind | producer | ownerBaseline | failSemantics |
| --- | --- | --- | --- | ---: | --- | --- | --- | --- |
| `bossVariantPhaseOverrideActionDistinctCount.reportOnly` | `bossHarness` runtime trace 中 phase override 命中后的 emphasized action ids | 对每个 variant 统计 `distinct(actionEmphasisIds observed after override trigger)`；最终报告输出 per-variant count 与 min count | 3 个固定 variant；每个 variant 的 override phase 单独统计 | `>= 1 per variant` | `reportOnlyOwner` | `bossHarness` | `docs/review/phase4/opt/baselines/2026-04-16-phase4-boss-phase-identity-owner-baseline.json` | `warn only` |

新增 supporting 指标：

| 指标 | 用途 | metricKind | producer | ownerBaseline | failSemantics |
| --- | --- | --- | --- | --- | --- |
| `bossVariantStructuralDivergenceNote` | 明确 Phase4 只做 data-level phase language | `supporting` | `bossHarness` | `N/A` | `display only` |
| `phaseGraphUnchangedReason` | 标明 base graph 未变但 override 已触发 | `supporting` | `bossHarness` | `N/A` | `display only` |

## 8. 验证命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test bossHarness goldenScreenshot clientSmoke reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
```

## 9. 完成定义

1. `boss-variants/index.yaml` 的 3 个 variant 均有 `phaseOverrides`。
2. 3 个 override telegraph 都能在 boss harness 中触发。
3. `bossVariantPhaseOverrideSchemaCoverage=3/3`、`bossVariantPhaseOverrideRuntimeTriggerCoverage=3/3` 且 `bossVariantPhaseOverrideTelegraphCoverage=3/3`。
4. report 同时保留 action trace divergence 和 structural phase divergence。
5. `MutationModels.kt` 是 `BossVariantDef.phaseOverrides` 的唯一 schema owner。
6. `DataLoader.parseBossVariants` 对 phase / trigger expression / telegraph / action emphasis 引用 fail fast。
7. `goldenScreenshot` 与 client render snapshot 覆盖 3 个 variant warning / telegraph presentation 的可见案例。
8. `verifyChanged` 覆盖 boss variant schema、data、harness、client presentation 和 report 影响面。
9. 没有新增图片计划文件。
10. 没有新增音频计划文件。

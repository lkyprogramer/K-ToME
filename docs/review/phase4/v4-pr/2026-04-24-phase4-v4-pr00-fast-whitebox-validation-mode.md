> 执行前必须先完整阅读并接受：
> `docs/INDEX.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/computer-use-whitebox-flow.md`
> `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md`
> `docs/review/phase4/v4-pr/README.md`

# Phase4 v4 PR-00 Fast Whitebox Validation Mode

**阶段**: `Phase 4 completion hardening / phase4-v4-pr00`
**优先级**: `P0`
**工作量**: `M`
**合并来源**: v4 PR 文档人工白盒缺口与验证效率债
**前置条件**: Phase4 UI/UX PR 已完成，`ValidationPreset`、`ValidationAction`、validation overlay、`scripts/capture-macos-app-window.sh`、`docs/computer-use-whitebox-flow.md` 可被复用
**资源生成结论**: 不生成图片资源；不生成音频资源

## 1. 玩家体验目标

本 PR 的目标不是新增玩家功能，而是改造已有游戏内 Validation Mode，把 Phase4 v4 后续 PR 的人工白盒验证从“逐项手动找场景”改成“固定 scenario 一键进入游戏内目标状态、packaged app 真实运行、Computer Use 快速留证”。外部脚本只负责启动和建档；真正提效必须来自游戏内 Validation Mode 的 scenario 化、快速 action 和目标状态预置。没有这个前置改造，PR-01 到 PR-07 的人工白盒会重复经历打包、建目录、启动、选 preset、找目标状态、截图命名和 manual record 建档，验证成本高且容易漏证。

完成标准：

1. `preparePhase4V4Whitebox` 生成当前 scenario 的 launch script、CUA runbook、manual record 模板、证据清单和启动参数摘要。
2. packaged app 启动时读取 `-Dktome.validation.scenario=<scenario-id>`，直接进入对应 validation session 或显示明确 fail-fast 错误。
3. 每个 scenario 固定 locale、窗口尺寸、preset、seed、profession、race、zone、floor、content pack、启动 UI 状态和证据文件名。
4. 生成的 launch script 必须使用单一 `JAVA_TOOL_OPTIONS` 启动 `client/build/release/K-ToME.app/Contents/MacOS/K-ToME`，其中必须包含 `-Duser.home=<runtime-home>`、`-Dktome.validation.scenario=<scenario-id>`、`-Dktome.whitebox.root=<whitebox-root>`、`-Dktome.whitebox.evidenceDir=<evidence-dir>`、`-Dktome.whitebox.manualRecord=<manual-record>` 五项。
5. 生成的 CUA runbook 必须只包含当前 PR 要执行的输入序列，不要求验证者临场推断路径。
6. manual record 模板必须包含 packaged app 路径、app executable SHA-256、runtime home、scenario id、seed、preset、CUA steps、截图路径、日志路径和结论。
7. 快速白盒模式不写 profile run summary，不写正式 run save，不进入正式玩家入口，不成为 core/game/tools owner metric 的替代证据。
8. 现有 Validation Mode 新增 `PHASE4_V4_FAST` section；该 section 只在 scenario session 中出现，普通 validation session 不显示。
9. Validation setup 新增 scenario entry；选择 scenario 后自动回填 preset、seed、profession、race、zone、floor、route、content pack。

## 2. 当前问题

1. `docs/computer-use-whitebox-flow.md` 保证了 packaged app + Computer Use 的证据质量，但没有定义 K-ToME v4 PR 的快速 scenario 入口。
2. 当前 validation overlay 已有 `MAPGEN_DIFF / HIDDEN_CONTENT / BOSS_VARIANT / LOOT_LAB / CONTENT_PACK` 和若干快速 action，但 PR-01 到 PR-07 的目标场景仍要靠人工组合 preset、seed、profession、travel、recovery、reward、search、boss action。
3. 每个 PR 都重复声明 runtime-home、evidence、manual record、截图文件名，但没有统一生成器，路径和证据名容易漂移。
4. packaged app 验证里曾出现同 bundle id 旧窗口被 Computer Use 命中的风险；当前流程缺少“当前 packaged app pid 与截图 metadata 必须匹配”的机器生成 runbook。
5. PR-06 这类验证代表性 PR 没有玩家场景，但仍需要通过 packaged app 展示 route diversity summary 和 verification routing 结果，否则人工白盒会退化成读报告。

## 2.1 对已有游戏 Validation Mode 的改造结论

必须改造已有游戏内 Validation Mode。

原因：

1. 现有 preset 只覆盖大类场景，不能保证 PR-01 的职业树断点、PR-02 的铭文满槽替换、PR-03 的 build reward adoption、PR-05 的三种 boss variant 和 PR-07 的 sample pack visibility 都在 1 分钟内抵达。
2. 现有 overlay action 是通用 action，缺少 PR 目标态 materialization；验证者仍要通过移动、战斗、刷资源和开菜单组合目标状态。
3. 只增加外部脚本无法改变游戏内状态抵达成本；脚本能启动 packaged app，但不能让 Talent UI、铭文商店、reward card、hidden search、boss variant、route summary、sample pack summary 自动进入验收状态。
4. 后续 PR 会频繁回归同一批验收面，若不把目标态固定进 Validation Mode，每次 review 修复都会重走长人工路径。

改造边界：

1. 改造对象是现有 `ValidationPreset`、`ValidationSessionOptions`、`ValidationSetupController`、`ValidationCommandSource`、validation overlay 和 validation-only action。
2. 不新增第二套 gameplay 规则。
3. 不让 validation scenario 写入正式 save/profile。
4. 不把 scenario action 接到正式玩家入口。
5. 不把 scenario 截图升级为 owner metric。

## 3. 范围与非目标

### 3.1 范围

生产代码：

- `game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationSessionOptions.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationPhase4Guide.kt`
- `client/src/main/kotlin/com/ktome/client/screen/ValidationScenarioBootstrap.kt`
- `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupController.kt`
- `client/src/main/kotlin/com/ktome/client/input/ValidationCommandSource.kt`
- `client/src/main/kotlin/com/ktome/client/GameApp.kt`
- `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt`
- `build.gradle.kts`
- `tools/build.gradle.kts`

数据与配置：

- `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml`
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`
- `docs/computer-use-whitebox-flow.md`
- `docs/review/phase4/v4-pr/README.md`

测试：

- `game/src/test/kotlin/com/ktome/game/validation/ValidationScenarioRegistryTest.kt`
- `client/src/test/kotlin/com/ktome/client/screen/ValidationScenarioBootstrapTest.kt`
- `client/src/test/kotlin/com/ktome/client/input/ValidationCommandSourceTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt`

### 3.2 非目标

1. 不新增图片资源。
2. 不新增音频资源。
3. 不新增数值型局外成长。
4. 不把 validation scenario 暴露为正式玩家菜单主入口。
5. 不让 scenario 修改 core 规则权威。
6. 不用快速白盒结果替代 owner suite、reportPhase4、goldenScreenshot、clientSmoke、longRunLab 或 content pack harness。
7. 不引入 Lua、脚本宿主或 runtime mod DSL。

## 4. 资源要求

### 4.1 图片资源

不生成新图片资源。

执行要求：

1. 快速白盒入口只新增文本、overlay rows、脚本和 Markdown 记录。
2. 所有截图来自 packaged app 真实窗口，不从新增视觉素材生成。
3. 若 scenario overlay 需要图标，复用现有 validation / UI icon key。

### 4.2 音频资源

不生成新音频资源。

执行要求：

1. scenario 启动、action 执行和错误反馈复用已有 validation UI 音频。
2. 本 PR 不新增 audio plan、generation report、processing report。
3. `audioLint` 必须证明没有新增未解析 audio key。

## 5. 技术方案

### 5.1 Scenario 模型

新增 typed model：

```kotlin
data class ValidationScenarioId(
    val value: String,
)

data class ValidationScenarioDef(
    val id: ValidationScenarioId,
    val prId: String,
    val titleKey: String,
    val preset: ValidationPreset,
    val seed: Long,
    val locale: GameLocale,
    val windowWidth: Int,
    val windowHeight: Int,
    val professionId: String,
    val raceId: String,
    val zoneId: String,
    val floor: Int,
    val routeIndex: Int,
    val contentPackMode: ValidationScenarioContentPackMode,
    val startupMode: ValidationScenarioStartupMode,
    val initialOverlaySection: ValidationOverlaySection,
    val requiredEvidenceFiles: List<String>,
)
```

固定枚举：

```kotlin
enum class ValidationScenarioStartupMode {
    VALIDATION_SETUP,
    DIRECT_VALIDATION_SESSION,
}

enum class ValidationScenarioContentPackMode {
    NONE,
    SAMPLE_PACK_ENABLED,
}
```

执行要求：

1. `ValidationScenarioDef` 只描述 validation 启动和证据需求，不描述正式玩法规则。
2. `ValidationScenarioRegistry` 从 typed Kotlin registry 加载官方 scenario，再与 `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml` 做 id parity 校验。
3. 未注册 scenario id 必须 fail fast，错误文本包含 scenario id、合法 id 列表和调用参数。
4. scenario id 必须保持 repo-wide 唯一。
5. scenario 启动必须走 `GameModule.newValidationSession`，不得绕过正式 session 装配。
6. `routeIndex = -1` 表示当前 scenario 不绑定 long-run corpus；PR-06 的 route summary scenario 使用 `routeIndex=0..15` 指向 `LONG_RUN_SMOKE` final corpus。
7. `ValidationScenarioStartupMode` 不包含 report summary 专用枚举；report/evidence 摘要必须通过 `PHASE4_V4_FAST / show-evidence-summary` action 打开。

### 5.1.1 现有 Validation Mode 具体改造点

必须改造以下游戏内路径：

1. `ValidationSetupEntryId` 新增 `SCENARIO`，显示当前 scenario id。
2. `ValidationSetupController` 在选择 scenario 时同步回填 preset、seed、profession、race、zone、floor、route、boss variant mode、preferred variant、sample content pack。
3. `ValidationSessionOptions` 新增 `scenarioId: ValidationScenarioId?`，并写入 `ValidationSummarySnapshot`。
4. `ValidationPhase4Guide` 在 scenario session 中展示当前 PR 的目标、快速路径和证据清单。
5. `ValidationOverlaySection` 新增 `PHASE4_V4_FAST`，只在 `scenarioId != null` 的 session 中显示。
6. `ValidationCommandSource` 为 `PHASE4_V4_FAST` section 生成当前 scenario 的 action 列表。
7. `ValidationAction` 新增 `Phase4V4ScenarioAction(scenarioId, actionId)`，由 game 层根据 scenario id 分派到正式 validation-only materialization 逻辑。
8. `FoundationGameSession.perform(PlayerCommand.Validation(...))` 继续作为唯一执行入口；client 不直接改规则状态。
9. `RenderSnapshot` 或 validation summary 中暴露 `scenarioId`、`scenarioTitleKey`、`requiredEvidenceKeys`，供 overlay 和截图验收读取。

`PHASE4_V4_FAST` 固定 action contract：

| action id | 固定用途 |
| --- | --- |
| `prepare-primary-scene` | 把当前 PR 的主验收状态 materialize 到当前 validation session |
| `prepare-secondary-scene` | 把当前 PR 的第二验收状态 materialize 到当前 validation session |
| `show-evidence-summary` | 打开当前 PR 的 evidence checklist / report summary 面 |
| `reset-scenario` | 按当前 scenario id 重启 validation session |

执行规则：

1. 每个 action 都必须输出 `log.validation.phase4_v4.action`，参数包含 `scenarioId`、`actionId`、`result`。
2. action 失败必须在日志和 overlay last result 中显示 typed failure reason。
3. `prepare-primary-scene` 和 `prepare-secondary-scene` 的具体效果由当前 PR 实现，但 action id 不变。
4. PR-01 到 PR-07 不得新增并行 overlay section；全部挂入 `PHASE4_V4_FAST`。
5. `prepare-primary-scene` 与 `prepare-secondary-scene` 必须幂等；重复执行等价于先执行 `reset-scenario` 再 materialize 目标场景。
6. `show-evidence-summary` 只读展示 scenario、evidence checklist、producer freshness 和 app hash，不修改 game state。
7. `reset-scenario` 必须清空 validation-only materialized state，并用同一 scenario seed 重建 session。

### 5.2 PR-00 固定 scenario id

PR-00 交付 selftest scenario、scenario registry 合同和 fail-fast 合同。PR-01 到 PR-07 必须在各自 PR 内补齐对应 scenario 的 domain action 与验收断言。

| PR | scenario id | 默认 preset | seed | routeIndex | contentPackMode | 默认职业 | 目标场景 |
| --- | --- | --- | ---: | ---: | --- | --- | --- |
| PR-00 | `phase4-v4-pr00-selftest` | `MAPGEN_DIFF` | `2026042430` | `-1` | `NONE` | `vanguard` | scenario 启动、action 幂等、错误页、证据清单、app hash |
| PR-01 | `phase4-v4-pr01` | `MAPGEN_DIFF` | `2026042431` | `-1` | `NONE` | `vanguard` | 职业树学习、升 rank、Tier 锁定、active/reserve |
| PR-02 | `phase4-v4-pr02` | `LOOT_LAB` | `2026042432` | `-1` | `NONE` | `rogue` | 铭文购买、第三槽、满槽替换、取消不扣资源 |
| PR-03 | `phase4-v4-pr03` | `LOOT_LAB` | `2026042433` | `-1` | `NONE` | `arcanist` | build identity reward、非武器 payoff、approved debt 消失 |
| PR-04 | `phase4-v4-pr04` | `HIDDEN_CONTENT` | `2026042434` | `-1` | `NONE` | `arcanist` | 搜索锚点、Search 反馈、zone hook、优先级不重叠 |
| PR-05 | `phase4-v4-pr05` | `BOSS_VARIANT` | `2026042435` | `-1` | `NONE` | `vanguard` | 三个 Boss variant 的 warning、phase language、report coverage |
| PR-06 | `phase4-v4-pr06` | `MAPGEN_DIFF` | `2026042436` | `0` | `NONE` | `rogue` | route diversity summary、hash diversity、verifyChanged routing |
| PR-07 | `phase4-v4-pr07` | `CONTENT_PACK` | `2026042437` | `-1` | `SAMPLE_PACK_ENABLED` | `arcanist` | sample pack active summary、secret route、touched ids、key resolution |

### 5.3 启动协议

packaged app 启动时读取以下 system properties：

| Property | 固定含义 |
| --- | --- |
| `ktome.validation.scenario` | 当前 scenario id |
| `ktome.whitebox.root` | `build/whitebox/<scenario-id>` |
| `ktome.whitebox.evidenceDir` | `build/whitebox/<scenario-id>/evidence` |
| `ktome.whitebox.manualRecord` | 当前 PR manual record repo-relative path |

启动规则：

1. 存在 `ktome.validation.scenario` 时，client 必须先解析 scenario，再启动 validation session。
2. scenario 解析失败时，app 必须停在错误页，错误页提供 copy detail payload。
3. scenario 启动成功后，主画面直接进入 validation session；`F9` 打开 overlay 后，默认选中 scenario 指定 section。
4. scenario mode 下 `Ctrl+S` 必须输出 `ui.message.save.blocked-in-validation`。
5. scenario mode 下 profile persistence 固定为 `NO_OP`。
6. scenario mode 下 runtime home 固定来自 launch script，不读取本机真实 home。

错误页 payload 固定包含：

1. `scenarioId`。
2. `knownScenarioIds`。
3. `startupProperties`。
4. `validationErrorCode=UNKNOWN_PHASE4_V4_SCENARIO`。
5. `manualRecordPath`。
6. `expectedEvidencePath`。

错误页 i18n key 固定为：

| key | 用途 |
| --- | --- |
| `validation.phase4.v4.error.unknown_scenario.title` | 错误页标题 |
| `validation.phase4.v4.error.unknown_scenario.body` | 错误正文 |
| `validation.phase4.v4.error.copy_detail` | copy detail 按钮 |

### 5.4 `preparePhase4V4Whitebox` 任务

新增 root Gradle task：

```bash
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr00-selftest
```

输入：

1. `-Pktome.whitebox.scenario=<scenario-id>`
2. `client/build/release/K-ToME.app`
3. `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml`

property 映射合同：

1. `preparePhase4V4Whitebox` 只读取 Gradle project property `ktome.whitebox.scenario`。
2. 生成的 `launch-packaged-app.sh` 必须把该值写入 JVM system property `ktome.validation.scenario`。
3. packaged app runtime 只读取 `ktome.validation.scenario`，不读取 `ktome.whitebox.scenario`。

输出：

```text
build/whitebox/<scenario-id>/
  launch-packaged-app.sh
  cua-runbook.md
  manual-record-template.md
  expected-evidence.json
  app-executable.sha256
  runtime-home/
  evidence/
```

生成规则：

1. `launch-packaged-app.sh` 必须把 `user.home`、`ktome.validation.scenario`、`ktome.whitebox.root`、`ktome.whitebox.evidenceDir`、`ktome.whitebox.manualRecord` 全部写成独立 `-D` 并拼入同一个 `JAVA_TOOL_OPTIONS`；除 `JAVA_TOOL_OPTIONS` 外不得再以命令行参数或 shell 环境变量形式传递这些 key。
2. `cua-runbook.md` 必须列出启动、连接 `com.ktome.client`、执行输入、截图、metadata 校验、manual record 回填的步骤。
3. `manual-record-template.md` 必须按 `docs/computer-use-whitebox-flow.md` 字段生成。
4. `expected-evidence.json` 必须列出当前 PR 要产出的截图、日志、metadata、sha256 和 manual record。
5. app executable SHA-256 必须写入 `app-executable.sha256`。
6. 输出路径全部 repo-relative，不写入 `/Users`、`/tmp` 或机器私有路径。
7. `launch-packaged-app.sh` 启动前必须重新计算 app executable SHA-256，并与 `app-executable.sha256` 匹配；不匹配时 fail fast，不启动 packaged app。

### 5.5 CUA runbook 固定结构

`cua-runbook.md` 固定包含以下段落：

1. Scenario summary
2. Launch command
3. Computer Use target
4. Starting state assertions
5. Input sequence
6. Screenshot capture commands
7. Metadata and SHA-256 checks
8. Manual record write-back
9. Failure retention

每个输入步骤固定使用表格：

| Step | Mode | Input | Expected visible result | Evidence file |
| --- | --- | --- | --- | --- |

执行要求：

1. 每个 scenario 至少 4 个截图证据。
2. 每张截图必须有 `.metadata.txt` 和 `.sha256` sidecar。
3. metadata 中 `window_owner`、`window_pid`、`window_bounds` 必须对应本次 packaged app。
4. `app.log` 必须记录 scenario id、preset、seed、profession、race、zone、floor。
5. CUA runbook 只写固定输入步骤，不写临场探索步骤。

### 5.6 PR-specific scenario 接入规则

PR-01 到 PR-07 在各自实现中必须完成：

1. 在 `phase4-v4-scenarios.yaml` 注册当前 `scenario id`。
2. 在 `ValidationScenarioRegistryTest` 中断言 scenario id、preset、seed、profession、content pack mode 与证据文件名。
3. 在 game 层实现当前 PR 的 `Phase4V4ScenarioAction` materialization，不在 client 伪造状态。
4. 在 `ValidationCommandSourceTest` 中断言 overlay 暴露当前 PR 的 action label。
5. 在 manual record 中写明本轮使用的是 `preparePhase4V4Whitebox` 生成的 runbook。

### 5.7 不允许的加速方式

1. 不允许用 IDE 启动代替 packaged app。
2. 不允许用 Gradle `run` 代替 packaged app。
3. 不允许用单元测试截图代替 CUA 目标窗口截图。
4. 不允许绕过正式 `GameModule.newValidationSession` 直接构造 client-only 假 snapshot。
5. 不允许把 scenario action 接入正式玩家菜单。
6. 不允许把 scenario 结果写入 canonical owner evidence。

## 6. 测试与自证

### 6.1 必测行为

1. 未传入 `ktome.whitebox.scenario` 时，`preparePhase4V4Whitebox` fail fast，并列出合法 scenario id。
2. 传入合法 scenario id 后，任务生成 `launch-packaged-app.sh`、`cua-runbook.md`、`manual-record-template.md`、`expected-evidence.json`、`app-executable.sha256`。
3. 生成的 launch script 使用 repo-relative runtime-home 和 evidence 目录。
4. packaged app 启动时识别 `ktome.validation.scenario`，直接进入对应 validation session。
5. 错误 scenario id 在 packaged app 中显示 copy detail 错误页，不进入普通主菜单。
6. `phase4-v4-scenarios.yaml` 与 `ValidationScenarioRegistry` 的 scenario id 完全一致。
7. CUA runbook 包含 `docs/computer-use-whitebox-flow.md` 要求的 packaged app、runtime home、Computer Use target、seed、preset、输入序列和证据路径。
8. `Ctrl+S` 在 scenario validation session 中被阻止，并输出 `ui.message.save.blocked-in-validation`。
9. `preparePhase4V4Whitebox` 不修改 `docs/review/phase4/v4-pr/manual-records/*.md`，只生成模板；真实 manual record 由执行者按证据回填。
10. `prepare-primary-scene` 连续执行两次后，scenario state、日志关键事件和 evidence checklist 与单次执行一致。
11. `show-evidence-summary` 连续执行两次不改 player hp、位置、背包、技能、铭文、zone hook 或 boss state。
12. `launch-packaged-app.sh` 的 executable SHA-256 校验失败时直接退出，并在终端输出 `APP_HASH_MISMATCH`。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest :client:test --tests com.ktome.client.screen.ValidationScenarioBootstrapTest :client:test --tests com.ktome.client.input.ValidationCommandSourceTest :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:packageMacApp preparePhase4V4Whitebox maintainabilityLint verifyChanged -Pktome.whitebox.scenario=phase4-v4-pr00-selftest
```

必须保留以下自证产物：

1. `tools/build/reports/tests/` 中 `Phase4V4WhiteboxScenarioCliTest` 结果。
2. `client/build/reports/tests/` 中 `ValidationScenarioBootstrapTest` 与 `ValidationCommandSourceTest` 结果。
3. `game/build/reports/tests/` 中 `ValidationScenarioRegistryTest` 结果。
4. `build/whitebox/phase4-v4-pr00-selftest/` 中生成的 launch script、CUA runbook、manual record template、expected evidence 和 executable hash。
5. `build/reports/verification/` 中 `maintainabilityLint`、`verifyChanged` 产物。

### 6.3 人工白盒验证流程

本流程必须遵循 `docs/computer-use-whitebox-flow.md`。人工白盒必须使用 packaged app + Computer Use，不得用 IDE、Gradle run 或测试 harness 替代。

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. scenario id：`phase4-v4-pr00-selftest`
4. preset：`MAPGEN_DIFF`
5. seed：`2026042430`
6. runtime home：`build/whitebox/phase4-v4-pr00-selftest/runtime-home`
7. evidence 目录：`build/whitebox/phase4-v4-pr00-selftest/evidence`
8. manual record 模板：`build/whitebox/phase4-v4-pr00-selftest/manual-record-template.md`

流程：

1. 打包并生成快速白盒材料：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr00-selftest
```

2. 执行 `build/whitebox/phase4-v4-pr00-selftest/launch-packaged-app.sh` 启动 packaged app。
3. 用 Computer Use 连接 `com.ktome.client`，确认窗口属于 `client/build/release/K-ToME.app` 当前进程。
4. 按 `build/whitebox/phase4-v4-pr00-selftest/cua-runbook.md` 执行输入序列。
5. 使用 `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --out build/whitebox/phase4-v4-pr00-selftest/evidence/phase4-v4-pr00-scenario-bootstrap.png` 保存截图。
6. 检查截图 sidecar metadata，确认 `capture_mode=macos-window-id` 且 `window_pid` 对应本轮 packaged app pid。
7. 将 `manual-record-template.md` 内容写入当前 PR 的 manual record，并记录实际结论。

通过标准：

1. packaged app 直接进入 `phase4-v4-pr00-selftest` validation session。
2. `F9` 打开 validation overlay 后，scenario summary 显示 `phase4-v4-pr00-selftest`、`MAPGEN_DIFF`、`2026042430`。
3. `build/whitebox/phase4-v4-pr00-selftest/cua-runbook.md` 不包含临场探索步骤。
4. `expected-evidence.json` 中列出的证据文件全部有路径、metadata 和 SHA-256 规则。
5. manual record 模板字段满足 `docs/computer-use-whitebox-flow.md`。

### 6.4 统一验证框架关系

本 PR 是验证效率前置改造，不新增 gameplay owner metric。`preparePhase4V4Whitebox` 只生成 packaged app 白盒执行材料；`verifyChanged`、`maintainabilityLint`、client/game/tools 测试证明验证模式自身不破坏现有 validation 合同。PR-01 到 PR-07 的 owner gate 仍由各 PR 自己声明。

## 7. Report 与验收指标

本 PR 不新增 Phase4 report owner metric。

新增 supporting 字段只出现在生成材料中：

| 字段 | 固定含义 | metricKind | producer | ownerBaseline | failSemantics |
| --- | --- | --- | --- | --- | --- |
| `whiteboxScenarioId` | 当前快速白盒 scenario id | `supporting` | `preparePhase4V4Whitebox` | `N/A` | `display only` |
| `whiteboxScenarioEvidencePlanPath` | `expected-evidence.json` 路径 | `supporting` | `preparePhase4V4Whitebox` | `N/A` | `display only` |
| `whiteboxScenarioRunbookPath` | `cua-runbook.md` 路径 | `supporting` | `preparePhase4V4Whitebox` | `N/A` | `display only` |

## 8. 验证命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest :client:test --tests com.ktome.client.screen.ValidationScenarioBootstrapTest :client:test --tests com.ktome.client.input.ValidationCommandSourceTest :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:packageMacApp preparePhase4V4Whitebox maintainabilityLint verifyChanged -Pktome.whitebox.scenario=phase4-v4-pr00-selftest
```

## 9. 完成定义

1. `preparePhase4V4Whitebox` root task 存在并能按 scenario id 生成白盒执行材料。
2. packaged app 能通过 `ktome.validation.scenario` 直接进入 validation scenario。
3. CUA runbook、manual record template、expected evidence、launch script、app executable hash 全部生成。
4. 生成路径全部 repo-relative。
5. `docs/computer-use-whitebox-flow.md` 写明 fast scenario 接入规则。
6. PR-01 到 PR-07 文档均引用 PR-00，并声明各自 scenario id。
7. 快速白盒模式不替代任何 owner gate。
8. 没有新增图片计划文件。
9. 没有新增音频计划文件。

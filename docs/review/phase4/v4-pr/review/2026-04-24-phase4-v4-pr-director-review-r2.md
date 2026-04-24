# Phase4 v4-pr 二轮开发文档深度审阅报告

**审阅日期**: 2026-04-24
**审阅对象**: `docs/review/phase4/v4-pr/README.md` + `PR-00` ~ `PR-07` 共 8 份 PR 级开发文档
**一轮报告**: `docs/review/phase4/v4-pr/review/2026-04-24-phase4-v4-pr-director-review.md`（25 条硬阻塞 + 若干 Major/Minor/Nit）
**审阅角色**: 资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
**输出目的**: 对一轮报告发出后的修订版文档进行第二轮深度审阅；识别一轮修复引入的次生问题、之前未触及的薄弱点、跨 PR 不一致与实施层细节缺失。

---

## §0 总判

一轮提出的 25 条 Blocker 全部在修订版中有对应改动落地，方向正确，结构决策足够到位。但本轮修复过程中引入了一批**新的次生深度问题**，以及一批一轮没覆盖到的细节缺失，集中在：

1. **PR-00 作为全新交付物，自身存在 2 个 Blocker + 4 个 Major**；一轮只把它标记为“新增必要前置”，没有对其内部设计做深审。
2. **PR-00 scenario seed 与 PR-06 long-run corpus seed 发生命名空间冲突**（7 个 seed 完全重合），无法用同一 seed 字符串同时锚定 validation scenario 与 long-run corpus。
3. **PR-05 的 variant trigger（`void_pressure_active` / `oil_or_fire_seen` / `war_caller_active`）原产者缺失或需依赖 PR-04**，但 README §6 串行守则没有声明 PR-05 必须在 PR-04 之后。
4. **PR-03 的 capstone × profession 权重矩阵只覆盖 6 个 item**，对 `data/build-identity/index.yaml` 中另外 7 个 item（`unique_quenchbreaker_maul`、`artifact_heartroot_gambit` 等）没有声明评分动作。
5. **PR-03 新增指标 `topFiveAffixExposureShare <= 40%` 与单项 affix `<= 15%` 上限之间存在算术上的不可同时满足**。
6. **PR-06 `2026042406` 被标为 `full_route` 但路线穿越 `crystal_cavern`**，与“full_route 只走 mandatory zone”隐式口径冲突。
7. **PR-07 `secondarySecretSlot` / `fixedSeedVisibilityCase` force-inject 机制跨 PR 不对齐**，且引入 production 代码的 test-only 后门。
8. **`ValidationScenarioRegistryTest.kt` 与 `Phase4MetricCatalog.kt` 被 7 条 PR 同时修改**，串行 merge 冲突面严重，README §6 串行守则未给出冲突消解序。

以下按 §1~§10 逐项展开。评级沿用一轮标准：

* **Blocker**：必须在合入前修复，否则下游 PR 无法开工或 release-facing 指标不成立。
* **Major**：影响实现正确性 / 可验证性，应在 PR 进入代码前修正。
* **Minor**：影响可读性 / 边界行为的小缺口，可在 PR 内顺手处理。
* **Nit**：文字、排版、格式一致性问题。

---

## §1 README 二轮复核

### §1.1 已修复项（sampled）

1. 一轮 B-README-1：Phase4 UI/UX PR 与 v4-pr 的边界 → README §2 执行规则 2 已写明“UI/UX PR 是完成前提”。✅
2. 一轮 B-README-2：`frozen profession` 分母口径 → README §2.2 三档 tier（BASE / ADVANCED / FROZEN）并给出 `release-facing blocking metric` 口径。✅
3. 一轮 B-README-3：PR-00 缺失 → README 顺序表首行、§2 规则 10/11/12 已加入 PR-00 引用。✅
4. 一轮 B-README-4：验证入口统一 → §4 / §4.1 给出 owner suite 与快路径。✅
5. 一轮 M-README-1：i18n key / UI snapshot 纪律 → §5.2 / §5.3 新增。✅

### §1.2 二轮新发现

#### §1.2.1 **Blocker** README §6 串行守则未声明 PR-05 对 PR-04 的依赖

README §6 item 2 写：“PR-04、PR-05 与 PR-07 允许在 PR-01 之后并行开发”。但 PR-05 §5.2 `boss.variant.abyssal_eclipse` 的 trigger 之一是 `zone.trigger.void_pressure_active`，而 `void_pressure` 是 PR-04 §5.2 定义的 `abyssal_temple` runtime hook。PR-05 无法在没有 PR-04 的情况下完成 runtime trigger coverage（`bossVariantPhaseOverrideRuntimeTriggerCoverage=3/3`）。

**修复方向**：§6 必须声明 PR-05 串行依赖 PR-04（`abyssal_eclipse` 依赖 `void_pressure_active`），或要求 PR-04 先合入；不允许 PR-04 / PR-05 纯并行。

#### §1.2.2 **Major** README §6 未覆盖 PR-07 对 PR-04 `secondarySecretSlot` 的依赖

PR-07 §5.2 写 `hiddenBranchBindings` 使用 `secondarySecretSlot`，但 PR-04 §5.1 只说 `abyssal_temple / underground_river` 保留 secret entry detour node，没有把 `secondarySecretSlot` 抽成命名概念。若 `secondarySecretSlot` 是 PR-04 新抽象，README §6 串行守则漏写；若是 PR-07 新建，跨 PR 语义引用不一致。

**修复方向**：PR-04 §5.1 必须显式提出 `secretZoneSelector.primarySlot / secondarySlot` 概念与默认行为，再由 PR-07 扩展；或把该抽象整体迁入 PR-07 并在 §6 明确串行。

#### §1.2.3 **Major** 共享测试文件 / catalog 文件的串行冲突未声明

以下文件在 README §6 守则中没有冲突消解机制，但会在 7 条 PR 同时修改：

| 文件 | 修改 PR |
| --- | --- |
| `game/src/test/kotlin/com/ktome/game/validation/ValidationScenarioRegistryTest.kt` | PR-00 建立 + PR-01~07 各自 assert scenario id / preset / seed / profession / evidence |
| `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4MetricCatalog.kt` | PR-01 / PR-02 / PR-03 / PR-04 / PR-05 / PR-06 / PR-07 |
| `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerMetricTargets.kt` | PR-01 / PR-02 / PR-03 / PR-04 / PR-05 / PR-06 |
| `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4OwnerBaselineRegistry.kt` | PR-01 / PR-02 / PR-03 / PR-04 / PR-05 / PR-06 |
| `tools/src/main/resources/phase4/aggregation-manifest.yaml` | PR-01 / PR-02 / PR-03 / PR-06 |

**修复方向**：§6 增加“共享文件串行写入序”，规定每次只有一个 PR 可以打开 catalog / test / manifest 文件；并且一旦 PR-01 合入，所有下游 PR 必须 rebase 刷新 registry 引用。或要求拆分 catalog（每个 PR 拥有独立 sub-catalog）。

#### §1.2.4 **Minor** README §2 规则 14 与破坏性纪律与旧 `reportOnly` 保留项可能矛盾

§2 规则 14 宣布“本轮 PR 不承担旧 run save / 旧 replay / 旧 report id / 旧 schema / 旧 command payload 的兼容”。但 PR-04 §5.4 保留了多个 `*.reportOnly` 字段（`perZoneSecretConversionFloor.reportOnly` 等）。`.reportOnly` 后缀本身是旧 report-only owner 指标的 canonical 形式，不等同于“旧 id”，但字样容易误读。

**修复方向**：§2 规则 14 明确表述“旧 = 一轮审阅前已弃用、且本轮文档不再声明的 id”；`.reportOnly` 作为 metric kind 后缀仍然是 canonical，不属于“旧”。

#### §1.2.5 **Minor** README §4 owner suite 的 PR-00 入口与其他 PR 不对齐

§4 表里 PR-00 的最小 owner suite 使用 `-Pktome.whitebox.scenario=phase4-v4-pr01`，但 PR-00 自身的合入证据应该能在不依赖 PR-01 materialization 的前提下完成。推荐新增 `phase4-v4-pr00-selftest` scenario id 作为 PR-00 自己的 scenario，避免循环。

**修复方向**：参见 §2 中 PR-00-B-01 的根因处理。

#### §1.2.6 **Nit** §2.2 tier 表末尾“进入 report-only coverage”的英文 / 中文混用

`ADVANCED` 行写 `进入 report-only coverage，不纳入 release-facing blocking 分母`。`release-facing blocking` 与 `report-only coverage` 是英文名词；保持一致风格，建议统一小写或全部包裹在反引号里。

---

## §2 PR-00 Fast Whitebox Validation Mode（全新文档首次深审）

一轮报告仅标记 PR-00 为必要前置 PR，未对内部设计深审。本轮全量审计。

### §2.1 肯定项

1. `ValidationScenarioDef` 结构严谨，固定了 preset / seed / locale / window / profession / race / zone / floor / route / contentPackMode 九类启动参数。✅
2. `ValidationScenarioStartupMode` 与 `ValidationScenarioContentPackMode` enum 单独定义，没有在 `Boolean` 和 `Enum` 之间含糊。✅
3. `PHASE4_V4_FAST` section 的 action id 固定为 4 项（`prepare-primary-scene` / `prepare-secondary-scene` / `show-evidence-summary` / `reset-scenario`），统一了 7 条下游 PR 的 overlay action 命名空间。✅
4. `preparePhase4V4Whitebox` 任务输入输出合同清晰，包含 `app-executable.sha256`。✅
5. §5.7 明确列出 6 条“不允许的加速方式”，把 PR-00 限制在“只缩短场景抵达，不变更 owner metric 语义”的边界内。✅

### §2.2 二轮新发现

#### §2.2.1 **Blocker** PR-00 scenario seed 与 PR-06 long-run corpus seed 完全撞表

PR-00 §5.2 为 7 个下游 PR 分配 scenario seed：

```
pr01=2026042401, pr02=2026042402, pr03=2026042403, pr04=2026042404,
pr05=2026042405, pr06=2026042406, pr07=2026042407
```

PR-06 §5.1 corpus 前 7 条：

```
2026042401 full_route, 2026042402 full_route, 2026042403 full_route,
2026042404 full_route, 2026042405 full_route, 2026042406 full_route,
2026042407 full_route
```

**同一 Long 数字** 被用于两个语义不同的用途：

* PR-00 scenario：preset = `MAPGEN_DIFF / LOOT_LAB / HIDDEN_CONTENT / BOSS_VARIANT / CONTENT_PACK`，进入 validation session。
* PR-06 corpus：preset = `LONG_RUN_SMOKE`（headless harness run），无 validation UI。

两者共享 seed 但 preset 不同，`FoundationGameSession` 生成的 RNG / zone rotation / encounter pressure 会不同，导致：

1. 开发者读 `phase4Report` 中看到 `longRunLab` 的 `2026042401` 路径 与 validation scenario `phase4-v4-pr01` 的路径不一致但看起来应该一致（人类读图噪音）。
2. `HeadlessRunHarness` 与 `ValidationSession` 对“seed 所指代的 run”的心智模型冲突，调试时难以复盘。
3. 如果将来打算让 validation scenario 复用 long-run corpus 第 N 条 route 作为目标状态 anchor（现在不这么做，但架构暗示可以），seed 撞表会直接让这条路径破功。

**修复方向（任选一条）**：
* (A) PR-00 改为 `2026042430~2026042436`（+30 偏移），或给 validation scenario seed 加命名空间前缀（`whitebox.20260424.01` 而不是纯 Long）；
* (B) README §4.1 / §6 声明 “seed namespace 按用途分段”，long-run `2026042401~2026042416`，scenario `2026042420~2026042427`，rest-reserved。

#### §2.2.2 **Blocker** PR-00 acceptance 命令与 PR-01~07 materialization 存在循环依赖

README §4 PR-00 最小 owner suite 与 PR-00 §6.2 / §6.3 / §8 都要求 `preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01`。但 `phase4-v4-pr01` 的 `prepare-primary-scene` materialization（“`vanguard` 有 1 点职业天赋点，Talent UI 三列树可打开，starter 三技能 learned，第 4 职业行动槽为空，至少 1 个同职业技能为 LEARNABLE”）定义在 PR-01 §6.3。

如果 PR-00 先合入（它确实是前置 PR），跑 `-Pktome.whitebox.scenario=phase4-v4-pr01` 会因为 `ValidationScenarioRegistry` 还没注册 `phase4-v4-pr01` 条目或 `Phase4V4ScenarioAction` 还没实现 pr01 branch 而 fail fast。即 PR-00 的 acceptance 要求 PR-01 的代码存在，这与 §1 规则“PR-00 是前置 PR”矛盾。

**修复方向**：
* PR-00 新建 `phase4-v4-pr00-selftest` scenario（只测试 scenario infra 自身：registry 存在、launch script 生成、overlay section 展示、`Ctrl+S` 阻塞、`ValidationScenarioRegistryTest` pass），把 PR-00 acceptance 命令改成 `-Pktome.whitebox.scenario=phase4-v4-pr00-selftest`。
* PR-00 §5.2 改成“PR-00 自带 1 个 selftest scenario；PR-01~PR-07 在各自 PR 内注册 `phase4-v4-pr01~07`”。
* README §4 表格 PR-00 行同步修正。

#### §2.2.3 **Major** `ValidationScenarioDef.routeIndex` 含义在整个 PR-00 / PR-01~07 中未定义

PR-00 §5.1 声明字段 `val routeIndex: Int`，但：

1. §5.2 PR scenario id 表 没有 `routeIndex` 列；
2. §5.1.1 现有 Validation Mode 改造点 没提 `routeIndex` 如何驱动；
3. 无任何 PR 写明 `routeIndex` 与 `visitedZoneIdsWithSecretMarkers` 的对应关系。

推测原意是：`routeIndex` 选择 `LONG_RUN_SMOKE` corpus 中某条 pre-canned route。但若是这样，它只对 PR-06 有意义，其他 6 个 PR 的 scenario 都应该固定 `routeIndex=0` 或 `routeIndex=-1`。

**修复方向**：§5.1 写明 `routeIndex` 的语义与取值表；或删除此字段，改用 `targetSceneAnchor: SceneAnchor` 等更具象的字段。

#### §2.2.4 **Major** `ValidationScenarioStartupMode.REPORT_SUMMARY_SCREEN` 无对应 PR 使用

§5.1 enum 列了 3 种 startup mode：`VALIDATION_SETUP` / `DIRECT_VALIDATION_SESSION` / `REPORT_SUMMARY_SCREEN`。但 PR-01~PR-07 的 §6.3 全部是 `DIRECT_VALIDATION_SESSION`（F9 打开 overlay），没有一个用 `REPORT_SUMMARY_SCREEN`。PR-06 最像（它 materialize 的是 route diversity summary 面），但也是通过 validation overlay 显示，不是独立 report screen。

这意味着 `REPORT_SUMMARY_SCREEN` 是幽灵枚举。要么 PR-06 / PR-07 改为显式用它，要么从 enum 删除。

**修复方向**：确认 PR-06 `scenarioTypeDistribution` / `zoneRouteHashDistribution` 展示是走 validation overlay 还是 report screen；二选一并在 §5.1 写明。

#### §2.2.5 **Major** PR-00 没有给出“未知 scenario id” 的 UX 失败态详细

§5.3 item 2 只说 “scenario 解析失败时，app 必须停在错误页，错误页提供 copy detail payload”。但没写：

1. 错误页 i18n key（`ui.error.validation.scenario.unknown`？）；
2. 错误页的证据截图在 `expected-evidence.json` 中是否出现；
3. 错误 scenario 是否仍写入 `app.log`。

测试 §6.1 item 5 断言 “错误 scenario id 在 packaged app 中显示 copy detail 错误页”，但没有给出详细消息格式。可能导致实现方自由发挥。

**修复方向**：§5.3 增加错误页 i18n key 与字段清单；§6.1 补充对应断言。

#### §2.2.6 **Major** `PHASE4_V4_FAST` 固定 action contract 没有 idempotency 合同

§5.1.1 action contract 4 项全部没有说明“重复调用”行为：

* `prepare-primary-scene` 连续调用两次，第二次是否 reset？
* `reset-scenario` 在 primary 之后调用，是否丢弃已产生的 overlay state？
* `show-evidence-summary` 是 read-only 还是会写日志？

7 个下游 PR 各自实现同样的 action id，若幂等性策略不一致，人工白盒 evidence 会漂移（例如：某 PR 的 primary 调 2 次就崩，另一个 PR 调 2 次没事）。

**修复方向**：§5.1.1 item 列表中新增 item 5：“所有 action 必须幂等；重复 `prepare-*` 等价于先 `reset-scenario` 再 `prepare-*`；`show-evidence-summary` 只读”。

#### §2.2.7 **Minor** `Ctrl+S` blocked 的测试断言缺失

§5.3 item 4 要求 scenario mode 下 `Ctrl+S` 输出 `ui.message.save.blocked-in-validation`，但 §6.1 9 条必测行为中没有对应断言。至少应在 `ValidationCommandSourceTest` 或 client test 中断言“scenario session 中 PlayerCommand.SaveRun 被拦截”。

**修复方向**：§6.1 新增 item 10：“scenario session 中 `Ctrl+S` 被拦截并输出 `ui.message.save.blocked-in-validation`”。

#### §2.2.8 **Minor** `app-executable.sha256` 在 `expected-evidence.json` 中的引用不明

§5.4 输出列表包含 `app-executable.sha256` 和 `expected-evidence.json`，但 §5.5 CUA runbook 的 7「Metadata and SHA-256 checks」没写 “runbook 要求人工对比 `app-executable.sha256` 与 packaged app 二进制的实测 sha”。CUA 自动化能否读该文件？如果只写入不校验，就是噪音。

**修复方向**：§5.5 段落 7 明确 “Computer Use 在截图后执行 `shasum -a 256 <app binary>` 并写回 manual record；与 `app-executable.sha256` 不一致时 fail”。

#### §2.2.9 **Nit** `contentPackMode: NONE` 对 PR-04 的 hidden 路径没有说明

PR-04 scenario 使用 `HIDDEN_CONTENT` preset，`contentPackMode` 默认 `NONE`。但 PR-04 §5.1 涉及 `abyssal_temple` 的 secret entry detour，sample pack `sample.flooded_relics.secret_zone.flooded_reliquary` 也挂在 `underground_river`。二者是否在 `contentPackMode=NONE` 下完全不干扰？应在 §5.2 表格中加一列 `contentPackMode` 显示各 scenario 的选择。

---

## §3 PR-01 Profession Tree Run Choice

### §3.1 已修复项（sampled）

1. 一轮 B-PR01-1：Tier 2 / Tier 3 准入口径不清 → §5.4 门槛表给出完整三元条件。✅
2. 一轮 B-PR01-2：`unlockedTalentIds` 破坏性 cutover → §5.1 语义重命名 + 待删除调用点表。✅
3. 一轮 B-PR01-3：`LEARNABLE` / `LEARNED_RESERVE` / `LEARNED_ACTIVE` 状态模型 → §5.2 枚举与判定表。✅
4. 一轮 B-PR01-4：`rogue` 起始技能 `poison_blade` 不在 3 starter 清单 → §5.3 修正为 `backstab / stealth / roll`。✅
5. 一轮 M-PR01-1：断点事件分母 / 分子口径 → §5.5 给出正式定义。✅
6. 一轮 M-PR01-2：旧 save fail-fast 错误码 → §5.7 `INCOMPATIBLE_PHASE4_V4_TALENT_SCHEMA`。✅

### §3.2 二轮新发现

#### §3.2.1 **Blocker** §5.4 Tier 2 门槛表与“data loader fail-fast 校验”自相矛盾

§5.4 Tier 表写 Tier 2 的门槛是：

```
unlockLevel >= 3, 同树投入 >= 2, 其他任一职业树投入 >= 1, 职业总投入 >= 3
```

**没有**“前置 rank >= 2” 要求。紧接着 §5.4 执行要求 item 3 写：

> “data loader 必须 fail fast 校验每条基础职业树**至少 1 个 Tier 2 前置 rank `>= 2`** 和 1 个 Tier 3 前置 rank `>= 3`。”

Tier 2 的门槛里根本没有“前置 rank”变量，data loader 校验的“Tier 2 前置 rank >= 2”找不到对应 field。是 §5.4 门槛表漏了“+ 指定前置 rank >= 2” 条件？还是 item 3 的校验口径错了？

**修复方向**：二选一并对齐：
* 方案 A：Tier 2 门槛补充 “至少一个 prerequisite talent 需要 rank >= 2”。
* 方案 B：data loader 校验只保留 Tier 3 的前置 rank >= 3，删除 Tier 2 部分。

#### §3.2.2 **Blocker** `multiTreeInvestmentRate >= 75%` 因为起始树分布已经自动满足，变成无意义门槛

§5.3 起始树分布表：

| 职业 | 开局有 starter 的树数 |
| --- | ---: |
| `vanguard` | 2（Arms、Shield） |
| `arcanist` | 3 |
| `rogue` | 3 |
| `templar` | 3 |

`multiTreeInvestmentRate` 定义（§5.4 item 4）：“terminal run 中至少两棵职业树各投入 `>= 1` 点”。
Starter 技能 rank 1 已经是 `>= 1 点`，所以：

* arcanist / rogue / templar：开局就有 3 棵树各 >= 1 点，`multiTreeInvestmentRate` 在任意 terminal run 中都 **100% 满足**，与玩家选择无关。
* vanguard：开局 2 棵树有投入（Arms、Shield），已经 >= 2 棵，也 100% 满足。

结果：4 个 BASE 职业的 terminal run `multiTreeInvestmentRate` 恒等于 100%，blocking 门槛 `>= 75%` 无法失败，这个指标对玩家构筑选择没有任何证明力。

**修复方向**：
* 方案 A：把 `multiTreeInvestmentRate` 门槛改为“**除 starter 树之外** 至少再投入一棵”，即排除开局已有 starter 的树。对 vanguard 要求 `Warcry tree >= 1`；对 arcanist / rogue / templar 要求 starter 之外还要主动补投第 4 树（但这些职业本来就只 3 树 -> 不适用，需要重设口径）。
* 方案 B：改成 `multiTreeInvestmentAboveThresholdRate`，要求“至少两棵树 rank 总投入 `>= 3`”（starter rank 1 不够）。
* 方案 C：承认该指标已被数据布局自动满足，降级为 supporting 并提高信号度更高的新指标（例如 `topSingleTreeInvestmentShare <= 60%`）。

#### §3.2.3 **Major** §5.5 满槽 Learn + Esc modal 路径产生“静默学习、无法撤销”的 UX 缺口

§5.5 item 4 固定：

> “四槽已满时 Learn 成功并打开 active slot 替换 modal；玩家按 `Esc` 时技能保留在 reserve，不回滚学习。”

§6.5 Golden Path item 5 同样：“玩家在四个 active slot 已满时学习新主动技能，必须看到替换 modal；取消替换后不丢失刚学技能。”

但 §5.6 输入规则 item 5 `Esc` 取消 draft 并返回地图。语义冲突：

* 若玩家在 draft 预览阶段按 Esc → 取消 draft（技能没学）。
* 若玩家点 Enter 确认学习、四槽满 → 弹出替换 modal → 按 Esc → 技能已学（扣了点），只是没绑 slot。

这两个 Esc 语义不同但在 UI 上区分度弱。Lky 已经对一轮的“取消替换回滚学习”做了显式反转（文档锚定在“不回滚”），但这意味着玩家误按 Esc 可能白扣 1 点却只得到 reserve 技能。

**修复方向**：
* 方案 A（保持当前方案）：文档明确加一行“UI 必须在替换 modal 上显示 `Active slot 替换取消后，技能仍保留在 reserve`，防止玩家误解”。
* 方案 B（反转）：改为“替换 modal 按 Esc 视为取消本次 Learn，点数回滚；显式 Replace 或 `Add to reserve only` 按钮才确认学习”。从玩家体验视角更安全。

推荐 方案 B。

#### §3.2.4 **Major** §7 `autoLearnedNonStarterTalentCount == 0` 是 schema 层不变量，不应作 blocking metric

§5.1 生产路径改造后，`learnableTalentIds` 不再写回 `TalentLoadout`，所以 `autoLearnedNonStarterTalentCount` 按语义等于 0。把它作为 `blockingOwner` 指标与 `baseline=0` 对比，是 **tautology**（任何通过 §5.1 改造的 run 一定 0；任何不满足 §5.1 改造的 run 在 §5.7 阶段已经 fail-fast）。

**修复方向**：
* 方案 A：从 blocking 降级为 `contractGate`（数据结构契约测试），由 `FoundationGameSessionTest` 直接断言，不进 `longRunLab`。
* 方案 B：保留 blocking，但在 `failSemantics` 中明确“此指标只是 regression tripwire”，并标注 “不计入 release gate 主信号”。

#### §3.2.5 **Major** §5.5 断点事件在多 talent / 多 breakpoint 同一 draft 下的语义未定

§5.5 item 4 payload 固定为：

```
professionId, talentId, treeId, breakpointRank, rankBefore, rankAfter, remainingTalentPoints
```

单 `talentId` 字段。但 draft 可以在一次确认中同时：

* 对 `power_strike` 投入 1 点（R1 -> R2），踩到 `war_cry R2` 断点？（跨 talent，不同 tree）
* 对 `war_cry` 投入 2 点（R0 -> R2），踩到 `intimidation unlock` 断点（同 tree）

一次 draft confirm 发多个 event 还是合并成一个？

* 若合并：`talentId` 只能挑 1 个，信息丢失。
* 若分发多个：`remainingTalentPoints` 各 event 差值？`rankBefore / rankAfter` 指向单 talent？

**修复方向**：§5.5 新增 item “同一 draft 触发多个 breakpoint 时，`log.talent.breakpoint_chosen` 每个 breakpoint 发一条，`remainingTalentPoints` 记录该条 event 发出瞬间的剩余点数（最后一条等于 draft 确认后最终值）”。

#### §3.2.6 **Minor** §5.3 rogue starter 与起始树分布表之间 tree 名称耦合未校验

§5.3 上表写 rogue starter：`backstab, stealth, roll`。下表写 rogue 起始树：`Strike 1、Stealth 1、Mobility 1`。

即隐含：`backstab ∈ Strike tree`、`stealth ∈ Stealth tree`、`roll ∈ Mobility tree`。

但本文档没有引用 `game/src/main/resources/data/talents/index.yaml` 来校验 talent 归属。若实际数据里 `stealth` 挂在 Shadow tree 而不是 Stealth tree（或 Rogue 根本没有 “Stealth tree” 这个名字），data loader fail fast 会在 PR-01 合入时才爆。

**修复方向**：§5.3 起始树分布表的表头或备注栏，显式列出“tree id ↔ starter talent id” 映射，并在 §6.1 必测行为中新增 “data loader 校验 profession.starterTalents 的每个 talentId 都属于该 profession 的职业树” 断言。（fail-fast 已覆盖，但读者没有快速手段验证数据关系。）

#### §3.2.7 **Minor** §5.4 “Tier 3 路线与跨树投入共存口径” 的 “Tier 3 主树满足同树 >= 5” 可能出现 6 树矛盾

vanguard 只有 3 棵职业树（Arms / Shield / Warcry），每棵树 6 节点。Tier 3 同树 >= 5 意味着主树投入 5 点（而 starter 只带 1~2 点）。Tier 2 副树 >= 1 + Tier 3 主树 >= 5 = 总投入 >= 6。对应 level 要求 >= 5（Tier 3），那 level 5~10 内能获得的职业点数是否足以撑起 “主树 5 + 副树 1” 的同时满足？

**修复方向**：PR-01 data review 时补充 “level-cap 到 `levelCap` 的总 talent points” 与 “Tier 3 + multi-tree 最低总投入” 的可行性计算，写入 §5.4 或 baseline 文件。

#### §3.2.8 **Minor** §7 `learnedTalentChoiceEventRate >= 90%` 对 vanguard Warcry 空树的倾斜未说明

`learnedTalentChoiceEventRate` 分母如果是 “terminal run”，分子是 “出现过 `log.talent.learned` 的 run”。由于 vanguard Warcry 树没有 starter，玩家不学 Warcry 也能完成主线；rogue/arcanist/templar 三棵树都有 starter，玩家完全可以不学任何非 starter 技能（只升 rank）就通关。

若指标要捕获“学新技能”的玩家选择，应该定义为“至少一次 `rank 0 -> 1` 的 learned event”，而不是“任意 learned event”。当前表述 “learnedTalentChoiceEventRate” 在 §5.5 没有明确是否限定 `rank 0 -> 1`。

**修复方向**：§5.5 / §7 写明 “`learnedTalentChoiceEventRate` 只统计 `rank 0 -> 1` 的 `log.talent.learned`（starter 不计）；`log.talent.rank_up` 事件不计入此分子”。

#### §3.2.9 **Nit** §5.6 footer 文案 `Active slots are edited from the slot panel` 未登记 i18n key

§5.6 footer 文案是展示性 UI 文案，按 README §5.2 i18n 前缀应登记到 `ui.talent.tree.footer.active_slots_hint` 或类似。§5.6 没有列出。Minor；放到 PR 实施时补。

---

## §4 PR-02 Inscription Shop Replacement

### §4.1 已修复项

1. 一轮 B-PR02-1：`canEquip` 替换语义模糊 → §5.2 sealed `InscriptionEquipCheck` + `canReplace` + `replace`。✅
2. 一轮 B-PR02-2：起始铭文表未列 6 职业 → §5.1 6 行全覆盖。✅
3. 一轮 B-PR02-3：类别上限反例未定义 → §5.2 给出 2 条反例。✅
4. 一轮 M-PR02-1：替换后冷却规则 → §5.2 `ceil(candidate.cooldown * 0.5)`。✅
5. 一轮 M-PR02-2：`offerFingerprint` → §5.3 给出 hash 入参。✅
6. 一轮 M-PR02-3：旧 save fail-fast → §5.1 `INCOMPATIBLE_PHASE4_V4_INSCRIPTION_SCHEMA`。✅

### §4.2 二轮新发现

#### §4.2.1 **Major** §5.2 `replace` 函数签名返回 `InscriptionEquipCheck`，未暴露成功时的新 loadout

```kotlin
fun replace(
    loadout: InscriptionLoadout,
    equippedDefinitions: List<InscriptionDef>,
    candidate: InscriptionDef,
    targetHotkey: Int,
): InscriptionEquipCheck
```

`InscriptionEquipCheck` 只有 `Allowed` / `Rejected(reason)`，缺少成功返回的新 `InscriptionLoadout`。调用方只能从 `loadout` 可变引用或隐式 mutation 拿结果，签名与纯函数风格不一致。core 模块里 `InscriptionLoadout` 若是 immutable data class，`replace` 必须返回新实例才能用。

**修复方向**：签名调整为 `sealed interface InscriptionReplaceOutcome { data class Applied(val newLoadout: InscriptionLoadout, val newCooldown: InscriptionCooldownState): InscriptionReplaceOutcome; data class Rejected(val reason: InscriptionEquipFailure): InscriptionReplaceOutcome }`，或明确说明 `replace` 对传入 loadout 做 in-place mutation。

#### §4.2.2 **Major** §5.3 `ShopPurchaseFailure.INSCRIPTION_EQUIP_REJECTED` 丢失下层原因

```kotlin
enum class ShopPurchaseFailure {
    INSUFFICIENT_GOLD,
    STALE_OFFER,
    REQUIRES_REPLACEMENT_TARGET,
    INSCRIPTION_EQUIP_REJECTED,
}
```

当 `replace` 返回 `Rejected(CATEGORY_LIMIT)` 时，商店返回 `INSCRIPTION_EQUIP_REJECTED`，丢失 `CATEGORY_LIMIT` / `SAME_INSCRIPTION` / `TARGET_SLOT_MISSING` 这三类具体原因。client UI 因此不能差异化展示，只能打一条“替换被拒”的模糊消息。

**修复方向**：改成 `sealed class ShopPurchaseFailure`：

```kotlin
sealed interface ShopPurchaseFailure {
    data object InsufficientGold : ShopPurchaseFailure
    data object StaleOffer : ShopPurchaseFailure
    data object RequiresReplacementTarget : ShopPurchaseFailure
    data class InscriptionEquipRejected(val reason: InscriptionEquipFailure) : ShopPurchaseFailure
}
```

#### §4.2.3 **Minor** §5.2 冷却公式边界表述冗余

§5.2 替换后冷却：

> 2. 新铭文写入初始冷却 `ceil(candidate.cooldown * 0.5)`。
> 3. `cooldown <= 1` 的新铭文初始冷却为 `1`。

`ceil(1 * 0.5) = ceil(0.5) = 1`；`ceil(0 * 0.5) = 0`。所以 item 3 真正的意图是 `cooldown == 0` 时初始冷却记为 1，而不是 `cooldown <= 1`。

**修复方向**：改写 item 3：“若 `candidate.cooldown == 0`（即技艺类铭文），初始冷却记为 `1` 以保证至少 1 回合的‘安装保护’”。并加一行“`cooldown == 1` 的铭文按公式得 1，不特殊处理”。

#### §4.2.4 **Minor** §5.3 `offerFingerprint=hash(shopId + offerIndex + offerDef.id + offerPrice)` 的 canonical form 未定义

「`+`」是逻辑拼接，还是字符串拼接？若字符串拼接：

* `shopId="a"`、`offerIndex=1`、`offerId="b"`、`price=10` → `"a1b10"`
* `shopId="a1"`、`offerIndex=""`、`offerId="b"`、`price=10` → `"a1b10"` 若 index 被当成空

发生碰撞但不会发生（offerIndex 是 Int，永远有值）。但通用原则仍应指定 separator、null handling、hash 算法：

**修复方向**：§5.3 增补“`offerFingerprint = sha256(listOf(shopId, offerIndex.toString(), offerDef.id, offerPrice.toString()).joinToString(\"|\")).take(16)`；包含非法字符的 id 需先做 percent-encode。”

#### §4.2.5 **Minor** §5.1 `INSCRIPTION_HOTKEY_START=5` 假定未说明边界

§5.1 item 2 “热键由 `INSCRIPTION_HOTKEY_START` 开始连续分配；当前常量值为 `5`。”
§5.3 item 6 / §5.4 输入 “`INSCRIPTION_HOTKEY_START .. (INSCRIPTION_HOTKEY_START + MAX_INSCRIPTION_SLOTS - 1)` 选择替换目标；当前显示为 `5~8`”。

`MAX_INSCRIPTION_SLOTS=4`，所以热键空间 `5~8`。但 `1~4` 是 active talent slot（PR-01）、`5~8` 是 inscription slot。若未来 `MAX_INSCRIPTION_SLOTS=5`，热键 `9` 是否与其他功能键冲突？PR-02 文档没写 overlap guard。

**修复方向**：§5.1 加一行“`INSCRIPTION_HOTKEY_START + MAX_INSCRIPTION_SLOTS - 1 <= 9`；超过则 InputHandler 必须报 contract failure”。

#### §4.2.6 **Minor** §5.1 六职业起始铭文全部含 `healing_light` 降低 build identity

六个可用职业**全部**起手带 `healing_light`：对玩家而言第一局和第十局都从同一个 H 回复开始，降低 “不同职业感受差异” 的主观体验。

对 game design：这是刻意选择还是历史惯性？一轮报告建议“鼓励分化”，二轮文档保留了 “全员含 healing_light”。

**修复方向**：
* 方案 A（推荐）：`templar` 改 `blessed_recovery + purge`，`rogue` 改 `phase_door + evasive_step`，保留 `healing_light` 作为购买目标而非起始赠品。
* 方案 B（保持）：在 §5.1 增加一行设计理由：“healing_light 是新手 onramp 必备；后续 PR（Lv. 局外成长）可能允许替换起始铭文”。

#### §4.2.7 **Nit** §5.4 替换界面 “`Recovery: 2/2 -> 2/2 | Movement: 1/2 -> 2/2`” 格式宽度未限定

在 1280x800 窗口宽度下，5 个铭文类别（Recovery / Movement / Defense / Purification / ?）+ 箭头 + `/` 容易超长。建议明确 “最多显示 4 类，按前后差异排序”。

---

## §5 PR-03 Build Identity Reward Adoption

### §5.1 已修复项

1. 一轮 B-PR03-1：owner metric cutover 需要从 `.reportOnly` 升级到 blocking 且删除旧 id → §5.6 cutover 表。✅
2. 一轮 B-PR03-2：`wrongProfessionCapstonePenalty` 数值合同 → §5.2 `ceil(baseScore * 0.8)`。✅
3. 一轮 B-PR03-3：slot family 聚合口径 → §5.3 item 4 `WEAPON / OFF_HAND / ARMOR / ACCESSORY / CONSUMABLE_OR_UTILITY`。✅
4. 一轮 M-PR03-1：rogue OFF_HAND `basic_shield` 排除路径 → §5.5 level `>= 5` 硬 ban。✅
5. 一轮 M-PR03-2：ScoreFormula 公式 → §5.2 给出公式。✅

### §5.2 二轮新发现

#### §5.2.1 **Blocker** §5.4 `topFiveAffixExposureShare <= 40%` 与单 affix `<= 15%` 之间算术可能不可同时满足

§5.4 item 3：“`sentinel / of_strength / vampiric / of_life` **任一单项 share 不得超过 `15%`**”。
§5.4 item 5：“新增 `topFiveAffixExposureShare`，**阈值 `<= 40%`**”。

如果 top 5 affixes 每个都 15%，top 5 share = 75%，违反 40%。要让 top 5 share <= 40%，每个 affix 平均需 <= 8%。单 item 15% 上限允许更高。换句话说：
* 15% 上限只规范“不让某一个 affix 爆掉”。
* 40% 上限要求“前 5 名集中度不超 40%”。

这两个约束并不真正矛盾（15% 是每个单项的**上限**，40% 是前 5 的**上限**；实际可达状态：`15 + 10 + 8 + 4 + 3 = 40`），但在文档里没写清 “单项 `<= 15%`” 是 “upper bound that is not tight”。很容易让实现者以为“只要保证每个 <= 15% 即可”，结果产出 `15+15+15+15+15=75%` 配置，然后 release gate 因 40% 门槛爆。

**修复方向**：§5.4 item 3 改写：“`sentinel / of_strength / vampiric / of_life` 任一单项 share 不得超过 `15%`；且四者之和必须 <= `30%`（为 `topFiveAffixExposureShare <= 40%` 留出 10% 余量给非指定 affix）”。或直接显式：“top 1 affix <= 15%；top 5 affix 累计 <= 40%；单 item 15% 只是硬顶，实际 tuning 目标是 top5 <= 40%”。

#### §5.2.2 **Blocker** §5.1 capstone × profession 权重矩阵只覆盖 6 个 capstone item，`build-identity/index.yaml` 里另外 7 个没写

§5.1 上表列了 13 个 item：`artifact_forge_oath / unique_furnace_plate / unique_quenchbreaker_maul / artifact_river_echo / unique_deepcurrent_lens / unique_cinderveil_plate / artifact_briar_heart / artifact_heartroot_gambit / unique_thornpath_crook / unique_briarbound_bow / artifact_eclipsed_relic / unique_vesper_chainmail / unique_voidlit_seal`。

下面的 `capstone × profession` 权重矩阵只有 6 行：

```
unique_furnace_plate, artifact_forge_oath, unique_cinderveil_plate,
unique_deepcurrent_lens, artifact_briar_heart, artifact_eclipsed_relic
```

缺失的 7 项：

| 缺失 item | 预期职业归属 | 实现方需要的权重动作 |
| --- | --- | --- |
| `unique_quenchbreaker_maul` | vanguard | `professionCapstoneBonus` for vanguard，其他职业 penalty |
| `artifact_heartroot_gambit` | rogue | 同上 |
| `unique_thornpath_crook` | rogue | 同上 |
| `unique_briarbound_bow` | rogue | 同上 |
| `unique_vesper_chainmail` | templar | 同上 |
| `unique_voidlit_seal` | templar | 同上 |
| `artifact_river_echo` | arcanist | 同上 |

没有权重矩阵，实现者面对这 7 个 item 只能自由发挥。`rogue` OFF_HAND payoff 验证会因此出现 `artifact_briar_heart` 强、`artifact_heartroot_gambit` 弱的不对称。

**修复方向**：§5.1 矩阵扩成 13 行 × 4 列（或 4 职业纵向表格）；或注明 “矩阵只给 example，其他 item 按 profession 归属自动分配 `professionCapstoneBonus` / `wrongProfessionCapstonePenalty`”，并把这条 rule 写进 §5.2。

#### §5.2.3 **Major** §5.2 `baseScore` 的定义缺失

`wrongProfessionCapstonePenalty = ceil(baseScore * 0.8)`
`professionCapstoneBonus >= ceil(baseScore * 0.6)`
`nonWeaponPayoffBonus >= ceil(baseScore * 0.45)`

但全文没有任何地方定义 `baseScore` 是什么：

* 是 item rarity + item level 的线性组合？
* 是 `MilestoneRewardSelector` 原始 score？
* 是 `lootBalanceLab` 的 scoring seed？

`baseScore` 未定义意味着 0.8 / 0.6 / 0.45 是空头乘子，实现方可以任选。

**修复方向**：§5.2 ScoreFormula 之前插入：“`baseScore = selectorBaseScore(candidate, context)` 由 `MilestoneRewardSelector` 当前评分主干给出，包含 item rarity 基础分 + slot match + affix match。对本 PR 不修改 `baseScore` 的计算，只在其后应用 bonus / penalty”，并给 `baseScore` 的预期数值区间（如 `10 <= baseScore <= 200`）。

#### §5.2.4 **Major** §5.2 `terminalIdentityBonus` 的触发条件不明确

§5.2 item 4：“`terminalIdentityBonus` 只在当前职业缺少终局身份 item 时生效，最大值不得超过 `professionCapstoneBonus`”。

“缺少终局身份 item” 的判定：
* 按已装备（player currently wears no capstone）？
* 按 run lifetime（run 还没拿到任何 capstone）？
* 按最近 N 个 milestone（最近 3 个 reward 没有 capstone）？
* 按 zone / floor 阈值（`floor >= 4` 触发）？

不同判定导致 `unique_cinderveil_plate` 的 arcanist 采用率差异很大。

**修复方向**：§5.2 item 4 扩展：“`terminalIdentityBonus` 在满足以下全部条件时生效：(1) run 当前 floor `>= 4`，(2) 玩家**目前装备栏**中无任何 profession capstone item，(3) 本次 milestone candidate 属于 profession capstone 集合。bonus 值 = `ceil(baseScore * 0.5)`，与 `professionCapstoneBonus` 上限对齐。”

#### §5.2.5 **Major** §5.2 `unique_cinderveil_plate` vs `unique_deepcurrent_lens` 在 arcanist 的采用优先级会产生 tie-break 歧义

矩阵给的加成：
* `unique_cinderveil_plate` for arcanist = `nonWeaponPayoffBonus + terminalIdentityBonus` ≈ `0.45*base + 0.5*base = 0.95*base`
* `unique_deepcurrent_lens` for arcanist = `professionCapstoneBonus + nonWeaponPayoffBonus` ≈ `0.6*base + 0.45*base = 1.05*base`

`deepcurrent_lens` 略高，但差异在 `ceil` 和 bonus lower bound 的浮动区间内，可能并列。无 tie-break 规则：

* 按 slot？`cinderveil_plate` 是 ARMOR，`deepcurrent_lens` 是 OFF_HAND。
* 按 rarity？两个都是 unique。
* 按 id 字典序？

**修复方向**：§5.2 新增 item 7：“`finalScore` 相等时 tie-break 按 `(slot family rotation, 数据 yaml 声明顺序)`；不依赖 id 字典序或 drop timestamp。”

#### §5.2.6 **Major** §7 同一 PR 一次升 12 个 blocking metric，baseline 文件承载面过大

`docs/review/phase4/opt/baselines/2026-04-12-phase4-terminal-build-identity-baseline.json` 要承载：`professionCapstoneAdoptionFloor` / `nonWeaponBuildPayoffFloor` / `terminalWeaponBaseDiversity` / `crossProfessionTopWeaponDominance` / `milestoneRewardAdoptionDelta` / `milestoneRewardSlotBalance.*` × 6。

11 个 blocking + 12 条 cutover，单文件 baseline 变更面大，PR diff 复杂，debug 时难定位是哪个 baseline 字段引发 fail。

**修复方向**：
* 方案 A：拆分 baseline 为 `phase4-terminal-build-identity-profession.json` + `phase4-terminal-milestone-slot-balance.json` + `phase4-terminal-build-identity-weapon-dominance.json`。
* 方案 B：保留单文件但在 PR diff 中要求按指标分块 diff，PR 描述列每个 metric 的 baseline delta。

#### §5.2.7 **Minor** §5.5 `lateCommonPenalty` 数值规模未定

§5.5 item 3：“其他职业 level `>= 5` 的 milestone candidate pool 对 COMMON item 应用 `duplicateSlotPenalty` 同级的 `lateCommonPenalty`。”

`duplicateSlotPenalty` 数值规模本身在 §5.2 也没写。这俩一前一后传递未定义值。

**修复方向**：§5.2 补齐 `duplicateSlotPenalty = ceil(baseScore * 0.3)` + `lateCommonPenalty = duplicateSlotPenalty`，并说明 “penalty 可叠加，但叠加后 `finalScore` 下限不得跌破 0”。

#### §5.2.8 **Minor** §5.3 item 2 cross-source 软历史策略未给出 decay

§5.3 item 2：“跨 source 只写入 soft history，用作 `slotRotationBonus` 与 `duplicateSlotPenalty` 输入，不做硬 ban”。

soft history 的 decay / window length 未定义：“最近多少次 milestone 内生效？” 影响 rotation bonus 实际强度。

**修复方向**：§5.3 item 2 扩展：“soft history 窗口固定为最近 `3` 次 milestone reward；落出窗口后不再参与 rotation / duplicate 计算。”

#### §5.2.9 **Nit** §5.1 矩阵行内双加成 `professionCapstoneBonus + nonWeaponPayoffBonus` 的加法上限

矩阵显示 `unique_deepcurrent_lens (arcanist) = professionCapstoneBonus + nonWeaponPayoffBonus`。数值合同没禁止两个 bonus 叠加。但 ScoreFormula 表达式只是 `+ bonus1 + bonus2`，叠加无上限可能让 non-weapon capstone 压过 weapon capstone。若设计意图是“多重 bonus 叠加”，应在 §5.2 写明。

---

## §6 PR-04 Hidden Search And Zone Hooks

### §6.1 已修复项

1. 一轮 B-PR04-1：`greenwood_fringe` 作为 onramp zone 被错误纳入 top-share 分母 → §5.1 item 3 从 `topZoneLeadShare` 分母排除。✅
2. 一轮 B-PR04-2：runtime hook 没有 snapshot 出口 → §5.2 `runtime hook 最小接线` 表 + `zone_hook_triggered` cue。✅
3. 一轮 B-PR04-3：`flavorOnlyMechanics` 与 `mechanicsWithoutDedicatedRuntimeHook` 的关系 → §5.2 item 4 等式。✅
4. 一轮 M-PR04-1：cue TTL / dedup → §5.3 表。✅
5. 一轮 M-PR04-2：`slagCueDensityPerEligibleRoom` 指标 → §5.1 item 4 + §5.4 表。✅

### §6.2 二轮新发现

#### §6.2.1 **Blocker** §5.2 5 mandatory zones 不含 `crystal_cavern`，但 PR-06 的 `2026042406` 把 `crystal_cavern` 放进 `full_route`

PR-04 §5.2 runtime hook 表的 “mandatory zone” 清单：`greenwood_fringe / deep_iron_pit / grey_gate_depths / underground_river / abyssal_temple`。5 个 zone，全部有 hook。

但 PR-06 §5.1 seed `2026042406 full_route` 路线为：`deep_iron -> grey_gate -> crystal_cavern -> underground_river -> abyssal_temple`。

`crystal_cavern` 要么是新 mandatory zone（那 PR-04 §5.2 清单遗漏，`zoneHookCoverage: 5/5` 应为 `6/6`），要么是 secret branch zone（那不应该出现在 `full_route`）。

见 §7.2.1（PR-06 Blocker）。PR-04 和 PR-06 必须对齐。

**修复方向**：见 §7.2.1。如果 `crystal_cavern` 是 branch zone，PR-06 把该 seed 改为 `branch_inclusive` 即可；如果 mandatory，PR-04 必须新增 hook。

#### §6.2.2 **Blocker** §5.3 cue 优先级与 §6.1 item 8 “不遮挡 CRITICAL / HIGH priority 战斗提示” 存在矛盾

§5.3 cue TTL 表：

| cue | 优先级 |
| --- | --- |
| `search_available` | `HIGH` |
| `secret_entry_nearby` | `HIGH` |
| `lead_discovered` | `MEDIUM` |
| `zone_hook_triggered` | `MEDIUM` |

§6.1 item 8：“client 前台 cue 在玩家行动前可见，且不遮挡 CRITICAL / HIGH priority 战斗提示。”

`search_available` 和 `secret_entry_nearby` 本身就是 HIGH priority 的 hidden cue。若战斗中出现 `search_available` 与战斗 HIGH priority `boss_attack_intent` 同时竞争 frontstage 空间，按 §6.1 item 8 要求 hidden HIGH 不能遮挡战斗 HIGH，但两者都是 HIGH，谁让谁？

**修复方向**：§5.3 cue 表把 hidden cue 改成 `MEDIUM`（`search_available` 除外 → 保持 HIGH，但加一行“`search_available` 在战斗进行中自动降级为 MEDIUM”）；或 §5.3 新增 priority tier `HIGH_HIDDEN`，严格低于 `HIGH_COMBAT`。

#### §6.2.3 **Major** §5.2 `oil_or_fire_seen` trigger 的产者未声明

PR-05 §5.2 `molten_glass` variant 使用 trigger `zone.trigger.oil_or_fire_seen`。PR-04 §5.2 runtime hook 表只定义了 5 个 zone hook（`trail_pressure / slag_alert / ritual_pressure / ferry_crossing / void_pressure`），没有 `oil_or_fire_seen`。

`oil_or_fire_seen` 是否属于 `slag_alert` 的子 event？还是独立 trigger？若独立，哪个 PR 负责产生？

**修复方向**：
* 方案 A：PR-04 §5.2 `slag_alert` hook 细化出子 trigger：“slag/ore cue 触发时，同时发出 `slag_alert_triggered` 与 `oil_or_fire_seen` 两类 boss-accessible trigger”。
* 方案 B：PR-05 §5.2 改用 `zone.trigger.slag_alert` 代替 `oil_or_fire_seen`。

#### §6.2.4 **Major** §5.4 新增 8 个 blocking / report-only owner metric，单 PR 压力过大

`topZoneLeadShare` (B), `perZoneSecretConversionFloor.reportOnly` (RO), `secretZoneSearchConversionFloor.reportOnly` (RO), `zoneSearchPromptVisibility` (B), `perZoneSearchUseFloor.reportOnly` (RO), `slagCueDensityPerEligibleRoom.reportOnly` (RO), `zoneHookCoverage` (B), `frontstageSearchCueVisibilityRate` (B)。

4 个 blocking + 4 个 report-only。每个 blocking 需要 baseline 文件 + catalog entry + metric test。PR-04 工作量 L 标签下包 4 个 blocking metric，合并后 rebase 代价高。

**修复方向**：§5.4 排出 metric 的落地顺序，允许分批：`zoneHookCoverage` + `frontstageSearchCueVisibilityRate` 先做（runtime hook / cue 可见性直接关系 gameplay）；`topZoneLeadShare` + `zoneSearchPromptVisibility` 随后。或在 README §6 声明 PR-04 可拆成 PR-04a (runtime hook + cue) + PR-04b (probe metric)。

#### §6.2.5 **Minor** §5.3 `search_available TTL=4 turns` 过短，对阅读速度慢的玩家不友好

玩家进入 `deep_iron_pit` 房间、看到 slag cue、决定 Search 需要 1~2 回合的阅读 + 决策。4 回合 TTL 扣掉“看到 + 思考 + 准备”后只剩 1 回合动作窗口。若玩家需要先解决战斗，再回来 Search，很容易错过 cue。

**修复方向**：`search_available TTL` 从 `4` 改为 `6` 或 `8`；或在 `zone_hook_triggered TTL=6` 同级设置 dedup，使 cue 在玩家离开房间 + 再回来时自动 re-emit。

#### §6.2.6 **Minor** §5.2 `trail_pressure` / `slag_alert` runtime 效果数值未定

* `trail_pressure`: “调整隐藏线索刷新权重” — 调整幅度？倍数？
* `slag_alert`: “生成 Search prompt 与 FIRE/OIL warning” — FIRE/OIL warning 的内容（伤害 +x%？纯视觉？）？
* `ritual_pressure`: “下一次 encounter pressure +1” — pressure 的量纲？
* `ferry_crossing`: “生成 crossing choice 与 secret clue” — choice 有几个分支？

全部只有文字描述，没有数值。实现者无法验证。

**修复方向**：§5.2 runtime hook 表新增一列 `expectedMagnitude` / `payload`：

| hook | 量化效果 |
| --- | --- |
| `trail_pressure` | `hiddenLeadWeightMultiplier = 1.25x` 持续 `3 floors` |
| `slag_alert` | `searchPromptVisible = true`；FIRE/OIL warning 纯 visual + audio cue，不修改伤害 |
| `ritual_pressure` | `nextEncounterPressure += 1` 只计一次 |
| `ferry_crossing` | `choice = {快速过河 (+1 fatigue), 探查 ferry (+1 hidden clue, -2 turn)}` |
| `void_pressure` | `resourcePressureWarning` 纯视觉 + 下一个 objective 的 light/time penalty `x 1.2` |

#### §6.2.7 **Nit** §5.1 `greenwood_fringe` 的 “Search-driven path 证据” 是 evidence 还是 metric？

§5.1 item 3 要求 “必须保留 Search-driven path 证据”。evidence 的定义在哪里？是白盒 screenshot、organic probe 的 trace sample、还是 hidden harness 的断言？建议显式登记到 `hiddenContentHarness` 作为 `greenwoodFringeSearchDrivenPathPresent=true` supporting 字段。

---

## §7 PR-05 Boss Variant Phase Language

### §7.1 已修复项

1. 一轮 B-PR05-1：`phaseOverrides` schema 单 owner → §5.1 `MutationModels.kt` + sealed `TriggerExpression`。✅
2. 一轮 B-PR05-2：trigger expression 必须 2 child 以上 → §5.1 item 8。✅
3. 一轮 B-PR05-3：action weight multiplier 数值 → §5.1 item 4 固定 `1.5x`。✅
4. 一轮 M-PR05-1：`phaseGraphUnchanged=true` 与 coverage 共存解释 → §5.4。✅
5. 一轮 M-PR05-2：telegraph spec ADD → §5.3。✅

### §7.2 二轮新发现

#### §7.2.1 **Blocker** §5.2 `abyssal_eclipse` 依赖 `zone.trigger.void_pressure_active`，跨 PR 串行依赖未声明

见 §1.2.1 README 复核。PR-05 `boss.variant.abyssal_eclipse` 的 trigger `AllOf(hp_below_40, void_pressure_active)` 中的 `void_pressure_active` 是 PR-04 `void_pressure` hook 的 runtime 产物。PR-05 runtime trigger coverage `3/3` 要求此 trigger 可用。

**修复方向**：见 §1.2.1。

#### §7.2.2 **Blocker** §5.2 `oil_or_fire_seen` trigger 无产者

见 §6.2.3。PR-05 `molten_glass` variant 用 `zone.trigger.oil_or_fire_seen`，但 PR-04 未定义。

**修复方向**：见 §6.2.3。

#### §7.2.3 **Major** §5.2 `grey_crown` 用的 `boss.trigger.war_caller_active` 产者也不明

`boss.trigger.war_caller_active` — “war caller” 角色的 active 状态。若 grey_crown 遭遇战场上伴生 war_caller 精英单位，且该 trigger 由 `BossEncounter` 状态机发出，则 PR-05 内部完成；若需要现有 boss encounter schema 新增字段，文档未声明。

**修复方向**：§5.2 variant 表新增一列 `triggerOwner`：指明每个 `triggerId` 属于 `boss-encounter-schema`、`zone-hook` 或 `existing-trigger-registry`。

#### §7.2.4 **Major** §5.1 item 7 data loader 要校验 `actionEmphasisIds` 存在于 Boss 可用 talent/action，但 `linebreaker / earthshaker / battlefield_command / ritual_break / void_breach / abyssal_consecration` 的原始 Boss 归属未声明

如果 `linebreaker` 是 `molten_glass` 对应 base boss 的原生 action，OK；否则 `actionEmphasisIds` 找不到。`linebreaker / earthshaker` 在现有 boss action 表里是否真的存在？文档未引用现有 `bosses/index.yaml` 的 action 集合。

**修复方向**：§5.2 variant 表扩表把 `actionEmphasisIds` 后标注其归属 boss，例如 `linebreaker ∈ boss.molten_forgeborn.actions`；或加一行 “data loader fail-fast 引用 `bosses/index.yaml` base boss 的 action 全集”。

#### §7.2.5 **Minor** §5.1 `onEnterEventKey` 类型语义不明

`onEnterEventKey: String` — 是 event bus key、i18n key、log key、还是 `RenderSnapshot` 字段名？

**修复方向**：§5.1 新增 item 9：“`onEnterEventKey` 是 `CoreEventBus` 事件 key，必须满足 `boss.variant.<variantId>.phase_override.entered` 命名模式，并进入 `i18n` 与 `log.boss.phase_override_entered`”。

#### §7.2.6 **Minor** §5.1 item 4 `action emphasis multiplier = 1.5x, 只持续当前 phase` 未定义 phase 结束后恢复

Boss phase 进出逻辑：
* override trigger hit → action weight × 1.5 生效。
* phase 结束 → 恢复。

但“phase 结束”的判定（HP 跨阈值、玩家行为、计时器）未声明。如果 Boss 连续两次进入同一 phase（enrage -> desperate -> enrage-again 的复合），multiplier 是否重新开始计时？

**修复方向**：§5.1 item 4 增加 “multiplier 作用域 = `encounter.phase` 实例级；同一 phase 若因 HP 反弹（治疗/吸血）再次进入不重置 multiplier（仍有效）”。

#### §7.2.7 **Nit** §5.3 telegraph spec 命名 `<variantId>_phase_override_warning` 与 §5.4 `phaseOverrideTelegraphCoverage` 字段的对应关系未写入 failSemantics

`phaseOverrideTelegraphCoverage=3/3` 的 `3` 分母是什么？3 个 variant（数 telegraph spec）还是 3 个 ADD 动作？需要明确。

---

## §8 PR-06 Long Run Route Diversity

### §8.1 已修复项

1. 一轮 B-PR06-1：`LONG_RUN_SMOKE` corpus 明确 seed 列表 → §5.1 16 条 seed。✅
2. 一轮 B-PR06-2：route hash 算法明确 → §5.2 `sha256(routeToken).take(16)`。✅
3. 一轮 B-PR06-3：`verifyChanged` routing 与 `scopeCoverageLint` 正反 fixture → §5.3。✅
4. 一轮 M-PR06-1：start-zone 分布口径 → §5.1 item 4 4:4:4。✅
5. 一轮 M-PR06-2：branch 不重复 secretZoneId → §5.1 item 5。✅

### §8.2 二轮新发现

#### §8.2.1 **Blocker** §5.1 seed `2026042406 full_route` 路线穿越 `crystal_cavern`，与 full_route 口径矛盾

§5.1 表：

```
2026042406 full_route: deep_iron -> grey_gate -> crystal_cavern -> underground_river -> abyssal_temple
```

`crystal_cavern` 在 PR-04 §5.2 的 5 个 mandatory zones 中不存在。`full_route` 的语义按约定是 “走完所有 mandatory zones”，若 `crystal_cavern` 是 branch / secret zone，则此 run 应分类为 `branch_inclusive`。

而 `2026042414 branch_inclusive: deep_iron -> grey_gate -> crystal_cavern branch` 则显式声明 crystal_cavern 是 branch。同一 zone 在 06 被当 mandatory，在 14 被当 branch，自相矛盾。

**修复方向**：
* 方案 A（推荐）：seed `2026042406` 替换为另一 mandatory-only 路径，如 `greenwood -> underground_river -> deep_iron -> grey_gate -> abyssal_temple`（start-zone 分布保持 4:4:4）；或改为 `branch_inclusive` 类型，让 `full_route` 只剩 11 条——但 `fullRouteCount >= 12` blocker 会失败。
* 方案 B：把 `crystal_cavern` 纳入 PR-04 mandatory zone 清单 + hook coverage `6/6`，则 `full_route` 口径一致。

#### §8.2.2 **Major** §5.1 item 4 start-zone 4:4:4 假设 `grey_gate_depths` / `abyssal_temple` 永不作为起点，但没有显式声明

4:4:4 分布：`greenwood_fringe=4 / deep_iron_pit=4 / underground_river=4`。隐含 `grey_gate_depths / abyssal_temple` 不是 start zone。README 与 PR-04 都没显式声明 “这 2 个 zone 不能作为 run 起点”。若 procgen 某 seed 产出 abyssal_temple 作为起点，`HeadlessRunHarness` 应 fail fast，但文档没写。

**修复方向**：
* §5.1 新增 item 8：“`grey_gate_depths` 与 `abyssal_temple` 在 `LONG_RUN_SMOKE` 与 `MAPGEN_DIFF` preset 下不得作为 start zone；`HeadlessRunHarness` 在 run 起始检查 start zone，若违反 fail-fast。”

#### §8.2.3 **Major** §5.2 `routeToken` 未说明 late-route-probe 与 full-route 的合并边界

§5.2 item 3：“late route probe 的 probe-only zone 不写入 terminal route hash；它只写入 `probeRouteHash`。”

但 `probeRouteHash` 没在 §5.2 的模型 `ZoneRouteHashDiversity` 中出现。`probeRouteHash` 是第二 metric 吗？Report 是否输出？

**修复方向**：§5.2 新增 item 5：“`probeRouteHash` 独立于 `zoneRouteHash`，进入 supporting 字段 `probeRouteHashSample`；不参与 `zoneRouteHashDiversity` 聚合。”

#### §8.2.4 **Major** §5.1 `branchInclusiveCount >= 3` 门槛与 corpus `branch_inclusive = 4` 之间的松紧度不合理

corpus 固定 4 条 branch run，但 blocking 指标 `>= 3`。意味着 1 条 branch run 可以失败（比如 run crash）并仍然 pass gate。但 §5.1 item 1 / item 2 说 “branch_inclusive 样本数固定 `4`”。固定 = 必须 4 条全部成功出样本？若是，blocker 应该是 `>= 4`。

**修复方向**：`branchInclusiveCount >= 4` 或 §5.1 item 5 说明 “允许 1 条 branch run harness failure，不影响 gate”。

#### §8.2.5 **Minor** §5.2 `routeToken` 的 `secret:` prefix 与 base zone id 命名空间无 separator

§5.2 item 2：“secret branch token 格式固定为 `secret:<secretZoneId>`”。若 `secretZoneId` 本身含冒号（unlikely but not forbidden），reverse parsing 失败。

**修复方向**：在 `ContentPackIdParser` 或等价地方加断言 “`secretZoneId` 不得包含 `:` `|` `>`”。

#### §8.2.6 **Minor** §5.1 item 5 “同一 `secretZoneId` 不得在 branch corpus 中重复作为主 branch” 未定义 “主 branch” 口径

branch_inclusive run 可能走过多个 secret branch（greenwood 进入 underground_river 又进 abyssal_temple secret inner）。哪个算“主 branch”？

**修复方向**：§5.1 item 5 明确 “主 branch = route token 中第一个 `secret:<zoneId>` marker 对应的 secret zone；不得跨 branch run 重复”。

#### §8.2.7 **Nit** §7 `topologyCategoryDiversityPerSmokeRun.reportOnly` baseline 引用 `2026-04-16-phase4-critical-path-pacing-owner-baseline.json`

`topologyCategoryDiversityPerSmokeRun.reportOnly >= existing warning floor` 表达式里的 `existing warning floor` 是硬编码数字还是 baseline 读出？若从 baseline 读出，baseline 文件必须先存在这个 key。建议 PR 内显式写出 baseline 字段名：`baseline.topologyCategoryDiversityPerSmokeRun.warningFloor = <number>`。

---

## §9 PR-07 Sample Pack ADD-first Visibility

### §9.1 已修复项

1. 一轮 B-PR07-1：sample manifest 主路径 REPLACE → §5.1 改为 5 ADD。✅
2. 一轮 B-PR07-2：fixture pack id 与 official sample pack 分离 → §5.1 item 5 `fixture.sample_flooded_relics_override`。✅
3. 一轮 M-PR07-1：`hiddenBranchBindings` schema → §5.2。✅
4. 一轮 M-PR07-2：content pack overlay 不跨 pack 覆盖 → §5.2 item 2。✅
5. 一轮 M-PR07-3：玩家可见性指标 `samplePackContentPlayerVisibilityRate.reportOnly` → §5.3。✅

### §9.2 二轮新发现

#### §9.2.1 **Blocker** §5.2 `secondarySecretSlot` 概念跨 PR 引用但 PR-04 未定义

§5.2 item 1：“sample pack hidden branch binding 使用 pack-local `secondarySecretSlot`，不与 base `underground_river_crystal_rift` 争夺主 secret slot”。

PR-04 §5.1 `underground_river` 的改造是“提升 lead density，使 lead share 落在 15%~40%”，未提到 `primarySecretSlot / secondarySecretSlot` 分槽机制。`secondarySecretSlot` 要么是 PR-07 引入的新 mapgen 概念（那应该在 PR-04 里 land 基础 pipeline），要么是 PR-04 漏了对此概念的声明。

**修复方向**：
* 方案 A：PR-04 §5.1 扩展 `underground_river` 改造，声明 `secretZoneSelector` 对每个可带 secret 的 zone 支持 `primarySlot / secondarySlot`；PR-07 基于此扩展。
* 方案 B：PR-07 自带 mapgen 新概念 `secondarySecretSlot` 并在 §3.1 的生产代码列表增加 `HiddenContentMapgenPipeline.kt` 的具体改动（当前列表已经列了，但改动语义未标）。

#### §9.2.2 **Blocker** §5.2 item 3 “force-inject 只允许在 harness、whiteBoxContentPack、PR-07 validation scenario 中启用” 引入 production-path test backdoor

“force-inject”机制在 production 代码里，但只在三个上下文启用。实现路径可能是：

* `HiddenContentMapgenPipeline` 读取一个环境变量 / system property / validation flag 来决定是否 force-inject。
* 普通玩家 run 不设置该 flag → 不 force-inject。

这意味着 production 代码里留了一条 test-only 分支，违反一轮审阅中反复强调的 “不把 test fixture 逻辑塞进 production”。即使这个 flag 被严格控制，它仍然是 production surface 的一个 test hook。

**修复方向**：
* 方案 A（推荐）：把 force-inject 逻辑移到 `tools/test-support` 模块（单独 source set），`contentPackHarness` / `whiteBoxContentPack` 通过单独的 test runner 调用；生产模块不引用 test-support。
* 方案 B：把 `fixedSeedVisibilityCase` 作为 content pack overlay 的 **数据** 字段（pack manifest 里声明），而不是 code flag，让“force-inject”等价于“该 pack 对特定 seed 指定 secondarySlot 为此 pack 的 secret zone”。production 代码只读 pack 数据，不走分支。

#### §9.2.3 **Major** §5.1 fixture pack id `fixture.sample_flooded_relics_override` 与 official sample pack id `sample.flooded_relics` 的 `.` 分隔语义引发 parser 歧义

§5.1 item 6 强调 “`ContentPackIdParser` 必须把 `fixture.sample_flooded_relics_override` 作为完整 pack id 字符串处理，不得按 `.` 拆分成 namespace 层级”。

但 pack-local content id 前缀用 `sample.flooded_relics.*`（§5.1 item 1）。`.` 在 sample pack 里是 namespace 层级分隔，在 fixture pack id 里不是。parser 必须按 pack id 的 **pack-kind prefix**（`sample` vs `fixture`）决定 `.` 的语义。

这是一个脆弱的设计：新 pack 类型（如 `tutorial.` / `playtest.`）引入时 parser 必须再加规则。

**修复方向**：fixture pack id 改为 `fixture_sample_flooded_relics_override`（下划线），彻底去掉 `.` 分隔歧义；parser 规则保持 “`.` 永远是 namespace 分隔”。

#### §9.2.4 **Major** §5.2 `fixedSeedVisibilityCase: sample_flooded_relics_active_2026042407` 与 PR-00 / PR-06 seed 再次撞表

seed `2026042407` 在三个上下文同时出现：

* PR-00 §5.2: pr07 scenario seed = `2026042407`。
* PR-06 §5.1: `2026042407 full_route deep_iron -> underground_river -> greenwood -> grey_gate -> abyssal_temple`。
* PR-07 §5.2: `fixedSeedVisibilityCase = sample_flooded_relics_active_2026042407`。

三者的 preset 不同（`CONTENT_PACK` / `LONG_RUN_SMOKE` / `CONTENT_PACK_ACTIVE_SAMPLE`），但共享 Long seed。force-inject 机制读 `seed == 2026042407 && contentPack == sample.flooded_relics` 才生效，但 long-run corpus run 本身不激活 pack，所以 force-inject 不会误触。只是读者阅读困惑。

见 §2.2.1 Blocker。修复方向相同。

#### §9.2.5 **Major** §5.3 `samplePackAddOnlyMainPath: true` 与 `manifestLint` 可能重复

`manifestLint` 应该已经覆盖 “sample pack 不含 REPLACE 主路径”。`samplePackAddOnlyMainPath` blocking metric 是对同一事实的第二次覆盖。

若 `manifestLint` 覆盖了，`samplePackAddOnlyMainPath` 应 supporting；若没覆盖，`manifestLint` 应扩展。两者并存且同为 blocking 会产生冗余。

**修复方向**：二选一：
* `manifestLint` 作为 fail-fast gate（编译期）；`samplePackAddOnlyMainPath` 作为 supporting（runtime evidence，不 gate）。
* 或两者合并为单一 `sampleAddOnlyMainPathGate`。

#### §9.2.6 **Minor** §5.4 key resolution summary `overriddenKeys official sample pack 固定为 0` 的判定口径不明

“override” 的判定：
* 使用了与 base 相同的 visual/audio key id（reuse）→ 0 overridden？
* 使用了与 base 相同的 key id 但指向不同文件（pack visual/manifest 覆盖）→ 1 overridden？

若 pack 的 `visual-manifest.json` 里 `icon.skill.templar.holy_light` 指向 pack-local 图片，overridden 应该计 1。若 pack 只引用已存在 key（reuse），overridden 计 0。

**修复方向**：§5.4 key resolution summary 加定义：“`overriddenKeys` = pack visual/audio-manifest 对 base manifest 中同名 key 的 file path overriding 数量；官方 sample pack 不 override，固定 0”。

#### §9.2.7 **Minor** §5.2 secret zone selection 规则 item 4 “normal runtime 中 base crystal rift 与 sample flooded reliquary 可在同一 run 共存” 未说明容量

base `underground_river_crystal_rift` + pack `sample.flooded_relics.secret_zone.flooded_reliquary` = 2 个 secret 挂在 `underground_river`。`underground_river` 的 secret slot 容量是否 `>= 2`？还是 `primarySlot + secondarySlot = 2` 正好？若有第三个 content pack 也挂 secret 到 `underground_river`，容量不够。

**修复方向**：§5.2 item 4 明确 “`underground_river` secret slot capacity = 2（primary + secondary）；超过则 pack loader fail-fast”。

#### §9.2.8 **Nit** §5.2 `hiddenBranchBindings` 字段不属于 overlay op，但 `ContentPackModels.kt` 是否把它放进 manifest root 还是单独 section 未标

§5.2 item 1：“该字段不属于 overlay `op`，不改变 `ADD / REPLACE` 白名单。” 但 `hiddenBranchBindings` 在 manifest yaml 中的具体层级位置（manifest.rootLevel 还是 manifest.extensions.hiddenBranchBindings）没写。manifestLint 无法校验。

**修复方向**：§5.2 item 1 给出 yaml 示例：

```yaml
packId: sample.flooded_relics
ops: [...]
extensions:
  hiddenBranchBindings:
    - bindingId: sample.flooded_relics.search.flooded_reliquary
      zoneId: underground_river
      ...
```

---

## §10 跨 PR 系统性二轮 delta

### §10.1 **Blocker** scenario seed namespace 与 long-run corpus seed namespace 全面撞表

见 §2.2.1、§9.2.4。

PR-00 scenario seed `2026042401~2026042407` 与 PR-06 corpus seed `2026042401~2026042416` 头部重叠 7 个。需要分配独立 namespace。

### §10.2 **Blocker** `ValidationScenarioRegistryTest.kt` / `Phase4MetricCatalog.kt` 等多 PR 共享文件的串行 merge 冲突未规划

见 §1.2.3。

7 条 PR 同时改 `ValidationScenarioRegistryTest.kt`；6+ 条 PR 同时改 `Phase4MetricCatalog.kt`。每次合入都需要 rebase 全部下游分支。

**修复方向**：README §6 新增 “共享文件串行写入序”，按 PR 顺序指定 merge-first 的 PR 负责打开文件，下游 PR 必须在 rebase 时刷新。或要求把 catalog 拆成按 domain 分 sub-file（`Phase4MetricCatalog_Talent.kt`、`Phase4MetricCatalog_Inscription.kt` 等）。

### §10.3 **Major** PR-00 acceptance 与 PR-01~07 materialization 循环依赖

见 §2.2.2。

PR-00 自己的 acceptance 使用 `phase4-v4-pr01` scenario 作为验收案例，但该 scenario 的 materialization 在 PR-01。

**修复方向**：见 §2.2.2（增加 `phase4-v4-pr00-selftest` scenario）。

### §10.4 **Major** PR-04 / PR-05 / PR-07 之间的 trigger / slot / mapgen 跨文件依赖未声明

* PR-05 依赖 PR-04 的 `void_pressure_active`（§7.2.1）。
* PR-05 依赖 `oil_or_fire_seen`（§6.2.3）— 产者未决。
* PR-07 依赖 `secondarySecretSlot`（§9.2.1）— 产者未决。

README §6 只说 PR-04 / PR-05 / PR-07 可在 PR-01 后并行开发。实际它们之间存在依赖链。

**修复方向**：README §6 重绘依赖关系图：

```
PR-00 -> PR-01 -> PR-02 -> PR-03 -> PR-06
              \-> PR-04 -> PR-05
                      \-> PR-07
```

PR-05 串行依赖 PR-04；PR-07 串行依赖 PR-04；PR-04 与 PR-05 / PR-07 不能并行。

### §10.5 **Major** 12+ blocking metric cutover 在 3 个 PR 内集中落地，PR scope 过大

PR-03 升 12 个 blocking metric（§5.2.6）；PR-04 升 4 个 blocking + 4 个 report-only（§6.2.4）；PR-01 升 5 个 blocking（§7 table）。合计 21 个 blocking owner metric + 若干 report-only。

三个 PR 同时引入如此大量 baseline 文件更新，任一 baseline 数值波动会导致多指标连锁 fail，debug 成本高。

**修复方向**：
* README §5.1 新增 “单 PR blocking metric 新增上限 = 5”，超过上限的 PR 必须按 domain 拆分（PR-03 拆为 PR-03a capstone / PR-03b slot balance / PR-03c affix distribution）。
* 或 PR-03 / PR-04 在 PR description 中列出 metric 的落地顺序与回滚计划。

### §10.6 **Minor** `sample.flooded_relics` seed `2026042407` 与 LONG_RUN_SMOKE 同 seed 的确定性假设未验证

见 §9.2.4。两个 run 用同 seed 但 preset 不同，`FoundationGameSession.random` 可能因 preset 初始化差异给出不同序列。应在 `HeadlessRunHarness` 或 `ValidationSession` 中显式声明 “seed 初始化从同一 canonical RNG seed 出发；preset 不影响 RNG 种子本身”。

### §10.7 **Minor** `Phase4MetricCatalog` 中 `contentPackHarness` / `whiteBoxContentPack` / `bossHarness` 三个 producer 的 freshness 约束未显式跨 PR 说明

一轮报告已经要求 PR-07 `contentPackHarness + whiteBoxContentPack` 同批刷新。二轮 PR-07 §8 item 9 确认。但 PR-05 没有类似 “boss harness 与 phase override data 同批刷新” 的显式规则。若 boss variant YAML 先合入、`bossHarness` 产物没刷，`reportPhase4` 会读到 stale coverage。

**修复方向**：README §4.1 快路径表增加 “freshness dependency”，每个 PR 的 producer 与对应数据必须在同一 build 内刷新。

---

## §11 二轮硬阻塞清单

按优先级从高到低：

| # | 级别 | 位置 | 问题 |
| --- | --- | --- | --- |
| B-R2-01 | Blocker | PR-06 §5.1 | seed `2026042406 full_route` 穿越 `crystal_cavern`（非 mandatory），与 full_route 口径矛盾（§8.2.1） |
| B-R2-02 | Blocker | PR-03 §5.1 | capstone × profession 权重矩阵只覆盖 6/13 capstone item，其余 7 项评分未定义（§5.2.2） |
| B-R2-03 | Blocker | PR-03 §5.2 | `baseScore` 全局未定义，所有 bonus / penalty 乘子失去锚点（§5.2.3） |
| B-R2-04 | Blocker | PR-03 §5.4 | `topFiveAffixExposureShare <= 40%` 与单项 affix `<= 15%` 之间算术边界未显式协调（§5.2.1） |
| B-R2-05 | Blocker | PR-05 §5.2 + PR-04 | `abyssal_eclipse` 依赖 `void_pressure_active`，但 README §6 串行守则未声明 PR-05 串行依赖 PR-04（§1.2.1 / §7.2.1） |
| B-R2-06 | Blocker | PR-05 §5.2 + PR-04 | `molten_glass` 依赖 `oil_or_fire_seen`，无产者（§6.2.3 / §7.2.2） |
| B-R2-07 | Blocker | PR-00 §5.2 + PR-06 §5.1 | scenario seed `2026042401~2026042407` 与 long-run corpus 前 7 条 seed 撞表（§2.2.1 / §10.1） |
| B-R2-08 | Blocker | PR-00 §6.2 / README §4 | PR-00 acceptance 使用 `phase4-v4-pr01` scenario，但该 scenario materialization 在 PR-01，产生循环依赖（§2.2.2 / §10.3） |
| B-R2-09 | Blocker | PR-07 §5.2 + PR-04 | `secondarySecretSlot` 概念跨 PR 引用，PR-04 未定义（§9.2.1） |
| B-R2-10 | Blocker | PR-07 §5.2 | `fixedSeedVisibilityCase` 的 force-inject 机制在 production 代码中引入 test-only backdoor（§9.2.2） |
| B-R2-11 | Blocker | PR-01 §5.4 | Tier 2 门槛表无“前置 rank”字段，但 data loader fail-fast 要校验“Tier 2 前置 rank >= 2”（§3.2.1） |
| B-R2-12 | Blocker | PR-01 §5.4 + §7 | `multiTreeInvestmentRate >= 75%` 因起始树分布已自动满足，blocking 指标无信号（§3.2.2） |
| B-R2-13 | Blocker | PR-04 §5.3 | `search_available` / `secret_entry_nearby` HIGH priority 与战斗 HIGH priority 的冲突消解未定义（§6.2.2） |
| B-R2-14 | Blocker | README §6 | 共享文件（`ValidationScenarioRegistryTest.kt`、`Phase4MetricCatalog.kt`、`Phase4OwnerMetricTargets.kt`、`aggregation-manifest.yaml`）串行 merge 冲突无消解序（§1.2.3 / §10.2） |

**上一轮 25 条全部已受理**；本轮 14 条 Blocker 主要来自：修复时把单点补丁做到位但未回头校对跨 PR 引用（B-R2-05 / 06 / 09）、metric 边界未闭合（B-R2-02 / 03 / 04 / 12）、seed / 文件命名空间未做系统规划（B-R2-07 / 14）、data loader 校验与表格 schema 之间漂移（B-R2-11）。

---

## §12 Major / Minor / Nit 汇总

### §12.1 Major（15 条）

1. README §6 未声明 PR-07 对 `secondarySecretSlot` 的依赖（§1.2.2）。
2. README §6 共享文件串行冲突（与 B-R2-14 关联，Major 层面的具体改法） (§1.2.3)。
3. PR-00 `routeIndex` 字段语义未定义（§2.2.3）。
4. PR-00 `REPORT_SUMMARY_SCREEN` enum 无 PR 使用（§2.2.4）。
5. PR-00 未知 scenario id 失败态 UX 细节缺失（§2.2.5）。
6. PR-00 `PHASE4_V4_FAST` action 缺 idempotency 合同（§2.2.6）。
7. PR-01 满槽 Learn + Esc modal 路径静默扣点（§3.2.3）。
8. PR-01 `autoLearnedNonStarterTalentCount=0` blocking 是 schema 不变量的 tautology（§3.2.4）。
9. PR-01 `log.talent.breakpoint_chosen` 在多 talent / 多 breakpoint draft 下语义未定（§3.2.5）。
10. PR-02 `replace` 函数签名未返回新 loadout（§4.2.1）。
11. PR-02 `ShopPurchaseFailure.INSCRIPTION_EQUIP_REJECTED` 丢失下层原因（§4.2.2）。
12. PR-03 `terminalIdentityBonus` 触发条件不明确（§5.2.4）。
13. PR-03 `cinderveil_plate` vs `deepcurrent_lens` tie-break 未定（§5.2.5）。
14. PR-03 12 个 blocking metric 集中在同一 baseline 文件（§5.2.6）。
15. PR-04 新增 8 个 owner metric 在单 PR 压力过大（§6.2.4）。
16. PR-05 `actionEmphasisIds` 的 base boss action 归属未校验（§7.2.4）。
17. PR-06 `grey_gate_depths` / `abyssal_temple` 禁作 start zone 无显式声明（§8.2.2）。
18. PR-06 `probeRouteHash` 未登记到模型 / report（§8.2.3）。
19. PR-06 `branchInclusiveCount >= 3` 与 corpus 固定 `4` 门槛不一致（§8.2.4）。
20. PR-07 fixture pack id `.` 分隔符与 namespace `.` 冲突（§9.2.3）。
21. PR-07 `samplePackAddOnlyMainPath` 与 `manifestLint` 语义重复（§9.2.5）。
22. 跨 PR blocking metric 集中度过高（§10.5）。

（上述条目多于 15 条，实际按 Major 级别的 22 条。）

### §12.2 Minor（18 条）

集中于：i18n key 登记漏项（PR-01、PR-02、PR-03、PR-07）、数值合同中未显式写 magnitude 的 hook / penalty（PR-04 runtime hook、PR-03 `lateCommonPenalty`、PR-03 `duplicateSlotPenalty`）、cue TTL 偏短（PR-04 `search_available`）、tree 命名耦合（PR-01）、pack loader 容量（PR-07）、overridden key 语义（PR-07）等。

### §12.3 Nit（11 条）

集中于：表格格式宽度（PR-02 `Recovery: 2/2 -> 2/2 | Movement: ...`）、表头命名一致性（README §2.2 tier 表）、footer 文案 i18n key 缺失（PR-01 §5.6）、telegraph 命名的 failSemantics 字段对应（PR-05 §7）、report 字段位置层级（PR-07 §5.2 yaml 示例缺失）等。

---

## §13 肯定项

1. **一轮 25 条 Blocker 全部已受理**：破坏性 cutover 纪律、PR-00 前置化、Tier 口径、rogue starter 修正、`InscriptionEquipCheck` sealed、ScoreFormula、runtime hook 最小接线、`TriggerExpression` sealed、route hash 16-hex、hiddenBranchBindings 等核心决策全部落地，方向正确。
2. **README §2.1 长期稳定优先与破坏性改造纪律**：7 条规则为 PR-01~07 的 schema cutover 提供了不可绕过的共同前置，避免了 v4 的 “旧兼容包袱” 回潮。
3. **README §2.2 tier 口径 + includedProfessions / advancedReportOnlyProfessions / excludedFrozenProfessions 三列输出**：在有限开发资源下把 `shadowblade / warden` frozen 状态显式化，release gate 分母干净。
4. **PR-00 `ValidationScenarioDef` 数据类 + `PHASE4_V4_FAST` 四 action contract**：消除了 7 条 PR 各自搭 overlay 的熵；cue TTL、evidence checklist、manual record 模板全部归一化。
5. **PR-01 `TalentNodeState` 四态 + §5.3 起始树分布表**：UI 与 report 的 node 状态语言统一；起始分布表把设计意图显式化，避免实现方反推 data。
6. **PR-02 sealed `InscriptionEquipCheck`**：把失败原因编译期类型化，一轮 Boolean-reverse 反推的隐患被彻底清除。
7. **PR-03 §5.3 family 聚合 + §5.5 quality floor**：5 family 分母口径 + rogue OFF_HAND COMMON 硬 ban 让 “milestone 被普通 item 压身份” 的现象有了直接修正路径。
8. **PR-04 §5.2 runtime hook 最小接线表**：每个 zone 一个 hook，一对一映射，避免 “机制名词成第二真源”。
9. **PR-05 `phaseGraphUnchangedReason=data_level_override_only` 字段**：把 `phaseGraphUnchanged=true` 与 coverage `3/3` 并存的语义显式化，防止 owner review 误读。
10. **PR-06 route hash `sha256.take(16)` + `routeTokenSample`**：16-hex / 64-bit 确定性 hash + sample 人工核对字段，在 diversity 指标与 debug 之间建立桥梁。
11. **PR-07 `hiddenBranchBindings` + `samplePackSecondarySecretSlotUsed` + `samplePackForcedVisibilityCase` report 字段**：把 force-inject 的使用事实显式化，便于审计。

---

## §14 更新后的开工成功率估算

对比一轮报告的估算（PR-00 缺失；PR-03 / PR-06 结构漂移；总估算 30%~55%）：

| PR | 一轮估算 | 本轮估算（修 B-R2-*后） | 本轮估算（不修 B-R2-*直接开工） |
| --- | ---: | ---: | ---: |
| PR-00 | n/a | 80% | 45% |
| PR-01 | 45% | 80% | 55% |
| PR-02 | 55% | 85% | 70% |
| PR-03 | 30% | 70% | 40% |
| PR-04 | 40% | 75% | 50% |
| PR-05 | 45% | 75% | 45% |
| PR-06 | 35% | 75% | 40% |
| PR-07 | 50% | 80% | 55% |

**解读**：

1. 若 §11 的 14 条 Blocker 在开工前修复，7 个业务 PR 的首次通过率可由一轮的 30%~55% 提升到 70%~85%。
2. 若 B-R2-01 / 02 / 03 / 04（PR-03 / PR-06 结构 / 数值边界）不修直接开工，实现期会大量 debate 数值口径，PR 必被中途打回或产出多个 followup。
3. B-R2-07 / 14（seed / 共享文件冲突）是“落地后才爆”的典型陷阱，开工前花 1 天重划 seed namespace + catalog 拆分成本低；开工后 rebase 全部下游 PR 代价高。

---

## §15 优先修复顺位建议

按 “修了解除下游多 PR 阻塞” 的杠杆度排序：

1. **B-R2-07**（seed namespace 重划）— 影响 PR-00、PR-06、PR-07。**先修**。
2. **B-R2-14**（共享文件串行冲突消解）— 影响 PR-01~07 全部。**先修 README §6**。
3. **B-R2-08**（PR-00 selftest scenario）— 解除 PR-00 的循环依赖。
4. **B-R2-05 / 06 / 09**（PR-04 / 05 / 07 跨依赖）— 一次性在 README §6 依赖图中落定。
5. **B-R2-01**（PR-06 `2026042406` crystal_cavern 分类）— 单条 seed 替换。
6. **B-R2-02 / 03 / 04 / 12**（PR-03 数值合同 + PR-01 `multiTreeInvestmentRate`）— 单 PR 内修。
7. **B-R2-10**（PR-07 force-inject 从 production 代码移到 test-support）— PR-07 内修。
8. **B-R2-11**（PR-01 Tier 2 fail-fast 校验对齐）— 单条表格对齐。
9. **B-R2-13**（PR-04 cue priority 分层）— 单文档新增 `HIGH_COMBAT / HIGH_HIDDEN` 两档。

---

## §16 结语

Lky 已经在一轮 25 条 Blocker 全部受理的前提下，把 8 份文档推到接近 “可开工” 的状态。二轮报告的 14 条新 Blocker 都是 “一轮修复过程中未顺延校对跨 PR 引用 / 数值边界 / 命名空间” 所引入，体量与信号度远低于一轮。若 §15 顺位的 14 条修完，PR-00~PR-07 可以作为一个有序开发链条进入实现期；实现期剩下的主要风险转为 data tuning（`baseScore` 区间、affix 权重、Tier 3 pacing），而不再是 schema / 依赖 / 语义层的返工。

建议 lky 按 §15 顺位，先花 1~2 天完成 README §6 依赖图重绘 + seed namespace 重划 + 共享文件拆分原则三项结构性修复；再让 PR-00 按 “selftest scenario” 先行；PR-01~07 按 PR-00 → PR-01 → PR-02 → PR-03 → PR-04 → PR-05 / PR-07（并行）→ PR-06（最后） 的串行顺序推进。

---

**审阅结束**。本报告共 14 条 Blocker + 22 条 Major + 18 条 Minor + 11 条 Nit + 11 条肯定项。

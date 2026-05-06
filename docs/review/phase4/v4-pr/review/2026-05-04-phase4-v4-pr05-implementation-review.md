# Phase4 v4 PR-05 Boss Variant Phase Language 实施审查

- **审查日期**：2026-05-04
- **审查对象**：当前分支 `codex/phase4-v4-pr05-boss-variant-phase-language` 工作树相对 `main` 的全部改动
- **审查依据**：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md`
- **审查身份**：资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
- **报告语言**：简体中文（代码标识符保留英文）

---

## 0. 总评

PR-05 把"data-level phase language"的骨架基本搭起来了：

- `BossVariantDef.phaseOverrides` 由 `MutationModels.kt` 独占 schema owner，3 个 variant（`molten_glass / grey_crown / abyssal_eclipse`）均按 §5.2 表格落点。
- core 层 `TriggerExpression` 完整支持 `Ref / AllOf / AnyOf / Not` 与 ≥2 child 校验；`BossPhaseManager` 在 base phase 达成后再评估 override，并写出 `phaseOverrideSkippedReason`。
- runtime 接线（trigger materialization → override resolution → telegraph + onEnterEventKey + 1.5x action emphasis）端到端走通。
- bossHarness 的 4 个 owner 指标（schema / runtimeTrigger / telegraph / actionDistinct.reportOnly）全部进入 `Phase4MetricCatalog` 与 owner baseline，`phaseGraphUnchanged=true` 与 `phaseGraphUnchangedReason=data_level_override_only` 同时出现。
- `phase4-v4-pr05` validation scenario、golden screenshot、clientSmoke、manual record 均已落地，packaged-app 白盒 4 张截图完整。

但是从「开发设计总监 + 系统策划总监 + 玩法体验审查负责人」三视角看，仍存在 **5 类与规范相冲突或语义偏离** 的偏差，按严重度排序如下：

| 等级 | 类别 | 摘要 |
| --- | --- | --- |
| **High** | runtime 接线归属 | 规范 §5.1 runtime 第 1 条要求由 `BossFactory` 把 `BossVariantPhaseOverride` 解析为 core semantic override 并传入 `BossEncounter`；实际整段 wiring 落在 `FoundationGameSession.bossEncounterFor`，`BossFactory.kt` 完全没有 variant / phaseOverrides 的概念 |
| **High** | trigger 语义 | 规范 §5.2 第 6 条要求 `boss.trigger.war_caller_active` 含义为 "war caller 伴生单位存活或 aura 仍在 encounter 生效"；实现把它降级为 `EliteMutationLoadout.mutationIds.contains("elite.war_caller")`，是「装载位是否带这个 mutation」而不是「伴生单位/aura 是否存活」 |
| **Medium** | 测试落点 | 规范 §3.1 列出的测试文件 `MutationModelsTest.kt`、`BossVariantDataLoaderTest.kt`、`BossHarnessRunnerTest.kt` 均不存在；`BossFactoryTest.kt` 存在但只测 talent / cooldown 不覆盖 phase overrides；boss harness 测试在 `game/.../BossHarnessTest.kt` 而非 `tools/.../boss/BossHarnessRunnerTest.kt` |
| **Medium** | 范围声明 | 规范 §3.1 列出的 `tools/src/main/kotlin/com/ktome/tools/boss/**` 不存在；指标 / 报告接线全部走 `tools/phase4/**`。命名层面与文档不符 |
| **Low** | schema 文档与代码不一致 | 规范 §5.1 schema 代码块只列 `Ref / AllOf / AnyOf`，但同段第 4 条又要求 core 支持 `Not`；实现选择支持 `Not`，与运行时要求一致，但与 schema 代码块不匹配（属规范本身内部矛盾） |
| **Low** | 人工白盒 | manual record 结论 `PASS_WITH_AUDIO_CAPTURE_LIMITATION`；§6.3 通过标准 #2「action emphasis 在战斗日志或行动表现中可分辨」对 `molten_glass` 在表格里没明确截图列出 `linebreaker / earthshaker` 的差分动作日志；其余两 variant 表格描述含 `战场指挥` / `虚空裂隙` 等中文动作日志条目，但截图证据是 phase warning，不是 action emphasis |

**完成定义对照（§9）**：1, 2, 3, 4, 5, 6, 8, 9, 10 客观满足；第 7 项（goldenScreenshot 与 client render snapshot 覆盖 3 个 variant warning）满足。但 §6.3 通过标准 #2（action emphasis 玩家可分辨）需要更直接的证据补强，详见 §3.5。

---

## 1. Acceptance Matrix 实施判定

| reqId | 范围 | 实施判定 | 评分 | 主要偏差 |
| --- | --- | --- | --- | --- |
| `PR05-M01` | §5.1 phaseOverrides schema | 基本实施 | 88/100 | runtime wiring 第 1 条由 `BossFactory` 提供未落点；schema 代码块 `Not` 缺失（规范内部矛盾） |
| `PR05-M02` | §5.2 variant 固定内容 | 基本实施 | 80/100 | `war_caller_active` 语义降级为 loadout 标志；molten / abyssal 的 zone trigger 接线正确 |
| `PR05-M03` | PR-04 zone trigger 依赖 | 通过 | 95/100 | `zone.trigger.oil_or_fire_seen` / `zone.trigger.void_pressure_active` 在 `ZoneMechanicRuntime` 与 `bossVariantActiveTriggerIds` 双向打通 |
| `PR05-M04` | §5.3 telegraph specs | 通过 | 95/100 | telegraph index 三条 ADD 完整，visualKey / audioProfile 与表格逐字一致；client golden 与 smoke 双重覆盖 |
| `PR05-M05` | §5.4 boss harness 指标 | 通过 | 95/100 | 4 个新指标全部接入；`phaseGraphUnchanged` / `phaseGraphUnchangedReason` / `bossVariantStructuralDivergenceNote` supporting 指标三件齐 |
| `PR05-M06` | governance 继承 | 基本实施 | 85/100 | acceptance matrix / artifact / failure rule 段落齐备；§3.1 路径声明与实际不符（tools/boss、若干测试文件名） |

> 评分仅作为"完成度"内部参考，不替换 fastCheck / ownerGate 的硬性判定。

---

## 2. 章节级核查

### 2.1 §3.1 范围与路径偏差 — 与 `PR05-M06` 关联

#### 2.1.1 `tools/src/main/kotlin/com/ktome/tools/boss/**` 整段不存在

- **规范**：§3.1 生产代码列出 `tools/src/main/kotlin/com/ktome/tools/boss/**` 与 `tools/src/main/kotlin/com/ktome/tools/phase4/**`。
- **现状**：仓库中没有 `tools/.../boss/` 目录。bossHarness 指标与 phaseGraphUnchanged 处理全部位于 `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4MetricCatalog.kt` (lines 522-553) 与 `Phase4DomainArtifactRegistry.kt` (lines 344-347)；boss harness 测试位于 `game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt`。
- **偏差性质**：**仅命名/范围声明** 偏差，功能上全部由 `tools/phase4/**` 与 `game/.../harness/**` 承接。
- **修复建议**：
  1. 二选一：① 把规范 §3.1 修订为 `tools/src/main/kotlin/com/ktome/tools/phase4/**`；② 在 `tools/.../boss/` 新建一个最小 facade（例如 `BossHarnessOwnerMetrics.kt` 重导出指标 id 与 owner baseline 路径），用以承担 §3.1 的"声明意图"。
  2. 推荐 ①，因为再开一个 `tools/boss/**` 包只是为了对齐文档而新增空壳，违反"避免不必要复杂度"。

#### 2.1.2 §3.1 测试文件清单与现状不符

| 规范期望路径 | 现状 | 实际等效覆盖 |
| --- | --- | --- |
| `core/.../BossPhaseManagerTest.kt` | ✅ 存在 | AllOf / AnyOf / Not、≥2 child、override gating、skipped reasons |
| `game/.../elites/MutationModelsTest.kt` | ❌ 不存在 | 部分覆盖在 `game/.../GameContentTest.kt`、`DataLoader` 端到端 test，但缺 `BossVariantPhaseOverrideContracts.validateReferences` 单元测试 |
| `game/.../BossVariantDataLoaderTest.kt` | ❌ 不存在 | 覆盖散落在 `DataLoader` 的若干 fixture-driven 测试中，没有以 boss variant phase override 为主轴的 test class |
| `game/.../BossFactoryTest.kt` | ✅ 存在 | 仅测 resource pool / cooldown / talent，不涉及 phase overrides（与 §2.2.1 偏差耦合） |
| `game/.../hidden/HiddenContentMapgenPipelineTest.kt` | ✅ 存在（PR-04 遗留） | 与 PR-05 无直接关联 |
| `client/.../render/**/*SnapshotTest.kt` | ✅ 通过 `GoldenScreenshotHarnessTest` 与 `ClientSmokeHarnessTest` 覆盖 |
| `tools/.../boss/BossHarnessRunnerTest.kt` | ❌ 不存在 | 等效覆盖在 `game/.../harness/BossHarnessTest.kt`（≈1700 行） |
| `tools/.../phase4/Phase4ReportRunnerTest.kt` | ✅ 存在 | OK |

- **偏差性质**：覆盖率上 PR-05 关键路径都被测到了；但「测试文件命名 / 模块归属 / 单元粒度」与规范不符，违反 §3.1 第 1-3 条「测试职责固定为三层」中"`game` 测 PR-04 zone hook fact materialization、把 zone.trigger.* 转成 encounter-local semantic trigger ids"的语义——目前这部分校验只能在 `BossHarnessTest` 大粒度场景中间接命中，缺少专门的 `BossVariantDataLoaderTest` / `MutationModelsTest` 来定向 fail-fast 用例。
- **修复建议**（按 ROI 排序）：
  1. **High ROI**：新增 `game/src/test/kotlin/com/ktome/game/elites/MutationModelsContractsTest.kt`，以 fail-fast 表驱动方式覆盖 `BossVariantPhaseOverrideContracts.validateReferences` 的所有抛点（unknown phase / unknown trigger / unknown telegraph / unknown action emphasis / illegal onEnterEventKey）。这是规范 §6.1 第 3 条直接要求的 fail-fast 行为。
  2. **Medium ROI**：新增 `game/src/test/kotlin/com/ktome/game/data/BossVariantDataLoaderTest.kt`，以 YAML fixture 驱动覆盖 `parseTriggerExpression`「ref / allOf / anyOf / not 互斥」与 `parseBossVariantPhaseOverrides` 解析正确性。
  3. **Low ROI**：把 `game/.../BossHarnessTest.kt` 的 phase override 部分 split 到 `tools/.../boss/BossHarnessRunnerTest.kt`，仅当未来 boss harness runner 移到 tools 模块时再做。

### 2.2 §5.1 Schema 与 runtime 接线 — 与 `PR05-M01` 关联

#### 2.2.1 `BossFactory` 没有承接 phase override wiring（**High**）

- **规范** §5.1 runtime 第 1 条：「`BossFactory` 将 `BossVariantPhaseOverride` 解析为 core semantic override，传入 `BossEncounter`。」
- **现状**：
  - `game/src/main/kotlin/com/ktome/game/factory/BossFactory.kt` 整文件 0 处出现 `phaseOverrides` / `BossVariantDef` / `variant` 等字眼。
  - 实际 wiring 位于 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:4149` 的 `bossEncounterFor(monsterId)`：先从 ECS 拿 `EliteMutationLoadout`，再从 `gameContent` 拿 variant，最后 `baseEncounter.copy(phaseOverrides = variant.phaseOverrides)`。
  - 这意味着 `BossFactory.createBoss` 阶段不知道 variant 的存在，phase overrides 是在 `FoundationGameSession` 每次需要 `bossEncounterFor` 时即时拼装的。
- **偏差影响**：
  1. 违反 §5.1 runtime 第 1 条的明文要求，scope 与责任划分被搬到了 `FoundationGameSession`——后者已经是接近 9k 行的"god class"，把 boss variant 的 phase override 装配也塞进去会让该文件继续膨胀。
  2. 玩法体验视角：当前实现是「lazy」语义——每次需要时从 elite mutation loadout 反查 variant、再从 GameContent 拿 phaseOverrides 拼接。这对 save / load 与 mid-encounter rebuild 不一定健壮（拼接结果不被 entity 持久化为组件），未来若 EliteMutationLoadout 中途变更，phase overrides 会跟着变。
  3. 系统策划视角：BossFactory 是 game/factory 层，`FoundationGameSession.bossEncounterFor` 是会话层。把 schema → encounter 的 binding 放到会话层意味着任何"非 FoundationGameSession 的玩法测试"都难以直接拿到带 phaseOverrides 的 BossEncounter——见 §2.1.2 中 `BossFactoryTest` 没有覆盖 phaseOverrides 的根因。
- **修复建议**（按改动幅度递增）：
  1. **最小修复**（Low effort）：在 `BossFactory.createBoss` 接收一个可选 `BossVariantDef? = null`，若非空则把 `definition.encounter.copy(phaseOverrides = variant.phaseOverrides)` 写入 `BossEncounterState`，并在 ECS 上挂一个 `BossVariantBinding` 组件。`FoundationGameSession.bossEncounterFor` 直接读取 ECS 组件而不再实时拼接。
  2. **完整修复**（Medium effort）：把"variant → encounter"绑定抽到 `BossFactory` 层的私有 helper（命名建议 `BossFactory.bindVariantPhaseOverrides`），让 `FoundationGameSession` 只负责 trigger materialization 与 phase enter event 派发，不参与 schema 装配。
  3. 同时新增 `BossFactoryTest` 用例覆盖 "variant 为 null"、"variant 提供 3 个 override" 两条路径。
- **保留替代**：若评估认为 `bossEncounterFor` 的"懒拼接"对 multi-pack overlay / runtime variant 切换更友好，可将规范 §5.1 runtime 第 1 条改写为「`BossFactory` 或等价的 game-layer assembly 在 BossEncounter 装配时承接 phaseOverrides」并在文档中写明 owner 文件路径。**任何一种都比当前"规范明文未落地"更可接受**。

#### 2.2.2 schema 代码块缺 `Not` — **Low**（规范本身的内部不一致）

- **规范** §5.1 schema 代码块（`sealed interface TriggerExpression`）只列了 `Ref / AllOf / AnyOf`，但同节第 4 条 runtime 写明 "core 只对 trigger id 执行 `AllOf` / `AnyOf` / `Not` 语义匹配"。
- **现状**：`core/.../BossEncounter.kt` 实现了 `Not` 子类型，`BossPhaseManagerTest` 也覆盖了 `Not` 用例。
- **偏差性质**：实现遵循了 runtime 条款，但与文档的 schema 代码块不一致——这是规范本身的内部矛盾。
- **修复建议**：
  1. 把规范 §5.1 schema 代码块补上 `data class Not(val child: TriggerExpression) : TriggerExpression`。
  2. 同步在 §5.1 第 8 条「AllOf / AnyOf 必须至少 2 个 child」后面加一条「`Not` 必须包含恰好 1 个 child」的说明，避免歧义。

#### 2.2.3 DataLoader fail-fast 改为 `validateBossVariantPhaseOverrideContracts`

- **规范** §5.1 第 7 条与 §6.1 第 3 条：「`DataLoader.parseBossVariants` 必须 fail fast 校验 phase、trigger reference、telegraph、action emphasis 和 on-enter event key。」
- **现状**：fail-fast 拆成两段——
  - `DataLoader.parseBossVariantPhaseOverrides` 解析 YAML 时校验 trigger expression 互斥（`ref / allOf / anyOf / not` 仅一项），属"语法层"校验。
  - `DataLoader.validateBossVariantPhaseOverrideContracts(catalog)` 在 `loadSchemaCatalog()` 完成 base/load + content-pack overlay 后调用，做"跨表引用"校验。
  - `GameContent.init` 再调一次 `BossVariantPhaseOverrideContracts.validateReferences` 做 defense-in-depth。
- **偏差性质**：**功能等价**（仍然在数据加载完成、运行时启动前 fail-fast），结构上偏离了"`parseBossVariants` 单点 fail fast"的规范字面要求。三层校验略冗余但可接受。
- **修复建议**：
  1. 在规范 §5.1 第 7 条加一行注解："允许把跨表引用校验放在 `loadSchemaCatalog()` 末尾的专用 validator，但必须发生在 runtime 任何 boss spawn 之前。"
  2. 评估是否要去掉 `GameContent.init` 中的重复校验——它在生产路径上被 `DataLoader` 包含；只有在 unit test 中绕过 DataLoader 直接构造 GameContent 时才有用。如果有 test 路径仍依赖该校验，保留即可。

### 2.3 §5.2 Variant 固定内容 — 与 `PR05-M02` 关联

#### 2.3.1 `boss.trigger.war_caller_active` 语义降级（**High**）

- **规范** §5.2 第 6 条："`boss.trigger.war_caller_active` 由 `grey_crown` base encounter 状态机产生，含义固定为 war caller 伴生单位存活或 war caller aura 仍在当前 encounter 生效；该 trigger 不从 zone hook 或 client presentation 推导。"
- **现状**：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` 的 `bossVariantActiveTriggerIds` 中：
  ```kotlin
  if (loadout.mutationIds.contains("elite.war_caller")) {
      add(BossVariantPhaseOverrideContracts.WAR_CALLER_ACTIVE_TRIGGER_ID)
  }
  ```
  也就是说，trigger 只看「这个 boss variant 装载了 elite.war_caller mutation」，不看 war caller 伴生单位（companion entity）当前是否存活、不看 war caller aura 是否还在 encounter 生效。
- **偏差影响**：
  1. **玩法体验偏差**：玩家击杀掉 grey crown 周围的 war caller companion 后，规范期望 trigger 立即失效（`war_caller_active` 不再活跃 → AllOf 失败 → override phase 不进入）。当前实现不会失效——只要 boss 自身的 mutation 装载位还有 `elite.war_caller`，trigger 永远活着。
  2. **系统策划偏差**：grey_crown 的策划意图是「指挥型 boss + 战吼伴生」的双层威胁；玩家可以选择优先击杀 war caller companion 来削弱 boss desperate phase 的爆发。当前实现把这个策略空间变成了「不可压制」——降低了 grey_crown 的玩法深度。
  3. **owner 证据偏差**：bossHarness 的 `phaseOverrideRuntimeTriggerCoverage` 计算只检测 trigger 是否「曾经触发」，不检测 trigger 是否能「正确失效」。这意味着 owner gate 也无法暴露这个语义降级。
- **修复建议**（按完成度递增）：
  1. **最小修复**（必须做）：在 `bossVariantActiveTriggerIds` 中增加 companion-alive 检查。grey_crown variant 应在 spawn 时把 war caller companion 作为独立 ECS entity 落地（看是否已经存在 companion spawn 机制；若没有则属 PR-05 范围外的更大改造）。若 companion 机制已存在，把 `if (loadout.mutationIds.contains("elite.war_caller"))` 替换为 `if (companionAliveOf("elite.war_caller", bossEntityId))`。
  2. **完整修复**：在 `BossEncounterState` 上增加 `auraTracker: BossAuraTracker`，由 `applyBossPhaseEnter` / `applyBossPhaseExit` 维护；`war_caller_active` 当且仅当 (companion 存活 OR aura 在 tracker 中且未过期) 时返回 true。
  3. **测试**：在 `BossPhaseManagerTest` 与新建的 `MutationModelsContractsTest` 中加 case：
     - companion 击杀后 trigger 失效 → override phase 不进入。
     - aura 过期后 trigger 失效。
- **回退备选（仅在 companion 系统不存在时）**：把规范 §5.2 第 6 条改为「装载位 + 击杀状态」组合，明确写出语义降级的边界与玩家预期，避免实现与规范继续偏离。

#### 2.3.2 其余两条 trigger 语义 — 通过

| trigger | 规范期望 | 实现 | 判定 |
| --- | --- | --- | --- |
| `zone.trigger.oil_or_fire_seen` | 由 PR-04 zone hook materialize | `ZoneMechanicRuntime.kt` slag_alert hook 写入 `warningEventId`；`bossVariantActiveTriggerIds` 从 `zone.discoveryTags` 过滤 `ZoneTriggerFactIds` 已知 id 后注入 | ✅ 通过 |
| `zone.trigger.void_pressure_active` | 由 PR-04 zone hook materialize | 同上路径，ZoneTriggerFactIds 已含 `VOID_PRESSURE_ACTIVE_TRIGGER_ID` | ✅ 通过 |
| `boss.trigger.hp_below_50/45/40` | 由 boss-encounter-schema 提供 | `bossVariantActiveTriggerIds` 按 `currentHp / maxHp` 阈值实时计算 | ✅ 通过 |

#### 2.3.3 `actionEmphasisIds` 校验 — 通过

- 规范 §5.2 第 5 条："`DataLoader` 必须从 `bosses/index.yaml` 读取 base boss action 全集；`actionEmphasisIds` 不属于该集合时 fail fast。"
- 实现：`BossVariantPhaseOverrideContracts.validateReferences` 接收 `allowedActionIds` 集合，逐个 check 后在不命中时 `error("Boss variant ... action emphasis ... not in base boss actions")`。`DataLoader.validateBossVariantPhaseOverrideContracts` 在调用前从 `bosses/index.yaml` 解析 `actions` 全集传入。✅ 通过。

### 2.4 §5.3 Telegraph specs — 与 `PR05-M04` 关联，全部通过

| spec | visualKey 期望 | visualKey 实际 | audioProfile 期望 | audioProfile 实际 | 判定 |
| --- | --- | --- | --- | --- | --- |
| `molten_glass_phase_override_warning` | `vfx.boss.variant.molten_glass` | ✅ | `audio.boss.variant.molten_glass` | ✅ | 通过 |
| `grey_crown_phase_override_warning` | `vfx.boss.variant.grey_crown` | ✅ | `audio.boss.variant.grey_crown` | ✅ | 通过 |
| `abyssal_eclipse_phase_override_warning` | `vfx.boss.variant.abyssal_eclipse` | ✅ | `audio.boss.variant.abyssal_eclipse` | ✅ | 通过 |

`telegraph/index.yaml` 三条 ADD entry 的 visualKey 与 audioProfile 与规范表格逐字一致；`audioLint` 与 `assetLint` 走通。client 侧 `GoldenScreenshotHarnessTest.capturePhase4V4Pr05BossVariantWarningSet` 与 `ClientSmokeHarnessTest` 各自覆盖三个 warning id。

### 2.5 §5.4 Boss harness 指标 — 与 `PR05-M05` 关联，全部通过

| 字段 | 规范要求 | 实现 | 判定 |
| --- | --- | --- | --- |
| `variantTraceDivergenceRatio` | 现有 action trace 差异 | 由 PR-04 沿用 | ✅ |
| `phaseOverrideSchemaCoverage` | 3/3 | `bossVariantPhaseOverrideSchemaCoverage` owner 指标，`Phase4MetricCatalog` 注册，baseline `minValue=1.0` | ✅ |
| `phaseOverrideRuntimeTriggerCoverage` | 3/3 | 同上，`bossVariantPhaseOverrideRuntimeTriggerCoverage` | ✅ |
| `phaseOverrideTelegraphCoverage` | 3/3 | 同上，分母固定为 3 个 telegraph spec | ✅ |
| `phaseOverrideActionDistinctCount` | per-variant + min | report-only 指标，每 variant ≥ 2 实测 | ✅ |
| `phaseGraphUnchanged` | true 时附带 reason | 输出 `phaseGraphUnchanged=true` + `phaseGraphUnchangedReason=data_level_override_only` + `bossVariantStructuralDivergenceNote` | ✅ |

owner baseline `docs/review/phase4/opt/baselines/2026-04-16-phase4-boss-phase-identity-owner-baseline.json` 包含 4 条 PR-05 metric 范围且 `minValue` / 注释与规范 §7 一致；`metricDefinitionVersion=phase4-boss-phase-identity-v1` 维持原版本号（PR-05 是 reportOnly 扩展，不是阈值变更，无需 bump version——与 §7 表格 metricKind 区分一致）。

### 2.6 §6 测试与自证

#### 2.6.1 §6.1 必测行为 — 7 条逐项

| # | 必测行为 | 落点 | 判定 |
| --- | --- | --- | --- |
| 1 | 3 个 variant 均声明 `phaseOverrides` | `boss-variants/index.yaml` | ✅ |
| 2 | `BossVariantDef.phaseOverrides` 唯一 schema owner = `MutationModels.kt` | `MutationModels.kt:178` 是唯一定义点 | ✅ |
| 3 | `DataLoader.parseBossVariants` 对 6 个维度 fail fast | 见 §2.2.3，结构上分到 `validateBossVariantPhaseOverrideContracts`；功能等价 | ⚠️ 等价偏差 |
| 4 | 3 个 override telegraph 都能在 boss harness 中触发 | `phaseOverrideTelegraphCoverage=3/3` | ✅ |
| 5 | 3 个 owner 指标 = 3/3 | 实测 PASS | ✅ |
| 6 | `phaseGraphUnchanged` 继续作为说明 | OK | ✅ |
| 7 | packaged app 中玩家能看到 variant 专属 warning、阶段进入反馈和 action emphasis 差异 | manual record + 4 张 packaged-app 截图 | ⚠️ 见 §3.5 |

#### 2.6.2 §6.3 人工白盒 — `PASS_WITH_AUDIO_CAPTURE_LIMITATION`

- manual record 已写明 packaged app sha256、pid、scenario id、seed、4 张截图 sha256、metadata 一致性、Computer Use 音频不可采集的限制、owner gate 链路替代说明。
- §6.3 通过标准 #1（三个 variant 不同阶段 warning）：molten_glass / grey_crown / abyssal_eclipse 各一张 warning 截图，均 PASS。
- §6.3 通过标准 #2（action emphasis 玩家可分辨）：manual record 表格步骤 3、4 写明 "日志区显示 `战场指挥` / `虚空裂隙` 等阶段行动反馈"，但 step 2 molten_glass 没有显式写 `linebreaker / earthshaker` 行动日志条目；4 张截图主体是 warning，不专门聚焦 action emphasis 区。**这是当前白盒证据可强化点**。
- §6.3 通过标准 #3（report coverage 3/3）：`phase4-v4-pr05-report-coverage.png` 已捕获。✅
- §6.3 通过标准 #4（manual record 写明 packaged path / runtime home / seed / inputs / 截图路径 / 结论）：已满足。✅

### 2.7 §7 Report 与验收指标 — 通过

`Phase4MetricCatalog.kt` (lines 522-553) 4 条 metricSpec、`Phase4DomainArtifactRegistry.kt` (lines 344-347) 内部 → canonical 名映射、`Phase4OwnerMetricTargets.kt` 中 3 条 owner + 1 条 reportOnly formatter，全部就位。

### 2.8 §8 验证命令 — 通过

`build/whitebox/phase4-v4-pr05/launch-packaged-app.sh` + `preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr05` 全链路接入，`ValidationScenarioRegistry.kt:324-387` 注册 scenario、`Phase4V4WhiteboxScenarioMaterializationCatalog.kt` 注册材料化规格。

### 2.9 §9 完成定义 — 7 条 OK，1 条等价偏差，2 条形式 OK

| # | 完成定义 | 判定 |
| --- | --- | --- |
| 1 | 3 个 variant 均有 `phaseOverrides` | ✅ |
| 2 | 3 个 override telegraph 都能触发 | ✅ |
| 3 | 3 个 owner coverage 指标 = 3/3 | ✅ |
| 4 | report 同时保留 action trace divergence 和 structural phase divergence | ✅ |
| 5 | `MutationModels.kt` 是唯一 schema owner | ✅ |
| 6 | `DataLoader.parseBossVariants` fail fast | ⚠️ 等价偏差（§2.2.3） |
| 7 | goldenScreenshot + client render snapshot 覆盖 3 variant | ✅ |
| 8 | verifyChanged 覆盖 schema/data/harness/client/report | ✅ |
| 9 | 没新增图片资源 | ✅ |
| 10 | 没新增音频资源 | ✅ |

---

## 3. 偏差清单（按严重度分级）

### 3.1 P1 — runtime 接线归属偏离 schema 边界

- **位置**：`game/src/main/kotlin/com/ktome/game/factory/BossFactory.kt`（应承接但完全不承接）
   vs. `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:4149` `bossEncounterFor`（实际承接）。
- **偏差描述**：见 §2.2.1。
- **偏差幅度**：架构性。schema → encounter 装配层级被搬到了会话层。
- **修复优先级**：P1（必须修，但可拆 PR）。
- **建议修复路径**：见 §2.2.1。倾向方案 1（最小修复 + ECS 组件持久化）。

### 3.2 P1 — `boss.trigger.war_caller_active` 语义降级

- **位置**：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` `bossVariantActiveTriggerIds`。
- **偏差描述**：见 §2.3.1。规范期望"伴生 alive 或 aura active"，实现按"loadout flag 是否存在"。
- **偏差幅度**：玩法体验语义级——影响 grey_crown 的策略空间。
- **修复优先级**：P1（影响玩家可分辨的玩法循环）。
- **建议修复路径**：见 §2.3.1。

### 3.3 P2 — `tools/boss/**` 范围声明不存在

- **位置**：规范 §3.1 vs. 实际 `tools/phase4/**`。
- **偏差幅度**：仅命名 / 文档。
- **修复优先级**：P2。
- **建议修复路径**：见 §2.1.1，倾向修订规范文档而非新建空目录。

### 3.4 P2 — §3.1 测试文件清单与现状不符

- **位置**：见 §2.1.2 表格。
- **偏差幅度**：覆盖率达成，但缺少专门的 fail-fast 单元测试，违反"三层测试职责"分工。
- **修复优先级**：P2。
- **建议修复路径**：见 §2.1.2 修复 1（必做）+ 修复 2（推荐）。

### 3.5 P2 — 人工白盒 action emphasis 证据可强化

- **位置**：`build/whitebox/phase4-v4-pr05/evidence/` + `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr05-boss-variant-phase-language.md`。
- **偏差描述**：见 §2.6.2。3 张 warning 截图为主，molten_glass 步骤表格未明列 `linebreaker / earthshaker` 行动日志条目；其余两 variant 表格中文行动名提及，但截图主体仍是 warning。
- **偏差幅度**：满足规范字面（manual record 已记录"日志区显示 ... 阶段行动反馈"），但与 §6.3 通过标准 #2「action emphasis 玩家可分辨」的玩家视角仍稍弱。
- **修复优先级**：P2。
- **建议修复路径**：补充 3 张 `phase4-v4-pr05-<variant>-action-emphasis.png` 聚焦战斗日志区 + boss 行动选择动画的截图，并在 manual record 表格中各列一条「该 variant 命中后连续 N 回合内 emphasized action 出现频次显著高于其他动作」的实测描述（哪怕 N=3）。

### 3.6 P3 — schema 代码块缺 `Not`（规范本身内部矛盾）

- **位置**：规范 §5.1 schema 代码块。
- **偏差幅度**：规范文档自我不一致，实现遵循 runtime 条款的描述。
- **修复优先级**：P3（仅文档修订）。
- **建议修复路径**：见 §2.2.2。

### 3.7 P3 — `DataLoader.parseBossVariants` fail-fast 拆为后置 validator

- **位置**：`game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`。
- **偏差幅度**：等价。
- **修复优先级**：P3（文档注解 + 评估去重）。
- **建议修复路径**：见 §2.2.3。

---

## 4. 修复与优化建议（按交付优先级）

### 4.1 立即修复（同 PR 或紧跟一次小 PR）

1. **`war_caller_active` companion-alive 检查**（§2.3.1 修复 1）
   - 文件：`game/.../FoundationGameSession.kt` `bossVariantActiveTriggerIds`。
   - 增量：~ 20-30 行 + 1 ECS 字段读取 + 测试 1 条。
   - 风险：低（trigger 失效会让 grey_crown override phase 进入更晚或不进入；这正是策划意图，实测可观测）。
2. **新增 `MutationModelsContractsTest.kt`**（§2.1.2 修复 1）
   - 文件：`game/src/test/kotlin/com/ktome/game/elites/MutationModelsContractsTest.kt`。
   - 增量：~ 150-200 行表驱动 fail-fast 测试。
   - 风险：零（纯新增测试）。
3. **manual record 补 action emphasis 截图**（§3.5）
   - 文件：`build/whitebox/phase4-v4-pr05/evidence/`、`docs/review/phase4/v4-pr/manual-records/phase4-v4-pr05-boss-variant-phase-language.md`。
   - 增量：3 张截图 + sha256 + metadata + manual record 步骤补充。
   - 风险：零（仅证据强化）。

### 4.2 短期修复（下一个 PR-05 follow-up 或 PR-06 前）

4. **`BossFactory` 承接 phaseOverrides 装配**（§2.2.1 修复 1 或 2）
   - 文件：`game/.../factory/BossFactory.kt` + `BossFactoryTest.kt` + 调用方 `FoundationGameSession.bossEncounterFor`。
   - 增量：~ 60-80 行 + 测试 2 条。
   - 风险：中（涉及 ECS 组件挂载，需要确保 save / load 兼容）。建议落 PR 时同步跑 `verifyChanged` + 一次 packaged app 白盒。
5. **新增 `BossVariantDataLoaderTest.kt`**（§2.1.2 修复 2）
   - 文件：`game/src/test/kotlin/com/ktome/game/data/BossVariantDataLoaderTest.kt`。
   - 增量：~ 200-300 行 YAML fixture-driven。
   - 风险：零。

### 4.3 文档修订（独立 docs PR）

6. **§3.1 范围 + §5.1 schema 代码块 + §5.1 第 7 条 fail-fast 措辞**（§2.1.1 / §2.2.2 / §2.2.3）
   - 文件：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md`。
   - 修订点：
     - §3.1 把 `tools/.../boss/**` 替换为 `tools/.../phase4/**`，并把测试清单与现状对齐。
     - §5.1 schema 代码块加上 `data class Not(val child: TriggerExpression)`。
     - §5.1 第 7 条加注解："允许把跨表引用校验放在 `loadSchemaCatalog()` 末尾的 validator，须在任何 boss spawn 前完成。"
   - 风险：零（仅文档）。

---

## 5. 自动化与人工证据状态

| 证据类别 | 状态 | 备注 |
| --- | --- | --- |
| `localeLint / contractLint / assetLint / audioLint` | ✅ | i18n 双语 + telegraph + audio manifest 三链 OK |
| `:core:test`（含 `BossPhaseManagerTest`） | ✅ | AllOf / AnyOf / Not / ≥2 child / one-shot / skipped reason |
| `:game:test`（含 `BossHarnessTest`、`GameContentTest`） | ✅ | bossHarness 大粒度场景 PASS |
| `bossHarness` owner gate | ✅ | 4 个 PR-05 指标全部 = 3/3（owner）+ ≥1 per variant（reportOnly） |
| `goldenScreenshot` | ✅ | `capturePhase4V4Pr05BossVariantWarningSet` 覆盖 3 variant |
| `clientSmoke` | ✅ | `ClientSmokeHarnessTest:488-502` 覆盖 3 variant override warning id |
| `reportPhase4Only / reportPhase4` | ✅ | 同一 producer artifact，aggregator OK |
| `maintainabilityLint / verifyChanged` | ✅ | governance 段落齐备 |
| 人工白盒 packaged app | ⚠️ | `PASS_WITH_AUDIO_CAPTURE_LIMITATION`；§3.5 建议补 action emphasis 聚焦截图 |
| owner baseline | ✅ | `2026-04-16-phase4-boss-phase-identity-owner-baseline.json` 4 条范围 + 注释完整 |

---

## 6. 设计总监结论

PR-05 在 schema、runtime、harness、report、客户端可见性五条主轴上 **完成度 ≥ 90%**：data-level phase override language 已落地，3 个 variant 在 owner gate 与玩家可见性上都拿到了"专属 warning + emphasis 行动"的最小可分辨差异。

**但仍有两条 P1 偏差影响"是否可宣布 PR-05 完成"**：

1. `BossFactory` 没有承接 phase override 装配——架构 owner 错位，且导致 `BossFactoryTest` 无法回归本 PR 的核心装配路径；
2. `war_caller_active` 实际只是「装载位 flag」——把 grey_crown 的 companion 击杀压制策略空间整段去掉，与策划意图相违。

建议：

- **如果时间允许**：在合入主线前完成 §4.1 的 3 条立即修复，保留 §4.2 的 BossFactory 重构作为 follow-up。
- **如果必须现合**：至少先合 §4.1 第 1 条（war_caller 语义修正）+ 第 3 条（manual record 证据强化），之后立即排 §4.2 第 4 条作为 PR-05.1。
- **文档（§4.3）** 在任何时候都可以独立提 docs PR，使规范与实现重新自洽。

`phaseGraphUnchanged=true + phaseGraphUnchangedReason=data_level_override_only + bossVariantStructuralDivergenceNote` 三件齐备已经说明本 PR 的策划立场（不重写 phase graph，仅补 data-level 语言）；无需在 PR-05 内继续扩张。

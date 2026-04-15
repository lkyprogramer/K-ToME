# K-ToME AGENTS Guide

本文件用于约束本仓库后续的人类与 AI 协作开发方式。目标不是增加流程负担，而是把 Phase 2 ~ Phase 5 的长期规范、模块边界、验证门禁与阶段冻结口径固定下来，避免在迭代中重新回到“临时实现可玩、正式合同漂移”的状态。

## 1. 项目定位与当前主线

K-ToME 是一个使用 `Kotlin + libGDX` 开发的类 ToME 回合制 Roguelike 项目。

当前主线不再是“继续在线性堆功能”，而是遵循以下阶段路径：

`Phase 1 可玩基线 -> Phase 2 语义合同 + Tile/i18n 短局 -> Phase 3 深层战斗/职业/长局 -> Phase 4 ProcGen/Loot/Content Pack -> Phase 5 战术 AI/稳定性/发布`

长期规划与执行权威见：

1. [docs/mvp-development-guide.md](docs/mvp-development-guide.md)
2. [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](docs/2026-03-13-phase2-to-phase5-final-roadmap.md)
3. [docs/2026-03-13-core-systems-design-and-phase-supplements.md](docs/2026-03-13-core-systems-design-and-phase-supplements.md)
4. 当前阶段对应的 `docs/phase*/roadmap.md`
5. 当前阶段对应的 verification checklist 与 `P?-W*` / PR 级设计文档

本文件负责固定：

1. 全局协作规范
2. 模块与依赖边界
3. 跨阶段冻结合同
4. 阶段门禁与验证规则

## 2. 权威层级与覆盖关系

后续开发按以下优先级理解文档：

1. [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](docs/2026-03-13-phase2-to-phase5-final-roadmap.md)
   - 权威内容：阶段边界、检查点、工作包编号、进入/退出条件、长期门禁
2. [docs/2026-03-13-core-systems-design-and-phase-supplements.md](docs/2026-03-13-core-systems-design-and-phase-supplements.md)
   - 权威内容：公式、数据结构、系统内部合同、阶段补充冻结决策
3. 当前阶段 roadmap
   - `Phase 2`：[docs/phase2/roadmap.md](docs/phase2/roadmap.md)
   - `Phase 3`：[docs/phase3/roadmap.md](docs/phase3/roadmap.md)
   - `Phase 4`：[docs/phase4/roadmap.md](docs/phase4/roadmap.md)
   - `Phase 5`：[docs/phase5/roadmap.md](docs/phase5/roadmap.md)
   - 权威内容：当前阶段文档索引、依赖拓扑、执行编号与阶段主题
4. 当前阶段 verification checklist
   - `Phase 2`：[docs/phase2/2026-03-13-phase2-verification-checklist.md](docs/phase2/2026-03-13-phase2-verification-checklist.md)
   - `Phase 3`：[docs/phase3/2026-03-13-phase3-verification-checklist.md](docs/phase3/2026-03-13-phase3-verification-checklist.md)
   - `Phase 4`：[docs/phase4/2026-03-13-phase4-verification-checklist.md](docs/phase4/2026-03-13-phase4-verification-checklist.md)
   - `Phase 5`：[docs/phase5/2026-03-13-phase5-regression-checklist.md](docs/phase5/2026-03-13-phase5-regression-checklist.md)
   - 权威内容：自动化命令、白盒步骤、量化阈值、可复现性契约
5. 当前工作包的 `P?-W*` / PR 级设计文档
   - 权威内容：该工作包内部的范围、非目标、拆分方式、实现锚点

覆盖与冲突规则：

1. 旧 `PR01 ~ PR12` 只作历史参考；Phase 2 以后统一以 `P2-W1 ~ P5-W5` 或对应 phase 的工作包编号为准。
2. 下游文档与上游权威冲突时，必须先修上游权威，再回写下游文档；禁止在实现里默默维持第二套长期口径。
3. 若任务已经命中当前阶段的 `P?-W*` 文档，就必须以该工作包的范围与非目标为边界，不得越界偷做后续阶段内容。

## 3. 开发前强制预检

对任何 `moderate / complex` 任务，开始实现前必须完成预检。

凡是会新增、修改或重构 Kotlin 代码，不论任务大小，开始编码前都必须先阅读并遵守 [docs/rule/kotlin.md](docs/rule/kotlin.md)；未完成该项检查，视为预检未完成，不得开始实现。若该规范与本文件、phase 文档或 checklist gate 存在交叉约束，按更严格者执行。

### 3.1 必读文档

至少阅读以下内容：

1. [docs/mvp-development-guide.md](docs/mvp-development-guide.md)
2. [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](docs/2026-03-13-phase2-to-phase5-final-roadmap.md)
3. [docs/2026-03-13-core-systems-design-and-phase-supplements.md](docs/2026-03-13-core-systems-design-and-phase-supplements.md)
4. 如果本次涉及 Kotlin 改动：[docs/rule/kotlin.md](docs/rule/kotlin.md)
5. 如果本次涉及 non-trivial Kotlin 改动、结构重排、review/gate/工具链治理：[docs/rule/ai-change-governance.md](docs/rule/ai-change-governance.md)
6. 当前阶段 roadmap
7. 当前阶段 verification checklist
8. 本次命中的 `P?-W*` / PR 级设计文档
9. 本次会修改到的代码与对应测试

如果任务只是局部 bugfix，可以不重复通读所有 phase 文档，但仍必须确认：

1. 没有破坏模块边界
2. 没有违反当前 phase 冻结口径
3. 没有绕过现有 harness / lint / 白盒门禁
4. 如果涉及 Kotlin 改动，已按 [docs/rule/kotlin.md](docs/rule/kotlin.md) 完成预检与自检
5. 如果涉及 non-trivial Kotlin 改动，已按 [docs/rule/ai-change-governance.md](docs/rule/ai-change-governance.md) 完成 anti-bloat 预检

### 3.2 第一条输出要求

对非 trivial 任务，第一条输出必须包含简短《预检摘要》，至少说明：

1. 当前命中的 phase、checkpoint 与文档
2. 受影响模块及其权威职责
3. 关键合同、风险与非目标
4. 计划执行的自动化命令 / harness / 白盒验证

如果文档缺失、约束冲突、或用户要求与当前阶段冻结目标明显矛盾，必须先指出冲突，不能直接实现。

### 3.3 任务映射规则

1. 任何触及公式、schema、版本、事件、snapshot、AI、world progress、content pack、release gate 的改动，都必须先映射到当前阶段的 `P?-W*`。
2. 如果一个任务跨越多个 checkpoint，必须优先落上游 contract，再落内容和表现。
3. 如果某个工作包结束后主干不可玩，说明切分过大，必须继续拆分。

### 3.4 AI 变更治理

1. 非 trivial Kotlin 改动，提交前必须经过一次 anti-bloat review；治理权威见 [docs/rule/ai-change-governance.md](docs/rule/ai-change-governance.md)。
2. 若改动新增以下任一结构，必须在设计文档、实现说明或 review 中显式说明理由；默认按阻塞项处理：
   - `Boolean` 参数或默认参数矩阵
   - `Helper / Utils / Manager` 一类业务类型
   - compat 分支、临时双路径或第二真源
   - 没有 `debt(<id>)` 与删除条件的临时逻辑
3. repo-specific anti-bloat 规则必须只在仓库文档内定义；不得在 skill、prompt 或 review 模板中复制出第二份长期口径。

## 4. 架构基线

### 4.1 基本原则

1. `core` 是规则真源，必须保持零引擎依赖。
2. `game` 负责内容 schema、注册表、官方内容包与会话装配。
3. `client` 只负责 Tile/UI/输入/音频/Locale bundle/manifest 消费与表现编排。
4. `tools` 负责 lint、smoke、batch、harness、asset pipeline、perf/soak、release 验证。
5. 任何阶段都必须同时满足：可运行、可测试、可度量、可白盒验证。

### 4.2 模块职责

#### `core`

职责：

1. ECS、地图、FOV、移动、战斗、公式、状态、AI、世界推进、ProcGen、掉落、存档 DTO、回放语义、`RenderSnapshot` DTO
2. 纯数据结构、纯函数、确定性算法
3. 可被 JUnit 和固定 seed harness 直接验证的所有核心行为

禁止：

1. 依赖 libGDX、GUI、音频、窗口、纹理句柄或任何渲染框架
2. 输出最终本地化字符串、原始资源路径或表现层缓存
3. 为了方便 UI 在规则层引入第二份显示状态

#### `game`

职责：

1. 内容 schema、注册表、官方内容包、世界内容、职业/怪物/掉落/zone 数据装配
2. 将 `core` 的规则能力组装为具体玩法会话
3. 内容与 contract 校验

禁止：

1. 重新实现本应属于 `core` 的公式、状态、AI、地图或世界推进规则
2. 保存表现层副本作为权威状态
3. 绕过 `core` 直接定义第二套规则语义

#### `client`

职责：

1. 桌面入口、窗口生命周期、输入采集、Tile/HUD/UI、音频、Locale bundle、manifest resolve
2. 把玩家输入转换为对 `game/core` 的调用
3. 把 `RenderSnapshot`、日志 token、manifest 解析为可见表现

禁止：

1. 地图生成、战斗结算、AI 决策、world progress、loot budget 等规则逻辑直接写进 `client`
2. 在 `client` 中维护规则权威状态的独立副本
3. 在 `client` 中生成正式业务文案或资源映射真值

#### `tools`

职责：

1. `localeLint`、`contractLint`、`maintainabilityLint`、`assetLint`、`audioLint`、`manifestLint`
2. `headlessSmoke`、`clientSmoke`、`goldenScreenshot`、`soloClearLab`、`longRunLab`
3. `combatTraceGolden`、`bossHarness`、`mapgenSmoke`、`solvabilityHarness`
4. `lootBalanceLab`、`hiddenContentHarness`、`contentPackHarness`
5. `tacticalAiHarness`、`replayHarness`、`perfSmoke`、`soakRun`、`balanceLab`、`packageRelease`

禁止：

1. 直接侵入运行时规则路径成为新的真源
2. 以“测试方便”为由绕过正式 schema、manifest、snapshot 或 replay 合同
3. 在工具层偷偷维护第二套内容解析或 AI 逻辑

### 4.3 模块依赖规则

只允许以下依赖方向：

1. `root -> core / game / client / tools`
2. `game -> core`
3. `client -> game`
4. `client -> core` 仅允许旧薄壳遗留路径；凡是 Phase 2+ 被修改的正式路径，优先收敛到 `RenderSnapshot` / session boundary
5. `tools -> core / game / client` 仅用于验证、批处理和离线流水线，不得成为运行时行为来源

不允许：

1. `core -> game`
2. `core -> client`
3. `core -> tools`
4. `game -> client`
5. 通过共享可变状态绕过模块边界

## 5. 全局开发合同

### 5.1 产品范围与长期非目标

1. 只做单机单人 run。
2. 不做联机、账号、云存档、排行榜或跨设备同步。
3. 不考虑旧存档兼容性；旧版本存档默认 fail fast，并提示需要新开局。
4. 每个 phase 都必须以真实可玩的纵切收尾，不允许长期主干不可玩。
5. `Phase 4` 只做到内容包与 overlay 级扩展，不引入 Lua runtime、行为树平台、通用脚本宿主或完整 Mod SDK。
6. 除文档显式定义的职业解锁、`ProfileData`、run history 外，不引入数值型 meta progression。
7. 从 `P2-B` 起，Tile 是正式玩家路径；ASCII 只允许作为 debug 或 fallback。

### 5.2 跨阶段冻结合同

以下概念一旦进入主线，就必须维持单一权威，不允许并行维护第二套：

1. `saveContractVersion`、`visualManifestVersion`、`audioManifestVersion`、`styleVersion`、replay schema version
2. `GameEvent`、`LogTokenEvent`、callback registry
3. `RenderSnapshot`、`VisualManifest`、`AudioManifest`
4. `DamageType`、`DamageInstance`、`PowerType`、`ApplicationPolicy`、`CombatResolutionTrace`
5. `ResourcePool`、`ResourceType`、`ResourceAxis`、profession resource contract
6. `StatusEffectDef`、`StatusInstance`、`ActorEffect / AreaEffectEmitter / WorldEffect`
7. `TalentDef V2`、`EffectOp`、`DescriptionModel`、`telegraphRef`
8. `AIProfile`、`BossEncounter`、`BossPhaseDef`、`TelegraphSpec`
9. `WorldProgress`、`Quest`、`GateCondition`、`RouteReward`、`RunSummary`
10. `TerrainTag`、`LootBudget`、`EliteMutationDef`、`HiddenEventDef`、`SecretZoneDef`
11. `ContentPackManifest`、`OverlayEntry`
12. `PerceptionState`、`HateFocus`、`ReplayHeader`、`DeathAnalysis`、`TacticalAIDecisionTrace`

### 5.3 已冻结的基础决策

1. `v1.0.0` 之前继续冻结为四主属性：`STR / DEX / CON / WIL`。
2. 伤害通道固定为六个：`PHYSICAL / FIRE / COLD / LIGHTNING / HOLY / SHADOW`。
3. 能力对抗通道固定为三个：`PHYSICAL / SPELL / MENTAL`。
4. 行动经济统一使用 `1000` 能量口径；默认行动成本必须集中管理，不能散落调用点。
5. `HEALTH` 保持独立生命系统；职业消耗资源统一走通用资源池。
6. 正式内容对象从第一天起就要具备 `id / nameKey / descKey / visualKey / iconKey / audioProfile / schemaVersion / tags` 等稳定字段。
7. 存档、日志、回放只保存 ID、数值、token 与语义事件，不保存最终本地化文案、raw asset path、glyph/color 或 client-only UI 状态。
8. `core` 只输出语义结构，不输出 localized string、tooltip 文案或视觉指令。
9. Talent、AI、content pack 只允许走 typed schema / typed effect op / DSL，不引入运行时脚本解释器。
10. `content pack` 不允许定义新的 `DamageType / PowerType / StatusEffectType / ResourceType`。
11. `content pack / overlay` 必须 namespaced、可 lint、可版本校验、冲突时 fail fast，且只能作用于 content registry，不能覆盖 `core` 规则语义或直接指向官方 raw 资源路径。
12. `ProfileData`、run save、replay persistence 必须显式分层，不得混成单一持久化宿主。
13. 所有性能优化都必须带对照数据；禁止拍脑袋提前优化。

### 5.4 可测试性、确定性与可追溯性

1. 同一 seed + 同一输入序列 = 同一输出。
2. 禁止在规则层使用系统时间、线程 ID、未定义的迭代顺序或其他非确定性来源。
3. 所有排序键必须完全定义；相等时顺序不能留给运行时偶然性。
4. 新增 harness / golden / batch 时，必须同时记录：
   - `buildId`
   - `phaseId`
   - 相关 schema / ruleset / trace version
   - seed
   - locale
   - profession / race / profile
   - zone route 或 route hash
   - content pack ids
5. 验证失败时必须保留对应 trace、hash、输入脚本、截图差异或 batch 摘要，不能只报一句“失败了”。
6. phase 边界发生正式合同变化时，golden 必须显式重录并带新 phase/version 标识；禁止混用旧 phase 产物。
7. 只要新系统涉及随机性、隐藏内容、批量验证或外部内容装载，就必须同步交付可重放的 harness / lab，不能把“人工偶现复现”当作正式验证路径。

## 6. 工作包与实施规则

### 6.1 四条开发线

后续阶段默认按四条 lane 拆任务：

1. `Rules Lane`
   - `core`
   - 规则、公式、状态、AI、世界推进、ProcGen、掉落、存档 DTO
2. `Client Lane`
   - `client`
   - Tile、UI、输入、音频、manifest 消费、golden screenshot
3. `Content Lane`
   - `game`
   - professions、talents、monsters、loot、zones、bosses、content pack 数据
4. `Tools/QA Lane`
   - `tools`
   - lint、smoke、harness、batch、perf/soak、release 验证

### 6.2 工作包设计规则

每个工作包都必须满足：

1. 只引入一个新的核心抽象族；超过一个就继续拆。
2. 同时触碰的生产模块不超过两个；若超过，必须说明为什么无法再拆。
3. 必须同时交付自动化验证和白盒步骤，不接受“先实现，之后补验证”。
4. 必须声明范围、非目标、依赖、版本影响。
5. 结束后主干仍可玩；否则说明切分过大。
6. 任何内容量扩张都必须建立在上游 contract 已冻结的前提下，不能反过来用内容推动 contract 漂移。

### 6.3 Root Alias 规则

从 Phase 2 起，新的 harness / lint / smoke 一旦被引入，就必须尽快暴露 root Gradle 入口，而不是只藏在子模块脚本里。

原因：

1. 统一本地与 CI 门禁
2. 让 phase checklist 可以直接落到命令级 contract
3. 避免“工具已经存在，但无人知道如何跑”

## 7. 阶段性规范

### 7.1 Phase 2

执行权威：

1. [docs/phase2/roadmap.md](docs/phase2/roadmap.md)
2. [docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md](docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md)
3. [docs/phase2/2026-03-13-phase2-verification-checklist.md](docs/phase2/2026-03-13-phase2-verification-checklist.md)
4. `P2-W1 ~ P2-W7` 对应文档

主题：

`Phase 1 可玩 MVP -> 语义合同重建 -> Tile/i18n 正式路径 -> 4 职业短局`

必须冻结：

1. `1000` 能量制、`kotlinx.serialization` 主链、fail-fast 版本纪律
2. `GameEvent / LogTokenEvent / callback registry`
3. `DamageType`、`ResourcePool`、基础状态骨架、`TalentDef V2`
4. `RenderSnapshot`、`VisualManifest`、`AudioManifest`
5. 全部正式对象切到 key 驱动 schema
6. `SoloClearLab v1` 变为正式硬门禁

阶段边界：

1. `P2-B` 至少交付 `2` 职业 + `1` zone + `1` Boss 的正式切片
2. `P2-C` 出口是 `4` 基础职业 + `4` zone + `24` 怪 + `24` 物品的短局闭环
3. 只做短局，不做长局世界分支
4. 不引入 Lua runtime、复杂 ProcGen、完整 content pack 平台

正式门禁：

```bash
./gradlew test
./gradlew :core:test
./gradlew :game:test
./gradlew headlessSmoke
./gradlew clientSmoke
./gradlew preReleaseAcceptance
./gradlew localeLint
./gradlew contractLint
./gradlew goldenScreenshot
./gradlew soloClearLab
./gradlew longRunLab
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

具体量化阈值、固定 seed、白盒步骤和报告留存要求，以 `Phase 2 verification checklist` 为权威。

### 7.2 Phase 3

执行权威：

1. [docs/phase3/roadmap.md](docs/phase3/roadmap.md)
2. [docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md](docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md)
3. [docs/phase3/2026-03-13-phase3-verification-checklist.md](docs/phase3/2026-03-13-phase3-verification-checklist.md)
4. `P3-W1 ~ P3-W6` 对应文档

主题：

`短局可玩 -> 正式战斗公式 -> 职业树/状态/AI DSL -> 4~6 小时长局`

必须冻结：

1. `CombatPipeline`、`ApplicationPolicy`、`CombatResolutionTrace` 与分层 golden corpus
2. 正式状态生命周期、effect carrier、净化与 tick 合同
3. `TalentDef V2`、`EffectOp`、`DescriptionModel`、`TalentAllocationDraft`
4. `AIProfile DSL`、`BossEncounter`、`BossPhaseDef`、`TelegraphSpec`
5. 职业正式化、多资源轴、种族、铭文、`ClassAvailabilityResolver`
6. `WorldProgress / Quest / Gate / RouteReward / RunSummary / headlessTurnEquivalent`

阶段边界：

1. 仍是纯 run-based Roguelike，不引入数值型 meta progression
2. 只允许显式文档定义的职业解锁、profile 与历史 run summary 跨 run 保留
3. 不得引入第二套 telegraph、第二套 AI 行为树或第二套资源解释口径

正式门禁：

```bash
./gradlew test
./gradlew :core:test
./gradlew combatTraceGolden
./gradlew bossHarness
./gradlew soloClearLab
./gradlew longRunLab
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

具体量化阈值、golden 元数据、fixed-seed corpus、`headlessTurnEquivalent` 口径和白盒步骤，以 `Phase 3 verification checklist` 为权威。

### 7.3 Phase 4

执行权威：

1. [docs/phase4/roadmap.md](docs/phase4/roadmap.md)
2. [docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md](docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md)
3. [docs/phase4/2026-03-13-phase4-verification-checklist.md](docs/phase4/2026-03-13-phase4-verification-checklist.md)

主题：

`长局成立 -> ProcGen 深化 -> Loot 生态深化 -> Hidden Content 成立 -> Content Pack Overlay`

必须冻结：

1. `MapgenPipeline`、`BiomeFamilyDef`、`TerrainTag`、`SolvabilityGraph`
2. `LootBudget`、`RarityTier`、`EliteMutationDef`、`HiddenEventDef`、`SecretZoneDef`
3. `ContentPackManifest`、`OverlayEntry`、pack schema lint / harness

阶段边界：

1. 不新增职业、主属性、伤害通道、资源类型或 `core` 规则语义
2. 不引入 Lua / GraalJS / WASM / Python 等通用脚本 runtime
3. hidden reward 不得绕过 `LootBudget`
4. secret zone 不得绕过 `SolvabilityGraph`
5. content pack 不得定义新的核心语义枚举
6. 元素亲和度只允许作为 isolated lab，不进入 Phase 4 主线门禁

正式门禁：

```bash
./gradlew test
./gradlew :core:test
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew lootBalanceLab
./gradlew hiddenContentHarness
./gradlew contentPackHarness
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

seed 覆盖、可达率、分布误差、hidden content 触发率、pack 回退行为和白盒步骤，以 `Phase 4 verification checklist` 为权威。

### 7.4 Phase 5

执行权威：

1. [docs/phase5/roadmap.md](docs/phase5/roadmap.md)
2. [docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md](docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md)
3. [docs/phase5/2026-03-13-phase5-regression-checklist.md](docs/phase5/2026-03-13-phase5-regression-checklist.md)

主题：

`系统稳定 -> 战术 AI -> perf/soak/replay -> QA 清盘 -> v1.0.0`

必须冻结：

1. `TacticalScoringLayer`、`PerceptionState`、`HateFocus`、`TacticalAIDecisionTrace`
2. `ReplayHeader / ReplayFrame / RunHistoryEntry / DeathAnalysis / SoakReport`
3. perf/soak 预算、采样方式、报告格式
4. Localization QA、Accessibility QA、BalanceLab、package 验收口径

阶段边界：

1. Tactical AI 只能建立在 `Phase 3/4` 的 `AIProfile DSL`、`BossPhaseDef`、action catalog 和 perception contract 之上
2. 不引入新的规则解释器、第二套怪物/Boss 行为树或通用 planner
3. 不再引入大新玩法系统
4. 不因为“看起来更聪明”而牺牲可解释性、确定性与回归定位能力

正式门禁：

```bash
./gradlew test
./gradlew :core:test
./gradlew tacticalAiHarness
./gradlew replayHarness
./gradlew perfSmoke
./gradlew soakRun
./gradlew localizationQa
./gradlew accessibilityQa
./gradlew balanceLab
./gradlew contentPackHarness
./gradlew packageRelease
```

AI 场景矩阵、replay 哈希一致性、perf/soak 预算、Localization/Accessibility QA 和封版报告字段，以 `Phase 5 regression checklist` 为权威。

## 8. 测试与验证规范

### 8.1 总原则

1. 所有核心规则优先以单元测试、固定 seed harness 和 golden 自证，而不是靠手动跑客户端观察。
2. 新功能不是“先写代码再补测试”，而是规则、内容和验证入口同步交付。
3. 对 `core` 的非平凡改动，没有对应测试视为未完成。

### 8.2 长期回归套件

后续阶段统一保留以下长期回归：

1. `GoldenSeed`
2. `LocaleLint + LocaleScreens`
3. `ContractLint`
4. `SoloClearLab`
5. `HeadlessSmoke`
6. `BossHarness`
7. `SaveCurrentVersion`
8. `Perf/Soak`

若某个 suite 在当前阶段被正式引入，就不能再退化为“可选工具”。

### 8.3 `core` / `game` / `client` / `tools` 的验证要求

#### `core`

1. 任何规则、公式、状态、AI、world progress、procgen、loot 改动都应有固定 seed / 固定输入 / 固定输出断言。
2. 对随机算法，必须断言确定性与关键不变量，而不是只断言“看起来像”。

#### `game`

1. 验证装配结果，而不是重复测试 `core` 算法。
2. 重点检查 schema、registry、cross-reference、manifest key、pack overlay 是否正确。

#### `client`

1. 当前阶段不追求复杂 GUI 自动化测试，但必须保证编译通过。
2. 涉及渲染、输入、HUD、localization、accessibility、package 的改动，必须保留白盒步骤。
3. 能抽成纯逻辑的输入映射、布局、计算，应优先补局部单测。

#### `tools`

1. harness/lint 不只是脚本样例，而是正式门禁的一部分。
2. batch/harness 需要输出可追溯报告，而不是只在控制台打印结论。
3. 只要改动命中 `trace schema`、golden corpus、snapshot contract、locale key、manifest key、report schema、lint/harness 断言中的任一项，就不能只跑 `:core:test` / `:game:test`；必须补跑对应 `:tools:test` 或其 owner gate。
4. 对 `tools` 侧失败，优先把问题归类为“合同/基线未同步”还是“运行时逻辑回归”，不要默认认为是 CI 环境噪声。
5. 对 non-trivial Kotlin 改动，若 `maintainabilityLint` 或 anti-bloat review 命中 `option-sprawl / helper-sprawl / second-authority / temp-path-without-expiry`，必须先解释 owner 与 contract 选择，再决定是否保留实现。

### 8.4 构建与基础验证

工具链环境固定规则：

1. 本仓库的 Java / Kotlin 工具链以仓库根目录 `.sdkmanrc` 为准；当前固定为 `java=21.0.10-tem`、`kotlin=2.2.21`。
2. 运行任何 `./gradlew`、`java`、`kotlinc` 之前，必须先执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

3. 若 `JAVA_HOME`、`java -version` 或 `which java` 与 `.sdkmanrc` 不一致，必须先修正环境，再分析构建或测试失败；禁止把错误 JDK 下的失败结果当成代码问题。
4. 不要依赖系统 PATH 上的默认 JDK，也不要在仓库内混用 Homebrew JDK、系统 JDK 与 SDKMAN JDK。
5. 所有 `./gradlew` 命令必须串行执行；同一时刻只允许一个 Gradle 进程运行，尤其是 `test`、各类 harness、lint、smoke、`clientSmoke`、`whiteBox*`、`phase4Report` 等验证任务，严禁并发跑多个 `./gradlew` 命令。
6. 若需要执行多个 Gradle 任务，只允许两种方式：
   - 单次 `./gradlew taskA taskB ...` 串行完成；
   - 前一条 `./gradlew` 完全退出后，再启动下一条。
7. 禁止在多个终端、后台作业或 agent 并行执行 Gradle；发现已有 Gradle 进程仍在运行时，必须等待其结束，或先明确中止，再启动新的 Gradle 命令。
8. 如果后续需要调整 `.sdkmanrc`，必须与对应的构建脚本、`AGENTS.md` 和相关验证记录一起提交，避免仓库工具链口径漂移。

提交前至少执行与本次改动匹配的命令。基础命令为：

```bash
./gradlew test
./gradlew :core:test
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
./gradlew check
```

如果本次改动涉及 non-trivial Kotlin 结构变更、review/gate/verification wiring 或 anti-bloat 治理能力，还必须额外执行：

```bash
./gradlew maintainabilityLint
```

如果当前机器存在 Maven / TLS 拉取问题，可先执行：

```bash
./scripts/bootstrap-deps.sh
```

如果本次改动涉及以下任一内容，必须额外执行一次 bootstrap 离线烟雾验证：

1. `gradle.properties`
2. `build.gradle.kts` / `settings.gradle.kts`
3. `scripts/bootstrap-deps.sh`
4. 任何依赖版本调整

验证命令：

```bash
./scripts/verify-bootstrap.sh
```

### 8.4.1 CI 覆盖经验回写

以下规则来自 `Phase 4 OPT PR-03` 的真实 CI 回归教训，后续按硬约束执行：

1. 修改 `CombatResolutionTrace`、`RenderSnapshot`、golden payload、harness JSON 字段、schema version 或 manifest 结构时，必须补跑对应的 golden / harness suite，并在同一提交中同步 canonical golden 或基线资源；禁止只改运行时代码不改 golden。
2. 新增或修改代码引用的 i18n key 时，必须补跑 locale lint；`game:test`、`clientSmoke` 或功能链路通过，不能替代 `LocaleLintTest` / `localeLint`。
3. 只要改动落在 `tools` 所拥有的断言面上，就要把 `:tools:test` 的相关子集纳入本地最小必跑集合；不要把 `tools` suite 留给 CI 首次发现。
4. 提交前的“最小必要验证”必须覆盖**断言所有者**，而不是只覆盖调用方。例：运行时改动如果会改变 golden 语料或 lint 结果，owner 是 `tools`，不是 `core/game`。
5. 遇到 GitHub Actions 失败时，先确认失败 run 对应的 `headSha` 与当前提交是否一致，再修问题；旧 run 的失败可能已经被新提交覆盖，但具体 root cause 仍应回收为仓库规则。

### 8.5 白盒验证与汇报

1. 涉及 `client`、渲染、输入、交互、Tile、Boss telegraph、content pack、package 的改动，都必须保留明确白盒步骤。
2. 基础启动命令通常为：

```bash
./gradlew :client:run
```

3. `Phase 5` 涉及安装包时，还必须补安装包启动与验收步骤。
4. 完成任务后必须真实说明：
   - 实际运行了哪些命令
   - 哪些命令未运行以及原因
   - 白盒验证是否实际执行
   - 仍然存在的风险或未覆盖点
5. 禁止把“理论上应该通过”当作已经验证。

### 8.6 Git 与 GitHub 操作规范

1. 本地已经安装 `gh` CLI 后，所有 Git / GitHub 相关操作默认优先使用 `gh`，尤其是 repository、branch、PR、issue、review、remote、workflow / check 相关动作。
2. 只有在 `gh` 没有等价能力、无法满足精确代码阅读需求、或明确属于纯本地 plumbing / working-tree 检查时，才回退使用 `git`。
3. 对代码审查、改动阅读与行级 patch 判断，允许直接使用 `git diff`、`git show`、`git status` 等原生命令；但只要进入 GitHub 协作语义，优先回到 `gh`。
4. 需要创建、查看、评论、检查、关闭或合并 PR / issue 时，默认先选 `gh pr *`、`gh issue *`、`gh repo *`、`gh run *`，不要先写一套 `git + 浏览器 + 手工链接` 流程。
5. 该约束只改变工具优先级，不放宽任何既有高风险限制；涉及重写历史、强推、破坏性 checkout/reset 等操作，仍按本文件其他条款执行。

## 9. 文档同步与高风险红线

### 9.1 文档同步

1. 如果变更影响架构边界、模块职责、系统合同、阶段出口、构建方式、测试入口或发布门禁，必须同步更新相关文档。
2. 如果 `AGENTS.md` 发生变化，应顺手评估 `docs/mvp-development-guide.md`、phase roadmap、verification checklist、相关 `P?-W*` 文档是否需要同步。
3. 如果新增目录承载明确职责，建议补简短 README，防止结构语义只存在于代码里。

### 9.2 高风险变更红线

以下变更不能静默做掉，必须先明确影响：

1. 存档格式、replay schema、profile schema 变化
2. 公共模型、跨模块契约、manifest/schema version 变化
3. `DamageType / PowerType / StatusEffectType / ResourceType` 等核心枚举变化
4. 把规则从 `core` 挪到 `game/client/tools`
5. 引入引擎类型进入 `core`
6. 删除 coverage gate、降低 harness 门禁、绕过白盒验证
7. 引入 Lua/runtime script host/第二套 AI 系统/第二套 telegraph 权威
8. 修改 content pack 边界，使其越权定义核心规则语义
9. 用“临时逻辑”污染长期 contract 或阶段出口

## 10. DoD（完成定义）

一次合格提交，至少应满足：

1. 已映射到正确的 phase/checkpoint，并阅读对应权威文档
2. 模块边界未被破坏
3. 命中的全局冻结合同未被绕开或复制
4. 新规则逻辑已有测试或固定 seed harness 覆盖
5. 对应自动化命令已运行并报告结果
6. 涉及 `client` 或 package 的改动已有白盒验证步骤
7. 文档、schema、manifest、lint/harness 说明已同步
8. 主干在当前 checkpoint 结束后仍保持可玩
9. 没有把当前阶段非目标偷偷带入主干实现
10. 非 trivial Kotlin 改动已完成 anti-bloat review，且新增 option/helper/compat path/second-authority 有显式理由或已被消除

## 11. 一句话原则

当你不确定代码该放在哪一层时，先问自己一句：

“这段代码是在定义游戏规则，还是在展示、装配、验证这些规则？”

如果答案是规则，默认放进 `core`；如果答案是内容装配，默认放进 `game`；如果答案是展示或交互，默认留在 `client`；如果答案是批量验证、流水线或报告，默认放进 `tools`。

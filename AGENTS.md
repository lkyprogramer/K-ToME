# K-ToME AGENTS Guide

本文件是 K-ToME 仓库级协作入口合同。

它只保留跨任务、跨阶段都稳定成立的规则：文档入口、模块边界、跨阶段冻结合同、全局验证纪律和高风险红线。当前 phase 的主题、阻塞项、量化门禁、完整命令清单与白盒步骤，统一以下游权威文档为准，不在本文件重复维护第二份真相。

## 1. Scope & Bootstrap

### 1.1 项目定位

K-ToME 是一个使用 `Kotlin + libGDX` 开发的类 ToME 回合制 Roguelike 项目。

主线阶段路径固定为：

`Phase 1 可玩基线 -> Phase 2 语义合同 + Tile/i18n 短局 -> Phase 3 深层战斗/职业/长局 -> Phase 4 ProcGen/Loot/Content Pack -> Phase 5 战术 AI/稳定性/发布`

本文件负责固定：

1. 仓库级协作规则
2. 模块职责与依赖边界
3. 跨阶段稳定合同
4. 全局验证纪律
5. 高风险变更红线

本文件不负责重复维护：

1. 当前 phase 的详细主题与出口定义
2. phase-specific 的完整 Gradle 命令块
3. checklist 中的量化阈值、固定 seed、白盒步骤
4. `docs/rule/*.md` 中已经独立定义的细粒度规则

### 1.2 权威文档顺序

后续开发按以下顺序理解文档：

1. [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](docs/2026-03-13-phase2-to-phase5-final-roadmap.md)
   - 阶段边界、检查点、工作包编号、进入/退出条件、长期门禁
2. [docs/2026-03-13-core-systems-design-and-phase-supplements.md](docs/2026-03-13-core-systems-design-and-phase-supplements.md)
   - 公式、数据结构、系统内部合同、阶段补充冻结决策
3. [docs/2026-04-04-unified-white-box-verification-framework.md](docs/2026-04-04-unified-white-box-verification-framework.md)
   - 统一白盒验证框架、artifact/report 合同、自动化与人工一致性策略
4. [docs/INDEX.md](docs/INDEX.md)
   - 文档索引、phase 入口、常见查找路径
5. 当前 phase 的 `roadmap.md`、主文档、verification checklist、`P?-W*` 文档
   - 当前阶段的执行范围、非目标、门禁与验收真相
6. [docs/rule/kotlin.md](docs/rule/kotlin.md)
   - Kotlin 改动的代码质量与边界纪律
7. [docs/rule/ai-change-governance.md](docs/rule/ai-change-governance.md)
   - non-trivial Kotlin 改动的 anti-bloat 治理、review taxonomy 与 gate 口径
8. [docs/mvp-development-guide.md](docs/mvp-development-guide.md)
   - 产品背景与历史 MVP 上下文；不覆盖上述更高优先级文档

固定覆盖规则：

1. 旧 `PR01 ~ PR12` 只作历史参考；执行编号统一使用 `P2-W1 ~ P5-W5`
2. 下游文档与上游权威冲突时，先修上游权威，再回写下游文档
3. 不允许在实现、prompt、skill 或 checklist 外再复制一套平行长期口径

### 1.3 非 trivial 任务预检

对任何 `moderate / complex` 任务，开始实现前必须完成预检。

至少确认以下事项：

1. 当前命中的 phase、checkpoint、`P?-W*` 或 PR 级设计文档
2. 受影响模块及其 owner
3. 是否触碰稳定合同、phase gate、schema/version/event/snapshot/AI/world progress/content pack/release gate
4. 需要补哪些自动化验证、owner gate 与白盒步骤
5. 是否会引入新的边界泄漏、第二真源或验证绕行

读取要求：

1. 任何 Kotlin 新增、修改、重构，开始编码前必须先读 [docs/rule/kotlin.md](docs/rule/kotlin.md)
2. 任何 non-trivial Kotlin 改动、结构重排、review/gate/工具链治理，还必须读 [docs/rule/ai-change-governance.md](docs/rule/ai-change-governance.md)
3. 任何触及公式、schema、版本、事件、snapshot、AI、world progress、content pack、release gate 的改动，都必须先映射到当前 phase 的 `P?-W*`
4. 任何改动都必须读到会修改的代码与对应测试，而不是只看文档

如果任务只是局部 bugfix，可以不重复通读所有 phase 文档，但仍必须确认：

1. 没有破坏模块边界
2. 没有违反当前 phase 冻结口径
3. 没有绕过现有 harness、lint 或白盒门禁
4. Kotlin 相关预检已经按触发条件完成

### 1.4 非 trivial 任务的第一条输出

对非 trivial 任务，第一条输出必须包含简短《预检摘要》，至少说明：

1. 当前命中的 phase、checkpoint 与文档
2. 受影响模块及其权威职责
3. 关键合同、风险与非目标
4. 计划执行的自动化命令、owner gate 或白盒验证

如果文档缺失、约束冲突、或用户要求与当前 phase 冻结目标明显矛盾，必须先指出冲突，不能直接实现。

补充规则：

1. 如果任务跨越多个 checkpoint，优先落上游 contract，再落内容与表现
2. 如果一个工作包结束后主干不可玩，说明切分过大，必须继续拆分
3. non-trivial Kotlin 改动提交前必须经过一次 anti-bloat review；具体 taxonomy 与阻塞规则以 [docs/rule/ai-change-governance.md](docs/rule/ai-change-governance.md) 为准

## 2. Architecture Baseline

### 2.1 基本原则

1. `core` 是规则真源，必须保持零引擎依赖
2. `game` 负责内容 schema、注册表、官方内容包与会话装配
3. `client` 只负责 Tile、UI、输入、音频、Locale bundle、manifest 消费与表现编排
4. `tools` 负责 lint、smoke、harness、batch、perf/soak、release 验证
5. 任何阶段都必须同时满足：可运行、可测试、可度量、可白盒验证
6. 不允许为局部便利引入第二真源、第二套规则路径或跨层业务泄漏

### 2.2 模块职责

| 模块 | 负责 | 禁止 |
| --- | --- | --- |
| `core` | ECS、地图、FOV、移动、战斗、公式、状态、AI、世界推进、ProcGen、掉落、存档 DTO、回放语义、`RenderSnapshot` DTO；纯数据结构、纯函数、确定性算法；可被 JUnit 和固定 seed harness 直接验证的核心行为 | 依赖 libGDX / GUI / 音频 / 窗口 / 纹理句柄；输出 localized string、raw asset path 或 client-only 状态；为了 UI 便利引入第二份显示状态 |
| `game` | 内容 schema、注册表、官方内容包、世界内容、职业 / 怪物 / 掉落 / zone 数据装配；把 `core` 规则能力组装成具体玩法会话；内容与 contract 校验 | 重新实现 `core` 已拥有的公式、状态、AI、地图或世界推进规则；保存表现层副本作为权威状态；绕过 `core` 直接定义第二套规则语义 |
| `client` | 桌面入口、窗口生命周期、输入采集、Tile / HUD / UI、音频、Locale bundle、manifest resolve；把玩家输入转换成对 `game/core` 的调用；把 `RenderSnapshot`、日志 token、manifest 解析成可见表现 | 地图生成、战斗结算、AI 决策、world progress、loot budget 等规则逻辑直接写进 `client`；维护规则权威状态的独立副本；生成正式业务文案或资源映射真值 |
| `tools` | `localeLint`、`contractLint`、`maintainabilityLint`、`assetLint`、`audioLint`、`manifestLint`、各类 smoke、harness、batch、perf/soak、release 验证 | 侵入运行时规则路径成为真源；以“测试方便”为由绕过正式 schema、manifest、snapshot、replay 合同；偷偷维护第二套内容解析或 AI 逻辑 |

### 2.3 模块依赖规则

只允许以下依赖方向：

1. `root -> core / game / client / tools`
2. `game -> core`
3. `client -> game`
4. `client -> core` 仅允许旧薄壳遗留路径；凡是 Phase 2+ 被修改的正式路径，优先收敛到 `RenderSnapshot` / session boundary
5. `tools -> core / game / client` 仅用于验证、批处理和离线流水线，不得成为运行时行为来源

明确不允许：

1. `core -> game`
2. `core -> client`
3. `core -> tools`
4. `game -> client`
5. 通过共享可变状态绕过模块边界

## 3. Stable Contracts & Work Package Rules

### 3.1 产品范围与长期非目标

1. 只做单机单人 run
2. 不做联机、账号、云存档、排行榜或跨设备同步
3. 不考虑旧存档兼容性；旧版本存档默认 fail fast，并提示需要新开局
4. 每个 phase 都必须以真实可玩的纵切收尾，不允许长期主干不可玩
5. `Phase 4` 只做到 content pack 与 overlay 级扩展，不引入 Lua runtime、行为树平台、通用脚本宿主或完整 Mod SDK
6. 除文档显式定义的职业解锁、`ProfileData`、run history 外，不引入数值型 meta progression
7. 从 `P2-B` 起，Tile 是正式玩家路径；ASCII 只允许作为 debug 或 fallback

### 3.2 跨阶段冻结合同

以下概念一旦进入主线，就必须保持单一权威，不允许并行维护第二套：

1. 版本与持久化：`saveContractVersion`、`visualManifestVersion`、`audioManifestVersion`、`styleVersion`、replay schema version
2. 事件与表现桥：`GameEvent`、`LogTokenEvent`、callback registry、`RenderSnapshot`、`VisualManifest`、`AudioManifest`
3. 战斗与资源：`DamageType`、`DamageInstance`、`PowerType`、`ApplicationPolicy`、`CombatResolutionTrace`、`ResourcePool`、`ResourceType`、`ResourceAxis`
4. 状态与天赋：`StatusEffectDef`、`StatusInstance`、`ActorEffect / AreaEffectEmitter / WorldEffect`、`TalentDef V2`、`EffectOp`、`DescriptionModel`、`telegraphRef`
5. AI 与长局：`AIProfile`、`BossEncounter`、`BossPhaseDef`、`TelegraphSpec`、`WorldProgress`、`Quest`、`GateCondition`、`RouteReward`、`RunSummary`
6. ProcGen / Loot / Pack：`TerrainTag`、`LootBudget`、`EliteMutationDef`、`HiddenEventDef`、`SecretZoneDef`、`ContentPackManifest`、`OverlayEntry`
7. 战术 AI / 回放：`PerceptionState`、`HateFocus`、`ReplayHeader`、`DeathAnalysis`、`TacticalAIDecisionTrace`

### 3.3 已冻结的基础决策

1. `v1.0.0` 之前继续冻结为四主属性：`STR / DEX / CON / WIL`
2. 伤害通道固定为六个：`PHYSICAL / FIRE / COLD / LIGHTNING / HOLY / SHADOW`
3. 能力对抗通道固定为三个：`PHYSICAL / SPELL / MENTAL`
4. 行动经济统一使用 `1000` 能量口径；默认行动成本必须集中管理，不能散落调用点
5. `HEALTH` 保持独立生命系统；职业消耗资源统一走通用资源池
6. 正式内容对象必须具备稳定 key 字段：`id / nameKey / descKey / visualKey / iconKey / audioProfile / schemaVersion / tags`
7. 存档、日志、回放只保存 ID、数值、token 与语义事件，不保存最终本地化文案、raw asset path、glyph/color 或 client-only UI 状态
8. `core` 只输出语义结构，不输出 localized string、tooltip 文案或视觉指令
9. Talent、AI、content pack 只允许走 typed schema / typed effect op / DSL，不引入运行时脚本解释器
10. `content pack` 不允许定义新的 `DamageType / PowerType / StatusEffectType / ResourceType`
11. `content pack / overlay` 必须 namespaced、可 lint、可版本校验、冲突时 fail fast，且只能作用于 content registry，不能覆盖 `core` 规则语义或直接指向官方 raw 资源路径
12. `ProfileData`、run save、replay persistence 必须显式分层，不得混成单一持久化宿主
13. 所有性能优化都必须带对照数据；禁止拍脑袋提前优化

### 3.4 工作包设计规则

每个工作包都必须满足：

1. 只引入一个新的核心抽象族；超过一个就继续拆
2. 同时触碰的生产模块不超过两个；若超过，必须说明为什么无法再拆
3. 自动化验证和白盒步骤同步交付，不接受“先实现，之后补验证”
4. 明确声明范围、非目标、依赖、版本影响
5. 结束后主干仍可玩；否则说明切分过大
6. 内容量扩张必须建立在上游 contract 已冻结的前提下，不能反过来用内容推动 contract 漂移
7. 新的 harness / lint / smoke 一旦引入，就要尽快暴露 root Gradle 入口，避免只藏在子模块脚本里

## 4. Phase Routing

本文件不再重复维护 phase-specific 主题、边界、冻结项与命令清单。当前 phase 的执行真相，统一从下表入口进入：

| Phase | 入口文档 |
| --- | --- |
| `Phase 2` | [docs/phase2/roadmap.md](docs/phase2/roadmap.md) + [docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md](docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md) + [docs/phase2/2026-03-13-phase2-verification-checklist.md](docs/phase2/2026-03-13-phase2-verification-checklist.md) + `P2-W1 ~ P2-W7` |
| `Phase 3` | [docs/phase3/roadmap.md](docs/phase3/roadmap.md) + [docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md](docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md) + [docs/phase3/2026-03-13-phase3-verification-checklist.md](docs/phase3/2026-03-13-phase3-verification-checklist.md) + `P3-W1 ~ P3-W6` |
| `Phase 4` | [docs/phase4/roadmap.md](docs/phase4/roadmap.md) + [docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md](docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md) + [docs/phase4/2026-03-13-phase4-verification-checklist.md](docs/phase4/2026-03-13-phase4-verification-checklist.md) + `P4-W1 ~ P4-W5` |
| `Phase 5` | [docs/phase5/roadmap.md](docs/phase5/roadmap.md) + [docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md](docs/phase5/2026-03-13-phase5-tactical-ai-stability-and-release.md) + [docs/phase5/2026-03-13-phase5-regression-checklist.md](docs/phase5/2026-03-13-phase5-regression-checklist.md) + `P5-W1 ~ P5-W5` |

补充规则：

1. 当前 checkpoint 真相以下游 phase roadmap 和 checklist 为准，不在 `AGENTS.md` 写死
2. 任何具体 gate、固定 seed、量化阈值、报告字段、白盒路径，统一回到当前 phase checklist 查
3. 任何当前 sprint/PR 拆分，以命中的 `P?-W*` 文档为边界

## 5. Global Verification Discipline

### 5.1 总原则

1. 所有核心规则优先用单元测试、固定 seed harness 和 golden 自证，而不是靠手动跑客户端观察
2. 新功能不是“先写代码再补测试”，而是规则、内容和验证入口同步交付
3. `core` 的非平凡改动，没有对应测试视为未完成
4. 同一 seed + 同一输入序列 = 同一输出
5. 禁止在规则层使用系统时间、线程 ID、未定义迭代顺序或其他非确定性来源
6. 任何 phase 合同变化导致 golden / harness / report schema 漂移时，必须在同一提交里同步更新其 owner 基线
7. `tools` 中任何用于验证 runtime selector、reward legality、owner metric 的派生逻辑，都必须复用 `game` 侧已冻结 authority 或共享 helper；禁止再手写一套 `sourceTier / professionSuitability / weight / forbiddenBaseIds` 等平行规则
8. canonical owner evidence 一旦缺字段或缺 artifact，必须 fail fast；禁止用 default-success fallback、伪造 `1.0` 覆盖率或空数组占位把 report/materialization 继续跑绿

长期保留的回归套件：

1. `GoldenSeed`
2. `LocaleLint + LocaleScreens`
3. `ContractLint`
4. `SoloClearLab`
5. `HeadlessSmoke`
6. `BossHarness`
7. `SaveCurrentVersion`
8. `Perf/Soak`

一旦某个 suite 在当前 phase 被正式引入，就不能再退化成“可选工具”。

### 5.2 环境与执行纪律

工具链环境固定规则：

1. 本仓库 Java / Kotlin 工具链以仓库根目录 `.sdkmanrc` 为准；运行任何 `./gradlew`、`java`、`kotlinc` 之前必须先执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

2. 如果 `JAVA_HOME`、`java -version` 或 `which java` 与 `.sdkmanrc` 不一致，必须先修正环境，再分析构建或测试失败
3. 不要依赖系统 PATH 上的默认 JDK，也不要在仓库内混用 Homebrew JDK、系统 JDK 与 SDKMAN JDK
4. 所有 `./gradlew` 命令必须串行执行；同一时刻只允许一个 Gradle 进程运行
5. 若需要执行多个 Gradle 任务，只允许单次 `./gradlew taskA taskB ...` 串行完成，或等待前一条彻底结束后再启动下一条
6. 禁止在多个终端、后台作业或 agent 并行执行 Gradle
7. phase-specific 的完整命令清单统一查当前 phase checklist；`AGENTS.md` 不再复制完整命令块
8. 命中 owner gate 时，必须补跑 owner suite，而不是只跑调用方测试；尤其是 `tools` 所拥有的 golden、manifest、locale、snapshot、report、lint / harness 断言面
9. non-trivial Kotlin 结构变更、review/gate/verification wiring 或 anti-bloat 治理改动，必须补跑 `./gradlew maintainabilityLint`
10. 只要改动涉及 `gradle.properties`、`build.gradle.kts`、`settings.gradle.kts`、`scripts/bootstrap-deps.sh` 或依赖版本调整，必须补跑 `./scripts/verify-bootstrap.sh`
11. 如果当前机器存在 Maven / TLS 拉取问题，可先执行 `./scripts/bootstrap-deps.sh`
12. 共享 PR CI 的默认 preflight 是 `./gradlew verifyChanged`；新增 verification/report/gate/governance 接线时，优先复用既有 `VerifyChangedPlanGate` / impact routing，不要在 workflow 或脚本里再造第二套变更判定逻辑
13. 共享 nightly automation 的默认入口是 `./gradlew nightlyGovernanceGate`；需要增加 governance、freshness 或 aggregate smoke 时，优先扩这个 root task，而不是再手写一份并行 nightly inventory
14. `com.ktome.build.testperf` 是 leaf-task、local-first 的 task perf monitor：plain `Test` 默认不进入监控面，只有显式 opt-in 才进入 baseline；CI 只允许保留 report-only summary，不得把 `.gradle/test-perf/` 的 lane、baseline 或 report-only 目录当成 unified verification registry 或 `reportPhase5` 的权威输入

### 5.3 白盒验证与结果汇报

1. 涉及 `client`、渲染、输入、交互、Tile、Boss telegraph、content pack、package 的改动，都必须保留明确白盒步骤
2. 涉及安装包的改动，还必须补安装包启动与验收步骤
3. 验证失败时必须保留对应 trace、hash、输入脚本、截图差异或 batch 摘要，不能只报一句“失败了”
4. 完成任务后必须真实说明：
   - 实际运行了哪些命令
   - 哪些命令未运行以及原因
   - 白盒验证是否实际执行
   - 仍然存在的风险或未覆盖点
5. 禁止把“理论上应该通过”当作已经验证

### 5.4 Git 与 GitHub 操作优先级

1. 本地已安装 `gh` CLI 后，涉及 repository、branch、PR、issue、review、workflow / check 等 GitHub 协作语义时，默认优先使用 `gh`
2. 只有在 `gh` 没有等价能力、无法满足精确代码阅读需求、或明确属于纯本地 plumbing / working-tree 检查时，才回退使用 `git`
3. 对代码审查、改动阅读与行级 patch 判断，允许直接使用 `git diff`、`git show`、`git status` 等原生命令
4. 该优先级规则不放宽任何高风险 Git 限制；涉及重写历史、强推、破坏性 checkout/reset 等操作，仍按更严格规则执行

## 6. Red Lines & Completion

### 6.1 文档同步

如果变更影响以下任一内容，必须同步更新对应文档：

1. 架构边界、模块职责、系统合同
2. phase 出口、checkpoint、工作包范围或执行编号
3. 构建方式、测试入口、release gate
4. schema、manifest、report 或 lint / harness 合同

补充要求：

1. 如果 `AGENTS.md` 发生变化，应同时评估 [docs/INDEX.md](docs/INDEX.md)、phase roadmap、verification checklist、相关 `P?-W*` 文档是否需要同步
2. 如果新增目录承载明确职责，建议补简短 README，避免结构语义只存在于代码里

### 6.2 高风险变更红线

以下变更不能静默做掉，必须先明确影响：

1. 存档格式、replay schema、profile schema 变化
2. 公共模型、跨模块契约、manifest/schema version 变化
3. `DamageType / PowerType / StatusEffectType / ResourceType` 等核心枚举变化
4. 把规则从 `core` 挪到 `game/client/tools`
5. 引入引擎类型进入 `core`
6. 删除 coverage gate、降低 harness 门禁、绕过白盒验证
7. 引入 Lua / runtime script host / 第二套 AI 系统 / 第二套 telegraph 权威
8. 修改 content pack 边界，使其越权定义核心规则语义
9. 用“临时逻辑”污染长期 contract 或阶段出口
10. 把 `testperf` lane / baseline / report-only 目录升级成共享 verification authority，或让它反向决定 `reportPhase5` / unified verification 的 canonical 结论

### 6.3 完成定义

一次合格提交，至少应满足：

1. 已映射到正确的 phase/checkpoint/工作包，并完成对应预检
2. 模块边界与冻结合同未被绕开，没有引入第二真源
3. 命中的自动化验证、owner gate、白盒步骤已经执行或明确说明为何未执行
4. 文档、schema、manifest、baseline、lint / harness 说明在同一改动中同步收口
5. 主干在当前 checkpoint 结束后仍保持可玩
6. non-trivial Kotlin 改动已完成 anti-bloat review，且新增 option/helper/compat path/second-authority/temp path 有明确处理结果

## 7. 一句话原则

当你不确定代码该放在哪一层时，先问自己一句：

“这段代码是在定义游戏规则，还是在展示、装配、验证这些规则？”

如果答案是规则，默认放进 `core`；如果答案是内容装配，默认放进 `game`；如果答案是展示或交互，默认留在 `client`；如果答案是批量验证、流水线或报告，默认放进 `tools`。

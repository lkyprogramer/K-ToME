# K-ToME AGENTS Guide

本文件是 K-ToME 仓库级协作入口合同。

它只保留跨任务、跨阶段都稳定成立的规则：文档入口、模块边界、跨阶段冻结合同、全局验证纪律和高风险红线。当前 phase 的主题、阻塞项、量化门禁、完整命令清单与白盒步骤，统一以下游权威文档为准，不在本文件重复维护第二份真相。

## 1. Scope & Bootstrap

### 1.1 项目定位

K-ToME 是 `Kotlin + libGDX` 单机 Roguelike。项目概况、当前状态、快速开始与完整阶段路线看 [README.md](README.md) 和 [docs/INDEX.md](docs/INDEX.md)，不要在本文件维护第二份说明。

本文件只固定 agent 容易做错、且跨任务稳定成立的仓库级规则：

1. 文档路由与覆盖纪律
2. 模块边界和跨层红线
3. 跨阶段稳定合同
4. 全局验证纪律
5. 高风险操作权限

本文件不重复维护 phase-specific 的完整命令块、固定 seed、量化阈值、白盒步骤、当前阻塞项或 `docs/rule/*.md` 中的细粒度规则。

### 1.2 文档路由

文档入口以 [docs/INDEX.md](docs/INDEX.md) 为准。开始非 trivial 任务前，先用它定位路线图、当前 phase roadmap、verification checklist、`P?-W*` 文档和规则文档。

固定覆盖规则：

1. 旧 `PR01 ~ PR12` 只作历史参考；执行编号统一使用 `P2-W1 ~ P5-W5`
2. 下游文档与上游权威冲突时，先修上游权威，再回写下游文档
3. 不允许在实现、prompt、skill、checklist 或临时报告里复制一套平行长期口径
4. verification / task perf monitor 语义以 [docs/verification/README.md](docs/verification/README.md) 和其下游文档为准，不在本文件展开实现细节

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

### 1.4 非 trivial 任务输出要求

对非 trivial 任务，第一条输出必须给出简短《预检摘要》：命中的 phase/checkpoint/doc、受影响模块、关键合同/风险/非目标、计划验证入口。若文档缺失、约束冲突或用户要求明显违背当前 phase 冻结目标，先指出冲突，不能直接实现。

如果任务跨 checkpoint，优先落上游 contract，再落内容与表现；如果一个工作包结束后主干不可玩，说明切分过大；non-trivial Kotlin 改动提交前必须按 [docs/rule/ai-change-governance.md](docs/rule/ai-change-governance.md) 完成 anti-bloat review。

### 1.5 子代理探索纪律

探索类任务优先派发给子代理执行，主代理负责拆分问题、汇总证据、裁决冲突并做最终方案选择。同一轮并发子代理最多 `3` 个；超过 `3` 条探索线时，必须按风险、证据价值和独立性排序后分批派发。

适用场景包括代码库探索、模块对比、调用链追踪、影响面分析、文档/实现一致性核查、假设验证和大范围证据收集。trivial、一眼可定的单文件修改、强顺序依赖、同一文件内高频反馈或拆分后明显增加重复扫描成本的任务，可以不强制派发子代理。

子代理必须默认只读：

1. 只允许读取文件、搜索代码、查看 diff/log/status、梳理调用关系和总结证据
2. 不允许编辑文件、生成资源、改配置、切分支、提交、推送、删除文件或执行任何破坏性命令
3. 不允许并行运行 `./gradlew`、测试、构建、package、codegen、resource pipeline 或其他会写入产物/缓存的命令；这些操作只能由主代理按全局验证纪律串行决策
4. 输出必须包含关键证据位置、实际查看过的文件/命令、结论、置信度和未解决疑点；不能只给主观判断

主代理必须对多个子代理结论去重、交叉验证并显式处理冲突；最终修改范围、验证入口、风险判断和取舍由主代理负责，不把子代理输出直接当作决策权威。

## 2. Architecture Baseline

### 2.1 基本原则

1. `core` 是规则真源，必须保持零引擎依赖
2. `game` 负责内容 schema、注册表、官方内容包与会话装配
3. `client` 只负责 Tile、UI、输入、音频、Locale bundle、manifest 消费与表现编排
4. `tools` 负责 lint、smoke、harness、batch、perf/soak、release 验证
5. 任何阶段都必须同时满足：可运行、可测试、可度量、可白盒验证
6. 不允许为局部便利引入第二真源、第二套规则路径或跨层业务泄漏

### 2.2 层级红线

模块职责总览见 [README.md](README.md)。本文件只保留容易被 agent 误判的禁止项：

| 模块 | 禁止 |
| --- | --- |
| `core` | 依赖 libGDX / GUI / 音频 / 窗口 / 纹理句柄；输出 localized string、raw asset path、tooltip 文案、视觉指令或 client-only 状态；为了 UI 便利引入第二份显示状态 |
| `game` | 重新实现 `core` 已拥有的公式、状态、AI、地图或世界推进规则；保存表现层副本作为权威状态；绕过 `core` 直接定义第二套规则语义 |
| `client` | 地图生成、战斗结算、AI 决策、world progress、loot budget 等规则逻辑直接写进 `client`；维护规则权威状态的独立副本；生成正式业务文案或资源映射真值 |
| `tools` | 侵入运行时规则路径成为真源；以“测试方便”为由绕过正式 schema、manifest、snapshot、replay 合同；复制 runtime selector、reward legality、owner metric 等 authority 逻辑 |

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
7. 从 `P2-B` 起，Tile 是唯一正式 client 渲染路径；client ASCII fallback / debug renderer 已删除

### 3.2 跨阶段冻结合同

以下合同一旦进入主线，就必须保持单一权威，不允许并行维护第二套。完整设计细节回到 [docs/2026-03-13-core-systems-design-and-phase-supplements.md](docs/2026-03-13-core-systems-design-and-phase-supplements.md) 和当前 phase 文档。

1. 版本与持久化：save / replay / visual / audio / style version
2. 事件与表现桥：`GameEvent`、`LogTokenEvent`、callback registry、`RenderSnapshot`、`VisualManifest`、`AudioManifest`
3. 战斗、资源、状态、天赋：`DamageType`、`PowerType`、`ResourceType`、`StatusEffectDef`、`TalentDef V2`、`EffectOp`
4. AI、Boss、长局与世界推进：`AIProfile`、`BossEncounter`、`BossPhaseDef`、`WorldProgress`、`Quest`、`RunSummary`
5. ProcGen、Loot 与 Content Pack：`TerrainTag`、`LootBudget`、`HiddenEventDef`、`SecretZoneDef`、`ContentPackManifest`、`OverlayEntry`
6. 战术 AI、回放、死因分析：`PerceptionState`、`HateFocus`、`ReplayHeader`、`DeathAnalysis`、`TacticalAIDecisionTrace`

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
14. 任何会提交进仓库的 manifest、report、golden、manual record、fixture、配置或脚本输出中的文件路径，必须使用 repo-relative path；禁止写入 `/Users/...`、`/tmp/...`、`C:\...` 等机器绝对路径。确实需要描述仓库外资源时，只能使用显式占位符或文档说明，不能把本机路径当合同

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

Phase 入口、执行编号、当前 checkpoint 真相、具体 gate、固定 seed、量化阈值、报告字段与白盒路径，统一从 [docs/INDEX.md](docs/INDEX.md) 路由到对应 phase roadmap / checklist / `P?-W*` 文档。本文件不写死 phase-specific 主题、边界、冻结项或命令清单。

任何当前 sprint/PR 拆分，以命中的 `P?-W*` 或 PR 级设计文档为边界。

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

长期回归套件包括 `GoldenSeed`、`LocaleLint + LocaleScreens`、`ContractLint`、`SoloClearLab`、`HeadlessSmoke`、`BossHarness`、`SaveCurrentVersion`、`Perf/Soak`。一旦某个 suite 在当前 phase 被正式引入，就不能再退化成“可选工具”。

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
14. task perf monitor 的目标、边界与产物位置以 [docs/verification/README.md](docs/verification/README.md) 为准；不得把 `.gradle/test-perf/` 的 lane、baseline 或 report-only 目录升级成 unified verification registry 或 `reportPhase5` 的 canonical 输入
15. 项目级图片、音频与 Dark UI sheet 的资源生成权威以 [docs/rule/ai-change-governance.md](docs/rule/ai-change-governance.md#10-resource-generation-authority) 为准；新增正式资源必须通过 `resourcePipelineLint`，不得只提交 runtime 文件或在生产 Kotlin 中维护 owner/inventory key 镜像清单

### 5.3 白盒验证与结果汇报

涉及 `client`、渲染、输入、交互、Tile、Boss telegraph、content pack、package 的改动，必须保留明确白盒步骤；涉及安装包的改动，还必须补安装包启动与验收步骤。

验证失败时保留 trace、hash、输入脚本、截图差异或 batch 摘要。完成任务后真实说明实际运行命令、未运行命令及原因、白盒验证是否执行、剩余风险；禁止把“理论上应该通过”当作已经验证。

### 5.4 Git 与 GitHub 操作优先级

1. 本地已安装 `gh` CLI 后，涉及 repository、branch、PR、issue、review、workflow / check 等 GitHub 协作语义时，默认优先使用 `gh`
2. 只有在 `gh` 没有等价能力、无法满足精确代码阅读需求、或明确属于纯本地 plumbing / working-tree 检查时，才回退使用 `git`
3. 对代码审查、改动阅读与行级 patch 判断，允许直接使用 `git diff`、`git show`、`git status` 等原生命令
4. 该优先级规则不放宽任何高风险 Git 限制；涉及重写历史、强推、破坏性 checkout/reset 等操作，仍按更严格规则执行

## 6. Red Lines & Completion

### 6.1 操作权限

| 级别 | 可做事项 |
| --- | --- |
| Allowed | 读取文件；搜索代码；运行非破坏性检查；执行 focused tests / lint / harness；做局部代码或文档编辑；使用 `git status` / `git diff` / `git show` / `gh pr view` 等只读命令 |
| Ask First | schema/version/public contract 变化；依赖升级；大范围资源生成或替换；删除已跟踪文件；修改 release / CI / nightly gate 语义；跨模块重排；任何会显著扩大 diff 或改变验收口径的操作 |
| Never | `git reset --hard`、强推、重写历史；删除用户未确认的本地成果；提交密钥或本地私有配置；绕过 verification gate；伪造验证结果；引入第二 authority 来让报告变绿 |

这些权限不替代更严格的系统级 Git / 文件安全规则；遇到冲突时按更严格者执行。

### 6.2 文档同步

如果变更影响以下任一内容，必须同步更新对应文档：

1. 架构边界、模块职责、系统合同
2. phase 出口、checkpoint、工作包范围或执行编号
3. 构建方式、测试入口、release gate
4. schema、manifest、report 或 lint / harness 合同

补充要求：

1. 如果 `AGENTS.md` 发生变化，应同时评估 [docs/INDEX.md](docs/INDEX.md)、phase roadmap、verification checklist、相关 `P?-W*` 文档是否需要同步
2. 如果新增目录承载明确职责，建议补简短 README，避免结构语义只存在于代码里

### 6.3 高风险变更红线

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
10. 把 task perf lane / baseline / report-only 目录升级成共享 verification authority，或让它反向决定 `reportPhase5` / unified verification 的 canonical 结论

### 6.4 完成定义

一次合格提交，至少应满足：

1. 已映射到正确的 phase/checkpoint/工作包，并完成对应预检
2. 模块边界与冻结合同未被绕开，没有引入第二真源
3. 命中的自动化验证、owner gate、白盒步骤已经执行或明确说明为何未执行
4. 文档、schema、manifest、baseline、lint / harness 说明在同一改动中同步收口
5. 主干在当前 checkpoint 结束后仍保持可玩
6. 提交内容不包含本机绝对路径；manifest、report、golden、manual record、fixture 与脚本输出中的文件路径已经保持 repo-relative
7. non-trivial Kotlin 改动已完成 anti-bloat review，且新增 option/helper/compat path/second-authority/temp path 有明确处理结果

## 7. 一句话原则

当你不确定代码该放在哪一层时，先问自己一句：

“这段代码是在定义游戏规则，还是在展示、装配、验证这些规则？”

如果答案是规则，默认放进 `core`；如果答案是内容装配，默认放进 `game`；如果答案是展示或交互，默认留在 `client`；如果答案是批量验证、流水线或报告，默认放进 `tools`。

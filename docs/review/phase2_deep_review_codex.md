# Phase2 深度审查报告

## 1. 执行摘要

1. 当前仓库不是“Phase2 已完成”，而是“部分合同骨架已落地，但正式玩法闭环没有成立”。
2. `save/schema/i18n/manifest/snapshot` 的形式化工作基本存在，但核心运行时仍停留在 Phase1 语义：`TurnScheduler` 还是 `100` 能量、运行时只有 `Stamina`、`DamageType / ResourcePool / TalentDef V2` 没有进入主链。
3. 正式玩家路径仍是 ASCII，不是文档要求的 Tile 正式路径：`FoundationGameScreen` 直接绑定 `AsciiRenderer`，四个 zone 的 `tilesetKey` 仍是 `tileset.foundation.ascii`。
4. 四职业只在 schema 层“看起来齐了”；当前真正可用的天赋只有 Vanguard 的 4 个，Arcanist / Rogue / Templar 没有最小 talent 包，客户端烟雾报告里 Arcanist HUD 仍显示 `STA`。
5. 当前不是文档定义的 4-zone 短局，而是 4 个彼此独立的 2-floor 微切片；`headlessSmoke` 只验证“到达第 2 层”，并不验证 Boss/结算闭环。
6. 内容量远未达到 Phase2 出口：当前只有 5 个 monster、1 个 boss、8 个实际 item；`24 怪 + 24 物品` 没有接近。
7. 奖励与目标系统基本空转：`lootProfiles`、`objectiveSetId`、`soloContract`、大部分 `aiProfileId` 只是 schema 字段，没有形成驱动玩家决策的运行时行为。
8. 验证门禁与文档承诺脱节：仓库里没有 `soloClearLab` 任务；`goldenScreenshot` 是软件拼块 hash，不是真实 Tile/UI golden；`longRunLab` 只跑 Vanguard 且在 seed `20260322` 失败。
9. 资源管线完成度是“可校验 key”，不是“正式资源闭环”：`visual-manifest.json` 的 149 个条目里有 92 个直接指向 `debug/missing_visual.png`，`audio-manifest.json` 的 62 个条目里有 37 个是静音 fallback。
10. 直接结论：当前版本属于“基础可跑、有形式化外壳，但不好玩，也不具备耐玩雏形”；不建议直接进入 Phase3。

当前状态判断：

- 不是“功能完成但体验未闭环”中的轻度问题，而是“合同名义完成、核心体验和门禁口径都未达标”。
- 不是“基本可玩但不够耐玩”，而是“有可启动的局内循环，但还不足以支撑 Phase2 应有的短局动机与多职业重玩价值”。

## 2. 审阅范围与依据

### 2.1 参考文档

- `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
- `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
- `docs/phase2/roadmap.md`
- `docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md`
- `docs/phase2/2026-03-13-phase2-pr-01-serialization-and-version-discipline.md`
- `docs/phase2/2026-03-13-phase2-pr-02-core-semantic-contracts.md`
- `docs/phase2/2026-03-13-phase2-pr-03-locale-and-schema-v2.md`
- `docs/phase2/2026-03-13-phase2-pr-04-snapshot-and-manifest.md`
- `docs/phase2/2026-03-13-phase2-pr-05-minimal-tile-shell.md`
- `docs/phase2/2026-03-13-phase2-pr-06-minimal-official-slice.md`
- `docs/phase2/2026-03-13-phase2-pr-07-short-run-expansion.md`
- `docs/phase2/2026-03-13-phase2-verification-checklist.md`

### 2.2 审查代码与资源范围

- `core/src/main/kotlin/com/ktome/core/save/*`
- `core/src/main/kotlin/com/ktome/core/turn/*`
- `core/src/main/kotlin/com/ktome/core/combat/*`
- `core/src/main/kotlin/com/ktome/core/talent/*`
- `core/src/main/kotlin/com/ktome/core/event/*`
- `core/src/main/kotlin/com/ktome/core/snapshot/*`
- `game/src/main/kotlin/com/ktome/game/*`
- `game/src/main/resources/data/**/*.yaml`
- `game/src/main/resources/i18n/*.json`
- `client/src/main/kotlin/com/ktome/client/**/*`
- `client/src/main/resources/manifests/*`
- `tools/src/test/kotlin/com/ktome/tools/lint/*`
- `assets-src/image/specs/*`
- `assets-src/audio/specs/*`
- `assets-src/image/manifests/*`
- `assets-src/audio/manifests/*`

### 2.3 实际执行的命令

已运行：

- `./gradlew localeLint contractLint headlessSmoke clientSmoke goldenScreenshot`
  - 结果：通过
- `./gradlew longRunLab`
  - 结果：失败，`seed 20260322` 出现 `Turn budget exhausted`

未运行：

- `./gradlew test`
- `./gradlew :core:test`
- `./gradlew :game:test`
- `./gradlew preReleaseAcceptance`

说明：

- 本次重点是 Phase2 深度审查，不是完整提测。
- 仓库内不存在 `soloClearLab` Gradle 入口，属于本次发现的问题之一。

### 2.4 审阅方法

1. 先按 `P2-W1 ~ P2-W7` 建立“设计承诺 -> 代码/数据/资源/门禁”映射。
2. 再从玩家视角重构当前真实体验链：开局、探索、战斗、奖励、成长、结算、复玩动机。
3. 最后用运行时证据校正纸面完成度，识别伪完成项、门禁口径漂移和当前阶段必须处理的设计债。

## 3. Phase2 设计实现一致性矩阵

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| Save Schema V2 / 版本纪律 | Phase2 要冻结 `Save Schema V2`、`saveContractVersion`、资源版本边界、fail-fast 读档。 | 部分实现 | `docs/phase2/2026-03-13-phase2-pr-01-serialization-and-version-discipline.md`；`core/src/main/kotlin/com/ktome/core/save/SaveSnapshot.kt`；`core/src/main/kotlin/com/ktome/core/save/SaveCodec.kt`；`game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt` | 存档 codec、fail-fast、`AssetVersionContract` 已存在，但 `SaveSnapshot.CURRENT_SCHEMA_VERSION` 仍是 `1`，与“Save Schema V2”口径不一致；同时 `timestampEpochMillis = System.currentTimeMillis()` 混入了非确定性调试字段，`WorldProgressSnapshot / objective` 也未进入保存结构。 | Medium |
| Core Semantic Contracts | Phase2 要落地 `1000` 能量、`DamageType`、`ResourcePool`、基础状态骨架、事件总线、`TalentDef V2` runtime、`FoundationGameSession` 拆厚。 | 偏离实现 | `docs/phase2/2026-03-13-phase2-pr-02-core-semantic-contracts.md`；`core/src/main/kotlin/com/ktome/core/turn/TurnScheduler.kt`；`core/src/main/kotlin/com/ktome/core/talent/TalentModels.kt`；`core/src/main/kotlin/com/ktome/core/event/GameEvent.kt`；`game/src/main/kotlin/com/ktome/game/factory/EntityFactory.kt`；`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` | `TurnScheduler` 默认阈值仍是 `100`；运行时只有 `Stamina`，没有 `ResourcePool`；`DamageType` 不存在于 combat runtime；状态只有 `STUNNED / ARMOR_BREAK / WAR_CRY_BUFF / WAR_CRY_DEBUFF` 四种；`GameEvent` 没有 `turnId/actionId/phase/payload` 结构；`FoundationGameSession` 仍是 `2216` 行巨类。 | Critical |
| Schema V2 + i18n | 文档要求正式对象切到 `nameKey/descKey/visualKey/iconKey/audioProfile/schemaVersion`，首页 locale 可切换，lint 成为门禁。 | 部分实现 | `docs/phase2/2026-03-13-phase2-pr-03-locale-and-schema-v2.md`；`game/src/main/resources/data/**/*.yaml`；`game/src/main/resources/i18n/en-US.json`；`game/src/main/resources/i18n/zh-CN.json`；`tools/src/test/kotlin/com/ktome/tools/lint/LocaleLintTest.kt`；`tools/src/test/kotlin/com/ktome/tools/lint/ContractLintTest.kt` | Schema/i18n/lint 的壳体成立，但大量对象只是 skeleton；lint 测试把当前低配集合硬编码成“正确范围”，例如只冻结 5 个 monster 与 4 个 vanguard talent，等于把 under-scope 实现合法化。 | High |
| RenderSnapshot / Manifest | 文档要求 `RenderSnapshot` 成为唯一正式视图模型，manifest 成为唯一资源真源，golden 稳定回归。 | 部分实现 | `docs/phase2/2026-03-13-phase2-pr-04-snapshot-and-manifest.md`；`core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`；`client/src/main/kotlin/com/ktome/client/assets/ManifestModels.kt`；`client/src/main/kotlin/com/ktome/client/assets/ManifestResolvers.kt`；`client/src/main/resources/manifests/visual-manifest.json`；`client/src/main/resources/manifests/audio-manifest.json` | snapshot / manifest 结构存在，但没有把 Phase2 正式玩家路径带到 Tile；同时 manifest 大量 key 只是占位资源，`RenderSnapshotAssetAudit` 只检查 key 可解析，不检查是否仍落在 `missing_visual` / 静音占位。 | High |
| Minimal Tile Shell | 文档要求 `TileRenderer + HUD + 背包/检视 + 双语 UI` 成为正式主路径，ASCII 只保留 debug/fallback。 | 未实现 | `docs/phase2/2026-03-13-phase2-pr-05-minimal-tile-shell.md`；`client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`；`client/src/main/kotlin/com/ktome/client/render/AsciiRenderer.kt`；`client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt` | 客户端正式渲染器仍是 `AsciiRenderer`；仓库中没有 `TileRenderer`；输入、HUD、inspect、inventory 也围绕 ASCII 侧栏模型组织。 | Critical |
| Minimal Official Slice (`P2-B`) | 文档要求至少成立 `2 职业 + 1 zone 链 + 1 Boss + 30~60 分钟切片`，含交互物、掉落、Boss warning、正式资源链路。 | 偏离实现 | `docs/phase2/2026-03-13-phase2-pr-06-minimal-official-slice.md`；`game/src/main/resources/data/professions/index.yaml`；`game/src/main/resources/data/zones/index.yaml`；`game/src/main/resources/data/bosses/index.yaml`；`game/src/main/kotlin/com/ktome/game/GameModule.kt`；`build/reports/harness/client-smoke.json` | 只有一个最小 dungeon loop；没有 `armory_gate / supply_crate / alarm_bonfire` 运行时实现；Arcanist 虽可开局但没有 talent 包；zone 不是链路，只是单独场景；切片目标更像“2 层可下楼”，不是 30~60 分钟闭环。 | High |
| Short Run Expansion (`P2-C`) | 文档要求 `4 职业 + 4 zone + 24 怪 + 24 物品 + SoloClearLab v1` 完整短局。 | 未实现 | `docs/phase2/2026-03-13-phase2-pr-07-short-run-expansion.md`；`game/src/main/resources/data/monsters/index.yaml`；`game/src/main/resources/data/items/index.yaml`；`build.gradle.kts`；`game/src/test/kotlin/com/ktome/game/harness/HeadlessSmokeSuiteTest.kt` | 当前只有 5 个 monster、1 个 boss、8 个实际 item；没有 `soloClearLab` 任务；4 zone 只是 4 个独立 2-floor 场景；4 职业也只有 schema 外壳，runtime 并未完成。 | Critical |
| 职业/资源/构筑驱动 | 文档要求四基础职业在 Phase2 就体现不同主资源、最小 talent 包和最小通关 build。 | 未实现 | `docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md`；`docs/phase2/2026-03-13-phase2-pr-06-minimal-official-slice.md`；`docs/phase2/2026-03-13-phase2-pr-07-short-run-expansion.md`；`game/src/main/resources/data/professions/index.yaml`；`game/src/main/resources/data/talents/index.yaml`；`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`；`build/reports/harness/client-smoke.json` | 4 个 profession schema 存在，但 talent 总数只有 4 个，且全部属于 Vanguard；`build/reports/harness/client-smoke.json` 中 `arcanist` HUD 仍显示 `STA 110/110`；`EntityFactory` 与 `FoundationGameSession` 全程只处理 `Stamina`。 | Critical |
| 奖励 / 掉落 / 目标 | 文档要求掉落、任务奖励、Boss 奖励和短局目标共同驱动推进。 | 偏离实现 | `docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md`；`docs/phase2/2026-03-13-phase2-pr-06-minimal-official-slice.md`；`game/src/main/resources/data/loot/index.yaml`；`game/src/main/resources/data/zones/index.yaml`；`core/src/main/kotlin/com/ktome/core/item/ItemGenerator.kt`；`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` | `lootProfiles` 只作为 schema 名字存在，运行时未使用；`objectiveSetId` 全为空；Boss `rewards` 也未消费；当前奖励来源基本是 starter kit + 随机掉落，且不会显著改变路线或构筑决策。 | High |
| AI / Boss Encounter | 文档要求 elite/Boss 至少用 Layer 2 simple scripted AI，并具备最小 telegraph / warning 反馈。 | 部分实现 | `docs/phase2/2026-03-13-phase2-pr-06-minimal-official-slice.md`；`docs/phase2/2026-03-13-phase2-pr-07-short-run-expansion.md`；`game/src/main/resources/data/ai/index.yaml`；`game/src/main/kotlin/com/ktome/game/factory/EntityFactory.kt`；`core/src/main/kotlin/com/ktome/core/ai/AIDecision.kt`；`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` | AIProfile 只有 4 个裸 id；运行时仍被压缩为 `CHASE / KITE / PATROL`；Boss 只是 `CHASE + Vanguard 4 技能优先表`，不是按 schema 定义的职业化/遭遇化脚本。Overlay warning 有最小形态，但不足以构成正式 encounter 设计。 | High |
| 资源生产管线 / 资源 DoD | 文档要求 asset/audio/style/manifest lint 成为正式门禁，并让 Phase2 主路径摆脱占位资源。 | 偏离实现 | `docs/phase2/2026-03-13-phase2-pr-04-snapshot-and-manifest.md`；`docs/phase2/2026-03-13-phase2-pr-07-short-run-expansion.md`；`assets-src/image/specs/phase2-asset-plan.yaml`；`client/src/main/resources/manifests/visual-manifest.json`；`client/src/main/resources/manifests/audio-manifest.json` | 管线和 lint 已有，但完整性不足：`visual-manifest.json` 中 149 项有 92 项直接指向 `debug/missing_visual.png`；`audio-manifest.json` 中 62 项有 37 项指向静音 fallback。当前门禁保证的是“不会丢 key”，不是“正式资源真的到位”。 | High |
| Smoke / Golden / Lab 门禁 | 文档要求 `save/load`、`locale-lint`、`contract-lint`、`golden screenshot`、`SoloClearLab`、`headless/client smoke` 共同证明 Phase2 出口。 | 偏离实现 | `docs/phase2/2026-03-13-phase2-verification-checklist.md`；`build.gradle.kts`；`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`；`game/src/test/kotlin/com/ktome/game/harness/HeadlessSmokeSuiteTest.kt`；`game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt`；`build/reports/harness/headless-smoke.md`；`build/reports/harness/long-run-summary.md` | 没有 `soloClearLab`；`headlessSmoke` 目标只是到达 2 层，且报告中 `outcome=InProgress`；`goldenScreenshot` 不是实际客户端截图，而是软件矩形渲染 hash；`longRunLab` 只跑 Vanguard 且对 seed `20260322` 失败。 | Critical |

## 4. 当前阶段玩法体验总评

### 4.1 核心循环

当前真实循环更接近：

`开局 -> BSP 房间探索 -> 基础近战/少量技能 -> 拾取随机装备/消耗品 -> 下楼 -> 两层后结束`

它不是文档承诺的：

`4 zone 短局路线 -> 职业/资源/奖励驱动分化 -> Boss/结算 -> 再开新局试不同构筑`

核心问题：

- 中期目标不存在。`objectiveSetId` 全为空，交互物没有进入运行时，四个 zone 也不是一条 route。
- 奖励驱动太弱。随机掉落能涨数值，但很少迫使玩家改变打法或路线。
- 复玩驱动不成立。因为 4 职业并没有真实打法差异，换职业只是在换初始 stat 与 starter item。

最弱环节是：

- `奖励 -> 成长 -> 新选择` 这一段。

当前有流程，但没有强烈爽点：

- 战斗大多仍是“靠近、平A、偶尔放一个 Vanguard 技能”。
- 物品主要是加攻击/加防御/回血/传送，不形成 build 决策链。
- zone 虽然换了名字和尺寸，但没有新的冒险动机或路线风险结构。

关于节奏：

- 当前已经不是“前 10 分钟有意思、30 分钟后掉线”，而是“前 5~10 分钟就基本看完主要决策模板”。
- 文档要求 `P2-B` 至少是 `30~60` 分钟正式切片，但当前 `longRunLab` 的大部分胜利都在 `159~277` turn 内结束于单 zone 两层，且 `headlessSmoke` 连 Boss/结算都不要求。

### 4.2 战斗体验

优点：

- 基础 hit/crit/伤害/死亡/升级反馈是可读的。
- inspect、日志 token、最小 overlay warning 已经具备某种“规则 -> 反馈”的轮廓。

根本缺陷：

- 有意义的技能决策几乎只存在于 Vanguard。`TalentResolver.supportedTalentIds` 只支持 `power_strike / charge / shield_bash / war_cry`。
- Boss 和 elite 没有真正独立的遭遇逻辑；`ai.boss.dungeon_lord` 在运行时仍被折叠成 `CHASE`。
- 伤害通道、资源通道、职业 answer 都没有真正打进玩法主链，因此战斗判断空间极窄。

结果是：

- “判断 -> 执行 -> 反馈”的闭环只在很窄的一层成立。
- 玩家能理解发生了什么，但很难因为职业差异、资源差异或敌人机制差异产生长期兴趣。
- 目前最优解容易收敛到“更硬、更稳、更能平推”，而不是不同 build 的互斥优势。

### 4.3 成长与构筑

当前成长系统给人的真实感受是“数值上涨”，不是“玩法变化”。

原因：

- 4 个 profession schema 存在，但真正进入运行时的天赋只有 4 个，且全部属于 Vanguard。
- `resolveStartingTalentIds()` 对 Arcanist / Rogue / Templar 最终返回空列表，导致这三职业没有正式起手 build。
- `FoundationGameSession.buildPlayerStatusSnapshot()` 和 `buildTalentSnapshots()` 固定使用 `stamina` 语义；职业资源没有形成可视化和操作上的真实分化。

直接体验后果：

- 玩家不会产生“这局我想试另一种 build”的冲动。
- 甚至不会稳定产生“换职业会怎么玩”的好奇心，因为当前换职业几乎只是换一套基础属性和起始装备。

### 4.4 奖励驱动

当前奖励系统的问题不是“掉率不对”，而是“奖励没有连接到玩法结构”。

证据：

- `lootProfiles` 只加载不消费。
- `bossEncounters.rewards` 只存在于 schema，不进入击杀结算。
- `objectiveSetId` 全空，交互物目标没有运行时实现。
- `ItemGenerator` 只是按 floor 在 8 个基础 item 中做通用随机。

体验层面：

- 每次战斗、探索、下楼都未必让玩家产生“下一步有新可能”的感受。
- 当前物品是通用 stat 补丁，不是构筑杠杆。
- Boss 胜利更多是 run 结束信号，不是 build 被确认和放大的高潮。

### 4.5 探索与重复游玩价值

当前“有 4 个 zone”不等于“有 4 段可探索体验”。

因为：

- 四个 zone 没有串成 route，只是独立配置。
- 内容池只有 5 个 monster、1 个 boss，重复度极高。
- `tilesetKey` 全部仍是 `tileset.foundation.ascii`，视觉差异没有成为正式主路径体验。
- 文档中提到的交互物与特殊机制，如 `armory_gate / supply_crate / alarm_bonfire`、路线物件、区域任务物件，并未进入运行时。

因此：

- 地图的“未知感”主要来自 BSP 几何，不来自内容组合。
- 风险感、发现感、记忆点都偏弱。
- 当前重复游玩理由不足以支撑 Phase2 结束语义。

### 4.6 UI / 反馈 / 新手体验

当前 UI/反馈层的评价是“两头失衡”：

- 规则文本与 locale 重渲染做得比玩法成熟度更靠前。
- 反馈基础设施比正式体验路径更完整。

做得对的部分：

- 首页语言切换成立。
- 背包、inspect、日志 token、保存后换语种继续都能工作。
- 玩家至少知道自己打到了什么、捡到了什么。

当前必须指出的问题：

- 正式玩家路径仍是 ASCII，不符合 Phase2 对外体验定位。
- 非 Vanguard 职业的资源 UI 信息错误或误导，直接抬高理解成本。
- 没有明确的 run route/当前 objective 告知，玩家难以理解“为什么我要继续这一局”。
- 没有失败复盘/死亡解释，玩家即使输也很难知道是路线、资源还是操作问题。

### 4.7 系统联动性

当前项目最危险的地方不在于“没做系统”，而在于“做了很多系统壳，但它们没有真正联动”。

联动得不错的部分：

- `schema -> i18n -> snapshot -> client locale render`
- `save/load -> locale re-render`
- `manifest -> asset audit`

联动断裂最严重的部分：

- `profession schema -> resource runtime -> HUD/操作`
- `monster/zone/boss schema -> AI/遭遇脚本 -> rewards`
- `lootProfile/objectiveSetId/soloContract -> run loop`
- `phase2 gate -> actual gameplay definition of done`

这意味着：

- 当前仓库更像“形式化边界项目 + 可玩的旧核心”，不是“Phase2 完整短局产品”。

## 5. 当前 Phase2 最需要解决的关键问题

### 5.1 核心语义合同仍是假完成

**问题描述**

Phase2 最该先落地的 `1000` 能量、资源池、伤害通道、事件总线、Talent V2 runtime 没有真正进入运行时主链。

**证据**

- `core/src/main/kotlin/com/ktome/core/turn/TurnScheduler.kt` 默认阈值仍为 `100`
- `game/src/main/kotlin/com/ktome/game/factory/EntityFactory.kt` 只给玩家挂 `Stamina`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` 的 HUD 与 talent snapshot 固定写死 `ui.hud.stamina.short`
- `core/src/main/kotlin/com/ktome/core/talent/TalentResolver.kt` 只支持 4 个 Vanguard 天赋
- `core/src/main/kotlin/com/ktome/core/event/GameEvent.kt` 没有文档要求的 `turnId/actionId/phase/payload`

**为什么这是当前必须解决的问题**

- Phase2 的存在意义就是“先把临时语义换成正式语义”，不是先把 schema 名字补齐。
- 如果 Phase3 建在当前运行时上，后续所有职业、Boss、状态、长局结构都会继续依赖错误的底层口径。

**不解决的后果**

- 未来每新增一个职业/怪物/技能都要再做一次底层翻修。
- 资源、UI、日志、平衡验证都会不断出现“文档说一套，运行时是一套”的双轨问题。

### 5.2 当前不是短局闭环，而是 4 个独立 demo

**问题描述**

文档承诺的是 4-zone 短局；当前实现是 4 个彼此独立的 2-floor zone scenario。

**证据**

- `game/src/main/kotlin/com/ktome/game/FoundationGameConfig.kt` 默认只有单 `zoneId`
- `game/src/main/kotlin/com/ktome/game/GameModule.kt` 每局只按单 zone 的 `floorCount` 构建 dungeon
- `build/reports/harness/headless-smoke.md` 四条 smoke 都只是各自 zone 到达第 2 层
- `build/reports/harness/long-run-summary.md` 全部 run 都是 `zone=shattered_outpost`，胜利也只在 `floor=2`

**为什么这是当前必须解决的问题**

- “短局闭环”是 Phase2 的出口，不是 Phase3 的前置草图。
- 如果现在不把短局 route 建起来，后续长局结构会建立在一个根本没有跑通过的短局基础上。

**不解决的后果**

- 玩家没有清晰的中期目标与阶段推进感。
- 后续 Phase3 的世界结构会被迫同时修补 Phase2 本该完成的 route 设计。

### 5.3 三个职业是 schema 完成，不是玩法完成

**问题描述**

Arcanist、Rogue、Templar 目前只在数据上存在，不在玩法上成立。

**证据**

- `game/src/main/resources/data/talents/index.yaml` 当前总 talent 数为 4，且全属于 Vanguard
- `game/src/main/resources/data/professions/index.yaml` 中 Arcanist / Rogue / Templar 的 `startingTalents` 为空
- `game/src/main/kotlin/com/ktome/game/GameModule.kt` 的 `resolveStartingTalentIds()` 对空树返回空 talent 列表
- `build/reports/harness/client-smoke.json` 中 `new-game-arcanist-greenwood-en` HUD 为 `STA 110/110`

**为什么这是当前必须解决的问题**

- Phase2 的重玩价值，本质上就来自“至少四个基础职业的最小 build 分化”。
- 这不是内容量问题，而是当前体验能否成立的问题。

**不解决的后果**

- 玩家不会因为职业差异继续开下一局。
- 所有后续 balance/content expansion 都会误判职业矩阵已经成立。

### 5.4 奖励、掉落与目标没有形成驱动

**问题描述**

当前奖励系统没有把探索、战斗、构筑与推进串起来。

**证据**

- `game/src/main/resources/data/loot/index.yaml` 只有三个名字化 `lootProfiles`
- `game/src/main/kotlin/com/ktome/game/GameModule.kt`、`FoundationGameSession.kt` 中没有消费 `lootProfileId`
- `game/src/main/resources/data/zones/index.yaml` 的 `objectiveSetId` 全为空
- `bossEncounters.rewards` 只在 schema 中存在，不进入运行时奖励发放

**为什么这是当前必须解决的问题**

- 这是当前“好不好玩”的中枢问题，不是未来内容加量才能解决的问题。
- 即使不做商店、制作和复杂经济，Phase2 也必须让战斗和探索的收益可感、可预测、可记忆。

**不解决的后果**

- 战斗只剩清怪效率，没有路线与 build 的抉择价值。
- 后续增加再多内容，也只是把浅层奖励池变大，不会自然变得更好玩。

### 5.5 正式玩家路径和可视验证都是伪完成

**问题描述**

Phase2 文档最强调的 Tile 正式路径没有实现，连 golden 也不是对真实正式路径的验证。

**证据**

- `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt` 直接 new `AsciiRenderer`
- 仓库内没有 `TileRenderer` 实现
- `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt` 使用 `SoftwareGoldenRenderer` 自己画矩形，不走真实 libGDX Tile/UI
- `game/src/main/resources/data/zones/index.yaml` 四个 zone 的 `tilesetKey` 全为 `tileset.foundation.ascii`
- `client/src/main/resources/manifests/visual-manifest.json` 中 92/149 条目仍是 `debug/missing_visual.png`

**为什么这是当前必须解决的问题**

- Tile/i18n 正式路径就是 Phase2 的主题之一，不能拿 ASCII 继续冒充正式玩家路径。
- 如果连 golden 都不验证真实主路径，门禁会持续失真。

**不解决的后果**

- 后续资源、UI、Boss warning、zone 视觉差异都会继续建立在 debug/fallback 语义上。
- 玩家看到的仍然是“可跑的研发壳”，不是阶段完成品。

### 5.6 验证门禁定义了错误的完成标准

**问题描述**

当前门禁不是在验证 Phase2 文档，而是在验证一个更弱、更窄的内部版本。

**证据**

- `build.gradle.kts` 中不存在 `soloClearLab`
- `game/src/test/kotlin/com/ktome/game/harness/HeadlessSmokeSuiteTest.kt` 目标只是 `ReachFloor(2)`
- `build/reports/harness/headless-smoke.md` 显示四条 smoke 全部 `outcome=InProgress`
- `game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt` 只跑 `FOUNDATION_PROFESSION_ID = vanguard`
- `tools/src/test/kotlin/com/ktome/tools/lint/ContractLintTest.kt` 直接把当前 5 个 monster / 4 zone / 单 boss 集合写成固定正确答案

**为什么这是当前必须解决的问题**

- 只要 gate 是错的，团队就会持续在错误的完成定义上前进。
- 这类问题拖到后续阶段会变成“所有历史绿色构建都不能代表质量”。

**不解决的后果**

- Phase 进度判断失真。
- 后续每次合入都会继续放大“看起来通过了，实际上没完成”的偏差。

### 5.7 `FoundationGameSession` 仍是设计债核心

**问题描述**

本应在 Phase2 首轮拆厚的会话边界没有落地，`FoundationGameSession` 仍承担过多职责。

**证据**

- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` 共 `2216` 行
- 该文件同时承担：snapshot 构建、怪物技能意图、overlay、inventory、inspect、经验、死亡、楼层切换、save 持久化等职责

**为什么这是当前必须解决的问题**

- 这是典型“当前不拆，后面越拆越难”的债。
- Phase3 要引入更复杂的战斗、状态、职业树和长局结构，若继续建立在当前巨类上，改一处必定牵动全身。

**不解决的后果**

- 所有 P0 修复成本都会被放大。
- Phase3 的任何新增系统都会进一步把边界做坏。

## 6. 优化建议（按优先级）

### P0：必须立刻处理

#### P0-1 让运行时合同真正达到 Phase2 最低线

- 问题本质：当前 schema/文档已经切到 Phase2，但核心 runtime 还停留在 Phase1。
- 影响范围：`core.turn`、`core.combat`、`core.talent`、`core.ecs`、`game.factory`、`game.FoundationGameSession`、save/snapshot/UI。
- 优化目标：先把“底层语义正确”补齐，再谈多职业短局。
- 具体改法：
  1. 把 `TurnScheduler` 和所有行动成本切到 `1000` 口径。
  2. 引入最小 `ResourceType / ResourcePool`，至少覆盖 `STAMINA / MANA / ENERGY / POSITIVE_ENERGY`。
  3. 让 `TalentDef` runtime 携带 `resourceCosts / castTime / damageType / targeting / telegraph`，不再只剩 `staminaCost`。
  4. 把 `GameEvent` 扩成带 `type / turnId / actionId / source / target / phase / payload` 的正式结构。
  5. 将 `buildPlayerStatusSnapshot()` 和 `buildTalentSnapshots()` 改成按 profession/resourceType 动态展示。
- 优先级：P0
- 预期收益：四职业、Boss、UI、save/load、日志、平衡验证终于建立在同一语义基线上。
- 可能副作用 / 风险：会触发大量测试重录与 save schema 调整；短期内构建会变红。
- 需同步项：`docs/phase2` 检查点、相关 checklist、save/snapshot 测试、client smoke、headless smoke。

#### P0-2 把四职业从“schema 存在”补成“最小可玩”

- 问题本质：当前只有 Vanguard 是职业，另外三个只是职业名。
- 影响范围：`game/src/main/resources/data/professions/index.yaml`、`game/src/main/resources/data/talents/index.yaml`、`DataLoader`、`TalentResolver` 或新 executor runtime、`FoundationGameSession`、client HUD/inspect。
- 优化目标：让四职业都具备文档承诺的最小 build contract 和资源差异。
- 具体改法：
  1. 按文档补齐四职业最小 talent 包，不再允许空树。
  2. Arcanist 至少落火/冰/位移/护盾最小包；Rogue 至少落机动/爆发/脱战；Templar 至少落 holy 输出/净化/护盾。
  3. 将资源消耗、回复和 HUD 文案绑定到 profession，而不是固定写死 stamina。
  4. 为四职业补独立 smoke 场景，至少覆盖一次职业 answer 是否存在。
- 优先级：P0
- 预期收益：玩家终于会有“换职业就是换玩法”的真实感受。
- 可能副作用 / 风险：会暴露战斗平衡、AI 适配和资源 UI 的连锁问题。
- 需同步项：文档 talent 包、数据、图标/音频 key、`headlessSmoke`、`clientSmoke`、后续 `soloClearLab`。

#### P0-3 把 4 个独立 zone 改造成一条真实短局路线

- 问题本质：当前没有 run route，只有场景切片。
- 影响范围：`FoundationGameConfig`、`GameModule`、`FoundationGameSession`、zone schema、save/load、harness。
- 优化目标：让 Phase2 真正拥有 `shattered_outpost -> greenwood_fringe -> deep_iron_pit -> grey_gate_depths` 的短局推进感。
- 具体改法：
  1. 引入最小 `ZoneRoute` / `RunRouteState`，下楼完成当前 zone 后转入下一 zone，而不是直接结束 run。
  2. 让 `grey_gate_depths` 成为真正的 final zone，不再是与其他 zone 并列的独立 demo。
  3. 把 `objectiveSetId` 至少接成轻量目标链，如“拿钥匙 -> 开门 -> 触发 Boss 房”。
  4. `headlessSmoke` / `clientSmoke` 的默认正式路径改为整条短局路线，而不是单 zone floor 2。
- 优先级：P0
- 预期收益：中期目标、推进感、地图记忆点和 Boss 前期待都会显著提升。
- 可能副作用 / 风险：save schema、run summary 和 smoke bot 都要调整。
- 需同步项：zone/boss/objective 数据、save contract、smoke harness、Phase2 checklist。

#### P0-4 重写 Phase2 门禁，让它验证文档而不是验证缩写版实现

- 问题本质：当前 gate 定义错了，导致 under-scope 实现也能保持绿色。
- 影响范围：`build.gradle.kts`、`game/build.gradle.kts`、`client/build.gradle.kts`、`tools` lint tests、harness tests、reports。
- 优化目标：让绿色构建真正代表“Phase2 达标”。
- 具体改法：
  1. 新增正式 `soloClearLab` 任务与报告。
  2. 把 `headlessSmoke` 的目标从 `ReachFloor(2)` 改为“短局闭环关键节点”，至少覆盖 Boss 或 route transition。
  3. 把 `clientSmoke` 扩展到四职业 UI/resource/inspect 验证，不再只验证 Vanguard 和一条 Arcanist 新局。
  4. 重写 `ContractLintTest` 中错误冻结的 under-scope 常量，改成文档承诺的真实范围。
  5. 将 `longRunLab` 至少扩为四职业矩阵，并修复当前 seed `20260322` 卡死问题。
- 优先级：P0
- 预期收益：之后每次提交都能被正确质量基线约束，不再出现“绿了但没完成”。
- 可能副作用 / 风险：短期会让很多历史绿色路径变红。
- 需同步项：`docs/phase2/2026-03-13-phase2-verification-checklist.md`、root Gradle alias、报告格式。

### P1：建议本阶段尽快处理

#### P1-1 用真正的 TileRenderer 替换 ASCII 正式主路径

- 问题本质：Phase2 的“Tile 正式路径”目前只存在于文档和 manifest，不在真实玩家界面。
- 影响范围：`client/screen`、`client/render`、manifest、zone tileset 数据、golden。
- 优化目标：让玩家看到的主路径终于与 Phase2 主题一致。
- 具体改法：
  1. 新建 `TileRenderer` 和图层 composer，消费 `terrain / prop / actor / overlay`。
  2. 将 `FoundationGameScreen` 切换到 Tile 渲染；ASCII 降级为 debug/fallback 开关。
  3. 把四个 zone 的 `tilesetKey` 改成 `tileset.ruins / forest_edge / mine / shadow_depths`。
  4. `goldenScreenshot` 改为真实 libGDX offscreen capture，不再使用 `SoftwareGoldenRenderer`。
- 优先级：P1
- 预期收益：视觉识别、区域差异、Boss warning、资源投入都会开始服务正式体验。
- 可能副作用 / 风险：会暴露当前大量 `missing_visual` 占位，短期资源缺口会更明显。
- 需同步项：manifest、截图基线、asset plan、白盒步骤。

#### P1-2 让奖励系统真正改变玩家下一步选择

- 问题本质：当前奖励只是数值补丁，不是决策杠杆。
- 影响范围：`lootProfiles`、`ItemGenerator`、boss rewards、zone/objective runtime、UI 提示。
- 优化目标：每次战斗、探索、Boss 胜利都能明确影响后续打法。
- 具体改法：
  1. 把 `lootProfileId` 接入 runtime，区分普通/精英/Boss 掉落池。
  2. 给每个 profession 至少准备 1~2 件“回答型”奖励，而不是纯白板武器甲。
  3. 让 Boss 奖励从 schema 发放到 inventory，并在结算页显示。
  4. 将 `objectiveSetId` 接到奖励节奏上，形成“完成目标 -> 拿关键奖励 -> 打下一段”的推进结构。
- 优先级：P1
- 预期收益：核心循环中最弱的“奖励 -> 新选择”环节会明显增强。
- 可能副作用 / 风险：平衡会变得更敏感，需要实验室一起跟进。
- 需同步项：item/boss/zone 数据、HUD/结算 UI、`headlessSmoke`、未来 `soloClearLab`。

#### P1-3 拆分 `FoundationGameSession`，把 Phase2 的边界做实

- 问题本质：当前实现把规则、编排、snapshot、UI 支持逻辑塞进同一个类。
- 影响范围：`game/FoundationGameSession.kt`，以及调用它的 `GameModule`、tests、save/snapshot mapper。
- 优化目标：把后续 Phase3 一定会扩大的职责先切开。
- 具体改法：
  1. 先抽出 `TurnSystem / CombatSystem / TalentSystem / InventorySystem / ProgressionSystem`。
  2. `FoundationGameSession` 只保留 orchestration、query 和 lifecycle。
  3. 把 snapshot builder 至少拆成专用 mapper/service，避免 session 继续吞 UI 细节。
- 优先级：P1
- 预期收益：P0 修复与后续 Phase3 扩展的实施成本都会明显下降。
- 可能副作用 / 风险：短期重构量较大，需要靠 existing tests 保驾。
- 需同步项：架构文档、模块边界说明、相关测试。

#### P1-4 收紧资源 DoD，禁止主路径长期吃占位资源

- 问题本质：当前 `key 可解析` 与 `正式资源到位` 被混为一谈。
- 影响范围：visual/audio manifest、asset lint、style lint、client asset audit。
- 优化目标：阻止正式路径继续被 `missing_visual` 和静音 fallback 掩盖。
- 具体改法：
  1. 定义主路径资源白名单：四职业主体、四 zone tileset、核心 skill/status/item icon、Boss warning、主要 ambience。
  2. 对这些 key，禁止 `debug/missing_visual.png` 或 `audio/fallback/silence.ogg`。
  3. 让 lint 区分“允许 fallback 的边缘 key”和“主路径不得 fallback 的 key”。
- 优先级：P1
- 预期收益：形式化资源工作会真正改善玩家体验，而不是只改善构建状态。
- 可能副作用 / 风险：短期会暴露出一批资源补全工作。
- 需同步项：asset/audio plan、manifest、lint 脚本、文档验收口径。

### P2：可以排期但不应忽略

#### P2-1 增加短局目标可视化与失败解释

- 问题本质：当前玩家知道怎么移动和打怪，但不知道这局的阶段目标，也不知道输在哪。
- 影响范围：HUD、summary、game over / victory screen、message log。
- 优化目标：降低理解成本，提升“愿意再来一局”的心理闭环。
- 具体改法：
  1. HUD 增加 route progress / zone objective 小面板。
  2. Game Over 增加最近关键伤害/状态/资源崩盘摘要。
  3. Victory / summary 展示职业、关键奖励、Boss answer 使用情况。
- 优先级：P2
- 预期收益：即使当前内容量不暴增，玩家也会更清楚“这局发生了什么”。
- 可能副作用 / 风险：需要增加 UI 文案和事件汇总结构。
- 需同步项：i18n、summary UI、event/log 汇总。

#### P2-2 修正文档与检查项里对“完成”的定义

- 问题本质：文档本身方向基本正确，但“什么叫实现完成”没有被 gate 具体约束到 runtime 与资源层。
- 影响范围：Phase2 PR 文档、verification checklist、lint/harness 说明。
- 优化目标：明确区分以下概念：
  1. schema skeleton 已存在
  2. runtime 已可用
  3. smoke/harness 已覆盖
  4. 资源已不是 placeholder
- 具体改法：
  1. 在 checklist 中增加“不得只靠 skeleton 过门禁”的条款。
  2. 对职业、zone、monster、item 分别定义 `schema complete` 与 `playable complete`。
- 优先级：P2
- 预期收益：后续阶段不会再重复出现“纸面完成、体验未成”的情况。
- 可能副作用 / 风险：文档会变长，但可维护性更高。
- 需同步项：`docs/phase2`、`docs/INDEX.md`、root alias 说明。

## 7. 可延后到后续阶段的问题

以下问题确实可以留给后续阶段，但当前应有轻量兜底，不应成为回避本阶段缺陷的借口。

| 问题 | 为什么可以延后 | 当前是否需要轻量兜底 |
| --- | --- | --- |
| 最终战斗公式、非线性抗性/穿透、完整 `Power/Save` 体系 | 文档明确归属 Phase3；Phase2 只要求最低可玩的战斗通路。 | 需要。当前至少要把 `DamageType`、资源语义和统一 combat entry 先落主链。 |
| 多阶段 Boss、完整 telegraph DSL、复杂 encounter state machine | Phase3 才正式冻结 `BossEncounter / BossPhaseDef / TelegraphSpec`。 | 需要。Phase2 至少要有真实 simple scripted AI，而不是 `CHASE + Vanguard 四技能优先表`。 |
| 长局世界分支、Quest/RouteReward 正式化、4~6 小时 run | 文档明确是 Phase3 目标。 | 需要。Phase2 仍应完成最小 route/objective/reward 链，不能只剩独立 demo。 |
| ProcGen 深化、Loot 预算生态、隐藏内容、content pack | 明确属于 Phase4。 | 不需要强行提前做，但当前掉落与奖励主链必须先接通。 |
| Tactical AI、Replay/DeathAnalysis、Perf/Soak 完整收口 | 明确属于 Phase5。 | 不需要完整实现，但当前 `longRunLab` 失败和门禁偏弱必须先修。 |

## 8. 最终结论

### 8.1 当前 Phase2 是否达到了文档预期

没有。

更准确地说：

- 文档里“正式合同骨架”的一部分已经做了。
- 但文档里“4 职业短局闭环 + Tile 正式路径 + 正确门禁”的核心承诺没有兑现。

### 8.2 当前版本是否已经“好玩”

还没有。

它具备：

- 可开局
- 可探索
- 可战斗
- 可保存/读档
- 可双语显示

但缺少：

- 稳定成立的职业差异
- 有意义的短局推进目标
- 真实驱动下一局尝试的奖励/构筑变化
- 符合 Phase2 定位的正式表现路径

### 8.3 当前版本是否具备“耐玩雏形”

不具备。

现在的重玩价值更多来自“换个 seed 再打一遍同样的基础模板”，不是来自：

- 换职业换打法
- 换路线换风险
- 换奖励换构筑

### 8.4 是否适合进入下一阶段开发

不适合直接进入 Phase3。

如果现在直接进入：

- Phase3 会被迫一边做深层战斗/职业树，一边补回 Phase2 本该完成的资源/路线/职业/runtime 地基。
- 这会显著放大返工和系统耦合成本。

### 8.5 进入下一阶段前必须先补的内容

必须先补以下四项：

1. 运行时合同回到真实 Phase2 基线：`1000` 能量、资源池、统一 talent runtime、正确 HUD 资源语义。
2. 四职业从 schema 补成最小可玩，并具备真实 build 分化。
3. 4 zone 从独立 demo 补成一条短局 route，并让奖励/目标进入运行时。
4. 重建 Phase2 门禁：补 `soloClearLab`，修正 `headless/client/golden/longRunLab` 的验证口径。

一句话结论：

当前仓库更像“披着 Phase2 文档外衣的 Phase1.5”，不是可以收口的 Phase2 完成态。

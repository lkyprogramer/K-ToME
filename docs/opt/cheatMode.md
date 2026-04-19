# Validation Mode / Cheat Mode：PR 级开发文档

## 1. 目标与边界

### 1.1 目标

本方案要把当前的“作弊 / 金手指 / 验证加速”想法，收敛成一条**正式、可维护、只服务白盒验证与 `Computer Use` 的 Validation Mode 路径**。

它要解决的不是“让玩家玩得更爽”，而是以下三个工程问题：

1. `docs/phase4` 里仍保留多条必须人工操作的白盒验证条件，当前靠正常游玩逐条触发成本过高。
2. 仓库内已经存在不少 `FoundationGameSession.automation*` 运行时辅助能力，但它们仍然主要被测试和 harness 使用，没有正式、安全、键盘优先的人机入口。
3. 如果直接把各种作弊塞进 `client` 热键或做一个任意命令控制台，最终很容易破坏 `core / game / client` 边界，变成第二套规则路径。

本文的目标是把这条能力拆成**严格串行的多个 PR**，每个 PR 都有清晰的代码范围、验证面、白盒收益和退出标准。

### 1.2 非目标

本方案**不做**以下事情：

1. 不做普通玩家可见的“官方作弊模式”。
2. 不做运行时脚本宿主、自由文本控制台、Lua、表达式求值器或任意命令执行。
3. 不做新的 save schema、profile schema 或 replay schema。
4. 不把测试里的任意 world mutation 直接暴露给 `client`。
5. 不替代 `terrainInteractionBatch`、`bossHarness`、`hiddenContentHarness`、`contentPackHarness` 等既有自动化验证任务。
6. 不把 Validation Mode 做成新的规则真源；正式规则仍只允许在 `core` / `game` 中定义。
7. 不为了“方便跳关”而改写 `BossEncounterDef` phase graph、`LootBudget`、`SearchAction`、`ContentPackManifest` 等已冻结合同。

### 1.3 当前仓库约束

实施时必须遵守以下已经核实的仓库事实：

1. `GameApp.startNewGame()` 当前硬编码走 `AvailabilityContext.PLAYER_CREATION`；Validation Mode 必须新增显式入口，不能偷偷劫持普通新局。
2. `MainMenuScreen` / `MainMenuController` 当前只有 `New Game / Continue / Exit` 三个可选菜单项；语言切换通过 `L` 热键触发 `ToggleLocale`。Validation Mode 需要新入口与新控制流，并同步回归主菜单光标与热键逻辑。
3. `FoundationGameConfig` 已经正式支持：
   - `seed`
   - `zoneId`
   - `playerProfessionId`
   - `playerRaceId`
   - `floor`
   - `maxFloor`
   - `zoneRoute`
   - `routeIndex`
   - `bossVariantSelectionMode`
   - `preferredBossVariantId`
   因此大量白盒验证预设应优先走“启动预设”而不是局内重建世界。
4. `GameModule.newFoundationSession(...)` 已经接受 `profile` 与 `availabilityContext`；Validation Mode 应利用已有正式边界，而不是再造平行 session 工厂。
5. `ClassAvailabilityResolver` 已经区分 `PLAYER_CREATION / DEV_LAB / WHITE_BOX`；其中 `DEV_LAB` 与 `WHITE_BOX` 当前在 resolver 中行为等价。Validation Mode 仍应明确落在 `WHITE_BOX`，这是为了白盒命名与未来差异化预留，而不是因为今天已经有独立规则分支。
6. `FoundationGameSession` 已存在一批与白盒验证高度相关的运行时辅助能力，例如：
   - `automationMovePlayerTo`
   - `automationStairPoint`
   - `automationBossPoint`
   - `automationInteractablePoint`
   - `automationPendingObjectiveInteractablePoint`
   - `automationSearchPointForBinding`
   - `automationHiddenEntrancePointForBinding`
   - `automationSecretRewardPointForBinding`
   - `automationSecretReturnPointForBinding`
   - `automationKillFirstExistingEliteMonster`
   - `automationForceKillFirstEliteMonster`
   - `automationForceDefeatPlayer`
   - `automationSpawnAndKillEliteMonsterForTest`
   - `automationTerrainOverrideAt`
   - `automationSetTerrainOverride`
   - `automationVisibleShopOffers`
   - `automationCanPurchaseShopOffer`
7. `TileRenderModel` / `AsciiRenderModel` 的 inspect/sidebar 已能显示大量白盒信息，例如：
   - terrain rule / remaining turns
   - elite mutation 名称、icon、summary
   - boss variant 信息
   - prop state / item detail
   因此 Validation Mode 的 UI 重点应是“控制入口 + 验证摘要”，而不是重复发明第二套解释界面。
8. 当前 profile persistence 允许失败后退化为“不写回进度，仅提示 notice”；Validation Mode 不应依赖正式 profile 目录才能可用。

### 1.4 文档映射

本开发文档与现有权威文档的关系固定如下：

1. `AGENTS.md` 继续定义仓库级模块边界、验证纪律和高风险红线。
2. `docs/2026-04-04-unified-white-box-verification-framework.md` 继续定义统一白盒框架、artifact/report 合同与人工一致性策略。
3. `docs/phase4/2026-03-13-phase4-verification-checklist.md` 继续定义 `Phase 4` 的人工白盒门槛与自动化入口。
4. `docs/phase4/2026-03-13-phase4-pr-02` ~ `pr-09` 继续定义各领域白盒要求与 contract 边界。
5. 本文只负责“如何实现一条正式的 Validation Mode 来加速这些白盒验证”，不重写现有 `Phase 4` contract。

### 1.5 Contract 边界

以下结论在整套 PR 中都必须保持稳定：

1. Validation Mode 是**额外运行路径**，不是正式产品模式。
2. 作弊动作必须是 **typed command**，不能是自由文本指令。
3. 作弊动作的执行 owner 固定在 `game`，`client` 只做输入与展示。
4. 启动预设和局内动作是两层概念：
   - 启动预设负责 `seed / zone / floor / route / boss variant / content pack`
   - 局内动作负责导航、恢复、遭遇控制、少量验证辅助
5. 任何会改变规则状态的能力都必须经过 `FoundationGameSession`，不允许直接由 `client` 修改 ECS / world。
6. `SearchAction`、reward、boss variant、terrain interaction 仍必须沿用正式 runtime path；Validation Mode 只能“更快到达 / 更快触发 / 更快观察”，不能改其正式语义。
7. Validation save 只允许服务本地单机续局，不进入 `replay`、`soloClearLab`、`InputReplayFullGameLoopTest` 或任何正式回归任务；Validation action 执行后的 save 在回放语义上视为非正式输入。
8. v1 的 Validation 可用性信号固定不通过 `RenderSnapshot` / `core.snapshot` 回流；`InputHandler` 通过构造参数或等价显式注入拿到 `validationEnabled`，避免为 UI gating 污染正式 snapshot contract。
9. `ValidationCapabilitySet` 的粒度固定为 action family 级：
   - `Restart`
   - `Travel`
   - `Recovery`
   - `Encounter`
   - `Terrain`
   - `RewardAndItem`
   - `Discovery`
   v1 默认全部开启；若个别高风险动作需要更严格限制，只允许在 family 内做显式 guard，不再叠第二层 capability contract。
   Validation 面板中的 action blocks 必须与这 7 个 family 一一对齐；摘要信息只作为独立 header，不占 family 槽位。
10. Validation path 必须同时具备两种入口：
   - 键盘 + overlay 的本地人工入口
   - `PlayerCommand.Validation(...)` 的 programmatic 入口
   后者从 `PR-03` 起即视为正式能力，不要求额外 UI。
11. Validation session 结束时不回写 `ProfileRunSummary`；即使存在独立 validation profile 目录，也默认走显式 no-op，避免把验证局统计误当成正式进度。
12. `Restart` family 是唯一允许从局内动作反向触发启动层重建的 action。它的语义等价于“带当前 preset 重新走一遍 Validation session 启动链”，不继承当前世界状态，也不做原地 reseed。

### 1.6 全局阶段门禁

本开发文档采用**严格 PR 串行推进**策略。

固定规则：

1. 任一时刻只允许实现当前 PR，不并行推进下一 PR。
2. 当前 PR 的必跑命令、必看产物、必过退出标准全部满足后，才允许进入下一 PR。
3. 任一 PR 若发现 contract 选型不成立，必须在当前 PR 回退并重写当前方案，不得把“后续 PR 再修”当成默认路径。
4. 所有 Gradle 命令串行执行，不允许并行跑多个 Gradle 进程。

---

## 2. 设计总览

### 2.1 设计结论

最终设计固定为一条**独立 Validation session + typed validation action + 键盘优先面板**的正式路径。

高层结论如下：

1. 主菜单新增 `Validation Mode`，而不是隐藏热键偷偷开启。
2. Validation session 使用独立 save/profile 根目录，默认固定为：

```text
~/.ktome/validation
```

3. Validation session 使用 `AvailabilityContext.WHITE_BOX` 启动。
4. 启动预设不继续膨胀 `FoundationGameConfig` 的职责；Validation 特有能力统一放进独立 config / capability 模型。
5. 局内作弊动作统一通过：
   - `PlayerCommand.Validation(...)`
   - `sealed interface ValidationAction`
6. `ValidationCapabilitySet` 在 v1 固定采用 family 级布尔开关，不做 action 级 capability 树。
7. `client` 新增 Validation overlay，但复用现有 inspect/sidebar 白盒信息，不重造第二套解释 UI。
8. Validation Mode 同时支持 programmatic command path 与 overlay path；overlay 是人机入口，不是唯一入口。
9. 第一版虽按“可扩展的通用框架”设计，但首批能力必须优先覆盖 `Phase 4` 的人工白盒主路径，而不是做一个泛化却缺实际验证价值的 debug shell。

### 2.2 架构摘要

```text
client
  GameApp
    ├─ validationEnabled / validation profile-save wiring
    ├─ MainMenuScreen / MainMenuController
    ├─ ValidationSetupScreen / Controller
    ├─ InputHandler 接受 `validationEnabled` 构造参数
    ├─ TileRenderModel / AsciiRenderModel
    └─ ValidationCommandSource / overlay 衔接层

game
  ValidationPaths
    └─ 统一持有 validation save/profile 根目录常量

  ValidationSessionOptions
    ├─ ValidationPreset
    ├─ ValidationCapabilitySet
    ├─ ValidationAction
    └─ ValidationSummarySnapshot

  GameModule
    └─ newValidationSession(...)

  FoundationGameSession
    ├─ perform(PlayerCommand.Validation(...))
    ├─ validation gating
    ├─ runtime action executors
    └─ summary snapshot builder

core
  AvailabilityContext.WHITE_BOX
    └─ 继续作为正式 selection gate，不引入新枚举语义；v1 不为 Validation Mode 额外扩 snapshot contract
```

### 2.3 不允许的 Shortcut

1. 不允许新增 `CheatManager` / `DebugManager` / `ValidationUtils` 这种无 owner 的业务类型。
2. 不允许把 validation-enabled 做成零散 `Boolean` 参数矩阵，污染 `GameApp`、`GameModule`、`FoundationGameSession` 调用链。
3. 不允许 `client` 直接拿 `automationWorld()` 或 world component 做裸写入。
4. 不允许新增独立规则 UI 文案表，绕过正式 i18n / render snapshot。
5. 不允许局内随意切换 zone / floor / content pack 并重建世界；这类变化统一走 “restart into preset”。
6. 不允许让普通 `New Game / Continue` 共享同一个 validation 持久化目录。
7. 不允许把 `Phase 4` 的手工白盒步骤私有化成作者记忆中的快捷键组合；所有高频动作都必须在 UI 中可见。

---

## 3. PR 拆分总览

| PR | 主题 | 主要模块 | 目标产出 | 进入下一 PR 的门槛 |
| --- | --- | --- | --- | --- |
| `PR-01` | Validation session boundary + 独立持久化 | `client`, `game` | 独立 Validation 启动路径、独立 save/profile 根目录、`WHITE_BOX` availability 上下文 | Validation session 与普通 session 完全隔离，正常新局不受影响 |
| `PR-02` | Validation setup flow + preset contract | `client`, `game` | 主菜单 `Validation Mode`、Validation 配置页、`ValidationPreset` / `ValidationSessionOptions` | 可从 UI 明确启动目标验证场景，不引入 config 第二真源 |
| `PR-03` | Typed validation action runtime | `game` | `PlayerCommand.Validation`、`ValidationAction`、session 内动作门禁与执行器 | 普通 session 拒绝动作，Validation session 能稳定执行正式辅助动作 |
| `PR-04` | 局内 Validation overlay + 键盘代理 UX | `client` | `UiMode.VALIDATION`、面板、快捷键、高频动作编排、摘要展示 | `Computer Use` 可在不依赖作者记忆的情况下使用主要动作 |
| `PR-05` | Phase 4 白盒映射、验证与文档收口 | `client`, `game`, docs | preset 与 `Phase 4` 白盒条件一一映射、白盒步骤文档、定向 smoke/test 收口 | `docs/phase4` 人工白盒主路径都能借 Validation Mode 明显提速 |

补充规则：

1. `PR-01` 之前不实现任何局内作弊动作。
2. `PR-02` 之前不引入 `ValidationPreset`。
3. `PR-04` 之前不开放 `F9` 面板或任何局内 action UI。
4. `PR-04` 不改自动化 harness 的 authority，只接 Validation UI。
5. `PR-05` 才统一回写 `docs/phase4` 和使用文档，不在前面 PR 提前扩写最终手册。

---

## 4. 全局开发与验证纪律

### 4.1 环境前置

每次执行 Gradle 验证前先运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

### 4.2 统一验证纪律

1. 所有 Gradle 命令串行执行。
2. 任何 non-trivial Kotlin 结构改动都必须补跑：

```bash
./gradlew maintainabilityLint
```

3. 每个 PR 至少补跑：

```bash
./gradlew verifyChanged
```

4. 命中 `client` 入口、输入、渲染、菜单时，必须补跑：

```bash
./gradlew clientSmoke
```

5. 命中 `Phase 4` 相关验证映射时，按改动范围补跑：

```bash
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew hiddenContentHarness
./gradlew contentPackHarness
```

6. 任何新增 Validation action 都必须同时具备：
   - `game` 层行为测试
   - `client` 层至少一条可触达 smoke 或输入测试
   - 拒绝路径测试
7. 任何新增 `ui.validation.*`、controls 文案或 Validation setup 文案，都必须在 `zh-CN.json` 与 `en-US.json` 同次提交；缺一即视为 PR 未完成。
8. 若某个 PR 引入了 `maintainability` baseline 漂移，提交说明中必须解释 delta 来源；不允许把 baseline 漂移当作“顺手更新”的无说明噪音。

### 4.3 统一通过标准

任一 PR 结束时，必须同时满足：

1. 代码编译通过。
2. 定向测试与 smoke 通过。
3. 没有破坏普通 `New Game / Continue` 正常路径。
4. Validation path 的 save/profile 与普通路径隔离成立。
5. 文档中该 PR 的退出标准全部满足。
6. 若发现 `automationWorld()` 被暴露到 `client`、出现 ECS 裸写 UI 路径、或出现自由文本 debug command，当前 PR 直接视为阻塞，不允许留到下一 PR 再修。
   这条红线默认依赖 code review 与现有 `maintainabilityLint` / 相关静态规则识别；若未来有可稳定静态判定的规则，应优先纳入同一治理面，而不是保留成纯人工约束。

---

## 5. PR-01：Validation Session Boundary + 独立持久化

### 5.1 目标

建立 Validation Mode 的正式边界，先解决“它是什么、如何与普通游玩隔离”的问题。

`PR-01` 的完成态是：

1. 仓库存在一条独立的 Validation session 创建路径。
2. Validation session 使用独立 save/profile 根目录。
3. Validation session 固定走 `AvailabilityContext.WHITE_BOX`。
4. 普通 `New Game / Continue` 路径完全不受影响。

### 5.2 非目标

`PR-01` **不做**：

1. 不新增主菜单入口。
2. 不做 Validation 配置页。
3. 不开放任何 Validation action。
4. 不做 Validation overlay。

### 5.3 设计约束

#### 5.3.1 Validation session 配置模型

新增独立的 Validation session 配置模型，建议命名为：

```text
game/src/main/kotlin/com/ktome/game/validation/ValidationSessionOptions.kt
```

固定原则：

1. `FoundationGameConfig` 继续只承载正式会话世界参数。
2. Validation 特有能力如 preset、capabilities、restart policy、summary 开关，不塞进 `FoundationGameConfig`。
3. `GameModule` 新增专用启动边界，例如：
   - `newValidationSession(...)`
   - 或 `newFoundationSession(..., validationOptions = ...)`
4. 若采用后者，也必须保证 validation 不是一堆可空参数矩阵。

#### 5.3.2 独立持久化根目录

Validation path 默认根目录固定为：

```text
~/.ktome/validation
```

包含：

```text
~/.ktome/validation/save
~/.ktome/validation/profile
```

要求：

1. 普通 `~/.ktome` 与 Validation path 互不读写。
2. Validation `Continue` 只读取 validation save。
3. Validation profile 失败时，只影响 validation path，不污染正式 profile notice。
4. 路径常量统一收敛到 `ValidationPaths` 或等价 owner 类型，不允许在 `GameApp`、`GameModule`、测试里各写一份 `~/.ktome/validation` 字面量。
   更精确地说，正式 `~/.ktome/save` 与 `~/.ktome/profile` 不读写 validation path 下的任何目录，反之亦然。

#### 5.3.3 AvailabilityContext

Validation path 固定使用：

```text
AvailabilityContext.WHITE_BOX
```

原因：

1. 文义上最贴近白盒验证。
2. 虽然它当前与 `DEV_LAB` 行为等价，但命名上更贴近白盒验证。
3. 为未来把 `WHITE_BOX` 与 `DEV_LAB` 行为拆开保留空间。
4. 与 `docs/phase4` 的白盒验证语义一致。
5. 若 validation profile 仍是空 `ProfileData()`，则 `LOCKED` 职业/种族在 `WHITE_BOX` 下依旧保持 `LOCKED`；v1 不在启动链里额外预填 unlock 状态。若后续需要“全职业可选”，应单独立项，不在本文顺手扩 scope。

#### 5.3.4 Outcome / Replay 边界

固定规则：

1. Validation session 的 save 只服务 validation `Continue`。
2. Validation session 的 `ProfileRunSummary` 默认不回写 profile。
3. Validation save 不进入 replay / regression / harness 正式输入集合。
4. 目录隔离已经天然保证既有 harness / replay 路径不会读取 validation 目录；v1 不再为此新增第二层过滤逻辑。
5. 若未来要求 Validation save 与 replay 严格兼容，必须单独立项处理 schema / replay contract；不在本文范围内顺手解决。

### 5.4 代码范围

#### `client` 新增 / 修改

- `client/src/main/kotlin/com/ktome/client/GameApp.kt`
- `client/src/test/kotlin/com/ktome/client/GameAppLifecycleTest.kt`

#### `game` 新增 / 修改

- `game/src/main/kotlin/com/ktome/game/GameModule.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameConfig.kt` 仅在确有必要时最小改动
- `game/src/main/kotlin/com/ktome/game/validation/ValidationPaths.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationSessionOptions.kt`
- `game/src/test/kotlin/com/ktome/game/GameModuleTest.kt`

### 5.5 必须实现的行为

1. `GameApp` 能创建独立的 validation `SaveManager` 与 `ProfileManager`。
2. Validation session 的 `profileManager` / `saveManager` 路径与普通路径完全分离。
3. Validation session 启动时使用 `WHITE_BOX`。
4. 普通 session 仍保持 `PLAYER_CREATION`。
5. 在 `PR-01` 阶段，Validation session 只允许通过 `GameModule.newValidationSession(...)` 在测试或内部 wiring 中构造；client main 代码路径中不允许出现任何可达的 Validation UI 入口。
6. `GameApp` 以稳定字段形式持有 validation `SaveManager` / `ProfileManager`，而不是按调用点临时构造。
7. Validation session 的 `ProfileRunSummary` / profile progression 更新默认 no-op。

### 5.6 必须新增的测试

1. `GameModuleTest`
   - Validation session 使用 `WHITE_BOX` 时能接受 white-box 可玩职业/种族。
   - 普通 session 保持原有 `PLAYER_CREATION` 约束。
2. `GameAppLifecycleTest`
   - Validation 与普通 session 的 save/profile 根目录分离。
   - Validation save 不影响普通 `Continue` 可用性。
   - 即使存在 validation save，正式 `Continue` 仍保持对普通目录的唯一判断。
   - Validation outcome 不回写 profile progression。
   - 使用 `@TempDir` 或等价临时目录驱动 validation path 测试，不向真实 `~/.ktome/validation` 落测试产物。

### 5.7 必跑命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests "com.ktome.game.GameModuleTest"
./gradlew :client:test --tests "com.ktome.client.GameAppLifecycleTest"
./gradlew maintainabilityLint
./gradlew verifyChanged
./gradlew clientSmoke
```

### 5.8 必看结果

1. 普通 `.ktome` 目录下没有被 validation path 覆盖的新 save/profile。
2. Validation session 可独立启动并可独立持久化。
3. 普通 `Continue` 与 Validation `Continue` 没有交叉误读。

### 5.9 退出标准

1. Validation session 已有正式边界。
2. save/profile 根目录隔离成立。
3. `WHITE_BOX` availability 语义生效。
4. 普通新局与继续游戏路径零回归。

---

## 6. PR-02：Validation Setup Flow + Preset Contract

### 6.1 目标

把 Validation path 从内部能力升级为正式、可选、可配置的启动流程。

`PR-02` 的完成态是：

1. 主菜单新增 `Validation Mode` 入口。
2. 存在最小 Validation 配置页。
3. `ValidationPreset` 与 `ValidationSessionOptions` 正式冻结第一版 contract。

### 6.2 非目标

`PR-02` **不做**：

1. 不实现局内作弊动作。
2. 不实现 Validation overlay。
3. 不做 `Phase 4` 白盒映射全量文档收口。

### 6.3 设计约束

#### 6.3.1 Preset 列表

v1 preset 固定为：

1. `MAPGEN_DIFF`
2. `HIDDEN_CONTENT`
3. `TERRAIN_INTERACTION`
4. `ELITE_MUTATION`
5. `BOSS_VARIANT`
6. `LOOT_LAB`
7. `CONTENT_PACK`
8. `CUSTOM`

固定约束：

1. v1 preset 刻意对齐 `docs/phase4` 白盒主路径，不额外扩成 `Phase 3/5` 全覆盖清单。
2. 其它阶段若需要快捷入口，默认先通过 `CUSTOM` 承接；只有形成独立稳定需求后，再单独追加 phase-specific preset。

#### 6.3.2 配置页输入范围

配置页支持：

1. preset
2. profession（映射到 `playerProfessionId`）
3. race（映射到 `playerRaceId`）
4. seed
5. zone
6. floor
7. route / routeIndex
8. boss variant mode / preferred variant
9. sample content pack 开关

固定规则：

1. `CUSTOM` 允许自由选。
2. 其它 preset 可以锁定默认 zone / floor / variant / content pack，但仍允许最小可控修改。
3. 配置页要避免膨胀成通用编辑器；只保留验证真正需要的字段。

#### 6.3.3 UI 形态

新增 screen 而不是继续塞进 `MainMenuScreen` 单页。

建议文件：

- `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupScreen.kt`
- `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupController.kt`

原因：

1. 主菜单继续保持薄壳。
2. Validation setup 与普通 player creation 的控制流分离。
3. 方便后续加 preset summary 与 white-box hints。

附加规则：

1. Validation setup 的键盘语义必须与主菜单保持同构：
   - 方向键移动
   - `Enter` 确认
   - `Esc` 返回
2. v1 不引入“仅 dev build 显示 Validation Mode”的 build 开关；Validation 入口默认在所有 build 中一致可见，避免文档与实际发行构建分叉。
3. v1 的 Validation setup 只承诺键盘交互，不对手柄 / 触控体验作额外保证。

### 6.4 代码范围

#### `client` 新增 / 修改

- `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt`
- `client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt`
- `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupScreen.kt`
- `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupController.kt`
- `client/src/main/kotlin/com/ktome/client/GameApp.kt`
- `client/src/test/kotlin/com/ktome/client/GameAppLifecycleTest.kt`
- `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`

#### `game` 新增 / 修改

- `game/src/main/kotlin/com/ktome/game/validation/ValidationPreset.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationSessionOptions.kt`
- `game/src/test/kotlin/com/ktome/game/GameModuleTest.kt`

#### i18n

- `game/src/main/resources/i18n/zh-CN.json`
- `game/src/main/resources/i18n/en-US.json`

### 6.5 必须实现的行为

1. 主菜单出现显式 `Validation Mode`。
2. Validation setup 页面能构造第一版 `ValidationSessionOptions`。
3. `CUSTOM` 与预设模式都能正常启动 validation session。
4. 若启用 sample content pack，则通过正式 `ContentPackSelection` 进入，不新增第二套 pack 开关逻辑。
5. UI 上能直接看出当前 preset 对应的主要验证目标。
6. UI 上必须显式显示 active pack ids 或 pack summary，不允许只靠一个 checkbox 暗示当前 pack 状态。

### 6.6 必须新增的测试

1. `GameAppLifecycleTest`
   - 主菜单进入 Validation setup 的流程成立。
   - 从 Validation setup 启动 session 后走 validation 持久化根目录。
   - 主菜单新增第四个可选项后，原有 `Continue / Exit / L` 热键逻辑无回归。
2. `ClientSmokeHarnessTest`
   - Validation setup screen 可渲染。
   - 典型 preset 可启动进入游戏。
3. preset -> `ValidationSessionOptions` 字段断言测试
   - `MAPGEN_DIFF` 与 `CUSTOM` 至少在 `zoneId / floor / routeIndex / bossVariantSelectionMode` 上存在可断言差异。

### 6.7 必跑命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.GameAppLifecycleTest"
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew maintainabilityLint
./gradlew verifyChanged
./gradlew clientSmoke
```

### 6.8 必看结果

1. 主菜单中 `Validation Mode` 可见且可进入。
2. Validation setup 中的 preset、seed、zone 等配置真实生效。
3. 开启 sample pack 时不是假 UI，而是走正式 content pack 选择链路。

### 6.9 退出标准

1. 主菜单到 Validation setup 到 Validation session 的链路完整可走通。
2. preset contract 已冻结第一版。
3. Validation setup 没有把普通 main menu 污染成复杂状态机。

---

## 7. PR-03：Typed Validation Action Runtime

### 7.1 目标

把 Validation Mode 的“作弊能力”收敛为正式、可测试、可拒绝的 typed runtime action。

### 7.2 非目标

`PR-03` **不做**：

1. 不做完整 overlay UI。
2. 不做 `Phase 4` 最终文档回写。

### 7.3 设计约束

#### 7.3.1 Command 形态

命令形态固定为：

```text
PlayerCommand.Validation(action)
sealed interface ValidationAction
```

不允许：

1. `PlayerCommand.Debug(String)`
2. `eval`
3. 任意 key-value 参数字典
4. 零散布尔开关驱动的行为分支

#### 7.3.2 Action 家族

首批 action 固定分为以下类别：

1. `Restart`
   - same preset restart
   - next-seed restart
2. `Travel`
   - teleport to stair up/down（优先复用 `automationStairPoint(...)`）
   - teleport to boss（优先复用 `automationBossPoint()`）
   - teleport to pending objective（优先复用 `automationPendingObjectiveInteractablePoint()`）
   - teleport to interactable（优先复用 `automationInteractablePoint(id)`）
   - teleport to inspect cursor（action executor 在 `PR-03` 就位；UI 入口在 `PR-04` 接入，便于与现有 inspect 能力协作）
   - teleport to search binding / hidden entrance / secret reward / secret return（优先复用现有 binding/secret 查询 helper）
3. `Recovery`
   - full heal
   - restore all player resource pools
   - reset talent / inscription cooldowns
   - grant shards
   - grant stat / talent points
4. `Encounter`
   - spawn elite near player（`PR-03` 需在 `FoundationGameSession` 内新增纯 spawn 的 owner 方法；当前仓库只有 spawn+kill 组合 helper，不直接满足）
   - kill nearest hostile
   - kill nearest elite（优先复用 `automationForceKillFirstEliteMonster()` 或等价 owner 方法）
   - kill active boss
   - force player defeat（优先复用 `automationForceDefeatPlayer()`）
5. `Terrain`
   - set terrain override at inspect cursor / target point（复用 `automationTerrainOverrideAt(...)` / `automationSetTerrainOverride(...)`）
   - clear terrain override
6. `RewardAndItem`
   - trigger synthesized reward presentation（合成一条 reward 并走正式 presentation pipeline，不直接裸塞背包）
   - spawn item by schema id
7. `Discovery`
   - execute 正式 `SearchAction`
   - reveal helper limited to typed binding target

#### 7.3.3 Gating 规则

固定规则：

1. 普通 session 一律拒绝 `PlayerCommand.Validation`。
2. Validation session 只允许执行被 capability set 明确开启的 action family。
3. 被拒绝的 action 必须写出明确日志，不允许静默失败。
4. `Travel` / `Terrain` / `RewardAndItem` 等动作，若目标不存在，也必须给出可见反馈。

#### 7.3.4 不允许直接裸写 world

即使测试中已有 world mutation，也不能直接把这套方式暴露给 UI。

要求：

1. Validation action 统一封装在 `FoundationGameSession` 内部执行。
2. 可复用现有 `automation*` 查询 helper。
3. 对真正的状态变更，必须经过 session owner 逻辑，不把 ECS 裸写漏到 `client`。
4. Validation action executor 禁止直接访问 `automationWorld()` 返回的 `World` 做业务写入；若现有 owner 方法不够，先在 `FoundationGameSession` 内补 owner 方法，再由 action 调用。

### 7.4 代码范围

#### `game` 新增 / 修改

- `game/src/main/kotlin/com/ktome/game/GameView.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationAction.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationCapabilitySet.kt`
- `game/src/main/kotlin/com/ktome/game/validation/ValidationSummarySnapshot.kt`
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
- `game/src/test/kotlin/com/ktome/game/GameModuleTest.kt`

#### `core`

v1 锁死 `ValidationSummarySnapshot` 保持在 `game.validation` 包下，不进入 `core.snapshot`。若未来确实需要跨 boundary 被 tools 或正式 snapshot 消费，必须单独起 PR 处理。

### 7.5 必须实现的行为

1. `PlayerCommand.Validation` 已进入 `perform(...)` / `executePlayerCommand(...)` 的正式分支。
2. 普通 session 拒绝该命令。
3. Validation session 至少支持每个 action 家族的一条正式路径。
4. `SearchAction` 必须复用正式搜索行为，不新建“假 reveal”主路径。
5. reward 必须尽量复用正式 reward pipeline，不直接裸塞背包作为主验证路径。

### 7.6 必须新增的测试

1. 普通 session 调用 validation command 被拒绝。
2. Validation session 的 `Travel` 能落到合法目标点。
3. `Recovery` / `Encounter` / `Terrain` / `Discovery` 能稳定改变权威状态。
4. 失败路径有明确日志反馈。
5. 至少一组 capability gating 矩阵测试成立：
   - 普通 session 拒绝
   - validation session 接受
   - 目标不存在时失败并给出反馈
   - capability family 关闭时拒绝
6. programmatic 入口闭环：至少一条测试必须显式通过 `FoundationGameSession.perform(PlayerCommand.Validation(...))` 触发，不经过 `InputHandler` 或 `CommandSource`。
7. save/load 代表性恢复测试：至少一条 validation session 在执行过一条 `Recovery` 与一条 `Encounter` action 后，save/load 仍能恢复到保存时的状态；`ProfileRunSummary` 继续保持不回写。

### 7.7 必跑命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew :game:test --tests "com.ktome.game.GameModuleTest"
./gradlew maintainabilityLint
./gradlew verifyChanged
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew hiddenContentHarness
```

### 7.8 必看结果

1. 普通 session 误触 validation command 时不会改变状态。
2. Validation session 下 travel / recovery / encounter / discovery 至少各有一条动作真实生效。
3. 没有新增第二套搜索、奖励、boss 或 terrain 规则路径。

### 7.9 退出标准

1. typed action contract 已冻结第一版。
2. Validation runtime 具备主要作弊能力，但仍保持 owner 清晰。
3. 普通 session 零回归。

---

## 8. PR-04：局内 Validation Overlay + 键盘代理 UX

### 8.1 目标

给 `Computer Use` 和人工验证者一个正式、可见、可读、键盘优先的局内控制面板。

### 8.2 非目标

`PR-04` **不做**：

1. 不继续新增新的 action 家族。
2. 不修改 `Phase 4` harness 逻辑。

### 8.3 设计约束

#### 8.3.1 UI 形态

新增：

```text
UiMode.VALIDATION
```

默认热键：

```text
F9 打开/关闭 Validation 面板
```

补充规则：

1. 以当前仓库状态看，`F9` 尚未被占用；`PR-04` 开工前仍必须再 grep 一次确认无新冲突。
2. 若 `F9` 在实施时被占用，则同一 PR 内直接切到 `F12`，不保留双热键兼容路径。
3. `validationEnabled` 的生命周期与 `InputHandler` 实例强绑定：`InputHandler` 自身不保留可变的 `validationEnabled`。`GameApp` 在普通 session / validation session 启动路径分别构造专属 `InputHandler` 实例；session 结束回到主菜单时旧实例被释放，不允许通过 setter 在运行时切换同一实例的 validation 状态。

面板必须满足：

1. 方向键 / `Enter` 可完整操作。
2. 可见当前选中项。
3. 能显示动作执行后的结果摘要。
4. 不依赖鼠标。
5. 是否允许打开 Validation overlay，固定由 `validationEnabled` 显式注入决定，不通过 `RenderSnapshot` 推断。

#### 8.3.2 面板信息架构

面板固定包含：

1. 顶部 `Summary` header（只承载摘要信息，不占 action family 槽位）
2. `Restart`
3. `Travel`
4. `Recovery`
5. `Encounter`
6. `Terrain`
7. `Reward & Item`
8. `Discovery`

要求：

1. 一级块不要过多。
2. 7 个 action block 必须与 `ValidationCapabilitySet` 的 7 个 family 一一对齐；`Summary` 只能作为顶部摘要，不得挤占 action block 槽位。
3. 同块内动作尽量按真实验证流程排序，而不是按底层技术实现排序。
4. 高价值动作允许数字键快捷触发，但仍必须能在面板中被读到。
5. `teleport to inspect cursor`、`set terrain override at inspect cursor` 作为高频动作应直接出现在面板主路径，而不是藏在二级菜单。

#### 8.3.3 摘要信息

面板必须显示最低限度的 Validation 摘要：

1. preset
2. seed
3. zone
4. floor
5. active pack ids（无 active pack 时显示 `(none)` 或等价占位，不留空字符串）
6. boss variant
7. 最近一次 validation action 结果

#### 8.3.4 渲染路径

命中 `UiMode` 新枚举时，必须同步收口：

1. `InputHandler`
2. `TileRenderModel`
3. `AsciiRenderModel`
4. 任何根据 `UiMode` 分支标题或 controls 文案的渲染代码

不允许只改 tile path，留下 ASCII / smoke / test 编译缺口。

### 8.4 代码范围

#### `client` 新增 / 修改

- `client/src/main/kotlin/com/ktome/client/GameApp.kt`
- `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
- `client/src/main/kotlin/com/ktome/client/input/CommandSource.kt`
- `client/src/main/kotlin/com/ktome/client/input/ValidationCommandSource.kt`
- `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
- `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
- `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
- `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`
- `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt`
- `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`

#### i18n

- `game/src/main/resources/i18n/zh-CN.json`
- `game/src/main/resources/i18n/en-US.json`

### 8.5 必须实现的行为

1. Validation session 中可按 `F9` 打开面板。
2. 普通 session 中 `F9` 不应暴露 validation 面板。
3. 面板能按块浏览并触发 action。
4. 面板显示 validation 摘要与最近结果。
5. inspect/sidebar 已有白盒信息继续可见，不被 validation 面板破坏。

### 8.6 必须新增的测试

1. `InputHandlerTest`
   - Validation session 才能进入 `UiMode.VALIDATION`
   - 普通 session 不进入
   - `validationEnabled = false` 时，即使收到 `F9` 也不会进入 `UiMode.VALIDATION`
   - Validation overlay 打开时，方向键/移动键只作用于面板导航，不同时下发 `PlayerCommand.Move`；关闭面板后 Map 输入恢复
   - 面板导航与触发命令成立
2. `ClientSmokeHarnessTest`
   - Validation overlay 可渲染
   - 打开面板后至少执行一条动作

### 8.7 必跑命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.input.InputHandlerTest"
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew maintainabilityLint
./gradlew verifyChanged
./gradlew clientSmoke
./gradlew goldenScreenshot
```

### 8.8 必看结果

1. `F9` 只在 Validation session 有效。
2. `Computer Use` 能通过键盘可靠地浏览和执行主要动作。
3. tile 与 ascii 渲染路径都已收口。
4. 若 `goldenScreenshot` baseline 发生变化，必须保留 diff 证据并在 PR 说明里解释是 Validation UI 引入的预期变化。

### 8.9 退出标准

1. Validation overlay 已具备正式、可见、可读的人机入口。
2. 高价值动作不再依赖作者记忆中的隐藏热键。
3. 现有 inspect/sidebar 白盒体验无明显退化。

---

## 9. PR-05：Phase 4 白盒映射、验证与文档收口

### 9.1 目标

把 Validation Mode 与 `docs/phase4` 的人工白盒门槛一一对齐，形成真正可交付的验证加速路径。

### 9.2 非目标

`PR-05` **不做**：

1. 不把 Validation Mode 升级成自动化 harness 真源。
2. 不重写 `Phase 4` 的核心 contract。

### 9.3 设计约束

#### 9.3.1 与 `docs/phase4` 白盒条件的映射

`PR-05` 必须把 Validation preset / action 收口成一张显式映射表，至少覆盖以下领域：

| `docs/phase4` PR | Validation preset | 快速路径 / 关键 action | 必须可见的证据 |
| --- | --- | --- | --- |
| `docs/phase4 PR-02` mapgen 差异性 | `MAPGEN_DIFF` | `next-seed restart`、固定 5-seed corpus、快速重开 | seed / zone / terrain tag / hidden entrance 摘要 |
| `docs/phase4 PR-03` hidden entrance + search | `HIDDEN_CONTENT` | search anchor / hidden entrance / return bridge 快速到达，正式 `SearchAction` | `SearchAction` 反馈、reveal 状态、返回主线可验证 |
| `docs/phase4 PR-05` loot | `LOOT_LAB` | `trigger synthesized reward presentation`、`spawn item by schema id` | inspect 中模板描述、icon、音频、日志来源 |
| `docs/phase4 PR-06` terrain / elite / boss | `TERRAIN_INTERACTION` / `ELITE_MUTATION` / `BOSS_VARIANT` | terrain override、elite 快速定位、boss variant 启动预设 | terrain rule、mutation 来源、boss variant 读屏可见 |
| `docs/phase4 PR-07` hidden event / secret zone | `HIDDEN_CONTENT` | secret reward / secret return 快速到达 | log / inspect / route 中 hidden 与 secret zone 可读 |
| `docs/phase4 PR-09` content pack | `CONTENT_PACK` | sample pack 预设启动、pack-enabled restart | active pack ids、客户端可见内容、namespace 可读性 |

补充规则：

1. 这张表本身就是 `PR-05` 的正式交付物之一。
2. 文中后续提到的 `PR-02 / PR-03 / ... / PR-09` 若未特别说明，均指 `docs/phase4` 下的工作包文档，不是本文自己的 `PR-01 ~ PR-05`。
3. `docs/phase4 PR-04` 与 `docs/phase4 PR-08` 不在 Validation Mode v1 的直接加速范围内：
   - `docs/phase4 PR-04` 以 save/snapshot/client contract 收口与 white-box 输入合同预埋为主，不定义独立的局内人工白盒捷径
   - `docs/phase4 PR-08` 以 loader/lint/harness/fixture 验证为主，客户端可见性主路径留到 `docs/phase4 PR-09`
   若未来这两个工作包新增稳定的人工白盒步骤，应回到本文补正式 preset，而不是临时挪用 `CUSTOM`

#### 9.3.2 文档收口

必须同步更新：

1. `docs/phase4/2026-03-13-phase4-verification-checklist.md`
2. 本文本身
3. `docs/verification/validation-mode.md`

#### 9.3.3 白盒步骤仍保留“人工确认”

Validation Mode 的职责是提速，不是替代人工判断。

所以它不能替代以下内容：

1. 是否确实看到了 5 seed 间至少 3 类可感知差异
2. log / inspect / route 文本是否可读
3. boss variant 只改 mutation / loot / 表现而不改 phase graph 的体验判断
4. sample pack 内容是否“真可见”

### 9.4 代码范围

#### `client`

- `client/src/main/kotlin/com/ktome/client/GameApp.kt`
- `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
- `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`

#### `game`

- `game/src/main/kotlin/com/ktome/game/validation/*`
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`

#### docs

- `docs/opt/cheatMode.md`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`
- `docs/verification/validation-mode.md`

### 9.5 必须实现的行为

1. 每个 `Phase 4` 白盒领域至少有一个对应 preset。
2. `MAPGEN_DIFF` 支持 next-seed restart。
3. `HIDDEN_CONTENT` 能在不手工长时间探索的前提下完成 reveal / enter / return 主路径核验。
4. `TERRAIN_INTERACTION`、`ELITE_MUTATION`、`BOSS_VARIANT` 可快速进入可观察状态。
5. `CONTENT_PACK` 可直接以 sample pack 启动并显示 active packs。

### 9.6 必须新增的测试

1. preset -> config 映射测试矩阵：
   - 每个 preset 的 `ValidationSessionOptions -> FoundationGameConfig` 输出中，`seed / zoneId / floor / maxFloor / routeIndex / bossVariantSelectionMode / preferredBossVariantId` 的固定字段都必须有显式断言
   - 任一字段的后续调整都必须同步更新该测试
2. `ClientSmokeHarnessTest`
   - 典型 Phase 4 preset 至少能进入并打开 Validation overlay。
3. 定向 session 测试
   - next-seed restart 会使用同一 corpus 的下一个 seed
   - content pack preset 真实带上 active pack ids

### 9.7 必跑命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew :client:test --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew maintainabilityLint
./gradlew verifyChanged
./gradlew verificationGate
./gradlew phase4Report
./gradlew clientSmoke
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew hiddenContentHarness
./gradlew contentPackHarness
```

### 9.8 必看结果

1. Validation preset 与 `Phase 4` 白盒条件的映射表完整存在。
2. `Computer Use` 能借 preset + overlay 明显缩短验证路径。
3. `docs/phase4` 中人工白盒说明已同步引用 Validation Mode，而不是仍要求完全手动摸索。

### 9.9 退出标准

1. `Phase 4` 人工白盒主路径都已有对应 Validation 入口。
2. 文档、实现、测试、手工验证步骤已同步收口。
3. Validation Mode 达到“可交付给其它 agent 直接使用”的完成度。

---

## 10. 每个 PR 的统一验收模板

### 10.1 已执行命令

至少记录：

```text
- 实际运行的 Gradle 命令
- 实际运行的定向测试
- 实际执行的白盒步骤
```

### 10.2 已检查产物

至少记录：

1. 主菜单或 setup screen 是否出现新入口
2. Validation session 是否进入独立目录
3. overlay / preset / action 是否真实生效
4. 是否存在日志、截图、可读 UI 证据

### 10.3 必答问题

每个 PR 完成时都必须回答：

1. 普通玩家路径是否保持零回归？
2. Validation path 是否仍保持单一 owner，而不是 client 私有作弊？
3. 当前 PR 是否真的减少了 `Phase 4` 白盒验证的人工成本？
4. 是否引入了新的 second-authority、临时路径或未声明 compat branch？

---

## 11. 最终完成定义

整套文档完成并按 PR 实现后，完成态必须同时满足：

1. 主菜单存在正式 `Validation Mode`。
2. Validation setup 可配置 preset / seed / zone / floor / route / boss variant / sample pack。
3. Validation session 使用独立 save/profile 根目录。
4. Validation session 固定使用 `WHITE_BOX` availability context。
5. 局内存在键盘优先的 Validation overlay。
6. 主要作弊动作已经 typed 化并走正式 `FoundationGameSession` owner。
7. `docs/phase4` 里的人工白盒主路径都能通过 Validation Mode 明显提速。
8. 普通 `New Game / Continue`、正式 save/profile、正式规则 contract 全部保持稳定。

## 12. 一句话原则

Validation Mode 的职责不是“帮玩家绕过规则”，而是“让白盒验证更快到达正式规则正在发生的地方”。

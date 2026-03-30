> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
> `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`
> `docs/review/phase3/2026-03-26-phase3-follow-up-pr-07-objective-runtime-and-gate-hardening.md`
> `docs/review/phase3/2026-03-26-phase3-pr-09-content-floor-completion.md`

# Phase 3 V2 - PR-13 Encounter Density And Zone Mechanics

**阶段**: `Phase 3 / v2 follow-up`  
**优先级**: `P1`  
**前置条件**: `PR-04 / PR-06 / PR-07 / PR-09` 已使 AI DSL、长局路线和 content floor 成立，允许继续补“战斗为什么不重复”的 runtime 层  
**对应问题**: v2 review 中“普通战斗无决策”和“区域 `specialMechanics` 是伪实现”这两条，在当前代码下都还有残余问题，但已不适合拆成两份独立 PR。更合理的切法是合并成一个“遭遇密度补强”PR：一边收掉剩余 basic-profile 怪物尾部，一边把真正能改变路径决策的 `zone mechanic runtime floor` 做起来。

**Lane-parallel 拆分**：

- **W13a (Content Lane)**: 剩余 basic-profile / empty-talent monster 清尾
- **W13b (Game Lane)**: `patrol_pressure / ambush_lane / furnace_pressure` 三个 runtime mechanic 正式落地
- **W13c (QA Lane)**: monster coverage、zone mechanic runtime、long-run regression

---

## 1. 阶段目标

把 Phase 3 的遭遇体验从“怪物 roster 已补量，但还有尾部模板化”和“zone mechanic 只有提示没有运行时”推进到“同一路线中的普通战斗和区域差异都能制造稳定决策点”。

完成标准：

1. `patrol_pressure / ambush_lane / furnace_pressure` 三个 `specialMechanics` 进入正式 runtime。
2. [ZoneMechanicRuntime.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt) 不再只负责 `introHintKey()` 和 reward helper。
3. 当前仍挂在 `ai.(chase|kite|patrol).basic` 上的 monster 从 `14` 个收敛到 `<= 4` 个。
4. 这 `<= 4` 个保留 basic profile 的 monster 只能是 tutorial fodder 或极低威胁填充怪，不能再覆盖 optional/late-game 主战斗面。
5. 本 PR 不重写 BSP，也不引入脚本化 zone runtime；三种机制必须建立在现有 typed schema / session / effect 主链上。

## 2. 当前问题

1. [ZoneMechanicRuntime.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt) 当前主要只提供 intro hint key 和 unique content reward helper。
2. [zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/zones/index.yaml) 中已经定义了大量 `specialMechanics`，但运行时几乎没有对应实现。
3. [monsters/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/monsters/index.yaml) 当前虽然已经不再是“只有 Boss 带 talent”的状态，但仍有 `14` 个 monster 使用 `ai.chase.basic / ai.kite.basic / ai.patrol.basic`。
4. optional / late-game zone 里仍有一部分战斗因为“AI 简单 + 没有 zone runtime 压力”而退化成数值交换。

### 2.1 本 PR 必须冻结的口径

1. 本 PR 只正式实现 `3` 个机制：
   - `patrol_pressure`
   - `ambush_lane`
   - `furnace_pressure`
2. 其余 `specialMechanics` 可以继续只作为 hint/tag 存在，但不能伪装成“已经正式运行”。
3. zone mechanic runtime 必须通过 typed program / typed state 进入 `FoundationGameSession`，不能把字符串判断散落在多个 call site。
4. 仍留在 basic AI 上的怪物只能是低威胁 tutorial tail，不允许 optional/late-game 主战斗仍靠 basic profile 撑场。
5. 优先复用现有 AIProfile / talents / world effect / overlay 主链；只有表达不足时才补最小必要 runtime。
6. 不引入新的 Lua/script host，不引入第二套地图生成器。

## 3. 范围与非目标

### 3.1 范围

1. zone mechanic runtime floor（仅 3 个机制）。
2. 剩余 basic-profile monster 清尾。
3. 对应的 AI / monster data 更新。
4. long-run / headless / schema / white-box 回归。

### 3.2 非目标

1. 不做完整随机事件系统。
2. 不做隐藏房间 / 宝箱系统。
3. 不做全部 `specialMechanics` 的一次性补齐。
4. 不新增正式 Boss encounter。
5. 不在本 PR 讨论 Shard 经济、楼层奖励节拍或战斗反馈 snapshot。

## 4. 技术方案

### 4.1 [W13b] Zone Mechanic Runtime Program

建议文件：

```text
game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt
```

冻结口径：

1. `ZoneMechanicRuntime` 从“文案 helper”提升为“mechanic spec + runtime helper”。
2. 建议新增 typed spec：
   - `PatrolPressureSpec`
   - `AmbushLaneSpec`
   - `FurnacePressureSpec`
3. spec 必须在 floor/session 初始化阶段一次性解析，避免每回合遍历 raw string tag。
4. 解析结果必须 deterministic，候选点位选择依赖 `seed + zoneId + floorIndex`，不得引入系统时间。

### 4.2 [W13b] Patrol Pressure

建议落点：

1. 仅在带 `patrol_pressure` 的 zone 启用。
2. 每 `20` 个 actor-equivalent turn 检查一次：
   - 若当前 hostile 数量低于上限
   - 且玩家不在 Boss encounter
   - 且本层 wave 次数未超上限
   则在玩家视野外的合法空地刷出 `1` 组 patrol 怪。
3. 每层上限建议先固定为 `3` 波。
4. 玩家首次进入该层时给一次 hint，后续不重复刷提示文案。

### 4.3 [W13b] Ambush Lane

建议落点：

1. 在带 `ambush_lane` 的 zone 中，于 floor build 后识别 `1~2` 条窄走廊触发点。
2. 触发点选择规则必须完全定义：
   - passable corridor
   - 两侧存在可生成怪的房间/区域
   - 不直接贴在 stairs / objective 上
3. 玩家首次踏入触发点时，绑定的伏击组同时激活并进入追击。
4. 伏击组只触发一次，不做无限重置。

### 4.4 [W13b] Furnace Pressure

建议落点：

1. 在带 `furnace_pressure` 的 zone 中，每 `30` 回合进入一次 hazard cycle。
2. cycle 分两段：
   - telegraph `1` 回合
   - 火焰压力实际生效 `2` 回合
3. hazard cell 应优先围绕 forge/slag 主题 props 或预定义候选区，找不到时再用 deterministic fallback。
4. 优先复用现有 `AreaEffectEmitter / WorldEffect / OverlayRenderSnapshot` 主链。

### 4.5 [W13a] Monster Tail Cleanup

建议文件：

```text
game/src/main/resources/data/monsters/index.yaml
game/src/main/resources/data/ai/index.yaml
game/src/test/kotlin/com/ktome/game/data/MonsterSchemaTest.kt
```

冻结口径：

1. 目标不是“重做全部 60 个怪”，而是收掉剩余 tail：
   - `ai.basic` 分配数从 `14 -> <= 4`
2. 优先按 archetype 批量收敛：
   - `skirmisher / scout` -> 至少具备追击、缠斗或位移压力
   - `artillery` -> 至少具备蓄力/远程压制行为
   - `sentinel` -> 至少具备警戒/召集/守线行为
   - `brute` -> 至少具备冲阵或破线行为
3. 优先复用已有 profile：
   - `ai.elite.huntmaster`
   - `ai.forge.guard`
   - `ai.river.lurker`
   - `ai.crystal.weaver`
   - `ai.abyssal.guard`
4. 只有现有 profile 明显无法表达时，才新增少量 archetype profile。

建议的验收指标：

1. `optional / late-game` zone 中不再出现只会 `MOVE_TOWARD_TARGET + ATTACK` 的主战怪。
2. tutorial/outpost 残留 basic profile 怪不超过 `4` 个。

### 4.6 资源复用与资源边界

冻结口径：

1. `W13a` 的 monster tail cleanup 默认**不新增 monster raw art / raw audio**。对既有怪物的 AI/profile/talent 调整，不自动触发 actor/icon/audio 重画；继续复用当前 family 资源。
2. `W13b` 的 zone mechanic runtime 第一版必须优先复用现有正式资源：
   - telegraph / warning overlay：`vfx.telegraph.warning.sigil_01`
   - warning cue：`audio.boss.warning`
   - forge 场景锚点：`prop.mine_furnace`
3. `patrol_pressure / ambush_lane / furnace_pressure` 的首版目标是让 runtime 成立，不是顺手扩一整包 zone VFX。只要现有 overlay 和 cue 足以通过白盒辨识，就不得追加新的 raw asset 范围。
4. 若白盒验证证明某个机制在正式玩家路径中仍不可读，只允许在同一 PR 内补最小 manifest 级复用或 alias；任何真正新增 raw image / raw audio 的需求，必须拆成单独 companion asset PR。
5. 若后续 companion asset PR 需要生成图片，执行方必须先向用户索取 `GEMINI_API_KEY`，并沿用 [PR-09 Asset Batch Generation Checklist](/Users/luo/Documents/github/K-ToME/docs/review/phase3/2026-03-26-phase3-pr-09-asset-batch-generation-checklist.md) 的目录、命名、manifest 和 lint gate。

## 5. 推荐改动面

### 5.1 `game`

1. `ZoneMechanicRuntime.kt`
2. `FoundationGameSession.kt`
3. `data/monsters/index.yaml`
4. `data/ai/index.yaml`

### 5.2 `core`（仅在现有 carrier 无法表达时）

1. `WorldEffect / AreaEffectEmitter / Overlay` 相关 carrier

### 5.3 `tools / QA`

1. `MonsterSchemaTest`
2. `LongRunLabTest / LongRunLabFullTest`
3. 需要新增 zone mechanic 行为级断言

## 6. 测试与自证

### 6.1 必测类

1. `MonsterSchemaTest`
2. `FoundationGameSessionTest`
3. `LongRunLabTest`
4. `LongRunLabFullTest`

### 6.2 必测行为

1. `patrol_pressure` zone 会在规定回合制造额外压力。
2. `ambush_lane` 只触发一次且会激活伏击组。
3. `furnace_pressure` 能 telegraph 并在后续回合真实结算。
4. basic AI 分配数下降到 `<= 4`。
5. optional/late-game zone 的主战怪不再大量挂在 `ai.basic`。

### 6.3 自动化命令

```bash
./gradlew :game:test --tests "com.ktome.game.data.MonsterSchemaTest"
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew longRunLab
./gradlew check
```

### 6.4 白盒验证

1. 打一局 `shattered_outpost / greenwood_fringe`，确认 `patrol_pressure` 能制造额外时间压力。
2. 进入 `bandit_camp`，确认走廊伏击会实际触发，而不是只显示文案。
3. 进入 `deep_iron_pit`，确认压力区会 telegraph 并在后续回合造成火焰威胁。

## 7. 出口门禁

1. 三个指定机制已进入正式 runtime。
2. `ZoneMechanicRuntime` 不再只是 hint helper。
3. `ai.basic` monster 尾部已收敛到 `<= 4`。
4. `longRunLab / check` 绿色。

## 8. 风险与止损

### 8.1 风险

1. `ambush_lane` 和 `furnace_pressure` 容易把难度推得过陡。
2. 若为每个尾部怪都新建一套 AI profile，会把内容补量演化成系统重构。
3. zone mechanic 若直接写死在 session 主循环里，后续会成为维护噪音。

### 8.2 止损

1. 机制只做 `3` 个，先建立 floor，不追求全覆盖。
2. monster tail 清理优先复用已有 profile / talent，不新增第二套行为解释器。
3. 若某机制在 smoke 中过于致命，先调频率/上限，而不是回滚整个 runtime contract。

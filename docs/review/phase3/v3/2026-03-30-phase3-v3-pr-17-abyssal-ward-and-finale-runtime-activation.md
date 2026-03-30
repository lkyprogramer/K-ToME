> 执行前必须先完整阅读并接受：
> `docs/review/phase3/phase3_opt_deep_review_final.md`
> `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
> `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`

# Phase 3 V3 - PR-17 Abyssal Ward And Finale Runtime Activation

**阶段**: `Phase 3 / v3 follow-up`  
**优先级**: `P0`  
**前置条件**: `PR-15` 已把 full-route gate 补硬，`PR-16` 已把中后段常规 late-zone runtime 打通  
**对应问题**: `abyssal_temple / abyssal_heart` 作为终线区段，当前主要靠怪物和 boss 承担差异化。若不补足 ward / pressure / pre-boss hook，`Phase 3` 终线仍会显得像“最后一条更长的战斗走廊”。  

**Lane-parallel 拆分**：

- **W17a (Game Lane)**: `abyssal_ward / void_pressure` runtime
- **W17b (Game Lane)**: `temple_ward_reliquary / heart_ward_focus` 进入实际玩法
- **W17c (QA Lane)**: finale branch 场景与 boss 前后衔接验证

---

## 1. 阶段目标

让终线区段在进入 Boss 前就有明确的玩法身份，而不是把全部记忆点都压到 `abyssal_guardian` 一场战斗上。

完成标准：

1. `abyssal_temple` 至少有一个会真实改变推进方式的 runtime mechanic。
2. `temple_ward_reliquary` 不再只是 support 名字，而是正式玩法节点。
3. `abyssal_heart` 至少有一个轻量 pre-boss runtime hook。
4. `heart_ward_focus` 不再只承担 objective 文案，而是进入实际状态变化。
5. finale 的玩法增强必须是 Boss 之前的铺垫，不允许再发明第二套 Boss 或第二套规则解释器。

## 2. 当前问题

1. `abyssal_temple` 的 `abyssal_ward / prayer_hall / void_pressure` 主要停留在命名层。
2. `abyssal_heart` 的 `finale / void_eruption / heart_ward` 主要停留在命名层。
3. 当前终线区的差异更多来自怪物 roster 和 Boss 自身，而不是 zone runtime。
4. 如果终线区自身没有玩法身份，完整 run 的后段节奏会显得“进了最后地图，但玩起来还是同一套推进方式”。

### 2.1 本 PR 必须冻结的口径

1. 本 PR 只做终线区段的最小 runtime，不顺手做新 Boss、新职业或 Phase 4 hidden content。
2. `abyssal_temple` 的核心身份固定为：
   - 需要处理 ward / pressure 才能更安全推进
3. `abyssal_heart` 的核心身份固定为：
   - Boss 前有轻量 pre-boss stabilizer / focus hook
4. `heart_ward_focus` 的作用必须是：
   - 改变 finale 压力曲线
   - 不是纯文案打卡
5. 默认复用现有主链：
   - `WorldEffect`
   - `OverlayRenderSnapshot`
   - `PendingTelegraphState`
   - 已有 interactable/objective 流程

## 3. 范围与非目标

### 3.1 范围

1. `abyssal_temple` runtime
2. `abyssal_heart` pre-boss runtime
3. `temple_ward_reliquary / heart_ward_focus` 行为落地
4. finale 验证场景

### 3.2 非目标

1. 不重做 `abyssal_guardian` Boss 设计
2. 不新增第二个 finale Boss
3. 不做 Phase 4 级 hidden event
4. 不新增第二套 Boss raw art / raw audio
5. 但必须补终线区段最小 identity pack：
   - `2` 个 zone card
   - `2` 个 finale interactable prop
   - `1` 个 void-pressure overlay
   - `2` 组 ambience/zone cue
   - `2` 个 interactable cue

## 4. 技术方案

### 4.1 [W17a] Abyssal Temple - Ward Pressure Runtime

建议文件：

```text
game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/main/resources/data/objectives/index.yaml
game/src/main/resources/data/interactables/index.yaml
```

冻结口径：

1. `abyssal_ward` 第一版固定为“需要处理 reliquary 才能压低 zone pressure”的机制。
2. `void_pressure` 第一版固定为：
   - 周期性 telegraph
   - 指定危险区域短时激活
3. `temple_ward_reliquary` 交互后可触发以下最小效果之一：
   - 临时压低 `void_pressure`
   - 开启安全推进 corridor
   - 给予限定回合的 ward protection
4. 规则重点是“让玩家决定先处理节点还是硬顶压力”，而不是单纯多打一波怪。

### 4.2 [W17b] Abyssal Heart - Pre-Boss Focus Hook

建议文件：

```text
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/main/resources/data/objectives/index.yaml
game/src/main/resources/data/interactables/index.yaml
game/src/test/kotlin/com/ktome/game/LongRunWorldStructureSessionTest.kt
```

冻结口径：

1. `heart_ward_focus` 必须进入正式玩法：
   - 若处理成功，降低 Boss 前 1 段 pressure
   - 或给 finale arena 一个短时保护窗口
2. 这一步只能做 pre-boss 铺垫，不得替代 Boss 战本身。
3. `void_eruption` 第一版若实现，必须是轻量：
   - telegraph
   - 小范围危害
   - 不做全图 chaos system
4. `finale` 标签应至少进入一个实际 runtime 消费点，而不是只做结算页分支。

### 4.3 [W17c] QA And Finale Validation

建议文件：

```text
game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt
game/src/test/kotlin/com/ktome/game/LongRunWorldStructureSessionTest.kt
game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt
```

冻结口径：

1. 至少补 3 类断言：
   - `temple_ward_reliquary` 会改变 pressure 行为
   - `heart_ward_focus` 会改变 finale 前状态
   - Boss 前后 telegraph / pressure / focus hook 不互相打架
2. `bossHarness` 不需要扩成第二套 finale harness，但要至少覆盖：
   - finale pre-hook 不会破坏 `abyssal_guardian` 的 phase trace

### 4.4 [W17d] Asset And Audio Identity Pack

建议文件：

```text
assets-src/image/specs/phase3-v3-pr17-gemini-plan.yaml
assets-src/audio/specs/phase3-v3-pr17-audio-plan.yaml
game/src/main/resources/data/visuals/index.yaml
game/src/main/resources/data/audio/index.yaml
game/src/main/resources/data/ambient/index.yaml
client/src/main/resources/manifests/visual-manifest.json
client/src/main/resources/manifests/audio-manifest.json
build.gradle.kts
```

冻结口径：

1. 这批资源只服务：
   - `zone.abyssal_temple.*`
   - `zone.abyssal_heart.*`
   - `prop.temple_ward_reliquary`
   - `prop.heart_ward_focus`
   - `vfx.zone.effect.void_pressure_01`
   - `ambient.* / audio.zone.* / audio.interactable.*` 对应新 key
2. `void_pressure / void_eruption` 的 warning cue 继续复用 `audio.boss.warning`。
3. `abyssal_guardian` 的 Boss cue 和 Boss 图不在本 PR 里重做。
4. `PR-19` 的 late-run shard spend 必须直接复用 `prop.temple_ward_reliquary` 与对应 audio，不再另开第二套 reliquary 资源。
5. 详细 asset/audio 表、prompt 方向和命令模板见 companion 文档：
   - `docs/review/phase3/v3/2026-03-30-phase3-v3-pr15-pr20-asset-and-audio-assessment.md`

## 5. 推荐改动面

### 5.1 `game`

1. `ZoneMechanicRuntime.kt`
2. `FoundationGameSession.kt`
3. `objectives/index.yaml`
4. `interactables/index.yaml`

### 5.2 `tools / QA`

1. `FoundationGameSessionTest`
2. `LongRunWorldStructureSessionTest`
3. `BossHarnessTest`

## 6. 测试与自证

### 6.1 必测类

1. `FoundationGameSessionTest`
2. `LongRunWorldStructureSessionTest`
3. `BossHarnessTest`

### 6.2 必测行为

1. `abyssal_temple` 的 ward / pressure 进入正式 runtime
2. `temple_ward_reliquary` 交互后有明确收益
3. `heart_ward_focus` 会改变 finale 前的推进状态
4. 这些变化不会破坏 `abyssal_guardian` 现有 Boss harness

### 6.3 自动化命令

```bash
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew :game:test --tests "com.ktome.game.LongRunWorldStructureSessionTest"
./gradlew :game:test --tests "com.ktome.game.harness.BossHarnessTest"
./gradlew bossHarness
./gradlew check
```

### 6.4 白盒验证

1. 进入 `abyssal_temple`，确认玩家会优先考虑处理 reliquary 或改变推进路线
2. 进入 `abyssal_heart`，确认 Boss 前有可理解的 pre-boss hook，而不是直接换房间打王

## 7. 出口门禁

1. `abyssal_temple / abyssal_heart` 都有正式 runtime 身份
2. `temple_ward_reliquary / heart_ward_focus` 进入主链
3. finale 前后衔接测试补齐
4. `./gradlew check` 保持绿色

## 8. 风险与止损

### 8.1 风险

1. 终线 runtime 若过重，会喧宾夺主，压掉 Boss 战本身
2. 若把 finale hook 做成“强制步骤”，可能拖慢终局节奏

### 8.2 止损

1. 终线 runtime 只做“轻量但有身份”的机制
2. 优先让玩家感受到节奏变化，而不是额外塞很多系统说明

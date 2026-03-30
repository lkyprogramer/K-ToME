> 执行前必须先完整阅读并接受：
> `docs/review/phase3/phase3_opt_deep_review_final.md`
> `docs/review/phase3/v2/2026-03-29-phase3-v2-pr-13-encounter-density-and-zone-mechanics.md`
> `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`

# Phase 3 V3 - PR-16 River And Crystal Runtime Activation

**阶段**: `Phase 3 / v3 follow-up`  
**优先级**: `P0`  
**前置条件**: `PR-15` 已把 full-route gate 补硬，允许用更真实的长局样本验证 late-zone runtime  
**对应问题**: `underground_river` 与 `crystal_cavern` 已有 `specialMechanics / objective / interactable` 数据，但玩家当前主要感受到的仍是“换一批怪继续打”。这会直接造成 `deep_iron_pit` 之后的新鲜感掉档。  

**Lane-parallel 拆分**：

- **W16a (Game Lane)**: `currents / ferry_crossing` runtime
- **W16b (Game Lane)**: `crystal_shards / resonance` runtime
- **W16c (QA Lane)**: late-zone 行为级测试与 long-run 回归

---

## 1. 阶段目标

把 `underground_river` 和 `crystal_cavern` 从“有命名和怪物池的后半程 zone”推进到“有自己决策压力的后半程 zone”。

完成标准：

1. `underground_river` 至少有一个会真实改变路线/站位的 runtime mechanic。
2. `river_ferry_anchor` 不再只是摆设 objective，而要进入实际玩法流程。
3. `crystal_cavern` 至少有一个会真实改变移动/交战节奏的 runtime mechanic。
4. `crystal_resonance_node` 不再只是普通 support 节点，而要带来清晰的行为后果。
5. 两个 zone 的机制都必须进入自动化测试，而不是只靠 hint 文案自证。

## 2. 当前问题

1. `underground_river` 的 `currents / ferry_crossing / drowned_ambush` 目前主要停留在命名层。
2. `crystal_cavern` 的 `crystal_shards / resonance_cache` 目前主要停留在命名层。
3. 这两个 zone 虽然怪物池不同，但在玩家视角仍偏向“普通战斗 + 普通探索”。
4. late-zone 若没有足够 runtime 差异，`Phase 3` 长局体验会在中后段塌成重复推图。

### 2.1 本 PR 必须冻结的口径

1. 本 PR 只做 `river / crystal` 两个 zone，不顺手扩到 `abyssal_temple / abyssal_heart`。
2. 机制必须优先复用现有主链：
   - `AreaEffectEmitter`
   - `WorldEffect`
   - `OverlayRenderSnapshot`
   - 已有 interactable/objective runtime
3. 本 PR 不引入 Phase 4 级复杂 ProcGen，也不加新脚本系统。
4. 若现有 carrier 能表达，就不新增第二套 runtime 容器。
5. `telegraph` 通用提示继续复用：
   - `audio.boss.warning`
   - 现有 warning overlay
6. 但 `river / crystal` 的 zone/interactable 身份不能继续完全复用 `grey_gate / ritual_altar / armory_gate`。
   本 PR 需要补一组最小资源 companion：
   - `2` 个 zone card
   - `2` 个 interactable prop
   - `2` 个 mechanic overlay
   - `2` 组 ambience/zone cue
   - `2` 个 interactable cue
7. 详细资源方案见：
   - `docs/review/phase3/v3/2026-03-30-phase3-v3-pr15-pr20-asset-and-audio-assessment.md`

## 3. 范围与非目标

### 3.1 范围

1. `underground_river` 的 `currents / ferry_crossing`
2. `crystal_cavern` 的 `crystal_shards / resonance`
3. 相关 objective/interactable 真正挂到 runtime
4. 行为级测试与 long-run 观测

### 3.2 非目标

1. 不做完整水地形系统
2. 不做完整 line-of-sight /折射系统
3. 不扩怪物 roster
4. 不做 finale 机制

## 4. 技术方案

### 4.1 [W16a] Underground River - Currents And Ferry Crossing

建议文件：

```text
game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/main/resources/data/objectives/index.yaml
game/src/main/resources/data/interactables/index.yaml
```

冻结口径：

1. `currents` 第一版固定为“周期性或进入格触发的位移压力”，不是全地图流体模拟。
2. 当前可选最小实现二选一：
   - 若玩家/怪物站在 `current` 标记区，回合结算时被向预定义方向推 `1` 格
   - 或者每隔固定 turn 激活一条流水 lane，强制玩家改走位
3. `river_ferry_anchor` 必须进入实际流程：
   - 交互后开启安全 crossing lane
   - 或短时关闭某段 current pressure
4. `drowned_ambush` 若加入，必须依赖已触发的 crossing 行为，不做独立无限刷怪系统。
5. 规则重点是“改变路径选择”，不是额外堆伤害。

### 4.2 [W16b] Crystal Cavern - Shard Pressure And Resonance Node

建议文件：

```text
game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/main/resources/data/objectives/index.yaml
game/src/main/resources/data/interactables/index.yaml
```

冻结口径：

1. `crystal_shards` 第一版固定为“可 telegraph 的晶刺 pressure”：
   - 指定格或房间在 telegraph 后生成晶刺伤害/阻挡
   - 不做完整 destructible terrain
2. `crystal_resonance_node` 必须进入正式玩法：
   - 交互后压低 shard pressure
   - 或暂时清除某片危害格
3. `resonance_cache` 不再只做命名层，可作为与 node 绑定的一次性奖励触发。
4. 规则重点是“迫使玩家处理 node 或改变站位”，而不是纯数值增伤。

### 4.3 [W16c] QA And Regression

建议文件：

```text
game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt
game/src/test/kotlin/com/ktome/game/LongRunWorldStructureSessionTest.kt
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
```

冻结口径：

1. `underground_river` 至少要有：
   - `currents` 行为断言
   - `ferry_anchor` 交互断言
2. `crystal_cavern` 至少要有：
   - `crystal_shards` telegraph / resolution 断言
   - `resonance_node` 交互后行为变化断言
3. `longRunLabFullTest` 至少补 1 条经过这两个 zone 的 branch 样本。

### 4.4 [W16d] Asset And Audio Identity Pack

建议文件：

```text
assets-src/image/specs/phase3-v3-pr16-gemini-plan.yaml
assets-src/audio/specs/phase3-v3-pr16-audio-plan.yaml
game/src/main/resources/data/visuals/index.yaml
game/src/main/resources/data/audio/index.yaml
game/src/main/resources/data/ambient/index.yaml
client/src/main/resources/manifests/visual-manifest.json
client/src/main/resources/manifests/audio-manifest.json
build.gradle.kts
```

冻结口径：

1. 这不是大资源包，只补：
   - `zone.underground_river.*`
   - `zone.crystal_cavern.*`
   - `prop.river_ferry_anchor`
   - `prop.crystal_resonance_node`
   - `vfx.zone.effect.current_lane_01`
   - `vfx.zone.effect.crystal_shard_01`
   - `ambient.* / audio.zone.* / audio.interactable.*` 对应新 key
2. `currents / crystal_shards` 的 warning cue 继续复用 `audio.boss.warning`，不再新增第二套 warning 音。
3. 资源生成必须复用现有脚本：
   - 图片：`scripts/generate_assets.sh`
   - 音频：`scripts/process_audio.py`
4. 执行图片批量生成前，必须先向用户索取 `GEMINI_API_KEY`。
5. 详细 asset/audio 表、prompt 方向和命令模板见 companion 文档：
   - `docs/review/phase3/v3/2026-03-30-phase3-v3-pr15-pr20-asset-and-audio-assessment.md`

## 5. 推荐改动面

### 5.1 `game`

1. `ZoneMechanicRuntime.kt`
2. `FoundationGameSession.kt`
3. `objectives/index.yaml`
4. `interactables/index.yaml`

### 5.2 `core`

1. 默认不动
2. 仅当现有 `AreaEffectEmitter / WorldEffect` 无法表达时，才最小扩展

### 5.3 `tools / QA`

1. `FoundationGameSessionTest`
2. `LongRunWorldStructureSessionTest`
3. `LongRunLabFullTest`

## 6. 测试与自证

### 6.1 必测类

1. `FoundationGameSessionTest`
2. `LongRunWorldStructureSessionTest`
3. `LongRunLabFullTest`

### 6.2 必测行为

1. `currents` 会真实影响位移或路线
2. `river_ferry_anchor` 会改变 crossing 风险
3. `crystal_shards` 会 telegraph 并在后续回合结算
4. `crystal_resonance_node` 交互会带来明确收益

### 6.3 自动化命令

```bash
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew :game:test --tests "com.ktome.game.LongRunWorldStructureSessionTest"
./gradlew :game:test --tests "com.ktome.game.harness.LongRunLabFullTest"
./gradlew longRunLab
./gradlew check
```

### 6.4 白盒验证

1. 进入 `underground_river`，确认 `currents` 或 crossing hook 会改变路径选择
2. 进入 `crystal_cavern`，确认玩家必须处理 resonance node 或承担 shard pressure

## 7. 出口门禁

1. `underground_river` 与 `crystal_cavern` 都有正式 runtime mechanic
2. 对应 interactable/objective 已进入玩法主链
3. late-zone branch 样本已进入 `longRunLab`
4. `./gradlew check` 保持绿色

## 8. 风险与止损

### 8.1 风险

1. 过强的位移/危害可能把后半程难度抬得过陡
2. zone 机制若直接写死在 session 大循环里，会继续累积维护噪音

### 8.2 止损

1. 第一版优先做“低复杂度、强差异”的路径/站位机制
2. 所有规则优先沉到 `ZoneMechanicRuntime`，不在 `FoundationGameSession` 中散落字符串特判

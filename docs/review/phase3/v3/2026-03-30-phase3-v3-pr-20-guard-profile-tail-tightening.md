> 执行前必须先完整阅读并接受：
> `docs/review/phase3/phase3_opt_deep_review_final.md`
> `docs/review/phase3/v2/2026-03-29-phase3-v2-pr-13-encounter-density-and-zone-mechanics.md`
> `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`

# Phase 3 V3 - PR-20 Guard Profile Tail Tightening

**阶段**: `Phase 3 / v3 follow-up`  
**优先级**: `P2`  
**前置条件**: `PR-18 / PR-19` 已完成或至少 `PR-16 / PR-17` 已让后半程体验更稳定，允许继续收中后期普通怪行为尾部  
**对应问题**: `ai.chase.basic / ai.patrol.basic` 的大尾巴已经基本清掉，但中后期仍有一批主战怪挂在 `ai.guard.basic` 这种泛用 profile 上。它不是严重架构问题，但会持续压平普通战斗的节奏差异。  

**Lane-parallel 拆分**：

- **W20a (Content Lane)**: 怪物重新分配到更贴切的现有 profile
- **W20b (Rules/Content Lane)**: 仅在必要时新增少量 archetype profile
- **W20c (QA Lane)**: monster schema 与 long-run / combat smoke 回归

---

## 1. 阶段目标

把当前中后期仍然挂在 `ai.guard.basic` 的一批怪物收口到更符合其 zone / archetype 身份的行为上，让普通战斗的差异性再往前推一步。

完成标准：

1. `ai.guard.basic` 不再承载当前这批中后段主战怪的主要行为。
2. 能复用现有 profile 的优先复用，不为每只怪单独造 profile。
3. 修改后普通战斗更有身份，但不引入新的不可控复杂度。

## 2. 当前问题

当前至少有以下怪物仍挂在 `ai.guard.basic`：

1. `goblin.scrapper`
2. `orc.miner`
3. `undead.chain_thrall`
4. `warded_ruin.vault_watcher`
5. `forge.slag_tender`
6. `river.undertow_brute`

其中至少后 `5` 个都已经脱离教程/早期区段，继续使用泛用守线行为，会削弱：

1. zone 身份差异
2. 怪物 archetype 识别度
3. 普通战斗的节奏变化

### 2.1 本 PR 必须冻结的口径

1. 优先复用现有 profile，不新开大批 profile。
2. 只有在现有 profile 明显无法表达时，才新增极少量 archetype profile。
3. 本 PR 不是 monster 内容扩容，不新增怪物数量。
4. 本 PR 不改 Boss AI，不改 telegraph 合同。

## 3. 范围与非目标

### 3.1 范围

1. `monsters/index.yaml`
2. `ai/index.yaml`
3. 怪物 AI 分配回归
4. 对应 smoke / long-run 观察

### 3.2 非目标

1. 不新增新怪
2. 不做新的 AI DSL 特性
3. 不做新的 boss/elite 系统

## 4. 技术方案

### 4.1 [W20a] Reassignment First

建议文件：

```text
game/src/main/resources/data/monsters/index.yaml
game/src/main/resources/data/ai/index.yaml
game/src/test/kotlin/com/ktome/game/data/MonsterSchemaTest.kt
```

冻结口径：

1. `orc.miner / forge.slag_tender` 优先尝试复用：
   - `ai.forge.guard`
   - `ai.forge.channeler`
2. `undead.chain_thrall / warded_ruin.vault_watcher` 优先尝试复用：
   - `ai.warded_ruin.sentinel`
   - `ai.warded_ruin.cleanser`
3. `river.undertow_brute` 优先尝试复用：
   - `ai.river.reaver`
   - `ai.river.lurker`
4. `goblin.scrapper` 若仍保留 `guard.basic`，必须明确标注为教程/过渡例外，而不是无意识残留。

### 4.2 [W20b] Minimal New Profiles Only If Needed

建议文件：

```text
game/src/main/resources/data/ai/index.yaml
game/src/test/kotlin/com/ktome/game/data/MonsterSchemaTest.kt
```

冻结口径：

1. 如果复用现有 profile 无法表达，再允许新增 `1~2` 个小 profile。
2. 新 profile 只允许围绕现有 DSL 组合：
   - `ATTACK_TARGET`
   - `MOVE_TOWARD_TARGET`
   - `RETREAT_FROM_TARGET`
   - `USE_ABILITY`
3. 不引入新 selection policy 或新 condition family。

### 4.3 [W20c] QA And Regression

建议文件：

```text
game/src/test/kotlin/com/ktome/game/data/MonsterSchemaTest.kt
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
```

冻结口径：

1. 至少补以下断言：
   - 目标怪物不再挂 `ai.guard.basic`
   - 对应 zone 的普通战斗样本仍稳定
2. `LongRunLabFullTest` 至少确认：
   - 中后段相关 zone 不因 profile 调整出现大面积 stall

## 5. 推荐改动面

### 5.1 `game`

1. `data/monsters/index.yaml`
2. `data/ai/index.yaml`

### 5.2 `tools / QA`

1. `MonsterSchemaTest`
2. `LongRunLabFullTest`

## 6. 测试与自证

### 6.1 必测类

1. `MonsterSchemaTest`
2. `LongRunLabFullTest`

### 6.2 必测行为

1. 指定怪物完成 profile 重分配
2. 中后段 zone 不因 profile 调整出现大量卡死或极端难度跳变

### 6.3 自动化命令

```bash
./gradlew :game:test --tests "com.ktome.game.data.MonsterSchemaTest"
./gradlew :game:longRunLab --tests "*LongRunLabFullTest"
./gradlew longRunLab
./gradlew check
```

### 6.4 白盒验证

1. 抽查 `underground_river / molten_core / elven_ruins` 的普通战斗
2. 确认目标怪不再显得只是“会往前走然后平A”

## 7. 出口门禁

1. 目标怪物完成 profile 收口
2. `ai.guard.basic` 不再承担当前这批中后段主战怪
3. `./gradlew check` 保持绿色

## 8. 风险与止损

### 8.1 风险

1. 若为了收口 profile 而过度新增新 profile，会把内容补量重新演化成系统重构
2. 若 profile 切换不谨慎，可能把普通战斗难度突然抬高

### 8.2 止损

1. 优先复用现有 profile
2. 新增 profile 只允许极少量，且必须服务多个怪物 archetype

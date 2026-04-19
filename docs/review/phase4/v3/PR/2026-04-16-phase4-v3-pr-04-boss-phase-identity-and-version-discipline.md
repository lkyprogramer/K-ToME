> 执行前必须先完整阅读并接受：
> `docs/rule/kotlin.md`
> `docs/rule/ai-change-governance.md`
> `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part1.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part4.md`
> `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-03-organic-hidden-loop-secret-reward-identity-and-replay-hook.md`

# Phase 4 V3 PR-04 Boss Phase Identity 与 Version Discipline

**阶段**: `Phase 4 / Post-Review Follow-up / V3-PR-04`  
**优先级**: `P1`  
**工作量评估**: `M`（`3~5` 人日）  
**前置条件**: `V3-PR-01`，建议在 `V3-PR-02 / 03` 后执行  
**对应问题**:

1. boss 终盘语言仍偏薄，当前只有 3 个 variant、每个 2–3 个 action、没有 phase 变化
2. runtime/build/sample pack 的版本纪律仍未对齐

## 0. 验证约束

1. 默认开发回路先跑 `./gradlew verifyChanged`。
2. 本 PR 默认联合验收固定为：
   - `bossHarness`
   - `clientSmoke`
   - `contentPackHarness`
   - `verifyOwner`
   - `phase4Report`
3. 若改动影响 build metadata / sample pack version range，再补 pack 侧白盒或最小 smoke 验证。

### 0.1 Computer Use / Validation Mode 快速白盒模块

本 PR 的人工白盒验证默认复用：

1. [docs/opt/cheatMode.md](/Users/luo/Documents/github/K-ToME/docs/opt/cheatMode.md:919) 中 `PR-06 / PR-09` 对应 Validation 映射
2. [docs/verification/validation-mode.md](/Users/luo/Documents/github/K-ToME/docs/verification/validation-mode.md:7) 中 `BOSS_VARIANT` 与 `CONTENT_PACK` 路径

推荐拆成两个模块，`Computer Use` 先做 boss，再做 pack version/boundary。

#### 模块 A：`BOSS_VARIANT` boss identity 快速核验

- Validation preset：`BOSS_VARIANT`
- setup 关注项：
  - 记录当前 boss variant / boss zone / floor
  - 若 setup 支持 variant 切换，至少人工验证 2 个 variant
- 局内操作顺序：
  1. `F9` 打开 overlay
  2. 确认 summary 中的 preset / boss variant / zone / floor
  3. 执行 `Travel to Boss`
  4. 观察 phase 变化、boss cue、variant 相关日志/inspect
  5. 如需要快速收尾，再执行 `Kill Active Boss`
- 必看证据：
  - boss variant 在 inspect / 顶部 cue / 日志中是可读的，不只是内部 id 变化
  - phase 变化真实发生，不是只有血量变化
  - 不同 variant 至少在 mutation / terrain preference /掉落/表现里有一项可感知差异

#### 模块 B：`CONTENT_PACK` version / pack boundary 快速核验

- Validation preset：`CONTENT_PACK`
- setup 关注项：
  - 确认 sample pack 默认启用
  - 若版本口径不匹配导致 setup 阻止启动，视为 version discipline blocker
- 局内操作顺序：
  1. 启动 `CONTENT_PACK`
  2. `F9` 打开 overlay
  3. 检查 `active pack ids`
  4. 沿 sample pack 主路径至少进入一次 pack 内容
- 必看证据：
  - pack 不因 `gameVersionRange` / build metadata 漂移而被拒绝
  - overlay 中 active pack ids 可见
  - sample pack 实际内容可见，而不是只有 metadata 可见

#### 人工判断边界

以下判断仍必须由人工负责：

1. boss 是否真的形成“不同阶段、不同打法”的终盘语言
2. variant/base trace divergence 是否在玩家前台也能被感知，而不是只存在于 harness 指标
3. version discipline 是否既不放宽 pack boundary，也不把 sample pack 意外锁死

## 1. 阶段目标

给终盘补上“不同阶段、不同打法”的正式语言，同时把版本纪律收口到当前 Phase4 语义。

完成标准：

1. boss 不再只有“换皮 + 轻微权重偏置”
2. `gradle.properties`、client title、sample pack `gameVersionRange` 对齐到当前阶段口径

## 2. 为什么把这两件事放一起

1. 它们都属于“Phase4 尾端的正式收口”，不是地基重做。
2. boss phase identity 与 version discipline 都在“玩家感知 / 交付口径”这一层收尾，而不在奖励主链上。
3. frontstage typed contract 已经被确认会引入新的 snapshot/action 抽象族，不再适合继续塞在这个 PR 里。

## 3. 当前问题拆解

### 3.1 boss 源数据本身偏薄

当前 [game/src/main/resources/data/boss-variants/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/boss-variants/index.yaml) 只有 3 个 variant，且无 `phaseOverrides`。

### 3.2 变体语义有的地方并未真正落地

`abyssal_eclipse` 这类 terrain preference 当前仍存在“写了偏好，但 zone 实际不可用”的风险，导致 source-level 语义和 fight 体感脱节。

### 3.3 版本口径漂移

文档语义已进入 Phase4，但 runtime/build/sample pack 仍是 `0.1.0` 口径。

## 4. 必须冻结的合同

1. 不引入新的 tactical AI planner。
2. 不新增大 UI 系统。
3. 版本对齐必须保持 pack boundary 单一真源，不搞兼容分叉。
4. frontstage action cue 正式合同改造不在本 PR 内完成，单独由 `PR-05` 承接。

## 5. 范围与非目标

### 5.1 范围

1. boss phase / variant source-level 强化
2. boss harness / owner metric 强化
3. version/build/sample pack 对齐

### 5.2 非目标

1. 不做 frontstage typed action cue contract
2. 不做完整 Phase5 replay 产品化
3. 不开新资源批次
4. 不引入新大玩法系统

## 6. 技术方案

### 6.1 boss phase identity

第一轮目标：

1. 保持现有 3 个 base encounter 不变
2. 每个 boss 至少支持 2 阶段
3. 通过 `phaseOverrides` 或等价阈值合同引入新 action pool
4. 对 `abyssal_eclipse` 这类 terrain preference 不落地的变体做语义校正

### 6.2 boss harness / owner metric 强化

至少补三件事：

1. `phaseTransitionObservedRatio` 进入 owner metric
2. `variant/base trace divergence` 设最低阈值，不再只验证 parity
3. terrain preference 落地率必须进入 boss harness 摘要

### 6.3 版本纪律对齐

需要统一：

1. `gradle.properties`
2. `DesktopLauncher`
3. `ktome-build.properties` 生成链路
4. `examples/content-packs/sample.flooded_relics/manifest.yaml`

## 7. 推荐改动面

1. `game/src/main/resources/data/boss-variants/index.yaml`
2. `game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt`
3. `tools/src/main/kotlin/com/ktome/tools/phase4/*`
4. `gradle.properties`
5. `client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt`
6. `examples/content-packs/sample.flooded_relics/manifest.yaml`
7. 相关 phase/review 文档

## 8. 任务拆解

### Task 1：boss phase source-level 强化

- **目标**: 让 boss 有阶段语言，不再只是轻微换皮
- **验收**:
  - `phaseTransitionObservedRatio` 可统计
  - `variant/base trace divergence` 达到阈值

### Task 2：version discipline 对齐

- **目标**: 让 Phase4 的 pack boundary 口径正式统一
- **验收**:
  - runtime/build/sample pack 版本范围一致
  - 文档引用不再漂移

## 9. 推荐命令

```bash
./gradlew bossHarness
./gradlew clientSmoke
./gradlew contentPackHarness
./gradlew verifyOwner
./gradlew phase4Report
```

## 10. 这是 Phase4 收口前的倒数第二类 PR

只有当下面两条都成立，且后续 `PR-05` 完成，Phase4 才适合真正进入下一阶段：

1. boss 不再是“只有血量和掉落”的终盘验收
2. 版本纪律和 sample pack 口径已经收口，不再让 Phase4 的 pack boundary 自己漂移

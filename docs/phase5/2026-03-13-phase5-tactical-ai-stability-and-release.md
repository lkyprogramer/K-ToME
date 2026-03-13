> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`

# Phase 5 - Tactical AI, Stability & Release

**阶段**: `Phase 5`  
**版本目标**: `v1.0.0`  
**优先级**: `P1`  
**前置条件**: `Phase 4` 出口全部满足  
**对应问题**: 到 Phase 4 为止，游戏应当已经“系统齐全、内容可玩”，但还不等于“能发布”。Phase 5 只处理发布必须项，不再引入大新系统。

---

## 1. 阶段目标

把 K-ToME 收口到真正可发布的单机版本。

完成标准：

1. 精英/Boss 的战术 AI 与感知系统达到清晰可感知提升。
2. 建立 `perf smoke`、`soak`、`run history`、`death analysis`、`replay` 的稳定工具链。
3. 做完 `Localization QA`、`Accessibility QA`、安装验证和发布文档。
4. 形成可验收的 `v1.0.0` 安装包。

## 2. 当前问题

1. Phase 4 解决了内容深度，但发布级稳定性仍未完成。
2. AI 目前仍偏脚本化执行，缺少更强的战术评分层。
3. 长局 soak、死因解释、性能预算和 replay 还缺统一收口。
4. 双语言和可访问性需要正式专项 QA。

### 2.1 本阶段必须冻结的系统

1. `tactical scoring` 接口与执行节点。
2. 感知、仇恨、潜行、最后已知信息状态机。
3. replay schema 与 run history。
4. perf/soak 预算与采样口径。
5. Localization QA 与 Accessibility QA 清单。

## 3. 范围与非目标

### 3.1 范围

1. 战术 AI 与感知/潜行系统。
2. replay、run history、death analysis。
3. perf smoke、profiling、soak。
4. 本地化 QA、可访问性 QA、安装打包、BalanceLab、发布说明。

### 3.2 非目标

1. 不再引入大新玩法系统。
2. 不做完整脚本平台。
3. 不为了追求内容量而破坏回归与发布门禁。

## 4. 技术方案

### 4.1 Tactical AI 在脚本化 AI 之上演进

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/ai/tactical/*
core/src/test/kotlin/com/ktome/core/ai/tactical/*
```

冻结口径：

1. tactical scoring 必须建立在 Phase 3/4 已有 action catalog 与 perception model 上。
2. 不另起一套并行怪物系统。
3. 重点提升对象是精英/Boss，不是把所有小怪都做成复杂 planner。

### 4.2 感知、仇恨与潜行

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/perception/*
core/src/test/kotlin/com/ktome/core/perception/*
```

冻结口径：

1. AI 只能基于可见信息、声音/事件提示和最后已知位置行动。
2. 潜行、脱战、重新索敌、仇恨焦点都必须有可解释状态。
3. `AIDecisionTrace` 必须能说明“为什么此刻追、退、换目标、保留技能”。

### 4.3 Replay、Run History 与 Death Analysis

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/replay/*
client/src/main/kotlin/com/ktome/client/history/*
tools/src/main/kotlin/com/ktome/tools/replay/*
```

冻结口径：

1. replay 只记录语义事件与输入，不记录渲染层状态。
2. 死亡必须能解释：
   - 最后击杀来源
   - 关键状态
   - 最近几回合伤害轨迹
3. run history 必须支持本地回看和 QA 取证。

### 4.4 Perf / Soak / Profiling

建议文件与模块：

```text
client/src/main/kotlin/com/ktome/client/perf/*
tools/src/main/kotlin/com/ktome/tools/perf/*
```

冻结口径：

1. 必须给出明确预算：
   - 首屏加载
   - 平均帧时
   - 长局内存
   - atlas/audio 句柄
2. soak 必须覆盖 `8~10` 小时 run。
3. perf smoke 失败不能被当成可忽略噪音。

### 4.5 QA 与发布收口

建议文件与模块：

```text
docs/releases/*
tools/src/main/kotlin/com/ktome/tools/release/*
```

冻结口径：

1. 本地化 QA 必须覆盖主界面、战斗、背包、长文本、换行和占位符。
2. 可访问性 QA 至少覆盖字体大小、对比度、日志可读性、色彩依赖提示。
3. 发布包必须有安装验证、已知问题清单和操作说明。

## 5. 推荐 PR / 工作包拆分

### P5-W1 Tactical Scoring Layer

1. 战术评分接口
2. 精英/Boss 决策增强
3. AI trace 扩展

### P5-W2 Perception / Hate / Stealth

1. 感知状态机
2. 仇恨焦点
3. 最后已知信息
4. 潜行与脱战

### P5-W3 Perf & Soak

1. perf smoke
2. profiling budget
3. soak harness

### P5-W4 Replay & Death Analysis

1. run history
2. replay
3. death report
4. QA 取证出口

### P5-W5 Release Closure

1. Localization QA
2. Accessibility QA
3. BalanceLab
4. 安装包与发布文档

## 6. 测试与自证

### 6.1 必测模块

1. `core.ai.tactical`
2. `core.perception`
3. `core.replay`
4. `client.history`
5. `client.perf`
6. `tools.perf`

### 6.2 必测行为

1. 精英/Boss 的战术决策在固定场景下稳定提升。
2. 感知、仇恨、潜行状态可解释且可回归。
3. replay 可复现关键 run。
4. death analysis 能解释最近关键致死链。
5. soak 过程中无明显内存泄漏、句柄泄漏和灾难性性能退化。
6. 中英双语与可访问性检查全部通过。

### 6.3 自动化命令

```bash
./gradlew test
./gradlew :core:test
./gradlew tacticalAiHarness
./gradlew replayHarness
./gradlew perfSmoke
./gradlew soakRun
./gradlew localizationQa
./gradlew accessibilityQa
./gradlew packageRelease
```

### 6.4 白盒验证

1. 固定 Boss 对局，观察战术 AI 的走位、留技能、换目标是否可感知提升。
2. 做一次潜行接敌与脱战，确认 AI 基于最后已知信息而不是全图透视。
3. 读取一局失败 run 的 death analysis，确认死因能解释。
4. 从安装包启动，完成一次从开局到结束的基本流程。

## 7. 出口门禁

1. tactical AI harness 稳定。
2. `8~10` 小时 soak 稳定。
3. replay 可复现，death analysis 可解释。
4. Localization QA 与 Accessibility QA 全绿。
5. 安装包、已知问题、操作说明和发布文档齐全。

## 8. 风险与止损

1. 如果 tactical AI 破坏可解释性，优先退回脚本化规则并保留评分层最小集。
2. 如果 replay 成本过高，优先保语义重放，不引入渲染帧录制。
3. 如果 soak 或 perf 长期不稳定，停止补内容，优先做资源装载和内存收口。
4. 如果 Localization QA 和 Accessibility QA 仍有大面积问题，不允许封版。

## 9. 当前状态

1. 本文已把 Phase 5 目标、边界、工作包和门禁具体化。
2. 可直接作为 `v1.0.0` 封板前的执行文档。
3. 当前尚未开始代码实现，perf/soak/replay/release 入口仍需建设。

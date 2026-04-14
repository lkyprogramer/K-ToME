# Phase 5 Roadmap（入口）

## 1. Phase 5 主题

`Phase 5` 的主题是把已经稳定的规则和内容提升到发布级：战术 AI、感知/潜行、性能、回放、QA、安装包与发布资料全部收口。

目标主线：

`系统稳定 -> 战术 AI -> perf/soak/replay -> QA 清盘 -> v1.0.0`

## 2. 检查点摘要

| checkpoint | 主题 | 最小交付 | 进入条件 | 退出条件 |
| --- | --- | --- | --- | --- |
| `P5-A` | 战术 AI 与感知 | `TacticalScoringLayer`、`PerceptionState`、`HateFocus`、`TacticalAIDecisionTrace` | `Phase 4` 出口满足 | 固定场景 AI 决策与感知合同稳定 |
| `P5-B` | 稳定性与取证 | `ReplayHeader`、`RunHistoryEntry`、`DeathAnalysis`、`SoakReport`、QA harness | `P5-A` contract 稳定 | replay 可复现、死因可解释、perf/soak 有量化证据 |
| `P5-C` | 发布收口 | `BalanceLab`、`packageRelease`、已知问题、安装验证、release docs | `P5-B` 门禁满足 | `v1.0.0` 安装包可验收并进入封板态 |

## 3. 并行开发线

1. `Rules/AI Lane`
   - `TacticalScoringLayer`
   - `PerceptionState`
   - `HateFocus`
   - `TacticalAIDecisionTrace`
2. `Tools/QA Lane`
   - `tacticalAiHarness`
   - `replayHarness`
   - `perfSmoke`
   - `soakRun`
   - `BalanceLab`
   - `reportPhase5`
   - `reportPhase5Only`
   - 统一白盒验证框架接入（`whiteBoxTacticalAi`、`whiteBoxReplay`、`whiteBoxPerf`、`whiteBoxSoak` 等）
3. `Release Lane`
   - Localization QA
   - Accessibility QA
   - package / install verification
   - known issues / release notes

## 4. 工作包与依赖

| 工作包 | checkpoint | 依赖 | 说明 |
| --- | --- | --- | --- |
| `P5-W1` | `P5-A` | `Phase 4` | tactical scoring、`AIContext` 扩展、`TacticalAIDecisionTrace` |
| `P5-W2` | `P5-A` | `P5-W1` | 感知/仇恨/潜行/最后已知位置 |
| `P5-W3` | `P5-B` | `P5-W1` | perf smoke、profiling budget、基线 soak |
| `P5-W4` | `P5-B` | `P5-W1`, `P5-W2` | replay、run history、death analysis、Localization/Accessibility QA |
| `P5-W5` | `P5-C` | `P5-W1` ~ `P5-W4` | `BalanceLab`、package、release docs、Gold Master 封板 |

执行原则：

1. `P5-W3` 可以在 `P5-W1` 完成后先起 perf/soak 基线，但 `P5-W2` 合入后必须重测。
2. `P5-W4` 不允许在没有 `TacticalAIDecisionTrace` 与 `PerceptionState` 正式合同的情况下先写临时 replay/death schema。
3. `P5-W4` 可以与 `P5-W3` 并行推进；`P5-B` 退出仍要求 replay/death 与 perf/soak 同时通过。
4. `P5-W5` 的 `BalanceLab` 复用 `longRunLab` 与 `LootBalanceLab` 的 batch 基础设施，不新建并行 runner。
5. `Phase 5` 的 verification/task/report wiring 统一受 [2026-04-14-phase5-unified-verification-and-development-spec.md](./2026-04-14-phase5-unified-verification-and-development-spec.md) 约束；后续不得再引入 phase 专用第二套 registry。
6. `reportPhase5` 是 `Phase 5` 的 canonical aggregate gate，默认产物路径固定为 `tools/build/reports/verification/phase5/report-phase5-summary.{json,md}`；`reportPhase5Only` 只做 artifact-only rebuild。
7. 若较早的跨 phase 文档仍出现 `phase5Report` 命名，以 `docs/phase5/*` 当前权威链为准；`Phase 5` 正式命令与证据路径统一使用 `reportPhase5`。
8. 后续若需要新增 sibling aggregate/report task、兼容 alias 或 report-only 入口，必须复用 shared helper 或 declarative generation path，并用 semantic build contract test 锁定 canonical / alias / fallback 行为。

## 5. 进入与退出摘要

进入 `Phase 5` 前：

1. `Phase 4` 的 `TerrainTag`、`BossEncounterDef`、`AI_SHIFT`、`ContentPackManifest` 与 `LootBudget` 已冻结。
2. `Phase 3` 的 `CombatTrace`、`STEALTH / TAUNT`、`Boss phase` 与 telegraph 合同已稳定。

`Phase 5` 退出时：

1. 战术 AI、感知/潜行和 replay/死因分析都有固定 schema 与 harness。
2. perf/soak、Localization QA、Accessibility QA 和 content pack compatibility 全部通过。
3. 安装包、已知问题、操作说明、验证说明和 release notes 齐全。
4. `Phase 5` 的固定场景域白盒验证统一接入项目级 white-box framework，并保留最小人工校准动作。

## 6. 文档索引

1. [2026-03-13-phase5-tactical-ai-stability-and-release.md](./2026-03-13-phase5-tactical-ai-stability-and-release.md)
2. [2026-03-13-phase5-regression-checklist.md](./2026-03-13-phase5-regression-checklist.md)
3. [2026-04-14-phase5-unified-verification-and-development-spec.md](./2026-04-14-phase5-unified-verification-and-development-spec.md)
4. [../2026-04-04-unified-white-box-verification-framework.md](../2026-04-04-unified-white-box-verification-framework.md)
5. [../2026-03-13-phase2-to-phase5-final-roadmap.md](../2026-03-13-phase2-to-phase5-final-roadmap.md)
6. [../2026-03-13-core-systems-design-and-phase-supplements.md](../2026-03-13-core-systems-design-and-phase-supplements.md)

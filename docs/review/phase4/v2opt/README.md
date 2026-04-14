# Phase 4 V2OPT PR 文档索引

本目录把 [2026-04-11-phase4-v2opt-verified-next-pr-plan.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md) 进一步展开为可直接执行的独立 `PR` 级开发文档。

## 拆分与工作量重估

| PR | 标题 | 主要模块 | 工作量评估 | 资源策略 | 整合结论 |
| --- | --- | --- | --- | --- | --- |
| `V2OPT-PR-01` | Experience Gate 与 Owner Metric 硬化 | `tools`, `game` harness | `L`（`3~4` 人日） | 不新增图片/音频；纯 gate/report PR | 保留为单 PR；只做指标合同和报告 owner，不混内容调优 |
| `V2OPT-PR-02` | 成长身份救火与职业化掉落分发 | `game`, `tools`, 少量 `core` 接线 | `XL`（`6~8` 人日） | 不新增图片/音频；复用现有 item 资源 | 必须把掉落分发、base item 底盘和 long-run owner metric 放在同一 PR；拆开会形成错误中间态 |
| `V2OPT-PR-03` | Secret Reward 身份重建与 Organic Hidden 闭环 | `game`, `tools`, 少量 `client` 提示 | `L`（`4~6` 人日） | 不新增图片/音频；复用现有 hidden/secret 资源 | 必须把 reward identity 和 organic hidden 放在同一 PR；只修其一没有体验意义 |
| `V2OPT-PR-04` | Terrain / Mutation 语义收口与主题防稀释 | `game`, `tools`, 少量 `client` 文案 | `M-L`（`3~5` 人日） | 不新增图片/音频；复用现有 terrain/mutation 资源 | 保持单 PR；只修语义、权重和主题约束，不混前台抛光 |
| `V2OPT-PR-05` | 前 30 分钟 Replay Hook 与 Frontstage Readability | `game`, `client`, `tools` | `M`（`2~4` 人日） | 不新增图片/音频；复用现有 terrain/mutation/hidden/item 资源并前台化 | 保持单 PR；聚焦前期 mapgen 记忆点和前台反馈，不再动深层掉落/隐藏合同 |

## 依赖顺序

```text
V2OPT-PR-01
    ↓
V2OPT-PR-02
    ↓
V2OPT-PR-03
    ↓
V2OPT-PR-04
    ↓
V2OPT-PR-05
```

## 文档列表

1. [2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md)
2. [2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md)
3. [2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md)
4. [2026-04-11-phase4-v2opt-pr-04-terrain-mutation-semantics-and-theme-hardening.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-04-terrain-mutation-semantics-and-theme-hardening.md)
5. [2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md)

## 统一原则

1. 默认不保兼容，优先长期合同清晰。
2. 每个 PR 都必须自带 owner metric 与 exit gate。
3. 不把 `Phase 5` 事项提前塞回 `Phase 4` follow-up。
4. 不接受“先做内容，验证以后补”的中间态。

## PR-06 之后的统一验证约束

1. `phase4Report` 现在是默认 canonical aggregate gate，产物路径固定为 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`。
2. `verifyOwner` 是后续 `V2OPT` PR 的默认联合验收入口；默认开发回路先跑 `verifyChanged`，提交前跑 `verifyOwner + phase4Report`。
3. `reportPhase4` 只用于 verification contract、baseline、aggregation schema 变更后的显式 parity 对账；它不是默认日常 gate。
4. `phase4LegacyReport` / `phase4LegacyReportOnly` 只保留为手工 fallback；后续 PR 文档不得再把 legacy `phase4-summary.{json,md}` 写成默认验收产物。
5. 本目录里凡是引用 `phase4-summary.{json,md}` 的旧报告快照，都只代表历史证据，不代表新的默认输出路径。
6. 后续若需要新增或调整 sibling aggregate/report task、alias 或 report-only 入口，必须复用统一 helper / declarative generation path，不得再复制一组新的 `tasks.register<Test>` block。
7. 相关 build contract test 必须直接断言 helper 参数与 canonical/parity/fallback 语义，不能再把“保留多段近似 DSL 文本”当成长期 contract。
8. 若 canonical aggregate 与 parity 入口共用输出目录，普通 canonical 运行后不得残留 parity-only artifact；否则该 PR 不算完成默认 gate 约束。

## 资源结论

`V2OPT` 这轮 follow-up **不再开新的图片/音频生成批次**。  
原因不是忽略前台，而是当前仓库已经有足够的 canonical 资源族；继续新开批次只会把系统/语义问题误包装成资源问题。

统一复用基线：

1. item 资源：`assets-src/image/specs/phase4-pr05-gemini-plan.yaml`、`assets-src/audio/specs/phase4-pr05-audio-plan.yaml`
2. terrain / mutation 资源：`assets-src/image/specs/phase4-pr06-gemini-plan.yaml`、`assets-src/audio/specs/phase4-pr06-audio-plan.yaml`
3. hidden / secret 资源：`assets-src/image/specs/phase4-pr07-gemini-plan.yaml`、`assets-src/audio/specs/phase4-pr07-audio-plan.yaml`
4. 深铁走私 stash 补充资源：`assets-src/image/specs/phase4-opt-pr05-gemini-plan.yaml`、`assets-src/audio/specs/phase4-opt-pr05-audio-plan.yaml`
5. 额外 mutation 资源：`assets-src/image/specs/phase4-opt-pr02-gemini-plan.yaml`、`assets-src/audio/specs/phase4-opt-pr02-audio-plan.yaml`

执行约束：

1. 若某个 `v2opt` PR 实现过程中声称“必须新增 visual/audio key 才能完成”，默认视为 PR 边界切错。
2. `v2opt` 文档里的“资源计划”是 **复用与接线计划**，不是新一轮 raw asset 生成任务。

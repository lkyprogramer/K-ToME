# Phase 3 Roadmap（入口）

## 1. Phase 3 主题

Phase 3 的主题是把 `Phase 2` 的正式合同升级为“有战斗深度、有职业构筑、有长局结构”的完整主线。

目标主线：

`短局可玩 -> 正式战斗公式 -> 职业树/状态/AI DSL -> 4~6 小时长局`

## 2. 文档索引

### 设计与验证

1. [2026-03-13-phase3-deep-combat-classes-and-long-run.md](./2026-03-13-phase3-deep-combat-classes-and-long-run.md)
2. [2026-03-13-phase3-verification-checklist.md](./2026-03-13-phase3-verification-checklist.md)
3. [../2026-03-13-phase2-to-phase5-final-roadmap.md](../2026-03-13-phase2-to-phase5-final-roadmap.md)
4. [../2026-03-13-core-systems-design-and-phase-supplements.md](../2026-03-13-core-systems-design-and-phase-supplements.md)

### PR 级开发文档（P3-W1 ~ P3-W6）

5. [2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md](./2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md) — P3-W1 战斗公式 V2、Resolution Trace 与 Golden Corpus
6. [2026-03-13-phase3-pr-02-status-lifecycle.md](./2026-03-13-phase3-pr-02-status-lifecycle.md) — P3-W2 状态生命周期
7. [2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md](./2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md) — P3-W3 天赋树 V2、动态说明与 talent-local effect 去类型化
8. [2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md](./2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md) — P3-W4 AIProfile DSL 与 BossEncounter
9. [2026-03-13-phase3-pr-05-class-formalization.md](./2026-03-13-phase3-pr-05-class-formalization.md) — P3-W5 职业正式化
10. [2026-03-13-phase3-pr-06-long-run-world-structure.md](./2026-03-13-phase3-pr-06-long-run-world-structure.md) — P3-W6 长局世界结构

## 3. 权威层级

1. `P3-W1 ~ P3-W6` 是 Phase 3 的统一验收编号。
2. [2026-03-13-phase2-to-phase5-final-roadmap.md](../2026-03-13-phase2-to-phase5-final-roadmap.md) 拥有阶段边界、工作包和出口门禁权威。
3. [2026-03-13-core-systems-design-and-phase-supplements.md](../2026-03-13-core-systems-design-and-phase-supplements.md) 拥有公式、数据结构和系统合同权威。
4. 各 PR 级开发文档（PR-01 ~ PR-06）拥有对应工作包内的详细执行权威。
5. 旧编号与新编号冲突时，以 `P3-W1 ~ P3-W6` 为准。
6. 若 PR 级文档与上游权威文档冲突，必须先修上游权威，再回写 PR 文档；禁止就地下游 patch 出第二套长期口径。

## 4. 执行说明

1. 实施上默认按 `Rules / Client / Content / Tools-QA` 四条 lane 并行推进，不按单链串行等待。
2. 具体并行拆分与依赖，以 [2026-03-13-phase3-deep-combat-classes-and-long-run.md](./2026-03-13-phase3-deep-combat-classes-and-long-run.md) 第 5 节为准。
3. `P3-W1` 只冻结 `formula corpus + CombatResolutionTrace + TraceEnvelope`，`status / integration / long-run` corpus 由后续工作包接管。
4. `P3-W5` 默认内部拆成 `W5a / W5b / W5c`；`P3-W6` 默认内部拆成 `W6a / W6b / W6c`。

## 5. 依赖拓扑

```text
P3-W1 (PR-01: 公式 / ApplicationPolicy / Resolution Trace)
  ├── P3-W2 (PR-02: 状态生命周期 / Effect Carrier)
  ├── P3-W3 (PR-03: Talent Schema / DescriptionModel / AllocationDraft / talent-local effect 去类型化)
  │       └── P3-W4 (PR-04: AIProfile / TelegraphSpec / BossEncounter)
  │                    └── P3-W5 (PR-05: W5a Rules -> W5b Content -> W5c Client+QA)
  │                              └── P3-W6 (PR-06: W6a World -> W6b Economy/Affix -> W6c LongRunLab)
  └── P3-W3 的 schema/说明建模可在 W1 后并行启动，但依赖状态语义的最终验收必须在 W2 口径冻结后收口
```

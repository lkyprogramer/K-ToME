# Phase4 v4 PR Development Governance

本文是 `docs/review/phase4/v4-pr/` 后续开发的 repo-owned 执行合同。它只定义流程、制度和验证成本控制规则；具体玩法、数值、schema 和 owner metric 仍以各 PR 文档为准。

## 1. 目标

后续 v4 PR 不再把重型 owner gate 当作需求理解和调参反馈循环。每个非 trivial PR 必须先把文档合同结构化成可验证清单，再用快速测试发现低层错误，最后用 owner producer、report gate 和 `verifyChanged` 收口。

固定目标：

1. 文档中的每个 MUST、完成定义、blocking metric、report field 都能追到 fast check、owner gate 或 canonical artifact。
2. 重型 gate 只做最终闭环证明，不承担反复试错。
3. fixture / report diff 只体现合同变化，不包含 timestamp、cache status、本机路径或其他 volatile 字段。
4. PR-03 作为本规范 canary；PR-03 未按本规范完成验收前，PR-04 到 PR-07 不得声称已经继承该制度。

## 2. Acceptance Matrix

每个 PR 文档必须包含 `## 0. 开发治理与验收矩阵`，并至少给出以下字段：

| Field | Required | Meaning |
| --- | --- | --- |
| `requirementId` | yes | 稳定需求编号，例如 `PR03-M01` |
| `source` | yes | PR 文档中的章节、完成标准或 metric table |
| `owner` | yes | `core` / `game` / `client` / `tools` / `docs` |
| `fastCheck` | yes for blocking behavior | 快速单测、lint 或静态检查 |
| `ownerGate` | yes for owner behavior | producer、harness、report gate 或 `N/A` |
| `artifact` | yes for report/evidence | repo-relative canonical artifact path |
| `whitebox` | yes | `required` / `skipped` / `N/A` |

执行规则：

1. blocking 行为不能只有 aggregate metric；必须至少有一个 fast check 或 owner gate。
2. report-only 指标不能证明 blocking 行为。
3. `artifact` 必须 repo-relative；禁止 `/Users/...`、`/tmp/...`、`C:\...`。
4. `whitebox=skipped` 必须写明原因和剩余风险。
5. 表中不得出现 `TBD owner`、`TBD gate`、`TBD artifact`。

## 3. Gate Ladder

所有非 trivial v4 PR 固定按以下顺序执行：

1. `acceptanceContractLint`
   - 检查文档是否具备验收矩阵、Gate Budget、canonical artifact 说明和失败规则。
2. fast lane
   - 运行对应 PR 的 focused unit / lint。
   - fast lane 失败时先修 fast check，不直接跳到重型 gate。
3. owner producer
   - 只在 fast lane 通过后运行，例如 `whiteBoxLoot`、`lootBalanceLab`、`longRunLab`、`bossHarness`、`contentPackHarness`。
4. report gate
   - `reportPhase4Only` 与 `reportPhase4` 必须读取同批 producer artifact。
5. governance gate
   - non-trivial Kotlin、gate wiring 或治理改动必须运行 `maintainabilityLint`。
6. final closure
   - 最后运行 `verifyChanged`。
   - `verifyChanged` 仍是最终 PR CI 权威；本规范不引入第二套 impact routing。

Gradle 必须串行执行；不得在多个终端或多个 agent 并发运行 Gradle。

## 4. Gate Budget

每个 PR 文档必须声明 Gate Budget：

1. 预计触发的重型任务。
2. 触发原因。
3. producer freshness 要求。
4. 最近耗时来源或 `build/verification/verify-changed/full-task-duration-summary.{json,md}` 的读取方式。

失败复盘阈值：

1. 同一重型 gate 失败超过 `2` 次，必须先写出失败原因、缺失的 fast check 和防复发动作。
2. 单轮本地验证超过 `90` 分钟，必须先判断是否把重型 gate 用成了调试循环。
3. 复盘结果必须回写到 PR 文档、implementation review 或 PR 描述，不得只留在聊天记录。

## 5. Canonical Artifact

长期 artifact 分两类：

1. raw evidence
   - 保存 producer 的原始调试信息。
   - 可以包含 cache 诊断、执行耗时、临时 trace。
2. canonical artifact
   - 作为 fixture、report gate 或 owner evidence 的合同输入。
   - 必须剥离 volatile 字段。

canonical artifact 禁止包含：

1. timestamp。
2. kernel cache status。
3. shard reuse count。
4. 本机绝对路径。
5. runtime-home 绝对目录。
6. 与玩法、schema、owner metric 无关的执行环境字段。

debug sample 必须按语义分桶选择，而不是偶然 top-N 排序。建议 bucket：

1. `selected`
2. `topRejected`
3. `penaltyApplied`
4. `qualityFloor`
5. `tieBreak`
6. `blockedByRule`

## 6. Doc-Vs-Implementation Self-Audit

PR 收口前必须逐条审计：

1. PR 文档完成定义是否已实现。
2. 每个 requirement 是否有测试、owner gate 或 canonical artifact 证明。
3. report field 是否进入 canonical owner evidence。
4. fixture diff 是否只包含合同变化。
5. 未执行项是否真实记录。
6. 人工白盒若被跳过，是否写明 `whitebox=skipped`、原因、替代证据和剩余风险。

## 7. PR-03 Canary

PR-03 是本规范 canary，必须严格执行：

1. PR-03 文档必须有完整 Acceptance Matrix。
2. PR-03 必须先跑 `acceptanceContractLint`。
3. PR-03 的 fast lane 失败时，先补 fast check，再进入 owner producer。
4. PR-03 的 `whiteBoxLoot`、`lootBalanceLab`、`longRunLab`、`verifyOwner`、`reportPhase4Only`、`reportPhase4` 必须同批刷新 producer evidence。
5. PR-03 最后运行 `verifyChanged`。
6. 如果本轮明确不做人工白盒，PR-03 的验收矩阵必须写 `whitebox=skipped`，并标明原因是用户要求不进行人工白盒测试。

## 8. PR-04 To PR-07 Inheritance

PR-04 到 PR-07 不复制本文件内容，只在各自文档中链接本文件并维护自己的 Acceptance Matrix。

继承关系固定：

1. PR-04 继承 PR-03 canary 后的治理流程。
2. PR-05 串行依赖 PR-04 的 zone trigger 产物。
3. PR-07 串行依赖 PR-04 的 `secretZoneSelector.primarySlot / secondarySlot`。
4. PR-06 排在 PR-03、PR-04、PR-07 之后，读取最终 owner 字段和 route / content-pack summary。

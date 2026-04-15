# K-ToME AI Change Governance

## 1. 直接结论

K-ToME 不再把 “AI 代码质量” 只当成一次性 prompt 技巧。

从本文件生效起，统一采用三层治理：

1. **repo-owned contract**
   - 由本文件定义什么算结构性膨胀、什么情况下必须改 owner / contract / boundary，而不是继续堆 patch。
2. **Codex skill**
   - `ktome-change-discipline` 负责作者侧约束。
   - `ktome-code-review` 负责审查侧约束。
   - skill 只能引用本文件，不得复制一套平行规则。
3. **deterministic gate**
   - `maintainabilityLint` 只拦高信号、可稳定判定的反模式。
   - 语义判断和设计裁决仍由 review 负责，不把 gate 做成噪声机。

本文件是仓库权威。若 skill、prompt、review 模板与本文件冲突，以本文件为准。

---

## 2. 目标与非目标

### 2.1 目标

1. 阻止 “为了让当前 diff 过关” 的补丁式实现进入主干。
2. 强制优先修正 owner、typed contract、模块边界，而不是新增 flag、兼容分支或第二条 path。
3. 让 review 结论、lint finding、baseline debt 使用统一 taxonomy，避免各说各话。
4. 把 repo-specific 规则版本化在仓库内，而不是散落在个人 prompt、临时 review 评论或本地记忆里。

### 2.2 非目标

1. 不把所有设计质量问题都交给静态 gate 自动判断。
2. 不要求 v1 立刻消灭所有历史债务；现有债务进入 baseline，新增债务 fail fast。
3. 不引入第二套 verification contract、第二套 baseline schema 或第二套 repo 规则说明。

---

## 3. 固定 Finding Taxonomy

后续所有 anti-bloat 审查结论，必须优先落到以下 7 类之一：

| taxonomy | 含义 | 当前默认处理 |
| --- | --- | --- |
| `option-sprawl` | 用 `Boolean` 参数、默认参数矩阵、过多参数或切换开关绕开建模问题 | review 必查；部分高信号子项进入 `maintainabilityLint` |
| `helper-sprawl` | 新增 `Helper / Utils / Manager` 一类业务中转层，增加调用链却没有降低复杂度 | review 必查；高信号子项进入 `maintainabilityLint` |
| `branch-patch` | 通过堆 `if/else`、compat 分支、例外分支修当前症状，而不是修边界/模型 | review 必查 |
| `second-authority` | 同一语义出现第二真源、第二条 code path、第二份状态或第二套判断口径 | review 阻塞项 |
| `stringly-contract` | 本应是 typed model / enum / schema / boundary 的语义，被 raw string / raw map / ad-hoc key 承载 | review 必查 |
| `temp-path-without-expiry` | 临时逻辑、兼容分支、`TODO/TEMP/HACK/compat` 注释没有 debt id 与删除条件 | review 必查；高信号子项进入 `maintainabilityLint` |
| `test-gap` | 非平凡逻辑变更未同步交付测试、harness、lint 或 white-box closure | review 必查 |

禁止在 review 里自创新的长期 taxonomy 名称。如果确有必要新增分类，必须先更新本文件。

---

## 4. 作者侧约束

任何 non-trivial Kotlin 改动，作者在动手前必须明确：

1. **目标行为**
   - 这次到底要修什么、加什么、收敛什么。
2. **owner 模块**
   - 这段逻辑为什么属于 `core` / `game` / `client` / `tools`。
3. **命中的稳定合同**
   - 是否触碰 save/schema/event/snapshot/report/baseline/replay 等长期合同。
4. **change shape**
   - 只能优先从以下 4 种中选：
   - `inline simplify`
   - `extract typed model`
   - `extend boundary`
   - `content/schema wiring`
5. **deletion budget**
   - 非 trivial 改动必须说明这次删掉或收敛了什么。
   - 如果只能增加、没有减少任何复杂度，必须写明原因。

### 4.1 默认禁止的 shortcut

以下做法默认禁止，除非在评审或设计文档里显式说明理由：

1. 新增 `Boolean` 参数绕逻辑。
2. 新增默认参数矩阵，让调用方自己猜组合语义。
3. 新增 `Helper / Utils / Manager` 承载业务编排。
4. 新增 compat 分支或第二条 path 维持旧行为。
5. 在 `client` / `tools` 塞入运行时规则逻辑。
6. 为了通过当前测试，临时引入第二真源或一次性补丁状态。

---

## 5. Reviewer 决策口径

reviewer 先判断是不是“应该改模型/改边界”，再判断是否允许局部 patch。

### 5.1 必须优先改模型或边界的场景

满足以下任一项，默认不能接受局部 patch：

1. 改动引入或维持 `second-authority`。
2. 改动新增 `option-sprawl`，但根因是 owner/contract 未建模。
3. 改动通过 `branch-patch` 堆分支，新增分支数明显多于新增行为。
4. 改动把 typed contract 退化成 `stringly-contract`。
5. 改动让 call graph 更长，但复杂度没有下降。

### 5.2 允许局部 patch 的最小条件

只有同时满足以下条件，才允许局部 patch：

1. root cause 已清楚，且 patch 不会引入第二真源。
2. patch 不新增 option matrix、compat path 或 helper-like business layer。
3. patch 保持当前 owner 边界清晰，没有把规则挪错层。
4. patch 交付了对应测试或验证入口。

### 5.3 默认严重度

1. `P1`
   - `second-authority`
   - 严重 `option-sprawl`
   - 边界泄漏
   - 稳定合同漂移
2. `P2`
   - `helper-sprawl`
   - `branch-patch`
   - `test-gap`
   - 非核心但正在扩散的 `stringly-contract`

---

## 6. Deterministic Gate：`maintainabilityLint`

### 6.1 任务定位

`maintainabilityLint` 是仓库级高信号 anti-bloat lint，v1 定位为 **quick preflight domain**。

固定约束：

1. root Gradle 入口：`./gradlew maintainabilityLint`
2. 产物目录：`tools/build/reports/verification/maintainability/`
3. 固定输出：
   - `summary.json`
   - `findings.json`
   - `report.md`
4. baseline 文件：`maintainability-baseline.json`
5. baseline 只豁免**已有**债务；新增债务必须 fail fast。
6. gate 默认是 blocking，但必须允许通过配置切到 report-only，仅产出报告、不阻断调用链。

### 6.2 v1 阻塞规则

v1 只拦 5 条高信号、低歧义规则：

1. **helper-like business type**
   - 新增 `*Helper` / `*Utils` / `*Manager` 类型即记为 `helper-sprawl`
   - `tools`、`build-logic`、测试源码仅允许显式 allowlist
2. **runtime API Boolean parameter**
   - `core / game / client / tools` 正式代码中的 runtime API 出现 `Boolean` 参数即记为 `option-sprawl`
   - DTO / serialization model / fixture 不纳入
3. **parameter matrix growth**
   - public / internal cross-file API 参数数 `> 4` 即记为 `option-sprawl`
   - 推荐用 typed request / config object 收敛
4. **temporary path without expiry**
   - 新增 `TODO | TEMP | HACK | compat` 注释，如果没有 `debt(<id>)` 与删除条件，即记为 `temp-path-without-expiry`
5. **baseline debt gate**
   - 当前扫描结果与 `maintainability-baseline.json` 对比
   - 新增 debt = fail

### 6.3 v1 不做 hard block 的项

以下问题保留在 review 判断，不进 v1 hard block：

1. `stringly-contract` 扩散
2. `branch-patch` 增长
3. 非平凡逻辑改动未带测试的 `test-gap`

原因：

1. 这三类需要更强语义判断。
2. v1 优先控制误报和执行成本，不把 gate 做成“逢改必红”的噪声源。

### 6.4 接线策略

1. 先接入 `verifyChanged`
2. 连续两轮无明显误报后，再评估是否并入 `unitAndToolsGate`
3. 在此之前，`maintainabilityLint` 不是 phase checklist 的默认重型 gate

---

## 7. Skill 约束

### 7.1 `ktome-change-discipline`

这个 skill 负责作者侧约束，至少必须做到：

1. 复述目标行为、owner 模块和命中的稳定合同
2. 搜索现有 helper / typed model / registry / boundary
3. 在 `inline simplify / extract typed model / extend boundary / content-schema wiring` 中明确选择 change shape
4. 显式声明这次没有引入哪些 shortcut
5. 给出 deletion budget 或增长理由

### 7.2 `ktome-code-review`

这个 skill 负责审查侧约束，至少必须做到：

1. 加载 `docs/rule/ai-change-governance.md`
2. finding 优先使用本文件 taxonomy
3. 区分 phase-blocking 现在必须修的问题与可延期项

skill 不得把 repo-specific 规则复制成另一份长文，只能引用本文件。

---

## 8. 与现有仓库规则的关系

1. Kotlin 代码质量、typed contract、命名、默认参数与布尔参数基本纪律，继续以 [kotlin.md](./kotlin.md) 为基础。
2. 本文件只补 “AI 变更治理 / anti-bloat taxonomy / reviewer 与 gate 口径”。
3. 若 `AGENTS.md`、phase 文档或 verification contract 与本文件交叉，按更严格者执行。

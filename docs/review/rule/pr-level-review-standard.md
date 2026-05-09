# K-ToME PR 级 Review 文档规范

本文定义 K-ToME 后续“基于 PR 文档开发”的 PR 级 review 标准。它适用于 `docs/phase*`、`docs/review/phase*/*/PR`、`docs/opt/ui-pr`、`UI/pr` 以及后续任何以 PR 文档作为开发合同的工作流。

目标不是写一份“看起来完整”的审查报告，而是让 review 结果能直接驱动开发：开发者按报告修改文档或代码时，不需要猜测字段、状态、owner、测试、gate、artifact、fallback 或验收口径。

本规范是 PR 级 review 的通用方法论；具体 PR 系列可以在自家 governance 文档中收敛 owner、gate ladder、artifact 和白盒字段取值，但不能放宽本规范的证据、验真和单一权威要求。

## 1. Review 目标

PR 级 review 必须同时回答四个问题：

1. 这个 PR 文档是否与上游路线图、阶段合同和当前代码一致。
2. 这个 PR 文档是否足够具体，开发者能否不靠猜测直接实施。
3. 这个 PR 的实现路径是否会破坏 `core / game / client / tools` 边界、稳定合同、验证权威或后续 PR。
4. 这个 PR 交付后，玩家体验、玩法闭环、调试证据和自动化验收是否都成立。

对 K-ToME 来说，“PR 文档写通了”不等于“可以开发”。可开发文档必须具备：

1. 明确的 owner。
2. 明确的输入、输出、状态机、nullability、fallback、排序和失败语义。
3. 明确的测试文件、测试名、gate、canonical artifact 和 manual evidence。
4. 明确的非目标、删除计划和跨 PR 继承关系。
5. 与当前代码、资源、manifest、报告和 validation 路径相符。

完整完成定义见 [第 8 节](#8-可开发文档完成定义)。

## 2. 输入优先级

review 前必须按以下顺序确认真源：

1. 仓库规则：`AGENTS.md`、`docs/INDEX.md`、`docs/rule/kotlin.md`、`docs/rule/ai-change-governance.md`、其它 `docs/rule/*`。
2. 上游路线图和阶段合同：`docs/2026-03-13-phase2-to-phase5-final-roadmap.md`、`docs/2026-03-13-core-systems-design-and-phase-supplements.md`、对应 `docs/phase*/roadmap.md`。
3. 当前 PR 系列入口和 governance：例如 `UI/PLAN.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`docs/review/phase4/v4-pr/README.md`。
4. 目标 PR 文档。
5. 当前代码、测试、资源、manifest、fixture、golden、manual record、report artifact。
6. 历史 review 报告。

历史 review 只能作为线索，不能直接作为当前修复合同。吸收历史反馈前必须重新对照当前代码和当前 PR 文档验真。

## 3. Review 前预检

每次 PR 级 review 开始前，必须完成预检摘要：

1. 已确认基准 ref、HEAD 和工作树状态：至少读取 `git status --short --branch`，并确认 review 范围是目标 PR 的 diff，而不是无关 in-progress 改动。
2. 命中的 phase、checkpoint、PR 系列、系列 governance 和目标 PR。
3. 上游权威文档和目标 PR 文档路径。
4. 受影响模块：`core`、`game`、`client`、`tools`、`assets`、`docs`。
5. 是否触碰稳定合同：schema、version、event、snapshot、visual/audio/style manifest、content pack、report、white-box、release gate。
6. 主要风险：第二真源、跨层泄漏、验证绕行、旧 fallback 复活、manual-only 验收、重型 gate 被当调试循环。
7. 是否触发 Kotlin 规则、`maintainabilityLint`、`verifyChanged` impact routing 或 bootstrap 验证。
8. 计划核验入口：focused test、owner gate、lint、golden、manual record、`verifyChanged`。
9. 如果计划运行 Gradle gate，先执行 `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env`，并确认 JDK 与仓库 `.sdkmanrc` 一致。

如果缺失信息会影响主方案，先指出缺口；如果只是局部细节，按最保守假设继续 review，并把假设写入报告。

## 4. Review Pass 顺序

每次完整 review 必须按以下顺序执行。后续增量复审可以聚焦变更区域，但仍必须做一次全局 sanity pass。

### 4.1 Pass 0: 文档路由和范围

检查项：

1. PR 编号、执行顺序、依赖 PR 和非目标是否与入口 README 一致。
2. 是否错误引用旧 `PR01 ~ PR12` 历史编号作为当前执行编号。
3. 是否把历史草稿、旧 review 或临时方案升级成新权威。
4. 是否遗漏上游已经冻结的 phase 目标、边界或验证要求。
5. PR 是否同时引入过多抽象族或触碰超过两个生产模块而没有拆分说明。

常见问题：

1. PR 文档只描述“要做什么”，没有说明“不做什么”。
2. 下游 PR 改写上游合同，而不是先修上游权威。
3. 当前 PR 暗含后续 PR 才能完成的核心体验闭环。

### 4.2 Pass 1: Public Contract Freeze

检查所有会被开发者、测试或后续 PR 消费的合同是否冻结：

1. 类型名、文件名、包名、字段名、枚举值、sealed variant。
2. request / result / snapshot / event / manifest / report schema。
3. nullability：哪些字段可空，空值如何处理，空值是否进入 identity。
4. identity：哪些字段参与 identity，哪些字段禁止参与 identity。
5. source of truth：每个字段只能有一个 primary source。
6. fallback：触发条件、回退目标、是否保留 mode、是否保留 anchor、是否 fail fast。
7. ordering：候选排序、layer 顺序、事件优先级、modal stack 扫描方向。
8. failure semantics：抛错、fail lint、warn only、manual note、report-only。

必须把以下模糊表达标为问题：

1. `或`、`可来自`、`必要时`、`如果需要`，但没有优先级和冲突处理。
2. `新增相关测试`，但没有测试文件、测试名、输入和断言。
3. `保持兼容`，但没有旧 schema、dual-read、删除时间和风险说明。
4. `可选 gate`，但该行为实际是 blocking。
5. `后续优化`，但当前 PR 没有最小可玩或最小可验证闭环。

### 4.3 Pass 2: State Machine Dry Run

对 PR 文档中的每个状态流转做手工推演。通用维度至少覆盖：

1. 正常路径。
2. 空输入、缺字段、缺资源或缺 artifact。
3. enter / update / exit 三段。
4. stale state、previous state、old frame、旧缓存。
5. failure semantics：fail fast、warn only、report-only、manual note。
6. debug / validation / manual probe 与正式 gameplay path 的隔离。

按 PR 类型追加维度：

1. UI / client PR：空 cursor、modal / overlay / validation 同时存在、identity changed / unchanged、focus 或 modal stack 扫描方向。
2. tools / verification PR：cache hit / miss、增量输入变化、report-only 与 blocking gate 的隔离、canonical artifact materialization。
3. assets / manifest PR：missing manifest key、missing locale key、unsupported field、fallback chain、canonical/runtime manifest 同步。
4. docs-only PR：上游权威、系列 governance、报告模板和验证入口是否只维护一套长期口径。

如果 reviewer 无法用文档推演出唯一结果，就必须报问题。不能把“实现时自然会处理”当作通过。

### 4.4 Pass 3: 模块边界和权威

按模块检查：

| 模块 | Review 重点 |
| --- | --- |
| `core` | 是否只定义规则语义；是否避免 libGDX、UI、raw asset path、localized string、client-only 状态；Kotlin 改动是否符合 `docs/rule/kotlin.md` |
| `game` | 是否只装配内容、schema、registry、session；是否复用 `core` 规则，不重写战斗、AI、地图、world progress；Kotlin 改动是否符合 `docs/rule/kotlin.md` |
| `client` | 是否只做 Tile、UI、输入、音频、locale、manifest 消费和表现编排；是否避免规则权威副本；Kotlin 改动是否符合 `docs/rule/kotlin.md` |
| `tools` | 是否只做验证、批处理、report；是否复用 runtime authority，不手写第二套 selector / reward / owner metric；不得用 default-success、空数组、伪造 coverage 或 report-only 指标让 gate 变绿 |
| `assets` | 是否有 specs、contact sheet、sheet plan、manifest、coverage 和 consumer 证据 |
| `docs` | 是否维护单一合同，不复制平行长期口径 |
| `governance` | 是否符合 `docs/rule/ai-change-governance.md`，包括 anti-bloat、`maintainabilityLint` 触发条件、`verifyChanged` routing、owner evidence 和审计字段 |

必须特别检查：

1. `client` 是否恢复 ASCII fallback、debug renderer 或 ASCII manifest 字段。
2. `tools` 是否用 default-success、空数组、伪造 coverage 或 report-only 指标让 gate 变绿。
3. `content pack / overlay` 是否越权定义核心枚举或覆盖官方 raw asset path。
4. `report` 是否把 volatile 字段、timestamp、本机绝对路径写进 canonical artifact。

### 4.5 Pass 4: Implementation Dry Run

把自己当成即将实现该 PR 的开发者，逐文件模拟开发：

1. PR 文档是否列出 Added / Modified / Deleted 三栏文件清单；Deleted 必须能映射到 Pass 8 的 removal plan。
2. 每个文件是否有 owner、输入、输出、失败语义和测试锚点。
3. 开发顺序是否可执行，是否存在循环依赖。
4. 是否需要同时修改未列出的 presenter、resolver、registry、manifest、locale、fixture、golden 或 report。
5. 是否存在“双来源输入”，例如 request 和 state 都持有同一 stack。
6. 是否存在“实现者必须猜”的细节，例如 enum 值、排序、默认值、null 处理、test fixture 生成方式。
7. 改动路径是否需要更新 `verifyChanged` impact routing，且没有在脚本或 workflow 里复制第二套变更判定逻辑。
8. Kotlin 文件数 `>= 5`、新增 public presentation model、renderer 共享组件重排、gate wiring 或结构性治理改动时，PR 文档是否声明 `maintainabilityLint`。

如果某条需求不能直接映射到代码路径、测试路径或 artifact，就必须标为至少 P2。

### 4.6 Pass 5: Test and Gate Matrix

每个 blocking requirement 必须能追到至少一个验证入口：

1. focused unit / snapshot / presenter / resolver test。
2. lint / contract lint / manifest lint / locale lint。
3. owner harness / producer / report gate。
4. golden / clientSmoke / packaged app manual record。
5. final `verifyChanged`。

检查 Acceptance Matrix 是否包含：

| Field | 要求 |
| --- | --- |
| `requirementId` | 稳定且可引用 |
| `source` | 指向 PR 文档章节或完成定义 |
| `owner` | 默认全集为 `core` / `game` / `client` / `tools` / `assets` / `docs`；系列 governance 可以收敛为子集 |
| `fastCheck` | blocking 行为必须有 |
| `ownerGate` | owner 行为必须有；无则写 `N/A` 并说明 |
| `artifact` | evidence 必须 repo-relative |
| `whitebox` | `required` / `skipped` / `N/A`，`skipped` 必须写原因和剩余风险 |
| `removalOwner` | 仅在替换旧路径、字段、fixture、golden 或 fallback 时必填；指向唯一负责删除的 PR 或 owner |
| `crossPrDependency` | 仅在依赖上游/下游 requirement 时必填；指向稳定 `requirementId` 或 PR 文档锚点 |

本节是 PR 级 review 的最小公共合同。各 PR 系列可以在自家 governance 中收敛 owner、whitebox、gate ladder、canonical artifact 和 failure rule 的合法值，例如 dark UI/UX 使用 `UI/pr/development-governance.md`。如果系列 governance 与本节字段取值冲突，以更具体的系列 governance 为准；如果系列 governance 试图放宽证据、gate 或单一权威要求，则以本规范为准。

禁止：

1. 用 aggregate report-only metric 证明 blocking 行为。
2. 用 manual record 替代可自动化的核心行为测试。
3. 用 `goldenScreenshot` 替代 resource manifest lint。
4. 用 `clientSmoke` 替代 packaged app 白盒。
5. 用 `verifyChanged` 替代 owner gate。

### 4.7 Pass 6: Gameplay and UX Director Review

涉及玩法、UI、可读性或前台体验的 PR，必须额外从玩家视角审查：

1. 当前 PR 是否提升核心循环，而不是只增加系统复杂度。
2. 玩家是否能理解目标、危险、奖励、失败原因和下一步动作。
3. 战斗、探索、奖励、成长、构筑是否形成闭环。
4. UI 是否支持高频操作、扫描、比较和重复决策。
5. 新视觉或新 surface 是否让信息更清晰，而不是只变好看。
6. 人工白盒步骤是否能验证真实 player-facing 体验，而不是只验证 debug harness。

玩法类问题不能用“后续 phase 再做”默认跳过。只有当前 phase 缺少明确前置条件时，才允许降级为 future work，并必须写当前可做的过渡处理。

如果 PR 没有 player-facing surface，报告中仍必须保留一行 N/A：`无 player-facing surface，理由：<具体路径或证据>`，不能直接省略本 pass。

### 4.8 Pass 7: Cross-PR Consistency

检查当前 PR 与同系列其他 PR 的关系：

1. 上游 PR 的产物是否作为硬依赖写清楚。
2. 下游 PR 是否误读或覆盖当前 PR 的 owner。
3. 同一个文件、surface、manifest key 或 report field 是否被多个 PR 重复定义。
4. 当前 PR 是否把后续 PR 的实现细节提前冻结，导致后续空间被压死。
5. 删除旧路径、迁移旧 artifact、刷新 fixture/golden 的责任是否归属唯一 PR。

任何跨 PR 共享合同都应回写到系列 README 或 governance 文档，不能只藏在单个 review 报告里。

本 pass 的主要产出应进入报告的 `当前阶段必须解决的问题`，明确哪些问题是当前 PR 合并前必须修，哪些只能作为后续 PR 输入。

### 4.9 Pass 8: Removal and Regression Audit

检查旧路径和 fallback：

1. Deleted 清单中的每一项是否有 `removalPlan`：删除 PR 或 owner、回归扫描入口、禁止复活规则和剩余风险。
2. 是否新增 temporary wrapper、compat alias、legacy fallback 或 dual-read。
3. 如果允许临时兼容，是否有 owner、删除 PR、测试保护和风险说明。
4. 是否存在旧测试、旧 fixture、旧 manifest、旧 resource 继续让旧路径看似可用。

K-ToME 默认长期稳定优先。除非 PR 文档明确要求兼容，否则不为旧 run save、旧 replay、旧 schema、旧 command payload 增加隐性兼容层。

本 pass 的主要产出应进入报告的 `Removal/Iteration Plan`。

## 5. 严重级别

Review findings 统一使用 P0-P3。2026-05-09 之后新增的 PR 级 review 报告必须使用 P0-P3；旧报告不强制重写，但增量复审必须在首次引用时标注历史等级的等价映射。

### P0: 不能进入开发或不能合入

满足任一条件即 P0：

1. 违反 `core / game / client / tools` 硬边界。
2. 引入第二 authority 或绕过 canonical owner evidence。
3. 破坏 schema/version/event/snapshot/manifest/report 稳定合同但未声明。
4. 删除或降低 blocking gate、coverage gate、release gate。
5. 文档与上游路线图或当前代码发生根本冲突，按文档实现会把主干带到不可玩或不可验证状态。

### P1: 会造成错误实现或核心体验失败

满足任一条件即 P1：

1. 状态机或 fallback 自相矛盾。
2. source of truth 不唯一，会导致不同实现都“看似符合文档”。
3. blocking requirement 没有测试、owner gate 或 artifact。
4. PR 依赖未冻结，开发者无法确定实现顺序。
5. 玩家关键体验路径会错误、丢反馈、误导或无法验收。

### P2: 会造成返工、测试漂移或长期维护成本

满足任一条件即 P2：

1. enum、字段、文件、测试名或 artifact 未冻结。
2. nullability、排序、默认值、冲突处理没有写清。
3. manual evidence、report field 或 diagnostic 字段缺少记录规则。
4. 影响范围表漏掉实际需要改的 owner 文件。
5. 文档可以开发，但实现者需要做非显然判断。
6. evidence label、manual record 字段、removalOwner 或 crossPrDependency 缺失，导致后续 PR 无法机械追溯证据或删除责任。

### P3: 不阻塞开发但应在本轮修正文档质量

满足任一条件即 P3：

1. 命名、章节、表格或测试描述不一致。
2. 历史术语残留，可能误导但不改变主实现。
3. 建议补充非阻塞交叉引用。
4. 报告可读性、索引性或后续查找体验不足。

### 5.5 历史等级映射

吸收历史 review 时按以下规则换算，不允许在新报告中继续混用多套等级：

| 历史写法 | 新等级 |
| --- | --- |
| `HIGH`、`Must Fix`、会导致 request changes 的阻塞项 | 默认 P1；若违反硬边界、删除 gate 或破坏稳定合同，升为 P0 |
| `MEDIUM`、`Should Fix`、会造成返工或验收漂移的问题 | 默认 P2 |
| `LOW`、`Nit`、纯可读性或索引性问题 | 默认 P3 |
| `D1` / `D2` / `Dn` 偏差项 | 按偏差影响重新映射到 P0-P3，并在 finding 标题或证据中保留原偏差编号 |

GitHub PR review 决策建议：

1. `request changes` 必须至少包含一个 P0 或 P1。
2. `comment` 可以包含 P1，但必须明确是合并前修还是后续 PR 输入。
3. `approve` 只能在无 P0/P1，且 P2 均有明确 owner、验收入口或后续 PR 承接时给出。

## 6. Findings 写法

每个 finding 必须包含：

1. 标题：用一句话说明具体缺陷和影响。
2. 证据：至少一个文档或代码锚点，优先 `path:line`。
3. 问题：说明为什么当前文字会让开发或验收出错。
4. 影响：说明会影响哪些实现、测试、gate、artifact 或玩家体验。
5. 修复方向：给出可直接写进 PR 文档或代码的具体改法。
6. 推荐测试：如果是行为问题，给出测试文件或测试名建议。

Finding ID 规则：

1. `P1-1` 这类 ID 只在单份报告内唯一。
2. 跨报告引用必须带 report shortname，例如 `pr01-r2#P1-1`。
3. 增量复审必须保留上一轮 P0/P1 的原 ID 状态：已解决、部分解决、未解决或新引入；不能为了让报告更整齐而重新编号。

禁止的 finding 写法：

1. “建议优化文档描述。”
2. “这里可能有问题。”
3. “需要补测试。”
4. “后续注意即可。”
5. 只有结论，没有证据锚点。

推荐写法：

```text
#### P1-1 projection candidate 与 fallback 合同冲突，会把 active targeting 错误降级为 PLAYER

证据：
- `UI/pr/example.md:120-128` 要求 cursor 为空时保留 TARGETING mode。
- `UI/pr/example.md:92-96` 又要求 candidate 必须 cursor 非空。

影响：
开发者按 candidate 规则实现时，targeting 首帧没有 candidate，最终 fallback 到 PLAYER，后续 identity、golden 和 manual evidence 都会漂移。

修复方向：
把 candidate 拆成 mode candidate 与 anchor candidate；active surface 产生 mode candidate，cursor 非空只决定 anchor validity。

推荐测试：
- `TileViewportFocusProjectionTest.targetingFallbackKeepsModeWhenCursorIsNull`
```

## 7. Review 报告模板

每份 PR 级 review 报告建议使用以下结构：

```markdown
# <PR 名称> Review

目标文档：`path/to/pr.md`

审查范围：
- 上游入口：
- 当前代码锚点：
- 本轮重点：

## Findings

### P0

无 / finding 列表

### P1

#### P1-1 ...

证据：

影响：

修复方向：

推荐测试：

### P2

### P3

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前代码/文档状态 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |

## 玩法与体验审查
<!-- Pass 6 output；无 player-facing surface 时写 N/A 和证据 -->

## 当前阶段必须解决的问题
<!-- Pass 7 output；区分合并前必须修与后续 PR 输入 -->

## Removal/Iteration Plan
<!-- Pass 8 output；列出 Deleted/removalPlan、compat 删除计划和回归扫描 -->

## Additional Suggestions

## Open Questions

## Suggested Verification

## Summary
```

如果是 implementation review，还必须增加：

1. `Doc-Vs-Implementation Self-Audit`
2. `Changed Surface Review`
3. `Executed Validation`
4. `Not Executed / Residual Risk`

## 8. “可开发文档”完成定义

本节扩展第 1 节的 5 条最小要求。PR 文档通过 review 的最低标准：

1. 所有 MUST 都有 owner、fast check、owner gate 或 artifact。
2. 所有 public contract 都有 exact names。
3. 所有 state machine 都有 enter、active、exit、fallback、failure semantics。
4. 所有 nullable 字段都有空值规则。
5. 所有 identity 字段都有 included / excluded 说明。
6. 所有排序都有优先级和 tie-break。
7. 所有 resource / manifest / locale / report 字段都有 repo-relative artifact。
8. 所有 manual white-box 都有 scenario、steps、expected evidence 和 skip rule。
9. 所有 old path / fallback / compat 都有删除或禁止复活规则。
10. 所有未执行验证都明确说明原因和剩余风险。
11. 所有删除文件、废弃 manifest 字段、废弃 locale token、废弃 golden label 和废弃 fixture 都已写入 Deleted 清单和 `removalPlan`。
12. 所有跨 PR 依赖和删除责任都能追到 `crossPrDependency` 或 `removalOwner`。

如果文档仍包含以下内容，默认不能判定为“无需猜测”：

1. `TBD`、`待定`、`后续确认`。
2. `可以` 但没有默认选择。
3. `或` 但没有 primary source 和冲突处理。
4. `相关测试` 但没有测试名。
5. `保持一致` 但没有一致性检查入口。
6. `必要时 fail fast` 但没有触发条件。
7. `manual record` 但没有字段和 evidence label。

## 9. 增量复审规则

当用户修改后要求“再次 review”：

1. 先读取上一轮 report。
2. 逐条核实上一轮 P0/P1/P2 是否被吸收；旧报告如果使用 HIGH/MEDIUM/LOW 或 Dn，先按第 5.5 节映射。
3. 对修改过的章节重新执行 Pass 1 到 Pass 5。
4. 对整份文档执行一次 Pass 0、Pass 7、Pass 8 sanity check。
5. 新发现问题必须说明为什么上一轮未显性暴露：例如上一轮阻塞项掩盖、修改引入、或审查标准升级。
6. 不要只检查上一轮问题是否修复；每次复审都要确认“现在是否已经可以实施”。

复审报告必须区分：

1. 已解决。
2. 部分解决。
3. 未解决。
4. 新引入问题。
5. 仍需冻结的实现细节。

## 10. Review 报告归档

报告路径与命名优先遵循目标 PR 系列的 governance；没有系列约定时使用以下默认规则：

1. 跨 phase 通用 review：`docs/review/`。
2. phase 内 PR review：`docs/review/phaseN/.../review/` 或该 phase 已有 review 目录。
3. 独立 PR 系列 review：优先放在系列目录下的 `review/`；如果该系列历史上使用同目录 `.review.md`，可以沿用但必须在 README 或 governance 中说明。
4. 命名格式：`<yyyy-mm-dd>-<series>-<pr-id>-review[-r<n>][-suffix].md`，suffix 使用 `post-fix`、`cross-reference`、`final` 等可搜索词。
5. 增量复审必须在报告头部写上一轮 report 路径，避免只靠聊天上下文承接。

## 11. 验证与自检

文档 review 或 review 规范变更至少执行：

```bash
git diff --check -- <changed-docs>
for f in <changed-docs>; do awk 'BEGIN{c=0} /^```/{c++} END{print FILENAME ":FENCE_OPEN=" c%2}' "$f"; done
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n --type md "$ABS_PATH_PATTERN" <changed-docs>
```

如果变更目标 PR 文档含 Acceptance Matrix 或 governance 合同，还应执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

如果 review 触及 Kotlin 实现、gate wiring、renderer 重排、schema 或 report 逻辑，必须按对应 PR 文档补跑 focused test、owner gate、`maintainabilityLint` 和最终 `verifyChanged`。未运行的命令不能写成已通过。

## 12. 常见红线

以下情况在 K-ToME PR 级 review 中必须主动寻找：

1. 一个字段有两个 authority。
2. request 和 state 重复携带同一信息。
3. report-only 指标证明 blocking 行为。
4. manual record 代替自动化 owner gate。
5. `client` 保存规则权威副本。
6. `tools` 手写 runtime selector 或 reward legality。
7. 资源只有 raw PNG，没有 registry、sheet plan、manifest、coverage。
8. 新 UI 只有视觉描述，没有 input、focus、modal、keyboard、golden、white-box。
9. 旧 fallback 被“临时保留”但没有删除计划。
10. canonical artifact 写入 timestamp、本机绝对路径或 cache status。
11. 测试只验证 happy path，不覆盖 null、exit、identity unchanged、conflict、missing resource。
12. 文档把“看起来合理的抽象”写成长期合同，但当前 PR 没有实际消费者或验证收益。
13. canonical visual manifest 与 runtime visual manifest 只改一侧，未通过 `syncPhase2Manifests` 或等价 owner gate 证明一致。
14. 新增或修改 UI 文案、locale token、presentation token 但未声明 `localeLint` / `contractLint`。
15. visual manifest `prefixRules` 与显式 entry 覆盖同一 key，却没有声明优先级和冲突处理。
16. 测试、fixture、report 或 golden 依赖系统时间、本机时区、环境变量、绝对路径或 transient source。
17. golden label 跨 PR 复用，或旧 golden baseline 没有随当前 PR 一起刷新/废弃。

## 13. 最小 Review Checklist

每次 PR 级 review 结束前，reviewer 必须自问：

1. 我是否确认了基准 ref、HEAD、工作树状态和 review 范围。
2. 我是否读了上游入口、目标 PR、系列 governance、相关代码或测试真源。
3. 每个 P0/P1/P2 是否都有证据锚点。
4. 每个 finding 是否能被开发者直接转化为文档或代码修改。
5. 我是否检查了 state machine、nullability、identity、ordering、source of truth。
6. 我是否检查了 test / gate / artifact / manual evidence。
7. 我是否检查了跨 PR 依赖和旧路径删除。
8. 我是否区分了当前阶段必须修和后续可做。
9. 我是否真实运行了报告自检，并准确记录结果。

只有以上问题都能回答“是”，本轮 review 才算完成。

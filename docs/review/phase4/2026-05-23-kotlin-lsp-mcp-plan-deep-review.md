# K-ToME Kotlin LSP MCP 实施计划深度 Review

Review target: 外部计划文档 `~/Documents/codexPlans/ktome-kotlin-lsp-mcp-implementation-plan.md` (当前提交版本,已含 R1/R2/R3 修订)

Review date: 2026-05-23

Reviewer focus: 战略价值、架构契约、运行风险、与 K-ToME 既有验证纪律的兼容性。不重复 R1/R2/R3 已识别并已采纳的发现。

External references reconfirmed:

- Official Kotlin LSP: <https://github.com/Kotlin/kotlin-lsp>
- Kotlin LSP releases: <https://github.com/Kotlin/kotlin-lsp/blob/main/RELEASES.md>
- Homebrew Kotlin LSP cask: <https://formulae.brew.sh/cask/kotlin-lsp>

Repo facts reconfirmed at review time:

```text
# main Kotlin files (git ls-files): 352   (plan snapshot says ~349)
# test/testFixtures Kotlin files:     308   (plan snapshot says ~302)
FoundationGameSession.kt = 16305 lines     (plan correctly marks as "never read whole-file")
resolveAttack at line 11491 (private fun)  (matches plan benchmark scenario 2)
CombatPipeline.kt:137 `fun resolve(`        (matches scenario 1)
GameApp.kt:1002 `private fun resolveContinueAvailability` (matches scenario 4)
build-logic/src/main/java:                  35 tracked .java files
build-logic/src/test/java:                  7 tracked .java files
rg --files build-logic (default ignore):    misses Java sources, confirms plan's note
git ls-files build-logic/src:               surfaces tracked Java sources
.codex/config.toml:                         only sets reasoning effort
AGENTS.md:                                  277 lines, no LSP section yet
docs/verification/:                         4 .md files, no kotlin-lsp doc yet
```

文件计数与 plan 的 snapshot 偏差在可接受范围;benchmark 锚点的真实文件位置全部对得上,Phase B 启动 sniff、`KotlinLspStatus` discriminated union、untracked 文件 hygiene 这三项 R3 P2 也已合并到计划主体。从这个意义上,计划已经"可实施";本轮 review 不再做合规性勾选,而是给出**实施前真正影响成败的剩余结构性问题**。

---

## 摘要与结论

- **总体判断:计划工程上成熟,但战略 ROI 偏弱,建议先以 Phase A 作为可被丢弃的实验落地,Phase B 默认关闭直到 benchmark 给出量化证据。**
- 计划的优点是工程纪律稳:七工具表面、Phase A/B 切分、JDK 21/25 隔离、build-logic 一等公民、advisor hook 受限化、payload byte budget 与 truncation 元数据,这些都不是新手做得到的设计。
- 但有四个结构性问题不在 R1-R3 的范围内,必须在落地前定下来:
  1. **价值与成本不匹配**: K-ToME 主仓 660 个 Kotlin 文件 + lky 个人项目体量,引入 ~2k 行 TypeScript MCP + pre-alpha LSP runtime + 项目级 hook,长期维护负担是否值得短期 navigation 收益,需要正面回答。
  2. **服务名与实际能力错位**: `ktome-kotlin-lsp` 在大多数时刻只是 `source-index + rg`。计划自己也承认 v1 的可靠价值在 Phase A。命名误导是隐性 cognitive tax。
  3. **payload token 经济从未被定义**: 120/220 KB JSON ≈ 30-55K tokens,远超大多数 Claude/Codex agent 单次工具回合期望的 5-10K。Plan 把"压缩"指标定为 bytes/files,但消费者是 LLM,真正的指标是 token 而非 byte。
  4. **`kotlin_impact` 之前强制 `kotlin_status` 验证 repoRoot**: 这是 plan 明文要求(line 68),把"一次 navigation"变成"两次 round-trip + 一次 hover 决策"。在 Codex 行为契约里这等同于每次都付双倍成本。
- **建议落地路径(详见 §7)**:
  - 改名 → `ktome-kotlin-impact`,把 LSP 定位为"可选增益",而不是品牌词。
  - 仅落 Phase A,Phase B 不进 v1 主体,放到分支或独立后续 plan。
  - 把 `kotlin_status` 与 `kotlin_impact` 合并成一次调用:`kotlin_impact` 在 response 里嵌 `status` 摘要,worktree 不匹配时直接 fail-fast。
  - 引入 token-equivalent budget,balanced ≤ 8K tokens,precision ≤ 12K,recall ≤ 18K(配合现有 byte 预算)。
  - advisor hook 加 `KTOME_KOTLIN_LSP_ADVISOR_DISABLED` 环境逃生口,默认行为更保守。
- 如果以上五项不接受,本评审仍认为计划"可实施",但请记录:这是一份**带已知 ROI 风险**的 Phase 4 工具链投资。

---

## 1. 战略价值与成本判断 (P1)

### 1.1 用户场景核对

计划目标(plan line 5): "让 Codex 在 Kotlin 语义导航中获益"。结合 K-ToME 当前状态:

- 主仓单作者,核心模块 4 个,总 Kotlin 660 文件。
- 仓库已经有 `verifyChanged`、`maintainabilityLint`、白盒框架等强验证纪律。这些是真正的"phase gate",MCP 反复强调不会代替它们。
- Codex(及任何接入 MCP 的 LLM agent)在 K-ToME 上的主要任务模式是: phase 设计 → PR 切分 → 局部 Kotlin 改动 + 测试 → 跑 owner gate。

在这个工作流里,导航主要承担两件事:
- **A. 改动定位**:找到一个 symbol 的定义和 callers。`rg + Read` 已经能完成,但容易在大文件(FoundationGameSession 16k 行)上付额外 token。
- **B. 影响面排查**:一个 schema/契约改动后,哪些 caller 需要同步更新。这是 LSP references 的强项,但前提是 LSP **ready** 且 **正确**。

A 类任务的 token 节省来自 readPlan windowing,确实是 plan 的核心增益;B 类任务的语义价值依赖 Phase B,但 Phase B 是 pre-alpha + 容易降级。

### 1.2 成本侧客观估算

- **一次性实施成本**: 七工具 + router + source-index + LSP session + 安装/检查 + advisor hook + 测试 + benchmark + 文档,按 plan 任务表保守估算 4-6 工作日。
- **持续维护成本**:
  - Kotlin LSP 升级 / launcher 重命名 / capability 变动 (pre-alpha 状态下,过去 6 个月发生过 launcher 由 `kotlin-lsp.sh` 切到 `bin/intellij-server`,JDK 17→25,这种 churn 不会立即停止)。
  - source-index 启发式规则要随项目演进同步(profile mapping、ignored paths、新模块)。
  - MCP SDK / zod 升级 (`@modelcontextprotocol/sdk ^1.29.0`, `zod ^4.1.13` 都是活跃发布通道)。
  - advisor hook 在不同 Codex 版本下的兼容性。
- **隐性成本**:
  - AGENTS.md 噪声(+ ~12 行,虽小但 lky 自己在 CLAUDE.md 里强调"删掉不影响决策的铺垫")。
  - 每次新 worktree 不必 `npm ci` 是好事,但**首次 install-runtime.sh** 仍然要在 user level 留下一个独立 dist。Bug 修复要先 install-runtime.sh 再生效,造成"我改了仓库,但 MCP 还是旧的"的认知摩擦。
  - 每个 `kotlin_impact` response ≥ 30K tokens 时,占用 Codex 一轮上下文不可忽略;长会话尤其敏感。

### 1.3 与"Slow is Fast"原则的对比

lky CLAUDE.md 第 4 节明确:"只实现用户要求或目标必需的能力,不添加 speculative feature;不为单次逻辑提前抽象"。

把这条原则套到本计划上:

- 当前 lky 大部分 Codex 任务是直接编辑/审阅。能用 `rg` 在 660 文件里找到一个 symbol 的时间 ≤ 200ms,token 成本 < 1K。
- MCP `kotlin_impact` 一次回合 token ≥ 30K,意味着导航开销提高 30 倍。换来的是: 一份"打包好的 readPlan + evidenceGaps"。
- 这份打包对 agent 自由度是**减少**的: agent 可能只想看 `FoundationGameSession.resolveAttack` 的 method body,但 MCP 返回的还包括了 6 个 caller 文件 + evidenceGaps + suppressed 元数据。
- 即使 readPlan 命中率很高,**信息密度增益 ÷ token 成本** 也未必 > 1。

**结论**: 把 Phase A 当作"压缩 rg 输出 + 减少大文件全读"的工具是合理的。**绝不要把 Phase B 当作日常工作流**,除非 benchmark 给出明确 token 节省证据。

### 1.4 命名错位 (P1)

服务名 `ktome-kotlin-lsp` 暗示 LSP 是主路径。但 plan line 7 自承:

> v1's reliable value is `source-index + rg + readPlan`; official Kotlin LSP is a bounded enrichment layer, not the primary authority.

这种错位会造成两类问题:
- **agent 期望偏差**: Codex 看到工具名 `kotlin_status`/`kotlin_impact`,会默认调用时已有 LSP 语义。实际多数情况返回 `indexState="unavailable"`,导致 agent 误判"出错了"而退回 rg。MCP 反而成为加速失败认知的负面缓冲。
- **未来扩展失锚**: 如果 Phase B 在 benchmark 里被证明"加不上价值",方案要么砍掉 LSP(此时服务名变成谎言),要么继续维护一个 dead path。

**建议**: 服务名改为 `ktome-kotlin-impact` 或 `ktome-kotlin-search`,工具命名维持(因为 `kotlin_impact` 名义已暗示了"导航"语义);plan README 首段直白说明"基于 source-index + rg,可选用 LSP 增益"。

### 1.5 ROI 兜底建议

如果不愿意完全砍掉这个项目,以下三个保守路径任选其一:

| 路径 | 描述 | 风险 |
| --- | --- | --- |
| 路径 1 (推荐): Phase A only v0.1 | 落 Phase A 七工具但 LSP session 仅留 stub。3 工作日内完成。Phase B 推后到 LSP GA 后再评估。 | 砍掉 Phase B benchmark 的"对照组",但 benchmark 仍可对比 `baseline-rg` vs `source-index-only`,够说明 Phase A 价值。 |
| 路径 2: 双轨试运行 | Phase A 直接进主分支,Phase B 留在一个 feature branch,只做开发者本地实验,不进 AGENTS.md。 | 增加分支维护负担,但保留可选实验空间。 |
| 路径 3: 全砍 | 用现成 `rg`+ `find` + agent 自决。把 plan 文档归档到 `docs/opt/codex/`。 | 损失打包式 readPlan 收益,但仓库纪律最干净。 |

lky 个人偏好"质量优于速度",路径 1 与"Slow is Fast"匹配度最高。

---

## 2. 架构层深度问题 (P1/P2)

### 2.1 source-index 无 AST 对超大文件的鲁棒性 (P2)

plan §3 `source-index.ts`:

- 用 brace tracking + 行注释/块注释剥离来近似 declaration 范围。
- 显式声明 v1 不引入 parser 依赖。
- 对 `FoundationGameSession.kt` (16,305 行) 做 `methodAt(file, line)` 解析。

**风险点**:

- Kotlin 语法里以下结构会让简单 brace counter 失误:
  - 字符串模板里的 `{...}` (e.g. `"prefix-${something.let { it.bar }}"`)。剥注释处理不掉字符串内容。
  - `when` 表达式分支 + 多层 lambda 嵌套。
  - 多层 `companion object` + 内部 `object`。
  - `inline fun` 后的 reified lambda + 链式调用。
- 一旦 `methodAt` 越界,readPlan 给出的 window 就是错的;P0 priority window 落在错误的方法体上,反而误导 agent。

**计划当前态度**:

- §8 "Known Limits": "may misread unusual nested declarations. LSP enrichment and `rg` compensate when available." → 把锅扔给 LSP,但 LSP 是 Phase B,plan A 没有兜底。
- §6 Task 2 测试目标包括 FoundationGameSession 但只测"method-scoped huge-file windowing",没有 fuzz / 反例。

**建议**:

- Task 2 增加针对 FoundationGameSession 的 **快照测试 (snapshot test)**: 把当前 `private fun resolveAttack` 在 line 11491 的预期 method window (startLine/endLine) 固定,任何 source-index 算法变更必须保持快照。
- 额外加 3-5 个手工挑选的"难"位置: companion object 内方法、`when` 分支内的 lambda、`object`-in-`class`。
- 如果 brace tracker 给不出可信 window,要在 `metrics` 里返回 `methodWindowConfidence: "approximate"`,让 agent 知道这是猜的。
- v2 可考虑用 `kotlin-tree-sitter`(WASM) 或调用 `kotlinc -Xprint-ir` 的子集,但 v1 至少要把"近似"做成可观测信号,不是隐式信任。

### 2.2 profile 推断硬编码具体文件路径 (P2)

plan §3 repo-layout.ts 给出的 classification rules:

```text
game/.../FoundationGameSession.kt -> gameSession
game/.../GameModule.kt -> gameSession primary, gameContent secondary
game/.../SessionSnapshotMapper.kt -> schemaModel primary, gameSession secondary
```

这违反"规则不要绑死具体文件名"。一旦 `FoundationGameSession.kt` 被改名(plan 自己在 §3 提到该文件 ≥ 16k 行,客观上有拆分需求),profile 推断会静默失效。

**建议**:

- profile 推断**只依赖目录路径 + 文件名子串模式**,不依赖文件全名。例如:
  - `*/FoundationGameSession*.kt` → `gameSession`
  - `*SessionSnapshotMapper*.kt`, `*SnapshotMapper*.kt` → `schemaModel`
  - 其他都走目录段推断。
- 或者更彻底: 把硬编码列表移到 `repo-layout.config.json`,允许仓库管理员独立维护,不需要重 build MCP runtime。
- 加一个 router 自检: 启动时扫描 indexed roots,如果硬编码列表里有任意条目找不到匹配文件,在 status 中 `note` 字段提示"profile rule drift"。

### 2.3 `kotlin_impact` 与 `kotlin_status` 强耦合 (P1)

plan line 68:

> When readiness succeeds, agents still must verify `kotlin_status.repoRoot` equals the current worktree before using `kotlin_impact`.

这把每次 navigation 变成"先 status 再 impact"。问题:

- Codex agent 不会自动记忆"上次 status 已通过",每次新会话或 token 压缩后都要重新 status。
- `kotlin_status` 本身 ≈ 1-2K tokens,double round-trip 也是延迟翻倍。
- `kotlin_impact` 内部完全可以做这个校验 -- repoRoot 已经在 router 构造时确定,只需要在 response 里返回。

**建议**:

- `kotlin_impact.output` 增加顶层字段 `runtime: { repoRoot, indexState, kotlinLspResolved }`,把 status 摘要嵌进每次 impact 响应。
- 把"必须先调 status"软化为"agent 可信任 impact 输出里的 runtime 摘要"。
- AGENTS.md 段落同步改写: 只在 worktree 切换时调一次 status 即可,不必每次 impact 前都 status。

### 2.4 `kotlin_diagnostics` 工具的实际价值 (P2)

plan §2 `kotlin_diagnostics`:

- 依赖 LSP pull/push diagnostics。
- 计划承认 indexing 期间"may be false-positive or incomplete"。
- evidenceGap 推荐 fallback 到 Gradle compile/test。

实际工作流里:
- LSP unavailable → `capabilityUnavailable=true`,工具变成不返回任何有意义信号。
- LSP ready 但首次 indexing → 可能假阳,agent 应当忽略并跑 Gradle。
- LSP ready 且 stable → diagnostics 与 Gradle compile 重复(都需要类型检查正确)。
- 如果 K-ToME 用 ksp / kapt / Kotlin-IR plugin,Kotlin LSP 不一定能正确处理 generated source。

**建议**:

- 把 `kotlin_diagnostics` 标记为 "Phase B optional, not in v1 surface"。从七工具中移到八工具? 不,改成: v1 不暴露此工具,等 LSP GA 后再加。
- 如果一定要保留,默认 return `capabilityUnavailable=true`,并在 README 明确"Always use `./gradlew compileKotlin` for truth"。
- 节省下来的位置可以让出给一个更实用工具 `kotlin_workspace_search`(纯 source-index + rg 的语义化检索包装)。

### 2.5 scoring 权重不可校准 (P2)

plan §3 agent-router scoring:

```text
Definition / anchor: +100
LSP implementation: +80
LSP reference in same module: +50
Internal rg direct symbol match: +40
taskKeywords match: +20 each (max +60)
Main sourceSet: +15
Test sourceSet with testReadMode=defer: -20
Docs/reports: -25 unless toolingHarness
Cross-module under crossModulePolicy=focused: -15
Java build-logic under toolingHarness: +35
Resource/schema under schema/content profile: +25
```

11 个 magic numbers,没有任何自动 tuning 反馈。benchmark §3 给出 `topFileHitRate`、`expectedP0FilesFound`,但 plan 没说: 如果命中率不达标,下一步是改 score 还是改 router?

**建议**:

- benchmark 输出每个 scenario 的 `scoreBreakdown[]`,把"为什么 X 文件得分 70"展开到具体加分项。
- 把权重放到 `scoring-config.json`,benchmark 失败时可以独立调整不重 build。
- 短期内可以把权重当成 "policy v1",版本化:任何改动写 `routingVersion: 2` 并跑 benchmark 回归。

### 2.6 file-watcher 主路径选择偏激进 (P2)

plan §3 file-watcher.ts:

- 优先 `fs.watch` recursive。
- 失败/drop 时降级到 `mtime-on-entry`。

`fs.watch` 在 macOS recursive 模式下的已知问题:
- 大量 inode 变化时可能 silently drop event,没有错误。
- 对 file rename / atomic write (Kotlin/Gradle 经常 atomic write) 表现不一致。
- 第三方工具 (Spotlight、Time Machine) 触发的事件会泄到 watcher。

也就是说: watcher 标 `active=true` 不一定真活着,但 plan §3 status 里 `fileWatcher.active=true` 会让 agent 信任新鲜度。

**建议**:

- **倒过来**: 把 `mtime-on-entry` 作为**主路径**,`fs.watch` 仅作为"加速 hint",触发主动 prefetch,不作为新鲜度信号源。
- `FileWatcherStatus.fallback` 字段已经存在,但默认值应该是 `"mtime-on-entry"`,watcher 只是 `eagerInvalidation`。
- 这样在 watcher 静默 drop 时,impact 不会基于 stale cache。

### 2.7 LSP request timeout 默认值过激 (P2)

plan §3 `kotlin-lsp-session.ts`:

```text
KTOME_KOTLIN_LSP_REQUEST_TIMEOUT_MS, default 120000  // 2 分钟
Impact semantic enrichment timeout: default 8000     // 8 秒
```

`120 秒 default request timeout`:

- 若 LSP 内部死锁(pre-alpha 软件常见情况),MCP server 会卡 2 分钟。
- 期间 Codex 的 stdio pipeline 不会立即知道,因为 MCP server 仍 alive。
- agent 误以为是网络/CPU 慢,继续等。

`8 秒 semantic timeout`:

- 对 first cold index 来说远不够。Kotlin LSP 对 K-ToME 这种 Gradle Kotlin DSL + 4 模块组合,首次 index 通常 60-180 秒。
- 即使 warm,reference 查询单次 5-10 秒不罕见。
- 8 秒会让多数 `precision`/`recall` 模式都进入 timeout fallback。

**建议**:

- 默认 `requestTimeoutMs` 调到 30000(30 秒)。120 秒是 hung-server 兜底,可作为 `KTOME_KOTLIN_LSP_HUNG_TIMEOUT_MS` 单独 env。
- `semanticTimeoutMs` 在 `mode="precision"` 默认改 15000-20000;`mode="recall"` 默认 25000。继续保留 `semanticPolicy="auto"` 在 8000 以内。
- 在 metrics 里区分 `lspRequestTimeoutBudgetMs` vs `lspRequestActualMs`,避免 R3 提到的字段名歧义没解决。

---

## 3. 公开契约与实现细节缺陷 (P2/P3)

### 3.1 payload 用 byte 还是 token 计量? (P1)

plan §3 agent-router payload budget:

```text
balanced:    120 KB
minimal:      60 KB
precision:   160 KB
recall:      220 KB
```

bytes 对人类是直观的,但 MCP 的消费者是 LLM。计算:

- JSON ASCII-dense, 平均 1 token ≈ 4 chars。
- 120 KB ≈ 30K tokens, 220 KB ≈ 55K tokens。
- Claude Opus 4.7 默认 context 200K, MCP response 一次性吃掉 1/4 是常态。
- Codex (GPT) 不同但量级相近。

这意味着 `mode="recall"` 单次回合就吃 1/4 context,后续会话压力陡增。

**建议**:

- benchmark §3 增加 `outputTokens`(用 tiktoken-cl100k 或简单 char/4 估算)与 byte 并列。
- 把预算改为**优先 token**:`balanced ≤ 8K tokens`、`minimal ≤ 4K`、`precision ≤ 12K`、`recall ≤ 18K`。
- byte 阈值变成"二级 cap"(防止 base64 等异常情况)。
- truncation 顺序(plan §3): "low-value rgSummary previews → low-score tail → duplicate positions → readPlan tail" -- 这个顺序合理,但要在 truncation reason 里写明被砍的是哪个维度。

### 3.2 `kotlin_impact` 输入 API 表面过宽 (P3)

输入字段(plan §2):
- `file, line, column`
- `anchors[]` (max 8)
- `mode` ∈ {minimal, balanced, precision, recall}
- `profile` ∈ {auto, coreRule, gameSession, gameContent, clientPresentation, toolingHarness, schemaModel, testHarness}
- `semanticPolicy` ∈ {auto, fast, required}
- `semanticTimeoutMs`
- `readPlanMaxItems`
- `testReadMode` ∈ {defer, include, priority}
- `focusModules`, `excludeModules`
- `focusBuildRoots`, `excludeBuildRoots`
- `taskKeywords[]`
- `crossModulePolicy` ∈ {auto, focused, all}

13 个旋钮。Codex agent 在多数任务里只用 `file/line/column` + `mode`。其他旋钮:

- agent 没有训练数据告诉它何时该用 `crossModulePolicy=focused`。
- `testReadMode` 的语义("defer"是否包括 testFixtures?)模糊。
- `focusBuildRoots`/`excludeBuildRoots` 只有一个值 `"build-logic"`,数组类型多余。

**建议**:

- v1 暴露的字段限于: `file/line/column`、`anchors`、`mode`、`taskKeywords`、`semanticTimeoutMs`。其他全部 hidden 字段(默认从 profile/mode 推导)。
- 如果 agent 真的要 fine-tune,文档化但不在 schema enum 里宣传。
- 收缩 API 表面有助于 LLM tool-use 不产生 hallucinated 字段。

### 3.3 `target.kind` 与 `target.profile` 没有 enum (P3)

plan §2:

```ts
target: {
  symbolName: string;
  kind: string;            // <-- 开放 string
  profile: string;         // <-- 开放 string
  ...
}
```

但 `profile` 在输入里是 enum,输出却是 string。下游测试或 sink 无法 type-narrow。同理 `kind` 应当是 SourceDeclaration.kind 的子集 enum。

**建议**:

- `target.kind: KotlinSymbolKind` (复用 source-index 里的 union)。
- `target.profile: KotlinImpactProfile`。
- schema test 强制约束。

### 3.4 `kotlin_references.positionsPerFile=20, limit=80` 过宽 (P3)

plan §2 `kotlin_references`:

```text
positionsPerFile?: number;  // default 3, max 20
limit?: number;             // default 80, max 300
```

最坏情形: 20 positions × 300 files = 6000 entries。每个 entry 含 line/column/preview 之类,即使无 preview 也是 50K+ tokens。这远超"summary only"承诺。

**建议**:

- `positionsPerFile` max 改 5。
- `limit` max 改 120。
- 仍超大时优先按 module/sourceSet 聚合,而不是逐 position 列举。

### 3.5 `kotlin_references.sourceSet="resource"` 仍是误导接口 (R3 P3 未完全闭合)

R3 提到过此问题,plan §2 line 339-340 现在加了一段:

> `sourceSet="resource"` is allowed only for Phase A text-search fallback evidence. Resource matches must be marked `semantic=false` and must never count as satisfying `semanticPolicy="required"`.

这段说明在文档里有,但接口形态没改: `kotlin_references` 仍然是"高精度引用"语义的工具。

**建议**:

- 仍然移除 `resource` 从 `kotlin_references.sourceSet`。
- 新增独立工具 `kotlin_resource_search`(或合并到 `kotlin_symbol(mode="query")`)做文本检索。这样语义边界清晰: references = 精确,resource_search = 文本。
- 或者,如果不愿增表面,直接让 `kotlin_impact` 在 routePlan 里返回 resource hits,不让外部直接通过 references 拿。

### 3.6 `evidenceGaps.suggestedCommand` 命令拼接一致性 (P3)

plan §3 agent-router 给出的命令模板:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:clientSmoke :client:goldenScreenshot
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew maintainabilityLint
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew verifyChanged
```

三处重复 prefix。若仓库未来改 SDKMAN 入口(例如改用 `mise`/`asdf`),需要 patch 多处。

**建议**:

- 在 router 中提供 `buildSdkmanGradleCommand(tasks: string[])` helper,所有 EvidenceGap 命令统一通过它生成。
- README + agent-guide 说明: 任何不通过 helper 生成的命令都算 plan drift。

### 3.7 安装脚本 rsync 默认目录的清空风险 (P3)

plan §4 `install-runtime.sh`:

```bash
RUNTIME_DIR="${KTOME_KOTLIN_LSP_MCP_RUNTIME_DIR:-$HOME/Library/Application Support/ktome/kotlin-lsp-mcp}"
...
rsync -a --delete --exclude node_modules --exclude dist ... "$SCRIPT_DIR/" "$RUNTIME_DIR/"
```

`rsync -a --delete` 在用户传错的 `KTOME_KOTLIN_LSP_MCP_RUNTIME_DIR` 时,会清空目标目录(部分排除项之外)。例如设置成 `$HOME/Documents` 会丢失大量个人文档。

**建议**:

- install-runtime.sh 启动时 sanity check `RUNTIME_DIR`:
  - 必须包含 `kotlin-lsp-mcp` 字符串。
  - 必须不是 `$HOME` 直接子目录(除特定白名单)。
  - 如果 `RUNTIME_DIR` 已存在但**不包含**之前 install 的 marker file `.ktome-kotlin-lsp-mcp.marker`,refuse 而不是 `--delete`。
- 安装成功后在 `RUNTIME_DIR/.ktome-kotlin-lsp-mcp.marker` 写一行 metadata(版本、提交 sha、install 时间),作为以后 rsync 的合法性凭证。

### 3.8 `kotlin_status` 的 `note: string` 字段语义模糊 (P3)

plan §2:

```ts
type KotlinStatusOutput = {
  ...
  note: string;
}
```

`note` 是必填,但没说语义: 是给人看?给 agent 看? agent 如何解析 note 决定行为?

**建议**:

- 改成 `notes: string[]`,每条 note 配 `code: string`(machine-readable)。
- 或者直接拆: `humanNote: string` + `agentHints: string[]`。
- 文档里给出 5-10 个常见 code,例如 `"phaseA:lsp-disabled"`, `"phaseB:indexing"`, `"phaseB:degraded:jdk-mismatch"`,这样 agent 行为可被测试覆盖。

---

## 4. 运行与维护风险 (P2)

### 4.1 LSP 子进程生命周期治理 (P2)

plan §2 `kotlin_shutdown` 是显式 stop,但 plan 也承认 agent "may forget to call"。Kotlin LSP import Gradle 后内存 ≥ 1-2 GB,长时间 idle 会持续占用。

风险场景:
- agent 起 `kotlin_status(start=true)`,做了几次 navigation,然后切去做别的事(写代码、跑 Gradle test),从不调 `kotlin_shutdown`。
- 用户切 worktree, Codex 会话不重启, MCP runtime 还在原 repoRoot 上跑 LSP。
- LSP 持续在 background indexing 干扰 Gradle build (kotlinc 共享 daemon)。

**建议**:

- session 内加 `idleShutdownMs` (default 300000 = 5 分钟)。每次 LSP 请求重置 timer,超时自动 `stop()`。
- `kotlin_status.kotlinLsp.idleShutdownAt: string`(ISO time)在 response 里暴露,让 agent 知情。
- 手动 `kotlin_shutdown` 仍可用,但不再是 hard requirement。

### 4.2 advisor hook 改变 Codex 行为的隐式影响 (P2)

plan §4 `.codex/hooks.json` + `scripts/codex-hooks/ktome-kotlin-lsp-advisor.mjs`:

- `UserPromptSubmit`: 在 Kotlin 任务前注入 LSP-related context。
- `PreToolUse`: 对"明显的宽泛 Kotlin search"在 readiness 通过时 block 一次。

合理的设计,但风险点:

- **跨开发者一致性**: K-ToME 现在只有 lky 一人。如果将来引入合作者,他们 clone repo 后 hook 自动生效,可能 block 他们习惯的 `rg` 行为。需要清楚的逃生口。
- **PreToolUse block 的边界**: 如何定义"obvious broad Kotlin search"? plan 说"without class/function/module/file anchors",但 regex 边界本身会有 false positive。某些 `grep -r 'foo' src/main` 这种 anchor 不强但合法的用法可能被误 block。
- **hook 与 Codex 自身 cache 交互**: Codex 是否会按 prompt-hash 做 cache?如果 hook 注入的 context 因为时间敏感(readiness 状态),cache 会失效频繁。

**建议**:

- 加全局开关 `KTOME_KOTLIN_LSP_ADVISOR_DISABLED=1`,任何 hook 行为都跳过。
- `.codex/hooks.json` 里的 hook 增加版本字段(`"version": 1`),将来兼容性破坏时可以 graceful skip。
- `PreToolUse` block 出来时,response context 必须包含: "我为什么 block 了你 + 你怎么继续"。否则 agent 容易陷入"我不知道为什么不让我用 rg"。
- block 不要"once per turn",改"once per repoRoot/session"。避免同一会话多次 friction。

### 4.3 macOS-only + 无 CI 的可见性问题 (P2)

plan §4: "v1 only supports and tests macOS installation."

意味着:

- 无 CI 校验。Phase A 是纯 TS + node test,可以跑 GitHub Actions ubuntu runner -- plan 没有提到。
- Phase B 的 fake-LSP test 也是 Node-only,可在 ubuntu 跑。
- 真正需要 macOS 的只有 `install-runtime.sh`、`check-codex-mcp.sh` 的 shell 行为 + 可能的 Homebrew cask 验证。

**建议**:

- 增加 `.github/workflows/codex-kotlin-lsp-mcp.yml`:
  - On PR touching `scripts/codex-kotlin-lsp-mcp/**` or `scripts/codex-hooks/**`。
  - ubuntu runner: `npm ci && npm run build && npm run test && npm run smoke`(Phase A only,LSP unavailable mode)。
  - macOS runner: 可选 nightly, `npm run benchmark:agent-impact` (no LSP)。
- 没有 CI 等于把"测试通过"押在 lky 本地。Slow-is-Fast 原则建议**前置自动化**。

### 4.4 Pre-alpha LSP 长期维护 (P2)

plan line 7, line 34-37 都承认 Kotlin LSP 是 pre-alpha。

- 过去 6 个月: launcher 从 `kotlin-lsp.sh` 切到 `bin/intellij-server`、JDK 要求 17→25。这种 breaking 不会立即停止。
- plan 的 `bin/intellij-server` 决策正确,但下一次 breaking 来时,需要谁 patch?
- pre-alpha 软件不承诺 backwards compatibility,issue tracker 上是 best effort response。

**建议**:

- AGENTS.md 段落里写明: "Phase B 依赖 Kotlin LSP pre-alpha,失败/breaking 时直接关闭 Phase B,不阻塞日常工作"。
- 提供独立的 `KTOME_KOTLIN_LSP_DISABLED=1` env,可一键关闭 Phase B 启动尝试,即使 binary 存在。
- benchmark 报告输出 `kotlin-lsp` 版本(`bin/intellij-server --version`),用于未来 regression triage。

### 4.5 token 经济与会话压缩交互 (P2)

Claude harness 会在 context 接近 limit 时压缩。`kotlin_impact` 输出在压缩时会被汇总掉,丢失 readPlan 精确 windows。

风险:
- 长会话后,先前的 impact response 被压缩成 "Used kotlin_impact, found 6 files",readPlan 已丢。
- agent 再次做 impact 时认为已有结果,可能跳过实际 read。

**建议**:

- impact response 里突出"短期可消费摘要"vs"长期需要持久化的信息"。
- v1 不要把这个问题当成 hard blocker,但 agent-guide 里提一句"impact 输出是短期参考,会话压缩后请重新调用或显式记录关键路径"。

---

## 5. 与 K-ToME 既有纪律的兼容性 (P2/P3)

### 5.1 evidenceGaps 不能弱化 verifyChanged / owner gate (P2)

plan §3 evidence gaps 已经将 `verifyChanged`、`maintainabilityLint`、owner gate、golden screenshot 等列为不可替代项,这是好设计。但要更严:

- 任何 `kotlin_impact` response 都应该至少包含**一条** evidence gap(除非 mode=minimal 且非生产代码)。
- 如果 router 没生成任何 gap,要主动 fallback 到通用 `verifyChanged` 提示。否则 agent 容易把"无 gap"误读为"不需要 Gradle 验证"。

具体落地: `agent-router/index.ts` 内强约束:

```ts
if (evidenceGaps.length === 0 && !isReadOnlyDocsOnly) {
  evidenceGaps.push({
    kind: "gradleVerification",
    message: "Run Gradle verifyChanged after the change to confirm impact.",
    suggestedCommand: buildSdkmanGradleCommand(["verifyChanged"]),
  });
}
```

### 5.2 `docs/verification/` 子路径混入风险 (P3)

plan §5 要新增:
- `docs/verification/kotlin-lsp-mcp-agent-guide.md`
- `docs/verification/kotlin-lsp-mcp-v1-report.md`

`docs/verification/` 当前是**验证真源**所在(test-task-performance-monitoring.md、validation-mode.md 等)。把 MCP **工具文档**塞进来会模糊"verification 真源 vs 工具说明书"的边界。

未来如果继续往 `docs/verification/` 加 MCP 相关文档,该目录会变成"杂物间"。

**建议**:

- 把 MCP 文档放 `docs/tooling/codex-kotlin-lsp-mcp/`(新建子目录),只在 `docs/verification/README.md` 给一行交叉引用。
- 或放 `scripts/codex-kotlin-lsp-mcp/docs/`,跟随代码包就近文档化。
- benchmark 报告 (`*-v1-report.md`) 是 measured evidence,可以放 `docs/verification/reports/`,但 *agent-guide* 不属于 verification 真源,应分开。

### 5.3 AGENTS.md 段落精简到 4-6 行 (P3)

plan §5 推荐的 AGENTS.md 段落 12-15 行,有重复:
- "before using it run check-codex-mcp.sh" + "advisory only" + "install once" + "new worktree should not run npm" + "use kotlin_impact" + "not replace verifyChanged" + "kotlin_shutdown after analysis"。

lky CLAUDE.md 要求"删掉不影响决策的铺垫"。建议:

```markdown
### Kotlin LSP MCP (macOS, optional)

K-ToME 本地可选 MCP 服务 `ktome-kotlin-impact`,把 `kotlin_impact` 作为非 trivial Kotlin 导航首选。它**不替代** `verifyChanged`、owner gate、`maintainabilityLint`、白盒框架。详见 `docs/tooling/codex-kotlin-lsp-mcp/README.md`。
```

详细说明全部链到 `docs/tooling/...`,AGENTS.md 只保留 routing。

### 5.4 跨阶段稳定合同维护 (P2)

AGENTS.md §1 提到"跨阶段冻结合同"。MCP 输出的 JSON schema 本身就是一种合同:
- `kotlin_impact` 输出字段一旦发布,Codex 训练/prompt 会习惯它。
- v2 schema 变更需要 deprecation 路径。

plan 当前没有 schema 版本字段(除 `routingVersion: 1` 在 metrics 里)。

**建议**:

- 在 `kotlin_impact.output` 顶层加 `schemaVersion: 1`。
- v1 不允许移除字段,只能新增 optional 字段。breaking 走 v2 + 一段 dual-output 期。

---

## 6. 推荐落地方案

按推荐度排序的三套方案。所有方案前都先做以下零成本动作:

- 把服务名改成 `ktome-kotlin-impact`(全局 search/replace plan 文档)。
- 把 plan 当前文档归档到 `docs/opt/codex/kotlin-impact-mcp-plan.md`(不在主仓 docs/INDEX.md 索引)。
- 在 `docs/INDEX.md` 增加一行: "Codex MCP 工具: 见 `docs/tooling/codex-kotlin-lsp-mcp/`",作为唯一 entry。

### 方案 A: Phase A only v0.1 (推荐)

只落 Phase A,Phase B 文档保留但代码全部 stub。预计 3-4 工作日。

- 七工具表面保留,LSP-related 字段全部返回 `kotlinLsp.resolved=false, lastUnavailableReason="disabled"`(新增 reason)。
- `install-runtime.sh` 不下载/检查 Kotlin LSP binary。
- `check-codex-mcp.sh --semantic-smoke` 直接 fail-fast 说"Phase B disabled in v0.1"。
- benchmark 跑 `baseline-rg` + `source-index-only` + `lsp-off-mcp` 三栏,够证明 Phase A 价值。
- 决策门: 三个月后或 K-ToME 出现需要 LSP 的具体 navigation case,再开 Phase B 立项。

**优势**: 维护负担最低,token 经济可控,pre-alpha 风险隔离。

### 方案 B: 双轨,Phase B 仅 feature branch

Phase A 进 main,Phase B 留在 `feature/kotlin-lsp-phase-b` 长期分支。

- AGENTS.md 不提 Phase B。
- benchmark 在 feature branch 内独立运行,结果可手动 cherry-pick 进 v1 report。
- 当 LSP GA(或某次 release 显著稳定)时合并 feature branch。

**劣势**: 长期分支维护,merge conflict 风险。

### 方案 C: 当前 plan 原样实施

按 plan 全文落地。

**劣势**: ROI 风险已述。如果坚持,**至少**在落地前完成以下硬要求:

1. payload budget 改为 token-priority(详 §3.1)。
2. `kotlin_impact` 内嵌 `runtime` 摘要,弱化 `kotlin_status` 双 round-trip(详 §2.3)。
3. profile 推断改路径 pattern,移除硬编码文件名(详 §2.2)。
4. file-watcher 改为 mtime-on-entry 主路径(详 §2.6)。
5. LSP request timeout 30s,semantic timeout 按 mode 分级(详 §2.7)。
6. install-runtime.sh 添加 marker 保护(详 §3.7)。
7. advisor hook 添加 env 关闭开关(详 §4.2)。

少做任何一项,review 会判 "实施前必须修"。

---

## 7. 验证建议

### 7.1 Phase A 测试增量

- `agent-router.test.ts` 增加 **snapshot tests**,固化 8 个 benchmark scenario 的 top-3 files 与 readPlan 头 2 项。任何 scoring 改动需更新 snapshot。
- `source-index.test.ts` 增加 `FoundationGameSession.kt` 的 `methodAt(line=11491)` 期望返回 `resolveAttack` 方法 window。
- 新增 `source-index-fuzz.test.ts`(可选,p3): 用 K-ToME 既有所有 .kt 文件做 `scan() -> reconstruct -> check no out-of-bounds methodAt`。

### 7.2 Phase B 测试增量

- fake-LSP "started but crashed after initialize" 用例。
- fake-LSP "indexing forever" 用例(`$/progress` 永不到 100%)。
- LSP capability missing(server 不返回 `referencesProvider`)用例。
- session 应在每种情况下 fallback 到 Phase A,而不是抛错。

### 7.3 benchmark 输出契约

- 输出 `kotlin-lsp-mcp-v1-report.json` + `*.md`。
- 必须包含字段: `outputTokens`、`outputBytes`、`elapsedMs`、`readPlanItems`、`returnedFiles`、`semantic.used`、`semantic.indexState`、每个 scenario 的 `expectedP0FilesFound` 命中。
- 命令行 flag: `--lane=baseline-rg|source-index-only|lsp-off-mcp|lsp-on-mcp`,显式选择对照组。
- 路径全部 repo-relative,通过 plan §6 Task 5 hygiene scan 验证。

### 7.4 CI 建议(若采纳方案 A 或方案 C)

- GitHub Actions: `ubuntu-latest` + `node 22`,跑 build/test/smoke。
- 触发条件: `paths: [scripts/codex-kotlin-lsp-mcp/**, scripts/codex-hooks/**, .codex/hooks.json]`。
- 不需要 macOS runner(Phase A 与 Phase B fake-LSP 都跨平台)。

---

## 8. 当前阶段必须解决的问题

按 review 内部优先级标注。`必须修`= 落地前 blocker;`建议修`= 可与首次 PR 同步处理但不阻断。

| ID | 主题 | 严重级 | 处置 |
| --- | --- | --- | --- |
| S1 | 战略 ROI 与服务名错位 (§1) | P1 | 必须先与 lky 确认方案 A/B/C |
| S2 | `kotlin_impact` 内嵌 runtime 摘要,弱化 status 双 round-trip (§2.3) | P1 | 必须修 |
| S3 | payload 改 token-priority budget (§3.1) | P1 | 必须修 |
| S4 | profile 推断硬编码文件名 (§2.2) | P2 | 必须修 |
| S5 | file-watcher 主路径改 mtime-on-entry (§2.6) | P2 | 必须修 |
| S6 | LSP request timeout 30s + semantic 分级 (§2.7) | P2 | 必须修 |
| S7 | install-runtime.sh rsync 安全 marker (§3.7) | P2 | 必须修 |
| S8 | advisor hook env 关闭开关 (§4.2) | P2 | 必须修 |
| S9 | source-index AST-less 超大文件 snapshot 测试 (§2.1) | P2 | 必须修(测试) |
| S10 | idle shutdown 默认 5 分钟 (§4.1) | P2 | 建议修 |
| S11 | `kotlin_diagnostics` 工具价值复审 (§2.4) | P2 | 建议修(或移出 v1 表面) |
| S12 | scoring 权重外配 + scoreBreakdown (§2.5) | P2 | 建议修 |
| S13 | `target.kind`/`profile` enum 化 (§3.3) | P3 | 建议修 |
| S14 | `kotlin_references.positionsPerFile/limit` 收紧 (§3.4) | P3 | 建议修 |
| S15 | `kotlin_references.sourceSet="resource"` 移除 (§3.5) | P3 | 建议修 |
| S16 | evidenceGap.suggestedCommand helper 化 (§3.6) | P3 | 建议修 |
| S17 | `kotlin_status.note` 改 structured (§3.8) | P3 | 建议修 |
| S18 | evidenceGaps 默认兜底 verifyChanged (§5.1) | P2 | 必须修 |
| S19 | docs/verification 路径不混 MCP 工具 doc (§5.2) | P3 | 建议修 |
| S20 | AGENTS.md 段落精简 4-6 行 (§5.3) | P3 | 建议修 |
| S21 | schemaVersion 顶层字段 (§5.4) | P2 | 必须修 |
| S22 | CI 增加 Phase A Linux job (§4.3) | P2 | 建议修 |
| S23 | Pre-alpha LSP env 一键关闭 (§4.4) | P2 | 建议修 |

如果按方案 A 落地,S11(diagnostics)直接执行"移出 v1 表面"。S22 是方案 A/C 通用。

---

## 9. Requirement Alignment

| Requirement | 状态 | 备注 |
| --- | --- | --- |
| Phase A 必须在 LSP 缺失时仍可用 | 一致 | R3 修复后已是合格的 graceful degradation |
| LSP 必须 read-only,不暴露 rename/format/quickfix | 一致 | plan §1 line 44 + §1 line 18-20 已明确 |
| 不替代 K-ToME 验证纪律 (verifyChanged 等) | 一致(但需 §5.1 加硬约束) | 文档反复声明,但 router 内未强制兜底 evidenceGap |
| build-logic Java 一等公民 | 一致 | R2 修复后 indexedRoots + benchmark scenario 都涵盖 |
| 不引入 docs/superpowers 等并行长期口径 | 部分一致 | plan §5 把 MCP 工具 doc 放 `docs/verification/` 仍混入风险(§5.2) |
| 不在 AGENTS.md 加冗长说明 | 部分一致 | 12-15 行可压到 4-6 行(§5.3) |
| 服务名与实际能力对应 | 不一致 | 服务名暗示 LSP 是主路径,但 v1 主路径是 source-index + rg(§1.4) |
| payload 对 LLM 消费友好 | 部分一致 | byte budget OK,但未定义 token budget(§3.1) |
| schema 演进有版本管理 | 不一致 | 缺顶层 `schemaVersion`(§5.4) |
| 自动化测试覆盖 routing 关键决策 | 部分一致 | 缺 snapshot/fixture-based test(§7.1) |

---

## 10. Suggested Verification(实施前可立即跑的命令)

```bash
# 当前仓库快照与 plan 文档对齐
git ls-files | grep -E '^(core|game|client|tools)/src/main/kotlin/.*\.kt$' | wc -l
git ls-files | grep -E '^(core|game|client|tools)/src/(test|testFixtures)/kotlin/.*\.kt$' | wc -l
git ls-files build-logic/src/main/java build-logic/src/test/java | wc -l

# 关键 benchmark 锚点存活
rg -n 'private fun resolveAttack' game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
rg -n 'fun resolve\(' core/src/main/kotlin/com/ktome/core/combat/CombatPipeline.kt
rg -n 'private fun resolveContinueAvailability' client/src/main/kotlin/com/ktome/client/GameApp.kt
rg -n 'class VerificationTaskRegistry' tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt
rg -n 'class (VerifyChangedPlanGate|VerificationTaskPlugin)' build-logic/src/main/java/com/ktome/build/verification

# 工具链
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
java -version  # 期望 21
sdk home java 25.0.1-open
command -v intellij-server || true
command -v kotlin-lsp || true
brew list --cask kotlin-lsp 2>/dev/null || true
```

如果计划进入实施阶段,首次 PR 前应跑(对应 Task 5):

```bash
cd scripts/codex-kotlin-lsp-mcp
npm ci
npm run build
npm run test
npm run smoke
# Phase A only 时 semantic-smoke 应直接 fail-fast 而非启动 LSP
```

---

## 11. 玩法与体验审查

This is developer-tooling only,不直接影响战斗、奖励、探索、UI、存档、玩家可见平衡。

间接体验风险已在 R3 评估:若 MCP 输出歧义,后续 Phase 4 review 可能据不全证据下结论。本轮新增的间接风险:

- **token 经济**: 长会话被 impact response 撑爆,agent 行为退化,间接拖慢 phase 推进。
- **LSP idle 内存**: 后台 ≥ 1-2 GB 长期占用,可能与 Gradle compile/test 共享 kotlinc daemon,造成 build 偶发性 OOM 或 slowdown。这是间接玩法风险(release gate 跑得更慢)。

---

## Summary

R1/R2/R3 已经把计划从"概念草稿"打磨到"工程上可实施"。本轮 R4 深度评审不再追加合规性勾选,聚焦三个未被前序覆盖的层面:

1. **战略 ROI**: 这个 MCP 在 K-ToME 单作者 660-file 项目里,是否值得长期维护? 答: 仅当限定到 Phase A、且接受"导航 token 翻倍换 readPlan 压缩"的 trade-off 时值得。
2. **服务定位与契约**: 服务名误导、`status` 与 `impact` 双 round-trip、payload byte vs token、scoring magic numbers、profile 硬编码 -- 这些都属于"实施前修比之后修便宜"的契约缺陷。
3. **运行风险**: pre-alpha LSP 维护、idle 内存治理、advisor hook 可关、macOS-only + 无 CI -- 这些都属于 plan 当前模糊处理但生产中会反复触发的风险。

建议落地路径:**方案 A (Phase A only v0.1) + 完成 §8 中 P1/P2 的"必须修"清单**。Phase B 推迟到 Kotlin LSP 出 GA 或 K-ToME 出现明确无法用 source-index + rg 解决的 navigation case。

如果用户选择继续按 plan 原样实施(方案 C),review 不阻止,但需要明确记录: 这是一份带已知 ROI 风险与 token 经济风险的 Phase 4 工具链投资,后续若维护负担超出预期,允许直接归档不留遗憾。

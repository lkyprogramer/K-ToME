  docs/review/rule/pr-level-review-standard.md 深度 Review

  审查范围:
  - 上游入口:AGENTS.md、docs/INDEX.md、docs/rule/kotlin.md、docs/rule/ai-change-governance.md、UI/pr/README.md、UI/pr/development-governance.md
  - 历史 review 实践:docs/review/2026-04-12-pr01-unified-verification-foundation-review.md、UI/pr/dark-uiux-pr00-style-and-pipeline-contract.review.md、docs/review/2026-04-04-unified
  -whitebox-framework-review.md
  - 当前工作树实况:git status(dark-uiux-pr01-client-shell-layout 删改面)
  - 本轮重点:规范是否能让一个不熟悉本仓库的 reviewer 直接产出"开发可用"的 PR 级 review

  Findings

  P0

  无。规范没有违反核心边界、也没有删除既有 gate。

  P1

  P1-1 严重级别 P0–P3 与项目历史 review 实际使用的 HIGH/MEDIUM/LOW 与"D1/D2 偏差项"不兼容,缺映射规则

  证据:
  - docs/review/rule/pr-level-review-standard.md:208-247 唯一定义 P0–P3,且 §9 增量复审要求"逐条核实上一轮 P0/P1/P2 是否被吸收"。
  - UI/pr/dark-uiux-pr00-style-and-pipeline-contract.review.md:8 决策为 comment,findings 用 HIGH / MEDIUM。
  - docs/review/2026-04-12-pr01-unified-verification-foundation-review.md:36-95 用 D1 / D2 / Dn 偏差编号 + "低风险"等措辞。
  - docs/review/2026-04-04-unified-whitebox-framework-review.md 又是另一种打分维度。

  问题:
  当前仓库历史 PR 级 review 至少存在三套严重级别词汇。规范没有规定 (a) 旧报告吸收时如何换算,(b) 当前 reviewer 能否继续用 HIGH/MEDIUM,(c) GitHub PR review 决策(approve / comment /
  request changes)与 P0–P3 的映射。增量复审若依赖"逐条核实 P0/P1/P2",会卡在第一步——上一轮报告根本没有 P 级。

  影响:
  - §9 增量复审无法机械执行,reviewer 必须自行二次翻译,容易漏吸收。
  - 同一 PR 在两轮中跨级别(HIGH→P1 还是 P0?)会产生争议,稀释规范的强制力。
  - acceptanceContractLint / governance gate 等结构化 lint 也无法用 P 级编号机械校对。

  修复方向:
  在 §5 末尾追加一节"5.x 历史等级映射":
  1. 既有 HIGH ⇄ P1、MEDIUM ⇄ P2、LOW ⇄ P3、D{n} 偏差按风险等级映射。
  2. GitHub 决策映射:request changes 必含至少一个 P0/P1;comment 允许 P1 但需注明合并前/后修;approve 仅在无 P0/P1 时给。
  3. 规定 2026-05-09 之后的新报告强制使用 P0–P3,旧报告归档不强制重写,但增量复审报告中以"原 HIGH(等价 P1)"形式标注一次即可。

  推荐测试 / 校验:
  - 写一条 lint(可放进 acceptanceContractLint 或新增 reviewReportLint):扫 docs/review/**/*.md 中的标题级别,匹配 ^####\s+P[0-3]-\d+,2026-05-09 之后的新报告若不命中即 fail。

  ---
  P1-2 §10 自检命令缺 SDKMAN init,违反规范自身"未运行的命令不能写成已通过"

  证据:
  - docs/review/rule/pr-level-review-standard.md:400-415 直接写 ./gradlew acceptanceContractLint 与各 lint。
  - UI/pr/README.md:248-253 强制要求"所有 Gradle 命令前必须执行 source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env"。
  - ~/.claude/CLAUDE.md §6 同样把 SDKMAN init 列入 native 命令场景。
  - docs/review/rule/pr-level-review-standard.md:415 自己在最后强调"未运行的命令不能写成已通过"。

  问题:
  新人 reviewer 按 §10 复制粘贴会直接 fail("Could not find tools.jar / unsupported class file"等),而规范没提示这是必须前置。./gradlew acceptanceContractLint 在没有 sdk env 的 shell
  里是不可重现的。

  影响:
  - 增量复审强制要求跑 verifyChanged,但命令块不带 init 步骤 → 形成"明知会失败但仍按规范执行"的执行漂移。
  - 与 AGENTS.md / README 的真源不一致,违反规范 §2 "输入优先级 1: 仓库规则"。

  修复方向:
  把 §10 的所有 Gradle 段落替换为:
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk env
  ./gradlew acceptanceContractLint
  # ...
  且在 §3 Review 前预检里加一条:"6. 已执行 source $HOME/.sdkman/bin/sdkman-init.sh && sdk env,JDK 与 Gradle 版本与 gradle.properties / sdkmanrc 对齐"。

  推荐测试:
  无需自动化,但可以让规范引用 UI/pr/README.md:248-253 作为环境前置真源,避免维护双口径。

  ---
  P1-3 模块边界与必读规则文档缺失:docs/rule/kotlin.md / docs/rule/ai-change-governance.md 未进入输入清单与 Pass 4

  证据:
  - AGENTS.md:1.3 明确"任何 Kotlin 新增、修改、重构,开始编码前必须先读 docs/rule/kotlin.md;non-trivial Kotlin 改动、结构重排、review/gate/工具链治理,还必须读
  docs/rule/ai-change-governance.md"。
  - 规范 §2 "输入优先级"第 1 条只写 "AGENTS.md、docs/INDEX.md、docs/rule/*",没列出这两份关键文件。
  - 规范 §4.4 Pass 3 模块边界表 core / game / client / tools 各列 review 重点,但没引用上述两份规则。
  - 规范 §7 模板 Doc-Vs-Implementation Self-Audit 也没要求"先核对 kotlin.md / ai-change-governance.md"。

  问题:
  PR 级 review 在 K-ToME 经常包含 Kotlin diff(参见当前分支 client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt 等)。AGENTS.md 把这两份规则列为强制前置,但规范没复述,导致
  reviewer 容易跳过,review 时漏掉命名、可空、并发安全、change governance 这类高频陷阱。

  影响:
  - Pass 4 Implementation Dry Run 缺失"是否符合 kotlin.md 命名/可空/不可变/并发条款"维度。
  - ai-change-governance.md 中的"结构性变更必须跑 maintainabilityLint"等具体门禁没在规范中显式被强制。
  - AGENTS.md 是上游真源,规范 §2 简写 docs/rule/* 会让 reviewer 误以为这是可选目录列表。

  修复方向:
  1. §2 第 1 条改为:"AGENTS.md、docs/INDEX.md、docs/rule/kotlin.md、docs/rule/ai-change-governance.md、其它 docs/rule/*"。
  2. §4.4 Pass 3 在 client / game / core 三行的右侧 review 重点都补一句"对照 docs/rule/kotlin.md(命名、可空、不可变、并发、副作用)"。
  3. §4.4 加一个 governance 行(或单独的 §4.4.x 子段),引用 docs/rule/ai-change-governance.md,要求审查变更治理纪律(maintainabilityLint 触发条件、verifyChanged routing
  是否补齐、审计字段)。
  4. §3 Review 前预检 #6 新增"是否触发 maintainabilityLint、verifyChanged 路由更新"。

  推荐测试:
  - acceptanceContractLint 已经检查 ## 0. 开发治理与验收矩阵,可考虑加一项"Kotlin 改动文件数 ≥1 时 PR 文档必须显式声明是否触发 maintainabilityLint";由 PR 文档侧落地,review
  端只要追这一行字段。

  ---
  P1-4 Pass 8 删除审计未把"PR 文档 MUST 列出删除清单"作为硬要求,与当前 dark UI/UX 实际流程相悖

  证据:
  - 当前 git status 删除了 client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt、AsciiRenderer.kt、client/src/main/resources/debug/tileset_foundation_ascii_*.png
  以及对应测试,这些是 dark-uiux PR-01 的关键产物。
  - UI/pr/README.md:108-112 的 Visual Manifest Field Policy 已经把"禁止 ASCII fallback 字段"作为长期合同。
  - 规范 §4.9 Pass 8 列了"被替换路径是否有删除计划",但措辞 检查 偏温和,没明确"PR 文档必须给出 added / modified / deleted 三栏清单"。
  - 规范 §4.5 Pass 4 第 1 条只说"PR 文档是否列出需要新增、修改、删除的文件",但没把这条与 Pass 8 闭环。

  问题:
  若 PR 文档不给删除清单,reviewer 只能事后用 git status 逆推。一旦漏 1 个废弃文件(测试 fixture、旧 manifest 字段、旧 docs 章节、旧 golden label 等),后续 PR
  就会"看似可用"地复活旧路径。规范现在的措辞只能在 Pass 8 抓到这种问题,而抓不住"PR 文档源头没写"。

  影响:
  - 增量复审里 Pass 0 不会显式 fail。
  - acceptanceContractLint 当前只校验"是否有验收矩阵",不会检查"是否有删除清单";规范应该提示这条。
  - 对 dark UI/UX 这类长期清理 ASCII / 旧 fallback 的系列尤其致命。

  修复方向:
  1. §4.5 Pass 4 第 1 条扩为:"PR 文档必须给出 Added / Modified / Deleted 三栏文件清单,且 Deleted 一栏对应 §4.9 Pass 8 的 removal plan,二者一一映射。"
  2. §4.9 Pass 8 第 1 条改为:"PR 文档 Deleted 清单中的每一条必须配 removalPlan 字段:删除 PR、回归扫描入口(如 manifestLint 严格未知字段)、禁止复活规则。"
  3. §8 完成定义补一条:"所有删除文件、废弃 manifest 字段、废弃 locale token、废弃 golden label 都已写入 Deleted 清单和 removalPlan。"

  推荐测试:
  - acceptanceContractLint 增加结构化检查:UI/pr/dark-uiux-pr*.md 必须含 ## N. 文件影响范围 段且包含 Deleted 子段(可空但不可缺);若 PR 实际删了文件而 Deleted 段为空,fail。
  - 在 review 工具脚手架里加 git diff --diff-filter=D --name-only $BASE..HEAD,reviewer 必须对照 PR 文档 Deleted 清单。

  ---
  P1-5 §4.6 Acceptance Matrix 字段定义与 UI/pr/development-governance.md 不一致,且未声明上下游关系

  证据:
  - 规范 §4.6 表格 owner 取值:core / game / client / tools / assets / docs,whitebox 取值:required / skipped / N/A。
  - UI/pr/development-governance.md:18-29 表格 owner 取值仅:client / assets / tools / docs,whitebox 取值:required / skipped / N/A,字段名也叫 requirementId / source / owner /
  fastCheck / ownerGate / artifact / whitebox。
  - 两文档没有互相 cross-link;规范没有说明 development-governance 是 dark UI/UX 系列的子合同。

  问题:
  1. 同一字段两套合法值。dark UI/UX PR 的 Acceptance Matrix 由 development-governance 收敛,但规范又允许 core/game,reviewer 写报告时无所适从。
  2. PR 级规范号称要覆盖"docs/phase*、docs/review/phase*/*/PR、docs/opt/ui-pr、UI/pr"全部,但只参考一份子合同。
  3. development-governance 还有 Gate Ladder(§3)、Canonical Artifact(§5)、Doc-Vs-Implementation Self-Audit(§6)等条款,规范没有把这些纳入"Pass 5 Test and Gate Matrix"或 §7 模板,导致
  dark UI/UX PR review 既要满足规范也要满足 governance,容易冲突。

  影响:
  - reviewer 在 dark UI/UX PR 上很可能出现两套字段并存的混乱报告。
  - 后续 phase2/phase3/phase4 PR 没有等价 governance,只能套规范默认值,但默认值没有列出 fast lane 命令清单或 canonical artifact 清单。

  修复方向:
  1. §4.6 加一段:"本节是 PR 级 review 的最小公共合同。各 PR 系列可在自家 governance 里收敛 owner / whitebox 的合法值并补充 Gate Ladder / Canonical Artifact / Failure Rule(参考
  UI/pr/development-governance.md)。如果系列 governance 与本节冲突,以系列 governance 为准。"
  2. 增加一个表格"系列 governance 索引",至少列出:dark UI/UX → UI/pr/development-governance.md;phase4 v4-pr → docs/review/phase4/v4-pr/README.md(若存在);phase3 PR →
  docs/review/phase3/...。
  3. §3 Review 前预检 #1 把"是否有同系列 governance"列为必做项。
  4. development-governance.md 反向也加一行回链到 docs/review/rule/pr-level-review-standard.md,在两份文档头部互相声明上下游(规范 = 通用方法论 / governance = 系列合同)。

  推荐测试:
  - 在 acceptanceContractLint 增加:若目标 PR 文档目录存在 development-governance.md 或同名 governance,Acceptance Matrix 的 owner / whitebox 取值必须是 governance 子集,否则 fail。

  P2

  P2-1 §4.6 Acceptance Matrix 缺 removalOwner 与 crossPrDependency 字段

  证据:docs/review/rule/pr-level-review-standard.md:150-160 Acceptance Matrix 字段;UI/pr/README.md:24 说明 PR-01-1 等价截图替换需要在 manual record 中说明映射关系;§4.7 / §4.9
  都强调跨 PR 删除责任唯一。

  问题:矩阵没有删除责任、跨 PR 依赖列。Pass 7 / Pass 8 抓的问题在矩阵里没有锚点,reviewer 无法机械复核"删除是否归属唯一 PR"。

  影响:跨 PR 删除责任只能藏在散文里,review 报告间继承困难。

  修复方向:Acceptance Matrix 表格加两列(可选,但 blocking 行为必须填):removalOwner(若该 requirement 替换旧路径)、crossPrDependency(指向上游/下游 PR 的 requirementId)。

  推荐测试:acceptanceContractLint 增量校验:Pass 8 抓到的删除 requirement,矩阵中 removalOwner 必须非空。

  ---
  P2-2 Pass 2 状态机维度过偏 client presenter,对 tools / 资源 / 文档 PR 不普适

  证据:docs/review/rule/pr-level-review-standard.md:91-104 列出 identity changed / unchanged、modal stack scan direction,这些只在 client viewport / overlay 场景成立。

  问题:tools PR(verification、lint、report)、资源 PR(sheet plan、manifest)、纯文档 PR 不存在 modal/identity 概念,reviewer 会强行套用或干脆跳过。

  影响:Pass 2 在非 UI PR 里要么虚走要么漏检。

  修复方向:Pass 2 拆成"通用维度(正常 / 空输入 / enter-update-exit / failure semantics)"与"按 PR 类型增量维度":UI 级补 modal stack、identity;tools 级补 cache hit/miss、incremental
  input change;资源级补 missing key / fallback chain、unsupported field。

  推荐测试:N/A,文档级修改即可。

  ---
  P2-3 §11 红线漏掉本仓库高频陷阱

  证据:docs/review/rule/pr-level-review-standard.md:419-432 红线 12 条;但仓库实践还存在以下高频陷阱:
  - canonical(assets-src/image/manifests/phase2-visual-manifest.json)与 runtime(client/src/main/resources/manifests/visual-manifest.json)双向 desync(UI/pr/README.md:138)。
  - locale / contractLint:中文 UI 文案不通过 localeLint、contractLint(UI/pr/README.md:282)。
  - prefixRules 与显式 entry 二者覆盖(UI/pr/README.md:140-141)。
  - 测试依赖系统时钟、本机时区、环境变量、绝对路径 fixture。
  - golden label 跨 PR 复用、golden 旧基线未刷新。

  问题:reviewer 不会主动想到这些项目特定红线,容易放过 manifest desync 与 golden 漂移。

  修复方向:§11 补 5–6 条:
  - "13. canonical 与 runtime visual manifest 必须由 syncPhase2Manifests 保证一致,任何只改一侧的 PR 视为 P0。"
  - "14. UI 文案 / locale token / presentation token 改动未跑 localeLint contractLint。"
  - "15. visual manifest prefixRules 与显式 entry 二者覆盖同一 key 但优先级未声明。"
  - "16. 测试依赖 System.currentTimeMillis、本机时区、绝对路径或 ~/.codex/generated_images 这类 transient source。"
  - "17. golden label 跨 PR 复用或旧基线未与当前 PR 一起刷新。"

  推荐测试:无,文档级红线清单。

  ---
  P2-4 §3 Review 前预检未要求"工作树状态确认"与"基准分支确认"

  证据:当前仓库 git status 中有 30+ 个 modified / deleted 文件(dark-uiux-pr01 在做),docs/review/rule/pr-level-review-standard.md:38-48 未要求 reviewer 先确认 base / HEAD /
  是否包含未提交 in-progress 内容。

  问题:reviewer 在脏工作树或错分支上 review,会把别人的 in-progress 当成 PR 的一部分,产生伪 finding。

  修复方向:§3 加一条预检:
  - "0. 已执行 git status 与 git log --oneline $BASE..HEAD,确认工作树干净、PR 基准 ref 与目标 PR 一致;否则先 git stash 或换分支再 review。$BASE 默认为 PR 文档声明的依赖 PR
  合并入主干后的状态。"

  推荐测试:无。

  ---
  P2-5 §6 Finding 写法没规定 finding ID 命名空间与跨报告复用规则

  证据:docs/review/rule/pr-level-review-standard.md:268-284 推荐写法只有 #### P1-1,没说明 ID 是 review 内唯一还是跨 review 稳定。

  问题:增量复审引用上一轮 finding 时不能机械锚定;同一 PR 多轮 review 中 P1-1 含义可能漂移。

  修复方向:§6 加一段 ID 规则:
  - "finding ID 仅 review 内唯一;跨 review 引用时使用 report-shortname#P1-1 形式(如 pr01-r2#P1-1)。"
  - "增量复审报告必须保留上一轮所有 P0/P1 ID 的状态(已解决 / 部分解决 / 未解决 / 新引入),不允许重新编号。"

  推荐测试:无。

  ---
  P2-6 §7 报告模板章节与 Pass 顺序未闭环,模板里"当前阶段必须解决的问题"与"Removal/Iteration Plan"无产出指引

  证据:docs/review/rule/pr-level-review-standard.md:286-350 模板包含这两节,但 §4 各 Pass 没说哪一段产出它们。

  问题:reviewer 写到这两节时没有判定标准,容易随手填或留空。

  修复方向:在 §4.7 Pass 7 末尾加"产出 当前阶段必须解决的问题",在 §4.9 Pass 8 末尾加"产出 Removal/Iteration Plan"。同时在 §7 每节标题后用一行说明对应 Pass。

  推荐测试:无。

  ---
  P2-7 Pass 6 Gameplay/UX Director 对非 player-facing PR 应给出明确"豁免理由"模板

  证据:docs/review/rule/pr-level-review-standard.md:170-181 把 Pass 6 设为条件性,但没说明纯 backend/tools PR 应如何记录"无适用项"。

  问题:reviewer 在 tools / verification PR 上要么跳过 Pass 6,要么硬写一段无意义内容,后者反而稀释玩法 review 的严肃性。

  修复方向:Pass 6 末尾加:"若 PR 无 player-facing surface,本节必须写一行 N/A: 无 player-facing surface,理由:<具体路径或证据>,不允许直接省略整节。"

  推荐测试:无。

  ---
  P2-8 规范没有 review 报告归档命名 / 路径约定

  证据:docs/review/ 当前混合放置 phase / dark UI/UX / 通用 review,命名前缀有 2026-04-12-pr01- / 2026-04-04- / phase4-pr05
  多种;UI/pr/dark-uiux-pr00-style-and-pipeline-contract.review.md 又放在 PR 同目录。

  问题:增量复审第 1 步"先读取上一轮 report"在没有命名 / 路径约定时检索成本高。

  修复方向:在 §9 之后加 §x "Review 报告归档":
  - 路径:跨 phase 通用 review → docs/review/;phase 内 → docs/review/phaseN/...;PR 系列内 → 与 PR 文档同目录,扩展名 .review.md。
  - 命名:<yyyy-mm-dd>-<series>-<pr-id>-review[-r<n>][-suffix].md,suffix 用 post-fix / cross-reference / final 等已存在词汇。

  推荐测试:acceptanceContractLint 可加 docs path 约定校验;非阻塞。

  ---
  P2-9 §4.5 Pass 4 缺"verifyChanged routing 与 maintainabilityLint 触发"维度

  证据:UI/pr/README.md:27-28、277 与 AGENTS.md 都把 verifyChanged routing 与 maintainabilityLint 视为结构性 PR 必跑;规范 §4.5 第 4 条只笼统提"presenter / resolver / registry /
  manifest / locale / fixture / golden / report"。

  问题:reviewer 容易漏验"该 PR 的改动路径是否已写入 verifyChanged impact routing"以及"Kotlin diff 文件数 ≥ 5 是否补跑 maintainabilityLint"。

  修复方向:Pass 4 加 2 条:
  - "PR 改动路径是否已写入 verifyChanged impact routing(tools/.../VerificationTaskRegistry.kt 或对应 routing 真源)。"
  - "Kotlin 改动文件数 ≥5、新增 public presentation model 或 renderer 共享组件重排时,PR 文档是否声明 maintainabilityLint,且 review 是否核对该 lint 已跑。"

  推荐测试:见 P1-3 推荐 lint。

  P3

  P3-1 §10 命令块可读性与适用范围

  证据:docs/review/rule/pr-level-review-standard.md:401-405。

  修复方向:
  - awk '...' <changed-doc> 应明确建议只对单文件运行,并给出多文件场景的循环示例。
  - rg -n "$ABS_PATH_PATTERN" <changed-docs> 限定 --type md,避免扫码块或二进制资源。
  - 按 CLAUDE.md §6 推荐用 rtk 包装 git status / git log / find / ls;若有 rtk,可在 §10 加一行"建议用 rtk 包装高噪声命令以便 review 摘要化"。

  ---
  P3-2 §1 与 §8 表述重叠且无反向引用

  证据:§1 第 16-22 行 5 条"可开发文档必须具备" 与 §8 10 条完成定义重叠。

  修复方向:§1 末尾加一行 "完整完成定义见 §8";§8 头加一行 "本节扩展 §1 的 5 条最小要求"。

  ---
  P3-3 §4.4 模块表行内措辞虚化

  证据:docs/review/rule/pr-level-review-standard.md:111-117,client 行写"是否避免规则权威副本",tools 行没把"default-success / 空数组 / 伪 coverage"显式列入(虽下方 §4.4 段落中有列)。

  修复方向:把 §4.4 表后面的"必须特别检查"4 条直接挪到表格对应行尾,合成"模块边界 + 红线"一图查。

  ---
  P3-4 §5 P3 描述与 §1 强调"长期可维护性"之间略有矛盾

  证据:§5.4 P3 列了"建议补充 evidence label、manual record 字段或交叉引用",但 evidence label 缺失常常会让后续 PR 找不到证据,实务上更接近 P2。

  修复方向:把"evidence label / manual record 字段缺失"上移到 P2 list,P3 严格限定为"命名一致性、错别字、章节顺序、可读性"。

  ---
  P3-5 缺与 docs/INDEX.md 的双向回链

  证据:docs/INDEX.md 当前未列 docs/review/rule/pr-level-review-standard.md。

  修复方向:
  - 规范 §0(可在 §1 之前加引言段)加一行 "本规范登记于 docs/INDEX.md"。
  - docs/INDEX.md §1 文档优先级里追加该规范条目,优先级建议放在"统一白盒验证框架"之后、phase 文档之前。

  ---
  Requirement Alignment

  ┌────────────────────────────────────────┬───────────────────────┬───────────────────────────────────────────────────────────────────────┐
  │              Requirement               │       Evidence        │                              Conclusion                               │
  ├────────────────────────────────────────┼───────────────────────┼───────────────────────────────────────────────────────────────────────┤
  │ 让 review 直接驱动开发,不靠猜测        │ §1, §6 推荐写法       │ 主体达成,P1-3、P1-5 会让部分 reviewer 无法机械落地                    │
  ├────────────────────────────────────────┼───────────────────────┼───────────────────────────────────────────────────────────────────────┤
  │ 覆盖 docs/phase*、UI/pr 等所有 PR 文档 │ §1 第 1 段, §2 优先级 │ 覆盖性 OK,但与 dark UI/UX governance 关系未声明(P1-5)                 │
  ├────────────────────────────────────────┼───────────────────────┼───────────────────────────────────────────────────────────────────────┤
  │ Pass 0–8 全局结构化                    │ §4                    │ 顺序合理;Pass 2 / 4 / 8 维度需补强(P1-4、P2-2、P2-9)                  │
  ├────────────────────────────────────────┼───────────────────────┼───────────────────────────────────────────────────────────────────────┤
  │ 严重级别可机械执行                     │ §5                    │ P0–P3 框架可用,但与历史报告不兼容(P1-1)                               │
  ├────────────────────────────────────────┼───────────────────────┼───────────────────────────────────────────────────────────────────────┤
  │ 自检命令可重现                         │ §10                   │ 不可重现,缺 SDKMAN init(P1-2)                                         │
  ├────────────────────────────────────────┼───────────────────────┼───────────────────────────────────────────────────────────────────────┤
  │ 可开发文档完成定义清晰                 │ §8                    │ 10 条充分,但缺"删除清单 / removalPlan / 系列 governance 子集"显式条款 │
  └────────────────────────────────────────┴───────────────────────┴───────────────────────────────────────────────────────────────────────┘

  功能/系统一致性矩阵

  ┌────────────────────────┬─────────────────────────┬────────────────────────────────────────────────────┬──────────────────────────────┬──────────┐
  │       模块/系统        │        文档合同         │                 当前代码/文档状态                  │             偏差             │ 严重级别 │
  ├────────────────────────┼─────────────────────────┼────────────────────────────────────────────────────┼──────────────────────────────┼──────────┤
  │ Acceptance Matrix 字段 │ 规范 §4.6 全集          │ dark UI/UX governance §2 子集                      │ 字段值与名重叠但未声明上下游 │ P1-5     │
  ├────────────────────────┼─────────────────────────┼────────────────────────────────────────────────────┼──────────────────────────────┼──────────┤
  │ Gradle 命令前置        │ 规范 §10 直接 ./gradlew │ README §通用验证入口要求 SDKMAN init               │ 缺 init,命令不可重现         │ P1-2     │
  ├────────────────────────┼─────────────────────────┼────────────────────────────────────────────────────┼──────────────────────────────┼──────────┤
  │ 严重级别               │ 规范 §5 P0–P3           │ 历史 review 用 HIGH/MEDIUM/LOW、D{n}               │ 词汇映射缺失                 │ P1-1     │
  ├────────────────────────┼─────────────────────────┼────────────────────────────────────────────────────┼──────────────────────────────┼──────────┤
  │ 必读规则文档           │ 规范 §2 仅 docs/rule/*  │ AGENTS.md 强制 kotlin.md / ai-change-governance.md │ 漏列 + Pass 4 不引用         │ P1-3     │
  ├────────────────────────┼─────────────────────────┼────────────────────────────────────────────────────┼──────────────────────────────┼──────────┤
  │ 删除审计               │ 规范 §4.9 检查删除      │ git status 实际删除若干 ASCII renderer 文件        │ PR 文档无强制 Deleted 清单   │ P1-4     │
  ├────────────────────────┼─────────────────────────┼────────────────────────────────────────────────────┼──────────────────────────────┼──────────┤
  │ verifyChanged routing  │ AGENTS.md / README 强调 │ 规范 Pass 4 未列                                   │ 漏检                         │ P2-9     │
  ├────────────────────────┼─────────────────────────┼────────────────────────────────────────────────────┼──────────────────────────────┼──────────┤
  │ 报告归档               │ 现有 review 多种命名    │ 规范无约定                                         │ 检索成本高                   │ P2-8     │
  └────────────────────────┴─────────────────────────┴────────────────────────────────────────────────────┴──────────────────────────────┴──────────┘

  玩法与体验审查

  规范本身不直接影响玩家体验,但 Pass 6 是规范唯一兜住玩法 review 的地方。P2-7 指出该节对非 player-facing PR 缺豁免模板,会稀释玩法 review 的严肃性;P2-3 红线缺失会让 reviewer 漏掉
  manifest desync / golden 漂移这类直接影响玩家可见效果的项目特定陷阱。除此之外,规范对玩法 review 的覆盖(战斗/探索/奖励/成长/构筑闭环、UI 高频操作、新视觉是否提升信息清晰度)与 K-ToME
   的 player-facing 验收实际口径(packaged app whitebox + golden + manual record)是对齐的。

  当前阶段必须解决的问题

  合并前必须吸收:
  1. P1-1 严重级别历史映射。
  2. P1-2 §10 命令块前置 SDKMAN init。
  3. P1-3 §2 / §4.4 引入 kotlin.md、ai-change-governance.md。
  4. P1-4 §4.5 / §4.9 / §8 强制 Deleted 清单与 removalPlan。
  5. P1-5 §4.6 声明与系列 governance 的上下游关系并加索引。

  合并后短期内吸收(微 PR):
  - P2-1、P2-3、P2-4、P2-9。

  可推迟到下一轮文档润色:
  - P2-2、P2-5、P2-6、P2-7、P2-8、所有 P3。

  Removal/Iteration Plan

  - 规范本身没有引入需要回滚的实体,主要是文本扩展。
  - 修复 P1-2 时,可考虑直接引用 UI/pr/README.md:248-253 的 SDKMAN 段落,避免再维护一份命令块,降低未来漂移概率。
  - 修复 P1-5 时,在 UI/pr/development-governance.md 反向加回链,保持双向 cross-link 不断裂。

  Additional Suggestions

  1. 在规范里加一段"reviewer 角色推荐"——历史报告普遍自带"Roguelike 开发设计总监 / 系统策划总监 / 玩法体验审查官"三栖标签,统一一下能让多份 review 之间的口径更稳。
  2. 把"输出 Open Questions 列表"作为推荐结构;UI/pr/dark-uiux-pr00-...review.md 的 Open Questions 段落已经形成事实标准,可以在 §7 模板里追加一节(可选)。
  3. 规范末尾可增设"快速 onboarding"清单,给一份 reviewer 的 5–10 分钟最小命令包(rg 高频违禁词、awk fence 检查、git diff stat、log $BASE..HEAD),降低新人 reviewer 上手成本。

  Suggested Verification

  文档级修改(吸收上述 P1)后,建议执行(已运行 ✅ / 建议运行 ⏳):
  # ⏳ 建议运行
  source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
  git diff --check -- docs/review/rule/pr-level-review-standard.md
  awk 'BEGIN{c=0} /^```/{c++} END{print "FENCE_OPEN=" c%2}' docs/review/rule/pr-level-review-standard.md
  rg -n --type md '/(Users|tmp)/|[A-Za-z]:\\\\' docs/review/rule/
  ./gradlew acceptanceContractLint
  本轮我未实际运行任何 Gradle / shell 校验,仅做了文档对照与代码 / git status 阅读;若吸收 P1-2 后,应由作者运行上述命令并把结果写入修订记录。

  Summary

  pr-level-review-standard.md 把 K-ToME PR 级 review 的 Pass、严重级别、Findings 写法和报告模板抽象成单一权威,主体设计扎实。但它在与项目真实的"必读规则文档(kotlin.md /
  ai-change-governance.md)、SDKMAN init 前置、dark UI/UX governance 子合同、删除清单纪律、历史 review 严重级别"五个面上还没有闭环,直接交付给一个不熟悉本仓库的
  reviewer,会出现"按规范跑命令直接失败 / 不知道如何吸收旧 review / 漏掉删除责任 / 与 governance 字段冲突"的实际问题。建议先合 5 个 P1 修复,P2/P3 在随后的文档微 PR
  中逐步收口,并把规范登记进 docs/INDEX.md 与 development-governance 的双向回链。
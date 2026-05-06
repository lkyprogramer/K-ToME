# Phase4 v4 PR-01 Profession Tree Run Choice 实施审查报告

**审查角色**: Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
**审查对象**: 当前分支 `codex/phase4-v4-pr01-profession-tree-run-choice`
**对照文档**: `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md`（PR-01 规约）
**对照基线**: `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md`（设计补充）
**审查日期**: 2026-04-26
**审查方法**: 静态代码 / 数据 / i18n / 报告聚合 / 验证场景对照（未运行 Gradle 验证命令）

---

## 0. 审查总评

| 维度 | 结论 | 备注 |
| --- | --- | --- |
| 玩家体验目标（§1） | **大体达成** | 升级 = 学习 vs 升 rank 的资源竞争语义已落地；前 30 分钟职业树承诺压力存在；UI 三列树以"sidebar 列式"形态实现，与文档示例的"水平三栏"略有出入但功能等价。 |
| 范围与非目标（§3） | **达成** | 未扩张基础职业技能数量；未触碰 `shadowblade / warden`；未引入数值型局外成长；未伪造 client 第二真源。 |
| 资源要求（§4） | **达成** | 无新图片/音频资源；学习新技能复用 `audio.ui.talent_unlock`；新 UI 复用既有 token 与 i18n key。 |
| 技术方案（§5） | **大体达成，1 处数据偏差 + 2 处签名/形态差异** | 见下文 §3。 |
| 测试与自证（§6） | **结构齐全** | `phase4-v4-pr01` 验证场景已注册并要求 `log.talent.learned/rank_up/breakpoint_chosen` 三事件、五份证据；未实际运行 `./gradlew … :game:test longRunLab reportPhase4Only` 等命令以核对真实指标。 |
| 报告与验收指标（§7） | **达成** | 4 项 blocking + 4 项 supporting 指标全部进 `Phase4MetricCatalog`；owner baseline JSON 与文档阈值一致；包含 3 类职业分组元数据。 |
| 完成定义（§9） | **9/11 全部达成；1 项数据细节偏差；1 项 UI 形态差异** | 详见 §4。 |

**整体评价**: 实现与规约**核心契约（破坏性改造、3 starter、Tier 2/3 三重锁、断点事件、owner 指标）已经全部对齐**；剩余 3 处偏差全部是"设计意图已落地但与文档表述对不齐"的局部不一致，不阻塞玩家体验目标，但应当在合并前以文档或数据小修正方式收敛。

---

## 1. 已对齐项（逐条核查）

### 1.1 §5.1 `TalentProgression` 语义重命名

- ✅ `unlockedTalentIds` 全仓库零残留：`grep -rn "unlockedTalentIds"` 返回 0 命中。
- ✅ `learnableTalentIds` 已实现于 `game/src/main/kotlin/com/ktome/game/TalentProgression.kt:55`，过滤掉已学（`rank > 0`）节点，按职业树 + 种族树合并 + 顺序去重。
- ✅ 调用点全部迁移：`FoundationGameSession.kt:935-938, 9254, 9274` 消费 `learnableTalentIds`；`SessionSnapshotMapper`/`GameView` 无 `unlocked` 残留。
- ✅ 无 `@Deprecated` wrapper。

### 1.2 §5.2 Talent 状态模型

- ✅ `TalentNodeStateSnapshot` 枚举位于 `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:363`，四态完整：`LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE`。
- ✅ 状态判定在 `FoundationGameSession.kt:4472-4475`，按 `rank / lockReasons / activeTalentIds` 派生，符合规约。

### 1.3 §5.3 起始技能固定改造

- ✅ `professions/index.yaml` 中 4 个 `BASE` 职业 + `berserker / spellblade` 全部固定为 3 starter（`vanguard:[power_strike,shield_bash,guard_stance]`、`arcanist:[fireball,blink,arcane_shield]`、`rogue:[backstab,stealth,roll]`、`templar:[holy_strike,holy_light,holy_shield]`、`berserker:[blood_rush,savage_hew,kill_frenzy]`、`spellblade:[arcane_edge,mana_lunge,spell_parry]`）。
- ✅ 第 4 个职业行动槽空：`EntityFactory.kt:83-87` 仅按 starter 列表绑定 slot 1..N，slot 4 自然为空。
- ✅ DataLoader fail-fast：`game/src/main/kotlin/com/ktome/game/data/DataLoader.kt:382-383` 强制非 frozen 职业 starter 数 == 3；`L395-397` 强制至少存在一个非 starter learnable。
- ✅ `shadowblade / warden` 保留 `tier: ADVANCED + tags:[…, frozen]`，`startingTalents:[]`；DataLoader `L375-380` 按 frozen 旁路。
- ✅ owner baseline JSON 中 `includedProfessions / advancedReportOnlyProfessions / excludedFrozenProfessions` 三段元数据齐全。

### 1.4 §5.4 树投入与 Tier 门槛

- ✅ Tier 1/2/3 等级阈值（1/3/5）见 `TalentProgression.tierUnlockLevelRequirement`。
- ✅ 同树投入 ≥2/≥5 见 `sameTreeInvestmentRequirement`。
- ✅ 跨树投入 Tier 2 要求另一棵职业树投入 ≥1：`talentLockReasons`（`L119-135`）实现"profession 树才计跨树"，避免种族树误算。
- ✅ DataLoader fail-fast：Tier 2 节点必须声明 `minRank >= 2` 前置；Tier 3 必须声明 `minRank >= 3` 前置（`L417-426`）。
- ✅ `multiTreeInvestmentAboveThresholdRate` 公式与文档定义对齐：`Phase4MetricCatalog.kt:254` "at least two profession trees each invested >= 3 points"。

### 1.5 §5.5 Allocation Draft 与断点事件

- ✅ `log.talent.learned / rank_up / breakpoint_chosen` 三事件均在 `FoundationGameSession.kt:3937-4003` 一次性、按 `treeId → talentId → breakpointRank` 升序输出。
- ✅ Breakpoint payload 完整：`professionId / talentId / treeId / breakpointRank / rankBefore / rankAfter / remainingTalentPoints`（`L3994-4001`）。
- ✅ 仅 `draft 确认时` 发出（事件构造逻辑位于 confirm 路径），hover/preview 不会触发。
- ✅ 同 draft 多 talent 多 breakpoint 时按 §5.5.6 排序输出。
- ✅ 四槽满时弹出 `ACTIVE_TALENT_SLOT_CHOICE` modal：`InputHandler.kt:1199, 1763, 1863, 1868` 与 `ModalStack.kt` 协作，`Esc` 取消（不扣点），`R` 入 reserve，`1~4` 替换。

### 1.6 §5.6 Talent UI 输入规则

- ✅ `T` 打开 `TALENT_ASSIGN` 模态。
- ✅ 方向键移动：`InputHandler.kt:1268, 1272` 调用 `moveTalentTreeColumn` 切换"列"（树）。
- ✅ `P` 切换 preview：`InputHandler.kt:1245-1246`。
- ✅ `Enter` 确认 / 创建 draft；`Esc` 取消。
- ✅ 三列树 UI 中 `1~4` 不改变选择；footer 显示 `ui.talent.tree.footer.active_slots_from_slot_panel`（`TalentSidebarPresenter.kt:99`，i18n 中英双文已就位）。

### 1.7 §5.7 存档与 fixture 破坏性边界

- ✅ Fail-fast 错误 `INCOMPATIBLE_PHASE4_V4_TALENT_SCHEMA: Start a new run.` 在 `core/src/main/kotlin/com/ktome/core/save/SaveCodec.kt:51` + `SaveSnapshot.kt:92`。
- ✅ 测试 `SaveCodecTest.kt:61, 92` 与 `GameModuleTest.kt:382, 400` 校验缺失/陈旧 schema 触发。
- ✅ `RunSummary` 新增 `starterProfessionTalentCount`、`learnableNonStarterTalentCount`、`autoLearnedNonStarterTalentCount`（`FoundationGameSession.kt:468, 935-966`）。

### 1.8 §6.3 人工白盒验证场景

- ✅ `ValidationScenarioRegistry` 注册 `phase4-v4-pr01`（`L66-132`）：seed `2026042431`、locale `zh-CN`、profession `vanguard`、4 张证据图 + 1 份 app.log、3 个必需日志事件 key。
- ✅ Manual record path `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr01-profession-tree-run-choice.md` 已创建（untracked file）。
- ✅ Whitebox CLI `Phase4V4WhiteboxScenarioCli` 与 manifest yaml 已就位。

### 1.9 §7 Report / Owner 指标

- ✅ `Phase4MetricCatalog.kt:235-297` 注册 4 项 blocking + 4 项 supporting，全部归属 `outputSection = "profession-tree-run-choice"`，`failSemantics / formula / decisionNotes` 与文档逐字对齐。
- ✅ `docs/review/phase4/opt/baselines/2026-04-24-phase4-profession-tree-run-choice-owner-baseline.json` 阈值与文档表格一致：`starterProfessionTalentMaxCount<=3`、`learnedTalentChoiceEventRate>=0.9`、`multiTreeInvestmentAboveThresholdRate>=0.75`、`breakpointChoiceEventRate>=0.75`。
- ✅ `autoLearnedNonStarterTalentCount` 标记为 supporting / display only，未进入 blocking 分母（符合 §7.5）。
- ✅ `breakpointChoiceEventRate` 公式严格按 §5.5.5 写为 "terminal runs confirming … / terminal runs with at least one breakpoint preview"，明确"hover / preview-only state 不计入"。

---

## 2. 偏差清单（按严重度排序）

### 2.1 [中] `spellblade` 起始树分布表与数据 + 设计补充不一致 — **文档 BUG，非代码 BUG**

**偏差内容**

- PR-01 文档 §5.3 起始树分布表写的是：

| 职业 | 开局有 starter 的树 | 开局为空的树 |
| --- | --- | --- |
| `spellblade` | `spellblade_enchanted_blade` `1`、**`spellblade_battle_spell` `2`** | **`spellblade_elemental_flux`** |

- 实际数据（`game/src/main/resources/data/talents/index.yaml`）：

| 树 | nodes | 含 starter 数 |
| --- | --- | --- |
| `spellblade_enchanted_blade` | `[arcane_edge, spell_rend, runic_edge, sunder_sigil]` | **1** (`arcane_edge`) |
| `spellblade_elemental_flux` | `[mana_lunge, spell_parry, blink_strike, counter_seal]` | **2** (`mana_lunge`, `spell_parry`) |
| `spellblade_battle_spell` | `[flux_anchor, flux_burst, balance_point, flux_reversal]` | **0** |

- 设计补充 `phase4_profession_tree_inscription_design_supplement.md` 与数据**一致**：

> `spellblade` | `arcane_edge`, `mana_lunge`, `spell_parry` | `flux_anchor` | 保留混合输出、位移接战、防御；把资源稳定器留给升级选择

设计补充把 `flux_anchor` 列为"learnable 第 4 技能"，而 `flux_anchor` 在数据中位于 `spellblade_battle_spell` —— 即 `spellblade_battle_spell` 是"开局为空、首次升级可学"的树，这与 PR 文档表的 `spellblade_battle_spell 2` 矛盾。

**偏差量**

PR 文档把 `spellblade_battle_spell` 与 `spellblade_elemental_flux` 在 §5.3 表中写反了。数据 + 设计补充内部自洽，**实现侧没有偏差**。

**影响**

- 玩家体验：无影响（实际 starter 仍是 3，`battle_spell` 仍是空树并在升级时第一可学 `flux_anchor`）。
- 校验链：DataLoader 只校验 starter 数 == 3 与"至少一个非 starter learnable"，不校验"哪棵树 starter 数为 N"，因此运行时不报错。
- 文档可信度：策划/审查侧顺着 PR 文档表去复核数据时会得到"数据错"的错误判断（实际是文档错）。

**修复优化建议**

仅修文档，零代码改动：

```diff
-| `spellblade` | `spellblade_enchanted_blade` `1`、`spellblade_battle_spell` `2` | `spellblade_elemental_flux` | ADVANCED 只做 starter / learnable coverage |
+| `spellblade` | `spellblade_enchanted_blade` `1`、`spellblade_elemental_flux` `2` | `spellblade_battle_spell` | ADVANCED 只做 starter / learnable coverage |
```

并在 PR 文档"设计效果"列追加一句"battle_spell 留作升级首学路线，承担 §5.4.4 multi-tree 投入的副树入口"，与设计补充语义保持一致。

---

### 2.2 [低] `learnableTalentIds` 函数签名与文档原型不一致

**偏差内容**

PR 文档 §5.1 给出原型：

```kotlin
fun learnableTalentIds(
    schemaCatalog: SchemaCatalog,
    profession: ProfessionSchemaV2,
    level: Int,
    learnedRanks: Map<String, Int>,
    race: RaceDef? = null,
): List<String>
```

实际实现（`TalentProgression.kt:55`）：

```kotlin
fun learnableTalentIds(request: TalentProgressionRequest): List<String>
```

把五个入参折叠成一个 `TalentProgressionRequest` data class。

**偏差量**

API 形态变化，行为等价（同一个 `TalentProgressionRequest` 同时被 `talentLockReasons / evaluationContext` 共享，避免重复计算 `evaluationContext`）。

**影响**

- 调用点：仓内调用点全部已迁移，不存在编译错。
- 性能：实际更优（共享 `evaluationContext` 避免 N×O(树)）。
- 规约对齐：违反 §5.1 原型。

**修复优化建议**

二选一：

1. **保留现实现 + 修文档**（推荐）：在 PR 文档 §5.1 原型旁补一行"出于复用 `evaluationContext` 的考虑，正式实现把入参聚合为 `TalentProgressionRequest`，对外语义与文档原型等价"。
2. **加一层薄 facade**：保留 `request` 主入口，新增一个文档原型一致的 5-arg 重载内部转发到 `learnableTalentIds(TalentProgressionRequest(...))`。增加一个浅函数，但满足规约字面要求。

---

### 2.3 [低] Talent UI 三列树呈现形态：垂直 sidebar 列式 vs 文档水平三栏

**偏差内容**

PR 文档 §5.6 给的可视化是典型水平三栏 ASCII：

```
[Arms]                 [Shield]               [Warcry]
power_strike R2        shield_bash R1         war_cry Learn
charge Learn           guard_stance R1        intimidation Locked: ...
```

实际渲染走的是 `TalentSidebarPresenter`（`client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt`）：每棵树作为一个 sidebar section，节点垂直堆叠，三棵树前后串联出现，**非水平 3 栏**。

但是：
- 输入侧（`InputHandler.moveTalentTreeColumn`）确实把"左右方向键"实现为"在三棵树之间换列"，即逻辑上把每棵树视为一列。
- `P` 切换 preview、preview 紧贴选中节点显示。
- 锁定原因、学习预览、断点预览都通过 `DescriptionPresenter` 进入 sidebar 行。

**偏差量**

- 玩家可读性：从"一屏看清三棵树并列"退化为"一屏只看一棵树连续展开 + 方向键切换"。在 80 列 ASCII 终端里，水平三栏每列只能给约 25 字符，节点名+rank+lock 原因往往要截断；当前 sidebar 形态空间充裕但需要纵向滚动/视觉跳转。
- 功能对齐：§5.6 的输入规则、`1~4` footer、preview 都已落地；§6.5 玩家 golden path 第 1 条"打开 Talent UI 玩家第一眼必须看到 3 个 learned starter、1 个空 active slot、至少 1 个 learnable 节点和至少 1 个 locked 节点" —— 在 sidebar 列式下，玩家仍能在首屏看到所有四类节点（vanguard 三棵树各只有 5~6 个节点，sidebar 总高度约 20 行可一屏完整）。

**影响**

- §1 玩家体验目标第 7 条"Talent UI 展示三列树、全量节点、锁定原因、学习预览、断点预览" —— 全量节点 / 锁定原因 / 学习预览 / 断点预览均已落地；"三列树"需要看是从输入语义还是视觉布局判断：
  - 输入语义层：左右方向键= 列切换 ✓
  - 视觉布局层：当前是垂直 sidebar，**与文档示例不完全一致**

**修复优化建议**

按工作量从小到大三档：

1. **接受现状 + 文档收敛**（最小改动）：修改 PR 文档 §5.6 把示例 ASCII 改为 sidebar 形态描述，并在 §1 第 7 条"三列树"加注"以列式输入语义实现"。
2. **加 ASCII 三栏渲染层**（中等改动）：在 `AsciiRenderModel` 把 `TalentSidebarPresenter` 输出按 `TREE_HEADER` 边界切片重排为水平 3 栏，每栏控制宽度 26 字符，为节点名/rank/lock 原因预留省略号截断。Tile 渲染器同步调整。这是文档示例字面对齐方案。
3. **重做 `TalentTreeColumnsPresenter`**（大改动）：新增独立 presenter 走纯三栏布局，把当前 sidebar 留作小屏 fallback。

**推荐**：方案 1 或方案 2。考虑到 §1 已经写明"三列树 / 全量节点 / 锁定原因"作为体验目标，方案 2 在不破坏现有数据流的前提下补齐视觉对齐，玩家可读性和文档对齐度都最好。如果决定保留 sidebar 形态，至少应当在 PR 文档 §5.6 显式说明"sidebar 列式呈现"，避免下游 PR 复用文档时按错误形态对齐。

---

## 3. 没有发现问题但建议加强的地方

### 3.1 ADVANCED 4 节点树的 Tier 切片需要在文档显式说明

`TalentProgression.talentNodeTier`（`L139-158`）对 `tree.nodes.size <= 4` 的树有特殊路径：

```kotlin
if (tree.nodes.size <= 4) {
    return when {
        index < 0 -> 1
        index <= 1 -> 1   // 前两个 = Tier 1
        index == 2 -> 2   // 第 3 个 = Tier 2
        else -> 3         // 第 4 个 = Tier 3
    }
}
```

这是 `berserker / spellblade` 4 节点树的 fallback，PR 文档 §5.4 表只描述了"6 节点树 1/2/3 = nodes[0,1] / nodes[2,3] / nodes[4,5]"。这一 fallback 实际合理（保证 ADVANCED 树仍有 Tier 2 和 Tier 3 各一个节点用于 prerequisites 校验），**但文档未提**。建议在 PR-01 文档 §5.4 末尾补一行：

> 对节点数 <= 4 的 ADVANCED 树（`berserker / spellblade`），Tier 切片为 `nodes[0,1]=Tier 1, nodes[2]=Tier 2, nodes[3]=Tier 3`，仅用于 prerequisites 数据校验通路，不进入 release-facing blocking 分母。

### 3.2 `careerTalentPointsByLevel` 数据 review 缺少集中记录

§5.4.6 要求"data review 记录每个基础职业的最小所需点数与实际可获得点数"。当前没有看到独立的 markdown 表格集中记录 4 个 BASE 职业 + 2 个 ADVANCED 职业的 min-required vs available。建议在 `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md` 或 PR 的 manual record 里补一张 6 行表格，证明 Tier 3 路线在等级上限内可达。这不是阻塞项但缺失会让"Tier 3 数学可达性"成为口头承诺。

### 3.3 `breakpointChoiceEventRate` 分母测量需要 longRunLab 实跑确认

我在静态审查中没有运行 `./gradlew longRunLab reportPhase4Only`，文档要求的"分母 = terminal run with at least one breakpoint preview"需要 longRunLab 真实输出包含 `breakpointPreviewObserved` 计数（或等价信号）才能区分"分母 0 → 跳过"和"分母 0 → vacuous pass"。建议在合并前手动跑一次 longRunLab 看一下产出 JSON 是否有这个字段、以及当所有 run 都 0 preview 时 owner gate 的 pass/fail 行为是否符合预期（推荐 vacuous 时 `expectedFailureCodes` 显式列入 `INSUFFICIENT_BREAKPOINT_PREVIEW_DENOMINATOR` 之类，或者在 baseline JSON 加 `minDenominatorRuns`）。

---

## 4. §9 完成定义逐条对照

| # | 完成定义 | 状态 | 证据 / 备注 |
| --: | --- | :---: | --- |
| 1 | 六个可用职业开局职业技能数均为 3；release-facing blocking 分母只包含四个 BASE 职业 | ✅ | `professions/index.yaml` + `DataLoader.kt:382-383` + owner baseline `includedProfessions=[vanguard,arcanist,rogue,templar]` |
| 2 | 升级不再自动学习非 starter 职业技能 | ✅ | `unlockedTalentIds` 全删；`learnableTalentIds` 只返回候选，不写 rank；`syncUnlockedPlayerTalents` 已重写为 starter materialize 单一职责 |
| 3 | Talent UI 显示 locked、learnable、reserve、active 全状态 | ⚠️ | 4 状态枚举 + Sidebar 行 role 已落地；视觉形态非"水平三栏"，见 §2.3 |
| 4 | 学习新技能与升 rank 共用职业天赋点 | ✅ | `TalentAllocationDraft` rank 0→1 与 N→N+1 统一扣点（log.talent.learned vs rank_up） |
| 5 | Tier 2 / Tier 3 受等级、前置 rank、树投入三重约束 | ✅ | `TalentProgression.talentLockReasons` 同时收集 LEVEL / PREREQUISITE_RANK / TREE_INVESTMENT / CROSS_TREE_INVESTMENT |
| 6 | Tier 3 至少一个节点要求前置 rank ≥3 | ✅ | DataLoader `L422-426` fail-fast |
| 7 | berserker / spellblade 只做 starter/learnable/prerequisite 数据改造，不扩技能数量，进入 report-only coverage | ✅ | tier=ADVANCED + tags 不含 frozen；owner baseline `advancedReportOnlyProfessions=[berserker,spellblade]` |
| 8 | longRunLab 和 phase4Report 中出现新增 blocking 指标，并进入 canonical owner evidence | ✅ | `Phase4MetricCatalog.kt` + owner baseline JSON + aggregation manifest |
| 9 | verifyChanged 覆盖本 PR 的 code/data/report/client 影响面 | ⚠️未实跑 | 静态审查无法证明 verifyChanged 的"changed file → check 路由"是否覆盖本 PR 的 76 个 modified files，需要 `./gradlew verifyChanged` 实跑核对 |
| 10 | 没有新增图片计划文件 | ✅ | git status 中无新增 image plan/report；现有图标均复用 |
| 11 | 没有新增音频计划文件 | ✅ | git status 中无新增 audio plan/report；`audio.ui.talent_unlock` 复用 |

---

## 5. 修复优先级与 SLA 建议

| 优先级 | 偏差 | 建议处理人 | 预估工时 | 是否阻塞合并 |
| :---: | --- | --- | :---: | :---: |
| P1 | §2.1 PR 文档 §5.3 spellblade 起始树分布表 elemental_flux ↔ battle_spell 写反 | 文档 owner | 5 min | **建议合并前修复**（避免下游 PR 沿用错误） |
| P2 | §2.3 三列树视觉形态决议 | UI/UX + 设计 | 取决方案：方案 1 = 10 min；方案 2 = 0.5~1 day | 不阻塞，但需要在 release blocker review 前显式决议（保留 sidebar 形态 or 实装水平三栏） |
| P3 | §2.2 `learnableTalentIds` 签名与文档原型对齐 | 文档 owner | 5 min | 不阻塞 |
| P4 | §3.1 ADVANCED 4 节点树 Tier 切片在文档显式记录 | 文档 owner | 5 min | 不阻塞 |
| P4 | §3.2 `careerTalentPointsByLevel` 数据 review 表 | 设计 | 30 min | 不阻塞，但属 §5.4.6 字面要求 |
| P3 | §3.3 longRunLab 实跑 + vacuous denominator 行为复核 | release engineer | 1~2 h（含分析） | 建议合并前补 |

---

## 6. 玩家体验落地评估（设计总监视角）

从 §1 "玩家体验目标"的 3 个核心问题出发：

1. **"前 10 分钟必须面对学习新技能还是强化已有技能的选择"** —— 落地。`vanguard` 升到 2 级拿到 1 点天赋点时，UI 同时展示 `power_strike R1→R2`（升 rank）与 `war_cry Learn`（学新技能 = 进入 warcry 树承诺）两类候选，玩家无法回避一次性选择。
2. **"前 30 分钟必须形成职业树路线承诺"** —— 落地。Tier 2 节点（如 `intimidation`）需要"warcry 树同树投入 ≥2 + 另一棵树投入 ≥1"，迫使玩家在 lvl 3~5 区间显式承诺 1~2 棵主树；`multiTreeInvestmentAboveThresholdRate` 75% 的 owner gate 在 longRunLab 上自动巡检"主副树双投入"。
3. **"四槽满时学新技能的取舍压力"** —— 落地。`ACTIVE_TALENT_SLOT_CHOICE` modal 在 commit draft 前弹出，`R` 入 reserve / `1~4` 替换槽 / `Esc` 取消（不扣点）三态完整，且 `talentReserveSwapCount` supporting 指标用于事后回看玩家选择压力。

唯一需要警惕的地方：**§2.3 视觉布局形态** 直接影响"玩家第一眼能否同时看到三棵树"。若 sidebar 列式让玩家只看到一棵树就要按方向键切换，"在 lvl 2 看到 warcry 树作为新选项"的视觉冲击可能减弱，从而拉低 `learnedTalentChoiceEventRate` 90% 的 owner gate 命中率。**强烈建议在合并到 release 候选分支前用 longRunLab + 一轮人工白盒（按 PR 文档 §6.3 流程 + 玩家不知道触发条件的情况下）实测 `learnedTalentChoiceEventRate` 是否 ≥ 90%**。如果发现 < 90%，§2.3 的方案 2（实装水平三栏）就从"可选"升级为"必须"。

---

## 7. 结论

**可合并性**: ✅ 可以进入 release 候选合并流程，不需要打回重做。

**前置条件**:

1. 修复 §2.1 PR 文档 §5.3 spellblade 表反转（5 min 文档级修复）。
2. 完成 §3.3 longRunLab 实跑 + `breakpointChoiceEventRate` denominator 行为复核（1~2 h）。
3. 实跑 `./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab soloClearLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged` 全绿，至少留一份完整 build/reports trace 作为合并证据。

**release-blocker 决议项**:

- §2.3 三列树视觉形态：在 release-blocker review 前决定是否实装方案 2（水平三栏）。结合 §6 玩家体验风险评估，**推荐方案 2**。

**长期债**:

- §3.1 / §3.2 / §2.2 是文档级偏差，建议在 PR-02 启动前一次性收敛 PR-01 文档，避免下游 PR 复用错误描述。

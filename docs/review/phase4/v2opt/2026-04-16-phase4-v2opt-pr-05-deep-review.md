# Phase 4 V2OPT PR-05 深度审查报告

**审查日期**: 2026-04-16  
**审查角色**: 资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人  
**审查对象**: 分支 `codex/phase4-v2opt-pr-05-replay-hook-readability` 相对 `main` 的全部变更  
**对标文档**: `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md`

---

## 一、总体评价

本 PR 整体 **高度对齐** 需求文档。五个 Task 均已实现，核心合同未被违反，代码改动面控制在合理范围内（827 行新增 / 103 行删除，41 个文件），没有引入新的大 UI 系统、tutorial 系统或深层规则变更。`Implementation Adjustment（2026-04-15）`中记录的 floor-aware `mapgenProfileBindings` 调整是合理且必要的演化，已正确同步到 docs。

**总体符合度**: ~92%。存在少量偏差和可优化项，详见下文。

---

## 二、逐 Task 对账

### Task 1：greenwood pattern / layout 扩容

| 验收项 | 文档要求 | 实际实现 | 符合度 |
|--------|---------|---------|--------|
| 新增 pattern room | >= 1 个 | 新增 `pattern.greenwood.ambush_hideout` | ✅ 100% |
| 新增 entrance layout family | >= 1 个 | floor-aware binding 拆分 floor1/floor2 两套 profile，floor2 使用 `hidden.critical.adjacent` 替代 `hidden.branch` | ✅ 100% |
| 文档口径 `distinctEntranceLayoutCount > 1` | 从 1 → > 1 | floor1 `hidden.branch` + floor2 `hidden.critical.adjacent` = 2 个 distinct layout | ✅ 100% |
| 不新增新地形系统 | 禁止 | 未新增，只在 `greenwood_fringe` floor 配置中加了 `ICE: 0.12` 权重并降低 `WATER` | ✅ 100% |
| 不新增新 hidden contract | 禁止 | 未新增 — `greenwood_ambush_hideout` 复用已有 `PERCEPTION_CHECK` 规则 | ✅ 100% |
| 不用特殊 seed 白名单拉高指标 | Forbidden Shortcut | 未使用 | ✅ 100% |
| 新 secret zone 内容合规 | 复用已有资源 | `greenwood_ambush_hideout` 复用 `greenwood_hidden_cache` 的 visual/icon/audio | ✅ 100% |

**偏差项**:

1. **[低] terrain 权重微调未在 PR 文档中显式记录**：floor1/floor2 将 `WATER: 0.32` 拆成 `WATER: 0.24 + ICE: 0.12`，这是合理的 replay hook 增强手段，但严格来说改了 terrain tag weight 的分布。文档第 4 节冻结合同说"不新增新地形系统"—— 这里只是调了已有 terrain tag 的权重，**未违反合同**，但建议在 `Implementation Adjustment` 段补一句说明。

2. **[低] secret zone `rewardProfileId` 复用了 `loot.greenwood_hidden_cache.secret`**：这是合理的复用，但第二个 secret zone 共享第一个的 loot profile 意味着玩家在两个 secret zone 获得的奖励池完全相同，略微削弱 replay hook 的奖励差异感。这不违反合同（文档明确说"不再改 loot 分发合同"），但从设计层面值得标注。

### Task 2：terrain / mutation 前台摘要

| 验收项 | 文档要求 | 实际实现 | 符合度 |
|--------|---------|---------|--------|
| mutation 核心危险点不依赖 inspect | 必须 | `buildMutationHighlights()` 取可见范围内最近的 elite mutation 并生成 `ui.hud.frontstage.mutation_line` | ✅ 100% |
| terrain 危险/收益进入前台路径 | 必须 | `buildTerrainHighlights()` 取当前格 terrain override + terrain tag hints | ✅ 100% |
| 没有 client 侧第二真源 | Forbidden Shortcut | `FrontstagePresentationText.kt` 只消费 `RenderSnapshot`，无独立状态 | ✅ 100% |
| 没有 Boolean 参数切摘要模式 | Forbidden Shortcut | 无 | ✅ 100% |
| 没有 Helper/Manager 汇总层 | Forbidden Shortcut | 使用 `FrontstagePresentationText.kt` 纯函数映射 | ✅ 100% |
| 不做成信息墙 | 风险控制 | `FRONTSTAGE_PRESENTATION_LIMIT = 5`，mutation limit = 2，terrain limit = 2，action limit = 2 | ✅ 100% |

**偏差项**:

3. **[中] frontstage focus card 回退到 HUD focus 位的条件判断存在语义耦合**：`TileRenderModel.kt` 中 `frontstageFocus()` 在 `overlayState.mode != UiMode.MAP || focusActor != null` 时返回 null，即 frontstage 信息只在地图模式且无目标聚焦时才显示在 HUD focus 位。这个交互设计意味着**战斗中看到敌人时 frontstage 信息只在 sidebar 可见，而不在 HUD focus 位显示**。这是合理的优先级设计（目标信息 > frontstage 摘要），但文档 §7.2 说"当前目标敌人摘要"应包含 mutation 信息 —— 当前实现中 focus actor 面板是否已经包含 mutation line？如果 focus actor 面板不含 mutation 摘要，那么战斗中 mutation 信息仍然偏 sidebar / 二级通道。

4. **[低] i18n `ui.hud.frontstage.title` / `ui.sidebar.frontstage` 使用 "Frontstage" 作为面向玩家的 label**："Frontstage" 是内部术语，不是玩家友好的措辞。在 Roguelike 场景中，建议改为更直觉的名称如 "Awareness" / "态势感知" 或 "Situation" / "战况"。这不影响功能，但影响玩家体验和可读性 —— 正好是本 PR 的核心目标。

### Task 3：search / hidden 成败反馈增强

| 验收项 | 文档要求 | 实际实现 | 符合度 |
|--------|---------|---------|--------|
| success reveal 强反馈 | 必须 | `addFrontstageMessage("log.search.revealed", ...)` + `addFrontstageMessage("log.search.revealed_tag")` | ✅ 100% |
| fail-but-present 可区分 | 必须 | `addFrontstageMessage("log.search.failed_check", ...)` + `addFrontstageMessage("log.search.failed_tag")` | ✅ 100% |
| no-content 可区分 | 必须 | `addFrontstageMessage("log.search.no_target")` | ✅ 100% |
| secret zone 进入来源更明显 | 必须 | `addFrontstageMessage("log.hidden.secret_zone.enter", ...)` | ✅ 100% |
| 不在 client 维护 hidden 独立状态机 | Forbidden Shortcut | 未违反 | ✅ 100% |

**偏差项**:

5. **[中] `addMessage` → `addFrontstageMessage` 的机械替换范围偏大**：当前实现将 search/hidden 相关的几乎所有 `addMessage` 都替换为 `addFrontstageMessage`，包括 `log.search.already_resolved`、`log.hidden.reward.already_claimed`、`log.hidden.secret_zone.return` 等低信号事件。这些事件被记录为 `recentFrontstageActionCues` 后，会在前 3 turns 内持续出现在 frontstage 区域，与 search success/failure 的高价值信号竞争有限的展示位（`FRONTSTAGE_RECENT_ACTION_LIMIT = 2`）。**建议**: 仅保留 `log.search.revealed*`、`log.search.failed_*`、`log.search.no_target`、`log.hidden.secret_zone.enter` 使用 `addFrontstageMessage`，其余回退为 `addMessage`。

### Task 4：passive 短摘要

| 验收项 | 文档要求 | 实际实现 | 符合度 |
|--------|---------|---------|--------|
| 关键 passive trigger 能被立即感知 | 必须 | passive trigger（hp_regen / on_hit_status / on_kill_resource_restore / damage_bonus_*）全部升级为 `addFrontstageMessage` | ✅ 100% |
| 装备效果在奖励预览更明显 | 必须 | `RewardPresentationEntrySnapshot.detailText` + `firstMeaningfulRewardDetailToken()` 提取 passive 描述 | ✅ 100% |
| 不新开 passive badge / mini-system | Forbidden Shortcut | 未违反 | ✅ 100% |
| 不用 helper 层重新拼真源 | Forbidden Shortcut | `firstMeaningfulRewardDetailToken()` 只取第一条已有的 passive description | ✅ 100% |

**偏差项**:

6. **[低] passive trigger 的 frontstage 优先级与 search/hidden 相同，可能互相挤占**：hp_regen 每回合触发一次 → 每次都写入 `recentFrontstageActionCues` → 3 turns TTL 内持续占位。当 hp_regen 和 search 同时发生时，hp_regen 可能把 search 成功提示挤出 frontstage action 区域。与偏差项 #5 同根 —— **信号优先级分层缺失**。

### Task 5：golden / white-box 回归

| 验收项 | 文档要求 | 实际实现 | 符合度 |
|--------|---------|---------|--------|
| golden screenshot 更新 | 必须 | 大量 hash 更新，覆盖 en/zh、各 zone、各 mode | ✅ 100% |
| clientSmoke 通过 | 必须 | `ClientSmokeHarnessTest` 新增 frontstage 断言 | ✅ 100% |
| render test 覆盖 | 必须 | `AsciiRenderModelTest` + `TileRendererCanvasTest` 各新增 frontstage 专项测试 | ✅ 100% |
| canonical report 引用 | 必须 | docs 更新同步 | ✅ 100% |
| 不引用 legacy artifact | Forbidden Shortcut | 无 legacy 引用 | ✅ 100% |

**偏差项**:

7. **[低] golden screenshot sample-pack 的 seed 从 20260304 → 20260302 变更**：`GoldenScreenshotHarnessTest.kt` 中 `underground_river` 的 seed 从 `20260304L` 改为 `20260302L`，同时 sample-pack 的入口查找方式从通用 `propByType` 改为显式 binding id `search.underground_river.crystal_rift`。这提升了测试稳定性，但 seed 变更理由应在 commit message 或 implementation adjustment 中说明。

---

## 三、合同冻结项合规审查

| 冻结合同（§4） | 遵守情况 |
|---------------|---------|
| 不新增大 UI 系统 | ✅ 只新增了 `FrontstagePresentationText.kt`（~30 行纯函数） |
| 不重开新教程系统 | ✅ |
| 前台提示只服务已存在的正式合同 | ✅ terrain / mutation / passive / search / hidden |
| greenwood replay hook 不依赖新规则系统 | ✅ 只通过 content/schema wiring 实现 |

| §0 验证约束 | 遵守情况 |
|------------|---------|
| canonical output path | 未在本次 diff 中看到对 report path 的修改，保持原合规 |
| docs 同步更新 | ✅ `README.md` + `verified-next-pr-plan.md` 已更新 |
| client 断言不仅标题 | ✅ `ClientSmokeHarnessTest` 断言了 frontstage 内容行 |

---

## 四、架构质量评估

### 4.1 正面

1. **boundary 清晰**：`game` → `RenderSnapshot.frontstageReadability` → `client` 的数据流单向且干净。`client` 侧 `FrontstagePresentationText.kt` 只做 snapshot → presentation 的纯映射，不持有状态。

2. **floor-aware profile resolver 设计合理**：`ZoneSchemaV2.resolvedMapgenProfileId(floorIndex)` 带 fallback 到 `mapgenProfileId`，向后兼容；`SchemaFloorMapgenProfileBinding` 的 init validation 充分。

3. **`addFrontstageMessage` 的双写策略（log + frontstage cue）正确**：消息同时进入 log 和 frontstage cue，不丢失信息。

4. **测试覆盖充分**：涵盖 schema validation、render model、render canvas、golden hash、contract lint 等多层。

### 4.2 需要关注

1. **`recentFrontstageActionCues` 使用 `ArrayDeque` 无大小上限**：只有 TTL 过期清理（`pruneExpiredFrontstageActionCues`），没有硬性大小限制。虽然 `FRONTSTAGE_RECENT_ACTION_LIMIT = 2` 限制了渲染输出，但如果在 3 turns 内产生大量 frontstage message（例如连续多次 passive trigger），ArrayDeque 会无限增长直到过期。建议加 `MAX_FRONTSTAGE_ACTION_CUES` 上限。

2. **`FrontstagePresentationText.kt` 是新文件但不在 `git diff --stat` 中**：说明它在之前的 commit 中已加入。作为跨 Tile/Ascii 共享的映射层，位置合理。

---

## 五、玩法体验审查（设计总监视角）

### 5.1 正面体验变化

1. **greenwood 前两层有了差异化的隐藏内容结构**：floor1 `hidden.branch` vs floor2 `hidden.critical.adjacent`，玩家在第二层需要不同的探索策略才能找到 secret zone，replay hook 显著提升。

2. **terrain / mutation 前台化降低了"死于不知道"的挫败感**：不再需要 inspect 每个敌人才能知道它有 phase_runner mutation，降低了认知摩擦。

3. **search 成败区分清晰**：`no_target` / `failed_check` / `revealed` 三种路径完全可区分。

### 5.2 体验风险

1. **frontstage 信息噪音风险**：当前所有 passive trigger + search/hidden 事件 + terrain hint 都在同一个 frontstage 区域竞争 5 个展位。在密集战斗中（hp_regen 每回合触发 + 多个 terrain tile 交互），frontstage 可能频繁刷新，反而增加阅读负担。**建议引入简单的 priority/category 分层**：mutation/terrain > search/hidden 事件 > passive trigger。

2. **"Frontstage" 命名对玩家不友好**（同偏差项 #4）。

3. **第二个 secret zone 与第一个共享 loot profile + visual/icon/audio**：在玩家感知上，两个 secret zone 几乎不可区分（除了名字和位置）。这削弱了"每局不一样"的目标。虽然合同禁止改 loot，但至少可以给 `greenwood_ambush_hideout` 一个独立的 `descKey` 描述来区分体验（已做到）和独立的 `guaranteedContent` 奖励内容。当前 `guaranteedContent` 奖励是 `STEALTH` buff + 相同 loot profile，这在主题上（伏击藏所 → stealth buff）是合理的。

---

## 六、偏差总结与修复建议

| # | 严重度 | 偏差描述 | 修复建议 | 预估工作量 |
|---|--------|---------|---------|-----------|
| 1 | 低 | terrain 权重微调未显式记录 | 在 PR 文档 §6.4 Implementation Adjustment 追加一句 `terrainTagWeights 从 WATER:0.32 调整为 WATER:0.24+ICE:0.12` | 5 分钟 |
| 2 | 低 | 两个 greenwood secret zone 共享 loot profile | 可接受（合同禁止改 loot），后续 PR 再考虑差异化 | 不改 |
| 3 | 中 | 战斗中 mutation 信息可能不在 HUD focus 位 | 审查 focus actor panel 是否已含 mutation 摘要；若缺失则在 `focusLines` 中追加 mutation highlight | 30 分钟 |
| 4 | 低 | "Frontstage" i18n 不是玩家友好术语 | en: "Awareness" / zh: "态势感知" 或 en: "Situation" / zh: "战况" | 10 分钟 |
| 5 | 中 | `addFrontstageMessage` 替换范围偏大 | 将 `already_resolved`、`already_claimed`、`secret_zone.return` 等低信号事件回退为 `addMessage` | 20 分钟 |
| 6 | 低 | passive trigger 与 search/hidden 事件无优先级分层 | 可后续 PR 处理；当前 `take` 顺序（mutation → terrain → action）提供了基本优先级 | 不改（标注风险） |
| 7 | 低 | golden seed 变更未在 commit/docs 记录 | 在 implementation adjustment 或 commit message 补充说明 | 5 分钟 |
| 8 | 低 | `recentFrontstageActionCues` 无大小上限 | 加 `val MAX_FRONTSTAGE_CUES = 8` 并在 `recordFrontstageActionCue` 中 trim | 10 分钟 |

---

## 七、建议优先处理的修复

按影响排序：

1. **偏差 #5**（中）：`addFrontstageMessage` 范围收窄 —— 直接影响前台信噪比，是本 PR 核心目标"低摩擦可感知"的关键。
2. **偏差 #3**（中）：确认 focus actor panel 含 mutation 摘要 —— 若缺失则战斗中 mutation 信息仍偏二级通道。
3. **偏差 #4**（低）：改 i18n label —— 简单改动，直接提升玩家可理解性。
4. **偏差 #8**（低）：加 cue 上限 —— 防御性改动，简单安全。
5. **偏差 #1 + #7**（低）：文档补充 —— 5 分钟，维护审计链完整。

---

## 八、结论

本 PR 完成了文档定义的全部 5 个 Task，核心合同（不新增大 UI / 教程 / 规则系统、不改 loot/hidden 合同、只做前台抛光）均未违反。`Implementation Adjustment` 中 floor-aware profile binding 的引入是正确的架构演化，已同步文档。

**主要优点**：
- boundary 设计干净，`game → RenderSnapshot → client` 单向数据流
- replay hook 从"刚好过线"提升到有实质差异
- 测试覆盖完整，含 schema / render / golden / contract lint 多层

**主要风险**：
- frontstage 信号优先级分层缺失，密集战斗时可能噪音偏高
- `addFrontstageMessage` 替换粒度偏粗，低信号事件挤占高价值展位

**审查结论**: **可合并，建议先处理偏差 #5 和 #3 再合并**。其余偏差可在后续 PR 或本 PR 的 follow-up commit 中处理。

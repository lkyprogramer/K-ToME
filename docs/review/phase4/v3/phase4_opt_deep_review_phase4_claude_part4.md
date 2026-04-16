# K-ToME Phase 4 深度审阅 · Part 4

> **本文件定位**：Part 4 = 可延后到 Phase 5+ 的问题清单 + 最终结论（4/4）
> **上游**：Part 1 一致性矩阵、Part 2 玩法体验、Part 3 P0/P1/P2 优化建议
> **强约束**：不把 Phase 4 未结的结构洞推到 Phase 5；Part 4 的条目必须是"不补不会让 Phase 4 合同自相矛盾"的真正长期项

---

## 一、筛选规则

Phase 4 已规划的设计维度中，**下面三类条目属于"可延后"**：

1. **本身就是 Phase 5 / Phase 6 的长期机制**（设计正文中未要求 Phase 4 内完成，或只给出骨架）；
2. **属于"规模增长"型、不是"结构补齐"型**（条目数、音视频资源、语种等量的扩张，不涉及合同变更）；
3. **在 P0/P1 改造后，边际收益才凸显**（需要先跑一两个 run cycle 以观测真实数据）。

凡是满足以下任一条件的，**必须留在 Phase 4 内**（即进入 Part 3 的 P0/P1），**不落入 Part 4**：

- 已是设计正文中 Phase 4 必达合同项；
- 不补会让 V2OPT-PR-01..05 的体验在 60–90 分钟 run 里自相矛盾；
- v4 基线已点名、在当前仓库仍为"低"一致性。

---

## 二、可延后到 Phase 5+ 的问题清单

### D1 · Content Pack 第二条内容线 / Overlay 示范扩展

**现状**：`ContentPackManifest + OverlayEntry`（runtime `ADD+REPLACE` / fixture `APPEND+DENY`）合同完整，schemaVersion=3 稳定，但 Phase 4 结算前只有一条 canonical pack 被 "真正消费"。

**为什么可延后**：这是一项**生态型**能力。Phase 4 的职责是 "overlay 合同存在且可跑"；"生态用例（独立美术 pack / 平衡 mod / 社区 pack）" 本身需要 content 生产侧的长期投入，不在 Phase 4 范围。

**进入 Phase 5 的触发条件**：

- P0-1 / P0-2 / P0-3 落地后，canonical 基底的内容密度达到 "有值得 overlay 的厚度"；
- 规划至少 1 条第三方/测试 pack 作为示范，包含 ≥ 5 items + 1 zone + 1 boss variant；
- `DataLoader` 的 `DENY` / `REPLACE` 语义通过示范 pack 做一次端到端压测。

**风险（若无限延后）**：一年后真有外部 pack 进入时，schema 兼容成本会指数增长；建议最晚在 Phase 5 中期做一次 "示范 pack" 的内容投入。

---

### D2 · Zone 数量扩张（第 12–16 个 canonical zone）

**现状**：11 个 canonical zone + 6 个 secret zone 已足够支撑 Phase 4 玩法评估与 gate；部分 v2opt 讨论提到的候选扩展（如"新一级地表森林 / 深层海沟 / 虚空回廊"）未立项。

**为什么可延后**：Phase 4 的 loot / mapgen / hidden 合同在 11 区就已充分暴露结构问题；在 P0-2（Dynamic Loot Pool 全覆盖）与 P1-5（Secret zone 内部差异化）落地前，**先扩区只会把结构洞按新的 zone 数量复制一遍**。

**进入 Phase 5 的触发条件**：

- P0-2 / P1-5 落地后；
- `longRunLab` 的 decisionDensityPerMinute（P2-2）有至少 2 轮基线；
- 明确每个新 zone 的"身份 tag 三元组"（elements / combat / lore）。

---

### D3 · Race 扩展（orc / undead 以及新种族）

**现状**：`organicHiddenProbe` 固定 `4 profession × 3 released race（human/elf/dwarf）× 11 seed = 528 case`，`orc/undead` 不参与（v2opt README 冻结明文）。

**为什么可延后**：扩种族属于"内容 × 数值平衡"的量级扩展，不影响 Phase 4 的任何现有合同；当前限定 3 种族让 probe 的 528 case 可闭环。

**进入 Phase 5 的触发条件**：

- P0-1 / P0-3 / P1-1 落地，Phase 4 结算；
- orc / undead 的 profession × race 组合在 `lootBalanceLab` / `bossHarness` 中有专门 baseline；
- `organicHiddenProbe` 的 528 case 能扩展为 `4 × 5 × 11 = 880` 而不崩 gate runtime。

---

### D4 · "模因 / Meme" 长期元素（perma-upgrade / 继承系）

**现状**：Phase 4 设计正文未提及跨局继承机制；所有 run 相互独立。

**为什么可延后**：典型 Roguelite 跨局积累是 Phase 6 + 的设计题；在 Phase 4 核心 loop 尚未完全"好玩"之前引入 meta-progression 会掩盖真正的玩法问题（"数值堆出来的假享受"）。

**进入 Phase 5+ 的触发条件**：

- 单局 loop 在 90 分钟内已能让资深玩家愿意再开一局；
- 经过至少 2 轮 live playtest / owner metric 对齐。

---

### D5 · 音频 / 视觉资源新批次

**现状**：v2opt README §资源结论明确 "不再开新的图片/音频生成批次"。

**为什么可延后**：Phase 4 结算不依赖新资源；Part 3 所有 P0/P1/P2 条目都可在现有资源基线内完成。

**进入 Phase 5 的触发条件**：

- P0-1 的 uniquePassive 需要"触发时的视觉强调"（可先用现有 vfx key）；
- P0-3 的 boss phase2/phase3 需要"阶段切换的视听强调"（可先用 frontstage cue + 现有 vfx tint）；
- 当这两条在玩家反馈中真正成为 "需要新资源才能放大" 的瓶颈时再开批次。

---

### D6 · 控制器 / 触屏 / accessibility 输入层

**现状**：客户端渲染已稳定在 Ascii + Tile 两端；控制器 / 触屏 / 可访问性适配未在 Phase 4 范围。

**为什么可延后**：属于交付平台问题，不影响 Phase 4 的玩法评审。

---

### D7 · 多语言扩展（超出 en-US / zh-CN）

**现状**：`i18n/en-US.json` 与 `zh-CN.json` 已覆盖 Phase 4 全部玩家可见文案（但见 Part 3 P1-1 的 "Frontstage" 玩家化）。

**为什么可延后**：扩展语种是 L10n 工作流问题，不影响玩法；但 **P1-1 的术语玩家化必须先在两个语言内完成**（留在 Phase 4）。

---

### D8 · 大规模 balance tuning（针对高难度 / NG+ / 模式化挑战）

**现状**：Phase 4 目前只有标准难度线；NG+ / Heroic / Nightmare 模式未规划。

**为什么可延后**：balance tuning 属于 "结构稳定后再调参"；在 P0-1/2/3 落地前就调参会出现 "给骨架调肌肉" 的错位。

---

### D9 · Lore / 文本叙事深度

**现状**：i18n 中 hidden event / secret zone 的文本叙事偏骨架（"线索/警告/提示"级），未展开 lore。

**为什么可延后**：Phase 4 的核心命题是系统深度，不是叙事深度；在 P1-4（failed search 二次弧）与 P1-5（secret zone 陌生感）落地后，叙事才有载体。

---

## 三、最终结论

### 3.1 Phase 4 当前真实状态

- **骨架 & 合同层**：健康。`phase4Report` canonical + `verifyOwner` + V2OPT-PR-01..05 五条 PR 的 owner metric + exit gate 形成自证闭环。这是自 v4 基线以来最关键的系统级进步。
- **内容 & 身份层**：**有洞**。三处结构洞（Unique/Artifact 独占效果缺失、Dynamic Loot Pool 仅覆盖 4/11 区、Boss 无阶段脚本）必须在 Phase 4 内结清。
- **前台 & 可读性层**：有正式通道但无优先级。`addFrontstageMessage` 入口过宽、cue 无 cap、"Frontstage" 术语未玩家化、secret reward 视觉权重不足——这些在本阶段可一并收敛。
- **体验曲线**：前 60 分钟站得住，终盘 30 分钟 collapse。P0-3（Boss 阶段）+ P0-1（Unique 独特效果）+ P0-2（Finale dynamic pool）合力可以把终盘拉回 "有决定性战斗"。

### 3.2 结算前的硬要求

在宣告 Phase 4 结算之前，**必须完成 Part 3 中的全部 P0**（3 条）与至少 3 条 P1（建议 P1-1 / P1-3 / P1-5）：

| 要求 | 归属 | 状态 |
| --- | --- | --- |
| P0-1 UNIQUE / ARTIFACT 独占效果合同落地 | V2OPT-PR-06 | 必须 |
| P0-2 Dynamic Loot Pool 全区覆盖 | V2OPT-PR-06 | 必须 |
| P0-3 Boss phaseOverrides + 变体 ≥ 6 | V2OPT-PR-07 | 必须 |
| P1-1 Frontstage priority + cap + i18n 玩家化 | V2OPT-PR-06 | 必须 |
| P1-3 Finale anchor + extras | V2OPT-PR-06 | 必须 |
| P1-5 Secret zone 内部差异化 | V2OPT-PR-07 | 必须 |
| P1-2 AI_SHIFT/AURA terrain 联动 | V2OPT-PR-06 | 建议 |
| P1-4 Failed search 二次弧 consumable | V2OPT-PR-07 | 建议 |
| P2-1 / P2-2 / P2-3 | V2OPT-PR-08 或 Phase 5 初 | 可选 |

**Gate 层硬要求（沿用 README §统一验证约束）**：

1. PR-06 / PR-07 必须跑 `verifyOwner + phase4Report`；
2. Baseline 变动一次性收敛，不得残留 parity-only artifact；
3. 不得新增 visual/audio 资源族；
4. PR 合并前附一轮 `longRunLab`（90 分钟 run ×N）结果，至少观测 P0-3 的 `phaseTransitionObservedRatio` 与 P0-1 的 `uniquePassiveTriggeredRatio` 是否进入合理区间。

### 3.3 评分与判断

| 维度 | v4 基线（2026-04-08） | 当前（2026-04-16） | 结算目标（PR-06/07 后） |
| --- | --- | --- | --- |
| 流程与合同层 | 6.5 | 8.5 | 9.0 |
| 战斗手感 | 6.0 | 7.2 | 8.0 |
| 成长 / Build identity | 5.0 | 6.2 | 8.2 |
| Loot 与奖励 | 5.5 | 7.0 | 8.5 |
| 探索 / Hidden | 6.0 | 8.0 | 8.5 |
| UI / 前台 | 5.0 | 6.8 | 8.0 |
| 终盘 / 系统联动 | 4.5 | 6.0 | 8.0 |
| **综合** | **6.5** | **7.2** | **8.5** |

**评语**：

> 当前 Phase 4 已经超过"骨架能证明自己"的阶段，但尚未达到"一次 90 分钟 run 能自成闭环"的阶段。继续推 Phase 5 会把"终盘 collapse"与"Unique 无身份"带入下一轮，成为结构性债务；在 Phase 4 尾端再打 1–2 次结构性收敛 PR（PR-06 + PR-07），是整个项目性价比最高的一段投入。

### 3.4 我对下一步的直接建议

1. **两周内推进 V2OPT-PR-06**：P0-1 + P0-2 + P1-1 + P1-2 + P1-3。这是单一 PR 能承载的上限，且这 5 条是互相加强的（职业分发 → 动态池 → 独特效果 → 前台视觉权重 → finale 锚）。
2. **随后 1–2 周推进 V2OPT-PR-07**：P0-3 + P1-4 + P1-5。Boss 阶段脚本 + failed search 二次弧 + secret zone 内部差异化构成终盘的"第二段语言"。
3. **Phase 5 初期开 V2OPT-PR-08（可选）**：P2-1 / P2-2 / P2-3。作为 Phase 4 → Phase 5 的体验放大尾款，不紧迫。
4. **Phase 5 的正事开始**：Content Pack 示范（D1）+ Zone 扩展（D2）+ Race 扩展（D3）+ NG+（D8）。

### 3.5 一句话终评

**Phase 4 现在是一个"骨架硬、血肉中、终盘软"的阶段。只要 V2OPT-PR-06 / PR-07 把 P0 三条和 P1 核心三条做掉，Phase 4 就可以以一个自证、可抗玩的"完成态"结算；否则再多 V2OPT 小修都是在修"体验平原"，不是修"体验山峰"。**

---

## 附录 A · 与 v4 基线 (2026-04-08) 的差异总览

| 维度 | v4 基线问题 | 2026-04-16 状态 | 处置建议 |
| --- | --- | --- | --- |
| 精英 mutation 仅 6 条 | 已升 12 条 + preferredTerrainTags 初步绑定 | **已闭**（P1-2 做最后补齐） |
| Affix ~40 无条件被动 | 升至 ~88 + 6 种条件被动 | **基本闭** |
| Loot profile 静态 4 件池 | 4/11 区升级为 TAG_WEIGHTED | **未闭 → P0-2** |
| Hidden trigger 仅 2 种 | 14 event × 6 trigger | **已闭** |
| Mutation × terrain 无绑定 | ELEMENT_PACKAGE 已绑；AI_SHIFT/AURA 未绑 | **部分闭 → P1-2** |
| UNIQUE/ARTIFACT 无独占效果 | 仍仅 fixedAffixIds | **未闭 → P0-1** |
| Boss 变体少、无阶段脚本 | 仍 3 条、无 phaseOverrides | **未闭 → P0-3** |
| Secret zone entrance 单一 | entranceBindingId 三种 | **已闭**（内部 layout 仍需 P1-5） |
| Phase4 report 分散 | phase4Report canonical + verifyOwner | **已闭** |
| 前台无正式通道 | uiState.frontstageReadability + recentRewards[*].detailText | **已闭**（priority/cap/i18n 需 P1-1） |

---

## 附录 B · 本轮审阅引用的关键证据路径清单

- 设计文档：
  - `docs/phase4/roadmap.md`
  - `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
  - `docs/phase4/2026-03-13-phase4-verification-checklist.md`
- V2OPT：
  - `docs/review/phase4/v2opt/README.md`
  - `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md`
  - `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01..05-*.md`
  - `docs/review/phase4/v2opt/2026-04-16-phase4-v2opt-pr-05-deep-review.md`
- v4 基线：
  - `docs/review/phase4/phase4_opt_deep_review_claude_v4_part1..4.md`
- 代码：
  - `core/.../mapgen/{BspBackedMapgenPipeline,HybridTopologyPipeline,MapgenContracts}.kt`
  - `core/.../snapshot/RenderSnapshot.kt`
  - `client/.../render/{AsciiRenderModel,TileRenderModel,RewardPresentationText}.kt`
  - `client/.../test/...GoldenScreenshotHarnessTest / AsciiRenderModelTest / TileRendererCanvasTest`
  - `game/.../FoundationGameSession.kt、GameContent.kt、DataLoader.kt、schema/SchemaModels.kt`
  - `game/.../harness/{SmokeBot,ScenarioUtil}.kt`
  - `game/.../mapgen/SchemaZoneMapgenProfileResolver.kt`
- 数据：
  - `game/src/main/resources/data/elites/index.yaml`（12 mutation）
  - `game/src/main/resources/data/events/index.yaml`（14 event / 6 trigger）
  - `game/src/main/resources/data/secret-zones/index.yaml`（6 zone）
  - `game/src/main/resources/data/items/index.yaml`（~88 affix + 22 unique + 8 artifact）
  - `game/src/main/resources/data/loot/index.yaml`（22 profile，4 区 TAG_WEIGHTED）
  - `game/src/main/resources/data/boss-variants/index.yaml`（3 条 / 无 phaseOverrides）
- 验证产物：
  - `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`

——以上为四部分深度审阅的完整交付。

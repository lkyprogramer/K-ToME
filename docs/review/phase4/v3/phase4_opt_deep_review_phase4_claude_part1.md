# K-ToME Phase 4 深度审阅 · Part 1

> **审阅角色**：资深 Roguelike / ToME 方向 游戏开发总监 + 系统设计总监 + 玩法评审负责人
> **审阅时间**：2026-04-16
> **审阅对象**：K-ToME 仓库 Phase 4 当前状态（V2OPT-PR-01 … V2OPT-PR-05 已依次落地后）
> **审阅范围**：Phase 4 设计文档 + V2OPT follow-up 文档 + 当前实现 + YAML 数据 + 验证工具产物
> **本文件定位**：Part 1 = 执行摘要 + 审阅范围与依据 + 设计实现一致性矩阵（1/4）

---

## 一、执行摘要

### 1.1 核心结论

Phase 4 从 2026-03 的骨架状态（见 v4 基线 `docs/review/phase4/phase4_opt_deep_review_claude_v4_part1.md`）到现在的状态，经过 `V2OPT-PR-01 … V2OPT-PR-05` 五轮 follow-up，**系统合同层明显收敛，前台反馈首次形成正式通道，关键 owner metric 已进入 canonical gate**；但核心“好玩不好玩”的答案仍是 **“骨架接近完备、血肉明显偏薄、终盘几乎没有自己的语言”**。

用一句话定性：

> **当前 Phase 4 已经不再是“只跑得通”的阶段，而是“能被客观度量、能被前台感知、但内容密度和职业/身份成长感依然不够抗玩”的阶段。**

再具体拆三句：

1. **流程与合同层（Proc-gen / Loot / Hidden / 验证）已经可以自证**：12 种精英 mutation、14 个 hidden event 覆盖全部 6 种触发、6 个 secret zone、22 条 loot profile、`phase4Report` canonical aggregate、`whiteBoxMapgen/whiteBoxLoot/whiteBoxHiddenContent/organicHiddenProbe/longRunLab` 全部进入默认 gate；这部分和设计一致性 **已达“高”**。
2. **内容密度与身份感层（Unique/Artifact 个性、Boss 机制、职业化流派深度、Loot 动态池覆盖）仍是骨架**：UNIQUE/ARTIFACT 仍只是 `fixedAffixIds`，**没有一个独占触发/阈值被动**；Boss 变体仍为 3 个、**无 phaseOverrides / 阶段动作脚本**；dynamic loot pool 只覆盖 `greenwood_fringe / deep_iron_pit / underground_river / abyssal_temple` 四区域，其他 7 个区仍是 4 件固定清单。这部分与设计 **一致性偏低**。
3. **前台与可读性层（Frontstage readability / Recent reward source）刚刚形成正式通道**：`V2OPT-PR-05` 把 `RenderSnapshot.uiState.frontstageReadability`、`recentRewards[*].detailText` 与 `FoundationGameSession -> Tile/Ascii` 统一路径固定为 PR close-out 合同；但信号分层、优先级、文案口径仍处于“先能被渲染”的阶段，**尚未进入“能分清主次”的阶段**。

### 1.2 一句话定价

**Phase 4 结算分（按“当前状态下是否是一个让资深 Roguelike 玩家愿意继续玩的产品”评估）：7.2 / 10（v4 基线 6.5）。**

提升点主要来自：

- mutation 数量 `6 → 12`、trigger 类型 `2 → 6`、secret zone `4 → 6`；
- affix 池 `~40 → ~88`，被动类型出现 `OnHitStatusProc / OnKillResourceRestore / DamageVsStatus / DamageVsTag / DamageTypeBonus / HpRegenPerTurn` 等真正改变打法的分支；
- 验证体系从“一组平铺 task”升级为 `phase4Report` canonical + `verifyOwner` 合约入口；
- secret loot 身份层（strict pair ceiling 0.35/0.40/0.40 + `specialTemplateTagPreference / affixTagPreference / excludeIds`）与 frontstage readability 第一次正式在合同里存在。

扣分点：

- UNIQUE/ARTIFACT 没有真正“独特”的玩法效果，只是属性更高的 RARE；
- Boss 仍然是“3 个有变体的 HP 包”，终盘没有自己的语言；
- Loot 区域化只完成 4 / 11 个 canonical zone；
- 前台 readability 是“有管道”但还不是“有优先级”；
- Phase 4 规划的 `ContentPackManifest / OverlayEntry` 在“可度量的、放大玩家体验的维度上”还没跑出第二条内容线。

### 1.3 三条必须在 Phase 4 内结清的问题（不能推迟到 Phase 5）

这是 Part 3 的详细议题，这里先点明方向，便于读者判断本次审阅的立场：

1. **P0 · UNIQUE / ARTIFACT 独占效果合同缺失**
   当前 `items/index.yaml` 的 `unique_*` / `artifact_*` 只是更高 rarityScore + 固定 affix 组合，没有任何 `uniquePassive / onHit / onKill / thresholdTrigger / perTurn` 维度的合同字段。这是 Phase 4 "高稀有度 = 独立玩法身份" 设计的根基，必须在本阶段合同化，**否则 V2OPT-PR-02/PR-03 的“身份救火”在 Unique 层面是假成立**。
2. **P0 · Dynamic Loot Pool 覆盖不到一半**
   动态池（`TAG_WEIGHTED` + `itemTagFilter` + `excludeIds`）仅覆盖 4 区的 cadence/reward，其他 7 区仍是 4 件 `FIXED_LIST`。这让 `lootBalanceLab` 在覆盖度上是假达标——刷到 `molten_core / crystal_cavern / grey_gate_depths` 等区时玩家体验仍是“同一个 4 件包反复出”，直接冲掉 V2OPT-PR-02 的职业化掉落意义。
3. **P1 · Boss 变体数量与阶段机制**
   `boss-variants/index.yaml` 仍然只有 `molten_glass / grey_crown / abyssal_eclipse` 三条，且 **没有 phaseOverrides**。设计文档明确把 "多变体 + 阶段切换" 作为终盘 identity 锚；当前实际是 "3 个换皮的 Boss 都靠两招打完"，终盘体验是本次审阅的最大空窗。

此外 Part 3 还会展开：

- `battle_axe` 等 foundation 武器在 boss 池与 cadence 池的终盘收敛（v4 已点名、当前仍在）；
- frontstage signal 优先级与 "Frontstage" i18n 口径（来自 `2026-04-16-phase4-v2opt-pr-05-deep-review.md`）；
- `recentFrontstageActionCues` 无上限、`addFrontstageMessage` 入口过宽等 PR-05 结构问题。

### 1.4 本次审阅的总体判断

- **骨架 OK**：流程、合同、gate、report、owner metric、harness 已经形成闭环，"Phase 4 能自证自己跑起来了"。
- **血肉偏薄**：内容密度（尤其是 Unique/Artifact 身份、Boss 身份、中后期 Zone 身份）不足，导致一次 30–90 分钟 run 里前 30 分钟（V2OPT-PR-05 聚焦）已有记忆点，但 60 分钟后同质化加剧。
- **前台可读性刚起步**：frontstage/reward detail 已是正式合同，但"哪条信息先看、哪条是噪音"这一层还没收敛。
- **身份救火是半成品**：V2OPT-PR-02 的职业化分发、PR-03 的 secret identity 是正确方向，但其下游（Unique 被动、Artifact 独特效果、Boss 阶段脚本）没跟上，身份感会在 high-roll loot 层断裂。

**结论：现在不是"发 Phase 5"的时机，而是"在 Phase 4 尾端再打 1–2 次结构性收敛"的时机。**

---

## 二、审阅范围与依据

本节列出本次审阅调阅过的文档与代码/数据，后续一致性矩阵和问题单里的每条结论都需要能回溯到这里的证据路径。

### 2.1 设计文档（作为"设计口径"基准）

| 类别 | 文档 | 关键作用 |
| --- | --- | --- |
| 主线 | `docs/phase4/roadmap.md` | 4 checkpoint（P4-X / A / B / C）、5 work package、PR-01 … PR-09 映射 |
| 设计正文 | `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` | Proc-gen / Loot V2 / Hidden Content / Content Pack 设计正文（~1418 行，含数据模型 Kotlin 骨架与 YAML 示例） |
| 验证合同 | `docs/phase4/2026-03-13-phase4-verification-checklist.md` | mapgenSmoke ≥ 500 seeds / solvabilityHarness ≥ 1000 seeds / lootBalanceLab ±5% / hiddenContentHarness ≥ 30%+10% 等 gate 阈值 |

### 2.2 V2OPT follow-up 文档（作为"收口口径"基准）

| 文档 | 定位 |
| --- | --- |
| `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md` | 五条 follow-up PR 的权威重排 |
| `docs/review/phase4/v2opt/README.md` | PR 索引 + V2OPT-PR-03 / PR-05 实施冻结补充 + PR-06 后统一验证约束 + 资源复用结论 |
| `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md` | Experience gate + owner metric 合同 |
| `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-02-build-identity-and-profession-aware-loot.md` | 成长身份救火 + 职业化掉落 |
| `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md` | Secret reward identity + organic hidden 闭环 |
| `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-04-terrain-mutation-semantics-and-theme-hardening.md` | Terrain / mutation 语义收口 + 主题防稀释 |
| `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md` | 前 30 分钟 replay hook + frontstage readability |
| `docs/review/phase4/v2opt/2026-04-16-phase4-v2opt-pr-05-deep-review.md` | PR-05 近期深审清单（i18n 口径、`addFrontstageMessage` 边界、`recentFrontstageActionCues` 无上限等 8 项） |

### 2.3 v4 基线（作为"上一次深度审阅"参照）

- `docs/review/phase4/phase4_opt_deep_review_claude_v4_part1.md … part4.md`（2026-04-08）
- v4 基线记录的 P0/P1/P2 列表会在本次的一致性矩阵与 Part 3 中显式回溯其当前状态。

### 2.4 代码与数据证据（抽样，重点在合同与数据）

| 区域 | 证据路径 |
| --- | --- |
| 核心 mapgen / topology | `core/src/main/kotlin/com/ktome/core/mapgen/BspBackedMapgenPipeline.kt`、`HybridTopologyPipeline.kt`、`MapgenContracts.kt` |
| Snapshot / 前台合同 | `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`、`client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`、`TileRenderModel.kt`、`RewardPresentationText.kt` |
| 客户端断言（PR-05 close-out） | `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`、`client/src/test/kotlin/com/ktome/client/render/AsciiRenderModelTest.kt`、`TileRendererCanvasTest.kt` |
| 游戏会话 / 内容装配 | `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`、`GameContent.kt`、`data/DataLoader.kt`、`data/schema/SchemaModels.kt` |
| Harness / Smoke | `game/src/main/kotlin/com/ktome/game/harness/SmokeBot.kt`、`ScenarioUtil.kt`、`mapgen/SchemaZoneMapgenProfileResolver.kt` |
| Elite / Event / Zone 数据 | `game/src/main/resources/data/elites/index.yaml`（12 mutation）、`events/index.yaml`（14 event / 6 trigger）、`secret-zones/index.yaml`（6 secret zone）、`zones/index.yaml`、`mapgen/zones/index.yaml`、`mapgen/patterns/index.yaml` |
| Loot 数据 | `game/src/main/resources/data/loot/index.yaml`（22 profile，动态池仅覆盖 4 区） |
| Item 数据 | `game/src/main/resources/data/items/index.yaml`（~88 affix + 22 unique + 8 artifact + ~25 base + 4 material） |
| Boss 数据 | `game/src/main/resources/data/boss-variants/index.yaml`（仍为 3 条，**无 phaseOverrides**） |
| i18n | `game/src/main/resources/i18n/en-US.json`、`zh-CN.json` |
| 验证产物 | `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`（canonical aggregate） |
| 测试更新 | `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`、`core/src/test/kotlin/com/ktome/core/mapgen/MapgenContractTest.kt`、`client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt` |

### 2.5 审阅立场声明

1. 本文是 **玩法 / 系统设计评审**，不是代码风格评审；所有结论都必须指向玩家体验或系统合同结构，不评论命名 / 格式。
2. 本文承认 V2OPT 系列 PR 的冻结补充（见 `README.md` 第 44 行起）是当前合同口径，并据此给出结论；对"过去旧口径"不做回溯评判。
3. 本文不把问题推迟到 Phase 5；"必须在 Phase 4 内解决" 与 "可以进入 Phase 5" 两类会在 Part 3 / Part 4 显式分开。
4. 本文的"ROI / 风险 / 对齐点" 口径与 `v2opt` 文档保持一致，即以 `phase4Report canonical` + `verifyOwner` 为默认闭环入口。

---

## 三、Phase 4 设计-实现一致性矩阵

### 3.1 说明

- 一致性档位：`高` = 合同与实现一致，且已进入默认 gate；`中` = 合同落地但覆盖不足 / 文案或口径偏差；`低` = 仍是骨架或假落地。
- 每条至少一条 **证据路径**（文档 / 代码 / YAML / 报告）。
- 标记 `△` 的条目代表自 v4 基线起有实质变化，已在本次回溯确认。

### 3.2 矩阵

#### A. Proc-gen / Topology / Terrain

| 设计项 | 设计文档依据 | 实现证据 | 一致性 | 说明 |
| --- | --- | --- | --- | --- |
| MapgenPipeline + TopologyGraph + Room DAG | `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` | `core/.../mapgen/HybridTopologyPipeline.kt`、`BspBackedMapgenPipeline.kt`、`MapgenContracts.kt`；`whiteBoxMapgen` 已进 default gate | **高** | 骨架与合同一致；room / pattern / entrance 三级 contract 被 `mapgenSmoke` 与 `whiteBoxMapgen` 共同覆盖 |
| TerrainTag（WATER/OIL/ICE/LAVA）+ zone weight | `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`（`terrainTagWeights`）+ v2opt-PR-04 | `game/src/main/resources/data/mapgen/zones/index.yaml`、`mapgen/patterns/index.yaml`；terrain×mutation 交互批 `terrainInteractionBatch` | **中** △ | 语义已收口（WATER/OIL/ICE 的可燃 / 减速 / 点燃约束在 PR-04 中固定）；但**主题防稀释还偏被动**——`patterns/index.yaml` 中多区共享同一批 terrain 模板，后期区差异主要靠 weight，视觉辨识度不够（见 Part 2 §探索） |
| Floor-aware mapgen profile binding（`ZoneMapgenProfileResolver(zoneId, floorIndex)`） | v2opt-PR-05 冻结补充 | `game/src/main/kotlin/com/ktome/game/mapgen/SchemaZoneMapgenProfileResolver.kt`；`greenwood_fringe` 白盒抬至 `distinctPatternRoomCount=2 / distinctEntranceLayoutCount=2 / differenceCategoryCount=5` | **高** △ | 前 30 分钟 replay hook 合同成立；**但只有 `greenwood_fringe` 做了 second anchor family**，其他前期区尚未同样 harden |
| Solvability / Connectivity 合同 | `docs/phase4/2026-03-13-phase4-verification-checklist.md`（`solvabilityHarness ≥ 1000`） | `whiteBoxSolvability` 与 `solvabilityHarness` 已进 `verifyOwner`；`MapgenContractTest.kt` | **高** | 合同、gate、harness 齐备 |

#### B. Loot V2 / Item 生态

| 设计项 | 设计文档依据 | 实现证据 | 一致性 | 说明 |
| --- | --- | --- | --- | --- |
| LootBudget（iLvl / qLvl / rarityScore / affixBudget） | 设计正文 §Loot V2 | `data/items/index.yaml` 结构已稳定；`lootBalanceLab` 在 gate 中 | **高** | 预算模型与 affix cost 分档（TRIVIAL=1 … SIGNATURE=14）已生效 |
| Rarity 分布（NORMAL 720 / MAGIC 220 / RARE 50）+ SpecialTier（UNIQUE/ARTIFACT）+ PityTracker | 设计正文 | `lootBalanceLab`、`whiteBoxLoot`；`phase4Report` 的 `rarityScoreTotals` 等字段 | **高** | 分布合同存在且被 canonical gate 覆盖 |
| Affix 池厚度（设计预期"每条路径 ≥ 20 个有效 affix"） | 设计正文 §Affix Pool | `data/items/index.yaml` ~88 affix，含 `OnHitStatusProc / OnKillResourceRestore / DamageVsStatus / DamageVsTag / DamageTypeBonus / HpRegenPerTurn` 等；v4 P0-2 状态：已基本覆盖 | **中→高** △ | 从 v4 "~40 且多为纯数值" 升级到 "~88 + 条件型被动"；但仍然 **缺少 "资源/触发/敌方状态" 三元组的组合深度**，Magic/Rare 层的组合空间仍然有限（见 Part 2 §成长与 build） |
| Unique / Artifact 身份（设计预期"独占触发 / 阈值被动 / on-kill / on-hit"） | 设计正文 §SpecialTier | `data/items/index.yaml` 中 `unique_*` / `artifact_*` **仅有 `fixedAffixIds`**，grep `uniquePassive / onHitTrigger / onKillTrigger / thresholdTrigger` 为 0 命中 | **低** | **v4 P1-3 仍然未解**；这是 Part 3 P0 之一 |
| 职业化分发（V2OPT-PR-02） | v2opt-PR-02 | `data/loot/index.yaml` 中 4 区 `TAG_WEIGHTED + itemTagFilter + excludeIds`；`typeWeights / slotBias` 在多区显式声明；`whiteBoxLoot` 对 `typeBias / slotBias` 有显式断言 | **中** △ | 已有分发骨架；**但只覆盖 4 / 11 canonical zone**（greenwood_fringe / deep_iron_pit / underground_river / abyssal_temple），其余 7 区仍是 `FIXED_LIST` 4 件清单 |
| Secret reward 身份（V2OPT-PR-03） | v2opt-PR-03 + README 冻结补充 | `data/loot/index.yaml` 的 `*.secret` profile（`greenwood_hidden_cache / deep_iron_slag_cache / deep_iron_smuggler_stash / underground_river_crystal_rift / abyssal_temple_warded_archive`）全部带 `specialTemplateTagPreference + affixTagPreference + excludeIds`；strict pair ceiling 0.35 / 0.40 / 0.40 在 `whiteBoxLoot` 中硬断 | **高** △ | identity 与 local identity guardrail 都已成型；**但下游 "recent reward source = SECRET_ZONE 的前台呈现"** 仍然偏弱（见 Part 2 §UI） |

#### C. Hidden Content / Secret Zone

| 设计项 | 设计文档依据 | 实现证据 | 一致性 | 说明 |
| --- | --- | --- | --- | --- |
| 6 种 trigger（ENTER_ROOM / OPEN_CHEST / KILL_ELITE / INTERACT_TILE / QUEST_STEP / PERCEPTION_REVEAL） | 设计正文 §Hidden | `data/events/index.yaml` 14 event 覆盖全部 6 种；v4 基线仅 2 种 | **高** △ | v4 P1-1 已闭 |
| HiddenEntranceDef 多样性 | 设计正文 | `data/secret-zones/index.yaml` 6 zone，`entranceBindingId` 在 `hidden.branch / hidden.critical.adjacent / hidden.goal.adjacent` 间变化 | **高** △ | v4 Medium 已闭 |
| Organic discovery（`organicHiddenProbe`） | v2opt-PR-03 冻结：4 profession × 3 released race × 11 seed = 528 case；报告字段含 `firstHiddenDiscoveryTurnP50/P90` / `firstSecretZoneEntryTurnP50/P90` / `zoneDiscoveryDistribution` / `scriptedVerification=false` / `primerActionUsedCount=0` | `game/src/test/.../organicHiddenProbe` + `phase4Report` | **高** △ | organic 闭环在 canonical gate 内，v4 "只有脚本化发现" 的问题已闭 |
| Hidden entrance → secret zone → reward 前台反馈三条最小合同（primer 提示 / failed search 反馈 / secret reward 文案区分） | v2opt-PR-03 README 冻结 §5 | `RewardPresentationText.kt`、`FoundationGameSession.kt`、`i18n/en-US.json/zh-CN.json`；`recentRewards[*].detailText` 渲染路径 | **中** △ | 三条文案合同在；但在多线信号同时抵达时 "secret reward 区分" 容易被 cadence reward 覆盖（见 Part 2 §奖励） |

#### D. Elite / Boss / Encounter

| 设计项 | 设计文档依据 | 实现证据 | 一致性 | 说明 |
| --- | --- | --- | --- | --- |
| EliteMutationDef 5 家族（STAT_PACKAGE/ABILITY_GRANT/AURA/AI_SHIFT/ELEMENT_PACKAGE）、数量 ≥ 10 | 设计正文 §Elite | `data/elites/index.yaml` 12 mutation；`stonehide/ironhide/battle_drill/dread_aura/hunt_protocol/phase_runner/war_caller/corrosion_cloud/emberblood/frostbound/tidebound/void_mirror` | **高** △ | v4 P0-1 已闭（v4 为 6 条） |
| Mutation × Terrain 交互（`preferredTerrainTags`） | v2opt-PR-04 | `elites/index.yaml` 上 `frostbound(WATER/ICE)、tidebound(WATER)、emberblood(OIL)、corrosion_cloud(OIL)`；`terrainInteractionBatch` harness | **中** △ | 已落地 4 条关键绑定；**但 AI_SHIFT / AURA 家族与 terrain 的组合策略仍偏弱**（见 Part 2 §战斗） |
| BossVariantDef 多变体 + phaseOverrides（阶段脚本） | 设计正文 §Boss | `data/boss-variants/index.yaml` 仅 3 条；文件中 **无 phaseOverrides / phaseScripts 字段** | **低** | v4 P1-4 未闭；这是 Part 3 P1（数量维度）+ P0（阶段机制维度）的核心 |
| `actionWeightProfiles` / boss 行动分布 | 设计正文 | `data/boss-variants/index.yaml` 有 `actionWeightProfiles`（每 boss 仅 2–3 条 action） | **中** | 动作权重机制成立，但单 boss 可选动作数量偏少，阶段变化不足 |

#### E. 验证体系 / Report / Gate

| 设计项 | 证据 | 一致性 | 说明 |
| --- | --- | --- | --- |
| Canonical aggregate: `phase4Report` → `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | README 第 74 行起；仓库中产物已存在 | **高** △ | 默认 gate 已从平铺 task 升级为 canonical aggregate |
| `verifyOwner` / `verifyChanged` / `reportPhase4` / `phase4LegacyReport*` 语义分层 | README §统一验证约束 | **高** | daily = `verifyChanged`，PR = `verifyOwner + phase4Report`，parity = `reportPhase4`，legacy 仅 fallback |
| Owner metric（experience / build / secret identity / terrain / frontstage） | v2opt-PR-01..05 每条都自带 owner metric | **中→高** △ | 5 条 owner metric 均已存在；但 PR-05 的 frontstage metric 只看 "内容是否进入 render" 不看 "优先级是否合理"（见 Part 2 §UI） |

#### F. Content Pack / Overlay

| 设计项 | 设计文档依据 | 实现证据 | 一致性 | 说明 |
| --- | --- | --- | --- | --- |
| ContentPackManifest + OverlayEntry（runtime ADD+REPLACE / fixture APPEND+DENY） | 设计正文 §Content Pack | `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`、`data/schema/SchemaModels.kt`；多个 `index.yaml` 已走 overlay 路径 | **中** | 结构存在；**但 Phase 4 尾端没有真正"第二条 content pack 扩展 canonical 基底"的案例**，overlay 仍是单批 canonical + 试点 fixture，覆盖度有限 |
| Overlay 顺序 / DENY 语义 / schemaVersion 3 | `SchemaModels.kt` + 各 `index.yaml` schemaVersion=3 | **高** | 合同稳定、schema 已到 v3 |

#### G. 前台 / UI / Readability（V2OPT-PR-05）

| 设计项 | 证据 | 一致性 | 说明 |
| --- | --- | --- | --- |
| `RenderSnapshot.uiState.frontstageReadability` | `core/.../RenderSnapshot.kt`；`FoundationGameSessionTest.kt`；`AsciiRenderModelTest.kt` / `TileRendererCanvasTest.kt` 断言 frontstage content 进入正式 render / canvas | **中** △ | 合同存在、client close-out 断言存在；**但 "至少一条内容行 / reward detail 行" 是下限合同，不是上限合同**，多条噪音信号同时进入时无分层 |
| `RenderSnapshot.uiState.recentRewards[*].detailText` | `RewardPresentationText.kt`；i18n 中 `recentRewards.detail.*` 键；client golden 覆盖 | **中** △ | 合同落地；secret vs cadence vs route 文案区分已在，但视觉层次在 ascii / tile 都偏平 |
| `FoundationGameSession → Tile/Ascii render model` 单一路径 | `FoundationGameSession.kt`、`TileRenderModel.kt`、`AsciiRenderModel.kt` | **高** △ | 边界已合拢到单一路径 |
| i18n 口径（"Frontstage" 中文化 / "recent action cues" 文案） | PR-05 深审 `2026-04-16-phase4-v2opt-pr-05-deep-review.md` 第 (1)(2)(3) 条 | **低** | 仍保留"Frontstage"英文术语 / 前台 i18n 化不完整；这是 Part 3 P1 |
| `addFrontstageMessage` 入口范围 / `recentFrontstageActionCues` 无上限 | PR-05 深审 第 (4)(5) 条 | **低** | 入口过宽、无最大条数约束；这是 Part 3 P1 |

#### H. 与 v4 基线 P0/P1/P2 的回溯

| v4 编号 | v4 原问题 | 当前状态 | 证据 |
| --- | --- | --- | --- |
| v4 P0-1 | 精英 mutation 仅 6 条、无 terrain 交互 | **已闭** | `elites/index.yaml` 12 条 + `preferredTerrainTags` |
| v4 P0-2 | Affix 仅 ~40、缺条件型被动 | **基本闭**；仍需组合深度 | `items/index.yaml` ~88 + 6 种条件被动 |
| v4 P0-3 | Loot profile 静态 4 件池、无区域身份 | **部分闭**；仅 4 / 11 区升级 | `loot/index.yaml` 动态池覆盖度 |
| v4 P1-1 | Hidden trigger 仅 2 种 | **已闭** | `events/index.yaml` 6 种 trigger |
| v4 P1-2 | Mutation × terrain 无绑定 | **已闭** | `elites/index.yaml` preferredTerrainTags |
| v4 P1-3 | UNIQUE/ARTIFACT 无独占效果 | **未闭** | `items/index.yaml` 仅 fixedAffixIds |
| v4 P1-4 | Boss 变体少、无阶段脚本 | **未闭** | `boss-variants/index.yaml` 3 条、无 phaseOverrides |
| v4 Medium | secret zone entrance 单一 | **已闭** | `secret-zones/index.yaml` entranceBindingId 三种 |
| v4 Medium | phase4 report 分散 | **已闭** | `phase4Report` canonical aggregate |
| v4 Low | 前台无正式通道 | **基本闭** | `RenderSnapshot.uiState.frontstageReadability` |

### 3.3 Part 1 小结

- **骨架层（A / C / E / F / G）** 一致性整体为 **中高 → 高**，合同与 gate 已形成闭环。
- **内容/身份层（B-Unique/Artifact、B-Dynamic Pool 覆盖、D-Boss 阶段）** 仍为 **低**，是 Phase 4 结算前最需要补齐的三个结构洞。
- **前台层（G）** 已有通道但缺优先级与 i18n 口径收敛，属于"先能被渲染、还不能被读懂"。

Part 2 将基于此矩阵，按"核心循环 / 战斗 / 成长 / 奖励 / 探索 / UI / 系统联动"七维展开当前 Phase 4 的玩法体验总评；Part 3 给出必须在 Phase 4 内解决的关键问题与 P0/P1/P2 优化建议；Part 4 给出可延后到 Phase 5 的问题与最终结论。

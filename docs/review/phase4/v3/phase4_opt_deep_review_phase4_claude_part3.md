# K-ToME Phase 4 深度审阅 · Part 3

> **本文件定位**：Part 3 = 关键问题（必须在 Phase 4 内解决）+ 优化建议（P0/P1/P2）（3/4）
> **编写约束**：每条都必须给 "问题 / 作用域 / 目标 / 具体修改 / P0/P1/P2 / ROI / 风险 / 对齐点"
> **参考基线**：Part 1 的一致性矩阵、Part 2 的玩法体验、`docs/review/phase4/v2opt/*`、v4 基线的 P0/P1/P2 回溯

---

## 零、筛选原则

1. **必须解决 = 能在 30–90 分钟 run 里被玩家直接感知、且其结构不补会让 V2OPT 系列 PR 的已落地合同在体验层自相矛盾**。
2. **可延后 = 属于 Phase 5+ 的长期内容扩展、不补不会在当前 run 中"自相矛盾"**。可延后项归到 Part 4。
3. **ROI 口径**：`高`=每人日 >= 1 条玩家可感知的体验改善或结构收敛；`中`=每 2–3 人日 1 条；`低`=> 3 人日/条。
4. **对齐点口径**：写明应在哪个 `V2OPT-PR-XX` 合同基础上追加、或需不需要新开 `V2OPT-PR-06`。

---

## 一、P0 · 必须在 Phase 4 内解决（3 条）

### P0-1 · UNIQUE / ARTIFACT 独占效果合同缺失

**问题**：

当前 `game/src/main/resources/data/items/index.yaml` 中所有 `unique_*` / `artifact_*` 条目**仅有 `fixedAffixIds`**，grep `uniquePassive / onHitTrigger / onKillTrigger / thresholdTrigger / perTurn / phaseTrigger` 在整个 `game/src/main/resources/data/` 下为 **0 命中**。换言之：

- Unique 与 Artifact 只是"affix 组合更贵的 RARE"，不是 Roguelike/ARPG 品类玩家理解中的"gimmick item"；
- V2OPT-PR-03 的 secret reward identity (`specialTemplateTagPreference / affixTagPreference` + strict pair ceiling 0.35/0.40/0.40) 在终端不产生"独立玩法"；
- v4 P1-3 未闭，Phase 4 设计正文 §SpecialTier 合同层未兑现。

**作用域**：

- `game/src/main/resources/data/items/index.yaml`（unique / artifact 条目）
- `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt`（SchemaVersion 4 或 v3 兼容字段追加）
- `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`
- `game/src/test/kotlin/com/ktome/game/...`（loot / item harness）
- `core` 端：unique passive 的触发钩（on-hit / on-kill / turn / threshold），如已有 EffectSystem 则复用，否则扩展最小事件总线

**目标**：

让 "一件 Unique / Artifact 本身就是一条 build"。具体：

- 每个 **unique** 至少带 1 条 `uniquePassive`（`OnHit / OnKill / ThresholdHp / PerTurn / OnStatusApplied` 其中之一），且不能与任何普通 affix 的被动效果 1:1 等价；
- 每个 **artifact** 至少带 2 条 `uniquePassive`，或 1 条带"模式切换/阶段性能开关"的 `modalPassive`；
- Artifact 提供至少 1 条"玩家决策型资源"（充能、层数、回合冷却），而不是永久生效。

**具体修改**：

1. **Schema 扩展**（`SchemaModels.kt` + schemaVersion bump 到 4）：
   ```yaml
   uniquePassives:
     - kind: ON_HIT
       trigger: { onHit: true, chance: 0.25 }
       effect: { applyStatus: BURN, stacks: 2 }
     - kind: THRESHOLD_HP
       trigger: { belowHpRatio: 0.35 }
       effect: { grantResourceStacks: rage, amount: 3, duration: 3 }
   ```
   字段最小集：`kind / trigger / effect / internalCooldownTurns?`。
2. **合同约束**：
   - `unique_*` 必须 `uniquePassives.size >= 1`；`artifact_*` 必须 `uniquePassives.size >= 2 OR hasModalPassive`。
   - 约束在 `DataLoader` 校验层硬失败，不走 "可降级 fallback"。
3. **内容改造**：本轮把 22 unique + 8 artifact 全部补齐；允许复用已有 status / tag 池，不新增视觉/音频资源（遵循 v2opt 资源复用基线）。
4. **Owner metric**：`lootBalanceLab` / `whiteBoxLoot` 追加断言：
   - `distinctUniquePassiveKindCount >= 4`
   - `artifactModalPassiveCoverage >= 0.75`
   - Secret profile 产出的 unique/artifact 实际触发一次 `uniquePassive` 的 run 比例 >= 预设阈值（通过 `longRunLab` 统计）。
5. **前台**：`RewardPresentationText` 的 unique/artifact 行追加 "独特效果说明"（复用 passive 描述 i18n key，不新增术语族）。

**P0** · **ROI 高**（单 PR 内可完成结构落地 + 内容补齐；对每一次拾取到 unique/artifact 的玩家都是即时体验升级）

**风险**：

- Schema bump 需同步升级 `whiteBoxLoot` baseline（在 canonical aggregate 范围内，属可控）；
- 个别 passive 与现有 affix 同效，需要小心避免 "double-dip"——应在 `effect.kind` 层做归一化校验；
- 触发类 passive 对 longRunLab 的平衡曲线有扰动，必须在 gate 内观察至少一轮 `longRunLab` 输出。

**对齐点**：

- 建议作为 **V2OPT-PR-06** 的核心；或并入 "Phase 4 最终收敛 PR"。
- 合同层必须同时更新：`docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` §SpecialTier 字段示例；
  `docs/review/phase4/v2opt/README.md` 追加 "V2OPT-PR-06 实施冻结补充"。

---

### P0-2 · Dynamic Loot Pool 覆盖不足（7 / 11 canonical zone 仍 `FIXED_LIST`）

**问题**：

`game/src/main/resources/data/loot/index.yaml`（22 profile）中只有 4 个 canonical zone 的 cadence / reward 使用了 `TAG_WEIGHTED + itemTagFilter + excludeIds`：

- 已升级：`greenwood_fringe` / `deep_iron_pit` / `underground_river.cadence` / `abyssal_temple.cadence`
- 仍 FIXED_LIST 4 件：`shattered_outpost` / `bandit_camp` / `elven_ruins` / `molten_core` / `grey_gate_depths` / `crystal_cavern` / `underground_river.reward` / `abyssal_temple.reward` / `abyssal_heart.reward`

后果（Part 2 §V 已说明）：

- 中后期区 loot 峰值被 4 件清单压扁；
- V2OPT-PR-02 "职业化分发" 的下游在 7 区呈现为 "无差异化"；
- Part 2 §IX 的 60–90 分钟 collapse 主因之一。

**作用域**：

- `data/loot/index.yaml`（9 条 FIXED_LIST profile 需重构）
- `data/items/index.yaml`（确认 itemTag 覆盖，需要必要的 tag 补齐）
- `whiteBoxLoot`、`lootBalanceLab` 的 baseline 更新

**目标**：

**所有 canonical zone 的 cadence / reward profile 转为 `TAG_WEIGHTED`**，每区产出满足：

- `distinctItemIdCountOver100Roll >= 8`
- `distinctAffixCombinationCount >= 20`
- `typeBias / slotBias` 与本区职业化倾向一致（沿用 V2OPT-PR-02 合同）
- secret / cadence / reward 三份产出的 Jaccard 相似度继续满足 V2OPT-PR-03 的 strict ceiling（本条改造不得放松已成立的 0.35/0.40/0.40）

**具体修改**：

1. 把 9 条 FIXED_LIST profile 改为 `poolStrategy: TAG_WEIGHTED`，配齐：
   - `itemTagFilter`（至少 1 条 zone-specific tag）
   - `excludeIds`（涵盖 unique / artifact 与对侧 profile 重叠的锚点）
   - `typeWeights / slotBias`（按区域职业倾向）
2. 对 `abyssal_heart.reward` 作为 finale，**保留 anchor drop `abyssal_heartstone`**，但改为 `TAG_WEIGHTED + excludeIds + specialTemplateTagPreference`，允许额外 4–6 条本区 tag-匹配的 reward 进入池；本条必须和 P0-1 联动，让 finale 时至少有机会拾取到带 uniquePassive 的武器。
3. 为 `molten_core / grey_gate_depths / crystal_cavern / shattered_outpost / bandit_camp / elven_ruins` 在 `data/items/index.yaml` 补齐至少 6 条 zone-tag 的 base/magic 候选（不新增 unique/artifact，保持 RARE 锚）。
4. `lootBalanceLab` / `whiteBoxLoot` baseline 一次性更新：每 zone 的 distinctItemId、distinctAffixCombination、typeBias 断言。
5. **Owner metric**：`phase4Report` 增加 `dynamicLootCoverage = 升级区数 / canonicalZone 数`，并硬断 `== 1.0`。

**P0** · **ROI 高**（内容层 ~3 人日 + 验证层 ~1 人日；能直接消掉 Part 2 §IX 60–90 分钟 collapse 的第一个大洞）

**风险**：

- TAG_WEIGHTED 放开后，某些区会出现 "稀有度分布尾部偏胖"（因候选增多），需在 pity / rarityScore 上做一次平衡回归；
- itemTag 的分布不均可能让升级后的 molten_core vs crystal_cavern 身份模糊，需确保每区至少 2 条独占 tag。

**对齐点**：

- 建议并入 **V2OPT-PR-06**（和 P0-1 同 PR），或拆为独立 **V2OPT-PR-06b** 若 PR 体积过大；
- 必须复用 v2opt README §资源结论第 3 条的 hidden/secret 资源基线，不新增 image/audio。

---

### P0-3 · Boss `phaseOverrides` + 变体数量不足

**问题**：

`game/src/main/resources/data/boss-variants/index.yaml` 当前仅 3 条（`molten_glass / grey_crown / abyssal_eclipse`），每条 action weight profile 只有 2–3 条 action；**无 phaseOverrides / phaseScripts 字段**。设计正文把 "多变体 + 阶段切换" 作为终盘 identity 锚，Phase 4 当前在 Boss 层只实现了 "换皮"。

后果：

- Part 2 §VIII 终盘 collapse 的第二个大洞；
- 与 P0-1 / P0-2 联动后会放大："unique 有效果、loot 有身份、Boss 没有语言"；
- v4 P1-4 未闭。

**作用域**：

- `data/boss-variants/index.yaml`（新增 phaseOverrides + 扩展 action 权重）
- `game/src/main/kotlin/com/ktome/game/...`（boss 行动调度读取 phase）
- `core` 端 Encounter/Boss 阶段触发
- `bossHarness` baseline 更新

**目标**：

- **变体数量从 3 扩到 6**（每个 canonical boss base 至少 2 变体）；
- 每个 boss 至少 2 阶段（`phaseOverrides`）：阈值 0.6 HP / 0.25 HP；
- 每阶段至少新增 1 条独占 action 进入权重池；
- 与 `terrainTag` / `elite.mutation` 联动：至少 2 个 boss 在 phase2 变更为 terrain 利用型。

**具体修改**：

1. Schema 追加：
   ```yaml
   bossVariants:
     - id: boss.variant.X
       phaseOverrides:
         - trigger: { belowHpRatio: 0.60 }
           grantedMutationsAdd: [elite.dread_aura]
           actionWeightProfileOverride: boss.variant.weight.X.phase2
         - trigger: { belowHpRatio: 0.25 }
           grantedMutationsAdd: [elite.void_mirror]
           actionWeightProfileOverride: boss.variant.weight.X.phase3
           onPhaseEnter:
             - spawnTerrainPatch: OIL
             - broadcastFrontstageCue: boss.phase3.enter
   ```
2. 每个 boss 至少补 3 条新 action（不新增资源，复用现有 action id / 技能效果；允许借 mutation 的 ability grant）。
3. `bossHarness` 追加断言：
   - `phaseTransitionObservedRatio >= 0.95`
   - `distinctActionPerBossOver200Run >= 5`
   - `phase2OrLaterUniqueActionTriggered >= 1` per boss
4. 前台：每次进入新阶段，向 `RenderSnapshot.uiState.frontstageReadability` 推送一条 `phaseEnterCue`（与 P1-1 的优先级体系联动）。

**P0** · **ROI 中高**（3–5 人日；直接解决终盘 30 分钟的 collapse）

**风险**：

- Phase 切换可能引入"瞬间难度跳跃"，需要 `bossHarness` 做至少一轮 difficulty slope 校准；
- 与 mutation 动态挂接（`grantedMutationsAdd`）需验证 encounter 里是否允许运行时追加，若当前 encounter 不支持需要最小化扩展；
- 阶段触发与 terrain 生成交互时要小心"玩家正踩在被覆盖 tile 上"。

**对齐点**：

- 建议作为独立 **V2OPT-PR-07**（若 PR-06 容量允许，也可并入；但内容量独立拆出更稳）；
- 需要在 `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` §BossVariantDef 补 phaseOverrides 字段示例；
- 资源策略：完全复用现有 mutation / terrain / action。

---

## 二、P1 · 强烈建议在 Phase 4 内解决（5 条）

### P1-1 · Frontstage 信号优先级、cap 与 i18n 玩家化

**问题**（源自 `docs/review/phase4/v2opt/2026-04-16-phase4-v2opt-pr-05-deep-review.md` §1-5 + 本次审阅 Part 2 §VII）：

1. "Frontstage" 英文术语直出玩家面板；
2. `addFrontstageMessage` 入口过宽；
3. `recentFrontstageActionCues` 无上限；
4. secret vs cadence vs route reward 在 render 视觉权重无分层；
5. 无基于 game state 的静默 / 延后。

**作用域**：

- `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `client/src/main/kotlin/com/ktome/client/render/{AsciiRenderModel,TileRenderModel,RewardPresentationText}.kt`
- `i18n/{en-US,zh-CN}.json`

**目标**：

- Frontstage 信号三层：`critical / info / ambient`；
- `recentFrontstageActionCues` 最大 10 条滚动，按 `critical > info > ambient > fresh` 排序；
- 玩家面板术语替换为"当前关注 / 最近动作 / 提示"（或等价中文术语），英文端同步替换；
- `recentRewards[*]` 按 `source` 分组渲染，secret 来源的行具有独立视觉权重（ascii 前缀 + tile 边框 hint）；
- `addFrontstageMessage` 收口为单一入口，新增 priority 参数，其余内部调用收敛为 helper。

**具体修改**：

1. `RenderSnapshot.uiState.frontstageReadability` 追加字段 `priority: CRITICAL|INFO|AMBIENT`、`sourceKind`；
2. 在 Session 层新增 `FrontstageSignalGate`（不是新 Session，只是收口 helper），其它模块一律通过 gate 投递；
3. Client 端 Ascii/Tile 对三层分别使用不同前缀 / 视觉权重（ASCII：`!>` / `>` / ` `；Tile：边框 / 高亮 / 普通）；
4. 测试追加：`AsciiRenderModelTest` / `TileRendererCanvasTest` 断言至少一条 `CRITICAL` 行独占一行渲染，且非 `CRITICAL` 不得覆盖 `CRITICAL`。

**P1** · **ROI 高**（~2 人日；直接放大 V2OPT-PR-03/PR-05 的已有投入）

**风险**：

- 渲染优先级不当会遮蔽玩家正在看的信息；需 golden 断言覆盖 "critical 必显、非 critical 可降级"。
- i18n 术语更换需同步所有既有 test 期待。

**对齐点**：

- 直接追加到 **V2OPT-PR-05 follow-up** 或并入 **V2OPT-PR-06**；
- `docs/review/phase4/v2opt/README.md` 的 V2OPT-PR-05 冻结补充第 3 条（client close-out）需要扩展为 "critical 行必须命中"。

---

### P1-2 · AI_SHIFT / AURA 家族与 terrain 联动补齐

**问题**：

`preferredTerrainTags` 当前只命中 ELEMENT_PACKAGE 家族（emberblood / corrosion_cloud / frostbound / tidebound）。AI_SHIFT（phase_runner / battle_drill）与 AURA（dread_aura / war_caller）在无明显元素区（grey_gate_depths / abyssal_temple）退化为 stat 差异。

**作用域**：

- `game/src/main/resources/data/elites/index.yaml`
- `terrainInteractionBatch` baseline

**目标**：

- 每个家族 ≥ 1 条 terrain 联动；
- 允许"非元素型 terrain"参与：`DARKNESS / SANCTUM / ECHO` 等非物理 tag（若当前 terrain tag 表不含需先扩 1–2 条非物理 tag）；
- `terrainInteractionBatch` 追加断言 `familyTerrainBindingCoverage >= 0.8`。

**具体修改**：

1. 为 phase_runner 增加 `ECHO` preferred（闪现在回声场中更频繁）；
2. 为 battle_drill 增加 `STONE / CRACKED_FLOOR` preferred；
3. 为 dread_aura 增加 `DARKNESS / LOW_VIS` preferred；
4. 为 war_caller 增加 `SANCTUM / PAVED` preferred；
5. 若上述 tag 不存在，则在 `patterns/index.yaml` 补齐最小非物理 tag 集合（不新增图片/音频资源，使用现有 tile variants）。

**P1** · **ROI 中**（~1.5 人日；消掉 Part 2 §III 结论 2 的短板）

**风险**：

- 新 terrain tag 需要 `terrainInteractionBatch` 的 baseline 重建；
- AI_SHIFT 在 DARKNESS 下行为可能导致 solvability 边界；需在 `whiteBoxSolvability` 复核。

**对齐点**：

- 归入 **V2OPT-PR-06**（terrain 细化是 V2OPT-PR-04 合同的自然延伸）；
- 或作为独立 follow-up 附在 PR-06 中。

---

### P1-3 · 非升级区终盘 reward 清单加厚（与 P0-2 联动的"身份兜底"）

**问题**：

即使 P0-2 把 9 条 FIXED_LIST 改为 TAG_WEIGHTED，**`abyssal_heart.reward` 仍需保持 finale 锚 `abyssal_heartstone`**；若仅放开 pool，锚会被稀释。同时 `abyssal_temple.reward / underground_river.reward` 的 reward 仍偏 WEAPON 主力，副手 / 消耗品轴偏窄。

**作用域**：

- `data/loot/index.yaml`（上述 3 条 profile）

**目标**：

- Finale 的 anchor drop 保 100%，额外随机池提供 4–6 件带"unique passive 或 modal passive"的候选（与 P0-1 联动）；
- 对 `abyssal_temple.reward / underground_river.reward` 补 CONSUMABLE / OFF_HAND 候选，让终盘的 "buffer 型 build" 有补给来源。

**具体修改**：

1. `abyssal_heart.reward` 改为 `poolStrategy: ANCHOR_WITH_EXTRAS`（新增策略，或用 `FIXED_LIST + bonusPool`），anchor = `abyssal_heartstone`，extras 从带 `abyssal_temple` / `abyssal_heart` tag 的 unique/artifact 池抽取；
2. 调整 `typeWeights / slotBias` 让 OFF_HAND/CONSUMABLE 在终盘 reward 至少占 25%；
3. `lootBalanceLab` 追加断言：`anchorDropGuaranteed == true`；`finaleBonusDropProbability >= 0.5`。

**P1** · **ROI 中**（~1 人日）

**风险**：

- 改策略需要 `LootProfile` 代码支持；若尚未有 `ANCHOR_WITH_EXTRAS` 策略，最小扩展一条；
- 可能与 P0-1 / P0-2 在同 PR 中完成更经济。

**对齐点**：

- 与 P0-1 / P0-2 同 PR（**V2OPT-PR-06**）。

---

### P1-4 · Failed search 二次弧 —— Consumable 层补偿

**问题**：

Secret zone 与 hidden entrance 的 "失败反馈只有文案" 导致玩家被卡在 "先回头刷属性再回来"，这是品类上玩家最反感的卡点。当前 CONSUMABLE 池（healing_potion / mana_potion / stamina_draught / energy_tonic）不提供 "一次性过 check" 能力。

**作用域**：

- `data/items/index.yaml`（新增 2–3 条 consumable）
- Hidden check / search logic 读取 consumable 标签（`core` 或 `game` 中现有 HiddenTrigger 处理器）

**目标**：

- 至少 1 条 consumable 能 "一次性抵 X 类 check"（perception / lore / strength），**不能完全替代属性**，只覆盖 1 次尝试；
- failed search 文案追加 "你可以用 Y 物品再试一次"（i18n）；
- 不让 consumable 成为万能钥匙：限定 `usesPerRun <= 2` + `tierRestriction`。

**具体修改**：

1. 新增 `scroll_of_insight`（抵 perception check）、`warden_token`（抵 lore check）、`oilbreaker_charge`（抵 strength check）；
2. Hidden trigger 处理追加 `allowConsumableBypassTag` 字段，匹配 consumable 的 `bypassCheckKind` tag；
3. i18n failed search 文案扩展；
4. 追加 `whiteBoxHiddenContent` 断言：`failedSearchWithConsumableRetryPath > 0`。

**P1** · **ROI 中**（~2 人日）

**风险**：

- 消耗品不平衡会让 perception 检定无意义；`usesPerRun` 与获取率需要 lootBalanceLab 做一次回归。

**对齐点**：

- 与 V2OPT-PR-03 的 "failed search 反馈" 合同直接对接；
- 进入 **V2OPT-PR-06** 或独立 PR-07。

---

### P1-5 · Secret zone 内部差异化（layout / encounter）

**问题**：

6 条 secret zone 的"身份"主要体现在 loot profile 与 entranceBindingId；**内部 layout 仍复用母 zone pattern 池**（`patterns/index.yaml`）。玩家进入 secret zone 的 "视觉陌生度" 低于 "loot 分析陌生度"。

**作用域**：

- `data/mapgen/patterns/index.yaml`
- `data/secret-zones/index.yaml`（增加 `patternProfileOverride`）
- `mapgenSmoke` / `whiteBoxMapgen`

**目标**：

- 每个 secret zone 至少 1 条 "secret-only" pattern family（可复用母 zone 的 tile set，但 entrance/encounter 布局不同）；
- `whiteBoxMapgen` 追加断言 `secretZoneDistinctPatternFamilyCount >= 6`。

**具体修改**：

1. 为每条 secret zone 指定 `secretPatternProfile`；
2. Pattern 复用母 zone 的 tile + terrain，但 layout 偏 "狭窄走廊 + 独立房间"；
3. mapgen 层读取 `secretPatternProfile` 优先级 > 母 zone；
4. `mapgenSmoke` baseline 扩充 secret zone 测量。

**P1** · **ROI 中**（~2 人日）

**风险**：

- Mapgen 合同的 room/pattern 接线若不小心会破坏 solvability；需 `whiteBoxSolvability` 随 PR 同跑；
- Secret zone 的 "陌生感" 与母 zone 主题防稀释冲突，需保留必要的视觉连续性（tile set 不换）。

**对齐点**：

- 自然延伸 V2OPT-PR-04 的 "主题防稀释" 合同；
- 合并入 V2OPT-PR-06 可能略重，建议作为 **V2OPT-PR-07** 与 P0-3 同 PR。

---

## 三、P2 · 本阶段可做、做了更好（3 条）

### P2-1 · PityTracker 前台暗示

**问题**：Pity 合同存在但对玩家不可见，典型"系统正确 / 体验未放大"。

**具体修改**：在 `RenderSnapshot.uiState.frontstageReadability` 的 ambient 层增加 `pityHintLevel: NONE|APPROACHING|HIGH`（阈值与 PityTracker 同步），客户端 Ascii/Tile 在高 pity 时加一条不显眼提示。

**ROI 中 / 1 人日 / 风险低**。对齐点：P1-1 的 priority 体系落地后即可开 P2-1。

---

### P2-2 · `longRunLab` 加玩家体验曲线指标

**问题**：当前 longRunLab 只看 "不崩溃"，没有 "玩家 60 分钟后决策密度 / 资源消耗节奏" 指标。

**具体修改**：加入：

- `decisionDensityPerMinute`（玩家作出非琐碎选择的速率，基于 Session event log）
- `resourceTensionCurve`（每 5 分钟一个采样点）
- `bossPhaseReachRatio`（与 P0-3 联动）

追加到 `phase4Report` canonical。

**ROI 中 / 2 人日 / 风险低**。对齐点：V2OPT-PR-01 的 experience gate 的自然延伸。

---

### P2-3 · Profession / Race 的 "成长支线提示" 合同

**问题**：Profession / Race 深度未在内容层延伸；organic probe 只覆盖 4×3。

**具体修改**：

- 在现有 hidden event 池中追加 `preferredProfessionTags / preferredRaceTags` 字段（不新增 event，只调 weight）；
- 玩家匹配偏好时，hidden primer 文案追加 "这条线索对你的职业似乎有用"（不加新 event，只改文案挂载）。

**ROI 中低 / 1.5 人日 / 风险低**。对齐点：V2OPT-PR-02 / PR-03 的延伸，可入 V2OPT-PR-06 或单独。

---

## 四、建议的 PR 打包顺序

为避免 PR 过大或依赖倒置，建议：

- **V2OPT-PR-06（核心收敛 PR）** ：P0-1（Unique/Artifact 独占效果）+ P0-2（Dynamic Loot Pool 全覆盖）+ P1-3（Finale anchor+extras）+ P1-1（Frontstage priority/cap/i18n）+ P1-2（AI_SHIFT/AURA terrain）
- **V2OPT-PR-07（终盘与 Hidden 深化 PR）** ：P0-3（Boss phaseOverrides + 变体扩展）+ P1-4（Consumable 二次弧）+ P1-5（Secret zone 内部差异化）
- **V2OPT-PR-08（可选的体验放大 PR）** ：P2-1（Pity 暗示）+ P2-2（longRunLab 指标）+ P2-3（Profession/Race 提示）

这样 P0 全部在 PR-06 + PR-07 中结清，P2 可作为 Phase 4 结算前的"体验放大"尾款，也可顺延到 Phase 5。

**PR 闭环硬要求（沿用 v2opt README §统一验证约束）**：

1. PR-06 / PR-07 都必须跑 `verifyOwner + phase4Report`；
2. 任何 baseline 变动必须一次性收敛，不得遗留 parity 残留 artifact；
3. 不得新增 visual/audio 资源族（除非 strict ceiling 覆盖空白）；所有改动复用 v2opt README §资源结论基线。

Part 4 将给出可延后到 Phase 5 的问题清单与最终结论。

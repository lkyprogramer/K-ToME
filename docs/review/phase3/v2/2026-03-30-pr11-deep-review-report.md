# PR-11 深度审查报告：天赋经济重平衡与进阶职业深度

**审查日期**: 2026-03-30
**审查基准**: `docs/review/phase3/v2/2026-03-29-phase3-v2-pr-11-talent-economy-and-advanced-class-depth.md`
**分支**: `codex/p3-pr11-talent-economy-advanced-class-depth-impl`
**审查结论**: **通过，无阻断项**

---

## 0. 总体评估

| Lane | 目标 | 实现状态 | 偏差 |
|------|------|----------|------|
| W11a Rules | 天赋点经济改为每级 +1 | ✅ 完全达标 | 无 |
| W11b Content | Berserker/Spellblade 各 12 talent | ✅ 完全达标 | 无 |
| W11c Tools/QA | 回归测试与 smoke 覆盖 | ✅ 完全达标 | 无 |
| W11d Asset/Audio | 正式资源管线接入 | ✅ 完全达标 | 无 |

**`./gradlew check` 结果**: BUILD SUCCESSFUL (40 tasks, 全绿)

---

## 1. W11a — Talent Point Economy Contract

### 1.1 合同验证

| 检查项 | 文档要求 | 实际实现 | 结果 |
|--------|----------|----------|------|
| 每级天赋点 | 每次升级 +1 | `talentPointsGrantedForLevel()` 返回 `1` | ✅ |
| Lv1→Lv20 总天赋点 | 19 | 测试断言 `assertEquals(19, experience.unspentTalentPoints)` | ✅ |
| Stat point 保持不变 | 每级 +2，总 38 | `statPointsGrantedForLevel()` 返回 `2`，测试断言 `assertEquals(38, ...)` | ✅ |
| 移除旧 `level % 2 == 1` 门槛 | 不再使用 | 全库搜索零匹配 | ✅ |
| 单一真源 helper | 建议新增 `talentPointsGrantedForLevel()` | 已实现，含 `require(level in 2..LEVEL_CAP)` 守卫 | ✅ |
| Health/resource 升级回满 | 语义不变 | `shouldRestoreHealthToMax` / `shouldRestorePrimaryResourceToMax` 在 `levelsGained > 0` 时为 true | ✅ |
| 不引入额外 talent point 来源 | 冻结 | 无 Boss/Route/Achievement 额外点数来源 | ✅ |

### 1.2 代码评审

`ExperienceSystem.kt` 实现简洁：
- `talentPointsGrantedForLevel()` 和 `statPointsGrantedForLevel()` 对称设计，维护友好
- `applyReward()` 循环中每次升级都调用 helper，符合"单一真源"原则
- XP 曲线 `nextLevelExp(level) = level * 100 + 50` 未被修改，合规

### 1.3 测试覆盖

| 测试 | 断言内容 | 状态 |
|------|----------|------|
| `grantWithoutLevelUpKeepsProgress` | 无升级不给点 | ✅ 通过 |
| `levelUpAwardsPointsAndRestoresResources` | 单次升级给 1 talent + 2 stat | ✅ 通过 |
| `multiLevelGainAwardsTalentPointsOnEveryLevel` | 连续升级每级都给点 | ✅ 通过 |
| `level 1 to 20 grants nineteen talent points and thirty eight stat points` | 总量断言 | ✅ 通过 |
| `phase three talent budget can branch across multiple advanced class nodes` | 19 点预算跨树分配 | ✅ 通过 |

**评价**: 测试覆盖完整，包含了边界场景（不升级、单级、多级、全局）和新增的 budget 分配测试。

---

## 2. W11b — Advanced Class Depth Floor

### 2.1 Talent 数量验证

| 职业 | 树 | 文档目标 | 实际节点 | 结果 |
|------|-----|----------|----------|------|
| Berserker | wrath | 4 | `[blood_rush, savage_hew, riven_edge, pursuit_drive]` | ✅ |
| Berserker | ruin | 4 | `[reckless_slam, rupture_wave, fault_line, aftershock]` | ✅ |
| Berserker | bloodwar | 4 | `[kill_frenzy, last_stand, pain_fuel, slaughter_drive]` | ✅ |
| **Berserker 合计** | | **12** | **12** | ✅ |
| Spellblade | enchanted_blade | 4 | `[arcane_edge, spell_rend, runic_edge, sunder_sigil]` | ✅ |
| Spellblade | elemental_flux | 4 | `[flux_anchor, flux_burst, balance_point, flux_reversal]` | ✅ |
| Spellblade | battle_spell | 4 | `[mana_lunge, spell_parry, blink_strike, counter_seal]` | ✅ |
| **Spellblade 合计** | | **12** | **12** | ✅ |

### 2.2 新增方向对齐

文档建议的新增方向 vs 实际实现：

**Berserker:**

| 文档建议 | 实际 Talent | 机制 | 对齐 |
|----------|------------|------|------|
| wrath: 高仇恨斩杀 | `riven_edge` — 仇恨驱动深切，附 ARMOR_BREAK | damage + boss | ✅ |
| wrath: 追击连段 | `pursuit_drive` — 冲锋追击 + kill-chain 续战 | mobility + burst | ✅ |
| ruin: 裂线/破阵 | `fault_line` — 破阵附 WEAKEN | damage + area + control | ✅ |
| ruin: 地面震荡 | `aftershock` — 近场地震 AoE | damage + area | ✅ |
| bloodwar: 受伤反打 | `pain_fuel` — 伤口转化为回血 + 反击窗口 | panic + buff | ✅ |
| bloodwar: 击杀续战 | `slaughter_drive` — 击杀链回仇恨 | resource + buff | ✅ |

**Spellblade:**

| 文档建议 | 实际 Talent | 机制 | 对齐 |
|----------|------------|------|------|
| enchanted_blade: 元素附刃 | `runic_edge` — 符文附刃混合切割 | damage + hybrid | ✅ |
| enchanted_blade: 护甲/抗性撕裂 | `sunder_sigil` — 破除护甲 + ARMOR_BREAK | damage + boss | ✅ |
| elemental_flux: 平衡区间 payoff | `balance_point` — 平衡态 payoff 窗口 + MANA_SURGE_BUFF | buff + resource | ✅ |
| elemental_flux: 失衡救场 | `flux_reversal` — 失衡回拉 + 防御提升 | resource + defense | ✅ |
| battle_spell: 近战位移施法 | `blink_strike` — 闪现近战 + 施法 | mobility + hybrid | ✅ |
| battle_spell: 反制/招架 follow-up | `counter_seal` — 反制符印 + HOLY_SHIELD_BUFF | defense + counter | ✅ |

### 2.3 机制复用验证

文档冻结口径要求新 talent 复用现有机制族。验证结果：

| 检查项 | 结果 |
|--------|------|
| 无新 DamageType | ✅ 未引入 |
| 无新 ResourceType | ✅ 未引入 |
| 无新 PowerType | ✅ 未引入 |
| 无新 TalentOp 家族 | ✅ 全库无 TalentOp 枚举 |
| 复用机制类型 | direct damage / charge lane / self-buff / status apply | ✅ 全部复用现有 |

### 2.4 资源轴一致性

| 职业 | 文档要求的资源/玩法轴 | 实际实现 | 结果 |
|------|----------------------|----------|------|
| Berserker | HATE / burst / grit / kill-chain | 所有 talent 使用 HATE 资源，主题围绕 burst/追击/受伤反打/击杀链 | ✅ |
| Spellblade | MANA + EQUILIBRIUM / enchanted blade / flux control / melee spell | 所有 talent 使用 MANA + EQUILIBRIUM 双轴，带 equilibriumAffinity 标记 | ✅ |

### 2.5 冻结职业验证

| 职业 | tags | initialUnlockState | startingTalents | 树节点 | 结果 |
|------|------|--------------------|-----------------|--------|------|
| Shadowblade | `[profession, advanced, frozen]` | `LOCKED` | `[]` | 全部 `nodes: []` | ✅ 未解冻 |
| Warden | `[profession, advanced, frozen]` | `LOCKED` | `[]` | 全部 `nodes: []` | ✅ 未解冻 |

Git diff 中无 shadowblade/warden 任何改动。

### 2.6 i18n 覆盖

| 语言 | 新增 talent 覆盖 | 结果 |
|------|-----------------|------|
| en-US.json | 12/12 talent name + desc | ✅ |
| zh-CN.json | 12/12 talent name + desc | ✅ |

---

## 3. W11c — Balance Safety And Verification

### 3.1 测试矩阵

| 测试文件 | 覆盖内容 | 状态 |
|----------|----------|------|
| `ExperienceSystemTest` | talent point 总量增长 | ✅ 通过 |
| `TalentAllocationDraftTest` | 19 点预算跨树分配 | ✅ 通过 |
| `ProfessionSchemaTest` | Berserker/Spellblade 各 12 talent 断言 | ✅ 通过 |
| `SchemaV2LoaderTest` | 全部 12 新 talent 入 catalog | ✅ 通过 |
| `ClassFormalizationRuntimeContractTest` | pursuit_drive/blink_strike charge 验证 | ✅ 通过 |
| `BerserkerPlayableTest` | 6 个新 talent 全覆盖 (效果/伤害/状态) | ✅ 通过 |
| `SpellbladeEquilibriumTest` | 6 个新 talent 全覆盖 (效果/状态/位移) | ✅ 通过 |
| `TalentResolverTest` | fault_line/pain_fuel/flux_reversal 解析 | ✅ 通过 |
| `LoadoutPlannerTest` | PR-11 talent 装备槽位排序 | ✅ 通过 |
| `LongRunLabTest` | Berserker(seed 20260318) / Spellblade(seed 20260319) smoke | ✅ 通过 |
| `LongRunLabFullTest` | 12 场景矩阵 (4 base × 3 race) | ✅ 通过 |
| `SoloClearLabSupport` | Berserker/Spellblade solo clear 场景 (mob/elite/boss) | ✅ 通过 |

### 3.2 SmokeBot / LoadoutPlanner 集成

- `LoadoutPlanner.desiredLoadoutOrder()` 已包含全部 12 个新 talent 的优先级排序
- `SmokeBot.SPELLBLADE_TALENT_IDS` 已同步更新
- SoloClearLab 中 Berserker/Spellblade 各有装备模板和资源校验

### 3.3 `./gradlew check` 全通过

```
BUILD SUCCESSFUL in 1m 10s
40 actionable tasks: 12 executed, 28 up-to-date
```

包含：assetLint / styleLint / audioLint / manifestLint / soloClearLab / jacocoTestCoverageVerification

---

## 4. W11d — Talent Asset And Audio Pack

### 4.1 资源注册

| 注册点 | Berserker | Spellblade | 结果 |
|--------|-----------|------------|------|
| `visuals/index.yaml` — `talent.<prof>.<id>.visual` | 6/6 | 6/6 | ✅ |
| `visuals/index.yaml` — `talent.<prof>.<id>.icon` | 6/6 | 6/6 | ✅ |
| `visuals/index.yaml` — `icon.skill.<prof>.<id>` | 6/6 | 6/6 | ✅ |
| `audio/index.yaml` — `audio.talent.<id>` | 6/6 | 6/6 | ✅ |
| `visual-manifest.json` 映射 | 18/18 条目 | 18/18 条目 | ✅ |
| `audio-manifest.json` 映射 | 6/6 条目 | 6/6 条目 | ✅ |

### 4.2 物理资源文件

| 资源类型 | 路径模式 | 数量 | 结果 |
|----------|----------|------|------|
| Icon | `phase3/p3-followup/icon_skill_berserker_*.png` | 6 | ✅ |
| Icon | `phase3/p3-followup/icon_skill_spellblade_*.png` | 6 | ✅ |
| Visual | `phase3/p3-followup/talent_berserker_*_visual.png` | 6 | ✅ |
| Visual | `phase3/p3-followup/talent_spellblade_*_visual.png` | 6 | ✅ |
| Audio | `audio/talent/<talent_id>.ogg` | 12 | ✅ |

共 **36** 个资源文件，全部到位。

### 4.3 Plan 与 Lint 接入

| Lint 任务 | PR-11 Plan 已接入 | 结果 |
|-----------|-------------------|------|
| `assetLint` | `phase3-v2-pr11-gemini-plan.yaml` | ✅ |
| `styleLint` | `phase3-v2-pr11-gemini-plan.yaml` | ✅ |
| `audioLint` | `phase3-v2-pr11-audio-plan.yaml` | ✅ |
| `manifestLint` | `phase3-v2-pr11-gemini-plan.yaml` + `phase3-v2-pr11-visual-alias-plan.yaml` | ✅ |

Lint 输出：
- `manifest-lint OK: entries=387, requiredMissingVisual=0`
- `style-lint OK: assets=321`

### 4.4 文件名合规

文档冻结的命名模式 vs 实际：

```
✅ phase3/p3-followup/talent_berserker_<talent_id>_visual.png
✅ phase3/p3-followup/icon_skill_berserker_<talent_id>.png
✅ phase3/p3-followup/talent_spellblade_<talent_id>_visual.png
✅ phase3/p3-followup/icon_skill_spellblade_<talent_id>.png
✅ audio/talent/<talent_id>.ogg
```

无任何 key 落到 `debug/missing_visual.png` 或 `audio/fallback/silence.ogg`。

---

## 5. 冻结口径逐条验证

| # | 冻结口径 | 验证结果 |
|---|----------|----------|
| 1 | 等级上限 20，不改 XP 曲线 | ✅ `LEVEL_CAP = 20`，`nextLevelExp` 未修改 |
| 2 | 天赋点每级 +1，不引入额外来源 | ✅ 唯一来源为 `talentPointsGrantedForLevel()` |
| 3 | stat point 每级 +2 | ✅ `STAT_POINTS_PER_LEVEL = 2` |
| 4 | talent rank cost 口径不重写 | ✅ 未修改 allocation/cost 逻辑 |
| 5 | Berserker/Spellblade 目标 12 talent | ✅ 各 12 |
| 6 | Shadowblade/Warden 维持 frozen | ✅ 未解冻 |
| 7 | 不引入新 DamageType/ResourceType/PowerType/TalentOp | ✅ 全部复用现有 |

---

## 6. 出口门禁逐条验证

| # | 门禁条件 | 验证结果 |
|---|----------|----------|
| 1 | ExperienceSystem talent point 口径已改为每级 +1 | ✅ |
| 2 | Berserker/Spellblade 各达到 12 个正式 talent | ✅ |
| 3 | 新增 talent 正式资源已接入 visual/audio index + manifest，通过所有 lint | ✅ |
| 4 | soloClearLab/longRunLab 没有明显崩塌 | ✅ |
| 5 | `./gradlew check` 绿色 | ✅ BUILD SUCCESSFUL |

---

## 7. 观察与建议（非阻断）

### 7.1 所有新增 talent 均为 ACTIVE 类型

12 个新增 talent 全部为 ACTIVE，无 PASSIVE。这在当前设计方向范围内完全合理（文档建议的机制族均为主动型），但后续 Phase 4 可以考虑为进阶职业补充被动天赋以增加 build 分化层次。

### 7.2 Berserker/Spellblade 使用借用 visual/icon

profession 配置中：
- `berserker` 的 `visualKey: actor.vanguard` / `iconKey: icon.profession.vanguard`
- `spellblade` 的 `visualKey: actor.arcanist` / `iconKey: icon.profession.arcanist`

这是已知的占位，不属于本 PR 范围，但建议在后续 PR 补充独立的进阶职业角色立绘。

### 7.3 LongRunLabFullTest 矩阵未直接包含进阶职业

`LongRunLabFullTest` 的 12 场景矩阵覆盖 4 个基础职业 × 3 种族，进阶职业通过 `LongRunLabTest` 中的独立场景覆盖。coverage 足够但矩阵维度不完全对称。建议后续扩展矩阵加入 berserker/spellblade 场景。

---

## 8. 审查结论

**PR-11 实现与设计文档 100% 对齐，无任何偏差或缺失项。**

四条 Lane 全部达标：
- 天赋点经济合同已收口为每级 +1（W11a）
- Berserker/Spellblade 各补到 12 talent，方向与机制复用符合设计意图（W11b）
- 测试覆盖完整，所有 smoke/regression 通过（W11c）
- 资源管线端到端完备，lint 全绿（W11d）

冻结口径 7 条全部满足，出口门禁 5 条全部通过。

**建议**: 合并。

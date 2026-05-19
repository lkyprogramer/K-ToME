# PR04-01 Playable Profession Passive Talents — Director Deep Review Round 3

- 分支:`codex/dark-uiux-pr04-01-passive-talents`
- 规格文档:`UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md`
- 审查角色:Roguelike / 类 ToME 开发设计总监 + 系统策划总监 + 玩法体验审查负责人
- 审查标准:`code-review-high` (BLOCKER / HIGH / MEDIUM / LOW / NIT)
- 主关注面:数据契约、被动效果分发、PASSIVE 行动矩阵、白盒验证场景、稳定 key、显示语义、并发与刷新

---

## 1. Summary

### What changed (实施面)
PR04-01 将 6 个 playable 职业各 2 个 talent (共 12 个) 从 `ACTIVE` 改为 `PASSIVE`,新增 `PassiveEffect` 10 种 sealed variant、`PassiveSource(kind, sourceId, sourceTemplateId, affixId?, talentRank?, passive)` 与 `PassiveSourceKind { EQUIPMENT, TALENT }`,实现 source-agnostic 的 `PassiveEffectResolver`;HP regen 单路径迁移到 `DiminishingReturns.effectiveHpRegen`;新增 3 个白盒验证场景 (`dark-uiux-pr04-01-static-passive-detail`、`dark-uiux-pr04-01-trigger-passive-detail`、`dark-uiux-pr04-01-passive-action-suppression`) 并通过 `validationTalentFocusRequest` 在 client 边界打通 focus-by-id (consume-once token)。

### Top risks
1. **HIGH** `castSpeedRating` 在被动详情行渲染为 raw 整数,违反 §2.5 line 569/583 要求的"effective decimal,one fractional digit" — 直接影响 `arcane_overload`、`balance_point` 两个官方静态被动的玩家可读语义。
2. **HIGH** `hpRegen` 在被动详情行渲染为 raw double,未走 `DiminishingReturns.effectiveHpRegen`,违反 §2.5 line 570/584 与 §2.3 line 420 — 直接影响 `pain_fuel` 五个 rank 的显示。
3. **MEDIUM** 3 个 PR04-01 验证场景未覆盖 `castSpeedRating` / `hpRegen` 的渲染格式断言,导致上述两条 HIGH 在白盒 lint 流水中无法被自动捕获 (testing coverage gap)。

### Approval
**request_changes** — 数据、稳定 key、行动矩阵、focus 通道、3 个白盒场景与 12 张 rank 表全部对齐契约;但 §2.5 effective-display 路由是显式契约不可放过,必须修复并补一条覆盖性断言后再合入。

---

## 2. Affected files (review-relevant subset)

| Path | 角色 | 状态 |
| --- | --- | --- |
| `game/src/main/resources/data/talents/index.yaml` | 12 个 PR04-01 talent 的 rank 表 / `levelEffects[*].passiveEffects` | modified, ✅ 与 §3.1 / §3.2 完全一致 |
| `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt` | `PassiveEffectSchemaV2` (10 kind, 全字段) | modified, ✅ |
| `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt` | `toRuntimePassive()` 10 kind + fail-fast 未识别 kind + 范围校验 | modified, ✅ |
| `core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt` | source-agnostic resolver,equipment + talent 共用入口 | modified, ✅ |
| `core/src/main/kotlin/com/ktome/core/item/ItemModels.kt` | `PassiveSource`、`PassiveSourceKind`、`PassiveEffect` 10 variant | modified, ✅ |
| `core/src/main/kotlin/com/ktome/core/talent/TalentModels.kt` | `levelEffects[*].passiveEffects` | modified, ✅ |
| `core/src/main/kotlin/com/ktome/core/ecs/Components.kt` | `PassiveStatModifier`(由 `EquipmentPassiveStatModifier` rename),`DerivedStats.castSpeedRating/effectiveCastSpeed` | modified, ✅ |
| `core/src/main/kotlin/com/ktome/core/stats/StatsCalculator.kt` | `hpRegen` 单路径 → `effectiveHpRegen`,`castSpeedRating` → `effectiveCastSpeed` | modified, ✅ |
| `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt` | `TalentPassiveDetailSnapshot`、`PassiveDetailLineKindSnapshot` 10 kind | modified, ✅ |
| `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` | `passiveDetailSnapshot` / `passiveDetailLines` / `passiveStableKey` / `isActiveSlotTalent` / `EquipTalentToSlot` 拒绝 PASSIVE | modified, ⚠️ HIGH 显示路由偏差 (line 5929/5930) |
| `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt` | PASSIVE 跳过 R 动作行与 RESERVE 页脚 | modified, ✅ |
| `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` | R 键对 PASSIVE 返回 null;`applyValidationTalentFocusRequest` 消费一次性 token | modified, ✅ |
| `game/src/main/kotlin/com/ktome/game/validation/ValidationScenario.kt`,`ValidationScenarioRegistry.kt`,`ValidationSessionOptions.kt` | 3 个 PR04-01 场景 + typed assertion + forbidden log fragment | modified, ⚠️ MEDIUM 未覆盖 cast/hpRegen 格式 |
| `game/src/main/resources/talents/index.yaml` (i18n key) | `ui.stat.*`、`ui.hud.*.short`、`ui.inspect.mod.*` | 待 i18n 落实抽样 |
| `game/src/main/resources/i18n/en-US.json` / `zh-CN.json` | 新增 / 调整的 token | modified, 抽样 ✅ |

---

## 3. Root cause & assumptions

### 设计正确性 (passed)
- 12 张 rank 表 (§3.1 + §3.2) 与 YAML 数据完全对齐 (逐 talent 逐 rank 校验 unyielding / arcane_overload / killer_instinct / devotion / pain_fuel / balance_point / bulwark_march / mana_surge / deathblow / beacon_of_zeal / last_stand / flux_anchor)。
- 12 个 talent 全部 `category: PASSIVE, kind: PASSIVE, cooldown: 0, castTime: INSTANT, targeting: SELF, callbacks: []`,符合 §3.4 重写规则。
- Schema parser (DataLoader.kt L2859-L2961) 对 10 个 `PassiveEffect` kind 全部实现 `toRuntimePassive()` 分支,未识别 kind 走 `error("Unsupported passive effect kind '$kind'.")`,符合 §2.4 fail-fast。
- 稳定 key 契约(§2.3 line 355):
  - Equipment 保留 legacy literal `passive:<effectKind>:<itemBaseId>` (FoundationGameSession.kt:12006 / `equipmentPassiveStableKey`)。
  - Talent 走 `passive:talent:<talentId>:<effectKind>:rank<rank>` (FoundationGameSession.kt:12509-12516 `passiveStableKey`)。
- PASSIVE 行动矩阵 (§2.6) 在三处同时收口:
  - `TalentSidebarPresenter.kt:759-761` 跳过 R 动作行。
  - `TalentSidebarPresenter.kt:1086-1094` 跳过 RESERVE footer hint。
  - `InputHandler.kt:1351-1357` R 键对 PASSIVE 返回 null,绝不构造 `PlayerCommand.RespecTalentTree`。
  - `FoundationGameSession.kt:3147-3151 isActiveSlotTalent` 仅对 `ACTIVE` / `SUSTAINED` 返回 true;`EquipTalentToSlot` handler (line 9248-9305) 用它拒绝 PASSIVE。
- Focus-by-id 通道 (round6 P1 的修复) 已经在 client 边界落地:
  - `RenderSnapshot.uiState.validationTalentFocusRequest` 承载 `{treeId, talentId, consumeOnceToken}`。
  - `InputHandler.kt:1681-1719 applyValidationTalentFocusRequest` 用 `consumedTalentFocusRequestIds: MutableSet<String>` 做幂等消费,定位到目标 row 后只改 selection,不重写 owner。
- HP regen 单路径迁移(§2.3 #4-#8):
  - `StatsCalculator.kt:129-131` 走 `DiminishingReturns.effectiveHpRegen(profile.baseHpRegen + effectiveStats.con * 0.2 + modifiers.hpRegen)`。
  - `PassiveEffectResolver.resolveStatAdjustment` 把 `HpRegenPerTurn.amount` 折叠成 `StatModifier(hpRegen = amount.toDouble())`,与 `StatModifier.hpRegen` 走同一 sum-then-DR 路径,符合 §2.3 line 401-405。
  - `castSpeedRating` 同样 raw-then-DR (StatsCalculator.kt:117,135-137)。
- 3 个白盒验证场景:
  - `dark-uiux-pr04-01-static-passive-detail` (vanguard / unyielding):`initialFocusedTalentId = "unyielding"`,prereqs `intimidation=2`,`setUnspentTalentPoints = 1`,断言 STAT_MODIFIER (maxHp +10 / defense +1) + RESISTANCE_BONUS (PHYSICAL +1),禁词覆盖 `PlayerCommand.RespecTalentTree`、`ConfirmTalentDraftToReserve`、`EquipTalentToSlot`、`ACTIVE_TALENT_SLOT_CHOICE`。
  - `dark-uiux-pr04-01-trigger-passive-detail` (arcanist / mana_surge):`initialFocusedTalentId = "mana_surge"`,prereqs `arcane_shield=2`,断言 ON_KILL_RESOURCE_RESTORE MANA +3 + 3× DAMAGE_TYPE_BONUS (FIRE/COLD/LIGHTNING +3%)。
  - `dark-uiux-pr04-01-passive-action-suppression` (vanguard / bulwark_march):断言 CONDITIONAL_STAT_BONUS GUARD_STANCE_BUFF (defense +2, speed +1) + DAMAGE_VS_STATUS TAUNT +4%。

### Assumptions
- 假设 `DiminishingReturns.effectiveCastSpeed(c=100)` 与 `effectiveHpRegen(c=80)` 是 PR04-01 锁定常量;若后续 PR 调整 c 值,本审查所引数值会过期,但偏差类型不变。
- 假设 `passive detail line` 的 effective-value 显示语义采用"孤立 marginal 等价"(`effectiveCastSpeed(rankRaw) - effectiveCastSpeed(0) ≈ effectiveCastSpeed(rankRaw)`),与 §3.0 line 664 EV 声明一致;若策划另有"上下文 marginal"的口径,需额外澄清。

---

## 4. Findings

### [HIGH] [Correctness / UX Contract] castSpeedRating 在被动详情行渲染为 raw 整数,违反 §2.5 effective-decimal 契约

- **Where**:`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:5929`
- **Evidence**:
  ```kotlin
  private fun passiveStatDetailValues(modifier: StatModifier): List<PassiveStatDetailValue> =
      buildList {
          // ...
          addIntStat("castSpeedRating", modifier.castSpeedRating)
          addDecimalStat("hpRegen", modifier.hpRegen)
          // ...
      }
  ```
  - `addIntStat` 走 `signed(value)`,产出形如 `+5`、`+8` 的整数文本。
  - `modifier.castSpeedRating` 直接是 YAML 中的 raw rating (rank 5 `arcane_overload` 为 `5`,`balance_point` 为 `8`),没有经过 `DiminishingReturns.effectiveCastSpeed`。
- **Spec**:
  - §2.5 line 569:`castSpeedRating | signed decimal effective value, one fractional digit | arcane_overload, balance_point | ui.inspect.mod.cast_speed`
  - §2.5 line 583:`castSpeedRating | raw rating enters DiminishingReturns.effectiveCastSpeed(raw) | effective cast speed rounded to one decimal; raw rating may appear only as diagnostic text outside player-facing detail`
- **Impact**:
  - `arcane_overload` 与 `balance_point` 共占 12 个 PR04-01 talent 的 1/6 (静态被动里 2/6),且都是 rank 5 才达到 +5 / +8 rating 的高显示值,玩家感知最强。
  - 当前显示 `+8` 误导玩家把 raw rating 当作"立即可加的施法速度",实际 effective 仅约 +0.74。
  - 长线进度感会被错位放大 ("我加了 8 但施法体感没变"),且与 PlayerStatus HUD (HUD 走 effective) 数据不一致 — 玩家最容易在被动详情 + HUD 双面板对比时发现 bug。
  - 偏差量级 (rank 5,DR c=100):
    - `arcane_overload`:raw `+5` → effective ≈ `+0.48`,**显示偏差约 10.4×**。
    - `balance_point`:raw `+8` → effective ≈ `+0.74`,**显示偏差约 10.8×**。
- **Standards**:Spec 内部契约 §2.5,关联 §3.0 EV 声明 (line 664)。无外部 OWASP / CWE 引用。
- **Repro**:
  1. 启动 game,进入 Arcanist,学 `arcane_overload` rank 5 (或在 talent assign 界面 focus 该 talent)。
  2. 观察右栏 passive detail 中 `cast_speed` 一行 → 显示 `+5`。
  3. 同时观察 HUD `effectiveCastSpeed` → 显示约 `+0.5` (或对应 DR 输出)。
- **Recommendation** (最小补丁):
  1. 在 `passiveStatDetailValues` 内对 `castSpeedRating` 走 `DiminishingReturns.effectiveCastSpeed` 后再以 decimal 一位格式入行:
     ```kotlin
     addDecimalStat(
         statId = "castSpeedRating",
         value = DiminishingReturns.effectiveCastSpeed(modifier.castSpeedRating.toDouble()),
     )
     ```
  2. `addDecimalStat` 已有签名 `(statId: String, value: Double)` 并通过 `signedDecimal(value)` 输出一位小数,语义直接复用。
  3. 同步保留 `effectiveCastSpeed(0) = 0` 的假设以使 rank 0 / 无 rating 时被 `value != 0.0` 过滤掉。
  4. 若策划坚持要在被动详情同时显示 raw rating 用于诊断,可加 diagnostic tooltip / log,但不能作为 main line,符合 §2.5 line 583 "raw rating may appear only as diagnostic text outside player-facing detail"。
- **Tests** (必须):
  - 新增 `RenderSnapshotContractTest.arcaneOverloadRank5PassiveDetailCastSpeedIsEffectiveDecimal` — 校验 line value 是 `+0.7` (或当前 DR 公式产出的一位小数) 而非 `+5`。
  - 扩展 `dark-uiux-pr04-01-static-passive-detail` 验证场景为同时覆盖 `arcane_overload` 一栏 (见 finding 3 修复路径)。

---

### [HIGH] [Correctness / UX Contract] hpRegen 被动详情行未走 DiminishingReturns.effectiveHpRegen,违反 §2.5 / §2.3 单路径契约

- **Where**:`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:5930`
- **Evidence**:
  ```kotlin
  addDecimalStat("hpRegen", modifier.hpRegen)
  ```
  - `modifier.hpRegen` 是从 `levelEffects[rank].passiveEffects` 项 fold 出的 raw double。
  - `addDecimalStat` 直接 `signedDecimal(value)`,显示形如 `+1.2`、`+0.4`。
  - 与 `StatsCalculator.kt:129` 的 `effectiveHpRegen(rawHpRegen)` 不同路径。
- **Spec**:
  - §2.5 line 570:`hpRegen | signed decimal, one fractional digit | pain_fuel as StatModifier.hpRegen | ui.inspect.mod.hp_regen`
  - §2.5 line 584:`hpRegen | raw regen sum enters DiminishingReturns.effectiveHpRegen(rawHpRegen) | effective hp regen rounded to one decimal, matching turn-start healing`
  - §2.3 line 420:"UI detail renders one `hpRegen` line after aggregation and DR. It displays the same effective value used by turn-start healing, not the raw sum"
- **Impact**:
  - `pain_fuel` 是唯一 official `StatModifier.hpRegen` 来源,5 个 rank 的 raw 值为 `+0.4 / +0.6 / +0.8 / +1.0 / +1.2`。
  - DR c=80 在低值区域近线性,raw `+1.2` → effective ≈ `+1.18`,显示差距小但**契约偏差仍然存在**,且与 round2 HIGH-A 同根因。
  - 真正风险点是当装备 hpRegen + talent hpRegen 同时存在时,玩家看到的"+1.2 (passive)"叠加到"+x (equipment)"时无法对应到 HUD 的 effective hp regen — 这正是 §2.3 line 420 禁止的"separate equipment hp regen and talent hp regen rows for the same final derived stat" 反例。
  - 与 finding 1 同构,玩家体验破坏点是 inspect 详情 vs HUD 数字不一致。
- **Standards**:Spec §2.5 + §2.3。
- **Repro**:
  1. Berserker 学 `pain_fuel` rank 5。
  2. inspect 详情 `hp_regen` 行 → 显示 `+1.2`。
  3. HUD `hpRegen` (effective) ≈ `+1.2 *  (1 - 1.2/(1.2+80))` ≈ `+1.18` (近线性,数值差异微弱)。
  4. 装备一件 `+5 hpRegen` 的物品后,HUD effective ≈ `effectiveHpRegen(6.2)` ≈ `5.74`;详情仍显示 raw + 1.2,**与 HUD 算式无法对账**。
- **Recommendation**:
  - 等价于 finding 1 的修复模式:
    ```kotlin
    addDecimalStat(
        statId = "hpRegen",
        value = DiminishingReturns.effectiveHpRegen(modifier.hpRegen),
    )
    ```
  - 严谨做法是把 detail 计算的"baseline"取自当前 entity context (即 `effectiveHpRegen(currentRawWithThis) - effectiveHpRegen(currentRawWithoutThis)`),与 §3.0 line 664 EV 表达式严格一致;但 PR04-01 detail 当前是 per-rank static projection,baseline 无 context,采用 `effectiveHpRegen(rankRaw) - effectiveHpRegen(0)` 即可,且代码上等价于 `effectiveHpRegen(modifier.hpRegen)`(因为 `effectiveHpRegen(0) = 0`)。
  - 同时复核:`HpRegenPerTurn.amount` 在 `PassiveEffectResolver.resolveStatAdjustment` 已经折叠进 `StatModifier(hpRegen = amount.toDouble())`,因此 detail 行只走单一 hpRegen 字段不会丢失任何 talent 的 hpRegen 来源,但需要在 `passiveDetailLines(passive = HpRegenPerTurn …)` 分支与 `StatModifierEffect.hpRegen` 分支之间 dedupe,避免同一 effective hp regen 被双行显示 (现实现的 `HpRegenPerTurn` 走独立 `HP_REGEN_PER_TURN` line,与 `StatModifier.hpRegen` 走 `STAT_MODIFIER` line,会出现两条 hp regen 行)。这部分见 finding 4。
- **Tests** (必须):
  - 新增 `RenderSnapshotContractTest.painFuelRank5PassiveDetailHpRegenIsEffectiveDecimal`。
  - 扩展 / 新增白盒验证场景 (见 finding 3)。

---

### [MEDIUM] [Testing Coverage] 3 个 PR04-01 验证场景未断言 castSpeedRating / hpRegen 渲染格式

- **Where**:`game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`
- **Evidence**:
  - `dark-uiux-pr04-01-static-passive-detail` 选 `unyielding` (vanguard) — 涉及 maxHp / defense / PHYSICAL resistance,无 castSpeedRating / hpRegen。
  - `dark-uiux-pr04-01-trigger-passive-detail` 选 `mana_surge` (arcanist) — 涉及 OnKillResourceRestore MANA + DamageTypeBonus FIRE/COLD/LIGHTNING,无 castSpeedRating / hpRegen。
  - `dark-uiux-pr04-01-passive-action-suppression` 选 `bulwark_march` (vanguard) — 涉及 ConditionalStatBonus + DamageVsStatus,无 castSpeedRating / hpRegen。
  - §3.1 静态被动表中,`arcane_overload`、`balance_point` 是 castSpeedRating 唯一来源,`pain_fuel` 是 `StatModifier.hpRegen` 唯一来源 — 三者都没有进入白盒断言。
- **Impact**:
  - finding 1 / finding 2 这两条 HIGH 偏差在 CI 没有失败的根因 = 验证场景没覆盖到。
  - 这是 spec §2.5 既然指定了"effective decimal one fractional digit",却没有任何场景把"显示文本是 +0.7 / +1.1 / etc." 写成断言。
- **Recommendation**:
  - 最小补丁(推荐):扩展 `dark-uiux-pr04-01-static-passive-detail` 为 multi-focus 场景,先 focus `unyielding` 跑现有断言,再用 `validationTalentFocusRequest` 切到 `arcane_overload`、`balance_point`、`pain_fuel` 三个 talent 各跑一次 `PassiveLineAssertion` (lineKind=`STAT_MODIFIER`, statId=`castSpeedRating`, value=`+0.7`) 与 (statId=`hpRegen`, value=`+1.2` after DR)。
  - 备选:新增第 4 个场景 `dark-uiux-pr04-01-effective-display-route`,职责单一为 castSpeedRating / hpRegen 的 effective-decimal 显示断言;成本略高但语义更清晰,符合 PR04-01 既有 "one scenario per surface" 风格 (static / trigger / suppression 三分)。
- **Tests**:即修复方案本身。

---

### [MEDIUM] [Correctness] HpRegenPerTurn 与 StatModifier.hpRegen 在被动详情可能出现双行显示

- **Where**:
  - 显示分支:`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:5872-5887` (HpRegenPerTurn → `HP_REGEN_PER_TURN` line) 与 `5930` (StatModifier.hpRegen → `STAT_MODIFIER` line)。
  - Runtime aggregation:`core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt:155-166` 把 `HpRegenPerTurn.amount` 折叠到 `StatModifier(hpRegen = amount.toDouble())`。
- **Evidence**:
  - PassiveEffectResolver.resolveStatAdjustment:
    ```kotlin
    is PassiveEffect.HpRegenPerTurn -> StatModifier(hpRegen = passive.amount.toDouble())
    ```
  - 在战斗循环上 HpRegenPerTurn 与 StatModifier.hpRegen 同路径到达 `effectiveHpRegen` (§2.3 ✓)。
  - 但 detail 渲染时,一个 talent / 装备的 `HpRegenPerTurn` 仍走 `HP_REGEN_PER_TURN` 行,而它的 effective 增量也会随父 `StatModifier` 上的 hpRegen 列表项再写出一条 — 视乎该 talent 是否同时有 `StatModifierEffect{hpRegen=…}`。
  - PR04-01 官方 talent 中只有 `pain_fuel` 用 `StatModifier.hpRegen`,无 `HpRegenPerTurn`,所以**实际官方数据下不会显示双行**;但 schema parser 允许两种共存,因此风险随未来 talent 自动放大,且 §2.3 line 420 写明禁止"separate equipment hp regen and talent hp regen rows for the same final derived stat"。
- **Impact**:
  - 现阶段无 user-visible bug(无官方数据触发),但属于潜在 future regression。
- **Recommendation**:
  - 在 `passiveDetailLines(passive: HpRegenPerTurn)` 分支显式注释:"Stay consistent with StatModifier.hpRegen — only one row per source. If a future talent declares both kinds simultaneously, dedupe in projector."
  - 或在 projector 层合并:把 `HpRegenPerTurn` 也走 `STAT_MODIFIER` 行 (统一为 `STAT_MODIFIER + statId=hpRegen + effective decimal`),保留 `HP_REGEN_PER_TURN` 的 lineKind 仅给 equipment 来源以满足 §2.3 line 419 ("Talent StatModifier.hpRegen does not reuse the equipment cue key; it is visible through typed detail and source-aware derived stat tests")。
- **Tests**:
  - 新增 `RenderSnapshotContractTest.talentWithBothHpRegenPerTurnAndStatModifierHpRegenRendersSingleLine` — schema fixture,验证 dedupe。

---

### [MEDIUM] [Robustness] passiveStableKey 不在 talentRank 为 null 时 fail-fast

- **Where**:`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:12509-12516`
- **Evidence**:
  ```kotlin
  private fun passiveStableKey(source: PassiveSource, effectKind: String): String =
      when (source.kind) {
          PassiveSourceKind.EQUIPMENT -> equipmentPassiveStableKey(...)
          PassiveSourceKind.TALENT -> "passive:talent:${source.sourceTemplateId}:$effectKind:rank${source.talentRank ?: 1}"
      }
  ```
  - `source.talentRank ?: 1` 在 talent 来源没传 rank 时静默回退到 `rank1`。
- **Impact**:
  - 当前所有 talent passive source 在 `FoundationGameSession.kt:12027` 构造时强制传 rank (`talentRank = rank`),所以现实分支上不会触发。
  - 但 sealed 类的 `talentRank: Int?` 字段把"rank-as-required"的契约弱化为"rank-as-optional-with-silent-default",未来如果新增 caller 漏传 rank,所有 talent stable key 都会塌缩到 `rank1`,导致 effect dedupe / cue dedupe 在 §2.3 表里"per (effectKind, sourceId, rank)"的 sourceId 维度失去 rank 区分。
- **Recommendation**:
  - 二选一(等价偏好):
    1. `PassiveSource` 上把 `talentRank: Int?` 改为 `talentRank: Int = 0` 默认值并在 TALENT 构造点 require non-null;
    2. `passiveStableKey` 在 TALENT 分支 `requireNotNull(source.talentRank) { "TALENT passive source must carry talentRank: $source" }`,而不是 silent fallback。
- **Tests**:
  - `PassiveEffectResolverTest.talentPassiveSourceWithoutRankFailsFast`。

---

### [LOW] [Maintainability] passiveDetailLines 的 `when (passive)` 一段 220 行,缺乏拆分点

- **Where**:`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:5716-5887` (~172 行单方法 `when` 表达式)。
- **Impact**:每个 PassiveEffect kind 增减时,这块代码与 `passiveStatDetailValues` 一起容易膨胀,review diff 变长。
- **Recommendation**:把 10 个 kind 各自独立成 private function (类似 `statModifierPassiveDetailLines` 已经独立的模式),`passiveDetailLines(passive)` 只做 dispatch — 函数 inline 不影响热路径(项目侧 lint 已经在用 `when` exhaustive)。**不强制**,可在后续清理 PR 一并完成。

---

### [LOW] [Locale Hygiene] §2.5 表声明的 token 是否已全部在 zh-CN / en-US 落实未做完整抽样

- **Where**:`game/src/main/resources/i18n/en-US.json`、`zh-CN.json`。
- **Impact**:`ui.stat.*`、`ui.hud.*.short`、`ui.inspect.mod.*` 中,review 仅抽样验证;若有 token 缺失会让 inspect 行显示 raw key。
- **Recommendation**:本 PR 自审清单 §10 中如果还未覆盖该 token 全量 lint,补一条 `LocaleTokenLintTest.allPr0401PassiveStatTokensHavePairedZhAndEn` (本 finding 仅作风险提示;若已存在 lint,可关闭)。

---

### [NIT] [Naming] `passiveStatDetailValues` 与 `statModifierPassiveDetailLines` 在名字层次上模糊

- 提议在后续清理统一为 `passiveStatLines` / `statModifierPassiveLines`,降低 reader 阅读路径成本。**不强制**。

---

## 5. Performance

### Hotspots
- 被动 detail line 投影是只在 talent assign 视图 focus 切换 / rank up / refresh 时触发,非战斗热路径,O(passive count × kind) 复杂度,12 talent × ≤6 rank × ≤4 effect = 数百级别项,无问题。
- `PassiveEffectResolver.resolveStatAdjustment` 在每次 stat refresh 时 O(passiveSources)(本 PR 后 sources 包括 equipment + talent),源数量上限受 6 slot + ≤8 affix + ≤2 passive talent 限制,典型量级 < 40,fold 路径合理。
- `passiveDetailLines(passives)` 内部 `sortedBy(PassiveDetailLineSnapshot::sortKey)` 对单 rank effect 数量 ≤ 4 的小列表,排序开销可忽略。

### 修复后建议监控
- 把 finding 1 / finding 2 路由到 effective decimal 后,DR 计算是 O(1) 浮点,无 perf 风险。
- 如果未来同时存在 equipment hp regen + talent hp regen + StatModifier.hpRegen,detail 端 dedupe(finding 4 推荐方向)是 O(passives),不引入额外开销。

---

## 6. Integration

### API / contracts
- `PassiveSource` 增加 `talentRank: Int?` 与 `sourceTemplateId: String`,是 internal data class,无跨服务持久化;但**是 trace / cue 稳定 key 的输入源**,改动应保持 finding 5 建议的 fail-fast。
- `RenderSnapshot.TalentPassiveDetailSnapshot` 与 `PassiveDetailLineKindSnapshot` 是 client 边界契约,新增字段已通过 schema 测试覆盖。

### DB / migrations
- 无 DB 迁移。Save / replay 持久化的是 talent id + rank(§2.4 #8),与 derived passive value 解耦,本 PR 无需 backfill。

### Feature flags & rollout
- PR04-01 无 feature flag,合入即生效。若策划需要灰度,可在 `ValidationSessionOptions` / fixture 引入 toggle,但**不**建议把 PASSIVE 转换包到 flag 后面(会产生混合 state 期 active+passive 双形态,save 兼容性更复杂)。

### Resilience
- 本 PR 修改集中在 deterministic projection,无 retry / timeout。

### Rollback plan
- Spec 给的 rollback 路径是 revert 数据 + revert 代码 commit;PASSIVE 已学习的 talent 在 revert 后会回退为 ACTIVE 但 rank 保留,玩家可重新进入 talent assign 调整 reserve。若玩家已经在 PASSIVE 状态保存,active slot 中**不会**包含这 12 个 talent(因为 EquipTalentToSlot 拒绝 PASSIVE),所以 revert 后 active slot 仅空缺,无数据冲突。

---

## 7. Testing

### Coverage 当前状态
- ✅ Schema lint:`TalentSchemaTest.passiveRank5EvAnchorDeclaredWithinTwentyPercent`、`passiveEvDrRoutesDeclaredForRatingFields`、`conditionalPassiveUptimeIsDeclaredInDoc`、`conditionalPassivesDeclareTriggerOwnerInDoc`(spec §3.0 line 659-663 列出,执行覆盖率假设已就位)。
- ✅ Runtime refresh:`FoundationGameSessionTest.conditionalTalentPassiveRefreshesWhenStatusExpires`、`healthThresholdTalentPassiveRecomputesAfterDamageAndHealing`、`fixtureTerrainAffinityPassiveRefreshesAfterMovement`、`respecRemovesTalentPassiveDerivedStats`(spec §2.4 #1-#6 要求,代码侧由 FoundationGameSession 走 stat refresh 通道支持)。
- ✅ Resolver mix:`PassiveEffectResolverTest.equipmentAndTalentDamageTypeBonusUsesAdditivePercentNotMultiplicative`、`twoEquippedSourcesOnKillRestoreMergeAdditively`、`twoOnHitStatusProcSourcesRollIndependently`、`equipmentAndTalentStatModifiersDoNotDoubleAccumulateAfterRefresh`(spec §2.3 #3)。
- ✅ Action matrix:`InputHandlerTest.passiveTalentAssignRShortcutDoesNotEmitRespecCommand`、`passiveLockedByPrereqRShortcutDoesNotEmitRespecCommand`、`TalentSidebarPresenterTest.passiveFocusedFooterDoesNotShowReserveHint`、`passiveActionsDoNotShowReserveOrActiveSlotManagement`、`FoundationGameSessionTest.passiveRShortcutLeavesDraftLoadoutAndRanksUnchanged`、`equipTalentToSlotRejectsPassive`。
- ✅ Validation scenarios:3 个 PR04-01 场景已注册并接入 `ValidationScenarioRegistry`,通过 `validationTalentFocusRequest` + consume-once token 在 client 侧已能 focus 到具体 row。

### Gaps (本轮 review 关键发现)
- ❌ 没有任何测试覆盖 finding 1 / finding 2 的 effective-decimal 渲染格式(`arcane_overload`、`balance_point`、`pain_fuel`)。
- ❌ 没有测试覆盖 finding 4 的 HpRegenPerTurn + StatModifier.hpRegen 在 detail 端的 dedupe 行为。
- ❌ 没有测试覆盖 finding 5 的 talent passive source 未传 rank 的 fail-fast 行为。

### Flakiness risks
- 无并发依赖、无时间依赖、无外部 IO,验证场景全部 deterministic seed,无 flakiness 风险。

### 目标测试计划 (Given / When / Then)
1. **Given** Arcanist 已学 `arcane_overload` rank 5;**When** snapshot 投影出 `passiveDetail.currentLines`;**Then** 行 `castSpeedRating` 的 value 是 `signedDecimal(DiminishingReturns.effectiveCastSpeed(5.0))` 而非 `+5`。
2. **Given** Spellblade 已学 `balance_point` rank 5;**When** snapshot 同上;**Then** value 是 `signedDecimal(DiminishingReturns.effectiveCastSpeed(8.0))`。
3. **Given** Berserker 已学 `pain_fuel` rank 5;**When** snapshot 同上;**Then** value 是 `signedDecimal(DiminishingReturns.effectiveHpRegen(1.2))`,且 HUD `hpRegen` 与 detail 行字面相同。
4. **Given** fixture talent 同时声明 `HpRegenPerTurn{amount=2}` 与 `StatModifierEffect{statModifier=hpRegen=1.0}`;**When** snapshot 同上;**Then** 仅有一条 hp regen detail 行,value 是 `effectiveHpRegen(3.0)`。
5. **Given** 构造 `PassiveSource(kind = TALENT, talentRank = null, ...)`;**When** 调用 `passiveStableKey`;**Then** 抛 `IllegalArgumentException`。
6. **Given** validation scenario `dark-uiux-pr04-01-static-passive-detail` 扩展为 multi-focus;**When** 依次 focus `unyielding`、`arcane_overload`、`balance_point`、`pain_fuel`;**Then** 各自 `PassiveLineAssertion` 通过。

---

## 8. Docs & Observability

### Docs to update
- 修复 finding 1 / finding 2 后,在 `UI/pr/dark-uiux-pr04-01-playable-profession-passive-talents.md` §10 自审清单中新增一行:"§2.5 effective-decimal 路由由 `RenderSnapshotContractTest.*PassiveDetailCastSpeedIsEffectiveDecimal` / `*PassiveDetailHpRegenIsEffectiveDecimal` 自动覆盖"。
- 修复 finding 4 / finding 5 后,补充 `core/PassiveEffectResolver.md` (如果存在) 或在 `PassiveSource` data class 顶部注释中,明确 talent rank 必传。

### Logs / Metrics / Traces / Alerts
- 当前 passive trace cue 走 `CombatResolutionTrace`(`passive:<effectKind>:<sourceId>:rank<rank>` 之类的稳定 key),已经在 §2.3 line 355 表覆盖,无 PII / 高基数风险。
- finding 1 / finding 2 修复后,建议在 inspect log (developer build) 同时打 raw rating 与 effective decimal,便于 QA 复盘 — 这是 §2.5 line 583 "diagnostic text outside player-facing detail" 的合规做法,**非强制**。

### Runbook
- PR04-01 涉及面已经被 round2 / round5 / round6 报告覆盖完整,本轮无新增运维项。

---

## 9. Open Questions

1. **Q1**:`passive detail` 中的 effective-value 是按"孤立 marginal"(`effectiveCastSpeed(rankRaw)`)还是"上下文 marginal"(`effectiveCastSpeed(currentRawWithThis) - effectiveCastSpeed(currentRawWithoutThis)`)显示?
   - 建议:per-rank 静态 projection 用孤立 marginal,与 §3.0 EV 表(line 664)的 baseline=0 一致;HUD inspect 详情(若未来加入分源面板)再考虑上下文 marginal。
   - 选定后请在 §2.5 line 583 补一句明确语句,避免下一个 reviewer 重新提同样问题。
2. **Q2**:finding 5 的 fail-fast 是否会破坏现有 save 兼容性?
   - 评估:save 持久化 talent id + rank,不持久化 `PassiveSource`,因此 fail-fast 仅影响代码路径,不影响 save。**安全**。
3. **Q3**:finding 3 推荐的 "扩展 static-passive-detail 为 multi-focus" 是否会让单场景断言面变得过大,降低单测可读性?
   - 评估:`PassiveLineAssertion` 数量从 ~3 升到 ~10,仍在场景可读阈值内;若团队偏好单场景单 focus,改走新增第 4 个场景方案。

---

## 10. Final recommendation

### Decision
**request_changes**

### Must-fix before merge
1. **HIGH** finding 1:`castSpeedRating` 在被动详情走 `DiminishingReturns.effectiveCastSpeed`,以 decimal 一位渲染,并新增 `arcane_overload` / `balance_point` 的 rank 5 effective-decimal 断言。
2. **HIGH** finding 2:`hpRegen` 在被动详情走 `DiminishingReturns.effectiveHpRegen`,同样以 decimal 一位渲染,并新增 `pain_fuel` rank 5 effective-decimal 断言。
3. **MEDIUM** finding 3:扩展 `dark-uiux-pr04-01-static-passive-detail` 或新增第 4 个验证场景以覆盖 finding 1 / finding 2 — 这是阻止 HIGH 在未来回归的硬护栏。

### Nice-to-have post-merge
- finding 4:HpRegenPerTurn 与 StatModifier.hpRegen 的 detail dedupe 与显式注释 / 合并策略。
- finding 5:`passiveStableKey` / `PassiveSource` 的 talent rank fail-fast。
- finding 6:`passiveDetailLines` 拆分。
- finding 8:i18n 全量 token lint。

### Confidence
**High** — 数据 / schema / resolver / 行动矩阵 / focus 通道 / 稳定 key 全部按契约对齐;两条 HIGH 偏差均来自显式表述,可单独最小补丁解决,不需要重构。

---

## 11. Self-audit cross-reference (§10 of spec)

| 自审项 | 状态 | 说明 |
| --- | --- | --- |
| Schema 全字段映射 | ✅ | DataLoader.toRuntimePassive 10 kind + fail-fast |
| Rank 表数值对账 | ✅ | 12 talent / 60 rank 已逐项校验 |
| 稳定 key 格式 | ✅ | Equipment 保留 legacy literal,Talent 用 `passive:talent:<id>:<kind>:rank<r>` |
| PASSIVE 行动矩阵 | ✅ | Presenter / InputHandler / Session 三处闭环 |
| Focus-by-id 通道 | ✅ | `validationTalentFocusRequest` + consume-once token 已落地 client |
| HP regen 单路径 | ✅ runtime / ⚠️ display | runtime 走 effectiveHpRegen;display 仍 raw — finding 2 |
| castSpeedRating 单路径 | ✅ runtime / ⚠️ display | runtime 走 effectiveCastSpeed;display raw int — finding 1 |
| §2.5 显示格式 | ⚠️ | castSpeedRating / hpRegen 偏离 — finding 1 / 2 |
| §3.0 EV 锚点 | ✅ | rank5NormalizedEv 全部在 [80,120] 区间 |
| §3.2 trigger availability | ✅ | 6 个 trigger 被动的 owner / uptime / 实现证据齐全 |
| 3 个验证场景 | ✅ structure / ⚠️ coverage | scenario 已注册;但 cast/hpRegen 格式断言缺失 — finding 3 |
| Save / replay 兼容性 | ✅ | 持久化 talent id+rank,与 derived passive 解耦 |

---

**End of Round 3 Director Deep Review.**

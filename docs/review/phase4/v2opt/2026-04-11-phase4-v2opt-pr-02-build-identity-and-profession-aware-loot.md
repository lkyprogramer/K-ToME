> 执行前必须先完整阅读并接受：
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md`
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md`
> `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-03-equipment-passive-vocabulary-and-item-content-density.md`
> `docs/phase4/2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md`

# Phase 4 - V2OPT PR-02 成长身份救火与职业化掉落分发

**阶段**: `Phase 4 / Post-Review Follow-up / V2OPT-W2`  
**优先级**: `P0`  
**工作量评估**: `XL`（`6~8` 人日）  
**前置条件**: `V2OPT-PR-01` 已冻结 terminal identity owner metric  
**对应问题**:

1. full-route 的四个职业终盘主武器全部收敛到 `battle_axe`
2. 当前掉落系统已经有 affix/special bias，但 base item 分发仍然近似 profession-agnostic
3. `SmokeBot` 已有职业偏好，但 runtime 奖励面仍把这些偏好压扁
4. `DamageVsTag` 对主要后续阵营覆盖严重不足

---

## 1. 阶段目标

让成长系统重新承担“职业身份放大器”的职责，而不是继续让四职业站到同一把斧头上。

完成标准：

1. full-route 不再出现 `12/12 battle_axe` 的终盘收敛。
2. base item 分发正式接入 profession/build context。
3. `battle_axe` 不再是对 `arcanist / rogue` 的默认最优终盘答案。
4. `DamageVsTag` 扩展到主要后续阵营，服务真实 zone 与 profession 组合。
5. 所有改动都通过 `lootBalanceLab / soloClearLab / longRunLab / phase4Report` 联合验收。

---

## 2. 工作量评估与整合结论

### 2.1 为什么这是 `XL`，但仍然必须是一个 PR

这个问题横跨三个层级：

1. **掉落分发层**：当前 base item 候选权重不看 profession/build。
2. **内容层**：base item 底盘和 loot profile 权重共同推动收敛。
3. **验收层**：若 long-run owner metric 不跟着改，调优会失控。

如果拆成多个 PR：

1. 只改分发不改数据，会形成一轮“逻辑变了、表还旧”的不稳定中间态。
2. 只改 base item 或 profile，不改分发上下文，仍会被 generic high-base weapon 吞并。
3. 只补 `DamageVsTag` 而不改 distribution，收益会被掉落入口吃掉。

因此必须整合为一个 PR，但 PR 内要强制分成 staged commits。

### 2.2 本 PR 的整合结论

本 PR **合并** 以下原本容易被误拆的工作：

1. profession-aware base item weighting
2. `items/index.yaml` 的 base item 底盘审计
3. `loot/index.yaml` 的 typeWeights / slotBias 调整
4. `DamageVsTag` 阵营扩面
5. `SmokeBot` 与 long-run owner metric 联动回归

本 PR **不**处理：

1. secret reward identity
2. hidden organic tuning
3. terrain / elite 主题语义

---

## 3. 当前问题拆解

### 3.1 runtime 当前怎么选 base item

当前路径是：

1. `LootProfileCandidatePoolResolver` 解析 `itemIds / itemTagFilter / typeWeights / slotBias`
2. `FoundationGameSession.lootWeightedBaseCandidatesFromPools(...)` 计算 base item 权重
3. `ItemGenerator.rollAndGenerate(...)` 再在此基础上叠 affix/special bias

现状的问题是：

1. affix 与 special bias 已经知道 build context
2. **base item 层几乎不知道**
3. 结果就是强底盘 base weapon 在进入 affix/special 阶段前就已经赢了

### 3.2 现有 build context 没有被充分利用

当前仓库已有：

1. `professionAffixBuildContext(...)`
2. `currentAffixBuildContext()`
3. `AffixBuildTags.kt`

但这些主要服务 affix / special template 语义，并没有直接参与 base item 候选选择。  
这说明本 PR 不需要重新造第三套 build context，只需要让 base item selection 正式消费现有 build tags。

### 3.3 `SmokeBot` 已经暴露出症状

`SmokeBot.preferredWeaponScore(...)` 明确给了职业偏好：

1. `MANA` -> `arcane_staff`
2. `ENERGY` -> `short_sword / hunter_bow`
3. `STAMINA` -> `battle_axe / long_sword`

但 full-route 结果仍然全面收敛。  
这意味着问题不在 bot 决策，而在 reward surface 和底盘强度。

---

## 4. 本 PR 必须冻结的合同

1. **继续复用现有 `AffixSelectionContext` / build tags**，不新增第二套 build identity system。
2. profession/build 影响 **base item 分发权重**，不影响 `LootBudget` 数学主链。
3. 优先通过分发与标签语义修正问题，最后才动 base stats。
4. `battle_axe` 可以被降权或削弱，但不允许通过“把其它职业强行绑白名单”做硬编码修复。
5. `DamageVsTag` 扩面必须围绕真实后续 zone 阵营，不做泛滥堆词条。

---

## 5. 范围与非目标

### 5.1 范围

1. base item 权重正式消费 build context
2. base item tag 审计与补齐
3. `loot/index.yaml` 中关键 profile 的 type/slot 偏置重调
4. `items/index.yaml` 中关键武器底盘与 faction counter 词条扩面
5. `SmokeBot` 与 long-run metric 的同步校正

### 5.2 非目标

1. 不重写 `LootBudgetResolver`
2. 不重做 affix/special 系统
3. 不把 secret reward 返工混进来
4. 不做 UI 抛光

---

## 6. 技术方案

### 6.1 让 base item selection 正式消费 build context

建议新增或扩展：

```text
game/src/main/kotlin/com/ktome/game/loot/LootBaseSelectionContext.kt
game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
```

建议结构：

```kotlin
data class LootBaseSelectionContext(
    val buildTags: Set<String>,
    val professionId: String,
    val primaryResourceType: String?,
    val committedTalentIds: Set<String>,
)
```

使用原则：

1. 这个 context 不新建词汇体系，只是 `AffixSelectionContext` 的 base item 专用投影视图。
2. base item 权重调整使用：
   - base item tags
   - slot
   - item type
   - profession/build semantic tags

### 6.2 Weight 规则

不允许用一堆 if/else 写死职业。  
正式规则建议分三层：

1. **profile weight**
   - 继续由 `typeWeights / slotBias` 提供 zone/reward 通道偏置
2. **item semantic weight**
   - base item tags 与 build tags 的交集产生额外倍率
3. **anti-collapse guard**
   - 对跨 profession 过度 dominant 的 base item 加 soft cap，而不是全局封禁

建议公式：

```text
finalWeight =
  base.dropWeight
  × profile.typeWeight
  × profile.slotBias
  × buildTagMatchMultiplier
  × antiCollapseMultiplier
```

`buildTagMatchMultiplier` 建议：

1. 无匹配 -> `1.0`
2. 弱匹配 -> `1.15`
3. 强匹配 -> `1.35`

`antiCollapseMultiplier` 建议：

1. 默认 `1.0`
2. 当某 base item 被定义为 cross-profession dominant risk 时，对不匹配 profession 降到 `0.70~0.85`

### 6.3 Base item tag 审计

建议先做一次 registry audit，重点对象：

1. `battle_axe`
2. `long_sword`
3. `arcane_staff`
4. `short_sword`
5. `hunter_bow`
6. `war_maul`
7. `forgebreaker_pick`

每个 base item 必须有清晰的语义 tags，例如：

1. `battle_axe` -> `frontline`, `cleave`, `stamina`, `bruise`
2. `arcane_staff` -> `mana`, `spell`, `ranged`, `channel`
3. `short_sword` -> `energy`, `precision`, `finesse`, `mark`
4. `hunter_bow` -> `energy`, `range`, `kite`, `precision`

约束：

1. 不用职业名直接给 item 打死标签。
2. 用玩法语义标签，让后续 advanced class 也能复用。

### 6.4 `items/index.yaml` 的底盘调整顺序

必须按以下顺序推进：

1. 先调 tag 和 distribution
2. 再调 `typeWeights / slotBias`
3. 最后才调 `baseAttack / derived stat floor`

严禁一上来直接把 `battle_axe.baseAttack` 砍穿，因为这会污染：

1. `vanguard`
2. `templar`
3. 多个 existing boss loot 基线

### 6.5 `DamageVsTag` 扩面

当前主要空缺阵营：

1. `orc`
2. `cultist`
3. `forge`
4. `river`
5. `crystal`
6. `abyssal`

要求：

1. 每个空缺阵营至少补 1 条正式词缀
2. PREFIX / SUFFIX 分布不能全堆一边
3. 不让 `DamageVsTag` 重新变成只服务单职业的 niche token

### 6.6 `SmokeBot` 回归策略

`SmokeBot` 不应成为主修复路径，但必须同步调整：

1. 保持 profession preference 逻辑与新的分发语义一致
2. 不能因为 bot 旧偏好与新 runtime 矛盾，导致 long-run metric 失真

---

## 7. 推荐改动面

### 7.1 `game`

1. [LootProfileCandidatePoolResolver.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt)
2. [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
3. [AffixBuildTags.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/AffixBuildTags.kt)
4. [GameModule.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameModule.kt)
5. [items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)
6. [loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml)

### 7.2 `harness / tools`

1. [SmokeBot.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/harness/SmokeBot.kt)
2. [LongRunLabFullTest.kt](/Users/luo/Documents/github/K-ToME/game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt)
3. `phase4Report` 新增的 identity metrics 透传

---

## 8. 实施顺序

### Task 1：base item semantic tag audit

- **目标**：先把 base item 语义标签做对
- **文件**：`items/index.yaml`
- **验收**：
  - 关键 weapon base 都具备玩法语义标签
  - 标签不直接硬绑 profession 名

### Task 2：profession-aware base weighting

- **目标**：把 build context 正式接入 base item selection
- **文件**：`LootProfileCandidatePoolResolver.kt`, `FoundationGameSession.kt`
- **验收**：
  - 分发逻辑可解释
  - 不是 switch-case 写死职业

### Task 3：profile reweight

- **目标**：重调关键 profile 的 `typeWeights / slotBias`
- **文件**：`loot/index.yaml`
- **重点 profile**：
  - foundation / cadence
  - late-route / boss / support
  - 强影响 full-route 的主线 reward channel

### Task 4：anti-collapse base item audit

- **目标**：处理 `battle_axe` 的跨职业 dominance
- **文件**：`items/index.yaml`
- **原则**：
  - 先软降
  - 最后才动硬面板

### Task 5：`DamageVsTag` 扩面

- **目标**：补齐后续主要阵营 counter surface
- **文件**：`items/index.yaml`
- **验收**：
  - 主要后续阵营都有覆盖
  - 不是纯数量补齐

### Task 6：SmokeBot 与 long-run owner metric 回归

- **目标**：让 harness 结论与 runtime 新语义一致
- **文件**：`SmokeBot.kt`, `LongRunLabFullTest.kt`

---

## 9. 资源生成计划

### 9.1 图片

本 PR 不新增图片资源。

### 9.2 音频

本 PR 不新增音频资源。

### 9.3 复用基线

1. 所有 base item、unique、artifact 继续复用现有 canonical item 资源，主基线为 `Phase4 PR05` 已落地的 `item.unique.*` / `item.artifact.*` icon、visual 与对应 item audio。
2. `DamageVsTag`、build tag、slot bias、profile reweight 只改变分发与语义，不产生新的玩家可见资源 key。
3. 奖励预览、inspect、日志若需要更强提示，只允许复用现有 item icon / audio family，不得借机新开一轮 profession-themed 资源批次。

### 9.4 约束

1. 若实现依赖新增 `item.unique.*`、`item.artifact.*`、`audio.item.*` key 才能让职业身份成立，说明修的是表面，不是分发合同，本 PR 视为方向错误。
2. `battle_axe` dominance 修复必须由 weighting、tag 与 base item 底盘完成，不能通过“给其他职业补一套新皮肤/新音效”掩盖。
3. 本 PR 不新增任何 `assets-src/*/specs/phase4-v2opt-pr02-*` 文件。

---

## 10. 测试策略

### 9.1 自动化命令

```bash
./gradlew lootBalanceLab
./gradlew soloClearLab
./gradlew longRunLab
./gradlew phase4Report
```

### 9.2 必测行为

1. full-route 不再 `12/12 battle_axe`
2. `professionAlignedWeaponAdoptionRate` 达标
3. `crossProfessionTopWeaponDominance` 明显下降
4. `lootBalanceLab` 分布仍在容差内
5. `soloClearLab` / `longRunLab` 没有整体崩坏

### 9.3 推荐新增测试

1. `LootProfileCandidatePoolResolverTest`
   - profession/build bias 对 base item weight 的影响
2. `SmokeBotTest`
   - bot 不会因为旧偏好压制新 distribution
3. `LongRunLabFullTest`
   - terminal identity 指标断言

---

## 11. 出口门禁

1. full-route 不再出现单一 base weapon 全职业垄断。
2. `phase4Report` 的 terminal identity 三指标通过。
3. `lootBalanceLab` 继续 PASS。
4. `soloClearLab` 与 `longRunLab` 无系统性回归。

---

## 12. 风险与 Gotchas

1. **不要只靠 nerf `battle_axe` 收尾**  
   这会把问题从“身份缺失”变成“整体掉落变弱”。
2. **不要引入第二套 build context**  
   必须复用现有 build tag 体系。
3. **不要把 secret reward 调整混进来**  
   这属于 `V2OPT-PR-03`。
4. **不要让 `DamageVsTag` 词条泛滥**  
   目标是 build surface 扩面，不是词条膨胀。

---

## 13. 回滚策略

1. 若 base stat 调整副作用过大，优先回退 stat，保留 distribution 和 tag 语义。
2. 若 profession-aware weighting 过强导致掉落过窄，优先调 multiplier，不回退整个 context 接线。

> 执行前必须先完整阅读并接受：
> `docs/rule/kotlin.md`
> `docs/rule/ai-change-governance.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part2.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part3.md`
> `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-01-experience-gate-and-critical-path-pacing.md`

# Phase 4 V3 PR-02 Build Identity、Dynamic Loot 与 Profession Capstones

**阶段**: `Phase 4 / Post-Review Follow-up / V3-PR-02`  
**优先级**: `P0`  
**工作量评估**: `L`（`5~8` 人日）  
**前置条件**: `V3-PR-01`  
**对应问题**:

1. `templar / vanguard` 终局武器明显收敛
2. 大量中后段 cadence/reward profile 仍是 `FIXED_LIST`
3. special-tier identity 仍主要靠通用 passive 组合撑着
4. 玩家缺少可追逐的 profession capstone target

## 0. 验证约束

1. 默认开发回路先跑 `./gradlew verifyChanged`。
2. 本 PR 默认联合验收固定为：
   - `lootBalanceLab`
   - `longRunLab`
   - `soloClearLab`
   - `verifyOwner`
   - `phase4Report`
3. canonical 证据必须包含：
   - `professionAlignedWeaponAdoptionRate`
   - `professionTerminalWeaponDistribution`
   - `crossProfessionTopWeaponDominance`
   - `dynamicPoolCoverage`
   - `specialTierPassiveFamilyDuplicateCount`
   - `professionCapstoneSeenRate`
   - `professionCapstoneAdoptionRate`
   - `nonWeaponBuildPayoffRate`

## 1. 阶段目标

把当前“中后段继续赌随机池”的成长体验，改造成“玩家能明确追逐职业终局锚点”的成长体验。

完成标准：

1. 主路径中后段 reward pool 不再大面积停留在 `FIXED_LIST`
2. 终局武器/装备不再被少数 base 吞并
3. 每个基础职业至少有 1 到 2 个可追逐的 capstone 装备

## 2. 为什么这一组要合并

这几个问题本质上是同一件事：

1. 动态池覆盖不足会让 reward 后半段变平
2. 动态池再强，如果 special-tier 身份太浅，仍然没有真正的 build peak
3. special-tier 再强，如果没有 profession capstone chase path，玩家还是只是在等随机数

所以必须合成一个完整的 reward ecology PR，而不是拆成三个小 patch。

## 3. 当前问题拆解

### 3.1 中后段 reward pool 覆盖不足

当前 [game/src/main/resources/data/loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml) 中，多个关键区和 reward/finale profile 仍为 `FIXED_LIST`。

### 3.2 profession-aware loot 只局部成立

`TAG_WEIGHTED` 目前主要集中在已重点升级的少数区，职业 identity 无法稳定贯穿整条 run。

### 3.3 special-tier 还不够“special”

当前 unique / artifact 已经有 passive，但 special-tier 模板合同仍偏浅，很多身份仍停留在“更强的通用组合”。

### 3.4 玩家缺少 profession capstone target

run 后半段缺一个明确的“我要追这件装备”的目标，导致成长驱动被动化。

## 4. 必须冻结的合同

1. 不新增新的核心枚举与规则语义。
2. 不引入大型 crafting / forging 子系统。
3. capstone 方案优先复用现有 special item、reward profile、secret zone、boss/finale 路径。

## 5. 范围与非目标

### 5.1 范围

1. `loot.index` 动态池覆盖扩展
2. typeWeights / slotBias / item taxonomy 调整
3. special-tier owner metric 强化
4. profession capstone chase path 设计与接线

### 5.2 非目标

1. 不新增新资源族
2. 不重写 hidden contract
3. 不做 boss phase 系统

## 6. 技术方案

### 6.1 dynamic loot 全覆盖

把以下 profile 从 `FIXED_LIST` 推进到 `TAG_WEIGHTED` 或等价 zone-aware 动态池：

1. `shattered_outpost.cadence`
2. `bandit_camp.cadence`
3. `elven_ruins.cadence`
4. `molten_core.cadence`
5. `grey_gate_depths.cadence`
6. `crystal_cavern.cadence`
7. `grey_gate_depths.reward`
8. `underground_river.reward`
9. `abyssal_temple.reward`
10. `abyssal_heart.reward`

### 6.2 profession-aware 分发强化

1. 拉高 strong match 权重
2. 引入 dominant terminal base 的递减约束
3. 重做 `templar / vanguard` 的终局池分叉
4. 提高副手 / 护甲 / 非武器 payoff 的中后段存在感
5. 让关键 reward 节点里的 `OFF_HAND / ARMOR` 不只是“能掉到”，而是能稳定承担 build-defining payoff

### 6.3 profession capstone chase path

原则：

1. 每个基础职业至少 2 个 capstone
2. 至少 1 个不是武器
3. 主路径可追求，低概率直掉只作捷径

第一轮种子建议：

1. `vanguard`
   - `artifact_forge_oath`
   - `unique_furnace_plate` / `unique_quenchbreaker_maul`
2. `arcanist`
   - `artifact_river_echo`
   - `unique_deepcurrent_lens`
3. `rogue`
   - `artifact_heartroot_gambit`
   - `unique_thornpath_crook` / `unique_briarbound_bow`
4. `templar`
   - `artifact_eclipsed_relic`
   - `unique_vesper_chainmail` / `unique_voidlit_seal`

主获取路径：

1. secret zone / hidden event 的 guaranteed reward 或 profession-tagged shard
2. boss / finale 的职业锚点高权重掉落
3. elite / boss 的低概率直掉，带 unseen bias / pity

### 6.4 special-tier 深化

第一轮不新开大系统，先做三件事：

1. special-tier owner metric
2. 同区 passive family 去重
3. 拾取瞬间前台明确“这是一件职业锚点”

## 7. 推荐改动面

1. `game/src/main/resources/data/loot/index.yaml`
2. `game/src/main/resources/data/items/index.yaml`
3. `game/src/main/resources/data/secret-zones/index.yaml`
4. `game/src/main/resources/data/events/index.yaml`
5. `game/src/main/kotlin/com/ktome/game/loot/*`
6. `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
7. `tools` 侧 owner metric/report

## 8. 任务拆解

### Task 1：dynamic loot 覆盖扩展

- **目标**: 把中后段 reward 从固定清单推进到 zone-aware 动态池
- **验收**:
  - `dynamicPoolCoverage` 达到目标
  - 中后段区的 distinct item/affix 组合明显提升

### Task 2：profession-aware 分发重构

- **目标**: 消除跨职业终局收敛
- **验收**:
  - `crossProfessionTopWeaponDominance` 下降
  - `professionAlignedWeaponAdoptionRate` 提升
  - `nonWeaponBuildPayoffRate` 提升，`OFF_HAND / ARMOR` 在中后段的 adoptable payoff 可观测增加

### Task 3：profession capstone chase path 接线

- **目标**: 让玩家在 30 分钟后有明确追逐目标
- **验收**:
  - 每个基础职业至少 2 个 capstone 可被看到
  - `professionCapstoneSeenRate / professionCapstoneAdoptionRate` 为正式 owner metric，且 `professionCapstoneSeenRate` 需要按职业 breakdown 执行最小 seen floor

### Task 4：special-tier identity owner metric

- **目标**: 避免 special-tier 继续只是“更好的通用组合”
- **验收**:
  - `specialTierPassiveFamilyDuplicateCount = 0`
  - 同区 special-tier passive kind 重复率下降
  - 拾取后玩法转向率可观察

## 9. 推荐命令

```bash
./gradlew lootBalanceLab
./gradlew soloClearLab
./gradlew longRunLab
./gradlew verifyOwner
./gradlew phase4Report
```

## 10. 完成后才能进入下一 PR 的条件

1. reward 后半段已经不再是固定清单抽奖。
2. 玩家有明确 profession capstone target，而不是继续盲赌。
3. special-tier 在体验上已经开始像“锚点”，而不只是“更高分组合”。

> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md`
> `docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`

# Phase 4 - OPT PR-02 精英突变补齐与 Boss 变体差异拉开

**阶段**: `Phase 4 / Post-Review Follow-up / OPT-W2`  
**优先级**: `P0`  
**前置条件**: `OPT PR-01` 完成，能够读取 mutation/boss 体验基线  
**对应问题**: 当前仓库只有 `6` 个 `eliteMutations`，低于路线图对 `elite mutation package ≈12` 的要求，Boss variant 的战术差异也不够明显。

---

## 1. 阶段目标

把精英突变从 `6` 套补到 `12` 套左右，并且让 `3` 个正式 Boss variant 在 mutation 组合和行为权重上形成可解释差异。

完成标准：

1. `eliteMutationDistinctCount >= 12`
2. `eliteMutationValidPairCount >= 40`
3. `BossHarnessTest` 能证明三组 variant 的 mutation 组合两两不同
4. 新 mutation 的 i18n、icon、audio cue 全部走正式 manifest / lint / harness

## 2. 当前问题

1. 当前 `eliteMutations` 只有 `6` 个，tier 分布为 `1 MINOR + 4 MAJOR + 1 SIGNATURE`。
2. 元素包目前只覆盖火焰和潮汐，没有形成更完整的 zone identity。
3. Boss variant 的 mutation 与 actionWeight 差异仍偏弱。

### 2.1 本 PR 必须冻结的口径

1. 同一 elite 最多 `2` 个 mutation。
2. `SIGNATURE + SIGNATURE` 禁止。
3. `incompatibleWith` 在数据加载阶段 fail-fast。
4. 本 PR 默认**不新增**新的 `StatusEffectType` 和新的 boss action id。
5. status / effect 语义允许复用现有 registry，但长期存在的 elite 行为不应直接绑定玩家 talent id；若行为节奏或平衡目标不同，应建立 dedicated elite-only talent id。

## 3. 范围与非目标

### 3.1 范围

1. 新增 `6` 个 mutation。
2. 调整 `boss-variants/index.yaml` 的 mutation 组合与现有 actionWeightProfile。
3. 为长期保留的 elite ability 建 dedicated talent entry（仍走正式 talent registry）。
4. 新增 mutation 的 i18n / icon / audio cue。
5. 补 mutation 与 Boss variant 的白盒/报告指标。

### 3.2 非目标

1. 不改 `Phase 4` 的 `TerrainTag`、`DamageType`、`ResourceType` 合同。
2. 不为复用的 talent 单独再生一套资源。
3. 不把 `PR-07` 的 hidden content 返工混进本 PR。

## 4. 技术方案

### 4.1 新增 mutation 设计

| ID | Kind | Tier | threatCost | minFloor | allowedZones | 设计意图 |
| --- | --- | --- | --- | --- | --- | --- |
| `elite.ironhide` | `STAT_PACKAGE` | `MINOR` | 2 | 1 | `[]` | 防御向 stat 包，和 `stonehide` 形成差异化 |
| `elite.phase_runner` | `AI_SHIFT` | `MINOR` | 2 | 2 | `[]` | 闪现/换位型精英，区别于 `hunt_protocol` |
| `elite.war_caller` | `ABILITY_GRANT` | `MAJOR` | 3 | 2 | `[]` | 低血时更偏向支援/增益 |
| `elite.corrosion_cloud` | `AURA` | `MAJOR` | 4 | 3 | `[deep_iron_pit, molten_core]` | 破甲 aura，强化矿坑/熔炉身份 |
| `elite.frostbound` | `ELEMENT_PACKAGE` | `MAJOR` | 4 | 3 | `[underground_river, crystal_cavern]` | COLD 元素包，补齐水/冰区的战术 identity |
| `elite.void_mirror` | `AURA` | `SIGNATURE` | 5 | 4 | `[grey_gate_depths, abyssal_temple, abyssal_heart]` | 防护/压制型 aura，替代原本不成立的 `SPELL_REFLECT` 方向 |

### 4.2 复用 effect 语义，而不是长期复用玩家 talent id

默认设计：

1. `elite.war_caller`
   - 新建 dedicated elite talent（例如 `elite_war_call`）
   - 效果语义可复用 `war_cry / rallying_banner` 的现有 EffectOp 组合
2. `elite.frostbound`
   - 新建 dedicated elite talent（例如 `elite_frost_nova`）
   - status / area / damage 语义可复用现有 `frost_nova`
3. `elite.void_mirror`
   - 复用现有 `ARCANE_SHIELD_BUFF` status 语义，但 talent/trigger 入口独立命名

补充运行时语义：

1. 对 mutation `AURA`，`DEBUFF` 继续作用于 aura 半径内 hostile。
2. 对 mutation `AURA`，`BUFF` 不外溢给 hostile；只在 aura 半径内存在 hostile 时刷新到 owner 自身。
3. 不因此新增新的 `StatusEffectType`，`void_mirror` 直接复用 `ARCANE_SHIELD_BUFF`。

约束：

1. 不新增 `SPELL_REFLECT`
2. 不新建“只有 Boss variant 能用”的私有 action catalog；若需新 talent id，仍走正式 talent registry

### 4.2.1 Dedicated Elite Talent 合同

为避免把玩家平衡链与 monster/boss 行为永久耦合，本 PR 对长期存在的 elite ability 采用以下正式约束：

1. talent id 命名固定加前缀：`elite_*`
2. `elite_*` talent 仍进入统一 `talents/index.yaml`，但必须带清晰 tag：
   - `tags: [elite_only, monster_only, mutation]`
3. `elite_*` talent 不进入职业树、不进入玩家起始 kit、不进入掉落奖励
4. `elite_*` talent 允许复用现有 `EffectOp / StatusEffect / DescriptionModel / telegraphRef`
5. `elite_*` talent 的数值节奏可与玩家 talent 分叉，不受玩家技能平衡回归牵连

### 4.2.2 实施边界

实现时按以下顺序推进，避免又退回“先直接复用玩家 talent，再说以后拆”：

1. 先创建 `elite_war_call` / `elite_frost_nova` 等 dedicated talent entry
2. 再把 mutation 的 `grantedTalents` 指向这些 elite talent
3. 如需复用视觉/音频/telegraph，可复用 key，不强制重做资源
4. 不允许最终主干里保留“同一个 talent id 同时服务玩家和 elite mutation”的长期设计

### 4.3 互斥关系

新增互斥最小集：

1. `corrosion_cloud ↔ dread_aura`
2. `frostbound ↔ emberblood`
3. `frostbound ↔ tidebound`
4. `void_mirror ↔ dread_aura`
5. `void_mirror ↔ corrosion_cloud`
6. `war_caller ↔ battle_drill`

### 4.4 Boss variant 调整

| Boss Variant | 当前 | 调整后 | 设计目标 |
| --- | --- | --- | --- |
| `molten_glass` | `stonehide + emberblood` | `ironhide + emberblood` | 防御+火焰重装 |
| `grey_crown` | `dread_aura` | `dread_aura + war_caller` | 支援/增益型指挥者 |
| `abyssal_eclipse` | `emberblood + dread_aura` | `void_mirror + corrosion_cloud` | 压制/减甲型法系威胁 |

动作权重规则：

1. 只允许在**现有** action catalog 内重配权重。
2. 不允许为了追求差异额外引入私有技能。

## 5. 推荐改动面

### 5.1 `game`

1. `game/src/main/resources/data/elites/index.yaml`
2. `game/src/main/resources/data/boss-variants/index.yaml`
3. `game/src/main/resources/data/talents/index.yaml`
4. `game/src/main/resources/i18n/zh-CN.json`
5. `game/src/main/resources/i18n/en-US.json`

### 5.2 `tools`

1. `BossHarnessTest` white-box summary
2. `phase4Report` metrics 透传

### 5.3 `client`

只消费 manifest / key，不单独加私有 mutation 表。

## 6. 测试与自证

### 6.1 自动化命令

```bash
./gradlew :game:test
./gradlew bossHarness
./gradlew terrainInteractionBatch
./gradlew whiteBoxVerify
./gradlew phase4Report
```

### 6.2 必测行为

1. mutation 数量、tier 分布、pair 数达标
2. 三个 Boss variant 组合两两不同
3. 不存在失效的 `allowedZones / incompatibleWith / grantedTalents`

## 7. 资源生成计划

### 7.1 图片

1. 计划文件：`assets-src/image/specs/phase4-opt-pr02-gemini-plan.yaml`
2. 覆盖对象：
   - `icon.mutation.ironhide`
   - `icon.mutation.phase_runner`
   - `icon.mutation.war_caller`
   - `icon.mutation.corrosion_cloud`
   - `icon.mutation.frostbound`
   - `icon.mutation.void_mirror`
3. 报告文件：
   - `assets-src/image/manifests/phase4-opt-pr02-generation-report.jsonl`
   - `assets-src/image/manifests/phase4-opt-pr02-processing-report.jsonl`
4. 管线固定为：

```bash
GEMINI_API_KEY=your_key \
GEMINI_CONCURRENCY=4 \
./scripts/generate_assets.sh \
  assets-src/image/specs/phase4-opt-pr02-gemini-plan.yaml \
  assets-src/image/raw/generated \
  assets-src/image/manifests/phase4-opt-pr02-generation-report.jsonl
```

### 7.2 音频

1. 计划文件：`assets-src/audio/specs/phase4-opt-pr02-audio-plan.yaml`
2. 生成参数文件：`assets-src/audio/specs/phase4-opt-pr02-audio-generation-plan.yaml`
3. 覆盖对象：
   - `audio.mutation.*`
4. 报告文件：
   - `assets-src/audio/manifests/phase4-opt-pr02-generation-report.jsonl`
   - `assets-src/audio/manifests/phase4-opt-pr02-processing-report.jsonl`
5. `generate_opt_pr02_audio.py` 必须直接接受正式 `audio-plan`；如需显式 profile/duration 覆写，可改传 `phase4-opt-pr02-audio-generation-plan.yaml`
6. 管线固定为：

```bash
python3 scripts/generate_opt_pr02_audio.py \
  --plan assets-src/audio/specs/phase4-opt-pr02-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-pr02-generation-report.jsonl

python3 scripts/process_audio.py \
  --filter-plan assets-src/audio/specs/phase4-opt-pr02-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-pr02-processing-report.jsonl
```

### 7.3 约束

1. dedicated elite talent 默认复用 mutation 的 icon/cue 或现有 telegraph 资源，不为内部 monster-only talent 再平铺一套玩家向资源。
2. 新资源必须进入 base runtime manifest，不允许藏在私有表。

## 8. 出口门禁

1. `eliteMutationDistinctCount >= 12`
2. `eliteMutationValidPairCount >= 40`
3. `mutationTierDistribution` 达到 `MINOR >= 2, MAJOR >= 5, SIGNATURE >= 2`
4. 三个 Boss variant 的 mutation 组合两两不同
5. mutation i18n / visual / audio key 全部可解析

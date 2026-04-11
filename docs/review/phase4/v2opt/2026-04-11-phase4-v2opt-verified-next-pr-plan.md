# Phase 4 V2OPT 核实后续 PR 计划

- 日期：`2026-04-11`
- 目的：对 `docs/review/phase4/codex/` 与 `docs/review/phase4/v2/` 两条深度审查路径做二次核实，剔除失效/误判项，只保留当前 tree、YAML、harness 报告和 Phase 4 权威文档共同支持的问题，再重排为下一轮真正值得做的 `PR` 级开发计划。
- 参考基线：
  - `docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md`
  - `docs/phase4/roadmap.md`
  - `docs/phase4/2026-03-13-phase4-verification-checklist.md`
  - `docs/phase4/2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md`
  - `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md`

---

## 1. 直接结论

当前最值得继续投入的，不是再开一轮“大而全”的 `opt PR-07`，也不是继续追 `v2` 报告里那条“11 个 zone 全量迁移”的误判，而是把已经被第一轮大优化放大的 4 类长期风险收口：

1. **成长身份仍被单一武器底盘吞并**  
   `build/reports/harness/long-run-full.md` 的 `12` 个 full-route case，四个职业最终主武器全部收敛到 `battle_axe`。这不是数值小瑕疵，而是成长/掉落系统正在系统性抹平职业身份。
2. **隐藏内容目前证明了 correctness，没有证明 organic experience**  
   `HiddenContentHarnessRunner` 仍然以 `primerAction` 驱动 scripted 场景；`phase4-summary.md` 也保留了 `hidden.primer.*` 证据。当前 `1.0` 发现率不能再当成“自然发现闭环成立”。
3. **奖励池全局平均分化合格，但局部关键 reward channel 仍高度同质**  
   `phase4-summary.md` 中仍有 `loot.abyssal_temple.cadence ↔ loot.abyssal_temple_warded_archive.secret = 0.818`、`loot.deep_iron_pit.reward ↔ loot.deep_iron_* secret > 0.90` 这类高重叠。玩家记住的是这些局部高价值通道，不是 corpus average。
4. **地形/精英体系已经接上，但还没完全形成稳定可感知的战术语言**  
   当前 `preferredTerrainTagsSeen = ["WATER"]`，`greenwood_fringe` 的 mapgen 差异度只是门槛线，`grey_crown` 仍缺 terrain flavor。问题不在“系统不存在”，而在“关键体验点仍偏弱或口径不够硬”。

结论上，下一轮优化不应再按旧的 `OPT-PR-01~06` 思路线性补量，而应进入第二阶段 follow-up：**先修 gate，再修成长身份，再修 hidden/reward 体验，再补 terrain/mutation 语义，最后做前台可感知性和前期重复游玩抛光。**

---

## 2. 核实结果

### 2.1 高置信成立的问题

| 主题 | 结论 | 证据 | 计划含义 |
| --- | --- | --- | --- |
| 终盘装备收敛 | **成立** | `long-run-full.md` 的 full-route 共 `12` 条样本，`arcanist / rogue / templar / vanguard` 最终主武器全部是 `battle_axe` | 这是下一轮最高优先级问题，必须进入掉落分发和 base item 底盘重构 |
| hidden 体验口径失真 | **成立** | `HiddenContentHarnessRunner.kt` 明确存在 `primerAction`；`phase4-summary.md` 也记录 `hidden.primer.*` | 需要把 scripted correctness 与 organic experience 分离，先改 gate 再改内容 |
| 局部 reward/secret 同质化 | **成立** | `phase4-summary.md` 的局部 overlap 仍有 `0.818 / 0.909 / 0.916` | 需要从“average overlap”升级为“pairwise local guardrail + secret identity” |
| greenwood 重复游玩辨识度偏弱 | **成立** | `whitebox-mapgen-summary.json` 中 `greenwood_fringe` 只有 `distinctPatternRoomCount=1`、`distinctEntranceLayoutCount=1`、`differenceCategoryCount=3` | 这是前 30 分钟体验的真实短板，但优先级低于成长/hidden/reward 主线 |
| terrain / mutation 语义仍偏弱 | **成立** | `preferredTerrainTagsSeen=["WATER"]`；`EncounterDecorationService.kt` 通过注入 `elite` tag 保底匹配；`boss.variant.grey_crown` 的两个 granted mutation 均无 `preferredTerrainTags` | 需要把“能跑”提升成“语义稳定、指标可解释、主题不被稀释” |

### 2.2 只部分成立的问题

| 主题 | 结论 | 说明 |
| --- | --- | --- |
| `applyToTags` 针对性被稀释 | **部分成立** | 注入 `elite` 解决了 runtime 断链，但也让所有带 `elite` 的 mutation 先天能过第一层过滤。问题不是“实现错误”，而是“主题约束太软”。应进后续收口 PR，而不是回滚到旧状态。 |
| greenwood terrain encounter 偏低 | **部分成立** | `greenwood_fringe` 仍是体感最弱的 combat-sampled zone，但当前 `terrainInteractionBatch` 已是针对“可稳定产生常规遭遇”的采样集，不应直接把它扩写成“所有升级 zone 都必须同口径采样”。真正要修的是 per-zone lower bound 和 greenwood 自身内容。 |
| grey_crown terrain flavor 缺失 | **部分成立** | 这是 boss 主题完成度问题，不是 contract 断裂。优先级应低于成长身份、hidden 和 reward 身份问题。 |

### 2.3 已失效、降级或不应继续推进的问题

| 原审查结论 | 现在的判断 | 依据 |
| --- | --- | --- |
| “Phase 4 必须覆盖全部 11 个 zone，7 个 `mapgenProfileId` 缺失是 P0” | **降级为误判** | `docs/phase4/2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md` 已明确：Phase 4 正式升级的就是 `greenwood_fringe / deep_iron_pit / underground_river / abyssal_temple` 四个 zone。 |
| “runtime 不支持 `APPEND / DENY` 是 Phase 4 未完成” | **不成立** | Phase 4 权威口径本来就是 runtime 主路径只保证 `ADD + whole-entry REPLACE`，`APPEND / DENY` 停在 fixture/lint 层，除非后续单独冻结 allowed-target。 |
| “`corrosion_cloud` / `dread_aura` 的 aura 只挂 emitter 不生效” | **已修复** | `FoundationGameSessionTest.kt` 已直接断言 `ARMOR_BREAK` 出现在玩家 `EffectTracker` 中；运行时也已有 `applyCarrierStatusEffect(...)` 路径。 |
| “没有 ICE 地形词缀” | **不成立** | `items/index.yaml` 已存在 `TerrainAffinityBonus` 的 `ICE` 条目。 |
| “`phase4Report` 体验 metric FAIL 不会进 gate” | **已修复** | `Phase4ReportRunner.kt` 已引入 `failedExperienceMetricCount` 和 `failedGateCount`。 |

---

## 3. 重新排序原则

这轮计划按以下顺序排：

1. **先修 owner gate，避免继续被错误口径带节奏。**
2. **再修成长身份，因为这是最深的长期结构债。**
3. **再修 hidden/reward 的真实体验闭环，因为这两条直接决定 Phase 4 的 replay hook。**
4. **terrain / mutation 放到第四位，只修真正影响语义稳定和主题感的问题，不再扩大战线。**
5. **前台反馈和前期 replay 抛光最后做。**

额外约束：

1. **不保兼容。** 允许破坏式调整 loot 分发、base item 权重、phase4Report 指标和阈值，只要长期语义更干净。
2. **不回到“先加内容数量再看体验”的旧路。** 后续 PR 必须每个都带 owner metric。
3. **不再把 Phase 4 的 follow-up 和 Phase 5 的大系统混在一起。** content pack runtime op、正式 death analysis、更多 boss variant 数量扩张都不在本轮主计划里。
4. **`V2OPT-PR-01 ~ 05` 全部不再开新图片/音频生成批次。** 当前 repo 已有 `Phase4 PR05/06/07` 与 `OPT PR02/05` 落下的 item、terrain/mutation、hidden/secret canonical 资源族；这轮 follow-up 只允许复用、接线和前台化，不再新增一套 raw asset plan。

---

## 4. 下一轮 PR 计划

> 新编号统一采用 `V2OPT-PR-01 ~ V2OPT-PR-05`，避免与已完成的 `OPT-PR-01 ~ OPT-PR-06` 混淆。

### V2OPT-PR-01：体验 Gate 校正与 Owner Metric 硬化

**优先级**：`P0`  
**目标**：把当前仍然“看起来全绿、但体验结论会失真”的 gate 先修正。

**为什么第一位做**

1. 如果不先修 gate，后面所有优化都会继续被 scripted hidden、average overlap 和 aggregate-only terrain 指标掩盖。
2. 这是最小破坏、最高杠杆的 PR，能为后续四个 PR 提供统一验收口径。

**范围**

1. `tools/src/main/kotlin/com/ktome/tools/phase4/Phase4ReportRunner.kt`
2. `tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt`
3. `tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt`
4. `build/reports/harness/long-run-full.md` 的 owner metric 生成链
5. `game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt` 的指标说明与 per-zone 下限

**核心改动**

1. 把 hidden 指标拆成两条：
   - `scriptedHiddenVerificationRate`
   - `organicHiddenDiscoveryRate`
2. 给 loot 增加局部 guardrail，而不是只看 corpus average：
   - `sameZoneSecretVsCadenceMaxOverlap`
   - `sameZoneSecretVsRewardMaxOverlap`
3. 给 long-run 增加成长身份指标：
   - `professionAlignedWeaponAdoptionRate`
   - `terminalWeaponBaseDiversity`
   - `crossProfessionTopWeaponDominance`
4. 给 terrain 增加更合理的口径约束：
   - 保留 aggregate gate
   - 增加 combat-sampled zone 的 `perZoneEncounterLowerBound`
   - 明确 `crystal_cavern` 为 combat sample、不是“升级 zone 全覆盖证明”
5. 把所有体验指标的 owner、公式、阈值和 note 收进 `phase4-summary.md/json`，禁止继续靠审查文本口头解释。

**建议验收**

1. `phase4-summary.md` 明确把 hidden 的 scripted 与 organic 分成两列。
2. `phase4-summary.md` 新增 terminal build identity 段。
3. `phase4-summary.md` 新增 local reward overlap 段。
4. `terrainInteractionBatch` 的 markdown 或 JSON 里明确写出 combat-sampled zone 列表与排除理由。

**推荐命令**

```bash
./gradlew whiteBoxLoot
./gradlew whiteBoxHiddenContent
./gradlew terrainInteractionBatch
./gradlew longRunLab
./gradlew phase4Report
```

**完成后再开下一 PR 的条件**

1. 后续四个 PR 的验收指标都已固定，不再临时改口。
2. 再也不会出现“脚本化 100% 发现率被误当成自然体验成立”的情况。

---

### V2OPT-PR-02：成长身份救火与职业化掉落分发

**优先级**：`P0`  
**目标**：阻止 `battle_axe` 继续吞并四职业长局终盘身份。

**为什么第二位做**

1. 这是当前最重的长期风险；不修它，后续扩再多 hidden/terrain/reward 都会落回同一套武器底盘。
2. 这类问题越晚修，越会污染 `Phase 5` 的 AI、平衡和 QA 基线。

**范围**

1. `game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt`
2. `game/src/main/resources/data/loot/index.yaml`
3. `game/src/main/resources/data/items/index.yaml`
4. `build/reports/harness/long-run-full.md`
5. 必要时 `SmokeBot.kt` 与 long-run 构筑评估逻辑

**核心改动**

1. 在掉落候选池层引入最小 profession/build context：
   - profession
   - resource axis
   - committed talent tags
   - 当前主副手 archetype
2. 下调 `battle_axe` 的“跨职业通吃”能力，方式优先级：
   - 先改分发，再改底盘
   - 再改 typeWeights / slotBias
   - 最后才动 baseAttack
3. 给 `arcane_staff / short_sword / hunter_bow / long_sword` 补足职业专属收益，而不是只堆面板数值。
4. 扩 faction/tag build surface：
   - 把 `DamageVsTag` 从 `bandit / undead` 扩到主要后续阵营
   - 优先服务 long-run 中真正出现的职业/zone 组合
5. 让 unique/artifact 的职业化采用进入长期 owner metric，而不是只看“meaningful swap rate”。

**建议验收**

1. full-route 样本中，不允许再出现 `12/12` 单一主武器收敛。
2. `professionAlignedWeaponAdoptionRate` 达到预设阈值。
3. `crossProfessionTopWeaponDominance` 明显下降。
4. `lootBalanceLab`、`soloClearLab`、`longRunLab` 不出现整体胜率崩塌。

**推荐命令**

```bash
./gradlew lootBalanceLab
./gradlew soloClearLab
./gradlew longRunLab
./gradlew phase4Report
```

**风险**

1. 这会触碰数值主链，必须把 owner metric 做成第一优先。
2. 不建议把这类修正拆成纯 YAML 小补丁；需要明确承认这是分发合同升级。

---

### V2OPT-PR-03：Secret Reward 身份重建与 Organic Hidden 闭环

**优先级**：`P0`  
**目标**：把 hidden 从“可验证系统”推进到“玩家值得主动搜索的系统”。

**为什么第三位做**

1. 当前 hidden 的主要问题不是数量，而是 organic discovery 与 reward identity 都没有真正成立。
2. 这个 PR 只有在 `V2OPT-PR-01` 把 gate 拆干净后才有意义。

**范围**

1. `game/src/main/resources/data/loot/index.yaml`
2. `game/src/main/resources/data/events/index.yaml`
3. `game/src/main/resources/data/secret-zones/index.yaml`
4. `tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt`
5. 必要时 `client` 最小提示路径

**核心改动**

1. 先清掉最糟糕的同质化对：
   - `abyssal_temple.cadence ↔ abyssal_temple_warded_archive.secret`
   - `deep_iron_pit.reward ↔ deep_iron_* secret`
   - 其他 same-zone secret/reward 高重叠对
2. secret profile 明确拥有自己的身份，而不是“更大的普通奖励”：
   - 专属 base item
   - 专属 slot bias
   - 专属 tag identity
   - 或者 `LOOT_PROFILE + BUFF / ENCOUNTER / SERVICE` 的混合奖励结构
3. 保留 existing primer contract，但增加 organic 路径：
   - 无 primer 时的发现率
   - 搜索动作使用率
   - 发现后 opt-in 进入 secret zone 的转化率
4. 给 primer 和 search 行为最小前台提示：
   - 不做大 UI
   - 只做足够低摩擦的提示和 reward 反馈

**建议验收**

1. scripted hidden 与 organic hidden 两条指标都存在。
2. organic hidden 至少达到现有 checklist 对 hidden 的最低体验级阈值，不再由 primer 造假。
3. same-zone secret vs non-secret overlap 全部降到 guardrail 以内。
4. 玩家进入 secret zone 后，奖励身份可被 report 与人工白盒同时解释。

**推荐命令**

```bash
./gradlew hiddenContentHarness
./gradlew whiteBoxHiddenContent
./gradlew whiteBoxLoot
./gradlew phase4Report
```

---

### V2OPT-PR-04：Terrain / Mutation 语义收口与主题防稀释

**优先级**：`P1`  
**目标**：不再扩大战线，只把当前已经存在的 terrain / elite / boss 体系做实。

**为什么第四位做**

1. terrain / mutation 现在的问题更多是“表达和口径仍偏弱”，不是“系统不存在”。
2. 相比成长身份和 hidden/reward，它对长期结构的伤害略小，但仍值得在 Phase 4 follow-up 内做完。

**范围**

1. `game/src/main/kotlin/com/ktome/game/elites/EncounterDecorationService.kt`
2. `game/src/main/resources/data/elites/index.yaml`
3. `game/src/main/resources/data/boss-variants/index.yaml`
4. `game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt`
5. `tools/build/reports/phase4/whitebox/terrain/*`

**核心改动**

1. 明确 combat-sampled zone 与 upgraded zone 的边界，避免继续把 scope 争论写成 bug。
2. 修 `applyToTags` 主题约束过软的问题：
   - `elite` 注入保留
   - 但对声明了更具体 tag 的 mutation 加上更强的二级约束或加权
3. 处理 `grey_crown` 的 terrain flavor 缺口。
4. 把 `preferredTerrainTagsSeen` 从只观测到 `WATER` 推到完整覆盖，至少不再长期卡在单维度。
5. 给 `greenwood_fringe` 单独调优，而不是继续靠 aggregate pass 掩盖。

**建议验收**

1. `preferredTerrainTagsSeen` 不再只剩单维度。
2. greenwood 具备稳定可观测的第二个 tactical hook。
3. grey_crown 具备明确主题，不再只是“Phase 3 boss + 两个普通 mutation”。
4. 不因为修 terrain/mutation 约束而把 valid pair 数打回不可接受区间。

**推荐命令**

```bash
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew phase4Report
```

---

### V2OPT-PR-05：前 30 分钟 Replay Hook 与 Frontstage Readability 抛光

**优先级**：`P2`  
**目标**：把当前“能查到”的战术/探索信息，推到更前台；同时补强开局 zone 的重复游玩辨识度。

**为什么最后做**

1. 这是价值很高的抛光，但在成长身份和 hidden/reward 主线没有修好之前，收益不如前四个 PR 硬。
2. 它非常适合在 `V2OPT-PR-01 ~ 04` 都稳定之后，做一次低风险收尾。

**范围**

1. `tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-summary.json`
2. `game/src/main/resources/data/mapgen/*`
3. `client/src/main/kotlin/com/ktome/client/render/*`
4. `game/src/main/resources/i18n/*.json`

**核心改动**

1. 让 `greenwood_fringe` 从“刚好过线”提升到“明显有重开辨识度”：
   - 新 pattern room
   - 新 entrance layout family
   - 更清晰的 early hidden signal
2. 把 mutation / terrain / passive / search 的关键信息从 inspect/log 拉一层到前台：
   - 目标摘要
   - 地形危险/收益摘要
   - secret/search 成功反馈
3. 保持最小 UI 改动，不开新系统。

**建议验收**

1. `greenwood_fringe` 的 `distinctPatternRoomCount`、`distinctEntranceLayoutCount`、`differenceCategoryCount` 全部抬高。
2. 手工白盒能更早感知 terrain、mutation、hidden 的决策价值。
3. 不引入屏幕噪音或信息过载。

**推荐命令**

```bash
./gradlew whiteBoxMapgen
./gradlew clientSmoke
./gradlew goldenScreenshot
./gradlew phase4Report
```

---

## 5. 推荐执行顺序

```text
V2OPT-PR-01  Gate 校正
    ↓
V2OPT-PR-02  成长身份 / 职业化掉落
    ↓
V2OPT-PR-03  Secret reward + organic hidden
    ↓
V2OPT-PR-04  Terrain / mutation 语义收口
    ↓
V2OPT-PR-05  前 30 分钟 replay hook + 前台可感知性
```

不建议改成：

1. 先做 terrain 收尾，再回头修成长身份。
2. 先继续堆 hidden/event 数量，再补 organic gate。
3. 直接开一个大而全的 `opt PR-07` 把所有事情重新打包。

---

## 6. 本轮不纳入计划的事项

以下项保留登记，但不进入本轮 follow-up 主计划：

1. content pack runtime `APPEND / DENY`
2. “11 zone 全量 HybridTopology 迁移”
3. 更多 boss variant 数量扩张
4. 完整 death analysis / replay diagnosis 平台
5. 更大规模 content pack 作者生态工具

原因很简单：它们都不如前述五个 PR 更直接决定 `Phase 4` 的长期乐趣地基。

---

## 7. 一句话收束

第一轮大优化已经把 `Phase 4` 从“系统成立”推到了“游戏开始成立”，下一轮不该再追求面更广，而该追求**把长期最贵的四个问题一次修准：成长身份、organic hidden、reward identity、terrain/mutation 主题稳定性**。  
如果只能开一个 PR，就先开 `V2OPT-PR-01`；如果能连开两到三个，就必须把 `V2OPT-PR-02` 和 `V2OPT-PR-03` 紧跟上去。

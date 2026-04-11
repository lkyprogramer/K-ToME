# phase4 深度审查报告（Part 3/4）

## 5. 当前 Phase4 最需要解决的关键问题

### 5.1 职业幻想被装备底盘收敛压扁

**问题描述**

当前最严重的问题不是职业技能树没差异，而是职业在长局中被奖励体系持续推向同一类高效武器底盘，导致“职业不同，终盘手感却越来越像”。

**证据**

- `build/reports/harness/long-run-full.md` 中多个 full-route run 的终盘 `buildHash` 收敛到 `WEAPON:battle_axe`，涉及 `vanguard`、`arcanist`、`templar`、`rogue`。
- 同一份报告显示 `affixSynergyActivationCount=6`，且 `affixSynergyActivationDistribution={of_shadow=6}`，说明构筑协同虽然存在，但真实激活面很窄。
- `game/src/main/kotlin/com/ktome/game/harness/SmokeBot.kt` 已对职业武器做偏好打分，但结果仍然收敛，证明并非纯 bot 选择错误。
- `game/src/main/resources/data/items/index.yaml` 中 `battle_axe.baseAttack=11`，显著高于 `short_sword=5`、`long_sword=8`、`arcane_staff=4`。
- `game/src/main/kotlin/com/ktome/game/loot/LootProfileCandidatePoolResolver.kt` 在 base item pool 层只看 `itemIds`、`itemTagFilter`、`excludeIds`、`typeWeights`、`slotBias`，没有 profession/build-aware 基底筛选。

**为什么这是当前必须解决的问题**

因为这不是“平衡性微调”，而是**成长系统的主轴问题**。Phase4 的 long-run、loot differentiation、artifact/passive vocabulary 都已经做出来了，如果底盘仍然把不同职业收敛到同一武器逻辑，那么后续继续扩内容，只会把错误的奖励偏好放大。

**不解决的后果**

- 后续职业、内容、敌人越多，越容易变成“表面职业很多，底层打法还是同一套”。
- 玩家会把掉落体验理解成“找更大的攻击数字”，而不是“找更适合我 build 的武器语义”。
- Phase5 再去做 AI、stability、release，会把一个已经偏平的职业生态直接冻结下来。

### 5.2 隐藏内容的自然发现闭环没有被当前验收真正证明

**问题描述**

当前 hidden content 的合同闭环做得很完整，但“玩家是否会自然发现、并因此更愿意继续探索”并没有被当前报告真实回答。

**证据**

- `tools/build/reports/phase4/hidden/hidden-content-summary.json` 与 `tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-report.md` 显示 `hiddenEventTriggerRate=1.0`、`secretZoneDiscoveryRate=1.0`。
- 但 `tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt` 明确把验证拆成固定 scenario：`FORCE_ELITE_KILL`、`OPEN_CRYSTAL_CACHE_CHEST`、`CLAIM_WARD_RELIQUARY`、`ENTER_HIDDEN_BRANCH_ROOM`，然后移动到指定搜索点执行 `PlayerCommand.Search`。
- `FoundationGameSession.kt` 中搜索、揭示、隐藏奖励执行路径是成立的，但这只能证明“给定条件时系统会工作”，不能证明“自然玩家行为下会形成驱动”。

**为什么这是当前必须解决的问题**

因为 hidden content 不是边缘调味，而是 Phase4 的主承诺之一。若当前只验证了合同，不验证自然发现率，就等于把“探索好不好玩”留在一个未测区。这个问题一旦拖到 Phase5，就会把错误的探索频率和错误的奖励感知一起带过去。

**不解决的后果**

- 你会误以为 hidden 系统已经完成，但玩家实际可能很少自然触发。
- Secret Zone 的开发成本可能高于它在真实体验中的存在感。
- 后续继续加 hidden content，只会增加内容量，不会增加发现乐趣。

### 5.3 奖励池局部高重叠，秘密奖励辨识度不足

**问题描述**

当前 loot profile 体系在全局上已经有分化，但某些关键 reward/secret pool 之间仍然过于相似，秘密奖励容易沦为“同类掉落的预算增强版”。

**证据**

- `tools/build/reports/phase4/phase4-summary.md` 中 `lootProfileBaseItemOverlapMatrix` 的平均 overlap 只有 `0.205`，整体达标。
- 但 `tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json` 显示：
  - `loot.deep_iron_pit.reward` 与 `loot.deep_iron_slag_cache.secret` overlap 为 `0.9166666666666666`
  - `loot.deep_iron_pit.reward` 与 `loot.deep_iron_smuggler_stash.secret` overlap 为 `0.9090909090909091`
  - `loot.deep_iron_pit.cadence` 与 `loot.deep_iron_slag_cache.secret` overlap 为 `0.9090909090909091`

**为什么这是当前必须解决的问题**

因为奖励身份感是 Phase4 探索价值的一部分，不是后续可有可无的 polish。当前如果 secret reward 只是“主线 reward 的近似副本”，玩家就不会把风险、搜索、绕路和发现感绑定成正反馈。

**不解决的后果**

- 隐藏内容会有系统价值，但没有情绪价值。
- 掉落表面看很多，玩家记忆里却只剩“掉的都差不多”。
- 后续再往这些 pool 里继续加内容，可能只是在扩大同质化池子。

### 5.4 构筑协同被做出来了，但真实激活面仍然偏窄

**问题描述**

当前 affix/passive vocabulary 已经做出来，但真实长局里真正被激活并形成 build identity 的协同仍然不够宽。

**证据**

- `tools/build/reports/phase4/phase4-summary.md` 中 `affixCount=78`、`uniqueTemplateCount=20`、`artifactTemplateCount=8`，说明词汇量已经够用。
- `build/reports/harness/long-run-full.md` 中 `synergyAffixRewardCount=122`、`synergyAffixAdoptionCount=52`，说明系统在发协同装备。
- 但同一份报告里 `affixSynergyActivationCount=6`，且 `affixSynergyActivationDistribution={of_shadow=6}`，显示真实成形的 endgame synergy 仍然过窄。

**为什么这是当前必须解决的问题**

因为这说明当前“装备词汇很多”不等于“玩家能稳定感到 build 在分化”。这是 Phase4 已经承诺的成长乐趣问题，不是 Phase5 才需要回答的问题。

**不解决的后果**

- 玩家会看见很多 affix 名称，但实际只记住少数几个真正有用的套路。
- 内容扩张越多，垃圾词汇感越强。
- 装备系统的复杂度会先增长，乐趣增量却跟不上。

### 5.5 前期探索差异度只是过线，不是强成立

**问题描述**

当前前期 zone 中至少有一块的重复游玩差异度只是“合格”，还没有到“足以成为早期 replay hook”的程度。

**证据**

- `tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-summary.json` 中 `greenwood_fringe` 的：
  - `distinctPatternRoomCount=1`
  - `distinctEntranceLayoutCount=1`
  - `differenceCategoryCount=3`
- 这刚好卡在“至少 3 个 perceptible difference categories”的门槛线上。

**为什么这是当前必须解决的问题**

因为早期 10~20 分钟的体验，决定玩家愿不愿意继续开下一局。前期 zone 如果只是“技术上有差异”，而不是“玩家能明显感觉每局不一样”，重复游玩的第一印象会被削弱。

**不解决的后果**

- 前期 replay hook 不够强。
- 玩家会把重复游玩的新鲜感更多寄托在中后期系统，而不是一开局就被勾住。
- 后续内容越多，越会放大“前段弱，后段强”的节奏落差。

## 6. 优化建议（按优先级）

以下建议都以“当前 Phase4 就应该做”的尺度提出，而不是把问题偷渡给 Phase5。

### P0：必须立刻处理

#### P0-1 重建职业感知的装备底盘分发

- 问题本质：当前掉落系统已经有词汇分化，但 base item 层没有稳定保住职业底盘，导致不同职业被高基础攻击武器吞并。
- 影响范围：`game` 掉落系统、物品基底数值、长局奖励节奏、职业 build identity、相关白盒指标。
- 优化目标：让不同职业在长局终盘仍更倾向于站在各自“合理而强”的武器/副手/护甲组合上，而不是被单一高效底盘强行收敛。
- 具体改法：
  - 在 `LootProfileCandidatePoolResolver.kt` 上层或其调用链引入 profession/build context，把候选 base item pool 从“只看静态 profile”提升到“静态 profile + 当前职业/资源轴/已选 talent 标签”。
  - 调整 `items/index.yaml` 中核心武器底盘，让 `battle_axe` 不再单靠 `baseAttack` 把其他职业武器压成次优；`arcane_staff`、`short_sword`、`hunter_bow`、`long_sword` 应有更强的职业语义收益，而不是只靠 affix 挽救。
  - 对 `loot/index.yaml` 中主线 cadence/reward profile 的 `typeWeights` 与 `slotBias` 做去全局化处理，减少“先发大量 WEAPON，再用底盘胜负决定一切”的倾向。
  - 把 `long-run-full.md` 的终盘装备分布纳入 owner-gate，至少新增“不同 profession full-route 终盘 base weapon 去重度 / profession-aligned adoption rate”指标。
- 预期收益：职业幻想更稳，奖励更像 build 选择而不是简单 DPS 升级，重复游玩动力显著增强。
- 可能副作用 / 风险：会牵动怪物强度、Boss 时间到击杀、artifact 价值排序，需要同步回归 `soloClearLab`、`longRunLab`、`bossHarness`。
- 需要同步修改：文档、数值、掉落配置、体验指标、必要时补 `tools` 报告口径。

#### P0-2 把“自然发现率”从隐藏系统的正式体验指标中补上

- 问题本质：当前 hidden metrics 证明的是合同闭环，不是玩家体验闭环。
- 影响范围：`tools` 体验报告、隐藏内容验收口径、搜索行为权重、secret zone 实际存在感。
- 优化目标：区分“脚本化可验证发现”与“自然游玩可感知发现”，防止阶段验收被假阳性掩盖。
- 具体改法：
  - 保留现有 `HiddenContentHarnessRunner.kt` 作为 correctness harness，但在 `phase4-summary` 中单独标注它是 scripted verification，不再把 `1.0 discovery rate` 直接当体验成立证据。
  - 新增一条 organic hidden run 指标链，基于 `headless-smoke` / `long-run` / 新的 full-route 探索 bot 采集：
    - 无脚本 primer 的 hidden discovery rate
    - first discovery turn / zone
    - secret zone opt-in rate
    - search action 使用频率与收益率
  - 若自然发现率过低，不先加更多 hidden content，而是先调入口密度、搜索可读性、奖励显著性。
- 预期收益：能真正回答“hidden 系统现在是否好玩”，避免把系统存在当成体验成立。
- 可能副作用 / 风险：新指标可能会直接暴露现有探索动力不足，短期内会让 Phase4 报表变难看，但这是必要的真相。
- 需要同步修改：`tools`、phase4 报表文档、可能的搜索 UI/提示与数据配置。

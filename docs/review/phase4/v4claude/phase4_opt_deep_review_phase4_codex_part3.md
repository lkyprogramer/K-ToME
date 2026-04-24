# Phase 4 深度玩法体验审阅 — Part 3：关键问题清单（按严重程度排序）

本 Part 将 Part 1 一致性矩阵与 Part 2 七维体验总评中暴露的问题统一抽成"关键问题清单"。每条问题采用固定 6 段式：
**严重级别 / 现状证据 / 可能原因 / 影响面（谁会被影响 / 影响哪条玩法回路） / 是否伪完成 / 触达/复现路径**。问题编号 `ISSUE-0XX` 用于在 Part 4 的修订方案中引用。

严重级别定义：

- **P0**：阻塞 Phase 4 收口，会直接伤到"重复游玩差异明显"这一核心卖点，必须在 Phase 4 内修复。
- **P1**：不阻塞 Phase 4 收口，但会放大"技术合同对 / 玩家感知错位"的剪刀差，必须在 Phase 4 内修复，不能延到 Phase 5。
- **P2**：细节问题或证据链补强问题，可在 Phase 4 收尾 PR 中顺手处理；若拖到 Phase 5 不会引发结构性回归。

---

## § P0 · 阻塞 Phase 4 收口（2 条）

### ISSUE-001：templar + vanguard 终盘武器撞车 long_sword，cross-profession dominance 压在阈值线

- **严重级别**：P0
- **现状证据**：
  - `tools/build/reports/verification/phase4/report-phase4-summary.md`：
    - `crossProfessionTopWeaponDominance: 50.0% (6/12) top=long_sword` — 阈值 `<= 50.0%`，**恰好压线**；
    - `terminalWeaponBaseDiversity: 3` — floor `>= 3`，**恰好压线**；
    - `topWeaponSemantics`：`templar=long_sword[item, weapon, melee, frontline, guard, discipline]; vanguard=long_sword[...]`。
  - `build/reports/harness/long-run-summary.md`：
    - vanguard full_route `buildHash=vanguard#human#WEAPON:long_sword:-:MITHRIL:ironroot+of_storms+warforged+of_piercing|...`；
    - templar full_route `buildHash=templar#human#WEAPON:long_sword:-:MITHRIL:sanctified+of_smite+warforged+of_strength|...`；
    - 两个职业在 BOSS/SUPPORT/ROUTE/CACHE milestone 上多次 adopted=true 的 WEAPON 都是 `long_sword`。
- **可能原因**：
  1. `long_sword` 的 semantic tags `[item, weapon, melee, frontline, guard, discipline]` 同时 match templar（`melee + frontline + discipline`）与 vanguard（`melee + frontline + guard`）的 profession-aligned weapon 语义，两职业都会"合法"地 adopt long_sword；
  2. vanguard 与 templar 的 capstone weapon 候选（vanguard: `unique_quenchbreaker_maul`；templar: 尚未在源数据中声明 terminal weapon-class capstone）未在长局中被 loot 分配到或未被 adopt 规则优先；
  3. `professionAlignedWeaponAdoptionRate = 100%` 把"aligned" 当成达标终点，但没有要求"每个 profession 的 aligned weapon 要彼此不同"。
- **影响面**：
  - **谁**：所有 4 个 canonical profession 中的两个（templar + vanguard），覆盖 50% 的职业池；
  - **哪条回路**：build identity → 成长 → 终盘——玩家在视觉层面看到 templar 和 vanguard 终盘都"挥长剑"，profession 辨识度被稀释；
  - **对核心卖点的影响**：直接冲击 Phase 4 主卖点"重复游玩差异明显"——在 4 个 profession 的重玩矩阵里，2 个职业的终盘武器趋同。
- **是否伪完成**：**是**。`crossProfessionTopWeaponDominance` 与 `terminalWeaponBaseDiversity` 都是"压在阈值上"的过线，不是"已经稳定分化"的通过。指标层 OK，玩家感知层不 OK。
- **复现路径**：
  1. 跑 `./gradlew longRunLab`，观察 full_route 采样中 templar 与 vanguard 的终盘 WEAPON；
  2. 或直接查 `build/reports/harness/long-run-summary.md` 中 vanguard 与 templar 的 `buildHash` 字段的 `WEAPON:` 段。

---

### ISSUE-002：arcanist / rogue 两个职业的 capstone adoption 为 0

- **严重级别**：P0
- **现状证据**：
  - phase4Report `professionCapstoneAdoptionFloor.reportOnly: 2/4` — 状态 `APPROVED_DEBT`；
  - 指标 note：`adoption=arcanist(0/1,samples=3), rogue(0/1,samples=3)`；
  - phase4Report `nonWeaponBuildPayoffFloor.reportOnly: 2/4` — `APPROVED_DEBT`，`nonWeapon=arcanist(0/1,samples=3), rogue(0/1,samples=3)`；
  - long-run arcanist full_route `buildHash`: `arcanist#human#WEAPON:arcane_staff:...|OFF_HAND:emerald_charm:...|ARMOR:unique_furnace_plate:...`——终盘 ARMOR 是 **vanguard 的 capstone**，arcanist 自己的 capstone `artifact_river_echo / unique_deepcurrent_lens` seen 但未 adopt；
  - long-run rogue full_route `buildHash`: `rogue#human#WEAPON:hunter_bow:...|OFF_HAND:basic_shield:...|ARMOR:leather_armor:...`——rogue 的 capstone `artifact_briar_heart / artifact_heartroot_gambit / unique_briarbound_bow / unique_thornpath_crook` seen 但未 adopt；milestone 中多次 `CACHE:artifact_briar_heart:OFF_HAND:before=basic_shield:final=basic_shield:adopted=false`。
- **可能原因**：
  1. **孤儿 capstone candidate**：secret zone 的 fixedItemIds 包含了 arcanist/rogue capstone（phase4Report §Local Reward Identity 已列出），但 secret zone 的 entry rate 在 arcanist/rogue 路线上可能更低；或 secret zone 出现在 objective 流的后段导致玩家 build 已定型不再换装；
  2. **Adopt 评估函数偏保守**：arcanist 持有 `arcane_staff` 时，对 OFF_HAND slot 上 artifact（`artifact_river_echo` / `unique_deepcurrent_lens`）的评分低于 `emerald_charm`（`warded+of_focus`），即使 capstone 的 tags 更契合；
  3. **Slot 兼容问题**：rogue 的 capstone `artifact_briar_heart` 为 OFF_HAND，但 rogue 倾向 `basic_shield` 作为 OFF_HAND 默认值，评分模型未对 capstone 赋予足够的"artifact 优先级"。
- **影响面**：
  - **谁**：arcanist + rogue（覆盖 50% 职业池）；
  - **哪条回路**：成长 → 终盘 → 重玩驱动——arcanist 和 rogue 玩家看不到自己的 profession capstone 兑现，profession fantasy 未闭环；
  - **对核心卖点的影响**：严重——V3 PR-02 明确承诺"每个 profession 都有稳定的 capstone 路径"。当前 2/4 的 APPROVED_DEBT 把承诺降级为"报告层 OK"。
- **是否伪完成**：**是 / 且是最严重的伪完成**。`professionCapstoneSeenRate=100%` 说明"玩家看到了" capstone，但"adopt=0" 说明"玩家没有真的用上"。这是最经典的"数据流穿透完整 / 用户行为未发生"的伪完成。
- **复现路径**：
  1. 跑 `./gradlew longRunLab`，查 arcanist 与 rogue 的 full_route `milestoneRewards` 序列，数 `adopted=true` 的 capstone item 条目；
  2. 检查 `game/src/main/resources/data/loot/index.yaml` 中 arcanist/rogue 相关 secret profile 的 fixedItemIds + weight；
  3. 在 loot picker 模块里 grep adopt 评估函数（大概率在 `core/.../loot/` 或 `game/.../item`）。

---

## § P1 · 必须在 Phase 4 内修复（5 条）

### ISSUE-003：milestone slot 分布严重偏 OFF_HAND/WEAPON，ARMOR 接近缺席

- **严重级别**：P1
- **现状证据**：
  - `build/reports/harness/long-run-summary.md`：`milestoneRewardSlotDistribution: {ARMOR=8, OFF_HAND=49, WEAPON=46}`；
  - 103 次 milestone 奖励中 ARMOR 仅占 7.8%，OFF_HAND=47.6%、WEAPON=44.7%；
  - 长局 6 full_route 中 5 个职业的终盘 ARMOR 要么是默认初始装（`leather_armor` / `apprentice_robe` / `chain_mail`），要么 across-profession 同装（vanguard + arcanist 都是 `unique_furnace_plate`）。
- **可能原因**：
  1. cadence / main reward profile 的 `slotBias` 权重向 WEAPON + OFF_HAND 倾斜，ARMOR 权重偏低；
  2. secret zone 的 `slotBias: ARMOR=1-3` 但 secret 触发密度本身就低（secretEntry=20.5%），ARMOR 曝光被进一步压缩；
  3. BOSS milestone 多次连续给 WEAPON 奖励，缺乏 slot-rotation 约束。
- **影响面**：
  - **谁**：所有职业，尤其 arcanist / rogue（自身没有 non-capstone ARMOR 候选）；
  - **哪条回路**：奖励 → 成长决策——ARMOR 维度无选择，build identity 的 3-slot 空间被压成 2-slot；
  - **对核心卖点的影响**：中等——奖励多样性被削弱，但不至于阻塞终盘达成。
- **是否伪完成**：**部分是**。`dynamicPoolCoverage=100%` 证明 pool 覆盖完整，但玩家实际看到的 slot 曝光严重偏斜；属于"分配层 OK / 展示层不 OK"的伪完成。
- **复现路径**：
  1. 跑 `./gradlew longRunLab`；
  2. 在 long-run-summary.md grep `milestoneRewardSlotDistribution`；
  3. 在 `game/src/main/resources/data/loot/index.yaml` 查各 profile 的 slotBias 权重。

---

### ISSUE-004：boss variant 源数据无 `phaseOverrides`，variant 体感 = actionWeight 微调

- **严重级别**：P1
- **现状证据**：
  - `game/src/main/resources/data/boss-variants/index.yaml`：3 个 variant（`molten_glass / grey_crown / abyssal_eclipse`），每个 2–4 个 `actionWeights` 条目，**无 `phaseOverrides` 字段**；
  - phase4Report `bossHarness`: `phaseTransitionObservedRatio=100% (6/6)` / `variantTraceDivergenceRatio=100% (3/3)` / `minVariantActionTraceDivergenceScore=1.000` / `bossVariantBasePhaseCountMin=2`；
  - `game/src/main/resources/data/bosses/index.yaml`: base bosses 有 `phase_full + phase_enraged/desperate`（molten_giant、dungeon_lord），`bandit_captain` 只有 `phase_full`；
  - `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-04-boss-phase-identity-and-version-discipline.md §6.1`：第一轮目标明确要求"通过 `phaseOverrides` 或等价阈值合同引入新 action pool"。
- **可能原因**：
  1. V3 PR-04 的 bossHarness 指标设计只要求"观察到 phase 过渡"与"trace divergence"，没有直接要求"variant 自身贡献 variant-level phase"；
  2. base boss 的 phase 结构足够满足 harness → variant 被允许只做 actionWeight 差分就拿到指标满分；
  3. `phaseOverrides` 合同可能已在 core/loader 侧实现但 source data 未填充。
- **影响面**：
  - **谁**：所有到终盘的玩家（即 100% 玩家）；
  - **哪条回路**：战斗 → 终盘 boss 体验——玩家遇到 variant 时感知是"这个 boss 换了点动作权重"而不是"这是一个不同的 boss 变体形态"；
  - **对核心卖点的影响**：中等偏高——V3 PR-04 的明确承诺之一是"不再只有血量和掉落"，当前状态 variant 的语言层面没达成。
- **是否伪完成**：**是**。`bossHarness` 4 项 100% PASS 的前提是"观察 trace 与 phase 有差"，而不是"variant 自己有阶段语言"。属于测量角度没覆盖承诺真实语义的伪完成。
- **复现路径**：
  1. 查 `game/src/main/resources/data/boss-variants/index.yaml`，grep `phaseOverrides`，预期返回 0；
  2. 跑 `./gradlew bossHarness`，phase4Report 会显示指标全绿；
  3. 对照 `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-04-boss-phase-identity-and-version-discipline.md §6.1` 的 phaseOverrides 要求。

---

### ISSUE-005：organic hidden lead / secret entry 分布严重错位

- **严重级别**：P1
- **现状证据**：
  - phase4Report `zoneDiscoveryDistribution`：`abyssal_temple=52.0%, greenwood_fringe=20.3%, deep_iron_pit=18.3%, underground_river=9.3%`；
  - phase4Report `secretZoneDiscoveryDistribution`：`greenwood_hidden_cache=38.9%, deep_iron_smuggler_stash=28.7%, abyssal_temple_warded_archive=18.5%, underground_river_crystal_rift=13.9%`；
  - aggregate `leadDiscoveryRate=46.6%` / `secretConversionRate=43.9%` 都 PASS，但分布极度倾斜；
  - `abyssal_temple` 的 lead 数量是 `underground_river` 的 5.6 倍，但其 secret entry 份额比 `greenwood_hidden_cache` 少一半。
- **可能原因**：
  1. `abyssal_temple` 是 objective-driven runtime（phase4Report note：`crystal_cavern remains in the combat-sampled set because ... abyssal_temple is excluded from combat-sampled terrain metrics because the current runtime is objective/pressure-driven`），objective 节奏让 lead 曝光密度高但 secret 入口可用窗口短；
  2. `greenwood_fringe` 有 `greenwood_hidden_cache` + `greenwood_ambush_hideout` 两个 secret profile 同时生效，且 `greenwood_hidden_cache` 的 reward 设计对 rogue 非常友好（fixedItemIds 含 `artifact_briar_heart / artifact_heartroot_gambit / unique_briarbound_bow / unique_thornpath_crook` 全套 rogue capstone），玩家 lead 转 secret 的动机强；
  3. organic probe 的 `probeBot=organic-hidden-probe-bot-v5` 在 abyssal_temple 的 objective 流中触发 search action 的时机可能被 objective hurry 压制。
- **影响面**：
  - **谁**：所有跑完整局的玩家；
  - **哪条回路**：探索 → 奖励——hidden 发现的体感被锁在两个 zone（greenwood + deep_iron），abyssal_temple 的 hidden 体感不兑现；
  - **对核心卖点的影响**：中等——影响重玩时的探索惊喜；间接放大 ISSUE-002（rogue capstone 集中在 greenwood 和 deep_iron，但这两个 zone 的 secret entry 高的是 rogue capstone 友好 zone，玩家反而更可能在 greenwood 拿到 rogue capstone 但 rogue adoption 还是 0/1——说明 adopt 评估函数本身也有问题，不是单纯曝光不足）。
- **是否伪完成**：**部分是**。aggregate OK 但 per-zone 分布失衡；属于指标设计"只看总量不看分布"的伪完成。
- **复现路径**：
  1. 跑 `./gradlew organicHiddenProbe` / `phase4Report`；
  2. 查 phase4-summary §Scripted vs Organic Hidden 的 `zoneDiscoveryDistribution` 与 `secretZoneDiscoveryDistribution`；
  3. 对比 abyssal_temple 的 lead/secret 比值与 greenwood_fringe 的 lead/secret 比值。

---

### ISSUE-006：long_run smoke corpus 主路线同构，branch 采样不足

- **严重级别**：P1
- **现状证据**：
  - `build/reports/harness/long-run-summary.md`：
    - `scenarioTypeDistribution: {full_route=6, branch_inclusive=1, route_probe=2, late_route_probe=2}`；
    - `zoneRouteHashDistribution`：hash `6871...4dbaaf=6` 独占 6 条 full_route；其余 hash 各 1 条；
  - 6 条 full_route 的 finalZone 全部 `abyssal_heart`，routeHash 高度集中到单一 hash。
- **可能原因**：
  1. `LONG_RUN_SMOKE` corpus 设计为确定性 smoke，seed 家族限制导致主路线收敛；
  2. branch_inclusive / route_probe 样本数偏少（1 + 2 + 2 = 5 条非主线样本），难以覆盖 Phase 4 要求的"5 局 3 差异"；
  3. corpus 在 V3 PR-01 关键路径节奏修订后未扩容 branch 采样。
- **影响面**：
  - **谁**：Phase 4 收口的证据链本身（不是玩家直接体感）；
  - **哪条回路**：证据链 → 审阅结论——smoke 的 topology/content 多样性在 owner metric 中无锚定；
  - **对核心卖点的影响**：低（玩家游戏内感知不到），但会让 Phase 4 "5 局 3 差异"的验证变成"aggregate 达标 / 路线多样性未被证明"。
- **是否伪完成**：**部分是**。corpus 小是工程现实，但 owner metric 没有 `zoneRouteHashDiversity` 或 `topologyCategoryDiversityPerSmokeRun` 之类的分布指标，属于验证层的伪完成（Phase 4 "重复游玩差异明显"的验证语言缺失）。
- **复现路径**：
  1. 跑 `./gradlew longRunLab`；
  2. 查 long-run-summary.md 的 `zoneRouteHashDistribution` 与 `scenarioTypeDistribution`；
  3. 对照 `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` 的"5 局 3 差异" 承诺。

---

### ISSUE-007：arcanist ARMOR 路径被 `unique_furnace_plate` 吞掉，armor identity 完全缺席

- **严重级别**：P1
- **现状证据**：
  - long-run arcanist full_route terminal ARMOR：`unique_furnace_plate:unique.furnace_plate:-:emberguard+of_fortitude` —— 这是 vanguard capstone；
  - `game/src/main/resources/data/` 下 arcanist 不存在专属 armor capstone（arcanist capstone 列表仅为 `artifact_river_echo / unique_deepcurrent_lens`，均为 OFF_HAND，无 ARMOR 项）；
  - milestone ARMOR 分布中 arcanist 本局的 ARMOR 反馈集中在 `unique_furnace_plate` / `apprentice_robe` 两档，缺少 arcanist-aligned 的 ARMOR 选择。
- **可能原因**：
  1. arcanist 的 capstone 列表设计时没有为 ARMOR slot 安排 profession-favored capstone；
  2. `unique_furnace_plate` 的 `specialTemplateTagPreference` 覆盖面过宽，不受 profession 限制；
  3. ARMOR milestone 出现率本身低（ISSUE-003），arcanist 的专属 armor 即便存在也难被曝光。
- **影响面**：
  - **谁**：arcanist（以及默认无 ARMOR capstone 的 rogue 也部分类似）；
  - **哪条回路**：成长 identity → 视觉差异——arcanist 与 vanguard 在 ARMOR slot 上撞装，进一步放大 ISSUE-001 的 identity 趋同；
  - **对核心卖点的影响**：中等——是 ISSUE-002 的姊妹问题，一起构成"arcanist identity 完全不成立"的合围。
- **是否伪完成**：**是**。arcanist 的 build identity 靠 `arcane_staff`（WEAPON）+ `emerald_charm`（OFF_HAND），ARMOR 是借用别家的 capstone。profession-aligned identity 只成立 1-2 个 slot，不是 Phase 4 承诺的 3-slot 空间。
- **复现路径**：同 ISSUE-002。

---

## § P2 · 细节或证据链补强（4 条）

### ISSUE-008：sample pack `flooded_relics` 大量使用 REPLACE ops，偏离 ADD-first 示范

- **严重级别**：P2
- **现状证据**：
  - `examples/content-packs/sample.flooded_relics/manifest.yaml`：version=1.0.0, gameVersionRange ">=0.4.0 <0.5.0" 对齐；
  - sample pack 内 hidden event 与 secret zone 多条以 REPLACE ops 形式覆盖主源（具体文件未逐条校验，但 Phase 4 设计明文强调 ADD-first）；
  - `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md` 与 PR-08 设计文档均明示 "ADD → MODIFY → REPLACE" 优先级。
- **可能原因**：作为官方 sample，可能为了"演示所有 ops 类型"而给 REPLACE 了过多权重，忽略了 sample 承担"示范最佳实践"的责任。
- **影响面**：
  - **谁**：下游 pack 作者；
  - **哪条回路**：pack 生态质量——下游 pack copy sample 姿势，REPLACE 模式扩散会让 pack 之间难以叠加；
  - **对核心卖点的影响**：低（不伤玩家当下体验），但长期 pack 生态质量会受损。
- **是否伪完成**：**是（示范维度）**。sample 加载 OK，但承担的"示范责任" 没有兑现。
- **复现路径**：
  1. `find examples/content-packs/sample.flooded_relics -name "*.yaml"`；
  2. grep `REPLACE` 查每条 ops 的类型；
  3. 核对是否必要用 REPLACE。

---

### ISSUE-009：`combat-decision-stub` golden label 清理验证缺失

- **严重级别**：P2
- **现状证据**：
  - `docs/opt/ui-pr/README.md` §Golden Label 所有权：PR-05 要求"上线时清理任何 `PR-02` 遗留 combat-decision-stub label"；
  - 当前未能直接在 repo 中看到清理证据（证据链缺失，非功能缺失）；
  - UI/UX PR-05 的 deferred 条目（README.md §跨 PR Deferred 与收口）已标注 COMBAT_DECISION 收口为单一 CombatDecisionFrame，内部 phase ACTION → METHOD → TARGET。
- **可能原因**：文档规则要求清理但具体 label 清理任务未在收尾 PR 的 manual record 中留下凭证。
- **影响面**：
  - **谁**：未来 UI 回归测试；
  - **哪条回路**：golden 回归护栏——如果 stub label 留在 golden 库，会伪装成"存在 legacy UI 状态"的 false positive。
- **是否伪完成**：**潜在是**。属于 housekeeping，未必真正遗留；需要一次 grep 验证。
- **复现路径**：
  1. `grep -r "combat-decision-stub" <golden-dir>`；
  2. 若有命中，删除或重命名。

---

### ISSUE-010：头部 affix 集中度偏高（sentinel / of_strength / vampiric / of_life）

- **严重级别**：P2
- **现状证据**：
  - `build/reports/harness/long-run-summary.md` §routeRewardAffixUsageSummary：`sentinel=8, of_strength=7, vampiric=7, of_life=6, of_cleansing=4, of_storms=2, of_fortitude=2, of_piercing=1, of_shadow=1 ...`；
  - 头部 4 affix 占约 50% 曝光，尾部长尾稀疏；
  - `synergyAffixDistribution: {of_piercing=11, of_shadow=6, of_smite=1}` — synergy affix 也集中。
- **可能原因**：
  1. affix 权重表对 "sentinel / of_life / vampiric" 等通用生存向 affix 曝光过高；
  2. milestone reward 的默认 affix bias 未按 profession 做分化。
- **影响面**：
  - **谁**：所有玩家的长局奖励惊喜感；
  - **哪条回路**：奖励多样性——重复局中玩家反复看到相同 affix 组合；
  - **对核心卖点的影响**：低（不伤 build 成立，但伤奖励意外性）。
- **是否伪完成**：**否**。aggregate 与 floor 都 OK，只是"尾部 affix 曝光不足"的分布问题。
- **复现路径**：
  1. 跑 `./gradlew longRunLab`；
  2. 查 long-run-summary.md 的 `routeRewardAffixUsageSummary`。

---

### ISSUE-011：ExplainPane 使用频次无 owner metric 锚定

- **严重级别**：P2
- **现状证据**：
  - `docs/opt/ui-pr/README.md` §跨 PR Deferred 与收口：ExplainPane 在 PR-02 引入 stub，PR-04 收口实现；
  - phase4Report 31 个 owner metric 中无 `explainPaneOpenRate` 或类似指标；
  - ExplainPane 属于"帮助玩家理解战斗反馈"的 UX 工具，用的多说明玩家需要解释，用的少说明 UX 自解释度高——两种都是有效信号，但当前没有捕获。
- **可能原因**：UIUX PR-04 的 owner metric 重点在 status badge / keyword description，未为 ExplainPane 单独建指标。
- **影响面**：
  - **谁**：UX 研究 / 后续迭代；
  - **哪条回路**：UX 反馈闭环；
  - **对核心卖点的影响**：很低（只影响迭代信号，不影响当前玩法）。
- **是否伪完成**：**否**。功能实现，只是验证覆盖不足。
- **复现路径**：grep `explainPane` 相关 owner metric 定义，预期返回 0。

---

## § 问题清单汇总（按严重度 / 维度交叉表）

| ISSUE | 严重 | 维度（Part 2） | 伪完成 | Phase 4 必须收口 |
| --- | --- | --- | --- | --- |
| 001 templar+vanguard long_sword 撞车 | P0 | 成长 | ✅ 是 | ✅ 必须 |
| 002 arcanist/rogue capstone adoption=0 | P0 | 成长 | ✅ 是（最严重） | ✅ 必须 |
| 003 milestone slot 偏 OFF_HAND/WEAPON | P1 | 奖励 | ✅ 部分 | ✅ 必须 |
| 004 boss variant 无 phaseOverrides | P1 | 战斗 | ✅ 是 | ✅ 必须 |
| 005 hidden lead/secret 分布错位 | P1 | 探索 | ✅ 部分 | ✅ 必须 |
| 006 long_run smoke 路线同构 | P1 | 核心循环/证据链 | ✅ 部分 | ✅ 必须 |
| 007 arcanist ARMOR 被 furnace_plate 吞 | P1 | 成长 | ✅ 是 | ✅ 必须 |
| 008 sample pack REPLACE 偏离 ADD-first | P2 | 系统联动 | ✅ 示范维度 | 可延到收尾 PR |
| 009 combat-decision-stub label 清理 | P2 | UI 反馈 | 潜在 | 可延到收尾 PR |
| 010 头部 affix 集中度偏高 | P2 | 奖励 | 否 | 可延到 Phase 5 |
| 011 ExplainPane 无 owner metric | P2 | UI 反馈 | 否 | 可延到 Phase 5 |

**核心观察**：

- 2 条 P0 都属于**成长维度**——这是 Phase 4"重复游玩差异明显"的核心卖点所在维度；
- 5 条 P1 散落在成长（1）/ 战斗（1）/ 奖励（1）/ 探索（1）/ 核心循环（1），覆盖 5 个维度；
- 7 条 P0+P1 中有 **6 条被判定为伪完成或部分伪完成**——也就是说，Phase 4 当前的核心风险不是"没做完"，而是"做完了但没真兑现玩家感知"。

---

> Part 4 将针对 P0 + P1 给出具体修订方案（含源数据改动、owner metric 补强、验收命令），P2 给出可延后列表与最终结论。

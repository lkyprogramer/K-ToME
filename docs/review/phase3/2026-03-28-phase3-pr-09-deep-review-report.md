# Phase 3 PR-09 Content Floor Completion — 深度审查报告

**审查日期**: 2026-03-28
**审查角色**: 资深 Roguelike 游戏设计总监 / 系统策划总监 / 玩法体验审查负责人
**对照基线**: `docs/review/phase3/2026-03-26-phase3-pr-09-content-floor-completion.md`
**分支**: `codex/p3-pr09-content-floor-completion`
**变更规模**: 27 files changed, +6673 / -163 lines

---

## 一、执行摘要

PR-09 的核心目标是把 Phase 3 的职业 talent 和 monster roster 补到路线图最低内容底线。经逐项对照设计文档后，**数据层面的内容底线已全部达标**（talent 64、monster 60、带 talent 怪 33、4 optional zone 独有内容、Boss 语义重分化）。但 **构建与验证链存在 4 项红灯**，其中 2 项为 P0 级阻断性问题，必须在合入前修复。

### 一句话结论

> **内容到位，管线未通。** 数据层完工度 95%+，但 `soloClearLab`、`contractLint`、`jacocoTestCoverageVerification`、`goldenScreenshot` 四项门禁失败，当前状态不满足出口门禁第 7 条。

---

## 二、逐项一致性矩阵

### 2.1 出口门禁对照（文档 §7）

| # | 门禁要求 | 目标 | 实际 | 状态 | 备注 |
|---|---------|------|------|------|------|
| 1 | 4 基础职业 talent 总数 >= 64 | 64 | 64 | **PASS** | vanguard=16, arcanist=16, rogue=16, templar=16，刚好踩线 |
| 2 | monster 模板总数 >= 60 | 60 | 60 | **PASS** | 刚好踩线，无 buffer |
| 3 | 带 talent 怪物数 >= 16 | 16 | 33 | **PASS** | 远超底线，质量良好 |
| 4 | 4 个 optional zone 全部具备独有敌方内容 | 4/4 | 4/4 | **PASS** | 每 zone 3-5 个独有怪 + 独有精英 |
| 5 | bossHarness 能断言三 Boss 的 phase-specific action | 3/3 | 3/3 | **PASS** | `./gradlew :game:bossHarness` 绿色 |
| 6 | 新增 visual/audio key 已全部补到 index + manifest | — | — | **PASS** | 116 PNG + 39 talent audio + 6 family audio + 4 boss audio 全部到位 |
| 7 | 所有门禁测试保持绿色 | 全绿 | **4 红灯** | **FAIL** | 见 §三 |

### 2.2 冻结口径对照（文档 §2.1）

| # | 口径 | 遵守情况 |
|---|------|---------|
| 1 | 不引入新的 DamageType / ResourceType / PowerType | **PASS** — 未新增任何枚举值 |
| 2 | 基础职业 talent >= 64，不允许"后续再补" | **PASS** — 精确达到 64 |
| 3 | monster >= 60，带 talent >= 16 | **PASS** — 60 / 33 |
| 4 | 每个 optional zone >= 1 独有普通怪或精英怪 | **PASS** |
| 5 | 三 Boss 可区分核心技能包 | **PASS** — 详见 §四 |
| 6 | 继续走 typed schema / AIProfile / TalentDef 主链 | **PASS** — 无旁路 |
| 7 | 不允许"复制同一技能换名字" | **PASS** — 详见 §五 |
| 8 | 同步 i18n / asset / audio key | **PASS** — en-US / zh-CN 全部补齐 |

### 2.3 W9a — 基础职业 talent floor（文档 §4.1）

| 职业 | 旧数量 | 新增 | 当前 | Build 角色覆盖 |
|------|-------|------|------|---------------|
| Vanguard (15→16) | 15 | linebreaker | 16 | 机动/控制: earthshaker(AoE stun), linebreaker(knockback) |
| | | earthshaker | | |
| | | bulwark_march | | 需注意: 只补了 1 个（spec 说 15→16），但实际补了 4 个达到 16。额外 3 个来自原始 12→15 与本轮补齐。 |
| | | battlefield_command | | |
| Arcanist (14→16) | 14 | cinder_burst, inferno_orb | 16 | Late-game control: glacial_seal(冰封), void_breach(虚空爆破) |
| | | glacial_seal, shard_storm | | Arcane payoff: shard_storm(碎片风暴) |
| | | void_breach | | |
| Rogue (14→16) | 14 | crippling_strike, eviscerate | 16 | Finisher: eviscerate(开膛) |
| | | shadow_bind, dusk_shroud | | Escape: dusk_shroud(暮影) |
| | | ricochet_knives | | Anti-elite: crippling_strike(致残打击) + ricochet_knives(弹射飞刀) |
| Templar (13→16) | 13 | radiant_lance, consecration | 16 | Aura: beacon_of_zeal(狂热灯塔) |
| | | sanctuary, absolution | | Cleanse: absolution(赦免), ritual_break(破仪) |
| | | beacon_of_zeal, ritual_break | | Delayed burst: consecration(圣别) |

**评价**: 每个职业的 build 角色缺口均已按 spec 补上，新增 talent 优先填补了中后段打法分叉而非纯数值被动，完全符合口径。

### 2.4 W9a — 进阶职业加厚（文档 §4.2）

| 职业 | 当前 talent 数 | spec 建议 10~12 | 状态 |
|------|---------------|----------------|------|
| Berserker | 6 | 10~12 | **未达建议值** |
| Spellblade | 6 | 10~12 | **未达建议值** |
| Shadowblade | 0 | frozen contract | 符合 |
| Warden | 0 | frozen contract | 符合 |

**评价**: spec §4.2 明确写"本 PR 不是主目标，允许顺手补"。当前未补属于低优先级，不阻断合入，但建议在后续 PR 中安排。

### 2.5 W9b — Monster roster floor（文档 §4.3）

| 区域 | spec 建议方向 | 实际新增 | 覆盖评价 |
|------|-------------|---------|---------|
| `bandit_camp` | 伏击/陷阱/碎片风险怪 | cutthroat(控制), slingshotter(远程), pillager(普通), banner_guard(buff), cache_overseer(tank) | **PASS** — 伏击(cutthroat ambush AI), buff(banner_guard battlefield_command) |
| `elven_ruins` | ward/cleanse/relic 守卫怪 | relic_guard, cleanse_adept, shard_archivist, vault_watcher, ward_lancer | **PASS** — ward(sanctuary+ritual_break), cleanse(absolution+radiant_lance) |
| `molten_core` | heat pressure/forge guard 变体 | slag_tender, heat_channeler, chain_overseer, anvil_guard, crucible_knight | **PASS** — heat(inferno_orb+cinder_burst), forge(earthshaker+linebreaker) |
| `crystal_cavern` | line/beam/shard resonance 怪 | shardling, prism_weaver, seal_keeper, beam_savant, resonant_colossus | **PASS** — shard(shard_storm+glacial_seal), beam(beam_savant) |
| `underground_river` | terrain/pull/displacement 怪 | hook_lurker, drowned_stalker, tide_mender, ferry_raider, current_reaver, silt_shaman, undertow_brute | **PASS** — pull(shadow_bind hook), displacement(ricochet_knives+eviscerate) |
| `abyssal_temple` | debuff/cleanse-check/shield-break 怪 | ward_breaker, ritual_binder, cleanse_hunter, void_preacher, reliquary_guard | **PASS** — shield-break(ritual_break+void_breach), debuff(ritual_binder) |
| `abyssal_heart` | finale escort/anti-defensive 怪 | null_lancer, eclipsed_seraph (+ 复用 void_preacher) | **PASS** — finale(linebreaker+sanctuary), anti-defensive(void_breach) |

**Monster 家族分布总览**:

| 家族 | 数量 | 带 talent | zone 归属 |
|------|------|----------|----------|
| bandit | 11 | 5 | shattered_outpost, greenwood, bandit_camp |
| abyssal | 8 | 7 | abyssal_temple, abyssal_heart |
| river | 7 | 5 | underground_river |
| undead | 6 | 0 | 全局通用 |
| crystal | 5 | 4 | crystal_cavern |
| forge | 5 | 4 | molten_core |
| warded_ruin | 5 | 4 | elven_ruins |
| cultist | 4 | 1 | grey_gate, deep_iron, abyssal |
| orc | 4 | 2 | deep_iron_pit |
| beast | 3 | 0 | 全局通用 |
| goblin | 2 | 0 | shattered_outpost |
| **合计** | **60** | **33** | |

### 2.6 W9b — Boss 语义重分化（文档 §4.4）

| Boss | 核心技能包 | 语义定位 | 与旧版对比 |
|------|----------|---------|-----------|
| `orc.molten_giant` | inferno_orb, cinder_burst, linebreaker, earthshaker | **火伤/重击/AoE** | 旧: war_cry + power_strike + charge → 新: 完全重构 |
| `cultist.dungeon_lord` | battlefield_command, shadow_bind, ritual_break, arcane_shield | **号令/控场/仪式** | 旧: 与 guardian 共享 → 新: 独立控制系 |
| `abyssal.guardian` | arcane_shield, void_breach, linebreaker, consecration | **虚空/护盾/圣别** | 旧: dungeon_lord 皮肤 → 新: 独立虚空系 |

**技能重叠分析**:

| 技能 | molten_giant | dungeon_lord | guardian |
|------|:-----------:|:------------:|:--------:|
| arcane_shield | — | enraged phase | both phases |
| linebreaker | both phases | — | both phases |
| 其他技能 | 全部独占 | 全部独占 | 全部独占 |

**评价**:
- `arcane_shield` 被 dungeon_lord 和 guardian 共享，但使用阶段和语境不同（dungeon_lord 仅在绝望阶段启用作为防御切换，guardian 常驻作为基础防御手段）。
- `linebreaker` 被 molten_giant 和 guardian 共享，作为基础近战接触工具，这在 Roguelike 中是合理的——boss 需要一个能近身的手段。
- 旧版的 `war_cry + power_strike + charge` 三件套共享已完全消除。
- **结论: PASS** — 三 Boss 核心语义可区分。

### 2.7 W9c — 美术/音频资源（文档 §4.5）

| 资源类别 | spec 要求 | 实际状态 |
|---------|----------|---------|
| Monster family actor + icon | 每 family 1 基底 + 变体 | **116 PNG** in `phase3/p3-followup/` |
| Monster family audio | 每 family 1 条 | 6 family audio (bandit, warded_ruin, forge, crystal, river, abyssal) |
| Boss 独立 actor/icon/visual | 3 Boss 各一套 | orc.molten_giant + abyssal.guardian 新增，dungeon_lord 沿用旧资产 |
| Boss 独立 audio | 3 Boss 各一条 | 4 boss audio (含 bandit_captain + warning) |
| Talent visual + icon + audio | 每主动 talent 一套 | 20 talent × (visual + icon + audio) 全部到位 |
| Manifest 完整性 | 所有 key 有 manifest entry | visual-manifest.json +2090 行, audio-manifest.json +360 行 |
| 输出目录 | `phase3/p3-followup/` | **PASS** — 未散落到 phase2 目录 |
| Tags | 至少含 phase3, followup | **PASS** — manifest 中 tags 齐全 |

### 2.8 W9d — 测试覆盖门禁（文档 §4.6）

| 测试 | spec 要求 | 实际状态 |
|------|----------|---------|
| MonsterSchemaTest | 验证 monster 总数、带 talent 数、optional/late zone 覆盖 | **PASS** — >= 60 monsters, >= 16 talented, >= 11 elites, 11 families |
| ZoneContentCoverageTest | optional zone 独有内容、unique roster anchors | **PASS** — 4 optional zones + 3 late zones, 每 zone 3+ unique anchors |
| BossHarnessTest | 3 Boss phase-specific action 断言 | **PASS** — 3 Boss × 4 distinct abilities |
| ProfessionSchemaTest | 基础职业 >= 16 talent | **PASS** — 4 × 16 |
| SoloClearLabV2Test | PR-09 talent 进入构筑路径 | **FAIL** — vanguard PR-09 talent 未执行 |
| LongRunLabFullTest | headless turn 与胜率无崩坏 | **PASS** — 52s 完成，全部通过 |

---

## 三、阻断性问题（必须修复才能合入）

### 🔴 P0-1: `soloClearLab` 红灯 — vanguard PR-09 talent 未进入构筑路径

**表现**: `SoloClearLabV2Test` 断言 vanguard 至少执行一个 PR-09 talent (linebreaker / earthshaker / battlefield_command)，但实际为 0。

**根因**: `SoloClearLabSupport.kt:514-524` 的 `installScenarioLevel()` 设置 `unspentTalentPoints = 0`。玩家仅持有 `startingTalents` (power_strike, shield_bash, guard_stance, war_cry)，无法学习 PR-09 新增 talent。`preferredCombatLoadoutCommand()` 虽然有新 talent 的 desiredOrder，但被 `unlockedTalentIds` 过滤掉了。

**影响**: 直接违反 spec §4.6 第 2 条和 §7 第 7 条。新 talent 在 solo clear 场景中是"死数据"。

**修复方向**:
- 方案 A（推荐）: 在 `buildScenarioRuntime()` 中根据 scenario level 自动从 profession 的 talent tree 中按优先级解锁 talent，使得 level 5+ 的场景能持有 PR-09 talent。
- 方案 B: 为 solo clear 场景增加一个 `extraTalents` 配置，直接将 PR-09 talent 注入到 reserve pool。
- 预估工作量: ~30 行代码改动。

### 🔴 P0-2: `contractLint` 红灯 — talent visual key 未注册到 contract registry

**表现**: `ContractLintTest.schema_v2_contracts_resolve_all_mandatory_cross_references` 报错 `Unknown visual key talent.vanguard.linebreaker.visual`。

**根因**: 新 talent 在 `visuals/index.yaml` 和 `visual-manifest.json` 中都已定义，但 `tools/` 模块的 contract lint 有自己的 key registry，新增的 20 个 talent visual key 未同步注册。

**影响**: 直接阻断 `./gradlew check`，违反 spec §7 第 7 条。

**修复方向**: 检查 `tools/src` 下的 ContractLintTest 或其支撑数据，将新 talent visual key 纳入合法 key 集合。如果 lint 是自动从 `visuals/index.yaml` 扫描的，则排查为什么新增 entry 未被识别（可能是 schemaVersion、category 或 path 格式不匹配）。

### 🟡 P1-1: `jacocoTestCoverageVerification` 红灯 — core 模块覆盖率跌破 80%

**表现**: `lines covered ratio is 0.79, but expected minimum is 0.80`。

**根因**: `TalentResolver.kt` 新增 188 行实现代码，但对应的单元测试仅通过 harness 间接覆盖，直接单元测试覆盖不足。

**修复方向**:
- 为 `TalentResolver` 中新增的 8 个 talent handler (linebreaker, earthshaker, bulwark_march, battlefield_command, inferno_orb, shard_storm, ricochet_knives, eviscerate 等) 补充 1-2 个直接单元测试用例，覆盖核心分支。
- 或者临时将 jacoco 阈值调整为 0.79（不推荐，仅作应急止损）。

### 🟡 P1-2: `goldenScreenshot` 红灯 — 5 个 golden hash 失配

**表现**: boss warning / outcome recap / gameplay log / route midpoint 等 5 个 golden screenshot hash 不再稳定。

**根因**: 新增内容改变了渲染输出（新 talent 名称出现在 UI 中、新怪物出现在 log 中等），golden hash 需要重新基线化。

**修复方向**: 运行 golden screenshot 更新命令（通常是 `./gradlew :client:updateGoldenScreenshots` 或等效 task），审核新截图无误后提交新 hash。

---

## 四、玩法体验深度评审

### 4.1 职业 talent 补齐质量

**优点**:
- 每个职业的新增 talent 都精准填补了 spec 指定的 build 角色缺口（而非泛泛加数值）。
- Templar 一次补 6 个（13→16 最大缺口），且覆盖了 aura / cleanse / delayed burst 三个维度。
- 所有新增 talent 均为主动技能，零被动填充，完全遵守"不能只补被动数值抬升"的口径。
- `TalentResolver.kt` 的实现复用了已有的 effect pipeline（damage → knockback → status apply），未引入新的 effect type。

**关注点**:
- `linebreaker`、`earthshaker`、`inferno_orb`、`shard_storm`、`ricochet_knives` 共享"AoE 多目标 + knockback"骨架。虽然伤害类型、元素效果、资源消耗不同导致战术意义不同，但从 ability template 层面的结构相似度较高。这在当前内容厚度下是合理的（不同元素 vs 不同抗性 = 不同决策），但如果后续再大量新增同骨架 talent，建议引入更多机制维度（如延时触发、地形效果、combo 链等）。
- 所有 76 个 talent 均为 ACTIVE，零 PASSIVE。这在当前作为"补底线"是正确的，但长期来看 roguelike 的 build 深度需要 active/passive 混合才能产生组合复杂度。

### 4.2 Monster 生态评审

**优点**:
- 6 个新 family (bandit, warded_ruin, forge, crystal, river, abyssal) 分布在 7 个 zone，每个 family 有 5-8 个变体，层次分明。
- 带 talent 怪物 33/60 = 55%，远超底线 16，意味着超过一半的怪能给玩家带来技能层面的决策压力。
- AI profile 设计遵循了"家族内部分工"原则：每个 family 有 kite (远程法师型) + chase (近战坦克型) + patrol (巡逻兵型) 的组合，保证遭遇战的节奏多样性。
- 新怪使用的 talent 同时也是玩家 talent（如 shadow_bind、ritual_break、linebreaker），这在 ToME 系游戏中是标准设计——"敌方能力 = 玩家能力的镜像"能让玩家通过对战来理解招式。

**关注点**:
- `river.drowned_stalker` 和 `river.undertow_brute` 是 river family 中唯二无 talent 的怪。作为 brute 类纯肉盾可以接受，但如果要提升 underground_river 的决策密度，可以考虑给 undertow_brute 一个 charge/knockback 类 talent。
- `undead` 家族 6 个怪全部无 talent，在 late-game zone 中作为填充物是够用的，但长期来看是最薄弱的 family。
- `beast` 和 `goblin` 家族仍然是纯数值怪，这在早期 zone 合理，不需要改动。

### 4.3 Optional Zone 独有性评审

| Zone | 相邻 mandatory | 独有怪数量 | 独有精英 | 特色机制 |
|------|---------------|-----------|---------|---------|
| bandit_camp | greenwood_fringe | 5 (cutthroat, slingshotter, pillager, banner_guard, cache_overseer) | cache_overseer | ambush_lane, cache_raids |
| elven_ruins | greenwood_fringe | 5 (relic_guard, cleanse_adept, shard_archivist, vault_watcher, ward_lancer) | ward_lancer | lore_cache, arcanist_echoes |
| molten_core | deep_iron_pit | 5 (slag_tender, heat_channeler, chain_overseer, anvil_guard, crucible_knight) | crucible_knight | lava_pockets, forge_cache |
| crystal_cavern | underground_river | 5 (shardling, prism_weaver, seal_keeper, beam_savant, resonant_colossus) | resonant_colossus | crystal_shards, resonance_cache |

**评价**: 每个 optional zone 都有完整的 5 怪 roster（含 1 精英），与相邻 mandatory zone 零重叠，远超 spec "至少 1 个独有怪"的底线。`ZoneContentCoverageTest` 有显式的 unique roster anchor 断言来保护这个不变量。

### 4.4 Boss 体验差异评审

以 BossHarnessTest 中的 phase 断言为基准：

| Boss | P1 核心循环 | P2 切换特征 | 玩家应对题目 |
|------|-----------|-----------|------------|
| molten_giant | inferno_orb(远程火球) + linebreaker(近身破阵) + cinder_burst(AoE灼烧) | earthshaker(地震) 权重 42% 跃升为主手段 | P1: 拉距离躲火球 → P2: 近战必须承受地震打断 |
| dungeon_lord | battlefield_command(号令buff) + shadow_bind(束影) + ritual_break(破仪) | ritual_break 权重升至 40% + arcane_shield 加入 | P1: 反控制/反buff → P2: 必须破盾后才能有效输出 |
| abyssal_guardian | arcane_shield(常驻盾) + void_breach(虚空裂隙) + linebreaker(近身) | void_breach 权重升至 44% + consecration 加入 | P1: 先破盾再打 → P2: 必须走位躲高频虚空弹幕 + 圣别地面效果 |

**评价**: 三 Boss 在"玩家应对策略"层面完全可区分——molten_giant 考验"近/远切换"，dungeon_lord 考验"反控/破盾"，guardian 考验"走位/DPS race"。这正是 Roguelike boss 设计应有的差异化深度。

---

## 五、"换名不换皮"检查（文档 §2.1 第 7 条）

逐个审查新增的 20 个 talent 是否存在"复制同一技能换名字"：

| 机制骨架 | 使用该骨架的 talent | 差异点 | 判定 |
|---------|-------------------|-------|------|
| AoE 多目标 + knockback | linebreaker, earthshaker, inferno_orb, shard_storm, ricochet_knives, consecration | 伤害类型不同(PHYSICAL/FIRE/ICE/SHADOW/HOLY)、资源类型不同(STAMINA/MANA/ENERGY/POSITIVE_ENERGY)、触发效果不同(stun/slow/bleed/ground_effect)、射程不同 | **PASS** — 战术意义因元素抗性系统而不同 |
| 单目标 + debuff | crippling_strike, shadow_bind, glacial_seal, void_breach, radiant_lance, ritual_break | 控制类型不同(slow/root/freeze/vuln/purge)、资源成本不同、射程不同 | **PASS** — 对不同目标的最优选择不同 |
| 自我 buff | bulwark_march, dusk_shroud, sanctuary, battlefield_command | 效果完全不同(GUARD/stealth/SHIELD/war_cry_empower)、持续时间不同 | **PASS** — 功能定位完全不同 |
| Finisher + restore | eviscerate, absolution | eviscerate 恢复 ENERGY，absolution 恢复 HP + cleanse | **PASS** — 资源回路完全不同 |
| Aura/beacon | beacon_of_zeal | 唯一新增 aura 类 | **PASS** |

**结论**: 虽然部分 talent 共享 effect pipeline 骨架，但由于 K-ToME 的元素抗性系统使得不同 damage type 面对不同 monster 产生不同决策，因此同骨架 ≠ 同技能。不存在"复制换名"情况。

---

## 六、代码质量与集成评审

### 6.1 数据层

| 文件 | 变更量 | 评价 |
|------|-------|------|
| talents/index.yaml | +1612 行 | 结构一致，每 talent 均有完整的 effects/keywords/cooldown/resource_cost 定义 |
| monsters/index.yaml | +902 行 | 结构一致，每 monster 均有 stats/resistances/loot/visualKey/audioProfile |
| ai/index.yaml | +675 行 | 新增 14 个 AI profile，覆盖所有带 talent 的新 family + 3 个 Boss 的双 phase |
| bosses/index.yaml | +12 行修改 | 三 Boss 的 talent/aiProfile 引用更新 |
| zones/index.yaml | +28 行修改 | 4 optional + 3 late zone 的 monsterPools/elitePools 更新 |
| visuals/index.yaml | +136 行 | 新 key 全部注册 |
| audio/index.yaml | +30 行 | 新 key 全部注册 |
| i18n/en-US.json | +110 行 | 20 talent + 33 monster 的 name/desc 全部翻译 |
| i18n/zh-CN.json | +110 行 | 同上，中文翻译质量良好 |

### 6.2 代码层

| 文件 | 变更量 | 评价 |
|------|-------|------|
| TalentResolver.kt | +188 行 | 8 个新 handler 遵循既有 pattern，无旁路实现。但新增代码导致 jacoco 覆盖率跌破阈值 |
| EntityFactory.kt | +39 行 | 兼容性更新，无问题 |
| LoadoutPlanner.kt | +19/-3 行 | 新 talent 按资源类型正确分类到 loadout order |
| SmokeBot.kt | +43/-3 行 | 新 talent 有合理的 priority 分级，offensive order 更新 |

### 6.3 测试层

| 文件 | 变更量 | 评价 |
|------|-------|------|
| MonsterSchemaTest.kt | +19/-3 行 | 阈值更新到 60/16/11，新增 family 覆盖断言 |
| ZoneContentCoverageTest.kt | +32 行 | 新增 unique roster anchor 断言，7 组 zone-vs-zone 对比 |
| BossHarnessTest.kt | +34/-3 行 | 3 Boss 各 4 个 distinct ability 断言 + telegraph 断言 |
| SoloClearLabV2Test.kt | +15 行 | 新增 PR-09 talent execution 断言（但当前失败） |
| LongRunLabFullTest.kt | +14/-3 行 | 阈值调整，52s 全通过 |
| LoadoutPlannerTest.kt | +142/-3 行 | 新 talent 的 loadout 排序测试 |
| SmokeBotTest.kt | +33 行 | 新 talent 的 priority 测试 |

---

## 七、延后事项（不阻断本 PR 但需追踪）

| # | 事项 | 优先级 | 建议安排 |
|---|------|-------|---------|
| 1 | Berserker / Spellblade talent 补到 10~12 | P2 | PR-10 或 PR-11 |
| 2 | undead 家族全部无 talent，late-game 缺乏决策压力 | P3 | Phase 4 |
| 3 | 基础 talent 64 和 monster 60 均踩线无 buffer | P3 | 下次内容批次时留余量 |
| 4 | 纯 ACTIVE talent 系统需要 passive 来产生组合复杂度 | P3 | Phase 4 build diversity PR |
| 5 | river.undertow_brute 可补 charge/knockback talent 增加决策密度 | P4 | 可选优化 |

---

## 八、修复优先级与建议操作序列

```
1. [P0] 修复 soloClearLab — 在 buildScenarioRuntime() 中按 level 解锁 talent
2. [P0] 修复 contractLint — 将新 talent visual key 纳入 lint registry
3. [P1] 修复 jacoco — 为 TalentResolver 新 handler 补单元测试
4. [P1] 修复 goldenScreenshot — 重新基线化 golden hash
5. [验证] ./gradlew check 全绿
6. [合入] PR ready for merge
```

预估修复工作量: ~2-4 小时。

---

## 九、最终结论

| 维度 | 评分 | 说明 |
|------|------|------|
| 内容底线达标 | **A** | 所有量化指标达标或超标 |
| 内容质量（不是充数） | **A-** | 每个 talent/monster 有明确的战术定位，无"换名换皮"，但 AoE 骨架复用率偏高 |
| Boss 差异化 | **A** | 三 Boss 完全可区分，应对策略各异 |
| 美术/音频完整性 | **A** | 116 PNG + 49 OGG + manifest 全部到位 |
| 测试覆盖 | **B-** | 数据层测试优秀，但 soloClearLab/contractLint/jacoco/goldenScreenshot 4 项红灯 |
| 代码质量 | **A-** | 遵循现有架构，无旁路，但 jacoco 需要补测试 |
| 可合入状态 | **待修复** | 修复 4 项红灯后可合入 |

**综合评价**: 这是一个高质量的内容补量 PR，数据设计扎实、资源链完整、测试框架到位。当前唯一阻塞是验证管线的 4 个失败点，均为可快速修复的集成问题而非设计缺陷。修复后即可合入。

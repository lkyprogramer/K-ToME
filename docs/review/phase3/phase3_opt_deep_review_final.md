# Phase 3 最终综合审查报告

> 综合来源：
> - `docs/review/phase3/phase3_opt_deep_review_claude_v3_part1.md`
> - `docs/review/phase3/phase3_opt_deep_review_claude_v3_part2.md`
>
> 校正依据：
> - `docs/phase3/`
> - `docs/review/phase3/v2/`
> - `core/game/client/tools` 当前实现
> - 2026-03-30 本地门禁命令结果：`./gradlew combatTraceGolden`、`./gradlew bossHarness`、`./gradlew longRunLab` 均返回 `BUILD SUCCESSFUL`（本次为 up-to-date 通过，不含重新生成的临时实验室报告）

## 1. 最终结论

1. `Phase 3` 的**系统合同与实现骨架基本完成**，主线功能口径已经达到进入下一阶段的最低工程门槛。
2. 相比早期审查，`PR-11 ~ PR-14` 的四个补强方向确实落地了：天赋点经济、战斗反馈快照、三种区域 runtime 机制、cadence 奖励与商店刷新服务都已经进入正式主链。
3. 当前版本的真实状态不是“功能完成但完全不好玩”，也不是“已经有明显耐玩性”，而是：**基本可玩，核心循环成立，但中后段体验和复玩驱动力仍明显偏弱**。
4. 两份现有报告的主判断大体正确，但其中若干统计口径明显偏大。按当前资源文件实数，项目是 **94 个 talent、33 个 AI profile、60 个 monster、11 个 zone、4 个 boss encounter、25 个 item base + 40 个 affix、8 个 inscription**，不是报告里写的 `123 / 101 / 209` 那个量级。
5. 当前最大的结构性问题不在“系统有没有”，而在**后半程 zone 机制稀薄、验证门禁偏软、基础职业构筑感知不足**。这三件事如果直接带进 `Phase 4`，后续加内容只会把问题放大。
6. 可以进入 `Phase 4`，但只能是**有条件进入**：先把本报告列出的两个 `P0` 级问题修完，否则 `Phase 4` 的 ProcGen/Loot 扩张会建在错误地基上。

## 2. 两份报告的共识

两份现有报告达成的核心共识，基本成立：

1. `Phase 3` 的规则主链已经完整成型。
   - 证据：`core/combat/*`、`core/status/*`、`core/talent/*`、`core/ai/*`、`core/world/*`
2. `PR-11 ~ PR-14` 的 v2 补强不是停留在文档层，而是已经进了 runtime。
   - 证据：
   - `core/progression/ExperienceSystem.kt`
   - `core/snapshot/RenderSnapshot.kt`
   - `game/ZoneMechanicRuntime.kt`
   - `core/economy/ShopModels.kt`
   - `game/src/main/resources/data/shops/index.yaml`
   - `game/src/main/resources/data/loot/index.yaml`
3. 当前版本相比 v2 审查时，玩法“最低谷”的几个问题已经被明显抬高。
   - 天赋点从“严重饥荒”变成“仍紧但可玩”
   - 奖励从“可能空层”变成“有 cadence 兜底”
   - 普通怪从“大量纯追击模板”变成“多数怪物至少有 archetype 行为差异”
4. 项目最大的长期资产确实是验证基础设施。
   - 证据：`tools/src/test/kotlin/com/ktome/tools/golden/CombatTraceGoldenHarnessTest.kt`
   - 证据：`game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt`
   - 证据：`game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt`
   - 证据：`game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt`

## 3. 需要校正的地方

两份现有报告的方向判断多数可用，但以下口径需要校正，否则最终报告会高估完成度。

### 3.1 统计口径校正

| 项目 | 现有报告写法 | 当前代码实数 | 证据 |
| --- | --- | --- | --- |
| Talent 数量 | `123` | `94` | `game/src/main/resources/data/talents/index.yaml` |
| AI profile 数量 | `101` | `33` | `game/src/main/resources/data/ai/index.yaml` |
| Item 数量 | `209` | `25 item base + 4 materials + 40 affix` | `game/src/main/resources/data/items/index.yaml` |
| Boss encounter | `4` | `4` | `game/src/main/resources/data/bosses/index.yaml` |
| Zone 数量 | `11` | `11` | `game/src/main/resources/data/zones/index.yaml` |
| Monster 数量 | `60` | `60` | `game/src/main/resources/data/monsters/index.yaml` |

结论：

1. 项目完成度依然高，但**内容量没有现有报告写得那么夸张**。
2. 这不是小误差，而是会影响“耐玩性是否已经成立”的判断。`94` 个 talent 和 `123` 个 talent 的玩法深度不是一回事。

### 3.2 怪物 AI 清理口径不能只看 `ai.chase.basic / ai.patrol.basic`

现有报告把“残留 basic-profile 怪物”写成 `4` 个，这只看了 `ai.chase.basic` 和 `ai.patrol.basic`。

如果按真实玩法风险看，还要把 `ai.guard.basic` 算进去。当前分布是：

| Profile | 数量 | 代表怪物 |
| --- | --- | --- |
| `ai.chase.basic` | 2 | `beast.rat`、`beast.rat_scavenger` |
| `ai.patrol.basic` | 2 | `goblin.scout`、`undead.restless_skeleton` |
| `ai.guard.basic` | 6 | `goblin.scrapper`、`orc.miner`、`undead.chain_thrall`、`warded_ruin.vault_watcher`、`forge.slag_tender`、`river.undertow_brute` |

结论：

1. 如果按“教程尾部清理”口径，`4` 个成立。
2. 如果按“中后期战斗是否足够有差异”口径，**至少还有 10 个怪物挂在泛用 profile 上**。
3. 这件事不能算 `P0`，但绝对不能说“普通怪行为问题已经完全解决”。

### 3.3 Zone 机制成立了，但只成立了一半

现有报告说区域机制从伪实现升级为 runtime，这句只说对了一半。

真正有运行时逻辑的，目前只有：

1. `patrol_pressure`
2. `ambush_lane`
3. `furnace_pressure`

对应代码：

1. `game/src/main/kotlin/com/ktome/game/ZoneMechanicRuntime.kt`
2. `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` 中的触发与结算路径

但 `zones/index.yaml` 里的大部分 `specialMechanics`，当前仍停留在 `introHintKey()` 层：

1. `line_of_sight`
2. `beacon_warning`
3. `cache_raids`
4. `lore_cache`
5. `arcanist_echoes`
6. `ore_cart`
7. `slag_alert`
8. `sealed_gate`
9. `ritual_pressure`
10. `currents`
11. `ferry_crossing`
12. `drowned_ambush`
13. `crystal_shards`
14. `resonance_cache`
15. `abyssal_ward`
16. `prayer_hall`
17. `void_pressure`
18. `finale`
19. `void_eruption`
20. `heart_ward`

结论：

1. “前中段有机制，后中段大量只有提示没有玩法”才是当前真实状态。
2. 这会直接造成 `deep_iron_pit` 之后新鲜感反而下降。

### 3.4 Long Run 门禁不能被当成“全程体验已被完全证明”

这点是现有两份报告都低估的地方。

`LongRunLabFullTest` 和 `LongRunLabTest` 里，存在明显的中途起跑口径：

1. `LongRunLabFullTest` 的部分矩阵组合直接从 `molten_core / crystal_cavern / underground_river / bandit_camp / elven_ruins` 开始。
2. `LongRunLabTest` 里的 `berserker / spellblade` smoke 直接从 `abyssal_temple` 起跑，不是完整 run。

证据：

1. `game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt`
2. `game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt`

这说明当前实验室更像是在验证：

1. 指定路段是否稳定
2. 终线与后期区段是否能跑通
3. 高风险区是否不会卡死

而不是严格证明：

1. `4` 基础职业 × `3` 种族大多数都能从 `shattered_outpost` 完整走到终线
2. `2` 进阶职业已经被完整长局验证

结论：

1. 自动化门禁证明了“系统稳定性不错”。
2. 但它**还没有充分证明“从开局到终局的完整节奏已经成立”**。

## 4. 最终玩法判断

### 4.1 当前版本是否已经好玩

结论：**基本可玩，但还不能直接判定为“已经明显好玩”。**

更准确的说法是：

1. 前中段已经不无聊了。
2. 核心循环已经成立。
3. 但中后段的新鲜感、构筑感和路径差异，还没有强到足以支撑“耐玩雏形”。

### 4.2 当前核心循环是否成立

结论：**成立，但强度一般。**

当前循环已经具备：

1. 探索
2. 战斗
3. 经验与 shard 收益
4. cadence 奖励兜底
5. 升级与天赋分配
6. 商店消费
7. Route 选择
8. 长局推进

但最弱的两个环节仍然是：

1. **后半段探索差异不够**
2. **成长的玩法分化感不够强**

### 4.3 当前版本是否具备耐玩雏形

结论：**只有弱雏形，还不够稳。**

原因不是内容量完全不够，而是结构上还有三处断层：

1. `deep_iron_pit` 之后 zone 机制明显变稀
2. 基础职业 build 的“体验分化”不够强
3. 奖励系统解决了“不给东西”，但还没有解决“给的东西是否让人兴奋”

## 5. 当前 Phase 3 必须先解决的问题

### P0-1：后半程 zone 机制断层

**问题本质**

`Phase 3` 已经承诺长局结构成立，但当前真正有运行时玩法的 zone mechanic 主要集中在前中段。进入 `grey_gate_depths / underground_river / crystal_cavern / abyssal_temple / abyssal_heart` 后，大量 `specialMechanics` 只有 hint，没有可持续改变玩家决策的 runtime。

**为什么必须在 Phase 3 修**

1. 这是长局体验是否成立的核心问题，不是内容扩张问题。
2. 如果现在不修，`Phase 4` 继续往这些 zone 上叠 ProcGen/Loot，只会把“地图更复杂但玩法仍同质”的问题放大。

**不修的后果**

1. 前段比后段更鲜活，体验曲线倒挂。
2. optional zone 和 late zone 的探索动机不足。
3. 玩家会把长局理解成“换皮战斗链”，而不是“逐段升级的冒险”。

**建议**

1. `underground_river` 至少补一个真正影响走位/路径的 `currents` runtime。
2. `crystal_cavern` 至少补一个可互动或可规避的 `crystal_shards / resonance` runtime。
3. `abyssal_temple` 或 `abyssal_heart` 至少补一个正式 `ward/pressure` runtime，不再只靠 boss 承担终线新鲜感。

### P0-2：Long Run 门禁口径偏软

**问题本质**

当前实验室证明了局部稳定性，但没有足够证明完整 run 的早中后节奏已经成立。

**为什么必须在 Phase 3 修**

1. `Phase 3` 的出口就是长局成立。
2. 如果连门禁本身都不能真实覆盖完整长局，那 `Phase 4` 里所有“重玩性”讨论都会失真。

**不修的后果**

1. 团队会高估当前长局完成度。
2. 后续 balance / loot / procgen 调优会建立在不充分样本上。

**建议**

1. `LongRunLabFullTest` 至少增加一组“完整起跑矩阵”，不能只靠中途起跑覆盖后段。
2. `berserker / spellblade` 至少各补一条从 `shattered_outpost` 开始的完整 smoke run。
3. 保留中途起跑 probe，但把它明确降级成“route probe / late-route probe”，不再与 full-route 成功等价。

### P1-1：基础职业构筑分化感不足

**问题本质**

天赋点经济已经改善，但玩家是否能明显感受到“这一局的 Vanguard 和上一局的 Vanguard 是两种打法”，当前证据仍不够强。

**为什么仍属于当前阶段**

`Phase 3` 的主题之一就是职业树和 build 深度成立。这不是 `Phase 4` 的额外锦上添花。

**建议**

1. 不一定继续加大量 talent。
2. 更有效的是把基础职业各补 1 到 2 个“build-defining breakpoint payoff”。
3. 同时把少数 affix 从纯数值加成改成更强的玩法标签联动，让装备开始真正推 build。

### P1-2：奖励从“兜底”走到了“稳定”，但还没走到“兴奋”

**问题本质**

cadence 奖励和 refresh stock 解决了奖励旱区，但大部分奖励仍偏“实用补给”，缺少强记忆点。

**为什么属于当前阶段**

`Phase 3` 已经要求掉落和构筑形成正反馈。现在的系统保证了“不会太差”，但还没有保证“足够想要”。

**建议**

1. 保留 cadence，但给 cadence reward 更明确的玩家可见标识。
2. 在 route reward / optional zone reward 中提高少量 build-defining affix 或 utility unique 的辨识度。
3. 给后半段 shard 增加轻量消费出口，避免只堆数字。

### P2-1：死亡分析与新手解释成本仍偏高

这不是阻塞 `Phase 4` 的问题，但已经值得排进近期清单：

1. 死亡复盘还不够强
2. zone 机制提示仍偏日志化
3. 新玩家对天赋断点、资源轴和 zone 规则的理解成本偏高

## 6. 最终优先级清单

### P0

1. 让至少 `2` 个后半程 zone 从“只有 hint”升级为正式 runtime mechanic。
2. 把 `LongRunLab` 的 full-route 口径补硬，尤其是基础职业全程起跑和进阶职业完整 smoke。

### P1

1. 强化基础职业 build differentiation，不靠大扩容，靠 breakpoint payoff 和少量装备联动提升感知差异。
2. 让奖励从“稳定有用”进一步升级到“有记忆点”，重点处理 optional zone reward、route reward、late-run shard sink。
3. 继续压缩中后期泛用 AI 的覆盖面，至少让 `ai.guard.basic` 不再挂在那么多中后段主战怪上。

### P2

1. 死亡分析、战斗后复盘与机制解释增强。
2. 反馈层继续提升，但不必在 `Phase 3` 强行做完整动画资产包。

## 7. 最终判决

### 当前 Phase 3 是否达到文档预期

**功能合同层面：基本达到。**

**玩法完成态层面：部分达到。**

更准确地说：

1. 规则、职业、AI、Boss、世界图、经济、存档、验证主链都已经到位。
2. 但“长局是否真正越来越有趣”这个问题，还不能给满分答案。

### 当前版本是否已经“好玩”

**可以玩，能持续玩一段，但还没到强推荐意义上的“明显好玩”。**

### 当前版本是否具备“耐玩雏形”

**有雏形，但偏弱。**

### 是否适合进入下一阶段

**可以有条件进入 `Phase 4`。**

前提是：

1. 先补后半程 zone mechanic 断层
2. 先补 full-route 长局门禁

如果这两项不补，`Phase 4` 会把当前的体验断层一起放大。

### 一句话结论

`Phase 3` 不是伪完成，也不是可以放心收工的完成态。它已经把系统骨架搭稳了，但还没有把“长局为什么值得一直玩下去”这件事真正做透。下一步不该急着加更多内容，而该先把后半程玩法密度和完整长局验证补硬。

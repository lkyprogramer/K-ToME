# K-ToME Phase 4 深度审阅 · Part 2

> **本文件定位**：Part 2 = 玩法体验总评（2/4）
> **评估视角**：30–90 分钟 run 的玩家实际感受，不评论代码风格
> **参考基线**：Part 1 的一致性矩阵 + `docs/review/phase4/phase4_opt_deep_review_claude_v4_part2.md`

---

## 一、总体定调

一句话：

> **"前 30 分钟像一个正经 Roguelike，30–60 分钟像一个合格的 ToME-lite，60 分钟之后像一个有模版的关卡生成器"。**

换句话说，Phase 4 当前在 **"曲线前半段有记忆点、曲线后半段缺语言"** 这一点上**明显收敛了、但没有真正翻过去**。这直接源于 Part 1 矩阵里标 **低** 的三条：Unique/Artifact 无独占效果、动态 loot pool 仅 4 区、Boss 无阶段脚本。

下面按七维具体展开。

---

## 二、核心 run 循环（Core Loop）

### 2.1 循环结构

当前一条标准 run 的感知结构是：

```
Zone Intro → Frontstage 读懂区语义 → 布点 / 开宝箱 / 打精英
    → 触发 hidden entrance（PERCEPTION / ENTER_ROOM / QUEST_STEP / INTERACT_TILE …）
    → Secret Zone 内识别身份（specialTemplateTag / affixTag）
    → cadence reward + secret reward（区分文案）
    → 下一层 / 下一区
    → Boss（3 变体 × 权重动作）
```

### 2.2 好的部分

- **前 30 分钟回放性（V2OPT-PR-05）真实可感**：`greenwood_fringe` 已抬到 `distinctPatternRoomCount=2 / distinctEntranceLayoutCount=2 / differenceCategoryCount=5`，`SchemaZoneMapgenProfileResolver(zoneId, floorIndex)` 让 floor-0 / floor-1 产出能明显不同；这不是统计意义上的"像"而是玩家打开下一层时**看到二次 anchor family 带来的视觉与 entrance 差异**。
- **隐藏发现的感知闭环成立**：`organicHiddenProbe` 的 `firstHiddenDiscoveryTurnP50 / firstSecretZoneEntryTurnP50` 进入 canonical report，意味着"探索线的节奏好坏第一次有客观 owner"；玩家侧表现是"大约前 30–40 步内会遇到第一条 hidden primer，且 primer 文案告诉你值得继续搜"。
- **3 条最小前台反馈合同（primer / failed search / secret reward 文案）真的改善"不知道发生了什么"的旧问题**。

### 2.3 不好的部分 · 为什么

1. **循环的"峰值-低谷"节奏偏平**。Phase 4 当前缺两类锚：
   - **中后期区（molten_core / grey_gate_depths / crystal_cavern）没有身份 loot**。这几个区的 cadence / reward 仍是 `FIXED_LIST` 4 件（见 Part 1 §B 动态池覆盖证据），玩家到这几层后会进入 "stat 上涨但 build 没变化" 的平原。
   - **终盘 Boss 是 3 个 HP 包**：同一 Boss 的 action weight 只有 2–3 条（`boss-variants/index.yaml` L4-14），没有阶段切换，打到 50%/25% HP 时不会有新的动作进入池，导致 Boss 观感退化为 "撑住资源消耗"。
2. **Hidden 闭环的奖励深度不等于反馈深度**。Secret reward identity guardrail 硬 ≤ 0.35/0.40/0.40 的 strict pair ceiling 是正确的，但玩家接触到 reward 的瞬间，前台呈现仍然只是一行 `detailText`；**没有 "secret 来源 / 特殊触发 / 局部身份" 的可视化强调**（见 §UI）。
3. **循环长度 vs 有效决策数比例偏低**。一条 60 分钟 run 里，真正"我选了一条不同的路"的决策点大约集中在：职业初始化 → hidden route 是否深入 → 关键 affix 搭配。第一和第三是 Phase 3 遗留能力；第二是 Phase 4 新增；**但中段"是否在这个区拖延多清一层 elite"** 这一经典 Roguelike 决策几乎没有差异化回报，因为多清 elite 并不显著改变 drop identity（除非落到 4 区升级过的 dynamic pool）。

---

## 三、战斗手感（Combat Feel）

### 3.1 好的部分

- **12 mutation × 5 家族 × 4 terrain preferred 的组合第一次有"敌人在地形里打出自己的牌"的观感**：
  - `emberblood / corrosion_cloud` 在 OIL 区真的会点燃 / 溅射；
  - `frostbound / tidebound` 在 WATER/ICE 区会主动制造打点差；
  - `void_mirror / dread_aura / war_caller` 在元素无关场合形成节奏控制。
  这已经不是 v4 基线 "mutation = 2 组 stat 加成 + 1 组 aura" 的阶段。
- **`terrainInteractionBatch` harness 把 terrain × mutation 的可观测覆盖进入 gate**，意味着战斗手感不再靠"策划口头描述"。

### 3.2 不好的部分 · 为什么

1. **AI_SHIFT 与 AURA 家族和 terrain 的联动仍然偏弱**。当前 `preferredTerrainTags` 命中集中在 ELEMENT_PACKAGE（emberblood / corrosion_cloud / frostbound / tidebound），AI_SHIFT（phase_runner / battle_drill）与 AURA（dread_aura / war_caller）并没有绑定合适的 terrain；玩家在没有明显元素层的 zone（如 `grey_gate_depths / abyssal_temple`）会感觉"精英差异退化为 stat 差异"。
2. **Elite 层数量感 vs 单体识别度**。12 mutation 是设计下限；但一局 run 中真正打到的独立 mutation 约 3–5 条；**没有"这只精英是 X 家族"的前台强提示**（参见 §UI），导致 mutation 12 的感知降维成 "stat 池 + 偶尔的 on-hit 惊喜"。
3. **玩家反打的"反制层"还没有**。Phase 4 为敌人加了 terrain 利用能力（点燃 OIL / ICE 打点差），但玩家侧的 **逆向利用 terrain 的工具** 仍然依赖现有 affix（DamageTypeBonus / DamageVsStatus），没有 "激活 terrain" 的主动技能或一次性 consumable。这让战斗呈现"敌人在玩 terrain、玩家在看 stat"的不对称。
4. **Boss 战是"短时长高 HP"**：阶段机制缺失导致 Boss 战的战斗心智是线性的，而不是 Roguelike 真正想要的"阶段切换 → 玩家决策更新 → 资源管理紧张"。

### 3.3 结论

> 战斗层从 v4 的 "被打但不会输" 升级为 "被打会紧张、但不会被吓到"；仍然不是 ToME 理想的 "敌人在用一套不同的语法攻击你"。

---

## 四、成长与 Build 感知（Growth & Build Identity）

### 4.1 好的部分

- **V2OPT-PR-02 的职业化分发骨架是对的方向**：`typeWeights / slotBias` 在升级过的 4 区 cadence/reward 显式配置，白盒 harness 对 `typeBias / slotBias` 有硬断，这让"法师区出 staff、弓手区出 bow"第一次不是纸面承诺。
- **Affix 池条件型被动（OnHit / OnKill / DamageVsStatus / DamageVsTag / DamageTypeBonus / HpRegenPerTurn）让 build 差异化从"纯数值堆叠"走到"条件触发"**，magic/rare item 的组合空间有了质的变化。

### 4.2 不好的部分 · 为什么

1. **UNIQUE/ARTIFACT 不是 build 锚点、只是高数值 RARE**。这是本次审阅最重要的一个结构发现。
   - 证据：`items/index.yaml` 中 `unique_*` / `artifact_*` 只有 `fixedAffixIds`，grep `uniquePassive / onHitTrigger / onKillTrigger / thresholdTrigger / perTurn / phaseTrigger` 为 0 命中。
   - 后果：所谓 "UNIQUE 武器" 实际上是 "一条预先搭配好的 affix set"，虽然稀有但并未带来 **让玩家围绕它重新组 build 的能力**；Roguelike / ARPG 品类的玩家会立即识别出 "这个 unique 没 gimmick"。
   - 连锁：`loot.*.secret.specialTemplateTagPreference` 与 `affixTagPreference` 在 V2OPT-PR-03 里被做得很严谨，但 pay-off 的核心载体（unique / artifact 的独特玩法）没有跟上，**secret reward identity 在最终端 collapse 回 "高分数值"**。
2. **动态池未覆盖区不产生 build 锚**。在 `molten_core / grey_gate_depths / crystal_cavern / underground_river.reward / abyssal_temple.reward / shattered_outpost / bandit_camp / elven_ruins` 这些仍是 `FIXED_LIST` 4 件清单的 profile 中，玩家收到的是 "该区最常见的 4 件 base"；没有 itemTagFilter 意味着**同一份 4 件清单打多次都不会变厚**。
3. **职业身份的感知层 (build 命名 / tag 聚集视觉提示) 尚未成型**。即便 affix 池厚、dynamic pool 骨架对，玩家在拾取瞬间**没有"这是在强化我当前 build"的可视化语言**，build identity 主要靠玩家自己对照数值来推断。
4. **资源轴深度未跟上打点深度**。Affix 加了"条件触发"，但 `OnKillResourceRestore` 外的资源类被动仍是稀的；build 里"被击伤 → 获得层数 → 特定阈值下爆发"的经典 ARPG 三段式在 Phase 4 当前只存在于概念，没有形成可凑的一条线。

### 4.3 结论

> 成长层从 v4 的"只有数值堆"升级为"有条件触发组合"，但 **高稀有度的"独立身份"这个 Roguelike 终极成长锚** 还缺失；build identity 在中后段会断掉。

---

## 五、奖励与 Loot 体验（Reward & Loot Feel）

### 5.1 好的部分

- **Secret reward identity 从"纸面合同"走到"白盒硬断"**：`whiteBoxLoot` 已显式断言每对 strict ceiling（`loot.abyssal_temple_warded_archive.secret <= 0.35` / `loot.deep_iron_slag_cache.secret <= 0.40` / `loot.deep_iron_smuggler_stash.secret <= 0.40`），意味着 "同区 cadence / reward / secret 三份产出不会互抄" 是数据保证，不是主观保证。
- **`recentRewards[*].detailText` + `recent reward source = SECRET_ZONE` 提供了 "反馈到玩家" 的第一层通路**：RewardPresentationText 已经把 cadence / reward / secret 分开文案（见 `client/.../RewardPresentationText.kt`）。

### 5.2 不好的部分 · 为什么

1. **"secret reward 与普通 reward 文案不同"这条最小合同，只实现了文案层，没实现"可视化层次"**。Ascii/tile renderer 的 golden 断言只要求"至少一条 reward detail 行进入 render"，不要求这条行在视觉上比普通 reward 条目更显眼（见 `client/src/test/.../GoldenScreenshotHarnessTest.kt`）；玩家可能在 3 条 reward 同时抵达时错过 secret 来源。
2. **Loot 的"峰值感"被 `FIXED_LIST` 压扁**。非升级区的 4 件清单在打到第二次时就已经被玩家背完，**第三次开宝箱时没有任何惊喜**；这是一个本质上和 Rarity 概率无关的问题——即便掉到 RARE，玩家已经知道它是那 4 件中的某件。
3. **Consumable 层缺乏 build 意义**。当前 CONSUMABLE 池仍以 `healing_potion / mana_potion / stamina_draught / energy_tonic` 为主，**没有 "临时给一回合 on-hit 能力" 或 "激活 terrain" 的 consumable**，导致宝箱中 CONSUMABLE 维的惊喜低于 WEAPON/ARMOR 维。
4. **PityTracker 对玩家不可见**。玩家无法感知 "我已经离下一次 RARE 很近"，即便底层 pity 合同是对的，前台没有任何暗示，这是典型的 "系统正确但体验没被放大"。

### 5.3 结论

> Loot 的"合同正义"做得漂亮，"体验正义"只做到一半；对"开宝箱"这个 Roguelike 最核心高潮动作，当前前台放大的幅度还不够。

---

## 六、探索与 Hidden / Secret 体验（Exploration）

### 6.1 好的部分

- 6 trigger × 14 event × 6 secret zone × entrance 三种 binding 已经让 "为什么隐藏" 的答案有至少 6 种不同理由；v4 基线 "只能靠 PERCEPTION_REVEAL" 的单调已不复存在。
- `organicHiddenProbe` 把 "非脚本玩家能否在合理回合数发现 hidden" 作为硬指标（`scriptedVerification=false / primerActionUsedCount=0`），这是一个非常成熟的设计决策。
- `greenwood_fringe` 第二锚点族让前 30 分钟的"熟悉感→陌生感"切换成立。

### 6.2 不好的部分 · 为什么

1. **Secret zone 的"身份"目前更多靠 loot profile 的 tag preference 而不是 zone 自身的 layout / terrain / encounter 独特性**。当前 `secret-zones/index.yaml` 的 6 条主要差异在 entrance binding 与 canonicalZoneId，**内部 layout 仍走母 zone 的 pattern 池**；玩家进入 secret zone 的视觉 "陌生度" 低于 loot 分析层的陌生度。
2. **Failed search 的反馈目前是文案层的，没有 "再尝试" 的弧**。冻结合同要求 "告诉玩家这里确实有内容但没过检定"，但玩家"第二次过关该检定"的方式要么是回头刷属性、要么是换队伍；**没有"临时 consumable 强行通过一次检定"** 这种典型 ARPG 补偿，导致失败反馈成为"卡点"而不是"下一次再来"。
3. **Hidden trigger 类型 6 种但 event 14 条，平均每个 trigger 约 2 条 event**，中后期区 hidden event 的 **同触发类型变化度低**；即便 trigger 维度多样，玩家在同一区多刷几次后会开始感到"又是这几条 hidden"。
4. **Discovery 的"时间性奖励"不足**。`firstHiddenDiscoveryTurnP50/P90` 指标只度量"多快发现"，不度量"发现早 vs 发现晚的玩家体验差异"；当前所有 discovery 都在 reward 层一次性结算，**没有 "早发现的玩家能带着发现物的加成进入下一段" 的延续感**。

### 6.3 结论

> 探索是本阶段改善最大的一维，已经接近 "合格 ToME-lite"；但距离 "每次探索都是一次小故事" 仍差 "Secret zone 内部差异化"+"failed search 二次弧"。

---

## 七、UI / 前台反馈（Readability / Frontstage）

### 7.1 好的部分

- **第一次有正式通道**：`RenderSnapshot.uiState.frontstageReadability` 与 `recentRewards[*].detailText` 形成"游戏内事件 → Session → snapshot → renderer"的单一路径（`FoundationGameSession -> Tile/Ascii render model`）。
- `AsciiRenderModelTest` / `TileRendererCanvasTest` 现在会断言**至少一条 frontstage content 行或 reward detail 行进入正式 canvas/model**，PR-05 close-out 口径是扎实的。

### 7.2 不好的部分 · 为什么

本节与 `docs/review/phase4/v2opt/2026-04-16-phase4-v2opt-pr-05-deep-review.md` 的 8 条发现对齐；这里按玩法影响排列。

1. **"Frontstage" 这个英文术语一直延伸到玩家可见面板**。PR-05 深审第 (1)(2) 条指出 i18n 中仍使用 "Frontstage" 作为玩家侧标题；这是典型的"从开发语义泄漏到玩家语义"，需要替换为 "当前关注 / 最近动作 / 关键提示" 等玩家语言。这是 Part 3 P1 之一。
2. **`addFrontstageMessage` 入口过宽**。任何系统都可以推消息进来，没有优先级与去重层；结果是玩家看到的 frontstage 内容缺乏"谁更重要"的区分。PR-05 深审第 (4) 条。
3. **`recentFrontstageActionCues` 无上限**。长 run 中越积越多，最新信号被旧信号稀释；这是 UI 最典型的"功能无 cap"错误，必须在本阶段加上限（建议 8–12 条滚动）。PR-05 深审第 (5) 条。
4. **Secret reward 与普通 reward 在 render 上缺分层**。客户端断言下限只要求 "至少一条 reward detail 行进入 render"，**不区分 secret vs cadence vs route**；多条 reward 同时抵达时，玩家会错过 "这条是 secret 来源" 的关键信号。与 V2OPT-PR-03 §5 "secret reward 文案必须和普通 route/cache reward 区分" 合同只完成一半。
5. **Frontstage readability 没有主动静默机制**。玩家正处于操作高峰（逃跑 / 使用 consumable 开溜）时，背景系统仍会推送 hidden primer / cadence reward 文案；没有基于 game state 的静默/延后，造成"该看清楚时看不清楚"。
6. **Ascii 与 Tile 两端视觉优先级不一致**。Ascii 走字符层、Tile 走 canvas 层，**当前 golden 断言覆盖两端的"存在性"但不覆盖"视觉权重相同"**；两端在 reward 呈现的显著性上可能出现偏差。

### 7.3 结论

> 前台刚从"没有正式入口"升级到"有正式入口"；下一跳必须做 "优先级 + cap + i18n 玩家化"，否则入口越通畅，噪音越多。

---

## 八、系统联动 & 终盘体验（System Coupling & Endgame）

### 8.1 好的部分

- V2OPT-PR-01 … PR-05 把 "owner metric + exit gate" 作为每条 PR 的硬合同，使 Phase 4 首次具备"玩法某一维劣化能被 gate 抓到"的能力。
- `verifyOwner` 作为默认 PR 联合闭环入口，意味着"提交前的内容不退化"这一编辑工作流是健康的。

### 8.2 不好的部分 · 为什么

1. **终盘 collapse 是本次审阅最刺眼的结构问题**。
   - Boss 仅 3 变体，无 phaseOverrides；
   - 终盘 zone `abyssal_temple / abyssal_heart` 的 reward 清单是 `FIXED_LIST`（`abyssal_heart.reward` 只有 `abyssal_heartstone` 一件）；
   - 终盘 Unique/Artifact 没独特玩法效果；
   三者叠加，终盘 30 分钟的玩家心智是 "数值验收"，不是 "决定性战斗"。
2. **Profession / Race 深度未在内容层延伸**。Organic hidden probe 已按 `4 profession × 3 released race × 11 seed` 做覆盖，但 profession/race 在 **战斗 / 成长 / 奖励 / 探索** 四维的观感层并没有对应差异——除了开局技能与初始装备，一条 orc (pending) / dwarf / human / elf 的 run 体验差别主要来自 loot RNG，而不是身份驱动的分支。
3. **Content Pack 没有跑出第二条内容线**。Overlay 合同完整，但 Phase 4 结算前只有一条 canonical pack；设计正文的 "第三方/测试 pack 扩展" 用例没出现可度量的示范，overlay 在产品层处于 "冷启动"。
4. **长 run 指标（`longRunLab`）与玩家"疲劳曲线"没有绑定**。当前 longRunLab 主要看"不崩溃、不死锁、不产生退化"；没有"60 分钟后的玩家决策密度" / "resource / consumable 消耗节奏" 指标，长 run 的 "好玩不好玩" 是灰箱。

### 8.3 结论

> 系统层的合同联动非常健康，但**"玩法层的终盘语言"还没有建立**——这是下一轮结构性收敛必须解决的。

---

## 九、玩家路径画像（30 分钟 / 60 分钟 / 90 分钟）

为了让 Part 3 的优先级有具体画像支撑：

- **0–30 分钟**：记忆点密度 **高**。前台反馈、hidden primer、第一次 secret reward、floor-aware profile 带来的 "第二层还是同一区但不一样"都在这一段形成。V2OPT-PR-05 的产出已经能撑住这一段。
- **30–60 分钟**：记忆点密度 **中**。玩家进入非升级区（7 个 FIXED_LIST 区中的某个），loot 惊喜下降；但 mutation 组合与 affix 条件触发仍能撑起战斗层手感；hidden / secret 继续供节奏。
- **60–90 分钟**：记忆点密度 **低 → 很低**。FIXED_LIST 区重复、Unique/Artifact 只是高数值、Boss 单阶段，玩家进入"打数字、算资源"的收尾模式。

**结论**：Phase 4 当前能撑住前 60 分钟，撑不住终盘 30 分钟。这是 Part 3 的核心靶子。

---

## 十、Part 2 小结

- **好的**：前期回放性、hidden 闭环、reward identity 合同、前台正式通道、mutation × terrain 交互、affix 条件型被动。
- **一般的**：动态 loot 覆盖、职业身份可视化、failed search 二次弧、Ascii/Tile 视觉权重一致。
- **差的**：Unique/Artifact 独占效果、Boss 阶段机制、非升级区 loot 峰值、前台信号优先级与 cap、终盘 collapse。

Part 3 将把上述 "差的" 与 "一般的" 条目转成 P0/P1/P2 优化提案（问题 / 作用域 / 目标 / 具体修改 / ROI / 风险 / 对齐点）。

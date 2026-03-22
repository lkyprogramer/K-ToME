# Phase 2 玩家可感知优化 PR 级开发计划

**生成日期**: 2026-03-22  
**适用基线**: `Stage A ~ E` 已完成、当前 `Phase 2` 主 gate 已恢复绿色的主线  
**定位**: 吸收深度审查中仍然有效、且确实属于当前 `Phase 2` 的玩家侧优化项，把它们改写成一组可直接开发、可独立验收的 PR 计划

---

## 1. 计划定位

这份计划是 [2026-03-21-phase2-post-review-optimization-pr-plan.md](/Users/luo/Documents/github/K-ToME/docs/review/phase2/2026-03-21-phase2-post-review-optimization-pr-plan.md) 的补充，不重开 `Stage A ~ E` 已关闭的合同问题，也不把 `Phase 3/4/5` 的系统偷带回 `Phase 2`。

它只处理 5 类当前仍然成立、且 ROI 明显偏高的体验缺口：

1. 已解锁天赋没有进入正式可操作热栏
2. `DamageType` 管线已接通，但抗性数据覆盖仍偏稀
3. zone 描述 key 已存在，但没有真正进入玩家主路径
4. 被动装备框架已成立，但非 accessory 装备身份仍偏弱
5. 前期遭遇组合和高信息量反馈仍不够显著

本计划默认遵守以下边界：

1. `Phase 2` 仍以“4 职业、4 zone、24 怪、24 物品、Tile/i18n 正式短局”作为主目标
2. 不提前引入 `TalentAllocationDraft`、树形 UI、respec、完整 difficulty、随机事件、群体 AI、经济循环或长局世界分支
3. 所有新行为都继续走 typed snapshot / log token / schema / manifest 真源，不在 `client` 或临时 fixture 中维护第二套规则

---

## 2. 当前真相与吸收结论

以下结论是基于当前代码与权威文档交叉核实后的结果，而不是沿用旧审查结论：

1. `Stage E` 已完成，不再属于待做项
2. `statGrowth` 升级日志已经落地，不再属于待做项
3. `Phase 2` 当前真正缺的不是“完整天赋树学习系统”，而是“已解锁天赋如何进入正式可操作热栏”
4. 近战职业并不是完全没有 gap closer / 2+ 格 answer：`Vanguard.charge`、`Rogue.roll / shadowstep`、`Templar.judgment_hammer` 都已存在于 talent 数据中；真正的问题是 `Phase 2` 正式热栏只支持 `1-4` 且缺少 remap/equip 流程，导致这些 answer 在解锁后不稳定进入玩家可操作路径
5. `KITE` 怪物的 `preferredRangeStart=2`、`preferredRangeEnd=3` 会把这个问题集中暴露在前期远程遭遇里；因此需要同时修“已解锁但不可操作”和“前期过早放大无还手窗口”这两层
6. 怪物抗性“家族最低覆盖线”已满足，但“路线内可感知覆盖”仍然偏弱
7. zone `descKey` 已存在于 schema/i18n，但没有稳定进入 snapshot / log / HUD 的正式显示路径
8. 被动框架已经够用，本阶段不需要再引入新的 passive kind，只需要扩大少量正式数据覆盖
9. 前期遭遇问题优先从 `game` 的生成/配置层解决，而不是提前做 `Phase 5` 的战术 AI

---

## 3. 为什么这些项仍应留在 Phase 2

### 3.1 应留在当前阶段的理由

1. 它们都直接影响玩家是否“看得到、用得到、记得住”当前 `Phase 2` 已经实现的系统
2. 它们主要是 `loadout 操作性 / 数据填充 / snapshot 呈现 / client 显著性 / 遭遇配置`，不要求引入新的长期核心抽象族
3. 它们能明显提升 `SoloClearLab`、默认短局和白盒体验的一致性

### 3.2 不应越界到后续阶段的内容

以下内容继续明确留在后续阶段，不进入本计划：

1. `Phase 3 / P3-W3`
   - talent tree V2
   - prerequisite
   - `TalentAllocationDraft`
   - respec / rollback
   - 断点成长预览
2. `Phase 3 / P3-W6`
   - affix v1
   - 固定经济循环
   - 长局掉落驱动构筑
3. `Phase 4`
   - 随机事件
   - hidden event / secret zone
   - loot ecology v2
4. `Phase 5`
   - 群体战术 AI
   - 感知/仇恨/潜行
   - replay/death analysis 深化

---

## 4. 规划原则

后续 PR 必须遵守：

1. 优先修“玩家无法感知或无法操作”的最后一公里，不重新打开已完成的结构性重构
2. 每个 PR 最多同时触碰两个生产模块；若超过，需要继续拆
3. 优先复用已有 typed contract，不再引入平行模型
4. `game` 继续只输出 key / token / snapshot 语义，不生成最终本地化字符串
5. `client` 只负责表现与输入，不推导规则真相
6. 数据扩展优先于公式扩展；内容补全优先于新系统

---

## 5. 依赖关系与建议顺序

```text
PR-F1 (已解锁天赋进入可操作热栏 / loadout remap)
  └── 不依赖其余 PR，可独立先做

PR-F2 (扩大抗性数据覆盖)
  └── 可与 PR-F1 / PR-F3 并行

PR-F3 (zone 入口描述呈现)
  └── 可与 PR-F1 / PR-F2 并行

PR-F4 (少量武器/防具补被动身份)
  └── 建议在 PR-F2 后做，确保 passive 有明确目标场景

PR-F5 (前期遭遇组合 + 反馈显著性)
  └── 建议最后做，消费 PR-F2 / PR-F3 / PR-F4 已提供的内容信号
```

推荐顺序：

1. `PR-F1`
2. `PR-F2`
3. `PR-F3`
4. `PR-F4`
5. `PR-F5`

---

## 6. PR 计划

### PR-F1：已解锁天赋进入可操作热栏与近战 answer 接入

**目标**

让 `Phase 2` 已自动解锁的职业天赋，能够稳定进入正式可操作热栏，并允许玩家在不进入 `Phase 3 talent tree` 的前提下重排 loadout；其中必须显式覆盖近战职业的追击/中程 answer 接入。

**为什么是当前 Phase 2 问题**

当前职业天赋会随等级自动解锁，但正式输入只支持 `1-4` 号热键，缺少最小 remap/equip 流程。结果不是“天赋没实现”，而是“天赋没有进入正式可操作路径”。这对近战职业影响尤其明显：`charge / judgment_hammer / shadowstep` 这类追击或中程 answer 在解锁后无法稳定进入活跃热栏。

**建议范围**

- [GameView.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameView.kt)
- [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
- [RenderSnapshot.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt)
- [InputHandler.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/input/InputHandler.kt)
- [TileRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt)
- [AsciiRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt)
- 对应 `:game:test` / `:client:test` / `goldenScreenshot`

**冻结决策**

1. `Phase 2` 继续保持“等级自动解锁”，不实现 learn-from-tree
2. 正式战斗热栏固定为 `4` 个 active slot，不提前扩成 `6` 个战斗热键
3. `TalentLoadout.talentLevels` 代表“已解锁 talent 集”；reserve talents 由 `talentLevels.keys - slotToTalentId.values` 推导，不新增第二套 unlocked container
4. 新解锁天赋在 `1-4` 已占满时进入 reserve，不再自动追加到 `5/6/...` 这种无正式输入绑定的虚空槽位
5. 新增单独的 `loadout edit` 模式，默认使用 `L` 打开；`T` 继续只用于 talent point 分配
6. cooldown 继续按 `talentId` 追踪；remap 不得重置冷却
7. `PR-F1` 必须显式覆盖 melee professions 的追击/中程 answer 接入，不只是抽象 remap
8. `Phase 2` 不改 `charge / judgment_hammer / roll / shadowstep` 的数值和 `unlockLevel`，只解决“解锁后能否正式操作”
9. reserve talent 中若包含 mobility / ranged answer，必须能被玩家替换进 active slot

**推荐实现**

1. 为 `PlayerCommand` 增加最小 loadout 配置命令，例如：
   - `OpenLoadout`
   - `EquipTalentToSlot(slot, talentId)`
   - 或两段式 `BeginLoadoutReplace(slot)` / `ConfirmLoadoutReplace(talentId)`
2. 在 snapshot 中补一组“已解锁但未装备”的 reserve talent 视图
3. `FoundationGameSession.syncUnlockedPlayerTalents()` 改为：
   - 初始化时只填满 `1-4`
   - 之后只维护 `talentLevels`
   - reserve 由现有 `talentLevels` 推导
4. `client` 增加最小 loadout 编辑界面：
   - 左侧显示 `1-4` 当前装备
   - 右侧显示 reserve
   - 支持选择 reserve 替换 active slot
5. reserve 列表和 loadout edit 文案需要明确呈现“追击 / 中程 answer”类 talent，避免玩家升级后仍不知道 `charge / judgment_hammer / shadowstep` 已可装备

**非目标**

1. 不做 tree graph UI
2. 不做 prerequisite
3. 不做 respec / rollback
4. 不做第 `5/6` 号正式战斗热键
5. 不改天赋数值平衡

**验收标准**

1. 任一职业升级后新增 talent 会出现在 reserve，而不是进入无热键槽位
2. 玩家可以把 reserve talent 装入 `1-4` 任一 active slot
3. `Vanguard` 到 `level 3` 后，`charge` 可进入 `1-4` 任一槽位并正常施放
4. `Templar` 到 `level 2` 后，`judgment_hammer` 可进入 `1-4` 任一槽位并正常施放
5. `Rogue` 的 `roll` 保持起始可用；`shadowstep` 在解锁后可进入 active slot
6. remap 后，目标高亮、`range/minRange` 校验、cooldown、save/load 都保持正确
7. HUD / sidebar / controls 会明确展示 loadout edit 入口，避免玩家不知道如何把新解锁技能装上

**验证**

- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest`
- `./gradlew :game:test --tests com.ktome.game.RenderSnapshotContractTest`
- `./gradlew :client:test --tests com.ktome.client.screen.* --tests com.ktome.client.render.*`
- `./gradlew clientSmoke`
- `./gradlew goldenScreenshot`
- `./gradlew soloClearLab`

建议新增覆盖点：

1. `FoundationGameSessionTest`
   - `charge` 解锁后进入 reserve，可被装备到 active slot
   - `judgment_hammer` 解锁后进入 reserve，可被装备到 active slot
   - remap 后使用 talent 命中目标，cooldown 不丢
2. `RenderSnapshotContractTest`
   - reserve talents 与 active talents 的 snapshot 语义稳定
3. `client` render / input tests
   - loadout edit 模式能完成替换，controls 文案正确

---

### PR-F2：扩大抗性数据覆盖到路线内可感知深度

**目标**

把当前“家族最低覆盖线”升级为“路线内可感知覆盖”，让 `DamageType`、抗性日志和相关被动在默认短局中更常被真正看到。

**为什么是当前 Phase 2 问题**

当前抗性框架已经工作，但非零抗性模板仍偏少。问题不是公式没接通，而是路线内可感知样本还不够。

**建议范围**

- [monsters/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/monsters/index.yaml)
- [MonsterSchemaTest.kt](/Users/luo/Documents/github/K-ToME/game/src/test/kotlin/com/ktome/game/data/MonsterSchemaTest.kt)
- 如需补 coverage summary，可增一条 `:game:test`

**冻结决策**

1. 不把目标写成“24/24 模板都必须非零抗性”
2. 本阶段只做“route 主线怪物的弱差异抗性”，不做模板级极端免疫
3. 单模板最多 `2` 个非零抗性
4. 非零值继续限制在 `[-25, +25]`
5. 继续保留 `beast` 的低抗性/零抗性基线，不强行为了覆盖率给所有兽类加元素差异

**推荐覆盖线**

1. 每个 zone 至少 `3` 个模板具备非零抗性
2. 每个 zone 至少覆盖：
   - `1` 个普通怪
   - `1` 个 signature / ranged / caster 怪
   - `1` 个 elite 或 boss 级模板
3. 让以下路线记忆点都有真实目标：
   - `FIRE` talent / passive
   - `SHADOW` 抗性 / vulnerability
   - `HOLY` 对 undead / cultist 的 answer

**非目标**

1. 不改 `CombatResolver`
2. 不引入穿透、破抗、递减等 Phase 3 公式
3. 不冻结每只怪的精确数值，只冻结 coverage 线

**验收标准**

1. 当前 `4 zone` 中，每区至少 `3` 个模板具有非零抗性
2. runtime 投影测试基于 `loadMonsterCatalog()` 校验，不只停留在 schema 原文
3. `resisted / vulnerable` 日志路径继续有自动化断言
4. 现有被动或职业 answer 至少能在默认短局中多次命中对应目标

**验证**

- `./gradlew :core:test --tests com.ktome.core.combat.CombatResolverTest`
- `./gradlew :game:test --tests com.ktome.game.data.MonsterSchemaTest`
- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest`
- `./gradlew soloClearLab`

---

### PR-F3：zone 入口描述进入正式玩家路径

**目标**

把 zone 现有 `descKey` 从“仅存在于 schema / i18n”升级为“进入 snapshot 与入口提示”的正式玩家路径。

**为什么是当前 Phase 2 问题**

zone 描述 key 已经存在，但当前玩家只稳定看到 zone name，看不到“这个地方是什么、为什么危险”的最小文本锚点。

**建议范围**

- [RenderSnapshot.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt)
- [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
- [TileRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt)
- [AsciiRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt)
- 对应 i18n / screenshot / render tests

**冻结决策**

1. 继续复用 zone schema 现有 `descKey`
2. 入口文本必须走 typed key/token，不允许在 `game` 层拼最终字符串
3. 每个 zone 只在“首次进入该 zone”时主动提示一次；同 zone 的楼层切换不重复刷屏
4. snapshot metadata 应允许 client 持续拿到当前 zone `descKey`
5. 不引入 quest / dialogue / narrative system

**推荐实现**

1. 在 snapshot metadata 增加 `zoneDescKey`
2. session 在开局进入 zone、以及 route 切到新 zone 时发出 `log.zone.enter`
3. HUD / sidebar 至少提供一种可见路径显示 zone 描述：
   - header 副标题
   - inspect / sidebar 顶部二行简介
   - 或最近日志首条

**非目标**

1. 不做剧情过场
2. 不做区域任务系统
3. 不做复杂世界观文本面板

**验收标准**

1. 四个 zone 在第一次进入时都有正式入口提示
2. 当前 zone 描述能从 snapshot metadata 稳定取到
3. locale 切换后描述继续正确本地化
4. save/load 不会重复错误触发 zone 入口提示

**验证**

- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest`
- `./gradlew :game:test --tests com.ktome.game.RenderSnapshotContractTest`
- `./gradlew :client:test --tests com.ktome.client.render.*`
- `./gradlew goldenScreenshot`
- `./gradlew clientSmoke`

---

### PR-F4：给少量武器/防具补被动身份

**目标**

在不打开 `Phase 3/4` 装备深水区的前提下，把被动身份从“4 件 accessory”扩成“少量非 accessory 正式装备”，让武器/防具不再全部退化成数值棒。

**为什么是当前 Phase 2 问题**

当前 passive contract 已经完整，但 4 件带被动装备全部是 `OFF_HAND` accessory 类装备。玩家能感知到被动系统存在，却仍然很少在武器/防具更换时产生真正的路线决策。

**建议范围**

- [items/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/items/index.yaml)
- 只在必要时补测试，不新增 passive kind

**冻结决策**

1. 继续只使用现有 `4` 种 passive：
   - `DamageVsTag`
   - `HpRegenPerTurn`
   - `DamageTypeBonus`
   - `ResistanceBonus`
2. 本 PR 至少新增：
   - `2` 件 weapon
   - `2` 件 armor
3. 新 passive item 应尽量 route 锚定，而不是泛化成“全池随机泛滥”
4. 优先选与 `PR-F2` 抗性覆盖能形成互动的装备

**推荐方向**

1. 一件偏 `bandit / human-like` 路线的武器
2. 一件偏 `undead / cultist` 路线的 `HOLY` 或 `SHADOW` 相关护甲
3. 一件给矿坑/熔炉环境的 `FIRE` 防护或加成 armor
4. 一件偏持续作战或机动 answer 的 armor sidegrade

**非目标**

1. 不新增 proc / on-hit trigger
2. 不新增 sustain 装备系统
3. 不做 affix v1
4. 不重做 item generator

**验收标准**

1. 至少 `4` 件非 accessory 装备带被动
2. 默认短局内能真实出现这些装备，不是永远只能在数据里看到
3. inspect / inventory list / pickup/drop log 继续沿用现有 passive 描述路径
4. 不破坏现有 `signature reward` 的 identity

**验证**

- `./gradlew :game:test --tests com.ktome.game.FoundationGameSessionTest`
- `./gradlew :game:test --tests com.ktome.game.RenderSnapshotContractTest`
- `./gradlew soloClearLab`
- `./gradlew goldenScreenshot`

---

### PR-F5：前期遭遇组合、近战对 kite 的最小基线与反馈显著性

**目标**

在不引入群体 AI 和不抬升公式复杂度的前提下，让前 `2~3` 层战斗少一点“单房单怪走过场”，同时补上近战对 kite 的最小公平性基线，并把高信息量反馈做得更显眼。

**为什么是当前 Phase 2 问题**

当前前期战斗偏“单房单怪 + 统一白色日志”，系统是对的，但低层压力和反馈信号都偏平。对近战职业来说，`KITE` 的 `2~3` 格偏好距离会把“还没拿到追击或中程 answer”的阶段放大成真实无还手窗口。最合适的修法不是引入新 AI，而是做一层生成/配置调优与 client 呈现显著化。

**建议范围**

- [GameModule.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/GameModule.kt)
- [monsters/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/monsters/index.yaml)
- [zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/zones/index.yaml)
- [TileRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt)
- [AsciiRenderModel.kt](/Users/luo/Documents/github/K-ToME/client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt)
- 如需要，少量 client render tests / game module tests

**冻结决策**

1. 不做群体 AI、增援 AI、编队 AI
2. 不通过单纯加数值把低层怪变成硬吃药检查
3. 本 PR 不改 `AIType.KITE` 规则，不把怪物改笨
4. 本 PR 不新增近战新技能，不提前做 `Phase 3` talent 设计
5. 解决方式优先是 encounter/content 约束，而不是公式或 AI 重写
6. `Phase 2` 的最小公平性基线是：
   - 在 `Vanguard` 拿到 `charge`、`Templar` 拿到 `judgment_hammer` 之前
   - 默认前期 route 不能持续制造“玩家没有任何可操作 answer，却被基础 kite 怪反复白打”的主路径体验
7. 前期压力优先来自：
   - 更早出现的 ranged / controller mix
   - 小规模双怪/三怪同房压力
   - elite 更早作为节奏点出现
8. 反馈显著性先留在 `client` 渲染侧完成，不新增 `RenderSnapshot` 第二套日志合同
9. `client` 可以基于已有 `message.key` 家族做 tone 分类，但不能反向推导规则数值

**推荐实现**

1. `game`
   - 调整 `zoneMonsterSpawnCount()` 和 `spawnMonsters()`：
     - `shattered_outpost`、`greenwood_fringe` 的前几层允许有限度的小 pack 生成
     - room 足够大时，允许一个 room 出现 `2` 只相邻怪
   - `shattered_outpost` 的 floor 1 怪物选择增加约束：
     - floor 1 默认只允许极低密度的 `KITE` 普通怪
     - 不让 floor 1 的普通遭遇稳定退化成“开场就追 archer”
   - 调整少量早期 `KITE` 模板的 `spawnWeight / spawnFloors`
     - 目标不是删除 ranged pressure
     - 目标是把高频 kite 压力稍微后移到玩家已有最小 answer 的阶段
   - 保留 `greenwood_fringe` 之后的 ranged/kite 压力，因为那时：
     - `Vanguard` 已可拿 `charge`
     - `Templar` 已可拿 `judgment_hammer`
     - `Rogue` 本来就有 `roll`
2. `client`
   - 为以下 key 家族提供更显眼 tone：
     - `log.talent.damage_resisted`
     - `log.talent.damage_vulnerable`
     - `log.passive.*`
     - `log.level_up*`
     - `log.zone.enter`
     - `log.boss.*`

**非目标**

1. 不引入新 log schema
2. 不引入 telegraph 系统扩写
3. 不改 `CombatResolver`
4. 不重写 `AIType.KITE`
5. 不做 `Phase 5` 战术 AI

**验收标准**

1. `shattered_outpost` floor 1 不再高频出现“近战被基础 kite 怪无 answer 风筝”的默认主路径
2. 前 `2~3` 层不再严格退化成“每房 1 只怪”的默认模式
3. 默认短局中可以稳定出现至少几种“melee + ranged / support”组合
4. `Vanguard` 在拿到 `charge` 后，默认短局中能真实遇到需要追击的 ranged/kite 场景
5. `Templar` 在拿到 `judgment_hammer` 后，默认短局中能真实用它处理 ranged/kite 目标
6. `Rogue` 的 `roll` 不被后续 loadout 逻辑误挤出主路径
7. 高信息量日志在 Tile / ASCII 中不再全部是同色同优先级
8. `client` 的显著化不依赖硬编码最终文案，只依赖现有 `message.key`

**验证**

- `./gradlew :game:test --tests com.ktome.game.GameModuleTest`
- `./gradlew :client:test --tests com.ktome.client.render.*`
- `./gradlew clientSmoke`
- `./gradlew soloClearLab`
- `./gradlew goldenScreenshot`

建议新增覆盖点：

1. `GameModuleTest`
   - 早期 floor 的 monster selection 对 `KITE` 有上限或权重约束
2. `soloClearLab`
   - `foundation_frontliner`、`foundation_judicator` spot-check 至少一条“ranged answer 可操作”路径
3. 白盒
   - `Vanguard` 升到 `3` 级后把 `charge` 装进热栏并追上 archer
   - `Templar` 升到 `2` 级后把 `judgment_hammer` 装进热栏并命中 kite 怪

---

## 7. 文档同步要求

这组 PR 完成后，至少需要评估以下文档是否要回写：

1. [2026-03-21-phase2-post-review-optimization-pr-plan.md](/Users/luo/Documents/github/K-ToME/docs/review/phase2/2026-03-21-phase2-post-review-optimization-pr-plan.md)
   - 若 `PR-F1` 落地，需要补一句：`Phase 2` 已解锁 talent 通过最小 loadout remap 进入正式可操作路径
2. [2026-03-13-phase2-verification-checklist.md](/Users/luo/Documents/github/K-ToME/docs/phase2/2026-03-13-phase2-verification-checklist.md)
   - 若 `PR-F1` 落地，建议增加一条白盒检查：升级后 reserve talent 能进入热栏并实际施放
   - 若 `PR-F3` 落地，建议增加一条白盒检查：zone 首次进入提示只触发一次
3. 这份计划自身
   - 如果后续吸收其中某些 PR，需要把完成态和剩余项回写，避免再次出现“报告比代码旧”的情况

---

## 8. 最终建议

如果只做一轮最小但高收益的 `Phase 2` polish，建议先做：

1. `PR-F1`
2. `PR-F2`
3. `PR-F3`

这三项解决的是：

1. 玩家能不能用到已经实现的天赋
2. `DamageType` 能不能在真实短局里更常起作用
3. zone 身份能不能进入玩家认知

`PR-F4` 和 `PR-F5` 继续是高 ROI，但它们更像“在当前闭环上再往前推半步”的增强，而不是进入 `Phase 3` 前的结构性补洞。

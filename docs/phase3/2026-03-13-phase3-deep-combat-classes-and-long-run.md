> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md`
> `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`

# Phase 3 - Deep Combat, Classes & Long Run

**阶段**: `Phase 3`  
**版本目标**: `v0.3.x`  
**优先级**: `P0`  
**前置条件**: `Phase 2` 出口全部满足  
**对应问题**: Phase 2 已经有正式合同和短局切片，但战斗深度、职业构筑、脚本化 AI、Boss telegraph 和世界长局结构仍然不足，无法支撑 4~6 小时 run。

---

## 1. 阶段目标

把 Phase 2 的“可验证短局”升级为“可构筑的长局”。

完成标准：

1. 冻结正式战斗公式、`Power/Save`、状态生命周期、元素交互首批规则。
2. 建立 `TalentTree V2`、动态说明、关键词注册表和 respec/rollback 验证。
3. 让 `4 基础职业` 进入正式树，并至少让 `2 个进阶职业` 成为可玩内容。
4. 建立 `AIProfile DSL`、Boss telegraph、Boss phase 和长局世界分支。
5. 形成 `4~6` 小时单人长局，并有稳定回归入口。

## 2. 当前问题

1. Phase 2 的战斗仍偏“合同已建、深度未落地”。
2. 状态系统框架存在，但尚未大规模进入职业、Boss 和世界危害主线。
3. 职业 schema 已定义，但还没有正式树、断点成长和 respec 验证。
4. 短局内容可以通关，但还没有长局世界分支和构筑驱动的掉落循环。

### 2.1 本阶段必须冻结的系统

1. `命中/暴击/PowerSave/护甲/抗性/穿透/护盾` 正式公式。
2. `CombatTrace` 正式字段与金样本。
3. `StatusEffectDef` 的完整生命周期、堆叠、驱散、免疫规则。
4. `TalentTree V2`、`targeting/telegraph/effect op` 语法。
5. `AIProfile DSL`、`BossEncounter`、`BossPhaseDef`。
6. 职业 roster 和资源轴边界。

说明：

1. `4 个进阶职业` 的 schema、资源、定位与 profile 必须在本阶段冻结。
2. `2 个进阶职业` 必须在本阶段进入可玩路径。
3. 另外 `2 个进阶职业` 若内容量不足，可在本阶段尾部或下阶段初期补完内容，但 contract 不允许再漂移。

## 3. 范围与非目标

### 3.1 范围

1. 战斗公式 V2 与 `CombatTrace` 金样本。
2. 状态/持续/姿态/标记/护盾/zone effect 正式化。
3. `4 基础职业` 正式树与 `2 进阶职业` 可玩化。
4. `AIProfile DSL`、Boss telegraph、Boss phase。
5. 世界分支、zone 入口、任务主支线、affix v1、经济循环。

### 3.2 非目标

1. 不在本阶段进入深度 ProcGen。
2. 不在本阶段引入完整 content pack 平台。
3. 不在本阶段做最终商业级内容量。

## 4. 技术方案

### 4.1 战斗公式 V2

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/combat/*
core/src/test/kotlin/com/ktome/core/combat/*
tools/src/main/kotlin/com/ktome/tools/golden/CombatTraceGolden.kt
```

冻结口径：

1. 命中、暴击、`Power/Save`、护甲、抗性、穿透、护盾顺序全部固定。
2. 首批元素交互只启用 `FIRE/COLD/HOLY/SHADOW` 的核心规则。
3. 每类关键战斗都必须有 `CombatTrace` 金样本：
   - 普攻
   - 暴击
   - 元素减伤
   - 护盾吸收
   - 状态施加成功/失败
   - 元素交互触发

必须补齐的正式公式合同：

1. 命中从 Phase 2 的线性模型升级到 Sigmoid：
   - `hitChance = clamp(0.05 + 0.90 * sigmoid(0.04 * (accuracy - evasion + 10)), 0.05, 0.95)`
2. 暴击固定第一版口径：
   - 基础暴击率 `5%`
   - 暴击率上限 `50%`
   - 基础暴击倍率 `1.5`
   - `critResistance` 直接抵扣有效暴击率
3. 物理减免固定为 `armor / (armor + 100)`，元素抗性固定为 `clamp(resistance - penetration, -25, 75)`。
4. `Power/Save` 固定为：
   - `applyChance = clamp(0.10 + 0.80 * sigmoid(0.05 * (power - save)), 0.10, 0.90)`
5. 收益递减只作用于二级属性，不作用于 `STR/DEX/CON/WIL` 本身；首批常量固定为：
   - `evasion C=150`
   - `critRating C=200`
   - `castSpeed C=100`
   - `hpRegen C=80`
6. `CombatPipeline` 固定为 `12` 步有序管线，child trace、callback 优先级和 miss/cleanse/elemental interaction 的挂点必须保持可追踪。
7. Phase 3 切换到上述正式公式后，Phase 2 的 `CombatTrace` / Golden Seed 基线必须全量重录，这是预期内破坏性变更。

### 4.2 状态、持续与回调生命周期

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/status/*
core/src/test/kotlin/com/ktome/core/status/*
game/src/main/resources/data/statuses/*.yaml
```

冻结口径：

1. `STUN / ROOT / SILENCE / BLEED / BURN / CURSED / GUARD / STEALTH / BANE` 进入正式主线。
2. sustain、mark、ward、zone effect 都必须走统一生命周期，不允许再写技能私有特判。
3. 清理、驱散、免疫和持续 tick 时机必须能在 trace 里定位。

状态矩阵还必须明确以下第一版规则：

1. 不叠加、只刷新持续时间：
   - `STUN`
   - `SLOW`
   - `FREEZE`
   - `SILENCE`
2. 独立叠层：
   - `BLEED`
   - `BURN`
   - `POISON`
3. 上限封顶：
   - `ARMOR_BREAK` 最多 `3` 层
4. 取较强值：
   - `SHIELD`
   - `REGEN`
5. 互斥/覆盖：
   - `FREEZE` 与 `BURN` 互斥，并触发元素交互
   - `HASTE` 与 `SLOW` 互相抵消
   - `STEALTH` 被任意 AoE 伤害打破
6. 净化优先级必须可配置，并明确不可被净化的效果集合。

### 4.3 Talent Tree V2 与动态说明

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/talent/*
game/src/main/resources/data/talents/*.yaml
client/src/main/kotlin/com/ktome/client/ui/talent/*
```

冻结口径：

1. talent schema 必须支持：
   - rank
   - breakpoint
   - prerequisites
   - targeting
   - telegraph
   - typed effect op
2. 动态说明只从 schema 和实际数值推导，不允许手写第二套文本逻辑。
3. `respec` 与 `rollback` 必须有自动化回归。

### 4.4 职业与 build 深度

本阶段职业目标：

1. `Vanguard / Arcanist / Rogue / Templar` 全部进入正式树。
2. 至少 `Berserker / Spellblade` 进入可玩路径。
3. `Shadowblade / Warden` 的 schema、资源轴、技能语义、定位必须冻结。
4. `3` 个种族和本地 Profile 进入主线验证。

冻结口径：

1. 每个职业最多 `2` 条资源轴。
2. 每个职业至少有：
   - 1 条生存支线
   - 1 条输出支线
   - 1 条控制或机动支线
3. 所有职业必须有清晰的 panic answer、位移方案和 boss answer。
4. 进阶职业解锁条件固定第一版：
   - `Berserker`：`Vanguard` 通关任意难度
   - `Spellblade`：`Arcanist` 通关任意难度
   - `Shadowblade`：`Rogue` 通关任意难度
   - `Warden`：`Templar` 通关任意难度
5. 种族天赋点独立于职业天赋点，每 `4` 级获得 `1` 点。
6. 铭文系统在 Phase 3 进入 build 轴，最小合同固定为：
   - 最大铭文数 `4`
   - 同类最多 `2`
   - 热键 `5~8`
   - 不消耗主资源，只受冷却控制

### 4.5 AIProfile DSL 与 BossEncounter

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/ai/*
game/src/main/resources/data/ai/*.yaml
game/src/main/resources/data/boss/*.yaml
client/src/main/kotlin/com/ktome/client/telegraph/*
```

冻结口径：

1. AI 继续走脚本化 DSL，不进入行为树平台化。
2. Boss 高伤技能必须有 telegraph。
3. `AIDecisionTrace` 与 `BossTrace` 必须可导出。
4. AI 不允许依赖作弊式全图透视，只能基于可感知状态和最后已知信息。

### 4.6 世界分支与长局结构

建议文件与模块：

```text
game/src/main/resources/data/zones/*.yaml
game/src/main/resources/data/world/*.yaml
core/src/main/kotlin/com/ktome/core/world/*
```

冻结口径：

1. 长局必须有主支线、分支 zone、经济循环和至少一层 affix 驱动。
2. `4~6` 小时 run 必须能稳定结束，而不是无限拖长。
3. 掉落与构筑要形成正反馈，但不追求最终生态复杂度。

长局主分支的第一版示意必须固定，至少覆盖：

1. `shattered_outpost -> greenwood_fringe -> deep_iron_pit -> grey_gate_depths`
2. `精灵遗迹 / 熔岩核心 / 盗贼营地` 等可选支线
3. `地下河 -> 水晶洞穴 -> 深渊神殿 -> 深渊之心` 的 Phase 3 扩展终线

`affix v1` 的最低预算也必须冻结：

1. 武器前缀 `12`
2. 武器后缀 `10`
3. 防具前缀 `10`
4. 防具后缀 `8`

## 5. 推荐 PR / 工作包拆分

### P3-W1 Combat Formula & Trace Goldens

1. 战斗公式 V2
2. `CombatTrace` 扩展
3. trace golden harness

### P3-W2 Status Lifecycle

1. sustain/mark/ward/zone effect
2. stack/dispel/immunity
3. 状态 UI 语义同步

### P3-W3 Talent Tree V2

1. tree schema
2. 动态说明
3. respec/rollback

### P3-W4 AIProfile & Boss Telegraph

1. AI DSL
2. `BossEncounter`
3. telegraph renderer
4. boss trace

### P3-W5 Class Formalization

1. 4 基础职业正式树
2. 2 个进阶职业可玩化
3. 3 个种族与 Profile

### P3-W6 Long-Run World Structure

1. 世界分支
2. zone 入口
3. affix v1
4. 经济循环
5. 长局回归实验室

## 6. 测试与自证

### 6.1 必测模块

1. `core.combat`
2. `core.status`
3. `core.talent`
4. `core.ai`
5. `core.world`
6. `game.data.talents`
7. `game.data.boss`

### 6.2 必测行为

1. `CombatTrace` 在固定输入下稳定。
2. 状态堆叠、驱散、免疫和 tick 顺序稳定。
3. talent 动态说明与实际数值一致。
4. respec/rollback 不破坏 build。
5. Boss telegraph 和 phase 切换可稳定复现。
6. 4~6 小时 run 可稳定收敛到死亡或通关。

### 6.3 自动化命令

Phase 3 必须建立或补齐以下入口：

```bash
./gradlew test
./gradlew :core:test
./gradlew combatTraceGolden
./gradlew bossHarness
./gradlew soloClearLab
./gradlew longRunLab
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

### 6.4 白盒验证

1. 进入任一长局默认 seed。
2. 观察至少一次 Boss telegraph，并确认：
   - 提示出现
   - 玩家可规避
   - 伤害与 trace 对齐
3. 用 `Vanguard`、`Arcanist`、`Rogue`、`Templar` 各完成一次长局。
4. 用至少 `2` 个进阶职业完成一局关键 Boss 路径。
5. 做一次洗点/回滚，确认 talent、资源和描述同步更新。

## 7. 出口门禁

1. `CombatTrace` 金样本全绿。
2. 4 基础职业和 2 进阶职业通过 `SoloClearLab` 与关键 Boss 回归。
3. `4~6` 小时 run 可以稳定结束。
4. telegraph、AI、Boss phase、状态生命周期都可白盒验证。
5. 关键包 coverage gate 达标，且没有以跳过 trace/golden 方式规避门禁。

## 8. 风险与止损

1. 如果公式和动态说明不同步，优先修正 schema/description pipeline，暂停补内容量。
2. 如果 Boss telegraph 仍依赖 client 特判，必须先回到 schema 和 event 层修正。
3. 如果长局时长失控，优先压缩分支和经济循环，不先继续加 zone。
4. 如果进阶职业内容不足，允许把后两职业的“可玩内容”后移，但不允许职业 schema 继续漂移。

## 9. 当前状态

1. Phase 3 总纲、系统设计与本文已对齐。
2. 本文可直接作为 `Phase 3` 的实现和 PR 切分底稿。
3. 当前尚未开始代码落地，实验室命令与 golden harness 仍需实现。

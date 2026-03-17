> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

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
2. `CombatResolutionTrace / TraceEnvelope / Golden Corpus` 的正式字段与分层金样本。
3. `StatusEffectDef` 与 `ActorEffect / AreaEffectEmitter / WorldEffect` 的完整生命周期、堆叠、驱散、免疫规则。
4. `TalentTree V2`、`targeting/telegraphRef/effect op`、`DescriptionModel`、`AllocationDraft`。
5. `AIProfile DSL`、`TelegraphSpec`、`BossEncounter`、`BossPhaseDef`。
6. 职业 roster、资源轴边界与开发态可用性合同。

说明：

1. `4 个进阶职业` 的 schema、资源、定位与 profile 必须在本阶段冻结。
2. `2 个进阶职业` 必须在本阶段进入可玩路径。
3. 另外 `2 个进阶职业` 若内容量不足，可在本阶段尾部或下阶段初期补完内容，但 contract 不允许再漂移。

## 3. 范围与非目标

### 3.1 范围

1. 战斗公式 V2 与 `Resolution Trace / Golden Corpus`。
2. 状态/持续/姿态/标记/护盾/zone effect 正式化。
3. `4 基础职业` 正式树与 `2 进阶职业` 可玩化。
4. `AIProfile DSL`、Boss telegraph、Boss phase。
5. 世界分支、zone 入口、任务主支线、affix v1、经济循环。

### 3.2 非目标

1. 不在本阶段进入深度 ProcGen。
2. 不在本阶段引入完整 content pack 平台。
3. 不在本阶段做最终商业级内容量。

### 3.3 并行开发线与工作包执行原则

Phase 3 虽仍以 `P3-W1 ~ P3-W6` 为统一验收编号，但实现不应按单链串行推进，而应明确挂到四条 lane：

1. `Rules Lane`
   - 战斗公式、状态生命周期、AI DSL、Boss phase、经济循环规则
2. `Client Lane`
   - 状态 UI 语义、telegraph renderer、断点成长 UX、长局白盒支撑
3. `Content Lane`
   - 职业正式树、种族、世界分支、铭文初始内容、Boss/profile 数据
4. `Tools/QA Lane`
   - `Resolution Trace` golden、`bossHarness`、`longRunLab`、phase/version/corpus 校验

执行约束：

1. `P3-W1` 结束后，`P3-W2` 与 `P3-W3` 允许并行推进。
2. `P3-W4` 必须拆成 rules 侧的 `AIProfile DSL + BossEncounter` 与 client 侧的 telegraph 呈现，两者并行但共享一份 DSL 规格。
3. `P3-W5` 与 `P3-W6` 不应等待整条前链全部结束；职业/种族内容和世界入口/zone 数据可在 schema 稳定后提前起稿。
4. 任何 lane 的临时特判都不得绕过 `core/game/client/tools` 的既有边界。

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
2. 首批元素交互只启用 `FIRE/COLD/HOLY/SHADOW` 的核心规则；`LIGHTNING` 在本阶段不参与跨元素交互，其关联效果 `OVERCHARGE` 作为状态规则在 `4.2` 处理。
3. 每类关键战斗都必须有 `FORMULA` corpus 金样本：
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
7. `ApplicationPolicy` 作为效果进入战斗系统的正式入口合同，必须在 `W1` 冻结。
8. Phase 3 切换到上述正式公式后，Phase 2 的 `FORMULA` corpus / Golden Seed 基线必须全量重录，这是预期内破坏性变更。

说明：

1. 上述命中公式与补充设计文档 `3.2.1` 的 `m = -10` 写法是**同一公式的等价展开**；实际实现与测试一律以补充设计文档中的标准形式为权威，本文只保留便于阅读的展开式。
2. `CombatPipeline` 的完整 `12` 步定义见补充设计文档 `3.2.3`；`Phase 3` 只冻结实现必须保持该顺序与挂点。
3. `TraceEnvelope` 从 Phase 3 起必须携带显式 `phaseId / rulesetVersion / traceSchemaVersion / corpusId`，防止沿用旧 Phase 2 golden 假阳性通过。

### 4.2 状态、持续与回调生命周期

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/status/*
core/src/test/kotlin/com/ktome/core/status/*
game/src/main/resources/data/statuses/*.yaml
```

冻结口径：

1. 下列状态从 `Phase 2` 骨架升级为 `Phase 3` 正式主线：`STUN / ROOT / SILENCE / BLEED / BURN / GUARD / MARKED / SHIELD / REGEN / HASTE / SLOW / FREEZE / POISON / ARMOR_BREAK`。
2. 下列状态在 `Phase 3` 新增并进入正式主线：`BANE / CURSE / WEAKEN / OVERCHARGE / INVULNERABLE / STEALTH / TAUNT`。
3. sustain、mark、ward、zone effect 共用统一生命周期引擎，但宿主必须区分为 `ActorEffect / AreaEffectEmitter / WorldEffect`，不允许再把 zone effect 伪装成 actor status。
4. 清理、驱散、免疫和持续 tick 时机必须能在 trace 里定位；`BLEED / BURN / POISON` 固定为在**受影响实体的回合开始、行动前**结算，即使目标处于 `STUN / FREEZE` 也照常 tick。
5. 同一 actor 调度点的跨 carrier 总顺序固定为 `ActorEffect -> AreaEffectEmitter -> WorldEffect`；每层内部都必须有稳定 tie-break，并在层结束后执行死亡检查。

状态矩阵还必须明确以下第一版规则：

1. 不叠加、只刷新持续时间：
   - `STUN`
   - `ROOT`
   - `HASTE`
   - `SLOW`
   - `FREEZE`
   - `SILENCE`
   - `MARKED`
   - `BANE`
   - `CURSE`
   - `WEAKEN`
   - `OVERCHARGE`
   - `STEALTH`
2. 独立叠层：
   - `BLEED`
   - `BURN`
   - `POISON`
3. 上限封顶：
   - `ARMOR_BREAK` 最多 `3` 层，且是**跨来源全局上限**
4. 取较强值：
   - `SHIELD`
   - `REGEN`
   - `GUARD`
   - `INVULNERABLE`
5. 后来者覆盖：
   - `TAUNT` 同时只保留一个嘲讽源，后来者覆盖前一个来源
6. 唯一效果：
   - 通过 `uniquenessKey / exclusiveGroup / sourceScopedUnique / replacePolicy` 等通用字段表达，不再保留名字级特判
7. 互斥/覆盖：
   - `FREEZE` 与 `BURN` 互斥，并触发元素交互
   - `HASTE` 与 `SLOW` 在本阶段都不叠加，只保留单个当前值；统一折算为 `speedModifier` 净值：`effectiveSpeed = baseSpeed + hasteModifier - slowModifier`
   - `STEALTH` 只在**实际受到伤害**时被 AoE 打破；仅进入 AoE 覆盖范围但最终未受伤，不解除隐匿
8. 净化优先级必须可配置，并明确不可被净化的效果集合。

第一版补充说明：

1. `CURSED` 在实现与文档中统一并名为 `CURSE`，不再保留双拼写。
2. `SHIELD / REGEN / GUARD / INVULNERABLE` 的“取较强值”按 `magnitude` 优先、`remainingTurns` 次级比较；同来源可刷新数值和持续时间，不同来源的较弱效果直接丢弃。
3. `CURSE` 与 `WEAKEN` 独立生效，最终以下游属性自然下限兜底；`OVERCHARGE` 作为 `debuff` 挂在受害者身上，不叠加，只刷新持续时间，使其下一次成功承受的 `LIGHTNING` 伤害 `+25%`，随后被消耗。
4. 默认净化策略为：
   - 若存在 `STUN / ROOT`，优先清除这两类硬控
   - 否则清除剩余持续时间最长的负面状态
5. 默认不可被净化集合为：
   - `INVULNERABLE`
   - `STEALTH`
   - Boss phase 锁定状态
6. `KNOCKBACK` 作为瞬时位移效果处理，不进入持久状态矩阵，也不进入净化集合。
7. `AreaEffectEmitter / WorldEffect` 默认不受 actor 级 `cleanse` 影响；若设计要求可被移除，必须单独声明 `remoteRemovalPolicy`，不复用 `dispel` 术语。

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
   - telegraphRef
   - typed effect op
2. 动态说明只从 schema 和实际数值推导，不允许手写第二套文本逻辑。
3. `core` 只输出 `DescriptionModel` 等语义结构，不直接输出最终本地化字符串。
4. `respec` 与 `rollback` 必须建立在 `AllocationDraft` 之上，并有自动化回归。
5. `DescriptionModel.placeholders` 必须保留数值 / 布尔 / 文本类型信息，不能过早降成纯字符串。

第一版 UX / 操作合同：

1. 断点成长必须同时展示：
   - 当前 rank 已激活效果
   - 下一断点会新增什么
2. 未达到的断点效果允许以灰态或次级文案预览，但不得伪装成已激活效果。
3. `respec` 第一版固定为：
   - 本条作为 `Phase 3` 执行权威口径，与补充设计文档保持一致
   - 仅允许在非战斗状态触发
   - 本阶段默认免费
   - 清空当前职业树已投点数并全额返还
4. `rollback` 第一版固定为：
   - 仅对最近一次未确认分配生效
   - 不支持跨多步历史撤销

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
4. 进阶职业的正式玩家解锁条件固定第一版：
   - `Berserker`：`Vanguard` 通关
   - `Spellblade`：`Arcanist` 通关
   - `Shadowblade`：`Rogue` 通关
   - `Warden`：`Templar` 通关
5. `P3-W5` 的可玩验证允许引入 `DEV_UNLOCKED` 状态，与正式 `RELEASE_UNLOCKED` 分离。
6. 种族天赋点独立于职业天赋点，每 `4` 级获得 `1` 点。
7. 铭文系统在 Phase 3 进入 build 轴，最小合同固定为：
   - 最大铭文数 `4`
   - 同类最多 `2`
   - 热键 `5~8`
   - 不消耗主资源，只受冷却控制

补充冻结：

1. 本阶段进入主线验证的 `3` 个种族固定为：
   - `human`
   - `elf`
   - `dwarf`
2. `orc / undead` 继续冻结 schema 和 profile，但正式可玩内容允许后补。
3. 通关的技术定义固定为：击败 `深渊之心` 并生成 run summary。`Phase 3` 只有 `Normal` 难度；若后续阶段引入更多难度选项，任意难度通关均满足解锁条件。
4. 进阶职业解锁是**局间持久化数据**，不得写在当前 run save 内；必须与 `profile` 或等价账号本地档分离。
5. 最小局间档至少包含 `profileVersion / releaseUnlockedClasses / runHistory` 三类数据；正式结构以补充设计文档 `9.2.2` 的 `ProfileData` skeleton 为权威。

`Shadowblade / Warden` 在 `P3-W5` 开始前至少冻结以下最小设计稿：

#### Shadowblade

1. 资源：`ENERGY`
2. 定位：单体爆发 + 暗影 DoT + 短时控制
3. 三棵树方向：
   - `assassination_plus`
   - `shadowstep_mastery`
   - `venom_night`
4. signature ability：暗影步后触发的高爆发处刑
5. panic answer：短时隐匿 + 位移脱离

#### Warden

1. 资源：`POSITIVE_ENERGY`
2. 定位：神圣防护 + 区域控制 + 回复强化
3. 三棵树方向：
   - `nature_guard`
   - `life_ward`
   - `earth_bastion`
4. signature ability：区域护盾 / 反击结界
5. panic answer：短回合不死或强护盾窗口

`Spellblade` 的 `EQUILIBRIUM` 作为 `stateAxis` 在本阶段正式进入可玩路径，边界必须先冻结：

1. 每回合只根据**上一回合最后一个成功施放的技能流派**偏移一次。
2. 动作归类固定为 `EquilibriumAffinity = PHYSICAL / ARCANE / NEUTRAL`。
3. 只有已确认成功且 `affinity != NEUTRAL` 的主动技能会改变平衡值；铭文、被动、free action、sustain toggle 默认 `NEUTRAL`。
4. 普攻与近战武技默认 `PHYSICAL`；法术主动技能默认 `ARCANE`；混合技能必须显式声明 affinity，缺失时按 `NEUTRAL`。
5. 同回合若同时触发多个动作，以结算顺序最后一个成功且 `affinity != NEUTRAL` 的动作作为判定依据。
6. `0 / 100` 两端必须有明确的 HUD 与音效反馈。
7. `30 ~ 70` 视为稳定区；超出稳定区后逐步强化一端并削弱另一端。

铭文系统的具体 `ID / tier / cooldown / effect` 以补充设计文档 `8.2.3` 为权威，不再在本页维护第二套独立清单。`Phase 3` 主线验证至少覆盖：

1. `HEALING`
2. `MOVEMENT`
3. `PROTECTION`
4. `CLEANSING`

若要在 `Phase 3` 主线额外加入 `OFFENSE` 类铭文，必须先把正式条目补进补充设计文档，再进入执行文档。

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
2. Boss encounter 固定为两层结构：
   - `BossEncounter` 层负责遭遇元信息、phase 列表、切换阈值和 `onEnter` 事件
   - `AIProfile` 层负责当前 phase 内的候选动作与选择策略
3. 普通怪与精英怪继续沿用补充设计文档 `7.3.2` 的 `behaviors[].priority + condition -> action` 确定性模型，保证可预测与可回放。
4. Boss `phase` 切换条件固定使用结构化字段（如 `hpThreshold / hpEnd / requiredStatus / turnCount`），不在 `Phase 3` 引入额外的字符串表达式解析器。
5. Boss 高伤技能必须有 telegraph；统一权威结构为 `TelegraphSpec`，能力与 `BossEncounter.onEnter` 都只做引用。
6. telegraph 伤害阈值固定遵循补充设计文档合同：单次技能预期伤害 `>= 30%` 标准 defender 最大 HP 时至少 `1` 回合预览，`>= 50%` 时至少 `2` 回合预览。
7. `AIProfile` 必须显式声明 `selectionPolicy`；`weight` 仅用于 `WEIGHTED_RANDOM`，不替代普通怪脚本的 `priority` 语义。
8. 候选动作在进入 policy 前统一按 `orderKey asc(null=Int.MAX_VALUE) -> actionId asc` 排序；未声明 `weight` 时默认 `1.0`，全 0 权重回退到排序后的首个候选。
9. `TelegraphSpec.threatProfileId` 只能引用注册表中的 `ThreatProfileDef`，不得在 Boss / talent YAML 内联新的 defender baseline。
10. `AIDecisionTrace` 与 `BossTrace` 必须可导出。
11. AI 不允许依赖作弊式全图透视，只能基于可感知状态和最后已知信息。

`P3-W4` 开始实现前，至少冻结以下最小 DSL / Boss 规格；下面示例同时展示 `BossEncounter` 层如何引用 `AIProfile` 层。示例使用最小名册内的 `molten_giant`，避免再引入额外示例 Boss ID：

```yaml
boss_encounter:
  id: "molten_giant"
  templateId: "molten_giant_template"
  phases:
    - id: "phase_full"
      hpThreshold: 1.0
      hpEnd: 0.50
      aiProfileId: "molten_giant_phase_full"
    - id: "phase_enraged"
      hpThreshold: 0.50
      hpEnd: 0.0
      aiProfileId: "molten_giant_phase_enraged"
      onEnter:
        - type: "TELEGRAPH"
          telegraphSpecId: "ground_slam_phase_warning"

ai_profiles:
  - id: "molten_giant_phase_full"
    perceptionRange: 8
    useLastKnownPosition: true
    defaultBehavior: "CHASE"
    actions:
      - id: "advance"
        type: "MOVE_TOWARD_PLAYER"
        weight: 30
      - id: "stomp"
        type: "USE_ABILITY"
        abilityId: "stomp"
        condition:
          type: "TARGET_DISTANCE_LESS_THAN"
          distance: 2
        weight: 70

  - id: "molten_giant_phase_enraged"
    perceptionRange: 8
    useLastKnownPosition: true
    defaultBehavior: "CHASE"
    actions:
      - id: "ground_slam"
        type: "USE_ABILITY"
        abilityId: "ground_slam"
        weight: 80
```

`BossPhaseDef` 第一版至少必须覆盖：

1. 切换条件：
   - `hpThreshold / hpEnd`
   - `turnCount`
   - `requiredStatus`
2. 切换时机：
   - 默认在回合开始时判定
   - 若遭遇“致死转阶段”例外，必须显式声明
3. 切换副作用：
   - `invulnerableTurns`
   - `clearStatuses`
   - `resetAiPhaseState`
   - `emitTelegraph`
4. phase 切换必须写入 `BossTrace`，并能和 telegraph/视觉提示对齐。

`STEALTH / TAUNT` 与 AI 的交互合同也必须冻结：

1. 目标进入 `STEALTH` 后，AI 当前目标引用立即失效。
2. 若 `useLastKnownPosition = true`，AI 必须移动到目标最后已知位置；到达后仍未重新发现目标，则回退到 `defaultBehavior`。
3. Boss 遇到 `STEALTH` 时不切换 phase，只能在当前 phase 内执行无目标 fallback 或范围扫描行为，不得作弊式锁定隐身目标。
4. AI 被 `TAUNT` 命中后，在持续期间必须强制把攻击目标设为嘲讽源；若目标不在攻击范围内，优先移动靠近，不得攻击其他目标。
5. `TAUNT` 结束后，AI 才恢复原有目标选择逻辑。

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

最小局间合同：

1. Phase 3 仍是纯 run-based Roguelike：角色死亡后 run 重置，不做数值型 meta progression。
2. 持久化到局间的只有：
   - 进阶职业解锁
   - 已发现 profile / 历史 run summary
3. “通关”固定定义为击败 `深渊之心` 并完成结算页面。
4. run 内获得的装备、affix、铭文和货币不跨 run 继承。
5. run 内推进状态必须通过 `WorldProgressSnapshot` 进入 `SaveDataV2`，无损保留 `questStates / worldFlags / unlockedRoutes / defeatedBossIds / claimedRouteRewards`。

最小经济模型：

1. Phase 3 只做一种局内货币：`shard`
2. `shard` 来源：
   - 怪物掉落
   - 支线奖励
   - Boss 奖励
3. Phase 3 只做固定商店节点，不做打造/附魔/合成：
   - `greenwood_fringe` 入口后 `1` 个补给商人
   - `deep_iron_pit` 路线上 `1` 个中段商店
4. 商店行为只包括：
   - 购买装备
   - 购买消耗品 / 铭文
   - 出售多余掉落
5. Phase 4 才扩展打造、附魔与更复杂的经济回路。
6. checkpoint 级 `AffordableRescueSlotPolicy` 也必须冻结：
   - `expectedShardBudgetByCheckpoint`
   - `mandatoryAffordableItemCount`
   - `requiredAffordableTags`
7. 第二商店除了存在位移/净化/护盾类工具外，还必须保证 checkpoint 期望 shard 预算下买得起至少一组救火组合。

长局主分支的第一版示意必须固定，至少覆盖：

1. `shattered_outpost -> greenwood_fringe -> deep_iron_pit -> grey_gate_depths`
2. `elven_ruins`（精灵遗迹）/ `molten_core`（熔岩核心）/ `bandit_camp`（盗贼营地）等可选支线
3. `underground_river`（地下河）-> `crystal_cavern`（水晶洞穴）-> `abyssal_temple`（深渊神殿）-> `abyssal_heart`（深渊之心）的 `Phase 3` 扩展终线

第一版连接关系固定为：

```text
shattered_outpost
  -> greenwood_fringe
    -> elven_ruins (optional)
    -> bandit_camp (optional)
    -> deep_iron_pit
      -> molten_core (optional)
      -> grey_gate_depths (contains inherited grey_gate_throne boss room)
        -> underground_river
          -> crystal_cavern (optional)
          -> abyssal_temple
            -> abyssal_heart
```

第一版 zone 等级范围固定为：

| Zone ID | 中文名 | 等级范围 |
| --- | --- | --- |
| `shattered_outpost` | 破碎前哨 | `Lv1-4` |
| `greenwood_fringe` | 绿林边缘 | `Lv3-6` |
| `bandit_camp` | 盗贼营地（可选） | `Lv3-5` |
| `elven_ruins` | 精灵遗迹（可选） | `Lv5-7` |
| `deep_iron_pit` | 深铁矿坑 | `Lv5-8` |
| `molten_core` | 熔岩核心（可选） | `Lv7-9` |
| `grey_gate_depths` | 灰门深窟 | `Lv7-10` |
| `underground_river` | 地下河 | `Lv10-12` |
| `crystal_cavern` | 水晶洞穴 | `Lv11-13` |
| `abyssal_temple` | 深渊神殿 | `Lv12-15` |
| `abyssal_heart` | 深渊之心 | `Lv15` |

`Phase 3` 的最小 Boss 名册固定为：

| Boss ID | Zone | 类型 | 来源 |
| --- | --- | --- | --- |
| `molten_giant` | `deep_iron_pit` | 中间 Boss | 继承 `Phase 2` |
| `dungeon_lord` | `grey_gate_depths` | 区域 Boss | 继承 `Phase 2` |
| `abyssal_guardian` | `abyssal_heart` | 最终 Boss | `Phase 3` 新增 |

`bossHarness` 在 `W4` 建立 `2` Boss 工具基线，在 Phase 3 总出口升级为覆盖上述 `3` 个 Boss。

内容预算基线参照补充设计文档 `7.6`：

1. 普通怪模板约 `40`
2. 精英怪模板约 `12`
3. Boss 预算按 `3~4` 个控制；当前最小名册固定交付 `3` 个，若后续为 `underground_river` 或 `abyssal_temple` 增补区域 Boss，不得突破该预算区间

可选 zone 的最小内容约束固定为：

1. 每个可选 zone 至少包含 `1` 个独有精英怪、独有奖励节点或独有事件，不能只是主线路径换皮。
2. 可选 zone 不得承载主线 Boss 或主线解锁道具。
3. 可选 zone 的怪物池、掉落预算和固定奖励必须与其等级范围对齐。
4. `Phase 3` 不要求可选 zone 有独立叙事任务，但必须提供至少 `1` 个明确的探索收益点。

`Spellblade` 的 `EQUILIBRIUM` 边界定义见 `4.4`，本节不重复维护第二份职业资源规格。

`affix v1` 的最低预算也必须冻结：

1. 武器前缀 `12`
2. 武器后缀 `10`
3. 防具前缀 `10`
4. 防具后缀 `8`

说明：

1. `affix v1` 在 Phase 3 的目的仅是验证“掉落驱动构筑差异”。
2. 完整的词条多样性和生态深度留给 Phase 4，不把 Phase 3 当成最终 affix 终局。

## 5. 推荐 PR / 工作包拆分

`P3-W1 ~ P3-W6` 仍是统一验收编号，但默认按 lane 并行执行，不按单链串行等待：

1. `P3-W2` 允许拆成：
   - `W2a`：状态生命周期骨架（Rules）
   - `W2b`：状态 UI 语义（Client）
2. `P3-W4` 允许拆成：
   - `W4a`：AIProfile DSL + BossEncounter（Rules）
   - `W4b`：telegraph renderer（Client）
3. `P3-W5` 允许拆成：
   - `W5a`：职业/Profile/资源合同（Rules）
   - `W5b`：基础职业树 + 种族 + 铭文内容（Content）
   - `W5c`：进阶职业可玩化 + UI + SoloClearLab（Client+QA）
4. `P3-W6` 允许拆成：
   - `W6a`：世界分支与 zone 入口（Content）
   - `W6b`：`WorldProgress / Quest / Gate` + affix v1 + 经济循环规则（Rules）
   - `W6c`：`RunSummary`、长局实验室与回归（Tools/QA）

### P3-W1 Combat Formula & Trace Goldens

1. 战斗公式 V2
2. `CombatResolutionTrace / TraceEnvelope`
3. 分层 golden harness

### P3-W2 Status Lifecycle

1. `ActorEffect / AreaEffectEmitter / WorldEffect`
2. stack/cleanse/immunity
3. 状态 UI 语义同步

### P3-W3 Talent Tree V2

1. tree schema
2. `DescriptionModel`
3. `AllocationDraft` / respec / rollback

### P3-W4 AIProfile & Boss Telegraph

1. AI DSL
2. `TelegraphSpec`
3. `BossEncounter`
4. telegraph renderer
5. boss trace

### P3-W5 Class Formalization

1. 4 基础职业正式树
2. 2 个进阶职业可玩化
3. 3 个种族与 Profile

### P3-W6 Long-Run World Structure

1. 世界分支
2. `WorldProgress / Quest / Gate`
3. affix v1
4. 固定经济循环与 rescue policy
5. `RunSummary`
6. 长局回归实验室

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

1. `CombatResolutionTrace` 在固定输入下稳定。
2. 状态堆叠、驱散、免疫和 tick 顺序稳定。
   - 跨 carrier 总顺序固定为 `ActorEffect -> AreaEffectEmitter -> WorldEffect`
3. talent 动态说明与实际数值一致。
4. respec/rollback 不破坏 build。
5. Boss telegraph 和 phase 切换可稳定复现。
6. 4~6 小时 run 可稳定收敛到死亡或通关。
7. 公式切换后，标准场景下 P3 与 P2 的数值差异不出现数量级崩塌；`P2toP3FormulaComparisonTest` 至少拆成：
   - 伤害类：`±30%`
   - 命中率类：`±10` percentage points
   - 状态施加率类：`±10` percentage points
8. `longRunLab` 两种模式共同检查：
   - 无卡死、无不可达主线
   - 长局主支线和可选支线都可达
   - `headlessTurnEquivalent` 的上限默认 `3000`
9. `longRunLab full` 第一版量化参考固定为：
   - `4` 基础职业 × `3` 种族的 `12` 个组合中，至少 `8` 个能到达 `abyssal_temple`
   - `50%` 以上失败 run 应发生在 `deep_iron_pit` 之后，避免前期难度曲线过陡

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

补充要求：

1. `combatTraceGolden` 至少支持阶段/version 校验。
2. `longRunLab` 至少支持 `smoke` 与 `full` 两种模式。

### 6.4 白盒验证

1. 进入任一长局默认 seed。
2. 观察至少一次 Boss telegraph，并确认：
   - 提示出现
   - 玩家可规避
   - 伤害与 trace 对齐
3. 用 `Vanguard`、`Arcanist`、`Rogue`、`Templar` 各完成一次长局。
4. 用至少 `2` 个进阶职业完成一局关键 Boss 路径。
5. 做一次洗点/回滚，确认 talent、资源和描述同步更新。

说明：完整白盒验证步骤与扩展场景以 [phase3-verification-checklist.md](2026-03-13-phase3-verification-checklist.md) `§3` 为权威，本节只列最低验证意图。

## 7. 出口门禁

1. 分层 golden / harness 全绿。
2. 4 基础职业和 2 进阶职业通过 `SoloClearLab` 与关键 Boss 回归。
3. golden / harness 产物带有 `phaseId / rulesetVersion / traceSchemaVersion / corpusId`，CI 会拒绝混用旧阶段产物。
4. `longRunLab` 的 `smoke` 模式可到达终局状态；`full` 模式必须满足以下第一版量化门槛：
   - `headlessTurnEquivalent` 的上限默认 `3000`
   - `4` 基础职业 × `3` 种族的 `12` 个组合中，至少 `8` 个能到达 `abyssal_temple`
   - `50%` 以上失败 run 发生在 `deep_iron_pit` 之后
5. `3` 个 Boss 全部通过 `bossHarness`；`W4` 的 `2` Boss 仅是工具基线，不是 Phase 3 最终出口。
6. telegraph、AI、Boss phase、状态生命周期都可白盒验证。
7. 关键包 coverage gate 达标，且没有以跳过 trace/golden 方式规避门禁；推荐门槛：
   - `core.combat >= 85%`
   - `core.status >= 80%`
   - `core.ai >= 75%`
   - `core.talent >= 80%`
   - `core.world >= 70%`

## 8. 风险与止损

1. 如果公式和动态说明不同步，优先修正 schema/description pipeline，暂停补内容量。
2. 如果 Boss telegraph 仍依赖 client 特判，必须先回到 schema 和 event 层修正。
3. 如果长局时长失控，优先压缩分支和经济循环，不先继续加 zone。
4. 如果进阶职业内容不足，允许把后两职业的“可玩内容”后移，但不允许职业 schema 继续漂移。
5. 如果 `AIProfile DSL` 在 `W4a` 实现中发生超出最小冻结的扩展，必须先冻结新规格并通知 Content Lane 暂停 Boss/精英数据起稿，直到新规格经过至少 `2` 个精英怪的端到端验证。

## 9. 当前状态

1. Phase 3 总纲、系统设计与本文已对齐。
2. 本文可直接作为 `Phase 3` 的实现和 PR 切分底稿。
3. 当前尚未开始代码落地，实验室命令与 golden harness 仍需实现。

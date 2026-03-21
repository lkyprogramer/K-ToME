> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-pr-07-short-run-expansion.md`
> `docs/phase2/2026-03-13-phase2-verification-checklist.md`
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
> `docs/2026-03-13-art-style-bible.md`

# Phase 2 - PR-07 Post-Review Execution Plan

**阶段**: `Phase 2 / P2-W7 / P2-C`  
**版本目标**: `v0.2.x`  
**优先级**: `P0`  
**前置条件**: `P2-W6` 已完成并稳定回归  
**文档定位**: `PR-07` 的审查后执行版，不替代原始设计基线，只负责把 `P2-C` 补齐到可交付状态

---

## 1. 文档定位与权威关系

### 1.1 本文解决什么问题

原始 [2026-03-13-phase2-pr-07-short-run-expansion.md](./2026-03-13-phase2-pr-07-short-run-expansion.md) 已经定义了 `4 职业 + 4 zone + 24 怪 + 24 物品 + SoloClearLab v1` 的目标，但没有回答当前仓库在完成 `PR-06` 之后，应该按什么顺序、以什么 PR 粒度、如何把代码线、内容线、客户端线、资源线一起收口。

本文吸收 2026-03-20 的 Phase 2 深度审查结论，把原始设计目标改写为：

1. 可以直接排期的 sprint / PR 列表
2. 与现有仓库状态相匹配的依赖顺序
3. 明确的自动化 gate 与白盒验证
4. 明确纳入 Phase 2 的美术 / 音频正式路径要求
5. 明确延后到 Phase 3+ 的内容边界

### 1.2 与现有文档的关系

| 文档 | 角色 | 本文与其关系 |
| --- | --- | --- |
| `2026-03-13-phase2-semantic-contracts-tile-and-i18n.md` | Phase 2 总纲 | 不改阶段边界，只继承 |
| `2026-03-13-phase2-pr-06-minimal-official-slice.md` | `P2-B` 官方切片设计 | 视为已完成基线，只允许 bugfix |
| `2026-03-13-phase2-pr-07-short-run-expansion.md` | `P2-W7` 原始设计版 | 保留为目标定义，本文提供执行顺序 |
| `2026-03-13-phase2-verification-checklist.md` | Phase 2 门禁清单 | 本文补充“完成态应如何解释这些 gate” |
| `2026-03-13-art-style-bible.md` | Phase 2+ 资源风格圣经 | 资源线强制遵守 |

### 1.3 当前阶段判定

当前仓库不应被描述为“Phase 2 已完成”，而应固定为：

1. `P2-B` 已成立
2. `P2-C` 未完成
3. `PR-06` 官方切片稳定性问题已关单
4. 真正阻塞 Phase 2 结束的是 `职业正式化 + route 真相 + 内容量下限 + gate 重对齐 + formal path 资源收口`

---

## 2. 当前真相与审查吸收结论

### 2.1 已成立的 P2-B 真相

以下结论已被视为当前主线基线：

1. `Save V2 / Schema V2 / i18n / RenderSnapshot / VisualManifest / AudioManifest / Tile 正式路径` 已进入正式主链
2. `Vanguard + Arcanist + shattered_outpost + bandit captain` 官方切片已能稳定闭环
3. `OfficialSliceStability`、`longRunLab`、`clientSmoke`、`goldenScreenshot` 等基础 gate 已证明 `P2-B` 切片稳定
4. `PR-06` 不再是当前修复目标的中心；后续只允许修 bug，不允许为了 `P2-C` 需求重写其真相

### 2.2 当前 P2-C 阻塞项

审查后需要被吸收为 `P2-C` 阻塞的事项如下：

1. `Rogue / Templar` 仍是 schema 壳，不是正式职业包
2. `4 zone` 仍未组成一条真实短局 route
3. `24 怪 + 24 物品` 下限未满足
4. gate 仍偏向证明 `PR-06` slice 绿，而非 `P2-C` 完成态绿
5. `greenwood_fringe / deep_iron_pit / grey_gate_depths` 的 identity 仍未在 runtime 成立
6. `Visual/Audio formal path` 仍存在大量 placeholder / silence fallback
7. 关键技术债仍影响体验闭环：
   - 资源消耗与 `ResourcePools` 双轨并存
   - 怪物掉落未接入战斗循环
   - damage type 语义未形成完整战斗主链
   - `statGrowth`、`CombatProfile`、解锁节奏未真正进入角色成长路径

### 2.3 本轮明确纳入 Phase 2 的修复范围

以下审查结论不再作为“建议”，而是纳入执行版范围：

1. `P0`
   - 统一资源消耗到 `ResourcePools`
   - 接入怪物死亡掉落
   - 重写 loot profile，加入武器和分级 Boss 掉落
2. `P1`
   - 接入基础 `DamageType` 与简化抗性
   - `CombatProfile` 数据驱动
   - `statGrowth` 自动成长生效
   - 减少初始天赋数量，建立解锁节奏
   - 修复 `blink` 的空 levelEffects
3. `P2`
   - 只吸收与 `P2-C` gate 直接相关的部分：怪物 acc/eva 数据、`charge` 归位、中间区域目标/Boss 收口
   - 其他优化项不阻塞 Phase 2 结束

### 2.4 明确延后到 Phase 3+

以下问题本轮不进入范围：

1. 多阶段 Boss 战与复杂相位系统
2. 完整的抗性递减、穿透收益递减公式
3. 高级难度模式
4. Meta progression
5. 随机事件大系统
6. 套装 / 深词缀 / 复杂装备联动
7. 完整 AI Profile DSL
8. 动态战斗音乐

这些延后项不应再被混入 `P2-C` 实施中。

### 2.5 Visual/Audio 现状与缺口

当前资源状态必须按正式路径理解，而不是按“将来再补”理解：

1. `P2-B` 核心资源已存在并可视为冻结基线
2. `P2-C` 已有部分资源条目，但 formal path 未收口
3. 截至当前 Sprint 7 开始时，formal-path required key 已经与“剩余 debug 预算”分开统计：
   - required visual key 当前为 `0 missing_visual`
   - required audio key 当前为 `0 silence.ogg`
   - 剩余 `phase2` 预算仍有 `25` 条 visual key 指向 `debug/missing_visual.png`
   - 剩余 `phase2` 预算仍有 `13` 条 audio key 指向 `audio/fallback/silence.ogg`

当前高风险缺口必须明确点名：

1. 非 formal 路径的 `affix.*`、`material.*`、`difficulty.normal.*` 仍保留 visual/audio budget
2. `missing_visual` / `audio.fallback.silence` 仍作为 debug entry 保留，必须与 formal-path blocker 分开解释
3. 后续若要继续压缩 budget，应优先决定 affix / material / difficulty 是否在 Phase 2 就升级为正式资产

为避免团队继续误判，本执行版使用以下三态表来定义资源状态：

| 资源域 | 已有正式资产 | 已有 spec / manifest 但未收口 | 当前仍是 placeholder / silence |
| --- | --- | --- | --- |
| Profession | `actor.*`、四职业 portrait、四职业 `icon.profession.*` 已正式化 | 无职业主入口 formal blocker | 无职业主入口 cue 留在 silence；剩余 budget 不在 profession 主路径 |
| Zone | `4 zone` 的 `visual / icon`、stairs、route objective prop 已正式化 | 无 route 主入口 placeholder blocker | 无 route 主入口 silence blocker；剩余预算不在 zone 主路径 |
| Boss | `bandit_captain`、`dungeon_lord` 的 actor / encounter visual / icon / cue 已正式化 | 无新的 boss formal blocker | boss 主路径当前无 placeholder / silence blocker |
| Objective / Interactable | route objective / interactable 已全部进入 runtime 主链 | 非主路径 objective 可继续走预算统计 | 当前主路径 objective/interactable 无 silence blocker |
| Reward / Item | `24` 物品矩阵、signature reward icon/cue 已进入主链 | 非主路径 affix / material 仍需在 Phase 3+ 决定是否正式化 | affix / material 的 visual/audio 仍属于非 formal budget |
| Talent / Tree | `33` 个 talent 的 skill icon/cue 与 `12` 棵 tree 的 `tree.* / icon.tree.*` 已有 runtime entry | 无新的职业树 formal blocker | 当前 visual budget 不再集中在 tree 主路径 |
| Core UI Cue | `confirm / cancel / hover / footstep / melee / spell / boss.warning` 与 route cue 已成型 | 报告字段仍需显式区分 required key 与 debug budget | 非 formal、未纳入 required set 的条目可以保留预算，但必须单独统计 |

---

## 3. 收口原则与实施顺序

### 3.1 总体原则

1. 先修地基，再扩内容
2. 先闭合核心循环，再补量
3. 先让 gate 验证正确目标，再宣称 Phase 2 完成
4. `P2-B` 已完成部分只做 bugfix，不再返工设计
5. 单个 PR 尽量只引入一个新抽象族，同时触碰的生产模块不超过两个

### 3.2 四条并行开发线

1. `Rules Lane`
   - `core`
   - 资源、伤害、成长、状态、战斗
2. `Content Lane`
   - `game`
   - profession / monster / item / zone / objective / reward
3. `Client Lane`
   - `client`
   - route 表现、HUD、golden、client smoke
4. `Visual/Audio Lane`
   - `assets-src`、runtime manifest、scripts
   - image spec、audio plan、manifest、formal path 收口

### 3.3 固定实施顺序

Phase 2 收口顺序固定为：

1. `Freeze The Truth`
2. `Repair Core Loop Foundations`
3. `Repair Combat And Growth Contracts`
4. `Formalize Rogue And Templar`
5. `Build The Real Zone Route`
6. `Reach The P2-C Content Floor`
7. `Realign The Gates`
8. `Formal Path Closure`

任何试图跳过前四步直接补内容量或补美术量的做法，都视为 scope 漂移。

### 3.4 Visual/Audio Lane 原则

1. `P2-B` 资产视为冻结基线，只允许 bugfix，不重生、不重排风格。
2. `P2-C` 新内容必须按以下顺序落地：
   - 先定义 `visualKey / iconKey / audioProfile`
   - 再进入 `phase2-asset-plan.yaml` 或 `phase2-audio-plan.yaml`
   - 再进入 canonical manifest
   - 再同步到 runtime manifest
3. 开发中允许短期 placeholder，但命中 formal path 的 key 在最终 gate 中必须 `0 fallback`。
4. `affix / material / difficulty` 等非正式主路径条目可以保留预算，但必须与 `profession / zone / boss / route objective / signature reward / core talent` 分开统计。
5. 美术必须服从 [2026-03-13-art-style-bible.md](../2026-03-13-art-style-bible.md)，不允许为了赶进度绕过 style discipline。

---

## 4. Sprint / PR 执行计划

## Sprint 0: Freeze The Truth

**目标**: 固定 `P2-B 已完成 / P2-C 未完成` 的事实，并把资产缺口正式纳入阻塞项。  
**依赖**: 无  
**Sprint Exit**:

1. `PR-06` 稳定性问题被正式关单
2. `formal-path placeholder` 被明确写成 `P2-C` 阻塞
3. 开发入口从“继续补 PR-06”切到“完成 P2-C”

### PR-0A：review 与 checklist 口径回写

- **范围**:
  - `docs/phase2/2026-03-13-phase2-verification-checklist.md`
  - 本执行版文档
- **目标**:
  - 明确 `OfficialSliceStability` 属于 `P2-B` 证据
  - 明确 `P2-C` 完成态还需要 route / content / formal path 证据
- **验收**:
  - 文档不再把 `PR-06` 稳定性缺失列为当前 blocker
  - 文档显式写出 `formal-path placeholder` 属于 `P2-C` 阻塞
- **验证**:
  - 人工检查 `docs/phase2` 入口与 checklist 口径一致

### PR-0B：资源真相基线清单

- **范围**:
  - 本执行版文档
  - `docs/phase2/roadmap.md`
- **目标**:
  - 给出 `已有正式资产 / 已有 spec 未收口 / 仍是 placeholder` 三态表
  - 把美术 / 音频从“最后统一收尾”提升为独立 Lane
- **验收**:
  - 文档能明确覆盖 profession、zone、boss、objective、reward、talent、UI cue
- **验证**:
  - 人工检查三态表是否覆盖 Phase 2 完成态所有关键资源域

## Sprint 1: Repair Core Loop Foundations

**目标**: 先闭合资源与奖励循环，同时准备资源 key 的正式接入点。  
**依赖**: Sprint 0  
**Sprint Exit**:

1. 资源消耗不再走错误路径
2. 怪物掉落进入战斗循环
3. 新增 reward / loot key 已进入 canonical 资源入口

### PR-1A：统一资源消耗到 `ResourcePools`

- **范围**:
  - `core/.../talent/*`
  - `game/.../PlayerResourcePools.kt`
  - `game/.../FoundationGameSession.kt`
  - `game/.../SessionSnapshotMapper.kt`
- **关键改动**:
  - `TalentDef` 从单一 `staminaCost` 转为通用 resource cost
  - `TalentResolver` 统一从 `ResourcePools` 扣费
  - save/load 与 HUD 同步真实资源状态
- **验收**:
  - Arcanist 技能真实消耗 `MANA`
  - Rogue / Templar 后续可无缝接入 `ENERGY / POSITIVE_ENERGY`
- **验证**:
  - `./gradlew :core:test`
  - `./gradlew :game:test`
  - `./gradlew soloClearLab`

### PR-1B：怪物掉落与 loot profile 修复

- **范围**:
  - `game/.../FoundationGameSession.kt`
  - `game/src/main/resources/data/loot/index.yaml`
  - 相关 item / i18n / log token
- **关键改动**:
  - 非 Boss 怪物死亡按 loot profile 产生掉落
  - common / elite / boss 三档掉落层次成立
  - 武器进入掉落表
- **验收**:
  - 战斗后可获得物品，不再只给 XP
  - Boss 掉落明显优于普通掉落
- **验证**:
  - `./gradlew :game:test`
  - `./gradlew headlessSmoke`

### Visual PR-1C：reward / loot 资源 key 接入

- **范围**:
  - `assets-src/image/specs/phase2-asset-plan.yaml`
  - `assets-src/image/manifests/phase2-visual-manifest.json`
  - `client/src/main/resources/manifests/visual-manifest.json`
- **关键改动**:
  - 为本 sprint 新引入的 reward / loot / inspect key 建立 canonical entry
  - 不要求量产新图，但不允许新增 key 直接命中 `missing_visual`
- **验收**:
  - 本 sprint 新增正式 item/reward key 均有 canonical entry
- **验证**:
  - `./gradlew assetLint`
  - `./gradlew manifestLint`

### Audio PR-1D：掉落与奖励 cue 接入

- **范围**:
  - `assets-src/audio/specs/phase2-audio-plan.yaml`
  - canonical/runtime audio manifest
- **关键改动**:
  - 增加掉落、拾取、奖励确认、monster death 所需 cue key
  - 不允许新奖励路径直接吃 silence fallback
- **验收**:
  - 本 sprint 新增 reward / loot cue 已进入 plan 与 runtime manifest
- **验证**:
  - `./gradlew audioLint`

## Sprint 2: Repair Combat And Growth Contracts

**目标**: 补齐关键 `P1` 技术债，并同步收口相关 UI/icon/cue。  
**依赖**: Sprint 1  
**Sprint Exit**:

1. damage type 已进入战斗主链
2. 职业成长开始真实分化
3. 天赋解锁不再是全开工具箱

### PR-2A：`DamageType` + 简化抗性接入

- **范围**:
  - `core/.../combat/*`
  - `core/.../stats/*`
  - `core/.../talent/*`
  - `game` 中 talent / monster schema 映射
- **关键改动**:
  - `DamageType` 枚举
  - `DamageResult` 携带 type
  - 简化抗性公式生效
- **验收**:
  - `FIRE / COLD / HOLY / SHADOW / PHYSICAL` 在战斗中产生真实差异
- **验证**:
  - `./gradlew :core:test`
  - `./gradlew :game:test`

### PR-2B：`CombatProfile` 数据驱动 + `statGrowth` 生效

- **范围**:
  - profession schema
  - `EntityFactory`
  - `ExperienceSystem` / 升级流程
- **关键改动**:
  - 四职业基础战斗轮廓从 schema 读取
  - 升级时叠加 `statGrowth`
- **验收**:
  - Vanguard / Arcanist / Rogue / Templar 的基础血量、护甲、速度不再同值
- **验证**:
  - `./gradlew :game:test --tests com.ktome.game.GameModuleTest`

### PR-2C：成长解锁节奏 + `blink` 修复 + `charge` 归位

- **范围**:
  - `professions/index.yaml`
  - `talents/index.yaml`
  - 相关 talent resolution / UI
- **关键改动**:
  - 减少 `startingTalents`
  - 建立最小解锁节奏
  - 修复 `blink` 空 levelEffects
  - 明确 `charge` 是玩家天赋还是 AI-only
- **验收**:
  - 玩家不再 1 级拿满全部技能
  - `blink` 升级有收益
- **验证**:
  - `./gradlew :game:test`
  - `./gradlew soloClearLab`

### Visual PR-2D：damage / growth / status 视觉收口

- **范围**:
  - `phase2-asset-plan.yaml`
  - canonical/runtime visual manifest
- **关键改动**:
  - 确认并补齐 `6` 个 damage type icon
  - 为成长、资源、状态、`blink`、新解锁节点建立正式 icon 规范
- **验收**:
  - 相关 key 在客户端可解析且不走 prefix fallback
- **验证**:
  - `./gradlew assetLint`
  - `./gradlew manifestLint`
  - `./gradlew goldenScreenshot`

### Audio PR-2E：damage / resource / blink cue 收口

- **范围**:
  - `phase2-audio-plan.yaml`
  - canonical/runtime audio manifest
- **关键改动**:
  - 增加 `HOLY / SHADOW / FIRE / COLD` 命中提示语义 cue
  - 增加 `blink`、资源消耗、解锁反馈相关 cue
- **验收**:
  - 新增 damage/resource/talent cue 不再落到 silence
- **验证**:
  - `./gradlew audioLint`

## Sprint 3: Formalize Rogue And Templar

**目标**: 把 Rogue / Templar 从 schema 壳变成正式职业包，并同步完成最小正式表现层。  
**依赖**: Sprint 2  
**Sprint Exit**:

1. 两职业都能真实使用天赋
2. 两职业的资源身份、构筑 answer、最小表现层成立
3. 客户端进入两职业时不出现明显 fallback

### PR-3A：Rogue 规则与数据正式化

- **范围**:
  - profession / talent / item / i18n / AI / session
- **关键改动**:
  - `backstab / poison_blade / stealth / smoke_bomb / roll / blade_flurry / shadowstep / deathblow`
  - `ENERGY` 消耗与回复闭环
- **验收**:
  - Rogue 在 `SoloClearLab` 中真实使用 talent，并出现 spend / restore
- **验证**:
  - `./gradlew :game:test`
  - `./gradlew soloClearLab`

### PR-3B：Templar 规则与数据正式化

- **范围**:
  - profession / talent / item / i18n / session
- **关键改动**:
  - `holy_strike / judgment_hammer / holy_light / holy_shield / devotion / holy_aura / purify / divine_intervention`
  - `POSITIVE_ENERGY` 积攒 / 衰减闭环
- **验收**:
  - Templar 在 `SoloClearLab` 中真实使用 talent，并出现 gain / decay
- **验证**:
  - `./gradlew :game:test`
  - `./gradlew soloClearLab`

### PR-3C：`SoloClearLab v1` 升级为职业成立门禁

- **范围**:
  - `SoloClearLabSupport.kt`
  - `SoloClearLabTest.kt`
- **关键改动**:
  - 断言不再只是“存活”
  - 强制检查 `UseTalent(...)`、资源轨迹、warning/telegraph
- **验收**:
  - 绿灯不再等于“普攻混过去”
- **验证**:
  - `./gradlew soloClearLab`

### Visual PR-3D：职业资产包

- **必交**:
  - `actor.rogue`
  - `portrait.rogue`
  - `actor.templar`
  - `portrait.templar`
  - `tree.rogue_*`
  - `tree.templar_*`
  - `16` 个冻结 talent 的 `icon.skill.*` 与必要的 `talent.*.visual`
- **验收**:
  - Rogue / Templar 进入正式路径时不再出现 `missing_visual`
- **验证**:
  - `./gradlew assetLint`
  - `./gradlew manifestLint`
  - `./gradlew goldenScreenshot`

### Audio PR-3E：职业音频包

- **必交**:
  - `audio.profession.rogue`
  - `audio.profession.templar`
  - `16` 个冻结 talent cue
  - Rogue 机动 / 隐匿类、Templar HOLY / 护盾 / 净化类 signature cue
- **验收**:
  - 两职业正式路径不再依赖 silence fallback
- **验证**:
  - `./gradlew audioLint`
  - `./gradlew clientSmoke`

## Sprint 4: Build The Real Zone Route

**目标**: 把 `4 zone` 串成真实短局，并让 route 上的 zone 表现开始成立。  
**依赖**: Sprint 3  
**Sprint Exit**:

1. save/load 能恢复 route 进度
2. zone 切换发生在同一 run 中
3. route 关键视觉 / 音频锚点成立

### PR-4A：`zoneRoute + routeIndex` 存档合同

- **范围**:
  - `FoundationGameConfig`
  - `SaveSnapshot`
  - `SessionSnapshotMapper`
- **验收**:
  - save/load 能恢复 route 位置
- **验证**:
  - `./gradlew :game:test --tests com.ktome.game.SessionSnapshotMapperTest`

### PR-4B：session 内 zone transition

- **范围**:
  - `FoundationGameSession`
  - route completion / victory condition / auto-save
- **验收**:
  - 非终点 zone 结束后切到下一区
  - victory 只在最终 route 终点发生
- **验证**:
  - `./gradlew :game:test --tests com.ktome.game.FullGameLoopAutomationTest`

### PR-4C：`ZoneChainSmokeTest`

- **范围**:
  - game harness
  - report writer
- **验收**:
  - 失败时保留 `seed / zoneRoute / routeHash / commandTraceHash`
- **验证**:
  - `./gradlew :game:test --tests com.ktome.game.harness.ZoneChainSmokeTest`

### Visual PR-4D：route 关键路径资产

- **必交**:
  - `zone.greenwood_fringe.visual/icon`
  - `zone.deep_iron_pit.visual/icon`
  - `zone.grey_gate_depths.visual/icon`
  - route transition 必需的 stairs / objective / interactable prop 锚点
- **验收**:
  - 跨 zone 时场景标识与 route 锚点可被正式渲染
- **验证**:
  - `./gradlew manifestLint`
  - `./gradlew goldenScreenshot`

### Audio PR-4E：route 关键路径音频

- **必交**:
  - `ambient.greenwood_fringe`
  - `ambient.deep_iron_pit`
  - `ambient.grey_gate_depths`
  - `zone transition / stairs / route-complete / objective-progress` cue
- **验收**:
  - route 切换时背景音和交互 cue 正确切换
- **验证**:
  - `./gradlew audioLint`
  - `./gradlew clientSmoke`

## Sprint 5: Reach The P2-C Content Floor

**目标**: 达到 `24 怪 + 24 物品 + 4 zone identity`，并把新增内容纳入正式资源矩阵。  
**依赖**: Sprint 4  
**Sprint Exit**:

1. `24` 怪、`24` 物品下限达标
2. 每个 zone 至少有自己的 runtime identity
3. 新增内容全部进入 canonical asset/audio 入口

### PR-5A：monster matrix 补齐

- **范围**:
  - `monsters/index.yaml`
  - `zones/index.yaml`
  - AI / boss / reward 配置
- **验收**:
  - `18 normal + 4 elite + 2 boss`
  - 每 zone 至少有 melee / ranged / controller / elite / signature 组合
- **验证**:
  - `./gradlew :game:test --tests com.ktome.game.data.MonsterSchemaTest`
  - `./gradlew contractLint`

### PR-5B：item matrix 补齐

- **范围**:
  - `items/index.yaml`
  - `loot/index.yaml`
  - 相关 reward 配置
- **验收**:
  - `6 weapon + 6 armor + 4 accessory + 6 consumable + 2 quest/boss/reward`
  - 每职业至少一件 signature reward
- **验证**:
  - `./gradlew :game:test --tests com.ktome.game.data.SchemaV2LoaderTest`

### PR-5C：zone objective / interactable / mini-boss / signature reward runtime 化

- **范围**:
  - `zones/index.yaml`
  - `objectives/index.yaml`
  - `interactables/index.yaml`
  - `bosses/index.yaml`
  - `FoundationGameSession`
- **验收**:
  - 中间区不再只是换 tileset
  - 终区不再只靠最终 Boss 单点撑住
- **验证**:
  - `./gradlew :game:test`
  - `./gradlew headlessSmoke`

### PR-5D：怪物 acc/eva 与 simple scripted AI 收口

- **范围**:
  - monster schema
  - entity factory
  - AI schema / boss harness
- **验收**:
  - 24 怪不再全部共享相同命中 / 闪避 / 行为壳
- **验证**:
  - `./gradlew :game:test`
  - `./gradlew longRunLab`

### Visual PR-5E：内容资产包

- **必交**:
  - 每个新增 zone signature monster 的正式 actor key
  - 每个 zone 至少一组 objective / interactable prop
  - 每个职业至少 `1` 件 signature reward icon
  - `grey_gate_depths / dungeon_lord / 中间区 mini-boss` 的正式 visual/icon
- **验收**:
  - 新增 content key 不再只停在 YAML
- **验证**:
  - `./gradlew assetLint`
  - `./gradlew manifestLint`

### Audio PR-5F：内容音频包

- **必交**:
  - zone signature 怪、Boss、objective、reward、interactable cue
  - signature reward 与 zone 机制 cue
- **验收**:
  - 新增 content 音频 key 全部有 runtime entry
- **验证**:
  - `./gradlew audioLint`

## Sprint 6: Realign The Gates

**目标**: 把 gate 从“代码和 schema 绿”升级到“代码 + 内容 + 正式表现路径都绿”。  
**依赖**: Sprint 5  
**Sprint Exit**:

1. gate 不再只代表 `PR-06` slice
2. formal-path required key 被纳入必检集合
3. 报告字段与失败工件契约固定

### PR-6A：`Phase2ContentCoverageTest`

- **范围**:
  - `tools/src/test/kotlin/com/ktome/tools/lint/*`
- **固定断言**:
  - `4 profession`
  - `4 zone`
  - `>=24 monsters`
  - `>=24 items`
  - 4 职业正式 talent / startingTalents / route 关键配置完整
- **验证**:
  - `./gradlew :tools:test --tests com.ktome.tools.lint.Phase2ContentCoverageTest`

### PR-6B：扩展 `headlessSmoke / FullGameLoop / longRunLab`

- **范围**:
  - `HeadlessSmokeSuiteTest`
  - `FullGameLoopAutomationTest`
  - `LongRunLabTest`
- **验收**:
  - 绿灯不再只覆盖 `vanguard / arcanist + shattered_outpost`
- **验证**:
  - `./gradlew headlessSmoke`
  - `./gradlew longRunLab`

### PR-6C：扩展 `clientSmoke / goldenScreenshot`

- **范围**:
  - `ClientSmokeHarnessTest`
  - `GoldenScreenshotHarnessTest`
- **验收**:
  - route 中后段 continue、四职业 formal path、reward/inspect/UI cue 都进入自动化覆盖
- **验证**:
  - `./gradlew clientSmoke`
  - `./gradlew goldenScreenshot`

### Visual PR-6D：formal-path required visual key 清单入 gate

- **范围**:
  - `asset_pipeline_common.py`
  - `manifest-lint.py`
  - 相关 spec / manifest
- **必检集合**:
  - profession、zone、boss、objective、reward、talent、tree、core UI cue
- **验收**:
  - formal path required visual keys 不能再走 `missing_visual`
- **验证**:
  - `./gradlew assetLint`
  - `./gradlew manifestLint`

### Audio PR-6E：formal-path required audio key 清单入 gate

- **范围**:
  - `audio-lint.py`
  - audio plan / manifest
- **必检集合**:
  - profession、ambient、boss warning、signature talent、objective/interactable、reward cue
- **验收**:
  - formal path required audio keys 不能再走 `audio/fallback/silence.ogg`
- **验证**:
  - `./gradlew audioLint`

## Sprint 7: Formal Path Closure

**目标**: 完成 `P2-C` 正式路径收口，不再让 `phase2` 名义下的关键入口继续使用 placeholder。  
**依赖**: Sprint 6  
**Sprint Exit**:

1. formal path required key 视觉 `0 missing_visual`
2. formal path required key 音频 `0 silence.ogg`
3. 文档入口、gate、manifest 统计口径一致

### PR-7A：P2-C 资产计划与 manifest 收尾

- **范围**:
  - `assets-src/image/specs/phase2-asset-plan.yaml`
  - `assets-src/image/manifests/phase2-visual-manifest.json`
  - `client/src/main/resources/manifests/visual-manifest.json`
  - bundled spec catalog
- **验收**:
  - P2-C formal path 关键 key 全部有 canonical / runtime 对应项
- **验证**:
  - `./gradlew assetLint`
  - `./gradlew styleLint`
  - `./gradlew manifestLint`

### PR-7B：P2-C 音频计划与 runtime 收尾

- **范围**:
  - `assets-src/audio/specs/phase2-audio-plan.yaml`
  - canonical/runtime audio manifest
  - `audioProcess` 产物
- **验收**:
  - P2-C formal path 关键 cue 全部进入 runtime，来源记录完整
- **验证**:
  - `./gradlew audioProcess`
  - `./gradlew audioLint`

### PR-7C：placeholder budget gate

- **范围**:
  - lint 脚本
  - manifest 报告字段
- **规则**:
  - 视觉：formal path required keys `0 missing_visual`
  - 音频：formal path required keys `0 silence.ogg`
  - 非 formal / debug key 可以保留预算，但必须显式统计
- **验收**:
  - 报告能清楚区分“正式路径未完成”和“剩余 debug 预算”
- **验证**:
  - `./gradlew assetLint`
  - `./gradlew manifestLint`
  - `./gradlew audioLint`

### PR-7D：文档入口同步

- **范围**:
  - `docs/phase2/roadmap.md`
  - `docs/phase2/2026-03-13-phase2-pr-07-short-run-expansion.md`
  - `docs/phase2/2026-03-13-phase2-verification-checklist.md`
- **验收**:
  - 新执行版文档能从 Phase 2 入口直接定位
- **验证**:
  - 人工检查文档交叉引用是否正确

---

## 5. Phase 2 最终门禁

### 5.1 代码 / 内容 / 客户端门禁

最终完成态必须通过以下 root gate：

```bash
./gradlew test
./gradlew :core:test
./gradlew :game:test
./gradlew headlessSmoke
./gradlew clientSmoke
./gradlew soloClearLab
./gradlew longRunLab
./gradlew localeLint
./gradlew contractLint
./gradlew goldenScreenshot
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
./gradlew preReleaseAcceptance
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

### 5.2 Formal Path Asset Gate

以下资源集合命中 Phase 2 完成态，必须有真实图像和真实音频，不允许依赖 placeholder：

#### Profession

1. 4 个职业 actor / portrait
2. 12 棵 talent tree 的 `tree.*`
3. 32 个正式 talent 的 skill icon
4. Rogue / Templar 的 signature visual / cue

#### Zone

1. 4 个 zone 的 `visual / icon`
2. 4 套 tileset family 的 ground / wall 核心切片
3. zone-specific objective / interactable prop
4. `ambient.*` 与 route transition cue

#### Boss / Reward

1. `bandit_captain`、`dungeon_lord`、中间区 mini-boss 的 actor / icon / cue
2. signature reward item 的 icon / cue

#### UI / Semantic

1. 6 个 damage type icon
2. boss warning / telegraph vfx
3. `confirm / cancel / hover / inspect / reward / open / stairs` 等核心 UI cue

### 5.3 报告字段契约

凡是 route / smoke / stability / solo clear / client smoke / golden 涉及的正式报告，至少要带：

1. `seed`
2. `profession`
3. `zoneRoute`
4. `routeHash`
5. `commandTraceHash`
6. `locale`
7. `scriptVersion`
8. `phaseId`

失败时至少保留：

1. failing seed
2. input script
3. snapshot / hash
4. log token 输出
5. screenshot diff
6. 对应 harness 报告片段

---

## 6. 验证、资源产线与提交纪律

### 6.1 自动化与白盒验证纪律

1. 每个 sprint 结束必须有可运行、可测试、可白盒演示的增量
2. 凡是涉及 `client / route / zone identity / reward / boss warning / resource HUD` 的改动，都必须附白盒走查步骤
3. 不得用“理论上应该通过”替代已验证结论

### 6.2 资源产线与提交纪律

图像与音频必须按以下顺序推进：

1. 定义 key 与需求面
2. 更新 `phase2-asset-plan.yaml` 或 `phase2-audio-plan.yaml`
3. 更新 canonical manifest
4. 执行 `syncPhase2Manifests`
5. 如有真实资源产出，再执行：
   - `scripts/generate_assets.sh`
   - `scripts/process_assets.py`
   - `./gradlew audioProcess`
6. 最后跑：
   - `./gradlew assetLint`
   - `./gradlew styleLint`
   - `./gradlew manifestLint`
   - `./gradlew audioLint`

没有 `GEMINI_API_KEY` 时，只允许推进：

1. spec
2. manifest
3. lint
4. placeholder budget

此时不得把“图像已生产完成”写成已完成事实。

### 6.3 formal path 与 debug budget 的边界

允许保留预算的条目：

1. `affix.*`
2. `material.*`
3. `difficulty.*`
4. 未进入 Phase 2 完成态的 debug / internal-only key

不允许保留预算的条目：

1. profession formal path
2. zone formal path
3. boss formal path
4. route objective / interactable formal path
5. signature reward / core talent / core UI cue formal path

---

## 7. 明确延后到 Phase 3+

### 7.1 延后项

以下内容不属于本轮：

1. 多阶段 Boss 战
2. 递减抗性与完整穿透收益递减
3. 高级难度模式
4. Meta progression
5. 随机事件大系统
6. 套装 / 深词缀 / 复杂装备联动
7. 完整 AI Profile DSL
8. 动态战斗音乐

### 7.2 当前过渡措施

1. Boss 只保留 `minimal warning + telegraph + simple scripted AI`
2. 抗性只做简化线性模型
3. 难度只保留 `normal`
4. 事件系统只通过 objective / interactable 承载最小玩法锚点
5. 音乐只保留 ambience 与基础 cue，不做战斗态切换

---

## 8. Phase 2 最终完成定义

只有同时满足以下条件，才允许对外宣称 “Phase 2 已完成”：

1. `P2-B` 仍稳定
2. `P0` 技术债已收口
3. `P1` 中与职业、成长、伤害类型相关的关键项已落地
4. `Rogue / Templar` 正式化完成
5. `4 zone` 形成真实短局 route
6. `24 怪 + 24 物品` 下限达成
7. gate 已重对齐到 `P2-C`
8. formal path required visual keys `0 missing_visual`
9. formal path required audio keys `0 silence.ogg`
10. `docs/phase2` 的入口文档、执行版文档、checklist 与 gate 一致

在此之前，仓库最多只能被描述为：

`Phase 2 的 P2-B 已完成，P2-C 正在收口。`

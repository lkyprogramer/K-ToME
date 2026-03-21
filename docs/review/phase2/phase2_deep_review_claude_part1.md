# Phase2 深度审查报告 — Part 1

**审查日期**: 2026-03-21
**审查视角**: 资深 Roguelike / 类 ToME 游戏设计总监 + 系统策划总监 + 玩法体验审查负责人
**审查范围**: K-ToME Phase 2 全部设计文档、代码实现、数据配置、资源管线、测试套件
**当前分支**: `codex/p2-w7-sprint0`

---

## 1. 执行摘要

### 1.1 整体判定

**当前 Phase 2 的状态是：「功能骨架完整，体验闭环未成立」。**

更精确地说：P2-B（2 职业 + 1 zone 最小切片）已稳定；P2-C（4 职业 + 4 zone 完整短局）仍处于「schema 与数据已铺设，但核心游玩循环尚未真正闭合」的阶段。

### 1.2 十条核心结论

1. **合同层高度完整**：Schema V2、i18n、manifest、save/load、render snapshot、资源管线、lint 体系——Phase 2 的「语义合同」层建设质量远超同类项目同阶段水准，为后续扩展奠定了极好的基础。

2. **4 职业数据已全部就位，但 Rogue / Templar 仍是「可被加载的 schema」而非「可被体验的职业」**：它们的天赋虽然已在 `TalentResolver.supportedTalentIds` 中注册，YAML 定义完整，但尚未经过 SoloClearLab 验证闭环（执行计划 Sprint 3 尚未启动）。

3. **核心循环存在关键断裂**：战斗 → 奖励 → 成长 → 驱动下次战斗，这条链的「奖励」环节几乎缺失——怪物死亡仅给 XP，不产生物品掉落（loot profile 数据存在但未接入 runtime 战斗流）。这是当前最严重的体验缺陷。

4. **资源消耗存在双轨问题**：Stamina 通过旧的 `Stamina` 组件直接扣减，其余资源通过 `ResourcePools` 扣减。`PlayerResourcePools.syncStaminaPoolFromComponent` 试图弥合但本质上是胶水代码，会导致 save/load 和 HUD 同步潜在不一致。

5. **天赋解锁节奏严重失衡**：所有职业的 `startingTalents` 直接给出 4 个技能（含解锁等级 4~5 的高级技能），1 级即获得全部核心工具箱，彻底消灭了成长感与探索驱动。

6. **DamageType 六通道设计文档完善，但战斗中几乎无感**：所有怪物默认 0 抗性、0 穿透，HOLY +50% 对亡灵的特殊规则存在于文档但未在 monster schema 中体现为实际 resistance 数据。

7. **4 zone 已在 YAML 中定义，但尚未形成真实 route**：`FoundationGameConfig.zoneRoute` 默认只含 `listOf(zoneId)` 即单 zone，zone transition 逻辑和 route save/load 尚未实现（对应执行计划 Sprint 4）。

8. **测试基础设施极强，但 gate 仍对齐 P2-B 而非 P2-C**：headlessSmoke、clientSmoke、goldenScreenshot 主要覆盖 vanguard + shattered_outpost 切片，四职业 route 覆盖和 formal path 验收尚未纳入 CI gate。

9. **内容量数字上达标（24 怪 + 24 物品），但体验分化不足**：怪物区别主要靠 HP 和 archetype 标签，实际战斗行为高度同质（AI 仅 CHASE/KITE/PATROL 三种）；物品区别主要靠 baseAttack/baseDefense 数值，没有主动效果或被动特性。

10. **资源产线（图像/音频）在 formal path required key 层面已清零，但大量 affix/material/difficulty 条目仍是 placeholder**：这不阻塞 Phase 2 完成，但会影响后续 Phase 3 的物品体验深度。

### 1.3 一句话定性

> 当前版本是一个 **「拥有出色工程骨架和完整数据层，但游玩体验尚未达到最低可玩标准」** 的项目。它的价值在于地基极其牢固，但地基之上的「好玩」还没有建起来。

---

## 2. 审阅范围与依据

### 2.1 参考文档

| 文档 | 路径 | 角色 |
|------|------|------|
| Phase 2 总纲 | `docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md` | 阶段目标与边界定义 |
| PR-01 序列化 | `docs/phase2/2026-03-13-phase2-pr-01-serialization-and-version-discipline.md` | Save V2 合同 |
| PR-02 核心合同 | `docs/phase2/2026-03-13-phase2-pr-02-core-semantic-contracts.md` | 资源/伤害/状态合同 |
| PR-03 Locale & Schema V2 | `docs/phase2/2026-03-13-phase2-pr-03-locale-and-schema-v2.md` | i18n + 数据规范 |
| PR-04 快照与 Manifest | `docs/phase2/2026-03-13-phase2-pr-04-snapshot-and-manifest.md` | 渲染 + 资源解析 |
| PR-05 Tile Shell | `docs/phase2/2026-03-13-phase2-pr-05-minimal-tile-shell.md` | UI/渲染壳层 |
| PR-06 最小切片 | `docs/phase2/2026-03-13-phase2-pr-06-minimal-official-slice.md` | P2-B 切片设计 |
| PR-07 短局扩展 | `docs/phase2/2026-03-13-phase2-pr-07-short-run-expansion.md` | P2-C 完整短局目标 |
| Post-Review 执行计划 | `docs/phase2/2026-03-20-phase2-pr-07-post-review-execution-plan.md` | 收口执行顺序 |
| 验证清单 | `docs/phase2/2026-03-13-phase2-verification-checklist.md` | 门禁与验收 |
| 路线图 | `docs/phase2/roadmap.md` | 文档索引 |

### 2.2 参考代码

| 模块 | 路径 | 核心关注 |
|------|------|---------|
| core ECS/Combat/Talent | `core/src/main/kotlin/com/ktome/core/` | 战斗公式、天赋解算、资源模型 |
| game Session/Config/Data | `game/src/main/kotlin/com/ktome/game/` | 游戏主循环、数据加载、快照 |
| game YAML 数据 | `game/src/main/resources/data/` | 职业/怪物/物品/区域/掉落/天赋配置 |
| game i18n | `game/src/main/resources/i18n/` | en-US.json (471 条), zh-CN.json |
| client 渲染/音频 | `client/src/main/kotlin/com/ktome/client/` | Tile 渲染、音频路由、资产加载 |
| 测试套件 | `*/src/test/kotlin/` | 25+ game tests, 24+ core tests, 12+ client tests |
| 资源规格 | `assets-src/*/specs/` | 图像/音频规格与产线 |
| Manifest | `client/src/main/resources/manifests/` | visual-manifest.json, audio-manifest.json |

### 2.3 审阅方法

1. 逐份阅读 Phase 2 全部设计文档，提取每项承诺与完成标准
2. 逐项对照代码实现、数据配置、资源文件，建立一致性映射
3. 模拟玩家视角走查核心循环：开局 → 战斗 → 奖励 → 成长 → route 推进
4. 从 Roguelike 设计经验出发，审查系统联动性、构筑深度、奖励驱动
5. 识别当前 Phase 2 必须解决的问题，区分可延后项

---

## 3. Phase2 设计实现一致性矩阵

### 3.1 P2-W1：序列化与版本纪律

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| Save Schema V2 | 13 字段 SaveRoot, 语义 key | **已实现** | `core/.../SaveSnapshot.kt`, `SaveCodec.kt` | 无偏差 | — |
| SaveContractVersion | 版本纪律 + fail-fast | **已实现** | `core/.../SaveContractVersion.kt` (v2.1) | 无偏差 | — |
| 1000 energy 容量冻结 | 标准化基础能量值 | **已实现** | `Stamina` 组件初始化 | 无偏差 | — |
| Round-trip 测试 | save/load 往返稳定 | **已实现** | `SessionSnapshotMapperTest.kt` | 无偏差 | — |

### 3.2 P2-W2：核心语义合同

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| ResourcePool 四资源 | STAMINA/MANA/ENERGY/POSITIVE_ENERGY | **已实现但存在双轨** | `PlayerResourcePools.kt`, `Stamina` 组件 | STAMINA 仍由旧组件直管，ResourcePools 做镜像同步；其他三种资源直接走 ResourcePools | **High** |
| DamageType 六通道 | PHYSICAL/FIRE/COLD/LIGHTNING/HOLY/SHADOW | **已实现但无实战差异** | `DamageType.kt`, `CombatResolver.kt` L36-58 | 枚举存在，CombatResolver 有元素抗性计算，但所有怪物 0 抗性 → 实战中 PHYSICAL 和 FIRE 伤害没有区别 | **High** |
| 16 核心状态效果 | StatusEffectType 枚举 + 堆叠规则 | **部分实现** | `TalentModels.kt` StatusEffectType | 枚举定义完整（STUNNED, ARMOR_BREAK, WAR_CRY_BUFF/DEBUFF, GUARD_STANCE_BUFF 等），但仅约 6~8 种在 TalentResolver 中真实使用，其余为未接入的预留 | **Medium** |
| Combat DTO shell | DamageRequest→Packet→Outcome→Trace | **偏离实现** | `CombatResolver.kt` 返回 `CombatResult` | 文档设计的是四步 DTO 链，实际是单一 `CombatResult` + `DamageResult`，没有 Trace 记录 | **Low** |
| TalentDef V2 | 通用资源消耗 + levelEffects | **已实现** | `talents/index.yaml`, `TalentResolver.kt` | 32 个天赋全部定义且全部在 supportedTalentIds 中注册 | — |
| Event Bus | 回调注册 | **未实现** | 文档提到但代码中没有 EventBus 类 | 当前通过 Session 直接调用替代，不影响 Phase 2 功能但会影响扩展性 | **Low** |
| FoundationGameSession 分解 | 首次分解 pass | **部分实现** | `FoundationGameSession.kt` (3113 行) | 仍是一个巨型类，GameModule/PlayerResourcePools/SessionSnapshotMapper 承担了部分职责，但主循环仍集中 | **Medium** |

### 3.3 P2-W3：Locale 与 Schema V2

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| nameKey/descKey/visualKey/iconKey/audioProfile | 所有正式对象必带 | **已实现** | 全部 YAML 条目 schemaVersion: 2 | 无偏差，严格执行 | — |
| i18n 双语 | en-US + zh-CN | **已实现** | `i18n/en-US.json` (471 条), `zh-CN.json` | 覆盖完整 | — |
| Locale 选择与切换 | 主菜单切换 + save/load 保持 | **已实现** | `GameApp.kt` locale cycling, `LocaleSaveReloadSmokeTest.kt` | 无偏差 | — |
| locale-lint | 缺失 key 检查 | **已实现** | `LocaleLintTest.kt`, gradle task | 无偏差 | — |
| contract-lint | 字段完整性 + 交叉引用 | **已实现** | `ContractLintTest.kt`, gradle task | 无偏差 | — |
| ProfessionDef / TalentDef / MonsterTemplateV2 / BossEncounterDef / ZoneSpec 最小对象壳 | 全部 schema 字段就位 | **已实现** | `SchemaModels.kt`, YAML 配置 | 无偏差 | — |

### 3.4 P2-W4：快照与 Manifest

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| RenderSnapshot | 5 核心字段组 | **已实现** | `RenderSnapshot.kt`, `RenderSnapshotContractTest.kt` | 无偏差 | — |
| VisualManifest / AudioManifest | 唯一运行时解析源 | **已实现** | `visual-manifest.json`, `audio-manifest.json` | 无偏差 | — |
| Golden Screenshot | 固定参数基线 | **已实现** | `GoldenScreenshotHarnessTest.kt` | 无偏差 | — |
| Asset Pipeline | spec→import→cleanup→manifest→resolve | **已实现** | `scripts/` 下完整管线 | 无偏差 | — |
| 4 lint 类型 | asset/style/audio/manifest | **已实现** | gradle tasks + python scripts | 无偏差 | — |
| 三层加载 | Bootstrap→Session→Warm Cache | **已实现** | `ClientAssetLoadStrategy.kt` | 无偏差 | — |

### 3.5 P2-W5：最小 Tile Shell

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| TileRenderer 4 层合成 | terrain/prop/actor/overlay | **已实现** | `TileRenderer.kt`, `TileRenderModel.kt` | 无偏差 | — |
| 最小 HUD | HP/资源/状态/目标/日志/技能栏 | **已实现** | `FoundationGameScreen.kt` | 无偏差 | — |
| 背包/检视 Shell | key-driven + fallback icon | **已实现** | 相关 Screen 文件 | 无偏差 | — |
| UI Audio 集成 | 7 类最小 cue | **已实现** | `AudioRouter.kt` (451 行) | 无偏差 | — |

### 3.6 P2-W6：最小正式切片 (P2-B)

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| Vanguard 8 天赋 | power_strike ~ unyielding | **已实现** | `talents/index.yaml`, `TalentResolver.kt` | 8/8 全部可用 | — |
| Arcanist 8 天赋 | fireball ~ ice_prison | **已实现** | 同上 | 8/8 全部可用。**但 blink 的 levelEffects 缺少非 range 收益（执行计划已识别）** | **Medium** |
| Shattered Outpost zone | 60x40, 2 层, level 1-4 | **已实现** | `zones/index.yaml` | 无偏差 | — |
| Bandit Captain Boss | boss encounter | **已实现** | `bosses/index.yaml`, `BossFactory.kt` | 无偏差 | — |
| 起始套装 | Vanguard 4件 / Arcanist 3件 | **已实现** | `professions/index.yaml` startingKit | 无偏差 | — |
| 11 怪物 + 1 elite + 1 boss | 最小 monster pool | **已实现** | `monsters/index.yaml` | 无偏差 | — |
| 3 交互物 | armory_gate / supply_crate / alarm_bonfire | **已实现** | `interactables/index.yaml`, i18n 条目存在 | 无偏差 | — |
| 30~60 分钟切片闭环 | 完整流程 | **已实现** | `OfficialSliceStabilityTest.kt`, headlessSmoke | 可闭环但体验未验证 | — |
| 最小视觉集 | tileset.ruins + actors + icons | **已实现** | visual-manifest.json, processed 目录 | 正式资产就位 | — |
| 最小音频集 | 8 cue families | **已实现** | audio-manifest.json, client resources | 正式资产就位 | — |

### 3.7 P2-W7：短局扩展 (P2-C)

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
|-----------|-------------|-------------|---------|---------|---------|
| Rogue 8 天赋 | backstab ~ deathblow | **数据已就位 / 未经验证** | `talents/index.yaml` 8/8, `TalentResolver.kt` 全部注册 | YAML 定义完整，TalentResolver 已实现全部 8 个天赋的 resolution 逻辑，但未通过 SoloClearLab 验证闭环 | **High** |
| Templar 8 天赋 | holy_strike ~ divine_intervention | **数据已就位 / 未经验证** | 同上 | 同 Rogue 情况 | **High** |
| ENERGY 消耗/回复闭环 | PerTurn(5) + OnHit(8) | **已实现** | `PlayerResourcePools.kt` L86-87, L103 | 代码逻辑存在但未经 SoloClearLab 实战验证 | **Medium** |
| POSITIVE_ENERGY 积攒/衰减 | OnDamageTaken(15%) + OnHit(3) + DecayPerTurn(5) | **已实现** | `PlayerResourcePools.kt` L88-93, L104, L109-121 | 同上 | **Medium** |
| 4 zone 短局结构 | shattered→greenwood→deep_iron→grey_gate | **数据就位 / route 未实现** | `zones/index.yaml` 4 zone 完整, `FoundationGameConfig.kt` `FOUNDATION_ZONE_ROUTE` 定义了 4 zone list，但 `zoneRoute` 默认 `listOf(zoneId)` | zone transition session 逻辑未实现 | **Critical** |
| 24 怪 | 18 normal + 4 elite + 2 boss | **已实现** | `monsters/index.yaml` 24 条 | 数量达标 | — |
| 24 物品 | 6 weapon + 6 armor + 4 accessory + 6 consumable + 2 quest | **已实现** | `items/index.yaml` 24+ 条 | 数量达标 | — |
| SoloClearLab v1 | 3 场景 × 4 职业硬门禁 | **已实现但覆盖不完整** | `SoloClearLabTest.kt`, `SoloClearLabSupport.kt` | 测试框架存在，四职业 × 三场景结构已搭好，但 Rogue/Templar 未经验证 | **High** |
| 怪物掉落接入战斗循环 | 死亡产生物品掉落 | **未实现** | `FoundationGameSession.kt` 怪物死亡仅给 XP | 执行计划 Sprint 1 PR-1B 识别 | **Critical** |
| 统一资源消耗 | ResourcePools 唯一路径 | **未实现** | STAMINA 仍走旧路径 | 执行计划 Sprint 1 PR-1A 识别 | **Critical** |
| statGrowth 生效 | 升级时叠加属性成长 | **未实现** | `ExperienceSystem.kt` 只给 stat/talent points，不叠加 statGrowth | 执行计划 Sprint 2 识别 | **High** |
| 天赋解锁节奏 | 不再 1 级全开 | **未实现** | `professions/index.yaml` startingTalents 4 个含 unlockLevel 4-5 的技能 | 执行计划 Sprint 2 识别 | **High** |
| Formal path 资源收口 | 0 missing_visual + 0 silence.ogg for required keys | **formal path required key 已清零** | 执行计划 2.5 节, 资源三态表 | 正式路径 required key 已达标，剩余 25 visual + 13 audio placeholder 属于 debug budget | — |

### 3.8 一致性总结

**按严重级别汇总：**

| 严重级别 | 数量 | 关键问题 |
|---------|------|---------|
| **Critical** | 3 | 怪物掉落未接入、资源消耗双轨、zone route 未实现 |
| **High** | 6 | DamageType 无感、Rogue/Templar 未验证、statGrowth 未生效、天赋全开、资源双轨 |
| **Medium** | 4 | 状态效果部分未使用、blink levelEffects 空、Session 未充分分解、ENERGY/PE 未实战验证 |
| **Low** | 2 | Combat DTO 简化、Event Bus 未实现 |

**关键发现**：执行计划（`2026-03-20-phase2-pr-07-post-review-execution-plan.md`）已精准识别了上述 Critical 和大部分 High 级别问题，并给出了 Sprint 0~7 的收口顺序。**本审查的价值在于从游戏体验视角评估这些问题的实际影响，以及识别执行计划未覆盖的体验层面问题。**

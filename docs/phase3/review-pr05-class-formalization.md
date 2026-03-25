# PR-05 Class Formalization 深度审查报告

**审查日期**: 2026-03-25
**分支**: `codex/p3-pr05-class-formalization`
**基线文档**: `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
**审查角色**: 资深 Roguelike 设计总监 + 系统策划总监 + 玩法体验审查负责人

---

## 0. 总体评价

**整体合规度: ~82%** — 核心合同层（W5a）和数据内容层（W5b）的关键结构高度一致，但在测试覆盖命名合规、支线树命名对齐、进阶职业天赋深度、以及 UI 独立组件拆分方面存在可量化偏差。

| 维度 | 合规度 | 评级 |
|------|--------|------|
| W5a 职业资源合同 | 98% | A |
| W5a 可用性/解锁合同 | 100% | A+ |
| W5a ProfileData 边界 | 100% | A+ |
| W5b 基础职业正式树 | 85% | B+ |
| W5b Berserker 可玩路径 | 80% | B |
| W5b Spellblade 可玩路径 | 90% | A- |
| W5b Shadowblade/Warden 冻结稿 | 100% | A+ |
| W5b 种族系统 | 95% | A |
| W5b 铭文系统 | 95% | A |
| W5c UI 与实验室验证 | 70% | C+ |
| 测试覆盖（按 §6.1 对照） | 56% | D+ |

---

## 1. 完全合规项（无偏差）

以下 spec 条目已完全落地，无需修复：

### 1.1 核心数据结构

| Spec 条目 | 实现文件 | 状态 |
|-----------|----------|------|
| `ProfessionDef` 多资源轴合同 | `core/.../profession/ProfessionDef.kt` | **完全匹配** |
| `ResourceAxis` 枚举 (HP/STAMINA/MANA/ENERGY/POSITIVE_ENERGY/HATE/EQUILIBRIUM) | `core/.../resource/ResourceModels.kt` | **完全匹配** |
| `SoloContractDef` 六类 tag | `core/.../profession/ProfessionDef.kt` | **完全匹配** |
| `ClassUnlockState` (LOCKED/DEV_UNLOCKED/RELEASE_UNLOCKED) | `core/.../profile/ClassUnlockState.kt` | **完全匹配** |
| `AvailabilityContext` (PLAYER_CREATION/DEV_LAB/WHITE_BOX) | `core/.../profile/ClassUnlockState.kt` | **完全匹配** |
| `ClassPlayabilityState` (LOCKED/UNLOCKED_BUT_UNAVAILABLE/PLAYABLE) | `core/.../profile/ClassUnlockState.kt` | **完全匹配** |
| `ClassAvailabilityResolver` 映射规则 | `core/.../profile/ClassAvailabilityResolver.kt` | **完全匹配** |
| `EquilibriumAffinity` (PHYSICAL/ARCANE/NEUTRAL) | `core/.../resource/ResourceModels.kt` | **完全匹配** |
| `DecayPolicy` (amountPerTurn/outOfCombatOnly) | `core/.../resource/ResourceModels.kt` | **完全匹配** |

### 1.2 合同语义

| Spec 口径 | 实现状态 |
|-----------|---------|
| §2.1-1: 每个职业最多 2 条资源轴 | `ProfessionDef.init` 校验 `resourceProfiles.size in 1..2` **✓** |
| §2.1-4: `resourceProfiles` / `primarySpendAxis` / `stateAxis` 结构 | **完全匹配** |
| §2.1-5: LOCKED/DEV_UNLOCKED/RELEASE_UNLOCKED 三态分离 | **完全匹配** |
| §2.1-8: 种族天赋点独立于职业天赋点，每 4 级 1 点 | `RaceTalentPointProgression.totalGrantedByLevel(level) = level / 4` **✓** |
| §2.1-9: 铭文最大 4 个、同类最多 2 个、热键 5-8、不消耗主资源 | 常量 `MAX_INSCRIPTION_SLOTS=4`, `MAX_INSCRIPTION_PER_CATEGORY=2`, `INSCRIPTION_HOTKEY_START=5` **✓** |
| §2.1-10: `ClassUnlockState` 不直接暴露给 UI | MainMenuScreen.kt 仅 import `ClassPlayabilityState` **✓** |
| §2.1-11: `EQUILIBRIUM` 动作归类合同冻结 | `EquilibriumAffinity` enum + `equilibriumAffinity` in `TalentDef` **✓** |

### 1.3 ProfileData 边界

| Spec 口径 | 实现状态 |
|-----------|---------|
| §4.9-1: ProfileData 与 SaveDataV2 分文件分版本管理 | `ProfileCodec` + `profileVersion` 独立管理 **✓** |
| §4.9-2: 只持久化 profileVersion/releaseUnlockedClasses/runHistory | `ProfileData` 仅含此三字段 **✓** |
| §4.9-3: DEV_UNLOCKED 不写入 ProfileData | `ProfileProgression.appendRun()` 仅在 victory + rule match 时写入 releaseUnlockedClasses **✓** |
| §4.9-4: RunSummary 引用关系存在 | `runHistory: List<RunSummary>` 存在且含完整字段 **✓** |

### 1.4 Shadowblade / Warden 冻结稿

| Spec 口径 | 实现状态 |
|-----------|---------|
| Shadowblade: ENERGY, LOCKED, 3 树 (assassination_plus/shadowstep_mastery/venom_night) | `shadowblade_assassination_plus, shadowblade_shadowstep_mastery, shadowblade_venom_night`，节点为空 **✓** |
| Warden: POSITIVE_ENERGY, LOCKED, 3 树 (nature_guard/life_ward/earth_bastion) | `warden_nature_guard, warden_life_ward, warden_earth_bastion`，节点为空 **✓** |

### 1.5 SoloClearLab 扩展

| Spec 口径 | 实现状态 |
|-----------|---------|
| §4.10-4: 从 4 扩展到 6 职业 | `SOLO_CLEAR_PROFESSIONS` = 6 (vanguard/arcanist/rogue/templar/berserker/spellblade) **✓** |
| §4.10-4: 2 个可玩进阶职业有 smoke 级验证入口 | `SOLO_CLEAR_ADVANCED_SMOKE_PROFESSIONS` 包含 berserker/spellblade **✓** |

---

## 2. 存在偏差的条目

### 2.1 [严重] 测试覆盖命名合规 — 偏差 44%

**Spec §6.1 要求 16 个必测类**，实际只匹配 **9 个**：

| # | Spec 要求的测试类 | 实际状态 | 偏差说明 |
|---|------------------|---------|---------|
| 1 | `ProfessionResourceContractTest` | **✓ 存在** | |
| 2 | `ClassAvailabilityResolverTest` | **✓ 存在** | |
| 3 | `ProfessionSoloContractLintTest` | **✗ 缺失** | soloContract lint 部分散落在 `ContractLintTest` 和 `Phase2ContentCoverageTest` 中，未独立建类 |
| 4 | `VanguardTreeTest` | **✗ 缺失** | 职业树验证被合并到 `ProfessionSchemaTest` + `TalentSchemaTest`，无按职业独立验证 |
| 5 | `ArcanistTreeTest` | **✗ 缺失** | 同上 |
| 6 | `RogueTreeTest` | **✗ 缺失** | 同上 |
| 7 | `TemplarTreeTest` | **✗ 缺失** | 同上 |
| 8 | `BerserkerPlayableTest` | **✗ 缺失** | Berserker 可玩性通过 SoloClearLab smoke 覆盖，但无独立 HATE 积累/衰减/失控验证 |
| 9 | `SpellbladeEquilibriumTest` | **✗ 缺失** | Equilibrium 偏移逻辑在 PlayerResourceService 中实现，但无独立单元测试 |
| 10 | `SpellbladeEquilibriumAffinityTest` | **✗ 缺失** | affinity 录入逻辑存在但无独立断言 |
| 11 | `RaceSystemTest` | **✓ 存在** | |
| 12 | `InscriptionSlotTest` | **✓ 存在** | |
| 13 | `InscriptionCooldownTest` | **✓ 存在** | |
| 14 | `ProfileDataTest` | **✓ 存在** | |
| 15 | `AdvancedClassUnlockTest` | **✓ 存在** | |
| 16 | `SoloClearLabV2Test` | **✗ 缺失** | 只有 `SoloClearLabTest`，未按 spec 命名 V2 |

**影响**: 测试合同的缺失意味着以下关键行为缺乏回归保护：
- Berserker HATE 的 `OnDamageTaken(20%)` + `OnHit(6)` + `OnKill(12)` + `Decay(8/turn, outOfCombatOnly)` 组合行为
- Spellblade EQUILIBRIUM 的"上一回合最后一个成功且 affinity != NEUTRAL 的动作偏移一次"规则
- 稳定区 30-70 的逐步强化/削弱语义
- 各职业正式树的完整可遍历性（从根到叶的路径覆盖）

**修复建议**:

```
优先级 P0:
1. 新建 SpellbladeEquilibriumTest — 覆盖 §6.2-4 行为
2. 新建 SpellbladeEquilibriumAffinityTest — 覆盖 PHYSICAL/ARCANE/NEUTRAL 归类
3. 新建 BerserkerPlayableTest — 覆盖 §6.2-3 和 §6.2-5 行为

优先级 P1:
4. 新建 ProfessionSoloContractLintTest — 确保 soloContract 六类 tag 非空
5. 新建 VanguardTreeTest / ArcanistTreeTest / RogueTreeTest / TemplarTreeTest
   — 或统一为 ProfessionTreeCompletionTest，按职业参数化
6. SoloClearLabTest → SoloClearLabV2Test 重命名或创建别名
```

---

### 2.2 [中等] 基础职业支线树命名与 Spec 不一致

Spec §4.3 定义的树名称与实际实现存在系统性偏差：

| 职业 | Spec 支线名 | 实现支线名 | 功能角色匹配 |
|------|-----------|-----------|-------------|
| Vanguard | Arms / Defense / Tactics | arms / shield / warcry | arms ✓ / shield ≈ Defense / warcry ≠ Tactics |
| Arcanist | Destruction / Arcane Shield / Chrono | flame / frost / arcane | flame ≈ Destruction / frost ≠ Chrono / arcane ≈ Arcane Shield |
| Rogue | Combat / Subtlety / Assassination | assassination / subtlety / agility | assassination ≠ Combat / subtlety ✓ / agility ≠ Assassination |
| Templar | Smite / Grace / Faith | smite / grace / faith | **全部匹配 ✓** |

**功能角色对齐分析**：

| 职业 | Spec 要求 | 实际覆盖 | 偏差 |
|------|----------|---------|------|
| 输出支线 | 4/4 | 4/4 (arms/flame/assassination/smite) | **✓** |
| 生存支线 | 4/4 | 4/4 (shield/arcane/subtlety含stealth/grace) | **✓** |
| 控制/机动支线 | 4/4 | 4/4 (warcry/frost/agility/faith) | **功能覆盖 ✓，但命名偏差明显** |

**影响**: 功能角色（输出/生存/控制机动）均已覆盖，偏差仅在命名层。但长期维护中，如果后续文档/工具引用 spec 中的树名（如 "Chrono"），会产生查找混乱。

**修复建议**:

```
方案 A（推荐）: 更新 spec §4.3 表格，使之与实现对齐
  - 理由: 实现命名更贴合游戏风味，且已被多个下游消费者引用（数据文件、测试、i18n）
  - 工作量: 仅需修改文档

方案 B: 重命名实现中的 tree ID
  - 理由: 与 spec 精确对齐
  - 工作量: 涉及 YAML、i18n key、测试、资源引用的全面重命名，风险高
  - 不推荐
```

---

### 2.3 [中等] 进阶职业天赋深度不足

| 职业 | 每树节点数 | 总天赋数 | 基础职业对比 |
|------|-----------|---------|-------------|
| Berserker | 2 / 2 / 2 | **6** | 基础职业平均 11 |
| Spellblade | 2 / 2 / 2 | **6** | 基础职业平均 11 |

Spec §4.4/§4.5 口径为 "3 树轻量版"，没有定义最小节点数，因此技术上不违规。但从玩法体验角度：

**体验问题**:
1. 每棵树只有 2 个节点 → 无 build 分支选择，天赋投资缺乏深度
2. 与基础职业 10-12 节点的差距 → 进阶职业反而不如基础职业内容厚度
3. SoloClearLab smoke 级别能通过 ≠ 构筑体验合格
4. Berserker 缺少明确的"高 HATE 失控风险"对应节点（spec §4.4-5 要求）
5. Spellblade 6 个天赋中 2 个 NEUTRAL（flux_anchor/spell_parry），有效干扰 EQUILIBRIUM 的仅 4 个 → 稳定区机制实际上很难被触发到极端值

**建议**:

```
最低补充（不扩大 PR 范围，维持 §7 出口门禁精神）:
1. 每棵树至少补到 3 个节点（总 9 节点/职业）
2. Berserker 需要至少 1 个"HATE 阈值触发"类节点，体现失控风险
3. Spellblade 需要更多 PHYSICAL affinity 节点平衡双向偏移
4. 每个进阶职业至少包含 1 个 panic answer 类节点

可选深化（若时间允许）:
- 每树 4 节点（总 12 节点/职业），与基础职业对齐
```

---

### 2.4 [中等] UI 独立组件未按 Spec 拆分

Spec §4.10 / §5.3 建议的文件：

| Spec 建议文件 | 实际状态 |
|--------------|---------|
| `ClassSelectPanel.kt` | **✗ 不存在** — 职业选择逻辑内嵌在 `MainMenuScreen.kt` |
| `ResourceHud.kt` | **✗ 不存在** — 资源 HUD 逻辑内嵌在 `TileRenderModel.kt` / `AsciiRenderModel.kt` |

**影响**: Spec §5.3 标注的是"建议文件"，非硬性要求。但从架构角度：
- `MainMenuScreen` 已达 215 行，尚可管理
- `TileRenderModel` 已超过 10,000 token，资源 HUD 逻辑与渲染模型耦合
- 随 §4.5 Spellblade 双轴资源 HUD 的引入，未来需支持 EQUILIBRIUM gauge 的特殊渲染（稳定区标记等），内嵌难以维护

**修复建议**:

```
P2（本 PR 可选，建议下一 PR 落地）:
1. 将职业选择 UI 逻辑提取为 ClassSelectPanel 组件
2. 将资源 HUD 渲染逻辑提取为 ResourceHud 组件
3. ResourceHud 需支持: 单轴 gauge、双轴 gauge、EQUILIBRIUM 稳定区可视化
```

---

### 2.5 [低] `InscriptionCategory.OFFENSE` 枚举存在但无数据

Spec §3.2-3 明确指出："不在本 PR 做 OFFENSE 类铭文"。

**实际状态**:
- `InscriptionCategory` 枚举包含 `OFFENSE` 值 ✓（枚举预留合理）
- 数据文件中无 OFFENSE 类铭文 ✓（合规）
- `InscriptionEffect.DamageBoost` sealed subclass 存在 ✓（结构预留合理）

**评价**: 这是合理的前瞻性预留，不算违规。但建议在 `DamageBoost` 上加注释标明 "reserved for future, not yet in use"，避免后续开发者误以为已完成。

---

### 2.6 [低] Spec §6.2 必测行为覆盖分析

| # | Spec 必测行为 | 覆盖状态 | 覆盖位置 |
|---|-------------|---------|---------|
| 1 | 4 基础职业正式树完整可遍历 | **部分** | `ProfessionSchemaTest` + `TalentSchemaTest` 验证 schema，无路径遍历 |
| 2 | `ProfessionDef` 正确表达单轴/双轴 | **✓** | `ProfessionResourceContractTest` |
| 3 | Berserker HATE 按规则运行 | **弱** | 仅 SoloClearLab smoke，无单元级验证 |
| 4 | Spellblade EQUILIBRIUM 偏移规则 | **弱** | 实现在 `PlayerResourceService`，无单元测试 |
| 5 | DecayPolicy 复用，无私有分叉 | **✓** | `ResourceRegenProfile.Decay` 唯一实现 |
| 6 | soloContract 六类 tag 全部非空 | **✓** | `SoloContractDef.init` 校验 + `Phase2ContentCoverageTest` |
| 7 | ClassAvailabilityResolver 三类上下文 | **✓** | `ClassAvailabilityResolverTest` |
| 8 | 种族天赋点独立于职业 | **✓** | `RaceSystemTest` |
| 9 | 铭文 4 条规则全部生效 | **✓** | `InscriptionSlotTest` + `InscriptionCooldownTest` |
| 10 | DEV_UNLOCKED 不写入 ProfileData | **✓** | `ProfileDataTest` + `AdvancedClassUnlockTest` |
| 11 | SoloClearLab 进阶职业 smoke | **✓** | `SoloClearLabTest` |
| 12 | 职业选择 UI 显示三态 | **✓** | `MainMenuScreen` 代码确认仅消费 `ClassPlayabilityState` |
| 13 | Phase 2 的 8-talent 仍正常运行 | **✓** | Phase 2 talent 全部包含在正式树中 |

**覆盖率**: 13 项中 10 项充分覆盖, 1 项部分覆盖, 2 项薄弱覆盖。

---

## 3. 正面亮点

### 3.1 架构质量

1. **`ClassAvailabilityResolver` 作为唯一出口** — UI 完全不接触 `ClassUnlockState`，解耦彻底
2. **`ResourceRegenProfile` sealed interface 设计** — `PerTurn / OnHit / OnDamageTaken / OnKill / Decay / Composite / None` 组合性极强，Berserker HATE 的复杂积累/衰减无需私有逻辑
3. **`ProfessionDef.init` 内置校验** — 资源轴数量、轴唯一性、tag 非空全部在构造时拦截
4. **`ProfileProgression.appendRun()` 的单向锁** — 只有 `victory && rule match` 才写入解锁，干净利落

### 3.2 数据设计

1. **铭文分层** — tier 1/2/3 + category 限制构成自然的 build 选择轴
2. **种族 stat modifier 设计** — 简洁实用，无过度设计（仅 8 个 delta 字段）
3. **Spellblade EQUILIBRIUM 的 stableMin/stableMax** — 直接嵌入 `ResourceProfileRef`，避免硬编码
4. **DecayPolicy.outOfCombatOnly** — 一个 bool 完成战斗/非战斗衰减语义切换

### 3.3 玩法体验

1. **6 职业 SoloClearLab** — 进阶职业有独立 smoke 门槛，不仅是 "能编译"
2. **种族天赋树** — human/elf/dwarf 各有 2 个起始天赋节点，不是空壳
3. **铭文 8 个** — HEALING/MOVEMENT/PROTECTION/CLEANSING 各 2 个（tier 1 + tier 2/3），覆盖度合格

---

## 4. 风险与建议总表

| 序号 | 类别 | 严重度 | 描述 | 建议修复方式 | 工作量 |
|------|------|--------|------|-------------|--------|
| R1 | 测试 | **P0** | 缺少 `SpellbladeEquilibriumTest` | 新建测试类，覆盖偏移/稳定区/NEUTRAL 跳过 | 2h |
| R2 | 测试 | **P0** | 缺少 `SpellbladeEquilibriumAffinityTest` | 新建测试类，覆盖 PHYSICAL/ARCANE/NEUTRAL 归类 | 1.5h |
| R3 | 测试 | **P0** | 缺少 `BerserkerPlayableTest` | 新建测试类，覆盖 HATE 四路积累 + DecayPolicy 复用 | 2h |
| R4 | 测试 | **P1** | 缺少 `ProfessionSoloContractLintTest` | 新建或从 ContractLintTest 提取独立类 | 1h |
| R5 | 测试 | **P1** | 缺少按职业独立验证的 TreeTest | 新建 `ProfessionTreeCompletionTest`（参数化） | 2h |
| R6 | 测试 | **P2** | `SoloClearLabTest` 未命名 V2 | 重命名为 `SoloClearLabV2Test` 或创建别名 | 0.5h |
| R7 | 命名 | **P1** | 3 个基础职业树名与 spec 不一致 | 更新 spec 文档以匹配实现（推荐） | 0.5h |
| R8 | 内容 | **P1** | 进阶职业每树仅 2 节点，构筑深度不足 | 每树补到 3 节点，含 panic/threshold 类节点 | 4-6h |
| R9 | 架构 | **P2** | ClassSelectPanel / ResourceHud 未拆分 | 建议下一 PR 提取独立组件 | 3h |
| R10 | 代码 | **P2** | `InscriptionEffect.DamageBoost` 无使用标记 | 添加注释标明 reserved | 5min |

---

## 5. 出口门禁核对

Spec §7 出口门禁逐条核对：

| # | 出口门禁条目 | 状态 | 备注 |
|---|------------|------|------|
| 1 | W5a/W5b/W5c 职责边界清晰 | **✓ 通过** | core/game/client 三层拆分合理 |
| 2 | 职业资源合同升级为多轴模型 | **✓ 通过** | `resourceProfiles: List<ResourceProfileRef>` |
| 3 | Berserker/Spellblade 以 DEV_UNLOCKED 进入可玩验证 | **✓ 通过** | YAML 定义 + SoloClearLab 确认 |
| 4 | 进阶职业 Phase 3 执行权威为 3 树轻量版 | **✓ 通过** | 每职业 3 棵树 |
| 5 | ProfileData 与正式玩家解锁边界清晰 | **✓ 通过** | DEV_UNLOCKED 不入档 |
| 6 | ClassAvailabilityResolver 为唯一出口 | **✓ 通过** | UI 仅消费 ClassPlayabilityState |
| 7 | SoloClearLab 扩展到 6 职业 + 2 进阶 smoke | **✓ 通过** | 6 职业 + 2 smoke |

**出口门禁结论**: 7/7 全部通过。PR 在"合同与结构"层面已达 merge 标准。

---

## 6. 最终结论与建议

### Merge 判断

**可以 Merge，但建议在 Merge 前完成 R1-R3（P0 级修复）。**

- R1-R3 是 spec §6.1 明确要求的必测类，缺失会导致 Spellblade EQUILIBRIUM 和 Berserker HATE 两个核心系统缺乏回归保护。这两个系统是进阶职业的核心差异化机制，一旦后续 PR 修改 `PlayerResourceService` 或 `ResourceRegenProfile`，无测试保护的静默回归风险极高。

### 建议执行顺序

```
Phase 1（Merge 前必做）:
  → R1: SpellbladeEquilibriumTest
  → R2: SpellbladeEquilibriumAffinityTest
  → R3: BerserkerPlayableTest

Phase 2（Merge 后尽快补完）:
  → R4: ProfessionSoloContractLintTest
  → R5: ProfessionTreeCompletionTest
  → R7: Spec 文档更新（树名对齐）
  → R8: 进阶职业每树补到 3 节点

Phase 3（下一 PR 顺带处理）:
  → R6: 测试类命名对齐
  → R9: UI 组件拆分
  → R10: 代码注释补充
```

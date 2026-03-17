> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
> `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 3 - PR-05 Class Formalization

**阶段**: `Phase 3 / P3-W5`  
**优先级**: `P0`  
**前置条件**: `P3-W3` 完成 + `P3-W4` schema 冻结  
**对应问题**: Phase 2 只有 4 基础职业的 8-talent 最小包，没有正式树结构、进阶职业、种族天赋、铭文系统与稳定的职业资源合同。若继续把这些内容塞进一个未拆分的工作包，后续 `core / game / client / tools` 会一起返工。

**Lane / 包拆分**：

- **W5a (Rules/Core)**: 职业资源合同、可用性/解锁合同、`ProfileData` 边界、铭文规则基线
- **W5b (Content/Game)**: 4 基础职业正式树、2 进阶职业最小可玩树、3 种族与铭文数据
- **W5c (Client + QA)**: 职业选择 UI、资源 HUD、`SoloClearLab` 扩展、开发态可用性验证

---

## 1. 阶段目标

把 Phase 2 的最小职业骨架扩展为 Phase 3 的正式多支线树结构，引入进阶职业、种族与铭文系统，并冻结**多资源轴合同 + 开发态可用性合同**。

完成标准：

1. 4 基础职业（`Vanguard / Arcanist / Rogue / Templar`）全部进入正式树（每职业至少 3 条支线）。
2. 2 进阶职业（`Berserker / Spellblade`）进入可玩路径。
3. 2 进阶职业（`Shadowblade / Warden`）的 schema、资源轴、定位与 profile 冻结。
4. 3 种族（`human / elf / dwarf`）进入主线验证。
5. 铭文系统进入 build 轴（最大 4 个、同类最多 2 个、热键 5-8、冷却制）。
6. 职业资源合同升级为多资源轴模型，不再使用单 `resourceType`。
7. 进阶职业的正式玩家解锁与开发/实验室可用性分离。
8. `SoloClearLab` 扩展到 6 职业，并给 2 个可玩进阶职业提供 smoke 级验证入口。

## 2. 当前问题

1. Phase 2 每个职业只有 8 个 talent 的最小包，没有完整的 3 支线树结构。
2. 没有进阶职业，缺少构筑深度与重复游玩动力。
3. 没有种族系统，角色差异化不足。
4. 没有铭文系统，build 轴过窄。
5. `ProfessionDef` 仍是单 `resourceType`，装不下 `Spellblade` 这类双轴职业。
6. `Berserker / Spellblade` 的资源语义在文档链里仍有冲突。
7. 进阶职业正式解锁被绑定到 `W6` 终局通关，导致 `W5` 自身无法独立验证“可玩路径已成立”。
8. `W5` 当前同时拉进职业树、双资源、种族、铭文、Profile、UI、`SoloClearLab`，已经超出单个 PR 的合理粒度。

### 2.1 本 PR 必须冻结的口径

1. 每个职业最多 2 条资源轴。
2. 每个职业至少有 1 条生存支线、1 条输出支线、1 条控制或机动支线。
3. 所有职业必须有 panic answer、位移方案和 boss answer。
4. 职业资源合同固定为多轴结构：
   - `resourceProfiles`
   - `primarySpendAxis`
   - `stateAxis`
5. 进阶职业的正式玩家解锁与开发/实验室可用性分离：
   - `LOCKED`
   - `DEV_UNLOCKED`
   - `RELEASE_UNLOCKED`
6. `W5` 验证的是 runtime/playability ready，不要求先打通 `W6` 终局解锁链。
7. Phase 3 对进阶职业的执行权威固定为**3 树轻量版**，不再隐含要求 4 树正式版。
8. 种族天赋点独立于职业天赋点，每 4 级 1 点。
9. 铭文最大 4 个、同类最多 2 个、热键 5-8、不消耗主资源，只受冷却控制。
10. `ClassUnlockState` 不直接暴露给 UI；`ClassAvailabilityResolver` 必须在给定上下文下输出唯一的 `ClassPlayabilityState`。
11. `Spellblade` 的 `EQUILIBRIUM` 必须冻结动作归类合同，不允许 combat / HUD / tooltip / lab 各自解释“什么动作会改变平衡值”。

## 3. 范围与非目标

### 3.1 范围

1. [W5a] 职业资源合同升级为多轴模型。
2. [W5a] 职业可用性 / 进阶职业解锁 / `ProfileData` 边界冻结。
3. [W5b] 4 基础职业正式树（每职业 3 支线、完整 talent）。
4. [W5b] `Berserker` 可玩路径。
5. [W5b] `Spellblade` 可玩路径。
6. [W5b] `Shadowblade / Warden` 最小冻结稿。
7. [W5b] 3 种族系统（`human / elf / dwarf` + 种族天赋点）。
8. [W5b] 铭文系统（schema + 最小覆盖 `HEALING / MOVEMENT / PROTECTION / CLEANSING`）。
9. [W5c] 职业选择 UI、资源 HUD、`SoloClearLab` 扩展。

### 3.2 非目标

1. 不在本 PR 完成 `Shadowblade / Warden` 的可玩内容（允许在 P3 尾部或 P4 补完）。
2. 不在本 PR 实现 `orc / undead` 种族的可玩内容。
3. 不在本 PR 做 `OFFENSE` 类铭文（除非先补入权威文档）。
4. 不在本 PR 做完整的种族天赋树终局设计（只做 schema 骨架 + 首批节点）。
5. 不在本 PR 把 `RunSummary` 的完整世界推进字段重复维护成第二套权威；详细 schema 由 `PR-06` 负责。

## 4. 技术方案

### 4.1 [W5a] 职业资源合同

建议文件：

```text
core/src/main/kotlin/com/ktome/core/profession/ProfessionDef.kt
core/src/main/kotlin/com/ktome/core/resource/ResourceProfileRef.kt
core/src/test/kotlin/com/ktome/core/profession/ProfessionResourceContractTest.kt
```

冻结口径：

1. `ProfessionDef` 不再只保存单 `resourceType`。
2. 最小合同固定为：

```kotlin
data class ProfessionDef(
    val id: String,
    val resourceProfiles: List<ResourceProfileRef>,
    val primarySpendAxis: ResourceAxis?,
    val stateAxis: ResourceAxis?,
    val soloContract: SoloContractDef,
)

enum class ResourceAxis {
    HP,
    STAMINA,
    MANA,
    ENERGY,
    POSITIVE_ENERGY,
    HATE,
    EQUILIBRIUM,
}
```

3. `primarySpendAxis` 表示技能通常消耗的资源轴。
4. `stateAxis` 表示职业状态、姿态或构筑张力所依赖的资源轴。
5. 若职业只有单资源轴，则 `stateAxis` 可为空或与主轴相同。
6. PR-05 的 `resourceProfiles` / `ResourceAxis` 是 `startingResources: List<ResourcePoolDef>` 的 Phase 3 语义化映射，不得与详设文档并行维护第二套资源初始化合同。

建议最小结构：

```kotlin
data class SoloContractDef(
    val offenseTags: List<String>,
    val defenseTags: List<String>,
    val mobilityTags: List<String>,
    val aoeAnswerTags: List<String>,
    val bossAnswerTags: List<String>,
    val panicAnswerTags: List<String>,
)
```

### 4.2 [W5a] 进阶职业可用性与正式解锁分离

建议文件：

```text
core/src/main/kotlin/com/ktome/core/profile/ClassUnlockState.kt
core/src/main/kotlin/com/ktome/core/profile/ClassAvailabilityResolver.kt
core/src/test/kotlin/com/ktome/core/profile/ClassAvailabilityResolverTest.kt
```

冻结口径：

1. 进阶职业状态固定为：

```kotlin
enum class ClassUnlockState {
    LOCKED,
    DEV_UNLOCKED,
    RELEASE_UNLOCKED,
}

enum class AvailabilityContext {
    PLAYER_CREATION,
    DEV_LAB,
    WHITE_BOX,
}

enum class ClassPlayabilityState {
    LOCKED,
    UNLOCKED_BUT_UNAVAILABLE,
    PLAYABLE,
}
```

2. `LOCKED`：未解锁且实验室默认不可用。
3. `DEV_UNLOCKED`：允许 `SoloClearLab`、白盒验证和开发测试使用，但不代表正式玩家已解锁。
4. `RELEASE_UNLOCKED`：正式玩家已解锁。
5. UI 不得直接根据 `ClassUnlockState` 猜可用性，一律通过 `ClassAvailabilityResolver.resolve(unlockState, context)` 输出 `ClassPlayabilityState`。
6. `PLAYER_CREATION` 上下文的唯一映射固定为：
   - `LOCKED -> LOCKED`
   - `DEV_UNLOCKED -> UNLOCKED_BUT_UNAVAILABLE`
   - `RELEASE_UNLOCKED -> PLAYABLE`
7. `DEV_LAB / WHITE_BOX` 上下文的唯一映射固定为：
   - `LOCKED -> LOCKED`
   - `DEV_UNLOCKED -> PLAYABLE`
   - `RELEASE_UNLOCKED -> PLAYABLE`
8. `W5` 的验收口径允许 `Berserker / Spellblade` 以 `DEV_UNLOCKED` 进入实验室与可玩验证，但正式玩家创建入口仍按 resolver 输出 `UNLOCKED_BUT_UNAVAILABLE`。

### 4.3 [W5a] 4 基础职业正式树

建议文件：

```text
game/src/main/resources/data/professions/vanguard.yaml
game/src/main/resources/data/professions/arcanist.yaml
game/src/main/resources/data/professions/rogue.yaml
game/src/main/resources/data/professions/templar.yaml
game/src/main/resources/data/talents/vanguard/*.yaml
game/src/main/resources/data/talents/arcanist/*.yaml
game/src/main/resources/data/talents/rogue/*.yaml
game/src/main/resources/data/talents/templar/*.yaml
```

冻结口径：

1. 每个职业 3 条支线。
2. 每个职业必须有 panic answer、位移方案和 boss answer。
3. Phase 2 的 8-talent 最小包必须全部包含在正式树中。

四基础职业概览：

| 职业 | 主轴 | 支线 1 | 支线 2 | 支线 3 |
| --- | --- | --- | --- | --- |
| `Vanguard` | `STAMINA` | Arms（输出） | Defense（生存） | Tactics（控制/机动） |
| `Arcanist` | `MANA` | Destruction（输出） | Arcane Shield（生存） | Chrono（控制/机动） |
| `Rogue` | `ENERGY` | Combat（输出） | Subtlety（机动） | Assassination（爆发） |
| `Templar` | `POSITIVE_ENERGY` | Smite（输出） | Grace（生存/净化） | Faith（光环/buff） |

### 4.4 [W5b] Berserker 可玩路径

建议文件：

```text
game/src/main/resources/data/professions/berserker.yaml
game/src/main/resources/data/talents/berserker/*.yaml
core/src/main/kotlin/com/ktome/core/resource/HateResource.kt
```

冻结口径：

1. `Berserker` 的 Phase 3 执行权威为 3 树轻量版：
   - 狂怒
   - 毁灭
   - 血战
2. 主资源轴：`HATE`。
3. `HATE` 语义固定为：
   - `OnDamageTaken` 主积累
   - `OnHit` 次积累
   - `OnKill` 可选爆发增益
   - `DecayPolicy`
4. `HATE` 的衰减行为必须显式复用详设文档的 `DecayPolicy` 合同，不允许在 Berserker 运行时私有实现一套“怒气自然回落”逻辑。
5. 高 `HATE` 段提供伤害或技能强化，但超过阈值时存在失控风险。
6. `Berserker` 在 `W5` 作为 `DEV_UNLOCKED` 进入实验室和可玩验证。

### 4.5 [W5b] Spellblade 可玩路径

建议文件：

```text
game/src/main/resources/data/professions/spellblade.yaml
game/src/main/resources/data/talents/spellblade/*.yaml
core/src/main/kotlin/com/ktome/core/resource/EquilibriumResource.kt
```

冻结口径：

1. `Spellblade` 的 Phase 3 执行权威为 3 树轻量版：
   - 附魔之刃
   - 元素涌动
   - 战斗法术
2. `Spellblade` 明确采用双轴合同：
   - `MANA` 为 `primarySpendAxis`
   - `EQUILIBRIUM` 为 `stateAxis`
3. `EQUILIBRIUM` 的动作归类合同固定为：

```kotlin
enum class EquilibriumAffinity {
    PHYSICAL,
    ARCANE,
    NEUTRAL,
}
```

4. 只有**已确认结算成功**且 `affinity != NEUTRAL` 的主动技能会改变 `EQUILIBRIUM`。
5. 普攻与近战武技默认 `PHYSICAL`；法术类主动技能默认 `ARCANE`；混合技能必须在 schema 中显式声明 affinity，未声明时回落为 `NEUTRAL`。
6. 铭文、被动触发、free action、sustain toggle 默认 `NEUTRAL`，不改变平衡值。
7. `EQUILIBRIUM` 每回合只根据上一回合最后一个成功且 `affinity != NEUTRAL` 的动作偏移一次；同回合多个动作冲突时，以结算顺序最后一个成功动作作为判定依据。
8. `30 ~ 70` 视为稳定区；超出稳定区后逐步强化一端并削弱另一端。
9. `Spellblade` 在 `W5` 作为 `DEV_UNLOCKED` 进入实验室和可玩验证。

### 4.6 [W5b] Shadowblade / Warden 最小冻结稿

本 PR 只冻结 schema、资源轴、定位和 profile；可玩内容允许在 P3 尾部或 P4 补完。

#### Shadowblade

1. 资源轴：`ENERGY`
2. 定位：单体爆发 + 暗影 DoT + 短时控制
3. 3 树方向：`assassination_plus / shadowstep_mastery / venom_night`
4. panic answer：短时隐匿 + 位移脱离
5. 初始状态：`LOCKED`

#### Warden

1. 资源轴：`POSITIVE_ENERGY`
2. 定位：神圣防护 + 区域控制 + 回复强化
3. 3 树方向：`nature_guard / life_ward / earth_bastion`
4. panic answer：短回合不死或强护盾窗口
5. 初始状态：`LOCKED`

### 4.7 [W5b] 种族系统

建议文件：

```text
core/src/main/kotlin/com/ktome/core/race/RaceDef.kt
core/src/main/kotlin/com/ktome/core/race/RaceTalentPoint.kt
game/src/main/resources/data/races/human.yaml
game/src/main/resources/data/races/elf.yaml
game/src/main/resources/data/races/dwarf.yaml
```

冻结口径：

1. Phase 3 可玩种族：`human / elf / dwarf`。
2. `orc / undead` 冻结 schema 和 profile，可玩内容允许后补。
3. 种族天赋点独立于职业天赋点，每 4 级获得 1 点。
4. 种族天赋用于投资种族天赋树。

### 4.8 [W5b] 铭文系统

建议文件：

```text
core/src/main/kotlin/com/ktome/core/inscription/InscriptionDef.kt
core/src/main/kotlin/com/ktome/core/inscription/InscriptionManager.kt
core/src/main/kotlin/com/ktome/core/inscription/InscriptionSlot.kt
game/src/main/resources/data/inscriptions/*.yaml
core/src/test/kotlin/com/ktome/core/inscription/InscriptionSlotTest.kt
core/src/test/kotlin/com/ktome/core/inscription/InscriptionCooldownTest.kt
```

冻结口径：

1. 最大铭文数 `4`。
2. 同类最多 `2`。
3. 热键 `5-8`。
4. 不消耗主资源，只受冷却控制。
5. 铭文冷却独立于天赋冷却。
6. Phase 3 主线验证至少覆盖：`HEALING / MOVEMENT / PROTECTION / CLEANSING`。
7. 铭文在非战斗和战斗状态都可正常使用。

### 4.9 [W5a] `ProfileData` 边界

建议文件：

```text
core/src/main/kotlin/com/ktome/core/profile/ProfileData.kt
core/src/main/kotlin/com/ktome/core/profile/ProfileManager.kt
core/src/test/kotlin/com/ktome/core/profile/ProfileDataTest.kt
core/src/test/kotlin/com/ktome/core/profile/AdvancedClassUnlockTest.kt
```

冻结口径：

1. `ProfileData` 与 `SaveDataV2` 分文件、分版本号管理，不允许混存。
2. `ProfileData` 只持久化：
   - `profileVersion`
   - `releaseUnlockedClasses`
   - `runHistory`
3. `DEV_UNLOCKED` 不是局间正式解锁数据，不写入 `ProfileData`。
4. `RunSummary` 的完整结构化字段由 `PR-06` 拥有单一权威；`W5` 只要求 `runHistory: List<RunSummary>` 的引用关系存在。

### 4.10 [W5c] UI 与实验室验证

建议文件：

```text
client/src/main/kotlin/com/ktome/client/ui/creation/ClassSelectPanel.kt
client/src/main/kotlin/com/ktome/client/ui/hud/ResourceHud.kt
tools/src/main/kotlin/com/ktome/tools/lab/SoloClearLab.kt
```

冻结口径：

1. 职业选择 UI 只消费 `ClassPlayabilityState`，不得直接读取 `ClassUnlockState`。
2. `Berserker / Spellblade` 在 `DEV_LAB / WHITE_BOX` 上下文下表现为 `PLAYABLE`；在 `PLAYER_CREATION` 且未正式解锁前表现为 `UNLOCKED_BUT_UNAVAILABLE`。
3. `Shadowblade / Warden` 在 `W5` 默认仍为 `LOCKED`；若某个开发工具需要展示未来职业卡片，也必须通过 resolver 输出 `UNLOCKED_BUT_UNAVAILABLE`，不得绕开状态机。
4. `SoloClearLab` 从 4 职业扩展到 6 职业，并为 2 个可玩进阶职业至少提供 smoke 级门槛。

## 5. 推荐改动面

### 5.1 `core`

1. `profession` 包扩展（多资源轴合同）。
2. `race` 包新建。
3. `inscription` 包新建。
4. `profile` 包扩展（仅维护正式解锁与 run history 边界）。
5. `resource` 包扩展（`HateResource / EquilibriumResource`）。

### 5.2 `game`

1. `professions/*.yaml` 扩展（6 个职业定义）。
2. `talents/**/*.yaml` 扩展（正式树数据）。
3. `races/*.yaml` 新建。
4. `inscriptions/*.yaml` 新建。

### 5.3 `client`

1. 职业选择 UI 扩展。
2. 多资源 HUD 扩展（支持双轴职业）。
3. 铭文栏 UI 新建。

### 5.4 `tools`

1. `SoloClearLab` 扩展到 6 职业。
2. 可玩进阶职业 smoke 验证入口。

## 6. 测试与自证

### 6.1 必测类

1. `ProfessionResourceContractTest`
2. `ClassAvailabilityResolverTest`
3. `ProfessionSoloContractLintTest`
4. `VanguardTreeTest`
5. `ArcanistTreeTest`
6. `RogueTreeTest`
7. `TemplarTreeTest`
8. `BerserkerPlayableTest`
9. `SpellbladeEquilibriumTest`
10. `SpellbladeEquilibriumAffinityTest`
11. `RaceSystemTest`
12. `InscriptionSlotTest`
13. `InscriptionCooldownTest`
14. `ProfileDataTest`
15. `AdvancedClassUnlockTest`
16. `SoloClearLabV2Test`

### 6.2 必测行为

1. 4 基础职业的正式树完整可遍历。
2. `ProfessionDef` 能正确表达单轴与双轴职业。
3. `Berserker` 的 `HATE` 能按受伤/命中/击杀/衰减规则运行。
4. `Spellblade` 的 `EQUILIBRIUM` 按上一回合最后成功且 `affinity != NEUTRAL` 的动作偏移，`30 ~ 70` 稳定区逻辑正确。
5. `DecayPolicy` 能作为 Berserker `HATE` 的正式衰减合同复用，不存在职业私有分叉实现。
6. `soloContract` 六类 tag 全部非空，且能被 `SoloClearLab` / lint 消费。
7. `ClassAvailabilityResolver` 在 `PLAYER_CREATION / DEV_LAB / WHITE_BOX` 三类上下文下输出唯一 `ClassPlayabilityState`。
8. 3 种族天赋点独立于职业天赋点。
9. 铭文系统 4 条槽位规则全部生效。
10. `DEV_UNLOCKED` 不写入 `ProfileData`，正式通关后才写入 `releaseUnlockedClasses`。
11. `SoloClearLab` 对 2 个可玩进阶职业至少有 smoke 级验证。
12. 职业选择 UI 能正确显示 `LOCKED / UNLOCKED_BUT_UNAVAILABLE / PLAYABLE`。
13. 现有 Phase 2 的 4 职业 8-talent 在正式树中仍正常运行。

### 6.3 自动化命令

```bash
./gradlew :core:test
./gradlew :game:test
./gradlew soloClearLab
./gradlew test
```

## 7. 出口门禁

1. `W5a / W5b / W5c` 的职责边界清晰，不再把整个职业正式化塞进一个未拆分工作包。
2. 职业资源合同升级为多轴模型。
3. `Berserker / Spellblade` 在 `W5` 以 `DEV_UNLOCKED` 进入可玩验证。
4. 进阶职业 Phase 3 的执行权威固定为 3 树轻量版。
5. `ProfileData` 与正式玩家解锁边界清晰，`DEV_UNLOCKED` 不进入局间档。
6. `ClassAvailabilityResolver` 已成为 `ClassUnlockState -> ClassPlayabilityState` 的唯一出口，UI 不再自行猜测状态。
7. `SoloClearLab` 扩展到 6 职业，且 2 个可玩进阶职业有 smoke 级门槛。

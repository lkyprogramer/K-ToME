> 执行前必须先完整阅读并接受：
> `docs/review/phase3/phase3_opt_deep_review_final.md`
> `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
> `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
> `docs/review/phase3/2026-03-26-phase3-pr-08-reward-milestone-affixization.md`

# Phase 3 V3 - PR-18 Base Class Breakpoint Payoff And Affix Synergy

**阶段**: `Phase 3 / v3 follow-up`  
**优先级**: `P1`  
**前置条件**: `PR-16 / PR-17` 已让完整长局的后半程玩法更成立，允许继续强化 build 感知层  
**对应问题**: 当前天赋点经济已经改善，但基础职业仍偏向“数值更高/更肉”的分化，而不是“玩法方式明显不同”。如果在 `Phase 4` 之前不先把基础职业的 build 感知补强，后续内容扩张仍会建立在“系统很多、打法感知不够强”的地基上。  

**Lane-parallel 拆分**：

- **W18a (Content Lane)**: 基础职业 tree breakpoint payoff 收口
- **W18b (Rules/Content Lane)**: affix-tag synergy 最小主链
- **W18c (QA Lane)**: talent description / loadout / long-run build 观测

---

## 1. 阶段目标

不靠大规模加 talent 数量，而是用少量高价值改动，让 `Vanguard / Arcanist / Rogue / Templar` 的 build 差异更容易被玩家真实感知。

完成标准：

1. 每个基础职业至少有 `1` 个明确的 breakpoint payoff。
2. payoff 必须能通过现有 `DescriptionModel / breakpoint preview` 清晰展示。
3. 至少有一层 affix/buildTags 联动开始真正影响玩法选择，不再只是纯数值增减。
4. 不引入新的资源轴，不改 Phase 3 已冻结的职业身份。

## 2. 当前问题

1. 基础职业虽然有 `15~16` 个 talent，但玩家更容易感受到的是“我变强了”，而不是“我这局是另一种玩法”。
2. `TalentBreakpoint` 与 `DescriptionModel` 主链已经存在，但当前缺少足够有辨识度的 payoff。
3. `affix v1` 已成立，但多数情况下仍偏向数值味道，玩家未必感知到它在推动 build。
4. 若不先把基础职业 build 感知层补强，进阶职业反而会比基础职业更容易形成“有趣的构筑”，这对主路径体验不合理。

### 2.1 本 PR 必须冻结的口径

1. 本 PR 不靠大扩容 talent 数量来解决问题。
2. 每个基础职业最多补 `1~2` 个 build-defining payoff，不把 PR 做成新的内容补量包。
3. payoff 必须优先复用现有主链：
   - `TalentBreakpoint`
   - `typed effect op`
   - `DescriptionModel`
   - `buildTags`
4. affix synergy 第一版重点是“让已有 affix 更有方向性”，不是新增一整层 Phase 4 生态。
5. 不引入新的核心枚举或第二套装备规则解释。

## 3. 范围与非目标

### 3.1 范围

1. 基础职业 breakpoint payoff
2. affix-tag synergy 最小主链
3. talent 说明与预览同步
4. build 观测与回归

### 3.2 非目标

1. 不扩成基础职业大补量
2. 不改进阶职业数量和定位
3. 不重做 affix 生态
4. 不引入新资源轴

## 4. 技术方案

### 4.1 [W18a] Base-Class Breakpoint Payoff

建议文件：

```text
game/src/main/resources/data/talents/index.yaml
game/src/main/resources/i18n/en-US.json
game/src/main/resources/i18n/zh-CN.json
client/src/test/kotlin/com/ktome/client/ui/talent/* 
```

冻结口径：

1. `Vanguard` 的 payoff 应围绕：
   - `GUARD / armor_break / taunt / hold_line`
2. `Arcanist` 的 payoff 应围绕：
   - `burn / freeze / teleport / mana tempo`
3. `Rogue` 的 payoff 应围绕：
   - `stealth / marked / crit / execute`
4. `Templar` 的 payoff 应围绕：
   - `bane / holy shield / cleanse / sustain`
5. 第一版建议做法：
   - 优先增强现有 breakpoint，而不是新增第四棵树或更多 active talent
   - 每职业至少让 `1` 个 tree 在 rank `3~4` 时产生“玩法变化”，而不是单纯数值更大
6. payoff 的描述必须在 UI 中可区分：
   - 已生效
   - 下一断点将生效

### 4.2 [W18b] Affix-Tag Synergy

建议文件：

```text
game/src/main/resources/data/items/index.yaml
core/src/main/kotlin/com/ktome/core/item/PassiveEffectResolver.kt
core/src/test/kotlin/com/ktome/core/item/* 
```

冻结口径：

1. 第一版不追求复杂装备套装，只做少量可感知联动：
   - `guard/armor_break`
   - `burn/freeze`
   - `stealth/crit/marked`
   - `holy/bane/cleanse`
2. 允许的最小做法：
   - affix 为特定 buildTags 提供更高权重
   - affix 对已有状态/伤害通道提供轻量加成
3. 禁止在本 PR 发明第二套 build 偏好真源。
4. 若现有 `EquipmentPassive` 足以表达，优先复用；只有确实不够时才最小扩展。

### 4.3 [W18c] QA And Build Observation

建议文件：

```text
game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt
game/src/test/kotlin/com/ktome/game/data/TalentSchemaTest.kt
game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt
```

冻结口径：

1. 至少补以下断言：
   - breakpoint preview 与实际效果一致
   - affix synergy 不会出现非法组合
   - 基础职业在 smoke 中能走出不同倾向的 build
2. `LongRunLab` 不要求在本 PR 内做新大矩阵，但至少要能观测：
   - payoff 是否被实际拿到
   - payoff 是否改变 buildHash 或关键战斗选择

## 5. 推荐改动面

### 5.1 `game`

1. `data/talents/index.yaml`
2. `data/items/index.yaml`
3. `i18n/en-US.json`
4. `i18n/zh-CN.json`

### 5.2 `core`

1. `PassiveEffectResolver.kt`
2. 必要时最小补充 item passive contract

### 5.3 `client`

1. talent breakpoint preview 相关测试
2. 如描述呈现需要，更新 description presenter 测试

## 6. 测试与自证

### 6.1 必测类

1. `TalentSchemaTest`
2. `FoundationGameSessionTest`
3. 相关 item/passive tests
4. `LongRunLabTest`

### 6.2 必测行为

1. 基础职业至少各有一个可感知 breakpoint payoff
2. breakpoint 预览与实际效果一致
3. affix synergy 进入正式生成/结算路径
4. payoff 不破坏现有职业资源合同

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.item.*"
./gradlew :game:test --tests "com.ktome.game.data.TalentSchemaTest"
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew :game:test --tests "com.ktome.game.harness.LongRunLabTest"
./gradlew check
```

### 6.4 白盒验证

1. 用四个基础职业各打一段 smoke
2. 确认玩家能从 talent 说明面读出 build 变化，而不是只看到更大的数字

## 7. 出口门禁

1. 四个基础职业各有至少一个 build-defining payoff
2. affix synergy 进入正式主链
3. 文案、说明、测试同步更新
4. `./gradlew check` 保持绿色

## 8. 风险与止损

### 8.1 风险

1. payoff 做得太大，会意外改变平衡曲线
2. affix synergy 做得太多，会提前长成 Phase 4 复杂生态

### 8.2 止损

1. 每职业先补一个主 payoff，不求一步到位
2. affix 先做“方向性增强”，不做“系统重写”

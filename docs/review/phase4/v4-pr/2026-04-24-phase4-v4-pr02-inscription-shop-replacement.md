> 执行前必须先完整阅读并接受：
> `docs/INDEX.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part3.md`
> `docs/review/phase4/v4/phase4_opt_deep_review_phase4_codex_part4.md`
> `docs/review/phase4/v4/phase4_profession_tree_inscription_design_supplement.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
> `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md`

# Phase4 v4 PR-02 Inscription Shop Replacement

**阶段**: `Phase 4 completion hardening / phase4-v4-pr02`
**优先级**: `P0`
**工作量**: `M`
**合并来源**: v4 P0-1 的铭文部分，职业树/铭文补充设计第 8~14 章
**前置条件**: PR-00、PR-01 完成；PR-01 产出 run 内构筑事件模型
**资源生成结论**: 不生成图片资源；不生成音频资源

## 1. 玩家体验目标

本 PR 把铭文从“开局四槽满配”改成 run 内构筑轴。玩家必须在恢复、位移、防护、净化之间做取舍，并且在商店看到铭文时能购买、安装、替换和复盘。

完成标准：

1. 玩家开局铭文数固定为 `2`。
2. 最大槽位仍为 `4`。
3. 第 3、4 槽通过 run 内商店、Boss、隐藏奖励安装。
4. 满槽购买铭文进入替换界面。
5. 替换保留热键，旧铭文销毁，不进背包，不返钱。
6. 类别上限继续为每类 `2`。
7. 满槽购买因缺替换流程直接失败次数 `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount` 为 `0`。
8. `RunSummary`、`longRunLab`、`phase4Report` 记录铭文安装与替换。

## 2. 当前问题

1. `FoundationGameSession.ensurePlayerInscriptions` 开局补满 `healing_light / phase_door / iron_shield / purge`。
2. `InscriptionManager.canEquip` 在 `slots.size >= 4` 时直接返回 false。
3. `game/src/main/resources/data/shops/index.yaml` 已经有 `controlled_phase`、`phase_door`、`purge` 等 offer，但满槽购买没有替换路径。
4. 当前商店卖铭文，但玩家没有“牺牲哪个 answer”的决策。

## 3. 范围与非目标

### 3.1 范围

生产代码：

- `core/src/main/kotlin/com/ktome/core/inscription/InscriptionManager.kt`
- `core/src/main/kotlin/com/ktome/core/inscription/InscriptionDef.kt`
- `core/src/main/kotlin/com/ktome/core/inscription/InscriptionSlot.kt`
- `core/src/main/kotlin/com/ktome/core/economy/ShopModels.kt`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/GameView.kt`
- `game/src/main/kotlin/com/ktome/game/SessionSnapshotMapper.kt`
- `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
- `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
- `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
- `client/src/main/kotlin/com/ktome/client/ui/card/ModalCardModel.kt`
- `client/src/main/kotlin/com/ktome/client/audio/AudioRouter.kt`
- `tools/src/main/kotlin/com/ktome/tools/phase4/**`

数据与文档：

- `game/src/main/resources/data/inscriptions/index.yaml`
- `game/src/main/resources/data/shops/index.yaml`
- `game/src/main/resources/i18n/en-US.json`
- `game/src/main/resources/i18n/zh-CN.json`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`
- `docs/review/phase4/opt/baselines/2026-04-24-phase4-inscription-shop-replacement-owner-baseline.json`

测试：

- `core/src/test/kotlin/com/ktome/core/inscription/InscriptionManagerTest.kt`
- `game/src/test/kotlin/com/ktome/game/ShopPurchaseFlowTest.kt`
- `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt`
- `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt`
- `tools/src/test/kotlin/com/ktome/tools/phase4/**`

### 3.2 非目标

1. 不新增铭文类别。
2. 不新增铭文总数。
3. 不新增永久铭文槽解锁。
4. 不把铭文变成背包物品。
5. 不提供旧铭文出售或返还金币。
6. 不引入数值型局外成长。

## 4. 资源要求

### 4.1 图片资源

不生成新图片资源。

执行要求：

1. `healing_light` 继续使用 `icon.skill.templar.holy_light`。
2. `phase_door` 与 `controlled_phase` 继续使用 `icon.skill.arcanist.blink`。
3. `iron_shield` 与 `diamond_shield` 继续使用 `icon.skill.templar.holy_shield`。
4. `purge` 与 `greater_purge` 继续使用 `icon.skill.templar.purify`。
5. 替换 UI 使用现有 `ModalCardModel`、`UiDesignTokens` 和文本状态，不新增图标。

### 4.2 音频资源

不生成新音频资源。

执行要求：

1. 安装成功使用已有 `audio.shop.purchase_success`。
2. 替换成功使用已有 `audio.item.equip.changed`。
3. 类别上限、金币不足、同铭文拒绝使用已有 `audio.shop.purchase_failed`。
4. 本 PR 不新增 audio plan、generation report、processing report。

## 5. 技术方案

### 5.1 起始铭文

`FoundationGameSession.ensurePlayerInscriptions` 必须按职业写入 2 个铭文：

| 职业 | 起始铭文 |
| --- | --- |
| `vanguard` | `healing_light`, `iron_shield` |
| `arcanist` | `healing_light`, `phase_door` |
| `rogue` | `healing_light`, `phase_door` |
| `templar` | `healing_light`, `purge` |
| `berserker` | `healing_light`, `iron_shield` |
| `spellblade` | `healing_light`, `phase_door` |

规则：

1. 新 run 只创建 2 个 `InscriptionSlot`。
2. 热键由 `INSCRIPTION_HOTKEY_START` 开始连续分配；当前常量值为 `5`。
3. 旧 save / replay 检测到 4 starter inscription schema 时 fail fast，不保留 active run。
4. 不自动补满 4 槽。

save / fixture 破坏性边界：

1. 新 run 的 `RunSummary.startingInscriptionCount` 固定记录为 `2`。
2. 旧 save / replay 不通过 canonicalize、补槽或删槽继续运行；检测到旧 starter count 或旧 inscription schema 时 fail fast。
3. fail-fast error 固定为 `INCOMPATIBLE_PHASE4_V4_INSCRIPTION_SCHEMA`，提示重新开局。
4. validation fixture、long-run fixture、golden fixture 必须按 2 starter 新 schema 全量刷新。
5. report 聚合 `starterInscriptionMaxCount <= 2` 时只统计新 schema run；旧 schema run 不进入分母。

### 5.2 `InscriptionManager` 替换 API

新增结果模型：

```kotlin
enum class InscriptionEquipFailure {
    FULL_REQUIRES_REPLACEMENT,
    CATEGORY_LIMIT,
    TARGET_SLOT_MISSING,
    SAME_INSCRIPTION,
}
```

新增函数：

```kotlin
sealed interface InscriptionEquipCheck {
    data object Allowed : InscriptionEquipCheck
    data class Rejected(val reason: InscriptionEquipFailure) : InscriptionEquipCheck
}

sealed interface InscriptionReplaceOutcome {
    data class Applied(
        val newLoadout: InscriptionLoadout,
        val newCooldownState: InscriptionCooldownState,
        val event: GameEvent,
    ) : InscriptionReplaceOutcome

    data class Rejected(val reason: InscriptionEquipFailure) : InscriptionReplaceOutcome
}

fun canEquip(
    loadout: InscriptionLoadout,
    equippedDefinitions: List<InscriptionDef>,
    candidate: InscriptionDef,
): InscriptionEquipCheck

fun canReplace(
    loadout: InscriptionLoadout,
    equippedDefinitions: List<InscriptionDef>,
    candidate: InscriptionDef,
    targetHotkey: Int,
): InscriptionEquipCheck

fun replace(
    loadout: InscriptionLoadout,
    cooldownState: InscriptionCooldownState,
    equippedDefinitions: List<InscriptionDef>,
    candidate: InscriptionDef,
    targetHotkey: Int,
): InscriptionReplaceOutcome
```

替换规则：

1. 目标热键必须存在。
2. 临时移除目标槽后检查类别上限，公式固定为 `postCount = currentCount - (targetCategory == candidateCategory ? 1 : 0) + 1`。
3. 替换后热键保持不变。
4. 同一铭文替换自己直接拒绝；同铭文定义固定为 `candidate.id == target.inscriptionId`。
5. 成功后返回 `InscriptionReplaceOutcome.Applied(newLoadout, newCooldownState, event)`，并写入 `INSCRIPTION_REPLACED` 事件。
6. `canEquip` 与 `canReplace` 的失败路径必须返回 `Rejected(reason)`，client 不得从 Boolean 反推失败原因。

类别上限反例：

1. `targetCategory == candidateCategory` 且 `currentCount == 2` 时，替换后 `2 -> 2`，允许。
2. `targetCategory != candidateCategory` 且 `candidateCategory.currentCount == 2` 时，替换后 `2 -> 3`，拒绝 `CATEGORY_LIMIT`。

升级关系：

1. `sameInscriptionEquivalenceKey` 固定为铭文本身 `id`。
2. `phase_door -> controlled_phase`、`iron_shield -> diamond_shield`、`purge -> greater_purge` 是 upgrade pair，不视为 same inscription。
3. `healing_light` 在当前数据集中没有 upgrade pair；文档和 loader 不得引用不存在的 `greater_healing_light`。
4. upgrade pair 替换在 UI 中显示 `upgrade` 标签，仍然走同一 `replace` 交易路径。

替换后冷却：

1. 旧铭文从 `InscriptionCooldownState.remainingByInscriptionId` 删除。
2. 新铭文写入初始冷却 `max(1, ceil(candidate.cooldown * 0.5))`。
3. `cooldown=0` 的新铭文初始冷却为 `1`。
4. 该规则防止玩家通过购买替换绕过当前铭文冷却墙。

### 5.3 商店购买流程

`PlayerCommand.BuyShopOffer` 必须扩展为可携带替换目标：

```kotlin
data class BuyShopOffer(
    val index: Int,
    val offerFingerprint: String,
    val replacementHotkey: Int? = null,
) : PlayerCommand
```

`ShopOfferSnapshot` 必须暴露由 game/session 生成的 opaque fingerprint：

```kotlin
@Serializable
data class ShopOfferSnapshot(
    val index: Int,
    val labelKey: String,
    val price: Int,
    val tags: List<String> = emptyList(),
    val tagLabelKeys: List<String> = emptyList(),
    val offerFingerprint: String,
)
```

边界规则：

1. `offerFingerprint` 由 game/session 根据当前 shop state 生成。
2. client 只把 `offerFingerprint` 当 opaque token 展示外传回，不复算、不解析、不读取 shopId、offerDef、stockVersion 或价格规则。
3. snapshot -> command -> replacement command 的 roundtrip 必须有测试覆盖。

商店失败模型新增：

```kotlin
sealed interface ShopPurchaseFailure {
    data object InsufficientGold : ShopPurchaseFailure
    data object StaleOffer : ShopPurchaseFailure
    data object RequiresReplacementTarget : ShopPurchaseFailure
    data class InscriptionEquipRejected(val reason: InscriptionEquipFailure) : ShopPurchaseFailure
}
```

流程：

1. 玩家选择 inscription offer。
2. 商店先做金币检查。
3. 槽位 `< 4` 时走安装。
4. 槽位 `== 4` 且 `replacementHotkey == null` 时返回 `RequiresReplacementTarget`，对应 stable diagnostic token 固定为 `shop.purchase.requires_replacement_target`。
5. client 打开替换界面。
6. 玩家选择 `INSCRIPTION_HOTKEY_START .. (INSCRIPTION_HOTKEY_START + MAX_INSCRIPTION_SLOTS - 1)` 的目标槽。
7. client 再次提交 purchase command，携带 `replacementHotkey` 与原始 `offerFingerprint`。
8. 替换成功后扣金币。
9. 任何失败路径不扣金币。
10. 服务端必须校验 `offerFingerprint == sha256(join("|", shopId, offerIndex, offerDef.id, offerPrice, offerKind, stockVersion))`；不一致返回 `StaleOffer`，不扣金币。
11. 热键区间必须满足 `INSCRIPTION_HOTKEY_START + MAX_INSCRIPTION_SLOTS - 1 <= 9`；当前 `5 + 4 - 1 = 8`，不会覆盖数字栏 `9` 的独立输入。

### 5.4 替换界面

替换界面必须展示：

1. 候选铭文名称、类别、冷却、作用标签、效果。
2. 四个现有铭文名称、类别、冷却、作用标签、效果。
3. 替换前类别数量。
4. 替换后类别数量，格式固定为 `Recovery: 2/2 -> 2/2 | Movement: 1/2 -> 2/2`。
5. 金币消耗。
6. 拒绝原因。

输入：

1. `INSCRIPTION_HOTKEY_START .. (INSCRIPTION_HOTKEY_START + MAX_INSCRIPTION_SLOTS - 1)` 选择替换目标；当前显示为 `5~8`。
2. `Enter` 确认替换。
3. `Esc` 放弃购买，返回商店。
4. 窄屏或本地化文本较长时，类别变化必须按类别分行展示；不得压缩成超出 modal 宽度的单行文本。

## 6. 测试与自证

### 6.1 必测行为

1. 新 run 开局铭文数固定为 `2`，热键从 `5` 开始连续分配。
2. 旧 save / replay 命中旧铭文 schema 时 fail fast，不执行兼容 canonicalize。
3. 槽位 `< 4` 时购买铭文直接安装，成功后扣金币。
4. 槽位 `== 4` 且未提供 `replacementHotkey` 时返回 `RequiresReplacementTarget`，不扣金币；UI / log 使用 `shop.purchase.requires_replacement_target` token。
5. 满槽购买铭文打开替换界面，玩家选择目标热键后再次提交 `BuyShopOffer(index, offerFingerprint, replacementHotkey)`。
6. 替换成功后保留热键、旧铭文销毁、不进背包、不返钱。
7. 类别上限、金币不足、同铭文替换失败时不扣金币，并显示拒绝原因。
8. `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount=0`，`inscriptionInstallOrReplaceRate` 进入 owner evidence。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr02
```

必须保留以下自证产物：

1. `build/reports/tests/` 中 `InscriptionManagerTest`、`ShopPurchaseFlowTest`、`FoundationGameSessionTest`、`InputHandlerTest` 的结果。
2. `tools/build/reports/` 中 `longRunLab` producer 产物。
3. `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` canonical report 产物，且 `reportPhase4Only` 与 `reportPhase4` 对 inscription 指标读取同一 producer artifact。
4. `build/reports/verification/` 中 `verifyChanged` 和 `maintainabilityLint` 产物。
5. `build/whitebox/phase4-v4-pr02/evidence/` 中人工白盒截图、日志、manual record。

### 6.3 人工白盒验证流程

本流程必须遵循 `docs/computer-use-whitebox-flow.md`。人工白盒必须使用 packaged app + Computer Use，不得用 IDE、Gradle run 或测试 harness 替代。

已有游戏 Validation Mode 改造要求：

1. 本 PR 必须接入 PR-00 的 `PHASE4_V4_FAST` section，scenario id 固定为 `phase4-v4-pr02`。
2. `prepare-primary-scene` 必须在现有游戏内 validation session 中进入 `rogue` 商人摊位附近，玩家开局只有 2 个铭文，商店至少展示 2 个可购买铭文，玩家拥有足够金币购买第 3 和第 4 个铭文。
3. `prepare-secondary-scene` 必须把玩家置于 4 铭文满槽、商店仍有 inscription offer 的状态，用于直接验证替换 modal、保留热键和取消不扣资源。
4. `show-evidence-summary` 必须列出本 PR 的 5 个截图名、金币变化检查、铭文槽位检查和 `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount=0`。
5. 铭文槽、金币、商店 offer、替换结果必须由 game 层 validation action materialize，不得由 client 伪造 UI 行。

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. scenario id：`phase4-v4-pr02`
4. preset：`LOOT_LAB`
5. seed：`2026042432`
6. runtime home：`build/whitebox/phase4-v4-pr02/runtime-home`
7. evidence 目录：`build/whitebox/phase4-v4-pr02/evidence`
8. manual record：`docs/review/phase4/v4-pr/manual-records/phase4-v4-pr02-inscription-shop-replacement.md`

流程：

1. 打包并生成快速白盒材料：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr02
```

2. 执行 `build/whitebox/phase4-v4-pr02/launch-packaged-app.sh` 启动 packaged app，Computer Use 目标 app 固定为 `com.ktome.client`。
3. 按 `build/whitebox/phase4-v4-pr02/cua-runbook.md` 打开 validation overlay，执行 `PHASE4_V4_FAST / prepare-primary-scene`。
4. 截图记录开局只有 `healing_light / phase_door` 两个铭文，热键为 `5 / 6`。
5. 进入商店，购买一个铭文到第 3 槽，确认热键为 `7`，金币减少，日志显示安装成功。
6. 购买第 4 个铭文，确认热键为 `8`，金币减少。
7. 执行 `PHASE4_V4_FAST / prepare-secondary-scene`，选择一个 inscription offer，确认界面进入替换流程而不是直接失败。
8. 在替换界面选择热键 `6`，确认新铭文占用热键 `6`，旧铭文消失，不出现在背包，金币只扣一次。
9. 触发同铭文替换或类别上限拒绝路径，确认显示拒绝原因且金币不变。
10. 执行 `PHASE4_V4_FAST / show-evidence-summary`，确认证据清单与本节文件名一致。
11. 保存证据：
    - `phase4-v4-pr02-start-inscriptions.png`
    - `phase4-v4-pr02-install-third-slot.png`
    - `phase4-v4-pr02-replacement-modal.png`
    - `phase4-v4-pr02-replace-keep-hotkey.png`
    - `phase4-v4-pr02-reject-no-gold-loss.png`
    - `phase4-v4-pr02-app.log`

通过标准：

1. 玩家能明确看到“购买新铭文 = 替换一个现有铭文”的取舍。
2. 满槽购买不再被系统静默拒绝。
3. 替换结果、热键、金币、拒绝原因全部可解释。
4. manual record 写明 packaged app 路径、runtime home、seed、输入序列、截图路径和结论。

### 6.4 统一验证框架关系

本 PR 新增 inscription owner metrics，必须进入 `Phase4MetricCatalog`、`Phase4OwnerMetricTargets`、`Phase4OwnerBaselineRegistry` 和 `tools/src/main/resources/phase4/aggregation-manifest.yaml`。人工白盒记录验证玩家是否能理解替换取舍，不能替代 core/game 状态机测试、`longRunLab` 和 `verifyChanged`。

### 6.5 玩家体验 Golden Path

1. 玩家从 `rogue` 新 run 开始，必须只看到 `healing_light / phase_door` 两个铭文，热键为 `5 / 6`，第 7、8 热键为空。
2. 玩家在商店买入第 3 个铭文时，必须看到安装成功、金币减少、热键为 `7`。
3. 玩家满 4 槽后再买铭文，必须进入替换 modal，而不是收到失败 toast。
4. 替换 modal 必须同时展示候选铭文、四个现有铭文、类别前后变化、金币消耗、upgrade 标签或拒绝原因。
5. 玩家选择替换后，旧铭文消失，新铭文保留目标热键，金币只扣一次，新铭文进入初始冷却。

## 7. Report 与验收指标

新增 blocking 指标：

| 指标 | 阈值 | metricKind | producer | ownerBaseline | failSemantics |
| --- | ---: | --- | --- | --- | --- |
| `starterInscriptionMaxCount` | `<= 2` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-inscription-shop-replacement-owner-baseline.json` | `fail owner gate` |
| `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount` | `0` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-inscription-shop-replacement-owner-baseline.json` | `fail owner gate` |
| `inscriptionInstallOrReplaceRate` | `>= 50% terminal runs` | `blockingOwner` | `longRunLab` | `docs/review/phase4/opt/baselines/2026-04-24-phase4-inscription-shop-replacement-owner-baseline.json` | `fail owner gate` |

新增 supporting 指标：

| 指标 | 用途 | metricKind | producer | ownerBaseline | failSemantics |
| --- | --- | --- | --- | --- | --- |
| `terminalInscriptionLoadoutDiversity` | 观察终局铭文组合集中度 | `supporting` | `longRunLab` | `N/A` | `display only` |
| `inscriptionCategoryCountDistribution` | 观察类别上限是否产生取舍 | `supporting` | `longRunLab` | `N/A` | `display only` |
| `shopInscriptionOfferConversionRate` | 观察商店铭文是否被购买 | `supporting` | `longRunLab` | `N/A` | `display only` |
| `inscriptionReplaceReasonDistribution` | 观察玩家牺牲的 answer 类型 | `supporting` | `longRunLab` | `N/A` | `display only` |

owner 接线要求：

1. 新 blocking 指标必须进入 `Phase4MetricCatalog`、`Phase4OwnerMetricTargets`、`Phase4OwnerBaselineRegistry` 和 `tools/src/main/resources/phase4/aggregation-manifest.yaml`。
2. `phase4Report` 与 `reportPhase4Only` 必须使用同一 producer artifact，不得从 Markdown 或 client UI 反推。
3. `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount` 只统计“满槽 inscription purchase 没有进入替换流程就被阻断”的失败；玩家进入替换 UI 后按 `Esc` 放弃计入 `inscriptionPurchaseCancelledAfterReplacementPrompt` supporting 字段，不计入 blocking。
4. 金币不足计入 `shopPurchaseDeniedInsufficientGoldCount` supporting 字段，不计入本 PR blocking。

## 8. 验证命令

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged
```

## 9. 完成定义

1. 新 run 开局铭文数为 `2`。
2. 满槽购买铭文进入替换界面。
3. 替换保持热键不变。
4. 替换成功后扣金币，失败不扣金币。
5. `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount=0`。
6. `reportPhase4Only` 与 `reportPhase4` 对 `starterInscriptionMaxCount`、`fullSlotInscriptionPurchaseBlockedWithoutReplacementCount`、`inscriptionInstallOrReplaceRate` 输出一致。
7. `BuyShopOffer` 保留字段名 `index`，新增 `offerFingerprint` 与 `replacementHotkey`。
8. `phase4Report` 输出铭文安装与替换指标，并进入 canonical owner evidence。
9. `verifyChanged` 覆盖 core/game/client/report 影响面。
10. 没有新增图片计划文件。
11. 没有新增音频计划文件。

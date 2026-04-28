# Phase4 v4 PR-02 铭文商店替换 — 深度审阅（资深 Roguelike 开发设计总监视角）

- 审阅日期：2026-04-28
- 审阅对象分支：`codex/phase4-v4-pr02-inscription-shop-replacement`
- 设计基线：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md`
- 审阅范围：本 PR 涉及 core / game / client / tools / build-logic / docs / 测试 / fixture / 白盒证据全链路
- 审阅角色：游戏设计总监 + 系统策划总监 + 玩法体验审查负责人

## 0. 总体结论（TL;DR）

**结论：基本一致；可上线但存在 5 处偏差需修复，其中 1 处属于体验/证据契约级别，必须在合并前修正；其余 4 处属于代码契约/防回归强度问题，建议在本 PR 内修复或显式记录例外。**

PR 已完整实现“开局 2 槽 + run 内构筑 + 满槽替换”这条玩法主轴：

- 开局铭文 2 槽（按职业从 `professions/index.yaml` 的 `startingInscriptions` 读取真源）— 落地。
- `InscriptionManager` 替换 API、类别上限二次校验、热键保留、同铭文拒绝、初始冷却 `max(1, ceil(cd*0.5))` — 落地。
- `BuyShopOffer(index, offerFingerprint, replacementHotkey?)` + 服务端 SHA-256 fingerprint 校验 — 落地。
- `ShopPurchaseFailure.RequiresReplacementTarget` + `shop.purchase.requires_replacement_target` 稳定 token — 落地。
- 旧 save fail-fast `INCOMPATIBLE_PHASE4_V4_INSCRIPTION_SCHEMA` — 落地。
- 4 项 blocking + 4 项 supporting 指标进入 `Phase4MetricCatalog` / `aggregation-manifest.yaml` / `Phase4OwnerBaselineRegistry` / 新 baseline JSON — 落地。
- 替换 modal 的展示与输入（5–8 选槽 / Enter 确认 / Esc 取消）— 落地，分类 delta 按行分行（满足窄屏要求）。
- Whitebox 场景 `phase4-v4-pr02` 注册到 validation registry、Phase4 V4 whitebox 场景目录、scenario YAML — 落地。

体验目标层面：玩家从“开局 4 槽满配 → 全程无构筑”切换为“先 2 后取舍”的核心玩法循环已经成立；满槽购买不再被静默拒绝，替换语义可解释、可观测。

## 1. 设计文档对齐核对（按章节）

| 设计章节 | 要求 | 实现位置 | 一致性 |
| --- | --- | --- | --- |
| §1 完成标准 1：开局铭文 = 2 | `startingInscriptions` 真源 + 2 槽 | `professions/index.yaml`；`FoundationGameSession.ensurePlayerInscriptions` (~10113) → `starterInscriptionIdsForCurrentProfession` (~10143) | ✅ |
| §1 完成标准 2/3：最大槽 4 / 第 3、4 槽通过 run 内获取 | 槽位常量与流程 | `core/inscription/InscriptionSlot.kt:3-5`；`InscriptionManager.canEquip:48` | ✅ |
| §1 完成标准 4/5：满槽进入替换；保留热键、销毁旧铭文、不进背包、不返钱 | 替换 API + 商店流程 | `InscriptionManager.replace` (~99) + `FoundationGameSession.buyShopOffer` (~7926) | ✅ |
| §1 完成标准 6：每类 ≤ 2 | `MAX_INSCRIPTION_PER_CATEGORY=2` + canReplace 二次公式 | `InscriptionManager.canReplace:87-92` | ✅ |
| §1 完成标准 7：`fullSlotInscriptionPurchaseBlockedWithoutReplacementCount=0` | 结构性 0（never incremented，由流程保证） | `FoundationGameSession.kt:729` 仅恢复值，无增量点 | ⚠️ 见 §2.4 |
| §1 完成标准 8：RunSummary / longRunLab / phase4Report 记录安装与替换 | `InscriptionRunTelemetrySnapshot` + 新指标 | `core/save/SaveSnapshot.kt:193-226`；`game/harness/HeadlessRunHarness.kt:339-349`；`tools/.../Phase4MetricCatalog.kt:299-361` | ✅ |
| §3.1 范围：保留 PR-01 talent sidebar 边界 | `1~4` 仍属于 active talent；铭文 modal 独立 | `InputHandler.pollShopCommand` 分流 vs talent path | ✅ |
| §4 资源：不新增图标/音频 | 仅复用 | `AudioRouter.onCommandResolved:304-316` 复用 `audio.shop.purchase_success / audio.item.equip.changed / audio.shop.purchase_failed` | ✅ |
| §5.1 起始铭文映射表 | 6 职业按表写入 | `professions/index.yaml` 全部命中（vanguard/arcanist/rogue/templar/berserker/spellblade）；shadowblade、warden 留空 | ✅ |
| §5.1 旧 save fail fast token 固定为 `INCOMPATIBLE_PHASE4_V4_INSCRIPTION_SCHEMA` | 不 canonicalize；不补槽 | `core/save/SaveSnapshot.kt:97-99`；`core/save/SaveCodec.kt:52,88-97` | ✅ |
| §5.2 失败枚举 / `InscriptionEquipCheck` / `InscriptionReplaceOutcome` | 4 种失败、Allowed/Rejected、Applied/Rejected | `InscriptionManager.kt:7-32` | ✅ |
| §5.2 替换函数签名 | 文档：`replace(loadout, cooldownState, equippedDefinitions, candidate, targetHotkey)` | 实现：`replace(InscriptionReplaceRequest)` 包装 | ⚠️ 见 §2.1 |
| §5.2 类别上限公式 `postCount = current - (sameCat?1:0) + 1` | 二次比较 ≤ 2 | `InscriptionManager.canReplace:87-92` | ✅ |
| §5.2 同铭文拒绝（按 id） | `candidate.id == target.inscriptionId` | `InscriptionManager.canReplace:80` | ✅ |
| §5.2 升级关系：phase_door→controlled_phase / iron_shield→diamond_shield / purge→greater_purge；不引用不存在的 `greater_healing_light` | upgrade 标签来源 | `FoundationGameSession.inscriptionUpgradeSource:5190-5196`；UI 标签 `RewardPresentationText.kt:92-93` | ✅（本 PR 实现侧未引用 `greater_healing_light`，旧 r1 director-review 文档遗留串扰仅在历史文档中） |
| §5.2 替换冷却：`max(1, ceil(cd*0.5))`；旧铭文从 cooldown map 删除 | 公式 + 删除 | `InscriptionManager.replace:116-122,163-164` | ✅ |
| §5.3 `BuyShopOffer(index, offerFingerprint, replacementHotkey?)` | 字段与默认值 | `game/GameView.kt:35-39`；client 注入 `client/input/InputHandler.kt:495-499` | ✅ |
| §5.3 `ShopOfferSnapshot.offerFingerprint: String`（无默认） | 必填 | `core/snapshot/RenderSnapshot.kt:507-514` 给了 `= ""` 默认 | ⚠️ 见 §2.2 |
| §5.3 fingerprint = `sha256(join("\|", shopId, offerIndex, offerDef.id, price, kind, stockVersion))` | 服务端校验 | `FoundationGameSession.shopOfferFingerprint:5150-5173` | ✅ |
| §5.3 失败模型 `ShopPurchaseFailure` 4 类 | 完整 | `core/economy/ShopModels.kt:102-112` | ✅ |
| §5.3 满槽且无 hotkey → `RequiresReplacementTarget` + 不扣碎晶 + 进入 modal | pendingInscriptionReplacementPurchase 流程 | `FoundationGameSession.buyShopOffer:7880-7897` | ✅ |
| §5.3 stale offer 校验：fingerprint 不一致 → 不扣 | 双校验（pre-flight + replace 提交） | `FoundationGameSession.kt:7826-7830,7915-7924` | ✅ |
| §5.3 热键区间 5..8 ≤ 9（不撞数字 9） | 常量与 save 验证 | `InscriptionSlot.kt:5,12`；`SaveSnapshot.kt:840` | ✅ |
| §5.4 替换 modal 展示候选/4 槽/类别 delta/价格/拒绝原因 | snapshot + UI 文本 | `InscriptionReplacementPromptSnapshot:517-525`；`i18n/*.json: ui.inscription.replace.*` | ✅ |
| §5.4 类别 delta 分行（窄屏） | 每类一行 `Recovery: 2/2 -> 2/2` | `i18n` 模板：`{category}: {before}/{limit} -> {after}/{limit}` 渲染逐行 | ✅（与文档"按类别分行"语义一致；文档示例的 ` \| ` 仅作语义说明） |
| §5.4 输入：5-8 / Enter / Esc | 完整 | `client/input/InputHandler.pollShopCommand:466-502`（含 Esc/Backspace 取消、↑↓/WS/AD/数字 选槽、Enter/Space/E 确认） | ✅ |
| §6.1 必测行为 1–8 | 测试覆盖 | `InscriptionSlotTest`、`FoundationGameSessionTest`、`SmokeBotTest`、`LongRunLabFullTest`、`InputHandlerTest` 均已修改 | ✅（行为枚举与拒绝原因覆盖到位） |
| §6.3 白盒 5 截图 + manual record | 文件路径与 SHA | 见 §2.3 | ⚠️ 见 §2.3 |
| §6.4 owner metrics 接线 | 4 blocking 进 manifest+catalog+registry+baseline | `aggregation-manifest.yaml:33-36`；`Phase4MetricCatalog.kt:298-361`；`Phase4OwnerBaselineRegistry.kt:22-23,41,64`；新 baseline JSON 4 项 expectedMetricRanges | ✅ |
| §7 owner 接线要求 3：`inscriptionPurchaseCancelledAfterReplacementPrompt` 作为 supporting 字段 | 计入 `Phase4AggregationInputRunner` | `Phase4AggregationInputRunner.kt:1333` | ✅ |
| §7 owner 接线要求 4：`shopPurchaseDeniedInsufficientGoldCount` 作为 supporting 字段 | 同上 | `Phase4AggregationInputRunner.kt:1334` | ✅ |
| §9 完成定义 1–11 | 字段名 / 报告一致性 / 没新增图音 plan | 全部满足；`BuyShopOffer.index` 字段名保留 | ✅ |

## 2. 关键偏差与修复建议

### 2.1 [中等] `InscriptionManager.replace` 函数签名与设计契约不符

- **现象**：设计文档 §5.2 给出的契约是
  ```kotlin
  fun replace(
      loadout: InscriptionLoadout,
      cooldownState: InscriptionCooldownState,
      equippedDefinitions: List<InscriptionDef>,
      candidate: InscriptionDef,
      targetHotkey: Int,
  ): InscriptionReplaceOutcome
  ```
  实际实现采用 `fun replace(request: InscriptionReplaceRequest): InscriptionReplaceOutcome`（位置：`core/inscription/InscriptionManager.kt:99`）。
- **影响**：
  - 行为正确，调用点 `FoundationGameSession.buyShopOffer:7926-7935` 也已用 `InscriptionReplaceRequest` 拼装，无功能性偏差。
  - 但文档级 API 契约出现偏差，未来重读文档的工程师/CI 契约 lint 在做"按设计文档断言签名"时会触发 false negative；契约一致性是 v4 PR 系列里反复强调的"single source of truth"。
- **修复建议（任选其一）**：
  1. 调整实现为 5 参数函数，保留 `InscriptionReplaceRequest` 仅作内部 DTO（推荐）；或
  2. 在设计文档中显式追加"为可读性引入 `InscriptionReplaceRequest` wrapper，参数名/语义保持不变"的脚注，并把 `replace(request)` 写进契约。
- **优先级**：合并前可选；若选方案 2，至少要在文档侧落地，避免下一轮 review 仍踩同一钉子。

### 2.2 [中等] `ShopOfferSnapshot.offerFingerprint` 含默认值，弱化序列化契约

- **现象**：`core/snapshot/RenderSnapshot.kt:507-514`
  ```kotlin
  data class ShopOfferSnapshot(
      val index: Int,
      val labelKey: String,
      val price: Int,
      val offerFingerprint: String = "",   // ← 设计文档要求是必填
      ...
  )
  ```
  设计文档 §5.3 明确写出 `val offerFingerprint: String,` 无默认值。
- **影响**：
  - 运行时无问题：服务端 `shopOfferFingerprint` 一定是非空 SHA-256；client 透传后会在 `expectedFingerprint != offerFingerprint` 比较里被识别为 `StaleOffer`。
  - 风险点：旧 fixture / replay JSON 中如果缺失该字段，会被反序列化为 `""`，并进而在没有 PR-02 改动的旧测试样本上"看似通过"（fingerprint 校验仍会失败但不会触发 schema 错误）。这违背了"snapshot ↔ command roundtrip 必须有测试覆盖"的边界规则（§5.3 第 3 条）。
  - 同样的默认值出现在 `harness/ScenarioModels.kt:470` (`ObservedShopOffer.offerFingerprint = ""`)：harness 层兜底默认值，会让 SmokeBot fixture 在 fingerprint 缺失场景下静默退化。
- **修复建议**：
  1. 移除 `ShopOfferSnapshot.offerFingerprint` 与 `ObservedShopOffer.offerFingerprint` 的默认值，强制 producer 在生成快照时显式写入；
  2. 在 `core/save/SaveCodec.kt` 同步增加 unknown-keys / missing-fingerprint 拒绝（已经 `ignoreUnknownKeys = false`，但当前默认值绕过了 missing-required 校验）；
  3. 给 `Phase4ContractLintTest` 或 `ContractLintTest` 增加一条断言：所有 inscription 类型的 `ShopOfferSnapshot` fingerprint 非空，否则 fail。
- **优先级**：建议合并前修复，否则一旦后续 schema 变更导致 producer 漏写 fingerprint，将无法被构建期捕获。

### 2.3 [高] 人工白盒证据文件名与设计文档/scenario registry 不一致

- **现象**：
  - 设计文档 §6.3 第 11 步明确要求第 5 张截图为 `phase4-v4-pr02-reject-no-shard-loss.png`（"shard"，与项目内"碎晶"通用命名一致）。
  - `game/validation/ValidationScenarioRegistry.kt:150,183` 也注册的是 `phase4-v4-pr02-reject-no-shard-loss.png`。
  - 但实际白盒证据 `docs/review/phase4/v4-pr/manual-records/phase4-v4-pr02-inscription-shop-replacement.md:31-33` 落盘的是
    ```
    build/whitebox/phase4-v4-pr02/evidence/phase4-v4-pr02-reject-no-gold-loss.png
    ```
    并附 SHA-256；manual record 自身 §Notes 也显式承认 "runbook names the final screenshot `phase4-v4-pr02-reject-no-gold-loss.png`; this is the same no-shard-loss check"。
- **影响**：
  - validation registry 的 `requiredEvidenceFiles` 校验会因为找不到 `*-reject-no-shard-loss.png` 而 fail（如果该断言在 verifyChanged / preparePhase4V4Whitebox 链路上跑）。
  - 文档与证据出现"碎晶 / shard"vs"金币 / gold"的术语裂痕，破坏 owner evidence 的可解释性，也违反 §6.3 通过标准 4（manual record 必须写明截图路径与结论）。
- **修复建议（按推荐顺序）**：
  1. 重新生成或重命名截图为 `phase4-v4-pr02-reject-no-shard-loss.png`，更新 manual record 的文件名、SHA-256、Notes 段（移除"runbook names …"那段解释）；
  2. 同步检查 cua-runbook（`build/whitebox/phase4-v4-pr02/cua-runbook.md`）是否也用了 `gold` 命名，统一改为 `shard`；
  3. 在 `tools/whitebox/Phase4V4WhiteboxScenarioCliTest` 之类断言里增加"manual record 中证据路径 ⊆ scenario registry requiredEvidenceFiles"的硬断言，防止再次出现命名漂移。
- **优先级**：必须合并前修复。这是 owner evidence 契约级问题，不是细枝末节。

### 2.4 [中等] `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount` 是"结构性 0"指标，缺乏回归报警能力

- **现象**：检索 `FoundationGameSession.kt`，该计数器仅在初始化时从 `restoredInscriptionTelemetry.fullSlotInscriptionPurchaseBlockedWithoutReplacementCount` 读取（行 729-730 与 9972 写回），**没有任何 `+= 1` 的写入点**。
  - 设计意图：满槽 inscription 购买如果"没有进入替换流程就被阻断"则计数；当前实现因为流程总是会进入替换或 install，所以计数器永远 0。
- **影响**：
  - blocking 阈值 `<= 0` 会被永远满足，gate 形同虚设。如果未来有人误改 `buyShopOffer` 逻辑，让满槽直接 `return CommandResolution.rejected()` 而不进入 pending replacement flow，计数器仍然是 0，**owner gate 不会失败**，回归只能靠人眼或 SmokeBot 行为差异捕获。
- **修复建议（任选其一）**：
  1. 在 `buyShopOffer` 的所有"满槽 inscription 购买被拒绝"路径上增加显式 `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount += 1`，仅在新流程开启 pending modal 时不计数。这样语义就是"硬拒绝一次 +1"，回归可被发现。具体落点：
     - `if (replacementHotkey == null) { ... if (reason == FULL_REQUIRES_REPLACEMENT) { ... pendingInscriptionReplacementPurchase = ... return accepted=true }`：此路径**不计数**；
     - 相同 `if (replacementHotkey == null)` 块的 else 分支（CATEGORY_LIMIT 等其他失败）：当槽位 == 4 且 reason 是 CATEGORY_LIMIT 时**计数**；
     - 当前没有"满槽且没有 modal"的硬拒绝代码路径，正是这个事实让计数器恒 0；
  2. 增加一条 `core/game` 测试：构造一个 patch 版 `buyShopOffer`（或注入旁路）模拟旧路径，断言计数器会 +1。
  3. 或者，将该指标重命名为 `fullSlotInscriptionPurchaseRoutedToReplacementCount`，定义为"满槽购买进入 modal 的次数"，把阈值改为 `>= 1`，让它真正反映新行为而不是回归。这是体验更直接的口径，但需要同步改 baseline JSON、metric formula 与 Phase4OwnerMetricTargets 文案。
- **优先级**：合并前可选，但**至少要补一条 §6.1 第 4 条"未提供 replacementHotkey 时返回 RequiresReplacementTarget 不扣碎晶 / UI 用 stable token"**的 SmokeBot/`InputHandlerTest` 黑盒断言，确认行为不会回归到旧硬拒绝路径。如果不修，请在 PR 描述里显式记录这个"结构性 0"事实，以免下一次 review 把它当作遗漏。

### 2.5 [低] `replace` 替换路径在槽 < 4 时给出的错误原因不精确

- **现象**：`FoundationGameSession.buyShopOffer:7904-7913`
  ```kotlin
  if (loadout.slots.size < MAX_INSCRIPTION_SLOTS) {
      val reason = (equipCheck as? InscriptionEquipCheck.Rejected)?.reason
          ?: InscriptionEquipFailure.TARGET_SLOT_MISSING
      addMessage(shopPurchaseFailureMessageKey(ShopPurchaseFailure.InscriptionEquipRejected(reason)), ...)
      return CommandResolution.rejected()
  }
  ```
  当客户端传了 `replacementHotkey != null` 但槽位 < 4 时，会用 `equipCheck.reason`（可能是 `CATEGORY_LIMIT`），而玩家收到的语义其实是"槽位还没满，不该走替换"。
- **影响**：
  - 仅在客户端构造异常 command（比如手动注入或测试 fixture）时触发；正常 UI 不会进入这个分支。
  - 但拒绝原因与玩家心智不匹配，未来如果开放更多 inscription 来源（例如 PR-03 mod 物品强制替换），这条路径的语义会更模糊。
- **修复建议**：
  - 当 `loadout.slots.size < MAX_INSCRIPTION_SLOTS && replacementHotkey != null` 时，固定返回 `TARGET_SLOT_MISSING` 或新增 `REPLACEMENT_NOT_REQUIRED` 失败原因；
  - 或者直接忽略 `replacementHotkey` 改走安装路径并写一条 supporting log，避免无声拒绝。
- **优先级**：建议本 PR 做一次小幅澄清，但不阻塞合并。

## 3. 玩家体验视角的二次审查

| 体验维度 | 评估 | 备注 |
| --- | --- | --- |
| 起手取舍 | ✅ 强 | 2 槽 + 6 个起始铭文表的差异（rogue/arcanist/spellblade 都拿 phase_door；vanguard/berserker 拿 iron_shield；templar 拿 purge；healing_light 共选）让"恢复 + 第二个 answer"的开局心智清晰，不会陷入"4 个都差不多"的麻木开局。 |
| 中段获取节奏 | ✅ 强 | 商店 offer 已经是真源（`shops/index.yaml`），Boss/隐藏奖励路径继承现有 reward observe；`shopInscriptionOfferConversionRate` 监控点保证可被后续 balance round 看见。 |
| 满槽决策可解释性 | ✅ 强 | modal 同时展示候选 + 4 槽 + 类别 delta + 价格 + upgrade 标签，符合"购买 = 替换"的体验目标。i18n 文本 `ui.inscription.replace.candidate_detail` / `slot_detail` / `category_delta` / `upgrade_tag` 完整。 |
| 拒绝路径的"心智可读" | ⚠️ 中 | 同铭文 / 类别上限 / 缺槽三类都有独立 message key，但拒绝时玩家可以选其他热键继续替换；当前 InputHandler 在 Rejected 后保留 modal（`pendingInscriptionReplacementPurchase` 不清空），这是对的；但需要 SmokeBot 用例证明"反复尝试 → 选合法槽 → 成功"的链路没有 Esc 才能退出的死锁。 |
| 取消恢复 | ✅ 强 | Esc → `CancelInscriptionReplacementPurchase` → `inscriptionPurchaseCancelledAfterReplacementPrompt += 1`；不扣碎晶；玩家可重选其他 offer。 |
| 升级 vs 替换的语义裂缝 | ✅ 已闭合 | 文档第 5.2 节明确 upgrade pair 仍走同一 `replace` 路径，`upgradeFromInscriptionId` 仅作为 UI 标签显示；玩家"升级 = 替换"的心智没有第二条隐藏入口。 |
| 满槽硬卡死的回归风险 | ⚠️ 中 | 见 §2.4。建议至少补一个"满槽 inscription offer + 玩家不带 hotkey 提交 → 必入 modal"的端到端断言。 |

## 4. 报告 / 验证链路完整性

- `Phase4MetricCatalog` 4 blocking + 4 supporting：✅ 全部就位（`Phase4MetricCatalog.kt:298-361`），`outputSection = inscription-shop-replacement` 一致。
- `Phase4OwnerMetricTargets`：仅 `inscriptionInstallOrReplaceRate` 列入 percent 渲染，其他 3 个 blocking（`starterInscriptionMaxCount` / `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount` / `inscriptionReplacementProbeSuccessCount`）落入 `formatNumber` 默认分支，呈现为整数。这是合理的——这三项本身就是计数/最大值。
- `aggregation-manifest.yaml`：✅ `longRunLab` 4 个 metricIds 与 catalog/baseline 三方一致。
- `Phase4OwnerBaselineRegistry`：✅ 新增 `INSCRIPTION_SHOP_REPLACEMENT_BASELINE_RELATIVE_PATH` + `inscriptionShopReplacementBaselinePath()`；`longRunLab` baseline list 第 4 项就是新 baseline。
- `Phase4AggregationInputRunner`：✅ supporting 指标 `inscriptionPurchaseCancelledAfterReplacementPrompt` / `shopPurchaseDeniedInsufficientGoldCount` 进入聚合输入；inscription evidence metric IDs 集合（line 49-57）正确去重。
- `reportPhase4Only` ↔ `reportPhase4` 同 producer artifact：通过 `aggregation-manifest.yaml` 单一 `longRunLab` artifactRelativePath 实现；与设计 §6.2 第 3 条一致。
- baseline JSON：✅ schemaVersion=1，4 项 expectedMetricRanges 与文档 §7 表一致；包含 `metricDefinitionVersion = phase4-v4-pr02-inscription-shop-replacement-v1`，便于未来 contract 演进追踪。
- fail-fast schema 版本：`SaveSnapshot.CURRENT_INSCRIPTION_SCHEMA_VERSION = 2`，搭配 `INCOMPATIBLE_PHASE4_V4_INSCRIPTION_SCHEMA` token 双层校验（codec + snapshot 自校验）。

## 5. 风险评估

| 风险 | 概率 | 影响 | 当前缓解 | 建议补充 |
| --- | ---: | --- | --- | --- |
| 旧 save / 旧 fixture 未刷新导致 fail-fast 误伤 | 低 | 中 | `inscriptionSchemaVersion=2` + 双层 check + 测试 `SaveCodecTest` 改动 | 检查 `LongRunKernelCache` 旧缓存（`tools/build/verification-cache/kernels/longrun/...`）是否需要清理；建议在 PR 描述中提示 reviewer 拉取后跑一次 `./gradlew clean longRunLab` |
| `fullSlotInscriptionPurchaseBlockedWithoutReplacementCount` 结构性 0，回归不报警 | 中 | 中 | 当前依赖 SmokeBot 行为差异 | 见 §2.4 修复建议 |
| `ShopOfferSnapshot.offerFingerprint` 默认值绕过 schema 必填 | 低 | 中 | 服务端二次 SHA 校验 | 见 §2.2 修复建议 |
| 白盒截图命名漂移使 owner evidence 链路断 | 高 | 中 | 无 | 见 §2.3 修复建议（必须修）|
| `replace(request)` 函数签名与文档契约不一致 | 中 | 低 | 行为正确 | 见 §2.1 修复建议 |
| modal 拒绝后 pending 未清空，玩家在多重连续拒绝时是否能持续看到正确 hint | 中 | 低 | InputHandler 当前在 Rejected 后保留 prompt，但 `inscriptionReplacementHotkeySelection` 会被 `takeIf { hotkey in hotkeys }` 重置 | 增加 InputHandler 测试：拒绝两次后再选合法槽 → 成功 |

## 6. 验收建议（合并 gate）

合并前必须通过：

1. **必须**：修复 §2.3 截图命名（`reject-no-gold-loss.png` → `reject-no-shard-loss.png`，含 manual record 内 SHA、cua-runbook、Notes 段）。
2. **必须**：跑通 `./gradlew localeLint contractLint assetLint audioLint :core:test :game:test goldenScreenshot clientSmoke longRunLab reportPhase4Only reportPhase4 maintainabilityLint verifyChanged`，并附产物清单。
3. **必须**：跑通 `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr02`，产物落盘到 `build/whitebox/phase4-v4-pr02/evidence/`，5 张截图 + 1 个 log。
4. **强烈建议**：修复 §2.2（fingerprint 默认值）+ §2.4（计数器结构性 0），二选一也可，但需在 PR 描述里显式声明遗留风险。
5. **建议**：澄清 §2.1（API 签名）、§2.5（拒绝原因精度）。

合并后建议：

- 1 个 sprint 内追踪 owner gate `inscriptionInstallOrReplaceRate >= 50%` 真实分布；如果生产 longRun 跑出 60%+ 则可考虑下调 supporting `shopInscriptionOfferConversionRate` 的可视化区间，让"高商店密度"路径不再被噪声覆盖。
- 收集 `inscriptionReplaceReasonDistribution`，在下一轮 balance round 决定是否有必要扩 `MAX_INSCRIPTION_PER_CATEGORY` 上限（当前 = 2 是有意为之的张力源）。

## 7. 可执行修复清单（建议工程顺序）

1. **白盒证据命名修复（§2.3）**
   - 重命名/重生 `reject-no-gold-loss.png` 为 `reject-no-shard-loss.png`；
   - 同步 manual record SHA、Notes、target_pid 段；
   - 检查 `build/whitebox/phase4-v4-pr02/cua-runbook.md`（如果是生成产物则需要重生）；
   - 增加 `Phase4V4WhiteboxScenarioCliTest`/manualRecord 一致性 test。
2. **fingerprint 默认值收紧（§2.2）**
   - `core/snapshot/RenderSnapshot.kt:511` 移除 `= ""`；
   - `game/harness/ScenarioModels.kt:470` 同步；
   - 跑 `:core:test :game:test`，修复因此暴露的 fixture 写入点；
   - 在 `ContractLintTest` 增加 `inscription offer fingerprint not blank` 断言。
3. **结构性 0 计数器加固（§2.4）**
   - 二选一：要么补 `+= 1` 写入点 + 防回归测试；要么改语义 + 改 baseline + 改 metric formula 文案。
4. **API 签名澄清（§2.1）**
   - 推荐：`InscriptionManager.replace(loadout, cooldowns, equipped, candidate, targetHotkey)` 5 参数版本；
   - 把 `InscriptionReplaceRequest` 降级为内部 helper 或删除；
   - 同步 `FoundationGameSession.buyShopOffer:7926-7935`。
5. **拒绝原因精度（§2.5）**
   - `buyShopOffer` 在 `slots.size < 4 && replacementHotkey != null` 分支返回固定 `TARGET_SLOT_MISSING`，避免 CATEGORY_LIMIT 误报。

## 8. 仍然成立的设计闪光点

- `ensurePlayerInscriptions` 把"职业 → 起始铭文"完全下沉到 YAML 真源，session 与 validation scene 不再各自维护副本，与 §5.1 的"single source of truth"完全对齐。
- `replace` 冷却公式 `max(1, ceil(cd*0.5))` 直接堵掉了"靠购买替换刷冷却"的 BM 路线，是体验闭环；测试 `InscriptionSlotTest:108` 显式断言 `4 == ceil(7*0.5)`，回归保护到位。
- `BuyShopOffer` 双校验（前置 fingerprint + 提交时 pending fingerprint 二次比对）让"上架变更后旧快照不会扣碎晶"成为契约级约束，比单一 stockVersion 更稳健。
- `phase_door → controlled_phase` 等 upgrade pair 走同一 `replace` 路径，UI 通过 `upgradeFromInscriptionId` 仅作显示标签，没有引入第二种"升级语义"，避免了"升级 vs 替换"双系统漂移。

---

> 本审阅文档可作为 PR 合并前的 walkthrough 清单。任何 §2 中的偏差如不修复，请在 PR 描述里以"已知遗留 / 后续工单"显式记录，让下游 owner gate 与下一轮 r7 director-review 不会再次踩同一钉子。

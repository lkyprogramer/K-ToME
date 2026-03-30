> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md`
> `docs/phase3/2026-03-13-phase3-pr-02-status-lifecycle.md`
> `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`
> `docs/review/phase3/2026-03-26-phase3-pr-10-player-facing-information-cleanup.md`

# Phase 3 V2 - PR-12 Combat Feedback Snapshot

**阶段**: `Phase 3 / v2 follow-up`  
**优先级**: `P1`  
**前置条件**: `PR-01 / PR-02` 已冻结战斗和状态主链，`PR-10` 已完成玩家信息清理，允许继续补 player-facing combat readability  
**对应问题**: 当前普通战斗和大部分 talent 互动仍然主要通过 `logEvents` 文本表现。Phase 3 后端已有完整 `CombatResolutionTrace` 与状态生命周期，但 `RenderSnapshot` 没有正式的战斗反馈数据层，导致“机制存在但玩家感知不到”。

**Lane-parallel 拆分**：

- **W12a (Core Contract Lane)**: `RenderSnapshot` combat feedback typed contract
- **W12b (Game Lane)**: 从伤害 / 治疗 / 命中失败 / 状态变化生成 feedback event
- **W12c (Client Lane)**: Tile 正式路径渲染 + ASCII fallback 最小消费
- **W12d (QA Lane)**: golden / client smoke / combat trace / boss harness 回归

---

## 1. 阶段目标

给战斗结果补上正式的 snapshot 数据层，让 Phase 3 的伤害、暴击、状态施加和净化结果能被客户端直接消费，而不是继续“只存在于文本日志里”。

完成标准：

1. `RenderSnapshot` 新增正式 `combatFeedbackEvents` 字段。
2. 至少支持以下反馈类型：
   - `DAMAGE`
   - `HEAL`
   - `MISS`
   - `STATUS_APPLIED`
   - `STATUS_REMOVED`
3. `DAMAGE` 反馈可表达是否暴击，不需要再单独发一条“crit”文案才能感知到。
4. Tile 路径能渲染最小可感知反馈；ASCII 路径至少能消费 typed feedback，而不是完全忽略。
5. `logEvents` 继续保留，承担历史文本职责，不被本 PR 替代。

## 2. 当前问题

1. [RenderSnapshot.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt) 当前只有 `logEvents`，没有战斗反馈事件列表。
2. [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt) 的伤害、暴击和状态变化主要通过日志 token 输出。
3. 玩家能看到 Boss telegraph，但普通战斗几乎没有“这下打得很重 / 这下净化成功 / 这次没命中”的即时视觉层。

### 2.1 本 PR 必须冻结的口径

1. combat feedback 是 `RenderSnapshot` 的**新增字段**，不是第二套战斗真源。
2. feedback 数据必须保持语义化，不保存：
   - localized string
   - raw color/path/glyph
   - animation 曲线
3. 反馈类型第一版固定为：
   - `DAMAGE`
   - `HEAL`
   - `MISS`
   - `STATUS_APPLIED`
   - `STATUS_REMOVED`
4. `critical` 作为 `DAMAGE` 的布尔属性表达，不额外创建第二种 damage 类型。
5. feedback 队列必须有上限，防止 snapshot 在 AoE / multi-hit 战斗里无限膨胀。
6. `logEvents` 继续存在；本 PR 不移除任何已有日志路径。
7. 本 PR 不引入完整粒子系统或时间轴动画，只交付正式数据层和最小表现层。

## 3. 范围与非目标

### 3.1 范围

1. `RenderSnapshot` / `FoundationGameSession` / client renderer 的 typed feedback 主链。
2. 伤害、治疗、未命中、状态施加/移除五类反馈。
3. Tile 正式路径的最小视觉呈现。
4. ASCII fallback 的最小文本消费。
5. golden / smoke / harness 回归。

### 3.2 非目标

1. 不做完整粒子系统。
2. 不做专门的战斗摄像机摇晃。
3. 不在本 PR 新增音效资源包；只允许复用现有 hit / crit / status cue。
4. 不把 inventory / reward / shop 操作混进 combat feedback。

## 4. 技术方案

### 4.1 [W12a] Snapshot Contract

建议文件：

```text
core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt
core/src/test/kotlin/com/ktome/core/snapshot/RenderSnapshotSerializationTest.kt
```

建议 contract：

```kotlin
@Serializable
enum class CombatFeedbackTypeSnapshot {
    DAMAGE,
    HEAL,
    MISS,
    STATUS_APPLIED,
    STATUS_REMOVED,
}

@Serializable
data class CombatFeedbackSnapshot(
    val targetEntityId: Int?,
    val sourceEntityId: Int? = null,
    val x: Int,
    val y: Int,
    val type: CombatFeedbackTypeSnapshot,
    val amount: Int? = null,
    val damageTypeId: String? = null,
    val statusNameKey: String? = null,
    val critical: Boolean = false,
)
```

冻结口径：

1. `x / y` 作为渲染锚点，来自当前回合结算后的实体位置。
2. `statusNameKey` 只在状态类反馈中出现。
3. 队列上限第一版固定 `12` 条，多余事件按“保留最后事件”截断。
4. 该字段不进入 save/profile；它是当前渲染 revision 的瞬时观察值。

### 4.2 [W12b] Game Emission

建议文件：

```text
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt
```

建议生成来源：

1. `DamageDealtEvent` -> `DAMAGE`
2. 治疗型结算 -> `HEAL`
3. 命中失败 / 无效攻击 -> `MISS`
4. 状态施加成功 -> `STATUS_APPLIED`
5. 状态净化 / 过期移除 -> `STATUS_REMOVED`

冻结口径：

1. feedback 只能从现有正式结算路径派生，不允许 client 端自行推断伤害数字。
2. 事件写入必须跟随 `invalidateRenderSnapshot()` 生命周期，避免重复消费旧反馈。
3. 同一回合的多段 child trace 允许产生多条反馈，但必须尊重 `12` 条上限。

### 4.3 [W12c] Client Presentation

建议文件：

```text
client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt
client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt
client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt
client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt
```

冻结口径：

1. Tile 路径作为正式玩家路径，必须提供最小可感知反馈：
   - `DAMAGE/HEAL` 数字
   - `MISS`
   - 状态名称短提示
2. `critical` 通过同一反馈渲染强调，不再额外依赖日志文本。
3. ASCII fallback 不要求做浮动动画，但至少要在侧栏或最近反馈区显示 typed feedback 内容。
4. client 只消费 semantic snapshot，不新增本地规则推断。

### 4.4 [W12d] QA / Regression

建议文件：

```text
game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt
client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt
client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt
```

冻结口径：

1. `combatTraceGolden` 继续验证公式正确性。
2. 本 PR 新增的是“感知层 contract”，因此必须补 `goldenScreenshot`。
3. boss harness 需要覆盖 telegraph 与 combat feedback 同时存在的场景。

### 4.5 资源复用冻结

冻结口径：

1. 本 PR 默认**不新增任何 raw image / raw audio**。`combatFeedbackEvents` 的玩家可见表达优先复用现有字体、现有 HUD layer、现有 overlay render path 和已有 cue。
2. `DAMAGE / HEAL / MISS / STATUS_*` 的第一版表达以数字、短标签、现有 emphasis 样式为主，不为五类 feedback 分别引入专属图标包或专属特效贴图。
3. 音频只允许复用现有 hit / crit / status / warning cue；本 PR 不新增 `audio.feedback.*` 之类的新 key family。
4. 若白盒验证发现可读性仍不足，第一优先是调 stack、位置、寿命、颜色和遮挡策略，而不是顺手扩成一套新的 VFX/音频包。
5. 如果后续确实需要专属 combat feedback raw asset，必须另开 companion asset PR，并沿用 [PR-09 Asset Batch Generation Checklist](/Users/luo/Documents/github/K-ToME/docs/review/phase3/2026-03-26-phase3-pr-09-asset-batch-generation-checklist.md)；涉及图片生成时，执行方必须先向用户索取 `GEMINI_API_KEY`。

## 5. 推荐改动面

### 5.1 `core`

1. `RenderSnapshot.kt`
2. serialization / contract tests

### 5.2 `game`

1. `FoundationGameSession.kt`
2. damage/status feedback emission tests

### 5.3 `client`

1. Tile render / overlay 消费
2. ASCII fallback
3. golden screenshot / smoke

## 6. 测试与自证

### 6.1 必测类

1. `FoundationGameSessionTest`
2. `GoldenScreenshotHarnessTest`
3. `ClientSmokeHarnessTest`
4. `BossHarnessTest`

### 6.2 必测行为

1. 普通攻击命中会产生 `DAMAGE` 反馈。
2. 暴击使用同一 `DAMAGE` 反馈表达 `critical=true`。
3. 治疗和净化会产生独立 typed feedback。
4. `MISS` 不再只能通过日志理解。
5. feedback 队列在大 AoE 场景下不会无限增长。

### 6.3 自动化命令

```bash
./gradlew :core:test
./gradlew combatTraceGolden
./gradlew bossHarness
./gradlew :client:clientSmoke --tests "*ClientSmokeHarnessTest"
./gradlew :client:goldenScreenshot --tests "*GoldenScreenshotHarnessTest"
./gradlew check
```

### 6.4 白盒验证

1. 触发一次普通攻击、一次暴击、一次治疗，确认 Tile 层都有即时反馈。
2. 对目标施加并移除一个状态，确认出现状态施加/移除提示。
3. 在 Boss 战里确认 telegraph 与 combat feedback 不互相遮挡。

## 7. 出口门禁

1. `RenderSnapshot` 已具备正式 `combatFeedbackEvents`。
2. Tile 路径已经消费 typed feedback。
3. `logEvents` 未被破坏，仍保留历史文本职责。
4. `goldenScreenshot / clientSmoke / bossHarness / check` 保持绿色。

## 8. 风险与止损

### 8.1 风险

1. snapshot 体积可能在高频 AoE 场景放大。
2. 如果 client 直接把所有 feedback 都画出来，UI 会变得过噪。
3. feedback 与 telegraph 叠加时可能产生遮挡。

### 8.2 止损

1. 队列先硬性截断到 `12` 条。
2. Tile 只做最小表达，不在本 PR 追求复杂动画。
3. 如果 ASCII fallback 成本过高，至少保证它能显示 compact feedback 列表，而不是完全忽略该字段。

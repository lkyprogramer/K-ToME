> 执行前必须先完整阅读并接受：
> `docs/review/phase3/phase3_opt_deep_review_final.md`
> `docs/review/phase3/v2/2026-03-29-phase3-v2-pr-14-floor-reward-cadence-and-shard-economy.md`
> `docs/review/phase3/2026-03-26-phase3-pr-08-reward-milestone-affixization.md`
> `docs/review/phase3/2026-03-26-phase3-pr-10-player-facing-information-cleanup.md`

# Phase 3 V3 - PR-19 Reward Presentation And Late-Run Reliquary Spend

**阶段**: `Phase 3 / v3 follow-up`  
**优先级**: `P1`  
**前置条件**: `PR-16 / PR-17` 已让完整 run 的后半程更成立，允许继续强化奖励“可见度”和后段 shard 花法  
**对应问题**: 当前奖励系统已经能避免空层，但玩家仍更容易感受到“有东西拿”，而不是“这个奖励很值得兴奋”。同时，现有 shard 花法主要集中在前中段两个 shop，后段资源价值感不够稳定。  

**Lane-parallel 拆分**：

- **W19a (Game/Client Contract)**: 奖励来源可见度与 presentation contract
- **W19b (Game/Data Lane)**: late-run reliquary spend 节点
- **W19c (QA Lane)**: cadence / route / boss / reliquary 的玩家可见回归

---

## 1. 阶段目标

把 Phase 3 的奖励体验从“稳定有用”推进到“至少关键奖励有记忆点、后段 shard 仍有明确花法”。

完成标准：

1. 玩家能够明确区分：
   - cadence reward
   - route reward
   - boss reward
   - cache / support reward
2. 至少新增一个 late-run shard spend 节点，优先复用现有 interactable / shop flow。
3. 新节点不要求复杂经济系统，但必须保证：
   - 有明确价格
   - 有明确次数限制
   - 不破坏既有救火工具语义
4. 本 PR 不引入 Phase 4 的 crafting / reforge / artifact economy。

## 2. 当前问题

1. cadence reward 已成立，但玩家未必能明显感知“这是系统补的 fallback reward”。
2. route / boss / cache reward 的来源身份在玩家侧仍然不够清晰。
3. `Shard` 的主要花法仍集中在 `greenwood_supply_post` 与 `deep_iron_pit_waystation` 两个 shop。
4. 后半段即使继续掉 shard，玩家也缺少明确的花法锚点。

### 2.1 本 PR 必须冻结的口径

1. 奖励 presentation 只消费正式 snapshot/log/mainfest 主链，不允许 client 自行推断第二套来源体系。
2. late-run shard spend 第一版优先复用现有 shop/service/interactable contract。
3. 若要新增 spend 节点，优先绑定：
   - `temple_ward_reliquary`
   - 必要时可考虑 `river_ferry_anchor`
4. 本 PR 默认不新增 raw art / raw audio。
5. 本 PR 不引入第二货币，不引入 crafting，不引入 reforge。

## 3. 范围与非目标

### 3.1 范围

1. 奖励来源 presentation
2. cadence / route / boss / cache 的玩家可见区分
3. `abyssal_temple` 或等价 late-run 节点的 shard spend
4. 相关 UI / smoke / golden / harness 观测

### 3.2 非目标

1. 不扩展完整商店系统
2. 不扩展完整 late-game economy loop
3. 不新增独立 reward asset 包

## 4. 技术方案

### 4.1 [W19a] Reward Source Presentation Contract

建议文件：

```text
core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt
client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt
game/src/main/resources/i18n/en-US.json
game/src/main/resources/i18n/zh-CN.json
```

冻结口径：

1. 第一版不需要新大 UI，只需要让玩家知道“这件奖励是什么来源”。
2. 可选最小实现：
   - reward log key 分层
   - reward list badge / label
   - cadence claim 的单独提示
3. `cadence reward` 必须有与普通掉落不同的玩家可见表达。
4. `route / boss / cache` 奖励来源至少在 log 或 summary 层可区分。

### 4.2 [W19b] Late-Run Reliquary Spend

建议文件：

```text
core/src/main/kotlin/com/ktome/core/economy/ShopModels.kt
game/src/main/resources/data/shops/index.yaml
game/src/main/resources/data/interactables/index.yaml
game/src/main/resources/data/objectives/index.yaml
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
```

冻结口径：

1. 第一版最小落点固定为 `temple_ward_reliquary`。
2. 允许的实现方式二选一：
   - 绑定一个新的 late-run `shopNodeId`
   - 或绑定单节点一次性 curated service
3. 推荐优先方案：
   - 新增 `abyssal_reliquary_post`
   - 库存偏 `PROTECTION / CLEANSING / OFFENSE / inscription`
   - 可包含一次 `REFRESH_STOCK`
4. 新节点必须满足：
   - 不破坏主线推进
   - 不强制玩家消费
   - 价格高于前中段 shop，形成后段 shard 出口
5. 若继续复用 `ShopOffer.serviceType`，优先沿用现有 buy flow，不开第三套菜单。

### 4.3 [W19c] QA And Reward Visibility Regression

建议文件：

```text
game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt
game/src/test/kotlin/com/ktome/game/LongRunWorldStructureSessionTest.kt
client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt
client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
```

冻结口径：

1. 至少补以下断言：
   - cadence reward 可见区分
   - late-run reliquary 节点能消费 shard
   - 新节点不破坏现有 objective/runtime
2. `LongRunLabFullTest` 至少增加一个 late-run spend 观测字段。

## 5. 推荐改动面

### 5.1 `core`

1. `ShopModels.kt`（若 late node 需要新 shop/service contract）
2. `RenderSnapshot.kt`（若需要最小 reward source snapshot 字段）

### 5.2 `game`

1. `FoundationGameSession.kt`
2. `shops/index.yaml`
3. `interactables/index.yaml`
4. `objectives/index.yaml`
5. `i18n/*.json`

### 5.3 `client`

1. `TileRenderModel.kt`
2. `AsciiRenderModel.kt`
3. `ClientSmokeHarnessTest`
4. `GoldenScreenshotHarnessTest`

## 6. 测试与自证

### 6.1 必测类

1. `FoundationGameSessionTest`
2. `LongRunWorldStructureSessionTest`
3. `ClientSmokeHarnessTest`
4. `GoldenScreenshotHarnessTest`
5. `LongRunLabFullTest`

### 6.2 必测行为

1. cadence reward 与普通掉落玩家可见层不同
2. route / boss / cache reward 来源可区分
3. late-run reliquary 节点能实际消费 shard
4. 新 spend 节点不会破坏既有 objective / boss / route gate

### 6.3 自动化命令

```bash
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew :game:test --tests "com.ktome.game.LongRunWorldStructureSessionTest"
./gradlew :client:clientSmoke --tests "*ClientSmokeHarnessTest"
./gradlew :client:goldenScreenshot --tests "*GoldenScreenshotHarnessTest"
./gradlew :game:test --tests "com.ktome.game.harness.LongRunLabFullTest"
./gradlew check
```

### 6.4 白盒验证

1. 在普通楼层触发一次 cadence reward，确认玩家能明显看出它不是普通掉落
2. 在 `abyssal_temple` 找到 reliquary 节点，确认 shard 有明确花法

## 7. 出口门禁

1. 奖励来源 presentation 正式成立
2. 至少一个 late-run shard spend 节点成立
3. smoke / golden / long-run 观测同步补齐
4. `./gradlew check` 保持绿色

## 8. 风险与止损

### 8.1 风险

1. 如果 reward presentation 做得过重，会让 UI 噪音增加
2. late-run spend 若定价不当，可能把 shard 价值推得过高或过低

### 8.2 止损

1. presentation 先做“能辨认来源”，不做复杂特效包
2. late-run spend 先做单节点、单价格、单次或小库存版本

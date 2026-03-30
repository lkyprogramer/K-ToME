> 执行前必须先完整阅读并接受：
> `docs/review/phase3/2026-03-26-phase3-pr-08-reward-milestone-affixization.md`
> `docs/review/phase3/2026-03-26-phase3-pr-09-content-floor-completion.md`
> `docs/review/phase3/2026-03-26-phase3-pr-10-player-facing-information-cleanup.md`
> `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`

# Phase 3 V2 - PR-14 Floor Reward Cadence And Shard Economy

**阶段**: `Phase 3 / v2 follow-up`  
**优先级**: `P2`  
**前置条件**: `PR-08` 已让 route / boss / cache reward 进入正式 affix milestone loop，`PR-10` 已完成玩家信息清理，允许继续处理“奖励节拍”和“Shard 花法不足”  
**对应问题**: 当前高价值奖励节点已经比旧版本完整，但楼层内的即时奖励节拍仍然偏薄；与此同时，Shard 经济仍主要停留在购买固定 shop offer。更合理的切法不是再拆成两个 PR，而是把它们合并成一个“奖励循环密度”PR：一边补非 Boss 楼层的 cadence reward，一边给 shop 增加第一个正式 shard sink 服务。

**Lane-parallel 拆分**：

- **W14a (Game Lane)**: floor reward cadence contract 与 fallback 生成链
- **W14b (Rules/Game/Client Lane)**: `shop service` 第一版 shard sink
- **W14c (QA Lane)**: reward cadence / shop economy regression 与长局消费观测

---

## 1. 阶段目标

把 Phase 3 的奖励循环从“路线奖励和 Boss 奖励成立，但楼层中段与 shard 消费都偏薄”推进到“每层都有最低回报节拍，Shard 也有购买固定货物之外的正式花法”。

完成标准：

1. 非 Boss 楼层在“本层没有拿到有意义奖励”时，会触发一次 cadence fallback reward。
2. cadence reward 不改 route / boss / cache milestone 契约，不回退成第二套随机系统。
3. `ShopOffer` 第一版新增正式 `service` 能力。
4. 第一种正式 shard sink 固定为 `REFRESH_STOCK`，用于刷新当前 shop 的未购买库存。
5. shop refresh 后，`rescuePolicy` 的 mandatory affordable rescue 语义仍成立。
6. 本 PR 不引入完整 crafting / reforge / strengthen 子系统。

## 2. 当前问题

1. 当前高价值奖励节点主要集中在 `route / boss / cache`，非 Boss 楼层仍可能出现“清完一层没有明确 build 回报”的空档。
2. [ShardEconomy.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/economy/ShardEconomy.kt) 当前只处理 affordability / sell value，没有更深的 shard sink 语义。
3. [ShopModels.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/economy/ShopModels.kt) 当前 `ShopOffer` 只支持 `itemBaseId / inscriptionId`，没有 service contract。
4. [shops/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/shops/index.yaml) 当前只有 `2` 个 shop node，且库存是固定列表，Shard 的 build 优化花法很少。

### 2.1 本 PR 必须冻结的口径

1. route / boss / cache milestone reward 合同不变，本 PR 不把它们改成第二套奖励系统。
2. cadence reward 只作为“非 Boss 楼层的 fallback 节拍”，不是每层强制白送一件高价值装备。
3. 第一版 shard sink 只实现一种正式 service：`REFRESH_STOCK`。
4. `REFRESH_STOCK` 必须保持：
   - rescue mandatory slot 不丢失
   - 已购买 offer 不回流
   - 价格和 affordability policy 继续受正式 shop contract 约束
5. 本 PR 不实现：
   - 装备强化
   - affix 重铸
   - 铭文永久刻印
6. 仍保持单一货币 `Shard`，不引入第二种经济资源。

## 3. 范围与非目标

### 3.1 范围

1. floor cadence reward fallback。
2. `ShopOffer.serviceType` typed contract。
3. `REFRESH_STOCK` 的 runtime、UI label 与验证。
4. reward / shop 相关回归与长局消费观测。

### 3.2 非目标

1. 不做完整 crafting 系统。
2. 不做 per-item 质量强化或 affix 重铸。
3. 不重写 shop UI 为新的多层菜单。
4. 不新增第二种 currency。
5. 不把 cadence reward 扩展成“每场战斗必掉落”。

## 4. 技术方案

### 4.1 [W14a] Floor Reward Cadence Contract

建议文件：

```text
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/main/resources/data/loot/index.yaml
game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt
game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt
```

冻结口径：

1. cadence reward 只在**非 Boss 楼层完成并准备下楼**时检查。
2. 若该层已经出现“有意义奖励”，则不触发 fallback。
3. 建议第一版“有意义奖励”定义为满足以下任一条件：
   - 获得可装备物品
   - 获得 inscription
   - 获得 `quality >= MAGIC` 的生成物
4. 若本层没有出现有意义奖励，则在 descent 结算前触发一次 cadence fallback reward：
   - 优先使用 zone-specific fallback profile
   - 找不到时回退到 `zone reward + common` 的受控组合
5. cadence reward 第一版每层最多一次。

建议实现方式：

1. 在 session 中维护 floor-local reward state：
   - `meaningfulRewardSeenThisFloor`
   - `cadenceRewardGrantedThisFloor`
2. 该状态在换层后重置。
3. cadence reward 走现有 `ItemGenerator + loot profile` 主链，不新建生成器。

### 4.2 [W14b] Shop Service Contract

建议文件：

```text
core/src/main/kotlin/com/ktome/core/economy/ShopModels.kt
core/src/test/kotlin/com/ktome/core/economy/ShopNodeTest.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/main/resources/data/shops/index.yaml
core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt
client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt
client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt
```

建议 contract：

```kotlin
enum class ShopServiceType {
    REFRESH_STOCK,
}

data class ShopOffer(
    val id: String,
    val itemBaseId: String? = null,
    val inscriptionId: String? = null,
    val serviceType: ShopServiceType? = null,
    val price: Int,
    val tags: Set<String> = emptySet(),
)
```

冻结口径：

1. `ShopOffer` 三选一：
   - `itemBaseId`
   - `inscriptionId`
   - `serviceType`
2. 第一版 `serviceType` 只允许 `REFRESH_STOCK`。
3. shop service 继续走现有 buy 流程，不新开第三种 shop mode。
4. `ShopOfferSnapshot` 继续用 `labelKey + price` 渲染即可，不需要新 UI 结构。

### 4.3 [W14b] REFRESH_STOCK Runtime

建议文件：

```text
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt
client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt
```

冻结口径：

1. `REFRESH_STOCK` 购买后只重刷：
   - 当前 shop 中未购买、且非 mandatory rescue 的 offer
2. 以下内容必须保留：
   - 已购买 offer 的缺失状态
   - rescue mandatory slot
   - affordability contract
3. 每个 shop node 在一局中最多允许购买一次 `REFRESH_STOCK`。
4. refresh 价格按 zone tier 固定，不做动态浮动。

建议的第一版价格：

| Shop | Service | Price |
| --- | --- | --- |
| `greenwood_supply_post` | `REFRESH_STOCK` | `35` |
| `deep_iron_pit_waystation` | `REFRESH_STOCK` | `60` |

### 4.4 [W14c] Reporting And Regression

建议文件：

```text
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
game/src/test/kotlin/com/ktome/game/harness/ScenarioModels.kt
```

冻结口径：

1. `longRunLab` 至少新增两类观测：
   - `cadenceRewardCount`
   - `shopRefreshPurchaseCount`
2. 验证目标不是“刷新越多越好”，而是确认：
   - run 中段 shard 有额外可花点
   - cadence reward 能填补空层
3. 若 cadence reward 导致装备膨胀过快，优先调触发阈值和 fallback profile，不回退 shop service contract。

### 4.5 资源复用冻结

冻结口径：

1. 本 PR 默认**不新增任何 raw image / raw audio**。`cadence reward` 继续复用现有 item / inscription / pickup cue；`REFRESH_STOCK` 继续复用现有 shop 列表、hover/confirm 音频和 i18n 文案路径。
2. `REFRESH_STOCK` 第一版不新增专属 shop icon、专属 NPC 立绘或专属服务动画；玩家识别依赖 `labelKey + price + once-per-shop` 正式 contract。
3. cadence reward fallback 也不引入新的宝箱/祭坛 raw asset。它只是现有 reward generator 的 fallback，不应被包装成第二套视觉系统。
4. 若后续需要专属 service badge、商店招牌或特制掉落提示，必须另开 companion asset PR；涉及图片生成时，执行方必须先向用户索取 `GEMINI_API_KEY`，再走现有 `generate_assets.sh` 管线。

## 5. 推荐改动面

### 5.1 `core`

1. `ShopModels.kt`
2. `ShopNodeTest.kt`
3. 必要时更新 `ShopOfferSnapshot`

### 5.2 `game`

1. `FoundationGameSession.kt`
2. `data/shops/index.yaml`
3. `data/loot/index.yaml`
4. reward / shop tests

### 5.3 `client`

1. shop 文案与 offer label 渲染
2. smoke / golden（如 shop 截图发生变化）

## 6. 测试与自证

### 6.1 必测类

1. `ShopNodeTest`
2. `FoundationGameSessionTest`
3. `LongRunLabTest`
4. `LongRunLabFullTest`
5. `ClientSmokeHarnessTest`

### 6.2 必测行为

1. 非 Boss 楼层在没有有意义奖励时会触发一次 cadence fallback reward。
2. 已经拿到有意义奖励的楼层不会重复触发 fallback。
3. `REFRESH_STOCK` 购买后只刷新未购买的非 mandatory rescue offer。
4. refresh 后 mandatory rescue affordance 仍满足原 contract。
5. 每个 shop node 只能刷新一次。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.economy.ShopNodeTest"
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew longRunLab
./gradlew :client:clientSmoke --tests "*ClientSmokeHarnessTest"
./gradlew check
```

### 6.4 白盒验证

1. 清一层普通楼层但没有拿到有意义奖励，确认下楼前会补一次 cadence reward。
2. 进入 `greenwood_supply_post`，购买 `REFRESH_STOCK`，确认库存发生变化但保底救火槽仍在。
3. 在同一 shop 再尝试刷新一次，确认会被拒绝。

## 7. 出口门禁

1. cadence reward 已能填补 dry floor。
2. `ShopOffer.serviceType` 已进入正式 contract。
3. `REFRESH_STOCK` 已成为第一种正式 shard sink。
4. `longRunLab / clientSmoke / check` 绿色。

## 8. 风险与止损

### 8.1 风险

1. cadence reward 可能导致装备膨胀和背包拥堵。
2. refresh service 若不保留 rescue guarantee，可能破坏长局救火链。
3. 如果在本 PR 同时加入过多 shop service，会把小优化做成半个 crafting 系统。

### 8.2 止损

1. cadence reward 只做 fallback，不做每层固定发奖。
2. shard sink 第一版只交付 `REFRESH_STOCK`，其余服务另开后续 PR。
3. 若 refresh 导致商店价值过高，先调价格和可用次数，不回滚 typed service contract。

> 执行前必须先完整阅读并接受：
> `docs/review/phase3/2026-03-26-phase3-follow-up-pr-07-objective-runtime-and-gate-hardening.md`
> `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
> `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 3 - PR-08 Reward Milestone Affixization

**阶段**: `Phase 3 / follow-up`  
**优先级**: `P0`  
**前置条件**: `PR-07` 完成（Objective / Optional Zone / Gate 已进入 runtime），`PR-06` 的 affix/buildTags 主链已稳定  
**对应问题**: 当前普通掉落已经能走 affix/buildTags，但路线奖励、Boss 奖励、cache 奖励仍大量回落为固定白板 `base item`。这会让 Phase 3 的中后程奖励更像“进度补丁”，而不是“构筑里程碑”。

**Lane-parallel 拆分**：

- **W8a (Rules Lane)**: milestone reward source contract、quality floor、min affix count、blacklist、reward build context
- **W8b (Content Lane)**: route/boss/cache reward data、profile/bias、保底 utility 与 affix milestone 分层
- **W8c (Tools/QA Lane)**: milestone reward harness、long-run/build report 字段、balance smoke

---

## 1. 阶段目标

把路线奖励、Boss 奖励和 cache 奖励接入正式 affix milestone loop，让高价值奖励节点真正推动 build 分化。

完成标准：

1. 路线奖励不再只发白板 `base item`，而是发放至少一个带质量底线和 affix 下限的 milestone reward。
2. Boss 奖励不再只靠职业偏好挑选固定 base item，而是进入统一的 milestone loot 生成链。
3. cache/support 奖励中的高价值节点同样接入 affix milestone loop。
4. `shop` 的 rescue inventory 继续保持 deterministic 保底，不被本 PR 随机化。
5. reward affix 化必须消费正式 `buildTags`，不允许再维护第二套“奖励偏好”真源。
6. 路线 `rescueTags` 要进入 reward bias，而不是只停留在 route preview 文案。
7. 需要冻结质量底线、affix 下限、blacklist 与 fallback 规则。
8. `longRunLab` 报告要能看出 milestone reward 是否真正改变 build，而不是只留下一个偏薄的 `buildHash`。

## 2. 当前问题

1. `claimRouteReward()` 仍直接调用 `itemBaseDef(...).toRuntimeItem()`，路线奖励是固定白板物品。
2. `rewardItemFromProfiles()` 最终仍走 `officialRewardItem()`，Boss / cache / support 奖励主要是“职业适配 base item”，不是 affix milestone。
3. `RouteReward.rescueTags` 只进入 route panel 信息面，没有进入 reward 生成权重。
4. 当前高价值奖励节点没有 quality floor 和 affix 下限，导致 affix 系统主要作为普通掉落调味料存在。
5. 现有 `buildHash` 能用于粗粒度复现，但无法解释“这局为什么因为 milestone reward 改了 build”。
6. 如果直接把所有 milestone reward 全部随机化，会破坏 `shop` 的救火职责和路线保底语义。

### 2.1 本 PR 必须冻结的口径

1. 高价值奖励节点必须区分：
   - `guaranteed utility reward`
   - `affix milestone reward`
2. `shop` 仍保持 deterministic rescue policy，不接入 affix 随机化。
3. `RouteReward` 的一次性领取语义不变，变化只发生在 reward 内容生成方式上。
4. milestone reward 统一走正式 `ItemGenerator + AffixSelectionContext` 主链，不允许新建第二套奖励专用生成器。
5. 质量底线第一版固定为：
   - route milestone：至少 `MAGIC`
   - optional/cache milestone：至少 `MAGIC`
   - mandatory Boss milestone：至少 `RARE`
   - finale Boss milestone：至少 `RARE` 且 affix 下限更高
6. affix 下限第一版固定为：
   - `MAGIC`: 至少 `1` affix
   - `RARE`: 至少 `2` affix
7. milestone reward blacklist 必须同时覆盖：
   - 与职业资源轴冲突
   - 与当前 base slot 冲突
   - 重复 affix family
   - rescue-only utility item
8. `rescueTags` 只影响 reward bias，不得让路线奖励重新退化成固定白板救火件。
9. 本 PR 不引入 unique / artifact / set item / crafting。

## 3. 范围与非目标

### 3.1 范围

1. route reward affix milestone 化。
2. Boss reward affix milestone 化。
3. cache / support 高价值奖励 affix milestone 化。
4. milestone quality floor / affix floor / blacklist / fallback。
5. reward build context 与 route bias。
6. long-run / run summary / balance report 的 milestone reward 观测字段。

### 3.2 非目标

1. 不做 `shop` inventory 全量随机化。
2. 不做 full artifact / unique / relic 生态。
3. 不做新的 affix family 扩容；优先使用现有 affix v1。
4. 不做玩家信息面文案收口，这属于 `PR-10`。
5. 不做基础职业/怪物补量，这属于 `PR-09`。
6. 不新增物品插画、reward 专属特效图或配乐；milestone reward 默认复用现有 `item / affix` 的 visual/audio key，来源区分优先通过 frame / tint / badge 组合表达。

## 4. 技术方案

### 4.1 [W8a] Milestone Reward Source Contract

建议文件：

```text
core/src/main/kotlin/com/ktome/core/item/MilestoneRewardSource.kt
core/src/main/kotlin/com/ktome/core/item/AffixSelectionContext.kt
core/src/test/kotlin/com/ktome/core/item/MilestoneRewardGenerationTest.kt
```

冻结口径：

1. milestone reward source 第一版固定为：
   - `ROUTE`
   - `BOSS`
   - `CACHE`
2. `AffixSelectionContext` 需要补充：
   - `rewardSource`
   - `qualityFloor`
   - `minAffixCount`
   - `buildTags`
   - `routeBiasTags`
   - `blacklistFamilies`
3. reward 生成不能只看 `base.tags`，必须同时看：
   - profession/resource axis
   - unlocked talent/build tags
   - route `rescueTags`
   - zone / reward source
4. fallback 规则必须冻结：
   - 若候选 affix 为空，不得 silently 回退到白板
   - 允许降到同品质最低合法 affix 组合
   - 只有 deterministic utility reward 才允许白板发放

### 4.2 [W8b] Route Reward 分层

建议文件：

```text
game/src/main/resources/data/world/world_graph.yaml
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/test/kotlin/com/ktome/game/LongRunWorldStructureSessionTest.kt
```

冻结口径：

1. `RouteReward` 分成两层：
   - `guaranteedUtilityDropIds`
   - `milestoneRewardProfileIds`
2. `guaranteedUtilityDropIds` 继续承担保底救火或路线特色件。
3. `milestoneRewardProfileIds` 负责生成真正的 affix 里程碑奖励。
4. 对 optional route，允许 `uniqueContentTag` 参与 milestone reward bias。
5. route reward 报告至少记录：
   - base item id
   - quality tier
   - affix ids
   - route id
   - reward source

### 4.3 [W8b] Boss / Cache Reward Affixization

建议文件：

```text
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
game/src/main/resources/data/bosses/index.yaml
game/src/main/resources/data/loot/*.yaml
```

冻结口径：

1. `officialRewardItem()` 不再直接返回 `base.toRuntimeItem()`。
2. Boss reward 需改成：
   - 先按 profile 选 base
   - 再按 milestone context 生成 affix item
3. cache / support 节点中标记为高价值的奖励，也走同一生成链。
4. `abyssal_heart` finale reward 必须保证比前置 mandatory Boss 更高的质量底线。
5. Boss-specific reward bias 可存在，但必须通过同一 context 表达，不允许在 `FoundationGameSession` 里硬编码三套分支。

### 4.4 [W8a] Blacklist 与 Build Relevance

冻结口径：

1. blacklist 第一版至少覆盖：
   - 不可装备的 base slot
   - 与 profession 主资源冲突的 affix
   - 互斥 affix family
   - deterministic rescue utility base
2. build relevance 的优先排序固定为：
   - profession/resource axis
   - unlocked talent/build tags
   - route bias tags
   - zone source bias
3. 若 milestone reward 生成结果不满足 `minAffixCount`，必须 fail fast 或重抽，不允许静默降级。

### 4.5 [W8c] 报告与回归

建议文件：

```text
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
game/src/test/kotlin/com/ktome/game/harness/ScenarioModels.kt
build/reports/harness/long-run-full.md
```

冻结口径：

1. `long-run-full` 至少新增：
   - milestone reward quality distribution
   - milestone affix count distribution
   - route reward affix usage summary
2. run summary / harness report 不要求完整 build 语义升级，但必须能回答：
   - 这局拿到了哪些 milestone item
   - 它们是否带 affix
   - 它们来自 route / boss / cache 哪一类节点

## 5. 推荐改动面

### 5.1 `core`

1. `core/item/AffixSelectionContext.kt`
2. `core/item/ItemGenerator.kt`
3. milestone reward source / generation test

### 5.2 `game`

1. `FoundationGameSession.kt`
2. route reward / loot profile / boss reward 数据
3. milestone reward 生成上下文装配

### 5.3 `client`

1. 本 PR 不强制做完整玩家信息面改造
2. 若需要最小配套，只允许补 reward preview 所需 snapshot 字段
3. 若需要区分 `ROUTE / BOSS / CACHE` 来源，优先复用现有 icon atlas 与 quality frame，不单开 raw asset 生产

### 5.4 `tools / QA`

1. `LongRunLabFullTest.kt`
2. reward 统计报告扩展
3. 必要时增加 milestone reward smoke

## 6. 测试与自证

### 6.1 必测类

1. `MilestoneRewardGenerationTest`
2. `LongRunWorldStructureSessionTest`
3. `FoundationGameSessionTest`
4. `LongRunLabTest`
5. `LongRunLabFullTest`

### 6.2 必测行为

1. route reward 不再全部是白板物品。
2. Boss reward 能生成带 affix 的里程碑奖励。
3. cache reward 的高价值节点也进入同一 reward 主链。
4. shop inventory 仍保持 deterministic rescue 行为。
5. milestone reward 不会生成与职业资源轴冲突的非法 affix 组合。
6. 报告能统计 milestone reward quality / affix 分布。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.item.*"
./gradlew :game:test --tests "com.ktome.game.LongRunWorldStructureSessionTest"
./gradlew :game:test --tests "com.ktome.game.FoundationGameSessionTest"
./gradlew :game:longRunLab --tests "*LongRunLabTest"
./gradlew :game:longRunLab --tests "*LongRunLabFullTest"
./gradlew check
```

### 6.4 白盒验证

1. 通关一条 mandatory route，确认路线奖励出现带 affix 的 milestone item。
2. 击杀一个 mandatory Boss，确认掉落不是固定白板件。
3. 进入商店，确认 rescue 保底 offer 仍 deterministic。

## 7. 出口门禁

1. route / boss / cache 三类 milestone reward 都已进入 affix 主链。
2. `shop` 仍保持 deterministic rescue policy。
3. `rescueTags` 已进入 reward bias，不再只是 route preview 文案。
4. `longRunLab` 报告能看见 milestone reward affix 统计。
5. `./gradlew check` 保持绿色。

## 8. 风险与止损

### 8.1 风险

1. 高价值奖励随机性上升后，数值波动会增大。
2. 当前 bot/harness 可能因为更强 milestone 奖励而改变路线/战斗节奏。
3. 如果一次性把 deterministic reward 全换成随机 reward，`shop` 与 route 的救火语义会被稀释。

### 8.2 止损

1. 保留 `guaranteedUtilityDropIds`，只把 milestone 部分 affix 化。
2. 先冻结 quality floor / minAffixCount，不追求一次做成完整 unique/artifact 系统。
3. 任何随机化变化必须进入报告字段，而不是只看是否仍能通关。

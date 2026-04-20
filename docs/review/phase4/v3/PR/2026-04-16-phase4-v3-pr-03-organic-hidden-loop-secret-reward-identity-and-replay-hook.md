> 执行前必须先完整阅读并接受：
> `docs/rule/kotlin.md`
> `docs/rule/ai-change-governance.md`
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-03-secret-reward-identity-and-organic-hidden-loop.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part2.md`
> `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part3.md`
> `docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-02-build-identity-dynamic-loot-and-profession-capstones.md`

# Phase 4 V3 PR-03 Organic Hidden Loop、Secret Reward Authority / Identity 与 Replay Hook 内容化

**阶段**: `Phase 4 / Post-Review Follow-up / V3-PR-03`  
**优先级**: `P0`  
**工作量评估**: `M-L`（`4~6` 人日）  
**前置条件**: `V3-PR-01`，建议在 `V3-PR-02` 后执行  
**对应问题**:

1. `organic hidden discovery` 当前是假绿
2. secret-zone reward 仍存在 `SecretZoneDef.rewardProfileId` 与 hidden event `LOOT_PROFILE` 并存的第二真源风险
3. secret zone / secret reward 的身份仍不够独立
4. `greenwood_ambush_hideout` 目前更像布局变化，不像内容分支

## 0. 验证约束

1. 默认开发回路先跑 `./gradlew verifyChanged`。
2. 本 PR 默认联合验收固定为：
   - `whiteBoxHiddenContent`
   - `organicHiddenProbe`
   - `whiteBoxLoot`
   - `verifyOwner`
   - `phase4Report`
3. canonical 证据必须包含：
   - `leadDiscoveryRate`
   - `secretConversionRate`
   - `searchUseRate`
   - `secretZoneEntryRate`
   - same-zone local overlap guardrail
   - `secretZoneRewardAuthorityViolations`

### 0.1 Computer Use / Validation Mode 快速白盒模块

本 PR 的人工白盒验证默认复用：

1. [docs/opt/cheatMode.md](/Users/luo/Documents/github/K-ToME/docs/opt/cheatMode.md:917) 中 `PR-03 / PR-07 / PR-09` 对应映射
2. [docs/verification/validation-mode.md](/Users/luo/Documents/github/K-ToME/docs/verification/validation-mode.md:7) 中 `HIDDEN_CONTENT / CONTENT_PACK` 使用说明

推荐按下面两个模块执行，优先使用 `Validation Mode`，不要重新走长流程自然探索。

#### 模块 A：`HIDDEN_CONTENT` 主闭环

- Validation preset：`HIDDEN_CONTENT`
- setup 关注项：
  - 确认 preset 摘要显示 hidden/search 路径
  - 若 UI 显示 seed / zone / floor，记录到人工验证备注
- 局内操作顺序：
  1. `F9` 打开 Validation overlay
  2. 执行 `Travel to Search Anchor`
  3. 执行正式 `Execute Search`
  4. 观察 reveal 结果后执行 `Travel to Hidden Entrance`
  5. 进入 secret 路径后继续执行 `Travel to Secret Reward`
  6. 最后执行 `Travel to Secret Return`
- 必看证据：
  - `SearchAction` 成功/失败反馈是正式日志，不是 Validation 私有文案
  - reveal 后 hidden entrance / return bridge 路径真实可达
  - secret reward 来源在 log / inspect / route 上可读，不只是地图几何变化
  - return 后能重新回到主路径，证明 secret loop 是闭环

#### 模块 B：`CONTENT_PACK` pack-enabled hidden 路径

- Validation preset：`CONTENT_PACK`
- setup 关注项：
  - 确认默认启用 `sample.flooded_relics`
  - 若 setup 未显示 sample pack active roots，视为 blocker
- 局内操作顺序：
  1. `F9` 打开 overlay
  2. 先确认 overlay header 中的 `active pack ids`
  3. 复用模块 A 的 `Search Anchor -> Execute Search -> Hidden Entrance -> Secret Reward` 路径
- 必看证据：
  - overlay 中可见 `sample.flooded_relics`
  - 客户端实际出现 sample pack 内容，而不是只看到 active ids
  - 内容 key / namespace 对人工读屏是可解释的，不是不可读 raw path

#### 人工判断边界

以下判断仍必须由人工确认，不能被 Validation 快速路径替代：

1. reveal / enter / reward / return 是否构成“愿意主动搜、且搜到了真的有记忆点”的完整 loop
2. `greenwood_ambush_hideout` 一类 replay hook 是否已经从几何扰动提升为真正内容分支
3. sample pack 内容是否在客户端里真实成立，而不是只有 metadata / active ids 成立

## 1. 阶段目标

把 hidden 从“系统存在且脚本能验证”推进到“玩家在自然游玩中愿意主动搜、且搜到了真的有记忆点”。

完成标准：

1. `leadDiscoveryRate` 不再靠 primer-only 假绿
2. secret-zone reward authority 收成单一真源
3. secret reward 在 same-zone 内有足够身份差异
4. replay hook 能产出内容记忆点，不只是地图扰动

## 2. 为什么单独成 PR

1. 这条线的核心不是内容数量，而是 discovery loop 的真实性。
2. 如果把它并进 build/reward 大 PR，问题会被“掉落池变好了”掩盖。
3. replay hook 的真正价值，也必须在 hidden 闭环里才能看见。

## 3. 当前问题拆解

### 3.1 `discovered` 定义过宽

当前 `hiddenEventIds.isNotEmpty()` 就能把 `discoveryRate` 刷绿，不能证明玩家真的揭示并进入了 secret zone。

### 3.2 replay hook 目前更多是几何变化

`greenwood_ambush_hideout` 当前仍复用 `greenwood_hidden_cache` 的 reward profile / visual / audio。

### 3.3 secret-zone reward authority 仍有第二真源风险

当前 secret-zone 奖励同时受：

1. `SecretZoneDef.rewardProfileId`
2. hidden event `LOOT_PROFILE`

两条路径影响。只要这两条线都能表达 secret reward，长期就会继续积累“文档说一套、运行时还能从另一条线绕过去”的漂移。

### 3.4 local reward identity 还不够硬

即便 average overlap 合格，same-zone 高价值 pair 仍可能在玩家感知里相互撞脸。

## 4. 必须冻结的合同

1. 不新增新的 hidden trigger 类型。
2. 不新开大型 tutorial / clue UI。
3. replay hook 继续走 content/schema wiring，不引入新规则系统。

## 5. 范围与非目标

### 5.1 范围

1. hidden owner metric 重定义
2. secret-zone reward authority 单一真源
3. secret reward local identity 强化
4. `greenwood` 第二 secret 分支内容化
5. 必要的最小 frontstage 提示强化

### 5.2 非目标

1. 不做新的 mapgen 主系统
2. 不做大型新 secret zone 扩容
3. 不重写 profession capstone 规则本身

## 6. 技术方案

### 6.1 拆开 hidden owner metric

把当前 discovery 口径拆为至少两层：

1. `leadDiscoveryRate`
2. `secretConversionRate` 或 `entranceRevealRate`

纯 hidden event / primer 命中，不再等价于“hidden loop 成功”。

### 6.2 secret-zone reward 单一权威

把 `SecretZoneDef.rewardProfileId` 收成 secret-zone 奖励的唯一真源：

1. 在 hidden reward schema 中增加 `SECRET_ZONE_REWARD`
2. `HiddenEventRewardPayload.SecretZoneReward` 不再携带 `lootProfileRef`
3. secret-zone reward 运行时统一通过 `secretZoneId -> SecretZoneDef.rewardProfileId` 解析
4. 保留通用 `LOOT_PROFILE`，但它只允许继续用于非 secret-zone hidden event
5. `FoundationGameSession.executeHiddenEvents(...)` 中，`SECRET_ZONE_REWARD` 路径必须要求 `secretZoneId != null`，否则 fail fast
6. `Phase4StaticContentValidator` 必须新增两条硬规则：
   - 被 `SecretZoneDef.guaranteedContent` 引用的 hidden event 不得再声明 `LOOT_PROFILE`
   - 每个 secret-zone reward hidden event 必须恰好包含一个 `SECRET_ZONE_REWARD`

### 6.3 same-zone local identity 硬化

对最差 pair 设置 strict ceiling，而不是只看 average overlap。

### 6.4 `greenwood_ambush_hideout` 内容化

至少补这三件事：

1. 新增 `loot.greenwood_ambush_hideout.secret`
2. `greenwood_ambush_hideout.rewardProfileId` 指向新 profile
3. `hidden.event.greenwood.ambush_hideout.reward` 改为 `SECRET_ZONE_REWARD`，不再自带 profile id
4. 独立 text identity
5. 独立 visual/audio identity；优先复用现有 hidden/secret 资源族，若现有 key 无法拉开记忆点，则走 `phase4-pr07` 既有 image/audio 生成管线补齐，不新开 resource family
6. 至少一条与原 `hidden_cache` 不同的 guaranteed payoff

### 6.5 replay hook 与 profession capstone 接线

如果 `PR-02` 已给出 profession capstone chase path，本 PR 应优先让 secret zone 成为其中一条“可追逐来源”，而不是继续只给 zone-generic reward。

## 7. 推荐改动面

1. `tools/src/main/kotlin/com/ktome/tools/hidden/*`
2. `tools/src/main/kotlin/com/ktome/tools/phase4/*`
3. `game/src/main/kotlin/com/ktome/game/Phase4StaticContentValidator.kt`
4. `game/src/main/kotlin/com/ktome/game/data/schema/SchemaModels.kt`
5. `game/src/main/kotlin/com/ktome/game/data/DataLoader.kt`
6. `game/src/main/resources/data/secret-zones/index.yaml`
7. `game/src/main/resources/data/events/index.yaml`
8. `game/src/main/resources/data/loot/index.yaml`
9. 必要时 `FoundationGameSession.kt`

## 8. 任务拆解

### Task 1：hidden metric 重定义

- **目标**: 消除假绿
- **验收**:
  - primer-only 不再计入闭环成功
  - report 中 scripted 与 organic 明确分开

### Task 2：secret-zone reward authority 单一真源

- **目标**: 消灭 secret reward 的第二真源
- **验收**:
  - `SecretZoneDef.rewardProfileId` 成为唯一 secret-zone reward authority
  - `secretZoneRewardAuthorityViolations = 0`
  - validator / runtime fail-fast 路径到位

### Task 3：same-zone secret reward identity 强化

- **目标**: 保证玩家能感到“这是 secret，不是放大的普通奖励”
- **验收**:
  - strict ceiling 命中最差 pair
  - secret source 的 payoff 与 cadence/reward 明显分离

### Task 4：greenwood 第二 secret 分支内容化

- **目标**: replay hook 从几何差异升级为内容分支
- **验收**:
  - `greenwood_ambush_hideout` 不再复用旧 reward profile
  - `greenwood_ambush_hideout` 通过 `SecretZoneDef.rewardProfileId` 走正式 secret reward authority
  - `greenwood_ambush_hideout` 不再与 `greenwood_hidden_cache` 共用同一套 visual/audio identity
  - 至少有 1 个独立记忆点

## 9. 推荐命令

```bash
./gradlew whiteBoxHiddenContent
./gradlew organicHiddenProbe
./gradlew whiteBoxLoot
./gradlew verifyOwner
./gradlew phase4Report
```

## 10. 完成后才能进入下一 PR 的条件

1. hidden 的 green 不再是报告解释出来的，而是指标自己能说明的。
2. secret-zone reward authority 已经收口成单一真源，而不是 schema/runtime 双路径并存。
3. replay hook 不再只代表“布局变了”，而代表“内容也变了”。

> 执行前必须先完整阅读并接受：
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md`
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md`
> `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-04-loot-profile-v3-and-reward-pool-differentiation.md`
> `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-05-hidden-content-diversity-and-entrance-entropy.md`

# Phase 4 - V2OPT PR-03 Secret Reward 身份重建与 Organic Hidden 闭环

**阶段**: `Phase 4 / Post-Review Follow-up / V2OPT-W3`  
**优先级**: `P0`  
**工作量评估**: `L`（`4~6` 人日）  
**前置条件**: `V2OPT-PR-01` 已把 hidden scripted/organic 指标分离  
**对应问题**:

1. current hidden correctness 成立，但 organic experience 没有被证明
2. secret reward 与 same-zone cadence/reward 高度同质
3. 玩家冒风险进入 secret zone，拿到的往往只是“普通奖励的大包”
4. primer / search / secret entry 没有形成稳定、可度量的体验闭环

---

## 1. 阶段目标

把 hidden 从“合同正确”推进到“玩家真的想继续搜”，同时让 secret reward 拥有可被记住的身份。

完成标准：

1. organic hidden 发现、搜索、进入、回报有正式指标。
2. secret reward 不再与 same-zone cadence/reward 高度重叠。
3. secret reward 至少在一个维度上拥有清晰身份：
   - base item
   - slot bias
   - template bias
   - reward 结构
4. 玩家不依赖 primer 内部机制，也能在真实 run 中得到可感知 hidden 回报。

---

## 2. 工作量评估与整合结论

### 2.1 为什么 reward identity 与 organic hidden 必须合并

如果拆开做：

1. 只做 organic hidden 指标，不改 reward 身份，结论仍会是“找到了，但不值得找”。
2. 只改 reward profile，不改 organic 路径，结论仍会是“东西值钱，但玩家自然进不去”。

这是同一条体验链：

```text
察觉线索 → 愿意搜索 → 找到入口 → 愿意进入 → 拿到记得住的回报 → 下局继续搜
```

任何一个环节单独修都不闭环。

### 2.2 本 PR 不做的事

1. 不返工全部 hidden event 数量
2. 不新增大规模 topology anchor 家族
3. 不做全面 UI 重构

本 PR 只修：

1. organic 路径能不能成立
2. reward 身份是不是足够强

---

## 3. 当前问题拆解

### 3.1 local reward identity 仍然破

当前已确认的高风险 pair：

1. `loot.abyssal_temple.cadence ↔ loot.abyssal_temple_warded_archive.secret = 0.818`
2. `loot.deep_iron_pit.reward ↔ loot.deep_iron_slag_cache.secret > 0.90`
3. `loot.deep_iron_pit.reward ↔ loot.deep_iron_smuggler_stash.secret > 0.90`

这类 pair 的问题不是“平均分化不够”，而是“玩家会把 secret 误认成普通主线奖励”。

### 3.2 organic hidden 缺的不是 correctness

当前已经成立：

1. `SearchAction` typed contract
2. proof 与 runtime 一致
3. reveal / reward / return bridge 路径正确

当前没有成立：

1. 无 primer 时玩家能否自然触发
2. 玩家是否愿意主动搜索
3. 玩家找到后是否愿意进
4. 进入后回报是否强到值得记住

---

## 4. 本 PR 必须冻结的合同

1. hidden 的 correctness 与 experience 永久分离。
2. secret reward 必须有自己的身份，不能再只是 `rewardBudget` 更大。
3. organic hidden 只允许通过正式 player-like pathing / search 行为产生，不允许调用内部 reveal shortcut。
4. 本 PR 可以破坏式重写 secret reward profile；不保留旧池兼容。

---

## 5. 范围与非目标

### 5.1 范围

1. `loot/index.yaml` 的 secret profile 身份重建
2. `events/index.yaml` 与 `secret-zones/index.yaml` 的 organic 路径与奖励结构重调
3. organic hidden probe runner
4. 必要的 search / reveal 反馈增强

### 5.2 非目标

1. 不做职业化掉落分发
2. 不做 terrain / elite 主题修复
3. 不扩 content pack 语义

---

## 6. 技术方案

### 6.1 Secret Reward 身份设计原则

每个 secret profile 至少要在以下四项里命中两项：

1. `base item exclusivity`
2. `slot bias distinctness`
3. `specialTemplateTagPreference distinctness`
4. `reward structure distinctness`

不再接受只有 `rewardBudget` 差异。

### 6.2 同 zone local guardrail

必须把以下 pair 当作设计 owner：

1. `zone.cadence ↔ zone.secret`
2. `zone.reward ↔ zone.secret`

第一版上限：

1. `sameZoneSecretVsCadenceMaxOverlap <= 0.50`
2. `sameZoneSecretVsRewardMaxOverlap <= 0.50`

对最差 pair 允许更严格：

1. `abyssal_temple_warded_archive.secret <= 0.35`
2. `deep_iron_* secret <= 0.40`

### 6.3 organicHiddenProbe

建议新增：

```text
tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt
```

输入：

1. 固定 seed 列表
2. 固定 profession/race 组合
3. 禁用任何 primer shortcut

输出：

1. `organicHiddenDiscoveryRate`
2. `organicSecretZoneEntryRate`
3. `organicSearchActionUseRate`
4. `firstDiscoveryTurnP50/P90`
5. `zoneDiscoveryDistribution`

执行策略：

1. 复用现有 run bot / headless harness
2. 只允许 bot 使用正常 `Search`、移动、战斗、交互
3. 不允许 test-only primer API

### 6.4 reward 结构差异化

每个 secret zone 至少落到以下三类之一：

1. `LOOT_PROFILE`
2. `LOOT_PROFILE + GRANT_BUFF`
3. `TRIGGER_ENCOUNTER + LOOT_PROFILE`
4. `LOOT_PROFILE + SERVICE / SHOP / CURRENCY`

推荐：

1. `greenwood_hidden_cache`
   - 偏 early control / mobility / stealth 回报
2. `deep_iron_slag_cache`
   - 偏 forge / oil / armor-break 主题
3. `deep_iron_smuggler_stash`
   - 偏 contraband / precision / burst 或 service
4. `underground_river_crystal_rift`
   - 偏 water / ice / mana / mobility
5. `abyssal_temple_warded_archive`
   - 偏 relic / ward / protection / controlled risk

### 6.5 primer 与 search 的最小反馈增强

不做大 UI，只做 runtime + log + inspect 足够强的提示：

1. primer 获得时必须有明确高价值日志
2. search 失败也要给出“这里确实值得搜，但你没过检定”的反馈
3. secret reward 首次可见时必须有区别于普通 loot 的 source 文案

---

## 7. 推荐改动面

### 7.1 `game` 数据

1. [loot/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/loot/index.yaml)
2. [events/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/events/index.yaml)
3. [secret-zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/secret-zones/index.yaml)

### 7.2 `game` runtime

1. [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
2. 现有 hidden/search 日志与 reward presentation 路径

### 7.3 `tools`

1. [HiddenContentHarnessRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/hidden/HiddenContentHarnessRunner.kt)
2. `OrganicHiddenProbeRunner.kt`（新增）
3. [WhiteBoxLootRunner.kt](/Users/luo/Documents/github/K-ToME/tools/src/main/kotlin/com/ktome/tools/loot/WhiteBoxLootRunner.kt)
4. `phase4Report` 对 hidden / reward identity 的透传

---

## 8. 实施顺序

### Task 1：锁定最差 local pair 并设计目标身份

- **位置**：`loot/index.yaml`
- **目标**：为每个最差 pair 指定 secret-only identity
- **验收**：
  - 每个 secret profile 都有书面身份

### Task 2：重写 secret profile

- **目标**：通过 `itemIds / itemTagFilter / excludeIds / typeWeights / slotBias / templateBias` 重建身份
- **验收**：
  - local overlap 达标

### Task 3：organicHiddenProbe runner

- **目标**：不依赖 primer 的真实发现率
- **验收**：
  - runner 可稳定产出 JSON/MD
  - 无 primer shortcut

### Task 4：runtime 最小反馈增强

- **目标**：提高 primer/search/secret reward 的可感知性
- **文件**：`FoundationGameSession.kt`、i18n
- **验收**：
  - 日志区分普通 reward 与 secret reward

### Task 5：report owner 收口

- **目标**：phase4Report 同时展示 organic hidden 与 local reward identity

---

## 9. 资源生成计划

### 9.1 图片

本 PR 不新增图片资源。

### 9.2 音频

本 PR 不新增音频资源。

### 9.3 复用基线

1. hidden entrance / return bridge 复用 `Phase4 PR07` 已落地资源：
   - `prop.hidden_entrance.revealed`
   - `prop.hidden_entrance.return_bridge`
2. secret zone 身份复用现有 secret-zone 视觉资源：
   - `zone.secret.greenwood_hidden_cache.(icon|visual)`
   - `zone.secret.deep_iron_slag_cache.(icon|visual)`
   - `zone.secret.deep_iron_smuggler_stash.(icon|visual)`
   - `zone.secret.underground_river_crystal_rift.(icon|visual)`
   - `zone.secret.abyssal_temple_warded_archive.(icon|visual)`
3. reveal / zone cue 复用现有音频资源：
   - `audio.hidden.reveal.secret_entrance`
   - `audio.secret_zone.greenwood_hidden_cache`
   - `audio.secret_zone.deep_iron_slag_cache`
   - `audio.secret_zone.deep_iron_smuggler_stash`
   - `audio.secret_zone.underground_river_crystal_rift`
   - `audio.secret_zone.abyssal_temple_warded_archive`

### 9.4 约束

1. secret reward identity 必须通过 reward structure、source 文案、local overlap 降低和 organic path 成立来证明，不得退化为“再画一套新 secret 美术/音效”。
2. 本 PR 允许把现有 hidden/secret 资源更明确地接到日志、inspect、reward source 和 reveal 路径，但不新增新的 visual/audio family。
3. 若实现声称必须新增 `zone.secret.*`、`audio.secret_zone.*` 或 `prop.hidden_*` key，先证明当前 canonical 资源无法覆盖；在 `v2opt` 范围内默认视为不成立。

---

## 10. 测试策略

### 9.1 自动化命令

```bash
./gradlew hiddenContentHarness
./gradlew whiteBoxHiddenContent
./gradlew whiteBoxLoot
./gradlew phase4Report
```

### 9.2 必测行为

1. `organicHiddenProbe` 不使用 primer 也能跑完。
2. same-zone secret overlap 全部低于 guardrail。
3. secret reward source 在日志与 report 中有独立身份。
4. hidden correctness 不回归。

### 9.3 推荐白盒

1. 抽查每个 secret zone：
   - 为什么值得进
   - 奖励身份与普通 reward 有何不同
2. 抽查至少一个 failed search：
   - 玩家是否能理解这是检定失败，而不是没内容

---

## 11. 出口门禁

1. `organicHiddenDiscoveryRate` 正式上线。
2. `sameZoneSecretVsCadenceMaxOverlap` 与 `sameZoneSecretVsRewardMaxOverlap` 全部达标。
3. hidden scripted correctness 不回归。
4. secret reward 在 report 中可解释。

---

## 12. 风险与 Gotchas

1. **不要只靠 `excludeIds` 暴力排空候选池**  
   这会让 profile 过窄，掉落失去弹性。
2. **不要让 organicHiddenProbe 变成“弱版 primer harness”**  
   一旦内部直接触发 primer，本 PR 就失真。
3. **不要把 UI 大改混进来**  
   本 PR 只做最小反馈增强。

---

## 13. 回滚策略

1. 若 organic runner 复杂度过高，可先用 headless bot + limited route 集合做第一版，不要求立即覆盖全部职业。
2. 若个别 secret profile 调整过头导致掉落过窄，优先回退 profile 权重，不回退 organic hidden 路径本身。

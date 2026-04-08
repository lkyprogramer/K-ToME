> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md`
> `docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`

# Phase 4 - OPT PR-05 Hidden Content 多样化与入口熵提升

**阶段**: `Phase 4 / Post-Review Follow-up / OPT-W5`  
**优先级**: `P1`  
**前置条件**: `OPT PR-01`、`OPT PR-04` 完成  
**对应问题**: 当前 hidden content 基本等于“到 `optional.branch.1` 后搜索”，trigger type 与 entrance binding 过于同质，发现过程缺少变化。

---

## 1. 阶段目标

把 hidden content 从单一路径、单一 reveal 语义，升级成多 trigger type、多入口 anchor 家族、多奖励结构的正式系统。

完成标准：

1. `hiddenTriggerTypeCoverage >= 4/6`
2. `secretEntranceBindingCoverage >= 3`
3. `hiddenContentHarness` 仍维持 `>=30% run 触发 hidden event`
4. `solvabilityHarness` 保持 `CRITICAL_PATH 100%` 可达

## 2. 当前问题

1. reveal event 只用 `PERCEPTION_REVEAL`
2. reward event 只用 `INTERACT_TILE`
3. `entranceBindingId` 全是 `optional.branch.1`
4. hidden reward 结构差异仍不足

### 2.1 本 PR 必须冻结的口径

1. hidden reward 不绕过 `LootBudget`
2. secret zone 不承担主线硬门槛
3. anchor 选择必须来自已审计 corpus，不允许在文档里臆造稳定 anchor
4. 若现有 anchor 家族审计不通过，本 PR 应优先升级 topology / hidden entrance contract，引入正式可复用的 hidden anchor 家族，而不是把设计长期锁死在当前 incidental anchor 上

## 3. 范围与非目标

### 3.1 范围

1. trigger type 多样化
2. entryRule 多样化
3. entrance binding / anchor 家族打散
4. hidden reward 结构差异化
5. `hiddenContentHarness + whiteBoxHiddenContent` 扩展

### 3.2 非目标

1. 不无边界重写 `mapgen topology`
2. 允许为 hidden content 正式引入最小 anchor family 升级
3. 不把 terrain 可感知性问题混进本 PR

## 4. 技术方案

### 4.1 trigger type 多样化

建议最小集合：

1. `greenwood` 保持 `PERCEPTION_REVEAL`
2. `deep_iron` 改成 `KILL_ELITE`
3. `underground_river` 改成 `OPEN_CHEST`
4. `abyssal_temple` 改成 `QUEST_STEP`

如需加分项，可再引入 `ENTER_ROOM`。

### 4.2 entryRule 多样化

建议：

1. 保留一条基础 `PERCEPTION_CHECK`
2. 至少一条改成 “reveal 即可进入”
3. 至少一条改成 “前置事件 + perception 组合”

### 4.3 anchor 审计优先

本 PR 先做 anchor audit，再决定采用哪一种长期方案：

1. `<selected-critical-anchor>`
2. `<selected-optional-anchor>`
3. `<selected-goal-adjacent-anchor>`

若现有家族不足以满足 `>=3` 类稳定 hidden 入口，则不再为了兼容现状而妥协，直接升级 topology contract，新增正式 hidden anchor families。

不再预设：

1. `critical.route.2`
2. `optional.branch.2`
3. `critical.goal`

### 4.3.1 Hidden Anchor 合同升级方案

如果现有 topology 无法稳定产出足够的 hidden 入口家族，本 PR 不接受长期停留在“从现有 incidental anchor 里勉强挑几个”的状态，而是直接升级正式合同：

1. 在 topology / mapgen schema 中引入 hidden 专用 anchor family，例如：
   - `hidden.branch`
   - `hidden.critical.adjacent`
   - `hidden.goal.adjacent`
2. `hiddenEntrancePlans` 只依赖这些正式 family，而不是写死某个偶然 anchor id
3. `whiteBoxMapgen / solvabilityHarness` 必须证明这些 anchor family 在目标 zone/floor 上稳定存在
4. 新 family 若进入主干，就成为 `Phase 4`/`Phase 5` 后续 hidden content 的唯一正式入口族

### 4.3.2 实施边界

为避免方向漂移，实施时按以下规则执行：

1. 先跑 anchor audit
2. 若现有 family 足够，直接做 hidden content 数据改造
3. 若现有 family 不足，同一 PR 内先做 topology contract 升级，再做 hidden content 数据改造
4. 不接受“先上线一版临时 anchor 绑定，后面再整理”的两阶段长期中间态

### 4.4 reward 结构差异化

最低应覆盖：

1. `LOOT_PROFILE`
2. `LOOT_PROFILE + GRANT_BUFF`
3. `TRIGGER_ENCOUNTER + LOOT_PROFILE`
4. `LOOT_PROFILE + 高 rewardBudget / specialTemplateTagPreference`

### 4.5 新增 hidden event 的条件化策略

若现有 topology 已足够，可直接新增：

1. `deep_iron.smuggler_stash`
2. `abyssal.void_whisper`

若 audit 不通过：

1. 在同一 PR 内补最小 `topology + hiddenEntrancePlan` 合同升级
2. 之后再新增 `deep_iron.smuggler_stash` / 其他新 secret zone
3. 不接受“因为当前 topology 不方便，所以把新增 secret zone 永久下掉”的方案

## 5. 推荐改动面

### 5.1 `game`

1. `game/src/main/resources/data/events/index.yaml`
2. `game/src/main/resources/data/secret-zones/index.yaml`
3. `game/src/main/resources/data/mapgen/zones/index.yaml`
4. `game/src/main/resources/data/loot/index.yaml`
5. `game/src/main/resources/i18n/zh-CN.json`
6. `game/src/main/resources/i18n/en-US.json`

### 5.2 `tools`

1. `HiddenContentHarnessRunner`
2. `WhiteBoxHiddenContentRunner`
3. `Phase4ReportRunner`

### 5.3 `mapgen`

优先使用已存在的稳定 anchor 家族；若不足以支撑正式 hidden diversity，必须升级 topology 结构，而不是把复杂度长期藏在 case-by-case 例外里。

若进入合同升级路径，至少落到：

1. topology / mapgen schema 定义
2. `game/src/main/resources/data/mapgen/zones/index.yaml`
3. `whiteBoxMapgen / solvabilityHarness / hiddenContentHarness` 的 anchor family 解释与断言

## 6. 测试与自证

### 6.1 自动化命令

```bash
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew hiddenContentHarness
./gradlew whiteBoxHiddenContent
./gradlew whiteBoxVerify
./gradlew phase4Report
```

### 6.2 必测行为

1. `hiddenTriggerTypeCoverage >= 4/6`
2. `secretEntranceBindingCoverage >= 3`
3. 某个 upgraded zone 的 hidden event 触发率不得长期为 `0`
4. `CRITICAL_PATH` 可达性不受影响

## 7. 资源生成计划

### 7.1 图片

默认不开资源批次。

只有在出现以下情况时，才新增：

1. 新 secret zone portrait/icon
2. 新 hidden entrance prop
3. 新 secret-zone 专属可见节点

对应 plan：

`assets-src/image/specs/phase4-opt-pr05-gemini-plan.yaml`

### 7.2 音频

默认不开资源批次。

只有在出现以下情况时，才新增：

1. 新 reveal cue
2. 新 secret-zone ambience
3. 新 hidden interactable cue

对应 plan：

`assets-src/audio/specs/phase4-opt-pr05-audio-plan.yaml`

### 7.3 管线约束

1. 若不开资源批次，不创建空 plan 文件
2. 若开资源批次，复用 `PR-07` 的 key 组织方式：
   - 图片：`prop.hidden_*`、`zone.secret.*.icon`、`zone.secret.*.visual`
   - 音频：`audio.hidden.reveal.*`、`audio.secret_zone.*`
3. 图片仍走：

```bash
GEMINI_API_KEY=your_key \
GEMINI_CONCURRENCY=4 \
./scripts/generate_assets.sh \
  assets-src/image/specs/phase4-opt-pr05-gemini-plan.yaml \
  assets-src/image/raw/generated \
  assets-src/image/manifests/phase4-opt-pr05-generation-report.jsonl
```

4. 音频仍走：

```bash
python3 scripts/generate_opt_pr05_audio.py \
  --plan assets-src/audio/specs/phase4-opt-pr05-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-pr05-generation-report.jsonl

python3 scripts/process_audio.py \
  --filter-plan assets-src/audio/specs/phase4-opt-pr05-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-pr05-processing-report.jsonl
```

## 8. 出口门禁

1. `hiddenTriggerTypeCoverage >= 4/6`
2. `secretEntranceBindingCoverage >= 3`
3. hidden event 总量达到 `>=12`
4. 若现有 anchor 审计不足，则必须完成 topology/anchor 合同升级；不接受以“保持 4 个 secret zone”作为长期终态
5. `hiddenContentHarness` 与 `solvabilityHarness` 同时通过

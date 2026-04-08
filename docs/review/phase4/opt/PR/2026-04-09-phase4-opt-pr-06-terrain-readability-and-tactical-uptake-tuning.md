> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md`
> `docs/review/phase4/opt/2026-04-08-phase4-verified-optimization-pr-plan.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`

# Phase 4 - OPT PR-06 地形可感知性与战术 Uptake 调优

**阶段**: `Phase 4 / Post-Review Follow-up / OPT-W6`  
**优先级**: `P1`  
**前置条件**: `OPT PR-01`、`OPT PR-02` 完成  
**对应问题**: 当前 terrain 系统已经“规则正确”，但 review 指向的主要问题是 exposure 低、实战触发少、客户端反馈弱。这个问题必须在基线指标出来后再定向调优。

---

## 1. 阶段目标

基于 `terrainTaggedCombatExposureRate` 与 `terrainInteractionEncounterRate` 的真实基线，对地形系统做分层调优，而不是继续无差别堆规则。

完成标准：

1. `terrainTaggedCombatExposureRate` 相对基线提升 `>=50%`
2. `terrainInteractionEncounterRate` 相对基线提升 `>=30%`
3. `solvabilityHarness` 保持 `100%` 主线可达
4. 不新增 `TerrainTag`

## 2. 当前问题

1. 自动化已证明五种地形交互规则正确，但没有证明实战经常发生
2. 现有问题可能来自三个不同层次：
   - 地形摆放位置不对
   - 战斗/遭遇没有和地形相遇
   - 玩家看到了也没感知

### 2.1 本 PR 必须冻结的口径

1. 不新增 `TerrainTag` 枚举值
2. 不引入第二套环境规则
3. 正式 terrain rule 仍保持单一路径
4. 若 elite-terrain affinity 成为正式长期设计，则应进入显式数据字段，而不是永久藏在生成权重代码里

## 3. 范围与非目标

### 3.1 范围

1. 基于基线走 A/B/C 决策树调优
2. 可能涉及：
   - terrainTagWeights
   - TerrainTagPainter 分布
   - elite 房间选择偏好
   - client readability

### 3.2 非目标

1. 不扩新的 terrain rule
2. 不返工 `PR-06` 已冻结的五种正式交互效果
3. 不把 hidden content 的入口问题混进本 PR

## 4. 技术方案

### 4.1 决策树

1. `Path A`
   - 条件：`exposureRate < 20%`
   - 动作：调 terrain 覆盖与摆放
2. `Path B`
   - 条件：`exposureRate >= 20%` 但 `encounterRate < 15%`
   - 动作：调精英/战斗与地形相遇概率
3. `Path C`
   - 条件：`exposureRate >= 20%` 且 `encounterRate >= 15%`
   - 动作：补 client readability

实际执行允许 `A+B` 或 `A+C` 组合。

### 4.2 Path A：地形覆盖与摆放

建议调优项：

1. zone `terrainTagWeights`
2. `TerrainTagPainter`
3. vault/pattern room 内的覆盖倾向
4. elite/boss room 内的覆盖优先级

约束：

1. 只改权重与分布策略
2. 不改变已有规则效果

### 4.3 Path B：精英-地形相遇概率

长期方案优先显式化，不优先隐藏在生成逻辑里：

1. 若 Path B 只是短期 spike，可先在生成逻辑里验证权重策略
2. 一旦确认 elite-terrain affinity 是正式长期内容语义，就把它提升为 `EliteMutationDef` 的显式字段，例如 `preferredTerrainTags: List<TerrainTag>`
3. 最终合入主干的正式方案不应长期依赖“按 tag 猜测 affinity”的隐式代码路径

### 4.3.1 `preferredTerrainTags` 正式化规则

若 Path B 成立并进入主干，字段设计应一次到位：

1. 字段落在 `EliteMutationDef`，而不是生成器私有配置
2. 类型使用显式集合：`preferredTerrainTags: List<TerrainTag>`
3. 语义只表达“房间/生成权重偏好”，不直接改变 combat rule
4. 没有 terrain 偏好的 mutation 明确写空集合，而不是依赖 null / 缺省猜测
5. loader / white-box / bossHarness 都要能读出该字段并解释其实际生效情况

### 4.3.2 不接受的中间态

1. 不接受最终主干长期保留“根据 mutation 名称/tag 手写 if/else 猜 terrain affinity”
2. 不接受一部分 mutation 走显式字段、另一部分继续走隐式权重逻辑
3. 不接受 client / tools 侧各自维护第二份 terrain affinity 推断表

### 4.4 Path C：客户端可感知性

建议最小增强：

1. terrain tile 视觉强化
2. combat log 高亮 terrain trigger
3. inspect 面板展示 terrain tag 与交互规则
4. 实体/格子上的 terrain badge

## 5. 推荐改动面

### 5.1 `game`

1. `game/src/main/resources/data/mapgen/biomes/index.yaml`
2. `game/src/main/resources/data/mapgen/zones/index.yaml`
3. `game/src/main/kotlin/com/ktome/game/mapgen/...`
4. elite 房间选择相关生成逻辑
5. 若 Path B 被正式化：`game/src/main/resources/data/elites/index.yaml` 与对应 loader/schema

### 5.2 `client`

1. terrain tile visual
2. log 可读性
3. inspect/tooltip
4. 如有需要，少量 badge/icon

### 5.3 `tools`

1. `terrainInteractionBatch`
2. `bossHarness`
3. `phase4Report`

## 6. 测试与自证

### 6.1 自动化命令

```bash
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew whiteBoxMapgen
./gradlew whiteBoxVerify
./gradlew phase4Report
./gradlew goldenScreenshot
```

### 6.2 必测行为

1. exposure 与 encounter rate 提升
2. `solvabilityHarness` 不回归
3. 任何 elite-terrain 偏置都不改变正式 rule contract
4. 若走 Path C，客户端截图能稳定呈现新反馈

## 7. 资源生成计划

### 7.1 图片

Path A/B 默认不强制新资源。

只有走 Path C 且净新增了玩家可见资源 key 时，才补：

1. `assets-src/image/specs/phase4-opt-pr06-gemini-plan.yaml`
2. `assets-src/image/manifests/phase4-opt-pr06-generation-report.jsonl`
3. `assets-src/image/manifests/phase4-opt-pr06-processing-report.jsonl`

建议覆盖对象：

1. terrain readability overlay
2. terrain badge / state icon
3. 必要时的 interact-highlight prop

### 7.2 音频

默认不新增音频。

只有在确实新增独立 terrain cue 时，才补：

1. `assets-src/audio/specs/phase4-opt-pr06-audio-plan.yaml`
2. `assets-src/audio/manifests/phase4-opt-pr06-generation-report.jsonl`
3. `assets-src/audio/manifests/phase4-opt-pr06-processing-report.jsonl`

### 7.3 管线约束

1. 纯 recolor / shader / atlas 重排不应强行开新资源批次
2. 若开图片批次，固定走：

```bash
GEMINI_API_KEY=your_key \
GEMINI_CONCURRENCY=4 \
./scripts/generate_assets.sh \
  assets-src/image/specs/phase4-opt-pr06-gemini-plan.yaml \
  assets-src/image/raw/generated \
  assets-src/image/manifests/phase4-opt-pr06-generation-report.jsonl
```

3. 若开音频批次，固定走：

```bash
python3 scripts/generate_opt_pr06_audio.py \
  --plan assets-src/audio/specs/phase4-opt-pr06-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-pr06-generation-report.jsonl

python3 scripts/process_audio.py \
  --filter-plan assets-src/audio/specs/phase4-opt-pr06-audio-plan.yaml \
  --report assets-src/audio/manifests/phase4-opt-pr06-processing-report.jsonl
```

## 8. 出口门禁

1. `terrainTaggedCombatExposureRate` 相比基线提升 `>=50%`
2. `terrainInteractionEncounterRate` 相比基线提升 `>=30%`
3. `solvabilityHarness` 的主线可达性保持 `100%`
4. 不新增 `TerrainTag`
5. 若新增 client 可见资源，`goldenScreenshot` 必须同步通过

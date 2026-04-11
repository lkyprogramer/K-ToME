> 执行前必须先完整阅读并接受：
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md`
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md`
> `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md`
> `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-06-terrain-readability-and-tactical-uptake-tuning.md`

# Phase 4 - V2OPT PR-04 Terrain / Mutation 语义收口与主题防稀释

**阶段**: `Phase 4 / Post-Review Follow-up / V2OPT-W4`  
**优先级**: `P1`  
**工作量评估**: `M-L`（`3~5` 人日）  
**前置条件**: `V2OPT-PR-01` 已冻结 terrain sample contract；`V2OPT-PR-03` 不再占用同一批 reward/hidden 改动  
**对应问题**:

1. `preferredTerrainTagsSeen` 当前只观测到 `WATER`
2. `applyToTags` 注入 `elite` 后，主题约束变软
3. `grey_crown` 缺 terrain flavor
4. `greenwood_fringe` 作为 combat-sampled zone 仍是体感最弱点

---

## 1. 阶段目标

把 terrain / elite / boss 体系从“已跑通”提升为“主题语义明确、指标可解释、不会被后续内容继续稀释”。

完成标准：

1. `preferredTerrainTagsSeen` 不再长期停留在单维度。
2. elite mutation 的主题约束不再只靠 `allowedZones` 撑住。
3. `grey_crown` 拥有可解释 terrain flavor。
4. `greenwood_fringe` 的 tactical hook 不再只剩单条脆弱路径。

---

## 2. 工作量评估与整合结论

### 2.1 为什么这个 PR 不再混 UI 抛光

terrain / mutation 当前的主要问题是**语义与分发**，不是“屏幕上不够亮”。  
如果把 frontstage 可感知性也混进来，容易重新变回旧 `OPT PR-06` 那种“大而散调优 PR”。

因此本 PR 只做：

1. 主题语义
2. 分发约束
3. terrain 观测维度

前台反馈统一放到 `V2OPT-PR-05`。

### 2.2 为什么不重开第二套 terrain 合同

本 PR 明确不做：

1. 新 `TerrainTag`
2. 新 terrain rule
3. 新 boss graph

只在已有合同上收口，避免扩大 Phase 4 债务面。

---

## 3. 当前问题拆解

### 3.1 `applyToTags` 语义混杂

当前 `EncounterDecorationService.mutationSelectionTags(...)` 会在 elite eligible 时注入 `elite`。  
这修掉了 runtime 断链，但也带来一个长期问题：

1. `applyToTags` 同时承担“能不能上”和“主题该不该上”两件事
2. 一旦注入 `elite`，很多 mutation 会在资格层过宽

长期更干净的做法是拆开语义。

### 3.2 `preferredTerrainTagsSeen` 单维度

当前 runtime 只稳定看到 `WATER`。  
这至少说明：

1. `OIL / ICE` 的主题没有被稳定映射成 encounter 感知
2. 现有 mutation + zone + monster pool 的联动还不够硬

### 3.3 `grey_crown` 缺 terrain flavor

当前 `boss.variant.grey_crown` 的 granted mutations 对 terrain 没有主题指向。  
这不是 correctness bug，但对中盘 boss identity 是明显缺口。

---

## 4. 本 PR 必须冻结的合同

1. 不新增 `TerrainTag`。
2. 不新增 terrain interaction rule。
3. mutation 的“资格”和“主题”语义分离。
4. boss variant 的主题优先继续由 mutation / weight profile 表达，不新开第二套 boss terrain system。

---

## 5. 范围与非目标

### 5.1 范围

1. `EliteMutationDef` 的主题语义收口
2. `EncounterDecorationService` 的 selection / weighting 调整
3. `boss-variants/index.yaml` 与 `elites/index.yaml` 的主题调优
4. `terrainInteractionBatch` 与 `bossHarness` 的新观测指标

### 5.2 非目标

1. 不改 `whiteBoxMapgen` replay hook
2. 不返工 hidden reward
3. 不改 client 前台视觉

---

## 6. 技术方案

### 6.1 把 `applyToTags` 拆成两层语义

建议破坏式升级 `EliteMutationDef`：

```kotlin
data class EliteMutationDef(
    val id: String,
    val eligibleTags: Set<String>,
    val themeTags: Set<String>,
    ...
)
```

迁移规则：

1. 旧 `applyToTags` 全量迁到 `eligibleTags`
2. 提取其中真正表达主题的 tags 到 `themeTags`
3. `elite` 只应保留在 `eligibleTags`

运行时规则：

1. `eligibleTags` 决定是否进入候选集
2. `themeTags` 不决定资格，只决定额外权重

这样既保留 elite 注入修复，又防止主题被资格逻辑稀释。

### 6.2 主题加权

建议在 `EncounterDecorationService` 增加：

1. `themeTagMatchBonus`
2. `terrainTagMatchBonus`
3. `themeTagMismatchPenalty`

推荐口径：

1. 若 template / zone / mutation 有具体 theme tag 交集 -> `+2 ~ +4`
2. 若只靠 `elite` 命中资格而无主题交集 -> 保底可选，但权重下调

### 6.3 `preferredTerrainTagsSeen` 目标

本 PR 不要求所有 zone 全维度均匀出现，但要求：

1. runtime 观测不再只剩 `WATER`
2. `OIL` 和 `ICE` 至少都进入稳定可观测样本

实现方向：

1. 校正适配 zone 的 monster pool / elite pool
2. 让对应 mutation 更可能与其 terrain zone 配对
3. 在 `terrainInteractionBatch` 新增 theme-specific observation

### 6.4 `grey_crown` 主题修复

推荐优先级：

1. 先修 granted mutation 组合
2. 再修 action weight profile
3. 最后才考虑新增 boss-specific 字段

建议目标：

1. `grey_crown` 至少有一个 granted mutation 具备非空 terrain 偏好或明确 zone theme
2. `grey_crown` 的 `bossHarness` 输出能说明其 terrain flavor 来源

### 6.5 greenwood tactical hook

greenwood 当前不应再只依赖“WATER 多一点”。  
本 PR 应至少补一个稳定 hook：

1. 更适合 early combat 的 terrain-trigger talent source
2. 或让 greenwood elite pool 更容易与已有 cold/water 规则形成交集

约束：

1. 不新增新地形规则
2. 不用隐藏第二套 early-game terrain exception

---

## 7. 推荐改动面

### 7.1 `game`

1. [EncounterDecorationService.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/elites/EncounterDecorationService.kt)
2. [elites/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/elites/index.yaml)
3. [boss-variants/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/boss-variants/index.yaml)
4. 必要时 `ai/index.yaml`
5. 必要时 zone monster/elite pool 数据

### 7.2 `tests / tools`

1. [TerrainInteractionBatchTest.kt](/Users/luo/Documents/github/K-ToME/game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt)
2. `BossHarnessTest.kt`
3. `phase4Report` 对新 theme 观测值的透传

---

## 8. 实施顺序

### Task 1：`EliteMutationDef` 语义拆分

- **目标**：`eligibleTags` / `themeTags` 分离
- **文件**：schema、loader、YAML
- **验收**：
  - 所有 mutation 完成迁移
  - fail-fast 校验正常

### Task 2：selection / weighting 调整

- **目标**：保留 elite 注入修复，同时恢复主题约束
- **文件**：`EncounterDecorationService.kt`

### Task 3：`preferredTerrainTagsSeen` 多维度恢复

- **目标**：让 `OIL / ICE` 也进入稳定观测
- **文件**：elite/zone pool 数据、terrain batch

### Task 4：`grey_crown` 主题修复

- **目标**：中盘 boss 不再缺 theme
- **文件**：`boss-variants/index.yaml`, `elites/index.yaml`, `bossHarness`

### Task 5：greenwood tactical hook 补强

- **目标**：greenwood 至少有两个稳定 hook，而不是单一路径

---

## 9. 资源生成计划

### 9.1 图片

本 PR 不新增图片资源。

### 9.2 音频

本 PR 不新增音频资源。

### 9.3 复用基线

1. mutation 图标继续复用 `Phase4 PR06` 与 `OPT PR02` 已落地的 `icon.mutation.*` 资源族。
2. terrain 相关前台素材继续复用 `vfx.terrain.interaction.water / oil / ice / oil_burning`。
3. mutation / terrain 音频继续复用 `audio.mutation.*` 与 `audio.terrain.water / oil / ice / oil_burning`。
4. `grey_crown` theme flavor 必须由现有 boss / mutation / terrain cue 组合表达，不额外引入 boss-only art/audio family。

### 9.4 约束

1. 这个 PR 的 owner 是语义、分发和 theme weight，不是新资源；若实现需要额外 `icon.mutation.*`、`vfx.terrain.*`、`audio.terrain.*` key，说明边界切错。
2. `greenwood` tactical hook 补强必须靠 zone pool、mutation 选择和 terrain 语义完成，不得靠“补一张更显眼的图”偷过。
3. 本 PR 不新增任何 `assets-src/*/specs/phase4-v2opt-pr04-*` 文件。

---

## 10. 测试策略

### 9.1 自动化命令

```bash
./gradlew terrainInteractionBatch
./gradlew bossHarness
./gradlew phase4Report
```

### 9.2 必测行为

1. `preferredTerrainTagsSeen` 不再只剩 `WATER`
2. `eligibleTags` / `themeTags` 迁移不破坏 elite mutation 总量和合法 pair
3. `grey_crown` 在 `bossHarness` 中具备明确 theme 来源
4. greenwood per-zone lower bound 不再成为单点离群

---

## 11. 出口门禁

1. mutation 资格与主题语义正式分离。
2. `preferredTerrainTagsSeen` 至少稳定覆盖 `WATER / ICE / OIL`。
3. `grey_crown` 的 theme 来源能被 `bossHarness` 解释。
4. 不新增地形规则，不新增第二套 boss/elite 体系。

---

## 12. 风险与 Gotchas

1. **不要用更多 `allowedZones` 硬补主题问题**  
   这只会让选择逻辑更脆。
2. **不要为 `grey_crown` 新开 boss terrain override 系统**  
   优先继续走 mutation/weight 语义。
3. **不要把 greenwood 改成“过度地形化”**  
   它仍然是 early-game zone。

---

## 13. 回滚策略

1. 若 `eligibleTags/themeTags` 迁移副作用过大，可先保留 loader 双读，但 YAML 只写新字段；最终主干不保留双写。
2. 若 greenwood 调整影响 early-game 难度过大，优先回调 pool 权重，不回退语义拆分本身。

> 生成依据：
> `docs/review/phase3/phase3_opt_deep_review_final.md`
> `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`
> `docs/review/phase3/v2/2026-03-29-phase3-v2-pr-13-encounter-density-and-zone-mechanics.md`
> `docs/review/phase3/v2/2026-03-29-phase3-v2-pr-14-floor-reward-cadence-and-shard-economy.md`

# Phase 3 V3 PR Roadmap

**阶段**: `Phase 3 / v3 follow-up`  
**目标**: 把最终综合审查报告中“必须在 Phase 3 修”和“建议在 Phase 3 增强”的事项，拆成可以直接进入开发排期的 PR 级工作包。  

## 1. 直接结论

当前 `Phase 3` 不需要再开“大而全的总修复 PR”。最合理的切法是：

1. 先补 `P0` 地基：
   - `PR-15`：长局 gate 收紧，补硬 full-route 验证
   - `PR-16`：`underground_river / crystal_cavern` 进入正式 runtime
   - `PR-17`：`abyssal_temple / abyssal_heart` 进入正式 runtime
2. 再做 `P1` 玩法增强：
   - `PR-18`：基础职业 breakpoint payoff 与 affix synergy
   - `PR-19`：奖励可见度增强 + late-run shard spend 节点
3. 最后做 `P2` 收尾：
   - `PR-20`：`guard.basic` 尾部清理与中后期普通怪行为再收口

## 2. 必做项与建议项

### 2.1 Phase 3 必做

| PR | 标题 | 优先级 | 解决问题 |
| --- | --- | --- | --- |
| `PR-15` | Full Route Gate Hardening And End-To-End Matrix | `P0` | `longRunLab` 口径偏软，不能充分证明完整长局成立 |
| `PR-16` | River And Crystal Runtime Activation | `P0` | `underground_river / crystal_cavern` 只有 hint，没有真正机制 |
| `PR-17` | Abyssal Ward And Finale Runtime Activation | `P0` | `abyssal_temple / abyssal_heart` 终线只有战斗，没有足够 runtime 差异 |

### 2.2 建议在 Phase 3 继续做

| PR | 标题 | 优先级 | 解决问题 |
| --- | --- | --- | --- |
| `PR-18` | Base Class Breakpoint Payoff And Affix Synergy | `P1` | 基础职业 build 分化感不足 |
| `PR-19` | Reward Presentation And Late-Run Reliquary Spend | `P1` | 奖励“有用但不兴奋”，后半段 shard 花法不足 |
| `PR-20` | Guard Profile Tail Tightening | `P2` | 中后期仍有一批怪物挂在泛用 profile 上，压平普通战斗体验 |

## 3. 依赖顺序

```text
PR-15 (gate hardening)
  ├── PR-16 (river / crystal runtime)
  └── PR-17 (abyssal / finale runtime)

PR-16 + PR-17
  ├── PR-18 (base class breakpoint payoff / affix synergy)
  └── PR-19 (reward presentation / late shard spend)

PR-18 + PR-19
  └── PR-20 (guard.basic tail tightening)
```

说明：

1. `PR-15` 必须最先做，因为它会定义后续 PR 的真实验收口径。
2. `PR-16 / PR-17` 可以并行，但推荐先做 `PR-16`，因为 `river / crystal` 的复杂度和风险都低于 finale 端。
3. `PR-18 / PR-19` 都依赖 late-zone runtime 基本成立，否则很难判断“构筑增强”和“奖励增强”是否真的改善完整 run。
4. `PR-20` 可以单独推进，但更适合放在 `PR-18 / PR-19` 之后一起看长局体验变化。

## 4. 工作量与切法说明

### 4.1 为什么不把 late-zone runtime 合成一个 PR

因为 `river / crystal` 与 `abyssal / finale` 的风险明显不同：

1. 前者主要是中后段常规 zone 差异化。
2. 后者直接影响终线节奏、finale 清晰度和 Boss 前后体验。
3. 合成一个 PR 会同时触碰太多 zone 数据、runtime 分支和回归场景，不利于止损。

### 4.2 为什么把奖励可见度和 late shard spend 合成一个 PR

因为这两个问题在玩家侧是同一件事：

1. 玩家当前缺的不是“系统里没有奖励”，而是“奖励不够可见、后段资源不够好花”。
2. 这两个增强都会主要触碰 `FoundationGameSession + RenderSnapshot + route/shop/interactable data + client render`。
3. 合成一个 PR，可以避免先做一半 UI 再回头补经济节点。

### 4.3 为什么 `guard.basic` 尾部清理单独成 PR

因为它最适合做成“低风险内容/AI 收口 PR”：

1. 大部分改动落在 `data/monsters/index.yaml` 和 `data/ai/index.yaml`
2. 可尽量复用现有 profile
3. 失败时容易回滚，不会污染其他 P0/P1 主链

### 4.4 资源补充结论

`PR-15 ~ PR-20` 不需要平均摊资源包。

1. `PR-16 / PR-17` 需要补最小 `zone/interactable/audio identity pack`
2. `PR-19` 不再单独新增 raw 资源，直接复用 `PR-17` 的 reliquary 资源
3. `PR-15 / PR-18 / PR-20` 不应掺资源生产范围

详细方案见：

`docs/review/phase3/v3/2026-03-30-phase3-v3-pr15-pr20-asset-and-audio-assessment.md`

## 5. 本轮不单独晋升为 PR 的事项

以下问题暂不单独立 PR，而是作为后续候选：

1. 失败复盘 / death analysis 更强的玩家信息面
2. 更完整的新手引导
3. 更丰富的 combat feedback 动画层

原因：

1. 它们价值明确，但当前不该抢在 `P0` 和核心玩法增强之前。
2. 其中一部分可以作为 `PR-19` 的 companion polish 跟进，而不必现在再开一个独立主 PR。

## 6. 进入开发前的统一要求

所有 `v3` follow-up PR 开始前，执行方都必须先阅读并接受：

1. `docs/review/phase3/phase3_opt_deep_review_final.md`
2. `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
3. `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
4. `docs/phase3/2026-03-13-phase3-verification-checklist.md`
5. 对应前置的 `Phase 3 follow-up` / `v2` PR 文档

## 7. 执行建议

推荐按下面顺序推进：

1. `PR-15`
2. `PR-16`
3. `PR-17`
4. `PR-18`
5. `PR-19`
6. `PR-20`

如果资源允许，`PR-16` 与 `PR-17` 可以并行；`PR-18` 与 `PR-19` 也可以并行，但前提是 `PR-15` 已经先把 gate 补硬。

# Phase 4 V3 后续 PR 计划

- 日期：`2026-04-16`
- 目的：把 `docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part1~4.md` 中所有需要继续优化改造的工作，按**工作量、依赖、可审阅性**重组为下一轮 `PR` 级文档。
- 参考风格：
  - [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-05-early-replay-hook-and-frontstage-readability.md)
  - [docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md)
- 上游结论来源：
  - [phase4_opt_deep_review_phase4_codex_part1.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part1.md)
  - [phase4_opt_deep_review_phase4_codex_part2.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part2.md)
  - [phase4_opt_deep_review_phase4_codex_part3.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part3.md)
  - [phase4_opt_deep_review_phase4_codex_part4.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/phase4_opt_deep_review_phase4_codex_part4.md)

## 1. 直接结论

下一轮不建议再开一个“大而全”的收尾 PR。  
最合理的推进方式，是按下面 5 个 PR 顺序执行：

1. 先修 **owner metric + 关键路径 pacing**，避免继续在假绿指标上做体验判断。
2. 再修 **build/reward 主干问题**，包括动态奖励覆盖、special-tier 深度、profession capstone chase path。
3. 然后修 **organic hidden + secret-zone reward authority + replay hook**，把 hidden 从“正确”推进到“值得主动追”，同时消灭 secret reward 的第二真源。
4. 再修 **boss 终盘语言 + version discipline**，把终盘辨识度和 pack boundary 口径收住。
5. 最后单独修 **frontstage action cue 正式合同**，把当前轻量可读性补强升级成稳定 contract。

## 2. PR 分组总览

| PR | 主题 | 归并来源 | 工作量 | 前置依赖 | 结论 |
| --- | --- | --- | --- | --- | --- |
| `PR-01` | 体验 Gate 与关键路径 Pacing 硬化 | `P0-1` + `P1-4` | `M`（`2~4` 人日） | 无 | 第一优先级，先修判断口径和主线平谷 |
| `PR-02` | Build Identity、Dynamic Loot 与 Profession Capstones | `P0-2` + `P1-5` + `P2-1` + `P2-3` | `L`（`5~8` 人日） | `PR-01` | 最大收益 PR，直接决定 Phase4 后半段是否还有 chase 动力 |
| `PR-03` | Organic Hidden Loop、Secret Reward Authority / Identity 与 Replay Hook 内容化 | `P0-3` + `P1-3` | `M-L`（`4~6` 人日） | `PR-01`，建议在 `PR-02` 后执行 | 让 hidden 不再是假绿，同时把 secret reward authority 收成单一真源，把 replay hook 从布局差异升级成内容记忆点 |
| `PR-04` | Boss Phase Identity 与 Version Discipline | `P1-1` + `P1-2` | `M`（`3~5` 人日） | `PR-01`，建议在 `PR-02/03` 后执行 | 给终盘补阶段语言，给 pack 边界补正式版本口径 |
| `PR-05` | Frontstage Action Cue Formal Contract | `P2-2` | `M`（`3~5` 人日） | `PR-03`，建议在 `PR-04` 后执行 | 把 frontstage 从“轻量 patch 式优先级”升级成正式 typed cue contract，避免长期退化成白名单堆叠 |

## 3. 为什么这样分

### 3.1 不把问题按模块切，而按“体验杠杆”切

如果按模块切，很容易得到：

1. 一个 `game` PR
2. 一个 `tools` PR
3. 一个 `client` PR

这种分法审起来方便，但体验上会割裂。  
当前 Phase4 的真正问题是：

1. 主线路径平谷
2. 奖励生态与职业身份不成立
3. hidden/replay hook 只有 correctness 没有 payoff，且 secret reward authority 仍有第二真源风险
4. boss 没把终盘体验托起来
5. frontstage 还停留在轻量补丁式 contract，长期会继续退化

所以 PR 需要围绕这些体验主题来分。

### 3.2 不把“纯小活”单独开 PR

例如：

1. version discipline 对齐
2. frontstage source priority
3. 文档 owner coverage 回写

这些都是真问题，但单独开 PR 会太碎，收益也不高。  
因此统一并入它们各自真正服务的主题 PR。

唯一例外是 **frontstage action cue typed contract**。  
它已经不再是“补一个排序规则”这么小，而是会引入新的 snapshot/action 抽象族，并横跨 `game -> core snapshot -> client`。继续把它塞进 `PR-04`，会让 `boss phase` 和 `frontstage contract` 这两套抽象在一个 PR 里互相污染。

### 3.3 先修口径，再修内容

当前 `organic hidden`、reward overlap、critical-path pacing 都存在“体验上有问题，但现有 gate 不一定直接红”的情况。  
因此 `PR-01` 必须先做，否则后面 4 个 PR 仍会在旧口径下反复解释。

## 4. 执行顺序

推荐顺序：

1. [2026-04-16-phase4-v3-pr-01-experience-gate-and-critical-path-pacing.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-01-experience-gate-and-critical-path-pacing.md)
2. [2026-04-16-phase4-v3-pr-02-build-identity-dynamic-loot-and-profession-capstones.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-02-build-identity-dynamic-loot-and-profession-capstones.md)
3. [2026-04-16-phase4-v3-pr-03-organic-hidden-loop-secret-reward-identity-and-replay-hook.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-03-organic-hidden-loop-secret-reward-identity-and-replay-hook.md)
4. [2026-04-16-phase4-v3-pr-04-boss-phase-identity-and-version-discipline.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-04-boss-phase-identity-and-version-discipline.md)
5. [2026-04-16-phase4-v3-pr-05-frontstage-action-cue-formal-contract.md](/Users/luo/Documents/github/K-ToME/docs/review/phase4/v3/PR/2026-04-16-phase4-v3-pr-05-frontstage-action-cue-formal-contract.md)

## 5. 全局约束

1. 后续所有 PR 默认先跑 `./gradlew verifyChanged`。
2. 默认联合验收固定为：命中的 owner task + `./gradlew verifyOwner` + `./gradlew phase4Report`。
3. canonical 输出路径固定为 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`。
4. 不再接受“只贴 legacy `phase4-summary`”作为完成证据。
5. 除非 PR 本身就是 release/build discipline 相关，否则不新增新资源族、不新开大 UI 系统、不引入 Phase5 系统。

## 6. 不在这轮 PR 里的内容

以下内容不进入本轮 5 个 PR：

1. Tactical AI 深化
2. Replay 产品化 / Run History / Death Analysis
3. 更大规模 zone 扩张
4. 更完整 content-pack 生态
5. 多语言扩展

这些都应留在 Phase5 或更后。

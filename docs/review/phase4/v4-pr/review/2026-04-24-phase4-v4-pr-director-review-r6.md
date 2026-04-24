# Phase4 v4 PR 开发文档 Director Review R6

**审阅对象**: `docs/review/phase4/v4-pr/` 下 PR-00 ~ PR-07 共 8 份开发文档
**对照基线**: R1–R5 历次 Director Review，重点对齐 R5 提出的 P2-01 ~ P3-04
**审阅日期**: 2026-04-24
**结论**: 总体可进入实施排期；仅存 5 条 P3 级细节与 1 条说明级口径润色，建议在进入实施前一轮文档微调中解决。

---

## 1. R5 遗留问题核验

R5 列出的 9 项修改建议基本完成。抽样验证如下：

| R5 问题 | 状态 | 证据 |
| --- | --- | --- |
| P2-01 README owner suite 缺 `reportPhase4Only` / PR-07 缺 `:game:test` | 已解决 | README 第 96~104 行 owner suite 表已补齐 `reportPhase4Only`、PR-07 补 `:game:test` |
| P2-02 PR-07 `samplePackContentPlayerVisibilityRate.reportOnly` 名称与阈值口径冲突 | 已解决 | PR-07 §5.3 第 247~253 行补出事件来源 / 分子 / 分母 / 聚合公式 / warning floor 五要素，阈值 `100% active sample fixed-seed runs with >= 1 runtime touch` |
| P2-03 PR-07 fixture id 命名 `_` vs `.` 跨范围 | 已解决 | PR-07 §5.1 第 149~150 行写明 `_` 约定只适用于本次 sample pack 的对照 fixture，不改其他 fixture 命名规范 |
| P2-04 PR-05 `BossPhaseEvaluationContext` 边界 | 已解决 | PR-05 §5.1 第 153~154 行新增 `activeTriggerIds: Set<String>`，并严格禁止 core 读 zone hook / YAML / registry |
| P3-01 PR-05 `bossVariantPhaseOverrideActionDistinctCount.reportOnly` 定义缺项 | 已解决 | PR-05 §7 第 322~326 行补出事件来源 / 聚合公式 / 分母 / warning floor / metricKind / failSemantics 全量 |
| P3-02 PR-05 完成标准未覆盖 telegraph coverage | 已解决 | PR-05 §6.1、§7 均登记 `bossVariantPhaseOverrideTelegraphCoverage=3/3` 阈值与分母口径 |
| P3-03 PR-04 owner suite 缺 `reportPhase4Only` | 已解决 | README §7 PR-04 owner suite 已补齐 |
| P3-04 PR-05 `variantSlug` 缺定义 | 已解决 | PR-05 第 147 行定义为 "去掉 `boss.variant.` 前缀后的稳定 slug" |

结论：R5 主体修订已落地。R6 在此基础上做细颗粒度复查，发现的问题全部为 P3 级。

---

## 2. R6 新发现问题

### P3-01 PR-01 `learnedTalentChoiceEventRate` 分母口径未显式化

- 位置：`2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md`
  - §5.5 第 285 行：`learnedTalentChoiceEventRate` 只写分子（"只统计 rank 0 -> 1 的非 starter talent"）。
  - §7 第 434 行表格阈值 `>= 90% terminal runs`。
- 问题：同一段 §5.5 第 283 行对 `breakpointChoiceEventRate` 已显式写出 "分母是 terminal run 中至少出现一次 breakpoint preview 的 run；分子是同一 run 中至少确认一次 breakpoint 事件的 run"，口径完备；但 `learnedTalentChoiceEventRate` 只给分子，分母只能从表格 `terminal runs` 反推。
- 风险：实现可能把 "完成 run 的数量" 与 "进入 level 2 以上的 run 数" 作为两种分母，baseline 与 report 对不上。
- 建议：在 §5.5 第 7 条补一句 "分母固定为所有 terminal run；分子是该 run 中至少触发一次 rank 0 → 1 非 starter learn 的 run"，与 breakpointChoiceEventRate 同级别。

### P3-02 PR-01 Tier 2 门槛中 "职业总投入 >= 3" 语义冗余

- 位置：`2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md` §5.4 第 247 行。
- 现状：Tier 2 条件列为 "同树投入 >= 2，其他任一职业树投入 >= 1，职业总投入 >= 3"。
- 问题：在前两项同时成立的前提下，职业总投入最小值天然等于 `2 + 1 = 3`；第三项永远由前两项推得。冗余条目有两种被误读风险：
  1. 将 "总投入 >= 3" 理解为额外要求（例如需要第三棵树投入），与 §5.2 "三列树" 叙述冲突；
  2. data loader 实现者把它写成独立门槛，日后删除同树 / 副树约束时规则失准。
- 建议：删除 "职业总投入 >= 3"，或改写为显式解释：`(此等价于前两项最小和，保留仅为对 data loader 的可读性说明)`。

### P3-03 PR-03 `lateCommonPenalty` 对 rogue 非 OFF_HAND 无显式处理

- 位置：`2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md` §5.5 第 269~271 行。
- 现状：
  1. `rogue` level `>= 5` 的 OFF_HAND COMMON 直接从 shortlist 排除；
  2. "其他职业" level `>= 5` 的任意 COMMON 施加 `lateCommonPenalty`。
- 问题：按字面读法，rogue level `>= 5` 的 **非 OFF_HAND** COMMON（例如 ACCESSORY / ARMOR / CONSUMABLE_OR_UTILITY / WEAPON COMMON）**既不排除、也不降权**。与 §1 完成标准 "milestone reward `notAdopted` 下降" 的意图不一致。
- 建议：把规则改写为对称形式，例如：
  > "所有职业 level `>= 5` 的 milestone COMMON candidate 施加 `lateCommonPenalty`；其中 rogue 的 OFF_HAND COMMON 额外升级为 shortlist 排除。"
- 补充：`rewardScoreBreakdownSamples` 也应新增一条 "rogue ARMOR COMMON 被 lateCommonPenalty 降权" 的样本证据，覆盖非 OFF_HAND 路径。

### P3-04 PR-06 `route_probe` / `late_route_probe` 种子未列入 corpus 表

- 位置：`2026-04-24-phase4-v4-pr06-long-run-route-diversity.md` §5.1 第 99~125 行。
- 现状：种子表仅列出 12 条 `full_route` + 4 条 `branch_inclusive` 共 16 条；第 122 行声明 "`route_probe` 固定为 2 条、`late_route_probe` 固定为 2 条"；第 125 行注明 "保持现有数量"。
- 问题：
  1. "保持现有" 表述对外部读者不友好，没有指明现有两组 probe 的 seed、zone 与 intent；
  2. 若后续有人重建 corpus（如 Phase5），会缺失 probe 种子传承证据；
  3. 与 §1 完成标准 "verifyChanged 只路由最小 affected subset" 的论述脱钩——读者需要 probe intent 才能评估 routing 粒度。
- 建议：在 §5.1 种子表末尾补两行 `route_probe` / `late_route_probe` 固定 seed + route intent，或单独列一个 "probe corpus" 小表，并明确 probe 不进入 `branchInclusiveCount` 分母。

### P3-05 PR-00 `launch-packaged-app.sh` 的 JAVA_TOOL_OPTIONS 口径两处不一致

- 位置：`2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
  - 完成标准第 27 行：要求 `JAVA_TOOL_OPTIONS="-Duser.home=<runtime-home> -Dktome.validation.scenario=<scenario-id>"`（仅 2 个属性）；
  - §5.4 生成规则第 302 行：要求 launch script "必须包含 `JAVA_TOOL_OPTIONS`、`ktome.validation.scenario`、`ktome.whitebox.root`、`ktome.whitebox.evidenceDir`、`ktome.whitebox.manualRecord`"（合计 5 项）。
- 问题：两段没有说明后三个 property 是放进 `JAVA_TOOL_OPTIONS` 同一行，还是作为 app 参数 / 独立 `-D`。实现者可能产生：
  1. 把 5 项全塞进 `JAVA_TOOL_OPTIONS`；
  2. 仅前 2 项在 `JAVA_TOOL_OPTIONS`，其余作为独立 JVM flag；
  3. 后 3 项作为 shell 环境变量，由 packaged app 读取。
  三种实现会让 `ValidationScenarioBootstrap` 的 system property 读取路径分叉。
- 建议：在 §5.4 补一行合同，例如：
  > "launch script 必须把全部 5 项写为独立 `-D` 并拼入 `JAVA_TOOL_OPTIONS`；除 `JAVA_TOOL_OPTIONS` 外不得再以命令行参数或 shell 环境变量形式传递这些 key。"
  同时将第 27 行完成标准的示例扩成 5 项，两处完全对齐。

### P3-06（说明级）PR-05 `variantSlug` 可补一条合法字符断言

- 位置：PR-05 §5.1 第 147 行。
- 现状：定义 "variantSlug 是去掉 `boss.variant.` 前缀后的稳定 slug"。
- 建议：可在 fail-fast 校验中追加 "`variantSlug` 必须匹配 `[a-z][a-z0-9_]*`"，防止后续 variant id 引入大写或连字符时 `CoreEventBus` key 碰撞。
- 优先级：非阻塞，实施期再决定。

---

## 3. 需求对齐矩阵（P0-P2 范围）

| 需求层 | 来源 | 文档落地 | 评价 |
| --- | --- | --- | --- |
| release classification 四分 BASE / 两分 ADVANCED / 两分 FROZEN | README §2.2 | PR-01、PR-03、PR-04、PR-07 均输出 `includedProfessions` / `advancedReportOnlyProfessions` / `excludedFrozenProfessions` | 通过 |
| schemaVersion v2 cutover fail-fast | PR-07 §5.2 | 删除 v1、无 dual-read / compat alias / legacy shim | 通过 |
| BossPhaseEvaluationContext 仅消费 semantic trigger id | PR-05 §5.1 | 新增 `activeTriggerIds: Set<String>`；core 不读 zone hook / YAML / registry | 通过 |
| owner metric cutover: `.reportOnly` → blocking | PR-03 §5.6 | `professionCapstoneAdoptionFloor`、`nonWeaponBuildPayoffFloor` 同步 catalog / aggregation / baseline registry；不保留旧 id | 通过 |
| content pack 主路径 ADD-first | PR-07 §5.1 | 5 条 ADD 主路径；REPLACE / precedence 留在 fixture | 通过 |
| `PHASE4_V4_FAST` scenario 一键进入目标态 | PR-00 §5.3、§5.6 | PR-01~PR-07 均接入 scenario id、prepare-primary-scene / prepare-secondary-scene / show-evidence-summary | 通过 |
| canonical report 单源 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}` | README §2 与 PR-03 / PR-05 / PR-06 | owner metric catalog、aggregation manifest、baseline registry 三端强制一致 | 通过 |

---

## 4. 功能与系统一致性检查

1. **命名闭环**：全部 PR 的 stable diagnostic token、sealed object、trigger id、metric id、i18n key 在文档内互相引用一致。PR-02 `RequiresReplacementTarget` / `shop.purchase.requires_replacement_target` 两个命名空间并存且对应关系清楚。
2. **owner 三件套（catalog / targets / baseline registry）**：PR-01、PR-03、PR-04、PR-05、PR-06、PR-07 新增 metric 均显式登记，aggregation manifest 同步更新；不存在 "只登记部分字段" 的半接线。
3. **producer 单点所有权**：longRunLab / whiteBoxContentPack / bossHarness / hiddenContentHarness 各自负责各自 metric；同一 metric 未出现双 producer（PR-06 `verifyChanged` 仍是消费者而非 producer）。
4. **破坏性边界**：PR-01 与 PR-07 在文档中均显式写明 `INCOMPATIBLE_*` fail-fast 错误码；PR-06 的 corpus 重建不改 procgen / reward，不破坏既有 blocking semantics。
5. **UI 与玩家可见性**：PR-04 前台提示 TTL + dedup、PR-05 telegraph coverage、PR-07 main menu pack summary 都具备 `goldenScreenshot / clientSmoke` 覆盖声明，未留空白。

---

## 5. 玩法与体验审查

1. **PR-01** 通过 starter 3 / learnable 第 4 + Tier 门槛把 "学新 vs 升 rank" 的资源竞争置于玩家前 10 分钟，玩家 pacing 合理。
2. **PR-02** 铭文满槽后 `RequiresReplacementTarget` 与 `offerFingerprint` 组合解决了 "买时被其他窗口刷新" 的一致性，体验路径完整。
3. **PR-03** capstone × profession 权重矩阵 + lateCommonPenalty 可解释；但见 P3-03，rogue 非 OFF_HAND 的覆盖需显式化。
4. **PR-04** 5 个 runtime hook 对 zone noun 兑现有力，且 PR-05 复用其 `zone.trigger.oil_or_fire_seen`、`zone.trigger.void_pressure_active`，体验链条闭合。
5. **PR-05** 三个 variant 的 telegraph / action emphasis / coverage 三指标组合，把 "变体感" 做成玩家视觉 + 听觉 + 行动倾向三层证据，收口清楚。
6. **PR-06** 12 + 4 corpus 解决 top hash 单点，但见 P3-04，probe 种子缺失会对外部读者造成困扰。
7. **PR-07** sample pack ADD-first + pack-local hidden branch binding 让 pack 作者学到正确范式，玩家面可见 pack 摘要，路径完整。

---

## 6. 当前阶段必须解决的问题

无 P0 / P1 / P2 级问题。

建议在开工前一次性修订以下 5 条 P3：

1. PR-01 §5.5 补 `learnedTalentChoiceEventRate` 分母口径（本文 §2 P3-01）；
2. PR-01 §5.4 删除或注释 Tier 2 的 "职业总投入 >= 3"（§2 P3-02）；
3. PR-03 §5.5 把 `lateCommonPenalty` 改为所有职业对称、rogue OFF_HAND 额外 shortlist 排除（§2 P3-03）；
4. PR-06 §5.1 把 `route_probe` / `late_route_probe` 种子与 intent 列入表（§2 P3-04）；
5. PR-00 完成标准与 §5.4 的 `JAVA_TOOL_OPTIONS` 口径对齐至 5 项（§2 P3-05）。

这五条全部属于开发文档的文字口径，不改变任何接口、指标阈值或破坏性边界，可在单次文档 PR 内完成。

---

## 7. 删除 / 演进计划检查

1. R3 曾提出 "合并 PR-06 与 PR-04 harness corpus"，R5 已否决，理由是 producer 单一性。本 R6 维持否决。
2. R4 曾提出 "将 `samplePackAddOnlyMainPath` 升级为 owner gate"，PR-07 §5.3 明确由 `manifestLint` 承担 fail-fast，保持 `supporting`。R6 同意。
3. R2 曾建议 "PR-02 热键区间扩展到 `9`"，PR-02 §5.3 第 279 行已保留 `9` 供数字栏独立输入，本决策维持。

无需新增删除项。

---

## 8. 追加建议（非阻塞）

1. **R7 检查点**：在 PR-03 实现 PR 提交前，建议用 `rewardScoreBreakdownSamples` 的 20 条 / run 抽样验证 `topFiveAffixExposureShare` 与单项 ≤15% 的交集是否稳定，避免上线后 baseline 频繁抖动。
2. **PR-00 执行性**：建议在 PR-00 实施时顺带把 `build/whitebox/<scenario>/expected-evidence.json` 的 JSON schema 单独登记为一个小 test，防止后续 PR 漏写证据条目。
3. **PR-06 routing 观测**：建议把 `zoneRouteHashDiversity.probeRouteHashSample` 展开成 list-with-seed，便于 Phase5 replay 继承对齐。
4. **PR-05 `variantSlug` 断言**（§2 P3-06）：可在 `DataLoader.parseBossVariants` 加 `[a-z][a-z0-9_]*` 断言，零成本提升稳健性。

---

## 9. 建议验证顺序

若上述 5 条 P3 均完成文档修订，则按以下顺序进入实施：

1. PR-00（验证基础，所有其他 PR 的前置）；
2. PR-01（玩法根基 + owner baseline 新增）；
3. PR-02（依赖 PR-01 talent 改造口径）；
4. PR-03（依赖 PR-01 / PR-02 的 learnable + 铭文选择事件进入 long-run）；
5. PR-04（hidden search runtime 与 zone trigger facts）；
6. PR-05（依赖 PR-04 的 zone trigger）；
7. PR-07（依赖 PR-04 的 secondary secret slot capacity、schema v2 cutover）；
8. PR-06（读取 PR-03 / PR-04 / PR-07 的最终 owner 字段）。

与 README §1 的固定顺序一致。

---

## 10. 结论

本批 8 份 PR 文档在经历 R1~R5 的迭代后，结构、范围、指标、白盒流程、owner 三件套已经稳定可实施。R6 仅发现 5 条 P3 级文字口径问题 + 1 条说明级补强。建议在实施前一轮文档微调后，按 README 顺序进入开发排期。

本报告结论为：**可进入实施排期，仅需先完成第 6 节列出的 5 条 P3 修订。**

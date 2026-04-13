# Phase 4 Roadmap（入口）

## 1. Phase 4 主题

Phase 4 的主题是把“可完成的长局”升级成“重复游玩差异明显的长局”，同时把内容扩展点做成正式数据包路径。

目标主线：

`长局成立 -> ProcGen 深化 -> Loot 生态深化 -> Hidden Content 成立 -> Content Pack Overlay`

## 2. 检查点摘要

| checkpoint | 主题 | 最小交付 | 进入条件 | 退出条件 |
| --- | --- | --- | --- | --- |
| `P4-X` | 横切合同收口 | save/replay、reward/threat ledger、`SearchAction`、pack runtime/test split | `Phase 3` 出口满足 | `PR-01 ~ PR-09` 可共享同一套横切 contract |
| `P4-A` | ProcGen 与可解性 | `MapgenPipeline`、`TerrainTag`、`SolvabilityGraph`、mapgen/solvability harness | `Phase 3` 出口满足 | 多 seed 地图差异明显且主线 `100%` 可达 |
| `P4-B` | Loot 与遭遇生态 | `LootBudget`、affix/unique/artifact、elite mutation、hidden event、secret zone | `P4-A` contract 稳定 | 掉落与遭遇差异可感知且可量化 |
| `P4-C` | Content Pack | `ContentPackManifest`、overlay、schema lint、headless harness、示例 pack + precedence fixture | `P4-B` schema 稳定 | 外部 pack 能以 `ADD + whole-entry REPLACE` 主路径在不改 `core` 的前提下装载并验证 |

## 3. 并行开发线

1. `Rules Lane`
   - `MapgenPipeline`
   - `SolvabilityGraph`
   - `LootBudget`
   - `TerrainTag` 与 `CombatPipeline` 接线
2. `Content Lane`
   - biome family
   - vault / pattern room
   - affix / unique / artifact
   - elite mutation / hidden event / secret zone
3. `Client Lane`
   - elite mutation 可读性
   - hidden entrance / secret zone 表现
   - pack 内容可见性验证
4. `Tools/QA Lane`
   - `mapgenSmoke`
   - `solvabilityHarness`
   - `whiteBoxMapgen`
   - `whiteBoxSolvability`
   - `whiteBoxVerify`
   - `lootBalanceLab`
   - `whiteBoxLoot`
   - `terrainInteractionBatch`
   - `bossHarness`（沿用 Phase 3 主 harness，扩展 variant/threat 汇总）
   - `hiddenContentHarness`
   - `organicHiddenProbe`
   - `longRunLab`（沿用 Phase 3 主 harness，补终盘 build identity owner metric）
   - `whiteBoxHiddenContent`
   - `contentPackHarness`
   - `whiteBoxContentPack`
   - `phase4Report`（artifact-only 聚合当前 `mapgenSmoke / solvabilityHarness / hiddenContentHarness / organicHiddenProbe / contentPackHarness / bossHarness / longRunLab / terrainInteractionBatch / whiteBoxMapgen / whiteBoxSolvability / lootBalanceLab / whiteBoxLoot / whiteBoxHiddenContent / whiteBoxContentPack` 的既有 artifact，并正式收口 hidden / loot / terrain / terminal identity owner metric）

## 4. 工作包与依赖

| 工作包 | checkpoint | 依赖 | 说明 |
| --- | --- | --- | --- |
| `P4-X` | `P4-X` | `Phase 3` | save/replay、reward/threat、`SearchAction`、pack runtime/test metadata 横切合同 |
| `P4-W1` | `P4-A` | `Phase 3` | room / loop / pattern room / vault / biome family |
| `P4-W2` | `P4-A` | `P4-W1` | lock-key DAG / hidden entrance / 可解性验证 |
| `P4-W3` | `P4-B` | `Phase 3` | `LootBudget` / affix cost / `LootBalanceLab` |
| `P4-W4` | `P4-B` | `P4-W2`, `P4-W3` | elite mutation / Boss variant / hidden content |
| `P4-W5` | `P4-C` | `P4-W3`, `P4-W4` | overlay / content pack / lint / harness |

执行原则：

1. `P4-X` 必须先冻结，再进入 `PR-01 ~ PR-09` 的正式实现稿。
2. `P4-W1` 与 `P4-W3` 可在 `P4-X` 收口后并行。
3. `P4-W2` 不允许在没有 `TopologyGraph` 和 `SearchAction` contract 的情况下先写局部例外逻辑。
4. `P4-W4` 的奖励、事件和 secret zone 不得绕过 `LootBudget / FloorRewardBudget` 与 `SolvabilityGraph`。
5. `P4-W5` 的 loader / lint 可以在 `P4-X + P4-W3` 稳定后起步，但 headless harness 依赖 `P4-W4` 的 hidden content registry 完整。
6. `PR-06` 的 terrain/mutation/boss variant 可以在 `PR-02` 完成后并行推进；验证主线优先挂在 `terrainInteractionBatch + bossHarness`，`hiddenContentHarness` 只消费其结果。
7. `P4-C` 以最小可行切片为目标：runtime 正式支持 `ADD + whole-entry REPLACE`；sample pack 以 `ADD` 为主验证路径，precedence / conflict 由第二夹具覆盖；`APPEND / DENY` 默认停在 fixture/lint 层，除非后续单独冻结 allowed-target 表。

### PR 级执行文档落位

| PR | 对应工作包 | 目标 |
| --- | --- | --- |
| `PR-01` | `P4-W1a` | `MapgenPipeline` contract、`TerrainTag`、BSP 兼容适配、`mapgenSmoke` baseline |
| `PR-02` | `P4-W1b` | 混合拓扑 planner、pattern / vault / biome family、zone mapgen profile |
| `PR-03` | `P4-W2` | `SolvabilityGraph`、hidden entrance、proof golden、`solvabilityHarness` |
| `PR-04` | `P4-W3a` | `LootBudget` / `RarityTier` contract、旧 `ItemQuality` 过渡桥 |
| `PR-05` | `P4-W3b` | affix cost、`UNIQUE / ARTIFACT` 模板、`lootBalanceLab`、资源计划 |
| `PR-06` | `P4-W4a` | 地形交互、elite mutation、Boss variant、资源计划 |
| `PR-07` | `P4-W4b` | hidden event、secret zone、client 可读性、`hiddenContentHarness`、资源计划 |
| `PR-08` | `P4-W5a` | content pack overlay loader、pack lint、`contentPackHarness` |
| `PR-09` | `P4-W5b` | 示例 pack、pack 资源管线、client 可见性验证 |

## 5. 进入与退出摘要

进入 `Phase 4` 前：

1. `Phase 3` 长局、Boss harness、status/talent/build contract 全部稳定。
2. `CombatPipeline`、`ElementInteractionRule`、`BossEncounterDef`、`ProfileData` 已冻结。

`Phase 4` 退出时：

1. mapgen 与 solvability harness 稳定。
2. loot 预算和阶段匹配通过实验室验收。
3. elite mutation、hidden event、secret zone 都可被自动化与白盒验证。
4. 示例 content pack 可在不改 `core` 的前提下装载和验证。
5. 差异性域的白盒验证和 AI 可读报告合同统一收口到项目级 white-box framework。

## 6. 文档索引

1. [2026-03-13-phase4-procgen-loot-and-content-pack.md](./2026-03-13-phase4-procgen-loot-and-content-pack.md)
2. [2026-03-13-phase4-cross-cutting-contracts.md](./2026-03-13-phase4-cross-cutting-contracts.md)
3. [2026-03-13-phase4-verification-checklist.md](./2026-03-13-phase4-verification-checklist.md)
4. [../2026-04-04-unified-white-box-verification-framework.md](../2026-04-04-unified-white-box-verification-framework.md)
5. [2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md](./2026-03-13-phase4-pr-01-mapgen-contract-and-smoke-baseline.md)
6. [2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md](./2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md)
7. [2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md](./2026-03-13-phase4-pr-03-solvability-graph-hidden-entrance-and-harness.md)
8. [2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md](./2026-03-13-phase4-pr-04-loot-budget-v2-and-rarity-pipeline.md)
9. [2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md](./2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md)
10. [2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md](./2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md)
11. [2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md](./2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md)
12. [2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md](./2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md)
13. [2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md](./2026-03-13-phase4-pr-09-sample-content-pack-and-pack-resource-pipeline.md)
14. [../2026-03-13-phase2-to-phase5-final-roadmap.md](../2026-03-13-phase2-to-phase5-final-roadmap.md)
15. [../2026-03-13-core-systems-design-and-phase-supplements.md](../2026-03-13-core-systems-design-and-phase-supplements.md)

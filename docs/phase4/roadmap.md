# Phase 4 Roadmap（入口）

## 1. Phase 4 主题

Phase 4 的主题是把“可完成的长局”升级成“重复游玩差异明显的长局”，同时把内容扩展点做成正式数据包路径。

目标主线：

`长局成立 -> ProcGen 深化 -> Loot 生态深化 -> Hidden Content 成立 -> Content Pack Overlay`

## 2. 检查点摘要

| checkpoint | 主题 | 最小交付 | 进入条件 | 退出条件 |
| --- | --- | --- | --- | --- |
| `P4-A` | ProcGen 与可解性 | `MapgenPipeline`、`TerrainTag`、`SolvabilityGraph`、mapgen/solvability harness | `Phase 3` 出口满足 | 多 seed 地图差异明显且主线 `100%` 可达 |
| `P4-B` | Loot 与遭遇生态 | `LootBudget`、affix/unique/artifact、elite mutation、hidden event、secret zone | `P4-A` contract 稳定 | 掉落与遭遇差异可感知且可量化 |
| `P4-C` | Content Pack | `ContentPackManifest`、overlay、schema lint、headless harness、示例 pack | `P4-B` schema 稳定 | 外部 pack 能在不改 `core` 的前提下装载并验证 |

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
   - `lootBalanceLab`
   - `hiddenContentHarness`
   - `contentPackHarness`

## 4. 工作包与依赖

| 工作包 | checkpoint | 依赖 | 说明 |
| --- | --- | --- | --- |
| `P4-W1` | `P4-A` | `Phase 3` | room / loop / pattern room / vault / biome family |
| `P4-W2` | `P4-A` | `P4-W1` | lock-key DAG / hidden entrance / 可解性验证 |
| `P4-W3` | `P4-B` | `Phase 3` | `LootBudget` / affix cost / `LootBalanceLab` |
| `P4-W4` | `P4-B` | `P4-W2`, `P4-W3` | elite mutation / Boss variant / hidden content |
| `P4-W5` | `P4-C` | `P4-W3`, `P4-W4` | overlay / content pack / lint / harness |

执行原则：

1. `P4-W1` 与 `P4-W3` 可并行。
2. `P4-W2` 不允许在没有 `TopologyGraph` 的情况下先写局部例外逻辑。
3. `P4-W4` 的奖励、事件和 secret zone 不得绕过 `LootBudget` 与 `SolvabilityGraph`。
4. `P4-W5` 的 loader / lint 可以在 `P4-W3` schema 稳定后起步，但 headless harness 依赖 `P4-W4` 的 hidden content registry 完整。

## 5. 进入与退出摘要

进入 `Phase 4` 前：

1. `Phase 3` 长局、Boss harness、status/talent/build contract 全部稳定。
2. `CombatPipeline`、`ElementInteractionRule`、`BossEncounterDef`、`ProfileData` 已冻结。

`Phase 4` 退出时：

1. mapgen 与 solvability harness 稳定。
2. loot 预算和阶段匹配通过实验室验收。
3. elite mutation、hidden event、secret zone 都可被自动化与白盒验证。
4. 示例 content pack 可在不改 `core` 的前提下装载和验证。

## 6. 文档索引

1. [2026-03-13-phase4-procgen-loot-and-content-pack.md](./2026-03-13-phase4-procgen-loot-and-content-pack.md)
2. [2026-03-13-phase4-verification-checklist.md](./2026-03-13-phase4-verification-checklist.md)
3. [../2026-03-13-phase2-to-phase5-final-roadmap.md](../2026-03-13-phase2-to-phase5-final-roadmap.md)
4. [../2026-03-13-core-systems-design-and-phase-supplements.md](../2026-03-13-core-systems-design-and-phase-supplements.md)

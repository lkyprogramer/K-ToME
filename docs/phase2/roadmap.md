# Phase 2 Roadmap（入口）

## 1. Phase 2 主题

Phase 2 的主题不是继续堆内容，而是先把 Phase 1 的最小可玩主线迁移到可长期演进的语义合同上，再建立最小 Tile、最小双语言、最小资源管线闭环。

目标主线：

`Phase 1 可玩 MVP -> 语义合同重建 -> Tile/i18n 正式路径 -> 4 职业短局`

## 2. 文档索引

1. [2026-03-13-phase2-semantic-contracts-tile-and-i18n.md](./2026-03-13-phase2-semantic-contracts-tile-and-i18n.md)
2. [2026-03-13-phase2-verification-checklist.md](./2026-03-13-phase2-verification-checklist.md)
3. [2026-03-13-phase2-pr-01-serialization-and-version-discipline.md](./2026-03-13-phase2-pr-01-serialization-and-version-discipline.md)
4. [2026-03-13-phase2-pr-02-core-semantic-contracts.md](./2026-03-13-phase2-pr-02-core-semantic-contracts.md)
5. [2026-03-13-phase2-pr-03-locale-and-schema-v2.md](./2026-03-13-phase2-pr-03-locale-and-schema-v2.md)
6. [2026-03-13-phase2-pr-04-snapshot-and-manifest.md](./2026-03-13-phase2-pr-04-snapshot-and-manifest.md)
7. [2026-03-13-phase2-pr-05-minimal-tile-shell.md](./2026-03-13-phase2-pr-05-minimal-tile-shell.md)
8. [2026-03-13-phase2-pr-06-minimal-official-slice.md](./2026-03-13-phase2-pr-06-minimal-official-slice.md)
9. [2026-03-13-phase2-pr-07-short-run-expansion.md](./2026-03-13-phase2-pr-07-short-run-expansion.md)
10. [2026-03-20-phase2-pr-07-post-review-execution-plan.md](./2026-03-20-phase2-pr-07-post-review-execution-plan.md)
11. [../2026-03-13-phase2-to-phase5-final-roadmap.md](../2026-03-13-phase2-to-phase5-final-roadmap.md)
12. [../2026-03-13-core-systems-design-and-phase-supplements.md](../2026-03-13-core-systems-design-and-phase-supplements.md)
13. [../2026-03-13-art-style-bible.md](../2026-03-13-art-style-bible.md)

## 3. 执行权威

1. `P2-W1 ~ P2-W7` 是 Phase 2 的唯一执行编号。
2. [最终路线图](../2026-03-13-phase2-to-phase5-final-roadmap.md) 负责阶段切分、工作包、预算和出口门禁。
3. [核心系统详细设计与阶段补充文档](../2026-03-13-core-systems-design-and-phase-supplements.md) 负责公式、数据结构和系统内部合同。
4. 旧 `PR01 ~ PR12` 编号只作历史参考，若与 `W1 ~ W7` 冲突，一律以 `W1 ~ W7` 为准。
5. 详细覆盖关系见 [docs/INDEX.md](../INDEX.md)。

## 4. 当前检查点真相

1. 当前主线应描述为 `P2-B 已成立 / P2-C 未完成`，而不是“Phase 2 已完成”。
2. [2026-03-13-phase2-pr-06-minimal-official-slice.md](./2026-03-13-phase2-pr-06-minimal-official-slice.md) 对应的最小官方切片已经进入稳定基线；后续只允许 bugfix，不允许为了 `P2-C` 目标返工其设计真相。
3. 当前真正阻塞 `P2-C` 的是：职业正式化、真实短局 route、`24 怪 + 24 物品` 内容下限、gate 重对齐，以及 Visual/Audio formal path 收口。
4. `PR-07` 的直接执行顺序以 [2026-03-20-phase2-pr-07-post-review-execution-plan.md](./2026-03-20-phase2-pr-07-post-review-execution-plan.md) 为准；后续 sprint/PR 拆分都按该文档推进。

## 5. P2-C 资源真相基线

以下表格是 `P2-C` 当前资源状态的入口摘要；详细解释、数量口径与 gate 要求仍以执行版文档为准。

当前需要用两条口径同时看：

1. formal-path required key 当前已经做到 `visual = 0 missing_visual`、`audio = 0 silence.ogg`
2. `phase2` 剩余 debug budget 当前只保留 fallback sentinel 自身：`1` 条 visual placeholder（`missing_visual`）与 `1` 条 audio silence（`audio.fallback.silence`）；这些不再与 required blocker 混算，但必须被显式统计

| 资源域 | 已有正式资产 | 已有 spec / manifest 但未收口 | 当前仍是 placeholder / silence |
| --- | --- | --- | --- |
| Profession | `actor.*`、四职业 portrait、`icon.profession.*` 已正式化 | `Vanguard / Arcanist` 的 tree portrait / tree icon 仍待补齐 | 职业主入口不再命中 silence；剩余缺口集中在旧树视觉 |
| Zone | `4 zone` 的 `visual / icon`、route prop、ambient/cue 已正式化 | route 主路径无新增 formal blocker | zone 主路径当前无 placeholder/silence blocker |
| Boss | `bandit_captain` 与 `dungeon_lord` 的 actor / encounter visual / icon / cue 已进入正式主链 | 无新的 boss formal blocker | boss 主路径当前无 placeholder/silence blocker |
| Objective / Interactable | route objective / interactable 已进入 runtime 主链 | 非主路径 objective 可以继续走预算 | 当前主路径 objective/interactable 无 silence blocker |
| Reward / Item | `24` 物品矩阵、signature reward、以及当前 `affix / material` key 集都已进入正式主链 | 更细粒度的专属 affix/material 资产仍可留待 Phase 3/4 扩展 | 当前 Phase 2 item 体验已不再依赖 affix/material placeholder |
| Talent / Tree | `33` 个 talent skill icon/cue 与 `12` 棵 tree 的 `tree.* / icon.tree.*` 已进入正式主链 | 无新的职业树 formal blocker | visual budget 不再集中在 tree 主路径 |
| Core UI Cue | `confirm / cancel / hover / footstep / melee / spell / boss.warning`、`difficulty.normal` 与 route cue 已成型 | 仍需把 required key 与 debug budget 的统计口径固定到 lint 输出 | 非 formal 条目可保留预算，但不能再与完成态 blocker 混算 |

## 6. 执行 Lane

1. `Rules Lane`：`core`，负责资源、伤害、成长、状态和战斗合同。
2. `Content Lane`：`game`，负责 profession / monster / item / zone / objective / reward 正式内容。
3. `Client Lane`：`client`，负责 route 表现、HUD、golden 与 client smoke。
4. `Visual/Audio Lane`：`assets-src`、runtime manifest、scripts；这是独立 lane，不是“最后统一收尾”的附属任务。

## 7. 收口顺序

1. `Freeze The Truth`
2. `Repair Core Loop Foundations`
3. `Repair Combat And Growth Contracts`
4. `Formalize Rogue And Templar`
5. `Build The Real Zone Route`
6. `Reach The P2-C Content Floor`
7. `Realign The Gates`
8. `Formal Path Closure`

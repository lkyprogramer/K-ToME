# Phase3 深度审查报告（Part 1/4）

## 1. 执行摘要

- 当前 Phase3 不是“体验未闭环”，也不是“已经具备较强耐玩性”；更准确地说，它是一个“规则骨架基本完成、可以稳定通关，但构筑深度、探索新鲜感和奖励驱动明显不足”的版本。
- `combat / status / talent / ai / world / profile / harness` 的主骨架基本都在，说明工程交付不是空壳。
- 真正的问题不在“有没有系统”，而在“这些系统是否真的把 4~6 小时长局撑成了值得继续玩的游戏”。当前答案偏否定。
- `P3-W5 / P3-W6` 的内容密度明显低于 Phase3 路线图口径。路线图要求基础职业正式树达到 `4 x 16 = 64` 天赋、怪物模板 `60` 左右；当前数据侧基础职业只有 `44` 天赋，怪物模板只有 `26`。
- 世界长局合同在 schema 层完整，但 `completionRule / uniqueContentTag / specialMechanics / environmentTheme` 大量停留在“有字段、有校验、无运行时行为”的状态，属于典型伪完成。
- 奖励循环是当前版本最弱的一环。affix 系统主要作用在普通掉落，高价值节点如路线奖励、缓存奖励、Boss 奖励大多仍是按职业偏好挑选的固定白板 base item，缺少 build 爽点。
- 战斗系统本身可用，但敌方行动语义偏薄。`26` 个怪物里只有 `6` 个带 talent；后段三个 Phase3 Boss 里有两个共享同一套核心技能包，导致中后程战斗辨识度不足。
- `longRunLab / bossHarness / soloClearLab` 能跑通，但验证口径偏宽，无法可靠识别“太线性、太容易、太同质”的版本。`long-run-full` 12/12 全胜且同一路线 hash，就是明显信号。
- UI 的基础操作流已经成立，但世界地图仍直接向玩家暴露 `lv3_5 / MOVEMENT / CLEANSING` 这类内部 token，说明 Phase3 的玩家信息面还没收口到产品级。
- 结论：当前 Phase3 属于“基本可玩，但不够耐玩；系统完成度高于体验完成度；不适合直接无条件进入 Phase4”。

## 2. 审阅范围与依据

本次审阅同时参考了以下文档与实现：

- `docs/phase3/roadmap.md`
- `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
- `docs/phase3/2026-03-13-phase3-verification-checklist.md`
- `docs/phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md`
- `docs/phase3/2026-03-13-phase3-pr-02-status-lifecycle.md`
- `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
- `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
- `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
- `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`
- `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
- `docs/2026-03-13-core-systems-design-and-phase-supplements.md`
- `docs/mvp-development-guide.md`
- `docs/rule/kotlin.md`

重点核查了以下实现面：

- `core/src/main/kotlin/com/ktome/core/combat/*`
- `core/src/main/kotlin/com/ktome/core/status/*`
- `core/src/main/kotlin/com/ktome/core/talent/*`
- `core/src/main/kotlin/com/ktome/core/ai/*`
- `core/src/main/kotlin/com/ktome/core/world/*`
- `core/src/main/kotlin/com/ktome/core/profile/*`
- `core/src/main/kotlin/com/ktome/core/item/*`
- `core/src/main/kotlin/com/ktome/core/economy/*`
- `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt`
- `game/src/main/kotlin/com/ktome/game/GameModule.kt`
- `game/src/main/resources/data/*`
- `client/src/main/kotlin/com/ktome/client/render/*`
- `client/src/main/kotlin/com/ktome/client/screen/*`
- `game/src/test/kotlin/com/ktome/game/harness/*`
- `tools/src/test/kotlin/com/ktome/tools/golden/*`

本次还补跑了 Phase3 关键自动化入口：

- `./gradlew bossHarness longRunLab soloClearLab combatTraceGolden --rerun-tasks`

并核对了生成报告：

- `build/reports/harness/combat-trace-golden.md`
- `build/reports/harness/boss-harness.md`
- `build/reports/harness/solo-clear-lab.md`
- `build/reports/harness/long-run-summary.md`
- `build/reports/harness/long-run-full.md`

审阅方法：

- 先从 Phase3 权威文档抽取“承诺项”与阶段出口。
- 再建立“设计项 -> 代码/数据/资源/验证入口”的映射。
- 再从玩家体验链路重构一遍实际 Phase3：开局、成长、战斗、奖励、探索、长局推进、结算。
- 最后区分“文档一致但不好玩”“工程已做但体验未成立”“本阶段必须修”和“确实可后置”的问题。

## 3. Phase3 设计实现一致性矩阵

| 系统/模块 | 文档设计目标 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| `P3-W1` 战斗公式 V2 / `CombatResolutionTrace` | 冻结命中、暴击、`Power/Save`、抗性/穿透、`ApplicationPolicy`、trace golden | 已实现 | `docs/phase3/2026-03-13-phase3-pr-01-combat-formula-v2-and-trace-golden.md`; `core/src/main/kotlin/com/ktome/core/combat/*`; `build/reports/harness/combat-trace-golden.md` | 核心公式和 trace 都在，当前主要问题不在 W1 缺失，而在后续整体验证没有把这些规则转成更强的玩法差异 | Low |
| `P3-W2` 状态生命周期 / Effect Carrier / HUD | 冻结状态矩阵、carrier 顺序、净化、tick、HUD 语义 | 已实现 | `docs/phase3/2026-03-13-phase3-pr-02-status-lifecycle.md`; `core/src/main/kotlin/com/ktome/core/status/*`; `core/src/test/kotlin/com/ktome/core/status/*`; `client/src/main/kotlin/com/ktome/client/ui/status/*` | 基础契约完整，当前短板是内容利用率不高，不是状态系统本身缺失 | Low |
| `P3-W3` Talent Schema V2 / 动态说明 / Allocation Draft | typed effect op、语义说明、draft/respec/rollback、`telegraphRef` 引用 | 已实现 | `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`; `core/src/main/kotlin/com/ktome/core/talent/*`; `client/src/main/kotlin/com/ktome/client/ui/talent/*`; `game/src/main/kotlin/com/ktome/game/GameView.kt` | 基础设施完成，但 Phase3 内容层没有把这套系统吃满，导致“设施强于内容” | Medium |
| `P3-W4` AIProfile DSL / TelegraphSpec | 统一 AI DSL、selection policy、telegraph 单一权威、trace 可导出 | 部分实现 | `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`; `core/src/main/kotlin/com/ktome/core/ai/*`; `game/src/main/resources/data/ai/index.yaml`; `game/src/main/resources/data/telegraph/*.yaml` | DSL、telegraph、renderer 都在，但常规怪 AI 复用率偏高，且验证没有真正锚住 action-level AI trace | Medium |
| BossEncounter / BossPhase / BossTrace | Boss phase、预警、切相、副作用、trace 与提示一致 | 偏离实现 | `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`; `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`; `game/src/main/resources/data/bosses/index.yaml`; `game/src/main/resources/data/monsters/index.yaml`; `build/reports/harness/boss-harness.md` | 相位切换能跑，但 `molten_giant / dungeon_lord / abyssal_guardian` 的技能语义差异太小；`bossHarness` 报告中 `aiTraceCount=0` 仍可成功 | High |
| 基础职业正式树 | 4 基础职业、每职业 3 支线，路线图最低口径 `64` 天赋 | 偏离实现 | `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`（`4 x 16 = 64` 天赋）；`game/src/main/resources/data/talents/index.yaml` | 当前基础职业只有 `44` 个 talent：`vanguard 12 / arcanist 11 / rogue 11 / templar 10`，比路线图少 `20` 个 | Critical |
| 2 个可玩进阶职业 | `Berserker / Spellblade` 进入可玩路径；`Shadowblade / Warden` 冻结 contract | 部分实现 | `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`; `game/src/main/resources/data/professions/index.yaml`; `game/src/main/resources/data/talents/index.yaml`; `build/reports/harness/long-run-summary.md` | 两个可玩进阶职业存在，但每个只有 `6` 个 talent；`Spellblade` 当前 smoke 是“到达终局即可”，并未证明稳定可胜利 | High |
| 种族 / 铭文 / Profile 边界 | 3 playable race、2 frozen stub、铭文系统、正式解锁与开发态分离 | 部分实现 | `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`; `game/src/main/resources/data/races/index.yaml`; `game/src/main/resources/data/inscriptions/index.yaml`; `core/src/main/kotlin/com/ktome/core/profile/*` | 合同和功能都在，但 run-to-run 差异被一套高度统一的救火铭文包压平，且 frozen 选项直接暴露在玩家创建界面 | Medium |

Part 2 继续矩阵剩余系统，并进入玩法体验总评。

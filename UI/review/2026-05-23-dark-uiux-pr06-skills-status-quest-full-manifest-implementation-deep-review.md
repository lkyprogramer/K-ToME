# Dark UI/UX PR-06 Full Manifest 实现层深度审查 (2026-05-23)

**审查身份**：资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
**审查对象**：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md` 的当前实现交付
**审查范围**：当前分支 `codex/dark-uiux-pr06-skills-status-quest` 的代码、资源、脚本、测试与 manual records，对照 PR-06 close gate 合同
**前序文档**：`UI/review/2026-05-22-dark-uiux-pr06-skills-status-quest-full-manifest-design-director-review-round4.md`（Round 4 已修订 PR-06 文档本身）

## 元信息

| 字段 | 取值 |
| --- | --- |
| date | 2026-05-23 |
| branch | `codex/dark-uiux-pr06-skills-status-quest` |
| spec source | `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`（592 lines） |
| reviewer | Codex 设计总监审查 (Opus 4.7, max effort) |
| 审查模式 | implementation-level deep review，不重复 Round 1-4 的文档级合同审查 |
| 输入证据 | 当前分支代码、`UI/sprite-sheets/dark-v1-final-full-inventory.json`、`UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json`、`UI/sprite-sheets/key-registry.yaml`、`UI/manual-records/dark-uiux-pr06-*.md`、`tools/src/test/kotlin/...DarkSpriteSheetPipelineScriptTest.kt`、`scripts/generate_dark_final_full_inventory.py`、`scripts/verify_dark_manifest_coverage.py`、`tools/build.gradle.kts`、`client/src/main/kotlin/com/ktome/client/render/*.kt`、`client/src/main/kotlin/com/ktome/client/ui/status/*.kt`、`assets-src/image/manifests/phase2-visual-manifest.json`、`game/src/main/resources/data/professions/index.yaml` |

## 总体结论

**当前 PR-06 不满足 close gate 条件，应判定 `NOT_READY_TO_CLOSE`。**

判定要点：

1. **资源侧未交付**：`sheet-plan.yaml` 没有 `r08-skills-*` / `r09-status-damage` / `r09-quest-zone-profession` / `r09-fallback-debug` / `r09-rejected-polish` 任何一张 Round 8-9 sheet（grep 命中数 = 0）。`key-registry.yaml` 中 PR-06 相关 family（`icon.skill.*` / `icon.status.*` / `icon.profession.*` / `icon.quest.*` / `icon.mutation.*` / `icon.damage_type.*` / `icon.tree.*` / `talent.*` / `zone.*.icon`） **全部为 0 条**。这意味着 PR-06 §1 阶段目标 1-4（替换 skill/status/damage/quest/zone/profession/tree icon、完成 fallback/debug/missing/hidden 资源返修、把新视觉切到 `visual-manifest.json` 玩家可见 keys、把 PR-04 临时复用的职业树 icon 切换到新风格）**实质上未完成**。
2. **Gate 自报失败**：`UI/manual-records/dark-uiux-pr06-overview-screenshot.md` 已显式记录 `darkManifestCoverageLint final-full = FAIL`，原因是“final-full inventory has many keys missing from key registry; `icon.quest.objective_marker` is missing; `missing_visual` remains pending/rejected player-visible; old-style player-visible keys are non-empty”。该记录本身写明 `result = LIMITED_FAIL_PR06_CLOSE_GATE_NOT_MET`。
3. **客户端逻辑侧基本到位**：`StatusIconResolver` / `StatusPresentationModel` / `StatusHudPresenter` / `QuestSummaryIconResolver` / `ValidationOverlaySummaryPresenter` / `ValidationPackSummaryText` / `ValidationScenarioEvidenceSummaryLines` 与对应 client 测试基本符合 §6.2-§6.5 的契约，少数细节偏严或偏松（详见 P1）。
4. **工具/脚本侧基本到位**：`scripts/generate_dark_final_full_inventory.py` + `scripts/verify_dark_manifest_coverage.py` + `tools/build.gradle.kts` 输入 wiring + `DarkSpriteSheetPipelineScriptTest` 的 7 项 `final full *` 与 1 项 `packaged sentinel audit` 测试都已落地。**Round 4 中我此前在分析摘要里写“0 个测试存在”的判断是错的**，本次确认这些测试存在，应改判为 `PASS_LIMITED`（缺 `finalFullRejectsLockedWithFallbackOnlyDisposition` 或等价 forbidden disposition 拦截）。
5. **跨 PR handoff 合同被绕过**：`UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json` 的 `entries: []` 为空，唯一条目是一条 conservativeDecision，声明“registry+manifest union 即为真值，PR-03/05 DRY_RUN 行不作为 final-full truth”。这与 spec §3 “PR-06 final-full inventory 只能消费 normalized handoff” 的合同直接冲突，把 normalize 责任挪给了 generator 直读 registry + manifest。
6. **manual records 大面积不达标**：`overview-screenshot` 自报 LIMITED_FAIL、`long-session-fatigue` 自报 FAIL（30/60 分钟截图 NOT_CAPTURED_YET）、`damage-float-visual` 自报 NOT_COVERED、`packaged-sentinel-audit` 自报 PASS_LIMITED 且仅审计了 PR-04 scenario 表面。`dark-uiux-pr06-sheet-qa-escalation.md` 与 `dark-uiux-pr06-talent-icon-rebaseline.md` 两个 Acceptance Matrix 显式要求的 artifact **完全缺失**。

整体偏差量级：**资源/manifest/registry 三条主线 ≈ 80% 未交付；客户端 presentation 与工具脚本约 90% 到位；manual evidence 约 30% 到位**。当前提交不应进入 close 流程，建议拆出 1 个资源主修 PR（Round 8-9 sheet + key registry + sheet plan）+ 1 个 evidence 补完 PR，再回主分支做 final-full coverage。

> 不抑制亮点：客户端与工具脚本侧的契约设计与测试落地是本次 PR-06 最扎实的部分，特别是 `ValidationOverlaySummaryPresenter` 把 compact/detailed budgeting 收敛在 single presenter、`QuestSummaryIconResolver` 严格白名单 4 个 `log.objective.*` 不漏不滥、`StatusHudPresenter` 10 + fold badge + hidden presentations 三段式封装，都是符合 “Slow is Fast” 取向的实现。下文 P0 全部集中在资源 / coverage / evidence 三条线。

## 一句话判定

PR-06 的客户端 presentation + 工具脚本 + 测试这三条横切线基本完工，但资源主线（sheet-plan、key-registry、Round 8-9 contact sheet、phase-2 manifest dark-v1 切换、final-full coverage gate）与 manual evidence 主线（同屏 overview、long-session、damage float、sheet QA escalation、talent rebaseline）大面积未交付，PR-06 close gate 不满足。

## 阶段目标符合度（§1）

| 目标 | spec 要求 | 当前状态 | 偏差 |
| --- | --- | --- | --- |
| 1. 替换 skill/status/damage/quest/zone/profession/tree icon | Round 8-9 sheet 落地，玩家可见 keys 走 dark-v1 | `sheet-plan.yaml` 命中 `r08-`/`r09-` sheet = 0；`key-registry.yaml` PR-06 family 命中 = 0 | **未交付**，约偏差 100% |
| 2. fallback / debug / missing / hidden 资源返修 | `r09-fallback-debug` sheet owner=PR-06，覆盖 `missing_visual` / hidden / debug / fallback / locked-placeholder | 同上，sheet 不存在；overview-screenshot manual record 显式承认 `missing_visual remains pending/rejected player-visible` | **未交付**，偏差 100% |
| 3. 新视觉纪元覆盖 `visual-manifest.json` 玩家可见 keys | canonical manifest 玩家可见 key 指向 dark-v1 | phase-2 manifest 已有 56 `icon.skill.*`、12 `icon.mutation.*`、12 `icon.tree.*` 等 key 条目，但 dark-v1 切换 = registry 漏登记 + sheet plan 缺失，无法绑定到 dark-v1 sheet/cell；coverage report 自报 `old-style player-visible keys is non-empty` | **manifest 路由表存在，dark-v1 资源绑定不存在**，偏差约 80% |
| 4. PR-04 临时复用职业树 icon 切到新风格 | UI06-M02 + `dark-uiux-pr06-talent-icon-rebaseline.md` | rebaseline manual record **完全缺失**；inventory 中 `tree icon` 12 / `tree portrait` 12 / `talent visual` 49 均 sheetId=unregistered | **未交付**，偏差 100% |
| 5. validation 模式 runtime overlay / pack summary / scenario evidence summary dark-v1 收口 | `ValidationOverlaySummaryPresenter` 合并 compact/detailed budgeting；`ValidationPackSummaryText` compact；`ValidationScenarioEvidenceSummaryLines` 不遮挡；CJK column-aware | 三个 presenter 与对应测试已落地，COMPACT 用 `max(6, min(12, floor(visibleOverlayRows * 0.25)))`、DETAILED `DETAILED_MAX_ROWS=32`、`clampLine` 走 96 / 144 column-aware budget，`compactPath` 做 machine-path 脱敏与 keep-tail | **达成 ≥ 90%**，参见 P1 §validation 细节 |

## 验收矩阵逐项核查（§0.Acceptance Matrix）

| reqId | source | 当前判断 | 主要依据 |
| --- | --- | --- | --- |
| `UI06-M01` | §3 Round 8-9 资源范围 | **FAIL** | sheet-plan 无任何 Round 8-9 entry；key-registry 无任何 PR-06 family；contact-sheet QA 目录 / artifact 未提交 |
| `UI06-M02` | §4 职业树联动 | **FAIL** | `dark-uiux-pr06-talent-icon-rebaseline.md` 缺失；inventory 中 talent / tree icon sheetId=unregistered |
| `UI06-M03` | §6 full manifest 验收 | **FAIL** | `darkManifestCoverageLint final-full` 自报 FAIL（见 overview-screenshot.md Gate Result）；当前 inventory 含 251 个无原 family 分类的 "upstream registered dark-v1 key" 条目；`icon.quest.objective_marker` 缺失 from registry；`missing_visual` 仍是 pending player-visible |
| `UI06-M04` | status / quest / skill presentation + quest summary row consumer | **PARTIAL** | client 测试存在（`StatusIconResolverTest` 4 个、quest summary 在 `TileRendererCanvasTest` 2522 / 2557）；golden 截图 `dark-uiux-pr06-status-quest-skill-overview-live.png` 已捕获，但 overview-screenshot.md 标注 "actor status icon coverage is not proven by this scenario"，等价于 LIMITED_PASS |
| `UI06-M05` | PR-03/05 rejected cell 返修 | **FAIL** | `dark-v1-pr06-handoff-inventory.json` entries: []；conservativeDecision 用 union 绕过 normalization；无 r09-rejected-polish sheet；rework PR 未启动 |
| `UI06-M06` | governance inheritance | **PARTIAL** | `acceptanceContractLint` 是否过未验；本次审查不重测，但 §0 治理条文延续没有结构性破坏 |
| `UI06-M07` | validation overlay / pack summary | **PASS** | `ValidationOverlaySummaryPresenterTest` 4 个测试齐全（boundsRows / repoRelativeEvidencePaths / compactWarningsAndEvidenceBeforeFold / columnAwareBudgetForCjk）；`ValidationPackSummaryTextTest` / `ValidationScenarioEvidenceSummaryLinesTest` 也已更新 |
| `UI06-M08` | cross-family same-screen visual coherence | **FAIL** | overview-screenshot.md 自报 `LIMITED_FAIL_PR06_CLOSE_GATE_NOT_MET`；validation overlay attempt 与 shell overview SHA-256 相同（说明 PR-04 scenario 无法暴露 PR-06 validation overlay）；damage-float-visual = NOT_COVERED |
| `UI06-M09` | sheet QA escalation policy | **FAIL** | `dark-uiux-pr06-sheet-qa-escalation.md` 完全缺失；spec §3.10 阈值合同没有实际执行证据 |
| `UI06-M10` | long-session readability | **FAIL** | `long-session-fatigue.md` 自报 `FAIL_NOT_A_VALID_60MIN_PR06_PASS`；minute 30 与 60 截图 `NOT_CAPTURED_YET`；§6.7 要求 ≥ 60 min 连续游玩 + 两次 profession switch + 三次战斗 + 一次 quest 完成 + 一次 zone transition + 一次 long-list validation overlay 均未覆盖 |
| `UI06-M11` | resource rework PR 合同 | **CONDITIONAL FAIL** | 未触发 §3.10 阈值的合规判定也不成立：因为 handoff inventory 是空 stub，无法从证据上判断是否已经触发 rework 阈值；rework PR fixture / 工具测试 / owner-scope close gate 未跑通；当前 PR06 主分支已经累计 ≥ 2 轮 contact-sheet rejection 的 sheet 至少应包含 r03 / r05 rejected polish |
| `UI06-M12` | actor status runtime definition bridge | **NOT VERIFIED** | 本次审查未跑 `:core:test --tests com.ktome.core.status.StatusDefinitionsTest`，但 `core/src/main/kotlin/com/ktome/core/status/StatusRuntime.kt` 的 `StatusEffectDef.iconKey` 字段存在；属于 bridge 测试未触发，需要 manual 跑一次确认 |
| `UI06-M13` | official status/profession schema bridge | **NOT VERIFIED** | 同上；`game/src/main/resources/data/professions/index.yaml` 已存在 `iconKey: icon.profession.*`，`StatusSchemaContractTest` 与 `ProfessionSchemaTest` 是否更新需要单跑确认 |

**FAIL：7 项**（M01 / M02 / M03 / M05 / M08 / M09 / M10）
**PARTIAL / LIMITED_PASS：3 项**（M04 / M06 / M11）
**PASS：1 项**（M07）
**NOT VERIFIED：2 项**（M12 / M13）

按 spec “PR-06 close gate 固定为 `final-full`” 的口径，**只要 M03 是 FAIL 就构成 close gate 不满足**；当前 7 项 FAIL 不仅是 close gate 不满足，而是资源主线没交付。

## 关键偏差分类（P0 / P1 / P2）

### P0 · 阻断 close，必须修复

#### P0-1 · Round 8-9 资源主线未交付

**证据**

- `UI/sprite-sheets/sheet-plan.yaml` grep `^- id: r08\|^- id: r09` 命中 = 0。
- `UI/sprite-sheets/key-registry.yaml` 共 266 条 entries，但 `icon.skill.*` / `icon.status.*` / `icon.profession.*` / `icon.quest.*` / `icon.mutation.*` / `icon.damage_type.*` / `icon.tree.*` / `icon.zone.*` / `icon.difficulty.*` / `talent.*` / `tree.*` 各为 0 条。
- `assets-src/image/contact-sheets/dark-v1/` 不包含 r08-/r09- 任何子目录的 contact-sheet QA 报告（请 owner 确认；本次审查未拉到对应 path）。
- `UI/manual-records/dark-uiux-pr06-overview-screenshot.md:42-43` 已显式承认 `darkManifestCoverageLint final-full = FAIL`，failure 文字直接列出 “many keys missing from key registry”、“icon.quest.objective_marker is missing”、“missing_visual remains pending/rejected player-visible”、“old-style player-visible keys are non-empty”。

**影响**

资源主线没交付 = PR-06 §1 阶段目标 1-4 全部未达成，玩家可见 dark-v1 切换在 skill / status / damage / quest / zone / profession / tree / fallback / debug / missing / hidden 任一 family 都还停留在 PR-05 之前的 fallback 状态。`darkManifestCoverageLint final-full` 也因此 fail，PR-06 close gate 不可能放行。

**修复**

1. 生成 Round 8 三张 skill sheet（`r08-skills-vanguard-berserker` / `r08-skills-templar-rogue` / `r08-skills-arcanist-spellblade`），覆盖 release-4 + dev-playable 2 共 6 个职业的所有 `icon.skill.*` 与 `talent.*.icon`。按 §3.6 Multi-size contact-sheet QA 合同采样 `16/24/32/48px`。
2. 生成 Round 9 四张 sheet（`r09-status-damage` / `r09-quest-zone-profession` / `r09-fallback-debug` / `r09-rejected-polish`），分别覆盖 `icon.status.*` + `icon.mutation.*` + `icon.damage_type.*`、`icon.quest.*` + `icon.zone.*` + `icon.profession.*` + `icon.tree.*` + `tree.*`、`missing_visual` + hidden + debug + fallback、PR-03/05/06 rejected cell。
3. `sheet-plan.yaml` 添加 7 张 sheet entry，按 `sheetId + row + col` 网格固定，`icon-sheet` 128px cell、`large-sheet` 256px cell；`talent.*.visual` portrait 走 `large-sheet`，不允许 2x2 merge（见 Round 4 P1）。
4. `key-registry.yaml` 把所有 inventory 中 sheetId=unregistered 的 PR-06 family key 全部登记（最少 ≥ 105 + 11 + 4 + 3 + 12 + 6 + 12 + 12 + 14 + 1 + 49 = **229 key**），写入 `targetKey` / `category` / `ownerPr=PR-06` / `sheetId` / `fallbackKey` / `consumer` / `consumerTest`。
5. 把 `icon.quest.objective_marker` 加入 registry（当前 generator 在 `manifest_family_keys` 里把它当 implicit always-included key，但 registry 不登记会触发 `verify_dark_manifest_coverage.py:289-291` fail-fast）。
6. 把 `phase2-visual-manifest.json` 中 PR-06 family 的 56 + 12 + 12 + ... 条已存在 key 的 `sheetId` 指向 dark-v1 sheet，并跑 `syncPhase2Manifests` 同步到 `client/src/main/resources/manifests/visual-manifest.json`。

**修复优先级**：P0-Top。**这是当前 PR-06 一切失败的根因**。

#### P0-2 · final-full coverage gate 当前必然 FAIL

**证据**

- `UI/manual-records/dark-uiux-pr06-overview-screenshot.md:42-43` 命令 `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json -Pktome.darkUiux.packagedSentinelEvidence=UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md` 自报 `FAIL`，failure 字段已罗列。
- `scripts/verify_dark_manifest_coverage.py:289-291` 在 final-full 模式下 fail-fast `inventory key missing from registry`；当前 229 个 PR-06 family key 未登记进 registry，必然命中。
- `scripts/verify_dark_manifest_coverage.py:463-469` 在 final-full 模式下 fail on missing / pending / old-style player-visible keys。
- `UI/sprite-sheets/dark-v1-final-full-inventory.json` 的 `families[]` 中 `expectedCount` 与实际 `keys[]` 长度匹配（机器一致），但 251 条 `upstream registered dark-v1 key` 全部 sheetId=unregistered；status icon 11 条、skill icon 105 条、profession icon 4 条、talent visual 49 条等核心 PR-06 family 也全部 sheetId=unregistered。

**影响**

这是 P0-1 的直接外部表现，`UI06-M03` FAIL 的根因都在 P0-1。但单列出来是为了说明：即使资源补完，coverage gate 还需要 sheetPlanLint / spriteSheetMapLint / contractLint 联动通过；当前不要假设 “资源补完 coverage 自动 PASS”。

**修复**

1. 完成 P0-1 全部步骤。
2. 在 sheet 上线后 strict 跑 `./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json -Pktome.darkUiux.packagedSentinelEvidence=UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md`，要求 `missingKeys=[]` / `oldStylePlayerVisibleKeys=[]` / `pendingOrRejectedPlayerVisibleCells=[]`。
3. coverage artifact 路径 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` 需在 PR 描述粘贴 status / key counts / error summary（按 §0 canonical artifact 第 7 项的非提交规则）。

#### P0-3 · 跨 PR handoff normalization 被绕过

**证据**

- `UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json`：
  - `entries: []`（空数组）
  - 唯一非空字段是 `conservativeDecisions: [{id: "PR06-HANDOFF-REGISTRY-MANIFEST-UNION", reason: "PR-06 final-full denominator is frozen from the generated inventory union of registry keys and canonical/runtime manifest family keys; legacy PR-03/05 DRY_RUN rows are not used directly as final-full truth."}]`
- spec §3 Cross-PR handoff normalization 段（line 165-175）写明：“`UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json` 是必经 normalization artifact，不只是 missing-artifact fallback。它从 PR-02-1 / PR-02-2 evidence、PR-03/05 JSONL、manual records、coverage reports 和 owner contracts 合成 `playerVisible`、`ownerPr`、`evidencePath`、`qaStatus` 与 `conservativeDecision`，并记录每个字段的 derivation source。PR-06 final-full inventory 只能消费 normalized handoff；不得直接把旧 schema JSONL 当作 final-full handoff truth。”
- spec §3 表格强制每个 input path 必须产出 normalized 行；当前 `entries: []` 等于把这 7 个 input path 全部归到 1 条 conservativeDecision，等价于 PR-06 自己定义自己的真值，**没有 derivation source 可审计**。

**影响**

- PR-03 / PR-05 的 DRY_RUN rejected rows 是否 player-visible、是否需要进 `r09-rejected-polish` 完全无法追溯；如果有 player-visible row 被静默归类成 PR-07 polish handoff，**这正是 PR-06 close gate 要拦的“静默转 PR-07”失败模式**。
- `generate_dark_final_full_inventory.py` 把 `handoff-inventory.json` 列为 source（line 34、line 147），实际只读取 digest 不读取 entries；这相当于 generator 自己直读 registry + manifest 决定 union，违反 spec §3 “只能消费 normalized handoff”。

**修复**

1. 将 `dark-v1-pr06-handoff-inventory.json` 从 stub 升为真实 normalization artifact，逐行写入 §3 表格 7 个 input path 的归一化结果：
   - 每行字段：`source_input_path`, `raw_targetKey`, `raw_sheetId`, `raw_qaStatus`, `raw_rejectionReason`, `raw_reviewedAt`, `raw_reviewer`, `derived_playerVisible`, `derived_ownerPr`, `derived_evidencePath`, `derivation_source`, `pr06_decision`（pending / fixed-in-pr06 / pr07-polish-handoff / allowed-exclusion）。
   - 对 `DRY_RUN` / `reviewedAt=null` / 缺 `playerVisible` / 缺 `ownerPr` 的 raw 行，必须 fail fast 或显式写 conservativeDecision 并标明影响范围。
2. `generate_dark_final_full_inventory.py` 改为 only 消费 normalized handoff 的 `entries[]`，而不是 source_paths 仅做 digest 校验；不允许 generator 自己读 PR-03/05 JSONL。
3. 新增 `DarkSpriteSheetPipelineScriptTest.handoffNormalizationRejectsEmptyEntriesWithoutExplicitConservativeDecision`：当 entries 为空且 conservativeDecisions 不覆盖所有 7 个 input path 时 fail。

#### P0-4 · `dark-uiux-pr06-long-session-fatigue.md` 不构成 ≥ 60 min PR06 pass

**证据**

- `UI/manual-records/dark-uiux-pr06-long-session-fatigue.md:7` `result = FAIL_NOT_A_VALID_60MIN_PR06_PASS`。
- 同文件 `Required Screenshots` 表 30 / 60 分钟均 `NOT_CAPTURED_YET`。
- §6.7 spec 要求：≥ 60 分钟连续游玩 + 两次 profession switch + 三次战斗 + 一次 quest 完成 + 一次 zone transition + 一次 long-list validation overlay。当前 PR-04 profession-tree scenario **结构上不能覆盖任意一项**（运行人自己写在 Fatigue findings #1）。

**影响**

UI06-M10 直接 FAIL。spec 明确把 long-session 作为 `required` whitebox，PR-07 re-audit 是 follow-up，**不是替代**。当前没有 PR-06 自己的 60-min pass 证据 = close gate 不满足。

**修复**

1. 准备或复用一个可触发以下流程的 packaged scenario：profession switch ≥ 2 次、boss / elite 战斗 ≥ 3 次（带 status / damage float / 长效 buff）、quest 完成 ≥ 1 次、zone transition ≥ 1 次、validation overlay long-list 触发 ≥ 1 次。
2. 跑 ≥ 60 min（建议 65 min 留 padding），按 minute 00 / 30 / 60 三段截图，记录 fatigue / accent strength / readability 三类发现，写回 manual record。
3. 同时输出 PR-07 re-audit 的接力 path（`UI/manual-records/dark-uiux-pr07-long-session-reaudit.md`）作为 PR-07 follow-up 占位；该 follow-up artifact 由 PR-07 owner 实现，但 PR-06 必须在自己的 manual record 引用它。

#### P0-5 · damage float 同屏对比未覆盖

**证据**

- `UI/manual-records/dark-uiux-pr06-damage-float-visual.md:7` `result = NOT_COVERED`。
- spec §3.6 Multi-size contact-sheet QA 第 6 条要求 status / mutation / damage type 在 `16/24/32px` 都不混淆；§3.7 Skill cooldown overlay 与同屏 contract 要求 damage type icon + damage float icon + status icon + skill icon 同屏。
- 该 manual record 自报 `INSUFFICIENT_FOR_DAMAGE_FLOAT`。

**影响**

UI06-M08 cross-family same-screen visual coherence 直接缺失证据；同屏 16-24 px damage float 是 PR-06 玩家可见侧最高频的小尺寸 icon，没证据等于 dark-v1 在 damage float 这一面没经过 readability verification。

**修复**

1. 启用一个 boss/elite 重复释放伤害技能的 packaged scenario（berserker basic attack swarm 或 spellblade DOT 多目标都可触发频繁 damage float）。
2. 同屏 capture `dark-uiux-pr06-damage-float-visual-live.png`，要求同屏可见 `icon.damage_type.*`（伤害类型）、`icon.skill.*`（技能 hotbar）、`icon.status.*`（actor status）、damage float 数字 + damage type small icon。
3. 16 / 24 px runtime scale 各采样一张，记录是否出现 silhouette collapse。
4. 写回 manual record，改 `result` 为 `PASS` 或 `LIMITED_PASS_BY_FAMILY_SIDE_BY_SIDE`。

#### P0-6 · `dark-uiux-pr06-sheet-qa-escalation.md` 完全缺失

**证据**

- `ls UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md` 不存在。
- spec UI06-M09 artifact 列：`UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md`（line 33）。
- spec §3.10 要求 ≥ 2 轮 contact-sheet rejection 或同 sheet ≥ 2 次 player-visible cell QA reject 时，必须在 golden / manual evidence 前决定拆 `r0X-<family>-rework` PR。

**影响**

UI06-M09 直接 FAIL。如果 PR-03 / PR-05 rejected polish 已经累计到阈值（按 dark-v1-pr03/05-sprite-map-report.jsonl 的 DRY_RUN 行有疑似 reject 痕迹），却没有 escalation record，等价于 PR-06 自己选了“合进主 PR”路径但没记录决策。

**修复**

1. 新建 `UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md`，记录每张 sheet 的累计 reject round count、player-visible cell reject count、决策（split rework PR vs in-place fix vs PR-07 polish handoff）、责任人。
2. 至少覆盖 Round 8 三张 skill sheet 与 Round 9 四张 sheet；上游 PR-03 / PR-05 累积 reject 也要在此 file 引用 normalized handoff 行汇总。

#### P0-7 · `dark-uiux-pr06-talent-icon-rebaseline.md` 完全缺失

**证据**

- `ls UI/manual-records/dark-uiux-pr06-talent-icon-rebaseline.md` 不存在；但 evidence 路径 `build/whitebox/dark-uiux-pr04-profession-tree-ui/evidence/dark-uiux-pr06-talent-icon-rebaseline-live.png` 与三张 color-blind 仿真截图存在。
- spec UI06-M02 / Acceptance Matrix Execution Addendum 行 row#M02 显式要求 `dark-uiux-pr06-talent-icon-rebaseline` artifact（line 60 附近的 Acceptance Matrix Execution Addendum）。

**影响**

UI06-M02 缺 manual record 文件；仅有截图 PNG 不构成 manual record，因为 manual record 同时承担 “learned/learnable/locked/active 四态可读性、protanopia/deuteranopia/tritanopia 在 32 px 下可区分、selected/focus 不遮 state badge” 的 §4 第 7-8 条结论文字。

**修复**

1. 新建 `UI/manual-records/dark-uiux-pr06-talent-icon-rebaseline.md`，引用现有 4 张 PNG（rebaseline + 3 色盲仿真），写出每张关键判定：state badge silhouette 在 32 px 是否可分辨、color-blind 三态是否仍以形状/字符区分、selected/focus 是否覆盖 state badge。
2. 必须包含 PR-06 §4.7 的合规判定：talent name 不允许嵌入 ASCII state prefix `[x]/[+]/[r]/[*]`，正式 row 必须有 1-char glyph 或固定 marker slot 作为 player-visible secondary cue。该判定需要 manual record reviewer 显式打勾。

### P1 · 实现质量问题，应在 close 前修复

#### P1-1 · 第 13 类 "upstream registered dark-v1 key" 家族违反 §6.1 12-family 合同

**证据**

- `UI/sprite-sheets/dark-v1-final-full-inventory.json` 共 13 个家族，其中 12 个对得上 spec：`skill icon` 105 / `talent visual` 49 / `tree icon` 12 / `tree portrait` 12 / `status icon` 11 / `mutation icon` 12 / `damage type icon` 6 / `quest icon` 3 / `zone icon` 14 / `profession icon` 4 / `difficulty icon` 1 / `fallback/debug/hidden` 5。**第 13 个 `upstream registered dark-v1 key` 含 251 条**（actor.* / vfx.* / tile.* / prop.* / ui.shell.* / item.* / zone.secret.* 等）。
- `scripts/generate_dark_final_full_inventory.py:133-137` `family_for_key()` 的 fallback 分支显式生成这个 catch-all family。

**spec 对应条文**

- spec §3 handoff 表第 1 行：`final-full inventory 必须把仍被 runtime 消费的 ui.shell.* 与 PR-02-2 demo map/actor/prop keys 作为 upstream-covered entries 纳入分母`。即 inclusion 是对的。
- spec frozen profession exclusion schema 的 `family` enum 只允许 `icon.profession`, `icon.skill`, `talent.icon`, `talent.visual`, `icon.tree`, `tree`（line 199）。换言之：spec 定义的 family 命名是按 key 前缀的分类槽，而不是“是否 PR-06 owned”的归属槽。

**实际偏差**

- inclusion 合规，但 251 条 actor / vfx / tile / prop / item / ui.shell 行被合并到 1 个无差别 catch-all 桶里，下游 coverage report 在 `families` 维度上无法显示 actor / tileset / ui.shell / vfx 各自的覆盖度。
- generator 没把 `actor`, `tileset`, `prop`, `vfx`, `ui.shell`, `ui.frame`, `ui.hud`, `ui.screen`, `ui.combat`, `ui.shop`, `ui.control`, `item`, `zone.secret` 等已经在 registry 自然分组的家族单独切片。

**修复**

1. `generate_dark_final_full_inventory.py:50-126` `family_rules()` 追加 upstream family 规则集，按 key 前缀切分：`actor.*`, `tileset.*`, `prop.*`, `vfx.*`, `ui.shell.*`, `ui.frame.*`, `ui.hud.*`, `ui.screen.*`, `ui.combat.*`, `ui.shop.*`, `ui.control.*`, `item.*`, `zone.secret.*`, `dark.uiux.*`。
2. 保留兜底 fallback 但 fail-fast：当一个 key 既不是 PR-06 family 也不是已知 upstream family 时，generator 应输出 `errors` 而不是默默归到 catch-all。
3. 同步更新 `DarkSpriteSheetPipelineScriptTest.finalFullInventoryGenerationIsDeterministic` fixture：upstream key 必须落到对应 family，而不是 `upstream registered dark-v1 key`。

#### P1-2 · `ownerPr` 默认成 PR-06 误归属上游 ownership

**证据**

- `scripts/generate_dark_final_full_inventory.py:195` `"ownerPr": str(registry_entry.get("ownerPr", "")).strip() or "PR-06"`。
- inventory 中 profession icon 4 条全部显示 `ownerPr=PR-06`，但 `key-registry.yaml` 没登记 `icon.profession.*`，所以 generator fallback 写 PR-06。这是合理的 — PR-06 是 profession icon 的新 owner — 但同样的逻辑在 `actor.abyssal.cleanse_hunter` 这种 PR-05 owned key 上 **也写成 PR-06** 是错的（fallback 触发是因为 registry 写了 PR-05，但只在 PR-05 owned key 上 fallback 不该 fire）。

我验证了上面的判断不成立：从 jq 结果看 actor.abyssal.cleanse_hunter 的 ownerPr 确实是 PR-05（registry 有登记，fallback 不触发）。问题只发生在 PR-06 family 没登记进 registry 的情况下：当前所有 PR-06 family key 都 fallback 成 `ownerPr=PR-06`，事实上是对的。

**实际偏差**

- 该 fallback 在 PR-06 完整登记 registry 之后会**自动失效**（registry 有 entry 就读 registry）。所以 P1-2 不是“当前必须修”的偏差，更像是“P0-1 修完后这条自动 OK”。
- 但 **fallback 的存在本身**是个风险点：spec §0 “close gate 不得带 ownerPr 缩小分母” 是对 coverage mode 的约束，generator 把任意未登记 key 默认 `ownerPr=PR-06` 等于把未登记锅扣到 PR-06 头上。如果未来有 PR-07 polish PR 新增 key 而忘了登记，generator 会把这些 key 默认归 PR-06，制造 cross-PR ownership leakage。

**修复**

1. `generate_dark_final_full_inventory.py:195` 改为：未登记 key 默认 `ownerPr="UNKNOWN"`，并把这种 key 写入 generator 的 `errors`；只有 PR-06 family 的预期未登记 key 才 fallback PR-06（白名单走 family_rules 携带 default ownerPr）。
2. 新增 `DarkSpriteSheetPipelineScriptTest.finalFullInventoryReportsUnknownOwnerForUnregisteredUpstreamKey` 拦截该路径。

#### P1-3 · `StatusIconResolver.statusIconKey` 用 `require()` 而非软 fallback

**证据**

- `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt:42-46`：

  ```kotlin
  private fun statusIconKey(presentation: StatusPresentationModel): String {
      val iconKey = presentation.iconKey?.takeIf(String::isNotBlank) ?: STATUS_ICON_KEY_PREFIX + presentation.typeId
      require(iconKey.startsWith(STATUS_ICON_KEY_PREFIX)) {
          "Actor status '${presentation.typeId}' iconKey must use $STATUS_ICON_KEY_PREFIX, got '$iconKey'."
      }
      return iconKey
  }
  ```

**spec 对应条文**

- spec §6.2.1 §1 (line ~280)：null/unknown iconKey 必须走 documented status family fallback `icon.status.<typeId>`，并由 visual manifest fallback 链兜底进 `missing_visual` sentinel；不得 silent drop。
- spec §2 (line 105) 也明确：`StatusIconResolver` 必须 `请求 icon.status.<typeId> 并进入 registered manifest fallback`。

**实际偏差**

- 现在 `require()` 在 official status YAML 误写 iconKey（例如未来 `iconKey: icon.skill.something`，schema 没拦住）时会**抛 IllegalArgumentException**，进而导致渲染线程 crash 或 panel 黑屏。
- spec 的设计意图是 “registered manifest fallback”，即让 manifest 路由把不合规 key 兜到 `missing_visual` 显式可视的 sentinel，而非 fail-fast crash。
- 但 `StatusIconResolverTest` 当前已经写过 "non status actor icon key is rejected"（验证了 require 行为）。这意味着实现侧解释了 “拒绝非 status family iconKey” = 通过 IllegalArgumentException 拒绝，但 spec 要求的是 “经过 documented status family fallback”。

**修复方向 1（推荐）**

把 `require()` 改为 “warn + fallback”：当 iconKey 不以 `icon.status.` 开头时，记录 warning（structured log），改用 `STATUS_ICON_KEY_PREFIX + presentation.typeId`，让 manifest fallback 接管；测试用例 `nonStatusActorIconKeyIsCoercedToStatusFamilyFallback` 替换 `nonStatusActorIconKeyIsRejected`。

**修复方向 2（与 spec §6.2.1 协商）**

如果 PR-06 design 决定 official status YAML 误写就是 schema 错误，应该在 `:game:test`（`StatusSchemaContractTest`）拦截，**不在 client renderer 抛**。client 仍然走 documented fallback。

#### P1-4 · `ValidationOverlaySummaryPresenterTest.compactModeKeepsWarningsAndEvidenceBeforeFold` 依赖 locale 字面量

**证据**

- `client/src/test/kotlin/com/ktome/client/render/ValidationOverlaySummaryPresenterTest.kt:56`：

  ```kotlin
  assertTrue(rows[0].text.contains("visual.missing"), rows.joinToString("\n") { it.text })
  ```

- `ValidationOverlaySummaryPresenter.buildRows()` 在 compact 模式把 `ValidationPackSummaryText.keyWarnings(localizer, summary.packKeyResolutionSummary)` 放到 warningRows[0]，文本内容来自 locale resource。

**实际偏差**

- 该测试假设 warning row 字面文本包含 `visual.missing`。当 ZH_CN locale 把 warning render 成 `视觉.missing` / `视觉.缺失` / `视觉资源缺失` 等本地化形式时，测试会断在 EN locale 字面量上，无法在 i18n 维度通过 owner gate。
- spec §6.3.5 (line ~376) 没有禁止 locale-aware 测试，但要求 “column-aware budget” 同时通过 `usesColumnAwareBudgetForCjkText`，意味着 zh-CN 路径必须可走通；当前 `compactModeKeepsWarningsAndEvidenceBeforeFold` 是 locale 不变量却用了 locale 可变文本。

**修复**

1. 把 `assertTrue(rows[0].text.contains("visual.missing"), ...)` 改为基于 row 索引 + tone + structural assertion：例如 “第一行 tone=WHITE 且 contains warning key count placeholder”，或者将 fixture 显式 `localizer.locale = GameLocale.EN_US`。
2. 同时加一个 ZH_CN locale 下的镜像测试，确保 fold 顺序在两种 locale 下都正确。

#### P1-5 · profession 家族 inventory 只含 4 个 release 职业，缺 dev playable 与 frozen disposition

**证据**

- `UI/sprite-sheets/dark-v1-final-full-inventory.json` profession icon family：
  - `icon.profession.arcanist`, `icon.profession.rogue`, `icon.profession.templar`, `icon.profession.vanguard` （= 4 条）
- `game/src/main/resources/data/professions/index.yaml`：
  - 8 个职业：`vanguard / arcanist / rogue / templar`（RELEASE_UNLOCKED），`berserker / spellblade`（DEV_UNLOCKED），`shadowblade / warden`（LOCKED）。
  - `berserker.iconKey = icon.profession.vanguard`（**共用 release 职业 icon key**），`spellblade.iconKey = icon.profession.arcanist`，`shadowblade.iconKey = icon.profession.rogue`，`warden.iconKey = icon.profession.templar`。

**spec 对应条文**

- spec §3 第 2-3 条（line 192-194）：dev playable / frozen excluded 职业必须明确登记切换范围。
- spec frozen profession exclusion schema（line 197-216）：frozen 职业必须进入 `allowedCoverageExclusions`，并配 `playerVisibility` / `productMessagingKey` / `hoverTooltipContentKey` / `hoverInteraction` / `saveSlotDisposition` / `deathSummaryDisposition` 等字段。
- `playerVisibility=locked-with-fallback-only` 是 forbidden disposition，必须由测试拦截。

**实际偏差**

- 因为 dev playable 与 frozen 职业 **复用 release 职业的 iconKey**，inventory 的 profession icon family 只有 4 个 unique key（这是合理的）。
- 但 spec 仍需要 inventory 的 `allowedCoverageExclusions` 列出 4 条 frozen-related entry（如果 LOCKED 职业卡在 UI 仍可见），或者明确写入 `dev-playable-reuses-release-icon` 的 conservativeDecision；当前 inventory `allowedCoverageExclusions=[]` 完全没有 frozen / dev 相关条目。
- `DarkSpriteSheetPipelineScriptTest.finalFullReportsAllowedFrozenProfessionExclusions` 存在但需要确认它覆盖了 “LOCKED 职业卡 UI 可见且复用 release icon” 这种边界。

**修复**

1. 决定 LOCKED 职业（shadowblade / warden）在 UI 上的 disposition：
   - 如果 `hidden`（不在选职页显示）：写一条 conservativeDecision，说明 LOCKED 职业不进 player-visible inventory。
   - 如果 `locked-with-coming-soon-label`：在 `allowedCoverageExclusions` 写入 2 条 entry（shadowblade、warden），每条含 spec line 199-211 全部字段，且 `playerVisibility=locked-with-coming-soon-label`、`productMessagingKey` 与 `hoverTooltipContentKey` 配齐。
   - 禁止 `locked-with-fallback-only`。
2. 决定 DEV_UNLOCKED 职业（berserker / spellblade）在 release build 是否可见：
   - 如果只在 dev build 可见：用 `playerVisibility=hidden` + `saveSlotDisposition=fallback-with-banner` + `deathSummaryDisposition=fallback-with-banner` 写入 inventory（保险起见）。
   - 如果 release build 也可见：必须按正式职业登记，inventory profession family 升到 6 key（berserker / spellblade 自己的 `icon.profession.*` key，不再复用 vanguard / arcanist）。
3. 新增 `DarkSpriteSheetPipelineScriptTest.finalFullRejectsLockedWithFallbackOnlyDisposition`（spec line 209 显式点名），如果存在等价测试请改名或加注释引用 spec。
4. 同步在 `ProfessionSchemaTest` 测试 dev / locked 职业的 iconKey 复用 / 独立选择是否符合 PR-06 决策。

#### P1-6 · `darkManifestCoverageLint` Gradle wiring 已存在，但 task 输入指纹不含 sheet-plan / key-registry 数字签名

**证据**

- `tools/build.gradle.kts:646-716` 已经把 `ktome.darkUiux.coverageMode` / `ktome.darkUiux.expectedInventory` / `ktome.darkUiux.packagedSentinelEvidence` 三个 property 接入；inputs 列了 `scripts/verify_dark_manifest_coverage.py` 与 `scripts/generate_dark_final_full_inventory.py`。
- `dark-v1-final-full-inventory.json.sourceDigests` 已经记录 10 个 source path 的 sha256。

**实际偏差**

- generator 已经验证 sourceDigests deterministic，但 Gradle task 还没把 inventory 自身作为输入指纹的一部分；如果有人手改 inventory 而不重跑 generator，coverage lint 在没有 generator stale check 的情况下可能误判。
- spec §6.1 (line 246-262) 要求 “generator 输入必须固定 + inventory 必须包含 source file digest + stale inventory fail”。这条已经在 generator 端实现（sourceDigests），但 `darkManifestCoverageLint` 在 final-full 模式下没有调用 generator 校验 inventory 是否 stale。

**修复**

1. `darkManifestCoverageLint` 在 final-full 模式增加 pre-check：调用 `generate_dark_final_full_inventory.py --check`（dry run，不写文件，只比较 expected sourceDigests）；mismatch 时 fail。
2. 等价方案：在 `verify_dark_manifest_coverage.py` 内直接复算 inventory.sourceDigests，与 inventory 文件中记录值比对，不一致就 fail。
3. `DarkSpriteSheetPipelineScriptTest.finalFullRejectsStaleInventorySourceDigest` 已存在；确认它覆盖了 “generator 没跑就改 source 文件” 的路径，而不是仅覆盖 “手改 inventory.sourceDigests” 的路径。

### P2 · 设计/体验细节问题，可在 close 后修正

#### P2-1 · `inscription` skill icon 消费链没在 generator 默认 family rules 标注

**证据**

- `scripts/generate_dark_final_full_inventory.py:54-56` skill icon family rule：

  ```python
  lambda key, _: key.startswith("icon.skill.") or exact_talent_key(key, ".icon")
  ```

- spec §3.1 (line 188) 指出 inscription active ability icon 也是 `icon.skill.*` 消费者；当前 rule 已经匹配到了 `icon.skill.*`，所以函数行为正确。但 default consumer 字符串 `TalentAssetReferences / inscription active ability icon consumers` 只在 fallback 路径（registry 没 consumer 字段时）使用。

**实际偏差**

- 不是 bug，是文档行为：因为 `key-registry.yaml` 当前没登记 `icon.skill.*`，generator 会用 default consumer 字符串。一旦 P0-1 把 registry 补完，default consumer 字符串就被 registry 覆盖，inscription 消费链信息会丢失。
- 建议在 key-registry 登记 `icon.skill.*` 时，把 inscription 与 skill icon 的所有 consumer 都列在 `consumer` 字段（用 ` / ` 分隔），保留 trace。

**修复**

1. registry 登记 `icon.skill.*` 时 `consumer` 写：`TalentAssetReferences / inscription active ability icon consumers / hotbar skill display`。
2. `consumerTest` 写：`ManifestResolveTest / TalentSidebarPresenterTest / shellHotbarSkillIconTest`。

#### P2-2 · `compactPath()` 的 keep-tail 当 path 末段是中文时会粗暴 drop CJK 字符

**证据**

- `client/src/main/kotlin/com/ktome/client/render/ValidationOverlaySummaryPresenter.kt:230-240`：

  ```kotlin
  private fun keepTail(value: String, limit: Int = MAX_VISUAL_COLUMNS): String {
      val marker = "..."
      var result = value
      while (result.isNotEmpty() && visualColumns(marker + result) > limit) {
          result = result.drop(1)
      }
      return marker + result
  }
  ```

**实际偏差**

- 没有保留 path separator 边界；如果 path 中包含 `/中文目录/file.png` 且需要 drop，可能保留 `...文目录/file.png`，部分字符被截断。
- spec §6.3 / §6.3.7 (line 360-385) 要求 path 显示走 compactPath；当前实现满足 column-aware budget，但读者体验上不友好。

**修复**

1. `keepTail()` 优先在 `/` 边界 drop（保留 last `/` 之后的完整 segment）；如果保留 last segment 已超 limit，再降级到字符级 drop。
2. 给 `compactPath` 加 unit test：`compactPath("/Users/x/中文目录/file.png").length > 0 && ...`。

#### P2-3 · `StatusHudPresenter` fold badge 借用 last visible icon asset 缺合规性回查

**证据**

- `client/src/main/kotlin/com/ktome/client/ui/status/StatusHudPresenter.kt`（构造 `+N more` badge 的 model 时复用第 10 个可见 icon 的 asset）。

**实际偏差**

- 视觉上 “+N 角标” 落在最后一个可见 icon 上是常规手法；但 spec §6.2.1.6 没有显式说 fold badge 必须复用同一个 asset，可能未来 design 要求独立 badge sheet entry。
- 这不是 close blocker，但应该在 fold badge 落地后跑一次 contact-sheet QA 看视觉是否互相干扰。

#### P2-4 · `QuestSummaryIconResolver` 白名单 4 个 `log.objective.*` 是否覆盖所有产生 marker 的 quest log 类型

**证据**

- `client/src/main/kotlin/com/ktome/client/render/QuestSummaryIconResolver.kt`：只在 `log.objective.kill`, `log.objective.collect`, `log.objective.escort`, `log.objective.boss` 4 个 key 上返回 `icon.quest.objective_marker`。
- spec §6.2.6（quest icon consumer）要求**精确白名单**，排除 `icon.quest.armory_key` / `icon.quest.seal_key` 等 inventory item key。

**实际偏差**

- 实现忠于 spec。但产品端如果未来新增 `log.objective.explore` / `log.objective.survive`，需要同步加白名单。
- 建议加 `TileRenderModelTest.shellQuestSummaryRejectsUnknownObjectiveTextKey` 测试，保证未来漏加白名单时被发现。

## 修复优先级与顺序

PR-06 当前状态不应进入 close 流程。建议按下述顺序拆分修复，**P0 全部完成才能跑 final-full coverage gate**：

### Phase A · 资源主线修复（P0-1 / P0-2）

1. 生成 Round 8 prompt（按 `UI/sprite-sheets/sheet-plan.yaml` 的 PR-00 固定命令），跑 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --overwrite` 落地 3 张 skill sheet PNG。
2. 同样落地 Round 9 4 张 sheet PNG（`r09-status-damage` / `r09-quest-zone-profession` / `r09-fallback-debug` / `r09-rejected-polish`）。
3. `UI/sprite-sheets/sheet-plan.yaml` 增 7 条 sheet entry（icon-sheet 128 px、large-sheet 256 px），不允许 2x2 merge。
4. `UI/sprite-sheets/key-registry.yaml` 增 ≥ 229 条 entry：`icon.skill.*` ≥ 105 + `icon.status.*` ≥ 11 + `icon.profession.*` ≥ 4 (+ frozen) + `icon.quest.*` ≥ 3 + `icon.mutation.*` ≥ 12 + `icon.damage_type.*` ≥ 6 + `icon.tree.*` ≥ 12 + `icon.zone.*` ≥ 14 + `icon.difficulty.*` ≥ 1 + `talent.*.icon`/`talent.*.visual` ≥ 49 + `tree.*` portrait ≥ 12 + `fallback/debug/hidden` ≥ 5 + `icon.quest.objective_marker` 1。
5. 跑 `darkSpriteSheetLint` / `spriteSheetMapLint` 验证 sheet plan / map deterministic。
6. 跑 `syncPhase2Manifests` 把 phase-2 manifest dark-v1 切换同步到 runtime manifest。
7. 跑 `./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json -Pktome.darkUiux.packagedSentinelEvidence=UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md`。要求：`exitCode=0` / `missingKeys=[]` / `oldStylePlayerVisibleKeys=[]` / `pendingOrRejectedPlayerVisibleCells=[]`。

### Phase B · 跨 PR handoff 与 inventory generator 修复（P0-3 / P1-1 / P1-2 / P1-6）

8. 重写 `UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json` 为真实 normalization artifact，逐行写入 PR-03 / PR-05 JSONL + 4 个 PR-02-1 / 02-2 / 03 / 05 manual record 的归一化结果，每行带 derivation source；空 entries 时必须用 conservativeDecision 覆盖所有 7 个 input path。
9. `generate_dark_final_full_inventory.py` 改造：
   - `family_rules()` 增加 actor / tileset / vfx / prop / item / ui.shell / ui.frame / ui.hud / ui.screen / ui.combat / ui.shop / ui.control / zone.secret / dark.uiux upstream family（消除 13th catch-all）。
   - `family_for_key` 未匹配时 fail-fast，不允许默默归到 catch-all。
   - `ownerPr` fallback 改为 `UNKNOWN` 并 emit error，PR-06 family 走 family_rules 携带的 default ownerPr。
   - generator 改为只消费 normalized handoff 的 `entries[]`，不再直读 PR-03 / PR-05 JSONL。
10. `scripts/verify_dark_manifest_coverage.py` 在 final-full 模式增加 stale check（复算 inventory.sourceDigests 与文件中记录比对，mismatch 时 fail）。
11. 在 `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt` 增量补：
    - `finalFullRejectsLockedWithFallbackOnlyDisposition`
    - `finalFullInventoryReportsUnknownOwnerForUnregisteredUpstreamKey`
    - `handoffNormalizationRejectsEmptyEntriesWithoutExplicitConservativeDecision`
    - `finalFullClassifiesUpstreamActorAndUiShellKeysByPrefix`

### Phase C · client 实现微调（P1-3 / P1-4）

12. `StatusIconResolver.statusIconKey` 把 `require()` 改为 warn+fallback，让 manifest fallback 接管不合规 iconKey；更新 `StatusIconResolverTest.nonStatusActorIconKeyIsRejected` → `nonStatusActorIconKeyIsCoercedToStatusFamilyFallback`。
13. 同时在 `:game:test`（`StatusSchemaContractTest`）增 official YAML iconKey schema 校验，把 fail-fast 责任放到 schema 层而不是 renderer。
14. `ValidationOverlaySummaryPresenterTest.compactModeKeepsWarningsAndEvidenceBeforeFold` 改为 structural assertion（tone + row index + warning placeholder pattern），或显式 EN_US locale；同时加 ZH_CN locale 镜像测试。

### Phase D · manual evidence 补完（P0-4 / P0-5 / P0-6 / P0-7）

15. 准备 ≥ 60 min packaged scenario 覆盖 §6.7 minimum structure（2 × profession switch + 3 × combat + 1 × quest complete + 1 × zone transition + 1 × long-list validation overlay）。
16. 跑完后写回 `UI/manual-records/dark-uiux-pr06-long-session-fatigue.md`，三段截图（00 / 30 / 60 minute）补齐 sha256。
17. 跑 boss/elite damage 多目标 scenario，capture `dark-uiux-pr06-damage-float-visual-live.png`（16 / 24 px runtime scale 各一张），写回 `UI/manual-records/dark-uiux-pr06-damage-float-visual.md`。
18. 新建 `UI/manual-records/dark-uiux-pr06-sheet-qa-escalation.md`：per-sheet reject round count、player-visible cell reject count、split rework PR 决策。
19. 新建 `UI/manual-records/dark-uiux-pr06-talent-icon-rebaseline.md`：引用已有 4 张 PNG，写出 learned/learnable/locked/active 四态 + 三种 color-blind 仿真的可读性判定，且确认 talent name 不嵌入 ASCII state prefix。
20. 在 `UI/manual-records/dark-uiux-pr06-overview-screenshot.md` 中刷新 Gate Result 表，要求 `darkManifestCoverageLint final-full = PASS`，并把 `dark-uiux-pr06-status-quest-skill-overview.png` 改为同屏可见 status icon + skill icon + quest marker + damage float + cooldown overlay 的真同屏截图（不是 PR-04 profession-tree scenario 借用）。

### Phase E · bridge 验证（M12 / M13）

21. 跑 `:core:test --tests com.ktome.core.status.StatusDefinitionsTest` 一次，把 result 引到 PR 描述。
22. 跑 `:game:test --tests com.ktome.game.data.StatusSchemaContractTest --tests com.ktome.game.data.ProfessionSchemaTest` 一次，把 result 引到 PR 描述。

### Phase F · close 前最终回归

23. `acceptanceContractLint`。
24. resource lint 全套：`darkSpriteSheetLint` / `spriteSheetMapLint` / `assetLint` / `styleLint` / `darkKeyRegistryLint` / `manifestLint`。
25. `:client:clientSmoke` / `:client:goldenScreenshot`。
26. `darkManifestCoverageLint final-full`（要求 PASS）。
27. `localeLint` / `contractLint` / `maintainabilityLint`。
28. `verifyChanged`。

## 推荐决策

| 决策点 | 推荐 |
| --- | --- |
| 当前是否可 close PR-06 | **否**。资源主线（Round 8-9 + sheet plan + key registry）未交付，coverage gate 自报 FAIL。 |
| 是否需要拆 rework PR | 是。建议把 resource generation + sheet-plan / key-registry 落地拆成 `dark-uiux-pr06-resource-rework` 独立 PR（owner=assets，参考 §3.10 rework 合同跑 `owner-scope`），与 PR-06 main 解耦。 |
| 是否需要把客户端 presentation 拆成单独 PR | 否。当前 client 改动（StatusIconResolver / StatusHudPresenter / QuestSummaryIconResolver / ValidationOverlaySummaryPresenter / ValidationPackSummaryText / ValidationScenarioEvidenceSummaryLines）已经成体系，且测试齐全，留在 PR-06 main 是正确的。 |
| 是否需要修订 PR-06 spec | 不需要再修订；Round 4 已经把文档级合同问题处理完了。本轮全部是实现层偏差，不要再回头改 spec。 |
| Phase A 估算工作量 | XL（≥ 7 张 sheet 生成 + ≥ 229 条 registry 登记 + manifest 切换 + coverage gate 跑通），不计入本 PR remaining work 显然不现实。 |
| Phase D evidence 补完 | M（packaged scenario 准备 + 60 min 录制 + 4 个 manual record 补齐），可与 Phase A 并行。 |

## 与 Round 4 文档级 review 的差异

Round 4（`2026-05-22-dark-uiux-pr06-skills-status-quest-full-manifest-design-director-review-round4.md`）已经修订了 PR-06 spec 的文档级合同问题（2x2 cell merge、bridge owner gate、stale inventory、handoff schema、final-full owner-scope 误用、packaged sentinel 输入合同、status fold renderer owner）。本轮是文档修完后的**实现层**审查，与 Round 4 不重复，专注：

1. spec §3 资源范围在分支上是否真的有 sheet / registry entry。
2. spec §6.1 final-full inventory 是否真的生成且家族分类符合 12-family。
3. spec §6.2 status icon / quest icon consumer 在 client 是否真的接入。
4. spec §6.3 validation overlay column-aware budget 是否真的统一在一个 presenter。
5. spec §6.7 long-session 等 manual evidence 是否真的捕获。

结论：

- 文档侧（Round 4）OK，无需再调。
- 客户端 + 工具脚本侧基本完工。
- 资源 + manifest + manual evidence 侧大面积未交付，是当前 close gate 不满足的全部原因。

## 审计依据清单

- `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`（spec, 592 lines）
- `UI/sprite-sheets/dark-v1-final-full-inventory.json`（generated 485 keys / 13 families / sourceDigests 10 paths）
- `UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json`（**entries:[]**, 1 conservativeDecision）
- `UI/sprite-sheets/key-registry.yaml`（266 entries, 0 PR-06 family）
- `UI/sprite-sheets/sheet-plan.yaml`（0 PR-06 Round 8-9 entry）
- `assets-src/image/manifests/phase2-visual-manifest.json`（629 keys，含 56 `icon.skill.*` / 12 `icon.mutation.*` / 12 `icon.tree.*` 等，但 dark-v1 sheet 绑定缺失）
- `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt:42-46`
- `client/src/main/kotlin/com/ktome/client/ui/status/StatusPresentationModel.kt`
- `client/src/main/kotlin/com/ktome/client/ui/status/StatusHudPresenter.kt`
- `client/src/main/kotlin/com/ktome/client/render/QuestSummaryIconResolver.kt`
- `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:865, 932-933, 1237-1252`
- `client/src/main/kotlin/com/ktome/client/render/ValidationOverlaySummaryPresenter.kt:1-263`
- `client/src/main/kotlin/com/ktome/client/render/ValidationPackSummaryText.kt`
- `client/src/main/kotlin/com/ktome/client/render/ValidationScenarioEvidenceSummaryLines.kt`
- `client/src/test/kotlin/com/ktome/client/ui/status/StatusIconResolverTest.kt`
- `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt:2522, 2557`
- `client/src/test/kotlin/com/ktome/client/render/ValidationOverlaySummaryPresenterTest.kt`
- `scripts/generate_dark_final_full_inventory.py:1-200`
- `scripts/verify_dark_manifest_coverage.py:41-52, 131-145, 212-218, 266-280, 289-291, 463-469`
- `tools/build.gradle.kts:646-716`
- `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt`（49 @Test，含 7 个 `final full *` + 1 个 packaged sentinel）
- `game/src/main/resources/data/professions/index.yaml`（8 个职业，dev / frozen 复用 release iconKey）
- `UI/manual-records/dark-uiux-pr06-overview-screenshot.md`（self-reported LIMITED_FAIL）
- `UI/manual-records/dark-uiux-pr06-long-session-fatigue.md`（self-reported FAIL）
- `UI/manual-records/dark-uiux-pr06-damage-float-visual.md`（self-reported NOT_COVERED）
- `UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md`（self-reported PASS_LIMITED）

## 结语

PR-06 当前提交在客户端 + 工具脚本 + 测试三条横切线达到了 close 标准；资源、manifest、key registry、coverage gate、manual evidence 这五条主线一起还差最关键的 Round 8-9 资源生成 + registry 登记 + manifest 切换。资源不上线，client 端再齐全也撑不起 “dark-v1 final-full” 这个收口承诺。建议：

1. **不要在 PR-06 main 上直接补资源** —— 按 spec §3.10 rework 合同拆 `dark-uiux-pr06-resource-rework` PR，跑 owner-scope，merge 后 main 再跑 final-full。
2. **不要把 long-session / damage float / sheet QA escalation / talent rebaseline 这 4 类 manual evidence 当 follow-up follow-up**；它们是 §6.7 / UI06-M02 / UI06-M08 / UI06-M09 / UI06-M10 的 close-gate 输入。
3. **不要回头改 spec**；Round 4 已经把文档级合同收口完，本轮全部是实现差距，spec 没问题。

总体：PR-06 已经在最难的“治理/合同设计 + client 实现 + 工具脚本”这三件事上把骨架立住了，差的是资源 + evidence 两条腿。这两条腿补上之后，PR-06 是 dark-v1 manifest 收口可信的一笔交付。

— Codex 设计总监审查（Opus 4.7, max effort），2026-05-23

# Phase 4 UI/UX PR 文档深度 Review Round 6

**日期**: 2026-04-21  
**被审对象**:

1. `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`
2. `docs/opt/ui-pr/README.md`
3. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md`
4. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md`
5. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md`
6. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr04-status-description-and-readability.md`
7. `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md`
8. `docs/opt/ui-pr/resource-fallback-audit-template.md`
9. `docs/opt/ui-pr/manual-records/_template.md`
10. `docs/opt/ui-pr/follow-ups.md`

**总评**: 最新一轮修改已经把 round5 的主要 P2 问题基本闭合：locale scope、`specialTierId` snapshot owner tests、unknown `qualityTierId` fail-fast、PR05 `LegalTargetSummary` 都已补强。本轮未发现 P0/P1。剩余问题集中在“文档可执行性”的细边界：源计划旧 PR 编号残留、PR05 一个旧字段名未同步、PR03 special 双字段不变量缺反向约束，以及 shared fallback 模板的一句例外仍可能被实现者放宽。

---

## Findings

### P2-1 源计划仍残留旧 8 PR 执行前缀，和当前 5 PR 执行合同冲突

**影响 PR**: PR-01、PR-02、PR-03  
**证据**:

- `docs/opt/ui-pr/README.md:9-13` 已明确当前执行拆分：`phase4-uiux-pr01` 合并原 PR-01+PR-02，`phase4-uiux-pr02` 对应原 PR-03，`phase4-uiux-pr03` 合并原 PR-04+PR-05。
- `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:477-485` 也写明 resource plan、processing report、golden label、manual record、`build.gradle.kts --extra-plan` 一律使用当前 5 PR 前缀。
- 但同一源计划 `:179-183` 的范围列表仍把 `phase4-uiux-pr02` 写成首页、`phase4-uiux-pr03` 写成局内信息面、`phase4-uiux-pr04/05` 写成 item/card；只有 `:184-186` 对旧 PR-06~08 标了“旧”。
- 首页资源与截图仍使用旧 `phase4-uiux-pr02-*`：`:684-685`、`:711`。按当前拆分，首页属于 PR-01，应使用 `phase4-uiux-pr01-*`。
- Look Mode 资源与记录仍使用旧 `phase4-uiux-pr03-*`：`:834-835`、`:869`。按当前拆分，Look Mode 属于 PR-02，应使用 `phase4-uiux-pr02-*`。
- item/card 相关截图和资源仍有旧 `phase4-uiux-pr04/05-*`：`:996`、`:1054`、`:1080`。按当前拆分，这些属于 PR-03，应使用 `phase4-uiux-pr03-*`。其中 `:1052` 图片 plan 已改成 `phase4-uiux-pr03-*`，但 `:1054` 音频 plan 仍是 `phase4-uiux-pr05-*`，形成同段自相矛盾。

**问题**: PR 文档本身的前缀大多已经修正，但源计划仍是被 PR 文档引用的上游真源。实现者按源计划执行资源 plan、manual record 或 golden label 时，仍可能生成旧前缀 artifact，导致 owner gate、review evidence 和后续 PR 查找路径错位。

**改进建议**:

1. 将源计划 §4.1 的 1~5 行全部改成“旧 PR-xx -> 当前执行 PR”的口径，或直接替换为当前 5 PR 列表，避免前半段旧编号看起来仍是执行编号。
2. 源计划所有 resource plan、golden label、manual record 示例统一替换为当前前缀：
   - 首页: `phase4-uiux-pr02-*` -> `phase4-uiux-pr01-*`
   - in-game/input/look: `phase4-uiux-pr03-*` -> `phase4-uiux-pr02-*`
   - item/card/state: `phase4-uiux-pr04-*` / `phase4-uiux-pr05-*` -> `phase4-uiux-pr03-*`
3. 特别修正 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:1054`，使共享卡片音频 companion plan 与同段图片 plan 一样使用 `phase4-uiux-pr03-audio-plan.yaml`。

### P2-2 PR-05 状态机仍引用旧 `legalTargetCountPreview`，和新 `LegalTargetSummary` 模型脱节

**影响 PR**: PR-05  
**证据**:

- PR-05 §4.2 状态机仍写：`action.legalTargetCountPreview == 0 -> action 显示 disabled`，见 `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md:130`。
- 同文档 §4.4 已将模型改为 `legalTargetSummary: LegalTargetSummary`，其中 `count: Int?`、`missingReason` 表达 unknown，见 `:189-206`。
- 测试清单也已要求覆盖 `missing legal target count -> missingFactReason` 与 `known zero legal targets -> no-legal-target feedback`，见 `:277`、`:300`。

**问题**: `legalTargetCountPreview` 已不是模型字段，也没有在 PR-05 中定义来源。实现者可能为了满足状态机重新做一个 client-local preview，绕开 `ActionHintModelBuilder`，或者把 `count == null` 错压成 `0`，重新引入 round5 已经修掉的“unknown 被显示成 no target”问题。

**改进建议**:

将 `:130` 改成基于新模型的判定，例如：

```text
若 action.legalTargetSummary.count == 0 && action.legalTargetSummary.missingReason == null
-> action 显示 disabled；提交时保持 ACTION + toast ui.message.combat.no-legal-target

若 action.legalTargetSummary.count == null
-> 不显示 no-legal-target disabled；展示 missingFactReason / missingReason，并禁止 client 反推合法目标数
```

同时把 §5.1 的 `CombatDecisionFrameTest` 明确拆成两条 case：`known zero` 与 `unknown legal target count`。

### P3-1 PR-03 special tier 双字段只约束了“有 template 缺 tier”，缺少“有 tier 无 template”的反向非法态

**影响 PR**: PR-03  
**证据**:

- PR-03 定义 special 轴必须由 `specialTierId` 与 `specialTemplateId` 一起判断，见 `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md:124-126`。
- 映射表覆盖了 `specialTemplateId != null && specialTierId == UNIQUE/ARTIFACT/null`，见 `:136-138`。
- lint 阻塞规则只写了 `specialTemplateId != null` 但 `specialTierId` 缺失/非法，见 `:287`。
- 出口门禁也只说非法 `specialTierId` 与 unknown quality fail-fast，见 `:498-499`。

**问题**: 目前文档没有明确 `specialTemplateId == null && specialTierId != null` 是非法 snapshot。这个反向组合虽然不该由正常 builder 产生，但一旦出现，它会让 renderer 看到 tier 却没有 special template 身份，可能被误渲染成 accent，或者在不同 UI surface 中表现不一致。

**改进建议**:

1. 在映射表补一行：`specialTemplateId == null && specialTierId != null` -> fail fast，不渲染 accent。
2. 将 lint 阻塞规则 `:287` 改成双向不变量：
   - `specialTemplateId != null` requires `specialTierId in UNIQUE/ARTIFACT`
   - `specialTierId != null` requires `specialTemplateId != null`
3. 将 snapshot contract tests 的非法 special item case 明确覆盖这两个方向。

### P3-2 Resource Fallback Audit 对 high risk 的例外表述仍偏宽，可能被误读为允许开放依赖新 key 的正式路径

**影响 PR**: PR-02、PR-03、PR-04、PR-05  
**证据**:

- 上游计划明确写：真正启用新 key 的 UI 路径，不得在资源未落地前提前开放，见 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md:470`。
- PR-05 也写：未生成正式资源时，不开放依赖新 key 的 UI 路径，见 `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md:439-441`。
- 但 shared 模板写：`失效风险等级=high` 时不得开放依赖该 key 的正式玩家路径，除非同 PR 已有人工白盒和 smoke/golden 替代证据，见 `docs/opt/ui-pr/resource-fallback-audit-template.md:15`。

**问题**: `除非同 PR 已有人工白盒和 smoke/golden 替代证据` 容易被理解为：只要证据齐，就可以开放依赖新 key 的正式玩家路径。这个语义弱于上游计划和 PR-05 的硬约束。smoke/golden 能证明 fallback 可见，不能替代正式资源落地，也不能让“依赖新 key 的正式路径”提前开放。

**改进建议**:

将模板第 15 条拆成两条：

```text
5. 失效风险等级=high 且 UI 路径依赖新 key 时，不得开放正式玩家路径；smoke/golden/人工白盒只能作为 fallback 可见性证据，不能替代正式资源落地。
6. 只有 fallback 复用既有 key，且不依赖新 key 启用新 UI 路径时，才允许在同 PR 证据齐全后临时开放。
```

这样可以保留 fallback 审计的灵活性，同时不放松资源未落地的 player path 门禁。

### P3-3 PR-02 truth table 中 `owner-defined` 仍偏宽，测试实现时缺少可断言结果

**影响 PR**: PR-02  
**证据**:

- PR-02 §4.3 行为矩阵中 `SHOP / WORLD_MAP / STAT_ASSIGN`、`VALIDATION`、`TARGETING` 仍保留多处 `owner-defined`，见 `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md:174-177`。
- 同 PR 已要求 `InputHandlerTest` 覆盖当前阶段 truth table 的最小行为面，源计划也要求人工逐项验证这些 mode。

**问题**: `owner-defined` 对设计讨论足够，但对 PR-02 的测试合同仍偏松。尤其 `VALIDATION` 和 `TARGETING` 都属于高风险 mode，若测试只断言“交给 owner”，很容易漏掉 `ESC / Backspace / Ctrl+S / save allowed` 的跨 mode 一致性。

**改进建议**:

1. 把 `owner-defined` 拆成更可测的结果类别，例如 `delegated-no-stack-mutation`、`delegated-target-cursor-only`、`delegated-validation-command-no-save`。
2. 对 `SHOP / WORLD_MAP / STAT_ASSIGN` 至少补一行共通约束：不创建 active stack、不允许 client-local close all、`Ctrl+S` 遵循 §4.3 统一 save gate。
3. 对 `VALIDATION` 和 `TARGETING` 单独补 `ESC / Backspace / Ctrl+S` 的预期，避免被 owner command 吞掉全局语义。

---

## Requirement Alignment

| 合同/要求 | 当前状态 | 缺口 |
| --- | --- | --- |
| 当前执行以 5 PR 前缀为准 | PR 文档和 README 基本一致 | 源计划多个资源、截图、manual 示例仍是旧前缀 |
| `RenderSnapshot` / typed fact 不得被 client 反推 | PR-03 `specialTierId`、PR-05 `LegalTargetSummary` 已收紧 | PR-05 状态机旧字段名可能重新诱导 client-local preview |
| unknown typed fact 不得显示成确定结果 | PR-05 已通过 `count: Int?` 和 `missingFactReason` 表达 | `legalTargetCountPreview == 0` 仍需同步改写 |
| special item 表现由正式 snapshot fact 驱动 | PR-03 已扩 `ItemRenderSnapshot.specialTierId` | 缺 `specialTierId != null` requires `specialTemplateId != null` 的反向非法态 |
| 资源 fallback 不得绕过正式资源门禁 | PR 文档多数已要求 fallback audit | shared 模板 high-risk 例外比上游硬约束宽 |
| truth table 应可被测试断言 | PR-02 已显著细化 | 个别 `owner-defined` 仍需要变成可断言 delegation contract |

---

## 功能/系统一致性矩阵

| PR | 不足 | 改进意见 | 严重度 |
| --- | --- | --- | --- |
| PR-01 Client Foundation and Main Menu | PR-01 文档自身未发现新增 blocker；但源计划首页资源、manual/golden label 仍使用旧 `phase4-uiux-pr02-*` | 源计划首页相关示例统一改为 `phase4-uiux-pr01-*`，或明确标为历史非执行示例 | P2 |
| PR-02 In-Game Info, Input, Modal, Look | 源计划 Look Mode 资源/记录仍使用旧 `phase4-uiux-pr03-*`；truth table 仍有宽泛 `owner-defined` | 源计划改为 `phase4-uiux-pr02-*`；truth table 将 `owner-defined` 改成可测试 delegation contract | P2/P3 |
| PR-03 Item, Content Presentation, UI States | 源计划 item/card 部分仍有旧 `phase4-uiux-pr04/05-*`；`specialTierId` 双字段不变量缺反向非法态 | 统一为 `phase4-uiux-pr03-*`；补 `specialTierId != null` requires `specialTemplateId != null` 的 lint/test/映射表 | P2/P3 |
| PR-04 Status, Description, Readability | PR-04 文档自身未发现新增 blocker；共享 fallback 模板的 high-risk 例外会影响本 PR 的 status/icon/audio fallback 审计 | 收紧模板，明确 high-risk 证据不能替代正式资源落地，只有复用既有 key 的 fallback 才能临时开放 | P3 |
| PR-05 Telegraph and Combat Decision Surface | 状态机仍引用旧 `legalTargetCountPreview`，和 `LegalTargetSummary(count: Int?)` 脱节；共享 fallback 模板同样影响 telegraph/combat 资源路径 | 状态机改用 `legalTargetSummary.count/missingReason`；模板收紧 high-risk 例外 | P2/P3 |

---

## 玩法与体验审查

1. **主菜单与首页路径**: PR-01 体验目标已经清楚；风险不在 PR-01 文档，而在源计划残留 `phase4-uiux-pr02-main-menu-*`，会让截图/golden evidence 被归到错误 PR。
2. **输入与 modal**: PR-02 的整体架构已可实施，但 `owner-defined` 仍会削弱键位一致性。玩家感知上最容易出问题的是 `ESC / Backspace / Ctrl+S` 在 owner mode 中被吞掉或含义漂移。
3. **物品与卡片**: PR-03 已避免 client 反查 special template；还需补齐 `specialTierId` 反向非法态，否则异常 snapshot 在不同 UI surface 的表现可能不一致。
4. **状态与说明**: PR-04 当前文档问题最少。需要注意的是 fallback 模板属于 shared gate，一旦模板过宽，PR-04 的 status/icon/keyword fallback 也会被放宽。
5. **战斗决策面**: PR-05 已修正 unknown fact 表达，但状态机旧字段会直接影响动作 disabled 体验。必须保持“unknown legal target count”和“known zero legal target”在 UI 上是两种状态。

---

## 当前阶段必须解决的问题

1. **必须修 P2-1**: 源计划旧前缀残留会污染 resource plan、golden、manual record 和 `--extra-plan`。这是串行 PR gate 的证据路径问题，应在实现 PR-01 前修完。
2. **必须修 P2-2**: PR-05 的 `legalTargetCountPreview` 是旧 contract 残留，会抵消 `LegalTargetSummary` 的修复价值。应在 PR-05 文档进入实现前修完。
3. **建议修 P3-1**: PR-03 special 双字段反向非法态是小缺口，但属于 snapshot contract 不变量，修起来成本低。
4. **建议修 P3-2**: fallback 模板一句话即可收紧，避免后续每个资源型 PR 都重新解释。
5. **建议修 P3-3**: PR-02 `owner-defined` 可在实现前或实现同 PR 中细化；不一定阻塞源计划修订，但应进入 PR-02 checklist。

---

## Removal / Iteration Plan

1. 删除或改写源计划中仍像“当前执行编号”的旧 `phase4-uiux-pr02/pr03/pr04/pr05` 示例；若保留历史设计语义，必须显式标注“旧 PR-xx，非执行前缀”。
2. 删除 PR-05 的 `legalTargetCountPreview` 名称，统一改为 `legalTargetSummary` 语义。
3. 删除 fallback 模板里“high risk 可由 smoke/golden 替代开放”的宽泛例外，改成“只证明 fallback 可见，不证明可开放新 key player path”。
4. PR-03 special 轴补充双向 invariant 后，不再允许单独用 `specialTierId` 或 `specialTemplateId` 触发 accent。

---

## Additional Suggestions

1. README 的“上游源计划修订状态”可以补一句：源计划正文若出现旧 `phase4-uiux-pr02/pr03/pr04/pr05` 前缀，只有在明确标为旧设计包时才可引用；执行 artifact 一律看 PR 文档。
2. PR-02 truth table 建议增加一个 `delegation contract` 小节，把 owner mode 的“不改 active stack / 不绕过 save gate / 不吞 ESC Backspace”抽出来，减少矩阵重复。
3. PR-03 snapshot contract tests 建议把非法 case 名写入文档，例如 `specialTierWithoutTemplateFails` 和 `specialTemplateWithoutTierFails`。
4. PR-05 `ActionHintModelBuilderTest` 建议把 `known zero` 与 `unknown count` 的截图/golden label 分开，避免只在 unit test 中覆盖。

---

## Suggested Verification

文档修订后建议跑以下只读核对命令：

```bash
rg -n "phase4-uiux-pr02-main-menu|phase4-uiux-pr03-look-mode|phase4-uiux-pr04-ground|phase4-uiux-pr05-card|phase4-uiux-pr05-audio-plan|legalTargetCountPreview|owner-defined|失效风险等级=high" docs/opt docs/opt/ui-pr
```

期望结果:

1. 不再出现旧前缀作为执行 artifact 示例；若出现，必须同段标明“旧设计包/历史语义”。
2. 不再出现 `legalTargetCountPreview`。
3. `owner-defined` 只出现在解释性文字中，不作为测试 expected result。
4. fallback 模板不再允许用 smoke/golden 替代正式资源落地来开放依赖新 key 的正式玩家路径。

---

## Summary

Round6 结论：文档主体已经接近可执行，未发现新的结构性 P0/P1。当前最值得马上修的是两个 P2：源计划旧前缀残留、PR05 `legalTargetCountPreview` 旧字段名。其余 P3 都是低成本的合同收紧，建议在进入实际开发前一并修掉，避免实现阶段再出现 artifact 命名漂移、unknown fact 误判和 fallback 门禁被放宽。

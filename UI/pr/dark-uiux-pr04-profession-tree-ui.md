# Dark UI/UX PR-04 Profession Tree UI

**阶段**: `dark-uiux-pr04-profession-tree-ui`
**优先级**: `P0`
**工作量**: `M`
**前置条件**: PR-01、PR-02 完成；`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md` 的职业树语义已落地，且目标分支能找到 `phase4-v4-pr01` scenario、`TalentSidebarPresenter` 和 `ACTIVE_TALENT_SLOT_CHOICE`。
**资源生成结论**: 默认复用现有资源，不批量生图；职业树 icon 正式重绘放到 PR-06。

## 1. 阶段目标

1. 把正在开发的职业树 UI 接入新的暗黑 UI 框架。
2. 保留 `TalentSidebarPresenter` 作为 presentation authority。
3. 展示三棵职业树、`LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE` 四态、点数、锁定原因、学习预览、断点预览。
4. `ACTIVE_TALENT_SLOT_CHOICE` modal 使用新 UI chrome：`1-4` 替换、`R` 入 reserve、`Esc` 取消。
5. 数字键在三树 sidebar 打开时不改变选择；仅 active slot modal 消费 `1-4`。
6. 阶段范围限定：PR-04 close 时职业树 icon、tree portrait、skill icon 仍可指向现有 manifest entry；PR-06 才统一切到 dark-v1 skill/tree 资源。

“三棵职业树”指当前已选职业的三棵树，不代表全仓总树数。全仓职业覆盖口径以 [README](README.md) 的职业覆盖分类为准：4 个 release playable、2 个 dev playable/report-only、2 个 frozen excluded。

## 2. 硬依赖合同

1. `TalentTreeNodeSnapshot.category` 是 typed enum；client 不做字符串 `valueOf` 解析。
2. `TalentSidebarPresenter` 继续负责 tree header、node row、preview、footer 文案。
3. `DescriptionPresenter.presentTalentTreeNodeLines` 继续负责节点说明；renderer 不重拼文案。
4. 本 PR 不改 `TalentProgression.learnableTalentIds`、starter 数、Tier 门槛、owner metric、longRun 分母。
5. 本 PR 默认复用现有 `icon.skill.*`、`icon.tree.*`、`tree.*`、`portrait.*` 资源；技能图标重绘进入 PR-06，不在本 PR 批量生图。
6. PR-04 依赖 PR-02 已交付的 `ui.frame.modal.body`、`ui.frame.tooltip.body`、`ui.frame.slot.*` 与 `ui.state.*` glyph；缺任一 key 时回到 PR-02 补 registry/manifest，不在 PR-04 临时裸路径。
7. 修改 `TalentSidebarPresenter` 时禁止新增会触发 `TalentProgression` 派生计算的字段或方法；只允许新增纯展示态、layout hint 或已存在 snapshot 的投影字段。

## 3. 前置检查与 Stop Condition

PR-04 开工前必须先执行只读检查：

```bash
rg -n "TalentSidebarPresenter|TalentTreeNodeSnapshot|ACTIVE_TALENT_SLOT_CHOICE" client core game
```

1. 三个合同都存在：继续本 PR，只做 UI chrome/layout/presentation。
2. 任一合同不存在：停止 PR-04，先合入或切到上游职业树分支。
3. presentation 缺字段：回上游职业树合同补，不在本 PR 新增 `core/game` 职业树语义字段。
4. 禁止为了 UI 临时 mock 第二套 talent snapshot。

## 4. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt` | 只补 presentation model 所需字段或 layout hint |
| `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` | 保持 active slot modal 的数字键消费边界 |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | 绘制职业树 panel、node row、preview、modal |
| `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt` | 消费 PR-01 已冻结的四态 tone，不新造颜色 |
| `client/src/test/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenterTest.kt` | 覆盖四态、预览、footer |
| `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt` | 覆盖数字键隔离 |

## 5. UI 改造范围

1. Talent panel header：职业名、职业天赋点、种族天赋点。
2. Tree section：tree icon、tree name、当前选中态、折叠/展开状态。
3. Node row：icon、`ui.state.locked/learnable/active/reserve.icon` 状态 glyph、rank chip、learnable/locked/active/reserve tone。
4. Preview pane：消耗、rank before/after、breakpoint、锁定原因 tokenized lines。
5. Active slot strip：4 个主动槽、空槽高亮、reserve 提示。
6. Active slot choice modal：新暗黑 modal chrome，保留输入语义。

## 6. 非目标

1. 不改职业树规则。
2. 不改 run summary / owner report。
3. 不改 `phase4-v4-pr01` whitebox scenario 的玩法状态；只允许更新 presentation/layout 相关截图或 manual evidence，不允许改 talent 状态、learnable、owner metric 或日志事件断言。
4. 不生成新音频。
5. 不重新设计职业树数据结构，不新增第二套 talent snapshot。

## 7. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.ui.talent.TalentSidebarPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.AsciiRenderModelTest --tests com.ktome.client.render.TileRendererCanvasTest assetLint :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint verifyChanged
```

白盒准备：

```bash
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01
```

PR-04 可以先固化职业树 layout golden；PR-06 切换 skill/tree icon 后必须显式 rebaseline 相关 golden，不能把旧 painterly icon 混入视为最终稳定状态。

## 8. 人工白盒

1. `phase4-v4-pr01` 场景下打开 Talent UI，首屏状态以该 scenario 的 `expected-evidence.json` 为准；人工记录必须覆盖 learned starter、active/reserve/empty slot、learnable 与 locked 节点，不在本文硬编码具体数量。
2. 学习 rank 0 技能时预览能解释点数消耗和 breakpoint。
3. 四槽满时 active-slot modal 的 `1-4 / R / Esc` 文案、焦点和输入行为正确。
4. 新 UI 不改变 `log.talent.learned / rank_up / breakpoint_chosen` 的产生条件。
5. 切到窄窗口时，职业树 panel 不遮挡底部日志的最新一条关键反馈。
6. 必填证据：`dark-uiux-pr04-talent-sidebar-start`、`dark-uiux-pr04-active-slot-choice`、`phase4-v4-pr01` scenario evidence。

## 9. 回滚边界

本 PR 回滚只应影响职业树 UI chrome、layout、renderer 和 golden；如果回滚需要改 `core` 或 `game` 职业树规则，说明 PR 范围已经越界。

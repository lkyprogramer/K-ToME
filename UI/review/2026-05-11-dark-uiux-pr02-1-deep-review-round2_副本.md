# Dark UI/UX PR-02-1 Demo Shell Foundation 深度审查 Round 2

- **审查对象**: `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`
- **基准 DEMO**: `UI/UI-demo.png` (`1672 x 941`)
- **审查日期**: 2026-05-11
- **审查角色**: Roguelike / 类 ToME 开发设计总监 + 系统策划总监 + 玩法体验审查负责人
- **结论**: **有条件通过**。当前文档已经足以把主方向从旧 text-first shell 拉到 DEMO 的框架级结构，但还不能直接声明 gate 闭环可信。开发前必须补齐 `acceptanceContractLint` 覆盖、PR-02-1 owner-scope/verifyChanged 接线，以及 `r01b-ui-shell-chrome` direct cell 的 `subject` 字段，否则资源路径和验收报告会出现假绿或第二真相。

## Findings

### P0

- 未发现会直接破坏 save/replay/schema/core 规则的 P0 问题。本文档的非目标明确禁止修改 core 规则、存档/replay/profile schema、loot/shop economy、item stats 或 input command 语义。

### P1

1. **`acceptanceContractLint` 当前没有真正覆盖 PR-02-1 文档，首个文档 gate 会假绿。**
   - 证据: PR-02-1 要求执行前先跑 `acceptanceContractLint`，并声明 `UI02-1-M01` 到 `UI02-1-M08` 验收矩阵；但 `Phase4V4AcceptanceContractLintTest.uiPrDocs` 只列了 `UI00`、`UI01`、`UI02`、`UI03`、`UI04`、`UI05`、`UI06`、`UI07`，没有 `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`，也没有 `PR-01-1` 细分文档。
   - 影响: 我实际运行 `./gradlew acceptanceContractLint` 通过，但它没有校验 PR-02-1 的 8 行 acceptance matrix、canonical artifact、failure rule 或 repo-relative path。开发者会误以为 PR-02-1 文档合同已经被工具保护。
   - 修复方向: 把 `UI01-1`、`UI02-1` 加入 `Phase4V4AcceptanceContractLintTest.uiPrDocs`，或改成从 `UI/pr/README.md` 扫描 PR 列表后动态校验所有 `dark-uiux-pr*.md`。PR-02-1 最少行数应为 `8`，requirementPrefix 建议用 `UI02-1`。

2. **PR-02-1 可选资源路径缺少 dedicated owner-scope task / verifyChanged 接线，资源报告会与 PR-02 owner 报告冲突。**
   - 证据: PR-02-1 文档要求 `darkManifestCoverageLint -Pktome.darkUiux.ownerPr=PR-02-1` 并把 artifact 写到 `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`；但当前 `tools/build.gradle.kts` 只有 `darkManifestCoveragePr02OwnerScope(ownerPr = "PR-02")`，`VerificationTaskRegistry` 的 dark-uiux ownerTaskPaths 也只包含 `:tools:darkManifestCoveragePr02OwnerScope`。通用 `darkManifestCoverageLint` 同样输出 `dark-v1-manifest-coverage.json`，会和 PR-02 owner-scope 报告共用文件名。
   - 影响: 如果 `r01b-ui-shell-chrome` 被启用，手动跑 PR-02-1 owner-scope 可能通过，但最终 `verifyChanged` 仍会跑 PR-02 owner-scope，并可能覆盖同名 coverage report。PR 描述引用的 coverage artifact 不能证明 PR-02-1 的 `ui.shell.*` owner keys 已覆盖。
   - 修复方向: 新增固定任务，例如 `darkManifestCoveragePr02_1OwnerScope`，参数固定为 `coverageMode=owner-scope`、`ownerPr=PR-02-1`、`ownerContract=UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml`，输出独立文件 `dark-v1-manifest-coverage-pr02-1-owner-scope.json`。同步更新 root allowed tasks、`VerificationTaskRegistry`、`VerificationImpactAnalyzerTest`、PR-02-1 文档 artifact/PR 描述要求。

3. **`r01b-ui-shell-chrome` direct cell 表缺少 `subject`，无法作为 sheet-plan / prompt 真源执行。**
   - 证据: PR-02-1 §6.2 direct cell 表只有 `row / col / targetKey / category / outputName / fallbackKey / aliasOf / consumer / consumerTest`；同节 Column ownership 又要求 `subject` 写入 `UI/sprite-sheets/sheet-plan.yaml`。当前 `scripts/dark_sprite_sheet_contract.py` 对非 reserved cell 明确要求 `subject` 非空。
   - 影响: 一旦需要生成 Round 1B，开发者要么临时手写 subject，形成文档外第二真相；要么 `darkSpriteSheetLint` 在 sheet-plan 校验时失败。这里会直接破坏 PR-02-1 “资源生成前先冻结 direct cell 表”的执行纪律。
   - 修复方向: 在 §6.2 表中增加 `subject` 列，为 15 个 direct cells 填写英文生成主体描述，例如 `forged iron full-screen shell frame with worn stone inset`、`compass rose nav icon in brass and cyan edge glow`。这些 subject 必须能直接复制到 `sheet-plan.yaml.cells[].subject`。

### P2

1. **`DemoShellLayout` 的命名/落点存在轻微自相矛盾，可能让实现绕回旧 layout 容器。**
   - 证据: §2 明确 `Added client/src/main/kotlin/com/ktome/client/render/layout/DemoShellLayout.kt`，§2 增长约束又说 `DemoShellLayout.kt` 是主 layout 合同文件；但 §3 又写“必须新增或收敛一个 typed shell layout，命名可按实现调整”。当前代码里已有 `GameShellLayout` / `InfoSurfaceLayout`，没有 `DemoShellLayout.kt`。
   - 影响: 如果实现者选择“收敛到 InfoSurfaceLayout”，文档中的 canonical artifact、focused test 和命令仍硬编码 `DemoShellLayoutTest`，容易造成 PR 描述与实际实现面不一致。
   - 修复方向: 二选一。推荐强制新增 `DemoShellLayout.kt` / `DemoShellLayoutTest` 作为 PR-02-1 几何合同；如果允许复用 `GameShellLayout`，则把文档中所有 `DemoShellLayout*` 改为明确的 `GameShellLayout demo shell contract`，并同步测试命名。

2. **DEMO 比例表已能防止旧三栏回退，但 rightPanel demo-aspect 区间仍偏宽，可能削弱 map dominance。**
   - 证据: §3.2 要求 demo-aspect 下 `rightPanel.width / viewport.width = 0.28..0.33`，同时 `mapStage.width / viewport.width >= 0.62`。以 `UI/UI-demo.png` 的 1672px 宽度目视估算，右栏更接近约 0.24-0.28，地图/暗区舞台约为 0.65+。
   - 影响: 该约束不会让实现失败，但允许一个右栏更宽、地图不够压场的布局通过机器测试。考虑当前问题正是“现在 UI 和 DEMO 差别太大”，这里最好更保守。
   - 修复方向: 将 demo-aspect 的 rightPanel band 调整为 `0.24..0.30`，或在文档中说明为什么为了中文文本与可读性接受比 DEMO 更宽的右栏。无论哪种，都应保留 `mapStage.width / rightPanel.width >= 2.05`。

### P3

1. **Manual record 字段已经足够，但建议补一个最小模板。**
   - 当前文档已要求 `manualReviewer`、`reviewedAt`、`demoParityVerdict`、`blockingFindings`、`demo-delta checklist`、`remaining gap`、`outOfScopeReductions`。为了减少执行偏差，建议在 `UI/manual-records/_template` 或本 PR 文档下补一个最小 YAML/Markdown 字段模板。

## Requirement Alignment

- Requirement: PR-02-1 必须把局内主 shell 从旧 text-first 三栏提升为 demo-like 结构。
  - Evidence: §1、§3、§5 明确 icon rail、dominant map stage、right panel grid scaffold、bottom hero/action/log/stats deck；§0 Failure Rule 明确截图仍是旧 text-first shell 必须失败。
  - Conclusion: **一致**。

- Requirement: PR-02-1 不能提前吞掉 PR-03/05/06 的资源范围。
  - Evidence: §0 禁止事项、§6、§11 均禁止 item/map/actor/skill/status/quest 资源提前进入 PR-02-1。
  - Conclusion: **一致**。

- Requirement: Optional Round 1B shell chrome 必须走 key registry / sheet plan / manifest / coverage 闭环。
  - Evidence: §6.1、§6.2、§10 定义 owner-scope、owner contract、registry、sheet plan、manifest、coverage gate；但 §6.2 缺 `subject`，当前 repo 也没有 PR-02-1 dedicated coverage task。
  - Conclusion: **部分一致**。

- Requirement: 文档 gate 和最终 `verifyChanged` 必须能保护 PR-02-1。
  - Evidence: §0 和 §10 写了 `acceptanceContractLint`、resource gate、client evidence、`verifyChanged`；但当前 acceptance lint 没列 PR-02-1，dark-uiux impact routing 只跑 PR-02 owner-scope。
  - Conclusion: **部分一致**。

- Requirement: 文档必须为后续 PR-03/05/06 打基础，不让后续资源继续推翻 shell。
  - Evidence: §3.2 数值约束、§4 draw order、§5 slot/grid/hitbox、§8 demo delta checklist 都把 shell geometry 作为当前 PR owner。
  - Conclusion: **一致，但 rightPanel demo-aspect 区间建议再收紧**。

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| PR 文档结构 gate | PR-02-1 进入 `acceptanceContractLint` 首 gate | 部分实现 | `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`; `Phase4V4AcceptanceContractLintTest.uiPrDocs` | task 运行通过但静态列表未包含 PR-02-1 | High |
| Demo shell layout contract | `DemoShellLayout` / typed bounds 保护 nav/map/right/bottom | 部分实现 | PR doc §2/§3; current `GameShellLayout` / `InfoSurfaceLayout` | 文档对“新增 DemoShellLayout”与“命名可调整”同时放行 | Medium |
| Renderer layer order | 14 步 draw order，保留 fog/light no-op marker | 未实现/待实现 | PR doc §4 | 文档合同清楚，当前审查未跑 renderer focused tests | Medium |
| Right panel scaffold | ground loot、equipment、inscriptions、backpack、help 分区和 slot sizes | 未实现/待实现 | PR doc §5.1 | 文档覆盖足够，后续实现需证明不是纯文本列表 | Medium |
| Bottom deck scaffold | hero/action/command/log/stats 分区 | 未实现/待实现 | PR doc §5.2 | 文档覆盖足够，需 focused tests 锁定 zh-CN wrap 与 overlap | Medium |
| Optional r01b resource path | `ui.shell.*` keys + owner contract + manifest coverage | 部分实现 | PR doc §6; scripts `OWNER_PR_PATTERN`; tools coverage tasks | direct cells 缺 `subject`；PR-02-1 owner-scope task/report 缺失 | High |
| Screen coverage matrix | main menu、validation setup、局内 shell 纳入 PR-02-1 labels | 已实现 | `UI/pr/screen-coverage-matrix.md` | 已同步 PR-02-1 labels | Low |
| Manual whitebox | DEMO delta checklist + reviewer verdict | 文档已覆盖 | PR doc §8 | 建议补模板降低执行偏差 | Low |

## 玩法与体验审查

### 核心循环

PR-02-1 的方向正确。DEMO 的价值不是“换一层皮”，而是把探索、装备、技能、日志、角色状态同时放进同一视野。当前文档通过 dominant map stage、右栏 grid scaffold 和底部 hero/action/log deck，把玩家每回合最常看的状态固定在主 shell 内，能为后续 PR-03/05/06 继续填内容提供稳定骨架。

### 战斗体验

文档没有改 combat rule，也不应该改。它真正影响的是战斗信息读取：技能快捷卡、日志 deck、bottom stats summary、右侧装备/铭刻信息可以减少玩家在战斗中切 UI mode 的成本。关键风险在实现阶段：如果 rightPanel 或 bottom deck 仍以长文本列表为主，就会回到当前截图的问题。§8 blocking examples 已经覆盖这个风险。

### 成长与构筑驱动

PR-02-1 只做 shell scaffold，不做最终 item/skill art。这个切分合理，因为构筑驱动需要先有装备/背包/铭刻/技能槽的稳定 hitbox 和视觉位置，再由 PR-03/06 填真实内容。文档的 slot/grid 下限已经能防止 PR-03 再推翻布局。

### 奖励驱动与掉落体验

右栏 `rightGroundLoot`、`rightBackpack`、`rightEquipment` 的 scaffold 是必要地基。文档允许 placeholder，但要求固定 slot bounds，这是正确取舍。后续 PR-03 必须在同一 scaffold 上做 quality、stack、tooltip 和 shop affordance，不应再重写主 shell。

### 探索与新鲜感

中央 `mapStage` 保持 dominant 是探索体验的第一优先级。文档已有 map width ratio 与 map/right ratio，但 demo-aspect 的 rightPanel band 建议略收窄，避免右栏在宽屏上吞掉地图的压场感。

### 新手体验与信息反馈

左侧 nav rail 的“icon-first + compact selected hint”能明显改善当前大段任务文字挤压地图的问题。文档还保留 accessibility text，不把辅助文本删除，这个边界正确。需注意如果不生成 `ui.shell.nav.*`，manual record 必须把 icon art quality 判为 deferred，不能写成 complete。

### 系统耦合与体验断层

文档明确 layout 不读 gameplay rule、不拼 locale 文案、不解析 manifest；renderer 继续通过 `VisualManifestResolver` 和 `ClientTextureRepository` 消费资源。这符合 `client` 只负责表现编排的边界。当前最大的系统断层不在 layout 思路，而在验证接线：PR-02-1 作为细分 PR 已进文档索引，但工具链还停留在 PR-02 owner-scope。

## 当前阶段必须解决的问题

1. **把 PR-02-1 加入 `acceptanceContractLint` 覆盖。**
   - 为什么必须当前修: 文档把 `acceptanceContractLint` 作为第一 gate，当前假绿会误导后续开发。
   - 为什么不能推迟: 等实现后再补，任何 acceptance matrix 缺口都已经进入开发分支，返工成本更高。
   - 修复方向: 更新 `Phase4V4AcceptanceContractLintTest.uiPrDocs` 或改成动态扫描 PR index。
   - 优先级: P1。

2. **为 PR-02-1 owner-scope 新增 dedicated coverage task 和 verifyChanged routing。**
   - 为什么必须当前修: 一旦启用 `r01b-ui-shell-chrome`，PR-02-1 资源 coverage 是 close gate，不是可选 polish。
   - 为什么不能推迟: 当前 `verifyChanged` 只跑 PR-02 owner-scope，最终报告会覆盖或误指向 PR-02。
   - 修复方向: 新增固定 task、独立 report file、impact routing 测试和文档 artifact 路径。
   - 优先级: P1。

3. **补齐 §6.2 direct cell `subject` 列。**
   - 为什么必须当前修: sheet-plan lint 要求 subject，prompt 生成也需要 subject；缺失会迫使实现者临场造词。
   - 为什么不能推迟: 资源生成一旦开始，subject 就会变成资源语义真源的一部分，不能散在实现备注里。
   - 修复方向: 给 15 个 direct cells 增加可直接复制到 `sheet-plan.yaml` 的英文 subject。
   - 优先级: P1。

4. **统一 `DemoShellLayout` 命名和落点。**
   - 为什么必须当前修: 这是本 PR 的几何 authority；允许“命名可调整”会削弱 canonical artifact 与测试命令的可执行性。
   - 为什么不能简单推迟: 实现一旦落进现有 `InfoSurfaceLayout`，后续 review 很难判断 PR-02-1 shell geometry 是否真的有独立 owner。
   - 修复方向: 推荐强制新增 `DemoShellLayout.kt` / `DemoShellLayoutTest`。
   - 优先级: P2。

## Removal/Iteration Plan

当前审查对象是 PR 文档，不建议删除已有代码或资源。

需要分阶段收敛的只有一个 iteration item：

| Item | Details |
| --- | --- |
| PR-02-1 owner-scope coverage 接线 | 先新增 `darkManifestCoveragePr02_1OwnerScope` 与独立 report，再把 `VerificationTaskRegistry` dark-uiux ownerTaskPaths 从只含 PR-02 扩展到覆盖 PR-02 与 PR-02-1；最后更新 PR 文档中的 artifact 路径。 |
| 回滚 | 如果 PR-02-1 最终不生成 `r01b-ui-shell-chrome`，可以不引入该 task，但必须在 manual record 中写 `r01b: NOT_USED`，并说明 `verifyChanged` 不承担 PR-02-1 owner-scope resource coverage。 |

## Additional Suggestions

1. 将 demo-aspect 的 `rightPanel.width / viewport.width` 从 `0.28..0.33` 调整到 `0.24..0.30`，或补一段“为中文文本可读性放宽右栏”的设计说明。
2. 给 `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` 增加模板字段，至少包含 `manualReviewer`、`reviewedAt`、`demoParityVerdict`、`blockingFindings`、`goldenLabels[]`、`r01b`、`remainingGaps[]`、`outOfScopeReductions`。
3. 如果 `r01b-ui-shell-chrome` 被使用，PR 描述不要只引用 `dark-v1-manifest-coverage.json`，应引用 PR-02-1 独立 coverage artifact，避免 reviewer 读到 PR-02 的旧报告。

## Suggested Verification

已运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：通过。注意：这次通过只能证明现有 acceptance lint 自身通过，不能证明 PR-02-1 文档已被该 lint 覆盖，原因见 P1 finding 1。

建议在修复文档/gate 后运行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

如果启用 `r01b-ui-shell-chrome`，建议追加：

```bash
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint \
  -Pktome.darkUiux.requireFullGrid=true \
  -Pktome.darkUiux.ownerContract=UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml \
  -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl
./gradlew darkManifestCoveragePr02_1OwnerScope
```

实现 PR-02-1 后再跑：

```bash
./gradlew :client:test \
  --tests com.ktome.client.render.DemoShellLayoutTest \
  --tests com.ktome.client.render.GameShellLayoutTest \
  --tests com.ktome.client.render.InfoSurfaceLayoutTest \
  --tests com.ktome.client.render.TileRendererCanvasTest \
  --tests com.ktome.client.screen.StandaloneScreenLayoutTest \
  --tests com.ktome.client.screen.MainMenuScreenTextTest \
  --tests com.ktome.client.assets.ManifestResolveTest
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint verifyChanged
git diff --check
```

本轮未运行 client focused tests、golden、resource gate、maintainabilityLint、verifyChanged，因为当前任务是 PR 文档审查，且 PR-02-1 实现面尚未按本文档完成。

## Summary

PR-02-1 文档的设计方向是正确的：它已经把 DEMO 的核心框架拆成可实现的 typed regions、尺寸约束、draw order、slot/grid scaffold、manual demo-delta checklist 和回滚边界。作为“为后续 UI 改造打地基”的文档，它比 PR-02 后直接补资源更稳。

但当前不能直接按“文档已可闭环”推进。必须先修三个执行合同问题：`acceptanceContractLint` 纳入 PR-02-1、PR-02-1 owner-scope resource coverage 进入 dedicated task / verifyChanged、`r01b` direct cell 表补 `subject`。补完后，这份文档可以作为 PR-03/05/06 继续填资源前的可靠 shell foundation。

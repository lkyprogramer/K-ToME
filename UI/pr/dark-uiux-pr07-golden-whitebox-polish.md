# Dark UI/UX PR-07 Golden Whitebox Polish

**阶段**: `dark-uiux-pr07-golden-whitebox-polish`
**优先级**: `P1`
**工作量**: `M`
**前置条件**: PR-06 完成。
**资源生成结论**: 不新增大批量资源，只允许修复 rejected cell 或明显破坏一致性的单点资源。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)，是 dark UI/UX 的最终 golden / packaged app 白盒收口。执行前先跑 `acceptanceContractLint`，再跑 final coverage、client evidence、packaged app 白盒和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI07-M01` | §3 golden / manual record scope | `client` / `docs` | golden label owner check | `goldenScreenshot`, `clientSmoke` | `client/build/reports/golden/` | `required` |
| `UI07-M02` | §4 完成定义 | `tools` | `acceptanceContractLint` | `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full`, `verifyChanged` | dark coverage artifact | `required` |
| `UI07-M03` | §5 性能与 atlas 决策 | `client` / `tools` | performance / atlas evidence check | `clientSmoke`, `goldenScreenshot` | `build/reports/verification/` | `N/A` |
| `UI07-M04` | §6 packaged app 白盒 | `client` / `docs` | packaged app launch checklist | `:client:packageMacApp`, packaged app whitebox | `UI/manual-records/` | `required` |
| `UI07-M05` | §7 验证命令 | `tools` | `assetLint`, `styleLint`, `manifestLint`, `localeLint`, `contractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |
| `UI07-M06` | §9 回滚边界 | `docs` | doc-vs-implementation self-audit | PR close review | `UI/manual-records/` | `N/A` |

### Gate Budget

预计重型任务：resource lint 全套、`darkManifestCoverageLint final-full`、`:client:clientSmoke`、`:client:goldenScreenshot`、`:client:packageMacApp`、packaged app 白盒、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-07 是全局 UI、golden、manual evidence 和 packaged app 最终收口。

### Canonical Artifact

canonical artifact 固定为 final-full coverage artifact、client golden、packaged app command / runtime home / evidence dir、manual record 和 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。packaged app 证据路径必须 repo-relative 或明确占位，不得写本机绝对路径。

### Failure Rule

如果 PR-07 发现玩家可见 rejected cell，不能在 PR-07 静默修补；必须回到 PR-06 或拆单独资源修复 PR。packaged app 白盒只有在纯文档或纯 golden metadata 且无 package-facing 改动时才允许跳过，并必须写明豁免原因。

## 1. 阶段目标

1. 统一刷新 golden、manual record、contact sheet QA report。
2. 必跑 packaged app 白盒验收。
3. 评估单 PNG 加载成本，决定是否进入 atlas/region manifest 后续方案。
4. 输出最终 doc-vs-implementation audit，记录 `UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/*` 与实际实现的差异；PR-07 默认不修订上游合同，除非发现的是 PR-07 自身引入的文档错误。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `client` golden baseline | 更新新 UI 期望截图和 hash |
| `UI/sprite-sheets/` | 补齐 QA report 和 rejected cell 处理结果 |
| `docs` 或 `UI` manual record | 记录人工白盒证据 |
| `client` render tests | 补遗漏的无重叠、fallback、modal 层级断言 |
| `UI/review/` | 输出最终 doc-vs-implementation audit |

## 3. 范围

1. 不再大批量生成新资源，只允许修复 PR-06 coverage artifact 已记录的 `rejected` cell。
2. 补齐 `dark-uiux-prNN-*` golden label owner。
3. 补齐职业树、装备背包、地图、战斗 HUD、Look/Inspect、modal 的人工验收。
4. 只产出 `UI/review/dark-uiux-final-doc-implementation-audit.md` 对照清单；如发现实际实现与 PLAN/ART_STYLE_BIBLE 矛盾，记入 audit 并开后续修订 PR，不在 PR-07 夹带改上游合同。

Rejected cell 处理规则：

1. `r09-fallback-debug` / `r09-rejected-polish` owner 仍属于 PR-06；PR-07 不重新拥有这些 sheet。
2. PR-07 只允许返修 PR-06 coverage artifact 中记录的非玩家可见 rejected/polish cell。若发现玩家可见 rejected cell，说明 PR-06 final-full 未完成，必须回到 PR-06 或单独资源修复 PR。
3. PR-07 返修 rejected cell 时，必须修订 PR-06 产出的 coverage artifact，记录原 cell hash、新 cell hash、替换原因和对应 golden/manual evidence。
4. 如返修数量超过少量单点，应回到 PR-06 或新开资源 PR，不在 PR-07 扩成新资源批次。

## 4. 完成定义

1. 新 UI 在 `1280x800` 与最小支持窗口下无重叠。
2. 职业树、装备、背包、HUD、地图、modal 均有 golden 或人工证据。
3. 玩家可见资源不存在旧风格混入；例外必须写明是 debug/history fallback。
4. atlas 决策写清：继续单 PNG，或开新 PR 引入 atlas/region manifest。
5. `verifyChanged`、client golden、resource lint、style lint 全部与本 PR 文档一致。

无重叠判定使用双口径：

1. `GameShellLayout` bounds 数学证明主要区域不相交。
2. golden / canvas hitbox 检查证明文本、slot、modal、tooltip 不互相覆盖。

## 5. 性能与 atlas 决策

1. 如果单 PNG 加载、纹理切换或内存没有实测问题，不引入 atlas，避免扩大 renderer schema。
2. 如果实测出现明显性能问题，新开独立 atlas PR；不得在本 PR 半途改 `VisualManifestEntry` schema。
3. atlas 决策至少记录：截图数量、加载耗时、纹理数量、峰值内存、是否影响 client smoke。
4. 阈值以 [UI/PLAN.md](../PLAN.md) 的 Atlas 决策阈值为准。

## 6. Packaged App 白盒

PR-07 涉及全局 UI 与运行时资源，macOS packaged app 验收默认必跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr07-final-ui
```

如果 `dark-uiux-pr07-final-ui` scenario 尚不存在，本 PR 必须新增或复用等价 scenario，并在 PR 文档中写清 scenario id。白盒流程必须遵循 [docs/computer-use-whitebox-flow.md](../../docs/computer-use-whitebox-flow.md)，不得只用 `:client:packageMacApp` build 结果替代启动验收。

Packaged runbook：

1. 运行 `:client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr07-final-ui`。
2. 确认生成 `build/whitebox/dark-uiux-pr07-final-ui/launch-packaged-app.sh`、`cua-runbook.md`、`manual-record-template.md`、`expected-evidence.json`、`app-executable.sha256`。
3. 执行 `build/whitebox/dark-uiux-pr07-final-ui/launch-packaged-app.sh`，它必须使用隔离 runtime home：`build/whitebox/dark-uiux-pr07-final-ui/runtime-home`。
4. 按 `cua-runbook.md` 打开主界面、背包/装备、职业树、状态/任务、地图与 modal 场景。
5. 使用 `scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --out build/whitebox/dark-uiux-pr07-final-ui/evidence/<step>.png` 或等价 Computer Use 截图保存证据。
6. 对比 packaged app 证据与 debug client golden/manual evidence，不允许 packaged app 出现资源缺失、旧风格 residue 或布局重叠。
7. 关闭 packaged app，保留 `app.log`、`app.pid`、截图、metadata、sha256 和 manual record。

证据必须记录：

1. packaged app 命令与退出码。
2. runtime home / app bundle repo-relative 路径。
3. 启动日志路径。
4. 截图 evidence dir。
5. manual record：`UI/manual-records/dark-uiux-pr07-packaged-app.md`。

只有本 PR 被拆成纯文档或纯 golden metadata 且没有 package-facing 改动时，才允许跳过，并必须在 PR 描述中写明豁免原因。非 macOS 执行环境只能跳过 packaged app 启动链路，不能跳过 debug client golden、coverage artifact 和手工证据要求。

## 7. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint localeLint contractLint :client:clientSmoke :client:goldenScreenshot maintainabilityLint verifyChanged
./gradlew syncPhase2Manifests manifestLint
```

本 PR 的 dark gate 必须使用显式 final-full 命令：

```bash
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full
```

用于确认 PR-07 polish 没有重新引入旧风格或 rejected cell。

## 8. 人工白盒

1. 主界面：左栏、地图、右栏、底部 HUD、快捷键提示无重叠。
2. 背包装备：空态、满格、品质、数量 badge、tooltip 均可读。
3. 职业树：三树、四态、预览、主动槽 modal、数字键边界正确。
4. 战斗：telegraph、状态、技能、日志反馈清楚。
5. 窄窗口：文本截断/换行稳定，不出现 UI 控件互相覆盖。
6. 必填证据：final doc-vs-implementation checklist、coverage artifact、packaged app manual record。

## 9. 回滚边界

本 PR 只允许回滚 golden、QA report、manual evidence、polish 级 renderer 修复和 rejected cell 替换。若需要回滚核心布局或资源 manifest 大段内容，应回到对应 PR 分支处理。

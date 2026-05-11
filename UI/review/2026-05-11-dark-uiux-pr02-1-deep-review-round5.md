# Dark UI/UX PR-02-1 Demo Shell Foundation Review (Round 5)

目标文档：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`（734 行版，较 Round-3 553 行版新增 ~181 行）

上一轮报告：`UI/review/2026-05-11-dark-uiux-pr02-1-deep-review-round3.md`

审查规范：`docs/review/rule/pr-level-review-standard.md`

审查范围：

- 上游入口：`UI/PLAN.md`、`UI/pr/README.md`、`UI/pr/development-governance.md`、`UI/pr/screen-coverage-matrix.md`、`UI/UI-demo.png` (1672×941)。
- 当前代码锚点：`scripts/dark_sprite_sheet_contract.py`、`scripts/verify_dark_key_registry.py`、`tools/build.gradle.kts`、`tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt`、`tools/src/test/kotlin/com/ktome/tools/verification/VerifyChangedBuildContractTest.kt`、`client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt`、`tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt`、`UI/sprite-sheets/sheet-plan.yaml`、`UI/sprite-sheets/key-registry.yaml`。
- 本轮重点：(a) Round-3 P1/P2/P3 关闭状态；(b) 文档新增 §3.1.1、§5.2 actionDeck 精化、§6.1 routing 子项 1-6、§8.1.1 packaged whitebox scenario contract、§10 manual record YAML template 后引入的细粒度缺陷；(c) "可开发文档完成定义"（标准 §8）逐项 sanity check；(d) 撤换前一轮（其他视角的 Round-4 草稿）已不成立的判断。
- 审查口径：纯静态比对，未运行 `acceptanceContractLint` 或任何 client gate。所有"未运行"在 §Suggested Verification 显式列出。

预检摘要：

- 当前分支：`codex/dark-uiux-pr02-ui-chrome-sprite-pilot`（基于 `git status`）。
- 工作树非干净，但被分类为 PR-02 / PR-01-1 已合入改动 + PR-02-1 文档更新，不进入本轮 review 范围。
- 稳定合同未被触碰：core 规则、save/replay/profile schema、loot/shop economy、item stats、input command 语义在 §0 与 §11 双向声明禁止变动。
- 触碰合同：client UI shell layout & renderer、visual manifest、dark-v1 sprite pipeline、`verifyChanged` impact routing、whitebox evidence。
- 主要风险：第二真源（§6.1 ownerExpectedKeys 双源、§8.1.1 path 派生）、golden screenshot 被当 focused test 兜底（§4.2 rule 7 vs `DemoShellRenderer` 无专属测试）、跨 PR artifact 一致性（PR-02 owner-scope reportFileName rename）。
- 未执行：`acceptanceContractLint`、client focused tests、resource gate、verifyChanged；纯静态读取。

## Round-3 闭环复核

| Round-3 编号 | 主题 | 当前状态 | 证据锚（文件:行） |
| --- | --- | --- | --- |
| 2.A | §6.2 缺 `subject` 列 | 已解决 | line 361-377 加 `subject` 列 + 15 cells 填齐英文生成主体 |
| 2.B | manifest coverage 文件名/任务名互相矛盾 | 已解决 | §10 line 547 改为 `darkManifestCoveragePr02_1OwnerScope`；§6.1 item 2 强制 PR-02 rename；§12 line 700 引用 PR-02-1 owner-scope filename |
| 2.C | §3.2 demo-aspect bottomDeck 数值/比例自矛盾 | 已解决 | line 227 改为 `>= 208 px`（与 `0.22 × 941 = 207.02` 对齐） |
| 2.D | §6.1 OWNER_PR_PATTERN 目标正则未明示 | 已解决 | line 346 写入 `^PR-\d{2}(?:-\d+)?$`；line 138 列出 PR-02-1 / PR-02-1-1 / pr-02-1 三条 regression |
| 2.E | nav.compass / nav.book 共指同一 fallbackKey | 已解决 | line 376 `nav.book` fallback 改为 `ui.control.copy.icon`（`UI/sprite-sheets/key-registry.yaml:165` 已确认该 fallback target 存在） |
| 3.A | layout ownership 边界不清 | 已解决 | §3.1.1 Layout Ownership Matrix 新增 (line 202-208) |
| 3.B | `modalSafeBounds` 缺数值边界 | 部分解决 | §3.2 line 233-234 加 width/height 比例带；锚点/中心点仍未声明（见 P2-2） |
| 3.C | §5.2 actionDeck 水平比例稳定不可断言 | 已解决 | line 315 精化为 "card.width `>= 96 px` 固定、slot 间距 `>= 12 px` 固定、整组水平居中、`cardCount` 超出 `1..4` fail layout test" |
| 3.D | M05 fastCheck 与 §10 命令不严格匹配 | 已解决 | line 23 fastCheck 字符串现含三个 `-P` 参数 |
| 3.E | §6.3 NOT_USED 与 §5.3/§8.2 冲突 | 已解决 | §6.3 line 396-398 直接禁止 NOT_USED 路径，强约束 r01b 五个 nav icon "实际消费且视觉可区分" 才能写 pass |
| 3.F | §3.1 outerFrame/navRail "兄弟区域" 描述歧义 | 已解决 | line 186 改写为 "并列 region；不得绘制覆盖 navRail.bounds 的装饰层" |
| 3.G | §5.1 rightInscriptions `5-8` 含义未释义 | 已解决 | line 294 改为 "至少 4 行 slot，对应铭刻槽位编号 `5..8`；编号 `1..4` 保留给 bottom actionDeck 的 active talent slots" |
| 4.A | §4.1 step 8 "cursor no-op group" 术语模糊 | 已解决 | line 260 改为 "fog / light masks recording marker; no-op until PR-05 implements lighting/fog art" |
| 4.B | §6.2 nav.gear fallback `ui.hud.warning.icon` 语义偏离 | **未解决** | line 377 仍保留 `ui.hud.warning.icon`（见 P3-3） |
| 4.C | §2 raw sheet hash 维护者未声明 | 已解决 | line 137 加入 "同步更新 `dark-v1-pr02-1-sprite-map-report.jsonl` 中该 sheet 的 `rawSheetHash`" |
| 4.D | M07 artifact 仅列 manual record | 已解决 | line 25 artifact 列扩展为 manual record / golden / packaged evidence 三处 |

闭环结果：5/5 P1 已解决；7/7 P2 已解决（modalSafeBounds 数值已补但锚点新发现）；3/4 P3 已解决，nav.gear fallback 转入 Round-4 P3-3。同时撤换前一份 Round-4 草稿中已不成立的论点（详见 §"前一轮草稿撤换"）。

## Findings

### P0

无。Core 规则、save/replay/profile schema、loot/shop economy、item stats、input command 语义在 §0 line 102 与 §11 line 686-690 双向声明禁止变动。

### P1

#### P1-1 §6.1 item 5 verifyChanged 静态合同覆盖测试 "二选一" 会留盲点

证据：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:354` "root `build.gradle.kts` 的 `verifyChangedTaskPaths` 必须包含 `:tools:darkManifestCoveragePr02_1OwnerScope`，并由 `VerifyChangedBuildContractTest` **或** `VerificationImpactAnalyzerTest` 覆盖"。

结合：

- `tools/src/test/kotlin/com/ktome/tools/verification/VerifyChangedBuildContractTest.kt`（验证 root `verifyChangedTaskPaths` 静态成员）
- `tools/src/test/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzerTest.kt`（验证 changed surface → 触发 task 的 impact routing）

两测试语义正交：前者保证 task path 出现在 root 静态列表中，后者保证给定 changed file 时被 routing 请求。

影响：实施者按字面"或"只补 `VerificationImpactAnalyzerTest`（变更最小），即可让 `acceptanceContractLint` 与 review pass；但 `verifyChangedTaskPaths` 静态列表若被误删，仅 `VerificationImpactAnalyzerTest` 不能发现（因为 impact routing 单独维护任务清单）。反之只补 `VerifyChangedBuildContractTest` 则放过 dark UI/UX 变更未触发 PR-02-1 owner-scope 的可能。两个合同必须同时校验。

修复方向：把 §6.1 item 5 的 "或" 改为 "和"，并显式写明：

```
- VerifyChangedBuildContractTest 断言 :tools:darkManifestCoveragePr02_1OwnerScope 出现在 root build.gradle.kts 的 verifyChangedTaskPaths 字面量列表中。
- VerificationImpactAnalyzerTest 断言 dark UI/UX 资源 / manifest / pipeline script 变更触发 PR-02 与 PR-02-1 两个 owner-scope task，且不触发裸 darkManifestCoverageLint。
```

推荐测试：`VerifyChangedBuildContractTest` 新增字面量断言；`VerificationImpactAnalyzerTest` 在已有 PR-02 owner-scope routing 案例旁追加 PR-02-1 case，避免任何一侧静默漂移。

#### P1-2 Canonical Artifact 列出 `DemoShellRenderer.kt` 但 §4.2 rule 7 禁止 golden 替代 focused test，文档未声明其专属测试

证据：

1. §0 Canonical Artifact 第 9 项（`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:60`）列出 `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`。
2. §4 line 244-245 明确："`DemoShellRenderer` 负责 shell chrome、nav rail、right panel scaffold、bottom hero/action/log deck 的绘制"。
3. §4.2 rule 7（line 281）："goldenScreenshot 只能作为视觉回归，不替代 bounds / owner key focused tests"。
4. §0 Canonical Artifact 测试相关项只列 `DemoShellLayoutTest.kt`、`TileRendererCanvasTest.kt`、`GoldenScreenshotHarnessTest.kt`，没有 `DemoShellRendererTest.kt` 或对 `DemoShellRenderer` 内部 dispatch 的明确覆盖声明。
5. §9 line 511-512 第 12 条 `TileRendererCanvasTest` 断言只覆盖 hero portrait bounds / right slot side / backpack grid / owner-bounds draw call / `rightPanelGridDoesNotUseNavOrHeroKeysForItemSlots`，没有覆盖 `DemoShellRenderer` 自身的 chrome / nav / right panel / bottom deck dispatch 路径。

影响：`DemoShellRenderer.kt` 是 PR-02-1 引入的新 renderer 文件，承载 §4.1 step 2、9、10、11a-d 共 7 个 layer 的实际绘制。若仅由 `TileRendererCanvasTest`（对 orchestrator 的 owner-bounds 黑盒断言）覆盖，则任何破坏 `DemoShellRenderer` 内部分发但仍把像素画在正确 bounds 内的 regression（例：把 nav rail 与 right panel chrome 互换 key、把 hero card frame key 错绑 action deck）都会绕过 focused test，只能在 golden screenshot 阶段被发现——直接违反 rule 7。

修复方向（二选一，作者择优）：

1. **优选**：在 §0 Canonical Artifact 列表第 12 项后新增 `client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`，并在 §9 验收标准新增条目断言 `DemoShellRenderer` 对每个 step (2/9/10/11a-d) 的 manifest key 调用顺序和 key 绑定。
2. **次选**：在 §4.2 显式声明 "`DemoShellRenderer` 由 `TileRendererCanvasTest` 直接覆盖，断言每个 step 内绑定的 `ui.shell.*` key 正确"，并相应在 §9 第 12 条扩展该 test 断言列。

推荐测试：新建 `DemoShellRendererTest` 用 fake batch 验证每个 draw step 的 manifest key、调用次序、bounds。或在 `TileRendererCanvasTest` 中加 key-binding map 断言。

#### P1-3 §6.1 item 2 修改 `darkManifestCoveragePr02OwnerScope.reportFileName` 缺迁移与 closed PR 引用一致性声明

证据：

1. `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:351` "`darkManifestCoveragePr02OwnerScope` 的 `reportFileName` 必须改为 `dark-v1-manifest-coverage-pr02-owner-scope.json`，禁止继续与裸 `darkManifestCoverageLint` 共用 `dark-v1-manifest-coverage.json`"。
2. `tools/build.gradle.kts:687` 当前 `darkManifestCoveragePr02OwnerScope` task 已存在，但 reportFileName 仍是默认 `dark-v1-manifest-coverage.json`（与裸 `darkManifestCoverageLint` 共用）。
3. PR-02（已 merge 的 PR）的 PR 描述、`build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` 链接、`UI/manual-records/dark-uiux-pr02-*.md` 中的 coverage 引用，全部指向旧文件名。
4. §2 "Deleted / replaced 清单"（line 155-160）只列局内 shell / 右栏 / 底部 / 左栏四种 UI surface 替换，未列 PR-02 coverage 文件名替换。
5. §13 回滚边界（line 716-727）也未声明 reportFileName rename 的回滚责任：单 commit 回退 PR-02-1 后，`darkManifestCoveragePr02OwnerScope` 的 reportFileName 是否回到默认 `dark-v1-manifest-coverage.json`？

影响：

1. **跨 PR artifact 一致性破坏**：PR-02 已 close，但其 review report / manual record / PR 描述中所有 `dark-v1-manifest-coverage.json` 引用，在 PR-02-1 merge 后将指向 "被 PR-02-1 覆写后" 的 PR-02 owner-scope 数据（同源、不同 path）。reviewer 历史溯源时可能误把 "找不到 dark-v1-manifest-coverage.json" 判定为 PR-02 evidence 丢失。
2. **回滚边界不清**：若 PR-02-1 需要单 commit 回退（§13 强约束），reportFileName 是否一起回退？文档未声明。
3. **`removalOwner` 不明**：标准 §8 第 11 条要求"所有删除文件、废弃 manifest 字段、废弃 locale token、废弃 golden label 和废弃 fixture 都已写入 Deleted 清单和 `removalPlan`"。旧 `dark-v1-manifest-coverage.json` 实际上是被废弃（不再由 PR-02 owner-scope task 写入），但未进入 Deleted 清单。

修复方向：

1. §2 "Deleted / replaced 清单"新增一行：`dark-v1-manifest-coverage.json (作为 PR-02 owner-scope output)` → `replace with dark-v1-manifest-coverage-pr02-owner-scope.json`，`removalOwner=PR-02-1`，`Regression Scan` 列指向 `VerificationImpactAnalyzerTest` 与 `VerifyChangedBuildContractTest`。
2. §6.1 item 2 末尾追加："旧 `dark-v1-manifest-coverage.json` 文件名仅在裸 `darkManifestCoverageLint` 路径下继续可见；PR-02 owner-scope 历史引用由本 PR 描述显式声明已迁移到新文件名。"
3. §13 回滚边界第 1-6 条之后增列："若回滚本 PR，`tools/build.gradle.kts` 中 `darkManifestCoveragePr02OwnerScope.reportFileName` 同步回退到默认值；PR-02 历史 artifact 不必恢复，owner-scope task 重新生成时即覆盖。"

推荐测试：在 `VerifyChangedBuildContractTest` 中加配对断言：`tasks.named("darkManifestCoveragePr02OwnerScope").reportFileName == "dark-v1-manifest-coverage-pr02-owner-scope.json"`、`tasks.named("darkManifestCoveragePr02_1OwnerScope").reportFileName == "dark-v1-manifest-coverage-pr02-1-owner-scope.json"`，禁止任一方退回默认值。

### P2

#### P2-1 §6.1 item 1 `ownerExpectedKeys` 两源一致性缺优先级声明

证据：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:339-343` "`ownerExpectedKeys` 必须与以下两类完全一致：1) `UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml.requiredCells[].targetKey`。2) §6.2 的 direct cell keys。"

影响：实施期 yaml 与 §6.2 漂移时，文档未声明 primary source。标准 §4.2 Pass 1 第 5 条明确"source of truth：每个字段只能有一个 primary source"。两源声明会让实施者在两份文件先后被编辑后无法判定哪份是真源。

修复方向：在 §6.1 item 1 加一行："`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md §6.2` 是 direct cell 真源；`pr02-1-owner-keys.yaml.requiredCells[].targetKey` 由 §6.2 派生，任一漂移必须修 §6.2，再同步 yaml；不得反向修改 yaml 让 §6.2 适配。"

推荐测试：`DarkSpriteSheetPipelineScriptTest` 或新增 `DarkUiuxPr02_1OwnerContractParityTest`，断言 yaml `requiredCells[].targetKey` 与 §6.2 表格 row 0..3 col 0..3 的 15 个 targetKey（去掉 1 个 reserved）字面一致。

#### P2-2 §3.2 `modalSafeBounds` clamping 锚点与中心源未明示

证据：

1. `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:200` "modal 默认锚到 `mapStage` 中心，但最终 bounds 必须被 clamp 到该区域"。
2. §3.2 line 233-234 给 `modalSafeBounds.width / viewport.width` 与 `modalSafeBounds.height / viewport.height` 比例带。
3. §3.2 line 241 "`modalSafeBounds` 必须与 `mapStage`、`rightPanel`、`bottomDeck` 同源计算"。

影响：缺以下三项明确：

1. modal 中心点最终位置：是 `viewport.center` 还是 `mapStage.center`？line 200 说"锚到 mapStage 中心"但 clamp 后中心点可能漂移。
2. clamp 算法：先按 `mapStage.center` 放置再 clamp 到 `modalSafeBounds`，还是直接居中在 `modalSafeBounds`？
3. `modalSafeBounds` 是否允许覆盖 `navRail` 边缘 1 列像素？line 200 提到"不遮挡关键 HUD 时仍可读"但未量化。

实施者可能选 `viewport.center` + 全屏 80% bounds，结果 modal 完全覆盖 navRail（违反 §11 line 691 "辅助文本必须保留"）。

修复方向：§3.2 modal 段加："`modalSafeBounds` 几何中心 = `mapStage.center`；`modalSafeBounds.left >= navRail.right`、`modalSafeBounds.right <= rightPanel.left`、`modalSafeBounds.bottom <= bottomDeck.top`；满足后再按 width/height 比例带计算；不得允许任一边覆盖 chrome region 边界。"

推荐测试：`DemoShellLayoutTest` 在 standard / minimum / demo-aspect 三 profile 下断言 `modalSafeBounds` 不与 `navRail` / `rightPanel` / `bottomDeck` 重叠且中心点 = `mapStage.center`。

#### P2-3 §5.1 line 308 `ui.shell.*` 滥用约束仅覆盖 nav/hero，其他 9 个 shell key 未声明边界

证据：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:308` "`ui.shell.hero_crest.placeholder` 只能用于 `heroCard`，`ui.shell.nav.*` 只能用于 `navRail`；二者不得作为地面物品、装备、铭刻、背包 slot 的 placeholder"。

影响：§6.2 表列出 15 个 PR-02-1 keys，但 line 308 仅约束 6 个 (`ui.shell.hero_crest.placeholder` + 5 个 `ui.shell.nav.*`)。其余 9 个未约束滥用：

- `ui.shell.outer_frame`
- `ui.shell.map_stage.frame`
- `ui.shell.nav_rail.frame`
- `ui.shell.nav_button.active`
- `ui.shell.hero_card.frame`
- `ui.shell.action_deck.frame`
- `ui.shell.log_deck.frame`
- `ui.shell.right_section.divider`
- `ui.shell.command_hint.plate`

`ui.shell.action_deck.frame` 被误用作 `rightBackpack` 容器、或 `ui.shell.command_hint.plate` 被用作 inscriptions slot 时，文档无 hard rule 拒绝。

修复方向：§5.1 line 308 改写为："`ui.shell.*` 各 key 只能用于 §6.2 表 consumer 列声明的 typed region；任何在其他 region 使用 `ui.shell.*` key 的行为视为 fail，`TileRendererCanvasTest` 必须按 (key, region) pair 验证 owner-bounds 与 key 绑定。"

推荐测试：扩展 §9 line 511 的 `rightPanelGridDoesNotUseNavOrHeroKeysForItemSlots`，新增 `shellKeysBindToConsumerRegionOnly` 断言，按 §6.2 consumer 列字面表生成 expected map，强制 (key → region) 绑定一一对应。

#### P2-4 §8.1.1 `windowSize=1280x800` 是否适用 5 个 screenshot label 未声明

证据：

1. `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:439` `windowSize | 1280x800`。
2. §8.1 表列 5 个 screenshot label，其中 `dark-uiux-pr02-1-demo-main-menu` 与 `dark-uiux-pr02-1-demo-validation-setup` 是 standalone surface，与局内 shell viewport 共享同 viewport 还是独立 viewport 未声明。
3. §3.2 列三 profile（standard 1280×800、minimum 1024×768、demo-aspect 1672×941）；packaged whitebox 是否仅 capture standard，其他 profile 仅在 `DemoShellLayoutTest` 中断言？

影响：实施者可能为 demo-aspect / minimum profile 也跑 packaged whitebox，导致 `evidenceDir` 出现重复 screenshot；或反之，将 standalone screenshot 误捕在 standalone 默认 viewport（可能不是 1280×800），与 §7 line 414-416 几何约束错位。

修复方向：§8.1.1 加："`windowSize=1280x800` 适用全部 5 个 screenshot label（包括 main menu / validation setup standalone surface）。其他 viewport profile 仅由 `DemoShellLayoutTest` 在 headless 模式断言，不进入 packaged whitebox evidence。"

推荐测试：`Phase4V4WhiteboxScenarioCliTest` 断言 launch script viewport 字面量 = `1280x800`。

#### P2-5 §10 manual whitebox steps 3 / 5 close crop 无独立 evidence 文件名

证据：

1. `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:598` step 3："nav rail | close crop of left rail"，pass condition 列出 5 个 nav icons 视觉差异；
2. line 600 step 5："bottom deck | close crop of bottom deck"，断言 hero crest size / action card alignment / command hints 位置 / bottom stats 不压 log；
3. §8.1.1 line 449-455 "必须生成的 evidence 文件名" 只列 6 项（5 个全图 + 1 个 app.log），无 close crop 文件名；
4. §10 manual record YAML template `screenshots[]` 字段示例（line 636-660）无 close crop 条目。

影响：

1. Manual record 没有 close crop 文件路径占位，reviewer 不知道 crop 应作为独立 PNG 还是同图标注。
2. 若 reviewer 直接复用 full screenshot 而声称完成 step 3 / 5 close crop 验证，则 nav rail icon distinctness（§6.3 line 398 强约束）实质未独立验证。

修复方向（二选一）：

1. §8.1.1 "必须生成的 evidence 文件名"列表追加 2 项：

   ```
   build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/dark-uiux-pr02-1-demo-shell-nav-rail-crop.png
   build/whitebox/dark-uiux-pr02-1-demo-shell-foundation/evidence/dark-uiux-pr02-1-demo-shell-bottom-deck-crop.png
   ```

   并在 §10 manual record template `screenshots[]` 补两条对应 entry。
2. 在 §10 step 3 / 5 显式声明"close crop 由 full screenshot 派生，crop region 在 manual record `screenshots[].cropRegion` 字段标注"。

推荐测试：`Phase4V4WhiteboxScenarioCliTest` 在原有 5 个 screenshot label 之外断言额外 2 个 crop label（若选派生方案则断言 `cropRegion` 字段存在性）。

#### P2-6 §10 packaged whitebox 命令行缺 `windowSize` / `locale` `-P`，catalog 派生关系未声明

证据：

1. `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:576-578` packaged whitebox 命令仅含 `-Pktome.whitebox.scenario=dark-uiux-pr02-1-demo-shell-foundation`，无 windowSize / locale `-P` 参数；
2. §8.1.1 line 438-446 声明 `windowSize=1280x800` / `locale=zh-CN` / `whiteboxRoot` / `evidenceDir` / `appLog` 5 字段；
3. 文档未声明这些字段是由 `Phase4V4WhiteboxScenarioMaterializationCatalog.kt` 内固化（CLI 不必传），还是 CLI 必须传 `-P` 注入。

影响：实施者若在 catalog 不固化的前提下 CLI 不传 `-P`，可能 fallback 到默认 locale（en-US）或默认 windowSize，导致 manual record 截图与 §8.1.1 contract 实际不符。

修复方向：§8.1.1 加一段："以上字段 (`windowSize`、`locale`、`whiteboxRoot`、`evidenceDir`、`manualRecord`、`appLog`) 全部由 `Phase4V4WhiteboxScenarioMaterializationCatalog.kt` 内固化，CLI 仅传 `-Pktome.whitebox.scenario=dark-uiux-pr02-1-demo-shell-foundation`，不得允许 CLI `-P` 覆盖；`Phase4V4WhiteboxScenarioCliTest` 必须断言 launch script 含且仅含上述 6 个字段的字面值。"

推荐测试：`Phase4V4WhiteboxScenarioCliTest` 与 `Phase4V4WhiteboxScenarioMaterializationCatalog` 一致性测试。

#### P2-7 §4.2 rule 6 draw order test 区分覆盖 step 8 / 11a-d 但未约束 step 9 / 10 顺序

证据：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:280` "`TileRendererCanvasTest` 必须能区分 draw order step 8、11a、11b、11c、11d"。

影响：§4.1 draw order 表列 step 9 (nav rail chrome + nav icons) 与 step 10 (right panel chrome + zones + grid placeholders)。若实施者把 step 9 / 10 顺序对调（比如 right panel 先于 nav rail 绘制），由于 navRail 与 rightPanel 在 typed region 上不重叠，视觉上可能完全无差异；rule 6 没有要求区分这两步，意味着不可机器断言。

未来 PR-05 引入 lighting/fog 后若需要在 nav/right 之间插入新 layer，缺乏 step 9 / 10 顺序断言会让 ordering regression 静默通过。

修复方向：§4.2 rule 6 扩为："`TileRendererCanvasTest` 必须能区分 draw order step 8、9、10、11a、11b、11c、11d、12、13、14；step 之间用 invocation index 记录顺序，违反顺序时 fail；step 8 / 12 / 13 / 14 即使 no-op 也保留 marker。"

推荐测试：`TileRendererCanvasTest` 用 fake batch 记录每个 step 的 invocation index，断言 step index 单调递增且每 step 至少有一次 marker invocation。

#### P2-8 §10 manual record template `goldenLabels[]` 只示范 1 label，与 §8.1 5 个 label / §4.2 rule 8 hash drift 范围不一致

证据：

1. `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:633-635` `goldenLabels[]` 仅示范 `dark-uiux-pr02-1-demo-shell-1280x800` 一项；
2. §8.1 表列 5 个 label，每个 label 都有 golden artifact 路径；
3. §4.2 rule 8（line 282）"demo shell 重排导致 golden hash 漂移时，manual record 的 `goldenLabels[]` 必须写 `hashDriftReason=dark-uiux-pr02-1-demo-shell-foundation`"。

影响：本 PR 是重排整 shell foundation，5 个 label 全部都会有 hash drift；template 只示范 1 项会让 reviewer 误填，漏掉其他 4 个 label 的 `hashDriftReason` 字段。

修复方向：§10 template `goldenLabels[]` 改为示范全部 5 个 label，每个都标 `hashDriftReason=dark-uiux-pr02-1-demo-shell-foundation`；或在 template 上方注释"`goldenLabels[]` 必须为 §8.1 全部 5 个 label 各写一条；hash drift 仅记录本轮实际漂移的 label"。

推荐测试：在 manual record schema lint（若存在）或 PR review checklist 中加入"`goldenLabels[].length == §8.1 labels.length` 或显式声明 `unchangedLabels[]` 字段"。

#### P2-9 §13 回滚 `rg` 命令搜索路径缺 `tools/src/test`

证据：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:731-733`：

```
rg "ui\.shell\.|r01b-ui-shell-chrome|PR-02-1|dark-uiux-pr02-1-demo-shell-foundation" \
  UI/sprite-sheets assets-src/image client/src/main/resources \
  client/src/main/kotlin client/src/test/kotlin tools/src/main game/src/main game/src/test
```

影响：`tools/src/test` 未列入。§2 line 138 / 139 / 140 / 143 已声明 `DarkSpriteSheetPipelineScriptTest` / `Phase4V4AcceptanceContractLintTest` / `VerificationImpactAnalyzerTest` / `Phase4V4WhiteboxScenarioCliTest` 等 tools 测试均会含 `PR-02-1` / `dark-uiux-pr02-1` 字面量；回滚后 `tools/src/test` 中残留引用不会被 rg 检出，实施者错以为回滚干净。

修复方向：§13 回滚 `rg` 命令扩展搜索路径：

```
rg "ui\.shell\.|r01b-ui-shell-chrome|PR-02-1|dark-uiux-pr02-1-demo-shell-foundation" \
  UI/sprite-sheets assets-src/image client/src/main/resources \
  client/src/main/kotlin client/src/test/kotlin \
  tools/src/main tools/src/test \
  game/src/main game/src/test
```

并加注释："`docs/` 与 `UI/review/` 中的 historical artifact 保留，不计入回滚扫描；如需清理 historical 引用，由 follow-up commit 处理。"

推荐测试：手工执行回滚 `rg` 验证；可选在 `:tools:darkUiuxRollbackScanTest` 中固化扫描路径列表。

### P3

#### P3-1 §6.1 item 6 诊断文本硬编码列举 PR 命名空间不可持续

证据：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:355` "`scripts/verify_dark_key_registry.py` 的 ownerPr 诊断文本必须从旧 `PR-00 format` 改为明确的 `PR-00 or PR-02-1 format`"。结合当前 `scripts/dark_sprite_sheet_contract.py:363` / `scripts/verify_dark_key_registry.py:91` 都用 `"PR-00 format"` 字面量。

影响：未来引入 PR-02-2 / PR-03-1 时，诊断文本要继续追加列举。当前修复仅治标。

修复方向：§6.1 item 6 改写为："`scripts/verify_dark_key_registry.py` 与 `scripts/dark_sprite_sheet_contract.py` 的 ownerPr 诊断文本必须输出 `OWNER_PR_PATTERN` 正则字面量自身，如 `ownerPr 必须匹配 ^PR-\\d{2}(?:-\\d+)?$, got '<value>'`；不得硬编码列举 `PR-00` / `PR-02-1` 等具体编号。"

推荐测试：`DarkSpriteSheetPipelineScriptTest` 断言诊断文本含 `^PR-\\d{2}(?:-\\d+)?$` 字面量；不包含 `PR-02-1` / `PR-00 or` 等编号列举。

#### P3-2 §2 line 151 migration "已存在临时实现" 完成检测标准缺失

证据：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:151` "如果进入 PR-02-1 时 `TileRenderer.kt` 已经存在 shell chrome / bounded text / panel scaffold 临时实现，第一步必须迁移：把 shell-specific draw helper 移入 `DemoShellRenderer.kt`...未完成迁移前不得捕获 golden 或 packaged whitebox evidence"。

影响：缺机器化完成检测：

- "shell-specific draw helper" 如何检测？grep `drawAsset(.*"ui\.shell` in `TileRenderer.kt`？
- "几何移入 `DemoShellLayout.kt`" 检测方式？
- 实施者可能跳过 audit，直接在 `TileRenderer.kt` 内增长，违反 §2 line 148 "净增长不得超过 +400 行" 后续约束但完成检测仍为人工。

修复方向：§2.1 step 1 preflight 之后加 step 1.5："migration audit：执行 `rg 'ui\\.shell\\.' client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` 与 `rg 'private fun draw[A-Z].*Shell' client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`，若任一返回非空，第一步必须先迁移至 `DemoShellRenderer.kt` / `DemoShellLayout.kt`；迁移完成的判定：上述命令返回空。"

推荐测试：`maintainabilityLint` 或新增 `TileRendererStructureLint` 规则：`TileRenderer.kt` 不得直接绘制 `ui.shell.*` key。

#### P3-3 §6.2 `ui.shell.nav.gear` fallback `ui.hud.warning.icon` 语义偏离（Round-3 4.B 未修）

证据：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:377` `ui.shell.nav.gear | ... | ui.hud.warning.icon`。`gear` 视觉对应 settings 按钮；`warning.icon` 视觉对应 warning。§6.3 line 398 已强约束 r01b 必须 USED，理论上 fallback 不会显示给玩家；但若实施期 r01b 临时未生成 / 切分失败导致 manifest resolver fallback 触发，玩家会把 nav rail 中"设置"看成"警告"。

影响：与 Round-3 4.B 一致：玩家信号语义偏离；r01b USED 时无影响，但 fallback 触发期间是 last-resort 视觉错位。

修复方向：在 PR-02 keys 中寻找语义更接近 settings 的 fallback（如 `ui.control.config.icon`、`ui.frame.tooltip.body`）；若 PR-02 无合适 key，承认 last-resort 并在 §6.2 表后注释："`nav.gear` fallback `ui.hud.warning.icon` 是 last-resort 视觉降级，仅在 r01b 生成失败时短暂出现；§6.3 强约束 r01b USED，正常路径下不展示给玩家。"

推荐测试：保持 `darkKeyRegistryLint` 现有 fallback validity 检查。

#### P3-4 §11 "screen reader hint" 与 PR-02-1 typed region 无实施合同

证据：`UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:691` "辅助文本必须保留在 tooltip、screen reader hint 和 compact help slot 中"。`tooltip` / `compact help slot` 都在 §3.1 typed region 列有对应；`screen reader hint` 在文档中只出现这一次，无实施合同，PR-02-1 影响范围未列任何 a11y API 引入。

影响：实施者会按字面引入 `ScreenReaderHint` 实体或忽略此约束；PR-02-1 范围若不实施，应在 §11 改为"留给 PR-07 final polish"或删除该词。

修复方向：§11 非目标第 7 条改为："辅助文本必须保留在 tooltip 与 compact help slot 中；screen reader hint 留给 PR-07 final polish 实施，PR-02-1 不引入新 a11y API。"

推荐测试：无需测试（文档微调）。

#### P3-5 §6.3 reserved cell 用途 + §8.1.1 path 派生关系未声明

证据：

1. `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:378` row 3 col 3 reserved。
2. §6.3 line 392-395 grid occupancy contract 仅列计数（direct=15 / alias=0 / reserved=1 / total=16），未声明 reserved cell roadmap。
3. §8.1.1 line 443-446 `whiteboxRoot`、`evidenceDir`、`appLog` 三 path 嵌套但未声明派生关系（`evidenceDir = ${whiteboxRoot}/evidence`、`appLog = ${evidenceDir}/${scenarioId}-app.log`）。

影响：

1. Reserved cell：未来若 PR-02-2 / PR-03 想填该 cell，没有 roadmap 引用，可能误以为是 dead reservation。
2. Path 派生：catalog 内固化时若 `whiteboxRoot` 变更，`evidenceDir` / `appLog` 是否自动更新？文档未声明派生，留下三处真源漂移可能。

修复方向：

1. §6.3 在 grid occupancy 表后加："reserved cell 留给 PR-02-1 follow-up（如 nav rail 第六个按钮、quest tracker placeholder）；若 PR-02-1 close 时仍未消费，下一个 dark UI/UX PR 引入新 shell chrome key 时优先填入此 cell，再考虑新 sheet。"
2. §8.1.1 表后加："`evidenceDir = ${whiteboxRoot}/evidence`，`appLog = ${evidenceDir}/${scenarioId}-app.log`；`Phase4V4WhiteboxScenarioMaterializationCatalog.kt` 必须从 `whiteboxRoot` 派生其余 path，禁止独立维护四处字面量。"

推荐测试：`Phase4V4WhiteboxScenarioCliTest` 断言派生关系。

## 前一轮草稿撤换

仓库内此前已存在另一视角的 Round-4 草稿（也写入本路径）。本轮按当前文档（734 行版）独立校验后撤换其中 4 项已不成立的判断：

| 旧 Round-4 判断 | 当前状态 | 反证 |
| --- | --- | --- |
| P1-1 "§2 影响范围未列 ValidationScenarioRegistry / Catalog / yaml / materialization" | 不成立 | `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md:142-144` 已显式列入这 7 个 owner 文件 + 对应测试 |
| P2-1 "§9 数值断言全归 DemoShellLayoutTest 与 §3.1.1 冲突" | 不成立 | §9 line 510-512 已分层归 DemoShellLayoutTest / InfoSurfaceLayoutTest / TileRendererCanvasTest |
| P2-3 "action_deck.frame subject 是 three sockets，与四槽合同冲突" | 不成立 | line 368 subject 已是 "four evenly spaced heavy card sockets and ember dividers" |
| P2-4 "manual record template 无双 reviewer + 五 label coverage" | 不成立 | line 607-625 template 已含 `manualReviewers` 数组（owner + non-owner 显式 role/verdict）和 `screenshotLabelCoverage.required[]` 五项 |

旧 Round-4 中"PR-02-1 owner-scope task 未注册"、"DemoShellLayout / DemoShellRenderer 未建"、"sheet-plan / key-registry / manifest 无 ui.shell.* 条目"等条目属实，但这些都是 PR-02-1 文档已声明的 "Added / Modified" 实施任务（不构成文档缺陷），实施期由开发者按 §2.1 顺序逐步落地。

旧 Round-4 中 P3-1 "verify_dark_key_registry.py 仍写死 PR-00 format" 与本轮 P3-1 重合，本轮提升为更彻底的 "使用 OWNER_PR_PATTERN 字面量替换硬编码" 修复方向。

旧 Round-4 中 P3-2 "§7 英文正文" / P3-3 "evidence path 自然语言" 属风格层面，本轮判定不阻塞实施，不再单列。

## Requirement Alignment

| Requirement (Acceptance Matrix 行号) | Evidence | Conclusion |
| --- | --- | --- |
| UI02-1-M01 demo shell foundation goal | §1 line 105-114 列出 demo 对齐目标 5 条 | Aligned |
| UI02-1-M02 shell layout contract | §3 typed region 14 项 + §3.1.1 ownership matrix + §3.2 11 行硬约束 + §3.2 modalSafeBounds 比例带 | Aligned；modalSafeBounds 锚点未声明（P2-2） |
| UI02-1-M03 renderer / layer order | §4.1 14-step draw order + §4.2 8 条 rule | Aligned；step 9 / 10 顺序未约束（P2-7）；`DemoShellRenderer` 缺专属测试覆盖（P1-2） |
| UI02-1-M04 right panel grid scaffold | §5.1 slot 几何表 + §3.2 slot side 下限 | Aligned；其他 9 个 ui.shell.* 滥用边界未声明（P2-3） |
| UI02-1-M05 Round 1B shell chrome | §6.1 owner scope rule + §6.2 15 cells + §6.3 occupancy | Aligned；`ownerExpectedKeys` 两源优先级未声明（P2-1）；reportFileName rename 缺 removalOwner（P1-3） |
| UI02-1-M06 standalone / menu alignment | §7 minimum geometry contract | Aligned；standalone screenshot viewport 是否固定 1280×800 未声明（P2-4） |
| UI02-1-M07 demo parity evidence | §8.1 + §8.1.1 + §10 manual record template | Aligned；close crop evidence 文件名未列入（P2-5）；template `goldenLabels[]` 只示范 1 项（P2-8）；packaged CLI 与 catalog 派生关系不明（P2-6） |
| UI02-1-M08 validation / maintainability gates | §10 命令组 + §13 回滚边界 | Aligned；回滚 rg 路径不完整（P2-9）；verifyChanged 静态合同覆盖测试"或"留盲点（P1-1） |

## 功能/系统一致性矩阵

| 模块/系统 | 文档合同 | 当前代码/文档状态 | 偏差 | 严重级别 |
| --- | --- | --- | --- | --- |
| `acceptanceContractLint` 静态合同 | §0 line 11 + `Phase4V4AcceptanceContractLintTest.uiPrDocs` 应含 UI02-1 minimumRows=8 | `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt:173-179` 已含 | 无 | OK |
| `OWNER_PR_PATTERN` 正则 | §6.1 line 346 `^PR-\d{2}(?:-\d+)?$`，§2 line 138 三条 regression | `scripts/dark_sprite_sheet_contract.py:20` 仍为 `^PR-\d{2}$` | 实施待办，非文档缺陷 | OK |
| `darkManifestCoveragePr02_1OwnerScope` task | §6.1 item 1 注册要求 | `tools/build.gradle.kts:687` 仅有 Pr02OwnerScope | 实施待办 | OK |
| `VerificationTaskRegistry.ownerTaskPaths` | §6.1 item 3 must 包含 PR-02-1 task | `VerificationTaskRegistry.kt:289` 仅含 Pr02OwnerScope | 实施待办 | OK |
| Validation scenario registry | §2 line 142 declare; §8.1.1 line 459-465 declare | `ValidationScenarioPresentationCatalog.kt:44-49` 已有 PR-02 scenario（`dark-uiux-pr02-ui-chrome-sprite-pilot`）；PR-02-1 scenario 未注册 | 实施待办 | OK |
| `phase4-v4-scenarios.yaml` | §8.1.1 line 461 declare | 当前未含 `dark-uiux-pr02-1-demo-shell-foundation` | 实施待办 | OK |
| `verifyChangedTaskPaths` 静态合同覆盖 | §6.1 item 5 verifyChangedBuildContractTest **或** VerificationImpactAnalyzerTest | 当前 `build.gradle.kts:96` 仅含 PR-02 task path；文档"或"留盲点 | 文档缺陷 | P1（见 P1-1） |
| Canonical artifact 列 `DemoShellRenderer.kt` 但无专属测试声明 | §0 line 60 + §4.2 rule 7 内部冲突 | 无 | 文档缺陷 | P1（见 P1-2） |
| PR-02 reportFileName rename | §6.1 item 2 强制 PR-02 rename | §2 Deleted 清单 / §13 回滚边界未声明 | 文档缺陷 | P1（见 P1-3） |
| 回滚边界 rg 扫描完整性 | §13 line 731 扫描路径列表 | 缺 `tools/src/test` | 文档缺陷 | P2（见 P2-9） |
| `ui.shell.*` key 滥用边界 | §5.1 line 308 仅约束 nav/hero | 9 个其他 shell key 无明确边界 | 文档缺陷 | P2（见 P2-3） |

## 玩法与体验审查

PR-02-1 是 shell foundation，不引入新玩法、技能、loot 或 NPC；玩家体验主要落在"局内主界面是否能从旧 text-first shell 升级为 demo-like icon rail + map stage + right panel + bottom deck 结构"。

- §1 line 106-114 与 §3.1 14 项 typed region + §5.1 slot 几何 + §5.2 bottom deck 五分区 + §5.3 nav rail icon-first 已经把玩家可见的视觉骨架声明完整。
- §4.1 draw order 把 fog / light placeholder 留在 step 8，避免 PR-05 引入光照时回头重排 shell，对玩家是"现在没光照但未来不会破坏布局"的保护。
- §6.3 line 398 禁止 fallback-only nav icon 通过 manual record，正面回应玩家"5 个 nav icon 视觉差异"诉求，是关键 UX 保护。
- §11 line 691 "screen reader hint" 与 typed region 缺合同（P3-4），对 a11y 玩家影响有限，可推迟到 PR-07，但应在本 PR 显式声明非目标。

整体玩家体验风险：modalSafeBounds 锚点未声明（P2-2）若实施期被忽视，可能在 inventory modal 打开时遮挡 navRail，让 ESC 之外的退出路径不可见；`ui.shell.action_deck.frame` / `ui.shell.command_hint.plate` 等 9 个 key 滥用未约束（P2-3）会让 PR-03 实施期意外把 shell chrome 用作 item slot 容器，破坏视觉一致性。这两条是体验侧 P2 优先修。

## 当前阶段必须解决的问题

合并 PR-02-1 前必须修：

1. **P1-1**：§6.1 item 5 "或"改为"和"，并分别明确两个测试断言点（影响 verifyChanged 静态合同与 impact routing 同步覆盖）。
2. **P1-2**：`DemoShellRenderer.kt` 专属 focused test 路径或在 §4.2 中显式声明覆盖归属（影响 §4.2 rule 7 内部一致性）。
3. **P1-3**：§6.1 item 2 reportFileName rename 同步迁入 §2 Deleted 清单 + §13 回滚边界（影响跨 PR artifact 一致性 + 回滚完整性）。

合并 PR-02-1 前强烈建议修（P2，避免实施期返工）：

4. P2-1 `ownerExpectedKeys` 两源 primary 声明。
5. P2-2 modalSafeBounds 锚点 / 中心声明。
6. P2-3 `ui.shell.*` 滥用约束扩展到全部 9 个其他 key。
7. P2-4 packaged whitebox 5 label viewport 适用范围。
8. P2-5 close crop evidence 文件名 / 派生方案声明。
9. P2-6 packaged whitebox CLI `-P` 与 catalog 派生关系。
10. P2-7 step 9 / 10 顺序加入 test 区分覆盖。
11. P2-8 manual record template `goldenLabels[]` 覆盖全 5 label。
12. P2-9 §13 回滚 `rg` 扫描路径补 `tools/src/test`。

允许进入 PR-03 / PR-05 / PR-06 后续 PR 时再处理（P3）：

13. P3-1 / P3-2 / P3-3 / P3-4 / P3-5（不阻塞实施，但建议本 PR 内一并修以避免长期漂移）。

## Removal/Iteration Plan

按当前 §2 "Deleted / replaced 清单"（line 155-160），PR-02-1 删除/替换四项：

| 路径 / 合同 | Action | removalOwner | Regression Scan |
| --- | --- | --- | --- |
| 左侧大段任务文字栏作为主 rail | replace | PR-02-1 | `TileRendererCanvasTest` |
| 右侧装备/背包纯文字列表作为主路径 | replace | PR-02-1 / PR-03 | scaffold by PR-02-1, icons by PR-03 |
| 底部裸 hint / debug HUD 密度 | replace | PR-02-1 | `InfoSurfaceLayoutTest` / golden |
| map 被 chrome 背景压低 | replace | PR-02-1 / PR-05 | stage by PR-02-1, assets by PR-05 |

P1-3 要求新增 1 行：

| 路径 / 合同 | Action | removalOwner | Regression Scan |
| --- | --- | --- | --- |
| `dark-v1-manifest-coverage.json`（作为 PR-02 owner-scope output 文件名） | replace with `dark-v1-manifest-coverage-pr02-owner-scope.json` | PR-02-1 | `VerifyChangedBuildContractTest` + `VerificationImpactAnalyzerTest` |

§13 回滚边界 6 条已涵盖单 commit 回退主流程，建议根据 P2-9 扩展 rg 扫描；P1-3 修复后追加"reportFileName rename 一起回退"明文。

跨 PR 删除责任：`UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` 的 `remainingGaps[]` 已正确把 final item / map / skill / polish 归属 PR-03 / PR-05 / PR-06 / PR-07，不存在 cross-PR 删除遗漏。

## Additional Suggestions

1. **§2.1 step 1.5 migration audit**：建议在 preflight 之后、demo decomposition 之前插入显式 migration audit step（P3-2），用 grep 命令固化检测，避免实施者人为跳过。
2. **§4.2 rule 6 扩展 step 9-14 区分覆盖**：相比 P2-7 仅补 9 / 10，建议直接扩到 8-14 全 step，让未来 PR-05 / PR-07 引入新 layer 时不必再扩 rule。
3. **§6.2 表后补 sheet QA acceptance**：表只声明 prompt subject 真源（§6.2 line 388），未声明 contact sheet QA reviewer 名单 / fail condition。可选在 PR-02 governance（PR-00 系列已有）继承，本 PR 显式引用即可。
4. **§9 验收第 12 条 `rightPanelGridDoesNotUseNavOrHeroKeysForItemSlots` test 名建议泛化**：当前 test 名硬编码 nav/hero，未来扩展 shell key 时需要重命名。建议改为 `shellKeysBindToConsumerRegionOnly`（与 P2-3 推荐测试合并）。
5. **§3.1.1 ownership matrix 加 GameShellLayout 注释**：当前第 3 行 "GameShellLayout 只保留兼容与安全边界断言" 可显式列出兼容字段（如 `viewportSafeBounds`、PR-01-1 留下的何种 region），方便实施者 grep 现有 GameShellLayout.kt 确认范围。

## Open Questions

1. Round-3 4.B / Round-4 P3-3 关于 nav.gear fallback `ui.hud.warning.icon`：是否在 PR-02 keys 中存在更接近 settings 视觉的 key（如 `ui.control.config.icon`、`ui.frame.tooltip.body` 等）？若存在，应替换；若否，是否接受 last-resort fallback？
2. §8.1.1 windowSize=1280x800 是否要求 standalone main menu / validation setup 也用此 viewport？还是 standalone 仍保留 PR-01 / PR-02 原 viewport（待 §7 几何 contract 自洽即可）？
3. `phase4-v4-scenarios.yaml` 当前不含 PR-02 / PR-02-1 scenario，文档要求新增；是否同步声明本文件中所有 dark UI/UX scenario 的 evidence summary key 命名规范？
4. P1-3 / Removal Plan 是否需要在 PR-02 closed branch 上 backport reportFileName 变更？还是仅在 main 上做单向 rename，依赖 PR-02-1 merge 时间窗口？

## Suggested Verification

文档侧（修文档后执行）：

```bash
# verify markdown formatting and no machine-absolute path
git diff --check -- UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md
awk 'BEGIN{c=0} /^```/{c++} END{print FILENAME ":FENCE_OPEN=" c%2}' \
  UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md
ABS_PATH_PATTERN='/(U[s]ers|tmp)/|[A-Za-z]:\\\\'
rg -n --type md "$ABS_PATH_PATTERN" UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md

# acceptance contract lint after doc edits
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
./gradlew acceptanceContractLint
```

实施侧（待 P1 / P2 patch 合入并按 §2.1 顺序进入实施后由开发者执行）：

```bash
./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest
./gradlew :tools:test --tests com.ktome.tools.verification.VerifyChangedBuildContractTest
./gradlew :tools:test --tests com.ktome.tools.verification.VerificationImpactAnalyzerTest
./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest
./gradlew :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest
./gradlew :client:test --tests com.ktome.client.input.ValidationCommandSourceTest
./gradlew :client:test \
  --tests com.ktome.client.render.DemoShellLayoutTest \
  --tests com.ktome.client.render.TileRendererCanvasTest \
  --tests com.ktome.client.render.InfoSurfaceLayoutTest
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint \
  -Pktome.darkUiux.requireFullGrid=true \
  -Pktome.darkUiux.ownerContract=UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml \
  -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl
./gradlew darkManifestCoveragePr02_1OwnerScope
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint verifyChanged
```

本轮未运行：上述全部命令；本轮纯静态读取，未执行任何 gate 或 client test。

## Summary

本轮（Round 4）确认 Round-3 列出的 5 条 P1 / 7 条 P2 / 4 条 P3 中：5/5 P1 已解决；7/7 P2 已解决（modalSafeBounds 数值已补但锚点新发现）；3/4 P3 已解决，nav.gear fallback 转入 Round-4 P3-3。文档从 553 行扩到 734 行，新增 §3.1.1 Layout Ownership Matrix、§3.2 modalSafeBounds 比例带、§5.2 actionDeck 精化（card.width / slot gap / cardCount 1..4 fail 条件）、§6.1 routing 子项 1-6、§8.1.1 Packaged Whitebox Scenario Contract、§9 分层断言（DemoShellLayoutTest / InfoSurfaceLayoutTest / TileRendererCanvasTest）、§10 manual record YAML template（双 reviewer + 5 label coverage），方向正确，所有 Round-3 阻塞项均已吸收。

Round 4 新发现 3 条 P1：

1. §6.1 item 5 verifyChanged 静态合同覆盖测试"或"留盲点（P1-1）。
2. `DemoShellRenderer.kt` 列为 canonical artifact 但未声明专属 focused test，与 §4.2 rule 7 自相矛盾（P1-2）。
3. §6.1 item 2 PR-02 reportFileName rename 缺迁移 / removalOwner / 回滚一致性声明（P1-3）。

另发现 9 条 P2、5 条 P3，全部含 `path:line` 证据锚与可执行修复方向。打完 3 条 P1 后，文档即可作为 PR-02-1 实施驱动；P2 9 条建议在实施进入 §2.1 step 2-3（demo decomposition + layout test）之前补齐，避免开发期临场补合同。P3 5 条不阻塞实施，但建议本 PR 内一并修，避免长期漂移。

同时撤换仓库内此前另一视角的 Round-4 草稿中 4 条已不成立的判断：(a) §2 影响范围已包含 ValidationScenario 系列文件；(b) §9 数值断言已分层归 owner test；(c) action_deck.frame subject 已是 "four heavy card sockets"；(d) manual record template 已支持双 reviewer + 5 label coverage。

前一轮（Round 3）已撤销的"acceptanceContractLint 未覆盖 PR-02-1"错误 P0 结论本轮继续不再列入。

# Dark UI/UX PR-02-1 Demo Shell Foundation 深度审查 Round 3

- **审查对象**: `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md` (553 行版)
- **基准 DEMO**: `UI/UI-demo.png` (实测 `1672 x 941`, 8-bit RGB, non-interlaced)
- **审查日期**: 2026-05-11
- **审查角色**: 资深 Roguelike / 类 ToME 开发设计总监 + 系统策划总监 + 玩法体验审查负责人
- **审查口径**: 全部基于实际仓库状态而非旧 agent 报告。本轮独立校验了文档声明的 18 条 canonical artifact 中可静态校验项，发现 1 处数值自矛盾、2 处文件名/命令路径互相冲突、1 处必须列缺失、3 处规约语义歧义；另撤销前一轮报告中已不成立的 P0 结论。
- **结论**: **方向正确，但文档当前不可直接驱动开发**。在 §3.2、§6.2、§10、§12 的若干条目修复前进入实施会出现 (a) DemoShellLayoutTest 写不出确定断言；(b) sheet-plan lint 在 cell freeze 时 fail；(c) PR 描述与 verifyChanged artifact 落到不同文件名造成假绿。文档建议先打 P1 patch 再开工。

## 0. 撤销前一轮报告中错误的 P0

`UI/review/2026-05-11-dark-uiux-pr02-1-deep-review-round2.md` (其他 agent 输出) 把 `Phase4V4AcceptanceContractLintTest.uiPrDocs` 未覆盖 PR-02-1 列为 P1 (第 1 条)。

证据反例：当前 `tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt` 第 153-205 行已经显式包含：

```
PrDoc(requirementPrefix = "UI01-1", path = "UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md", minimumRows = 7)
PrDoc(requirementPrefix = "UI02-1", path = "UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md", minimumRows = 8)
```

`uiPrDocs` 已涵盖 UI00 / UI01 / UI01-1 / UI02 / UI02-1 / UI03 / UI04 / UI05 / UI06 / UI07，acceptance lint gate 已落地。该 P1 不再成立。

附带说明：`acceptanceRows(markdown, "UI02")` 用 `line.startsWith("| \`UI02-")` 做前缀匹配，理论上对 `UI02-1-M01` 会越界匹配；但实际上 PR-02 与 PR-02-1 是两份独立 markdown，PR-02 文档内部不含 `| \`UI02-1-` 行，PR-02-1 文档内部不含 `| \`UI02-M` 行，因此当前 lint 无实际越界。仅作为潜在脆弱点保留意见，不计入本轮发现。

## 1. 严重度分布

| 级别 | 数量 | 含义 |
| --- | ---: | --- |
| P0 | 0 | 无破坏 core / save / replay / schema / 经济规则 |
| P1 | 5 | 文档内部矛盾或必须列缺失，进入实施前必须 patch |
| P2 | 7 | 开发细节语义歧义，进入实施前补齐可显著降低返工 |
| P3 | 4 | 微调与术语清晰度 |

## 2. P1 — 必须当前 patch 的文档级缺陷

### 2.A §6.2 direct cell 表缺 `subject` 列

- 位置: `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md` §6.2 表头 (line 334) 和 Column ownership 表 (line 355-358)
- 证据:
  - §6.2 Column ownership 表显式声明 sheet-plan 应接收 `subject` 字段。
  - `scripts/dark_sprite_sheet_contract.py:30-40` `CELL_INPUT_FIELDS` 包含 `subject`。
  - 真实仓库 `UI/sprite-sheets/sheet-plan.yaml` 中每个非 reserved cell 都有 `subject` 字段 (如 line 22-23 `forged iron worn stone panel body with ember scratches and subtle cyan edge light`)。
  - 而 §6.2 direct cell 表只有 9 列：`row | col | targetKey | category | outputName | fallbackKey | aliasOf | consumer | consumerTest`。没有 `subject` 列。
- 影响: 实施者一旦执行 §2.1 步骤 6 "resource contract fifth"，需要把 §6.2 cells 写入 `sheet-plan.yaml`。`subject` 必填但文档外没有真源，只能临场撰文，形成文档外第二真源；prompt 生成阶段同样需要 subject。一旦实施者本地补的 subject 与本 PR doc 不同，PR review 无法判断哪个才是 canonical。
- 修复方向: 在 §6.2 表头加 `subject` 列，并为 15 个 direct cells 各填一句英文生成主体描述。可直接复制为 `sheet-plan.yaml.cells[].subject`。建议草案 (示例三个，其余请补齐)：
  - `ui.shell.outer_frame`: `forged iron full-screen shell frame with worn stone inset and faint cyan rim light`
  - `ui.shell.map_stage.frame`: `dark stone arched frame around playable map area with subtle ember glow on inner edge`
  - `ui.shell.nav.compass`: `brass compass rose nav icon with engraved cardinal marks and cyan needle glow`

### 2.B §0 / §10 / §12 三处对 manifest coverage report 文件名互相矛盾

- 位置:
  - §0 Canonical Artifact 第 11 项 (line 79): `build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-1-owner-scope.json`
  - §0 Acceptance Matrix UI02-1-M05 artifact (line 23): 同上。
  - §10 验证命令 (line 480-484): 使用裸 task `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02-1`，该裸 task 在 `tools/build.gradle.kts:635-676` 的注册形式下输出到默认 `dark-v1-manifest-coverage.json`，与 PR-02 owner-scope task 共名 (line 691)。
  - §12 PR 描述要求第 4 条 (line 533): 引用 `dark-v1-manifest-coverage.json`。
- 影响: 三处口径产生两套并发文件名。verifyChanged 一旦在同一 build 内同时跑 PR-02 owner-scope 和 PR-02-1 owner-scope，文件互相覆盖；PR 描述按 §12 引用 PR-02 文件名，但实际 PR-02-1 数据写在另一处，reviewer 读到 PR-02 旧报告即声称 PR-02-1 验证通过 —— **假绿真实风险**。
- 修复方向:
  1. §10 删除裸 `darkManifestCoverageLint` 调用，改为 `./gradlew darkManifestCoveragePr02_1OwnerScope`，与 M05 fastCheck/ownerGate 一致。
  2. §12 第 4 条把文件名改为 `dark-v1-manifest-coverage-pr02-1-owner-scope.json`，与 §0 canonical 对齐。
  3. 进入实施时同步把 `tools/build.gradle.kts:686-692` 的 `darkManifestCoveragePr02OwnerScope` reportFileName 从 `dark-v1-manifest-coverage.json` 改为 `dark-v1-manifest-coverage-pr02-owner-scope.json`，消除与裸 task 默认名共用问题（这是 PR-02-1 范围内的 verifyChanged routing 修缮，§2 line 138 已声明范围合法）。

### 2.C §3.2 demo-aspect bottomDeck 的绝对值与比例下限自矛盾

- 位置: §3.2 硬约束表第 6 与第 7 行 (line 211-212)
- 证据: demo-aspect viewport `1672x941`。
  - 第 6 行: `bottomDeck.height >= 205 px`
  - 第 7 行: `bottomDeck.height / viewport.height >= 0.22`
  - 真值: `0.22 × 941 = 207.02 px > 205 px`。
- 影响: `DemoShellLayoutTest` 在 demo-aspect profile 必须断言两条；若实现给 `bottomDeck.height = 206 px`，比例断言 fail (206/941 = 0.219 < 0.22)；若给 `204 px`，绝对值断言 fail。即 205 与 0.22 之间存在 [205, 207] px 区间无可行下限。
- 修复方向: 推荐把第 6 行 demo-aspect 改为 `>= 208 px`（与 0.22 ratio 对齐，留 1 px 安全余量）；或把第 7 行 demo-aspect 改为 `>= 0.218`。建议前者，因为 `>= 208 px` 给到的高度更符合 DEMO 视觉。

### 2.D §6.1 `OWNER_PR_PATTERN` 目标正则未明示

- 位置: §6.1 line 328 "本 PR 必须修改 `scripts/dark_sprite_sheet_contract.py` 的 `OWNER_PR_PATTERN` 和 `DarkSpriteSheetPipelineScriptTest`，允许 `PR-02-1` 这类细分 owner"
- 证据: 当前 `scripts/dark_sprite_sheet_contract.py:20` 为 `OWNER_PR_PATTERN = re.compile(r"^PR-\d{2}$")`。仓库无任何 PR-02-1 regex regression。
- 影响: "允许 `PR-02-1` 这类" 至少有三种合法解读：
  - `^PR-\d{2}(?:-\d+)?$` — 允许 0 或 1 级数字后缀；未来 `PR-02-1-2` 不合法。
  - `^PR-\d{2}(?:-\d+)*$` — 允许任意层级；`PR-02-1-1` / `PR-02-2-3` 都合法。
  - `^PR-\d{2}(?:-1)?$` — 只允许后缀 `-1`，强约束细分编号唯一。
  实施者选择不同正则，会让未来 PR 命名空间分叉，回退成本高。
- 修复方向: §6.1 明确写出目标正则。推荐 `^PR-\d{2}(?:-\d+)?$`（允许任意细分编号，但只允许一级深度），同时在 `DarkSpriteSheetPipelineScriptTest` 中至少补三条正负回归：`PR-02-1` ✓、`PR-02-1-1` ✗、`pr-02-1` ✗。`scripts/verify_dark_key_registry.py:14` 共享同一 pattern，无需独立修改。

### 2.E §6.2 nav icon fallback 中 compass 与 book 共指同一 fallbackKey

- 位置: §6.2 表 line 346 与 line 349
- 证据:
  - `ui.shell.nav.compass` → fallback `ui.hud.quest_marker.icon`
  - `ui.shell.nav.book` → fallback `ui.hud.quest_marker.icon`
- 影响:
  - §5.3 第 2 条要求 nav rail 必须呈现 5 个不同 icon 视觉。
  - §6.3 末段允许 r01b NOT_USED 路径（manual record + reviewer verdict 放行）。
  - r01b NOT_USED 时，nav rail 中 compass 与 book 视觉完全相同，玩家无法区分"地图/罗盘"与"天赋/书本"按钮——**这是 §0 Failure Rule 精神（"Fallback-only shells that visually collapse"）的直接违反**，虽然技术上 fallback 不是 `panel.body`。
  - r01b USED 时也存在 prompt 风险：若实施者直觉性继承 fallback 视觉 (quest marker)，两张 icon 可能也雷同。
- 修复方向: 二选一：
  1. **优选**：把 `ui.shell.nav.book` 的 fallback 改为另一 PR-02 key（先确认存在），如 `ui.control.equipment.icon` 或 `ui.frame.tooltip.body`。若 PR-02 无合适 fallback，新增 `aliasOf` 或显式 reserved。
  2. **次选**：在 §6.2 表后加一段 "r01b NOT_USED policy"，声明 r01b NOT_USED 时 nav rail "icon distinctness" 必须在 manual record 列为 `remainingGap → followup PR-02-1.x`，不允许 close PR-02-1 同时声称 nav rail icon 视觉完整。

## 3. P2 — 开发细节语义歧义（强烈建议修）

### 3.A §2 三个 layout 文件 ownership 边界不清

- §2 同时声明 `Added DemoShellLayout.kt`、`Modified GameShellLayout.kt`、`Modified InfoSurfaceLayout.kt`。
- §3 强调 "PR-02-1 必须新增 `DemoShellLayout.kt` 作为 typed shell layout authority"，§2 实现增长约束 3 又说 "`DemoShellLayout.kt` 是本 PR 的主 layout 合同文件"。
- 但 M02 (line 20) 同时列了三个 layout test 为 fastCheck，没区分各自断言哪些 typed region。
- 实施者会被两种约束拉扯：要么 (a) DemoShellLayout 拥有 outerFrame/navRail/mapStage/rightPanel/modalSafeBounds，InfoSurfaceLayout 拥有 bottomDeck 五分区，GameShellLayout 退化为 PR-01-1 legacy contract；要么 (b) DemoShellLayout 沦为薄壳代理 GameShellLayout，PR-02-1 几何被埋回旧 layout。
- 建议: §3 加一段 ownership matrix：

| typed region | layout owner | test owner |
| --- | --- | --- |
| `outerFrame`, `navRail`, `mapStage`, `rightPanel*`, `modalSafeBounds` | `DemoShellLayout` | `DemoShellLayoutTest` |
| `heroCard`, `actionDeck`, `commandHints`, `logDeck`, `bottomStatsSummary` | `InfoSurfaceLayout` | `InfoSurfaceLayoutTest` |
| `viewportSafeBounds`, legacy 3-column non-demo shell | `GameShellLayout` | `GameShellLayoutTest` |

### 3.B `modalSafeBounds` 缺数值边界

- §3.1 仅说 modal 默认锚到 mapStage 中心，clamp 到 modalSafeBounds；§3.2 第 4 条说同源计算。
- 缺约束: modalSafeBounds.width / height 区间？最大覆盖比例？是否允许覆盖 navRail / rightPanel？
- 影响: 实施时一旦 modal 取 viewport 全幅，会把 navRail 和 rightPanel 完全遮蔽，DEMO 视觉破坏。
- 建议: §3.2 表添加：

| Metric | standard | minimum | demo-aspect | assertion |
| --- | ---: | ---: | ---: | --- |
| `modalSafeBounds.width / viewport.width` | `0.55..0.80` | `0.55..0.85` | `0.50..0.78` | `DemoShellLayoutTest` |
| `modalSafeBounds.height / viewport.height` | `0.55..0.78` | `0.55..0.85` | `0.55..0.78` | `DemoShellLayoutTest` |

或显式声明 `modalSafeBounds := mapStage`，不再独立。

### 3.C §5.2 actionDeck "水平比例稳定" 不可断言

- §5.2 第 2 条 "card slots ... 布局必须在 `cardCount in 1..4` 时保持水平比例稳定"。
- "水平比例稳定" 不可机器断言：是 (a) 卡片宽度固定、整组居中？(b) 卡片宽度随 N 缩放、间距固定？(c) 卡片左对齐、右侧留白？
- DEMO 视觉是 (a) 居中等宽。
- 建议改为可断言形态：`actionDeck` 内 `card.width` 与 `cardCount` 解耦（固定 `>= 96 px`），slot 间距固定（`>= 12 px`），整组水平居中在 `actionDeck` 内；断言由 `InfoSurfaceLayoutTest` 或 `TileRendererCanvasTest` 完成。

### 3.D M05 fastCheck 与 §10 命令行不严格匹配

- M05 fastCheck: `spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl`（1 个 `-P`）
- §10 同步命令: 含 `requireFullGrid=true`、`ownerContract=...`（3 个 `-P`）
- 影响: acceptance lint 把 M05 fastCheck 列作"被 doc 承诺的执行口径"，但实际 §10 才是真正命令。两者不一致，CI/agent 按 M05 跑会跑出更宽松的 lint。
- 建议: M05 fastCheck 列字符串补齐 `-Pktome.darkUiux.requireFullGrid=true` 和 `-Pktome.darkUiux.ownerContract=UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml`。

### 3.E §8.2 demo delta checklist 与 §6.3 NOT_USED 路径相互冲突

- §8.2 "left icon rail" 状态: `icon art complete only if ui.shell.nav.* keys are generated`。
- §6.3 末段允许 `r01b: NOT_USED`，仅需 manual record 与 reviewer verdict。
- §5.3 第 2 条又要求 nav rail 必须呈现 5 个 nav icon 视觉差异。
- 三者组合: r01b NOT_USED 时，§5.3 强约束无法达成，但 §6.3 又允许 NOT_USED 通过。文档没说"NOT_USED 路径下 §5.3 的视觉完整条件作为 followup gap"。
- 建议: §6.3 NOT_USED 段后加一句："r01b NOT_USED 时，§5.3 第 2 条的 5 个 nav icon 视觉差异不可达成；manual record 必须在 `remainingGaps[]` 列 `nav-rail-icon-distinctness: deferred-followup`，PR 描述同步引用。"

### 3.F §3.1 outerFrame 与 navRail "兄弟区域" 描述歧义

- 原文 line 178: "包住地图、右栏、底部 deck；`navRail` 是兄弟区域，不塞进 `outerFrame` 内容层"。
- 含义不明：navRail 在 outerFrame 之外独立绘制？还是 navRail 与 outerFrame 在 layout typed region 列表中平级但 outerFrame 不"包含"navRail？
- 影响 §4.1 draw order：若 navRail 在 outerFrame 之外，则 step 9 (nav rail) 必须晚于 step 2 (outer frame) 但不应被 outer frame border 覆盖；若 navRail 在 outerFrame 内但不在"内容层"，需要 frame 几何留缺口。
- 建议改写: "`navRail` 与 `outerFrame` 是 typed regions 中的并列 region；`outerFrame` 提供整屏背景与外缘装饰，但不绘制覆盖 `navRail.bounds` 区域；`navRail` 自身 chrome 由 `ui.shell.nav_rail.frame` 绘制。"

### 3.G §5.1 rightInscriptions "5-8 当前绑定" 含义未释义

- §5.1 第 3 条: "至少 4 行/slot，可显示 `5-8` 当前绑定"。
- 读者第一反应可能解读为"显示 5 到 8 个绑定"（区间数量）或"显示编号 5 到 8 的 4 个绑定"（编号枚举）。
- 上下文 §3.1 `rightInscriptions` 描述 "列表/slot 化" + §5.1 "至少 4 行/slot" 暗示后者（4 个编号 5..8 的 slot）。
- 建议改写: "至少 4 行 slot，对应铭刻槽位编号 `5..8`（其中 `1..4` 已在 §3.1 `rightEquipment` 占用）。"

## 4. P3 — 微调与术语清晰度

### 4.A §4.1 step 8 "cursor no-op group" 术语模糊

- 表 row "fog / light masks / cursor no-op group" 中的 "cursor" 含义不明：mouse cursor？target cursor？actor cursor？
- 建议改为 "fog / light masks (recording marker even if no-op)"，删除 cursor 一词，或明确为 "target cursor (no-op marker if not implemented)"。

### 4.B §6.2 `ui.shell.nav.gear` fallback `ui.hud.warning.icon` 语义偏离

- gear 是设置按钮，warning.icon 是警告标志，r01b NOT_USED 时玩家会把"设置"看成"警告"。
- 建议改 fallback 为更接近 settings 语义的 PR-02 key；若 PR-02 不存在，承认是 last-resort fallback 并在 manual record 显式声明。

### 4.C §0 freshness 第 1 条引用 raw sheet 但 §2 影响范围未列 raw sheet metadata

- §0 freshness 第 1 条要求 raw sheet hash 与 sheet-plan / key-registry 同批。
- §2 影响范围把 `assets-src/image/raw/sheets/dark-v1/r01b-ui-shell-chrome.png` 列为 Added，但未声明谁负责 update raw sheet hash 真源（一般是 sprite map report）。
- 建议在 §2 raw sheet 行的"预期改动"列补 "同步更新 `assets-src/image/manifests/dark-v1-pr02-1-sprite-map-report.jsonl` 中该 sheet 的 rawSheetHash"。

### 4.D §0 acceptance matrix UI02-1-M07 artifact 路径只列一份 manual record

- M07 artifact: `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md`。
- §8.1 列 5 个 screenshot label；建议在 M07 artifact 列里追加 `client/build/reports/golden/` 与 packaged app screenshot 目录，与 §12 PR 描述要求第 2 条对齐。

## 5. 与仓库实际状态对照（不是文档缺陷，仅供 reviewer 同步）

以下条目是 §2 明确声明的 "Added / Modified" 实施任务，PR 实施 close 前必须完成。本轮静态校验确认这些文件尚未变更：

| 实施待办（doc 已声明） | 当前仓库状态 |
| --- | --- |
| `client/.../layout/DemoShellLayout.kt` | 不存在（只有 `GameShellLayout.kt` / `InfoSurfaceLayout.kt`） |
| `client/.../render/DemoShellLayoutTest.kt` | 不存在 |
| `UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml` | 不存在（只有 `pr02-owner-keys.yaml`） |
| `tools/build.gradle.kts` 中 `darkManifestCoveragePr02_1OwnerScope` task | 未注册（line 686-692 仅有 Pr02OwnerScope） |
| `VerificationTaskRegistry.ownerTaskPaths` 包含 PR-02-1 owner-scope | 未包含（line 284-290 仅 Pr02OwnerScope） |
| `scripts/dark_sprite_sheet_contract.py:20` `OWNER_PR_PATTERN` 接受 `PR-02-1` | 仍为 `^PR-\d{2}$`（见 2.D） |
| `DarkSpriteSheetPipelineScriptTest` 中 PR-02-1 regression | 不存在（仅引用 `PR-00` / `PR-02`） |
| `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` | 不存在 |
| `r01b-ui-shell-chrome.png` raw sheet | 不存在 |
| `key-registry.yaml` 内 `ui.shell.*` keys | 不存在 |

这些都属于"文档已声明，实施未开始"。本轮不算作文档缺陷，但建议实施前先打 P1 patch，避免边实施边发现文档矛盾。

## 6. 修复优先级建议

| 优先级 | 改动 | 预计 patch 大小 |
| --- | --- | ---: |
| Block-implementation | §6.2 加 `subject` 列 + 15 个生成主体描述 (2.A) | ~30 行 |
| Block-implementation | §0/§10/§12 manifest coverage 文件名/任务名对齐 (2.B) | ~5 行 |
| Block-implementation | §3.2 demo-aspect bottomDeck 矛盾修正 (2.C) | 1 行 |
| Block-implementation | §6.1 OWNER_PR_PATTERN 目标正则明示 (2.D) | ~3 行 |
| Block-implementation | §6.2 nav.book fallbackKey 改或 NOT_USED 政策声明 (2.E) | ~3 行 |
| Strong-recommend | §3 加 typed region ownership 矩阵 (3.A) | ~10 行 |
| Strong-recommend | §3.2 加 modalSafeBounds 边界 (3.B) | ~5 行 |
| Strong-recommend | §5.2 actionDeck "水平比例稳定" 改成可断言形态 (3.C) | ~5 行 |
| Strong-recommend | M05 fastCheck 补齐 `-P` 参数 (3.D) | 1 行 |
| Strong-recommend | §6.3 NOT_USED + §5.3 + §8.2 三方一致声明 (3.E) | ~3 行 |
| Recommend | §3.1 outerFrame/navRail 关系改写 (3.F) | 1 行 |
| Recommend | §5.1 rightInscriptions 5..8 含义明确 (3.G) | 1 行 |
| Polish | §4.1 step 8 / §6.2 nav.gear fallback / §2 raw sheet hash / M07 artifact (4.A-4.D) | ~5 行 |

## 7. 对"能否承载后续 UI 改造"的判断

- **结构正确度**: ~92%。typed region 列表、draw order、slot 几何下限、demo delta checklist、回滚边界都已经覆盖，能阻止 PR-03/05/06 重新推翻主框架。
- **数值可断言度**: ~80%。§3.2 11 行硬约束已经覆盖大部分典型 region，但 (a) demo-aspect bottomDeck 自矛盾 (2.C)、(b) actionDeck 水平比例语义不可断言 (3.C)、(c) modalSafeBounds 缺数值 (3.B) 三处需要补齐。
- **资源闭环可信度**: ~75%。M05 acceptance 列与 §0 canonical artifact 已经把 owner-scope coverage 写进 close gate，但 (a) subject 列缺失 (2.A)、(b) PR 描述/canonical/§10 三处文件名互相矛盾 (2.B)、(c) OWNER_PR_PATTERN 目标正则不明 (2.D)，使闭环存在文档外口径。
- **DEMO 视觉接近度**（按文档约束推导，非实际渲染验证）: 结构约 90%，视觉约 78%。nav rail icon 视觉差异性受 fallback 选择影响（见 2.E），actionDeck 居中度未约束（见 3.C），右栏在 standard 上限 0.32 仍偏宽（见前一轮建议，本轮不重列）。

打完 P1 五项 patch 后，文档可作为 PR-02-1 实施驱动；P2 七项 patch 建议在实施 §2.1 第 2-3 步之前补齐，避免开发期临场决策。

## 8. 建议的下一步

1. 文档作者：对 §2.A-2.E 出一份单文件 patch；patch 完成后重新 review。
2. 实施前 dry-run：
   ```bash
   source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
   ./gradlew acceptanceContractLint
   ```
   验证 patch 没有破坏现有 `Phase4V4AcceptanceContractLintTest` 对 UI02-1 行数（`minimumRows = 8`）的检查。
3. 进入 §2.1 步骤 6 (resource contract fifth) 之前，确认：
   - `scripts/dark_sprite_sheet_contract.py:20` `OWNER_PR_PATTERN` 已按 2.D 修正。
   - `tools/build.gradle.kts` 已注册 `darkManifestCoveragePr02_1OwnerScope` 并独立 reportFileName。
   - `UI/sprite-sheets/owner-contracts/pr02-1-owner-keys.yaml` 已存在且 `requiredCells[].targetKey` 与 §6.2 表完全一致。

## 9. 本轮已执行的验证

- 静态校验：读取并交叉对照 `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`、`UI/pr/README.md`、`UI/sprite-sheets/sheet-plan.yaml`、`scripts/dark_sprite_sheet_contract.py`、`scripts/verify_dark_key_registry.py`、`tools/build.gradle.kts`、`tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt`、`tools/src/test/kotlin/com/ktome/tools/phase4/Phase4V4AcceptanceContractLintTest.kt`、`tools/src/test/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzerTest.kt`、`tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt`。
- 实测文件：`file UI/UI-demo.png` → `PNG image data, 1672 x 941`。
- 数值矛盾验证：`0.22 × 941 = 207.02 > 205`（见 2.C）。
- 撤销前一轮 P1 #1：`Phase4V4AcceptanceContractLintTest.uiPrDocs` 已含 UI02-1（见 §0）。
- 未运行：`./gradlew acceptanceContractLint`（本轮纯静态读取，未执行 gate）、client focused tests（待 §3 实施）、resource gate（待 §6 实施）、verifyChanged（待整 PR 完成）。

## 10. Summary

文档方向正确，作为 PR-02-1 实施驱动几乎可用。但当前存在 **5 条会阻塞实施的 P1 缺陷**：

1. §6.2 缺 `subject` 列 → sheet-plan freeze 失败。
2. §0/§10/§12 三处对 manifest-coverage 文件名/任务名不一致 → verifyChanged 假绿风险。
3. §3.2 demo-aspect bottomDeck 数值与比例自矛盾 → DemoShellLayoutTest 无可行下限。
4. §6.1 OWNER_PR_PATTERN 目标正则未明示 → 未来 PR 命名分叉。
5. §6.2 nav.compass/nav.book fallback 共指 quest_marker → r01b NOT_USED 时 nav rail 视觉重复。

打完这 5 个 patch 后，文档即可作为后续 PR-03/05/06/07 继续填资源的 shell 基础。否则进入 §2.1 步骤 6（resource contract）即会暴露文档外口径，造成第二真源与文档冲突。

前一份 round2 报告（其他 agent 输出）中 "acceptanceContractLint 未覆盖 PR-02-1" 的 P0 已不成立，本轮不再列入。

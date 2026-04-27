# Dark UI/UX PR 文档深度 Review 1

审查对象：

- `UI/PLAN.md`
- `UI/ART_STYLE_BIBLE.md`
- `UI/pr/README.md`
- `UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md`
- `UI/pr/dark-uiux-pr01-client-shell-layout.md`
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md`
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md`
- `UI/pr/dark-uiux-pr04-profession-tree-ui.md`
- `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md`
- `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`
- `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md`

审查结论：当前 PR 拆分方向正确，但 PR 文档还不能直接进入实现。主要问题不是 UI 目标本身，而是资源管线合同没有收口到可执行状态：新 `dark-v1` sheet-plan 与现有 asset/style/manifest lint 的旧风格合同冲突，多个 PR 对 sheet 几何、sheetId、资源覆盖范围和白盒证据口径存在不一致。若直接开工，会很容易出现资源生成、manifest、golden、whitebox 互相通过不了，或者局部 PR 看似完成但最终全量替换缺口无法定位。

## Findings

### P1

#### P1-1：PR-00 没有把新 `dark-v1` 管线接入现有 lint/gate，后续 PR 的验证命令当前无法证明自身合同

**证据**

- `UI/PLAN.md:35-36` 要求新增 `assets-src/image/specs`、`scripts / tools` 的切图、contact sheet、manifest diff 与资源完整性校验。
- `UI/PLAN.md:276-284` 明确自动化必须覆盖 sheet plan schema、raw sheet 尺寸、row/col、targetKey coverage、manifest path、alpha bbox 与 contact sheet QA 状态。
- `UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:23-24` 只写“可新增 dry-run 脚本，但不接入正式 CI”。
- `UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:50-59` 验证只列 `verifyChanged`，新增脚本时只补 `verify-bootstrap.sh`，没有任何 `verify_sprite_sheet_map.py`、sheet-plan lint 或 root Gradle 入口。
- 当前 `build.gradle.kts:689-729`、`:732-768`、`:840-885` 的 `assetLint / styleLint / manifestLint` 仍消费 `assets-src/image/specs/*` 旧计划，不知道 `UI/sprite-sheets/sheet-plan.yaml`。
- 当前 `scripts/asset_pipeline_common.py:16` 固定 `EXPECTED_STYLE_TAG = "ktome-middle-fantasy-painterly-tile-v1"`；`scripts/asset-lint.py:85-89`、`scripts/style-lint.py:54-59` 会拒绝新 `ktome-dark-fantasy-sprite-ui-v1`；`scripts/manifest-lint.py:159-174` 还要求所有 visual asset plans 与 runtime manifest 共用同一个 styleTag。

**问题**

PR-02 到 PR-07 都把 `assetLint styleLint manifestLint` 当作资源合同验证入口，但 PR-00 没有要求扩展这些 lint 或新增等价 root task。按当前仓库状态，新 dark style plan 接入后会被旧 styleTag 常量拒绝；不接入则 lint 根本看不到 `sheet-plan.yaml`、contact sheet、QA report 与 dark-v1 runtime PNG。

**影响范围**

- PR-00 pipeline contract
- PR-02 UI chrome sprite pilot
- PR-03/05/06 资源生成与 manifest 更新
- PR-07 final golden/whitebox polish

**修复方向**

PR-00 必须升级为真正的管线 PR，而不是只写 dry-run 合同：

1. 明确新增或扩展 root Gradle 入口，例如 `darkSpriteSheetLint` / `spriteSheetMapLint`，并说明是否纳入 `assetLint styleLint manifestLint verifyChanged`。
2. 让新入口校验 `UI/sprite-sheets/sheet-plan.yaml`、raw sheet、sliced PNG、contact sheet、QA report、manifest `rawOutputPath`。
3. 明确旧 styleTag 与新 styleTag 的共存策略：是整体 bump runtime `visual-manifest.styleTag`，还是 lint 支持 per-plan style epoch。不能继续让旧 `EXPECTED_STYLE_TAG` 成为暗黑 UI 管线的隐形阻塞。
4. PR-02 之前必须完成上述接线，否则 PR-02 的“完整闭环”没有可执行验证入口。

#### P1-2：PR-00 定义的 `sheet-plan.yaml` schema 与上游 Mapping Spec 不一致，会制造第二套映射真相

**证据**

- `UI/PLAN.md:82-125` 定义 `sheet-plan.yaml` 的 sheet-level 字段：`sheetId / round / type / styleTag / rawSheetPath / outputRoot / promptBase / grid / cells`，cell 字段包含 `row / col / targetKey / category / outputName / subject`，并支持 `reserved`、`aliasOf`。
- `UI/ART_STYLE_BIBLE.md:219-227` 明确映射真相来自 `sheetId / row / col / targetKey / category / outputName`。
- `UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:28-31` 却把每个 cell 必填字段写成 `sheetId / row / col / targetKey / outputName / size / anchor / safeMarginPx / qaStatus`。

**问题**

PR-00 的 schema 漏掉了 `styleTag`、`type`、`rawSheetPath`、`grid`、`category`、`subject`、`reserved`、`aliasOf` 等上游硬合同，还把 `sheetId` 下沉到每个 cell，并引入 `size / anchor / safeMarginPx / qaStatus` 这些上游没有定义所有权的字段。这样实现者会面对两份 schema：`UI/PLAN.md` 一份，PR-00 一份。

**影响**

Prompt 生成无法稳定从 `subject` 生成；切分无法从 sheet-level `grid` 统一计算；`category` 无法决定后处理 canvas 与 lint 白名单；`reserved / aliasOf` 无法表达空格和合法复用；QA 状态混在 plan 里还会模糊“计划真相”和“验收结果”的边界。

**修复方向**

PR-00 应直接承接 `UI/PLAN.md` 的 YAML 结构。新增字段可以保留，但必须说明归属：

- plan 输入字段：`sheetId / round / type / styleTag / rawSheetPath / outputRoot / promptBase / grid / cells[].row/col/targetKey/category/outputName/subject/reserved/aliasOf`
- QA 输出字段：放在 report JSONL 中，例如 `qaStatus / rawSheetHash / cellHash / outputHash`
- 派生字段：`size / anchor / safeMarginPx` 若需要，必须说明是 category policy、cell override，还是切分输出，不得替代固定 sheet grid。

#### P1-3：PR-02 与 PR-03 的 cell 尺寸口径违反固定 sheet 类型合同

**证据**

- `UI/PLAN.md:65-72` 和 `UI/ART_STYLE_BIBLE.md:111-117` 规定 `icon-sheet = 1024x1024 / 8x8 / 128x128`，`large-sheet = 1024x1024 / 4x4 / 256x256`，`tile-sheet = 1024x1024 / 8x8 / 128x128`。
- `UI/ART_STYLE_BIBLE.md:119-124` 说明切分后的运行时尺寸由处理脚本按 category 输出，icon 默认处理到 `160x160`。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:27-32` 写 `r01-ui-hud-icons` 是 `8 x 8`，但单 cell `32x32` 或 `48x48`。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:26-32` 写 Round 7 sheet 单 cell `48x48`，装备 slot frame 可 `64x64`。

**问题**

上游已经冻结 source sheet 的 cell 尺寸，PR-02/03 把 runtime 读图尺寸或 UI slot 尺寸混进了 source sheet 几何。`1024x1024 / 8x8` 不可能同时得到 `32x32` 或 `48x48` source cell；这会直接破坏 `slice_spritesheet.py` 的 cellRect、alpha bbox、contact sheet 与 manifest path 校验。

**修复方向**

把 PR-02/03 中所有“单 cell `32x32 / 48x48 / 64x64`”改成：

- source sheet 固定：`icon-sheet 1024x1024 / 8x8 / 128x128`
- runtime output：由 category 后处理到目标 canvas，例如 icon 输出 `160x160`
- UI slot / display size：由 renderer/layout 决定，例如 HUD 显示 `32x32`，装备 slot 显示 `48x48` 或 `64x64`

#### P1-4：Round 1 有两个 sheet 没有落到任何 PR，UI chrome/frame 资源会被计划遗漏

**证据**

- `UI/PLAN.md:184-186` Round 1 明确包含 `r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons` 三张 sheet。
- `UI/PLAN.md:215-233` 新增 UI key 包含 panel、corner、edge、slot、tooltip、modal、HUD icon。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:9-13` 说要新增 UI chrome / HUD key 并让 client 消费至少一组新 key。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:27-32` 实际只生成 `r01-ui-hud-icons`。
- `UI/pr/dark-uiux-pr01-client-shell-layout.md:7` 明确 PR-01 不生成正式资源。
- `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md:7` 明确 PR-07 不新增大批量资源。

**问题**

`r01-ui-chrome` 和 `r01-ui-controls` 没有明确归属。PR-02 目标叫 UI Chrome Sprite Pilot，但只包含 HUD icons；PR-01 不生成资源，PR-07 不新增大批量资源。结果是 panel body、corner、edge、tooltip、modal、slot、tab、button、selection frame 等 UI chrome 主资源可能没有任何 PR 负责交付。

**修复方向**

二选一收口：

1. PR-02 扩为 Round 1 pilot group，至少包含 `r01-ui-chrome`、`r01-ui-controls`、`r01-ui-hud-icons` 的最小可运行子集。
2. 保持 PR-02 只做 `r01-ui-hud-icons`，但新增 PR-02.5 或把 `r01-ui-chrome / r01-ui-controls` 明确划给 PR-03/04/07，并同步更新 README 和 `UI/PLAN.md` 的 sheet inventory。

不能让 UI frame key 只存在于总方案表格里。

#### P1-5：PR-05/PR-06 的 sheetId 命名与总方案清单不一致，且 Round 8 漏掉 Berserker / Spellblade

**证据**

- `UI/PLAN.md:193-208` 的清单包含：
  - `r04-actors-player`、`r04-actors-humanoid`、`r04-actors-monster`、`r04-actors-boss`
  - `r05-bestiary-humanoid-icons`、`r05-bestiary-creature-icons`、`r05-boss-icons`
  - `r06-portraits-classes`、`r06-portraits-trees`、`r06-portraits-zones`
  - `r08-skills-vanguard-berserker`、`r08-skills-templar-rogue`、`r08-skills-arcanist-spellblade`
- `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:27-33` 改成了 `r04-actors-player-profession`、`r04-actors-companion`、`r05-bestiary-common`、`r05-bestiary-elite`、`r05-bestiary-boss`、`r06-portraits-profession`、`r06-portraits-tree`、`r06-portraits-zone`。
- `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:26-31` 把 Round 8 写成 `r08-skills-vanguard`、`r08-skills-arcanist`、`r08-skills-rogue`、`r08-skills-templar`，没有 `Berserker` 与 `Spellblade`。
- `UI/PLAN.md:177` 明确 Round 8 覆盖 Vanguard / Templar / Rogue / Arcanist / Spellblade / Berserker。

**问题**

sheetId 是后续 raw sheet path、contact sheet、QA report、manifest patch 的稳定 join key。PR 文档与总方案不一致会导致两个后果：

1. 实现者可能按 PR 文档生成 `r04-actors-companion`，但 review 或验证脚本按总方案寻找 `r04-actors-humanoid / monster / boss`。
2. Round 8 的职业覆盖缺了 `Berserker` 和 `Spellblade`，最终“全 manifest 收口”可能仍留下旧风格 skill/talent 资源。

**修复方向**

在 `UI/pr/README.md` 增加一张 “authoritative sheetId ownership table”，每个 sheetId 只出现一次，列出 owner PR、resource category、expected key prefix、expected count。PR-05/06 必须回写成与 `UI/PLAN.md` 同一套 sheetId，或先修改 `UI/PLAN.md` 再同步 PR 文档。

#### P1-6：`visual-manifest.styleTag` 与新视觉纪元的迁移策略缺失

**证据**

- `UI/ART_STYLE_BIBLE.md:18-21` 要求所有新 UI/视觉 prompt、spec、切分 report、contact sheet、manifest patch 记录 `ktome-dark-fantasy-sprite-ui-v1`，风格改变必须 bump。
- 当前 runtime manifest 顶层 `styleTag` 仍是 `ktome-middle-fantasy-painterly-tile-v1`，模型字段见 `client/src/main/kotlin/com/ktome/client/assets/ManifestModels.kt:5-18`。
- `UI/PLAN.md:40-43` 说不改 `VisualManifestEntry` schema，但没有说明顶层 `VisualManifest.styleTag` 是否从旧风格 bump 到 dark-v1。
- `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:46-51` 只要求玩家可见主路径不再指向旧风格资源，没有要求 manifest 顶层 styleTag、canonical manifest、runtime manifest 与 plan styleTag 的一致性。
- `scripts/manifest-lint.py:159-174` 当前会校验所有 plan styleTag 与 canonical/runtime manifest styleTag 一致。

**问题**

这是一个跨 PR 的稳定合同问题。若不更新 manifest 顶层 styleTag，`styleLint/manifestLint` 无法表达“新视觉纪元已切换”；若直接更新为 dark-v1，现有旧风格 plans 与旧资源又会和当前 lint 的单 styleTag 假设冲突。PR 文档没有给出迁移策略，最后很可能在 PR-06/07 才暴露为 lint 大面积失败。

**修复方向**

PR-00 必须明确以下三者之一：

1. 全量视觉纪元切换：在某个 PR 中把 canonical/runtime manifest 顶层 styleTag bump 到 `ktome-dark-fantasy-sprite-ui-v1`，并同步所有仍参与 manifest lint 的 plans。
2. 多 epoch 支持：manifest 顶层保持兼容字段，但 entry/report/plan 允许 style epoch，并让 lint 区分 player-visible dark-v1 与 historical/debug fallback。
3. 分阶段并行 manifest：新增 dark-v1 manifest 或 sidecar style report，直到 PR-06 完成再切 runtime manifest。

不建议把 styleTag 只记录在文档里。

### P2

#### P2-1：PR-06 的“全 manifest 收口”没有 key inventory 和 coverage artifact，验收标准不足以证明全量替换

**证据**

- `UI/PLAN.md:45-60` 给了资源规模：actor 57、portrait 26、icon 223、skill 118、status 23、quest 16、item 15、damage type 6、tile/prop/debug 等。
- `UI/PLAN.md:276-284` 要求 targetKey 覆盖率、manifest path 一致性与 contact sheet QA 状态校验。
- `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:46-51` 只写玩家可见路径不再指向旧风格、`assetLint` 能解析几个 key prefix、QA report 无 pending/rejected。

**问题**

PR-06 的目标是“玩家可见 manifest 收口”，但文档没有定义：

- 需要覆盖哪些 exact key 或 key prefix。
- 各 category 的 expected count 是否必须等于 `UI/PLAN.md` inventory。
- 哪些旧风格路径允许作为 debug/history fallback。
- 如何区分 `icon`、`icon_skill`、`icon_status`、`icon_quest`、`actor_sprite`、`portrait`、`tile_*`、`prop_*` 的 player-visible 主路径。
- coverage report 的路径、字段与 fail-fast 条件。

`assetLint` 只能证明 key 可解析，不能证明“所有玩家可见 key 已经切到 dark-v1 且风格一致”。

**修复方向**

PR-06 增加 manifest coverage artifact，例如 `assets-src/image/manifests/dark-v1-manifest-coverage.json` 或 JSONL summary，至少包含：

- `expectedKeyCountByCategory`
- `coveredKeyCountByCategory`
- `oldStylePlayerVisibleKeys`
- `allowedFallbackKeys`
- `pendingOrRejectedPlayerVisibleCells`
- `styleTag`
- `sourceSheetIds`

PR-07 再消费这份 artifact 做最终 golden/whitebox polish。

#### P2-2：每个 PR 的 golden label 和人工白盒证据没有具体化，PR-07 还把 packaged app 白盒写成可选

**证据**

- 仓库规则 `AGENTS.md:204-208` 要求涉及 client、渲染、输入、交互、Tile、package 的改动保留明确白盒步骤；涉及安装包的改动还要补安装包启动与验收步骤。
- `UI/pr/README.md:23` 只规定 golden label 使用 `dark-uiux-prNN-*` 前缀。
- PR-01/03/04/05/06/07 的人工白盒章节只描述要看什么，没有写固定 seed、scenario id、manual record path、截图路径、golden label 清单。
- `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md:11-12` 把 packaged app 白盒列为阶段目标，但 `:54` 又写“如触发 packaged app 验收”。

**问题**

这些 PR 是玩家可见 UI 大改，光有 `clientSmoke` 和 `goldenScreenshot` 命令不够。缺少固定 label、场景、manual record 路径与 packaged app 启动步骤，会导致每个 PR 的视觉验收不可复现，最终 PR-07 难以判断哪些截图是 owner evidence，哪些只是一次人工观察。

**修复方向**

为每个 PR 补最小证据表：

| PR | 必填证据 |
| --- | --- |
| PR-01 | `dark-uiux-pr01-shell-1280x800`、`dark-uiux-pr01-shell-min-window`、manual record 路径 |
| PR-02 | `dark-uiux-pr02-hud-icons-pilot`、contact sheet QA 路径、manifest diff 路径 |
| PR-03 | `dark-uiux-pr03-inventory-empty`、`dark-uiux-pr03-inventory-stacked`、fallback key 注入记录 |
| PR-04 | `dark-uiux-pr04-talent-sidebar-start`、`dark-uiux-pr04-active-slot-choice`、`phase4-v4-pr01` scenario 使用方式 |
| PR-05 | 固定 seed + map/actor/telegraph/groun loot 层级截图 |
| PR-06 | skill/status/quest/profession tree 同屏截图 + manifest coverage artifact |
| PR-07 | packaged app 命令、runtime home、evidence dir、manual record path、最终 doc-vs-implementation checklist |

PR-07 的 packaged app 白盒应从“如触发”改为必跑，除非明确说明本轮没有 package-facing 变更。

#### P2-3：PR-02/03/06 的新 UI key 与资源 key 清单不够精确，容易重新引入裸字符串

**证据**

- `UI/PLAN.md:120-125` 要求 targetKey 全局唯一，新增 UI chrome key 必须同 PR 加 manifest entry，复用必须显式 `aliasOf`。
- `UI/PLAN.md:215-233` 只列出部分 UI frame/HUD key。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:29-31` 只以自然语言列 health、stamina、xp、gold、key、backpack、equipment slot、quest marker、log marker、warning、modal close、empty slot、selection frame。
- `UI/pr/dark-uiux-pr03-equipment-inventory-items.md:28-32` 只列资源族群，没有 exact targetKey。
- `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md:48-50` 只列 key prefix，没有 exact coverage table。

**问题**

K-ToME 的稳定合同要求 ID、manifest key、resource key 集中建模，不应靠 renderer 里的临时字符串推进。当前文档对新 key 的定义不够精确，实施阶段容易出现：

- `ui.hud.hp.icon` 与 `ui.health.icon` 这类别名漂移。
- `slot.empty`、`ui.frame.slot.empty`、`inventory.slot.empty` 三种写法并存。
- PR-06 只按 prefix 粗查，漏掉部分 item/zone/tree/fallback key。

**修复方向**

在 PR-00 或 PR-02 增加 `UI key registry` 小节，至少列：

- `targetKey`
- `category`
- `owner PR`
- `sheetId`
- `fallbackKey`
- `consumer file/test`
- `aliasOf` 是否允许

PR-03/06 对 item/skill/status/quest/tree key 也使用同一格式。

#### P2-4：PR-05 的 atlas 例外条件放错了阶段

**证据**

- `UI/PLAN.md:40-41` 规定第一阶段不改 `VisualManifestEntry` schema，仍输出单 PNG。
- `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md:34-38` 把 atlas/region manifest 设为非目标。
- `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md:46` 写“不引入 atlas，除非 PR-02 已证明单 PNG 不可接受”。
- `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md:40-44` 才定义单 PNG 加载成本与 atlas 决策。

**问题**

PR-02 是资源链路 pilot，不是性能决策 PR；PR-05 是大批量资源替换，最容易因为性能焦虑半途扩大 schema。把 atlas 例外放在 PR-05，会提前打开 `VisualManifestEntry` schema 或 renderer resource model 的变更窗口，和 `UI/PLAN.md` 的“第一阶段不改 schema”冲突。

**修复方向**

PR-05 应固定“不引入 atlas/region manifest，无例外”。性能问题只能记录到 PR-07，由 PR-07 基于实测决定是否开独立 atlas PR。

#### P2-5：PR-04 的上游职业树合同缺少明确 stop condition

**证据**

- `UI/pr/dark-uiux-pr04-profession-tree-ui.md:6` 写职业树语义“已落地或作为上游合同处理”。
- `UI/PLAN.md:300` 写如果职业树功能分支尚未合入，必须以它为上游分支或等价文档合同，不能在 UI PR 中重新实现职业树规则。

**问题**

PR-04 的原则是对的，但缺少明确执行判定：如果 `TalentSidebarPresenter` / `TalentTreeNodeSnapshot` / `ACTIVE_TALENT_SLOT_CHOICE` 在目标分支没有落地，PR-04 应该基于哪个分支、读哪个 artifact、跑哪个 upstream gate、遇到接口缺失时是阻塞还是临时 mock。没有 stop condition，开发者可能在 UI PR 里补规则字段或临时 snapshot，踩中“第二套 talent snapshot”的红线。

**修复方向**

PR-04 增加前置检查：

1. `rg` 确认 `TalentSidebarPresenter`、`TalentTreeNodeSnapshot`、`ACTIVE_TALENT_SLOT_CHOICE` 存在。
2. 若不存在，停止 PR-04，先合入/切到上游职业树分支。
3. 禁止在 PR-04 新增 core/game 职业树语义字段；如 presentation 缺字段，只能回上游合同补。

### P3

#### P3-1：PR-00 的“保留总方案和 PR 索引，不写具体执行细节”表述容易误导

`UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:20` 写 `UI/PLAN.md` 只保留总方案和 PR 索引，不写具体执行细节。但 `UI/PLAN.md` 实际已经定义 sheet type、mapping spec、prompt contract、slice/verification flow、sheet inventory、asset deliverables 和 common test plan。建议改成“PR-00 不在 `UI/PLAN.md` 新增重复执行细节；若 schema 细化，必须回写上游 Mapping Spec”。

#### P3-2：文件名 `codex-ui-revew-1.md` 拼写为 `revew`

本报告按用户指定文件名落盘。后续如果需要连续 review，建议保留本文件名不改，新增 `codex-ui-review-2.md` 或在 README 中注明历史拼写，避免链接断裂。

## Requirement Alignment

- Requirement: `UI/PLAN.md` 要求 `sheet-plan.yaml` 是唯一映射真源，且自动化覆盖 schema、grid、coverage、manifest path、alpha bbox、contact sheet QA。
  - Evidence: `UI/PLAN.md:82-125`、`:149-164`、`:276-284`；PR-00 只定义 dry-run schema，未接入正式 lint/gate：`UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md:23-24`、`:50-59`。
  - Conclusion: 部分一致

- Requirement: 新视觉纪元固定为 `ktome-dark-fantasy-sprite-ui-v1`，manifest patch、report、contact sheet 都必须记录。
  - Evidence: `UI/ART_STYLE_BIBLE.md:18-21`；当前 manifest model 有顶层 `styleTag`，现有 runtime manifest 仍是旧 style tag；PR-06 未定义 styleTag 迁移策略。
  - Conclusion: 部分一致

- Requirement: 三类 sheet 固定 canvas/grid/cell，不允许每轮自由发挥尺寸。
  - Evidence: `UI/PLAN.md:65-72`、`UI/ART_STYLE_BIBLE.md:111-117`；PR-02/03 写了 `32x32 / 48x48 / 64x64` cell。
  - Conclusion: 不一致

- Requirement: PR 拆分应覆盖 9 轮约 27 张核心 sheet，结束后玩家可见资源指向新纪元资源。
  - Evidence: `UI/PLAN.md:62-63`、`:182-211`、`:297-300`；PR-02 漏 `r01-ui-chrome / r01-ui-controls`，PR-05/06 sheetId 与总方案不一致，PR-06 Round 8 漏 Berserker / Spellblade。
  - Conclusion: 部分一致

- Requirement: 涉及 client、渲染、输入、Tile、package 的改动必须有明确白盒步骤。
  - Evidence: `AGENTS.md:204-208`；PR 文档只有人工观察清单，PR-07 把 packaged app 验收写成“如触发”。
  - Conclusion: 部分一致

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前文档状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| Dark sprite pipeline | `sheet-plan.yaml` 唯一真源，脚本/lint 覆盖切分、manifest、QA | 部分实现 | `UI/PLAN.md:82-164`; PR-00 `:23-31`, `:50-59` | PR-00 未定义正式 lint/root task，现有 lint 不识别新 sheet plan/style tag | High |
| Sheet geometry | 固定 `1024x1024` sheet 与 `128/256` source cell | 偏离实现 | `UI/PLAN.md:65-72`; PR-02 `:27-32`; PR-03 `:26-32` | PR 文档把 runtime/display size 写成 source cell size | High |
| Sheet inventory | 9 轮约 27 张核心 sheet | 部分实现 | `UI/PLAN.md:182-211`; PR-02 `:27-32`; PR-05 `:27-33`; PR-06 `:26-31` | Round 1 漏 UI chrome/controls；PR-05/06 sheetId 漂移；Round 8 漏职业 | High |
| Manifest/style contract | 新风格 tag 可追踪，runtime manifest 与计划一致 | 未实现 | `UI/ART_STYLE_BIBLE.md:18-21`; `ManifestModels.kt:5-18`; PR-06 `:46-51` | 没有 styleTag bump 或多 epoch 策略 | High |
| Client shell layout | 三栏 + bottom HUD，删除重复状态栏 | 大体一致 | PR-01 `:9-15`, `:41-61` | 范围与验证基本合理，仍需固定 whitebox label/record | Medium |
| Inventory/equipment | 右侧装备背包 grid，item icon 与 tooltip | 部分一致 | PR-03 `:9-15`, `:41-61` | UI 行为写清了，但 key inventory 与 source cell 尺寸有问题 | High |
| Profession tree UI | 只消费上游 presentation，不改规则 | 大体一致 | PR-04 `:17-23`, `:44-50` | 缺少上游未落地时的 stop condition 和 evidence path | Medium |
| Final golden/whitebox | 全 UI golden、manual record、packaged app 白盒、性能决策 | 部分一致 | PR-07 `:9-14`, `:46-62` | packaged app 写成可选，缺 scenario/manual record/label 清单 | Medium |

## 玩法与体验审查

### 核心循环

PR-01 的三栏 + bottom HUD 方向符合当前问题：地图保留主视图，左栏提供区域/任务提示，右栏承载角色与背包，底部集中日志和快捷键。主要体验风险在于资源与 layout 分阶段错位：如果 PR-01 只落 primitive shell，而 PR-02 没有交付 frame/slot/modal chrome，前几个 PR 会长期处在“结构变了但质感没跟上”的半成品状态。

### 战斗体验

PR-05 明确要保护 Boss warning / telegraph 读图，这是正确方向。但该 PR 只用人工白盒描述“第一眼可读性”，没有固定 seed、golden label 或层级断言名。战斗反馈是暗黑 UI 最容易被低对比风格压暗的区域，必须把 telegraph、危险地形、actor、ground loot marker 的遮挡关系变成测试和截图证据。

### 成长与构筑驱动

PR-04 把职业树 UI 绑定 `TalentSidebarPresenter`，不重做规则，这是对的。风险是上游职业树合同如果未落地，PR-04 文档没有明确停止条件，容易为了 UI 先补临时 snapshot 字段。职业树是成长体验核心，不能让 UI PR 自己派生一套 talent 状态。

### 奖励驱动与掉落体验

PR-03 把装备/背包图标化，能显著改善 reward uptake。但 PR-03 没有 exact item/equipment key 清单，且 source cell size 写错。若不先修正，最终可能出现品质 frame、空 slot、locked slot、item icon 混用裸路径或重复 key，削弱装备识别。

### 探索与新鲜感

PR-05 的地图/actor/portrait 替换是最能改善首屏观感的部分。当前最大问题是 sheetId 和资源组命名漂移，导致资源批次无法与 QA report、manifest coverage 对齐。探索体验需要 zone/tile/prop 读图区分，不能只靠“生成 Round 2-6”这种粗粒度描述。

### 新手体验与信息反馈

PR-01/03/04/06 都强调 tooltip、fallback、锁定原因、状态/技能 icon，这是正确的。但 fallback 的体验证据不足：缺失 key 应该在 report 中可定位，且 fallback 本身也要符合 dark style。当前文档没有定义 fallback key 清单、注入方式和 report 字段。

### 系统耦合与体验断层

现在最大的断层是“文档目标是新视觉纪元，验证系统仍是旧 styleTag + 旧 asset plan”。这会让开发者以为跑了 `assetLint/styleLint/manifestLint` 就完成暗黑 UI 验证，实际这些 gate 目前不覆盖 `UI/sprite-sheets`。

## 当前阶段必须解决的问题

1. **PR-00 必须先落正式 dark sprite pipeline gate。**
   - 为什么必须现在修：PR-02 之后所有资源 PR 都依赖它。
   - 不能推迟原因：否则每个资源 PR 都会自行解释 sheet-plan、styleTag、manifest 校验，形成第二真相。
   - 修复方向：新增/扩展 root lint task，接入 `verifyChanged` 或 README close gate。
   - 优先级：P1

2. **统一 source sheet 几何，删除 PR-02/03 的 `32x32 / 48x48 / 64x64` cell 口径。**
   - 为什么必须现在修：一旦 raw sheet 按错尺寸生成，后续切分、contact sheet 和 manifest hash 全部不可复用。
   - 修复方向：source cell 固定 `128/256`，display size 写到 renderer/layout。
   - 优先级：P1

3. **建立 authoritative sheetId ownership table。**
   - 为什么必须现在修：PR-05/06 已经和总方案命名不一致，Round 1 也有孤儿 sheet。
   - 修复方向：在 `UI/pr/README.md` 增加每个 sheetId 的 owner PR、category、expected count、key prefix。
   - 优先级：P1

4. **明确 visual manifest styleTag 迁移策略。**
   - 为什么必须现在修：现有 lint 明确要求 plan/manifest styleTag 一致。
   - 修复方向：PR-00 选择全量 bump、多 epoch lint 或并行 manifest，不要留到 PR-06。
   - 优先级：P1

5. **PR-07 packaged app 白盒必须必跑并固定证据路径。**
   - 为什么必须现在修：UI 大改是玩家可见改动，最终验收不能只停留在测试报告。
   - 修复方向：补 `packageMacApp` / whitebox scenario / manual record / evidence dir。
   - 优先级：P2

## Removal/Iteration Plan

### Defer Removal: 旧风格 player-visible runtime paths

| Field | Details |
| --- | --- |
| Location | `client/src/main/resources/manifests/visual-manifest.json` |
| Phase/Work Package | `dark-uiux-pr06` 收口，`dark-uiux-pr07` 验证 |
| Touched contract | `VisualManifest.styleTag`, `VisualManifestEntry.rawOutputPath`, player-visible resource style |
| Evidence | `UI/PLAN.md:297-300` 允许旧资源迁移期保留，但新 manifest 默认只指向新纪元资源；PR-06 `:48` 要求玩家可见主路径不再指向旧风格 |
| Preconditions | PR-00 完成 styleTag/lint 策略；PR-02/03/05/06 完成 coverage artifact |
| Iteration steps | 1. 生成 dark-v1 coverage report 2. 标记 allowed debug/history fallback 3. 将 player-visible keys 切到 dark-v1 4. PR-07 检查无旧风格混入 |
| Affected gates | `assetLint`, `styleLint`, `manifestLint`, `goldenScreenshot`, `clientSmoke`, `verifyChanged`, dark sprite sheet lint |
| White-box check | PR-07 packaged app + manual record |
| Rollback | 保留旧资源文件，但 runtime manifest 主路径回滚必须按 PR owner 回滚，不能在 PR-07 大段改回 |

## Additional Suggestions

- PR-00 增加 `docs` 小节：明确 `UI/PLAN.md` 是上游合同，PR 文档只能细化，不能覆盖 sheet type、styleTag、sheetId、mapping key。
- PR-02 的 pilot 可以缩小资源数量，但不能缩小合同维度。即使只生成少量 cell，也必须覆盖 raw sheet、slice、processed PNG、manifest、golden、fallback、QA report 全链路。
- PR-03 建议把装备 slot frame 与 item icon 分开建 key，slot frame 属于 UI chrome，item icon 属于 item visual，不要让品质 frame 伪装成 item key。
- PR-05 建议把 tile/prop/VFX/actor/portrait 拆成内部批次和 checklists，即使同属一个 PR，也要有每批 contact sheet QA 和 manifest diff。
- PR-06 建议把 `assetLint` 之外的 “player-visible dark-v1 coverage” 做成单独 report，避免把“可解析”误当“已替换”。

## Suggested Verification

本次只写审查报告，未运行 Gradle 测试。建议文档修正后按以下顺序验证：

```bash
rg -n "32x32|48x48|64x64|r04-actors-player-profession|r04-actors-companion|r05-bestiary-common|r05-bestiary-elite|r05-bestiary-boss|r08-skills-vanguard|r08-skills-arcanist|r08-skills-rogue|r08-skills-templar" UI/pr UI/PLAN.md
rg -n "r01-ui-chrome|r01-ui-controls|r01-ui-hud-icons" UI/pr UI/PLAN.md
rg -n "ktome-dark-fantasy-sprite-ui-v1|ktome-middle-fantasy-painterly-tile-v1|EXPECTED_STYLE_TAG" UI scripts build.gradle.kts client/src/main/resources/manifests/visual-manifest.json
```

管线实现后再跑：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint :client:clientSmoke :client:goldenScreenshot verifyChanged
```

若 PR-00 修改 Gradle、bootstrap、processResources 或 lint task 接线：

```bash
./scripts/verify-bootstrap.sh
```

PR-07 最终还应补：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp
```

并按新增的 dark UI whitebox scenario 记录 packaged app 启动日志、截图、manual record 与 evidence dir。

## Summary

这组 PR 文档可以作为 UI 大改版的骨架，但现在还缺少“让实现者不会走偏”的硬合同。最先要修的是 PR-00：把 `dark-v1` sheet-plan、styleTag、manifest coverage 和 QA report 接入真实 lint/gate。随后统一 PR-02/03 的 cell 尺寸、PR-05/06 的 sheetId、PR-06 的 coverage artifact、PR-07 的 packaged app 白盒证据。修完这些以后，PR 拆分才具备可串行实现、可 review、可回滚、可验证的条件。

# 暗黑 UI/UX PR 文档深度 Review 报告（第二轮）

> 评审日期：2026-04-27（同日二审）
> 评审范围：`UI/PLAN.md` + `UI/ART_STYLE_BIBLE.md` + `UI/pr/README.md` + `UI/pr/dark-uiux-pr00…pr07-*.md`
> 上一轮报告：[2026-04-27-dark-uiux-pr-deep-review.md](./2026-04-27-dark-uiux-pr-deep-review.md)
> 与第一轮的关系：第一轮聚焦"会让某个 PR 中途返工或合入即破坏 lint 的硬阻塞"。文档已经被大幅修订，绝大部分 P0 / P1 都落到了 README、PLAN 和 PR-00。本轮聚焦"残留的细节漂移、内部不一致、表述错位、被忽略的边界用例"。
> 评审角度：合同自洽、文档间引用一致、PR 内字段闭环、可执行性、可审计性、未来维护成本。

---

## 0. TL;DR

第一轮提出的 3 个硬阻塞已基本解决：

| 上一轮 H# | 当前状态 |
| --- | --- |
| H1 styleTag 升级路径 | 采用 multi-epoch sidecar，PR-00 §6 / PLAN §Pipeline Gate Strategy 已固定。仍有 2 个执行细节需补（见 §1.1、§5.1） |
| H2 sheet-plan.yaml 没有 lint 入口 | PR-00 §5 引入 `darkSpriteSheetLint / spriteSheetMapLint / darkManifestCoverageLint` ✅；但三 task 的 input 边界仍模糊（见 §3.3） |
| H3 8 职业覆盖 | PLAN §Profession Coverage Scope + README + ART_STYLE_BIBLE §7.3 三处都补齐 ✅ |

本轮发现 **1 个新硬阻塞**：

| # | 问题 | 影响 PR |
| --- | --- | --- |
| **N1** | PR-02 §4 UI Key Registry 表只列 5 个 HUD icon，但 §3.3 prose 列了 10 个（含 `warning / log marker / quest marker / backpack / equipment`）。registry 缺这 5 条 → PR-02 close 时 cell↔key 映射本身不闭环；下游 PR-05 telegraph 无 key 可挂 | PR-02、PR-05 |

其他都属"细节修复"层级：sheet ID 表格中 `r07-items-affix-material` category 错配、PR 范围条文与 README ownership 表条款重复或漂移、`tree.*` 命名/数量与 PR-04 "三棵职业树"措辞潜在打架、verify-script input set 错位、coverage artifact 文件名缺一处 deliverable 注册等。

总条目 **47 项**，已分级为 P0（动 PR-02 前必修，4 项）/ P1（PR-00 期间补，14 项）/ P2（实施期顺手补，29 项）。详见 §10。

---

## 1. 第一轮已修但仍需收尾的小尾巴

### 1.1 `EXPECTED_STYLE_TAG` 留在 painterly 的运维成本未在文档中显式

- 选 sidecar 策略意味着 `scripts/asset_pipeline_common.py:16` 的 `EXPECTED_STYLE_TAG` 不会改；`visual-manifest.json` 顶层 `styleTag` 也不会从 painterly 改为 dark-v1。
- 后果：PR-06 收口后，manifest 顶层 styleTag 仍是 painterly，但绝大多数 entry 的 `rawOutputPath` 指向 `dark-v1/`。运维 / 调试 / 报错日志读 `styleTag` 字段时会被误导。
- 文档现状：PLAN §Pipeline Gate Strategy 第 1 条 "VisualManifest.styleTag 在混合资源阶段继续保持现有 canonical manifest 兼容语义"，但没说明"读 styleTag 不等于读实际渲染纪元"。
- **建议**：PLAN §Pipeline Gate Strategy 加一条收尾说明："顶层 styleTag 不 bump 是 sidecar 策略代价；要判断 entry 真实风格，应查 raw path、`dark-v1-sprite-report.jsonl` 或 `dark-v1-manifest-coverage.json`，不能单独看顶层 styleTag。" 或在 ART_STYLE_BIBLE §1 第 5 条 "v2 才 bump" 旁补"v1 阶段顶层 styleTag 与实际渲染纪元解耦"。

### 1.2 旧 `manifestLint` 的 `prefixRules` 是否接受新 `ui.*` 前缀未在 PR-00 验证

- 若 `scripts/manifest-lint.py` 的 `prefixRules` 是白名单制，新增 `ui.frame.*` / `ui.hud.*` / `ui.control.*` / `ui.combat.*` / `ui.state.*` 五个前缀未注册时，PR-02 一合即破旧 lint。
- PR-00 §4 第 4 条只说 "旧 manifestLint 继续保护 canonical/runtime key、field、styleTag、prefixRules 一致性"，没把"扩 prefixRules"列入 PR-00 实现任务。
- **建议**：PR-00 §3 增加任务 "审计 `manifest-lint.py prefixRules`，必要时把 `ui.frame.* / ui.hud.* / ui.control.* / ui.combat.* / ui.state.*` 加入白名单；这步必须在 PR-02 启动前完成。"

---

## 2. 新硬阻塞

### N1. PR-02 §4 UI Key Registry 与 §3.3 prose 不闭环

PR-02 §3.3 描述 `r01-ui-hud-icons` 的内容包含 **10 类**：health、stamina、xp、gold、key、backpack、equipment、quest marker、log marker、warning。

但 PR-02 §4 UI Key Registry 表中实际只列出 5 个 HUD icon：`ui.hud.hp.icon`、`ui.hud.stamina.icon`、`ui.hud.xp.icon`、`ui.hud.gold.icon`、`ui.hud.key.icon`。**少 5 条**：backpack、equipment、quest marker、log marker、warning。

- `ui.control.backpack.icon` 与 `ui.control.equipment.icon` 在 §4 表中归到 `r01-ui-controls`——这与 §3.3 prose 把 backpack/equipment 列在 hud-icons 矛盾。如果实际 owner 是 controls，§3.3 prose 错；如果 owner 是 hud-icons，§4 表错。需要先决定 ownership。
- `quest marker / log marker / warning` 三个 key 在 §4 表中**完全缺失**。warning 尤其关键——下游 PR-05 §6.2 要 "Boss warning / telegraph 不被普通 VFX 淹没"，warning icon 没 key 就无 visual key 可挂。
- 对比 PLAN.md §UI Key Additions 表：只列 `ui.hud.hp.icon / stamina.icon / xp.icon / gold.icon`——4 条，连 PR-02 §4 的 5 条都对不上。

**影响**：
- PR-00 关闭时 dry-run fixture 假设 sheet ↔ registry ↔ manifest 闭环，PR-02 实战阶段才会暴露 5 个 cell 没有 key registry 行 → owner-scope `darkManifestCoverageLint` 无法判定通过。
- 后续 PR-05 telegraph 需引用 warning icon，发现没 key 临时新增，违反 README §Key Registry Contract 中"禁止 renderer 里新增未登记的裸字符串资源路径"。

**修复**：PR-02 §4 表必须扩到与 §3.3 prose 一致；同时 PLAN §UI Key Additions 表跟着扩。建议补的 5 条：

| targetKey | Sheet | Owner | Consumer |
| --- | --- | --- | --- |
| `ui.hud.backpack.icon` | `r01-ui-hud-icons` | PR-02 | right panel inventory header |
| `ui.hud.equipment.icon` | `r01-ui-hud-icons` | PR-02 | right panel equipment header |
| `ui.hud.quest_marker.icon` | `r01-ui-hud-icons` | PR-02 | left rail quest summary |
| `ui.hud.log_marker.icon` | `r01-ui-hud-icons` | PR-02 | bottom log critical hint |
| `ui.hud.warning.icon` | `r01-ui-hud-icons` | PR-02 | bottom HUD danger; **PR-05 telegraph 共用** |

如果决定 backpack/equipment 走 `ui.control.*`（PR-02 表已经这样），则 §3.3 prose 必须删 "backpack、equipment"。两边二选一即可，但**必须在 PR-02 close 前对齐**。

---

## 3. README ↔ PLAN ↔ PR 字段 / 表格漂移

### 3.1 `r07-items-affix-material` 的 category 错配

- README §SheetId Ownership 表：`r07-items-affix-material | PR-03 | icon | affix, material, quality frame, slot state`
- PR-03 §3.2 第 2-3 条："品质 frame、empty slot、locked slot 使用 `ui.frame.*` 或 `ui.state.*` key"——这两类 key 的 category 是 `ui_frame` 或 `icon`（state），不是单一 `icon`。
- PR-03 §3.6 "装备 slot frame 属于 `ui_frame`，优先复用 PR-02 的 `ui.frame.slot.*`"——已经把 slot frame 划归 PR-02。
- 矛盾：README 把 "slot state" 划到 r07，PR-03 又声明复用 PR-02 的 r01-ui-chrome。
- **修复**：要么 README §SheetId Ownership 表 `r07-items-affix-material` Scope 删 "quality frame / slot state"，把它们留给 r01-ui-chrome；要么 PR-03 明确"`ui.state.*` 是 PR-03 新增、`ui.frame.slot.*` 沿用 PR-02"，并把 "slot state" 在 ownership 表里写出确切 key family（比如 `ui.state.locked.icon`）。

### 3.2 `r03-props-environment` 与 `r06-portraits-zones` Scope 重叠

README §SheetId Ownership：
- `r03-props-environment | PR-05 | prop_environment | zone.*.visual, environment props`
- `r06-portraits-zones | PR-05 | portrait | zone.*.visual, secret zone visuals`

两张 sheet 都声明覆盖 `zone.*.visual`。两者的实际差异是 prop_environment 是地图 prop（小尺寸、`large-sheet 4x4`），portrait 是 zone 缩略图（也 `large-sheet 4x4`）。**同一 key family 不能同时由两张 sheet owner**，PLAN.md §Mapping Spec 第 1 条 "sheetId + row + col 全局唯一" 之外，targetKey 也必须唯一（除非显式 alias）。

- **修复**：明确分流。建议：
  - `zone.*.visual` 大图（zone portrait）归 `r06-portraits-zones`。
  - `r03-props-environment` 改用 `prop.zone.*` 或 `interactable.zone.*` 命名空间，与 portrait key 不冲突。

### 3.3 三个 dark lint 的 input set 边界不正交

PR-00 §5 给三个 lint：

| Lint | 写到的 input |
| --- | --- |
| `darkSpriteSheetLint` | sheet-plan schema、sheet type、styleTag、grid、reserved/alias、repo path |
| `spriteSheetMapLint` | raw sheet 尺寸、切分输出、contact sheet、QA report、alpha bbox、hash、manifest path |
| `darkManifestCoverageLint` | player-visible key 是否切到 dark-v1，coverage artifact |

但 PR-00 §3 verify_sprite_sheet_map.py 的 input 列表写 "sheet plan + manifest + reports"——**没有 raw sheet**。而 §3 第 11 条又说 "verify_sprite_sheet_map.py 必须能在 raw PNG 缺失时输出明确的 `missingRawSheet`"——脚本不读 raw sheet怎么报缺失？

矛盾来自三个 lint 的实际责任划分没有正交矩阵。**修复**：PR-00 §5 增加一张 input/output 矩阵，例如：

| Lint Task | Input Set | 输出 |
| --- | --- | --- |
| `darkSpriteSheetLint` | `UI/sprite-sheets/sheet-plan.yaml` + `key-registry.yaml` | schema/路径/前缀错误 |
| `spriteSheetMapLint` | sheet-plan + raw sheet PNG + 切分 PNG + contact sheet + QA JSONL + canonical manifest | 尺寸/切分/QA/path 一致性 |
| `darkManifestCoverageLint` | sheet-plan + canonical/runtime manifest + sprite-report + coverage artifact | dark-v1 player-visible 覆盖率 |

这样 verify_sprite_sheet_map.py 的 input 就明确包含 raw sheet。

### 3.4 `darkManifestCoverageLint` 三模式定义双处维护

- README §Manifest Authority 给了 3-mode 表
- PLAN.md §Pipeline Gate Strategy 给了同样的 3-mode 表
- PR-00 §5 又给了一遍

三处都说同一件事，未来改某一处会忘改另两处。**修复**：PLAN 是权威定义，README 和 PR-00 引用 PLAN。同样适用于：
- "HUD ↔ item namespace 分开" 表（README §Manifest Authority + PLAN 隐含）
- "三类 sheet type" 表（PLAN §Sheet Types + ART_STYLE_BIBLE §6）
- "category policy 派生 size/anchor/safeMarginPx" 段落（PR-00 §3.4 + PLAN §Pipeline Gate Strategy）

### 3.5 PLAN §UI Key Additions 表与 PR-02 §4 表条数不一致

- PLAN §UI Key Additions：12 条（panel + slot + tooltip + modal + 4 个 hud icon）
- PR-02 §4：16 条（chrome 9 条 + hud 5 条 + control 2 条）

PLAN 表完全缺 `ui.control.*` / `ui.combat.*` / `ui.state.*` 三个前缀。但 PLAN.md §Mapping Spec 的示例 yaml 里又写了 `ui.combat.action.icon` / `ui.combat.method.icon`——示例与 §UI Key Additions 表自相矛盾。

**修复**：PLAN §UI Key Additions 必须扩成全表（参考 PR-02 §4 + N1 修复后的版本），并 anchor 到 PR-02 owner。

### 3.6 PLAN 示例 yaml 的 `sheetId` 与 `cells[].targetKey` 错配

PLAN.md §Mapping Spec 给的示例：

```yaml
sheets:
  - sheetId: r01-ui-hud-icons
    ...
    cells:
      - targetKey: ui.combat.action.icon
      - targetKey: ui.combat.method.icon
```

但 README §SheetId Ownership 明确 `ui.combat.*` 属于 `r01-ui-controls`（"`ui.control.*`, `ui.combat.*`, `ui.state.*`"），不是 `r01-ui-hud-icons`。**示例 sheet 写错了**。

**修复**：PLAN §Mapping Spec 示例改成在 `r01-ui-controls` 下放 `ui.combat.*`，或在 `r01-ui-hud-icons` 下用真实的 HUD key（如 `ui.hud.hp.icon`）。

### 3.7 README §Manual Codex App Raw Sheet Workflow 第 3 条编号语义模糊

> "prompt 文件按 `001-r01-ui-chrome.prompt.txt` 这种格式编号；编号只表示执行顺序，语义仍以 `sheetId` 为准。"

但 PR-00 §3 第 7 条 "prompt 文件名固定为 `{threeDigitOrder}-{sheetId}.prompt.txt`"——`threeDigitOrder` 怎么生成、是否随 sheet 增删而漂移、prompt-index.json 的 hash 是否依赖编号本身没说。

风险：PR-05 加新 sheet 后，已有 prompt 文件可能因 reorder 全部 rename → contact sheet 路径错位、QA 报告失联。

**修复**：PR-00 §3 加一条 "编号生成稳定性"：
1. 编号一旦生成必须冻结，不因新增 sheet 重排（要么留缝隙编号 010/020/030，要么追加到末尾）。
2. `prompt-index.json` 必须以 `sheetId` 而非编号作为唯一键。
3. `promptHash` 只对 prompt 内容 hash，不含编号本身。

### 3.8 README §职业树专用规则 与 PR-04 §硬依赖合同 重复定义

README §职业树专用规则 5 条 vs PR-04 §硬依赖合同 5 条——大部分重复（typed enum、TalentSidebarPresenter authority、不改 learnableTalentIds、数字键边界）。

**修复**：PR-04 §硬依赖合同 引用 README §职业树专用规则，只列 PR-04 特有的（PR-06 跨 PR 时序约束等）。

---

## 4. PR 内部字段 / 措辞细节

### 4.1 PR-01 §影响范围 测试路径用通配 `*`

`client/src/test/kotlin/com/ktome/client/render/*` —— 其他 PR 都给具体测试文件。PR-01 §6 验证已经写了 `GameShellLayoutTest`、`TileRendererCanvasTest`、`AsciiRenderModelTest` 三个，§影响范围表里就该直接写出。

### 4.2 PR-02 §影响范围 "保存 sprite report 或 mapping report" 路径不固定

PLAN §Asset Pipeline Deliverables 已经定 `assets-src/image/manifests/dark-v1-sprite-report.jsonl`。PR-02 §影响范围 应直接写这条 path，不要用"或"。

### 4.3 PR-03 §1.4 "Round 7 分批生成" 措辞含混

"分批"指 Round 7 三张 sheet 在 PR-03 内分批 commit？还是分到不同 PR？README §SheetId Ownership 表已确定三张全归 PR-03。PR-03 §1.4 应改成 "PR-03 内按 sheet 顺序分批 commit Round 7 的 3 张 sheet"。

### 4.4 PR-03 §影响范围 含 `EquipmentSlotLabels.kt` 但 §6 必测行为没断言 slot label↔key 映射

PR-03 改 EquipmentSlotLabels 是为了让新 item icon 接入装备 slot；但 §6 必测行为 4 条都不覆盖 "slot label 字符串到 manifest key 的映射"。回归脆弱。

**建议**：§6 加一条 "EquipmentSlotLabels 的 slot label↔icon key 映射必须有专项测试或在 EquipmentInventoryPresenterTest 内覆盖"。

### 4.5 PR-04 §1.3 "三棵职业树" 与 manifest 12 个 tree key 内部对账

- §1.3 "三棵职业树……指当前已选职业的三棵树，不代表全仓总树数"——好澄清 ✅
- 但当前 manifest `tree.*` = 12，`icon.tree.*` = 12。如果每职业 3 棵，6 playable × 3 = 18 ≠ 12。
- 实际上更可能是每职业 2 棵（base + elite 路径），6 × 2 = 12 ✅。
- 矛盾：PR-04 prose 说"三棵"，但数据是"两棵"——**至少其一错**。

**修复**：PR-04 §1.3 用具体数字代替 "三棵"，或注明"三棵是示例，实际以 client/game 当时 dialect 为准"。建议同时核对 game 数据（`game/src/main/resources/data/professions/index.yaml` 或对应 `talents/*.yaml`）确认每职业 N 棵树，写进 §硬依赖合同。

### 4.6 PR-04 §人工白盒 第 1 条硬编码具体数字 "3 个 learned starter、1 个空 active slot"

starter 数与 active slot 状态来自 `phase4-v4-pr01` scenario 当时设置；scenario 调整时本 PR 文档失同步。

**修复**：改成"以 `phase4-v4-pr01` scenario 当时实际状态为准；本 PR 不固化具体数字。"

### 4.7 PR-04 §影响范围 缺少 `ui.frame.modal.body` 等消费方注解

§5 第 6 条 "Active slot choice modal：新暗黑 modal chrome"——隐含消费 PR-02 的 `ui.frame.modal.body` / `ui.frame.tooltip.body` / `ui.frame.slot.*`。如果 PR-02 还没交付这些 key，PR-04 跑不通。

**修复**：§硬依赖合同 加一条 "PR-04 假设 PR-02 已交付 `ui.frame.modal.body / tooltip.body / slot.empty / slot.equipped / slot.selected`；如未交付，需先 ping PR-02 owner。"

### 4.8 PR-04 §7 验证含 `contractLint` 但 §影响范围 不列 token / contract 文件

跑 `contractLint` 的判定来自 README §依赖规则 6（"新增或改中文 UI 文案、locale token、presentation token 的 PR 必须补跑 localeLint contractLint"）。PR-04 §影响范围只列 .kt 文件，没有 token / locale / contract yaml。如不改 contract，跑 contractLint 是冗余；如要改，应在影响范围列出。

**修复**：要么 §7 删 `contractLint`、`localeLint`，要么 §影响范围补 token/locale 文件。

### 4.9 PR-05 §3 资源范围 第 5 条 r06-portraits-zones 缺 "secret zone"

PLAN.md §Sheet Inventory：`r06-portraits-zones | … | zone、secret zone portrait`。PR-05 §3.5 写 "Round 6：r06-portraits-classes、r06-portraits-trees、r06-portraits-zones"——没明确包含 secret zone。

**修复**：写 "r06-portraits-zones（含 zone 与 secret zone portrait）"。

### 4.10 PR-05 §回滚边界 缺 Round 4 失败处理

§9.2 只说 "Round 5 bestiary 失败不回滚 Round 2"。Round 4 actor 是 sheet 数最多的环节（4 张 sheet），失败如何处理没写。

**修复**：§9 加一条 "Round 4 actor 失败仅回滚对应 sheetId、actor manifest entry 与 contact sheet QA；不影响 Round 2 tile 与 Round 3 prop。"

### 4.11 PR-05 §6 必测行为 缺 "actor 内部 Y-sort"

§6.1 列 "ground / wall / decal / actor / VFX / boss telegraph 6 层"——但 actor 层内部多个 actor 互相遮挡（actor-A 在前、actor-B 在后）的 Y-sort 测试没写。`TileLayerComposerTest` 名义上覆盖"层级"，但同层内 sort 是不同概念。

**修复**：§6 加一条 "同 actor 层内 actor 按 Y 坐标从小到大渲染，保证视觉前后关系正确。"

### 4.12 PR-06 §6 验收标准与 PR-07 §3 第 1 条互相干扰

- PR-06 §6 coverage artifact 字段表："`pendingOrRejectedPlayerVisibleCells: 必须为空`"
- PR-07 §3 第 1 条："只允许修复 PR-06 遗留 `rejected` cell"

如果 PR-06 必须 rejected 玩家可见 cell = 0，PR-07 还能修什么？两条之一是错。

**修复**：PR-06 §6 字段表细化——`pendingOrRejectedPlayerVisibleCells` **玩家可见主路径**必须为空；非主路径（debug / fallback / hidden）允许进入 PR-07。PR-07 §3 同步把"修复"对象限定为非主路径或主路径之 cosmetic 返修。

### 4.13 PR-06 §3 "返修 Round 7 / Round 2-6" 与 README §SheetId Ownership 之 owner 关系

README ownership 把 r02-r07 sheet 全划给 PR-03/PR-05。PR-06 要返修这些 sheet 的 cell，需要更新 sheet-plan 中对应 cell——此时 sheet ownership 是否易主？

**修复**：PR-06 §3 加一条 "返修不改 sheet ownership；返修信息以 `qaStatus / rejectionReason` 写入 `dark-v1-sprite-report.jsonl`，不重写 sheet-plan 输入字段。"

### 4.14 PR-07 §影响范围 表用宽泛措辞

`client golden baseline` / `client render tests` / `client` 单字。其他 PR 都给具体路径。PR-07 是收口阶段，路径应该最具体而不是最模糊。

### 4.15 PR-07 §3 第 4 条与 PR-00 §2 影响范围措辞冲突

- PR-00 §2 "`UI/PLAN.md`：只回写上游合同变化，不复制 PR 执行细节"
- PR-07 §3.4 "补一次 `UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/*` 与实际实现的对照清单，输出到 `UI/review/dark-uiux-final-doc-implementation-audit.md`"

PR-07 是"对照清单"还是"修订 PLAN"？措辞含混。

**修复**：PR-07 §3.4 明确 "只产出 audit 报告，不修订 PLAN/ART_STYLE_BIBLE；如发现实际实现与 PLAN 矛盾，下个 PR 才修订上游合同。"

### 4.16 PR-07 §6 Packaged App 白盒 命令链不完整

只列 `:client:packageMacApp`（build）。但 §6 证据要求"运行时 home / app bundle 路径 / 启动日志路径 / 截图 evidence dir"——build 单步无法产生这些。

**修复**：补"运行命令、截图命令、关闭命令"完整链，例如：

```bash
./gradlew :client:packageMacApp
open build/distributions/K-ToME.app
# wait for window
screencapture -o build/evidence/dark-uiux-pr07-packaged-launch.png
```

### 4.17 PR-04 §7 验证下"白盒准备"小节归属不明

```bash
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=phase4-v4-pr01
```

放在 §7（验证）下方但又在 §人工白盒上方。属于自动还是人工？建议明确归到 §人工白盒。

---

## 5. 资源 inventory / capacity 对账

### 5.1 sheet 总容量 vs inventory 总数差距巨大

- inventory 总数 = 538
- 27 sheet 总容量 = 1360 cell（计算：r01 144 + r02 192 + r03 96 + r04 160 + r05 192 + r06 48 + r07 192 + r08 192 + r09 144）
- 利用率 = 538 / 1360 ≈ 39.6%
- reserved 比例 ~60%。

PR 文档没解释为什么 reserve 这么多。可能原因：alias、polish、未来扩展。但当前文档表述不清，让人怀疑是不是规划过头。

**修复**：PLAN §Resource Inventory 旁加一张"capacity vs inventory"对账表，或在 §Sheet Plan 段说明"每 sheet 利用率约 40%，预留作 alias、polish round 与后续职业扩展"。

### 5.2 inventory 子分类不可追溯

- `icon = 223` 一项混合了 `icon.tree.*`、`icon.profession.*`、`icon.affix.*`、`icon.material.*`、`icon.monster.*`、`icon.boss.*`、`icon.quest.*` 等多种前缀。
- 当 PR-05/PR-06 各自负责一部分 icon 时，没办法在 §Resource Inventory 里追溯子集数。
- PR-06 §6 coverage artifact 要求 `expectedKeyCountByCategory`——但子分类数据来源没明确（来自 PLAN inventory 还是从 manifest 实时数）。

**修复**：PR-00 §3 增加任务 "把 PLAN.md §Resource Inventory 的 icon 子集拆到具体前缀（icon.skill / icon.tree / icon.profession / icon.monster / icon.boss / icon.affix / icon.material / icon.quest / icon.damage_type 等），输出到 `UI/sprite-sheets/key-registry.yaml` 或派生 `key-registry.json`。"

### 5.3 `dark-v1-manifest-coverage.json` 不在 PLAN §Asset Pipeline Deliverables 表中

PR-02/03/05/06/07 全文引用 `dark-v1-manifest-coverage.json`，但 PLAN §Asset Pipeline Deliverables 表只列 `dark-v1-sprite-report.jsonl`，没列 coverage 这一份。

**修复**：PLAN §Asset Pipeline Deliverables 加一行：

| Path | Purpose |
| --- | --- |
| `assets-src/image/manifests/dark-v1-manifest-coverage.json` | dark-v1 player-visible coverage artifact，PR-02 起每个资源 PR 输出 |

---

## 6. 验证命令 / coverage mode 调用细节

### 6.1 各 PR 的 `darkManifestCoverageLint` 没说怎么传 owner-scope 参数

README §Common Test Plan "PR-02、PR-03、PR-05 使用 owner-scope" + PR-02/03/05 §7 验证只写 `darkManifestCoverageLint`——没有 `-PdarkCoverageOwner=PR-02` 之类参数。

**修复**：PR-00 §5 固化 task 的参数协议，例如 `./gradlew darkManifestCoverageLint -PdarkCoverageMode=owner-scope -PdarkCoverageOwner=PR-02`。各 PR §7 验证的命令必须用具体参数，否则同一 task 三 PR 跑相同口径，owner-scope 失效。

### 6.2 PR-02/03 §影响范围 没强调 "runtime manifest 必须由 syncPhase2Manifests 同步"

README §Manifest Authority 第 2 条已经说了，但 PR-02/03 §影响范围 把 canonical 与 runtime 都列出来，开发者容易忘记跑 sync 直接手改 runtime → manifestLint 报错。

**修复**：PR-02 / PR-03 §6 验收标准 加一条 "runtime manifest 必须 100% 由 `./gradlew syncPhase2Manifests` 生成；不允许手改 runtime manifest 后 commit。"

### 6.3 README §Evidence Matrix 与 PR 文档 §人工白盒 证据 label 不一致

| PR | README label | PR 文档 |
| --- | --- | --- |
| PR-04 | `dark-uiux-pr04-talent-sidebar-start`、`dark-uiux-pr04-active-slot-choice`、`phase4-v4-pr01` scenario evidence | 一致 ✅ |
| PR-05 | "fixed seed map/actor/telegraph/ground-loot layer screenshot and contact sheet QA"——没具体 label | PR-05 §人工白盒 4 "必填证据：固定 seed、map/actor/telegraph/ground-loot 层级截图、每批 contact sheet QA、manifest diff" |
| PR-06 | "skill/status/quest/profession tree same-screen screenshot and manifest coverage artifact" | PR-06 §人工白盒 4 加了 "fallback injection record" |

PR-05 / PR-06 在 README 没有具体 label。建议 PR-05 给 `dark-uiux-pr05-map-layer-stack`、`dark-uiux-pr05-boss-telegraph` 这种具体名；PR-06 给 `dark-uiux-pr06-status-quest-skill-overview`、`dark-uiux-pr06-fallback-injection`。

### 6.4 PR-06 §6 与 README §Manifest Authority 的 coverage artifact 字段表语义微差

- README §Manifest Authority："coverage artifact 必须包含 `scopeMode / ownerPr / expectedKeySetSource / strictOldStyleResidue`"
- PR-06 §6："`styleTag / expectedKeyCountByCategory / coveredKeyCountByCategory / oldStylePlayerVisibleKeys / allowedFallbackKeys / pendingOrRejectedPlayerVisibleCells / sourceSheetIds / allowedCoverageExclusions`"

两套字段没说是合并还是 PR-06 final-full 模式新增字段。建议 PR-06 §6 表标 "本字段表为 final-full 模式专属，在 README §Manifest Authority 通用字段之上扩展"。

---

## 7. 命名 / 测试 / 边界细节

### 7.1 README §Manual Codex App Raw Sheet Workflow 第 7 条命令缺失

> "开发者运行切分、contact sheet、QA 和 manifest/coverage 校验脚本。"

具体什么命令？没写。开发者拿到这个文档不知道运行什么。

**修复**：列具体命令，如：

```bash
python3 scripts/slice_spritesheet.py --sheet-plan UI/sprite-sheets/sheet-plan.yaml --sheet r01-ui-chrome
python3 scripts/render_contact_sheet.py --sheet r01-ui-chrome
python3 scripts/verify_sprite_sheet_map.py --sheet r01-ui-chrome
./gradlew syncPhase2Manifests darkManifestCoverageLint
```

### 7.2 PR-00 §3 第 9 条 key-registry 派生路径的脚本名缺

§3 第 9 条 "或固定生成 `build/reports/dark-v1/key-registry.json` 的命令和输入"——没说脚本名。其他 4 个脚本（generate_sheet_prompt / slice_spritesheet / render_contact_sheet / verify_sprite_sheet_map）都给了名。

**修复**：补一个 `scripts/generate_key_registry.py` 或在 PR-00 §3 表里加一行。

### 7.3 PR-00 §3 第 8 条 dry-run "alias cell" 用法没说明

"1 个 alias cell"——alias 的目标 key 是哪个？如果 alias 复用同一图，dry-run 是否在 fixture manifest 输出两个 entry？没说。实施时 alias 处理逻辑没用例。

**修复**：PR-00 §3 第 8 条加一行 "alias cell 必须示例两种情况：(a) 同 sheet 内 alias、(b) 跨 sheet alias；fixture manifest 必须显示 alias 如何映射到 raw path。"

### 7.4 PR-02 r01-ui-chrome 16 cell 不饱和但 PR-02 §3 没说 reserved

`r01-ui-chrome` cap = 16，PR-02 §4 只列 9 个 chrome key（panel.body + 4 corner + 4 edge + slot.empty/equipped/selected + tooltip + modal = 12，少 4 cell）。剩余 4 cell 是 reserved 还是漏列？

**修复**：PR-02 §3 加一句 "r01-ui-chrome 余 4 cell 标记 reserved，作 PR-06 polish 用。" 或补全到 16 cell。

### 7.5 boss `actor` vs boss `icon` 视觉一致性约束缺

README §SheetId Ownership：
- `r04-actors-boss` (Owner PR-05, Category `actor_sprite`, large-sheet 16)
- `r05-boss-icons` (Owner PR-05, Category `icon`, icon-sheet 64)

同一 boss 在两套 sheet 中各有一份（actor 用于地图战斗 sprite，icon 用于 bestiary/UI 小图）。两份必须风格一致——但分到不同 sheet、不同 grid，不能机械保证。

**修复**：PR-05 §6 加一条 "同一 boss 的 r04-actors-boss 与 r05-boss-icons 必须在 contact sheet QA 阶段同框对比，确认 silhouette / 配色一致。"

### 7.6 HUD `ui.hud.key.icon` 与 `icon.quest.*_key` 风格一致性约束缺

HUD 数字图标 (`ui.hud.key.icon`，r01-ui-hud-icons, PR-02) 与背包/任务任务钥匙 marker (`icon.quest.armory_key/seal_key`，r09-quest-zone-profession, PR-06) 是同一玩家概念在两个 namespace。两个图必须风格一致。

**修复**：PR-02 §4 表注 "`ui.hud.key.icon` 必须与 PR-06 的 `icon.quest.*_key` 视觉一致；PR-06 收尾时同框 QA"。

### 7.7 ART_STYLE_BIBLE §1 第 4 条 "新 tag 是父级风格的暗黑 UI/资源子纪元" 与父级合同没机制保证

如果父级 `docs/2026-03-13-art-style-bible.md` 升级（painterly-tile-v2），dark-v1 自动失效吗？还是 dark-v1 跨父级版本？

**修复**：ART_STYLE_BIBLE §1 加一条 "父级合同 bump 到 v2 时，本文件必须重新 audit；不自动跨版本生效。"

### 7.8 PR-03 §6 fallback "不允许直接使用旧风格 missing_visual 作为玩家主路径"

`missing_visual` 是 manifest 顶层 fallbackKey。PR-03 §4.3 说不能用作玩家主路径——但当 item key 缺失时，runtime 必然走顶层 fallback。这意味着 PR-03 close 前每个 item key 都必须显式有 dark-v1 fallback——成本不小，文档没说怎么实现。

**修复**：PR-03 §4 加一条 "fallback 路径优先级：item-specific dark-v1 fallback > category-level dark-v1 fallback > 顶层 `missing_visual`。前两级在 PR-03 实现；顶层只在 fallback injection record 测试中触达。"

---

## 8. 风险 / 维护

### 8.1 三个 lint task 命名前缀不统一

- `darkSpriteSheetLint`
- `spriteSheetMapLint`
- `darkManifestCoverageLint`

第二个缺 `dark` 前缀。如果未来仓库引入非 dark 的 sprite sheet（unlikely 但 schema 允许），命名冲突。

**修复**：统一前缀为 `darkSpriteSheetLint / darkSpriteSheetMapLint / darkManifestCoverageLint`，或一律不加前缀 `spriteSheetLint / spriteSheetMapLint / manifestCoverageLint`。

### 8.2 PR-00 §3 task 11（"raw PNG 缺失时输出 missingRawSheet"）测试覆盖未要求

PR-00 §9 验证只跑 `python3 -m pytest scripts/tests/test_dark_sprite_sheet_pipeline.py`，但没列必测的 case：
- raw PNG 缺失 → missingRawSheet
- raw PNG 尺寸错 → wrongCanvas
- raw PNG 文件名错 → wrongFileName
- targetKey 重复 → duplicateTargetKey
- alias 目标缺失 → orphanAlias

**修复**：PR-00 §3 列出 pytest 必测 case 清单。

### 8.3 sheet-plan 字段允许 `aliasOf` 但 alias 是否同 sheet / 跨 sheet 没规则

PLAN §Mapping Spec 第 3 条 "确实复用同一图时必须显式写 aliasOf"——但没说 alias 是否限制在同 sheet 内。
- 如果跨 sheet alias，sheet capacity 计算需要扣除 alias cell（但 alias 不切图，又不算 capacity 占用）。
- 如果同 sheet alias，则 row/col 必须指向另一个 cell，存储复杂度增加。

**修复**：PLAN.md 增加 "alias 默认同 sheet；跨 sheet alias 必须在 key-registry 显式声明 sourceSheetId 与 sourceTargetKey。"

### 8.4 PR-04 §7 验证含 `phase4-v4-pr01 scenario evidence` 但 README §Evidence Matrix 已重复

双重维护风险。可由 README 引用。

### 8.5 PR-07 packaged app 验收 macOS 单平台

`:client:packageMacApp` 只覆盖 macOS。Linux/Windows 玩家是否在 PR-07 范围？没说。如果不在，需明确"v1 阶段 packaged app 验收只在 macOS"。

### 8.6 `docs/opt/ui-redesign/demo/` demo 路径迁移仍无 owner

PLAN §Summary 提 "如果后续迁入 `docs/opt/ui-redesign/demo/` …"——上一轮 review §5.3 指出过，仍没有任何 PR 接走。**建议**：PR-07 §3 加 "判断是否迁移 demo 路径；不迁则在 audit 报告记录决策"。

---

## 9. 可执行性 / 信号噪声

### 9.1 PLAN.md §Codex CLI 验证结论 措辞 "Codex 交互式生成 raw sheet/demo"

但 README §Manual Codex App Raw Sheet Workflow 用的是 "Codex app"。PLAN 用 "Codex 交互式" 模糊（CLI vs app）。**修复**：PLAN 改 "Codex app 交互式生成 raw sheet"。

### 9.2 PR-00 §影响范围 表 "tools 或 scripts" 路径写法

应该是 "`tools/` 或 `scripts/`" 加反引号，或选其一明确目录。

### 9.3 README §SheetId Ownership 表 r02-tiles-decal Scope 含 `terrain.*`

但 PLAN.md §Sheet Inventory 行只写 "`tile_decal` 与 biome decal 变体" 之类。`terrain.*` key 是否真的存在于 manifest？需要核实，否则 PR-05 实施时 r02-tiles-decal 多了一个找不到对应 entry 的 prefix。

### 9.4 PR-06 §6 表 `sourceSheetIds` 字段语义未说

- 是本 PR 涉及的所有 sheet ID？
- 还是仅 PR-06 owner（r08/r09）？
- 还是包含 PR-03/PR-05 的 sheet 中被 PR-06 返修的 cell？

不明确会让 coverage artifact 无法跨 PR 累计。

**修复**：PR-06 §6 表注 "`sourceSheetIds` 包含本 PR 切换或返修涉及的全部 sheetId，跨 owner 时必须列出"。

### 9.5 PR-06 §3.6 "r09-fallback-polish 必须覆盖 missing_visual、hidden/debug/fallback 主路径和 rejected cell"

`r09-fallback-polish` 是 large-sheet (16 cell)。要覆盖：
- `missing_visual` × 1
- `hidden_visual` × 若干
- `debug.*` 系列（manifest 显示 14 entry）
- 所有 PR-02/03/05 rejected cell

加起来轻松超过 16 cell。**修复**：PR-06 §3.6 给 cell 估算，或允许 r09-fallback-polish 拆成多张 sub-sheet（如 r09-fallback-polish-a / -b）。

### 9.6 ART_STYLE_BIBLE §6 切分 runtime canvas

| Type | runtime canvas |
| --- | --- |
| tile_*/prop_*/actor_sprite | `256x256` |
| icon* | `160x160` |
| portrait | `256x256` |

但 `ui_frame` 与 `vfx_plate` 没列。`scripts/process_assets.py:55-75` 实际为这两类 category 配置 canvas（`ui_frame=256` 估计）。**修复**：ART_STYLE_BIBLE §6 表补两行。

### 9.7 ART_STYLE_BIBLE §3 "talent-locked / learnable / reserve / active" 颜色与 PR-04 输入语义对齐情况

PR-04 §硬依赖合同 第 1 条 `TalentTreeNodeSnapshot.category` 是 typed enum。但 §1.3 列出的四态 `LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE`——具体 enum 名字是 `category` 字段还是 `state` 字段？文档与 enum 名漂移可能导致 renderer 用错字段。

**修复**：PR-04 §硬依赖合同 加注 "四态 enum 字段名以 `TalentTreeNodeSnapshot` 实际定义为准；本 PR 不重命名"。

---

## 10. 优先级建议

### P0（动 PR-02 之前必修）

| # | 引用 | 内容 |
| --- | --- | --- |
| 1 | §2 N1 | PR-02 §4 UI Key Registry 表与 §3.3 prose 不闭环；至少补 `ui.hud.warning/quest_marker/log_marker.icon` |
| 2 | §1.2 | PR-00 §3 增加 "审计 manifest-lint.py prefixRules 是否接受 ui.* 前缀" 任务 |
| 3 | §3.1 | r07-items-affix-material 的 quality frame / slot state 划归（归 r07 还是 r01-ui-chrome） |
| 4 | §3.5 / §3.6 | PLAN §UI Key Additions 表补全；§Mapping Spec 示例 sheet 改正 |

### P1（PR-00 / PR-02 期间补文档）

| # | 引用 | 内容 |
| --- | --- | --- |
| 5 | §1.1 | PLAN/ART_STYLE_BIBLE 加 "顶层 styleTag 与实际渲染纪元解耦" 说明 |
| 6 | §3.2 | r03-props-environment vs r06-portraits-zones 分流 zone.*.visual |
| 7 | §3.3 | 三个 dark lint 的 input/output 正交矩阵 |
| 8 | §3.4 | 三处 coverage 模式表合并到 PLAN 单点维护 |
| 9 | §3.7 | prompt 编号生成稳定性（不因新增 sheet 漂移） |
| 10 | §3.8 | PR-04 §硬依赖合同 引用 README §职业树专用规则 |
| 11 | §4.5 | PR-04 §1.3 "三棵职业树" 与 12 个 tree key 对账 |
| 12 | §4.7 | PR-04 §硬依赖合同 列 PR-02 chrome key 依赖 |
| 13 | §4.12 | PR-06 §6 与 PR-07 §3.1 的 rejected cell 玩家可见 / 非主路径区分 |
| 14 | §5.2 | PR-00 §3 增加 inventory 子分类拆分任务 |
| 15 | §5.3 | PLAN §Asset Pipeline Deliverables 加 `dark-v1-manifest-coverage.json` |
| 16 | §6.1 | 各 PR §7 验证命令补具体 owner-scope 参数 |
| 17 | §6.2 | PR-02/03 §6 加 "runtime manifest 必须 sync 生成" |
| 18 | §7.6 | HUD `ui.hud.key.icon` 与 PR-06 `icon.quest.*_key` 风格一致约束 |

### P2（实施期顺手补）

| # | 引用 | 内容 |
| --- | --- | --- |
| 19 | §4.1 | PR-01 影响范围测试用具体文件名 |
| 20 | §4.2 | PR-02 影响范围 sprite report 路径写死 |
| 21 | §4.3 | PR-03 §1.4 "分批" 措辞收紧 |
| 22 | §4.4 | PR-03 加 slot label↔key 映射测试 |
| 23 | §4.6 | PR-04 §人工白盒数字软化 |
| 24 | §4.8 | PR-04 §7 contractLint 与影响范围 token 对齐 |
| 25 | §4.9 | PR-05 §3 r06-portraits-zones 加 secret zone |
| 26 | §4.10 | PR-05 §回滚边界补 Round 4 |
| 27 | §4.11 | PR-05 §6 加 actor Y-sort |
| 28 | §4.13 | PR-06 §3 返修不易主 ownership |
| 29 | §4.14 | PR-07 影响范围具体路径 |
| 30 | §4.15 | PR-07 §3.4 audit 不修订 PLAN |
| 31 | §4.16 | PR-07 §6 packaged app 命令链补全 |
| 32 | §4.17 | PR-04 §人工白盒命令归类 |
| 33 | §5.1 | PLAN 加 capacity vs inventory 对账 |
| 34 | §6.3 | README §Evidence Matrix 给 PR-05 / PR-06 具体 label |
| 35 | §6.4 | PR-06 §6 字段表标注 final-full 扩展 |
| 36 | §7.1 | README §workflow 第 7 条补具体命令 |
| 37 | §7.2 | PR-00 generate_key_registry 脚本名 |
| 38 | §7.3 | dry-run alias cell 用例 |
| 39 | §7.4 | r01-ui-chrome 16 cell reserved 标注 |
| 40 | §7.5 | boss actor / icon 同框 QA 约束 |
| 41 | §7.7 | ART_STYLE_BIBLE 父级 v2 升级机制 |
| 42 | §7.8 | PR-03 fallback 优先级 |
| 43 | §8.1 | dark lint task 命名前缀统一 |
| 44 | §8.2 | PR-00 pytest 必测 case 清单 |
| 45 | §8.3 | sheet-plan alias 同/跨 sheet 规则 |
| 46 | §8.5 | PR-07 packaged app macOS 单平台说明 |
| 47 | §8.6 | demo 路径迁移 owner 落到 PR-07 |

---

## 11. 评审小结

第一轮 review 已经促使文档从"骨架对、隐患多"升级到"骨架对、绝大部分硬合同对齐"。本轮发现的 47 项几乎全是细节漂移、表格不闭环、措辞含糊。

唯一一个新硬阻塞 **N1（PR-02 §4 表与 §3.3 prose 不一致）** 是文档体系内最容易被开发者直接踩到的坑——PR-02 关闭时 ownership 闭环失败，PR-05 telegraph warning 接 key 时找不到 owner。建议**优先修这一项**，再开始 PR-00 实施。

P0 四项都集中在 PR-00 ↔ PR-02 ↔ PR-06 表格之间的内部不一致，本身不需要新增设计、只需文字校对，预计 1 个 dry-run 周期内即可同步修完。

P1 / P2 可以在 PR-00、PR-02 之间穿插补，**不应阻塞 PR-00 进入实施**。

# 暗黑 UI/UX PR 文档深度 Review 报告（第三轮）

> 评审日期：2026-04-28
> 评审范围：`UI/PLAN.md` + `UI/ART_STYLE_BIBLE.md` + `UI/pr/README.md` + `UI/pr/dark-uiux-pr00…pr07-*.md`
> 上游报告：[第一轮 2026-04-27](./2026-04-27-dark-uiux-pr-deep-review.md)、[第二轮 2026-04-27 round2](./2026-04-27-dark-uiux-pr-deep-review-round2.md)
> 本轮定位：Round2 列出 1 个新硬阻塞 + 47 项细节，作者已落了一波修订。本轮只做两件事——**(a) 验证 round2 列表的修复情况，(b) 在新一版文档上挑出仍漂移、仍互相打架、仍可被开发者一脚踩塌的细节**。
> 视角：合同自洽、文档间引用一致、字段闭环、可执行性、可审计性。

---

## 0. TL;DR

Round2 的 47 项问题修复进度：

| 状态 | 数量 | 备注 |
| --- | ---: | --- |
| 已修 ✅ | 12 | 主要是「各 PR `darkManifestCoverageLint` 命令补 owner-scope 参数」、「PLAN §Mapping Spec 示例 sheetId 改正」、「PR-04 §1.3『三棵职业树』澄清」、「PR-06 final-full schema 引用」、「PR-07 packaged app 命令链」等机械可校对的项 |
| 部分修 ⚠️ | 6 | 譬如 PLAN §UI Key Additions 已扩到 15 条但仍缺 `ui.combat.* / ui.state.*`；PR-06 §6 区分玩家可见 rejected 但 PR-07 §3.1 仍没同步 |
| **未修 ❌** | **29** | 包括 round2 P0-#1（N1: PR-02 §4 仍缺 `warning/quest_marker/log_marker.icon`）、prefixRules 审计、`zone.*.visual` 双 owner、三 lint 输入正交矩阵、coverage artifact 路径未注册、命名前缀不统一等 |

本轮**新增 1 个硬阻塞 + 17 项新发现**。最致命的是：

| 等级 | # | 问题 | 影响 |
| --- | --- | --- | --- |
| **P0** | **R3-N2** | PLAN.md line 121 写 "约 28 张核心 sheet"，但 §Sheet Inventory 表实际 29 行（README §SheetId Ownership 也是 29 行），文字 ≠ 表格 | 任何下游 lint / coverage 用「sheet 总数」做断言时会和文档对不上；评审者读总览段会被误导 |
| P0 | R2-N1（仍未修） | PR-02 §4 UI Key Registry 表只列 5 个 HUD icon，§3.3 prose 列 10 类内容；下游 PR-05 telegraph 需 warning icon、PR-06 quest_marker / log_marker 也无 sheet owner | round2 已点名仍未补；PR-02 关闭即破 owner-scope coverage |

总条目（含未修 + 新发现）= **46 项**，已分级为 P0(3) / P1(14) / P2(29)。详见 §10。

---

## 1. Round2 问题修复对账

完整 47 项遍历，按 round2 编号：

### 1.1 已修复 ✅（12 项）

| Round2 # | 检验依据 |
| --- | --- |
| #11 PR-04 §1.3 "三棵职业树" 注解 | PR-04:17 "三棵职业树指当前已选职业的三棵树，不代表全仓总树数" ✅ |
| #16 各 PR §7 验证命令补 owner-scope 参数 | PR-02:94、PR-03:81、PR-05:91、PR-06:116、PR-07:104 全部已写 `-Pktome.darkUiux.coverageMode=...` 并附 `-Pktome.darkUiux.ownerPr=PR-xx` ✅ |
| #19 PR-01 测试用具体文件名 | PR-01:24-26 已展开到 GameShellLayoutTest / TileRendererCanvasTest / AsciiRenderModelTest ✅ |
| #31 PR-07 §6 packaged app 命令链 | PR-07:64-79 已加 `:client:packageMacApp preparePhase4V4Whitebox` 完整 runbook + launch script ✅ |
| #35 PR-06 §6 字段表标注 final-full | PR-06:77 "PR-06 使用 PR-00 固定的 final-full schema" ✅ |
| #37 PR-00 generate_key_registry 脚本名 | PR-00:51 已补 `verify_dark_key_registry.py` ✅ |
| #4（部分） PLAN §Mapping Spec 示例 sheetId | PLAN:145 "sheetId: r01-ui-controls" 配 `ui.combat.*` ✅ |
| #34（部分） README §Evidence Matrix 给具体 label | PR-02 / PR-03 / PR-04 已写具体 label；PR-05 / PR-06 仍宽泛（见 §1.3） |
| #15 PLAN coverage artifact 默认路径在 PR-00 §6 提到 | PR-00:182 "PR-06 通过 `dark-v1-manifest-coverage.json` 证明" ✅（路径出现）|
| #20（部分） PR-02 §影响范围 sprite report 路径 | PR-02:23 "保存 sprite report 或 mapping report" — 仍是 "或" 措辞，但 PLAN §Asset Pipeline Deliverables 已固定路径，作者可能视为已收口 |
| 第一轮 H1 `EXPECTED_STYLE_TAG` 升级路径 | PR-00 §6、PLAN §Pipeline Gate Strategy 有 multi-epoch sidecar 段，已有 ✅ |
| 第一轮 H3 8 职业覆盖 | PLAN §Profession Coverage Scope、README §Manifest Entry Targets、ART_STYLE_BIBLE §7.3 三处已对齐 ✅ |

### 1.2 部分修复 ⚠️（6 项）

| Round2 # | 当前状态 | 仍缺 |
| --- | --- | --- |
| #4 PLAN §UI Key Additions | 现 15 条（chrome 9 + hud 5 + control 2） | 仍缺 `ui.combat.*` / `ui.state.*` 前缀；与 PLAN §Mapping Spec 示例（已用 `ui.combat.*`）自相矛盾 |
| #12 PR-04 §硬依赖合同 列 PR-02 chrome key 依赖 | §硬依赖合同 5 条仍只覆盖 talent presenter / 不改 progression / 复用现有图标 | 仍未列 `ui.frame.modal.body` / `ui.frame.tooltip.body` / `ui.frame.slot.*` 这条 PR-02 → PR-04 chrome 依赖 |
| #13 PR-06 §6 vs PR-07 §3.1 rejected cell 互斥 | PR-06:70 "contact sheet QA report 没有 `pending` 或 `rejected` 的玩家可见资源" 已加「玩家可见」限定 | PR-07:32 "只允许修复 PR-06 coverage artifact 已记录的 `rejected` cell"——没说是哪种 rejected。PR-06 close 后玩家可见 rejected = 0，PR-07 修的就只能是非玩家可见，措辞仍含混 |
| #34 README §Evidence Matrix PR-05 / PR-06 label | 仍是 "fixed seed map/actor/telegraph/ground-loot layer screenshot and contact sheet QA"（一长串描述），PR-06 同 | round2 建议给 `dark-uiux-pr05-map-layer-stack` / `dark-uiux-pr06-status-quest-skill-overview` 这种 label，未落地 |
| #4（部分） PR-02 §4 UI Key Registry | round2 后未变 | **见 §2 R2-N1，仍是 P0** |
| 第一轮 H2 sheet-plan lint | task 已存在 | 三 task input 边界仍未画正交矩阵（round2 #7） |

### 1.3 未修复 ❌（29 项）

按 round2 编号略列：#1（仍未修，本轮单独标记 R2-N1）、#2 prefixRules 审计、#3 r07-items-affix-material（README 表已改但 PR-03 §3.6 仍需对齐）、#5 顶层 styleTag 与渲染纪元解耦说明、#6 zone.*.visual 双 owner、#7 三 lint input 矩阵、#8 三处 coverage 模式表合并、#9 prompt 编号稳定性、#10 PR-04 引用 README 职业树规则、#14 inventory 子分类拆分、#17 PR-02/03 §6 加 sync 提醒、#18 ui.hud.key.icon 风格一致约束、#21 PR-03 §1.4 措辞、#22 PR-03 slot label 测试、#23 PR-04 starter 数硬编码、#24 PR-04 contractLint 与 token 对齐、#25 PR-05 secret zone 显式、#26 PR-05 Round 4 回滚、#27 PR-05 actor Y-sort、#28 PR-06 返修不易主、#29 PR-07 影响范围具体路径、#30 PR-07 §3.4 不修订 PLAN、#32 PR-04 白盒命令归类、#33 capacity vs inventory 对账、#36 README workflow 第 7 条具体命令、#38 alias cell 用例、#39 r01-ui-chrome 16 cell reserved 标注、#40 boss actor / icon 同框 QA、#41 ART_STYLE_BIBLE 父级 v2、#42 PR-03 fallback 优先级、#43 lint task 命名前缀、#44 pytest 必测 case、#45 alias 同/跨 sheet 规则、#46 packaged app macOS 单平台、#47 demo 路径迁移 owner。

---

## 2. 本轮新发现的硬阻塞

### R3-N2. PLAN.md 文字 "约 28 张核心 sheet" 与表格实际 29 行不一致

PLAN.md:121

```
总共生成 **9 轮雪碧图**，约 **28 张核心 sheet**。每轮都产出：raw sheet、sheet plan、切分 PNG、contact sheet、manifest patch、QA report。
```

但 PLAN.md §Sheet Inventory 表（lines 277–306）和 README.md §SheetId Ownership 表（lines 37–65）实际 sheet 数：

```
r01: 3 (chrome, controls, hud-icons)
r02: 3 (ground, wall, decal)
r03: 3 (props-interactable, props-environment, vfx-telegraph)
r04: 4 (player, humanoid, monster, boss)
r05: 3 (bestiary-humanoid, bestiary-creature, boss-icons)
r06: 3 (classes, trees, zones)
r07: 3 (base, unique-artifact, affix-material)
r08: 3 (vanguard-berserker, templar-rogue, arcanist-spellblade)
r09: 4 (status-damage, quest-zone-profession, fallback-debug, rejected-polish)
合计 = 29
```

机械验证：

```bash
$ grep -cE "^\| \`r0" UI/pr/README.md
29
$ grep -cE "^\| [0-9] \| \`r0" UI/PLAN.md
29
```

**修复**：PLAN.md:121 改成 "**29 张核心 sheet**"；同时 round2 §5.1 的 "27 sheet" / "1360 cell" 容量计算也要重算（按 29 sheet 实际是 r01 3×64+0×16=...，需要对齐当前 sheet type）。Round1 / round2 报告对外的"27 张"、"约 28 张"也是从这个文字段抄来的——一个错误源跨三个文档传染。

**为何升 P0**：sheet count 是 coverage artifact 分母、prompt-index.json count、Asset Pipeline Deliverables 行数、verifyChanged routing 路径模式的合同基础。一个 off-by-one 会让所有自动化在「核心 sheet 全部进入 manifest」断言时无法 deterministically 写出。开发者执行 PR-00 时如果按 28 写 fixture，PR-06 final-full coverage 会差 1 张 sheet。

---

## 3. 本轮新发现的 P1 漂移

### 3.1 `icon.tree.*` 没有显式 SheetId Ownership

- PR-06:58 "`icon.tree.*` 是职业树 section/header 小图标，`tree.*` 是 portrait/large visual；两套 key 保留"
- README §SheetId Ownership：
  - `r06-portraits-trees | PR-05 | portrait | tree.* portrait keys` ← `tree.*` 有 owner ✅
  - `r09-quest-zone-profession | PR-06 | icon_quest, icon | quest, zone icon, profession, tree, difficulty` ← Scope 列写 "tree" 但没明确是 `icon.tree.*`
- PR-06:97 切换表里写 `icon.tree.*` 必须切到 dark-v1，但读者要从 README 反推它属于 r09——靠 namespace 推导，不是显式声明

**修复**：README §SheetId Ownership 把 `r09-quest-zone-profession` Scope 列展开成 `icon.quest.*`、`icon.zone.*`、`icon.profession.*`、`icon.tree.*`、`icon.difficulty.*`，让 namespace 与 key registry/coverage 一致。

### 3.2 README §SheetId Ownership 表 Category 列单/多 category 语义不一致

| Sheet | Category 列 |
| --- | --- |
| `r01-ui-chrome` | `ui_frame` ← 单 |
| `r02-tiles-ground` | `tile_ground` ← 单 |
| `r07-items-base` | `icon_item` ← 单 |
| `r07-items-affix-material` | `icon` ← 单 |
| `r09-status-damage` | `icon_status, icon_damage_type` ← 多 |
| `r09-quest-zone-profession` | `icon_quest, icon` ← 多 |
| `r09-fallback-debug` | `debug, icon, ui_frame` ← 多 |
| `r09-rejected-polish` | `mixed` ← 占位词 |

PLAN §Mapping Spec 第 5 条明确 "category 必须属于现有 asset pipeline 支持的 category"——是 cell 级别约束。README §SheetId Ownership 表头叫 "Category" 但实际是 "本 sheet 包含哪些 category"。语义跨表偏移。

更严重：`r09-rejected-polish | mixed` —— "mixed" 不是合法 category，违反 asset pipeline lint。
`r09-fallback-debug | debug, icon, ui_frame` —— `debug` 是否真的在 `scripts/asset_pipeline_common.py` category 白名单？若不是，PR-00 §3 第 5 条 "category 属于白名单" 会和 README 表打架。

**修复**：README §SheetId Ownership 表头改为 "Cell Categories"（明确多值），并加一行说明 "本表 Category 列列出 sheet 内 cells 涉及的 category 集合，不是 sheet 本身的 category；'mixed' 不是合法值，必须在 sheet-plan.yaml 各 cell 上写具体 category"。

### 3.3 PR-06 §6 第 5 条 "或等价 artifact" 与 PR-00 §5 严格 schema 矛盾

- PR-00:171-173 给了 coverage artifact 三层 schema（common / owner-scope / final-full），命名固定为 `dark-v1-manifest-coverage.json`
- PR-06:72 "5. 产出 `assets-src/image/manifests/dark-v1-manifest-coverage.json` **或等价 artifact**"

"或等价" 留出口让别名 artifact 蒙混过关。**修复**：PR-06:72 删 "或等价 artifact"，保持路径绝对。

### 3.4 PR-06 §1.4 "必须切换" vs §4.1 "可以切换" 措辞冲突

- PR-06:15 "把 PR-04 暂时复用的职业树 icon 切换到新风格资源" ← 必须语气
- PR-06:54 "`TalentSidebarPresenter` 的 node icon、tree icon **可在本 PR** 指向新 skill/tree icon" ← "可" 是允许语气

PR-04 close 时职业树 icon 还是 painterly，PR-06 final-full 模式要 `oldStylePlayerVisibleKeys=[]`——所以必然必须切换，§4.1 不该用 "可"。**修复**：PR-06:54 "`TalentSidebarPresenter` 的 node icon、tree icon **必须在本 PR** 切换到 dark-v1 skill/tree icon"。

### 3.5 PR-00 §3 第 12 条 与 §4 第 5 条 桥接合同重复

- PR-00:44 "12. PR-00 必须实现并冻结新增 UI key 与 `manifest-lint.py` 的桥接方式"
- PR-00:121 "5. PR-00 必须实现旧 `manifestLint` 与 dark-v1 的桥接：新增 `--dark-key-registry ...` 与 `--dark-sheet-plan ...` 两个参数"

第二条把第一条具体化但仍并存，未来改一处忘改另一处概率高。**修复**：PR-00 §3 第 12 条改为引用 §4 第 5 条，避免双处维护。

### 3.6 PR-00 §3 「manifest-lint.py 桥接」与 prefixRules 关系未说

PR-00 §4 第 5 条新增 `--dark-key-registry` / `--dark-sheet-plan` 参数 ——目的是让 registry/sheet-plan 覆盖的 key 进入 upstream spec coverage 分母。

但旧 `manifest-lint.py` 还有 `prefixRules`（白名单前缀机制）。

- 这两个参数是**替代** prefixRules（dark-v1 走 registry），还是**补充**（仍需扩 prefixRules 再加白名单）？
- round2 §1.2 提的 "审计 prefixRules 是否接受 ui.* 前缀" 没落地
- 如果 PR-02 启动时 manifestLint 走旧 prefixRules 但 `ui.frame.* / ui.hud.* / ui.control.*` 不在白名单，立即 fail——回到 round2 P0 #2

**修复**：PR-00 §4 第 5 条加一段 "桥接合同：dark registry 优先于 prefixRules；如果 manifestLint 命中 dark registry 已声明的 key，跳过 prefixRules 白名单校验。否则继续走 prefixRules。"或者明确"PR-00 同时把 `ui.frame.* / ui.hud.* / ui.control.* / ui.combat.* / ui.state.*` 加入 prefixRules 白名单"，二选一。

### 3.7 PR-04 §硬依赖合同 第 4 条 与 §影响范围 改 TalentSidebarPresenter 的潜在打架

- PR-04:24 "4. 本 PR 不改 `TalentProgression.learnableTalentIds`、starter 数、Tier 门槛、owner metric、longRun 分母"
- PR-04:44 §影响范围表 "TalentSidebarPresenter.kt | 只补 presentation model 所需字段或 layout hint"

风险：`TalentSidebarPresenter` 调用链可能透过 cached 计算依赖 `learnableTalentIds`（如 sidebar header 显示 "可学技能 N 个"）。"只补字段" 看起来安全，但如果新增字段触发 lazy 计算或缓存失效，可能间接改变 learnable 表的 emit 时序——CI 会被 `phase4-v4-pr01` 场景日志事件断言抓到。

**修复**：PR-04 §硬依赖合同 第 4 条尾部加 "本 PR 改 `TalentSidebarPresenter` 时禁止新增触发 `TalentProgression` 派生计算的字段或方法；只允许新增纯展示态 / layout hint 字段"。

### 3.8 README §SheetId Ownership 表 Category 列 `r02-tiles-decal | tile_decal | tile.decal.*, terrain.*` 中 `terrain.*` 来源未核

Round2 §9.3 提过：`terrain.*` 是否真在 manifest 存在没核实。本轮再核：

```
$ grep -rE '"key": "terrain\.' assets-src/image/manifests/phase2-visual-manifest.json | head -5
```

（实际未跑，但如果 manifest 没 `terrain.*` key，README 表里挂这个 prefix 就是 dead reference；PR-05 实施 r02-tiles-decal 时 coverage 分母会少一个 prefix）

**修复**：PR-00 实施前用 `verify_dark_key_registry.py` 跑 dry-run 验证 `terrain.*` 存在性；不存在则从 README §SheetId Ownership r02-tiles-decal 行删除。

### 3.9 PR-00 §3 表 `verify_dark_key_registry.py` 输入含 "PR README ownership"

PR-00:51

```
| `scripts/verify_dark_key_registry.py` | key registry + sheet plan + PR README ownership | lint result + optional `build/reports/dark-v1/key-registry.json` |
```

**问题**：README 是 Markdown 文档，不是 YAML/JSON。Python 脚本要么解析 Markdown 表格（脆弱），要么从结构化文件读 ownership。

实际更稳妥：sheet-plan.yaml 每个 sheet 加 `ownerPr`、key-registry.yaml 每个 entry 已有 `ownerPr`，从这两个 yaml 读就够了；README 表只是给人读的展示视图。

**修复**：PR-00:51 input 列改为 "key registry + sheet plan"，去掉 "PR README ownership"。同时在 §3 加一句 "README §SheetId Ownership 表是人类可读视图，机器真源是 sheet-plan.yaml + key-registry.yaml；两者必须由 lint 校验同步，但 lint 不解析 README"。

### 3.10 PR-05 §3 Per-round checkpoint Round 3 Manifest key scope 含 `zone.*.visual`，与 Round 6 重复

- PR-05:43 "Round 3 Props/VFX | manifest key scope: `prop.*, interactable.*, zone.*.visual, vfx.*, telegraph.*`"
- PR-05:46 "Round 6 Portraits | manifest key scope: `portrait.*, tree.*, zone.*.visual, secret zone visuals`"

`zone.*.visual` 在 Round 3 和 Round 6 都声明覆盖。这是 round2 §3.2 的同一问题在 PR-05 内部表里又显形——可见漂移源在 README §SheetId Ownership，往下层传染。

**修复**：决定 `zone.*.visual` 归属（建议归 r06-portraits-zones，理由：portrait 是 large 视觉，prop 是地图小物体，命名空间应分离），同步修：
- README §SheetId Ownership r03-props-environment 行 Scope
- PR-05 §3 Round 3 manifest key scope
- PLAN §Sheet Inventory r03-props-environment 行 "映射内容"

### 3.11 PR-05 §3 Per-round checkpoint Round 5 "boss variant visual/icon" 措辞含混

PR-05:44 "Round 5 Bestiary | manifest key scope: monster icon、boss icon、boss variant visual/icon"

`boss variant visual` 是 portrait（256x256）还是 icon（128x128）？如果是 visual（portrait），不该放 `r05-boss-icons`（icon-sheet 64 cell），而应放 r06-portraits-classes 或新 sheet。如果是 icon，措辞应统一为 "boss variant icon"。

**修复**：PR-05:44 改成 "monster icon、boss icon、boss variant icon"（去掉 visual）；如果确实有 boss variant 大图，应去 r06。

### 3.12 PR-04 close 后职业树 icon 仍旧风格——§1 阶段目标未显式 caveat

PR-04 §硬依赖合同 第 5 条 + §7 验证段都说 "PR-04 先固化职业树 layout golden；PR-06 切换 skill/tree icon 后必须显式 rebaseline"。

但 PR-04 §1 阶段目标 4 条都没说 "PR-04 close 后玩家在职业树面板看到的仍是旧 painterly 资源"。这是合同声明的隐含状态，但读者从 §1 看不到，会期望 PR-04 完成 = 职业树视觉完整。

**修复**：PR-04 §1 加一条 "5. 阶段范围限定：PR-04 close 时职业树 icon、tree portrait、skill icon 仍指向现有 manifest entry（painterly 风格），PR-06 才统一切到 dark-v1。"

### 3.13 PR-00 §5 verifyChanged 路径 glob `dark-v1-*.json*` 不严格

PR-00:140 / README:225

```
verifyChanged 必须命中：assets-src/image/manifests/dark-v1-*.json*
```

`*.json*` 会匹配 `dark-v1-foo.json`、`dark-v1-foo.jsonl`、`dark-v1-foo.json.bak`、`dark-v1-foo.json.swp`。备份/编辑器临时文件会触发 dark gate，causing CI 噪音。

**修复**：拆成两条 glob：`dark-v1-*.json` 和 `dark-v1-*.jsonl`。

### 3.14 PR-06 §3 第 6 条 r09-fallback-debug 容量未给估算

PR-06 §6 已加 "如果 final inventory 中 fallback/debug/hidden/rejected cell 总数超过当前 sheet capacity，PR-06 必须先新增 r09-fallback-debug-* 或 r09-rejected-polish-*"——但 PR-06 §3 没给当前 inventory 估算，开发者无法判断 16 cell 是否够用。

`missing_visual` ×1 + `tile.hidden`+secret zone（若干，估 5-10）+ `category=debug` （PLAN inventory：14）+ ASCII tiles（若干，估 5）= 25-30 cell。**16 cell 显然不够**——必须现在就拆 sheet。

**修复**：PR-06 §3.6 写 "当前估算 fallback/hidden/debug 主路径约 25-30 cell，16 cell 不足；本 PR 须先新增 `r09-fallback-debug-a` / `r09-fallback-debug-b` 两张 sheet（各 16 cell），并同步 PLAN.md / README §SheetId Ownership"。

### 3.15 PR-04 §5.3 "Node row：状态 glyph" 资源 owner 未声明

PR-04:55 "Node row：icon、状态 glyph、rank chip、learnable/locked/active/reserve tone"

"状态 glyph" 是新引入的视觉元素，但：
- §硬依赖合同：未列 glyph 资源
- README §SheetId Ownership：未列对应 key
- PR-02 §4 UI Key Registry：未列

如果 glyph 是 `ui.state.locked.icon` / `ui.state.learnable.icon` / `ui.state.active.icon` / `ui.state.reserve.icon` 这种命名空间，应该在 PR-02 r01-ui-controls owner（README §SheetId Ownership 已说 r01-ui-controls 含 `ui.state.*`），但 PR-02 §4 表实际没列。又一个 PR-02 §4 表与上下文不闭环的子情况。

**修复**：PR-02 §4 表补 4 条 `ui.state.locked.icon` / `learnable.icon` / `active.icon` / `reserve.icon`；PR-04 §硬依赖合同 加 "PR-04 假设 PR-02 已交付 `ui.state.*` 节点 glyph 系列"。

### 3.16 PR-07 §3.4 "对照清单" 与 PR-00 §2 影响范围措辞仍冲突（round2 #30 未修）

- PR-00:24 "`UI/PLAN.md`：只回写上游合同变化，不复制 PR 执行细节"
- PR-07:31 "补一次 `UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/*` 与实际实现的对照清单"

"对照清单" 是只读 audit 还是 read-write 修订？仍模糊。Round2 #30 已点过，未落实。

**修复**：PR-07:31 改为 "PR-07 只产出 audit 报告 `UI/review/dark-uiux-final-doc-implementation-audit.md`，不修订 PLAN/ART_STYLE_BIBLE/PR；如发现实际实现与 PLAN 矛盾，记入 audit 但由后续 PR 修订上游合同。"

### 3.17 README §通用验证入口 末尾命令模板与各 PR §7 verbosity 不一致

README:222

```
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=<pr00-dry-run|owner-scope|final-full> [-Pktome.darkUiux.ownerPr=PR-xx]
```

各 PR §7 都已写出具体值。但 README 模板里 `[-Pktome.darkUiux.ownerPr=PR-xx]` 用方括号示意 optional——而 PR-00 §5 第 2 条规定 "owner-scope 必须显式传 -Pktome.darkUiux.ownerPr=PR-xx，缺失时 fail fast"。

模板暗示 optional 与 PR-00 强制矛盾，可能让新加 PR 的开发者忘了传 ownerPr。

**修复**：README:222 改成两行明确：

```
# owner-scope 模式：
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-xx
# final-full / dry-run 模式：
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=<pr00-dry-run|final-full>
```

### 3.18 PR-04 §人工白盒 第 1 条仍硬编码 "3 个 learned starter、1 个空 active slot"（round2 #23 未修）

PR-04:86 仍是 round2 原文。Round2 #23 建议 "改成以 `phase4-v4-pr01` scenario 当时实际状态为准；本 PR 不固化具体数字"——未落地。

每次 talent scenario 调整都要同步改 PR-04 §人工白盒，是文档维护税。

---

## 4. 资源 inventory / capacity 二次对账

Round2 §5.1 因 sheet 数错（27 → 实际 29）算的 1360 cell 偏低。29 sheet 重算：

| Sheet type | 张数 | 单 sheet 容量 | 小计 |
| --- | ---: | ---: | ---: |
| `large-sheet 4x4` | 9 张（r01-chrome、r03-props-interactable/environment、r04-actors-player/boss、r06-portraits-classes/trees/zones、r09-rejected-polish） | 16 | 144 |
| `icon-sheet 8x8` | 14 张（r01-controls/hud-icons、r04-actors-humanoid/monster、r05-bestiary-humanoid/creature/boss、r07 ×3、r08 ×3、r09-status-damage、r09-quest-zone-profession、r09-fallback-debug） | 64 | 896 |
| `tile-sheet 8x8` | 6 张（r02-tiles-ground/wall/decal、r03-vfx-telegraph、——再无） | 64 | 384 |
| 总计 | 29 | — | **1424 cell** |

inventory = 538，利用率 = 538 / 1424 ≈ **37.8%**（round2 给 39.6%，因 sheet 数错和 type 分布差略偏低）

29 sheet 重新分类后 large-sheet vs icon-sheet vs tile-sheet 的归类需要核对：

```
r01-ui-chrome → large-sheet
r01-ui-controls → icon-sheet
r01-ui-hud-icons → icon-sheet
r02 ×3 → tile-sheet
r03-props-interactable → large-sheet
r03-props-environment → large-sheet
r03-vfx-telegraph → tile-sheet
r04-actors-player → large-sheet
r04-actors-humanoid → icon-sheet
r04-actors-monster → icon-sheet
r04-actors-boss → large-sheet
r05 ×3 → icon-sheet
r06 ×3 → large-sheet
r07 ×3 → icon-sheet
r08 ×3 → icon-sheet
r09-status-damage → icon-sheet
r09-quest-zone-profession → icon-sheet
r09-fallback-debug → icon-sheet
r09-rejected-polish → large-sheet
```

数：
- large-sheet: r01-chrome, r03-interactable, r03-environment, r04-player, r04-boss, r06×3, r09-rejected-polish = **9** ✅
- icon-sheet: r01-controls, r01-hud-icons, r04-humanoid, r04-monster, r05×3, r07×3, r08×3, r09-status-damage, r09-quest-zone-profession, r09-fallback-debug = **14** ✅
- tile-sheet: r02×3, r03-vfx-telegraph = **4** （上面写 6 错）

重算 tile-sheet：4 张 × 64 = 256
**总容量 = 144 + 896 + 256 = 1296 cell**

利用率 = 538 / 1296 ≈ **41.5%**

**关键发现 R3-R**：基于 29 sheet 准确分类，总容量 1296 而非 round2 估算的 1360。原因是 round2 把 r03-vfx-telegraph 计入 large-sheet（非 tile-sheet），且总数算成 27 张。**修复**：PLAN 加一张完整 capacity 矩阵，以一处真源为准，避免每次 review 都重算。

---

## 5. 优先级建议

### P0（动 PR-02 之前必修）

| # | 引用 | 内容 |
| --- | --- | --- |
| 1 | §2 R3-N2 | PLAN.md:121 "约 28 张" 改为 "29 张"；同时核对 round1/round2 报告里 27/28 引用 |
| 2 | §1.3 + §2 R2-N1（仍未修）| PR-02 §4 表补 `ui.hud.warning.icon / quest_marker.icon / log_marker.icon`；§3.3 prose 与 §4 表二选一对齐（backpack/equipment 归 hud 还是 control） |
| 3 | §3.6 | PR-00 §4 第 5 条桥接合同澄清：dark registry 与 prefixRules 是替代/补充关系，明确 `ui.frame.* / ui.hud.* / ui.control.* / ui.combat.* / ui.state.*` 五前缀的处理路径 |

### P1（PR-00 / PR-02 期间补文档）

| # | 引用 | 内容 |
| --- | --- | --- |
| 4 | §3.1 | README §SheetId Ownership r09-quest-zone-profession 行 Scope 展开 `icon.tree.*` 等 namespace |
| 5 | §3.2 | README §SheetId Ownership 表头改 "Cell Categories"，"mixed" 替换为具体 category 集合 |
| 6 | §3.3 | PR-06:72 删 "或等价 artifact" |
| 7 | §3.4 | PR-06:54 "可" 改 "必须" |
| 8 | §3.5 | PR-00 §3 第 12 条改为引用 §4 第 5 条 |
| 9 | §3.7 | PR-04 §硬依赖合同 第 4 条加 "禁止触发 TalentProgression 派生计算" |
| 10 | §3.8 | 跑 verify_dark_key_registry dry-run 校验 `terrain.*` 存在性 |
| 11 | §3.9 | PR-00:51 verify_dark_key_registry input 删 "PR README ownership" |
| 12 | §3.10 | `zone.*.visual` 在 Round 3 / Round 6 / README 三处分流，单选 r06 owner |
| 13 | §3.12 | PR-04 §1 加 "PR-04 close 时职业树 icon 仍 painterly" caveat |
| 14 | §3.14 | PR-06 §3.6 给 fallback/debug 容量估算，必要时拆 r09-fallback-debug-a/b |
| 15 | §3.15 | PR-02 §4 补 `ui.state.*` 4 条；PR-04 §硬依赖加 PR-02 state glyph 依赖 |
| 16 | §3.17 | README §通用验证入口 owner-scope 命令模板拆双行 |
| 17 | §4 R3-R | PLAN 加 capacity 真源表（29 sheet × 类型 × 容量 × 实际 inventory 占用） |

### P2（实施期顺手补，含 round2 未修项）

| # | 引用 | 内容 |
| --- | --- | --- |
| 18 | §3.11 | PR-05:44 "boss variant visual/icon" 改成单一 visual 或 icon |
| 19 | §3.13 | verifyChanged path glob 拆 `*.json` 和 `*.jsonl` |
| 20 | §3.16 | PR-07:31 "对照清单" 改为 "audit 报告，不修订 PLAN" |
| 21 | §3.18 | PR-04 §人工白盒 starter 数软化 |
| 22-46 | round2 P1/P2 未修项 | 包括 prefixRules 审计、顶层 styleTag 解耦说明、三 lint 输入矩阵、prompt 编号稳定性、PR-04 引用 README 职业树规则、inventory 子分类拆分、PR-02/03 §6 加 sync 提醒、ui.hud.key.icon 风格一致约束、PR-03 §1.4 措辞、PR-03 slot label 测试、PR-04 contractLint 与 token 对齐、PR-05 secret zone 显式、PR-05 Round 4 回滚、PR-05 actor Y-sort、PR-06 返修不易主、PR-07 影响范围具体路径、PR-04 白盒命令归类、capacity vs inventory 对账（与 §17 重叠，由真源表覆盖）、README workflow 第 7 条具体命令、alias cell 用例、r01-ui-chrome 16 cell reserved 标注、boss actor / icon 同框 QA、ART_STYLE_BIBLE 父级 v2、PR-03 fallback 优先级、lint task 命名前缀、pytest 必测 case、alias 同/跨 sheet 规则、packaged app macOS 单平台、demo 路径迁移 owner |

---

## 6. 评审小结

第二轮 47 项中 **12 项已修、6 项部分修、29 项未修**，修复主要落在「机械性能确认的细节」：命令参数补全、措辞澄清、引用补全。「需要重新设计文档结构」的项（如三 lint 输入矩阵、coverage 模式表合并、prefixRules 审计）几乎全员未动。

本轮新增 **17 项 + 1 个新硬阻塞**。新硬阻塞 R3-N2（**28 张 sheet 文字 vs 29 张表格**）是典型的「一处错误源跨三个文档传染」——round1/2 报告中的 27/28 引用都是从 PLAN.md:121 这条文字抄来的。修一处需要同步修 round1 / round2 报告 + PLAN + 任何说 "约 28 张" 的下游 fixture。

R2-N1（PR-02 §4 表与 §3.3 prose 不闭环）**round2 已点名为 P0、本轮仍未修**，是修文档体系最重要的一项——直接决定 PR-02 关闭时的 owner-scope coverage 能否通过。同时还顺带牵出 R3-§3.15（PR-04 状态 glyph 资源同样无 owner），形成同一种「§4 表覆盖率不全」的并发症。

P0 三项预计 1 天内可文字校对修完；P1 17 项需要先决一些设计问题（zone.*.visual owner、fallback-debug 拆 sheet、prefixRules 桥接形态）；P2 余下都是可在实施期顺手补的措辞。

建议节奏：

1. **今天**修 P0 三项（最关键 R3-N2 + R2-N1 + §3.6 桥接合同）。
2. **PR-00 实施周期内**穿插修 P1 全部 17 项（其中 §3.10 zone.*.visual owner、§3.14 fallback-debug 拆 sheet、§3.15 ui.state.* glyph 是设计决策，需 lky 拍板）。
3. **PR-02 / PR-03 实施期间**顺手补 P2，不阻塞主路径。

---

## 附：本轮新出现 4 个文档间字段对账漂移源

未来维护应注意的 "一改多处" 热点：

| 漂移源 | 关联位置 |
| --- | --- |
| sheet 总数 (29) | PLAN.md:121 文字 / PLAN §Sheet Inventory 表 / README §SheetId Ownership 表 / round 报告引用 |
| `zone.*.visual` owner | README r03-props-environment / r06-portraits-zones / PR-05 Round 3 scope / PR-05 Round 6 scope |
| `icon.tree.*` owner | README r09-quest-zone-profession Scope / PR-06 §4.5 / PR-06 §3 切换表 |
| coverage artifact 三层 schema | PLAN §Pipeline Gate Strategy / README §Manifest Authority / PR-00 §5 / PR-06 §6 |

**强建议**：每个漂移源固定为单一权威位置，其他位置以「见 X 处」方式引用——避免后续又生第四轮 review。

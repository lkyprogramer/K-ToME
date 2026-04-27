# 暗黑 UI/UX PR 文档深度 Review 报告

> 评审日期：2026-04-27
> 评审范围：`UI/PLAN.md` + `UI/ART_STYLE_BIBLE.md` + `UI/pr/README.md` + `UI/pr/dark-uiux-pr00…pr07-*.md`
> 交叉对照对象：`client/src/main/resources/manifests/visual-manifest.json`、`scripts/asset_pipeline_common.py`、`scripts/asset-lint.py`、`scripts/manifest-lint.py`、`scripts/process_assets.py`、`build.gradle.kts`、`game/src/main/resources/data/professions/index.yaml`、`client/src/main/kotlin/com/ktome/client/**`

本次评审目标：在动手开 PR 之前，按"合同一致性 / 管线衔接 / PR 边界 / 验证覆盖 / 风险与回滚 / 文档治理"六个维度，把会让某个 PR 中途返工或合入即破坏 lint 的隐患全部摊开。结论：**当前 8 份 PR 文档不能直接进入开发**，至少有 3 类"硬阻塞"必须先在 PR-00 阶段修掉，另有多处跨 PR 边界、数量、key 不一致问题。

---

## 0. TL;DR — 必须在动工前解决的硬阻塞

| # | 问题 | 影响 PR | 影响后果 |
| --- | --- | --- | --- |
| H1 | 引入新 `ktome-dark-fantasy-sprite-ui-v1` style tag，但 `EXPECTED_STYLE_TAG` 与 `manifest-lint.py` 强制单 styleTag，没有迁移策略 | PR-00、PR-02、PR-06 | PR-02 一合入就让 `manifestLint`/`styleLint`/`assetLint` 全红 |
| H2 | `UI/sprite-sheets/sheet-plan.yaml` 是新 schema，但既不接入 `assetLint`，也没新 lint 任务 | PR-00、PR-02 起所有资源 PR | 新映射真源静默失效，错配/路径漂移不会被 CI 捕获 |
| H3 | 实际有 8 个职业（vanguard/arcanist/rogue/templar/berserker/spellblade/shadowblade/warden），文档体系按 4–6 个职业切 sheet | PR-04、PR-05、PR-06 | shadowblade/warden 全程没有 dark-v1 资源；PR-06 收口时被迫追加 sheet 或破规则 |

---

## 1. 跨文档一致性问题

### 1.1 职业总数错位（H3）

`game/src/main/resources/data/professions/index.yaml` 当前注册 8 个职业：

```
BASE     : vanguard, arcanist, rogue, templar
ADVANCED : berserker, spellblade, shadowblade, warden
```

但相关文档与 PR 各说各话：

| 文档 | 说法 |
| --- | --- |
| `ART_STYLE_BIBLE.md` §7.3 | 只列 `Vanguard / Rogue / Templar / Arcanist`（4 个） |
| `PLAN.md` §Resource Inventory | 未细分；表格按 8 类资源给总数 |
| `PLAN.md` §Sheet Inventory Round 4 | 提"4 职业 + Boss"，与 8 职业不符 |
| `PLAN.md` §Sheet Inventory Round 8 | 拆成 `vanguard-berserker / templar-rogue / arcanist-spellblade`，覆盖 6 职业，**漏 shadowblade / warden** |
| `pr04-profession-tree-ui.md` §1.3 | "三棵职业树"——若每职业 3 树则需 24 树 portrait，与 PLAN 的 12 个 tree portrait 不符 |
| `pr06-skills-status-quest-full-manifest.md` §3.1 | Round 8 仅 `r08-skills-vanguard / arcanist / rogue / templar`，**漏 berserker/spellblade/shadowblade/warden** |
| `visual-manifest.json` 现状 | 已存在 `icon.skill.berserker.*` 与 `icon.skill.spellblade.*`，但无 `shadowblade/warden` 的 skill/portrait/profession/tree |

**结论**：UI 重构的目标是"全量替换玩家可见视觉"，但目前 PR-04/05/06 的资源覆盖至少漏 2 个职业。要么 PLAN 显式声明"shadowblade/warden 不在 v1 范围内、由后续 PR 处理"，要么 PR-04/06 的 sheet 列表必须扩到 8 职业。否则玩家选择 advanced 职业时会看到旧 painterly 风格的 fallback / 缺图。

### 1.2 Sheet ID 在 PLAN ↔ PR 之间漂移

PLAN.md §Sheet Inventory 与 PR-05 / PR-06 给的 sheet ID 大量不一致：

| Round | PLAN.md | PR 文档 |
| --- | --- | --- |
| 4 | `r04-actors-player`、`r04-actors-humanoid`、`r04-actors-monster`、`r04-actors-boss` | PR-05：`r04-actors-player-profession`、`r04-actors-companion` |
| 5 | `r05-bestiary-humanoid-icons`、`r05-bestiary-creature-icons`、`r05-boss-icons` | PR-05：`r05-bestiary-common`、`r05-bestiary-elite`、`r05-bestiary-boss` |
| 6 | `r06-portraits-classes`、`r06-portraits-trees`、`r06-portraits-zones` | PR-05：`r06-portraits-profession`、`r06-portraits-tree`、`r06-portraits-zone` |
| 8 | `r08-skills-vanguard-berserker`、`r08-skills-templar-rogue`、`r08-skills-arcanist-spellblade` | PR-06：`r08-skills-vanguard`、`r08-skills-arcanist`、`r08-skills-rogue`、`r08-skills-templar`（4 张，无合并、无 advanced 职业） |

PLAN.md §Mapping Spec 第 1 条强约束 `sheetId + row + col` 全局唯一——而 sheetId 命名都未对齐，"全局唯一"何谈达成。**必须在 PR-00 把 PLAN.md sheet 清单与 PR-05/PR-06 的 sheet 清单合并成一份单一来源**，而不是 PLAN/PR 各自维护。

### 1.3 HUD 图标 vs item 图标边界冲突

- PR-02 §3.2 `r01-ui-hud-icons` 内容包含 `gold`、`key`、`backpack`、`equipment slot`、`quest marker`、`empty slot`。
- PR-03 §3.1 `r07-items-base` 也声明包含"金币、钥匙、材料"。

`gold` 和 `key` 同时被两份 PR 声明。两者要么映射到同一 manifest key（则 PR-03 不该再出），要么是不同 key（则要分清"HUD 显示用"与"背包 item 实例用"——目前 manifest 只看到 `icon.quest.armory_key / icon.quest.seal_key`，没见 `ui.hud.gold.icon` / `ui.hud.key.icon`）。需要在 PR-00 阶段就把"HUD 数值化身的小图标"和"背包 item 实例图标"分到不同 namespace 并锁死归属。

### 1.4 `tree.X` vs `icon.tree.X` 双 key 未规划替换路径

现有 manifest 同时存在：

```
"key": "tree.vanguard_arms",       category: portrait, raw: phase2/p2-b/tree_vanguard_arms.png
"key": "icon.tree.vanguard_arms",  category: icon,     raw: phase2/p2-b/icon_tree_vanguard_arms.png
```

- PR-04 §1.5 写 "默认复用现有 `icon.skill.*`、`icon.tree.*`、`tree_*`、`portrait_*` 资源"——但实际 key 用点号不是下划线，`tree_*` / `portrait_*` 是 key 还是文件前缀含糊；
- PR-06 §4.1 "TalentSidebarPresenter 的 node icon、tree icon 可在本 PR 指向新 skill/tree icon"——只点了 `icon.tree.*`；
- 没有任何 PR 显式说明 `tree.X`（portrait 类的大图）由 Round 6 portraits 替换还是仍保留旧画。

**建议**：PR-04 必须列出"职业树面板上 哪些位置消费 `tree.*` portrait、哪些消费 `icon.tree.*`"；PR-06 验收清单要逐 key 列表"切换前 → 切换后"。

### 1.5 `icon_status` 与 `icon.mutation` 命名分裂

PLAN.md §Resource Inventory 给 `icon_status: 23`。manifest 实际是 `icon.status.* = 11` + `icon.mutation.* = 12`，加起来 23。但：

- PR-06 §3.1 把 `r09-status-damage` 与 `r09-quest-zone-profession` 拆成两 sheet；
- ART_STYLE_BIBLE.md §7.4 "状态正负面" 没区分 status / mutation；
- `StatusIconResolver.kt` 是否对 mutation 走同一解析链未在 PR-06 说明。

应在 PR-06 §3 明确：`icon.mutation.*` 是否进 Round 9 同一张 sheet、是否复用 `icon_status` category 还是新增 `icon_mutation`。

### 1.6 ART_STYLE_BIBLE 颜色 token 不够覆盖职业树四态

PR-04 §1.3 显式四态：`LOCKED / LEARNABLE / LEARNED_RESERVE / LEARNED_ACTIVE`。

ART_STYLE_BIBLE.md §3 颜色合同里：`cold-cyan` 给"可交互边缘"，`ember-gold` 给"标题、稀有、确认态"，`muted-text` 给"次级文本"。**没有为 reserve 与 active 分别规定 hue，也没有 locked tone**。Renderer 一旦实施会临场拍 token，破坏"风格合同"权威。

PR-04 §3 影响范围里 `UiDesignTokens.kt` 缺席。**必须把"四态 tone token 扩展"补进 PR-01 或 PR-04 的 token 改动清单**，并相应更新 ART_STYLE_BIBLE。

---

## 2. 资源管线 / 合同遗漏（H1、H2、H3 的细化）

### 2.1 styleTag 升级路径未定义（H1）

证据：
- `scripts/asset_pipeline_common.py:16`：`EXPECTED_STYLE_TAG = "ktome-middle-fantasy-painterly-tile-v1"` 写死。
- `scripts/asset-lint.py:86`：plan styleTag 必须等于 `EXPECTED_STYLE_TAG`；line 185 禁止 asset 内覆盖 styleTag。
- `scripts/manifest-lint.py:159-173`：所有 plan styleTag 必须互相一致，并且 manifest 顶层 styleTag 必须等于 plan styleTag（runtime manifest 同理）。
- `client/src/main/resources/manifests/visual-manifest.json:3`：当前 manifest 顶层 `"styleTag": "ktome-middle-fantasy-painterly-tile-v1"`。

文档现状：
- PLAN.md §Art Style Authority 与 ART_STYLE_BIBLE.md §1 说"新视觉纪元固定为 `ktome-dark-fantasy-sprite-ui-v1`"，并要求 sheet plan / contact sheet / manifest patch 都记录该 tag。
- PR-00、PR-02 都没提"如何让 lint 接受新 styleTag"。
- PR-06 "玩家可见 key 指向 dark-v1"——意味着大量 entry 用新 raw path，但 schema 无 per-entry styleTag，文档又承诺"不改 `VisualManifestEntry` schema"。

后果：
- PR-02 提交第一批 dark-v1 资源 → `assetLint` 立刻报 styleTag mismatch；
- 即便强行让 plan styleTag 也等于 middle-fantasy（不诚实），`manifestLint` 与 ART_STYLE_BIBLE 的 prompt 合同直接冲突。

**必须在 PR-00 增加任务**：
1. 决策方案 A：bump 全仓 `EXPECTED_STYLE_TAG` 到 dark-v1，旧 plan/manifest entry 同步迁移；
2. 决策方案 B：放宽 lint 接受多 styleTag 共存（schema 加 set），允许 entry/plan 各自声明 styleTag；
3. 决策方案 C：引入"manifest era"一级字段，区分 v1-painterly / v1-dark。

任一决策都必须在 PR-00 写进 PR 文档与 ART_STYLE_BIBLE，并修改 `asset_pipeline_common.py / asset-lint.py / manifest-lint.py`。否则 PR-02 不可能 merge。

### 2.2 `UI/sprite-sheets/sheet-plan.yaml` 没有 lint 入口（H2）

`build.gradle.kts:689` `assetLint` 任务硬编码读取一组 `assets-src/image/specs/phaseN-*.yaml`：

```
--plan       assets-src/image/specs/phase2-asset-plan.yaml
--extra-plan assets-src/image/specs/macos-app-icon-plan.yaml
--extra-plan assets-src/image/specs/phase3-pr09-gemini-plan.yaml
…  …
--extra-plan assets-src/image/specs/phase4-pr06-gemini-plan.yaml
```

`UI/sprite-sheets/sheet-plan.yaml`：
- 不在 `--plan` 列表中；
- schema 与现有 `phaseN-*-plan.yaml` 完全不同（顶层 `sheets[].cells[]` vs 现有 `assets[]`）。

PR-00 §3.4 "写清脚本合同"，但 §6 验证只跑 `verifyChanged`，没有任何 Gradle 任务读取 `sheet-plan.yaml`。

后果：
- PR-02 把新 cell 写错 row/col、targetKey 拼错、outputName 与 manifest `rawOutputPath` 不一致 — 这些都不会被 CI 抓到；
- contact sheet QA 状态、`reserved` 标记的覆盖检查全部成"君子协议"。

**必须在 PR-00 增加**：
1. 写一个 `sheetPlanLint`（独立 Gradle task），调用 `verify_sprite_sheet_map.py` 校验 sheet-plan.yaml 的 schema、唯一性、与 manifest 一致性；
2. 把 `sheetPlanLint` 加入 `verifyChanged` 与 PR README §通用验证 列表；
3. 或者——如果选择复用 `assetLint` —— 先扩 `asset-lint.py` 支持 sheet schema 并把新 plan 加入 `--extra-plan`。
4. PR-00 的 §6 验证命令要包含 `./gradlew sheetPlanLint`。

### 2.3 4 个新脚本无落地、无测试

PR-00 §3.4 列出 `generate_sheet_prompt.py / slice_spritesheet.py / render_contact_sheet.py / verify_sprite_sheet_map.py`，但：

- 没指定路径（`scripts/` 还是 `tools/`？）；
- 没说与现有 `process_assets.py` 是替代还是补充（`process_assets.py:55` 已对 `ui_frame / vfx_plate` 设置 canvas/padding，可能与新 slice 流程重叠）；
- 没要求单元测试 / dry-run fixture；
- PR-00 §6 验证只跑 `verifyChanged`，无法覆盖这些脚本的回归。

**建议**：PR-00 §3 明确每个脚本的 input/output schema、路径、单测样本；§6 验证命令补 `python3 -m pytest scripts/tests/test_sheet_pipeline.py` 或等价 Gradle wrapper。

### 2.4 `ALLOWED_CATEGORIES` 与 PLAN 的"新增 category"已经矛盾

PLAN.md §Mapping Spec 第 5 条："`category` 必须属于现有 asset pipeline 支持的 category，**新增 `ui_frame` / `vfx_plate` 也必须进入 lint 白名单**"。

实际：`scripts/asset_pipeline_common.py:18-37` 的 `ALLOWED_CATEGORIES` **已经包含** `ui_frame` 与 `vfx_plate`。`process_assets.py:55-75` 已为这两类 category 配置 canvas size 与 padding。

需要修正：
- 删除 PLAN.md "新增 …进入 lint 白名单" 这种"提议"语气，改为"已支持"陈述；
- 但同时 PR 应主动审计这些 category 的 canvas（256 / 192）、padding（5%、8%）能不能容纳暗黑 UI 框图的真实需求，否则可能首屏 ui_frame 被 over-crop 或 padding 太小；
- 现有 `EXPECTED_FOOTPRINT_BY_CATEGORY` 没收 `vfx_plate=overlay` 之外的特例，`tile_decal` 默认 `1x1` 是否能满足新 telegraph 资源、需要在 PR-00 评估。

### 2.5 `audioLint` 误用

PR-06 §7 "若本 PR 触发 audio manifest" → 但 PR 范围明确"不改音频"。同时 `verifyChanged` 在 root build 已默认包含 `audioLint`（`build.gradle.kts:353`）。把 `audioLint` 列为"附加可选"既冗余又误导，建议删掉。PR-04 / PR-05 同理：现有 verifyChanged 链已足，附加列表冗余得人为遗漏更危险。

---

## 3. PR 边界与依赖问题

### 3.1 PR-02 隐含依赖 PR-00 的"可执行脚本"，但 PR-00 工作量定为 M

PR-02 §5 "至少看到一个来自新 sheet 的 HUD/icon 资源"需要 PR-00 的 4 个脚本都能跑通。但 PR-00 §1 的目标只写"冻结合同"、§4 "不生成正式 PNG"、§6 验证不要求脚本可运行。

**风险**：执行者把 PR-00 当文档级 PR 推过，PR-02 启动时才发现要先写脚本，工作量从 L 膨胀到 XL。建议：

1. PR-00 显式在 §3 增加任务"交付可运行的 dry-run 脚本，覆盖 `r01-ui-hud-icons` 至少 3 个 cell"；
2. PR-00 §6 验证补一行 `python3 scripts/verify_sprite_sheet_map.py --dry-run`。

### 3.2 PR-04 "默认复用旧资源" + 新 dark UI chrome → golden 必然撕裂

PR-04 §1.5 说沿用旧 painterly icon，§4 装到新暗黑 panel，§7.4 又要 golden 通过。结果：
- 旧 painterly skill icon 嵌在新 charcoal 面板上，golden 截图固化为"风格混合"快照；
- PR-06 一旦把 `icon.skill.*` / `icon.tree.*` 全切到 dark-v1，PR-04 时期的 golden 会大面积 rebaseline。

**建议**：PR-04 §1.5 要么改成"等到 PR-06 完成才让 PR-04 的 golden baseline 落盘"，要么增加临时 placeholder 暗色 icon 集 (PR-02 已生成的 `r01-ui-hud-icons` fallback 临时复用)。当前合约二义。

### 3.3 PR-04 缺前置依赖 PR-02

PR-04 §1.4 "ACTIVE_TALENT_SLOT_CHOICE modal 使用新 UI chrome"——chrome key (`ui.frame.modal.body` 等) 来自 PR-02。但 PR-04 §前置条件只写 "PR-01 完成"。如果按这份依赖去并行（PR-04 与 PR-02 同时开），PR-04 里 modal chrome 拿不到 key。

**建议**：PR-04 前置条件补 PR-02。同时 README.md §执行顺序里 PR-02 → PR-04 已经串行，但 README 与 PR-04 文档说法应一致。

### 3.4 PR-05 包含 R3 VFX 但 telegraph layer 在 PR-03 已可能用到

PR-05 §3.3 Round 3 包含 `r03-vfx-telegraph`。PR-03 §5 "数量 badge 不遮挡 item 主体；品质 frame 不影响点击/选中区域" 与 tooltip 内 inline VFX 占位无直接冲突，但战斗 HUD 的 boss telegraph 在 PR-02 就可能开始接 (PR-02 §3.2 含 warning icon)。

**建议**：明确 R3 VFX 资源不在 PR-02、PR-03 的依赖路径上；如果 PR-02 的 warning icon 与 PR-05 的 telegraph 共享 visual key，写明谁是单一真源。

### 3.5 PR-06 "切换玩家可见 key 到 dark-v1" 没有 per-key 切换列表

PR-06 §6.1 "玩家可见主路径不再指向旧风格资源" 是定性合同。manifest 现有 538 entry 中：
- `icon.skill.*`：56
- `icon.tree.*`：12
- `tree.*`：12
- `portrait.*`：4 (仅 4 职业)
- `icon.profession.*`：4
- `icon.status.* + icon.mutation.*`：23
- `icon.quest.*`：2
- `icon.damage_type.*`：6
- `icon.monster.*`：30+
- `actor.*`：57
- `boss.*` `affix.*` `item.*` `talent.*` `tile_*` `prop_*`…

PR-06 §3 影响列只点了 `dark-v1/icons/` 与 `StatusIconResolver.kt` / `TalentAssetReferences.kt`，但没列**所有要切换的 manifest key 总清单**。结果是 PR-06 验收 §6.1 "玩家可见主路径不再指向旧风格" 会因清单未冻结而无法判定通过/失败。

**建议**：PR-06 必须在 §3 附一份"待切换 manifest key 表"（可分批，但全表必须给出），由 PR-00 / PR-02 阶段开始预填写。

### 3.6 PR-07 atlas 决策无量化阈值

PR-07 §5.1 "如果单 PNG 加载、纹理切换或内存没有实测问题，不引入 atlas"——什么叫"实测问题"？没有阈值。建议：

| 指标 | 触发 atlas 阈值（建议） |
| --- | --- |
| client smoke 内 sprite texture 数 | > 256 |
| 首屏 visual asset 加载累计 | > 800ms |
| 切场景峰值 GPU memory | > 256MB |
| `verifyChanged` golden 阶段耗时增量 | > 25% |

阈值定不定 PR-07 自己定，但**必须有具体数字**，否则该决策段落沦为后续争执点。最佳做法：在 PR-00 §合同段把阈值写进 PLAN.md。

### 3.7 PR-05 "XL" 工作量缺回滚边界段

PR-02 / PR-04 / PR-06 / PR-07 都有 §回滚边界，PR-05 §没有。XL 资源 PR 中途失败的代价最高，反而没写：

- 是否允许按 round 阶段性合入（每完成 1 个 round 一次 mini-PR）？
- 如果 R5 bestiary 资源失败，是否回滚 R2 tiles？
- 是否允许 manifest 部分 key 暂保留旧 path？

**建议**：PR-05 必须补 §回滚边界，至少明确"按 Round 切 sub-PR 是允许的，但 Round 内必须原子"。

### 3.8 fallback / debug 资源风格不闭环

manifest 顶层 `fallbackKey: "missing_visual"`，依赖 `debug` category 资源。PR-06 §6.4 "图标风格一致" 是闭环目标，但 §3 / §6 没说 `missing_visual` / `hidden_visual` 等 debug key 是否在 Round 9 `r09-fallback-polish` 内重绘。如果不重绘，"图标风格一致" 自相矛盾——只要触发 fallback，就是旧 painterly。

**建议**：PR-06 §3.2 显式列 `missing_visual / hidden_visual / debug.*` 必须进 Round 9。

---

## 4. 测试 / 验证覆盖断层

| PR | 缺口 | 建议补救 |
| --- | --- | --- |
| PR-01 | 新增 `GameShellLayout` helper 但 §6 只跑 `TileRendererCanvasTest / AsciiRenderModelTest`；layout 数学没单测 | 新增 `GameShellLayoutTest`（bounds 不重叠、最小尺寸下不溢出、文本截断断言） |
| PR-03 | 数量 badge / 品质 frame 没单测；只靠 golden 覆盖；后期一改图就难定位 | 增加 `EquipmentInventoryPresenterTest`，断言 badge 相对坐标稳定、frame 不入选中区域 |
| PR-04 | §6 没列 `phase4-v4-pr01` 白盒；talent telemetry 回归（commit a875713a）没纳入 | 补 `--tests com.ktome.client.…Phase4V4Pr01*` 与 talent telemetry 相关测试 |
| PR-04 | UiDesignTokens 改动未列 §3 影响范围 | §3 加 `UiDesignTokens.kt`；§6 跑 contractLint（已有但值得显式） |
| PR-05 | `TileLayerComposerTest` 是"如果新增就跑"——可选意味可省 | 改为强制：本 PR 必须新增/扩 `TileLayerComposerTest` 覆盖 ground / wall / decal / actor / VFX / boss telegraph 6 层先后 |
| PR-06 | 改 `TalentAssetReferences.kt` 但没跑 `InputHandlerTest` 或 modal 渲染测试 | 加 `--tests com.ktome.client.input.InputHandlerTest`；补 `ActiveTalentSlotChoiceModalRenderTest`（如不存在则新建） |
| PR-06 | `StatusPresentationModelTest` 不能验证 `icon.mutation.*` 的解析 | 补 `StatusIconResolverTest`，覆盖 status + mutation 双前缀 |
| PR-07 | "无重叠" 没定义判定方式（像素 / bounds / presenter） | §4 补：以 `GameShellLayout` bounds 数学 + 像素级 hitbox 双口径判定 |
| 全 PR | golden label 前缀 `dark-uiux-prNN-*` 是约定，没有 enforcement | `goldenScreenshot` 增加 `--label-prefix` 校验；或在 PR README §依赖规则 4 写明命令格式 |

---

## 5. 风险 / 回滚问题

### 5.1 PR-04 "不改职业树规则" 与 "更新白盒证据预期" 冲突

PR-04 §5.3 "不改 `phase4-v4-pr01` whitebox scenario 的玩法状态，只更新 UI 证据预期"。但白盒证据本身是测试断言，**改断言就是改测试**——要么承认 PR 边界包含 scenario expected output 的 UI 部分，要么说清"只允许改 presentation/layout 相关断言、不允许改 talent 状态/learnable/owner metric 断言"。当前措辞含混。

### 5.2 manifest 顶层 styleTag 升级 vs 父级合同冲突

PLAN.md §Art Style Authority 第 1 条："`docs/2026-03-13-art-style-bible.md` 继续是父级合同。" 第 4 条："如冲突，先保父级，再调整专项。"

但若 PR-06 把 manifest 顶层 styleTag 改成 `ktome-dark-fantasy-sprite-ui-v1`，父级合同里关于 painterly 的描述不再与运行时 styleTag 一致。**需要在 PR-00 同步父级合同**（添加 dark-v1 era 段落，或显式声明 dark-v1 是父级 painterly 的子分支）。否则两份合同打架，未来评审无所适从。

### 5.3 demo 路径迁移声明无 owner

PLAN.md §Summary "如果后续迁入 `docs/opt/ui-redesign/demo/` …" 没说由谁迁。PR-07 §3 列 "doc-vs-implementation 对照"，但没说包含 demo 迁移。建议：PR-00 或 PR-07 显式承担。

### 5.4 PR-06 "保留 history fallback" 与 "图标风格一致" 互斥

PR-06 §5.3 "可保留历史/debug fallback"，但 §6.4 又要"图标风格一致"。两者并存的唯一解释是 fallback 不算玩家主路径——文档要写明：fallback 触发即意味"调试/异常路径"，玩家不应在正常游戏中看到，UI 不必为它做 dark-v1 化。同时还要保证：用户白盒中 fallback 触发率 = 0，由 `assetLint` 的 coverage 校验承诺。

---

## 6. 文档治理 / 表述问题

### 6.1 PR-02 / PR-03 的 "cell 32x32 / 48x48" 措辞错误

- PR-02 §3.1 "`r01-ui-hud-icons`: `8 x 8` cell，单 cell `32x32` 或 `48x48`。"
- PR-03 §3.4 "每张 sheet 优先 `8 x 8`，单 cell `48x48`；装备 slot frame 可 `64x64`。"

ART_STYLE_BIBLE §6 与 PLAN.md §Sheet Types 已经规定 icon-sheet 单 cell **128×128**。PR-02/03 写的 32/48/64 是"运行时主体大小"或"目标缩放"，不是 cell 大小。措辞错位会让生图 prompt 一旦按字面执行就违反 §Sheet Types 约束。

**建议**：明确措辞——"sheet cell 仍是 128×128；主体在 cell 中按 32–48px 等效线宽生成，预留 10-14px 透明边距。" 同时与 ART_STYLE_BIBLE §6 "切分后运行时尺寸 icon = 160×160 canvas" 的"运行时 canvas" 区分清楚。

### 6.2 PR 文档没写"如何生成 raw sheet"

PLAN.md 提到 Codex CLI `0.125.0` 不直接出 PNG，要走"交互式生成 raw sheet/demo + 仓库脚本切分"。但 PR-00/02/05/06 都没写具体生成步骤——执行者拿到 PR 仍不知道：

- 用什么 prompt 工具（Codex 交互、外部 stable diffusion、Gemini？已存在 `generate_assets_gemini.py` / `generate_pr06_audio.py` 等）
- 生成失败时谁负责评估"重生成"还是"换工具"
- raw sheet 文件是否进 LFS / 是否被 `.gitignore` 排除

**建议**：PR-00 §3 增加"raw sheet 获取流程"段，把工具选择、prompt 来源、人工 QA、文件提交方式都写明。

### 6.3 README 与各 PR 的"前置条件"对不齐

| PR | README §依赖规则 | PR 文档 §前置条件 |
| --- | --- | --- |
| PR-04 | 串行 PR-03 → PR-04 | PR-04 写 "PR-01 完成" |
| PR-05 | 串行 PR-04 → PR-05 | PR-05 写 "PR-02 完成；PR-01 shell 布局稳定"（绕过 PR-03、PR-04） |
| PR-06 | 串行 PR-05 → PR-06 | PR-06 写 "PR-03、PR-04、PR-05 完成"（一致） |

PR-04、PR-05 的 §前置条件 与 README 不一致。要么 README 串行规则放松、要么 PR 文档收紧。建议统一为 README 的 strict 串行（README §1）。

### 6.4 资源数与 PR 任务分配未对账

PLAN.md §Resource Inventory 给出 538 总数，但每 PR 没说自己负责的子集是多少：

- PR-02 ≈ Round 1 ≈ ~30+ entries (HUD + UI chrome)
- PR-03 ≈ Round 7 部分 ≈ ~150 entries (装备 / item / affix / material)
- PR-05 ≈ Round 2-6 ≈ ~150 entries (tile / prop / VFX / actor / portrait)
- PR-06 ≈ Round 8-9 + 返修 ≈ ~200 entries (skill / status / quest)

总和应 ≈ 538。建议在 README 表格新增 "目标 manifest entry 数" 列，给执行者提供完成度判定标尺。

### 6.5 `phase4-v4-pr01` 上游分支锁定状态未声明

PR-04 §前置条件提"职业树语义已落地或作为上游合同处理"。当前 git log 显示 commit `df1337b3 Merge pull request #99 from … codex/phase4-v4-pr01-profession-tree-run-choice` 已合入 main——可以正式声明"已落地"。建议 PR-04 §前置条件改为："phase4-v4-pr01 已合入 main（commit df1337b3）"，去掉"或作为上游合同"二义。

### 6.6 `tree.*` 与 `icon.tree.*` 两套 key 是否在 v1 内合并

manifest 同时维护两套 tree key 是历史惯性。PR 文档没声明是否在 dark-v1 重构内合并、还是继续保留两套。建议在 PR-00 决断：保留则 §Mapping Spec 写明双 key 各自服务的 UI 区位；合并则 PR-04 或 PR-06 显式 deprecate `tree.*` 并迁移所有引用。

### 6.7 PR-07 "doc-vs-implementation 自审" 没说交付物

PR-07 §3.4 "补一次 `UI/PLAN.md`、`UI/ART_STYLE_BIBLE.md`、`UI/pr/*` 与实际实现的对照清单。" 但没说写到哪里——是新增 `UI/review/2026-XX-final-audit.md`？还是 inline 修订 PLAN？建议 §3 写明输出文件路径，与本评审报告同目录最佳。

---

## 7. 优先级建议

按"必须修 → 建议修 → 留作下轮"分三档：

### P0（必须在动 PR-00 实施前修文档）

1. **H1 / §2.1**：决定 styleTag 迁移策略并落到 PR-00；同步修改 `asset_pipeline_common.py` / `asset-lint.py` / `manifest-lint.py`。
2. **H2 / §2.2**：把 `sheetPlanLint` 接入 Gradle，并在 PR-00 §6 验证命令中声明。
3. **H3 / §1.1**：声明 8 职业的 dark-v1 覆盖范围；shadowblade/warden 要么进 PR-04/05/06，要么显式延后并写进 PLAN §Assumptions。
4. **§1.2**：合并 PLAN ↔ PR-05/06 的 sheet ID 命名，建立单一真源。
5. **§1.3**：HUD `gold/key` vs item `gold/key` 归属分清。
6. **§1.6**：补四态颜色 token 到 ART_STYLE_BIBLE 与 `UiDesignTokens` 改动清单。
7. **§3.6 / §6.7**：PR-07 atlas 阈值具体化、自审输出路径明确化。

### P1（PR-00 / PR-02 期间补文档）

1. §1.4 / §6.6：`tree.*` 与 `icon.tree.*` 双 key 处置。
2. §1.5：`icon.mutation.*` 归属、是否扩 `icon_status` category。
3. §2.3：4 个新脚本路径、单测、Gradle 接入。
4. §2.5：删除 PR-04/05/06 中冗余的 audioLint 列项。
5. §3.1：PR-00 增加"交付可运行 dry-run 脚本"任务。
6. §3.2：PR-04 临时 placeholder 暗色 icon 或推迟 golden baseline。
7. §3.3：PR-04 §前置条件补 PR-02。
8. §3.5：PR-06 §3 附 manifest key 切换全表。
9. §3.7：PR-05 补 §回滚边界。
10. §3.8：fallback / debug 资源进 Round 9 显式声明。
11. §6.1：cell 大小措辞修正。
12. §6.3：PR-04/05 §前置条件与 README 对齐。

### P2（PR 实施期顺手补）

1. §4 测试覆盖断层逐项。
2. §5.1 "更新白盒证据预期" 措辞收紧。
3. §5.2 父级 art-style-bible 同步说明。
4. §6.2 raw sheet 获取流程具体化。
5. §6.4 README 增"目标 manifest entry 数"列。
6. §6.5 PR-04 §前置条件锁定 commit。

---

## 8. 评审小结

文档体系骨架良好（PLAN ↔ ART_STYLE_BIBLE ↔ PR README ↔ 8 个 PR 的层级清晰，串行依赖明确，sheet 类型与映射 schema 设计严谨），但**最致命的三个隐患均集中在"管线接得通 / 范围对得齐"层面**：

1. styleTag 升级路径未交代 → 第一个资源 PR 即破 lint；
2. `sheet-plan.yaml` 没 lint 入口 → 单一真源失去强制力；
3. 8 职业 vs 文档 4–6 职业 → 玩家可见资源覆盖必有遗漏。

只要在 PR-00 阶段把 P0 七项修掉，整个 7-PR 串行链路就能避免 80% 的中途返工。建议把本报告 §7 的 P0/P1 清单作为 PR-00 的 checklist 直接挂进 `pr00` 文档 §3 实现任务里。

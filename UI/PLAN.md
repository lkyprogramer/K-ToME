# K-ToME 全仓库暗黑 UI/UX 与视觉资源重构最终方案

## Summary
将 K-ToME 的玩家可见界面和视觉资源统一升级为暗黑 Roguelike 风格：保留现有 Kotlin + libGDX + Tile 渲染框架可承载的结构，重做 UI 信息架构、装备/背包图标化、左侧导航栏、右侧角色面板、底部 HUD 与快捷键提示，并全量替换旧视觉资源，避免新 UI 被旧资源风格拖垮。

主 demo 参考：

- [UI-demo.png](./UI-demo.png)
- 已验证为 `1672 x 941` PNG；如果后续迁入 `docs/opt/ui-redesign/demo/`，必须继续使用 repo-relative 路径。
- 专项美术合同：[ART_STYLE_BIBLE.md](./ART_STYLE_BIBLE.md)
- 父级世界观美术合同：[docs/2026-03-13-art-style-bible.md](../docs/2026-03-13-art-style-bible.md)

Codex CLI 验证结论：

- 本机 `codex-cli 0.125.0` 支持 prompt 输入和 `--image <FILE>` 图片附件。
- 未发现直接 `prompt -> PNG` 的 CLI 生图子命令。
- 方案采用：Codex 交互式生成 raw sheet/demo，仓库脚本负责切分、校验、处理、注册 manifest。

## Art Style Authority
新增 [ART_STYLE_BIBLE.md](./ART_STYLE_BIBLE.md) 作为本方案的 UI/UX 与雪碧图专项风格合同。

权威关系：

1. `docs/2026-03-13-art-style-bible.md` 继续是 K-ToME 世界观、美术气质、材质、Tile 读图原则的父级合同。
2. `UI/ART_STYLE_BIBLE.md` 是本次暗黑 UI/UX 重构的专项合同，负责新 UI、雪碧图生成、切分映射、contact sheet 验收和 prompt 约束。
3. 新视觉纪元风格标签固定为 `ktome-dark-fantasy-sprite-ui-v1`。
4. 如果两者冲突，先保持父级世界观不变，再调整专项 UI 表达；不得用新 UI 需求反向破坏 K-ToME 的高幻想基调。
5. 后续所有 `sheet-plan.yaml`、生成 prompt、切分 report、manifest patch 都必须记录该 style tag。

## Precheck
影响范围：

- `client`：UI layout、TileRenderer 拆分、HUD/sidebar/inventory/equipment 展示。
- `client resources`：`visual-manifest.json` 指向新资源。
- `assets-src/image/specs`：新增雪碧图生成规格。
- `scripts` / `tools`：新增切图、contact sheet、manifest diff、资源完整性校验。

稳定合同：

- 第一阶段不改玩法规则、不改存档/replay/profile schema。
- 第一阶段不改 `VisualManifestEntry` schema，仍输出单 PNG 并写入 `rawOutputPath`。
- 不把规则状态复制进 client；client 只消费 snapshot 和 manifest。
- 图片本身不是资源映射真相；`sheet-plan.yaml` 才是 `sheet cell -> visualKey -> rawOutputPath` 的唯一权威。

## Resource Inventory
当前 manifest 需要覆盖的资源规模：

| Category | Count | Scope |
| --- | ---: | --- |
| `actor_sprite` | 57 | 玩家、职业、怪物、Boss |
| `portrait` | 26 | 职业、天赋树、区域图 |
| `icon` | 223 | 装备、怪物、职业、UI、affix、material |
| `icon_skill` | 118 | 技能与 talent |
| `icon_status` | 23 | 状态、mutation |
| `icon_quest` | 16 | 任务、区域、隐藏区域 |
| `icon_item` | 15 | 职业基础装备、药水 |
| `icon_damage_type` | 6 | 六类伤害 |
| `tile_*` | 23 | 地面、墙体、decal、telegraph |
| `prop_*` | 17 | 楼梯、门、祭坛、箱子、区域物件 |
| `debug` | 14 | missing、hidden、fallback |

## Sprite Sheet Plan
总共生成 **9 轮雪碧图**，约 **27 张核心 sheet**。每轮都产出：raw sheet、sheet plan、切分 PNG、contact sheet、manifest patch、QA report。

### Sheet Types
统一使用三类 sheet，禁止每轮自由发挥尺寸。

| Sheet Type | Canvas | Grid | Cell | 用途 |
| --- | ---: | ---: | ---: | --- |
| `icon-sheet` | `1024x1024` | `8x8` | `128x128` | UI 图标、装备、技能、状态、任务、小怪头像、紧凑 actor sprite |
| `large-sheet` | `1024x1024` | `4x4` | `256x256` | portrait、Boss、大型 actor、UI chrome/frame pieces |
| `tile-sheet` | `1024x1024` | `8x8` | `128x128` | ground、wall、prop、decal、VFX source |

生成约束：

1. 每个 cell 只允许一个主体，居中，禁止跨格、禁止文字、禁止编号、禁止水印。
2. icon/actor/prop cell 内保留约 `10-14px` 透明安全边距；portrait 保留 `16-24px`；tile ground/wall 必须完整填满 cell。
3. 同一 sheet 内必须保持同一视角、同一光源、同一描边粗细、同一像素密度。
4. 未使用 cell 标记为 `reserved`，切分脚本忽略；reserved cell 不进入 manifest。
5. 如果生成结果 cell 边界不清晰、主体串格、风格漂移或含文字，必须重生成，不靠人工猜裁。

### Mapping Spec
新增 `UI/sprite-sheets/sheet-plan.yaml` 作为唯一映射真源。图片只提供像素，不提供资源语义。

```yaml
sheets:
  - sheetId: r01-ui-hud-icons
    round: 1
    type: icon-sheet
    styleTag: ktome-dark-fantasy-sprite-ui-v1
    rawSheetPath: assets-src/image/raw/sheets/dark-v1/r01-ui-hud-icons.png
    outputRoot: client/src/main/resources/dark-v1/ui
    promptBase: Dark fantasy roguelike UI icon sheet, no text, transparent background
    grid:
      columns: 8
      rows: 8
      cellWidth: 128
      cellHeight: 128
    cells:
      - row: 0
        col: 0
        targetKey: ui.combat.action.icon
        category: icon
        outputName: dark-v1/ui/ui_combat_action_icon.png
        subject: crossed weapon and ember spark action-choice icon
      - row: 0
        col: 1
        targetKey: ui.combat.method.icon
        category: icon
        outputName: dark-v1/ui/ui_combat_method_icon.png
        subject: branching tactic glyph with two route strokes
      - row: 7
        col: 7
        reserved: true
        note: unused polish slot
```

硬规则：

1. `sheetId + row + col` 在全局唯一。
2. `targetKey` 必须等于 `visual-manifest.json` 中的 `key`，新增 UI chrome key 必须在同一 PR 增加 manifest entry。
3. `targetKey` 全局唯一；确实复用同一图时必须显式写 `aliasOf`，禁止隐式重复。
4. `outputName` 必须是 repo-relative path，并且最终必须与 manifest `rawOutputPath` 完全一致。
5. `category` 必须属于现有 asset pipeline 支持的 category，新增 `ui_frame` / `vfx_plate` 也必须进入 lint 白名单。
6. prompt 由脚本从 `sheet-plan.yaml` 生成，避免 prompt 顺序和切分映射出现第二真相。

### Prompt Generation Contract
每张 sheet 的最终 prompt 由 `generate_sheet_prompt.py` 生成，结构固定：

```text
Create a {canvasWidth}x{canvasHeight} transparent-background sprite sheet for K-ToME.
Style tag: ktome-dark-fantasy-sprite-ui-v1.
Grid: {columns} columns x {rows} rows, each cell is {cellWidth}x{cellHeight}.
Every cell contains exactly one centered subject, no text, no numbers, no labels, no watermark, no merged cells.
Visual style: dark fantasy roguelike, charcoal black panels, forged iron, worn stone, ember highlights, restrained cyan edge glow, readable at 32px.
Cell list:
{cellList}
Avoid: sci-fi HUD, neon cyberpunk, anime, chibi, glossy plastic, bright glassmorphism, modern icons, franchise symbols.
```

`cellList` 从 `cells[].subject` 生成：

```text
row 0 col 0: crossed weapon and ember spark action-choice icon.
row 0 col 1: branching tactic glyph with two route strokes.
row 0 col 2: focused target reticle over a worn stone tile.
```

### Slice & Verification Flow
新增脚本职责：

1. `generate_sheet_prompt.py`：从 `sheet-plan.yaml` 生成每张 sheet 的 prompt。
2. `slice_spritesheet.py`：按 `sheetId/grid/row/col` 切图，输出单 PNG。
3. `render_contact_sheet.py`：生成带 `row,col,targetKey` 标签的验收图。
4. `verify_sprite_sheet_map.py`：校验映射、路径、coverage、透明边距和 manifest 一致性。

验收规则：

1. `sheet-plan.yaml` 中所有非 reserved cell 都必须切出 PNG。
2. 切分后 PNG 的 alpha bbox 不能为空；icon/actor/prop 不得触碰 cell 边界；tile ground/wall 必须覆盖完整 cell。
3. 每个 `targetKey` 必须在 manifest 中存在，或在同一 PR 中新增 manifest entry。
4. manifest `rawOutputPath` 必须等于 `sheet-plan.yaml.outputName`。
5. contact sheet 必须人工确认：图像语义与标签一致、同一类别风格一致、无错位、无串格。
6. QA report 必须记录 `sheetId`、`rawSheetHash`、`targetKey`、`cellRect`、`cellHash`、`outputHash`、`qaStatus`。

### Sheet Inventory

| Round | Sheet Group | Content | Prompt 概要 |
| --- | --- | --- | --- |
| 1 | UI Chrome/HUD | 面板、边框、slot、tab、button、progress bar、空状态、combat UI 图标 | `Dark fantasy roguelike pixel UI sprite sheet, charcoal metal panels, subtle cyan edge glow, amber highlights, transparent background, no text` |
| 2 | Tiles | foundation/forest/mine/ruins/shadow 的 ground、wall、可平铺变体 | `Top-down dungeon tiles, cracked stone, mine floor, ruined masonry, shadow depths, seamless 32x32 pixel tiles, dark palette` |
| 3 | Props/VFX | oil/water/ice/fire/void、Boss warning sigil、楼梯、门、祭坛、箱子、furnace | `Dungeon prop and hazard sprite sheet, readable top-down icons, torchlit stone, magic sigils, interactable objects, consistent scale` |
| 4 | Actors | player、4 职业、怪物 faction、Boss | `Top-down roguelike character sprites, 32x32/48x48, strong silhouettes, dark fantasy armor and monsters, transparent background` |
| 5 | Bestiary Icons | 怪物小头像、Boss 图标、boss variant icon/visual | `Dark fantasy monster portrait icons, square game UI icons, distinct faction colors, no text, high contrast` |
| 6 | Portraits | 职业 portrait、talent tree portrait、区域/隐藏区域图 | `Painterly pixel portrait cards, moody dungeon lighting, heroic class portraits and mysterious zone thumbnails, unified framing` |
| 7 | Items | 基础装备、职业起始装备、药水、卷轴、unique、artifact、affix、material | `Dark fantasy inventory icon sheet, weapons armor relics potions materials, 64x64 slots, transparent background, cohesive lighting` |
| 8 | Skills | Vanguard/Templar/Rogue/Arcanist/Spellblade/Berserker 技能与 talent visual | `Roguelike ability icon sheet, elemental magic, shield tactics, rogue shadows, holy light, brutal melee, consistent icon grammar` |
| 9 | Status/Quest/Fallback | status、mutation、damage type、quest、zone、profession、tree、difficulty、missing/hidden、返修资源 | `Status and quest icon sheet, readable symbolic icons, curses buffs elemental damage keys, dark UI style, no letters or labels` |

完整核心 sheet 清单：

| Round | Sheet ID | Type | Capacity | 映射内容 |
| --- | --- | --- | ---: | --- |
| 1 | `r01-ui-chrome` | `large-sheet` | 16 | panel、sidebar、HUD、slot、tooltip、modal frame |
| 1 | `r01-ui-controls` | `icon-sheet` | 64 | tab、button、empty state、combat UI、lock/invalid/target |
| 1 | `r01-ui-hud-icons` | `icon-sheet` | 64 | hp、stamina、xp、gold、inventory、equipment、log markers |
| 2 | `r02-tiles-ground` | `tile-sheet` | 64 | `tile_ground` 与 biome ground 变体 |
| 2 | `r02-tiles-wall` | `tile-sheet` | 64 | `tile_wall` 与 biome wall 变体 |
| 2 | `r02-tiles-decal` | `tile-sheet` | 64 | terrain decal、zone effect、warning sigil |
| 3 | `r03-props-interactable` | `large-sheet` | 16 | stairs、gate、crate、altar、bonfire、reliquary |
| 3 | `r03-props-environment` | `large-sheet` | 16 | furnace、zone visual prop、环境物件 |
| 3 | `r03-vfx-telegraph` | `tile-sheet` | 64 | oil/water/ice/fire/void、Boss telegraph、overlay VFX |
| 4 | `r04-actors-player` | `large-sheet` | 16 | player、4 职业、unknown、职业变体 |
| 4 | `r04-actors-humanoid` | `icon-sheet` | 64 | bandit、orc、cultist、warded ruin |
| 4 | `r04-actors-monster` | `icon-sheet` | 64 | abyssal、crystal、forge、river、undead、beast |
| 4 | `r04-actors-boss` | `large-sheet` | 16 | ashgate、dungeon lord、molten giant、abyssal guardian |
| 5 | `r05-bestiary-humanoid-icons` | `icon-sheet` | 64 | humanoid monster icons |
| 5 | `r05-bestiary-creature-icons` | `icon-sheet` | 64 | beast、undead、abyssal、crystal、river icons |
| 5 | `r05-boss-icons` | `icon-sheet` | 64 | boss icons、boss variant visual/icon |
| 6 | `r06-portraits-classes` | `large-sheet` | 16 | 4 职业 portrait、职业候选返修格 |
| 6 | `r06-portraits-trees` | `large-sheet` | 16 | 12 个 talent tree portrait |
| 6 | `r06-portraits-zones` | `large-sheet` | 16 | zone、secret zone portrait |
| 7 | `r07-items-base` | `icon-sheet` | 64 | 基础装备、职业起始装备、药水、卷轴 |
| 7 | `r07-items-unique-artifact` | `icon-sheet` | 64 | unique、artifact |
| 7 | `r07-items-affix-material` | `icon-sheet` | 64 | affix、material |
| 8 | `r08-skills-vanguard-berserker` | `icon-sheet` | 64 | Vanguard、Berserker skills/talents |
| 8 | `r08-skills-templar-rogue` | `icon-sheet` | 64 | Templar、Rogue skills/talents |
| 8 | `r08-skills-arcanist-spellblade` | `icon-sheet` | 64 | Arcanist、Spellblade skills/talents |
| 9 | `r09-status-damage` | `icon-sheet` | 64 | status、mutation、damage type |
| 9 | `r09-quest-zone-profession` | `icon-sheet` | 64 | quest、zone icon、profession、tree、difficulty |
| 9 | `r09-fallback-polish` | `large-sheet` | 16 | missing、hidden、debug、返修资源 |

Round 9 专门用于返修低质量、风格不一致、识别度不足、透明背景异常或映射语义不清的资源。

### UI Key Additions
UI chrome 新增明确 key，避免 renderer 使用裸路径：

| Key | 用途 |
| --- | --- |
| `ui.frame.panel.body` | 通用面板主体 |
| `ui.frame.panel.corner_tl` / `tr` / `bl` / `br` | 面板角块 |
| `ui.frame.panel.edge_top` / `right` / `bottom` / `left` | 面板边 |
| `ui.frame.slot.empty` | 空 slot |
| `ui.frame.slot.equipped` | 已装备 slot |
| `ui.frame.slot.selected` | 当前选中 slot |
| `ui.frame.tooltip.body` | tooltip 背板 |
| `ui.frame.modal.body` | modal 背板 |
| `ui.hud.hp.icon` | 生命 icon |
| `ui.hud.stamina.icon` | 耐力 icon |
| `ui.hud.xp.icon` | 经验 icon |
| `ui.hud.gold.icon` | 金币 icon |

现有 item/skill/status/quest/actor/portrait 继续使用已有 manifest key。新 UI key 只在 `client` 渲染层消费，不进入 `core` / `game` 规则层。

## UI/UX Changes
- 中央地图保持主视图，但视觉上用暗色框架、地图阴影和边界材质增强质感。
- 左侧新增导航/区域栏：当前 dungeon、楼层、任务摘要、关键提示。
- 右侧改为玩家面板：角色状态、装备格、背包 grid、资源计数。
- 底部只保留一套 HUD：生命/耐力/经验、日志、快捷栏、键位提示。
- 删除重复状态栏；快捷键提示改为紧凑分组，不再挤压日志。
- 装备、技能、物品、状态全部图标化，文字只作为名称、数量、tooltip 或日志补充。
- 不引入 D-pad；当前阶段继续以键盘输入为主。

## Implementation Phases
1. **Design Baseline**
   固化 demo、[ART_STYLE_BIBLE.md](./ART_STYLE_BIBLE.md)、颜色/token、UI 布局草图、资源替换清单。

2. **Asset Pipeline**
   新增 `UI/sprite-sheets/sheet-plan.yaml` schema、prompt 生成脚本、切图脚本、contact sheet、manifest diff、缺失 key report。先提交 1 张 `r01-ui-hud-icons` dry-run prompt，跑通 raw sheet -> 切分 -> manifest -> client golden 后，再批量进入 27 张 sheet。

3. **Core Visual Replacement**
   完成 Round 1-3，替换 UI chrome、tile、prop、VFX，让主界面先具备新质感。

4. **Actor/Portrait Replacement**
   完成 Round 4-6，替换角色、怪物、Boss、职业 portrait、区域图。

5. **Icon System Replacement**
   完成 Round 7-9，替换装备、技能、状态、任务、damage type、fallback。

6. **Client UI Refactor**
   拆分 `TileRenderer`：map、left rail、right panel、bottom HUD、tooltip/modal；引入 `UiDesignTokens` 和 grid/slot 组件。

7. **Polish & Regression**
   更新 golden screenshot、资源 lint baseline、contact sheet QA report、白盒验收记录；只有在单 PNG 加载成本被测出问题时，再进入 atlas/region manifest 方案。

## Asset Pipeline Deliverables
必须新增或更新以下文件族：

| Path | Purpose |
| --- | --- |
| `UI/ART_STYLE_BIBLE.md` | 暗黑 UI/UX 专项美术合同 |
| `UI/sprite-sheets/sheet-plan.yaml` | sheet/cell/visualKey/outputName 映射真源 |
| `assets-src/image/raw/sheets/dark-v1/*.png` | Codex 交互式生成的 raw sheet |
| `assets-src/image/contact-sheets/dark-v1/*.png` | 带 row/col/key 标签的验收图 |
| `assets-src/image/manifests/dark-v1-sprite-report.jsonl` | 切分与 QA report |
| `client/src/main/resources/dark-v1/**/*.png` | 切分和后处理后的 runtime PNG |
| `client/src/main/resources/manifests/visual-manifest.json` | 更新 `rawOutputPath` 到新资源 |

自动化必须覆盖：

1. sheet plan schema 校验。
2. raw sheet 尺寸和 grid 校验。
3. row/col 越界和重复校验。
4. `targetKey` 覆盖率校验。
5. `outputName` 与 manifest `rawOutputPath` 一致性校验。
6. alpha bbox、透明边距、空 cell、tile full-coverage 校验。
7. contact sheet 人工验收状态校验。

## Test Plan
- 环境预备：`source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env`
- UI smoke：`./gradlew :client:clientSmoke :client:goldenScreenshot`
- 资源合同：`./gradlew assetLint styleLint manifestLint`
- 治理检查：`./gradlew maintainabilityLint`
- 总入口：`./gradlew verifyChanged`
- 若改 Gradle/依赖/bootstrap：`./scripts/verify-bootstrap.sh`
- 白盒检查：桌面端确认左侧栏、地图、右侧装备/背包、底部 HUD、快捷键提示、中文文本、fallback、窗口缩放均正常。

## Assumptions
- “全量替换”限定为视觉资源和 UI 表现，不修改核心规则、数值、存档、replay、profile schema。
- 旧资源迁移期可保留作回滚参考，但新 manifest 默认只指向新纪元资源。
- Codex CLI 未来若支持直接图片输出，只替换 raw sheet 获取步骤，不改变切分、manifest、验证流程。

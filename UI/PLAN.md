# K-ToME 全仓库暗黑 UI/UX 与视觉资源重构最终方案

## Summary
将 K-ToME 的玩家可见界面和视觉资源统一升级为暗黑 Roguelike 风格：保留现有 Kotlin + libGDX + Tile 渲染框架可承载的结构，重做 UI 信息架构、装备/背包图标化、左侧导航栏、右侧角色面板、底部 HUD 与快捷键提示，并全量替换旧视觉资源，避免新 UI 被旧资源风格拖垮。

主 demo 参考：

- [UI-demo-new.png](./UI-demo-new.png)
- 当前 PR-02-1 shell 验收只以 `UI-demo-new.png` 为准；`UI-demo.png` 仅保留为历史参考。
- 已验证为 `1672 x 941` PNG；如果后续迁入 `docs/opt/ui-redesign/demo/`，必须继续使用 repo-relative 路径。
- 专项美术合同：[ART_STYLE_BIBLE.md](./ART_STYLE_BIBLE.md)
- 父级世界观美术合同：[docs/2026-03-13-art-style-bible.md](../docs/2026-03-13-art-style-bible.md)

Codex CLI 生图验证结论：

- `codex exec "生成一个上海一日旅游海报图片" --skip-git-repo-check` 已验证会把图片写入 Codex CLI 的 generated-images 目录。
- 仓库固定通过 `scripts/codex-generate-image.py` 调用 Codex CLI，按目录修改时间选取 `<codex-generated-images-dir>` 下最新文件夹中的最新图片，并复制到调用方指定的 repo-relative `rawSheetPath`。
- `<codex-generated-images-dir>` 只是 transient source，不进入 manifest、coverage artifact 或 PR 合同；正式合同只认 `assets-src/image/raw/sheets/dark-v1/{sheetId}.png` 这类 repo-relative 输出路径。
- 方案采用：脚本生成 prompt，`scripts/codex-generate-image.py` 调用 Codex CLI 生成并落位 raw sheet，仓库脚本负责切分、校验、处理、注册 manifest。

## Art Style Authority
新增 [ART_STYLE_BIBLE.md](./ART_STYLE_BIBLE.md) 作为本方案的 UI/UX 与雪碧图专项风格合同。

权威关系：

1. `docs/2026-03-13-art-style-bible.md` 继续是 K-ToME 世界观、美术气质、材质、Tile 读图原则的父级合同。
2. `UI/ART_STYLE_BIBLE.md` 是本次暗黑 UI/UX 重构的专项合同，负责新 UI、雪碧图生成、切分映射、contact sheet 验收和 prompt 约束。
3. 新视觉纪元风格标签固定为 `ktome-dark-fantasy-sprite-ui-v1`。
4. `ktome-dark-fantasy-sprite-ui-v1` 是父级 `ktome-middle-fantasy-painterly-tile-v1` 的暗黑 UI/资源子纪元，不是世界观重置。
5. 如果两者冲突，先保持父级世界观不变，再调整专项 UI 表达；不得用新 UI 需求反向破坏 K-ToME 的高幻想基调。
6. 后续所有 `sheet-plan.yaml`、生成 prompt、切分 report、manifest patch 都必须记录该 style tag。

## Precheck
影响范围：

- `client`：UI layout、TileRenderer 拆分、HUD/sidebar/inventory/equipment 展示。
- `client resources`：先更新 canonical manifest，再同步 runtime manifest 指向新资源。
- `UI/sprite-sheets`：新增暗黑雪碧图生成规格和映射真源；不复用旧 `assets-src/image/specs/*` 作为新 sheet-plan 真源。
- `scripts` / `tools`：新增切图、contact sheet、manifest diff、资源完整性校验。

稳定合同：

- 第一阶段不改玩法规则、不改存档/replay/profile schema。
- 第一阶段不引入 atlas / region manifest schema，仍输出单 PNG 并写入 `rawOutputPath`；`VisualManifestEntry` 字段合同以 [pr/README.md](./pr/README.md#visual-manifest-field-policy) 为准，不在 canonical / runtime / fixture / sample-pack manifest 中保留 ASCII manifest 字段。旧 v1 manifest decode-only 剥离规则只用于避免历史 content pack 因已删除字段启动失败，不是新的 authoring 合同。
- 不把规则状态复制进 client；client 只消费 snapshot 和 manifest。
- 图片本身不是资源映射真相；`sheet-plan.yaml` 才是 `sheet cell -> visualKey -> rawOutputPath` 的唯一权威。

## Pipeline Gate Strategy
PR-00 必须先把暗黑雪碧图管线接入可执行 gate，后续资源 PR 才能开始。当前仓库已有 `assetLint / styleLint / manifestLint` 仍以旧 `assets-src/image/specs/*` 和 `ktome-middle-fantasy-painterly-tile-v1` 为主，不能把它们误认为已经覆盖 `UI/sprite-sheets/sheet-plan.yaml`。

第一阶段采用 **multi-epoch sidecar validation**，不引入 atlas / region manifest schema：

1. `VisualManifest.styleTag` 在混合资源阶段继续保持现有 canonical manifest 兼容语义。
2. `ktome-dark-fantasy-sprite-ui-v1` 记录在 `sheet-plan.yaml`、prompt、切分 report、contact sheet、manifest coverage artifact 中。
3. PR-00 新增 root gate：`darkKeyRegistryLint`、`darkSpriteSheetLint`、`spriteSheetMapLint`、`darkManifestCoverageLint`。
4. `darkSpriteSheetLint` 校验 `sheet-plan.yaml` schema、sheet type、grid、reserved/alias、styleTag 和 repo-relative path。
5. `spriteSheetMapLint` 校验 raw sheet、切分 PNG、contact sheet、QA JSONL、alpha bbox、cell hash、`targetKey -> outputName`。
6. `darkKeyRegistryLint` 校验 key registry 的 owner、fallback、consumer、test、alias 和 sheet ownership 一致性。
7. `darkManifestCoverageLint` 校验 dark-v1 player-visible key coverage、allowed fallback、old-style key residue 和 manifest `rawOutputPath`。
8. PR-02 之后所有资源 PR 必须同时跑旧资源 lint 和 dark-v1 gate：`assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint`，并按当前 PR 显式传 `darkManifestCoverageLint` mode。
9. PR-07 基于覆盖率与性能证据决定是否另开 manifest epoch PR，把顶层 manifest style 表达升级为 entry-level epoch 或 atlas / region manifest schema；不得在资源替换 PR 中半途扩大 manifest 结构。

Manifest 权威关系固定为 canonical-first：

1. `assets-src/image/manifests/phase2-visual-manifest.json` 是 visual manifest 真源。
2. `client/src/main/resources/manifests/visual-manifest.json` 是 `syncPhase2Manifests` 生成的 runtime 副本，不能手改后当作权威。
3. 所有 `targetKey -> outputName -> rawOutputPath` 变更必须先进入 canonical manifest，再由同步任务生成 runtime manifest。
4. 旧 `manifestLint` 继续校验 canonical/runtime 一致性；dark-v1 覆盖完整性由 `darkManifestCoverageLint` 和 coverage artifact 负责。
5. PR-00 必须让 `verifyChanged` 在命中 `UI/sprite-sheets/**`、`assets-src/image/raw/sheets/dark-v1/**`、`client/src/main/resources/dark-v1/**`、canonical/runtime visual manifest、dark-v1 report/coverage artifact 时触发 dark-v1 gate。

`darkManifestCoverageLint` 分阶段模式：

| Mode | 使用阶段 | 严格度 |
| --- | --- | --- |
| `pr00-dry-run` | PR-00 fixture 与脚本接线 | 允许没有正式 PNG 和生产 manifest 覆盖，但必须输出可解释的 missing/pending，不允许静默通过 |
| `owner-scope` | PR-02、PR-02-1、PR-03、PR-05 | 只强制当前 owner PR 的 sheetId / key family；owner scope 外允许 pending，但必须列入 artifact |
| `final-full` | PR-06、PR-07 | 玩家可见主路径全量收口，`oldStylePlayerVisibleKeys=[]` 且 `pendingOrRejectedPlayerVisibleCells=[]` |

`dark-v1-manifest-coverage.json` schema 以 PR-00 文档为权威。总方案只固定最低要求：必须记录 `scopeMode`、`ownerPr`、`expectedKeySetSource`、`strictOldStyleResidue`，并按 mode 输出 owner-scope 或 final-full 扩展字段，避免同名 gate 在不同 PR 里语义漂移。

`sheet-plan.yaml` 字段归属固定如下：

| 字段类别 | 字段 |
| --- | --- |
| Plan 输入 | `sheetId / round / type / styleTag / rawSheetPath / outputRoot / promptBase / grid / cells[].row / col / targetKey / category / outputName / subject / reserved / aliasOf` |
| QA 输出 | `qaStatus / rawSheetHash / cellRect / cellHash / outputHash / reviewer / reviewedAt / rejectionReason` |
| 派生策略 | `size / anchor / safeMarginPx` 只能来自 category policy 或 cell override，不替代 sheet-level `grid` |

QA 状态不得写回 plan 输入当作真相；计划真相与验收结果必须分离。

`ui_frame` 与 `vfx_plate` 已在现有 `scripts/asset_pipeline_common.py` category 白名单和 `scripts/process_assets.py` 后处理策略中存在。PR-00 不重新定义 category，只审计这两个 category 的 canvas/padding 是否适配 dark-v1 UI chrome 与 telegraph；如需调整，必须同步更新脚本、lint 和相关 focused tests。

### Profession Coverage Scope

当前数据真源包含 8 个职业：

| Scope | Professions | Dark-v1 资源要求 |
| --- | --- | --- |
| Release playable | `vanguard`, `arcanist`, `rogue`, `templar` | actor、profession icon、tree icon、tree portrait、skill/talent icon 全覆盖 |
| Dev playable / report-only | `berserker`, `spellblade` | actor/portrait 可继承基础职业但必须 dark-v1；skill/talent icon 全覆盖 |
| Frozen excluded | `shadowblade`, `warden` | 不进入 PR-04 starter/learnable/breakpoint 分母；不生成完整 skill/talent sheet；锁定态职业卡如在 UI 可见，必须使用 dark-v1 locked/fallback 表达并写入 coverage artifact |

`shadowblade / warden` 的排除原因必须写入 `dark-v1-manifest-coverage.json.allowedCoverageExclusions`，不得作为 silent missing key 处理。

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
总共生成 **9 轮雪碧图**，约 **29 张核心 sheet**。每轮都产出：raw sheet、sheet plan、切分 PNG、contact sheet、manifest patch、QA report。

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
  - sheetId: r01-ui-controls
    round: 1
    type: icon-sheet
    styleTag: ktome-dark-fantasy-sprite-ui-v1
    rawSheetPath: assets-src/image/raw/sheets/dark-v1/r01-ui-controls.png
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
2. `targetKey` 必须等于 canonical `assets-src/image/manifests/phase2-visual-manifest.json` 中的 `key`；runtime manifest 只能由同步任务生成。
3. `targetKey` 全局唯一；确实复用同一图时必须显式写 `aliasOf`，禁止隐式重复。
4. `outputName` 必须是 repo-relative path，并且最终必须与 canonical/runtime manifest 的 `rawOutputPath` 完全一致。
5. `category` 必须属于现有 asset pipeline 支持的 category，新增 `ui_frame` / `vfx_plate` 也必须进入 lint 白名单。
6. prompt 由脚本从 `sheet-plan.yaml` 生成，避免 prompt 顺序和切分映射出现第二真相。
7. 每个非 reserved `targetKey` 必须在 `UI/sprite-sheets/key-registry.yaml` 中有记录；registry 是 owner/fallback/consumer/test/alias 真源。
8. `sheetId`、`targetKey` prefix、`category` 必须与 [pr/README.md](./pr/README.md) 的 SheetId Ownership 和 key registry 匹配。

### Prompt Generation Contract
每张 sheet 的最终 prompt 由 `generate_sheet_prompt.py` 生成，结构固定。raw sheet 通过 `scripts/codex-generate-image.py` 调用 Codex CLI 生成；该脚本只从 `<codex-generated-images-dir>` 读取最新输出并复制到 `rawSheetPath`，不把本机生成目录写入长期合同。

Prompt 输出目录固定为 `UI/sprite-sheets/prompts/dark-v1/`。文件名由脚本按 `round + sheet order + sheetId` 生成，格式固定：

```text
001-r01-ui-chrome.prompt.txt
002-r01-ui-controls.prompt.txt
003-r01-ui-hud-icons.prompt.txt
...
028-r09-fallback-debug.prompt.txt
029-r09-rejected-polish.prompt.txt
```

prompt 文件头必须包含脚本可执行的信息：

```text
Prompt ID: 001-r01-ui-chrome
Sheet ID: r01-ui-chrome
Expected output file: assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png
Canvas: 1024x1024
Grid: 4 columns x 4 rows
Cell: 256x256
Style tag: ktome-dark-fantasy-sprite-ui-v1
```

生成 raw sheet 时，只允许通过脚本把 Codex CLI 最新输出复制到 `rawSheetPath` 指定文件名。例如 `001-r01-ui-chrome.prompt.txt` 对应 `assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png`：

```bash
scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/001-r01-ui-chrome.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png \
  --overwrite
```

后续切分、contact sheet、QA 和 manifest 校验都由脚本执行。

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

1. `generate_sheet_prompt.py`：从 `sheet-plan.yaml` 生成编号 prompt 文件与 prompt index。
2. `slice_spritesheet.py`：按 `sheetId/grid/row/col` 切图，输出单 PNG。
3. `render_contact_sheet.py`：生成带 `row,col,targetKey` 标签的验收图。
4. `verify_sprite_sheet_map.py`：校验映射、路径、coverage、透明边距和 manifest 一致性。

生成/验证边界：

1. 脚本生成 prompt：`sheet-plan.yaml -> UI/sprite-sheets/prompts/dark-v1/*.prompt.txt`。
2. 脚本调用 Codex CLI：`scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --overwrite`。
3. 脚本落位 raw sheet：文件必须放到 `rawSheetPath`，文件名必须等于 `{sheetId}.png`。
4. 脚本验证 raw sheet：尺寸、grid、hash、文件名、repo-relative path 和 prompt index 对齐。
5. 脚本切分 PNG、生成 contact sheet、输出 QA report。
6. 人工只在 contact sheet 上判断语义和风格是否可接受；不得通过改 `row/col` 掩盖生成错位。

验收规则：

1. `sheet-plan.yaml` 中所有非 reserved cell 都必须切出 PNG。
2. 切分后 PNG 的 alpha bbox 不能为空；icon/actor/prop 不得触碰 cell 边界；tile ground/wall 必须覆盖完整 cell。
3. 每个 `targetKey` 必须在 manifest 中存在，或在同一 PR 中新增 manifest entry。
4. manifest `rawOutputPath` 必须等于 `sheet-plan.yaml.outputName`。
5. contact sheet 必须人工确认：图像语义与标签一致、同一类别风格一致、无错位、无串格。
6. QA report 必须记录 `promptId`、`promptPath`、`sheetId`、`rawSheetPath`、`rawSheetHash`、`targetKey`、`cellRect`、`cellHash`、`outputHash`、`qaStatus`。

### Sheet Inventory

| Round | Sheet Group | Content | Prompt 概要 |
| --- | --- | --- | --- |
| 1 | UI Chrome/HUD | 面板、边框、slot、tab、button、progress bar、空状态、combat UI 图标 | `Dark fantasy roguelike pixel UI sprite sheet, charcoal metal panels, subtle cyan edge glow, amber highlights, transparent background, no text` |
| 2 | Tiles | foundation/forest/mine/ruins/shadow 的 ground、wall、可平铺变体 | `Top-down dungeon tiles, cracked stone, mine floor, ruined masonry, shadow depths, readable when displayed at 32px, source cells remain 128x128, dark palette` |
| 3 | Props/VFX | oil/water/ice/fire/void、Boss warning sigil、楼梯、门、祭坛、箱子、furnace | `Dungeon prop and hazard sprite sheet, readable top-down icons, torchlit stone, magic sigils, interactable objects, consistent scale` |
| 4 | Actors | player、release/dev playable 职业、怪物 faction、Boss | `Top-down roguelike character sprites, readable silhouettes, dark fantasy armor and monsters, transparent background` |
| 5 | Bestiary Icons | 怪物小头像、Boss 图标、boss variant icon | `Dark fantasy monster portrait icons, square game UI icons, distinct faction colors, no text, high contrast` |
| 6 | Portraits | 职业 portrait、talent tree portrait、区域/隐藏区域图 | `Painterly pixel portrait cards, moody dungeon lighting, heroic class portraits and mysterious zone thumbnails, unified framing` |
| 7 | Items | 基础装备、职业起始装备、药水、卷轴、unique、artifact、affix、material | `Dark fantasy inventory icon sheet, weapons armor relics potions materials, readable in 48-64px UI slots, source cells remain 128x128, transparent background, cohesive lighting` |
| 8 | Skills | Vanguard/Templar/Rogue/Arcanist/Spellblade/Berserker 技能与 talent visual | `Roguelike ability icon sheet, elemental magic, shield tactics, rogue shadows, holy light, brutal melee, consistent icon grammar` |
| 9 | Status/Quest/Fallback | status、mutation、damage type、quest、zone、profession、tree、difficulty、missing/hidden、返修资源 | `Status and quest icon sheet, readable symbolic icons, curses buffs elemental damage keys, dark UI style, no letters or labels` |

完整核心 sheet 清单：

| Round | Sheet ID | Type | Capacity | 映射内容 |
| --- | --- | --- | ---: | --- |
| 1 | `r01-ui-chrome` | `large-sheet` | 16 | panel、sidebar、HUD、slot、tooltip、modal frame |
| 1 | `r01-ui-controls` | `icon-sheet` | 64 | tab、button、empty state、combat UI、lock/invalid/target |
| 1 | `r01-ui-hud-icons` | `icon-sheet` | 64 | hp、stamina、xp、gold、inventory、equipment、log markers |
| 1B | `r01b-ui-shell-chrome` | `large-sheet` | 16 | PR-02-1 必须生成的 shell scaffold：outer/map/nav/bottom frames、map stage backdrop、right divider、hero placeholder、nav rail icons |
| 2 | `r02-tiles-ground` | `tile-sheet` | 64 | `tile_ground` 与 biome ground 变体 |
| 2 | `r02-tiles-wall` | `tile-sheet` | 64 | `tile_wall` 与 biome wall 变体 |
| 2 | `r02-tiles-decal` | `tile-sheet` | 64 | tile decal、zone floor effect、warning sigil |
| 3 | `r03-props-interactable` | `large-sheet` | 16 | stairs、gate、crate、altar、bonfire、reliquary |
| 3 | `r03-props-environment` | `large-sheet` | 16 | furnace、biome ambience prop、环境物件 |
| 3 | `r03-vfx-telegraph` | `tile-sheet` | 64 | oil/water/ice/fire/void、Boss telegraph、overlay VFX |
| 4 | `r04-actors-player` | `large-sheet` | 16 | player、release/dev playable 职业、unknown、锁定态职业占位 |
| 4 | `r04-actors-humanoid` | `icon-sheet` | 64 | bandit、orc、cultist、warded ruin |
| 4 | `r04-actors-monster` | `icon-sheet` | 64 | abyssal、crystal、forge、river、undead、beast |
| 4 | `r04-actors-boss` | `large-sheet` | 16 | ashgate、dungeon lord、molten giant、abyssal guardian |
| 5 | `r05-bestiary-humanoid-icons` | `icon-sheet` | 64 | humanoid monster icons |
| 5 | `r05-bestiary-creature-icons` | `icon-sheet` | 64 | beast、undead、abyssal、crystal、river icons |
| 5 | `r05-boss-icons` | `icon-sheet` | 64 | boss icons、boss variant icon |
| 6 | `r06-portraits-classes` | `large-sheet` | 16 | release/dev playable 职业 portrait、锁定态职业占位、返修格 |
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
| 9 | `r09-fallback-debug` | `icon-sheet` | 64 | missing、hidden、debug、fallback、locked/placeholder |
| 9 | `r09-rejected-polish` | `large-sheet` | 16 | PR-03/05/06 rejected cell 与高风险返修资源 |

Round 9 专门用于 resource fallback / debug resource 收口与返修。`r09-status-damage` 必须同时区分 status / mutation / damage type 三套 visual grammar；actor status runtime/schema 不得复用 `icon.skill.*`，来自技能的 buff/debuff 也必须落到 dedicated `icon.status.*` key。`r09-fallback-debug` 先覆盖 current manifest 中所有 `missing_visual`、hidden、resource-debug、fallback、locked/placeholder 相关 key；不包含 client ASCII renderer、ASCII manifest 字段或 ASCII 可玩/调试路径，这些已从 client 合同删除。`r09-rejected-polish` 只承接已写入 coverage artifact 的 rejected cell，避免 fallback sheet 容量被返修资源挤占。任一 sheet 命中两轮 rejection 或两次以上 player-visible reject 时，必须按 PR-06 rework PR 合同先拆资源修复 PR，再允许 PR-06 close。

PR-06 的 quest summary 最小路径是 generic `icon.quest.objective_marker`。typed quest icon mapping（例如 activate / progress / advance / complete 或 key/封印专用 icon）命名为 `UI07-quest-typed-icon-mapping` 后续项；它不能在 PR-07 中静默改变 `RenderSnapshot`、quest schema 或 `WorldProgress`，必须作为单独 public contract change 处理。quest `complete` 的 transient accent 属于 PR-06 quest row tone 合同；若 PR-06 未落地，必须显式记录 `UI07-quest-complete-accent` outstanding finding，不能由 PR-07 final polish 静默吸收。

Round-3 review 后新增的命名后续项统一记录如下，避免 PR-07 静默接管 PR-06 owner：`UI07-telegraph-actor-anchored-indicator`、`UI07-quest-activate-accent`、`UI07-objective-token-emission-audit`、`UI07-status-fold-expand`、`UI07-profession-icon-character-sheet`、`UI07-profession-icon-save-slot`、`UI07-profession-icon-death-summary`、`UI07-frozen-profession-cross-surface-audit`、`UI07-dev-playable-banner`、`UI07-content-pack-style-certification`、`UI07-third-party-fallback-key-namespace`、`UI07-color-blind-runtime-toggle`、`UI07-profession-onboarding-hint`、`UI07-status-tooltip-detail`、`UI07-replay-frozen-profession`、`UI-FOLLOWUP-race-icon-family-contract`。这些项不得改变 PR-06 的 final-full 分母，只能作为后续有 owner 的 contract/implementation work；race icon 当前不得借 `icon.profession.*` 或 `icon.monster.*` 计入 profession/monster coverage。

容量矩阵是 prompt index、coverage artifact 和生图批次的单一总量真源：

| Sheet Type | Count | Capacity Each | Total Cells |
| --- | ---: | ---: | ---: |
| `large-sheet` | 9 | 16 | 144 |
| `icon-sheet` | 16 | 64 | 1024 |
| `tile-sheet` | 4 | 64 | 256 |
| Total | 29 | - | 1424 |

当前 manifest inventory 约 538 个资源，理论利用率约 37.8%。容量只是生产余量，不代表可以省略 key registry；每个非 reserved cell 仍必须有 `targetKey / ownerPr / category / consumerTest`。

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
| `ui.hud.key.icon` | 钥匙/任务 key 计数 icon |
| `ui.hud.warning.icon` | HUD 警告/危险提示 icon |
| `ui.hud.quest_marker.icon` | HUD 任务提示 marker |
| `ui.hud.log_marker.icon` | HUD 日志提示 marker |
| `ui.screen.home.title_frame` | 首页标题/header frame |
| `ui.screen.home.action_frame` | 首页主操作栈 frame |
| `ui.screen.validation.badge` | 验证模式标识 marker |
| `ui.screen.outcome.victory_marker` | 胜利结算 marker |
| `ui.screen.outcome.defeat_marker` | 失败结算 marker |
| `ui.screen.error.marker` | error / asset failure marker |
| `ui.screen.loading.marker` | loading marker |
| `ui.control.back.icon` | 返回动作 icon |
| `ui.control.confirm.icon` | 确认/开始动作 icon |
| `ui.control.copy.icon` | 复制详情 icon |
| `ui.control.backpack.icon` | 背包入口 icon |
| `ui.control.equipment.icon` | 装备面板入口 icon |
| `ui.shop.offer.frame` | shop offer card frame，默认 alias 到 PR-02 panel frame |
| `ui.shop.price.affordable` | 可购买价格 marker |
| `ui.shop.price.unaffordable` | 不可购买价格 marker |
| `ui.shop.offer.disabled` | deferred marker；只有存在 typed disabled source 的 PR 才能生成，PR-03 默认不生成 |
| `ui.shop.inscription.marker` | 铭文 offer 类型 marker |
| `ui.shop.replacement.slot_marker` | 铭文满槽替换槽位 marker |
| `ui.combat.action.icon` | 战斗动作选择 icon |
| `ui.combat.method.icon` | 战斗方式选择 icon |
| `ui.combat.target.icon` | 战斗目标选择 icon |
| `ui.combat.lock.icon` | 战斗锁定/不可变更 icon |
| `ui.combat.invalid.icon` | 战斗无效选择 icon |
| `ui.state.locked.icon` | 职业树锁定状态 glyph |
| `ui.state.learnable.icon` | 职业树可学习状态 glyph |
| `ui.state.active.icon` | 职业树已激活状态 glyph |
| `ui.state.reserve.icon` | 职业树 reserve 状态 glyph |

本表是 PR-02 初始 key 摘要，不是 registry 真源；正式 key inventory 以 PR-00 交付的 `UI/sprite-sheets/key-registry.yaml` 为准。现有 item/skill/status/quest/actor/portrait 继续使用已有 manifest key。新 UI key 只在 `client` 渲染层消费，不进入 `core` / `game` 规则层。

## UI/UX Target
最终 UI 形态：

1. 首页/主菜单是玩家第一屏，必须与局内 UI 使用同一暗黑 token、同一焦点态、同一禁用态和同一键盘优先交互语义；不能保留旧风格“标题 + 文本菜单”孤岛。
2. 验证模式入口和 `ValidationSetupScreen` 必须纳入正式 UI 改造。验证模式可以标记为 validation/debug flow，但视觉、布局、locale、焦点和返回路径必须与正式 UI 统一。
3. 中央地图保持主视图，但使用暗色框架、地图阴影和边界材质增强质感。
4. 左侧新增 icon-first 导航栏：compass/map、bag/equipment、scroll/log、book/talent、settings/accessibility 作为主视觉槽位；当前 dungeon、楼层、任务摘要、关键提示只能作为 compact hint 或 tooltip。
5. 右侧改为玩家面板：角色状态、装备格、背包 grid、资源计数。
6. 底部只保留一套 HUD：生命/耐力/经验、日志、快捷栏、键位提示。
7. 删除重复状态栏；快捷键提示改为紧凑分组，不再挤压日志。
8. 装备、技能、物品、状态全部图标化，文字只作为名称、数量、tooltip 或日志补充。
9. 铭文商店 / shop 是装备、背包、物品 UX family 的一部分，必须统一为暗黑 offer card、buy/sell 双列、价格/禁用态、满槽替换 modal、空态和 tooltip。
10. 职业树 UI 使用暗黑侧栏/面板化表达，继续消费正在开发的 `TalentSidebarPresenter`、`TalentTreeNodeSnapshot`、`DescriptionPresenter` 和 `ACTIVE_TALENT_SLOT_CHOICE` 合同。
11. 胜利/失败结算、loading、runtime error、独立 `UiErrorScreen`、设置/无障碍、world route、stat assign、reward/frontstage、Look/Inspect 等玩家可见 surface 必须进入最终覆盖矩阵；不能只因为它们不是局内常驻 HUD 就跳过。
12. Tile dark UI 是唯一正式 client 渲染路径；不再保留 ASCII fallback/debug renderer。
13. 不引入 D-pad；当前阶段继续以键盘输入为主。

全量 UI 面覆盖清单以 [pr/screen-coverage-matrix.md](./pr/screen-coverage-matrix.md) 为审计入口。PR-07 关闭前，矩阵中所有 Required/Conditional 项必须有 owner PR、focused test 或 golden/manual evidence、packaged app 或 debug client 白盒记录。

## PR-Level Development Documents
本文件只保留总方案、风格合同、资源合同和公共验证纪律；具体执行拆分落到 [pr/README.md](./pr/README.md) 与各 PR 文档。

| PR | 文档 | 工作量 | 目标 |
| --- | --- | --- | --- |
| `dark-uiux-pr00` | [Style And Pipeline Contract](./pr/dark-uiux-pr00-style-and-pipeline-contract.md) | M | 冻结风格合同、sheet schema、prompt/切分/验收流程 |
| `dark-uiux-pr01` | [Client Shell Layout](./pr/dark-uiux-pr01-client-shell-layout.md) | L | 首页/主菜单、验证入口、三栏 + 底部 HUD 框架、token、renderer 拆分 |
| `dark-uiux-pr01-1` | [Client Viewport Renderer And Overlay Architecture](./pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md) | XL | 玩家居中 viewport、TileRenderer orchestration 拆分、tooltip/modal overlay layer |
| `dark-uiux-pr02` | [UI Chrome Sprite Pilot](./pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md) | L | 跑通 UI chrome/HUD/standalone screen chrome 第一批 sheet 到 manifest/golden |
| `dark-uiux-pr02-1` | [Demo Shell Foundation](./pr/dark-uiux-pr02-1-demo-shell-foundation.md) | XL | 基于 PR-02 chrome 搭建 demo-like 主 shell：icon rail、dominant map stage、right grid scaffold、bottom hero/action/log deck |
| `dark-uiux-pr03` | [Equipment Inventory Items](./pr/dark-uiux-pr03-equipment-inventory-items.md) | L | 装备/背包 grid、item icon、铭文商店、quality、空态/tooltip |
| `dark-uiux-pr04` | [Profession Tree UI](./pr/dark-uiux-pr04-profession-tree-ui.md) | M | 职业树 dark UI、节点状态、预览、主动槽 modal |
| `dark-uiux-pr05` | [Map Actor Portrait Replacement](./pr/dark-uiux-pr05-map-actor-portrait-replacement.md) | XL | Tile、prop、VFX、actor、portrait 统一替换 |
| `dark-uiux-pr06` | [Skills Status Quest Full Manifest](./pr/dark-uiux-pr06-skills-status-quest-full-manifest.md) | XL | 技能、状态、任务、fallback、全 manifest 收口 |
| `dark-uiux-pr07` | [Golden Whitebox Polish](./pr/dark-uiux-pr07-golden-whitebox-polish.md) | M | 全 UI 面 golden/白盒、验证模式、结算/错误页、性能与 atlas 决策 |
| `dark-uiux-pr08` | [Director Grade Asset Reset](./pr/dark-uiux-pr08-director-grade-asset-reset.md) | XL | PR-07 后对 map-stage、shell chrome、right panel、bottom HUD 做 director-grade 质感重置 |
| `dark-uiux-pr08-d10` | [Retained UI And Map-Stage Authority](./pr/dark-uiux-pr08-d10-map-stage-authority-optimization.md) | XL | 将后续 UI 修复迁移到 libGDX retained UI / Skin / Table authority，并按 phase gate 大改首页、shell、背包、shop、天赋、combat/frontstage |

执行顺序固定为：`PR-00 -> PR-01 -> PR-01-1 -> PR-02 -> PR-02-1 -> PR-03 -> PR-04 -> PR-05 -> PR-06 -> PR-07 -> PR-08 -> PR-08 D10`。D10 是 PR-08 retained UI authority 子包，不替代 PR-08 director visual evidence；D10 内部按 `D10-P0 ~ D10-P8` 串行推进，每个 phase 完成测试、golden / 白盒和旧 route 删除或隔离后才能进入下一 phase。

## Asset Pipeline Deliverables
必须新增或更新以下文件族：

| Path | Purpose |
| --- | --- |
| `UI/ART_STYLE_BIBLE.md` | 暗黑 UI/UX 专项美术合同 |
| `UI/sprite-sheets/sheet-plan.yaml` | sheet/cell/visualKey/outputName 映射真源 |
| `UI/sprite-sheets/key-registry.yaml` | `targetKey / ownerPr / fallbackKey / consumer / consumerTest` 审计入口 |
| PR-00 document + `scripts/verify_dark_manifest_coverage.py` output | `dark-v1-manifest-coverage.json` common/owner/final 字段合同；`UI/sprite-sheets/coverage-schema.md` 只有实际落盘后才能成为输入 |
| `UI/sprite-sheets/prompts/dark-v1/*.prompt.txt` | 编号后的 Codex CLI 生图 prompt |
| `UI/sprite-sheets/prompts/dark-v1/prompt-index.json` | promptId、sheetId、rawSheetPath、prompt hash 索引 |
| `assets-src/image/raw/sheets/dark-v1/*.png` | `scripts/codex-generate-image.py` 从 Codex CLI 最新输出复制来的 raw sheet |
| `assets-src/image/contact-sheets/dark-v1/*.png` | 带 row/col/key 标签的验收图 |
| `assets-src/image/manifests/dark-v1-sprite-report.jsonl` | 切分与 QA report |
| `assets-src/image/manifests/phase2-visual-manifest.json` | canonical visual manifest，先更新它 |
| `client/src/main/resources/dark-v1/**/*.png` | 切分和后处理后的 runtime PNG |
| `client/src/main/resources/manifests/visual-manifest.json` | 由 `syncPhase2Manifests` 同步生成的 runtime manifest |

自动化必须覆盖：

1. sheet plan schema 校验。
2. raw sheet 尺寸和 grid 校验。
3. row/col 越界和重复校验。
4. `targetKey` 覆盖率校验。
5. `outputName` 与 manifest `rawOutputPath` 一致性校验。
6. alpha bbox、透明边距、空 cell、tile full-coverage 校验。
7. contact sheet 人工验收状态校验。

## Common Test Plan
- 环境预备：`source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env`
- UI smoke：`./gradlew :client:clientSmoke :client:goldenScreenshot`
- 旧资源合同：`./gradlew assetLint styleLint manifestLint`
- 暗黑雪碧图合同：`./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint`
- coverage 模式：PR-00 使用 `./gradlew darkManifestCoveragePr00DryRun`；PR-02 使用 `./gradlew darkManifestCoveragePr02OwnerScope`；PR-02-1 使用 `./gradlew darkManifestCoveragePr02_1OwnerScope`；PR-03/05 使用 `owner-scope` 并显式传 `-Pktome.darkUiux.ownerPr=PR-xx`；PR-06/07 使用 `final-full`。
- 治理检查：`./gradlew maintainabilityLint`
- 总入口：`./gradlew verifyChanged`
- 若改 Gradle/依赖/bootstrap：`./scripts/verify-bootstrap.sh`
- 职业树 UI 改造额外覆盖：`TalentSidebarPresenterTest`、`InputHandlerTest`、`TileRendererCanvasTest`、`phase4-v4-pr01` 白盒场景。
- 首页/验证入口额外覆盖：`MainMenuFocusPolicyTest`、`MainMenuControllerTest`、`MainMenuScreenTextTest`、`ValidationSetupControllerTest`、`GameAppLifecycleTest` 中主菜单/验证 setup case。
- 铭文商店额外覆盖：`InputHandlerTest` shop / inscription replacement case、`DescriptionPresenterTest` shop item lines、`TileRendererCanvasTest` shop modal/card 不重叠断言。
- 结算/错误/loading 额外覆盖：`OutcomeSummaryPresenterTest`、`UiErrorPayloadTest`、`UiLoadingStateTest`、golden outcome set。
- 白盒检查：桌面端确认首页/主菜单、角色创建、继续游戏异常、验证模式 setup、验证 overlay、左侧栏、地图、右侧装备/背包、铭文商店、底部 HUD、职业树、主动槽 modal、胜利/失败结算、runtime error/loading、设置/无障碍、快捷键提示、中文文本、manifest fallback / missing visual 状态（不含 ASCII renderer）、窗口缩放均正常。
- PR-07 必须执行 packaged app 白盒；只有明确没有 package-facing 改动且 PR 文档记录豁免原因时才允许跳过。

Atlas 决策阈值：

| 指标 | 触发后续 atlas PR 的阈值 |
| --- | ---: |
| client smoke 内 sprite texture 数 | `> 256` |
| 首屏 visual asset 加载累计 | `> 800ms` |
| 切场景峰值 GPU memory | `> 256MB` |
| `goldenScreenshot` 阶段耗时相对当前基线增量 | `> 25%` |

达到任一阈值时，PR-07 只记录证据并新开 atlas/region manifest PR；不得在 PR-07 直接改变 `VisualManifestEntry` 字段集合或 atlas/region 结构。

## Assumptions
- “全量替换”限定为视觉资源和 UI 表现，不修改核心规则、数值、存档、replay、profile schema。
- 旧资源迁移期可保留作回滚参考，但新 manifest 默认只指向新纪元资源。
- 如果未来 Codex CLI 支持显式 `--out` 图片路径，只替换 `scripts/codex-generate-image.py` 内部获取步骤，不改变 prompt、切分、manifest、验证流程。
- 职业树 UI PR 只消费 Phase4 v4 PR-01 已落地的 presentation/snapshot 合同；如职业树功能分支尚未合入，必须以它为上游分支或等价文档合同，不能在 UI PR 中重新实现职业树规则。

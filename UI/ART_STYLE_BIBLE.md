# K-ToME 暗黑 UI/UX 美术风格圣经

> 日期：2026-04-27
> 状态：Draft for UI redesign production
> 父级风格合同：[docs/2026-03-13-art-style-bible.md](../docs/2026-03-13-art-style-bible.md)
> 适用范围：`UI/PLAN.md` 所定义的新 UI、雪碧图、切分资源、运行时 icon、Tile、Actor、Portrait、VFX、UI chrome

本文是 K-ToME 全仓库暗黑 UI/UX 重构的专项美术合同。它继承原有中土高幻想风格，但把表现重心从“Phase 2+ 单张资源生成”收敛到“统一暗黑 UI、批量雪碧图、可切分、可映射、可运行时验证”。

## 1. 风格标签

新视觉纪元固定为：

`ktome-dark-fantasy-sprite-ui-v1`

使用规则：

1. 所有新 UI/视觉 prompt 必须显式包含该 style tag。
2. 所有雪碧图 spec、切分 report、contact sheet、manifest patch 必须记录该 style tag。
3. 旧 `ktome-middle-fantasy-painterly-tile-v1` 是父级世界观风格；新 tag 是 UI/资源替换专项风格。
4. 如果后续改变核心调色、材质或图标语言，必须 bump 到 `v2`，不能静默漂移。

## 2. 视觉方向

一句话目标：

`一个低饱和、铁石质感、火光与冷色边缘高光并存的暗黑 Roguelike 战术界面。`

正向关键词：

1. charcoal black
2. forged iron
3. worn stone
4. old leather
5. ember light
6. cyan edge glow
7. weathered brass
8. dungeon grit
9. readable silhouette
10. tactical inventory clarity

反向关键词：

1. sci-fi HUD
2. neon cyberpunk
3. mobile gacha shine
4. anime/chibi
5. glossy plastic
6. bright glassmorphism
7. pure flat office icons
8. text baked into images
9. merged sprite cells
10. inconsistent camera angle

## 3. 色彩合同

UI 与资源必须以暗色为主，不做单色蓝紫主题。

| Role | Hex | Usage |
| --- | --- | --- |
| `void-black` | `#05070A` | 主背景、地图外暗区 |
| `charcoal-panel` | `#10151D` | 面板底色 |
| `iron-edge` | `#2B3542` | 面板边、slot 边 |
| `cold-cyan` | `#1CB7C8` | 可交互边缘、高亮线 |
| `ember-gold` | `#D99A2B` | 标题、稀有资源、确认态 |
| `blood-red` | `#B64242` | 生命、危险、警告 |
| `stamina-green` | `#52C989` | 耐力、恢复 |
| `arcane-violet` | `#7B5CE1` | 奥术、经验、稀有魔法 |
| `muted-text` | `#AEB5BF` | 次级文本 |
| `primary-text` | `#E7E1D3` | 主文本 |

纪律：

1. 高亮面积必须小，主要用于交互、危险、稀有和当前选中态。
2. 冷青色只能做边缘和焦点，不允许大面积铺底。
3. 紫色只用于奥术/暗影，不得成为全局 UI 主色。
4. 图标必须在 `32x32` 缩放下仍能读出主体轮廓。

## 4. UI 材质语言

UI chrome 应表现为：

1. 深色铁框
2. 磨损石板
3. 局部旧金属铆钉
4. 低透明度阴影
5. 轻微冷青边缘光
6. 关键标题使用旧金/火光点缀

禁止：

1. 现代玻璃拟态大面积模糊
2. 纯矢量扁平图标
3. 科幻网格线
4. 高饱和发光按钮
5. 图片里烘焙中文、英文、数字或快捷键

## 5. 雪碧图生成硬约束

所有雪碧图必须遵守以下约束，否则不得进入切分流程：

1. 单张 sheet 固定网格，禁止自由摆放。
2. 每个 cell 只允许一个主体。
3. 主体不得跨格，不得遮挡相邻 cell。
4. 透明背景优先；tile ground/wall 例外，必须完整填满格子。
5. 禁止把 `row/col` 编号、名称、文字、logo、水印画进图片。
6. 同一 sheet 内光源、镜头、描边、像素密度必须一致。
7. 保留可切分安全边距：icon/actor/prop 默认 `10-14px`，portrait 默认 `16-24px`。
8. 生成结果如果 cell 边界不清晰，必须重生成，不允许靠人工猜测裁切。

## 6. Sheet 类型

| Type | Canvas | Grid | Cell | Runtime Usage |
| --- | ---: | ---: | ---: | --- |
| `icon-sheet` | `1024x1024` | `8x8` | `128x128` | UI icon、装备、技能、状态、任务、怪物小头像 |
| `large-sheet` | `1024x1024` | `4x4` | `256x256` | portrait、Boss、大型 actor、UI frame pieces |
| `tile-sheet` | `1024x1024` | `8x8` | `128x128` | ground、wall、decal、prop、VFX source |

切分后的运行时尺寸仍由处理脚本按 category 输出：

1. `tile_*`、`prop_*`、`actor_sprite` 默认处理到 `256x256` canvas。
2. `icon*` 默认处理到 `160x160` canvas。
3. `portrait` 默认处理到 `256x256` canvas。
4. UI frame 是否九宫格拉伸，由 client UI 实现决定，不由源图烘焙文字或尺寸。

## 7. 资源族群

### 7.1 UI Chrome

风格：黑铁、石板、低亮边缘、旧金属磨损。

必须读得出：

1. 普通面板
2. 当前选中面板
3. 装备 slot
4. 背包 slot
5. 禁用 slot
6. tooltip / modal
7. 生命、耐力、经验条材质

### 7.2 Tile

风格：正交、顶视优先、边界清晰。

必须区分：

1. foundation
2. forest edge
3. mine
4. ruins
5. shadow depths
6. ground vs wall
7. 普通地表 vs 危险地表

### 7.3 Actor

风格：强 silhouette，职业/阵营优先于细节。

必须区分：

1. player
2. Vanguard / Rogue / Templar / Arcanist
3. humanoid enemy
4. beast / undead
5. abyssal / crystal / forge / river faction
6. Boss

### 7.4 Icon

风格：单主体、高对比、无文字、强轮廓。

必须区分：

1. 装备类型
2. 技能元素
3. 状态正负面
4. 任务/区域
5. 伤害类型
6. 稀有度和 artifact 感

### 7.5 Portrait

风格：暗黑英雄肖像或区域缩略图，不做全景宣传图。

必须满足：

1. 职业身份第一眼清楚。
2. 天赋树 portrait 使用符号/姿态/材质区分，不靠文字。
3. 区域 portrait 保留地点主题，不需要可玩地图精度。

## 8. Prompt 合同

每张 sheet 的 prompt 必须由 spec 生成，不手写临场版本。

模板：

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

`cellList` 由 `sheet-plan.yaml` 输出：

```text
row 0 col 0: crossed weapon and ember spark action-choice icon.
row 0 col 1: branching tactic glyph with two route strokes.
row 0 col 2: focused target reticle over a worn stone tile.
```

## 9. 切分与映射合同

图片本身不是映射真相。映射真相只来自 `sheet-plan.yaml`：

1. `sheetId` 决定源大图。
2. `row` / `col` 决定切分矩形。
3. `targetKey` 决定 manifest key。
4. `category` 决定后处理 canvas 和 lint 规则。
5. `outputName` 决定最终 `rawOutputPath`。

任何 `targetKey` 缺失、重复、路径不一致、cell 越界，都必须 fail fast。

## 10. 评审清单

每张 sheet 进入 manifest 前必须完成：

1. contact sheet 上能看到 `row,col,targetKey` 标签。
2. 每个 cell 的主体和标签语义一致。
3. 无跨格、无文字、无水印、无透明异常。
4. 缩小到 `32x32` 后仍能识别核心轮廓。
5. 同一 sheet 内风格、光源、材质统一。
6. 切分 PNG 的 `outputName` 与 manifest `rawOutputPath` 一致。
7. 低质量 cell 进入 polish round，不用临时兼容破坏整体风格。

## 11. 一句话原则

新视觉不是“更花的图”，而是：

`每个小图都能被机器切准、被 manifest 映射准、被玩家在暗黑 UI 中一眼读准。`

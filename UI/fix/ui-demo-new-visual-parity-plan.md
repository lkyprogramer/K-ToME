# UI-demo-new 终局视觉一致性改造方案

日期：2026-05-13

参照图：`UI/UI-demo-new.png`

整合输入：

- `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`
- external codex plan: `UIUXUpdate.md`
- 当前游戏截图与 `UI/UI-demo-new.png` 的人工对比结论
- `UI/ART_STYLE_BIBLE.md`

## 1. 预检摘要

命中工作包不是原始 PR-02-1 的纯 shell scaffold 收口，而是 `UI-demo-new` 终局视觉一致性修复包。原 PR-02-1 的边界是搭框架，允许 PR-05 再处理 terrain / actor / prop；但当前验收目标已经提升为“截图第一眼必须接近新 DEMO”，因此必须把少量首屏视觉阻塞资源纳入本次改造。

受影响 owner：

- `client`：主 shell layout、renderer、UI token、golden screenshot、input pagination。
- `game`：validation scenario 的演示职业、区域、证据清单与固定数据。
- `UI/sprite-sheets` / manifest：必要资源生成、替换、覆盖率和 owner contract。
- `UI/manual-records` / `UI/pr` / `UI/PLAN.md`：验收口径、白盒记录和路线图同步。

硬边界：

- 不改 `core/game` 规则公式，不改 save / replay / profile schema。
- 不把 `UI/UI-demo-new.png` 当 runtime asset。
- 不引入 validation-only 视觉第二真源；首屏阻塞资源优先复用现有 semantic visual key。
- 不全量提前生成 PR-04 职业树、PR-06 技能/status、PR-07 polish 资源。
- 本次只生成或替换首屏 DEMO 一致性必需资源，不能用代码 scrim、拉伸、占位假装美术已达标。

## 2. 与 UIUXUpdate.md 的整合结论

保留 `UIUXUpdate.md` 中有价值的部分：

- UI token 暖色化：从冷蓝灰转向黑铁、旧铜、暖琥珀。
- 清理 renderer 中散落的 `#05070A`、冷色 scrim、临场 hex。
- 启用 `ChromeFramePainter` / 9-slice / corner sprite，而不是纯色矩形。
- 加 `WarmTorchOverlayRenderer` 或等价 map-stage 暖色边缘融合层。
- 增强装备、铭刻、背包的信息密度。
- 用 golden screenshot + packaged white-box + manual checklist 闭环。

必须修正 `UIUXUpdate.md` 的过期前提：

1. **不能继续坚持“不重生成 sprite”。** 当前差距已经不是 token 和 layout 能解决的：地图 tile、actor、stairs、stage backdrop 的风格与 `UI/UI-demo-new.png` 不一致，必须生成或替换资源。
2. **底部不能保留独立 `commandHints` 区。** 当前截图中技能栏右侧快捷键说明与右栏 `操作提示` 重复，视觉上切碎底栏，也与新 DEMO 不一致。最终只保留右栏 `操作提示`，底部改为 `hero -> action deck -> log deck`。
3. **不能继续用森林/冷灰验证场景对齐地牢 DEMO。** PR-02-1 scenario 当前 `zoneId=greenwood_fringe`、`professionId=rogue`，与新 DEMO 的地牢石室、战士盾牌语义不匹配；应切到 `shattered_outpost` + `vanguard`。

## 3. 当前截图阻塞项

按 UI 总监、美术总监、游戏设计总监标准，当前截图不能验收，阻塞点如下：

1. 地图视觉不是 DEMO 的暗黑地牢：tile 尺寸感、纹理、光照、fog 和右侧空区融合都不对。
2. 舞台空区虽然从程序 grid 改成暗底，但仍偏“铺了一张背景”，没有 demo 中的石质暗场、雾感和局部光照层次。
3. 底部技能栏右侧有重复快捷键说明，造成底栏拥挤；新 DEMO 的操作提示只在右栏底部。
4. 英雄卡信息密度和视觉层次不足，crest / 生命 / 资源 / 攻防关系没有形成 demo 式状态块。
5. action deck 至少要稳定支持 4 个主动槽；当前 3 个技能 + 右侧重复 hints 不是最终布局。
6. 右栏装备区空槽太多且 placeholder 弱，读起来像未完成 UI。
7. 铭刻栏 7-12 空白或 `-` 过多，虽然有 row plate，但密度和 demo 不一致。
8. 背包需要固定 `4x2` 当前页，并支持超过 8 件时翻页和页码，不允许靠截断隐藏后续物品。
9. 字体、slot、边框和地图之间仍有冷硬断层，缺少 `UI/ART_STYLE_BIBLE.md` 要求的黑铁、旧铜、火光层次。

## 4. 资源生成决策

结论：**需要生成或替换少量美术资源。**

理由：代码可以修比例、绘制顺序、色调和 slot backing，但不能把现有冷灰/森林/旧 phase2 资源变成新 DEMO 的暗黑石室。继续只靠代码会导致反复调 scrim、alpha、缩放，最终仍像“工程补丁”，不是美术验收。

本次采用“替换现有 semantic key”的方式，而不是新增 demo-only key：

- 避免 validation scenario 里出现第二套视觉映射。
- 让 `shattered_outpost`、`vanguard`、`stairs.down` 的正式视觉一起升级。
- 影响是视觉层面的，不改变玩法规则、内容 schema、存档或回放。

资源归属策略：

- 当前仓库已经存在 `PR-02-2` owner：`UI/sprite-sheets/key-registry.yaml` 已登记 `tileset.ruins.ground_01`、`tileset.ruins.wall_01`、`actor.vanguard`、`prop.stairs.down`；`UI/sprite-sheets/sheet-plan.yaml` 已有 `r02-ui-demo-ruins-tiles` 和 `r03-ui-demo-actor-props`；`UI/sprite-sheets/owner-contracts/pr02-2-owner-keys.yaml` 已固定 4 个 direct cell。
- 这意味着本轮不再走“新增 PR-02-2 owner / 新增 sheet round”流程，而是**重生已有 cell**：复用 sheetId / row / col / targetKey，只刷新 raw sheet、runtime PNG、hash、coverage、manual evidence。
- `ui.shell.map_stage.backdrop` 仍归 `PR-02-1` 的 `r01b-ui-shell-chrome`，本轮同样只重生同一 sheet cell，不改变 owner。
- 不新增 demo-only visual key，不在 validation scenario 里做视觉 override。
- 若首屏装饰 props 被人工判定为阻塞项，单独启用 `PR-02-3` scenery mini-round；不能把 scenery props 混进 PR-02-2 已冻结 owner contract。

必须生成或替换的资源：

| visual key | 当前真源 | sheet-plan 归属 | 本轮动作 | 验收标准 |
| --- | --- | --- | --- | --- |
| `ui.shell.map_stage.backdrop` | `dark-v1/ui/ui_shell_map_stage_backdrop.png` | 已有 `r01b-ui-shell-chrome` row 3 col 3，ownerPr=`PR-02-1` | 重生同一 cell；保持 key/path，刷新 hash、coverage、manual evidence | near-black radial fog backdrop；不带砖墙/石板纹理；不含 actor、item、文字；只支撑 mapStage 未探索/外延暗场 |
| `tileset.ruins.ground_01` | `dark-v1/tiles/tileset_ruins_ground_01.png` | 已有 `r02-ui-demo-ruins-tiles` row 0 col 0，ownerPr=`PR-02-2` | 重生同一 cell；保持 key/path、pivot、footprint、tags | 32px 下仍能读出至少 2 种石板裂纹方向；暖暗色；cell 边界可读但不出现粗黑网格 |
| `tileset.ruins.wall_01` | `dark-v1/tiles/tileset_ruins_wall_01.png` | 已有 `r02-ui-demo-ruins-tiles` row 0 col 1，ownerPr=`PR-02-2` | 重生同一 cell；保持 key/path、pivot、footprint、tags | 与 ground 同光源同材质；墙体轮廓清晰；顶部不出现周期性砖墙条纹 |
| `actor.vanguard` | `dark-v1/actors/actor_vanguard.png` | 已有 `r03-ui-demo-actor-props` row 0 col 0，ownerPr=`PR-02-2` | 先做 draw-call 诊断；若 renderer 已绘制但不可读，再重生同一 cell | 64px 能识别盾+剑轮廓；32px 仍有清楚主体；主体不只是一两个暗点 |
| `prop.stairs.down` | `dark-v1/props/prop_stairs_down.png` | 已有 `r03-ui-demo-actor-props` row 0 col 1，ownerPr=`PR-02-2` | 重生同一 cell；保持 key/path、pivot、footprint、tags | 小尺寸可读、方向明确、与地牢地面融合，不喧宾夺主 |

backdrop 的强约束：

- prompt 必须明确 `near-black radial fog backdrop, no brick pattern, no stone texture, single-channel dark amber vignette`。
- runtime 不能把 backdrop 当作高细节 terrain 使用；backdrop 只在 mapStage 外延暗场、未探索/不可见空区中露出。
- 已探索/可见 tile 区必须以 `tileset.ruins.*` 为主，不能被 backdrop 砖墙纹理覆盖。

可复用不重生的资源：

- `ui.frame.slot.*`
- `ui.shell.*` frame、divider、nav、hero crest、operation hint plate
- 已有 item icon、skill icon、currency icon，除非单张资源在截图中明显破坏验收

资源生成纪律：

- prompt 必须包含 `ktome-dark-fantasy-sprite-ui-v1`。
- 不允许图片中烘焙中文、英文、数字、快捷键、水印。
- 每个资源生成后先人工放大和 32px/64px 缩略检查；不合格先重生，不进入代码补救。
- PR-02-1 / PR-02-2 已有 cell 的重生只刷新 raw sheet、runtime PNG、hash、coverage artifact、manual record；不得擅自改 sheetId、row、col、targetKey。
- 如新增 PR-02-3 scenery mini-round，才同步新增 `sheet-plan.yaml`、`key-registry.yaml`、`owner-contracts/pr02-3-owner-keys.yaml` 和 owner-scope coverage。

## 5. 布局与渲染改造

### 5.1 底部 deck：删除重复快捷键区

目标结构：

```text
bottomDeck = heroCard + actionDeck(4 slots) + logDeck
```

实施点：

- `DemoBottomDeckLayout` 不再暴露可见 `commandHints` 区。
- `InfoSurfaceLayout.footerHintBounds` 如仍需兼容旧类型，设为零宽或移除，不能再参与真实绘制。
- `DemoShellRenderer.renderCommandHints` 从底部绘制路径删除。
- `TileRenderModel` 仍可生成一份 operation hint 文案，但只交给右栏 `operationHints`。
- `ui.shell.command_hint.plate` 不再用于底部，而是用于右栏 `操作提示` 的 row backing 或 compact plate，避免 owner key 失去消费路径。
- `DemoShellLayoutTest`、`InfoSurfaceLayoutTest`、`TileRendererCanvasTest` 删除“bottom command hints 存在”的断言，改断言 bottom 三分区不重叠、log deck 不被挤压。

主动槽口径：

- 用户明确要求技能可能不止 3 个，布局至少支持 4 个；因此 internal action deck 固定 4 slot bounds。
- 首屏视觉可以是 3 个已填槽 + 第 4 个低强调 framed empty slot，但第 4 槽不能退化成快捷键文字区。
- golden 断言应检查 4 个 slot bounds 稳定存在；视觉审查允许第 4 空槽透明度低于已填槽。

验收：

- 技能栏右侧不再出现 `i 背包 / g 拾取 / Ctrl+S 保存 / 5-8 使用铭刻`。
- 右栏底部仍有 `操作提示`，且是唯一快捷键提示区域。
- action deck 能稳定显示 4 个主动槽；不足 4 个时第 4 格是 framed empty slot，不是文字区。

### 5.2 右栏：装备 -> 铭刻栏 -> 背包 -> 操作提示

固定顺序：

```text
equipment
inscriptions
backpack
operationHints
```

高度分配锚点：

- `equipment`: 42%
- `inscriptions`: 26%
- `backpack`: 15%
- `operationHints`: 剩余高度，最低约 10-17%

该比例按 `UI/UI-demo-new.png` 重新校准：装备区是右栏最高的视觉锚点，铭刻栏与背包更紧凑。允许 layout solver 为最小窗口做压缩，但 demo-aspect `1672x941` 下必须保持“装备高区 -> 铭刻双列 -> 背包 4x2 -> 操作提示”的密度关系。不得因为操作提示文本过长反向压缩背包和铭刻栏。

实施点：

- 保持无 `rightGroundLoot`，不再恢复地面物品区。
- 装备区为 icon-first socket：
  - 真实规则槽只映射 `WEAPON / OFF_HAND / ARMOR / ACCESSORY`。
  - helmet / cape / gloves / ring / boots 等为 display-only empty socket，不引入规则层装备槽。
  - 空槽只画低强调 frame / socket，不画装备图标、silhouette 或伪装成已有物品的 placeholder。
- 铭刻栏固定显示 5-12：
  - 双列 4 行，共 8 个 framed rows。
  - 5-8 只显示真实可操作铭刻；9-12 只保留空 framed row 和弱热键，不显示假图标、假名称或 `预留` 文案。
  - 文本必须在 row plate 内，禁止直接裸贴到背景。
  - validation scenario 不允许为视觉密度填充 9-12 的假铭刻；真实为空时就保持空。
- 背包固定 `4x2` 当前页：
  - page size = 8。
  - 超过 8 件时显示 `1/N` 和翻页提示。
  - 全局只有一份背包页状态：`inventoryPage = overlayState.inventorySelection / 8`。
  - 右栏背包不维护自己的 page；它只被动反映当前 `inventorySelection`。
  - inventory modal 内 `PageUp/PageDown` 修改 selection 到目标页第一格；关闭 modal 后下次打开仍由 selection 推导当前页。
  - map mode 的 `PageUp/PageDown` 移动语义不回归，也不偷偷改变 inventory page。
- 操作提示区使用 compact rows，文本不压住背包 grid。

验收：

- 右栏没有 `地面物品`。
- 铭刻栏 5-12 都有 framed row；5-8 有真实 icon/text，9-12 是弱热键空行。
- 背包显示 8 格当前页，并能证明第 9 件以后可翻页。

装备 9-cell 布局：

```text
weapon       offhand
helm         armor
cloak        gloves
amulet       ring
      boots
```

装备槽映射：

| visual cell | rule slot | 状态 |
| --- | --- | --- |
| `weapon` | `WEAPON` | 真实装备槽 |
| `offhand` | `OFF_HAND` | 真实装备槽 |
| `armor` | `ARMOR` | 真实装备槽 |
| `amulet` | `ACCESSORY` | 真实装备槽，ACCESSORY 优先显示在 amulet；ring 是 display-only empty socket |
| `helm` | N/A | display-only empty socket |
| `cloak` | N/A | display-only empty socket |
| `gloves` | N/A | display-only empty socket |
| `ring` | N/A | display-only empty socket |
| `boots` | N/A | display-only empty socket，layout solver 居中 |

display-only socket 只能提供布局密度，不能提供假装备内容。空槽不得复用 manifest item icon，也不得用 `ui.shell.*` 资源假装装备 icon。

### 5.3 地图舞台：资源底布 + 真实 terrain + 暖色融合

目标绘制顺序：

```text
clearColor
mapStage backdrop, only for stage dark extension / hidden empty area
terrain ground
terrain wall
floor decoration props
actors
loot markers
fog overlay
warm vignette
ember edge feather
chrome 9-slice corners / nav / right / bottom
```

实施点：

- `DemoShellRenderer` / `TileRenderer` 只能通过 manifest key 消费 `ui.shell.map_stage.backdrop`，禁止 raw path。
- map stage backdrop 只负责舞台空区暗石/雾感，不替代 `tileset.*`。
- `tileset.ruins.*` 负责真实地面/墙体质量，不能继续用 backdrop 掩盖 terrain 质量。
- actor 可读性必须先诊断再重生：
  - 用 canvas draw-call / layer trace 确认 `actor.vanguard` 是否进入 `MAP_ACTORS` batch。
  - 如果 actor draw-call 缺失，先修 renderer/model 的 actor layer；此时重生 PNG 无效。
  - 如果 draw-call 存在但 32px/64px 不可读，再重生 `actor.vanguard` cell。
- `WarmTorchOverlayRenderer` 或等价层只做：
  - 地图边缘暗角：径向梯度外圈 `#1A0E04` alpha `0.65`，内圈 alpha `0`，初始实现可用 16 圈矩形序列近似。
  - 玩家附近低 alpha 暖光：半径 8 cells，颜色 `#D99A2B`，alpha `0.06`。
  - mapStage 内侧细微 ember edge feather：内侧 12px，颜色 `#D99A2B`，alpha `0.18`。
- fog 颜色改为暖暗，不再冷蓝/纯黑大块遮罩。
- 地图缩放以 DEMO 观感为准：房间在舞台中偏左上，暗区保留空间，但不能出现粗大现代网格。
- 首屏装饰 props 策略：
  - Phase 0 先检查现有 mapgen / props / loot marker 是否已经能提供火光、血迹、骨堆、武器和金币视觉密度。
  - 如果缺失导致 `UI/UI-demo-new.png` 首屏观感 fail，启用 `PR-02-3` scenery mini-round，不允许用 renderer hard-code 假装地图内容。
  - 新 scenery 必须作为 floor decoration 或 marker 进入 typed render layer：terrain 之后、actor 之前、fog 之前；不能成为规则层交互物，不能影响 save/replay。
  - 如果不启用 PR-02-3，manual record 必须写 `secondaryPropDeferred[]`，列出 visual key、截图位置、是否阻塞本轮 close。

PR-02-3 最小 scenery key 预案：

| visual key | category | sheet round | 用途 | 首屏放置口径 |
| --- | --- | --- | --- | --- |
| `prop.torch.wall_mounted` | `prop_environment` | `r04-ui-demo-scenery` | 墙面火把暖光主体 | 相对首屏房间上边墙 1-2 个点，数据驱动放置 |
| `prop.blood_splatter` | `prop_environment` | `r04-ui-demo-scenery` | 地面血迹 | terrain 后、actor 前，低饱和暗红 |
| `prop.skeleton.fragment` | `prop_environment` | `r04-ui-demo-scenery` | 骨堆/碎骨 | 地面装饰，不阻挡 |
| `prop.gold_pile.small` | `prop_environment` | `r04-ui-demo-scenery` | 金币小堆 | 可作为 loot presentation，不改变 loot 规则 |
| `marker.loot.weapon` | `icon` | `r04-ui-demo-scenery` | 地面武器掉落 marker | 由 `GroundLootMarkerModel` 或等价 marker presenter 消费 |
| `marker.loot.scroll` | `icon` | `r04-ui-demo-scenery` | 地面卷轴掉落 marker | 由 `GroundLootMarkerModel` 或等价 marker presenter 消费 |

验收：

- 空区不是程序黑格。
- 真实 terrain 与 backdrop 不割裂。
- 角色、楼梯、loot marker 均在 mapStage 内且与 tile 尺度协调。
- `UI/UI-demo-new.png` 第一眼的地牢石室感成立。

### 5.4 色彩、chrome、字体密度

实施点：

- 按 `UIUXUpdate.md` Phase A 做 token 暖色化，但最终值要以 `UI/ART_STYLE_BIBLE.md` 的 `ember-gold`、`charcoal-panel`、`iron-edge` 为准。
- 文本色必须同步对齐 `UI/ART_STYLE_BIBLE.md`：
  - `text.primary`: `#DDDDDD` -> `#E7E1D3`
  - `text.secondary`: `#AAAAAA` -> `#AEB5BF`
  - `text.disabled`: `#777777` -> `#59616C`
- 清理 `DemoShellRenderer.kt`、`StandaloneScreenLayout.kt` 中冷色/黑色字面量。
- `ChromeFramePainter` 统一使用 9-slice/corner sprite，content inset 进 token。
- mapStage frame 不画连续 ember-gold 外周矩形；只绘四角 filigree corner sprite 和短 edge 装饰，避免 Image #3 那种硬矩形描边。
- hero card 使用 `ui.shell.hero_crest.placeholder`，状态条、攻击、防御收进同一卡片，不再另起 stats summary。
- hero crest 左下角绘制 depth/floor badge：圆形小徽章，背景用 `bar.background`，文字用 `text.primary`，文本来自 scenario floor / depth presentation。
- log deck 文本区域要有稳定 padding 和 clip，不允许文字压边或被图案吞掉。
- log deck 色阶：
  - zone / quest 名使用 `ember-gold #D99A2B`。
  - 关键叙事正文使用 `text.primary`。
  - 路线提示、说明性文本使用 `text.secondary`。
  - cold-cyan 不用于普通 log 文本，只保留给明确交互高亮。
- 暖色分布纪律：
  - 面板 body / scrim 使用暗琥珀黑底，例如 `#1A140E`、`#15100A`。
  - corner / edge / title 才使用 `ember-gold` 装饰，面积要小。
  - `border.strong` 使用 `#D99A2B` 时 alpha 控制在 `0.42-0.62` 区间，避免全界面泛金。
  - actor selection ring 使用 ember-gold 暖色，匹配 `UI/UI-demo-new.png` 玩家高亮；cold-cyan 只用于 UI 菜单、可交互态、可学习态或辅助边缘光。

action card 排版：

```text
+----------------+
| 1      [icon]   |
|                |
| 猛击           |
| 8 耐力         |
+----------------+
```

- 数字角标在左上，使用 `ember-gold`。
- icon 在卡片主体中心偏上，不压住名称。
- 名称与消耗在底部两行，使用 row-safe clipping。
- 第 4 空槽只绘低 alpha frame / placeholder，不绘快捷键说明。

operation hints 色阶：

- key 字符（`i`、`g`、`Ctrl+S`、`L`、`1-4`、`5-8`）使用 `text.primary` 或小面积 `ember-gold`。
- 说明文本使用 `text.secondary`。
- 组之间使用 xs spacing，不画成底部独立 command deck。

验收：

- 整体不再冷青灰。
- 铜色边、暗石底、火光高亮只用于关键结构，不大面积泛光。
- 文本在面板内有 backing，不能像悬浮在 UI 上。

## 6. Validation scenario 和证据改造

PR-02-1 demo scenario 应从“shell scaffold 验证”升级为“UI-demo-new parity 验证”。

建议修改：

- `ValidationScenarioRegistry.kt`
  - 只修改 `id = dark-uiux-pr02-1-demo-shell-foundation` 这一条 demo scenario；其他 PR-00/01/02/03/04/05/06/07 validation scenario 不动。
  - `professionId`: `rogue` -> `vanguard`
  - `zoneId`: `greenwood_fringe` -> `shattered_outpost`
  - seed 保持固定；如房间形状无法接近 DEMO，可换固定 seed，但必须同步测试和 manual record。
  - evidence 增加 `1672x941` demo aspect 截图，不只保留 `1280x800`。
- `ValidationScenarioPresentationCatalog.kt`
  - active talent display 至少 4 slot。
  - 装备、铭刻、背包只展示真实 scenario 状态；空装备/空铭刻/空背包/空技能槽只画空框。
  - 首屏 parity scenario 的背包恰好 8 件，保持 `UI/UI-demo-new.png` 的无页码首屏观感。
  - 背包翻页能力用单独 pagination evidence fixture 覆盖，fixture 至少 9 件；不要让首屏 golden 因 `1/2` 页码偏离 DEMO。
- i18n 文案：
  - 描述应从 “scaffold bounds” 更新为 “UI-demo-new parity / no duplicate bottom command hints / pagination evidence”。

建议证据文件：

- `evidence/ui-demo-new-parity-1672x941.png`
- `evidence/ui-demo-new-parity-1280x800.png`
- `evidence/ui-demo-new-right-panel-grid.png`
- `evidence/ui-demo-new-bottom-deck-no-command-hints.png`
- `evidence/ui-demo-new-inventory-page-1.png`
- `evidence/ui-demo-new-inventory-page-2.png`
- `evidence/ui-demo-new-nav-rail-crop.png`
- `evidence/ui-demo-new-map-stage-crop.png`
- `evidence/ui-demo-new-app.log`

证据前置条件：

| evidence | 分辨率 / 范围 | 前置条件 | 触发方式 |
| --- | --- | --- | --- |
| `ui-demo-new-parity-1672x941.png` | `1672x941` full window | scenario 已切 `shattered_outpost` + `vanguard`；新 ruins / actor / stairs 资源已接入 | packaged white-box 启动后首屏截图 |
| `ui-demo-new-parity-1280x800.png` | `1280x800` full window | 同上，用于常规窗口回归 | packaged white-box 启动后首屏截图 |
| `ui-demo-new-right-panel-grid.png` | right panel crop | 装备 9 sockets、铭刻 5-12、背包 4x2、operation hints 均可见 | capture crop |
| `ui-demo-new-bottom-deck-no-command-hints.png` | bottom deck crop | action deck 4 slots；底部无 command hints；右栏仍有操作提示 | capture crop |
| `ui-demo-new-inventory-page-1.png` | inventory/right backpack | pagination fixture 背包至少 9 件，selection 在第 1 页；首屏 parity golden 仍只用 8 件 | 打开 inventory modal 或捕获右栏第一页 |
| `ui-demo-new-inventory-page-2.png` | inventory/right backpack | pagination fixture 中 selection 已通过 `PageDown` 跳到第 2 页 | inventory modal 中按 `PageDown` 后截图 |
| `ui-demo-new-nav-rail-crop.png` | nav rail crop | 五个 nav icon 都走 `ui.shell.nav.*`，selected 状态 bounded | capture crop |
| `ui-demo-new-map-stage-crop.png` | map stage crop | backdrop、ruins terrain、vanguard、stairs、fog、warm overlay 均已绘制 | capture crop |
| `ui-demo-new-app.log` | app log | packaged app 启动、scenario materialization、截图流程无 runtime error | 保存运行日志 |

manual record 必须逐项对照 `UI/UI-demo-new.png`，不能再写 `map backdrop/art quality deferred` 作为可接受差异。真正可以留给后续的只有非首屏、非 DEMO 必需的完整 PR-05/PR-06 资源量。

## 7. 执行顺序

### Phase 0：基线冻结

1. 运行现有 `:client:goldenScreenshot`，保存当前 fail/diff。
2. 用 `UI/UI-demo-new.png` 对当前截图做阻塞项标注。
3. 确认 `UI/fix/ui-demo-new-visual-parity-plan.md` 是本轮执行真源，旧 `UIUXUpdate.md` 只作参考。
4. 验证 scenario 数据前置：
   - `game/src/main/resources/data/zones/index.yaml` 必须存在 `shattered_outpost`，且 `tilesetKey=tileset.ruins`。
   - `game/src/main/resources/data/professions/index.yaml` 必须存在 `vanguard`，且 starting kit / starting talents / starting inscriptions 完整。
   - 固定 seed 跑一次 scenario smoke，确认首屏房间形状、起始点、楼梯、loot marker 不与 `UI/UI-demo-new.png` 的布局目标严重冲突。
5. 固定资源 owner 决策：
   - `ui.shell.map_stage.backdrop` 继续归 `PR-02-1` / `r01b-ui-shell-chrome`。
   - `tileset.ruins.*`、`actor.vanguard`、`prop.stairs.down` 已归 `PR-02-2`，复用现有 `r02` / `r03` sheet cell。
   - 只有新增 scenery props 才走 `PR-02-3` / `r04-ui-demo-scenery`。
6. 诊断 actor 可见性：
   - dump 或断言当前帧 `MAP_ACTORS` draw-call 包含 `actor.vanguard`。
   - 若 draw-call 缺失，先修 `TileRenderModel.actorTiles` / `TileRenderer.TileMapRenderer` actor layer。
   - 若 draw-call 存在但缩略不可读，再进入资源重生。

退出条件：

- 当前问题清单落到 manual record 草稿。
- 确认本轮允许重生 5 个核心首屏资源；如 scenery 被判阻塞，确认是否追加 PR-02-3 的 6 个 scenery / marker 资源。
- `PR-02-1` / `PR-02-2` 既有 cell 的 sheetId、row、col、输出路径已核实；PR-02-3 是否启用已有明确判定。

### Phase 1：资源生成与替换

1. 重生 `ui.shell.map_stage.backdrop`，保持 `r01b` cell。
2. 重生 `tileset.ruins.ground_01`、`tileset.ruins.wall_01`，保持 `r02` cell。
3. 在 actor draw-call 已确认存在后，重生 `actor.vanguard`；重生 `prop.stairs.down`，保持 `r03` cell。
4. 刷新 raw sheet、runtime PNG、resource hashes、coverage artifact；PR-02-2 的 sheetId / row / col / targetKey 不改。
5. 如 Phase 0 判定需要 scenery props，新增 `r04-ui-demo-scenery` 和 `PR-02-3` owner contract。
6. 小图尺寸检查：32px、48px、64px、map tile runtime 尺度。

退出条件：

- 新资源单独预览合格。
- 资源 lint / manifest coverage 通过。
- 不合格资源必须重生，不能先靠 renderer 补救。

### Phase 2：删除底部重复 command hints

1. 改 `DemoShellLayout.kt` / `InfoSurfaceLayout.kt` 的 bottom deck 分区。
2. 改 `DemoShellRenderer.kt` 删除 bottom command hints 绘制。
3. 改 `TileRenderModel.kt`，operation hints 只流向右栏。
4. 更新 tests 中对 `bottomDeck.commandHints` 的断言。

退出条件：

- golden 中技能栏右侧无快捷键说明。
- 右栏 `操作提示` 仍可见。
- bottom deck 三分区不重叠。

### Phase 3：右栏密度、铭刻、背包分页

1. 装备区保持 9 格视觉密度，但空位只显示 empty socket，不放 placeholder icon。
2. 铭刻栏固定 5-12 双列 framed rows，5-8 显示真实铭刻，9-12 为空 framed row。
3. 背包当前页固定 4x2；首屏 parity data 恰好 8 件，不显示 `1/2` 页码。
4. pagination fixture 至少 9 件，inventory modal 增加 `PageUp/PageDown` 翻页，map mode 不回归。

退出条件：

- 右栏 crop 和 `UI/UI-demo-new.png` 的信息密度接近。
- 背包第 9 件以后可以通过 pagination fixture 的白盒操作看到；首屏 golden 不显示分页页码。

### Phase 4：地图舞台、暖色 overlay、chrome 收口

1. 接入新 backdrop 和 ruins terrain 资源。
2. 确认绘制顺序：backdrop -> terrain/actor/prop -> fog -> warm overlay -> chrome。
3. 清理冷色 token 和 raw hex。
4. 启用 9-slice corner / edge sprite。
5. hero card、action deck、log deck 做最终 padding 和 clip。

退出条件：

- map stage crop 不是程序黑格。
- 地牢石室和暗区融合接近 DEMO。
- 文字不压边、不悬浮、不被装饰吞掉。

### Phase 5：scenario、golden、manual record

1. scenario 切 `shattered_outpost` + `vanguard`。
2. 增加 `1672x941` demo aspect golden label。
3. 增加右栏、底栏、地图 stage、背包分页 crop evidence。
4. 更新 `UI/manual-records/*` 和 PR 文档。

退出条件：

- `:client:goldenScreenshot` 产物可直接与 `UI/UI-demo-new.png` 并排审查。
- manual record 所有 DEMO 关键项有 pass/fail 和截图路径。

### Phase 6：白盒验收

按 `docs/computer-use-whitebox-flow.md` 运行 packaged app 白盒：

1. 启动验证场景。
2. 截 `1672x941`、`1280x800`。
3. 截 right panel crop、bottom deck crop、map stage crop、inventory page 1/2。
4. 对照 `UI/UI-demo-new.png` 写 manual record。
5. 任一阻塞项 fail，回到对应 Phase 修复，不允许停止。

估时：

| phase | 预计耗时 | 主要不确定性 |
| --- | --- | --- |
| Phase 0 | 0.5 day | seed 首屏形状是否需要调整 |
| Phase 1 | 1-2 day；启用 PR-02-3 scenery 时至少 2 day | 生图质量、重生次数、sheet/manifest 同步次数 |
| Phase 2 | 0.5 day | 删除 bottom command hints 后测试更新范围 |
| Phase 3 | 1 day | 背包分页和右栏密度调试 |
| Phase 4 | 1-1.5 day | warm overlay 调参和地图融合 |
| Phase 5 | 0.5 day | golden baseline 更新和 evidence 文案同步 |
| Phase 6 | 0.5 day | packaged app 白盒截图与人工复核 |

合计约 5-6 个工作日；如果资源生成连续失败，Phase 1 单独延长，不允许把失败资源带入后续 Phase。

## 8. 自动化验证计划

环境前置：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

Focused tests：

```bash
./gradlew :client:clientSmoke
./gradlew :client:goldenScreenshot
```

需要补或更新的断言：

- `DemoShellLayoutTest`
  - 无可见 bottom command hints region。
  - bottom deck 为 hero/action/log 三分区。
  - action deck 支持 4 slots。
  - right panel 顺序为 equipment/inscriptions/backpack/operationHints。
  - inscriptions 显示 5-12。
  - backpack 当前页 4x2。
- `InfoSurfaceLayoutTest`
  - `footerHintBounds` 移除或零宽。
  - log deck 不被 command hints 挤压。
- `DemoShellRendererTest`
  - 不绘制 `地面物品`。
  - 不在 bottom deck 绘制 command hint text。
  - `ui.shell.command_hint.plate` 只在右栏 operation hints 消费。
  - right panel 文本不逃逸 row bounds。
- `TileRendererCanvasTest`
  - map backdrop 在 terrain 前绘制。
  - warm overlay 在 terrain/fog 后、chrome 前绘制。
  - `MAP_ACTORS` layer 中能看到 `actor.vanguard` draw-call；不能只看到 focus / selection ring。
  - right panel / bottom deck draw bounds 不重叠。
- `InputHandlerTest`
  - inventory modal `PageUp/PageDown` 翻页并更新 selection。
  - map mode `PageUp/PageDown` 保持原移动语义。
- manifest/resource tests
  - 新/替换资源进入 runtime manifest。
  - owner-scope coverage 包含 `ui.shell.map_stage.backdrop`。
  - `PR-02-2` owner-scope coverage 包含 `tileset.ruins.ground_01`、`tileset.ruins.wall_01`、`actor.vanguard`、`prop.stairs.down`。
  - 如启用 PR-02-3 scenery，owner-scope coverage 包含 `prop.torch.wall_mounted`、`prop.blood_splatter`、`prop.skeleton.fragment`、`prop.gold_pile.small`、`marker.loot.weapon`、`marker.loot.scroll`。
  - 资源路径保持 repo-relative。
  - `darkSpriteSheetLint` / `spriteSheetMapLint` 能从 sheet-plan 找到所有 owner cell 的 row/col/cell hash。

完整 gate：

```bash
./gradlew assetLint styleLint manifestLint \
  darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint \
  :client:clientSmoke :client:goldenScreenshot \
  maintainabilityLint verifyChanged
```

最后检查：

```bash
git diff --check
```

## 9. 验收标准

必须全部满足：

1. 第一眼结构与 `UI/UI-demo-new.png` 一致：左 rail、中间地图舞台、右栏四区、底部 hero/action/log。
2. 右栏无地面物品区域。
3. 底部无重复快捷键说明；操作提示只在右栏。
4. action deck 稳定支持 4 个主动槽。
5. 铭刻栏 5-12 是双列 framed rows，文字不悬浮。
6. 背包 4x2 当前页，超过 8 件可翻页。
7. 地图舞台不是黑底程序格线，真实 ruins terrain / actor / stairs 与 backdrop 风格统一。
8. 整体色调为暗黑地牢暖琥珀，不再冷青灰。
9. hero card 内有完整状态摘要，不再依赖独立 stats 卡。
10. `text.primary`、`text.secondary`、`text.disabled` 与 `UI/ART_STYLE_BIBLE.md` 对齐。
11. golden、manual record、packaged white-box 三类证据都已更新。

## 10. 风险与回滚

风险 1：替换 `tileset.ruins.*`、`actor.vanguard`、`prop.stairs.down` 会影响所有使用这些 semantic key 的正式画面。

- 接受理由：这是正式视觉升级，不是 validation-only hack；不改变规则。
- 缓解：替换前保留 git diff，golden 和 manual record 覆盖首屏；若风格失败，回滚具体 PNG 和 manifest hash。
- 回归补充：先 `rg "tilesetKey: tileset\\.ruins" game/src/main/resources/data` 列出受影响 zone，再至少补 2 张 ruins 相关截图或 golden crop，覆盖 `shattered_outpost` 首屏和另一个 `tileset.ruins` 场景。如果时间不足，必须写入 follow-up，不能误称全量 ruins screen 已验收。

风险 2：资源质量仍不达标。

- 缓解：Phase 1 设置硬退出条件，不合格先重生，不进入 renderer 补丁。

风险 3：删除 bottom command hints 影响旧 tests 或 accessibility text。

- 缓解：可保留 presentation 文案源，但改变消费位置；右栏操作提示和 tooltip 继续承载可见帮助。

风险 4：warm overlay 带来过多 draw call。

- 缓解：用低层数矩形渐变或缓存 pixmap；如 FPS 明显下降，降级为边缘 feather + 玩家光晕两层。

回滚方式：

- 资源失败：回滚对应 PNG、manifest、coverage artifact。
- 布局失败：回滚 `DemoShellLayout.kt`、`InfoSurfaceLayout.kt`、`DemoShellRenderer.kt` 的底栏分区改动。
- scenario 失败：回滚 `ValidationScenarioRegistry.kt`、presentation catalog、i18n、manual record。

## 11. 完成定义

本轮只有在以下条件全部满足时才能停止：

- `UI/UI-demo-new.png` 并排人工审查通过。
- demo parity 判定主体固定为 PR owner + 至少 1 位非 owner reviewer；结果写入 `UI/manual-records/ui-demo-new-visual-parity.md` 的 `manualReviewers[]`、`reviewedAt`、`demoParityVerdict`、`blockingFindings`、`screenshotLabelCoverage` 字段。任一方判 `fail`，本轮不能 close。
- 当前游戏截图不再出现底部重复快捷键说明。
- 首屏地图资源不再被标记为 “PR-05 deferred”。
- 背包分页有自动化和白盒证据。
- `:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged` 至少按影响面执行并记录结果。
- manual record 不写“理论应通过”，只写实际截图和命令结果。

## 12. 附录 A：Demo Presentation Data

这份数据只定义 `UI/UI-demo-new.png` 首屏展示密度，不新增规则层 hotkey 或装备槽。若没有真实数据，对应格子必须保持空框，不能以 validation-only display key 补视觉密度。

铭刻栏 5-12：

| slot | display text | source / nameKey | iconVisualKey | state |
| --- | --- | --- | --- | --- |
| 5 | 治疗之印 | `inscription.healing_light.name` | `icon.skill.templar.holy_light` | usable |
| 6 | 相位门 | `inscription.phase_door.name` | `icon.skill.arcanist.blink` | usable |
| 7 | 铁壁之印 | `inscription.iron_shield.name` | `icon.skill.templar.holy_shield` | usable |
| 8 | 净除之印 | `inscription.purge.name` | `icon.skill.templar.purify` | usable |
| 9 | N/A | N/A | N/A | empty framed row with weak hotkey only |
| 10 | N/A | N/A | N/A | empty framed row with weak hotkey only |
| 11 | N/A | N/A | N/A | empty framed row with weak hotkey only |
| 12 | N/A | N/A | N/A | empty framed row with weak hotkey only |

背包首屏 8 件：

| slot | display text | iconVisualKey | quantity | source |
| --- | --- | --- | --- | --- |
| 1 | 药水 | `item.healing_potion.icon` | 2 | existing item |
| 2 | 卷轴 | `item.scroll_teleport.icon` | 3 | existing item |
| 3 | 钥匙 | `ui.hud.key.icon` or `icon.quest.armory_key` | 1 | existing icon |
| 4 | 金币 | `ui.hud.gold.icon` | 215 | existing UI icon |
| 5 | 红宝石 | existing item/material icon only if backed by real scenario item | 5 | real inventory entry |
| 6 | 草药 | existing item/material icon only if backed by real scenario item | 2 | real inventory entry |
| 7 | 蓝瓶 | `item.mana_potion.icon` | 3 | existing item |
| 8 | 紫宝石 | existing item/material icon only if backed by real scenario item | 1 | real inventory entry |

分页测试数据：

- 首屏 parity scenario 只放 8 件，避免 `1/2` 页码破坏 DEMO 对齐。
- `ui-demo-new-inventory-page-2.png` 使用单独 pagination fixture，额外加入第 9 件和第 10 件；该 fixture 只验证翻页能力，不作为首屏 golden 基准。

装备 empty socket：

| visual cell | source |
| --- | --- |
| `weapon` | actual `WEAPON` item icon; empty frame only when unequipped |
| `offhand` | actual `OFF_HAND` item icon; empty frame only when unequipped |
| `helm` | empty frame only |
| `armor` | actual `ARMOR` item icon |
| `cloak` | empty frame only |
| `gloves` | empty frame only |
| `amulet` | actual `ACCESSORY` item icon |
| `ring` | empty frame only |
| `boots` | empty frame only |

## 13. 附录 B：Sprite 验收 Rubric

每张重生资源必须通过本表；连续两张同类输出失败时，先改 prompt / sheet promptBase，再继续生成。

| asset | reject criteria |
| --- | --- |
| `ui.shell.map_stage.backdrop` | 出现清晰砖块、石板、墙体、actor、item、文字、水印；或在 mapStage 平铺后抢过 terrain 主视觉 |
| `tileset.ruins.ground_01` | 4 次平铺后出现明显重复接缝；32px 缩略图只有灰块看不出裂纹方向；边缘像现代网格 |
| `tileset.ruins.wall_01` | 顶部出现周期性砖墙条纹；与 ground 光源不一致；32px 下无法区分阻挡墙 |
| `actor.vanguard` | 64px 缩略图识别不出盾+剑或人物轮廓；32px 只剩暗点；主体占 cell 面积过小 |
| `prop.stairs.down` | 32px 下不像楼梯；方向不明确；亮度高到抢过 actor；与 ground 无法融合 |
| scenery props | 任何 prop 带文字、水印、UI 边框、过亮高饱和、或作为 floor decoration 时遮挡 actor/loot |

## 14. Review 反馈吸收记录

本节记录 external codex plan `UIUXfix.md` 与 `newUI2.md` 的反馈核实结果，避免执行时再次回到口头讨论。

| review item | 核实结论 | 吸收动作 |
| --- | --- | --- |
| UIUXfix P0-1 资源 sheet-plan 归属未明确 | 原反馈成立；随后 repo 已补齐 PR-02-2：`r02-ui-demo-ruins-tiles` / `r03-ui-demo-actor-props`、key-registry、owner contract、dark-v1 manifest 路径均已存在。 | §4 已从“新增 PR-02-2 owner”改为“重生已有 cell，刷新 PNG/hash/coverage/evidence”。 |
| P0-2 `shattered_outpost` / `vanguard` 可用性未验证 | 部分成立。repo 中两者已存在且数据完整，但固定 seed 形状仍需 smoke。 | §7 Phase 0 增加 zone/profession/source 验证和 seed smoke。 |
| P0-3 文本色仍偏冷 | 成立。当前 token 为 `DDDDDD` / `AAAAAA` / `777777`，圣经要求 `E7E1D3` / `AEB5BF` / `59616C`。 | §5.4 增加 text token 改造。 |
| P0-4 demo parity 判定主体缺失 | 成立。原 PR-02-1 有 reviewer 字段要求，新方案漏写。 | §11 增加 PR owner + 非 owner reviewer 和 manual record 字段。 |
| P1-1 warm overlay 参数缺失 | 成立。 | §5.3 增加暗角、玩家光晕、edge feather 初始值。 |
| P1-2 右栏高度比例缺失 | 成立。 | §5.2 已按新 DEMO 重新校准为装备高区、铭刻/背包紧凑区、操作提示低区。 |
| P1-3 背包分页状态归属不清 | 成立。 | §5.2 明确只有 `inventorySelection / 8` 一份页状态。 |
| P1-4 evidence 前置条件缺失 | 成立。 | §6 增加 evidence 前置条件表。 |
| P1-5 次级 props 策略缺失 | 成立。 | §5.3 增加 `secondaryPropDeferred[]` 策略。 |
| P1-6 暖色分布纪律缺失 | 成立。 | §5.4 增加 ember-gold 面积分布和 alpha 纪律。 |
| P2-1 ruins 其他 screen 回归 | 有价值。 | §10 增加至少 2 张 ruins 回归截图或 follow-up。 |
| P2-2 估时缺失 | 有价值。 | §7 增加 5-6 工作日估时表。 |
| newUI2 P0-1 资源现状误读 | 成立。当前仓库已有 PR-02-2 owner、sheet-plan、owner contract、dark-v1 PNG 和 manifest 指向。 | §4 / Phase 0 / Phase 1 全部改成复用既有 cell，不再要求新增 PR-02-2。 |
| newUI2 P0-2 backdrop 根因诊断不彻底 | 成立。backdrop 若是砖墙纹理，会抢过 terrain 主视觉。 | §4 增加 near-black radial fog prompt 和“backdrop 不覆盖 explored tile”的运行时约束。 |
| newUI2 P0-3 warm overlay / fog 规格丢失 | 已覆盖且保留。 | §5.3 明确 draw order、fog、vignette、玩家光晕、edge feather 参数。 |
| newUI2 P0-4 actor 不可见需先诊断 | 成立。当前代码有 `MAP_ACTORS` 绘制路径，但仍要区分 draw-call 缺失和 PNG 不可读。 | §5.3 / Phase 0 / 测试计划增加 actor draw-call 诊断。 |
| newUI2 P0-5 首屏 props 无 key/owner/层级 | 有价值，但不应无条件扩大 PR-02-2。 | §5.3 增加 PR-02-3 scenery mini-round 预案、6 个 key、层级和启用条件。 |
| newUI2 P0-6 铭刻/背包 demo data 不可枚举 | 成立。 | 附录 A 改为真实数据清单：铭刻 9-12 和装备展示空位只保留 empty socket，不允许 validation-only 假内容。 |
| newUI2 P0-7 装备 9 槽布局缺失 | 成立。 | §5.2 增加 9-cell ASCII 和 rule-slot 映射。 |
| newUI2 P0-8 双签纪律丢失 | 当前方案已覆盖。 | §11 保留 PR owner + 非 owner reviewer 双签。 |
| newUI2 P1-1 log deck 色阶 | 成立。 | §5.4 增加 zone/quest gold、正文 primary、路线 secondary，cyan 不用于普通 log。 |
| newUI2 P1-2 focus ring 颜色 | 成立。 | §5.4 改为 actor selection ring 使用 ember-gold，cold-cyan 只用于 UI 交互态。 |
| newUI2 P1-3 action deck 3 vs 4 | 不采纳 3-slot 化。用户明确要求至少 4 个技能槽。 | §5.1 明确 internal 4 slot bounds；首屏可 3 filled + 第 4 低强调 empty slot。 |
| newUI2 P1-4 draw order | 成立。 | §5.3 写回完整 draw order。 |
| newUI2 P1-5 首屏分页冲突 | 成立。 | §6 / Phase 3 改为首屏 8 件无页码，单独 pagination fixture 验证第 2 页。 |
| newUI2 P1-6/P1-7 text token 与暖色纪律 | 已覆盖且保留。 | §5.4 保留 text token、alpha 区间、ember-gold 分布纪律。 |
| newUI2 P1-8 hero depth badge | 有价值。 | §5.4 增加 crest 左下 depth/floor badge。 |
| newUI2 P1-9 mapStage 外框 | 成立。 | §5.4 增加四角/短 edge 装饰，禁止连续硬描边。 |
| newUI2 P1-10 evidence 清单 | 已覆盖，补充 page-2 流程。 | §6 维持 9 张 evidence + app.log，并区分 parity scenario 与 pagination fixture。 |
| newUI2 P1-11 ruins 回归 | 已覆盖，补充 grep 要求。 | §10 保留至少 2 个 tileset.ruins 场景截图 / golden crop。 |
| newUI2 P1-12 sprite 程序感标准 | 成立。 | 新增附录 B sprite reject criteria。 |
| newUI2 P2-4 validation scenario 代码点 | 有价值。 | §6 明确只改 `dark-uiux-pr02-1-demo-shell-foundation` scenario，不影响其他 validation 场景。 |

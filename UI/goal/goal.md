# K-ToME UI/UX Director-Grade Upgrade Goal

## 0. 预检摘要

- 当前分支：`codex/dark-uiux-pr07-golden-whitebox-polish`，工作区已有 PR07 相关未提交改动；本文件只新增目标与验证文档，不回退既有改动。
- 命中文档入口：`docs/INDEX.md` -> `UI/PLAN.md` -> `UI/pr/README.md` -> `UI/pr/screen-coverage-matrix.md`。
- 参考输入：`UI/UI-demo-new.png` 是第一眼质感标尺；`UI/review/open-design/ktome-dark-ui-design.md` 是辅助设计参考，不替代真实实现合同。
- 受影响模块：首轮以 `client` UI/render/input/presentation、`client` resources、`UI/sprite-sheets`、golden/manual/whitebox evidence 为主；不主动触碰 `core` 规则。
- 稳定合同：不改变玩法规则、save/replay/profile schema、manifest 结构权威和 content-pack 边界；不引入第二套 UI 资源 authority；所有长期产物使用 repo-relative path。
- 当前 P0 风险：本轮实跑 `:client:goldenScreenshot` 发现多处当前截图 hash 漂移，且部分 evidence 图像出现上下颠倒或镜像式文本翻转。该问题优先级高于审美微调，必须先归因并修复。
- 计划验证入口：`:client:clientSmoke`、`:client:goldenScreenshot`、`UI/pr/screen-coverage-matrix.md` 的必填 label、PR07 packaged whitebox/manual record、必要时追加 focused renderer/layout tests。

## 1. 目标

把 K-ToME 当前 dark UI/UX 从“已经有暗黑皮肤和部分资源替换”提升到“UI 总监和美术总监可接受的产品级首屏与全流程体验”。

核心不是复制 `UI/UI-demo-new.png`，而是达到同等质量层级：

1. 第一眼有统一产品气质，而不是拼贴式工程 UI。
2. 中央地图是主舞台，边栏和底栏服务战术判断，不抢占或污染地图。
3. 装备、铭刻、背包、技能、状态、任务、提示的视觉语言一致且可读。
4. 所有 required / conditional screen surface 都有可复验 evidence，不靠主观口头通过。
5. 资源、manifest、renderer、golden 和 manual record 形成闭环，不能为了让截图好看绕过合同。

## 2. 参考图拆解标准

`UI/UI-demo-new.png` 的可继承质量点：

1. **构图层级**：左 nav rail、中央 map stage、右侧 character/inventory panel、底部 hero/action/log deck 有清晰主次。
2. **材质统一**：黑铁、旧石、低饱和暗面和少量金色边线组成稳定材质，而不是大面积纯色面板。
3. **地图压迫感**：fog、局部暖光、石砖细节和暗边把地图做成第一视觉焦点。
4. **操作密度**：信息密集但分组清楚，玩家能扫读装备、铭刻、背包、HP、行动和日志。
5. **状态克制**：高亮、选择、危险、可操作状态都节制，避免整屏发光。
6. **小图可读**：装备、铭刻、背包图标在小尺寸仍可辨认，不依赖文字解释。

不继承的内容：

1. 不把参考图当精确像素稿。
2. 不把中文文字、数字或热键烘焙进图片。
3. 不为了视觉相似改变游戏规则、schema 或资源 authority。

## 3. 当前现状判断

基于本轮实际运行和截图观察，当前 UI 不满足 director-grade 标准，主要差距如下。

### 3.1 P0 阻塞

1. **截图/渲染方向异常**
   `client/build/reports/golden/dark-uiux-pr02-1/dark-uiux-pr02-1-demo-main-menu.png`、`client/build/reports/golden/dark-uiux-pr05-1/dark-uiux-pr05-1-inventory-workbench.png` 等 evidence 出现上下颠倒或镜像式文本翻转。该问题会使所有视觉验收失效。

2. **golden 基线大面积漂移**
   本轮 `:client:goldenScreenshot` 有 11 个 test group 失败。PR01-1、PR02、PR02-1、PR03、phase4-uiux-pr05、outcome、route、sample-pack 等截图 hash 都与当前固定期望不一致。必须先判断是预期改造未 rebaseline，还是渲染/capture 退化。

3. **`UI-demo-new` parity 当前画面退化**
   当前 `ui-demo-new-parity-1672x941` 与参考图并排看，地图舞台更像程序网格和低质 tile 拼接，暗部层次、墙体质感、暖光和 fog 压迫感明显弱于参考图。

### 3.2 P1 体验缺口

1. 地图舞台缺少参考图的局部照明、边界压暗和材质层次，第一眼视觉重心不够稳。
2. 右侧装备和铭刻区域有结构，但 spacing、slot 视觉权重和图标质量不稳定；部分状态读起来像 debug scaffold。
3. 底部 hero/action/log deck 的文字、图标、边框和背景层级还不够精致，局部有重叠/拥挤风险。
4. 主菜单、验证 setup、全屏背包 workbench 等非局内首屏当前 evidence 质量明显低于首屏目标，不能只优化 in-run shell。
5. 小图标家族之间的光源、密度、轮廓和材质统一性不足，需要用 contact-sheet 和 32/48/64px runtime preview 双重验收。

### 3.3 P2 质量提升方向

1. 增加 screen-by-screen 的 visual QA 评分，而不是只看 hash 稳定。
2. 为 map stage、right panel、bottom deck、modal/workbench 建立固定 crop 对比。
3. 对缺口资源先做 art direction brief，再生成候选资源，避免直接批量换图。
4. 建立“当前实现截图 -> 参考图/设计目标 -> 差异说明 -> 代码/资源入口 -> 验证入口”的闭环。

### 3.4 2026-05-24 当前进度

1. Capture 方向已在 golden harness 层归一化，当前 evidence 可用于 upright 人工查看；full golden 仍未通过，不能关闭 P0。
2. 已完成地图舞台材质 pass 1：降低高频 stage 网格、增强地面连续底色、加入连续暗面和可见房间 material wash。
3. 本轮 focused renderer tests 与 `:client:clientSmoke` 已通过；PR02-1 focused golden slice 仍因 expected hash 未验收而失败。
4. 已继续完成地图舞台 compositing pass 2/3：提高 runtime floor tile 权重、降低 visible fog 与舞台总暗罩、补 visible room seam softener、wall relief 和轻量 torch/prop atmosphere。
5. 当前首屏比初始 evidence 更接近可审查状态，地图 tile/墙面细节更清楚；但黑色 tile seam、墙体体积、局部灯光和底部/右栏信息密度仍未达到 `UI/UI-demo-new.png` 的 director-grade 标准。
6. 不接受当前 hash rebaseline；下一步优先处理资源级 dungeon floor/wall candidate 或更高阶 map compositing，而不是把现有格线画面固定为 golden。
7. 已完成资源级 r02 dungeon floor/wall candidate pass：生成候选图后拒绝直接采用的跨格墙体 spill，改为受控裁切进入 `r02-ui-demo-ruins-tiles`，并对 ground cell 做 edge-muted 处理以压低重复外圈暗边。
8. 资源 owner gate、focused renderer tests、`maintainabilityLint` 与 `:client:clientSmoke` 已通过；PR02-1 focused golden 仍按预期失败，只作为 latest visual evidence 和 hash 记录，不作为可接受 rebaseline。
9. 最新 map stage 相比上一版更有地牢材质和墙体可读性，但黑色 grid、墙顶重复性、局部光照层次和右/底栏密度仍未达到 UI Director / Art Director 验收标准。
10. 追加完成 map stage anti-grid pass：把 visible room 的完整规则线改成低透明断续石缝，给可见房间补轻量暖色 seam wash；降低 ground terrain 底图权重，增强 floor material 连续覆盖；压低 `DemoShellRenderer` 背景 64px 工程网格并改为更大、更弱的 ambient stonework。
11. 本轮 focused renderer tests、`maintainabilityLint`、`:client:clientSmoke` 通过；PR02-1 focused golden 继续失败，原因仍是 expected hash 未接收，不能作为 rebaseline 成功声明。
12. 视觉结论：这轮能降低外围背景网格和部分冷黑边界，但主房间仍偏棋盘化，和 `UI/UI-demo-new.png` 的手工石板、墙体体积、局部光照差距仍明显；下一轮优先资源级重生/重修 `tileset.ruins.ground_01` 与 `tileset.ruins.wall_01`，不要继续只靠遮罩堆叠固定当前画面。
13. 继续完成 r02 floor/wall hybrid resource pass：拒绝 v3/v4 的程序化块面和 v5/v6/v8 的噪点/透明像素问题，采用 v9 的低特征全不透明 floor base 与去全宽橙线 wall cap，写入正式 `r02-ui-demo-ruins-tiles` raw sheet 后通过 slice/contact/report 链路刷新 runtime 资源。
14. 本轮 `darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint` r02 owner gate 通过；`assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl` 只结构化替换 `tileset.ruins.ground_01` / `tileset.ruins.wall_01` 两条记录，保持 62 行全量 evidence。
15. 对 v9 后的 golden 继续做 map stage material alpha 收敛，降低逐格边框和裂缝矩形的支配权重；focused renderer tests 通过，PR02-1 focused golden 仍按预期 hash mismatch，不接受 rebaseline。
16. 视觉结论：重复斜裂缝和墙顶橙线已显著降低，主房间从“AI tile 棋盘”转向更连续的暗石面；但与 `UI/UI-demo-new.png` 相比仍偏平，cell grid 仍强，右栏/底栏信息密度和文字层级仍未达到 UI Director / Art Director 关闭标准。
17. 继续完成 room-level slab / mortar pass：把 visible room 的大面积 wash 降为辅助层，新增宽石板色块和低透明 mortar bridge，并提高 ground asset 原始绘制权重，避免地面被半透明绿黄覆盖压平成 debug grid。
18. 继续完成 r02 ruins tile resource replacement：拒绝过强中心大石块循环暴露的 candidate，采用更中性的裂石 floor 与更清晰的 wall cell 写入 `r02-ui-demo-ruins-tiles`，并通过 slice/contact/map-report 链路刷新 runtime 资源。
19. 本轮已接受 PR02-1 focused golden 的新基线，仅限 `UI-demo-new` 首屏 evidence：`ui-demo-new-map-stage-crop` 从 flat wash / 斜裂缝重复推进到可读暗石地面与墙体体积；这不是整个 UI/UX 目标关闭，右栏/底栏和 full-surface cohesion 仍需后续 director review。
20. 最新视觉结论：主舞台第一眼质感明显提升，地面不再由单一大裂缝或低对比暗块主导，墙体不再只有周期性横线；剩余差距是 cell seam 仍偏硬、局部火光和暗角层次仍弱于参考图，右栏/底栏信息密度仍需继续精修。
21. 继续完成 bottom HUD hierarchy pass：英雄卡增加暗底数值井、加高 HP/资源 gauge、把攻击/防御收进 chip、在纹章上叠加等级徽章；日志 deck 增加细 signal rail 并给正文让出扫描边距，避免重新拆成多行 card。
22. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800` 与 `ui-demo-new-bottom-deck-no-command-hints`，右栏、地图、导航、背包 crop 保持稳定。
23. 最新视觉结论：底部从工程拼接面板推进为有身份锚点、数值层级和日志扫描引导的 HUD；仍未关闭整个 UI/UX 目标，下一轮优先继续处理 right panel 信息密度、底部 action slot text/icon 层级，以及 map seam/局部光照。
24. 继续完成 right panel hierarchy pass：为装备区补 forged rig 背板和分区底材，让装备 socket 读成一个角色装备面而不是浮动图标；铭刻双列改为更明确的 forged rail 行，保留图标优先和热键扫读。
25. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 漂移 `ui-demo-new-parity-*`、`ui-demo-new-right-panel-grid` 与受同一右栏材质影响的 `ui-demo-new-inventory-page-1/2`，bottom、nav、map crop 保持稳定。
26. 最新视觉结论：右栏从“slot 漂浮在暗底上”推进到有装备 rig、分区底材和列表 rail 的角色面板；仍未关闭整个目标，下一轮优先处理 action slot label/icon hierarchy、operation hints 可读性和 map seam / player-light 层次。
27. 继续完成 action deck hierarchy pass：为底部技能区补连续 dark action tray，每个技能位增加 forged socket well、顶边锻造线和图标/文字连接 rail，使 action slot 从“图标 + 标签散件”推进为统一控制面。
28. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800` 与 `ui-demo-new-bottom-deck-no-command-hints`，right、nav、map、inventory crop 保持稳定。
29. 最新视觉结论：底部 action deck 已具备更明确的托盘层级和技能位材质；整个 UI/UX 目标仍未关闭，后续优先继续处理 operation hints 密度、map seam / player-light 层次，以及全量 surface cohesion。
30. 继续完成 operation hints compact matrix pass：把右下操作提示从三条 full-width 大文本行改为两列 key chip + muted label，保留所有快捷键但降低视觉权重。
31. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 漂移 `ui-demo-new-parity-*`、`ui-demo-new-right-panel-grid` 与受同一右栏操作区影响的 `ui-demo-new-inventory-page-1/2`，bottom、nav、map crop 保持稳定。
32. 最新视觉结论：右栏底部提示已从高亮文本块收敛为紧凑工具条，装备/铭刻/背包层级更稳；整个目标仍未关闭，后续优先处理 map seam / player-light / torch 层次，以及 Phase C 全量 surface cohesion。
33. 继续完成 map seam / torch / player-light pass：把 visible room seam softener 从细弱线提升为更宽的低透明 mortar band，并给玩家与火把补 compact local warm pool 和更亮玩家核心。
34. 光照改动限定在 visible material 范围/包围盒内，避免新火光照亮暗区形成大矩形光块；本轮只改 `client` renderer 与 focused tests，不改规则、schema、manifest authority 或正式资源。
35. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。视觉结论：地图局部光源和主角焦点更明确，黑色 seam 有所缓解；但主房间仍偏格子化，整个 UI/UX director-grade 目标仍未关闭，后续继续处理地牢房间轮廓、墙体体积和全量 surface cohesion。
36. 继续完成 room silhouette / hidden-grid suppression pass：对可见材质边界检测 hidden stage 邻居，向可见墙外侧投射 clipped soft shadow，压低暗区规则网格并让房间边界更像有体积的地牢空间。
37. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
38. 最新视觉结论：房间外沿压暗比上一版更稳定，暗区网格不再直接贴着可见墙面抢读；但参考图的墙体厚度、非矩形洞口、手工石板节奏仍明显更强，整个 UI/UX director-grade 目标仍未关闭。
39. 继续完成 main room cross-cell slab / grid dissolve pass：在 visible room 内部增加 wider low-alpha stone grout、director-scale cross-cell slab fields 和 seam-break overlay，把内部黑色 cell joints 从第一读数压成石材接缝。
40. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
41. 最新视觉结论：主房间内部从纯单格棋盘推进到更连续的石板/接缝层级；但参考图的手工石板破形、墙体厚度、洞口形状和全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
42. 继续完成 visible wall masonry mass pass：把相邻 visible wall tiles 的 floor-facing 边按连续 run 合并为水平/垂直 dark masonry band，并叠加低透明暖色磨损线，让墙边从逐格 relief 推进到连续厚墙读数。
43. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
44. 最新视觉结论：地图墙边界更像连续建筑体块，底墙和侧墙不再完全由单格 wall tile 支配；但参考图的非矩形洞口、暗区背景压制和全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
45. 继续完成 narrow passage threshold pass：识别被两侧墙/暗区夹住且沿通道方向延续的 visible floor tile，补连续 dark jamb、worn threshold cap 和低透明暖色磨损线，让房间到单格走廊的过渡不再像矩形房间被硬切。
46. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
47. 最新视觉结论：左侧通道口开始具备门洞/门槛读数，map stage 从完整矩形叠暗区进一步推进到有空间转折的地牢结构；整个 UI/UX director-grade 目标仍未关闭，后续优先处理暗区背景进一步弱化、墙角/洞口破形细节和 Phase C 全量 surface cohesion。
48. 继续完成 hidden stage grid suppression pass：在 stage texture 之后、visible room 绘制之前，按 `visibleClip` 对房间外的大块暗区补 clipped dark veil 和少量不规则暗面，压低背景规则网格的第一读数。
49. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
50. 最新视觉结论：暗区大块网格比上一版更退后，中央房间和左侧通道更像从黑暗中浮出；但参考图的墙角破形、手工石板节奏和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
51. 继续完成 visible room corner breakup pass：在 warm overlay 内按 `visibleRoomClip` 对可见房间四角增加 clipped dark masonry chip 和低透明 worn stone lip，让房间外轮廓从完整矩形推进到有破损墙角的 dungeon silhouette。
52. 本轮 focused renderer tests 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
53. 最新视觉结论：四角暗部和左侧洞口不再只读成规整矩形边框，调暗后的 stone lip 没有形成漂浮亮块；但参考图的手工石板节奏、墙面高差和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
54. 继续完成 staggered stone slab rhythm pass：在 visible room atmosphere 中新增错缝宽石板、短暗 mortar cut 与低透明磨损边，把主房间地面从均匀单格棋盘进一步推进到有手工石材节奏的 dungeon floor。
55. 本轮 focused renderer tests、完整 `TileRendererCanvasTest`、PR02-1 focused golden、`maintainabilityLint :client:clientSmoke` 与 `git diff --check` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
56. 最新视觉结论：主房间中部出现更明确的错缝石板和局部断线，黑色 grid 不再是唯一地面节奏；但参考图的墙面高差、地砖细节密度和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
57. 继续完成 raised wall crown / interior face pass：在 visible wall mass band 后新增连续墙冠与室内侧磨损面，按完整 visible wall run 绘制，但仍由 floor adjacency 决定是否落笔，让墙体从单层 tile 边推进到有厚度的 masonry boundary。
58. 本轮 focused renderer tests、完整 `TileRendererCanvasTest`、PR02-1 focused golden、`maintainabilityLint :client:clientSmoke` 与 `git diff --check` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
59. 最新视觉结论：房间顶部、底部和侧边墙体有更明确的连续压暗、暖色磨损边和立面读数，主角、loot、torch 与右/底栏仍可读；但参考图的细颗粒石砖密度、局部污迹和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
60. 继续完成 localized floor grime / broken stone detail pass：在 visible room atmosphere 的错缝石板和 cross-cell slab 后新增少量局部暗色破损、暖色 chipped edge、旧血/苔痕细节，把地面从均匀 overlay 继续推向有手工污损和破碎质感的 dungeon floor。
61. 本轮 focused renderer tests、完整 `TileRendererCanvasTest`、PR02-1 focused golden、`maintainabilityLint :client:clientSmoke` 与 `git diff --check` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
62. 最新视觉结论：主房间地面有更明确的局部 grime、stone scar、worn edge 和少量色彩污损，actor、loot、torch、右栏与底栏仍可读；但参考图的全量手绘石砖密度、墙角破形和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
63. 继续完成 wall-foot rubble / contact debris pass：在 raised wall face 后新增沿 visible wall/floor adjacency 的局部暗色碎石、暖色 stone chip 和墙脚脏边，让墙与地面不再只靠干净直线或黑色 grid 分隔。
64. 本轮 focused renderer tests、完整 `TileRendererCanvasTest`、PR02-1 focused golden、`maintainabilityLint :client:clientSmoke` 与 `git diff --check` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
65. 最新视觉结论：上下墙脚、左侧入口边缘和侧墙接触线出现更局部的 rubble/debris，房间边界更接近参考图的脏石墙脚读数；但主房间仍能看到规则 grid，Phase C 全量 surface cohesion 与更高密度手绘石砖仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
66. 继续完成 broken mortar cap / seam bridge pass：在 visible floor adjacency 上按稳定坐标节奏给内部接缝增加局部 stone-colored bridge、暗色缺口和暖色 chipped mortar cap，目标是继续削弱主房间“等宽等距网格”的第一读数。
67. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest` 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
68. 最新视觉结论：主房间内部 seam 出现更多断续、磨损和石材桥接，grid 读数比上一版更不机械；但整体仍未达到 `UI/UI-demo-new.png` 的手工石砖密度、墙面高差和全量 surface cohesion，整个 UI/UX director-grade 目标继续保持打开。
69. 继续完成 visible wall crown cap-block pass：在 raised wall face 后新增依赖 visible wall/floor adjacency 的离散 cap-stone block、暗色 notch 和侧墙小型 masonry nick，让长墙不再只读成连续半透明矩形带。
70. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden、`maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
71. 最新视觉结论：墙顶与侧墙出现更局部的块状亮暗变化，墙体高差略强且未压住 actor、loot、torch 或右/底栏；但参考图的墙体砖块密度、洞口破形和全量 surface cohesion 仍明显更强，整个 UI/UX director-grade 目标继续保持打开。
72. 继续完成 corridor mouth broken lintel pass：在 `drawVisiblePassageThresholds` 中识别单格通道接入宽房间的 mouth topology，为上下左右入口补更宽的暗石门楣、暖色磨损 lip 和局部破口，让走廊入口不再只像干净切开的 grid。
73. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden、`maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
74. 最新视觉结论：房间口和走廊 throat 的石质门楣更明显，入口边界从简单矩形开口推进到“被石墙包住”的读数；但全量 `GoldenScreenshotHarnessTest` 仍会因公共 map renderer 历史截图漂移失败，未在本轮做全 suite rebaseline，整个 UI/UX director-grade 目标继续保持打开。
75. 继续完成 asymmetric room-center veil pass：在 `drawVisibleRoomGridDissolve` 中补宽石面 veil、细 worn lip 和 off-grid shadow terrace，目标是让主房间中心先读成跨格石面与脏污层，而不是等距黑色 tile lattice。
76. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden、`maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
77. 最新视觉结论：主房间中心有更宽的非对称石面和低透明阴影层，内部 grid 第一读数继续下降，actor、loot、torch 与选中态未被压糊；但与参考图相比，墙面颗粒、暗区整体压迫感和全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
78. 继续完成 focal warm stone dropout pass：在 player 与 torch 的 compact warm-light 路径补局部暖石面 veil、短磨损 lip 和小暗边，让主角/火把光池先读成嵌入石材的光，而不是单纯叠在 grid lattice 上的发光矩形。
79. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden、`maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
80. 最新视觉结论：主角与近墙火把附近的暖光更像落在连续石面上，局部 grid seam 有退后趋势，actor、loot、目标框和右/底栏仍可读；但参考图的暗区压迫、墙砖颗粒和手工地砖密度仍更强，整个 UI/UX director-grade 目标继续保持打开。
81. 继续完成 visible wall masonry course pass：在 visible wall/floor adjacency 上新增小块砖面、短暗缝和侧墙竖向 course，让长墙从半透明整带进一步推进到有手工砖块颗粒的 masonry boundary。
82. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest` 与 PR02-1 focused golden 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
83. 最新视觉结论：房间顶部、侧墙和右侧墙面出现更细的砖块 course 颗粒，墙体读数比上一版更接近参考图的手工石墙；但暗区压迫、整体墙砖密度和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
84. 继续完成 hidden stage ambient pocket pass：在 stage texture 后、visible room 绘制前新增 lower-left deep pocket 与贴近房间外缘的弱暖暗遮断，让大块暗区不再只靠规则矩形网格读数支撑。
85. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
86. 最新视觉结论：左下暗区比上一版更像吞没房间边缘的黑暗材质，房间从 stage 中浮出的压迫感略强；但参考图的整体暗区非矩形层次、墙角破形密度和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
87. 继续完成 left-stage shoulder pocket pass：把新增暗区层从不影响当前 evidence 的 top/right 条件收敛到当前首屏左侧大暗区，补一块沿房间左肩横向展开的 clipped deep pocket，目标是继续削弱大面积规则背景网格。
88. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
89. 最新视觉结论：左侧暗区网格第一读数进一步退后，房间左缘的黑暗过渡更像材质深度而不是均匀遮罩；但参考图的全局非矩形暗区、墙角破形密度、细颗粒地砖和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
90. 继续完成 bottom hidden-stage void basin pass：在当前首屏房间下方暗区新增 clipped broad void basin 与贴近可见房间底缘的弱遮断层，让下方 backdrop 不再优先读成规则 stage grid。
91. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
92. 最新视觉结论：下方 hidden stage 更退入黑暗，中央房间、actor、torch、loot、target frame、右栏与底栏仍可读；但整体 UI/UX director-grade 目标仍未关闭，下一步继续处理全局非矩形暗区层次、墙角破形密度、细颗粒地砖和 Phase C 全量 surface cohesion。
93. 继续完成 visible room micro debris / joint plug pass：在可见房间地面补多点小碎石、短暖色磨损边，并对选定 grid intersection 加 off-grid stone plug，目标是压低长直黑色 lattice 的工程感。
94. 本轮两个 focused renderer test 均先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
95. 最新视觉结论：主房间地面获得更多小尺度破损和交叉点断裂，grid 第一读数略有下降且未破坏 actor/loot/target/torch 可读性；但参考图的整体手工石板密度、墙体高差和全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
96. 继续完成 visible wall vertical face / cast-shadow pass：在连续 visible wall run 的室内侧补更宽更深的 underface shadow 与轻微 worn lip，让墙体从薄 tile 边继续推进到有厚度的 raised masonry mass。
97. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest` 与 `maintainabilityLint :client:clientSmoke` 已通过；PR02-1 hash 已 scoped 接受，漂移仍只限 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。全量 `GoldenScreenshotHarnessTest` 类级运行仍因其它历史 golden drift 失败，本轮不把它们混入 rebaseline。
98. 最新视觉结论：房间上下长墙的下沿暗面更厚，墙体高差第一读数强于上一版且未破坏 actor/loot/torch/target/right/bottom 可读性；但参考图的整体砖块密度、非矩形暗区压迫和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
99. 继续完成 visible room seam erosion / slab mask pass：在 `drawVisibleRoomGridDissolve` 内新增三块 off-grid 低透明石面板和短边缘，用跨格石面遮断内部长直黑线，让主房间先读成破碎石板而不是 tile lattice。
100. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest` 与 `maintainabilityLint :client:clientSmoke` 已通过；PR02-1 scoped hash 已接受，漂移仍只限 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。全量 `GoldenScreenshotHarnessTest` 类级运行仍剩 12 个其它 golden drift 失败，本轮不扩大 rebaseline。
101. 最新视觉结论：主房间内部多了几处不贴网格的石面遮断，长直 lattice 第一读数继续下降，actor/loot/torch/target 仍可读；但参考图的全局手工石板密度、墙砖颗粒、暗区压迫和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
102. 继续完成 visible floor micro-etch detail pass：在 `drawFloorMaterial` 的 visible floor 基础材质内新增低透明暗色 hairline etch 与短暖色磨损边，让每个运行尺寸地砖获得细颗粒手工石面读数，而不是只靠大块 overlay 遮断网格。
103. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
104. 最新视觉结论：主房间 floor cell 内部多了一层非常克制的细刻线和磨损边，能提高石材密度且不压住 actor、loot、torch、target 或周边 UI；但参考图的手绘石板密度、墙体暗部高差、非矩形暗区压迫和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
105. 继续完成 wall corner buttress / return shadow pass：在 visible wall crown pass 内识别两个正交墙与对角 floor 形成的房间角，补 dark masonry return block 和短 worn lip，让房间角落从薄 tile intersection 推进到厚墙回折读数。
106. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
107. 最新视觉结论：可见房间角落出现更深的石墙回折阴影，墙体厚度和 dungeon silhouette 略强，actor、loot、torch、target 和周边 UI 未被压糊；但参考图的非矩形洞口破形、暗区压迫、墙面颗粒密度和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
108. 继续完成 map-stage upper-left vault veil pass：在 map stage backdrop shadow veil 中补一块偏左上的非矩形深暗层和克制石材磨损边，目标是让首屏左侧暗区从均匀黑色网格推进到更有层次的 dungeon depth。
109. 本轮先验证 visible-clip 右上/左上分支，但 PR02-1 focused golden 无漂移，说明该分支未触达当前首屏 evidence；随后收敛为 map-stage backdrop 断言并完成 RED/GREEN。完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
110. 最新视觉结论：左上/左侧 hidden stage 更压入暗处，房间从黑暗中浮出的第一读数略强，主房间、actor、loot、torch、target、右栏和底栏仍可读；但参考图的手绘石材密度、全局非矩形暗区层次和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
111. 继续完成 visible wall secondary masonry grain pass：在 `drawVisibleWallMasonryCourses` 的 visible wall/floor adjacency 上补 secondary dark mortar tick、短暖色 worn fleck 与侧墙细缝，让墙面从少量 course marker 推进到更密的手工石墙颗粒。
112. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
113. 最新视觉结论：房间顶部/底部墙面和侧墙获得更密的暗缝与暖色磨损点，墙体材质密度比上一版更接近参考图；但非矩形洞口破形、全局暗区压迫、细颗粒地砖整体密度和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
114. 继续完成 visible floor fine-grain density pass：在 `drawVisibleRoomLocalizedStoneDamage` 中补稳定坐标的 tiny pitted stone speck 与短 off-grid cut mark，让主房间地面不只依赖大块污迹和长 seam，而是在运行尺寸下获得更密的手工石面颗粒。
115. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
116. 最新视觉结论：主房间地面增加了一层克制的小颗粒、短划痕和暖色磨损点，能进一步弱化中心 lattice 的工程感且未压住 actor、loot、torch、target 或周边 UI；但全局非矩形暗区压迫、洞口破形深度和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
117. 继续完成 asymmetric stage void pressure pass：先在 `visibleClip` 右侧补 synthetic hidden-stage void shoulder，再把真实首屏会命中的 map-stage backdrop 增加 lower-right irregular void veil 与低透明石缘，让外围 stage 从均匀矩形网格继续退成非对称暗区。
118. 本轮两个 focused renderer test 均先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
119. 最新视觉结论：右下/右侧外围暗区更退后，中央房间从 stage 中浮出的压迫感略强，actor、loot、torch、target、右栏和底栏未被压糊；但参考图的资源级墙砖厚度、手工石材密度、洞口破形和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
120. 继续完成 recessed corridor aperture depth pass：在 `drawVisiblePassageThresholds` 的宽洞口路径中补低位 recessed throat shadow，让 corridor mouth 不只依赖 lintel/cap，而是先读成凿进厚墙的暗口。
121. 本轮 focused renderer test 先 RED 后 GREEN，完整 `TileRendererCanvasTest`、PR02-1 focused golden 与 `maintainabilityLint :client:clientSmoke` 已通过；accepted hash 只漂移 `ui-demo-new-parity-*` 与 `ui-demo-new-map-stage-crop`，right、bottom、nav、inventory crop 保持稳定。
122. 最新视觉结论：左侧/下方走廊口的暗部深度比上一版更稳定，opening 更接近 carved wall aperture；但首屏 map composition 仍明显偏右，左侧暗区占比过大，且参考图的资源级墙砖厚度、手工石材密度和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
123. 继续完成 map composition anchor pass：为 PR02-1/PR07 demo 场景新增专用 visual anchor，把可见房间质量中心拉回 map stage 中线附近，同时保留通用 `roomEntryPoint()` 语义入口不变。
124. 本轮 focused composition test、PR02/PR07 game scenario tests、PR02-1 focused golden、`maintainabilityLint :client:clientSmoke` 已通过；随后对全量 golden 漂移做独立 evidence review。
125. 完成 Phase A 全量 golden stabilization pass：抽查 PR01-1、PR02、PR03、PR05 当前 artifact，确认不是黑屏、翻转或明显裁切错误；将 `GoldenScreenshotHarnessTest` 的历史 expected hash 同步到当前 upright evidence 后，`:client:goldenScreenshot` 与 `verifyChanged` 均通过。
126. 当前状态：P0 evidence/golden gate 已收口，`ui-demo-new` 首屏构图也比上一轮更稳定；UI/UX director-grade 总目标仍未关闭，下一轮应继续 Phase B/Phase D 的资源级 wall/floor fidelity、非矩形 silhouette、光照/fog 压迫和 Phase C 全量 surface cohesion。
127. 继续完成 torch focal restraint pass：把 visible wall torch focal point 从最多 6 个收敛为最多 4 个，让火光回到局部焦点，而不是把整块 dungeon room 刷成大面积琥珀 wash。
128. 本轮新增 focused RED/GREEN test，约束大房间火把数量必须保持在 3-4 个 authored focal points；实现只改 `client` map renderer 的 torch candidate selection，不改资源、规则、schema 或 manifest authority。
129. 全量 golden 因共享 map lighting 行为受控漂移；抽查 PR02-1、PR01-1、PR03 shop、PR05 telegraph artifact 后接受 rebaseline，`:client:goldenScreenshot` 与 `verifyChanged` 均通过。
130. 最新视觉结论：火把噪声下降，首屏更接近参考图的“局部火光 + 大面积暗石压迫”语言；但主房间 grid、墙体厚度、地面手工密度和全量 surface cohesion 仍未达到 UI Director / Art Director 关闭标准。
131. 继续完成 actor grounding contact-shadow pass：在 actor sprite 绘制前补紧凑 floor contact shadow 和弱暖石缘，让角色、敌人、bonfire 等地图主体不再像直接贴在格子层上。
132. 本轮新增 focused RED/GREEN test，约束 actor footprint 下方必须出现紧凑暗色接触阴影和 material edge；实现只改 `client` renderer 的 actor draw path，不改 snapshot、规则、资源、schema 或 manifest authority。
133. 全量 golden 因共享 actor renderer 行为受控漂移；抽查 PR02-1、PR01-1 reference shell、PR05 telegraph surface 后接受 rebaseline，`:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 均通过。
134. 最新视觉结论：actor 与地面关系更稳，首屏主体不再完全漂在 tile lattice 上，telegraph/log/sidebar 可读性未被压坏；但主房间长直 grid、墙面资源级厚度、洞口破形和 Phase C 全量 surface cohesion 仍未达到 UI Director / Art Director 关闭标准。
135. 继续完成 irregular stone islands lattice-mask pass：在可见大房间 painterly floor pass 中增加跨行列 seam 的非网格石块岛与窄 worn lip，目标是让主房间第一眼先读到破碎石材 mass，而不是连续 debug-like tile lattice。
136. 本轮新增 focused RED/GREEN test，约束大房间必须出现两块 off-grid stone island 和 material lip；实现只改 `client` renderer 的 floor presentation，不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、content-pack、manifest authority 或正式资源。
137. 全量 golden 因共享 map floor renderer 行为受控漂移；抽查 PR02-1 map/full 与 PR05 telegraph surface 后接受 rebaseline，`:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 均通过。
138. 最新视觉结论：主房间中心和上半区多了跨格石材 mass，格线工程感进一步下降，actor、loot、telegraph、right panel 与 bottom deck 未被遮挡；但参考图级的手绘石材密度、墙体厚度、洞口/墙角破形和 Phase C 全量 surface cohesion 仍未达到关闭标准，整个 UI/UX director-grade 目标继续保持打开。
139. 继续完成 jagged aperture edge-bite pass：在可见房间已有 corner breakup 后补左右侧边和上墙 crown 的不规则暗色 bite，让主房间外轮廓从完整矩形进一步推进到被暗石侵蚀的 dungeon aperture。
140. 本轮新增 focused RED/GREEN test，约束大房间左右侧边必须出现深色 bite、短 material lip，顶部墙冠必须出现较宽 chipped crown bite；实现只改 `client` renderer 的 room edge presentation，不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、content-pack、manifest authority 或正式资源。
141. 全量 golden 因共享 map edge presentation 行为受控漂移；抽查 PR02-1 map/full 与 PR05 telegraph surface 后接受 rebaseline，`:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 均通过。
142. 最新视觉结论：房间左右边和上墙 crown 的直线矩形读感下降，暗部 aperture 更接近参考图的破碎石墙压力，actor、loot、telegraph、right panel 与 bottom deck 未被遮挡；但资源级 wall/floor hand-painted density、fog/lighting 压迫和 Phase C 全量 surface cohesion 仍未达到 UI Director / Art Director 关闭标准，整个 UI/UX director-grade 目标继续保持打开。
143. 继续完成 cool edge fog pressure pass：在 visible-room atmosphere 内补右侧冷暗雾压、左下不对称暗盆和克制 stone lip，让首屏从均匀暖光 wash 推向“局部火光 + 冷暗边界”的参考图光照语言。
144. 本轮新增 focused RED/GREEN test，约束右侧 room edge 必须有 cool fog pressure veil，左下地面必须有 broad dark basin 和 material lip；实现只改 `client` renderer 的 atmosphere presentation，不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、content-pack、manifest authority 或正式资源。
145. 全量 golden 因共享 visible-room atmosphere 行为受控漂移；抽查 PR02-1 map/full 与 PR05 telegraph surface 后接受 rebaseline，`:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 均通过。
146. 最新视觉结论：右侧和左下地面开始有更明确的冷暗 falloff，火把不再把整块房间均匀刷暖，actor、loot、telegraph、right panel 与 bottom deck 未被遮挡；但资源级 wall/floor hand-painted density、墙体高差和 Phase C 全量 surface cohesion 仍未达到 UI Director / Art Director 关闭标准，整个 UI/UX director-grade 目标继续保持打开。
147. 继续完成 hidden void chroma neutralization pass：把 hidden-stage suppression 的基础暗罩从旧绿黑 `050604` 收敛到 `05070A`，并把 secondary veil 从 moss tint `0B0E0B` 收敛到 cool-neutral `050607`，让暗区更接近参考图的 void-black/cold-stone 压迫，而不是绿色矩形幕布。
148. 本轮 focused test 先 RED 后 GREEN；实现只改 `client` map renderer 的暗区色温，不改 alpha、尺寸、位置、资源、规则、schema、manifest authority 或输入链路。全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 与 `git diff --check` 均通过。
149. 最新视觉结论：首屏左侧和下方 hidden stage 的 moss-green veil 感下降，房间从更中性的黑石暗部中浮出，actor、loot、telegraph、right panel 与 bottom deck 未被遮挡；但资源级 wall/floor hand-painted density、墙体高差、洞口破形和 Phase C 全量 surface cohesion 仍未达到 UI Director / Art Director 关闭标准，整个 UI/UX director-grade 目标继续保持打开。
150. 继续完成 operation hint caption / key-chip clearance pass：把右栏底部操作提示矩阵从 `SMALL` 文本层级收敛为 `CAPTION`，并把 key chip 高度降到 13px，避免 `Ctrl+S`、`1-4`、`5-8` 等快捷键在两列三行矩阵中读成重叠工程文本。
151. 本轮 focused test 先 RED 后 GREEN；实现只改 `DemoShellRenderer.drawCompactOperationCommandMatrix(...)` 的 caption text / key chip geometry 和 muted label tone，不改输入命令、localization 文案、layout model、资源、schema、manifest authority 或规则链路。全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 均通过。
152. 最新视觉结论：右栏 `操作提示` 从抢读的大号文本块推进为辅助 caption 工具条，装备、铭刻、背包和地图信息层级更稳；但地图资源级手绘密度、墙体高差、洞口破形、右栏/底栏整体 surface cohesion 和 Phase C 全量 screen polish 仍未达到最终关闭标准。
153. 继续完成 room-center high-relief stone facet pass：在大房间 painterly floor pass 内补 off-grid 高差石面、短暗 undercut 和克制 warm lip，让主舞台中心从 smoky tile lattice 继续推进到更清晰的手工石面读数。
154. 本轮新增 focused RED/GREEN test，约束中心石面必须有更高对比的 off-grid facet 与短暗切口；实现只改 `client` map renderer 的 floor presentation，不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、content-pack、manifest authority、资源或输入链路。全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 均通过。
155. 最新视觉结论：`ui-demo-new` 主房间中心多了一层更清晰的 stone relief，actor、loot、telegraph、right panel 与 bottom deck 未被遮挡；但参考图级 wall/floor hand-painted density、墙体高差、非矩形暗区压迫和 Phase C 全量 surface cohesion 仍未达到最终关闭标准，整个 UI/UX director-grade 目标继续保持打开。
156. 继续完成 wall-foot contact occlusion / masonry grit pass：在可见墙体脚边补破碎接触暗带、侧墙窄 occlusion run 和确定性 grit chip，让墙体从“贴在地板上的线条”继续推进到有重量的沉降石墙。
157. 本轮新增 focused RED/GREEN test，约束长墙脚必须把暗带压入相邻地面、侧墙必须有窄接触阴影、墙地交界必须有 deterministic grit chips；实现只改 `client` map renderer 的 wall presentation，不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、content-pack、manifest authority、资源或输入链路。全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 与 `git diff --check` 均通过。
158. 最新视觉结论：`ui-demo-new` 地图墙脚更有压地重量，底墙/侧墙和地面交界不再只读成规则格线，actor、loot、telegraph、right panel 与 bottom deck 未被遮挡；但整体仍偏雾化，参考图级 wall/floor hand-painted density、墙体块面高差、洞口破形和 Phase C 全量 screen polish 仍未达到最终关闭标准，整个 UI/UX director-grade 目标继续保持打开。
159. 继续完成 wall block face relief / interior masonry shadow pass：在连续可见墙体立面内补错缝 raised stone plate、暗色 mortar return 和侧墙纵向 stone plate，让长墙从“雾化的连续条带”继续推进到有堆砌块面高差的 masonry boundary。
160. 本轮新增 focused RED/GREEN test，约束水平墙面必须有 offset raised stone plate、暗 mortar return 和侧墙堆砌块；实现只改 `client` map renderer 的 wall presentation，不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、content-pack、manifest authority、资源、输入链路或 localization。完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 与 `git diff --check` 均通过。
161. 最新视觉结论：`ui-demo-new` 顶墙、底墙和侧墙出现更明确的块面高差与暗缝回折，墙体不再只靠整条 shadow band 读数，actor、loot、telegraph、right panel 与 bottom deck 未被遮挡；但整体仍偏雾化，参考图级 wall/floor hand-painted density、洞口破形、暗区压迫和 Phase C 全量 surface cohesion 仍未达到最终关闭标准，整个 UI/UX director-grade 目标继续保持打开。
162. 继续完成 visible wall chipped micro-joint / worn fleck pass：在既有 masonry course 和 raised wall face 之间补更小尺度的暗色 chipped joints、暖色 pin flecks 与侧墙竖向 micro chips，让墙面从“有大块面但仍偏平滑”继续推进到更接近参考图的手工石墙颗粒密度。
163. 本轮新增 focused RED/GREEN test，约束可见水平墙面必须有 compact chipped dark joint、长墙必须有 restrained warm pin flecks、侧墙必须有 narrow vertical chipped joint；实现只改 `client` map renderer 的 wall presentation，不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、content-pack、manifest authority、资源、输入链路或 localization。完整 `TileRendererCanvasTest` 与全量 `:client:goldenScreenshot` 已通过。
164. 最新视觉结论：`ui-demo-new` 顶墙、底墙、侧墙和 map-backed modal 背后的房间边界获得更多小尺度石缝与 worn fleck，墙体比上一版更有手工砌石密度，actor、loot、telegraph、modal、right panel 与 bottom deck 未被遮挡；但整体仍偏雾化，参考图级地面资源本体细节、洞口破形、暗区压迫和 Phase C 全量 surface cohesion 仍未达到最终关闭标准，整个 UI/UX director-grade 目标继续保持打开。
165. 继续完成 corridor mouth rubble teeth / aperture chip pass：在已有 broken lintel、recessed throat shadow 和 asymmetric bite 基础上，为 corridor mouth 的宽口边缘补 compact dark rubble teeth 与 restrained worn pin chips，让洞口边缘从“半透明矩形切口”继续推进到“被碎石咬出的厚墙开口”。
166. 本轮新增 focused RED/GREEN test，约束 corridor mouth 必须有不等高 dark rubble teeth 和至少两处 worn stone pin chips；实现只改 `client` map renderer 的 passage threshold presentation，不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、content-pack、manifest authority、资源、输入链路或 localization。完整 `TileRendererCanvasTest` 与全量 `:client:goldenScreenshot` 已通过。
167. 最新视觉结论：`ui-demo-new` 左侧、下侧和右侧 corridor mouth 的开口边缘更碎，洞口不再只靠平滑 lintel/shadow 读数，actor、loot、telegraph、right panel 与 bottom deck 未被遮挡；但整体仍偏雾化，参考图级地面资源本体细节、暗区压迫和 Phase C 全量 surface cohesion 仍未达到最终关闭标准，整个 UI/UX director-grade 目标继续保持打开。
168. 继续完成 visible floor fracture kernel / asset-detail clarity pass：在正式 r02 floor asset 已具备较好本体细节的前提下，针对运行尺寸下仍被 fog/overlay 柔化的问题，为每个 visible floor cell 内部补 compact dark fracture kernel、短竖向 cut 和 restrained worn edge，让地面本体细节更清晰，而不是只靠大块 overlay 破 grid。
169. 本轮新增 focused RED/GREEN test，约束 visible floor cell 必须有短暗裂核、竖向切口和暖色磨损边；实现只改 `client` map renderer 的 floor material presentation，不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、content-pack、manifest authority、正式资源、输入链路或 localization。完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot` 与 `maintainabilityLint :client:clientSmoke verifyChanged` 已通过。
170. 最新视觉结论：`ui-demo-new` 主房间 floor cell 内部多了一层更尖的短裂和磨损芯，地面本体细节在 fog/torch wash 下更不容易糊成单一格面，actor、loot、telegraph、modal、right panel 与 bottom deck 未被遮挡；但整体仍偏雾化，参考图级非矩形暗区压迫、资源级手绘密度和 Phase C 全量 surface cohesion 仍未达到最终关闭标准，整个 UI/UX director-grade 目标继续保持打开。

## 4. 改造原则

1. **先修 P0 证据可信度**：截图方向、hash 漂移和 evidence 生成链路没有稳定前，不做大规模 rebaseline。
2. **先主框架后细资源**：先让 shell、map stage、right panel、bottom deck 站住，再补图标和 ornament。
3. **先玩家主路径后边缘页**：in-run shell、主菜单、validation setup、inventory/workbench、outcome/error 按矩阵覆盖。
4. **美术资源必须服务运行尺寸**：所有图标/slot/portrait 在 runtime 32/48/64px 验收，source sheet 好看不等于游戏里可读。
5. **不引入第二真源**：新资源走 `UI/sprite-sheets/key-registry.yaml`、`UI/sprite-sheets/sheet-plan.yaml`、canonical manifest、runtime manifest 和 consumer test。
6. **不把审美问题伪装成规则问题**：client 只消费 typed snapshot 和 manifest；规则仍在 `core/game`。

## 5. 改造路线

### Phase A: Evidence Stabilization

目标：让截图 evidence 可信，确认当前 UI 退化来自 capture、layout、资源还是 expected hash 未同步。

交付：

1. 修复或确认 golden 输出方向问题。
2. 复跑 `:client:goldenScreenshot`，把失败项分成 expected rebaseline、真实 UI regression、capture bug 三类。
3. 更新 `UI/goal/goalTest.md` 的 test run 和 finding 状态。
4. 生成可读的 `ui-demo-new` side-by-side 和关键 crop evidence。

验收：

1. main menu、inventory workbench、demo shell evidence 均方向正确。
2. `:client:goldenScreenshot` 要么通过，要么每个失败项都有明确归因和对应修复任务。

### Phase B: First-Screen Visual Parity

目标：把 `ui-demo-new-parity-1672x941` 恢复并提升到参考图同级第一眼质量。

重点：

1. map stage 的 tile、fog、light、wall silhouette、selected actor highlight。
2. left nav rail 的图标尺寸、active state、竖向节奏。
3. right equipment/inscription/backpack 的 slot framing、icon scale、empty state 和 pagination。
4. bottom hero/action/log deck 的信息密度、文字层级和 command surface 去重。

验收：

1. `ui-demo-new-parity-1672x941` 与 `UI/UI-demo-new.png` 并排审查，UI Director / Art Director finding 无 blocking。
2. `ui-demo-new-map-stage-crop`、`ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints` 单独通过。

### Phase C: Full Surface Cohesion

目标：全量 required / conditional UI 面不再像不同阶段拼起来。

重点 screen：

1. Main menu / new run / validation setup。
2. Inventory workbench / item compare / shop / replacement modal。
3. Talent assign / active slot modal / passive detail。
4. Status / quest / skill overview / combat decision / inspect。
5. Runtime loading/error、UiErrorScreen、victory/defeat outcome、accessibility/settings。

验收：

1. `UI/pr/screen-coverage-matrix.md` 的 Required / Conditional 面都有当前 evidence。
2. PR07 final-all-screens index 能引用 golden、focused test、manual record、packaged whitebox evidence。

### Phase D: Art Resource Polish

目标：把资源质量从“可用”提升到“同一美术总监体系下的可用”。

资源策略：

1. 不直接批量替换全部资源。先用 P0/P1 finding 选出影响首屏和高频路径的关键资源。
2. 为 map tile、wall、fog/decal、UI chrome、slot frame、nav icons、equipment icons 分别写 prompt/QA brief。
3. 需要生成新资源时，先产候选和 contact sheet，经过 runtime preview 后再进入 manifest。
4. 所有正式资源必须走 style tag、sheet plan、key registry、manifest、consumer evidence。

验收：

1. 32/48/64px preview 无不可辨认图标。
2. contact sheet 无跨格、文字、水印、视角漂移、过噪或旧风格残留。

## 6. Director-Level Acceptance Rubric

每个关键 screen 用 0-3 评分，任一 P0 维度低于 2 不允许关闭目标。

| 维度 | 3 分标准 | P0/P1 |
| --- | --- | --- |
| Composition | 主次清晰，map/stage/panel/HUD 比例稳定 | P0 |
| Readability | 关键数值、状态、可操作对象一眼可读 | P0 |
| Visual Cohesion | 材质、光源、边框、图标密度统一 | P0 |
| Interaction Clarity | focus/selected/disabled/warning 不靠颜色单通道 | P0 |
| Runtime Evidence | golden/manual/whitebox/focused test 覆盖真实路径 | P0 |
| Art Quality | 资源无 AI-slop、旧风格残留、裁切错误 | P1 |
| Long-Session Comfort | 高亮克制，长时间看不疲劳 | P1 |

## 7. 非目标

1. 不在本目标中重做 gameplay rule、AI、地图生成或数值平衡。
2. 不改变 save/replay/profile schema。
3. 不引入 Lua、runtime script host、第二套 AI 或第二套 telegraph authority。
4. 不用一次性“大换皮”替代 evidence-driven 的 screen-by-screen 收口。
5. 不把 build 目录产物提交为长期合同；需要长期保存的结论写入 `UI/manual-records`、`UI/review` 或本目录文档。

## 8. 进度记录规则

后续所有 UI/UX 改造进度写入：

1. `UI/goal/goal.md`：目标、策略、验收标准和路线变更。
2. `UI/goal/goalTest.md`：每次命令、截图、白盒、manual review、finding、修复结果和剩余风险。

更新规则：

1. 每轮改造开始前补当前假设和目标。
2. 每轮验证后记录实际命令、结果和证据路径。
3. 失败不改写成通过；rebaseline 必须说明视觉变化原因。
4. director-level pass 必须引用具体 screenshot/crop/manual record。

## 9. 2026-05-26 Map Composition Anchor Pass

目标：

1. 继续 Phase B 的 `UI-demo-new` 首屏 map stage 质量收敛，优先处理上一轮留下的 P0：房间/玩家视觉重心偏右、左侧暗区占比过大。
2. 不改全局 `TileMapViewport` 小地图居中合同，不影响 `core/game` 规则、save/replay/profile/schema/content-pack/manifest authority。
3. 将构图问题落成 focused client renderer test，再做 PR02-1 scoped golden rebaseline。

实现边界：

1. `FoundationGameSession.prepareDarkUiuxDemoMapStageAnchor()` 改为使用 PR02-1/PR07 demo 专用 visual anchor；保留通用 `roomEntryPoint()`，避免影响 secret-zone、automation return point 等语义入口。
2. `TileRendererCanvasTest` 新增 PR02-1 composition test，走真实 validation scenario、真实 visual manifest 和 `TileRenderer` viewport，断言 visible room mass center 与 map stage center 漂移不超过 2 tile。
3. `GoldenScreenshotHarnessTest` 只更新 PR02-1 full parity 与 map-stage crop hash；right panel、bottom deck、nav、inventory crop 保持稳定。

验收状态：

1. `ui-demo-new-map-stage-crop` 的粗略亮度/饱和像素重心从上一轮约 `x=1142` 收回到约 `x=906`，crop 中线为 `x=841.5`。
2. 当前首屏 map composition 明显减轻左侧整块空暗场，但仍未达到参考图的资源级墙体厚度、手工石材密度和整体 art direction 完成度。
3. UI/UX director-grade 总目标仍未关闭；下一轮优先继续 Phase B/Phase D：资源级 wall/floor material fidelity、visible room silhouette break-up、torch/fog 层次和 Phase C full-surface cohesion。

## 10. 2026-05-26 Full Golden Stabilization Pass

目标：

1. 将上一轮 `verifyChanged` 暴露的全量 `:client:goldenScreenshot` 漂移从 PR02-1 scoped work 中拆出来单独验收。
2. 确认漂移不是截图方向、黑屏、裁切或资源加载机制问题，再决定是否接受当前 upright evidence 作为新基线。
3. 让共享 `verifyChanged` 入口重新闭环，为后续 UI/UX 迭代恢复可信回归面。

实现边界：

1. 只同步 `GoldenScreenshotHarnessTest` 中已有 golden expected hash，不新增 renderer 行为、schema、manifest authority 或 content-pack 合同。
2. 代表性人工复核覆盖 `dark-uiux-pr01-1`、`dark-uiux-pr02`、`dark-uiux-pr03` 与 `phase4-uiux-pr05` 当前 artifact；这些截图未见翻转、黑屏或明显裁切错误。
3. 这次 rebaseline 是 evidence stabilization，不等价于 UI/UX director-grade 目标关闭。

验收状态：

1. `:client:goldenScreenshot`：通过。
2. `verifyChanged`：通过。
3. 当前剩余缺口回到产品质量本身：资源级石墙/地面密度、非矩形暗区压迫、洞口破形、光照/fog 层次和全量 screen cohesion。

## 11. 2026-05-26 Torch Focal Restraint Pass

目标：

1. 继续 Phase B / Phase D 的 map stage 光照层级收敛，减少多火把造成的全局琥珀 wash。
2. 保留局部 torch/player focal light，让地图仍有战术焦点和暗黑地牢氛围。
3. 通过 focused renderer test 和全量 golden 证明共享 map renderer 改动没有破坏其它 screen surface。

实现边界：

1. 只将 `visibleTorchWallTiles(...)` 的可见火把焦点上限从 6 收敛为 4。
2. 不改 `core/game` 规则、地图生成、save/replay/profile/schema、content-pack、manifest authority 或正式图片资源。
3. 这次 rebaseline 接受的是共享 map lighting 行为的视觉变化，不代表整个 UI/UX 目标关闭。

验收状态：

1. 新增 focused test 先 RED 后 GREEN：大房间火把 flame core 必须保持在 3-4 个 authored focal points。
2. `ui-demo-new` 首屏 full parity 与 map-stage crop 更新；right panel、bottom deck、nav、inventory crop 保持稳定。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 均通过。
4. 下一步继续处理：主房间 long straight lattice、资源级 wall/floor hand-painted density、洞口/墙角破形和 Phase C 全量 surface cohesion。

## 12. 2026-05-26 Actor Grounding Contact-Shadow Pass

目标：

1. 继续 Phase B / Phase D 的 map stage runtime 质感收敛，优先处理 actor、enemy、bonfire 等主体与 tile floor 的贴合感。
2. 让 sprite 在地图上有稳定的 floor contact，而不是像无阴影贴图浮在 grid/lattice 之上。
3. 通过 focused renderer test、PR02-1 golden、full golden 和 owner gate 证明 shared actor renderer 改动可回归。

实现边界：

1. 在 `TileRenderer` 的 actor asset 绘制前增加 `drawActorGroundingShadow(...)`，基于已有 visual footprint / pivot 计算阴影，不新增模型字段或资源。
2. 阴影与 actor asset 同属 `MAP_ACTORS` 绘制路径，只是 draw order 前置；不改变 actor sorting、snapshot、AI、规则、save/replay/profile、schema、content-pack 或 manifest authority。
3. 这次 rebaseline 接受的是 shared actor presentation 行为的像素变化，不代表整个 UI/UX 目标关闭。

验收状态：

1. 新增 focused test 先 RED 后 GREEN：actor 下方必须有紧凑暗色 floor contact shadow 和弱暖色 worn-stone edge。
2. `ui-demo-new` full parity 与 map-stage crop 更新；right panel、bottom deck、nav、inventory crop 保持稳定。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 均通过。
4. 下一步继续处理：主房间 long straight lattice、资源级 wall/floor hand-painted density、洞口/墙角破形和 Phase C 全量 surface cohesion。

## 13. 2026-05-26 Irregular Stone Islands Lattice-Mask Pass

目标：

1. 继续 Phase B / Phase D 的 map stage 地面质感收敛，优先处理 `ui-demo-new` 主房间仍显著的长直 floor lattice。
2. 用少量跨格、非网格化石块岛压住连续横纵 seam，让运行尺寸下先读到手工石面 mass，再读到 tile structure。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared floor renderer 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleRoomPainterlyBreakup(...)`，新增两块低透明 stone island、一条暖色 worn lip 和一条暗色 cut。
2. 不新增模型字段、配置项、helper 抽象、资源、schema、manifest authority、content-pack 合同或临时 second authority。
3. 这次 rebaseline 接受的是 shared map floor presentation 行为的像素变化，不代表整个 UI/UX 目标关闭。

验收状态：

1. 新增 focused test 先 RED 后 GREEN：大房间必须出现跨行列 seam 的 off-grid stone island 与 narrow worn lip。
2. `ui-demo-new` full parity 与 map-stage crop 更新；right panel、bottom deck、nav、inventory crop 保持稳定。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 均通过。
4. 下一步继续处理：更大范围的资源级 wall/floor hand-painted density、非矩形 aperture/silhouette 破形、fog/lighting 压迫和 Phase C 全量 surface cohesion。

## 14. 2026-05-26 Jagged Aperture Edge-Bite Pass

目标：

1. 继续 Phase B / Phase D 的 map stage silhouette 收敛，优先处理 `ui-demo-new` 主房间左右边和上墙 crown 仍过直、过矩形的问题。
2. 在已有 corner breakup 基础上补侧边暗 bite、短 worn lip 与顶部 chipped crown，让 aperture 第一眼更像被暗石侵蚀的地下空间，而不是规则 UI viewport。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared map edge presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleRoomCornerBreakup(...)`，复用现有 `drawClippedRect(...)` 和 `visibleRoomClip` 约束侧边 bite。
2. 不新增模型字段、配置项、helper 抽象、资源、schema、manifest authority、content-pack 合同、地图生成逻辑或临时 second authority。
3. 这次 rebaseline 接受的是 shared map edge presentation 的像素变化，不代表整个 UI/UX 目标关闭。

验收状态：

1. 新增 focused test 先 RED 后 GREEN：大房间左右侧边必须出现 jagged aperture bite、material lip，顶部墙冠必须出现 chipped dark crown bite。
2. `ui-demo-new` full parity 与 map-stage crop 更新；right panel、bottom deck、nav、inventory crop 保持稳定。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 均通过。
4. 下一步继续处理：资源级 wall/floor hand-painted density、fog/lighting 压迫、墙体高差和 Phase C 全量 surface cohesion。

## 15. 2026-05-26 Cool Edge Fog Pressure Pass

目标：

1. 继续 Phase B / Phase D 的 map stage 光照和 fog 层级收敛，优先处理 `ui-demo-new` 主房间仍偏均匀暖光 wash 的问题。
2. 在可见房间内部补右侧冷暗 fog pressure、左下不对称 dark basin 和克制 stone lip，让火把焦点被冷暗边界托住，而不是整块房间同亮度铺开。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared visible-room atmosphere 改动可回归。

实现边界：

1. 只在 `TileRenderer.drawVisibleRoomAtmosphere(...)` 既有 pass 内补少量低透明 rect；不新增 helper 抽象、模型字段、配置项或资源。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、manifest authority、content-pack 合同或正式图片资源。
3. 这次 rebaseline 接受的是 shared map atmosphere presentation 的像素变化，不代表整个 UI/UX 目标关闭。

验收状态：

1. 新增 focused test 先 RED 后 GREEN：右侧 visible room edge 必须出现 cool fog pressure veil，左下地面必须出现 broad dark basin 与 restrained worn-stone lip。
2. `ui-demo-new` full parity 与 map-stage crop 更新；right panel、bottom deck、nav、inventory crop 保持稳定。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 均通过。
4. 下一步继续处理：资源级 wall/floor hand-painted density、墙体高差、全局 text/surface cohesion 和 Phase C 全量 screen polish。

## 16. 2026-05-26 Raised Wall Height Terrace Pass

目标：

1. 继续 Phase B / Phase D 的 map stage 墙体厚度收敛，优先处理 `UI-demo-new` 主房间墙体仍容易读成 tile border 的问题。
2. 在已有 raised wall face 基础上补长墙内侧 floor shadow terrace、窄 worn-stone ledge 和侧墙内向 occlusion strip，让墙体第一眼更像有高度差的厚石墙。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared wall presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleWallRaisedFaces(...)` 既有长墙 run pass；不新增 helper 抽象、模型字段、配置项或资源。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、manifest authority、content-pack 合同或正式图片资源。
3. 这次 rebaseline 接受的是 shared wall height presentation 的像素变化，不代表整个 UI/UX 目标关闭。

验收状态：

1. 新增 focused test 先 RED 后 GREEN：长墙必须向相邻 floor 投出 broad interior shadow terrace，lower wall 必须有 narrow ledge，side wall 必须有 slim interior occlusion strip。
2. `ui-demo-new` full parity 与 map-stage crop 更新；right panel、bottom deck、nav、inventory crop 保持稳定。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 均通过。
4. 下一步继续处理：资源级 wall/floor hand-painted density、全局 text/surface cohesion、Phase C 全量 screen polish 和真正资源级石材重制。

## 17. 2026-05-26 Authored Tile Material Clarity Pass

目标：

1. 继续 Phase B / Phase D 的 map stage 资源级可读性收敛，优先处理已有 ground/wall 资源细节被大面积 atmosphere wash 和规则 grid dissolve 压住的问题。
2. 让运行截图第一眼先读到 generated stone tile texture / cracks / hand-painted grain，再读到 fog、glaze 和 tile structure。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared map material clarity 改动可回归。

实现边界：

1. 只收敛 `TileRenderer` 既有 foundation glaze、visible-room atmosphere、grid dissolve 和 painterly wash 的 alpha；不新增资源、manifest key、helper 抽象、模型字段或配置项。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、manifest authority 或 content-pack 合同。
3. 这次 rebaseline 接受的是 shared map material clarity 的像素变化，不代表整个 UI/UX 目标关闭。

验收状态：

1. 新增 focused test 先 RED 后 GREEN：terrain asset opacity 必须保持主读；大面积 foundation glaze 降为 `0.082`，painterly wash 降为 `0.045`，internal grid dissolve 降为 `0.039`。
2. `ui-demo-new` full parity 与 map-stage crop 更新；right panel、bottom deck、nav、inventory crop 保持稳定。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 均通过。
4. 下一步继续处理：真正资源级石材重制、全局 text/surface cohesion 和 Phase C 全量 screen polish。

## 18. 2026-05-26 Operation Hint Restraint / Surface Cohesion Pass

目标：

1. 继续 Phase C 的 full-surface cohesion，优先处理右栏底部 `操作提示` 仍偏亮、偏抢读的问题。
2. 保留所有快捷键可发现性，但把 key chip、金色 accent、label 文字从主信息层降到辅助层，让装备、铭刻、背包和地图重新成为右栏主读信息。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared command hint presentation 改动可回归。

实现边界：

1. 只收敛 `DemoShellRenderer.drawCompactOperationCommandMatrix(...)` 和 command hint plate 的 alpha / text tone；不改输入命令、localization 文案、layout model、资源、manifest key 或 schema。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、content-pack 合同或正式资源。
3. 这次 rebaseline 接受的是共享 demo shell command hint 降权造成的像素变化，不代表整个 UI/UX 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：operation key chip 必须降到低权重 alpha，`Ctrl+S` 等快捷键文字必须可读但不能使用满强度 gold emphasis。
2. 人工抽查 `ui-demo-new` full/right-panel、PR03 shop replacement、Phase4 PR05 telegraph surface 后接受方向：右下提示仍可读，但不再压过铭刻/背包/地图信息层级。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 均通过；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED，`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 下一步继续处理：真正资源级石材重制、右栏/底栏 text scale 与 surface texture cohesion、Phase C 全量 screen polish。

## 19. 2026-05-26 Bottom Deck Caption Scale / Surface Cohesion Pass

目标：

1. 继续 Phase C 的 full-surface cohesion，优先处理底部日志和 action slot 标签仍偏大、偏工程化的问题。
2. 把 bottom log 与 action label 降到 caption 层级，让地图、英雄状态、技能图标和右栏信息重新成为主读层。
3. 保留中文长日志、技能热键和 slot label 的可读性，不通过删文案或隐藏快捷键解决拥挤。

实现边界：

1. 只新增 `TileTextStyle.CAPTION` 字体层级，并把 demo shell bottom log / action label 切到 caption；不改 layout model、输入命令、localization 文案、规则、schema、manifest authority 或正式资源。
2. 这次 rebaseline 接受的是共享底部文字层级变化造成的像素漂移，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：底部 action label 必须使用 `CAPTION`，bottom log 所有路线提示行也必须使用 `CAPTION`。
2. 人工抽查 `ui-demo-new` full/bottom crop、PR03 shop replacement 和 Phase4 PR05 telegraph surface 后接受方向：底部日志和技能标签更克制，未出现黑屏、翻转、裁切或字体缺失。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 已通过；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED，`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭，下一步继续处理真正资源级石材重制、底部/右栏 text scale 全量一致性和 Phase C screen polish。

## 20. 2026-05-26 Broad Mortar Veil / Lattice Suppression Pass

目标：

1. 继续 Phase B / Phase D 的 map stage 材质质感收敛，优先处理 `ui-demo-new` 主房间中心仍能一眼读成连续 debug-like tile lattice 的问题。
2. 在已有 staggered stone rhythm 基础上补更宽的 horizontal mortar veil、off-grid vertical shadow veil 与 restrained worn lip，让玩家附近长直网格缝被跨格石材层切断。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared map-grid presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleRoomGridDissolve(...)` 既有 visible-room overlay pass；不新增 helper 抽象、模型字段、配置项、资源、manifest key 或 schema。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享地图材质 overlay 的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：大房间必须绘制宽水平 mortar veil、非格点竖向 veil 和细 worn-stone lip，且锚定玩家附近长缝而不是单格内部。
2. 人工抽查 `ui-demo-new` full parity 与 map-stage crop 后接受方向：主房间长缝被跨格 stone/mortar 层打断，右栏、底栏、nav、inventory crop 中不触达 map stage 的证据保持稳定。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与最终 `verifyChanged` 已通过；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED。
4. 整个 UI/UX director-grade 目标仍未关闭，下一步继续处理真正资源级石材重制、墙体/地面 hand-painted density、fog/lighting 压迫和 Phase C 全量 screen polish。

## 21. 2026-05-26 Torch Glow Restraint / Cool Stone Pressure Pass

目标：

1. 继续 Phase B / Phase D 的 map stage 光照质感收敛，优先处理 `ui-demo-new` 可见房间仍被火把叠成 broad amber grid wash 的问题。
2. 保留火把和玩家作为局部焦点，但把火把 tile-by-tile glow 与 warm pool 从大范围琥珀矩形收回到更紧致的局部光源，让周围冷暗石材重新托住主舞台。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared torch lighting presentation 改动可回归。

实现边界：

1. 只调整 `TileRenderer.drawTorchLightBlooms(...)` / `drawTorchWarmPool(...)` 的既有半径、alpha 和尺寸；不新增 helper 抽象、模型字段、配置项、资源、manifest key 或 schema。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享地图火把光照的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. 新增 focused test 先 RED 后 GREEN：高 alpha 的 30px 琥珀 tile glow 必须保持局部，不能把火把光污染成全图网格；RED 计数为 `137`，目标上限为 `96`。
2. 更新 compact local light pool 断言并先 RED 后 GREEN：火把高 alpha warm pool 从约 `100x70` 收缩到约 `82x53`，避免读成宽琥珀矩形。
3. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 后接受方向：火把仍有局部焦点，但左侧和中轴 warm wash 明显收窄，周围 dark stone / fog pressure 更接近参考图的第一眼层级。
4. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 已通过；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED，`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
5. 整个 UI/UX director-grade 目标仍未关闭，下一步继续处理真正资源级石材重制、墙体/地面 hand-painted density、雾层边界的非矩形化和 Phase C 全量 screen polish。

## 22. 2026-05-26 Ragged Fog Teeth / Non-Rectangular Hidden Edge Pass

目标：

1. 继续 Phase B / Phase D 的 map stage 氛围质感收敛，优先处理可见房间左右边界仍像干净矩形 cutout 的问题。
2. 在既有 hidden-stage suppression 与 visible-room atmosphere 基础上，把暗部边缘做成窄而不对称的 ragged fog teeth，让黑暗像吞入房间边界，而不是一层直角遮罩。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared hidden-edge presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawHiddenStageGridSuppression(...)` 既有暗部遮罩 pass；不新增 helper 抽象、模型字段、配置项、资源、manifest key 或 schema。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享 map hidden-edge presentation 的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：左右边界必须出现不同尺寸和 alpha 的窄暗雾齿，同时不能覆盖 playable center。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 后接受方向：房间左右边缘不再只是矩形 veil，暗部进入方式更破碎，中心与右栏/底栏未被污染。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 与 `git diff --check` 已通过；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED，`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭，下一步继续处理真正资源级石材重制、墙体/地面 hand-painted density、fog shape 细节、右栏/底栏 surface cohesion 和 Phase C 全量 screen polish。

## 23. 2026-05-26 Hidden Void Chroma Neutralization / Moss Veil Restraint Pass

目标：

1. 继续 Phase B / Phase D 的 map stage 暗区氛围收敛，优先处理首屏 hidden stage 仍有偏 moss-green 的矩形雾块感。
2. 把 hidden-stage suppression 的暗罩色温从绿黑改为更接近 `void-black` / cool-stone 的中性黑，让暗区服务局部火光和房间轮廓，而不是形成绿色幕布。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared hidden-stage chroma 改动可回归。

实现边界：

1. 只收敛 `TileRenderer.drawHiddenStageGridSuppression(...)` 既有基础暗罩和 secondary veil 的 color token；不改 alpha、尺寸、位置、draw order、helper 抽象、模型字段、配置项或资源。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、schema、manifest authority、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享 map hidden-stage 色温变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：旧 moss tint `#0B0E0B` 不得继续出现在 hidden-stage secondary veil 中，并且必须保留 cool-neutral `#050607` secondary darkness pass。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 后接受方向：左侧和下方暗区绿色幕布感下降，首屏没有黑屏、翻转、裁切错误或 UI 面板污染。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 与 `git diff --check` 已通过；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED，`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭，下一步继续处理资源级 wall/floor hand-painted density、墙体高差、洞口破形、fog shape 细节和 Phase C 全量 surface cohesion。

## 24. 2026-05-26 Operation Hint Caption / Key-Chip Clearance Pass

目标：

1. 继续 Phase C 的 right-panel surface cohesion，优先处理 `ui-demo-new` 右栏底部 `操作提示` 仍像拥挤大号工程文本的问题。
2. 保留所有快捷键可发现性，但把 key chip 与 label 降到 caption 辅助层级，并确保两列三行矩阵的 chip 不再纵向重叠。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared operation hint presentation 改动可回归。

实现边界：

1. 只调整 `DemoShellRenderer.drawCompactOperationCommandMatrix(...)` 的 text style、chip height 和 muted label style；不改 layout solver、输入命令、localization 文案、资源、manifest key、schema 或规则层。
2. 不新增 helper 抽象、模型字段、配置项、compat path、second authority 或临时路径。
3. 这次 rebaseline 接受的是共享 demo shell operation hint presentation 的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：operation shortcuts 必须使用 `TileTextStyle.CAPTION`，并且 key chip 在 operation hint plate 内不得相互重叠。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 与 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：右栏提示仍可读，但视觉权重更接近参考图的辅助操作提示，而不是压过装备/铭刻/背包。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 已通过；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED，`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭，下一步继续处理资源级 wall/floor hand-painted density、墙体高差、洞口破形、右栏/底栏全量 surface cohesion 和 Phase C screen polish。

## 25. 2026-05-26 Room-Center High-Relief Stone Facet Pass

目标：

1. 继续 Phase B / Phase D 的 map stage floor fidelity，优先处理 `ui-demo-new` 主房间中心仍偏 smoky tile lattice 的问题。
2. 在已有 irregular stone island / painterly breakup 基础上，为玩家附近地面补更清晰的 off-grid 高差石面、短暗 undercut 和克制 warm lip，让中心区域先读成手工石材而不是半透明网格。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared map floor presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleRoomPainterlyBreakup(...)` 既有 floor presentation pass；不新增 helper 抽象、模型字段、配置项、资源、manifest key 或 schema。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享地图中心石面高差的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：大房间中心必须出现更高对比的 off-grid stone facet 和短暗 undercut，避免主焦点继续只读成 smoky tile lattice。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 与 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：地图中心石面清晰度略升，没有黑屏、翻转、裁切错误或右栏/底栏污染。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭，下一步继续处理资源级 wall/floor hand-painted density、墙体高差、非矩形暗区压迫和 Phase C 全量 surface cohesion。

## 26. 2026-05-26 Fine Aggregate Stone Grain Pass

目标：

1. 继续 Phase B / Phase D 的 map stage floor fidelity，优先处理 `ui-demo-new` 主房间地面近看仍缺少细骨料、坑点和微小磨损颗粒的问题。
2. 在已有 hairline etch、localized grime、high-relief facet 基础上，为每个 visible floor cell 增加低透明度的暗色 stone pit 与克制 warm fleck，让地面第一眼更接近手工石材而不是平滑雾层。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared floor material presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawFloorMaterial(...)` 既有 floor material pass；不新增 helper 抽象、模型字段、配置项、资源、manifest key 或 schema。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享地图细颗粒石面材质的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：visible floor cell 必须有多个 2px 暗色 aggregate pits，并有一条短 warm fleck，保证 runtime 尺寸下存在可回归的细石材颗粒。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：地面微颗粒密度上升，没有黑屏、翻转、裁切错误或右栏/底栏污染。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 与 `git diff --check` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭，下一步继续处理资源级 wall/floor hand-painted density、墙体高差、非矩形洞口压迫、右栏/底栏 surface cohesion 和 Phase C 全量 screen polish。

## 27. 2026-05-26 Asymmetric Passage Throat Shadow Bite Pass

目标：

1. 继续 Phase B / Phase D 的 map stage architecture fidelity，优先处理通道入口仍像规则矩形开口、缺少墙体厚度和暗部压迫的问题。
2. 在既有 passage threshold / broken lintel 基础上，为单格窄通道 throat 增加非对称暗咬边、对侧短 shadow bite 和小 warm worn nick，让门洞先读成厚石墙中被凿开的开口，而不是平面 grid slot。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared passage-threshold presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisiblePassageThresholds(...)` 既有 passage presentation pass；不新增 helper 抽象、模型字段、配置项、资源、manifest key 或 schema。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享通道 threshold 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：单格窄通道 throat 必须有非对称暗咬边、对侧短 shadow bite 和一条小 warm worn nick，避免洞口继续读成对称矩形 slot。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：通道暗部压迫略升，没有黑屏、翻转、裁切错误或右栏/底栏污染。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 与 `git diff --check` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭，下一步继续处理资源级 wall/floor hand-painted density、墙体高差、水平洞口破形、右栏/底栏 surface cohesion 和 Phase C 全量 screen polish。

## 28. 2026-05-26 Horizontal Passage Throat Shadow Bite Pass

目标：

1. 继续 Phase B / Phase D 的 map stage architecture fidelity，补齐上一轮 vertical throat polish 后仍缺失的水平洞口破形。
2. 在既有 horizontal passage threshold / broken lintel 基础上，为左右向通道 throat 增加上沿 offset dark bite、下沿 shorter shadow bite 和小 warm worn nick，让侧向门洞也读成厚石墙中被凿开的 aperture。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared passage-threshold presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisiblePassageThresholds(...)` 既有 horizontal passage presentation pass；不新增 helper 抽象、模型字段、配置项、资源、manifest key 或 schema。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享水平通道 threshold 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：水平 passage throat 必须有非对称暗咬边、对侧短 shadow bite 和一条小 warm worn nick，避免左右向洞口继续读成对称矩形 slot。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1280x800.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 与 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：水平洞口暗部压迫和石墙厚度略升，没有黑屏、翻转、裁切错误或右栏/底栏污染。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke`、`verifyChanged` 与 `git diff --check` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理资源级 wall/floor hand-painted density、墙体高差、右栏/底栏 surface cohesion 和 Phase C 全量 screen polish。

## 29. 2026-05-26 Right Panel Equipment Forged Rig Cohesion Pass

目标：

1. 继续 Phase C 的 right-panel surface cohesion，优先处理装备区仍像浮动 slot 矩阵、缺少一体化锻铁装备架结构的问题。
2. 在既有 equipment rig backdrop 基础上，为装备 slot 增加成对纵向 forged rail、低亮度 row tie bar 和小 rivet，使右栏顶部更接近参考图的 paper-doll rack，而不是单纯表格化 UI。
3. 通过 focused renderer test、full golden 和 owner gate 证明 shared right-panel equipment presentation 改动可回归。

实现边界：

1. 只扩展 `DemoShellRenderer.drawEquipmentRigBackdrop(...)` 既有 presentation pass；不新增 helper 抽象、模型字段、配置项、资源、manifest key 或 schema。
2. 不改 `core/game` 规则、snapshot、save/replay/profile、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享右栏装备区像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：装备区必须有成对 forged rail、多条 restrained row tie bar 和 worn rivet，确保 slot 作为同一装备架结构读取。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png` 后接受方向：新增结构笔触保持暗部克制，不抢文字和图标，不把右栏变成亮网格。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理右栏 inscriptions/backpack 的 surface cohesion、底栏 command/action cohesion、资源级 wall/floor hand-painted density 和 Phase C screen polish。

## 30. 2026-05-26 Right Panel Inscription / Backpack Forged Utility Rack Pass

目标：

1. 继续 Phase C 的 right-panel surface cohesion，补齐装备架之后右栏中段 `铭刻栏` 和下段 `背包` 仍偏 detached list / loose grid 的问题。
2. 让铭刻行读成同一 forged rune rack：两列行组由贯穿 ledger spine 和 worn rivet 连接，而不是只靠每行左侧亮条独立存在。
3. 让背包格子读成同一 pack tray：slot 背后有 restrained shelf rail 与 subtle leather / iron strap，弱化“漂浮 icon grid”。

实现边界：

1. 只扩展 `DemoShellRenderer` 的 right-panel presentation pass；不新增 layout model、render model 字段、配置项、manifest key、资源、schema 或 second authority。
2. 不改 `core/game` 规则、snapshot、save/replay/profile、content-pack 合同、输入命令或 localization 文案。
3. 这次 rebaseline 接受的是共享右栏铭刻/背包 presentation 的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：铭刻栏必须有贯穿多行的 column ledger spines 和 worn rivets，背包区必须有 shelf rails 与 straps。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`ui-demo-new-parity-1672x941.png`、`dark-uiux-pr03-inventory-stacked.png`、`dark-uiux-pr03-inscription-shop.png` 和 `phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：新增结构笔触收束右栏中下段，不遮挡文字/图标，不污染地图、bottom deck、nav 或 map crop。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理底栏 command/action cohesion、右栏 operation hint 进一步精修、资源级 wall/floor hand-painted density 和 Phase C screen polish。

## 31. 2026-05-26 Left Nav Rail Icon-First Forged Command Rail Pass

目标：

1. 继续 Phase C 的 first-glance shell polish，优先处理左侧 nav rail 过暗、过细、图标像贴在竖槽里的问题。
2. 对齐 Open Design 的 icon-first left navigation rail 方向：让五个导航图标读成一条锻铁命令 rail 上的主控锚点，而不是空面板里的小装饰。
3. 通过 focused renderer test、full golden 和 owner gate 证明共享 shell nav presentation 改动可回归。

实现边界：

1. 只调整 `DemoNavRailButtonLayout` 的 slot 尺寸计算和 `DemoShellRenderer` 的 nav rail presentation pass；不新增模型字段、配置项、manifest key、资源、schema 或 second authority。
2. 不改 `core/game` 规则、snapshot、save/replay/profile、content-pack 合同、输入 mode 语义、localization 文案或右栏/底栏布局。
3. 这次 rebaseline 接受的是共享 left-nav shell presentation 的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：nav 图标必须足够大，左栏必须有贯穿 forged backbone、每个图标的 shelf bar 和选中态 cyan halo。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-nav-rail-crop.png` 和 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png` 后接受方向：左栏存在感显著增强，没有遮挡地图、右栏或底栏。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke` 与 `verifyChanged` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理底栏 command/action cohesion、右栏 operation hint 进一步精修、资源级 wall/floor hand-painted density 和 Phase C screen polish。

## 32. 2026-05-26 Bottom Action Deck Forged Command Console Pass

目标：

1. 继续 Phase C 的 bottom HUD / command surface cohesion，优先处理底部 action deck 仍像三个独立 action card，而不是统一命令台的问题。
2. 在既有 action slot、slot label、deck frame 基础上，为 action slot 下方增加共享 command plinth、贯穿 forged rail、restrained cyan glint 与 worn rivet，让技能槽读成同一 forged command console。
3. 通过 focused renderer test、full golden 和 owner gate 证明共享 bottom deck presentation 改动可回归。

实现边界：

1. 只扩展 `DemoShellRenderer.renderActionDeck(...)` 既有 presentation pass；不新增 layout model、render model 字段、配置项、manifest key、资源、schema 或 second authority。
2. 不改 `core/game` 规则、snapshot、save/replay/profile、content-pack 合同、输入 mode 语义、localization 文案或 action legality。
3. 这次 rebaseline 接受的是共享底栏 action deck presentation 的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：action label 必须坐在共享 command plinth 上，slot 底部必须有贯穿 forged rail，并且每个 filled slot 有 worn rivet 锚点。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：action deck 更像一体化命令台，没有黑屏、翻转、裁切错误或右栏/地图污染。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理 bottom log panel / hero stats 与 action console 的同层整合、右栏 operation hint 精修、资源级 wall/floor hand-painted density 和 Phase C screen polish。

## 33. 2026-05-26 Bottom HUD Shared Forged Foundation Rail Pass

目标：

1. 继续 Phase C 的 bottom HUD surface cohesion，处理英雄卡、action deck、log deck 虽各自完成 polish，但仍像三张并排卡片的问题。
2. 在既有 bottom layout 不变的前提下，为三段底栏补共享 dark foundation slab、贯穿 forged top rail 和两处 restrained connector post，让底栏第一眼读成一条连续 HUD 梁。
3. 通过 focused renderer test、full golden 和 owner gate 证明共享 bottom HUD foundation presentation 改动可回归。

实现边界：

1. 只在 `DemoShellRenderer.renderShell(...)` 的 bottom section 绘制前增加共享 foundation presentation pass；不新增 layout model、render model 字段、配置项、manifest key、资源、schema 或 second authority。
2. 不改 `core/game` 规则、snapshot、save/replay/profile、content-pack 合同、输入 mode 语义、localization 文案或 action/log 业务语义。
3. 这次 rebaseline 接受的是共享底栏 foundation 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：hero/action/log 必须坐在同一个 dark foundation slab 上，顶沿必须有贯穿三段的 forged rail，并且两处 panel gap 有 restrained vertical connector post。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：底栏三段的后方暗底和顶沿连接更明显，文本、血条、技能图标没有被遮挡，地图和右栏没有污染。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理 bottom log panel 的文字层级/滚动密度、右栏 operation hint 精修、资源级 wall/floor hand-painted density 和 Phase C screen polish。

## 34. 2026-05-26 Bottom Log Compact Ledger / Scroll Density Pass

目标：

1. 继续 Phase C 的 bottom HUD surface cohesion，在共享底梁完成后处理 log deck 仍像带强 cyan 左条的窄文本盒、事件流密度不足的问题。
2. 在不改变日志内容、排序、业务语义或布局模型的前提下，把 bottom log 渲染成 compact event ledger：左侧短刻度组织多行事件，右侧 subdued scroll spine / lower thumb 表达可滚动密度，弱化整条 neon 边。
3. 通过 focused renderer RED/GREEN、full golden 和 owner gate 证明 bottom log presentation 改动可回归。

实现边界：

1. 只扩展 `DemoShellRenderer.renderLogAndStats(...)` / `drawLogDeckSurface(...)` 的 presentation pass；不新增 layout model、render model 字段、配置项、manifest key、资源、schema 或 second authority。
2. 不改 `core/game` 规则、snapshot、save/replay/profile、content-pack 合同、输入 mode 语义、localization 文案或 log event 业务语义。
3. 这次 rebaseline 接受的是 bottom log ledger / scroll-density presentation 的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：dense bottom log 必须有 compact ledger ticks、右侧 scroll-density spine 和 lower thumb marker，同时继续避免 stacked row plates。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-bottom-deck-no-command-hints.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-combat-action.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：log deck 更像事件账本，强 cyan 整条边被压低，文字仍为 caption 层级，没有遮挡地图、技能、右栏或 modal。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理右栏 operation hint 精修、资源级 wall/floor hand-painted density、hero stats 与 bottom log/action 的同层比例，以及 Phase C screen polish。

## 35. 2026-05-26 Right Operation Hint Forged Command Dock Pass

目标：

1. 继续 Phase C 的 right-panel surface cohesion，处理右下 `操作提示` 虽已压成 compact matrix，但仍像松散文字/按键 chip 贴在 plate 上的问题。
2. 在不改变快捷键、文案、输入语义或 layout model 的前提下，把 operation hints 渲染成一块嵌入右栏的 forged command dock：共享暗底、列间 command rail、keyplate rivet 同步出现。
3. 通过 focused renderer RED/GREEN、full golden 和 owner gate 证明共享 right operation hint presentation 改动可回归。

实现边界：

1. 只扩展 `DemoShellRenderer.drawCompactOperationCommandMatrix(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、snapshot、save/replay/profile/schema、content-pack、manifest authority、输入 mode 语义、localization 文案或右栏布局模型。
3. 这次 rebaseline 接受的是包含 operation hint plate 的共享 shell 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：right operation hints 必须坐在共享 dark command dock 上，两列快捷键必须有 forged vertical command rail，keyplate 必须有 worn rivet anchors。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inventory-stacked.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-combat-action.png` 后接受方向：operation hint 更像嵌入式 utility rack，没有遮挡右栏装备/铭刻/背包、底栏 action/log 或战斗提示。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 仍输出既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续资源级 wall/floor hand-painted density、hero stats 与 bottom log/action 的比例精修，以及 Phase C screen polish。

## 36. 2026-05-26 Wall-Foot Contact Occlusion / Masonry Grit Pass

目标：

1. 继续 Phase C 的 map-stage material density，处理墙脚和地面交界仍偏干净、部分长墙仍像规则线条贴在地板上的问题。
2. 在不改变地图、规则、资源或布局的前提下，为可见长墙补破碎 contact occlusion、侧墙窄 grit run 和确定性小碎石，让墙体读成有重量的黑石边界。
3. 通过 focused renderer RED/GREEN、full golden 和 owner gate 证明 wall-foot presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleWallFootRubble(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案。
3. 这次 rebaseline 接受的是共享 map wall-foot presentation 的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：长墙脚必须有压入相邻地面的 broken dark contact band，侧墙必须有窄 gritty occlusion run，墙地交界必须有 deterministic grit chips。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr05/dark-uiux-pr05-map-layer-stack.png` 和 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：墙脚更有压地重量，right panel、bottom deck、actor、loot 与 telegraph 没有被遮挡。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 与 `git diff --check` 已通过；全量 golden 在第一次执行时曾卡在 PR05 LWJGL `GLFW.glfwInit()`，中断后单独重跑 PR05 evidence 与全量 golden 均通过。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续资源级 wall/floor hand-painted density、墙体块面高差、洞口破形和 Phase C screen polish。

## 37. 2026-05-26 Focal Cool Undercut / Stone Clarity Pass

目标：

1. 继续 Phase C 的 map-stage material density，在 wall-face relief 后处理英雄与火把焦点区仍像暖雾覆盖 tile grid、局部石面清晰度不足的问题。
2. 在不改变地图、光源规则、actor、资源或布局的前提下，为既有 focal warm dropout 增加 compact cool undercut 和细窄 worn stone lip，让暖光切出更清楚的石面口袋。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 focal clarity presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawFocalWarmStoneDropout(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案。
3. 这次 rebaseline 接受的是共享 map focal-light presentation 的像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：player focal light 必须在暖色光池下方切出 compact cool undercut，并保留较细的 worn stone lip，避免英雄区继续读成 amber fog over grid。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png`、`client/build/reports/golden/dark-uiux-pr05/dark-uiux-pr05-map-layer-stack.png` 与 `client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-round1-chrome.png` 后接受方向：英雄/火把焦点区石面更冷、更清楚，telegraph、right panel、bottom deck、actor 与 loot 没有被遮挡。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`clientSmoke` 仍只有既有环境相关 SKIPPED，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理整体地图雾化、地面 grid 可见度、洞口破形、暗区压迫和资源级 hand-painted density。

## 38. 2026-05-26 Hidden-Stage Asymmetric Void Collar Pass

目标：

1. 继续 Phase C 的 map-stage darkness / silhouette polish，在 focal clarity 后处理可见房间外侧仍能读到规则 hidden-stage grid、房间像浮在背景格上的问题。
2. 在不改变地图、可见性、资源或布局的前提下，为 visible-room 外缘增加 compact dark collar、上下 asymmetric void crown 和细窄 worn-stone lip，让房间更像从黑暗里被切出。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 hidden-stage darkness presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawHiddenStageGridSuppression(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、可见性语义、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案。
3. 这次 rebaseline 接受的是共享 hidden-stage darkness / map focal silhouette 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：hidden stage 必须在 visible-room 左侧压入 compact dark collar，上缘必须有 asymmetric void crown，边缘必须保留 restrained worn-stone lip，且不能覆盖 player focal center。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：可见房间外缘更有暗区压迫，隐藏区 grid 不再同等抢眼，telegraph、right panel、bottom deck、actor、loot 与 shop modal 没有被遮挡。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`clientSmoke` 仍只有既有环境相关 SKIPPED，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理地面 grid 可见度、洞口破形、墙地资源级 hand-painted density 和 Phase C screen polish。

## 39. 2026-05-26 Visible Floor Heavy Slab Cap / Lattice Break Pass

目标：

1. 继续 Phase C 的 map-stage floor material density，在 hidden-stage 暗区压迫后处理可见房间内部长横/竖 floor grid 仍偏像调试 lattice 的问题。
2. 在不改变地图、地形、actor、loot、资源或布局的前提下，为 visible-room floor 增加 heavy off-grid horizontal slab cap、tall broken vertical slab cap 和细 worn lip，让中心地面更像破碎石板结构。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 visible floor lattice presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleRoomStaggeredStoneRhythm(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案。
3. 这次 rebaseline 接受的是 shared map floor presentation 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：可见地面必须用重型横向 slab cap 和竖向 broken slab cap 打断连续 lattice，cap 必须带窄 worn lip，且不能覆盖 player focal center。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：中心地面长格线被更多跨格石板断开，actor、loot、telegraph、modal、right panel 与 bottom deck 没有被遮挡。
3. 全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`clientSmoke` 仍只有既有环境相关 SKIPPED，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续洞口破形、墙地资源级 hand-painted density、地面 tile asset 本体细节和 Phase C screen polish。

## 40. 2026-05-26 Visible Wall Chipped Micro-Joint / Worn Fleck Pass

目标：

1. 继续 Phase C 的 map-stage wall material density，在 wall block face relief 后处理长墙仍有“有大块面但表面偏平滑”的问题。
2. 在不改变地图、地形、actor、loot、资源或布局的前提下，为 visible wall face 增加 compact chipped dark joints、restrained warm pin flecks 和 side-wall vertical micro chips，让墙面获得更接近参考图的手工砌石颗粒密度。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 wall micro-relief presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleWallMasonryCourses(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案。
3. 这次 rebaseline 接受的是 shared map wall presentation 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：可见墙面必须有 compact chipped dark joint，长墙必须有 restrained warm pin flecks，侧墙必须有 narrow vertical chipped joint。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：墙体边界有更多细碎石缝和 worn fleck，actor、loot、telegraph、modal、right panel 与 bottom deck 没有被遮挡。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`clientSmoke` 仍只有既有 3 个环境相关 SKIPPED，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理地面 tile asset 本体细节、洞口破形、暗区压迫和 Phase C 全量 surface cohesion。

## 41. 2026-05-26 Corridor Mouth Rubble Teeth / Aperture Chip Pass

目标：

1. 继续 Phase C 的 map-stage aperture breakup，在已有 broken lintel、recessed throat shadow 和 asymmetric bite 后处理洞口边缘仍偏平滑的问题。
2. 在不改变地图、地形、actor、loot、资源或布局的前提下，为 corridor mouth 宽口补 compact dark rubble teeth 和 restrained worn pin chips，让开口边缘更像被碎石咬出的厚墙。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 aperture chip presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisiblePassageThresholds(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案。
3. 这次 rebaseline 接受的是 shared map passage presentation 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：corridor mouth 必须有 compact dark rubble teeth、不等高 opposite-jamb tooth 和至少两处 worn stone pin chips。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 和 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：洞口边缘更碎，开口不再只靠平滑 rectangular lintel/shadow 读数，actor、loot、telegraph、right panel 与 bottom deck 没有被遮挡。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；全量 golden 第一次重跑曾在 PR05/LWJGL worker 无输出阶段挂起，终止该次 Gradle 后同一命令重跑通过；`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理地面 tile asset 本体细节、暗区压迫和 Phase C 全量 surface cohesion。

## 42. 2026-05-26 Visible Floor Fracture Kernel / Asset-Detail Clarity Pass

目标：

1. 继续 Phase C 的 map-stage floor material density，在 corridor mouth aperture breakup 后处理地面本体细节在运行尺寸下仍被 fog/overlay 柔化的问题。
2. 在不改变地图、地形、actor、loot、正式资源或布局的前提下，为 visible floor cell 内部补 compact dark fracture kernel、短竖向 cut 和 restrained worn edge，让每格地面有更清晰的手工石面读数。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 floor material clarity presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawFloorMaterial(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案。
3. 这次 rebaseline 接受的是 shared map floor material 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：visible floor cell 必须有 compact dark fracture kernel、short vertical cut 和 restrained worn edge。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：地面 cell 内部短裂与磨损芯更清晰，telegraph、modal、right panel、bottom deck、actor 与 loot 没有被遮挡。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理非矩形暗区压迫、全屏 surface cohesion 和资源级 floor/wall 手绘密度。

## 43. 2026-05-26 Hidden-Stage Staggered Void Cascade / Backdrop Grid Breakup Pass

目标：

1. 继续 Phase C 的 map-stage darkness / surface cohesion，在 visible floor fracture 后处理隐藏舞台左侧与底部仍能读出大面积规则矩形 grid 的问题。
2. 在不改变地图、可见性、actor、loot、正式资源或布局的前提下，为 visible-room 外侧隐藏区补 staggered deep void cascade、cross-axis void shelf 和短 restrained stone lip，让房间周围暗区更接近参考图的非均质黑场。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 hidden-stage backdrop presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawHiddenStageGridSuppression(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、visibility 语义、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源、输入 mode 语义或 localization 文案。
3. 这次 rebaseline 接受的是 shared hidden-stage darkness / map backdrop 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：left hidden stage 必须有 staggered deep void cascade，lower hidden stage 必须有 cross-axis void shelf，且两者不能覆盖 playable focal center。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inscription-shop.png` 后接受方向：左侧与底部隐藏舞台 grid 竞争继续降低，暗区更像错位黑场而不是规则背景格；telegraph、modal、right panel、bottom deck、actor 与 loot 没有被遮挡。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、contract preflight、boss/hidden/loot/mapgen 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理全屏 surface cohesion、资源级 floor/wall 手绘密度、右侧纯黑大场与参考图的层次差距。

## 44. 2026-05-26 Right Panel Charcoal Material Skin / Large Black Field Depth Pass

目标：

1. 继续 Phase C 的 full-screen surface cohesion，在 map-stage 暗区与 floor/wall 细节推进后处理右侧 equipment / inscriptions / backpack / operation 大面积黑场仍偏平涂的问题。
2. 在不改变右栏布局、slot 数量、输入、文案、资源、snapshot 或规则的前提下，为 right-panel section body 增加 restrained charcoal grain、old-leather striation 和 tiny soot flecks，让右栏从“黑色容器”推进到更接近参考图的暗材质面。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明右栏材质 presentation 改动可回归。

实现边界：

1. 只扩展 `DemoShellRenderer.drawRightSectionSurface(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、装备/铭文/背包数据、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案。
3. 这次 rebaseline 接受的是 shared right-panel surface 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：equipment section body 必须有宽幅低对比 charcoal grain，inscription section 必须有纵向 old-leather striation，backpack tray 必须有 tiny soot / worn-stone flecks。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-inventory-page-1.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-equipment-slots.png` 后接受方向：右栏大黑面不再完全平涂，equipment rig、铭文行、背包 slot、操作提示与中文文本没有被材质层压住。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、contract preflight、boss/hidden/loot/mapgen 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理右栏 equipment 上半区的 silhouette richness、map-stage 雾化/锐度、资源级 floor/wall 手绘密度和全屏第一眼焦点层次。

## 45. 2026-05-26 Right Panel Equipment Paper-Doll Scaffold / Silhouette Pass

目标：

1. 继续 Phase C 的 right-panel silhouette richness，在右栏材质层之后处理 equipment 上半区仍像 slot icon 漂在黑面上的问题。
2. 在不改变 equipment slot 布局、数量、装备数据、输入、资源、snapshot 或规则的前提下，为 equipment rig 补 restrained central armored torso shadow、broad shoulder mantle 和 paired side armor plates，让装备区第一眼更像纸娃娃装具架。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 equipment silhouette presentation 改动可回归。

实现边界：

1. 只扩展 `DemoShellRenderer.drawEquipmentRigBackdrop(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、装备/铭文/背包模型、地图生成、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案。
3. 这次 rebaseline 接受的是 shared right-panel equipment scaffold 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：equipment rig 必须有 central armored torso shadow、upper shoulder mantle 和 paired side armor plates，且这些形状只能作为低对比背景 silhouette，不能遮挡 slot/icon。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-equipment-slots.png` 后接受方向：equipment 上半区更有纸娃娃装具架轮廓，slot、icon、铭文、背包、操作提示、地图和底栏没有被遮挡。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、contract preflight、boss/hidden/loot/mapgen 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续处理 map-stage 雾化/锐度、资源级 floor/wall 手绘密度、右栏空 slot affordance 和全屏第一眼焦点层次。

## 46. 2026-05-26 Map-Stage Tactical Clarity Plane / Warm-Haze Cut Pass

目标：

1. 继续 Phase C 的 map-stage 雾化/锐度收敛，在 floor/wall/hidden-stage 多轮材质补强后处理中心可玩区仍被暖色 haze 柔化的问题。
2. 在不改变地图、可见性、actor、loot、正式资源、输入、snapshot 或规则的前提下，为 visible room 焦点区补 compact cool tactical clarity plane、dark undercut 和 restrained worn lip，让石面局部对比更接近参考图的清晰可读焦点。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 map-stage focus sharpness presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleRoomAtmosphere(...)` 下的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、visibility 语义、装备/铭文/背包模型、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案。
3. 这次 rebaseline 接受的是 shared map-stage focus clarity 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：visible room focus 必须有 cool tactical clarity plane、compact dark undercut 和 restrained worn lip，用来切掉中心暖雾感，而不是恢复完整 grid line。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr05/dark-uiux-pr05-map-layer-stack.png` 后接受方向：中心可玩区少了一层暖雾糊面，石面局部对比更清楚，actor、loot、telegraph、right panel 与 bottom deck 没有被遮挡。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、dark manifest、keyword registry、boss/hidden/loot/mapgen/solvability 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续资源级 floor/wall 手绘密度、右栏空 slot affordance 和全屏第一眼焦点层次。

## 47. 2026-05-26 Right Panel Empty Slot Hollow Socket / Affordance Pass

目标：

1. 继续 Phase C 的 right-panel empty slot affordance，在 equipment scaffold 与 section material skin 后处理空装备、空铭文、空背包格仍容易读成黑色占位块的问题。
2. 在不改变 slot 布局、数量、装备/铭文/背包数据、输入、资源、snapshot 或规则的前提下，为 empty slot interior 补 hollow socket center、inner top shadow、restrained worn lip 和小角部磨损，让空槽位第一眼读成“可放置目标”而不是死黑面。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明空槽位 affordance presentation 改动可回归。

实现边界：

1. 只扩展 `DemoShellRenderer.drawEmptySlotInterior(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、装备/铭文/背包模型、地图生成、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案。
3. 这次 rebaseline 接受的是 shared right-panel empty-slot 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：所有右侧 empty equipment / inscription / backpack slot 必须有可识别的 hollow socket center，证明空槽不是平面黑块。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-right-panel-grid.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 和 `client/build/reports/golden/dark-uiux-pr03/dark-uiux-pr03-inventory-empty.png` 后接受方向：空槽位有明确内凹、边缘压暗和轻量金属磨损；装备图标、铭文文案、背包项目、地图和底栏没有被遮挡。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、dark manifest、keyword registry、boss/hidden/loot/mapgen/solvability 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续资源级 floor/wall 手绘密度、全屏第一眼焦点层次、map-stage resource-level clarity 和右栏整体层级收束。

## 48. 2026-05-26 Floor Resource-Scale Chipped Stone Cluster Pass

目标：

1. 继续 Phase C 的资源级 floor/wall 手绘密度，在 aggregate / fracture / tactical clarity 多轮之后处理 floor cell 内部仍偏平涂、细节不够像参考图手绘石板的问题。
2. 在不改变地图、可见性、actor、loot、正式资源、输入、snapshot 或规则的前提下，为每个可见 floor cell 补 deterministic chipped-stone socket、cold stone fleck 和 restrained worn lip，让 32px 运行时尺寸下的石面更有局部破损和材质层次。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 floor material presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawFloorMaterial(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、visibility 语义、装备/铭文/背包模型、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案。
3. 这次 rebaseline 接受的是 shared map-stage floor material 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：visible floor cell 必须包含 compact dark chipped-stone socket、cold stone fleck 和 restrained worn lip，证明新增细节是资源级石面破损，而不是大面积雾化或恢复 debug grid。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 和 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：地面 chipped cluster 在 map stage 内可读，没有遮挡 actor、loot、telegraph、右栏或底栏。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、dark manifest、keyword registry、boss/hidden/loot/mapgen/solvability 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续全屏第一眼焦点层次、map-stage resource-level clarity 的整体观感、右栏整体层级收束，以及必要时继续 wall/floor 资源级手绘密度。

## 49. 2026-05-26 Player Focal Bracket / First-Glance Anchor Pass

目标：

1. 继续 Phase C 的全屏第一眼焦点层次收敛，在 floor/wall/resource-density 多轮后处理玩家位置仍只是平面 warm outline、首屏焦点不够像参考图中明确主角锚点的问题。
2. 在不改变地图、可见性、actor、loot、telegraph、输入、snapshot 或规则的前提下，为 player tile 增加 restrained forged warm corner brackets 与 compact dark underfoot shelf，让玩家格在 32px 运行时尺寸下更像可行动主角锚点，而不是普通格子描边。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 player focal indicator presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer` 中 `MAP_PLAYER_INDICATOR` presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、visibility 语义、actor/loot/telegraph 模型、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案。
3. 这次 rebaseline 接受的是 shared player focal indicator 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：player tile 必须包含四角 warm forged brackets 与低对比 underfoot shelf，证明焦点强化来自受控 presentation layer，而不是恢复强网格或扩大 selection 框。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 和 `client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-round1-chrome.png` 后接受方向：玩家格第一眼更清楚，corner brackets 克制，不遮挡 actor、loot、telegraph、右栏或底栏。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、dark manifest、keyword registry、boss/hidden/loot/mapgen/solvability 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续全屏焦点层次的整体收束、map-stage 资源级 clarity、右栏整体层级与必要的 wall/floor 手绘密度微调。

## 50. 2026-05-26 Hidden-Stage Neutral Void Ballast / Focal Cohesion Pass

目标：

1. 继续 Phase C 的全屏焦点层次和 map-stage resource-level clarity 收敛，在 player focal bracket 后处理可见房间外侧 left / bottom hidden-stage 仍有偏绿、偏规则的雾格抢读问题。
2. 在不改变地图、可见性、actor、loot、telegraph、输入、snapshot 或规则的前提下，为远左和底部 hidden-stage 增加 high-opacity cool-neutral void ballast，让中心石室更像从黑场中切出，而不是漂浮在规则雾格背景上。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 hidden-stage darkness presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawHiddenStageGridSuppression(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、visibility 语义、actor/loot/telegraph 模型、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案。
3. 这次 rebaseline 接受的是 shared map-stage hidden darkness 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：far-left hidden stage 必须有 `#010101` high-opacity neutral void ballast，lower hidden stage 必须有 broad neutral void ballast，并且两者不得覆盖 playable focal center。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png` 和 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：左/底 hidden-stage 更退后，中心石室、actor、loot、telegraph、右栏和底栏仍可读。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、dark manifest、keyword registry、boss/hidden/loot/mapgen/solvability 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续中心石室可读锐度、墙/地资源级手绘密度、右栏整体层级，以及必要时继续 map-stage 暗区非矩形压迫。

## 51. 2026-05-26 Visible Room Focal Stone Cutline / Center Sharpness Pass

目标：

1. 继续 Phase C 的中心石室可读锐度收敛，在 hidden-stage neutral void ballast 后处理 visible-room center 仍偏软、偏雾化、格面缺少手切石板锐度的问题。
2. 在不改变地图、可见性、actor、loot、telegraph、输入、snapshot 或规则的前提下，为 visible room focus 区补 short dark focal cutline、compact vertical bite 和 cool worn lip，让中心地面从软雾平面推进到更像参考图的手工石板。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 visible-room center sharpness presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleRoomTacticalClarityPlane(...)` 的 presentation pass，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、visibility 语义、actor/loot/telegraph 模型、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案。
3. 这次 rebaseline 接受的是 shared visible-room focus sharpness 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：visible room center 必须有 short dark focal cutline、compact vertical bite 和 cool worn lip，证明锐度来自受控 presentation layer，而不是恢复完整 debug grid。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png` 和 `client/build/reports/golden/phase4-uiux-pr05/phase4-uiux-pr05-telegraph-triple-surface.png` 后接受方向：中心石室局部切线更清楚，右栏、底栏、actor、loot、telegraph 与中文文本没有被遮挡。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、dark manifest、keyword registry、boss/hidden/loot/mapgen/solvability 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续中心石室整体手绘密度、墙体资源级锐度、右栏整体层级，以及 map-stage 暗区非矩形压迫。

## 52. 2026-05-26 Visible Wall Broken Capstone Silhouette / Wall Sharpness Pass

目标：

1. 继续 Phase C 的墙体资源级锐度收敛，在中心石室 stone cutline 后处理 visible wall crown 仍偏平、偏烟雾叠片、长墙轮廓不够像参考图手工垒砌石墙的问题。
2. 在不改变地图、可见性、actor、loot、telegraph、输入、snapshot 或规则的前提下，为长墙 crown 增加 heavy broken capstone slab、short worn lip 和 dark vertical cleft，让墙顶/墙脚从连续软带推进到更清楚的石块切面。
3. 通过 focused renderer RED/GREEN、完整 `TileRendererCanvasTest`、full golden 和 owner gate 证明 visible wall capstone presentation 改动可回归。

实现边界：

1. 只扩展 `TileRenderer.drawVisibleWallRaisedFaces(...)` 的 long-wall presentation branch，并同步 `TileRendererCanvasTest` 与 `GoldenScreenshotHarnessTest`。
2. 不改 `core/game` 规则、地图生成、visibility 语义、actor/loot/telegraph 模型、输入 mode、snapshot、save/replay/profile/schema、content-pack、manifest authority、正式资源或 localization 文案。
3. 这次 rebaseline 接受的是 shared visible-wall capstone 像素变化，不代表整个 UI/UX director-grade 目标关闭。

验收状态：

1. focused renderer test 先 RED 后 GREEN：long visible wall crowns 必须有 heavy broken capstone slab、short worn lip 和 dark vertical cleft，证明墙体锐度来自受控 presentation layer，而不是扩大整片雾化或恢复 debug grid。
2. 人工抽查 `client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-parity-1672x941.png`、`client/build/reports/golden/dark-uiux-pr02-1/ui-demo-new-map-stage-crop.png`、`client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-round1-chrome.png` 和 `client/build/reports/golden/dark-uiux-pr01-1/dark-uiux-pr01-1-viewport-deadzone-still.png` 后接受方向：墙顶/墙脚局部块面更重，右栏、底栏、actor、loot、telegraph 与中英文文本没有被遮挡。
3. 完整 `TileRendererCanvasTest`、全量 `:client:goldenScreenshot`、`maintainabilityLint :client:clientSmoke verifyChanged` 已通过；`verifyChanged` 因当前工作区已有多模块 UI/资源变更同时覆盖 resource-pipeline、dark manifest、keyword registry、boss/hidden/loot/mapgen/solvability 等 owner 入口，`verifyContractLintPreflight` 仍只有既有 `__stage_e_probe__` fallback warning。
4. 整个 UI/UX director-grade 目标仍未关闭。下一轮优先继续中心石室整体手绘密度、墙体/地面资源级材质密度、右栏整体层级，以及 map-stage 暗区非矩形压迫。

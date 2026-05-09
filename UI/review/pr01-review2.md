 深度审查报告 — dark-uiux-pr01-client-shell-layout

  审查角色：资深 Roguelike / 类 ToME 开发设计总监 + 系统策划总监 + 玩法体验审查负责人
  审查范围：当前 codex/dark-uiux-pr01-client-shell-layout 分支相对 main 的全部改动
  对照规格：UI/pr/dark-uiux-pr01-client-shell-layout.md、UI/manual-records/dark-uiux-pr01-shell.md
  评估时间：2026-05-07
  整体合格度：⚠️ 大体合规，但存在 3 处中级缺陷与 6 处可见的偏差，建议在 PR-02/PR-07 落地前先修复或显式立项

  ---
  1. 总览：要求与实际对应矩阵

  ┌─────────────────────────────────────────┬──────────────────────────────────────────────────────────────┬────────────────┬─────────────────────────────────────────────────────┐
  │                规格条目                 │                           实际实现                           │      状态      │                      关键文件                       │
  ├─────────────────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┼─────────────────────────────────────────────────────┤
  │ §3.1 GameShellLayout 输出四区域      │ GameShellLayout +                                    │                       │ client/.../render/layout/GameShellLayout.kt,            │
  │ bounds                               │ InfoSurfaceLayoutSolver.mapDominantMetrics 计算 left │ ✅ 实现               │ InfoSurfaceLayout.kt:34-85                              │
  │                                      │  rail/map/right panel/bottom HUD                     │                       │                                                         │
  ├──────────────────────────────────────┼──────────────────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────────────┤
  │ §3.2 左栏不承载规则状态              │ 左栏调用 drawLeftRail，仅展示                        │ ✅ 实现               │ TileRenderer.kt:658-672                                 │
  │                                      │ model.shell.leftRail.rows                            │                       │                                                         │
  ├──────────────────────────────────────┼──────────────────────────────────────────────────────┼───────────────────────┼─────────────────────────────────────────────────────────┤
  │ §3.3 右栏不重复 HP/Stamina           │ drawRightPanel 只画 model.shell.rightPanel，gauges   │ ✅ 实现               │ TileRenderer.kt:354-475, 674-688                        │
  │                                        │  仅在 drawHud 的 infoCard 里                       │                      │                                                        │
  ├────────────────────────────────────────┼────────────────────────────────────────────────────┼──────────────────────┼────────────────────────────────────────────────────────┤
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §3.5 Tooltip/Modal                │ 未在本 PR 覆盖，无 tooltip 渲染、无层级测试                        │ ❌ 偏差           │ —                                               │
  │ 同一层级、不遮日志                │                                                                    │                   │                                                 │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §3.6 Talent 四态 token            │ UiTalentToneColors + StandaloneScreenLayoutTest 锁定具体 hex       │ ✅ 实现           │ UiDesignTokens.kt:96-101,257-263,               │
  │ (locked/learnable/reserve/active) │                                                                    │                   │ StandaloneScreenLayoutTest.kt:55-62             │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §3.7 standalone screen 共享       │ StandaloneScreenChrome + DarkStandaloneScreenLayout                │ ✅ 部分实现（见   │ StandaloneScreenLayout.kt                       │
  │ primitive                         │                                                                    │ §3）              │                                                 │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §3.8 首页 action stack            │ MainMenuController 提供 QuickStart / Continue / Validation / Exit  │ ✅ 实现           │ MainMenuController.kt:17-92                     │
  │ 三件套+continue disabled+复制详情 │ / CopyContinueErrorDetail                                          │                   │                                                 │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §3.9 角色创建固定宽高             │ UiFixedDimensionTokens.playerCreationSection* +                    │ ⚠️ 部分实现（见   │ PlayerCreationPanel.kt:11-95                    │
  │                                   │ PlayerCreationPanel.fitText                                        │ §3）              │                                                 │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §3.10 Validation Mode 一等入口    │ MainMenuAction.ValidationMode、key ui.menu.action.validation       │ ✅ 实现           │ MainMenuController.kt:91-92, i18n L14           │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §3.11 ValidationSetupScreen 接入  │ 已使用 UiDesignTokens.color.* + 双列布局                           │ ✅ 实现           │ ValidationSetupScreen.kt:82-128                 │
  │ dark token                        │                                                                    │                   │                                                 │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §3.12 GameOver/Victory/UiError    │ 三屏均使用 DarkStandaloneScreenLayout.outcome() +                  │ ⚠️ 接入但内容超出 │ GameOverScreen.kt、VictoryScreen.kt、UiErrorScr │
  │ 接入 standalone token             │ StandaloneScreenChrome                                             │ 框（见 §3）       │ een.kt                                          │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §3.13 ASCII fallback 只保留语义   │ 分支未改 AsciiRenderModelTest / AsciiRenderModel                   │ ⚠️ 未验证（见     │ —                                               │
  │                                   │                                                                    │ §4）              │                                                 │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §5.1 1280×800 四区域不重叠+底部仅 │ 几何已自动 require 不重叠；底部唯一性靠人工记录，无 unit 断言      │ ⚠️ 部分实现       │ GameShellLayoutTest.kt,                         │
  │ 一套生命/耐力/经验                │                                                                    │                   │ TileRendererCanvasTest.kt                       │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §5.2 最小窗口中文不越界           │ 几何用 coerceAtLeast(184f/240f)；文本截断仅按字符数估算（approxima │ ⚠️ 偏差           │ TileRenderer.kt:931-940                         │
  │                                   │ teCharWidth=18f 对中文偏窄）                                       │                   │                                                 │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §5.3 地图视口以玩家附近内容为主   │ 没有滚动/居中逻辑；map 以 mapWidth × cellWidth 整张铺出            │ ❌ 偏差           │ —                                               │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │ §5.10 桌面标题/locale             │ 人工记录 §Findings.2 自承 continue-unavailable 复制详情含本机 save │ ⚠️ 偏差           │ manual-records/...md:79-80                      │
  │ 不含本机绝对路径                  │  path                                                              │                   │                                                 │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────┼───────────────────┼─────────────────────────────────────────────────┤
  │                                   │ manual record 仅跑了                                               │                   │                                                 │
  │ §6 验证命令                       │ packageMacApp/StandaloneScreenLayoutTest/maintainabilityLint；未跑 │ ⚠️ 验证不完整     │ manual-records/...md:24-44, 81-85               │
  │                                   │  clientSmoke/goldenScreenshot/verifyChanged                        │                   │                                                 │
  └───────────────────────────────────┴────────────────────────────────────────────────────────────────────┴───────────────────┴─────────────────────────────────────────────────┘

  ---
  2. 设计架构层评价（从总监视角）

  2.1 优点

  1. GameShellLayout 把 shell 几何收敛到不可变值对象，并在构造器里 require(!overlaps)。这是纯函数式 layout，是后续 PR-02~07 接 sprite/talent 的好基座。
  2. TileShellModel 把"左栏/右栏/底部 hint"从 renderer 渲染期解耦到 model 期，便于无头测试和未来切换渲染后端。
  3. StandaloneScreenChrome + DarkStandaloneScreenLayout 给 standalone 屏定义了统一的 background/header/primaryActionStack/secondaryPanel/disabledDetail/footer 六块
  slot，比纯坐标硬编码进步明显。
  4. Talent 四态 hex 在 StandaloneScreenLayoutTest 中被锁定 (#59616C/#1CB7C8/#D99A2B/#52C989)，对 PR-04 是硬合同 —— 这是规格 §3.6 "PR-04 只消费不重新发明" 的强约束实现，做得到位。
  5. Validation setup 双列降级（>9 行自动两列）是对中文长行 + 1280×800 真实约束的好响应；测试覆盖到等高 step、列数、不重叠。

  2.2 系统性缺陷

  缺陷 A（中级）：standalone chrome 与具体 screen 内容的"两套坐标系"已经开始漂移

  DarkStandaloneScreenLayout 给出一套面板矩形，但 MainMenuScreen 同时维护一套自己的硬编码 Y：MAIN_MENU_TITLE_Y=472、MAIN_MENU_PANEL_TOP_Y=370、MAIN_MENU_FOOTER_LANGUAGE_DEFAULT_Y=100
  、MAIN_MENU_FOOTER_CONTROLS_DEFAULT_Y=72、MAIN_MENU_FOOTER_NOTICE_DEFAULT_Y=42（MainMenuScreen.kt:26-42）。两者已经不一致：
  - footerHelp 面板矩形 = (80, 28, 800, 44)，top=72
  - 实际文字 baselineY：language=100、controls=72、notice=42
  - → language 文字渲染在 footerHelp 面板上方（y=100 > top=72），落到了 disabledDetailArea（y=82-118）的视觉区域里，footer 矩形其实只兜住 controls 与 notice 各一行的一部分

  这导致 StandaloneScreenChrome.draw 画出来的 footer 边框与 disabledDetailArea 边框，跟实际文本位置错位。设计上 chrome 应该是 1:1 框住内容，现在不是。后续 PR-02 接 sprite chrome
  时会跟着错。

  缺陷 B（中级）：TileRenderer "拆分"流于表面

  规格 §3 第 4 项："拆分 TileRenderer 的 map、left rail、right panel、bottom HUD、tooltip/modal 绘制职责"。

  实际拆分情况：
  - TileLayerComposer.kt 只有 7 行，是 4 个 list 的 addAll（TileLayerComposer.kt:1-12）
  - TileRenderModelBuilder 把 model 构造提出来了 ✅
  - 但 TileRenderer.kt 仍是 38 KB / 988 行单文件，所有 drawHud / drawMessages / drawLeftRail / drawRightPanel / drawFooterHints / drawCursor / drawGroundLootMarkers /
  drawCombatFeedback / drawPaneFocusRing 全部仍在 TileRenderer.companion object 里，是 80~120 行的过程式函数堆叠
  - 没有 TileShellRenderer / TileHudRenderer / TileMapRenderer / TileOverlayRenderer 之类的对象拆分
  - 也完全没有 tooltip 渲染相关的代码

  这违背了规格"renderer 拆分"职责，且是 PR-02~PR-07 后续工作的一根脏脊梁。manifest review 与维护成本会继续累积。

  缺陷 C（中级）：地图视口未实现"以玩家附近为主"的 §5.3 必测

  InfoSurfaceLayoutSolver.mapDominantMetrics 把 mapWidthPx = mapWidth × cellWidth 整张当作 map 区域宽度（InfoSurfaceLayout.kt:36-85），renderer 在 drawPlacement / drawCursor
  里也是直接 mapOffsetX + x*cellWidth（TileRenderer.kt:763-786）。整个渲染路径没有 viewport / camera-on-player / scroll 概念。

  后果：
  1. 当 dungeon metadata.width 大于 1280 / 32 - 13 ≈ 27 cells 时，右侧/上侧地图就直接超出 standalone 视口
  2. 玩家移动到地图边缘时也不会有 viewport 跟随
  3. GameShellLayoutTest 只验证了 mapWidth=24 与 mapWidth=18 两个静态尺寸；当前 foundation zone 多大、未来 PR-04 talent tree zone 多大都未约束

  如果 PR-01 范围内确实只考虑 ≤24 列地图，应在 acceptance contract 显式声明并加 require(mapWidth ≤ N)；否则这是个隐藏的 production 风险。

  缺陷 D（轻级）：menuWidth=960f / menuHeight=540f 与 token 重复定义

  MainMenuScreen.kt:24-25、UiErrorScreen.kt:16-17 自带常量；UiDesignTokens.fixed.standaloneWidth/Height 也是 960/540。两处都暴露 risk：以后改 token 不会同步到 menuWidth。建议直接
  private val menuWidth = UiDesignTokens.fixed.standaloneWidth 收敛。

  ---
  3. 玩法/体验层 — 各屏深入审查

  3.1 MainMenuScreen — 主菜单

  正面：
  - Action stack 命中"Quick Start / Continue / Validation / Exit"四档；Continue 三态都通过 controller.entries(continueAvailability) 走 disabled color
  - 复制错误详情通过 MainMenuAction.CopyContinueErrorDetail，不阻塞主操作 ✓
  - MainMenuScreenTextTest 较扎实，覆盖了 footer 不挤压 entry stack、long profession 截断、locale 切换、unavailable note 等

  问题：

  ┌──────┬────────┬─────────────────────────────────────────────────────────────────────────────────────────────────────────────┬──────────────────────────────────────────────────┐
  │ 编号 │ 严重度 │                                                    描述                                                     │                       证据                       │
  ├──────┼────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │      │        │ PlayerCreationPanel 的 profession heading 在 y=370，落在 secondaryPanel.top=374 上方 4px：title             │ MainMenuScreen.kt:247 +                          │
  │ M1   │ 中     │ 字体高度会跨过面板顶边；race title 同样位于 secondaryPanel 上方                                             │ PlayerCreationPanel.kt:107 +                     │
  │      │        │                                                                                                             │ StandaloneScreenLayout.kt:136                    │
  ├──────┼────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │      │        │ profession 列（x=80）整段位于 primaryActionStack (x=80, w=320) 内部，与 action entries (y=238 起)           │ PlayerCreationPanel.kt:93,116,                   │
  │ M2   │ 中     │ 共用同一矩形 → profession note 行（y=262）距 entry 0（y=238）只有 24px，仅 1 行字距，长 note + 4 entries    │ MainMenuScreen.kt:32-34                          │
  │      │        │ 时视觉粘连                                                                                                  │                                                  │
  ├──────┼────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │      │        │ 当 entries 数 ≥ 5（continue unavailable + copy detail + quick + validation + exit）时，第 5 个 entry        │ MainMenuScreen.kt:44-67,                         │
  │ M3   │ 中     │ baseline = 370 - 132 - 4*32 = 110f，已 跌出 primaryActionStack.y=126，叠到 footer                           │ MainMenuScreenTextTest.kt:32-48                  │
  │      │        │ 字底；MainMenuScreenTextTest 只测了 entryCount=3,4，没有覆盖 5                                              │                                                  │
  ├──────┼────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────┤
  │ M4   │ 轻     │ language baseline=100 渲染在 footerHelp 面板（top=72）之上、disabledDetailArea 区域内 → footer              │ MainMenuScreen.kt:38,256 +                       │
  │      │        │ 框白看，语言提示视觉归错区域                                                                                │ StandaloneScreenLayout.kt:138                    │
  └──────┴────────┴─────────────────────────────────────────────────────────────────────────────────────────────────────────────┴──────────────────────────────────────────────────┘

  3.2 ValidationSetupScreen — 验证模式入口

  正面：
  - 双列布局 + per-column 截断 ✓
  - 14 个 entry（12 设置 + 3 action）已经被人工记录 PASS_AFTER_FIX ✓
  - 文案、controls、active pack summary 都接入 token ✓

  问题：

  ┌──────┬────────┬────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │ 编号 │ 严重度 │                                                                              描述                                                                              │
  ├──────┼────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ V1   │ 轻     │ ValidationSetupScreen.kt:107-108 用 layout.secondaryPanel.top - 12f / top - 36f 画 presetSummary 与 activePackSummary — 两行 24px 间距属于硬编码字距，未走     │
  │      │        │ token，将来若改字号会错位                                                                                                                                      │
  ├──────┼────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ V2   │ 轻     │ entry list 颜色"focused = focusRing / 否则 textPrimary"；不区分 disabled action（如 sample pack toggle 不可用时仍是同色），但 spec §5.8 要求 setup             │
  │      │        │ 页能感知不可用                                                                                                                                                 │
  ├──────┼────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ V3   │ 轻     │ notice 渲染在 disabledDetailArea.top baseline，可能字体顶部跨过 detailArea 顶边（同 M1 问题，影响小）                                                          │
  └──────┴────────┴────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

  3.3 GameOverScreen / VictoryScreen / UiErrorScreen — 三个 outcome 屏

  正面：
  - 已使用 DarkStandaloneScreenLayout.outcome() + StandaloneScreenChrome ✓
  - 颜色全部走 token：GameOver 用 telegraph.high、Victory 用 status.badge.stack、UiError 用 telegraph.high ✓ → 满足 §3.12 / §5.9 "去掉旧红绿白临时风格" 的形式要求

  问题：

  ┌──────┬────────┬──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┬────────────────────────────────────────┐
  │ 编号 │ 严重度 │                                                         描述                                                         │                  证据                  │
  ├──────┼────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────┤
  │ O1   │ 中     │ UiErrorScreen 第一个 action label 渲染在 y = layout.header.top - 22 - 40 - 60 = 370，但 primaryActionStack.top =     │ UiErrorScreen.kt:58-72,                │
  │      │        │ 396；后续 action 才进入 stack。意味着第 1 行 action 标签飘在 stack 上沿之上，视觉上 actions 不在 actions 框里        │ StandaloneScreenLayout.kt:158          │
  ├──────┼────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────┤
  │ O2   │ 轻     │ GameOverScreen / VictoryScreen body lines 从 primaryActionStack.top - 18 = 378 起，每行 26px。长 OutcomeSummary（10+ │ GameOverScreen.kt:62-66,               │
  │      │        │  行）会跌穿 primaryActionStack.y=112，进而落到 disabledDetailArea（y=72-106）甚至 footer 上，没有最大行数截断保护    │ VictoryScreen.kt:62-66                 │
  ├──────┼────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────┤
  │ O3   │ 轻     │ 三屏 truncate(line, 80) 用字符数硬截断，对中文（更宽）不安全                                                         │ 同上                                   │
  ├──────┼────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────┤
  │ O4   │ 轻     │ UiErrorScreen 仍自定义 errorScreenWidth=960f / errorScreenHeight=540f，没用 token                                    │ UiErrorScreen.kt:16-17                 │
  └──────┴────────┴──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┴────────────────────────────────────────┘

  3.4 局内 shell（TileRenderer）

  正面：
  - 四区域 bounds 通过 GameShellLayout.init 强制不重叠，是硬合同 ✓
  - 底部 hotbar、infoCard、logCard、focusCard 在同一 bottomHudBounds 内分配，没有再到右栏复制血条 ✓
  - pane focus ring 通过 PaneFocusAnchor 三选一画在合理位置 ✓
  - messageRows = 5、uiRows = 7 与 shellBottomHudHeight = 224f（=7×32px）数值耦合一致 ✓

  问题：

  ┌──────┬────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┬───────────────────────────────────┐
  │ 编号 │ 严重度 │                                                           描述                                                            │               证据                │
  ├──────┼────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
  │ S1   │ 中     │ 缺 §5.3 玩家居中 viewport（详见缺陷 C）                                                                                   │ —                                 │
  ├──────┼────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
  │ S2   │ 中     │ 缺 §3.5 tooltip/modal 层级；当前没有任何 tooltip 渲染入口与测试                                                           │ TileRenderer.kt 全文搜不到        │
  │      │        │                                                                                                                           │ tooltip                           │
  ├──────┼────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
  │ S3   │ 轻     │ GameShellLayoutTest 只断言不重叠 + 最小宽度阈值；没有断言：(a) bottom HUD 内部"仅一套 HP/Stamina/EXP"，(b) right panel    │ GameShellLayoutTest.kt            │
  │      │        │ 不再画 gauge，(c) hotbar 永远位于 bottom HUD 之内                                                                         │                                   │
  ├──────┼────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
  │ S4   │ 轻     │ TileRendererCanvasTest 覆盖了 left/right panel 文字落在 bounds 内、focus ring 颜色等，但未覆盖 spec §"slot                │ TileRendererCanvasTest.kt:86-133  │
  │      │        │ 固定尺寸"、"tooltip/modal 层级"                                                                                           │                                   │
  ├──────┼────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
  │ S5   │ 轻     │ surface.overlay（α=0.93）画 left/right rail，surface.raised（α=0.96）画 bottom HUD —— 视觉一致性可接受，但 4 个区域使用 3 │ TileRenderer.kt:266-281           │
  │      │        │  种 surface（base/overlay/raised），UiSurfaceColors 选用规则未文档化                                                      │                                   │
  └──────┴────────┴───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┴───────────────────────────────────┘

  ---
  4. 测试与验证缺口

  ┌──────┬────────┬────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │ 缺口 │ 严重度 │                                                                              说明                                                                              │
  ├──────┼────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ T1   │ 中     │ AsciiRenderModelTest 未在本 PR 修改（git diff 不含该文件），但规格 §"影响范围"明确列入。规格 §5.4 "ASCII fallback 不丢关键日志/状态" 缺乏 PR 内归约            │
  ├──────┼────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ T2   │ 中     │ manual record 自承未跑 clientSmoke / goldenScreenshot / verifyChanged（manual-records/...md:81-85 "Residual Risk"），而规格 §0 / §6 把这三个列为 owner         │
  │      │        │ gates。当前合规依据不完整                                                                                                                                      │
  ├──────┼────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ T3   │ 轻     │ 没有 entry count = 5 的覆盖（M3）；没有"min window + 长中文"的端到端文本越界 unit test                                                                         │
  ├──────┼────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ T4   │ 轻     │ GoldenScreenshotHarnessTest 哈希被批量更新，但没有针对 PR-01 新增的 dark-uiux-pr01-* 标签的专属 golden 锁；现有是 in-game 状态截图，不直接覆盖 main menu /     │
  │      │        │ validation setup                                                                                                                                               │
  ├──────┼────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ T5   │ 轻     │ 没有 LeftRail / RightPanel 内容字段的语义合同测试（如"left rail 不出现 HP 关键字"），仅靠人工 evidence 断                                                      │
  └──────┴────────┴────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

  ---
  5. 与人工白盒记录一致性

  UI/manual-records/dark-uiux-pr01-shell.md 已涵盖全部 §7 要求的 8 张 evidence 标签（main-menu / new-run / continue-unavailable / validation-entry / validation-setup-after-fix /
  shell-1280x800 / shell-min-window）。

  警告：
  1. dark-uiux-pr01-continue-unavailable 标记 PASS_WITH_WARNING，复制详情含本机 save 绝对路径 → 违反 §5.10。规格 §0 "canonical artifact 不得绑定本机窗口/路径"
  已构成事实性破例，应在落地 PR-07 前在 ContinueUnavailablePayloadFormatter 内将 path 转为 repo-relative
  2. metadata 显示 dark-uiux-pr01-shell-min-window.png 实际窗口 1024×768，规格只笼统说"最小支持窗口"，但 GameShellLayoutTest.kt:23-33 的最小测试也是 mapWidth=18, height=17 → 推算
  1024×768，自洽 ✓

  ---
  6. 修复优先级与建议清单

  P0（应在合并前修，影响 spec 合规）

  1. 修 M1/M2：把 PlayerCreationPanel 的实际渲染 baseline 收敛到 secondaryPanel/primaryActionStack 边界内
    - 方案：删掉 MAIN_MENU_PANEL_TOP_Y 等 5 个手写常量，让 PlayerCreationPanel.render 接收 StandaloneScreenLayout 引用，从中读 secondaryPanel.top - typography.body 作为 title
  baseline
  2. 修 M3：给 entries.size ≥ 5 加 unit test；要么允许 primaryActionStack 自适应高度，要么把 Exit 折叠进 footer hint
  3. 修 O1：UiErrorScreen 把 actions 起始 y 改为 layout.primaryActionStack.top - 22f；body 内容做行数 cap
  4. 修 M4：language baseline 改到 layout.footerHelp.top - 16f 之类，确保所有 footer 文案都在 footer 矩形内
  5. 补 T1：增加 AsciiRenderModelTest 覆盖"shell 改造后 ASCII fallback 仍含 HP/Quest/Floor"
  6. 补 T2：跑完 owner gate（clientSmoke + goldenScreenshot + verifyChanged）并把结果写回 manual record
  7. 修 §5.10 违规：ContinueUnavailablePayloadFormatter 路径 redact 化

  P1（可与 PR-02 一起立项）

  8. 拆 TileRenderer：抽出 TileShellRenderer（含 left/right/footerHints/HUD card 子函数），主 renderer 只 orchestrate
  9. 实现 §3.5 tooltip/modal 层级：定义 TileTooltipModel + TileModalLayer，渲染顺序固定为"map < panes < hotbar/log < tooltip < modal < toast"，并加层级测试
  10. 实现 §5.3 viewport-on-player：InfoSurfaceLayoutSolver 接收 playerX/playerY，把 mapBounds 当作 viewport 而非"整张地图"
  11. 收敛 §3.12 outcome 屏：把 GameOver/Victory/UiError 三屏的 body 渲染合并成 OutcomeBodyRenderer，统一行数截断、颜色、actions 摆位

  P2（清洁度）

  12. 删掉 menuWidth/menuHeight/errorScreenWidth/errorScreenHeight 局部 const，全部走 UiDesignTokens.fixed
  13. ValidationSetupScreen.kt:107-108 presetSummary/activePackSummary 间距改用 UiDesignTokens.spacing 派生
  14. truncate(line, N) 全部替换为基于 approximateCharWidth 的宽度截断，对中英文一致
  15. GameShellLayoutTest 增加"右栏不画 gauge / 底部唯一一套 gauges / hotbar y∈[0, mapOffsetY]" 三条强约束

  ---
 ▐▛███▜▌   Claude Code v2.1.132
▝▜█████▛▘  Opus 4.7 with xhigh effort · Claude Pro
  ▘▘ ▝▝    ~/Documents/github/K-ToME

✻ Conversation compacted (ctrl+o for history)


❯ /compact
  ⎿  Compacted (ctrl+o to see full summary)
  ⎿  Read ../../../.claude/RTK.md (30 lines)
  ⎿  Read UI/manual-records/dark-uiux-pr01-shell.md (93 lines)
  ⎿  Read client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt (336 lines)
  ⎿  Read client/src/test/kotlin/com/ktome/client/screen/StandaloneScreenLayoutTest.kt (93 lines)
  ⎿  Read client/src/main/kotlin/com/ktome/client/screen/ValidationSetupScreen.kt (238 lines)

❯ 我现在已经按照你的反馈进行了修改,对于重大项,我新写了方案,需要你对这个方案进行深入的 review,最后给出的 review 报告,哪怕是最细小的建议都行,尽管提出各种意见和批评
  UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md

⏺ 我先读取这份新方案文档。

  Read 1 file (ctrl+o to expand)

⏺ 下面是对 UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md 的深度 review,角色仍然是资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人。

  ---
  Dark UI/UX PR-01-1 方案 Deep Review

  结论先行:Conditional Approve, with significant additions required.
  方案选对了三个长期合同(player-centered viewport / TileRenderer 拆分 / overlay layer order)做一次冻结,这是正确的"Slow is Fast"决策。但作为 Roguelike 玩家体验和工程合同的双重锚点,§3
  / §5 当前的颗粒度足以让本 PR 看似"完成",却把若干高频体验问题(viewport 抖动 / tooltip flip 优先级 / modal 居中 / damage number 层级 / camera deadzone)延迟到 PR-02+
  才暴露。这些问题一旦冻结到本 PR 的合同里再改,代价会指数上升。

  ---
  一、强项确认(必须保留的)

  1. 资源生成结论冻结合同(§0 末段) 写得很硬:PR-02 只替换 chrome paint source,不改 layout / overlay model 合同。这是少有的"提前为下一 PR 设防"的写法,必须保留。
  2. Failure Rule(§Failure Rule) 三条都点中要害:viewport 失败 → 补 focused test,overlay 失败 → 修 placement contract,maintainability 失败 → 收敛 typed model。明确禁止 golden hash
  更新掩盖布局越界 是上一份 PR-01 缺的硬约束,必须保留。
  3. Acceptance Matrix UI01-1-M03 把 TileRenderer orchestration 化 与 maintainabilityLint 绑定,直接堵死"换汤不换药地把 100 行函数搬到另一个文件"的退化路径(PR-01 已经踩过这个坑)。
  4. §2 影响范围 明确把 TileTextMetrics 列为新增收敛点 — 这正是 PR-01 当前 TileRenderer / 各 screen 各写各的 approximateCharWidth / truncate 的根源,处理对了。
  5. §4.7 明确说不把 validation overlay 改成正式玩家 modal stack — 防止 validation owner 和 player overlay 真源混淆,精准的边界声明。

  ---
  二、重大问题(P0,合同冻结前必须回写文档)

  R1. 玩家居中相机缺失 deadzone / hysteresis 设计 — 直接影响游戏体验质量

  §3.2 / §5.2 的 viewport 模型只覆盖了"中心 / 边缘 clamp / 小地图居中"三种几何状态,完全没有讨论时间维度的相机行为。

  问题:严格"player tile screen position 接近 mapBounds 中心"意味着玩家每移动一格,整张地图都跟着滚动一格。在 ToME / DCSS / Caves of Qud
  这类需要长时间凝视地图、用键盘连按方向键移动的游戏里,这种"视图永远跟着角色滚"的相机被反复证明会:
  - 玩家眼睛失去固定锚点(地标、走廊角)
  - 长走廊走 30 步会触发明显的视觉抖动
  - 在中文用户里特别容易引发"晕"的反馈(汉字密度高)

  ToME / Cogmind / DCSS 的解法都是 deadzone(死区,玩家在屏幕中心矩形内移动相机不动) 或 hysteresis(滞后,玩家走到屏幕 1/4 / 3/4 处才开始滚屏)。这是 Roguelike 视觉设计的常识,本 PR
  既然要冻结 viewport 合同,deadzone 算法是必须一次冻结的;否则 PR-XX 再加,要么 break 现有 golden,要么因为 TileMapViewport 接口不带 prevPlayerTile / prevViewportTopLeft 而无法落地。

  最小修复:
  - §3 增加任务 "TileMapViewport 接口必须接受 prev viewport state(可空,首帧为 null);本 PR 实现 STRICT_FOLLOW 策略,但留出 DEADZONE 策略接口,默认不启用"
  - §5 增加必测 "TileMapViewport 给定相邻两帧玩家位置,viewport top-left 满足策略约束"
  - §4 显式说明 "本 PR 不实现 deadzone / smooth scroll,但接口必须为 PR-XX 留口"

  R2. Overlay 层级顺序违反交互直觉,会让玩家在 modal 里丢失信息

  §3.14 给的层级:

  ▎ background < map < shell panes < bottom HUD/log/hotbar < tooltip < modal backdrop/modal < toast/debug message

  toast / debug message 高于 modal 是错的。常见违反场景:
  - 玩家在 COMBAT_DECISION modal 里读"使用闪现还是格挡"
  - 此时 NPC 死亡触发 toast "已击杀 Goblin"覆盖 modal 中央
  - 玩家无法决策,被迫关闭 modal 重看 — 在 turn-based Roguelike 里这等于一个"必输"事件

  正确的语义应该是:
  - debug message(开发期可见,玩家版本不出现):可置最顶,但本 PR 不应把它当作正式合同
  - toast(玩家可见的 transient 反馈):置于 shell panes 之上、tooltip 之下,且绕开 modal 中心区域(如锚定 bottom HUD 上沿、modal 打开时改为 log-only)
  - modal:必须是除了 debug overlay 之外的最高层

  §5.5 又把 combat feedback 归到 map 层 — 这又出了第二个矛盾:"我打了一拳掉了 12 血"的 floating damage number 在 map 层意味着 modal 一打开就完全看不到。但 floating damage 是 ToME
  类游戏战斗节奏的关键反馈,不应该被 modal 完全压死。

  最小修复:
  - §3.14 改为:background < map (含 floating combat number 锚定 actor tile) < shell panes < bottom HUD/log/hotbar < tooltip < toast(锚定 HUD 上沿,绕开 modal body) < modal
  backdrop/modal < dev-only debug overlay
  - §3 新增任务:TileOverlayRenderer 必须在 modal active 时把 toast 强制 reroute 到 log,不允许覆盖 modal body
  - §5 新增必测:"modal active 时,toast 不出现在 modal body 矩形内"

  R3. TileRenderFrame 设计存在冗余真源,与本 PR 想消灭的"两套坐标"是同类病

  §3.7 让 TileRenderFrame 同时持有 mapViewport 和 cellWidth/cellHeight。cellWidth/cellHeight 必然能从 mapViewport 推导(viewport 不知道 tile 尺寸不可能算
  tile-to-screen),双份持有等于鼓励"renderer 直接读 frame.cellWidth 跳过 viewport"。这正是 PR-01 chrome 坐标系不统一的根因 — 两个真源,有人读 A 有人读 B。

  §3.7 又把 overlayState 放进 frame,但 §3.11 又说 renderer 消费 TileOverlayModel。如果 model 还没构造好就要进 frame,说明 build phase 不完整;如果 model 已构造好,raw overlayState
  不该继续暴露。

  最小修复:
  - 删掉 frame 里的 cellWidth/cellHeight,通过 mapViewport.cellSize 唯一访问
  - frame 不持有 overlayState,只持有派生后的 TileOverlayModel
  - 显式拆为 MapRenderFrame / ShellRenderFrame / OverlayRenderFrame,每个 renderer 只接受自己需要的 frame;保留一个 TileRenderFrame 作为三者的聚合 record(只在 orchestration 层使用)

  R4. Tooltip placement 规则缺失关键合同,落地后会出现"飘忽 tooltip"体验

  §3.15 / §5.9 只说"flip 或 clamp,不遮挡 bottom log",但没有规定:

  1. flip 优先级序列:右 → 下 → 左 → 上?还是右 → 左 → 下 → 上?不同顺序导致玩家在屏幕不同位置看到 tooltip 朝向不一致,产生"系统在乱跳"的感受。
  2. anchor 锚点精确语义:tooltip 锚点是 anchor tile 的右上角、tile 中心、还是 cursor 位置?inspect 模式 cursor 飞到玩家右侧很远的 tile 上时,tooltip 是跟 cursor 还是停在 anchor
  tile?方案沉默。
  3. 嵌套 tooltip 禁止合同:status effect 自身又有 tooltip 的话会触发"tooltip 套 tooltip",这在 PR-02+ 实现 status presenter 时极容易发生。本 PR 必须明确禁止嵌套
  tooltip,否则将来要回退合同。
  4. selection cursor 与 inspect cursor 共存:键盘 selection 选中一个 actor,玩家又按 inspect 看另一个 — 现在显示一个 tooltip 还是两个?方案未涉及。

  最小修复:
  - §3.15 增加 "flip 优先级:right → down → left → up(右手键盘玩家最自然);同样优先级下选 anchor 距离 mapBounds 中心更近的方向"
  - §3.15 增加 "tooltip anchor = tile screen center;inspect 模式 anchor = inspect cursor tile,不跟 mouse"
  - §4 新增非目标 "本 PR 禁止 tooltip 嵌套,status effect 二级信息必须并入主 tooltip 或留待 PR-XX 显式实现"
  - §5 新增必测 "anchor 在 mapBounds 右边缘时,tooltip 必须 flip 到 anchor 左侧;不允许 anchor 与 tooltip 矩形相交"

  R5. Viewport tile cell 对齐没有强制约束,§5.4 "不出现半格"是空话

  §3.4 说 mapBounds 是 viewport 窗口,§5.3 说 visible range 必须为完整 cell。但如果 InfoSurfaceLayoutSolver 算出的 mapBounds.width = 1268f、cellWidth = 24f,那 1268 / 24 =
  52.83,即天然不是整数倍。当前合同没要求 layout 和 viewport 之间做 cell 对齐协商,各自实现完全可以"我自己 floor 一下就行" — 然后边缘出现 0.83 cell 的灰边或半格 actor。

  最小修复:
  - §3.4 增加 "InfoSurfaceLayoutSolver 必须对 mapBounds 做 floor((width|height) / cellSize) * cellSize 对齐;剩余像素由 shell 其他区域吸收(优先加到 right panel)"
  - §5 新增必测 "对所有 tested 视窗(1024×768、1280×800),mapBounds.width % cellWidth == 0 && mapBounds.height % cellHeight == 0"
  - 这条测试同时验证 R1 的 viewport 接口能给出 integer-cell visible range

  ---
  三、中等问题(P1,落地前应回写,但可在实现 commit 中修)

  M1. ASCII fallback 与 viewport 的关系完全没有定义

  §5.13 只说"ASCII fallback 信息不丢失"。但 ASCII path 是否参与 viewport clamping 完全没说。

  - 如果走 viewport:终端宽度通常足够看完整地图,clamp 反而切掉信息
  - 如果不走 viewport:debug 时看到的 ASCII 输出和 tile 输出空间感不一致,排查相机问题更难

  建议合同:
  - §3 / §4 / §5 任一处明确 "ASCII path 渲染整张地图(不走 TileMapViewport),仅 HUD/log 复用 shell 文本宽度合同"
  - 同步明确 "ASCII path 不渲染 overlay(modal/tooltip),inspect 仅在 tile path 提供"

  M2. Modal 居中算法没规定 footer 反向占位

  §3.16 "modal 不得覆盖 footer hints" 是软约束。如果 modal 算 vertical center 时把 footer 区域算入可用空间,会让 modal 看起来上偏(因为 footer 总是在底部不动)。

  建议合同:
  - §3.16 改为 "modal vertical center = (headerBottom + footerTop) / 2;modal height 上限 = footerTop - headerBottom - 2 * overlay padding"
  - §5 新增必测 "modal body 矩形不与 footer hints 矩形相交"

  M3. TileOverlayModel 的 presenter 真源没有强制合同

  §3.11 说 model 不解析 localized text,但没说 ITEM_DETAIL modal 和 inventory tooltip 是否共用同一 presenter。如果各自从 snapshot 派生,+5 STR vs +6 STR 类不一致会在 PR-03 出现。

  建议合同:
  - §3.11 增加 "tooltip 与 modal 描述同一 entity 时必须复用同一 presenter 输出 EntityPresentation,renderer 只负责 typography 和 placement"
  - §5 新增必测 "对同一 inventory item,tooltip 和 ITEM_DETAIL modal 的 stat list 文本完全一致"

  M4. Camera 在 zone 切换 / modal 关闭后的恢复语义未定义

  - 玩家从 zone A 走到 zone B,viewport 是从 zone A 的 clamp 状态瞬间跳到 zone B 中心,还是先黑屏再淡入?
  - modal 期间玩家用方向键操作 modal 内焦点,modal 关闭后 mapViewport 是否需要重新 center?
  - inspect cursor 关闭后,viewport 跟随对象是从 cursor 立刻 snap 回玩家 tile,还是平滑滚回?

  建议合同:
  - §3 增加 "viewport state 仅由 (mapDimensions, playerTile, inspectCursorTile?) 决定;切 zone / 关 modal / 退出 inspect 都通过更新这三元组触发重算,不引入独立 transition state"
  - 这等价于"每次都强制 snap,不做平滑"。本 PR 不实现平滑相机是合理的,但要显式声明,避免 PR-XX 加平滑时破坏 §5.5 的 "对同一 tile 使用同一 screen transform"

  M5. 多 modal 嵌套的 backdrop 行为没说

  §3.10 / §3.13 暗示 ModalStack 可以同时持有多个 frame。打开 ITEM_COMPARE 时,ITEM_DETAIL 还在栈底 — 此时 backdrop 是叠加(屏幕越来越暗)还是只画最顶 modal 的 backdrop?

  建议合同:
  - §3.13 / §3.16 显式 "backdrop 只画一层,基于 stack top 的 modal kind 决定 alpha;backdrop 不叠加"
  - 否则 PR-04 active talent slot choice 弹在 item compare 之上时会出现"屏幕完全黑"

  M6. SpriteBatch 的 layer 切换没规定 flush 策略

  LibGDX SpriteBatch 没有真正 z-buffer,完全靠调用顺序。三个 renderer pass 之间如果不强制 flush,某些 sprite 会乱层。

  建议合同:
  - §3 末尾或 §6 增加 "renderer 之间共享同一 batch,但每个 renderer pass 必须以 batch.flush() 结束;TileRenderer.renderToCanvas 负责调用顺序与 flush"
  - 或者 "每个 renderer 独立 begin/end,以 batch.totalRenderCalls 为基准做性能监控"

  M7. TileRenderFrame 不可变性 / 复用性没约束

  每帧构造一个 frame 对象会增加 GC 压力(LibGDX 主循环是 60fps);但如果 frame mutable + reuse,renderer 之间可能互相污染。

  建议合同:
  - §3.7 增加 "TileRenderFrame 必须为 immutable record;实现可通过对象池复用,但 renderer 不得持有 frame 引用跨帧"
  - §5 增加 "TileRendererCanvasTest 中相邻两帧的 frame 实例不共享可变字段"

  ---
  四、轻微问题(P2,可在文档润色阶段处理)

  L1. 命名一致性

  ┌─────────────────────┬──────────────────────────────────────────────────┬─────────────────────────────────────┐
  │      当前命名       │                       建议                       │                理由                 │
  ├─────────────────────┼──────────────────────────────────────────────────┼─────────────────────────────────────┤
  │ TileMapViewport     │ OK                                               │ tile-aware,合理                     │
  ├─────────────────────┼──────────────────────────────────────────────────┼─────────────────────────────────────┤
  │ TileShellRenderer   │ ShellRenderer                                    │ shell 不是 tile 概念,前缀 Tile 误导 │
  ├─────────────────────┼──────────────────────────────────────────────────┼─────────────────────────────────────┤
  │ TileOverlayRenderer │ OverlayRenderer                                  │ 同上                                │
  ├─────────────────────┼──────────────────────────────────────────────────┼─────────────────────────────────────┤
  │ TileTextMetrics     │ RenderTextMetrics 或 UiTextMetrics               │ 服务于 shell/overlay,不只是 tile    │
  ├─────────────────────┼──────────────────────────────────────────────────┼─────────────────────────────────────┤
  │ TileRenderFrame     │ RenderFrame(或拆为 Map/Shell/OverlayRenderFrame) │ 同上                                │
  └─────────────────────┴──────────────────────────────────────────────────┴─────────────────────────────────────┘

  L2. 术语歧义

  - "viewport" 同时指 TileMapViewport(map 子区域)和 FoundationViewportSupport(LibGDX FitViewport)。建议在文档显式区分:stage viewport(LibGDX,屏幕全 NDC)与 map
  viewport(TileMapViewport,map 子区域)。
  - "overlay" 同时指 tile-level overlay sprite(prop / item)与 UI overlay layer(tooltip / modal)。建议把前者改叫 tile prop layer 或 tile sprite overlay,后者保留 UI overlay。
  - "shell world size" / "shell preferred/min world dimensions" / "shell fixed dimensions" — 同概念三种叫法,统一为 shell world dimensions。

  L3. §6 验证命令冗余

  acceptanceContractLint / localeLint / contractLint / maintainabilityLint 跑全套对本 PR 是合理的,但应该在 §6 标注 "本 PR 触发的额外校验是 X / Y / Z",而不是只列命令。否则 reviewer
  不知道为什么这条 lint 一定要在这里跑,后续删命令容易误删。

  L4. §7 manual record 截图时机模糊

  dark-uiux-pr01-1-targeting-cursor-viewport 截图时机没说。targeting cursor 是动态的,要求 "按 t 进入 targeting 模式后,把 cursor 移到 mapBounds 中心后截图" 应当显式写出,否则 manual
  record 复现性差。

  L5. 与 PR-01 已修复 finding 的关系

  PR-01 manual record 有两条 finding(continue-unavailable 路径泄漏、validation overlap)已经在 PR-01 修复。本 PR 文档应在 §0 或 §1 加一句:"PR-01 的两条 finding 不在本 PR 范围,但
  manual record 必须保留 PR-01 的修复结论,golden 比对必须确认这两条不回退。" 否则 reviewer 容易把视觉变化误判为回退。

  L6. 工作量切片建议

  XL 标记合理,但建议 §1 末尾增加里程碑切分:

  - M1: TileMapViewport + InfoSurfaceLayoutSolver 改造 + FoundationViewportSupport 改造(独立可 review)
  - M2: TileRenderer orchestration 化 + TileMapRenderer / TileShellRenderer 抽出
  - M3: TileOverlayRenderer + TileOverlayModel + tooltip / modal layer
  - M4: golden / manual record / verifyChanged

  明确 "同一 branch、同一 PR,但实现按 M1→M2→M3→M4 分 commit",降低 reviewer 单次审阅负担。

  L7. Frame kind 的 enum / sealed 选择

  §3.13 列出 ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION / ACTIVE_TALENT_SLOT_CHOICE 当作 frame kind。Kotlin 里这是 enum 还是 sealed class?如果 PR-03 加 ITEM_DUAL_COMPARE,本 PR
  是否需要预留扩展点?建议明确 "frame kind 为 sealed class,新 kind 不破坏 overlay layer 合同"。

  L8. 性能预算缺失

  方案完全没有提性能合同。建议 §0 加一条:"本 PR 不引入新的每帧 allocation hot path;TileMapViewport / TileRenderFrame / TileOverlayModel 在稳态(无 modal、玩家不移动)下每帧 0
  allocation"。这能堵住"为了拆文件每帧 new 一堆 record" 的退化。

  L9. Token 增量未明示

  §2 说在 UiDesignTokens.kt 增加 "shell preferred/min world dimensions、overlay max width/height、tooltip/modal padding"。但具体新增哪些字段、命名规范没说。建议在 §3 增加一个明确的
  token 清单(类似 PR-01 给的 UiFixedDimensionTokens),否则 reviewer 无法判断 token 设计是否合理。

  L10. golden 命名空间

  §Acceptance Matrix 提到 dark-uiux-pr01-1-* golden,但没说现有 dark-uiux-pr01-* golden 是更新还是替换。如果 PR-02 引入资源后 chrome 视觉会变,本 PR 的 golden 是中间状态;建议显式说 "本
   PR golden 命名为 dark-uiux-pr01-1-*,不覆盖 PR-01 已有 golden;PR-02 资源接入后再决定哪些可合并"。

  ---
  五、必须新增的合同(文档当前完全没有的)

  将上述 R1–R5 + M1–M7 合并,至少需要在文档新增以下合同条目:

  1. R1 deadzone 接口预留 + 必测
  2. R2 toast/floating combat number 层级修正 + 必测
  3. R3 frame 拆分 + 单一真源 + 删除 cellWidth/cellHeight 冗余
  4. R4 tooltip flip 优先级 + anchor 语义 + 禁止嵌套 + 必测
  5. R5 mapBounds cell 对齐 + 必测
  6. M1 ASCII path 不走 viewport 的明确声明
  7. M2 modal 垂直居中算法 + 必测
  8. M3 tooltip / modal 共用 presenter + 必测
  9. M4 viewport state 三元组合同
  10. M5 backdrop 不叠加 + 必测
  11. M6 SpriteBatch flush 策略
  12. M7 frame immutable / 0 allocation 稳态 + 性能预算
  13. L7 frame kind sealed class 扩展点

  这 13 条都是未来不写就一定后悔的,因为它们都是合同级别(改一次破坏所有 golden)。实现细节可以推迟,合同必须先冻结。

  ---
  六、测试覆盖缺口(§5 必测行为之外应补)

  ┌────────────────────────────────────────────────────────────────┬───────────────────┬────────────────────────────────────────────────────────┐
  │                            测试合同                            │     当前状态      │                          建议                          │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ mapBounds % cellSize == 0 对所有 tested 视窗                   │ 缺失              │ GameShellLayoutTest 新增                               │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ viewport 相邻帧合同(R1 deadzone 接口)                          │ 缺失              │ TileMapViewportTest 新增                               │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ tooltip flip 决策(R4)                                          │ 缺失              │ TileRendererCanvasTest*tooltip-flip* 新增              │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ modal active 时 toast reroute(R2)                              │ 缺失              │ TileRendererCanvasTest*toast-reroute* 新增             │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ modal 不与 footer 相交(M2)                                     │ 缺失              │ TileRendererCanvasTest*modal-bounds* 新增              │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ presenter 一致性(M3)                                           │ 缺失              │ *PresenterParityTest 或 focused presenter test 新增    │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ backdrop 不叠加(M5)                                            │ 缺失              │ ModalStackTest 新增                                    │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ renderer pass 调用顺序(layer order)                            │ 缺失,只是命名暗示 │ TileOverlayLayerOrderTest 显式新增                     │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ Floating combat number 在 modal 下仍可见 / 在 modal 上被压(R2) │ 缺失              │ TileRendererCanvasTest*combat-feedback-layer* 新增     │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ zone 切换 viewport 重置(M4)                                    │ 缺失              │ FoundationViewportSupportTest 新增                     │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ ASCII path 不调用 viewport(M1)                                 │ 缺失              │ AsciiRenderModelTest 加断言                            │
  ├────────────────────────────────────────────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
  │ 稳态 0 allocation(M7,可选)                                     │ 缺失              │ TileRendererCanvasTest*allocation* 或在 smoke 中加观测 │
  └────────────────────────────────────────────────────────────────┴───────────────────┴────────────────────────────────────────────────────────┘

  ---
  七、人工白盒补充建议

  §7 当前 9 步 + 6 张证据。基于上面的批评,建议增加:

  11. 长走廊连续移动 30 步(对应 R1):截图前后两帧,主观判断 viewport 是否抖动到不可接受。
  12. 战斗中触发 floating damage,同时打开 COMBAT_DECISION modal:确认 floating number 的层级符合 R2。
  13. tooltip 在 mapBounds 四角各打一次 inspect:确认 flip 优先级一致。
  14. 中文 + 最小窗口下打开 ITEM_DETAIL modal:确认 footer hints 完整可见(对应 M2)。
  15. 多 modal 嵌套(ITEM_DETAIL + ITEM_COMPARE):确认 backdrop 不叠加(对应 M5)。

  新增证据建议:
  - dark-uiux-pr01-1-corridor-no-jitter
  - dark-uiux-pr01-1-floating-damage-with-modal
  - dark-uiux-pr01-1-tooltip-flip-corners
  - dark-uiux-pr01-1-modal-stack-backdrop

  ---
  八、终评

  ┌────────────────────────┬──────┬────────────────────────────────────────────────────────────────┐
  │          维度          │ 评分 │                              说明                              │
  ├────────────────────────┼──────┼────────────────────────────────────────────────────────────────┤
  │ 问题诊断准确度         │ A    │ 三个核心病灶选得对,优先级正确                                  │
  ├────────────────────────┼──────┼────────────────────────────────────────────────────────────────┤
  │ 合同冻结完整度         │ C+   │ 几何合同 OK,时间合同 / 交互合同 / 性能合同均缺失               │
  ├────────────────────────┼──────┼────────────────────────────────────────────────────────────────┤
  │ 命名与术语一致性       │ B-   │ viewport / overlay 双义,Tile 前缀滥用                          │
  ├────────────────────────┼──────┼────────────────────────────────────────────────────────────────┤
  │ 测试设计严密度         │ B    │ 命名暗示式覆盖,关键合同测试缺 8 条以上                         │
  ├────────────────────────┼──────┼────────────────────────────────────────────────────────────────┤
  │ 与下游 PR 的合同稳定性 │ A-   │ PR-02 paint source 替换合同写得好,frame kind 扩展点缺          │
  ├────────────────────────┼──────┼────────────────────────────────────────────────────────────────┤
  │ Roguelike 玩法体验考量 │ C    │ deadzone / floating damage / tooltip flip / modal 居中均欠考虑 │
  └────────────────────────┴──────┴────────────────────────────────────────────────────────────────┘

  总评:这是一份"工程师视角写得不错、玩家视角写得不够"的方案。把 R1–R5 这 5 条重大合同回写之后,可以放行进入实现;否则即使本 PR 顺利合并,PR-XX 第一次实现 deadzone / floating combat
  number / tooltip flip 时,就一定会被迫破坏本 PR 冻结的合同 — 这正是当前文档想避免的事。

  如需,我可以把上面这份 review 写入 UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.review.md(与 PR-00/PR-01 同款命名)落盘,告诉我即可。

✻ Brewed for 5m 20s

※ recap: 目标是对 `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md` 做深度 review。已交付完整 review 报告,指出 5 条 P0 合同缺口(deadzone、overlay 层级、frame 冗余、tooltip
   flip、cell 对齐)等。下一步:等你确认是否落盘到 `*.review.md`。 (disable recaps in /config)

❯ 已经按照反馈对UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md进行了大量的优化,请再次进行深度的 review,给出 review 报告,哪怕是最细节的问题都行

  Read 1 file (ctrl+o to expand)

⏺ 下面是对修改后版本的 deep review。整体方向显著改善,响应了上一轮 R1–R5 与 M1–M7 中的绝大部分;但仍有一处大决策(删除 ASCII fallback)铺得不够、若干合同字段含糊,还有 1
  条上一轮提的问题(SpriteBatch flush 策略)被遗漏。

  ---
  Dark UI/UX PR-01-1 方案二轮 Deep Review

  结论先行:Approve, with focused additions required。
  方案现在已经具备"工程合同 + 玩法体验合同 + 设计参考合同"三层冻结,§2 ToME 设计合同的引入是亮点,deadzone / overlay 层级 / frame 拆分 / cell 对齐都按上一轮反馈钉死。但删除 ASCII
  fallback 是上一轮没要求的一个新决策,带来的副作用文档没充分讨论;SpriteBatch flush 策略 仍然没有写;若干合同(deadzone token 命名、modal 嵌套 backdrop 选择、presenter
  共用、tile-to-screen 像素精度)还有"留给实现者解释"的灰色地带。这些灰色地带在合同 PR 是不允许的。

  ---
  一、上一轮反馈的响应度核对

  ┌──────────────────────────┬───────────────────────────────────────────────────────────┬───────────────────┐
  │         上一轮项         │                         当前状态                          │       备注        │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ R1 deadzone              │ ✅ 已加 §3.2 / §3.4 / §3.5 / §8.2                         │ 完全采纳,语义清晰 │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ R2 overlay 层级修正      │ ✅ 已重排 §5.1,combat feedback 归入 map 层,modal > toast  │ 修对了            │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ R3 frame 冗余            │ ✅ §4.2–§4.5 拆为三个独立 frame,显式禁止总 frame 长期化   │ 完全采纳          │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ R4 tooltip placement     │ ✅ §5.4–§5.7 定义了 source 优先级、anchor、flip 顺序      │ 大部分采纳,见 N9  │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ R5 cell 对齐             │ ✅ §3.9 显式 floor + 居中 padding                         │ 采纳              │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ M1 ASCII path            │ ⚠️ 直接删除(超出原建议范围)                               │ 见 N1             │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ M2 modal 居中            │ ✅ §5.8 居中于 headerBottom..footerTop                    │ 采纳              │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ M3 presenter 共用        │ ❌ 未写                                                   │ 见 N3             │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ M4 viewport state 三元组 │ ✅ §3.3 identity 字段                                     │ 采纳但仍可强化    │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ M5 backdrop 不叠加       │ ✅ §5.3 显式合同                                          │ 采纳              │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ M6 SpriteBatch flush     │ ❌ 未写                                                   │ 见 N6             │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ M7 frame allocation 预算 │ ⚠️ §4.10 / §4.11 提了 hot-loop 与 self-audit,但缺数字预算 │ 见 N7             │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ L1 命名                  │ ⚠️ §1.4 钉死 frame 命名,§6 又允许"等价命名"               │ 矛盾,见 l12       │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ L7 frame kind sealed     │ ❌ 未写                                                   │ 见 l21            │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ L8 性能预算              │ ⚠️ 同 M7                                                  │ 仍含糊            │
  ├──────────────────────────┼───────────────────────────────────────────────────────────┼───────────────────┤
  │ L9 token 增量            │ ⚠️ 提了 deadzone token 但没列 field name                  │ 见 l9             │
  └──────────────────────────┴───────────────────────────────────────────────────────────┴───────────────────┘

  响应度评分:11/16 全采纳,3/16 部分采纳,2/16 未采纳。

  ---
  二、新引入或仍未解决的重大问题(P0,合同冻结前必须回写)

  N1. 删除 ASCII fallback 是大决策,但冲击面没有讨论

  §1.6 / §6 / §7.9 / §8.14 一齐宣布删除 client ASCII renderer / model / fallback。方向我支持(避免把 ASCII 当 evidence),但这个决定的影响面文档没回答:

  1. ASCII model 是否被 server / game / core 跨模块消费?
  §1.6 只说"client ASCII fallback / debug renderer 依赖",但没在文档里证明全仓库扫描结果。如果 game/...replay、core/...debug 任一处 import 了
  AsciiRenderModel,删除会触发跨模块编译断裂。
  修复:§6 或新增 §6 删除清单子段,显式列出 "本 PR 删除的类型 / 文件 / manifest key 清单",并在末尾加一句 "扫描 git grep -l Ascii,client 之外的引用 = 0(若不为 0,本 PR
  范围扩大或方案重审)"。
  2. CI / headless test 的非 UI 路径如何替代?
  ASCII path 通常是 fog/LOS/actor positioning 这类非渲染逻辑的快速断言路径(text snapshot 比 canvas hash 稳定且人类可读)。删除后,这些测试要么:
  (a) 改用 TileRendererCanvasTest(慢、依赖 LibGDX headless)
  (b) 改用 vector-based snapshot(RenderSnapshot 直读)
  修复:§7 非目标补一句 "本 PR 删除 ASCII path 后,原 ASCII text snapshot 测试改用 RenderSnapshot vector 断言;不允许新增任何 ASCII-like text dump test"。
  3. 开发者 debug 路径
  "actor 没渲染" 这类问题,ASCII model 是 60 秒级的 sanity check。删除后,debug 流程是什么?
  修复:§5.1 末段已留 "dev-only debug overlay" 层,但应明确 "dev-only debug overlay 不是 ASCII 接班人,不得渲染整张 map ASCII;只可绘制 viewport bounds / mapBounds outline / focus tile
  高亮等诊断 hint",防止有人用 dev overlay 重建 ASCII 渲染。
  4. §1.6 措辞"依赖"
  "删除 client ASCII fallback / debug renderer 依赖" — 是删除依赖关系还是删除类型本体?如果只删依赖,renderer 类还可能留作死代码。
  修复:改为 "删除 client AsciiRenderer / AsciiRenderModel / AsciiRenderModelTest 类型本体及所有消费路径,manifest 中所有 *.ascii.* key 同步移除"。

  N2. SpriteBatch flush 策略仍然完全没说(原 M6 遗漏)

  LibGDX SpriteBatch 是 vertex queue,内部排队 vertex,只有 flush() 或 end() 时才提交 GPU。三个 renderer pass 共享一个 batch 时:
  - TileMapRenderer 画完 actor → batch queue 持有 actor vertices
  - TileShellRenderer 开始画 right panel → 如果 panel sprite 用相同 texture,vertex 混到 actor batch,flush 时按"插入顺序"画,但 layer 顺序混乱
  - 即使 texture 不同(LibGDX 会 auto-flush on texture change),仍可能因为 Z 顺序错乱出现 actor 跑到 right panel 上方

  修复:§4 或 §5 加一条:
  "renderer pass 之间共享同一 SpriteBatch,但每个 pass 退出前必须 batch.flush();layer order 由调用顺序保证,不依赖 z-coordinate 或 texture binding 切换"

  并在 §8 必测行为加:"TileRendererCanvasTest 必须断言 batch.totalRenderCalls 在 layer 切换处递增(证明 flush 被调用)"。

  N3. M3 (Presenter 共用) 仍然没有强合同

  §5.4 解决了 tooltip source 优先级,但没有约束 modal 与 tooltip 共用同一 presenter。当 PR-03 实现 ITEM_DETAIL modal 时,极容易出现:
  - tooltip presenter 计算 stat = base + buff = +5 STR
  - modal presenter 计算 stat = base only = +4 STR
  - 玩家看 tooltip 决定装备,装备后 modal 显示 +4,误以为 bug

  修复:§5 加一条:
  "对同一 entity / item / actor,tooltip 与 modal 的 stat / status / icon / quality 字段必须来自同一 presenter 函数;renderer / overlay model 不允许各自从 RenderSnapshot
  二次派生属性。"

  §8 必测加:"对同一 inventory item,tooltip stat 列表与 ITEM_DETAIL modal stat 列表逐项一致(focused presenter parity test)"。

  N4. inspect 关闭后 viewport 过渡语义未定义

  §3.6 说 inspect / targeting 关闭后回到 player-centered state,但关闭瞬间是 snap 还是经由 deadzone:

  - 场景:玩家 tile 在屏幕中心,玩家用 inspect 把 cursor 移到屏幕右下角 → viewport 跟 cursor 滚动 → 关闭 inspect 时 player tile 离 viewport 中心已经远超 deadzone
  - 方案 A (snap):关闭瞬间 viewport 立即 center 到 player → 视觉跳变 1 帧
  - 方案 B (soft return):关闭后 deadzone 算法接管,因为 player 在死区外,viewport 滚回 player → 玩家看到 viewport 反向滚动一次,有"自己被拽回"的体感

  修复:§3.6 必须二选一:
  "inspect / targeting 关闭瞬间,viewport 立即 snap 到 player-centered + clamp 边界;下一帧 deadzone 算法以该状态为基线重算"

  (我推荐 snap;Roguelike 玩家更习惯"切换模式 = 视图重置")。

  N5. Tile-to-screen 浮点精度 / 取整路径未规定

  §3.10 "必须通过同一个 tileToScreen 或等价 transform"。但浮点精度路径完全没说:
  - tileToScreen(tileX, tileY) 返回 Float 还是 Int?
  - 如果是 Float,某 renderer floor、另一个 round → 不同 layer 的 sprite 在屏幕上偏 0.5px
  - nearest-neighbor 采样下,半像素偏移会让 actor sprite 与 terrain tile 错位 1px,长时间观看会看到 actor "粘在 tile 边"

  修复:§3 加一条:
  "tileToScreen 返回 Int 像素坐标(floor(mapBounds.x + offsetX + tileX * cellWidth));所有 renderer 必须复用同一函数,不得在调用方各自 floor / round / cast"

  §8 必测加:"两个独立 renderer pass 对同一 tile 调用 tileToScreen,返回值 byte-identical"。

  N6. Deadzone token 命名规则 + 边界条件未冻结

  §3.4 "水平至少 4 cells 或可视列数的 1/4,垂直至少 3 cells 或可视行数的 1/4"。问题:

  1. "至少" = max?即 max(4, columns / 4)?
    - 如果可视列数 = 16,max(4, 4) = 4,deadzone = 4 cells
    - 如果可视列数 = 80,max(4, 20) = 20,deadzone = 20 cells
    - 大 deadzone 在大屏上让玩家走很久才滚屏 — 对 ToME 长廊 OK,但对短走廊会让玩家走到边缘 → 跳一大段
  2. 窄窗口下 deadzone 越界:可视列数 < 8 时,max(4, columns/4) = 4 ≥ columns/2,死区超过可视区域一半 → 视觉抖动反而严重
  3. token field name 没列(我上一轮 L9 提过)

  修复:§3.4 改为:
  - "deadzone 水平 cells = max(deadzoneHorizontalMinCells, floor(visibleColumns * deadzoneHorizontalRatio)),且不超过 visibleColumns - 2(预留至少 1 cell 滚动 margin 两侧)"
  - "默认 token:deadzoneHorizontalMinCells = 4、deadzoneHorizontalRatio = 0.25f、deadzoneVerticalMinCells = 3、deadzoneVerticalRatio = 0.25f"

  §6 token 列表显式列出这 4 个 field name。

  N7. modal 嵌套时 backdrop alpha / color 选择未定义

  §5.3 "Backdrop 只允许由 active modal frame 生成一个 shared layer"。当 stack 里有 ITEM_DETAIL + ITEM_COMPARE 两个 modal 时,backdrop 来自哪个?

  - 来自 stack 顶 (ITEM_COMPARE)?
  - 来自 stack 底 (ITEM_DETAIL)?
  - 不同 modal kind 是否允许不同 alpha?(例如 COMBAT_DECISION 比 ITEM_DETAIL 更强遮挡)

  修复:§5.3 加一句:
  "backdrop 视觉参数(alpha / color)取自 ModalStack.peek() 的 frame kind 配置;两个 modal 切换时若 alpha 不同,直接切换不做 fade 过渡(本 PR 不实现过渡动画,见 §7)"

  N8. §3.3 viewport state identity 缺 visibleRange 验证

  §3.3 列了 zone/session id、map dimensions、cell size、map bounds、active focus mode。但缺少 prev visibleRange 一致性的隐含约束。

  如果上一帧合法 state 持有 visibleRange = (10..30, 5..25),本帧 mapBounds 不变但玩家移到了 prev 死区外,新 visibleRange 应该是上一帧 + 滚动量,但代码可能直接根据 player tile 重新算
  center,忽略 prev offset → 死区失效。

  修复:§3 加一条:
  "deadzone 计算必须以 prev TileMapViewportState.viewportTopLeft 为基线;只有 identity 字段变化时才允许重新 center;identity 一致时,新 viewportTopLeft = prev viewportTopLeft +
  minimalScroll(player exits deadzone)"

  §8 必测加:"给定 prev state 与 player 移动 1 cell(仍在死区内),新 viewportTopLeft == prev viewportTopLeft"。

  N9. Tooltip "关键 token" 与 "list row bounds" 含糊

  §5.7 "减少行数并保留标题/关键 token"。"关键 token" 没定义。
  §5.5 anchor 来自 "tile center、slot bounds 或 list row bounds"。"list row bounds" 指什么 list 没说。

  修复:
  - §5.5 改为:"tile center / inventory slot bounds / equipment slot bounds / talent tree node bounds / quest log row bounds 等具有可见 bounds 的 entity;不允许 mouse
  position、硬编码屏幕角、上一帧坐标"
  - §5.7 改为:"clamp 后 tooltip body 仅保留 title + body 前 N 行(N = floor(maxHeight / approximateLineHeight) - 1),末尾以 … 表示截断"

  N10. §6 影响范围清单是"修改"清单,没有"删除"清单

  删除 ASCII fallback 涉及多个文件 / manifest key,但 §6 表格只列 "预期改动" 不列 "删除"。reviewer 无法快速核对哪些文件应该消失。

  修复:§6 表格之后新增一段 "删除清单":
  - client/src/main/kotlin/.../AsciiRenderer.kt
  - client/src/main/kotlin/.../AsciiRenderModel.kt
  - client/src/test/kotlin/.../AsciiRenderModelTest.kt
  - client/.../resources/manifest/*.json 中所有 *.ascii.* key
  - i18n/*.json 中所有 ASCII-only key(若有)
  - :client:goldenScreenshot 中所有 *ascii* golden(若有)

  §8 必测加:"git grep -i 'ascii' client/src 返回 0 行"(可作为 maintainabilityLint 规则)。

  N11. modal active 时 inspect cursor 视觉是否抑制

  §5.4 说 modal active 时抑制 passive tooltip,但没说 inspect cursor 本身的视觉是否还在 map layer 渲染:
  - 玩家进入 inspect → cursor 在 actor 上,显示 tooltip
  - 玩家按 i 打开 inventory modal(假设双开)
  - inspect cursor sprite 还在 map 上,但 tooltip 抑制了 → 玩家困惑 "为什么 cursor 在 actor 上但没信息"

  修复:§5.4 加一句:
  "modal active 时,inspect cursor sprite 仍渲染于 map layer(保留空间锚点),但 inspect tooltip 内容抑制;modal 关闭后 tooltip 恢复"

  或者更严格:"modal 打开时强制退出 inspect / targeting,viewport 立即 snap 到 player-centered"(更简单,推荐)。

  N12. footerTop 取自 footer hints top edge 还是 window bottom

  §5.8 "modal 居中于 headerBottom..footerTop 的可用 content area"。footerTop 是 footer hints 的 top edge 还是窗口底部?

  如果是窗口底部,modal 仍然可能盖住 footer hints。

  修复:§5.8 改为:
  "headerBottom = shell header bottom edge;footerTop = footer hints 的 top edge(不是窗口 / shell 的 bottom);modal vertical center = (headerBottom + footerTop) / 2"

  ---
  三、细节问题(P1,落地前应回写)

  l1. §1.4 与 §6 frame 命名矛盾

  §1.4 已经把 4 个 frame 类型钉死(MapRenderFrame / ShellRenderFrame / OverlayRenderFrame 加上不允许的总 TileRenderFrame),§6 影响范围又说"或等价命名"。

  修复:二选一。建议保留 §1.4 钉死命名,§6 删除"等价命名"措辞;或者 §1.4 改为 "命名建议如下,实现可使用等价 owner 名,但禁止 Helper / Manager / Support"。

  l2. §1.4 列了 4 个 frame,但 §4.5 只规范了 3 个

  §1.4 列出 TileMapViewport、MapRenderFrame、ShellRenderFrame、OverlayRenderFrame、TileOverlayModel。
  §4.5 禁止 TileRenderFrame 持有总 model。
  TileMapViewport 不在 frame 系列里,但 §4.5 没说它的所属。

  修复:§4 加一条:"TileMapViewport 是独立 typed 真源,既不归 frame 也不归 model;每帧由 orchestration 构造,被 MapRenderFrame / OverlayRenderFrame 通过引用消费"。

  l3. §3.5 "最小距离" 没量化

  "viewport 只滚动到让该 tile 回到死区内的最小距离"。是 1 cell 还是 (tile - deadzone 边界) 取整?

  修复:"滚动量 = ceil((player tile - 最近 deadzone 边界) / 1 cell) ,即玩家走一格 viewport 滚一格,不做 sub-cell 平滑"。

  l4. §3.4 deadzone 中心定义模糊

  "死区默认以可视区域中心为基准" — 是 mapBounds 像素中心还是 visible tile range 几何中心?cell-aligned 后两者通常一致,但极端 mapBounds 不能整除时不一致。

  修复:"deadzone 中心 tile 索引 = (visibleRange.firstColumn + visibleRange.lastColumn) / 2(整数运算);若 visible columns 为偶数,优先取下半个"。

  l5. §5.4 "active modal explicit tooltip" 概念未定义

  只出现一次。ITEM_DETAIL modal 自己有内部 tooltip slot 吗?

  修复:§5.4 加定义:"'active modal explicit tooltip' 指 modal frame kind 在合同里声明的 secondary tooltip slot(如 ITEM_DETAIL 的 stat 行 hover);本 PR 不实现 secondary
  tooltip,但合同保留扩展位"。

  或者直接删掉这层概念,本 PR 只允许 modal 期间 tooltip 全抑制。

  l6. §5.6 flip 顺序在 RTL locale 不友好

  right -> down -> left -> up 对右手键盘玩家自然。RTL locale(阿拉伯语)将来需要 mirror。

  修复:§7 非目标加:"不实现 RTL locale tooltip flip mirroring;若 PR-XX 引入 RTL,需要新方案"。预防性合同。

  l7. §4.2 MapRenderFrame 的 cell metrics 字段冗余风险(原 R3 残留)

  §4.2 "cell metrics" 是独立字段还是 viewport.cellSize 引用?如果独立,又回到 R3 双真源问题。

  修复:§4.2 改为:"cell metrics 通过 viewport.cellSize 单一访问;frame 不独立持有 cellWidth / cellHeight 字段"。

  l8. §4.10 "frame-level immutable records" 与 allocation 预算

  §4.10 / §4.11 说 hot-loop 不允许 per-tile 新对象,但没说 per-frame allocation 总量预算。

  修复:§4 加一条:"稳态(无 modal、无 inspect、玩家不移动)下,renderer pass 总 allocation < 1 KB;frame data class 字段优先使用 primitive(Int / Float / Boolean),避免 boxed
  类型;集合字段使用 ImmutableList 或预分配 array"。

  或加性能监测条款:":client:clientSmoke 启动 60s 站立观察 allocation rate;若 minor GC < 5s 一次,maintainability gate 失败"(可选)。

  l9. §6 token field name 仍未列

  参考 N6 修复,§6 应给完整 token field 列表:
  deadzoneHorizontalMinCells: Int
  deadzoneVerticalMinCells: Int
  deadzoneHorizontalRatio: Float
  deadzoneVerticalRatio: Float
  tooltipMaxWidth: Float
  tooltipMaxHeight: Float
  tooltipPadding: Float
  tooltipFlipMargin: Float
  modalMaxWidth: Float
  modalMaxHeight: Float
  modalPadding: Float
  modalBackdropAlpha: Float
  shellPreferredWorldWidth: Float
  shellPreferredWorldHeight: Float
  shellMinWorldWidth: Float
  shellMinWorldHeight: Float
  否则两个实现者各起各的名,PR-XX 又要重命名。

  l10. §3.2 "可 snap" 措辞含糊

  中文"可"是 may。语义上是 must(否则 prev state 错误延续)。

  修复:改为 "首帧、zone 变化、resize、focus mode 切换后必须 snap 到 center/clamp,不沿用 prev state"。

  l11. §4.5 "局部变量" 不可强制

  如何 verify "局部"?typed system 不能。

  修复:"TileRenderer.renderToCanvas 内部聚合三个 frame 的中间类型必须为 internal / private 可见性,不得 export 给 file 外"。可由 maintainabilityLint 静态规则验证。

  l12. §10.7 inspect 关闭 "正常恢复" 不可观

  "关闭后 pane focus 和地图 viewport 正常恢复" 含糊,与 N4 关联。

  修复:"关闭后 pane focus 回到 PaneFocusAnchor.WORLD;viewport 立即 snap 到 player-centered + clamp;通过截图对比可观察"。

  l13. §10.10 ToME-like 视觉对照如何"可观"

  "对照 UI/UI-demo.png,确认第一视觉焦点仍是地图..." 由人眼判断,但作为 evidence 应该可量化。

  修复:加一条 "通过 golden screenshot 测量 mapBounds 像素面积 ≥ shell 总像素的 50%"。或在 manual record 里强制截图标注 mapBounds rect 占比。

  l14. §10.11 必填证据没有 "viewport 静止" 证据

  dark-uiux-pr01-1-viewport-deadzone-corridor 暗示包含 still + scroll 两阶段,但 manual record 模板应明确:该证据要求"两张截图,玩家移动 5 步后 viewport 像素相同(死区内)+ 玩家再移动 5
  步 viewport 滚动(越过死区)"。

  或者拆成两张证据:viewport-still-in-deadzone 和 viewport-scroll-leaving-deadzone。

  l15. §9 viewport / overlay focused lane 命令依赖测试名命名约定

  TileRendererCanvasTest*viewport* / *overlay* / *tooltip* / *modal* 是字符串匹配,要求测试方法名严格遵循。

  修复:§4 或 §6 加一条 "TileRendererCanvasTest 测试方法命名约定:renderViewport*、renderOverlay*、renderTooltip*、renderModal*,以保证 focused lane 命令稳定"。

  或者强制要求显式测试类:TileMapViewportTest、TileOverlayLayerTest、TooltipPlacementTest,而不是用名字匹配。

  l16. §7 非目标缺 "不实现 modal / tooltip / viewport 过渡动画"

  虽然 §3.2 说 "不做逐步动画平滑",但 modal / tooltip 入场动画也应禁。否则 PR-XX 加 modal slide-in 会让 layer 顺序临时叠加。

  修复:§7 加 "不实现 modal / tooltip / viewport 的过渡动画;layer 切换、modal 打开、tooltip 出现均为瞬间生效"。

  l17. §5.5 "上一帧残留坐标" 禁止之外,缺 "事件驱动 anchor 失效" 处理

  如果 keyboard selection 选中一个 inventory slot,然后玩家操作让该 slot 消失(消耗道具),此时 anchor 失效。tooltip 应自动隐藏还是 clamp 到 inventory 区域中心?

  修复:§5 加一条 "anchor source 失效时,renderer 必须立即隐藏关联 tooltip;不允许保留上一帧 anchor 位置,也不允许 fallback 到屏幕中心"。

  l18. §0 "信息架构 vs 装饰" 边界缺定义

  "只约束信息架构、密度、层级和 map-first 方向,不要求逐像素复刻"。信息架构 vs 装饰未定义。

  修复:§0 视觉参考补一句:"信息架构 = 区域分布 / 占比比例 / 焦点优先级 / 信息分层;装饰 = 具体配色 / 字体 / icon 风格(由 PR-02 资源决定)。本 PR 不复制装饰"。

  l19. §4.11 self-audit 落地位置 + 复检触发

  self-audit 写哪儿?manual record 还是 PR description?如果 PR-02 改了 frame 字段,audit 是否需要更新?

  修复:§4.11 改为 "PR close 前在 UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md 写入 frame ownership self-audit 子段,字段格式包含 frame name / 字段列表 / 是否含 raw
  OverlayState / 是否含 duplicated cell metrics / 是否含完整 aggregate model;后续 PR 修改 frame 字段必须同步更新该子段"。

  l20. §5.10 inspect 关闭测试合约重复但分散

  §5.10 (overlay 合同) 和 §3.6 (viewport 合同) 都涉及 inspect 关闭,但测试侧没说两个合同一起验证。

  修复:§9 验证脚本加一行 "InputHandlerTest 与 TileMapViewportTest 必须各自包含 inspect/targeting 关闭路径测试,且二者通过同一 OverlayState fixture 联动验证"。

  l21. Frame kind 为 sealed class / open enum 仍未冻结

  §5.9 说 ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION / ACTIVE_TALENT_SLOT_CHOICE 为 frame kind。但 PR-03 加新 kind(ITEM_DUAL_COMPARE)时,overlay layer 合同会不会被破?

  修复:§5 加一条 "ModalFrameKind 为 sealed hierarchy;新增 frame kind 不破坏 overlay layer 合同;若新 kind 需要新 layer,必须开新 PR 修改本合同"。

  l22. §2 ToME 设计合同的术语对中文 reviewer 不友好

  §2.5 "marketing hero、说明页式首屏、大块装饰渐变、纯卡片堆叠" 是 UX 业内英文术语。

  修复:§2.5 加链接或简短括号注释,或在文档底部加一个 mini glossary。

  l23. §10.11 "viewport-edge-clamp" 只截一张图

  "右下边缘" 和 "左上边缘" 两个场景只截一张证据。建议拆为 viewport-edge-clamp-top-left + viewport-edge-clamp-bottom-right 两张,以确认 clamp 在两个方向都对。

  l24. §3.7 "玩家靠近左上 / 右下边缘" 的 clamp 优先级未说

  如果玩家在地图右下角,水平 clamp 与垂直 clamp 同时触发,顺序如何?并列即可,但需明确"两轴独立 clamp"。

  修复:§3.7 改为:"水平和垂直 clamp 独立计算;边角玩家时两轴同时 clamp,不存在顺序依赖"。

  l25. §0 UI/UI-demo.png 不在仓库?

  文档引用 UI/UI-demo.png,但当前 review 上下文没看到这个文件。如果 demo 图缺失,§2 / §10.10 / §UI01-1-M05 全部无法验证。

  修复:§0 加一句 "该文件必须随本 PR 进入仓库;若缺失,maintainabilityLint 必须报缺失资源"。

  (可能这是 PR-00 提供的,但应在文档显式声明前置依赖 / 文件位置。)

  ---
  四、测试覆盖二轮缺口

  ┌──────────────────────────────────────────────┬──────────────────────────┬──────────────────────────────┐
  │                   测试合同                   │           当前           │             建议             │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ mapBounds % cellSize == 0 对所有 tested 视窗 │ §3.9 / §8.4 暗示但未显测 │ GameShellLayoutTest 显式断言 │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ viewport prev state 一致性(N8)               │ 缺失                     │ TileMapViewportTest 加       │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ tile-to-screen byte-identical(N5)            │ 缺失                     │ TileRendererCanvasTest 加    │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ presenter parity(N3)                         │ 缺失                     │ 新增 *PresenterParityTest    │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ backdrop alpha 取自 stack peek(N7)           │ 缺失                     │ ModalStackTest 加            │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ inspect 关闭 viewport snap(N4)               │ 缺失                     │ TileMapViewportTest 加       │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ 死区在窄窗口下 cap(N6)                       │ 缺失                     │ TileMapViewportTest 加       │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ modal active 时 inspect cursor 视觉(N11)     │ 缺失                     │ TileRendererCanvasTest 加    │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ layer 切换处 batch.flush 调用(N2)            │ 缺失                     │ TileRendererCanvasTest 加    │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ git grep -i ascii client/src == 0(N1)        │ 缺失                     │ maintainabilityLint 规则     │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ anchor 失效 tooltip 隐藏(l17)                │ 缺失                     │ TooltipPlacementTest 加      │
  ├──────────────────────────────────────────────┼──────────────────────────┼──────────────────────────────┤
  │ frame kind sealed(l21)                       │ 缺失                     │ 单元测试或 KSP 验证          │
  └──────────────────────────────────────────────┴──────────────────────────┴──────────────────────────────┘

  ---
  五、文档治理建议

  1. §1 阶段目标 6 条:目标 6 "删除 client ASCII fallback / debug renderer 依赖" 与其他 5 条目标性质不同(其他是新增合同,这条是删除决策)。建议把它从"阶段目标"挪到 §0 顶部的"重大决策"
  段或独立 §11 删除清单段,避免读者把它当成与 viewport 合同同级的功能目标。
  2. §4.10 "renderer 热循环":"热循环" 在 LibGDX 上下文清楚,但合同写明 "heat loop = 每帧 render 调用栈,包含 renderToCanvas、drawXxx 系列方法,不含 build phase / setup phase"会更精确。
  3. §7 非目标重复:第 8 条 "不为了拆文件新增 Helper / Utils / Manager 类型" 与 §6 末段 "禁止 Helper / Utils / Manager / Support 这类不能说明 owner 的长期类型名"
  重复。建议保留一处(§6),§7 删除。
  4. CHANGES from PR-01:文档没说本 PR 与 PR-01 已 ship 内容的差异(例如 PR-01 已修复 continue-unavailable 路径泄漏)。建议 §0 加一段 "本 PR 不回退 PR-01 已修复的 continue-unavailable
  路径泄漏 / validation overlap;golden 比对必须确认这两条仍 PASS"。
  5. §Acceptance Matrix 中 UI01-1-M05 的 source = "§2":但 §2 是设计原则文,没有明确测试断言。建议要么加程序化测试(参考 l13),要么把 source 改为 "§2 + §10.10 manual record"。
  6. §Failure Rule 没说 ASCII 删除失败如何处理:如果删除 ASCII 后某个测试断裂,先回滚 ASCII 删除还是先迁移测试?建议加 "ASCII 删除导致测试断裂时,优先迁移测试到 RenderSnapshot vector
  断言;不允许回滚 ASCII 删除"。

  ---
  六、终评

  ┌──────────────────────────┬──────────┬──────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  5. §Acceptance Matrix 中 UI01-1-M05 的 source = "§2":但 §2 是设计原则文,没有明确测试断言。建议要么加程序化测试(参考 l13),要么把 source 改为 "§2 + §10.10 manual record"。
  6. §Failure Rule 没说 ASCII 删除失败如何处理:如果删除 ASCII 后某个测试断裂,先回滚 ASCII 删除还是先迁移测试?建议加 "ASCII 删除导致测试断裂时,优先迁移测试到 RenderSnapshot vector
  断言;不允许回滚 ASCII 删除"。

  ---
  六、终评

  ┌──────────────────────────┬──────────┬──────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │           维度           │ 一轮评分 │ 二轮评分 │                                                   说明                                                    │
  ├──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ 问题诊断准确度           │ A        │ A        │ 三个核心病灶处理得当                                                                                      │
  ├──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ 合同冻结完整度           │ C+       │ B+       │ 几何 / 时间 / 交互合同全部上来,allocation 预算 + flush 策略 + presenter 一致性仍待补                      │
  ├──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ 命名与术语一致性         │ B-       │ B        │ frame 命名钉死,但 §1.4 与 §6 矛盾未消                                                                     │
  ├──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ 测试设计严密度           │ B        │ B+       │ 加入了 deadzone / cell-align / state identity 测试,但 batch flush / presenter parity / N8 prev state 仍缺 │
  ├──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ 与下游 PR 合同稳定性     │ A-       │ A-       │ self-audit 不错,但 frame kind sealed / RTL flip 仍欠预防                                                  │
  ├──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Roguelike 玩法体验考量   │ C        │ A-       │ deadzone / overlay layer / modal 居中 / floating combat 都修对;§2 ToME 设计合同是大亮点                   │
  ├──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ 大决策(删除 ASCII)讨论度 │ N/A      │ C        │ 决策正确但冲击面 / 替代路径 / 删除清单未明示                                                              │
  └──────────────────────────┴──────────┴──────────┴───────────────────────────────────────────────────────────────────────────────────────────────────────────┘

  总评:从 "Conditional Approve" 升级为 "Approve, with focused additions required"。把 N1–N12 这 12 条重大补回之后,可以正式进入实施阶段;l1–l25 大部分可以放到实现 commit / manual
  record 中处理,但 l9(token 命名)、l21(frame kind sealed)、l25(demo.png 是否在仓库)建议在合同冻结前回写,因为它们都是合同级。

  最关键的两条必须立即补:
  1. N1 ASCII 删除清单 + 替代 debug 路径 + 跨模块扫描结果
  2. N2 SpriteBatch flush 策略

  这两条不补,实现阶段一定踩坑,且踩坑时只能再开 PR-01-2 修合同,违背"一次冻结"的初衷。
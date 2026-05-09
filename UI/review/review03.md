Dark UI/UX PR-01-1 方案三轮 Deep Review

  结论先行:Approve, with cleanup before merge。
  方案合同已经达到"可冻结"状态,P0 缺口仅 7 条,其余均为表述清理 / 边界补全。建议合并到一个清理 commit 后正式开始实现。

  ---
  一、文档内部矛盾(P0,必须先消)

  A1. TileMapViewportTest 是否必须新增,合同前后不一致

  - §6 影响范围列了 TileMapViewportTest.kt 为新增项
  - §Acceptance Matrix UI01-1-M01/M02 fastCheck 把 TileMapViewportTest 当作硬依赖
  - §9 又允许 "如果仓库最终不新增独立 TileMapViewportTest",改用 GameShellLayoutTest + TileRendererCanvasTest 同名测试覆盖

  三处合同冲突。一旦实施时选择"不新增 TileMapViewportTest",fastCheck 命令会立即失败,reviewer 不知道是合同期望还是实现疏漏。

  修复:二选一,推荐保留新增。
  - 选 A:§9 删除"如果仓库最终不新增"分支;§Acceptance Matrix 维持硬依赖
  - 选 B:§Acceptance Matrix fastCheck 改为 "TileMapViewportTest 或等价 viewport focused test";§6 也用 "或等价命名"

  A2. §6 影响范围与 Deletion Checklist 没桥接

  §6 影响范围只列"新增 / 修改",没有任何 ASCII 文件的删除条目;Deletion Checklist 单独放在表格下方。reviewer 对照时容易漏。

  修复:§6 影响范围最后加一行:(ASCII renderer / model / test 删除条目见下方 Deletion Checklist),或将影响范围拆为"新增 / 修改 / 删除"三块。

  A3. §8.5 与 §8.18 重叠,且 §8.5 用了已被废弃的"overlay"含混词

  - §8.5 "terrain、prop、overlay、actor、fog、... 对同一 tile 使用同一 screen transform" — 描述性
  - §8.18 "tileToScreen(tile) 返回 byte-identical 的整数坐标" — 断言性
  - §3.11 已将 tile-side 的 "overlay" 改名为 "tile sprite overlay",§8.5 没同步

  修复:删除 §8.5(被 §8.18 完全覆盖),或保留 §8.5 同时把 "overlay" 改为 "tile sprite overlay" 与 §3.11 对齐。推荐前者。

  A4. §8.9 与 §5.1 layer 顺序在表述上冲突

  - §5.1 layer 顺序:bottom HUD/log/hotbar/footer < tooltip — tooltip 在 log 上层
  - §8.9 "tooltip 不遮挡 bottom log 最新反馈" — 听上去 tooltip 应在 log 下层

  实际语义是:tooltip 视觉层级在 log 之上,但 placement 算法避开 log 区域。当前措辞容易误读。

  修复:§8.9 改为:"Tooltip 与 modal 视觉层级高于 shell panes 和 bottom HUD;但 tooltip placement 必须避开 bottom log reserved bounds,确保 bottom log 最新反馈仍可见"。

  A5. §3.6 滚动量缺方向

  §3.6 "滚动量等于让该 tile 回到最近 deadzone 边界内的最小整 cell 距离"。距离是绝对值,方向没说。

  修复:"滚动方向 = focus tile 越出 deadzone 的方向(focus tile 在 deadzone 右侧 → viewport 向右滚);滚动量 = ceil((focus tile - 最近 deadzone 边界) / 1 cell)"。

  A6. §Major Decision 4 列了两条,但 PR-01 修复 finding 共 4 条

  §Major Decision 4 只列 continue-unavailable 路径泄漏 + validation setup overlap;PR-01 manual record 还有 outcome body 行数 cap、talent tones 对齐 / :client:goldenScreenshot outcome
   recap hash 更新等。

  修复:改为 "PR-01 已修复的所有 finding(包括 continue-unavailable 路径泄漏、validation setup overlap、outcome body 行数 cap、talent tones 对齐、outcome recap hash 等)不得在本 PR
  回退"。

  A7. §Acceptance Matrix UI01-1-M07 source 引用了不存在的段落

  UI01-1-M07 source = "§9 验证命令 / governance / anti-bloat"。但 "governance / anti-bloat" 在 §9 里没有显式段落 — 实际散在 §0 / §6 / §7。

  修复:source 改为 "§0 / §6 Deletion Checklist / §7 / §9"。

  ---
  二、合同灰色地带(P1,落地前应回写)

  B1. viewportTopLeft / visibleRange 是否随 prev state 持久化

  §3.4 identity 字段不含 viewportTopLeft / visibleRange,但 §3.4 又说 "deadzone 计算必须以上一帧 viewportTopLeft / visible range 为基线"。这两个字段是 prev state
  的内容,但合同没显式说"持久化"还是"每帧重算"。

  修复:§3.4 末尾加:"viewportTopLeft 与 visibleRange 作为 TileMapViewportState 的非 identity 字段持久化;identity 一致时随 deadzone 计算更新,identity 不一致时随 prev state 一起丢弃"。

  B2. resize 引发的 sub-cell mapBounds 微调是否触发 snap

  §3.3 说 resize 触发 snap。但若窗口微调导致 mapBounds 像素 +8,cell-aligned 后实际 mapBounds 不变(余数进入 inner padding) — 此时 snap 是过激。

  修复:§3.3 加 "只有 cell-aligned mapBounds 变化时才触发 snap;sub-cell 像素余数变化只更新 inner padding,不丢弃 prev state"。

  B3. 地图小于 viewport 时 deadzone 是否短路

  §3.9 说小地图居中。但 deadzone 算法在 visible range 大于 mapDimensions 时会算出无效值。

  修复:§3.9 加 "mapDimensions < visibleCells 任一轴时,该轴 deadzone 算法短路:viewportTopLeft 该轴固定为 mapBounds 居中位置,不响应玩家移动"。

  B4. inspect 期间 deadzone 锚点是 cursor 还是 player

  §3.5 说死区基于 visible range 中心(几何),但 viewport 跟随 focus tile。inspect 期间 focus tile = cursor,deadzone 是基于 cursor 还是 player?

  逻辑上必须基于 cursor(否则 cursor 走到屏幕边缘时 viewport 不动,玩家看不到 cursor)。

  修复:§3.5 加 "deadzone 锚点 = 当前 active focus tile;inspect / targeting 期间 focus tile = cursor;§3.7 关闭 inspect 后切回 player 并 snap"。

  B5. tooltip flip 各方向贴齐 anchor 哪条边

  §5.7 flip 顺序 right -> down -> left -> up,但没说 tooltip 如何对齐 anchor entity 的边。implementer 可能用 anchor 中心 + 一律向右偏移,导致 tooltip 与 anchor 重叠。

  修复:§5.7 加 "flip 各方向贴齐 anchor entity 对应边:right placement = tooltip 左缘贴 anchor 右缘 + tooltipFlipMargin;down placement = tooltip 上缘贴 anchor 下缘 +
  margin;以此类推。不允许使用 anchor 中心作为偏移基准"。

  B6. modal max height 的截断与未来 scroll 关系

  §5.9 "modal body 不得覆盖 footer hints"。如果 modal 内容很多,需要的高度 > content area 怎么办?

  修复:§5.9 加 "modal max height = footerTop - headerBottom - 2 * modalPadding;超出此高度的 body 内容必须由 presenter 截断或 cap 行数;modal 内部 scroll 不在本 PR 实现,留待后续 PR
  扩展 OverlayRenderFrame 字段"。

  B7. layer order 中 modal explicit tooltip 的位置

  §5.1 layer 顺序:... < tooltip < toast < modal backdrop < modal < dev-only debug overlay。
  §5.4 提到 "active modal explicit tooltip"。该 tooltip 是 layer 顺序里的 tooltip 那一层(modal 之下)还是 modal 之上?

  如果是 modal 之下,modal 会盖住自己的 explicit tooltip,合同矛盾。

  修复:§5.1 改为:... < tooltip < toast < modal backdrop < modal < modal-internal explicit tooltip < dev-only debug overlay;并在 §5.4 显式说 "modal explicit tooltip 在 modal
  上层渲染,passive tooltip 在 modal 下层被 §5.1 layer 顺序与 §5.4 抑制规则同时压制"。

  B8. dev-only debug overlay 是否允许 backdrop / dim

  §5.3 backdrop 只允许 modal frame 生成。dev-only debug overlay 在最高层,它能不能 dim 背景?

  修复:§5.3 加 "dev-only debug overlay 不得生成 backdrop、全屏 dim 或 fullscreen blur;只允许小区域诊断 hint(viewport bounds / mapBounds outline / focus tile 高亮 / layer
  id),不得阻挡正常游戏视觉"。

  B9. SpriteBatch begin/end 由谁管

  §4.11 说 "renderer pass 之间共享 SpriteBatch 时每个 pass 退出前必须 flush",但 begin/end 谁负责没说。LibGDX 惯例是最外层一次性管理。

  修复:§4.11 加 "SpriteBatch.begin() / end() 由 TileRenderer.renderToCanvas 在最外层一次性管理;renderer pass 内只允许 draw + flush,不得调用 begin / end,也不得引入第二个 batch"。

  B10. ShapeRenderer / 第二 batch / framebuffer 扩展点是否禁止

  如果 chrome 必须用 ShapeRenderer(画 stroke / fill),会破坏 §4.11 的 batch 合同(必须 batch.end() → ShapeRenderer.begin() 切换 GL state)。

  修复:§4 加一条:"不得在 renderer pass 内引入 ShapeRenderer、第二个 SpriteBatch 或 offscreen FrameBuffer;若 chrome 必须使用 ShapeRenderer,该需求必须先在合同 PR 冻结 render pipeline
  扩展点,本 PR 只允许 SpriteBatch 单一管线"。

  ---
  三、缺失的合同(P1,实施时一定踩坑)

  C1. player tile 跨地图大跳变(死亡 / respawn / 切角色)

  active focus mode 不变 + map dimensions 不变(同 zone respawn)时,player tile 单帧跳变 100 cells,deadzone 算法会算出 100 cells 滚动,瞬间从 A 区滑到 B 区。

  修复:§3 加一条 "player tile 在单帧内跳变 > deadzone cells 数量时,viewport 必须 snap 到新 player-centered + clamp 状态,不走 deadzone 最小滚动算法"。

  C2. identity 多字段同帧变化的 snap 顺序

  zone 切换通常伴随 mapDimensions 变化、可能 cellSize 变化。如果分别 snap,中间状态可能被 prev state 算法捕获。

  修复:§3.3 加 "identity 字段同帧变化时,snap 仅基于本帧 final state 计算一次;不允许中间状态被 prev state 算法捕获"。

  C3. 测试 spy SpriteBatch 标准化

  §8.24 要求 spy / fake batch 证明 pass order。如果每个测试自己实现 spy,行为不一致,测试可信度下降。

  修复:§9 加 "测试 fixture 必须使用统一的 RecordingSpriteBatch 或等价 typed spy;命名固定,放在 client/src/test/.../render/RecordingSpriteBatch.kt;不允许 ad-hoc spy 散落在多个测试"。

  C4. canonical evidence 子段清单

  §Canonical Artifact 列了 manual record 路径,但 §4.14 说 self-audit 写入 manual record,§10.15 说 ASCII deletion scan 写入 manual record。manual record 的子段清单没有 enumerate。

  修复:§Canonical Artifact 加 "manual record 必须包含以下子段:Verification Source / Commands Run / Evidence / Frame Ownership Self-Audit(§4.14)/ ASCII Deletion Scan(§10.15)/ Findings
   / Residual Risk;缺失任一必填子段,canonical evidence 无效"。

  C5. allocation 预算的测量降级路径

  §4.12 "稳态下 < 1 KB/frame"。如果项目没有 JFR / async-profiler 集成,这条无法测,变成空合同。

  修复:§4.12 加 "如无 allocation 测量基础设施,本预算作为 code review 心智模型;实际禁止由 §4.13 (immutable + 无 cross-frame ref) 与 §4.14 (self-audit) 保证。引入 allocation
  测量后,该预算转为程序化合同;manual record 必须记录测量方法"。

  C6. Token Contract 的具体默认值

  §6 Token Contract 有 4 项标注 "由最小窗口可读性决定" / "复用 spacing token 或明确值" / "受 content area 限制" — 这些不是默认值,是 hint,implementer 各取一值。

  修复:§6 给具体默认值,例如:
  - tooltipMaxWidth = 360f、tooltipMaxHeight = 200f
  - tooltipPadding = UiDesignTokens.spacing.sm(8f)
  - tooltipFlipMargin = UiDesignTokens.spacing.xs(4f)
  - modalMaxWidth = 640f(1280×800 下)、modalMaxHeight = footerTop - headerBottom - 2 * modalPadding
  - modalPadding = UiDesignTokens.spacing.lg(18f)
  - modalBackdropAlpha = 0.55f

  如果暂无最终值,标 TBD by implementation, must be locked in PR description。

  C7. cell size 的 token 归属

  §6 Token Contract 没列 cell size。§3.2 input 含 cellSize,但它是 token 还是 runtime 计算?

  修复:§6 Token Contract 加一行 cellSize: Int,引用 PR-01 真源(查 UiDesignTokens / InfoSurfaceLayout 当前值)。或在 §3 显式 "cellSize 由 PR-01 已 ship 真源决定,本 PR 不变"。

  C8. Golden namespace 与 PR-01 的关系

  §Canonical Artifact 提到 dark-uiux-pr01-1-* golden,但**是否取代 PR-01 的 dark-uiux-pr01-* golden?**有重叠的(如 shell-min-window)如何处理?

  修复:§6 或 §Canonical Artifact 加 "Golden Namespace" 子段:"PR-01 已 ship 的 dark-uiux-pr01-* golden 不删除;本 PR 新增/更新的 golden 命名 dark-uiux-pr01-1-*;若功能完全等价(如
  shell-min-window),manual record 必须显式说明替换关系并保留 PR-01 golden 的注释"。

  C9. RecordingSpriteBatch 与 §8.24 的命名约定

  §9 末段已说 "InputHandlerTest 只断言 mode / modal stack / focus transition,不复制 viewport 计算"。但 §8.24 说 "测试用 spy / fake batch" — 如果 fake 散落在多个测试且名字各异,§8.24
  无法 enforce。

  修复:见 C3,统一命名 + 放共用 fixture 路径。

  C10. nested tooltip 被禁但没有测试

  §7.11 不实现 nested tooltip,§5.4 优先级合同确保 modal 期间 passive tooltip 抑制。但没有断言 "同时只有 1 个 tooltip 渲染" 的测试。

  修复:§8 加一条 "TileOverlayRenderer 在任意 frame 渲染的 tooltip 数量 ≤ 1;包含 modal explicit tooltip 与 passive tooltip 同时存在的边界场景"。

  ---
  四、§Major Decisions / Acceptance Matrix 的细节

  D1. §Major Decision 1 与 §1.6 表述强度差异

  §Major Decision 1 强:"Tile dark UI 是唯一正式 client 渲染路径"。
  §1.6 弱:"执行 §0 的 Tile-only 删除决策"。

  修复:§1.6 改为 "执行 §0 Major Decision 1 的 Tile-only 渲染合同;本 PR 之后所有玩家可见 evidence(golden、manual screenshot、smoke artifact)都必须走 Tile dark UI"。

  D2. §Acceptance Matrix UI01-1-M05 的 fastCheck 需要程序化锚点

  UI01-1-M05 source = §2 ToME-like map-first;fastCheck = GameShellLayoutTest, TileRendererCanvasTest。但 §2 是设计原则,只有 §2.7 是程序化合同。

  修复:§2.7 末尾加程序化阈值 "在 1280×800 standard layout 下,mapBounds.width * mapBounds.height >= 0.5 * (shell.contentWidth * shell.contentHeight)";GameShellLayoutTest 加对应断言。

  D3. §Canonical Artifact 中 manual record 模板未指明

  UI/manual-records/dark-uiux-pr01-1-viewport-renderer-overlay.md 文件还不存在。

  修复:§Canonical Artifact 加 "该文件由 PR-01-1 实施时创建,模板参考 UI/manual-records/dark-uiux-pr01-shell.md,必填子段见 C4"。

  ---
  五、§7 / §8 / §9 / §10 的小细节

  E1. §7.2 鼠标 hover 与 §5.6 anchor source 的兼容声明

  §7.2 不新增鼠标 hover;§5.6 anchor source 不含鼠标位置 — 对齐 OK。但若后续引入 hover,tooltip placement 需要兼容 mouse position。

  修复:§7.2 末尾加 "若后续引入鼠标 hover,tooltip placement contract 需要新方案;本 PR 不为此预留扩展点,不允许预先放宽 anchor source 列表"。

  E2. §7.11 nested tooltip 与 §5.4 explicit tooltip

  §7.11 不实现 nested tooltip;§5.4 允许 active modal explicit tooltip。需明确两者互斥(已隐含但未显式)。

  修复:§5.4 末尾加 "active modal explicit tooltip 与 passive tooltip 互斥;同一时间最多一个 tooltip,不存在 nested tooltip"。

  E3. §8.1 与 §8.16 维度不统一

  §8.1 是窗口固定测多种地图;§8.16 是地图固定测多窗口。两者维度不同,容易漏覆盖(例如 1024×768 × 90×56 没测)。

  修复:合并为一个 matrix:"对 (1024×768, 1280×800, 1440×900) × (60×40, 70×45, 90×56) 共 9 个组合,断言 mapBounds cell-aligned + shell world 不被撑大"。

  E4. §8.20 "立即恢复"

  "关闭顶层 modal 后下一层 backdrop contract 立即恢复"。"立即" 含糊。

  修复:"关闭顶层 modal 后,同一帧 ModalStack.peek() 切换为下层 modal,backdrop alpha/color 同帧切换;不允许跨帧 fade 或闪烁"。

  E5. §9 末尾 rg 扫描范围过广

  rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii' client/src assets-src examples game/src/main/resources/data/tilesets

  game/src/main/resources/data/tilesets 范围里可能含 server 端 ASCII fixture(§Deletion Checklist 末段允许保留),会误命中。

  修复:拆为多条 rg,或加 negative pattern 排除允许路径:
  rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex' client/src
  rg -n -i 'tileset_foundation_ascii' client/src assets-src examples
  rg -n -i '\.ascii\.' game/src/main/resources/data/tilesets/index.yaml

  E6. §9 的 acceptanceContractLint 时机

  §0 顶部已说 "执行前先跑 acceptanceContractLint"。建议 §9 加注 "acceptanceContractLint 必须在 PR open 后第一次 commit 就跑通;后续每次合同修改(§Acceptance Matrix / §Major Decisions /
   §Token Contract / §Deletion Checklist)都必须重跑"。

  E7. §10.16 必填证据漏列三项

  §10.12 触发 combat feedback + COMBAT_DECISION,但证据列没有 combat-feedback-with-modal;§10.13 触发 tooltip 四角 flip,没有 tooltip-flip-corners;§10.14 触发 tooltip / modal / compare
  三视图,没有 item-tooltip-vs-modal-parity。

  修复:§10.16 补三项必填证据。

  E8. §10.11 deadzone-still 与 deadzone-scroll 截图步数没说

  "录制 10 步移动,分别截取... still frame 与... scroll frame" — 步数模糊。

  修复:"先在 deadzone 内连续移动 N1 步(N1 ≤ deadzoneCells - 1)截 still frame(viewportTopLeft 应不变);再继续移动 1 步越过 deadzone 截 scroll frame(viewportTopLeft 应位移 1
  cell);两张截图必须在 manual record 标注 viewportTopLeft 与 mapBounds"。

  E9. §10.15 ASCII deletion scan evidence 格式

  ascii-deletion-scan 是文本还是 screenshot?放哪?

  修复:"证据格式为文本 block(UI/manual-records/.../ascii-deletion-scan.txt)或 manual record 内嵌 fenced code block;包含 rg 命令、命中数、允许的非 client renderer 命中标注"。

  ---
  六、文档结构 / 表述清理(P2)

  F1. §3.1 stage viewport vs map viewport 应升格为 Glossary

  §3.1 是术语澄清,但放在 §3 第一条容易被误认为合同。建议升级为 §0 的子段 Glossary 或独立 术语 段,与 "信息架构 vs 装饰" 段并列。

  F2. §6 Token Contract 的列结构

  "Default / rule" 列里既有具体值(4、0.25f)又有规则文(由最小窗口可读性决定),信息密度差太大。

  修复:拆成两列 Default value 和 Constraint;无具体值的标 TBD, locked in PR description。

  F3. §6 Deletion Checklist 第 6 / 7 条范围模糊

  - 第 6 条 "canonical/runtime/example/content-pack visual manifest" 路径未列
  - 第 7 条 "locale 中只服务 client ASCII renderer 的 key" — "只服务" 怎么判断,谁来扫?

  修复:第 6 条列具体路径模式(如 assets-src/canonical/...、examples/.../manifest.json);第 7 条要求 manual record 在删除前列出待删 key 列表 + 消费者扫描结果。

  F4. §6 Deletion Checklist "允许保留" 列表中 ensure_ascii

  "允许 ensure_ascii JSON 输出参数继续存在" — 这是 Python json.dumps(ensure_ascii=...) 用法。Kotlin/Gradle 项目实际有这个吗?

  修复:核实是否存在;若不存在,删除该条;若存在(例如 Python 工具脚本),给路径示例。

  F5. §8 必测行为 25 条无分组

  太长不好定位,建议加小标题分组:
  - 8.1–8.4 viewport 几何
  - 8.5–8.8 渲染层级
  - 8.9–8.13 overlay layer
  - 8.14–8.16 删除合规与 ToME 视觉
  - 8.17–8.22 viewport identity / deadzone / clamp 详
  - 8.23–8.25 frame / batch / sealed kind

  F6. §3.11 "tile sprite overlay" 词汇未全文统一

  §3.11 引入 "tile sprite overlay";§8.5 仍用 "overlay";§4.7 用 "tile sprite overlay" 又同时用 "tile prop / overlay"(§7 第 5 条 "actor / tile / VFX 资源")。

  修复:全文统一为 "tile sprite overlay"(指 prop / decoration);"overlay" 单独使用时仅指 UI overlay layer(tooltip/modal/toast)。

  F7. §10.10 demo.png 对照需要 side-by-side 提示

  "对照 UI/UI-demo.png,确认第一视觉焦点仍是地图..." — reviewer 看 manual record 截图时怎么对照?

  修复:"对照 UI/UI-demo.png(在 manual record 中以 side-by-side 截图呈现);确认第一视觉焦点仍是地图,右侧装备/背包和底部日志/快捷栏是稳定工作面"。

  F8. §9 命令对 rg 的 OS 依赖

  rg 在某些 CI 镜像可能没装。

  修复:加注 "rg 必须在 CI 环境预装;若无,等价 grep -rIn 命令必须由 manual record 提供"。

  F9. §9 末尾的命名约定段落定位

  "viewport 使用 centersSmallMapWithinBounds、..." 是测试方法命名规范,放在 §9 末段。但 viewport focused lane 命令依赖测试名通配符 TileRendererCanvasTest*viewport* —
  测试方法名约定需要前置才能让通配符稳定。

  修复:把命名约定提到 viewport focused lane / overlay focused lane 命令之前,或在两个 lane 命令之后立刻给出。

  F10. §9 命名约定缺 usesIntegerCellRange / producesCellAlignedVisibleRange

  §6 影响范围列了 viewport full-cell-range focused tests,但 §9 命名约定没列对应方法名。

  修复:§9 命名约定补 producesCellAlignedVisibleRange 或 usesIntegerCellRange。

  F11. §3.7 inspect 关闭"snap" 与 §10.7 "正常恢复" 表述不一致

  §10.7 仍说 "关闭后 pane focus 和地图 viewport 正常恢复" — 上一轮我提过 l13。"正常恢复" 含糊。

  修复:§10.7 改为 "关闭后 pane focus 回到 PaneFocusAnchor.WORLD;viewport 立即 snap 到 player-centered + clamp(§3.7),下一帧 deadzone 从该 state 重算"。

  F12. §Major Decisions 编号风格不一

  §Major Decisions 4 条用阿拉伯数字。其他段(§1 / §2 / §3 / §5 / §7 / §8)也用阿拉伯数字。但 §0 的 sub-section(Acceptance Matrix / Gate Budget / Canonical Artifact / Failure
  Rule)用中文标题。建议统一,或在每个段落首句给中文 alias。

  ---
  七、本轮新发现的潜在风险

  R1. §3.5 deadzone cap 在极小 visible cells 时仍会出问题

  §3.5 cap 到 visibleCells - 2。但 visibleCells - 2 < minCells 时 cap 优先,deadzone 实际值会小于 minCells。minCells 默认 4,cap 后可能是 2 或 1。这是设计意图吗?

  如果 visibleCells = 4(极窄窗口),cap = 2,deadzone = 2 cells 占 50% viewport — 可以接受,但需要说明。

  修复:§3.5 加注 "cap 优先级高于 minCells;visible cells 极少时 deadzone 可能 < minCells,以保留滚动余量;此场景极少出现于支持的最小窗口"。

  R2. snap 时 deadzone 是否立即生效

  §3.3 snap to player-centered + clamp。snap 后当帧 deadzone 是否生效?如果生效,player tile 必然在 viewport 中心(deadzone 中心),deadzone 检查不会触发滚动 — OK。但如果实现把 snap 的
  viewport state 当作 "无效 prev state",下一帧 deadzone 可能误算。

  修复:§3.3 末尾加 "snap 后产生的 viewportTopLeft 必须作为下一帧 deadzone 计算的合法 prev state;不允许下一帧因为 'prev state 来自 snap' 而再次 recenter"。

  R3. focus mode 切换在 inspect 期间多次发生

  inspect → targeting → inspect 切换时,focus tile 可能从 cursor A 变到 target B 再回到 cursor C。每次切换都触发 snap?

  §3.3 明说 focus mode 切换触发 snap。但 inspect → targeting → inspect 过程中,如果 cursor C ≠ cursor A,snap 会让 viewport 跳变。

  修复:§3.3 加 "active focus mode 字段值变化(PLAYER ↔ INSPECT ↔ TARGETING)触发 snap;同一 focus mode 内 focus tile(cursor / target tile)切换不触发 snap,由 deadzone 算法处理"。

  R4. modal 嵌套时 footer hints 是否仍可见的合同未明确层级

  §5.9 "modal body 不得覆盖 footer hints"。但嵌套 modal 时,两个 modal body 各自不覆盖 footer,但 modal 顶层与底层叠加时,backdrop 单层(§5.3)+ 两个 modal body 各自不覆盖 footer。OK。

  但"footer hints 仍可见或可读" 在嵌套场景没显式断言。

  修复:§8 加 "ModalStack.size >= 2 时,backdrop 仍只一层,所有 modal body 均不覆盖 footer hints"。

  R5. tileToScreen 的 boundary tile 行为

  §3.11 "返回 Int 像素坐标"。但边界 tile(visible range 第一格 / 最后一格)的 screen position 是格的左上角还是中心?

  应该是左上角(LibGDX 惯例:Y 从底向上,但 sprite 用左下角)。

  修复:§3.11 加 "tileToScreen(tile) 返回该 tile 在屏幕的像素左下角坐标**(LibGDX Y-up 坐标系);所有 sprite draw 以此为基准**"。或者明确选 top-left + Y-down,取决于实际坐标系。

  R6. 测试方法命名约定的反推合同

  §9 末段说 "测试命名必须能从失败名反推出合同"。这是好原则,但如果 implementer 改测试名(例如 keepsTopLeftInsideDeadzone → viewportRespectsdeadzone),maintainabilityLint 是否能拦?

  修复:§9 加 "测试名约定通过 acceptanceContractLint 或专门的 testNamingLint 验证;改名必须同步更新本文档命名表"。

  ---
  八、终评

  ┌──────────────────────────┬──────────┬──────────┬─────────────────────────────────────────────────────────────────────┐
  │           维度           │ 二轮评分 │ 三轮评分 │                                说明                                 │
  ├──────────────────────────┼──────────┼──────────┼─────────────────────────────────────────────────────────────────────┤
  │ 合同冻结完整度           │ B+       │ A-       │ 合同覆盖 + token 默认 + deletion checklist 都到位,只剩 5 条灰色场景 │
  ├──────────────────────────┼──────────┼──────────┼─────────────────────────────────────────────────────────────────────┤
  │ 内部一致性               │ B        │ B+       │ A1–A7 7 条小冲突需消                                                │
  ├──────────────────────────┼──────────┼──────────┼─────────────────────────────────────────────────────────────────────┤
  │ 命名与术语               │ B        │ B+       │ "tile sprite overlay" 引入但未全文统一                              │
  ├──────────────────────────┼──────────┼──────────┼─────────────────────────────────────────────────────────────────────┤
  │ 测试设计严密度           │ B+       │ A-       │ §8 加到 25 条 + §9 命名约定;C3 spy fixture / C10 nested 抑制仍缺    │
  ├──────────────────────────┼──────────┼──────────┼─────────────────────────────────────────────────────────────────────┤
  │ 与下游 PR 稳定性         │ A-       │ A        │ sealed ModalFrameKind / golden namespace / token contract 全部冻结  │
  ├──────────────────────────┼──────────┼──────────┼─────────────────────────────────────────────────────────────────────┤
  │ Roguelike 玩法体验       │ A-       │ A        │ deadzone / overlay layer / map-first 验证 / inspect snap 全到位     │
  ├──────────────────────────┼──────────┼──────────┼─────────────────────────────────────────────────────────────────────┤
  │ 大决策(ASCII 删除)透明度 │ C        │ A-       │ Major Decisions + Deletion Checklist + rg 扫描齐全                  │
  └──────────────────────────┴──────────┴──────────┴─────────────────────────────────────────────────────────────────────┘

  整体定级:Approve, with cleanup before merge。

  P0 必改清单(7 条):
  1. A1 TileMapViewportTest 是否必须新增,三处合同对齐
  2. A4 §8.9 与 §5.1 layer 顺序的措辞冲突
  3. A5 §3.6 滚动量缺方向
  4. A6 §Major Decision 4 列全 PR-01 修复 finding
  5. B7 modal explicit tooltip 在 layer order 中的位置
  6. B9 SpriteBatch begin/end 归属
  7. C6 Token Contract 给具体默认值或显式标 TBD

  P1 建议补完清单(15 条):A2 / A3 / A7 / B1–B6 / B8 / B10 / C1–C5 / C7–C10。

  P2 表述清理(20+ 条):F1–F12 / E1–E9 / R1–R6。

  P0 一次清理 commit + manual record 同步,可正式进入实现。P1/P2 可以并入实现 commit 或在 manual record 留痕处理。
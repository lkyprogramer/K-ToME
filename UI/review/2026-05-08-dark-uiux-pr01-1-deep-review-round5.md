# Dark UI/UX PR-01-1 方案深度 Review 报告（第五轮 / image-aligned）

> 评审日期：2026-05-08
> 评审范围：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`（328 行版本）
> 视觉参考真源：`UI/UI-demo.png`（plan §0 line 8 已声明为 canonical layout reference，并加入 "缺失时 PR-01-1 不允许关闭" 硬约束）
> 上游报告：[Round 1](./pr01-01.md) / [Round 2](./pr01-review2.md) / [Round 3](./review03.md) / [Round 4](./2026-05-08-dark-uiux-pr01-1-deep-review-round4.md)
> 视角：合同自洽 / 边界可计算 / 文档间引用一致 / 测试与 manual record 闭环 / Roguelike 玩家体验底线 / **§2 设计合同与 UI-demo.png 的 element-by-element 一致性**

---

## 0. TL;DR

Round 4 三类清单修复进度：

| 等级 | Round 4 数量 | Round 5 状态 |
| --- | ---: | --- |
| Round 4 P0 (N1, N8) | 2 | **全部已修 ✅**（§3.5 cap 已加 `max(0, ...)` + STRICT_FOLLOW 退化说明；§6 Token Contract `modalMaxWidth` 改为 `min(640f, shellContentWidth - 2 * modalPadding)` 并新增 worked example 表） |
| Round 4 P1 (N2/N4/N5/N6/N7/N9/N15/N19) | 8 | **全部已修 ✅**（§3.8 `>` / `==` 显式；§3.5 几何中心给 `floor(visibleCells / 2)` 公式；§5.7 LibGDX Y-up 公式 + sign(direction)；§5.4 tooltip 优先级合并 keyboard/focus 为 focused interactive entity + Glossary 新条；§5.6 anchor source 穷尽 7 类 + "新增必须新开 PR"；§6 Modal worked examples 表；§4.6 删除 `internal` 选项；§0 Failure Rule 加 client/game 边界） |
| Round 4 P2 (~14) | 14 | **基本已修 ✅**（仅 N10 部分落地：UI01-1-M02 fastCheck 仍写 "或 `GameAppLifecycleTest` viewport focused case"，软合同，本轮记为 N52） |

**新增 image 作为合同引用后，本轮发现：**

- **P0 = 2**（N42 §2.4 HUD 状态条目 "生命/耐力/经验" 与 image "生命/魔法/攻击/防御" 不一致；N43 §10.10 image side-by-side 缺可重现的验收 checklist，导致 manual record "对照通过" 无可证伪标准）
- **P1 = 7**（N44 / N45 / N46 / N47 / N48 / N49 / N50）
- **P2 = 6**（N51 / N52 / N53 / N54 / N55 / N56）

**整体定级**：`Approve, ready to merge after addressing 2 P0 + 7 P1 image-alignment cleanups`。

P0 不修会导致 PR-01-1 manual record 的 "对照 `UI/UI-demo.png`" 步骤无可计算判定，且 implementer 在 HUD 设计上自由派生与 ToME-like reference 偏离的 stat 条目。P1 是 image 引入后必要的 §2 合同细化与跨段桥接。P2 全部为表述清理。

---

## 1. Round 4 修复对账

### 1.1 Round 4 P0 全部已修 ✅

| Round 4 # | 验证位置 | 落地形式 |
| --- | --- | --- |
| N1 §3.5 deadzone cap < 0 未定义 | line 87 | `cap = max(0, visibleCells - 2)`；`cap = 0 时 deadzone 退化为 0 cells, viewport 严格跟随 player / focus tile, 视为 supported STRICT_FOLLOW 退化而不是 bug` |
| N8 modalMaxWidth = 640f 越界 | line 171 + lines 174–181 | `min(640f, shellContentWidth - 2 * modalPadding)`；新增 Modal worked examples 表（`1024×768` width = `564f`，`1280×800` width = `640f`，height 锁在 `GameShellLayoutTest`） |

### 1.2 Round 4 P1 全部已修 ✅

| Round 4 # | 验证位置 | 落地形式 |
| --- | --- | --- |
| N2 player tile 跳变阈值 | line 90 | `> deadzoneCells 时必须 snap... == deadzoneCells 时仍走最小滚动算法` |
| N4 几何中心未定义 | line 87 | `几何中心索引 = floor(visibleCells / 2)，deadzone 跨度由中心两侧 floor(deadzoneCells / 2) 一致化处理` |
| N5 §5.7 right `+` / down `-` 易反 | line 124 | `本合同基于 LibGDX Y-up 坐标系：right = +X、down = -Y、left = -X、up = +Y；flip 各方向公式为 tooltipEdge = anchorEdge + sign(direction) * tooltipFlipMargin，不允许使用 anchor center 作为通用偏移基准` |
| N6 tooltip 源 keyboard/focus 重叠 | line 121 + line 25 | 链改为 `modal explicit tooltip > inspect cursor > targeting cursor > focused interactive entity`；Glossary 加 "focused interactive entity" 新条 |
| N7 anchor source 列表 "等" | line 123 | 改为穷尽 7 类（`tile center / inventory slot / equipment slot / hotbar slot / talent tree node / quest log row / modal explicit row bounds`）；"新增 anchor source 必须新开 PR 修改本合同"；anchor source 失效隐藏 tooltip，禁止 fallback 屏幕中心 |
| N9 modalMaxHeight worked example 缺 | lines 174–181 | Modal worked examples 表给 1024×768 / 1280×800 两个 shell 的 height 公式与 `GameShellLayoutTest` 锁定关系 |
| N15 `internal` Kotlin 模块作用域陷阱 | line 104 | `不得使用 Kotlin internal，更不得 export 给其他 renderer 文件` |
| N19 Failure Rule 缺 client/game 边界 | line 58 | `client ASCII renderer / model / manifest 删除导致 client 测试断裂时...；core / game 模块 ASCII fixture 不在本 PR 删除范围；若误删导致 game test 断裂，必须恢复 core / game ASCII fixture` |

### 1.3 Round 4 P2 表述清理（基本已修 ✅）

| Round 4 # | 状态 |
| --- | --- |
| N3 identity 字段重叠 | ✅ line 86 `cell-aligned mapBounds（含 mapDimensions）` 一并表达 |
| N10 UI01-1-M02 fastCheck "或" 余地 | ⚠️ line 39 改为 `TileMapViewportTest, FoundationViewportSupportTest 或 GameAppLifecycleTest viewport focused case`，比 Round 4 时强但仍保留 "或 ... viewport focused case"（本轮记为 N52） |
| N11 §3.13 / §8.12 verbatim 重复 | ✅ §8.12 line 232 改为 `canvas 测试断言 PaneFocusAnchor.WORLD focus ring 矩形与 §3.13 的 layout.shell.mapBounds 一致` |
| N13 modal explicit tooltip 用词 | ✅ 统一为 "在合同中声明" |
| N14 §3.7 重复 §3.2 snap 触发 | ✅ line 89 改为 `按 §3.2 规则触发 snap` |
| N16 §10 evidence 标签映射 | ✅ lines 311–327 新增 `必填证据映射` 表 |
| N17 §9 lint 顺序前置 ASCII scan | ✅ lines 261–263 三条 `rg` 已紧贴 `acceptanceContractLint` |
| N20 UI01-1-M05 source 程序化锚点 | ✅ line 42 `§2.7 程序化阈值 + §2.1-§2.6 设计原则 + §10.10 manual record` |
| N21 inspect-tooltip-layer 标签来源 | ✅ §10.6 line 298 `本步产出 item-modal-layer，不产出 inspect-tooltip-layer`；§10.13 line 305 `本步产出 tooltip-flip-corners 与 inspect-tooltip-layer` |
| N23 visibleCells / visibleRange Glossary | ✅ line 23 新增定义 |
| N25 nested modal manual coverage | ✅ §10.15 line 307 显式 `ITEM_DETAIL 中触发 ITEM_COMPARE` |
| N27 §10.6 inventory 措辞 | ✅ line 298 `按 Enter 或 PR-01 真源对应键位触发 ITEM_DETAIL modal` |
| N28 M03 whitebox = N/A 含义 | ✅ line 40 `doc-only self-audit` |
| N32 §4.16 self-audit 正向字段 | ✅ line 114 加 `frame 字段总数 / immutable model 列表 / 通过 reference 消费的 viewport / layout truth` |
| N34 GoldenScreenshotHarnessTest 行 | ✅ line 152 `更新 harness 注册表新增 dark-uiux-pr01-1-* golden 项；harness 主体不变` |
| N37 Failure Rule 前向约束 | ✅ line 58 `PR-01-1 之后不允许任何 PR 重新引入 client ASCII renderer / model / manifest 字段` |
| N38 §5.10 / §10.7 cross-reference | ✅ line 127 `modal 关闭只触发 ModalStack pop；viewport snap 由 §3.7 inspect / targeting 关闭路径独立触发，不与 modal 关闭耦合` |
| N40 validation overlay 措辞 | ✅ line 206 改为 `validation 系列 standalone screen` |
| N41 §8.5 与 §8.1 9 组合矩阵 | ✅ §8.5 line 222 转为 "矩阵必须分别输出 cell-aligned 断言结果与 shell world size 断言结果" 增量约束，不再是 §8.1 的 subset |

---

## 2. 引入 UI-demo.png 后的硬阻塞（P0 = 2）

`UI/UI-demo.png` 自 §0 line 8 起已升格为 canonical layout reference（"缺失时 PR-01-1 不允许关闭"），并被 §2、§8.25、§10.10 三处合同直接消费。本轮重点 audit 合同声明与 image 实际信息架构的 element-by-element 一致性。

### N42. §2.4 HUD 状态条目 "生命/耐力/经验" 与 image 实际显示 "生命/魔法/攻击/防御" 直接矛盾

**位置**：§2.4 line 76

```
4. 底部是唯一 HUD 与日志带：生命/耐力/经验、快捷栏、关键日志和紧凑快捷键提示必须稳定，
   不再出现第二套状态栏。
```

**Image 实际显示**（bottom HUD 最左 portrait + stats 区）：

```
英雄
破碎前哨   层 1/2
[red bar]    152 / 152      ← 生命 (HP)
[green bar]   84 / 84       ← 魔法 / 内力 / 法力 (MP)，不是耐力
攻击 38     防御 13          ← 数值 stat，不是经验条
```

image 既没有 "耐力" 进度条，也没有 "经验" 进度条；显示的是 `HP / MP / Atk / Def` 这种 ToME-like 经典玩家状态四元组，外加 floor depth (`层 1/2`) 与 character class label (`英雄`)。

**为什么是 P0**：

1. plan §0 line 8 已声明 image 是 canonical 视觉参考且 "缺失时 PR-01-1 不允许关闭"；§2 又用 "生命/耐力/经验" 自行派生与 image 不一致的 HUD 内容，合同自相矛盾。
2. PR-02 / PR-03 / PR-04 等下游 PR 实施 HUD 资源时会优先 reference plan §2.4 还是 image？两者矛盾时实施者的自由派生空间过大，golden 跨 PR 不稳定。
3. ToME-like 玩家关键体感是 HP/MP/Atk/Def 四元组——把 "耐力" 写成 stamina-like（ToME 里无）或把 "经验" 写成 XP bar（ToME 里 XP 是 modal/sheet，不是 HUD bar）会让玩家在长跑模式下读盘面节奏完全不同。
4. PR-01-1 本身不实现 HUD 内容，但合同冻结的就是 "稳定不再变" 的口径——口径错了后续 PR 会沿用错误。

**最小修复**（任选其一，推荐选 A）：

- **选 A（推荐）**：§2.4 改为以 image 为准：
  > "底部是唯一 HUD 与日志带：左侧固定 portrait + 角色名 + dungeon/floor 标签 + 生命 / 魔法（或同位 ToME-like 资源条）+ 攻击 / 防御等核心数值 stat；中段是技能 / 天赋 / 物品快捷栏；右段是关键日志（含目标 / 路线提示 / 战斗反馈）与紧凑快捷键提示。HUD 不再出现第二套状态栏。"
  > 同时在 §6 Token Contract 或 §10.10 manual record 注明 "经验进度若实现为 HUD bar 须新开 PR 改 §2.4 与 token，不允许在 PR-02+ 隐式追加"。

- **选 B**：保留 plan 文字 "生命/耐力/经验" 不变，但在 §0 line 8 image 引用处加注 "image 显示的 HUD stat 条目（HP/MP/Atk/Def）为 PR-02 资源参考；§2.4 'HUD 关键状态' 文字描述以 §2.4 口径为准；不一致时由 PR-01-1 manual record findings 显式裁决"。

选 A 显著降低后续 PR 派生空间；选 B 是最低成本不冲突方案但损失 image 的 freeze 价值。

### N43. §10.10 "对照 `UI/UI-demo.png` 以 side-by-side 截图呈现" 缺可重现的验收 checklist

**位置**：§10.10 line 302

```
10. 对照 `UI/UI-demo.png`，在 manual record 中以 side-by-side 截图呈现，
    确认第一视觉焦点仍是地图，右侧装备/背包和底部日志/快捷栏是稳定工作面，
    不是装饰性卡片堆。
```

**问题**：

- "side-by-side 截图呈现" 没有规定比较粒度（视觉印象？区域占比？element 存在性？）。
- "第一视觉焦点仍是地图" 已被 §2.7 程序化阈值 (`mapBounds * 面积 ≥ 0.5 * shellUsableContentArea`) 锁住；§10.10 重复说一遍但不给可验证的验收点。
- "右侧装备/背包和底部日志/快捷栏是稳定工作面，不是装饰性卡片堆" 是判断主观词；reviewer 无法从 manual record 判定 "对照通过" 是否合规。

**为什么是 P0**：

PR-01-1 把 image 升格为 canonical 后，`UI01-1-M05` 的 owner gate 链是 `goldenScreenshot + manual record §10.10`。`§10.10` 是唯一依赖 image 比较的人工步骤，它的可证伪强度直接决定了 image 引用的实际 enforcement。当前措辞 → manual record reviewer 实际只能写 "已 side-by-side, 看起来 ToME-like, 通过"，与 Round 4 之前的 §2.7 文字阈值差距没本质拉开。

**最小修复**：

§10.10 改为 region-by-region acceptance checklist，并要求 manual record 明确 PASS / FAIL 每一项：

```
10. 对照 UI/UI-demo.png 在 manual record 输出 side-by-side 截图与下表逐行验收：

| Region | 验收 | 通过判定 |
| --- | --- | --- |
| 中央地图 | mapBounds 占 shellUsableContent 像素面积 ≥ 50% | §2.7 程序化阈值已断言 |
| 左侧 rail | 仅含模式入口/导航 icon 列, 不重复完整角色状态 | image 显示 5 icon column; 实现的 icon 数量与功能由 PR-02 决定, 但本步必须在 manual record 列出实际 icon list |
| 右侧 panel | 至少包含装备 / 铭刻 / 背包三个 grid section, 且具备 ground items 入口（panel 顶部或 modal） | image 显示 4 section: 地面物品 / 装备 / 铭刻栏 / 背包；本步 manual record 必须列出实际 section 列表与排序 |
| 底部 HUD | 至少包含 portrait + 角色名 + dungeon/floor + 生命 / 魔法 (或同位状态条) + 攻击 / 防御 + 快捷栏 + 日志 + 快捷键提示, 不出现第二套状态栏 | image 是 ToME-like reference; manual record 必须列出实际 HUD 子区域列表 |
| 视觉基调 | 暗色石质 / 金属边框 / 低饱和背景, 不出现 marketing hero 或纯卡片堆叠 | 主观但可证伪：manual record 必须明示 "未观察到 hero / 卡片堆" |
```

### 整体修复优先级

N42 与 N43 必须一起修：N42 改正 §2.4 文字、N43 把 §10.10 改成 region-by-region checklist，否则任何一方独自落地都会留下不闭环。

---

## 3. 合同灰色地带（P1 = 7）

### N44. §2.3 "右侧装备/铭刻/背包/资源面板" 缺 image 显示的 "地面物品" section

**位置**：§2.3 line 75

```
3. 右侧是玩家装备/铭刻/背包/资源面板，使用 grid、slot、icon、数量和短标签；
   长解释进入 tooltip/modal/log，不挤占地图。
```

**Image 实际**：right panel 顶部第一个 section 是 `地面物品`（"废弃边境前哨,短局的起点"+ `碎晶: 0` + 3 ground item slots），上方还有玩家在当前 tile 可拾取物品的列表。

`地面物品` 是 Roguelike "loot awareness" 关键交互——玩家走到 tile 上立刻看见可拾取物，按 `g` 拾取。把它合并进背包 section 会丢失这个 affordance。

**修复**：§2.3 改为：

> "右侧是 ground items + 装备 + 铭刻 + 背包 + 资源面板的稳定垂直堆叠，使用 grid、slot、icon、数量和短标签；ground items 默认置顶以最大化 loot awareness；长解释进入 tooltip/modal/log，不挤占地图。"

或保留措辞但在 §6 影响范围 / §2.7 注释中 explicit 列出 image 显示的 4 section。

### N45. §5.6 anchor source 穷尽 7 类列表缺 "engraving slot bounds" 与 "ground item slot bounds"

**位置**：§5.6 line 123

```
6. Tooltip anchor source 穷尽为 tile center、inventory slot bounds、equipment slot bounds、
   hotbar slot bounds、talent tree node bounds、quest log row bounds、modal explicit row bounds 这七类...
```

**Image 实际显示**还有两类独立 grid：

- `铭刻栏` (engraving / sigil slots): `5. 治疗之印 / 6. 相位门 / 7. 铁壁之印 / 8. 净除之印` + 6 icon slots
- `地面物品` (ground item slots): 3 slots 在 right panel 顶部

如果实施者把铭刻 anchor 视为 inventory slot 的 subset → 拖累 inventory tooltip presenter 与 engraving 业务字段（如 `等级 / 蓝条消耗 / 冷却`）混在一起。如果按 §5.6 字面读 → engraving slot tooltip 就 "失去 anchor source 资格" 必须 fallback。两者都是 P1 风险。

**修复**：§5.6 列表从 7 类扩展到 9 类，新增：

```
- engraving slot bounds (sigil / inscription / glyph 等同位 ToME-like 镶嵌槽)
- ground item slot bounds (right panel ground items section / floor 拾取面板 slot)
```

或将列表分组为 `inventory family (player carrying)` / `equipment family (worn)` / `engraving family (inscribed)` / `world family (ground / map)` / `panel family (talent / quest / hotbar / modal explicit)`，避免后续再次需要逐条新增。

### N46. §2.4 没有展开 bottom HUD 的子区域结构

**位置**：§2.4 line 76 + §6 line 142

`§2.4` 仅说 "底部 HUD 与日志带：生命/耐力/经验、快捷栏、关键日志和紧凑快捷键提示" — 这是一行平铺描述。

**Image 实际**：bottom HUD 在视觉上明确分 4 个垂直 column：

```
| portrait + crest + level 7   | skill cards 1/2/3   | hotkey legend column      | log + 路线提示 + bottom hint |
| 名字 / floor / HP / MP /     | 猛击/盾击/格挡姿态  | i背包 / g拾取 /          | 你进入了地牢...              |
| 攻击 / 防御                  |                     | Ctrl+S 保存 / L 调整装备 | 1-4 使用天赋 / 5-8 使用铭刻 |
```

`TileShellRenderer` 实施时如果按 §2.4 一行描述自由派生，4 column 之间的 padding、min width、对齐基线都可能与 image 偏离，golden 跨 PR 不稳定。

**修复**：§2.4 在 "底部是唯一 HUD 与日志带" 之后展开：

> "bottom HUD 的子区域固定为 4 个 horizontal slot：(a) portrait + 角色名 + dungeon/floor + HP/MP + 核心数值 stat；(b) skill / talent 快捷栏卡片；(c) 紧凑功能键 legend (背包 / 拾取 / 保存 / 装备调整 等)；(d) 关键日志 + 目标 / 路线提示 + 方向键 / 数字键 hint。子区域之间的 min width / padding 由 PR-01-1 在 `UiDesignTokens.fixed` 加 token 锁定（见 §6 Token Contract 待补字段）；新增子区域必须新开 PR 修改本合同。"

### N47. §10.10 image side-by-side 缺验收 checklist（与 N43 同根但本身是 P1 子项）

见 N43 修复方案的 region 表；N47 的具体诉求是 "checklist 必须出现在 §10.10 而不是在 manual record 模板里" — 否则 plan close 时 reviewer 仍要回去推 manual record 作者。建议 §10.10 直接内嵌 region 表，manual record 模板 reference §10.10。

### N48. §3 / §4 fog 在 viewport 中的渲染范围未定

**位置**：§3.10 line 92 + §3.12 line 94 + §4.7 line 105

§3.10 `mapDimensions < visibleCells 的轴必须短路 deadzone：该轴 viewportTopLeft 固定为居中结果` — 但只规定 viewport 几何，没规定 fog 在 mapBounds 大于 mapDimensions 时怎么渲染。

§3.12 `terrain、prop、tile sprite overlay、actor、fog、ground loot marker、targeting cursor、inspect cursor、combat feedback 必须通过同一个 tileToScreen transform` — 只说 transform 一致，没说 fog 是否在 tile range 之外仍渲染。

**Image 实际**：可见 fog（黑色渐变/暗色）覆盖 mapBounds 内但 mapDimensions 之外的区域（即 image 中央的房间四周纯黑石雕装饰区）；这是 ToME-like fog-of-war 的标准行为，但 plan 没显式合同。

**风险**：implementer 写 fog renderer 时可能实现两种行为：
- (A) fog 仅在 mapDimensions 范围内绘制 → image 那种 "整个 mapBounds 暗" 的视觉无法复刻；
- (B) fog 在 mapBounds 全区域绘制 → 与 §3.12 "通过 tileToScreen transform" 字面冲突（mapBounds 内但 mapDimensions 外的位置没有 valid tile index）。

**修复**：§3 加一条（建议 §3.13 之后新设 §3.14）或 §4.7 加补充：

> "fog 渲染范围为 `mapBounds` 与 `visibleRange tileBounds` 的并集；mapBounds 内但 mapDimensions 外的区域（小地图居中 padding）以 `fog.outOfMap` 颜色渲染（默认与 fog 同色或更暗），并明确不参与 tileToScreen 索引（按 mapBounds rect 像素填充）。"

### N49. §6 影响范围 `TileShellRenderer.kt` 行只描述区域名，未与 image 桥接

**位置**：§6 line 142

```
| TileShellRenderer.kt | 新增 shell renderer，绘制 left rail、right panel、HUD、log、hotbar、
  footer hints、pane focus ring；不得重拼业务文案，只消费 presentation model |
```

实施者按这一行只能知道有哪些区域，不知道 right panel 内是 4 个 section、bottom HUD 是 4 个 column。

**修复**：§6 该行改为：

> "新增 shell renderer，绘制：(a) left rail icon column；(b) right panel 垂直堆叠 4 section（ground items / equipment / engraving / inventory）；(c) bottom HUD 4 horizontal slot（portrait+stats / hotbar cards / hotkey legend / log+hint）；(d) pane focus ring；不得重拼业务文案，只消费 presentation model。section / slot 数量与排序由 §2 合同冻结，新增必须新开 PR 修改 §2。"

### N50. image 装饰 vs 信息架构边界需要在 §0 / §2 双向引用

**位置**：§0 line 16 + §2 line 71

§0 line 16 声明 "信息架构 vs 装饰" 划分 + "本 PR 不复制 UI/UI-demo.png 的装饰，只冻结 ToME-like map-first 结构"。
§2 line 71 仅说 "参考 UI/UI-demo.png 的结构方向"，未引用 §0 的 装饰/信息架构 划分。

**风险**：实施者按 §2 工作时可能复制 image 的 textured wall / portrait crest / level "7" badge / 战斗坐骑动作姿势等装饰元素，提前消耗 PR-02 资源预算。

**修复**：§2 line 71 末尾加一句：

> "image 的装饰部分（textured wall / portrait crest / level badge / sprite art 等）由 PR-02 / PR-05 实施；PR-01-1 不复制装饰，仅冻结 ToME-like 信息架构（左 rail / 中地图 / 右 panel section 列表 / 底部 HUD 子区域列表 / map-first ≥ 50% 阈值）。"

并在 §10.10 region checklist 加一行：

> "装饰元素（textured wall / portrait crest / level badge / sprite art）不在本步验收范围；如 manual record 截图含装饰差异，需明示 'PR-02 范围, 已忽略'。"

---

## 4. 表述与结构清理（P2 = 6）

### N51. §10 manual record 模板缺 image region map 子段

§0 Canonical Artifact line 52 列出 manual record 七个必填子段（`Verification Source / Commands Run / Evidence / Frame Ownership Self-Audit / ASCII Deletion Scan / Findings / Residual Risk`）。引入 image 验收后，建议加第八个子段 `Image Region Comparison`：

> "Image Region Comparison 子段必须列出 §10.10 region checklist 的逐行 PASS / FAIL 与说明；缺失该子段, canonical evidence 无效。"

### N52. UI01-1-M02 fastCheck 仍写 "或 GameAppLifecycleTest viewport focused case" 软合同

`fastCheck = TileMapViewportTest, FoundationViewportSupportTest 或 GameAppLifecycleTest viewport focused case`

"或" 让 owner gate 失去确定性。Round 4 留给本轮的 N10 唯一未消项。

**修复**：要么固定 `FoundationViewportSupportTest`（不存在则 PR 内新增）作为硬合同，要么删 "或 GameAppLifecycleTest viewport focused case" 并接受 fastCheck 范围更窄。

### N53. §4.7 layer 列表中 player tile indicator 与 cursor 关系不明

§4.7 `terrain / prop / tile sprite overlay / actor / fog / cursor / loot marker / combat feedback`. image 显示 player 中心有 yellow 选择 ring，但合同没区分 "player tile indicator" 与 "cursor"。如果合并进 cursor → cursor 必须永远 active 跟随 player（与 cursor "可见时才显示" 的传统语义冲突）；如果合并进 actor → 需要在 actor pass 内特判 player.

**修复**：§4.7 末加 "player tile indicator（默认随 player 跟随的 yellow / accent 高亮 ring）属 actor pass 的子层；cursor 仅在 inspect / targeting active 时渲染". 或在 §3.12 列表内单独点名 player tile indicator.

### N54. §5.6 hotbar slot bounds 颗粒度未定（card vs icon）

image hotbar 是 "数字 + icon + 名字" 的 card layout. tooltip anchor 是整个 card bounds 还是仅 icon bounds？两者会导致 tooltip flip 后位置不同（card bounds 较大, flip down 后更易压 log）.

**修复**：§5.6 列表项 "hotbar slot bounds" 改为 "hotbar slot bounds（slot 完整可视 bounds, 含 number / icon / label, 不细分 sub-region）".

### N55. §6 Token Contract 缺 right panel section + bottom HUD column 的 token 占位

N44 / N46 引入新 region 后, §6 Token Contract 应至少占位:

```
| rightPanelSectionCount | Int | 4 | ground / equipment / engraving / inventory; 新增必须改 §2.3 |
| bottomHudSlotCount     | Int | 4 | portrait / hotbar / hotkey / log; 新增必须改 §2.4 |
```

或在 Constraint 列写 "本 PR 不引入数值 token, 仅冻结 section / slot 概念; PR-02 + 引入实际 token". 后者更符合 "本 PR 不实现资源" 的非目标.

### N56. §10.16 ASCII deletion scan 子段命名建议

§10.16 line 308 新增 ASCII 删除扫描 manual record 子段, 但 §0 Canonical Artifact line 52 七个必填子段里写的是 `ASCII Deletion Scan`. §10.16 句子里写 "ASCII 删除扫描摘要", 两处 verbatim 不一致, 容易让 manual record 作者只写其中一种.

**修复**: §10.16 末尾改为 "...写入 manual record 的 `ASCII Deletion Scan` 子段（与 §0 Canonical Artifact 子段名一致）".

---

## 5. Roguelike 玩法与 image alignment 维度复核

| 维度 | R3 | R4 | R5 | 说明 |
| --- | --- | --- | --- | --- |
| 合同冻结完整度 | A- | A | **A** | Token Contract worked example 表 + Glossary 完整收口 + STRICT_FOLLOW 退化条款全到位 |
| 内部一致性 | B+ | A- | **A-** | A1–A7 全消, N42 引入 §2.4 与 image 矛盾, 修复后即升 A |
| 命名与术语 | B+ | A- | **A** | "tile sprite overlay / focused interactive entity / visibleCells" 全文统一 |
| 测试设计严密度 | A- | A | **A** | RecordingSpriteBatch 路径 + 命名约定 + acceptanceContractLint 重跑触发全部冻结 |
| 与下游 PR 稳定性 | A | A | **A** | sealed `ModalFrameKind` + `dark-uiux-pr01-1-*` namespace + Modal worked example 表全部冻结 |
| Roguelike 玩法体验 | A | A | **A** | deadzone / overlay layer / map-first ≥ 50% / inspect snap 全到位; image-aligned HUD stat 修复后玩家感受将与 ToME-like reference 完全对齐 |
| 大决策（ASCII 删除）透明度 | A- | A | **A** | Major Decisions + Deletion Checklist + 三条 rg scan + non-client 允许列表全部清晰 |
| **Image alignment（新维度）** | – | – | **B+** | image 升格为 canonical 后, §2.4 与 image HUD stat 不一致 (N42 P0); §10.10 缺 region checklist (N43 P0); §2.3 / §2.4 / §5.6 / §6 多处需细化 (N44–N49 P1) |

**整体定级**：`Approve, ready to merge after addressing 2 P0 + 7 P1 image-alignment cleanups`.

Round 3 → "with cleanup before merge"; Round 4 → "ready after 2 P0 + 8 P1"; Round 5 → "ready after 2 P0 + 7 P1 image-alignment cleanups". 每轮 P0 数量在 image 引入前持续下降; 本轮 P0 重新出现的 2 项均为 image 引入触发的合同自洽问题, 不是回退.

---

## 6. 行动清单

### 必改（P0 = 2, image alignment 闭环前）

1. **N42**: §2.4 改 HUD 状态条目以 image 为准: 选 A 推荐 (改写为 "生命 / 魔法 (或同位 ToME-like 资源条) + 攻击 / 防御等核心数值 stat"); 选 B 备选 (在 §0 line 8 加 "image vs §2.4 不一致由 manual record 显式裁决" 注释).
2. **N43**: §10.10 改为 region-by-region acceptance checklist (中央地图 / 左 rail / 右 panel sections / 底部 HUD slot / 视觉基调 5 行), 每行有可证伪通过条件; manual record 必须逐行 PASS / FAIL.

### 应补（P1 = 7, manual record 闭环前）

3. **N44**: §2.3 加入 "ground items" section (置顶); 或在 §6 影响范围 / §2.7 注释中 explicit 列出 image 4 section.
4. **N45**: §5.6 anchor source 列表从 7 类扩展到 9 类 (新增 engraving slot bounds + ground item slot bounds); 或重新分组为 5 family.
5. **N46**: §2.4 展开 bottom HUD 4 子区域 (portrait+stats / hotbar / hotkey legend / log+hint).
6. **N47**: §10.10 region checklist 必须内嵌 §10.10 而不是仅在 manual record 模板, 与 N43 一并修.
7. **N48**: §3 / §4 加 fog 在 mapBounds 内但 mapDimensions 外的渲染合同.
8. **N49**: §6 `TileShellRenderer.kt` 行展开 4 section / 4 slot 列表; section / slot 数量冻结由 §2 合同决定.
9. **N50**: §2 line 71 + §10.10 加 "image 装饰由 PR-02 / PR-05 实施, PR-01-1 不复制" 的双向引用.

### 表述清理（P2 = 6, 可与实现 commit 合并）

10. **N51**: manual record 加第八个必填子段 `Image Region Comparison`.
11. **N52**: UI01-1-M02 fastCheck 删 "或 GameAppLifecycleTest viewport focused case" 软合同.
12. **N53**: §4.7 / §3.12 区分 player tile indicator vs cursor 层级.
13. **N54**: §5.6 hotbar slot bounds 颗粒度明示 "整 card bounds, 不细分".
14. **N55**: §6 Token Contract 加 `rightPanelSectionCount / bottomHudSlotCount` 概念占位 (数值 token 由 PR-02 + 引入).
15. **N56**: §10.16 措辞与 §0 Canonical Artifact `ASCII Deletion Scan` 子段名 verbatim 对齐.

### 强项保留（不要在清理 commit 中误改）

- §0 Glossary 六条术语澄清
- §0 Major Decisions 4 条强声明
- §0 Acceptance Matrix 七列结构 + Failure Rule 三段（含 client/game 边界 + 前向约束）
- §0 Canonical Artifact 七子段必填清单
- §3.2–3.4 identity / persistence / snap 一致性合同
- §3.5 deadzone 公式 + cap = `max(0, visibleCells - 2)` + STRICT_FOLLOW 退化条款
- §3.8 `>` / `==` 阈值显式
- §3.12 LibGDX Y-up + Int 像素坐标
- §4.6 aggregate frame 局部化禁止 + 删 `internal`
- §4.11 SpriteBatch begin/end 单点管理
- §4.13 ShapeRenderer / 第二 batch / framebuffer 禁止
- §4.14 allocation 预算降级路径
- §4.16 frame ownership self-audit (含正向字段)
- §5.1 完整 layer 顺序（含 modal-internal explicit tooltip + dev-only debug overlay 不得渲染 full-map ASCII）
- §5.3 backdrop 单层 + dev-only debug overlay 禁止 backdrop / dim / blur
- §5.4 tooltip 优先级 4 级链 + Glossary `focused interactive entity` 收口
- §5.6 anchor source 穷尽 + "新增必须新开 PR" 锁
- §5.7 LibGDX Y-up 坐标系 + `tooltipEdge = anchorEdge + sign(direction) * tooltipFlipMargin`
- §5.9 modal max height 公式 + future scroll 不在范围
- §5.10 modal 关闭 / viewport snap 解耦 cross-reference
- §6 Token Contract Modal worked examples 表
- §6 Deletion Checklist 路径明示 + non-client 允许列表
- §8 四 subsection 分组 + nested modal 单层 backdrop (§8.20 / §8.21) + RecordingSpriteBatch (§8.14)
- §9 lint 顺序 ASCII scan 前置 + 测试命名约定锁
- §10 必填证据映射表 + ASCII 删除扫描 manual record 子段

---

## 7. 终评

引入 `UI/UI-demo.png` 作为 canonical layout reference 是本轮最关键的合同升级——把 §2 ToME 设计合同从 "文字描述 + §2.7 程序化阈值" 升级成 "文字描述 + §2.7 阈值 + image 视觉真源". 升级的代价是必须把 §2 文字与 image element 完全对齐, 否则反而引入新的合同自相矛盾. 本轮 P0(N42, N43) 都属于这类 "升级触发的对齐债".

P0(2) + P1(7) 一次清理 commit + manual record 同步, 即可正式进入实现. P2(6) 可并入实现 commit.

合同从 Round 3 "Approve, with cleanup before merge" → Round 4 "Approve, ready after 2 P0 + 8 P1" → Round 5 "Approve, ready after 2 P0 + 7 P1 image-alignment cleanups". Round 1 提的三个长期合同（player-centered viewport / TileRenderer 拆分 / overlay layer）冻结目标继续保持; 本轮新增的第四长期合同——`UI-demo.png` 信息架构 freeze——在 P0 / P1 修复后也将达到与前三个同等的可执行强度.

下一个里程碑仍是 PR-01-1 实施 commit + 第一份 manual record (含新加的 `Image Region Comparison` 子段). 到那一步, 本评审可以转为 **post-implementation 验证**.

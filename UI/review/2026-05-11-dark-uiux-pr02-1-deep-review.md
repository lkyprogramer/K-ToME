# Dark UI/UX PR-02-1 Demo Shell Foundation 深度审查

- **审查对象**: `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md`
- **基准 DEMO**: `UI/UI-demo.png` (1672×941)
- **上游基线**: PR-00 (style/pipeline)、PR-01 (shell layout)、PR-01-1 (viewport overlay)、PR-02 (UI chrome sprite pilot)
- **审查角色**: 资深 Roguelike/ToME 系统策划总监 + UI/UX 框架审查负责人
- **审查日期**: 2026-05-11
- **结论**: **有条件通过 (Conditional Approve)**。文档结构与治理纪律继承到位，能把"局内主 shell 从旧 text-first 升级到 demo-like 骨架"作为 P0 PR 推进，但在 **demo aspect 数值边界、nav rail icon 资源闭环、右侧栏 slot 几何契约、refactor 的可测试粒度、screen-coverage-matrix 同步、qualitative failure rule 的责任主体**这六条上还有结构性缺口，按下文 M1/M2 修订后再开发为佳。

---

## 0. 审查方法

1. 把文档 §0–§12 当作 PR 合同，逐项对照 `development-governance.md` 和 `screen-coverage-matrix.md` 的硬约束。
2. 把 §3 (layout contract) / §5 (right panel + bottom deck + nav rail scaffold) 和 `UI/UI-demo.png` 做"区域 → 区域"几何比对。
3. 用 `client/src/main/kotlin/com/ktome/client/render/layout/GameShellLayout.kt`、`InfoSurfaceLayout.kt`、`TileRenderer.kt` (≈1599 行)、`TileRenderModel.kt` (≈2158 行) 评估实现可达性。
4. 把 PR-02 已上线的 `r01-ui-chrome / r01-ui-controls / r01-ui-hud-icons` 当作可用资源池，反推 §5 scaffold 是否真的能"用 PR-02 资源 + placeholder 走完"。
5. 把 §9 验收标准、§10 验证、§8 demo parity manual record 当作"实际能否锁住 demo 框架"的最后一道闸。

---

## 1. 高层结论 (TL;DR)

| 维度 | 评分 | 一句话 |
| --- | --- | --- |
| 治理继承 / Acceptance Matrix | ✅ 充分 | M01–M08 字段齐全，fastCheck/ownerGate/artifact/whitebox 都对得上 governance |
| Demo 区域分解 (§3.1) | 🟡 基本对，缺数值锚 | 区域类型齐 (14 个 region)，但 mapStage/navRail/rightPanel 的比例约束过松，1672×941 demo-aspect 没固定像素契约 |
| 渲染层级 (§4 draw order) | ✅ 合理 | 14 步 draw order 完整保留 fog/light 预留位，能为 PR-05 让路 |
| 右侧栏 scaffold (§5.1) | 🟡 容器形对，slot 几何缺契约 | 没有 slot 尺寸 / grid 列数 / hitbox 半径的最小契约，PR-03 接 item icon 时仍可能推翻几何 |
| Nav rail (§5.3) | 🟠 资源闭环不成立 | DEMO 需要 compass/bag/scroll/book/gear 5 类语义图，§6.2 表里没有这 5 个 key，PR-02 现有控件 key 也覆盖不全 |
| Bottom deck (§5.2) | 🟡 形对，hero portrait 没尺寸下限 | hero card portrait/crest 只写"placeholder 可用"，没像素或比例下限 |
| Optional sheet r01b-ui-shell-chrome (§6) | ✅ 合同结构清晰 | 10 direct + 6 reserved 计数闭合，owner contract 路径正确 |
| Failure Rule (§0) | 🟡 质性判定缺主体 | "截图仍是旧 text-first shell 必须 fail" 没指明谁做这个判定、如何记录 override |
| Standalone / Main Menu (§7) | 🟠 契约过虚 | 只有 "must not look like debug setup panel"，没像素契约，golden hash 失去判别力 |
| screen-coverage-matrix 同步 | 🔴 缺失 | PR-02-1 重排局内主 shell，但矩阵中"局内主 shell"行 owner 仍是 `PR-01 + PR-02 + PR-07`，未把 PR-02-1 写进去 |
| 回滚边界 / 风险登记 | 🟠 缺 §9 | PR-02 有"回滚边界"，PR-02-1 没有；layout/renderer 双改有真实回滚需求 |

> 整体结论：**作为"打地基"的 PR，骨架方向是对的，并且也清楚地把 PR-03/05/06/07 的边界划出来了；但要保证"PR-02-1 合并后截图肉眼接近 demo"，必须先补 M1 等级的硬约束。**

---

## 2. 与 DEMO 的几何对照

把 `UI/UI-demo.png` (1672×941) 视为坐标真源，对每个 demo 区域估算近似像素，再和 §3.2 / §3.1 / §5 的硬约束做差分。

### 2.1 DEMO 实测 (目视估算)

| 区域 | DEMO 像素估算 | DEMO 占比 (W / H) |
| --- | --- | --- |
| navRail 宽度 | ≈ 42 px | 约 2.5% W |
| mapStage 宽度 | ≈ 1080 px | 约 64.6% W |
| rightPanel 宽度 | ≈ 510 px | 约 30.5% W |
| 顶部偏移 (mapStage.y) | ≈ 12 px | 约 1.3% H |
| bottomDeck 高度 | ≈ 215 px | 约 22.8% H |
| heroCard 宽度 | ≈ 285 px | 约 17% W |
| actionDeck 宽度 | ≈ 220 px | 约 13% W (3 card + gap) |
| commandHints 宽度 | ≈ 105 px | 约 6% W (窄列) |
| logDeck 宽度 | ≈ 470 px | 约 28% W |
| 右下 stats 摘要 | ≈ 220 px | 约 13% W |

### 2.2 §3.2 / §3.1 硬约束 vs DEMO

| 约束 (PR-02-1) | DEMO 实际 | 评价 |
| --- | --- | --- |
| `mapStage.width > rightPanel.width * 2.0` | 1080 / 510 ≈ 2.12，刚好过线 | 🟡 边界很紧；只要 rightPanel 略宽 (≥540) 就会反过来违约。建议改为 `mapStage.width >= rightPanel.width * 2.1` 或 `mapStage.width >= viewportWidth * 0.6` |
| `navRail.width < rightPanel.width * 0.35` | 42 / 510 ≈ 8.2% | 🟠 上限过松。如按 0.35 上限，navRail 可达 178 px，是 DEMO 的 4 倍，"icon-first"会变成"窄文字栏"。建议改为绝对上限 `navRail.width <= 64 px (standard)` 或比率 `< 0.12 * viewportWidth` |
| `bottomDeck.height` 容纳 hero + action + log + hint | DEMO ≈ 215 px (22.8% H) | 🟡 doc 没下限；如果 bottomDeck < 160 px，hero portrait + log wrap 都会被挤。建议 `bottomDeck.height >= 180 px @ 1280×800` |
| `rightBackpack` 必须 grid bounds | DEMO 4×3 grid | 🟡 未定义最少列/行。建议 `>=4 cols × >=2 rows`，cell 边长 >= 48 px |
| 所有 region 必须有 content bounds | OK | ✅ |

> **要点**: §3.2 给了 3 个 viewport profile (1280×800 / 1024×768 / 1672×941)，但 §3.1 的硬约束只用比率不用绝对像素，导致同样满足约束的两种实现可能在视觉上差出一个数量级。**建议把比率约束 + 每个 profile 的绝对 min/max 像素都写进合同**。

### 2.3 区域 → §3.1 region 命名一致性

| DEMO 区域 | §3.1 region | 状态 |
| --- | --- | --- |
| 左侧 icon rail | `navRail` | ✅ |
| 中央地图 + 暗黑帧 | `mapStage` (+ `outerFrame`) | ✅ |
| 顶部"地面物品" | `rightGroundLoot` | ✅ |
| 装备 (3 行 + slot 栏) | `rightEquipment` | ✅ |
| 铭刻 5–8 行 | `rightInscriptions` | ✅ |
| 4×3 背包 grid | `rightBackpack` | ✅ |
| 帮助/操作短行 | (§5.1 末尾的"帮助"分区, §3.1 `commandHints`) | 🟡 §3.1 把 commandHints 放在 bottom deck；DEMO 右侧也有一块帮助短行。文档没明确"右侧帮助"的归属，可能落到 `rightPanel` 内的另一个未命名分区 |
| 底部 hero portrait + name + gauges + stats | `heroCard` | ✅ |
| 底部 3 张技能卡 | `actionDeck` | ✅ |
| 底部中段长 log | `logDeck` | ✅ |
| 底部右段 stats 摘要 (`层 1/2 生命 152/152` 等) | **未命名** | 🟠 DEMO 明显有这一块；§3.1 表里没有 `bottomStatsSummary`。InfoSurfaceLayout.kt 现有 `focusBounds` 大概对应这个块，但文档没把它当 demo region 写出来 |

**修复建议**: §3.1 增加一行 `bottomStatsSummary` 或在 `heroCard`/`logDeck` 的 Content rule 中明确"层/HP/MP/ATK/DEF 的紧凑摘要"归属。

---

## 3. 按章节深度审查

### 3.1 §0 Acceptance Matrix / Gate Budget / Failure Rule

✅ 强项：
- M01–M08 覆盖 doc / shell layout / renderer / right panel / optional sheet / standalone / demo parity / governance；与 PR-02 模板风格一致。
- Failure Rule 明确 "no overlap" 不足以 pass，必须截图视觉接近 DEMO — 这是把 demo parity 升格为 blocking 的关键设计，值得保留。
- "PR-02-1 不能把 PR-07 当兜底" 写进禁止事项，杜绝把 final audit 当垃圾桶的反模式。

🟡 缺口：

1. **Failure Rule 没有判定主体**。"截图仍是旧 text-first shell 必须 fail" 由谁判定？是 PR owner、reviewer、还是 manual record 中的 manualReviewer 字段？建议在 §0 补一段：
   > "Demo parity 视觉判定由 PR owner 与至少一位非 PR owner reviewer 联合签字。判定记录写入 `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` 的 `manualReviewer / reviewedAt / demoParityVerdict` 字段；任一方判 fail 即触发 §3/§4 回修。"

2. **Gate Budget 缺时长基线**。PR-02 在 freshness 要求里挂了 `verify-changed/full-task-duration-summary` 读取规则；PR-02-1 沿用了，但没给"本 PR 预计 verifyChanged 耗时区间"。如果同时改 layout + renderer + 可选新 sheet，full-task gate 时长大概率超过 PR-02 基线，建议加一条"如本轮 verifyChanged 超过 PR-02 基线 1.5×，先做 Doc-vs-Implementation Self-Audit 再继续重跑"。

3. **freshness 第 5 条**："同一 golden / packaged app 白盒连续失败超过 2 次，先补 focused layout/rendering 断言再重跑" — ✅ 这是好规则，但建议把它显式绑到 `DemoShellLayoutTest` 与 `TileRendererCanvasTest` 的具体新增断言条目，否则容易被解读成"挂个空断言"。

### 3.2 §1 阶段目标 / §2 影响范围

✅ 强项：
- 把"icon rail / map stage / right panel / bottom deck"作为四象限来描述，定位准确。
- §2 表格列了"哪些文件 Added/Modified"，path 完全 repo-relative，可被 `acceptanceContractLint` 识别。
- "Deleted / replaced 清单"四行明确点名了四种"必须从旧形态消失"的反模式（左栏大段文字、右栏纯文字列表、裸 hint footer、map 被 chrome 压低），可被 reviewer 用来快速对照截图。

🟠 缺口：

1. **TileRenderer.kt 当前 1599 行；TileRenderModel.kt 2158 行。** PR-02-1 §2 写"Modified TileRenderer.kt 按 demo shell draw order 重排"，但没有限制 LOC 增长上界。这种规模的文件再叠"按 demo 重排"会进一步压低可读性。建议补一条：
   > "PR-02-1 收口时 `TileRenderer.kt` 净增长不得超过 +400 行；超过则必须按 chrome / map / nav / right / bottom 拆出至少一个 internal renderer 组件 (例如 `DemoShellChromeRenderer`)。"

2. §2 列出新增 `DemoShellLayout.kt 或等价文件`，"或等价文件"会被实现者解读为"放进 GameShellLayout.kt 也行"。这与 §3 "PR-02-1 必须新增或收敛一个 typed shell layout" 语义有冲突。建议二选一：要么强制独立文件，要么允许内嵌但限定函数边界。

3. **`FoundationGameScreen.kt` 只写"使用 demo shell viewport/layout，保持输入和 session 生命周期不变"**。但 demo 在 modal 打开 (inventory / talent slot choice) 时的"map stage 是否被遮挡"没有合同。建议在 §2 表里补 "FoundationGameScreen modal anchoring 使用 modalSafeBounds，不在地图中央打开时遮挡 nav rail"。

### 3.3 §2.1 实施执行顺序

✅ 强项：10 步顺序合理 (preflight → demo decomposition → layout tests → render model → renderer → resource decision → resource generation → golden → packaged whitebox → audit)。"layout tests second" / "render model third" / "renderer fourth" 这一段强制了 TDD-ish 的实施纪律。

🟡 缺口：

1. 步骤 6 "resource decision fifth" 写"只有当 PR-02 资源不足才新增 r01b"，但没有定义"不足"的可机器判定门槛。提议改为：
   > "若 §3.1 任一 region 在 §6.2 fallback 链下绘制结果与 demo 视觉差异 > 阈值 (例如 hero card crest 完全空白) 或截图被判 fail，必须新增对应 r01b cell；否则保持 r01b 未生成状态，并在 manual record 中写 `r01b: NOT_USED, reason: <…>`。"

2. 步骤 10 "doc-vs-implementation audit" 让 PR owner self-audit — ✅，但缺一条"checkpoint 时机"。建议把这步明确为"PR ready-for-review 前 + verifyChanged 通过后"两次。

### 3.4 §3 Demo Shell Layout Contract

✅ 强项：
- 14 个 typed region 命名清晰，职责单一 ("layout 只计算区域，不读取 gameplay rule")。
- 3 个 viewport profile + 5 条硬约束，方向正确。

🟠 / 🔴 缺口 (按严重度排序)：

1. **🟠 数值约束过松** (见 §2.2 表格)。**强烈建议**改写为：
   ```
   | constraint | rule |
   | mapStage / viewport width | >= 0.58 (standard), >= 0.55 (minimum), >= 0.62 (demo-aspect) |
   | navRail / viewport width | <= 0.06 (standard), <= 0.08 (minimum) |
   | rightPanel / viewport width | 0.25–0.32 (standard) |
   | bottomDeck / viewport height | >= 0.22 (standard) |
   | heroCard 最小宽 | >= 240 px @ 1280, >= 200 px @ 1024 |
   | rightBackpack grid | >= 4 cols × >= 2 rows, cell side >= 48 px |
   ```
   这些数值都能直接落入 `DemoShellLayoutTest`，把"看起来像 demo"从感性判定转为机器判定。

2. **🟠 missing `bottomStatsSummary` region** (见 §2.3)。DEMO 底部明显有"层/HP/MP/ATK/DEF"的右侧紧凑摘要，§3.1 没列。建议补行：
   ```
   | `bottomStatsSummary` | 底部右侧紧凑数据摘要 | 层 / HP / MP / ATK / DEF | 与 logDeck 同高，不重叠 actionDeck/commandHints |
   ```

3. **🟡 `modalSafeBounds` 没说锚点策略**。现行 `GameShellLayout.modalSafeBounds` 是 left/right/top/bottom 绝对坐标。当 inventory modal / talent slot choice modal 打开时，应在 mapStage 中央还是 modalSafeBounds 中央？建议补 "modal 默认锚到 mapStage 中央，但不得越过 modalSafeBounds"。

4. **🟡 §3.1 `outerFrame` 与 §4.1 draw order step 2 (`outerFrame / shell background`) 的边界**。如果 outerFrame "包住地图、右栏、底部 deck"，那么 navRail 在 outerFrame 之外还是之内？draw order step 2 在 step 9 (nav rail chrome) 之前，意味着 navRail 不被 outerFrame 视觉包裹。建议在 §3.1 outerFrame 行补 "包含 mapStage / rightPanel / bottomDeck / commandHints；navRail 是 outerFrame 的兄弟而非子节点"。

### 3.5 §4 Renderer / Draw Order Contract

✅ 强项：
- 14 步 draw order 完整，包含 fog/light 预留位 (step 8)，为 PR-05 lighting/fog 留好接口。
- §4.2 把 `drawRect` 限制在 scrim / bar / debug / fallback outline，把 `font.draw` 替换为 `TileTextMetrics` — 直接堵住"裸坐标 + 裸字体"的反退化路径。

🟡 缺口：

1. **§4.1 "PR-02-1 不要求实现复杂动态光照，但必须预留 mapStage 内的 fog/light 层"** — "预留"如何被测试？建议在 `TileRendererCanvasTest` 补一个 layer index 断言："recording canvas 在 draw order step 8 位置接收一个空 group / no-op，PR-05 可在此处插入 fog/light 渲染。"

2. **§4.2.6 "goldenScreenshot 只能作为视觉回归，不替代 bounds / owner key focused tests"** — ✅ 正确。但 PR-02-1 同时在 §0 把 demo parity 提到 blocking，意味着 golden hash 漂移变得很常见。建议补一条："demo parity 重排 → golden hash 漂移 → 在 manual record `goldenLabels` 字段写明 `hashDriftReason: dark-uiux-pr02-1 demo shell foundation` 并取得 reviewer 签字。"

3. **§4.1 step 11 `bottom hero card + action deck + command hints + log deck` 是同一步**。这把四个独立 surface 压在同一渲染原子里。建议拆为 11a (hero card) / 11b (action deck) / 11c (command hints) / 11d (log deck)，便于 focused test 用 recording canvas 分别断言。

### 3.6 §5 Right Panel / Bottom Deck / Nav Rail Scaffold

#### §5.1 Right Panel Scaffold

✅ 强项：5 个分区 (地面物品 / 装备 / 铭刻栏 / 背包 / 帮助) 顺序与 DEMO 一致。

🟠 缺口：

1. **slot 尺寸 / 列数 / hitbox 没契约**。§5.1 只说"4×N 或 responsive grid"。
   - DEMO 装备区是 "3 行 text label + 右侧 4-slot column grid"，doc 没要求 "slot column 必须存在"。
   - DEMO 背包是 4×3 grid，doc 只说 ">= 4 cols"。
   - DEMO 地面物品是 3 个 compact slot，doc 没限制最小 hitbox。
   - 这一段必须 pin 死，否则 PR-03 接 item icon 时仍可能推翻 grid 几何。
   - **建议**：
     ```
     | section | min slots | slot side @1280 | slot side @1024 |
     | rightGroundLoot | 3 | 48 px | 40 px |
     | rightEquipment | weapon/offhand/armor 3 行 + slot column 4 格 | 48 px | 40 px |
     | rightInscriptions | 4 rows × 1 col (5-8 当前绑定) | 48 px | 40 px |
     | rightBackpack | 4 cols × 2 rows minimum | 48 px | 40 px |
     ```

2. **"compact slots" 和 "section divider"**。§5.1 末尾的"帮助"分区与底部 `commandHints` 是不是同一东西？建议要么删除 §5.1 "帮助"分区 (让它只存在 bottom deck)，要么明确"右侧帮助 = 简短键位提示，bottom commandHints = 完整命令列表"。

#### §5.2 Bottom Deck Scaffold

✅ 强项：4 分区 (hero card / action deck / command hints / log deck) 顺序与 DEMO 一致；"禁止把 HP bar 绘在 text baseline 上" 是直接对应 InfoSurfaceLayout.kt 历史问题的对症条款。

🟡 缺口：

1. **hero portrait/crest 尺寸下限缺失**。DEMO 中 portrait 约 110×110 px。建议 "heroCard portrait/crest placeholder >= 96×96 px @ 1280×800；不得只是一个 32 px 的 icon"。
2. **action deck card 数与 game 规则的绑定**。§5.2 说 "1–4 active talents / actions"，但 `PLAYER_ACTIVE_TALENT_SLOT_COUNT` 当前是常量。如果代码改动这个常量，PR-02-1 layout 是否会自动适配？建议加一条 "actionDeck.cardCount = PLAYER_ACTIVE_TALENT_SLOT_COUNT，但布局在 cardCount ∈ [1, 4] 时必须保持 bottom deck 的水平比例不变；超出区间 fail layout test"。
3. **logDeck 与 commandHints 分区**。DEMO 中两者并列；§5.2 顺序是 hero → action → commandHints → log，与 DEMO 一致。但 commandHints 写在 action deck 右侧 (DEMO 视觉) 还是介于 action 与 log 之间 (doc 字序)？建议补"commandHints 在 actionDeck 右侧 / logDeck 左侧"明文。

#### §5.3 Left Navigation Rail Scaffold

🔴 **最大风险**：
- DEMO 左侧 nav rail 5 个 icon：compass / bag / scroll / book / gear。
- §5.3 写"可以先复用 PR-02 control/HUD icons；若复用语义不清，新增 PR-02-1 shell icon key"。
- §6.2 r01b-ui-shell-chrome direct cells 只有 `ui.shell.nav_button.active`（**选中态背板，不是图标本身**），没有 compass / scroll / book / gear icon key。
- PR-02 提供：`ui.control.backpack.icon` / `ui.control.equipment.icon` / `ui.hud.log_marker.icon` / `ui.hud.quest_marker.icon` / `ui.hud.warning.icon` — 可以勉强凑出 bag / equipment / log，但 compass、book、gear 没有对应资源。
- 结果：要么 nav rail 出现 3 个空 placeholder + 2 个语义错位 icon，要么必须把 nav icons 提前到 PR-02-1 但 §6.2 又不允许在 r01b 加 control icon。

**强烈建议**改写 §5.3 / §6.2：
- 方案 A：在 r01b-ui-shell-chrome 内扩 5 个 cell 给 nav icons (compass / bag / scroll / book / gear)，类别从 `ui_frame` 改为 `icon`，targetKey 例如 `ui.shell.nav.compass`、`ui.shell.nav.bag`、`ui.shell.nav.scroll`、`ui.shell.nav.book`、`ui.shell.nav.gear`，fallback 到 PR-02 `ui.control.*`。
- 方案 B：明确接受"nav rail 在 PR-02-1 仅有 selected frame + 占位符号 (例如 ●○■▲★)，真实 icon 由 PR-06 接入"，并在 demo-delta checklist 中把 "left icon rail icon quality" 显式归到 PR-06。

不修这一段，§0 Failure Rule "screenshot 仍是旧 shell 必须 fail" 几乎可以预测会卡在 nav rail。

### 3.7 §6 Optional Round 1B Shell Chrome Contract

✅ 强项：
- Owner Scope Rule 写得很严：`ownerExpectedKeys` 必须与 owner contract `requiredCells[].targetKey` 完全一致，"PR-02 keys 不能改成 PR-02-1 owner" 杜绝跨 PR 偷搬。
- Grid Occupancy Contract: 10 direct + 0 alias + 6 reserved = 16 slots，与 4×4 sheet 闭合。
- "If not used, remains a not-used option in manual record" 给了 opt-out 路径。

🟡 缺口：

1. fallback 链统一指向 `ui.frame.panel.body` (5 个 entry) / `ui.frame.slot.selected` / `ui.frame.tooltip.body` / `ui.control.equipment.icon`。若 r01b 未生成，DEMO 中 navRail / mapStage frame / heroCard / actionDeck / rightSectionDivider 全部退化为 `ui.frame.panel.body` 同一张图 — 视觉单调到几乎肯定触发 Failure Rule。文档没有提示这种 fallback-only 后果，建议加一条 "r01b 未生成时，PR owner 需在 manual record 中明确说明 fallback 后视觉差异，并由 reviewer 评估是否触发 Failure Rule"。
2. §6.2 行 0/3 `ui.shell.nav_button.active` fallback 到 `ui.frame.slot.selected` — ✅ 语义对齐。但仍未解决 nav icon 本体资源问题 (见 §5.3 缺口 3.6)。

### 3.8 §7 Standalone / Main Menu Alignment

🟠 **契约过虚**：4 条 (no debug look / footer 在 chrome slot / validation 与右侧 panel 同 density / Victory/GameOver/UiError 不 regression)，都是"不要回退"风格而非"要达成什么"。

建议补一条像素契约：
- "Main menu 标题区高度 >= 96 px @ 1280×800"
- "Validation setup 列表卡片间距 >= 8 px，每卡片 height >= 48 px"
- "Victory/GameOver outcome 卡片 width >= 480 px"

否则 `dark-uiux-pr02-1-demo-main-menu` golden 在视觉上和当前主菜单几乎没区别也能 pass。

### 3.9 §8 Demo Parity Manual Record

✅ 强项：4 个 required screenshot label + 13 行 demo-delta checklist + 5 个 blocking finding example，非常完整。

🟡 缺口：

1. **demo-delta checklist 缺"机器判定 vs 人工判定"标记**。"left icon rail must be structurally present" — 通过 `DemoShellLayoutTest.navRailBoundsExist()` 可机判；但 "command hints must be inside a shell plate, not naked footer text" 是人判。建议补一列 `judgeMode: machine / manual`，把人判项目集中。
2. **3 个"allowed remaining gap"项目**："map tile/actor/fog art quality" / "item/icon art quality" / "final all-screen polish" — ✅，但没明确"如果这些 gap 也已经在 PR-02-1 内顺手做了一部分，PR 描述如何登记"。建议补 "any gap reduction outside this PR scope must be a separate commit and noted in PR description's `outOfScopeReductions` section"。

### 3.10 §9 验收标准 / §10 验证 / §11 非目标 / §12 PR 描述要求

✅ 强项：
- §9 10 条均为"已对应到具体 test / artifact"，无虚条。
- §10 commands 完整，先 layout/renderer focused → optional resource → client evidence → governance → packaged，顺序与 §2.1 一致。
- §11 非目标 7 条覆盖了 stat/loot/shop/combat/AI/save/replay/profile/atlas/region manifest — 防止 scope creep。
- §12 PR 描述必须列出 demo-delta 实际状态，禁"整体接近 demo"空话 — ✅ 力度足。

🟠 缺口：

1. **§9 缺一条"DemoShellLayoutTest 必须包含数值约束"**。如果 §3 的硬约束按本审查建议补足数值，§9 应增加：
   > "11. `DemoShellLayoutTest` 必须断言 §3.1 / §3.2 表中所有数值约束 (mapStage 占比 / navRail 占比 / rightPanel 占比 / bottomDeck 占比 / hero portrait 最小尺寸 / 背包 grid 最小列行 / slot side 最小像素)。"

2. **§10 "Optional resource gate" 命令块缺少 `-Pktome.darkUiux.requireFullGrid` 应用对象**。PR-02 在 §7 命令里 `darkSpriteSheetLint` 同时校验 3 张 sheet 的 grid；PR-02-1 只有 1 张可选 sheet，建议明确 `-Pktome.darkUiux.requireFullGrid=true` 在 r01b 启用时同样适用。
3. **§10 没有"failed run 复盘"提示**。建议补 "若 `:client:goldenScreenshot` 连续失败 ≥2 次，先回到 `DemoShellLayoutTest` 或 `TileRendererCanvasTest` 增加缺失断言，再重跑 golden；不得把 golden 当布局调试循环 (governance §0 freshness 第 5 条)"。

### 3.11 跨文档同步

🔴 **`UI/pr/screen-coverage-matrix.md` 没更新**：
- "局内主 shell" 行 Owner PR 仍是 `PR-01 + PR-02 + PR-07`。
- PR-02-1 在合并前必须在矩阵中插一行 (或修改原行) 把 owner 改为 `PR-01 + PR-02 + PR-02-1 + PR-07`，并把 PR-02-1 的 golden label (`dark-uiux-pr02-1-demo-shell-1280x800` 等 4 个) 加入 §3 表。
- §3 中 `dark-uiux-pr02-1-demo-*` 4 个 label 应该作为 Required Inventory 单独列出，与 `dark-uiux-pr01-shell-1280x800` 并列；否则 PR-07 final-all-screens evidence index 会漏。

🟠 **`UI/PLAN.md` 未列入 canonical artifact update**：
- PR-02-1 §0 canonical artifact 没把 `UI/PLAN.md` 写进去；§2.1 preflight 只让"读"。
- 既然 PR-02-1 在 PR-02 与 PR-03 之间插入一个新 phase，`UI/PLAN.md` 的 phase 时间线必然要新增一节。建议把 PLAN.md 加入 canonical artifact 并约束 "PLAN.md 必须包含 dark-uiux-pr02-1 phase 描述"。

🟡 **`UI/pr/README.md` 未列为修改对象**：
- README.md 通常是 PR 索引；PR-02-1 作为新 PR 必须出现在 README 中。建议把 `UI/pr/README.md` 加到 §2 表 Modified。

### 3.12 回滚边界缺失

PR-02 §9 "回滚边界" 明确"可以通过回滚 dark-v1/ui runtime PNG / sheet-plan / canonical manifest / runtime manifest / renderer 消费点完整回退"。PR-02-1 没有对应章节。

**风险**: 如果 PR-02-1 合并后发现 shell foundation 有严重 regression，应:
1. 回滚 `TileRenderer.kt` / `TileRenderModel.kt` / `GameShellLayout.kt` / `InfoSurfaceLayout.kt` / `FoundationGameScreen.kt` / `MainMenuScreen.kt` 改动？
2. 同时回滚 r01b-ui-shell-chrome (若已生成)？
3. 还是保留 r01b 但 disable demo shell 路径，回到旧 text-first shell 作为 fallback flag？

建议补一节 §9.5 或 §13：
> "**回滚边界**：本 PR 可通过单 commit 完整回退；回退后 `GameShellLayout` / `InfoSurfaceLayout` 恢复 PR-02 末态，TileRenderer 恢复旧 draw order。若 r01b-ui-shell-chrome 已合并，回滚同时移除 r01b raw sheet / sliced PNG / canonical+runtime manifest entry / key registry entries / owner contract / coverage artifact，但不影响 PR-02 三张 sheet。"

---

## 4. 实现可达性评估

基于现状 (`TileRenderer.kt` 1599 LOC、`TileRenderModel.kt` 2158 LOC、`InfoSurfaceLayout.kt` 202 LOC、`GameShellLayout.kt` 83 LOC) 与文档要求：

| 模块 | 现状 | PR-02-1 目标 | 难度 |
| --- | --- | --- | --- |
| `GameShellLayout` | 4 region (leftRail / map / rightPanel / bottomHud) + modal safe | 14 typed region (按 §3.1) | **中** — 加 region 容易，关键是"hard constraints 加进 init 块"和"DemoShellLayoutTest 覆盖 3 个 profile" |
| `InfoSurfaceLayout` | mapDominant 已实现，wideSplit/modalOverlay 占位 | 沿用 mapDominant，补 §3.2 三个 profile 的 metrics | **中** — 现有 `mapDominantMetrics` 已经做了大量精算，扩展即可 |
| `TileRenderer` | 1599 LOC，全部坐标在文件内 | 按 §4.1 draw order 重排 + 拆 internal renderer | **高** — 风险点是越改越大，必须有 LOC 上限 |
| `TileRenderModel` | 2158 LOC，已是大型 presentation snapshot | 加 nav rail / right zones / grid placeholder / hero summary | **中高** — 模型扩展不难，但更难维护 |
| `FoundationGameScreen` | 现有 | 切换到 demo shell viewport | **低** — 只换 layout 入口 |
| `MainMenuScreen` | 现有 | density 对齐 | **中** — 因 §7 契约虚，标准模糊 |
| `DemoShellLayoutTest` | 不存在 | 新建，覆盖 3 profile + 数值约束 | **中** — 取决于 §3 是否补足数值 |
| `TileRendererCanvasTest` | 现有 | 加 nav rail / right zones / grid / hero / action / log 断言 | **中** — recording canvas 已就绪，加 case 即可 |
| `r01b-ui-shell-chrome` (可选) | 不存在 | 新建 4×4 sheet (10 direct + 6 reserved) | **中** — 流程同 PR-02，已熟练 |
| Nav rail icon resources | 缺 | 见 §3.6 风险 | **高** — 必须先解决 §3.6 |

**整体可达性**: 在补完本审查 §6 提出的 M1 修订后，**XL 工作量、单 sprint 内可达**。如果不补 M1，nav rail 与 right panel slot 可能落不出 demo 感，需要二次 PR。

---

## 5. 必修条款清单

### 5.1 M1 (必修，否则 PR-02-1 难以判 pass)

| ID | 章节 | 必修内容 |
| --- | --- | --- |
| M1-01 | §3.1 / §3.2 | 把 §2.2 表中的数值约束 (mapStage / navRail / rightPanel / bottomDeck 占比，hero portrait 最小尺寸，背包 grid 最小列行，slot side 最小像素) 写进 hard constraints，并在 `DemoShellLayoutTest` 中断言 |
| M1-02 | §3.1 | 新增 `bottomStatsSummary` region 或在 `heroCard`/`logDeck` 中明文归属 DEMO 右下 stats 摘要 |
| M1-03 | §5.3 / §6.2 | 解决 nav rail icon 资源闭环：要么在 r01b 增加 compass/bag/scroll/book/gear 5 cell，要么在 §8 demo-delta checklist 中把 "nav icon quality" 显式归到 PR-06 |
| M1-04 | §5.1 | 补 slot 几何契约表 (right ground loot / equipment / inscriptions / backpack 的 min slot count、cell side @1280、cell side @1024) |
| M1-05 | §0 Failure Rule | 明确 demo parity 视觉判定主体 (PR owner + ≥1 非 owner reviewer)，并把 verdict 字段写入 manual record |
| M1-06 | `UI/pr/screen-coverage-matrix.md` | 更新"局内主 shell"行 Owner PR 包含 PR-02-1；§3 Required Inventory 加入 4 个 `dark-uiux-pr02-1-*` label |
| M1-07 | §2 影响范围 | 限定 `TileRenderer.kt` 净 LOC 增长 (例如 ≤ +400)；超过则强制拆出 internal renderer |

### 5.2 M2 (建议修，但不阻断)

| ID | 章节 | 建议内容 |
| --- | --- | --- |
| M2-01 | §0 / §10 | 给 `verifyChanged` 时长基线区间；超过 1.5× 时触发 Doc-vs-Implementation Self-Audit |
| M2-02 | §3.1 | 明确 outerFrame 与 navRail 的兄弟/子节点关系 |
| M2-03 | §3.1 | 明确 modal 锚点策略 (默认 mapStage 中央，受 modalSafeBounds 限制) |
| M2-04 | §4.1 step 11 | 拆为 11a/11b/11c/11d 便于 recording canvas 分别断言 |
| M2-05 | §5.2 | 给 hero portrait/crest placeholder 最小尺寸 (≥ 96×96 @ 1280×800) |
| M2-06 | §5.2 | actionDeck.cardCount = `PLAYER_ACTIVE_TALENT_SLOT_COUNT`，在 [1, 4] 区间内布局比例不变 |
| M2-07 | §7 | 给 main menu / validation setup / outcome 至少各加 1 条像素契约 |
| M2-08 | §8.2 | demo-delta checklist 加 `judgeMode: machine / manual` 列 |
| M2-09 | §13 (新增) | 加 "回滚边界" 章节 (见 §3.12) |
| M2-10 | §0 canonical artifact / §2 | 把 `UI/PLAN.md` 加入 canonical artifact，把 `UI/pr/README.md` 加入 §2 Modified |
| M2-11 | §10 | 加"golden 连续失败 ≥2 次必须回到 layout/renderer 焦点测试"的命令注脚 |

### 5.3 M3 (可选优化)

| ID | 章节 | 建议内容 |
| --- | --- | --- |
| M3-01 | §6.2 | r01b 6 个 reserved cell 给出未来用途 hint，避免 PR-02-2/PR-02-3 重复占用 |
| M3-02 | §9 | 增加第 11 条 "DemoShellLayoutTest 数值约束断言" 闭合 M1-01 |
| M3-03 | §10 | 把 standalone screen layout/text test 列入 focused commands |

---

## 6. 答用户原问题：本 PR 完成后能否实现 DEMO 框架级要求

**答**：**部分可以，按 M1 修订后才能稳定达成"框架级"目标**。

### 6.1 已能达成的框架要素 (无需修订)

- ✅ 四象限布局 (icon rail / map stage / right panel / bottom deck) 类型化区域定义。
- ✅ 14 步 draw order 把 chrome / map / nav / right / bottom / modal / overlay 分层。
- ✅ inventory/modal 不再压在裸 footer 文字上。
- ✅ HUD gauge 不再压在 text baseline 上。
- ✅ Right panel 从纯文字列表升级为 5 分区 scaffold (即使 PR-03 未到，slot/grid 形已经存在)。
- ✅ Action deck 卡片化，commandHints/logDeck 分离。
- ✅ Optional sheet r01b 的 owner-scope 闭环纪律。
- ✅ Failure Rule 把"视觉仍像旧 text-first shell"列为 blocking。

### 6.2 不修 M1 就难以达成的框架要素

- ❌ Nav rail icon-first 视觉 — 因为 §6.2 没给 compass/bag/scroll/book/gear key，PR-02 现有 control icon 仅覆盖 2/5 语义。
- ❌ DEMO aspect (1672×941) 下的"比例对得上"视觉 — §3.2 viewport profile 没绑数值约束，可能出现 navRail 占 12% 宽、mapStage 仅 50% 宽这种"形过约束但看不像 demo"。
- ❌ 右侧 slot grid 的视觉密度 — §5.1 没 pin slot 几何，PR-03 接 item icon 时仍可能因 grid 太小被推翻。
- ❌ "demo parity 视觉判定"的可追溯性 — §0 Failure Rule 没指明判定主体。
- ❌ screen-coverage-matrix 中 PR-02-1 的可见性 — 不更新矩阵则 PR-07 final audit 无法引用 PR-02-1 证据。

### 6.3 即使按 M1 修订，仍需后续 PR 兜底的要素 (不阻断 PR-02-1，但要在 manual record 写清)

- 🟡 map tile / actor / fog / light 美术品质 → PR-05。
- 🟡 final item / equipment / shop icon 美术品质 → PR-03。
- 🟡 final skill / status / quest icon → PR-06。
- 🟡 packaged app 全屏 polish 与 outcome / loading / error 截图集 → PR-07。

### 6.4 综合判断

> 如果本 PR 按当前文档落地，**结构上 70–80% 接近 DEMO**，但因 nav rail icon 缺资源、右侧 slot 几何缺契约、demo aspect 缺数值锚，**视觉上很可能在 §0 Failure Rule 这一关被判 fail，需要二次提交补救**。
>
> 按 M1 7 条修订后再开发，**结构上 95% 接近 DEMO，视觉上 75–85% 接近 DEMO**（剩余 15–25% 由 PR-03/05/06 美术资源在后续阶段填补），**满足"为后续 UI 改造打地基"的目标**。
>
> 因此推荐：**先把 M1 修进文档，再正式启动开发；M2 在开发过程中并行补齐；M3 留作 PR 收口前 polish**。

---

## 7. 推荐下一步动作

1. **回到 PR-02-1 doc，按 M1 七条做一次定稿修订**，特别是 §3.1 / §3.2 数值约束表与 §5.3 / §6.2 nav rail 资源闭环。
2. **同步更新** `UI/pr/screen-coverage-matrix.md`、`UI/pr/README.md`、可能的 `UI/PLAN.md`。
3. **开发前**：先写 `DemoShellLayoutTest` 三个 profile 的 hard constraint 断言 (M1-01)，确保数值表能被机器执行。
4. **开发中**：若选择新增 r01b-ui-shell-chrome，先在 §6.2 表里把 nav icon 5 cell 补进 (M1-03 方案 A) 再生 prompt。
5. **开发末**：填 `UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md` 时严格按 demo-delta checklist 写 verdict；reviewer 给出独立的 demo parity 判定。
6. **合并后**：在 PR-07 evidence index 中把 4 个 `dark-uiux-pr02-1-*` golden label 纳入 final audit。

---

## 8. 附：本审查涉及的真源路径

| 类别 | 路径 |
| --- | --- |
| 审查对象 | `UI/pr/dark-uiux-pr02-1-demo-shell-foundation.md` |
| 上游 | `UI/pr/dark-uiux-pr02-ui-chrome-sprite-pilot.md` |
| 治理 | `UI/pr/development-governance.md`、`docs/review/rule/pr-level-review-standard.md` (referenced) |
| 矩阵 | `UI/pr/screen-coverage-matrix.md` |
| Demo 真源 | `UI/UI-demo.png` (1672×941) |
| 当前 layout | `client/src/main/kotlin/com/ktome/client/render/layout/GameShellLayout.kt`、`InfoSurfaceLayout.kt` |
| 当前 renderer | `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`、`TileRenderModel.kt` |
| PR-02 资源 | `UI/sprite-sheets/sheet-plan.yaml`、`UI/sprite-sheets/key-registry.yaml`、`r01-ui-chrome / r01-ui-controls / r01-ui-hud-icons` |
| 本审查输出 | `UI/review/2026-05-11-dark-uiux-pr02-1-deep-review.md` |

---

**审查结束。建议把本文件 + M1/M2 修订项一并带回 PR-02-1 doc 作者，定稿后再启动开发。**

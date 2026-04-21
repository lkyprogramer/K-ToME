# 业界经典 Roguelike / 相关产品的设计哲学与对 K-ToME 的启示

## 0 · 文档定位

这份文档和 [`2026-04-20-client-ui-ux-reference-driven-optimization-design-review.md`](./2026-04-20-client-ui-ux-reference-driven-optimization-design-review.md) 互为姊妹：

1. 后者是对 [`docs/opt`](../opt/2026-04-20-client-ui-ux-reference-driven-optimization-design.md) 单一设计文档的批注 review。
2. 本文是**横向调研**——从业界挑出对 K-ToME 有最高借鉴价值的 10 个核心参照和 6 个次级参照，提炼 6 大设计哲学流派，最后给出 K-ToME 应选择的主线哲学与取舍清单。

本文不负责：

1. 不复述已有 review 结论。
2. 不给像素级规范（走 review §5 / Design Token）。
3. 不把任何单一产品的美术风格当作 K-ToME 应该克隆的目标。

---

## 1 · 核心参照（深度展开）

按"对 K-ToME 借鉴价值"排序，每个条目给：一句话定位 → 关键设计手法（3-4 条） → 对 K-ToME 的具体建议（2-3 条）。

### 1.1 Tales of Maj'Eyal（ToME4） — 第一参照，也是词源

**定位**：开源 Java/Lua 桌面 roguelike，K-ToME 的精神前身。机制透明派的样板。

**关键设计手法**：

1. **所有数值、公式、冷却对玩家可见**。每个天赋 tooltip 写清"当前层数 → 下一层数"差值、命中公式、与哪些属性挂钩、免疫列表。
2. **Talent Tree 作为玩家身份锚点**。每个职业 4-6 棵主 tree + 通用 tree，UI 把 tree 可视化成图谱，点数分配可预览。
3. **关键词高亮**。tooltip 里 `burning` `bleed` `stun` 等词自动高亮颜色，悬停展开子 tooltip 解释机制。
4. **死亡前预警**。HP<50% 屏幕边缘变红 vignette；<25% 强化；剩余 1 回合必死时直接弹出对话框"你可能会死，确认继续？"。

**对 K-ToME 的建议**：

1. 天赋 / 技能 / 词缀 tooltip 的**关键词高亮 + 子 tooltip**骨架直接照搬，locale 压力下这套最成熟。
2. 天赋 tree 的可视化图谱作为 `P3-W3` 动态说明的主界面——当前 "5-8 使用铭刻" 键位表完全不够承载一棵天赋树。
3. 死亡预警分级 vignette 作为可达性 + 紧张感的一石二鸟。

### 1.2 Caves of Qud（CoQ） — 信息密度 + Look Mode

**定位**：商业化的硬核 roguelike，把"文本 + tile + 部位系统"做到极致。

**关键设计手法**：

1. **Look Mode（`l` 键）作为一等公民**。按 `l` 进入纯检视模式，光标在地图上任意移动，右栏实时更新该位置的地形/怪物/物品描述。不按 `l` 就不检视，主模式不被 tooltip 噪声污染。
2. **装备按身体部位分槽**。头/颈/躯干/双手/指（多个环）/脚——每个部位可独立装备，突变种族甚至会多出"第三只眼""尾巴"等特殊槽。
3. **历史与地理叠加**。每个地点有"随机生成的历史传说"文本，点开查看即世界观。
4. **右栏极致信息堆叠**：HP / 属性 / 部位装备 / 状态 / 重要事件通知全在一列，靠颜色块 + 分隔符分组，一屏可扫读 30+ 条信息。

**对 K-ToME 的建议**：

1. **把"检视"从 UiMode 上升为独立模式（`x` 键已存在）**并扩展为可在地图上自由移动光标的 Look Mode，比当前"按 x 检视当前格"更强大。
2. 装备部位系统参考 CoQ 而非一般 RPG（武器/副手/护甲三大项太单调）——头/臂/躯/腿部位 HP 已经是 K-ToME 的优势（参见 review §3.5），装备也应该和部位绑定。
3. 右栏靠**颜色块 + 分隔符**分组而非 tab 切页：`StatusHudRenderer` 当前只有 1.3K 远不够，应扩到一个能承载 CoQ 级别密度的 subcomponent。

### 1.3 Cogmind — 颜色即语义 + 单字符最大化

**定位**：单人开发 10 年的 ASCII 机器人 roguelike，纯字符能表达的信息密度上限。

**关键设计手法**：

1. **颜色作为一等语义**。每种武器/装备/状态有固定颜色，玩家通过颜色而非文字识别；重要度通过亮度 + 加粗分级。
2. **武器槽独立热量与冷却**。每个武器槽是独立子系统，每回合独立结算热量、过载、自毁风险——UI 直接显示每个槽的热条。
3. **整合度（Integrity）分部位**。机器人的每个子系统（推进/视觉/武器/电源）独立 HP，被击毁会改变操作——直接映射到 K-ToME 的肢体系统。
4. **粒子级 feedback**。射击、爆炸、掉落用字符粒子动画 + 屏幕震动，**纯字符也能做手感**。
5. **日志 filter 层**。可开关"显示战斗细节 / 仅显示致命 / 显示拾取"等 6-7 档筛选。

**对 K-ToME 的建议**：

1. **"颜色即语义"必须写进 Design Token**：不允许"红色有时表示伤害、有时表示危险、有时表示愤怒状态"——每个颜色预留一个语义槽。
2. Cogmind 的整合度分部位是 K-ToME 肢体/部位系统的直接模板，连"被击毁后 debuff 具体是什么"都可以参考它的设计。
3. **日志 filter 层**（review §5.7 第 4 条）的成熟实现。K-ToME 的日志如果不做 filter，战斗爆量时完全不可读。

### 1.4 Slay the Spire（StS） — Intent Telegraph 集大成

**定位**：现代 roguelike deckbuilder 的标杆，把"完美信息 + 概率透明"做到极致。

**关键设计手法**：

1. **敌人 Intent 图标**。每个敌人头顶有下一回合意图图标：⚔️攻击（旁边带伤害数字）、🛡️防御（数字）、✨增益、☠️debuff。玩家**永远知道**敌人下一招做什么，决策不是猜而是算。
2. **牌的 tooltip 完整透明**。每张牌写清伤害、费用、关键词（Exhaust / Ethereal / Retain），关键词悬停有二级 tooltip。
3. **遗物 tooltip 完整展开**。遗物的触发条件、数值、边界情况全文本化。
4. **战斗日志（Turn Log）**：可回看每个回合发生了什么，数字追溯。
5. **图像风格克制**：全程卡片 + 背景 + 敌人 sprite，**几乎没有过场动画**，节奏极快。

**对 K-ToME 的建议**：

1. **Intent Telegraph 作为 Boss 以外的常态**。不只 boss 才 telegraph，普通敌人下一招的行动意图也应该常驻显示（图标 + 伤害预估），让战斗决策从"赌"变成"算"。这是 review §3.8 "倒数节奏"的升级版。
2. 关键词悬停二级 tooltip 直接照搬 ToME4 + StS 双模板。
3. **Turn Log 回溯面板**（review §5.7 第 5 条"回溯滚动锁"之外，额外加按回合检索的面板），让硬核玩家能做 post-mortem 分析。

### 1.5 Dwarf Fortress（Steam 版） — ASCII 到 Tile 的平滑升级

**定位**：2022 年 Steam 版把 40 年的 ASCII 巨兽升级成 tile 界面，同时保留 ASCII 模式可切换。

**关键设计手法**：

1. **ASCII 与 Tile 双渲染模式共存**。玩家菜单里可实时切换，同一局存档两种视图完全一致。
2. **鼠标 + 键盘双输入通道**。传统 DF 是键盘驱动，Steam 版加入鼠标点击菜单但保留所有键盘快捷键。
3. **工具提示集中化**。原版 DF 信息散在各菜单，Steam 版把所有"为什么不能做"收敛到统一 tooltip 格式。
4. **音乐与音效**。加入 Kevin MacLeod 式配乐，彻底改变氛围感但不破坏核心玩法。

**对 K-ToME 的建议**：

1. K-ToME 已有 Tile 渲染为主、Classic (ASCII) 可选的方向——**DF Steam 版的切换模型是黄金标准**。两种模式共享 `RenderSnapshot`，仅 `TileRenderer` 层面不同。
2. **鼠标作为键盘的非抢夺辅助**。点击地图 = 检视；点击目标卡按钮 = 对应键位——不抢键盘玩家的主线，但帮鼠标玩家入门。
3. 音效预算。配乐 + 情境音效（治疗/受伤/等级提升）直接提升 30% 沉浸感，远高于视觉打磨的边际收益。K-ToME 当前 `client/audio/` 目录存在但内容覆盖面有限，值得作为独立工作包前置。

### 1.6 Into the Breach — 空间即 UI 的极致

**定位**：FTL 开发商的第二作，回合制网格战棋 + 完全可预见信息。

**关键设计手法**：

1. **敌方下一步完全绘制在地图上**：箭头显示移动方向、红色格子显示攻击范围、数字显示伤害。玩家每一回合看到的就是"如果我不行动，下一回合会发生什么"。
2. **撤销（Undo）机制**。每个回合可以撤销移动（不可撤销攻击），消除"手滑点错"的沮丧。
3. **玻璃化屏幕**。没有任何现代游戏的油腻贴图，全部扁平 + 网格，信息密度最大化。
4. **三机甲小队的齐心感**。每次行动都是整个队伍的协作，失败的代价由队伍分担。

**对 K-ToME 的建议**：

1. **Diegetic HUD**（review §12.1）的最强背书。K-ToME 既然已经是 tile 地图，必须把 telegraph / 状态 / 伤害预估**直接画在地图格上**，不用全靠右栏。
2. **Undo 机制**作为新手友好的可选项。按 `u` 撤销上一步移动（不可撤销战斗行动），能让新玩家不怕尝试。
3. 界面**"玻璃化"美学**——不追求油画质感，追求"每一像素都有信息"。

### 1.7 Dungeon Crawl Stone Soup（DCSS） — 反 Grind 主义

**定位**：开源 roguelike 的"玩法纯度守护者"，40 年来砍掉所有 "noise" 系统。

**关键设计手法**：

1. **Autoexplore / Autotravel / Autofight**：`o` 自动探索、`\` 回到商店、`tab` 自动攻击。**重复性操作被完全消除**。
2. **移除饥饿系统、身份识别惩罚、恶搞陷阱等传统 grind**。DCSS 口号："Remove the grind, keep the challenge."
3. **死亡回放**。死亡后可回看最后 N 回合，帮助学习。
4. **内置 tutorial**。5 个简短 tutorial 局，覆盖基本机制，新玩家可零门槛入门。

**对 K-ToME 的建议**：

1. **Autoexplore 是桌面 roguelike 的 ROI 最高 UX 改造**，应在 `P2-W5` 期内前置——否则玩家在空房间里按 50 次方向键就退坑了。
2. "Remove the grind, keep the challenge" 作为玩法内容扩张的北极星——每新增一个机制问一句"它是挑战还是 grind"。
3. **死亡回放**（review §12.5 第 2 条）的哲学出处：DCSS 20 年前就这么做。

### 1.8 Rift Wizard（+ 续作） — Tooltip 极致透明

**定位**：单人开发的法术 roguelike，把"每个技能都完全透明"做到偏执。

**关键设计手法**：

1. **每个法术 tooltip 显示所有升级分支**。不是只显示下一级，而是整棵分支树，玩家永远知道"如果我走这条路，终点是什么"。
2. **所有伤害来源标注 tag**（火焰/寒冰/神圣/物理），抗性计算完全可预估。
3. **AoE 形状可视化**。施法前红色格子高亮影响范围，误伤友军的风险提前可见。
4. **整局选择可回溯**。虽然不是 undo，但每个 run 的 build 路径可查。

**对 K-ToME 的建议**：

1. **tooltip 的"显示所有分支"比"显示下一级"信息密度高 10 倍**。K-ToME 的天赋升级预览不应只显示下一级，而是整棵 tree 的最终形态。
2. **伤害 tag 系统**作为 `core/game` 的数据契约：每次伤害附 tag，`client` 消费 tag 决定颜色和日志 icon。
3. AoE 可视化强制在地图格上画，进一步坐实 Diegetic HUD。

### 1.9 Path of Exile 2 — 高级 Tooltip 与装备对比框

**定位**：ARPG 标杆，tooltip 工程学的天花板。

**关键设计手法**：

1. **Advanced Tooltip（Alt 键切换）**。基础 tooltip 只显示关键信息；按住 Alt 切换到 advanced 模式，显示所有词条的数值范围、tier、哈希值。**两档密度切换**。
2. **装备对比框（Ctrl 键 hover）**。鼠标 hover 一件可穿戴的物品时按 Ctrl，旁边弹出当前已装备同槽位的对比框，差值 +/- 高亮。
3. **Passive Tree**（天赋树）可视化近 2000 个节点，依然保持可扫读——靠**大节点 / 中节点 / 小节点**三档字号 + 主题色分区。
4. **过滤器脚本（Item Filter）**。玩家可自定义"什么品质以上的装备才显示"，ARPG 的终极反 grind。

**对 K-ToME 的建议**：

1. **Advanced Tooltip（两档密度切换）**：基础模式给新玩家一句话效果；Alt 按住切换为 advanced 显示所有公式、抗性、边界条件。ToME4 的 tooltip 已经很密但是一档，PoE2 的两档更友好。
2. **装备对比框**作为背包/检视的标配（review §3.7 第 2 条）。
3. **Item Filter 思路**→ K-ToME 的"日志 filter 层"（§1.3 Cogmind 已提）+ "掉落 filter"（低品质装备自动不拾取）。

### 1.10 Hades II — 微交互的叙事密度

**定位**：Supergiant 顶级 roguelite，叙事与机制融合的当代范本。

**关键设计手法**：

1. **Boon（祝福）徽章化**。每次选择一个神的祝福，右上角堆叠图标——**主题色严格按神分**：Zeus 黄、Poseidon 蓝、Ares 红、Aphrodite 粉……玩家一眼看懂 build 倾向。
2. **每个 Boon 有关键词**：Chain / Doom / Revenge 等，同主题 Boon 可触发连锁效果，UI 显示触发条件。
3. **死亡即进度**：死亡后在老家有 NPC 对话、资源投资、永久升级——每次死亡都是正反馈。
4. **极致微动效**：数字飞字、伤害闪烁、暴击屏幕震动、技能冷却圆形扫光——每一帧都有信息。
5. **Codex（图鉴）自动解锁**。每遇到新元素自动加入图鉴，玩家可回查。

**对 K-ToME 的建议**：

1. **主题色严格按系统分类**（review §3.8 第 4 条的加强版）：法术/词缀/天赋按学派颜色严格区分，玩家能从"色彩构成"一眼看懂当前 build。
2. **图鉴自动解锁**。遇到新怪物/物品/词缀自动加入图鉴，无需"翻书式"主动查阅。降低机制学习门槛的黄金手段。
3. Hades II 的动效节奏是 review §5.4 的教科书，**但要警惕美术风格 ≠ 可借鉴**——不学它的插画质感，学它的时间曲线。

---

## 2 · 次级参照（简要展开）

只列最关键的一条手法和对 K-ToME 的一条建议。

### 2.1 Darkest Dungeon 1/2 — 角色卡情绪压力

角色卡"头像 + 血条 + 压力条 + 怪癖标签"三联布局，紧凑但信息密度极高。→ **K-ToME 右栏玩家卡 / 目标卡**直接对齐 DD 的三联骨架。

### 2.2 FTL: Faster Than Light — 子系统独立 + 暂停即思考

飞船每个子系统独立 HP，空格键暂停后分配能量、指派船员、设置武器目标。→ **暂停即思考**作为非实时 roguelike 也可借鉴——K-ToME 可在战斗态允许"指令预排"：先设定 3-4 回合意图，再逐回合确认。

### 2.3 Jupiter Hell — 现代化桌面 roguelike 模板

3D Tile + 传统回合 + 彩色日志 + 现代 UI，是 K-ToME 最接近的技术形态参照。→ **整体布局比例**可对齐 Jupiter Hell（中央地图大而不满屏，右栏紧凑，顶部状态条）。

### 2.4 Balatro — 微动效节奏大师

分数飞字、卡牌 bounce、手牌 tilt、背景慢流光——极致克制但精准。→ **动效时间曲线**作为 Design Token 的基准：120ms 快 / 200ms 标准 / 400ms 慢 / 800ms 呼吸，Balatro 的时间常量可直接抄。

### 2.5 Final Fantasy XIV — 目标条 + Cast Bar 标准

屏幕顶部永远是当前目标（名称 / HP / 距离 / 层数），目标正在施法时下方浮现 cast bar + 技能名。→ **Cast Bar 作为 Boss telegraph 的具象形式**：右栏目标卡下方加 cast bar，倒数到 0 时释放，玩家有明确反应窗口。

### 2.6 Crusader Kings 3 — 禁用原因 UI 集大成

按钮禁用时 hover 弹 tooltip，结构为"❌ 需要 X / ❌ 对方拒绝 / ✅ 可以执行但代价高"分行展示，玩家知道**还差什么**才能执行。→ **禁用原因 tooltip 模板**（review §3.9 + §5.5 第 2 条）的落地参照。

---

## 3 · 横向对照表：每条设计原则的最佳出处

| 设计原则 | 最佳出处 | 次选 |
| --- | --- | --- |
| 解释型决策分层 | Slay the Spire | PoE2, Crusader Kings 3 |
| Tooltip 关键词高亮 + 子 tooltip | ToME4 | StS, Rift Wizard |
| Tooltip 两档密度切换（基础/Advanced） | Path of Exile 2 | Stellaris |
| 装备对比框 | Path of Exile 2 | Diablo 2 |
| iconKey 全链路消费 | ToME4 | Hades II |
| 颜色即一等语义 | Cogmind | Hades II |
| 主题色按系统严格分类 | Hades II | StS |
| 空间 HUD / Diegetic UI | Into the Breach | Cogmind, Jupiter Hell |
| 部位独立 HP 与身份影响 | Cogmind | CoQ |
| 对称代入感（玩家 ↔ 目标） | Caves of Qud | ToME4 |
| Intent Telegraph 常态化 | Slay the Spire | Into the Breach |
| Cast Bar 倒数节奏 | FFXIV | Rift Wizard |
| 日志分层分组 filter | Cogmind | DCSS |
| 同源伤害聚合 | Cogmind | DCSS |
| 自动探索 / 反 grind | Dungeon Crawl Stone Soup | ToME4 |
| 死亡回放 | DCSS | StS Turn Log |
| 图鉴自动解锁 | Hades II | ToME4 |
| 撤销 / Undo | Into the Breach | DF Steam |
| ASCII/Tile 双渲染切换 | Dwarf Fortress (Steam) | NetHack tilesets |
| 微动效时间曲线 | Balatro | Hades II |
| 死亡预警 Vignette | ToME4 | Darkest Dungeon |
| 禁用原因 UI | Crusader Kings 3 | PoE2 |
| 音效层次 | Dwarf Fortress (Steam) | Hades II |
| 键盘优先 + 鼠标辅助 | DF Steam | Jupiter Hell |

把这张表挂在墙上。每次做一项 UI 决策时，先问"最佳出处是谁、它是怎么做的"，比"我觉得这样好看"靠谱十倍。

---

## 4 · 六大设计哲学流派

把业界的设计哲学归纳成 6 派，不是穷尽，是为 K-ToME 选主线提供参照系。

### 4.1 机制透明派 — ToME4 / Slay the Spire / PoE2 / Rift Wizard

**信条**："玩家的失败必须是他的选择失败，不是信息失败。"

**共同手法**：所有伤害公式、概率、冷却、触发条件完全可见；tooltip 作为一等 UI；关键词系统化。

**风险**：信息过载、新玩家被劝退。对策是**分层 tooltip**（基础/进阶/专家三档）。

### 4.2 反疲劳派 — Dungeon Crawl Stone Soup / ToME4（部分）

**信条**："重复性操作不是挑战，是噪声。"

**共同手法**：autoexplore、autofight、快捷保存、无饥饿系统、简化身份识别。

**风险**：简化过度变成"挂机游戏"。对策是**保留危险时的主动决策点**——auto 只对安全情境有效，遇敌自动中断。

### 4.3 空间即 UI 派 — Into the Breach / Cogmind / Jupiter Hell

**信条**："如果信息可以画在地图上，就不要塞进面板。"

**共同手法**：telegraph 直接画格、伤害飞字从 tile 浮起、状态头顶徽章、路径虚线。

**风险**：地图视觉噪音。对策是**按重要度分层 alpha**——只有"当前决策需要的信息"用高 alpha，历史信息渐隐。

### 4.4 涌现派 — Caves of Qud / NetHack / Dwarf Fortress

**信条**："系统间的意外交互比预设剧情更有趣。"

**共同手法**：rich simulation（万物皆可交互）、程序化生成的世界/NPC/历史、规则间的涌现。

**风险**：学习曲线陡峭、UX 难以承载。对策是**Look Mode + 图鉴 + tooltip** 三件套，让涌现可被解释。

### 4.5 叙事融合派 — Hades II / Darkest Dungeon / Disco Elysium

**信条**："机制是叙事的载体，死亡是故事的一部分。"

**共同手法**：每次 run 推进元剧情、NPC 对话、美术风格强烈、主题色分类。

**风险**：叙事吞噬机制权重。对策是**机制日志与叙事文本明确分区**（review §5.7 第 1 条）。

### 4.6 微交互大师派 — Balatro / Hades II / Celeste

**信条**："手感 = 时间曲线 × 粒子 × 音效。"

**共同手法**：严格的动效时间常量、反馈粒子、屏幕震动节奏。

**风险**：动效喧宾夺主。对策是**动效预算**（review §9 第 2 条）+ 回合锚定（review §12.2）。

---

## 5 · K-ToME 应选择的主线哲学

建议：**"机制透明派为主 + 空间即 UI 派为辅 + 反疲劳派为底线 + 微交互大师派为打磨层"**。

拒绝作为主线的：**叙事融合派、涌现派**。

### 5.1 为什么主线是机制透明派

1. **词源锁定**。K-ToME = K（可能是 Kotlin）+ ToME，精神前身是 ToME4，ToME4 就是机制透明派。
2. **规则驱动 vs 叙事驱动**。K-ToME 当前 `RenderSnapshot / CombatResolutionTrace / iconKey / log token` 的架构就是围绕"机制可消费"设计的，和机制透明派天然同构。
3. **面向玩家人群**。硬核 roguelike 玩家（含 ToME4/CoQ/DCSS 圈）第一诉求是透明度，不是叙事。
4. **团队规模与现实**。叙事融合派（Hades 级）需要美术/编剧/配音/声优全栈，K-ToME 规模不适配；机制透明派只要数据契约干净就能做好。

### 5.2 为什么辅线是空间即 UI 派

1. **Tile 渲染的结构红利**。既然已经付出 TileRenderer 的技术代价，必须把收益拿满——telegraph/状态/伤害都画在地图上。
2. **与机制透明派天然兼容**。Into the Breach 本身就是"透明 + 空间"的融合，不冲突。
3. **桌面 + GPU 的介质优势**。Web roguelike 做不到的，K-ToME 必须做到，这是差异化点。

### 5.3 为什么底线是反疲劳派

1. **roguelike 重玩性的前提是低摩擦**。死一次要重开，重开要穿过 5 层空房间，玩家第 3 次死就退坑了。
2. **Autoexplore / Autofight 是现代 roguelike 的入场券**。DCSS 20 年前就有，K-ToME 不能没有。
3. **实现成本低、ROI 极高**。一周工作量，显著降低玩家流失。

### 5.4 为什么打磨层是微交互派

1. **"手感"是在机制做对之后才有意义的**。不能反过来——Balatro 的机制极简但微交互撑起了整个游戏，K-ToME 不走 Balatro 路线。
2. **微交互放到 Phase 5 再打磨最划算**。提前做会因为机制变动反复返工。

### 5.5 为什么不做叙事融合派主线

1. **AI 叙事应该是"可选的味精"而非主菜**。开启时增色，关闭时完整可玩。
2. K-ToME 当前"AI 叙事"的切换位置（review §3.5 第 4 条）必须明确：默认关闭、玩家主动开启；不允许叙事吞掉机制日志的视觉权重。
3. 若以叙事为主线，会迫使团队投入美术/文案预算，错失机制深度。

### 5.6 为什么不做涌现派主线

1. **涌现派的 UI 成本极高**（CoQ 做了 10 年还在补 UI 缺口）。
2. K-ToME 当前 phase 合同要求"最小正式可玩切片先收口"，涌现派的"系统 × 系统"扩展会冲垮这个合同。
3. 涌现派**可以作为长期方向**（Phase 5 以后），但不是当前主线。

---

## 6 · 分阶段的"取什么、不取什么"清单

### 6.1 P2-W5（当前）：打基础

**取**：

1. ToME4 的 tooltip 关键词高亮骨架（review §1.1）。
2. DF Steam 的 ASCII / Tile 双渲染切换框架（review §11 第 1 条的远期伏笔）。
3. DCSS 的 Autoexplore（review §5.3 + 本文 §1.7）。
4. Cogmind 的颜色即语义（写入 Design Token）。
5. CoQ 的 Look Mode（把 `x` 键升级为自由光标检视）。

**不取**：

1. Hades II 的美术强度。
2. 叙事文本的主视觉权重。
3. PoE2 的 passive tree 规模（等天赋内容量上来再说）。
4. CoQ 的涌现深度（收敛到"部位系统"即可）。

### 6.2 P3（状态/说明/telegraph）：做透明

**取**：

1. StS 的 Intent Telegraph 常态化（所有敌人下一招可见）。
2. Rift Wizard 的"显示整棵升级分支"tooltip。
3. PoE2 的 Advanced Tooltip 两档密度切换。
4. FFXIV 的 Cast Bar 作为 Boss telegraph 的具象。
5. Crusader Kings 3 的禁用原因 UI 模板。
6. ToME4 的死亡预警 vignette。

**不取**：

1. Hades II 的 Boon 连锁系统复杂度。
2. Disco Elysium 的对话骨架（不是 K-ToME 核心）。

### 6.3 P4（内容扩张）：做空间

**取**：

1. Into the Breach 的敌方行动格绘制。
2. Cogmind 的武器槽独立热量（可选，作为装备深度扩展）。
3. Hades II 的图鉴自动解锁。
4. DCSS 的死亡回放。

**不取**：

1. NetHack 的物品 identify 惩罚。
2. CoQ 的历史传说文本（工作量太大）。
3. Angband 的饥饿/负重（经典但是 grind）。

### 6.4 P5+（打磨与边界）：做手感

**取**：

1. Balatro 的动效时间曲线标定。
2. Hades II 的主题色严格分类。
3. DF Steam 的音效层次。
4. FTL 的暂停即思考（可选，给新玩家）。
5. Into the Breach 的 Undo（可选，给新玩家）。

**不取**：

1. 任何喧宾夺主的常驻动画。
2. 任何牺牲可达性换美术的折中。

---

## 7 · 结语

K-ToME 最珍贵的资产不是"做得像哪款游戏"，而是：

1. 有 ToME4 作为词源锁定的机制透明哲学；
2. 有 Kotlin + libgdx + RenderSnapshot 的干净数据契约；
3. 有 Tile + 回合 + 部位系统三个可以把空间 HUD 做透的结构红利；
4. 有足够小的团队规模可以快速迭代微交互。

业界的 10 + 6 个参照不是"抄作业清单"，而是**当 K-ToME 的某个模块要做决策时可以查的出处库**。主线守住"透明 + 空间 + 反疲劳 + 微交互打磨"，每次 UI 决策先翻 §3 横向对照表、再查 §1/§2 的具体做法，就不会在"像深渊纪事吗"这类伪问题上浪费时间。

最终目标不是"成为下一个 ToME4 / CoQ / Hades"，而是成为**只有 K-ToME 能做到的那款桌面 roguelike**——基于 ToME 的透明哲学、Tile 的空间红利、Kotlin 的工程优势、本项目 phase 合同的数据契约优势，做出一个业界还没有的**"机制透明 + 空间 HUD + 干净数据契约"**三位一体的 roguelike。

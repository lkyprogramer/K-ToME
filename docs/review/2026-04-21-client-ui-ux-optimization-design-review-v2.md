# K-ToME 客户端 UI/UX 优化设计文档 Review v2

## 0 · Review 视角与基准

本 Review 对 [`docs/opt/2026-04-20-client-ui-ux-reference-driven-optimization-design.md`](../opt/2026-04-20-client-ui-ux-reference-driven-optimization-design.md) v2（已根据前两轮 review 改写）进行第二轮深入评审。

1. 基准：前两轮 review ([v1](./2026-04-20-client-ui-ux-reference-driven-optimization-design-review.md)、[业界参考](./2026-04-20-roguelike-industry-reference-design-philosophies.md)) 的全部建议作为底线清单。
2. 视角：**规划落地性** > **概念完整性**。用户明确强调"后续改造的规划和设计必须尽可能补充完善、确认细节"，本 Review 把 §9 规划层作为重点压力测试面。
3. 范围：不重复已在前两轮 review 中讲过的概念层问题，只关注本次 v2 引入的新文本以及仍未收口的细节。
4. 对当前代码的交叉验证基于：`TileRenderer.kt` / `FoundationGameScreen.kt` / `StatusHudRenderer.kt` / `StatusIconResolver.kt` / `InputHandler.kt` / `MainMenuScreen.kt`。

---

## 1 · 文档已吸收的显著改进

为避免"全是问题"的不平衡 review，先明确本次 v2 比 v1 进步的地方：

1. **§3.3 明确承认了前两轮 review 的 5 条核心反馈**并转化为设计约束，而不是照搬。
2. **§3.4 / §3.7 明确"不吸收"清单**——三栏强制、深渊纪事主模板、渲染风格切换、元叙事、过早 passive tree 等全部显性降级。这是理性的"反设计"姿态。
3. **§3.6 行业参考表**把 ToME4 / CoQ / JH / StS / ItB / DCSS / PoE2 / Hades II / Balatro 的"吸收什么 / 什么阶段"列成可检索的表格，比 v1 "笼统提一个深渊纪事" 质变。
4. **§5.1 主线哲学选型**敲定"机制透明派为主 + 空间即 UI 为辅 + 反疲劳为底线 + 微交互为打磨层"，为后续决策提供北极星。
5. **§6.2 "面稳定，不预设唯一栏位形态"**把"固定三栏"降级为"世界面 / 上下文面 / 角色面 / modal 面"四类信息面职责——更贴合 K-ToME 真实的 map-dominant 布局现状。
6. **§6.5 ~ §6.8 新增 4 条原则**（键盘优先 / 地图与空间感 / 动效克制以回合为节拍 / design token 只属于 client）——这是 v1 缺失的桌面 roguelike 骨架。
7. **§7.3 K-ToME 特有机会**（空间即 UI / 回合即节拍 / 按键即 affordance / modal / 本地快照 / Inspect 升级 Look Mode）完整吸收了前一轮 review §12 的六大差异化点。
8. **§7.6 装备品质 contract 约束**明确锁到 `NORMAL / MAGIC / RARE + UNIQUE / ARTIFACT`——比 v1 "借用 ARPG 通用品质色" 更稳健，避免 client 先跑在规则层前面。
9. **§7.8 细粒度视觉与微交互规范**引入 8 条"最细小改动"的首轮落地。
10. **§8.1 / §8.2 / §8.4 组件层三件套**（design token / 信息面 view model / 解释型按钮基座）形成可实现的工程中间层。
11. **§9.1 P2-W5.0 新增前置收口层**——把"design token + 信息面骨架 + 去开发态标题 + 重拍 golden"作为所有后续 W 的地基，这是 v1 没有但前两轮 review 明确要求的关键前置包。
12. **§9.9 长期高价值候选**替换了"一次性全做"的野心，明确 Look Mode 增强 / Autoexplore / Advanced Tooltip / Death Replay / Seed Share 作为非当前阶段承诺项。

---

## 2 · 最大的问题：§9 规划深度不足

用户明确要求"**后续改造的规划和设计必须尽可能补充完善**"，但现版 §9 的每个子包仍然只有 3–4 条纲要式 bullet。作为实际可执行的 roadmap，以下维度**每个 W 都缺**：

| 缺失维度 | W5.0 | W5a | W5b | W5c | W6–7 | P3-W2 | P3-W3 | P3-W4 |
| --- | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
| Exit Criteria（完成判据）| ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 前置依赖（contract / 上游 W）| ❌ | ❌ | ❌ | ❌ | 部分 | ❌ | ❌ | ❌ |
| 产出物清单（文件 / 类 / contract）| 部分 | ❌ | ❌ | 部分 | ❌ | ❌ | ❌ | ❌ |
| 自动化验证点（test / lint / golden）| ❌ | ❌ | ❌ | ❌ | 部分 | ❌ | ❌ | ❌ |
| 工作量估算（粗粒度）| ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 风险与兜底（降级路径）| ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 最小切片定义（若只做 50% 还能否交付）| ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

**没有这七个维度，§9 只能作为"目标宣言"，不能作为"执行手册"。** 本 Review §5 提供每个 W 的增强模板，建议文档 v3 整章改写 §9。

同时 §9 整体缺以下结构：

1. **依赖关系图**。W5.0 是否硬阻塞 W5a/b/c？W5.0 内部 4 个子项能否并行？P3-W2 是否必须等 W5c 完成？当前文档用顺序隐含依赖，但真实工程里很多可并行。
2. **跨阶段回归策略**。W5b 改了布局后，W5c / W6 做完时要不要重拍 W5b 的 golden？是每 W 独立 golden 还是累积 golden？
3. **最小可交付切片（MVP slice）定义**。例如 W5.0 的 MVP slice 能不能只做 token + 信息面容器，不拆 view model？
4. **失败/延期的处理**。若 W5b 因为规则层依赖延期，W5c 是否能独立推进？
5. **各 W 之间的 golden 命名和版本管理**。建议建立 `golden/vN-wXX-yyyymmdd/...` 命名规范，避免 W5b 和 W5c 的 golden 互相覆盖。

---

## 3 · 蓝图 §7 与规划 §9 缺映射矩阵

§7 目标体验蓝图列了 8 个子节，§9 规划列了 9 个 W，但**没有一张表把它们映射起来**，导致以下问题：

1. §7.1 首页 5 个子项里，只有 §9.2 W5a 覆盖"快速开始 / 差异化 / 构筑差异 / locale"；**"首焦点"这一条在 W5a 里没有显性提及**——容易漏。
2. §7.2.D modal / drawer 面在 §9.3 W5b 里只一句"map-dominant 布局与 modal / drawer 路径"带过，但 §7.2.D 实际列了 5 种内容（背包 / 商店 / 检视 / 事件详情 / 构筑详情）——**"事件详情"和"构筑详情"两个 modal 在规划里完全没对应**。
3. §7.3 K-ToME 特有机会 6 条里，只有 §9.1 覆盖"去开发态标题" 间接响应"按键即 affordance"的一部分；"回合即节拍"、"本地快照 seed"、"modal 优势"、"Inspect 升级 Look Mode"——**这 4 条都没有明确落在哪个 W**。
4. §7.4 事件房 / 商店 / 奖励房统一卡片模型——**在 §9 整条线里都没有明确 W 归属**。按文档语义属于 W5b 或 W5c 但没写。
5. §7.8 微交互 8 条——**全部没有显性归属 W**。等宽数字、焦点戒指、禁用态、战斗节拍词、伤害飞字、日志块边界、色盲回退——这些应该在 W5.0 token 阶段就定义，但 W5.0 只说了"建立 token"没说"覆盖哪些具体细节"。

建议文档 v3 新增一节 `§9.0 蓝图—规划映射矩阵`，用一张 "§7 子节 × §9 W" 的矩阵固化归属。本 Review §4.1 给出完整矩阵示例。

---

## 4 · 必须补完的维度（v2 仍然完全缺失的）

### 4.1 蓝图—规划映射矩阵（样例）

```
§7 蓝图子项                     W5.0  W5a  W5b  W5c  W6-7  P3W2 P3W3 P3W4
§7.1 首页 · 首焦点              ·     ✓    ·    ·    ·     ·    ·    ·
§7.1 首页 · 快速开始/继续        ·     ✓    ·    ·    ·     ·    ·    ·
§7.1 首页 · build 能力速览       ·     ✓    ·    ·    ·     ·    ·    ·
§7.1 首页 · 新老玩家差异化       ·     ✓    ·    ·    ·     ·    ·    ·
§7.1 首页 · 常驻帮助区          ✓     ·    ·    ·    ·     ·    ·    ·
§7.2.A 世界面                   ·     ·    ✓    ✓    ·     ·    ·    ✓
§7.2.B 上下文面（日志）          ·     ·    ✓    ·    ·     ·    ·    ·
§7.2.C 角色/目标/动作面          ·     ·    ✓    ·    ·     ✓    ✓    ✓
§7.2.D modal/drawer 面          ·     ·    ✓    ·    ·     ·    ·    ·
§7.3 空间即 UI                  ·     ·    ·    ·    ·     ·    ·    ✓
§7.3 回合即节拍                  ·     ·    ·    ·    ·     ✓    ·    ·
§7.3 按键即 affordance           ✓     ·    ✓    ·    ·     ·    ·    ·
§7.3 modal 优势                  ·     ·    ✓    ·    ·     ·    ·    ·
§7.3 本地快照 seed               长期候选 §9.9
§7.3 Inspect → Look Mode         ·     ·    ✓    ·    ·     ·    ·    ·  （基础）长期 §9.9（完整）
§7.4 事件/商店/奖励卡片模型       ·     ·    ✓    ✓    ·     ·    ·    ·
§7.5 战斗决策三层                ·     ·    ·    ·    ·     ·    ·    ✓
§7.6 背包/商店/检视统一语言       ·     ·    ·    ✓    ·     ·    ✓    ·
§7.7 状态 & telegraph            ·     ·    ·    ·    ·     ✓    ·    ✓
§7.8 微交互 8 条                 ✓     ·    ✓    ✓    ·     ✓    ✓    ✓
```

列出这张矩阵后立刻暴露 v2 的覆盖漏洞：§7.1 "常驻帮助区"、§7.3 "回合即节拍"、§7.3 "按键即 affordance"、§7.4 "事件/商店/奖励卡片模型"——**v2 全部没有显性 W 归属**，建议在文档 v3 显性锁定。

### 4.2 "统一语义"未固化

§6.5 第 4 条写"`ESC / Backspace / F / Tab / ?` 等高频键位要有统一语义"，但**没有告诉读者"统一语义"是什么**。本 Review 建议直接在 §6.5 或新增 §6.10 固化一张键位语义表：

| 键 | 地图态 | modal 态 | targeting 态 | 通用原则 |
| --- | --- | --- | --- | --- |
| `ESC` | 打开主菜单 | 关闭当前 modal（不堆栈回退） | 取消 targeting，回地图态 | 退出当前上下文 |
| `Backspace` | 无 | 回退一层 modal 栈 | 回动作层选择 | 上一层决策 |
| `F` / `.` | 等待一回合 | 无 | 无 | 空等 |
| `Tab` | 切换侧栏折叠 | 焦点循环 | 切换目标 | 焦点/视图切换 |
| `?` | 打开帮助 overlay | 打开 modal 内帮助 | 解释当前动作 | 上下文帮助 |
| `i` | 打开 Inventory modal | 无 | 无 | 快捷 modal |
| `x` | 进入 Look Mode | 无 | 无 | 检视模式 |
| `Enter` | 上下楼 / 确认 | 确认当前焦点 | 确认当前目标 | 肯定性提交 |
| `1-9` | 快捷动作 | 无 | 直接选择对应编号 | 数字热键 |

**没有这张表，各 W 各自实现很容易出现"ESC 在战斗里直接退出游戏"这种体验灾难。**

### 4.3 错误态 / 空态 / 加载态 规范

v2 完全未覆盖以下 UX 关键面：

1. **资源缺失态**：`iconKey` 在 manifest 中找不到时 UI 显示什么（降级字符？问号图标？隐藏？）。§11.1 `contract-lint` 会在 lint 阶段报错，但**运行时兜底**必须定义。
2. **空状态**：背包 0 物品、日志 0 条、检视无光标选中——UI 该显示什么文案 / 插画 / 提示？
3. **加载态**：content pack 解析中、首次进入游戏时的 splash loading 设计？
4. **错误态**：save 损坏、snapshot 断层、manifest 版本不匹配——UI 如何引导玩家？
5. **窗口异常态**：最小化、失焦、分辨率变化——UI 是否保持绝对坐标？是否暂停游戏？

建议 §7 新增 §7.9 或 §8 新增 §8.5 覆盖。

### 4.4 a11y（无障碍）全维度

§7.8 第 8 条"高对比度与色盲回退"只覆盖了 a11y 的一小块。缺：

1. **字号可调**。最小 / 默认 / 大 / 特大四档切换，覆盖所有正文与 UI 元素。
2. **键盘导航完整性**。保证每个可交互元素都有键盘路径，Tab 焦点链不断裂。
3. **屏幕阅读器友好度**。即使桌面游戏，文字元素也应有语义 label（Kotlin / libGDX 不直接支持 ARIA，但可通过 Stage / Actor 的 description 字段暴露给辅助工具）。
4. **动效可关闭**。`Reduce Motion` 选项——关闭后动效时长归零或改为瞬切。Balatro / Hades II 都有这个开关。
5. **色盲模拟预览**。设置面板内嵌 Deuteranopia / Protanopia / Tritanopia 预览。
6. **输入延迟容忍**。按键到响应间隔的 UX 上限（建议 ≤ 100ms），超过要有视觉 feedback。

建议在 §6 新增 §6.10 "可访问性基线" 作为一条单独原则，并在 §11 "验证矩阵" 里新增 a11y lint 和 a11y golden screenshot（用色盲滤镜跑一次 golden）。

### 4.5 音效 / 反馈层完全缺失

前一轮 review §1.5 参照 DF Steam 版已经提过"音效是 ROI 最高的沉浸感提升点"，v2 **完全没有吸收**。

建议 §7 新增 §7.10 或 §6 新增 §6.11 "声音作为信息层"：

1. **音效分层**：UI 操作音 / 战斗反馈 / 环境氛围 / 叙事节拍 四层互不覆盖。
2. **信息型音效**：HP<25% 心跳声、telegraph 倒计时滴答、敌人出场低音。
3. **与 `AudioManifest` 的 contract 边界**：音效也应该从 manifest 来，client 不能硬编码资源路径。
4. **静音 / 音量独立控制**：UI / SE / BGM / 叙事旁白四个独立滑杆。

这和 §6.1 单一权威一致——所有音效走 `AudioManifest`，避免 client 硬编码。

### 4.6 Onboarding / Tutorial 体系完全缺失

§7.1 提到"新老玩家差异化首屏"和"新玩家安全入口"，但没有说 **tutorial 体系**。DCSS / ToME4 都有专门 tutorial 局。建议：

1. 一个**独立的 Tutorial UiMode**（和 MAP / SHOP / INVENTORY 等并列），覆盖 5 个分钟级教学关：移动 / 基础战斗 / 背包与装备 / 天赋使用 / 事件决策。
2. 每个 tutorial 局用**预置 seed + 强制教学事件**推进，和主局隔离。
3. **首启自动进入 tutorial 询问**（"是否教学？否 / 快速入门 / 完整教学"）。
4. Tutorial 局的日志带有**教学高亮**（比普通日志多一种 tone），结束后自动解锁 Codex 里对应条目。

这属于长期候选，建议补入 §9.9。

### 4.7 性能预算

§6.7 第 3 条"高频 idle animation 吃掉帧预算"是对的，但**没有预算数字**。建议：

1. **60 fps 硬目标**：单帧总渲染时间 < 16.6ms，其中 UI 部分 < 3ms。
2. **modal 切换 < 50ms 完成首帧**（避免白屏感）。
3. **日志 append 操作 < 1ms**（否则战斗爆量会卡顿）。
4. **golden screenshot 覆盖"极端场景"**：20+ 怪物 + 10+ telegraph + 50+ 日志行同屏帧时间。
5. 新增 `client-perf-lint`：一个 smoke test 跑 1000 帧并断言帧时间 P95 < 20ms。

建议在 §11 新增 §11.4 "性能验收"。

### 4.8 Telemetry / 观察口

v2 完全没有"改完怎么知道用户真的用得更好"的回答。建议：

1. **UI 事件埋点**（本地、不联网）：modal 打开次数 / 时长 / 首次到决策键的时间 / Tab 切换频率 / 键位误按率。
2. **死亡态自动收集**：死亡前最后 10 回合快照 + 最后一个 modal + 最后一个键——帮助开发者分析 UX 瓶颈。
3. **本地 dump**：按 F10 导出当前会话的 UI 事件流到 `logs/ui-session-<timestamp>.json`，方便玩家随截图附带发反馈。
4. 这些数据**不联网**，也不进入 `RenderSnapshot`（遵守 §6.1 和 §6.8）。

建议在 §11 新增 §11.5 "观察口"。

### 4.9 Review gate / PR checklist

§9.5 P2-W6~P2-W7 说"新增内容时同步补齐图标 / 检视 / locale"，但没有**强制执行机制**。建议在 §9 末尾新增 §9.10 或独立 §13：

1. **UI-affecting PR checklist**（固化到 `.github/pull_request_template.md` 的 UI 段）：
   - [ ] 新增内容已有 `iconKey`
   - [ ] 新增内容已有 zh-CN / en-US 两语 locale
   - [ ] 新增 UI 组件使用了 design token，未硬编码颜色/间距
   - [ ] 新增 UI 状态已被 `ClientSmokeHarnessTest` 覆盖
   - [ ] 新增 UI 状态已重拍相关 golden
   - [ ] 新增键位已加入 §6.5 统一语义表
   - [ ] 新增禁用态按钮已提供 disabledReason
2. **Design review gate**：涉及 §7.2 信息面职责调整的 PR 必须有 1 名设计 reviewer 签字。

### 4.10 其他维度（简要列）

1. **响应式 / 多分辨率**。文档完全没提 1280×720 / 1600×900 / 1920×1080 / 2560×1440 的差异策略——golden screenshot 以哪个分辨率为准？是否多分辨率 golden？
2. **颜色主题**。未来是否支持 Dark / High Contrast / Classic ASCII 三主题切换？即使当前不做，token 层设计时就应该预留。
3. **存档回滚 UX**。save 损坏时的"回到上一个保存点"引导文案和按钮路径。
4. **首次启动的用户数据目录说明**。save / screenshot / log 的落盘位置在哪？允许玩家修改？
5. **截图快捷键与水印**。前一轮 review §12.5 提过的 seed + turn 水印，v2 没吸收。

---

## 5 · §9 每个 W 的增强模板（推荐的重写结构）

建议 §9 每个子包都改写为以下模板。本 Review 为所有 W 填一版作为示范。

### 5.1 P2-W5.0 前置 | 设计 Token + 信息面骨架

#### 目标

建立 client-local design token、把 `TileRenderer` 的"地图 + sidebar + bottom cards"重构为稳定的信息面骨架，为后续所有 UI 工作包提供共享基础设施。

#### 产出物

1. `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt`（或等价 json + loader）
   - `ColorToken`: 至少 24 个语义色（hp-high/mid/low、mp、sp、exp、gold、品质 × 5、danger/warn/info/success × 主辅、日志分类 × 5、文本主/次/禁用）
   - `SpacingToken`: 8 档（2/4/8/12/16/24/32/48）
   - `TypographyToken`: 6 档（display/title/body/label/caption/numeric）+ 字重三档
   - `MotionToken`: 4 档（fast 120 / base 200 / slow 400 / pulse 800）+ 3 种 easing
   - `RadiusToken`: 3 档（sm 2 / md 4 / lg 8）
   - `StrokeToken`: 1 档（1px 40% alpha）
2. `client/src/main/kotlin/com/ktome/client/render/layout/InfoSurfaceLayout.kt`
   - 定义 World / Context / Character / Modal 四面的抽象容器
   - 不强制三栏，提供 `MapDominant / WideSplit / ModalOverlay` 三个响应式模式
3. 把 `TileRenderer` 内约 20 处硬编码色值替换为 token 引用
4. 窗口标题从 `"K-ToME v0.1.0 | arrows/numpad move, Enter stairs, x inspect, Ctrl+S save"` 改为 `"K-ToME · <profession> · 回合 T<turn> · 层 F<floor>/<floors> · 房间 <room>/<rooms>"`
5. 新 golden baseline：`golden/v2-w50-baseline/` 覆盖 zh-CN × en-US × (MAP 空态 / INVENTORY 空态 / BATTLE 空态) × (1600×900)

#### Exit Criteria

1. 所有 `TileRenderer` / `StatusHudRenderer` 硬编码色值 0 剩余（lint 断言）。
2. `UiDesignTokens` 被 ≥ 3 个 renderer 消费。
3. 窗口标题不再承担操作说明。
4. 新 golden 通过，旧 golden 已迁移或删除。
5. `ClientSmokeHarnessTest` 新增 token consumption smoke 断言通过。

#### 前置依赖

1. 无（作为最底层前置）。
2. 但若 `RenderSnapshot` 需要暴露新字段（如 turn / floor / room 给窗口标题），需先在 `core/game` 侧扩 snapshot。

#### 自动化验证

1. `token-lint`：扫 `client/` 下所有 `.kt` 文件，硬编码 `#` hex 色值数 = 0。
2. `golden screenshot v2-w50-baseline`：zh/en × 3 态 × 1 分辨率 = 6 张。
3. `ClientSmokeHarnessTest::tokenConsumption` 断言关键 renderer 至少消费 1 个 token。

#### 风险与兜底

1. 风险：token 命名难以一次定稿。兜底：先锁定**语义**（hp-high/mid/low）而非**数值**（#xx），允许后续精修色值。
2. 风险：布局重构破坏现有 golden。兜底：W5.0 内只搭骨架不动内容，旧 golden 保留为 `golden/v1-legacy/` 一周过渡期。

#### 最小切片

Token 层 + 窗口标题替换即可切出 MVP，布局重构可拆到 W5.0.1。

#### 工作量（粗）

1–1.5 周。

---

### 5.2 P2-W5a | 首页首屏优化

#### 目标

完成 `MainMenuScreen` 的首屏信息架构重构，覆盖首焦点、快速开始、新老玩家差异化、构筑摘要、常驻帮助 5 条。

#### 产出物

1. `MainMenuScreen` / `MainMenuController` 重写，首屏新布局。
2. `MainMenuStartupController`：检测首启 vs 有存档，决定首焦点落"新玩家引导" vs "继续最近局"。
3. `BuildCapabilitySummaryModel`：可玩职业/种族/词条的 `已可玩 / 总量` 速览数据聚合。
4. `SaveSlotSnapshot` 扩展：支持"最近局摘要"（职业 / 当前层 / 回合 / 存档时间）。可能需要上游 contract 扩展。
5. 首页 golden：`golden/v2-w5a/` zh/en × (首启态 / 有存档态 / 职业选择展开态) × 1600×900。

#### Exit Criteria

1. 首启 5 秒内到第一个可操作焦点（快速开始）。
2. 有存档玩家 Enter 一键继续。
3. 每个职业/种族有玩法标签 + iconKey + 资源类型摘要。
4. Locale 切换按钮在首页永远可见。
5. 常驻键位底栏包含 ≥ 5 个 affordance badge。

#### 前置依赖

1. W5.0 完成（token 和信息面骨架已建立）。
2. `core/game` 确认 `ClassDefinition` / `RaceDefinition` 是否已有玩法标签字段，若无需先扩。
3. `SaveSlotSnapshot` 扩展是否需要，若需先在 `core/game` 扩。

#### 自动化验证

1. `ClientSmokeHarnessTest::mainMenuFirstFocus`：首启 / 有存档两态首焦点断言。
2. `locale-lint`：首页所有文本无裸字符串。
3. `golden v2-w5a` × 6 张。

#### 风险与兜底

1. 风险：`ClassDefinition` 添加"玩法标签"会扩大到 content 侧所有职业文件。兜底：允许先在 content 里留空，client 侧 fallback 为"通用角色"标签。
2. 风险：`SaveSlotSnapshot` 扩展破坏旧存档兼容。兜底：新字段必须 nullable，旧存档读取时回退到"无摘要"。

#### 最小切片

不做"新老差异化"也能交付 MVP：首焦点 + 快速开始 + 基础职业展示。差异化和构筑摘要可延期到 W5a.1。

#### 工作量

1–1.5 周。

---

### 5.3 P2-W5b | 局内信息面稳定化

#### 目标

把当前"地图 + sidebar + bottom cards"重构为 §7.2 定义的四面（World / Context / Character / Modal），拆出独立 view model，统一键盘焦点与退出语义。

#### 产出物

1. `LogPresentationModel`（见 §8.2）：分类 × 重要度二维、房间锚点、同源聚合、折叠、叙事块边界。
2. `PlayerCardModel` / `TargetCardModel` / `ActionPanelModel`：三卡独立 view model。
3. `ModalStack`：modal 打开 / 关闭 / 焦点锁定 / 回退路径。
4. `InputHandler` 扩展：统一键位语义表（见本 Review §4.2）。
5. `ESC / Backspace / Tab / ?` 四个键在所有 UiMode 下行为一致。
6. Drawer 动效 <= W5.0 动效 token 规范（200ms）。
7. 新 golden：`golden/v2-w5b/` × (MAP / INVENTORY modal / INSPECT modal / EVENT modal / TARGETING) × zh/en。

#### Exit Criteria

1. 所有 modal 打开时 ESC 正确关闭并回到打开前焦点。
2. Tab 焦点链不断裂（自动化 smoke：连续 Tab 50 次不卡死不越界）。
3. `LogPresentationModel` 覆盖日志分类 × 重要度、聚合、房间锚点、叙事块四个基本能力。
4. 玩家卡 / 目标卡 / 动作卡在所有 UiMode 下都能稳定渲染。
5. 所有键位符合 §6.5 统一语义表。

#### 前置依赖

1. W5.0 完成（token + 信息面骨架）。
2. §6.5 键位语义表固化到文档。
3. 不依赖 W5a。

#### 自动化验证

1. `ClientSmokeHarnessTest::modalStackAndFocusLock`：开关 modal 焦点恢复断言。
2. `ClientSmokeHarnessTest::tabNavigationIntegrity`：Tab 50 次不崩。
3. `golden v2-w5b` × 10 张。
4. 新增 `log-model-lint`：断言所有新日志消息能路由到分类 tone。

#### 风险与兜底

1. 风险：`LogPresentationModel` 抽象扩大导致 `RenderSnapshot` 变更。兜底：LogPresentationModel 纯 client-local，消费 `LogTokenEvent` 已有 contract，不改 snapshot。
2. 风险：`ModalStack` 引入新 state 与 `InputHandler` 现有 overlay mode 冲突。兜底：先在 `InputHandler` 抽 `ModeController` 接口，overlay mode 作为其一种实现，modal stack 作为另一种。

#### 最小切片

`LogPresentationModel` + `ModalStack` 可单独先发。三卡 view model 可延到 W5b.1。

#### 工作量

2–3 周（规划层最重的一个包）。

---

### 5.4 P2-W5c | `iconKey` 首轮消费

#### 目标

把 `iconKey` 从 schema 字段扩展到 UI 全链路，完成状态徽章化、装备槽 / 背包 / 检视 / 掉落的图标覆盖、品质色 token 化、地图掉落提示。

#### 产出物

1. `StatusHudRenderer` 扩展：从 1.3K 扩到支持徽章（icon + 计数 + 分类 + tooltip hover）。
2. 所有可装备物检查 `iconKey` 非空（`contract-lint`）。
3. 品质色 token（对应 `NORMAL / MAGIC / RARE / UNIQUE / ARTIFACT`）进入 `UiDesignTokens`。
4. 品质色仅作用于名称 / 边角标 / 对比摘要，正文不染色（§7.6 第 5 条）。
5. 地图单元格掉落提示：
   - 单件掉落显示图标叠在地块右下角。
   - 多件掉落显示"图标 + 数量 badge"。
   - 玩家覆盖在掉落物上时地块边框发光。
6. 日志前缀 icon 覆盖 danger / reward / status / combat 四类。
7. Golden：`golden/v2-w5c/` × (状态徽章 / 背包 5 品质 / 地图多掉落 / 日志前缀) × zh/en。

#### Exit Criteria

1. `contract-lint` 断言：所有可装备物与 special template item 都有可解析的 `iconKey`。
2. `UiDesignTokens.qualityColor(quality)` 覆盖所有正式品质档。
3. 地图态下地面物品可见性 smoke test 通过。
4. 状态徽章支持堆叠 ≥ 5 个且不溢出（溢出时折叠为"+N"）。
5. 不在 client 侧引入未冻结品质档（`SET / LEGENDARY / MYTHIC` 都应编译错误而非静默 fallback）。

#### 前置依赖

1. W5.0 完成（token 已有）。
2. W5b 的 `StatusHudRenderer` / `TargetCardModel` 扩展完成。
3. content 侧所有装备的 `iconKey` 完整覆盖（可能 content 侧还有未补的）。

#### 自动化验证

1. `contract-lint::equipmentIconKey` 全量扫描。
2. `golden v2-w5c` × 10 张。
3. 状态徽章堆叠 smoke test。
4. 地图掉落 smoke test。

#### 风险与兜底

1. 风险：content 侧 iconKey 不全，lint 阻塞 CI。兜底：lint 先 warn，一周后升为 error；同时给 content 侧 issue 列表。
2. 风险：状态徽章溢出规则设计复杂。兜底：MVP 版本只做"最多 5 个，超过不显示"，折叠为"+N"作为 W5c.1。

#### 最小切片

状态徽章 + 品质色 + 日志前缀可先发。地图掉落提示可延到 W5c.1。

#### 工作量

1.5–2 周。

---

### 5.5 P2-W6~P2-W7 | 内容扩张期的体验一致性

#### 目标

保证随着内容（怪物 / 物品 / 房间 / 事件 / locale）扩张，UI 体验不退化。

#### 产出物

1. UI-affecting PR checklist（见本 Review §4.9）。
2. `content-ui-lint`：新增内容自动卡 iconKey / locale / logProfile 完整性。
3. 新怪物接入时自动跑一次 battle golden regression。
4. Locale 新语言接入时自动跑全量 golden 并人工 review 爆版。

#### Exit Criteria

1. PR checklist 上线，任何 UI-affecting PR 必须勾选。
2. `content-ui-lint` 跑在 CI 中，阻塞 merge。
3. 每次 content pack 扩张后，golden 重拍流程 ≤ 1 小时完成。

#### 前置依赖

1. W5a / W5b / W5c 均完成（否则 lint 规则不完整）。

#### 自动化验证

1. CI 跑 `content-ui-lint`。
2. PR checklist 提醒 hook（optional）。

#### 风险与兜底

1. 风险：PR checklist 沦为"勾了就交"的形式主义。兜底：checklist 第一条必须提供"此 PR 的新增 iconKey 列表"，审核人可交叉验证。

#### 最小切片

`content-ui-lint` 是硬骨头，checklist 是软习惯，建议先上 checklist，lint 作为 W5c.1。

#### 工作量

0.5–1 周（主要是流程建设）。

---

### 5.6 P3-W2 | 状态语义可见化

#### 目标

把状态系统的分类、badge、高风险层级、区域/持续效果一致表达落在 UI 上。

#### 产出物

1. `core/game` 侧状态分类枚举（`BUFF / DEBUFF / PERSISTENT / ZONE / TELEGRAPH`）—— **必须先在规则层立法**。
2. `StatusIconResolver` 扩展：按分类返回不同 badge 样式。
3. 状态堆叠规则：同分类最多 5 个可见，超过折叠为 "+N"；跨分类按 `TELEGRAPH > DEBUFF > BUFF > ZONE > PERSISTENT` 排序。
4. Badge 文字规则：优先显示 `剩余回合 → 层数 → stack/cap → 冷却`。
5. 高风险 telegraph badge 尺寸 28×28，普通状态 20×20；前者加边框脉冲（800ms）。
6. 区域效果同时在地图面和状态面显示（同 icon 同色）。

#### Exit Criteria

1. 状态分类枚举在 `core/game` 已固化且被 `RenderSnapshot` 消费。
2. 状态 badge 堆叠规则 smoke test 通过。
3. 高风险 telegraph 视觉权重断言：截图面积 / 主色亮度均高于普通状态 ≥ 20%。
4. 区域效果在地图面和状态面 iconKey 一致性断言通过。

#### 前置依赖

1. W5c 完成（iconKey 已全链路消费）。
2. `core/game` 状态分类 contract 扩展已合入。

#### 自动化验证

1. `status-category-lint`：扫所有状态定义，确保归类到某一分类。
2. `golden/p3-w2/` × (单状态 / 5 状态 / 溢出 / 高风险 telegraph / 区域效果) × zh/en。

#### 风险与兜底

1. 风险：状态分类扩枚举涉及大量 content 侧修改。兜底：`core/game` 侧提供默认分类 `PERSISTENT`，content 侧按需覆盖。
2. 风险：高风险 telegraph 视觉权重难以量化。兜底：用 golden 像素差分作为客观指标。

#### 最小切片

基础 badge + 分类可先发，区域效果一致性可延到 P3-W2.1。

#### 工作量

2 周。

---

### 5.7 P3-W3 | 动态说明 + 关键词消费

#### 目标

天赋 / 装备 / 词缀 / 地块说明统一进入解释型检视；关键词不只存在 tooltip，也进入主要阅读路径。

#### 产出物

1. `core/game` 侧 `KeywordDictionary` 立法：每个关键词（如 `burning` / `bleed` / `stun`）有 `key / nameKey / descKey / iconKey / colorKey` 五字段。
2. `DescriptionPresenter` 扩展：识别 `descKey` 中的关键词占位符并替换为高亮 span。
3. 检视面板：关键词高亮 + hover 展开子 tooltip（桌面鼠标 + 键盘聚焦双通道）。
4. 动态说明 + iconKey + 资源 + 构筑标签联合显示。
5. `KeywordUsageRegistry`：自动化统计每个关键词在 content 中的使用次数，用于关键词梳理。

#### Exit Criteria

1. `KeywordDictionary` 在 `core/game` 固化。
2. 所有 descKey 中的关键词占位符可被 `DescriptionPresenter` 解析。
3. 检视面板关键词高亮 + 子 tooltip 正常工作。
4. `keyword-lint`：descKey 中使用的关键词必须在 dictionary 中注册，否则报错。

#### 前置依赖

1. W5c 完成（iconKey 覆盖）。
2. P3-W2 完成（状态分类已立法，关键词常常和状态绑定）。
3. `KeywordDictionary` contract 扩展已合入。

#### 自动化验证

1. `keyword-lint` 全量扫描。
2. `golden/p3-w3/` × (装备说明 / 天赋说明 / 词缀说明) × (基础 / advanced) × zh/en。

#### 风险与兜底

1. 风险：关键词字典扩大后，content 侧所有老文本需要改写插入占位符。兜底：先识别"高频关键词" top-20 作为 MVP，其余陆续改写。
2. 风险：子 tooltip 嵌套过深影响可读性。兜底：限制嵌套最多 2 层。

#### 最小切片

`KeywordDictionary` + `DescriptionPresenter` 基础版可先发，子 tooltip 可延到 P3-W3.1。

#### 工作量

2–2.5 周。

---

### 5.8 P3-W4 | telegraph + 战斗二级决策

#### 目标

把 §7.5 战斗三层决策结构落地，telegraph 实现地图 × 目标卡 × 日志三位一体，普通敌人意图也进入可见范畴，侦察结果反向影响按钮解释。

#### 产出物

1. `AIPlanSnapshot`（或等价字段）：所有敌人下一回合意图作为 snapshot 字段 —— **规则层立法**。
2. `TelegraphRenderer` 重写：地图面扇形 / 直线 / 圆形绘制规则固化；同一 telegraph 的 iconKey / colorKey 跨面一致。
3. `CombatDecisionPanel`：动作层 → 方式层 → 目标层三层面板，每层都有独立的返回键（Backspace 上一层，ESC 全退）。
4. `AttackHintModel`：从规则层聚合 typed reason + 风险标签 + 高中低倾向 + 可选区间。
5. 侦察结果（`observe` 类动作）注入 `AttackHintModel`，使下一次进入战斗面板时按钮解释变化。
6. 高风险按钮（伤害 ≥ 玩家 HP 30%）自动加红边 + 预警 label。
7. Cast bar（FFXIV 风格）嵌入目标卡：敌人正在施放的技能 + 倒数。

#### Exit Criteria

1. 所有敌人（非 boss）至少提供一级意图图标。
2. Boss telegraph 同时在地图 / 目标卡 / 日志三处一致可见（自动化断言 iconKey × colorKey 三处一致）。
3. 三层面板 ESC / Backspace 行为符合 §6.5 统一语义表。
4. 观察类动作必须能改变下一回合 `AttackHintModel` 的 typed reason（smoke test）。
5. 高风险按钮视觉权重断言通过。

#### 前置依赖

1. P3-W2 状态分类完成。
2. P3-W3 关键词字典完成（telegraph 的 tooltip 会用关键词）。
3. `AIPlanSnapshot` contract 扩展合入。
4. `CombatResolutionTrace` / `AttackHintModel` 已有足够信息暴露给 client。

#### 自动化验证

1. `telegraph-consistency-lint`：同 `telegraphKey` 在三处的 iconKey / colorKey 差异 = 0。
2. `golden/p3-w4/` × (战斗三层 / boss telegraph / 普通 intent / observe 后按钮变化) × zh/en。
3. `ClientSmokeHarnessTest::combatDecisionLayers`。

#### 风险与兜底

1. 风险：`AIPlanSnapshot` 暴露"下一回合意图"可能泄露规则信息，破坏"涌现"的乐趣。兜底：支持难度分级——低难度全可见，高难度只显示意图类别（攻/守/特）而非具体伤害。
2. 风险：高风险预警导致玩家过度保守。兜底：预警只标高亮不拦截，仍可选择风险动作。
3. 风险：观察结果反向影响按钮解释需要规则层给新接口。兜底：先让 observe 只追加一条日志 `typed hint`，UI 消费这条 hint 注入按钮 subtitle；第二阶段再做深度整合。

#### 最小切片

三层面板 + Boss telegraph 三位一体先发。普通敌人 intent + observe 反向影响可延到 P3-W4.1。

#### 工作量

3 周（所有 W 中最重）。

---

### 5.9 §9.9 长期候选 | 补充触发条件与前置依赖

现版 §9.9 列了 5 条但没有触发条件和前置依赖，建议每条补足：

| 候选 | 前置依赖 | 触发条件 | 预估价值 |
| --- | --- | --- | --- |
| Look Mode 完整增强 | W5b、P3-W3 | 玩家反馈"检视信息不够" 或 Codex 条目数 > 200 | 高（CoQ 级别能力） |
| Autoexplore / Autotravel | W5b、房间数 > 5/局 | 玩家测试反馈空房间移动疲劳；或 pathfinding 已稳定 | 极高（ROI 最高） |
| Advanced Tooltip | P3-W3 | 装备词缀密度 > 4/件 平均 | 中 |
| Death Replay / Turn Log | 存档系统已稳定 | 玩家反馈"死了不知为什么" | 高（post-mortem 学习） |
| Seed / Snapshot Share | 稳定 seed 字符串化 | 社区化诉求出现 | 中长期 |
| **（新增）Tutorial 体系** | W5a、W5b | 新玩家留存率 < 40% | 极高 |
| **（新增）主题切换** | 全量 token 化 | a11y 需求或多显示器用户诉求 | 中 |
| **（新增）a11y 完整支持** | W5.0 | 任何阶段都可推进 | 高（伦理/合规底线） |

---

## 6 · 细节质疑清单（逐条，按文档章节顺序）

以下是 v2 里依然存在的"最细小"问题。按在原文档中的章节顺序列出。

### §3 系列

1. **§3.2 参考输入链接使用绝对路径** `/Users/luo/Documents/github/K-ToME/...`，不可移植。→ 改为相对路径 `../review/2026-04-20-...md`。
2. **§3.6 表中深渊纪事消失**。但 §6.9 又把它列为参考对象之一。→ 建议在 §3.6 表里保留一行"深渊纪事 | 解释型按钮 / 事件房节奏 | 次级参考 | 当前阶段可局部吸收"，保持语义一致。
3. **§3.7 "死亡即元进度"不吸收** 和 §9.9 "Death Replay" 容易混淆。→ 显性区分：元进度 = 跨局持久化升级（Hades 风格）；Death Replay = 单局 post-mortem 学习工具。前者拒绝，后者候选。

### §6 系列

4. **§6.3 "图标不承担独占语义，关键风险仍需有文字说明"** 过于笼统。→ 明确"关键风险"定义，例如：HP ≤ 30%、telegraph 即将命中、禁用按钮、不可逆动作。
5. **§6.5 "ESC / Backspace / F / Tab / ? 统一语义"** 没写具体语义（见本 Review §4.2）。必须固化。
6. **§6.7 动效允许清单的 4 条没有配套时间 token**。→ 建议直接引用 §8.1 MotionToken 的对应档位（例如"回合节拍提示 = fast 120ms"）。
7. **§6.8 design token 只属于 client** 这条原则很对，但没说**如果规则层确实需要"颜色"呢**？比如怪物有"红色外观"属于规则内容。→ 建议补：规则层的视觉内容走 `VisualManifest`，不走 design token；design token 仅用于 UI chrome。

### §7 系列

8. **§7.1 第 1 条"首焦点"** 和 §9.2 W5a 的 4 条子项里没有对应项。→ W5a 补入"首焦点规范"。
9. **§7.2.A 第 5 条"地图掉落 badge"**说"同格掉落件数"作为当前 phase 实现，但没定义 badge 的**图标 fallback**。→ 若多件物品 iconKey 各异，badge 显示哪一个？建议 `显示第一件的 iconKey + 数量 N`；若件数 > 9 显示 `9+`。
10. **§7.2.C 第 3 条 "作用 / 消耗 / 风险 / 禁用原因"** 没说哪些必填哪些选填。→ 建议"作用 + 禁用原因"必填，"消耗 + 风险"选填（由规则层是否暴露决定）。
11. **§7.2.D modal 未说 modal 栈深度限制**。→ 建议最大栈深 3 层（Inventory → Item Detail → Compare），防止无限嵌套。
12. **§7.2.D "关闭后能回到原地图上下文"** 没说焦点恢复是"地图光标位置"还是"上次操作的按键位置"。→ 建议恢复到"打开 modal 前的地图光标 + 主菜单焦点"。
13. **§7.3 第 5 条"本地快照与 seed 能成为分享维度"** 在 §9 里没 W 归属。→ 落入 §9.9 长期候选（已在映射矩阵中标注）。
14. **§7.3 第 6 条 "Inspect 升级为正式 Look Mode"** 和 §9.9 "Look Mode 的进一步增强" 存在歧义。→ 显性区分：W5b 做基础 Look Mode（地图自由光标 + 目标卡同步），W5b 之后做完整版（多层 tooltip 等）。
15. **§7.4 卡片模型 5 个字段没说哪些必填**。→ 建议标题 + 摘要 + 动作按钮必填，其他选填。
16. **§7.4 "事件 / 商店 / 奖励房统一卡片"** 在 §9 里没显性 W 归属。→ 建议落入 W5b 的 `ModalPanelModel`。
17. **§7.5 "区间式预期结果"** 没说若规则层不给区间怎么办。→ 建议：若规则层只给单值，UI 显示单值；若给 typed reason 没给区间，UI 显示 typed reason；两者都有时优先显示区间。
18. **§7.5 "普通敌人的下一步意图，也应逐步从隐藏信息过渡为可见信息"** 没说是"立即全可见"还是"逐步渐进"。→ 建议按难度分级（见本 Review §5.8 风险兜底 1）。
19. **§7.6 "UNIQUE / ARTIFACT 作为 special tier"** 没说视觉差异。→ 建议：UNIQUE 用品质橙色 + 普通边框；ARTIFACT 用品质红色 + 发光边框；两者都有独立 iconKey 命名空间。
20. **§7.6 第 4 条拒绝 `SET / LEGENDARY / MYTHIC`** 很好。→ 建议在 `UiDesignTokens.qualityColor` 里对这些未定义品质直接 `throw IllegalArgumentException`，不给静默 fallback。
21. **§7.7 第 2 条 badge 规则 "回合 / 层数 / stack/cap"** 没说并存时怎么展示。→ 建议优先级：剩余回合 > stack/cap > 层数 > 冷却，只显示最高优先级一条。
22. **§7.7 第 3 条"同 icon / 同主色 / 同语义"** 没说 `iconKey` 如何保证三处一致。→ 建议在 `TelegraphRenderer` 中央管理，三处都从它读。
23. **§7.8 第 1 条"资源和属性数字优先等宽"** 没说哪些不用。→ 建议正文描述文本不用等宽，避免中英混排时视觉僵硬。
24. **§7.8 第 5 条"战斗节拍词独立强调层级"** 没给具体样式。→ 建议字号 +2pt、字重 +100、0.3s scale pop 动效、独立色 token `--battle-beat`。

### §8 系列

25. **§8.1 token 载体"文件后续决定"** 太模糊。→ 建议直接锁 Kotlin 类（`UiDesignTokens.kt`），因为 json 在 libGDX 下需要额外 loader 且编译期不校验。后续需要动态切主题时再切 json。
26. **§8.2 5 个 view model 没说依赖关系**。→ 建议显性依赖图：`LogPresentationModel` 独立；`PlayerCardModel` / `TargetCardModel` / `ActionPanelModel` 独立但共享 `StatusBadgeModel`；`ModalPanelModel` 可引用前四者。
27. **§8.3 iconKey 消费矩阵 "日志前缀 - 基本缺失 - 建议增加轻量 icon 前缀"** 没说哪些日志类型加。→ 建议 danger / reward / status change / combat hit / loot 五类，其余不加。
28. **§8.4 按钮基座 7 个字段没说必选**。→ 建议图标 + 标题 + 热键 badge 必填（热键若无则显 `·`），其余选填。

### §9 系列

29. **§9.1 "重拍最小 HUD / 空态 / locale golden"** 没说 golden 覆盖的分辨率。→ 建议单一分辨率 1600×900（对应大多数笔记本）。
30. **§9.2 W5a 4 条子项缺"首焦点"**（见 §4.1 矩阵）。→ 补入。
31. **§9.3 W5b 没说 sidebar 是否保留**。→ 建议 W5b 保留 sidebar 作为 Character 面容器，不强行拆栏。
32. **§9.4 W5c 没说"contract-lint 失败策略"**。→ 建议先 warn 一周，再升 error。
33. **§9.4 W5c "地图掉落提示进入 world surface"** 没说和现有 `MapCellSnapshot.items` 的映射。→ 建议：`MapCellSnapshot.items.firstOrNull()?.iconKey` 作为 badge icon，`items.size` 作为数量。
34. **§9.5 W6~W7 "不允许随着内容膨胀退化成纯文字列表"** 没说检测方法。→ 建议 `content-ui-lint` 断言每件新内容至少有一个 iconKey 引用。
35. **§9.6 P3-W2 "状态分类"** 没列出分类枚举。→ 建议文档里列 5 类（BUFF / DEBUFF / PERSISTENT / ZONE / TELEGRAPH）作为规则层立法目标。
36. **§9.7 P3-W3 "关键词进入主要阅读路径"** 没说"主要阅读路径"具体是哪些面板。→ 建议：检视面板、战斗动作 tooltip、装备对比框三处。
37. **§9.8 P3-W4 "观察 / 侦察结果反向影响按钮解释"** 没说数据流路径。→ 建议在本 Review §5.8 中已建议"observe 追加 `typed hint` 日志，UI 消费"。
38. **§9.9 5 条候选没前置依赖和触发条件**（见 §5.9 补全表）。

### §11 / §12 系列

39. **§11.1 "golden screenshot zh-CN / en-US 双语"** 没说 golden 的"微小像素差异"容忍度。→ 建议 `±2px anti-aliasing diff tolerance`，超过触发 review。
40. **§11.2 白盒验收 7 条没说"谁来做"**。→ 建议开发者自测（每个 W 结束）+ 1 名独立 reviewer 走查（W5.0 / W5b / P3-W4 这三个关键节点）。
41. **§11.3 指标 1 "不得出现裸字符串或明显爆版"** 不可量化。→ 改为 "`locale-lint` 报 0 裸字符串 + golden 无 bounding box 溢出"。
42. **§11.3 指标 2 "日志可见区不得被低价值消息淹没危险提示"** 不可量化。→ 改为 "连续 20 回合战斗样本中，危险类日志在日志可见区平均存活 ≥ 3 回合"。
43. **§11 整章缺 a11y / 性能 / telemetry 验收面**（见本 Review §4.4 / §4.7 / §4.8）。
44. **§12.1 高风险点 8 条，但没覆盖"动效预算"**（前两轮 review 已提）。→ 补入 "为了动效效果牺牲 60fps 目标"。

---

## 7 · 文档结构层面的改进建议

### 7.1 新增章节建议

1. **§9.0 蓝图—规划映射矩阵**（见本 Review §4.1）。
2. **§6.10 键位统一语义表**（见本 Review §4.2）。
3. **§6.11 可访问性基线**（见本 Review §4.4）。
4. **§6.12 音效作为信息层**（见本 Review §4.5）。
5. **§7.9 错误态 / 空态 / 加载态**（见本 Review §4.3）。
6. **§11.4 性能验收**（见本 Review §4.7）。
7. **§11.5 观察口与埋点**（见本 Review §4.8）。
8. **§13 术语表**：统一定义 "信息面" / "解释型按钮" / "构筑标签" / "节拍词" / "叙事块" 等重复出现的术语。

### 7.2 可选章节调整

1. §4.1 / §4.2 / §12.2 合并为一张"已完成 / 待做 / 不做"三态总览表，避免分散。
2. §3.6 行业参考表补充"次级 / 打磨 / 拒绝"三列分类，和 §1 核心参照 ↔ §2 次级 ↔ §3 打磨 的深度分层对齐。
3. §10 建议文件落点扩成"文件 × 负责的蓝图条目 × 负责的 W"的矩阵，方便工程师认领。

### 7.3 工程细节

1. 所有绝对路径链接改相对路径（已在 §6 问题 1 提出）。
2. 建议文档加版本号（`v2.0 2026-04-20` / `v3.0 ?`），每次大改写新建 PR 而不是静默覆盖。
3. 建议文档末尾加"变更日志"（Changelog）区块，记录每次主要改版的取舍。

---

## 8 · 优先级建议（若文档 v3 要做）

若本 Review 的建议要落入 v3，建议按以下优先级处理（从高到低）：

### P0（必须吸收，否则规划层不能执行）

1. §9 每个 W 补齐 Exit Criteria / 前置依赖 / 产出物 / 验证点 四项（见本 Review §5）。
2. §9.0 蓝图—规划映射矩阵（§4.1）。
3. §6.10 键位统一语义表（§4.2）。
4. §7 / §8 里的必填/选填字段补全（§6 细节问题 10 / 15 / 28）。
5. §9.9 长期候选的前置依赖 + 触发条件（§5.9）。

### P1（高价值，建议尽早吸收）

1. a11y 基线 §6.11（§4.4）。
2. 错误态 / 空态 / 加载态 §7.9（§4.3）。
3. 性能验收 §11.4（§4.7）。
4. Onboarding / Tutorial 体系（§4.6）落入 §9.9 长期候选。
5. 细节问题 1 ~ 24 逐条修订（§6）。

### P2（锦上添花）

1. 音效作为信息层 §6.12（§4.5）。
2. 观察口与埋点 §11.5（§4.8）。
3. Review gate / PR checklist §9.10 或 §13（§4.9）。
4. 术语表 / 变更日志（§7）。
5. 细节问题 25 ~ 44（§6）。

---

## 9 · 结论

v2 相对 v1 是**质变级别的进步**：从"像不像某个网站"升级到"K-ToME 自己的设计哲学选型 + 介质差异化策略 + 四面信息架构 + 分阶段落地"。前两轮 review 的核心建议基本被吸收。

但作为"执行手册"，v2 依然有三个结构性不足：

1. **§9 规划层没有 Exit Criteria / 依赖 / 产出物 / 验证点 四件套**，每个 W 都还只是纲要。工程师拿到现文档**仍然不能直接排期**。
2. **§7 蓝图和 §9 规划缺映射矩阵**，导致 7 条以上蓝图目标"没有显性 W 归属"。
3. **§6 原则缺"统一语义表"、a11y 基线、音效层、错误态**四个维度。

另外 40+ 个细节问题散落在 §3 ~ §12 各章节，逐条可修复。

若要把本文档升级为真正的"执行手册"，P0 建议**必须**进入 v3，否则后续实施会在"每个 W 具体做什么 / 做完算不算完 / 谁来验证"这些基本问题上反复对齐，吃掉大量团队协作成本。

**下一步建议的 3 个最优先动作**：

1. §9 整章按本 Review §5 模板改写。
2. 新增 §9.0 映射矩阵。
3. 新增 §6.10 键位语义表。

完成这三件事，v3 就能从"设计文档"真正升级到"可执行蓝图"。

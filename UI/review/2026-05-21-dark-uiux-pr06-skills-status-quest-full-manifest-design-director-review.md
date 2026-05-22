# Dark UI/UX PR06 设计总监 / 系统策划 / 玩法体验深审

**目标文档**：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`
**审查立场**：资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
**审查日期**：2026-05-21
**互补关系**：本 review 不重复 `2026-05-09-dark-uiux-pr06-pr-level-standard-rereview-round2.md` 已经覆盖的 PR-level 工程合同 / gate / artifact 维度问题；聚焦于「PR06 作为玩家可见 UI 收口 PR」在玩法体验、视觉信息层次、系统策划 disposition、Roguelike 长会话使用模式与跨家族同屏一致性方面的风险。

> 重要前置：PR06 的 gate / inventory / exclusion / coverage schema 接线在 round2 review 之后已显著收口，本文档不再重审这些合同。本文档只覆盖**那些 coverage 全绿也无法发现的设计 / 玩法层缺口**。

---

## 0. 总评 (TL;DR)

PR06 文档作为"暗色风格 manifest 全量收口"已经把工程侧的合同打磨得相当严密：final-full inventory 冻结、frozen profession per-key exclusion、quest icon 最小路径、validation overlay row/line budget、PR-03/05 handoff 精确路径，全部从模糊散文升级到可机械执行的合同。

但站在**游戏开发设计总监**的视角，PR06 仍有 8 类属于「玩法体验 / 系统策划 disposition」层面的高优风险尚未在文档里冻结，且 §8 人工白盒清单与现有 Acceptance Matrix 都不足以让这些风险在 PR06 close 之前暴露。这些问题的共同特征是：

1. 它们都是「coverage 全绿、focused test 全绿、golden 全绿」**之后**才会暴露的；
2. 它们一旦留到 PR07 才发现，PR07 已经不是资源 PR，没法承接；
3. 它们影响的是玩家在战斗 / 探索 / 成长 / 验证四个核心循环里的**关键反馈通道**，不是装饰瑕疵。

最高风险三项：

- **§2 Status icon `mapNotNull` 静默丢弃**：在 Roguelike 高频死亡 / 高频状态叠加场景里，玩家失去最关键的战斗反馈通道，**且 PR06 §6.2.1 写了 null 不能为合法终态，但并未要求修掉当前 resolver 的 `mapNotNull`**（只补测试）。
- **§3 Quest icon 降级为单 generic marker**：`activate / progress / complete` 三类 token 共用 `icon.quest.objective_marker`，玩家信息密度从"可区分"退到"只知道有任务"。如果这是有意的简化，必须配套显式 follow-up；不能在 PR06 close 时让 generic marker 看起来像"任务可视化已完成"。
- **§4 Talent row 4 态主 cue 只剩 badge + tone**：从 ASCII marker 退到纯视觉，违背"不能仅靠颜色传达状态"的内部原则，且 32px badge 在色弱玩家 / 低对比显示器 / 户外场景下塌成 2-3 态的风险未在文档里管理。

下面分节论证。

---

## 1. 战斗反馈链：status / mutation / damage_type 的同屏混淆

### 1.1 `StatusIconResolver.mapNotNull` 在 PR06 后仍是 silent drop

证据锚点：

- `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt:19-33` 当前实现：

```kotlin
StatusPresentationBuilder
    .sorted(effects.map(StatusPresentationBuilder::build))
    .mapNotNull { presentation ->
        presentation.iconKey?.let(visualResolver::resolve)?.let { asset -> ... }
    }
```

- PR06 §6.2.1 已声明 "正式 status / mutation presentation 的 `iconKey` 不得为 null；缺失时必须进入 final inventory 的 missing/fallback 路径"。
- PR06 §2 影响范围把 `StatusIconResolver.kt` 列入"确认 status icon key 覆盖"，但**没有把"移除 mapNotNull silent drop"列为本 PR 必须改的代码契约**。

设计影响：

1. Roguelike 玩法里玩家依赖 status icon 决定行动节奏（中毒 → 拉距离；眩晕 → 蓄力；buff 即将到期 → 主动消耗）。
2. ToME 类游戏单回合常态叠加 4-7 个 status；如果其中任意一个 icon resolve 失败而被 silent drop，玩家会以「该状态已结束」误判，做出错误决策。
3. silent drop 同时也让 QA / reviewer 完全看不到资源缺口，因为 HUD 看起来"干净"。
4. PR06 close 后即便 `darkManifestCoverageLint final-full` 全绿、`StatusIconResolverTest.unknownStatusIconUsesRegisteredFallback` 全绿，**runtime 仍可能 silent drop**：因为现 resolver 在 `presentation.iconKey == null` 这一支根本不调 fallback，新测试只覆盖 `resolve` 返回 null 那一支。

修复方向（PR06 必须在 §2 / §6.2.1 显式冻结）：

1. 把 `StatusIconResolver.resolveIcons` 的实现合同写进 PR06：`presentation.iconKey == null` 必须进入 documented fallback 或显式 sentinel，不允许从 list 里消失。
2. `StatusPresentationBuilder.buildZoneEffect` 当前 `iconKey = null` 是已知合法路径（`StatusPresentationModel.kt:52-63`），但 PR06 必须明确：zone effect 在 PR06 后是「不进入 status icon 行」（路由到独立 row），还是「分配 explicit `icon.status.zone_effect`」。不能让 zone effect 永远以 silent drop 形式存在却又不出现在分母里——这正是 final-full 分母看不见的"暗洞"。
3. §8 人工白盒至少加一项"故意制造 3 个 unknown status key + 1 个 zone effect"的复合 case，验证「丢失 icon 时玩家依然能从 HUD 数到全部 status 数量」。

### 1.2 `icon.status.*` 与 `icon.mutation.*` 双前缀的视觉差异化未冻结

证据锚点：

- PR06 §3.5：「`r09-status-damage` 同时覆盖 `icon.status.*` 和 `icon.mutation.*`；不新增 `icon_mutation` category。」
- design notes §4 把 status 和 mutation 都归到「状态家族」，但 mutation 在 ToME-likes 中代表"永久性变异 / 体质变更"，与短时 status 在玩家心智中是两个层级。
- PR06 没有规定 mutation icon 在视觉 grammar 上必须与 status 区别（外框形状 / accent color / 持久性 cue）。

设计影响：

1. 共用同一前缀家族 + 共用同一 sheet `r09-status-damage` + 没有 visual separation rule = 它们必然会在 32px 下混淆。
2. 玩法上 mutation 不可主动驱散（或代价高），status 多数可解；玩家如果视觉无法区分，会在「能否解除」这一关键策略点上反复试错。
3. design notes §4.2 separation rules 表里**漏了** status vs mutation 这一组——这是 PR06 开 sheet 之前就应该补上的设计规则。

修复方向：

1. 在 PR06 §3 资源范围里增补一行 mutation 的 shape grammar 描述，例如"mutation 使用闭合框 + scar/etching 母题；status 使用 open badge + single condition glyph"。
2. 同步在 design notes §4 separation rules 表补 status vs mutation 行；或 PR06 §6 明确"本 PR 不区分 mutation 和 status visual grammar，且接受由此带来的体验风险"，作为已知 trade-off 写进 remaining risk。

### 1.3 `icon.damage_type.*` 在 Acceptance Matrix 上的体验权重不足

证据锚点：

- PR06 §6.1 表把 damage type 的 required consumer/test 写为 "`ManifestResolveTest` or status/combat focused test"（"or" 是关键）。
- §7 focused tests 命令没有任何 damage type 相关 test。
- Acceptance Matrix UI06-M04 source = "status / quest / skill presentation"，没有 damage type。

设计影响：

ToME-style 战斗策略核心之一是"对抗特定 damage type 抗性 / 易伤"。在装备 tooltip、技能预览、status 解释、伤害浮字四处都会出现 damage type icon。如果该家族只做了 manifest resolve 而没有"player-visible consumer 上屏证明"，PR06 close 后大概率出现「装备 tooltip 显示新 dark-v1 火元素 icon，技能预览仍残留旧 vector icon」这种跨时代 collage。

修复方向：

1. §6.1 把 damage type 行的"or"改为"and"，至少要求一处 player-visible consumer test 上屏；可选 consumer：item tooltip / skill preview / status detail 任一。
2. §6.4 同屏一致性验收里把 damage type 显式加入"职业树、HUD、背包、状态栏同时出现时图标风格一致"清单（当前只列了四类，没有 damage type）。
3. §8 人工白盒第 2 项"触发至少 3 类状态和 2 类技能预览"——加一句"且确认伤害浮字 / 技能预览 / 装备 tooltip 中的 damage type icon 视觉一致"。

---

## 2. Quest icon：从"信息密度可区分"降级到 generic marker 的体验代价

### 2.1 单 key 兜底 vs typed quest icon 的取舍

证据锚点：

- PR06 §6.2.3 「PR-06 固定最小路径为 generic `icon.quest.objective_marker`，不扩 `RenderSnapshot`、`WorldProgress` 或 quest schema。」
- PR06 §6.2.4 mapping 表：`log.objective.activate / progress / complete` 三类 token 全部 → `icon.quest.objective_marker`。
- PR06 §6.2.5 「`icon.quest.armory_key`、`icon.quest.seal_key` 只能用于真实钥匙/目标语义，不能为了"有图标"硬塞到所有 objective log。」

设计影响：

1. 三类 token 共用一个 marker 意味着玩家从"图标"获取的信息只有「有任务进展」这一比特，没有"是激活 / 进度 / 完成"的状态比特。
2. 在 Roguelike 单局快节奏里，玩家几乎不会有时间去读对应文本行；图标是唯一可被瞥见的信号。降到 generic marker 直接削弱该信号。
3. `icon.quest.armory_key / seal_key` 这类 typed quest icon 已经存在但 PR06 没有任何强制路径把它接到 consumer——意味着它们永远是孤儿 manifest key，存在却不上屏。这是一种"形式上覆盖、实质上失效"的资源浪费。
4. design notes §3.1 明确写"quest marker is intentional objective"，与 status badge / damage type 必须 visually distinct；PR06 把 generic marker 当唯一接线，实际上把"objective 的方向 / 类别"语义直接抹平。

修复方向（按优先级降序）：

1. **首选**：在 PR06 §6.2 里冻结一份更细的 token→icon mapping，例如：
   - `log.objective.activate` → `icon.quest.objective_marker`
   - `log.objective.progress` → `icon.quest.objective_marker`
   - `log.objective.complete` → 显式 complete variant（可以是同 icon + green tone overlay，由 row tone 控制，不引入新 manifest key）
   - 涉及钥匙 / 封印的 typed log token → `icon.quest.armory_key / seal_key`
   - 该 mapping 由 `QuestSummaryIconResolver` 持有；新增 typed token 失败时回落到 generic marker。
2. **次选（若 PR06 必须卡住范围）**：在 §6.2 显式补一段"已知体验降级"说明，并把"PR07 或单独 PR 引入 typed quest icon mapping"作为命名的 follow-up，写进 `UI/PLAN.md` 与 PR06 PR 描述。
3. **不可接受**：让 generic marker 接线保持原样并在 PR07 静默扩展。typed quest mapping 是面向玩家的合同变更，不应该藏在"polish" PR 里。

### 2.2 空状态 / 满状态的视觉一致性

证据锚点：

- PR06 §6.2.6「空状态保留 `ui.shell.quest.none` 文案；空状态不消耗 quest icon，不计入 quest icon 上屏证明。」
- design notes §3.1 推荐空状态依然需要"objective frame"作为视觉占位以维持 HUD 节奏。

设计影响：

PR06 把空状态做成纯文本（无 icon），与有 icon 的非空状态视觉权重不一致。HUD 任务行在两种状态间切换时会出现"行高保持 / icon 位置突然消失或出现"的抖动观感。

修复方向：

1. 空状态保留一个 muted "no objective" placeholder icon（不计入 quest icon 上屏证明、不进入 final-full 分母，但视觉上保留 slot）；或
2. 在 row layout 上固定 icon slot 宽度，无 icon 时填透明占位 sprite。

§8 人工白盒应加一项「在 quest 完成 → 无任务 → 接受新任务」过渡序列里目测行抖动。

### 2.3 `dark-uiux-pr06-status-quest-skill-overview` 同屏证据未被强制为 blocking artifact

证据锚点：

- PR06 §6.2.8 写 "Golden `dark-uiux-pr06-status-quest-skill-overview` 必须同时展示 status icon、skill/talent icon 和 quest marker/icon；只出现文本不算通过。"
- Acceptance Matrix UI06-M04 的 artifact 列只到 `client/build/reports/golden/`，没有点名这张同屏 overview。
- §6.4 验收"职业树、HUD、背包、状态栏同时出现时图标风格一致"是文字要求，**没有任何 gate 验证**。

设计影响：

同屏 overview screenshot 是本 PR 最重要的「玩法体验一致性证据」——它是唯一能反映"coverage 全绿但视觉不一致"的artifact。如果它不是 blocking artifact，reviewer 在 PR 描述里只看到 coverage report + 单家族 golden，就会盖章 close，而真正的同屏冲突要到 PR07 才暴露。

修复方向：

1. Acceptance Matrix UI06-M04 artifact 列显式加 `client/build/reports/golden/dark-uiux-pr06-status-quest-skill-overview.png`，且 manualEvidence 列写明它必须出现在 PR 描述里、不只是 manual record。
2. 或者新增 UI06-M08 = "同屏视觉一致性合同"，artifact = `dark-uiux-pr06-status-quest-skill-overview` golden + `dark-uiux-pr06-overview-screenshot` 人工白盒截图；fastCheck 为 `goldenScreenshot`，ownerGate 为人工签字。

---

## 3. Talent row 4 态主 cue 从 ASCII marker 退到 badge + tone：可访问性风险

证据锚点：

- PR06 §4.7：「PR-06 可以在 debug trace 或 accessibility metadata 中保留 `[x]`、`[+]`、`[r]`、`[*]`，但这些 ASCII 前缀不得出现在正式 talent row 可见文本里；primary dark visual cue 必须来自 state badge 与 tone」。
- design notes §6.3 talent row 视觉规则建议把 marker 作为 row grammar 的稳定组件（`indent + connector + state marker + skill icon + talent name + rank`）。
- ktome-dark-ui-design §4 color discipline 第 6 条：「State 必须不被颜色单独传达；使用 marker、icon、文本、位置或边框作为第二通道。」

设计冲突点：

1. PR06 §4.7 把 marker 从可见文本里移除，主 cue 仅剩 state badge + tone。
2. 但 ktome-dark-ui-design 自己规定 "状态不能仅靠颜色传达"。
3. badge shape 在 dark-v1 风格下要做出 4 个一眼可分的 silhouette（locked / learnable / reserve / active）已经很挑战；32px 缩略 + dark fantasy 低饱和 + 4 态轮换 + focus highlight 叠加 ≈ 状态识别衰减到 2-3 态。

设计影响：

1. 玩家在 talent build 阶段必须能区分「可学（learnable）」「未达预备（locked）」「已学但未激活（reserve）」「已学激活（active）」四态——这是 ToME-likes 成长体验的核心读屏动作。
2. 色弱玩家（红绿色盲 ≈ 男性 8%、女性 0.5%）在 dark fantasy 风格的 muted palette 下，`talent-locked #59616C` 与 `talent-reserve #D99A2B` 在低对比度场景中可能塌成一对，`talent-learnable #1CB7C8` 与 selected/focus 的 cyan edge 也可能塌成一对。
3. 户外 / 低亮度显示器 / 长时间游玩疲劳期都会进一步压缩这种区分能力。
4. PR06 §4.6 已经明文把 selection marker 和 state badge 分离到不同 manifest key，但仍未规定"selection state 不得覆盖 state badge 颜色优先级"。

修复方向：

1. **强烈建议**保留 marker 作为 secondary cue，且位置必须出现在 player-visible 区域（不只是 accessibility metadata）。具体可选：
   - 在 badge 内部叠加 1-char glyph（`x / + / r / *`，且 glyph 字色由 tone 决定）；
   - 或在 row layout 里把 marker 紧贴 badge 之前一个 char 宽度的位置，使其与 talent name 隔离；
2. 若坚持只用 badge + tone，PR06 必须在 §4 增加：
   - 每个 state 的 badge silhouette 必须 32px 下完全 distinguishable（不依赖颜色），并以 contact-sheet QA 单独验证；
   - selected/focus highlight 不得覆盖 state badge 的位置、形状或主色；
   - §8 人工白盒新增"色弱模拟（protanopia / deuteranopia / tritanopia）三种 filter 下 4 态全部可分"的检查。
3. Acceptance Matrix UI06-M02 manualEvidence 当前写 `dark-uiux-pr06-talent-icon-rebaseline`——建议补一项 `dark-uiux-pr06-talent-state-color-blind-snapshot`，使色弱回退路径成为 PR06 close 的硬证据。

### 3.1 这是不是过度设计？不是。

读者可能反驳"K-ToME 当前没有承诺色弱友好"。但这里的论点不是"为了色弱"——核心论点是：

```text
state 由"marker"携带时，玩家心智的 read cost 是 O(1)；
state 由"colour + badge shape"携带时，read cost 在 32px + dark palette 下是 O(2-3)，
且依赖玩家显示器、光线、疲劳。
```

PR06 砍掉 marker 是用"玩家持续读屏成本上升"换取"row 视觉更干净"。这是一笔体验交易，必须**在文档里以"已知 trade-off"形式承认，并配套白盒覆盖度，而不是当成默认升级**。

---

## 4. Validation overlay 的双重定位：debug 工具 vs player-facing UI

证据锚点：

- PR06 §1.5：「将验证模式运行时 overlay、active content pack summary、scenario evidence summary 的玩家可见 presentation 纳入 dark-v1 全量覆盖；setup 页 layout 仍归 PR-01，最终证据归 PR-07。」
- PR06 §6.3 给出 12 行 / 96 字符 / section order 等 row budget 合同。
- 当前 `ValidationPackSummaryText.kt:7-68` 是"散点 leaf formatter"，没有统一 budget owner。
- ktome-dark-ui-design §2.1.4 把 "validation overlay and active content pack summaries" 列为 player-visible 必备 surface。

设计冲突点：

1. validation overlay 同时承担两个用户故事：
   - A. **mod / content pack 开发者** 调试时需要看到尽量完整的 evidence path 列表（10+ active packs / 多个 namespace / 几十条 touched content id）。
   - B. **普通玩家** 在 debug client 误打开 validation mode 时，overlay 不应遮挡 HUD。
2. 12 行 / 96 字符 budget 是为 B 优化的，会把 A 的关键 evidence 推到 `+N more` fold 后。
3. CJK 玩家的 96 字符在等宽字体下视觉宽度可达 192 列；fold 行为对 CJK 用户提前触发。

设计影响：

1. 现实场景下 mod 开发者会习惯性把 overlay 当真源去看 evidence path——一旦发现 evidence 被 fold，他们要么开始切回 raw report 文件（脱离 in-app 工作流），要么 PR-after-PR 反复要求扩大 budget。
2. 普通玩家场景下，12 行其实仍可能与战斗 HUD 重叠（游戏内 HUD 区域常常压到 12-15 行），budget 设小并未真正解决遮挡。
3. PR06 §6.3 "section order" 写死 preset → seed → zone/floor → active packs → namespaces → overlay ops → touched content → key warnings → evidence summary，**意味着 key warning 与 evidence summary 在 fold 后会被推到看不见**——而这两项才是开发者最关心的项。

修复方向：

1. PR06 必须显式承认 overlay 的 **双 mode** 设计：
   - `validation.overlay.compact`：12 行 / 96 字符 budget；仅出现在 in-run（玩家误进入 validation 模式）；
   - `validation.overlay.detailed`：无 12 行硬上限（或显著放宽到 32 行），按 evidence path 自然展开；仅出现在 ValidationSetupScreen / dedicated validation panel。
2. 若不做双 mode，必须把 section order 调整为「先 key warnings、evidence summary，再 active packs、namespaces 等」——把开发者关心的项放到 fold 之前。
3. PR06 §6.3 的 96 字符上限改为 column-budget（按渲染列宽度，CJK 字符记 2 列），或针对 CJK 提供 144 字符回退；不再以 char count 作为合同单位。
4. §8 人工白盒新增"用 6 个 active pack + 4 个 namespace + 12 个 touched content + 8 条 evidence path 触发 overlay，确认 evidence summary 没被 fold 折掉"。

### 4.1 ValidationOverlaySummaryPresenter 的 owner 边界仍嫌窄

PR06 §6.3 已经把 `ValidationOverlaySummaryPresenter` 写成 row budget 唯一 owner，但**没有规定** presenter 是否拥有「fallback warning 分栏」「path compaction」「token-边界 truncation」全部行为。当前 `ValidationPackSummaryText.kt` / `ValidationScenarioEvidenceSummaryLines.kt` 是各自实现，如果 PR06 落地时 presenter 只接管"行数控制"而把字符 / 路径压缩留在 leaf，所有 round2 review 担心的"多处第二套 compact 逻辑"会再次出现。

建议在 PR06 §6.3 把 presenter 的 owner 范围写成显式表：

| Responsibility | Owner |
| --- | --- |
| row count budget | `ValidationOverlaySummaryPresenter` |
| line length budget (column-aware) | `ValidationOverlaySummaryPresenter` |
| path compaction & repo-relative enforcement | `ValidationOverlaySummaryPresenter` |
| stable sorting | leaf formatter |
| section order | `ValidationOverlaySummaryPresenter` |
| fallback warning column separation | leaf formatter |

---

## 5. Frozen profession locked card：产品信号风险

证据锚点：

- PR06 §3 职业范围：`shadowblade / warden` 是 Frozen excluded；锁定职业卡可见时使用 `visibleFallbackKey` 的 dark-v1 fallback。
- §3 frozen profession exclusion schema 字段：`removalOwner = PR-07 or explicit future PR if frozen profession becomes playable`、`expiresAfterPr = PR-07 unless a later playable-profession PR is named`。
- 当前 repo 中没有 frozen profession 解冻路线图。

设计冲突点：

Roguelike / class-driven 游戏的玩家心智：

```text
看到一张"锁定 / Coming Soon"卡 →  "这是付费 / 后期解锁内容"。
看到一张"灰掉但点不开"的卡 →  "我没满足条件，可以查为什么"。
看到一张"完全消失"的卡 →  "这职业不存在"。
```

PR06 当前路径让 frozen profession 落在第二档（灰掉但点不开），但实际上 K-ToME 的 frozen profession 是「未开发完」而非「玩家未达成条件」。这会持续误导玩家在 build 论坛 / 探索中花费时间寻找解锁条件，最终发现「条件不存在」会产生强烈挫败感。这是 ToME-like 社区在 0.x → 1.x 过渡中真实发生过的事。

设计影响：

1. 影响新玩家 onboarding：profession 选择是早期决策点，挫败感累积成留存损失。
2. 影响 wiki / 攻略生态：玩家自发整理"未开放职业列表"会比官方信号更显眼。
3. 影响 PR07 的 close：frozen 卡如果继续可见，PR07 的"final all-screen polish"也无法解决产品信号歧义。

修复方向（按优先级降序）：

1. **首选**：明确在 profession 选择 UI 隐藏 frozen profession（不出现在 player-visible 路径），仅保留 frozen exclusion 作为内部 manifest 状态。
2. **次选**：locked card 上必须叠加显式文案，例如 `ui.profession.frozen.label = "未开放"`（或 "Development Preview"），与 "locked by requirement" 区分；同时 locked card 的 ember accent 必须收敛，避免被读成"高价值锁定内容"。
3. **不可接受**：保持现状但用 dark-v1 fallback 美化 locked card 视觉——这会让卡片更精致，反而强化"完整版"误导。

PR06 §3 frozen exclusion schema 应增补：

| Field | Rule |
| --- | --- |
| `playerVisibility` | one of `hidden` (preferred), `locked-with-coming-soon-label`, `locked-with-fallback-only` |
| `productMessagingKey` | locale token for the "未开放" / "Development Preview" label, required when `playerVisibility != hidden` |

---

## 6. Fallback / Missing / Debug 视觉的悖论

证据锚点：

- design notes §5「Missing/fallback ... too polished, too bright, or too crude for dark-v1」——美学窗口很窄。
- design notes §6.6 Variant E 描述："cracked iron frame, empty shadow center, broken rune, muted warning edge, clearly unresolved but still polished enough to belong in the UI."
- PR06 §6 Fallback inventory 表把 `missing_visual` / `tile.hidden` / `category=debug` / debug-only tiles / PR-03/05 rejected cells 五种语义统统映射到 `r09-fallback-debug`（或 `r09-rejected-polish`）单张 sheet。

设计冲突点：

1. 五种 disposition 在玩家心智里是完全不同的东西：
   - `missing_visual` = "资源缺失（开发问题）"；
   - `tile.hidden` = "你发现了一个隐藏入口（玩法奖励）"；
   - `category=debug` = "这是开发用资源（不应被玩家看到）"；
   - PR-03/05 rejected cells = "返修中的 player-visible 资源（暂时合法）"。
2. 设计要求 fallback "polished enough to belong in UI" 又 "clearly unresolved"——在 dark fantasy 风格里，`cracked iron / broken rune / muted warning` 本身就是合法装饰元素（dungeon decor / cursed item）。玩家无法靠 visual 单独区分"装饰"和"missing"。
3. 五种语义同一张 sheet 意味着它们共享 shape / lighting / outline，差异化只能靠 accent color——而 PR06 §6 又规定 color 不是唯一通道。

设计影响：

1. 真正的"资源缺失"被读成"装饰"，开发者再也无法靠玩家反馈定位 manifest gap。
2. `tile.hidden` 被读成"missing"，反而削弱玩家发现秘密时的成就感。
3. `category=debug` 资源若在 packaged app 被错误暴露，玩家可能把它读成隐藏内容，制造"卡死的彩蛋"社区争议。

修复方向：

1. PR06 §6 fallback inventory 表分裂为至少三类 sub-sheet 或三类 visual rule：
   - `missing_visual` → 显式 textual badge "VISUAL MISSING"（开发模式高对比，packaged app 默认隐藏并以 sentinel sprite 替代）；
   - `tile.hidden` → dark fantasy 合法装饰元素（broken rune 等），与 missing 必须 visually distinct；
   - `category=debug` → packaged app 内强制不可见，development client 内显示明显的 "DEBUG" overlay。
2. PR06 §6.2.x 增加新合同："missing/fallback sentinel 在 packaged app 内必须包含 text 标记（不依赖纯视觉），并附 manifest report path 提示。"
3. design notes §5 应把"clearly unresolved"语义从"视觉差异化"升级为"视觉差异化 + 文本标记"，承认 dark fantasy 风格本身不足以承担"missing"的语义。

### 6.1 §6 表中"debug-only tiles / missing visual sentinel"那一行

证据锚点：

- PR06 §6 fallback inventory 表："不允许 client ASCII renderer 或 ASCII manifest 字段；纯 debug/history resource fallback 可列入 `allowedFallbackKeys` 并说明原因。"

这一行只规定了"不能用 ASCII"，但没规定**用什么替代**。PR06 close 时如果 `missing_visual` 仍然是一张和 hidden / debug 视觉同源的图，玩家依旧无法识别"资源缺失"——只是不再是 ASCII 字符而已。修复方向同上。

---

## 7. 单 PR 承载 8+ 张 sheet 与历史返修：风险耦合

证据锚点：

- PR06 §3：Round 8 三张 skill sheet（`r08-skills-vanguard-berserker / templar-rogue / arcanist-spellblade`）+ Round 9 四张 sheet（`r09-status-damage / quest-zone-profession / fallback-debug / rejected-polish`）+ Round 7 剩余 + Round 2-6 rejected 返修。
- §3.8 已规定"fallback inventory > 64 cells 时拆 `r09-fallback-debug-a/b`"。
- §3 Cross-PR handoff 表强调 PR-03/05 rejected cell 必须由 PR06 接手或单独资源 PR。

设计冲突点：

1. PR06 close gate 是 final-full，**整张分母**必须全部 covered + no pending/rejected。任一 sheet 在 contact-sheet QA 失败 → close gate 阻塞。
2. 单 PR 承载 8+ 张 sheet 意味着任何一张反复返修都会拖延整个 PR06 的 close window；进而拖延 PR07 的 final all-screen polish。
3. §3.8 只规定了"cell 数超 64 时拆 sheet"，**没有规定** "单张 sheet 累积 2+ 轮 contact-sheet rejection 时怎么办"——这是真实落地中更常见的阻塞模式。
4. PR06 没有规定"哪些 sheet 可以延后到独立资源修复 PR"。

设计影响：

1. 现实风险：如果 `r08-skills-arcanist-spellblade` 因 spellblade 子家族 visual 风格争议反复 QA reject，PR06 close 被单一职业拖累；其它 7+ 张 sheet 等待。
2. 这种耦合一旦发生，开发者倾向于"妥协接受 QA-borderline cell"以推动 close——直接违背 PR06 §6.4 "图标风格一致" 的体验目标。

修复方向：

1. PR06 §6 Failure Rule 新增：「若任一 sheet 累计 ≥2 轮 contact-sheet rejection 或两次以上 player-visible cell QA reject，必须立即拆出独立资源修复 PR（命名规则 `r0X-<family>-rework`），不阻塞 PR06 整体 close。该独立 PR 必须在 PR07 之前 merge。」
2. PR06 §3 Implementation Order 第 6 步「Golden and manual evidence」之前插入一个 checkpoint："如果到此为止任一 sheet 仍在 pending/rejected，回到 §6 Failure Rule 第 1 条决定拆 PR。"
3. Acceptance Matrix 增设 `UI06-M09 = sheet QA escalation policy`，owner = `assets`，artifact = "QA rejection round count summary per sheet"。

---

## 8. Color token 多角色绑定：ember-gold / cold-cyan 同屏冲突

证据锚点：

- ktome-dark-ui-design §4：
  - `ember-gold #D99A2B` = 标题 / rare 资源 / **reserve state** / 确认 accent；
  - `cold-cyan #1CB7C8` = focus edge / **learnable** affordance / 选中。
- design notes §5：
  - quest active marker = "objective marker with warmer route/goal accent"（即 ember-gold 系）。
- PR06 §4.6 已显式说 talent row 不要把 skill icon / state badge / selected marker 混进同一 manifest key，但**没有规定** quest marker / talent reserve / rare item / focus / learnable 五者间的 accent 优先级。

设计冲突点：

可能同屏出现的 ember-gold 累加：

```text
talent reserve badge (talent panel)
+ quest active marker (HUD)
+ rare item drop indicator (inventory / ground loot)
+ title text accent (header)
+ confirmation feedback (recent UI action)
```

可能同屏出现的 cold-cyan 累加：

```text
focused row highlight
+ selected slot edge
+ learnable badge (talent panel)
+ telegraph / overlay focus glow (combat)
+ tooltip focus edge
```

设计影响：

1. accent 的"焦点价值"是稀缺资源——同屏 3+ 个 ember-gold 出现时，玩家无法判断"现在最重要的是哪个"。
2. PR06 §6.4 验收只验"图标风格一致"，不验 accent 总量。
3. 这是 dark fantasy palette 自带的体验风险——dark-v1 本身用 accent 替代亮色，accent 的 cost-of-attention 在该风格下非常高。

修复方向：

1. PR06 §4 / §6.4 / design notes §5 至少补一张 accent priority 表：

```text
Ember-gold strength budget (per visible HUD frame):
  - quest active marker:       strength 3 (max one on screen)
  - rare item drop indicator:  strength 2
  - talent reserve badge:      strength 2 (max two visible)
  - title/header accent:       strength 1
  - confirmation feedback:     strength 1 (transient, < 0.5s)

Cold-cyan strength budget:
  - selected slot edge:        strength 3 (max one focus chain)
  - focused row highlight:     strength 2
  - telegraph overlay glow:    strength 2 (max two combat tiles)
  - learnable badge:           strength 1
  - tooltip focus edge:        strength 1
```

2. PR06 §8 人工白盒新增"同屏 ember/cyan 计数"检查：同时打开背包 + 任务 + 天赋 + 战斗 telegraph，目测两类 accent 不超过 4 个总和。
3. 如果不愿在 PR06 落地 accent budget，至少在 design notes §5 增补一段"已知 accent 冲突风险"。

---

## 9. Long-session 视觉疲劳与 dark fantasy palette

证据锚点：

- PR06 §8 人工白盒五条：HUD 风格统一 / 状态技能区分 / fallback 注入 / overlay 长列表 / repo-relative path。**没有任何一条要求长时间体验**。
- Roguelike 玩家单次 run 平均 1-3h，重 build / 高阶玩家可超 4h。
- dark fantasy palette 偏低饱和；ember + cyan 是高对比 accent，疲劳期反差感知会下降。

设计影响：

1. PR06 close 后的玩家可能在 30min - 1h 范围内觉得 icon "看着精致"，但到 2h+ 开始出现 status icon 难以一眼区分 / talent state 颜色判断变慢 / damage type 模糊的现象。
2. PR06 §6.4 验"图标风格一致"是单帧 snapshot；它不能反映"持续读屏疲劳"。
3. 该问题在 ToME / Caves of Qud / Tangledeep 等同类游戏的玩家反馈里反复出现。

修复方向：

1. PR06 §8 新增白盒第 6 条：「至少进行一次 ≥ 60min 连续游戏（战斗 + 背包 + 任务 + 天赋切换），手写日志至少 3 处视觉疲劳点（哪类 icon 开始难以一眼区分、accent 是否过度堆积、长 overlay 是否读屏成本飙升）。日志归档到 `UI/manual-records/dark-uiux-pr06-long-session-fatigue.md`。」
2. Acceptance Matrix 增设可选 `UI06-M10 = long-session readability`，作为 PR07 闭环的输入。

---

## 10. CJK / 多语言下的字符宽度风险

证据锚点：

- PR06 §6.3：「max line length 单行 display text 最多 96 chars；超过按 token 边界 deterministic truncation」。
- design notes §6.3 路径 / token 不能切断 path segment 的文件名。

设计冲突点：

1. CJK 字符在等宽字体下视觉宽度 ≈ 2 × ASCII 字符；96 字符 ≈ 192 视觉列。
2. ValidationSetupScreen / overlay 现有 layout 在 1280×720 debug client 下宽度约 110-130 列，96 字符在 CJK 渲染下会被前一组件挤出 viewport，或者反过来在 ASCII 渲染下大量留白。
3. "不切断 path segment 的文件名"在 CJK / 长 namespace 下尤其难，因为前缀已经吃掉了显示预算，文件名段往往出现在尾部 fold 区。

设计影响：

1. 中文 / 日文 / 韩文 用户的 validation overlay 会出现「关键 evidence 文件名被截断或被 fold」的情况。
2. truncation 算法若以 char 为单位，CJK 会比 ASCII 提前触发截断且截断位置不可预期。

修复方向：

1. PR06 §6.3 把 "max line length 96 chars" 改写为 column-budget：

```text
max line length:
  - 96 visual columns (each CJK char counts as 2 columns, combining marks count as 0)
  - if column-aware width measurement is not implemented, fall back to 144 chars for CJK locale
```

2. truncation 必须保留 path segment 末尾文件名（"keep-tail"模式），即使前缀被压缩到 ellipsis；与"不切断 file name"规则保持一致。
3. §8 人工白盒加 "切换 CJK locale 触发 overlay 长列表" 检查。

---

## 11. Difficulty 扩展性债务

证据锚点：

- PR06 §6.1 表："`difficulty.normal.icon` and any future `difficulty.<id>.icon`"。
- §6 切换表："`difficulty.normal.icon` 切到 dark-v1；未来新增 difficulty 使用 `difficulty.<id>.icon`"。
- dark-v1 风格已经偏 dramatic，难度递增的视觉空间被风格本身吃掉了一半。

设计影响：

1. 未来 `difficulty.hard / nightmare / madness / insane.icon` 如何在 dark-v1 内部做出"难度感视觉升级"未冻结。
2. 常见错误升级路径：更红 / 更血腥 / 更亮——这恰好踩中 dark-v1 anti-pattern（design notes §8）。
3. 正确升级路径：用 material / texture / accent shape 而非颜色饱和度承担难度差异——但 PR06 没有为后续 PR 留下任何指引。

修复方向：

1. PR06 §6 切换表 difficulty 行补一句："未来 `difficulty.<id>.icon` 必须保留 dark-v1 era，不得通过提高红/亮饱和度或加血腥元素来表达难度递增；推荐通过 material 复杂度（plain stone → cursed iron → ember-cracked obsidian）或 accent 形状（无 → 单刺 → 双刺 → 环形）表达难度梯度。"
2. 或者在 design notes 新建 §12 "Difficulty Visual Roadmap"，作为 PR07 之后扩展时的设计输入。

---

## 12. Player-Visible Disposition 矩阵缺失

证据锚点：

- PR06 §6 close 要求"玩家可见 rejected cell = 0"。
- 但"玩家可见"在以下边界场景下定义不清：
  - frozen profession locked card → 可见，但属于"未开放"产品信号；
  - validation overlay → 在 debug client 可见，packaged app 不可见；
  - debug-only tiles → 不该可见，但若 toggled 是否算 player-visible；
  - hidden zone（玩家发现前不可见、发现后可见）；
  - missing visual sentinel → 默认理论上"任何 PR 都不该让它出现"，但作为最后兜底它依然 player-visible。

设计影响：

1. 没有 disposition 矩阵 → reviewer 与开发者在 close gate 时争议「这个 cell 算不算玩家可见」。
2. 历史上 PR-03/05 的 rejected cell 大多卡在此类争议上。
3. PR06 是 dark UI 全量收口 PR，恰恰是这种边界 disposition 必须落锤的节点。

修复方向：

PR06 §6 增设一张 disposition 矩阵：

| Surface / Cell | Player visibility | Coverage 分母归属 | PR06 disposition |
| --- | --- | --- | --- |
| Release playable skill / talent / tree | visible | in | required full dark-v1 |
| Dev playable skill / talent / tree | visible (gated by debug flag) | in | required full dark-v1 |
| Frozen profession card | visible (recommend hidden) | excluded with reason | locked-with-coming-soon-label or hidden |
| Frozen profession skill/talent | not normally visible | excluded with reason | n/a |
| Validation overlay (debug client) | visible | in | compact + dark-v1 |
| Validation overlay (packaged app) | not visible | n/a | n/a |
| Hidden zone tile (pre-discovery) | not visible | n/a | n/a |
| Hidden zone tile (post-discovery) | visible | in | required dark-v1 |
| Debug-only tile (toggled) | not visible by default | n/a | n/a |
| missing visual sentinel (development) | visible (intentional) | excluded with reason | text-marked sentinel |
| missing visual sentinel (packaged) | should not appear | n/a (if appears = PR06 failure) | text-marked sentinel |
| PR-03/05 rejected cells still player-visible | visible | in | r09-rejected-polish |
| PR-03/05 rejected cells not player-visible | not visible | n/a | PR-07 polish |

矩阵作为 §6 close gate 的 disposition 真源；任何 dispute 时优先以此矩阵裁定。

---

## 13. Acceptance Matrix UI06-M04 标题与内容失配

证据锚点：

- UI06-M04 source 字段 = "status / quest / skill presentation"。
- UI06-M04 fastCheck 包含 `TileRenderModelTest` / `TileRendererCanvasTest`，这两个其实是 quest summary row 改造 + canvas 路径测试，不仅是 "presentation"。
- talent / tree icon 切换归 UI06-M02。
- §6.4 "职业树、HUD、背包、状态栏同时出现时图标风格一致"**没有任何 requirementId 直接对应**。
- §6.2.8 同屏 overview golden 是 §6.2 的子要求，没有进入 Acceptance Matrix。

设计影响：

1. reviewer 用 Acceptance Matrix 做 checklist 时，会漏审"同屏视觉一致性"——它是 PR06 体验目标的关键之一。
2. UI06-M04 标题与 fastCheck 不闭合，未来若拆解 quest summary 与 status presentation 时，requirement 追踪会断裂。

修复方向：

1. 把 UI06-M04 source 改写为 "status / quest / skill presentation + quest summary row consumer + same-screen visual coherence"。
2. 增加 UI06-M08 = "Cross-family same-screen visual coherence"，artifact = `client/build/reports/golden/dark-uiux-pr06-status-quest-skill-overview.png` + manual record `UI/manual-records/dark-uiux-pr06-overview-screenshot.md`。
3. Execution Addendum 补 UI06-M08 行：crossPrDependency = PR-02-1 shell + PR-04 talent + PR-05 actor/portrait；removalOwner = PR-07 only re-audits, does not re-own。

---

## 14. Roguelike / 类 ToME 经验维度的额外审查

### 14.1 战斗节奏与 icon read cost

ToME-likes 的战斗节奏：每回合 1-3s 决策窗口，玩家眼球扫描顺序为「中心地图 → 自身 status 条 → 目标 status 条 → 技能可用性 → 任务提示」。PR06 把这五处 icon 全部一次性替换为 dark-v1，**必须确保扫描顺序不被新视觉破坏**。

关键风险：

1. 新 status icon 若与 skill icon 的 silhouette / outline weight 趋同，玩家眼球扫描时会在 status 条上多停留 0.2-0.5s → 在高强度战斗中会被打断节奏。
2. 当前 design notes §4 separation rules 表已经规定 "Skill vs status" 必须 distinguishable，但 PR06 没有要求 contact-sheet QA 在 contact sheet 上做 "skill vs status side-by-side" 显式对比。
3. 建议 PR06 §3 raw sheet 生成交接流程里增加一步："在 contact sheet QA 同时摆放 status / skill 各两枚，验证扫描成本"，作为 contact sheet QA path 的硬要求。

### 14.2 build 多样化与 icon taxonomy 实际可扩展性

K-ToME 当前 release playable 4 职业 + dev playable 2 职业 = 6 职业，每职业平均 ≥ 3 棵 tree，每棵 tree 平均 ≥ 8 talents → talent icon 数量级在 150-200。PR06 sheet 容量是 64 cell × 3 (r08) ≈ 192 cell；接近上限。

设计风险：

1. 未来若任一职业新增子流派 / 新增 talent，sheet 容量饱和 → 需要新增 `r08-skills-<class>-extra` sheet。
2. PR06 没有规定 "sheet 容量 ≥ 80% 时必须为该家族预留 expansion sheet" 的阈值。
3. talent visual `talent.*.visual` 是大尺寸 portrait，sheet 占用更高；未来扩展更紧张。

修复方向：

1. PR06 §3 资源范围增补一行："任一 sheet 实际 cell 使用 ≥ 80% 时，必须在 §6 final-full inventory 注释中标记 `nearCapacityWarn = true`，并在 PR07 / 后续职业 PR 之前预留 `r0X-<family>-extra` 占位。"
2. 或者把 sheet capacity 监控加入 `darkSpriteSheetLint` 的 warn-level（非阻塞）。

### 14.3 Profession icon 的 "class identity" 语义负载

ktome-dark-ui-design §4 把 profession family 描述为 "class crest, weapon/tool motif, tree emblem"。但 K-ToME 当前 6 职业 + frozen 2 职业 = 8 个 profession icon，每个 icon 必须：

1. 在 profession 选择屏幕（大尺寸 ≥ 128px）作为视觉地标；
2. 在 talent panel header（中尺寸 ≈ 48px）作为身份提示；
3. 在 status / log 行（小尺寸 ≈ 16-24px）作为简略身份；
4. 同时与 skill icon、talent visual 视觉风格相容。

PR06 §6.1 表把 profession icon 的 required consumer 写成 "profession selection/talent consumer test or `ManifestResolveTest`"——又是 "or"。意味着可能没有任何 player-visible test 强制 profession icon 在 selection / talent / status / log 四处一致显示。

修复方向：

1. §6.1 把 profession icon 行的 "or" 改为 "and at least one"，要求至少 profession selection consumer test + talent panel header consumer test 两处上屏证明。
2. §8 人工白盒新增 "切换至少 3 个职业，每个职业从 selection → talent panel → 战斗 log 三处确认 profession icon 视觉一致"。

### 14.4 ToME 风格 status effect 密度

ToME 在 high-level 战斗中常态叠加 5-10 个 status。PR06 §6.2.1 已经把 null icon / unknown icon 行为冻结，但**没有规定** status icon 行的最大显示数量与 fold 策略。

设计风险：

1. 当前 HUD status 行未冻结上限——如果实际渲染时 6+ icon 横向溢出，玩家会失去后续 status 的可见性。
2. PR06 §6.1 把 status family 的 consumer test 限定为 `StatusIconResolverTest`，但 resolver 测试只验"是否解析"，不验"是否显示"。

修复方向：

1. PR06 §6.2.1 增补 "status icon 行最大显示数量必须冻结为 N（建议 8-10），超出部分按 sortKey 优先级 fold，并以 `+N more` 形式提示。"
2. Acceptance Matrix UI06-M04 fastCheck 加 `StatusHudOverflowTest` 或在 `StatusPresentationModelTest` 内补 fold case。

---

## 15. 留给 PR07 的范围 vs 留给 PR06 的范围（边界澄清）

PR06 当前文档对 "PR07 只能 polish 非玩家可见 leftover" 的边界已经写得相当严格（§Acceptance Matrix Execution Addendum / §Failure Rule），但仍存在如下灰色地带需要在 PR06 内 lock 死：

| 灰色地带 | PR06 必须处理 | PR07 仅可 audit |
| --- | --- | --- |
| frozen profession locked card 视觉 / 文案 | yes | re-audit only |
| validation overlay debug-mode budget vs in-run-mode budget 双 mode 拆分 | yes if adopted | re-audit only |
| same-screen ember/cyan accent budget | yes (at least in design notes) | re-audit only |
| missing visual sentinel textual annotation | yes | re-audit only |
| sheet capacity 80% expansion threshold | yes (lint warn level) | re-audit only |
| talent state badge color-blind contingency | yes (white-box step) | re-audit only |
| long-session readability white-box | yes (white-box step) | accept hand-off |
| CJK column-budget overlay truncation | yes (contract change) | re-audit only |
| difficulty visual roadmap | optional but recommended | inherits roadmap |
| typed quest icon mapping | optional but must be explicit follow-up if deferred | does not inherit |

凡是表中 "yes" 项若 PR06 close 时仍未解决，PR07 不应被允许接手——这些都是合同 / 设计 / 体验层级的决策，不属于 PR07 的 "final polish" 范畴。

---

## 16. 严重级别总结表

| 严重级别 | 章节 | Finding 简述 | 一句话原因 |
| --- | --- | --- | --- |
| **Blocker** | §1.1 | `StatusIconResolver.mapNotNull` 仍是 silent drop | PR06 §6.2.1 写了规则但未要求改实现，runtime 仍可能 drop 关键战斗反馈 |
| **Blocker** | §2.1 | quest icon 降到 generic marker 无 typed follow-up | 玩家信息密度损失，且 typed key 永远孤儿，没有显式 follow-up = PR06 close 后无法补救 |
| **Blocker** | §3 | talent 4 态从 ASCII marker 退到纯 badge + tone | 违反"不能仅靠颜色传达状态"，色弱 / 长会话 / 32px 下塌成 2-3 态 |
| **High** | §4 | validation overlay debug vs in-run 双 mode 未拆 | 一套 budget 兼容两类用户必然两边都不满意；fold 优先级把开发者关心的项推到不可见 |
| **High** | §5 | frozen profession locked card 产品信号歧义 | "Coming Soon" 误读对 onboarding / 留存负面 |
| **High** | §6 | fallback / missing / debug / hidden 五语义共享 sheet | dark fantasy 装饰元素 vs missing 视觉无法区分，必须配套 textual marker |
| **High** | §8 | ember-gold / cold-cyan 多绑定缺 accent budget | 同屏 3+ accent 时玩家无法判定焦点，违背 dark-v1 accent 稀缺原则 |
| **High** | §13 | UI06-M04 标题与内容失配 + 同屏 overview 未为 blocking artifact | reviewer checklist 可能漏审"同屏视觉一致性" |
| **Medium** | §1.2 | status vs mutation 双前缀无视觉差异化规则 | 共用 sheet 必然混淆，玩法策略点（能否驱散）反复试错 |
| **Medium** | §1.3 | damage type 缺 player-visible consumer test | 容易出现跨时代 collage |
| **Medium** | §2.2 | quest 空状态 / 满状态视觉权重不一致 | HUD 行抖动观感 |
| **Medium** | §7 | 单 PR 8+ sheet 风险耦合无拆 PR 阈值 | 单一 sheet 阻塞 close window |
| **Medium** | §9 | long-session 视觉疲劳白盒缺失 | 30min 通过 ≠ 2h 通过 |
| **Medium** | §10 | CJK 字符宽度合同未冻结 | 中文 / 日文用户 evidence 关键尾段被 fold |
| **Medium** | §12 | player-visible disposition 矩阵缺失 | reviewer 与开发者 close 时争议边界 |
| **Medium** | §14.1 | contact sheet QA 缺 skill vs status side-by-side | 战斗扫描节奏被破坏的风险 |
| **Medium** | §14.3 | profession icon required consumer 仍是 "or" | 选择 / talent / 战斗三处可能视觉不一致 |
| **Medium** | §14.4 | status icon 行 fold 策略未冻结 | 高密度 status 时后续 icon 可能不可见 |
| **Low** | §11 | difficulty 未来扩展无视觉路线图 | 后续 PR 可能踩中 dark-v1 anti-pattern |
| **Low** | §14.2 | sheet capacity 80% 阈值未冻结 | 后续职业 / talent 扩展时 sheet 紧张 |

---

## 17. 推荐文档补丁清单（按章节）

PR06 文档建议在 close 前补以下字段（不重复历史 round2 review 已提出的工程合同字段）：

1. **§2 影响范围**新增：
   - `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt` 实现合同从 "mapNotNull silent drop" 升级到 "documented fallback or explicit sentinel"。
   - `client/src/main/kotlin/com/ktome/client/ui/status/StatusPresentationModel.kt` `buildZoneEffect` iconKey 决策必须写进 inventory。

2. **§3 资源范围**新增：
   - 单 sheet 累计 ≥2 轮 QA reject 时强制拆 PR 的阈值；
   - sheet capacity ≥ 80% 时 `nearCapacityWarn` 注释；
   - mutation vs status visual grammar 必须显式 separation rule。

3. **§4 职业树联动**新增：
   - talent state badge + tone 4 态在色弱模拟 / 32px / focus overlay 下的可分性要求；
   - 强烈建议保留 marker 作为 secondary cue（可见，不只是 accessibility metadata）。

4. **§6 验收标准**新增：
   - §6.1 status / damage type / profession 行的 "or" 改成 "and at least one player-visible consumer test"；
   - §6.2.1 status icon 行 fold 上限与 `+N more` 策略；
   - §6.2.3 / §6.2.4 quest icon 若坚持 generic marker，必须 named follow-up；否则升级 typed mapping；
   - §6.3 validation overlay 拆双 mode 或 reorder section（warning / evidence 提前）；
   - §6.3 char budget 改 column budget；
   - §6.4 同屏视觉一致性提升为 blocking artifact；
   - 新增 §6.6 = "Player-visible Disposition Matrix"；
   - 新增 §6.7 = "Accent Strength Budget"（ember / cyan 同屏总量限制）；
   - 新增 §6.8 = "Missing Visual Sentinel Textual Annotation Contract"；
   - 新增 §6.9 = "Frozen Profession Player Visibility Disposition"（参见 §5 修复方向）。

5. **§8 人工白盒**新增：
   - 第 6 条：≥60min long-session readability；
   - 第 7 条：色弱模拟 3 filter 下 talent 4 态 + status icon 全可分；
   - 第 8 条：CJK locale 切换下 overlay 长列表 evidence 尾段不被 fold；
   - 第 9 条：同屏 ember/cyan accent 总量目测；
   - 第 10 条：3 个职业在 selection → talent → 战斗 log 三处 profession icon 一致。

6. **Acceptance Matrix** 新增 / 修改：
   - UI06-M04 source 加上 "+ same-screen visual coherence"；
   - 新增 UI06-M08 = "Cross-family same-screen visual coherence"；
   - 新增 UI06-M09 = "Sheet QA escalation policy"；
   - 可选 UI06-M10 = "Long-session readability"；
   - Execution Addendum 同步增补。

7. **design notes（open-design）** 同步：
   - §4 separation rules 补 status vs mutation 一行；
   - §5 state badge 表新增 mutation 与 damage type 项；
   - §5 fallback / missing / debug 三语义独立 visual rule；
   - 新增 §12 = "Difficulty Visual Roadmap"。

---

## 18. 玩法体验维度补充评论

### 18.1 战斗循环

PR06 直接决定战斗中 status / mutation / damage_type / skill / talent 五大类 icon 的视觉同框表现。最大风险：silent-drop status + generic quest marker + 4 态 badge-only = 玩家在战斗中失去三层关键反馈。这三项必须在 PR06 内冻结，PR07 已经没有合同 surface 接收。

### 18.2 成长 / build 循环

talent state 4 态可分性是 ToME-likes build 体验的核心。PR06 已经做了 manifest 切换合同与 selection/state 分离的工作，但**最关键的"状态认知通道"反而被削弱**（marker 移除）。该问题若不解决，玩家会通过悬停 tooltip / 文本行 / debug overlay 等 workaround 弥补 → 与 dark-v1 "视觉优先"目标背道而驰。

### 18.3 探索 / 任务循环

quest icon 是 Roguelike 探索循环里最容易被低估的反馈通道。当前 PR06 把它降到 generic marker，等价于在 UI 视觉升级中"任务通道未升级"。如果有意为之，必须命名 follow-up；如果只是"先 ship 再说"，会在 PR07 前后形成 typed quest icon 永远孤儿的局面。

### 18.4 验证 / 调试循环

validation overlay 是 mod 开发者 / 内容作者 / QA 三类用户的真实工作面。PR06 把它压缩到 12 行 / 96 字符的"in-run friendly" budget，会同时损害 mod 作者效率（看不全 evidence）和玩家观感（仍然遮挡 HUD 12 行）。双 mode 拆分是更负责任的设计。

### 18.5 新手 onboarding

frozen profession locked card 是新玩家第一次进入 profession 选择时直面的产品信号。PR06 仍允许它显示 dark-v1 fallback 视觉——会被读成"待开放完整版"。这种误读对早期留存的伤害大于任何技术合同问题。

### 18.6 长会话沉浸

dark-v1 palette 在短时间内非常出色（design notes 评估都是优秀），但 2-3h 后会出现"低对比度疲劳 + accent 注意力饱和"两类问题。PR06 没有任何长会话验证步骤——这是该 PR 最容易被"单帧 golden 全绿"掩盖的体验缺口。

---

## 19. 结语

PR06 的工程合同打磨已经接近"开发文档应有的样子"，特别是 final-full inventory / frozen profession exclusion / handoff 路径几项，与 round2 review 相比是显著进步。本文档的所有 finding 都不否定这些进步。

但 PR06 在产品定位上是「玩家可见 UI 收口」——它必须既证明 manifest 全绿，又证明同屏体验真的好。当前文档在「同屏体验」这一边仍有 3 个 Blocker、5 个 High、9 个 Medium 风险。这些问题大多不是"再加一段 lint"能解决的；它们要求设计 / 系统策划 / 工程三方在 PR06 close 之前达成一致 disposition，并把 disposition 写回文档作为合同。

建议下一步：

1. 召开一次 30-45min 的 design-director / system-designer / lead-engineer 三方对齐会，对本文 §16 严重级别表中 Blocker + High 共 8 项当面决策（accept / mitigate / defer-with-named-followup）；
2. 决策结果回写 PR06 §6（新增 §6.6 - §6.9）与 Acceptance Matrix 新增条目；
3. design notes 同步补 §4 / §5 / §12 内容；
4. §8 人工白盒按 §17.5 列表落到具体步骤；
5. PR07 文档同步澄清"哪些不接收" disposition；
6. 重新跑 `acceptanceContractLint` 与 `darkManifestCoverageLint final-full` 验证文档级 lint 不退化。

PR06 close 之后再返工设计层 disposition 的成本会高一个量级，因为 PR07 已不再拥有资源 sheet / consumer / overlay layout 的 owner 边界。**现在收口仍是最便宜的窗口**。

---

## 附录 A：本次未运行的命令清单

本轮为文档级 design review，未运行任何 Gradle / Python / test 命令。所有锚点均来自：

- `UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`
- `UI/review/open-design/dark-uiux-pr06-skills-status-quest-manifest-design.md`
- `UI/review/open-design/ktome-dark-ui-design.md`
- `UI/review/2026-05-09-dark-uiux-pr06-pr-level-standard-rereview-round2.md`
- `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt`
- `client/src/main/kotlin/com/ktome/client/render/ValidationPackSummaryText.kt`
- `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`

建议本文档接收 review 后再触发 `acceptanceContractLint` 与 `darkManifestCoverageLint final-full`，验证补丁回填后 lint 不退化。

## 附录 B：与既有 review 的关系

| Review 文档 | 维度 | 关注层 |
| --- | --- | --- |
| `2026-05-09-dark-uiux-pr06-pr-level-standard-review.md` | PR-level standard 工程合同 | 第一轮 |
| `2026-05-09-dark-uiux-pr06-pr-level-standard-rereview-round2.md` | PR-level standard 工程合同 | 第二轮（gate / inventory / exclusion） |
| **本文档** | **设计总监 / 系统策划 / 玩法体验** | **第三轮（玩法 / disposition / 同屏 / 长会话）** |

三份文档应当作互补的合同输入；本文档不重审 round2 已覆盖的 gate / coverage / artifact 维度，专注于 "coverage 全绿之后才会暴露的体验缺口"。

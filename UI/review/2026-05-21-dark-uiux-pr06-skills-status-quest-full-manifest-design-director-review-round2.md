# Dark UI/UX PR06 设计总监 / 系统策划 / 玩法体验深审 (Round 2)

**目标文档**：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`（已吸收 2026-05-21 round-1 director review 的补丁后的当前版本）
**审查立场**：资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
**审查日期**：2026-05-21（同日二审）
**互补关系**：
1. 不重复 `2026-05-09-dark-uiux-pr06-pr-level-standard-rereview-round2.md` 已覆盖的 PR-level 工程合同维度问题。
2. 不重复 `2026-05-21-dark-uiux-pr06-skills-status-quest-full-manifest-design-director-review.md`（round 1）已经修复/合同化的项目；本文档**只对 round-1 补丁**做 *patch verification*，并列出 round-1 未触及或被新合同引入的次生风险。
3. 本文档明确区分「文档层合同」与「代码事实」，并对 round-1 中只在文档层冻结的关键问题逐项交叉比对当前 `client/` 实际实现，避免合同与代码再次脱节。

> 重要前置：PR06 的 inventory / final-full gate / frozen exclusion schema / disposition matrix / accent budget / long-session contract 在 round 1 后**显著收口**。本轮聚焦：(a) 补丁是否真把 round-1 的洞填上；(b) 新合同自身有没有引入新洞；(c) round-1 视角未触及的 game-feel / 系统策划维度。

---

## 0. 总评 (TL;DR)

PR06 文档相对 round-1 状态有明显进步：

| Round-1 Blocker | Round-2 状态 |
| --- | --- |
| §1.1 `StatusIconResolver.mapNotNull` silent drop | **代码已 fix**：`StatusIconResolver.kt:26-37` 改为 `.map { ... ?: visualResolver.resolveMissing(...) }`；同时新增 `StatusIconResolverTest` 两条 case |
| §2.1 quest icon 单 generic marker 无 follow-up | **文档已 fix**：§6.2.9 新增 named follow-up `UI07-quest-typed-icon-mapping` |
| §3 talent 4 态从 ASCII marker 退到 badge + tone | **文档已 fix**：§4.7 显式要求保留 1-char glyph or 固定 marker slot 作为 secondary cue |

Round-1 列出的 5 个 High 和 9 个 Medium 大部分也在 PR06 §6.4–§6.7、§3.5、§3.9–§3.10、§6.1 "and at least one"、§6.3 column-aware budget 中落到合同。这是一份**显著优于行业平均水平**的开发文档。

但站在游戏开发设计总监视角，本轮仍发现 **3 个 Blocker + 4 个 High + 7 个 Medium** 是 round-1 没看到、或被 round-1 补丁本身**新引入**的风险。它们的共同特征：

1. **合同 vs 代码错位**：文档已经写了规则，但当前 implementation（已经在 working tree 的代码）没把规则真的接到 runtime。
2. **patch 引入的次生洞**：round-1 修复一个问题时，新的边界条件没被同时冻结。
3. **系统策划真源歧义**：当 disposition / fold / accent 出现冲突时，PR06 没有指定谁是裁定者。

**Round-2 三大新 Blocker**：

| 序号 | 章节 | 一句话 |
| --- | --- | --- |
| **B1** | §1.1 | `StatusIconResolver` 的 fix **把 silent drop 换成 silent flood**：所有 `iconKey == null`（含 `buildZoneEffect` 的合法 null 路径）都被路由到 `missing_visual` sentinel，HUD 在任何含 zone effect 的回合都会显示 sentinel icon；这是 visible regression，且 `status.icon.missing.<typeId>` key shape 不在 inventory 分母里。 |
| **B2** | §2.1 | `r09-status-damage` 单 sheet 同时承载 `icon.status.*` / `icon.mutation.*` / `icon.damage_type.*` **三**类家族；PR06 §3.5 强调 status vs mutation visual grammar 分离，但 **damage_type 与 status / mutation 的视觉分离规则在 PR06 与 design notes §4.2 separation rules 里仍然缺一行**。同张 sheet + 缺第三方分离规则 ⇒ 32px 必然 collapse。 |
| **B3** | §3 | §6.2 quest summary row contract 把 `activate / progress / complete` 三类 token 全部映射到 *同一* `icon.quest.objective_marker`，**且没有为 `complete` 指定 row tone / accent 差异**。`complete` 在 Roguelike 探索循环中是核心成就反馈，被压平到与 `progress` 视觉同质 = 玩家完成任务那一瞬间没有任何视觉确认。这是单点最容易被低估的体验损失。 |

详见 §1–§3 各章节论证。

---

## 1. Patch Verification：round-1 修复是否真的关掉了战斗反馈通道

### 1.1 [Blocker B1] `StatusIconResolver` 的 fix 把 silent drop 换成 silent flood

**代码事实**（`client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt:19-38`）：

```kotlin
internal object StatusIconResolver {
    private const val MISSING_STATUS_ICON_KEY_PREFIX: String = "status.icon.missing."

    fun resolveIcons(
        visualResolver: VisualManifestResolver,
        effects: List<StatusEffectRenderSnapshot>,
    ): List<StatusHudIconModel> =
        StatusPresentationBuilder
            .sorted(effects.map(StatusPresentationBuilder::build))
            .map { presentation ->
                val asset =
                    presentation.iconKey
                        ?.let(visualResolver::resolve)
                        ?: visualResolver.resolveMissing(MISSING_STATUS_ICON_KEY_PREFIX + presentation.typeId)
                StatusHudIconModel(asset = asset, presentation = presentation)
            }
}
```

**`StatusPresentationBuilder.buildZoneEffect`**（`StatusPresentationModel.kt:52-63`）：

```kotlin
fun buildZoneEffect(terrainOverride: TerrainOverrideRenderSnapshot): StatusPresentationModel =
    StatusPresentationModel(
        typeId = terrainOverride.sourceRuleId,
        nameKey = terrainOverride.ruleNameKey,
        iconKey = null,                       // ← 永久 null，作为合法路径
        category = StatusEffectCategorySnapshot.NEUTRAL,
        rawBadge = turnBadge(terrainOverride.remainingTurns),
        priority = 650 + dangerLevel * 10,
        group = StatusPresentationGroup.ZONE_EFFECT,
    )
```

**问题链**：

1. `buildZoneEffect` 是 PR-04 之前就存在的合法路径（zone effect / 地形效应在 HUD 状态条出现），并且 **设计上 zone effect 不归属 `icon.status.*` 家族**——PR06 §6.2.1 #2 自己也承认这点：「`buildZoneEffect` 当前 `iconKey = null` 不是 `icon.status.*` / `icon.mutation.*` 覆盖分母」。
2. round-1 review §1.1 给出修复方向之一："zone effect 在 PR06 后路由到独立 row，**不要让 zone effect 永远以 silent drop 形式存在却又不出现在分母里**"。
3. 当前代码事实：`resolveIcons` 不区分 zone effect 与正常 status，**所有 `iconKey == null` 一律落到 `resolveMissing(MISSING_STATUS_ICON_KEY_PREFIX + typeId)`**。`resolveMissing` 直接走 `manifest.fallbackKey`（`ManifestResolvers.kt:59-72`），意味着：
   - 每一回合，只要场上有任何 zone effect（acid pool / fire field / lightning storm），HUD status 行就会出现 1+ 个 `missing_visual` sentinel。
   - 复杂战斗（多个 zone overlay）下 HUD 会出现 3-5 个并排的 sentinel——玩家会判定"游戏崩了 / 资源缺失"。
   - `logSink.error(...)` 在每个渲染帧都会触发（`ManifestResolvers.kt:64`），日志被海量 spam。
4. round-1 修复目标"不能 silent drop"满足了，但**新行为是 silent flood**：sentinel 替换了原本的 drop，HUD 仍然没法把 zone effect 表达成它该有的样子（独立 row / 独立 visual grammar）。

**为什么 round-1 没看到这点**：

- round-1 § 1.1 已经点名 `buildZoneEffect` 的 null 路径是"暗洞"，并要求 PR06 在 §6.2.1 显式决策（变成 status icon ， or 独立 row）。
- round-1 之后 PR06 §6.2.1 #2 只把"在 inventory 里写 manifest-only-with-reason"作为 fallback，**没有要求 resolver 实现层把 zone effect 路由出 status icon 行**。
- 提交者按"补测试 + 提供 fallback"路径满足了 round-1 的测试断言（`StatusIconResolverTest.null status icon uses visual fallback instead of dropping status`），但行为变成 sentinel flood，**测试通过等于 production-broken**。

**修复方向（必须在 PR06 close 前完成）**：

1. `StatusIconResolver.resolveIcons` 必须显式区分两类 null iconKey：
   - **zone effect (group == ZONE_EFFECT)**：不进入 status icon HUD 行——`resolveIcons` 应在入口处把 `group == ZONE_EFFECT` 的 presentation **过滤出去**，让 zone effect 由独立 row owner（如 `ZoneEffectHudRow` / `TerrainOverlayRow`）渲染；或者
   - **真正的 missing status icon**（`group != ZONE_EFFECT && iconKey == null`）：进入 documented sentinel 路径。
2. PR06 §6.2.1 必须把 "zone effect 不进入 status icon 行" 从"inventory 里写 reason"升级为 **runtime 实现合同**：
   - 增补一句"`StatusIconResolver.resolveIcons` 必须排除 `group == ZONE_EFFECT` 的 presentation，并由独立 row 渲染。如果 PR-06 决定让 zone effect 进入 status icon 行，必须新增 `icon.status.zone_effect.*` family 并进 inventory 分母。"
3. `MISSING_STATUS_ICON_KEY_PREFIX = "status.icon.missing."` 这个 ad-hoc key shape 不在 inventory 分母（§6.1 表格），也不在 key registry / sheet plan 里。必须：
   - 要么把它从代码删掉，改用 `resolveMissing("icon.status." + typeId)` 让 `VisualManifestResolver.resolve` 走 prefix 规则（与其他 status icon 一致）；
   - 要么在 PR06 §6.1 inventory `fallback/debug/hidden` 行显式列出"`status.icon.missing.*` 作为 resolver-only sentinel key"，并写出它与 `missing_visual` 的关系。
4. 测试必须新增：
   - `StatusIconResolverTest.zoneEffectIsRoutedOutOfStatusIconHudRow`（验证 zone effect 不出现在 `resolveIcons` 返回列表里）。
   - `StatusIconResolverTest.missingIconLogsAtMostOncePerKeyPerFrame`（验证不会每帧 spam log——可以用 dedup log sink）。

**为什么这是 Blocker**：

PR06 close gate 跑 `goldenScreenshot` 时如果 golden scenario 不含 zone effect，sentinel flood 不会被发现；但 `:client:clientSmoke` 与人工白盒（§8 第 2 条 "1 个 null-icon status/zone-effect case"）一旦触发 zone effect，HUD 就会显示 sentinel——这是面向 reviewer 的**第一眼"PR06 完成度不够"信号**。修复成本约 30-60 行代码，不修复成本是 PR-07 packaged app 评测时的连环 bug report。

### 1.2 [High] `MISSING_STATUS_ICON_KEY_PREFIX` 的 per-typeId sentinel 与 `missing_visual` 单点 fallback 之间逻辑空转

**代码事实**：

- `resolveMissing(requestedKey)` 直接走 `manifest.fallbackKey`，**完全忽略 `requestedKey` 的内容**（`ManifestResolvers.kt:59-72`）。
- 意味着 `resolveMissing("status.icon.missing.poison")` 和 `resolveMissing("status.icon.missing.stun")` 返回**完全相同**的 `ResolvedVisualAsset(resolvedKey="missing_visual", ...)`，唯一差别只在 `requestedKey` 字符串（用于 log）。

**设计影响**：

1. `MISSING_STATUS_ICON_KEY_PREFIX + typeId` 本意可能是"给每个 missing status 一个独立 sentinel"或"在 manifest 中预定义 per-typeId missing variant"——但 `resolveMissing` 实现不消费 prefix，意味着这个 prefix 现在是**纯日志噪声**。
2. 测试 `StatusIconResolverTest:38-42` 断言 `requestedKey == "status.icon.missing.missing_status_icon"`，**测的是字符串拼接**，不是 visual behavior。这是一个 false-positive 测试——它通过了，但没证明 HUD 表现正确。
3. 如果未来某天有人决定"每个 missing status 用独立 sentinel sprite"（合理设计选择），现在的 ad-hoc prefix 与 fallback 单点的 collision 会让 manifest 改动牵涉 resolver 内部常量，违反 PR06 §6 "不能只改 renderer 让 gate 变绿"。

**修复方向**：

1. **首选**：删除 `MISSING_STATUS_ICON_KEY_PREFIX`，直接用 `presentation.typeId` 拼真实 manifest key（例如 `"icon.status.$typeId"`）调 `resolve(...)`——让 manifest prefix 规则与 fallback 链统一处理，而不是在 resolver 里搞 ad-hoc routing。这也让 missing icon 与 unknown icon 的处理路径合并。
2. **次选**：保留 prefix 但在 PR06 §6.2.1 显式说明 "`status.icon.missing.<typeId>` 是 resolver-internal log marker，**不进入** manifest contract，永不在 sheet/registry 出现"；并补一条测试验证：`assertThat(missingSentinelKey).startsWith("status.icon.missing.")` 是允许的内部前缀，但 `manifestLint` / `darkKeyRegistryLint` 必须**忽略** `status.icon.missing.*`。

**为什么这是 High（不是 Blocker）**：

行为上 sentinel 与 `missing_visual` 共享视觉，不会引起 production crash；但合同与实现不一致会让未来的 schema 变更踩坑。

### 1.3 [Medium] `+N more` fold badge 的合同未冻结到 visual / locale 层

PR06 §6.2.1 #4：

> 超过上限时按 `StatusPresentationBuilder.sorted` 的 priority / typeId 顺序保留前 10 项，并以 `+N more` 或等价 badge 告知折叠数量。

**遗漏点**：

1. `+N more` 字符串本身：locale key 是什么？ASCII 字面量？`ui.status.fold.more.label`？格式（`+3 more` vs `+3` vs `…(+3)`）？
2. badge 的视觉 owner：是 status icon HUD 行最后一个 slot（占用 status icon 容量），还是独立 overlay row？
3. accent treatment：fold badge 是中性 muted，还是 ember（因为它隐含"还有重要状态没显示"）？
4. tap/hover：玩家是否能点 fold badge 展开完整列表？如果不能展开，那 ToME-like 玩家会在 build 阶段抱怨"看不到全部 status"——这是经典的 HUD UX 投诉点。

**修复方向**：

1. PR06 §6.2.1 #4 增补：
   - locale key 固定为 `ui.status.fold.summary`，content `+{count} more`。
   - 视觉 owner = status icon HUD 行的固定末尾 slot；fold 时占用 1 status icon 宽度。
   - tone = `LIGHT_GRAY` 或对应 muted accent，**不**使用 ember/cyan（避免成为新 accent 焦点）。
   - hover/tap：PR-06 可不实现 expand 行为，但必须命名 follow-up `UI07-status-fold-expand`，避免再次成为 typed-quest-icon 那样的孤儿 follow-up。
2. 测试增补：`StatusPresentationModelTest.foldsAtTenWithSummaryRow`（断言 fold badge 出现在 11+ status 场景，slot 是固定末尾，count 正确）。

### 1.4 [Medium] Status overflow fold 的 priority 倒置：fold 时 NEUTRAL 先被砍掉，而 NEUTRAL 涵盖 ZONE_EFFECT

**代码事实**（`StatusPresentationModel.kt:96-111`）：

- DEBUFF priority = 700+；BUFF = 500+；NEUTRAL = 300+；TELEGRAPH = 900+；ZONE_EFFECT = 650+。
- `buildZoneEffect` 强制 `category = NEUTRAL` 但 `group = ZONE_EFFECT`；priority 走 `650 + dangerLevel * 10`，不走 `statusPriority`。
- `sorted()` 按 priority desc 排，fold 时保留 top 10。

**问题**：

1. 如果 §1.1 的 fix 没有先把 zone effect 路由出 status icon 行，zone effect 会占据 status icon 容量（每个 zone effect = 1 slot），而它在视觉上已经是 sentinel——**双重浪费 fold 容量**。
2. ToME-likes 战斗中 DEBUFF 频繁叠加（中毒 / 出血 / 眩晕 / 易伤 / 冰冻 / debuff buff stacks），玩家最关心 DEBUFF 数量与剩余回合。fold 顺序如果让 zone effect（NEUTRAL 范畴但 priority 650+）压在 BUFF 上面，玩家自己的 buff（priority 500+）反而被 fold。
3. 当前 priority 表合理性（telegraph > debuff > zone > buff > neutral）在 game-feel 上是对的（敌人下一步 > 你被打 > 地面 > 你给自己加 > 一般），但没有写进任何 contract——未来调整 priority 时 QA 没有 anchor 拒绝改动。

**修复方向**：

1. PR06 §6.2.1 增补一张 "Status HUD fold priority order" 表，把 `telegraph > debuff > zone_effect > buff > neutral_other` 作为 disposition contract 冻结。
2. 测试 `StatusPresentationBuilderTest.foldPreservesTelegraphBeforeDebuffBeforeZoneBeforeBuff` 显式断言顺序。
3. 真正的解法仍然是 §1.1 fix——zone effect 拉出 status icon HUD 行后，fold 表只需要管 `telegraph > debuff > buff > neutral`，更稳。

### 1.5 [Medium] Round-1 §1.2 mutation vs status visual grammar 已写进 §3.5，但合同没下沉到 contact-sheet QA 表

**PR06 §3.5**：

> mutation 必须保留独立 visual grammar：使用闭合框、scar / etching 或永久体质变更母题；status 使用 open badge、单条件 glyph 或可驱散状态母题。两者可以共用 sheet，但 contact-sheet QA 必须做 side-by-side 区分。

这一段写得好，但**没有进一步写成 contact-sheet QA rubric**。design notes §7 Contact-Sheet QA Rubric 也没补 "status vs mutation visually distinct" 这一行（design notes §4.2 separation rules 表已补 status vs mutation，但 §7 rubric 没同步）。

**修复方向**：

1. design notes §7 表新增一行：「Status vs mutation: closed-frame/scar/etching motif vs open badge/single-glyph motif | minor frame overlap | scar/etching used on a removable status, or open badge used on a mutation」。
2. PR06 §3.5 末尾追加 "contact-sheet QA report 必须明确标记每个 mutation cell 的 closed-frame motif 命中情况；side-by-side QA 文件名固定 `dark-uiux-pr06-status-mutation-sidebyside-qa.md`"。

---

## 2. `r09-status-damage` 单 sheet 三家族：damage_type 的视觉孤儿

### 2.1 [Blocker B2] damage_type 与 status / mutation 共享 sheet 但缺独立分离规则

**证据锚点**：

- PR06 §3.2 `r09-status-damage` 单张 sheet。
- PR06 §3.5 mutation 与 status visual grammar 分离规则。**但同段没提 damage_type**。
- PR06 §6.1 family 表 damage type 行 owner sheet = `r09-status-damage`，consumer = `ManifestResolveTest` and at least one player-visible status/combat/tooltip focused test（round-1 已修复 "or" → "and at least one"）。
- design notes §4 Icon Taxonomy 表给 damage type 写 "elemental or physical silhouette with hard outline"，与 status 区别在 "color tied to type but low saturation"。
- design notes §4.2 separation rules 表**没有** "Status vs damage type" 或 "Mutation vs damage type" 行。

**问题链**：

1. ToME-likes 战斗中 damage_type icon 出现位置极多：
   - 装备 tooltip（武器 → 物理/魔法/元素 type）；
   - 技能预览（释放某技能 → 伤害 type icon）；
   - 状态 tooltip（被某 status 影响 → DOT type icon）；
   - 伤害浮字（每次结算 → type icon + 数字）；
   - 抗性 panel（character 属性 → 各 type 抗性 icon）。
2. damage_type 与 status 共用 sheet 意味着它们必然继承相同 frame / lighting / outline / silhouette weight。设计 brief 给 damage_type 写"hard outline + elemental silhouette"，给 status 写"open badge + single condition glyph"——但**两者都是 32px 圆形 / 方形 frame + 中心 motif + 类型色**的结构。
3. 32px 缩略时，"火元素 damage type" 与 "燃烧 status" 共用 sheet 不可避免地塌成同一个：
   - 火 damage type = 红色火苗 silhouette + 物理 outline。
   - 燃烧 status = 红色火苗 condition badge + open frame。
   - 32px 下 outline 是否 closed 几乎不可见——剩下的就是火苗 silhouette + 红色 tone。**collapse**。
4. 玩家看战斗浮字时，"我被燃烧 status 折磨"和"我吃了火元素 damage"是两个完全不同的策略点：
   - 燃烧 status → 需要 cleanse / 拉距离让 tick 自然结束 / 跑出火域；
   - 火 damage → 需要堆抗性 / 换装备 / 选元素保护 talent。
   - 视觉混淆 = 策略决策被迫退回读文字。

**为什么 round-1 没看到这点**：

- round-1 §1.3 只指出 damage type 缺 player-visible consumer test，没看到三家族共享 sheet 的视觉风险。
- round-1 §1.2 只关注 status vs mutation 分离，跳过 damage type。

**修复方向**：

1. PR06 §3.5 mutation/status separation rule 段落必须扩成 **三家族 separation rule**：
   - status：open badge + 单条件 glyph + condition-tied tone（短时 / 可驱散）。
   - mutation：closed frame + scar/etching + 永久体质变更母题（不可驱散 / 高代价）。
   - damage_type：hexagonal or angular frame + elemental/physical silhouette **without badge frame**（伤害通道 ID，不是状态）。
2. design notes §4.2 separation rules 表补三行：
   - Status vs damage type；
   - Mutation vs damage type；
   - Status badge vs damage type icon at 32px。
3. design notes §7 Contact-Sheet QA Rubric 表补一行 "Damage type vs status / mutation at 32px"，Pass = 三家族的 frame shape 一眼可分。
4. PR06 §3 raw sheet 生成交接段落增补："`r09-status-damage` contact-sheet QA 必须在同一 contact sheet 上摆放 ≥ 2 个 status、≥ 2 个 mutation、≥ 2 个 damage type，做三家族 cross-row side-by-side 检查。"
5. PR06 §6.1 damage type 行 required consumer 补具体路径：建议至少 (a) 武器 tooltip 渲染 path 的 focused test，(b) 伤害浮字渲染 path 的 focused test。当前 "at least one player-visible status/combat/tooltip focused test" 仍是 OR——必须 specify 哪一类是 minimum。

**为什么这是 Blocker**：

PR06 是 dark UI 全量收口 PR。damage_type 是装备 / 技能 / 战斗三处共同出现的 family，它如果在收口 PR 没被强制分离规则覆盖，进入 PR-07 后没有 sheet owner 能接住（PR-07 不再拥有 sheet）。一旦 player-visible 装备 tooltip 与战斗浮字的 damage_type icon 与 status icon 32px 混淆，玩家反馈会持续到下一个大版本。

### 2.2 [High] damage_type 缺 "至少哪个 consumer test 是 minimum" 的硬约束

**证据锚点**：

PR06 §6.1：

> | damage type icon | `icon.damage_type.*` | damage type manifest entries and canonical manifest | `r09-status-damage` | `ManifestResolveTest` and at least one player-visible status/combat/tooltip focused test |

"and at least one" 比 round-1 的 "or" 改进了，但 "any one of three" 仍允许 PR-06 只挑最轻量的 path 满足。

**问题**：

- "status focused test" 可以是 status tooltip 渲染 damage_type icon 的 unit test——但这只验"icon 解析"，不验"装备 tooltip 与战斗浮字一致"。
- "combat focused test" 可以是 damage-resolve unit test，与 UI 完全无关。
- "tooltip focused test" 没指明是装备 tooltip 还是 status tooltip 还是技能 tooltip。

**修复方向**：

PR06 §6.1 damage type 行 required consumer 必须 specify：
> `ManifestResolveTest` **and** (a) 武器/装备 tooltip damage type icon focused test, **and** (b) 至少一个伤害浮字 / 技能预览 / 抗性 panel 的 player-visible focused test。

或者 §6.4 disposition matrix 表新增一行 "weapon/equipment tooltip damage type icon | visible | in | required dark-v1"。

---

## 3. Quest summary row：`complete` 被压平到 `progress` 的体验损失

### 3.1 [Blocker B3] 三类 token 映射同一 marker + 无 row tone 差异 = 任务完成无视觉反馈

**证据锚点**：

PR06 §6.2 quest icon consumer contract：

| Mapping | Token | Icon key |
| --- | --- | --- |
| activate | `log.objective.activate` | `icon.quest.objective_marker` |
| progress | `log.objective.progress` | `icon.quest.objective_marker` |
| advance | `log.objective.advance` | `icon.quest.objective_marker` |
| complete | `log.objective.complete` | `icon.quest.objective_marker` |

`TileRenderModel.kt:929`：当前 quest summary 已经写成 `TileTextRow(questSummary, TileTextTone.LIGHT_GRAY)`，**单一 tone**。

**问题链**：

1. ToME-likes 探索循环的核心反馈节点是"任务完成"（"You found the lost amulet!" / "The portal opens!" / "Goal reached."）。这一刻是玩家继续 run 还是收尾的决策点。
2. PR06 §6.2 让 `complete` 与 `activate / progress / advance` 全部使用同一 icon key 同一 row tone，意味着 HUD 在玩家完成任务的那一帧只能靠**文本变化**通知——而 ToME-likes 玩家在战斗中视线常常不在 HUD 文本行，他们用 icon 周边的 accent 闪烁感知"有新东西"。
3. round-1 review §2.1 已经提议过 "complete → 同 icon + green tone overlay，由 row tone 控制"——这是低成本高回报方案，但**当前 PR06 §6.2 没采纳**。
4. round-1 review §2.2 提到的"空状态保留占位 slot"问题，PR06 §6.2.6 仍然保持"空状态保留 `ui.shell.quest.none` 文案；空状态不消耗 quest icon"——意味着 quest summary row 在"无任务 → 接受新任务"切换时仍会出现 icon-slot 抖动（无 icon → 有 icon）。

**修复方向**：

1. **首选**：PR06 §6.2 在 mapping 表后追加 **row tone disposition**：
   ```
   complete row tone: ember-gold accent for 0.5-1.0s, decay to LIGHT_GRAY（与 §6.5 confirmation feedback strength 1 一致）；
   activate row tone: LIGHT_GRAY；
   progress / advance row tone: LIGHT_GRAY；
   empty state: LIGHT_GRAY 且保留 muted placeholder icon slot（消耗 row 高度，不消耗 quest icon 上屏证明）。
   ```
2. 测试 `TileRenderModelTest.shellQuestSummaryUsesEmberAccentOnObjectiveComplete`。
3. 同步 design notes §3 / §5 / §5.2：把 "complete" 作为 accent 的合法触发点列出（design notes §5 state badge 表当前只列了 "Active beneficial / Harmful / Timed / Locked / Learnable / Learned active / Quest active / Missing / Mutation / Damage type"，没有 "Quest complete" 行）。
4. 如果 PR-06 决定不在本 PR 内做 row tone 差异化，必须**命名 follow-up `UI07-quest-complete-accent`** 并写进 `UI/PLAN.md`——与 quest typed mapping follow-up（`UI07-quest-typed-icon-mapping`）平行。**禁止**把 "complete 视觉与 progress 一致"作为默认终态。

**为什么这是 Blocker**：

- 此问题与 PR-06 的同屏 overview 一致性合同**不冲突**——同屏 overview 仍然全绿，但任务完成那一帧的体验丢失。
- round-1 §13 已经把 UI06-M08 加为 same-screen visual coherence，但"任务完成时 accent 闪烁" 不在 same-screen overview 的 frozen frame 内，**需要动态 evidence**（manual record 描述 + 短视频 / GIF）。
- PR-07 不再拥有 quest summary row owner，这是 PR-06 close 后**没有 surface 可以接收**的体验合同。

### 3.2 [High] Quest summary row 当前在代码里仍是 text-only：PR06 实施前置不清

**代码事实**（`TileRenderModel.kt:861, 929`）：

```kotlin
val questSummary = questSummaryText(localizer, snapshot)
...
TileTextRow(questSummary, TileTextTone.LIGHT_GRAY),
```

`questSummaryText` 返回 `String`，row 是 plain `TileTextRow`，**没有 icon slot**。PR06 §6.2.1 说"Quest summary row 必须变成携带 icon 的 `TileTextRow`"——这是 PR06 commit 要做的事，**当前未做**。

**问题**：

1. PR06 §3 Implementation Order 第 4 步 "status、talent/tree、quest summary、validation overlay 必须各有实际 consumer 和 focused test"。但实际 commit 顺序若是 "先补 inventory → 再补 manifest → 再补测试 → 最后改 renderer"，renderer 改完前 `dark-uiux-pr06-status-quest-skill-overview` golden 截图就会暴露：quest summary row 没 icon。
2. 当前 working tree 已经有 PR06 doc 的修改、StatusIconResolver 的修改、StatusIconResolverTest 的新增，但 **`TileRenderModel` 没改**——意味着提交者已经在补合同与 status 行为，**还没动 quest row**。Round-2 review 必须把这个 gap 显式 flag，避免最终 PR commit 漏掉 renderer 改动。

**修复方向**：

PR06 §3 Implementation Order 第 4 步应改写为 explicit checklist：

```
4. Client consumers:
   4a. StatusIconResolver: route ZONE_EFFECT out + documented sentinel for non-zone null iconKey (see §1.1)
   4b. TileRenderModel.buildShell: replace plain TileTextRow questSummary with icon-bearing TileTextRow using QuestSummaryIconResolver
   4c. ValidationOverlaySummaryPresenter: implement and route through ValidationPackSummaryText / ValidationScenarioEvidenceSummaryLines
   4d. TalentSidebarPresenter: switch node icon / tree icon to dark-v1 keys; preserve secondary cue per §4.7
```

每一子步骤必须有对应的 focused test 名（与 §6.1 family 表 consumer 列对齐）。

### 3.3 [Medium] `QuestSummaryIconResolver` mapping 表对 `advance` token 的语义没明示

PR06 §6.2.4：

> 新增 client-only owner `QuestSummaryIconResolver` 或等价小对象，唯一 mapping 表为：`log.objective.activate`、`log.objective.progress`、`log.objective.advance`、`log.objective.complete` -> `icon.quest.objective_marker`。

**问题**：

`activate` / `progress` / `complete` 语义清晰，但 `advance` 在 K-ToME 现有 log token 体系里语义是什么？是 "objective state transition" 还是 "next phase"？code search 看不到现行 `log.objective.advance` 触发点。如果它实际上代表"objective 进入下一阶段（多 step quest 翻页）"，那它的视觉权重应该接近 `complete` 而不是 `progress`。

**修复方向**：

PR06 §6.2.4 mapping 表后增补一行 token semantics 说明：

| Token | Trigger | Visual weight |
| --- | --- | --- |
| `log.objective.activate` | new objective unlocked | LIGHT_GRAY + brief pulse |
| `log.objective.progress` | counter increment within current objective | LIGHT_GRAY |
| `log.objective.advance` | objective enters next sub-phase | medium pulse, no accent |
| `log.objective.complete` | objective finished | ember-gold accent 0.5-1.0s |

如果 token 语义未知，PR06 必须先 audit `core` / `game` 中所有 `log.objective.*` emission 点，把语义写入 `QuestSummaryIconResolver` 注释，然后再决定 visual weight。

---

## 4. ValidationOverlaySummaryPresenter：双 mode 合同的实施盲区

### 4.1 [High] `detailed` mode 的 row budget 32 行从哪里来？由谁裁定？

**证据锚点**：

PR06 §6.3：

> | display mode | `validation.overlay.compact` 用于 in-run overlay，最多 12 行；`validation.overlay.detailed` 用于 ValidationSetupScreen / dedicated validation panel，最多 32 行或按 panel 可视高度自然展开 |

**问题**：

1. round-1 §4.1 已经把双 mode 拆分写进 PR06，这是显著进步。但 detailed mode 的 owner 边界仍模糊：
   - "32 行" 是 hard cap 还是 soft target？
   - "按 panel 可视高度自然展开" 在 `ValidationSetupScreen` 与 dedicated panel 是两个不同 layout，哪个 panel 的可视高度作为 truth？
   - `ValidationSetupScreen` 归 PR-01，PR-06 §1.5 写得明白 "setup 页 layout 仍归 PR-01"。那 detailed mode 的 row budget 由 PR-06 presenter 控制，还是 PR-01 layout 容器控制？
2. PR06 §6.3 还提供了一张 `ValidationOverlaySummaryPresenter` owner 边界表，但表里只列了 "row count budget / line length budget / path compaction / section order"——**没有 mode 切换 owner**。如果 caller（PR-01 setup 或 PR-06 in-run renderer）需要切换 mode，谁负责传 `displayMode` 参数？
3. compact mode 12 行硬上限 vs detailed mode 32 行 soft cap 之间，如果 evidence 输入有 40 条，到底 fold 几条？谁定义 fold 算法？

**修复方向**：

PR06 §6.3 增补 mode owner 表：

| Concern | Owner |
| --- | --- |
| display mode selection (caller 是 in-run renderer 还是 setup/panel) | caller |
| `compact` mode hard cap 12 rows | `ValidationOverlaySummaryPresenter` |
| `detailed` mode hard cap 32 rows OR panel-height-driven natural expansion | `ValidationOverlaySummaryPresenter`（hard cap）；layout 容器（natural expansion 时） |
| fold algorithm (compact 与 detailed 共用) | `ValidationOverlaySummaryPresenter` |
| ValidationSetupScreen layout | PR-01 |
| in-run overlay layout | PR-06 |

显式规定 detailed mode 的 32 行 cap **始终生效**，layout 容器只决定可见区域是否裁切（≥32 行裁切）；不允许 layout 容器倒推 presenter cap。

### 4.2 [High] Section order "key warnings → evidence → preset → ..." 已修复，但 compact mode 与 detailed mode 顺序是否需要差异化？

**证据锚点**：

PR06 §6.3 section order：
> key warnings -> evidence summary -> preset -> seed -> zone/floor -> active packs -> namespaces -> overlay ops -> touched content；compact mode 必须让 warning/evidence 在 fold 前可见

round-1 §4 提出过"warning/evidence 提前"，已采纳。但 detailed mode 是否仍用同一顺序？

**问题**：

1. detailed mode 的目标用户是 mod 开发者，他们读 evidence path 的频率 > 读 warnings。compact mode 顺序应该把 warning 提前（in-run 玩家最关心警报），detailed mode 顺序可能要把 evidence 提前（开发者最关心证据链）。
2. PR06 §6.3 写"compact mode 必须让 warning/evidence 在 fold 前可见"——隐含 detailed mode 也用同序，但没有显式说。
3. mod 开发者反向用工作流：他们看到 in-run compact mode 出现 warning → 切到 detailed mode 读完整 evidence path。这是 ValidationOverlaySummaryPresenter 设计目标的核心 user story，应该在 PR06 §6.3 显式提到。

**修复方向**：

PR06 §6.3 增补一段：

```
Compact mode order:
  key warnings -> evidence summary -> preset/seed/zone -> active packs -> namespaces -> overlay ops -> touched content

Detailed mode order:
  key warnings -> evidence summary (full path expansion) -> active packs (with namespaces inline) -> overlay ops -> touched content -> preset/seed/zone

Detailed mode rationale: developer reads evidence path top-to-bottom; preset/seed/zone moves to the bottom as bookkeeping.
```

### 4.3 [Medium] Compact mode 12 行 与 in-run HUD 区高度的真实冲突未量化

PR06 §6.3 "compact mode 主体最多 12 行"。但 1280×720 debug client 与 1920×1080 packaged app 的 in-run HUD 区高度不同：

- 1280×720：HUD 区常占用屏幕下 1/4 ≈ 180px，按 14px 行高 = ~13 行 HUD 内容；overlay 12 行刚好 cover HUD 区。
- 1920×1080：HUD 区常占用屏幕下 1/5 ≈ 216px，按 14px 行高 = ~15 行；overlay 12 行可能不 cover。

**问题**：

"12 行"作为统一硬上限，在不同 viewport 下行为差异大。round-1 §4 已经讨论过 12 行的合理性，但没结合 viewport 量化。

**修复方向**：

1. PR06 §6.3 增补 "compact mode max rows = min(12, viewport height / line height / 4)"——overlay 永不超过 viewport 高度的 25%。
2. 或者：保留 12 行硬上限，但增补 "compact mode 显示位置必须避开 HUD 主要状态条 / minimap / 日志区"，由 layout 容器（PR-06 in-run renderer）保证。

### 4.4 [Medium] CJK column-aware budget 已采纳但未规定 measurement source

PR06 §6.3：

> max line length: 单行 display text 最多 96 visual columns；CJK 字符按 2 列、combining mark 按 0 列估算。若暂未实现 column-aware width，CJK locale 使用 144 chars fallback。

**问题**：

1. "CJK 字符" 范围是 Unicode 哪些 block？仅 CJK Unified Ideographs (U+4E00–U+9FFF) 还是包括 Hiragana / Katakana / Hangul / Fullwidth Forms？
2. "combining mark 按 0 列估算" 是 grapheme cluster 边界处理还是 Unicode general category Mn / Mc？
3. CJK locale 怎么判定？`Locale.getDefault()`？运行时 setting？

**修复方向**：

1. PR06 §6.3 增补 column-width 算法源：
   - 推荐 East Asian Width property（Unicode UAX #11）的 W / F / A category 按 2 列，N / Na / H 按 1 列，combining mark / format / Cc / Cn 按 0 列。
   - 或参考 wcwidth (POSIX) 算法，明确列出参考实现。
2. CJK locale 判定 = `Locale.language in {"zh", "ja", "ko"}`。
3. `ValidationOverlaySummaryPresenterTest.usesColumnAwareBudgetForCjkText` 必须包含 fullwidth ASCII / combining mark 边界 case，否则容易写出"只测了中文汉字"的伪覆盖。

---

## 5. Frozen profession locked card：hover/tooltip 合同空缺

### 5.1 [High] `locked-with-coming-soon-label` 的 hover tooltip 合同未冻结

**证据锚点**：

PR06 §6.6 三档 disposition：
1. hidden (preferred);
2. locked-with-coming-soon-label;
3. locked-with-fallback-only (forbidden)。

round-1 §5 提议过 `productMessagingKey` locale token + "未开放 / Development Preview" 文案，已经在 PR06 §3 frozen profession exclusion schema 中采纳。

**遗漏点**：

1. locked card 是**可悬停的 UI 元素**——ToME-likes 玩家选职业时会逐个 hover 读说明。当前 PR06 没规定 hover 时弹什么：
   - 弹一个 stub "Coming soon" tooltip？
   - 弹一个完整 profession description（强化"完整版"误读）？
   - 不弹（玩家以为 UI 坏了）？
   - 弹 dev log / roadmap 链接（破坏沉浸）？
2. PR06 §6.6 与 §3 frozen schema 字段都没列 `tooltipContentKey` / `hoverDisposition` 字段。
3. 即便 `productMessagingKey` 已经在 card 文字里写了 "Development Preview"，hover 时 tooltip 内容仍可能不一致——因为 tooltip 通常由不同的 component 渲染（`ProfessionCardTooltip` vs `ProfessionCard`），两者数据源若不统一会出现 "card 写 Development Preview / tooltip 写完整 description" 这种 onboarding 灾难。

**修复方向**：

PR06 §3 frozen profession exclusion schema 表新增字段：

| Field | Rule |
| --- | --- |
| `hoverTooltipContentKey` | required when `playerVisibility == locked-with-coming-soon-label`；locale token 必须包含 "not in current build" / "未在当前版本提供" 措辞，不得复用正式 profession description token |
| `hoverInteraction` | `tooltip-only` (默认) or `disabled-no-tooltip`；不得使用 `click-redirects` 或 `expandable-description` |

§6.6 修复方向增补：

> 当 `playerVisibility = locked-with-coming-soon-label` 时，hover 必须只触发 `hoverTooltipContentKey` 引用的 tooltip，**禁止**复用 release/dev playable profession description token；tooltip 必须包含明确"未在当前版本提供"措辞与 muted accent，并附"剩余职业可继续探索"提示。

测试 `ProfessionSelectionPresenterTest.frozenProfessionHoverShowsDevelopmentPreviewTooltipOnly`。

### 5.2 [Medium] §6.6 disposition `locked-with-fallback-only` 标为 forbidden，但代码层缺 lint enforcement

**证据锚点**：

PR06 §6.6 "Forbidden: show a polished locked card with dark-v1 fallback only"。

**问题**：

forbidden 是文档级声明，但 `ProfessionSelectionPresenter` / `ProfessionCard` 渲染时只检查 `visibleFallbackKey` 是否非空——并不阻止"无 productMessagingKey + 有 visibleFallbackKey" 的组合，**这正是 forbidden disposition**。

**修复方向**：

`darkManifestCoverageLint final-full` 必须新增校验：若 frozen exclusion entry 同时 `playerVisibility != hidden` 且 `productMessagingKey == null`，必须 fail；`DarkSpriteSheetPipelineScriptTest.finalFullRejectsLockedWithFallbackOnlyDisposition` 验证。

PR06 §6.6 末尾追加："`darkManifestCoverageLint final-full` 必须强制：`playerVisibility != hidden` 时 `productMessagingKey` 必填；缺失即 fail，禁止留到 manual review 阶段才发现。"

---

## 6. Accent Strength Budget：操作化定义缺失

### 6.1 [High] §6.5 "strength" 是抽象计数，没有可测量定义，reviewer 之间会分歧

**证据锚点**：

PR06 §6.5 给出 ember/cyan strength budget 表，配 `strength 3 / 2 / 1` 标签与 max-count 限制。round-1 §8 提议的 accent budget 已采纳。

**问题**：

`strength` 当前是抽象单位，没规定怎么测量：

- 是 accent **面积**？（quest active marker 占 6×6 = 36px² 算 strength 3）
- 是 accent **饱和度**？（饱和度 ≥ 70% 算 strength 3）
- 是 accent **频闪**？（pulse 频率 ≥ 1Hz 算 strength 3）
- 是 accent **位置权重**？（中央 / 焦点位算 strength 3）

不同 reviewer 在 manual whitebox §8.8 "目测同屏 strength-2-or-higher cue 总量不超过 4" 时会有不同认知，导致 PR06 close gate 时争议。

**修复方向**：

PR06 §6.5 增补操作化定义表：

| Strength | Definition |
| --- | --- |
| 3 | accent area ≥ 24px² **AND** saturation ≥ 60% **AND** (pulsing OR steady-glow) |
| 2 | accent area 8–24px² OR saturation 40–60% with steady fill |
| 1 | accent area < 8px² OR saturation < 40% OR transient (< 0.5s) |

或者更简单的方式：在 `UI/manual-records/dark-uiux-pr06-overview-screenshot.md` 模板里附**一张 reference 同屏截图**作 calibration anchor——所有未来 reviewer 比对参考图判定。

manual whitebox §8.8 改写为："对比 `UI/manual-records/dark-uiux-pr06-overview-screenshot.md` reference 截图判定同屏 ember/cyan accent；若数量目测超过 reference + 50% 即触发 sheet/tone 修正。"

### 6.2 [Medium] Accent budget 与 telegraph glow 共享 cold-cyan，未规定优先级

**证据锚点**：

PR06 §6.5 表：

- `cold-cyan selected slot edge`: strength 3, max one focus chain；
- `cold-cyan focused row / telegraph glow`: strength 2, max two combat tiles；
- `cold-cyan learnable badge / tooltip edge`: strength 1。

**问题**：

战斗中同时出现：selected slot edge（玩家在 inventory）+ telegraph glow（敌方 telegraph）+ tooltip edge（玩家 hover 装备）= 3 + 2 + 1 = 6 strength 总和，但表只规定**每个角色**的 max，没规定**总和** cap。round-1 §8 提议的"≤4 visible strength-2-or-higher items"已在 §8.8 manual whitebox 落地，但 §6.5 budget 表本身没明示总和上限。

**修复方向**：

PR06 §6.5 表后追加："**aggregate cap**: 同屏 ember + cyan strength-2-or-higher cue 总和 ≤ 4；超过则 telegraph glow 优先保留（战斗反馈优先级最高），其次 selected slot edge / quest active marker，learnable badge 与 tooltip edge 主动降级或闪烁淡出。"

---

## 7. Fallback / missing / hidden / debug sheet：四 quadrant 验证未冻结

### 7.1 [Medium] `r09-fallback-debug` 单 sheet 承载 4 类语义，contact-sheet QA 没要求 quadrant 验证

**证据锚点**：

PR06 §3.8：

> 当前 manifest fallback / hidden / resource-debug 主路径估算约 25-30 cell，`r09-fallback-debug` 的 64 cell 容量足够作为首选。

design notes §5.1：

> 这四个 bucket（missing/fallback/hidden/debug）不能塌进同一视觉语言。

**问题**：

1. 25-30 cell 分到 4 类，每类 ~6-8 cell。contact-sheet QA 时若按 sheet 顺序读 cell，不容易发现"hidden 与 fallback 视觉互相像了"。
2. 当前 PR06 没要求 contact-sheet QA 按"4 类 quadrant"排版——这意味着 QA 时只能逐 cell 看，不能 side-by-side 验证四类语言的 visual distinctness。

**修复方向**：

PR06 §3 raw sheet 生成交接段落增补一行：

> `r09-fallback-debug` 与 `r09-rejected-polish` 的 contact-sheet QA 排版必须按"4 类 quadrant"组织：左上 `missing_visual` sentinel，左下 fallback variant，右上 hidden/secret，右下 debug-only；每 quadrant ≥ 4 cell。side-by-side QA 报告必须明确写出每个 quadrant 与相邻 quadrant 的视觉差异点（frame shape / text marker / accent color / motif），不允许只写"all dark-v1 era OK"。

### 7.2 [Medium] `missing_visual` sentinel 在 packaged app 不应出现，但 PR06 没有 lint 校验

**证据锚点**：

PR06 §6 fallback inventory 表：

> `missing_visual` 在 packaged app 出现即 PR06 failure。

**问题**：

这是文档级声明，但 lint / coverage 都没有 enforce：

- `goldenScreenshot` 可能不触发 missing path；
- `clientSmoke` 可能跑的是 dev client；
- PR-07 packaged app 评测时才发现 sentinel 出现——已经晚了。

**修复方向**：

PR06 §6 增补："`darkManifestCoverageLint final-full` 必须新增 `packagedAppMissingSentinelAuditPath` 输入：扫描 packaged app 启动后的 `client/build/reports/golden/` 与 `UI/manual-records/dark-uiux-pr07-*.md`（如果存在）中是否引用 `missing_visual` sprite；若 packaged app evidence 引用 sentinel，coverage 必须 fail。"

或者更简单：PR06 §6 manual evidence 要求里增补一项："packaged app smoke test 截图（PR-07 evidence）必须人工确认无 `missing_visual` sentinel 出现；如出现，必须回到 PR-06 修补 inventory，不能在 PR-07 静默处理。"

---

## 8. Long-session 验证：60min 仍可能漏 cross-surface 切换疲劳

### 8.1 [Medium] §6.7 "≥60min 连续游戏" 缺职业切换 / surface 切换 minimum

**证据锚点**：

PR06 §6.7：

> PR-06 manual record must include a `>= 60min` continuous play pass covering combat, inventory, quest, talent, and validation overlay transitions.

§8 manual whitebox 第 6 条与第 9 条已有 "至少 3 处疲劳点 + 切换至少 3 个职业" 要求。

**遗漏点**：

1. 60min 连续游戏在 Roguelike 里可能是 "1 个 run / 同一职业 / 同一节奏 / 同一区域"——这种 path 不会触发跨职业疲劳与跨 surface 切换疲劳。
2. ToME-likes 玩家典型 session 含 multi-run（1-3 次 death + restart）、build 切换、装备 swap、地图区域跃迁。dark-v1 在不同 zone biome / lighting 下视觉 fatigue 模式可能不同。
3. §8.9 "切换至少 3 个职业" 与 §8.6 "≥60min 连续游戏" 没有显式 link——可能开发者完成 60min run 后单独跑 3 职业切换，两件事不在同一 session 内。

**修复方向**：

PR06 §6.7 增补 minimum session structure：

```
≥ 60min single-session play 必须涵盖：
1. ≥ 2 次职业切换（profession selection -> talent panel -> combat）
2. ≥ 3 次战斗（不同强度：trash / elite / boss）
3. ≥ 2 次背包/装备整理
4. ≥ 1 次 quest 完成 + 1 次 quest 接受
5. ≥ 1 次 validation overlay 触发（debug client）
6. ≥ 1 次区域跃迁（不同 biome / zone）

如果一次 run 无法覆盖以上全部，必须 across multi-runs within the same session；session 总时长仍 ≥ 60min。
```

### 8.2 [Medium] Long-session record 缺签字流程

PR06 §6.7 与 §8 第 6 条要求 manual record，但没规定 reviewer 二次签字。

**问题**：

manual record 是开发者自己写自己读的文本文件——风险是：写得太轻（"未发现问题"）或太重（事无巨细写半页）都不能闭环。

**修复方向**：

PR06 §6.7 / UI06-M10 manualEvidence 列增补：

> manual record 必须包含 (a) play session log（时间戳 + 关键事件），(b) screenshot at minute 0 / 30 / 60，(c) PR description 中 reviewer 二次确认（"I have read the long-session record and confirm the 3+ fatigue findings are recorded"）。

---

## 9. `r09-rejected-polish` 与 rework PR 的所有权矛盾

### 9.1 [High] §3.10 rework PR 与 §3.7 `r09-rejected-polish` 单 owner 的冲突未澄清

**证据锚点**：

PR06 §3.7：

> `r09-rejected-polish` owner 固定为 PR-06，只承接 PR-03/05/06 coverage artifact 已记录的 rejected cell；PR-07 只能返修这些记录，不重新拥有 sheet。

PR06 §3.10：

> 任一 sheet 累计 `>= 2` 轮 contact-sheet rejection，或同一 sheet 出现两次以上 player-visible cell QA reject，必须在进入 golden/manual evidence 前决定拆出独立资源修复 PR（命名规则 `r0X-<family>-rework`）。

**问题链**：

1. 假设 `r09-rejected-polish` 在 contact-sheet QA 第 2 轮被 reject——这是 §3.10 的触发条件。按 §3.10 必须拆 `r09-rejected-polish-rework` PR。
2. 拆 rework PR 后：
   - rework PR 的 owner 是谁？还是 PR-06？还是新 PR？
   - rework PR merge 前 PR-06 主 PR 能否 close？
   - rework PR 是否也要走 final-full coverage gate？还是 owner-scope？
3. §3.10 的命名规则 `r0X-<family>-rework` 与 §3.2 现有 sheet ID 命名空间没明示关系。
4. §3.7 "PR-07 只能返修这些记录" 与 §3.10 "rework PR 必须在 PR-07 前 merge" 是同一时间窗口的两条约束——但 rework PR 的 owner、close gate、artifact 都没指定，等于 PR-06 close 时有一个"隐形的、未冻结的 PR 在外面跑"。

**修复方向**：

PR06 §3.10 增补：

```
rework PR 合同：
- owner: 新分支 / 新 PR，但 owner role 仍是 `assets`（与 PR-06 的 r09-* sheet owner 一致）。
- close gate: rework PR 必须使用 final-full coverage gate 的 owner-scope 模式（只验该 sheet 的 cells），不允许 registry-only。
- artifact: rework PR 必须产出新 contact-sheet QA report + 替换原 sheet 的 inventory 行；inventory 中 sheetId 字段 update 为 rework sheetId，原 sheetId 进入 historicalSheetIds 字段。
- 时间窗: rework PR 必须在 PR-06 主 PR close 之前 merge；PR-06 主 PR 不允许在 rework PR 未 merge 时进入 close。
- PR-07 边界: PR-07 只 audit rework PR 合入后的 final state，不重新接管 rework sheet。
```

### 9.2 [Medium] `historicalSheetIds` 或等效字段在 inventory schema 未定义

PR06 §6.1 inventory schema 表当前没有"被替换的 sheet 历史 ID"字段。如果 rework PR 替换 sheet，原 sheetId 在 inventory 里直接被 overwrite，coverage report 无法追踪历史。

**修复方向**：

`UI/sprite-sheets/dark-v1-final-full-inventory.json` schema 增补：

| Field | Rule |
| --- | --- |
| `families[].keys[].historicalSheetIds` | optional list；记录该 key 曾经 reside 的 sheetId（含 rework 替换历史） |
| `families[].historicalSheetIds` | optional list；记录该 family owner sheet 的替换历史 |

---

## 10. Difficulty 视觉路线图：dark-v1 lighting 兼容性未量化

### 10.1 [Low] design notes §12 给了 material 升级路径，但没给 lighting / contrast budget

**证据锚点**：

design notes §12 Difficulty Visual Roadmap：

| Difficulty tier | Visual escalation |
| --- | --- |
| normal | plain worn stone or iron mark |
| hard | single crack, thorn, or edge notch |
| nightmare | cursed iron, ember crack, or double-spike motif |
| madness / insane | ringed or fractured sigil with controlled accent; still low saturation |

**问题**：

1. 4 档 material 升级是 shape / texture 维度，没规定 lighting 与 contrast 维度的允许范围。
2. dark-v1 era 本身偏 charcoal / low saturation；从 normal 到 madness，如果 contrast 提升 ≥ 2 stop（亮度差 1:8 以上），即使保留 charcoal palette 也会破坏 era 一致性。
3. round-1 review §11 已经提示过"不得通过提高红/亮饱和度或加血腥元素表达难度递增"——但 design notes §12 落地后仍未把"允许的 contrast 差异"量化。

**修复方向**：

design notes §12 表后增补：

```
跨 difficulty tier 的 visual contrast budget:
- 4 档间总 contrast 差 ≤ 1 stop（亮度比 1:4 以内）；
- 同 tier 内 silhouette / outline weight 必须保持 dark-v1 era 常量；
- 饱和度差异通过 accent 形状（spike / ring / fracture）承载，禁止通过 hue rotation（红 → 紫 → 黑）表达难度。
```

### 10.2 [Low] Difficulty 当前只有 `normal`，未来扩展时缺 trigger condition

PR06 §6.1 与 §6 切换表只写 `difficulty.normal.icon`。`difficulty.hard.icon` 等未来 key 的引入条件没规定：

- 是 difficulty 数据进 `core` 时同步引入？
- 还是 difficulty UI 落地时单独 PR？
- 何时算 player-visible？

**修复方向**：

design notes §12 末尾增补："`difficulty.<id>.icon` key 引入条件：当 `core` 或 `game` data layer 引入新 difficulty enum value 且 difficulty 可在 character selection / dungeon entry 等 player-visible surface 选择时，必须同步在 PR06 之外开新 manifest contract change PR；不允许预创建未使用的 difficulty icon key。"

---

## 11. Roguelike / 类 ToME 经验维度的二审

### 11.1 [Medium] Status icon 与 skill icon 同屏 silhouette weight 检测未冻结到 contact-sheet 阶段

**证据锚点**：

round-1 §14.1 已提议 "contact sheet QA 同时摆放 status / skill 各两枚，验证扫描成本"。design notes §7 Contact-Sheet QA Rubric 表 "Taxonomy" 行覆盖了 "skill/status/quest/fallback 可互换 = block"。

**遗漏点**：

`r08-skills-*` sheet 与 `r09-status-damage` sheet 是**两张独立 contact sheet**——单独 review 每张时不易发现"skill 与 status 跨 sheet 风格收敛"。

**修复方向**：

PR06 §3 raw sheet 生成交接段落增补："skill (r08) 与 status/damage (r09) sheet 的 contact-sheet QA 必须做 cross-sheet side-by-side：随机抽 4 个 skill cell + 4 个 status cell + 4 个 damage_type cell，并排排版，验证 32px 下三家族 silhouette weight 不收敛；该 cross-sheet QA 是 r08-skills-* 与 r09-status-damage 共同的 PR-06 close 前置。"

### 11.2 [Medium] Profession icon 3 size context（≥128 / 48 / 16-24px）未冻结

**证据锚点**：

round-1 §14.3 已提议 profession icon 在 selection / talent header / status log 三 size 下保持一致。PR06 §6.1 表 profession icon 行 required consumer 已改为 "profession selection consumer test + talent header/log consumer test + ManifestResolveTest"，落地了多 consumer test 要求。

**遗漏点**：

但 PR06 没规定 profession icon 的**多 size 渲染合同**——同一个 PNG 在 128px / 48px / 24px 三种尺寸下是否使用同一 source 缩放，还是有 multi-size sprite atlas？dark-v1 era 32px 优化的 silhouette 在 24px / 128px 可能效果不同。

**修复方向**：

PR06 §3 资源范围增补："`icon.profession.*` 必须验证在 128 / 48 / 24 三种尺寸下的可读性；contact-sheet QA 必须包含三 size 缩略对比；如某 size 不可用，必须在 sheet plan 中指定 multi-size variant key（如 `icon.profession.<id>.lg` / `.md` / `.sm`），不允许只生成 128px 期望 GPU 缩放兜底。"

### 11.3 [Medium] Build 多样化下 sheet 容量上限：r08 三张 sheet × 64 cell = 192 cell vs talent 库实际规模

**证据锚点**：

round-1 §14.2 估算 talent icon 数量级 150-200，sheet 容量 192 接近上限。PR06 §3.9 已采纳 80% nearCapacityWarn 阈值。

**遗漏点**：

但 PR06 没规定"哪些 cells 优先 reserve for 未来扩展"——意味着 r08 sheet 填充顺序若按 alphabetical / sheetId 顺序，先填的是 vanguard / berserker，最后填的是 spellblade（dev playable）。如果 r08-arcanist-spellblade 满了，后续 spellblade 子流派扩展无 cell 可用。

**修复方向**：

PR06 §3 资源范围增补："`r08-skills-*` sheet 必须保留 ≥ 15% cell capacity（~10 cell）作为 expansion reserve；reserve cell 必须在 contact-sheet QA 报告中显式标记，不允许后期"反正空着先塞别的"。reserve 释放需经 design director 单独审批，目的是给未来子流派 / talent 扩展留 expansion sheet 之前的缓冲。"

### 11.4 [Medium] Skill icon vs talent visual：r08 sheet 同时承载技能图标与 talent portrait visual

**证据锚点**：

PR06 §6.1 family 表：
- skill icon (`icon.skill.*`, `talent.*.icon`) → `r08-skills-*`
- talent visual (`talent.*.visual`) → `r08-skills-*` or explicit pending/exclusion

**问题**：

`icon.skill.*` 是 32-48px 操作图标，`talent.*.visual` 是大尺寸 portrait visual（典型 128-256px）。**两类 size 完全不同**的资源同张 sheet：

1. sheet 切分时，portrait 占据 multiple cell，icon 占据 single cell——sheet plan 复杂化。
2. contact-sheet QA 时小 icon 与大 portrait 并排，size 差异让 "32px silhouette readability" 这一规则失去检验意义（portrait 不需要 32px 可读）。
3. design notes §4 Icon Taxonomy 表只规定 skill icon 的 shape language，没规定 talent portrait 的 visual language——portrait 通常需要叙事 / 情绪感，不同于 icon 的功能感。

**修复方向**：

1. PR06 §3 资源范围增补："`talent.*.visual` portrait 在 sheet plan 中必须使用独立的 portrait-size cell 配置（如 2×2 cell merge），与 `icon.skill.*` / `talent.*.icon` 的 1×1 cell 在同张 sheet 时必须用 sheet plan 显式标记 size group。"
2. design notes §4 Icon Taxonomy 表后追加一段 "Talent portrait visual" 行，给出与 skill icon 不同的 shape language（叙事场景 / 人物半身 / 情绪 lighting），避免 talent portrait 被画成"放大的 skill icon"。

### 11.5 [Medium] Telegraph status 的 priority 900+ 顺位与 fold 行为

**证据锚点**：

`StatusPresentationModel.kt:91-94` 给 TELEGRAPH `priority = 900 + dangerLevel * 20 + previewTurnsInverse(...)`，明显高于 DEBUFF/BUFF/NEUTRAL。

**问题**：

1. ToME-likes 战斗中 telegraph 是 "敌方下一回合动作预告"——它出现在 HUD status 行的语义是"敌人即将做什么"，不是"你身上有什么"。把 telegraph 与 status 共用 HUD slot 是 game-feel 设计选择（合理：节省 HUD 空间，集中读屏），但**PR06 没明确规定 telegraph 是否进入 status icon 行的 fold 名单**。
2. 高密度战斗（多 telegraph + 多 debuff）下，fold 后 telegraph 全部保留，玩家自己的 buff 全部 fold——这可能是设计 intent，但需要写进 disposition contract。
3. telegraph 视觉上常带"敌方意图"色（红 / 橘 / 紫），与 player buff / debuff 的"状态色"语义不同——design notes §4 Icon Taxonomy 表把 telegraph 暗含在 status family，没单独列出。

**修复方向**：

1. design notes §4 Icon Taxonomy 表增补 "Telegraph (enemy intent)" 行，shape language = "ranged arc / aim line / impact marker"，与 status badge 区分。
2. PR06 §6.2.1 fold 规则增补："telegraph status 永远不在 fold 之列（max 10 cap 内 telegraph 先保留），但 telegraph 数量 ≥ 5 时必须在 fold badge 上独立显示 telegraph 计数（与一般 status fold 计数分开）。"
3. 同步在 §6.5 accent budget 表把 "telegraph glow" 的 cold-cyan 改成 "telegraph indicator"，明确它的颜色非 cyan 即 ember（与 dangerLevel 关联），避免与"focused row" cyan 同色混淆。

### 11.6 [Low] Frozen profession 在 talent panel header 的视觉处理未规定

**证据锚点**：

PR06 §6.6 frozen profession disposition 集中在 profession selection 入口处理（hidden / coming-soon / fallback-only）。但 frozen profession 的 **talent panel** 在 dev / debug client 可能仍然 accessible（开发者 debug 用），那时 panel header 显示什么 profession icon？

**修复方向**：

design notes §4 Icon Taxonomy 表增补 "Frozen profession panel header" 行，shape language = "muted profession crest with `[DEV]` watermark or fallback frame"，与正式 profession header 区分。或 PR06 §6.6 增补一句："frozen profession 在 dev/debug client 内的 talent panel header 必须使用 `visibleFallbackKey` 渲染且叠加 `ui.dev-only.banner` 文案；不允许复用正式 profession icon。"

---

## 12. Acceptance Matrix UI06-M04 与 UI06-M08 的 fastCheck 重叠

### 12.1 [Medium] UI06-M04 / UI06-M08 fastCheck 都包含 `TileRendererCanvasTest`，分工不清

**证据锚点**：

| requirementId | fastCheck |
| --- | --- |
| UI06-M04 | `StatusPresentationModelTest`, `StatusIconResolverTest`, `TileRenderModelTest`, `TileRendererCanvasTest` |
| UI06-M08 | `TileRendererCanvasTest`, contact-sheet side-by-side QA |

**问题**：

`TileRendererCanvasTest` 同时挂在 M04 与 M08 下——失败时归属不清，reviewer 无法快速定位是 quest summary row 问题还是同屏一致性问题。

**修复方向**：

1. M04 fastCheck 拆细：`TileRendererCanvasTest.shellQuestSummary*` 系列归 M04；`TileRendererCanvasTest.sameScreenOverview*` 系列归 M08。
2. 或者：M08 fastCheck 改为新建专用 test class `SameScreenOverviewTest`，与 `TileRendererCanvasTest` 分离。

### 12.2 [Medium] UI06-M10 long-session readability 的 ownerGate 是 "PR-07 re-audit" 但无具体 evidence 引用

**证据锚点**：

| requirementId | ownerGate | manualEvidence |
| --- | --- | --- |
| UI06-M10 | PR-07 re-audit | `UI/manual-records/dark-uiux-pr06-long-session-fatigue.md` |

**问题**：

"PR-07 re-audit" 是 gate 还是任务？re-audit 失败时 PR-06 是否需要返修？还是 PR-07 单独修复？manualEvidence 只指向 PR-06 自己写的 manual record，PR-07 re-audit 的结果归档在哪里没规定。

**修复方向**：

UI06-M10 manualEvidence 列扩展为：`UI/manual-records/dark-uiux-pr06-long-session-fatigue.md` + `UI/manual-records/dark-uiux-pr07-long-session-reaudit.md`（PR-07 evidence；如发现 PR-06 未覆盖的 fatigue point，必须 link 回 PR-06 PR description 作为 outstanding finding）。

---

## 13. Round-2 严重级别总结表

| 严重级别 | 章节 | Finding 简述 | 一句话原因 |
| --- | --- | --- | --- |
| **Blocker** | §1.1 (B1) | `StatusIconResolver` silent flood：zone effect 全部路由到 missing sentinel | round-1 fix 把 silent drop 换成 silent flood，HUD 在含 zone effect 的任一回合显示 missing_visual sentinel；测试通过 = production broken |
| **Blocker** | §2.1 (B2) | `r09-status-damage` 单 sheet 三家族，damage_type 缺独立 separation rule | status / mutation / damage_type 32px 必然 collapse；design notes §4.2 separation rules 表缺第三方分离行 |
| **Blocker** | §3.1 (B3) | Quest `complete` 与 `activate / progress` 视觉同质 | 任务完成那一帧无视觉反馈，是 Roguelike 探索循环核心成就反馈丢失 |
| **High** | §1.2 | `MISSING_STATUS_ICON_KEY_PREFIX` ad-hoc key shape 不在分母 | per-typeId sentinel 与 `missing_visual` 单点 fallback 之间逻辑空转，测试断言字符串而非 visual |
| **High** | §2.2 | damage_type required consumer "at least one" 仍允许只挑最轻的 path | 没 specify weapon/equipment tooltip 是 minimum，玩家最关心 surface 可能未上屏 |
| **High** | §3.2 | Quest summary row 当前代码仍 text-only | PR06 commit 还没动 `TileRenderModel.buildShell` row 改造，§3 Implementation Order 没拆 sub-step |
| **High** | §4.1 | `ValidationOverlaySummaryPresenter` detailed mode owner 边界模糊 | 32 行硬上限 vs panel-height-driven 自然展开，未指定优先级 |
| **High** | §4.2 | detailed mode section order 是否与 compact 一致未规定 | mod 开发者读 evidence 频率 > 读 warning，detailed mode 应该 evidence 提前 |
| **High** | §5.1 | Frozen profession locked card hover tooltip 合同空缺 | tooltip 内容若复用正式 profession description 会强化"完整版"误读；productMessagingKey 与 tooltip data source 可能不一致 |
| **High** | §6.1 | Accent strength 无可测量定义 | reviewer 之间 strength-2-or-higher 认知分歧，close gate 会争议 |
| **High** | §9.1 | rework PR 与 `r09-rejected-polish` owner 冲突 | rework PR 的 owner / gate / artifact / 时间窗都没规定，PR-06 close 时有"隐形 PR 在外跑" |
| **Medium** | §1.3 | `+N more` fold badge locale / visual / hover 合同未冻结 | fold badge 是常驻 UI 元素，不能是临时文字 |
| **Medium** | §1.4 | Fold priority 顺序未写进 disposition contract | telegraph > debuff > zone > buff 顺序合理但无 anchor，未来调整 priority 无 QA 拦截 |
| **Medium** | §1.5 | mutation vs status separation 未下沉到 contact-sheet QA rubric | design notes §7 QA rubric 表缺这一行，QA 时只能逐 cell 看 |
| **Medium** | §3.3 | `advance` token 语义未明示 | `QuestSummaryIconResolver` mapping 表覆盖了 4 token 但 advance 视觉权重需先 audit 触发点 |
| **Medium** | §4.3 | Compact mode 12 行 vs viewport 高度未量化 | 1280×720 与 1920×1080 行为差异大，"12 行" 单一硬上限不通用 |
| **Medium** | §4.4 | CJK column-aware budget 算法源未规定 | "CJK 字符按 2 列" 范围不明，combining mark 处理无参考实现 |
| **Medium** | §5.2 | `locked-with-fallback-only` forbidden 文档级声明缺 lint enforcement | darkManifestCoverageLint 未强制 productMessagingKey 必填 |
| **Medium** | §6.2 | Accent budget 缺 aggregate cap | 同屏多角色 cyan 累加可能突破 4 strength-2-or-higher 总和 |
| **Medium** | §7.1 | `r09-fallback-debug` contact-sheet QA 未要求 quadrant 排版 | 4 类语义共 sheet，逐 cell 看不出 hidden 与 fallback 视觉互相像 |
| **Medium** | §7.2 | `missing_visual` sentinel packaged app 出现无 lint 校验 | "packaged app 出现即 PR06 failure" 只在文档，PR-07 evidence 才发现已晚 |
| **Medium** | §8.1 | 60min long-session 缺 minimum session structure | 1 个 run / 1 个职业 / 1 种节奏的 60min 不暴露跨 surface 疲劳 |
| **Medium** | §8.2 | Long-session record 缺 reviewer 签字 | 开发者自己写自己读 = 写得太轻或太重都不能闭环 |
| **Medium** | §9.2 | inventory schema 缺 `historicalSheetIds` 字段 | rework PR 替换 sheet 后无法追踪历史 |
| **Medium** | §11.1 | skill / status / damage_type 缺 cross-sheet side-by-side QA | 单独 review 每张 contact-sheet 不易发现跨 sheet 风格收敛 |
| **Medium** | §11.2 | Profession icon 多 size 渲染合同未冻结 | 128/48/24 三 size 同 source 缩放可能塌 silhouette |
| **Medium** | §11.3 | r08 sheet capacity 缺 ≥15% reserve 强制 | 80% nearCapacityWarn 已采纳，但未规定 reserve 留给谁 |
| **Medium** | §11.4 | r08 sheet 同时承载 32px icon 与 portrait visual | sheet 切分复杂化，contact-sheet QA 失去 32px readability 检验意义 |
| **Medium** | §11.5 | Telegraph status fold 与 accent 合同未明示 | telegraph 是 enemy intent 不是 player state，视觉语义与 status 不同 |
| **Medium** | §12.1 | UI06-M04 / UI06-M08 fastCheck 重叠 | `TileRendererCanvasTest` 失败时归属不清 |
| **Medium** | §12.2 | UI06-M10 PR-07 re-audit evidence 路径未规定 | re-audit 失败时返修责任不明 |
| **Low** | §10.1 | Difficulty 视觉路线图缺 contrast budget | dark-v1 lighting 兼容性未量化 |
| **Low** | §10.2 | Difficulty key 引入条件未规定 | 未来 `difficulty.<id>.icon` 引入时机不明 |
| **Low** | §11.6 | Frozen profession talent panel header 视觉未规定 | dev/debug client 内访问 frozen panel 时 header 行为模糊 |

合计：3 Blocker + 7 High + 19 Medium + 3 Low。

---

## 14. 推荐文档补丁清单（与 §13 一一对应，按章节）

### 14.1 PR06 §2 影响范围

1. 增补 "`StatusIconResolver.resolveIcons` 必须 filter out ZONE_EFFECT，并由独立 row owner 渲染；如果 zone effect 进入 status icon 行，必须新增 `icon.status.zone_effect.*` family 并进 inventory 分母"（§1.1）。
2. 增补 "如保留 `MISSING_STATUS_ICON_KEY_PREFIX`，必须在 PR06 §6.2.1 显式说明它是 resolver-internal log marker，manifestLint / darkKeyRegistryLint 必须忽略 `status.icon.missing.*`"（§1.2）。

### 14.2 PR06 §3 资源范围

1. mutation/status/damage_type 三家族 separation rule 显式列出（§2.1）。
2. raw sheet 生成交接增补 "skill (r08) 与 status/damage (r09) cross-sheet side-by-side QA"（§11.1）。
3. r08 sheet ≥ 15% capacity reserve 强制（§11.3）。
4. `talent.*.visual` portrait size group 在 sheet plan 中独立标记（§11.4）。
5. `icon.profession.*` 多 size 渲染要求（§11.2）。
6. rework PR 合同与 owner 边界（§9.1）。
7. `r09-fallback-debug` / `r09-rejected-polish` contact-sheet QA 4 quadrant 排版（§7.1）。

### 14.3 PR06 §4 职业树联动

无新增（round-1 已覆盖；新版 PR06 §4 已采纳）。

### 14.4 PR06 §6 验收标准

1. §6.1 damage type 行 required consumer 改为 `ManifestResolveTest` and 武器/装备 tooltip and 至少一个浮字/技能预览/抗性 panel test（§2.2）。
2. §6.2 quest mapping 表后增补 row tone disposition（complete = ember accent, 0.5-1.0s）（§3.1）。
3. §6.2 增补 `advance` token 语义注释（§3.3）。
4. §6.2.1 fold badge locale / visual / hover 合同（§1.3）。
5. §6.2.1 fold priority disposition contract（§1.4）。
6. §6.3 ValidationOverlaySummaryPresenter mode owner 表（§4.1）。
7. §6.3 compact / detailed mode section order 差异化（§4.2）。
8. §6.3 compact mode max rows = min(12, viewport*0.25 / line_height)（§4.3）。
9. §6.3 column-width 算法源 + CJK locale 判定（§4.4）。
10. §6.5 strength 操作化定义 + reference 截图 anchor（§6.1）。
11. §6.5 aggregate cap（§6.2）。
12. §6.6 frozen profession hover tooltip 合同（§5.1）。
13. §6.6 darkManifestCoverageLint 强制 productMessagingKey 必填（§5.2）。
14. §6.7 long-session session structure（§8.1）+ reviewer 签字（§8.2）。
15. §6 fallback inventory 表增补 `packagedAppMissingSentinelAuditPath` 校验（§7.2）。

### 14.5 Acceptance Matrix

1. UI06-M04 / UI06-M08 fastCheck 拆细（§12.1）。
2. UI06-M10 manualEvidence 增补 PR-07 re-audit 路径（§12.2）。
3. 可选新增 UI06-M11 = "rework PR 合同合规" owner = `assets`（§9.1）。

### 14.6 design notes（open-design）

1. §4 Icon Taxonomy 表增补 telegraph 行（§11.5）、talent portrait visual 行（§11.4）、frozen profession panel header 行（§11.6）。
2. §4.2 separation rules 表增补 status vs damage type、mutation vs damage type、telegraph vs status 三行（§2.1 / §11.5）。
3. §5 state badge 表增补 "Quest complete" 行（§3.1）。
4. §7 contact-sheet QA rubric 表增补 status vs mutation、damage type vs status/mutation、cross-sheet side-by-side 三行（§1.5 / §2.1 / §11.1）。
5. §12 Difficulty Visual Roadmap 增补 contrast budget + key 引入条件（§10.1 / §10.2）。

### 14.7 §8 人工白盒

1. 第 2 条增补 "zone effect 场景"显式 case（§1.1）。
2. 第 6 条 long-session minimum session structure 落细（§8.1）。
3. 新增 "任务完成 accent 反馈" 一项（§3.1）。
4. 新增 "frozen profession hover tooltip" 一项（§5.1）。
5. 新增 "weapon tooltip damage type icon vs status icon vs skill icon 同屏对比" 一项（§2.1 / §2.2）。

---

## 15. 玩法体验维度的二审补充

### 15.1 战斗循环：zone effect sentinel flood 是 round-1 没找到的最危险体验回归

round-1 review 已经把 silent drop 标为 Blocker，文档与代码都做了响应。但响应方式是"补 sentinel + 补测试"，而不是"修 routing"。结果 silent drop 变 silent flood——HUD 在 zone effect 出现时显示 missing 图标。这正是 round-1 §1.1 警告过的"分母看不见的暗洞"的具体形态。本轮 §1.1 必须在 PR-06 close 前修。

### 15.2 探索循环：quest complete 无视觉反馈是 ToME-likes 真实痛点

ToME / Caves of Qud / Tangledeep 等社区反馈里反复出现"任务完成没看到，过半小时才发现 quest log 已更新"。dark-v1 era 本身偏 muted，accent 是稀缺资源——但稀缺不代表不能用。`complete` 时给一次 0.5-1.0s 的 ember accent 是 dark-v1 era 内最低成本的完成反馈。

### 15.3 成长循环：4 态可分性已在 §4.7 §4.8 落地，无新增

round-1 §3 提议的 secondary cue 已采纳；32px / focused overlay / color-blind 三重检验也写进 §4.8。本轮无新增。

### 15.4 验证循环：双 mode 拆分实施盲区

round-1 §4 双 mode 已采纳；但 detailed mode 的实施 owner、section order 差异化、viewport scaling 三个细节仍未冻结。这是 mod 开发者生态体验的关键合同——如果留到 PR-07，PR-07 没有 ValidationOverlaySummaryPresenter owner。

### 15.5 新手 onboarding：frozen profession hover 合同空缺

round-1 §5 已经把 frozen disposition 落地；但 hover/tooltip 这一交互通道 round-1 没覆盖——而 ToME-likes 玩家选择职业时 hover 是常态动作。`productMessagingKey` 在 card 上有了，tooltip 没保证一致 = 体验仍然破。

### 15.6 长会话沉浸：60min 是好起点但需要 session structure

round-1 §9 提议的 ≥ 60min 已采纳；但 60min "连续游戏" 在 Roguelike 里能用 1 个 run / 1 个职业 / 1 种节奏跑完，不暴露跨 surface / 跨 build 疲劳。本轮 §8.1 增补 minimum session structure。

---

## 16. 与 round-1 review 的关系

| Round-1 Finding | Round-2 状态 |
| --- | --- |
| §1.1 `StatusIconResolver.mapNotNull` silent drop | code fix 完成；但 fix 引入新风险（zone effect sentinel flood），见本轮 §1.1 |
| §1.2 mutation vs status visual grammar | doc fix 完成（§3.5）；本轮 §1.5 提议下沉到 contact-sheet QA rubric |
| §1.3 damage type required consumer "or" | doc fix 完成（§6.1 "and at least one"）；本轮 §2.2 提议进一步 specify minimum |
| §2.1 quest icon generic marker 无 follow-up | doc fix 完成（§6.2.9 follow-up `UI07-quest-typed-icon-mapping`）；本轮 §3.1 提议为 `complete` 增补 row tone |
| §2.2 quest 空状态 / 满状态视觉不一致 | partial fix（§6.2.6 仍是文本）；本轮 §3.1 仍要求 placeholder slot |
| §2.3 同屏 overview 未为 blocking artifact | doc fix 完成（新增 UI06-M08） |
| §3 talent 4 态 marker 退化 | doc fix 完成（§4.7 secondary cue + §4.8 32px / color-blind / focus overlay） |
| §4 validation overlay 双 mode | doc fix 完成（§6.3 双 mode）；本轮 §4.1-§4.3 提议进一步细化 owner / section order / viewport scaling |
| §5 frozen profession 产品信号 | doc fix 完成（§6.6 三档 disposition + §3 schema 增补 productMessagingKey）；本轮 §5.1 提议增补 hover/tooltip 合同 |
| §6 fallback / missing / debug / hidden 共 sheet | doc fix 完成（§6 fallback inventory 表 + visual rule）；本轮 §7.1 提议 contact-sheet QA quadrant 排版 |
| §7 单 PR 8+ sheet 风险耦合 | doc fix 完成（§3.10 rework PR + UI06-M09）；本轮 §9.1 提议 rework PR 合同细化 |
| §8 ember/cyan accent budget | doc fix 完成（§6.5）；本轮 §6.1 提议 strength 操作化定义 |
| §9 long-session readability | doc fix 完成（§6.7 + §8.6 + UI06-M10）；本轮 §8.1-§8.2 提议 session structure + reviewer 签字 |
| §10 CJK column budget | doc fix 完成（§6.3 column-aware + CJK 144 chars fallback）；本轮 §4.4 提议算法源细化 |
| §11 difficulty roadmap | doc fix 完成（design notes §12）；本轮 §10.1-§10.2 提议 contrast budget + key 引入条件 |
| §12 player-visible disposition matrix | doc fix 完成（§6.4） |
| §13 UI06-M04 标题失配 | doc fix 完成（source 已改写 + 新增 UI06-M08） |
| §14.1 contact-sheet skill vs status side-by-side | partial fix（design notes §7 QA rubric 包含 family identity）；本轮 §11.1 提议 cross-sheet side-by-side |
| §14.2 sheet capacity 80% | doc fix 完成（§3.9）；本轮 §11.3 提议 ≥15% reserve 强制 |
| §14.3 profession icon multi-consumer | doc fix 完成（§6.1）；本轮 §11.2 提议多 size 渲染合同 |
| §14.4 status icon overflow fold | doc fix 完成（§6.2.1 fold 10 + `+N more`）；本轮 §1.3-§1.4 提议 fold badge 合同与 priority disposition |

整体评价：round-1 的合同层 finding 大多被采纳，并升级到代码层（StatusIconResolver fix）+ Acceptance Matrix 层（M08-M10）。这是非常好的 review 闭环。

本轮 finding 大部分是 round-1 修复在落地时**没有同时关掉**的次生洞——属于"一个 fix 引入下一个 review 节点"，是健康的 review 迭代过程，不是 round-1 失误。

---

## 17. 结语

PR06 文档经过 round-1 review 后已经是一份**优于行业平均**的开发合同。round-2 finding 大部分是 fix 实施细节、合同与代码错位、game-feel 维度的二阶问题。

但站在游戏开发设计总监立场，本轮 3 个 Blocker 必须在 PR-06 close 前修：

1. **§1.1** `StatusIconResolver` 必须把 ZONE_EFFECT 路由出 status icon HUD 行，否则 packaged app smoke 一旦含 zone effect 就会出现 sentinel flood。
2. **§2.1** `r09-status-damage` 必须为 damage_type 增补 visual separation rule，并在 contact-sheet QA 做 3 家族 side-by-side。
3. **§3.1** quest `complete` row tone 必须有 ember accent 差异化，或显式 named follow-up `UI07-quest-complete-accent`；禁止默认让"任务完成无视觉反馈"作为终态。

7 个 High 应作为 PR-06 close gate 的硬约束（不修不能 close）；19 个 Medium + 3 个 Low 可作为 PR-06 close 时的 outstanding finding（写入 PR description 的 remaining risk），允许在后续 PR / hotfix 单独修复，但不允许**静默继承到 PR-07**。

建议下一步：

1. 召开 30min 的 design-director / system-designer / lead-engineer 三方对齐会，对本轮 §13 严重级别表中 Blocker + High 共 10 项当面决策（accept / mitigate / defer-with-named-followup）。
2. 决策结果回写 PR06 §2 / §3 / §6 / §8 + design notes §4 / §5 / §7 / §12 + Acceptance Matrix。
3. `StatusIconResolver` 必须先按 §1.1 修复 routing 再继续后续测试与 golden 评估——避免 sentinel flood 出现在 golden baseline。
4. 重新跑 `acceptanceContractLint` 与 `darkManifestCoverageLint final-full`。
5. PR-06 close 前 reviewer 必须读完 `dark-uiux-pr06-status-quest-skill-overview` golden + long-session manual record（含 reviewer 二次签字）。

PR06 是 dark UI 全量收口窗口，错过这次窗口的体验问题在 PR-07 没有 owner surface 可以接收。**round-1 已经把工程合同打磨得相当严密；round-2 是把体验合同也同步收紧的最后机会**。

---

## 附录 A：本轮 patch verification 走访的代码事实

本轮在 PR06 doc 之外读取并核实的代码文件：

- `client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt` —— round-1 §1.1 fix 已落地，但引入新风险（§1.1 / §1.2）。
- `client/src/test/kotlin/com/ktome/client/ui/status/StatusIconResolverTest.kt` —— 新增两条 test case，但断言字符串而非 visual 行为（§1.2）。
- `client/src/main/kotlin/com/ktome/client/ui/status/StatusPresentationModel.kt` —— `buildZoneEffect` `iconKey = null` 路径未变（§1.1）。
- `client/src/main/kotlin/com/ktome/client/assets/ManifestResolvers.kt` —— `resolveMissing` 不消费 prefix，直接走 fallback（§1.2）。
- `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:861, 929` —— quest summary 仍是 text-only `TileTextRow`（§3.2）。

未运行任何 Gradle / Python / test 命令；本轮为文档级 design review。

---

## 附录 B：与既有 review 文档的关系

| Review 文档 | 维度 | 轮次 |
| --- | --- | --- |
| `2026-05-09-dark-uiux-pr06-pr-level-standard-review.md` | PR-level 工程合同 | 第一轮 |
| `2026-05-09-dark-uiux-pr06-pr-level-standard-rereview-round2.md` | PR-level 工程合同（gate / inventory / exclusion） | 第二轮 |
| `2026-05-21-dark-uiux-pr06-skills-status-quest-full-manifest-design-director-review.md` | 设计总监 / 系统策划 / 玩法体验 | 第三轮 |
| **本文档** | **设计总监 / 系统策划 / 玩法体验（patch verification + 次生洞）** | **第四轮** |

四份文档应作为互补的合同输入；本文档不重审 round-1 director review 已采纳的合同层修复，专注于"round-1 修复在代码 / 合同细节中没同时关掉的二阶风险" + "round-1 视角未触及的 game-feel 维度"。

# Dark UI/UX PR06 设计总监 / 系统策划 / 玩法体验深审 (Round 3)

**目标文档**：`UI/pr/dark-uiux-pr06-skills-status-quest-full-manifest.md`（已吸收 round-1、round-2 director review 的补丁后的当前版本 + 当前 working tree 代码事实）
**审查立场**：资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
**审查日期**：2026-05-22
**与前轮的关系**：

1. 不重复 `2026-05-09-dark-uiux-pr06-pr-level-standard-rereview-round2.md` 的 PR-level 工程合同维度。
2. 不重复 `2026-05-21-dark-uiux-pr06-skills-status-quest-full-manifest-design-director-review.md`（round-1）与 `…review-round2.md`（round-2）已经入合同或仍在合同里讨论的问题；只对 round-2 提出的修复方向是否真落到 *working tree* 做 *patch verification*，并列出 round-1/2 未触及的 Roguelike / 类 ToME 系统策划与玩法体验维度的次生风险。
3. 本文档对所有 patch 结论严格区分「文档层合同」与「代码事实」，引用 `client/` 实际行号；不靠记忆推断 working tree 状态。

> 重要前置：PR06 文档相对 round-2 已经把 inventory schema、frozen exclusion schema、disposition matrix、accent budget、`r09-fallback-debug` four-quadrant、long-session minimum structure、CJK column budget、rework PR contract、`historicalSheetIds`、§6.2 objective token visual weight 表全部落到合同；这是同行业里相当靠前的文档质量。本轮聚焦三类继续漏掉的体验风险：
>
> 1. **round-2 B1 的二次修复路径自身有合同 vs 代码错位**：working tree 的 `StatusIconResolver.kt` 现在用 `icon.status.<typeId>` 作为 null iconKey 的 fallback，与 PR06 §6.2.1 #1/#2 自相矛盾，且 zone effect sentinel flood 没有被消除，只是改了构造路径；
> 2. **Roguelike / 类 ToME 战斗循环里 telegraph、quest activate、status stack count 等高频玩家路径**没有合同；PR06 当前讨论的几乎都是「manifest 收口」+「同屏一致性」，缺少「玩法体验循环」维度的合同；
> 3. **跨 surface profession identity、tooltip 16–24px readability、save/load/death/character-sheet 的 frozen profession 边界、content pack / mod 的 dark-v1 era 合同**都是 PR06 收口范围内但被忽略的 surface。

---

## 0. 总评 (TL;DR)

PR06 当前版本 + working tree 的体验风险按严重度排序：

**Round-3 三大新 Blocker**：

| 序号 | 章节 | 一句话 |
| --- | --- | --- |
| **B1** | §1 | `StatusIconResolver.kt:30-32` 当前实现把 `iconKey == null` 的 zone effect 强行构造成 `icon.status.<sourceRuleId>` 走 manifest prefix rule，与 PR06 §6.2.1 #1/#2 明文规定的「zone effect 不在 `icon.status.*` 覆盖分母」自相矛盾；同时 round-2 B1 的「sentinel flood + 日志 spam」没有被消除，只是构造路径换了一层皮。 |
| **B2** | §2 | PR06 §6.2.1 #6 把 telegraph 路由进 status HUD 是错误的系统策划决策：类 ToME / Roguelike 体系里 telegraph 是 enemy intent（敌人下一步行动预告），它的语义 owner 是 enemy actor，不是 player actor 的 status；强行进 status icon 行会同时损害「玩家状态扫描成本」与「敌人威胁判断」两条战斗反馈通道，并破坏 §6.5 accent budget 的 telegraph 分配。 |
| **B3** | §3 | round-2 B3 已经把 `complete` token 被压平问题列为 Blocker，但 PR06 §6.2 objective token visual weight 表里 `activate` 仍然是 `muted objective marker; no persistent high accent`——这把「新任务出现」（探索发现节点）压成与 `progress`（计数 +1）同 tone。Roguelike 玩家最珍视的是「发现感」，`activate` 与 `progress` 共享视觉权重等于把发现节点的反馈通道也关掉。 |

**Round-3 High（5 条）**：

1. §4 `icon.profession.*` 的 required consumer 仅覆盖 `profession selection / talent header/log / ManifestResolveTest` 三 surface，类 ToME 实际触点至少包含 character sheet 头像、ally bar、death screen 总结、save/load slot 列表、minimap legend 共 8 surface，PR06 收口缺一半。
2. §5 32px contact-sheet QA 是 sheet 验收锚点，但实际 runtime 的 damage_type icon / status icon 在 weapon tooltip、inventory tooltip、log row 经常以 16–24px 显示——`r09-status-damage` 现行 QA 流程没有 sub-32px readability sample，sub-32px collapse 会在 PR-07 评测时才暴露。
3. §6.1 status `rawBadge` 当前只规定 turnBadge / stackBadge 文本，没规定 stack count `> 99` 与 multi-status 同 typeId 多实例的视觉处理；类 ToME 玩家 build「bleeding stack / disease stack / summoner stack」会在 boss 战触发 99+ stack 行为。
4. §7 PR06 §3 资源合同假设第三方 content pack 不引入新 player-visible visual key，但 K-ToME 已经支持 content pack 注入；PR06 没规定 third-party pack 提交 cartoon / out-of-era icon 时 dark-v1 era 的 disposition——是 reject、stub、还是 fallback？这是 PR06 收口后的「era 漏出口」。
5. §8 frozen profession 当前合同只覆盖 character/profession selection 一处；ToME-like 玩家 save 数据可能 reference frozen profession（旧 save、dev playable demoted、未来 frozen 化），save/load slot、death summary、achievement panel、replay viewer 没有 disposition。

**Round-3 Medium（7 条）**：见 §9–§11，包含 color-blind runtime support、onboarding tooltip scaffolding、quest `armory_key`/`seal_key` 的 game data 端语义合同、PR-06 与 PR-07 交接边界争议、`r09-fallback-debug` 与 `r09-rejected-polish` 的 sheet 容量交叉、`+N more` fold badge tooltip 行为、long-session 与 multi-run 边界。

详见 §1–§11 各章节论证。

---

## 1. Patch Verification：round-2 B1 修复后的二次回归

### 1.1 [Blocker B1] `StatusIconResolver` 的 round-2 修复在 working tree 里又被改了一次，新版本与文档自相矛盾

**代码事实**（`client/src/main/kotlin/com/ktome/client/ui/status/StatusIconResolver.kt:19-38`）：

```kotlin
internal object StatusIconResolver {
    private const val STATUS_ICON_KEY_PREFIX: String = "icon.status."

    fun resolveIcons(
        visualResolver: VisualManifestResolver,
        effects: List<StatusEffectRenderSnapshot>,
    ): List<StatusHudIconModel> =
        StatusPresentationBuilder
            .sorted(effects.map(StatusPresentationBuilder::build))
            .map { presentation ->
                val asset =
                    visualResolver.resolve(
                        presentation.iconKey ?: STATUS_ICON_KEY_PREFIX + presentation.typeId,
                    )
                StatusHudIconModel(
                    asset = asset,
                    presentation = presentation,
                )
            }
}
```

测试事实（`client/src/test/kotlin/com/ktome/client/ui/status/StatusIconResolverTest.kt:13-71`）：当前测试只覆盖两条 case，`null status icon uses visual fallback instead of dropping status` 和 `unknown status icon uses manifest fallback without dropping status`，**两条都假设 fallback 路径正确**，但都没断言「zone effect 不进入 status icon HUD 行」。

**为什么是 Blocker**：

1. **PR06 §6.2.1 #1 明文规定**：「当前 runtime `StatusIconResolver.resolveIcons` 只消费 actor `StatusEffectRenderSnapshot`，不消费 terrain / zone-effect inspect rows；zone effect 若未来进入 HUD，必须新增明确 owner、key family 和 inventory 分母。」
2. **PR06 §6.2.1 #2 明文规定**：「`StatusPresentationBuilder.buildZoneEffect` 当前 `iconKey = null` 不是 `icon.status.*` / `icon.mutation.*` 覆盖分母。」
3. **但当前 `StatusIconResolver.resolveIcons` 实现路径**：
   - `effects` 列表本身只来自 `StatusEffectRenderSnapshot`（符合 §6.2.1 #1）；
   - 但 `StatusPresentationBuilder.build(snapshot)` 内部仍可能产出 `iconKey == null` 的 presentation——例如 `StatusEffectRenderSnapshot.iconKey == null` 时，`StatusPresentationBuilder.build` 没强制拒绝；
   - 一旦 `presentation.iconKey == null`，resolver 强行构造 `icon.status.<typeId>` 走 manifest 解析。
4. **关键问题**：`typeId` 在 status presentation 里可以是 `acid_pool`、`fire_field`、`thorn_trap` 等 zone effect 的 `sourceRuleId`——历史上 `buildZoneEffect` 路径用的 `typeId` 是 `terrainOverride.sourceRuleId`。如果未来 (a) `StatusEffectRenderSnapshot` 被 zone effect 复用，或 (b) game 端某天把 zone effect 序列化成 `StatusEffectRenderSnapshot` 注入 `effects` 列表，**resolver 会自动把 `icon.status.acid_pool` 注入 manifest 查询**，与 §6.2.1 #2 明文约定的「zone effect 不在 `icon.status.*` 分母」**直接对冲**。
5. **更隐蔽的问题**：`STATUS_ICON_KEY_PREFIX + typeId` 是 **resolver 内部硬编码的 family 推断**。它假设「所有走进 resolver 的 null iconKey 都属于 `icon.status.*` family」——但 status presentation 与 mutation presentation 共享同一 resolver 路径，`buildMutation` 路径如果某天出现 null iconKey，会被强行打成 `icon.status.<typeId>` 而不是 `icon.mutation.<typeId>`，**family 串味**。

**round-2 B1 没消除的问题**：

| round-2 B1 标记 | round-3 当前状态 |
| --- | --- |
| `mapNotNull` silent drop | ✅ 已修（不再 silent drop） |
| sentinel flood (`status.icon.missing.*` 与 `missing_visual` 单点 fallback 共享视觉) | ⚠️ 仍存在但换了壳：现在每个 null iconKey 都查 `icon.status.<typeId>` → 通过 prefix rule fallback 到 `missing_visual` → HUD 渲染同 sentinel sprite |
| `MISSING_STATUS_ICON_KEY_PREFIX` ad-hoc key shape 不在分母 | ✅ 已删（改用 `STATUS_ICON_KEY_PREFIX = "icon.status."`） |
| zone effect 路由出 HUD 行 | ❌ **未做**：resolver 没区分 `group == ZONE_EFFECT` 与 `group != ZONE_EFFECT`；只是因为 `effects: List<StatusEffectRenderSnapshot>` 当前不含 zone effect 输入路径，**问题暂时被「数据源不包含 zone effect」掩盖**，但 resolver 自身没拒绝路由 zone effect |

**修复方向（必须在 PR06 close 前完成）**：

1. **代码层**：`StatusIconResolver.resolveIcons` 必须显式按 `presentation.group` 路由：
   ```kotlin
   internal object StatusIconResolver {
       fun resolveIcons(
           visualResolver: VisualManifestResolver,
           effects: List<StatusEffectRenderSnapshot>,
       ): List<StatusHudIconModel> =
           StatusPresentationBuilder
               .sorted(effects.map(StatusPresentationBuilder::build))
               .asSequence()
               .filter { it.group != StatusPresentationGroup.ZONE_EFFECT } // zone effect 不入 HUD
               .map { presentation ->
                   val key = presentation.iconKey
                       ?: keyForFamily(presentation) // family-aware fallback
                   val asset = visualResolver.resolve(key)
                   StatusHudIconModel(asset, presentation)
               }
               .toList()

       private fun keyForFamily(presentation: StatusPresentationModel): String =
           when (presentation.group) {
               StatusPresentationGroup.MUTATION -> "icon.mutation.${presentation.typeId}"
               else -> "icon.status.${presentation.typeId}"
           }
   }
   ```
2. **文档层**：PR06 §6.2.1 #1 增补一行 *runtime contract*：
   > `StatusIconResolver.resolveIcons` 必须在入口处 `filter { group != ZONE_EFFECT }`，并按 `presentation.group` 选 family-aware fallback key（`icon.mutation.<typeId>` 对 `MUTATION` group，`icon.status.<typeId>` 对其它 group）。resolver 内部不允许把所有 null iconKey 一律打成 `icon.status.*` family。
3. **测试层**：必须新增
   - `StatusIconResolverTest.zoneEffectIsFilteredOutOfHudRow`（注入一个伪造的 `group == ZONE_EFFECT` presentation，断言 resolver 返回不含该 presentation 的 list）。当前 test fixture `sampleResolver` 只构造 `StatusEffectRenderSnapshot` 输入，无法触达 zone path；需要 expose 一个 `StatusPresentationBuilder.buildZoneEffect`-equivalent 注入点或 test-only builder seam。
   - `StatusIconResolverTest.mutationNullIconUsesMutationFamilyFallback`（断言 `group == MUTATION` 的 null iconKey 走 `icon.mutation.<typeId>` 而不是 `icon.status.<typeId>`）。
4. **冻结合同**：PR06 §6.2.1 #7 测试名单增补上述两条；否则未来 patch 仍可能 silent regress。

**为什么这是「合同 vs 代码二度错位」**：

round-2 B1 修复的目标是「zone effect 拉出 status icon HUD」+「per-typeId sentinel key shape 不能与 inventory 分母错位」。当前 working tree 完成了第二个目标（删了 `MISSING_STATUS_ICON_KEY_PREFIX`），但用一个更隐蔽的方式重新引入了第一个目标的反面：**resolver 现在假装 zone effect 是 status，因为输入数据源恰好不含 zone effect**。这是「测试通过 ≠ 合同满足」的经典案例，PR06 close 前必须用 group-aware filter 在 resolver 层显式拒绝 zone effect 路径，而不是依赖上游数据流碰巧不灌入 zone effect。

### 1.2 [High] `StatusIconResolverTest` 的 `requestedKey` 断言对 fallback 行为是 false-positive

**代码事实**（`StatusIconResolverTest.kt:38-44, 64-69`）：

```kotlin
assertEquals("icon.status.missing_status_icon", missingIcon.requestedKey)
assertEquals("missing_visual", missingIcon.resolvedKey)
assertTrue(missingIcon.matchedByPrefix)
assertTrue(missingIcon.fallbackUsed)
```

两条测试都在断言 `requestedKey` 字符串等于 `"icon.status.<typeId>"` 并 fallback 到 `missing_visual`。**断言通过等于「resolver 行为符合预期」，但「预期」本身是 round-3 §1.1 指出的 family-串味错误行为**——测试把错误行为冻结进 baseline。

**修复方向**：

1. `null status icon uses visual fallback instead of dropping status` 应改名为 `actorStatusWithoutIconKeyResolvesViaActorStatusFamilyPrefix`，并加 precondition：注入的 `StatusEffectRenderSnapshot` 必须显式带 `group = NEUTRAL`（非 zone effect / 非 mutation）。
2. 新增 `zoneEffectWithoutIconKeyDoesNotResolveAsActorStatus`，断言：注入一个 `group == ZONE_EFFECT` 的 presentation 后，resolver 返回 list 不包含该 presentation（或抛 IllegalStateException 表示数据源错位）。
3. 新增 `mutationWithoutIconKeyUsesMutationFamilyPrefix`，断言 `requestedKey == "icon.mutation.<typeId>"` 而非 `icon.status.<typeId>`。

### 1.3 [Medium] `StatusPresentationBuilder.buildZoneEffect` 仍保留 `iconKey = null` 作为合法路径，runtime 与文档之间缺 cross-check lint

**代码事实**（`StatusPresentationModel.kt`）：`buildZoneEffect` 仍构造 `iconKey = null` 的 presentation。当前 working tree 没有任何 lint 校验「`group == ZONE_EFFECT` 的 presentation 不会被 `StatusIconResolver.resolveIcons` 消费」。

**修复方向**：

1. 在 `darkKeyRegistryLint` 或 `manifestLint` 增加一条 cross-check：扫描 `StatusIconResolver` 实现，断言它在 `resolveIcons` 入口存在 `group != ZONE_EFFECT` 的 filter（可用简单的 source-level grep + AST check）；缺失即 fail。
2. 或者更稳健的方式：把「HUD-eligible group」做成 `StatusPresentationGroup` 上的属性（如 `isHudEligible: Boolean`），让 resolver 用属性查询而不是硬编码 enum filter。zone effect 的 `isHudEligible = false`，未来新增 group 必须显式声明。

---

## 2. Telegraph 路由进 status HUD：类 ToME enemy intent 体系的合同错位

### 2.1 [Blocker B2] PR06 §6.2.1 #6 把 telegraph 当作 status overflow 的一部分参与 fold，是错误的系统策划决策

**证据锚点**：

PR06 §6.2.1 #5：

> status icon 行最大显示数量必须冻结为 `10`。超过上限时按 `TELEGRAPH > DEBUFF > ZONE_EFFECT(if explicitly HUD-routed) > BUFF > NEUTRAL_OTHER` 的 family priority…

PR06 §6.2.1 #6：

> Telegraph 若进入 status HUD，默认不参与普通 fold；telegraph 数量 `>= 5` 时 fold badge 必须独立显示 telegraph count，不能把 enemy intent 混入普通 buff/debuff overflow。

design notes §4 Icon Taxonomy 表第 5 行：

> Telegraph (enemy intent) | enemy action preview or danger cue | ranged arc, aim line, impact marker, directional strike shape | danger-linked ember/red/violet restraint; not focus cyan | player buff/debuff or selected row

**问题链**：

1. **系统策划维度**：类 ToME / Roguelike 的 telegraph 是 **enemy intent**——它表达「这个敌人下一步要做什么动作」（旋风斩、远程射击、AOE 起手）。它的语义 owner 是「场上某个敌人」，渲染 anchor 是「敌人头顶 / 敌人 tile / 敌人 facing 方向」，**不是 player actor 的状态条**。
2. 把 telegraph 路由进 player 的 status icon HUD 等于把 enemy intent 与 player condition 混在同一个视觉容器里。玩家视线扫 status 行的目的是「我现在有什么 buff / debuff / 资源条件」，扫 telegraph 的目的是「我下一回合要规避什么」——**这是两套不同的认知任务**：
   - status scan 是「inventory check」，节奏慢、用 hover/tooltip 扩展；
   - telegraph scan 是「threat assessment」，节奏快、需要空间定位（哪个敌人 / 哪个方向）。
3. 把 telegraph 塞进 status 行后：
   - 玩家 status scan 时必须先过滤 telegraph icon（认知负担 +30-50%）；
   - 玩家 threat assessment 时无法看到敌人空间位置（status 行不带 actor anchor）；
   - PR06 §6.2.1 #6 已经隐约意识到这是问题（专门给 telegraph 独立 fold count），但解决方案是「再加一个 telegraph fold badge」而不是「telegraph 不入 status 行」。这是「补丁的补丁」。
4. **ToME 原生实现参考**：ToME 4 用 character sheet 顶部 / 敌人 tile 上方的 chevron + buff/debuff 行严格分离；Caves of Qud、Cogmind、Tangledeep 都把 enemy intent 渲染在 map tile 而不是 status 行。把 telegraph 路由进 status 行没有先例。
5. **§6.5 accent budget 自相矛盾**：PR06 §6.5 表给 telegraph 分配了 `cold-cyan focused row / telegraph indicator | strength 2, max two combat tiles` 的预算，括号里写 "telegraph color follows danger semantics and must not reuse focus cyan when it reads as enemy intent"——也就是说 §6.5 自己已经声明 telegraph 是 enemy intent 且需要独立 anchor（"combat tiles"，不是 status 行）。这与 §6.2.1 #6 把 telegraph 算进 status fold 的写法**直接对冲**。
6. **玩法体验维度**：类 ToME 玩家最珍视的「我能精确判断要不要走 / 要不要打」依赖于 telegraph 的空间感。把 telegraph 拉出敌人位置变成 HUD icon 是把 turn-based tactical depth 直接降级为 ARPG 提示条。

**为什么 round-1/2 没看到这点**：

- round-1 §1 集中在 `mapNotNull` 与 quest follow-up，没审 telegraph 路径；
- round-2 §1.4 提了 fold priority 表里 telegraph priority 最高是合理的，但默认接受了 "telegraph 在 status HUD 里" 的前提，没回头质疑这个前提。

**修复方向（必须在 PR06 close 前完成）**：

1. **PR06 §6.2.1 #5/#6 重写**：删掉 `TELEGRAPH` 在 status family priority 表里的位置，改为：
   > status HUD 行 family 范围限定为 `DEBUFF / BUFF / ZONE_EFFECT (only if explicitly HUD-routed via icon.status.zone_effect.*) / NEUTRAL_OTHER`。telegraph 不在 status HUD 行的合同范围内。telegraph 的渲染 owner 是 enemy actor 旁的 intent indicator（独立 row 或 map tile overlay），归属于 PR-02-1 shell 或独立 telegraph PR 的责任范围，不在 PR-06 status icon 合同内。
2. **PR06 §6.5 accent budget 表清理**：把 telegraph 从 cyan budget 拆出来，单独列：
   > danger-red / danger-violet telegraph indicator | enemy intent preview | strength 2, anchored at enemy tile, max two simultaneous | must not reuse player focus cyan or player status badge frame
3. **PR-06 范围内必须 audit**：当前是否真有代码把 telegraph 路由进 `StatusEffectRenderSnapshot`？如果是，必须删；如果不是，文档段落必须删（避免误导未来 implementer）。
4. **follow-up 命名**：如果 telegraph indicator UI 在 PR-06 内确实没 surface，必须命名 follow-up `UI07-telegraph-actor-anchored-indicator` 并写进 `UI/PLAN.md`，明确禁止把 telegraph 重新塞回 status HUD。
5. **测试断言**：`StatusIconResolverTest.telegraphEffectIsNotPartOfStatusHudRow`（如果 `StatusEffectRenderSnapshot` 有 telegraph 数据源），resolver 返回 list 不含 telegraph entry。

**为什么这是 Blocker**：

PR06 是「dark UI/UX 全量收口 PR」。一旦 telegraph 路由进 status HUD 的文档段落不删，未来 implementer 会按文档实现 telegraph icon 进 HUD，**最终的 game-feel 损失发生在 PR-07 packaged app 评测**——而 PR-07 不再拥有 status HUD owner，没有 surface 接住这个回滚。这是「PR-06 一句话埋了三个 PR 后才能挖出来的体验地雷」。

### 2.2 [High] `StatusPresentationGroup.TELEGRAPH` enum 与 telegraph 实际归属边界冲突

**问题**：

PR06 §6.2.1 #5 默认 `StatusPresentationGroup.TELEGRAPH` 是合法 group。但按 §2.1 修复方向，telegraph 不应在 `StatusPresentation` 体系内。

**修复方向**：

1. 如果 `StatusPresentationGroup.TELEGRAPH` 当前没被实际 telegraph 数据消费（只是 enum 占位），直接删除该 enum case；保留 enum 会让 implementer 误以为 telegraph 是 status family 的合法成员。
2. 如果当前有代码路径 emit `group = TELEGRAPH`，必须 audit 该路径是否真表达「player-owned status with telegraph-like priority」（合理：如 charge-up 技能蓄力 status），还是误把 enemy intent 路由过来（必须删）。
3. PR06 §6.2.1 增补一行：`StatusPresentationGroup.TELEGRAPH` 仅用于 player-owned 蓄力 / charge-up status；不允许 enemy intent / aim line / impact marker 通过该 group 进入 status HUD。

---

## 3. Quest 探索循环：activate 与 complete 的对称失误

### 3.1 [Blocker B3] PR06 §6.2 objective token visual weight 表把「新任务出现」的发现节点也压平

**证据锚点**：

PR06 §6.2 objective token visual weight 表（round-2 后已经修复 `complete` 行为 `ember-gold completion accent for 0.5-1.0s, then returns to muted marker`）：

| Token | Current code meaning | Row tone |
| --- | --- | --- |
| `log.objective.activate` | objective appears or becomes active | muted objective marker; no persistent high accent |
| `log.objective.progress` | objective step/progress updated | muted objective marker; brief low pulse allowed |
| `log.objective.advance` | objective enters next sub-phase, currently emitted on outpost depth/interactable progression | medium pulse, no rare-item accent |
| `log.objective.complete` | objective completion | ember-gold completion accent for `0.5-1.0s`, then returns to muted marker |

**问题链**：

1. Roguelike / 类 ToME 的探索循环只有两个高强度反馈节点：「新任务出现」（探索发现）和「任务完成」（成就反馈）。round-2 B3 修复了 `complete` 端的压平问题。但 `activate` 端仍然是 `muted objective marker; no persistent high accent`——这等于把「我刚刚发现一个新任务」这一帧的视觉反馈也关掉。
2. ToME-likes 玩家的核心 dopamine loop 是「探索 → 触发事件 → 接受/完成任务 → 装备/经验回报 → 下一段探索」。`activate` 和 `complete` 是这个 loop 的两个端点；两端都 muted 等于让玩家无法在 HUD 上感知到 loop 推进——他们必须读 log 文本才能知道发生了什么。这是 console / mobile RPG 的反馈模式，不是 Roguelike 的。
3. round-2 B3 的诊断同样适用于 `activate`：「ToME-likes 玩家在战斗中视线常常不在 HUD 文本行，他们用 icon 周边的 accent 闪烁感知『有新东西』」。round-2 把这个论证应用到了 `complete`，没回头看 `activate`。
4. **§6.5 accent budget 与 §6.2 token 表对冲**：§6.5 `ember-gold title / confirmation` 行允许 `strength 1, confirmation < 0.5s`——这正是 `activate` transient 应该消费的预算，但 §6.2 把 `activate` 写死成 `no persistent high accent`，禁止了 transient 也禁止了 strength-1 confirmation。**两个段落自相矛盾**。

**为什么 round-2 没看到这点**：

round-2 B3 是从 `complete` 体验损失角度切入的，论证写得很完整；但 round-2 修复进 PR06 §6.2 表后，`activate` 行抄了 round-2 文字「muted objective marker; no persistent high accent」——其实 round-2 自己的论证逻辑就要求 `activate` 也有低强度 transient 反馈。这是 round-2 patch 完成度的盲点。

**修复方向（必须在 PR06 close 前完成）**：

1. **PR06 §6.2 objective token visual weight 表**改写：

   | Token | Visual weight |
   | --- | --- |
   | `log.objective.activate` | brief ember-gold confirmation `< 0.5s`，decay to muted objective marker；与 §6.5 `confirmation` accent budget strength 1 一致 |
   | `log.objective.progress` | muted objective marker；不消费 accent 预算 |
   | `log.objective.advance` | medium pulse `< 0.4s`，decay to muted；不使用 ember/cyan accent，只用 row tone 渐变（如 LIGHT_GRAY → mid-gray → LIGHT_GRAY） |
   | `log.objective.complete` | ember-gold completion accent `0.5-1.0s`，decay to muted objective marker |

   关键不同：`activate` 与 `complete` 都是 transient ember confirmation，但 duration 不同（0.4s vs 1.0s），让玩家通过 duration 区分「新任务接受」与「任务完成」。
2. **测试增补**：
   - `TileRenderModelTest.shellQuestSummaryUsesTransientAccentOnObjectiveActivate`（断言 activate 触发 transient row tone 状态变化）；
   - `TileRendererCanvasTest.shellQuestSummaryActivateAccentDecaysWithinExpectedDuration`（断言 transient 持续 < 0.5s）；
   - round-2 B3 提议的 `TileRenderModelTest.shellQuestSummaryUsesEmberAccentOnObjectiveComplete` 仍然需要补。
3. **若 PR-06 决定不在本 PR 实现 row tone transient**：必须命名 follow-up `UI07-quest-activate-accent` 并写进 `UI/PLAN.md`，与 `UI07-quest-complete-accent`（round-2 命名）平行。
4. **design notes §5 state badge 表**增补两行：
   - `Quest activate`：transient ember confirmation `< 0.5s`，与 quest active 主 marker 区分；
   - `Quest advance`：transient row pulse，no accent color；与 progress 区分。

**为什么这是 Blocker**：

round-2 B3 已经被接受为 Blocker（complete 端 muted 不可接受）。`activate` 端 muted 是同结构问题，违反「不能把 Roguelike 探索 loop 两个端点同时压平」的 design 原则。如果 PR-06 只修 `complete` 而不修 `activate`，玩家会觉得「完成有反馈，发现没反馈」——更糟的反馈不一致。

### 3.2 [High] `log.objective.advance` 与 `log.objective.progress` 的语义区分仍模糊，token 命名空间风险

**证据锚点**：

PR06 §6.2 token 表给 advance 写 `objective enters next sub-phase, currently emitted on outpost depth/interactable progression`——round-2 §3.3 提议的语义说明已经入 PR06 表，这是 round-2 之后的明显进步。

**剩余问题**：

1. `outpost depth` 与 `interactable progression` 是当前 emit 点，但没规定**未来扩展时**哪些事件可以 emit `advance` 而不是 `progress`。等于把 token 命名空间漂移的风险留给后续 PR。
2. `log.objective.progress` 与 `log.objective.advance` 在玩家视角是「同一类任务进度推进」。两个 token 共存意味着 emit 端必须严格区分「计数 +1」（progress）与「阶段切换」（advance）。如果 game data 层没有强约束，emit 端最容易出错的就是把所有进度都 emit 成 progress（更 conservative）或都 emit 成 advance（更 visible）。
3. round-2 §3.3 提出的「audit `core` / `game` 中所有 `log.objective.*` emission 点」没在 PR06 §6.2 落地——只是把 advance 的 "currently emitted" 写进了表，没要求 audit 完整。

**修复方向**：

1. PR06 §6.2 后增补 emission 合同：
   ```
   `log.objective.advance` emission contract:
   - Required: objective entering a new sub-phase that the player should perceive as a stage change
     (e.g., outpost depth +1, interactable progression to next state, multi-step quest flips to next step).
   - Forbidden: counter +1 within the same sub-phase (use progress instead),
     side-effect resolution (use ambient/log lines), or speculative pre-completion (use activate).
   - Validation: any new emission site must add a focused test naming the sub-phase boundary it represents.
   ```
2. 测试增补：`ObjectiveEventEmissionTest.advanceIsOnlyEmittedAtSubPhaseBoundary`（用 fixture quest 验证 progress vs advance 的 emit 边界）。
3. PR06 §6.2 命名 follow-up `UI07-objective-token-emission-audit`，PR-07 之前完成 emission audit。

### 3.3 [High] Quest summary row icon slot 在「无任务 → 有任务」切换时的 reserved-space 合同未冻结

**证据锚点**：

PR06 §6.2.7：

> 空状态保留 `ui.shell.quest.none` 文案；空状态不消耗 quest icon，不计入 quest icon 上屏证明。

**问题链**：

1. PR06 §6.2.1 要求 quest summary row 变成 icon-bearing `TileTextRow`——意味着行布局是 `[icon slot][text...]`。
2. PR06 §6.2.7 要求空状态「不消耗 quest icon」——但没规定 icon slot 是「保留 reserved space 但渲染空白」还是「row 折叠为纯文本行（slot 消失）」。
3. 两种行为差异巨大：
   - 保留 reserved space：玩家「无任务 → 接受新任务」时只看到 icon 从空白变成 marker，**slot 位置稳定**，符合 §6.5 accent 预算与「fold 时位置稳定」原则；
   - row 折叠为纯文本：「无任务 → 接受新任务」会触发 row 宽度 + icon slot 同时出现的 layout shift。HUD layout shift 在战斗中是高频可感知的视觉抖动。
4. round-2 §3.1 #1 提到「empty state: LIGHT_GRAY 且保留 muted placeholder icon slot（消耗 row 高度，不消耗 quest icon 上屏证明）」——这是 round-2 提议的修复方向，但 PR06 §6.2.7 只采纳了「不消耗 quest icon 上屏证明」一半，没采纳「保留 placeholder icon slot」另一半。

**修复方向**：

1. PR06 §6.2.7 增补：
   > 空状态必须保留 quest icon slot 的 reserved space，渲染为 transparent 或 `icon.quest.empty` placeholder（与 `icon.quest.objective_marker` visual 不同）；slot 高度与宽度与有任务时一致，避免 `activate` 触发 layout shift。`icon.quest.empty` 是否新增 manifest key 由 PR-06 决定：如果保留 reserved space 走 transparent，不需要新 key；如果用独立 placeholder visual，必须在 §6.1 inventory 加 `icon.quest.empty` 行并进 `r09-quest-zone-profession` sheet。
2. 测试增补：`TileRenderModelTest.shellQuestSummaryReservesIconSlotInEmptyState`（断言 row 结构包含 icon slot 但 asset 为空 placeholder）。

---

## 4. Cross-surface profession identity：3 surface 不够覆盖类 ToME 实际触点

### 4.1 [High] `icon.profession.*` 的 required consumer 列表只覆盖一半 surface

**证据锚点**：

PR06 §6.1 family 表 profession icon 行：

| Family | Required consumer/test |
| --- | --- |
| profession icon | `icon.profession.*` | profession index and canonical manifest | `r09-quest-zone-profession` | profession selection consumer test + talent header/log consumer test + `ManifestResolveTest` |

PR06 §8 manual whitebox 第 9 条：

> 切换至少 3 个职业，分别在 profession selection -> talent panel -> combat log / HUD 三处确认 profession icon 视觉一致

**问题链**：

1. 类 ToME / Roguelike 游戏的 character class identity 是一个**跨 surface 高频元素**。在 K-ToME 当前 build 与未来 build 中，profession icon 至少出现在以下 8 个 surface：

   | # | Surface | 频率 | 当前 PR06 是否覆盖 |
   | --- | --- | --- | --- |
   | 1 | Profession selection screen | 一次性 | ✅ |
   | 2 | Talent panel header | 高频 | ✅ |
   | 3 | Combat log / HUD profession line | 高频 | ✅ |
   | 4 | Character sheet portrait / header | 高频 | ❌ |
   | 5 | Ally bar (party / minion / pet 显示玩家职业) | 中频 | ❌ |
   | 6 | Death summary / run end screen | 一次性但高 emotional weight | ❌ |
   | 7 | Save/load slot list | 高频 | ❌ |
   | 8 | Minimap legend / overlay 标识玩家位置 | 中频 | ❌ |

2. PR06 当前只覆盖 1/2/3，缺 4/5/6/7/8。这些 surface 不需要 PR-06 实现，但**必须在 PR-06 inventory 里被显式 surveyed**——否则 PR-07 polish 时会发现「character sheet 用旧风格 profession icon」「save slot 用 fallback」「death screen 用占位符」，PR-07 不再拥有 sheet owner，无法修补。
3. 即使其中部分 surface 当前 K-ToME 还没实装（如 ally bar 可能在未来 PR），inventory 必须显式记录「PR-06 close 时该 surface 是否存在」+「未来实装时是 polish PR 还是新 sheet PR」。

**为什么 round-1/2 没看到**：

round-1/2 接受了「PR-04 talent panel + profession selection + combat log」三 surface 作为 profession icon 的覆盖证明，没回头审 character sheet / save / death 等系统级 surface。

**修复方向（建议在 PR06 close 前 audit）**：

1. PR06 §6.1 profession icon 行 required consumer 扩写为：
   ```
   profession selection consumer test + talent header/log consumer test +
   ManifestResolveTest +
   character sheet header consumer test (or explicit "no character sheet surface yet" entry in inventory) +
   save/load slot list consumer test (or explicit "no save slot ui yet" entry) +
   death summary consumer test (or explicit "no death summary ui yet" entry)
   ```
2. PR06 §6.1 inventory 表的 `consumer` 字段对于「PR-06 close 时不存在的 surface」必须填 `manifest-only-with-reason=surface-not-yet-implemented-tracked-as-UI07-<surface>`，并在 `UI/PLAN.md` 命名对应 follow-up（如 `UI07-profession-icon-character-sheet`、`UI07-profession-icon-save-slot`、`UI07-profession-icon-death-summary`）。
3. PR06 §8 manual whitebox 第 9 条扩写：
   > 切换至少 3 个职业，**列出当前 build 存在的所有 profession-identity surface**（不限于 selection / talent / combat log），逐一检查 dark-v1 icon consistency 与 fallback 行为；记录「当前 build 不存在但未来需实装」的 surface 列表，每个 surface 必须有对应 follow-up ID。

### 4.2 [High] PR-06 close 后 PR-07 不再拥有 sheet owner，跨 surface profession 漂移会成为孤儿问题

**问题**：

PR06 §6.1 字段 `removalOwner = PR-07` + §3 §4 反复声明 "PR-07 只 audit"。但 character sheet / save slot / death summary 一旦在未来 PR 实装并使用旧风格 profession icon，PR-07 audit 没有权限 fix sheet——必须开新 sheet PR。

**修复方向**：

PR06 §3 增补 cross-surface sheet ownership 段落：

```
Cross-surface profession identity contract:
- PR-06 owns the dark-v1 profession sheet (r09-quest-zone-profession) for all profession icon keys.
- Any future surface that consumes profession icon must reuse the dark-v1 key; not allowed
  to introduce a parallel profession icon family for cosmetic difference between surfaces.
- If a future surface needs a different visual size or framing (e.g., character sheet
  needs 96px portrait), it must request a sized variant via the multi-size variant
  contract (see §11.2 round-2) instead of a new sheet.
- PR-07 audit responsibility: confirm all live surfaces consume the dark-v1 key.
  PR-07 cannot create a new sheet PR; if a live surface uses an old-era profession icon,
  PR-06 must reopen.
```

---

## 5. Sub-32px readability：contact-sheet QA 锚点与 runtime 实际尺寸的错位

### 5.1 [High] PR06 contact-sheet QA 全部基于 32px readability，runtime 实际尺寸覆盖 16/24/32/48/128

**证据锚点**：

PR06 §3 资源 QA 补充合同 #1：

> `r08-skills-*` 与 `r09-status-damage` 的 contact-sheet QA 必须做 cross-sheet side-by-side：随机抽 4 个 skill cell、4 个 status/mutation cell、4 个 damage type cell，并排验证 `32px` 下 silhouette weight 不收敛。

PR06 §3 #4（round-2 后补的多 size 合同）：

> `icon.profession.*` 必须验证 `128 / 48 / 24` 三种尺寸的可读性。

design notes §4.1 #5：

> clear silhouette at `32x32`

**问题链**：

1. PR06 明确规定 profession icon 验证 3 size，但**没有对 skill / status / damage_type / quest icon 提同等多 size 合同**。
2. 类 ToME / Roguelike 实际 runtime 渲染尺寸：
   - HUD status icon row：通常 24-32px；
   - inventory tooltip 内嵌的 damage_type icon：16-20px（受 tooltip 行高约束）；
   - skill hotkey bar：32-48px；
   - log row 内嵌的 quest marker icon：12-16px（受 log 行高约束）；
   - 战斗浮字旁的 damage_type icon：16-24px；
   - 装备 tooltip header 内嵌的 weapon profession icon：20-28px。
3. 32px contact-sheet QA 通过，**不能证明** 16-24px sub-32px 路径下 silhouette / outline 仍可分。dark-v1 era 偏 charcoal + low saturation，sub-32px 下 outline 厚度若不足 1px，silhouette 会塌成模糊块。
4. **未来灾难场景**：PR-06 close, PR-07 packaged app smoke 时 reviewer 截图发现「装备 tooltip 内的火 damage_type icon 与燃烧 status icon 在 16px 下看不出区别」——PR-07 不再拥有 sheet owner，必须开新 sheet rework PR；按 PR06 §3.10 rework 合同必须在 PR-07 前 merge——**触发回滚** PR-06 close 状态。

**为什么 round-1/2 没看到**：

round-2 §11.2 提到 profession icon 3 size context，但没把同样的逻辑推广到 status / skill / damage_type / quest icon。

**修复方向**：

1. PR06 §3 资源 QA 补充合同新增 #6：
   ```
   Multi-size contact-sheet QA contract:
   - r08-skills-* sheets: contact-sheet QA must include side-by-side 16/24/32/48 px sample
     for each cell category (skill icon, talent visual portrait).
   - r09-status-damage sheet: contact-sheet QA must include side-by-side 16/24/32 px sample
     for status icon, mutation icon, damage_type icon.
   - r09-quest-zone-profession sheet: contact-sheet QA must include side-by-side 12/16/24/32 px sample
     for quest marker icon and zone icon.
   - If any cell fails sub-32px readability, the sheet must define multi-size variant keys
     (`<family>.<id>.sm/md/lg`) or request a single source resampled with hard outline reinforcement;
     it cannot rely on GPU downscaling as the readability guarantee.
   ```
2. PR06 §6.1 family 表的 `consumer/test` 列对 status / damage_type / quest icon 必须 specify 至少一个 sub-32px consumer test（如 inventory tooltip / log row / damage float）；当前 §6.1 status icon 行已经有 "HUD overflow/fold focused test" 但没指明 sub-32px 路径。
3. PR06 §8 manual whitebox 增补一条：
   > 在 inventory tooltip / log row / damage float 三个 sub-32px 路径下，逐一检查 status / damage_type / quest icon 的 silhouette 是否仍可分；与 32px reference 对比若有 collapse，必须回 sheet QA。

### 5.2 [Medium] dark-v1 outline / silhouette 厚度在 sub-32px 的最小可见值未冻结

**问题**：

design notes §4.1 给「strong readable silhouette at 32px」与「no clean vector sticker look」，但没规定 outline 厚度的最小可见 px 值。在 sub-32px 路径下，outline < 1px 会被 GPU 抗锯齿吃掉。

**修复方向**：

design notes §4.1 增补：

```
Outline/silhouette pixel budget:
- 32px source resolution: outline ≥ 2 px, primary silhouette occupies ≥ 60% bounding box;
- sub-32px downscale path: source must include hard outline ≥ 2 px so that GPU bilinear
  downscale to 16-24px preserves ≥ 1 px outline integrity;
- if a source cell relies on antialiased edges instead of hard outline, it must be regenerated
  with explicit outline layer before passing contact-sheet QA.
```

---

## 6. ToME 玩家高频 corner case 缺合同

### 6.1 [High] Status `rawBadge` stack count > 99 与多实例 status 的视觉处理未规定

**证据锚点**：

PR06 §6.2.1 没规定 `rawBadge` 的 overflow 行为。`StatusPresentationModel.rawBadge` 当前是 `String` 类型（看 round-2 review 引用的 `turnBadge(terrainOverride.remainingTurns)`）。

**问题链**：

1. ToME 4 / Caves of Qud / Tangledeep 等 ToME-likes 的核心 build 之一是「dot stacker」——玩家堆叠 bleed / poison / disease / curse stack 到 99+。boss 战触发瞬间可能出现 `bleed × 256` / `poison × 184` 这样的 stack count。
2. PR06 当前 `rawBadge: String` 没限制长度——意味着 stack count `256` 会以 3 字符渲染。class HUD status icon size 24-32px 与 3 字符 badge 在视觉上会冲突：
   - 3 字符 badge 必须缩字号 → 可读性下降；
   - 或 badge overflow 出 icon 边界 → 与相邻 icon 重叠；
   - 或 badge 截断到 2 字符 `25…` → 玩家不知道还剩多少 stack。
3. 类 ToME 的标准处理是「stack ≥ 100 时显示 `99+`」+「同 typeId 多实例时 stack count 累加，不出现两个 icon」。PR06 没声明这两个合同。
4. 同样问题在 `remainingTurns` 上：「-1 turn」（持续到死亡 / 永久效果）的 badge 渲染是什么？空？「∞」？数字？PR06 没规定。
5. **multi-instance status**：ToME-likes 中同 status type 但不同 source（不同敌人对玩家施加 poison）可能创建多个 `StatusEffectRenderSnapshot` 实例。HUD 应该 group 还是 separate？PR06 没规定，这会导致 status icon row 在被多敌人围攻时迅速塞满。

**修复方向**：

1. PR06 §6.2.1 新增 stack/badge contract 段落：
   ```
   Status badge rendering contract:
   - stack count badge: "<n>" for 1 ≤ n ≤ 99; "99+" for n ≥ 100; empty for n ≤ 0.
   - remaining turn badge: "<n>" for 1 ≤ n ≤ 99; "99+" for n ≥ 100; "∞" for permanent (n < 0);
     empty for n == 0 (status expiring this turn) - status should drop before next render.
   - multi-instance status: same typeId across multiple StatusEffectRenderSnapshot must be
     grouped into a single HUD entry, badge counter shows total stack across instances;
     not allowed to render two icons for same typeId.
   - badge max visual width: 3 character cells; if locale renders longer (e.g., CJK fullwidth),
     truncate with ellipsis and provide hover/tap detail.
   ```
2. 测试增补：
   - `StatusPresentationBuilderTest.stackBadgeRendersNinetyNinePlusWhenOverflow`；
   - `StatusPresentationBuilderTest.multiInstanceStatusGroupsByTypeIdWithCumulativeStack`；
   - `StatusPresentationBuilderTest.permanentStatusRendersInfinityBadge`.
3. PR06 §6.1 family 表 status icon 行的 `consumer/test` 列增补「stack/badge overflow focused test」。

### 6.2 [Medium] Status `fold` badge `+N more` 缺 hover/tap expand 行为，类 ToME build 阶段的 HUD UX 投诉点

**证据锚点**：

PR06 §6.2.1 #5：

> 以 locale key `ui.status.fold.summary` 渲染固定末尾 slot 的 `+N more` badge

round-2 §1.3 提议 hover/tap expand 行为，PR06 文档暂未采纳。

**问题**：

类 ToME 玩家 build 阶段需要 inspect 全部 status——「我现在到底叠了多少 buff，每个还剩几回合？」如果 fold badge 不提供 expand 行为，玩家必须切换到 character sheet 或 status detail panel。PR-06 没规定这个 expand 是「PR-06 实现」「follow-up」还是「不实现」，三种选择都合法但不能模糊。

**修复方向**：

1. PR06 §6.2.1 #5 增补：
   > `+N more` fold badge 必须支持 hover (PC) / tap (touch) expand 行为，弹出全部隐藏 status 的 detail tooltip；tooltip 排序与 status row 同（priority desc + typeId asc）。如 PR-06 内不实现 expand，必须命名 follow-up `UI07-status-fold-expand` 并写进 `UI/PLAN.md`；fold badge 不实现 expand 时必须显式 cursor: not-allowed / non-interactive，避免玩家误以为可点击。
2. 测试增补：`StatusPresentationModelTest.foldBadgeProvidesExpandTooltipOrIsExplicitlyNonInteractive`.

### 6.3 [Medium] Skill cooldown 视觉状态在 PR06 §6.1 skill icon 行没体现

**证据锚点**：

PR06 §6.1 skill icon 行 required consumer：

> `TalentAssetReferences` / `ManifestResolveTest` / talent golden

**问题**：

skill icon 有多个 runtime state：available / cooldown / locked / not-yet-learned / passive。32px contact-sheet QA 默认 verify 「subject identity」，但 cooldown 状态（典型表现是灰阶 + 倒计时 badge + 圆环 overlay）的视觉合同没在 PR06 落地。

类 ToME 战斗中玩家最关心的不是「这个技能 icon 是什么」，而是「这个技能现在能不能用 / 还剩几回合 cooldown」——这是 cooldown overlay 而不是 icon 本身的合同。

**修复方向**：

1. PR06 §6.1 skill icon 行 required consumer 增补：
   > skill cooldown overlay focused test (proves cooldown ring/badge renders over skill icon without breaking dark-v1 era)
2. design notes §5 state badge 表增补行：
   | Cooldown | desaturated icon + ring overlay showing remaining turns | becomes fully invisible / icon disappears during cooldown |

### 6.4 [Medium] Damage float (战斗浮字) 与 damage_type icon 的复合视觉合同未冻结

**问题**：

每次伤害结算，K-ToME 显示 `<damage_type icon> <number>` 浮字。这是高频元素（boss 战每秒 5-10 次）。

PR06 §6.1 damage_type icon 行的 required consumer 写「at least one damage float / skill preview / resistance panel focused test」——round-2 §2.2 已经提出「specify 哪一类是 minimum」但 PR06 仍是 OR。

更严重的是：damage float 是**动画 + 衰减 + 数字 + icon** 的复合元素，contact-sheet QA 只看 static icon，无法预测 16-24px 浮字状态下的可读性。

**修复方向**：

1. PR06 §6.1 damage_type icon 行 required consumer 改为 AND 列表：
   > `ManifestResolveTest` AND weapon/equipment tooltip damage_type icon focused test AND damage float rendering focused test AND resistance panel focused test.
2. PR06 §8 manual whitebox 增补一条：
   > 在 boss 战触发至少 30 次 damage float，截屏 / 录屏 1 段 boss 战，确认 damage_type icon 在浮字尺寸 (16-24px) 下与 damage number 不重叠、不模糊、与 status icon 视觉不混淆；将证据写进 `UI/manual-records/dark-uiux-pr06-damage-float-visual.md`。

---

## 7. Content pack / mod 与 dark-v1 era 的合同空缺

### 7.1 [High] PR06 §3 资源合同假设无第三方 content pack 引入 player-visible visual key

**证据锚点**：

PR06 §5 非目标：

> 不改 validation scenario 选择、content pack 加载、scenario bootstrap 或 whitebox materialization 规则；只改 overlay / summary 的 presentation 与资源覆盖。

PR06 §6 切换表只覆盖 K-ToME built-in player-visible key。content pack 注入的新 key 没有 disposition。

**问题链**：

1. K-ToME 已经支持 content pack（PR06 §6.3 / §7 都引用 content pack / validation overlay 的 namespace、touched content 列表）——意味着 third-party pack 已经是 active 路径。
2. content pack 可以注入新的 skill / status / damage_type / quest / profession 数据。data 注入伴随的是 visual asset 注入需求——pack 提交 PNG，game 渲染。
3. PR06 §6 切换表全部基于「我们自己生成的 dark-v1 sheet」，没规定第三方 pack 提交 visual asset 时的 disposition：
   - 通过 `ktome-dark-fantasy-sprite-ui-v1` style certification → 进入 dark-v1 era？
   - 拒绝并强制走 fallback / stub？
   - 接受但渲染时用「third-party era marker」标识？
4. 当前 dark-v1 era 的视觉一致性是 PR-06 close 的核心承诺。一旦 third-party pack 注入 cartoon style skill icon，玩家会在同屏看到 「dark-v1 charcoal forged iron skill icon 旁边一个 mobile RPG cartoon icon」——era 直接破。
5. 这不是「未来才发生」的问题——K-ToME 现在的 fallback / debug resource 路径已经在做「外来 visual key + manifest fallback」的事情，只是 fallback 视觉本身是 `missing_visual`。一旦 fallback 视觉变成第三方 PNG，问题就立刻发生。

**修复方向（建议在 PR06 close 前 audit）**：

1. PR06 §5 非目标段落增补：
   > content pack visual asset 的 dark-v1 era certification 不在 PR-06 实现范围，但 PR-06 必须冻结 fallback disposition：第三方 pack 提交 player-visible visual asset 时，runtime 必须按以下顺序处理：(a) 检查 asset 是否声明 `styleTag = ktome-dark-fantasy-sprite-ui-v1`；(b) 若声明 dark-v1，走正常 manifest 路径；(c) 若未声明或声明其他 styleTag，**runtime 必须用 dark-v1 sentinel 或 dark-v1 era 通用 fallback 替换**，不允许直接渲染 third-party asset，避免 era 漂移；(d) PR-06 必须新增 `icon.<family>.third_party_stub` keys 或复用 `missing_visual` 作为 stub，并记录在 §6.1 inventory。
2. 命名 follow-up `UI07-content-pack-style-certification` 写进 `UI/PLAN.md`，PR-07 之后完成 third-party visual asset 的 style 认证流程。
3. 测试增补：`VisualManifestResolverTest.thirdPartyAssetWithoutDarkV1StyleTagUsesFallback`.

### 7.2 [Medium] `allowedFallbackKeys` 的 third-party visual 入口未冻结

**问题**：

PR06 §6 coverage artifact 字段表里有 `allowedFallbackKeys`。但没规定 third-party content pack 的 visual key 是否可以进入这个列表。若可以，等于给 third-party 开了 era 漂移的合法口；若不可以，必须显式拒绝。

**修复方向**：

PR06 §6 coverage artifact 段落增补：

> `allowedFallbackKeys` 仅包含 K-ToME 内置的 debug / hidden / history resource fallback；第三方 content pack 注入的 visual key 不允许进入 `allowedFallbackKeys`，必须走独立的 `thirdPartyFallbackKeys` 字段（PR-06 不实装该字段，但必须在 schema 中预留命名空间并写进 follow-up `UI07-third-party-fallback-key-namespace`）。

---

## 8. Save/load/death/character-sheet 的 frozen profession 边界

### 8.1 [High] PR06 §6.6 frozen profession 合同仅覆盖 profession selection，save/load 路径无 disposition

**证据锚点**：

PR06 §6.6：

> `shadowblade / warden` are frozen excluded, not unlockable player goals. PR-06 must choose one disposition before close:
> 1. Preferred: hide frozen professions from player-visible profession selection…
> 2. Allowed: show locked card only with explicit unavailable/development-preview locale token…
> 3. Forbidden: show a polished locked card with dark-v1 fallback only…

**问题链**：

1. PR06 §6.6 三档 disposition 全部围绕 "profession selection" surface。但 frozen profession 可能在以下 surface 间接 player-visible：
   - **Save slot list**：玩家可能有 old save 文件创建于 frozen profession 还是 dev playable 的时期；save slot UI 显示「Shadowblade Lv 12, depth 8」时该 slot 的 profession icon / name 必须有 disposition；
   - **Death summary / run end screen**：玩家用 dev playable 的 berserker 死了，death summary 显示 "Berserker fell at depth 7" + profession icon——这是 dev playable 路径，不是 frozen，但 dev playable 与 frozen 的 disposition 在 death summary 是否一致？
   - **Achievement / statistics panel**：跨 run 的 statistics 可能 reference frozen profession（"You played Shadowblade 3 times before freeze"）；
   - **Replay viewer / spectator mode**（如未来实装）：replay 中的玩家可能用 frozen profession，replay 渲染时该 profession identity 如何展示？
2. 当前 PR06 完全没覆盖这些 surface。**最坏情况**：玩家加载 old save 看到「Shadowblade Lv 12」slot，但 selection screen 没有 Shadowblade 选项——玩家无法理解「为什么这个角色存在但不能新建」。
3. PR06 §6.6 `playerVisibility` 字段只有 `hidden / locked-with-coming-soon-label / locked-with-fallback-only` 三档，没有 covering save-slot / death-summary 的语义。

**修复方向（建议在 PR06 close 前 audit）**：

1. PR06 §6.6 末尾增补 cross-surface disposition matrix：

   | Surface | Frozen profession disposition |
   | --- | --- |
   | Profession selection (new run) | preferred: hidden; allowed: locked-with-coming-soon-label; forbidden: locked-with-fallback-only |
   | Save slot list (old save references frozen profession) | required: render slot with `visibleFallbackKey` + locked-banner + "this character is from a previous build, current build does not support starting this class" tooltip; loading the save may be allowed or disabled per save schema policy |
   | Death summary (current run is dev playable berserker/spellblade or frozen) | required: render profession identity normally for dev playable; for frozen, render `visibleFallbackKey` + dev-only banner |
   | Achievement / statistics panel | required: keep historical achievement entries readable; mark frozen profession entries with muted dev-only marker |
   | Replay viewer | not applicable until replay implemented; record as follow-up `UI07-replay-frozen-profession` |

2. PR06 §3 frozen profession exclusion schema 表新增字段：
   - `saveSlotDisposition`: enum `[hidden, fallback-with-banner, disabled-with-explanation]`；
   - `deathSummaryDisposition`: enum `[normal-render, fallback-with-banner, hide]`.

3. 命名 follow-up `UI07-frozen-profession-cross-surface-audit` 写进 `UI/PLAN.md`，PR-07 必须验证所有 surface 上 frozen profession 显示一致。

### 8.2 [Medium] dev playable (berserker / spellblade) 与 release playable 在 death/save 上的边界

**问题**：

PR06 §3 把 dev playable 列为「全量切换」+「report-only」。但 dev playable 在 death summary 中是否需要 dev-only marker？如果不需要，玩家会以为 berserker 是正式职业；如果需要，PR06 必须规定 marker 的视觉与文案。

**修复方向**：

PR06 §3 dev playable 段落增补：

> dev playable (berserker, spellblade) 在 character/profession selection 与 talent panel 中使用 dark-v1 正式视觉，但 death summary / save slot / achievement panel 中必须附带 "Development Preview - balance not final" 标识；标识使用 `ui.dev-only.banner` 或等价 locale key。如不实装该标识，必须命名 follow-up `UI07-dev-playable-banner`.

---

## 9. Color-blind 与 accessibility runtime support

### 9.1 [Medium] PR06 §6.7 / §8.7 的「color-blind simulation」只是评测工具，不是 runtime feature

**证据锚点**：

PR06 §6.7：

> Talent state and status icon evidence must include color-blind simulation checks for protanopia, deuteranopia, and tritanopia.

PR06 §8 manual whitebox #7：

> 在 protanopia / deuteranopia / tritanopia 三种色弱模拟下，确认 talent locked / learnable / reserve / active 四态、status vs mutation、status vs skill 仍可分。

**问题**：

1. simulation check 是开发者/QA 工具，**不是玩家可感知的 feature**。色弱玩家在自己电脑上玩 K-ToME 时没有 simulation toggle，他们看到的是真实色弱视野。
2. 通过 simulation 验证「sheet 在色弱下可分」是必要前提，但不是充分条件——sheet 通过 simulation 不代表 runtime accent budget / row tone / focus highlight 也通过 simulation。
3. PR06 没规定 runtime 是否提供「color-blind mode toggle」（典型实现：提供 protanopia/deuteranopia/tritanopia 模式，runtime 自动用 pattern overlay / alternate color 增强可分性）。

**修复方向**：

1. PR06 §6.7 末尾增补：
   > Color-blind simulation 仅是 sheet/visual asset 的 QA 工具，不是 runtime feature。runtime color-blind support（如 mode toggle、pattern overlay、alternate row tone）不在 PR-06 实装范围，但 PR-06 必须命名 follow-up `UI07-color-blind-runtime-toggle` 并写进 `UI/PLAN.md`；如果 PR-07 或后续 PR 不实装 toggle，dark-v1 sheet 必须独立满足色弱可分（即不依赖 runtime overlay）。
2. design notes §5 state badge 表必须保证「state 仅靠 silhouette + frame shape + pattern 就可分」，accent color 只是 secondary cue——这一点目前 design notes 已经写到（"Use shape first, color second"），但需要在 PR06 §6.5 accent budget 表显式重申。

### 9.2 [Medium] §6.5 accent budget 的 ember/cyan 在色弱视野下的等效感知未量化

**问题**：

`ember-gold` 与 `red` 在 protanopia 下感知接近；`cyan` 与 `green` 在 deuteranopia 下感知接近。PR06 §6.5 把 ember 与 cyan 当作两个独立 accent channel，但色弱玩家可能只感知到一个 channel——意味着 §6.5 "aggregate cap ≤ 4" 在色弱视野下实际上是 `<= 2`（因为 cyan/ember 被合并感知）。

**修复方向**：

PR06 §6.5 末尾增补：

> 色弱视野下 ember 与 cyan 可能被合并感知。aggregate cap ≤ 4 的合同必须通过 sheet 端的 shape/silhouette 差异保证：ember accent 的 visual carrier 必须是 sigil/marker/objective-shape 类，cyan accent 的 visual carrier 必须是 edge/glow/badge-shape 类，两者 shape 不重叠。即使 color 合并，shape 差异仍能保留 accent identity。`UI/manual-records/dark-uiux-pr06-overview-screenshot.md` 必须附 protanopia / deuteranopia 模拟下的同屏 reference 截图，证明 shape-based identity 仍成立。

---

## 10. Onboarding / tutorial 视觉脚手架

### 10.1 [Medium] 类 ToME 新手玩家的 profession / skill / status onboarding tooltip 合同空缺

**问题**：

K-ToME 是 Roguelike，新手玩家面对 8 profession × 多 talent × 多 status 的认知负担。

PR06 当前合同：

- frozen profession 有 `hoverTooltipContentKey` 显示 "Development Preview"；
- release playable profession 在 selection 时按正常 profession description 渲染——但**没规定 profession description 是否包含 onboarding hint**（"Vanguard: hold the line, protect allies"）；
- skill icon hover / status icon hover 是否提供 tooltip？PR06 没规定。

**问题链**：

1. ToME 4 的 profession selection 给每个职业一段 1-2 段 onboarding 描述 + role tag（"Tank / Damage / Support"）。这是新手发现「我适合玩什么」的关键路径。
2. K-ToME 当前 profession description 是否覆盖这些维度？PR06 没 audit。
3. status icon tooltip 在战斗中是新手玩家学习 status 系统的唯一路径——他们没有时间读手册，只能 hover icon 学。

**修复方向**：

1. PR06 §6.1 profession icon 行增补 required consumer：
   > profession description tooltip onboarding focused test (proves profession description includes role tag, primary playstyle hint, beginner difficulty)
2. PR06 §6.1 status icon 行增补 required consumer：
   > status icon hover tooltip focused test (proves tooltip describes effect, source, remaining duration, stack count, and cleanse condition if applicable)
3. 如果 PR-06 内不实装 onboarding tooltip，必须命名 follow-up `UI07-profession-onboarding-hint` 与 `UI07-status-tooltip-detail`，写进 `UI/PLAN.md`。

---

## 11. PR-06 与 PR-07 实际交接边界争议

### 11.1 [Medium] PR06 §6.4 disposition matrix 没规定「PR-07 发现新 player-visible surface」的回滚路径

**证据锚点**：

PR06 §6.4 disposition matrix 列出 surface × visibility × denominator，但没规定一种情况：**PR-07 polish 阶段发现 PR-06 inventory 漏了一个 player-visible surface**（比如新发现的 quest reward preview panel 也消费 quest icon）。

**问题**：

- 如果 PR-07 audit 时发现新 surface，按 §3 §4 反复声明「PR-07 不再拥有 sheet owner」与「PR-07 不能静默修补 player-visible 问题」，PR-07 既不能修也不能放过——必须回滚 PR-06。
- 但 PR06 没规定「回滚」的具体含义：是 reopen PR-06、开 rework PR、还是 abort PR-07？
- 这种 surface miss 在 round-2 §11 已经隐约提到（profession icon 3 size），但没冻结回滚合同。

**修复方向**：

PR06 §6.4 末尾增补：

```
Newly discovered player-visible surface during PR-07 audit:
- If PR-07 audit identifies a player-visible surface not covered by PR-06 inventory,
  PR-07 must immediately escalate to PR-06 reopen rather than silently use fallback.
- Reopen decision is binary: either
  (a) PR-06 reopens with new inventory entries and new sheet QA, blocking PR-07 close; or
  (b) the surface is reclassified as non-player-visible with explicit reason recorded in inventory,
      reviewed by design-director.
- Forbidden: PR-07 adding sheet/inventory entries on its own;
  PR-07 audit must remain pure audit, not implementation.
```

### 11.2 [Medium] PR06 §7 与 PR-07 evidence 的交接路径未规定

**问题**：

PR06 §6 第 5 条要求 coverage artifact `default 不提交`，PR 描述只引用 path。但 PR-07 的 audit evidence（packaged app screenshot、long-session re-audit 等）是否需要回引 PR-06 的 coverage artifact path？如果不需要，PR-07 audit 与 PR-06 commit 在审查时是断裂的——reviewer 必须在两份 PR description 间来回切换。

**修复方向**：

PR06 §6 末尾增补：

> PR-07 audit PR description 必须显式引用 PR-06 final-full coverage artifact 的 repo-relative path、status、key counts，与 PR-07 packaged app evidence 形成完整 audit chain；不允许 PR-07 audit 仅靠口头 "已确认无 missing_visual" 通过。PR-07 audit checklist 必须包含至少一条 "I read PR-06 coverage report at <path> on <date>, found <N> player-visible keys, packaged app smoke screenshot at <path> shows no missing_visual sentinel" 的明确条目。

### 11.3 [Medium] PR06 §3 raw sheet 生成交接 `<codex-generated-images-dir>` 占位符的可验证性

**证据锚点**：

PR06 §3 raw sheet 生成交接 #6：

> 不得把 Codex CLI transient source folder 的真实路径写入 PR 描述、coverage artifact、manual record 或 manifest。确需说明来源时只能写 `<codex-generated-images-dir>` 占位符，并以复制后的 `rawSheetPath` 与 hash 作为合同。

**问题**：

- 占位符路径无法 reviewer-verify。
- 如果 Codex CLI 行为变化（如下次生成图片不再写入同一目录），合同失效。
- raw sheet hash 是合同主要 anchor，但 PR06 没规定 hash 算法（SHA-256 / xxhash / git blob hash？）。

**修复方向**：

PR06 §3 raw sheet 生成交接 #6 增补：

> raw sheet hash 算法固定为 git blob hash（`git hash-object <path>`），与仓库 git ledger 一致；PR 描述与 coverage artifact 引用该 hash，reviewer 通过 `git hash-object` 复算验证。如果未来 `scripts/codex-generate-image.py` 实装变化（如改换 generator），必须同步更新本段落并跑 maintainabilityLint 验证脚本契约。

---

## 12. 总体合同地图与 follow-up 命名

Round-3 提议的 follow-up 命名汇总（建议全部写进 `UI/PLAN.md`）：

| Follow-up ID | 来源章节 | 描述 |
| --- | --- | --- |
| `UI07-status-resolver-group-aware-filter` | §1.1 | runtime resolver 必须 group-aware filter，zone effect 不入 HUD；mutation null fallback 走 `icon.mutation.*` |
| `UI07-telegraph-actor-anchored-indicator` | §2.1 | telegraph 不入 status HUD，必须 enemy actor anchored indicator |
| `UI07-quest-activate-accent` | §3.1 | `log.objective.activate` transient ember confirmation accent |
| `UI07-quest-complete-accent` | round-2 §3.1 (确认仍未实装) | `log.objective.complete` ember-gold accent 0.5-1.0s |
| `UI07-quest-typed-icon-mapping` | round-2 §2 (已命名) | typed quest/objective metadata 与 icon 映射 |
| `UI07-objective-token-emission-audit` | §3.2 | audit `core/game` 中所有 `log.objective.*` emission 点 |
| `UI07-profession-icon-character-sheet` | §4.1 | profession icon 在 character sheet 的 surface 接入 |
| `UI07-profession-icon-save-slot` | §4.1 | profession icon 在 save/load slot 的 surface 接入 |
| `UI07-profession-icon-death-summary` | §4.1 | profession icon 在 death summary 的 surface 接入 |
| `UI07-content-pack-style-certification` | §7.1 | third-party content pack visual asset 的 dark-v1 style 认证流程 |
| `UI07-third-party-fallback-key-namespace` | §7.2 | `thirdPartyFallbackKeys` schema 字段预留 |
| `UI07-frozen-profession-cross-surface-audit` | §8.1 | frozen profession 在 save/death/achievement 的 cross-surface audit |
| `UI07-dev-playable-banner` | §8.2 | dev playable 在 death/save/achievement 的 "Development Preview" banner |
| `UI07-color-blind-runtime-toggle` | §9.1 | runtime color-blind mode toggle |
| `UI07-profession-onboarding-hint` | §10.1 | profession description tooltip onboarding |
| `UI07-status-tooltip-detail` | §10.1 | status icon hover tooltip detail |
| `UI07-status-fold-expand` | §6.2 (round-2 §1.3) | `+N more` fold badge hover/tap expand |
| `UI07-replay-frozen-profession` | §8.1 | replay viewer 中的 frozen profession 视觉处理 |

---

## 13. Round-3 修复清单

### 13.1 Blocker（PR-06 close 前必须修）

| # | 章节 | 修复内容 | 修复成本估计 |
| --- | --- | --- | --- |
| B1 | §1.1 | `StatusIconResolver` 增加 group-aware filter + family-aware fallback；`StatusIconResolverTest` 补 zone effect / mutation case；PR06 §6.2.1 #1 增补 runtime contract 行 | code: ~30-50 行；doc: 1 段；test: 2-3 条 |
| B2 | §2.1 | PR06 §6.2.1 #5/#6 删除 telegraph 进 status HUD 的段落，改写为独立 actor-anchored 合同；§6.5 telegraph 从 cyan budget 拆出；命名 follow-up `UI07-telegraph-actor-anchored-indicator` | code: 视当前实现，可能 0；doc: 重写 2 段 |
| B3 | §3.1 | PR06 §6.2 objective token visual weight 表 `activate` 行改为 transient ember confirmation `< 0.5s`；test 补 `shellQuestSummaryUsesTransientAccentOnObjectiveActivate`；如不实装命名 follow-up `UI07-quest-activate-accent` | code: ~10-20 行 row tone state machine；doc: 1 段；test: 1-2 条 |

### 13.2 High（强烈建议 close 前修）

| # | 章节 | 修复内容 |
| --- | --- | --- |
| H1 | §1.2 | `StatusIconResolverTest` 重命名 + 增补 zone/mutation 测试 |
| H2 | §2.2 | `StatusPresentationGroup.TELEGRAPH` enum 用途 audit + PR06 §6.2.1 增补一行限定 |
| H3 | §3.2 | PR06 §6.2 token emission contract 段落 + `ObjectiveEventEmissionTest` |
| H4 | §3.3 | PR06 §6.2.7 增补 quest icon slot reserved space 合同 |
| H5 | §4.1 | PR06 §6.1 profession icon required consumer 扩 + 命名 character sheet / save / death follow-up |
| H6 | §4.2 | PR06 §3 增补 cross-surface profession identity contract |
| H7 | §5.1 | PR06 §3 增补 multi-size contact-sheet QA contract |
| H8 | §6.1 | PR06 §6.2.1 增补 stack/badge rendering contract + 3 条测试 |
| H9 | §7.1 | PR06 §5 增补 content pack style fallback disposition |
| H10 | §8.1 | PR06 §6.6 末尾增补 cross-surface frozen profession disposition matrix |

### 13.3 Medium（PR-06 close 可接受 follow-up，但需命名）

| # | 章节 | 修复内容 |
| --- | --- | --- |
| M1 | §1.3 | runtime lint cross-check：`StatusPresentationGroup.isHudEligible` 属性化 |
| M2 | §5.2 | design notes §4.1 outline / silhouette pixel budget |
| M3 | §6.2 | PR06 §6.2.1 fold badge expand 行为 follow-up |
| M4 | §6.3 | PR06 §6.1 skill cooldown overlay test |
| M5 | §6.4 | damage_type icon AND 列表 + boss 战 30 次 damage float manual evidence |
| M6 | §7.2 | `thirdPartyFallbackKeys` schema 字段预留 follow-up |
| M7 | §8.2 | dev playable banner follow-up |
| M8 | §9.1 / §9.2 | color-blind runtime toggle follow-up + accent budget shape-first 重申 |
| M9 | §10.1 | profession / status onboarding tooltip follow-up |
| M10 | §11.1 / §11.2 / §11.3 | PR-06 / PR-07 交接 reopen path + audit chain + raw sheet hash 算法 |

---

## 14. 关于 PR-06 当前完成度的总评

站在「资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人」的立场上，PR-06 当前版本（含 round-1/2 补丁 + 当前 working tree）**距离 close 还差三类工作**：

1. **Blocker 级合同 vs 代码错位修复（B1/B2/B3）**：必须在 close 前修，否则 PR-07 评测时会出现回滚级别的体验事故；
2. **High 级 cross-surface coverage 扩展（H5/H6/H10）**：profession identity / frozen profession 必须在 inventory 阶段把所有 surface 列出，即使 PR-06 不实装也必须命名 follow-up；
3. **Roguelike / 类 ToME 玩法循环维度合同（B3/H8/M3/M4）**：当前 PR-06 文档讨论几乎全部围绕「manifest 收口」+「同屏一致性」，**缺少「玩法体验循环」维度的合同**——quest activate/complete、stack count overflow、cooldown overlay、damage float 这些是 ToME-likes 玩家每分钟接触的高频元素，PR-06 必须把它们的视觉合同纳入收口范围。

**总评**：PR-06 当前是一份 **远超平均水平** 的开发文档，但仍处于「manifest 工程合同已经收口，玩法体验合同尚未收口」的中间状态。round-3 修复完成后，PR-06 可以作为 K-ToME 视觉一致性的合同范本进入 PR-07。

---

## 15. 评审签字

| 评审维度 | 结论 | 评审签字 |
| --- | --- | --- |
| 设计总监 / 视觉一致性 | conditional pass，需修 Blocker B1/B2 + High H5/H7/H10 | 待 PR-06 修订后复审 |
| 系统策划 / 战斗反馈循环 | conditional pass，需修 Blocker B2/B3 + High H8/H10 | 待 PR-06 修订后复审 |
| 玩法体验 / Roguelike 探索循环 | conditional pass，需修 Blocker B3 + High H3/H4/H5 + Medium M3/M4 | 待 PR-06 修订后复审 |
| Content pack / mod 友好度 | pass with follow-up，需命名 H9/M6 follow-up | 接受 |
| Accessibility / color-blind | pass with follow-up，需命名 M8 follow-up | 接受 |
| Cross-surface coverage | conditional pass，需修 High H5/H10 + Medium M10 | 待 PR-06 修订后复审 |

PR-06 修订完毕后建议进入 round-4 director review，校验 round-3 Blocker / High 是否全部落到文档与代码；round-4 通过后进入 PR-07 audit。

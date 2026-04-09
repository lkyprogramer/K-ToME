# Phase 4 OPT PR-02 深度审查报告

审查对象：

1. 当前 `git diff`
2. `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-02-elite-mutation-package-and-boss-variant-differentiation.md`

审查范围：

1. elite mutation 扩容
2. dedicated elite talent
3. boss variant mutation/action-weight 差异化
4. i18n / visual / audio manifest 接线
5. white-box / phase4Report 指标透传

审查方法：

1. 阅读 `Phase 4` 权威文档、`PR-06` 基线文档、`OPT` 计划文档与当前目标 PR 文档
2. 审阅当前 diff 与直接相关运行时代码、测试、manifest、资产计划
3. 对 mutation 数量、tier 分布、valid pair、boss variant 组合做静态量化
4. 补跑定向验证命令；其中 `EliteMutationRegistryTest` 已确认通过，`Phase4ReportRunnerTest` 误投到 `:tools:test` 任务，未形成有效通过证据

当前结论：

1. 硬指标层面，本 PR 已达到文档要求的数量与基础报告门槛：
   - `eliteMutationDistinctCount = 12`
   - `eliteMutationValidPairCount = 51`
   - `mutationTierDistribution = MINOR 3 / MAJOR 7 / SIGNATURE 2`
   - `bossVariant` 三组 mutation 组合两两不同
2. 但玩法语义和可读性层面 **不是完全一致**：
   - `corrosion_cloud` 这类 `AURA + DEBUFF` 当前不会真正把 debuff 挂到目标身上
   - `phase_runner` 的 inspect 摘要仍暴露原始 AI profile id，不满足正式可读性
   - `frostbound` 的 AI overlay 没有形成预期的冰霜拉扯节奏，战术 identity 仍偏弱

偏差量化：

1. `一致`: 6 项
2. `部分一致`: 3 项
3. `不一致`: 1 项

---

## Findings

### P1

1. `corrosion_cloud` 的 debuff aura 当前是“有 emitter、无生效”，与文档要求的 hostile debuff 语义不一致。

证据：

1. 文档要求 `AURA + DEBUFF` 继续作用于 aura 半径内 hostile，见 `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-02-elite-mutation-package-and-boss-variant-differentiation.md:82-86`
2. `elite.corrosion_cloud` 被定义为 `AURA`，状态为 `ARMOR_BREAK`，见 `game/src/main/resources/data/elites/index.yaml:129-144`
3. 运行时只是在 `syncMutationAurasFor()` 里为 hostile 创建 `AreaEffectEmitter`，见 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:7071-7111`
4. 但 `applyTurnStartStatusEffects()` 只消费带 `tickDamageType` 的 carrier effect；没有任何把 area emitter 中的非伤害状态写入目标 `EffectTracker` 的路径，见 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:5193-5238`
5. 新增测试本身也证明了这个问题：它断言 player 身上 **没有** `ARMOR_BREAK`，只检查 emitter 存在，见 `game/src/test/kotlin/com/ktome/game/FoundationGameSessionTest.kt:2694-2724`

影响：

1. `corrosion_cloud` 不会产生文档承诺的减甲威胁
2. `abyssal_eclipse` 的 `void_mirror + corrosion_cloud` 组合实际只剩 `void_mirror` 在工作
3. 现有 `dread_aura` 也暴露在同一 carrier 语义缺口下，Boss 变体差异会被进一步削弱

修复建议：

1. 在 `AREA` carrier 的 turn-start 处理中补正式分支：
   - 对 `tickDamage > 0` 的 effect 继续走现有 DOT 结算
   - 对纯状态型 effect，调用 `StatusLifecycle.applyEffect(world, actorId, effect.copy(...))` 或等价路径，把 aura 状态正式挂到受影响 actor 身上
2. 新增回归测试时不要再只断言 emitter 存在，应直接断言：
   - hostile actor 身上出现 `ARMOR_BREAK`
   - owner 身上不出现该 debuff
   - effect 在离开 aura 半径后按预期衰减
3. `BossHarness` 或 `terrain/boss white-box` 应额外记录 aura mutation 是否真正改变了目标状态或防御面板

### P2

1. `phase_runner` 的 inspect 摘要仍显示原始 AI profile id，未达到“正式可读性冻结”。

证据：

1. 文档明确要求 mutation 的 inspect / 可读性走正式路径，见 `docs/phase4/2026-03-13-phase4-pr-06-terrain-interaction-elite-mutation-and-boss-variant.md:18-24` 与当前 PR 文档 `:44-48`
2. `elite.phase_runner` 是新增 `AI_SHIFT` mutation，见 `game/src/main/resources/data/elites/index.yaml:101-113`
3. `mutationSummaryToken()` 对 `AI_SHIFT` 直接把 `aiProfileOverlay` 作为 literal 输出，见 `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:3690-3719`
4. 本地化字符串也是 `AI overlay {profile}` / `AI 覆写 {profile}`，见 `game/src/main/resources/i18n/en-US.json:197-202` 与 `game/src/main/resources/i18n/zh-CN.json:197-202`

影响：

1. 玩家 inspect 到的是 `ai.elite.phase_runner` 这种实现细节，而不是“相位奔行 / 闪现换位”之类的玩法语义
2. 这会削弱 mutation 差异的可解释性，也不利于 Phase 5 做战术 AI / death analysis 的玩家向表达

修复建议：

1. 为 `AI_SHIFT` mutation 增加正式 `summaryKey` 或 `readabilityKey`
2. 至少把 `phase_runner` 的 inspect 文案改成玩家语义，例如“会闪现换位并拉近切入”
3. `mutationSummaryToken()` 不应再把内部 profile id 暴露给 UI

2. `frostbound` 的 AI overlay 没有形成稳定的“冰霜拉扯”节奏，和文档里的水/冰区战术 identity 只部分一致。

证据：

1. 文档将 `elite.frostbound` 的设计意图写成补齐水/冰区的战术 identity，见 `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-02-elite-mutation-package-and-boss-variant-differentiation.md:65-66`
2. 当前 `ai.elite.frostbound` 标了 `defaultBehavior: KITE`，但显式动作只有：
   - 距离 `< 4` 时放 `elite_frost_nova`
   - 距离 `< 2` 时近战
   - 其余可见状态下 `MOVE_TOWARD_TARGET`
   见 `game/src/main/resources/data/ai/index.yaml:742-770`
3. 同文件里的 `ai.crystal.weaver` 已经展示了更完整的 KITE 模式：近距离 `RETREAT_FROM_TARGET`，远距离再 `MOVE_TOWARD_TARGET`，见 `game/src/main/resources/data/ai/index.yaml:700-739`

影响：

1. `frostbound` 更像“贴脸开 nova 的冰系近战”，而不是会拉距离、逼走位、制造寒冰压力的区域型 mutation
2. 这会让 `frostbound` 与 `tidebound` 的体验差异不足，也削弱地下河/水晶洞的元素 identity

修复建议：

1. 给 `ai.elite.frostbound` 补一个 `RETREAT_FROM_TARGET` 分支，至少在目标贴脸时优先退开
2. 把 `frost_nova` 的触发距离收敛到更适合 area-control 的窗口，例如 `2..4`
3. 在 `BossHarness` 或 white-box 中补一个“实际 selected action 分布”指标，证明该 overlay 真能形成不同节奏

3. `grey_crown` 的“支援/增益型指挥者”定位目前只部分落地。

证据：

1. 文档把 `grey_crown` 的目标写成“支援/增益型指挥者”，见 `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-02-elite-mutation-package-and-boss-variant-differentiation.md:124-130`
2. `elite_war_call` 当前只做：
   - 自身 `war_cry_empower`
   - 对半径内 hostile 施加 `war_cry_shaken`
   见 `game/src/main/resources/data/talents/index.yaml:559-600`
3. `ai.elite.war_caller` 的触发条件也是自身低血时放一次这个技能，没有 ally-support、队友节奏强化或指挥型行为，见 `game/src/main/resources/data/ai/index.yaml:361-390`

影响：

1. `grey_crown` 确实比旧版多了第二个 mutation，但“指挥者”感更像“低血自嗨再压制玩家”
2. 从玩法体验看，这更接近强化版 duel boss，而不是能改变战场节奏的 commander boss

修复建议：

1. 如果不想引入新 action catalog，可以在 `elite_war_call` 内复用现有 ally buff 语义，让其在半径内为友军提供短时强化
2. 若当前正式 encounter 场景没有可靠小怪协同，也至少应让 `grey_crown` 的权重重排更明显地围绕 shield / ritual / aura 节奏，而不只是一次低血战吼
3. `BossHarness` 建议新增 base-vs-variant 的 action selection frequency 摘要，否则“差异拉开”只停留在静态配置层

### P3

1. 本次 diff 没有发现必须立即阻塞提交的纯命名或排版类问题。

---

## Requirement Alignment

1. Requirement: `eliteMutationDistinctCount >= 12`
   Evidence:
   - 当前 registry 共 `12` 个 mutation
   - 静态计算结果 `distinctCount = 12`
   Conclusion: 一致

2. Requirement: `eliteMutationValidPairCount >= 40`
   Evidence:
   - 当前静态计算结果 `validPairCount = 51`
   - 各正式 zone 的有效 pair 数均高于 `14`
   Conclusion: 一致

3. Requirement: `mutationTierDistribution` 达到 `MINOR >= 2, MAJOR >= 5, SIGNATURE >= 2`
   Evidence:
   - 当前分布 `MINOR 3 / MAJOR 7 / SIGNATURE 2`
   Conclusion: 一致

4. Requirement: 三个 Boss variant 的 mutation 组合两两不同
   Evidence:
   - `molten_glass = [ironhide, emberblood]`
   - `grey_crown = [dread_aura, war_caller]`
   - `abyssal_eclipse = [void_mirror, corrosion_cloud]`
   Conclusion: 一致

5. Requirement: 长期保留的 elite ability 使用 dedicated `elite_*` talent，并避免直接绑定玩家 talent id
   Evidence:
   - 已新增 `elite_war_call`
   - 已新增 `elite_frost_nova`
   - 已新增 `elite_phase_step`
   - 三者都带 `tags: [talent, elite_only, monster_only, mutation]`
   Conclusion: 一致

6. Requirement: `AURA` 的 `DEBUFF` 继续作用于 aura 半径内 hostile；`BUFF` 只刷新 owner 自身
   Evidence:
   - `BUFF` 分支已直接写回 owner，自测路径可达
   - `DEBUFF` 分支只创建 `AreaEffectEmitter`，但没有把非伤害状态写入目标 actor 的路径
   Conclusion: 不一致

7. Requirement: `grey_crown` 形成“支援/增益型指挥者”
   Evidence:
   - 配置上新增了 `war_caller`
   - 但技能与 AI 仍以 self-buff + hostile debuff 为主，没有形成明显 commander / ally-support 节奏
   Conclusion: 部分一致

8. Requirement: `frostbound` 补齐水/冰区战术 identity
   Evidence:
   - 已新增 cold package、专用 talent 和专用 AI overlay
   - 但该 overlay 缺少 retreat 行为，实际更偏向近身压制，不像拉扯/控场
   Conclusion: 部分一致

9. Requirement: mutation inspect / readability 走正式可读路径
   Evidence:
   - 名称、icon、audio key 已接正式路径
   - `AI_SHIFT` 摘要仍直接显示内部 profile id
   Conclusion: 部分一致

10. Requirement: 新 mutation 的 i18n / icon / audio cue 进入正式 manifest / lint / harness
    Evidence:
    - `game` 数据索引已接新 visual/audio ids
    - `assets-src/*manifest.json` 与 `client/src/main/resources/manifests/*` 已接新 key
    - 客户端资源目录下已存在 `6` 个新图标与 `6` 个新音频文件
    Conclusion: 一致

11. Requirement: `BossHarnessTest` / `phase4Report` 增加本 PR 指标
    Evidence:
    - `BossHarnessTest` 已新增 `eliteMutationDistinctCount`、`eliteMutationValidPairCount`、tier 分布、boss variant pairwise distinct
    - `Phase4ReportRunner` 已透传这些指标
    Conclusion: 一致

---

## Removal/Iteration Plan

- 没有识别到可以立即安全删除的旧路径；当前更合适的是补 runtime 语义和 white-box 证据，而不是先做删除式清理。
- 建议在修复 `AURA + DEBUFF` 语义后，再评估是否把当前“仅暴露 raw AI profile id 的 inspect 摘要”替换为正式玩家向 summary key，并移除这类调试式文案。

---

## Additional Suggestions

1. `BossHarness` 目前证明了“静态组合不同”，但还没有证明“实际行动分布不同”。建议追加每个 variant 的 `selectedActionIds` 频率表，把“差异拉开”从配置差异升级为运行时证据。
2. 既然 `validPairCount` 已经进入 Phase 4 指标，建议顺手把 `perZoneValidPairCount` 也透传到 `phase4Report`，这样能更快看出某个 zone 是否出现组合贫瘠。
3. `incompatibleWith` 目前只校验“引用存在”，没有 fail-fast 校验互斥关系是否双向对称。下一步可以补一个轻量 contract test，避免后续改表时 silently drift。

---

## Suggested Verification

建议命令：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :game:test
./gradlew bossHarness
./gradlew terrainInteractionBatch
./gradlew whiteBoxVerify
./gradlew phase4Report
./gradlew assetLint
./gradlew audioLint
./gradlew manifestLint
./gradlew clientSmoke
```

建议白盒步骤：

1. 在 `deep_iron_pit` 或 `molten_core` 固定 seed 刷出 `corrosion_cloud` 精英，确认玩家贴身进入 aura 后，面板或 inspect 中实际出现 `ARMOR_BREAK`，而不是只有 emitter/日志痕迹。
2. 在 `grey_gate_depths` 固定 seed 下对比 base `dungeon_lord` 与 `grey_crown`：
   - 观察低血时是否真的出现“指挥/增益”节奏，而不是一次普通 self-buff
   - 对比 `ritual_break`、护盾与 mutation 相关行为的实际触发频率
3. 在 `underground_river` / `crystal_cavern` 固定 seed 下观察 `frostbound` 精英：
   - 是否会在近身时主动拉开距离
   - 是否能形成“冰控+拉扯”而不是近战贴脸 nova
4. inspect 任意 `phase_runner` 精英，确认 mutation 摘要不再显示内部 profile id。

实际执行情况：

1. 已完成静态 diff 审查、文档对照、资源文件存在性检查与 mutation 指标量化
2. 已确认 `EliteMutationRegistryTest` 通过
3. 一次定向 Gradle 命令中把 `Phase4ReportRunnerTest` 错投到了 `:tools:test`，该命令没有形成完整的 `phase4Report` 通过证据；后续应改用 `:tools:phase4Report` 或 root `phase4Report`

---

## Summary

这是一个“硬指标基本达标，但玩法语义还没完全站稳”的 PR。

已经达标的部分：

1. mutation 数量、tier 分布、valid pair、Boss 组合差异、资源 key、manifest 接线、报告指标透传
2. dedicated elite talent 路线整体正确，没有退回“直接复用玩家 talent id”

尚未达标或只部分达标的部分：

1. `corrosion_cloud` 的 debuff aura 当前不真正落到目标身上，这是本报告唯一明确的 `不一致`
2. `phase_runner` 的 inspect 可读性仍暴露实现细节
3. `frostbound` 与 `grey_crown` 的战术 identity 仍偏弱，离“差异拉开”还有最后一段路

综合判断：

1. 作为数据扩容和 contract 接线，这个 PR 可以判定为“主体完成”
2. 作为“elite mutation package and boss variant differentiation”的体验签收，它还不应被判定为完全收口
3. 建议先修 `AURA + DEBUFF` 运行时语义，再补一轮 boss/elite 行为差异 white-box 证据，然后再做最终体验签收

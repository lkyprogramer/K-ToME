# Phase3 深度审查报告（Part 4/4）

## 6. 优化建议（按优先级，续）

### P1-2 补齐玩家信息面，把内部 token 收口成玩家语言

- 问题本质：Phase3 的系统名词已经很多，如果 UI 继续直接暴露内部 token，理解成本会被无意义放大。
- 影响范围：world map、route preview、main menu / player creation、outcome summary、i18n。
- 优化目标：让玩家在做路线、职业、奖励判断时看到的是“含义”，不是“实现名”。
- 具体改法：
- 把 `levelBandRef` 本地化为明确的推荐等级文案，例如“推荐等级 3-5”，不要再展示 `lv3_5`。
- 把 `rescueTags` 映射到可读标签，例如“位移 / 净化 / 护盾 / 恢复”，不要直接显示 `MOVEMENT / CLEANSING / PROTECTION / RECOVERY`。
- main menu 只默认展示当前可玩的职业/种族；`DEV_UNLOCKED` 可以显示解锁提示，`frozen` stub 不应作为默认轮播项。
- 结算页补充更可理解的失败概括，例如关键 Boss 击杀进度、路线奖励领取概览、最后一次资源崩塌点。
- 优先级：P1
- 预期收益：新手理解成本明显下降，路线与奖励选择更可读，Phase3 的复杂系统不会被 UI 反向抹黑。
- 可能副作用 / 风险：需要同步扩充 i18n key，并更新 golden screenshot。
- 需要同步修改：`client/render/*`、`client/screen/*`、`game/FoundationGameSession.kt`、i18n、golden screenshot。

### P1-3 让 late-game zone 至少各有一个可记忆的玩法钩子

- 问题本质：后段 zone 的“存在感”目前主要靠名字、推荐等级和 Boss，不靠过程。
- 影响范围：`underground_river`、`crystal_cavern`、`abyssal_temple`、`abyssal_heart` 的 objective / interactable / encounter / audio-visual 组织。
- 优化目标：把“打到后面更像同一张图的延长版”改成“打到后面确实在经历更危险、更独特的段落”。
- 具体改法：
- `underground_river` 增加至少 `1` 个与地形/位移相关的 objective interactable。
- `abyssal_temple` 增加至少 `1` 个与净化/护盾/抗性相关的 route-specific threat 或奖励点。
- `abyssal_heart` 在最终 Boss 前增加一个短前置事件，让 finale 不只是进门即 Boss。
- 即使不新增整套 tileset，也至少在 ambient、overlay、日志、交互点上做区分，避免后段三段体验几乎同色同味。
- 优先级：P1
- 预期收益：后程动力会从“快通关了”转回“这段本身有内容可玩”。
- 可能副作用 / 风险：会增加长局 headless turn，需要同步平衡总时长。
- 需要同步修改：zones/objectives/interactables data、session logic、client 资源与白盒步骤。

### P1-4 降低“统一铭文包”对职业差异的压平效应

- 问题本质：当前大多数稳定 build 都会收束到相似的 `5-8` 热键铭文组合，职业 panic answer 被一套公共工具包覆盖得太彻底。
- 影响范围：铭文池、shop offer、route reward、职业起手装与长期构筑差异。
- 优化目标：保留 Phase3 的救火底线，同时减少所有职业都长成同一套外部工具箱。
- 具体改法：
- 仍然只在 `HEALING / MOVEMENT / PROTECTION / CLEANSING` 四大类内做文章，不违反当前 Phase3 非目标。
- 每一类至少做 `2~3` 个具备不同 tradeoff 的 variant，让商店与路线奖励给出不同组合，而不是全职业收束到同四个最优项。
- 把部分 variant 与职业资源轴、zone route、rescue tag 绑定，让构筑在“保命工具”层也有分歧。
- 优先级：P1
- 预期收益：职业差异不会被一套万能铭文方案过度抹平，重复游玩价值提升。
- 可能副作用 / 风险：需要重新调 shop affordability 和 smoke bot 逻辑。
- 需要同步修改：inscriptions data、shops data、bot/harness、i18n。

### P2-1 扩展 `RunSummary` 的 build 语义，不再只留一个偏薄的 `buildHash`

- 问题本质：当前 summary 足够做解锁和粗粒度报告，不足以支撑 build 分析。
- 影响范围：`ProfileData`、`RunSummary`、报告生成、回归分析脚本。
- 优化目标：让 run history 真正能回答“这局是什么 build、为什么赢、为什么输”。
- 具体改法：
- 在不污染存档边界的前提下，补充 stat allocation、reserve talents、race talents、关键 affix、铭文 variant 的结构化摘要。
- 保留 `buildHash` 作为快速指纹，但不要把它当唯一 build 语义载体。
- 优先级：P2
- 预期收益：后续 balance review、玩家历史回顾、问题复盘都会更有抓手。
- 可能副作用 / 风险：profile schema 需要升版本，历史数据要处理兼容或 fail fast。
- 需要同步修改：`core/profile/*`、`GameApp` profile persistence、相关文档。

### P2-2 把 zone metadata 至少接到轻量 runtime 反馈里

- 问题本质：`specialMechanics / environmentTheme / uniqueContentTag` 现在多数只是静态数据。
- 影响范围：zone data、render/log/harness。
- 优化目标：哪怕不把这些字段都做成复杂系统，也要让它们在运行时有最低可感知性。
- 具体改法：
- 至少让这些字段驱动一段 zone intro log、一条 world map 提示、一个 encounter modifier 或一个 reward bias。
- 对后段 zone 的 metadata 做最小白盒验证，而不是只保证字段非空。
- 优先级：P2
- 预期收益：schema 不再是“写给文档和测试看”的死字段。
- 可能副作用 / 风险：如果做得太轻会变成另一层装饰字段，需要避免只加提示不加行为。
- 需要同步修改：zone data、session/log、相关 tests。

## 7. 可延后到后续阶段的问题

下面这些问题可以延后，但前提是本报告标出的 P0 / P1 已先处理：

### 7.1 深度 ProcGen 与隐藏内容生态

- 为什么可以延后：这是 Phase4 正题，Phase3 不需要进入混合拓扑、vault、lock-key、hidden event 体系。
- 当前是否需要兜底：需要。当前阶段至少要先让 optional zone 的独有内容真正落地，否则 Phase4 的 hidden content 只会建立在假分支之上。

### 7.2 真正丰富的 unique / artifact / loot ecology

- 为什么可以延后：Phase3 只要求 affix v1，不要求完整 unique/artifact 生态。
- 当前是否需要兜底：需要。当前阶段至少要把路线/Boss/缓存奖励接入 affix 驱动，否则 Phase4 再加 unique/artifact 也只是补在扁平奖励链上。

### 7.3 Tactical AI / Utility scoring / Death analysis

- 为什么可以延后：这是 Phase5 的主题，Phase3 不需要进入 Utility AI 和完整死亡分析。
- 当前是否需要兜底：需要。当前阶段至少要让 `bossHarness` 真正断言 `AIDecisionTrace`，否则后续 Tactical AI 没有可信基线。

### 7.4 `Shadowblade / Warden / orc / undead` 的完整可玩化

- 为什么可以延后：PR-05 明确允许它们只冻结 schema，不要求在本阶段完全 playable。
- 当前是否需要兜底：需要。当前阶段不应把这些 frozen stub 当作正式 player-facing 选择展示；至少应从默认创建入口降噪。

## 8. 最终结论

### 当前 Phase3 是否达到了文档预期

没有完整达到。

更准确地说：

- 规则基础设施大体达标。
- 工程合同与验证入口大体达标。
- 内容密度、奖励驱动、分支厚度、后段体验和验证严密度没有达到“完成态”。

如果按“系统都已经有”来打分，Phase3 可以算高完成度。

如果按“文档承诺的长局 build depth 是否真正成立”来打分，当前不能算完成。

### 当前版本是否已经“好玩”

可以玩，也能通关，但“好玩”主要来自：

- 类 ToME 题材本身的吸引力
- 玩家职业侧系统已经够完整
- 长局骨架已经有连续感

不是来自：

- 足够厚的 build 分歧
- 足够强的奖励爽点
- 足够鲜明的分支与敌方内容差异

我的判断是：

- 当前版本“基本可玩”成立。
- 当前版本“稳定好玩”不成立。

### 当前版本是否具备“耐玩雏形”

有雏形，但还只是雏形。

驱动重复游玩的三个关键支柱里：

- 职业底盘：有雏形
- 路线分支：有外形，缺肉
- 掉落构筑：有系统，没进主奖励节点

所以它具备“耐玩雏形”，但还没有进入“我愿意连续打很多局试不同 build”的状态。

### 是否适合进入下一阶段开发

不建议直接无条件进入 Phase4。

更合理的判定是：

- 可以开始做 Phase4 级别的技术预研。
- 不应该把当前版本当成“Phase3 已收口完毕”的稳定地基。
- 在进入正式 Phase4 主线开发前，至少应先完成本报告里的 P0 项。

### 进入下一阶段前必须先补的内容

必须先补的不是“小修小补”，而是这四类地基问题：

1. 把 objective / optional zone / unique content 从 schema 装饰项变成真实玩法。
2. 把 Boss / route / cache reward 接到 affix 驱动上，让奖励真正推动 build。
3. 把基础职业树和 encounter roster 补到不再明显低于 Phase3 路线图底线。
4. 把 `longRunLab / bossHarness` 的门禁改到能识别“太线性、太容易、trace 不充分”的版本。

一句话收尾：

- 当前 Phase3 是“系统像 Phase3，体验还没完全像 Phase3”。
- 如果现在就宣布完成并进入 Phase4，后续会持续为这个判断买单。

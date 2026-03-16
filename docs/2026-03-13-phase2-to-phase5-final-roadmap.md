# K-ToME Phase 2 ~ Phase 5 最终长期开发路线图（AI 执行版）

> 日期：2026-03-13  
> 状态：Draft for execution  
> 适用范围：Phase 2 ~ Phase 5  
> 文档定位：后续阶段执行权威；若与此前的 Phase 2~5 draft 冲突，以本文为准
> 详细系统设计补充：见 [2026-03-13-core-systems-design-and-phase-supplements.md](./2026-03-13-core-systems-design-and-phase-supplements.md)

## 1. 直接结论

后续开发不再沿用“Phase 2 一次性全面重建，随后继续线性堆系统”的路线。

新的主线固定为：

`继承 Phase 1 的可玩 + 可证合同 -> 先固化跨阶段语义合同 -> 再做最小 Tile 闭环 -> 再做深层战斗与长局结构 -> 再做 ProcGen/Loot 深化 -> 最后做战术 AI、性能、QA 与发布收口`

关键调整如下：

1. 保留 `Phase 2 ~ Phase 5` 的长期目标，但每个 phase 内拆成更小的检查点和工作包。
2. 优先固化会引发全局返工的合同：序列化、事件、日志 token、RenderSnapshot、DamageType、ResourcePool、状态系统、i18n key、manifest key。
3. 每个 phase 都必须以一个真实可玩的纵切收尾，不允许长时间处于“主干不可玩”的重构状态。
4. 不考虑旧存档兼容性。允许阶段间破坏式升级，但必须明确版本与失效提示。
5. Phase 4 只做到数据包与 Registry Overlay 级别的可扩展性，不把 Lua runtime / 完整 Mod SDK 绑进 v1 主路径。

---

## 2. 输入与覆盖关系

本文综合以下文档生成：

1. [docs/mvp-development-guide.md](./mvp-development-guide.md)
2. [docs/phase1/2026-03-12-phase1-roadmap.md](./phase1/2026-03-12-phase1-roadmap.md)
3. [docs/K-ToME_Phase2_to_Phase5_PR_Development_Guide_v2_SinglePlayer_Tile_i18n.md](./K-ToME_Phase2_to_Phase5_PR_Development_Guide_v2_SinglePlayer_Tile_i18n.md)
4. [docs/2026-03-13-phase2-5-review-and-recommendations.md](./2026-03-13-phase2-5-review-and-recommendations.md)
5. [docs/Roguelike 游戏开发指导文档.md](./Roguelike%20游戏开发指导文档.md)

覆盖规则：

1. 本文继承 Phase 1 的架构边界、测试门禁、白盒自证和阶段出口合同。
2. 原 Phase 2~5 draft 中的长期目标、术语、资源与回归思想可继续参考，但不再作为直接执行顺序。
3. review 文档中的阻塞项、迁移断层和粒度问题，已视为必须吸收的修订意见。
4. 技术白皮书中的有效内容只以“契约、抽象、验证方式”进入主线；过度具体的算法拍板和过大的内容预算不作为硬承诺。
5. 旧 `PR01 ~ PR12` 编号体系只保留为历史参考；从本路线图开始，执行编号统一以 `P2-W1 ~ P2-W7`、`P3-W1 ~ ...` 为准。

---

## 3. Phase 1 继承基线

### 3.1 不可破坏的继承合同

1. `core` 继续作为规则真源，保持零引擎依赖。
2. `game` 继续负责内容装配、注册表、官方内容与会话胶水。
3. `client` 继续只负责渲染、输入、窗口、音频、UI 和表现编排。
4. 每个阶段都必须同时满足：
   - 可运行
   - 可测试
   - 可度量
   - 可白盒验证
5. 新增核心逻辑必须走确定性测试：固定 seed、固定输入、固定输出断言。
6. `JaCoCo` 覆盖率门禁和白盒步骤不能退化成可选项。

### 3.2 当前代码到后续阶段的迁移基线

以下表格只记录当前仓库真实存在、且会影响 Phase 2 起路线的断层：

| 当前实现 | 当前位置 | 后续目标 | 首次落地阶段 |
| --- | --- | --- | --- |
| `TurnScheduler` 仍是 `100` 阈值，按 `EntityId` 排序 | `core/turn` | 升级为 `1000` 能量制，固定确定性排序合同 | `P2-A` |
| 存档主链已使用 `kotlinx.serialization`，但 Schema V2/版本纪律仍需继续收口 | `core/save` | 固化 `Save Schema V2`、fail-fast 版本策略与资产边界 | `P2-A` |
| 日志仍是 `ArrayDeque<String>`，大量裸文案 | `game/FoundationGameSession` | 切到 `LogTokenEvent + args`，文案只在 client 渲染 | `P2-A` |
| 天赋定义仍直接带 `name/description`，状态类型只有少数基础项 | `core/talent` | 切到 key 驱动 schema，扩展状态类型与生命周期模型 | `P2-A` / `P3-A` |
| 当前正式表现依赖 glyph / ASCII | `client/render`、`game/ActorView` | 切到 `RenderSnapshot + VisualKey/AudioKey` | `P2-B` |
| 当前 AI 主要是简单行为模板 | `core/ai` | 在 Phase 3 前引入脚本化行为层作为过渡 | `P3-A` |
| 当前 Save / Profile / Content schema 没有长期版本纪律 | `core/save`、`game/data` | 建立破坏式升级可接受的版本策略与失效提示 | `P2-A` |

### 3.3 允许直接复用的资产

1. 构建与模块基线：Java 21、Kotlin 2.2.21、Gradle 8.14.3、libGDX 1.14.0、多模块结构。
2. 规则资产：ECS、地图、FOV、A*、战斗、属性、物品、天赋、随机源、存档骨架、基础 AI。
3. 装配资产：YAML 内容加载、工厂、会话、现有 test fixture 和 automation driver。
4. 验证资产：`./gradlew test`、`JaCoCo`、固定 seed 的白盒检查方式。

---

## 4. 全局开发合同

### 4.1 产品与范围合同

1. 只做单机单人 run。
2. 不做联机、账号、云存档、排行榜、队友依赖职业、共斗机关。
3. 所有职业必须满足单通合同：
   - 主输出路径
   - 自保路径
   - 位移或脱困
   - 群体处理
   - Boss answer
   - panic answer
4. 从 `P2-B` 起，Tile 是正式可玩路径；`P2-A` 可以临时保留 ASCII 作为迁移和调试 harness，但不再作为正式出口。

### 4.2 模块边界合同

| 模块 | 权威职责 | 明确禁止 |
| --- | --- | --- |
| `core` | 规则、公式、状态、AI、地图、掉落、事件、存档 DTO、RenderSnapshot DTO | libGDX 类型、最终本地化文本、纹理句柄、屏幕状态 |
| `game` | 内容 schema、注册表、官方内容包、组装会话、数据校验 | 重新实现规则、保存表现层副本、绕过 `core` |
| `client` | Tile/UI/输入/音频/Locale bundle/manifest 消费/golden screenshot | 地图生成、战斗结算、AI 决策、权威世界状态 |
| `tools` | lint、资源 pipeline、headless smoke、golden seed、solo clear、perf soak | 直接侵入运行时规则路径 |

### 4.3 跨阶段语义合同

从 Phase 2 开始，以下合同必须尽早稳定：

1. `GameEvent` / `LogTokenEvent`
2. `RenderSnapshot`
3. `DamageType` / `DamageInstance`
4. `ResourcePool` / `ResourceType`
5. `StatusEffectType` / `ActiveEffect`
6. `nameKey / descKey / uiKey / logKey / loreKey`
7. `visualKey / iconKey / audioProfile / vfxProfile`
8. `saveVersion / profileVersion / contentSchemaVersion / assetManifestVersion`

推荐的正式内容最小字段：

| 字段 | 用途 |
| --- | --- |
| `id` | 稳定内容 ID |
| `nameKey` | 名称本地化 key |
| `descKey` | 描述本地化 key |
| `visualKey` | 主视觉 key |
| `iconKey` | UI 图标 key |
| `audioProfile` | 音频路由 key |
| `tags` | 检索、掉落、AI 与构筑标签 |
| `logProfile` | 日志/tooltip/战斗词条映射 |

### 4.4 存档与版本合同

用户已明确：不需要考虑任何旧存档兼容性。

因此固定采用以下策略：

1. 允许在检查点之间直接 bump `saveVersion` / `profileVersion`。
2. 默认不写旧版本迁移器。
3. 当前主线只保证“同一阶段当前格式”的存读稳定。
4. 发现版本不匹配时，UI 只需明确提示“旧存档已失效，需要新开局”，不承担兼容义务。
5. 存档仍然只存 ID、数值和 token，不存最终渲染后的文本。

### 4.5 资源与工具链合同

1. 资源流水线是离线开发工具，不进入运行时。
2. 图片生成 provider 必须可替换，不能把 Gemini 写成唯一来源。
3. `P2-B` 前只需要能支撑最小可玩切片的资源集，不把大规模资产预算绑成主路径阻塞项。
4. `P4` 只做到内容包与 Overlay 级扩展；Lua runtime / 完整 Mod SDK 进入 `Post-v1` backlog。

音频来源策略：

1. `SFX` 首选开源音效库（如 `freesound.org` 的可商用许可资源），不足部分可用 AI 合成补齐。
2. `BGM` 与环境循环允许使用 AI 音乐生成，但必须经过人工审核、裁切与授权归档。
3. 所有音频条目都必须显式记录 `license/source/provider`，进入 `AudioSpec`。
4. 运行时只消费 `AudioCueManifest`，不直接引用原始音频来源路径。

### 4.6 性能与可观测性合同

1. 性能基线从 `P2-B` 就建立，不等到 `Phase 5` 才开始度量。
2. 渲染、FOV、音频、资源装载都要有可回归的 smoke 或 profile 指标。
3. 所有“看起来更快”的优化都必须有对照数据，不接受拍脑袋的提前优化。

---

## 5. AI 开发与 AI 验证工作流

### 5.1 四条并行开发线

后续阶段默认按四条线拆任务，而不是按“一个超大 PR 全包”推进：

1. `Rules Lane`
   - `core`
   - 规则、公式、事件、状态、AI、存档 DTO
2. `Client Lane`
   - `client`
   - Tile、UI、输入、音频、golden screenshot
3. `Content Lane`
   - `game`
   - YAML/JSON、职业、怪物、掉落、zone、世界内容
4. `Tools/QA Lane`
   - `tools`
   - lint、smoke、seed harness、solo clear、perf soak

### 5.2 工作包设计规则

每个工作包都必须满足：

1. 只引入一个新的核心抽象族。
2. 同时触碰的生产模块不超过两个；如超过，必须再拆。
3. 必须带自动化验证和白盒步骤，不接受“先实现，之后补验证”。
4. 必须声明范围、非目标、依赖、版本影响。
5. 若一个工作包结束后主干不可玩，则说明它切分仍然过大。

### 5.3 固定回归套件

后续阶段统一保留八套长期回归：

| 套件 | 目标 | 首次建立 |
| --- | --- | --- |
| `GoldenSeed` | 固定 seed 的规则与流程回归 | `P2-A` |
| `LocaleLint + LocaleScreens` | 双语言 key 与截图布局回归 | `P2-A` / `P2-B` |
| `ContractLint` | content/manifest/schema 完整性 | `P2-A` |
| `SoloClearLab` | 职业单通与关键遭遇自证 | `P2-C` |
| `HeadlessSmoke` | save/load、snapshot、地图、任务、客户端最小烟雾 | `P2-B` |
| `BossHarness` | Boss warning、AI、阶段切换与 scripted 行为自证 | `P2-B` |
| `SaveCurrentVersion` | 当前阶段主线格式是否可靠存读 | `P2-A` |
| `Perf/Soak` | 长局稳定性、性能回归、资源泄漏 | `P5-A` |

### 5.4 统一验证入口

从 Phase 2 起，路线图默认以以下 root alias 命令族为门禁目标；若实现上落在子模块任务，也应对外暴露同名聚合入口：

```bash
./gradlew test
./gradlew :core:test
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
./gradlew contractLint
./gradlew localeLint
./gradlew headlessSmoke
./gradlew goldenScreenshot
```

在后续阶段逐步补齐：

```bash
./gradlew soloClearLab
./gradlew perfSmoke
./gradlew :tools:soak
```

说明：

1. 上述 `tools` 任务很多目前还不存在，它们本身就是 `P2-A ~ P2-C` 的交付内容。
2. 新增任务必须先建立命令入口，再扩大覆盖范围。

---

## 6. 路线图总览

| Phase | 核心目标 | 中间检查点 | 结束形态 |
| --- | --- | --- | --- |
| `Phase 2` | 迁移跨阶段合同并完成最小 Tile / i18n 可玩闭环 | `P2-A` / `P2-B` / `P2-C` | `v0.2.x`，双语言、Tile、4 职业短局 |
| `Phase 3` | 建立深层战斗、正式职业树和世界分支，形成长局 | `P3-A` / `P3-B` / `P3-C` | `v0.3.x`，4~6 小时单人长局 |
| `Phase 4` | 深化 ProcGen 与 Loot 生态，增强 replayability，并建立内容包扩展点 | `P4-A` / `P4-B` / `P4-C` | `v0.4.x`，重复游玩差异明显 |
| `Phase 5` | 战术 AI、性能、回放、QA 与发布收口 | `P5-A` / `P5-B` / `P5-C` | `v1.0.0`，稳定发布版 |

---

## 7. Phase 2 — 语义合同迁移与最小 Tile 闭环（v0.2.x）

### 7.1 目标

在不破坏 Phase 1 可玩性的前提下，先完成跨阶段语义合同迁移，再完成最小 Tile、最小双语言、最小内容闭环。

### 7.2 非目标

1. 不追求长局深度。
2. 不追求完整资源大生产。
3. 不做最终战斗公式。
4. 不做完整 Mod SDK。

### 7.3 检查点

| 检查点 | 目标 | 结果要求 |
| --- | --- | --- |
| `P2-A` | 先完成合同与迁移基线 | 新事件/新日志/新 schema/新存档已落地，旧 Phase 1 内容仍可跑 |
| `P2-B` | 建立最小 Tile 可玩切片 | 首页切语言、最小 Tile 渲染、1 条可通关切片 |
| `P2-C` | 扩成完整短局 | 4 职业、4 zone、双语言、Tile、保存当前阶段可用 |

### 7.4 工作包切分

| ID | 所属检查点 | 目标 | 主要模块 | 依赖 | 完成定义 |
| --- | --- | --- | --- | --- | --- |
| `P2-W1` | `P2-A` | 在已采用 `kotlinx.serialization` 的基线之上，冻结 `Save Schema V2`、版本纪律与资产边界 | `core`, `game` | 无 | 当前格式可存读；版本不匹配有明确提示；save 与 manifest/style 版本边界清晰 |
| `P2-W2` | `P2-A` | 1000 能量调度、`DamageType`、`ResourcePool`、基础状态扩展、事件总线与回调注册表 | `core` | 无 | 战斗/行动事件可稳定哈希；资源与伤害通道通过单测 |
| `P2-W3` | `P2-A` | i18n key、日志 token、首页 locale 选择、`locale-lint` 骨架 | `game`, `client`, `tools` | `P2-W1`, `P2-W2` | 正式文本不再新增裸字符串；日志可按 locale 重渲染 |
| `P2-W4` | `P2-A` | `RenderSnapshot`、`VisualManifest`、`AudioManifest`、`contract-lint` | `core`, `client`, `tools` | `P2-W2`, `P2-W3` | 相同 world state 的 snapshot 哈希稳定；manifest key 能校验 |
| `P2-W5` | `P2-B` | 最小 TileRenderer、最小 HUD/背包/检视、golden screenshot 基线 | `client`, `tools` | `P2-W4` | 1 个 seed 下的截图可回归；局内界面可中英双语显示 |
| `P2-W6` | `P2-B` | 最小正式内容切片：2 职业、1 条 zone 链、1 个 Boss、基础音画资源集 | `game`, `client` | `P2-W5` | Tile 模式下可完整跑通一条 30~60 分钟切片 |
| `P2-W7` | `P2-C` | 短局扩展：4 职业、4 zone、24 怪、24 物品、`SoloClearLab` v1、locale/content 覆盖清盘 | `core`, `game`, `client`, `tools` | `P2-W6` | Phase 2 出口全部满足，ASCII 退出正式路径 |

执行映射说明：

1. `P2-W1 ~ P2-W7` 是唯一执行编号。
2. 若需要追溯原始 PR 指南，可按以下近似映射理解：

| 执行工作包 | 历史 PR 参考 |
| --- | --- |
| `P2-W1` | `PR01/PR11` 的存档与版本纪律部分 |
| `P2-W2` | `PR01/PR09` 的规则合同部分 |
| `P2-W3` | `PR02/PR10` 的 i18n 与日志部分 |
| `P2-W4` | `PR03` |
| `P2-W5` | `PR04/PR07` |
| `P2-W6` | `PR05/PR06/PR08` |
| `P2-W7` | `PR09/PR10/PR11/PR12` |

3. `P2-W2` 允许在实现上拆成 `W2a（1000 能量 + 事件总线）` 与 `W2b（DamageType + ResourcePool + 状态扩展）` 两个提交序列，但验收仍按一个工作包结算。

### 7.5 最小可发布集

`Phase 2` 只承诺最小可发布集，不承诺原 draft 中的大规模预算：

| 类别 | 最低交付 |
| --- | --- |
| 可玩职业 | 4 个基础职业 |
| 天赋 | `4 x 8 = 32` |
| 可玩种族 | 1 个默认种族 |
| Zone | 4 个 |
| 怪物模板 | 24 个 |
| 基础装备/消耗品 | 24 个 |
| Tile / Prop | 120 个左右 |
| Icon | 60 个左右 |
| VFX | 15~20 套 |
| SFX | 40 条左右 |
| BGM / Ambience | 2 首音乐 + 4 条环境循环 |

### 7.6 Phase 2 出口标准

1. 4 个职业都能完成默认难度短局。
2. 首页切语言后，新开局与读当前阶段存档都能按所选语言重渲染。
3. 正式路径中不再依赖 ASCII 表现。
4. 当前阶段 save/load、日志、snapshot、manifest、locale、golden screenshot 全部有回归入口。
5. `SoloClearLab` 至少覆盖四职业的杂兵包、精英战、Boss 战。

---

## 8. Phase 3 — 深层战斗、正式职业树与长局结构（v0.3.x）

### 8.1 目标

在 Phase 2 的合同之上，把“短局可玩”升级为“有构筑深度的长局可玩”。重点是战斗公式、状态/持续、天赋树 schema、脚本化 AI、世界分支与掉落驱动。

### 8.2 非目标

1. 不做最终 ProcGen 深水区。
2. 不做完整数据包平台化。
3. 不做全量商业级内容堆量。

### 8.3 检查点

| 检查点 | 目标 | 结果要求 |
| --- | --- | --- |
| `P3-A` | 战斗深度核心 | 公式、状态、天赋 schema、CombatTrace、脚本化 AI 可自证 |
| `P3-B` | 构筑与角色扩展 | 基础职业正式化，种族/Profile 进入主线 |
| `P3-C` | 长局结构成型 | 世界分支、Boss telegraph、affix v1、完整长局 |

### 8.4 工作包切分

| ID | 所属检查点 | 目标 | 主要模块 | 依赖 | 完成定义 |
| --- | --- | --- | --- | --- | --- |
| `P3-W1` | `P3-A` | 战斗公式 V2：命中、防御、Power/Save、暴击、护甲/抗性、`CombatTrace` | `core` | `Phase 2` | 所有关键对抗都有金样本回归，描述与 trace 对齐 |
| `P3-W2` | `P3-A` | 状态/持续/铭文骨架，扩展回调生命周期与 UI 状态语义 | `core`, `client` | `P3-W1` | buff/debuff/sustain/zone effect 生命周期可自证 |
| `P3-W3` | `P3-A` | 天赋树 schema V2、动态说明、关键词注册表、断点成长 | `core`, `game`, `client` | `P3-W1`, `P3-W2` | 天赋解析、升级预览、回滚和洗点都可测试 |
| `P3-W4` | `P3-B` | 脚本化 AI、Boss telegraph 语法、精英/Boss 状态机 v1 | `core`, `client` | `P3-W2`, `P3-W3` | 高伤技能都有预警；AI 不依赖全知全能 |
| `P3-W5` | `P3-B` | 4 个基础职业正式化，新增 2 个进阶职业，接入 3 个种族与本地 Profile | `game`, `client`, `tools` | `P3-W3` | 至少 2 条可通关 build；职业/种族组合可进局 |
| `P3-W6` | `P3-C` | 世界分支、zone 入口、主支线任务、affix v1、经济循环、长局回归 | `core`, `game`, `client`, `tools` | `P3-W4`, `P3-W5` | 4~6 小时长局闭环成立，掉落能驱动构筑差异 |

### 8.5 最小可发布集

| 类别 | 最低交付 |
| --- | --- |
| 基础职业正式树 | `4 x 16 = 64` 天赋 |
| 进阶职业 | 2 个 |
| 种族 | 3 个 |
| 世界 Zone | 8 个左右 |
| 怪物模板 | 60 个左右 |
| 新装备/掉落条目 | 60 个左右 |
| 新图标 / VFX | 120 个左右 |
| 新 SFX / BGM | 60 条左右 / 3 首左右 |

### 8.6 Phase 3 出口标准

1. 形成稳定的 4~6 小时单人长局。
2. 4 个基础职业和 2 个进阶职业都通过 `SoloClearLab` 与关键 Boss 回归。
3. 种族、Profile、世界分支、Boss telegraph、掉落 affix v1 全部可白盒验证。
4. 双语言在完整长局中无明显术语漂移、无大面积 UI 爆版。

---

## 9. Phase 4 — ProcGen 与 Loot 生态深化，可扩展数据包落地（v0.4.x）

### 9.1 目标

把游戏从“可完成的长局”推进到“重复游玩差异明显的长局”。重点是混合拓扑地图、可解性验证、掉落生态深化、精英变体与隐藏内容。

### 9.2 非目标

1. 不引入 Lua runtime。
2. 不冻结完整 Mod SDK。
3. 不做平台级脚本宿主。

### 9.3 检查点

| 检查点 | 目标 | 结果要求 |
| --- | --- | --- |
| `P4-A` | ProcGen 深化 | map family、pattern、环路、lock-key 可解性稳定 |
| `P4-B` | Loot 与遭遇生态深化 | affix/unique/artifact、精英突变、隐藏内容成立 |
| `P4-C` | 数据包扩展点 | registry overlay、content pack、headless harness 成型 |

### 9.4 工作包切分

| ID | 所属检查点 | 目标 | 主要模块 | 依赖 | 完成定义 |
| --- | --- | --- | --- | --- | --- |
| `P4-W1` | `P4-A` | 混合拓扑地图：房间、环路、pattern room、vault、biome family | `core`, `game` | `Phase 3` | 多 seed 下差异明显但仍满足可达性与读图清晰 |
| `P4-W2` | `P4-A` | 锁钥匙、任务拓扑、隐藏入口和可解性验证 | `core`, `game`, `tools` | `P4-W1` | 不出现死局；隐藏内容有清晰发现逻辑 |
| `P4-W3` | `P4-B` | Loot 生态 V2：`iLvl/qLvl/rarity budget`、affix 预算、unique/artifact | `core`, `game` | `Phase 3` | 掉落质量与阶段匹配，不出现明显破局或废词条泛滥 |
| `P4-W4` | `P4-B` | 精英突变、Boss 变体、隐藏事件、探索奖励层 | `core`, `game`, `client` | `P4-W2`, `P4-W3` | 重复 run 中遭遇差异清晰，但规则仍可解释 |
| `P4-W5` | `P4-C` | 数据包 Overlay、内容包装载、schema lint、headless content harness、示例 content pack | `game`, `tools` | `P4-W3` | 第三方内容包可在不改 `core` 的前提下被装载与验证 |

### 9.5 最小可发布集

| 类别 | 最低交付 |
| --- | --- |
| biome family | 4 套 |
| pattern / vault | 12 个左右 |
| 锁钥匙/可解性场景 | 6 套左右 |
| affix / unique / artifact | 120 条左右总量 |
| elite mutation package | 12 套左右 |
| hidden event / secret zone hook | 8 套左右 |
| 示例 content pack | 1 个 |

### 9.6 Phase 4 出口标准

1. 多次 run 的地图、掉落和精英遭遇差异达到“明显可感知”。
2. lock-key、隐藏区域和探索奖励都有自动化可解性验证。
3. Loot 生态进入可扩展阶段，但仍保持可解释的预算模型。
4. 数据包 Overlay 能让新增内容走 schema + lint + harness，而不引入脚本 runtime。

---

## 10. Phase 5 — 战术 AI、稳定性、QA 与发布收口（v1.0.0）

### 10.1 目标

在已有规则和内容足够稳定的前提下，集中处理战术 AI、感知系统、性能、回放与发布门禁，产出真正能发布的 `v1.0.0`。

### 10.2 非目标

1. 不再引入大的新玩法系统。
2. 不做完整脚本平台。
3. 不为追求内容量而牺牲回归定位能力。

### 10.3 检查点

| 检查点 | 目标 | 结果要求 |
| --- | --- | --- |
| `P5-A` | 战术 AI 与感知 | 精英/Boss 决策明显提升，潜行/仇恨/感知自洽 |
| `P5-B` | 性能、回放、QA | 长局稳定，关键路径有 perf/soak，死因与 replay 可追溯 |
| `P5-C` | 发布与封板 | 安装包、文档、已知问题、平衡基线、发布门禁全部完成 |

### 10.4 工作包切分

| ID | 所属检查点 | 目标 | 主要模块 | 依赖 | 完成定义 |
| --- | --- | --- | --- | --- | --- |
| `P5-W1` | `P5-A` | 战术 AI 评分层与执行节点：面向精英/Boss，而不是做全功能通用平台 | `core` | `Phase 4` | 精英/Boss 会做更合理的走位、技能时机和目标选择 |
| `P5-W2` | `P5-A` | 感知、仇恨焦点、潜行、战术 telegraph polish | `core`, `client` | `P5-W1` | AI 基于最后已知信息行动，不依赖作弊式全图透视 |
| `P5-W3` | `P5-B` | FOV/渲染/音频/资源装载性能收口，建立 perf smoke 与 soak 基线 | `client`, `tools` | `Phase 4` | 性能预算可测，长局不出现明显句柄泄漏和内存异常 |
| `P5-W4` | `P5-B` | Run history、死因分析、本地 replay、Localization QA、Accessibility QA | `core`, `client`, `tools` | `P5-W1`, `P5-W2` | 死亡可解释、关键 run 可回放、语言与可读性完成专项清盘 |
| `P5-W5` | `P5-C` | 打包、安装验证、BalanceLab、已知问题清单、发布文档与 Gold Master 封板 | `client`, `tools`, `docs` | `P5-W4` | `v1.0.0` 门禁全部满足，进入发布态 |

### 10.5 Phase 5 出口标准

1. 精英/Boss 的战术行为达到可感知提升，且可通过固定场景回归。
2. 8~10 小时 run 的稳定性通过 soak。
3. 性能、死因、回放、双语言、可访问性都有固定验证入口。
4. 安装包、已知问题、操作说明、验证说明齐全，可发布 `v1.0.0`。

---

## 11. 长期回归矩阵

Phase 2 起，以下矩阵长期保留，后续阶段只能扩展，不能删除：

| 回归矩阵 | 核心问题 |
| --- | --- |
| `GoldenSeed` | 同一输入下，事件、snapshot、战斗结果是否稳定 |
| `Locale` | key 完整性、占位符一致性、UI 截图是否稳定 |
| `ContractLint` | schema、manifest、content key 是否完整 |
| `SoloClearLab` | 职业是否仍满足单通合同 |
| `HeadlessSmoke` | save/load、snapshot、地图、任务、最小 client 闭环是否还稳定 |
| `BossHarness` | telegraph、AI、阶段切换是否仍可解释 |
| `Perf/Soak` | 长局性能、句柄、内存、atlas/audio 是否稳定 |
| `SaveCurrentVersion` | 当前阶段主线格式是否可靠存读 |

---

## 12. Post-v1 Backlog

以下内容不进入 `Phase 2 ~ Phase 5` 主路径，但可以作为 `v1.0.0` 之后的扩展方向：

1. Lua runtime
2. 完整 Mod SDK
3. 更强的脚本宿主与 sandbox
4. 更激进的 AI 编辑器化工具
5. 更复杂的内容市场与发布分发机制

原因很简单：它们会显著扩大 API 冻结、版本治理和测试矩阵，不适合绑在 v1 主路径。

---

## 13. 一句话原则

后续开发的主判断标准不是“还能再加多少系统”，而是：

`这个工作包是否让主线更可玩、更可证、更容易被 AI 分阶段实现与验证。`

如果答案是否定的，就不应该进入当前阶段主路径。

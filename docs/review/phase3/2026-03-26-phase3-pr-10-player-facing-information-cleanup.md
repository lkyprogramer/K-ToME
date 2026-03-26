> 执行前必须先完整阅读并接受：
> `docs/review/phase3/2026-03-26-phase3-follow-up-pr-07-objective-runtime-and-gate-hardening.md`
> `docs/review/phase3/2026-03-26-phase3-pr-08-reward-milestone-affixization.md`
> `docs/review/phase3/2026-03-26-phase3-pr-09-content-floor-completion.md`
> `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
> `docs/phase3/2026-03-13-phase3-pr-06-long-run-world-structure.md`

# Phase 3 - PR-10 Player-Facing Information Cleanup

**阶段**: `Phase 3 / follow-up`  
**优先级**: `P1`  
**前置条件**: `PR-07/08/09` 完成（世界结构、奖励语义、内容底线已稳定）  
**对应问题**: 当前 world map、player creation、outcome summary 仍直接暴露 `levelBandRef / rescueTags / locked stub / zonePath ids / headlessTurnEquivalent` 等内部语义。Phase 3 的系统复杂度已经不低，如果玩家信息面还直接泄露内部 token，会持续抬高理解成本。

**Lane-parallel 拆分**：

- **W10a (Game/Client Contract)**: player-facing snapshot 字段、人类可读 label contract、outcome summary 数据模型收口
- **W10b (Client Lane)**: world map、player creation、victory/game over 页面改造 + i18n
- **W10c (QA Lane)**: golden screenshot、client smoke、manual white-box 更新

---

## 1. 阶段目标

把 Phase 3 当前直接暴露给玩家的内部 token 收口成可理解、可本地化、可维护的玩家语言。

完成标准：

1. world map / route preview 不再直接显示 `levelBandRef`。
2. world map / route preview 不再直接显示 `MOVEMENT / CLEANSING / PROTECTION / RECOVERY` 这类内部 rescue tag。
3. player creation 默认浏览路径不再把 frozen / dev-only 项和正式可玩项混在同一轮播层级。
4. outcome summary 不再直接展示 raw `zonePath id` 与纯技术向指标。
5. failure / victory recap 要能说明：
   - 打到了哪一段
   - 击败了哪些关键 Boss
   - 拿到了哪些关键路线奖励
   - 最后是因为什么崩掉
6. 所有变化都必须通过 i18n key 表达，不允许在 client 层硬编码第二套术语。

## 2. 当前问题

1. `TileRenderModel` / `AsciiRenderModel` 当前仍直接输出 `option.levelBandRef`。
2. 同一条 route preview 当前仍直接拼接 `option.rescueTags` 原始枚举值。
3. `MainMenuController / MainMenuScreen` 当前职业/种族轮播仍基于完整 options 列表，`UNLOCKED_BUT_UNAVAILABLE` 与 frozen stub 会干扰主路径。
4. `OutcomeSummaryPresenter` 当前 `route_path` 仍直接输出 `summary.zonePath.joinToString(" -> ")`，这实际是 zone id，不是玩家语言。
5. outcome summary 当前保留了 `headlessTurnEquivalent` 这类对玩家几乎无解释价值的技术指标。
6. outcome summary 还没有把关键 Boss 击杀进度、关键路线奖励与失败概括组织成真正面向玩家的信息层。

### 2.1 本 PR 必须冻结的口径

1. player-facing UI 不得直接显示以下内部 token：
   - `levelBandRef`
   - `rescueTags`
   - `zone id`
   - `zoneRouteHash`
   - `buildHash`
   - `ClassUnlockState`
2. `ClassPlayabilityState` 也不应原样显示给玩家，而要映射到稳定文案。
3. `UNLOCKED_BUT_UNAVAILABLE` 可以有提示，但不能与默认可玩选择混为同一浏览层。
4. `OutcomeSummary` 面向玩家的正文不得默认显示 `headlessTurnEquivalent`。
5. 本 PR 不改变正式 runtime 合同，只改变 player-facing 表达层与必要的 snapshot label 字段。
6. 所有新文案必须进入 i18n，不允许硬编码中文或英文句子进 renderer。

## 3. 范围与非目标

### 3.1 范围

1. world map / route preview 的信息重写。
2. player creation / main menu 的可玩选项收口。
3. victory / game over / outcome summary 的信息重写。
4. 必要的 snapshot label / presenter contract 调整。
5. i18n / golden screenshot / client smoke 更新。

### 3.2 非目标

1. 不做新的 UI 视觉风格大改。
2. 不做完整 codex / journal / encyclopedia 系统。
3. 不做 profile history 的全新浏览器。
4. 不做新的 unlock 规则，只重写呈现方式。
5. 不在本 PR 讨论 reward affix 化或内容补量逻辑。
6. 不新增 BGM、角色立绘、zone 插画或 reward 原画；若需要图标化提示，优先复用现有 icon atlas、manifest alias 与文字 label。

## 4. 技术方案

### 4.1 [W10a] World Map / Route Preview Player Labels

建议文件：

```text
game/src/main/kotlin/com/ktome/game/GameView.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt
client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt
```

冻结口径：

1. route option snapshot 不再直接把 `levelBandRef` 作为 player-facing 文本传给 client。
2. game 层应提供可本地化 label key 或结构化字段，例如：
   - `recommendedLevelMin`
   - `recommendedLevelMax`
   - `rescueHintLabelKeys`
3. client 只负责把这些结构化字段渲染为玩家语言：
   - `推荐等级 3-5`
   - `路线特性：位移 / 净化`
4. 若 route 具有 `uniqueContentTag / specialMechanics`，这里允许补一条轻量 mechanic hint。

### 4.2 [W10b] Player Creation 收口

建议文件：

```text
client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt
client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt
game/src/main/kotlin/com/ktome/game/GameModule.kt
```

冻结口径：

1. 默认轮播集合只包含 `PLAYABLE` 项。
2. `UNLOCKED_BUT_UNAVAILABLE` 项不进入默认轮播，但允许在侧边提示区显示“已发现，尚未可用”。
3. frozen stub 不在默认 player creation 主路径展示。
4. 若需要开发态白盒入口，必须显式 gated，不得污染正式主菜单路径。

### 4.3 [W10a] Outcome Summary 数据模型

建议文件：

```text
game/src/main/kotlin/com/ktome/game/GameView.kt
game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
client/src/main/kotlin/com/ktome/client/screen/OutcomeSummaryPresenter.kt
```

冻结口径：

1. `OutcomeSummary` 继续与 `core.profile.RunSummary` 分层，不回退成双真源。
2. 但需要增加 player-facing 友好字段，例如：
   - `zonePathNameKeys`
   - `defeatedBossNameKeys`
   - `claimedRouteRewardNameKeys`
   - `failureSummaryKey`
3. `OutcomeSummaryPresenter` 默认不显示 `headlessTurnEquivalent`。
4. 失败页需要优先回答：
   - 你死在什么阶段
   - 被谁击杀
   - 最后关键事件是什么
   - 这局已经达成了哪些里程碑
5. 胜利页需要优先回答：
   - 清到哪条路线
   - 击败了哪些关键 Boss
   - 关键奖励和构筑方向是什么

### 4.4 [W10b] 文案与本地化策略

建议文件：

```text
game/src/main/resources/i18n/en-US.json
game/src/main/resources/i18n/zh-CN.json
client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt
```

冻结口径：

1. 新文案全部进入 i18n key。
2. 不允许在 renderer 中拼接出一套新的不可本地化术语。
3. `selectionState` 文案必须改成玩家语言，不暴露内部 availability 名字。
4. 如果新增 outcome summary section，golden screenshot 必须同步更新。

## 5. 推荐改动面

### 5.1 `game`

1. route option snapshot 字段
2. `OutcomeSummary` player-facing label 字段
3. 必要的本地化 key 装配

### 5.2 `client`

1. `TileRenderModel.kt`
2. `AsciiRenderModel.kt`
3. `MainMenuController.kt`
4. `MainMenuScreen.kt`
5. `OutcomeSummaryPresenter.kt`
6. `VictoryScreen.kt`
7. `GameOverScreen.kt`

### 5.3 `tools / QA`

1. `GoldenScreenshotHarnessTest`
2. `ClientSmokeHarnessTest`
3. main menu / outcome summary presenter tests

## 6. 测试与自证

### 6.1 必测类

1. `OutcomeSummaryPresenterTest`
2. `GameAppLifecycleTest`
3. `GoldenScreenshotHarnessTest`
4. `ClientSmokeHarnessTest`

### 6.2 必测行为

1. world map 不再显示 `lv3_5` 这类内部 token。
2. world map 不再显示原始 rescue tag 枚举值。
3. player creation 默认轮播只包含当前可玩项。
4. frozen / dev-only 项不会干扰正式主路径。
5. outcome summary 不再显示 raw zone id route path。
6. outcome summary 默认不再显示纯技术指标。

### 6.3 自动化命令

```bash
./gradlew :client:test --tests "com.ktome.client.screen.OutcomeSummaryPresenterTest"
./gradlew :client:test --tests "com.ktome.client.GameAppLifecycleTest"
./gradlew :client:goldenScreenshot --tests "*GoldenScreenshotHarnessTest"
./gradlew :client:clientSmoke --tests "*ClientSmokeHarnessTest"
./gradlew check
```

### 6.4 白盒验证

1. 打开 world map，确认 route preview 是玩家语言而不是内部 token。
2. 进入 player creation，确认默认浏览路径只有当前可玩职业/种族。
3. 触发一次 victory 和一次 defeat，确认结算页给出人类可读的路线、Boss 与失败概括。

## 7. 出口门禁

1. world map / route preview 已去内部 token 化。
2. player creation 默认路径已收口到当前可玩项。
3. outcome summary 已去 raw route id 与纯技术指标。
4. i18n / golden screenshot / client smoke 全部同步通过。
5. `./gradlew check` 保持绿色。

## 8. 风险与止损

### 8.1 风险

1. snapshot 字段调整会牵动 client test / golden 大量更新。
2. 如果直接隐藏 unavailable/frozen 项，可能影响现有白盒/开发态流程。
3. 如果 outcome summary 一次塞太多信息，会从“技术 token”变成“信息噪音”。

### 8.2 止损

1. 先收口最明显的内部 token，不追求一次重做整套信息架构。
2. 对 dev-only/frozen 项保留显式白盒入口，但与正式主路径分离。
3. outcome summary 优先显示里程碑与失败概括，不做过长战报。

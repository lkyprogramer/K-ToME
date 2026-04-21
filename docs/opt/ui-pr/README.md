# Phase 4 UI/UX PR 级开发文档索引

本目录是 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md` 的 PR 级执行落点。

这里不机械沿用源计划的 8 个执行包，而是按实际开发耦合与验证成本合并为 5 个 PR：

| PR | 文档 | 合并来源 | 合并理由 |
| --- | --- | --- | --- |
| `phase4-uiux-pr01` | [Client Foundation and Main Menu](2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md) | 原 `PR-01 + PR-02` | 首页首屏直接消费 token、标题规则和帮助区，拆开会产生重复 golden 与文案回写 |
| `phase4-uiux-pr02` | [In-Game Info, Input, Modal, Look](2026-04-21-phase4-uiux-pr02-ingame-info-input-modal-look.md) | 原 `PR-03` | 输入语义、modal 栈和 Look Mode 是高风险 owner，必须独立 |
| `phase4-uiux-pr03` | [Item, Content Presentation, UI States](2026-04-21-phase4-uiux-pr03-item-content-presentation-and-ui-states.md) | 原 `PR-04 + PR-05` | icon/quality、共享卡片、错误/空态/加载态共享 content-ui lint 与资源管线 |
| `phase4-uiux-pr04` | [Status, Description, Readability](2026-04-21-phase4-uiux-pr04-status-description-and-readability.md) | 原 `PR-06 + PR-07` | 状态 badge、关键词、动态说明和 explain pane 都服务于同一套可读性/解释入口 |
| `phase4-uiux-pr05` | [Telegraph and Combat Decision Surface](2026-04-21-phase4-uiux-pr05-telegraph-and-combat-decision-surface.md) | 原 `PR-08` | 战斗三层决策面与 telegraph 三位一体风险高，保留独立门禁 |

上游源计划修订状态：

1. 源计划旧 PR 编号中涉及 `BuildInfo.shortHash` 的描述已前移到 `phase4-uiux-pr01`；后续错误态/共享卡片只能消费该来源。
2. 源计划旧 `specialTemplateId != null` 的 client-local accent 口径已修订为 `phase4-uiux-pr03` 正式扩 `ItemRenderSnapshot.specialTierId`；client 不允许反查 content template 推断 special tier。
3. 源计划旧资源文件名前缀已重映射为当前 5 PR 前缀：旧 `PR-04 + PR-05` 使用 `phase4-uiux-pr03-*`，旧 `PR-06 + PR-07` 使用 `phase4-uiux-pr04-*`，旧 `PR-08` 使用 `phase4-uiux-pr05-*`。执行时以每份 PR 的 §7 资源计划为准。

执行纪律：

1. 仍按 `PR-01 -> PR-05` 串行推进。
2. 每个 PR 的自动化、golden、人工白盒记录完成前，不进入下一 PR。
3. 人工白盒不是可选补充；UI/视觉类改动不能只靠 skipped golden 或编译测试签收。
4. 如果某个 PR 的实际实现继续膨胀，允许在开发前从该 PR 文档再拆一个 follow-up，但不得绕过当前文档定义的出口门禁。
5. 若 golden 因 LWJGL backend unavailable 或环境问题被 skipped，必须保留人工截图、输入记录或 smoke artifact；skipped golden 不能替代人工白盒。
6. 若任一 PR 新增/删除/移动的 Kotlin 文件数 `>= 5`，或修改公共 `sealed` / `interface` / 跨模块 DTO 类型 `>= 2`，必须补跑 `./gradlew maintainabilityLint`。
7. `ValidationPreset.*` 依赖只锚定 `com.ktome.game.validation.ValidationPreset` 枚举合同；源文件路径可变，不作为长期文档真源。
8. 每个 PR 完成本地实现与人工白盒后，必须先跑一次 `ktome-diff-doc-review` 和 `ktome-code-review`，解决发现后才进入下一 PR。
9. 全部 PR 完成后，再跑 `ktome-diff-doc-review -> ktome-code-review -> simplify-code-review-cleanup` 总收口；发现的问题进入对应 PR 或 follow-up，不能静默留空。
10. 每个 PR 的 focused tests 只作为开发内循环；提交或开 PR 前必须运行 root 出口 gate：`./gradlew clientSmoke goldenScreenshot verifyChanged`。若某项因环境 skipped，必须在人工记录中补等价证据和原因。

## 每 PR 必跑命令总览

| 阶段 | 命令/动作 | 说明 |
| --- | --- | --- |
| 开发内循环 | 各 PR §6.2 的 focused test selector 与 lint | 用于快速定位当前 PR 直接行为 |
| UI root gate | `./gradlew clientSmoke goldenScreenshot` | 使用 root alias，保证 smoke/golden report 与 CI 路由一致 |
| PR-close gate | `./gradlew verifyChanged` | 使用 impact routing 兜底变更面，不得被 focused tests 替代 |
| 资源条件 gate | `./gradlew assetLint styleLint manifestLint audioLint` | 只要 PR 触发 image/audio/manifest plan 或 `--extra-plan` 接线就必须跑 |
| 构建接线 gate | `./scripts/verify-bootstrap.sh` | 只要修改 Gradle、processResources、bootstrap 或依赖版本就必须跑 |
| review gate | `ktome-diff-doc-review`、`ktome-code-review` | 每个 PR 完成后执行；全部 PR 完成后追加 `simplify-code-review-cleanup` |

## 跨 PR Deferred 与收口

| 条目 | 首次引入 PR | 收口 PR | 收口前临时形态 | 验收方式 | 清理要求 |
| --- | --- | --- | --- | --- | --- |
| `ITEM_COMPARE` frame | `PR-02` | `PR-03` | 占用 `ModalStack` 深度的 no-op frame；render 返回空视图；除 `ESC / Backspace` 外不消费命令 | `ModalStackTest` 断言 push、深度、pop；`InputHandlerTest` 断言业务键 no-op | 若曾录 golden，只允许 `phase4-uiux-pr02-item-compare-stub-*` 前缀；`PR-03` 收口时删除或重录 |
| `COMBAT_DECISION` frame | `PR-02` | `PR-05` | 占用 `ModalStack` 深度的 no-op frame；`Ctrl+S` 先保留 blocked stub；不暴露玩家可见三层面 | `InputHandlerTest` 覆盖栈深度和 `Ctrl+S` blocked stub；`PR-05` 补真实 phase 机与 toast 文案 | 若曾录 golden，只允许 `phase4-uiux-pr02-combat-decision-stub-*` 前缀；`PR-05` 收口时删除或重录 |
| `ExplainPane` sub-view | `PR-02` | `PR-04` | `INSPECT + ?` 只预留 sub-view 入口；未打开时 `Backspace` 仍按 `PR-02` 回退 | `PR-04` 补 `ExplainPane` 后同步更新 `InputHandlerTest`：ExplainPane 打开时 `Backspace` 先关闭 sub-view | 不新增 `UiMode.EXPLAIN`，不留下 standalone explain frame |
| `BuildInfo.shortHash` | `PR-01` | `PR-01` | 不允许跨 PR 占位；PR-01 直接落 `BuildInfo.shortHash` 与 formatter，注入失败回退 `unknown` 并 warn | `BuildInfoTest` / smoke 断言 hash 字段存在；失败回退路径不写死为唯一期望 | 后续 PR 只能消费，不得新增第二 hash reader |
| `UiEmptyState` 统一模型 | `PR-02` | `PR-03` | `PR-02` 可临时用 `RenderTextTokenSnapshot("ui.inspect.empty.tile")` 表达 Look Mode 空地 | `PR-03` 迁移到 `UiEmptyState`，并在 empty-state smoke/golden 中覆盖 | `PR-03` 合入时删除 `ui.inspect.empty.tile` 的 `zh-CN/en-US` 条目，并把它列入 locale deprecated-key 断言 |

## Golden Label 所有权

| PR | label 前缀 | 所属场景 | 迁移规则 |
| --- | --- | --- | --- |
| `PR-01` | `phase4-uiux-pr01-*` | 首页三态、token、MapDominant、窗口标题相关截图/hash | 不承载 modal/look/combat 场景 |
| `PR-02` | `phase4-uiux-pr02-*` | modal stack、Look Mode、force-switch、validation input | deferred stub 必须使用 `phase4-uiux-pr02-item-compare-stub-*` 或 `phase4-uiux-pr02-combat-decision-stub-*`，收口 PR 按前缀删除或重录 |
| `PR-03` | `phase4-uiux-pr03-*` | item icon、quality、ground loot、shared card、empty/error/loading | 不承载 status/explain/combat 场景 |
| `PR-04` | `phase4-uiux-pr04-*` | status badge、telegraph compact、ExplainPane、keyword description | telegraph 完整三位一体仍归 `PR-05` |
| `PR-05` | `phase4-uiux-pr05-*` | telegraph triple-surface、combat decision ACTION/METHOD/TARGET | 上线时清理任何 `PR-02` 遗留 combat-decision-stub label |

## 人工白盒记录

所有人工白盒记录统一按 [manual-records/_template.md](manual-records/_template.md) 填写。每个 PR 可追加自己的检查表，但不得删掉模板里的 `环境 / 输入序列 / 预期行为 / 实际行为 / 证据路径 / 签收结论` 六类信息。

人工白盒记录文件必须随对应 PR 一并提交到 `docs/opt/ui-pr/manual-records/phase4-uiux-prNN-*.md`。截图、录屏、日志、copied payload、golden hash 或 smoke artifact 必须给出 repo 内路径、artifact 路径或可追溯说明；若 golden 被 skipped，记录中必须额外写明 `HEAD sha / locale / seed / 窗口尺寸 / 截图或录屏证据`。

失败时必须保留输入序列、截图或录屏、golden hash 或 smoke artifact、日志来源和复现步骤。禁止只写“未通过”或“目测异常”作为失败记录。

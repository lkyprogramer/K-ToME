# Phase4 v4 PR-00 实现审查报告

- **审查范围**：`codex/phase4-v4-pr00-fast-whitebox-validation` 分支当前已落地的 PR-00 改造
- **对照规范**：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
- **审查视角**：资深 Roguelike / 类 ToME 设计总监 + 系统策划总监 + 玩法体验审查负责人
- **审查日期**：2026-04-26
- **总体结论**：**契约层 9.5/10，可合入；建议补 1 个只读断言用例 + 1 段实现注记后正式合并**

---

## 一、执行摘要

整体实现与规范高度一致，**核心契约层全部落地**：

- typed `ValidationScenarioRegistry` + YAML id-parity 双向校验
- 5 个 JVM 系统属性合并进单一 `JAVA_TOOL_OPTIONS`，并通过 packaged macOS app 启动
- SHA-256 应用哈希校验失败即以 `APP_HASH_MISMATCH` 退出码 20 终止
- 仓库相对路径纪律（`/Users/`、`/tmp/` 反向断言）
- 单进程 `DIRECT_VALIDATION_SESSION` 入口，profile 持久化 `NO_OP`，VALIDATION 模式禁用 Ctrl+S
- `PHASE4_V4_FAST` 覆盖层仅在 `scenarioId != null` 时显示 4 个幂等动作
- `Phase4V4ScenarioAction` 通过 `FoundationGameSession.perform(PlayerCommand.Validation(...))` 单一入口派发
- CUA Runbook 9 节、`expected-evidence.json`、`manual-record` 模板、`app-executable.sha256` 五件套同步生成
- 失败留存语义贯穿 launch script + runbook §9

**关键偏差（结构层）**：场景元数据被切分到 3 个 catalog（game `ValidationScenarioRegistry` + client `ValidationScenarioPresentationCatalog` + tools `Phase4V4WhiteboxScenarioMaterializationCatalog`），与规范"单一扁平 `ValidationScenarioDef`"的措辞不完全对齐。这是主动的模块分层选择，已通过双向 parity 测试约束，**契约层影响低，结构层影响中**。

**次要扩展**（规范允许范围内的合理增强）：

- 新增 `ktome.whitebox.appHash` JVM 属性（仍归并到 `JAVA_TOOL_OPTIONS`，不破坏五属性合一语义）
- 新增 `MISSING_PHASE4_V4_SCENARIO_PRESENTATION` / `INVALID_PHASE4_V4_SCENARIO_STARTUP_MODE` 错误码
- `evidence/app.log` 进入 `requiredEvidenceFiles` 列表
- 错误模态新增 `validation.phase4.v4.error.copy_detail` 复制按钮

**测试缺口**：规范 §6.1 #11（"`show-evidence-summary` 不改变玩家位置/HP/资源"）未直接断言。建议补一个 < 30 行的只读用例。

---

## 二、规范分节对齐矩阵

| 规范 § | 主题 | 实现状态 | 偏差等级 | 主要落地点 |
|---|---|---|---|---|
| §1 | 背景与目标 | ✓ 完全对齐 | 无 | — |
| §2 | 名词与契约 | ✓ 完全对齐 | 无 | — |
| §3.1 | 场景注册 + YAML parity | ✓ 实现，但字段切分到 3 catalog | 结构=中 / 契约=低 | `ValidationScenarioRegistry.kt`、`ValidationScenarioPresentationCatalog.kt`、`Phase4V4WhiteboxScenarioMaterializationCatalog.kt` |
| §3.2 | 5 个固定 JVM 属性 | ✓ + 合理新增 `appHash` | 低（扩展） | `Phase4V4WhiteboxScenarioCli.kt`、`ValidationScenarioBootstrap.kt` |
| §3.3 | 应用哈希 SHA-256 校验 | ✓ `APP_HASH_MISMATCH`，退出码 20 | 无 | `Phase4V4WhiteboxScenarioCli.kt` launch-script 段 |
| §3.4 | 仓库相对路径纪律 | ✓ test 反向断言 `/Users/`、`/tmp/` | 无 | `Phase4V4WhiteboxScenarioCliTest.kt#assertFalseMachinePath` |
| §4.1 | `DIRECT_VALIDATION_SESSION` 入口 | ✓ 单 dispatch | 无 | `GameApp.create()` 路由 → `startValidationSession()` |
| §4.2 | NO_OP 持久化 / saveOnExit=false | ✓ | 无 | `FoundationGameSession.saveOnExit()`、`ValidationSessionOptions.profileRunPersistenceMode` |
| §4.3 | Ctrl+S VALIDATION 模式禁用 | ✓ | 无 | `InputHandler.kt:1420` → `ui.message.save.blocked-in-validation` |
| §4.4 | `PHASE4_V4_FAST` 仅 scenarioId 存在时显示 | ✓ scope-keyed cache | 无 | `ValidationCommandSource.kt` `ValidationOverlayDescriptorPlanCache` |
| §4.5 | 4 个幂等动作 + `scenario_mismatch` | ✓ | 无 | `FoundationGameSession.executePhase4V4ScenarioAction()` |
| §4.6 | `RESET_SCENARIO` → 入口期 + `queueValidationRestart` | ✓ | 无 | 同上 |
| §4.7 | 错误统一通过 `ValidationAction` family 派发 | ✓ | 无 | `ValidationAction.Phase4V4ScenarioAction` + family `PHASE4_V4_FAST` |
| §5.1.1 | 启动失败 fail-fast 模态 + copy-detail | ✓ + 错误码细分 | 低（扩展） | `validationScenarioErrorState()` + `UiErrorScreen` |
| §5.1.2 | i18n 引导文案 scenario-aware | ✓ overload 路由 | 无 | `ValidationPhase4Guide.validationPhase4Guide(summary)` |
| §5.1.3 | 证据摘要侧边栏 9 行 | ✓ | 无 | `ValidationScenarioEvidenceSummaryLines.kt` |
| §6 | 测试覆盖 12 项 | 11/12 直接断言 | 低 | 见下文 §3.3 |
| §7 | CUA Runbook 9 节 + sidecar | ✓ | 无 | `Phase4V4WhiteboxScenarioCli.kt` runbook 模板 |
| §8 | 失败留存（不清理） | ✓ runbook §9 明示 | 无 | runbook §9 + launch script 不做清理 |
| §9 | PR-00 作为 PR-01..PR-07 契约层 | ✓ 字段、错误码、key 命名就位 | 无 | 见下文 §4 |

---

## 三、详细发现

### 3.1 结构性偏差：场景元数据三层切分

#### 现状

| Catalog | 模块 | 持有字段 |
|---|---|---|
| `ValidationScenarioRegistry` | `game` | runtime（preset / seed / locale / professionId / raceId / zoneId / floor / routeIndex / contentPackMode）+ evidence（requiredEvidenceFiles / cuaSteps / manualRecordPath） |
| `ValidationScenarioPresentationCatalog` | `client` | titleKey、startupMode、initialOverlaySection |
| `Phase4V4WhiteboxScenarioMaterializationCatalog` | `tools` | windowWidth、windowHeight |

每个 catalog 都暴露 `validateRegistryParity()`，缺一即测试红线。

#### 对照规范

规范 §3.1 描述的是单一扁平 `ValidationScenarioDef`，包含 titleKey / windowWidth / windowHeight / startupMode / initialOverlaySection 等字段。当前实现按"模块归属"切分。

#### 评估

- **契约层影响 = 低**：
  - `Phase4V4WhiteboxScenarioCliTest#whitebox materialization catalog stays in parity with scenario registry` 直接断言 tools↔game parity
  - `ValidationCommandSourceTest` 间接断言 client presentation parity
  - 任何 PR 新增场景必须 3 处同步，否则 CI 红
- **结构层影响 = 中**：
  - 违背规范"单一可信源"措辞
  - 新人需要理解 3 个 catalog 的职责切分
  - 多场景叠加后维护成本随场景数线性增长（每新增 1 场景需在 3 处补条目）
- **架构优点**：
  - `game` 模块不感知 LibGDX 视窗参数 → 单元测试不需要拉起图形栈
  - `client` 模块不依赖 `tools` 资产路径 → 客户端发布产物不带物化脚手架
  - 这是合理的"按编译期依赖切边界"决策

#### 修复优化建议（按代价升序）

1. **【P1，推荐】最小修复**：在规范文档 `2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md` 末尾追加一段"实现注记 / Implementation Notes"，承认三层 catalog 切分基于模块边界，并以 parity 校验作为契约保证。**文档对齐代码，无代码改动**。
2. **【P2】中等成本**：将 `Phase4V4WhiteboxScenarioMaterializationCatalog` 从 `tools` 模块迁移到 `client`（窗口尺寸本质属于 client 启动期决策），与 `ValidationScenarioPresentationCatalog` 合并为一个 client-side 文件。`tools` 通过反射或 ServiceLoader 在生成 launch script 时只读 client 资源。**收益**：消除 tools 持有 UI 尺寸的反向依赖；catalog 数量从 3 降到 2。**风险**：tools↔client 反向依赖需仔细评估。
3. **【P3，不推荐】高成本**：把所有字段塞回 game `ValidationScenarioDef`。会让 game 模块直接感知 LibGDX 视窗概念，破坏现有清晰分层，与"Slow is Fast"长期演进相悖。

→ **强烈推荐采纳方案 1（仅文档更新）**，方案 2 留待 PR-08 或后续清理 PR 评估。

---

### 3.2 实现扩展项盘点

| 扩展点 | 影响范围 | 评估 |
|---|---|---|
| `ktome.whitebox.appHash` JVM 属性 | launch script 计算出 SHA-256 后注入进程，进程内可只读校验自证 | **合理增强**。属性仍在 `JAVA_TOOL_OPTIONS`，未破坏"五属性合一"。建议规范 §3.2 补注 |
| `MISSING_PHASE4_V4_SCENARIO_PRESENTATION` 错误码 | 由 §3.1 三层 catalog parity 缺失派生 | 是结构选择派生的契约，**应同步进规范 §5.1.1** |
| `INVALID_PHASE4_V4_SCENARIO_STARTUP_MODE` 错误码 | 防御非 `DIRECT_VALIDATION_SESSION` 启动尝试 | 与规范 §4.1 完全一致，仅是"显式表达"。建议规范 §5.1.1 补注 |
| `evidence/app.log` 入 `requiredEvidenceFiles` | 失败诊断完整性 | **合理**。规范 §3.1 evidence 列表是开放下界 |
| `validation.phase4.v4.error.copy_detail` 复制按钮 | UX 增强：一键复制 startupProperties + scenarioId + errorCode | 直接对应规范 §5.1.1 "可复制粘贴的纯文本"。**优秀** |
| `ValidationCapabilitySet.phase4V4Fast` 默认 true | family-level capability gating | 为后续 PR 灰度提供开关位。**前瞻性合理** |

---

### 3.3 测试缺口与补全建议

#### 缺口：规范 §6.1 #11

> "`show-evidence-summary` 动作不改变玩家位置/HP/资源。"

**当前覆盖状态**：

- `ValidationScenarioRegistryTest` 已断言：派发成功、4 动作幂等、`scenario_mismatch` 拒绝、evidence summary visibility、route index sentinel、yaml parity、≥4 PNG 校验、reset 后 evidence summary 重新隐藏
- **未直接断言**：执行 `Phase4V4ScenarioAction(... SHOW_EVIDENCE_SUMMARY)` 前后玩家 `position` / `hp` / `resources` 数值不变

#### 推荐补丁（< 30 行）

```kotlin
// game/src/test/kotlin/com/ktome/game/FoundationGameSessionPhase4V4Test.kt 新增
@Test
fun `show evidence summary action does not mutate player core state`() {
    val session = newScenarioSession(scenarioId = "phase4-v4-pr00-selftest")
    val before = session.playerCoreFingerprint() // position + hp + resources
    val result = session.perform(
        PlayerCommand.Validation(
            ValidationAction.Phase4V4ScenarioAction(
                scenarioId = ValidationScenarioId("phase4-v4-pr00-selftest"),
                actionId = ValidationScenarioActionId.SHOW_EVIDENCE_SUMMARY,
            ),
        ),
    )
    val after = session.playerCoreFingerprint()

    assertEquals(before, after)
    assertTrue(session.validationSummarySnapshot().scenarioEvidenceSummaryOpen)
}
```

**价值**：直接锁住"覆盖层只读"不变量，防止未来误改 `executePhase4V4ScenarioAction` 把玩家状态写脏。代价低，**P1 应在合入前补**。

#### 次要建议

- `app hash mismatch` 当前只在 unit 级 placeholder 覆盖，可考虑加一条 `bash -n launch-packaged-app.sh` 语法校验 + 一条 mock SHA 不匹配的 shell-level 用例（**P3**，可选）。

---

### 3.4 其它细节观察

| 观察项 | 现状 | 评估 |
|---|---|---|
| 路径分隔符跨平台 | `ValidationScenarioBootstrap.portablePath()` 显式 `replace('\\', '/')` | ✓ 良好，即使当前只跑 macOS，对 Windows 开发分支也兼容 |
| `UnknownValidationScenarioException` message 含 startupProperties 全文 | client 侧 `validationScenarioErrorState` 已做受控展示 | 安全。`ktome.*` 属性名空间受控，无 secrets 风险 |
| `saveOnExit()` scenario-aware 关闭 | 一处判定（`scenarioId != null`）覆盖所有 scenario | **架构干净**。提醒 PR-01..PR-07 复用同一判定，不要散落 if 分支 |
| `ValidationOverlayDescriptorPlanCache` 按 scope 含 scenarioId 缓存 | scenario 切换时缓存自动失效 | ✓ 防止悬空 descriptor |
| `ValidationSessionOptions.scenarioRouteIndex = -1` sentinel | `routeIndex = -1` 表示"未指定路线" | 与 `MAPGEN_DIFF` preset 自动选路语义一致 |

---

## 四、对 PR-01..PR-07 的影响与建议

PR-00 作为后续 7 个 PR 的契约层，已为后续工作提供以下复用路径：

### 4.1 场景声明三件套

每新增 1 个场景需要：

1. `game/.../ValidationScenarioRegistry.kt` 内追加 `ValidationScenarioDef` 条目
2. `client/.../ValidationScenarioPresentationCatalog.kt` 追加 `ValidationScenarioPresentationSpec`
3. `tools/.../Phase4V4WhiteboxScenarioMaterializationCatalog.kt` 追加 window 尺寸
4. `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml` 追加 `- id: ...`

**建议**：在 `docs/review/phase4/v4-pr/` 下提供一份 `scenario-onboarding-checklist.md`，单页列出这 4 处 + i18n key + 必产 PNG 列表。

### 4.2 launch-script + runbook + manual-record + expected-evidence + app-sha256 五件套自动化

后续 PR 不再需要手写 shell 脚本或 runbook 模板，只需在 `Phase4V4WhiteboxScenarioCli` 的资产生成路径上扩展即可。**复用率高，PR-01..PR-07 单 PR 工作量预计低于 PR-00 的 30%**。

### 4.3 错误码命名空间

保持 `*_PHASE4_V4_*` 前缀模式。PR-01..PR-07 引入新错误码时，统一在 `ValidationScenarioBootstrapErrorCode` enum 扩展，**禁止散落到各 PR 的私有 enum**，否则 copy-detail 模态需要多套 i18n key。

### 4.4 i18n key 模板

已确立 `validation.phase4.v4.{scenarioId}.{slot}` 模板：

- `target` / `quick.prepare` / `quick.evidence`
- `evidence.bootstrap` / `evidence.primary` / `evidence.secondary` / `evidence.summary`

后续 PR 直接复用模板键空间，不需要再设计新 key 命名。

### 4.5 测试模板复用

PR-01..PR-07 应至少复制 PR-00 的测试套件：

- (a) yaml parity
- (b) presentation parity
- (c) materialization parity
- (d) bootstrap valid / unknown / missing / invalid-mode
- (e) overlay scope visibility（无 scenarioId 不显示）
- (f) action dispatch + idempotency
- (g) read-only assertion（即 §3.3 建议补的 §6.1 #11 用例）

---

## 五、总体打分与下一步动作

### 5.1 维度打分

| 维度 | 打分 | 备注 |
|---|---|---|
| 契约一致性 | 9.5/10 | 仅扣 0.5 给"三层 catalog 与规范文字描述错位" |
| 失败可观测性 | 9.0/10 | copy-detail UI + startupProperties 全量上报；扣 1 给"manual record 模板缺少自动校验链接" |
| 测试覆盖 | 8.5/10 | 扣 1 给 §6.1 #11；扣 0.5 给 "app hash mismatch shell-level e2e 未覆盖" |
| 模块分层 | 9.0/10 | game / client / tools 边界干净 |
| 可演进性 | 9.0/10 | PR-01..PR-07 复用路径清晰；扣 1 给"每新增场景需 3 处同步" |
| **加权总分** | **9.0/10** | **可合入** |

### 5.2 行动清单

#### P1（建议本 PR 合入前补）

1. **补 §6.1 #11 测试用例**（read-only assertion，预计 < 30 行，参见 §3.3 模板）
2. **在规范文档 `2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md` 末尾追加"实现注记"段落**，说明三层 catalog 切分及 parity 保证机制

#### P2（可放到下一个 PR 一起做）

3. 提供 `docs/review/phase4/v4-pr/scenario-onboarding-checklist.md`（一页式新增场景必改清单）
4. 评估是否将 `Phase4V4WhiteboxScenarioMaterializationCatalog` 从 `tools` 迁移到 `client`，与 `ValidationScenarioPresentationCatalog` 合并

#### P3（可忽略 / 视精力）

5. 给 launch-script 加 `set -euo pipefail` 校验（如尚未）
6. 增加 mock SHA 不匹配的 shell-level e2e 用例

---

## 六、结论

**实现质量整体优秀，契约层与规范高度一致，可以接受合并**。建议 P1 项目在合入前补完，P2 项目在 PR-01 启动前消化掉，P3 项目按精力调度即可。

PR-00 已为 PR-01..PR-07 提供了清晰的 scenario onboarding 模板、自动化资产生成路径、错误码命名空间与测试套件骨架。后续 7 个 PR 的工作量主要将在"场景内容定义"而非"基础设施搭建"，符合规范 §9 对 PR-00 作为契约层的预期定位。

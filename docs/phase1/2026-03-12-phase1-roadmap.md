# Phase 1 总体执行路线图（从可验证内核到可通关 MVP）

**日期**: 2026-03-12  
**状态**: Proposed  
**适用分支**: `main`  
**前置输入**:

1. `docs/mvp-development-guide.md`
2. `docs/wiggly-sprouting-platypus.md`
3. `docs/phase1/roadmap.md`

---

## 1. 直接结论

Phase 1 的核心任务不是“尽快把系统数量堆出来”，而是建立一条可持续、可验证、可迭代的 Roguelike 开发主线：

`零引擎依赖 core -> 可运行 client 壳层 -> 可自证战斗/成长循环 -> 5 层可通关 MVP -> 可发布 v0.1.0`

因此，Phase 1 必须固定按以下顺序执行：

1. `1-1.0` 引擎基础 + 可验证
2. `1-2.0` 战斗与 AI
3. `1-3.0` 物品与天赋
4. `1-4.0` 完整循环
5. `1-5.0` 打磨与发布

每个阶段都必须同时满足四类交付标准：

1. **可运行**：存在可演示的真实切片，而不是只有核心类。
2. **可测试**：所有新增核心逻辑都有确定性单元测试。
3. **可度量**：JaCoCo 覆盖率报告可以证明 `core` 的测试充分性。
4. **可白盒验证**：有固定 seed、固定操作步骤、固定预期结果，AI 或开发者都可以重复验证。

本路线图已拆分为逐阶段执行文档：

1. `docs/phase1/2026-03-12-phase1-1.0-foundation-and-verifiable-core.md`
2. `docs/phase1/2026-03-12-phase1-2.0-combat-and-ai.md`
3. `docs/phase1/2026-03-12-phase1-3.0-items-and-talents.md`
4. `docs/phase1/2026-03-12-phase1-4.0-full-game-loop.md`
5. `docs/phase1/2026-03-12-phase1-5.0-polish-and-release.md`

---

## 2. 当前基线（Phase 1 输入事实）

### 2.1 已经明确的技术基线

1. 项目语言与运行时基线已固定为：
   - Java `21`
   - Kotlin `2.2.21`
   - Gradle Wrapper `8.14.3`
   - libGDX `1.14.0`
2. 当前仓库已经具备：
   - 项目级 `.sdkmanrc`
   - 项目级 `Gradle Wrapper`
   - 根 `settings.gradle.kts` / `build.gradle.kts` 极简引导文件
3. 当前仓库仍未具备：
   - `core/`、`game/`、`client/` 真实模块
   - 任何核心逻辑实现
   - 任何测试代码
   - 任何运行时数据文件

### 2.2 已经冻结的架构原则

1. `core` 只允许纯 JVM 依赖，不允许引入 libGDX、Lua、GUI 或渲染库。
2. `game` 负责 YAML 数据、注册表和胶水逻辑。
3. `client` 只负责渲染、输入、音频和画面层 orchestration。
4. 公式、AI、寻路、地图生成、战斗、物品、存档等规则必须留在 `core`。
5. Phase 1 目标是完整游戏闭环，不是 mod 系统、Lua 桥、复杂内容流水线。

### 2.3 当前真正的风险

当前最大的风险不在“技术栈是否成立”，而在于：

1. 如果不从 `1-1.0` 开始建立覆盖率与测试门禁，后续阶段会快速退化为“能跑但不可证”。
2. 如果把过多逻辑放进 `client`，后续 AI 自验证与长期演进会直接受损。
3. 如果在 `1-2.0` 之前提前堆物品、天赋或多层地牢，系统之间的调试成本会被成倍放大。

---

## 3. Signature Experience（Phase 1 体验合同）

Phase 1 的完成口径固定为：

1. 玩家可以从主菜单开始一局新游戏。
2. 玩家可以在有视野限制的地牢里移动、探索、战斗、升级、换装、使用天赋。
3. 玩家可以在 5 层地牢中推进，最终要么死亡，要么击杀 Boss 通关。
4. 整个过程中：
   - 战斗结果可解释；
   - 属性变化可观察；
   - 存档/读档可恢复；
   - 核心逻辑可以通过 `./gradlew test` 与 JaCoCo 报告自证。

Phase 1 的完成标准以“可玩 + 可证”为主，而不是以系统数量为主。

---

## 4. 全局硬约束

1. 不允许把游戏规则回流进 `client`。
2. 所有新增核心逻辑都必须提供确定性测试：
   - 固定随机种子
   - 固定输入
   - 固定输出断言
3. `1-1.0` 必须建立 `jacocoTestReport` 与 `jacocoTestCoverageVerification`。
4. 每个阶段都必须给出：
   - 自动化命令
   - 报告产物位置
   - 人工白盒验收步骤
5. 每个阶段都必须形成一个可演示、可截图、可录屏的真实切片。
6. 任何阶段都不能为了赶进度而降低 `core` 零引擎依赖约束。
7. 不允许在 Phase 1 内引入 Lua mod runtime、网络联机、复杂 UI 系统或 Tile 正式美术管线。

---

## 5. 非目标

1. 不在 Phase 1 内引入真正的 mod-first 运行时。
2. 不在 Phase 1 内引入第二职业或多职业体系。
3. 不在 Phase 1 内做多平台发布矩阵。
4. 不在 Phase 1 内做完整 tile 渲染、动画资源流水线或复杂 UI 皮肤系统。
5. 不在 Phase 1 内追求“像 ToME 一样深”的内容量。

---

## 6. 阶段顺序（1-1.0 ~ 1-5.0）

### 1-1.0 Foundation & Verifiable Core（P0）

**目标**: 建立多模块骨架、ECS、地图、FOV 和可验证的最小 client 壳层。  
**出口**: `@` 能在 BSP 地牢里移动，`core` 的 ECS/地图/FOV 已有覆盖率门禁。

详细文档：
`docs/phase1/2026-03-12-phase1-1.0-foundation-and-verifiable-core.md`

### 1-2.0 Combat & AI（P0）

**目标**: 建立回合推进、近战战斗、A* 与怪物 AI，形成“探索 -> 接战 -> 升级”的基本循环。  
**出口**: 地牢中存在多种 AI 怪物，玩家可以战斗、升级，日志可解释。

详细文档：
`docs/phase1/2026-03-12-phase1-2.0-combat-and-ai.md`

### 1-3.0 Items & Talents（P1）

**目标**: 建立物品、装备、属性派生与 4 个战士天赋，形成 build 的第一层差异。  
**出口**: 玩家能拾取、穿戴、使用消耗品，并通过热键使用 4 个天赋。

详细文档：
`docs/phase1/2026-03-12-phase1-3.0-items-and-talents.md`

### 1-4.0 Full Game Loop（P1）

**目标**: 把多层地牢、Boss、存档、胜负判定串起来，形成完整 run。  
**出口**: 从主菜单开局到击杀 Boss 或死亡，完整 run 可闭环。

详细文档：
`docs/phase1/2026-03-12-phase1-4.0-full-game-loop.md`

### 1-5.0 Polish & Release（P1）

**目标**: 数值平衡、检视模式、覆盖率达标、Bug 收口与发布准备。  
**出口**: `v0.1.0` 具备可下载运行形态，`core` 覆盖率达标，P0/P1 Bug 清零。

详细文档：
`docs/phase1/2026-03-12-phase1-5.0-polish-and-release.md`

---

## 7. 全局测试与自证契约

### 7.1 自动化门禁

从 `1-1.0` 起，必须把以下命令作为阶段验收入口：

```bash
./gradlew test
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

若后续按模块拆分更细，还应允许：

```bash
./gradlew :core:test
./gradlew :game:test
./gradlew :client:run
```

### 7.2 覆盖率门禁

Phase 1 的覆盖率不是只看收尾结果，而是阶段内逐步抬升：

1. `1-1.0`
   - `ecs / map / fov` 相关包行覆盖率 `>= 85%`
   - `core` 总体行覆盖率 `>= 60%`
2. `1-2.0`
   - `turn / combat / pathfinding / ai` 相关包行覆盖率 `>= 85%`
   - `core` 总体行覆盖率 `>= 70%`
3. `1-3.0`
   - `item / talent / stats / inventory` 相关包行覆盖率 `>= 85%`
   - `core` 总体行覆盖率 `>= 75%`
4. `1-4.0`
   - `save / progression / dungeon loop` 相关包行覆盖率 `>= 80%`
   - `core` 总体行覆盖率 `>= 80%`
5. `1-5.0`
   - `core` 总体行覆盖率 `>= 80%`
   - 战斗、地图、天赋、物品、存档五类关键包行覆盖率 `>= 85%`

### 7.3 白盒自证

每个阶段都必须保留：

1. 固定 seed 或固定数据夹具
2. 固定操作步骤
3. 固定预期结果
4. 固定证据位置

推荐证据形态：

1. `build/reports/tests/test/index.html`
2. `core/build/reports/jacoco/test/html/index.html`
3. 手动验收 checklist 文档或 issue comment
4. 必要时的短录屏或截图

### 7.4 AI 自验证路径

Phase 1 的 AI 自验证路径冻结为：

1. 通过 `./gradlew test` 验证核心规则正确性
2. 通过 `JaCoCo` 报告验证“不是只测 happy path”
3. 通过固定 seed 的白盒手动步骤验证 client 壳层行为
4. 通过阶段出口清单防止“代码写了，但体验没闭环”

---

## 8. 当前状态

当前阶段状态：

1. 文档路线图已建立
2. 项目级 Java / Kotlin / Gradle 基线已固定
3. 真正的 `core / game / client` 模块与测试尚未启动

下一步默认入口：

`docs/phase1/2026-03-12-phase1-1.0-foundation-and-verifiable-core.md`

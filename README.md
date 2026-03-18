# K-ToME

K-ToME 是一个使用 `Kotlin + libGDX` 开发的 ASCII Roguelike 项目。Phase 1 的目标不是堆系统数量，而是交付一条可运行、可测试、可白盒验证的完整 run：

`主菜单 -> 新游戏 -> 5 层地牢 -> 战斗 / 掉落 / 天赋成长 -> Boss -> Victory / Game Over -> Save / Continue`

当前仓库基线对应 `v0.1.0` 的桌面版 MVP 收口。

## 项目结构

1. `core`
   规则真源，保持零引擎依赖，承载 ECS、地图、FOV、移动、战斗、AI、物品、天赋、存档等核心逻辑。
2. `game`
   内容装配层，负责 YAML 数据、实体/物品/Boss 工厂、会话拼装和规则层对外视图。
3. `client`
   表现层，负责桌面入口、输入采集、ASCII 渲染、主菜单、结算画面和检视 UI。

阶段文档见：

1. [docs/mvp-development-guide.md](docs/mvp-development-guide.md)
2. [docs/phase1/2026-03-12-phase1-roadmap.md](docs/phase1/2026-03-12-phase1-roadmap.md)
3. [docs/phase1/2026-03-12-phase1-5.0-polish-and-release.md](docs/phase1/2026-03-12-phase1-5.0-polish-and-release.md)

## 环境要求

1. JDK `21`
2. 仓库自带的 Gradle Wrapper
3. 桌面图形环境

如果当前机器不能直接拉取依赖，可先执行：

```bash
./scripts/bootstrap-deps.sh
```

## 快速开始

先跑自动化验证：

```bash
./gradlew test
./gradlew headlessSmoke
./gradlew clientSmoke
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

如果要走发布前整体验收，直接运行：

```bash
./gradlew preReleaseAcceptance
```

启动桌面客户端：

```bash
./gradlew :client:run
```

客户端会进入主菜单，你可以直接开始新游戏或继续已有存档。

## 当前特性

`v0.1.0` 的 Phase 1 基线包含：

1. 固定 seed 的 BSP 地牢生成
2. 5 层地牢推进与上下楼切换
3. 视野、探索状态与不可见信息遮蔽
4. 回合制移动、近战战斗和 `CHASE / KITE / PATROL` AI
5. 经验、升级、属性点与天赋点分配
6. 地面掉落、背包、装备、消耗品
7. 4 个战士天赋与 `Stamina + Cooldown` 约束
8. 单槽 JSON 存档、自动存档、死亡删档
9. 主菜单、Victory、Game Over
10. `x` 检视模式，用于解释当前场上的怪物、物品和玩家状态

## 操作说明

### 主菜单

1. `Up / Down`
   选择菜单项。
2. `Enter`
   确认。

### 地图移动

支持 8 方向移动：

1. `Q W E`
2. `A S D`
3. `Z C`
4. 方向键
5. `Home / End / PageUp / PageDown`
6. 小键盘 `1 2 3 4 6 7 8 9`

等待一回合：

1. `.`
2. `Space`
3. 小键盘 `5`

### 基础交互

1. `g`
   拾取脚下物品。
2. `i`
   打开或关闭背包。
3. `x`
   进入或退出检视模式。
4. `Ctrl+S`
   手动存档。
5. `Shift+,`
   上楼。
6. `Shift+.`
   下楼。
7. `Esc`
   关闭当前 UI 模式，或在地图模式返回主菜单。

### 检视模式

进入检视后：

1. 用移动键移动光标。
2. 右侧侧栏会显示光标所在格子的地形、怪物数值、状态效果、地面物品效果和楼梯信息。
3. 不可见格子不会泄露实时实体信息。

### 背包界面

打开背包后：

1. `W` 或 `Up`
   向上选择物品。
2. `X` 或 `Down`
   向下选择物品。
3. `Enter` / `Space` / `E`
   使用消耗品，或装备/卸下装备。
4. `Esc` 或 `i`
   返回地图。

### 天赋热键

当前玩家默认带 4 个战士天赋：

1. `1` 猛击
2. `2` 冲锋
3. `3` 盾击
4. `4` 战吼

有目标技能进入目标模式后：

1. 用移动键移动光标
2. `Enter` 或 `Space` 确认施放
3. `Esc` 取消施放

## 开发与验证

常用命令：

```bash
./gradlew test
./gradlew headlessSmoke
./gradlew clientSmoke
./gradlew longRunLab
./gradlew :core:test
./gradlew :game:test
./gradlew :client:test
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
./gradlew :client:run
./gradlew :client:releaseDesktopDist
./gradlew preReleaseAcceptance
```

如果改动涉及依赖、Gradle 配置或 bootstrap 逻辑，再额外执行：

```bash
./scripts/verify-bootstrap.sh
```

Phase 5 的回归与白盒模板见：

1. [docs/phase1/2026-03-12-phase1-5.0-regression-checklist.md](docs/phase1/2026-03-12-phase1-5.0-regression-checklist.md)
2. [docs/releases/v0.1.0-pre-release-acceptance.md](docs/releases/v0.1.0-pre-release-acceptance.md)

## 发布产物

生成桌面分发包：

```bash
./gradlew :client:releaseDesktopDist
```

产物位置：

1. `client/build/release/ktome-v0.1.0-desktop.zip`

压缩包内包含：

1. 可直接启动的桌面脚本与依赖
2. 当前 README
3. 回归清单
4. 已知限制说明

已知限制见：

1. [docs/releases/v0.1.0-known-limitations.md](docs/releases/v0.1.0-known-limitations.md)

发布前验收口径见：

1. [docs/releases/v0.1.0-pre-release-acceptance.md](docs/releases/v0.1.0-pre-release-acceptance.md)

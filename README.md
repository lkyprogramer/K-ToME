# K-ToME

K-ToME 是一个使用 `Kotlin + libGDX` 开发的 ASCII Roguelike 项目，目标是先建立一条可验证、可维护、可持续演进的主线，再逐步扩展到完整 MVP。

当前 `main` 分支已经具备一个可玩的 Phase 1 切片：

1. 固定 seed 的 BSP 地牢生成
2. 视野与探索状态
3. 回合制移动与近战战斗
4. `CHASE / KITE / PATROL` 三类怪物 AI
5. 经验、升级与属性派生
6. 地面掉落、背包、装备与消耗品
7. 4 个战士天赋与 `Stamina + 冷却` 约束

这还不是完整正式版。当前默认是直接进入一层测试地牢开始游戏，还没有主菜单、多层通关和发布版本包装。

## 项目结构

项目采用多模块结构，职责边界固定如下：

1. `core`
   纯规则层，保持零引擎依赖，包含 ECS、地图、FOV、移动、战斗、AI、物品、天赋、属性等核心逻辑。
2. `game`
   内容装配层，负责 YAML 数据、工厂、注册表和把 `core` 拼装成可运行会话。
3. `client`
   表现层，负责 libGDX 桌面入口、输入采集、ASCII 渲染和画面编排。

更完整的阶段设计见：

1. [docs/mvp-development-guide.md](docs/mvp-development-guide.md)
2. [docs/phase1/2026-03-12-phase1-roadmap.md](docs/phase1/2026-03-12-phase1-roadmap.md)

## 环境要求

1. JDK `21`
2. 使用仓库自带的 Gradle Wrapper
3. 桌面运行环境

如果当前机器无法直接从远端仓库拉取依赖，可以先执行：

```bash
./scripts/bootstrap-deps.sh
```

这个脚本会准备本地 `.bootstrap/m2` 仓库和匹配的 Gradle 发行版。

## 快速开始

先做一次自动化验证：

```bash
./gradlew test
```

启动桌面客户端：

```bash
./gradlew :client:run
```

客户端会直接进入当前默认会话：

1. 地图大小 `80 x 50`
2. 默认 seed `20260312`
3. 默认楼层 `1`
4. 默认 FOV 半径 `8`

如果你只是想验证 1-2.0 和 1-3.0 的当前切片，常用命令是：

```bash
./gradlew :core:test
./gradlew :game:test
./gradlew :client:run
```

## 游戏目标

当前切片的目标是熟悉这条基础循环：

1. 在地牢中探索可见区域
2. 与怪物接战并获得经验
3. 拾取地面物品
4. 装备武器或护甲，使用消耗品
5. 通过 `1-4` 热键施放战士天赋

它更接近“白盒可玩切片”而不是完整 run。你可以把它当成当前主循环的验证入口。

## 操作说明

### 地图移动

支持 8 方向移动：

1. `Q W E A D Z X C`
2. 方向键
3. `Home / End / PageUp / PageDown`
4. 小键盘 `1 2 3 4 6 7 8 9`

等待一回合：

1. `S`
2. `.`
3. `Space`
4. 小键盘 `5`

### 基础交互

1. `g`
   拾取脚下物品。
2. `i`
   打开或关闭背包。
3. `Esc`
   关闭背包，或取消目标选择模式。

### 背包界面

打开背包后：

1. `W` 或 `↑`
   向上选择物品。
2. `X` 或 `↓`
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

说明：

1. `猛击`、`盾击` 是近战目标技能。
2. `冲锋` 需要远处敌方目标，并且路径可达。
3. `战吼` 是无目标技能，会直接结算自身增益。

进入目标选择后：

1. 用移动键移动光标
2. `Enter` 或 `Space` 确认施放
3. `Esc` 取消施放

如果体力不足、技能在冷却中、目标不合法或路径不可达，技能会被拒绝并在消息栏给出原因。

## 界面说明

1. 顶部状态栏显示 `HP / STA / ATK / DEF / LV / XP / STAT / TAL`。
2. 主地图区域展示地牢、玩家、怪物和当前视野。
3. 底部消息栏显示命中、闪避、暴击、死亡、经验和技能反馈。
4. 右侧侧栏显示装备栏、天赋冷却状态和脚下物品。
5. 在背包或目标模式下，右侧侧栏会切换为对应的操作提示。

## 当前内容范围

目前 `main` 分支大致覆盖到 Phase 1 的 `1-3.0`：

1. 地图、FOV、回合推进、战斗和基础 AI 已接通
2. 物品、装备、消耗品、属性派生和 4 个战士天赋已接通
3. 桌面客户端可以直接白盒验证上述主循环

尚未完成的内容包括：

1. 主菜单
2. 多层完整通关循环
3. Boss 和正式胜负判定
4. 存档/读档
5. 正式发布包装与更完整的内容量

## 开发与验证

常用命令：

```bash
./gradlew test
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
./gradlew :client:run
```

如果改动涉及依赖、Gradle 配置或 bootstrap 逻辑，再额外执行：

```bash
./scripts/verify-bootstrap.sh
```

阶段验证清单见：

1. [docs/phase1/2026-03-12-phase1-1.0-foundation-verification.md](docs/phase1/2026-03-12-phase1-1.0-foundation-verification.md)
2. [docs/phase1/2026-03-12-phase1-2.0-combat-and-ai-verification.md](docs/phase1/2026-03-12-phase1-2.0-combat-and-ai-verification.md)

# K-ToME

K-ToME 是一个使用 `Kotlin + libGDX` 开发的 Roguelike 项目。当前仓库基线已经从 `Phase 1` 的单职业单 `5` 层 MVP，迁移到 `Phase 2` 的 `locale/schema` 正式合同基线：

`Schema V2 + locale 选择 + formal zone/profession + save/load fail-fast + smoke/lint gate`

当前基线的关键特征：

1. 主菜单 locale 选择已经进入正式主路径，并同时影响 `New Game` 与 `Continue`
2. 首批正式内容对象已切到 `nameKey / descKey / visualKey / iconKey / audioProfile / schemaVersion`
3. 会话配置由 formal `ZoneSpec / ProfessionDef` 驱动，首批 `Phase 2` zone 采用 `2` 层短局基线
4. `localeLint / contractLint / headlessSmoke / clientSmoke / preReleaseAcceptance` 已成为正式验证入口

## 项目结构

1. `core`
   规则真源，保持零引擎依赖，承载 ECS、地图、FOV、移动、战斗、AI、物品、天赋、存档等核心逻辑。
2. `game`
   内容装配层，负责 YAML schema、注册表、locale key、官方内容与会话拼装。
3. `client`
   表现层，负责桌面入口、输入采集、渲染、主菜单 locale、结算画面和检视 UI。
4. `tools`
   lint、smoke、harness 与回归验证入口，负责把 schema / locale / 合同错误尽量提前到构建阶段暴露。

阶段文档见：

1. [docs/mvp-development-guide.md](docs/mvp-development-guide.md)
2. [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](docs/2026-03-13-phase2-to-phase5-final-roadmap.md)
3. [docs/phase2/roadmap.md](docs/phase2/roadmap.md)
4. [docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md](docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md)
5. [docs/phase2/2026-03-13-phase2-pr-03-locale-and-schema-v2.md](docs/phase2/2026-03-13-phase2-pr-03-locale-and-schema-v2.md)
6. [docs/phase1/2026-03-12-phase1-roadmap.md](docs/phase1/2026-03-12-phase1-roadmap.md)

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
./gradlew localeLint
./gradlew contractLint
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

客户端会进入主菜单，你可以直接切换语言、开始新游戏或继续已有存档。

## 当前特性

当前 `Phase 2 locale/schema` 基线包含：

1. 固定 seed 的 BSP 地牢生成
2. formal `ZoneSpec` 驱动的 `2` 层短局与上下楼切换
3. `vanguard / arcanist / rogue / templar` 四个正式 profession skeleton
4. `shattered_outpost / greenwood_fringe / deep_iron_pit / grey_gate_depths` 四个正式 zone skeleton
5. `Schema V2` 内容目录、双语 bundle、`nameKey / descKey / visualKey / iconKey / audioProfile` 正式字段
6. 主菜单 locale 选择，且局内日志、HUD、背包、检视、楼梯名都按当前 locale 渲染
7. 单槽 JSON 存档、自动存档、手动 `Ctrl+S`、Continue 重建与 fail-fast 存档合同校验
8. 回合制移动、近战战斗和 `CHASE / KITE / PATROL` AI，以及 formal `aiProfileId` / `bossEncounterId` 接线
9. `localeLint / contractLint / headlessSmoke / clientSmoke / preReleaseAcceptance` 回归入口
10. `x` 检视模式、背包、Victory / Game Over / 主菜单生命周期闭环

## 操作说明

### 主菜单

1. `Up / Down`
   选择菜单项。
2. `Enter`
   确认。
3. `L`
   切换当前 locale。

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

默认 `vanguard` 开局会带 4 个基础天赋：

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
./gradlew localeLint
./gradlew contractLint
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

发布前验收与回归模板见：

1. [docs/phase1/2026-03-12-phase1-5.0-regression-checklist.md](docs/phase1/2026-03-12-phase1-5.0-regression-checklist.md)
2. [docs/releases/v0.1.0-pre-release-acceptance.md](docs/releases/v0.1.0-pre-release-acceptance.md)

`Phase 2` 相关入口见：

1. [docs/phase2/2026-03-13-phase2-pr-03-locale-and-schema-v2.md](docs/phase2/2026-03-13-phase2-pr-03-locale-and-schema-v2.md)
2. [docs/phase2/2026-03-13-phase2-pr-04-snapshot-and-manifest.md](docs/phase2/2026-03-13-phase2-pr-04-snapshot-and-manifest.md)
3. [docs/phase2/2026-03-13-phase2-pr-05-minimal-tile-shell.md](docs/phase2/2026-03-13-phase2-pr-05-minimal-tile-shell.md)

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

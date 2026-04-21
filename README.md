# K-ToME

K-ToME 是一个使用 `Kotlin + libGDX` 开发的单机 Roguelike。项目当前主线已经从 `Phase 1` 的可玩 MVP，推进到 `Phase 4` 的正式基线：

`可玩的长局 + 稳定的语义合同 + Tile/i18n 正式路径 + ProcGen/Loot/Hidden Content + Content Pack Overlay`

当前长期路线固定为：

`Phase 1 可玩基线 -> Phase 2 语义合同与 Tile/i18n -> Phase 3 深层战斗与长局 -> Phase 4 ProcGen/Loot/Content Pack -> Phase 5 Tactical AI/稳定性/发布`

## 当前状态

当前主干的重点不是继续堆单点玩法，而是维持一套可运行、可验证、可扩展的正式合同。按当前仓库状态，已经落地的主能力域包括：

1. `core / game / client / tools` 四层边界已经固定，规则、内容装配、表现和验证不再混层。
2. 正式内容对象已经全面采用 key 驱动 schema 与版本纪律，主路径围绕 `id / nameKey / descKey / visualKey / iconKey / audioProfile / schemaVersion / tags` 展开。
3. Phase 3 形成了正式的战斗、状态、资源、天赋、Boss、世界路线与长局结构。
4. Phase 4 已经落地 `MapgenPipeline`、`SolvabilityGraph`、`LootBudget`、affix/unique/artifact、elite mutation、hidden event、secret zone 和 content-pack overlay。
5. 统一 white-box framework、固定 seed harness 和 `phase4Report` 已经成为正式验证面，而不是一次性脚本。
6. Phase 5 的目标已明确固定为战术 AI、回放/死因、perf/soak、QA 与发布收口。

## 当前可玩切片

当前仓库已经不只是短局样品，而是一条正式的 run-based 主线：

1. foundation profession 已覆盖 `vanguard / arcanist / rogue / templar`，并且已经有部分 advanced profession 切片进入仓库。
2. 世界路径从 `shattered_outpost` 延伸到 `abyssal_heart`，中间包含 mandatory zone、optional branch、shop、route reward、hidden content 与 finale。
3. Phase 4 zone 已接入 biome、vault、pattern room、terrain tag、solvability proof 与 zone reward profile。
4. 掉落生态已从固定列表演进到 `LootBudget + rarity + affix + special template + unique/artifact` 的正式管线。
5. 仓库内包含官方 sample content pack：[`examples/content-packs/sample.flooded_relics`](examples/content-packs/sample.flooded_relics)，并由 `contentPackHarness` 验证 overlay、资源与 precedence 行为。

## 模块结构

1. `core`
   - 规则真源。
   - 承载 ECS、地图、FOV、回合调度、战斗、状态、AI、ProcGen、Loot、Save/Replay DTO 等确定性逻辑。
   - 保持零引擎依赖。
2. `game`
   - 内容装配层。
   - 负责 YAML schema、registry、官方 data、content-pack overlay、`DataLoader`、`FoundationGameSession` 与正式运行时拼装。
3. `client`
   - 表现层。
   - 负责桌面入口、输入、Tile/HUD/UI、audio、locale bundle、manifest 消费和 `RenderSnapshot` 渲染。
4. `tools`
   - lint、smoke、harness、lab、white-box、phase report 的 owner。
   - 负责把 schema、资源、合同和统计回归提前暴露到自动化门禁。

允许的依赖方向固定为：

1. `game -> core`
2. `client -> game`
3. `tools -> core / game / client`

更详细的架构图见：

1. [docs/project-architecture-mermaid.md](docs/project-architecture-mermaid.md)
2. [docs/project-functional-flow-mermaid.md](docs/project-functional-flow-mermaid.md)

## 仓库热点路径

1. 正式游戏数据：`game/src/main/resources/data`
2. 示例 content pack：`examples/content-packs/sample.flooded_relics`
3. 客户端 manifest 与运行时资源：`client/src/main/resources`
4. 资源规格与离线流水线：`assets-src`
5. 阶段文档与验证清单：`docs/phase2` ~ `docs/phase5`
6. 统一白盒与项目级设计权威：`docs/`

## 环境要求

仓库工具链以根目录 `.sdkmanrc` 为准：

1. `java=21.0.10-tem`
2. `kotlin=2.2.21`

开始任何 Gradle 命令前，先执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

其他要求：

1. 使用仓库自带 `Gradle Wrapper`
2. 桌面运行需要图形环境
3. 若当前机器依赖拉取异常，可先执行 `./scripts/bootstrap-deps.sh`

## 快速开始

最小开发回路：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew test
./gradlew :game:test
./gradlew :tools:test
```

如果要直接验证当前 Phase 4 聚合状态：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew phase4Report
```

Phase 4 报告会输出到：

1. `tools/build/reports/verification/phase4/report-phase4-summary.json`
2. `tools/build/reports/verification/phase4/report-phase4-summary.md`

旧的 `tools/build/reports/phase4/phase4-summary.{json,md}` 只属于 `phase4LegacyReport*` 手工 fallback，不作为默认验收证据。

启动客户端：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:run
```

桌面打包：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:releaseDesktopDist
./gradlew :client:packageMacApp
```

区别：

1. `:client:releaseDesktopDist` 产出跨平台 zip 分发包：`client/build/release/ktome-v0.4.0-desktop.zip`
2. `:client:packageMacApp` 产出 macOS 本地 `.app`：`client/build/release/K-ToME.app`
3. 当前 `.app` 仅用于本地 macOS 打包与启动验证，不包含签名或 notarization
4. app icon 来自离线美术管线生成的 packaging asset：`client/src/packaging/macos/K-ToME-app-icon.png`

更具体的使用方式：

1. `:client:releaseDesktopDist`
   - 适用于需要一个不依赖 Gradle 的桌面分发包时。
   - 产物固定落在 `client/build/release/ktome-v0.4.0-desktop.zip`。
   - 解压后可直接使用生成的启动脚本运行桌面版。
2. `:client:packageMacApp`
   - 仅支持 macOS。
   - 依赖当前 SDKMAN JDK 21 自带的 `jpackage`，以及系统 `/usr/bin/sips`、`/usr/bin/iconutil`。
   - 产物固定落在 `client/build/release/K-ToME.app`。
   - 该任务会自动准备 runtime jars、生成 `.icns` 图标，并用 `jpackage --type app-image` 组装 `.app`。
3. 如果只需要生成 macOS `.app`，直接执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp
```

4. 打包完成后，可在 Finder 中打开 `client/build/release/K-ToME.app`，或在终端执行：

```bash
open client/build/release/K-ToME.app
```

5. 当前仓库的打包目标仍是本地验证和 pre-release acceptance，不包含 Developer ID 签名、notarization、DMG 打包或发布态安装器。

## 当前验证入口

### 通用 gate

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew test
./gradlew verificationGate
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

### Phase 3 / 长局稳定性

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew headlessSmoke
./gradlew soloClearLab
./gradlew longRunLab
./gradlew combatTraceGolden
./gradlew bossHarness
```

### Phase 4 / ProcGen + Loot + Hidden Content + Pack

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew mapgenSmoke
./gradlew solvabilityHarness
./gradlew lootBalanceLab
./gradlew terrainInteractionBatch
./gradlew hiddenContentHarness
./gradlew contentPackHarness
./gradlew whiteBoxVerify
./gradlew phase4Report
```

### 客户端与资源

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew clientSmoke
./gradlew goldenScreenshot
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
./scripts/generate_macos_app_icon.sh
./gradlew :client:releaseDesktopDist
./gradlew :client:packageMacApp
```

## 文档导航

执行权威与入口优先看这些文档：

1. [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](docs/2026-03-13-phase2-to-phase5-final-roadmap.md)
2. [docs/2026-03-13-core-systems-design-and-phase-supplements.md](docs/2026-03-13-core-systems-design-and-phase-supplements.md)
3. [docs/phase4/roadmap.md](docs/phase4/roadmap.md)
4. [docs/phase4/2026-03-13-phase4-verification-checklist.md](docs/phase4/2026-03-13-phase4-verification-checklist.md)
5. [docs/phase5/roadmap.md](docs/phase5/roadmap.md)
6. [docs/2026-04-04-unified-white-box-verification-framework.md](docs/2026-04-04-unified-white-box-verification-framework.md)
7. [docs/project-architecture-mermaid.md](docs/project-architecture-mermaid.md)
8. [docs/project-functional-flow-mermaid.md](docs/project-functional-flow-mermaid.md)
9. [docs/INDEX.md](docs/INDEX.md)

## 下一阶段

当前主线仍以 Phase 4 的收口与一致性维护为主，下一阶段固定为 `Phase 5`：

1. `TacticalScoringLayer / PerceptionState / HateFocus / TacticalAIDecisionTrace`
2. `replayHarness / perfSmoke / soakRun / death analysis / run history`
3. Localization QA、Accessibility QA、`packageRelease` 与发布资料

换句话说，当前仓库已经不是“做不做玩法”的问题，而是“在不破坏既有合同的前提下，把系统推进到发布级”。

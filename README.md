# K-ToME

K-ToME 是一个使用 `Kotlin + libGDX` 开发的单机 Roguelike。当前仓库已经从早期可玩 MVP 推进到以正式合同、自动化验证和 Tile-only 客户端为核心的阶段：

`可玩的长局 + 稳定语义合同 + Tile/i18n 正式路径 + ProcGen/Loot/Hidden Content + Content Pack Overlay + Dark UI/UX v1 改造`

长期路线保持不变：

`Phase 1 可玩基线 -> Phase 2 语义合同与 Tile/i18n -> Phase 3 深层战斗与长局 -> Phase 4 ProcGen/Loot/Content Pack -> Phase 5 Tactical AI/稳定性/发布`

## 当前状态

当前主线的重点不是继续堆单点玩法，而是在不破坏既有规则合同的前提下，把玩法、内容扩展、验证体系和客户端表现推进到发布级。

已经稳定落地的能力域：

1. `core / game / client / tools` 四层边界已经固定，规则、内容装配、表现和验证不再混层。
2. 正式内容对象采用 key 驱动 schema 与版本纪律，主路径围绕 `id / nameKey / descKey / visualKey / iconKey / audioProfile / schemaVersion / tags` 展开。
3. Phase 3 已形成正式的战斗、状态、资源、天赋、Boss、世界路线与长局结构。
4. Phase 4 已落地 `MapgenPipeline`、`SolvabilityGraph`、`LootBudget`、affix/unique/artifact、elite mutation、hidden event、secret zone 和 content-pack overlay。
5. 统一 white-box framework、固定 seed harness、`verifyChanged`、`nightlyGovernanceGate` 和 `reportPhase4` 是当前验证与报告主面。
6. Phase 5 的目标已经固定为战术 AI、回放/死因、perf/soak、QA 与发布收口。

当前活跃工作重心：

1. Dark UI/UX v1 是现阶段客户端发布级改造主线，入口在 [`UI/PLAN.md`](UI/PLAN.md) 和 [`UI/pr/README.md`](UI/pr/README.md)。
2. 当前 PR 级执行顺序是 `PR-00 -> PR-01 -> PR-01-1 -> PR-02 -> ... -> PR-07`，其中 `PR-01-1` 聚焦 player-centered viewport、`TileRenderer` orchestration 化和 tooltip/modal overlay layer。
3. Dark UI/UX 第一阶段不改 gameplay rule、不改 save/replay/profile schema、不引入 atlas/region manifest schema。
4. Client ASCII fallback 不再是正式验收面；玩家可见 evidence 应走 Tile dark UI、client smoke、golden 或白盒记录。

## 当前可玩切片

当前仓库已经不只是短局样品，而是一条正式的 run-based 主线：

1. release playable profession 覆盖 `vanguard / arcanist / rogue / templar`，`berserker / spellblade` 处于 dev playable / report-only 范围。
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
   - lint、smoke、harness、lab、white-box、phase report 和 dark UI/UX gate 的 owner。
   - 负责把 schema、资源、合同、报告和统计回归提前暴露到自动化门禁。

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
5. Dark UI/UX 总方案与 PR 执行：`UI/PLAN.md`、`UI/pr/README.md`
6. Dark UI/UX sprite plan、key registry 与证据：`UI/sprite-sheets`、`UI/manual-records`
7. 阶段文档与验证清单：`docs/phase2` ~ `docs/phase5`
8. verification 基础设施入口：`docs/verification/README.md`
9. 全局文档路由入口：`docs/INDEX.md`

## 环境要求

仓库工具链以根目录 [`.sdkmanrc`](.sdkmanrc) 为准：

1. `java=21.0.10-tem`
2. `kotlin=2.2.21`

开始任何 Gradle 命令前，先执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

其他要求：

1. 使用仓库自带 `Gradle Wrapper`
2. 所有 `./gradlew` 命令串行执行，不并行启动多个 Gradle 进程
3. 桌面运行需要图形环境
4. 若当前机器依赖拉取异常，可先执行 `./scripts/bootstrap-deps.sh`

## 快速开始

最小开发回路：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew test
./gradlew :game:test
./gradlew :tools:test
```

启动客户端：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:run
```

当前 PR CI 默认入口：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew verifyChanged
```

`verifyChanged` 的耗时摘要输出到：

1. `build/verification/verify-changed/full-task-duration-summary.json`
2. `build/verification/verify-changed/full-task-duration-summary.md`
3. `build/verification/verify-changed/preflight-task-duration-summary.json`
4. `build/verification/verify-changed/preflight-task-duration-summary.md`

当前 nightly/governance 默认入口：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew nightlyGovernanceGate
```

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
./gradlew reportPhase4
```

Phase 4 canonical report 输出到：

1. `tools/build/reports/verification/phase4/report-phase4-summary.json`
2. `tools/build/reports/verification/phase4/report-phase4-summary.md`

`phase4Report` / `phase4ReportOnly` 仍保留为兼容入口；新文档和 PR 收口优先使用 `reportPhase4` / `reportPhase4Only`。

### Dark UI/UX

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew maintainabilityLint
./gradlew verifyChanged
```

资源 PR 追加：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-xx
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
```

## 桌面运行与打包

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

如果只需要生成 macOS `.app`：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp
open client/build/release/K-ToME.app
```

当前仓库的打包目标仍是本地验证和 pre-release acceptance，不包含 Developer ID 签名、notarization、DMG 打包或发布态安装器。

## 文档导航

执行权威与入口优先看：

1. [docs/INDEX.md](docs/INDEX.md)
2. [docs/2026-03-13-phase2-to-phase5-final-roadmap.md](docs/2026-03-13-phase2-to-phase5-final-roadmap.md)
3. [docs/2026-03-13-core-systems-design-and-phase-supplements.md](docs/2026-03-13-core-systems-design-and-phase-supplements.md)
4. [docs/verification/README.md](docs/verification/README.md)
5. [docs/phase4/roadmap.md](docs/phase4/roadmap.md)
6. [docs/phase4/2026-03-13-phase4-verification-checklist.md](docs/phase4/2026-03-13-phase4-verification-checklist.md)
7. [docs/phase5/roadmap.md](docs/phase5/roadmap.md)
8. [docs/2026-04-04-unified-white-box-verification-framework.md](docs/2026-04-04-unified-white-box-verification-framework.md)
9. [UI/PLAN.md](UI/PLAN.md)
10. [UI/pr/README.md](UI/pr/README.md)
11. [docs/project-architecture-mermaid.md](docs/project-architecture-mermaid.md)
12. [docs/project-functional-flow-mermaid.md](docs/project-functional-flow-mermaid.md)

## 下一阶段

当前仓库有两条并行但不混淆的推进线：

1. Phase 4 收口线：继续维护 ProcGen、Loot、Hidden Content、Content Pack、owner evidence 与 `reportPhase4` 的一致性。
2. Dark UI/UX 发布级客户端线：按 `UI/pr/README.md` 从 PR-00 到 PR-07 串行推进，最终收敛玩家可见界面、sprite 资源、golden、白盒和 packaged app 证据。

Phase 5 仍是后续发布收口阶段，固定目标是：

1. `TacticalScoringLayer / PerceptionState / HateFocus / TacticalAIDecisionTrace`
2. `replayHarness / perfSmoke / soakRun / death analysis / run history`
3. Localization QA、Accessibility QA、`packageRelease` 与发布资料

换句话说，当前仓库已经不是“做不做玩法”的问题，而是“在不破坏既有合同的前提下，把系统推进到发布级”。

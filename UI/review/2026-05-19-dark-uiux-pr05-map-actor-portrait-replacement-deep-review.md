# Dark UI/UX PR-05《Map / Actor / Portrait Replacement》落地深度审查

- 审查日期：2026-05-19
- 审查角色：资深 Roguelike / 类 ToME 游戏开发设计总监 + 系统策划总监 + 玩法体验审查负责人
- 审查分支：`codex/dark-uiux-pr05-map-actor-portrait-replacement`
- 比对基准：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md`（443 行，下文统称 spec）
- 输出依据：`code-review-high` skeleton + 用户私有指令（中文叙述、代码 English、不写完整代码、仅给修补轮廓）
- 审查方式：仅基于 working tree 静态阅读 + `git diff` 比对；本轮没有执行任何 Gradle / JUnit / lint 跑流程，所有验证均以"建议运行"形式给出。

---

## 1. Summary

### 1.1 What changed（直观概览）

本 PR 在分支上累计修改 22 个跟踪文件 + 新增 134 个未跟踪文件。从 staged 内容看：

- **资源生产线**：交付 16 张原始 sheet（`r02-tiles-*`、`r03-props-*`、`r03-vfx-telegraph`、`r04-actors-*`、`r05-bestiary-*`、`r05-boss-icons`、`r06-portraits-*`），对应 16 份 prompt（`UI/sprite-sheets/prompts/dark-v1/010-…-025-…`），161 张已切片的 runtime PNG 落到 `client/src/main/resources/dark-v1/{tiles,props,vfx,actors,icons,portraits}/`。
- **Manifest 与 registry**：扩展 `assets-src/image/manifests/phase2-visual-manifest.json`、`client/src/main/resources/manifests/visual-manifest.json`；扩展 `UI/sprite-sheets/sheet-plan.yaml`（+1312 行）、`UI/sprite-sheets/key-registry.yaml`（+1135 行）；新增 `UI/sprite-sheets/pr05-owner-key-inventory.{json,md}`（161 entries、16 sheets）。
- **Client 渲染契约**：复用既有 `TileLayerComposer`（pass-through），未修改组合顺序；`TileRenderModel.kt` 中 actor 维持 `sortedBy { if (actor.isPlayer) 1 else 0 }`；新增的 PR-05 测试落到 `TileLayerComposerTest`、`TileRendererCanvasTest`、`ManifestResolveTest`、`GoldenScreenshotHarnessTest`。
- **验证场景注册**：`ValidationScenarioRegistry` 增加 `dark-uiux-pr05-actor-boss-telegraph`；`ValidationScenarioPresentationCatalog` 同步；`game/src/main/resources/i18n/{en-US,zh-CN}.json` 加 9 行 i18n。
- **手动白盒**：`UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md` 新增并以 `PARTIAL_PASS_…BOSS_TELEGRAPH_BLOCKED` 结案。
- **Out-of-scope 改动（高度可疑）**：`client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt`（+54 行 args 解析）、`tools/.../Phase4V4WhiteboxScenarioCli.kt`（JAVA_TOOL_OPTIONS → EXTRA_KTOME_ARGS 重构）、`game/.../FoundationGameSession.kt`（whitebox 路径解析新增）、`scripts/codex-generate-image.py`（alpha 阈值调整）、`scripts/repack_generated_sheet.py`（`normalize_piece_count` 新逻辑）、`tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml` 把 `dark-uiux-pr05-actor-boss-telegraph` 追加进 phase4-v4 目录。
- **数据归类异常**：`assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl` 被追加了 161 条 `r02–r06` 行（PR05 数据），同时 `dark-v1-pr05-sprite-map-report.jsonl` 已独立存在并含 161 行。

### 1.2 Top Risks（按严重度从重到轻）

1. **PR-05 close-gate 字段缺失**：`pr05-owner-key-inventory.json` 仅 `entries[]`，没有 `ownerExpectedKeys / ownerCoveredKeys / allowedOwnerFallbackKeys / oldStyleOwnerKeys / pendingOwnerKeys` 这五项 close-gate 闭环字段；spec §7（PR 关闭闸门）依赖这些字段独立闭环，当前形态只能间接通过 `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-05` 拿到结果，**PR 不可自闭门**。
2. **`dark-uiux-pr05-map-layer-stack` 场景仅在 GoldenScreenshotHarnessTest 直连 seed 落地**，5 个 spec §4/§8 必需文件（`ValidationScenarioRegistry`、`ValidationScenarioPresentationCatalog`、`i18n/en-US.json`、`i18n/zh-CN.json`、`ClientSmokeHarnessTest`）**全部缺失对应条目**，spec 强制的"两个 manual whitebox label 必须都经过 ValidationScenarioRegistry + Catalog + i18n + ClientSmoke 联调"契约被绕开。
3. **PR-05 sprite-map 数据污染 PR-00 baseline 文件**：`dark-v1-pr00-sprite-map-report.jsonl` 被追加 161 条 PR05 sheet 数据（`r02–r06`），同时 `dark-v1-pr05-sprite-map-report.jsonl` 已经独立成文件。这意味着 PR-00 baseline 不再可作为 raw baseline 比对源，破坏 spec §6.6 "PR-05 资源全部落到 `dark-v1/` 输出，不影响其它 round" 的隔离前提。
4. **Spec §6.2 "Boss warning / telegraph 不被普通 VFX 淹没" 没有生产代码护栏**：`TileRenderModel.kt` 的 `overlayTiles` 由 `snapshot.overlays.flatMap` 顺序生成，**没有按 dangerLevel 排序**；`TileLayerComposer` 是 pass-through；新增的 `keepsBossTelegraphAboveOrdinaryVfx`、`keepsBossTelegraphReadableWhenActorOccupiesCell` 两个测试本质上只是断言"传入顺序被保留"，并未驱动一段会主动重排的代码。Boss 警告渲染的可读性当前只是"碰巧因为快照顺序对了"。
5. **`dark-uiux-pr05-actor-boss-telegraph` 在 ClientSmokeHarnessTest 也不存在**：之前认为只有 `map-layer-stack` 缺，这里复核之后 `actor-boss-telegraph` 同样未进入 ClientSmoke。Spec §4 / §8 明确要求两个场景都覆盖 ClientSmoke。
6. **Manual record 自身存在事实矛盾且以 PARTIAL_PASS 结案**：`UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md` 记录中宣称 `dark-uiux-pr05-actor-boss-telegraph` 不是 ValidationScenarioRegistry 场景，与 `ValidationScenarioRegistry.kt:969` 的注册事实直接冲突；同时整体 result 为 `PARTIAL_PASS_PACKAGED_CUA_MAP_LAYER_STACK_BOSS_TELEGRAPH_BLOCKED`，而 spec §8 明文要求两个 label 都 PASS。
7. **多处明显 out-of-scope 修改**：DesktopLauncher.kt args 通道改造、FoundationGameSession.kt 增加 `resolveWhiteboxRuntimePath`、Phase4V4WhiteboxScenarioCli.kt 把 `JAVA_TOOL_OPTIONS` 全面改写为 `EXTRA_KTOME_ARGS`、phase4-v4-scenarios.yaml 把 PR-05 dark-uiux 场景塞进 phase4-v4 目录，均与 spec §1 "PR-05 只改资源 / manifest / client presentation；不改地图生成、战斗、AI、Boss、掉落、白盒框架" 不符。
8. **`ManifestResolveTest` 仅 spot-check 子集**（zone 4/14、portrait 4/16、bestiary 3/45），161 个 owner key 中只有不到 30% 在 JUnit 层面被 exact-entry 校验，剩余 70% 完全依赖 `darkManifestCoverageLint` Gradle 任务。

### 1.3 Approval

**Approval: `request_changes`**（核心 close-gate / scenario 注册 / sprite-map 隔离三条 BLOCKER 必须修复，否则 PR-05 不能进入合并队列）。

---

## 2. Affected Files（已 modify / new）

| 路径 | 状态 | 范围与关键性 |
|---|---|---|
| `UI/sprite-sheets/sheet-plan.yaml` | modified, +1312 / -18 | In-scope，扩展 R02–R06 sheet 蓝图；同时含大量自动 re-flow 噪声 |
| `UI/sprite-sheets/key-registry.yaml` | modified, +1135 / -16 | In-scope，登记 161 owner keys；含同样的 reformat 噪声 |
| `UI/sprite-sheets/pr05-owner-key-inventory.json` | new, 2606 行 | In-scope，**缺 close-gate 字段（BLOCKER）** |
| `UI/sprite-sheets/pr05-owner-key-inventory.md` | new | In-scope，16 sheets / 161 keys 统计表 |
| `UI/sprite-sheets/prompts/dark-v1/010-r02-tiles-decal..025-r06-portraits-zones.prompt.txt` | new ×16 | In-scope，与 sheet-plan 对应 |
| `assets-src/image/raw/sheets/dark-v1/r02-*.png … r06-*.png` | new ×16（untracked） | In-scope，原始 sheet |
| `assets-src/image/manifests/phase2-visual-manifest.json` | modified | In-scope canonical manifest 扩展 |
| `client/src/main/resources/manifests/visual-manifest.json` | modified | In-scope runtime mirror，需要 `syncPhase2Manifests` 复现 |
| `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` | new, 161 行 | In-scope，PR-05 sprite-map |
| `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl` | modified, +161 行 | **BLOCKER：PR-05 数据污染 PR-00 baseline** |
| `client/src/main/resources/dark-v1/{tiles,props,vfx,actors,icons,portraits}/*.png` | new ×~160（untracked） | In-scope 切片产物 |
| `client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt` | unchanged | 仍为 pass-through，**spec §6.2 排序契约依赖此处** |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | unchanged | overlay 无 dangerLevel 排序（HIGH 风险） |
| `client/src/test/kotlin/com/ktome/client/render/TileLayerComposerTest.kt` | modified | 新增两条 PR-05 测试，但只保留输入顺序 |
| `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | modified | 4 条 PR-05 测试，断言基于输入快照构造顺序 |
| `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt` | modified | 4 条 PR-05 resolver 测试，**仅 spot-check 子集** |
| `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt` | modified | 两个 PR-05 label 的 capture，**直接构造 FoundationGameConfig 绕开 registry** |
| `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt` | unchanged | **缺两条 PR-05 scenario 烟测（HIGH/BLOCKER 见 §4）** |
| `game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt` | modified | 注册 `dark-uiux-pr05-actor-boss-telegraph`；**`dark-uiux-pr05-map-layer-stack` 缺失** |
| `client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt` | modified | 同上：仅 boss-telegraph |
| `game/src/main/resources/i18n/{en-US,zh-CN}.json` | modified, +9 / +9 | 同上：仅 boss-telegraph |
| `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt` | modified, +17 / -3 | Out-of-scope：whitebox 路径解析 + alias 路由 |
| `client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt` | modified, +54 / -1 | Out-of-scope：`--ktome.*` args 解析 |
| `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt` | modified, +18 / -19 | Out-of-scope：runbook 模板从 JAVA_TOOL_OPTIONS 改为 EXTRA_KTOME_ARGS |
| `tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioMaterializationCatalog.kt` | modified | Out-of-scope，同上配套 |
| `tools/src/test/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCliTest.kt` | modified | Out-of-scope，同上配套 |
| `tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml` | modified, +1 | Out-of-scope：把 PR-05 dark-uiux 场景塞进 phase4-v4 目录 |
| `scripts/codex-generate-image.py` | modified, +3 / -3 | 阈值调整：α=255→192/24，checker 阈值 225/14→188/28 |
| `scripts/repack_generated_sheet.py` | modified, +76 / -0 | 新增 `normalize_piece_count`、`squared_distance`、`merge_components`、`repack_from_existing_grid` 旁路 |
| `client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt` 等其它 modified | — | 见上 |
| `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md` | new | In-scope，但 result `PARTIAL_PASS_…`（HIGH） |

---

## 3. Root Cause & Assumptions

### 3.1 改动意图（重建）

从 git diff 推断，本 PR 的实际工作流是：

1. 根据 spec §3 的 16 张 sheet 蓝图，先扩 `sheet-plan.yaml`、`key-registry.yaml`，落 prompt → 跑 codex 生图 → 切片 → 写 runtime PNG。
2. 把 161 个 owner key 写入 `phase2-visual-manifest.json`，再 `syncPhase2Manifests` 到 runtime manifest。
3. 写两条 manual whitebox label（`map-layer-stack` seed 202605090501、`actor-boss-telegraph` seed 202605090502）以及对应 golden screenshot capture。
4. 顺手 fix codex 切图 pipeline 的若干阈值（`codex-generate-image.py` 把 α 检测放宽，`repack_generated_sheet.py` 加 normalize_piece_count 兜底）。
5. 因为 macOS `open -n` 启动 `.app` 时旧的 `JAVA_TOOL_OPTIONS="-D…"` 方案在某些场景下不稳，改造 DesktopLauncher 接 `--ktome.*` args，并把 Phase4V4WhiteboxScenarioCli 输出的 runbook 模板改写为 `EXTRA_KTOME_ARGS` 数组。
6. 把 `dark-uiux-pr05-actor-boss-telegraph` 同时塞进 `ValidationScenarioRegistry`、`ValidationScenarioPresentationCatalog`、`i18n`，并把它复用到 `FoundationGameSession` 的 `phase4-v4-pr05` 准备分支上。
7. 手动白盒只跑了一遍，没两个标签都 PASS，记录 `PARTIAL_PASS_…BOSS_TELEGRAPH_BLOCKED` 收尾。

### 3.2 隐含假设与代价

| 假设 | 代价 / 风险 |
|---|---|
| close-gate 由 Gradle `darkManifestCoverageLint` 单点保护即可，`pr05-owner-key-inventory.json` 自己不必保留 close-gate 三元字段 | 离线审查 / PR 关闭门禁难以从 JSON 上自证；与 spec §7 的 schema 期望偏离 |
| `dark-uiux-pr05-map-layer-stack` 只需要在 golden screenshot 中以 seed 直构 capture，就能满足 spec | 绕开了 ValidationScenarioRegistry / Catalog / i18n / ClientSmoke 这条全链路，违反 spec §4 §8 |
| Spec §6.2 boss-telegraph 顺序要求可以靠"输入快照里 boss 在 ordinary 之后"自然满足 | 一旦上游 snapshot 来源换序（如新增 telegraph、AI 模块改输出顺序），渲染契约即刻破坏，但测试照常通过 |
| 修 codex pipeline 阈值不会回归 PR-01/02/03/04 的旧切片产物 | 没有给出旧 sheet 的回归证据；阈值是全局 helper，下次重切任意旧 sheet 都会走新阈值 |
| Phase4V4WhiteboxScenarioCli 的 JAVA_TOOL_OPTIONS → EXTRA_KTOME_ARGS 改造对历史 white-box runbook 输出 hash 不影响 | 模板字符串变了，runbook 的输出文件指纹会变；下游若有指纹比对（如 CI gates）会一起失败 |
| `dark-v1-pr00-sprite-map-report.jsonl` 可以追加 PR-05 数据 | 破坏 PR00 baseline 数据的可重现性；后续 round 的 diff 比对会被噪声污染 |

### 3.3 本次审查的局限

- 没有跑任何 Gradle / JUnit / lint：所有"绿不绿"的判断都基于源码静态阅读，最终结论需被实际 CI 复核。
- `ManifestResolveTest` 中对 `bestiary`、`portrait`、`zone` 等仅 spot-check 几个 key，剩余 key 是否真的解析到 `dark-v1/` 输出，本审查仅做了 manifest 内部抽样核验，没有逐条 161 行回归。
- Out-of-scope 范围我以 spec §1 的字面定义（"只改资源、manifest 和 client presentation；不改变地图生成、战斗、AI、Boss 或掉落规则"）作判据；若团队规则把 whitebox runbook 模板视为 visual presentation 工具链的一部分，则这些 out-of-scope 判定需要重新校准。

---

## 4. Findings（按严重度排序）

### 4.1 BLOCKER

#### F-B1 · `pr05-owner-key-inventory.json` 缺少 close-gate 关键字段
- **类别**：Integration & Rollout / Contract
- **位置**：`UI/sprite-sheets/pr05-owner-key-inventory.json:1-27`
- **证据**：JSON 顶层只有 `schemaVersion / ownerPr / generatedFrom / requiredOwnerSheetCount / requiredOwnerSheetIds / entries[]`；通过 `rg -n 'ownerExpectedKeys|ownerCoveredKeys|allowedOwnerFallbackKeys'` 全文 0 匹配，spec §7 期望的 `ownerCoveredKeys == ownerExpectedKeys` 闭环字段、空 `missing/pending/old-style/fallback` 集合字段全部缺失。
- **影响**：
  - PR 的关闭闸门（Spec §7）无法仅凭 inventory JSON 自证，强依赖 Gradle `darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-05` 跑出来。
  - 离线评审、跨 PR 的 owner-scope 对账失去 single source of truth；spec 给出的 `dark-uiux-pr05-owner-key-inventory-v1` schema 期望被破坏。
  - 后续 PR-06 / PR-07 若沿用同 schema，会一并丢失 close-gate 字段，连锁污染。
- **Repro**：在仓库根目录执行 `rg -n '"ownerExpectedKeys"|"ownerCoveredKeys"|"allowedOwnerFallbackKeys"|"oldStyleOwnerKeys"|"pendingOwnerKeys"' UI/sprite-sheets/pr05-owner-key-inventory.json`，应得到 0 匹配。
- **Recommendation（patch outline）**：
  1. 修订 inventory generator，在 `entries[]` 之上追加 5 个顶层数组字段：`ownerExpectedKeys` ← 16 sheet 蓝图汇总；`ownerCoveredKeys` ← 实际 resolve 到 `dark-v1/` 的 key 集；`allowedOwnerFallbackKeys` ← 空集；`oldStyleOwnerKeys` ← 空集；`pendingOwnerKeys` ← 空集。
  2. 增加 JSON schema 校验测试（参照其它 PR 的 OwnerKeyInventoryContractTest），在 `darkManifestCoverageLint` 通过后才允许写入这 5 个字段。
  3. 给 `pr05-owner-key-inventory.md` 增加 close-gate snapshot 段，便于评审看到当前 161/161、0 fallback、0 pending。
- **Tests**：新增 `Pr05OwnerKeyInventoryContractTest`（unit）验证：schema 字段齐全、`ownerCoveredKeys.size == ownerExpectedKeys.size == 161`、四个空集字段确实为空。

#### F-B2 · `dark-uiux-pr05-map-layer-stack` 场景未注册到 ValidationScenarioRegistry / Catalog / i18n / ClientSmoke
- **类别**：Integration & Contract / Testing
- **位置**：
  - 缺失：`game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt`（不含 `dark-uiux-pr05-map-layer-stack`）
  - 缺失：`client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt`（仅 `dark-uiux-pr05-actor-boss-telegraph` 见 line ~111）
  - 缺失：`game/src/main/resources/i18n/en-US.json:1863-1871`、`zh-CN.json:1863-1871` 仅含 boss-telegraph 9 keys，无 map-layer-stack
  - 缺失：`client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt` 全文 0 匹配 `dark-uiux-pr05-map-layer-stack`
  - 唯一落点：`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:987-1093` 直接 `FoundationGameConfig(seed=202605090501L, zoneId="greenwood_fringe", playerProfessionId="arcanist")` 构造，**绕开 registry**
- **证据**：
  ```
  $ rg -n 'dark-uiux-pr05-map-layer-stack' \
      client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt \
      game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt \
      client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt \
      game/src/main/resources/i18n/en-US.json \
      game/src/main/resources/i18n/zh-CN.json
  # 0 matches
  ```
- **影响**：
  - Spec §4 / §8 明文要求两个 manual whitebox label 都通过 `ValidationScenarioRegistry → Catalog → i18n → GoldenScreenshot → ClientSmoke` 全链路。当前 `map-layer-stack` 没有 registry/catalog/i18n/smoke 任一支撑，仅靠 golden harness seed 复现——任何后续重命名、scenarioId 校验、CI 拒收未注册场景的 lint 都会爆。
  - 与 PR-01/02/03/04 的全链路习惯不一致，破坏跨 PR review 的稳定性。
  - 实际 runtime 用户根本无法选择该场景（启动器不知道它存在），spec §6.1 "manual QA 选场景跑回归" 失效。
- **Repro**：把 `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt` 中 `darkUiuxPr05MapLayerStackLabel` 改为通过 `ValidationScenarioRegistry.scenarioById("dark-uiux-pr05-map-layer-stack")` 取场景—编译失败。
- **Recommendation（patch outline）**：
  1. 在 `ValidationScenarioRegistry.kt` 增加：
     - `ValidationScenarioId("dark-uiux-pr05-map-layer-stack")` 条目，seed 202605090501，zoneId `greenwood_fringe`，profession `arcanist`，preset `MAP_LAYER_STACK_DARK_UIUX_PR05`（或复用 baseline preset）。
  2. 在 `ValidationScenarioPresentationCatalog.kt` 注册对应展示元数据（labelKey、descriptionKey、acceptanceMatrixSection=`UI05-M01`/`UI05-M02`）。
  3. `i18n/en-US.json` + `zh-CN.json` 增加 9 个 key：`validation.phase4.v4.dark-uiux-pr05-map-layer-stack.{label,description,objective,...}`，与 boss-telegraph 一致。
  4. `ClientSmokeHarnessTest.kt` 复制现有 `phase4-v4-pr05` 烟测，参数化两个新 scenarioId，断言：场景能被 `ValidationScenarioRegistry.scenarioById` 找到、`FoundationGameSession.prepareScenarioPrimaryScene` 不抛、ClientSmoke 路径不退化为 fallback。
  5. 修改 `GoldenScreenshotHarnessTest.kt` 让两个 capture 通过 registry 取场景，而不是手搓 `FoundationGameConfig`。
- **Tests**：上述 4–5 中的 ClientSmokeHarness + GoldenScreenshot 改造即为最小验证集；额外建议 `ValidationScenarioRegistryContractTest` 上加一条 "PR-05 dark-uiux 标签必须 ∈ registry"。

#### F-B3 · PR-05 sprite-map 数据被错误追加到 `dark-v1-pr00-sprite-map-report.jsonl`
- **类别**：Integration / Data Integrity
- **位置**：
  - `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`（modified, +161 行；`rg -c 'sheetId.*r0[2-6]-' = 165`）
  - `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl`（new, 161 行；`rg -c 'sheetId.*r0[2-6]-' = 161`）
- **证据**：
  ```
  $ rg -c 'sheetId.*r0[2-6]-' \
      assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl \
      assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl
  dark-v1-pr00-sprite-map-report.jsonl: 165
  dark-v1-pr05-sprite-map-report.jsonl: 161
  $ git diff HEAD assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl | head -3
  +{"...sheetId": "r02-tiles-ground", "targetKey": "tileset.forest_edge.ground_01"...
  ```
- **影响**：
  - 破坏 PR-00 baseline 的数据隔离前提（spec §6.6 隐含的"各 round 报告互不污染"）。
  - 任何基于 `dark-v1-pr00-…` 做对照的后续 PR review、bisect、回滚都会引入虚假 diff。
  - 增加 165 vs 161 这种"奇怪 4 行多余"症状的排查噪声（多 4 条很可能是 PR-00 原本就有的非 r02–r06 行，但混入 PR-05 后总体数据语义乱）。
- **Repro**：`git log -p assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl` 看追加 commit，对应一次 generator run 同时输出到了两个文件。
- **Recommendation（patch outline）**：
  1. 回滚 `dark-v1-pr00-sprite-map-report.jsonl` 中由本 PR 追加的 161 行（保留 PR-00 原始 base line）。
  2. 修 generator：sprite-map 报告输出文件名必须与 `ownerPr` 严格一一对应，禁止在同一次 dry-run 中同时写多份 `dark-v1-pr??-*.jsonl`。
  3. 给 generator 加 idempotency 测试：跑两次 dry-run，PR-00 文件 hash 不变；只有 PR-05 文件被生成。

### 4.2 HIGH

#### F-H1 · Spec §6.2 boss-telegraph 顺序契约缺生产代码护栏
- **类别**：Correctness / Testing
- **位置**：
  - 缺排序：`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:559-570`（`overlayTiles = snapshot.overlays.flatMap { … }`，无 `sortedBy { it.dangerLevel }`）
  - 缺排序：`client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt`（pass-through，无任何按 dangerLevel 重排）
  - 弱测试：`client/src/test/kotlin/com/ktome/client/render/TileLayerComposerTest.kt`（`composesTerrainPropsVfxTelegraphBeforeActors`、`keepsBossTelegraphAboveOrdinaryVfx`）—— 断言"输出顺序 == 输入顺序"
  - 弱测试：`client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt:1672` `keepsBossTelegraphReadableWhenActorOccupiesCell`—— ordinaryIndex < bossWarningIndex < bossActorIndex 由测试快照构造顺序决定，不是生产代码导致
- **影响**：
  - "Boss warning 不被普通 VFX 淹没"这条 spec §6.2 显式承诺没有任何生产代码强制；只要某天 `FoundationGameSession` 输出 overlays 时把 boss telegraph 放到 ordinary 之前，渲染就破，但所有测试都还会绿。
  - 类 ToME 玩家最敏感的 readability invariant 居然依赖隐式输入顺序，长期维护风险极高。
- **Repro**：把任一现有测试中 `overlays` 列表里的两个元素位置对调（boss 放前 ordinary 放后），断言不变；同时手动渲染会看到 ordinary 把 boss warning 盖住——这就是漏洞。
- **Recommendation（patch outline）**：
  1. 在 `TileRenderModel.kt` 中给 overlay 投影后追加一道稳定排序：`overlayTiles.sortedWith(compareBy({ it.dangerLevel }, { it.spawnOrder }))`（dangerLevel 升序意味着 boss telegraph 在后绘制，最上层）。
  2. 修改 `TileLayerComposerTest.keepsBossTelegraphAboveOrdinaryVfx`：传入顺序故意反过来（ordinary 在 boss 之后），断言输出顺序按 dangerLevel 正确重排——而非保持输入顺序。
  3. 给 `TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell` 加入"反序输入"用例，确保排序是生产代码护栏，不是测试 fixture 的副作用。
  4. 给 `dark-uiux-pr05-actor-boss-telegraph` 的 golden screenshot 增加一帧"输入故意反序"快照对比。
- **Tests**：新增 `TileLayerComposerSortingTest.bossTelegraphPriorityIsEnforced`、`TileRenderModelOverlayOrderingTest.dangerLevelDecidesDrawOrder`。

#### F-H2 · `dark-uiux-pr05-actor-boss-telegraph` 未进入 ClientSmokeHarnessTest
- **类别**：Testing / Integration
- **位置**：`client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`（`rg -n 'dark-uiux-pr05-actor-boss-telegraph' = 0`）
- **证据**：
  ```
  $ rg -n 'dark-uiux-pr05-actor-boss-telegraph' \
      client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt
  # 0 matches
  ```
- **影响**：
  - Spec §8 要求两条 manual whitebox label 都过 ClientSmoke；现仅 `phase4-v4-pr05` 通用烟测覆盖，无 dark-uiux 专属断言。
  - 假如 `FoundationGameSession.prepareScenarioPrimaryScene` 对 `dark-uiux-pr05-actor-boss-telegraph` 路由路径未来回归（参考 F-M3 中的 alias-fallthrough），ClientSmoke 不会发出告警。
- **Recommendation**：在 ClientSmokeHarnessTest 中按 boss-telegraph + map-layer-stack 两条 ID 各加一条 parametrized smoke：构造 `ValidationScenarioRegistry.scenarioById` → 进入主场景 → 断言不抛、不退化为 fallback、TileRenderModel 至少含 1 个 dangerLevel >= BOSS_WARNING 的 overlay 项。
- **Tests**：同上 Recommendation。

#### F-H3 · Manual record 自相矛盾且以 PARTIAL_PASS 收尾
- **类别**：Docs / Acceptance
- **位置**：`UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md`（result 字段 `PARTIAL_PASS_PACKAGED_CUA_MAP_LAYER_STACK_BOSS_TELEGRAPH_BLOCKED`；reason 文本声称 `dark-uiux-pr05-actor-boss-telegraph` 不是 ValidationScenarioRegistry 场景）
- **证据**：手动记录中关于"boss-telegraph 未注册"的陈述与 `ValidationScenarioRegistry.kt:969` 的实际注册条目互相冲突。
- **影响**：
  - Spec §8 明确要求两个 label 都 PASS；当前是单边 PARTIAL_PASS。
  - reason 字段里事实陈述错误，会误导后续审计；可能导致下一个 PR 复用错误判断（"反正 boss-telegraph 不在 registry"）。
  - Result 枚举值 `PARTIAL_PASS_PACKAGED_CUA_…` 长得像一个 ad-hoc 字符串，破坏 manual record 的枚举闭包，下游分析脚本（如 manual-record audit）可能 silently 跳过。
- **Recommendation**：
  1. 立即修正 reason：boss-telegraph 已注册在 `ValidationScenarioRegistry.kt:969`、`Catalog:111`、`i18n` 中；如果 manual 还没跑通，应明确写出真实阻塞原因（环境 / 资源 / 时间）。
  2. 选择：要么完成两条 label 的实跑达到 `PASS`，要么把当前 PR 拆为"主体 + 后续手动验证"两步，并在 spec §9 deferred handoff 段挂账。
  3. Result 字段必须落到既有枚举（如 `PASS / FAIL / DEFERRED`），不允许临时复合字符串。

#### F-H4 · `phase4-v4-scenarios.yaml` 把 PR-05 dark-uiux 场景塞进 phase4-v4 目录
- **类别**：Integration / Scope
- **位置**：`tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml`（+1 行：`- id: dark-uiux-pr05-actor-boss-telegraph` 插在 `phase4-v4-pr05` 与 `phase4-v4-pr06` 之间）
- **影响**：
  - 批次混淆：phase4-v4 序列的资源 / acceptance 节奏与 dark-uiux PR-05 不在同一波次，把 PR-05 dark-uiux scenario 当作 phase4-v4 的一部分会让 phase4-v4 的 CI gate / runbook 出现非预期 case。
  - Spec §1 明确"不动地图生成、战斗、AI、Boss"——把 boss-telegraph 写进 phase4-v4 目录也是潜在的"动框架"。
  - 加上 `FoundationGameSession.kt` 的 alias 路由（见 F-M3），boss-telegraph 会同时被两套 gate 驱动，行为含混。
- **Recommendation**：
  1. 把这一行从 `phase4-v4-scenarios.yaml` 移除。
  2. 在 `tools/src/main/resources/dark-uiux/…` 或同等 dark-uiux owned 的目录下登记，与 `phase4-v4` 严格隔离。
  3. CI lint 增加规则："只能在 dark-uiux owned 的 yaml 中出现 `dark-uiux-*` 场景 id"。

### 4.3 MEDIUM

#### F-M1 · DesktopLauncher.kt 接 `--ktome.*` args 通道，是 PR-05 视觉范围外修改
- **类别**：Scope / Architecture
- **位置**：`client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt:13-90`（+54 行：`applyKtomeLaunchProperties`、`parseKtomeLaunchProperty`、`resolveLaunchRuntimePath`）
- **影响**：
  - Spec §1 "PR-05 只改资源、manifest 和 client presentation"；启动器 args 解析是 client bootstrap 通路，不算 visual presentation。
  - 错误使用 `require(separator > 0 && separator < property.lastIndex) { … }` 会在解析失败时直接抛 IllegalArgumentException，杀掉整个 Desktop launcher——任何启动 args 拼写错误都不再 graceful fail。
  - 接受 `user.home` 这种 JDK 标准 key 直接 `System.setProperty("user.home", …)` 修改进程 user.home，对其它使用 `~` 的库（preferences、tmpdir 推导）副作用面较大。
- **Recommendation**：
  1. 将本改动拆到独立 PR（"launcher: 支持 --ktome.* arg 透传"），由 client/launcher 域 owner 审；本 PR 回退。
  2. 若必须并入：把 `require(...)` 换成"输出 warning + 跳过"；显式白名单允许的 property name，避免任意 system property 注入。
  3. 给 `applyKtomeLaunchProperties` 增加单元测试覆盖 happy path / 非法格式 / 不在白名单 / `user.home` 路径替换。

#### F-M2 · `Phase4V4WhiteboxScenarioCli` 把 `JAVA_TOOL_OPTIONS` 全面改写为 `EXTRA_KTOME_ARGS`
- **类别**：Scope / Tooling
- **位置**：`tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt:145-200`（+18 / -19）；配套 `Phase4V4WhiteboxScenarioMaterializationCatalog.kt`、`Phase4V4WhiteboxScenarioCliTest.kt`
- **影响**：
  - Spec §1 "不改地图生成、战斗、AI、Boss、掉落"；白盒 runbook 模板属于 phase4 跑流程工具链，与 PR-05 视觉资源严格无关。
  - 同时把"启动后 `pgrep` 寻 PID + 20 次 0.5 秒轮询" 这段健壮性逻辑整段移除，换成纯靠 `--ktome.*` 注入；如果 DesktopLauncher 解析失败（见 F-M1），新 runbook 无法回退监测 PID，white-box 不再有 `APP_LAUNCH_FAILED` 检测。
  - runbook 模板字符串变了，下游若对 runbook 输出 hash 做指纹比对（如 phase4 CI gates），会产生未声明的破坏。
- **Recommendation**：
  1. 将本块改动拆到独立 PR（"whitebox: prefer --ktome.* args over JAVA_TOOL_OPTIONS"），由 tools/whitebox owner 单独审。
  2. 保留 PID 探活逻辑，新参数通道与旧检测并存一段时间。
  3. 给该改动加 snapshot 测试：固定一个示例场景，把生成的 runbook 字符串作为 expected fixture 检入，避免静默变形。

#### F-M3 · `FoundationGameSession.kt` 给 `phase4-v4-pr05` 加 `dark-uiux-pr05-actor-boss-telegraph` 别名
- **类别**：Scope / Routing
- **位置**：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:1966-2010`（在 `preparePhase4V4Pr05PrimaryScene` 与 `…SecondaryScene` 的 when 分支加 `"dark-uiux-pr05-actor-boss-telegraph"` 作为同等别名）；新增 `resolveWhiteboxRuntimePath(rawPath: String)`（+17 行）
- **影响**：
  - 表面看是 routing 复用，实际把 dark-uiux scenario 与 phase4-v4 scenario 绑定到同一段 `preparePhase4V4Pr05PrimaryScene` —— 任何 phase4-v4-pr05 的场景调整都会顺带改变 dark-uiux boss-telegraph 的可视化。两个批次的演进节奏不同，长期会互相牵制。
  - `resolveWhiteboxRuntimePath` 在 game/ 模块里读 `ktome.repo.root` system property，让 game session 依赖外部进程级配置，破坏 game 模块的纯度（spec §1 隐含 client/manifest scope）。
- **Recommendation**：
  1. 将 `dark-uiux-pr05-actor-boss-telegraph` 拆出独立的 `prepareDarkUiuxPr05BossTelegraphScene` 方法，不与 phase4-v4 共用一段 when 分支；可以共用底层 helper，但 entry point 必须独立。
  2. `resolveWhiteboxRuntimePath` 迁出 game/，放到 tools/whitebox 或 client/启动期；game 不应读 `ktome.repo.root`。
  3. 若坚持复用，请在代码注释中明示"耦合事实"，并在 CI lint 中加 "phase4-v4 与 dark-uiux scenario 不共享 when 分支" 规则。

#### F-M4 · 切图脚本阈值放宽可能影响旧 round 切片产物
- **类别**：Performance / Regression
- **位置**：
  - `scripts/codex-generate-image.py:182-209`（α 检测 `255` → `>= 192` / `>= 24`；checker 阈值 `>=225 && delta<=14` → `>=188 && delta<=28`）
  - `scripts/repack_generated_sheet.py:140-210`（新增 `normalize_piece_count`、`squared_distance`、`merge_components`、`repack_from_existing_grid`）
- **影响**：
  - 阈值放宽用意是接住 PR-05 生成的 sheet 中半透明 / 弱对比 cell；但脚本是全局 helper，下次任何 round（PR-01..04）的旧 sheet 重切都会走新阈值，可能轻微改变像素剪裁边界，产生隐式 visual diff。
  - `normalize_piece_count` 把"过多 piece"按面积合并到最近 anchor，可能在多碎片场景下把语义独立的小 prop 合并到大 prop 中，破坏 contact sheet 与 sheet plan 期望的 cell-by-cell 一对一关系。
- **Recommendation**：
  1. 给 `codex-generate-image.py` 和 `repack_generated_sheet.py` 加 unit tests / golden 切片回归（采样 PR-01..04 的几张代表 sheet，断言重切后 hash 不变；或显式接受新 hash 并落 baseline 更新条目）。
  2. `normalize_piece_count` 触发时打印一条 WARN，告知操作者"自动合并了 N 个碎片到 anchor"，并把决策记录到 sprite-map report 中。

#### F-M5 · `ManifestResolveTest` 仅 spot-check 子集，未覆盖全部 161 个 owner key
- **类别**：Testing / Coverage
- **位置**：`client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt:138, 160, 182, 198`
- **证据**（来自之前会话对该测试的细读）：
  - Zone resolver 仅校验 4/14
  - Portrait resolver 仅校验 4/16（4 classes + 12 trees 的子集）
  - Bestiary resolver 仅校验 3/45（含 boss）
  - Tileset resolver 校验范围相对全
- **影响**：
  - 70% 以上的 owner key 没有 JUnit 层 exact-entry 校验；强依赖 Gradle `darkManifestCoverageLint`。
  - 当 `phase2-visual-manifest.json` 与 `pr05-owner-key-inventory.json` 不同步（人为编辑后未 sync），unit 测试无信号。
- **Recommendation**：
  1. 把 4 个 resolver 测试改为参数化 / table-driven，从 `pr05-owner-key-inventory.json` 直接读 161 个 entry，逐条断言：解析路径前缀 == `dark-v1/`，hash 命中 manifest exact entry。
  2. 或保留 spot-check 的同时新增独立 `Pr05OwnerKeyCoverageJunitTest`：取 inventory entries.size，与解析成功命中 `dark-v1/` 的 entry 数比对，断言 161 == 161。
  3. 若团队主张 close gate 只在 Gradle 层做，则在测试文件顶部注释明确写出"unit 仅 spot-check，权威 gate 是 `darkManifestCoverageLint`"，避免新人误读。

#### F-M6 · `pr05-owner-key-inventory.json` 的 `fallbackKey` 设为 targetKey 自身
- **类别**：Contract / Correctness
- **位置**：`UI/sprite-sheets/pr05-owner-key-inventory.json:29-50`（示例 entry：`"targetKey": "tileset.forest_edge.ground_01"` 同时 `"fallbackKey": "tileset.forest_edge.ground_01"`）
- **影响**：
  - 语义自回环：fallbackKey 的本意是"resolve 失败时退到哪个 key"，等于自身意味着"失败时还回到自己"，提供不了真实兜底链路。
  - 与 spec 示例（fallbackKey = `missing_visual` 或语义合法的 fallback target）偏离。
  - 在 `allowedOwnerFallbackKeys` 为空集（close-gate 期望）的前提下，理论上 fallback 永远不应触发；但若代码无意中走 fallback 分支，会陷入"自指闭环"，可能出现死循环（取决于 ResolverLogic）。
- **Recommendation**：
  1. 短期：把每个 entry 的 `fallbackKey` 改为 `missing_visual`，并依赖 manifest 顶层 `fallbackKey: "missing_visual"` 命中（看 §6.6 调查结果，PR-05 keys 全部 resolve 到 dark-v1，因此 fallback 实际不会走）。
  2. 长期：审视 `pr05-owner-key-inventory-v1` schema 的 fallback 语义—— inventory 是"清单"还是"resolver 配置"，二者职责必须分离。
  3. 在 close-gate lint 增加规则："PR-05 owner inventory 中 fallbackKey 不允许等于 targetKey 自身"。

### 4.4 LOW

#### F-L1 · `sheet-plan.yaml` / `key-registry.yaml` 自动 re-flow 引起的大量 diff 噪声
- **位置**：`UI/sprite-sheets/sheet-plan.yaml` +1312 / -18；`UI/sprite-sheets/key-registry.yaml` +1135 / -16
- **现象**：除新增的 PR-05 16 sheets / 161 keys 真实数据外，大量旧行被 YAML formatter 重新换行（如 `subject: forged iron worn stone panel body with ember scratches and subtle cyan edge light` 由两行合并到一行），让人无法在 diff 中快速看到"真正新增的字段"。
- **影响**：审查带宽被噪声吞掉，evidence 风险窗口增大。
- **Recommendation**：
  1. 把 YAML formatter 的 re-flow 改动单独提交一次（pure formatting commit），随后再提交 PR-05 实质新增。
  2. 或在仓库 `.editorconfig` / pre-commit 中固定 line width，避免每次保存重排。

#### F-L2 · `dark-uiux-pr05-actor-boss-telegraph` 在 i18n 中只有 9 keys
- **位置**：`game/src/main/resources/i18n/en-US.json:1863-1871`、`zh-CN.json:1863-1871`
- **现象**：仅 9 个 key（label/description/objective/etc.），未给"acceptance matrix UI05-M04 行/列说明"留任何字符串槽位；spec §0 Preflight 提到的 acceptance matrix UI05-M01..M05 在 i18n 层面没有可读说明。
- **Recommendation**：要么补齐 acceptance matrix 解释字段，要么显式声明"acceptance matrix 只在 spec 中维护，不进 i18n"，并在 `ValidationScenarioPresentationCatalog` 注释中标注。

#### F-L3 · Manual record 用复合 status 字符串 `PARTIAL_PASS_PACKAGED_CUA_MAP_LAYER_STACK_BOSS_TELEGRAPH_BLOCKED`
- **位置**：`UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md`
- **现象**：状态值脱离既有枚举（`PASS` / `FAIL` / `DEFERRED` 等），分析脚本无法识别。
- **Recommendation**：把信息拆成两层——`status: PARTIAL_PASS`（或 `DEFERRED`）+ `notes:` 文本描述具体阻塞；保留枚举的封闭性。

#### F-L4 · `dark-uiux-pr05-map-layer-stack` golden 与 `dark-uiux-pr05-actor-boss-telegraph` 之间缺乏共享 fixture
- **位置**：`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:987-1093`
- **现象**：两个 capture 各自手搓 `FoundationGameConfig`，无共用 helper；后续 scenario 增加时容易 copy-paste 漂移。
- **Recommendation**：提取 `darkUiuxScenarioConfig(scenarioId)` 单 helper，从 `ValidationScenarioRegistry` 取 config 而非手搓 seed / zoneId / profession（同步 F-B2 的修复路径）。

### 4.5 NIT

#### F-N1 · 16 张 prompt 文件未通过 schema check
- **位置**：`UI/sprite-sheets/prompts/dark-v1/010-r02-tiles-decal.prompt.txt ... 025-r06-portraits-zones.prompt.txt`
- **建议**：增加 `Pr05PromptFilesContractTest` 验证每个 prompt 文件名前缀（编号 + sheetId）严格对应 `sheet-plan.yaml` 中相应 sheetId 的 promptBase，杜绝 typo。

#### F-N2 · `pr05-owner-key-inventory.md` 的 Sheet Summary 表中部分计数较小
- **位置**：`UI/sprite-sheets/pr05-owner-key-inventory.md`
- **现象**：`r03-props-environment` 仅 1 key、`r05-boss-icons` 仅 3 key、`r02-tiles-{ground,wall}` 仅 3/3 key——是否符合 spec §3 蓝图需要 owner 二次确认。
- **建议**：在 MD 中显式贴出"spec §3 蓝图 vs 实际计数"的对照表，避免漏 cell。

#### F-N3 · `phase2-visual-manifest.json` 中含 `phase2/`、`phase3/`、`phase4/` 路径的非 PR-05 owner key 未在 inventory 标注
- **位置**：`assets-src/image/manifests/phase2-visual-manifest.json` 全量
- **现象**：本审查未逐项 cross-check 这些 legacy 路径是否会被 PR-05 owner key 间接 resolve（manifest fallback 链或者前缀规则）；spec §6.6 要求 PR-05 owner key 全部 resolve 到 `dark-v1/`。
- **建议**：在 `darkManifestCoverageLint` 中显式输出每个 PR-05 owner key 的最终 resolved path，落到 inventory MD 中可审计。

---

## 5. Performance

### 5.1 Hotspots
- **TileRenderModel overlay 投影**：当前为 `snapshot.overlays.flatMap`，无排序、无去重；按 F-H1 修复会引入一次 `sortedWith(compareBy(...))`，复杂度从 O(n) 升到 O(n log n)。n 一般 ≤ 数十 overlay tiles，可忽略，但应通过 micro-benchmark 确认（建议运行 `./gradlew :client:test --tests TileRenderModelOverlayOrderingTest` + JMH 抽样）。
- **`pr05-owner-key-inventory.json` 加载**：2606 行 JSON，启动期由 contract test / lint 解析；若以后挂进 runtime（不建议），需要预先解析为内存 map。

### 5.2 复杂度备注
- `repack_generated_sheet.py:normalize_piece_count` 是 O(extras × anchors)；当 piece 数 >> 期望 cell 数时（如生成错误大批碎片），可能产生 O(n^2)；不致命，但建议加上"碎片数上限"检查，超过即报错而非合并。
- `DesktopLauncher.applyKtomeLaunchProperties` 是启动期 O(args) 解析，无性能问题；但 `System.setProperty("user.home", …)` 全进程副作用值得关注。

### 5.3 Bench / Monitoring 建议
- 给 `TileLayerComposer` 加 `LayerPlanCompositionBenchmark`（micro-benchmark），固定 200 actor + 100 overlay 场景下，p99 < 1 ms 才算合格。
- 给 `pr05-owner-key-inventory.json` 加大小护栏：在 contract test 中检查 `entries.size <= 200`，避免无限膨胀。

---

## 6. Integration

### 6.1 API / Contracts
- `pr05-owner-key-inventory-v1` schema 期望与现状偏离 → F-B1 必修。
- `ValidationScenarioRegistry` 与 `ValidationScenarioPresentationCatalog` 字段格式与现有 PR-04 / PR-04-01 习惯一致；只是缺 `map-layer-stack` 条目（F-B2）。

### 6.2 Manifest 隔离
- `phase2-visual-manifest.json` 是 canonical；`client/src/main/resources/manifests/visual-manifest.json` 由 `syncPhase2Manifests` 重建。两者在本 PR 都被修改，验证两份是否 byte-byte 一致建议运行 `./gradlew :client:syncPhase2Manifests` 后跑 `git diff` 检查 runtime 那份是否仍与上次 generator 一致。

### 6.3 数据迁移 / Sprite-Map 报告
- F-B3 必修：回滚 PR-00 sprite-map report 中本次追加的 161 行，确保 baseline 不被污染。
- Generator 行为契约：一次 dry-run 只能产出一份 `dark-v1-pr??-sprite-map-report.jsonl`，需 lint 强约束。

### 6.4 Feature Flag / Rollout
- PR-05 不涉及 runtime feature flag。但建议：在 `FoundationGameSession` 准备 `dark-uiux-pr05-actor-boss-telegraph` 时，加一个 dev 模式开关（`ktome.darkUiux.bossTelegraph.enable`），出错时可以回退到既有 phase4-v4 体验。

### 6.5 Resilience
- DesktopLauncher 改造（F-M1）会增加启动期出错面：参数格式错误 → 整个进程退出。需要降级到 WARN + skip。
- Phase4V4Whitebox runbook（F-M2）移除了 PID 探活，需要补回或显式声明 "macOS open -n 路径下无法 PID 探活"。

### 6.6 Rollback Plan
- 单 PR 整体回滚成本中等：资源 / manifest 可整体回退；out-of-scope 改动（DesktopLauncher / Phase4V4Whitebox / FoundationGameSession `resolveWhiteboxRuntimePath` / scripts）建议先 cherry-pick 拆 PR。
- 若紧急回滚：保留资源 + manifest + sheet-plan + key-registry + 测试改动；剔除 launcher / whitebox runbook / FoundationGameSession alias / pr00 sprite-map 污染。

---

## 7. Testing

### 7.1 Coverage 现状（基于源码静态阅读）

| 测试文件 | PR-05 新增覆盖点 | 备注 |
|---|---|---|
| `TileLayerComposerTest` | 2 条新测试 | 仅断言"输出顺序 == 输入顺序"，未驱动排序 |
| `TileRendererCanvasTest` | 4 条新测试（line 1288/1593/1672/1705） | 排序断言依赖测试快照构造，非生产排序 |
| `ManifestResolveTest` | 4 条 resolver 测试 | spot-check 子集，未覆盖 161 |
| `GoldenScreenshotHarnessTest` | 2 个 dark-uiux label capture | 直构 FoundationGameConfig，绕开 registry |
| `ClientSmokeHarnessTest` | **0 条 dark-uiux 烟测** | F-H2 / F-B2 必补 |
| `ValidationScenarioRegistry` contract test | 仅 boss-telegraph 注册 | F-B2 必补 map-layer-stack |
| `Pr05OwnerKeyInventoryContractTest`（应存在） | **不存在** | F-B1 推荐新增 |

### 7.2 缺口（Given / When / Then）

1. **Given** `dark-uiux-pr05-map-layer-stack` 已注册到 registry，**When** ClientSmoke 运行该场景，**Then** TileRenderModel 至少包含 1 个 terrain + 1 个 props + 1 个 telegraph + 1 个 actor 层 tile。（覆盖 F-B2 + F-H2）
2. **Given** snapshot.overlays 输入"ordinary 在 boss 之后"反序，**When** TileRenderModel/TileLayerComposer 处理，**Then** `overlayTiles` 输出顺序中 boss telegraph index > ordinary index。（覆盖 F-H1）
3. **Given** `pr05-owner-key-inventory.json` 文件，**When** 启动 contract test，**Then** `ownerExpectedKeys.size == 161 && ownerCoveredKeys == ownerExpectedKeys && 其余三集为空`。（覆盖 F-B1）
4. **Given** PR-05 owner key 全集 161，**When** 运行 ManifestResolveTest 参数化版本，**Then** 每个 key resolve path startsWith `dark-v1/`。（覆盖 F-M5）
5. **Given** 任意一次 generator dry-run 写 PR-05 sprite-map，**When** 检查 `dark-v1-pr00-sprite-map-report.jsonl` hash，**Then** 与生成前一致；新数据只落 `dark-v1-pr05-sprite-map-report.jsonl`。（覆盖 F-B3）
6. **Given** `dark-uiux-pr05-actor-boss-telegraph` scenarioId，**When** `ValidationScenarioRegistry.scenarioById` 查询，**Then** 返回对象的 seed=202605090502, zoneId=`grey_gate_depths`, profession=`templar`, preset=BOSS_VARIANT。（守住现有注册的回归）

### 7.3 Flakiness 风险
- GoldenScreenshotHarness 当前直接构造 `FoundationGameConfig`，避开 registry；若 registry 内部对 scenario 还做 zone 解锁 / actor 装备状态等额外初始化，capture 出来的画面与 ClientSmoke 走 registry 出来的画面会出现细微差异，触发未来 golden re-baseline 浪潮。
- `repack_generated_sheet.py:normalize_piece_count` 在边界场景（碎片刚刚多于 cell 期望数）会出现"merge or not" 模糊判定，未来切片输出 hash 可能波动。

### 7.4 Targeted Test Plan（按优先级）
1. F-B2 + F-H2 + F-B1 的契约 / 烟测改造（BLOCKER 起步）
2. F-H1 的反序排序测试（HIGH）
3. F-M5 全量 161 keys 参数化（MEDIUM）
4. F-M4 旧 round 切片回归（MEDIUM）
5. F-L4 共享 fixture 抽取（LOW）

---

## 8. Docs & Observability

### 8.1 Docs 需要更新
- `UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md`：修正 reason 字段、改 status 为合法枚举（F-H3 / F-L3）。
- `UI/sprite-sheets/pr05-owner-key-inventory.md`：增加 close-gate snapshot（F-B1 配套）、spec §3 蓝图对照表（F-N2）。
- `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md` §9 deferred handoff 段：把"boss-telegraph 已在 registry / 但 ClientSmoke 与 map-layer-stack 仍未注册"显式落账，避免下个 PR 误以为 PR-05 已经 close。

### 8.2 Logs / Metrics / Traces
- 给 `FoundationGameSession.prepareScenarioPrimaryScene` 增加 log： `scenarioId=… preset=… outcome=…`，便于 ClientSmoke 出错时一眼定位是否走对了别名分支（F-M3 配套）。
- 给 `repack_generated_sheet.py:normalize_piece_count` 增加 WARN（F-M4 配套）。

### 8.3 Runbook
- whitebox runbook 改造 (F-M2) 若必须保留，请在 `tools/src/main/resources/phase4/whitebox/RUNBOOK.md`（或同等）注明"由 `--ktome.*` args 取代 JAVA_TOOL_OPTIONS"，并保留旧路径作为 fallback。

---

## 9. Open Questions

1. Spec §7 close-gate 字段是不是真的从 inventory JSON 移除到了 Gradle 单点？请给出 owner 决策来源（如 PR-04 / PR-04-01 后某次 schema 演进），如果是这样请同步更新 spec §7 文本，避免 reviewer 误判。
2. `dark-v1-pr00-sprite-map-report.jsonl` 是否被设计为"累计追加文件"（不是 PR-00 baseline）？若是，则 F-B3 降级；若不是，则需要回滚。请由资源管线 owner 确认。
3. DesktopLauncher / Phase4V4WhiteboxScenarioCli 的 args 通路改造是否属于"另一条 in-flight PR 的分支提前合入"？若是，请把这部分 cherry-pick 出去单独走审查。
4. 16 张 sheet 的 prompt 数字编号（010..025）从哪里起序？为什么 PR-05 选 010 而不是 001 或更早号段？是否会与 PR-03 / PR-04 等其它 round 的 prompt 编号冲突？
5. Manual whitebox label `dark-uiux-pr05-map-layer-stack` 与 `dark-uiux-pr05-actor-boss-telegraph` 的 seed（202605090501 / 502）是否与 spec §8 字面一致？请人工对照 spec 行号确认。
6. `FoundationGameSession.resolveWhiteboxRuntimePath` 引入 `ktome.repo.root` system property 依赖；是否所有 ClientSmoke / Golden 跑流程都会设这个 property？没设的环境下 fallback 到当前 working dir 是否安全？

---

## 10. Final Recommendation

### 10.1 Decision
- **Approval: `request_changes`**

### 10.2 Must-fix before merge
1. **F-B1**：补齐 `pr05-owner-key-inventory.json` close-gate 五元字段（ownerExpectedKeys / ownerCoveredKeys / allowedOwnerFallbackKeys / oldStyleOwnerKeys / pendingOwnerKeys）+ contract test。
2. **F-B2**：把 `dark-uiux-pr05-map-layer-stack` 注册到 ValidationScenarioRegistry / Catalog / en-US / zh-CN / ClientSmokeHarnessTest 五个文件，并把 golden harness 改为通过 registry 取场景。
3. **F-B3**：回滚 `dark-v1-pr00-sprite-map-report.jsonl` 中追加的 161 行 PR-05 数据，修 generator 确保单次输出只写一份 round 报告。
4. **F-H1**：在 `TileRenderModel.kt` 加入按 `dangerLevel`（或等价 priority）排序的生产代码护栏，并把测试改为反序输入断言。
5. **F-H2**：把 `dark-uiux-pr05-actor-boss-telegraph` 同步进 ClientSmokeHarnessTest。
6. **F-H3**：修正 manual record 的 reason 矛盾叙述并把 result 收敛到合法枚举；若两 label 实跑不能 PASS，请按 spec §9 deferred handoff 流程登记，并明确开新 PR 跟进。
7. **F-H4**：把 `dark-uiux-pr05-actor-boss-telegraph` 从 `phase4-v4-scenarios.yaml` 移出，与 phase4-v4 严格隔离。

### 10.3 Strongly-suggested before merge（M 级）
- F-M1 / F-M2 / F-M3 / F-M4 / F-M5 / F-M6 拆 PR 或在本 PR 内单独提交并附独立审查证据。其中 F-M3 的 FoundationGameSession alias 与 F-M1 / F-M2 的 launcher / whitebox 改造，强烈建议拆出独立 PR 由原域 owner 审。

### 10.4 Nice-to-have post-merge
- F-L1 / F-L2 / F-L3 / F-L4：noisy diff 拆分、i18n 字段补全、status 枚举闭包、golden / smoke 共享 fixture。
- F-N1 / F-N2 / F-N3：prompt 文件 schema 测试、inventory MD 与 spec §3 对照、phase2 manifest 中 PR-05 owner key 最终 resolved path 显式落表。

### 10.5 Confidence
- **Medium-high**：BLOCKER / HIGH 全部基于源码与 manifest 静态可核对的事实（注册条目存在与否、字段存在与否、git diff 行号）；MEDIUM 中 F-M4 阈值回归风险因未实际跑 codex pipeline 而带不确定性。建议合并前由 PR 作者运行：
  - `./gradlew :client:darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-05`
  - `./gradlew :client:test --tests "*ManifestResolveTest*" --tests "*TileLayerComposerTest*" --tests "*TileRendererCanvasTest*" --tests "*ClientSmokeHarnessTest*" --tests "*GoldenScreenshotHarnessTest*"`
  - `./gradlew :game:test --tests "*ValidationScenarioRegistry*" --tests "*FoundationGameSessionTest*"`
  - 然后把结果贴回 PR 描述，作为本审查的最终复核证据。

---

## 附录 A · 关键文件 / 行号速查

- Spec：`UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md`（443 行）
- Inventory：`UI/sprite-sheets/pr05-owner-key-inventory.json:1-27`（schema 头）
- Registry：`game/src/main/kotlin/com/ktome/game/validation/ValidationScenarioRegistry.kt:969`（boss-telegraph 注册位）
- Catalog：`client/src/main/kotlin/com/ktome/client/validation/ValidationScenarioPresentationCatalog.kt:~111`（boss-telegraph 元数据）
- i18n：`game/src/main/resources/i18n/en-US.json:1863-1871`、`zh-CN.json:1863-1871`
- Golden capture：`client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:156-160, 987-1093`
- Composer：`client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt:全文 pass-through`
- Overlay 投影：`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:559-570`
- Actor 排序：`client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt:585`（player 在 NPC 之后）
- 切图阈值：`scripts/codex-generate-image.py:182-209`
- repack 兜底：`scripts/repack_generated_sheet.py:140-210`
- DesktopLauncher args：`client/src/main/kotlin/com/ktome/client/DesktopLauncher.kt:13-90`
- Whitebox runbook：`tools/src/main/kotlin/com/ktome/tools/whitebox/Phase4V4WhiteboxScenarioCli.kt:145-200`
- Whitebox scenarios yaml：`tools/src/main/resources/phase4/whitebox/phase4-v4-scenarios.yaml`（+1）
- FoundationGameSession alias / 路径解析：`game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:510, 1966-2010, 14535`
- Sprite-map：`assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`（+161）vs `dark-v1-pr05-sprite-map-report.jsonl`（new, 161）
- Manual record：`UI/manual-records/dark-uiux-pr05-map-actor-portrait-replacement.md`

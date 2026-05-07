# Dark UI/UX PR-00 Style & Pipeline Contract — 深度审查报告

> 审查日期：2026-05-07
> 审查范围：当前工作树（branch `codex/dark-uiux-pr00-style-pipeline`）相对 `main` 的全部增量
> 对照基准：[`UI/pr/dark-uiux-pr00-style-and-pipeline-contract.md`](./dark-uiux-pr00-style-and-pipeline-contract.md)
> 角色：Roguelike / 类 ToME 开发设计总监 + 系统策划总监 + 玩法体验审查官
> 决策：`comment`（核心合同已实现，存在 1 项 HIGH 与若干 MEDIUM 偏差，建议合并前/后修复）

---

## 1. Summary

- **What changed**：新增 `UI/sprite-sheets/{sheet-plan,key-registry}.yaml` 与 prompt 编号体系；新增 7 个 Python 脚本（`generate_sheet_prompt`、`slice_spritesheet`、`render_contact_sheet`、`verify_sprite_sheet_map`、`verify_dark_key_registry`、`verify_dark_manifest_coverage`、共享 `dark_sprite_sheet_contract`）；root + tools `build.gradle.kts` 暴露 4 个 dark gate 与 `darkManifestCoveragePr00DryRun`；`VerificationTaskRegistry` 增加 `dark-uiux-pipeline` domain 接入 `verifyChanged`；`manifest-lint.py` 加 `--dark-key-registry / --dark-sheet-plan` 桥接；canonical + runtime visual manifest 同步新增 4 条 dark UI key；`ManifestResolveTest` 增 dry-run 解析回归；`DarkSpriteSheetPipelineScriptTest` 25.5K 提供 11 个端到端用例。
- **Top risks**
  1. **HIGH**：`darkManifestCoveragePr00DryRun` 被绑进默认 verification 链条（`build.gradle.kts:91`、`tools/build.gradle.kts:1136-1140`），使 `final-full` 模式实际从未在合并前出口跑过，与合同 §211.1（裸 `darkManifestCoverageLint = final-full`）的默认语义冲突，是 PR-06/07 的隐性回归雷区。
  2. **MEDIUM**：`verify_dark_manifest_coverage.py` 把 `oldStylePlayerVisibleKeys` 与 `pendingOrRejectedPlayerVisibleCells` 用同一个 `pending_keys` 集合填充，又把 `scopeExternalPendingKeys` 算成"全部 scope 外 key"，丢失合同 §223 schema 设计的语义区分。
  3. **MEDIUM**：`scripts/codex-generate-image.py` 没有超时与失败回退；newest-folder 用 mtime 选取在 macOS APFS 上对快速连续 session 不稳；smoke 输出仅 PNG，没有合同 §8.11 / §5.9 要求的"source folder + source image + repo output + sha256" 落盘记录（脚本只 stdout 打印）。
- **Approval**：`comment` — 合同主体可工作、可被 PR-02..PR-07 继续叠加，但建议把 HIGH 项在合并前修掉，MEDIUM 项纳入紧随其后的微 PR。

---

## 2. Affected Files

| Path | Why | Change |
| --- | --- | --- |
| `UI/sprite-sheets/sheet-plan.yaml` | dry-run sheet plan 真源 | added |
| `UI/sprite-sheets/key-registry.yaml` | key registry 真源 | added |
| `UI/sprite-sheets/prompts/dark-v1/001-r01-ui-chrome.prompt.txt` | 编号 prompt 文件 | added |
| `UI/sprite-sheets/prompts/dark-v1/prompt-index.json` | prompt 索引 | added |
| `assets-src/image/manifests/phase2-visual-manifest.json` | canonical manifest 增 4 条 dark UI key | modified |
| `client/src/main/resources/manifests/visual-manifest.json` | 由 syncPhase2Manifests 同步 | modified |
| `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl` | spriteSheetMapLint 报告（DRY_RUN） | added |
| `assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png` | dry-run raw fixture | added |
| `assets-src/image/contact-sheets/dark-v1/r01-ui-chrome-contact.png` | contact QA fixture | added |
| `scripts/dark_sprite_sheet_contract.py` | schema/load 公共合同 | added |
| `scripts/generate_sheet_prompt.py` | prompt 生成 + index | added |
| `scripts/codex-generate-image.py` | Codex CLI 交接 | added |
| `scripts/slice_spritesheet.py` | sheet → runtime PNG | added |
| `scripts/render_contact_sheet.py` | contact sheet 渲染 | added |
| `scripts/verify_dark_key_registry.py` | darkKeyRegistryLint 入口 | added |
| `scripts/verify_sprite_sheet_map.py` | darkSpriteSheetLint + spriteSheetMapLint 入口 | added |
| `scripts/verify_dark_manifest_coverage.py` | darkManifestCoverageLint 入口 | added |
| `scripts/manifest-lint.py` | 加 dark registry/sheet-plan 桥接 | modified |
| `build.gradle.kts` | 暴露 4 个 root dark gate；接入默认 lint chain；接 dark 桥接参数到 manifestLint | modified |
| `tools/build.gradle.kts` | 注册 5 个 Exec dark task 与 helper；接入 verification chain | modified |
| `tools/src/main/kotlin/com/ktome/tools/verification/VerificationTaskRegistry.kt` | `dark-uiux-pipeline` domain | modified |
| `tools/src/test/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzerTest.kt` | dark 路由回归 | modified |
| `tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt` | 11 个 pipeline 用例 | added |
| `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt` | dry-run frame key 解析回归 | modified |
| `build/codex-image-smoke/shanghai-one-day-poster.png` | codex CLI smoke 产物 | added (build dir) |

---

## 3. Root cause & assumptions

- 合同把 PR-00 定义为"建立后续资源 PR 的 pipeline 与 gate 合同"，因此本 PR 重点不是图，而是：schema、脚本、Gradle 接线、manifest 桥接、verifyChanged 路由、Kotlin pipeline 测试。本次实现整体 follow 了这个定位。
- canonical/runtime manifest 中 `ui.frame.panel.body` / `ui.frame.panel.focus` 的 `rawOutputPath` 都指向 `debug/missing_visual.png`，是有意 fixture：合同 §7.1 不允许提交正式 PNG，§4.6 要求新增 UI key 走通 sync→resolver。`ManifestResolveTest#dark ui dry-run frame keys resolve through exact manifest entries` 确认 resolver 能命中 entry（`fallbackUsed=false`），但实际 PNG 仍是 missing_visual。这是设计选择，不是 bug。
- **关键假设**：`UI/PLAN.md` 与 `UI/ART_STYLE_BIBLE.md` 的"上游合同变化回写 / 风格冻结" 条目（§44 影响范围表）已在更早的 commit 完成；本次 git status 没看到这两个文件改动。如果未做，则违反合同 §44 第一/二行。**Open Question #1**。
- **关键假设**：合同 §5.9 要求的 codex smoke 记录，是用 `build/codex-image-smoke/shanghai-one-day-poster.png` 这个临时产物来证明，对应日志只在历史 stdout 中。如果 reviewer 要求审计 trail，需要 dump 一份 JSON。

---

## 4. Findings（按 severity 倒序）

### [HIGH] [Integration & Rollout Safety] PR-00 默认 verification 链条只跑 dry-run，永远不会 trip final-full

- **Where**：
  - `build.gradle.kts:88-92`（`coverageMode=verifyAll` 链 includes `:tools:darkManifestCoveragePr00DryRun`）
  - `tools/build.gradle.kts:1136-1140`（`verifyAll` 同上）
  - `tools/build.gradle.kts:600-638`（同时存在 `darkManifestCoverageLint`（默认 final-full）+ 固定 mode 的 `darkManifestCoveragePr00DryRun`）
- **Evidence**：
  ```kotlin
  // build.gradle.kts:88
  ":tools:darkKeyRegistryLint",
  ":tools:darkSpriteSheetLint",
  ":tools:spriteSheetMapLint",
  ":tools:darkManifestCoveragePr00DryRun",   // dry-run 进入默认链
  ```
  ```kotlin
  // tools/build.gradle.kts:597
  val darkCoverageMode = rootProject.providers.gradleProperty("ktome.darkUiux.coverageMode").orElse("final-full")
  ```
- **Impact**：合同 §211.1 明确"裸 `darkManifestCoverageLint` 等价 `coverageMode=final-full`"。把 dry-run 接入 default chain，等于让 PR-06/07 之前**任何 PR 的本地/CI `verifyAll` 都不会真正校验 final-full**，`oldStylePlayerVisibleKeys` 红线形同虚设；当 PR-06/07 把"final-full"挂回链条时，会一次性暴露大量历史欠债。
- **Standards**：合同 §188-216 gate stop conditions、§211 默认语义、§215 "`verifyChanged` 触发 dark gate 时必须由 impact routing 显式选择 mode"。
- **Recommendation**（最小修法）：
  1. 在 default chain 里**不要用 `darkManifestCoveragePr00DryRun`**；改成 `darkManifestCoverageLint`（默认 final-full）。
  2. `darkManifestCoveragePr00DryRun` 仅作为 PR-00 的人工命令与 `verifyChanged` 在 PR-00 phase 的显式 routing 目标存在；当前 `VerificationImpactAnalyzerTest#dark ui sprite sheet and manifest changes route to pr00 dry run dark gates` 也只断言路由 dry-run，需要同步更新成 final-full（或加 routing tier 区分）。
  3. 若策略上确实要让所有 PR-00..PR-05 都跑 dry-run，必须在 PR-06 出口前提供"切换 mode"的可执行步骤并在合同中写入退出条款，否则现状是单向陷阱。
- **Tests**：保留 `coverage lint requires owner for owner scope and rejects old style residue in final full`（已覆盖 final-full 失败行为）；新增 `verifyAll` 任务图断言：`darkManifestCoverageLint` 在 mode=final-full 时被 trigger，不被 dry-run 替身遮挡。

---

### [MEDIUM] [Correctness] coverage artifact 三个 pending 集合语义被混用

- **Where**：`scripts/verify_dark_manifest_coverage.py:69`、`105-117`
- **Evidence**：
  ```python
  pending_keys = sorted(key for key in expected_keys if is_pending_output(manifest_by_key.get(key)))
  ...
  "oldStylePlayerVisibleKeys": pending_keys if args.coverage_mode == "final-full" else [],
  "pendingOrRejectedPlayerVisibleCells": pending_keys,
  ```
  以及 owner-scope：
  ```python
  "scopeExternalPendingKeys": sorted(set(registry_keys) - set(expected_keys)),
  ```
- **Impact**：合同 §223 schema 把这三个字段设计成不同语义：
  - `oldStylePlayerVisibleKeys`：仍指向旧风格 PNG 的 player-visible key；
  - `pendingOrRejectedPlayerVisibleCells`：审核未通过 / 待生成的 cell；
  - `scopeExternalPendingKeys`：scope 外**且**仍 pending 的 key（不应包括 scope 外但已 cover 的 key）。
  当前实现把"任何指向 `debug/missing_visual.png` 或非 `dark-v1/` 路径的 key"统统视为 pending，并复用同一集合填多个字段。PR-06/07 的 reviewer 在 artifact 中无法区分"是没切图（pending）"还是"指向旧 phase4 PNG（old style）"；owner-scope 报告会把别的 PR 已 cover 的 key 误标 scope external pending。
- **Standards**：合同 §220-223 三层 schema。
- **Recommendation**：拆分判定函数：
  1. `is_pending(entry)`：rawOutputPath 不存在 / 是 missing_visual / dark-v1 但 PNG 还没切到 runtime；
  2. `is_old_style(entry)`：存在 rawOutputPath 但 prefix ∉ `{dark-v1/, debug/missing_visual.png}` 且 key 在 player-visible registry 中；
  3. `scopeExternalPendingKeys = (registry_keys - expected_keys) ∩ pending_keys`。
- **Tests**：扩展 `coverage lint writes pr00 pending artifact schema`，断言 fixture 中"非 dark 风格 key"出现在 `oldStylePlayerVisibleKeys` 但不在 `pendingOrRejectedPlayerVisibleCells`。

---

### [MEDIUM] [Security / Operations] codex-generate-image 的 race / 超时 / 审计缺口

- **Where**：`scripts/codex-generate-image.py:53-67, 89-112`
- **Evidence**：
  ```python
  subprocess.run(command, check=True, stdin=subprocess.DEVNULL)         # 无 timeout
  ...
  candidates.sort(key=lambda path: path.stat().st_mtime_ns, reverse=True)
  selected = candidates[0]
  if selected.stat().st_mtime_ns < started_at_ns: raise RuntimeError(...)
  ```
- **Impact**：
  1. 没有 `timeout=`：Codex CLI 卡住会让 Gradle/CI 永远 hang；
  2. 用 mtime 选 latest folder，APFS 在快速连续 session 下精度可能并列，且任何 `touch` / index 工具改了旧目录会被错选；
  3. smoke 输出 stdout 4 行（source folder/image/output/sha256）但**未落盘成可审计文件**，合同 §8.11/§5.9 要求"smoke 记录"，仅靠 PNG 不能反查触发链。
- **Standards**：合同 §107.5 / §177 / §254；OWASP "fail loudly, fail fast"。
- **Recommendation**：
  1. `subprocess.run(..., timeout=600)`，TimeoutExpired 时打印 stderr 摘要并退非零；
  2. 把 latest folder 选择改成"在 `started_at_ns` 之后**新建**的 inode"（用 `pathlib.Path.stat().st_birthtime` 或回退到 set diff before/after），避免依赖 mtime；
  3. 在 `--out` 同级写一份 `<out>.smoke.json`（source_folder/source_image/output/sha256/started_at/finished_at），合同 §29 已禁止把 `~/.codex/generated_images` 写进 canonical artifact，但本机调用日志写到 `build/` 临时目录是允许的。
- **Tests**：用 fake codex stub（写 dummy PNG 到一个临时 generated_dir 子目录）跑端到端，断言 smoke JSON schema；增加 `--generated-dir` 在测试里指向 tmp。

---

### [MEDIUM] [Testing] DarkSpriteSheetPipelineScriptTest 写入 repo 真路径，存在污染风险

- **Where**：`tools/src/test/kotlin/com/ktome/tools/darkuiux/DarkSpriteSheetPipelineScriptTest.kt:352-391`
- **Evidence**：
  ```kotlin
  val rawSheet = repoRoot().resolve("assets-src/image/raw/sheets/dark-v1/$sheetId.png")
  val contactSheet = repoRoot().resolve("assets-src/image/contact-sheets/dark-v1/$sheetId-contact.png")
  try {
      writeImage(rawSheet); writeImage(contactSheet); ...
  } finally {
      Files.deleteIfExists(rawSheet); Files.deleteIfExists(contactSheet)
  }
  ```
- **Impact**：测试在 repo 真实目录下写文件，依赖 try/finally 清理。任何 SIGKILL、JVM crash、IDE 中断都会留下未跟踪的 PNG，污染开发者 `git status` 并可能被误 commit；CI 上若并行同 sheetId（虽然加了 nanoTime 抗冲突）仍可能改 working tree。
- **Standards**：测试隔离（避免 side effect on shared state）。
- **Recommendation**：让 `verify_sprite_sheet_map.py` 与 `slice_spritesheet.py` 接收 `--raw-root`（跟 `--runtime-root` 类似）；测试改成全部走 tempDir。无需修脚本签名时，可以用 `JUnit @TempDir` 创建 sheet-plan 时把 `rawSheetPath` 改成临时绝对路径——但当前 `dark_sprite_sheet_contract.repo_relative_error` 会拒绝绝对路径，所以最干净是给脚本加 `--raw-root`。
- **Tests**：现有 `slice script writes dark output and map lint records output hash` 改成 `--raw-root` 注入。

---

### [MEDIUM] [Correctness] manifest-lint dark 桥接对 plan 解析失败时仍合并 expected key

- **Where**：`scripts/manifest-lint.py:296-323`
- **Evidence**：
  ```python
  _, dark_cells, dark_plan_errors = load_sheet_plan(dark_sheet_plan_path)
  ...
  errors += [f"dark sheet plan bridge: {e}" for e in dark_plan_errors]
  ...
  expected_spec_keys |= set(dark_cell_by_key)   # 即使 dark_plan_errors 非空也合并
  ```
- **Impact**：当 sheet-plan 自身 schema 错误（例如 styleTag 漂移、grid 不符 policy），`load_sheet_plan` 仍会返回部分 cells；这些"半解析"的 targetKey 进入 `expected_spec_keys`，掩盖原 manifest 的 missing/extra 报告。最终 errors 非空仍会失败，但报告内容会让 reviewer 误判根因。
- **Standards**：合同 §159（"使 registry/sheet-plan 覆盖的 canonical key 进入 upstream spec coverage 分母，但**不要求**绕过 category/rawOutputPath 一致性"）。
- **Recommendation**：当 `dark_plan_errors or dark_registry_errors` 时，**不要**把 dark cell key 合入 expected_spec_keys，直接 short-circuit 后面的"missing_spec_keys / extra_spec_keys"对 dark 的展开比对，并在错误 message 前缀里加 "dark bridge skipped"，保持原 manifestLint 输出可读。
- **Tests**：新增 manifest-lint 单测：dark plan 故意写错 styleTag，断言报告包含 `dark sheet plan bridge: ...` 但不包含混淆性的"canonical visual manifest is missing dark sheet-plan targetKey..."。

---

### [LOW] [Performance / Build cache] darkSpriteSheetLint 没有声明 outputs

- **Where**：`tools/build.gradle.kts:512-530`（没有 `outputs.file/dir`）
- **Impact**：Gradle 无法把它视为可缓存任务；每次 `verifyChanged` 命中相关 input 都会真正重跑 Python 脚本（虽然秒级，但与其他 dark gate 风格不一致）。
- **Recommendation**：写一个空 marker 文件（如 `build/reports/verification/dark-uiux/sprite-sheet-lint.ok`），在 `--check sheet-plan` 成功时由脚本 touch；Gradle 把它作为 `outputs.file`，并在 task 顶部 `doFirst { delete(marker) }` 保证 dirty re-run。

---

### [LOW] [Correctness] alias cell 仍会切两份独立 PNG 与"复用同一图"语义不符

- **Where**：`scripts/slice_spritesheet.py:37-47`、`scripts/verify_sprite_sheet_map.py:99-130`
- **Evidence**：alias cell（`aliasOf` 非空）被当作普通 cell 切片、独立 hash、独立写 output。
- **Impact**：合同 §94 说"`aliasOf` 仅真实复用同一图时允许"。当 alias cell 的 outputName 指向 `dark-v1/...` 真路径时，slicer 会写两份相同语义但不同字节的 PNG（来自不同 grid cell），让 alias 退化成"两份图"，与 PR-02..PR-07 的"图复用 + manifest 别名"预期相悖。当前 dry-run alias cell outputName=`debug/missing_visual.png`，被 `if not cell.output_name.startswith("dark-v1/")` skip，不会爆雷；但合同没禁止 alias cell 用真 dark-v1 outputName。
- **Recommendation**：在 contract 中显式加约束："`aliasOf` 非空时，cell.outputName 必须等于 alias 目标的 outputName；slice/contact/spriteMap 跳过该 cell 的写盘动作，只在 report 里记录 alias 引用"。`dark_sprite_sheet_contract.load_sheet_plan` 加校验。
- **Tests**：新增 unit："alias cell with mismatched outputName fails sheet-plan lint"。

---

### [LOW] [Maintainability] magic 字符串散落

- **Where**：
  - `scripts/dark_sprite_sheet_contract.py:159`（`f"assets-src/image/raw/sheets/dark-v1/{sheet_id}.png"`）
  - `scripts/verify_dark_manifest_coverage.py:36-43`（`"dark-v1/"`、`"debug/missing_visual.png"`）
  - `scripts/slice_spritesheet.py:38`、`render_contact_sheet.py:35`、`verify_sprite_sheet_map.py:106`
- **Impact**：未来路径或前缀策略变化（例如引入 `dark-v2/`）需要散点改 5 处，容易漏。
- **Recommendation**：在 `dark_sprite_sheet_contract.py` 顶部新增常量 `DARK_RAW_SHEET_DIR = "assets-src/image/raw/sheets/dark-v1"`、`DARK_RUNTIME_PREFIX = "dark-v1/"`、`PENDING_RAW_OUTPUT = "debug/missing_visual.png"`，所有脚本统一引用。

---

### [LOW] [Testing] verifyChanged routing 回归只覆盖 2 路径

- **Where**：`tools/src/test/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzerTest.kt:293-311`
- **Impact**：`InputScope` 配置了 4 个 path prefix + 8 个脚本 + manifest 前缀，但回归只断言"sheet-plan + canonical manifest"路由命中。`scripts/manifest-lint.py`、`assets-src/image/raw/sheets/dark-v1/foo.png`、`client/src/main/resources/dark-v1/foo.png` 等典型变更没回归，未来重构 InputScope 可能静默丢失路由。
- **Recommendation**：增加 parameterized 断言：每个声明在 `dark-uiux-pipeline` 的 path prefix 至少有一条触发样例。

---

### [LOW] [Correctness] sheet-plan 对 size / anchor / safeMarginPx 字段静默忽略

- **Where**：`scripts/dark_sprite_sheet_contract.py:213-247`
- **Impact**：合同 §3.4 "如需支持，只能作为 category policy 或 cell override；不能替代 sheet-level grid"。当前 contract 既不读取也不报错；如果某天 PR-02 在 cell 里写 `size: {w:128,h:128}`，会被静默忽略 → 切图与预期不符却无 lint 信号。
- **Recommendation**：要么显式保留这三个字段并校验"必须遵守 category policy"，要么 strict mode 拒绝未知 cell 字段（白名单）。最小做法是白名单 + warn。

---

### [LOW] [Docs] PR-00 的 fixture 设计未在合同/README 标注"故意指向 missing_visual"

- **Where**：`assets-src/image/manifests/phase2-visual-manifest.json:8213+`、`UI/sprite-sheets/sheet-plan.yaml:21`
- **Impact**：`ui.frame.panel.body / .focus` 的 `rawOutputPath = debug/missing_visual.png` 是 dry-run 设计，但 reviewer 第一眼会怀疑"是不是忘了挂图"。`tags: [dark-uiux,pr00,dry-run]` 提供了一些信号，但没有正文文档。
- **Recommendation**：在 `UI/pr/README.md` 或 sheet-plan.yaml 顶部注释里加一行："PR-00 fixture：UI 帧 key 走 missing_visual fallback 验证 sync→resolver 通路；正式 PNG 由 PR-02+ 接入。"

---

### [NIT] 多处 `print(...)` 不带 logger 前缀，hard 抓难度略高

`render-contact-sheet OK: written=1` / `slice-spritesheet OK: written=1, skippedPending=4` 等，建议加 `[dark-v1]` 前缀，CI 抓日志更友好。

---

## 5. Performance

- **Hotspots**：`verify_sprite_sheet_map.py` 对每个 cell 做 `crop().tobytes() → sha256`，对 4 个 256×256 cell 是亚秒级，未来 8×8 icon-sheet 会到 64 cell × 16k 字节，仍可接受；建议改成对裁剪 PNG 字节做 hash（`save(BytesIO, format="PNG")` 后 sha256），不要 hash 原始 raw RGBA byte，因为同图不同 alpha pre-mult 会让 hash 变。
- **Build cache**：除 `darkSpriteSheetLint` 外其余 dark task 都设了 inputs+outputs+RELATIVE 路径敏感性，配置正确。
- **Bench/Monitor**：建议在 CI 增加 `--profile` 一次，确认 dark gate 占总 verifyAll 时间 ≤ 5%。

---

## 6. Integration

- **API/contracts**：sheet-plan / key-registry / coverage artifact / sprite-map report 的 `schemaVersion` 字段全部到位，未来 bump 时按合同 §6 走"另开 PR"原则。
- **DB migrations**：N/A。
- **Feature flags & rollout**：Gradle 项目属性 `ktome.darkUiux.coverageMode / ownerPr` 已经走 provider，OK。需要补 `verify-bootstrap.sh` 是否包含 dark gate dry-run 验证（合同 §9 要求若新增 Gradle 接线需补 bootstrap）。建议核查 `scripts/verify-bootstrap.sh` 已经被增量改过吗（git status 没看到改动 → **Open Question #2**）。
- **Resilience**：`codex-generate-image.py` 缺超时（见 MEDIUM 项 3）；其余脚本 IO 失败会抛 SystemExit，OK。
- **Rollback plan**：本 PR 仅新增脚本与 4 个 manifest entry；rollback 直接 `git revert` 即可，runtime 不依赖新 dark-v1/ PNG，回滚不会破坏现有玩家可见路径。

---

## 7. Testing

- **覆盖**：
  - `DarkSpriteSheetPipelineScriptTest`（11 用例）：missingRawSheet ✓、alias 同/跨 sheet ✓、alias 循环 ✓、capacity 超限 ✓、schema 错误 ✓、prompt 编号稳定 + 追加 + 移除 ✓、slice fallback skip ✓、slice 真切 + map record ✓、coverage owner-scope 缺 ownerPr fail-fast ✓、final-full pending fail-fast ✓、PR-00 dry-run schema ✓、key registry schema/ownerPr 格式 ✓
  - `ManifestResolveTest#dark ui dry-run frame keys resolve through exact manifest entries`（1 用例）
  - `VerificationImpactAnalyzerTest#dark ui sprite sheet and manifest changes route to pr00 dry run dark gates`（1 用例）
- **Gaps**：
  1. final-full mode 在 default chain 没跑（HIGH 项 1）；
  2. `manifest-lint --dark-key-registry` 桥接没专属单测（仅集成）；
  3. `codex-generate-image.py` 没单测（依赖外部 CLI，可 stub）；
  4. routing 回归覆盖窄（LOW 项）；
  5. `--check map` 的 raw size mismatch / alpha bbox 空 / contact 缺失 / output_hash 路径分支没显式断言（被合并在大用例里）。
- **Flakiness risks**：`DarkSpriteSheetPipelineScriptTest` 写 repo 真路径（MEDIUM 项 4）+ 依赖 system `python3` PATH。
- **Targeted test plan**：
  - **Given** sheet-plan 中 alias cell 的 outputName ≠ alias target outputName **When** 跑 `darkSpriteSheetLint` **Then** fail with "alias outputName mismatch"。
  - **Given** dark sheet-plan styleTag 漂移 **When** 跑 `manifestLint --dark-*` **Then** 报告含 `dark sheet plan bridge: ...` 但**不**追加 `canonical visual manifest is missing dark sheet-plan targetKey`。
  - **Given** Codex CLI stub 在 30s 内不返回 **When** `codex-generate-image.py` 默认 timeout=600 缩成 1 **Then** 报 TimeoutExpired 退非零。
  - **Given** `verifyChanged` 输入 = `client/src/main/resources/dark-v1/foo.png` **When** 跑 analyzer **Then** 路由命中 dark-uiux-pipeline。

---

## 8. Docs & Observability

- **Docs to update / create**：
  - `UI/pr/README.md` 或 sheet-plan 注释解释 PR-00 fallback fixture（LOW 项）。
  - 合同 §211 与 default chain 的偏差，要么修代码（建议），要么在合同里增一段"PR-00..PR-05 期间默认链跑 dry-run，PR-06 出口必须切回 final-full" 的退出条款。
  - 若 `UI/PLAN.md` / `ART_STYLE_BIBLE.md` 未在更早 commit 同步，需补一条上游回写。
- **Logs / Metrics / Traces**：dark task 全是 stdout 文本，没 metric 指标。建议把 `dark-v1-manifest-coverage.json` 的 `expectedKeys / coveredKeys / pendingKeys` 三个数 emit 到 `build/reports/verification/dark-uiux/summary.json`，方便后续 dashboard。
- **Runbook**：合同 §9 验证命令清单可直接落到 `docs/verification/dark-uiux-pr00.md`（如未存在）。

---

## 9. Open Questions

1. `UI/PLAN.md` / `UI/ART_STYLE_BIBLE.md` 是否已在更早 commit 完成上游合同回写与风格冻结？本 PR git diff 未见这两个文件改动。如果未做，需补 commit 才完整满足合同 §44。
2. `scripts/verify-bootstrap.sh` 是否已经为 dark gate 注册引导？合同 §9 "若新增 Gradle 接线" 要求同步更新；当前 git status 未见。
3. `darkManifestCoveragePr00DryRun` 接入默认链是出于"PR-00..PR-05 桥接窗口期"的临时策略还是疏漏？需要 owner 明确。

---

## 10. Final Recommendation

- **Decision**：`comment`（不阻断合并，但需要 follow-up）。
- **Must-fix before merge**：
  1. HIGH 项 1 — 让默认 verifyAll 链最终路径回到 final-full；至少在合同里把退出条款写死；
  2. 解决 Open Question #1 / #2 — 至少明确"已在前置 PR 完成"或"刻意延后到 PR-01"。
- **Nice-to-have post-merge（建议在紧随 PR-01 / PR-02 内修复）**：
  - MEDIUM 项 2、3、4、5（coverage 字段语义、codex 脚本健壮性、测试卫生、桥接错误处理）；
  - LOW 项全部；
  - 增加 routing 与 codex-stub 单测。
- **Confidence**：medium-high。本 PR 实现忠实承接合同骨架，主要风险集中在"默认链行为"与"coverage artifact 语义"两处，会在 PR-06 出口前被放大；其余偏差均为可平滑迭代项。

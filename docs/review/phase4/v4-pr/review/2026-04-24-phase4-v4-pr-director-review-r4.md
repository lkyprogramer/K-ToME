# Phase4 v4 PR 开发文档深度审阅报告 R4

审阅日期：2026-04-24

审阅范围：

- `docs/review/phase4/v4-pr/README.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr03-build-identity-reward-adoption.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr04-hidden-search-zone-hooks.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr06-long-run-route-diversity.md`
- `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md`

参考真源：

- `AGENTS.md`
- `docs/INDEX.md`
- `docs/phase4/roadmap.md`
- `docs/phase4/2026-03-13-phase4-verification-checklist.md`
- `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
- `docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md`
- `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
- 当前代码与资源表：`ContentPackManifest`、content pack resolver、profession tier enum、profession YAML、content pack fixtures

## 结论

本轮修改已经吸收了上一轮多数关键反馈：PR-01 的 tree id 点号问题、PR-02 的 shop offer fingerprint、PR-03 的 `localeLint` / baseline、PR-04 的 mechanics set 公式、PR-05 的 client render snapshot、PR-06 的 route 去重、PR-07 的 runtime manifest 与 harness sidecar 分层，整体质量明显提升。

当前仍建议在进入实现前修 1 个高优先级合同问题、2 个中优先级验证/口径问题、3 个低优先级一致性与目录卫生问题。最重要的是 PR-07 把 `ContentPackManifest.SCHEMA_VERSION` 从 1 升到 2，但文档只要求刷新 sample pack fixture；按当前 loader 代码，所有 `schemaVersion: 1` 的 content pack manifest 都会 fail fast，这不是一个局部 sample pack 变更。

## Findings

### P1-01 PR-07 的 manifest schemaVersion=2 是全局合同变更，当前刷新范围明显不足

证据：

- PR-07 scope 只列了 `ContentPackModels.kt`、resolver、DataLoader、HiddenContentMapgenPipeline、一个 sample fixture spec、一个 renamed fixture 目录、content pack runner 与 phase4 tools：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:57`
- PR-07 明确要求 `ContentPackManifest.SCHEMA_VERSION` 升级为 `2`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr07-sample-pack-add-first-visibility.md:194`
- 当前代码的 manifest schema 常量仍是 `1`：`game/src/main/kotlin/com/ktome/game/contentpack/ContentPackModels.kt:65`
- 当前 resolver 对 schema mismatch 是 fail-fast diagnostic：`game/src/main/kotlin/com/ktome/game/contentpack/ContentPackRuntimeResolver.kt:197`
- 当前示例包与 fixtures 的 runtime manifest 仍大量是 `schemaVersion: 1`，包括 `examples/content-packs/sample.flooded_relics/manifest.yaml` 和 `tools/src/main/resources/fixtures/content-packs/packs/*/manifest.yaml`
- Phase4 content pack 权威文档示例仍展示 `schemaVersion: 1`：`docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1210`

风险：

- 实现者按 PR-07 文档升级常量后，只刷新 sample pack 或单个 fixture，其他 content pack harness fixture 会全部出现 schema mismatch。
- 如果为了让 PR-07 过关临时绕开 schema check，就会违反 Phase4 content pack fail-fast 合同。
- 这是 public manifest contract 变化，不能隐藏在 sample pack visibility PR 内局部处理。

建议：

1. 推荐保守方案：不升级 `ContentPackManifest.SCHEMA_VERSION`，把 `extensions.hiddenBranchBindings` 定义为 schema v1 的可选 additive field；没有该字段的既有 pack 继续合法。
2. 如果坚持升级到 schema v2，PR-07 scope 必须显式加入并刷新所有 runtime manifest：`examples/content-packs/**/manifest.yaml`、`tools/src/main/resources/fixtures/content-packs/packs/*/manifest.yaml`，并同步更新 Phase4 content pack 主文档、PR-08 / PR-09 content pack 文档、verification checklist 和 authoring 示例。
3. 无论选哪条路，都要补一个 schema migration / fixture inventory 测试：断言仓库内所有 runtime manifest 的 schemaVersion 与 `ContentPackManifest.SCHEMA_VERSION` 一致，且 sidecar harness spec 不被误判为 runtime manifest。

### P2-01 README 把 `FROZEN` 写成职业 tier，和当前 `ProfessionTier` enum / 数据模型不一致

证据：

- README 的 tier 表直接列出 `FROZEN`：`docs/review/phase4/v4-pr/README.md:55`
- 当前 `ProfessionTier` 只有 `BASE` 和 `ADVANCED`：`core/src/main/kotlin/com/ktome/core/profession/ProfessionDef.kt:38`
- 当前 `shadowblade` / `warden` 数据是 `tier: ADVANCED`，通过 `tags: [profession, advanced, frozen]` 表示冻结：`game/src/main/resources/data/professions/index.yaml:218`、`game/src/main/resources/data/professions/index.yaml:252`
- PR-01 的 scope 没有声明要修改 `ProfessionTier` enum，但 README 文案容易让实现者新增第三个 enum tier。

风险：

- 如果实现者把 `FROZEN` 当作 enum tier，会触碰 core public model、DataLoader、report classification 和已有 YAML，扩大 PR-01 的合同变更面。
- 如果实现者不改 enum，只按 tags 做分类，README 的 “tier” 表述会造成 review / owner metric 命名歧义。

建议：

1. 将 README 标题改成“职业 release classification 与阻塞门槛口径”，表头从 `tier` 改为 `classification`。
2. 明确写成：`FROZEN` 不是 `ProfessionTier` enum 值，而是由 `tags contains frozen` 派生的 report / eligibility classification。
3. PR-01 对 `excludedFrozenProfessions` 的指标说明同步引用该 classification，避免 downstream PR 误加 enum 或改 YAML tier。

### P2-02 PR-02 要求 `reportPhase4Only` 同源，但 owner suite 与命令没有跑 `reportPhase4Only`

证据：

- PR-02 明确新增 blocking owner metrics：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md:402`
- PR-02 owner wiring 明确要求 `phase4Report` 与 `reportPhase4Only` 使用同一 producer artifact：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md:421`
- PR-02 命令只跑 `reportPhase4`，没有 `reportPhase4Only`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr02-inscription-shop-replacement.md:319`
- README 的 PR-02 最小 owner suite 同样只跑 `reportPhase4`：`docs/review/phase4/v4-pr/README.md:106`

风险：

- PR-02 是 owner metric cutover 类改动，若不跑 `reportPhase4Only`，无法证明 canonical producer artifact 与 aggregation/report-only lane 一致。
- 这会让 PR-03 / PR-06 继续消费 PR-02 指标时带入未验证的 report parity 风险。

建议：

1. PR-02 两处命令均改为包含 `reportPhase4Only reportPhase4`。
2. 自证产物补充 `reportPhase4Only` producer / summary 输出。
3. Completion checklist 增加 `reportPhase4Only` 与 `reportPhase4` 对新 inscription metrics 的 metric id、producer、ownerBaseline、failSemantics 一致性断言。

### P3-01 README 的 PR-00 owner suite 漏掉 `ValidationCommandSourceTest`

证据：

- PR-00 测试范围包含 `ValidationCommandSourceTest`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md:91`
- PR-00 自身命令也包含该测试：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr00-fast-whitebox-validation-mode.md:380`
- README 的 PR-00 最小 owner suite 只列了 CLI test、bootstrap test、registry test，遗漏 command source test：`docs/review/phase4/v4-pr/README.md:104`

建议：

- README PR-00 owner suite 增加 `:client:test --tests com.ktome.client.input.ValidationCommandSourceTest`。

### P3-02 PR-05 自证产物清单漏掉 client render snapshot / golden 证据

证据：

- PR-05 测试范围包含 `client/src/test/kotlin/com/ktome/client/render/**/*SnapshotTest.kt`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:70`
- PR-05 命令包含 `goldenScreenshot clientSmoke`：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:216`
- PR-05 completion checklist 要求 golden 或 client render snapshot 覆盖 3 个 variant warning / telegraph presentation：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:333`
- 但“必须保留以下自证产物”只列了 core/game/tools test 与 bossHarness/report，未列 client snapshot/golden 产物：`docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr05-boss-variant-phase-language.md:222`

建议：

- 在 PR-05 自证产物中增加 client render snapshot 或 `goldenScreenshot` 差异/结果产物，确保 boss telegraph 的玩家可读性不是只靠 `bossHarness` trace 证明。

### P3-03 目标目录存在 `.DS_Store` 本地文件

证据：

- `docs/review/phase4/v4-pr/.DS_Store` 当前存在。
- `.gitignore` 已覆盖 `.DS_Store`，但本地文件仍会污染目录枚举、review file list 与后续打包/同步脚本输出。

建议：

- 在提交前删除该本地文件；无需改 `.gitignore`，因为根 `.gitignore` 已有规则。

### P3-04 PR-07 的 schema bump 若保留，需要把上游 content pack 文档列入同步范围

证据：

- Phase4 content pack 主文档仍把 runtime manifest 示例写为 `schemaVersion: 1`：`docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md:1210`
- PR-08 overlay loader 文档、cross-cutting contract 文档和 verification checklist 都把 runtime manifest schema / sidecar 分层作为稳定合同。
- PR-07 scope 当前只列了 v4-pr 侧开发文档和实现文件，没有列入这些上游 contract 文档。

建议：

- 如果采纳 P1-01 的保守方案，本条只需补一句“本 PR 不升级 manifest schemaVersion，只新增 optional extension”。
- 如果坚持 v2，则必须把 `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`、`docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md`、`docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`、`docs/phase4/2026-03-13-phase4-verification-checklist.md` 纳入 PR-07 文档同步 scope。

## Requirement Alignment

| 维度 | 当前状态 | 结论 |
| --- | --- | --- |
| PR 编排 | README 已固定 `PR-00 -> PR-01 -> PR-02 -> PR-03 -> PR-04 -> PR-05 -> PR-07 -> PR-06`，依赖链合理 | 通过 |
| 非兼容策略 | README 明确 legacy alias / wrapper 删除，PR 文档多数已同步 | 通过 |
| Phase4 owner evidence | 多数 PR 已纳入 `reportPhase4Only`、owner baseline、aggregation manifest | PR-02 需补 `reportPhase4Only` |
| UI 可读性 | PR-01/02/04/05/07 已列 snapshot 或 golden surface | PR-05 自证产物需补 client 证据 |
| content pack 边界 | PR-07 已修正 runtime manifest 与 harness sidecar 分层 | schemaVersion v2 冲击面未收敛 |
| repo hygiene | Markdown fence 平衡；未发现真实机器绝对路径泄漏 | `.DS_Store` 需清理 |

## 功能 / 系统一致性矩阵

| PR | 审阅结果 | 仍需处理 |
| --- | --- | --- |
| PR-00 Fast Whitebox Validation Mode | 文档结构完整，scenario id、command source、registry、package whitebox 路径清晰 | README owner suite 漏列 `ValidationCommandSourceTest` |
| PR-01 Profession Tree Run Choice | tree id 已统一为现有 underscore 权威；starter / learnable / report 分母口径清楚 | README 的 `FROZEN` 表述需从 tier 改为 classification |
| PR-02 Inscription Shop Replacement | offer fingerprint、替换流程、blocking/supporting 指标边界已清楚 | 命令与 README owner suite 补 `reportPhase4Only` |
| PR-03 Build Identity Reward Adoption | 上一轮 baseline / `localeLint` / rollback 边界已修正 | 未发现新的 blocking 问题 |
| PR-04 Hidden Search Zone Hooks | mechanics set 公式、runtime hook 与 flavor-only 分层已修正 | 未发现新的 blocking 问题 |
| PR-05 Boss Variant Phase Language | boss variant schema、harness、telegraph 可读性目标清楚 | 自证产物补 client render snapshot / golden evidence |
| PR-06 Long Run Route Diversity | route intent 去重与 distinct count 已修正；最后合入位置合理 | 未发现新的 blocking 问题 |
| PR-07 Sample Pack Add First Visibility | pack-local binding、secondary slot、sidecar 分层方向正确 | schemaVersion v2 的全局影响必须重新收敛 |

## 玩法与体验审查

从 Roguelike / 类 ToME 体验角度，本轮文档的核心方向是成立的：

- PR-01 把 starter 从“全给”改成“3 个起手 + 第 4 技能进入 learnable”，能让 early build choice 更早出现，不再只是升级 rank。
- PR-02 的满槽铭文购买进入 replacement modal，比直接拒绝购买更符合玩家预期，也能让 shop offer 真正进入构筑选择。
- PR-03 的 capstone / non-weapon payoff 指标能把“构筑身份”从文案描述推进到 run-end 行为证据。
- PR-04 的 zone hook 与 frontstage cue 能降低 hidden content 的黑箱感，但仍保持不是全图提示器。
- PR-05 的 boss variant phase language 方向正确：variant 不应只体现在 JSON 或 report，而要在 telegraph / warning 中被玩家读到。
- PR-06 放在最后合入合理，因为它依赖前面所有构筑、路线、hidden、boss、content pack 字段稳定后再验证 long-run diversity。
- PR-07 的 sample pack first visibility 是 Phase4 content pack 能力从“loader 能跑”走向“玩家能感知”的必要一步。

体验侧唯一需要特别防守的是 PR-07：如果用 schema v2 强制刷新所有 pack，开发成本和回归面会明显超过“让 sample pack first visibility 可见”的体验目标。除非 v2 是有意的 Phase4 content pack contract cutover，否则不建议在这个 PR 引入全局 manifest schema bump。

## 当前阶段必须解决的问题

1. PR-07 先决定 `extensions.hiddenBranchBindings` 是 schema v1 optional field 还是 manifest schema v2 cutover。
2. README / PR-01 明确 `FROZEN` 不是 `ProfessionTier` enum，避免实现阶段误改 core model。
3. PR-02 owner suite 与命令补齐 `reportPhase4Only`。
4. README PR-00 owner suite 补 `ValidationCommandSourceTest`。
5. PR-05 自证产物补 client render snapshot / golden 证据。
6. 清理 `docs/review/phase4/v4-pr/.DS_Store`。

## Removal / Iteration Plan

| 项目 | 建议动作 | 归属 PR |
| --- | --- | --- |
| `schemaVersion: 2` | 若不做全局 cutover，则回退为 `schemaVersion: 1` optional extension；若保留 v2，则全量刷新 runtime manifests 与上游文档 | PR-07 |
| `FROZEN` tier 表述 | 改为 release classification，禁止暗示新增 enum | README / PR-01 |
| PR-02 report 命令 | 添加 `reportPhase4Only`，并在自证产物列出对应 summary | PR-02 / README |
| PR-00 README suite | 补 `ValidationCommandSourceTest` | README |
| PR-05 self-cert | 补 client snapshot/golden 证据 | PR-05 |
| `.DS_Store` | 删除本地文件，保留 `.gitignore` 现状 | 目录卫生 |

## Additional Suggestions

1. README 的 owner suite 表建议和各 PR 文档命令建立“完全包含关系”：README 可以更短，但不能漏掉各 PR 自己声明的 blocking owner test。
2. 对所有使用 `reportPhase4Only` 的 PR，建议统一自证产物措辞：既列 producer artifact，也列 canonical Markdown / JSON summary，避免后续只保存一个 report 入口。
3. PR-07 如果采用 optional extension，建议在 manifest 示例中显式写一句“absence means no pack-local hidden branch binding”，让旧 pack 行为可读。
4. PR-07 的 `secret slot capacity = 2` 建议在 PR-04 或 PR-07 中只保留一个权威 owner，另一个 PR 只消费该字段，避免 hidden pipeline 与 sample pack PR 双方都宣称定义容量。
5. README 的 quick path 表可以继续较短，但建议标题明确为“debug quick path，不替代 owner suite”，避免被实现者当成最终验收命令。

## Suggested Verification

本次是文档审阅，未运行 Gradle；建议文档修正后做以下轻量验证：

1. Markdown 结构：检查 `docs/review/phase4/v4-pr/*.md` fenced code block 平衡。
2. 路径卫生：检查 v4-pr 目录下没有 `.DS_Store`，没有真实机器绝对路径。
3. PR-02 文档 grep：确认 `reportPhase4Only` 同时出现在 PR-02 command、README owner suite、自证产物与 completion checklist。
4. PR-07 schema grep：确认最终口径下，所有 `manifest.yaml` 的 `schemaVersion` 与 `ContentPackManifest.SCHEMA_VERSION` 一致；如果不升级 schema，则 PR-07 manifest 示例不应再出现 v2。
5. README / PR-01 grep：确认 `FROZEN` 只作为 classification / excluded group 出现，不作为 `ProfessionTier` enum 或 YAML tier 出现。

## Summary

可以进入下一轮文档修订，但建议先处理 P1-01。其余 P2/P3 都是小范围文档一致性修补，修完后这组 PR 文档基本可以作为 phase4 v4 后续实现合同使用。

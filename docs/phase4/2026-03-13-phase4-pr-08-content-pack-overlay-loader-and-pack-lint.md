> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-05-affix-cost-unique-artifact-and-loot-balance-lab.md`
> `docs/phase4/2026-03-13-phase4-pr-07-hidden-event-secret-zone-and-client-readability.md`
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`

# Phase 4 - PR-08 Content Pack Overlay Loader 与 Pack Lint

**阶段**: `Phase 4 / P4-C / P4-W5a`  
**优先级**: `P0`  
**前置条件**: `PR-05`, `PR-07` 完成  
**对应问题**: `Phase 4` 主文档已经冻结了 `ContentPackManifest / OverlayEntry` 的最小边界，但仓库还没有 pack loader、overlay precedence、pack lint 或 `contentPackHarness`。如果先做 sample pack 而不先做 loader/lint，最终只会得到一堆无法验证的示例数据。

---

## 1. 阶段目标

建立 `Phase 4` 的 content pack 正式装载路径，但保持边界严格停在 `content-only overlay`。

完成标准：

1. `ContentPackManifest / OverlayEntry / OverlayOp` 进入正式 runtime contract。
2. pack loader 的官方 runtime 主路径固定支持 `ADD / REPLACE`；`APPEND / DENY` 只要求 fixture/lint/harness 级可验证。
3. pack lint 能检查 schemaVersion、gameVersionRange、namespace、overlay 冲突和 i18n/visual/audio key。
4. `contentPackHarness` root alias 建立，并输出 pack-manifest + seed-list + report 三件套。
5. `Phase 4` 仅要求 sample pack 正式证明 `ADD` 路径；`REPLACE / APPEND / DENY` 至少要有 fixture/harness 级覆盖，不强制都落到正式 sample pack。

## 2. 当前问题

1. 当前仓库没有 pack loader，也没有 overlay precedence。
2. visual/audio manifest 和 i18n 目前只有 base game 路径，没有 pack-local 合并语义。
3. 若不先冻结 loader/lint，再做 sample pack，会把 schema 和资源路径一起拖着漂移。

### 2.1 本 PR 必须冻结的口径

1. pack metadata 继续使用 `manifest.yaml`。
2. pack-local `i18n / visual / audio` 复用现有 JSON bundle / manifest schema：
   - `i18n/*.json`
   - `visual/visual-manifest.json`
   - `audio/audio-manifest.json`
3. overlay precedence 固定为：`base game < dependency pack < current pack`。
4. pack 不允许改 `core` 规则常量、不允许注入脚本宿主。
5. `harnessSeeds`、双包 precedence 场景和 fixture 顺序不再放在 runtime manifest，而是移到 harness sidecar。
6. root alias `./gradlew contentPackHarness` 必须建立，并固定报告路径：
   - `tools/build/reports/phase4/content-pack/content-pack-summary.json`
   - `tools/build/reports/phase4/content-pack/content-pack-runs.jsonl`
7. `DENY` 保留在 contract 中，但 `Phase 4` 只要求它在 loader/lint/fixture 中可验证；不要求 sample pack 必须作为主要演示路径。

## 3. 范围与非目标

### 3.1 范围

1. loader、overlay precedence 和 fail-fast 策略。
2. pack lint。
3. `contentPackHarness` 骨架和 root alias。
4. pack-local JSON i18n / visual / audio schema 复用说明。

### 3.2 非目标

1. 不在本 PR 交付完整 sample pack 数据和资源。
2. 不在本 PR 引入 pack 热更新、目录自动发现或动态脚本。

## 4. 技术方案

### 4.1 Manifest 与 overlay

建议文件：

```text
game/src/main/kotlin/com/ktome/game/contentpack/*
tools/src/main/kotlin/com/ktome/tools/contentpack/*
```

核心结构：

```kotlin
@JvmInline
value class PackId(val value: String)

@JvmInline
value class RegistryId(val value: String)

data class ContentRef(
    val registry: RegistryId,
    val id: String,
)

data class PackDependency(
    val id: PackId,
    val versionRange: String,
)

enum class OverlayOp {
    ADD,
    REPLACE,
    APPEND,
    DENY,
}

data class OverlayEntry(
    val targetRef: ContentRef,
    val op: OverlayOp,
    val sourceFile: String,
    val fieldPath: String? = null,
    val mergePolicy: String? = null,
    val dedupeKey: String? = null,
)

data class ContentPackManifest(
    val id: PackId,
    val version: String,
    val schemaVersion: Int,
    val gameVersionRange: String,
    val namespace: String,
    val dependencies: List<PackDependency>,
    val overlays: List<OverlayEntry>,
    val localeBundles: List<String>,
    val visualManifest: String?,
    val audioManifest: String?,
)
```

```kotlin
data class DualPackScenario(
    val fixturePackId: PackId,
    val expectedOrder: List<PackId>,
    val expectedOps: List<OverlayOp> = emptyList(),
)

data class ContentPackHarnessSpec(
    val packId: PackId,
    val harnessSeeds: List<Long>,
    val dualPackScenarios: List<DualPackScenario> = emptyList(),
    val overlayContractVersion: Int = 1,
)
```

补充说明：

1. sidecar YAML 中 `packId / fixturePackId / expectedOrder` 仍以字符串序列化，但 loader/lint/harness 的 contract 语义按 `PackId` 处理。

### 4.2 JSON 资源边界

本 PR 正式口径：

1. `manifest.yaml` 只承载 pack 元数据。
2. i18n 继续复用现有 JSON bundle 格式。
3. visual/audio 继续复用当前 runtime JSON manifest schema，不创建第二套 YAML-only 表现资源路径。
4. loader 在内存中把 pack-local manifest merge 成与 canonical runtime manifest 同构的 resolver 输入。
5. `PR-09` 的最小可行切片固定为 `ADD` 主路径；其他 op 通过 fixture 或双包场景覆盖，避免把 sample pack 复杂度做成主线阻塞项。
6. `ContentPackHarnessSpec` 固定放在：
   - `tools/src/main/resources/fixtures/content-packs/<packId>.yaml`

### 4.3 pack lint

最小检查：

1. `schemaVersion` 匹配。
2. `gameVersionRange` 覆盖当前 base game。
3. `namespace` 唯一。
4. 未声明 `REPLACE` 的重复 ID 直接失败。
5. i18n / visual / audio key 能解析。
6. 至少存在一个双 pack fixture，覆盖 `REPLACE / APPEND / DENY` 中至少一种非 `ADD` 场景。
7. 必须输出可读失败诊断：
   - 缺失依赖
   - 依赖环
   - `versionRange` 冲突
   - namespace 冲突
   - 同优先级 pack 覆盖同一 target
   - 非白名单 `APPEND / DENY`

`APPEND / DENY` 的 `Phase 4` allowed targets：

1. `APPEND`
   - 默认只允许在 fixture/harness 路径使用
   - `Phase 4` 官方 runtime 不要求实现字段级 append
   - 当前建议白名单仅作为 fixture 诊断参考：`loot pool / zone pool / event pool`
2. `DENY`
   - 只允许指向明确标记为 optional 的内容
   - 不允许删除主线必需 entry
   - 默认只要求 fixture/harness 可验证

### 4.4 `contentPackHarness`

固定输出：

1. 所有 summary / report 统一带 `HarnessReportHeader`；其中至少包括 `activePackIds / activePackManifestVersions / seedList / overlayContractVersion`。
2. manifest 解析结果
3. overlay 冲突报告
4. i18n / visual / audio key 解析结果
5. registry 完整性报告
6. 固定 seed headless run 结果
7. 至少一个双 pack precedence 场景的解析结果
8. dependency / version / namespace / pack order 的失败诊断摘要
9. 当前列表描述的是 `contentPackHarness` 的业务载荷，不替代统一报告头。

## 5. 推荐改动面

### 5.1 `game`

1. 新建 content pack loader。
2. base registry 装配前后增加 overlay merge。
3. 为 visual/audio/i18n 提供 pack-local merge hooks。
4. official registry assembly 与 pack overlay merge 必须尽量共享 resolver / validation path，避免官方内容和外部 pack 走两套 loader 语义。

### 5.2 `tools`

1. 新建 pack lint 和 harness runner。
2. root `build.gradle.kts` 暴露 `contentPackHarness` alias。
3. 报告格式按 `pack-manifest + seed-list + report` 固定。
4. 夹具分层明确区分：
   - 正式 sample pack fixture：验证 `ADD`
   - 双 pack 或冲突 fixture：验证 `REPLACE / APPEND / DENY`
5. harness seed 与 fixture 顺序从 `ContentPackHarnessSpec` 读取，而不是从 runtime manifest 读取。

## 6. 测试与自证

### 6.1 必测行为

1. schemaVersion 不匹配时 fail-fast。
2. `REPLACE` 缺失的重复 ID 被 lint 拒绝。
3. pack 禁用后能回落到 base registry / base manifest。
4. i18n / visual / audio key 解析结果完整可追溯。
5. 至少一个双 pack fixture 覆盖非 `ADD` 语义，且 precedence 结果可解释。
6. `DENY` 即使没有正式 sample pack 消费者，也必须能在 fixture 中被 loader/lint 正确识别。
7. 缺失依赖、依赖环、`versionRange` 冲突和 namespace 冲突都能输出可读诊断，而不是只报“加载失败”。
8. 非白名单 `APPEND / DENY` 进入 runtime 路径时必须 fail-fast，而不是被静默接受。

### 6.2 自动化命令

```bash
./gradlew contentPackHarness
./gradlew clientSmoke
```

## 7. 出口门禁

1. loader/lint/harness contract 冻结。
2. pack-local JSON i18n / manifest 口径冻结，不再引入第二套格式。
3. `PR-09` 只需要填示例 pack，不需要再改 loader 语义。
4. `Phase 4` 的最小可行切片是“sample pack 验证 `ADD` + fixture 验证其他 op”，不是要求一个 sample pack 演完全部 overlay 语义。
5. runtime manifest 与 harness sidecar 的边界冻结，`tools` 元数据不再侵入 `game` runtime contract。
6. 官方 runtime merge 白名单已经收窄到 `ADD / REPLACE`，不会在 `Phase 4` 意外演变成通用 patch DSL。

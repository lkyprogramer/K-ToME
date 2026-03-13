> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-semantic-contracts-tile-and-i18n.md`
> `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`

# Phase 2 - PR-01 Serialization & Version Discipline

**阶段**: `Phase 2 / P2-W1`  
**优先级**: `P0`  
**前置条件**: `Phase 1` 全部完成  
**对应问题**: 现有 `Gson + SaveSnapshot + 裸字符串日志` 仍是 Phase 1 的临时结构。如果不先重建序列化和版本纪律，后续 schema、日志、i18n、Tile manifest 都没有稳定承载体。

---

## 1. 阶段目标

把存档主链从 `Gson` 迁移到 `kotlinx.serialization`，同时建立 Phase 2 的破坏式版本纪律。

完成标准：

1. `kotlinx.serialization` 成为唯一正式存档序列化方案。
2. 新增 `SaveContractVersion` 或等价版本结构。
3. 当前阶段存档支持稳定读写与 round-trip 测试。
4. 对旧 Phase 1 存档不做兼容承诺，但要给出明确错误提示。
5. 存档不再直接持久化 glyph、颜色和裸消息字符串。
6. save 版本与资源/manifest 版本边界明确分离，避免后续美术与音频管线混入玩家态协议。

## 2. 当前问题

1. `Gson` 不适合后续明确 schema 演进与跨模块语义稳定。
2. 当前 snapshot 混入了表现层字段，破坏 `core -> client` 边界。
3. 没有明确版本纪律时，Phase 2 以后每次结构变更都会制造脏状态。
4. 如果 save version、manifest version、style version 从一开始不分离，后续 Tile 和音频资源导入会直接污染存档协议。

### 2.1 本 PR 必须冻结的口径

1. Phase 2 开始只保证“当前阶段存档”可读写，不做历史兼容。
2. 存档只保存规则真值和必要 content key，不保存渲染态缓存。
3. 版本不匹配必须 fail fast，不做静默迁移。
4. `saveContractVersion`、`visualManifestVersion`、`audioManifestVersion`、`styleVersion` 不允许混成一个字段。

## 3. 范围与非目标

### 3.1 范围

1. `core.save` 序列化层
2. `SaveContractVersion`
3. 新存档主路径接线
4. 当前阶段存档 round-trip 测试
5. 错误版本提示与失败策略
6. 资源版本边界预埋

### 3.2 非目标

1. 不在本 PR 处理事件 token 化。
2. 不在本 PR 处理 Tile manifest 或 snapshot。
3. 不在本 PR 实现旧存档迁移工具。
4. 不在本 PR 建立完整资源管线，但必须先为后续资源版本合同留出稳定边界。

## 4. 技术方案

### 4.1 序列化主链迁移

建议文件：

```text
core/src/main/kotlin/com/ktome/core/save/SaveContractVersion.kt
core/src/main/kotlin/com/ktome/core/save/SaveSnapshot.kt
core/src/main/kotlin/com/ktome/core/save/SaveCodec.kt
core/src/test/kotlin/com/ktome/core/save/*
game/src/main/kotlin/com/ktome/game/session/FoundationGameSession.kt
```

冻结口径：

1. `SaveSnapshot` 只表达世界规则状态。
2. `SaveCodec` 必须显式声明序列化配置，不允许隐式默认。
3. `FoundationGameSession` 只能通过 `SaveCodec` 读写，不允许直接 new `Gson`。

### 4.2 版本纪律

建议模型：

1. `schemaVersion`
2. `saveContractVersion`
3. `buildMetadata` 或等价调试字段

冻结口径：

1. `saveContractVersion` 是玩家态存档协议版本。
2. manifest/style 版本不与 save 版本混用。
3. 当前版本以外的存档一律拒绝加载并给出明确提示。
4. `SaveContractVersion` 必须足够小且明确，不允许承载资产来源、style tag 或 atlas 信息。

### 4.3 需要从存档中剥离的字段

必须剥离：

1. glyph
2. color
3. 裸日志字符串
4. client-only UI 状态

允许保留：

1. entity id
2. 属性、资源、状态
3. zone / floor / objective 语义字段
4. content key / schemaVersion

### 4.4 SaveRoot 最小结构与 Phase 2 必备字段

建议保存根对象至少包含：

1. `saveContractVersion`
2. `worldSeed`
3. `currentZoneId`
4. `floorIndex`
5. `playerProfessionId`
6. `startingKitRefs`
7. `learnedTalentIds`
8. `resourcePools`
9. `statusInstances`
10. `inventoryItemRefs`
11. `questOrObjectiveState`
12. `worldEntities`
13. `monsterTemplateIds`
14. `bossEncounterState`

冻结口径：

1. `professionId`、`zoneId`、`itemId` 等必须都是稳定 content id。
2. `talentId`、`monsterTemplateId`、`bossEncounterId` 也必须都是稳定 content id。
3. 日志如果需要保留，只保存 token 化语义事件或截断后的只读记录，不把 UI 渲染结果写回存档。
4. 不允许把 atlas 名、region 名、音频文件路径作为玩家态存档字段。

### 4.5 与资源管线的版本边界

虽然完整资源管线在后续 PR 才建立，但本 PR 必须先冻结版本边界：

```kotlin
data class AssetVersionContract(
    val styleVersion: String,
    val visualManifestVersion: Int,
    val audioManifestVersion: Int,
    val assetPipelineVersion: Int,
)
```

约束：

1. `AssetVersionContract` 由 client/bootstrap 校验，不进入 `SaveSnapshot` 主体。
2. 资源版本变化不会自动抬升 `saveContractVersion`。
3. 如果资源版本不兼容，必须在启动或进局前失败，而不是到读档中途才爆炸。

### 4.6 回归设计

本 PR 必须建立至少三类 round-trip：

1. 空场景
2. 正在战斗的场景
3. 带资源、状态、物品、目标点的场景

## 5. 推荐改动面

### 5.1 `core`

1. 新建 `SaveContractVersion`
2. 替换存档 codec
3. 为 snapshot 去掉表现层字段

### 5.2 `game`

1. 调整 session 的 save/load 接线
2. 增加版本失败提示入口

### 5.3 `client`

1. 启动阶段预留 `AssetVersionContract` 校验入口
2. 为后续 manifest 不兼容提示保留统一错误展示路径

## 6. 测试与自证

### 6.1 必测类

1. `SaveCodecTest`
2. `SaveSnapshotRoundTripTest`
3. `SaveVersionCompatibilityTest`
4. `AssetVersionBoundaryTest`

### 6.2 必测行为

1. 当前版本存档可完整读写。
2. round-trip 前后关键规则状态一致。
3. 旧版本或非法版本存档会明确失败。
4. 存档中不再出现表现层字段。
5. save 版本与资源版本边界不互相污染。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.save.*"
./gradlew :game:test
./gradlew saveSmoke
./gradlew test
```

### 6.4 白盒验证

1. 启动游戏，开一局。
2. 在局中进行一次保存并立即读取。
3. 预期：
   - 角色位置、资源、状态一致
   - 不出现乱码、崩溃或空日志
4. 人工修改版本号后再读取，预期明确失败。
5. 人工制造 manifest 版本不匹配，预期在启动或进局前明确失败，而不是破坏 save/load。

## 7. 出口门禁

1. `Gson` 退出正式 save/load 主路径。
2. 当前阶段存档 round-trip 全绿。
3. 非法版本加载明确失败。
4. 存档不再混入 glyph、颜色、裸字符串日志。
5. save 版本与资源版本边界冻结完成。

## 8. 风险与止损

1. 如果 session 仍直接依赖具体 codec，优先抽象接口再继续改格式。
2. 如果 round-trip 发现 snapshot 仍有 client 字段，必须继续清理，不能先兼容。
3. 如果版本策略开始滑向“隐式修补”，必须立即收回到 fail-fast。

## 9. 当前状态

1. 本文是 `P2-W1` 的 PR 级执行文档。
2. 该 PR 完成后，Phase 2 才有稳定的存档与版本基线。

> 执行前必须先完整阅读并接受：
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
> `docs/2026-03-13-phase2-to-phase5-detailed-systems-design.md`

# Phase 2 - Semantic Contracts, Tile & i18n Short Run

**阶段**: `Phase 2`  
**版本目标**: `v0.2.x`  
**优先级**: `P0`  
**前置条件**: `Phase 1` 全部完成并可重复验证  
**对应问题**: Phase 1 已经能玩，但仍建立在 `100 能量 + 硬编码天赋/状态/日志/ASCII` 的临时语义上。如果不先迁移合同，后续职业、元素、Tile、音频和长局都只会继续堆在临时实现上。

---

## 1. 阶段目标

把 `Phase 1` 的最小可玩主线迁移为可长期演进的正式主线，并交付最小 Tile + 双语言 + 4 职业短局切片。

完成标准：

1. 把存档、事件、日志、资源、状态、技能 schema 迁移到正式语义合同。
2. 建立 `Tile + i18n + manifest + snapshot` 的正式消费路径。
3. 建立 `4 职业 + 4 zone + 24 怪 + 24 物品` 的最小短局。
4. 让 Phase 2 的关键验证入口可以稳定回归：
   - `save/load`
   - `locale-lint`
   - `contract-lint`
   - `golden screenshot`
   - `SoloClearLab v1`

## 2. 当前问题

1. `Gson + SaveSnapshot + 裸字符串日志` 无法支撑长期 schema 演进。
2. `TalentResolver`、状态、战斗和日志语义仍是 Phase 1 的局部硬编码。
3. 资源、Tile、音频、i18n 还没有“规则层 key -> 表现层解析”的正式边界。
4. 现有可玩路径仍依赖 ASCII/glyph 表现，无法进入正式内容生产节奏。

### 2.1 本阶段必须冻结的基础合同

1. `1000` 能量制取代 `100` 能量制。
2. `kotlinx.serialization` 成为主序列化方案。
3. `GameEvent / LogTokenEvent / callback registry` 成为规则到表现的正式事件主线。
4. `DamageType`、`ResourcePool`、`StatusEffectDef/StatusInstance`、`TalentDef V2` 完成首轮落地。
5. `RenderSnapshot`、`VisualManifest`、`AudioManifest` 成为客户端唯一正式消费入口。
6. 所有正式内容对象必须切到 `nameKey/descKey/visualKey/iconKey/audioProfile/schemaVersion`。
7. 不考虑旧存档兼容性，允许 Phase 2 对旧 Phase 1 存档做破坏式升级。

## 3. 范围与非目标

### 3.1 范围

1. 回合、战斗、资源、状态、技能、事件、存档的语义合同迁移。
2. `locale` 选择、key 化文本、日志 token 重渲染。
3. Tile 正式渲染路径、最小 HUD、最小背包/检视、golden screenshot 基线。
4. `4 职业 + 4 zone + 24 怪 + 24 物品` 的短局内容切片。
5. 美术/音频 manifest、最小资源导入流程与 lint。

### 3.2 非目标

1. 不在本阶段冻结最终战斗公式。
2. 不在本阶段实现完整长局世界分支。
3. 不在本阶段引入复杂 ProcGen、完整 content pack 平台或 Lua runtime。
4. 不在本阶段追求最终内容量，只做足以验证合同的最小闭环。

## 4. 技术方案

### 4.1 合同迁移顺序

Phase 2 必须固定按下面顺序推进，不能为了尽快出 Tile 而跳过前两步：

1. `序列化 / 版本纪律`
2. `事件 / 日志 / snapshot / manifest`
3. `schema / locale / key`
4. `Tile renderer / HUD / inventory shell`
5. `最小正式内容切片`
6. `4 职业短局与回归实验室`

原因：

1. 如果先做 Tile，再回补事件/manifest，客户端边界会很快失真。
2. 如果先堆内容，再回补 schema/i18n，后续改名、换图、换音会放大返工。

### 4.2 序列化、事件与日志迁移

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/save/*
core/src/main/kotlin/com/ktome/core/event/*
core/src/main/kotlin/com/ktome/core/log/*
game/src/main/kotlin/com/ktome/game/session/FoundationGameSession.kt
```

冻结口径：

1. `SaveSnapshot` 只保存语义字段，不再直接保存 ASCII glyph、颜色和裸消息字符串。
2. 新旧版本不兼容时必须显式报错，不做隐式 best-effort 修复。
3. 日志必须由 token + 参数驱动，表现层按当前 locale 渲染。
4. 所有规则侧关键行为都必须先变成语义事件，再由 client 消费。

### 4.3 Schema V2 与 i18n

建议文件与模块：

```text
game/src/main/resources/data/**/*.yaml
game/src/main/resources/i18n/*.json
game/src/main/kotlin/com/ktome/game/data/*
tools/src/main/kotlin/com/ktome/tools/lint/*
```

冻结口径：

1. 所有正式对象必须带 `schemaVersion`。
2. 正式内容不再新增裸中文或裸英文文本。
3. `locale-lint` 必须检查：
   - 缺 key
   - 多余 key
   - 占位符不一致
   - desc/name 漏配
4. `contract-lint` 必须检查：
   - schema 字段完整性
   - `visualKey/audioProfile` 可解析性
   - id 与 key 唯一性

### 4.4 RenderSnapshot 与 Tile 路径

建议文件与模块：

```text
core/src/main/kotlin/com/ktome/core/snapshot/*
client/src/main/kotlin/com/ktome/client/render/*
client/src/main/kotlin/com/ktome/client/ui/*
client/src/test/kotlin/com/ktome/client/golden/*
```

冻结口径：

1. `client` 不直接读取 world mutable state，只消费 `RenderSnapshot`。
2. Tile 渲染与 UI 状态必须全部能从 snapshot + manifest 解析。
3. 相同 seed、相同状态必须得到稳定 screenshot。
4. ASCII 可以保留为 debug 或 fallback，不再是正式玩家路径。

### 4.5 最小内容切片

内容最低标准：

1. `2 职业 + 1 zone + 1 Boss` 作为 `P2-B` 切片。
2. `4 职业 + 4 zone + 24 怪 + 24 物品` 作为 `P2-C` 出口。
3. 先只支持 `1` 个默认种族。
4. 资源与音画内容都以“足以验证合同”为目标，不按最终商业量级堆量。

建议职业顺序：

1. `Vanguard`
2. `Arcanist`
3. `Rogue`
4. `Templar`

原因：

1. 这四个职业刚好覆盖 `PHYSICAL / FIRE / COLD / HOLY / SHADOW / MENTAL` 的基础语义。
2. 适合验证 `Stamina / Mana / Positive / Momentum` 的资源差异。

### 4.6 资源生产管线的 Phase 2 最低要求

Phase 2 不是做完整资源工厂，而是建立正式骨架：

1. `AssetSpec -> Prompt/Import -> Cleanup -> Manifest -> Runtime Resolve`
2. `AudioSpec -> Cue Manifest -> AudioRouter`
3. `style-lint`、`asset-lint`、`audio-lint` 三条基础 lint

冻结口径：

1. `game` 只持有 `visualKey/audioProfile`。
2. 所有正式图片生成都必须引用风格圣经。
3. 缺图、缺音必须有 fallback 和显式错误日志。

当前离线脚本入口：

```bash
python3 scripts/asset-lint.py --plan assets-src/image/specs/phase2-asset-plan.yaml
python3 scripts/style-lint.py --plan assets-src/image/specs/phase2-asset-plan.yaml
python3 scripts/manifest-lint.py \
  --plan assets-src/image/specs/phase2-asset-plan.yaml \
  --manifest assets-src/image/manifests/phase2-visual-manifest.json
```

## 5. 推荐 PR / 工作包拆分

### P2-W1 Serialization & Version Discipline

1. 切 `kotlinx.serialization`
2. 建 `SaveContractVersion`
3. 主路径移除 GSON 依赖
4. 新存档读写单测

### P2-W2 Core Semantic Contracts

1. `1000` 能量调度迁移
2. `DamageType`、`ResourcePool`、基础状态扩展
3. 事件总线与 callback registry
4. `FoundationGameSession` 拆厚

### P2-W3 Locale & Schema V2

1. `nameKey/descKey` 全量切换
2. 首页 locale 选择
3. `locale-lint`
4. `contract-lint`

### P2-W4 Snapshot & Manifest

1. `RenderSnapshot`
2. `VisualManifest`
3. `AudioManifest`
4. golden screenshot 基线

### P2-W5 Minimal Tile Shell

1. Tile renderer
2. 最小 HUD
3. 背包/检视最小壳层
4. 中英双语 UI 走查

### P2-W6 Minimal Official Slice

1. 2 职业
2. 1 条 zone 链
3. 1 Boss
4. 最小资源集

### P2-W7 Short Run Expansion

1. 扩至 4 职业、4 zone
2. 24 怪、24 物品
3. `SoloClearLab v1`
4. Phase 2 出口收口

## 6. 测试与自证

### 6.1 必测模块

1. `core.save`
2. `core.event`
3. `core.snapshot`
4. `core.turn`
5. `game.data`
6. `game.i18n`
7. `client.render`
8. `tools.lint`

### 6.2 必测行为

1. 同一 world state 的 snapshot 哈希稳定。
2. save/load 往返后关键状态一致。
3. log token 能按 locale 正确重渲染。
4. `visualKey/audioProfile` 缺失时 lint 失败。
5. 首页切语言后，开局和读当前阶段存档都能正确显示。
6. 4 职业至少各完成一次默认短局。

### 6.3 自动化命令

Phase 2 必须建立或补齐以下入口：

```bash
./gradlew test
./gradlew :core:test
./gradlew :game:test
./gradlew localeLint
./gradlew contractLint
./gradlew goldenScreenshot
./gradlew soloClearLab
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

### 6.4 白盒验证

固定白盒步骤：

1. 启动 client。
2. 首页切到中文，新开一局，确认菜单、HUD、日志、背包标题都走 key 渲染。
3. 用默认 seed 跑通 `P2-B` 切片，确认 Tile、交互、Boss、结算成立。
4. 切到英文，重新进入同阶段存档，确认文本重渲染且不依赖旧字符串。
5. 用四个职业各跑一次 `SoloClearLab` 默认脚本，确认都能完成短局。

## 7. 出口门禁

1. `4 职业 + 4 zone + 24 怪 + 24 物品` 全部可进正式路径。
2. `save/load`、`locale-lint`、`contract-lint`、`golden screenshot`、`SoloClearLab` 全绿。
3. 正式玩家路径不再依赖 ASCII。
4. 新事件、日志、snapshot、manifest 有稳定哈希或稳定 golden。
5. `core` 关键改动都有确定性测试和 coverage gate。

## 8. 风险与止损

1. 如果 `FoundationGameSession` 继续膨胀，必须优先拆分会话职责，暂停继续堆 UI。
2. 如果 i18n key 与 schema V2 没完成，不允许继续新增正式内容。
3. 如果 golden screenshot 不稳定，必须先稳定 snapshot/manifest，再继续补 Tile 表现。
4. 如果短局内容不足以支撑四职业验证，优先补实验室与 seed harness，不优先补资源量。

## 9. 当前状态

1. 总纲与详细系统设计已完成。
2. 本文作为 `Phase 2` 的执行文档，可直接用于 PR 切分与任务拆解。
3. 当前尚未开始实际代码落地，验证命令和 lint 入口仍需在实现过程中建立。

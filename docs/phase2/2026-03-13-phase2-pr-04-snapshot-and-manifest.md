> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-pr-03-locale-and-schema-v2.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 2 - PR-04 Snapshot & Manifest

**阶段**: `Phase 2 / P2-W4`  
**优先级**: `P0`  
**前置条件**: `P2-W2`、`P2-W3` 完成  
**对应问题**: 没有 `RenderSnapshot` 与 manifest 之前，client 仍会倾向直接读规则态或直接按文件路径找资源，这会立即破坏 Phase 2 之后的客户端边界。

---

## 1. 阶段目标

建立 `RenderSnapshot`、`VisualManifest`、`AudioManifest` 和 `golden screenshot` 的正式链路。

完成标准：

1. `client` 只通过 snapshot 消费规则态。
2. 资源只通过 manifest key 解析，不允许正式路径直读文件路径。
3. 建立稳定的 screenshot golden 基线。
4. `contract-lint` 能校验 manifest key 可解析。
5. 最小图像/音频 pipeline、style tag、lint 与 runtime load 策略在本 PR 冻结。

## 2. 当前问题

1. 现有 client 更接近直接消费 world state 的薄壳，难以进入正式 Tile 路径。
2. 如果没有 manifest，资源命名和导入会快速失控。
3. 没有 golden screenshot，UI/Tileset 漂移很难回归。
4. 如果不在 Phase 2 前半段先建立资源 pipeline，后面的职业、地图、怪物和 UI 只会用临时素材硬接。

### 2.1 本 PR 必须冻结的口径

1. snapshot 是 `core -> client` 的唯一正式视图模型。
2. manifest 是 runtime 资源解析的唯一真源。
3. golden screenshot 基线必须建立在固定 seed、固定 locale、固定 resolution 上。
4. 正式图片生成必须绑定统一 style tag，正式音频必须绑定 cue family。

## 3. 范围与非目标

### 3.1 范围

1. `RenderSnapshot`
2. `VisualManifest`
3. `AudioManifest`
4. golden screenshot harness
5. manifest 解析与 fallback
6. 最小资源管线 bootstrap
7. asset/style/audio/manifest lint

### 3.2 非目标

1. 不在本 PR 完成完整 Tile 视觉 polish。
2. 不在本 PR 追求大量正式资源。
3. 不在本 PR 完成全部 UI 壳层。
4. 不在本 PR 大规模生产所有 Phase 2 资源，但必须把资源“从 spec 到 runtime”的链路打通。

## 4. 技术方案

### 4.1 RenderSnapshot

建议文件：

```text
core/src/main/kotlin/com/ktome/core/snapshot/*
client/src/main/kotlin/com/ktome/client/render/*
```

冻结口径：

1. snapshot 只包含 client 真正需要的渲染语义。
2. snapshot 不回流 gameplay 逻辑。
3. 相同 world state 下 snapshot 哈希稳定。
4. `RenderSnapshot` 至少包含：
   - `mapCells`
   - `props`
   - `actors`
   - `overlays`
   - `uiState`
   - `logEvents`
5. `overlays` 在 Phase 2 就必须预留最小 telegraph / Boss warning 语义，至少包含：
   - `previewTurns`
   - `dangerLevel`
   - `shape`
   - `sourceAbilityId`
6. Phase 2 的 `overlay` 只服务最小 Boss warning 与 Layer 2 simple scripted AI 的 `TELEGRAPH` 动作，不在本 PR 预实现 Phase 3 的完整 telegraph 系统、伤害预估和复杂多阶段预警。

### 4.2 VisualManifest / AudioManifest

建议文件：

```text
client/src/main/resources/manifests/*
client/src/main/kotlin/com/ktome/client/assets/*
assets-src/image/specs/*
assets-src/audio/specs/*
tools/asset-pipeline/*
tools/audio-pipeline/*
scripts/generate_assets_gemini.py
scripts/generate_assets.sh
```

冻结口径：

1. `visualKey/audioProfile` 只能通过 manifest resolve。
2. 缺资源必须有 fallback 和显式错误日志。
3. atlas/region 名称必须稳定。
4. `game` 和 `core` 不能直接感知 raw 资产路径。

### 4.3 AssetSpec / AudioSpec Bootstrap

本 PR 必须建立最小 pipeline 骨架：

```text
assets-src/
  style/
  image/specs/
  image/raw/
  image/processed/
  image/manifests/
  audio/specs/
  audio/raw/
  audio/cleaned/
  audio/manifests/
```

至少支持以下 spec：

1. `AssetSpec`
2. `AudioSpec`
3. `VisualManifestEntry`
4. `AudioCueManifestEntry`
5. `assets-src/image/specs/phase2-asset-plan.yaml`

冻结口径：

1. 任何正式资源都必须先有 spec，再有导入结果。
2. 图像 prompt 必须显式引用 [2026-03-13-art-style-bible.md](../2026-03-13-art-style-bible.md) 对应 style tag。
3. provider 只是生成来源，不是风格定义者。
4. Phase 2 图片生成统一走：
   - `scripts/generate_assets.sh`
   - `scripts/generate_assets_gemini.py`
   - `assets-src/image/specs/phase2-asset-plan.yaml`
5. 没有显式提供 `GEMINI_API_KEY` 时，生成必须直接失败，不允许 placeholder fallback 或 provider 回退。

### 4.4 Lint 与 Smoke

本 PR 必须建立以下入口骨架：

1. `asset-lint`
2. `style-lint`
3. `audio-lint`
4. `manifest-lint`

至少检查：

1. `visualKey/audioProfile` 是否都能解析
2. atlas region/pivot/footprint 是否齐全
3. cue family 是否合法
4. style tag 和 raw 来源是否有记录

### 4.5 运行时加载策略

运行时必须至少分三层加载：

1. `Bootstrap Load`
   - locale bundle
   - 主菜单 UI
   - 基础字体
2. `Session Load`
   - 当前 tileset atlas
   - 基础 actor/icon atlas
   - 当前 zone ambience
3. `Warm Cache`
   - Boss 预警资源
   - 稀有 portrait
   - 高价值 VFX

冻结口径：

1. 进入局内时不允许同步扫描 raw 资产目录。
2. manifest 在启动时一次校验。
3. `missing_visual` 和静默音频 fallback 必须存在。
4. zone 的 `tilesetKey`、`ambientProfile`、状态 icon、Boss 预警资源都必须可由 manifest 解析。

### 4.6 Golden Screenshot

必须固定：

1. seed
2. locale
3. resolution
4. font
5. UI scale

本 PR 只要求建立首批基线：

1. 主菜单
2. 局内默认 HUD
3. 背包/检视最小壳层

### 4.7 Snapshot 生成时机

本 PR 必须先固定最小生成时机：

1. session load 完成后
2. 玩家行动结算后
3. 一整轮 AI 结算后
4. save/load 往返后
5. zone 切换后

### 4.8 Phase 2 最小资源套件

本 PR 虽不要求全部成品资源到位，但 pipeline 必须能承载以下对象族：

1. 4 zone 的基础 Tile family
2. 4 基础职业的主体 sprite 或 portrait
3. 核心 skill/status/item icon
4. `ui / footstep / melee / spell / monster / interactable / ambience / music` 基础 cue 家族
5. 六通道伤害与首批状态的 icon family，避免 `P2-W5` 再回头补 manifest 结构

## 5. 推荐改动面

### 5.1 `core`

1. snapshot 生成接口
2. snapshot 哈希辅助

### 5.2 `client`

1. manifest loader
2. asset resolve
3. golden screenshot harness

### 5.3 `tools`

1. screenshot baseline 管理
2. manifest key 校验接线
3. asset/style/audio/manifest lint 入口

## 6. 测试与自证

### 6.1 必测类

1. `RenderSnapshotTest`
2. `ManifestResolveTest`
3. `GoldenScreenshotHarnessTest`
4. `AssetSpecSchemaTest`
5. `AudioCueManifestTest`
6. `RenderSnapshotContractTest`

### 6.2 必测行为

1. 相同状态下 snapshot 哈希稳定。
2. `visualKey/audioProfile` 正确解析。
3. 缺资源时 fallback 生效且有错误日志。
4. screenshot golden 在无改动时稳定。
5. style/asset/audio/manifest lint 能对非法输入失败。
6. snapshot 至少能完整表达 map/actor/ui/log 这五类 client 所需信息。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.snapshot.*"
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
./gradlew goldenScreenshot
./gradlew contractLint
```

当前离线脚本入口：

```bash
python3 scripts/asset-lint.py --plan assets-src/image/specs/phase2-asset-plan.yaml
python3 scripts/style-lint.py --plan assets-src/image/specs/phase2-asset-plan.yaml
python3 scripts/manifest-lint.py \
  --plan assets-src/image/specs/phase2-asset-plan.yaml \
  --manifest assets-src/image/manifests/phase2-visual-manifest.json
```

### 6.4 白盒验证

1. 启动 client，进入主菜单。
2. 检查主菜单和局内 HUD 的视觉输出。
3. 人为打断一个 `visualKey`，确认 fallback 与错误提示出现。
4. 人为放入一个未标 style tag 的正式图片 spec，确认 `style-lint` 失败。
5. 切换到带 `ambientProfile` 和 Boss 预警的场景，确认 snapshot 和 manifest 都能解析对应资源。

## 7. 出口门禁

1. `client` 正式路径只消费 snapshot。
2. manifest 解析生效。
3. 首批 golden screenshot 基线建立。
4. key 缺失能被 lint 与运行时同时发现。
5. 最小图像/音频 pipeline bootstrap 已建立，可承载后续职业、怪物、地图资源创建。
6. `RenderSnapshot` 合同字段和生成时机冻结完成。
7. `scripts/asset-lint.py`、`scripts/style-lint.py`、`scripts/manifest-lint.py` 已成为正式离线门禁入口。

## 8. 风险与止损

1. 如果 client 仍需要回读 world mutable state，必须继续补 snapshot，而不是绕过。
2. 如果 manifest 开始退化为文件路径表，必须及时抽回语义 key。
3. 如果 golden 过于不稳定，优先稳定环境变量和 snapshot，不优先改图。

## 9. 当前状态

1. 本文是 `P2-W4` 的 PR 级开发文档。
2. 该 PR 完成后，Tile 与资源正式路径才有稳定消费边界。

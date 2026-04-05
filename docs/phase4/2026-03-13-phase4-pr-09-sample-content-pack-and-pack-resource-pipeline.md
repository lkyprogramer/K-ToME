> 执行前必须先完整阅读并接受：
> `docs/phase4/2026-03-13-phase4-pr-08-content-pack-overlay-loader-and-pack-lint.md`
> `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`
> `docs/phase4/2026-03-13-phase4-cross-cutting-contracts.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`

# Phase 4 - PR-09 Sample Content Pack 与 Pack Resource Pipeline

**阶段**: `Phase 4 / P4-C / P4-W5b`  
**优先级**: `P0`  
**前置条件**: `PR-08` 完成  
**对应问题**: `PR-08` 只冻结了 loader/lint/harness contract，还没有真实的示例 pack 去证明 overlay、pack-local i18n/visual/audio、fixed-seed harness 和 client 可见性都能工作。没有 sample pack，`Phase 4` 的数据包能力仍然停留在接口层。

---

## 1. 阶段目标

交付一个真实可加载、可 lint、可跑 headless harness、可做 client 白盒验证的 sample pack，并把 pack 资源生成计划接到现有资源管线。

完成标准：

1. `sample.flooded_relics` pack 正式进入仓库。
2. pack-local i18n / visual / audio 使用 `PR-08` 已冻结的 JSON schema。
3. `contentPackHarness` 能以固定 seed 跑 sample pack。
4. client 能实际看见 sample pack 内容，而不是只在 lint 中通过。
5. 至少存在一个第二夹具用于 precedence / conflict 验证；它可以是极简 pack fixture 或模拟双包输入，不要求做成完整资源包。
6. harness seed、dual-pack 顺序和预期 op 结果固定放在 `tools/src/main/resources/fixtures/content-packs/*.yaml`，不回流到 runtime pack 目录。

## 2. 当前问题

1. 没有示例 pack，无法验证 overlay precedence 和 key 解析是否真的可用。
2. 没有 pack 资源计划，容易让 sample pack 变成“只有 YAML/JSON，没有可见内容”的空壳。
3. 若 sample pack 不走现有 runtime JSON manifest schema 和资源管线，会立即出现第三套格式。

### 2.1 本 PR 必须冻结的口径

1. sample pack id 固定为 `sample.flooded_relics`。
2. 示例 pack 只扩内容层，不改 `core` 规则常量。
3. pack-local i18n / visual / audio 一律使用 JSON。
4. pack 资源仍需通过现有 `assetLint / manifestLint / audioLint` 或 pack-local 等价校验。
5. 第二夹具只承担 loader/harness precedence 验证，不承担完整美术/音频演示义务。

## 3. 范围与非目标

### 3.1 范围

1. 示例 pack 的 manifest、data、i18n、visual、audio。
2. `contentPackHarness` 的 official sample pack fixture 与固定 seed corpus。
3. client 可见性白盒步骤。
4. pack 资源生成计划。
5. 一个最小 dual-pack fixture 或模拟双包 precedence 夹具。

### 3.2 非目标

1. 不把 sample pack 做成完整 Mod SDK 样板。
2. 不在本 PR 引入复杂 pack 依赖链或多层 overlay 生态；dual-pack 第二夹具只允许作为测试夹具存在。
3. 不把 official sample pack 和 loader 回归夹具 pack 混成一个目录职责。

## 4. 技术方案

### 4.1 pack 目录

建议目录：

```text
examples/content-packs/sample.flooded_relics/
  manifest.yaml
  i18n/
    zh-CN.json
    en-US.json
  visual/
    visual-manifest.json
  audio/
    audio-manifest.json
  data/
    events/*.yaml
    items/*.yaml
    secret-zones/*.yaml
    elites/*.yaml
```

### 4.2 示例内容范围

建议至少包含：

1. `1` 个 hidden event
2. `1` 个 secret zone
3. `1` 个 unique 或 artifact 奖励
4. `1` 条玩家可直接感知的主扩展路径
5. `3` 个固定 harness seed
6. `1` 个双 pack precedence 夹具：
   - 形式一：`examples/content-packs/sample.flooded_relics_override_fixture/`
   - 形式二：`contentPackHarness` 内联构造的模拟第二包
   - 两者二选一，不要求都实现

说明：

1. `official sample pack` 的目标是证明玩家价值和 runtime 可见性，不是覆盖所有 overlay 语义。
2. `fixture pack` 的目标是证明 loader/lint/harness 边界，不要求具备完整美术/音频。

### 4.3 harness 验证

`contentPackHarness` 必须同时验证：

1. pack 开启时 overlay 生效
2. pack 禁用时回落到 base
3. `tools/src/main/resources/fixtures/content-packs/<packId>.yaml` 中声明的 seed 下 headless run 全通过
4. pack-local i18n / visual / audio key 可解析
5. 双 pack precedence 或 conflict 场景可稳定复现，且至少覆盖一种非 `ADD` 语义；`REPLACE / APPEND / DENY` 的工程验证以第二夹具或模拟双包场景为主，不强制都落到 official sample pack 主入口

## 5. 推荐改动面

### 5.1 `examples/content-packs`

1. 新增 `sample.flooded_relics`。
2. 所有资源 key 必须带 pack namespace。
3. 若采用目录型第二夹具，必须显式标注其用途是 `fixture-only`，避免被误认为正式 sample pack。
4. official sample pack 的资源最终落在 pack 自己目录内，不回写或污染 base canonical manifest 目录。

### 5.2 `tools`

1. `contentPackHarness` 增加 sample pack fixture。
2. pack run 报告复用全局 reproducibility contract，至少输出单元素形式的 `activePackIds / activePackManifestVersions`，并保留 `seedList / overlayContractVersion`。
3. 增加第二夹具或模拟双包输入，用于 precedence / conflict 回归。
4. 读取 `tools/src/main/resources/fixtures/content-packs/<packId>.yaml`，而不是从 runtime manifest 读取测试字段。

### 5.3 `client`

1. 用 sample pack 进入一局，确认新增内容可见。
2. 若需要截图对比，补 `goldenScreenshot` 或手动白盒步骤。
3. 无论第二夹具是目录还是模拟输入，都不要求 client 做完整白盒展示；它只需要在 harness 中证明 precedence 正确。

### 5.4 `tools / white-box` 补充改造

1. `PR-08` 冻结好的 `whiteBoxContentPack` contract 在本 PR 必须被真实 sample pack 与 precedence fixture 消费，不允许只用 prose 假设“理论上可接”。
2. `contentPackHarness` 和 `whiteBoxContentPack` 至少要覆盖：
   - official sample pack 开启
   - official sample pack 禁用回落
   - 第二夹具或模拟双包 precedence
3. sample pack 相关 artifact 至少包括：
   - pack-local manifest resolve 结果
   - merged i18n / visual / audio key 摘要
   - fixed-seed headless run 摘要
   - precedence fixture 解析结果
4. `phase4Report` 中 content pack 侧的 AI 主入口默认指向 `whiteBoxContentPack`，而不是只让 AI grep `contentPackHarness` 的控制台结果。

## 6. 测试与自证

### 6.1 自动化命令

```bash
./gradlew contentPackHarness
./gradlew whiteBoxContentPack
./gradlew whiteBoxVerify
./gradlew phase4Report
./gradlew clientSmoke
./gradlew goldenScreenshot
```

### 6.2 白盒验证

1. 装载 sample pack 开新局，确认新增内容真实出现。
2. 关闭 sample pack 重开一局，确认回落到 base。
3. 人工确认新增内容的名称、图标、音频和日志文本全部带 namespace 且可读。
4. 若第二夹具为目录型 pack，再跑一次双包顺序验证；若为模拟输入，则保留 `contentPackHarness` 报告截图或摘要作为证据。
5. 确认 official sample pack 目录和 fixture pack 目录职责清晰，没有把测试专用资源误放进官方演示 pack。

### 6.3 统一白盒框架验证

1. `whiteBoxContentPack` 必须能把 sample pack 与 fixture pack 的结果区分建模，而不是混在一个“全部通过”的 summary 里。
2. AI case 读取时至少要能反查：
   - `packId`
   - fixture id
   - `activePackIds`
   - `activePackManifestVersions`
   - seed 或 harness case id
3. precedence / conflict 的失败诊断必须以结构化字段进入 case facts / artifacts，不接受只留一张截图或一句“人工确认有问题”。

## 7. 资源生成计划

### 7.1 图片

1. 计划文件：`assets-src/image/specs/phase4-pr09-gemini-plan.yaml`
2. 覆盖对象：
   - `sample_flooded_relics.zone.*`
   - `sample_flooded_relics.prop.*`
   - `sample_flooded_relics.item.*`
3. 报告文件：
   - `assets-src/image/manifests/phase4-pr09-generation-report.jsonl`
   - `assets-src/image/manifests/phase4-pr09-processing-report.jsonl`
4. `*-gemini-plan.yaml` 是复用既有图片生成计划文件的命名约定，不代表 pack 运行时需要识别新的资源格式。

### 7.2 音频

1. 计划文件：`assets-src/audio/specs/phase4-pr09-audio-plan.yaml`
2. 覆盖对象：
   - `sample_flooded_relics.audio.zone.*`
   - `sample_flooded_relics.audio.interactable.*`
   - `sample_flooded_relics.audio.item.*`
3. 报告文件：
   - `assets-src/audio/manifests/phase4-pr09-processing-report.jsonl`

### 7.3 资源与 manifest 约束

1. pack-local `visual-manifest.json` 和 `audio-manifest.json` 必须复用 canonical runtime schema。
2. pack-local i18n JSON 必须复用 base `LocalizationBundle` 的 key/value 结构。
3. sample pack 的 visual/audio/i18n merge 结果必须继续喂给现有单一 resolver 接口；harness 负责验证 merge 后结果，不能为 pack 单独新起第二套 resolver。
4. `gemini` 仅表示图片 plan 文件命名约定，不能被实现方当成 pack schema 或 manifest version 的一部分。
5. `harnessSeeds`、dual-pack fixture 顺序和预期 op 结果只允许出现在 `tools/src/main/resources/fixtures/content-packs/*.yaml` / `ContentPackHarnessSpec`，不允许回流到 runtime manifest。
6. `packId / namespace / 资源 key prefix` 的规则固定为：
   - `packId = sample.flooded_relics`
   - `namespace = sample_flooded_relics`
   - pack-local visual/audio/i18n key prefix 必须统一使用 `sample_flooded_relics.*`
   - 点号 pack id 到下划线 namespace 的转换规则由 loader/lint 统一执行，不能在资源脚本里各自发明

## 8. 出口门禁

1. sample pack 可被独立装载、lint、harness 验证和 client 白盒验证。
2. pack 资源计划与现有 JSON manifest / lint / process 管线完全对齐。
3. `Phase 4` 的 content pack 能力从“接口存在”升级为“示例可用”。
4. precedence / conflict 至少有一个第二夹具或模拟双包场景可以复现，避免只验证单包 happy path。
5. official sample pack 与 regression fixture pack 的职责分离清晰，不再混成一个“什么都想证明”的大而杂样例。
6. `whiteBoxContentPack` 已能在 sample pack 与 precedence fixture 上产出 AI 可读报告，不再依赖人工翻 pack 目录定位问题。

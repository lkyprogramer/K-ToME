> 生成依据：
> `docs/review/phase3/v3/2026-03-30-phase3-v3-pr-roadmap.md`
> `docs/review/phase3/v3/2026-03-30-phase3-v3-pr-16-river-and-crystal-runtime-activation.md`
> `docs/review/phase3/v3/2026-03-30-phase3-v3-pr-17-abyssal-ward-and-finale-runtime-activation.md`
> `docs/review/phase3/v3/2026-03-30-phase3-v3-pr-19-reward-presentation-and-late-run-reliquary-spend.md`
> `docs/review/phase3/2026-03-26-phase3-pr-09-asset-batch-generation-checklist.md`
> `docs/review/phase3/2026-03-27-phase3-pr-09-asset-namespace-table.md`

# Phase 3 V3 PR-15 ~ PR-20 Asset And Audio Assessment

**阶段**: `Phase 3 / v3 follow-up / asset-audio companion`  
**目标**: 判断 `PR-15 ~ PR-20` 是否需要补充美术或音频资源，并把真正需要补的部分收敛成可以直接走现有生产管线的资源方案。  

## 1. 直接结论

| PR | 是否需要新增 raw 美术 | 是否需要新增 raw 音频 | 结论 |
| --- | --- | --- | --- |
| `PR-15` | 否 | 否 | 纯 gate / harness 收紧，不应掺资源包 |
| `PR-16` | 是 | 是 | 必须补 `river / crystal` 的 zone/interactable/runtime identity pack |
| `PR-17` | 是 | 是 | 必须补 `abyssal_temple / abyssal_heart` 的终线 identity pack |
| `PR-18` | 否 | 否 | 现阶段是 breakpoint / affix payoff 收口，不新增 talent id，不应带资源扩容 |
| `PR-19` | 否 | 否 | 奖励 presentation 先走 text/badge/log；late-run 节点复用 `PR-17` 的 reliquary 资源 |
| `PR-20` | 否 | 否 | AI/profile 收口，不应引入额外资源噪音 |

结论重点：

1. 真正需要补资源的只有 `PR-16` 和 `PR-17`。
2. `PR-16 / PR-17` 不只是“可以补资源”，而是**应该补最小 identity pack**。
3. 不补这两组资源，late-zone 和 finale zone 即使 runtime 成立，玩家体感仍会被当前的复用口径压扁。
4. `PR-19` 不应再另开一套 reliquary 美术/音频；应直接复用 `PR-17` 的节点资源。

## 2. 证据锚点

当前资源复用已经直接压缩了这几个 zone 的身份：

1. [zones/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/zones/index.yaml) 中：
   - `underground_river` 仍使用 `zone.grey_gate_depths.visual` / `zone.grey_gate_depths.icon` / `audio.zone.grey_gate_depths` / `ambient.grey_gate_depths`
   - `crystal_cavern` 仍使用 `zone.grey_gate_depths.visual` / `zone.grey_gate_depths.icon` / `audio.zone.grey_gate_depths` / `ambient.grey_gate_depths`
   - `abyssal_temple` 与 `abyssal_heart` 也仍复用 `grey_gate_depths`
2. [interactables/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/interactables/index.yaml) 中：
   - `crystal_resonance_node` 复用 `prop.ritual_altar` + `audio.interactable.open`
   - `river_ferry_anchor` 复用 `prop.armory_gate` + `audio.interactable.open`
   - `temple_ward_reliquary` / `heart_ward_focus` 都复用 `prop.ritual_altar` + `audio.interactable.open`
3. [visuals/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/visuals/index.yaml) 与 [audio/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/audio/index.yaml) 中：
   - 目前没有 `zone.underground_river.*`
   - 没有 `zone.crystal_cavern.*`
   - 没有 `zone.abyssal_temple.*`
   - 没有 `zone.abyssal_heart.*`
   - 没有 `prop.river_ferry_anchor` / `prop.crystal_resonance_node` / `prop.temple_ward_reliquary` / `prop.heart_ward_focus`
   - 没有对应的 `ambient.*` 与 `audio.zone.*` 新 key

这意味着：

1. `PR-16 / PR-17` 如果只补 runtime，不补最小 identity pack，zone 身份仍然被旧资源复用口径掩盖。
2. 这种问题不会被代码测试发现，但会直接损害“后半段终于进了新地方”的玩家感受。

## 3. 资源边界与冻结口径

本 companion 只允许补**最小 identity pack**，不把 `PR-16 / PR-17` 膨胀成资源大包。

统一冻结：

1. 继续沿用现有 `Phase 3 follow-up` 资源生产管线：
   - 图片：`scripts/generate_assets.sh -> scripts/process_assets.py`
   - 音频：raw 导入后走 `scripts/process_audio.py`
2. 新图片计划文件统一放：

```text
assets-src/image/specs/phase3-v3-pr16-gemini-plan.yaml
assets-src/image/specs/phase3-v3-pr17-gemini-plan.yaml
```

3. 新音频计划文件统一放：

```text
assets-src/audio/specs/phase3-v3-pr16-audio-plan.yaml
assets-src/audio/specs/phase3-v3-pr17-audio-plan.yaml
```

4. 新 runtime 资源继续落到：

```text
client/src/main/resources/phase3/p3-followup/
client/src/main/resources/audio/ambient/
client/src/main/resources/audio/interactable/
```

5. `telegraph` 通用视觉与 warning cue 继续复用：
   - `vfx.telegraph.warning.sigil_01`
   - `vfx.boss.warning.sigil_01`
   - `audio.boss.warning`
6. 不为 `PR-19` 另开新的 reliquary raw pack。
7. 真正启动图片批量生成前，执行方必须先向用户索取 `GEMINI_API_KEY`。

## 4. PR-16 必补资源包

### 4.1 为什么必须补

`PR-16` 的目标是把 `underground_river / crystal_cavern` 做成“有自己决策压力的 late-zone”。  
如果仍继续复用 `grey_gate_depths` 的 zone card、环境音和交互 prop，玩家只会得到“机制变了点，但还是同一个洞窟”的感受。

所以 `PR-16` 必补：

1. `2` 个 zone card（river / crystal）
2. `2` 个 interactable prop（anchor / resonance node）
3. `2` 个 mechanic vfx（current lane / crystal shard pressure）
4. `2` 组 ambience + zone cue
5. `2` 个 interactable cue

### 4.2 PR-16 视觉资产清单

| Asset ID | Category | Visual Key | Runtime Path | 用途 | 生成主题 |
| --- | --- | --- | --- | --- | --- |
| `phase3_v3_pr16_zone_underground_river_icon` | `icon_quest` | `zone.underground_river.icon` | `phase3/p3-followup/zone_underground_river_icon.png` | world map / route selection | 地下暗河的锚链、旧木渡台、湿冷石壁与逆流水纹 |
| `phase3_v3_pr16_zone_underground_river_visual` | `portrait` | `zone.underground_river.visual` | `phase3/p3-followup/zone_underground_river_visual.png` | zone card / inspect | 阴湿地底河道、断裂栈桥、锁链锚座、冷蓝水面反光 |
| `phase3_v3_pr16_zone_crystal_cavern_icon` | `icon_quest` | `zone.crystal_cavern.icon` | `phase3/p3-followup/zone_crystal_cavern_icon.png` | world map / route selection | 多面晶簇、封印节点、冷白共鸣裂隙 |
| `phase3_v3_pr16_zone_crystal_cavern_visual` | `portrait` | `zone.crystal_cavern.visual` | `phase3/p3-followup/zone_crystal_cavern_visual.png` | zone card / inspect | 洞窟内部的高耸晶柱、折射冷光、裂开的共鸣节点 |
| `phase3_v3_pr16_prop_river_ferry_anchor` | `prop_interactable` | `prop.river_ferry_anchor` | `phase3/p3-followup/prop_river_ferry_anchor.png` | objective / support node | 沉重锈蚀锚座、粗链、木制锁扣、被水浸透的踏板 |
| `phase3_v3_pr16_prop_crystal_resonance_node` | `prop_interactable` | `prop.crystal_resonance_node` | `phase3/p3-followup/prop_crystal_resonance_node.png` | objective / support node | 安在石座上的晶簇共鸣节点、封印铜环、冷白脉冲 |
| `phase3_v3_pr16_vfx_zone_effect_current_lane_01` | `tile_decal` | `vfx.zone.effect.current_lane_01` | `phase3/p3-followup/vfx_zone_effect_current_lane_01.png` | current lane overlay | 斜向水流纹与碎泡沫，方向性清晰，可小图辨识 |
| `phase3_v3_pr16_vfx_zone_effect_crystal_shard_01` | `tile_decal` | `vfx.zone.effect.crystal_shard_01` | `phase3/p3-followup/vfx_zone_effect_crystal_shard_01.png` | shard pressure overlay | 晶刺从地面穿出前的裂纹与冷白尖簇轮廓 |

### 4.3 PR-16 图片 prompt 约束

所有 `PR-16` 图片继续沿用 `ktome-middle-fantasy-painterly-tile-v1`：

1. 禁止 anime / sci-fi / neon / 玻璃未来感。
2. `zone` 资产强调“湿冷地底 / 晶体封印”与可读轮廓，不做大场景堆砌。
3. `prop_interactable` 必须有明显可交互 affordance：
   - `river_ferry_anchor` 看得出“可固定 / 解锁 / 放下”
   - `crystal_resonance_node` 看得出“可触碰 / 调谐 / 稳定”
4. `tile_decal` 必须服务玩法辨识，不画成插画：
   - `current_lane` 强调方向感
   - `crystal_shard` 强调即将刺出的危险感
5. 所有图片 spec 必须设置 `geminiKeyRequired: true`。

### 4.4 PR-16 音频清单

| Key | Cue Family | Source Path | 作用 | 声音方向 |
| --- | --- | --- | --- | --- |
| `ambient.underground_river` | `ambience` | `audio/ambient/underground_river.ogg` | zone 常驻 ambience | 低频水流、铁链碰撞、潮湿洞窟回声 |
| `audio.zone.underground_river` | `zone` | `audio/ambient/underground_river.ogg` | zone 进入 cue | 与 ambience 同源，允许复用同文件 |
| `ambient.crystal_cavern` | `ambience` | `audio/ambient/crystal_cavern.ogg` | zone 常驻 ambience | 冷亮共鸣、细碎晶体震颤、空洞回音 |
| `audio.zone.crystal_cavern` | `zone` | `audio/ambient/crystal_cavern.ogg` | zone 进入 cue | 与 ambience 同源，允许复用同文件 |
| `audio.interactable.river_ferry_anchor` | `interactable` | `audio/interactable/river_ferry_anchor.ogg` | 锚座触发 / crossing 安全窗 | 重链下落、木闸锁定、潮水压回 |
| `audio.interactable.crystal_resonance_node` | `interactable` | `audio/interactable/crystal_resonance_node.ogg` | resonance node 交互成功 | 晶体调谐、短促高频共鸣、余振衰减 |

冻结：

1. `currents` / `crystal_shards` 的 telegraph warning 继续复用 `audio.boss.warning`。
2. 不为 `drowned_ambush` 额外再做一套 cue。

## 5. PR-17 必补资源包

### 5.1 为什么必须补

`PR-17` 负责把 `abyssal_temple / abyssal_heart` 从“最后一段继续打”提升成“终线区段”。  
如果它仍复用 `grey_gate_depths` 的 zone card 与 `ritual_altar/open` 的节点表现，终线只会像换怪不换场。

所以 `PR-17` 必补：

1. `2` 个 finale zone card
2. `2` 个 finale interactable prop
3. `1` 个 void-pressure vfx
4. `2` 组 finale ambience + zone cue
5. `2` 个 finale interactable cue

### 5.2 PR-17 视觉资产清单

| Asset ID | Category | Visual Key | Runtime Path | 用途 | 生成主题 |
| --- | --- | --- | --- | --- | --- |
| `phase3_v3_pr17_zone_abyssal_temple_icon` | `icon_quest` | `zone.abyssal_temple.icon` | `phase3/p3-followup/zone_abyssal_temple_icon.png` | world map / route selection | 破损圣印、深渊裂隙、护符 reliquary |
| `phase3_v3_pr17_zone_abyssal_temple_visual` | `portrait` | `zone.abyssal_temple.visual` | `phase3/p3-followup/zone_abyssal_temple_visual.png` | zone card / inspect | 坍塌祈祷厅、被侵蚀的护符走廊、深色祭坛 |
| `phase3_v3_pr17_zone_abyssal_heart_icon` | `icon_quest` | `zone.abyssal_heart.icon` | `phase3/p3-followup/zone_abyssal_heart_icon.png` | world map / route selection | 深渊心核、断裂护环、最终封印纹 |
| `phase3_v3_pr17_zone_abyssal_heart_visual` | `portrait` | `zone.abyssal_heart.visual` | `phase3/p3-followup/zone_abyssal_heart_visual.png` | zone card / inspect | 心核大厅、黑曜裂缝、压迫性 void glow 与 arena 前室 |
| `phase3_v3_pr17_prop_temple_ward_reliquary` | `prop_interactable` | `prop.temple_ward_reliquary` | `phase3/p3-followup/prop_temple_ward_reliquary.png` | ward 节点 / 后续 late-run spend 复用 | 深渊侵蚀的 reliquary 柜座、圣印残片、可触发护符锁 |
| `phase3_v3_pr17_prop_heart_ward_focus` | `prop_interactable` | `prop.heart_ward_focus` | `phase3/p3-followup/prop_heart_ward_focus.png` | finale pre-boss stabilizer | 黑曜石基座上的心核聚焦器、破碎护环、收束光脉 |
| `phase3_v3_pr17_vfx_zone_effect_void_pressure_01` | `tile_decal` | `vfx.zone.effect.void_pressure_01` | `phase3/p3-followup/vfx_zone_effect_void_pressure_01.png` | void pressure overlay | 暗色裂纹、收缩性虚空脉冲、低饱和紫黑能量 |

### 5.3 PR-17 图片 prompt 约束

1. 继续沿用 `ktome-middle-fantasy-painterly-tile-v1`。
2. `abyssal_temple` 侧重“被污染但仍有秩序残骸”。
3. `abyssal_heart` 侧重“最终封印场所与心核压力”，不是再画一个新 Boss。
4. `prop.temple_ward_reliquary` 与 `prop.heart_ward_focus` 必须明显不同于现有 `prop.ritual_altar`。
5. `vfx.zone.effect.void_pressure_01` 必须在暗色背景上仍可辨认，但不要做成高亮霓虹。

### 5.4 PR-17 音频清单

| Key | Cue Family | Source Path | 作用 | 声音方向 |
| --- | --- | --- | --- | --- |
| `ambient.abyssal_temple` | `ambience` | `audio/ambient/abyssal_temple.ogg` | zone 常驻 ambience | 低沉祷声残响、护印颤鸣、深渊漏风 |
| `audio.zone.abyssal_temple` | `zone` | `audio/ambient/abyssal_temple.ogg` | zone 进入 cue | 与 ambience 同源，允许复用同文件 |
| `ambient.abyssal_heart` | `ambience` | `audio/ambient/abyssal_heart.ogg` | zone 常驻 ambience | 心核脉动、压缩低频、终线静压 |
| `audio.zone.abyssal_heart` | `zone` | `audio/ambient/abyssal_heart.ogg` | zone 进入 cue | 与 ambience 同源，允许复用同文件 |
| `audio.interactable.temple_ward_reliquary` | `interactable` | `audio/interactable/temple_ward_reliquary.ogg` | reliquary 净化 / 激活 / spend 共用 | 护印解锁、金属扣合、短促圣性回响 |
| `audio.interactable.heart_ward_focus` | `interactable` | `audio/interactable/heart_ward_focus.ogg` | focus 稳定成功 | 心核收束、护环锁定、压迫低频瞬间收窄 |

冻结：

1. `void_pressure / void_eruption` 的 warning cue 继续复用 `audio.boss.warning`。
2. `abyssal_guardian` 的 Boss cue 不在本 PR 内重做。

## 6. PR-19 的资源口径

`PR-19` 不应新增新的 raw 资源包。

原因：

1. 它的核心问题是“奖励来源可见”与“late-run shard 花法”。
2. `来源可见` 第一版完全可以用：
   - log key 分层
   - badge / text row
   - summary label
3. `late-run reliquary spend` 若落到 `temple_ward_reliquary`，应直接复用 `PR-17`：
   - `prop.temple_ward_reliquary`
   - `audio.interactable.temple_ward_reliquary`
4. 如果 `PR-19` 另开 raw pack，会把本来应该在 `PR-17` 固化的 finale 节点身份又复制一份。

因此 `PR-19` 的资源冻结为：

1. 不新增新 prop
2. 不新增新 ambience
3. 不新增新 reward badge 图片
4. 只在 `RenderSnapshot / client render / i18n` 层补玩家可见文案和标签

## 7. PR-18 / PR-20 的资源口径

### 7.1 `PR-18`

当前文档假设的是：

1. 不新增 talent id
2. 不新增职业树
3. 只重做 breakpoint payoff 与 affix synergy

在这个范围下，不需要新增 raw art/audio。  
如果执行过程中把某个 payoff 做成新的主动 talent 或新的正式 status icon，那已经超出当前 `PR-18` 口径，应另开 companion 资源补丁，而不是偷带进主 PR。

### 7.2 `PR-20`

纯 AI/profile 收口，不应消耗资源生产预算。

## 8. 现有生产管线接法

### 8.1 图片计划与 manifest 接入

图片新增后，统一接入：

1. `game/src/main/resources/data/visuals/index.yaml`
2. `client/src/main/resources/manifests/visual-manifest.json`
3. `assets-src/image/specs/phase3-v3-pr16-gemini-plan.yaml`
4. `assets-src/image/specs/phase3-v3-pr17-gemini-plan.yaml`
5. 若一个 raw 图要提供多个 key，再单开 alias plan；否则不需要 alias plan

root lint 入口要追加：

```text
build.gradle.kts
  assetLint   -> --extra-plan phase3-v3-pr16-gemini-plan.yaml / phase3-v3-pr17-gemini-plan.yaml
  styleLint   -> --extra-plan phase3-v3-pr16-gemini-plan.yaml / phase3-v3-pr17-gemini-plan.yaml
  manifestLint -> --extra-plan phase3-v3-pr16-gemini-plan.yaml / phase3-v3-pr17-gemini-plan.yaml
```

### 8.2 音频计划与 manifest 接入

音频新增后，统一接入：

1. `game/src/main/resources/data/audio/index.yaml`
2. `game/src/main/resources/data/ambient/index.yaml`
3. `client/src/main/resources/manifests/audio-manifest.json`
4. `assets-src/audio/specs/phase3-v3-pr16-audio-plan.yaml`
5. `assets-src/audio/specs/phase3-v3-pr17-audio-plan.yaml`

root lint 入口要追加：

```text
build.gradle.kts
  audioLint -> --extra-plan phase3-v3-pr16-audio-plan.yaml / phase3-v3-pr17-audio-plan.yaml
```

### 8.3 图片生成命令

真正执行图片生成前，必须先向用户索取 `GEMINI_API_KEY`。  
拿到后再跑：

```bash
GEMINI_API_KEY=your_key ./scripts/generate_assets.sh \
  assets-src/image/specs/phase3-v3-pr16-gemini-plan.yaml \
  assets-src/image/raw/generated \
  assets-src/image/manifests/phase3-v3-pr16-generation-report.jsonl

GEMINI_API_KEY=your_key ./scripts/generate_assets.sh \
  assets-src/image/specs/phase3-v3-pr17-gemini-plan.yaml \
  assets-src/image/raw/generated \
  assets-src/image/manifests/phase3-v3-pr17-generation-report.jsonl
```

对应处理报告默认会落到：

```text
assets-src/image/manifests/phase3-v3-pr16-processing-report.jsonl
assets-src/image/manifests/phase3-v3-pr17-processing-report.jsonl
```

### 8.4 音频处理命令

音频 raw 文件落好之后，按 plan 过滤处理：

```bash
python3 scripts/process_audio.py \
  --runtime-manifest client/src/main/resources/manifests/audio-manifest.json \
  --raw-dir assets-src/audio/raw \
  --cleaned-dir assets-src/audio/cleaned \
  --runtime-root client/src/main/resources \
  --filter-plan assets-src/audio/specs/phase3-v3-pr16-audio-plan.yaml \
  --report assets-src/audio/manifests/phase3-v3-pr16-processing-report.jsonl

python3 scripts/process_audio.py \
  --runtime-manifest client/src/main/resources/manifests/audio-manifest.json \
  --raw-dir assets-src/audio/raw \
  --cleaned-dir assets-src/audio/cleaned \
  --runtime-root client/src/main/resources \
  --filter-plan assets-src/audio/specs/phase3-v3-pr17-audio-plan.yaml \
  --report assets-src/audio/manifests/phase3-v3-pr17-processing-report.jsonl
```

若本地只需要先打通 CI，可临时加：

```bash
--bootstrap-missing
```

但约束是：

1. 只能作为开发期占位，不是最终体验结论。
2. `PR-16 / PR-17` 合并前必须至少人工审核 cleaned 输出，确认 zone/interactable cue 已经能区分身份。

## 9. 建议执行顺序

1. 先改 `PR-16 / PR-17` 文档口径
2. 先补 plan YAML 与 key namespace，不先生成
3. 先把 root `assetLint / styleLint / audioLint / manifestLint` 接上新 plan
4. 再拿 `GEMINI_API_KEY` 跑图片批量
5. 再导入 / 处理音频
6. 最后跑：

```bash
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
./gradlew check
```

## 10. 最终判断

`PR-15 ~ PR-20` 里，真正应该补资源的是 `PR-16` 和 `PR-17`。  
它们的问题不是“画面还不够华丽”，而是当前 `zone/interactable/audio` 仍然大量复用 `grey_gate_depths / ritual_altar / armory_gate / interactable.open`，这会直接削弱 late-zone 与 finale 的玩法身份。

反过来：

1. `PR-15 / PR-20` 纯逻辑，不应掺资源
2. `PR-18` 当前口径不需要资源
3. `PR-19` 应复用 `PR-17` 的 reliquary 资源，不另开 raw pack

所以最合理的资源策略不是“所有 PR 都补一点图和音”，而是把资源预算集中到 `PR-16 / PR-17` 的 identity pack 上。

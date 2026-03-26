> 执行前必须先完整阅读并接受：
> `docs/review/phase3/2026-03-26-phase3-pr-09-content-floor-completion.md`
> `docs/phase2/2026-03-13-phase2-pr-04-snapshot-and-manifest.md`
> `docs/phase2/2026-03-20-phase2-pr-07-post-review-execution-plan.md`

# Phase 3 - PR-09 Asset Batch Generation Checklist

**阶段**: `Phase 3 / follow-up / PR-09 companion`  
**优先级**: `P0`  
**适用对象**: `W9c Asset/Audio Lane`  
**目标**: 把 `PR-09` 中新增 monster / boss / talent 的资源生产拆成可直接执行的批量清单，避免出现“data key 已落地、manifest 未补齐、raw asset 命名混乱、formal path 继续吃 fallback”的半成品。

---

## 1. 使用方式

这份清单不是新的 PR 范围，而是 `PR-09` 的 `W9c` 执行细则。

执行顺序固定为：

1. 先冻结 namespace、命名规则和 family/boss/talent 分组。
2. 再按批次生产 raw asset。
3. 每批 raw asset 完成后，立即补 `visuals/audio index + manifest`。
4. 每批接入后立即跑 `assetLint / audioLint / manifestLint`，不允许堆到最后一次性修。
5. 所有 `PR-09` 新增正式路径 key，在最终 gate 前必须做到：
   - `0 missing_visual`
   - `0 audio.fallback.silence`
   - `0 prefix fallback`

## 2. 命名与目录冻结

### 2.1 原始资源目录

`PR-09` 新增 raw asset 统一落到：

```text
client/src/main/resources/phase3/p3-followup/
client/src/main/resources/audio/monster/
client/src/main/resources/audio/boss/
client/src/main/resources/audio/talent/
```

目录约束：

1. 视觉资源不再继续写入 `phase2/p2-*`。
2. 音频继续沿现有 `audio/<family>/` 目录，但新增文件必须打 `phase3/followup` tag。
3. 若多个 key 复用同一 raw asset，允许 manifest 多 key 指向同一路径；不允许反过来靠 prefix fallback 偷过。

### 2.2 文件名规则

视觉 raw 文件名统一使用小写蛇形：

```text
actor_<monster_id>.png
icon_monster_<monster_id>.png
boss_<boss_id>_visual.png
icon_boss_<boss_id>.png
talent_<profession>_<talent_id>_visual.png
icon_skill_<profession>_<talent_id>.png
```

示例：

```text
actor_bandit_cutthroat.png
icon_monster_bandit_cutthroat.png
boss_orc_molten_giant_visual.png
icon_boss_orc_molten_giant.png
talent_vanguard_shield_slam_visual.png
icon_skill_vanguard_shield_slam.png
```

音频 raw 文件名统一使用小写蛇形：

```text
audio/monster/<family_or_anchor>.ogg
audio/boss/<boss_id>.ogg
audio/talent/<talent_id>.ogg
```

示例：

```text
audio/monster/bandit_skirmisher.ogg
audio/monster/abyssal_family.ogg
audio/boss/orc_molten_giant.ogg
audio/talent/shield_slam.ogg
```

### 2.3 Key 命名规则

视觉 key：

```text
actor.<monster_id>
icon.monster.<monster_id>
boss.<boss_id>.visual
boss.<boss_id>.icon
talent.<profession>.<talent_id>.visual
talent.<profession>.<talent_id>.icon
icon.skill.<profession>.<talent_id>
```

音频 key：

```text
audio.monster.<monster_id_or_family>
audio.boss.<boss_id>
audio.talent.<talent_id>
```

规则：

1. key 使用点号命名，raw 文件使用下划线命名。
2. `boss_id` 与 monster/boss schema id 保持一致，不新造第二套别名。
3. `talent` 音频 key 不带职业前缀，继续沿现有 `audio.talent.<talent_id>` 模式。
4. `icon.skill.*` 与 `talent.*.icon` 可同时存在：
   - `talent.*.icon` 作为正式 visual key
   - `icon.skill.*` 作为历史兼容和 UI 直接引用入口
5. monster family 允许共享音频，但 visual key 不允许用 family key 代替正式 monster key。

## 3. Manifest 记录规则

### 3.1 `visuals/index.yaml`

每个新增正式 visual key 都要先登记到：

[visuals/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/visuals/index.yaml)

最低登记集合：

1. 新 monster：`actor.*` + `icon.monster.*`
2. 新 Boss：`actor.*` + `icon.monster.*` + `boss.*.visual` + `boss.*.icon`
3. 新主动 talent：`talent.*.visual` + `talent.*.icon` + `icon.skill.*`
4. 新被动 talent：至少 `talent.*.icon`，若 UI/动画需要再补 `visual`

### 3.2 `visual-manifest.json`

[visual-manifest.json](/Users/luo/Documents/github/K-ToME/client/src/main/resources/manifests/visual-manifest.json) 的字段规则：

| key 前缀 | category | rawOutputPath 模板 | 必填 tags |
| --- | --- | --- | --- |
| `actor.` | `actor_sprite` | `phase3/p3-followup/actor_<id>.png` | `phase3`, `followup`, `monster/boss`, `family/zone` |
| `icon.monster.` | `icon` | `phase3/p3-followup/icon_monster_<id>.png` | `phase3`, `followup`, `icon`, `monster` |
| `boss.*.visual` | `actor_sprite` | `phase3/p3-followup/boss_<id>_visual.png` | `phase3`, `followup`, `boss` |
| `boss.*.icon` | `icon` | `phase3/p3-followup/icon_boss_<id>.png` | `phase3`, `followup`, `boss`, `icon` |
| `talent.*.visual` | `icon_skill` | `phase3/p3-followup/talent_<profession>_<id>_visual.png` | `phase3`, `followup`, `talent`, `profession` |
| `talent.*.icon` | `icon` 或 `icon_skill` | `phase3/p3-followup/icon_skill_<profession>_<id>.png` | `phase3`, `followup`, `talent`, `profession` |
| `icon.skill.*` | `icon` | `phase3/p3-followup/icon_skill_<profession>_<id>.png` | `phase3`, `followup`, `talent`, `profession` |

额外规则：

1. `actor_sprite` 必须带 `asciiGlyph` 与 `asciiColorHex`。
2. `pivotX/pivotY` 默认沿用现有 actor 口径：`0.5 / 0.08`；若是大型 Boss，可单独调整，但必须在同批清单里显式记录。
3. `footprint` 默认为 `1x1`，若出现大体型 Boss，必须同步验证占位与渲染，不允许只改图不改 footprint。
4. 不允许把正式路径 key 指向 `debug/missing_visual.png`。

### 3.3 `audio/index.yaml`

每个新增正式 audio profile 都要先登记到：

[audio/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/audio/index.yaml)

最低登记集合：

1. 新 family monster audio：`audio.monster.<anchor>`
2. 新 Boss：`audio.monster.<boss_id>` + `audio.boss.<boss_id>`
3. 新主动 talent：`audio.talent.<talent_id>`
4. 被动 talent 默认不强制新增 audio，除非它有主动触发反馈

### 3.4 `audio-manifest.json`

[audio-manifest.json](/Users/luo/Documents/github/K-ToME/client/src/main/resources/manifests/audio-manifest.json) 的字段规则：

| key 前缀 | cueFamily | sourcePath 模板 | 必填 tags |
| --- | --- | --- | --- |
| `audio.monster.` | `monster` | `audio/monster/<id_or_family>.ogg` | `phase3`, `followup`, `monster`, `family/zone` |
| `audio.boss.` | `boss` | `audio/boss/<boss_id>.ogg` | `phase3`, `followup`, `boss` |
| `audio.talent.` | `talent` | `audio/talent/<talent_id>.ogg` | `phase3`, `followup`, `talent`, `profession/family` |

额外规则：

1. `eventId` 必须与 `key` 保持一致。
2. 不允许新增正式 key 指向 `audio/fallback/silence.ogg`。
3. 同动作族 talent 可以共用同一 `sourcePath`，但 key 仍需各自独立登记。

## 4. 批次顺序

### 4.1 Batch-0 Namespace Freeze

先产出 `asset_namespace_table.md` 或等价清单，冻结：

1. 新 monster id 列表
2. family/zone 归属
3. 新 Boss id 列表
4. 新 talent id 列表
5. 每个 id 对应的 visual/audio key
6. 每个 key 对应的 raw 文件名

通过标准：

1. 同一对象没有第二套 key。
2. 不存在 manifest key 与 schema id 不一致。
3. 不存在 `phase2/p2-*` 新路径。

### 4.2 Batch-1 Monster Family Base Sheet

按 family 先生产最小基底资源：

1. `bandit`
2. `warded_ruin`
3. `forge`
4. `crystal`
5. `river`
6. `abyssal`

每个 family 最少交付：

1. `1` 个基底 actor sprite
2. `1` 个 icon 模板
3. `1` 条 family monster audio

执行顺序：

1. 先做 optional zone family：`bandit / warded_ruin / forge / crystal`
2. 再做 late-game family：`river / abyssal`

通过标准：

1. 同 family 新怪可以只靠 palette / weapon / crest overlay 拉开差异。
2. 所有 family 基底资源都已进入 `visuals/audio index + manifest`。

### 4.3 Batch-2 Monster Variant Expansion

在 family base sheet 之上，按 zone roster 把具体 monster key 补齐。

每个新 monster 必须完成：

1. `actor.<monster_id>`
2. `icon.monster.<monster_id>`
3. `audio.monster.<monster_id_or_family>`
4. `name/desc` i18n

优先级顺序：

1. optional zone 独有怪
2. `underground_river`
3. `abyssal_temple`
4. `abyssal_heart`

通过标准：

1. 正式 monster key 不走 prefix fallback。
2. 同 zone 至少有 `1` 个视觉上可识别的 family anchor。

### 4.4 Batch-3 Boss Pack

Boss pack 只做 3 个正式 Boss：

1. `orc.molten_giant`
2. `cultist.dungeon_lord`
3. `abyssal.guardian`

每个 Boss 必须交付：

1. `actor.<boss_id>`
2. `icon.monster.<boss_id>`
3. `boss.<boss_id>.visual`
4. `boss.<boss_id>.icon`
5. `audio.monster.<boss_id>`
6. `audio.boss.<boss_id>`

通过标准：

1. 三 Boss 视觉 silhouette 可区分。
2. 三 Boss 音频 cue 不再共享同一默认音。
3. `bossHarness` 不再只凭 telegraph/phase id 识别 Boss。

### 4.5 Batch-4 Talent Active Pack

只对新增主动 talent 做正式资源；被动 talent 以 icon 为主。

动作族顺序固定为：

1. `charge / slam`
2. `ward / shield / cleanse`
3. `beam / shard / arcane_burst`
4. `curse / control / ritual_break`

每个新增主动 talent 必须交付：

1. `talent.<profession>.<talent_id>.visual`
2. `talent.<profession>.<talent_id>.icon`
3. `icon.skill.<profession>.<talent_id>`
4. `audio.talent.<talent_id>`

通过标准：

1. 同动作族允许共享音频底板。
2. 但正式 key 必须全部存在，不允许 UI 继续走缺省 skill icon。

### 4.6 Batch-5 Manifest / Index / i18n Integration

每完成一批 raw asset，立即做：

1. 更新 `visuals/index.yaml`
2. 更新 `audio/index.yaml`
3. 更新 `visual-manifest.json`
4. 更新 `audio-manifest.json`
5. 更新 `zh-CN.json / en-US.json`

通过标准：

1. 新增 key 在 client 端 exact resolve。
2. `ContractLintTest` / `RenderSnapshotAssetAudit` 不出现 fallbackUsed 或 matchedByPrefix。

### 4.7 Batch-6 Cleanup Gate

最终批次只做清理和门禁，不再补新内容。

必须清掉：

1. `placeholder` tag
2. 指向 `missing_visual` 的正式新 key
3. 指向 `audio.fallback.silence` 的正式新 key
4. 依赖 prefix fallback 的正式新 key

## 5. 批次交付模板

每批交付都按同一模板记录：

```text
Batch:
Scope:
Schema IDs:
Visual keys:
Audio keys:
Raw files:
Manifest entries:
Fallback count:
Lint commands:
Residual risk:
```

建议把每批记录附在同一次 PR 描述或 `build/reports/asset-batch/` 输出中，避免最后无法追溯“哪个 batch 引入了 fallback”。

## 6. 最小执行样例

### 6.1 新增 monster `bandit.cutthroat`

需要同时落地：

```text
actor.bandit.cutthroat
icon.monster.bandit.cutthroat
audio.monster.bandit.cutthroat
phase3/p3-followup/actor_bandit_cutthroat.png
phase3/p3-followup/icon_monster_bandit_cutthroat.png
audio/monster/bandit_cutthroat.ogg
```

### 6.2 新增 boss `orc.molten_giant`

需要同时落地：

```text
actor.orc.molten_giant
icon.monster.orc.molten_giant
boss.orc.molten_giant.visual
boss.orc.molten_giant.icon
audio.monster.orc.molten_giant
audio.boss.orc.molten_giant
phase3/p3-followup/actor_orc_molten_giant.png
phase3/p3-followup/icon_monster_orc_molten_giant.png
phase3/p3-followup/boss_orc_molten_giant_visual.png
phase3/p3-followup/icon_boss_orc_molten_giant.png
audio/monster/orc_molten_giant.ogg
audio/boss/orc_molten_giant.ogg
```

### 6.3 新增主动 talent `vanguard.shield_slam`

需要同时落地：

```text
talent.vanguard.shield_slam.visual
talent.vanguard.shield_slam.icon
icon.skill.vanguard.shield_slam
audio.talent.shield_slam
phase3/p3-followup/talent_vanguard_shield_slam_visual.png
phase3/p3-followup/icon_skill_vanguard_shield_slam.png
audio/talent/shield_slam.ogg
```

## 7. 自动化门禁

每个 batch 完成后至少跑：

```bash
./gradlew assetLint
./gradlew audioLint
./gradlew manifestLint
```

集成回归时再跑：

```bash
./gradlew :game:test --tests "com.ktome.game.data.MonsterSchemaTest"
./gradlew :game:bossHarness --tests "*BossHarnessTest"
./gradlew :game:soloClearLab --tests "*SoloClearLabV2Test"
./gradlew :game:longRunLab --tests "*LongRunLabFullTest"
./gradlew check
```

## 8. 一句话原则

先冻结 key 和目录，再生产 raw asset；每批做完立即补 manifest 并清 fallback，不要把“资源只是晚点补”留到 `PR-09` 末尾。

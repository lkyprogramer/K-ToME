> 执行前必须先完整阅读并接受：
> `docs/review/phase3/2026-03-26-phase3-follow-up-pr-07-objective-runtime-and-gate-hardening.md`
> `docs/review/phase3/2026-03-26-phase3-pr-08-reward-milestone-affixization.md`
> `docs/review/phase3/2026-03-26-phase3-pr-09-asset-batch-generation-checklist.md`
> `docs/phase3/2026-03-13-phase3-pr-04-aiprofile-dsl-and-boss-encounter.md`
> `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 3 - PR-09 Phase3 Content Floor Completion

**阶段**: `Phase 3 / follow-up`  
**优先级**: `P0`  
**前置条件**: `PR-07` 完成（世界推进/runtime 成立），`PR-08` 完成（milestone reward 已能驱动 build）  
**对应问题**: 当前 Phase 3 的规则设施已经足够深，但内容厚度仍明显低于路线图底线。基础职业 talent 当前核实为 `56 / 64`，monster roster 仍只有 `26`，其中只有 `6` 个怪带 talent，这会让长局深度长期被“系统强、内容薄”拖住。

**Lane-parallel 拆分**：

- **W9a (Content Lane / Talents)**: 基础职业 talent 补齐到 `64`，必要时顺手补进阶职业薄弱节点
- **W9b (Content Lane / Monsters)**: monster roster 补到 Phase 3 底线，late-game 与 optional zone 独有 roster 成立
- **W9c (Asset/Audio Lane)**: monster/talent visual key、audio profile、manifest、fallback 与批量生成规则
- **W9d (Tools/QA Lane)**: content coverage、boss distinctness、solo/long-run/boss harness 回归

---

## 1. 阶段目标

把 Phase 3 的职业树与怪物 roster 补到“足以支撑长局 build 与 encounter 差异”的最低内容底线。

完成标准：

1. 4 基础职业 talent 总数至少达到 `64`，即每职业至少 `16` 个正式 talent。
2. 基础职业当前缺口按实际状态补齐：
   - `vanguard 15 -> 16`
   - `arcanist 14 -> 16`
   - `rogue 14 -> 16`
   - `templar 13 -> 16`
3. monster roster 至少补到 `60` 个模板。
4. 带 talent 的 monster 至少提升到 `16` 个。
5. 4 个 optional zone 全部具备独有敌方内容，而不只是地图壳。
6. `molten_giant / dungeon_lord / abyssal.guardian` 三个 Phase 3 Boss 不再共享同质技能包。
7. `bossHarness / soloClearLab / longRunLab` 能证明新内容不是只在数据层存在。

## 2. 当前问题

1. 基础职业 talent 总数当前仍只有 `56`，低于路线图的 `64`。
2. monster 模板总数当前仍只有 `26`，与路线图 `~60` 的底线相差过大。
3. 带 talent 的 monster 当前只有 `6` 个，导致敌方侧决策密度远低于玩家侧。
4. `cultist.dungeon_lord` 与 `abyssal.guardian` 当前仍共享 `war_cry + power_strike + charge` 核心技能包。
5. optional zone 当前即使进入 runtime，也仍缺少足够的 zone-specific roster 来支撑重复游玩差异。
6. 如果继续只补规则、不补内容，Phase 3 会长期停留在“能通关但不耐玩”的状态。

### 2.1 本 PR 必须冻结的口径

1. 本 PR 只补内容，不引入新的 `DamageType / ResourceType / PowerType`。
2. 基础职业 talent 目标固定为 `>= 64`，不允许再用“后续再补”模糊处理。
3. monster roster 目标固定为 `>= 60`，其中带 talent 的模板 `>= 16`。
4. 每个 optional zone 至少有 `1` 个独有普通怪或精英怪，且不与相邻 mandatory zone 完全重合。
5. Phase 3 三个正式 Boss 必须拥有可区分的核心技能包。
6. 新增内容必须继续走现有 typed schema / AIProfile / TelegraphSpec / TalentDef 主链。
7. 本 PR 不允许用“复制同一技能换名字”伪装成内容补量。
8. 新内容需要同步 i18n / asset/audio key，不允许把占位字段留给后续。

## 3. 范围与非目标

### 3.1 范围

1. 基础职业 talent 补齐到 `64`。
2. optional / late-game zone 的 monster roster 补量。
3. 带 talent 怪物数量与质量提升。
4. Phase 3 三个 Boss 的技能语义重分化。
5. content coverage / boss harness / solo clear / long run 回归。

### 3.2 非目标

1. 不新增可玩职业。
2. 不新增可玩种族。
3. 不做 Phase 4 的 hidden content、ProcGen、artifact、unique。
4. 不做完整 UI 信息面收口，这属于 `PR-10`。
5. 不把 `Shadowblade / Warden` 强行补成 release-ready，可维持 frozen contract。

## 4. 技术方案

### 4.1 [W9a] 基础职业 talent floor

建议文件：

```text
game/src/main/resources/data/talents/index.yaml
game/src/main/resources/data/professions/index.yaml
game/src/test/kotlin/com/ktome/game/ClassFormalizationTestSupport.kt
```

冻结口径：

1. 4 基础职业至少各有 `16` 个 talent。
2. 新增 talent 必须优先补“中后段打法分叉”，不能只补被动数值抬升。
3. 每个职业至少补一类当前欠缺的 build 角色：
   - `Vanguard`: 机动/控制 endline
   - `Arcanist`: late-game control / arcane payoff
   - `Rogue`: finisher / escape / anti-elite answer
   - `Templar`: aura / cleanse / delayed burst answer
4. 当前 4 基础职业的现有 talent 不做语义重写，新增内容优先沿现有 tree 继续长出。

### 4.2 [W9a] 进阶职业最小加厚

冻结口径：

1. `Berserker / Spellblade` 当前不是本 PR 主目标，但允许顺手补到“至少 10~12 个 talent”。
2. 这一步只为减少 smoke/build 同质，不把它们升级成新的 Phase 3 主线重点。
3. 如果与 monster roster 补量冲突，优先 monster roster。

### 4.3 [W9b] Monster roster floor

建议文件：

```text
game/src/main/resources/data/monsters/index.yaml
game/src/main/resources/data/ai/index.yaml
game/src/test/kotlin/com/ktome/game/data/MonsterSchemaTest.kt
```

冻结口径：

1. 总 monster 模板数至少 `60`。
2. 带 talent 的 monster 模板至少 `16`。
3. 分布上必须优先补：
   - optional zone 独有怪
   - `underground_river / abyssal_temple / abyssal_heart` 的 late-game 怪
   - 不同 zone 的控制/位移/净化/护盾压力怪
4. 不允许只通过“同模板换颜色/换抗性”刷数量。

建议底线分布：

| 区域 | 最低新增方向 |
| --- | --- |
| `bandit_camp` | 伏击/陷阱/碎片风险怪 |
| `elven_ruins` | ward / cleanse / relic 守卫怪 |
| `molten_core` | heat pressure / forge guard 变体 |
| `crystal_cavern` | line / beam / shard resonance 怪 |
| `underground_river` | terrain / pull / displacement 怪 |
| `abyssal_temple` | debuff / cleanse-check / shield-break 怪 |
| `abyssal_heart` | finale escort / anti-defensive 怪 |

### 4.4 [W9b] Boss 语义重分化

建议文件：

```text
game/src/main/resources/data/monsters/index.yaml
game/src/main/resources/data/bosses/index.yaml
game/src/main/resources/data/ai/index.yaml
game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt
```

冻结口径：

1. `orc.molten_giant` 继续承担重击/冲锋/热压题目。
2. `cultist.dungeon_lord` 应偏命令/号令/控场，而不是继续和 guardian 共用主包。
3. `abyssal.guardian` 必须拥有自己的核心技能包，不能继续是 `dungeon_lord` 的再皮肤化。
4. `bossHarness` 需要能断言这些 phase-specific action，而不是只看 telegraph / phase id。

### 4.5 [W9c] 美术 / 音频补充生成方案

详细批次、命名和 manifest 规则见：

[2026-03-26-phase3-pr-09-asset-batch-generation-checklist.md](/Users/luo/Documents/github/K-ToME/docs/review/phase3/2026-03-26-phase3-pr-09-asset-batch-generation-checklist.md)

建议文件：

```text
game/src/main/resources/data/visuals/index.yaml
game/src/main/resources/data/audio/index.yaml
client/src/main/resources/manifests/visual-manifest.json
client/src/main/resources/manifests/audio-manifest.json
client/src/main/resources/phase3/p3-followup/*
client/src/main/resources/audio/monster/*
client/src/main/resources/audio/boss/*
client/src/main/resources/audio/talent/*
```

冻结口径：

1. `PR-09` 需要补充美术/音频资源，但只允许沿现有 `visuals index -> manifest`、`audio index -> manifest` 主链扩展，不得绕开 manifest 直接引用 raw 路径。
2. 本 PR 不要求为每个新怪物或每个新 talent 独立作曲；音乐/BGM 不是 `PR-09` 的交付重点。
3. 普通怪与精英怪的资源策略固定为“家族复用 + 少量变体”，不做一怪一套独立资源：
   - 同一 zone/family 的普通怪可共享 `actor sprite` 基底与 `audio.monster.<family>` profile
   - 变体差异优先通过 silhouette、配色、武器/冠饰 overlay 表达
   - 只有 captain / elite anchor / boss 才允许独立 sprite 或独立 monster audio
4. Boss 资源策略固定为：
   - 3 个 Phase 3 正式 Boss 必须拥有独立 `actor` / `icon` / `boss.visual`
   - 3 个正式 Boss 必须拥有独立 `audio.boss.*`
   - 不要求本 PR 新增 boss 专属 BGM，继续复用 zone ambience + boss cue
5. Talent 资源策略固定为：
   - 新增主动 talent：必须具备 `talent.*.visual`、`talent.*.icon`、`icon.skill.*`，并绑定合法 `audio.talent.*`
   - 新增被动 talent：允许复用 tree/icon 家族，不强制每个被动都生产独立 raw 图
   - 语义接近的 talent 应优先复用同族音频，不做一招一条全新音频
6. 原始资源输出目录固定为 `client/src/main/resources/phase3/p3-followup/`，避免继续把 Phase 3 资源散落在 `phase2/p2-*` 目录下。
7. manifest 记录要求固定为：
   - visual: `category / rawOutputPath / footprint / pivot / tags / asciiGlyph / asciiColorHex`
   - audio: `cueFamily / eventId / sourcePath / tags`
   - tags 必须至少包含 `phase3`、`followup`、`zone/family`
8. fallback 策略固定为：
   - 开发期允许多个 key 指向同一 raw 资源
   - 但不允许只加 data key、不补 manifest entry
   - 不允许新增 `placeholder` key 后长期不清理
9. zone 级 ambience 只在某个 optional/late zone 首次因内容补量变成正式高频路径、且现有 ambience 明显不适配时才补；否则默认复用同 biome/family 的环境音。

建议批量生成单位：

1. `Monster Family Sheet`
   - `bandit / warded_ruin / forge / crystal / river / abyssal`
   - 每个 family 先出 `1` 个基底 actor sprite + `1` 个 icon 模板 + `1` 条 family monster audio
   - 同 family 变体通过 palette / weapon / crest overlay 扩展
2. `Boss Pack`
   - `actor.<boss>`
   - `icon.monster.<boss>`
   - `boss.<boss>.visual`
   - `audio.boss.<boss>`
3. `Talent Pack`
   - 主动 skill 先按 `charge / slam / ward / beam / curse / shard` 这类动作族归并
   - 同动作族允许共用音频底板，不要求每个 talent 独立录制

### 4.6 [W9d] 内容覆盖门禁

建议文件：

```text
game/src/test/kotlin/com/ktome/game/data/MonsterSchemaTest.kt
game/src/test/kotlin/com/ktome/game/harness/SoloClearLabV2Test.kt
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
```

冻结口径：

1. `MonsterSchemaTest` 要显式验证：
   - monster 总数
   - 带 talent 数
   - optional / late-game zone 覆盖
2. `SoloClearLab` 要证明基础职业新增 talent 至少有一部分进入脚本/构筑路径，而不是完全死数据。
3. `LongRunLab` 要验证更多怪物语义后，headless turn 与胜率没有明显崩坏。

## 5. 推荐改动面

### 5.1 `core`

1. 尽量不改 `core` 规则
2. 若新增 talent / monster 需要少量 effect op 复用扩展，必须保持 typed schema 主链

### 5.2 `game`

1. `talents/index.yaml`
2. `monsters/index.yaml`
3. `ai/index.yaml`
4. `bosses/index.yaml`
5. 相关 profession / zone 数据

### 5.3 `client`

1. `client/src/main/resources/phase3/p3-followup/*`
2. `client/src/main/resources/manifests/visual-manifest.json`
3. `client/src/main/resources/manifests/audio-manifest.json`
4. 如新增 talent / monster 需要新的 name/desc/icon/audio key，同步补 manifest 与 i18n
5. 不在本 PR 做大 UI 改造

### 5.4 `tools / QA`

1. `MonsterSchemaTest`
2. `BossHarnessTest`
3. `SoloClearLabV2Test`
4. `LongRunLabFullTest`
5. `assetLint / audioLint / manifestLint`

## 6. 测试与自证

### 6.1 必测类

1. `MonsterSchemaTest`
2. `SchemaV2LoaderTest`
3. `BossHarnessTest`
4. `SoloClearLabV2Test`
5. `LongRunLabTest`
6. `LongRunLabFullTest`

### 6.2 必测行为

1. 基础职业 talent 总数达到 `64`。
2. monster 总数达到 Phase 3 底线。
3. optional zone 的怪物内容不再只是相邻 mandatory zone 的重复池。
4. 三个 Phase 3 Boss 有可断言的独立行动语义。
5. 新增内容不会让 `soloClearLab / longRunLab` 整体回退。
6. 新增 visual/audio key 能全部 resolve 到 manifest，不产生长期 placeholder。

### 6.3 自动化命令

```bash
./gradlew :game:test --tests "com.ktome.game.data.MonsterSchemaTest"
./gradlew :game:test --tests "com.ktome.game.data.SchemaV2LoaderTest"
./gradlew :game:bossHarness --tests "*BossHarnessTest"
./gradlew :game:soloClearLab --tests "*SoloClearLabV2Test"
./gradlew :game:longRunLab --tests "*LongRunLabTest"
./gradlew :game:longRunLab --tests "*LongRunLabFullTest"
./gradlew assetLint
./gradlew audioLint
./gradlew manifestLint
./gradlew check
```

### 6.4 白盒验证

1. 用 4 基础职业进入 talent assign / loadout，确认新增 talent 至少能在 UI 中形成新的主动栏选择。
2. 分别进入至少 2 个 optional zone 和 2 个 late-game zone，确认遇到独有怪或独有精英。
3. 分别触发 `molten_giant / dungeon_lord / abyssal.guardian` 的关键动作，确认体感上可区分。

## 7. 出口门禁

1. 4 基础职业 talent 总数 `>= 64`。
2. monster 模板总数 `>= 60`。
3. 带 talent 怪物数 `>= 16`。
4. 4 个 optional zone 全部具备独有敌方内容。
5. `bossHarness` 能断言三 Boss 的 phase-specific action。
6. 新增 visual/audio key 已全部补到 `visuals/audio index + manifest`，且无长期 placeholder。
7. `assetLint / audioLint / manifestLint / soloClearLab / longRunLab / check` 保持绿色。

## 8. 风险与止损

### 8.1 风险

1. 内容补量过大后，平衡会明显波动。
2. 新怪过多复用旧 AI，会出现“数量上去了，语义没上去”的伪完成。
3. 新 talent 如果只是数值抬升，会稀释而不是提升构筑差异。

### 8.2 止损

1. 先补内容底线，不同时引入新的系统族。
2. 每批新增怪都必须指定 zone 语义与玩家应对题目。
3. 新 talent 的验收标准优先看玩法分叉，不只看数量达标。
